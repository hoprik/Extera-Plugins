package ru.hoprik.player.hooks;

import android.animation.ValueAnimator;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import de.robv.android.xposed.XC_MethodHook;
import org.telegram.messenger.*;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.VideoPlayer;

import java.util.ArrayList;

public class MediaControllerHook extends XC_MethodHook {

    private static Boolean hasCastSync = null;
    private static Object castSyncInstance = null;
    private static Boolean hasChromecast = null;
    private static Object chromecastInstance = null;

    private static void initOptionalComponents() {
        if (hasCastSync == null) {
            try {
                Class<?> cls = Class.forName("org.telegram.messenger.CastSync");
                castSyncInstance = cls.getMethod("getInstance").invoke(null);
                hasCastSync = true;
            } catch (Exception e) {
                hasCastSync = false;
            }
        }
        if (hasChromecast == null) {
            try {
                Class<?> cls = Class.forName("org.telegram.messenger.ChromecastController");
                chromecastInstance = cls.getMethod("getInstance").invoke(null);
                hasChromecast = true;
            } catch (Exception e) {
                hasChromecast = false;
            }
        }
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        MessageObject messageObject = (MessageObject) param.args[0];
        boolean silent = (boolean) param.args[1];
        MediaController controller = (MediaController) param.thisObject;

        String attachPath = messageObject.messageOwner.attachPath;
        if (attachPath == null || (!attachPath.startsWith("http://") && !attachPath.startsWith("https://"))) {
            return;
        }

        // Проверка на повторное воспроизведение того же сообщения
        VideoPlayer existingPlayer = (VideoPlayer) HookUtils.getPrivateField(controller, "audioPlayer");
        MessageObject currentPlaying = (MessageObject) HookUtils.getPrivateField(controller, "playingMessageObject");
        if (existingPlayer != null && currentPlaying != null && currentPlaying.getId() == messageObject.getId()) {
            boolean isPaused = (boolean) HookUtils.getPrivateField(controller, "isPaused");
            if (isPaused) {
                HookUtils.invokePrivateMethod(controller, "resumeAudio", new Class[]{MessageObject.class}, messageObject);
            }
            if (!SharedConfig.enabledRaiseTo(true)) {
                ChatActivity raiseChat = (ChatActivity) HookUtils.getPrivateField(controller, "raiseChat");
                if (raiseChat != null) {
                    HookUtils.invokePrivateMethod(controller, "startRaiseToEarSensors", new Class[]{ChatActivity.class}, raiseChat);
                }
            }
            param.setResult(true);
            return;
        }

        param.setResult(false);
        AndroidUtilities.runOnUIThread(() -> playHttpStream(controller, messageObject, silent));
    }

    private void playHttpStream(MediaController controller, MessageObject messageObject, boolean silent) {
        try {
            controller.cleanupPlayer(true, false);

            HookUtils.setPrivateField(controller, "downloadingCurrentMessage", false);
            HookUtils.setPrivateField(controller, "lastProgress", 0);
            HookUtils.setPrivateField(controller, "seekToProgressPending", 0f);
            HookUtils.setPrivateField(controller, "audioInfo", null);
            HookUtils.setPrivateField(controller, "shouldSavePositionForCurrentAudio", null);
            HookUtils.setPrivateField(controller, "playingMessageObject", messageObject);
            HookUtils.setPrivateField(controller, "isPaused", false);

            int playerNum = (int) HookUtils.getPrivateField(controller, "playerNum");
            playerNum++;
            HookUtils.setPrivateField(controller, "playerNum", playerNum);
            final int currentTag = playerNum;

            VideoPlayer audioPlayer = new VideoPlayer();
            HookUtils.setPrivateField(controller, "audioPlayer", audioPlayer);

            if (messageObject.audioProgress != 0) {
                HookUtils.setPrivateField(controller, "seekToProgressPending", messageObject.audioProgress);
                messageObject.audioProgress = 0;
            }

            audioPlayer.setDelegate(new VideoPlayer.VideoPlayerDelegate() {
                @Override
                public void onStateChanged(boolean playWhenReady, int playbackState) {
                    int nowNum = (int) HookUtils.getPrivateField(controller, "playerNum");
                    if (nowNum != currentTag) return;

                    float seekToProgress = (float) HookUtils.getPrivateField(controller, "seekToProgressPending");
                    if (audioPlayer != null && seekToProgress != 0 &&
                            (playbackState == 3 || playbackState == 1)) {
                        long duration = audioPlayer.getDuration();
                        int seekTo = (int) (duration * seekToProgress);
                        audioPlayer.seekTo(seekTo);
                        HookUtils.setPrivateField(controller, "lastProgress", seekTo);
                        HookUtils.setPrivateField(controller, "seekToProgressPending", 0f);
                        boolean ignorePlayerUpdate = (boolean) HookUtils.getPrivateField(controller, "ignorePlayerUpdate");
                        if (!ignorePlayerUpdate && hasCastSync != null && hasCastSync) {
                            try {
                                castSyncInstance.getClass().getMethod("seekTo", int.class).invoke(castSyncInstance, seekTo);
                            } catch (Exception ignored) {}
                        }
                    }

                    if (audioPlayer != null && hasCastSync != null && hasCastSync) {
                        try {
                            boolean isActive = (boolean) castSyncInstance.getClass().getMethod("isActive").invoke(castSyncInstance);
                            if (isActive) audioPlayer.setMute(true);
                        } catch (Exception ignored) {}
                    }

                    if (playbackState == 4) {
                        AndroidUtilities.runOnUIThread(() -> {

                            messageObject.audioProgress = 1f;
                            NotificationCenter.getInstance(messageObject.currentAccount)
                                    .postNotificationName(NotificationCenter.messagePlayingProgressDidChanged, messageObject.getId(), 0);
                            Boolean restored = (Boolean) HookUtils.invokePrivateMethodWithReturn(controller, "restoreMusicPlaylistState", new Class[]{});
                            if (restored == null || !restored) {
                                ArrayList<MessageObject> playlist = (ArrayList<MessageObject>) HookUtils.getPrivateField(controller, "playlist");
                                boolean isVoice = messageObject.isVoice();
                                if (playlist != null && !playlist.isEmpty() && (playlist.size() > 1 || !isVoice)) {
                                    HookUtils.invokePrivateMethod(controller, "playNextMessageWithoutOrder", new Class[]{boolean.class}, true);
                                } else {
                                    Boolean noNext = (Boolean) HookUtils.invokePrivateMethodWithReturn(controller, "hasNoNextVoiceOrRoundVideoMessage", new Class[]{});
                                    controller.cleanupPlayer(true, noNext != null && noNext, isVoice, false);
                                }
                            }
                        });
                    }
                }
                @Override public void onError(VideoPlayer player, Exception e) {
                    AndroidUtilities.runOnUIThread(() -> controller.cleanupPlayer(true, true));
                }
                @Override public void onVideoSizeChanged(int w, int h, int rot, float ratio) {}
                @Override public void onRenderedFirstFrame() {}
                @Override public void onRenderedFirstFrame(AnalyticsListener.EventTime et) {}
                @Override public void onSeekFinished(AnalyticsListener.EventTime et) {}
                @Override public void onSeekStarted(AnalyticsListener.EventTime et) {}
                @Override public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture st) {}
                @Override public boolean onSurfaceDestroyed(android.graphics.SurfaceTexture st) { return false; }
            });

            Uri uri = Uri.parse(messageObject.messageOwner.attachPath);
            audioPlayer.preparePlayer(uri, "other");
            audioPlayer.setStreamType(AudioManager.STREAM_MUSIC);

            HookUtils.invokePrivateMethod(controller, "checkAudioFocus", new Class[]{MessageObject.class, boolean.class}, messageObject, true);
            HookUtils.invokePrivateMethod(controller, "setPlayerVolume", new Class[]{});
            HookUtils.invokePrivateMethod(controller, "startProgressTimer", new Class[]{MessageObject.class}, messageObject);

            // Анимация громкости (оставлена)
            if (!messageObject.isVoice()) {
                ValueAnimator audioVolumeAnimator = (ValueAnimator) HookUtils.getPrivateField(controller, "audioVolumeAnimator");
                if (audioVolumeAnimator != null) {
                    audioVolumeAnimator.removeAllListeners();
                    audioVolumeAnimator.cancel();
                }
                float audioVolume = (float) HookUtils.getPrivateField(controller, "audioVolume");
                audioVolumeAnimator = ValueAnimator.ofFloat(audioVolume, 1f);
                audioVolumeAnimator.addUpdateListener(animation -> {
                    float val = (float) animation.getAnimatedValue();
                    HookUtils.setPrivateField(controller, "audioVolume", val);
                    HookUtils.invokePrivateMethod(controller, "setPlayerVolume", new Class[]{});
                });
                audioVolumeAnimator.setDuration(300);
                audioVolumeAnimator.setInterpolator(new LinearInterpolator());
                audioVolumeAnimator.start();
                HookUtils.setPrivateField(controller, "audioVolumeAnimator", audioVolumeAnimator);
            } else {
                HookUtils.setPrivateField(controller, "audioVolume", 1f);
                HookUtils.invokePrivateMethod(controller, "setPlayerVolume", new Class[]{});
            }

            audioPlayer.play();

            NotificationCenter.getInstance(messageObject.currentAccount)
                    .postNotificationName(NotificationCenter.messagePlayingDidStart, messageObject, null);

            Boolean canStart = (Boolean) HookUtils.invokePrivateMethodWithReturn(controller, "canStartMusicPlayerService", new Class[]{});
            android.content.Intent intent = new android.content.Intent(ApplicationLoader.applicationContext, MusicPlayerService.class);
            if (canStart != null && canStart) {
                ApplicationLoader.applicationContext.startService(intent);
            } else {
                ApplicationLoader.applicationContext.stopService(intent);
            }

            initOptionalComponents();
            if (hasCastSync != null && hasCastSync) {
                try {
                    castSyncInstance.getClass().getMethod("check", int.class).invoke(castSyncInstance, 1);
                    boolean ignorePlayerUpdate = (boolean) HookUtils.getPrivateField(controller, "ignorePlayerUpdate");
                    if (!ignorePlayerUpdate) {
                        if (hasChromecast != null && hasChromecast && chromecastInstance != null) {
                            boolean isCasting = (boolean) chromecastInstance.getClass().getMethod("isCasting").invoke(chromecastInstance);
                            if (isCasting) {
                                Object currentMedia = HookUtils.invokePrivateMethodWithReturn(controller, "getCurrentChromecastMedia", new Class[]{});
                                chromecastInstance.getClass().getMethod("setCurrentMediaAndCastIfNeeded", Object.class).invoke(chromecastInstance, currentMedia);
                            }
                        }
                        castSyncInstance.getClass().getMethod("setPlaying", boolean.class).invoke(castSyncInstance, true);
                    }
                } catch (Exception ignored) {}
            }

            if (!SharedConfig.enabledRaiseTo(true)) {
                ChatActivity raiseChat = (ChatActivity) HookUtils.getPrivateField(controller, "raiseChat");
                if (raiseChat != null) {
                    HookUtils.invokePrivateMethod(controller, "startRaiseToEarSensors", new Class[]{ChatActivity.class}, raiseChat);
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
            controller.cleanupPlayer(true, true);
        }
    }
}
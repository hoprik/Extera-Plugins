package ru.hoprik.player.hooks;

import android.media.AudioManager;
import android.net.Uri;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import de.robv.android.xposed.XC_MethodHook;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MusicPlayerService;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.VideoPlayer;

import java.util.ArrayList;

public class MediaControllerHook extends XC_MethodHook {

    @Override
    protected void beforeHookedMethod(MethodHookParam param) {
        MessageObject messageObject = (MessageObject) param.args[0];
        boolean silent = (boolean) param.args[1];
        MediaController controller = (MediaController) param.thisObject;

        String attachPath = messageObject.messageOwner.attachPath;
        if (attachPath != null && (attachPath.startsWith("http://") || attachPath.startsWith("https://"))) {
            param.setResult(false);
            AndroidUtilities.runOnUIThread(() -> playHttpStream(controller, messageObject, silent));
        }
    }

    private void playHttpStream(MediaController controller, MessageObject messageObject, boolean silent) {
        try {
            // 1. Очистка и сброс состояния (как в оригинале)
            controller.cleanupPlayer(true, false);
            HookUtils.setPrivateField(controller, "downloadingCurrentMessage", false);
            HookUtils.setPrivateField(controller, "lastProgress", 0);
            HookUtils.setPrivateField(controller, "seekToProgressPending", 0);
            HookUtils.setPrivateField(controller, "audioInfo", null);
            HookUtils.setPrivateField(controller, "shouldSavePositionForCurrentAudio", null);
            HookUtils.setPrivateField(controller, "playingMessageObject", messageObject);
            HookUtils.setPrivateField(controller, "isPaused", false);

            // 2. Увеличиваем playerNum для уникальности тега
            int playerNum = (int) HookUtils.getPrivateField(controller, "playerNum");
            playerNum++;
            HookUtils.setPrivateField(controller, "playerNum", playerNum);
            final int currentTag = playerNum;

            // 3. Создаём плеер
            VideoPlayer audioPlayer = new VideoPlayer();
            HookUtils.setPrivateField(controller, "audioPlayer", audioPlayer);

            // Сохраняем позицию для seek, если была
            if (messageObject.audioProgress != 0) {
                HookUtils.setPrivateField(controller, "seekToProgressPending", messageObject.audioProgress);
                messageObject.audioProgress = 0;
            }

            // 4. Делегат с полной логикой (seek, cast, окончание)
            audioPlayer.setDelegate(new VideoPlayer.VideoPlayerDelegate() {
                @Override
                public void onStateChanged(boolean playWhenReady, int playbackState) {
                    int nowNum = (int) HookUtils.getPrivateField(controller, "playerNum");
                    if (nowNum != currentTag) return;

                    // Обработка seekToProgressPending (как в оригинале)
                    float seekToProgress = (float) HookUtils.getPrivateField(controller, "seekToProgressPending");
                    if (audioPlayer != null && seekToProgress != 0 &&
                            (playbackState == 3|| playbackState == 1)) {
                        long duration = audioPlayer.getDuration();
                        int seekTo = (int) (duration * seekToProgress);
                        audioPlayer.seekTo(seekTo);
                        HookUtils.setPrivateField(controller, "lastProgress", seekTo);
                        HookUtils.setPrivateField(controller, "seekToProgressPending", 0f);

                        // Синхронизация с CastSync, если не ignorePlayerUpdate
                        boolean ignorePlayerUpdate = (boolean) HookUtils.getPrivateField(controller, "ignorePlayerUpdate");
                        if (!ignorePlayerUpdate) {
                            try {
                                Class<?> castSyncClass = Class.forName("org.telegram.messenger.CastSync");
                                Object castSyncInstance = castSyncClass.getMethod("getInstance").invoke(null);
                                castSyncClass.getMethod("seekTo", int.class).invoke(castSyncInstance, seekTo);
                            } catch (Exception e) {}
                        }
                    }

                    // Mute при активном CastSync
                    if (audioPlayer != null) {
                        try {
                            Class<?> castSyncClass = Class.forName("org.telegram.messenger.CastSync");
                            Object castSyncInstance = castSyncClass.getMethod("getInstance").invoke(null);
                            boolean isActive = (boolean) castSyncClass.getMethod("isActive").invoke(castSyncInstance);
                            if (isActive) {
                                audioPlayer.setMute(true);
                            }
                        } catch (Exception e) {}
                    }

                    // Окончание трека с логикой плейлиста (как в оригинале)
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
                                    if (noNext == null) noNext = true;
                                    controller.cleanupPlayer(true, noNext, isVoice, false);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onError(VideoPlayer player, Exception e) {
                    e.printStackTrace();
                    AndroidUtilities.runOnUIThread(() -> controller.cleanupPlayer(true, true));
                }

                @Override public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {}
                @Override public void onRenderedFirstFrame() {}
                @Override public void onRenderedFirstFrame(AnalyticsListener.EventTime eventTime) {}
                @Override public void onSeekFinished(AnalyticsListener.EventTime eventTime) {}
                @Override public void onSeekStarted(AnalyticsListener.EventTime eventTime) {}
                @Override public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surfaceTexture) {}
                @Override public boolean onSurfaceDestroyed(android.graphics.SurfaceTexture surfaceTexture) { return false; }
            });

            // 5. Подготовка плеера с URL
            Uri uri = Uri.parse(messageObject.messageOwner.attachPath);
            audioPlayer.preparePlayer(uri, "other");
            audioPlayer.setStreamType(AudioManager.STREAM_MUSIC);

            // 6. Вызов приватных методов (checkAudioFocus, setPlayerVolume, startProgressTimer)
            HookUtils.invokePrivateMethod(controller, "checkAudioFocus", new Class[]{MessageObject.class, boolean.class}, messageObject, true);
            HookUtils.invokePrivateMethod(controller, "setPlayerVolume", new Class[]{});
            HookUtils.invokePrivateMethod(controller, "startProgressTimer", new Class[]{MessageObject.class}, messageObject);

            // 7. Анимация громкости (как в оригинале для музыки)
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

            // 8. Запуск воспроизведения
            audioPlayer.play();

            // 9. Уведомление о старте
            NotificationCenter.getInstance(messageObject.currentAccount)
                    .postNotificationName(NotificationCenter.messagePlayingDidStart, messageObject, null);

            // 10. Управление MusicPlayerService (как в оригинале: старт или стоп)
            Boolean canStart = (Boolean) HookUtils.invokePrivateMethodWithReturn(controller, "canStartMusicPlayerService", new Class[]{});
            android.content.Intent intent = new android.content.Intent(ApplicationLoader.applicationContext, MusicPlayerService.class);
            if (canStart != null && canStart) {
                ApplicationLoader.applicationContext.startService(intent);
            } else {
                ApplicationLoader.applicationContext.stopService(intent);
            }

            // 11. CastSync и Chromecast (как в оригинале)
            try {
                Class<?> castSyncClass = Class.forName("org.telegram.messenger.CastSync");
                Object castSyncInstance = castSyncClass.getMethod("getInstance").invoke(null);
                castSyncClass.getMethod("check", int.class).invoke(castSyncInstance, 1); // TYPE_MUSIC = 1

                boolean ignorePlayerUpdate = (boolean) HookUtils.getPrivateField(controller, "ignorePlayerUpdate");
                if (!ignorePlayerUpdate) {
                    // ChromecastController
                    try {
                        Class<?> chromecastClass = Class.forName("org.telegram.messenger.ChromecastController");
                        Object chromecastInstance = chromecastClass.getMethod("getInstance").invoke(null);
                        boolean isCasting = (boolean) chromecastClass.getMethod("isCasting").invoke(chromecastInstance);
                        if (isCasting) {
                            Object currentMedia = HookUtils.invokePrivateMethodWithReturn(controller, "getCurrentChromecastMedia", new Class[]{});
                            chromecastClass.getMethod("setCurrentMediaAndCastIfNeeded", Object.class).invoke(chromecastInstance, currentMedia);
                        }
                    } catch (Exception e) {}
                    castSyncClass.getMethod("setPlaying", boolean.class).invoke(castSyncInstance, true);
                }
            } catch (Exception e) {}

            // 12. Сенсор приближения (опционально)
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
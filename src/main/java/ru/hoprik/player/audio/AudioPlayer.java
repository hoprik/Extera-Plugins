package ru.hoprik.player.audio;

import android.util.Log;
import org.telegram.messenger.*;
import org.telegram.tgnet.ConnectionsManager;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.holder.AudioSource;
import ru.hoprik.player.audio.objects.Track;
import ru.hoprik.player.hooks.HookUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AudioPlayer implements NotificationCenter.NotificationCenterDelegate {
    private AudioElement playingElement;
    private boolean playing;
    private float progress;

    public AudioPlayer() {
        this.playing = false;
        load();
    }

    public void play(AudioElement element) {
        playingElement = element;
        MediaController.getInstance().playMessage(element.getAudio(), false);
    }

    public void play(MessageObject messageObject) {
        play(new AudioElement(AudioSource.ofMessage(messageObject), AudioUtils.getTrackByMessageObject(messageObject)));
    }

    public void play(File file) {
        play(new AudioElement(AudioSource.ofFile(file), AudioUtils.getTrackByFile(file)));
    }

    public void play(String url, Track track) {
        play(new AudioElement(AudioSource.ofUrl(url), track));
    }

    public void playPlaylist(List<AudioElement> element) {
        if (element.isEmpty()) return;
        ArrayList<MessageObject> messages = element.stream().map(AudioElement::getAudio).collect(Collectors.toCollection(ArrayList::new));
        MediaController.getInstance().setPlaylist(messages, messages.get(0), -1);
    }

    public static void setupPlaylist(MediaController controller,
                                     ArrayList<MessageObject> messageObjects,
                                     MessageObject current,
                                     long mergeDialogId,
                                     boolean loadMusic,
                                     Object params) { // params – PlaylistGlobalSearchParams (или null)
        try {
            // 1. Получаем приватные поля
            ArrayList<MessageObject> playlist = (ArrayList<MessageObject>) HookUtils.getPrivateField(controller, "playlist");
            java.util.Map<Integer, MessageObject> playlistMap = (java.util.Map<Integer, MessageObject>) HookUtils.getPrivateField(controller, "playlistMap");
            if (playlist == null) return;

            // 2. Сохраняем старые флаги и очищаем
            boolean oldPlayMusicAgain = (boolean) HookUtils.getPrivateField(controller, "playMusicAgain");
            HookUtils.setPrivateField(controller, "playMusicAgain", !playlist.isEmpty());
            HookUtils.invokePrivateMethod(controller, "clearPlaylist", new Class[]{});

            // 3. Устанавливаем параметры плейлиста
            HookUtils.setPrivateField(controller, "forceLoopCurrentPlaylist", !loadMusic);
            HookUtils.setPrivateField(controller, "playlistMergeDialogId", mergeDialogId);
            HookUtils.setPrivateField(controller, "playlistGlobalSearchParams", params);

            boolean isSecretChat = !messageObjects.isEmpty() && DialogObject.isEncryptedDialog(messageObjects.get(0).getDialogId());
            int minId = Integer.MAX_VALUE, maxId = Integer.MIN_VALUE;

            // 4. Добавляем только музыкальные сообщения, НО без дублирования по ID
            java.util.HashSet<Integer> addedIds = new java.util.HashSet<>();
            for (MessageObject mo : messageObjects) {
                if (mo.isMusic()) {
                    int id = mo.getId();
                    if (!addedIds.contains(id) && (id > 0 || isSecretChat)) {
                        addedIds.add(id);
                        playlist.add(mo);
                        if (playlistMap != null) playlistMap.put(id, mo);
                        if (id < minId) minId = id;
                        if (id > maxId) maxId = id;
                    }
                }
            }

            // 5. Сортируем плейлист (по оригинальному методу sortPlaylist)
            HookUtils.invokePrivateMethod(controller, "sortPlaylist", new Class[]{});

            // 6. Определяем позицию текущего трека
            int currentPlaylistNum = playlist.indexOf(current);
            if (currentPlaylistNum == -1) {
                // Если current не найден – добавляем его один раз
                playlist.add(current);
                if (playlistMap != null) playlistMap.put(current.getId(), current);
                currentPlaylistNum = playlist.size() - 1;
            }
            HookUtils.setPrivateField(controller, "currentPlaylistNum", currentPlaylistNum);

            // 7. Если это музыка и не отложенное сообщение
            if (current.isMusic() && !current.scheduled) {
                if (SharedConfig.shuffleMusic) {
                    HookUtils.invokePrivateMethod(controller, "buildShuffledPlayList", new Class[]{});
                }
                if (params == null) {
                    MediaDataController.getInstance(current.currentAccount)
                            .loadMusic(current.getDialogId(), minId, maxId);
                } else {
                    HookUtils.setPrivateField(controller, "playlistClassGuid", ConnectionsManager.generateClassGuid());
                }
            }

            // 8. Запускаем воспроизведение (оригинальный метод publish)
            controller.playMessage(current, false);

        } catch (Exception e) {
            Log.e("HookUtils", "setupPlaylist error", e);
        }
    }

    public void stop() {
        MediaController.getInstance().stopMediaObserver();
    }

    public void pause() {
        MediaController.getInstance().pauseMessage(playingElement.getAudio());
    }

    public void next() {
        MediaController.getInstance().playNextMessage();
    }

    public void prev() {
        MediaController.getInstance().playPreviousMessage();
    }

    public void seek(float position) {
        if (playingElement == null) return;
        MediaController.getInstance().seekToProgress(playingElement.getAudio(), position);
    }

    public float getProgress() {
        return progress;
    }

    public void updateTrack() {

    }

    public void setTrack(Track track) {

    }

    public void playlist() {

    }

    public AudioElement getAudioElement() {
        return playingElement;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isPlaying() {
        return playing;
    }

    private void load() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.musicDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.moreMusicDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.musicIdsLoaded);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.messagePlayingSpeedChanged);
    }

    public void destroy() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.musicDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.moreMusicDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.musicIdsLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingSpeedChanged);
    }

    public void updatePlayingMessage() {
        updatePlayingMessage(MediaController.getInstance().getPlayingMessageObject());
    }

    private void updatePlayingMessage(MessageObject playingMessage) {
        if (playingMessage == null) {
            return;
        }
        String nameTrack = playingMessage.getMusicTitle();
        if (playingElement == null || !playingElement.getTrack().getName().equals(nameTrack)) {
            playingElement = new AudioElement(AudioSource.ofMessage(playingMessage), AudioUtils.getTrackByMessageObject(playingMessage));
        }
        if (playingElement.getTrack().isWeb() || playingElement.getTrack().isLocal()) {
            return;
        }


    }

    private void updateProgress(float progress) {
        if (playingElement == null) return;
        float progressInSec = convertProgress(progress, playingElement);
        this.progress = progressInSec;
        this.playingElement.getTrack().setProgress(progressInSec);
    }

    public static float convertProgress(float progress, AudioElement element) {
        return progress * element.getAudio().audioPlayerDuration;
    }

    @Override
    public void didReceivedNotification(int i, int i1, Object... objects) {
        if (i == NotificationCenter.messagePlayingDidStart) {
            updatePlayingMessage();
        }
        if (i == NotificationCenter.messagePlayingProgressDidChanged) {
            updateProgress((Float) objects[1] / 1000);
        }
    }
}

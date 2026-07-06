package ru.hoprik.player.audio;

import android.util.Log;
import j$.util.U;
import org.telegram.messenger.*;
import org.telegram.tgnet.ConnectionsManager;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.holder.AudioSource;
import ru.hoprik.player.audio.holder.Playlist;
import ru.hoprik.player.audio.objects.Track;
import ru.hoprik.player.helpers.NotificationCenterCodes;
import ru.hoprik.player.hooks.HookUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AudioPlayer implements NotificationCenter.NotificationCenterDelegate {
    private AudioElement playingElement;
    private Playlist playlist;
    private Playlist previousTracks;
    private float progress;

    public AudioPlayer() {
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

    public void playPlaylist(String playlistName, List<AudioElement> element) {
        if (element.isEmpty()) return;
        playPlaylist(new Playlist(playlistName, element));
    }

    public void playPlaylist(Playlist playlist) {
        this.playlist = playlist;
        playlist.play();
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

    public AudioElement getAudioElement() {
        return playingElement;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void loadMoreMusic() {
        MediaController.getInstance().loadMoreMusic();
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
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenterCodes.updateMediaPlaylist);
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
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenterCodes.updateMediaPlaylist);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingSpeedChanged);
    }

    public void updatePlayingMessage() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenterCodes.updateMediaPlaylist);
        updatePlayingMessage(MediaController.getInstance().getPlayingMessageObject());
    }

    private void updatePlayingMessage(MessageObject playingMessage) {
        if (playingMessage == null) {
            return;
        }
        if (!playingMessage.isMusic()) {
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

    private boolean arePlaylistsEqual(ArrayList<MessageObject> source, ArrayList<MessageObject> playlist) {
        // 1. Быстрый фильтр по размеру
        int musicCount = 0;
        for (MessageObject m : source) if (m.isMusic()) musicCount++;

        if (musicCount != playlist.size()) return false;

        // 2. Только если размеры совпали, делаем глубокую проверку
        return compareIds(source, playlist);
    }

    private boolean compareIds(ArrayList<MessageObject> sourceList, ArrayList<MessageObject> playlist) {
        Set<Integer> sourceIds = new HashSet<>();
        for (MessageObject msg : sourceList) {
            if (msg.isMusic()) {
                sourceIds.add(msg.getId());
            }
        }
        Set<Integer> playlistIds = new HashSet<>();
        for (MessageObject msg : playlist) {
            playlistIds.add(msg.getId());
        }
        return sourceIds.equals(playlistIds);
    }

    private void updatePlaylist() {
        Log.d("AudioPlayer_Update", "=== Вызов updatePlaylist() ===");

        ArrayList<MessageObject> tgPlaylist = MediaController.getInstance().getPlaylist();

        if (tgPlaylist == null) {
            Log.d("AudioPlayer_Update", "tgPlaylist равен NULL. Прерываем выполнение (return).");
            return;
        }

        if (tgPlaylist.isEmpty()) {
            Log.d("AudioPlayer_Update", "tgPlaylist ПУСТОЙ (size = 0). Прерываем выполнение (return).");
            return;
        }

        Log.d("AudioPlayer_Update", "Размер tgPlaylist от MediaController: " + tgPlaylist.size());

        boolean needsUpdate = false;

        if (this.playlist == null) {
            Log.d("AudioPlayer_Update", "Локальный this.playlist равен NULL. Требуется первичное создание.");
            needsUpdate = true;
        } else {
            ArrayList<MessageObject> localMessages = (ArrayList<MessageObject>) this.playlist.getMessageObjects();
            Log.d("AudioPlayer_Update", "Размер текущего локального this.playlist: " + (localMessages != null ? localMessages.size() : "null"));

            boolean areEqual = arePlaylistsEqual(tgPlaylist, localMessages);
            Log.d("AudioPlayer_Update", "Результат arePlaylistsEqual: " + areEqual);

            needsUpdate = !areEqual;
        }

        if (needsUpdate) {
            Log.d("AudioPlayer_Update", "Начинаем конвертацию Playlist.convertToPlaylist...");

            this.playlist = Playlist.convertToPlaylist(tgPlaylist);

            Log.d("AudioPlayer_Update", "Конвертация завершена. Новый размер this.playlist: " +
                    (this.playlist != null && this.playlist.getElements() != null ? this.playlist.getElements().size() : "null"));

            Log.d("AudioPlayer_Update", "Отправляем ивент updateMediaPlaylistUI в NotificationCenter...");
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenterCodes.updateMediaPlaylistUI);
        } else {
            Log.d("AudioPlayer_Update", "Плейлисты идентичны. Обновление UI и конвертация пропущены.");
        }

        Log.d("AudioPlayer_Update", "=== updatePlaylist() завершил работу ===");
    }

    @Override
    public void didReceivedNotification(int i, int i1, Object... objects) {
        // Добавили musicDidLoad и moreMusicDidLoad
        if (i == NotificationCenter.messagePlayingDidStart ||
                i == NotificationCenter.messagePlayingDidReset ||
                i == NotificationCenter.messagePlayingPlayStateChanged ||
                i == NotificationCenter.musicDidLoad ||
                i == NotificationCenter.musicIdsLoaded ||
                i == NotificationCenter.moreMusicDidLoad) {

            updatePlayingMessage();
            updatePlaylist(); // Форсируем обновление плейлиста, когда телега догрузила треки
        }

        if (i == NotificationCenter.messagePlayingProgressDidChanged) {
            Object progressObj = objects[1];
            float normalizedProgress = 0f;
            if (progressObj instanceof Float) {
                normalizedProgress = (Float) progressObj;
            } else if (progressObj instanceof Integer) {
                normalizedProgress = ((Integer) progressObj).floatValue();
            } else if (progressObj instanceof Long) {
                normalizedProgress = ((Long) progressObj).floatValue();
            } else {
                android.util.Log.e("AudioPlayer", "Unexpected progress type: " + progressObj.getClass().getName());
            }
            updateProgress(normalizedProgress);
        }

        if (i == NotificationCenterCodes.updateMediaPlaylist) {
            updatePlaylist();
        }
    }
}

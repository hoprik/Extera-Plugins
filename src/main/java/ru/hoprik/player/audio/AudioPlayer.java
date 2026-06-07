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
    private List<AudioElement> playlist;
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

    public void playPlaylist(List<AudioElement> element) {
        if (element.isEmpty()) return;
        this.playlist = element;
        ArrayList<MessageObject> messages = element.stream().map(AudioElement::getAudio).collect(Collectors.toCollection(ArrayList::new));
        MediaController.getInstance().setPlaylist(messages, messages.get(0), -1);
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
    }
}

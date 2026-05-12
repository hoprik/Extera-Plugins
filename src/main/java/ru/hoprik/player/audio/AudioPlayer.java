package ru.hoprik.player.audio;

import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.holder.AudioSource;
import ru.hoprik.player.audio.objects.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AudioPlayer implements NotificationCenter.NotificationCenterDelegate{
    private AudioElement playingElement;
    private boolean playing;
    private int progress;

    public AudioPlayer() {
        this.playing = false;
    }

    public void play(AudioElement element) {
        playingElement = element;
        MediaController.getInstance().playMessage(element.getAudio(), false);
    }

    public void play(MessageObject messageObject) {
        play(new AudioElement(AudioSource.ofMessage(messageObject), AudioUtils.getTrackByMessageObject(messageObject)));
    }

    public void play(File file){
        play(new AudioElement(AudioSource.ofFile(file), AudioUtils.getTrackByFile(file)));
    }

    public void play(String url, Track track) {
        play(new AudioElement(AudioSource.ofUrl(url), track));
    }

    public void playPlaylist(List<AudioElement> element){
        if(element.isEmpty()) return;
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

    public void seek(int position) {
        this.progress = position;
    }

    public int getProgress() {
        return progress;
    }

    public void updateTrack(){

    }

    public void setTrack(Track track) {

    }

    public void playlist(){

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

    public void load(){
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

    }

    @Override
    public void didReceivedNotification(int i, int i1, Object... objects) {
        if (i == NotificationCenter.messagePlayingProgressDidChanged) {
        }
    }
}

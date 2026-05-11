package ru.hoprik.player.audio.holder;

import org.telegram.messenger.MessageObject;
import ru.hoprik.player.audio.AudioUtils;
import ru.hoprik.player.audio.objects.Track;

public class AudioElement {
    private MessageObject audio;
    private AudioSource audioSource;
    private Track track;

    public AudioElement(AudioSource audioSource, Track track) {
        this.audioSource = audioSource;
        this.track = track;
        setupAudio();
    }

    public MessageObject getAudio() {
        return audio;
    }

    public void setupAudio() {
        switch (audioSource.getType()){
            case MESSAGE:
                audio = audioSource.getMessage();
                break;
            case FILE:
                audio = AudioUtils.getMessageObjectByFile(audioSource.getFile());
                break;
            case URL:
                audio = AudioUtils.getMessageObjectByUrl(audioSource.getUrl(), track);
                break;
        }
    }

    public AudioSource getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }
}

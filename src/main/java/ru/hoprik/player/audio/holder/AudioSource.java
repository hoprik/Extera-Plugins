package ru.hoprik.player.audio.holder;

import org.telegram.messenger.MessageObject;

import java.io.File;
import java.net.URL;

public class AudioSource {
    public enum Type { MESSAGE, URL, FILE, ID }
    private final Type type;
    private final Object value;

    private AudioSource(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static AudioSource ofMessage(MessageObject msg) {
        return new AudioSource(Type.MESSAGE, msg);
    }

    public static AudioSource ofUrl(String url) {
        return new AudioSource(Type.URL, url);
    }

    public static AudioSource ofFile(File file) {
        return new AudioSource(Type.FILE, file);
    }

    public static AudioSource ofID(TrackID id) {return new AudioSource(Type.ID, id);}

    public Type getType() { return type; }

    public MessageObject getMessage() {
        if (type != Type.MESSAGE) throw new IllegalStateException();
        return (MessageObject) value;
    }

    public String getUrl() {
        if (type != Type.URL) throw new IllegalStateException();
        return (String) value;
    }
    public File getFile() {
        if (type != Type.FILE) throw new IllegalStateException();
        return (File) value;
    }
    public TrackID getTrackID(){
        if (type != Type.ID) throw new IllegalStateException();
        return (TrackID) value;
    }
}
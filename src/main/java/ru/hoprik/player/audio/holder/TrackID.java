package ru.hoprik.player.audio.holder;

public class TrackID {
    public final String id;
    public final String providerName;

    public TrackID(String id, String providerName) {
        this.id = id;
        this.providerName = providerName;
    }

    public String getId() {
        return id;
    }

    public String getProviderName() {
        return providerName;
    }
}

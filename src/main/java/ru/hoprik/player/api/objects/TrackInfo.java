package ru.hoprik.player.api.objects;

public class TrackInfo {
    private String id;
    private String title;
    private String artist;
    private ReleaseInfo releaseInfo;

    public TrackInfo(String id, String title, String artist, ReleaseInfo releaseInfo) {
        this.title = title;
        this.artist = artist;
        this.releaseInfo = releaseInfo;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }
}

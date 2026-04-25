package ru.hoprik.player.api.objects;

import java.util.List;

public class TrackInfo {
    private String id;
    private String title;
    private List<ArtistInfo> artist;
    private ReleaseInfo releaseInfo;

    public TrackInfo(String id, String title, List<ArtistInfo> artist, ReleaseInfo releaseInfo) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.releaseInfo = releaseInfo;
    }

    public String getTitle() {
        return title;
    }

    public List<ArtistInfo> getArtist() {
        return artist;
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(List<ArtistInfo> artist) {
        this.artist = artist;
    }

    public void setReleaseInfo(ReleaseInfo releaseInfo) {
        this.releaseInfo = releaseInfo;
    }
}

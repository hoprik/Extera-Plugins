package ru.hoprik.player.api.objects;

import java.util.List;

public class ReleaseInfo {
    private String id;
    private String title;
    private List<ArtistInfo> artist;
    private long releaseDate;
    private String coverUrl;
    private List<TrackInfo> tracks;

    public ReleaseInfo(String id, String title, List<ArtistInfo> artist, long releaseDate, String coverUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.releaseDate = releaseDate;
        this.coverUrl = coverUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<ArtistInfo> getArtist() {
        return artist;
    }

    public long getReleaseDate() {
        return releaseDate;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public List<TrackInfo> getTracks() {
        return tracks;
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

    public void setReleaseDate(long releaseDate) {
        this.releaseDate = releaseDate;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setTracks(List<TrackInfo> tracks) {
        this.tracks = tracks;
    }
}

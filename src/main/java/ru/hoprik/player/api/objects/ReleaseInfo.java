package ru.hoprik.player.api.objects;

import java.util.List;

public class ReleaseInfo {
    private final String id;
    private final String title;
    private final String artist;
    private final String releaseDate;
    private final String coverUrl;
    private List<TrackInfo> tracks;

    public ReleaseInfo(String id, String title, String artist, String releaseDate, String coverUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.releaseDate = releaseDate;
        this.coverUrl = coverUrl;
    }

    private void setTracks(List<TrackInfo> tracks) {
        this.tracks = tracks;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public List<TrackInfo> getTracks() {
        return tracks;
    }
}

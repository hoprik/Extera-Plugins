package ru.hoprik.player.audio.objects;

import java.util.List;

public class Release {
    private String id;
    private String title;
    private List<Artist> artists;
    private long releaseDate;
    private Cover cover;
    private List<Track> tracks;

    public Release(String id, String title, List<Artist> artists, long releaseDate, Cover cover, List<Track> tracks) {
        this.id = id;
        this.title = title;
        this.artists = artists;
        this.releaseDate = releaseDate;
        this.cover = cover;
        this.tracks = tracks;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public long getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(long releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }
}
package ru.hoprik.player.audio;

import java.util.List;

public class Track {
    private String id;
    private String name;
    private List<Artist> artists;
    private int duration;
    private int progress;
    private Cover cover;
    private Release release;

    public Track(String id, String name, List<Artist> artists, int duration, int progress, Cover cover, Release release) {
        this.id = id;
        this.name = name;
        this.artists = artists;
        this.duration = duration;
        this.progress = progress;
        this.cover = cover;
        this.release = release;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }
}

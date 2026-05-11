package ru.hoprik.player.audio;

import java.util.List;

public class Artist {
    private String id;
    private String name;
    private Cover cover;
    private List<Track> popularTracks;
    private List<Release> popularReleases;

    public Artist(String id, String name, Cover cover, List<Track> popularTracks, List<Release> popularReleases) {
        this.id = id;
        this.name = name;
        this.cover = cover;
        this.popularTracks = popularTracks;
        this.popularReleases = popularReleases;
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

    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    public List<Track> getPopularTracks() {
        return popularTracks;
    }

    public void setPopularTracks(List<Track> popularTracks) {
        this.popularTracks = popularTracks;
    }

    public List<Release> getPopularReleases() {
        return popularReleases;
    }

    public void setPopularReleases(List<Release> popularReleases) {
        this.popularReleases = popularReleases;
    }
}

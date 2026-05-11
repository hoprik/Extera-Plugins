package ru.hoprik.player.audio.objects;

import java.util.List;

public class Artist {
    private String id;
    private String name;
    private Cover cover;
    private List<Track> popularTracks;
    private List<Release> popularReleases;
    private boolean isWeb;
    private boolean local;

    public Artist(String id, String name, Cover cover, List<Track> popularTracks, List<Release> popularReleases, boolean isWeb, boolean local) {
        this.id = id;
        this.name = name;
        this.cover = cover;
        this.popularTracks = popularTracks;
        this.popularReleases = popularReleases;
        this.isWeb = isWeb;
        this.local = local;
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

    public boolean isWeb() {
        return isWeb;
    }

    public void setWeb(boolean web) {
        isWeb = web;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
}

package ru.hoprik.player.api.objects;

import java.util.List;

public class ArtistInfo {
    private String id;
    private String name;
    private String coverUrl;
    private List<TrackInfo> popularTracks;
    private List<ReleaseInfo> popularReleases;

    public ArtistInfo(String id, String name, String coverUrl, List<TrackInfo> popularTracks, List<ReleaseInfo> popularReleases) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.popularTracks = popularTracks;
        this.popularReleases = popularReleases;
    }

    public String getName() {
        return name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public List<TrackInfo> getPopularTracks() {
        return popularTracks;
    }

    public List<ReleaseInfo> getPopularReleases() {
        return popularReleases;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setPopularTracks(List<TrackInfo> popularTracks) {
        this.popularTracks = popularTracks;
    }

    public void setPopularReleases(List<ReleaseInfo> popularReleases) {
        this.popularReleases = popularReleases;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

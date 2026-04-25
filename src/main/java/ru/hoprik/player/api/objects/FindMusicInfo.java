package ru.hoprik.player.api.objects;

import ru.hoprik.player.api.providers.StatsFM;

import java.util.List;

public class FindMusicInfo {
    private List<TrackInfo> tracks;
    private List<ReleaseInfo> releaseInfos;
    private List<ArtistInfo> artists;

    public FindMusicInfo(List<TrackInfo> tracks, List<ReleaseInfo> releaseInfos, List<ArtistInfo> artists) {
        this.tracks = tracks;
        this.releaseInfos = releaseInfos;
        this.artists = artists;
    }

    public List<TrackInfo> getTracks() {
        return tracks;
    }

    public List<ReleaseInfo> getReleaseInfos() {
        return releaseInfos;
    }

    public List<ArtistInfo> getArtists() {
        return artists;
    }

    public void setTracks(List<TrackInfo> tracks) {
        this.tracks = tracks;
    }

    public void setReleaseInfos(List<ReleaseInfo> releaseInfos) {
        this.releaseInfos = releaseInfos;
    }

    public void setArtists(List<ArtistInfo> artists) {
        this.artists = artists;
    }
}

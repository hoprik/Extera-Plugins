package ru.hoprik.player.api.objects;

import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Release;
import ru.hoprik.player.audio.objects.Track;

import java.util.List;

public class FindMusicInfo {
    private List<Track> tracks;
    private List<Release> releaseInfos;
    private List<Artist> artists;

    public FindMusicInfo(List<Track> tracks, List<Release> releaseInfos, List<Artist> artists) {
        this.tracks = tracks;
        this.releaseInfos = releaseInfos;
        this.artists = artists;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Release> getReleaseInfos() {
        return releaseInfos;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public void setReleaseInfos(List<Release> releaseInfos) {
        this.releaseInfos = releaseInfos;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }
}

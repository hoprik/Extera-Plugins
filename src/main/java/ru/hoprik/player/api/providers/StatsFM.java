package ru.hoprik.player.api.providers;

import okhttp3.OkHttpClient;
import ru.hoprik.player.api.interfaces.*;
import ru.hoprik.player.api.interfaces.info.IArtistInfo;
import ru.hoprik.player.api.interfaces.info.IReleaseInfo;
import ru.hoprik.player.api.interfaces.info.ITrackInfo;
import ru.hoprik.player.api.objects.ArtistInfo;
import ru.hoprik.player.api.objects.ReleaseInfo;
import ru.hoprik.player.api.objects.TrackInfo;

import java.util.List;

public class StatsFM extends Provider implements ITrackInfo, IReleaseInfo, IArtistInfo {

    public StatsFM(OkHttpClient client) {
        super(client);
    }


    @Override
    public String getBaseUrl() {
        return "https://stats.fm/";
    }

    @Override
    public ICallback<TrackInfo> getTrackInfo(String name, List<String> artists) {
        return null;
    }

    @Override
    public ICallback<ArtistInfo> getArtistInfo(String name) {
        return null;
    }

    @Override
    public ICallback<ReleaseInfo> getReleaseInfo(String name, List<String> artists) {
        return null;
    }


    private static class StatsFMArtist{
        int id;
        String name;
        String image;
    }

    private static class StatsFMAlbum{
        int id;
        String name;
        String image;
    }

    private static class ExternalIds{
        List<String> spotify;
        List<String> appleMusic;
        String upc;
        String ean;
        String isrc;
    }

    private static class StatsFMTrack{
        List<StatsFMAlbum> albums;
        List<StatsFMArtist> artists;
        double durationMs;
        boolean explicit;
        ExternalIds externalIds;
        int id;
        name: string;
        spotifyPopularity: number;
        spotifyPreview: string | null;
        appleMusicPreview: string | null;
    }

    private static class ResFindStatsFM{

    }
}

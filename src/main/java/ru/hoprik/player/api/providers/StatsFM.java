package ru.hoprik.player.api.providers;

import android.util.Log;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import ru.hoprik.player.api.interfaces.*;
import ru.hoprik.player.api.interfaces.info.IArtistInfo;
import ru.hoprik.player.api.interfaces.info.IFindMusicInfo;
import ru.hoprik.player.api.interfaces.info.IReleaseInfo;
import ru.hoprik.player.api.interfaces.info.ITrackInfo;
import ru.hoprik.player.api.objects.ArtistInfo;
import ru.hoprik.player.api.objects.FindMusicInfo;
import ru.hoprik.player.api.objects.ReleaseInfo;
import ru.hoprik.player.api.objects.TrackInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class StatsFM extends Provider implements ITrackInfo, IReleaseInfo, IArtistInfo, IFindMusicInfo {
    private static final String TAG = "StatsFM";
    private static final String API_TRACKS = "api/v1/tracks/";
    private static final String API_ARTISTS = "api/v1/artists/";
    private static final String API_ALBUMS = "api/v1/albums/";
    private static final String API_SEARCH = "api/v1/search/elastic";
    private final Gson gson = new Gson();

    public StatsFM(OkHttpClient client) {
        super(client);
    }

    @Override
    public String getBaseUrl() {
        return "https://api.stats.fm/";
    }

    @Override
    public void getTrackInfo(int id, ICallback<TrackInfo> callback) {
        makeReq(API_TRACKS + id, "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResTrackInfo res = (ResTrackInfo) gson.fromJson(body, ResTrackInfo.class);
                        ReleaseInfo album = convertAlbums(res.items.albums).isEmpty() ? null : convertAlbums(res.items.albums).get(0);
                        callback.onSuccess(new TrackInfo(
                                String.valueOf(res.items.id),
                                res.items.name,
                                convertArtists(res.items.artists),
                                album
                        ));
                    } else {
                        callback.onError(new IOException("Failed to get track info: " + response.code()));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing track info", e);
                    callback.onError(e);
                }
            }
        });
    }

    @Override
    public void getArtistInfo(int id, ICallback<ArtistInfo> callback) {
        ArtistInfo info = new ArtistInfo(null, null, null, null, null);
        AtomicInteger remaining = new AtomicInteger(3);
        Runnable finish = () -> {
            if (info.getId() != null) {
                callback.onSuccess(info);
            } else {
                callback.onError(new IOException("Failed to fetch artist info"));
            }
        };

        makeReq(API_ARTISTS + id, "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (remaining.decrementAndGet() == 0) finish.run();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResArtistInfo res = (ResArtistInfo) gson.fromJson(body, ResArtistInfo.class);
                        info.setId(String.valueOf(res.items.id));
                        info.setName(res.items.name);
                        info.setCoverUrl(res.items.image);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing artist info", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });

        makeReq(API_ARTISTS + id + "/albums", "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (remaining.decrementAndGet() == 0) finish.run();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResTopArtistAlbums res = (ResTopArtistAlbums) gson.fromJson(body, ResTopArtistAlbums.class);
                        info.setPopularReleases(convertAlbums(res.items));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing artist albums", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });

        makeReq(API_ARTISTS + id + "/tracks", "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (remaining.decrementAndGet() == 0) finish.run();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResTopArtistTracks res = (ResTopArtistTracks) gson.fromJson(body, ResTopArtistTracks.class);
                        info.setPopularTracks(convertTracks(res.items));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing artist tracks", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });
    }

    @Override
    public void getReleaseInfo(int id, ICallback<ReleaseInfo> callback) {
        ReleaseInfo info = new ReleaseInfo(null, null, null, 0, null);
        AtomicInteger remaining = new AtomicInteger(2);
        Runnable finish = () -> {
            if (info.getId() != null) {
                callback.onSuccess(info);
            } else {
                callback.onError(new IOException("Failed to fetch release info"));
            }
        };

        makeReq(API_ALBUMS + id, "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (remaining.decrementAndGet() == 0) finish.run();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResReleaseInfo res = (ResReleaseInfo) gson.fromJson(body, ResReleaseInfo.class);
                        info.setId(String.valueOf(res.items.id));
                        info.setTitle(res.items.name);
                        info.setCoverUrl(res.items.image);
                        info.setReleaseDate(res.items.releaseDate);
                        info.setArtist(convertArtists(res.items.artists));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing release info", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });

        makeReq(API_ALBUMS + id + "/tracks", "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (remaining.decrementAndGet() == 0) finish.run();
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResReleaseTracks res = (ResReleaseTracks) gson.fromJson(body, ResReleaseTracks.class);
                        info.setTracks(convertTracks(res.items));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing release tracks", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });
    }

    @Override
    public void findMusicInfo(String query, ICallback<FindMusicInfo> callback) {
        makeReq(API_SEARCH + "?query=" + query + "&type=album,artist,track,user&limit=50", "GET", null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        ResFindStatsFM res = (ResFindStatsFM) gson.fromJson(body, ResFindStatsFM.class);
                        callback.onSuccess(new FindMusicInfo(
                                convertTracks(res.items.tracks),
                                convertAlbums(res.items.albums),
                                convertArtists(res.items.artists)
                        ));
                    } else {
                        callback.onError(new IOException("Search failed: " + response.code()));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing search result", e);
                    callback.onError(e);
                }
            }
        });
    }

    private java.util.List<TrackInfo> convertTracks(java.util.List<StatsFMTrack> tracks) {
        return tracks.stream()
                .map(t -> {
                    java.util.List<ReleaseInfo> albums = convertAlbums(t.albums);
                    return new TrackInfo(
                            String.valueOf(t.id),
                            t.name,
                            convertArtists(t.artists),
                            albums.isEmpty() ? null : albums.get(0)
                    );
                })
                .collect(Collectors.toList());
    }

    private java.util.List<ArtistInfo> convertArtists(java.util.List<StatsFMArtist> artists) {
        return artists.stream()
                .map(a -> new ArtistInfo(String.valueOf(a.id), a.name, a.image, null, null))
                .collect(Collectors.toList());
    }

    private java.util.List<ReleaseInfo> convertAlbums(java.util.List<StatsFMAlbum> albums) {
        return albums.stream()
                .map(a -> new ReleaseInfo(String.valueOf(a.id), a.name, null, 0, a.image))
                .collect(Collectors.toList());
    }

    private static class StatsFMArtist {
        int id;
        String name;
        String image;
    }

    private static class StatsFMAlbum {
        int id;
        String name;
        String image;
    }

    private static class ExternalIds {
        java.util.List<String> spotify;
        java.util.List<String> appleMusic;
        String upc;
        String ean;
        String isrc;
    }

    private static class StatsFMTrack {
        java.util.List<StatsFMAlbum> albums;
        java.util.List<StatsFMArtist> artists;
        double durationMs;
        boolean explicit;
        ExternalIds externalIds;
        int id;
        String name;
        int spotifyPopularity;
        String spotifyPreview;
        String appleMusicPreview;
    }

    private static class StatsFMItem {
        java.util.List<StatsFMTrack> tracks;
        java.util.List<StatsFMAlbum> albums;
        java.util.List<StatsFMArtist> artists;
    }

    private static class StatsFmReleaseFull {
        String name;
        String image;
        String label;
        int spotifyPopularity;
        int totalTracks;
        long releaseDate;
        java.util.List<String> genres;
        java.util.List<StatsFMArtist> artists;
        ExternalIds externalIds;
        String type;
        int id;
    }

    private static class StatsFmArtistFull {
        ExternalIds externalIds;
        int followers;
        java.util.List<String> genres;
        int id;
        String image;
        String name;
        int spotifyPopularity;
    }

    private static class ResTrackInfo {
        StatsFMTrack items;
    }

    private static class ResReleaseInfo {
        StatsFmReleaseFull items;
    }

    private static class ResReleaseTracks {
        java.util.List<StatsFMTrack> items;
    }

    private static class ResArtistInfo {
        StatsFmArtistFull items;
    }

    private static class ResFindStatsFM {
        StatsFMItem items;
    }

    private static class ResTopArtistTracks {
        java.util.List<StatsFMTrack> items;
    }

    private static class ResTopArtistAlbums {
        java.util.List<StatsFMAlbum> items;
    }
}
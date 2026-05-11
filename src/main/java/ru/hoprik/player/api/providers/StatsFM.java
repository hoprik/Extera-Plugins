package ru.hoprik.player.api.providers;

import android.util.Log;
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegLibrary;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.interfaces.Provider;
import ru.hoprik.player.api.interfaces.info.IArtistInfo;
import ru.hoprik.player.api.interfaces.info.IFindMusicInfo;
import ru.hoprik.player.api.interfaces.info.IReleaseInfo;
import ru.hoprik.player.api.interfaces.info.ITrackInfo;
import ru.hoprik.player.api.objects.FindMusicInfo;
import ru.hoprik.player.audio.*;

import java.io.IOException;
import java.util.List;
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

    // --------------------------------------------------------------
    // 1. Получение информации о треке -> Track
    // --------------------------------------------------------------
    @Override
    public void getTrackInfo(int id, ICallback<Track> callback) {
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
                        Track track = mapToTrack(res.items);
                        callback.onSuccess(track);
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

    // --------------------------------------------------------------
    // 2. Получение информации об артисте -> Artist
    // --------------------------------------------------------------
    @Override
    public void getArtistInfo(int id, ICallback<Artist> callback) {
        ArtistInfoHolder holder = new ArtistInfoHolder();
        AtomicInteger remaining = new AtomicInteger(3);
        Runnable finish = () -> {
            if (holder.id != null) {
                Artist artist = new Artist(
                        holder.id,
                        holder.name,
                        holder.cover != null ? new Cover(holder.cover) : null,
                        holder.popularTracks,
                        holder.popularReleases
                );
                callback.onSuccess(artist);
            } else {
                callback.onError(new IOException("Failed to fetch artist info"));
            }
        };

        // Запрос основного info
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
                        holder.id = String.valueOf(res.items.id);
                        holder.name = res.items.name;
                        holder.cover = res.items.image;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing artist info", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });

        // Запрос альбомов артиста
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
                        holder.popularReleases = mapToReleasesSimple(res.items);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing artist albums", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }

        });

        // Запрос треков артиста
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
                        holder.popularTracks = mapToTracks(res.items);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing artist tracks", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });
    }

    // --------------------------------------------------------------
    // 3. Получение информации о релизе (альбоме) -> Release
    // --------------------------------------------------------------
    @Override
    public void getReleaseInfo(int id, ICallback<Release> callback) {
        ReleaseInfoHolder holder = new ReleaseInfoHolder();
        AtomicInteger remaining = new AtomicInteger(2);
        Runnable finish = () -> {
            if (holder.id != null) {
                Release release = new Release(
                        holder.id,
                        holder.title,
                        holder.artists,
                        holder.releaseDate,
                        holder.cover != null ? new Cover(holder.cover) : null,
                        holder.tracks
                );
                callback.onSuccess(release);
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
                        holder.id = String.valueOf(res.items.id);
                        holder.title = res.items.name;
                        holder.cover = res.items.image;
                        holder.releaseDate = res.items.releaseDate;
                        holder.artists = mapToArtistsSimple(res.items.artists);
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
                        holder.tracks = mapToTracks(res.items);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing release tracks", e);
                } finally {
                    if (remaining.decrementAndGet() == 0) finish.run();
                }
            }
        });
    }

    // --------------------------------------------------------------
    // 4. Поиск -> MusicSearchResult
    // --------------------------------------------------------------
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
                        List<Track> tracks = mapToTracks(res.items.tracks);
                        List<Release> releases = mapToReleasesSimple(res.items.albums);
                        List<Artist> artists = mapToArtistsSimple(res.items.artists);
                        callback.onSuccess(new FindMusicInfo(tracks, releases, artists));
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

    // --------------------------------------------------------------
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ МАППИНГА
    // --------------------------------------------------------------

    private Track mapToTrack(StatsFMTrack raw) {
        String id = String.valueOf(raw.id);
        String name = raw.name;
        // Конвертируем артистов
        List<Artist> artists = raw.artists.stream()
                .map(this::mapToArtistSimple)
                .collect(Collectors.toList());
        int durationMs = (int) raw.durationMs;
        // Берём обложку из первого альбома (если есть)
        Cover cover = null;
        if (raw.albums != null && !raw.albums.isEmpty()) {
            cover = new Cover(raw.albums.get(0).image);
        }
        // Release пока не заполняем (можно позже через getReleaseInfo)
        return new Track(id, name, artists, durationMs, 0, cover, null);
    }

    private Artist mapToArtistSimple(StatsFMArtist raw) {
        String id = String.valueOf(raw.id);
        String name = raw.name;
        Cover cover = raw.image != null ? new Cover(raw.image) : null;
        // Без популярных треков и альбомов (заполняются отдельно)
        return new Artist(id, name, cover, null, null);
    }

    private Artist mapToArtistSimple(StatsFmArtistFull raw) {
        String id = String.valueOf(raw.id);
        String name = raw.name;
        Cover cover = raw.image != null ? new Cover(raw.image) : null;
        return new Artist(id, name, cover, null, null);
    }

    private List<Artist> mapToArtistsSimple(List<StatsFMArtist> rawList) {
        if (rawList == null) return List.of();
        return rawList.stream()
                .map(this::mapToArtistSimple)
                .collect(Collectors.toList());
    }

    private List<Artist> mapToArtistsSimpleFromFull(List<StatsFmArtistFull> rawList) {
        if (rawList == null) return List.of();
        return rawList.stream()
                .map(this::mapToArtistSimple)
                .collect(Collectors.toList());
    }

    private Release mapToReleaseSimple(StatsFMAlbum rawAlbum) {
        // Простой релиз без треков и артистов (только id, название, обложка)
        String id = String.valueOf(rawAlbum.id);
        String title = rawAlbum.name;
        Cover cover = rawAlbum.image != null ? new Cover(rawAlbum.image) : null;
        return new Release(id, title, null, 0, cover, null);
    }

    private List<Release> mapToReleasesSimple(List<StatsFMAlbum> rawAlbums) {
        if (rawAlbums == null) return List.of();
        return rawAlbums.stream()
                .map(this::mapToReleaseSimple)
                .collect(Collectors.toList());
    }

    private List<Track> mapToTracks(List<StatsFMTrack> rawTracks) {
        if (rawTracks == null) return List.of();
        return rawTracks.stream()
                .map(this::mapToTrack)
                .collect(Collectors.toList());
    }

    // --------------------------------------------------------------
    // ВНУТРЕННИЕ КЛАССЫ ДЛЯ ХРАНЕНИЯ ПРОМЕЖУТОЧНЫХ ДАННЫХ
    // --------------------------------------------------------------

    private static class ArtistInfoHolder {
        String id;
        String name;
        String cover;
        List<Track> popularTracks;
        List<Release> popularReleases;
    }

    private static class ReleaseInfoHolder {
        String id;
        String title;
        String cover;
        long releaseDate;
        List<Artist> artists;
        List<Track> tracks;
    }

    // --------------------------------------------------------------
    // DTO КЛАССЫ ДЛЯ ДЕСЕРИАЛИЗАЦИИ (STATS.FM)
    // --------------------------------------------------------------

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
        List<String> spotify;
        List<String> appleMusic;
        String upc;
        String ean;
        String isrc;
    }

    private static class StatsFMTrack {
        List<StatsFMAlbum> albums;
        List<StatsFMArtist> artists;
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
        List<StatsFMTrack> tracks;
        List<StatsFMAlbum> albums;
        List<StatsFMArtist> artists;
    }

    private static class StatsFmReleaseFull {
        String name;
        String image;
        String label;
        int spotifyPopularity;
        int totalTracks;
        long releaseDate;
        List<String> genres;
        List<StatsFMArtist> artists;
        ExternalIds externalIds;
        String type;
        int id;
    }

    private static class StatsFmArtistFull {
        ExternalIds externalIds;
        int followers;
        List<String> genres;
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
        List<StatsFMTrack> items;
    }

    private static class ResArtistInfo {
        StatsFmArtistFull items;
    }

    private static class ResFindStatsFM {
        StatsFMItem items;
    }

    private static class ResTopArtistTracks {
        List<StatsFMTrack> items;
    }

    private static class ResTopArtistAlbums {
        List<StatsFMAlbum> items;
    }
}
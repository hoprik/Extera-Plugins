package ru.hoprik.player.api;

import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.api.helpers.ICallback;
import ru.hoprik.player.api.helpers.info.IArtistInfo;
import ru.hoprik.player.api.helpers.info.ITrackInfo;
import ru.hoprik.player.api.providers.GlobalProvider;
import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Track;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ApiInterface {

    private static GlobalProvider getGlobalProvider() {
        return MusicPlayer.getInstance().getProvider();
    }

    public static CompletableFuture<Track> getTrack(String id) {
        List<ITrackInfo> providers = getGlobalProvider().getProviders(ITrackInfo.class);
        CompletableFuture<Track> future = new CompletableFuture<>();

        for (ITrackInfo provider : providers) {
            provider.getTrackInfo(id, new ICallback<>() {
                @Override
                public void onSuccess(Track item) {
                    if (item != null) {
                        future.complete(item);
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
        }

        return future.orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> null);
    }

    public static CompletableFuture<Artist> getArtist(String id) {
        List<IArtistInfo> providers = getGlobalProvider().getProviders(IArtistInfo.class);
        CompletableFuture<Artist> future = new CompletableFuture<>();

        for (IArtistInfo provider : providers) {
            provider.getArtistInfo(id, new ICallback<>() {
                @Override
                public void onSuccess(Artist item) {
                    if (item != null) {
                        future.complete(item);
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
        }

        return future.orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> null);
    }
}

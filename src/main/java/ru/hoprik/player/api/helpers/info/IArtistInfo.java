package ru.hoprik.player.api.helpers.info;

import ru.hoprik.player.api.helpers.ICallback;
import ru.hoprik.player.audio.objects.Artist;

public interface IArtistInfo {
    void getArtistInfo(int id, ICallback<Artist> callback);
}

package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.audio.Artist;

public interface IArtistInfo {
    void getArtistInfo(int id, ICallback<Artist> callback);
}

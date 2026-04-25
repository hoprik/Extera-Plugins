package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.ArtistInfo;

public interface IArtistInfo {
    void getArtistInfo(int id, ICallback<ArtistInfo> callback);
}

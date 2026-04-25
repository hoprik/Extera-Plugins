package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.TrackInfo;

public interface ITrackInfo extends IReleaseInfo, IArtistInfo {
    void getTrackInfo(int id, ICallback<TrackInfo> callback);
}

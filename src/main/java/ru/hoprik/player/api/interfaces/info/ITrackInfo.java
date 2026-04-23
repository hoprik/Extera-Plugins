package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.TrackInfo;

import java.util.List;

public interface ITrackInfo extends IReleaseInfo, IArtistInfo {
    ICallback<TrackInfo> getTrackInfo(String name, List<String> artists);
}

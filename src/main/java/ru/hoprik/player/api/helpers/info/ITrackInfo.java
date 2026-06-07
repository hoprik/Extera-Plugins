package ru.hoprik.player.api.helpers.info;

import ru.hoprik.player.api.helpers.ICallback;
import ru.hoprik.player.audio.objects.Track;

public interface ITrackInfo{
    void getTrackInfo(String id, ICallback<Track> callback);
}

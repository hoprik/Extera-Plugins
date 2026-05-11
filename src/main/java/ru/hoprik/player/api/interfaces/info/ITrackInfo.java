package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.audio.Track;

public interface ITrackInfo{
    void getTrackInfo(int id, ICallback<Track> callback);
}

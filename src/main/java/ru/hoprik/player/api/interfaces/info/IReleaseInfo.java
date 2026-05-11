package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.audio.Release;

public interface IReleaseInfo {
    void getReleaseInfo(int id, ICallback<Release> callback);
}

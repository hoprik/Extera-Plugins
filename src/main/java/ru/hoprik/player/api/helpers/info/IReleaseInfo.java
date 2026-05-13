package ru.hoprik.player.api.helpers.info;

import ru.hoprik.player.api.helpers.ICallback;
import ru.hoprik.player.audio.objects.Release;

public interface IReleaseInfo {
    void getReleaseInfo(int id, ICallback<Release> callback);
}

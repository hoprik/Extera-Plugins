package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.ReleaseInfo;

public interface IReleaseInfo {
    void getReleaseInfo(int id, ICallback<ReleaseInfo> callback);
}

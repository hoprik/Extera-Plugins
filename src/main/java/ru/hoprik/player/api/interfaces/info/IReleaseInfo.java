package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.ReleaseInfo;

import java.util.List;

public interface IReleaseInfo {
    ICallback<ReleaseInfo> getReleaseInfo(String name, List<String> artists);
}

package ru.hoprik.player.api.interfaces.info;

import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.FindMusicInfo;

public interface IFindMusicInfo {
    void findMusicInfo(String query, ICallback<FindMusicInfo> callback);
}

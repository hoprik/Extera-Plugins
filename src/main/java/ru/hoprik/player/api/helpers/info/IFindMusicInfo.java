package ru.hoprik.player.api.helpers.info;

import ru.hoprik.player.api.helpers.ICallback;
import ru.hoprik.player.api.objects.FindMusicInfo;

public interface IFindMusicInfo {
    void findMusicInfo(String query, ICallback<FindMusicInfo> callback);
}

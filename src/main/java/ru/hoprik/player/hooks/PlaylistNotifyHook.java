package ru.hoprik.player.hooks;

import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.audio.AudioPlayer;
import ru.hoprik.player.helpers.NotificationCenterCodes;

public class PlaylistNotifyHook extends XC_MethodHook {
    @Override
    protected void beforeHookedMethod(MethodHookParam methodHookParam) {
        Log.d("MusicPlayer", "PlaylistNotifyHook: work, created...");

    }
}

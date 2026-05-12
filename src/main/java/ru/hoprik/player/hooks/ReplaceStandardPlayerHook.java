package ru.hoprik.player.hooks;

import de.robv.android.xposed.XC_MethodHook;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.LaunchActivity;
import ru.hoprik.player.MusicPlayer;

public class ReplaceStandardPlayerHook extends XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam methodHookParam) {
        Object arg = methodHookParam.args[0];
        if (arg instanceof AudioPlayerAlert && MusicPlayer.getInstance().isFeatureEnabled("enable_audioplayer", true)){
//            methodHookParam.setResult(null);
//            MusicPlayer.getInstance().startPlayerUI(HookUtils.getFragment());
        }
    }
}

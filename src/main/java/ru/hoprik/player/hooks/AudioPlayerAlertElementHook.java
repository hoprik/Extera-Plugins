package ru.hoprik.player.hooks;

import de.robv.android.xposed.XC_MethodHook;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Components.AudioPlayerAlert;
import ru.hoprik.player.MusicPlayer;

public class AudioPlayerAlertElementHook extends XC_MethodHook {
    public static final int AUDIO_PLAYER_ALERT_ELEMENT = 1111111;

    @Override
    protected void afterHookedMethod(MethodHookParam methodHookParam) {
        AudioPlayerAlert audioPlayerAlert = (AudioPlayerAlert) methodHookParam.thisObject;
        if (audioPlayerAlert != null) {
            ActionBarMenuItem actionBarMenuItem = (ActionBarMenuItem) HookUtils.getPrivateField(audioPlayerAlert, "optionsButton");
            if (actionBarMenuItem != null) {
                actionBarMenuItem.addSubItem(AUDIO_PLAYER_ALERT_ELEMENT, R.drawable.player, MusicPlayer.getInstance().getString("player"));
                actionBarMenuItem.setSubItemShown(AUDIO_PLAYER_ALERT_ELEMENT, HookUtils.isMusicPlayed());
            }
        }
    }
}

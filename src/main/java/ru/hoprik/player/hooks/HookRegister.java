package ru.hoprik.player.hooks;

import android.app.Dialog;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BaseFragment;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

public class HookRegister {
    public List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    public void registerHooks() throws NoSuchMethodException {
        registerHookMethod(BaseFragment.class.getDeclaredMethod("showDialog", Dialog.class), new ReplaceStandardPlayerHook());
        registerHookMethod(MediaController.class.getDeclaredMethod("playMessage", MessageObject.class, boolean.class), new MediaControllerHook());
        registerHookMethod(MediaController.class.getDeclaredMethod("setPlaylist", ArrayList.class, MessageObject.class, long.class), new PlaylistNotifyHook());
    }

    private void registerHookAllMethods(Class<?> clazz, String method, Object hook) {
        hooks.addAll(XposedBridge.hookAllMethods(clazz, method, (XC_MethodHook) hook));
    }

    private void registerHookMethod(Member member, Object hook) {
        hooks.add(XposedBridge.hookMethod(member, (XC_MethodHook) hook));
        Log.d("HOOK REGISTER", member.getName() + "is registred");
    }

    public void unregisterHooks() {
        for (XC_MethodHook.Unhook hook : hooks) {
            if (hook != null) {
                hook.unhook();
            }
        }
    }
}

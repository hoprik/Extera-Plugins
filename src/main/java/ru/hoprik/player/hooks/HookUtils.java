package ru.hoprik.player.hooks;

import android.util.Log;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HookUtils {
    public static BaseFragment getFragment() {
        return LaunchActivity.getSafeLastFragment();
    }

    public static Object getPrivateField(Object object, String fieldName) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                Log.e("HookUtils", "Access error: " + e.getMessage());
                return null;
            }
        }
        Log.e("HookUtils", "Field not found: " + fieldName);
        return null;
    }

    public static void setPrivateField(Object object, String fieldName, Object value) {
        Class<?> clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                Log.e("HookUtils", "Set access error: " + e.getMessage());
                return;
            }
        }
        Log.e("HookUtils", "Field not found for set: " + fieldName);
    }

    public static void invokePrivateMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            method.invoke(obj, args);
        } catch (Exception e) {
            Log.e("HookUtils", "invokePrivateMethod error: " + e.getMessage());
        }
    }

    public static Object invokePrivateMethodWithReturn(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = obj.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (Exception e) {
            Log.e("HookUtils", "invokePrivateMethodWithReturn error: " + e.getMessage());
            return null;
        }
    }

    public static boolean isMusicPlayed() {
        MessageObject object = MediaController.getInstance().getPlayingMessageObject();
        return object != null && object.isMusic();
    }
}
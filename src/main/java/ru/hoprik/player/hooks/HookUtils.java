package ru.hoprik.player.hooks;

import android.util.Log;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class HookUtils {
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    public static BaseFragment getFragment() {
        return LaunchActivity.getSafeLastFragment();
    }

    public static Object getPrivateField(Object object, String fieldName) {
        return getPrivateFieldCached(object, fieldName);
    }

    private static Object getPrivateFieldCached(Object object, String fieldName) {
        Class<?> clazz = object.getClass();
        String key = clazz.getName() + "#" + fieldName;
        Field field = FIELD_CACHE.get(key);
        if (field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(key, field);
            } catch (NoSuchFieldException e) {
                // пробуем суперкласс
                try {
                    field = clazz.getSuperclass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    FIELD_CACHE.put(key, field);
                } catch (NoSuchFieldException ex) {
                    Log.e("HookUtils", "Field not found: " + fieldName);
                    return null;
                }
            }
        }
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void setPrivateField(Object object, String fieldName, Object value) {
        Class<?> clazz = object.getClass();
        String key = clazz.getName() + "#" + fieldName;
        Field field = FIELD_CACHE.get(key);
        if (field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                FIELD_CACHE.put(key, field);
            } catch (NoSuchFieldException e) {
                try {
                    field = clazz.getSuperclass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    FIELD_CACHE.put(key, field);
                } catch (NoSuchFieldException ex) {
                    Log.e("HookUtils", "Field not found for set: " + fieldName);
                    return;
                }
            }
        }
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            Log.e("HookUtils", "Set access error: " + e.getMessage());
        }
    }

    public static void invokePrivateMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        Class<?> clazz = obj.getClass();
        String key = clazz.getName() + "#" + methodName + Arrays.toString(paramTypes);
        Method method = METHOD_CACHE.get(key);
        if (method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                METHOD_CACHE.put(key, method);
            } catch (NoSuchMethodException e) {
                Log.e("HookUtils", "Method not found: " + methodName);
                return;
            }
        }
        try {
            method.invoke(obj, args);
        } catch (Exception e) {
            Log.e("HookUtils", "invoke error: " + e.getMessage());
        }
    }

    public static Object invokePrivateMethodWithReturn(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        Class<?> clazz = obj.getClass();
        String key = clazz.getName() + "#" + methodName + Arrays.toString(paramTypes);
        Method method = METHOD_CACHE.get(key);
        if (method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                METHOD_CACHE.put(key, method);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isMusicPlayed() {
        MessageObject object = MediaController.getInstance().getPlayingMessageObject();
        return object != null && object.isMusic();
    }
}
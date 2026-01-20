package ru.hoprik.player;

import org.telegram.ui.ActionBar.BaseFragment;
import ru.hoprik.player.ui.MusicPlayerUI;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class MusicPlayer {
    private static final MusicPlayer instance = new MusicPlayer();

    public MusicPlayer() {
    }

    public static MusicPlayer getInstance() {
        return instance;
    }

    public void startPlayerUI(BaseFragment baseFragment) {
        baseFragment.presentFragment(new MusicPlayerUI());
    }

    public static void injectDex(ClassLoader hostLoader, ClassLoader pluginLoader) {
        try {
            // 1. Получаем pathList из основного загрузчика (Telegram)
            Object hostPathList = getPathList(hostLoader);
            // 2. Получаем pathList из твоего InMemory загрузчика
            Object pluginPathList = getPathList(pluginLoader);

            // 3. Достаем массив dexElements у обоих
            Object hostElements = getDexElements(hostPathList);
            Object pluginElements = getDexElements(pluginPathList);

            // 4. Объединяем массивы (Host + Plugin)
            Object combinedElements = combineArrays(hostElements, pluginElements);

            // 5. Подменяем массив в основном загрузчике
            setDexElements(hostPathList, combinedElements);

            System.out.println("✅ DEX успешно внедрен в основной ClassLoader!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object getPathList(Object classLoader) throws Exception {
        Class<?> baseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        Field pathListField = baseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        return pathListField.get(classLoader);
    }

    private static Object getDexElements(Object pathList) throws Exception {
        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        return dexElementsField.get(pathList);
    }

    private static void setDexElements(Object pathList, Object newElements) throws Exception {
        Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        dexElementsField.set(pathList, newElements);
    }

    private static Object combineArrays(Object array1, Object array2) {
        Class<?> componentType = array1.getClass().getComponentType();
        int length1 = Array.getLength(array1);
        int length2 = Array.getLength(array2);
        Object combined = Array.newInstance(componentType, length1 + length2);
        System.arraycopy(array1, 0, combined, 0, length1);
        System.arraycopy(array2, 0, combined, length1, length2);
        return combined;
    }
}
package ru.hoprik.player.api.providers;

import ru.hoprik.player.api.ApiClient;
import ru.hoprik.player.api.helpers.info.IArtistInfo;
import ru.hoprik.player.api.helpers.info.IFindMusicInfo;
import ru.hoprik.player.api.helpers.info.IReleaseInfo;
import ru.hoprik.player.api.helpers.info.ITrackInfo;

import java.util.*;

public class GlobalProvider{
    private static class ProviderEntry implements Comparable<ProviderEntry> {
        final Object provider;
        final int priority;

        ProviderEntry(Object provider, int priority) {
            this.provider = provider;
            this.priority = priority;
        }

        @Override
        public int compareTo(ProviderEntry other) {
            // Сортируем по убыванию: чем ВЫШЕ цифра приоритета, тем ПЕРВЕЕ он будет в списке
            return Integer.compare(other.priority, this.priority);
        }
    }

    private final Map<Class<?>, List<ProviderEntry>> registry = new HashMap<>();

    public GlobalProvider(){
        register();
    }

    private void register(){
    }

    public void registerProvider(Object provider, int priority) {
        if (provider instanceof ITrackInfo) addFeature(ITrackInfo.class, provider, priority);
        if (provider instanceof IArtistInfo) addFeature(IArtistInfo.class, provider, priority);
        if (provider instanceof IFindMusicInfo) addFeature(IFindMusicInfo.class, provider, priority);
        if (provider instanceof IReleaseInfo) addFeature(IReleaseInfo.class, provider, priority);
    }

    public void unregisterProvider(Object provider) {
        for (List<ProviderEntry> list : registry.values()) {
            // Используем Java 8+ метод для безопасного удаления по условию
            list.removeIf(entry -> entry.provider.equals(provider));
        }
    }

    // 💡 НОВЫЙ МЕТОД: Полная очистка (удобно при логауте пользователя)
    public void clearAll() {
        registry.clear();
    }

    private <T> void addFeature(Class<T> clazz, Object provider, int priority) {
        List<ProviderEntry> list = registry.computeIfAbsent(clazz, k -> new ArrayList<>());
        list.removeIf(entry -> entry.provider.equals(provider));

        if (priority>=0) {
            list.add(new ProviderEntry(provider, priority));
        }
        Collections.sort(list);
    }
    @SuppressWarnings("unchecked")
    public <T> List<T> getProviders(Class<T> featureClass) {
        return (List<T>) registry.getOrDefault(featureClass, Collections.emptyList());
    }
}

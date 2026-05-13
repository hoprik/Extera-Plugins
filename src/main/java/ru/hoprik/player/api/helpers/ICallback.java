package ru.hoprik.player.api.helpers;

public interface ICallback<T> {
    void onSuccess(T item);
    void onError(Throwable throwable);
}

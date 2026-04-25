package ru.hoprik.player.api.interfaces;

public interface ICallback<T> {
    void onSuccess(T item);
    void onError(Throwable throwable);
}

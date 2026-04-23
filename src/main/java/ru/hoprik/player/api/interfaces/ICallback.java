package ru.hoprik.player.api.interfaces;

public interface ICallback<T> {
    T onSuccess(Class<T> type);
}

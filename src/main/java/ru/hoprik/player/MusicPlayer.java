package ru.hoprik.player;

public class MusicPlayer {
    private static MusicPlayer instance = new MusicPlayer();

    public MusicPlayer() {
    }

    public static MusicPlayer getInstance() {
        return instance;
    }
}
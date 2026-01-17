package ru.hoprik.player;


import org.telegram.ui.ActionBar.BaseFragment;
import ru.hoprik.player.ui.MusicPlayerUI;

public class MusicPlayer {

    private static final MusicPlayer instance = new MusicPlayer();

    public MusicPlayer() {
    }

    public static MusicPlayer getInstance() {
        return instance;
    }

    public void startMusicPlayer(BaseFragment bar){
        bar.presentFragment(new MusicPlayerUI());
    }
}

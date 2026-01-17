package ru.hoprik.player;


import org.telegram.ui.ActionBar.BaseFragment;
import ru.hoprik.player.ui.MusicPlayerUI;

public class MusicPlayer {
    public MusicPlayer(){

    }

    public void startMusicPlayer(BaseFragment bar){
        bar.presentFragment(new MusicPlayerUI());
    }
}

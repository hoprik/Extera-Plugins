package ru.hoprik.player;

import org.telegram.ui.ActionBar.BaseFragment;
import ru.hoprik.player.ui.MusicPlayerUI;

public class MusicPlayer {

    // 1. Initialize the instance
    private static final MusicPlayer instance = new MusicPlayer();

    // 2. Explicit constructor is required for some Proxy generators to work correctly
    public MusicPlayer() {
    }

    // 3. Static accessor
    public static MusicPlayer getInstance() {
        return instance;
    }

    public void startMusicPlayer(BaseFragment bar) {
        if (bar != null) {
            // CRITICAL: If MusicPlayerUI is not in your classes.dex, this line will cause the crash
            bar.presentFragment(new MusicPlayerUI());
        }
    }
}
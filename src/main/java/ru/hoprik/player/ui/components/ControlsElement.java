package ru.hoprik.player.ui.components;

public class ControlsElement {
    private Runnable runnable;
    private int iconId;
    private boolean isAnimation;

    public ControlsElement(Runnable runnable, int iconId, boolean isAnimation) {
        this.runnable = runnable;
        this.iconId = iconId;
        this.isAnimation = isAnimation;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public boolean isAnimation() {
        return isAnimation;
    }

    public void setAnimation(boolean animation) {
        this.isAnimation = animation;
    }
}


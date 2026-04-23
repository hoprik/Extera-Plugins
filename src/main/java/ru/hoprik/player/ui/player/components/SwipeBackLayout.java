package ru.hoprik.player.ui.player.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;

public class SwipeBackLayout extends FrameLayout {
    private float startX;
    private float startY;
    private boolean isTracking;
    private boolean isFinished;
    private final int edgeSize = AndroidUtilities.dp(20);
    private final BaseFragment fragment;

    public SwipeBackLayout(Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isFinished) return true;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                if (startX < edgeSize) {
                    isTracking = true;

                    return false;
                }
                isTracking = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isTracking) {
                    float dx = ev.getX() - startX;
                    float dy = ev.getY() - startY;

                    if (dx > AndroidUtilities.dp(10) && Math.abs(dx) > Math.abs(dy)) {
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTracking = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTracking) {
            event.getAction();
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float dx = x - startX;
                if (dx > 0) {
                    setTranslationX(dx);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTracking = false;
                if (getTranslationX() > getWidth() / 3.0f) {
                    isFinished = true;
                    animate().translationX(getWidth())
                            .setDuration(200)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    fragment.finishFragment();
                                }
                            }).start();
                } else {
                    animate().translationX(0).setDuration(200).start();
                }
                return true;
        }
        return true;
    }
}
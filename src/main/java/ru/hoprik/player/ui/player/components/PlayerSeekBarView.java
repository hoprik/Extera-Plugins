package ru.hoprik.player.ui.player.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.TouchDelegate;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.VelocityTracker;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;

import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.audio.AudioPlayer;

public class PlayerSeekBarView extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {

    private static final long MIN_ANIMATION_DURATION_MS = 90L;
    private static final long MAX_ANIMATION_DURATION_MS = 220L;
    private static final int TOUCH_TARGET_EXPAND_DP = 18;

    private final TextView currentTimeView;
    private final TextView remainingTimeView;
    private final SliderView slider;
    private final Rect touchDelegateRect = new Rect();

    private boolean isDragging = false;
    // Кэшируем AudioPlayer, чтобы не дёргать MusicPlayer.getInstance() в горячих местах
    private AudioPlayer cachedAudioPlayer;

    private interface SliderProgressListener {
        void onProgress(float normalized);
    }

    public PlayerSeekBarView(Context context, AudioPlayer audioPlayer) {
        super(context);

        // Сохраняем переданный плеер как кэшированный
        this.cachedAudioPlayer = audioPlayer;

        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams progressContainerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressContainerParams.setMargins(
                AndroidUtilities.dp(0),
                AndroidUtilities.dp(10),
                AndroidUtilities.dp(0),
                AndroidUtilities.dp(0)
        );
        setLayoutParams(progressContainerParams);

        slider = new SliderView(context);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sliderParams.setMargins(0, 0, 0, AndroidUtilities.dp(8));
        slider.setLayoutParams(sliderParams);
        slider.setProgressListener(normalized -> {
            // Используем закэшированный плеер для получения длительности
            int duration = getTrackDuration(cachedAudioPlayer);
            updateTimeLabels(Math.round(normalized * duration), duration);
        });
        addView(slider);

        LinearLayout timeContainer = new LinearLayout(context);
        timeContainer.setOrientation(LinearLayout.HORIZONTAL);
        timeContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        currentTimeView = new TextView(context);
        currentTimeView.setTextColor(Theme.getColor(Theme.key_player_time));
        currentTimeView.setTextSize(12);
        currentTimeView.setText(AndroidUtilities.formatLongDuration(0));
        currentTimeView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        remainingTimeView = new TextView(context);
        remainingTimeView.setTextColor(Theme.getColor(Theme.key_player_time));
        remainingTimeView.setTextSize(12);
        remainingTimeView.setGravity(Gravity.RIGHT);
        remainingTimeView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        timeContainer.addView(currentTimeView);
        timeContainer.addView(remainingTimeView);
        addView(timeContainer);

        syncState(cachedAudioPlayer);
        register();
        post(this::updateSeekBarTouchDelegate);
    }

    // Обновляет закэшированный AudioPlayer (вызывается при смене трека)
    private void updateCachedAudioPlayer() {
        cachedAudioPlayer = MusicPlayer.getInstance().getAudioPlayer();
    }

    private void syncState(AudioPlayer audioPlayer) {
        int duration = getTrackDuration(audioPlayer);
        if (duration <= 0 || audioPlayer == null) {
            applyProgress(0f, 0, false);
            return;
        }

        float normalizedProgress = clampNormalized(audioPlayer.getProgress() / duration);
        applyProgress(normalizedProgress, duration, false);
    }

    private void updateProgress(float normalizedProgress) {
        if (isDragging) return;

        // Используем закэшированный плеер вместо вызова getInstance()
        int duration = getTrackDuration(cachedAudioPlayer);
        if (cachedAudioPlayer == null || duration <= 0) {
            applyProgress(0f, 0, false);
            return;
        }

        applyProgress(clampNormalized(normalizedProgress), duration, true);
    }

    private void applyProgress(float normalizedProgress, int duration, boolean animate) {
        normalizedProgress = clampNormalized(normalizedProgress);
        slider.setProgressNormalized(normalizedProgress, animate);
        if (!animate) {
            updateTimeLabels(Math.round(normalizedProgress * duration), duration);
        }
    }

    private void updateTimeLabels(int currentTimeSeconds, int durationSeconds) {
        currentTimeView.setText(AndroidUtilities.formatLongDuration(Math.max(0, currentTimeSeconds)));
        remainingTimeView.setText(AndroidUtilities.formatLongDuration(Math.max(0, durationSeconds)));
    }

    private int getTrackDuration(AudioPlayer audioPlayer) {
        if (audioPlayer == null || audioPlayer.getAudioElement() == null || audioPlayer.getAudioElement().getTrack() == null) {
            return 0;
        }
        return Math.max(0, audioPlayer.getAudioElement().getTrack().getDuration());
    }

    private float clampNormalized(float progress) {
        return Math.max(0f, Math.min(progress, 1f));
    }

    private void updateSeekBarTouchDelegate() {
        if (slider.getWidth() == 0 || slider.getHeight() == 0) return;

        slider.getHitRect(touchDelegateRect);
        int expand = AndroidUtilities.dp(TOUCH_TARGET_EXPAND_DP);
        int verticalExtra = AndroidUtilities.dp(10);
        touchDelegateRect.left -= expand;
        touchDelegateRect.top -= (expand + verticalExtra);
        touchDelegateRect.right += expand;
        touchDelegateRect.bottom += (expand + verticalExtra);
        setTouchDelegate(new TouchDelegate(touchDelegateRect, slider));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (slider != null) {
            slider.cancelAnimations();
        }
        unregister();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateSeekBarTouchDelegate();
    }

    public void register() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        // Подписываемся на уведомление о смене трека, чтобы обновить кэш AudioPlayer
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messageAudioTrackChanged);
    }

    private void unregister() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messageAudioTrackChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            if (args == null || args.length < 2 || args[1] == null) return;

            Object progressObj = args[1];
            float normalizedProgress;
            if (progressObj instanceof Float) {
                normalizedProgress = (Float) progressObj;
            } else if (progressObj instanceof Integer) {
                normalizedProgress = ((Integer) progressObj).floatValue();
            } else if (progressObj instanceof Long) {
                normalizedProgress = ((Long) progressObj).floatValue();
            } else {
                Log.e("AudioPlayer", "Unexpected progress type: " + progressObj.getClass().getName());
                return;
            }
            updateProgress(normalizedProgress);
        } else if (id == NotificationCenter.messageChan) {
            // При смене трека сбрасываем кэш и синхронизируем состояние
            updateCachedAudioPlayer();
            syncState(cachedAudioPlayer);
        }
    }

    /**
     * Кастомный слайдер с анимацией, инерцией и увеличенной областью касания.
     */
    private class SliderView extends View {

        private SliderProgressListener listener;
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF trackRect = new RectF();
        private final RectF progressRect = new RectF();

        private final float thumbRadius;
        private final float trackHeight;

        private float normalized = 0f;
        private VelocityTracker velocityTracker;
        private ValueAnimator flingAnimator;

        SliderView(Context ctx) {
            super(ctx);
            trackPaint.setColor(0x33FFFFFF);
            progressPaint.setColor(Theme.getColor(Theme.key_player_progress));
            thumbPaint.setColor(Theme.getColor(Theme.key_player_progress));

            thumbRadius = AndroidUtilities.dp(8);
            trackHeight = AndroidUtilities.dp(3);

            setClickable(true);
        }

        void setProgressListener(SliderProgressListener l) {
            this.listener = l;
        }

        void setProgressNormalized(float value, boolean animate) {
            value = clampNormalized(value);
            if (flingAnimator != null) {
                flingAnimator.cancel();
                flingAnimator = null;
            }
            if (!animate) {
                normalized = value;
                invalidate();
                if (listener != null) listener.onProgress(normalized);
                return;
            }
            float start = normalized;
            if (Math.abs(start - value) < 0.0001f) return;
            flingAnimator = ValueAnimator.ofFloat(start, value);
            long duration = Math.min(MAX_ANIMATION_DURATION_MS, Math.max(MIN_ANIMATION_DURATION_MS, (long)(Math.abs(value - start) * 400L)));
            flingAnimator.setDuration(duration);
            flingAnimator.setInterpolator(new DecelerateInterpolator());
            flingAnimator.addUpdateListener(a -> {
                normalized = (Float) a.getAnimatedValue();
                invalidate();
                if (listener != null) listener.onProgress(normalized);
            });
            flingAnimator.start();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int desiredH = (int) (thumbRadius * 2) + getPaddingTop() + getPaddingBottom();
            int height = resolveSize(desiredH, heightMeasureSpec);
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float left = getPaddingLeft() + thumbRadius;
            float right = w - getPaddingRight() - thumbRadius;
            float centerY = h / 2f;

            trackRect.set(left, centerY - trackHeight / 2f, right, centerY + trackHeight / 2f);
            canvas.drawRoundRect(trackRect, trackHeight / 2f, trackHeight / 2f, trackPaint);

            float progressRight = left + normalized * Math.max(0f, (right - left));
            progressRect.set(left, centerY - trackHeight / 2f, progressRight, centerY + trackHeight / 2f);
            canvas.drawRoundRect(progressRect, trackHeight / 2f, trackHeight / 2f, progressPaint);

            canvas.drawCircle(progressRight, centerY, thumbRadius, thumbPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
            velocityTracker.addMovement(event);

            float x = event.getX();
            float left = getPaddingLeft() + thumbRadius;
            float right = getWidth() - getPaddingRight() - thumbRadius;
            float usable = Math.max(1f, right - left);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    if (flingAnimator != null) {
                        flingAnimator.cancel();
                        flingAnimator = null;
                    }
                    PlayerSeekBarView.this.isDragging = true;
                    // fallthrough
                case MotionEvent.ACTION_MOVE:
                    float nx = (x - left) / usable;
                    normalized = clampNormalized(nx);
                    invalidate();
                    if (listener != null) listener.onProgress(normalized);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    velocityTracker.computeCurrentVelocity(1000);
                    float vx = velocityTracker.getXVelocity();
                    final float absV = Math.abs(vx);
                    final float decel = AndroidUtilities.dp(4000);

                    float distance = (vx * vx) / (2f * decel);
                    if (Float.isNaN(distance)) distance = 0f;
                    float sign = vx >= 0 ? 1f : -1f;
                    float currX = left + normalized * usable;
                    float targetX = currX + sign * distance;
                    float targetNormalized = clampNormalized((targetX - left) / usable);

                    if (absV > 500f) {
                        long duration = Math.max(150L, Math.min(900L, (long)(Math.abs(vx) / decel * 1000f)));
                        if (flingAnimator != null) flingAnimator.cancel();
                        final float start = normalized;
                        flingAnimator = ValueAnimator.ofFloat(start, targetNormalized);
                        flingAnimator.setDuration(duration);
                        flingAnimator.setInterpolator(new DecelerateInterpolator());
                        flingAnimator.addUpdateListener(a -> {
                            normalized = (Float) a.getAnimatedValue();
                            invalidate();
                            if (listener != null) listener.onProgress(normalized);
                        });
                        flingAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(android.animation.Animator animation) {
                                PlayerSeekBarView.this.isDragging = false;
                                safeRecycleVelocityTracker();
                                // Используем закэшированный плеер для seek
                                if (cachedAudioPlayer != null) {
                                    cachedAudioPlayer.seek(normalized);
                                }
                            }
                        });
                        flingAnimator.start();
                    } else {
                        PlayerSeekBarView.this.isDragging = false;
                        safeRecycleVelocityTracker();
                        if (flingAnimator != null) flingAnimator.cancel();
                        flingAnimator = ValueAnimator.ofFloat(normalized, normalized);
                        flingAnimator.setDuration(120);
                        flingAnimator.addUpdateListener(a -> {});
                        flingAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(android.animation.Animator animation) {
                                if (cachedAudioPlayer != null) {
                                    cachedAudioPlayer.seek(normalized);
                                }
                            }
                        });
                        flingAnimator.start();
                    }
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private float clampNormalized(float v) {
            return Math.max(0f, Math.min(v, 1f));
        }

        void cancelAnimations() {
            if (flingAnimator != null) {
                flingAnimator.cancel();
                flingAnimator = null;
            }
            safeRecycleVelocityTracker();
        }

        private void safeRecycleVelocityTracker() {
            if (velocityTracker != null) {
                try {
                    velocityTracker.clear();
                    velocityTracker.recycle();
                } catch (IllegalStateException e) {
                    Log.w("SliderView", "VelocityTracker already recycled", e);
                } finally {
                    velocityTracker = null;
                }
            }
        }
    }
}
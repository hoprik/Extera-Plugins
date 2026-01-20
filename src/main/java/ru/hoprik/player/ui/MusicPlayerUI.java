package ru.hoprik.player.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.SeekBar;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.*;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.*;
import ru.hoprik.player.utils.ControlsHelpers;
import ru.hoprik.player.utils.ImageHelper;
import ru.hoprik.player.utils.MusicInfo;
import ru.hoprik.player.utils.UpdateManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicPlayerUI extends BaseFragment {

    BackupImageView backgroundImage;
    GradientDrawable overlayColor;
    TextView authorView;
    TextView songView;
    TextView currentTimeView;
    TextView remainingTimeView;
    ImageView repeatButtonView;
    ImageView shuffleButtonView;
    SeekBar seekBar;

    UpdateManager manager;

    boolean enableShafle = true;

    public MusicPlayerUI() {
        this.enableShafle = true;
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean canBeginSlide() {
        return true;
    }

    @Override
    public ActionBar createActionBar(Context context) {
        return null;
    }

    @Override
    public View createView(Context context) {
        fragmentView = new SwipeBackLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        FrameLayout container = (FrameLayout) fragmentView;

        MusicInfo info = new MusicInfo();
        if (info.getMessageObject() == null) {
            renderError(container, context);
            return fragmentView;
        }

        LinearLayout main_layout = new LinearLayout(context);
        main_layout.setOrientation(LinearLayout.VERTICAL);
        main_layout.setPadding(
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20)
        );
        main_layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int statusBarHeight = insets.getSystemWindowInsetTop();
            main_layout.setPadding(
                    AndroidUtilities.dp(20),
                    AndroidUtilities.dp(statusBarHeight),
                    AndroidUtilities.dp(20),
                    AndroidUtilities.dp(20)
            );
            return ViewCompat.onApplyWindowInsets(v, insets);
        });


        ImageView backButton = new ImageView(context);
        backButton.setOnClickListener(view -> finishFragment());
        backButton.setImageResource(R.drawable.ic_ab_back);
        backButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector)));
        backButton.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));

        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        backParams.gravity = Gravity.START;
        backButton.setLayoutParams(backParams);

        main_layout.addView(backButton);


        FrameLayout background = createBackground(context);
        container.addView(background);

        BackupImageView avatarCover = new BackupImageView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
            }
        };

        avatarCover.setAspectFit(true);
        avatarCover.setRoundRadius(10);
        avatarCover.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        main_layout.addView(avatarCover);

        LinearLayout songNameAndAuthor = createSongNameAndAuthor(context, info);
        main_layout.addView(songNameAndAuthor);

        LinearLayout timeline = createSeekBar(context, info);
        main_layout.addView(timeline);

        LinearLayout controls1 = createControl(context);
        main_layout.addView(controls1);

        View space = new View(context);
        LinearLayout.LayoutParams space_params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        );
        space.setLayoutParams(space_params);
        main_layout.addView(space);

        LinearLayout controls2 = createControl2(context, getElements(info));
        main_layout.addView(controls2);

        ImageHelper.updateCover(info.getMessageObject(), avatarCover, false);
        ImageHelper.updateCover(info.getMessageObject(), this.backgroundImage, true);

        this.manager = new UpdateManager(info, songView, authorView, avatarCover, backgroundImage, overlayColor, currentTimeView, remainingTimeView, seekBar);
        this.manager.startUpdater();

        container.addView(main_layout);

        return fragmentView;
    }

    @Override
    public void onFragmentClosed() {
        this.manager.stopUpdater();
    }

    private List<ControlsElement> getElements(MusicInfo info){
        List<ControlsElement> list = new ArrayList<>();
        list.add(new ControlsElement(
                () -> {
                    ControlsHelpers.saveToMusic(info.getMessageObject(), this.getParentActivity());
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_download, "Музыка была скачана").show(true);
                },
                R.drawable.msg_download,
                false
        ));

        list.add(new ControlsElement(
                () -> {
                    ControlsHelpers.share(info.getMessageObject(), this.getParentActivity());
                },
                R.drawable.share,
                false
        ));

        list.add(new ControlsElement(
                () -> {
                    ControlsHelpers.forward(info.getMessageObject(), UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId());
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_save_to_music, "Музыка сохранена в избранное").show(true);
                },
                R.drawable.msg_save_story,
                false
        ));

        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Log.i("LOAFD", loader.toString());
        // 2. Проверяем наличие класса ЧЕРЕЗ ЗАГРУЗЧИК ХОСТА
        if (loader != null && isLyricsAvailable(loader)) {
            list.add(new ControlsElement(
                    () -> {
                        try {
                            Class<?> lyricsClass = loader.loadClass("com.pessdes.lyrics.ui.LyricsActivity");

                            Object instance = lyricsClass.getDeclaredConstructor().newInstance();

                            if (instance instanceof BaseFragment) {
                                this.presentFragment((BaseFragment) instance);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "Ошибка загрузки класса").show(true);
                        }
                    },
                    R.drawable.msg_photo_text2,
                    false
            ));
        }

        return list;
    }
    // Хелпер для проверки через правильный ClassLoader
    private boolean isLyricsAvailable(ClassLoader loader) {
        try {
            // false - не инициализировать (просто проверить наличие)
            Class.forName("com.pessdes.lyrics.ui.LyricsActivity", false, loader);
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateRepeatButtons() {
        if (this.repeatButtonView != null) {
            if (SharedConfig.repeatMode == 2) {
                this.repeatButtonView.setAlpha(1.0f);
            } else {
                this.repeatButtonView.setAlpha(0.5f);
            }
        }

        if (this.shuffleButtonView != null){
            if (SharedConfig.shuffleMusic){
                this.shuffleButtonView.setAlpha(1.0f);
            }else{
                this.shuffleButtonView.setAlpha(0.5f);
            }
        }
    }

    private LinearLayout createControl2(Context context, List<ControlsElement> elements){
        int iconColor = Color.parseColor("#FFFFFF");

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        linearLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(30),
                AndroidUtilities.dp(30)
        );
        buttonsParams.setMargins(
                AndroidUtilities.dp(10), 0,
                AndroidUtilities.dp(10), 0
        );

        for (ControlsElement element: elements){
            RLottieImageView buttonElement = new RLottieImageView(context);
            buttonElement.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (element.isAnimation){
                buttonElement.setAnimation(element.getIconId(), 36, 36);
            }else {
                buttonElement.setImageResource(element.getIconId());
                buttonElement.setColorFilter(iconColor);
            }
            buttonElement.setLayoutParams(buttonsParams);

            buttonElement.setOnClickListener(view -> new Handler(Looper.getMainLooper()).post(element.getRunnable()));

            linearLayout.addView(buttonElement);
        }

        return linearLayout;
    }

    private LinearLayout createControl(Context context) {
        PlayPauseDrawable playPauseDrawable = null;

        LinearLayout controllerLayout = new LinearLayout(context);
        controllerLayout.setOrientation(LinearLayout.HORIZONTAL);
        controllerLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controllerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        controllerParams.topMargin = AndroidUtilities.dp(15);
        controllerLayout.setLayoutParams(controllerParams);

        int iconColor = Color.parseColor("#FFFFFF");

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(50),
                AndroidUtilities.dp(50)
        );
        buttonParams.setMargins(
                AndroidUtilities.dp(3), 0,
                AndroidUtilities.dp(3), 0
        );

        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(64),
                AndroidUtilities.dp(64)
        );
        playParams.setMargins(
                AndroidUtilities.dp(5), 0,
                AndroidUtilities.dp(5), 0
        );

        if (this.enableShafle) {
            this.repeatButtonView = new ImageView(context);
            this.repeatButtonView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.repeatButtonView.setImageResource(R.drawable.player_new_repeatall);
            this.repeatButtonView.setColorFilter(iconColor);
            this.repeatButtonView.setLayoutParams(buttonParams);
            this.repeatButtonView.setAlpha(0.5f);
            this.repeatButtonView.setOnClickListener(view -> {
                if (SharedConfig.repeatMode == 2) {
                    SharedConfig.setRepeatMode(0);
                } else {
                    SharedConfig.setRepeatMode(2);
                }
                this.updateRepeatButtons();
            });
            controllerLayout.addView(repeatButtonView);
        }

        RLottieImageView prevButton = new RLottieImageView(context);
        prevButton.setScaleType(ImageView.ScaleType.CENTER);
        prevButton.setAnimation(R.raw.player_prev, 40, 40);
        prevButton.setLayerColor("Triangle 3.**", iconColor);
        prevButton.setLayerColor("Triangle 4.**", iconColor);
        prevButton.setLayerColor("Rectangle 4.**", iconColor);
        prevButton.setLayoutParams(buttonParams);

        PlayPauseDrawable finalPlayPauseDrawable = playPauseDrawable;
        prevButton.setOnClickListener(view -> {
            RLottieImageView v = (RLottieImageView) view;
            MediaController.getInstance().playPreviousMessage();
            v.setProgress(0.0f);
            v.playAnimation();
            if (finalPlayPauseDrawable != null) {
                finalPlayPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
            }
            manager.updateUI();
        });

        controllerLayout.addView(prevButton);

        playPauseDrawable = new PlayPauseDrawable(50);
        playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);

        ImageView playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        playButton.setImageDrawable(playPauseDrawable);
        playButton.setColorFilter(iconColor);
        playButton.setLayoutParams(playParams);

        PlayPauseDrawable finalPlayPauseDrawable1 = playPauseDrawable;
        playButton.setOnClickListener(view -> {
            if (MediaController.getInstance().isDownloadingCurrentMessage()) {
                return;
            }
            if (MediaController.getInstance().isMessagePaused()) {
                MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
            } else {
                MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
            }
            finalPlayPauseDrawable1.setPause(!MediaController.getInstance().isMessagePaused(), false);
        });

        controllerLayout.addView(playButton);

        RLottieImageView nextButton = new RLottieImageView(context);
        nextButton.setScaleType(ImageView.ScaleType.CENTER);
        nextButton.setAnimation(R.raw.player_prev, 40, 40);
        nextButton.setLayerColor("Triangle 3.**", iconColor);
        nextButton.setLayerColor("Triangle 4.**", iconColor);
        nextButton.setLayerColor("Rectangle 4.**", iconColor);
        nextButton.setRotation(180);
        nextButton.setLayoutParams(buttonParams);

        PlayPauseDrawable finalPlayPauseDrawable2 = playPauseDrawable;
        nextButton.setOnClickListener(view -> {
            RLottieImageView v = (RLottieImageView) view;
            MediaController.getInstance().playNextMessage();
            v.setProgress(0.0f);
            v.playAnimation();
            if (finalPlayPauseDrawable2 != null) {
                finalPlayPauseDrawable2.setPause(!MediaController.getInstance().isMessagePaused(), false);
            }
            manager.updateUI();
        });
        controllerLayout.addView(nextButton);

        if (this.enableShafle) {
            this.shuffleButtonView = new ImageView(context);
            this.shuffleButtonView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.shuffleButtonView.setImageResource(R.drawable.player_new_shuffle);
            this.shuffleButtonView.setColorFilter(iconColor);
            this.shuffleButtonView.setLayoutParams(buttonParams);
            this.shuffleButtonView.setAlpha(0.5f);

            this.shuffleButtonView.setOnClickListener(view -> {
                if (SharedConfig.shuffleMusic) {
                    MediaController.getInstance().setPlaybackOrderType(0);
                } else {
                    MediaController.getInstance().setPlaybackOrderType(2);
                }
                updateRepeatButtons();
            });
            controllerLayout.addView(this.shuffleButtonView);
        }
        return controllerLayout;
    }

    private LinearLayout createTimeline(Context context, MusicInfo info) {
        LinearLayout timeContainer = new LinearLayout(context);
        timeContainer.setOrientation(LinearLayout.HORIZONTAL);
        timeContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        this.currentTimeView = new TextView(context);
        this.currentTimeView.setTextColor(Color.parseColor("#FFFFFF"));
        this.currentTimeView.setTextSize(12);
        this.currentTimeView.setText(info.getTimeString());

        LinearLayout.LayoutParams currentTimeParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        this.currentTimeView.setLayoutParams(currentTimeParams);

        this.remainingTimeView = new TextView(context);
        this.remainingTimeView.setTextColor(Color.parseColor("#AAAAAA"));
        this.remainingTimeView.setTextSize(12);
        this.remainingTimeView.setText(info.getAudioProgressString());
        this.remainingTimeView.setGravity(Gravity.RIGHT);

        LinearLayout.LayoutParams remainingTimeParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        this.remainingTimeView.setLayoutParams(remainingTimeParams);

        timeContainer.addView(currentTimeView);
        timeContainer.addView(remainingTimeView);
        return timeContainer;
    }

    private LinearLayout createSeekBar(Context context, MusicInfo info) {
        LinearLayout progressContainer = new LinearLayout(context);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setGravity(Gravity.CENTER_HORIZONTAL);

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
        progressContainer.setLayoutParams(progressContainerParams);

        seekBar = new SeekBar(context);
        seekBar.setMax(100);

        GradientDrawable thumbDrawable = new GradientDrawable();
        thumbDrawable.setShape(GradientDrawable.OVAL);
        thumbDrawable.setSize(
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(16)
        );
        thumbDrawable.setColor(Color.parseColor("#FFFFFF"));
        seekBar.setThumb(thumbDrawable);

        seekBar.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(Color.parseColor("#fff300"), PorterDuff.Mode.SRC_IN)
        );

        LinearLayout.LayoutParams seek_bar_params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setThumbOffset(AndroidUtilities.dp(0));
        seek_bar_params.setMargins(0, 0, 0, AndroidUtilities.dp(8));
        seekBar.setLayoutParams(seek_bar_params);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.i("TTTT", i+" "+b);
                if (b && !manager.isDragging()) {
                    currentTimeView.setText(AndroidUtilities.formatLongDuration((i / seekBar.getMax()) * info.getAudioProgress()));
                    info.update();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                manager.setDragging(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaController.getInstance().seekToProgress(info.getMessageObject(), (float) seekBar.getProgress() / seekBar.getMax());
                if (manager != null){
                    manager.setDragging(false);
                }
            }
        });

        if (info.getAudioProgress() > 0) {
            int currentProgress = ((info.getCurrentDuration() / info.getAudioProgress()) * 100);
            seekBar.setProgress(currentProgress);
        } else {
            seekBar.setProgress(0);
        }
        progressContainer.addView(seekBar);
        progressContainer.addView(createTimeline(context, info));

        return progressContainer;
    }

    private LinearLayout createSongNameAndAuthor(Context context, MusicInfo musicInfo) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
        linearLayout.setLayoutParams(layoutParams);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, AndroidUtilities.dp(5), 0, 0);

        this.songView = new TextView(context);
        this.songView.setTextColor(Color.parseColor("#FFFFFF"));
        this.songView.setTextSize(20);
        this.songView.setSingleLine(true);
        this.songView.setTypeface(null, Typeface.BOLD);
        this.songView.setEllipsize(TextUtils.TruncateAt.END);
        this.songView.setText(musicInfo.getCurrentTitle());
        this.songView.setLayoutParams(textParams);

        this.authorView = new TextView(context);
        this.authorView.setTextColor(Color.parseColor("#FFFFFF"));
        this.authorView.setTextSize(16);
        this.authorView.setSingleLine(true);
        this.authorView.setEllipsize(TextUtils.TruncateAt.END);
        this.authorView.setText(musicInfo.getCurrentAuthor());
        this.authorView.setAlpha(0.8f);
        this.authorView.setLayoutParams(textParams);

        linearLayout.addView(this.songView);
        linearLayout.addView(this.authorView);

        return linearLayout;
    }

    private FrameLayout createBackground(Context context) {
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        this.backgroundImage = new BackupImageView(context);
        this.backgroundImage.setAspectFit(false);
        this.backgroundImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        this.backgroundImage.getImageReceiver().setDelegate(new ImageReceiver.ImageReceiverDelegate() {
            @Override
            public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {
                if (imageReceiver.getBitmap() != null) {
                    int dominant = ImageHelper.getDominantColor(imageReceiver.getBitmap());
                    overlayColor.setColors(new int[]{dominant, ImageHelper.darkenColor(dominant, 0.6f)});
                }

            }

            @Override
            public void didSetImageBitmap(int i, String s, Drawable drawable) {
                if (drawable != null) {
                    if (drawable instanceof BitmapDrawable) {
                        int dominant = ImageHelper.getDominantColor((BitmapDrawable) drawable);
                        overlayColor.setColors(new int[]{dominant, ImageHelper.darkenColor(dominant, 0.6f)});
                    }
                }
            }

            @Override
            public void onAnimationReady(ImageReceiver imageReceiver) {
            }
        });

        this.overlayColor = new GradientDrawable();
        this.overlayColor.setColor(Color.parseColor("#CC08090a"));

        FrameLayout overlayFrame = new FrameLayout(context);
        overlayFrame.setBackground(this.overlayColor);
        overlayFrame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        frameLayout.addView(this.backgroundImage);
        frameLayout.addView(overlayFrame);

        return frameLayout;
    }

    private void renderError(FrameLayout container, Context context) {
        TextView textView = new TextView(context);
        textView.setText("Включите музыку");
        textView.setTextSize(24);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setGravity(Gravity.CENTER);

        container.addView(textView, LayoutHelper.createFrame(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
    }

    private static class ControlsElement {
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
            isAnimation = animation;
        }
    }

    private class SwipeBackLayout extends FrameLayout {
        private float startX;
        private float startY;
        private boolean isTracking;
        private boolean isFinished;
        private final int edgeSize = AndroidUtilities.dp(20);

        public SwipeBackLayout(Context context) {
            super(context);
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
                                        finishFragment();
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
}
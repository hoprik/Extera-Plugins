package ru.hoprik.player.ui.player;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.core.view.ViewCompat;

import com.exteragram.messenger.utils.text.LocaleUtils;
import org.telegram.messenger.*;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.*;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.ui.player.components.*;
import ru.hoprik.player.utils.ControlsHelpers;
import ru.hoprik.player.utils.ImageHelper;
import ru.hoprik.player.utils.MusicInfo;
import ru.hoprik.player.utils.UpdateManager;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerUI extends BaseFragment {
    PlayerBackgroundView backgroundView;
    GradientDrawable overlayColor;
    List<MessageObject> playlist;
    PlaylistContainerView playlistContainerView;
    PrimaryControlsView primaryControlsView;

    UpdateManager manager;

    boolean enableShuffle;
    boolean enableDownload;
    boolean enableShare;
    boolean enableSave;
    boolean enableSaveProfile;

    private MotionBackgroundDrawable motionBackgroundDrawable;

    public MusicPlayerUI() {
        this.enableShuffle = MusicPlayer.getInstance().isFeatureEnabled("enable_feature_shuffle", true);
        this.enableDownload = MusicPlayer.getInstance().isFeatureEnabled("enable_feature_download", true);
        this.enableShare = MusicPlayer.getInstance().isFeatureEnabled("enable_feature_share", true);
        this.enableSave = MusicPlayer.getInstance().isFeatureEnabled("enable_feature_save", true);
        this.enableSaveProfile = MusicPlayer.getInstance().isFeatureEnabled("enable_feature_save_profile", true);
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
        fragmentView = new SwipeBackLayout(context, this);
        FrameLayout container = (FrameLayout) fragmentView;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN &&
                    event.getX() < AndroidUtilities.dp(20)) {

                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });


        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        scrollView.setLayoutParams(scrollParams);

        MusicInfo info = new MusicInfo(MediaController.getInstance().getPlayingMessageObject());
        if (info.getMessageObject() == null) {
            renderError(container, context);
            return fragmentView;
        }

        playlist = MediaController.getInstance().getPlaylist();

        LinearLayout main_layout = new LinearLayout(context);
        main_layout.setOrientation(LinearLayout.VERTICAL);
        main_layout.setPadding(
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20),
                AndroidUtilities.dp(20)
        );
        main_layout.setLayoutParams(
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int statusBarHeight = ActionBar.getCurrentActionBarHeight();
            main_layout.setPadding(
                    AndroidUtilities.dp(20),
                    AndroidUtilities.dp(statusBarHeight),
                    AndroidUtilities.dp(20),
                    AndroidUtilities.dp(20)
            );
            return ViewCompat.onApplyWindowInsets(v, insets);
        });

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerParams.setMargins(0, ActionBar.getCurrentActionBarHeight() / 2, 0, AndroidUtilities.dp(10));
        headerLayout.setLayoutParams(headerParams);

        ImageView backButton = new ImageView(context);
        backButton.setOnClickListener(view -> finishFragment());
        backButton.setImageResource(R.drawable.ic_ab_back);
        backButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_player_actionBarSelector)));
        backButton.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        backButton.setColorFilter(Theme.getColor(Theme.key_player_actionBarItems));

        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        backButton.setLayoutParams(backParams);

        TextView headerTitle = new TextView(context);
        String playingText = MusicPlayer.getInstance().getString("now_playing");
        headerTitle.setText(TextUtils.isEmpty(playingText) ? "Now Playing" : playingText);
        headerTitle.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        headerTitle.setTextSize(16);
        headerTitle.setTypeface(AndroidUtilities.bold());
        headerTitle.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        headerTitle.setLayoutParams(titleParams);

        View rightPlaceholder = new View(context);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(44),
                AndroidUtilities.dp(44)
        );
        rightPlaceholder.setLayoutParams(rightParams);

        headerLayout.addView(backButton);
        headerLayout.addView(headerTitle);
        headerLayout.addView(rightPlaceholder);

        main_layout.addView(headerLayout);

        backgroundView = new PlayerBackgroundView(context);
        container.addView(backgroundView);

        CoverLayout avatarCover = new CoverLayout(context) {
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
        avatarCover.setOnSwipeListeners(
                () -> {
                    if (primaryControlsView != null) {
                        primaryControlsView.setPlayPauseState(!MediaController.getInstance().isMessagePaused(), false);
                    }
                    refreshPlaylistIfReady();
                    if (manager != null) manager.updateUI();
                },
                () -> {
                    if (primaryControlsView != null) {
                        primaryControlsView.setPlayPauseState(!MediaController.getInstance().isMessagePaused(), false);
                    }
                    refreshPlaylistIfReady();
                    if (manager != null) manager.updateUI();
                }
        );
        main_layout.addView(avatarCover);

        SongInfoView songInfoView = new SongInfoView(context, info);
        main_layout.addView(songInfoView);

        PlayerSeekBarView playerSeekBarView = new PlayerSeekBarView(context, info);
        main_layout.addView(playerSeekBarView);

        primaryControlsView = new PrimaryControlsView(context, this.enableShuffle, () -> {
            refreshPlaylistIfReady();
            if (manager != null) manager.updateUI();
        });
        main_layout.addView(primaryControlsView);

        View space = new View(context);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(40)
        ));
        main_layout.addView(space);

        SecondaryControlsView secondaryControlsView = new SecondaryControlsView(context, getElements(info));
        main_layout.addView(secondaryControlsView);

        ImageHelper.updateCover(info.getMessageObject(), avatarCover, false);
        ImageHelper.updateCover(info.getMessageObject(), backgroundView.getBackgroundImage(), true);

        this.manager = new UpdateManager(
                info,
                songInfoView.getSongView(),
                songInfoView.getAuthorView(),
                avatarCover,
                backgroundView.getBackgroundImage(),
                overlayColor,
                playerSeekBarView.getCurrentTimeView(),
                playerSeekBarView.getRemainingTimeView(),
                playerSeekBarView.getSeekBar(),
                this::refreshPlaylistIfReady
        );
        playerSeekBarView.setManager(this.manager);
        this.manager.startUpdater();

        View space2 = new View(context);
        space2.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(40)
        ));
        main_layout.addView(space2);

        playlistContainerView = new PlaylistContainerView(context, playlist, message -> {
            MediaController.getInstance().playMessage(message);
            if (primaryControlsView != null) {
                primaryControlsView.setPlayPauseState(!MediaController.getInstance().isMessagePaused(), false);
            }
            refreshPlaylistIfReady();
            if (manager != null) manager.updateUI();
        });
        main_layout.addView(playlistContainerView);

        TextView bottomText = new TextView(context);
        bottomText.setText(LocaleUtils.fullyFormatText("Сделано с ❤️ от @hoprik и fork by @tecxz5 для exteragram"));
        bottomText.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
        bottomText.setTextSize(12);
        bottomText.setGravity(Gravity.CENTER);
        bottomText.setAlpha(0.6f);
        
        LinearLayout.LayoutParams bottomTextParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bottomTextParams.setMargins(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(40));
        bottomText.setLayoutParams(bottomTextParams);
        
        main_layout.addView(bottomText);

        View bottomSpacer = new View(context);
        bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(20)
        ));
        main_layout.addView(bottomSpacer);

        scrollView.addView(main_layout);
        container.addView(scrollView);

        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (playlist.size() >= 5) {
                float maxScroll = AndroidUtilities.dp(200);
                float progress = Math.min(1f, scrollY / maxScroll);

                float scale = 1f - (progress * 0.8f);
                avatarCover.setScaleX(scale);
                avatarCover.setScaleY(scale);
                avatarCover.setRoundRadius((int) (10 + (progress)));

                avatarCover.setTranslationY(-scrollY * 0.2f);
            }
        });

        return fragmentView;
    }

    @Override
    public void onFragmentClosed() {
        if (this.manager != null) {
            this.manager.stopUpdater();
        }
    }

    private void refreshPlaylistIfReady() {
        if (playlistContainerView != null) {
            playlistContainerView.refreshPlaylistUI();
        }
    }

    private List<ControlsElement> getElements(MusicInfo info) {
        List<ControlsElement> list = new ArrayList<>();
        if (enableSaveProfile) {
            list.add(new ControlsElement(
                    () -> {
                        MessageObject object = MediaController.getInstance().getPlayingMessageObject();
                        if (object == null || object.getDocument() == null) return;
                        
                        int currentAccount = UserConfig.selectedAccount;
                        MessagesController.SavedMusicIds musicIds = MessagesController.getInstance(currentAccount).getSavedMusicIds();
                        final long documentId = object.getDocument().id;
                        boolean isSaved = musicIds.ids.contains(documentId);
                        
                        ControlsHelpers.saveToProfile(object, !isSaved, () -> {}, false, this);
                        if (!isSaved) {
                            BulletinFactory.of(this)
                                    .createSimpleBulletin(R.raw.saved_messages, LocaleController.getString(R.string.AudioSaveToMyProfileSaved))
                                    .show();
                        }else{
                            BulletinFactory.of(this)
                                    .createSimpleBulletin(R.raw.ic_delete, LocaleController.getString(R.string.AudioSaveToMyProfileUnsaved))
                                    .show();
                        }
                    },
                    R.drawable.filled_widget_music,
                    false
            ));
        }
        if (enableDownload) {
            list.add(new ControlsElement(
                    () -> {
                        ControlsHelpers.saveToMusic(info.getMessageObject(), this.getParentActivity());
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_download, MusicPlayer.getInstance().getString("downloaded")).show(true);
                    },
                    R.drawable.msg_download,
                    false
            ));
        }
        if (enableShare) {
            list.add(new ControlsElement(
                    () -> {
                        ControlsHelpers.share(info.getMessageObject(), this.getParentActivity());
                    },
                    R.drawable.share,
                    false
            ));
        }
        if (enableSave) {
            list.add(new ControlsElement(
                    () -> {
                        ControlsHelpers.forward(info.getMessageObject(), UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId());
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_save_to_music, MusicPlayer.getInstance().getString("saved")).show(true);
                    },
                    R.drawable.msg_save_story,
                    false
            ));
        }
        if (isLyricsActivityAvailable()) {
            list.add(new ControlsElement(
                    () -> {
                        try {
                            Class<?> lyricsClass = MusicPlayer.getInstance().getLyricsClass();
                            Object instance = lyricsClass.getDeclaredConstructor().newInstance();
                            this.presentFragment((BaseFragment) instance);

                        } catch (Exception e) {
                            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, MusicPlayer.getInstance().getString("start_error")).show(true);
                            e.printStackTrace();
                        }
                    },
                    R.drawable.msg_photo_text2,
                    false
            ));
        }


        return list;
    }

    private boolean isLyricsActivityAvailable() {
        return MusicPlayer.getInstance().getLyricsClass() != null;
    }

    private void renderError(FrameLayout container, Context context) {
        TextView textView = new TextView(context);
        textView.setText(MusicPlayer.getInstance().getString("no_music"));
        textView.setTextSize(24);
        textView.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        textView.setGravity(Gravity.CENTER);

        container.addView(textView, LayoutHelper.createFrame(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

    }
}

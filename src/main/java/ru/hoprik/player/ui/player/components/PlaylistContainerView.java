package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RLottieImageView;
import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.holder.Playlist;
import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Track;
import ru.hoprik.player.helpers.NotificationCenterCodes;

// import ru.hoprik.player.helpers.ImageHelper;
// import ru.hoprik.player.helpers.MusicInfo;

import java.util.ArrayList;
import java.util.List;

import static android.widget.LinearLayout.VERTICAL;

public class PlaylistContainerView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private final RecyclerView recyclerView;
    private final PlaylistAdapter adapter;
    private final PlaylistListener playlistListener;


    public interface PlaylistListener {
        void onTrackClicked(MessageObject message);
    }

    public PlaylistContainerView(Context context, PlaylistListener listener) {
        super(context);
        this.playlistListener = listener;
        register();

        setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        ));

        // Фон
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x1A000000);
        background.setCornerRadius(AndroidUtilities.dp(16));
        setBackground(background);
        setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

        // Ванильный RecyclerView
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, VERTICAL, false));
        recyclerView.setClipToPadding(false);
        // Убираем стандартные анимации мерцания при обновлении
        recyclerView.setItemAnimator(null);

        addView(recyclerView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        adapter = new PlaylistAdapter(MusicPlayer.getInstance().getAudioPlayer().getPlaylist());
        recyclerView.setAdapter(adapter);
    }

    public void updatePlaylistSelection() {
        MessageObject currentPlaying = MediaController.getInstance().getPlayingMessageObject();

        // Быстрое обновление только видимых элементов без пересборки всего списка
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof TrackViewHolder) {
                ((TrackViewHolder) holder).updateState(currentPlaying);
            }
        }
    }

    public void refreshPlaylistUI() {
        Playlist currentPlaylist = MusicPlayer.getInstance().getAudioPlayer().getPlaylist();
        if (currentPlaylist == null) {
            updatePlaylistSelection();
            return;
        }

        for (AudioElement element : currentPlaylist.getElements()) {
            Log.d("PlaylistContainerView", "Element: " + element.getAudio().getId());
        }

        adapter.update(currentPlaylist);
        updatePlaylistSelection();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    public void register() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenterCodes.updateMediaPlaylistUI);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
    }

    public void destroy() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenterCodes.updateMediaPlaylistUI);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
    }

    @Override
    public void didReceivedNotification(int i, int i1, Object... objects) {
        if (i == NotificationCenterCodes.updateMediaPlaylistUI) {
            refreshPlaylistUI();
        }else if (i == NotificationCenter.messagePlayingDidStart || i == NotificationCenter.messagePlayingDidReset || i == NotificationCenter.messagePlayingPlayStateChanged){
            updatePlaylistSelection();
        }

    }

    // ==========================================
    // ВАНИЛЬНЫЙ ADAPTER
    // ==========================================
    // ==========================================
    // ВАНИЛЬНЫЙ ADAPTER (Адаптированный под форк Telegram)
    // ==========================================
    private class PlaylistAdapter extends RecyclerView.Adapter { // <-- Убрали <RecyclerView.ViewHolder>

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_TRACK = 1;

        private Playlist playlist;

        public PlaylistAdapter(Playlist playlist) {
            this.playlist = playlist;
        }

        public void update(Playlist playlist) {
            this.playlist = playlist;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            if (playlist == null || playlist.getElements() == null) {
                return 0;
            }
            int count = playlist.getElements().size() + 1;
            Log.d("PlaylistAdapter_UI", "Адаптер сообщает размер: " + count);
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_TRACK;
        }

        // Обратите внимание: @NonNull тоже лучше убрать, так как в форке Telegram
        // у базовых методов их может не быть, и компилятор будет ругаться.
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            if (viewType == TYPE_HEADER) {
                TextView titleView = new TextView(context);
                titleView.setText("Плейлист");
                titleView.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
                titleView.setTextSize(14);
                titleView.setTypeface(AndroidUtilities.bold());
                titleView.setAlpha(0.8f);
                titleView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
                titleView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                return new HeaderViewHolder(titleView);
            } else {
                return new TrackViewHolder(context, playlistListener);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Log.d("PlaylistAdapter_UI", "Отрисовка позиции: " + position);
            if (holder instanceof TrackViewHolder) {
                if (playlist == null) return;
                AudioElement message = playlist.getElements().get(position - 1);
                ((TrackViewHolder) holder).bind(message);
            }
        }
    }

    // ==========================================
    // ВАНИЛЬНЫЕ VIEWHOLDERS
    // ==========================================
    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class TrackViewHolder extends RecyclerView.ViewHolder {

        private final CoverLayout cover;
        private final SimpleTextView title;
        private final SimpleTextView author;
        private final TextView duration;
        private final RLottieImageView playingIcon;

        private MessageObject currentMessage;

        public TrackViewHolder(Context context, PlaylistListener listener) {
            super(createView(context));

            LinearLayout container = (LinearLayout) itemView;
            cover = (CoverLayout) container.getChildAt(0);
            LinearLayout textContainer = (LinearLayout) container.getChildAt(1);
            title = (SimpleTextView) textContainer.getChildAt(0);
            author = (SimpleTextView) textContainer.getChildAt(1);
            duration = (TextView) container.getChildAt(2);
            playingIcon = (RLottieImageView) container.getChildAt(3);

            // Обработка клика вешается один раз при создании холдера (Best Practice)
            container.setOnClickListener(v -> {
                if (listener != null && currentMessage != null) {
                    listener.onTrackClicked(currentMessage);
                }
            });
        }

        private static View createView(Context context) {
            LinearLayout itemView = new LinearLayout(context);
            itemView.setOrientation(LinearLayout.HORIZONTAL);
            itemView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
            itemView.setBackground(Theme.createSelectorDrawable(0x1A000000));
            itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            CoverLayout cover = new CoverLayout(context, true);
            cover.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48)));
            cover.setRoundRadius(AndroidUtilities.dp(12));
            cover.setAspectFit(true);
            itemView.addView(cover);

            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(VERTICAL);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            textContainer.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);

            SimpleTextView title = new SimpleTextView(context);
            title.setTextSize(16);
            title.setScrollNonFitText(true);
            textContainer.addView(title);

            SimpleTextView author = new SimpleTextView(context);
            author.setTextSize(14);
            author.setScrollNonFitText(true);
            textContainer.addView(author);

            itemView.addView(textContainer);

            TextView duration = new TextView(context);
            duration.setTextSize(14);
            duration.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            duration.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(60), ViewGroup.LayoutParams.WRAP_CONTENT));
            itemView.addView(duration);

            RLottieImageView playingIcon = new RLottieImageView(context);
            playingIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER);
            playingIcon.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24)));
            playingIcon.setVisibility(View.GONE);
            itemView.addView(playingIcon);

            return itemView;
        }

        public void bind(AudioElement message) {
            this.currentMessage = message.getAudio();

            Track info = message.getTrack();
            title.setText(info.getName());
            author.setText(info.getArtists().stream().map(Artist::getName).reduce((a, b) -> a + ", " + b).orElse(""));
            duration.setText(AndroidUtilities.formatLongDuration(info.getDuration()));
            cover.setupCover(info.getCover());

            // Проверяем статус при скролле
            updateState(MediaController.getInstance().getPlayingMessageObject());
        }

        public void updateState(MessageObject currentPlaying) {
            if (currentMessage == null) return;

            boolean isPlaying = (currentMessage == currentPlaying);
            if (isPlaying) {
                title.setTypeface(AndroidUtilities.bold());
                title.setTextColor(Theme.getColor(Theme.key_player_buttonActive));
                author.setTypeface(AndroidUtilities.bold());
                author.setTextColor(Theme.getColor(Theme.key_player_buttonActive));
                duration.setTextColor(Theme.getColor(Theme.key_player_buttonActive));
                playingIcon.setVisibility(View.VISIBLE);
            } else {
                title.setTypeface(AndroidUtilities.bold());
                title.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
                author.setTypeface(null);
                author.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
                duration.setTextColor(Theme.getColor(Theme.key_player_time));
                playingIcon.setVisibility(View.GONE);
            }
        }
    }
}
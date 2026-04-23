package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.RLottieImageView;

import ru.hoprik.player.utils.ImageHelper;
import ru.hoprik.player.utils.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class PlaylistContainerView extends LinearLayout {

    private List<TrackItemHolder> playlistHolders = new ArrayList<>();
    private final TextView playlistTitle;
    private final PlaylistListener playlistListener;

    public interface PlaylistListener {
        void onTrackClicked(MessageObject message);
    }

    public PlaylistContainerView(Context context, List<MessageObject> playlist, PlaylistListener listener) {
        super(context);
        this.playlistListener = listener;
        setOrientation(LinearLayout.VERTICAL);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0x1A000000); // Semi-transparent black 10%
        background.setCornerRadius(AndroidUtilities.dp(16));
        setBackground(background);
        setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        setLayoutParams(params);

        playlistTitle = new TextView(context);
        playlistTitle.setText("Плейлист");
        playlistTitle.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        playlistTitle.setTextSize(14);
        playlistTitle.setTypeface(AndroidUtilities.bold());
        playlistTitle.setAlpha(0.8f);
        playlistTitle.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        rebuildPlaylist(playlist);
    }

    public void updatePlaylistSelection() {
        MessageObject currentPlaying = MediaController.getInstance().getPlayingMessageObject();
        for (TrackItemHolder holder : playlistHolders) {
            boolean isPlaying = (holder.message == currentPlaying);
            if (isPlaying) {
                holder.title.setTypeface(AndroidUtilities.bold());
                holder.title.setTextColor(Theme.getColor(Theme.key_player_buttonActive));
                holder.author.setTypeface(AndroidUtilities.bold());
                holder.author.setTextColor(Theme.getColor(Theme.key_player_buttonActive));
                holder.duration.setTextColor(Theme.getColor(Theme.key_player_buttonActive));
                holder.playingIcon.setVisibility(View.VISIBLE);
            } else {
                holder.title.setTypeface(AndroidUtilities.bold());
                holder.title.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
                holder.author.setTypeface(null);
                holder.author.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
                holder.duration.setTextColor(Theme.getColor(Theme.key_player_time));
                holder.playingIcon.setVisibility(View.GONE);
            }
        }
    }

    public void refreshPlaylistUI() {
        List<MessageObject> currentPlaylist = MediaController.getInstance().getPlaylist();
        if (currentPlaylist == null) {
            updatePlaylistSelection();
            return;
        }

        // Skip heavy rebuild when playlist size is unchanged.
        if (currentPlaylist.size() == playlistHolders.size()) {
            updatePlaylistSelection();
            return;
        }

        rebuildPlaylist(currentPlaylist);
    }

    private void rebuildPlaylist(List<MessageObject> playlist) {
        playlistHolders.clear();
        removeAllViews();
        addView(playlistTitle);

        for (MessageObject message : playlist) {
            LinearLayout trackItem = createTrackItemView(getContext(), message, playlistListener);
            addView(trackItem);
        }

        updatePlaylistSelection();
        requestLayout();
        invalidate();
    }

    private LinearLayout createTrackItemView(Context context, MessageObject message, PlaylistListener listener) {
        LinearLayout itemView = new LinearLayout(context);
        itemView.setOrientation(LinearLayout.HORIZONTAL);
        itemView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12),
                AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        itemView.setBackground(Theme.createSelectorDrawable(0x1A000000));
        itemView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        BackupImageView cover = new CoverLayout(context, true);
        cover.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48)));
        ((CoverLayout) cover).setRoundRadius(AndroidUtilities.dp(12));
        ((CoverLayout) cover).setAspectFit(true);
        itemView.addView(cover);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textContainer.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);

        SimpleTextView title = new SimpleTextView(context);
        title.setTextSize(16);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        title.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        title.setScrollNonFitText(true);
        textContainer.addView(title);

        SimpleTextView author = new SimpleTextView(context);
        author.setTextSize(14);
        author.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
        author.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        author.setScrollNonFitText(true);
        textContainer.addView(author);

        itemView.addView(textContainer);

        TextView duration = new TextView(context);
        duration.setTextSize(14);
        duration.setTextColor(Theme.getColor(Theme.key_player_time));
        duration.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        duration.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(60), ViewGroup.LayoutParams.WRAP_CONTENT));
        itemView.addView(duration);

        RLottieImageView playingIcon = new RLottieImageView(context);
        playingIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        playingIcon.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24)));
        playingIcon.setVisibility(View.GONE);
        itemView.addView(playingIcon);

        MusicInfo info = new MusicInfo(message);
        ImageHelper.updateCover(message, cover, false);
        title.setText(info.getCurrentTitle());
        author.setText(info.getCurrentAuthor());
        duration.setText(info.getAudioProgressString());

        TrackItemHolder holder = new TrackItemHolder();
        holder.container = itemView;
        holder.cover = cover;
        holder.title = title;
        holder.author = author;
        holder.duration = duration;
        holder.playingIcon = playingIcon;
        holder.message = message;
        playlistHolders.add(holder);

        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTrackClicked(message);
            }
        });

        return itemView;
    }

    private static class TrackItemHolder {
        LinearLayout container;
        BackupImageView cover;
        SimpleTextView title;
        SimpleTextView author;
        TextView duration;
        RLottieImageView playingIcon;
        MessageObject message;
    }
}


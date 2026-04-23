package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;

import ru.hoprik.player.utils.MusicInfo;

public class SongInfoView extends LinearLayout {

    private SimpleTextView songView;
    private TextView authorView;

    public SongInfoView(Context context, MusicInfo musicInfo) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
        setLayoutParams(layoutParams);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, AndroidUtilities.dp(5), 0, 0);

        this.songView = new SimpleTextView(context);
        this.songView.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        this.songView.setTextSize(20);
        this.songView.setTypeface(AndroidUtilities.bold());
        this.songView.setText(musicInfo.getCurrentTitle());
        this.songView.setLayoutParams(textParams);
        this.songView.setScrollNonFitText(true);

        this.authorView = new TextView(context);
        this.authorView.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
        this.authorView.setTextSize(16);
        this.authorView.setSingleLine(true);
        this.authorView.setEllipsize(TextUtils.TruncateAt.END);
        this.authorView.setText(musicInfo.getCurrentAuthor());
        this.authorView.setAlpha(0.8f);
        this.authorView.setLayoutParams(textParams);

        addView(this.songView);
        addView(this.authorView);
    }

    public SimpleTextView getSongView() {
        return songView;
    }

    public TextView getAuthorView() {
        return authorView;
    }
}


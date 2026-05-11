package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import ru.hoprik.player.audio.objects.Artist;

public class ArtistTextView extends TextView {
    public ArtistTextView(Context context, Artist artist) {
        super(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, 0, AndroidUtilities.dp(3), 0);

        setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
        setTextSize(16);
        setSingleLine(true);
        setEllipsize(TextUtils.TruncateAt.END);
        setAlpha(0.8f);
        setLayoutParams(textParams);
        setText(artist.getName());
    }
}

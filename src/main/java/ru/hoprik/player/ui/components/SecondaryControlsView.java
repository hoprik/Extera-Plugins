package ru.hoprik.player.ui.components;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RLottieImageView;

import java.util.List;

public class SecondaryControlsView extends LinearLayout {

    public SecondaryControlsView(Context context, List<ControlsElement> elements) {
        super(context);
        int iconColor = Theme.getColor(Theme.key_player_button);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        setLayoutParams(new FrameLayout.LayoutParams(
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

        for (ControlsElement element : elements) {
            RLottieImageView buttonElement = new RLottieImageView(context);
            buttonElement.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            if (element.isAnimation()) {
                buttonElement.setAnimation(element.getIconId(), 36, 36);
            } else {
                buttonElement.setImageResource(element.getIconId());
                buttonElement.setColorFilter(iconColor);
            }
            buttonElement.setLayoutParams(buttonsParams);

            buttonElement.setOnClickListener(view -> new Handler(Looper.getMainLooper()).post(element.getRunnable()));

            addView(buttonElement);
        }
    }
}


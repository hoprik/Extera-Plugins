package ru.hoprik.player.ui.settings.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.GLIcon.GLIconRenderer;
import org.telegram.ui.Components.Premium.GLIcon.GLIconTextureView;
import org.telegram.ui.Components.Premium.StarParticlesView;
import ru.hoprik.player.MusicPlayer;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static android.widget.ListPopupWindow.MATCH_PARENT;
import static android.widget.ListPopupWindow.WRAP_CONTENT;
import static org.telegram.messenger.AndroidUtilities.dp;

public class CustomHeader extends FrameLayout {
    StarParticlesView starParticlesView;
    GLIconTextureView iconTextureView;
    private final Theme.ResourcesProvider resourcesProvider;

    public CustomHeader(Context context, BaseFragment fragment) {
        super(context);
        this.resourcesProvider = fragment.getResourceProvider();
        init(context);
    }

    private void init(Context context) {
        starParticlesView = new StarParticlesView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                drawable.rect2.set(0, 0, getMeasuredWidth(), getMeasuredHeight() - dp(52));
            }

            @Override
            protected void configure() {
                drawable.useGradient = true;
                drawable.useBlur = false;
                drawable.checkBounds = true;
                drawable.isCircle = true;
                drawable.centerOffsetY = dp(-14);
                drawable.minLifeTime = 2000;
                drawable.randLifeTime = 3000;
                drawable.size1 = 16;
                drawable.useRotate = false;
                drawable.type = 28;
                drawable.colorKey = Theme.key_premiumGradient2;
                drawable.init();
            }
        };
        addView(starParticlesView, LayoutHelper.createFrame(MATCH_PARENT, 190, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        iconTextureView = new GLIconTextureView(context, 1, 3) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                setPaused(false);
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                setPaused(true);
            }
        };
        iconTextureView.setStarParticlesView(starParticlesView);
        Bitmap bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(ColorUtils.blendARGB(Theme.getColor(Theme.key_premiumGradient2, resourcesProvider), Theme.getColor(Theme.key_dialogBackground, resourcesProvider), 0.5f));
        iconTextureView.setBackgroundBitmap(bitmap);
        iconTextureView.mRenderer.colorKey1 = Theme.key_premiumGradient2;
        iconTextureView.mRenderer.colorKey2 = Theme.key_premiumGradient1;
        iconTextureView.mRenderer.updateColors();

        addView(iconTextureView, LayoutHelper.createFrame(160, 160, Gravity.CENTER_HORIZONTAL));

        if (iconTextureView != null) {
            iconTextureView.startEnterAnimation(-360, 100);
        }

        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        titleView.setText("DotFi");
        titleView.setGravity(Gravity.CENTER);
        addView(titleView, LayoutHelper.createFrame(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 33, 150, 33, 0));

        TextView subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        subtitleView.setText(MusicPlayer.getInstance().getString("description"));
        subtitleView.setGravity(Gravity.CENTER);
        addView(subtitleView, LayoutHelper.createFrame(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 33, 183, 33, 20));
    }


}
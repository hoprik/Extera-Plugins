package ru.hoprik.pillmusic.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import androidx.core.content.ContextCompat;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import ru.hoprik.pillmusic.controller.SaveManager;
import ru.hoprik.pillmusic.ui.dialogs.InputAuthDialog;
import ru.hoprik.pillmusic.ui.dialogs.YandexAuthDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PillStatsFMSettings extends BasePreferencesActivity {

    // === Данные о сервисах ===
    private static class ServiceInfo {
        String name;
        int iconRes;
        int iconColorTop;
        int iconColorBottom;

        ServiceInfo(String name, int iconRes, int colorTop, int colorBottom) {
            this.name = name;
            this.iconRes = iconRes;
            this.iconColorTop = colorTop;
            this.iconColorBottom = colorBottom;
        }
    }

    private final HashMap<Integer, ServiceInfo> serviceDetails = new HashMap<>();
    private ActionBarMenuItem resetItem;
    private Drawable reorderIcon;
    private int activeSectionId = -1;
    private int hiddenSectionId = -1;

    // Статические списки (будут загружены из Preferences)
    public static ArrayList<Integer> activeServices = new ArrayList<>();
    public static ArrayList<Integer> hiddenServices = new ArrayList<>();
    public static Map<Integer, String> tokens = new HashMap<>();

    private static final int SERVICE_STATS_FM = 1;
    private static final int SERVICE_YANDEX = 2;
    private static final int SERVICE_VK = 4;
    private static final int SERVICE_TELEGRAM = 5;
    private static final int SERVICE_LASTFM = 6;
    private static final String PREF_UPDATE_INTERVAL = "pill_music_update_interval";
    private int updateIntervalSeconds = 10; // значение по умолчанию

    @Override
    protected void initializeOptionStrings() {
        initServiceDetails();
        loadFromPreferences();
    }

    @Override
    protected void onClick(UItem uItem, View view, int i, float v, float v1) {
        int id = uItem.id;
        // Перемещение сервиса между активными и неактивными
        if (activeServices.contains(id)) {
            activeServices.remove((Integer) id);
            if (!hiddenServices.contains(id)) {
                hiddenServices.add(0, id); // добавляем в начало скрытых
            }
        } else if (hiddenServices.contains(id)) {
            hiddenServices.remove((Integer) id);
            activeServices.add(id);
            if (tokens.containsKey(id)) {
                openDialogs(id);
            }
        }
        saveToPreferences();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        updateResetButtonVisibility();
    }

    private void initServiceDetails() {
        serviceDetails.put(SERVICE_STATS_FM, new ServiceInfo("Stats.fm", R.drawable.files_music, 0xFF1DB954, 0xFF191414));
        serviceDetails.put(SERVICE_YANDEX, new ServiceInfo("Яндекс Музыка", R.drawable.files_music, 0xFFFC3F5E, 0xFF000000));
        serviceDetails.put(SERVICE_LASTFM, new ServiceInfo("Last.fm", R.drawable.files_music, 0xFFD51007, 0xFF000000));
    }

    private void loadFromPreferences() {
        activeServices.clear();
        hiddenServices.clear();
        SaveManager.SaveData saveData = SaveManager.getInstance().getSaveData();
        if (saveData.getEnabled() != null) {
            activeServices.addAll(saveData.getEnabled());
        }
        if (saveData.getDisabled() != null) {
            hiddenServices.addAll(saveData.getDisabled());
        }
        if (saveData.getTokens() != null) {
            tokens.putAll(saveData.getTokens());
        }
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        Log.d("PillMusic", "Loading: active=" + activeServices + ", hidden=" + hiddenServices);

        updateResetButtonVisibility();
    }

    private void saveToPreferences() {
        SaveManager.SaveData saveData = SaveManager.getInstance().getSaveData();
        saveData.setEnabled(new ArrayList<>(activeServices));
        saveData.setDisabled(new ArrayList<>(hiddenServices));
        saveData.setTokens(new HashMap<>(tokens));
        Log.d("PillMusic", "Saved: active=" + activeServices + ", hidden=" + hiddenServices);
        SaveManager.getInstance().save(saveData);
    }

    @Override
    public View createView(Context context) {
        View createView = super.createView(context);
        // Кнопка сброса
        resetItem = actionBar.createMenu().addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString(R.string.Reset));
        resetItem.setOnClickListener(v -> resetToDefault());
        updateResetButtonVisibility();

        if (listView != null) {
            listView.allowReorder(true);
            listView.listenReorder((fromPos, items) -> updateConfigFromReorder((Integer) fromPos, (ArrayList<org.telegram.ui.Components.UItem>) items));
        }
        return createView;
    }

    @Override
    protected void fillItems(ArrayList items, UniversalAdapter adapter) {
        if (reorderIcon == null) {
            reorderIcon = ContextCompat.getDrawable(getContext(), R.drawable.media_more);
        }

        // === Активные сервисы ===
        if (!activeServices.isEmpty()) {
            activeSectionId = addServiceSection(items, adapter, "Активные сервисы", activeServices);
            items.add(UItem.asShadow("Перетащите для изменения порядка, нажмите для перемещения в неактивные"));
        }

        // === Неактивные сервисы ===
        if (!hiddenServices.isEmpty()) {
            hiddenSectionId = addServiceSection(items, adapter, "Неактивные сервисы", hiddenServices);
            if (activeServices.isEmpty()) {
                items.add(UItem.asShadow("Нажмите на сервис, чтобы добавить его в активные"));
            }
        }

        items.add(UItem.asHeader("Интервал обновления"));
        final SeekBar seekBar = new SeekBar(getContext());
        seekBar.setMax(59); // от 1 до 60
        seekBar.setProgress(updateIntervalSeconds - 1);
        seekBar.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        UItem intervalItem = UItem.asCustom(9999, seekBar);
        items.add(intervalItem);
        items.add(UItem.asShadow(null));
    }

    private void updateResetButtonVisibility() {
        if (resetItem == null) return;
        boolean isDefault = activeServices.equals(getDefaultActiveServices());
        if (!isDefault && resetItem.getVisibility() == View.GONE) {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
        } else if (isDefault && resetItem.getVisibility() == View.VISIBLE) {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
        }
    }

    private ArrayList<Integer> getDefaultActiveServices() {
        ArrayList<Integer> def = new ArrayList<>();
        def.add(SERVICE_STATS_FM);
        def.add(SERVICE_YANDEX);
        def.add(SERVICE_VK);
        def.add(SERVICE_TELEGRAM);
        def.add(SERVICE_LASTFM);
        return def;
    }

    private void resetToDefault() {
        SaveManager.getInstance().reset();
        loadFromPreferences();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        updateResetButtonVisibility();
    }

    private int addServiceSection(ArrayList<UItem> items, UniversalAdapter adapter, String title, ArrayList<Integer> serviceList) {
        adapter.whiteSectionStart();
        items.add(UItem.asHeader(title));
        int startPos = adapter.reorderSectionStart();
        for (int id : serviceList) {
            ServiceInfo info = serviceDetails.get(id);
            if (info != null) {
                UItem item = UItem.asButton(id, info.iconRes, info.name);
                item.object2 = reorderIcon;
                item.clickCallback2 = v -> {openDialogs(id);};
                item.bind = v -> {
                    if (v instanceof TextCell) {
                        TextCell cell = (TextCell) v;
                        cell.setColorfulIcon(info.iconColorTop, info.iconColorBottom, info.iconRes, true);
                        ImageView icon = cell.getValueImageView();
                        if (icon != null) {
                            icon.setOnClickListener(ignored -> {openDialogs(id);});
                        }
                    }
                };
                items.add(item);
            }
        }
        adapter.reorderSectionEnd();
        adapter.whiteSectionEnd();
        return startPos;
    }

    public void openDialogs(int id){
        switch (id){
            case SERVICE_YANDEX:
                YandexAuthDialog dialog = new YandexAuthDialog(getContext(), token -> {
                    if (!token.isEmpty()){
                        tokens.put(SERVICE_YANDEX, token);
                        saveToPreferences();
                    }
                });
                dialog.show();
                break;
            case SERVICE_LASTFM:
                InputAuthDialog authDialog = new InputAuthDialog(getContext(), "Last.fm", tokens.getOrDefault(SERVICE_LASTFM, ""), token -> {
                    if (!token.isEmpty()){
                        tokens.put(SERVICE_LASTFM, token);
                        saveToPreferences();
                    }
                });
                authDialog.show();
                break;
            case SERVICE_STATS_FM:
                InputAuthDialog authDialog2 = new InputAuthDialog(getContext(), "Stats.fm", tokens.getOrDefault(SERVICE_STATS_FM, ""), token -> {
                    if (!token.isEmpty()){
                        tokens.put(SERVICE_STATS_FM, token);
                        saveToPreferences();
                    }
                });
                authDialog2.show();
                break;
        }
    }

    public void updateConfigFromReorder(int sectionStart, ArrayList<UItem> items) {
        ArrayList<Integer> newOrder = new ArrayList<>();
        for (UItem u : items) {
            newOrder.add(u.id);
        }
        if (sectionStart == activeSectionId) {
            activeServices.clear();
            activeServices.addAll(newOrder);
        } else if (sectionStart == hiddenSectionId) {
            hiddenServices.clear();
            hiddenServices.addAll(newOrder);
        }
        saveToPreferences();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
        updateResetButtonVisibility();
    }

    @Override
    public String getTitle() {
        return "Hoprik's Pill Music";
    }
}
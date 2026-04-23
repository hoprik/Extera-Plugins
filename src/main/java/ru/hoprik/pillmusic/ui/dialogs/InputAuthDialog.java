package ru.hoprik.pillmusic.ui.dialogs;

import android.content.Context;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.EditTextBoldCursor;

public class InputAuthDialog {
    private final Context context;
    private final String serviceName;
    private final DialogResponse response;
    private final String oldValue;

    public InputAuthDialog(Context context, String serviceName, String oldValue, DialogResponse response) {
        this.context = context;
        this.serviceName = serviceName;
        this.response = response;
        this.oldValue = oldValue;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(serviceName);
        builder.setMessage("Введите ваш никнейм (Username):");

        // Создаем поле ввода в стиле Telegram
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(18);
        editText.setTextColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextBlack));
        editText.setHintColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_dialogTextHint));
        editText.setHint("Username");
        editText.setText(oldValue);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setCursorColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);

        // Контейнер с отступами
        FrameLayout container = new FrameLayout(context);
        container.addView(editText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER));
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), 0);

        builder.setView(container);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String input = editText.getText().toString().trim();
            if (response != null) {
                response.onGetToken(input);
            }
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", org.telegram.messenger.R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Автоматически показываем клавиатуру
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }
}
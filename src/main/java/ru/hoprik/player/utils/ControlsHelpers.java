package ru.hoprik.player.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.FrameLayout;
import androidx.core.content.FileProvider;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.TopicsFragment;

import java.io.File;
import java.util.ArrayList;

public class ControlsHelpers {
    public static void saveToMusic(MessageObject messageObject, Activity parentActivity) {
        if (Build.VERSION.SDK_INT >= 23 && (Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && parentActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            parentActivity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
            return;
        }
        String fileName = FileLoader.getDocumentFileName(messageObject.getDocument());
        if (TextUtils.isEmpty(fileName)) {
            fileName = messageObject.getFileName();
        }
        String path = messageObject.messageOwner.attachPath;
        if (path != null && !path.isEmpty()) {
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (path == null || path.isEmpty()) {
            path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner).toString();
        }
        MediaController.saveFile(path, parentActivity, 3, fileName, messageObject.getDocument() != null ? messageObject.getDocument().mime_type : "");
    }

    public static void share(MessageObject messageObject, Activity parentActivity) {
        try {
            File f = null;
            boolean isVideo = false;

            if (!TextUtils.isEmpty(messageObject.messageOwner.attachPath)) {
                f = new File(messageObject.messageOwner.attachPath);
                if (!f.exists()) {
                    f = null;
                }
            }
            if (f == null) {
                f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner);
            }

            if (f.exists()) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(messageObject.getMimeType());
                if (Build.VERSION.SDK_INT >= 24) {
                    try {
                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ApplicationLoader.applicationContext, ApplicationLoader.getApplicationId() + ".provider", f));
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignore) {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                    }
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                }

                parentActivity.startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
                builder.setTitle(LocaleController.getString(R.string.AppName));
                builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                builder.setMessage(LocaleController.getString(R.string.PleaseDownload));
                builder.show();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void forward(MessageObject messageObject, long dialogId) {
        final ArrayList<MessageObject> fmessages;
        final TLRPC.TL_document document;
        if (messageObject.getId() < 0) {
            fmessages = null;
            if (!(messageObject.getDocument() instanceof TLRPC.TL_document)) {
                return;
            }
            document = (TLRPC.TL_document) messageObject.getDocument();
        } else {
            fmessages = new ArrayList<>();
            fmessages.add(messageObject);
            document = null;
        }
        if (fmessages != null) {
            SendMessagesHelper.getInstance(UserConfig.selectedAccount).sendMessage(fmessages, dialogId, false, false, true, 0, 0);
        } else {
            SendMessagesHelper.getInstance(UserConfig.selectedAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(document, null, messageObject.messageOwner.attachPath, dialogId, null, null, null, null, null, null, true, 0, 0, 0, null, null, false, false));
        }
        final BaseFragment lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment != null) {
            BulletinFactory.of(lastFragment)
                    .createSimpleBulletin(
                            R.raw.forward,
                            dialogId == UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId() ?
                                    LocaleController.getString(R.string.FwdMessageToSavedMessages) :
                                    dialogId > 0 ?
                                            LocaleController.formatString(R.string.FwdMessageToUser, DialogObject.getShortName(dialogId)) :
                                            LocaleController.formatString(R.string.FwdMessageToGroup, DialogObject.getShortName(dialogId))
                    )
                    .show();
        }
    }

    public static void forward(MessageObject messageObject, int currentAccount, BaseFragment parentActivity) {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        // https://github.com/DrKLO/Telegram/blob/fff7c8d97f5ca5708a1252ed58a6bb526de6e08e/TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java#L11164
        args.putInt("dialogsType", 3);
        args.putBoolean("canSelectTopics", true);
        DialogsActivity fragment = new DialogsActivity(args);
        final ArrayList<MessageObject> fmessages;
        final TLRPC.TL_document document;
        if (messageObject.getId() < 0) {
            fmessages = null;
            if (!(messageObject.getDocument() instanceof TLRPC.TL_document)) {
                return;
            }
            document = (TLRPC.TL_document) messageObject.getDocument();
        } else {
            fmessages = new ArrayList<>();
            fmessages.add(messageObject);
            document = null;
        }
        fragment.setDelegate(new DialogsActivity.DialogsActivityDelegate() {
            @Override
            public boolean canSelectStories() {
                return false;
            }

            @Override
            public boolean didSelectDialogs(DialogsActivity fragment1, ArrayList list, CharSequence message, boolean param, boolean notify, int scheduleDate, TopicsFragment topicsFragment) {
                ArrayList<MessagesStorage.TopicKey> dids = (ArrayList<MessagesStorage.TopicKey>) list;
                if (dids.size() > 1 || dids.get(0).dialogId == UserConfig.getInstance(currentAccount).getClientUserId() || message != null || fmessages == null) {
                    for (int a = 0; a < dids.size(); a++) {
                        long did = dids.get(a).dialogId;
                        if (message != null) {
                            SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(message.toString(), did, null, null, null, true, null, null, null, true, 0, 0, null, false));
                        }
                        if (fmessages != null) {
                            SendMessagesHelper.getInstance(currentAccount).sendMessage(fmessages, did, false, false, true, 0, 0);
                        } else {
                            SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(document, null, messageObject.messageOwner.attachPath, did, null, null, null, null, null, null, notify, scheduleDate, 0, 0, null, null, false, false));
                        }
                    }
                    fragment1.finishFragment();
                    final BaseFragment lastFragment = LaunchActivity.getLastFragment();
                    if (lastFragment != null) {
                        BulletinFactory.of(lastFragment)
                                .createSimpleBulletin(
                                        R.raw.forward,
                                        dids.size() == 1 && dids.get(0).dialogId == UserConfig.getInstance(currentAccount).getClientUserId() ?
                                                LocaleController.getString(R.string.FwdMessageToSavedMessages) :
                                                dids.size() == 1 && dids.get(0).dialogId > 0 ?
                                                        LocaleController.formatString(R.string.FwdMessageToUser, DialogObject.getShortName(dids.get(0).dialogId)) :
                                                        dids.size() == 1 && dids.get(0).dialogId < 0 ?
                                                                LocaleController.formatString(R.string.FwdMessageToGroup, DialogObject.getShortName(dids.get(0).dialogId)) :
                                                                LocaleController.formatPluralStringComma("FwdMessageToManyChats", dids.size())
                                )
                                .show();
                    }
                } else {
                    MessagesStorage.TopicKey topicKey = dids.get(0);
                    long did = topicKey.dialogId;
                    Bundle args1 = new Bundle();
                    args1.putBoolean("scrollToTopOnResume", true);
                    if (DialogObject.isEncryptedDialog(did)) {
                        args1.putInt("enc_id", DialogObject.getEncryptedChatId(did));
                    } else if (DialogObject.isUserDialog(did)) {
                        args1.putLong("user_id", did);
                    } else {
                        args1.putLong("chat_id", -did);
                    }
                    ChatActivity chatActivity = new ChatActivity(args1);
                    if (topicKey.topicId != 0) {
                        ForumUtilities.applyTopic(chatActivity, topicKey);
                    }
                    if (parentActivity.presentFragment(chatActivity, true, false)) {
                        chatActivity.showFieldPanelForForward(true, fmessages);
                        if (topicKey.topicId != 0) {
                            fragment1.removeSelfFromStack();
                        }
                    } else {
                        fragment1.finishFragment();
                    }
                }
                return true;
            }

            @Override
            public boolean didSelectStories(DialogsActivity dialogsActivity) {
                return false;
            }
        });
        parentActivity.presentFragment(fragment);
    }

}

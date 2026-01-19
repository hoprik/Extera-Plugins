package ru.hoprik.player.utils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

public class MusicInfo {
    private boolean shouldUpdate;
    private String currentTitle = "";
    private String currentAuthor = "";
    private int currentDuration;
    private int audioProgress;
    private String timeString = "";
    private String audioProgressString = "";
    private MessageObject messageObject;

    private String lastTitle = "";
    private String lastAuthor = "";

    public MusicInfo() {
        update();
    }

    public void update() {
        MessageObject playingMessageObject = MediaController.getInstance().getPlayingMessageObject();
        if (playingMessageObject == null) {
            this.shouldUpdate = false;
            this.currentTitle = "";
            this.currentAuthor = "";
            this.currentDuration = 0;
            this.audioProgress = 0;
            this.timeString = "";
            this.audioProgressString = "";
            this.messageObject = null;
            return;
        }

        this.messageObject = playingMessageObject;

        int calculatedDuration = 0;

        if (!MediaController.getInstance().isPlayingMessage(playingMessageObject)) {
            TLRPC.Document document = playingMessageObject.getDocument();
            if (document != null && document.attributes != null) {
                for (int i = 0; i < document.attributes.size(); i++) {
                    TLRPC.DocumentAttribute attribute = document.attributes.get(i);
                    // Проверяем, является ли атрибут аудио
                    if (attribute.getClass().getSimpleName().equals("TL_documentAttributeAudio")) {
                        calculatedDuration = (int) attribute.duration;
                        break;
                    }
                }
            }
        } else {
            calculatedDuration = playingMessageObject.audioProgressSec;
        }

        int totalDuration = playingMessageObject.audioPlayerDuration;

        this.timeString = AndroidUtilities.formatLongDuration(calculatedDuration);
        this.audioProgressString = AndroidUtilities.formatLongDuration(totalDuration);

        this.currentDuration = calculatedDuration;
        this.audioProgress = totalDuration;

        String newTitle = playingMessageObject.getMusicTitle();
        String newAuthor = playingMessageObject.getMusicAuthor();

        this.shouldUpdate = false;

        if (!newTitle.equals(this.lastTitle) || !newAuthor.equals(this.lastAuthor)) {
            this.shouldUpdate = true;
            // Обновляем "память"
            this.lastTitle = newTitle;
            this.lastAuthor = newAuthor;
        }

        this.currentTitle = newTitle;
        this.currentAuthor = newAuthor;
    }

    public boolean isShouldUpdate() {
        return shouldUpdate;
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getCurrentAuthor() {
        return currentAuthor;
    }

    public int getCurrentDuration() {
        return currentDuration;
    }

    public int getAudioProgress() {
        return audioProgress;
    }

    public String getTimeString() {
        return timeString;
    }

    public String getAudioProgressString() {
        return audioProgressString;
    }

    public MessageObject getMessageObject() {
        return messageObject;
    }
}
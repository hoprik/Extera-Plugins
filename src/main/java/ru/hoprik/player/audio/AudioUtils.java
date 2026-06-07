package ru.hoprik.player.audio;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import okhttp3.OkHttpClient;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.*;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import ru.hoprik.player.api.ApiClient;
import ru.hoprik.player.api.helpers.ICallback;
import ru.hoprik.player.api.objects.FindMusicInfo;
import ru.hoprik.player.api.providers.musicinfo.StatsFM;
import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Cover;
import ru.hoprik.player.audio.objects.Track;
import ru.hoprik.player.helpers.ImageHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AudioUtils {
    public static Track getTrackByMessageObject(MessageObject messageObject){
        if (messageObject.isMusic()){
            String trackName = messageObject.getMusicTitle();
            String trackAuthor = messageObject.getMusicAuthor();
            double duration = messageObject.getDuration();
            String artistArtWork = messageObject.getArtworkUrl(false);
            Cover cover = new Cover(ImageHelper.getArtworkThubImageLocation(messageObject), artistArtWork);
            findTrack();
            return new Track("-1", trackName, parseArtists(trackAuthor), (int) duration, 0, cover, null, false, false);
        }

        return null;
    }

    public static Track getTrackByFile(File file) {
        if (file.exists() && file.isFile() && file.canRead() && isAudioFile(file)) {
            MediaMetadataRetriever mediaMetadataRetriever = null;
            String title = "";
            String author = "";
            int duration = 0;
            Cover cover = null;

            try {
                mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(file.getAbsolutePath());

                // MIME чек (ещё одна защита через retriever)
                String mime = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                if (mime == null || !mime.startsWith("audio/")) {
                    return null; // Не музыка — выходим
                }

                title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                author = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

                String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    duration = (int) (Long.parseLong(durationStr) / 1000);
                }

                byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length);
                    if (bitmap != null) {
                        cover = new Cover(bitmap);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mediaMetadataRetriever.release();
                } catch (IOException ignored) {
                }
            }

            if (title == null || title.isEmpty()) title = file.getName();
            if (author == null) author = "";

            if (cover == null) {
                cover = new Cover(file);
            }

            return new Track(
                    "-1",
                    title,
                    parseArtists(author),
                    duration,
                    0,
                    cover,
                    null,
                    false,
                    false
            );
        }
        return null;
    }
    public static void findTrack(){
        OkHttpClient client = ApiClient.getInstance().getProxiedClient();
        new StatsFM(client).findMusicInfo("hoprik", new ICallback<>() {
            @Override
            public void onSuccess(FindMusicInfo item) {

                Log.e("MusicPlayer", "onSuccess: " + item.getArtists().get(0).getId());
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MusicPlayer", "onError: ", throwable);
            }
        });
    }

    public static MessageObject getMessageObjectByFile(File localFile) {
        Track track = getTrackByFile(localFile);
        if (track == null) return null;

        TLRPC.Message msg = new TLRPC.TL_message();
        msg.id = SharedConfig.getLastLocalId();
        long selfUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        msg.dialog_id = selfUserId;
        msg.out = true;

        // Создаём peer и to_id
        TLRPC.Peer peer = new TLRPC.TL_peerUser();
        peer.user_id = selfUserId;
        msg.peer_id = peer;
        msg.from_id = peer;

        msg.media = new TLRPC.TL_messageMediaDocument();
        TLRPC.TL_document document = new TLRPC.TL_document();
        document.id = 0;
        document.access_hash = 0;
        document.size = (int) localFile.length();
        document.mime_type = "audio/mpeg";

        TLRPC.TL_documentAttributeAudio audioAttr = new TLRPC.TL_documentAttributeAudio();
        audioAttr.title = track.getName();
        audioAttr.performer = track.getArtists().stream()
                .map(Artist::getName)
                .collect(Collectors.joining(", "));
        audioAttr.duration = track.getDuration();
        document.attributes.add(audioAttr);

        ((TLRPC.TL_messageMediaDocument) msg.media).document = document;
        msg.attachPath = localFile.getAbsolutePath();
        return new MessageObject(UserConfig.selectedAccount, msg, false, false);
    }

    public static MessageObject getMessageObjectByUrl(String url, Track track) {
        if (track == null) return null;
        TLRPC.Message msg = new TLRPC.TL_message();
        msg.id = SharedConfig.getLastLocalId();
        long selfUserId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        msg.dialog_id = selfUserId;
        msg.out = true;

        // Создаём peer и to_id
        TLRPC.Peer peer = new TLRPC.TL_peerUser();
        peer.user_id = selfUserId;
        msg.peer_id = peer;
        msg.from_id = peer;

        msg.media = new TLRPC.TL_messageMediaDocument();
        TLRPC.TL_document document = new TLRPC.TL_document();
        document.id = 0;
        document.access_hash = 0;
        document.size = 0;
        document.mime_type = "audio/mpeg";

        TLRPC.TL_documentAttributeAudio audioAttr = new TLRPC.TL_documentAttributeAudio();
        audioAttr.title = track.getName();
        audioAttr.performer = track.getArtists().stream()
                .map(Artist::getName)
                .collect(Collectors.joining(", "));
        audioAttr.duration = track.getDuration();
        document.attributes.add(audioAttr);

        ((TLRPC.TL_messageMediaDocument) msg.media).document = document;
        msg.attachPath = url;
        return new MessageObject(UserConfig.selectedAccount, msg, false, false);
    }

    public static void getMessageObjectByChatAndId(long chatId, int messageId, Consumer<MessageObject> callback){
        int currentAccount = UserConfig.selectedAccount;

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                // 1. Query the SQLite database directly
                SQLiteCursor cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized(
                        "SELECT data FROM messages WHERE uid = " + chatId + " AND mid = " + messageId
                );

                if (cursor.next()) {
                    // 2. Extract the raw byte buffer
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        // 3. Deserialize into a raw MTProto Message
                        TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                        message.readAttachPath(data, currentAccount);
                        data.reuse();
                        MessageObject messageObject = new MessageObject(currentAccount, message, false, true);
                        callback.accept(messageObject);
                    }
                }
                cursor.dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static List<Artist> parseArtists(String input) {
        List<Artist> result = new ArrayList<>();
        List<String> parsedArtists = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]+)\"|([^,]+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                parsedArtists.add(m.group(1).trim());
            } else if (m.group(2) != null) {
                parsedArtists.add(m.group(2).trim());
            }
        }
        for (String artist : parsedArtists) {
            result.add(new Artist("-1", artist, null, new ArrayList<>(), new ArrayList<>(), false, false));
        }
        return result;
    }

    private static boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") ||
                name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".aac") ||
                name.endsWith(".opus") || name.endsWith(".wma");
    }
}

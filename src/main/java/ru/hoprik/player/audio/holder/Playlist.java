package ru.hoprik.player.audio.holder;

import com.google.gson.Gson;

import org.telegram.messenger.MediaController;
import ru.hoprik.player.audio.AudioUtils;
import ru.hoprik.player.audio.objects.*;
import org.telegram.messenger.MessageObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Playlist {

    // ---------- DTO для сериализации ----------
    private static class PlaylistStorage {
        String playlistName;
        List<AudioElementData> elements;
    }

    private static class AudioElementData {
        AudioSourceData source;
        TrackData track;
    }

    private static class AudioSourceData {
        String type; // MESSAGE, URL, FILE, ID
        String url;
        String filePath;
        Long chatId;
        Integer messageId;
        TrackID trackId;
    }

    // DTO для Track (без циклов)
    private static class TrackData {
        String id;
        String name;
        List<String> artistNames;   // только имена артистов
        int duration;
        float progress;
        String coverUrl;            // URL или путь к файлу обложки
        String releaseTitle;        // только название релиза
        boolean isWeb;
        boolean local;

        TrackData(Track t) {
            this.id = t.getId();
            this.name = t.getName();
            this.artistNames = t.getArtists() == null ? new ArrayList<>()
                    : t.getArtists().stream().map(Artist::getName).collect(Collectors.toList());
            this.duration = t.getDuration();
            this.progress = t.getProgress();
            Cover c = t.getCover();
            if (c != null) {
                if (c.getUrl() != null) this.coverUrl = c.getUrl();
                else if (c.getFile() != null) this.coverUrl = c.getFile().getAbsolutePath();
            }
            Release r = t.getRelease();
            this.releaseTitle = r != null ? r.getTitle() : null;
            this.isWeb = t.isWeb();
            this.local = t.isLocal();
        }

        Track toTrack() {
            List<Artist> artists = new ArrayList<>();
            for (String name : artistNames) {
                artists.add(new Artist(null, name, null, null, null, isWeb, local));
            }
            Cover cover = null;
            if (coverUrl != null) {
                if (coverUrl.startsWith("http")) cover = new Cover(coverUrl);
                else cover = new Cover(new File(coverUrl));
            }
            Release release = releaseTitle != null ? new Release(null, releaseTitle, null, 0, null, null) : null;
            return new Track(id, name, artists, duration, (int) progress, cover, release, isWeb, local);
        }
    }
    
    private String playlistName;
    private List<AudioElement> elements;

    // ---------- Конструкторы и методы ----------
    public Playlist(String playlistName) {
        this.playlistName = playlistName;
        this.elements = new ArrayList<>();
    }

    public Playlist(String playlistName, List<AudioElement> elements) {
        this.playlistName = playlistName;
        this.elements = elements;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public List<AudioElement> getElements() {
        return elements;
    }

    public List<MessageObject> getMessageObjects() {
        return elements.stream().map(AudioElement::getAudio).collect(Collectors.toCollection(ArrayList::new));
    }

    public void setElements(List<AudioElement> elements) {
        this.elements = elements;
    }

    public void addElement(AudioElement element) {
        elements.add(element);
    }

    public void play() {
        ArrayList<MessageObject> messages = (ArrayList<MessageObject>) getMessageObjects();
        MediaController.getInstance().setPlaylist(messages, messages.get(0), -1);
    }

    // ---------- Сериализация ----------
    public static Playlist convertToPlaylist(List<MessageObject> objects) {
        Playlist playlist = new Playlist("");
        objects.forEach(messageObject -> {
            if (messageObject.isMusic()) {
                playlist.addElement(new AudioElement(AudioSource.ofMessage(messageObject), AudioUtils.getTrackByMessageObject(messageObject)));
            }
        });
        return playlist;
    }
    public static String toJson(Playlist playlist) {
        PlaylistStorage storage = new PlaylistStorage();
        storage.playlistName = playlist.playlistName;
        storage.elements = new ArrayList<>();

        for (AudioElement elem : playlist.elements) {
            AudioElementData data = new AudioElementData();
            data.source = convertSourceToData(elem.getAudioSource());
            data.track = new TrackData(elem.getTrack());
            storage.elements.add(data);
        }
        return new Gson().toJson(storage);
    }

    private static AudioSourceData convertSourceToData(AudioSource source) {
        AudioSourceData data = new AudioSourceData();
        data.type = source.getType().name();
        switch (source.getType()) {
            case MESSAGE:
                MessageObject msg = source.getMessage();
                data.chatId = msg.getDialogId();
                data.messageId = msg.getId();
                break;
            case URL:
                data.url = source.getUrl();
                break;
            case FILE:
                data.filePath = source.getFile().getAbsolutePath();
                break;
            case ID:
                data.trackId = source.getTrackID();
                break;
        }
        return data;
    }

    // ---------- Десериализация ----------
    public static void fromJson(String json, Consumer<Playlist> callback) {
        Gson gson = new Gson();
        PlaylistStorage storage = (PlaylistStorage) gson.fromJson(json, PlaylistStorage.class);
        if (storage == null) {
            callback.accept(null);
            return;
        }

        List<AudioElement> elements = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(storage.elements.size());

        for (AudioElementData data : storage.elements) {
            convertDataToSource(data.source, sourceReady -> {
                if (sourceReady == null) {
                    // Пропускаем элемент при ошибке
                    if (pending.decrementAndGet() == 0) {
                        callback.accept(new Playlist(storage.playlistName, elements));
                    }
                    return;
                }
                Track track = data.track.toTrack();
                AudioElement element = new AudioElement(sourceReady, track);
                synchronized (elements) {
                    elements.add(element);
                }
                if (pending.decrementAndGet() == 0) {
                    callback.accept(new Playlist(storage.playlistName, elements));
                }
            });
        }
    }
    private static void convertDataToSource(AudioSourceData data, Consumer<AudioSource> callback) {
        if (data == null || data.type == null) {
            callback.accept(null);
            return;
        }
        switch (data.type) {
            case "MESSAGE":
                if (data.chatId == null || data.messageId == null) {
                    callback.accept(null);
                    return;
                }
                AudioUtils.getMessageObjectByChatAndId(data.chatId, data.messageId, msg -> {
                    if (msg != null) {
                        callback.accept(AudioSource.ofMessage(msg));
                    } else {
                        callback.accept(null);
                    }
                });
                break;
            case "URL":
                callback.accept(AudioSource.ofUrl(data.url));
                break;
            case "FILE":
                File file = new File(data.filePath);
                callback.accept(file.exists() ? AudioSource.ofFile(file) : null);
                break;
            case "ID":
                callback.accept(AudioSource.ofID(data.trackId));
                break;
            default:
                callback.accept(null);
        }
    }
}
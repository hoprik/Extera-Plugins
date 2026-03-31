package ru.hoprik.pillkstati.controller;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class StreamResponse {
    private Item item;

    public Item getItem() { return item; }

    public static class Item {
        private String date;
        private boolean isPlaying;
        private int progressMs;
        private String deviceName;
        private Track track;
        private String platform;

        // геттеры
        public String getDate() { return date; }
        public boolean isPlaying() { return isPlaying; }
        public int getProgressMs() { return progressMs; }
        public String getDeviceName() { return deviceName; }
        public Track getTrack() { return track; }
        public String getPlatform() { return platform; }
    }

    public static class Track {
        private long id;
        private String name;
        private int durationMs;
        private boolean explicit;
        private List<Album> albums;
        private List<Artist> artists;
        private Map<String, List<String>> externalIds;
        @SerializedName("spotifyPopularity")
        private int popularity;
        private String spotifyPreview;
        private String appleMusicPreview;

        // геттеры
        public long getId() { return id; }
        public String getName() { return name; }
        public int getDurationMs() { return durationMs; }
        public boolean isExplicit() { return explicit; }
        public List<Album> getAlbums() { return albums; }
        public List<Artist> getArtists() { return artists; }
        public Map<String, List<String>> getExternalIds() { return externalIds; }
        public int getPopularity() { return popularity; }
        public String getSpotifyPreview() { return spotifyPreview; }
        public String getAppleMusicPreview() { return appleMusicPreview; }
    }

    public static class Album {
        private long id;
        private String image;
        private String name;
        // геттеры
        public long getId() { return id; }
        public String getImage() { return image; }
        public String getName() { return name; }
    }

    public static class Artist {
        private long id;
        private String name;
        private String image;
        // геттеры
        public long getId() { return id; }
        public String getName() { return name; }
        public String getImage() { return image; }
    }
}
package com.hillywave.audioplayer;

class Audio {

    private String data;
    private String title;
    private String album;
    private String artist;
    private String display_name;
    private String duration;
    private String year;
    private long lastChange;


    Audio(String data, String title, String album, String artist, String display_name, String duration, String year, long lastChange) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.display_name = display_name;
        this.duration = duration;
        this.year = year;
        this.lastChange = lastChange;


    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getData() {
        return data;
    }

    public String getTitle() {
        return title;
    }

}


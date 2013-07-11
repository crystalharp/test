package com.tigerknows.util;

public class SuggestWord {

    private String word;
    private float lon;
    private float lat;
    
    public SuggestWord(String word, float lon, float lat) {
        this.word = word;
        this.lon = lon;
        this.lat = lat;
    }

    public String getWord() {
        return word;
    }

    public float getLon() {
        return lon;
    }

    public float getLat() {
        return lat;
    }

    @Override
    public String toString() {
        return "SuggestWord [lat=" + lat + ", lon=" + lon + ", word=" + word + "]";
    }
}

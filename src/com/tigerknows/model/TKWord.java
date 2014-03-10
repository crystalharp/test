/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.model;

import com.tigerknows.R;
import com.tigerknows.android.location.Position;

import android.content.Context;

/**
 * @author Peng Wenyue
 */
public class TKWord {
    public static final int ATTRIBUTE_CLEANUP = 0;
    
    public static final int ATTRIBUTE_HISTORY = 1;
    
    public static final int ATTRIBUTE_SUGGEST = 2;
    
    public int attribute=ATTRIBUTE_HISTORY;
    public String word;
    public Position position;
    public String address;
    
    public TKWord() {
        
    }
    
    public TKWord(int attribute, String word) {
        this(attribute, word, null);
    }
    
    public TKWord(int attribute, String word, Position position) {
        this(attribute, word, position, null);
    }
    
    public TKWord(int attribute, String word, Position position, String address) {
        this.attribute = attribute;
        this.word = word;
        this.position = position;
        this.address = address;
    }
    
    private volatile int hashCode = 0;
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int hash = 17;
            hash += 37 * (attribute != ATTRIBUTE_CLEANUP ?  ATTRIBUTE_HISTORY : ATTRIBUTE_CLEANUP);
            if (word != null) {
                hash += word.hashCode();
            }
            if (address != null) {
                hash += address.hashCode();
            }
//            if (position != null) {
//                hash += position.hashCode();
//            }
            hashCode = hash;
        }
        
        return hashCode;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof TKWord) {
            TKWord other = (TKWord) object;
            if (attribute == other.attribute || (attribute != ATTRIBUTE_CLEANUP && other.attribute != ATTRIBUTE_CLEANUP)) {
                if ((word != null && word.equals(other.word)) || (word == null && word == other.word)) {
                    if ((address != null && address.equals(other.address)) || (address == null && address == other.address)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public TKWord clone() {
        TKWord tkWord = new TKWord(attribute, word);
        if (this.position != null) {
            tkWord.position = this.position.clone();
        }
        tkWord.address = this.address;
        return tkWord;
    }
    
    public POI toPOI() {
        POI poi = new POI();
        poi.setName(word);
        poi.setPosition(position);
        poi.setAddress(address);
        return poi;
    }
    
    public static TKWord getCleanupTKWord(Context context) {
        TKWord tkWord = new TKWord(TKWord.ATTRIBUTE_CLEANUP, context.getString(R.string.clean_history));
        return tkWord;
    }
    
    public String toString(){
        return word;
    }
}

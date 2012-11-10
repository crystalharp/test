/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.model;

/**
 * @author Peng Wenyue
 */
public class TKWord {
    public static final int TYPE_CLEANUP = 0;
    
    public static final int TYPE_HISTORY = 1;
    
    public static final int TYPE_SUGGEST = 2;
    
    public int type=0;
    public String word;
    
    public TKWord(int type, String word) {
        this.type = type;
        this.word = word;
    }
}

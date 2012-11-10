/*
 * @(#)VersionData.java  下午04:24:39 2010-4-16 2010
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */
package com.tigerknows.model.response;

import java.io.IOException;

import com.tigerknows.util.ParserUtil;

/**
 * 表征位置信息.
 *
 * @author Administrator
 */
public class PositionCake extends Appendix {
    
    /*
                  类型码              type lat lon accuracy  
       0x00 0x18 uint2 uint4 uint4 uint4  
     */
    
    
    private int positionType;
    private int lat;
    private int lon;
    private int accuracy;

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        positionType = util.readIntFromTwoBytes();
        lat = util.readIntFromFourBytes();
        lon = util.readIntFromFourBytes();
        if (positionType == 1) {
            lat = decodeLat(lat);
            lon = decodeLon(lon);
        }
        accuracy = util.readIntFromFourBytes();
    }
    
    public int getPositionType() {
        return positionType;
    }
    
    public int getLat() {
        return lat;
    }
    
    public int getLon() {
        return lon;
    }
    
    public int getAccuracy() {
        return accuracy;
    }
    
    /**
     * 坐标解密
     * @param oldVal
     * @return
     */
    protected static int decodeLat(int oldVal){
        return left(oldVal)^0x196290E8;
    }
    
    protected static int decodeLon(int oldVal){
        return oldVal^0x2BC2EF60;
    }
    
    /**
     * 循环左移5位
     * @param a
     * @return
     */
    protected static int left(int a){
        return (a << 5) | ((a >> 27) & 0x1f);
    }
}

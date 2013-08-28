/*
 * Copyright (C) 2007 2010 Beijing TigerKnows Science and Technology Ltd. All rights reserved.
 */
package com.tigerknows.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.decarta.android.location.Position;
import com.tigerknows.TKConfig;
import com.tigerknows.map.LocalRegionDataInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapWord;
import com.tigerknows.map.TileDownload;
import com.tigerknows.map.MapEngine.RegionMetaVersion;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.xobject.ByteReader;
import com.tigerknows.model.xobject.ByteWriter;
import com.tigerknows.model.xobject.XObject;

/**
 * @author etarip 
 * Modified by Li Hong
 */
public final class ByteUtil {

    /**
     * 将byte转换为int，转换后所得int取值范围为[0, 255]，应把byte视作无符号数（Java不支持无符号数，因此byte可能取负值）
     * 
     * @param b
     *            要转换的byte
     * @return 转换后的int
     */
    public static int byteToInt(byte b) {
        if (b < 0)
            return (int) b + 256;
        return b;
    }

    /**
     * 将两个byte转换为int，int为有符号整数，其由低到高第16个bit为符号位，取值范围为[Short.MIN_VALUE, Short.MAX_VALUE]
     * 
     * @param b1
     *            高8 bit组成的字节
     * @param b2
     *            低8 bit组成的字节
     * @return 转换后的int
     */
    public static int byteToInt(byte b1, byte b2) {
        return (b1 << 8) | (b2 & 0xff);
    }

    /**
     * 将4个byte转换为int，int为有符号整数
     * 
     * @param b1
     *            最高8 bit组成的字节
     * @param b2
     *            次高8 bit组成的字节
     * @param b3
     *            次低8 bit组成的字节
     * @param b4
     *            最低8 bit组成的字节
     * @return 转换后的int
     */
    public static int byteToInt(byte b1, byte b2, byte b3, byte b4) {
        return (b1 << 24) | ((b2 << 16) & 0xff0000) | ((b3 << 8) & 0xff00) | (b4 & 0xff);
    }

    public static int arr2int(byte[] arr, int start) {
        return byteToInt(arr[start + 3], arr[start + 2], arr[start + 1], arr[start]); 
    }
    
    public static float arr2float(byte[] arr, int start) {
        // 4 bytes
        int len = 4;
        byte[] tmp = new byte[len];
        for (int i = start, cnt = 0; i < (start + len); i++, cnt++) {
            tmp[cnt] = arr[i];
        }
        
        int l;  
        l = tmp[0];  
        l &= 0xff;  
        l |= ((long) tmp[1] << 8);  
        l &= 0xffff;  
        l |= ((long) tmp[2] << 16);  
        l &= 0xffffff;  
        l |= ((long) tmp[3] << 24);  
        return Float.intBitsToFloat(l);
    }

    public static double arr2double(byte[] arr, int start) {
        int i = 0;
        int len = 8;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for (i = start; i < (start + len); i++) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
            accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
            i++;
        }
        return Double.longBitsToDouble(accum);
    }
   
    public static int byteToInt(byte[] b, int from, int len){
        int r =0;
        for(int i=from; i< from+len; i++){
            r<<=8;
            r|=((int)b[i])&0xff;
        }
        return r;
    }
    
    public static List<String> byte2SuggestWords(byte[] data, int wordsNum) {
        List<String> words = new ArrayList<String>();
        for (int i=0; (i<wordsNum*36) && (data[i]!=0); i += 36) {
            try {
                int wordLen = 0;
                while ((wordLen<32) && (data[i+wordLen] != 0)) {
                    wordLen++;
                }
                if (wordLen>0) {
                    String word = new String(data, i, wordLen, "GBK");
                    words.add(word);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return words;
    }
    
    public static ArrayList<TKWord> byte2LonLatSuggestWords(byte[] data, int wordsNum) {
        ArrayList<TKWord> words = new ArrayList<TKWord>();
        for (int i=0; (i<wordsNum*36) && (data[i]!=0); i += 36) {
            try {
                int wordLen = 0;
                while ((wordLen<32) && (data[i+wordLen] != 0)) {
                    wordLen++;
                }
                if (wordLen>0) {
                    String word = new String(data, i, wordLen, "GBK");
                    float lon = arr2float(data, i + 28);
                    float lat = arr2float(data, i + 32);
                    words.add(new TKWord(TKWord.ATTRIBUTE_SUGGEST, word, new Position(lat, lon)));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return words;
    }

    /**
     * 
     * @param byteTileInfo:lost data num, real return lost data num, lost data
     * @return
     */
    public static List<TileDownload> parseTileInfo(byte[] byteTileInfo) {
        int returnLostDataNum = arr2int(byteTileInfo, 4);
        return parseLostData(byteTileInfo, 8, returnLostDataNum);
    }
    
    public static List<TileDownload> parseLostData(final byte[] byteTileInfo, final int start, final int num) {
        List<TileDownload> list = new ArrayList<TileDownload>();
        if (num < 1) {
            return list;
        }
        MapEngine mapEngine = MapEngine.getInstance();
        for (int i=0; i<num; i++) {
            int rid = arr2int(byteTileInfo, start + 12*i);
            
            int offset = arr2int(byteTileInfo, start + 12*i + 4);
            
            int len = arr2int(byteTileInfo, start + 12*i + 8);
            
            RegionMetaVersion regionMetaVersion = mapEngine.getRegionMetaVersion(rid);
            if (regionMetaVersion != null) {
                String version = regionMetaVersion.toString();
                TileDownload tileInfo = new TileDownload(rid, offset, len, version);
                
                list.add(tileInfo);
            }
        }
        return list;
    }
    
    /**
     * 
     * @param byteTileInfo:lost data num, real return lost data num, total size, downloaded size, lost data
     * @return
     */
    public static LocalRegionDataInfo parseRegionMapInfo(final byte[] byteRegionMapInfo) {
        LocalRegionDataInfo regionMapInfo = new LocalRegionDataInfo();
        if (byteRegionMapInfo == null) {
            return regionMapInfo;
        }
        
        int lostDataNum = arr2int(byteRegionMapInfo, 0);
        regionMapInfo.setLostDataNum(lostDataNum);
        
        int returnLostDataNum = arr2int(byteRegionMapInfo, 4);
        regionMapInfo.setLostDatas(parseLostData(byteRegionMapInfo, 16, returnLostDataNum));
        
        int totalSize = arr2int(byteRegionMapInfo, 8);
        regionMapInfo.setTotalSize(totalSize);
        
        int downloadedSize = arr2int(byteRegionMapInfo, 12);
        regionMapInfo.setDownloadSize(downloadedSize);
        
        return regionMapInfo;
    }
    
     /**    
     * 全角转半角    
     * @param input String.    
     * @return 半角字符串    
     */     
     public static String toDBC(String input) {
         char c[] = input.toCharArray();
         for (int i = 0; i < c.length; i++) {
             if (c[i] == '\u3000') {
                 c[i] = ' ';
             } else if (c[i] > '\uFF00' && c[i] < '\uFF5F') {
                 c[i] = (char) (c[i] - 65248);
             }
         }
         String returnString = new String(c);
         return returnString;
     }

    public static MapWord[] parseMapText(byte[] mapTextBytes) {
        if (mapTextBytes == null || mapTextBytes.length < 1) {
            return null;
        }
        int mapTextNum = arr2int(mapTextBytes, 0);
        if (mapTextNum <= 0) {
            return null;
        }
        
        MapWord[] mapWords = new MapWord[mapTextNum];
        int start = 4;
        for(int i=0; i<mapTextNum; i++) {
            int nameLen = 0;
            while(mapTextBytes[start+nameLen] != 0) {
                nameLen ++;
            }
            nameLen ++;
            
            try {
                
//                String name = new String(mapTextBytes, start, start+nameLen, "GBK");
                byte[] tempNameBytes = new byte[nameLen-1];
                System.arraycopy(mapTextBytes, start, tempNameBytes, 0, nameLen-1);
                String name = new String(tempNameBytes, "GBK");
                
                start += nameLen;
                int fontColor = arr2int(mapTextBytes, start);
                int fontSize = arr2int(mapTextBytes, start + 4);
                float slope = arr2float(mapTextBytes, start + 8);
                int outlineColor = arr2int(mapTextBytes, start + 12);
                int x = arr2int(mapTextBytes, start + 16);
                int y = arr2int(mapTextBytes, start + 20);
                int iconIndex = arr2int(mapTextBytes, start + 24);
                int iconX = arr2int(mapTextBytes, start + 28);
                int iconY = arr2int(mapTextBytes, start + 32);
                MapWord.Icon icon = new MapWord.Icon(iconIndex, iconX, iconY);
                MapWord mapWord = new MapWord(toDBC(name), fontColor, fontSize, slope, outlineColor, x, y, icon);
                mapWords[i] = mapWord;
                start += 36;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return mapWords;
    }
    
    public static XObject byteToXObject(byte[] data) throws IOException {
        if (data == null) {
            return null;
        }
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        ByteReader reader = new ByteReader(is, TKConfig.getEncoding());
        return XObject.readFrom(reader);
    }
    
    public static byte[] xobjectToByte(XObject xobject) throws IOException {
        if (xobject == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteWriter writer = new ByteWriter(os, TKConfig.getEncoding());
        xobject.writeTo(writer);
        return os.toByteArray();
    }

    public static int getCharArrayLength(String str){
    	char[] strChar = str.toCharArray();
    	int doubleChar = 0;
        for(int i=0;i<strChar.length;i++){
        	if((char)(byte)strChar[i] != strChar[i])doubleChar ++;
        }
        return str.length() + doubleChar;      	
    }
}

/*
 * @(#)ParserUtil.java  下午04:08:12 2008-5-17 2008
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */
package com.tigerknows.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 本类提供数据包的解析工具，它包含待解析的数据，同时设定一个字段保下一次解析时的起始位置.
 *
 * @author pengjunjie
 * @version
 * @see
 *
 * Change log:
 */
public class ParserUtil {
    
    /**
     * 足够大的字符缓冲.
     */
    private static char[] charBuffer = new char[20 * 1024];
    
    private int start = 0;
    private byte[] data;
    private String encoding;
    
    /**
     * 获得一个工具类实例.
     * @param start
     *      解析的起点. 如果小于0则视为0.
     * @param data
     *      待解析数据
     * @return
     *      工具类实例.
     */
    
    public ParserUtil(byte[] data, String encoding){
        this(data, 0, encoding);
    }
    
    public ParserUtil(byte[] data, int start, String encoding){
        if(start < 0)
            start = 0;
        
        this.start = start;
        this.data = data;
        this.encoding = encoding;
    }
    
    /**
     * 向前移动n个字节.
     * @param n
     *      向前移动的字节数目，应当大于等于0，并且小于等于剩下的字节数.
     *      如果小于等于0，视为不移动.
     */
    public void advance(int n){
        if(n <= 0)
            return;
        
        if(n > availableDataleng())
            throw new IllegalArgumentException("n超过了剩余字节数！");
        
        start += n;            
    }
    
    /**
     * 返回剩下的可用的字节数.
     * @return
     *      剩下的可用的字节数.
     */
    public int availableDataleng(){
        return data.length - start;
    }
    
        
    /**
     * 获得下一次解析的起点.
     * @return
     */
    public int getStart(){
        return start;
    }
    
    /**
     * 返回数据.
     * @return
     */
    public byte[] getData(){
        return data;
    }
    
    /**
     * 读两个字节，解析出对应整数.
     * @return
     *  两个字节对应整数.
     */
    public int readIntFromTwoBytes(){
        int result = ByteUtil.byteToInt(data[start], data[start + 1]);
        start += 2;
        
        return result;
    }
    
    /**
     * 读一个字节，解析出对应整数.
     * @return
     *  一个字节对应整数.
     */
    public int readIntFromOneByte(){
        return ByteUtil.byteToInt(data[start++]);
    }
    
    /**
     * 读一个字节，解析出对应整数.
     * @return
     *  一个字节对应整数.
     */
    public int readIntFromFourBytes(){
        int result = ByteUtil.byteToInt(data[start], data[start + 1],
                data[start + 2], data[start + 3]);
        start += 4;
        
        return result;
    }    
    
    /**
     * 解析一个字符串.同时start属性自动增加相应的值.增加数目等于本次解析的长度.
     * @param encoding 字符串编码方式.
     * @return .
     * @throws IOException .
     */
    public String transferString()
        throws IOException{    
        int len = ByteUtil.byteToInt(data[start], 
                data[start + 1]);
        start += 2;
        
        String result = transferString(data, start, len, encoding);
        start += len;
        
        return result;
    }
    
    /**
     * 解析一个字符串.
     * @param response 待解析的二进制数组.
     * @param encoding 字符串编码方式.
     * @param start
     *  起点.
     *  @param len
     *  长度.
     * @return .
     * @throws IOException .
     */
    public static String transferString(byte[] response, 
            int start, int len, String encoding)
        throws IOException{    
        int l = readByteArray(response, start, len, charBuffer, encoding);        
        
        if(l >= 0)
            return new String(charBuffer, 0, l);
        else
            return null;   
    }
    
    /**
     * 把字节解析为字符数组.<b>本方法不增加start字段的值.</b>
     * @param response 待解析的二进制数组.
     * @param encoding 字符串编码方式. 
     * @param start
     *  起点.
     *  @param len
     *  长度.
     * @param charBuffer
     *      足够大的字符数组，用于接受解析后的字符串.<b>本方法不处理它溢出时的问题</b>.
     * @return 调到的字符数
     * @throws IOException .
     */
    public static int readByteArray(byte[] response, 
            int start, int len, char[] charBuffer, String encoding) throws IOException{
        ByteArrayInputStream bais = new ByteArrayInputStream(response, start, len);
        InputStreamReader is = new InputStreamReader(bais, encoding);
        int l = is.read(charBuffer);
        is.close();
        bais.close();
        
        return l;
    }
}

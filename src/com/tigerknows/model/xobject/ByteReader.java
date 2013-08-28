package com.tigerknows.model.xobject;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
/**
 * 
 * @author duliang
 *
 */
public class ByteReader {

    private static final String EMPTY_STRING = "";
    private String charset;
    private InputStream is;
    private int bytesReaded = 0;
    
    public ByteReader(InputStream is, String charset) {
        this.charset = charset;
        this.is = is;
    }
    
    public int getBytesReaded(){
        return bytesReaded;
    }

    public byte readByte() throws IOException {
        int b = is.read();
        if (b == -1)
            throw new EOFException("read 1 byte,0 left");
        bytesReaded ++;
        return (byte) b;
    }
    
    public byte[] readBytes(int n) throws IOException {
        byte[] bytes = new byte[n];
        int length = is.read(bytes);
        if (length != n)
            throw new EOFException("read " + n + " bytes," + length + " left");
        bytesReaded += n;
        return bytes;
    }

    public long readInt() throws IOException {
        long value;
        byte currentByte;
        
        currentByte = readByte();
        if((currentByte & 64) != 0)
            value = -1;
        else
            value = 0;
        value = (value << 7) | currentByte & 127;
        
        while ((currentByte & 128) != 0){
            currentByte = readByte();
            value = (value << 7) | currentByte & 127;
        }
        return value;
    }
    
    public double readDouble() throws IOException {
        return Double.longBitsToDouble( readLong() );
    }
    
    private long readLong() throws IOException {
        long l = 0;
        for(int i=0;i<8;i++)
            l = (l << 8) | (readByte() & 0xFF);
        return l;
    }
    
    public String readString() throws IOException {
        String str;
        int length = (int)readInt();
        if (length > 0) {
            str = new String(readBytes(length), charset);
        } else {
            str = EMPTY_STRING;
        }
        return str;
    }

}

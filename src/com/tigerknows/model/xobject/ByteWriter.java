package com.tigerknows.model.xobject;

//import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

//import javax.imageio.ImageIO;

/**
 * 
 * @author duliang
 * 
 */
public class ByteWriter {

    private OutputStream os;
    private String charset;
    private int bytesWrited;
    
    public ByteWriter(OutputStream os, String charset) {
        this.os = os;
        this.charset = charset;
        this.bytesWrited = 0;
    }
    
    public int getBytesWrited(){
        return bytesWrited;
    }

    public void writeByte(byte b) throws IOException {
        os.write(b);
        bytesWrited ++;
    }

    public void writeBytes(byte[] ba) throws IOException {
        os.write(ba);
        bytesWrited += ba.length;
    }

    public void writeInt(long i) throws IOException {
        
        boolean positive = (i >= 0);
        
        List<Byte> list = new ArrayList<Byte>();
        list.add((byte) (i & 0x7f));
        i = i >> 7;

        while (i != 0 && i != -1) {
            list.add((byte) (i & 0x7f | 0x80));
            i = i >> 7;
        }
        
        byte firstByte = list.get(list.size()-1);
        
        boolean asPositive = (firstByte & 0x40) == 0;
        if( positive ^ asPositive ){
            list.add( positive ? (byte)0x80 : (byte)0xff);
        }

        for (int j = list.size() - 1; j >= 0; j--) {
            os.write(list.get(j));
        }
        bytesWrited += list.size();
    }

    public void writeDouble(double d) throws IOException {
        writeLong( Double.doubleToLongBits(d) );
    }
    
    private void writeLong(long l)throws IOException {
        os.write((byte)((l >> 56) & 0xff));
        os.write((byte)((l >> 48) & 0xff));
        os.write((byte)((l >> 40) & 0xff));
        os.write((byte)((l >> 32) & 0xff));
        os.write((byte)((l >> 24) & 0xff));
        os.write((byte)((l >> 16) & 0xff));
        os.write((byte)((l >>  8) & 0xff));
        os.write((byte)(l & 0xff));
        bytesWrited += 8;
    }
    
    public void writeString(String s) throws IOException {
        if(s == null){
            writeInt(0);
        }else{
            byte[] bytes = s.getBytes(charset);
            writeInt(bytes.length);
            os.write(bytes);
            bytesWrited += bytes.length;
        }
    }

    public void writeStringBytes(String s) throws IOException {
        byte[] bytes = s.getBytes(charset);
        os.write(bytes);
        bytesWrited += bytes.length;
    }
    
    public void writeStream(ByteArrayOutputStream bos) throws IOException {
        bos.writeTo(os);
        bytesWrited += bos.size();
    }

//    public void writeImage(BufferedImage img, String formatName)
//            throws IOException {
//        ImageIO.write(img, formatName, os);
//        //TODO:如何计算写了多少字节？
//    }
    
    public void setCharset(String charset){
        this.charset = charset;
    }
    
    public String getCharset(){
        return charset;
    }

}

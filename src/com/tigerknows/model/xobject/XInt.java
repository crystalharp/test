package com.tigerknows.model.xobject;

import java.io.IOException;

/**
 * 
 * @author duliang
 *
 */
public class XInt extends XObject{

    private long value;
    
    protected XInt(long l) {
        super(XType.INT);
        value = l;
    }
    
    public void setValue(long l) {
        this.value = l;
    }
    
    public long getValue(){
        return value;
    }
    
    @Override
    protected void writeContent(ByteWriter writer) throws IOException {
        writer.writeInt(value);
    }
    
    protected static XInt readContent(ByteReader reader) throws IOException{
        return new XInt( reader.readInt() );
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XInt) {
            
            XInt other = (XInt) object;
            
            if(other.value == value) {
                return true;
            }
        }
        
        return false;
    }
    
    public static XInt valueOf(long l){
        return new XInt(l);
    }
}

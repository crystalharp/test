package com.tigerknows.model.xobject;

import java.io.IOException;

/**
 * 
 * @author duliang
 *
 */
public class XString extends XObject{
    
    private String value;
    
    protected XString(String s) {
        super(XType.STRING);
        value = s;
    }
    
    protected String getValue(){
        return value;
    }
    
    @Override
    protected void writeContent(ByteWriter writer) throws IOException {
        writer.writeString(value);
    }
    
    protected static XString readContent(ByteReader reader) throws IOException{
        return new XString( reader.readString() );
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XString) {
            
            XString other = (XString) object;
            
            if((null != other.value && !other.value.equals(value)) || (null == other.value && other.value != value)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
}

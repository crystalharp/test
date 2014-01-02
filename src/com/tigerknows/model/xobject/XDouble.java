package com.tigerknows.model.xobject;

import java.io.IOException;

/**
 * 
 * @author duliang
 *
 */
public class XDouble extends XObject {
    private double value;

    protected XDouble(double d) {
        super(XType.DOUBLE);
        value = d;
    }
    
    protected double getValue(){
        return value;
    }
    
    public void setValue(double value){
        this.value = value;
    }

    @Override
    protected void writeContent(ByteWriter writer) throws IOException {
        writer.writeDouble(value);
    }
    
    protected static XDouble readContent(ByteReader reader) throws IOException{
        return new XDouble( reader.readDouble() );
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XDouble) {
            
            XDouble other = (XDouble) object;
            
            if(other.value == value) {
                return true;
            }
        }
        
        return false;
    }
}
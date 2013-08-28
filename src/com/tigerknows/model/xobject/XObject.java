package com.tigerknows.model.xobject;

import java.io.IOException;

/**
 * 
 * @author duliang
 * 
 * 
 */
public abstract class XObject implements Writable{
    public static final byte INT = 0x10;
    public static final byte STRING = 0x11;
    public static final byte DATA = 0x12;
    public static final byte DOUBLE = 0x13;
    
    public static final byte ARRAY = 0x20;
    public static final byte MAP = 0x21;
    
    public void writeTo(ByteWriter writer) throws IOException {
        writer.writeByte(type.code);
        writeContent(writer);
    }
    
    public static XObject readFrom(ByteReader reader) throws IOException{
        byte code = reader.readByte();
        return readByCode(reader, code);
    }
    
    @SuppressWarnings("rawtypes")
    public XArray toXArray(){
        throw new UnsupportedOperationException("can not toXArray");
    }

    public XMap toXMap(){
        throw new UnsupportedOperationException("can not toXMap");
    }
        
    protected XType type;
    
    protected XObject(XType type){
        this.type = type;
    }
    
    protected abstract void writeContent(ByteWriter writer) throws IOException;
    
    public static XObject readByCode(ByteReader reader, byte code) throws IOException{
        switch(code){
        case INT:
            return XInt.readContent(reader);
        case STRING:
            return XString.readContent(reader);
        case DATA:
            return XBinaryData.readContent(reader);
        case DOUBLE:
            return XDouble.readContent(reader);
        case ARRAY:
            return XArray.readContent(reader);
        case MAP:
            return XMap.readContent(reader);
        }
        throw new IllegalArgumentException("unknown code:"+code);
    }
    
    @SuppressWarnings( "rawtypes" )
    public static XObject valueOf(Object o){
        if(o instanceof Integer ){
            return new XInt((Integer)o);
        }else if(o instanceof Long ){
            return new XInt((Long)o);
        }else if(o instanceof String){
            return new XString((String)o);
        }else if(o instanceof Float){
            return new XDouble((Float)o);
        }else if(o instanceof Double){
            return new XDouble((Double)o);
        }else if(o instanceof byte[]){
            return new XBinaryData((byte[])o);
        }else if(o instanceof XArray){
            return (XArray)o;
        }else if(o instanceof XMap){
            return (XMap)o;
        }else if(o instanceof XInt){
            return (XInt)o;
        }else if(o instanceof XString){
            return (XString)o;
        } else if(o instanceof XDouble){
            return (XDouble)o;
        }else if(o instanceof XBinaryData){
            return (XBinaryData)o;
        } else{
            throw new IllegalArgumentException(o + " can not convert to xobject");
        }
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XObject) {
            
            XObject other = (XObject) object;
            
            if(other.type == type) {
                if((null != other && !other.equals(this)) || (null == other && other != this)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        
        return false;
    }
}

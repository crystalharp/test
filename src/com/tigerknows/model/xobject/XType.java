package com.tigerknows.model.xobject;
/**
 * 
 * @author duliang
 *
 */
public class XType{
    public final byte code;
    private XType(byte code){
        this.code = code;
    }
    public String toString(){
        if(code == INT.code){
            return "int";
        }else if(code == STRING.code){
            return "string";
        }else if(code == DATA.code){
            return "binary data";
        }else if(code == DOUBLE.code){
            return "double";
        }else if(code == ARRAY.code){
            return "array";
        }else if(code == MAP.code){
            return "map";
        }else{
            return "unknown";
        }
    }
    
    public static XType INT = new XType((byte)XObject.INT);
    public static XType STRING = new XType((byte)XObject.STRING);
    public static XType DATA = new XType((byte)XObject.DATA);
    public static XType DOUBLE = new XType((byte)XObject.DOUBLE);

    public static XType ARRAY = new XType((byte)XObject.ARRAY);
    public static XType MAP = new XType((byte)XObject.MAP);
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XType) {
            
            XType other = (XType) object;
            
            if(other.code == code) {
                return true;
            }
        }
        
        return false;
    }
}

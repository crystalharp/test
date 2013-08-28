package com.tigerknows.model.xobject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author duliang
 *
 */
public class XMap extends XObject {
    
    private HashMap<Byte, XObject> objects = new HashMap<Byte, XObject>();

    public XMap() {
        super(XType.MAP);
    }

    public void put(byte key, long value) {
        objects.put(key, new XInt(value));
    }
    
    public void put(byte key, String value) {
        objects.put(key, new XString(value));
    }

    public void put(byte key, double value) {
        objects.put(key, new XDouble(value));
    }

    public void put(byte key, byte[] value) {
        objects.put(key, new XBinaryData(value));
    }

    public void put(byte key, XObject o) {
        objects.put(key, o);
    }

    public void remove(byte key) {
        if (containsKey(key)) {
            objects.remove(key);
        }
    }

    public boolean containsKey(byte key){
        return objects.containsKey(key);
    }
    
    public String getString(byte key){
        checkKey(key);
        return ((XString)objects.get(key)).getValue();
    }
    
    public long getInt(byte key){
        checkKey(key);
        return ((XInt)objects.get(key)).getValue();
    }
    
    public double getDouble(byte key){
        checkKey(key);
        return ((XDouble)objects.get(key)).getValue();
    }
    
    public byte[] getBytes(byte key){
        checkKey(key);
        return ((XBinaryData)objects.get(key)).getValue();
    }
    
    public XObject getXObject(byte key){
        checkKey(key);
        return objects.get(key);
    }
    
    @SuppressWarnings("rawtypes")
	public XArray getXArray(byte key){
        checkKey(key);
        return objects.get(key).toXArray();
    }
    
    public XMap getXMap(byte key){
        checkKey(key);
        return objects.get(key).toXMap();
    }
    
    private void checkKey(byte key){
        if(!containsKey(key)){
            throw new IllegalArgumentException("no such key");
        }
    }
    
    // TODO: protected changed to public
    @Override
    public void writeContent(ByteWriter writer) throws IOException {
        writer.writeInt(objects.size());
        List<Byte> keyList = new ArrayList<Byte>(objects.keySet()); 
        Collections.sort(keyList);
        for (Byte key : keyList) {
            writer.writeByte(key);
            objects.get(key).writeTo(writer);
        }
    }
    
    protected static XMap readContent(ByteReader reader) throws IOException{
        int itemSize = (int) reader.readInt();
        byte key;
        byte code;
        XObject o;
        
        XMap xMap = new XMap();
        for(int i=0;i<itemSize;i++){
            key = reader.readByte();
            code = reader.readByte();
            o = XObject.readByCode(reader, code);
            xMap.put(key, o);
        }
        return xMap;
    }
    
    @Override
    public XMap toXMap(){
        return this;
    }

    protected XMap getValue() {
        return this;
    }

    @Override
    public String toString() {
        if (objects == null) {
            return "null"; //$NON-NLS-1$
        }
        int length = objects.size();
        if (length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        List<Byte> keyList = new ArrayList<Byte>(objects.keySet()); 
        StringBuilder sb = new StringBuilder(2 + length * 4);
        sb.append('[');
        for (Byte key : keyList) {
            if (sb.length() > 1) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(key);
            sb.append('=');
            sb.append(objects.get(key));
        }
        sb.append(']');
        return sb.toString();
    }
    
    public List<Byte> getKeyList() {
        List<Byte> keyList = new ArrayList<Byte>(objects.keySet()); 
        return keyList;
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XMap) {
            
            XMap other = (XMap) object;
            
            if((null != other.objects && !other.objects.equals(objects)) || (null == other.objects && other.objects != objects)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
}

package com.tigerknows.model.xobject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author duliang
 * 
 */
public class XArray<T> extends XObject {
    private List<T> elements = new ArrayList<T>();

    public XArray() {
        super(XType.ARRAY);
    }

    @Override
    public XArray<T> toXArray() {
        return this;
    }
    
    public void add(T t){
        elements.add(t);
    }

    public T get(int i) {
        return elements.get(i);
    }

    public T remove(int i) {
        return elements.remove(i);
    }
    
    public int size() {
        return elements.size();
    }
    
    @Override
    public String toString() {
        if (elements == null) {
            return "null"; //$NON-NLS-1$
        }
        int length = elements.size();
        if (length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuilder sb = new StringBuilder(2 + length * 4);
        sb.append('[');
        sb.append(elements.get(0));
        for (int i = 1; i < length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(elements.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    protected void writeContent(ByteWriter writer) throws IOException {
        writer.writeInt(elements.size());
        for (int i=0,size=elements.size();i<size;i++) {

            XObject o = XObject.valueOf( elements.get(i) );
            if( i==0 ){
                writer.writeByte(o.type.code);
            }
            o.writeContent(writer);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static XArray readContent(ByteReader reader) throws IOException {
        int itemSize = (int) reader.readInt();
        XArray xArray = new XArray();
        if (itemSize > 0) {
            byte code = reader.readByte();
            for (int i = 0; i < itemSize; i++)
                xArray.add(XObject.readByCode(reader, code));
        }
        return xArray;
    }

    protected XArray<T> getValue() {
        return this;
    }

    public List<Long> toIntList() {
        int size = size();
        ArrayList<Long> list = new ArrayList<Long>(size);
        for (T xobject : elements) {
            XInt xint = (XInt)XObject.valueOf(xobject);
            list.add(xint.getValue());
        }
        return list;
    }
    
    public static XArray<XInt> fromIntList(List<Long> list) {
        XArray<XInt> xarray = new XArray<XInt>();
        for (long i : list) {
            xarray.add(new XInt(i));
        }
        return xarray;
    }

    public List<String> toStringList() {
        int size = size();
        ArrayList<String> list = new ArrayList<String>(size);
        for (T xobject : elements) {
            list.add(xobject+"");
        }
        return list;
    }
    
    public static XArray<XString> fromStringList(List<String> list) {
        XArray<XString> xarray = new XArray<XString>();
        for (String str : list) {
            xarray.add(new XString(str));
        }
        return xarray;
    }
    
    @SuppressWarnings({ "rawtypes" })
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XArray) {
            
            XArray other = (XArray) object;
            
            if((null != other.elements && !other.elements.equals(elements)) || (null == other.elements && other.elements != elements)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
}

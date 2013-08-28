package com.tigerknows.model.xobject;

import java.io.IOException;

/**
 * 
 * @author duliang
 * 
 */
class XBinaryData extends XObject {
    private byte[] value;

    protected XBinaryData(byte[] bytes) {
        super(XType.DATA);
        value = bytes;
    }

    @Override
    protected void writeContent(ByteWriter writer) throws IOException {
        writer.writeInt(value.length);
        writer.writeBytes(value);
    }

    protected static XBinaryData readContent(ByteReader reader)
            throws IOException {
        int length = (int) reader.readInt();
        return new XBinaryData(reader.readBytes(length));
    }

    protected byte[] getValue() {
        return value;
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof XBinaryData) {
            
            XBinaryData other = (XBinaryData) object;
            
            if(null != other.value && null != value) {
                if(other.value.length == value.length) {
                    int i = 0;
                    for(byte data : value) {
                        if (data != other.value[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            } else if(null == other.value && null == value) {
                return true;
            }
        }
        
        return false;
    }
}

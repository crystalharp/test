package com.tigerknows.model.xobject;


import java.io.IOException;
/**
 * 
 * @author duliang
 *
 */
public interface Writable {
    public void writeTo(ByteWriter writer) throws IOException;
}

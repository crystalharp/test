package com.tigerknows.maps;

public class TileDownload {

    private int rid;
    private int offset;
    private int length;
    
    public TileDownload() {
    }
    
    public TileDownload(int rid, int offset, int length) {
        super();
        this.rid = rid;
        this.offset = offset;
        this.length = length;
    }

    public int getRid() {
        return rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof TileDownload) {
            TileDownload other = (TileDownload)object;
            if (length == other.length && offset == other.offset && rid == other.rid) {
                return true;
            }
        }
        
        return false;
    }
    
    public TileDownload clone() {
        return new TileDownload(rid, offset, length);
    }

    @Override
    public String toString() {
        return "rid: " + rid + "\toffset: " + offset + "\t lenth: " + length;
    }
    
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        int hash = 3;
        hash = 29 * hash + rid;
        hash = 29 * hash + offset;
        hash = 29 * hash + length;
        return hash;
    }

}

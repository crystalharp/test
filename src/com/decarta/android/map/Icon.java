/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import com.decarta.android.util.XYInteger;

/**
 * The Icon class is used to create custom icon objects that can be overlaid
 * on a Map in conjunction with a Pin.
 */
public class Icon {
    public static final int OFFSET_LOCATION_LEFT_TOP = 0;
    public static final int OFFSET_LOCATION_RIGHT_TOP = 1;
    public static final int OFFSET_LOCATION_RIGHT_BOTTOM = 2;
    public static final int OFFSET_LOCATION_LEFT_BOTTOM = 3;
    public static final int OFFSET_LOCATION_CENTER = 4;
    public static final int OFFSET_LOCATION_CENTER_BOTTOM = 5;
	private XYInteger size;
	private Bitmap image;
	//icon image's top left corner will be placed at position.x-offset.x, position.y-offset.y
	private XYInteger offset;
	
	public Icon(Bitmap image){
		this(image, new XYInteger(20,30), new XYInteger(10,15));
	}

	public Icon(Bitmap image, XYInteger size, XYInteger offset) {
		setImage(image);
		this.size=size;
		this.offset=offset;
	}
	
	public Icon clone(){
	    return new Icon(image,new XYInteger(size.x,size.y),new XYInteger(offset.x,offset.y));
	}
	
	public Bitmap getImage() {
		return image;
	}
	
	public void setSize(XYInteger size) {
		this.size = size;
	}
    /**
     * set the image of icon
     */
	public void setImage(Bitmap image) {
		this.image = image;
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Icon)) {
			return false;
		}
		Icon other = (Icon) obj;
		if(this.image==other.image && this.size.equals(other.size) && this.offset.equals(other.offset))
			return true;
		return false;
	}
	public XYInteger getOffset() {
		return offset;
	}
	
	/**
	 * icon image's top left corner will be placed at position.x-offset.x, position.y-offset.y
	 * @param offset
	 */
	public void setOffset(XYInteger offset) {
		this.offset = offset;
	}

	public XYInteger getSize() {
		return size;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + (this.size != null ? this.size.hashCode() : 0);
		hash = 53 * hash + (this.offset != null ? this.offset.hashCode() : 0);
		hash = 53 * hash + (this.image != null ? this.image.hashCode() : 0);
		return hash;
	}

    public static Icon getIcon(Resources resources, int resId) {
        return getIcon(resources, resId, OFFSET_LOCATION_CENTER);
    }

    public static Icon getIcon(Resources resources, int resId, int offsetLocation) {
        Options ops=new Options();
        ops.inScaled=false;
        Bitmap bm=BitmapFactory.decodeResource(resources, resId, ops);
        int w = bm.getWidth();
        int h = bm.getHeight();
        XYInteger offset = null;
        switch (offsetLocation) {
            case OFFSET_LOCATION_LEFT_TOP:
                offset = new XYInteger(0, 0);
                break;
            case OFFSET_LOCATION_RIGHT_TOP:
                offset = new XYInteger(w, 0);
                break;
            case OFFSET_LOCATION_RIGHT_BOTTOM:
                offset = new XYInteger(w, h);
                break;
            case OFFSET_LOCATION_LEFT_BOTTOM:
                offset = new XYInteger(0, h);
                break;
            case OFFSET_LOCATION_CENTER:
                offset = new XYInteger(w/2, h/2);
                break;
            case OFFSET_LOCATION_CENTER_BOTTOM:
                offset = new XYInteger(w/2, h);
                break;
        }
        Icon icon=new Icon(bm,
                new XYInteger(w,h),
                offset);
        return icon;
    }
}

/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */

package com.decarta.android.map;

import android.graphics.Bitmap;

import com.decarta.android.util.XYInteger;

/**
 * The Icon class is used to create custom icon objects that can be overlaid
 * on a Map in conjunction with a Pin.
 */
public class Icon {
	private XYInteger size;
	private Bitmap image;
	//icon image's top left corner will be placed at position.x-offset.x, position.y-offset.y
	private XYInteger offset;
	private int order = -1;
	
	public Icon(Bitmap image){
		this(image, new XYInteger(20,30), new XYInteger(10,15));
	}

	public Icon(Bitmap image, XYInteger size, XYInteger offset) {
		this(image, size, offset, -1);
	}

    public Icon(Bitmap image, XYInteger size, XYInteger offset, int order) {
		setImage(image);
		this.size=size;
		this.offset=offset;
        this.order = order;
	}
	
	public Icon clone(){
	    return new Icon(image,new XYInteger(size.x,size.y),new XYInteger(offset.x,offset.y), this.order);
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
		if(this.image==other.image && this.size.equals(other.size) && this.offset.equals(other.offset) && this.order == other.order)
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
        hash = 53 * hash + order;
		return hash;
	}

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
	
}

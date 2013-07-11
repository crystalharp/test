/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.tigerknows.model.xobject.XMap;

public class Zhanlan extends BaseData implements Parcelable{
    
    public static final String NEED_FILELD = "0001041407080a0b0c0f101219";
    
    // 0x00 x_string uid uid
    public static final byte FIELD_UID = 0x00;

    // 0x01 x_string 展览名称 name
    public static final byte FIELD_NAME = 0x01;

    // 0x03 x_string 城市ID cityId
    public static final byte FIELD_CITY_ID = 0x03;

    // 0x04 x_string 图片信息 pictures
    public static final byte FIELD_PICTURES = 0x04;

    // 0x14 x_map   详情页图片信息 pictures 
    public static final byte FIELD_PICTURES_DETAIL = 0x14;

    // 0x05 x_string 展览详情 description
    public static final byte FIELD_DESCRIPTION = 0x05;

    // 0x06 x_string 关联的poi的id link_uid
    public static final byte FIELD_LINK_UID = 0x06;

    // 0x07 x_int 所在经度, 是普通的度数 × 10万 x
    public static final byte FIELD_X = 0x07;

    // 0x08 x_int 所在纬度, 是普通的度数 × 10万 y
    public static final byte FIELD_Y = 0x08;

    // 0x09 x_string 时间信息 time_info
    public static final byte FIELD_TIME_INFO = 0x09;

    // 0x0a x_string 时间描述 time_desc
    public static final byte FIELD_TIME_DESC = 0x0a;

    // 0x0b x_string 展览费用 price
    public static final byte FIELD_PRICE = 0x0b;

    // 0x0c x_string 联系电话 contact_tel
    public static final byte FIELD_CONTACT_TEL = 0x0c;

    // 0x0f x_string 展览地点名称 place_name
    public static final byte FIELD_PLACE_NAME = 0x0f;

    // 0x10 x_string 展览地点地址 address
    public static final byte FIELD_ADDRESS = 0x10;

    // 0x12 x_int 人气 hot
    public static final byte FIELD_HOT = 0x12;

    // 0x15 x_string 网址 url
    public static final byte FIELD_URL = 0x15;

    // 0x16 x_string 所属一级地片名 adminname
    public static final byte FIELD_ADMINNAME = 0x16;

    // 0x17 x_string 所属二级地片名 areaname
    public static final byte FIELD_AREANAME = 0x17;

    // 0x18 x_string 数据来源 source
    public static final byte FIELD_SOURCE = 0x18;

    // 0x19 x_string 与当前位置的距离 实时生成
    public static final byte FIELD_DISTANCE = 0x19;

    private String uid; // 0x00 x_string uid uid
    private String name; // 0x01 x_string 展览名称 name
    private String cityId; // 0x03 x_string 城市ID cityId
    private TKDrawable pictures; // 0x04 x_string 图片信息 pictures
    private TKDrawable picturesDetail; 
    private String description; // 0x05 x_string 展览详情 description
    private String linkUid; // 0x06 x_string 关联的poi的id link_uid
    private long x; // 0x07 x_int 所在经度, 是普通的度数 × 10万 x
    private long y; // 0x08 x_int 所在纬度, 是普通的度数 × 10万 y
    private String timeInfo; // 0x09 x_string 时间信息 time_info
    private String timeDesc; // 0x0a x_string 时间描述 time_desc
    private String price; // 0x0b x_string 展览费用 price
    private String contact_tel; // 0x0c x_string 联系电话 contact_tel
    private String placeName; // 0x0f x_string 展览地点名称 place_name
    private String address; // 0x10 x_string 展览地点地址 address
    private long hot; // 0x12 x_int 人气 hot
    private String url; // 0x15 x_string 网址 url
    private String adminname; // 0x16 x_string 所属一级地片名 adminname
    private String areaname; // 0x17 x_string 所属二级地片名 areaname
    private String source; // 0x18 x_string 数据来源 source
    private String distance; // 0x19 x_string 与当前位置的距离 实时生成
    
    private Position position;
    private POI poi;
    
    public Zhanlan() {
    }

    public Zhanlan (XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.uid = getStringFromData(FIELD_UID, reset ? null : this.uid);
        this.name = getStringFromData(FIELD_NAME, reset ? null : this.name);
        this.cityId = getStringFromData(FIELD_CITY_ID, reset ? null : this.cityId);
        this.pictures = getObjectFromData(FIELD_PICTURES, TKDrawable.Initializer, reset ? null : this.pictures);
        this.picturesDetail = getObjectFromData(FIELD_PICTURES_DETAIL, TKDrawable.Initializer, reset ? null : this.picturesDetail);
        this.description = getStringFromData(FIELD_DESCRIPTION, reset ? null : this.description);
        this.linkUid = getStringFromData(FIELD_LINK_UID, reset ? null : this.linkUid);
        this.position = getPositionFromData(FIELD_X, FIELD_Y, reset ? null : this.position);
        this.timeInfo = getStringFromData(FIELD_TIME_INFO, reset ? null : this.timeInfo);
        this.timeDesc = getStringFromData(FIELD_TIME_DESC, reset ? null : this.timeDesc);
        this.price = getStringFromData(FIELD_PRICE, reset ? null : this.price);
        this.contact_tel = getStringFromData(FIELD_CONTACT_TEL, reset ? null : this.contact_tel);
        this.placeName = getStringFromData(FIELD_PLACE_NAME, reset ? null : this.placeName);
        this.address = getStringFromData(FIELD_ADDRESS, reset ? null : this.address);
        this.hot = getLongFromData(FIELD_HOT, reset ? 0 : this.hot);
        this.url = getStringFromData(FIELD_URL, reset ? null : this.url);
        this.adminname = getStringFromData(FIELD_ADMINNAME, reset ? null : this.adminname);
        this.areaname = getStringFromData(FIELD_AREANAME, reset ? null : this.areaname);
        this.source = getStringFromData(FIELD_SOURCE, reset ? null : this.source);
        this.distance = getStringFromData(FIELD_DISTANCE, reset ? null : this.distance);
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Zhanlan) {
            Zhanlan other = (Zhanlan) object;
            if((null != other.uid && !other.uid.equals(this.uid)) || (null == other.uid && other.uid != this.uid)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    public POI getPOI() {
        if (poi == null) {
            poi = new POI();
            poi.setName(placeName);
            poi.setAddress(address);
            poi.setPosition(position);
            poi.setSourceType(POI.SOURCE_TYPE_ZHANLAN);
        }
        return poi;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public TKDrawable getPictures() {
        return pictures;
    }
    
    public TKDrawable getPicturesDetail() {
        return picturesDetail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkUid() {
        return linkUid;
    }

    public void setLinkUid(String linkUid) {
        this.linkUid = linkUid;
    }

    public String getTimeInfo() {
        return timeInfo;
    }

    public void setTimeInfo(String timeInfo) {
        this.timeInfo = timeInfo;
    }

    public String getTimeDesc() {
        return timeDesc;
    }

    public void setTimeDesc(String timeDesc) {
        this.timeDesc = timeDesc;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getContactTel() {
        return contact_tel;
    }

    public void setContact_tel(String contactTel) {
        contact_tel = contactTel;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getHot() {
        return hot;
    }

    public void setHot(long hot) {
        this.hot = hot;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdminname() {
        return adminname;
    }

    public void setAdminname(String adminname) {
        this.adminname = adminname;
    }

    public String getAreaname() {
        return areaname;
    }

    public void setAreaname(String areaname) {
        this.areaname = areaname;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }  

    public Position getPosition() {
        return position;
    }

    public Zhanlan(Parcel in){
    	uid = in.readString();
		name = in.readString();
		cityId = in.readString();
		pictures = in.readParcelable(TKDrawable.class.getClassLoader());
		picturesDetail = in.readParcelable(TKDrawable.class.getClassLoader());
		description = in.readString();
		linkUid = in.readString();
		x = in.readLong();
		y = in.readLong();
		timeInfo = in.readString();
		timeDesc = in.readString();
		price = in.readString();
		contact_tel = in.readString();
		placeName = in.readString();
		address = in.readString();
		hot = in.readLong();
		url = in.readString();
		adminname = in.readString();
		areaname = in.readString();
		source = in.readString();
		distance = in.readString();
		position = in.readParcelable(Position.class.getClassLoader());
    }

    public static final Parcelable.Creator<Zhanlan> CREATOR
		    = new Parcelable.Creator<Zhanlan>() {
		public Zhanlan createFromParcel(Parcel in) {
		    return new Zhanlan(in);
		}
		
		public Zhanlan[] newArray(int size) {
		    return new Zhanlan[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(uid);
		dest.writeString(name);
		dest.writeString(cityId);
		dest.writeParcelable(pictures, flags);
		dest.writeParcelable(picturesDetail, flags);
		dest.writeString(description);
		dest.writeString(linkUid);
		dest.writeLong(x);
		dest.writeLong(y);
		dest.writeString(timeInfo);
		dest.writeString(timeDesc);
		dest.writeString(price);
		dest.writeString(contact_tel);
		dest.writeString(placeName);
		dest.writeString(address);
		dest.writeLong(hot);
		dest.writeString(url);
		dest.writeString(adminname);
		dest.writeString(areaname);
		dest.writeString(source);
		dest.writeString(distance);
		dest.writeParcelable(position, flags);
	}

	
	@Override
	public String toString() {
		return "Zhanlan [uid=" + uid + ", name=" + name + ", cityId=" + cityId
				+ ", pictures=" + pictures + ", picturesDetail="
				+ picturesDetail + ", description=" + description
				+ ", linkUid=" + linkUid + ", x=" + x + ", y=" + y
				+ ", timeInfo=" + timeInfo + ", timeDesc=" + timeDesc
				+ ", price=" + price + ", contact_tel=" + contact_tel
				+ ", placeName=" + placeName + ", address=" + address
				+ ", hot=" + hot + ", url=" + url + ", adminname=" + adminname
				+ ", areaname=" + areaname + ", source=" + source
				+ ", distance=" + distance + ", position=" + position
				+ ", poi=" + poi + "]";
	}

    public static XMapInitializer<Zhanlan> Initializer = new XMapInitializer<Zhanlan>() {

        @Override
        public Zhanlan init(XMap data) throws APIException {
            return new Zhanlan(data);
        }
    };
	
}

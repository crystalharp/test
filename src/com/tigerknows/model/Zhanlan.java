/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XMap;

public class Zhanlan extends BaseData {
    
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
    private int orderNumber;
    private POI poi;
    
    public Zhanlan() {
    }

    public Zhanlan (XMap data) throws APIException {
        super(data);
        init(data);
    }
    
    public void init(XMap data) throws APIException {
        super.init(data);
        if (this.data.containsKey(FIELD_UID)) {
            this.uid = this.data.getString(FIELD_UID);
        }
        if (this.data.containsKey(FIELD_NAME)) {
            this.name = this.data.getString(FIELD_NAME);
        }
        if (this.data.containsKey(FIELD_CITY_ID)) {
            this.cityId = this.data.getString(FIELD_CITY_ID);
        }
        if (this.data.containsKey(FIELD_PICTURES)) {
            this.pictures = new TKDrawable(this.data.getXMap(FIELD_PICTURES));
        }
        if (this.data.containsKey(FIELD_PICTURES_DETAIL)) {
            this.picturesDetail = new TKDrawable(this.data.getXMap(FIELD_PICTURES_DETAIL));
        }
        if (this.data.containsKey(FIELD_DESCRIPTION)) {
            this.description = this.data.getString(FIELD_DESCRIPTION);
        }
        if (this.data.containsKey(FIELD_LINK_UID)) {
            this.linkUid = this.data.getString(FIELD_LINK_UID);
        }
        if (this.data.containsKey(FIELD_X) && this.data.containsKey(FIELD_Y)) {
            this.x = this.data.getInt(FIELD_X);
            this.y = this.data.getInt(FIELD_Y);
            this.position = new Position(((double)this.y)/TKConfig.LON_LAT_DIVISOR, ((double)this.x)/TKConfig.LON_LAT_DIVISOR);
        }
        if (this.data.containsKey(FIELD_TIME_INFO)) {
            this.timeInfo = this.data.getString(FIELD_TIME_INFO);
        }
        if (this.data.containsKey(FIELD_TIME_DESC)) {
            this.timeDesc = this.data.getString(FIELD_TIME_DESC);
        }
        if (this.data.containsKey(FIELD_PRICE)) {
            this.price = this.data.getString(FIELD_PRICE);
        }
        if (this.data.containsKey(FIELD_CONTACT_TEL)) {
            this.contact_tel = this.data.getString(FIELD_CONTACT_TEL);
        }
        if (this.data.containsKey(FIELD_PLACE_NAME)) {
            this.placeName = this.data.getString(FIELD_PLACE_NAME);
        }
        if (this.data.containsKey(FIELD_ADDRESS)) {
            this.address = this.data.getString(FIELD_ADDRESS);
        }
        if (this.data.containsKey(FIELD_HOT)) {
            this.hot = this.data.getInt(FIELD_HOT);
        }
        if (this.data.containsKey(FIELD_URL)) {
            this.url = this.data.getString(FIELD_URL);
        }
        if (this.data.containsKey(FIELD_ADMINNAME)) {
            this.adminname = this.data.getString(FIELD_ADMINNAME);
        }
        if (this.data.containsKey(FIELD_AREANAME)) {
            this.areaname = this.data.getString(FIELD_AREANAME);
        }
        if (this.data.containsKey(FIELD_SOURCE)) {
            this.source = this.data.getString(FIELD_SOURCE);
        }
        if (this.data.containsKey(FIELD_DISTANCE)) {
            this.distance = this.data.getString(FIELD_DISTANCE);
        }
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
//            poi.setOrderNumber(orderNumber);
            poi.setSourceType(POI.SOURCE_TYPE_ZHANLAN);
            poi.setAssociatedObject(this);
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
    
    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }
}

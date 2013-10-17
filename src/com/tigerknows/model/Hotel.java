/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.Utility;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 酒店
 */
public class Hotel extends XMapData {
    
    public static final String NEED_FILED_LIST = "5051525357";
    
    public static final String NEED_FILED_DETAIL = "5455565859";
    
    public static final String NEED_FILED_ROOM_INFO = "5055";
    
    // 0x50 x_string 酒店ID
    public static final byte FIELD_UUID = 0x50;

    // 0x51 x_string 合作商来源
    public static final byte FIELD_SOURCE = 0x51;

    // 0x52 x_string 酒店品牌，{ "如家", ... "其它" }
    public static final byte FIELD_BRAND = 0x52;

    // 0x53 x_string 列表页图片的url
    public static final byte FIELD_IMAGE_THUMB = 0x53;

    // 0x54 x_array<x_map> 酒店相关的所有x_array<图片>
    public static final byte FIELD_IMAGE_LIST = 0x54;

    // 0x55 x_array<x_map> x_array<房型套餐>
    public static final byte FIELD_ROOM_TYPE_LIST = 0x55;

    // 0x56 x_string 酒店设施服务
    public static final byte FIELD_SERVICE = 0x56;

    // 0x57 x_int 是否可预订，0为否，大于0为是
    public static final byte FIELD_CAN_RESERVE = 0x57;
    
    // 0x58 x_string 房间服务设施
    public static final byte FIELD_ROOM_DESCRIPTION = 0x58;
    
    // 0x59 x_string 长简介
    public static final byte FIELD_LONG_DESCRIPTION = 0x59;
    
    // 0x50 x_string 酒店ID
    private String uuid;

    // 0x51 x_string 合作商来源
    private String source;

    // 0x52 x_string 酒店品牌，{ "如家", ... "其它" }
    private long brand;

    // 0x53 x_string 列表页图片的url
    private TKDrawable imageThumb;
    private String imageThumbUrl;
    
    /**
     * 仅用于绘制infowindow时
     */
    private Drawable imageInfoWindow;

    // 0x54 x_array<x_map> 酒店相关的所有x_array<图片>
    private List<HotelTKDrawable> hotelTKDrawableList;
    
    private List<HotelTKDrawable> originalHotelTKDrawableList;

    // 0x55 x_array<x_map> x_array<房型套餐>
    private List<RoomType> roomTypeList;

    // 0x56 x_string 酒店设施服务
    private String service;

    // 0x57 x_int 是否可预订，0为否，大于0为是
    private long canReserve;
    
    // 0x58 x_string 房间服务设施
    private String roomDescription;
    
    // 0x59 x_string 长简介
    private String longDescription;
    
    Hotel() {
        
    }

    public Hotel(XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    @Override
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        
        this.uuid = getStringFromData(FIELD_UUID, reset ? null : this.uuid);
        this.source = getStringFromData(FIELD_SOURCE, reset ? null : this.source);
        this.brand = getLongFromData(FIELD_BRAND, reset ? 0 : this.brand);
        
        imageThumbUrl = getStringFromData(FIELD_IMAGE_THUMB);
        if (imageThumbUrl != null) {
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(imageThumbUrl, Globals.getPicWidthHeight(TKConfig.PICTURE_HOTEL_LIST)));
            this.imageThumb = tkDrawable;
        } else if (reset) {
            this.imageThumb = null;
        }
        this.originalHotelTKDrawableList = getListFromData(FIELD_IMAGE_LIST, HotelTKDrawable.Initializer, reset ? null : this.originalHotelTKDrawableList);
        if (originalHotelTKDrawableList == null) {
            hotelTKDrawableList = null;
        } else {
            hotelTKDrawableList = new ArrayList<Hotel.HotelTKDrawable>();
            for(int i = 0, size = originalHotelTKDrawableList.size(); i < size; i++) {
                HotelTKDrawable originalHotelTKDrawable = originalHotelTKDrawableList.get(i);
                HotelTKDrawable hotelTKDrawable = new HotelTKDrawable();
                hotelTKDrawable.setName(originalHotelTKDrawable.name);
                if (originalHotelTKDrawable.tkDrawable != null) {
                    TKDrawable tkDrawable = new TKDrawable();
                    tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(originalHotelTKDrawable.tkDrawable.getUrl(), Globals.getPicWidthHeight(TKConfig.PICTURE_HOTEL_LIST)));
                    hotelTKDrawable.setTkDrawable(tkDrawable);
                }
                hotelTKDrawableList.add(hotelTKDrawable);
            }
        }
        this.roomTypeList = getListFromData(FIELD_ROOM_TYPE_LIST, RoomType.Initializer, reset ? null : this.roomTypeList);
        this.service = getStringFromData(FIELD_SERVICE, reset ? null : this.service);
        this.canReserve = getLongFromData(FIELD_CAN_RESERVE, reset ? 0 : this.canReserve);
        this.longDescription = getStringFromData(FIELD_LONG_DESCRIPTION, reset ? null : this.longDescription);
        this.roomDescription = getStringFromData(FIELD_ROOM_DESCRIPTION, reset ? null : this.roomDescription);
        
        if (reset == false) {
            this.data = null;
        }
    }
    
    public XMap getData() {
        if (this.data == null) {
            XMap data = new XMap();
            if (uuid != null) {
                data.put(FIELD_UUID, uuid);
            }
            if (source != null) {
                data.put(FIELD_SOURCE, source);
            }
            if (brand != 0) {
                data.put(FIELD_BRAND, brand);
            }
            if (imageThumbUrl != null) {
                data.put(FIELD_IMAGE_THUMB, imageThumbUrl);
            }
            if (originalHotelTKDrawableList != null) {
                XArray<XMap> xArray = new XArray<XMap>();
                for(int i = 0, size = originalHotelTKDrawableList.size(); i < size; i++) {
                    xArray.add(originalHotelTKDrawableList.get(i).getData());
                }
                data.put(FIELD_IMAGE_LIST, xArray);
            }
            if (this.roomTypeList != null) {
                XArray<XMap> xArray = new XArray<XMap>();
                for(int i = 0, size = roomTypeList.size(); i < size; i++) {
                    xArray.add(roomTypeList.get(i).getData());
                }
                data.put(FIELD_ROOM_TYPE_LIST, xArray);
            }
            if (service != null) {
                data.put(FIELD_SERVICE, service);
            }
            // TODO 是否可预订状态只在第一次初始化时赋值，后续不能被更新
//            if (canReserve != 0) {
//                data.put(FIELD_CAN_RESERVE, canReserve);
//            }
            if (longDescription != null) {
                data.put(FIELD_LONG_DESCRIPTION, longDescription);
            }
            if (roomDescription != null) {
                data.put(FIELD_ROOM_DESCRIPTION, roomDescription);
            }
            this.data = data;
        }
        return data;
    }
    
    public String getUuid() {
        return uuid;
    }

    public String getSource() {
        return source;
    }

    public long getBrand() {
        return brand;
    }

    public TKDrawable getImageThumb() {
        return imageThumb;
    }

    public Drawable getImageInfoWindow() {
        return imageInfoWindow;
    }

    public void setImageInfoWindow(Drawable imageInfoWindow) {
        this.imageInfoWindow = imageInfoWindow;
    }

    public List<HotelTKDrawable> getHotelTKDrawableList() {
        return hotelTKDrawableList;
    }

    public List<HotelTKDrawable> getOriginalHotelTKDrawableList() {
        return originalHotelTKDrawableList;
    }

    public List<RoomType> getRoomTypeList() {
        return roomTypeList;
    }

    public String getService() {
        return service;
    }

    public long getCanReserve() {
        return canReserve;
    }
    
    public String getRoomDescription(){
        return roomDescription;
    }
    
    public String getLongDescription(){
        return longDescription;
    }

    public static class HotelTKDrawable extends XMapData implements Parcelable {

        // 0x01 x_string 图片名
        public static final byte FIELD_NAME = 0x01;

        // 0x02 x_string 图片url
        public static final byte FIELD_URL = 0x02;

        private String name;

        private TKDrawable tkDrawable;
        
        public HotelTKDrawable() {
            this.data = new XMap();
        }

        public HotelTKDrawable(XMap data) throws APIException {
            super(data);
            this.name = getStringFromData(FIELD_NAME);
            String url = getStringFromData(FIELD_URL);
            if (url != null) {
                tkDrawable = new TKDrawable();
                tkDrawable.setUrl(url);
            }
        }

        public String getName() {
            return name;
        }

        public TKDrawable getTKDrawable() {
            return tkDrawable;
        }

        public void setTkDrawable(TKDrawable tkDrawable) {
            String url = null;
            if (tkDrawable != null) {
                url = tkDrawable.getUrl();
            }
            getData().put(FIELD_URL, url);
            this.tkDrawable = tkDrawable;
        }

        public void setName(String name) {
            getData().put(FIELD_NAME, name);
            this.name = name;
        }
        
        public HotelTKDrawable clone() {
            HotelTKDrawable other = null;
            try {
                other = new HotelTKDrawable(getData());
            } catch (APIException e) {
                e.printStackTrace();
            }
            return other;
        }

        public static XMapInitializer<HotelTKDrawable> Initializer = new XMapInitializer<Hotel.HotelTKDrawable>() {

            @Override
            public HotelTKDrawable init(XMap data) throws APIException {
                return new HotelTKDrawable(data);
            }
        };

        public static final Parcelable.Creator<HotelTKDrawable> CREATOR
                = new Parcelable.Creator<HotelTKDrawable>() {
            public HotelTKDrawable createFromParcel(Parcel in) {
                return new HotelTKDrawable(in);
            }

            public HotelTKDrawable[] newArray(int size) {
                return new HotelTKDrawable[size];
            }
        };
        
        private HotelTKDrawable(Parcel in) {
            name = in.readString();
            tkDrawable = in.readParcelable(TKDrawable.class.getClassLoader());
        }
        
        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(name);
            out.writeParcelable(tkDrawable, flags);
        }
    }

    public static class RoomType extends XMapData {

        // 0x00 x_string 房型id room_id
        public static final byte FIELD_ROOM_ID = 0x00;

        // 0x01 x_string 套餐id rateplan_id
        public static final byte FIELD_RATEPLAN_ID = 0x01;

        // 0x02 x_string 房型名称 room_type
        public static final byte FIELD_ROOM_TYPE = 0x02;
        
        // 0x03 x_string 所在楼层 floor
        public static final byte FIELD_FLOOR = 0x03;

        // 0x04 x_string 床型 bed_type
        public static final byte FIELD_BED_TYPE = 0x04;

        // 0x05 x_string 房型面积 area
        public static final byte FIELD_AREA = 0x05;

        // 0x06 x_string 宽带，{ "宽带", "无", "宽带收费", "宽带免费" } net_service
        public static final byte FIELD_NET_SERVICE = 0x06;

        // 0x07 x_string 含早情况 breakfast
        public static final byte FIELD_BREAKFAST = 0x07;

        // 0x08 x_string 当前价格 动态获取
        public static final byte FIELD_PRICE = 0x08;

        // 0x09 x_int 可预订情况，0不可预订，1可预订 动态判断
        public static final byte FIELD_CAN_RESERVE = 0x09;
        
        // 0x10 x_int 是否需要担保，0为不需要，1为需要
        public static final byte FIELD_NEED_GUARANTEE = 0x10;
        
        // 0x11 x_string 房型套餐的特殊说明
        public static final byte FIELD_SUBTITLE = 0x11;
        
        // 0x12 x_int 第三方商家来源ID
        public static final byte FIELD_VENDOR_ID = 0x12;
        
        // 0x13 x_string 第三方商家来源名字
        public static final byte FIELD_VENDOR_NAME = 0x13;

        // 0x00 x_string 房型id room_id
        private String roomId;

        // 0x01 x_string 套餐id rateplan_id
        private String rateplanId;
        
        // 0x02 x_string 房型名称
        private String roomType;

        // 0x03 x_string 所在楼层 floor
        private String floor;

        // 0x04 x_string 床型 bed_type
        private String bedType;

        // 0x05 x_string 房型面积 area
        private String area;

        // 0x06 x_string 宽带，{ "宽带", "无", "宽带免费", "宽带收费" } net_service
        private String netService;

        // 0x07 x_string 含早情况 breakfast
        private String breakfast;

        // 0x08 x_string 当前价格 动态获取
        private String price;

        // 0x09 x_int 可预订情况，0不可预订，1可预订 动态判断
        private long canReserve;
        
        // 0x10 x_int 是否需要担保，0为不需要，1为需要
        private long needGuarantee;
        
        // 0x11 x_string 房型套餐的特殊说明
        private String subtitle;
        
        // 0x12 x_int 第三方商家来源ID
        private long vendorId;
        
        // 0x13 x_string 第三方商家来源名字
        private String vendorName;

        public RoomType(XMap data) throws APIException {
            super(data);
            this.roomId = getStringFromData(FIELD_ROOM_ID);
            this.rateplanId = getStringFromData(FIELD_RATEPLAN_ID);
            this.roomType = getStringFromData(FIELD_ROOM_TYPE);
            this.floor = getStringFromData(FIELD_FLOOR);
            this.bedType = getStringFromData(FIELD_BED_TYPE);
            this.area = getStringFromData(FIELD_AREA);
            this.netService = getStringFromData(FIELD_NET_SERVICE);
            this.breakfast = getStringFromData(FIELD_BREAKFAST);
            this.price = getStringFromData(FIELD_PRICE);
            this.canReserve = getLongFromData(FIELD_CAN_RESERVE);
            this.needGuarantee = getLongFromData(FIELD_NEED_GUARANTEE);
            this.subtitle = getStringFromData(FIELD_SUBTITLE);
            this.vendorId = getLongFromData(FIELD_VENDOR_ID);
            this.vendorName = getStringFromData(FIELD_VENDOR_NAME);
        }

        public String getRoomId() {
            return roomId;
        }

        public String getRateplanId() {
            return rateplanId;
        }
        
        public String getRoomType() {
        	return roomType;
        }

        public String getFloor() {
            return floor;
        }

        public String getBedType() {
            return bedType;
        }

        public String getArea() {
            return area;
        }

        public String getNetService() {
            return netService;
        }

        public String getBreakfast() {
            return breakfast;
        }

        public String getPrice() {
            return price;
        }

        public long getCanReserve() {
            return canReserve;
        }
        
        public void setCanReserve(long can) {
            canReserve = can;
        }
        
        public long getNeedGuarantee() {
            return needGuarantee;
        }
        
        public String getSubtitle() {
            return subtitle;
        }
        
        public long getVendorID(){
        	return vendorId;
        }
        
        public String getVendorName(){
        	return vendorName;
        }

        public static XMapInitializer<RoomType> Initializer = new XMapInitializer<RoomType>() {

            @Override
            public RoomType init(XMap data) throws APIException {
                return new RoomType(data);
            }
        };

    }

    public static XMapInitializer<Hotel> Initializer = new XMapInitializer<Hotel>() {

        @Override
        public Hotel init(XMap data) throws APIException {
            return new Hotel(data);
        }
    };
}

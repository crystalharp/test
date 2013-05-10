/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

import java.util.List;

/**
 * 酒店
 */
public class Hotel extends XMapData {
    
    public static final String NEED_FILED = "50515253545657";
    
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
    
    // 0x50 x_string 酒店ID
    private String uuid;

    // 0x51 x_string 合作商来源
    private String source;

    // 0x52 x_string 酒店品牌，{ "如家", ... "其它" }
    private String brand;

    // 0x53 x_string 列表页图片的url
    private TKDrawable imageThumb;

    // 0x54 x_array<x_map> 酒店相关的所有x_array<图片>
    private List<HotelTKDrawable> hotelTKDrawableList;

    // 0x55 x_array<x_map> x_array<房型套餐>
    private List<RoomType> roomTypeList;

    // 0x56 x_string 酒店设施服务
    private String service;

    // 0x57 x_int 是否可预订，0为否，大于0为是
    private long canReserve;

    public Hotel(XMap data) throws APIException {
        super(data);
        this.uuid = getStringFromData(FIELD_UUID);
        this.source = getStringFromData(FIELD_SOURCE);
        this.brand = getStringFromData(FIELD_BRAND);
        
        String imageThumb = getStringFromData(FIELD_IMAGE_THUMB);
        if (imageThumb != null) {
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(imageThumb);
            this.imageThumb = tkDrawable;
        }
        this.hotelTKDrawableList = getListFromData(FIELD_IMAGE_LIST, HotelTKDrawable.Initializer);
        this.roomTypeList = getListFromData(FIELD_ROOM_TYPE_LIST, RoomType.Initializer);
        this.service = getStringFromData(FIELD_SERVICE);
        this.canReserve = getLongFromData(FIELD_CAN_RESERVE);
    }
    
    public String getUuid() {
        return uuid;
    }

    public String getSource() {
        return source;
    }

    public String getBrand() {
        return brand;
    }

    public TKDrawable getImageThumb() {
        return imageThumb;
    }

    public List<HotelTKDrawable> getHotelTKDrawableList() {
        return hotelTKDrawableList;
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

    public static class HotelTKDrawable extends XMapData {

        // 0x01 x_string 图片名
        public static final byte FIELD_NAME = 0x01;

        // 0x02 x_string 图片url
        public static final byte FIELD_URL = 0x02;

        private String name;

        private TKDrawable tkDrawable;

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

        public static XMapInitializer<HotelTKDrawable> Initializer = new XMapInitializer<Hotel.HotelTKDrawable>() {

            @Override
            public HotelTKDrawable init(XMap data) throws APIException {
                return new HotelTKDrawable(data);
            }
        };

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

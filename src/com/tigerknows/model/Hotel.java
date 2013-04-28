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
    
    private POI poi;

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
        poi = new POI(this.data);
        if (this.data.containsKey(FIELD_UUID)) {
            this.uuid = this.data.getString(FIELD_UUID);
        }
        if (this.data.containsKey(FIELD_SOURCE)) {
            this.source = this.data.getString(FIELD_SOURCE);
        }
        if (this.data.containsKey(FIELD_BRAND)) {
            this.brand = this.data.getString(FIELD_BRAND);
        }
        if (this.data.containsKey(FIELD_IMAGE_THUMB)) {
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(this.data.getString(FIELD_IMAGE_THUMB));
            this.imageThumb = tkDrawable;
        }
        if (this.data.containsKey(FIELD_IMAGE_LIST)) {
            this.hotelTKDrawableList = getListFromXArray(HotelTKDrawable.getXMapInitializer(),
                    data, FIELD_IMAGE_LIST);
        }
        if (this.data.containsKey(FIELD_ROOM_TYPE_LIST)) {
            this.roomTypeList = getListFromXArray(RoomType.getXMapInitializer(), data,
                    FIELD_ROOM_TYPE_LIST);
        }
        if (this.data.containsKey(FIELD_SERVICE)) {
            this.service = this.data.getString(FIELD_SERVICE);
        }
        if (this.data.containsKey(FIELD_CAN_RESERVE)) {
            this.canReserve = this.data.getInt(FIELD_CAN_RESERVE);
        }
    }
    
    public POI getPOI() {
        return poi;
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
            if (this.data.containsKey(FIELD_NAME)) {
                this.name = this.data.getString(FIELD_NAME);
            }
            if (this.data.containsKey(FIELD_URL)) {
                String url = this.data.getString(FIELD_URL);
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

        private static XMapInitializer<HotelTKDrawable> initializer = new XMapInitializer<Hotel.HotelTKDrawable>() {

            @Override
            public HotelTKDrawable init(XMap data) throws APIException {
                return new HotelTKDrawable(data);
            }
        };

        public static XMapInitializer<HotelTKDrawable> getXMapInitializer() {
            return initializer;
        }

    }

    public static class RoomType extends XMapData {

        // 0x00 x_string 房型id room_id
        public static final byte FIELD_ROOM_ID = 0x00;

        // 0x01 x_string 套餐id rateplan_id
        public static final byte FIELD_RATEPLAN_ID = 0x01;

        // 0x02 x_string 所在楼层 floor
        public static final byte FIELD_FLOOR = 0x02;

        // 0x03 x_string 床型 bed_type
        public static final byte FIELD_BED_TYPE = 0x03;

        // 0x04 x_string 房型面积 area
        public static final byte FIELD_AREA = 0x04;

        // 0x05 x_string 宽带，{ "宽带", "无" } net_service
        public static final byte FIELD_NET_SERVICE = 0x05;

        // 0x06 x_string 宽带免费，{ "免费", "收费" } net_service_fee
        public static final byte FIELD_NET_SERVICE_FEE = 0x06;

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

        // 0x02 x_string 所在楼层 floor
        private String floor;

        // 0x03 x_string 床型 bed_type
        private String bedType;

        // 0x04 x_string 房型面积 area
        private String area;

        // 0x05 x_string 宽带，{ "宽带", "无" } net_service
        private String netService;

        // 0x06 x_string 宽带免费，{ "免费", "收费" } net_service_fee
        private String netServiceFee;

        // 0x07 x_string 含早情况 breakfast
        private String breakfast;

        // 0x08 x_string 当前价格 动态获取
        private String price;

        // 0x09 x_int 可预订情况，0不可预订，1可预订 动态判断
        private String canReserve;

        public RoomType(XMap data) throws APIException {
            super(data);
            if (this.data.containsKey(FIELD_ROOM_ID)) {
                this.roomId = this.data.getString(FIELD_ROOM_ID);
            }
            if (this.data.containsKey(FIELD_RATEPLAN_ID)) {
                this.rateplanId = this.data.getString(FIELD_RATEPLAN_ID);
            }
            if (this.data.containsKey(FIELD_FLOOR)) {
                this.floor = this.data.getString(FIELD_FLOOR);
            }
            if (this.data.containsKey(FIELD_BED_TYPE)) {
                this.bedType = this.data.getString(FIELD_BED_TYPE);
            }
            if (this.data.containsKey(FIELD_AREA)) {
                this.area = this.data.getString(FIELD_AREA);
            }
            if (this.data.containsKey(FIELD_NET_SERVICE)) {
                this.netService = this.data.getString(FIELD_NET_SERVICE);
            }
            if (this.data.containsKey(FIELD_NET_SERVICE_FEE)) {
                this.netServiceFee = this.data.getString(FIELD_NET_SERVICE_FEE);
            }
            if (this.data.containsKey(FIELD_BREAKFAST)) {
                this.breakfast = this.data.getString(FIELD_BREAKFAST);
            }
            if (this.data.containsKey(FIELD_PRICE)) {
                this.price = this.data.getString(FIELD_PRICE);
            }
            if (this.data.containsKey(FIELD_CAN_RESERVE)) {
                this.canReserve = this.data.getString(FIELD_CAN_RESERVE);
            }
        }

        public String getRoomId() {
            return roomId;
        }

        public String getRateplanId() {
            return rateplanId;
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

        public String getNetServiceFee() {
            return netServiceFee;
        }

        public String getBreakfast() {
            return breakfast;
        }

        public String getPrice() {
            return price;
        }

        public String getCanReserve() {
            return canReserve;
        }

        public static XMapInitializer<RoomType> getXMapInitializer() {
            return initializer;
        }

        private static XMapInitializer<RoomType> initializer = new XMapInitializer<RoomType>() {

            @Override
            public RoomType init(XMap data) throws APIException {
                return new RoomType(data);
            }
        };

    }

    private static XMapInitializer<Hotel> initializer = new XMapInitializer<Hotel>() {

        @Override
        public Hotel init(XMap data) throws APIException {
            return new Hotel(data);
        }
    };

    public static XMapInitializer<Hotel> getXMapInitializer() {
        return initializer;
    }
}

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.android.location.Position;
import com.tigerknows.model.xobject.XMap;

public class Fendian extends BaseData {
    
    public static final String NEED_FIELD = "50565758595b5c";
    
    // 0x50 x_string uid uid
    public static final byte FIELD_UID = 0x50;

    // 0x51 x_int 分店id brc_id
    public static final byte FIELD_BRC_ID = 0x51;

    // 0x52 x_string 关联团购id tuan_id
    public static final byte FIELD_TUAN_ID = 0x52;

    // 0x53 x_string 关联的poi的id link_uid
    public static final byte FIELD_LINK_UID = 0x53;

    // 0x54 x_string 所属一级地片名 adminname
    public static final byte FIELD_ADMINNAME = 0x54;

    // 0x55 x_string 所属二级地片名 areaname
    public static final byte FIELD_AREANAME = 0x55;

    // 0x56 x_int 所在经度, 是普通的度数 × 10万 x
    public static final byte FIELD_X = 0x56;

    // 0x57 x_int 所在纬度, 是普通的度数 × 10万 y
    public static final byte FIELD_Y = 0x57;

    // 0x58 x_string 分店名称 place_name
    public static final byte FIELD_PLACE_NAME = 0x58;

    // 0x59 x_string 分店电话 place_phone
    public static final byte FIELD_PLACE_PHONE = 0x59;

    // 0x5a x_sring 团购商家营业时间 open_time
    public static final byte FIELD_OPEN_TIME = 0x5a;

    // 0x5b x_string 分店地址 address
    public static final byte FIELD_ADDRESS = 0x5b;

    // 0x5c x_string 分店距当前位置距离 实时计算
    public static final byte FIELD_DISTANCE = 0x5c;

    private String uid; // 0x50 x_string uid uid
    private long brcId; // 0x51 x_int 分店id brc_id
    private String tuanId; // 0x52 x_string 关联团购id tuan_id
    private String linkUid; // 0x53 x_string 关联的poi的id link_uid
    private String adminname; // 0x54 x_string 所属一级地片名 adminname
    private String areaname; // 0x55 x_string 所属二级地片名 areaname
    private String placeName; // 0x58 x_string 分店名称 place_name
    private String placePhone; // 0x59 x_string 分店电话 place_phone
    private String openTime; // 0x5a x_sring 团购商家营业时间 open_time
    private String address; // 0x5b x_string 分店地址 address
    private String distance; // 0x5c x_string 分店距当前位置距离 实时计算
    
    private Position position;
    private POI poi;
    
    public Fendian() {
    }

    public Fendian (XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.uid = getStringFromData(FIELD_UID, reset ? null : this.uid);
        this.brcId = getLongFromData(FIELD_BRC_ID, reset ? 0 : this.brcId);
        this.tuanId = getStringFromData(FIELD_TUAN_ID, reset ? null : this.tuanId);
        this.linkUid = getStringFromData(FIELD_LINK_UID, reset ? null : this.linkUid);
        this.adminname = getStringFromData(FIELD_ADMINNAME, reset ? null : this.adminname);
        this.areaname = getStringFromData(FIELD_AREANAME, reset ? null : this.areaname);
        this.position = getPositionFromData(FIELD_X, FIELD_Y, reset ? null : this.position);
        this.placeName = getStringFromData(FIELD_PLACE_NAME, reset ? null : this.placeName);
        this.placePhone = getStringFromData(FIELD_PLACE_PHONE, reset ? null : this.placePhone);
        this.openTime = getStringFromData(FIELD_OPEN_TIME, reset ? null : this.openTime);
        this.address = getStringFromData(FIELD_ADDRESS, reset ? null : this.address);
        this.distance = getStringFromData(FIELD_DISTANCE, reset ? null : this.distance);
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Fendian) {
            Fendian other = (Fendian) object;
            if((null != other.uid && !other.uid.equals(this.uid)) || (null == other.uid && other.uid != this.uid)) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getBrcId() {
        return brcId;
    }

    public void setBrcId(long brcId) {
        this.brcId = brcId;
    }

    public String getTuanId() {
        return tuanId;
    }

    public void setTuanId(String tuanId) {
        this.tuanId = tuanId;
    }

    public String getLinkUid() {
        return linkUid;
    }

    public void setLinkUid(String linkUid) {
        this.linkUid = linkUid;
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

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getPlacePhone() {
        return placePhone;
    }

    public void setPlacePhone(String placePhone) {
        this.placePhone = placePhone;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
    
	@Override
	public POI getPOI() {
        if (poi == null) {
            poi = new POI();
            poi.setName(placeName);
            poi.setAddress(address);
            poi.setPosition(position);
        }
        return poi;
	}

    public static XMapInitializer<Fendian> Initializer = new XMapInitializer<Fendian>() {

        @Override
        public Fendian init(XMap data) throws APIException {
            return new Fendian(data);
        }
    };
    
}

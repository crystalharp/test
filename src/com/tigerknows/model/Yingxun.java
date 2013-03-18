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
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import java.util.ArrayList;
import java.util.List;

public class Yingxun extends BaseData implements Parcelable {

    public static final String NEED_FILELD = "20262728292a2b2c";
        
    // 0x20 x_string UUID uid
    public static final byte FIELD_UID = 0x20;
    
    // 0x21 x_string 影院的cityId cityId
    public static final byte FIELD_CITY_ID = 0x21;
    
    // 0x22 x_string 影院的poi的uid link_uid
    public static final byte FIELD_LINK_UID = 0x22;
    
    // 0x23 x_string 电影的uid movie_uid
    public static final byte FIELD_MOVIE_UID = 0x23;
    
    // 0x24 x_string 影院所属一级地片名 adminname
    public static final byte FIELD_ADMINNAME = 0x24;
    
    // 0x25 x_string 影院所属二级地片名 areaname
    public static final byte FIELD_AREANAME = 0x25;
    
    // 0x26 x_double 影院的经度 x
    public static final byte FIELD_X = 0x26;
    
    // 0x27 x_double 影院的纬度 y
    public static final byte FIELD_Y = 0x27;
    
    // 0x28 x_string 影院对应poi名称 name
    public static final byte FIELD_NAME = 0x28;
    
    // 0x29 x_string 影院对应poi地址 address
    public static final byte FIELD_ADDRESS = 0x29;
    
    // 0x2a x_string 影院对应poi电话 phone
    public static final byte FIELD_PHONE = 0x2a;
    
    // 0x2b x_string 该影讯所对应的影院到当前位置的距离 实时计算生成
    public static final byte FIELD_DISTANCE = 0x2b;
    
    // 0x2c x_array 该影讯所对应电影在该影院的播放场次列表 通过关联changci表得到
    public static final byte FIELD_CHANGCI_LIST = 0x2c;

    private String uid; // 0x20 x_string UUID uid
    private String cityId; // 0x21 x_string 影院的cityId cityId
    private String linkUid; // 0x22 x_string 影院的poi的uid link_uid
    private String movieUid; // 0x23 x_string 电影的uid movie_uid
    private String adminname; // 0x24 x_string 影院所属一级地片名 adminname
    private String areaname; // 0x25 x_string 影院所属二级地片名 areaname
    private long x; // 0x26 x_double 影院的经度 x
    private long y; // 0x27 x_double 影院的纬度 y
    private String name; // 0x28 x_string 影院对应poi名称 name
    private String address; // 0x29 x_string 影院对应poi地址 address
    private String phone; // 0x2a x_string 影院对应poi电话 phone
    private String distance; // 0x2b x_string 该影讯所对应的影院到当前位置的距离 实时计算生成
    private List<Changci> changciList = new ArrayList<Changci>(); // 0x2c x_array 该影讯所对应电影在该影院的播放场次列表 通过关联changci表得到
    
    private Position position;
    private POI poi;
    private int changciOption=0;
    private int changciToday=0;
    private int changciTomorrow=0;
    private int changciAfterTomorrow=0;
    
    public Yingxun() {
    }

    public Yingxun (XMap data) throws APIException {
        super(data);
        init(data);
    }
    
    @SuppressWarnings("unchecked")
    public void init(XMap data) throws APIException {
        super.init(data);
        if (this.data.containsKey(FIELD_UID)) {
            this.uid = this.data.getString(FIELD_UID);
        }
        if (this.data.containsKey(FIELD_CITY_ID)) {
            this.cityId = this.data.getString(FIELD_CITY_ID);
        }
        if (this.data.containsKey(FIELD_LINK_UID)) {
            this.linkUid = this.data.getString(FIELD_LINK_UID);
        }
        if (this.data.containsKey(FIELD_CITY_ID)) {
            this.cityId = this.data.getString(FIELD_CITY_ID);
        }
        if (this.data.containsKey(FIELD_MOVIE_UID)) {
            this.movieUid = this.data.getString(FIELD_MOVIE_UID);
        }
        if (this.data.containsKey(FIELD_ADMINNAME)) {
            this.adminname = this.data.getString(FIELD_ADMINNAME);
        }
        if (this.data.containsKey(FIELD_AREANAME)) {
            this.areaname = this.data.getString(FIELD_AREANAME);
        }
        if (this.data.containsKey(FIELD_X) && this.data.containsKey(FIELD_Y)) {
            this.x = this.data.getInt(FIELD_X);
            this.y = this.data.getInt(FIELD_Y);
            this.position = new Position(((double)this.y)/TKConfig.LON_LAT_DIVISOR, ((double)this.x)/TKConfig.LON_LAT_DIVISOR);
        }
        if (this.data.containsKey(FIELD_NAME)) {
            this.name = this.data.getString(FIELD_NAME);
        }
        if (this.data.containsKey(FIELD_ADDRESS)) {
            this.address = this.data.getString(FIELD_ADDRESS);
        }
        if (this.data.containsKey(FIELD_PHONE)) {
            this.phone = this.data.getString(FIELD_PHONE);
        }
        if (this.data.containsKey(FIELD_DISTANCE)) {
            this.distance = this.data.getString(FIELD_DISTANCE);
        }
        if (this.data.containsKey(FIELD_CHANGCI_LIST)) {
            changciList.clear();
            XArray<XMap> xArray = this.data.getXArray(FIELD_CHANGCI_LIST);
            for (int i = 0, size = xArray.size(); i < size; i++) {
                Changci changci = new Changci(xArray.get(i));
                changciList.add(changci);
            }
        }
        
        for(Changci changci : changciList) {
            long option = changci.getOption();
            if ((option & Changci.OPTION_DAY_TODAY) > 0) {
                changciToday++;
            }
            if ((option & Changci.OPTION_DAY_TOMORROW) > 0) {
                changciTomorrow++;
            }
            if ((option & Changci.OPTION_DAY_AFTER_TOMORROW) > 0) {
                changciAfterTomorrow++;
            }
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Yingxun) {
            Yingxun other = (Yingxun) object;
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
            poi.setName(name);
            poi.setAddress(address);
            poi.setPosition(position);
        }
        return poi;
    }
    
    public static class Changci extends BaseData implements Parcelable{

        public static final String NEED_FILELD = "40414243";
        
        // 0x40 x_string uid uid
        public static final byte FIELD_UID = 0x40;

        // 0x41 x_string 关联影讯id yingx_uid
        public static final byte FIELD_YINGX_UID = 0x41;

        // 0x42 x_string 放映时间 start_time
        public static final byte FIELD_START_TIME = 0x42;

        // 0x43 x_string 版本 version
        public static final byte FIELD_VERSION = 0x43;

        // 0x44 x_int byte[0]中：
        // bit[0]==1表示是今天，
        // bit[1]==1表示是明天，
        // bit[2]==1表示是后天；
        // byte[1]中：
        // bit[0]==1表示命中筛选项 
        // 实时生成
        public static final byte FIELD_OPTION = 0x44;
        
        public static final int OPTION_DAY_TODAY = 1;
        public static final int OPTION_DAY_TOMORROW = 2;
        public static final int OPTION_DAY_AFTER_TOMORROW = 4;
        public static final int OPTION_FILTER = 256;

        private String uid; // 0x40 x_string uid uid
        private String yingxUid; // 0x41 x_string 关联影讯id yingx_uid
        private String startTime; // 0x42 x_string 放映时间 start_time
        private String version; // 0x43 x_string 版本 version
        private long option; // 0x44 x_int byte[0];
        
        public Changci() {
        }

        public Changci (XMap data) throws APIException {
            super(data);
            init(data);
        }
        
        public void init(XMap data) throws APIException {
            super.init(data);
            if (this.data.containsKey(FIELD_UID)) {
                this.uid = this.data.getString(FIELD_UID);
            }
            if (this.data.containsKey(FIELD_YINGX_UID)) {
                this.yingxUid = this.data.getString(FIELD_YINGX_UID);
            }
            if (this.data.containsKey(FIELD_START_TIME)) {
                this.startTime = this.data.getString(FIELD_START_TIME);
            }
            if (this.data.containsKey(FIELD_VERSION)) {
                this.version = this.data.getString(FIELD_VERSION);
            }
            if (this.data.containsKey(FIELD_OPTION)) {
                this.option = this.data.getInt(FIELD_OPTION);
            }
        }
        
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            
            if (object instanceof Changci) {
                Changci other = (Changci) object;
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

        public String getYingxUid() {
            return yingxUid;
        }

        public void setYingxUid(String yingxUid) {
            this.yingxUid = yingxUid;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
        
        public long getOption() {
            return option;
        }
        
        public Changci(Parcel in){
			uid = in.readString();
			yingxUid = in.readString();
			startTime = in.readString();
			version = in.readString();
			option = in.readLong();
        }

        public static final Parcelable.Creator<Changci> CREATOR
    		    = new Parcelable.Creator<Changci>() {
    		public Changci createFromParcel(Parcel in) {
    		    return new Changci(in);
    		}
    		
    		public Changci[] newArray(int size) {
    		    return new Changci[size];
    		}
    	};

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(uid);
			dest.writeString(yingxUid);
			dest.writeString(startTime);
			dest.writeString(version);
			dest.writeLong(option);
		}
		
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getLinkUid() {
        return linkUid;
    }

    public void setLinkUid(String linkUid) {
        this.linkUid = linkUid;
    }

    public String getMovieUid() {
        return movieUid;
    }

    public void setMovieUid(String movieUid) {
        this.movieUid = movieUid;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public List<Changci> getChangciList() {
        return changciList;
    }

    public void setChangciList(List<Changci> changciList) {
        this.changciList = changciList;
    }
    
    public Position getPosition() {
        return position;
    }

    public int getChangciOption() {
        return changciOption;
    }

    public void setChangciOption(int changciOption) {
        this.changciOption = changciOption;
    }

    public int getChangciToday() {
        return changciToday;
    }

    public int getChangciTomorrow() {
        return changciTomorrow;
    }

    public int getChangciAfterTomorrow() {
        return changciAfterTomorrow;
    }

    public Yingxun(Parcel in){
		uid = in.readString();
		cityId = in.readString();
		linkUid = in.readString();
		movieUid = in.readString();
		adminname = in.readString();
		areaname = in.readString();
		x = in.readLong();
		y = in.readLong();
		name = in.readString();
		address = in.readString();
		phone = in.readString();
		distance = in.readString();
		changciList = new ArrayList<Changci>();
		in.readList(changciList, null);
		position = in.readParcelable(null);
		changciOption = in.readInt();
		changciToday = in.readInt();
		changciTomorrow = in.readInt();
		changciAfterTomorrow = in.readInt();
		
    }

    public static final Parcelable.Creator<Yingxun> CREATOR
		    = new Parcelable.Creator<Yingxun>() {
		public Yingxun createFromParcel(Parcel in) {
		    return new Yingxun(in);
		}
		
		public Yingxun[] newArray(int size) {
		    return new Yingxun[size];
		}
	};
    
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(uid);
		dest.writeString(cityId);
		dest.writeString(linkUid);
		dest.writeString(movieUid);
		dest.writeString(adminname);
		dest.writeString(areaname);
		dest.writeLong(x);
		dest.writeLong(y);
		dest.writeString(name);
		dest.writeString(address);
		dest.writeString(phone);
		dest.writeString(distance);
		dest.writeList(changciList);
		dest.writeParcelable(position, flags);
		dest.writeInt(changciOption);
		dest.writeInt(changciToday);
		dest.writeInt(changciTomorrow);
		dest.writeInt(changciAfterTomorrow);
		
		//!!! Here poi is ignored!
	}

	@Override
	public String toString() {
		return "Yingxun [uid=" + uid + ", cityId=" + cityId + ", linkUid="
				+ linkUid + ", movieUid=" + movieUid + ", adminname="
				+ adminname + ", areaname=" + areaname + ", x=" + x + ", y="
				+ y + ", name=" + name + ", address=" + address + ", phone="
				+ phone + ", distance=" + distance + ", changciList="
				+ changciList + ", position=" + position + ", poi=" + poi
				+ ", changciOption=" + changciOption + ", changciToday="
				+ changciToday + ", changciTomorrow=" + changciTomorrow
				+ ", changciAfterTomorrow=" + changciAfterTomorrow + "]";
	}
    
}

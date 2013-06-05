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
import com.tigerknows.model.xobject.XMap;

public class Dianying extends BaseData implements Parcelable{

    public static final String NEED_FIELD = "000102030414060708090a0b0c0d" +
    		"20262728292a2b2c";

    public static final String NEED_FIELD_ONLY_DIANYING = "000102030414060708090a0b0c0d";
    
    public static final String NEED_FIELD_POI_DETAIL = "00010304070920";
    
    // 0x00 x_string UUID uid
    public static final byte FIELD_UID = 0x00;

    // 0x01 x_string 电影名称 name
    public static final byte FIELD_NAME = 0x01;

    // 0x02 x_string 电影别名（如英文电影的中文名） alias
    public static final byte FIELD_ALIAS = 0x02;

    // 0x03 x_string 电影类型 tag
    public static final byte FIELD_TAG = 0x03;

    // 0x04 x_string 电影海报 pictures
    public static final byte FIELD_PICTURES = 0x04;

    // 0x14 x_map   详情页电影海报 pictures 
    public static final byte FIELD_PICTURES_DETAIL = 0x14;
    
    // 0x05 x_string 简介 description
    public static final byte FIELD_DESCRIPTION = 0x05;

    // 0x06 x_string 首映时间 start_time
    public static final byte FIELD_START_TIME = 0x06;

    // 0x07 x_string 电影时长 length
    public static final byte FIELD_LENGTH = 0x07;

    // 0x08 x_string 语言 language
    public static final byte FIELD_LANGUAGE = 0x08;

    // 0x09 x_int 电影评分 rank
    public static final byte FIELD_RANK = 0x09;

    // 0x0a x_string 国家 country
    public static final byte FIELD_COUNTRY = 0x0a;

    // 0x0b x_string 导演 director
    public static final byte FIELD_DIRECTOR = 0x0b;

    // 0x0c x_string 主演 main_actor
    public static final byte FIELD_MAIN_ACTOR = 0x0c;

    // 0x0d x_int 满足筛选条件的影院的数量    实时生成
    public static final byte FIELD_NUM = 0x0d;

    private String uid; // 0x00 x_string UUID uid
    private String name; // 0x01 x_string 电影名称 name
    private String alias; // 0x02 x_string 电影别名（如英文电影的中文名） alias
    private String tag; // 0x03 x_string 电影类型 tag
    private TKDrawable pictures; // 0x04 x_string 电影海报 pictures
    private TKDrawable picturesDetail; 
    private String description; // 0x05 x_string 简介 description
    private String startTime; // 0x06 x_string 首映时间 start_time
    private String length; // 0x07 x_string 电影时长 length
    private String language; // 0x08 x_string 语言 language
    private long rank; // 0x09 x_int 电影评分 rank
    private String country; // 0x0a x_string 国家 country
    private String director; // 0x0b x_string 导演 director
    private String mainActor; // 0x0c x_string 主演 main_actor
    private long num; // 0x0d x_int 满足筛选条件的影院的数量    实时生成
    private Yingxun yingxun;
    
    private DataQuery yingxunQuery;
    private String filterArea;
    
    public Dianying() {
    }

    public Dianying (XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.uid = getStringFromData(FIELD_UID, reset ? null : this.uid);
        this.name = getStringFromData(FIELD_NAME, reset ? null : this.name);
        this.alias = getStringFromData(FIELD_ALIAS, reset ? null : this.alias);
        this.tag = getStringFromData(FIELD_TAG, reset ? null : this.tag);
        this.pictures = getObjectFromData(FIELD_PICTURES, TKDrawable.Initializer, reset ? null : this.pictures);
        this.picturesDetail = getObjectFromData(FIELD_PICTURES_DETAIL, TKDrawable.Initializer, reset ? null : this.picturesDetail);
        this.description = getStringFromData(FIELD_DESCRIPTION, reset ? null : this.description);
        this.startTime = getStringFromData(FIELD_START_TIME, reset ? null : this.startTime);
        this.length = getStringFromData(FIELD_LENGTH, reset ? null : this.length);
        this.language = getStringFromData(FIELD_LANGUAGE, reset ? null : this.language);
        this.rank = getLongFromData(FIELD_RANK, reset ? 0 : this.rank);
        this.country = getStringFromData(FIELD_COUNTRY, reset ? null : this.country);
        this.director = getStringFromData(FIELD_DIRECTOR, reset ? null : this.director);
        this.mainActor = getStringFromData(FIELD_MAIN_ACTOR, reset ? null : this.mainActor);
        this.num = getLongFromData(FIELD_NUM, reset ? 0 : this.num);
        if (yingxun == null) {
            yingxun = new Yingxun(this.data);
        } else {
            yingxun.init(this.data, reset);
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Dianying) {
            Dianying other = (Dianying) object;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getMainActor() {
        return mainActor;
    }

    public void setMainActor(String mainActor) {
        this.mainActor = mainActor;
    }

    public long getNum() {
        return num;
    }

    public void setNum(long num) {
        this.num = num;
    }

    public Yingxun getYingxun() {
        return yingxun;
    }

    public DataQuery getYingxunQuery() {
        return yingxunQuery;
    }

    public void setYingxun(Yingxun yingxun) {
		this.yingxun = yingxun;
	}

	public void setYingxunQuery(DataQuery yingxunQuery) {
        this.yingxunQuery = yingxunQuery;
    }

    public String getFilterArea() {
        return filterArea;
    }

    public void setFilterArea(String filterArea) {
        this.filterArea = filterArea;
    }

	@Override
	public POI getPOI() {
		return yingxun.getPOI(POI.SOURCE_TYPE_DIANYING);
	}

	public Dianying(Parcel in){
		uid = in.readString();
		name = in.readString();
		alias = in.readString();
		tag = in.readString();
		pictures = in.readParcelable(TKDrawable.class.getClassLoader());
		picturesDetail = in.readParcelable(TKDrawable.class.getClassLoader());
		description = in.readString();
		startTime = in.readString();
		length = in.readString();
		language = in.readString();
		rank = in.readLong();
		country = in.readString();
		director = in.readString();
		mainActor = in.readString();
		num = in.readLong();
		yingxun = in.readParcelable(Yingxun.class.getClassLoader());
		filterArea = in.readString();
	}

    public static final Parcelable.Creator<Dianying> CREATOR
            = new Parcelable.Creator<Dianying>() {
        public Dianying createFromParcel(Parcel in) {
            return new Dianying(in);
        }

        public Dianying[] newArray(int size) {
            return new Dianying[size];
        }
    };
    
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(uid);
		dest.writeString(name);
		dest.writeString(alias);
		dest.writeString(tag);
		dest.writeParcelable(pictures, flags);
		dest.writeParcelable(picturesDetail, flags);
		dest.writeString(description);
		dest.writeString(startTime);
		dest.writeString(length);
		dest.writeString(language);
		dest.writeLong(rank);
		dest.writeString(country);
		dest.writeString(director);
		dest.writeString(mainActor);
		dest.writeLong(num);
		dest.writeParcelable(yingxun, flags);
		dest.writeString(filterArea);
		
		//!!! here data query is ignored!!
		
	}

    public static XMapInitializer<Dianying> Initializer = new XMapInitializer<Dianying>() {

        @Override
        public Dianying init(XMap data) throws APIException {
            return new Dianying(data);
        }
    };
	
}

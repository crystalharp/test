package com.tigerknows.map;

import com.tigerknows.android.location.Position;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class CityInfo implements Parcelable {
    
    public static final int CITY_ID_INVALID = -1;
    public static final int CITY_ID_QUANGUO = -3;
    public static final int CITY_ID_BEIJING = 1;
    
    // "城市中文名字, City english name, latitude, longitude, level, 省份中文名字, city
    // Province english name" such as
    // "北京,beijing,39.90415599,116.397772995,11, 北京,beijing"
    public static int TYPE_CITY = 0;
    public static int TYPE_PROVINCE = 1;
    
    private int id = CITY_ID_INVALID;
    private int type = TYPE_CITY;
    private List<CityInfo> cityInfoList = new ArrayList<CityInfo>();
    
    private String cName;
    private String eName;
    private Position position;
    private int level;
    private String cProvinceName;
    private String eProvinceName;
    public int order = 0;
    
    public CityInfo() {
    }
    
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    
    public List<CityInfo> getCityList() {
        return cityInfoList;
    }
    
    public String getCName() {
        return cName;
    }
    public void setCName(String cName) {
        this.cName = cName;
    }
    public String getEName() {
        return eName;
    }
    public void setEName(String eName) {
        this.eName = eName;
    }
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }
    public String getCProvinceName() {
        return cProvinceName;
    }
    public void setCProvinceName(String cProvinceName) {
        this.cProvinceName = cProvinceName;
    }
    public String getEProvinceName() {
        return eProvinceName;
    }
    public void setEProvinceName(String eProvinceName) {
        this.eProvinceName = eProvinceName;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    
    public Position getPosition() {
        return position;
    }
    
    @Override
    public String toString() {
        return "CityInfo[id:=" + id +", cName: " + cName + ", eName: " + eName + ", level: " + level + ", cProvinceName: "
                + cProvinceName + ", eProvinceName: " + eProvinceName  + ", position: " + position +"]";
    }
    
    public CityInfo clone() {
        CityInfo cityInfo = new CityInfo();
        cityInfo.id = id;
        cityInfo.cName = cName;
        cityInfo.cProvinceName = cProvinceName;
        cityInfo.eName = eName;
        cityInfo.eProvinceName = eProvinceName;
        cityInfo.type = type;
        if (position != null) {
            cityInfo.position = position.clone();
        } else {
            cityInfo.position = null;
        }
        cityInfo.level = level;
        List<CityInfo> cityInfoList1 = new ArrayList<CityInfo>();
        for(CityInfo cityInfo1 : cityInfoList) {
            cityInfoList1.add(cityInfo1.clone());
        }
        cityInfo.cityInfoList = cityInfoList1;
        return cityInfo;
    }
    
    public boolean isAvailably() {
        return id != CITY_ID_INVALID && !TextUtils.isEmpty(cName);
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof CityInfo) {
            CityInfo other = (CityInfo) object;
            if (this.id != other.id) {
                return false;
//            } else if((null != this.cName && !this.cName.equals(other.cName)) || (null == this.cName && this.cName != other.cName)) {
//                return false;
//            } else if((null != this.cProvinceName && !this.cProvinceName.equals(other.cProvinceName)) || (null == this.cProvinceName && this.cProvinceName != other.cProvinceName)) {
//                return false;
//            } else if((null != this.eName && !this.eName.equals(other)) || (null == this.eName && this.eName != other.eName)) {
//                return false;
//            } else if((null != this.eProvinceName && !this.eProvinceName.equals(other.eProvinceName)) || (null == this.eProvinceName && this.eProvinceName != other.eProvinceName)) {
//                return false;
            } else if (this.type != other.type) {
                return false;
//            } else if((null != this.position && !this.position.equals(other.position)) || (null == this.position && this.position != other.position)) {
//                return false;
//            } else if (this.level != other.level) {
//                return false;
//            } else {
//                if (cityInfoList.size() != other.cityInfoList.size()) {
//                    return false;
//                } else {
//                    int i = 0;
//                    for(CityInfo cityInfo : cityInfoList) {
//                        if (!cityInfo.equals(other.cityInfoList.get(i++))) {
//                            return false;
//                        }
//                    }
//                }
            }
        } else {
            return false;
        }
        
        return true;
    }
    
    private volatile int hashCode = 0;
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int hash = 29 * id;
            if (cName != null) {
                hash += cName.hashCode();
            }
//            if (cProvinceName != null) {
//                hashCode += cProvinceName.hashCode();
//            }
//            if (eName != null) {
//                hashCode += eName.hashCode();
//            }
//            if (eProvinceName != null) {
//                hashCode += eProvinceName.hashCode();
//            }
//            hashCode += type * id;
//            hashCode += level * id;
//            hashCode += position.hashCode();
//            for(int i = cityInfoList.size()-1; i >= 0; i--) {
//                CityInfo cityInfo = cityInfoList.get(i);
//                hashCode += cityInfo.hashCode();
//            }
            hashCode = hash;
        }
        return hashCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(id);
        parcel.writeInt(type);
        parcel.writeString(cName);
        parcel.writeString(eName);
        parcel.writeInt(level);
        parcel.writeString(cProvinceName);
        parcel.writeString(eProvinceName);
        parcel.writeInt(order);    
        double[] pos = new double[2];
        if (position != null) {
            pos[0] = position.getLat();
            pos[1] = position.getLon();
        }
        parcel.writeDoubleArray(pos);
    }

    public static final Parcelable.Creator<CityInfo> CREATOR
            = new Parcelable.Creator<CityInfo>() {
        public CityInfo createFromParcel(Parcel in) {
            return new CityInfo(in);
        }

        public CityInfo[] newArray(int size) {
            return new CityInfo[size];
        }
    };
    
    private CityInfo(Parcel in) {
        id = in.readInt();
        type = in.readInt();
        cName = in.readString();
        eName = in.readString();
        level = in.readInt();
        cProvinceName = in.readString();
        eProvinceName = in.readString();
        order = in.readInt();
        double[] pos = new double[2];
        in.readDoubleArray(pos);
        position = new Position(pos[0], pos[1]);
    }
}
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
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.DataQuery.HotelVendorResponse;
import com.tigerknows.model.DataQuery.HotelVendorResponse.HotelVendorList;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HotelVendor extends BaseData implements Parcelable {
    
    public static final String NEED_FIELD = "01020304";
    
    public static final String ALL = "all";
    
    // 测试 1999
    public static final int SOURCE_TEST = 1999;
    
    // 艺龙
    public static final int SOURCE_ELONG = 2000;
    
    // TODO: 携程
    public static final int SOURCE_CTRIP = 2001;
    
    public static final List<HotelVendor> hotelVendorList = new ArrayList<HotelVendor>();
    
    // 0x01 x_int    酒店商家ID
    public static final byte FIELD_ID = 0x01;
    
    // 0x02 x_string    商家名称
    public static final byte FIELD_NAME = 0x02;
    
    // 0x03 x_string    客服电话
    public static final byte FIELD_SERVICE_TEL = 0x03;

    // 0x04 x_string    预订电话，若商家（如: 艺龙）没有提供预订电话，则此字段为空字符串(长度为0的字符串） 
    public static final byte FIELD_RESERVE_TEL = 0x04;

    private long id;
    private String name;
    private String serviceTel;
    private String reserveTel;
    
    private HotelVendor() {
        
    }

    public HotelVendor (XMap data) throws APIException {
        super(data);
        init(data, true);
        
        synchronized (hotelVendorList) {

            if (this.data.containsKey(FIELD_ID)) {
                for(int i = hotelVendorList.size()-1; i >= 0; i--) {
                    if (hotelVendorList.get(i).id == id) {
                    	hotelVendorList.remove(i);
                    }
                }
                hotelVendorList.add(this);
            }
        }
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.id = getLongFromData(FIELD_ID, reset ? 0 : this.id);
        this.name = getStringFromData(FIELD_NAME, reset ? null : this.name);
        this.serviceTel = getStringFromData(FIELD_SERVICE_TEL, reset ? null : this.serviceTel);
        this.reserveTel = getStringFromData(FIELD_RESERVE_TEL, reset ? null : this.reserveTel);
    }

    public static final Parcelable.Creator<HotelVendor> CREATOR
            = new Parcelable.Creator<HotelVendor>() {
        public HotelVendor createFromParcel(Parcel in) {
            return new HotelVendor(in);
        }

        public HotelVendor[] newArray(int size) {
            return new HotelVendor[size];
        }
    };

    
    private HotelVendor(Parcel in) {
        id = in.readLong();
        serviceTel = in.readString();
        name = in.readString();
        reserveTel = in.readString();
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeString(serviceTel);
        out.writeString(reserveTel);
    }
    
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof HotelVendor) {
        	HotelVendor other = (HotelVendor) object;
            if (other.id == this.id) {
                return true;
            }
        }
        
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public static void readHotelVendorList(Context context) {
        synchronized (hotelVendorList) {
        	hotelVendorList.clear();
            sLoad.clear();
            String path = TKConfig.getDataPath(false) + "HotelVendorList";
            File file = new File(path);
            if (file.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    XArray<XMap> xarray = (XArray<XMap>)ByteUtil.byteToXObject(Utility.readFileToByte(fis));
                    for(int i = 0, size = xarray.size(); i < size; i++) {
                    	HotelVendor hotelVendor = new HotelVendor();
                    	hotelVendor.init(xarray.get(i), true);
                    	hotelVendorList.add(hotelVendor);
                    }
                } catch (Exception e) {
                    file.delete();
                    e.printStackTrace();
                } finally {
                    if (null != fis) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            // Ignore
                            LogWrapper.e("HotelVendor", "readHotelVendorList() IOException caught while closing stream");
                        }
                    }
                }
            } else {

                try {
                    XMap xmap = new XMap();
                    xmap.put(FIELD_ID, SOURCE_ELONG);
                    xmap.put(FIELD_SERVICE_TEL, "4009333333");
                    xmap.put(FIELD_NAME, context.getString(R.string.elong));
                    xmap.put(FIELD_RESERVE_TEL, "");
                    
                    HotelVendor hotelVendor = new HotelVendor();
                    hotelVendor.init(xmap, true);
                    hotelVendorList.add(hotelVendor);
                    
                    xmap = new XMap();
                    xmap.put(FIELD_ID, SOURCE_CTRIP);
                    xmap.put(FIELD_SERVICE_TEL, "4009210661");
                    xmap.put(FIELD_NAME, context.getString(R.string.ctrip));
                    xmap.put(FIELD_RESERVE_TEL, "");
                    
                    hotelVendor = new HotelVendor();
                    hotelVendor.init(xmap, true);
                    hotelVendorList.add(hotelVendor);
                    
                } catch (APIException e) {
                    e.printStackTrace();
                }
            }
            
            DataQuery dataQuery = new DataQuery(context);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_HOTELVENDOR);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, NEED_FIELD);
            dataQuery.setup(Globals.getCurrentCityInfo().getId(), -1, -1, null);
            dataQuery.query();
            
            Response response = dataQuery.getResponse();
            if (response != null) {
            	HotelVendorResponse hotelvendorResponse = (HotelVendorResponse)response;
                HotelVendorList hotelVendorList = hotelvendorResponse.getList();
                if (hotelVendorList != null) {
                    List<HotelVendor> hotelVendorArrayList = hotelVendorList.getList();
                    List<HotelVendor> oldHotelVendorList = new ArrayList<HotelVendor>();
                    oldHotelVendorList.addAll(HotelVendor.hotelVendorList);
                    
                    HotelVendor.hotelVendorList.clear();
                    if (hotelVendorArrayList != null && hotelVendorArrayList.size() > 0) {
                        
                    	HotelVendor.hotelVendorList.addAll(hotelVendorArrayList);
                    }
                    writeHotelVendorList();
                }
            }
        }
    }
    
    private static void writeHotelVendorList() {
        synchronized (hotelVendorList) {
            try {
                XArray<XMap> xarray = new XArray<XMap>();
                for(HotelVendor hotelVendor : hotelVendorList) {
                    xarray.add(hotelVendor.data);
                }
                String path = TKConfig.getDataPath(false) + "HotelVendorList";
                byte[] data = ByteUtil.xobjectToByte(xarray);
                Utility.writeFile(path, data, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    private static List<Long> sLoad = new ArrayList<Long>();
    
    private static void loadHotelVendor(final Activity activity, long id, final Runnable runnable) {
        synchronized (hotelVendorList) {
            if (sLoad.contains(id) == false && activity != null) {
                sLoad.add(id);
                final DataQuery dataQuery = new DataQuery(activity);
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_HOTELVENDOR);
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, NEED_FIELD);
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_IDS, String.valueOf(id));
                dataQuery.setup(Globals.getCurrentCityInfo().getId(), -1, -1, null, false, false, null);
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        dataQuery.query();
                        activity.runOnUiThread(runnable);
                    }
                }).start();
            }
        }
    }
    
    public static HotelVendor getHotelVendorById(long id, Activity activity, Runnable runnable) {
        synchronized (hotelVendorList) {
            for(HotelVendor hotelVendor : hotelVendorList) {
                if (hotelVendor.id == id) {
                    return hotelVendor;
                }
            }
        }
        loadHotelVendor(activity, id, runnable);
        return null;
    }
    
    public static List<HotelVendor> getHotelVendorList() {
        return hotelVendorList;
    }

    public long getId() {
        return id;
    }

    public String getServiceTel() {
        return serviceTel;
    }

    public String getName() {
        return name;
    }
    
    public String getReserveTel() {
        return reserveTel;
    }

    public static XMapInitializer<HotelVendor> Initializer = new XMapInitializer<HotelVendor>() {

        @Override
        public HotelVendor init(XMap data) throws APIException {
            return new HotelVendor(data);
        }
    };
}

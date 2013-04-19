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
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Shangjia extends BaseData implements Parcelable {
    
    public static final String NEED_FILELD = "1f707172737475";
    
    public static final String NEED_FILELD_NO_LOGON = "1f707175";
    
    // 测试 999
    public static final int SOURCE_TEST = 999;
    
    // 55团 1000
    public static final int SOURCE_WOWOTUAN = 1000;
    
    // 糯米 1001
    public static final int SOURCE_NUOMI = 1001;
    
    public static final List<Shangjia> shangjiaList = new ArrayList<Shangjia>();
    
    // 0x1f x_string    合作商数据源  source 
    public static final byte FIELD_SOURCE = 0x1f;
    
    // 0x70 x_string    合作商联系电话 service_tel
    public static final byte FIELD_SERVICE_TEL = 0x70;
    
    // 0x71 x_binary_data   用于团购列表页和详情页的合作商logo，三角形 资源文件 
    public static final byte FIELD_MARKER = 0x71;

    // 0x72 x_string    合作商查询第三方订单的网址   url 
    public static final byte FIELD_URL = 0x72;

    // 0x73 x_string    合作商查询第三方订单的提示语  message 
    public static final byte FIELD_MESSAGE = 0x73;

    // 0x74 x_binary_data   用于订单页的合作商logo，长方形   资源文件 
    public static final byte FIELD_LOGO = 0x74;
    
    // 0x75 x_string    合作商名字   糯米、窝窝等 
    public static final byte FIELD_NAME = 0x75;

    private long source;
    private String serviceTel;
    private Drawable marker;
    private String url;
    private String message;
    private Drawable logo;
    private String name;
    
    private Shangjia() {
        
    }

    public Shangjia (XMap data) throws APIException {
        super(data);
        init(data);
        
        synchronized (shangjiaList) {

            if (this.data.containsKey(FIELD_SOURCE)) {
                for(int i = shangjiaList.size()-1; i >= 0; i--) {
                    if (shangjiaList.get(i).source == source) {
                        shangjiaList.remove(i);
                    }
                }
                shangjiaList.add(this);
            }
        }
    }
    
    public void init(XMap data) throws APIException {
        super.init(data);
        if (this.data.containsKey(FIELD_SOURCE)) {
            this.source = this.data.getInt(FIELD_SOURCE);
        }
        if (this.data.containsKey(FIELD_SERVICE_TEL)) {
            this.serviceTel = this.data.getString(FIELD_SERVICE_TEL);
        }
        if (this.data.containsKey(FIELD_MARKER)) {
            byte[] bitmap = this.data.getBytes(FIELD_MARKER);
            this.marker = new BitmapDrawable(BitmapFactory.decodeByteArray(bitmap, 0, bitmap.length));
        }
        if (this.data.containsKey(FIELD_URL)) {
            this.url = this.data.getString(FIELD_URL);
        }
        if (this.data.containsKey(FIELD_MESSAGE)) {
            this.message = this.data.getString(FIELD_MESSAGE);
        }
        if (this.data.containsKey(FIELD_LOGO)) {
            byte[] bitmap = this.data.getBytes(FIELD_LOGO);
            this.logo = new BitmapDrawable(BitmapFactory.decodeByteArray(bitmap, 0, bitmap.length));
        }
        if (this.data.containsKey(FIELD_NAME)) {
            this.name = this.data.getString(FIELD_NAME);
        }
    }

    public static final Parcelable.Creator<Shangjia> CREATOR
            = new Parcelable.Creator<Shangjia>() {
        public Shangjia createFromParcel(Parcel in) {
            return new Shangjia(in);
        }

        public Shangjia[] newArray(int size) {
            return new Shangjia[size];
        }
    };

    
    private Shangjia(Parcel in) {
        source = in.readLong();
        serviceTel = in.readString();
        Bitmap bt = null;
        BitmapDrawable bd = null;
        bt = (Bitmap)in.readParcelable(Bitmap.class.getClassLoader());
        if (bt != null) {
            bd= new BitmapDrawable(bt);
            marker = bd;
        }
        url = in.readString();
        message = in.readString();
        bt = (Bitmap)in.readParcelable(Bitmap.class.getClassLoader());
        if (bt != null) {
            bd= new BitmapDrawable(bt);
            logo = bd;
        }
        name = in.readString();
    }
    
    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(source);
        out.writeString(serviceTel);
        if (marker != null)
            out.writeParcelable(((BitmapDrawable)marker).getBitmap(), flags);
        else
            out.writeParcelable(null, flags);
        out.writeString(url);
        out.writeString(message);
        if (logo != null)
            out.writeParcelable(((BitmapDrawable)logo).getBitmap(), flags);
        else
            out.writeParcelable(null, flags);
        out.writeString(name);
    }
    
    @SuppressWarnings("unchecked")
    public static void readShangjiaList(Sphinx sphinx) {
        synchronized (shangjiaList) {
            if (shangjiaList.size() > 0) {
                return;
            }
            sLoad.clear();
            String path = TKConfig.getDataPath(false) + "ShangjiaList";
            File file = new File(path);
            if (file.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    XArray<XMap> xarray = (XArray<XMap>)ByteUtil.byteToXObject(Utility.readFileToByte(fis));
                    for(int i = 0, size = xarray.size(); i < size; i++) {
                        Shangjia shangjia = new Shangjia();
                        shangjia.init(xarray.get(i));
                        shangjiaList.add(shangjia);
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
                            LogWrapper.e("Shangjia", "readShangjiaList() IOException caught while closing stream");
                        }
                    }
                }
            }

            try {

                boolean exist = false;
                for(Shangjia shangjia : shangjiaList) {
                    if (shangjia.source == SOURCE_WOWOTUAN) {
                        exist = true;
                        break;
                    }
                }
                if (exist == false) {
                    XMap xmap = new XMap();
                    xmap.put(FIELD_SOURCE, SOURCE_WOWOTUAN);
                    xmap.put(FIELD_SERVICE_TEL, "4000155555");
                    xmap.put(FIELD_MARKER, Utility.getDrawableResource(sphinx, R.drawable.ic_wowotuan_marker));
                    xmap.put(FIELD_NAME, sphinx.getString(R.string.wowotuan_name));
                    
                    Shangjia shangjia = new Shangjia();
                    shangjia.init(xmap);
                    shangjiaList.add(shangjia);
                }
                
                exist = false;
                for(Shangjia shangjia : shangjiaList) {
                    if (shangjia.source == SOURCE_NUOMI) {
                        exist = true;
                        break;
                    }
                }
                if (exist == false) {
                    XMap xmap = new XMap();
                    xmap.put(FIELD_SOURCE, SOURCE_NUOMI);
                    xmap.put(FIELD_SERVICE_TEL, "4006888887");
                    xmap.put(FIELD_MARKER, Utility.getDrawableResource(sphinx, R.drawable.ic_nuomi_marker));
                    xmap.put(FIELD_NAME, sphinx.getString(R.string.nuomi_name));
                    
                    Shangjia shangjia = new Shangjia();
                    shangjia.init(xmap);
                    shangjiaList.add(shangjia);
                }
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public static void writeShangjiaList() {
        synchronized (shangjiaList) {
            try {
                XArray<XMap> xarray = new XArray<XMap>();
                for(Shangjia shangjia : shangjiaList) {
                    shangjia.data.remove(FIELD_URL);
                    shangjia.data.remove(FIELD_MESSAGE);
                    shangjia.data.remove(FIELD_LOGO);
                    xarray.add(shangjia.data);
                }
                String path = TKConfig.getDataPath(false) + "ShangjiaList";
                byte[] data = ByteUtil.xobjectToByte(xarray);
                Utility.writeFile(path, data, true);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    
    private static List<Long> sLoad = new ArrayList<Long>();
    
    private static void loadShangjia(final Activity activity, long source, final Runnable runnable) {
        synchronized (shangjiaList) {
            if (sLoad.contains(source) == false && activity != null) {
                sLoad.add(source);
                final DataQuery dataQuery = new DataQuery(activity);
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_SHANGJIA);
                criteria.put(DataQuery.SERVER_PARAMETER_SHANGJIA_IDS, String.valueOf(source));
                dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(), -1, -1, null, false, true, null);
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
    
    public static Shangjia getShangjiaById(long source, Activity activity, Runnable runnable) {
        synchronized (shangjiaList) {
            for(Shangjia shangjia : shangjiaList) {
                if (shangjia.source == source) {
                    return shangjia;
                }
            }
        }
        loadShangjia(activity, source, runnable);
        return null;
    }

    public long getSource() {
        return source;
    }

    public String getServiceTel() {
        return serviceTel;
    }

    public Drawable getMarker() {
        return marker;
    }

    public String getUrl() {
        return url;
    }
    
    public String getMessage() {
        return message;
    }

    public Drawable getLogo() {
        return logo;
    }
    
    public String getName() {
        return name;
    }
}

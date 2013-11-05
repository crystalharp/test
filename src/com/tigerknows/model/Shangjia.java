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
import com.tigerknows.model.DataQuery.ShangjiaResponse;
import com.tigerknows.model.DataQuery.ShangjiaResponse.ShangjiaList;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.content.Context;
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
import java.util.List;

public class Shangjia extends BaseData implements Parcelable {
    
    public static final String NEED_FIELD = "1f7071727374757677";
    
    public static final String NEED_FIELD_NO_LOGON = "1f70717374757677";
    
    // 测试 999
    public static final int SOURCE_TEST = 999;
    
    // 55团 1000
    public static final int SOURCE_WOWOTUAN = 1000;
    
    // 糯米 1001
    public static final int SOURCE_NUOMI = 1001;
    
    
    public static final int SOURCE_MEITUAN = 1002;
    
    public static final List<Shangjia> shangjiaList = new ArrayList<Shangjia>();
    
    static final Object sWriteLock = new Object();
    
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
    
    // 0x76     x_string    退款说明    refund_service 
    public static final byte FIELD_REFUND_SERVICE = 0x76;
    
    // 0x77  x_int   是否支持快捷购买，1为支持   商家id转换成的fast_purchase
    public static final byte FIELD_FAST_PURCHASE = 0x77;

    private long source;
    private String serviceTel;
    private Drawable marker;
    private String url;
    private String message;
    private Drawable logo;
    private String name;
    private String refundService;
    private long fastPurchase;
    
    private Shangjia() {
        
    }

    public Shangjia (XMap data) throws APIException {
        super(data);
        init(data, true);
        
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
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.source = getLongFromData(FIELD_SOURCE, reset ? 0 : this.source);
        this.serviceTel = getStringFromData(FIELD_SERVICE_TEL, reset ? null : this.serviceTel);
        byte[] bytes = getBytesFromData(FIELD_MARKER, null);
        if (bytes != null) {
            BitmapDrawable bd = new BitmapDrawable(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            bd.setTargetDensity(Globals.g_metrics);
            this.marker = bd;
        } else if (reset) {
            this.marker = null;
        }
        this.url = getStringFromData(FIELD_URL, reset ? null : this.url);
        this.message = getStringFromData(FIELD_MESSAGE, reset ? null : this.message);
        bytes = getBytesFromData(FIELD_LOGO, null);
        if (bytes != null) {
            BitmapDrawable bd = new BitmapDrawable(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            bd.setTargetDensity(Globals.g_metrics);
            this.logo = bd;
        } else if (reset) {
            this.logo = null;
        }
        this.name = getStringFromData(FIELD_NAME, reset ? null : this.name);
        this.refundService = getStringFromData(FIELD_REFUND_SERVICE, reset ? null : this.refundService);
        this.fastPurchase = getLongFromData(FIELD_FAST_PURCHASE, reset ? 0 : this.fastPurchase);
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
            bd.setTargetDensity(Globals.g_metrics);
            logo = bd;
        }
        name = in.readString();
        refundService = in.readString();
        fastPurchase = in.readLong();
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
        out.writeString(refundService);
        out.writeLong(fastPurchase);
    }
    
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        
        if (object instanceof Shangjia) {
            Shangjia other = (Shangjia) object;
            if (other.source == this.source) {
                return true;
            }
        }
        
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public static void readShangjiaList(Context context) {
        synchronized (sLoadedList) {
            sLoadedList.clear();
        }
        List<Shangjia> list = new ArrayList<Shangjia>();
        String path = TKConfig.getDataPath(false) + "ShangjiaList";
            
        File file = new File(path);
        if (file.exists()) {
            synchronized (sWriteLock) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    XArray<XMap> xarray = (XArray<XMap>)ByteUtil.byteToXObject(Utility.readFileToByte(fis));
                    for(int i = 0, size = xarray.size(); i < size; i++) {
                        Shangjia shangjia = new Shangjia();
                        shangjia.init(xarray.get(i), true);
                        list.add(shangjia);
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
        } else {

            try {
                XMap xmap = new XMap();
                xmap.put(FIELD_SOURCE, SOURCE_WOWOTUAN);
                xmap.put(FIELD_SERVICE_TEL, "4001055555");
                xmap.put(FIELD_MARKER, Utility.getDrawableResource(context, R.drawable.ic_wowotuan_marker));
                xmap.put(FIELD_MESSAGE, context.getString(R.string.wowotuan_message));
                xmap.put(FIELD_NAME, context.getString(R.string.wowotuan_name));
                
                Shangjia shangjia = new Shangjia();
                shangjia.init(xmap, true);
                list.add(shangjia);
                
                xmap = new XMap();
                xmap.put(FIELD_SOURCE, SOURCE_NUOMI);
                xmap.put(FIELD_SERVICE_TEL, "4006888887");
                xmap.put(FIELD_MARKER, Utility.getDrawableResource(context, R.drawable.ic_nuomi_marker));
                xmap.put(FIELD_NAME, context.getString(R.string.nuomi_name));
                xmap.put(FIELD_MESSAGE, context.getString(R.string.nuomi_message));
                xmap.put(FIELD_REFUND_SERVICE, context.getString(R.string.nuomi_refund_service));
                
                shangjia = new Shangjia();
                shangjia.init(xmap, true);
                list.add(shangjia);
                
                xmap = new XMap();
                xmap.put(FIELD_SOURCE, SOURCE_MEITUAN);
                xmap.put(FIELD_SERVICE_TEL, "4006605335");
                xmap.put(FIELD_MARKER, Utility.getDrawableResource(context, R.drawable.ic_meituan_marker));
                xmap.put(FIELD_NAME, context.getString(R.string.meituan_name));
                xmap.put(FIELD_MESSAGE, context.getString(R.string.meituan_message));
                xmap.put(FIELD_REFUND_SERVICE, context.getString(R.string.meituan_refund_service));
                xmap.put(FIELD_URL, "http://r.union.meituan.com/url/visit/?a=1&key=Sl2zGAgo4NiBcy67K83MtEDx5XIsrLjP&url=http://i.meituan.com/orders");
                xmap.put(FIELD_FAST_PURCHASE, 1);
                
                shangjia = new Shangjia();
                shangjia.init(xmap, true);
                list.add(shangjia);

            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        DataQuery dataQuery = new DataQuery(context);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_SHANGJIA);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, Globals.g_Session_Id != null ? NEED_FIELD : NEED_FIELD_NO_LOGON);
        dataQuery.setup(Globals.getCurrentCityInfo().getId(), -1, -1, null);
        dataQuery.query();
        
        boolean writeShangjiaList = false;
        Response response = dataQuery.getResponse();
        if (response != null) {
            ShangjiaResponse shangjiaResponse = (ShangjiaResponse)response;
            ShangjiaList shangjiaList = shangjiaResponse.getList();
            if (shangjiaList != null) {
                List<Shangjia> shangjiaArrayList = shangjiaList.getList();
                List<Shangjia> oldShangjiaList = new ArrayList<Shangjia>();
                oldShangjiaList.addAll(list);
                
                list.clear();
                if (shangjiaArrayList != null && shangjiaArrayList.size() > 0) {
                    
                    list.addAll(shangjiaArrayList);
                    
                    // 如果此时返回的url为空且之前存在url(没有提交sessionId)，那么需要沿用之前的url
                    for(int i = list.size()-1; i >= 0; i--) {
                        Shangjia shangjia = list.get(i);
                        if (shangjia.url == null) {
                            for(int j = oldShangjiaList.size()-1; j >= 0; j--) {
                                Shangjia oldShangjia = oldShangjiaList.get(j);
                                if (oldShangjia.source == shangjia.source) {
                                    shangjia.url = oldShangjia.url;
                                    break;
                                }
                            }
                        }
                    }
                }
                writeShangjiaList = true;
            }
        }
        synchronized (shangjiaList) {
            shangjiaList.clear();
            shangjiaList.addAll(list);
        }
        
        if (writeShangjiaList) {
            writeShangjiaList(list);
        }
    }
    
    private static void writeShangjiaList(List<Shangjia> list) {
        synchronized (sWriteLock) {
            try {
                XArray<XMap> xarray = new XArray<XMap>();
                for(Shangjia shangjia : list) {
                    if (shangjia.fastPurchase != 1) {
                        shangjia.data.remove(FIELD_URL);
                    }
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
    
    private static List<Long> sLoadedList = new ArrayList<Long>();
    
    private static void loadShangjia(final Activity activity, long source, final Runnable runnable) {
        if (activity == null) {
            return;
        }
        synchronized (sLoadedList) {
            if (sLoadedList.contains(source)) {
                return;
            }
            sLoadedList.add(source);
        }
        final DataQuery dataQuery = new DataQuery(activity);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_SHANGJIA);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, Globals.g_Session_Id != null ? NEED_FIELD : NEED_FIELD_NO_LOGON);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SHANGJIA_IDS, String.valueOf(source));
        dataQuery.setup(Globals.getCurrentCityInfo().getId(), -1, -1, null, false, false, null);
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                dataQuery.query();
                activity.runOnUiThread(runnable);
            }
        }).start();
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
    
    public static List<Shangjia> getShangjiaList() {
        return shangjiaList;
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
    
    public String getRefundService() {
        return refundService;
    }

    public long getFastPurchase() {
        return fastPurchase;
    }

    public static XMapInitializer<Shangjia> Initializer = new XMapInitializer<Shangjia>() {

        @Override
        public Shangjia init(XMap data) throws APIException {
            return new Shangjia(data);
        }
    };
}

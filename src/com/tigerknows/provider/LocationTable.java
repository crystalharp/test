/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.provider;

import com.tigerknows.android.location.TKLocationManager;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.model.LocationQuery.TKNeighboringCellInfo;
import com.tigerknows.model.LocationQuery.TKScanResult;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 此类用于存储定位信息及绑定参数信息（基站、WIFI接入点列表、邻近基站列表信息）
 */
public class LocationTable {
    
    static final String TAG = "LocationTable";
    
    /**
     * 存储记录总数的最大值
     */
    static final int COUNT_MAX = 512;

	// HELPERS
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/** 
	 * 列名
	 */
    public static final String ID = "_id";
	public static final String HASHCODE = "tk_hashCode";
	public static final String MNC = "tk_mnc";
	public static final String MCC = "tk_mcc";
    public static final String TKCELLLOCATION = "tk_TKCellLocation";
    public static final String NEIGHBORINGCELLINFO_LIST = "tk_neighboringCellInfoList";
    public static final String WIFI_LIST = "tk_wifiList";
    public static final String TIME = "tk_time";
    public static final String LOCATION = "tk_location";
    public static final String PROVIDER = "tk_provider";

    /**
     * 来自老虎宝典的定位
     */
    static final int PROVIDER_TIGERKNOWS = 0;
    
    /**
     * 来自系统的GPS定位
     */
    static final int PROVIDER_GPS = 1;
    
    /**
     * 来自系统的网络定位
     */
    static final int PROVIDER_NETWORK = 2;
    
    /**
     * 来自收集的GPS定位
     */
    static final int PROVIDER_GPS_COLLECTION = 10;
    
    /**
     * 用于历史定位的来源列表
     */
    public static final List<Integer> Provider_List_Cache = new ArrayList<Integer>();
    
    /**
     * 用于定位收集的来源列表
     */
    public static final List<Integer> Provider_List_Collection = new ArrayList<Integer>();
    
    static {
        Provider_List_Cache.add(PROVIDER_TIGERKNOWS);
        Provider_List_Cache.add(PROVIDER_GPS);
        Provider_List_Cache.add(PROVIDER_NETWORK);
        
        Provider_List_Collection.add(PROVIDER_GPS_COLLECTION);
    }

	/**
	 * 数据库文件名
	 */
	protected static final String DATABASE_NAME = "locationDB";

	/**
	 *  表名
	 */
	protected static final String TABLE_NAME = "location";

	/**
	 * 数据库版本
	 */
	protected static final int DATABASE_VERSION = 2;

	/**
	 * 创建表的SQL语句
	 */
	private static final String TABLE_CREATE = "create table if not exists "
			+ TABLE_NAME
			+ "( " 
			+ ID + " INTEGER PRIMARY KEY, "
            + HASHCODE + " INTEGER, "
            + PROVIDER + " INTEGER, "
            + MNC + " INTEGER, "
            + MCC + " INTEGER, "
            + TKCELLLOCATION + " TEXT not null, "
            + NEIGHBORINGCELLINFO_LIST + " TEXT, "
            + WIFI_LIST + " TEXT, "
            + TIME + " TEXT, "
			+ LOCATION + " TEXT not null )";

	public Context mCtx;

	/**
	 * constructor
	 * 
	 * @param context
	 * 
	 */
	public LocationTable(Context context) {
		this.mCtx = context;
		open();
	}

	/**
	 * DatabaseHelper extends SQLiteOpenHelper
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
		    db.execSQL(TABLE_CREATE);
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		    switch (oldVersion) {
                case 1:
                    if (newVersion <= 1) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion2(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
		    }
		}
		
		void upgradeDatabaseToVersion2(SQLiteDatabase db) {
		    db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + TIME + " TEXT;");
		}
	}
	

	/**
	 * open
	 * @throws SQLException
	 */
	public LocationTable open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		mDb.execSQL(TABLE_CREATE);
		return this;
	}

	/**
	 * 关闭数据库
	 */
	public void close() {
		mDbHelper.close();
	}
	
	/**
	 * 是否已打开数据库
	 * @return
	 */
	public boolean isOpen(){
		return mDb.isOpen();
	}

	/**
	 * 将定位信息及绑定参数信息（基站、WIFI接入点列表、邻近基站列表信息）存储数据表中
	 */
	public void write(LocationParameter locationParameter, Location location) {
        if (locationParameter == null || location == null || LocationQuery.PROVIDER_ERROR.equals(location.getProvider())) {
            return;
        }
		if(!mDb.isOpen())
			return;
		ContentValues cv = new ContentValues();
		cv.put(HASHCODE, locationParameter.hashCode());
		cv.put(MNC, locationParameter.mnc);
        cv.put(PROVIDER, getProvider(location));
        cv.put(MCC, locationParameter.mcc);
        cv.put(TKCELLLOCATION, locationParameter.tkCellLocation.toString());
        cv.put(NEIGHBORINGCELLINFO_LIST, locationParameter.getNeighboringCellInfoString());
        cv.put(WIFI_LIST, locationParameter.getWifiString());
        if (locationParameter.time != null) {
            cv.put(TIME, locationParameter.time);
        }
        cv.put(LOCATION, location.getLatitude()+","+location.getLongitude()+","+location.getAccuracy());
		if(check(locationParameter)<=0) // new
			mDb.insert(TABLE_NAME, null, cv);
		else // overwrite
			mDb.update(TABLE_NAME, cv, HASHCODE + "=" + locationParameter.hashCode(), null);
	}
	
	/**
	 * 获取用于存储定位的来源标识
	 * @param location
	 * @return
	 */
	public int getProvider(Location location) {
	    int provider = PROVIDER_TIGERKNOWS;
	    if (location == null) {
	        return provider;
	    }
	    
	    if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
	        provider = PROVIDER_GPS;
	    } else if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
	        provider = PROVIDER_NETWORK;
	    } else if (TKLocationManager.GPS_COLLECTION_PROVIDER.equals(location.getProvider())) {
            provider = PROVIDER_GPS_COLLECTION;
        }
        return provider;
	}

	/**
	 * 查询绑定参数信息相同的记录数目
	 * @param locationParameter
	 * @return
	 */
	public int check(LocationParameter locationParameter) {
	    int count = -1;
	    if (locationParameter == null) {
	        return count;
	    }
		if(!mDb.isOpen())
			return count;
		int hashCode = locationParameter.hashCode();
		Cursor mCursor = mDb.query(true, TABLE_NAME,
				null, HASHCODE + "=" + hashCode,
				null, null, null, null, null);
		count = mCursor.getCount();
		if(mCursor!=null){
			mCursor.close();
		}
		return count;
	}

	/**
	 * 读取存储的定位记录
	 * @param map
	 * @param providerList
	 */
    public void read(HashMap<LocationParameter, Location> map, List<Integer> providerList) {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[] { PROVIDER, MNC, MCC, TKCELLLOCATION, NEIGHBORINGCELLINFO_LIST, WIFI_LIST, TIME, LOCATION}, null,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            int count = mCursor.getCount();
            try {
                for(int j = 0;j < count; j++) {
                    int provider = mCursor.getInt(0);
                    if (providerList.contains(provider)) {
                        LocationParameter locationParameter = new LocationParameter();
                        locationParameter.mnc = mCursor.getInt(1);
                        locationParameter.mcc = mCursor.getInt(2);
                        String str= mCursor.getString(3);
                        locationParameter.tkCellLocation = new TKCellLocation(str);
                        str= mCursor.getString(4);
                        if (TextUtils.isEmpty(str) == false) {
                            String[] arr = str.split(";");
                            for(int i = arr.length - 1; i >= 0; i--) {
                                TKNeighboringCellInfo neighboringCellInfo = new TKNeighboringCellInfo(arr[i]);
                                locationParameter.neighboringCellInfoList.add(neighboringCellInfo);
                            }
                        }
                        str= mCursor.getString(5);
                        if (TextUtils.isEmpty(str) == false) {
                            String[] arr = str.split(";");
                            for(int i = arr.length - 1; i >= 0; i--) {
                                TKScanResult tkScanResult = new TKScanResult(arr[i]);
                                if (tkScanResult != null)
                                    locationParameter.wifiList.add(tkScanResult);
                            }
                        }
                        str=mCursor.getString(6);
                        if (TextUtils.isEmpty(str) == false) {
                            locationParameter.time = str;
                        }
                        str= mCursor.getString(7);
                        if (TextUtils.isEmpty(str) == false) {
                            Location location = new Location(LocationQuery.PROVIDER_HISTORY);
                            String[] arr = str.split(",");
                            location.setLatitude(Double.parseDouble(arr[0]));
                            location.setLongitude(Double.parseDouble(arr[1]));
                            location.setAccuracy(Float.parseFloat(arr[2]));
                            map.put(locationParameter, location);
                        }
                    }
                    mCursor.moveToNext();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCursor.close();
        }
    }
    
	/**
	 * 清空定位数据表中所有记录
	 * @throws SQLException
	 */
	public boolean clear() throws SQLException {
		if(!mDb.isOpen())
			return false;
		mDb.delete(TABLE_NAME, null, null);
		return true;
	}
	
	/**
	 * 删除定位数据表中记录
	 * @param providerList
	 * @return
	 * @throws SQLException
	 */
    public boolean clear(List<Integer> providerList) throws SQLException {
        if(!mDb.isOpen())
            return false;
        mDb.delete(TABLE_NAME, makeWhereByProviderList(providerList), null);
        return true;
    }
    
    /**
     * 生成用于SQL查询语句WHERE的条件表达式字符串
     * @param providerList
     * @return
     */
    String makeWhereByProviderList(List<Integer> providerList) {
        StringBuilder s = new StringBuilder();
        for(int i = providerList.size()-1; i >= 0; i--) {
            s.append(PROVIDER);
            s.append("=");
            s.append(providerList.get(i));
            if (i > 0) {
                s.append(" OR ");
            }
        }
        return s.toString();
    }

    /**
     * 存储的记录数目超过最大值时，将超过的部分记录（插入时间较早的）删除
     * @param providerList
     * @throws SQLException
     */
    public void optimize(List<Integer> providerList) throws SQLException {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[]{ID}, makeWhereByProviderList(providerList),
                null, null, null, ID + " ASC", null);
        int count = mCursor.getCount();
        if (count > COUNT_MAX) {
            mCursor.moveToFirst();
            mCursor.move(count-COUNT_MAX);
            mDb.delete(TABLE_NAME, ID + " <= " + mCursor.getInt(0), null);
        }
        if(mCursor!=null){
            mCursor.close();
        }
    }
}

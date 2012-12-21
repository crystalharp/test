/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.provider;

import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.LocationQuery.LocationParameter;
import com.tigerknows.model.LocationQuery.TKCellLocation;
import com.tigerknows.model.LocationQuery.TKNeighboringCellInfo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.text.TextUtils;

import java.util.HashMap;

/**
 */
public class LocationTable {
    
    static final String TAG = "LocationTable";
    
    static final int MAX_COUNT = 512;

	// HELPERS
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// COLUMNS
    public static final String ID = "_id";
	public static final String HASHCODE = "hashCode";
	public static final String MNC = "mnc";
	public static final String MCC = "mcc";
    public static final String TKCELLLOCATION = "TKCellLocation";
    public static final String NEIGHBORINGCELLINFO_LIST = "neighboringCellInfoList";
    public static final String WIFI_LIST = "wifiList";
    public static final String LOCATION = "location";

	// DB NAME
	protected static final String DATABASE_NAME = "locationDB";

	// TABLES
	protected static final String TABLE_NAME = "location";
	protected static final int DATABASE_VERSION = 1;

	private static final String TABLE_CREATE = "create table if not exists "
			+ TABLE_NAME
			+ "( " 
			+ ID + " INTEGER PRIMARY KEY, "
            + HASHCODE + " INTEGER, "
            + MNC + " INTEGER, "
            + MCC + " INTEGER, "
            + TKCELLLOCATION + " TEXT not null, "
            + NEIGHBORINGCELLINFO_LIST + " TEXT, "
            + WIFI_LIST + " TEXT, "
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
		public void onCreate(SQLiteDatabase db) {}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
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

	public void close() {
		mDbHelper.close();
	}
	
	public boolean isOpen(){
		return mDb.isOpen();
	}

	/**
	 * set preference
	 * 
	 * 
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
        cv.put(MCC, locationParameter.mcc);
        cv.put(TKCELLLOCATION, locationParameter.tkCellLocation.toString());
        cv.put(NEIGHBORINGCELLINFO_LIST, locationParameter.getNeighboringCellInfoString());
        cv.put(WIFI_LIST, locationParameter.getWifiString());
        cv.put(LOCATION, location.getLatitude()+","+location.getLongitude()+","+location.getAccuracy());
		if(check(locationParameter)<=0) // new
			mDb.insert(TABLE_NAME, null, cv);
		else // overwrite
			mDb.update(TABLE_NAME, cv, HASHCODE + "=" + locationParameter.hashCode(), null);
	}

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

    public void read(HashMap<LocationParameter, Location> map) {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[] { MNC, MCC, TKCELLLOCATION, NEIGHBORINGCELLINFO_LIST, WIFI_LIST, LOCATION}, null,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        if(mCursor.getCount()>0){
            try {
                LocationParameter locationParameter = new LocationParameter();
                locationParameter.mnc = mCursor.getInt(0);
                locationParameter.mcc = mCursor.getInt(1);
                String str= mCursor.getString(2);
                locationParameter.tkCellLocation = new TKCellLocation(str);
                str= mCursor.getString(3);
                if (TextUtils.isEmpty(str) == false) {
                    String[] arr = str.split(";");
                    for(int i = arr.length - 1; i >= 0; i--) {
                        TKNeighboringCellInfo neighboringCellInfo = new TKNeighboringCellInfo(arr[i]);
                        locationParameter.neighboringCellInfoList.add(neighboringCellInfo);
                    }
                }
                str= mCursor.getString(4);
                if (TextUtils.isEmpty(str) == false) {
                    String[] arr = str.split(",");
                    for(int i = arr.length - 1; i >= 0; i--) {
                        locationParameter.wifiList.add(arr[i]);
                    }
                }
                str= mCursor.getString(5);
                if (TextUtils.isEmpty(str) == false) {
                    Location location = new Location(LocationQuery.PROVIDER_DATABASE);
                    String[] arr = str.split(",");
                    location.setLatitude(Double.parseDouble(arr[0]));
                    location.setLongitude(Double.parseDouble(arr[1]));
                    location.setAccuracy(Float.parseFloat(arr[2]));
                    map.put(locationParameter, location);
                }
                mCursor.moveToNext();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(mCursor!=null){
            mCursor.close();
        }
    }
	/**
	 * truncates table
	 * @throws SQLException
	 */
	public boolean clear() throws SQLException {
		if(!mDb.isOpen())
			return false;
		mDb.delete(TABLE_NAME, null, null);
		return true;
	}

    public void optimize() throws SQLException {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[]{ID}, null,
                null, null, null, ID + " ASC", null);
        int count = mCursor.getCount();
        if (count > MAX_COUNT) {
            mCursor.moveToFirst();
            mCursor.move(count-MAX_COUNT);
            mDb.delete(TABLE_NAME, ID + " <= " + mCursor.getInt(0), null);
        }
        if(mCursor!=null){
            mCursor.close();
        }
    }
}

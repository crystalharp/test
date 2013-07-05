package com.tigerknows.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.tigerknows.crypto.DataDecode;
import com.tigerknows.crypto.DataEncryptor;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;

import android.R.integer;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

public class HotelOrderTable {

    static final String TAG = "HotelOrderTable";
    
	// HELPERS
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// COLUMNS
    public static final String ORDER_ID= "tk_id";
    public static final String ORDER_CREATE_TIME = "tk_create_time";
    public static final String ORDER_DELETED= "tk_deleted";
	public static final String ORDER_CONTENT = "tk_content";
	
	// DB NAME
	protected static final String DATABASE_NAME = "hotelOrderDB";

	// TABLES
	protected static final String TABLE_NAME = "hotelOrder";
	protected static final int DATABASE_VERSION = 2;

	private static final String TABLE_CREATE = "create table if not exists "
			+ TABLE_NAME
			+ "( " 
            + ORDER_ID + " TEXT PRIMARY KEY, "
            + ORDER_CREATE_TIME + " INTEGER(20) not null,"
            + ORDER_DELETED + " INTEGER(1) not null,"
            + ORDER_CONTENT + " BLOB not null);";

	public Context mCtx;

	/**
	 * DatabaseHelper extends SQLiteOpenHelper
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			System.out.println("Create database.");
			db.execSQL(TABLE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			System.out.println("old version: " + oldVersion);
		}
		
	}
	

	/**
	 * constructor
	 * 
	 * @param context
	 * 
	 */
	public HotelOrderTable(Context context) {
		this.mCtx = context;
		open();
	}

	/**
	 * open
	 * @throws SQLException
	 */
	public HotelOrderTable open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}

	/**
	 * Insert a hotel order
	 * @param order	the order object to be inserted
	 * @return	1 if succeed, -1 if fail
	 * @throws IOException
	 * @throws APIException
	 */
	public long write(HotelOrder order) throws IOException, APIException {
		long result = -1;
        if (order == null) {
            return result;
        }
        
		if(!mDb.isOpen())
			return result;
		
		ContentValues cv = new ContentValues();
        cv.put(ORDER_ID, order.getId());
        cv.put(ORDER_CREATE_TIME, order.getCreateTime());
        DataEncryptor dataEncryptor = DataEncryptor.getInstance();
        byte[] encrypted = ByteUtil.xobjectToByte(order.toXMapForStorage());
        dataEncryptor.encrypt(encrypted);
        cv.put(ORDER_CONTENT, encrypted);
        result = mDb.insert(TABLE_NAME, null, cv);
        
        return result;
	}
	
	/**
	 * Read {@link count} orders starting from {@link start}
	 * @param start
	 * @param limit
	 * @return
	 * @throws IOException
	 * @throws APIException
	 */
	public List<HotelOrder> read(int start, int count) throws IOException, APIException{
		
		if(!mDb.isOpen())
			return null;
		
		List<HotelOrder> results = new ArrayList<HotelOrder>();
		Cursor cursor = mDb.query(TABLE_NAME, new String[]{ORDER_CONTENT}, null, null, null, null, ORDER_CREATE_TIME + " desc", start + "," + (count));
		int contentIndex = cursor.getColumnIndex(ORDER_CONTENT); 
		while (cursor.moveToNext()) {
			DataEncryptor dataEncryptor = DataEncryptor.getInstance();
			byte[] decrypted = cursor.getBlob(contentIndex);
			dataEncryptor.decrypt(decrypted);
			XMap orderXMap = (XMap) ByteUtil.byteToXObject(decrypted);
			results.add(new HotelOrder(orderXMap));
		}
		
		if (cursor != null) {
		    cursor.close();
		}
		
        return results;
	}
	
	/**
	 * update the content of a given order
	 * @param order
	 * @return
	 * @throws IOException
	 * @throws APIException
	 */
	public int update(HotelOrder order) throws IOException, APIException{
		int result = 0;
		
		if(!mDb.isOpen())
			return result;
		
		ContentValues cv = new ContentValues();
        DataEncryptor dataEncryptor = DataEncryptor.getInstance();
        byte[] encrypted = ByteUtil.xobjectToByte(order.toXMapForStorage());
        dataEncryptor.encrypt(encrypted);
        cv.put(ORDER_CONTENT, encrypted);
        result = mDb.update(TABLE_NAME, cv, ORDER_ID + "=?", new String[]{order.getId()});
		
		return result;
	}
	
	/**
	 * delete a order whoes id is {@link orderId}
	 * @param orderId
	 * @return the number of orders affected by
	 */
	public int delete(String orderId){
		int result = 0;

		if(!mDb.isOpen())
			return result;
		
		result = mDb.delete(TABLE_NAME, ORDER_ID + "=?", new String[]{orderId});
		
		return result;
	}
	
	public int count(){

		if(!mDb.isOpen())
			return 0;

		int count=0;
		Cursor cursor = mDb.query(TABLE_NAME, new String[]{"count(*)"}, null, null, null, null, null, null);
		if( cursor.moveToNext()){
			count = cursor.getInt(0);
		}
		cursor.close();
        return count;
		
	}
	
	public String getAllIds(){
		StringBuilder sb;

		if(!mDb.isOpen())
			return null;
		
		List<HotelOrder> results = new ArrayList<HotelOrder>();
		Cursor cursor = mDb.query(TABLE_NAME, new String[]{ORDER_ID}, null, null, null, null, null, null);
		sb = new StringBuilder(cursor.getCount() * 10);
		int contentIndex = cursor.getColumnIndex(ORDER_ID); 
		while (cursor.moveToNext()) {
			if(sb.length() > 0){
				sb.append(",");
			}
			sb.append(cursor.getString(contentIndex));
		}
		
		if (cursor != null) {
		    cursor.close();
		}
		
		return sb.toString();
	}
	
}

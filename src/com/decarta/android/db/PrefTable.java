/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * General wrapped database interface to save/retrieve user preference. All activities using this
 * class access the same database and same table.
 */
public class PrefTable {

	// HELPERS
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// PREF COLUMNS
	public static final String KEY_PREF_NAME = "name";
	public static final String KEY_PREF_VALUE = "value";

	// DB NAME
	protected static final String DATABASE_NAME = "tigerknowsPrefDB";

	// TABLES
	protected static final String PREF_TABLE_NAME = "prefs";
	protected static final int DATABASE_VERSION = 2;

	private static final String PREF_TABLE_CREATE = "create table if not exists "
			+ PREF_TABLE_NAME
			+ "("
			+ KEY_PREF_NAME
			+ " TEXT primary key, "
			+ KEY_PREF_VALUE + " TEXT not null )";

	public Context mCtx;

	/**
	 * constructor
	 * 
	 * @param context
	 * 
	 */
	public PrefTable(Context context) {
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
	public PrefTable open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		mDb.execSQL(PREF_TABLE_CREATE);
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
	public void setPref(String name, String value) {
		if(!mDb.isOpen())
			return;
		ContentValues cv = new ContentValues();
		cv.put(KEY_PREF_NAME, name);
		cv.put(KEY_PREF_VALUE, value);
		if(getPref(name)==null) // new
			mDb.insert(PREF_TABLE_NAME, null, cv);
		else // overwrite
			mDb.update(PREF_TABLE_NAME, cv, KEY_PREF_NAME + "='" + name + "'", null);
	}

	/**
	 * get preference
	 * @param name
	 */
	public String getPref(String name) {
		if(!mDb.isOpen())
			return null;
		Cursor mCursor = mDb.query(true, PREF_TABLE_NAME,
				new String[] { KEY_PREF_VALUE }, KEY_PREF_NAME + "='" + name.trim() + "'",
				null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		//Log.i("curosr",mCursor.toString());
		String returnString=null;
		if(mCursor.getCount()>0){
			returnString= mCursor.getString(0);
		}
		if(mCursor!=null){
			mCursor.close();
		}
		return returnString;
	}
	/**
	 * truncates table
	 * @throws SQLException
	 */
	public boolean clearPrefs() throws SQLException {
		if(!mDb.isOpen())
			return false;
		mDb.delete(PREF_TABLE_NAME, null, null);
		return true;
	}

}

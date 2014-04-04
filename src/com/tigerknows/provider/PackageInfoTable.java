package com.tigerknows.provider;

import java.io.IOException;
import java.util.List;

import com.decarta.android.exception.APIException;
import com.tigerknows.crypto.DataEncryptor;
import com.tigerknows.model.AppPush;
import com.tigerknows.model.POI;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;


public class PackageInfoTable {
    
    static final String TAG = "PackageInfoTable";
    
    // HELPERS
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    // COLUMNS
    public static final String PACKAGE_NAME = "_pname";
    public static final String FILE_NAME = "_fname";
    public static final String NOTIFY_TIME = "_notify_time";
    public static final String INSTALLED = "_installed";
    public static final String APPPUSH = "_app_push";

    // DB NAME
    protected static final String DATABASE_NAME = "packageinfoDB";

    // TABLES
    protected static final String TABLE_NAME = "package_info";
    protected static final int DATABASE_VERSION = 1;

    private static final String TABLE_CREATE = "create table if not exists "
            + TABLE_NAME
            + "( " 
            + PACKAGE_NAME + " TEXT PRIMARY KEY, "
            + FILE_NAME + " TEXT, "
            + NOTIFY_TIME + " INTEGER, "
            + INSTALLED + " INTEGER, "
            + APPPUSH + " BLOB )";

    public Context mCtx;

    /**
     * constructor
     * 
     * @param context
     * 
     */
    public PackageInfoTable(Context context) {
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
    public PackageInfoTable open() throws SQLException {
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
    
    public int readPackageInfo(List<RecordPackageInfo> list) throws APIException, IOException{
    	return readPackageInfo(list, null);
    }
    
    /**
     * 
     * @param list
     * @param installValue, -1 if select all
     * @param notifyValue, -1 if select all
     */
    public int readPackageInfo(List<RecordPackageInfo> list, int installValue, int notifyValue) throws APIException, IOException{
    	StringBuilder sb = new StringBuilder("");
    	if(installValue >= 0){
    		sb.append(" " + INSTALLED + "=" + installValue);
    	}
    	if(notifyValue >= 0){
    		if(sb.toString().length() != 0){
    			sb.append(" and");
    		}
    		sb.append(" " + NOTIFY_TIME + (notifyValue == 1 ? "<>" : "=") + "0");
    	}
    	if(sb.toString().length() == 0){
    		return readPackageInfo(list, null);
    	}else{
    		return readPackageInfo(list, sb.toString());
    	}
    }
    
    private int readPackageInfo(List<RecordPackageInfo> list, String selection) throws APIException, IOException{
        int total = 0;
        Cursor c = mDb.query(TABLE_NAME, null, selection, null, null, null, PACKAGE_NAME + " DESC");
        list.clear();
        if (c != null) {
            total = c.getCount();
            if (total > 0) {
                c.moveToFirst();
                for(int i = 0; i<total; i++) {
                	RecordPackageInfo data = readFromCursor(c);
                    if (data != null) {
                        list.add(data);
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }
        return total;
    }
    

    public static RecordPackageInfo readFromCursor(Cursor c) throws APIException, IOException {
        RecordPackageInfo data = null;
        if (c != null) {
            if (c.getCount() > 0) {
                String pname = c.getString(c.getColumnIndex(PACKAGE_NAME));
                String fname = c.getString(c.getColumnIndex(FILE_NAME));
                int installed = c.getInt(c.getColumnIndex(INSTALLED));
                long time = c.getLong(c.getColumnIndex(NOTIFY_TIME));
            	DataEncryptor dataEncryptor = DataEncryptor.getInstance();
            	byte[] decrypted = c.getBlob(c.getColumnIndex(APPPUSH));
            	if(decrypted == null){
                    data = new RecordPackageInfo(pname, installed, time, fname, null);
            	}else{
            		dataEncryptor.decrypt(decrypted);
            		XMap appPushXMap = (XMap)ByteUtil.byteToXObject(decrypted);
            		data = new RecordPackageInfo(pname, installed, time, fname, new AppPush(appPushXMap));
            	}
            }
        }
        return data;
    }

    public boolean addPackageInfo(RecordPackageInfo p) throws IOException, APIException {
        boolean isFailed = false;
        ContentValues values = new ContentValues();
        values.put(FILE_NAME, p.package_name);
        values.put(NOTIFY_TIME, p.notify_time);
        values.put(PACKAGE_NAME, p.package_name);
        values.put(INSTALLED, p.installed);
        DataEncryptor dataEncryptor = DataEncryptor.getInstance();
        byte[] encrypted = ByteUtil.xobjectToByte(p.app_push == null ? null : p.app_push.toXMapForStorage());
        dataEncryptor.encrypt(encrypted);
        values.put(APPPUSH, encrypted);
        if (!isFailed) {
            long result = mDb.insert(TABLE_NAME, null, values);
            if (result == -1) {
                isFailed = true;
            }
        }
        
        return isFailed;
    }
    

    public int deletePackageInfo(RecordPackageInfo p) {
		int count = mDb.delete(TABLE_NAME,  "(" + PACKAGE_NAME + "='" + p.package_name + "')", null);
        return count;
    }

    public int updateDatabase(RecordPackageInfo p) throws IOException, APIException {
        int count = 0;
        ContentValues values = new ContentValues();
        values.put(FILE_NAME, p.file_name);
        values.put(INSTALLED, p.installed);
        values.put(NOTIFY_TIME, p.notify_time);
        DataEncryptor dataEncryptor = DataEncryptor.getInstance();
        byte[] encrypted = ByteUtil.xobjectToByte(p.app_push == null ? null : p.app_push.toXMapForStorage());
        dataEncryptor.encrypt(encrypted);
        values.put(APPPUSH, encrypted);
        count = mDb.update(TABLE_NAME, values, "(" + PACKAGE_NAME + "='" + p.package_name + "')", null);
        return count;
    }
    

    public static class RecordPackageInfo {

        public String package_name;
        public String file_name;
        public long notify_time;
        public int installed;
        public AppPush app_push;
        
        // 下载完的package
        public RecordPackageInfo(String pname, String fname, AppPush app_push) {
            this(pname, 0, 0, fname, app_push);
        }
        
        // 扫描到的package
        
        public RecordPackageInfo(String pname, int installed, long notify_time) {
            this(pname, installed, notify_time, null, null);
        }
        
        public RecordPackageInfo(String pname, int installed, long notify_time, String fname, AppPush app_push) {
            this.package_name = pname;
            this.installed = installed;
            this.notify_time = notify_time;
            this.file_name = fname;
            this.app_push = app_push;
        }
        
    }
    
}

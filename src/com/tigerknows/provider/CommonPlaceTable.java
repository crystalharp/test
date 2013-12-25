package com.tigerknows.provider;

import java.util.List;

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


public class CommonPlaceTable {
    public static final int MAX_COUNT = 50;
    public static final int HISTORY_WORD_LIST_SIZE = 3;
    public static final int HISTORY_WORD_FIRST_SIZE = 10;
    
//    private static final LinkedList<TKWord> History_Word_POI = new LinkedList<TKWord>();
//    public static final LinkedList<TKWord> History_Word_Traffic = new LinkedList<TKWord>();
//    public static final LinkedList<TKWord> History_Word_Busline = new LinkedList<TKWord>();
//    
//    public static long POI_CityId = CityInfo.CITY_ID_INVALID;
//    public static long Traffic_CityId = CityInfo.CITY_ID_INVALID;
//    public static long Busline_CityId = CityInfo.CITY_ID_INVALID;
    
    static final String TAG = "HistoryWordTable";
    
    // HELPERS
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    // COLUMNS
    public static final String ID = "_id";
    public static final String ALIAS = "_alias";
    public static final String TYPE = "_type";
    public static final String POI = "_poi";

    // DB NAME
    protected static final String DATABASE_NAME = "commomplaceDB";

    // TABLES
    protected static final String TABLE_NAME = "commonplace";
    protected static final int DATABASE_VERSION = 1;

    private static final String TABLE_CREATE = "create table if not exists "
            + TABLE_NAME
            + "( " 
            + ID + " INTEGER PRIMARY KEY, "
            + ALIAS + " TEXT not null, "
            + TYPE + " INTEGER, "
            + POI + " BLOB )";

    public Context mCtx;

    /**
     * constructor
     * 
     * @param context
     * 
     */
    public CommonPlaceTable(Context context) {
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
    public CommonPlaceTable open() throws SQLException {
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
    
    public int readCommonPlace(List<CommonPlace> list){
        int total = 0;
        Cursor c = mDb.query(TABLE_NAME, null, null, null, null, null, ID + " DESC");
        if (c != null) {
            total = c.getCount();
            if (total > 0) {
                CommonPlace data;
                c.moveToFirst();
                for(int i = 0; i<total; i++) {
                    data = readFromCursor(c);
                    if (data != null) {
                        if (list.contains(data)) {
                            list.remove(data);
                        }
                        list.add(data);
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }
        
        if (total == 0) {
            CommonPlace cp = new CommonPlace("home", null, CommonPlace.TYPE_FIXED);
            addCommonPlace(cp);
            list.add(cp);
        }
        
        return total;
    }
    

    public static CommonPlace readFromCursor(Cursor c) {
        CommonPlace data = null;
        if (c != null) {
            if (c.getCount() > 0) {
                String a = c.getString(c.getColumnIndex(ALIAS));
                byte[] t = c.getBlob(c.getColumnIndex(POI));
                int type = c.getInt(c.getColumnIndex(TYPE));
                POI p = new POI();
                try {
                    XMap xmap = (XMap) ByteUtil.byteToXObject(t);
                    p.init(xmap, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                data = new CommonPlace(a, p, type);
                data.id = c.getLong(c.getColumnIndex(ID));
            }
        }
        return data;
    }

    public boolean addCommonPlace(CommonPlace c) {
        boolean isFailed = false;
        ContentValues values = new ContentValues();
        values.put(ALIAS, c.alias);
        values.put(TYPE, c.type);
        if (c.isEmptyFixedPlace()) {
            //空的就把POI信息删掉，目前用于第一条“家”的内容
            values.put(POI, "");
        } else {
            try {
                byte[] data = ByteUtil.xobjectToByte(c.poi.getData());
                if (data.length > 0) {
                    values.put(POI, data);
                }
            } catch (Exception e) {
                isFailed = true;
            }
        }
        if (!isFailed) {
            long result = mDb.insert(TABLE_NAME, null, values);
            if (result != -1) {
               c.id = result;
            } else {
                isFailed = true;
            }
        }
        
        return isFailed;
    }
    

    public int deleteCommonPlace(CommonPlace c) {
		int count = mDb.delete(TABLE_NAME,  "(" + ID + "=" + c.id + ")", null);
        c.id = -1;
        return count;
    }

    public int updateDatabase(CommonPlace c) {
        int count = 0;
        boolean isFailed = false;
        ContentValues values = new ContentValues();
        if (c.isEmptyFixedPlace()) {
            //空的就把POI信息删掉，目前用于第一条“家”的内容
            values.put(POI, "");
        } else {
            try {
                byte[] data = ByteUtil.xobjectToByte(c.poi.getData());
                if (data.length > 0) {
                    values.put(POI, data);
                }
            } catch (Exception e) {
                isFailed = true;
            }
        }
        if (!isFailed) {
//            count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(CONTENT_URI, c.id), values, null, null);
            count = mDb.update(TABLE_NAME, values, "(" + ID + "=" + c.id + ")", null);
        }
        return count;
    }
    

    public static class CommonPlace {
        public static int TYPE_NORMAL = 0;
        public static int TYPE_FIXED = 1;
        public String alias;
        public POI poi;
        public int type;
        long id;
        
        public CommonPlace(String a, POI p, int t) {
            alias = a;
            poi = p;
            type = t;
        }
        
        public void delete() {
            if (type == TYPE_FIXED) {
                poi = null;
            }
        }
        
        public boolean isEmptyFixedPlace() {
            return (type == TYPE_FIXED && 
                    (poi == null || TextUtils.isEmpty(poi.getName())));
        }
        
    }
    
}

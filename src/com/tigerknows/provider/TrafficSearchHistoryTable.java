package com.tigerknows.provider;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tigerknows.R;
import com.tigerknows.model.POI;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;

public class TrafficSearchHistoryTable {

    public static final int MAX_COUNT = 50;
    public static final int HISTORY_WORD_LIST_SIZE = 3;
    public static final int HISTORY_WORD_FIRST_SIZE = 10;
    
    public static final int TYPE_EMPTY = 0;
    public static final int TYPE_NO_EMPTY = 1;
    
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
    public static final String START = "_start";
    public static final String END = "_end";
    public static final String DATETIME = "_datetime";

    // DB NAME
    protected static final String DATABASE_NAME = "traffichistoryDB";

    // TABLES
    protected static final String TABLE_NAME = "traffichistory";
    protected static final int DATABASE_VERSION = 1;

    private static final String TABLE_CREATE = "create table if not exists "
            + TABLE_NAME
            + "( " 
            + ID + " INTEGER PRIMARY KEY, "
            + DATETIME + " INTEGER, "
            + START + " BLOB, "
            + END + " BLOB )";

    public Context mCtx;

    /**
     * constructor
     * 
     * @param context
     * 
     */
    public TrafficSearchHistoryTable(Context context) {
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
    public TrafficSearchHistoryTable open() throws SQLException {
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
    

    private static SearchHistory readFromCursor(Cursor c) {
        SearchHistory data = null;
        if (c != null) {
            if (c.getCount() > 0) {
                byte[] bs = c.getBlob(c.getColumnIndex(START));
                byte[] be = c.getBlob(c.getColumnIndex(END));
                POI os = new POI();
                POI oe = new POI();
                try {
                    XMap xs = (XMap) ByteUtil.byteToXObject(bs);
                    os.init(xs, true);
                    XMap xe = (XMap) ByteUtil.byteToXObject(be);
                    oe.init(xe, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                data = new SearchHistory(os, oe);
                data.id = c.getLong(c.getColumnIndex(ID));
            }
        }
        return data;
    }

    public boolean add(SearchHistory sh) {
        boolean isFailed = false;
        ContentValues values = new ContentValues();
        long dateTime = System.currentTimeMillis();
        values.put(Tigerknows.History.DATETIME, dateTime);
        if (sh.start.getName().equals(mCtx.getString(R.string.my_location))) {
            sh.start.setPosition(null);
        } else if (sh.end.getName().equals(mCtx.getString(R.string.my_location))) {
            sh.end.setPosition(null);
        }
        try {
            byte[] s = ByteUtil.xobjectToByte(sh.start.getData());
            if (s.length > 0) {
                values.put(START, s);
            }
            byte[] e = ByteUtil.xobjectToByte(sh.end.getData());
            if (e.length > 0) {
                values.put(END, e);
            }
        } catch (Exception e) {
            isFailed = true;
        }
        if (!isFailed) {
            long result = mDb.insert(TABLE_NAME, null, values);
            if (result != -1) {
                sh.id = result;
            } else {
                isFailed = true;
            }
        }
        
        optimize();
        
        return isFailed;
    }
    

    public int delete(SearchHistory sh) {
        int count = mDb.delete(TABLE_NAME, "(" + ID + "=" + sh.id + ")", null);
        return count;
    }

    public int update(SearchHistory sh) {
        int count = 0;
        ContentValues values = new ContentValues();
        long dateTime = System.currentTimeMillis();
        values.put(Tigerknows.History.DATETIME, dateTime);
        count = mDb.update(TABLE_NAME, values, "(" + ID + "=" + sh.id + ")", null);
        return count;
    }
    
    public void clear() {
        if(!mDb.isOpen())
            return;
        mDb.delete(TABLE_NAME, null, null);
    }
    
    public int readTrafficSearchHistory(List<SearchHistory> list){
        int count = 0;
        Cursor c = mDb.query(TABLE_NAME, null, null, null, null, null, "_datetime DESC");
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                SearchHistory data;
                c.moveToFirst();
                //              maxId = 0;
                for(int i = 0; i<count; i++) {
                    data = readFromCursor(c);
                    if (data != null) {
                        if (list.contains(data)) {
                            list.remove(data);
                        }
                        list.add(data);
                        //                      maxId = traffic.getDateTime();
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }
        return count;
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
            mCursor.move(count-MAX_COUNT-1);
            mDb.delete(TABLE_NAME, ID + " <= " + mCursor.getInt(0), null);
        }
        if(mCursor!=null){
            mCursor.close();
        }
    }
    
    static public class SearchHistory {
        public POI start;
        public POI end;
        long id;
        final static String arrow = "\u2192";
        
        public SearchHistory(POI s, POI e) {
            start = s;
            end = e;
        }
        
        public String genDescription() {
            return start.getName() + arrow + end.getName();
        }
        
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof SearchHistory) {
                SearchHistory other = (SearchHistory) o;
                if (start.getName().equals(other.start.getName()) &&
                        end.getName().equals(other.end.getName())) {
                    return true;
                }
            }
            return false;
            
        }
        
    }

}

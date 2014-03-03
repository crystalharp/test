/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.provider;

import com.decarta.Globals;
import com.decarta.android.db.PrefTable;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.TKWord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class HistoryWordTable {


    public static final int MAX_COUNT = 50;
    public static final int HISTORY_WORD_LIST_SIZE = 3;
    public static final int HISTORY_WORD_FIRST_SIZE = 10;
    
    public static final int TYPE_POI = 0;
    public static final int TYPE_TRAFFIC = 1;
    public static final int TYPE_BUSLINE = 2;
    
    private static final LinkedList<TKWord> History_Word_POI = new LinkedList<TKWord>();
    private static final LinkedList<TKWord> History_Word_Traffic = new LinkedList<TKWord>();
    private static final LinkedList<TKWord> History_Word_Busline = new LinkedList<TKWord>();
    
    static final String TAG = "HistoryWordTable";
    
	// HELPERS
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// COLUMNS
    public static final String ID = "_id";
    public static final String HASHCODE = "tk_hashCode";
	public static final String WORD = "tk_word";
    public static final String CITY_ID = "tk_cityId";
	public static final String TYPE = "tk_type";
	public static final String POSITION = "tk_position";
    public static final String ADDRESS = "tk_address";

	// DB NAME
	protected static final String DATABASE_NAME = "historyWordDB";

	// TABLES
	protected static final String TABLE_NAME = "historyWord";
	protected static final int DATABASE_VERSION = 2;

	private static final String TABLE_CREATE = "create table if not exists "
			+ TABLE_NAME
			+ "( " 
			+ ID + " INTEGER PRIMARY KEY, "
            + HASHCODE + " INTEGER, "
            + WORD + " TEXT not null, "
            + CITY_ID + " INTEGER, "
            + TYPE + " INTEGER, "
            + POSITION + " TEXT,"
            + ADDRESS + " TEXT )";

	public Context mCtx;

	/**
	 * constructor
	 * 
	 * @param context
	 * 
	 */
	public HistoryWordTable(Context context) {
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
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            assert (newVersion == DATABASE_VERSION);
            LogWrapper.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + "...");
            switch (oldVersion) {
                case 1:
                    if (newVersion <= 1) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseFrom2(db);
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
		
		private void upgradeDatabaseFrom2(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + ADDRESS + " TEXT;");
        }
	}

	/**
	 * open
	 * @throws SQLException
	 */
	public HistoryWordTable open() throws SQLException {
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
	public void write(TKWord tkWord, int type) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
            return;
        }
		if(!mDb.isOpen())
			return;
        int hashCode = tkWord.hashCode();
		ContentValues cv = new ContentValues();
        cv.put(HASHCODE, hashCode);
		cv.put(WORD, tkWord.word);
        cv.put(TYPE, type);
		Position position = tkWord.position;
		if (position != null) {
            cv.put(POSITION, position.getLat()+","+position.getLon());
		}
        String address = tkWord.address;
        if (address != null) {
            cv.put(ADDRESS, address);
        }

		mDb.delete(TABLE_NAME,  "(" + TYPE + "=" + type+ ") AND (" + HASHCODE + "=" + hashCode+ ")", null);
		mDb.insert(TABLE_NAME, null, cv);
	}

    private void read(List<TKWord> list, int type) {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[] { WORD, POSITION, ADDRESS}, "(" + TYPE + "=" + type+ ")",
                null, null, null, ID + " DESC", null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        int count = mCursor.getCount();
        if(mCursor.getCount()>0){
            for(int i = 0; i < count; i++) {
                TKWord tkWord = new TKWord();
                tkWord.attribute = TKWord.ATTRIBUTE_HISTORY;
                tkWord.word = mCursor.getString(0);
                String str= mCursor.getString(1);
                if (TextUtils.isEmpty(str) == false) {
                    String[] arr = str.split(",");
                    try {
                        double lat = Double.parseDouble(arr[0]);
                        double lon = Double.parseDouble(arr[1]);
                        Position position = new Position(lat, lon);
                        tkWord.position = position;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                tkWord.address = mCursor.getString(2);
                list.add(tkWord);
                mCursor.moveToNext();
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
	public boolean clear(int type) throws SQLException {
		if(!mDb.isOpen())
			return false;
		mDb.delete(TABLE_NAME, "(" + TYPE + "=" + type+ ")", null);
		return true;
	}
	
	/**
     * 存储的记录数目超过最大值时，将超过的部分记录（插入时间较早的）删除
     * 某个类型的历史词最大存储MAX_COUNT个，与城市没有关系
     * @param providerList
     * @throws SQLException
     */
    public void optimize(int type) throws SQLException {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[]{ID}, "(" + TYPE + "=" + type+ ")",
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
    
    public static void readHistoryWord(Context context, int type) {
        List<TKWord> list;
        String key;
        if (TYPE_POI == type) {
            key = TKConfig.PREFS_HISTORY_WORD_POI;
            list = History_Word_POI;
        } else if (TYPE_TRAFFIC == type) {
            key = TKConfig.PREFS_HISTORY_WORD_TRAFFIC;
            list = History_Word_Traffic;
        } else {
            key = TKConfig.PREFS_HISTORY_WORD_BUSLINE;
            list = History_Word_Busline;
        }
        synchronized (list) {
            list.clear();
            HistoryWordTable historyWordTable = new HistoryWordTable(context);
            List<String> listAtPrefTable = readHistoryWordV4(context, type);
            if (listAtPrefTable.size() > 0) {
                for(int i = 0, size = listAtPrefTable.size(); i < size; i++) {
                    historyWordTable.write(new TKWord(TKWord.ATTRIBUTE_HISTORY, listAtPrefTable.get(i)), type);
                }
                listAtPrefTable.clear();
                clearHistoryWord(context, key);
            }
            
            historyWordTable.read(list, type);
            historyWordTable.close();
        }
    }
    
    public static void addHistoryWord(Context context, TKWord tkWord, int type) {
        if (tkWord == null || tkWord.word == null || TextUtils.isEmpty(tkWord.word.trim())) {
            return;
        }
        List<TKWord> list;
        if (TYPE_POI == type) {
            list = History_Word_POI;
        } else if (TYPE_TRAFFIC == type) {
            list = History_Word_Traffic;
        } else {
            list = History_Word_Busline;
        }
        synchronized (list) {
            int index = list.indexOf(tkWord);
            TKWord old = null;
            if (index >= 0 && index < list.size()) {
                old = list.remove(index);
                if (tkWord.position == null) {
                    tkWord.position = old.position;
                }
                if (tkWord.address == null) {
                    tkWord.address = old.address;
                }
            }
            list.add(0, tkWord);
            if (list.size() > MAX_COUNT) {
                for(int i = list.size()-1; i >= MAX_COUNT; i--) {
                    list.remove(i);
                }
            }
        }
        HistoryWordTable historyWordTable = new HistoryWordTable(context);
        historyWordTable.write(tkWord, type);
        historyWordTable.optimize(type);
        historyWordTable.close();
    }
    
    public static void clearHistoryWord(Context context, int type) {
        List<TKWord> list;
        if (TYPE_POI == type) {
            list = History_Word_POI;
        } else if (TYPE_TRAFFIC == type) {
            list = History_Word_Traffic;
        } else {
            list = History_Word_Busline;
        }
        synchronized (list) {
            list.clear();
        }
        HistoryWordTable historyWordTable = new HistoryWordTable(context);
        historyWordTable.clear(type);
        historyWordTable.close();
    }
    
    public static List<TKWord> generateSuggestWordList(Sphinx sphinx, String searchWord, int type){
        List<TKWord> suggestList = new LinkedList<TKWord>();
        MapEngine mapEngine = MapEngine.getInstance();
        List<TKWord> associationalList;
        List<TKWord> historyWordList;
        
        if (searchWord == null) {
            return suggestList;
        }
        String key = searchWord.trim();
        
        switch (type){
        case TYPE_POI:
            associationalList = stringToTKWord(mapEngine.getwordslistString(key, 2), TKWord.ATTRIBUTE_SUGGEST);
            historyWordList = History_Word_POI;
            break;
        case TYPE_TRAFFIC:
            associationalList = stringToTKWord(mapEngine.getwordslistString(key, 0), TKWord.ATTRIBUTE_SUGGEST);
            historyWordList = History_Word_Traffic;
            break;
        case TYPE_BUSLINE:
            associationalList = stringToTKWord(mapEngine.getwordslistString(key, 0), TKWord.ATTRIBUTE_SUGGEST);
            historyWordList = History_Word_Busline;
            break;
        default:
            return suggestList;
        }

        synchronized (historyWordList) {
            if (TextUtils.isEmpty(key) && historyWordList.size() > 0){
                suggestList.addAll(historyWordList);
                suggestList.add(TKWord.getCleanupTKWord(sphinx));
                return suggestList;
            }
            
            int historyIndex = 0;
            for (int i = 0, size = historyWordList.size(); i < size; i++) {
                if (historyIndex >= HISTORY_WORD_LIST_SIZE) {
                     break;
                }
                TKWord historyWord = historyWordList.get(i);
                //xupeng:确认key与建议词完全匹配的时候保留建议词
    //            if (historyWord.word.startsWith(searchword) && !historyWord.word.equals(searchword)) {
                if (historyWord.word.startsWith(key)) {
                    suggestList.add(historyWord);
                    associationalList.remove(historyWord);
                    historyIndex++;
                }
            }
        }
        
        suggestList.addAll(associationalList);
        return suggestList;
    }
    
    public static List<TKWord> getHistoryWordList(String searchword, int type) {
        List<TKWord> list = new LinkedList<TKWord>();
        List<TKWord> historyWordList;
        if (HistoryWordTable.TYPE_POI == type) {
            historyWordList = History_Word_POI;
        } else if (HistoryWordTable.TYPE_TRAFFIC == type) {
            historyWordList = History_Word_Traffic;
        } else {
            historyWordList = History_Word_Busline;
        }

        synchronized (historyWordList) {
            int historyIndex = 0;
            for (int i = 0, size = historyWordList.size(); i < size; i++) {
                if (historyIndex >= HISTORY_WORD_FIRST_SIZE) {
                    break;
                }
                TKWord tkWord = historyWordList.get(i);
                if (searchword == null || (!tkWord.word.equals(searchword) && !list.contains(tkWord.word))) {
                    list.add(tkWord);
                    historyIndex++;
                }
            }
        }
        return list;
    }
    
    public static List<TKWord> stringToTKWord(List<String> list, int attribute) {
        List<TKWord> tkList = new LinkedList<TKWord>();
        if (list == null) {
            return tkList;
        }
        for(int i = 0, size = list.size(); i < size; i++) {
            tkList.add(new TKWord(attribute, list.get(i)));
        }
        return tkList;
    }
    
    @Deprecated
    private static List<String> readHistoryWordV4(Context context, int type) {
        int cityId = Globals.getCurrentCityInfo(context).getId();
        List<String> list = new ArrayList<String>();
        if (context == null) {
            return list;
        }
        String key;
        if (TYPE_POI == type) {
            key = TKConfig.PREFS_HISTORY_WORD_POI;
        } else if (TYPE_TRAFFIC == type) {
            key = TKConfig.PREFS_HISTORY_WORD_TRAFFIC;
        } else {
            key = TKConfig.PREFS_HISTORY_WORD_BUSLINE;
        }
        
        PrefTable prefTable = new PrefTable(context);
        String historyWord = prefTable.getPref(String.format(key, cityId));
        prefTable.close();
        
        if (!TextUtils.isEmpty(historyWord)) {
            String[] strs = historyWord.split(" ");
            for (int i = 0, size = strs.length; i < size; i++) {
                String word = strs[i];
                if (!TextUtils.isEmpty(word)) {
                    list.add(word.replace("\n", " "));
                }
            }
        }
        
        return list;
    }
    
    @Deprecated
    private static void clearHistoryWord(Context context, String key) {
        if (context == null || key == null) {
            return;
        }
        PrefTable prefTable = new PrefTable(context);
        prefTable.setPref(key, "");
        prefTable.close();
    }
    
    public static void onDestroy() {
        synchronized (History_Word_POI) {
            History_Word_POI.clear();
        }
        synchronized (History_Word_Traffic) {
            History_Word_Traffic.clear();
        }
        synchronized (History_Word_Busline) {
            History_Word_Busline.clear();
        }
    }
}

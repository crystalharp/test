/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.provider;

import com.decarta.android.db.PrefTable;
import com.decarta.android.location.Position;
import com.tigerknows.TKConfig;
import com.tigerknows.model.TKWord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

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
    public static final LinkedList<TKWord> History_Word_Traffic = new LinkedList<TKWord>();
    public static final LinkedList<TKWord> History_Word_Busline = new LinkedList<TKWord>();
    
    static final String TAG = "HistoryWordTable";
    
	// HELPERS
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// COLUMNS
    public static final String ID = "_id";
    public static final String HASHCODE = "hashCode";
	public static final String WORD = "word";
    public static final String CITY_ID = "cityId";
	public static final String TYPE = "type";
	public static final String POSITION = "position";

	// DB NAME
	protected static final String DATABASE_NAME = "historyWordDB";

	// TABLES
	protected static final String TABLE_NAME = "historyWord";
	protected static final int DATABASE_VERSION = 1;

	private static final String TABLE_CREATE = "create table if not exists "
			+ TABLE_NAME
			+ "( " 
			+ ID + " INTEGER PRIMARY KEY, "
            + HASHCODE + " INTEGER, "
            + WORD + " TEXT not null, "
            + CITY_ID + " INTEGER, "
            + TYPE + " INTEGER, "
            + POSITION + " TEXT )";

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
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
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
	public void write(TKWord tkWord, int cityId, int type) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
            return;
        }
		if(!mDb.isOpen())
			return;
		ContentValues cv = new ContentValues();
		cv.put(WORD, tkWord.word);
        cv.put(CITY_ID, cityId);
        cv.put(TYPE, tkWord.attribute);
		Position position = tkWord.position;
		if (position != null) {
            cv.put(POSITION, position.getLat()+","+position.getLon());
		}
		if(check(tkWord, cityId)<=0) // new
			mDb.insert(TABLE_NAME, null, cv);
		else // overwrite
			mDb.update(TABLE_NAME, cv, HASHCODE + "=" + tkWord.hashCode(), null);
	}

	public int check(TKWord tkWord, int cityId) {
	    int count = -1;
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
	        return count;
	    }
		if(!mDb.isOpen())
			return count;
		int hashCode = tkWord.hashCode();
		Cursor mCursor = mDb.query(true, TABLE_NAME,
				null, "(" + HASHCODE + "=" + hashCode + ") AND (" + CITY_ID + "=" + cityId+ ")",
				null, null, null, null, null);
		count = mCursor.getCount();
		if(mCursor!=null){
			mCursor.close();
		}
		return count;
	}

    public void read(List<TKWord> list, int type) {
        if(!mDb.isOpen())
            return;
        Cursor mCursor = mDb.query(true, TABLE_NAME,
                new String[] { WORD, POSITION}, TYPE + "=" + type,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        if(mCursor.getCount()>0){
            try {
                TKWord tkWord = new TKWord();
                tkWord.word = mCursor.getString(0);
                String str= mCursor.getString(1);
                if (TextUtils.isEmpty(str) == false) {
                    String[] arr = str.split(",");
                    double lat = Double.parseDouble(arr[0]);
                    double lon = Double.parseDouble(arr[1]);
                    Position position = new Position(lat, lon);
                    tkWord.position = position;
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
	public boolean clear(int cityId, int type) throws SQLException {
		if(!mDb.isOpen())
			return false;
		mDb.delete(TABLE_NAME, "(" + CITY_ID + "=" + cityId + ") AND (" + TYPE + "=" + type+ ")", null);
		return true;
	}

    public void optimize(int cityId, int type) throws SQLException {
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
    
    public static void readHistoryWord(Context context, int cityId, int type) {
        List<TKWord> list;
        HistoryWordTable historyWordTable = new HistoryWordTable(context);
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
        list.clear();
        List<String> listAtPrefTable = new ArrayList<String>();
        readHistoryWord(context, listAtPrefTable, String.format(key, cityId));
        if (listAtPrefTable.size() > 0) {
            for(int i = 0, size = listAtPrefTable.size(); i < size; i++) {
                historyWordTable.write(new TKWord(TKWord.ATTRIBUTE_HISTORY, listAtPrefTable.get(i)), cityId, type);
            }
            listAtPrefTable.clear();
            clearHistoryWord(context, key);
        }
        
        historyWordTable.read(list, type);
        historyWordTable.close();
    }
    
    public static void addHistoryWord(Context context, TKWord tkWord, int cityId, int type) {
        List<TKWord> list;
        if (TYPE_POI == type) {
            list = History_Word_POI;
        } else if (TYPE_TRAFFIC == type) {
            list = History_Word_Traffic;
        } else {
            list = History_Word_Busline;
        }
        list.add(tkWord);
        HistoryWordTable historyWordTable = new HistoryWordTable(context);
        historyWordTable.write(tkWord, cityId, type);
        historyWordTable.optimize(cityId, type);
        historyWordTable.close();
    }
    
    public static void clearHistoryWord(Context context, int cityId, int type) {
        HistoryWordTable historyWordTable = new HistoryWordTable(context);
        historyWordTable.clear(cityId, type);
        historyWordTable.close();
    }
    
    public static List<TKWord> mergeTKWordList(List<TKWord> suggestWordList, String searchword, int type) {
        List<TKWord> list = new ArrayList<TKWord>();
        if (suggestWordList == null || searchword == null) {
            return list;
        }
        List<TKWord> historyWordList;
        if (TYPE_POI == type) {
            historyWordList = History_Word_POI;
        } else if (TYPE_TRAFFIC == type) {
            historyWordList = History_Word_Traffic;
        } else {
            historyWordList = History_Word_Busline;
        }
        // TODO: sony close history word
        int historyIndex = 0;
        for (int size = historyWordList.size(); historyIndex < size; historyIndex++) {
            if (historyIndex >= HISTORY_WORD_LIST_SIZE) {
                 break;
            }
            TKWord historyWord = historyWordList.get(historyIndex);
            if (historyWord.word.startsWith(searchword) && (!historyWord.word.equals(searchword) || historyWord.position != null)) {
                if (TYPE_TRAFFIC == type) {
                    for(int i = suggestWordList.size()-1; i >= 0; i--) {
                        TKWord tkWord = suggestWordList.get(i);
                        if (historyWord.word.equals(tkWord.word) && (tkWord.position == null || historyWord.position != null)) {
                            suggestWordList.remove(tkWord);
                        }
                    }
                } else {
                    suggestWordList.remove(historyWord);
                }
                list.add(historyWord);
                historyIndex++;
            }
        }
        suggestWordList.remove(searchword);
        
        list.addAll(suggestWordList);
        return list;
    }
    
    public static List<TKWord> getHistoryWordList(String searchword, int type) {
        List<TKWord> list = new ArrayList<TKWord>();
        if (searchword == null) {
            return list;
        }
        List<TKWord> historyWordList;
        if (HistoryWordTable.TYPE_POI == type) {
            historyWordList = History_Word_POI;
        } else if (HistoryWordTable.TYPE_TRAFFIC == type) {
            historyWordList = History_Word_Traffic;
        } else {
            historyWordList = History_Word_Busline;
        }
        for (int i = 0, size = historyWordList.size(); i < size; i++) {
            if (i >= HISTORY_WORD_FIRST_SIZE) {
                break;
            }
            TKWord tkWord = historyWordList.get(i);
            if (!tkWord.word.equals(searchword) && !list.contains(tkWord.word)) {
                list.add(tkWord);
                i++;
            }
        }
        return list;
    }
    
    public static List<TKWord> stringToTKWord(List<String> list, int attribute) {
        List<TKWord> tkList = new ArrayList<TKWord>();
        if (list == null) {
            return tkList;
        }
        for(int i = 0, size = list.size(); i < size; i++) {
            tkList.add(new TKWord(attribute, list.get(i)));
        }
        return tkList;
    }
    
    @Deprecated
    private static void readHistoryWord(Context context, List<String> list, String key) {
        list.clear();
        if (context == null || list == null || key == null) {
            return;
        }
        
        PrefTable prefTable = new PrefTable(context);
        String historyWord = prefTable.getPref(key);
        prefTable.close();
        
        if (!TextUtils.isEmpty(historyWord)) {
            String[] strs = historyWord.split(" ");
            for (int i = 0, size = strs.length; i < size; i--) {
                String word = strs[i];
                if (!TextUtils.isEmpty(word)) {
                    list.add(word);
                }
            }
        }
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
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.provider;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.provider.Tigerknows.Favorite;
import com.tigerknows.provider.Tigerknows.History;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * @author Peng Wenyue
 */
public class TigerknowsProvider extends ContentProvider {
    private DatabaseHelper mOpenHelper;

    private static final String TAG = "TigerknowsProvider";

    private static final String DATABASE_NAME = "tigerknows.db";

    private static final int DATABASE_VERSION = 8;
    
    private static HashMap<String, String> HISTORY_LIST_PROJECTION_MAP;
    
    private static HashMap<String, String> FAVORITE_LIST_PROJECTION_MAP;
    
    private static HashMap<String, String> POI_LIST_PROJECTION_MAP;
    
    private static HashMap<String, String> BUSLINE_LIST_PROJECTION_MAP;
    
    private static HashMap<String, String> TRANSITPLAN_LIST_PROJECTION_MAP;
    
    private static final int HISTORY = 1;
    
    private static final int HISTORY_ID = 2;
    
    private static final int FAVORITE = 9;
    
    private static final int FAVORITE_ID = 10;
    
    private static final int POI = 11;
    
    private static final int POI_ID = 12;
    
    private static final int BUSLINE = 15;
    
    private static final int BUSLINE_ID = 16;
    
    private static final int TRANSITPLAN = 17;
    
    private static final int TRANSITPLAN_ID = 18;
    
    private static final int POI_COUNT = 19;
    
    private static final int HISTORY_COUNT = 20;
    
    private static final int FAVORITE_COUNT = 21;
    
    
    private static final UriMatcher URL_MATCHER;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        
        
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            
            createTables(db);
        }

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
                        upgradeDatabaseToVersion2(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 2:
                    if (newVersion <= 2) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion3(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 3:
                    if (newVersion <= 3) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion4(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 4:
                    if (newVersion <= 4) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion5(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 5:
                    if (newVersion <= 5) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion6(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 6:
                    if (newVersion <= 6) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion7(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                case 7:
                    if (newVersion <= 7) {
                        return;
                    }

                    db.beginTransaction();
                    try {
                        upgradeDatabaseToVersion8(db);
                        db.setTransactionSuccessful();
                    } catch (Throwable ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        break;
                    } finally {
                        db.endTransaction();
                    }
                    break;
                default:
                    break;
            }
            
        }

        private void upgradeDatabaseToVersion2(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS popcategories");
            db.execSQL("DROP TABLE IF EXISTS popchildcategories");
            db.execSQL("DROP TABLE IF EXISTS popplace");
            upgradeDatabaseToVersion3(db);
        }

        private void upgradeDatabaseToVersion3(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS province");
            db.execSQL("DROP TABLE IF EXISTS city");
            db.execSQL("DROP TABLE IF EXISTS region");
            db.execSQL("DROP TABLE IF EXISTS searchhistory");
            upgradeDatabaseToVersion4(db);
        }

        private void upgradeDatabaseToVersion4(SQLiteDatabase db) {
            upgradeDatabaseToVersion5(db);
        }

        private void upgradeDatabaseToVersion5(SQLiteDatabase db) {
            upgradeDatabaseToVersion6(db);
        }

        private void upgradeDatabaseToVersion6(SQLiteDatabase db) {
            // delete the title column in favorite and history table 
            // delete the richcategory table
            db.execSQL("DROP TABLE IF EXISTS richcategory;");
            
            db.execSQL("DROP TABLE IF EXISTS historysearch;");
            db.execSQL("DROP TABLE IF EXISTS historydiscover;");
            db.execSQL("DROP TABLE IF EXISTS historytraffic;");
            db.execSQL("DROP TABLE IF EXISTS historybusline;");
            db.execSQL("DROP TABLE IF EXISTS categorygroup;");
            
            db.execSQL("DROP TABLE IF EXISTS history;");
            db.execSQL("DROP TABLE IF EXISTS favorite;");
            db.execSQL("DROP TABLE IF EXISTS poi;");
            db.execSQL("DROP TABLE IF EXISTS transitplan;");
            db.execSQL("DROP TABLE IF EXISTS transitstep;");
            db.execSQL("DROP TABLE IF EXISTS pathline;");
            db.execSQL("DROP TABLE IF EXISTS busline;");

            db.execSQL("CREATE TABLE history (_id INTEGER PRIMARY KEY,"
                    + "    history_type INTEGER, _datetime INTEGER);");
            db.execSQL("CREATE TABLE favorite (_id INTEGER PRIMARY KEY,"
                    + "    favorite_type INTEGER, _datetime INTEGER);");
            db.execSQL("CREATE TABLE poi (_id INTEGER PRIMARY KEY,"
                    + "    store_type INTEGER, parent_id INTEGER, poi_name TEXT, poi_x REAL, poi_y REAL, "
                    + "    poi_version INTEGER, _data BLOB, _datetime INTEGER);");
            db.execSQL("CREATE TABLE transitplan (_id INTEGER PRIMARY KEY,"
                    + "    store_type INTEGER, _type INTEGER, parent_id INTEGER, times INTEGER, total_length INTEGER, start INTEGER, end INTEGER, _data BLOB);");
            db.execSQL("CREATE TABLE busline (_id INTEGER PRIMARY KEY,"
                    + "    store_type INTEGER, parent_id INTEGER, busline_name TEXT, busline_num INTEGER, total_length INTEGER, _data BLOB);");
            
            upgradeDatabaseToVersion7(db);
        }
        
        private void upgradeDatabaseToVersion7(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE favorite ADD _alias TEXT;");
            db.execSQL("ALTER TABLE poi ADD _alias TEXT;");
            upgradeDatabaseToVersion8(db);
        }
        
        private void upgradeDatabaseToVersion8(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE poi ADD _comment_data BLOB;");
        }
        
        private void createTables(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE history (_id INTEGER PRIMARY KEY,"
                    + "    history_type INTEGER, _datetime INTEGER);");
            db.execSQL("CREATE TABLE favorite (_id INTEGER PRIMARY KEY,"
                    + "    favorite_type INTEGER, _alias TEXT,  _datetime INTEGER);");
            db.execSQL("CREATE TABLE poi (_id INTEGER PRIMARY KEY,"
                    + "    store_type INTEGER, parent_id INTEGER, poi_name TEXT, _alias TEXT, poi_x REAL, poi_y REAL, "
                    + "    poi_version INTEGER, _data BLOB, _comment_data BLOB, _datetime INTEGER);");
            db.execSQL("CREATE TABLE transitplan (_id INTEGER PRIMARY KEY,"
                    + "    store_type INTEGER, _type INTEGER, parent_id INTEGER, times INTEGER, total_length INTEGER, start INTEGER, end INTEGER, _data BLOB);");
            db.execSQL("CREATE TABLE busline (_id INTEGER PRIMARY KEY,"
                    + "    store_type INTEGER, parent_id INTEGER, busline_name TEXT, busline_num INTEGER, total_length INTEGER, _data BLOB);");
        }
    }
    
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());

        return (mOpenHelper == null) ? false : true;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        String myWhere;
        int count = 0;
        
        switch (URL_MATCHER.match(url)) {
            case HISTORY:
                deleteHistoryDetail(url, where, whereArgs);
                count = db.delete("history", where, whereArgs);
                break;
                
            case HISTORY_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
                deleteHistoryDetail(Tigerknows.History.CONTENT_URI, myWhere, whereArgs);    
                count = db.delete("history", myWhere, whereArgs);
                break;
                
            case FAVORITE:
                deleteFavoriteDetail(url, where, whereArgs);
                count = db.delete("favorite", where, whereArgs);
                break;
                
            case FAVORITE_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
                deleteFavoriteDetail(Favorite.CONTENT_URI, myWhere, whereArgs);    
                count = db.delete("favorite", myWhere, whereArgs);
                break;
                
            case TRANSITPLAN:
                count = db.delete("transitplan", where, whereArgs);
                break;
                
            case TRANSITPLAN_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");  
                count = db.delete("transitplan", myWhere, whereArgs);
                break;
                
            case BUSLINE:
                count = db.delete("busline", where, whereArgs);
                break;
                
            case BUSLINE_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");    
                count = db.delete("busline", myWhere, whereArgs);
                break;
                
            case POI:
                count = db.delete("poi", where, whereArgs);
                break;
                
            case POI_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");    
                count = db.delete("poi", myWhere, whereArgs);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        return count;
    }
    
    private void deleteHistoryDetail(Uri url, String where, String[] whereArgs) {
        // Cursor c = query(url, null, where, whereArgs, null);
        // 因为上一行的query()有Limit为20的限制，导致不能全部删除，所以用qb.query()查询表中所有行
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("history");
        qb.setProjectionMap(HISTORY_LIST_PROJECTION_MAP);
    	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, null, where, whereArgs,
                null, null, null, null);
        if (c != null) {
            int count = c.getCount();
            if (count > 0) {
                c.moveToFirst();
                int type;
                int id;
                StringBuilder s = new StringBuilder();
                for(int i = 0; i < count; i++) {
                    id = c.getInt(c.getColumnIndex(History._ID));
                    type = c.getInt(c.getColumnIndex(History.HISTORY_TYPE));
                    s.setLength(0);
                    switch (type) {

                        case History.HISTORY_TRANSFER:
                        case History.HISTORY_DRIVE:
                        case History.HISTORY_WALK:
                            s.append("(");
                            s.append(Tigerknows.TransitPlan.PARENT_ID);
                            s.append("=");
                            s.append(id);
                            s.append(") AND(");
                            s.append(Tigerknows.TransitPlan.STORE_TYPE);
                            s.append("=");
                            s.append(Tigerknows.STORE_TYPE_HISTORY);
                            s.append(")");
                            delete(Tigerknows.TransitPlan.CONTENT_URI, s.toString(), null);
                            break;
                            
                        case History.HISTORY_BUSLINE:
                            s.append("(");
                            s.append(Tigerknows.Busline.PARENT_ID);
                            s.append("=");
                            s.append(id);
                            s.append(") AND(");
                            s.append(Tigerknows.Busline.STORE_TYPE);
                            s.append("=");
                            s.append(Tigerknows.STORE_TYPE_HISTORY);
                            s.append(")");
                            delete(Tigerknows.Busline.CONTENT_URI, s.toString(), null);
                            break;

                        default:
                            break;
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }
    }
    
    private void deleteFavoriteDetail(Uri url, String where, String[] whereArgs) {
        // Cursor c = query(url, null, where, whereArgs, null);
        // 因为上一行的query()有Limit为20的限制，导致不能全部删除，所以用qb.query()查询表中所有行
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("favorite");
        qb.setProjectionMap(FAVORITE_LIST_PROJECTION_MAP);
    	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, null, where, whereArgs,
                null, null, null, null);
        if (c != null) {
            int count = c.getCount();
            if (count > 0) {
                c.moveToFirst();
                int type;
                int id;
                StringBuilder s = new StringBuilder();
                for(int i = 0; i < count; i++) {
                    id = c.getInt(c.getColumnIndex(Favorite._ID));
                    type = c.getInt(c.getColumnIndex(Favorite.FAVORITE_TYPE));
                    s.setLength(0);
                    switch (type) {
                            
                        case Favorite.FAVORITE_TRANSFER:
                        case Favorite.FAVORITE_DRIVE:
                        case Favorite.FAVORITE_WALK:
                            s.append("(");
                            s.append(Tigerknows.TransitPlan.PARENT_ID);
                            s.append("=");
                            s.append(id);
                            s.append(") AND(");
                            s.append(Tigerknows.TransitPlan.STORE_TYPE);
                            s.append("=");
                            s.append(Tigerknows.STORE_TYPE_FAVORITE);
                            s.append(")");
                            delete(Tigerknows.TransitPlan.CONTENT_URI, s.toString(), null);
                            break;
                            
                        case Favorite.FAVORITE_BUSLINE:
                            s.append("(");
                            s.append(Tigerknows.Busline.PARENT_ID);
                            s.append("=");
                            s.append(id);
                            s.append(") AND(");
                            s.append(Tigerknows.Busline.STORE_TYPE);
                            s.append("=");
                            s.append(Tigerknows.STORE_TYPE_FAVORITE);
                            s.append(")");
                            delete(Tigerknows.Busline.CONTENT_URI, s.toString(), null);
                            break;

                        default:
                            break;
                    }
                    c.moveToNext();
                }
            }
            c.close();
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (URL_MATCHER.match(uri)) {
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        long rowID;
        ContentValues values;
        String table;

        if (initialValues != null)
            values = new ContentValues(initialValues);
        else
            values = new ContentValues();
        
        Uri uri = null;
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (URL_MATCHER.match(url)) {
            case HISTORY:
                table = "history";
                
                Cursor c = db.query(table, new String[] {Tigerknows.History._ID}, null, null, null, null, "_datetime ASC");
                if (c != null) {
                    if (c.getCount() >= Tigerknows.HISTORY_MAX_SIZE) {
                        c.moveToFirst();
                        for(int i = c.getCount(); i >= Tigerknows.HISTORY_MAX_SIZE; i--) {
                        	deleteHistoryDetail(url, Tigerknows.History._ID + "=" + c.getLong(c.getColumnIndex(Tigerknows.History._ID)), null);
                            db.delete(table, Tigerknows.History._ID + "=" + c.getLong(c.getColumnIndex(Tigerknows.History._ID)), null);
                            c.moveToNext();
                        }
                    }
                    c.close();
                }
                
                rowID = db.insert(table, Tigerknows.History.DATETIME, values);
                if (rowID > 0) {
                    uri = ContentUris.withAppendedId(Tigerknows.History.CONTENT_URI, rowID);
                }
                
                break;
                
            case FAVORITE:
                table = "favorite";
                rowID = db.insert(table, Tigerknows.Favorite.DATETIME, values);
                if (rowID > 0) {
                    uri = ContentUris.withAppendedId(Tigerknows.Favorite.CONTENT_URI, rowID);
                }
                break;
                
            case TRANSITPLAN:
                table = "transitplan";
                rowID = db.insert(table, Tigerknows.TransitPlan.START, values);
                if (rowID > 0) {
                    uri = ContentUris.withAppendedId(Tigerknows.TransitPlan.CONTENT_URI, rowID);
                }
                break;
                
            case BUSLINE:
                table = "busline";
                rowID = db.insert(table, Tigerknows.Busline.BUSLINE_NAME, values);
                if (rowID > 0) {
                    uri = ContentUris.withAppendedId(Tigerknows.Busline.CONTENT_URI, rowID);
                }
                break;
                
            case POI:
                table = "poi";
                
                if (values.containsKey(Tigerknows.POI.STORE_TYPE) && values.getAsInteger(Tigerknows.POI.STORE_TYPE) == Tigerknows.STORE_TYPE_HISTORY) {
                    Cursor poiCursor = db.query(table, new String[] {Tigerknows.POI._ID}, Tigerknows.POI.STORE_TYPE + "=" + Tigerknows.STORE_TYPE_HISTORY, null, null, null, "_datetime ASC");
                    if (poiCursor != null) {
                        if (poiCursor.getCount() >= Tigerknows.HISTORY_MAX_SIZE) {
                            poiCursor.moveToFirst();
                            for(int i = poiCursor.getCount(); i >= Tigerknows.HISTORY_MAX_SIZE; i--) {
                                db.delete(table, Tigerknows.POI._ID + "=" + poiCursor.getLong(poiCursor.getColumnIndex(Tigerknows.POI._ID)), null);
                                poiCursor.moveToNext();
                            }
                        }
                        poiCursor.close();
                    }
                }
                
                rowID = db.insert(table, Tigerknows.POI.POI_NAME, values);
                if (rowID > 0) {
                    uri = ContentUris.withAppendedId(Tigerknows.POI.CONTENT_URI, rowID);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        
        return uri;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
            String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        
        String defaultSort = null;
        
        String defalutLimit = null;
        
        switch (URL_MATCHER.match(url)) {
                
            case HISTORY:
                qb.setTables("history");
                qb.setProjectionMap(HISTORY_LIST_PROJECTION_MAP);
                defaultSort = Tigerknows.History.DEFAULT_SORT_ORDER;
                defalutLimit = String.valueOf(TKConfig.getPageSize());
                break;

            case HISTORY_COUNT:
                qb.setTables("history");
                break;

            case HISTORY_ID:
                qb.setTables("history");
                qb.setProjectionMap(HISTORY_LIST_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;
                
            case POI:
                qb.setTables("poi");
                qb.setProjectionMap(POI_LIST_PROJECTION_MAP);
                defaultSort = Tigerknows.POI.DEFAULT_SORT_ORDER;
                defalutLimit = String.valueOf(TKConfig.getPageSize());
                break;

            case POI_ID:
                qb.setTables("poi");
                qb.setProjectionMap(POI_LIST_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case POI_COUNT:
                qb.setTables("poi");
                break;

            case TRANSITPLAN:
                qb.setTables("transitplan");
                qb.setProjectionMap(TRANSITPLAN_LIST_PROJECTION_MAP);
                defaultSort = Tigerknows.TransitPlan.DEFAULT_SORT_ORDER;
                break;

            case TRANSITPLAN_ID:
                qb.setTables("transitplan");
                qb.setProjectionMap(TRANSITPLAN_LIST_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;
                
            case BUSLINE:
                qb.setTables("busline");
                qb.setProjectionMap(BUSLINE_LIST_PROJECTION_MAP);
                defaultSort = Tigerknows.Busline.DEFAULT_SORT_ORDER;
                break;

            case BUSLINE_ID:
                qb.setTables("busline");
                qb.setProjectionMap(BUSLINE_LIST_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;
                
            case FAVORITE:
                qb.setTables("favorite");
                qb.setProjectionMap(FAVORITE_LIST_PROJECTION_MAP);
                defaultSort = Tigerknows.Favorite.DEFAULT_SORT_ORDER;
                defalutLimit = String.valueOf(TKConfig.getPageSize());
                break;

            case FAVORITE_COUNT:
                qb.setTables("favorite");
                break;

            case FAVORITE_ID:
                qb.setTables("favorite");
                qb.setProjectionMap(FAVORITE_LIST_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        
        String orderBy;

        if (TextUtils.isEmpty(sort))
            orderBy = defaultSort;
        else
            orderBy = sort;
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs,
                null, null, orderBy, defalutLimit);

        return c;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {

        int count = 0;
        String myWhere;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (URL_MATCHER.match(url))
        {
            case FAVORITE_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");            
                count = db.update("favorite", values, myWhere, whereArgs);
                break;
                
            case HISTORY_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");            
                count = db.update("history", values, myWhere, whereArgs);
                break;
                
            case POI_ID:
                myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");  
                count = db.update("poi", values, myWhere, whereArgs);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(url, null);
        }
        return count;
    }

    static {
        URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "history", HISTORY);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "history_count", HISTORY_COUNT);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "history/#", HISTORY_ID);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "favorite", FAVORITE);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "favorite_count", FAVORITE_COUNT);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "favorite/#", FAVORITE_ID);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "poi", POI);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "poi/#", POI_ID);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "poi_count", POI_COUNT);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "transitplan", TRANSITPLAN);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "transitplan/#", TRANSITPLAN_ID);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "busline", BUSLINE);
        URL_MATCHER.addURI(Tigerknows.AUTHORITY, "busline/#", BUSLINE_ID);
        
        HISTORY_LIST_PROJECTION_MAP = new HashMap<String, String>();
        HISTORY_LIST_PROJECTION_MAP.put(Tigerknows.History._ID, "_id");
        HISTORY_LIST_PROJECTION_MAP.put(Tigerknows.History.HISTORY_TYPE, "history_type");
        HISTORY_LIST_PROJECTION_MAP.put(Tigerknows.History.DATETIME, "_datetime");
        
        FAVORITE_LIST_PROJECTION_MAP = new HashMap<String, String>();
        FAVORITE_LIST_PROJECTION_MAP.put(Tigerknows.Favorite._ID, "_id");
        FAVORITE_LIST_PROJECTION_MAP.put(Tigerknows.Favorite.ALIAS, "_alias");
        FAVORITE_LIST_PROJECTION_MAP.put(Tigerknows.Favorite.FAVORITE_TYPE, "favorite_type");
        FAVORITE_LIST_PROJECTION_MAP.put(Tigerknows.Favorite.DATETIME, "_datetime");
        
        POI_LIST_PROJECTION_MAP = new HashMap<String, String>();
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI._ID, "_id");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.STORE_TYPE, "store_type");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.PARENT_ID, "parent_id");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.POI_NAME, "poi_name");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.ALIAS, "_alias");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.POI_X, "poi_x");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.POI_Y, "poi_y");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.POI_VERSION, "poi_version");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.DATETIME, "_datetime");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.DATA, "_data");
        POI_LIST_PROJECTION_MAP.put(Tigerknows.POI.COMMENT_DATA, "_comment_data");
        
        BUSLINE_LIST_PROJECTION_MAP = new HashMap<String, String>();
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline._ID, "_id");
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline.BUSLINE_NAME, "busline_name");
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline.BUSLINE_NUM, "busline_num");
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline.TOTAL_LENGTH, "total_length");
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline.STORE_TYPE, "store_type");
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline.PARENT_ID, "parent_id");
        BUSLINE_LIST_PROJECTION_MAP.put(Tigerknows.Busline.DATA, "_data");
        
        TRANSITPLAN_LIST_PROJECTION_MAP = new HashMap<String, String>();
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan._ID, "_id");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.TYPE, "_type");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.TIMES, "times");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.TOTAL_LENGTH, "total_length");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.START, "start");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.END, "end");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.STORE_TYPE, "store_type");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.PARENT_ID, "parent_id");
        TRANSITPLAN_LIST_PROJECTION_MAP.put(Tigerknows.TransitPlan.DATA, "_data");
    }
}

/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */
package com.tigerknows.model;

import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.SqliteWrapper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.List;

/**
 * @author pengwenyue
 * @version
 * @see
 * 
 * Change log:
 */
public class Favorite extends BaseData{
    
    private int favoriteType;
    
    private boolean isSelected;
    
    private BuslineQuery buslineQuery = null;
    
    private TrafficQuery trafficQuery = null;

    public BuslineQuery getBuslineQuery() {
		return buslineQuery;
	}
    
    public TrafficQuery getTrafficQuery() {
        return trafficQuery;
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
    
    public int getFavoriteType() {
        return favoriteType;
    }

    public String getAlise() {
        return alise;
    }

    public void setAlise(String alise) {
        this.alise = alise;
    }

    public void setFavoriteType(int favoriteType) {
        this.favoriteType = favoriteType;
    }
    
    public Uri writeToDatabases(Context context, int favoriteType) {
        ContentValues values = new ContentValues();
        this.favoriteType = favoriteType;
        values.put(Tigerknows.Favorite.FAVORITE_TYPE, this.favoriteType);
        this.dateTime = System.currentTimeMillis();
        values.put(Tigerknows.Favorite.DATETIME, this.dateTime);
        Uri uri = SqliteWrapper.insert(context, context.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, values);
        if (uri != null) {
            id = Integer.parseInt(uri.getPathSegments().get(1));
        }
        
        return uri;
    }
    
    public boolean readDetailData(Context context) {
        try {
        if (favoriteType == Tigerknows.Favorite.FAVORITE_BUSLINE) {
        	buslineQuery = new BuslineQuery(context);
            BuslineModel buslineModel = new BuslineModel();
            List<Line> lineList = Line.readFormDatabases(context, id, Tigerknows.STORE_TYPE_FAVORITE);
            buslineModel.setLineList(lineList);
            buslineQuery.setup(-1, lineList.get(0).getName(), 0, true, -1, null);
            buslineModel.setTotal(1);
            buslineModel.setType(BuslineModel.TYPE_BUSLINE);

            buslineQuery.setBuslineModel(buslineModel);
            return lineList.isEmpty() == false;
        } else {
            TrafficModel trafficModel = new TrafficModel();
            List<Plan> planList = Plan.readFormDatabases(context, id, Tigerknows.STORE_TYPE_FAVORITE);
            trafficModel.setPlanList(planList);
            trafficModel.setStart(planList.get(0).getStart());
            trafficModel.setEnd(planList.get(0).getEnd());
            trafficModel.setStartName(planList.get(0).getStart().getName());
            trafficModel.setEndName(planList.get(0).getEnd().getName());
            trafficModel.setType(TrafficModel.TYPE_PROJECT);
            int trafficType = Step.TYPE_TRANSFER;
            if (Tigerknows.History.HISTORY_TRANSFER == favoriteType) {
                trafficType = Step.TYPE_TRANSFER;
            } else if (Tigerknows.History.HISTORY_DRIVE == favoriteType) {
                trafficType = Step.TYPE_DRIVE;
            } else if (Tigerknows.History.HISTORY_WALK == favoriteType) {
                trafficType = Step.TYPE_WALK;
            }
            planList.get(0).setType(trafficType);
            trafficQuery = new TrafficQuery(context);
            trafficQuery.setTrafficModel(trafficModel);
            return planList.isEmpty() == false;
        }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从数据库中读取Favorite
     * @param id
     */
    public static Favorite readFormDatabases(Context context, long id) {
        Favorite favorite = null;
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.Favorite.CONTENT_URI, id), null, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                favorite = readFormCursor(context, c);
            }
            c.close();
        }
        return favorite;
    }

    public static Favorite readFormCursor(Context context, Cursor cursor) {
        Favorite favorite = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                favorite = new Favorite();
                favorite.id = cursor.getLong(cursor.getColumnIndex(Tigerknows.Favorite._ID));
                String str = cursor.getString(cursor.getColumnIndex(Tigerknows.Favorite.ALIAS));
                if (!TextUtils.isEmpty(str)) {
                    favorite.setAlise(str);
                }
                favorite.favoriteType = cursor.getInt(cursor.getColumnIndex(Tigerknows.Favorite.FAVORITE_TYPE));
                favorite.dateTime = cursor.getLong(cursor.getColumnIndex(Tigerknows.Favorite.DATETIME));
                if (favorite.readDetailData(context) == false) {
                    SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, "_id="+favorite.id, null);
                    favorite = null;
                }
            }
        }
        return favorite;
    }
    
    public int updateAlias(Context context) {
        int count = 0;
        ContentValues values = new ContentValues();
        values.put(Tigerknows.Favorite.ALIAS, this.alise);
        count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.Favorite.CONTENT_URI, id), values, null, null);
        return count;
    }
    
    @Override
    public boolean checkFavorite(Context context) {
        return checkStore(context);
    }

    public int deleteFavorite(Context context) {
        int count = SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, "_id="+id, null);
        this.id = -1;
        return count;
    }
    
    public boolean checkStore(Context context) {
        boolean result = false;
        if (id == -1) {
            return result;
        }
        StringBuilder s = new StringBuilder();
        s.append("(");
        s.append(com.tigerknows.provider.Tigerknows.Favorite._ID);
        s.append("=");
        s.append(id);
        s.append(")");
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, null, s.toString(), null, null);
        int count;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                result = true;
            }
            c.close();
        }
        return result;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof Favorite) {
            Favorite other = (Favorite) object;
            if (favoriteType != other.favoriteType) {
                return false;
            }
        
            if (favoriteType == com.tigerknows.provider.Tigerknows.Favorite.FAVORITE_BUSLINE) {
                Line line = buslineQuery.getBuslineModel().getLineList().get(0);
                Line otherLine = other.buslineQuery.getBuslineModel().getLineList().get(0);
                if((null != otherLine && !otherLine.equals(line)) || (null == otherLine && otherLine != line)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                Plan plan = trafficQuery.getTrafficModel().getPlanList().get(0);
                Plan otherPlan = other.trafficQuery.getTrafficModel().getPlanList().get(0);
                if((null != otherPlan && !otherPlan.equals(plan)) || (null == otherPlan && otherPlan != plan)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        
        return false;
    }
}


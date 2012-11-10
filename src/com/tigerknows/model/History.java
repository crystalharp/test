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

import java.util.List;

/**
 * @author pengwenyue
 * @version
 * @see
 * 
 * Change log:
 */
public class History extends BaseData{
    
    private int historyType;
    
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
    
    public int getHistoryType() {
        return historyType;
    }

    public void setHistoryType(int historyType) {
        this.historyType = historyType;
    }
    
    public Uri writeToDatabases(Context context, int historyType) {
        ContentValues values = new ContentValues();
        this.historyType = historyType;
        values.put(Tigerknows.History.HISTORY_TYPE, historyType);
        this.dateTime = System.currentTimeMillis();
        values.put(Tigerknows.History.DATETIME, this.dateTime);
        Uri uri = SqliteWrapper.insert(context, context.getContentResolver(), Tigerknows.History.CONTENT_URI, values);
        if (uri != null) {
            this.id = Integer.parseInt(uri.getPathSegments().get(1));
        }
        
        return uri;
    }
    
    public boolean readDetailData(Context context) {
        try {
        if (historyType == Tigerknows.History.HISTORY_BUSLINE) {
        	buslineQuery = new BuslineQuery(context);
            BuslineModel buslineModel = new BuslineModel();
            List<Line> lineList = Line.readFormDatabases(context, id, Tigerknows.STORE_TYPE_HISTORY);
            buslineModel.setLineList(lineList);
            buslineQuery.setup(-1, lineList.get(0).getName(), 0, true, -1, null);
            buslineModel.setTotal(1);
            buslineModel.setType(BuslineModel.TYPE_BUSLINE);

            buslineQuery.setBuslineModel(buslineModel);
            return lineList.isEmpty() == false;
        } else {
            TrafficModel trafficModel = new TrafficModel();
            List<Plan> planList = Plan.readFormDatabases(context, id, Tigerknows.STORE_TYPE_HISTORY);
            trafficModel.setPlanList(planList);
            trafficModel.setStart(planList.get(0).getStart());
            trafficModel.setEnd(planList.get(0).getEnd());
            trafficModel.setStartName(planList.get(0).getStart().getName());
            trafficModel.setEndName(planList.get(0).getEnd().getName());
            trafficModel.setType(TrafficModel.TYPE_PROJECT);
            int trafficType = Step.TYPE_TRANSFER;
            if (Tigerknows.History.HISTORY_TRANSFER == historyType) {
                trafficType = Step.TYPE_TRANSFER;
            } else if (Tigerknows.History.HISTORY_DRIVE == historyType) {
                trafficType = Step.TYPE_DRIVE;
            } else if (Tigerknows.History.HISTORY_WALK == historyType) {
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
     * 从数据库中读取History
     * @param id
     */
    public static History readFormDatabases(Context context, long id) {
        History history = null;
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.History.CONTENT_URI, id), null, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                c.moveToFirst();
                history = readFormCursor(context, c);
            }
            c.close();
        }
        return history;
    }

    public static History readFormCursor(Context context, Cursor cursor) {
        History history = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                history = new History();
                history.id = cursor.getLong(cursor.getColumnIndex(Tigerknows.History._ID));
                history.historyType = cursor.getInt(cursor.getColumnIndex(Tigerknows.History.HISTORY_TYPE));
                history.dateTime = cursor.getLong(cursor.getColumnIndex(Tigerknows.History.DATETIME));
                if (history.readDetailData(context) == false) {
                    SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.History.CONTENT_URI, "_id="+history.id, null);
                    history = null;
                }
            }
        }
        return history;
    }
    
    @Override
    public BaseData checkStore(Context context, int storeType) {
        return checkStore(context);
    }
    
    private BaseData checkStore(Context context) {
        BaseData baseData = null;
        StringBuilder s = new StringBuilder();
        s.append("(");
        s.append(com.tigerknows.provider.Tigerknows.History._ID);
        s.append("=");
        s.append(id);
        s.append(")");
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), Tigerknows.History.CONTENT_URI, null, s.toString(), null, null);
        int count;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                c.moveToFirst();
                History other = readFormCursor(context, c);
                if((null != other && !other.equals(this)) || (null == other && other != this)) {
                } else {
                    baseData = other;
                }
            }
            c.close();
        }
        return baseData;
    }
    
    public int updateHistory(Context context) {
        int count = 0;
        ContentValues values = new ContentValues();
        this.dateTime = System.currentTimeMillis();
        values.put(Tigerknows.History.DATETIME, this.dateTime);
        count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.History.CONTENT_URI, id), values, null, null);
        return count;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof History) {
            History other = (History) object;
            if (historyType != other.historyType) {
                return false;
            }
        
            if (historyType == com.tigerknows.provider.Tigerknows.History.HISTORY_BUSLINE) {
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


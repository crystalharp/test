package com.tigerknows.model;


import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.provider.Tigerknows.Favorite;
import com.tigerknows.provider.Tigerknows.History;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.SqliteWrapper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * BuslineModel
 * 		|
 * 		|____EMPTY
 * 		|
 * 		|____LINE
 * 		|	   |
 * 		|	   |
 * 		|____STATION	
 * 
 * 
 * 表示公交线路查询结果的数据结构
 * @author pengwenyue
 *
 */

public class BuslineModel extends XMapData {
    
    // 0表示空结果
    public static final int TYPE_EMPTY = 0;
    
    // 1有公交线路
    public static final int TYPE_BUSLINE = 1;
    
    // 2表示只有公交站点
    public static final int TYPE_STATION = 2;

    // 3表示当前城市不支持公交线路查询
    public static final int TYPE_UNSUPPORT = 3;
    
    // 0x00 x_int 结果类型，0表示空结果，1有公交线路，2表示只有公交站点
    private static final byte FIELD_TYPE = 0x00;

    // 0x01 x_int 查询到的线路数量或公交站数量（可能比本次结果集大）
    private static final byte FIELD_TOTAL = 0x01;

    // 0x10 x_array<x_map> 线路结果，每个map　是一条线路（类型为１时才有）
    private static final byte FIELD_LINE = 0x10;

    public static class Line extends BaseData {
        // 0x01 x_int 线路总长度
        private static final byte FIELD_LENGTH = 0x01;

        // 0x02 x_array<int> 线路点序列的x坐标（增量方式存储）
        private static final byte FIELD_X = 0x02;

        // 0x03 x_array<int> 线路点序列的y坐标（增量方式存储）
        private static final byte FIELD_Y = 0x03;

        // 0x10 x_string 线路名称
        private static final byte FIELD_NAME = 0x10;
        
        // 0x11	x_string 线路运营时间
        private static final byte FIELD_TIME = 0x11;

        // 0x20 x_array<x_map> 线路经过的车站，　每一个map是一个车站
        private static final byte FIELD_STATION = 0x20;

        private int length;
        private List<Long> x;
        private List<Long> y;
        private String name;
        private String time;
        private List<Station> stationList;
        private List<Position> positionList;
        
        public void setPositionList(List<Position> positionList) {
            if (positionList == null) {
                return;
            }
            this.positionList = (ArrayList<Position>)positionList;
            this.x = new ArrayList<Long>();
            this.y = new ArrayList<Long>();
            int i = 0;
            long lon = 0;
            long lat = 0;
            long previousLon = 0;
            long previousLat = 0;
            for(Position position : positionList) {
                if (i == 0) {
                    lon = (long)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
                    lat = (long)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
                    previousLon = lon;
                    previousLat = lat;
                } else {
                    lon = previousLon - (long)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
                    lat = previousLat - (long)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
                    previousLon = (long)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
                    previousLat = (long)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
                }
                this.x.add(lon);
                this.y.add(lat);
            }
        }
        
        public List<Position> getPositionList() {
            return positionList;
        }
        
        public int getLength() {
            return length;
        }
        
        public String getLengthStr(Context mContext) {
        	return ShareTextUtil.getPlanLength(mContext, length);
        }

        public void setLength(int length) {
            this.length = length;
        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Station> getStationList() {
            return stationList;
        }

        public String getTime() {
			return time;
		}

		public void setTime(String time) {
			this.time = time;
		}

		public void setStationList(List<Station> stationList) {
            this.stationList = stationList;
        }
        
        public Line() {
        }
        
        public Line(XMap data) throws APIException {
            super(data);
            init(data, true);
        }

        @SuppressWarnings("unchecked")
        public void init(XMap data, boolean reset) throws APIException {
            super.init(data, reset);
            length = (int)getLongFromData(FIELD_LENGTH, reset ? 0 : this.length);

            if (this.data.containsKey(FIELD_X) && this.data.containsKey(FIELD_Y)) {
                this.x = this.data.getXArray(FIELD_X).toIntList();
                this.y = this.data.getXArray(FIELD_Y).toIntList();
            
                if (this.x.size() == this.y.size()) {
                    int i = 0;
                    Position position;
                    this.positionList = new ArrayList<Position>(this.x.size());
                    double lon = 0d;
                    double lat = 0d;
                    for(long x : this.x) {
                        if (i == 0) {
                            position = new Position(long2doubleForLatLon(this.y.get(i)), long2doubleForLatLon(x));
                            lon = position.getLon();
                            lat = position.getLat();
                        } else {
                            position = new Position(lat + long2doubleForLatLon(this.y.get(i)), lon + long2doubleForLatLon(x));
                            lon = position.getLon();
                            lat = position.getLat();
                        }
                        this.positionList.add(position);
                        i++;
                    }
                }
            } else if (reset) {
                if (this.positionList != null) {
                    this.positionList.clear();
                }
            }
            
            name = getStringFromData(FIELD_NAME, reset ? null : this.name);
            time = getStringFromData(FIELD_TIME, reset ? null : this.time);
            
            this.stationList = getListFromData(FIELD_STATION, Station.Initializer, reset ? null : this.stationList);
        }

        public XMap getData() {
            if (this.data == null) {
                this.data = new XMap();
                
                this.data.put(FIELD_TYPE, this.length);
                if (this.x != null && this.y != null) {
                    this.data.put(FIELD_X, XArray.fromIntList(this.x));
                    this.data.put(FIELD_Y, XArray.fromIntList(this.y));
                }
                this.data.put(FIELD_NAME, this.name);
                this.data.put(FIELD_TIME, this.time);
                if (this.stationList != null) {
                    XArray<XMap> statcions = new XArray<XMap>();
                    for(Station station : this.stationList) {
                        statcions.add(station.getData());
                    }
                    this.data.put(FIELD_STATION, statcions);
                }
            }
            
            return this.data;
        }
        
        public Uri writeToDatabases(Context context, long parentId, int storeType) {
            boolean isFailed = false;
            ContentValues values = new ContentValues();
            if (!TextUtils.isEmpty(name)) {
                values.put(Tigerknows.Busline.BUSLINE_NAME, name);
            }
            values.put(Tigerknows.Busline.TOTAL_LENGTH, length);
            values.put(Tigerknows.Busline.BUSLINE_NUM, this.stationList.size());
            try {
                byte[] data = ByteUtil.xobjectToByte(getData());
                if (data.length > 0) {
                    values.put(Tigerknows.Busline.DATA, data);
                }
            } catch (Exception e) {
                isFailed = true;
            }
            
            Uri uri = null;
            if (!isFailed) {
                uri = writeToDatabasesInternal(context, Tigerknows.Busline.CONTENT_URI, values, storeType, 
                        (storeType == Tigerknows.STORE_TYPE_FAVORITE) ? Favorite.FAVORITE_BUSLINE : History.HISTORY_BUSLINE);
            }
            
            return uri;
        }

        /**
         * 从数据库中读取Line
         * 
         * @param id
         */
        public static List<Line> readFormDatabases(Context context, long parentId, int storeType) {
            List<Line> lineList = new ArrayList<Line>();
            Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                    Tigerknows.Busline.CONTENT_URI, null, "(" + Tigerknows.Busline.PARENT_ID + "="
                            + parentId + ") AND (" + Tigerknows.Busline.STORE_TYPE + "="
                            + storeType + ")", null, null);
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    for (int i = 0; i < c.getCount(); i++) {
                        lineList.add(readFormCursor(context, c));
                        c.moveToNext();
                    }
                }
                c.close();
            }
            return lineList;
        }

        public static Line readFormCursor(Context context, Cursor c) {
            Line line = new Line();
            String str;
            if (c != null) {
                line.id = c.getLong(c.getColumnIndex(Tigerknows.TransitPlan._ID));
                line.parentId = c.getLong(c.getColumnIndex(Tigerknows.TransitPlan.PARENT_ID));
                line.storeType = c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.STORE_TYPE));
                str = c.getString(c.getColumnIndex(Tigerknows.Busline.BUSLINE_NAME));
                if (!TextUtils.isEmpty(str)) {
                    line.setName(str);
                }
                line.setLength(c.getInt(c.getColumnIndex(Tigerknows.Busline.TOTAL_LENGTH)));
                byte[] data = c.getBlob(c.getColumnIndex(Tigerknows.Busline.DATA));
                try {
                    XMap xmap = (XMap) ByteUtil.byteToXObject(data);
                    line.init(xmap, true);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return line;
        }

        @Override
        public Line checkStore(Context context, int store_Type) {
            Line line = null;
            //运营时间是否也要判断?
            Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                    Tigerknows.Busline.CONTENT_URI, null, "(" + Tigerknows.Busline.BUSLINE_NAME
                            + "='" + name.replace("'", "''") + "') AND ("
                            + Tigerknows.Busline.TOTAL_LENGTH + "=" + length
                            + ") AND (" + Tigerknows.Busline.STORE_TYPE + "=" + store_Type + ")",
                    null, null);
            if (c != null) {
                int count = c.getCount();
                if (count > 0) {
                    c.moveToFirst();
                    for(int i = 0; i < count; i++) {
                        Line other = readFormCursor(context, c);
                        if((null != other && !other.equals(this)) || (null == other && other != this)) {
                        } else {
                            line = other;
                            break;
                        }
                        c.moveToNext();
                    }
                }
                c.close();
            }
            return line;
        }
        
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            
            if (object instanceof Line) {
                Line other = (Line) object;
                if((null != other.data && !other.data.equals(data)) || (null == other.data && other.data != data)) {
                    return false;
                } else {
                    return true;
                }
            }
            
            return false;
        }
        
        public String getSMSString(Context context) {
            StringBuilder body = new StringBuilder();
            body.append(context.getString(R.string.busline_, name));
            body.append(context.getString(R.string.busline_line_listitem_title, time, Utility.meter2kilometre(length)));//, stationList.size()));
            body.append("\n");
            int i = 0;
            for(Station station : stationList) {
                if (i > 0) {
                    body.append(";");
                }
                body.append(i+1);
                body.append(".");
                body.append(station.getName());
                i++;
            }
            return body.toString();
        }

        public void updateHistory(Context context) {
            BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_HISTORY);
            if (baseData != null) {
                com.tigerknows.model.History history = com.tigerknows.model.History.readFormDatabases(context, baseData.parentId);
                if (history != null) {
                    history.updateHistory(context);
                }
            } else {
                try {
                    Line line = new Line();
                    line.init(getData(), true);
                    line.writeToDatabases(context, -1, Tigerknows.STORE_TYPE_HISTORY);
                } catch (APIException e) {
                    // TODO: handle exception
                }
            }
        }

        public static XMapInitializer<Line> Initializer = new XMapInitializer<Line>() {

            @Override
            public Line init(XMap data) throws APIException {
                return new Line(data);
            }
        };
    }

    // 0x20 x_array<x_map> 站点结果，每个map　是一个公交站（类型为２时才有）
    private static final byte FIELD_STATION = 0x20;

    public static class Station extends BaseData {

        // 0x01 x_int 公交站在线路上的位置,只有在结果类型为1时才有
        private static final byte FIELD_INDEX = 0x01;

        // 0x02 x_int x坐标
        private static final byte FIELD_X = 0x02;

        // 0x03 x_int y坐标
        private static final byte FIELD_Y = 0x03;

        // 0x10 x_string 公交站名称
        private static final byte FIELD_NAME = 0x10;

        // 0x20 x_array<x_string> 经过公交站的线路，　只有在结果类型为２时才有
        private static final byte FIELD_LINE = 0x20;
        
        public static final int TOTAL_LENGTH = -1;
        
        private int index;
        private long x;
        private long y;
        private String name;
        private List<String> lineList;
        private Position position;
        
        public Position getPosition() {
            return this.position;
        }
        
        public void setPosition(Position position) {
            if (!Util.inChina(position)) {
                return;
            }
            
            this.x = (long)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
            this.y = (long)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
            this.position = position;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getLineList() {
            return lineList;
        }

        public void setLineList(List<String> lineList) {
            this.lineList = lineList;
        }
        
        public Station() {
        }

        public Station(XMap data) throws APIException {
            super(data);
            init(data, true);
        }

        @SuppressWarnings("unchecked")
        public void init(XMap data, boolean reset) throws APIException {
            super.init(data, reset);
            
            index = (int)getLongFromData(FIELD_INDEX, reset ? 0 : index);
            x = (int)getLongFromData(FIELD_X, reset ? 0 : x);
            y = (int)getLongFromData(FIELD_Y, reset ? 0 : y);
            position = getPositionFromData(FIELD_X, FIELD_Y, reset ? null : position);
            name = getStringFromData(FIELD_NAME, reset ? null : name);
            if (this.data.containsKey(FIELD_LINE)) {
                this.lineList = this.data.getXArray(FIELD_LINE).toStringList();
            } else if (reset) {
                if (this.lineList != null) {
                    this.lineList.clear();
                }
            }
        }

        public POI toPOI() {
            POI poi = new POI();
            poi.setName(name);
            if (this.position == null) {
                this.position = new Position(long2doubleForLatLon(y), long2doubleForLatLon(x));
            }
            poi.setPosition(this.position);
            return poi;
        }
        
        public XMap getData() {
            if (this.data == null) {
                this.data = new XMap();
                this.data.put(FIELD_INDEX, this.index);
                if (!TextUtils.isEmpty(this.name)) {
                    this.data.put(FIELD_NAME, this.name);
                }
                this.data.put(FIELD_X, this.x);
                this.data.put(FIELD_Y, this.y);
                
                if (this.lineList != null) {
                    this.data.put(FIELD_LINE, XArray.fromStringList(this.lineList));
                }
            }
            
            return this.data;
        }
        
        public String getSMSString(Context context) {
            StringBuilder body = new StringBuilder();
            body.append(name);
            body.append(context.getString(R.string.busline_station_listitem_title, lineList.size()));
            body.append("\n");
            int i = 0;
            for(String lineName : lineList) {
                if (i > 0) {
                    body.append(";");
                }
                body.append(lineName);
                i++;
            }
            return body.toString();
        }

        public static XMapInitializer<Station> Initializer = new XMapInitializer<Station>() {

            @Override
            public Station init(XMap data) throws APIException {
                return new Station(data);
            }
        };
    }
    
    private int type;
    private List<Line> lineList;
    private List<Station> stationList;    
    private int total;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Line> getLineList() {
        return lineList;
    }

    public void setLineList(List<Line> lineList) {
        this.lineList = (List<Line>)lineList;
    }

    public List<Station> getStationList() {
        return stationList;
    }

    public void setStationList(List<Station> stationList) {
        this.stationList = stationList;
    }
    
    public BuslineModel() {
        
    }
    
    public BuslineModel(XMap data) throws APIException {
        super(data);
        type = (int)getLongFromData(FIELD_TYPE);
        total = (int)getLongFromData(FIELD_TOTAL);
        lineList = getListFromData(FIELD_LINE, Line.Initializer);
        stationList = getListFromData(FIELD_STATION, Station.Initializer);
    }
}

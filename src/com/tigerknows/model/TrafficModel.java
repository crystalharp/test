package com.tigerknows.model;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.model.xobject.XObject;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.provider.Tigerknows.Favorite;
import com.tigerknows.provider.Tigerknows.History;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.NavigationSplitJointRule;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.SqliteWrapper;

/**
 * TrafficModel
 * 		|
 * 		|____EMPTY
 * 		|
 * 		|____PROJECT
 * 		|		|
 * 		|		|____BUS
 * 		|		|
 * 		|		|____DRIVE
 * 		|		|
 * 		|		|____WALK
 * 		|		|
 * 		|		|____TRANSFER
 * 		|				|
 * 		|				|____WALK
 * 		|				|
 * 		|				|____BUS
 * 		|
 * 		|____ALTERNATIVE
 * 
 * 
 * @author linqingzu
 *
 */
public class TrafficModel extends XMapData {

    // 0表示空结果
    public static final int TYPE_EMPTY = 0;

    // 1表示交通方案
    public static final int TYPE_PROJECT = 1;

    // 2表示备选站点
    public static final int TYPE_ALTERNATIVES = 2;
    
    // 0x00 x_int 结果类型，0表示空结果，1表示交通方案，2表示备选站点
    private static final byte FIELD_TYPE = 0x00;

    // 0x01 x_string 起点名称
    private static final byte FIELD_START_NAME = 0x01;

    // 0x02 x_string 终点名称
    private static final byte FIELD_END_NAME = 0x02;

    // 0x10 x_array<x_map> array<交通方案>
    private static final byte FIELD_PROJECT_LIST = 0x10;
    
    public static class Plan extends BaseData {     

        // 0x01 x_string 方案描述
        private static final byte FIELD_DESCRIPTION = 0x01;

        // 0x02 x_int 全程长度（单位为米）
        private static final byte FIELD_LENGTH = 0x02;

        // 0x03 x_array<x_map> array<交通步骤>
        private static final byte FIELD_STEP_LIST = 0x03;

        // 0x10 x_string 出发方向
        private static final byte FIELD_START_OUT_ORIENTATION = 0x10;
        
        // 0x11    x_string    预计时间(驾车类)
        private static final byte FIELD_EXPECTED_DRIVE_TIME = 0x11;
        // 0x12    x_string    全程距离(驾车类)
        private static final byte FIELD_DRIVE_DISTANCE = 0x12;
        // 0x13    x_string    红绿灯数(驾车类)
        private static final byte FIELD_TRAFFIC_LIGHT_NUM = 0x13;
        // 0x14    x_string    打车费用(驾车类)
        private static final byte FIELD_TAXI_COST = 0x14;
        // 0x21    x_string    预计时间(公交类)
        private static final byte FIELD_EXPECTED_BUS_TIME = 0x21;
        // 0x22    x_string    全程长度(公交类)
        private static final byte FIELD_BUS_DISTANCE = 0x22;
        // 0x23    x_string    总步行距离(公交类)
        private static final byte FIELD_WALK_DISTANCE = 0x23;
        // 0x24    x_string    公交站数(公交类) 
        private static final byte FIELD_BUSSTOP_NUM = 0x24;
        // 0x30    x_array<x_map>  array<方案标签>(公交类)
        private static final byte FIELD_PLAN_TAG = 0x30;
        
        public static final int TRANSFER_WALK_MIN_DISTANCE = 50;

        public static class Step extends XMapData {

            public static final int TYPE_WALK = 1;
            public static final int TYPE_TRANSFER = 2;
            public static final int TYPE_DRIVE = 3;
            
            // 添加这些标志用于在界面上根据标志来显示相应的图标
            public static final int TYPE_FORWARD = 11;
            public static final int TYPE_BACK = 12;
            public static final int TYPE_LEFT = 13;
            public static final int TYPE_ROUND_LEFT = 14;
            public static final int TYPE_RIGHT = 15;
            public static final int TYPE_ROUND_RIGHT = 16;
            
            // 公交换乘方案中用于标志某一线路是地铁或公交
            public static final int TYPE_LINE_SUBWAY = 0;
            public static final int TYPE_LINE_BUS = 1;
            
            // 0x00 x_int 类型 1为步行；2为公交；3为驾车
            private static final byte FIELD_TYPE = 0x00;

            // 0x01 x_int 距离（单位为米）
            private static final byte FIELD_DISTANCE = 0x01;

            // 0x02 x_array<int> x坐标（增量方式存储）
            private static final byte FIELD_X = 0x02;

            // 0x03 x_array<int> y坐标（增量方式存储）
            private static final byte FIELD_Y = 0x03;

            // 0x10 x_int 附加人行信息，int值表示人行设施的类型
            private static final byte FIELD_WALK_PUBLIC_NUDITY = 0x10;

            // 0x11 x_string 当前道路的名称
            private static final byte FIELD_WALK_ROAD_NAME = 0x11;

            // 0x12 x_string 转向信息
            private static final byte FIELD_WALK_TURN_TO = 0x12;

            // 0x13 x_int 是否属于环岛
            private static final byte FIELD_WALK_WHETHER_ROUNDABOUT = 0x13;
            
            // 0x20 x_string 上车站名称
            private static final byte FIELD_TRANSFER_UP_STOP_NAME = 0x20;

            // 0x21 x_string 下车站名称
            private static final byte FIELD_TRANSFER_DOWN_STOP_NAME = 0x21;

            // 0x22 x_string 前一站名称
            private static final byte FIELD_TRANSFER_PREVIOUS_STOP_NAME = 0x22;

            // 0x23 x_string 经过站数量
            private static final byte FIELD_TRANSFER_STOP_NUMBER = 0x23;

            // 0x24 x_string 线路名称
            private static final byte FIELD_TRANSFER_LINE_NAME = 0x24;

            // 0x25 x_string 始发站名称
            private static final byte FIELD_TRANSFER_START_STOP_NAME = 0x25;

            // 0x26 x_string 终点站名称
            private static final byte FIELD_TRANSFER_END_STOP_NAME = 0x26;

            // 0x27 x_string 票价信息
            private static final byte FIELD_TRANSFER_TICKET_PRICE = 0x27;
            
            // 0x28 x_string 运营时间
            private static final byte FIELD_TRANSFER_OPERATION_TIME = 0x28;

            // 0x29 x_string 运营公司
            private static final byte FIELD_TRANSFER_OPERATION_COMPANY = 0x29;

            // 0x30 x_string 当前道路名称
            private static final byte FIELD_DRIVE_ROAD_NAME = 0x30;

            // 0x31 x_string 转向信息
            private static final byte FIELD_DRIVE_TURN_TO = 0x31;

            // 0x32 x_string 高速出入口信息
            private static final byte FIELD_DRIVE_GANGWAY = 0x32;

            // 0x33 x_int 是否属于环岛
            private static final byte FIELD_DRIVE_WHETHER_ROUNDABOUT = 0x33;

            // 0x34 x_int 是否收费路段
            private static final byte FIELD_DRIVE_WHETHER_CHARGE = 0x34;

            // 0x35 x_string 匝道信息
            private static final byte FIELD_DRIVE_RAMP = 0x35;

            // 0x36 x_string 高速路方向信息
            private static final byte FIELD_DRIVE_ORIENTATION = 0x36;
            
            // 0x37 x_int 当前线路是公交还是地铁
            private static final byte FIELD_LINE_TYPE = 0x37;
            
            // 0x38 x_string 进入的地铁口信息
            private static final byte FIELD_SUBWAY_ENTRANCE = 0x38;
            
            //类型
            private int type;

            //距离
            private int distance;

            //x坐标
            private List<Long> x;

            //y坐标
            private List<Long> y;
            
            private List<Position> positionList;

            //人行设施的类型
            private int walkPublicNudity;

            //当前道路的名称
            private String walkRoadName;
            
            //转向信息
            private String walkTurnTo;

            //是否属于环岛
            private int walkWhetherRoundabout;

            //上车站名称
            private String transferUpStopName;

            //下车站名称
            private String transferDownStopName;

            //前一站名称
            private String transferPreviousStopName;

            //经过站数量
            private int transferStopNumber;

            //线路名称
            private String transferLineName;

            //始发站名称
            private String transferStartStopName;

            //终点站名称
            private String transferEndStopName;

            //运营时间
            private String transferOperationTime;

            //票价信息
            private String transferTicketPrice;

            //运营公司
            private String transferOperationCompany;

            //当前道路名称
            private String driveRoadName;

            //转向信息
            private String driveTurnTo;

            //高速出入口信息
            private String driveGangway;

            //是否属于环岛
            private int driveWhetherRoundabout;

            //是否收费路段
            private int driveWhetherCharge;

            //匝道信息
            private String driveRamp;

            //高速路方向信息
            private String driveOrientation;
            
            // 当前线路类型
            private int lineType = TYPE_LINE_BUS;
            
            // 进出地铁口信息
            private String subwayEntrance;
            
            public int getType() {
                return type;
            }

            public void setType(int type) {
                this.type = type;
            }

            public int getDistance() {
                return distance;
            }

            public void setDistance(int distance) {
                this.distance = distance;
            }
            
            public void setPositionList(List<Position> positionList) {
                if (positionList == null) {
                    return;
                }
                this.positionList = positionList;
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

            public int getWalkPublicNudity() {
                return walkPublicNudity;
            }

            public void setWalkPublicNudity(int walkPublicNudity) {
                this.walkPublicNudity = walkPublicNudity;
            }

            public String getWalkRoadName() {
                return walkRoadName;
            }

            public void setWalkRoadName(String walkRoadName) {
                this.walkRoadName = walkRoadName;
            }

            public String getWalkTurnTo() {
                return walkTurnTo;
            }

            public void setWalkTurnTo(String walkTurnTo) {
                this.walkTurnTo = walkTurnTo;
            }

            public int getWalkWhetherRoundabout() {
                return walkWhetherRoundabout;
            }

            public void setWalkWhetherRoundabout(int walkWhetherRoundabout) {
                this.walkWhetherRoundabout = walkWhetherRoundabout;
            }

            public String getTransferUpStopName() {
                return transferUpStopName;
            }

            public void setTransferUpStopName(String transferUpStopName) {
                this.transferUpStopName = transferUpStopName;
            }

            public String getTransferDownStopName() {
                return transferDownStopName;
            }

            public void setTransferDownStopName(String transferDownStopName) {
                this.transferDownStopName = transferDownStopName;
            }

            public String getTransferPreviousStopName() {
                return transferPreviousStopName;
            }

            public void setTransferPreviousStopName(String transferPreviousStopName) {
                this.transferPreviousStopName = transferPreviousStopName;
            }

            public int getTransferStopNumber() {
                return transferStopNumber;
            }

            public void setTransferStopNumber(int transferStopNumber) {
                this.transferStopNumber = transferStopNumber;
            }

            public String getTransferLineName() {
                return transferLineName;
            }

            public void setTransferLineName(String transferLineName) {
                this.transferLineName = transferLineName;
            }

            public String getTransferStartStopName() {
                return transferStartStopName;
            }

            public void setTransferStartStopName(String transferStartStopName) {
                this.transferStartStopName = transferStartStopName;
            }

            public String getTransferEndStopName() {
                return transferEndStopName;
            }

            public void setTransferEndStopName(String transferEndStopName) {
                this.transferEndStopName = transferEndStopName;
            }

            public String getTransferOperationTime() {
                return transferOperationTime;
            }

            public void setTransferOperationTime(String transferOperationTime) {
                this.transferOperationTime = transferOperationTime;
            }

            public String getTransferTicketPrice() {
                return transferTicketPrice;
            }

            public void setTransferTicketPrice(String transferTicketPrice) {
                this.transferTicketPrice = transferTicketPrice;
            }

            public String getTransferOperationCompany() {
                return transferOperationCompany;
            }

            public void setTransferOperationCompany(String transferOperationCompany) {
                this.transferOperationCompany = transferOperationCompany;
            }

            public String getDriveRoadName() {
                return driveRoadName;
            }

            public void setDriveRoadName(String driveRoadName) {
                this.driveRoadName = driveRoadName;
            }

            public String getDriveTurnTo() {
                return driveTurnTo;
            }

            public void setDriveTurnTo(String driveTurnTo) {
                this.driveTurnTo = driveTurnTo;
            }

            public String getDriveGangway() {
                return driveGangway;
            }

            public void setDriveGangway(String driveGangway) {
                this.driveGangway = driveGangway;
            }

            public int getDriveWhetherRoundabout() {
                return driveWhetherRoundabout;
            }

            public void setDriveWhetherRoundabout(int driveWhetherRoundabout) {
                this.driveWhetherRoundabout = driveWhetherRoundabout;
            }

            public int getDriveWhetherCharge() {
                return driveWhetherCharge;
            }

            public void setDriveWhetherCharge(int driveWhetherCharge) {
                this.driveWhetherCharge = driveWhetherCharge;
            }

            public String getDriveRamp() {
                return driveRamp;
            }

            public void setDriveRamp(String driveRamp) {
                this.driveRamp = driveRamp;
            }

            public String getDriveOrientation() {
                return driveOrientation;
            }

            public void setDriveOrientation(String driveOrientation) {
                this.driveOrientation = driveOrientation;
            }

            public int getLineType() {
				return lineType;
			}

			public void setLineType(int lineType) {
				this.lineType = lineType;
			}
			
			public String getSubwayEntrance() {
			    return subwayEntrance;
			}
			
			public void setSubwayEntrance(String s) {
			    this.subwayEntrance = s;
			}

			@SuppressWarnings("unchecked")
            public Step(XMap data) throws APIException {
                super(data);

                type = (int)getLongFromData(FIELD_TYPE);
                distance = (int)getLongFromData(FIELD_DISTANCE);
                //服务器数据过滤不完全导致出现0米的路段，为了兼容将其改为1米
                if (distance == 0) { distance = 1; }

                if (this.data.containsKey(FIELD_X) && this.data.containsKey(FIELD_Y)) {
                    this.x = this.data.getXArray(FIELD_X).toIntList();
                    this.y = this.data.getXArray(FIELD_Y).toIntList();
                    
                    if (this.x.size() == this.y.size()) {
                        int i = 0;
                        Position position;
                        this.positionList = new ArrayList<Position>(this.x.size());
                        double lon = 0;
                        double lat = 0;
                        for(long x : this.x) {
                            if (i == 0) {
                                position = new Position(long2doubleForLatLon(this.y.get(i)), long2doubleForLatLon(x));
                                lat = position.getLat();
                                lon = position.getLon();
                            } else {
                                position = new Position(lat + long2doubleForLatLon(this.y.get(i)), lon + long2doubleForLatLon(x));
                                lat = position.getLat();
                                lon = position.getLon();
                            }
                            this.positionList.add(position);
                            i++;
                        }
                    }
                }

                if (type == TYPE_WALK) {
                    walkPublicNudity = (int)getLongFromData(FIELD_WALK_PUBLIC_NUDITY);
                    walkRoadName = getStringFromData(FIELD_WALK_ROAD_NAME);
                    walkTurnTo = getStringFromData(FIELD_WALK_TURN_TO);
                    walkWhetherRoundabout = (int)getLongFromData(FIELD_WALK_WHETHER_ROUNDABOUT);
                    subwayEntrance = getStringFromData(FIELD_SUBWAY_ENTRANCE);
                } else if (type == TYPE_TRANSFER) {
                    transferUpStopName = getStringFromData(FIELD_TRANSFER_UP_STOP_NAME);
                    transferDownStopName = getStringFromData(FIELD_TRANSFER_DOWN_STOP_NAME);
                    transferPreviousStopName = getStringFromData(FIELD_TRANSFER_PREVIOUS_STOP_NAME);
                    transferStopNumber = (int)getLongFromData(FIELD_TRANSFER_STOP_NUMBER);
                    transferLineName = getStringFromData(FIELD_TRANSFER_LINE_NAME);
                    transferStartStopName = getStringFromData(FIELD_TRANSFER_START_STOP_NAME);
                    transferEndStopName = getStringFromData(FIELD_TRANSFER_END_STOP_NAME);
                    transferOperationTime = getStringFromData(FIELD_TRANSFER_OPERATION_TIME);
                    transferTicketPrice = getStringFromData(FIELD_TRANSFER_TICKET_PRICE);
                    transferOperationCompany = getStringFromData(FIELD_TRANSFER_OPERATION_COMPANY);
                    lineType = (int)getLongFromData(FIELD_LINE_TYPE);
                    subwayEntrance = getStringFromData(FIELD_SUBWAY_ENTRANCE);
                } else if (type == TYPE_DRIVE) {
                    driveRoadName = getStringFromData(FIELD_DRIVE_ROAD_NAME);
                    driveTurnTo = getStringFromData(FIELD_DRIVE_TURN_TO);
                    driveGangway = getStringFromData(FIELD_DRIVE_GANGWAY);
                    driveWhetherRoundabout = (int)getLongFromData(FIELD_DRIVE_WHETHER_ROUNDABOUT);
                    driveWhetherCharge = (int)getLongFromData(FIELD_DRIVE_WHETHER_CHARGE);
                    driveRamp = getStringFromData(FIELD_DRIVE_RAMP);
                    driveOrientation = getStringFromData(FIELD_DRIVE_ORIENTATION);
                }
            }
            
            public XMap getData() {
                if (this.data == null) {
                    this.data = new XMap();
                    
                    this.data.put(FIELD_TYPE, type);
                    this.data.put(FIELD_DISTANCE, distance);
                    
                    if (this.x != null && this.x.size() > 0) {
                        XArray<XObject> xarray = new XArray<XObject>();
                        for(long i : this.x) {
                            xarray.add(XObject.valueOf(i));
                        }
                        this.data.put(FIELD_X, xarray);
                    }
                    
                    if (this.y != null && this.y.size() > 0) {
                        XArray<XObject> xarray = new XArray<XObject>();
                        for(long i : this.y) {
                            xarray.add(XObject.valueOf(i));
                        }
                        this.data.put(FIELD_Y, xarray);
                    }

                    if (type == TYPE_WALK) {
                        this.data.put(FIELD_WALK_PUBLIC_NUDITY, walkPublicNudity);
                        if (!TextUtils.isEmpty(walkRoadName)) {
                            this.data.put(FIELD_WALK_ROAD_NAME, walkRoadName);
                        }
                        if (!TextUtils.isEmpty(walkTurnTo)) {
                            this.data.put(FIELD_WALK_TURN_TO, walkTurnTo);
                        }
                        this.data.put(FIELD_WALK_WHETHER_ROUNDABOUT, walkWhetherRoundabout);
                    } else if (type == TYPE_TRANSFER) {
                        if (!TextUtils.isEmpty(transferUpStopName)) {
                            this.data.put(FIELD_TRANSFER_UP_STOP_NAME, transferUpStopName);
                        }
                        if (!TextUtils.isEmpty(transferDownStopName)) {
                            this.data.put(FIELD_TRANSFER_DOWN_STOP_NAME, transferDownStopName);
                        }
                        if (!TextUtils.isEmpty(transferPreviousStopName)) {
                            this.data.put(FIELD_TRANSFER_PREVIOUS_STOP_NAME, transferPreviousStopName);
                        }
                        this.data.put(FIELD_TRANSFER_STOP_NUMBER, transferStopNumber);
                        if (!TextUtils.isEmpty(transferLineName)) {
                            this.data.put(FIELD_TRANSFER_LINE_NAME, transferLineName);
                        }
                        if (!TextUtils.isEmpty(transferStartStopName)) {
                            this.data.put(FIELD_TRANSFER_START_STOP_NAME, transferStartStopName);
                        }
                        if (!TextUtils.isEmpty(transferEndStopName)) {
                            this.data.put(FIELD_TRANSFER_END_STOP_NAME, transferEndStopName);
                        }
                        if (!TextUtils.isEmpty(transferOperationTime)) {
                            this.data.put(FIELD_TRANSFER_OPERATION_TIME, transferOperationTime);
                        }
                        if (!TextUtils.isEmpty(transferTicketPrice)) {
                            this.data.put(FIELD_TRANSFER_TICKET_PRICE, transferTicketPrice);
                        }
                        if (!TextUtils.isEmpty(transferOperationCompany)) {
                            this.data.put(FIELD_TRANSFER_OPERATION_COMPANY, transferOperationCompany);
                        }
                    } else if (type == TYPE_DRIVE) {
                        if (!TextUtils.isEmpty(driveRoadName)) {
                            this.data.put(FIELD_DRIVE_ROAD_NAME, driveRoadName);
                        }
                        if (!TextUtils.isEmpty(driveTurnTo)) {
                            this.data.put(FIELD_DRIVE_TURN_TO, driveTurnTo);
                        }
                        if (!TextUtils.isEmpty(driveGangway)) {
                            this.data.put(FIELD_DRIVE_GANGWAY, driveGangway);
                        }
                        this.data.put(FIELD_DRIVE_WHETHER_ROUNDABOUT, driveWhetherRoundabout);
                        this.data.put(FIELD_DRIVE_WHETHER_CHARGE, driveWhetherCharge);
                        if (!TextUtils.isEmpty(driveRamp)) {
                            this.data.put(FIELD_DRIVE_RAMP, driveRamp);
                        }
                        if (!TextUtils.isEmpty(driveOrientation)) {
                            this.data.put(FIELD_DRIVE_ORIENTATION, driveOrientation);
                        }
                    }
                }
                return this.data;
            }
            
            public String toString() {
                StringBuilder s = new StringBuilder();
                if (TYPE_WALK == type) {
                    s.append("distance=");
                    s.append(distance);
                    s.append(",walkPublicNudity=");
                    s.append(walkPublicNudity);
                    s.append(",walkRoadName=");
                    s.append(walkRoadName);
                    s.append(",walkTurnTo=");
                    s.append(walkTurnTo);
                    s.append(",walkWhetherRoundabout=");
                    s.append(walkWhetherRoundabout);
                } else if (TYPE_TRANSFER == type) {
                    s.append("distance=");
                    s.append(distance);
                    s.append(",transferUpStopName=");
                    s.append(transferUpStopName);
                    s.append(",transferDownStopName=");
                    s.append(transferDownStopName);
                    s.append(",transferPreviousStopName=");
                    s.append(transferPreviousStopName);
                    s.append(",transferStopNumber=");
                    s.append(transferStopNumber);
                    s.append(",transferLineName=");
                    s.append(transferLineName);
                    s.append(",transferStartStopName=");
                    s.append(transferStartStopName);
                    s.append(",transferEndStopName=");
                    s.append(transferEndStopName);
                    s.append(",transferOperationTime=");
                    s.append(transferOperationTime);
                    s.append(",transferTicketPrice=");
                    s.append(transferTicketPrice);
                    s.append(",transferOperationCompany=");
                    s.append(transferOperationCompany);
                } else if (TYPE_DRIVE == type) {
                    s.append("distance=");
                    s.append(distance);
                    s.append(",driveRoadName=");
                    s.append(driveRoadName);
                    s.append(",driveTurnTo=");
                    s.append(driveTurnTo);
                    s.append(",driveGangway=");
                    s.append(driveGangway);
                    s.append(",driveWhetherRoundabout=");
                    s.append(driveWhetherRoundabout);
                    s.append(",driveWhetherCharge=");
                    s.append(driveWhetherCharge);
                    s.append(",driveRamp=");
                    s.append(driveRamp);
                    s.append(",driveOrientation=");
                    s.append(driveOrientation);
                }
                return s.toString();
            }

            public static XMapInitializer<Step> Initializer = new XMapInitializer<Step>() {

                @Override
                public Step init(XMap data) throws APIException {
                    return new Step(data);
                }
            };
        }
        
        public static class PlanTag extends XMapData {
            //0x01    x_int   标签对应的底图序号；
            //客户端支持的底图序号从1开始，0是超出支持范围之后的底图序号
            final static private byte FIELD_BACKGROUND_TYPE = 0x01;
            //0x02    x_string    标签对应的字符串描述，在客户端将被加到底图上显示 
            final static private byte FIELD_DESCRIPTION = 0x02;
            
            private int backgroundType;
            private String description;
            
            public int getBackgroundtype() {
                return backgroundType;
            }
            
            public String getDescription() {
                return description;
            }
            
            public PlanTag(XMap data) throws APIException {
                super(data);

                backgroundType = (int) data.getInt(FIELD_BACKGROUND_TYPE);
                
                description = data.getString(FIELD_DESCRIPTION);
            }
            
            public static XMapInitializer<PlanTag> Initializer = new XMapInitializer<PlanTag>() {

                @Override
                public PlanTag init(XMap data) throws APIException {
                    return new PlanTag(data);
                }
            };
        }
        
        private String description;
        
        private int length;
        
        private List<Step> stepList;
        
        private List<PlanTag> planTagList;
        
        private String startOutOrientation;
        
        private POI start;
        
        private POI end;
        
        private String expectedDriveTime;
        
        private String driveDistance;
        
        private String trafficLightNum;
        
        private String taxiCost;
        
        private String expectedBusTime;
        
        private String busDistance;
        
        private String walkDistance;
        
        private String busstopNum;
        
        private List<Position> routeGeometry = new ArrayList<Position>();
        
        /*
         * 交通方案类型, 解析返回的交通查询结果时, 从交通查询类TrafficQuery获得类型并设值
         */
        private int type;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public POI getStart() {
            return start;
        }
        
        public void setStart(POI start) {
            this.start = start;
        }
        
        public void setEnd(POI end) {
            this.end = end;
        }

        public POI getEnd() {
            return end;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public List<Step> getStepList() {
            return stepList;
        }

        public void setStepList(List<Step> stepList) {
            this.stepList = stepList;
        }
        
        public List<PlanTag> getPlanTagList() {
            return planTagList;
        }

        public String getStartOutOrientation() {
            return startOutOrientation;
        }

        public void setStartOutOrientation(String startOutOrientation) {
            this.startOutOrientation = startOutOrientation;
        }

        public String getExpectedDriveTime() {  return expectedDriveTime;}
        
        public String getDriveDistance() {  return driveDistance;}
        
        public String getTrafficLightNum() {  return trafficLightNum;}
        
        public String getTaxiCost() {  return taxiCost;}
        
        public String getExpectedBusTime() {  return expectedBusTime;}
        
        public String getBusDistance() {  return busDistance;}
        
        public String getWalkDistance() {  return walkDistance;}
        
        public String getBusstopNum() {  return busstopNum;}
        
        /**
         * 获取交通方案路线所经过的所有经纬坐标点
         * 
         * @return
         */
        public List<Position> getRouteGeometry() {
            return routeGeometry;
        }

        public Plan() {
        }

        public Plan(XMap data) throws APIException {            
            super(data);
            init(data, true);
        }
        
        public void init(XMap data, boolean reset) throws APIException {
            super.init(data, reset);
            title = null;
            lengthStr = null;
            
            description = getStringFromData(FIELD_DESCRIPTION, reset ? null : description);
            length = (int)getLongFromData(FIELD_LENGTH, reset ? 0 : length);
            stepList = getListFromData(FIELD_STEP_LIST, Step.Initializer, reset ? null : stepList);
            planTagList = getListFromData(FIELD_PLAN_TAG, PlanTag.Initializer, reset ? null : planTagList);
            startOutOrientation = getStringFromData(FIELD_START_OUT_ORIENTATION, reset ? null : startOutOrientation);
            expectedDriveTime = getStringFromData(FIELD_EXPECTED_DRIVE_TIME, reset ? null : expectedDriveTime);
            driveDistance = getStringFromData(FIELD_DRIVE_DISTANCE, reset ? null : driveDistance);
            trafficLightNum = getStringFromData(FIELD_TRAFFIC_LIGHT_NUM, reset ? null : trafficLightNum);
            taxiCost = getStringFromData(FIELD_TAXI_COST, reset ? null : taxiCost);
            expectedBusTime = getStringFromData(FIELD_EXPECTED_BUS_TIME, reset ? null : expectedBusTime);
            busDistance = getStringFromData(FIELD_BUS_DISTANCE, reset ? null : busDistance);
            walkDistance = getStringFromData(FIELD_WALK_DISTANCE, reset ? null : walkDistance);
            busstopNum = getStringFromData(FIELD_BUSSTOP_NUM, reset ? null : busstopNum);

            if (stepList != null) {
                for (int i = 0, size = stepList.size(); i < size; i++) {
                    Step step = stepList.get(i);
                    routeGeometry.addAll(step.getPositionList());
                }
            }
            
        }
        
        public void resetData() {
        	/*
             * 将服务端返回的"长度为0米"的步骤去除
             */
        	this.data = null;
            this.data = getData();
        }
        
        public XMap getData() {
            if (data == null) {
                data = new XMap();
                
                if (!TextUtils.isEmpty(description)) {
                    data.put(FIELD_DESCRIPTION, XObject.valueOf(description));
                }
                
                data.put(FIELD_LENGTH, XObject.valueOf(length));

                if (stepList != null) {
                    XArray<XMap> xarray = new XArray<XMap>();
                    for(Step step : stepList) {
                        xarray.add(step.getData());
                    }
                    data.put(FIELD_STEP_LIST, xarray);
                }
                
                if (!TextUtils.isEmpty(startOutOrientation)) {
                    data.put(FIELD_START_OUT_ORIENTATION, XObject.valueOf(startOutOrientation));
                }
            }
            return data;
        }

        public String getSMSString(Context context, String title) {
            StringBuilder body = new StringBuilder();
            body.append(title);
            body.append("\n");
            body.append(start.getName());
            body.append(" >> ");
            body.append(end.getName());
            body.append("\n");

            List<CharSequence> strList = NavigationSplitJointRule.splitJoint(context, this);
            int i = 1;
            for(CharSequence str : strList) {
                body.append(i++);
                body.append(". ");
                body.append(str);
                body.append("\n");
            }
                
            return body.toString();
        }
        
        public Uri writeToDatabases(Context context, long parentId, int storeType) {
            boolean isFailed = false;
            ContentValues values = new ContentValues();
            if (stepList != null) {
                values.put(Tigerknows.TransitPlan.TIMES, stepList.size());
            }
            values.put(Tigerknows.TransitPlan.TOTAL_LENGTH, length);
            values.put(Tigerknows.TransitPlan.TYPE, type);
            Uri uri = start.writeToDatabases(context, -1, Tigerknows.STORE_TYPE_OTHER);
            if (uri != null) {
                values.put(Tigerknows.TransitPlan.START, uri.getPathSegments().get(1));
            } else {
                isFailed = true;
            }
            uri = end.writeToDatabases(context, -1, Tigerknows.STORE_TYPE_OTHER);
            if (uri != null) {
                values.put(Tigerknows.TransitPlan.END, uri.getPathSegments().get(1));
            } else {
                isFailed = true;
            }
            try {
                byte[] data = ByteUtil.xobjectToByte(getData());
                if (data.length > 0) {
                    values.put(Tigerknows.TransitPlan.DATA, data);
                }
            } catch (Exception e) {
                // TODO: handle exception
                isFailed = true;
            }
            
            uri = null;        
            if (!isFailed) {
                int trafficType = -1;
                if (storeType == Tigerknows.STORE_TYPE_FAVORITE) {
                    if (Step.TYPE_TRANSFER == type) {
                        trafficType = Favorite.FAVORITE_TRANSFER;
                    } else if (Step.TYPE_DRIVE == type) {
                        trafficType = Favorite.FAVORITE_DRIVE;
                    } else if (Step.TYPE_WALK == type) {
                        trafficType = Favorite.FAVORITE_WALK;
                    }
                } else if (storeType == Tigerknows.STORE_TYPE_HISTORY) {
                    if (Step.TYPE_TRANSFER == type) {
                        trafficType = History.HISTORY_TRANSFER;  //4
                    } else if (Step.TYPE_DRIVE == type) {
                        trafficType = History.HISTORY_DRIVE;     //3
                    } else if (Step.TYPE_WALK == type) {
                        trafficType = History.HISTORY_WALK;      //5
                    }
                }
                if (-1 != trafficType) {
                    uri = writeToDatabasesInternal(context, Tigerknows.TransitPlan.CONTENT_URI, values, storeType, trafficType);
                }
            }
            return uri;
        }
        
        public static Plan readFromCursor(Context context, Cursor c) {
            Plan plan = new Plan();
            if (c != null) {
                if (c.getCount() > 0) {
                    plan.id = c.getLong(c.getColumnIndex(Tigerknows.TransitPlan._ID));
                    plan.parentId = c.getLong(c.getColumnIndex(Tigerknows.TransitPlan.PARENT_ID));
                    plan.storeType = c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.STORE_TYPE));
                    plan.length = c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.TOTAL_LENGTH));
                    POI start = POI.readFromDatabases(context, c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.START)));
                    POI end = POI.readFromDatabases(context, c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.END)));
                    byte[] data = c.getBlob(c.getColumnIndex(Tigerknows.TransitPlan.DATA));
                    try {
                        XMap xmap = (XMap) ByteUtil.byteToXObject(data);
                        plan.init(xmap, true);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    plan.start = start;
                    plan.end = end;

                    if (TrafficQuery.filterTooSmallLengthStepFromProjectPlan(plan) > 0) {
                        plan.resetData();
                    }
                }
            }
            return plan;
        }
        
        public static List<Plan> readFromDatabases(Context context, long parentId, int storeType) {
            List<Plan> planList = new ArrayList<Plan>();
            Cursor c = SqliteWrapper.query(context,
                    context.getContentResolver(), 
                    Tigerknows.TransitPlan.CONTENT_URI,
                    null,
                    "(" + Tigerknows.TransitPlan.PARENT_ID + "=" + parentId + ") AND (" + Tigerknows.TransitPlan.STORE_TYPE + "=" + storeType + ")",
                    null, 
                    null);
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    for(int i =0; i < c.getCount(); i++) {
                        planList.add(readFromCursor(context, c));
                        c.moveToNext();
                    }
                }
                c.close();
            }
            return planList;
        }
        
        String buildSelectionString(int storeType) {
            return  "(" + Tigerknows.TransitPlan.TOTAL_LENGTH + "="
                    + length + ") AND (" + Tigerknows.TransitPlan.TYPE + "="
                    + type + ") AND (" + Tigerknows.TransitPlan.STORE_TYPE + "="
                    + storeType + ")";
        }
        
        @Override
        public boolean checkStore(Context context) {
            boolean result = false;
            Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                    Tigerknows.TransitPlan.CONTENT_URI, null, buildSelectionString(storeType), null, null);
            if (c != null) {
                int count = c.getCount();
                if (count > 0) {
                    result = true;
                }
                c.close();
            }
            return result;
        }
            
        @Override
        public Plan checkStore(Context context, int storeType) {
            Plan path = null;
            Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                    Tigerknows.TransitPlan.CONTENT_URI, null, buildSelectionString(storeType), null, null);
            if (c != null) {
                int count = c.getCount();
                if (count > 0) {
                    c.moveToFirst();
                    for(int i = 0; i < count; i++) {
                        Plan other = readFromCursor(context, c);
                        if((null != other && !other.equals(this)) || (null == other && other != this)) {
                        } else {
                            path = other;
                            break;
                        }
                        c.moveToNext();
                    }
                }
                c.close();
            }
            return path;
        }
        
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            
            if (object instanceof Plan) {
                Plan other = (Plan) object;
                if((null != other.data && !other.data.equals(data)) || (null == other.data && other.data != data)) {
                    return false;
                } else {
                    if (other.getStart().getName().equals(this.getStart().getName()) && other.getEnd().getName().equals(this.getEnd().getName())){
                        return true;
                    }
                }
            }
            
            return false;
        }
    
        /**
         * This method used to get the type of each traffic step, 
         * and form the types into List.
         * 
         * @param context: used to get the String resources.
         * @return List of the types
         */
        public List<Integer> getStepTypeList(Context context){
            
            List<Step> steps = getStepList();
            List<Integer> stepTypeList = new ArrayList<Integer>();
            
            if(steps == null || steps.size() == 0)
                return stepTypeList;
                        
            if(TrafficQuery.QUERY_TYPE_TRANSFER == type){
                for(Step step : steps){
                    if(Step.TYPE_WALK == step.getType())
                        stepTypeList.add(Step.TYPE_WALK);
                    else if(Step.TYPE_TRANSFER == step.getType())
                        stepTypeList.add(Step.TYPE_TRANSFER);
                    else{
                        // should do something?
                    }
                }
            }else if(TrafficQuery.QUERY_TYPE_DRIVE == type) {
                for(Step step : steps){
                    if (context.getString(R.string.traffic_forward).equals(step.driveTurnTo)) {                   	
                        stepTypeList.add(Step.TYPE_FORWARD);
                    } else if (context.getString(R.string.traffic_back).equals(step.driveTurnTo)) {
                        stepTypeList.add(Step.TYPE_BACK);
                    } else if (context.getString(R.string.traffic_left).equals(step.driveTurnTo) && 
                            step.walkWhetherRoundabout == 1) {
                        stepTypeList.add(Step.TYPE_ROUND_LEFT);
                    } else if (context.getString(R.string.traffic_left).equals(step.driveTurnTo)) {
                        stepTypeList.add(Step.TYPE_LEFT);
                    } else if (context.getString(R.string.traffic_right).equals(step.driveTurnTo) && 
                            step.walkWhetherRoundabout == 1) {
                        stepTypeList.add(Step.TYPE_ROUND_RIGHT);
                    } else if (context.getString(R.string.traffic_right).equals(step.driveTurnTo)) {
                        stepTypeList.add(Step.TYPE_RIGHT);
                    } else{
                        // "沿着****路****"
                        stepTypeList.add(Step.TYPE_FORWARD);
                    }
                }
            }else if(TrafficQuery.QUERY_TYPE_WALK == type) {
                for(Step step : steps){
                    if (context.getString(R.string.traffic_forward).equals(step.walkTurnTo)) {                   	
                        stepTypeList.add(Step.TYPE_FORWARD);
                    } else if (context.getString(R.string.traffic_back).equals(step.walkTurnTo)) {
                        stepTypeList.add(Step.TYPE_BACK);
                    } else if (context.getString(R.string.traffic_left).equals(step.walkTurnTo) && 
                            step.walkWhetherRoundabout == 1) {
                        stepTypeList.add(Step.TYPE_ROUND_LEFT);
                    } else if (context.getString(R.string.traffic_left).equals(step.walkTurnTo)) {
                        stepTypeList.add(Step.TYPE_LEFT);
                    } else if (context.getString(R.string.traffic_right).equals(step.walkTurnTo) && 
                            step.walkWhetherRoundabout == 1) {
                        stepTypeList.add(Step.TYPE_ROUND_RIGHT);
                    } else if (context.getString(R.string.traffic_right).equals(step.walkTurnTo)) {
                        stepTypeList.add(Step.TYPE_RIGHT);
                    } else{
                        // "沿着****路****"
                        stepTypeList.add(Step.TYPE_FORWARD);
                    }
                }
            }else{
                // should do something?
            }
            
            return stepTypeList;
        }
        
        /**
         * 如果历史浏览数据表已经存在此交通结果记录则只更新最近浏览时间，否则将其写入历史浏览数据表
         */
        public void updateHistory(Context context) {
            BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_HISTORY);
            if (baseData != null) {
                com.tigerknows.model.History history = com.tigerknows.model.History.readFromDatabases(context, baseData.parentId);
                if (history != null) {
                    history.updateHistory(context);
                }
            } else {
                Plan plan = new Plan();
                plan.setStart(start);
                plan.setEnd(end);
                plan.type = this.type;
                try {
                    plan.init(getData(), true);
                    plan.writeToDatabases(context, -1, Tigerknows.STORE_TYPE_HISTORY);
                } catch (APIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        private String title = null;
        private String lengthStr = null;
        
        public String getTitle(Context context) {
            if (title == null) {
                String title = null;
                if (stepList != null) {
                    for(int i = 0, size = stepList.size(); i < size; i++) {
                        Step step = stepList.get(i);
                        if (Step.TYPE_TRANSFER == step.getType()) {
                            if (title != null) {
                                title = title + context.getString(R.string.traffic_transfer_arrow) + step.getTransferLineName();
                            } else {
                                title = step.getTransferLineName();
                            }
                        }
                    }
                }
                this.title = title;
                //没有在换乘类型取到title并且有步行的信息，则说明步行可达。
                if (title == null && stepList.size() != 0 && stepList.get(0).getType() == Step.TYPE_WALK) {
                	this.title = context.getString(R.string.traffic_noneed_transfer);
                }
            }
            return title;
        }
        
        public String getLengthStr(Context context) {
            if (lengthStr == null) {
                this.lengthStr = ShareTextUtil.getPlanLength(context, length);
            }
            
            return lengthStr;
        }

        public static XMapInitializer<Plan> Initializer = new XMapInitializer<Plan>() {

            @Override
            public Plan init(XMap data) throws APIException {
                return new Plan(data);
            }
        };
    }
    // 0x11    x_map   附加信息
    private static final byte FIELD_ADDTIONAL_INFO = 0x11;
    
    public static class AddtionalInfo extends XMapData {
        //0x01    x_string    打车路线耗时
        private static final byte FIELD_TAXI_TIME = 0x01;
        
        //0x02    x_string    打车路线里程
        private static final byte FIELD_TAXI_DISTANCE = 0x02;
        
        //0x03    x_string    打车预计花费 
        private static final byte FIELD_TAXI_COST = 0x03;
        
        private String taxiTime;
        
        private String taxiDistance;
        
        private String taxiCost;
        
        public String getTaxiTime() { return taxiTime ;}
        
        public String getTaxiDistance() { return taxiDistance ;}
        
        public String getTaxiCost() { return taxiCost ;}
        
        public AddtionalInfo(XMap data) throws APIException {
            super(data);

            taxiTime = data.getString(FIELD_TAXI_TIME);
            
            taxiDistance = data.getString(FIELD_TAXI_DISTANCE);
            
            taxiCost = data.getString(FIELD_TAXI_COST);
        }
        
        public static XMapInitializer<AddtionalInfo> Initializer = new XMapInitializer<AddtionalInfo>() {

            @Override
            public AddtionalInfo init(XMap data) throws APIException {
                return new AddtionalInfo(data);
            }
        };
    }
    
    // 0x20 x_array<x_map> array<起点备选站点>
    private static final byte FIELD_START_ALTERNATIVES_LIST = 0x20;
    
    public static class Station extends XMapData {
        // 0x01 x_string 名称
        private static final byte FIELD_NAME = 0x01;

        // 0x02 x_int 经度＊100000
        public static final byte FIELD_LONGITUDE = 0x02;

        // 0x03 x_int 纬度＊100000
        public static final byte FIELD_LATITUDE = 0x03;
        
        // 0x04 x_string 备选站点地址
        public static final byte FIELD_ADDRESS = 0x04;
        
        //名称
        private String name;
        
        private Position position;
        
        private String address;
        
        public String getName() {
            return name;
        }

        public Position getPosition() {
			return position;
		}
        
        public String getAddress() {
            return address;
        }

		public void setName(String name) {
            this.name = name;
        }
		
		public void setAddress(String address) {
		    this.address = address;
		}

		public void setPosition(Position position) {
            if (!Util.inChina(position)) {
                return;
            }
            
            this.position = position;
        }

        public Station(XMap data) throws APIException {
            super(data);
            
            name = data.getString(FIELD_NAME);
            this.position = getPositionFromData(FIELD_LONGITUDE, FIELD_LATITUDE);
            address = data.getString(FIELD_ADDRESS);
        }
        
        public Station(String name, Position position) {
        	this.name = name;
        	this.position = position;
        }
        
        public Station(String name, Position position, String address) {
        	this.name = name;
        	this.position = position;
        	this.address = address;
        }
        
        public POI toPOI() {
            POI poi = new POI();
            poi.setName(name);
            poi.setPosition(this.position);
            return poi;
        }

        public static XMapInitializer<Station> Initializer = new XMapInitializer<Station>() {

            @Override
            public Station init(XMap data) throws APIException {
                return new Station(data);
            }
        };
        
    }

    // 0x21 x_array<x_map> array<终点备选站点>
    private static final byte FIELD_END_ALTERNATIVES_LIST = 0x21;
    
    private int type = TYPE_EMPTY;
    private String startName;
    private String endName;
    private List<Plan> planList;
    private AddtionalInfo addtionalInfo;
    private List<Station> startAlternativesList;
    private List<Station> endAlternativesList;
    
    private POI start;
    private POI end;
    
    public POI getStart() {
        return start;
    }
    
    public void setStart(POI start) {
        this.start = start;
        if (planList != null) {
            for(Plan plan : planList) {
                plan.setStart(start);
            }
        }
    }
    
    public POI getEnd() {
        return end;
    }
    
    public void setEnd(POI end) {
        this.end = end;
        if (planList != null) {
            for(Plan plan : planList) {
                plan.setEnd(end);
            }
        }
    }

    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public void setPlanList(List<Plan> planList) {
        this.planList = planList;
    }

    public List<Plan> getPlanList() {
        return planList;
    }
    
    public AddtionalInfo getAddtionalInfo() {
        return addtionalInfo;
    }

    public List<Station> getStartAlternativesList() {
        return startAlternativesList;
    }

    public List<Station> getEndAlternativesList() {
        return endAlternativesList;
    }

    public String getStartName() {
        return startName;
    }

    public void setStartName(String startName) {
        this.startName = startName;
    }

    public String getEndName() {
        return endName;
    }

    public void setEndName(String endName) {
        this.endName = endName;
    }

    public TrafficModel() {
    }
    
    public TrafficModel(XMap data, int queryType) throws APIException {
        super(data);
        
        this.type = (int)getLongFromData(FIELD_TYPE);
        this.startName = getStringFromData(FIELD_START_NAME);
        this.endName = getStringFromData(FIELD_END_NAME);
        this.addtionalInfo = getObjectFromData(FIELD_ADDTIONAL_INFO, AddtionalInfo.Initializer);
        if (TYPE_PROJECT == this.type) {
            this.planList = getListFromData(FIELD_PROJECT_LIST, Plan.Initializer);
            if (this.planList != null) {
                for(int i = 0, size = this.planList.size(); i < size; i++) {
                    Plan plan = this.planList.get(i);
                    plan.setType(queryType);
                    POI poi = plan.getStart();
                    if (poi != null) {
                        poi.setName(this.startName);
                    }
                    poi = plan.getEnd();
                    if (poi != null) {
                        poi.setName(this.endName);
                    }
                }
            }
        } else {
            this.startAlternativesList = getListFromData(FIELD_START_ALTERNATIVES_LIST, Station.Initializer);
            this.endAlternativesList = getListFromData(FIELD_END_ALTERNATIVES_LIST, Station.Initializer);
        }
    }
}

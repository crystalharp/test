package com.tigerknows.model;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.model.xobject.XObject;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.provider.Tigerknows.Favorite;
import com.tigerknows.provider.Tigerknows.History;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.NavigationSplitJointRule;
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
            
            //类型
            private int type;

            //距离
            private int distance;

            //x坐标
            private List<Integer> x;

            //y坐标
            private List<Integer> y;
            
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
                this.x = new ArrayList<Integer>();
                this.y = new ArrayList<Integer>();
                int i = 0;
                int lon = 0;
                int lat = 0;
                int previousLon = 0;
                int previousLat = 0;
                for(Position position : positionList) {
                    if (i == 0) {
                        lon = (int)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
                        lat = (int)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
                        previousLon = lon;
                        previousLat = lat;
                    } else {
                        lon = previousLon - (int)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
                        lat = previousLat - (int)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
                        previousLon = (int)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
                        previousLat = (int)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
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

			@SuppressWarnings("unchecked")
            public Step(XMap data) throws APIException {
                super(data);

                if (this.data.containsKey(FIELD_TYPE)) {
                    type = (int)this.data.getInt(FIELD_TYPE);
                }
                
                if (this.data.containsKey(FIELD_DISTANCE)) {
                    distance = (int)this.data.getInt(FIELD_DISTANCE);
                }

                if (this.data.containsKey(FIELD_X) && this.data.containsKey(FIELD_Y)) {
                    this.x = this.data.getXArray(FIELD_X).toIntList();
                    this.y = this.data.getXArray(FIELD_Y).toIntList();
                    
                    if (this.x.size() == this.y.size()) {
                        int i = 0;
                        Position position;
                        this.positionList = new ArrayList<Position>(this.x.size());
                        double lon = 0;
                        double lat = 0;
                        for(int x : this.x) {
                            if (i == 0) {
                                position = new Position(((double)this.y.get(i))/TKConfig.LON_LAT_DIVISOR, ((double)x)/TKConfig.LON_LAT_DIVISOR);
                                lat = position.getLat();
                                lon = position.getLon();
                            } else {
                                position = new Position(lat + ((double)this.y.get(i))/TKConfig.LON_LAT_DIVISOR, lon + ((double)x)/TKConfig.LON_LAT_DIVISOR);
                                lat = position.getLat();
                                lon = position.getLon();
                            }
                            this.positionList.add(position);
                            i++;
                        }
                    }
                }

                if (type == TYPE_WALK) {
                    if (this.data.containsKey(FIELD_WALK_PUBLIC_NUDITY)) {
                        walkPublicNudity = (int)this.data.getInt(FIELD_WALK_PUBLIC_NUDITY);
                    }
                    if (this.data.containsKey(FIELD_WALK_ROAD_NAME)) {
                        walkRoadName = this.data.getString(FIELD_WALK_ROAD_NAME);
                    }
                    if (this.data.containsKey(FIELD_WALK_TURN_TO)) {
                        walkTurnTo = this.data.getString(FIELD_WALK_TURN_TO);
                    }
                    if (this.data.containsKey(FIELD_WALK_WHETHER_ROUNDABOUT)) {
                        walkWhetherRoundabout = (int)this.data.getInt(FIELD_WALK_WHETHER_ROUNDABOUT);
                    }
                } else if (type == TYPE_TRANSFER) {
                    if (this.data.containsKey(FIELD_TRANSFER_UP_STOP_NAME)) {
                        transferUpStopName = this.data.getString(FIELD_TRANSFER_UP_STOP_NAME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_DOWN_STOP_NAME)) {
                        transferDownStopName = this.data.getString(FIELD_TRANSFER_DOWN_STOP_NAME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_PREVIOUS_STOP_NAME)) {
                        transferPreviousStopName = this.data.getString(FIELD_TRANSFER_PREVIOUS_STOP_NAME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_STOP_NUMBER)) {
                        transferStopNumber = (int)this.data.getInt(FIELD_TRANSFER_STOP_NUMBER);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_LINE_NAME)) {
                        transferLineName = this.data.getString(FIELD_TRANSFER_LINE_NAME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_START_STOP_NAME)) {
                        transferStartStopName = this.data.getString(FIELD_TRANSFER_START_STOP_NAME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_END_STOP_NAME)) {
                        transferEndStopName = this.data.getString(FIELD_TRANSFER_END_STOP_NAME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_OPERATION_TIME)) {
                        transferOperationTime = this.data.getString(FIELD_TRANSFER_OPERATION_TIME);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_TICKET_PRICE)) {
                        transferTicketPrice = this.data.getString(FIELD_TRANSFER_TICKET_PRICE);
                    }
                    if (this.data.containsKey(FIELD_TRANSFER_OPERATION_COMPANY)) {
                        transferOperationCompany = this.data.getString(FIELD_TRANSFER_OPERATION_COMPANY);
                    }
                    if (this.data.containsKey(FIELD_LINE_TYPE)) {
                    	lineType = (int)this.data.getInt(FIELD_LINE_TYPE);
                    }
                } else if (type == TYPE_DRIVE) {
                    if (this.data.containsKey(FIELD_DRIVE_ROAD_NAME)) {
                        driveRoadName = this.data.getString(FIELD_DRIVE_ROAD_NAME);
                    }
                    if (this.data.containsKey(FIELD_DRIVE_TURN_TO)) {
                        driveTurnTo = this.data.getString(FIELD_DRIVE_TURN_TO);
//                        Log.d("eric", "drive_turn_to from xmap:"+driveTurnTo);
                    }
                    if (this.data.containsKey(FIELD_DRIVE_GANGWAY)) {
                        driveGangway = this.data.getString(FIELD_DRIVE_GANGWAY);
                    }
                    if (this.data.containsKey(FIELD_DRIVE_WHETHER_ROUNDABOUT)) {
                        driveWhetherRoundabout = (int)this.data.getInt(FIELD_DRIVE_WHETHER_ROUNDABOUT);
                    }
                    if (this.data.containsKey(FIELD_DRIVE_WHETHER_CHARGE)) {
                        driveWhetherCharge = (int)this.data.getInt(FIELD_DRIVE_WHETHER_CHARGE);
                    }
                    if (this.data.containsKey(FIELD_DRIVE_RAMP)) {
                        driveRamp = this.data.getString(FIELD_DRIVE_RAMP);
                    }
                    if (this.data.containsKey(FIELD_DRIVE_ORIENTATION)) {
                        driveOrientation = this.data.getString(FIELD_DRIVE_ORIENTATION);
                    }
                }
            }
            
            public XMap getData() {
                if (this.data == null) {
                    this.data = new XMap();
                    
                    this.data.put(FIELD_TYPE, type);
                    this.data.put(FIELD_DISTANCE, distance);
                    
                    if (this.x != null && this.x.size() > 0) {
                        XArray<XObject> xarray = new XArray<XObject>();
                        for(int i : this.x) {
                            xarray.add(XObject.valueOf(i));
                        }
                        this.data.put(FIELD_X, xarray);
                    }
                    
                    if (this.y != null && this.y.size() > 0) {
                        XArray<XObject> xarray = new XArray<XObject>();
                        for(int i : this.y) {
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
        }
        
        private String description;
        
        private int length;
        
        private List<Step> stepList;
        
        private String startOutOrientation;
        
        private POI start;
        
        private POI end;
        
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

        public String getStartOutOrientation() {
            return startOutOrientation;
        }

        public void setStartOutOrientation(String startOutOrientation) {
            this.startOutOrientation = startOutOrientation;
        }
        
        /**
         * 获取交通方案路线所经过的所有经纬坐标点
         * 
         * @return
         */
        public List<Position> getRouteGeometry() {
            List<Position> positions = new ArrayList<Position>();
            for (Step step : stepList) {
                positions.addAll(step.getPositionList());
            }
            return positions;
        }

        public Plan() {
        }

        public Plan(XMap data) throws APIException {            
            super(data);
            init(data);
        }
        
        @SuppressWarnings("unchecked")
        public void init(XMap data) throws APIException {
            super.init(data);
            if (this.data.containsKey(FIELD_DESCRIPTION)) {
                description = this.data.getString(FIELD_DESCRIPTION);
            }
            
            if (this.data.containsKey(FIELD_LENGTH)) {
                length = (int)this.data.getInt(FIELD_LENGTH);
            }

            XArray<XMap> xarray;
            if (this.data.containsKey(FIELD_STEP_LIST)) {
                xarray = (XArray<XMap>)this.data.getXArray(FIELD_STEP_LIST);
                int size = xarray.size();
                XMap xmap;
                this.stepList = new ArrayList<Step>(size);
                for(int i = 0; i < size; i++) {
                    xmap = (XMap)xarray.get(i);
                    this.stepList.add(new Step(xmap));
                }
            }
            
            if (this.data.containsKey(FIELD_START_OUT_ORIENTATION)) {
                startOutOrientation = this.data.getString(FIELD_START_OUT_ORIENTATION);
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

                if (stepList != null && stepList.size() > 0) {
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

            List<CharSequence> strList = NavigationSplitJointRule.splitJoint(context, type, this);
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
            this.storeType = storeType;
            this.parentId = parentId;
            ContentValues values = new ContentValues();
            values.put(Tigerknows.TransitPlan.TIMES, stepList.size());
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
        
        public static Plan readFormCursor(Context context, Cursor c) {
            Plan plan = new Plan();
            if (c != null) {
                if (c.getCount() > 0) {
                    plan.id = c.getLong(c.getColumnIndex(Tigerknows.TransitPlan._ID));
                    plan.parentId = c.getLong(c.getColumnIndex(Tigerknows.TransitPlan.PARENT_ID));
                    plan.storeType = c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.STORE_TYPE));
                    plan.length = c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.TOTAL_LENGTH));
                    POI start = POI.readFormDatabases(context, c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.START)));
                    POI end = POI.readFormDatabases(context, c.getInt(c.getColumnIndex(Tigerknows.TransitPlan.END)));
                    byte[] data = c.getBlob(c.getColumnIndex(Tigerknows.TransitPlan.DATA));
                    try {
                        XMap xmap = (XMap) ByteUtil.byteToXObject(data);
                        plan.init(xmap);
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
        
        public static List<Plan> readFormDatabases(Context context, long parentId, int storeType) {
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
                        planList.add(readFormCursor(context, c));
                        c.moveToNext();
                    }
                }
                c.close();
            }
            return planList;
        }
            
        @Override
        public BaseData checkStore(Context context, int store_Type) {
            BaseData baseData = null;
            Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
                    Tigerknows.TransitPlan.CONTENT_URI, null, "(" + Tigerknows.TransitPlan.TOTAL_LENGTH + "="
                            + length + ") AND (" + Tigerknows.TransitPlan.TYPE + "="
                            + type + ") AND (" + Tigerknows.TransitPlan.STORE_TYPE + "="
                            + store_Type + ")", null, null);
            if (c != null) {
                int count = c.getCount();
                if (count > 0) {
                    c.moveToFirst();
                    for(int i = 0; i < count; i++) {
                        Plan other = readFormCursor(context, c);
                        if((null != other && !other.equals(this)) || (null == other && other != this)) {
                        } else {
                            baseData = other;
                            break;
                        }
                        c.moveToNext();
                    }
                }
                c.close();
            }
            return baseData;
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
                    return true;
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

        public void updateHistory(Context context) {
            BaseData baseData = checkStore(context, Tigerknows.STORE_TYPE_HISTORY);
            if (baseData != null) {
                com.tigerknows.model.History history = com.tigerknows.model.History.readFormDatabases(context, baseData.parentId);
                if (history != null) {
                    history.updateHistory(context);
                }
            } else {
                Plan plan = new Plan();
                plan.setStart(start);
                plan.setEnd(end);
                plan.type = this.type;
                try {
                    plan.init(getData());
                    plan.writeToDatabases(context, -1, Tigerknows.STORE_TYPE_HISTORY);
                } catch (APIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    // 0x20 x_array<x_map> array<起点备选站点>
    private static final byte FIELD_START_ALTERNATIVES_LIST = 0x20;
    
    public static class Station {
        // 0x01 x_string 名称
        private static final byte FIELD_NAME = 0x01;

        // 0x02 x_int 经度＊100000
        public static final byte FIELD_LONGITUDE = 0x02;

        // 0x03 x_int 纬度＊100000
        public static final byte FIELD_LATITUDE = 0x03;
        
        //名称
        private String name;
        
        //经度＊100000
        private int longitude;
        
        //纬度＊100000
        private int latitude;
        
        private Position position;
        
        public String getName() {
            return name;
        }

        public Position getPosition() {
			return position;
		}

		public void setName(String name) {
            this.name = name;
        }

        public int getLongitude() {
            return longitude;
        }

        public int getLatitude() {
			return latitude;
		}

		public void setPosition(Position position) {
            if (!Util.inChina(position)) {
                return;
            }
            
            this.longitude = (int)(position.getLon()*TKConfig.LON_LAT_DIVISOR);
            this.latitude = (int)(position.getLat()*TKConfig.LON_LAT_DIVISOR);
            this.position = position;
        }

        public Station(XMap data) {
            if (data == null) {
                return;
            }
            
            if (data.containsKey(FIELD_NAME)) {
                name = data.getString(FIELD_NAME);
            }
            
            if (data.containsKey(FIELD_LONGITUDE) && data.containsKey(FIELD_LATITUDE)) {
                longitude = (int)data.getInt(FIELD_LONGITUDE);// /10
                latitude = (int)data.getInt(FIELD_LATITUDE);
                this.position = new Position(((double)this.latitude)/TKConfig.LON_LAT_DIVISOR, ((double)this.longitude)/TKConfig.LON_LAT_DIVISOR);
            }
            
        }
        
        public Station(String name, Position position) {
        	this.name = name;
        	this.position = position;
        }
        
        public POI toPOI() {
            POI poi = new POI();
            poi.setName(name);
            if (this.position == null) {
                this.position = new Position(((double)this.latitude)/TKConfig.LON_LAT_DIVISOR, ((double)this.longitude)/TKConfig.LON_LAT_DIVISOR);
            }
            poi.setPosition(this.position);
            return poi;
        }
        
    }

    // 0x21 x_array<x_map> array<终点备选站点>
    private static final byte FIELD_END_ALTERNATIVES_LIST = 0x21;
    
    private int type = TYPE_EMPTY;
    private String startName;
    private String endName;
    private List<Plan> planList;
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
    
    @SuppressWarnings("unchecked")
    public TrafficModel(XMap data, int queryType) throws APIException {
        super(data);
        
        if (this.data.containsKey(FIELD_TYPE)) {
            this.type = (int)this.data.getInt(FIELD_TYPE);
        }
        
        if (this.data.containsKey(FIELD_START_NAME)) {
            this.startName = this.data.getString(FIELD_START_NAME);
        }
        
        if (this.data.containsKey(FIELD_END_NAME)) {
            this.endName = this.data.getString(FIELD_END_NAME);
        }
        if (TYPE_PROJECT == this.type) {
            XArray<XMap> xarray;
            if (this.data.containsKey(FIELD_PROJECT_LIST)) {
                xarray = (XArray<XMap>)data.getXArray(FIELD_PROJECT_LIST);
                int size = xarray.size();
                XMap xmap;
                this.planList = new ArrayList<Plan>(size);
                for(int i = 0; i < size; i++) {
                    xmap = (XMap)xarray.get(i);
                    Plan plan = new Plan(xmap);
                    plan.setType(queryType);
                    POI poi = plan.getStart();
                    if (poi != null) {
                        poi.setName(this.startName);
                    }
                    poi = plan.getEnd();
                    if (poi != null) {
                        poi.setName(this.endName);
                    }
                    this.planList.add(plan);
                }
            }
        } else {
            XArray<XMap> xarray;
            if (this.data.containsKey(FIELD_START_ALTERNATIVES_LIST)) {
                xarray = (XArray<XMap>)data.getXArray(FIELD_START_ALTERNATIVES_LIST);
                int size = xarray.size();
                XMap xmap;
                this.startAlternativesList = new ArrayList<Station>(size);
                for(int i = 0; i < size; i++) {
                    xmap = (XMap)xarray.get(i);
                    this.startAlternativesList.add(new Station(xmap));
                }
            }
            if (this.data.containsKey(FIELD_END_ALTERNATIVES_LIST)) {
                xarray = (XArray<XMap>)data.getXArray(FIELD_END_ALTERNATIVES_LIST);
                int size = xarray.size();
                XMap xmap;
                this.endAlternativesList = new ArrayList<Station>(size);
                for(int i = 0; i < size; i++) {
                    xmap = (XMap)xarray.get(i);
                    this.endAlternativesList.add(new Station(xmap));
                }
            }
        }
    }
}

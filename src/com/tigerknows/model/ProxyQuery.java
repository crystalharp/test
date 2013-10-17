package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.ProxyQueryTest;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;

import java.util.List;

/**
 * 
 * @author pengwenyue
 *
 */
public class ProxyQuery extends BaseQuery {

    // task int true 参见task
    public static final String SERVER_PARAMETER_TASK = "task";

    // 1 获取酒店房型动态信息
    public static final String TASK_ROOM_TYPE_DYNAMIC = "1";
    
    // vendorid string false 第三方商家id (实际情况5.9版本之后为必传)
    public static final String SERVER_PARAMETER_VENDORID = "vendorid";

    // hotelid string true 酒店ID
    public static final String SERVER_PARAMETER_HOTELID = "hotelid";

    // roomid string true 房型ID
    public static final String SERVER_PARAMETER_ROOMID = "roomid";

    // pkgid string true 房型套餐ID
    public static final String SERVER_PARAMETER_ROOM_TYPE_TAOCANID = "pkgid";

    // checkindate string true 入住日期，格式如2013-04-22
    public static final String SERVER_PARAMETER_CHECKIN_DATE = "checkindate";

    // checkoutdate string true 离开日期，格式如2013-04-22
    public static final String SERVER_PARAMETER_CHECKOUT_DATE = "checkoutdate";

    //动态房型信息查询必选参数key
    private static final String[] ROOM_TYPE_DYNAMIC_EKEYS = new String[] {
        SERVER_PARAMETER_VENDORID, SERVER_PARAMETER_HOTELID, SERVER_PARAMETER_ROOMID,
        SERVER_PARAMETER_ROOM_TYPE_TAOCANID,
        SERVER_PARAMETER_CHECKIN_DATE,
        SERVER_PARAMETER_CHECKOUT_DATE, SERVER_PARAMETER_TASK};
    
    //动态房型信息查询可选参数key
    private static final String[] ROOM_TYPE_DYNAMIC_OKEYS = null;
    
    static final String TAG = "TaskQuery";

    public ProxyQuery(Context context) {
        super(context, API_TYPE_PROXY);
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        String task = getParameter(SERVER_PARAMETER_TASK);
        if (task.equals(TASK_ROOM_TYPE_DYNAMIC)) {
            debugCheckParameters(ROOM_TYPE_DYNAMIC_EKEYS, ROOM_TYPE_DYNAMIC_OKEYS);
        } else {
            throw APIException.wrapToMissingRequestParameterException("task type invalid.");
        }
    }
    
    @Override
    protected void addCommonParameters() {
        super.addCommonParameters();
        addSessionId();
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.getProxyUrl(), TKConfig.getQueryHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);

        String taskType = getParameter(SERVER_PARAMETER_TASK);
        if (taskType.equals(TASK_ROOM_TYPE_DYNAMIC)) {
            response = new RoomTypeDynamic(responseXMap);
        } else {
            throw APIException.wrapToMissingRequestParameterException("task type invalid.");
        }
    }

    protected void launchTest() {
        super.launchTest();
        String task = getParameter(SERVER_PARAMETER_TASK);
        if (task.equals(TASK_ROOM_TYPE_DYNAMIC)) {
            responseXMap = ProxyQueryTest.launchRoomTypeDynamicRespnose(context);
        }
    }

    public static class RoomTypeDynamic extends Response {
        // 0x02 x_double 房型套餐单价
        public static final byte FIELD_PRICE = 0x02;

        // 0x03 x_int 可预订数量上限
        public static final byte FIELD_NUM = 0x03;

        // 0x04 xmap 参见担保规则
        public static final byte FIELD_DANBAO_GUIZE = 0x04;
        
        // 0x05 x_string 宾客类型代码
        public static final byte FIELD_GUEST_TYPE = 0x05;
        
        // 0x06 x_double 1间房首晚价格
        public static final byte FIELD_FIRST_NIGHT_PRICE = 0x06;
        
        // 0x07 x_int 可预订数量下限
        public static final byte FIELD_MINIMUM = 0x07;

        private double price;

        private long num;

        private DanbaoGuize danbaoGuize;
        
        private String guestType;
        
        private double firstNightPrice;
        
        private long minimum;

        public double getPrice() {
            return price;
        }

        public long getNum() {
            return num;
        }

        public DanbaoGuize getDanbaoGuize() {
            return danbaoGuize;
        }
        
        public String getGuesttype() {
        	return guestType;
        }
        
        public double getFirstNightPrice() {
        	return firstNightPrice;
        }
        
        public long getMinimum() {
        	return minimum;
        }

        public RoomTypeDynamic(XMap data) throws APIException {
            super(data);

            this.price = getDoubleFromData(FIELD_PRICE);
            this.num = getLongFromData(FIELD_NUM);
            this.danbaoGuize = getObjectFromData(FIELD_DANBAO_GUIZE, DanbaoGuize.Initializer);
            this.guestType = getStringFromData(FIELD_GUEST_TYPE);
            this.firstNightPrice = getDoubleFromData(FIELD_FIRST_NIGHT_PRICE);
            this.minimum = getLongFromData(FIELD_MINIMUM);
        }

        public static class DanbaoGuize extends XMapData {
            // 0x01 x_int 担保的起始房间数量T，当订房数量 >= T，需要担保
            public static final byte FIELD_NUM = 0x01;

            // 0x02 x_array<x_map> 订房数量 < T时，展示给用户的保留时间选项列表，参见保留时间选项
            public static final byte FIELD_NUM_LESS = 0x02;

            // 0x03 x_array<x_map> 订房数量 >= T时，展示给用户的保留时间选项列表，参见保留时间选项
            public static final byte FIELD_NUM_GREATER = 0x03;

            private long num;

            private List<RetentionTime> lessList;

            private List<RetentionTime> greaterList;

            public long getNum() {
                return num;
            }

            public List<RetentionTime> getLessList() {
                return lessList;
            }

            public List<RetentionTime> getGreaterList() {
                return greaterList;
            }

            public DanbaoGuize(XMap data) throws APIException {
            	super(data);
                num = getLongFromData(FIELD_NUM);
                lessList = getListFromData(FIELD_NUM_LESS, RetentionTime.Initializer);
                greaterList = getListFromData(FIELD_NUM_GREATER, RetentionTime.Initializer);
            }

            public static class RetentionTime extends XMapData {
                // 0x01 x_string 保留时间（保留到XX点），格式“保留到hh:mm”或“保留到次日hh:mm”
                public static final byte FIELD_TIME = 0x01;

                // 0x02 x_int 是否需要信用卡担保的标志：0 - 不需要，1 - 需要
                public static final byte FIELD_NEED = 0x02;

                public static final int NEED_NO = 0;

                public static final int NEED_YES = 1;

                // 0x03 x_int 担保类型（0x02为1时有效）：1 - 首晚担保，2 - 全额担保
                public static final byte FIELD_TYPE = 0x03;

                public static final int TYPE_FIRST_NIGHT = 1;

                public static final int TYPE_ALL = 2;
                
                // 0x04 x_string 保留时间点所在的日期和时间，格式为YYYY-MM-DD hh:mm:ss，用于数据提交
                
                public static final byte FIELD_TIME_DETAIL = 0x04;
                
                private String time;

                private long need;

                private long type;
                
                private String timeDetail;

                public String getTime() {
                    return time;
                }

                public long getNeed() {
                    return need;
                }

                public long getType() {
                    return type;
                }
                
                public String getTimeDetail(){
                	return timeDetail;
                }
                
                public void setTimeDeatil(String td){
                	this.timeDetail = td;
                }
                
                public RetentionTime(XMap data) throws APIException {
                	super(data);
                    time = getStringFromData(FIELD_TIME);
                    need = getLongFromData(FIELD_NEED);
                    type = getLongFromData(FIELD_TYPE);
                    timeDetail = getStringFromData(FIELD_TIME_DETAIL);
                }

                public static XMapInitializer<RetentionTime> Initializer = new XMapInitializer<RetentionTime>() {

                    @Override
                    public RetentionTime init(XMap data) throws APIException {
                        return new RetentionTime(data);
                    }
                };
            }

            public static XMapInitializer<DanbaoGuize> Initializer = new XMapInitializer<DanbaoGuize>() {

                @Override
                public DanbaoGuize init(XMap data) throws APIException {
                    return new DanbaoGuize(data);
                }
            };
        }
    }
}

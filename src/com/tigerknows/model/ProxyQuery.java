package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.BaseQueryTest;
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

    // 1 获取艺龙酒店房型动态信息
    public static final String TASK_ROOM_TYPE_DYNAMIC = "1";

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

    // 2 注册七天酒店会员
    public static final String TASK_REGISTER_7_DAY_MEMBER = "2";

    // username string true 用户姓名，不支持标点符号
    public static final String SERVER_PARAMETER_USERNAME = "username";

    // mobile string true 用户手机号
    public static final String SERVER_PARAMETER_MOBILE = "mobile";

    // idcardno string true 用于注册酒店会员的身份证号
    public static final String SERVER_PARAMETER_IDCARDNO = "idcardno";

    static final String TAG = "TaskQuery";

    public ProxyQuery(Context context) {
        super(context, API_TYPE_TASK);
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters, cityId);
        if (criteria == null) {
            throw new APIException(APIException.CRITERIA_IS_NULL);
        }
        String task = addParameter(SERVER_PARAMETER_TASK);
        if (task.equals(TASK_ROOM_TYPE_DYNAMIC)) {
            addParameter(new String[] {
                    SERVER_PARAMETER_HOTELID, SERVER_PARAMETER_ROOMID,
                    SERVER_PARAMETER_ROOM_TYPE_TAOCANID,
                    SERVER_PARAMETER_CHECKIN_DATE,
                    SERVER_PARAMETER_CHECKOUT_DATE
            });
        } else if (task.equals(TASK_REGISTER_7_DAY_MEMBER)) {
            addParameter(new String[] {
                    SERVER_PARAMETER_USERNAME,
                    SERVER_PARAMETER_MOBILE,
                    SERVER_PARAMETER_IDCARDNO
            });
        } else {
            throw APIException.wrapToMissingRequestParameterException("task type invalid.");
        }
        addSessionId(false);
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.getQueryUrl(), TKConfig.getQueryHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);

        String taskType = this.criteria.get(SERVER_PARAMETER_TASK);
        if (taskType.equals(TASK_ROOM_TYPE_DYNAMIC)) {
            response = new RoomTypeDynamic(responseXMap);
        } else if (taskType.equals(TASK_REGISTER_7_DAY_MEMBER)) {
            response = new Response(responseXMap);
        } else {
            throw APIException.wrapToMissingRequestParameterException("task type invalid.");
        }
    }

    protected void launchTest() {
        super.launchTest();
        String task = this.criteria.get(SERVER_PARAMETER_TASK);
        if (task.equals(TASK_ROOM_TYPE_DYNAMIC)) {
            responseXMap = ProxyQueryTest.launchRoomTypeDynamicRespnose(context);
        } else if (task.equals(TASK_REGISTER_7_DAY_MEMBER)) {
            responseXMap = BaseQueryTest.launchResponse();
        }
    }

    public static class RoomTypeDynamic extends Response {
        // 0x02 x_double 房型套餐单价
        public static final byte FILED_PRICE = 0x02;

        // 0x03 x_int 可预订数量
        public static final byte FILED_NUM = 0x03;

        // 0x04 xmap 参见担保规则
        public static final byte FILED_DANBAO_GUIZE = 0x04;

        private double price;

        private long num;

        private DanbaoGuize danbaoGuize;

        public double getPrice() {
            return price;
        }

        public long getNum() {
            return num;
        }

        public DanbaoGuize getDanbaoGuize() {
            return danbaoGuize;
        }

        public RoomTypeDynamic(XMap data) throws APIException {
            super(data);

            this.price = getDoubleFromData(FILED_PRICE);
            this.num = getLongFromData(FILED_NUM);
            this.danbaoGuize = getObjectFromData(FILED_DANBAO_GUIZE, DanbaoGuize.Initializer);
        }

        public static class DanbaoGuize extends XMapData {
            // 0x01 x_int 担保的起始房间数量T，当订房数量 >= T，需要担保
            public static final byte FILED_NUM = 0x01;

            // 0x02 x_array<x_map> 订房数量 < T时，展示给用户的保留时间选项列表，参见保留时间选项
            public static final byte FILED_NUM_LESS = 0x02;

            // 0x03 x_array<x_map> 订房数量 >= T时，展示给用户的保留时间选项列表，参见保留时间选项
            public static final byte FILED_NUM_GREATER = 0x03;

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
                num = getLongFromData(FILED_NUM);
                lessList = getListFromData(FILED_NUM_LESS, RetentionTime.Initializer);
                greaterList = getListFromData(FILED_NUM_LESS, RetentionTime.Initializer);
            }

            public static class RetentionTime extends XMapData {
                // 0x01 x_string 保留时间（保留到XX点），格式YYYY-MM-DD hh:mm:ss
                public static final byte FILED_TIME = 0x01;

                // 0x02 x_int 是否需要信用卡担保的标志：0 - 不需要，1 - 需要
                public static final byte FILED_NEED = 0x02;

                public static final int NEED_NO = 0;

                public static final int NEED_YES = 1;

                // 0x03 x_int 担保类型（0x02为1时有效）：1 - 首晚担保，2 - 全额担保
                public static final byte FILED_TYPE = 0x03;

                public static final int TYPE_FIRST_NIGHT = 1;

                public static final int TYPE_ALL = 2;

                private String time;

                private long need;

                private long type;

                public String getTime() {
                    return time;
                }

                public long getNeed() {
                    return need;
                }

                public long getType() {
                    return type;
                }

                public RetentionTime(XMap data) throws APIException {
                	super(data);
                    time = getStringFromData(FILED_TIME);
                    need = getLongFromData(FILED_NEED);
                    type = getLongFromData(FILED_TYPE);
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

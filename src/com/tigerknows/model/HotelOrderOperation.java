/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.test.HotelOrderOperationTest;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;

import java.util.List;

public class HotelOrderOperation extends BaseQuery {

    // orderids	 String	 true	 订单id的列表字符串，id间以下划线间隔
    public static final String SERVER_PARAMETER_ORDER_IDS = "orderids";
    
    public static final String SERVER_PARAMETER_ORDER_ID_FILTER = "idfilter";
    
    // orderid  String  true    订单id
    public static final String SERVER_PARAMETER_ORDER_ID = "orderid";
    
    // 酒店ID
    public static final String SERVER_PARAMETER_HOTEL_ID = "hotelid";

    // 酒店品牌标识，来自酒店POI
    public static final String SERVER_PARAMETER_BRAND = "brand";

    // 房型ID
    public static final String SERVER_PARAMETER_ROOMTYPE = "roomtype";

    // 房型套餐id
    public static final String SERVER_PARAMETER_PKGID = "pkgid";
    
    // 入住日期，格式YYYY-MM-DD
    public static final String SERVER_PARAMETER_CHECKIN_DATE = "checkindate";
    
    // 离开日期，格式YYYY-MM-DD
    public static final String SERVER_PARAMETER_CHECKOUT_DATE = "checkoutdate";
    
    // 用户所选的保留时间，格式YYYY-MM-DD hh:mm:ss
    public static final String SERVER_PARAMETER_RESERVE_TIME = "rtime";
    
    // 预订房间数
    public static final String SERVER_PARAMETER_NUMROOMS = "numrooms";
    
    // 总价
    public static final String SERVER_PARAMETER_TOTAL_PRICE = "totalprice";
    
    // 预订人姓名
    public static final String SERVER_PARAMETER_USERNAME = "username";
    
    // 用户手机号
    public static final String SERVER_PARAMETER_MOBILE = "mobile";
    
    // 入住人姓名，若有多人，以分号间隔
    public static final String SERVER_PARAMETER_GUESTS = "guests";
    
    // 宾客类型代码
    public static final String SERVER_PARAMETER_GUESTTYPE = "guesttype";
    
    // 第三方商家id，若不提交则默认为艺龙
    public static final String SERVER_PARAMETER_VENDORID = "vendorid";
    
    // 信用卡号
    public static final String SERVER_PARAMETER_CREDIT_CARD_NO = "creditcardno";

    // 信用卡背面的验证码，由三位数字构成
    public static final String SERVER_PARAMETER_VERIFY_CODE = "verifycode";

    // 有效期，年
    public static final String SERVER_PARAMETER_VALID_YEAR = "validyear";

    // 有效期，月
    public static final String SERVER_PARAMETER_VALID_MONTH = "validmonth";

    // 持卡人姓名
    public static final String SERVER_PARAMETER_CARD_HOLDER_NAME = "cardholdername";

    // 证件类别代码
    public static final String SERVER_PARAMETER_IDCARD_TYPE = "idcardtype";

    // 证件号码
    public static final String SERVER_PARAMETER_IDCARD_NO = "idcardno";

    // action	 int	 true	 更新数据的操作类型：如1标识取消该订单
    public static final String SERVER_PARAMETER_UPDATE_ACTION = "action";
    
    // 取消订单的操作
    public static final String ORDER_UPDATE_ACTION_CANCEL = "1";
    
    // 操作码:
    // 查询 r
    public static final String OPERATION_CODE_QUERY = "r";

    // 新建 c
    public static final String OPERATION_CODE_CREATE = "c";

    // 更新 u
    public static final String OPERATION_CODE_UPDATE = "u";

    // 删除 d
    public static final String OPERATION_CODE_DELETE = "d";
    
    // 查询 r
    public static final String OPERATION_CODE_SYNC= "s";
    
    public HotelOrderOperation(Context context) {
        super(context, API_TYPE_HOTEL_ORDER);
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        String operationCode = getParameter(SERVER_PARAMETER_OPERATION_CODE);
        if (OPERATION_CODE_QUERY.equals(operationCode)) {
            debugCheckParameters(new String[] {SERVER_PARAMETER_ORDER_IDS, SERVER_PARAMETER_OPERATION_CODE});
        } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
            String[] ekeys = new String[]{SERVER_PARAMETER_OPERATION_CODE,
                SERVER_PARAMETER_ROOMTYPE, SERVER_PARAMETER_PKGID,
                SERVER_PARAMETER_CHECKIN_DATE, SERVER_PARAMETER_CHECKOUT_DATE,
                SERVER_PARAMETER_RESERVE_TIME, SERVER_PARAMETER_NUMROOMS,
                SERVER_PARAMETER_TOTAL_PRICE, SERVER_PARAMETER_USERNAME,
                SERVER_PARAMETER_MOBILE, SERVER_PARAMETER_GUESTS,
                SERVER_PARAMETER_GUESTTYPE, SERVER_PARAMETER_HOTEL_ID, SERVER_PARAMETER_VENDORID};
            String[] okeys = new String[] {SERVER_PARAMETER_BRAND};
            String[] ekeys_with_ccard = new String[]{SERVER_PARAMETER_CREDIT_CARD_NO,
                    SERVER_PARAMETER_VERIFY_CODE, SERVER_PARAMETER_VALID_YEAR,
                    SERVER_PARAMETER_VALID_MONTH, SERVER_PARAMETER_CARD_HOLDER_NAME,
                    SERVER_PARAMETER_IDCARD_TYPE, SERVER_PARAMETER_IDCARD_NO};
            String creditCardNo = getParameter(SERVER_PARAMETER_VERIFY_CODE);
            LogWrapper.d("Trap", "s"+creditCardNo);
            if (creditCardNo != null && !TextUtils.isEmpty(creditCardNo)) {
                debugCheckParameters(Utility.mergeArray(ekeys, ekeys_with_ccard), okeys);
            } else {
                debugCheckParameters(ekeys, okeys);
            }
        } else if (OPERATION_CODE_UPDATE.equals(operationCode)) {
            debugCheckParameters(new String[] {SERVER_PARAMETER_OPERATION_CODE, 
                    SERVER_PARAMETER_UPDATE_ACTION, SERVER_PARAMETER_ORDER_ID});
        } else if (OPERATION_CODE_SYNC.equals(operationCode)) {
            debugCheckParameters(new String[] {SERVER_PARAMETER_OPERATION_CODE, 
                    SERVER_PARAMETER_NEED_FIELD, SERVER_PARAMETER_ORDER_ID_FILTER});
        } else {
            throw APIException.wrapToMissingRequestParameterException("operationCode invalid.");
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
        String url = String.format(TKConfig.getHotelOrderUrl(), TKConfig.getHotelOrderHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);

        String operationCode = getParameter(SERVER_PARAMETER_OPERATION_CODE);
        
        if (OPERATION_CODE_QUERY.equals(operationCode)) {
            response = new HotelOrderStatesResponse(responseXMap);
        } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
            response = new HotelOrderCreateResponse(responseXMap);
        } else if (OPERATION_CODE_UPDATE.equals(operationCode)) {
            response = new Response(responseXMap);
        }else if( OPERATION_CODE_SYNC.equals(operationCode)){
            response = new HotelOrderSyncResponse(responseXMap);
        }
    }

    public static class HotelOrderCreateResponse extends Response {
        
        // 0x02 x_string   单个POI数据   
        public static final byte FIELD_ORDER_ID = 0x02;
        
        // 0x03 x_int      该订单的最晚取消时间距1970-01-01 00:00:00的毫秒数
        public static final byte FIELD_CANCEL_DEADLINE = 0x03;
        
        private String orderId;
        
        private long cancelDeadline;
        
        public HotelOrderCreateResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_ORDER_ID)) {
            	orderId = this.data.getString(FIELD_ORDER_ID);
            }
            if (this.data.containsKey(FIELD_CANCEL_DEADLINE)) {
            	cancelDeadline = this.data.getInt(FIELD_CANCEL_DEADLINE);
            }
        }

        public long getCancelDeadline() {
			return cancelDeadline;
		}

		public String getOrderId() {
            return orderId;
        }

    }
    
    public static class HotelOrderStatesResponse extends Response {
        
        // 0x02 x_map   单个POI数据   
        public static final byte FIELD_STATES = 0x02;
        
        private List<Long> states;
        
        @SuppressWarnings("unchecked")
        public HotelOrderStatesResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_STATES)) {
            	states = this.data.getXArray(FIELD_STATES).toIntList();
            }
        }

        public List<Long> getStates() {
            return states;
        }

    }

    public static class HotelOrderSyncResponse extends Response {
        
        // 0x02 x_map   单个POI数据   
        public static final byte FIELD_LIST = 0x02;
        
        private List<HotelOrder> orders;
        
        public HotelOrderSyncResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_LIST)) {
            	orders = getListFromData(FIELD_LIST, HotelOrder.InitializerServerData);;
            }
        }

		public List<HotelOrder> getOrders() {
			return orders;
		}

    }
    
    protected void launchTest() {
        super.launchTest();
        if (hasParameter(SERVER_PARAMETER_OPERATION_CODE)) {
            String operationCode = getParameter(SERVER_PARAMETER_OPERATION_CODE);
            
            if (OPERATION_CODE_CREATE.equals(operationCode)) {
                responseXMap = HotelOrderOperationTest.launchHotelOrderCreateResponse(context, System.currentTimeMillis() % 1000000);
            } if (OPERATION_CODE_QUERY.equals(operationCode)) {
                responseXMap = HotelOrderOperationTest.launchHotelOrderStateResponse(context, getParameter(SERVER_PARAMETER_ORDER_IDS));
            } if (OPERATION_CODE_UPDATE.equals(operationCode)) {
                responseXMap = new XMap();
                responseXMap = BaseQueryTest.launchResponse(responseXMap);
            }
        }
    }
}

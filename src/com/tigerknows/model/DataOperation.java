/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.test.DataOperationTest;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;

import java.util.List;

public class DataOperation extends BaseQuery {

    // dataid string true 数据uid
    public static final String SERVER_PARAMETER_DATA_UID = "dataid";
    
    // orderids	 String	 true	 订单id的列表字符串，id间以下划线间隔
    public static final String SERVER_PARAMETER_ORDER_IDS = "orderids";
    
    // orderid  String  true    订单id
    public static final String SERVER_PARAMETER_ORDER_ID = "orderid";

    // entity string true key-value数组
    public static final String SERVER_PARAMETER_ENTITY = "entity";

    // 分享到新浪
    public static final String SERVER_PARAMETER_SHARE_SINA = "sina";

    // 分享到QQ
    public static final String SERVER_PARAMETER_SHARE_QZONE = "qzone";

    // checkin     String  true    入住酒店时间，格式"yyyy-MM-dd"
    public static final String SERVER_PARAMETER_CHECKIN = "checkin";
    
    // checkout    String  true    离开酒店时间，格式"yyyy-MM-dd" 
    public static final String SERVER_PARAMETER_CHECKOUT = "checkout";
    
    // 订单类型
    public static final String SERVER_PARAMETER_ORDER_TYPE = "otype";
    
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
    
    // 用户姓名
    public static final String SERVER_PARAMETER_USERNAME = "username";
    
    // 用户手机号
    public static final String SERVER_PARAMETER_MOBILE = "mobile";
    
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
    public static final String SERVER_PARAMETER_ORDER_ACTION = "action";
    
    // 取消订单的操作
    public static final String ORDER_ACTION_CANCEL = "1";
    
    // 操作码:
    // 查询 r
    public static final String OPERATION_CODE_QUERY = "r";

    // 新建 c
    public static final String OPERATION_CODE_CREATE = "c";

    // 更新 u
    public static final String OPERATION_CODE_UPDATE = "u";

    // 删除 d
    public static final String OPERATION_CODE_DELETE = "d";

    /**
     * 团购订单类型
     */
    public static final String ORDER_TYPE_TUANGOU = "1";
    
    /**
     * 酒店订单类型
     */
    public static final String ORDER_TYPE_HOTEL = "2";
    
    public DataOperation(Context context) {
        super(context, API_TYPE_DATA_OPERATION);
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        addCommonParameters(requestParameters, cityId);
        
        if (criteria == null) {
            throw new APIException(APIException.CRITERIA_IS_NULL);
        }
        
        String dataType = addParameter(SERVER_PARAMETER_DATA_TYPE);
        String operationCode = addParameter(SERVER_PARAMETER_OPERATION_CODE);
        if (OPERATION_CODE_QUERY.equals(operationCode)) {
        	boolean add_NF_UID = true;
            if(DATA_TYPE_POI.equals(dataType)){
                String subDataType = addParameter(SERVER_PARAMETER_SUB_DATA_TYPE);
                if(SUB_DATA_TYPE_HOTEL.equals(subDataType)){
                	addParameter(new String[]{SERVER_PARAMETER_CHECKIN, SERVER_PARAMETER_CHECKOUT});
                }
            }
            
            if(DATA_TYPE_DINGDAN.equals(dataType)){
                String orderType = addParameter(SERVER_PARAMETER_ORDER_TYPE);
                if(ORDER_TYPE_HOTEL.equals(orderType)){
                    addParameter(SERVER_PARAMETER_ORDER_IDS);
                    add_NF_UID = false;
                }

            }
            
        	if(dataType.equals(DATA_TYPE_DIAOYAN)){
        		add_NF_UID = false;
            }
        	
        	if(add_NF_UID == true){
        		addParameter(new String[] {SERVER_PARAMETER_NEED_FEILD, SERVER_PARAMETER_DATA_UID});
        	}
            
        	// 部分查询需要提交pic信息和dsrc信息，据说上一个写这行代码的人懒得用一堆if判断于是就直接用这行代码了
        	// fengtianxiao 2013.05.10
        	addParameter(new String[] {SERVER_PARAMETER_PICTURE, SERVER_PARAMETER_REQUSET_SOURCE_TYPE}, false);
        } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
            if(DATA_TYPE_DINGDAN.equals(dataType)){
            	// 酒店订单、团购订单、点评，共3种
                String orderType = addParameter(SERVER_PARAMETER_ORDER_TYPE);
                if(ORDER_TYPE_HOTEL.equals(orderType)){
                	// 酒店订单
                	addParameter(SERVER_PARAMETER_HOTEL_ID);
                	addParameter(SERVER_PARAMETER_BRAND,false);
                	addParameter(new String[]{
                			SERVER_PARAMETER_ROOMTYPE,
                			SERVER_PARAMETER_PKGID,
                			SERVER_PARAMETER_CHECKIN_DATE,
                			SERVER_PARAMETER_CHECKOUT_DATE,
                			SERVER_PARAMETER_RESERVE_TIME,
                			SERVER_PARAMETER_NUMROOMS,
                			SERVER_PARAMETER_TOTAL_PRICE,
                			SERVER_PARAMETER_USERNAME,
                			SERVER_PARAMETER_MOBILE
                	});
                	addParameter(new String[]{
                		    SERVER_PARAMETER_CREDIT_CARD_NO,
                		    SERVER_PARAMETER_VERIFY_CODE,
                		    SERVER_PARAMETER_VALID_YEAR,
                		    SERVER_PARAMETER_VALID_MONTH,
                		    SERVER_PARAMETER_CARD_HOLDER_NAME,
                		    SERVER_PARAMETER_IDCARD_TYPE,
                		    SERVER_PARAMETER_IDCARD_NO
                	},false);
                }else{
                	// 团购订单
                	addParameter(SERVER_PARAMETER_ENTITY);
                }
            }else{
            	// 点评
            	addParameter(SERVER_PARAMETER_ENTITY);
            }
        } else if (OPERATION_CODE_UPDATE.equals(operationCode)) {
            if(DATA_TYPE_DINGDAN.equals(dataType)){
                String orderType = addParameter(SERVER_PARAMETER_ORDER_TYPE);
                if (ORDER_TYPE_HOTEL.equals(orderType)) {
                    addParameter(new String[] {SERVER_PARAMETER_ORDER_ACTION, SERVER_PARAMETER_ORDER_ID});
                }
            }
            addParameter(new String[] {SERVER_PARAMETER_DATA_UID, SERVER_PARAMETER_ENTITY});
        } else if (OPERATION_CODE_DELETE.equals(operationCode)) {
            addParameter(SERVER_PARAMETER_DATA_UID);
        } else {
            throw APIException.wrapToMissingRequestParameterException("operationCode invalid.");
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

        String dataType = criteria.get(SERVER_PARAMETER_DATA_TYPE);
        String operationCode = criteria.get(SERVER_PARAMETER_OPERATION_CODE);
        
        if (OPERATION_CODE_QUERY.equals(operationCode)) {
            if (DATA_TYPE_POI.equals(dataType)) {
                String subDataType = criteria.get(SERVER_PARAMETER_SUB_DATA_TYPE);
                if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                    response = new POIQueryResponse(responseXMap);
                }
            } else if (DATA_TYPE_DIANPING.equals(dataType)) {
                response = new CommentQueryResponse(responseXMap);
            } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                response = new TuangouQueryResponse(responseXMap);
            } else if (DATA_TYPE_FENDIAN.equals(dataType)) {
                response = new FendianQueryResponse(responseXMap);
            } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                response = new DianyingQueryResponse(responseXMap);
            } else if (DATA_TYPE_YINGXUN.equals(dataType)) {
                response = new YingxunQueryResponse(responseXMap);
            } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                response = new YanchuQueryResponse(responseXMap);
            } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                response = new ZhanlanQueryResponse(responseXMap);
            } else if (DATA_TYPE_DIAOYAN.equals(dataType)){
            	response = new DiaoyanQueryResponse(responseXMap);
            } else if (DATA_TYPE_DINGDAN.equals(dataType)){
                String orderType = criteria.get(SERVER_PARAMETER_ORDER_TYPE);
                if (ORDER_TYPE_HOTEL.equals(orderType)) {
                    response = new HotelOrderStatesResponse(responseXMap);
                }
            }
        } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
            if (DATA_TYPE_DIANPING.equals(dataType)) {
                response = new CommentCreateResponse(responseXMap);
            } else if (DATA_TYPE_DINGDAN.equals(dataType)) {
                String orderType = criteria.get(SERVER_PARAMETER_ORDER_TYPE);
                if (ORDER_TYPE_TUANGOU.equals(orderType)) {
                    response = new DingdanCreateResponse(responseXMap);
                } else if (ORDER_TYPE_HOTEL.equals(orderType)) {
                    response = new HotelOrderCreateResponse(responseXMap);
                }
            }
        } else if (OPERATION_CODE_UPDATE.equals(operationCode)) {
            if (DATA_TYPE_DIANPING.equals(dataType)) {
                response = new CommentUpdateResponse(responseXMap);
            } else if (DATA_TYPE_DINGDAN.equals(dataType)){
                String orderType = criteria.get(SERVER_PARAMETER_ORDER_TYPE);
                if (ORDER_TYPE_HOTEL.equals(orderType)) {
                	response = new Response(responseXMap);
                }
            }
        }
    }

    public static class HotelOrderCreateResponse extends Response {
        
        // 0x02 x_map   单个POI数据   
        public static final byte FIELD_ORDER_ID = 0x02;
        
        private String orderId;
        
        public HotelOrderCreateResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_ORDER_ID)) {
            	orderId = this.data.getString(FIELD_ORDER_ID);
            }            
        }

        public String getOrderId() {
            return orderId;
        }

    }
    
    public static class HotelOrderStatesResponse extends Response {
        
        // 0x02 x_map   单个POI数据   
        public static final byte FIELD_STATES = 0x02;
        
        private List<Integer> states;
        
        @SuppressWarnings("unchecked")
        public HotelOrderStatesResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_STATES)) {
            	states = this.data.getXArray(FIELD_STATES).toIntList();
            }            
        }

        public List<Integer> getStates() {
            return states;
		}

    }
    
    public static class POIQueryResponse extends Response {
        
        // 0x02 x_map   单个POI数据   
        public static final byte FIELD_POI = 0x02;
        
        private POI poi;

        public POI getPOI() {
            return poi;
        }

        public POIQueryResponse(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_POI)) {
                poi = new POI(this.data.getXMap(FIELD_POI));
            }            
        }        
    }
    
    public static class CommentQueryResponse extends Response {
        
        // 0x02 x_map   点评数据   
        public static final byte FIELD_COMMENT = 0x02;
        
        private Comment comment;
        
        public Comment getComment() {
            return comment;
        }

        public CommentQueryResponse(XMap data) throws APIException {
            super(data);
            
            comment = getObjectFromData(FIELD_COMMENT, Comment.Initializer);
        }

        public void setComment(Comment comment) {
            this.comment = comment;
        }     
    }
    
    public static class CommentCreateResponse extends Response {
        
        // 0x03 x_string    点评数据的时间戳 
        public static final byte FIELD_TIME_STAMP = 0x03;
        
        // 0x04 x_string    点评数据的uid 
        public static final byte FIELD_UID = 0x04;
        
        private String timeStamp;
        
        private String uid;

        public CommentCreateResponse(XMap data) throws APIException {
            super(data);
            
            timeStamp = getStringFromData(FIELD_TIME_STAMP);
            uid = getStringFromData(FIELD_UID);
        }

        public String getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }
    }

    public static class CommentUpdateResponse extends Response {
        
        // 0x03 x_string    点评数据的时间戳 
        public static final byte FIELD_TIME_STAMP = 0x03;
        
        private String timeStamp;
        
        public CommentUpdateResponse(XMap data) throws APIException {
            super(data);
            
            timeStamp = getStringFromData(FIELD_TIME_STAMP);
        }
    
        public String getTimeStamp() {
            return timeStamp;
        }
    
        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }   
    }
    
    public static class TuangouQueryResponse extends Response {
        
        // 0x02 x_map   分店数据   
        public static final byte FIELD_DATA = 0x02;
        
        private Tuangou tuangou;
        
        public Tuangou getTuangou() {
            return tuangou;
        }

        public TuangouQueryResponse(XMap data) throws APIException {
            super(data);
            
            tuangou = getObjectFromData(FIELD_DATA, Tuangou.Initializer);
        } 
    }

    public static class FendianQueryResponse extends Response {
        
        public static final byte FIELD_DATA = 0x02;
        
        private Fendian fendian;
        
        public Fendian getFendian() {
            return fendian;
        }

        public FendianQueryResponse(XMap data) throws APIException {
            super(data);
            
            fendian = getObjectFromData(FIELD_DATA, Fendian.Initializer);
        } 
    }
    
    public static class YingxunQueryResponse extends Response {
        
        public static final byte FIELD_DATA = 0x02;
        
        private Yingxun yingxun;

        public Yingxun getYingxun() {
			return yingxun;
		}

		public YingxunQueryResponse(XMap data) throws APIException {
            super(data);
            
            yingxun = getObjectFromData(FIELD_DATA, Yingxun.Initializer);
        } 
    }
    
    public static class DianyingQueryResponse extends Response {
        
        public static final byte FIELD_DATA = 0x02;
        
        private Dianying dianying;
        
        public Dianying getDianying() {
            return dianying;
        }

        public DianyingQueryResponse(XMap data) throws APIException {
            super(data);
            
            dianying = getObjectFromData(FIELD_DATA, Dianying.Initializer);
        } 
    }
    
    public static class YanchuQueryResponse extends Response {
        
        public static final byte FIELD_DATA = 0x02;
        
        private Yanchu yanchu;
        
        public Yanchu getYanchu() {
            return yanchu;
        }

        public YanchuQueryResponse(XMap data) throws APIException {
            super(data);
            
            yanchu = getObjectFromData(FIELD_DATA, Yanchu.Initializer);
        } 
    }
    
    public static class ZhanlanQueryResponse extends Response {
        
        public static final byte FIELD_DATA = 0x02;
        
        private Zhanlan zhanlan;
        
        public Zhanlan getZhanlan() {
            return zhanlan;
        }

        public ZhanlanQueryResponse(XMap data) throws APIException {
            super(data);
            
            zhanlan = getObjectFromData(FIELD_DATA, Zhanlan.Initializer);
        } 
    }
    
    public static class DiaoyanQueryResponse extends Response {
    	
    	//是否参加过本期调研
    	public static final byte FIELD_HAS_SURVEYED = 0x03;
    	
    	//调研URL
    	public static final byte FIELD_URL = 0x04;
    	
		private long hasSurveyed;
		public void setHasSurveyed(long hasSurveyed) {
			this.hasSurveyed = hasSurveyed;
		}

		private String url;

		public long getHasSurveyed() {
			return hasSurveyed;
		}
    	
		public String getUrl() {
			return url;
		}
 	
		public DiaoyanQueryResponse(XMap data) throws APIException{
			super(data);
			
			hasSurveyed = getLongFromData(FIELD_HAS_SURVEYED);
            url = getStringFromData(FIELD_URL);
		}
    }
    
    public static class DingdanCreateResponse extends Response {
        
        public static final byte FIELD_DATA = 0x02;
        
        private String url;
        
        public String getUrl() {
            return url;
        }

        public DingdanCreateResponse(XMap data) throws APIException {
            super(data);
            
            url = getStringFromData(FIELD_DATA);
        } 
    }
    
    protected void launchTest() {
        super.launchTest();
        String dataType = this.criteria.get(SERVER_PARAMETER_DATA_TYPE);
        if (criteria.containsKey(SERVER_PARAMETER_OPERATION_CODE)) {
            String operationCode = criteria.get(SERVER_PARAMETER_OPERATION_CODE);
            
            if (OPERATION_CODE_CREATE.equals(operationCode)) {
                if (DATA_TYPE_DINGDAN.equals(dataType)) {
                    String orderType = criteria.get(SERVER_PARAMETER_ORDER_TYPE);
                    if (ORDER_TYPE_TUANGOU.equals(orderType)) {
                        responseXMap = DataOperationTest.launchDinydanCreateResponse();
                    } else if (ORDER_TYPE_HOTEL.equals(orderType)) {
                        responseXMap = DataOperationTest.launchHotelOrderCreateResponse(context);
                    }
                } else if (DATA_TYPE_DIANPING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianpingCreateResponse();
                }
            } if (OPERATION_CODE_QUERY.equals(operationCode)) {
                if (DATA_TYPE_POI.equals(dataType)) {
                    String subDataType = criteria.get(SERVER_PARAMETER_SUB_DATA_TYPE);
                    if (SUB_DATA_TYPE_POI.equals(subDataType)) {
                        responseXMap = DataOperationTest.launchPOIQueryResponse();
                    }else if(SUB_DATA_TYPE_HOTEL.equals(subDataType)){
                    	responseXMap = DataOperationTest.launchHotelPOIQueryResponse();
                    }
                } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                    responseXMap = DataOperationTest.launchTuangouQueryResponse(context);
                } else if (DATA_TYPE_FENDIAN.equals(dataType)) {
                    responseXMap = DataOperationTest.launchFendianQueryResponse(context);
                } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianyingQueryResponse(context);
                } else if (DATA_TYPE_YINGXUN.equals(dataType)) {
                    responseXMap = DataOperationTest.launchYingxunQueryResponse(context);
                } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                    responseXMap = DataOperationTest.launchYanchuQueryResponse(context);
                } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                    responseXMap = DataOperationTest.launchZhanlanQueryResponse(context);
                } else if (DATA_TYPE_DIANPING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianpingQueryResponse();
                } else if (DATA_TYPE_DIAOYAN.equals(dataType)) {
                	responseXMap = DataOperationTest.launchDiaoyanQueryResponse(context);
                } else if (DATA_TYPE_DINGDAN.equals(dataType)) {
                    String orderType = criteria.get(SERVER_PARAMETER_ORDER_TYPE);
                    if (ORDER_TYPE_HOTEL.equals(orderType)) {
                    	responseXMap = DataOperationTest.launchHotelOrderStateResponse(context, criteria.get(SERVER_PARAMETER_ORDER_IDS));
                    }
                }
            } if (OPERATION_CODE_UPDATE.equals(operationCode)) {
                if (DATA_TYPE_DIANPING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianpingUpdateResponse();
                } else if (DATA_TYPE_DINGDAN.equals(dataType)) {
                    String orderType = criteria.get(SERVER_PARAMETER_ORDER_TYPE);
                    if (ORDER_TYPE_HOTEL.equals(orderType)) {
                    	responseXMap = new XMap();
                    	responseXMap = BaseQueryTest.launchResponse(responseXMap);
                    }
                }
            }
        }
    }
}

/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.model.test.DataOperationTest;
import com.tigerknows.model.xobject.XMap;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.text.TextUtils;

public class DataOperation extends BaseQuery {

    // dataid string true 数据uid
    public static final String SERVER_PARAMETER_DATA_UID = "dataid";

    // entity string true key-value数组
    public static final String SERVER_PARAMETER_ENTITY = "entity";

    // 分享到新浪
    public static final String SERVER_PARAMETER_SHARE_SINA = "sina";

    // 分享到QQ
    public static final String SERVER_PARAMETER_SHARE_QZONE = "qzone";
    
    // 操作码:
    // 查询 r
    public static final String OPERATION_CODE_QUERY = "r";

    // 新建 c
    public static final String OPERATION_CODE_CREATE = "c";

    // 更新 u
    public static final String OPERATION_CODE_UPDATE = "u";

    // 删除 d
    public static final String OPERATION_CODE_DELETE = "d";
    
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
        String sessionId = Globals.g_Session_Id;

        if (criteria.containsKey(SERVER_PARAMETER_DATA_TYPE) == false) {
        	throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_DATA_TYPE);
        } else if (criteria.containsKey(SERVER_PARAMETER_OPERATION_CODE) == false) {
        	throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_OPERATION_CODE);
        } else {
        	String dataType = criteria.get(SERVER_PARAMETER_DATA_TYPE);
            requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_DATA_TYPE, dataType));
            String operationCode = criteria.get(SERVER_PARAMETER_OPERATION_CODE);
            requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_OPERATION_CODE, operationCode));
            if (OPERATION_CODE_QUERY.equals(operationCode)) {
            	if (criteria.containsKey(SERVER_PARAMETER_NEED_FEILD)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_NEED_FEILD, criteria.get(SERVER_PARAMETER_NEED_FEILD)));
                } else if(dataType.equals(DATA_TYPE_DIAOYAN)){
                	if(TextUtils.isEmpty(sessionId)){
                		throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_SESSION_ID);
                	}
                } else {
                    //只有dty!=21时需要提供额外参数
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_NEED_FEILD);
                }
                
                if (criteria.containsKey(SERVER_PARAMETER_DATA_UID)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_DATA_UID, criteria.get(SERVER_PARAMETER_DATA_UID)));
                } else if(dataType.equals(DATA_TYPE_DIAOYAN) == false){
                    //只有dty!=21时需要提供额外参数
                	throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_DATA_UID);
                }
                if (criteria.containsKey(SERVER_PARAMETER_PICTURE)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_PICTURE, criteria.get(SERVER_PARAMETER_PICTURE)));
                }
            } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_ENTITY)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_ENTITY, criteria.get(SERVER_PARAMETER_ENTITY)));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_ENTITY);
                }
            } else if (OPERATION_CODE_UPDATE.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_DATA_UID)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_DATA_UID, criteria.get(SERVER_PARAMETER_DATA_UID)));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_DATA_UID);
                }
                if (criteria.containsKey(SERVER_PARAMETER_ENTITY)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_ENTITY, criteria.get(SERVER_PARAMETER_ENTITY)));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_ENTITY);
                }
            } else if (OPERATION_CODE_DELETE.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_DATA_UID)) {
                    requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_DATA_UID, criteria.get(SERVER_PARAMETER_DATA_UID)));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_DATA_UID);
                }
            } else {
                throw APIException.wrapToMissingRequestParameterException("operationCode invalid.");
            }
        }

        
//        String sessionId = Globals.g_Session_Id;	挪动到了几十行之前
        if (!TextUtils.isEmpty(sessionId)) {
            requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_SESSION_ID, sessionId));
        } 
        if (!TextUtils.isEmpty(Globals.g_ClientUID)) {
            requestParameters.add(new BasicNameValuePair(SERVER_PARAMETER_CLIENT_ID, Globals.g_ClientUID));
        } else {
            throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_CLIENT_ID);
        }
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
                response = new POIQueryResponse(responseXMap);
            } else if (DATA_TYPE_DIANPING.equals(dataType)) {
                response = new CommentQueryResponse(responseXMap);
            } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                response = new TuangouQueryResponse(responseXMap);
            } else if (DATA_TYPE_FENDIAN.equals(dataType)) {
                response = new FendianQueryResponse(responseXMap);
            } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                response = new DianyingQueryResponse(responseXMap);
            } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                response = new YanchuQueryResponse(responseXMap);
            } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                response = new ZhanlanQueryResponse(responseXMap);
            } else if (DATA_TYPE_DIAOYAN.equals(dataType)){
            	response = new DiaoyanQueryResponse(responseXMap);
            }
        } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
            if (DATA_TYPE_DIANPING.equals(dataType)) {
                response = new CommentCreateResponse(responseXMap);
            } else if (DATA_TYPE_DINGDAN.equals(dataType)) {
                response = new DingdanCreateResponse(responseXMap);
            }
        } else if (OPERATION_CODE_UPDATE.equals(operationCode)) {
            if (DATA_TYPE_DIANPING.equals(dataType)) {
                response = new CommentUpdateResponse(responseXMap);
            }
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
            
            if (this.data.containsKey(FIELD_COMMENT)) {
                comment = new Comment(this.data.getXMap(FIELD_COMMENT));
            }
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
            
            if (this.data.containsKey(FIELD_TIME_STAMP)) {
                timeStamp = this.data.getString(FIELD_TIME_STAMP);
            }   

            if (this.data.containsKey(FIELD_UID)) {
                uid = this.data.getString(FIELD_UID);
            }
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
            
            if (this.data.containsKey(FIELD_TIME_STAMP)) {
                timeStamp = this.data.getString(FIELD_TIME_STAMP);
            }   
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
            
            if (this.data.containsKey(FIELD_DATA)) {
                tuangou = new Tuangou(this.data.getXMap(FIELD_DATA));
            }
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
            
            if (this.data.containsKey(FIELD_DATA)) {
                fendian = new Fendian(this.data.getXMap(FIELD_DATA));
            }
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
            
            if (this.data.containsKey(FIELD_DATA)) {
                dianying = new Dianying(this.data.getXMap(FIELD_DATA));
            }
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
            
            if (this.data.containsKey(FIELD_DATA)) {
                yanchu = new Yanchu(this.data.getXMap(FIELD_DATA));
            }
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
            
            if (this.data.containsKey(FIELD_DATA)) {
                zhanlan = new Zhanlan(this.data.getXMap(FIELD_DATA));
            }
        } 
    }
    
    public static class DiaoyanQueryResponse extends Response {
    	
    	//调研标题
    	public static final byte FIELD_SURVEY_TITLE = 0x02;
    	
    	//是否参加过本期调研
    	public static final byte FIELD_HAS_SURVEYED = 0x03;
    	
    	//调研URL
    	public static final byte FIELD_URL = 0x04;
    	
    	private String surveyTitle;
		private long hasSurveyed;
		private String url;

		public long getHasSurveyed() {
			return hasSurveyed;
		}
    	
    	public String getSurveyTitle() {
			return surveyTitle;
		}
		
		public String getUrl() {
			return url;
		}
 	
		public DiaoyanQueryResponse(XMap data) throws APIException{
			super(data);
			
			if(this.data.containsKey(FIELD_SURVEY_TITLE)){
				surveyTitle = this.data.getString(FIELD_SURVEY_TITLE);
			}
			
			if(this.data.containsKey(FIELD_HAS_SURVEYED)){
				hasSurveyed = this.data.getInt(FIELD_HAS_SURVEYED);
			}
			
			if(this.data.containsKey(FIELD_URL)){
				url = this.data.getString(FIELD_URL);
			}
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
            
            if (this.data.containsKey(FIELD_DATA)) {
                url = this.data.getString(FIELD_DATA);
            }
        } 
    }
    
    protected void launchTest() {
        super.launchTest();
        String dataType = this.criteria.get(SERVER_PARAMETER_DATA_TYPE);
        if (criteria.containsKey(SERVER_PARAMETER_OPERATION_CODE)) {
            String operationCode = criteria.get(SERVER_PARAMETER_OPERATION_CODE);
            
            if (OPERATION_CODE_CREATE.equals(operationCode)) {
                if (DATA_TYPE_DINGDAN.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDinydanCreateResponse();
                } else if (DATA_TYPE_DIANPING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianpingCreateResponse();
                }
            } if (OPERATION_CODE_QUERY.equals(operationCode)) {
                if (DATA_TYPE_POI.equals(dataType)) {
                    responseXMap = DataOperationTest.launchPOIQueryResponse();
                } else if (DATA_TYPE_TUANGOU.equals(dataType)) {
                    responseXMap = DataOperationTest.launchTuangouQueryResponse(context);
                } else if (DATA_TYPE_FENDIAN.equals(dataType)) {
                    responseXMap = DataOperationTest.launchFendianQueryResponse(context);
                } else if (DATA_TYPE_DIANYING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianyingQueryResponse(context);
                } else if (DATA_TYPE_YANCHU.equals(dataType)) {
                    responseXMap = DataOperationTest.launchYanchuQueryResponse(context);
                } else if (DATA_TYPE_ZHANLAN.equals(dataType)) {
                    responseXMap = DataOperationTest.launchZhanlanQueryResponse(context);
                } else if (DATA_TYPE_DIANPING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianpingQueryResponse();
                } else if (DATA_TYPE_DIAOYAN.equals(dataType)) {
                	responseXMap = DataOperationTest.launchDiaoyanQueryResponse(context);
                }
            } if (OPERATION_CODE_UPDATE.equals(operationCode)) {
                if (DATA_TYPE_DIANPING.equals(dataType)) {
                    responseXMap = DataOperationTest.launchDianpingUpdateResponse();
                }
            }
        }
    }
}

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
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.test.AccountManageTest;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;
import android.text.TextUtils;

public class AccountManage extends BaseQuery {

    // pwd string true 密码sha1
    public static final String SERVER_PARAMETER_PASSWORD = "pwd";

    // vc string true 验证码
    public static final String SERVER_PARAMETER_VALIDATE_CODE = "vc";

    // nick string false 昵称
    public static final String SERVER_PARAMETER_NICKNAME = "nick";

    // oldpwd   string  true    旧密码sha1
    public static final String SERVER_PARAMETER_OLD_PASSWORD = "oldpwd";

    // 操作类型 操作码
    // 绑定手机号验证码 bc
    public static final String OPERATION_CODE_BIND_TELEPHONE = "bc";

    // 新建 cr
    public static final String OPERATION_CODE_CREATE = "cr";

    // 修改昵称 un
    public static final String OPERATION_CODE_UPDATE_NICKNAME = "un";

    // 修改密码 up
    public static final String OPERATION_CODE_UPDATE_PASSWORD = "up";

    // 修改手机号 ut
    public static final String OPERATION_CODE_UPDATE_TELEPHONE = "ut";

    // 重置密码获取验证码 rc
    public static final String OPERATION_CODE_GET_VALIDATE_CODE = "rc";

    // 重置密码 rp
    public static final String OPERATION_CODE_RESET_PASSWORD = "rp";

    // 登录 li
    public static final String OPERATION_CODE_LOGIN = "li";

    // 注销 lo
    public static final String OPERATION_CODE_LOGOUT = "lo";
    
    static final String TAG = "AccountManage";
    
    public AccountManage(Context context) {
        super(context, API_TYPE_ACCOUNT_MANAGE);
    }

    @Override
    protected void makeRequestParameters() throws APIException {
        super.makeRequestParameters();
        if (cityId < MapEngine.CITY_ID_BEIJING) {
            throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_CITY);
        }
        addCommonParameters(requestParameters, cityId);
        if (criteria == null) {
            throw new APIException(APIException.CRITERIA_IS_NULL);
        }
        String sessionId = Globals.g_Session_Id;
        if (criteria.containsKey(SERVER_PARAMETER_OPERATION_CODE)) {
            String operationCode = criteria.get(SERVER_PARAMETER_OPERATION_CODE);
            requestParameters.add(SERVER_PARAMETER_OPERATION_CODE, operationCode);
            if (OPERATION_CODE_BIND_TELEPHONE.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
            } else if (OPERATION_CODE_CREATE.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
                if (criteria.containsKey(SERVER_PARAMETER_PASSWORD)) {
                    requestParameters.add(SERVER_PARAMETER_PASSWORD, criteria.get(SERVER_PARAMETER_PASSWORD));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_PASSWORD);
                }
                if (criteria.containsKey(SERVER_PARAMETER_VALIDATE_CODE)) {
                    requestParameters.add(SERVER_PARAMETER_VALIDATE_CODE, criteria.get(SERVER_PARAMETER_VALIDATE_CODE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_VALIDATE_CODE);
                }
                if (criteria.containsKey(SERVER_PARAMETER_NICKNAME)) {
                    requestParameters.add(SERVER_PARAMETER_NICKNAME, criteria.get(SERVER_PARAMETER_NICKNAME));
                }
            } else if (OPERATION_CODE_UPDATE_NICKNAME.equals(operationCode)) {
                if (!TextUtils.isEmpty(sessionId)) {
                    requestParameters.add(SERVER_PARAMETER_SESSION_ID, sessionId);
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_SESSION_ID);
                }
                if (criteria.containsKey(SERVER_PARAMETER_NICKNAME)) {
                    requestParameters.add(SERVER_PARAMETER_NICKNAME, criteria.get(SERVER_PARAMETER_NICKNAME));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_NICKNAME);
                }
            } else if (OPERATION_CODE_UPDATE_PASSWORD.equals(operationCode)) {
                if (!TextUtils.isEmpty(sessionId)) {
                    requestParameters.add(SERVER_PARAMETER_SESSION_ID, sessionId);
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_SESSION_ID);
                }
                if (criteria.containsKey(SERVER_PARAMETER_OLD_PASSWORD)) {
                    requestParameters.add(SERVER_PARAMETER_OLD_PASSWORD, criteria.get(SERVER_PARAMETER_OLD_PASSWORD));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_OLD_PASSWORD);
                }
                if (criteria.containsKey(SERVER_PARAMETER_PASSWORD)) {
                    requestParameters.add(SERVER_PARAMETER_PASSWORD, criteria.get(SERVER_PARAMETER_PASSWORD));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_PASSWORD);
                }
            } else if (OPERATION_CODE_UPDATE_TELEPHONE.equals(operationCode)) {
                if (!TextUtils.isEmpty(sessionId)) {
                    requestParameters.add(SERVER_PARAMETER_SESSION_ID, sessionId);
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_SESSION_ID);
                }
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
                if (criteria.containsKey(SERVER_PARAMETER_VALIDATE_CODE)) {
                    requestParameters.add(SERVER_PARAMETER_VALIDATE_CODE, criteria.get(SERVER_PARAMETER_VALIDATE_CODE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_VALIDATE_CODE);
                }
                if (criteria.containsKey(SERVER_PARAMETER_PASSWORD)) {
                    requestParameters.add(SERVER_PARAMETER_PASSWORD, criteria.get(SERVER_PARAMETER_PASSWORD));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_PASSWORD);
                }
            } else if (OPERATION_CODE_GET_VALIDATE_CODE.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
            } else if (OPERATION_CODE_RESET_PASSWORD.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
                if (criteria.containsKey(SERVER_PARAMETER_PASSWORD)) {
                    requestParameters.add(SERVER_PARAMETER_PASSWORD, criteria.get(SERVER_PARAMETER_PASSWORD));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_PASSWORD);
                }
                if (criteria.containsKey(SERVER_PARAMETER_VALIDATE_CODE)) {
                    requestParameters.add(SERVER_PARAMETER_VALIDATE_CODE, criteria.get(SERVER_PARAMETER_VALIDATE_CODE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_VALIDATE_CODE);
                }
            } else if (OPERATION_CODE_LOGIN.equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
                if (criteria.containsKey(SERVER_PARAMETER_PASSWORD)) {
                    requestParameters.add(SERVER_PARAMETER_PASSWORD, criteria.get(SERVER_PARAMETER_PASSWORD));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_PASSWORD);
                }
            } else if (OPERATION_CODE_LOGOUT.equals(operationCode)) {
                if (!TextUtils.isEmpty(sessionId)) {
                    requestParameters.add(SERVER_PARAMETER_SESSION_ID, sessionId);
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_SESSION_ID);
                }
            } else if ("du".equals(operationCode)) {
                if (criteria.containsKey(SERVER_PARAMETER_TELEPHONE)) {
                    requestParameters.add(SERVER_PARAMETER_TELEPHONE, criteria.get(SERVER_PARAMETER_TELEPHONE));
                } else {
                    throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_TELEPHONE);
                }
            } else {
                throw APIException.wrapToMissingRequestParameterException("operationCode invalid.");
            }
        } else {
            throw APIException.wrapToMissingRequestParameterException(SERVER_PARAMETER_OPERATION_CODE);
        }
    }
    
    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.getAccountManageUrl(), TKConfig.getAccountManageHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);

        String operationCode = criteria.get(SERVER_PARAMETER_OPERATION_CODE);
        if (OPERATION_CODE_CREATE.equals(operationCode)) {
            UserRespnose userResponse = new UserRespnose(responseXMap);
            this.response = userResponse;
        } else if (OPERATION_CODE_LOGIN.equals(operationCode)) {
            UserRespnose userResponse = new UserRespnose(responseXMap);
            this.response = userResponse;
        } else {
            response = new Response(responseXMap);
        }
    }
    
    public static class UserRespnose extends Response {

        // 0x02 x_string session id
        public static final byte FIELD_SESSION_ID = 0x02;

        // 0x03 x_string nickname
        public static final byte FIELD_NICKNAME = 0x03;

        // 0x04 x_int userId
        public static final byte FIELD_USER_ID = 0x04;

        // 0x05 x_int timeout
        public static final byte FIELD_TIMEOUT = 0x05;

        private String sessionId;

        private String nickname;

        private long userId = Long.MIN_VALUE;

        private long timeout;

        public UserRespnose(XMap data) throws APIException {
            super(data);
            
            if (this.data.containsKey(FIELD_SESSION_ID)) {
                this.sessionId = this.data.getString(FIELD_SESSION_ID);
            }
            
            if (this.data.containsKey(FIELD_NICKNAME)) {
                this.nickname = this.data.getString(FIELD_NICKNAME);
            }
            
            if (this.data.containsKey(FIELD_USER_ID)) {
                this.userId = this.data.getInt(FIELD_USER_ID);
            }
            
            if (this.data.containsKey(FIELD_TIMEOUT)) {
                this.timeout = this.data.getInt(FIELD_TIMEOUT);
            }
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return super.toString() + " sessionId: " + sessionId + " nickname: " + nickname 
					+ " userId: " + userId + " timeout: " + timeout;
		}
        
    }
    
    public String getOperationCode() {
    	return criteria.get(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
    }
    
    protected void launchTest() {
        super.launchTest();
        String operationCode = this.criteria.get(SERVER_PARAMETER_OPERATION_CODE);
        if (OPERATION_CODE_CREATE.equals(operationCode)) {
            responseXMap = AccountManageTest.launchUserRespnose(context);
        } else if (OPERATION_CODE_LOGIN.equals(operationCode)) {
            responseXMap = AccountManageTest.launchUserRespnose(context);
        } else {
            responseXMap = BaseQueryTest.launchResponse();
        }
    }
}

/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

import android.text.TextUtils;

/**
 * present a poi record.
 * 
 * @author peng wenyue
 * @version
 */
public class Response extends XMapData {
    
    // 200 成功
    public static final int RESPONSE_CODE_OK = 200;

    // 201 操作成功，但是相关POI已失效
    public static final int RESPONSE_CODE_POI_INVALID = 201;

    // 202  服务器成功响应，但数据库插入点评操作失败
    public static final int RESPONSE_CODE_INSERT_FAILED = 202;

    // 203  服务器成功响应，但数据库更新点评操作失败 
    public static final int RESPONSE_CODE_UPDATE_FAILED = 203;
    
    // 300 session失效
    public static final int RESPONSE_CODE_SESSION_INVALID = 300;

    // 301 用户已在别处登录
    public static final int RESPONSE_CODE_LOGON_EXIST = 301;

    // 400 注册/修改手机号时，手机号已存在
    public static final int RESPONSE_CODE_MOBILE_PHONE_EXIST = 400;

    // 401 注册/修改昵称时，昵称已被使用
    public static final int RESPONSE_CODE_NICKNAME_EXIST = 401;

    // 402 注册/修改手机/重置密码时，验证码错误
    public static final int RESPONSE_CODE_SECURITY_CODE_ERROR = 402;

    // 403 手机号不存在
    public static final int RESPONSE_CODE_NO_MOBILE_PHONE = 403;

    // 404 密码错误
    public static final int RESPONSE_CODE_PASWORD_ERROR = 404;

    // 405 修改密码时，原始密码错误
    public static final int RESPONSE_CODE_ORIGINAL_PASSWORD_ERROR = 405;

    // 406 无效的手机号（一般是格式错误） 
    public static final int RESPONSE_CODE_MOBILE_PHONE_INVALID = 406;

    // 407 参数非法或缺少参数
    public static final int RESPONSE_CODE_INVALID_PARAMETER = 407;

    // 408 缺少参数
    public static final int RESPONSE_CODE_MISSING_PARAMETER = 408;

    // 410 请求的数据不存在（已过时）
    public static final int RESPONSE_CODE_REQUEST_DATA_OUT = 410;

    // 500 服务器错误
    public static final int RESPONSE_CODE_SERVER_ERROR = 500;

    // 503 服务器短信发送失败 
    public static final int RESPONSE_CODE_SEND_SMS_FAILED = 503;

    // 504  短信长度超过限制 
    public static final int RESPONSE_CODE_SEND_SMS_OVERLENGTH = 504;

    // 601 该用户已点评过该POI
    public static final int RESPONSE_CODE_COMMENT_ON_POI = 601;
    
    // 602 该点评不存在
    public static final int RESPONSE_CODE_COMMENT_NO_EXIST = 602;

    // 603 该poi不存在或已失效
    public static final int RESPONSE_CODE_POI_NO_EXIST = 603;

    // 604 匿名用户不能更新点评
    public static final int RESPONSE_CODE_COMMENT_NO_UPDATE = 604;

    // 700 本城市没有所请求的发现类型（如小城市没有任何团购信息，ps：客户端应通过发现首页避免该类请求的产生）
    public static final int RESPONSE_CODE_DISCOVER_NO_SUPPORT = 700;

    // 701 发现数据已更新（在列表页请求具有一定偏移量的结果时，服务器切换了数据库，客户端需给出提示“需重新发请求”）
    public static final int RESPONSE_CODE_DISCOVER_REQUEST = 701;
    
    // 801 团购订单创建失败
    public static final int RESPONSE_CODE_801 = 801;
    
    // 802 无效的团购商家id
    public static final int RESPONSE_CODE_802 = 802;
    
    // 821 酒店订单提交失败
    public static final int RESPONSE_CODE_HOTEL_ORDER_CREATE_FAILED = 821;
    
    // 822 需要注册酒店会员
    public static final int RESPONSE_CODE_HOTEL_NEED_REGIST = 822;
    
    // 823 注册酒店会员失败
    public static final int RESPONSE_CODE_HOTEL_REGIST_MEMBER_FAILED = 823;
    
    // 824 获取房型动态信息失败
    public static final int RESPONSE_CODE_HOTEL_ROOMTYPE_DYNAMIC_FAILED = 824;
    
    // 825 需要信用卡担保
    public static final int RESPONSE_CODE_HOTEL_NEED_CREDIT_ASSURE = 825;
    
    // 826 艺龙返回其他错误信息
    public static final int RESPONSE_CODE_HOTEL_OTHER_ERROR = 826;
    
    // 831 取消订单失败（得到了艺龙的回复，但是因为某些原因失败了）
    public static final int RESPONSE_CODE_HOTEL_ORDER_CANCEL_FAILED = 831;
    
    // 0x00 x_int   响应状态码 
    public static final byte FIELD_RESPONSE_CODE = 0x00;
    
    // 0x01 x_string    状态描述
    public static final byte FIELD_DESCRIPTION = 0x01;
    
    protected long responseCode = 0; // 0x00 x_int   响应状态码 
    
    protected String description; // 0x01 x_string    状态描述

    public Response(XMap data) throws APIException {
        super(data);
        this.responseCode = getLongFromData(FIELD_RESPONSE_CODE);
        this.description = getStringFromData(FIELD_DESCRIPTION);
    }
    
    public XMap getData() {
        if (this.data == null) {
            this.data = new XMap();            
            this.data.put(FIELD_RESPONSE_CODE, this.responseCode);
            if (!TextUtils.isEmpty(this.description)) {
                this.data.put(FIELD_DESCRIPTION, this.description);
            }
        }
        return data;
    }

    public int getResponseCode() {
        return (int)responseCode;
    }

    public String getDescription() {
        return description;
    }
}

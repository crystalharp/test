/*
 * @(#)POI.java 5:30:43 PM Aug 26, 2007 2007
 * 
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd. All rights
 * reserved.
 * 
 */

package com.tigerknows.model;

public class Dingdan {
    
    // id int 订单号 0x01
    public static final byte FIELD_ID = 0x01;

    // pname string 商品名称 0x03
    public static final byte FIELD_PNAME = 0x03;

    // uid string 商品uid 0x04
    public static final byte FIELD_UID = 0x04;

    // count int 数量 0x06
    public static final byte FIELD_COUNT = 0x06;

    // userid int 用户id 0x08
    public static final byte FIELD_USERID = 0x08;

    // state String 订单状态（比如未支付，支付） 0x09
    public static final byte FIELD_STATE = 0x09;

    // create_time datetime 订单生成时间 0x0a
    public static final byte FIELD_CREATE_TIME = 0x0a;

    // sj_id int 数据来源（商家标识） 0x0b
    public static final byte FIELD_SJ_ID = 0x0b;

    // type int 订单类型(如团购) 0x0c
    public static final byte FIELD_TYPE = 0x0c;
    
    public static final String TYPE_TUANGOU = "1";
}

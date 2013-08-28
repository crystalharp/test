/*
 * @(#)Appendix.java  下午02:34:06 2009-1-19 2009
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */
package com.tigerknows.model.response;

import java.io.IOException;

import com.tigerknows.util.ParserUtil;

/**
 * 本类表征一个服务器返回响应中的一条附加信息.
 * 
 * @author pengjunjie
 */
public abstract class Appendix {

    /**
     * 附加信息的分类，对应无效值.
     */
    public static final int TYPE_INVALID = 0;   
    /**
     * 附加信息的分类，对应UUID.
     */
    public static final int TYPE_UUID = 1;
    /**
     * 附加信息的分类，对应文字广告.
     */
    public static final int TYPE_AD_TEXT = 2;
    /**
     * 航班预订电话.
     */
    public static final int TYPE_FLIGHT_TELNO = 5;
    
    /**
     * sp configuration.
     */
    public static final int TYPE_SP_CONFIG = 7;
    /**
     * 服务器端时间（北京时间）
     */
    public static final int TYPE_SERVER_TIME = 8;
    
    /**
     * 更新软件版本信息
     */
    public static final int TYPE_UPDATE_VERSION_DATA = 12;
    
    /**
     * 通用响应码(必选)
     */
    public static final int TYPE_RESPONSE_CODE = 21;
    
    /**
     * 地图下载服务器域名
     */
    public static final int TYPE_DOWNLOAD_DOMAIN = 23;
    
    /**
     * 定位信息
     */
    public static final int TYPE_POSITION = 24;
    
    /**
     * last region map data version
     */
    public static final int TYPE_MAP_VERSION = 0x28;
    
    /**
     * User action track switch
     */
    public static final int TYPE_USER_ACTION_TRACK_SWITCH = 38;
    
    /**
     * 通用原始数据包(可选) 
     */
    public static final int TYPE_DATA_PACKAGE = 8193;
    
    public int type = 0;
   
    /**
     * 解析返回的结果.
     * @param util
     *       响应头.<warning>应当已读入了本条附加信息类型码与长度.</warning>
     * @param encoding
     *      编码方式.
     * @throws IOException
     *      出现了IO异常，本条附加信息无效.      
     */
    public abstract void parse(ParserUtil util) throws IOException;
}

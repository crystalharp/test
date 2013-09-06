/*
 * @(#)VersionData.java  下午04:24:39 2010-4-16 2010
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */
package com.tigerknows.model.response;

import java.io.IOException;

import com.tigerknows.util.ParserUtil;

/**
 * 通用响应码.
 *
 * @author Administrator
 */
public class ResponseCode extends Appendix {
    
    // 联想词
    // 响应码(十进制) 语义 例子  
    public static final int LEXICON_OK = 200; // 处理成功
    public static final int LEXICON_NOT_FOUND = 404; // 某城市无词库文件，或找不到目标修订号的词库文件
    public static final int LEXICON_LATEST = 204; // lr已经为最新版本
    public static final int LEXICON_BAD_REQUEST = 400; // rs为非法值   

    // 边看边下载meta数据
    // 响应码(十进制) 语义 例子  
    public static final int MAP_META_OK = 200; // 请求处理成功
    public static final int MAP_META_NOT_FOUND = 404; // 提交的rid没有数据
    public static final int MAP_META_BAD_REQUEST = 400; // 缺少参数或参数非法
    public static final int MAP_META_NEW_REVISION = 401; // 数据版本更新，这时通用原始数据包中的数据为新的版本号

    // 边看边下载tile数据
    // 响应码(十进制) 语义 例子  
    public static final int MAP_TILE_OK = 200; // 请求处理成功
    public static final int MAP_TILE_NOT_FOUND = 404; // 提交的rid没有数据 
    public static final int MAP_TILE_BAD_REQUEST = 400; // 缺少参数或参数非法
    public static final int MAP_TILE_NEW_REVISION = 401; // 数据版本更新，这时通用原始数据包中的数据为新的版本号

    // 边看边下载region版本查询
    // 响应码(十进制) 语义 例子  
    public static final int MAP_REGION_VERSION_OK = 200; // 请求处理成功
    public static final int MAP_REGION_VERSION_NOT_FOUND = 400; // 缺少参数或参数非法

    private int responseCode;

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        responseCode = util.readIntFromTwoBytes();
    }
    
    public int getResponseCode() {
        return responseCode;
    }
}

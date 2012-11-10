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
 * 表征版本信息.
 *
 * @author Administrator
 */
public class DownloadDomainCake extends Appendix {
    
    /*
     * 字段名 类型 含义  
        cake_id uint2 cake类型id 0x17  
        length uint2 后续信息长度  
        data_version array<string> 地图下载服务器域名列表，客户端要按照列表顺序进行尝试下载，在服务端无故障的情况下，只需要对列表的第一项进行尝试  
                    
                    无此cake时 默认下载服务器为 download.tigerknows.net 
     */
    
    
    private String[] domains;

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        int size = util.readIntFromTwoBytes();
        domains = new String[size];
        for(int i = 0; i < size; i++) {
            domains[i] = util.transferString();
        }
    }
    
    public String[] getDataVersion() {
        return domains;
    }
}

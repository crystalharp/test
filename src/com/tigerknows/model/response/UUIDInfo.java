/*
 * @(#)UUIDInfo.java  下午02:41:15 2009-1-19 2009
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */
package com.tigerknows.model.response;

import java.io.IOException;

import com.tigerknows.util.ParserUtil;

/**
 * 表征一个从响应中解析出来的UUID.
 * @author pengjunjie
 */
public class UUIDInfo extends Appendix {
    private String uuid;

    /**
     * 获取UUID.
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        String uuid = util.transferString();
        this.uuid = uuid;
    }
}

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
 *
 * @author Pengwenyue
 */
public class UserActionTrackSwitch extends Appendix {
    
    /*
                  字段名   类型  含义
        cake_id uint2   cake类型id 0x26 十进制38
        length  uint2   后续信息长度
        user_action_track_switch    byte    0不记录用户行为，1记录全部用户行为并上传
     */

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        util.readIntFromOneByte();
    }
}

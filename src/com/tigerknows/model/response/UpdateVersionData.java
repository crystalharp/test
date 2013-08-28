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
public class UpdateVersionData extends Appendix {
    private String versionNo;
    private String updateURL;
    private String description;
    private int fileSize;

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        updateURL = util.transferString();
        description = util.transferString();
        versionNo = util.transferString();
        fileSize = util.readIntFromTwoBytes();
    }

    public String getDescription() {
        return description;
    }

    public String getUpdateURL() {
        return updateURL;
    }
    
    public int getFileSize() {
        return fileSize;
    }
    
    public String getVersionNo() {
        return versionNo;
    }
}

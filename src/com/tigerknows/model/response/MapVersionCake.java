/*
 * @(#)MapVersionCake.java   2011-1-7 2011
 *
 * Copyright (C) 2007 Beijing TigerKnows Science and Technology Ltd.
 * All rights reserved.
 *
 */
package com.tigerknows.model.response;

import java.io.IOException;
import java.util.HashMap;

import com.tigerknows.maps.MapEngine.RegionMetaVersion;
import com.tigerknows.model.MapVersionQuery.RegionDataInfo;
import com.tigerknows.util.ParserUtil;

/**
 * last map region version.
 *
 * @author zhouwentao
 */
public class MapVersionCake extends Appendix {
    
    /*
         类型码   type   lat   lon  accuracy  
       0x00 0x18 uint2 uint4 uint4 uint4  
       类型码  信息长度    type   lat*10e6 lon*10e6 accuracy
     0x00 0x18   uint2   uint2   uint4   uint4   uint4 
     区域数据版本Cake :
      类型码     信息长度    数据
    0x00 0x19   uint2   array<regionVersion>
    
    regionVersion:
    regionID 主版本号  次版本号    年      月      日
      uint2   uint1   uint1   uint2   uint1   uint1
      
      info:(v2)
regionID    主版本号    次版本号    年   月   日   数据大小
uint2   uint1   uint1   uint2   uint1   uint1   unit4 
    
    regionID = 0xfffd 表示-3
    无效的时候版本号和年月日全为0
     */
    
    private HashMap<Integer, RegionDataInfo> mRegionDataInfos;
    
    public MapVersionCake(final int lenth) {
    }

    /* (non-Javadoc)
     * @see com.tigerknows.map.models.response.Appendix#parse(com.tigerknows.util.ParserUtil, java.lang.String)
     */
    public void parse(ParserUtil util) throws IOException {
        int num = util.readIntFromTwoBytes();
        util.advance(2);
        mRegionDataInfos = new HashMap<Integer, RegionDataInfo>(num);
        for (int i=0; i<num; i++) {
            int regionID = util.readIntFromTwoBytes();
            if (regionID == 0xfffd) {
                regionID = -3;
            }
            int mainVersion = util.readIntFromOneByte();
            int secondVersion = util.readIntFromOneByte();
            int year = util.readIntFromTwoBytes();
            int month = util.readIntFromOneByte();
            int day = util.readIntFromOneByte();
            RegionMetaVersion version = new RegionMetaVersion(mainVersion, secondVersion, year, month, day);
            int totalSize = util.readIntFromFourBytes();
            RegionDataInfo regionDataInfo = new RegionDataInfo(version.toString(), totalSize);
            mRegionDataInfos.put(regionID, regionDataInfo);
        }
    }
    
    public HashMap<Integer, RegionDataInfo> getRegionVersionMap() {
        return mRegionDataInfos;
    }
}

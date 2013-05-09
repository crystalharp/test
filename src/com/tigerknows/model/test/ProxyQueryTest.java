package com.tigerknows.model.test;

import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic.DanbaoGuize;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic.DanbaoGuize.RetentionTime;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;

public class ProxyQueryTest {

    public static XMap launchRoomTypeDynamicRespnose(Context context) {
        XMap xmap = BaseQueryTest.launchResponse();
        xmap.put(RoomTypeDynamic.FILED_NUM, 168);
        xmap.put(RoomTypeDynamic.FILED_PRICE, 168.168);
        xmap.put(RoomTypeDynamic.FILED_DANBAO_GUIZE, launchDanbaoGuize());
        return null;
    }

    public static XMap launchDanbaoGuize() {
        XMap xmap = new XMap();
        xmap.put(DanbaoGuize.FILED_NUM, 1);
        XArray<XMap> xarray = new XArray<XMap>();
        for(int i = 0; i < 9; i++) {
            xarray.add(launchRetentionTime(i));
        }
        xmap.put(DanbaoGuize.FILED_NUM_LESS, xarray);
        xarray = new XArray<XMap>();
        for(int i = 0; i < 9; i++) {
            xarray.add(launchRetentionTime(i));
        }
        xmap.put(DanbaoGuize.FILED_NUM_GREATER, xarray);
        return xmap;
    }

    public static XMap launchRetentionTime(int i) {
        XMap xmap = new XMap();
        xmap.put(RetentionTime.FILED_TIME, "9999-13-32 25:61:1"+i);
        xmap.put(RetentionTime.FILED_NEED, RetentionTime.NEED_YES);
        xmap.put(RetentionTime.FILED_TYPE, RetentionTime.TYPE_ALL);
        return xmap;
    }
    
}

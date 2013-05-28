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
        xmap.put(RoomTypeDynamic.FIELD_NUM, 168);
        xmap.put(RoomTypeDynamic.FIELD_PRICE, 168.168);
        xmap.put(RoomTypeDynamic.FIELD_DANBAO_GUIZE, launchDanbaoGuize());
        return xmap;
    }
    
    public static XMap launchDanbaoGuize() {
        XMap xmap = new XMap();
        xmap.put(DanbaoGuize.FIELD_NUM, 1);
        XArray<XMap> xarray = new XArray<XMap>();
        for(int i = 0; i < 5; i++) {
            xarray.add(launchRetentionTime(i));
        }
        xmap.put(DanbaoGuize.FIELD_NUM_LESS, xarray);
        xarray = new XArray<XMap>();
        for(int i = 0; i < 5; i++) {
            xarray.add(launchRetentionTime(i));
        }
        xmap.put(DanbaoGuize.FIELD_NUM_GREATER, xarray);
        return xmap;
    }

    public static XMap launchRetentionTime(int i) {
        XMap xmap = new XMap();
        xmap.put(RetentionTime.FIELD_TIME, "2"+i+"点之前");
        if(i < 2){
        	xmap.put(RetentionTime.FIELD_NEED, RetentionTime.NEED_NO);
        }else{
        	xmap.put(RetentionTime.FIELD_NEED, RetentionTime.NEED_YES);
        	if(i < 4){
        		xmap.put(RetentionTime.FIELD_TYPE, RetentionTime.TYPE_FIRST_NIGHT);
        	}else{
        		xmap.put(RetentionTime.FIELD_TYPE, RetentionTime.TYPE_ALL);
        	}
        }
        xmap.put(RetentionTime.FIELD_TIME_DETAIL, "9999-13-32 2"+i+":61:00");
        
        return xmap;
    }
    
}

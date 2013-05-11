package com.tigerknows.model.test;

import com.tigerknows.model.DataOperation.CommentCreateResponse;
import com.tigerknows.model.DataOperation.CommentQueryResponse;
import com.tigerknows.model.DataOperation.CommentUpdateResponse;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.DataOperation.DiaoyanQueryResponse;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.HotelOrderCreateResponse;
import com.tigerknows.model.DataOperation.HotelOrderStatesResponse;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.DataOperation.YingxunQueryResponse;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import android.R.integer;
import android.content.Context;

public class DataOperationTest {

    public static XMap launchTuangouQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(TuangouQueryResponse.FIELD_DATA, DataQueryTest.launchTuangou(context, 
                "tuangou",
                "`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,." +
                "`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,.",
                "`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,." +
                "`1234567890-=  qwertyuiop[]asdfghjkl;'zxcvbnm,.",
                true));
        return data;
    }

    public static XMap launchFendianQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(FendianQueryResponse.FIELD_DATA, DataQueryTest.launchFendian("fendian"));
        return data;
    }

    public static XMap launchDianyingQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(DianyingQueryResponse.FIELD_DATA, DataQueryTest.launchDianying(context, "tuangou", "descrption"));
        return data;
    }

    public static XMap launchYingxunQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(YingxunQueryResponse.FIELD_DATA, DataQueryTest.launchYingxun("launchYingxun"));
        return data;
    }

    public static XMap launchYanchuQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(YanchuQueryResponse.FIELD_DATA, DataQueryTest.launchYanchu(context, "yanchu", "descrption"));
        return data;
    }

    public static XMap launchZhanlanQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(ZhanlanQueryResponse.FIELD_DATA, DataQueryTest.launchZhanlan(context, "zhanlan", "descrption"));
        return data;
    }

    public static XMap launchDinydanCreateResponse() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(ZhanlanQueryResponse.FIELD_DATA, "http://www.tigerknows.com");
        return data;
    }

    public static XMap launchDianpingCreateResponse() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(CommentCreateResponse.FIELD_UID, "FIELD_UID");
        data.put(CommentCreateResponse.FIELD_TIME_STAMP, "FIELD_TIME_STAMP");
        return data;
    }

    public static XMap launchDianpingUpdateResponse() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(CommentUpdateResponse.FIELD_TIME_STAMP, "FIELD_TIME_STAMP");
        return data;
    }

    public static XMap launchDianpingQueryResponse() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(CommentQueryResponse.FIELD_COMMENT, DataQueryTest.launchDianping("comment"));
        return data;
    }
    

    public static XMap launchPOIQueryResponse() {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(POIQueryResponse.FIELD_POI, DataQueryTest.launchPOI("poi"));
        return data;
    }
    
    public static XMap launchHotelPOIQueryResponse(){
    	XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(POIQueryResponse.FIELD_POI, DataQueryTest.launchHotelPOI("HotelPOI"));
        return data;
    }

    public static XMap launchDiaoyanQueryResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(DiaoyanQueryResponse.FIELD_HAS_SURVEYED, 0);
        data.put(DiaoyanQueryResponse.FIELD_URL, "http://www.tigerknows.com");
        return data;
    }

	public static XMap launchHotelOrderStateResponse(Context context, String ids) {
		int size = ids.split("_").length;
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        XArray<Integer> xArray = new XArray<Integer>();
        for (int i = 0; i < size; i++) {
			xArray.add(i&5+1);
		}
        data.put(HotelOrderStatesResponse.FIELD_STATES, xArray);
		return data;
	}
	
	public static XMap launchHotelOrderCreateResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(HotelOrderCreateResponse.FIELD_ORDER_ID, "123456789");
		return data;
	}
	
}

package com.tigerknows.model.test;

import com.tigerknows.model.DataOperation.CommentCreateResponse;
import com.tigerknows.model.DataOperation.CommentQueryResponse;
import com.tigerknows.model.DataOperation.CommentUpdateResponse;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;
import com.tigerknows.model.xobject.XMap;

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
}

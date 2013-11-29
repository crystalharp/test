package com.tigerknows.model.test;

import com.tigerknows.R;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.model.AccountManage.VerifyCodeResponse;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;


public class AccountManageTest {
    
    public static final String SESSION_ID = "001283963xxUrV2YLuEBviwxN2Kpixry";

    public static XMap launchUserRespnose(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(UserRespnose.FIELD_SESSION_ID, SESSION_ID);
        data.put(UserRespnose.FIELD_NICKNAME, context.getString(R.string.default_nick_name));
        data.put(UserRespnose.FIELD_USER_ID, 168);
        data.put(UserRespnose.FIELD_TIMEOUT, 0);
        return data;
    }
    public static XMap launchVerifyCodeResponse(Context context) {
        XMap data = new XMap();
        BaseQueryTest.launchResponse(data);
        data.put(VerifyCodeResponse.FIELD_DIALOG_MESSAGE, "FIELD_DIALOG_MESSAGE");
        return data;
    }
}

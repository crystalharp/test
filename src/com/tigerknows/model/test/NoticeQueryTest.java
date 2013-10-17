package com.tigerknows.model.test;

import android.content.Context;

import com.tigerknows.R;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult.Notice;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

public class NoticeQueryTest {
	
	private static final String USER_SURVEY_URL = "http://192.168.11.236/magina-site/20130809-2374068165e.php?sid=20130809-2374068165e&uid=0dd4f252&checksum=862dc540049d9757a45c1dddf1aab0d8&city=%E5%8C%97%E4%BA%AC&vs=5.00.20130725I";
    
    public static final String ICON_URL = "http://www.tigerknows.com/wp-content/uploads/2011/05/features-0";
	
	public static XMap launchNoticeResultResponse(Context context) {
		XMap xmap = BaseQueryTest.launchResponse();
		xmap.put(NoticeResultResponse.FIELD_NOTICE_RESULT, launchNoticeResult(context));
		return xmap;
	}

	public static XMap launchNoticeResult(Context context) {
		XMap xmap = new XMap();
		xmap.put(NoticeResult.FIELD_NUM, 2);
		XArray<XMap> xarray = new XArray<XMap>();
		for(int i = 0; i < 8; i++){
			xarray.add(launchNotice(i, context));
		}
		xmap.put(NoticeResult.FIELD_NOTICE, xarray);
		xmap.put(NoticeResult.FIELD_NOTE, "FIELD_NOTE");
		return xmap;
	}

	public static XMap launchNotice(int op, Context context) {
		XMap xmap = new XMap();
		xmap.put(Notice.FIELD_NOTICE_ID, (long)op);
		xmap.put(Notice.FIELD_OPERATION_TYPE, (op == 0)? 2 : 0);
		if( (op & 1)!=0){
			xmap.put(Notice.FIELD_NOTICE_TITLE, "FIELD_NOTICE_TITLE");
		}else if(op ==0){
			xmap.put(Notice.FIELD_NOTICE_TITLE, context.getString(R.string.message_tip_user_survey));
		}
		if( (op & 4)!=0){
			if(op == 7){
				xmap.put(Notice.FIELD_NOTICE_DESCRIPTION, "iPhone5, iPad mini, 点评,一二三四五六七八九十");
			}else{
				xmap.put(Notice.FIELD_NOTICE_DESCRIPTION, "FIELD_NOTICE_DESCRIPTION");
			}
		}
		if(op == 2){
			xmap.put(Notice.FIELD_PIC_URL, BaseQueryTest.PIC_URL);
		}else if( (op & 2)!=0){
			xmap.put(Notice.FIELD_PIC_URL, ICON_URL + (op-2) + ".png");
		}
		if(op == 0){
			xmap.put(Notice.FIELD_URL, USER_SURVEY_URL);
			xmap.put(Notice.FIELD_WEB_TITLE, context.getString(R.string.user_survey));
		}else {
			xmap.put(Notice.FIELD_URL, BaseQueryTest.URL);
			xmap.put(Notice.FIELD_WEB_TITLE, context.getString(R.string.app_name) + context.getString(R.string.home));
		}
		return xmap;
	}

}

package com.tigerknows.model.test;

import android.content.Context;

import com.tigerknows.R;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult.Notice;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

public class NoticeQueryTest {
	
	public static XMap launchNoticeResultResponse(Context context) {
		XMap xmap = BaseQueryTest.launchResponse();
		xmap.put(NoticeResultResponse.FIELD_NOTICE_RESULT, launchNoticeResult(context));
		return xmap;
	}

	public static XMap launchNoticeResult(Context context) {
		XMap xmap = new XMap();
		xmap.put(NoticeResult.FIELD_NUM, 4);
		XArray<XMap> xarray = new XArray<XMap>();
		for(int i = 0; i < 4; i++){
			xarray.add(launchNotice(i, context));
		}
		xmap.put(NoticeResult.FIELD_NOTICE, xarray);
		xmap.put(NoticeResult.FIELD_NOTE, "FIELD_NOTE");
		return xmap;
	}

	public static XMap launchNotice(int op, Context context) {
		XMap xmap = new XMap();
		xmap.put(Notice.FIELD_OPERATION_TYPE, (long)op);
		xmap.put(Notice.FIELD_NOTICE_DESCRIPTION, "FIELD_NOTICE_DESCRIPTION");
		xmap.put(Notice.FIELD_URL, BaseQueryTest.URL);
		xmap.put(Notice.FIELD_PIC_URL, BaseQueryTest.PIC_URL);
		xmap.put(Notice.FIELD_WEB_TITLE, context.getString(R.string.app_name) + context.getString(R.string.home));
		return xmap;
	}

}

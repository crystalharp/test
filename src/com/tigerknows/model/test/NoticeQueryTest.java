package com.tigerknows.model.test;

import android.content.Context;

import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse.NoticeResult.Notice;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

public class NoticeQueryTest {
	
	public static XMap launchNoticeResultResponse(Context context) {
		XMap xmap = BaseQueryTest.launchResponse();
		xmap.put(NoticeResultResponse.FIELD_NOTICE_RESULT, launchNoticeResult());
		return xmap;
	}

	public static XMap launchNoticeResult() {
		XMap xmap = new XMap();
		xmap.put(NoticeResult.FIELD_NUM, 4);
		XArray<XMap> xarray = new XArray<XMap>();
		for(int i = 0; i < 4; i++){
			xarray.add(launchNotice(i));
		}
		xmap.put(NoticeResult.FIELD_NOTICE, xarray);
		xmap.put(NoticeResult.FIELD_NOTE, "FIELD_NOTE");
		return xmap;
	}

	public static XMap launchNotice(int op) {
		XMap xmap = new XMap();
		xmap.put(Notice.FIELD_OPERATION_TYPE, (long)op);
		xmap.put(Notice.FIELD_NOTICE_DESCRIPTION, "FIELD_NOTICE_DESCRIPTION");
		xmap.put(Notice.FIELD_URL, BaseQueryTest.URL);
		xmap.put(Notice.FIELD_PIC_URL, BaseQueryTest.PIC_URL);
		return xmap;
	}

}

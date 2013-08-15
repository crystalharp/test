package com.tigerknows.model.test;

import java.util.Calendar;

import com.tigerknows.model.HotelOrderOperation.HotelOrderCreateResponse;
import com.tigerknows.model.HotelOrderOperation.HotelOrderStatesResponse;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import android.content.Context;

public class HotelOrderOperationTest {

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
        data.put(HotelOrderCreateResponse.FIELD_DESCRIPTION, Calendar.getInstance().getTimeInMillis() + 120000);
		return data;
	}
}

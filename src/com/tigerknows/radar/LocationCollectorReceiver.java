package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.Setting;
import com.tigerknows.service.LocationCollectionService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LocationCollectorReceiver extends BroadcastReceiver{
    
    static final String TAG = "LocationCollectorReceiver";

    public static final String ACTION_LOCATION_COLLECTION = "action.com.tigerknows.radar.LocationCollectionReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        LogWrapper.d(TAG, "onReceive() " + intent);
        if (Setting.radarOn(context)) {
            Intent service = new Intent(context, LocationCollectionService.class);
            context.startService(service);
        }
    }
}



package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.service.LocationCollectionService;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RadarReceiver extends BroadcastReceiver{
    
    static final String TAG = "RadarReceiver";

    public static final String ACTION_PULL = "action.com.tigerknows.radar.PullReceiver";

    public static final String ACTION_LOCATION_COLLECTION = "action.com.tigerknows.radar.LocationCollectionReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        LogWrapper.d(TAG, "onReceive() " + intent);
        String action = intent.getAction();
        if (ACTION_PULL.equals(action)){
            Intent service = new Intent(context, PullService.class);
            context.startService(service);
        } else if (ACTION_LOCATION_COLLECTION.equals(action)){
            Intent service = new Intent(context, LocationCollectionService.class);
            context.startService(service);
        }
    }
}



package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class RadarReceiver extends BroadcastReceiver{
    
    static final String TAG = "RadarReceiver";

    public static final String ACTION_PULL = "action.com.tigerknows.radar.PullReceiver";

    public static final String ACTION_LOCATION_COLLECTION = "action.com.tigerknows.radar.LocationCollectionReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        LogWrapper.d(TAG, "onReceive() " + intent);
        if (TextUtils.isEmpty(TKConfig.getPref(context, TKConfig.PREFS_RADAR_PULL_SERVICE_SWITCH, ""))) {
            Intent service = new Intent(context, PullService.class);
            context.startService(service);
        }
    }
}



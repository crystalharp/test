package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.service.PullService;
import com.tigerknows.ui.more.SettingActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class RadarReceiver extends BroadcastReceiver{
    
    static final String TAG = "RadarReceiver";

    public static final String ACTION_PULL = "action.com.tigerknows.radar.PullReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        LogWrapper.d(TAG, "onReceive() " + intent);
        if (SettingActivity.radarOn(context)) {
            Intent service = new Intent(context, PullService.class);
            context.startService(service);
        }
    }
}



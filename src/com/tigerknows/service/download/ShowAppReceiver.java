package com.tigerknows.service.download;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.common.ActionLog;
import com.tigerknows.radar.AppPushNotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShowAppReceiver extends BroadcastReceiver{
    
    static final String TAG = "ShowAppReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        LogWrapper.d(TAG, "onReceive() " + intent);
        if (intent != null && intent.getData() != null) {
        	LogWrapper.d(TAG, "onReceived() " + intent);
            AppPushNotify.decreaseTRange(context);
            ActionLog.getInstance(context).addAction(ActionLog.AppPush + ActionLog.AppPushClick);
            Intent installApp = new Intent(Intent.ACTION_VIEW);
            installApp.setAction(android.content.Intent.ACTION_VIEW);
            installApp.setDataAndType(intent.getData(), "application/vnd.android.package-archive");
            installApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(installApp);
        }
        
        
    }
}



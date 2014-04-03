package com.tigerknows.service.download;

import com.decarta.android.util.LogWrapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShowAppReceiver extends BroadcastReceiver{
    
    static final String TAG = "ShowAppReceiver";

    public static final String ACTION = "action.com.tigerknows.service.download.ShowAppReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        LogWrapper.d(TAG, "onReceive() " + intent);
        if (intent != null && intent.getData() != null) {
            // TODO： fengtianxiao 更新一个pref 
            Intent installApp = new Intent(Intent.ACTION_VIEW);
            installApp.setAction(android.content.Intent.ACTION_VIEW);
            installApp.setDataAndType(intent.getData(), "application/vnd.android.package-archive");
            installApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            context.startActivity(installApp);
        }
        
        
    }
}



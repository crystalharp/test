package com.tigerknows.radar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.service.PullService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class ConnectivityChangeReceiver extends BroadcastReceiver{

    // android 中网络变化时所发的Intent的名字
    private static final String netACTION="android.net.conn.CONNECTIVITY_CHANGE";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(netACTION)){
            // Intent中ConnectivityManager.EXTRA_NO_CONNECTIVITY这个关键字表示着当前是否连接上了网络
            // true 代表网络断开   false 代表网络没有断开
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            LogWrapper.d("NetCheckReceiver", "onReceive() noConnectivity="+noConnectivity);
            if (noConnectivity == false
                    && AlarmInitReceiver.IS_WAITING_NETWORK_CHANGE) {
                AlarmInitReceiver.IS_WAITING_NETWORK_CHANGE = false;
                Intent service = new Intent(context, PullService.class);
                context.startService(service);
            }
        }
    }
}



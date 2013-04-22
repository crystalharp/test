/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.util.Utility;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

/**
 * 监听网络可用情况的服务父类。
 * @author pengwenyue
 *
 */
public abstract class TKNetworkService extends Service {

    private ConnectivityBroadcastReceiver receiver;
    
    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new ConnectivityBroadcastReceiver();
        registerReceiver(receiver, filter);
        isNetworkAvailable = Utility.checkNetworkStatus(getBaseContext());
        if (networkThread == null) {
            networkThread = new NetworkThread();
            networkThread.start();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        networkThread.setDone();
        super.onDestroy();
    }
    
    public static final long NOT_NETWORK_INTERVAL = 15 * 1000; 
    
    public static final long RETRY_INTERVAL = 8 * 1000;

    private Object networkThreadLock = new Object();

    // true if we have network connectivity
    protected boolean isNetworkAvailable;

    private NetworkThread networkThread;
    
    public abstract void doWork();

    //可被停止的网络连接线程。
    private class NetworkThread extends Thread {

        private boolean mDone = false;

        public NetworkThread() {
            super("NetworkThread");
        }

        public void run() {
            synchronized (networkThreadLock) {
                if (!mDone) {
                    runLocked();
                }
            }
        }

        public void runLocked() {

            while (!mDone) {
                doWork();
            }
        }

        synchronized void setDone() {
            mDone = true;
        }
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            isNetworkAvailable = Utility.checkNetworkStatus(context);
        }
    };
}

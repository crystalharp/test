/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.android.app;

import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.label.SingleRectLabel;
import com.tigerknows.service.CollectService;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * @author Peng Wenyue
 */
public class TKApplication extends Application {

    private static TKApplication sMe;
    
    private int mTimeTicks = 0;
    
    public TKApplication() {
        sMe = this;
    }

    /**
     * 返回TigerknowsApp的单实例对象
     */
    public static TKApplication getInstance() {
        return sMe;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        ActionLog.getInstance(this);
        TKConfig.init(this);
        SingleRectLabel.init(this);
        
        final Intent service = new Intent(this, CollectService.class);
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK); 
        registerReceiver(new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mTimeTicks % 10 == 0) {
                    TKApplication.this.startService(service);
                    mTimeTicks = 0;
                } else {
                    mTimeTicks++;
                }
            }
        }, filter); 
    }

    @Override
    public void onTerminate() {
        ActionLog.getInstance(this).onTerminate();
        MapEngine.getInstance().destroyEngine();
        super.onTerminate();
    }
}

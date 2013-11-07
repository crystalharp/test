/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.android.app;

import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.label.SingleRectLabel;
import com.tigerknows.service.download.DownloadService;

import android.app.Application;

/**
 * @author Peng Wenyue
 */
public class TKApplication extends Application {

    private static TKApplication sMe;
    
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
    }

    @Override
    public void onTerminate() {
        DownloadService.DownloadedList.clear();
        ActionLog.getInstance(this).onTerminate();
        MapEngine.getInstance().destroyEngine();
        super.onTerminate();
    }
}

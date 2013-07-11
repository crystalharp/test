/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapWord;

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
        MapWord.Icon.init(this);
    }

    @Override
    public void onTerminate() {
        ActionLog.getInstance(this).onTerminate();
        MapEngine.getInstance().destroyEngine();
        super.onTerminate();
    }
}

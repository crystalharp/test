/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows.service;

import com.tigerknows.model.SuggestLexiconDownload;

import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * @author pengwenyue
 *
 */
public class SuggestLexiconService extends TKNetworkService {

    public static final String CITY_ID ="city_id";
    
    private int nextCityId = -1;
    
    private int currentCityId = -1;
    
    private SuggestLexiconDownload suggestDownload;
    
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent != null) {
            nextCityId = intent.getIntExtra(CITY_ID, -1);
        }
        if (nextCityId == -2) {
            if (suggestDownload != null) {
                suggestDownload.stop();
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (suggestDownload != null) {
            suggestDownload.stop();
        }
        super.onDestroy();
    }

    @Override
    public void doWork() {
        if (isNetworkAvailable) {   
            if (nextCityId != -1 && nextCityId != currentCityId) {
                currentCityId = nextCityId;
                if (suggestDownload != null) {
                    suggestDownload.stop();
                }
                suggestDownload = new SuggestLexiconDownload(getBaseContext());
                suggestDownload.setCityId(currentCityId);
                suggestDownload.query();
            }
            
            try {
                Thread.sleep(TKNetworkService.RETRY_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Thread.sleep(TKNetworkService.NOT_NETWORK_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

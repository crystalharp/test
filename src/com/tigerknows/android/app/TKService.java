package com.tigerknows.android.app;

import com.decarta.Globals;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TKService extends Service {

    @Override
    public void onCreate() {
        Globals.init(getBaseContext());
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

}

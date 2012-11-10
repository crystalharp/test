/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;

public class MapActivity extends Activity {
	public ConnectivityManager cm=null;
	public int networkType=ConnectivityManager.TYPE_MOBILE;
	public String subNetworkType="";//EDGE, HSDPA, CDMA - EvDo rev. A
	
	//public Object networkLock=new Object();
	//private BroadcastReceiver receiver=null;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        LogWrapper.i("Sequence","MapActivity.onCreate");
        
        cm=(ConnectivityManager)(getSystemService(Context.CONNECTIVITY_SERVICE));
        NetworkInfo info=cm.getActiveNetworkInfo();
        if(info!=null){
        	networkType=info.getType();
        	subNetworkType=info.getSubtypeName();
        	LogWrapper.i("MapActivity","onCreate networkInfo type is:"+((networkType==ConnectivityManager.TYPE_WIFI)?"wifi":"mobile")+",subtype:"+subNetworkType);
        }
        
        /*receiver=new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				Log.i("MapActivity","BroadcastReceiver.onReceive");
				NetworkInfo info=cm.getActiveNetworkInfo();
				if(info!=null){
					
					networkType=info.getType();
			        subNetworkType=info.getSubtypeName();
			        LogWrapper.i("MapActivity","networkInfo type is:"+((networkType==ConnectivityManager.TYPE_WIFI)?"wifi":"mobile")+",subtype:"+subNetworkType);
			        if(networkType==ConnectivityManager.TYPE_MOBILE){
			        	synchronized(networkLock){
				        	networkLock.notifyAll();
				        }
			        }
			    }
			}
		};
        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        */
        
        WindowManager winMan=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
		Display display=winMan.getDefaultDisplay();
		display.getMetrics(Globals.g_metrics);
		LogWrapper.i("MapActivity","xdpi:"+Globals.g_metrics.xdpi+",ydpi:"+Globals.g_metrics.ydpi+",widthPixels:"+Globals.g_metrics.widthPixels+",heightPixesl:"+Globals.g_metrics.heightPixels+",density:"+Globals.g_metrics.density+",densityDpi:"+Globals.g_metrics.densityDpi);
		
    }
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		LogWrapper.i("Sequence","MapActivity.onDestroy");
		//unregisterReceiver(receiver);
		
	}
	
	@Override
	public void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		super.onDetachedFromWindow();
		LogWrapper.i("Sequence","MapActivity.onDetachedFromWindow");
		LogWrapper.i("Sequence","MapActivity.onDetachedFromWindow stopWritingAndRemoveOldTiles done");
	}
	
	
}
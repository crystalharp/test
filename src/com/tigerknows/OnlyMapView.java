package com.tigerknows;

import com.decarta.android.exception.APIException;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.map.label.Label;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.ZoomControls;

import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

/**
 * 本类用于测试，只显示地图
 * @author pengwenyue
 *
 */
public class OnlyMapView extends TKActivity {

    private MapView mMapView;
    private LinearLayout mZoomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mMapEngine = MapEngine.getInstance();
        try {
            mMapEngine.initMapDataPath(getApplicationContext());
        } catch (APIException exception) {
            exception.printStackTrace();
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }        
        WindowManager winMan=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display display=winMan.getDefaultDisplay();
        Label.init(display.getWidth(), display.getHeight());
        
        CityInfo cityInfo = MapEngine.getCityInfo(MapEngine.CITY_ID_BEIJING);
        if (cityInfo.isAvailably() == false) {
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        
        setContentView(R.layout.onlymapview);
        
        mMapView = (MapView) findViewById(R.id.mapview);
        try {
            mMapView.centerOnPosition(cityInfo.getPosition());
        } catch (APIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        mZoomView = (LinearLayout) findViewById(R.id.zoomview);
        
        ZoomControls zoomControls = mMapView.getZoomControls();
        zoomControls.setOnZoomInClickListener(new OnClickListener(){
            public void onClick(View view){
                mMapView.zoomIn();
            }
        });
        zoomControls.setOnZoomOutClickListener(new OnClickListener(){
            public void onClick(View view){
                mMapView.zoomOut();
            }
        });
                    
        // add zoom controller to zoom view
        mZoomView.addView(zoomControls);

        mLocationListener = new MyLocationListener(mThis, null);
    }

}

package com.tigerknows.ui.traffic;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.more.SettingActivity;
import com.tigerknows.util.Utility;

public class TrafficCompassActivity extends BaseActivity implements SensorEventListener{
	
	
	private float mCurrentDegree = 0f;
	private ImageView mCompassImv;
	private ImageView mCompassBgImv;
	private SensorManager mSensorManager;
	private Button mGPSBtn;
	
	private TextView[] mLocationDetailTxv= new TextView[6];
	private int[] mLocationDetailID = new int[]{
		R.string.compass_longitude,
		R.string.compass_latitude,
		R.string.compass_altitude,
		R.string.compass_speed,
		R.string.compass_satellite,
		R.string.compass_accuracy
	};
	private final String[] fUnit = new String[]{
			"",
			"",
			"m",
			"km/h",
			"",
			"m"
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficCompass;
        setContentView(R.layout.traffic_compass);
    	findViews();
    	setListener();
    	mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    	mLocationListener = new MyLocationListener(mThis, new Runnable(){
			@Override
			public void run() {
				refreshLocation();
			}
    	});
    }
    
    protected void findViews(){
    	super.findViews();
    	mCompassImv = (ImageView)findViewById(R.id.compass_imv);
    	mCompassBgImv = (ImageView)findViewById(R.id.compass_bg_imv);
    	mGPSBtn = (Button)findViewById(R.id.gps_btn);
    	mLocationDetailTxv[0] = (TextView)findViewById(R.id.longitude_txv);
    	mLocationDetailTxv[1] = (TextView)findViewById(R.id.latitude_txv);
    	mLocationDetailTxv[2] = (TextView)findViewById(R.id.altitude_txv);
    	mLocationDetailTxv[3] = (TextView)findViewById(R.id.speed_txv);
    	mLocationDetailTxv[4] = (TextView)findViewById(R.id.satellite_txv);
    	mLocationDetailTxv[5] = (TextView)findViewById(R.id.accuracy_txv);
    }
    
    protected void setListener(){
    	super.setListener();
    	mGPSBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mActionLog.addAction(mActionTag + ActionLog.TrafficCompassGPS, String.valueOf(SettingActivity.checkGPS(mThis)));
				startActivityForResult(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"), R.id.activity_setting_location);
			}
		});
    }
    
    @Override
    public void onPause(){
    	if(mSensorManager != null){
    		mSensorManager.unregisterListener(this);
    	}
    	super.onPause();
    }

    @Override
    public void onStop(){
    	if(mSensorManager != null){
    		mSensorManager.unregisterListener(this);
    	}
    	super.onStop();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	mTitleBtn.setText(getString(R.string.compass));
    	refreshCompass();
    	refreshGPS();
    	refreshLocation();
    }
    
    public void refreshCompass(){
    	if(mSensorManager != null){
    		Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		if(sensor != null){
    			mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    			mCompassImv.setImageResource(R.drawable.ani_compass);
    			mCompassBgImv.setImageResource(R.drawable.bg_compass);
    		}else{
    			// TODO: default image
    			mCompassImv.setImageResource(R.drawable.bg_query_fail);
    			mCompassBgImv.setImageResource(R.drawable.transparent_bg);
    		}
    	}else{
    		// TODO: default image
    		mCompassImv.setImageResource(R.drawable.bg_query_fail);
    		mCompassBgImv.setImageResource(R.drawable.transparent_bg);
    	}
    }
    
    public void refreshGPS(){
    	if(SettingActivity.checkGPS(mThis) == true){
    		mGPSBtn.setText(getString(R.string.compass_gps_open));
    	}else{
    		mGPSBtn.setText(getString(R.string.compass_gps_close));
    	}
    }
    
    public void refreshLocation(){
    	Location location = Globals.g_My_Location;
    	final String EMPTY = "...";
    	String locationDetail[] = new String[6];
    	if(location == null){
			for(int i=0; i<6; i++){
				locationDetail[i] = EMPTY;
			}
    	}else{
    		locationDetail[0] = String.valueOf(Utility.doubleKeep(location.getLongitude(), 6));
    		locationDetail[1] = String.valueOf(Utility.doubleKeep(location.getLatitude(), 6));
    		locationDetail[2] = location.hasAltitude() ? String.valueOf(Utility.doubleKeep(location.getAltitude(), 1)) : EMPTY;
    		locationDetail[3] = location.hasSpeed() ? String.valueOf(Utility.doubleKeep(location.getSpeed()*3.6, 1)) : EMPTY;
    		locationDetail[4] = location.hasAltitude() ? String.valueOf(mTKLocationManager.getSatelliteSize()) : EMPTY;
    		locationDetail[5] = location.hasAccuracy() ? String.valueOf(Utility.doubleKeep(location.getAccuracy(), 1)) : EMPTY;
    		//Toast.makeText(mThis, location.getProvider(), Toast.LENGTH_SHORT).show();
    	}

    	for(int i=0; i<6; i++){
    		if(!TextUtils.equals(locationDetail[i], EMPTY)){
    			locationDetail[i] += fUnit[i];
    		}
    		mLocationDetailTxv[i].setText(getString(mLocationDetailID[i], locationDetail[i]));
    	}
    }
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
		case Sensor.TYPE_ORIENTATION:
			// 获取绕Z轴转过的角度。
			float degree = event.values[0];
			// 创建旋转动画（反向转过degree度）
			RotateAnimation rotateAnimation = new RotateAnimation(mCurrentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			rotateAnimation.setDuration(200);
			mCompassImv.startAnimation(rotateAnimation);
			mCurrentDegree = -degree;
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

}

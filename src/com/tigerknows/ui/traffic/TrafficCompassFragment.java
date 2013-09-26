package com.tigerknows.ui.traffic;

import java.util.Currency;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.SettingActivity;

public class TrafficCompassFragment extends BaseFragment implements SensorEventListener{
	
	public TrafficCompassFragment(Sphinx sphinx) {
		super(sphinx);
	}
	
	float mCurrentDegree = 0f;
	ImageView mCompassImv;
	SensorManager mSensorManager;
	Button mGPSBtn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: mActionTag = ActionLog.TrafficCompass;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	mRootView = mLayoutInflater.inflate(R.layout.traffic_compass, container, false);
    	findViews();
    	setListener();
    	mSensorManager = (SensorManager) mSphinx.getSystemService(Context.SENSOR_SERVICE);
    	return mRootView;
    }
    
    protected void findViews(){
    	mCompassImv = (ImageView)mRootView.findViewById(R.id.compass_imv);
    	mGPSBtn = (Button)mRootView.findViewById(R.id.gps_btn);
    }
    
    protected void setListener(){
    	mGPSBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO: ActionLog
				mSphinx.startActivityForResult(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"), R.id.activity_setting_location);
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
    	mTitleBtn.setText(mSphinx.getString(R.string.compass));
    	refreshCompass();
    	refreshGPS();

    }
    
    public void refreshCompass(){
    	if(mSensorManager != null){
    		Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		if(sensor != null){
    			mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    			mCompassImv.setBackgroundResource(R.drawable.ani_compass);
    		}else{
    			mCompassImv.setBackgroundResource(R.drawable.ani_compass); // TODO: default image
    		}
    	}else{
    		mCompassImv.setBackgroundResource(R.drawable.ani_compass); // TODO: default image
    	}
    }
    
    public void refreshGPS(){
    	if(SettingActivity.checkGPS(mContext) == true){
    		mGPSBtn.setText(mSphinx.getString(R.string.compass_gps_open));
    	}else{
    		mGPSBtn.setText(mSphinx.getString(R.string.compass_gps_close));
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
			rotateAnimation.setDuration(50);
			mCompassImv.startAnimation(rotateAnimation);
			mCurrentDegree = -degree;
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

}

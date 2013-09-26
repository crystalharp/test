package com.tigerknows.ui.traffic;

import java.util.Currency;

import android.content.Context;
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
import android.widget.ImageView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseFragment;

public class TrafficCompassFragment extends BaseFragment implements SensorEventListener{
	
	float mCurrentDegree = 0f;
	ImageView mCompassImv;
	SensorManager mSensorManager;
	
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
    }
    
    protected void setListener(){
    	
    }
    
    @Override
    public void onPause(){
    	mSensorManager.unregisterListener(this);
    	super.onPause();
    }

    @Override
    public void onStop(){
    	mSensorManager.unregisterListener(this);
    	super.onStop();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	mTitleBtn.setText(mSphinx.getString(R.string.compass));
    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
    }

	public TrafficCompassFragment(Sphinx sphinx) {
		super(sphinx);
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

package com.tigerknows.ui.traffic;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.service.LocationService;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.more.SettingActivity;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.SolarRadiation;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.RotateImageView;

public class TrafficCompassActivity extends BaseActivity implements SensorEventListener{
	
	
	private float mCurrentDegree = 0f;
	private ImageView mCompassImv;
	private ImageView mCompassBgImv;
	private SensorManager mSensorManager;
	private Button mGPSBtn;
	private RelativeLayout mBodyRly;
	private TextView mSunCompassHintTxv;
    private LocationManager mLocationManager;
    private boolean mAddGpsStatusListener = false;
    private List<GpsSatellite> mSatelliteList = new ArrayList<GpsSatellite>();
    private GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        
        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
                GpsStatus status = mLocationManager.getGpsStatus(null);
                if(status != null){
                    int maxSatellites = status.getMaxSatellites();
                    Iterator<GpsSatellite> it = status.getSatellites().iterator();
                    mSatelliteList.clear();
                    int count = 0;
                    while (it.hasNext() && count <= maxSatellites){
                        mSatelliteList.add(it.next());
                        count++;
                    }
                }
            }
        }
    };
	
	private TextView[] mLocationDetailTxv = new TextView[6];
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
				refreshLocationAndCompass();
			}
    	});

    	mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }
    
    protected void findViews(){
    	super.findViews();
    	mBodyRly = (RelativeLayout)findViewById(R.id.body_rly);
    	mCompassImv = (ImageView)findViewById(R.id.compass_imv);
    	mCompassBgImv = (ImageView)findViewById(R.id.compass_bg_imv);
    	mGPSBtn = (Button)findViewById(R.id.gps_btn);
    	mSunCompassHintTxv = (TextView)findViewById(R.id.sun_compass_hint_txv);
    	mLocationDetailTxv[0] = (TextView)findViewById(R.id.longitude_txv);
    	mLocationDetailTxv[1] = (TextView)findViewById(R.id.latitude_txv);
    	mLocationDetailTxv[2] = (TextView)findViewById(R.id.altitude_txv);
    	mLocationDetailTxv[3] = (TextView)findViewById(R.id.speed_txv);
    	mLocationDetailTxv[4] = (TextView)findViewById(R.id.satellite_txv);
    	mLocationDetailTxv[5] = (TextView)findViewById(R.id.accuracy_txv);
    }
    
    protected void setListener(){
    	super.setListener();
        mRightBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mActionLog.addAction(mActionTag + ActionLog.TrafficCompassShare);
				Bitmap bm = Utility.viewToBitmap(mBodyRly);
				String path = TKConfig.getSnapShotPath();
                Uri uri = null;
                if (bm != null && !TextUtils.isEmpty(path)) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                    uri = Utility.bitmap2Png(bm, simpleDateFormat.format(Calendar.getInstance().getTime())+".png", path);
                    if (bm.isRecycled() == false) {
                        bm.recycle();
                    }
                    bm = null;
                    if (uri != null) {
                        Utility.share(mThis, getString(R.string.share), null, uri);
                    }
                }
			}
		});
    	
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
        if (mAddGpsStatusListener) {
            mLocationManager.removeGpsStatusListener(mGpsStatusListener);
        }
        mAddGpsStatusListener = false; 
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
        List<String> providers = mLocationManager.getAllProviders();
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            mAddGpsStatusListener = true;
            mLocationManager.addGpsStatusListener(mGpsStatusListener);
        }
    	refreshLocationAndCompass();
    	refreshGPS();
    }
    
   
    private void setSunCompass(Location location){
    	if(location == null){
    		mCompassImv.setImageResource(R.drawable.ani_compass_notavailable);
    		mCompassBgImv.setImageResource(R.drawable.transparent_bg);
    		setCompassAnimation(0, 0, LocationService.RETRY_INTERVAL * 30);
    		mSunCompassHintTxv.setVisibility(View.GONE);
    		return;
    	}
    	mSunCompassHintTxv.setVisibility(View.VISIBLE);
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTimeInMillis(CalendarUtil.getExactTime(mThis));
    	double degree = 0;
		try {
			degree = SolarRadiation.taiYangFangWeiJiao(calendar, location.getLongitude(), location.getLatitude());
		} catch (APIException e) {
			e.printStackTrace();
		}
		mCompassImv.setImageResource(R.drawable.ani_compass_sun);
		mCompassBgImv.setImageResource(R.drawable.bg_compass_sun);
		setCompassAnimation(-(float)degree, -(float)degree, LocationService.RETRY_INTERVAL * 30);
    }
    
    private void setCompassAnimation(float fromDegree, float toDegree, long duration){
    	RotateAnimation rotateAnimation = new RotateAnimation(fromDegree, toDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    	rotateAnimation.setDuration(duration);
    	mCompassImv.startAnimation(rotateAnimation);
    }
    
    public void refreshGPS(){
    	if(SettingActivity.checkGPS(mThis) == true){
    		mGPSBtn.setText(getString(R.string.compass_gps_open));
    	}else{
    		mGPSBtn.setText(getString(R.string.compass_gps_close));
    	}
    }
    
    public void refreshLocationAndCompass(){
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
    		locationDetail[4] = location.hasAltitude() ? String.valueOf(mSatelliteList.size()) : EMPTY;
    		locationDetail[5] = location.hasAccuracy() ? String.valueOf(Utility.doubleKeep(location.getAccuracy(), 1)) : EMPTY;
    		//Toast.makeText(mThis, location.getProvider(), Toast.LENGTH_SHORT).show();
    	}

    	for(int i=0; i<6; i++){
    		if(!TextUtils.equals(locationDetail[i], EMPTY)){
    			locationDetail[i] += fUnit[i];
    		}
    		mLocationDetailTxv[i].setText(getString(mLocationDetailID[i], locationDetail[i]));
    	}
    	if(TKConfig.UseSunCompassCheck == false && mSensorManager != null){
    		Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    		if(sensor != null){
    			mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    			mCompassImv.setImageResource(R.drawable.ani_compass);
    			//mCompassImv.setAngle(0);
    			mCompassBgImv.setImageResource(R.drawable.transparent_bg);
    			mSunCompassHintTxv.setVisibility(View.GONE);
    		}else{
    			setSunCompass(location);
    		}
    	}else{
    		setSunCompass(location);
    	}
    }
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
		case Sensor.TYPE_ORIENTATION:
			// 获取绕Z轴转过的角度。
			float degree = event.values[0];
			// 创建旋转动画（反向转过degree度）
			setCompassAnimation(mCurrentDegree, -degree, 200);
			mCurrentDegree = -degree;
			break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

}

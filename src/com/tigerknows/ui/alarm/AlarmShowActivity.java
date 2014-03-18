package com.tigerknows.ui.alarm;

import com.decarta.Globals;
import com.tigerknows.LauncherActivity;
import com.tigerknows.R;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Alarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 显示公交闹铃
 * @author pengwenyue
 *
 */
public class AlarmShowActivity extends TKActivity implements View.OnClickListener {

    static final String TAG = "AlarmShowActivity";
    
    public static final String EXTRA_ALARM_LIST = "EXTRA_ALARM_LIST";
    
    private static final long VIBRATOR_TIME = 1000 * 60 * 2;
    
    private MediaPlayer mAudio;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;
    private List<Alarm> mAlarms = new ArrayList<Alarm>();
    private Alarm mAlarm;
    private long[] mPattern = {1000, 2000, 1000, 2000, 1000, 2000};
    private Handler mHandler;
    private Runnable mShowNextAlarm = new Runnable() {
        
        @Override
        public void run() {
            showNextAlarm();
        }
    };
    
    private TextView mBodyTxv;
    private Button mCloseBtn;
    private boolean onWindowFocusChanged = false;
    
    private Runnable mLocationChangedRun = new Runnable() {

        @Override
        public void run() {
            refreshText();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.AlarmShow;
        onWindowFocusChanged = false;
        
        mHandler = new Handler();
        
        setContentView(R.layout.alarm_show);
        
        findViews();
        setListener();
        
        mLocationListener = new MyLocationListener(mThis, mLocationChangedRun);
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        mVibrator.vibrate(mPattern, 0);
        
        Intent intent = getIntent();
        readAlarm(intent);
        
        showNextAlarm();
    }
    
    private void findViews() {
        mBodyTxv = (TextView) findViewById(R.id.message);
        mCloseBtn = (Button) findViewById(R.id.button1);
    }
    
    private void setListener() {
        mCloseBtn.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        readAlarm(newIntent);
    }
    
    private void readAlarm(Intent intent) {
        if (intent != null) {
            ArrayList<Alarm> alarms = intent.getParcelableArrayListExtra(EXTRA_ALARM_LIST);
            if (alarms != null && alarms.size() > 0) {
                mAlarms.addAll(alarms);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            onWindowFocusChanged = hasFocus;
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LauncherActivity.LastActivityClassName = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (onWindowFocusChanged) {
            finish();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                showNextAlarm();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showNextAlarm() {
        if (mAlarms.size() > 0) {
            mAlarm = mAlarms.remove(0);
            
            refreshText();
            mHandler.removeCallbacks(mShowNextAlarm);
            mHandler.postDelayed(mShowNextAlarm, VIBRATOR_TIME);
            stop();
            play();
            
        } else {
            finish();
        }
    }
    
    private void refreshText() {
        Position position = Globals.getMyLocationPosition();
        if (position != null) {
            mBodyTxv.setText(getString(R.string.alarm_tip_text, mAlarm.getName(), Position.distanceBetween(position, mAlarm.getPosition())));
        } else {
            mBodyTxv.setText(getString(R.string.alarm_tip_text, mAlarm.getName(), mAlarm.getRange()));
        }
    }

    private void play() {
        
        if (mAudio == null) {
            try {
                mAudio = new MediaPlayer();
            } catch (Exception e) {
                e.printStackTrace();
                mAudio = null;
            }
        }
        
        if (mAudio != null) {
            
            Uri uri = mAlarm.getRingtone();
            if (uri != null) {
                try {
                    mAudio.setDataSource(mThis, uri);
                    mAudio.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mAudio.prepare();
                    
                    // do not ringtones if stream volume is 0
                    // (typically because ringer mode is silent).
                    if (mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                        mAudio.setLooping(true);
                        mAudio.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void stop() {
        if (mAudio != null) {
            mAudio.stop();
            mAudio.reset();
        }
    }

    @Override
    public void onClick(View v) {
        addActionLog(ActionLog.AlarmShowClose);
        showNextAlarm();
    }

    @Override
    public void finish() {
        mHandler.removeCallbacks(mShowNextAlarm);
        mVibrator.cancel();
        stop();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
}

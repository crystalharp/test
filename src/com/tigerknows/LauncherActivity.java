package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.BootstrapModel.StartupDisplay;
import com.tigerknows.model.XMapData;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class LauncherActivity extends TKActivity {
    
    static final String TAG = "LauncherActivity";
    
    public static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    
    public static String LastActivityClassName = null;
    
    static final int AD_ANIMATION_TIME = 2000;
    
    /**
     * 第一次打开本应用时会发送的广播的Action
     */
    public static final String ACTION_FIRST_STARTUP = "action.com.tigerknows.first.startup";
    
    View mRootView;
    
    ImageView mStartupImv;
    
    boolean mOnResume = false;
    
    Animation mAnimation = new AlphaAnimation(0.3f, 1.0f);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionTag = ActionLog.Launcher;
        
        TKConfig.readConfig();
        
        if (Intent.ACTION_MAIN.equals(mIntent.getAction())) {   // 从快捷方式启动老虎时，老虎正在运行但不在前台的情况
            
            String lastActivityClassName = LastActivityClassName;
            if (lastActivityClassName != null && lastActivityClassName.equals(this.getLocalClassName()) == false) {
                finish();
                return;
            }
        }
        
        mRootView = mLayoutInflater.inflate(R.layout.launcher, null, false);
        
        mStartupImv = (ImageView) mRootView.findViewById(R.id.startup_imv);
        
        synchronized (Globals.StartupDisplayFile) {
            File startupDisplayFile = new File(Globals.StartupDisplayFile);
            if (startupDisplayFile.exists() && startupDisplayFile.isFile()) {
                try {
                    byte[] data = Utility.readFileToByte(new FileInputStream(startupDisplayFile));
                    XMap xmap = (XMap) ByteUtil.byteToXObject(data);
                    List<StartupDisplay> list = XMapData.getListFromData(xmap, BootstrapModel.FIELD_STARTUP_DISPLAY_LIST, StartupDisplay.Initializer, null);
                    TKDrawable tkDrawable = getStartupDisplayDrawable(mThis, list);
                    
                    if (tkDrawable != null) {
                        Drawable drawable = tkDrawable.loadDrawable(mThis, null, tkDrawable.toString());
                        if (drawable != null) {
                            setImageDrawable(tkDrawable.getUrl(), drawable);
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        final boolean fristUse = TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_FIRST_USE));
        final boolean upgrade = TextUtils.isEmpty(TKConfig.getPref(mThis, TKConfig.PREFS_UPGRADE));
        
        mHandler = new Handler();
        setContentView(mRootView);
        
        mHandler.postDelayed(new Runnable() {
            
            @Override
            public void run() {
                launch(fristUse, upgrade);
            }
        }, AD_ANIMATION_TIME);

    }
    
    void launch(boolean fristUse, boolean upgrade) {

        if (mOnResume) {
            Intent intent = getIntent();
            Intent newIntent = new Intent();
            newIntent.setData(intent.getData());
            newIntent.putExtras(intent);
            if (fristUse || upgrade) {   // 首次安装或升级安装使用的情况
                sendFirstStartupBroadcast();
                newIntent.setClass(mThis, GuideScreenActivity.class);
                newIntent.putExtra(GuideScreenActivity.APP_FIRST_START, fristUse);
                newIntent.putExtra(GuideScreenActivity.APP_UPGRADE, upgrade);
                startActivity(newIntent);
                
                TKConfig.setPref(mThis, TKConfig.PREFS_FIRST_USE, "1");
                TKConfig.setPref(mThis, TKConfig.PREFS_UPGRADE, "1");
                
            } else {
                newIntent.setClass(mThis, Sphinx.class);
                if (Intent.ACTION_MAIN.equals(intent.getAction())) {   // 从快捷方式启动老虎
                    startActivity(newIntent);
                } else {   // 来自第三方的调用
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(newIntent);
                }
            }
        } else {
            finish();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendFirstStartupBroadcast() {
        Intent intent = new Intent(ACTION_FIRST_STARTUP);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        mOnResume = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mOnResume = false;
        super.onPause();
    }
    
    public static TKDrawable getStartupDisplayDrawable(Activity activity, List<StartupDisplay> startupDisplayList) {
        TKDrawable tkDrawable = null;
        if (startupDisplayList != null && startupDisplayList.size() > 0) {
            StartupDisplay startupDisplay = startupDisplayList.get(0);
            if (startupDisplay.isAvailably(activity)) {
                String url = startupDisplay.getUrl();
                url = Utility.getPictureUrlByWidthHeight(url, Globals.getPicWidthHeight(TKConfig.PICTURE_STARTUP_DISPLAY));
                LogWrapper.d(TAG, "url:"+url);
                tkDrawable = new TKDrawable();
                tkDrawable.setUrl(url);
            }
        }
        
        return tkDrawable;
    }
    
    void setImageDrawable(String url, Drawable drawable) {
        if (drawable != null &&
                isFinishing() == false) {
            
            mStartupImv.setImageDrawable(drawable);
            
            if (mAnimation != null) {
                mAnimation.setDuration(1000);
                mStartupImv.setAnimation(mAnimation);
                mAnimation.startNow();
                mAnimation = null;
            }
            mRootView.findViewById(R.id.logo_imv).setVisibility(View.VISIBLE);
            
            int beginIndex = url.lastIndexOf("/");
            int endIndex = url.lastIndexOf(".");
            String md5 = url.substring(beginIndex+1, endIndex)+"_"+SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
            LogWrapper.d(TAG, "StartupDisplay md5:"+md5);
            synchronized (Globals.StartupDisplayLogFile) {
                Utility.writeFile(Globals.StartupDisplayLogFile, (";"+md5).getBytes(), false);
            }
        }
    }
}
package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.Sphinx;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.common.AsyncImageLoader;
import com.tigerknows.common.AsyncImageLoader.ImageCallback;
import com.tigerknows.common.AsyncImageLoader.TKURL;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.Response;
import com.tigerknows.model.BootstrapModel.StartupDisplay;
import com.tigerknows.model.XMapData;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    
    String mStartupDisplayLogPath;
    
    String mStartupDisplayLog = null;
    
    boolean mOnResume = false;
	
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
        
        mStartupDisplayLogPath = TKConfig.getDataPath(true)+"startupDisplayLog";
        
        AsyncImageLoader.SUPER_VIEW_TOKEN = mThis.toString();
        
        File startupDisplayFile = new File(TKConfig.getDataPath(true)+StartupDisplay.FILE_NAME);
        if (startupDisplayFile.exists() && startupDisplayFile.isFile()) {
            try {
                byte[] data = Utility.readFileToByte(new FileInputStream(startupDisplayFile));
                XMap xmap = (XMap) ByteUtil.byteToXObject(data);
                List<StartupDisplay> list = XMapData.getListFromData(xmap, BootstrapModel.FIELD_STARTUP_DISPLAY_LIST, StartupDisplay.Initializer, null);
                showStartupDisplay(list);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
        
        List<BaseQuery> list = new ArrayList<BaseQuery>();
        
        Bootstrap bootstrap = new Bootstrap(this);
        if (getIntent().getBooleanExtra(Sphinx.EXTRA_WEIXIN, false)) {
            bootstrap.addParameter(BaseQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, BaseQuery.REQUSET_SOURCE_TYPE_WEIXIN);
        }
        if (fristUse) {
            bootstrap.addParameter(Bootstrap.SERVER_PARAMETER_FIRST_LOGIN, Bootstrap.FIRST_LOGIN_NEW);
        } else if (upgrade) {
            bootstrap.addParameter(Bootstrap.SERVER_PARAMETER_FIRST_LOGIN, Bootstrap.FIRST_LOGIN_UPGRADE);
        }
        list.add(bootstrap);
        try {
            synchronized (mStartupDisplayLogPath) {
                File startupDisplayLogFile = new File(mStartupDisplayLogPath);
                if (startupDisplayLogFile.exists() && startupDisplayLogFile.isFile() && startupDisplayLogFile.length() > 0) {
                    mStartupDisplayLog = Utility.readFile(new FileInputStream(startupDisplayLogFile));
                    
                    if (mStartupDisplayLog != null && mStartupDisplayLog.length() > 0) {
                        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
                        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_DISPLAY, mStartupDisplayLog.substring(1));
                        
                        list.add(feedbackUpload);
                    }
                }
            }
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        queryStart(list);
        
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
        }
        
        finish();
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

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        List<BaseQuery> list = tkAsyncTask.getBaseQueryList();
        for(BaseQuery baseQuery : list) {
            if (baseQuery instanceof Bootstrap) {
                BootstrapModel bootstrapModel = ((Bootstrap) baseQuery).getBootstrapModel();
                if (bootstrapModel != null) {
                    Globals.g_Bootstrap_Model = bootstrapModel;
                    showStartupDisplay(bootstrapModel.getStartupDisplayList());
                }
            } else if (baseQuery instanceof FeedbackUpload) {
                Response response = baseQuery.getResponse();
                if (response != null && response.getResponseCode() == Response.RESPONSE_CODE_OK) {
                    synchronized (mStartupDisplayLogPath) {
                        File file = new File(mStartupDisplayLogPath);
                        try {
                            String startupDisplayLog = Utility.readFile(new FileInputStream(file));
                            if (startupDisplayLog != null && mStartupDisplayLog != null && mStartupDisplayLog.length() > 0) {
                                Utility.writeFile(mStartupDisplayLogPath, startupDisplayLog.replace(mStartupDisplayLog, "").getBytes(), true);
                            }
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    void showStartupDisplay(List<StartupDisplay> startupDisplayList) {
        if (startupDisplayList != null && startupDisplayList.size() > 0) {
            StartupDisplay startupDisplay = startupDisplayList.get(0);
            if (startupDisplay.isAvailably(mThis)) {
                final String url = startupDisplay.getUrl();
                LogWrapper.d(TAG, "url:"+url);
                BitmapDrawable bitmapDrawable = AsyncImageLoader.getInstance().loadDrawable(mThis,
                        new TKURL(Utility.getPictureUrlByWidthHeight(url, Globals.getPicWidthHeight(TKConfig.PICTURE_STARTUP_DISPLAY)), mThis.toString()),
                        new ImageCallback() {
                            
                            @Override
                            public void imageLoaded(BitmapDrawable imageDrawable) {
                                setImageDrawable(url, imageDrawable);
                            }
                        });
                
                setImageDrawable(url, bitmapDrawable);
            }
        }
    }
    
    void setImageDrawable(String url, BitmapDrawable bitmapDrawable) {
        if (bitmapDrawable != null &&
                isFinishing() == false) {
            mStartupImv.setImageDrawable(bitmapDrawable);
            int beginIndex = url.lastIndexOf("/");
            int endIndex = url.lastIndexOf(".");
            String md5 = url.substring(beginIndex+1, endIndex)+"_"+SIMPLE_DATE_FORMAT.format(Calendar.getInstance().getTime());
            LogWrapper.d(TAG, "StartupDisplay md5:"+md5);
            synchronized (mStartupDisplayLogPath) {
                Utility.writeFile(mStartupDisplayLogPath, (";"+md5).getBytes(), false);
            }
        }
    }
}

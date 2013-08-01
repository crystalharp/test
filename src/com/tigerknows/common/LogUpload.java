/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */
package com.tigerknows.common;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.Utility;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * 日志上传类
 * @author pengwenyue
 *
 */
public class LogUpload {
    
    static final String TAG = "LogUpload";

    /**
     * 日志文件数据的最大长度，若长度大于此值则清空数据重新记录
     */
    static final int MAX_LENGTH = 150 * 1024;
    
    /**
     * 日志文件长度达到此值时则请求上传日志到服务器
     */
    static final int UPLOAD_LENGTH = 15 * 1024;
    
    /**
     * 日志数据缓冲区大小
     * 日志文件长度达到上传的大小后，每增加此值大小的数据则请求上传日志到服务器
     */
    static final int WRITE_FILE_SIZE = 1024;
    
    static final long TIME_DAY = 24*60*60*1000;
    
    protected Context mContext;
    protected Object mLock = new Object();
    protected StringBuilder mStringBuilder = null;
    protected String mLogFilePath;
    protected String mLogFileName;
    protected int mLogFileLength = 0;
    protected String mServerParameterKey;
    
    public LogUpload(Context context, String logFileName, String serverParameterKey) {
        synchronized (mLock) {
            mContext = context;
            mLogFileName = logFileName;
            mLogFilePath = TKConfig.getDataPath(false) + mLogFileName;
            mLogFileLength = 0;
            mServerParameterKey = serverParameterKey;
            
            try {
                File file = new File(mLogFilePath);
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        LogWrapper.e(TAG, "Unable to create new file: " + mLogFilePath);
                        return;
                    }
                }
                FileInputStream fis = new FileInputStream(file);
                mLogFileLength = fis.available();
                fis.close();
    
                mStringBuilder = new StringBuilder();
            } catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    protected void tryUpload() {
        synchronized (mLock) {
            try {
                if (mStringBuilder == null) {
                    return;
                }
    
                // 判断是否要写入文件
                if (mStringBuilder.length() > WRITE_FILE_SIZE) {
                    write();
                    
                    // 判断是否要上传
                    if (mLogFileLength > UPLOAD_LENGTH) {
                        upload();
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                LogWrapper.e(TAG, e.getMessage());
            }
        }
    }
    
    protected void upload() {
        synchronized (mLock) {
            try {
                if (mStringBuilder == null) {
                    return;
                }
                final File file = new File(mLogFilePath);
                FileInputStream fis = new FileInputStream(file);
                final String log = Utility.readFile(fis)+getLogOutToken();
                fis.close();
                
                if (canUpload()) {
                    new Thread(new Runnable() {
                        
                        @Override
                        public void run() {
                            FeedbackUpload feedbackUpload = new FeedbackUpload(mContext);
                            feedbackUpload.addParameter(mServerParameterKey, log);
                            CityInfo cityInfo = Globals.getCurrentCityInfo();
                            int cityId = MapEngine.CITY_ID_BEIJING;
                            if (cityInfo != null) {
                                cityId = cityInfo.getId();
                            }
                            feedbackUpload.setup(cityId);
                            feedbackUpload.query();
                            com.tigerknows.model.Response response = feedbackUpload.getResponse();
                            if ((response != null && response.getResponseCode() == com.tigerknows.model.Response.RESPONSE_CODE_OK) ||
                                    mLogFileLength > MAX_LENGTH) {

                                synchronized (mLock) {
                                    if (file.delete()) {
                                        try {
                                            if (!file.createNewFile()) {
                                                mStringBuilder = null;
                                                LogWrapper.e(TAG, "Unable to create new file: " + mLogFilePath);
                                            }
                                        } catch (IOException e) {
                                            mStringBuilder = null;
                                            e.printStackTrace();
                                        }
                                    }
                                    
                                    onLogOut();
                                }
                            }
                        }
                    }).start();
                }
            } catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    protected boolean canUpload() {
        return true;
    }
    
    protected String getLogOutToken() {
        return "";
    }
    
    protected void onLogOut() {
        mLogFileLength = 0;
    }
    
    public void onCreate() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            
            File file = new File(mLogFilePath);
            long lastModified = file.lastModified();
            long current = System.currentTimeMillis();
            if (current - lastModified >= TIME_DAY) {
                upload();
            } 
            
        }
    }
    
    public void onDestroy() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            write();
        }
    }
    
    public void onTerminate() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            write();
        }
    }
    
    protected void write() {
        synchronized (mLock) {
            if (mStringBuilder == null) {
                return;
            }
            if (mStringBuilder.length() > 0) {
                try {
                    File file = new File(mLogFilePath); 
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    byte[] data = mStringBuilder.toString().getBytes();
                    fileOutputStream.write(data);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    mStringBuilder = new StringBuilder();
                    mLogFileLength += data.length;
                } catch (Exception e) {
                    mStringBuilder = null;
                    e.printStackTrace();
                }
            }
        }
    }
}

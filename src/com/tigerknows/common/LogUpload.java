/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */
package com.tigerknows.common;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.map.CityInfo;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志记录及上传
 * 行为日志和定位日志的实现类从此类继承
 * @author pengwenyue
 *
 */
public class LogUpload {
    
    static final String TAG = "LogUpload";

    /**
     * 日志文件数据的最大长度，若长度大于此值则清空数据重新记录
     */
    static final int FILE_MAX_LENGTH = 150 * 1024;
    
    /**
     * 日志文件长度达到此值时则请求上传日志到服务器
     */
    static final int FILE_LENGTH_OUT = 15 * 1024;
    
    /**
     * 1、日志数据缓冲区大小
     * 2、日志文件长度达到上传的大小后，每增加此值大小的数据则请求上传日志到服务器
     */
    static final int WRITE_FILE_SIZE = 1024;
    
    /**
     * 当前时间截减去上次存取日志文件的时间截大于此值则上传
     */
    static final long ACCESS_TIME_OUT = 24*60*60*1000;
    
    /**
     * 如果距离上次提交行为日志或者定位采集点超过2分钟就增加提交（对于Android，行为日志和定位采集可以合并成一次提交）
     */
    static final int UPLOAD_TIME_OUT = 2 * 60 * 1000;
    
    /**
     * 上下文
     */
    protected Context mContext;
    
    /**
     * 同步锁
     */
    protected Object mLock = new Object();
    
    /**
     * 日志数据的缓存区
     */
    protected StringBuilder mStringBuilder = null;
    
    /**
     * 日志文件的路径
     */
    protected String mLogFilePath;
    
    /**
     * 日志文件名
     */
    protected String mLogFileName;
    
    /**
     * 当前日志文件的长度
     */
    protected int mLogFileLength = 0;
    
    /**
     * 提交到服务器的参数名称
     */
    protected String mServerParameterKey;
    
    /**
     * 最近一次上传日志的时间截
     */
    protected long mLastUploadTime = 0;
    
    protected boolean onCreate = false;
    
    /**
     * 构造函数
     * @param context
     * @param logFileName
     * @param serverParameterKey
     */
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
    
    /**
     * 检测是否可以写入数据及上传日志
     */
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
                    if (mLogFileLength > FILE_LENGTH_OUT) {
                        upload();
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                LogWrapper.e(TAG, e.getMessage());
            }
        }
    }
    
    /**
     * 获取日志文件的文本内容
     * @return
     */
    String getLogText() {
        synchronized (mLock) {
            String logText = null;
            try {
                File file = new File(mLogFilePath);
                if(file.exists()){
                    FileInputStream fis = new FileInputStream(file);
                    logText = Utility.readFile(fis);
                    fis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return logText;
        }
    }
    
    /**
     * 删除日志文件
     */
    void deleteLogFile() {
        synchronized (mLock) {
            try {
                File file = new File(mLogFilePath);
                if(file.exists()){
                    file.delete();
                    onLogOut();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 若距离上次提交日志是否超过2分钟则提交
     * @param context
     * @param logUploadArray
     */
    public static void uploadLog(final Context context, final LogUpload... logUploadArray) {
        if (context == null || logUploadArray == null || logUploadArray.length == 0) {
            return;
        }
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                final List<LogUpload> logUploadList = new ArrayList<LogUpload>();
                final FeedbackUpload feedbackUpload = new FeedbackUpload(context);
                
                long currentTime = System.currentTimeMillis();
                for(int i = 0, length = logUploadArray.length; i < length; i++) {
                    LogUpload logUpload = logUploadArray[i];
                    String logText = logUpload.getLogText();
                    long lastUploadTime = logUpload.mLastUploadTime;
                    if (lastUploadTime != 0 && currentTime - lastUploadTime > UPLOAD_TIME_OUT && TextUtils.isEmpty(logText) == false) {
                        logUpload.mLastUploadTime = currentTime;
                        feedbackUpload.addParameter(logUpload.mServerParameterKey, logText);
                        logUploadList.add(logUpload);
                    }
                }
                LogWrapper.d(TAG, "UploadLog() logUploadList.size="+logUploadList.size());
                if (logUploadList.isEmpty()) {
                    return;
                }
                
                feedbackUpload.query();
                com.tigerknows.model.Response response = feedbackUpload.getResponse();
                if ((response != null && response.getResponseCode() == com.tigerknows.model.Response.RESPONSE_CODE_OK)) {
                    for(int i = logUploadList.size()-1; i >= 0; i--) {
                        logUploadList.get(i).deleteLogFile();
                    }
                }
            }
        }).start();
    }
    
    /**
     * 上传日志数据到服务器，提交成功或日志文件已经超长最大长度则删除日志文件并执行onLogOut()
     */
    protected void upload() {
        synchronized (mLock) {
            try {
                if (mStringBuilder == null) {
                    return;
                }
                final File file = new File(mLogFilePath);
                if(!file.exists()){
                	file.createNewFile();
                }
                FileInputStream fis = new FileInputStream(file);
                String logText = Utility.readFile(fis);
                fis.close();
                if (TextUtils.isEmpty(logText)) {
                    return;
                }
                final String log = logText + getLogOutToken();
                
                if (canUpload()) {
                    mLastUploadTime = System.currentTimeMillis();
                    new Thread(new Runnable() {
                        
                        @Override
                        public void run() {
                            mLastUploadTime = System.currentTimeMillis();
                            
                            FeedbackUpload feedbackUpload = new FeedbackUpload(mContext);
                            feedbackUpload.addParameter(mServerParameterKey, log);
                            
                            feedbackUpload.query();
                            
                            com.tigerknows.model.Response response = feedbackUpload.getResponse();
                            // 提交成功或日志文件已经超长最大长度
                            if ((response != null && response.getResponseCode() == com.tigerknows.model.Response.RESPONSE_CODE_OK) ||
                                    mLogFileLength > FILE_MAX_LENGTH) {

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
    
    /**
     * 是否可以上传日志
     * @return
     */
    protected boolean canUpload() {
        return true;
    }
    
    /**
     * 获取日志超长的标识符
     * @return
     */
    protected String getLogOutToken() {
        return "";
    }
    
    /**
     * 日志超长事件
     */
    protected void onLogOut() {
        mLogFileLength = 0;
    }
    
    /**
     * 在主Acitivty执行onCreate()时调用
     */
    public void onCreate() {
        synchronized (mLock) {
            if (onCreate) {
                return;
            }
            onCreate = true;
            
            File file = new File(mLogFilePath);
            long lastModified = file.lastModified();
            long current = System.currentTimeMillis();
            
            if (current - lastModified >= ACCESS_TIME_OUT) {
                upload();
            } 
            
        }
    }
    
    /**
     * 在主Acitivty执行onDestroy()时调用
     */
    public void onDestroy() {
        synchronized (mLock) {
            write();
            onCreate = false;
        }
    }
    
    /**
     * 在Application执行onTerminate()时调用
     */
    public void onTerminate() {
        synchronized (mLock) {
            write();
            onCreate = false;
        }
    }
    
    /**
     * 将缓存区的数据以追加方式写入到日志文件
     */
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

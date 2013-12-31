/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */
package com.tigerknows.common;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
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
     * 当前日志文件的长度
     */
    protected long mLogFileLength = 0;
    
    /**
     * 临时日志文件的路径
     */
    protected String mTmpLogFilePath;
    
    /**
     * 临时日志文件的长度
     */
    protected long mTmpLogFileLength = 0;
    
    protected boolean mUploading = false;
    
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
            mLogFilePath = TKConfig.getDataPath(false) + logFileName;
            mTmpLogFilePath = mLogFilePath + ".tmp";
            mServerParameterKey = serverParameterKey;
            
            try {
                File file = new File(mLogFilePath);
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        LogWrapper.e(TAG, "Unable to create new file: " + mLogFilePath);
                        return;
                    }
                }
                mLogFileLength = file.length();
                
                mStringBuilder = new StringBuilder();
                
                file = new File(mTmpLogFilePath);
                if (file.exists()) {
                    String log = readLogText(true);
                    if (TextUtils.isEmpty(log) == false) {
                        mStringBuilder.append(log);
                        write(false);
                    }
                    if (file.delete() == false) {
                        LogWrapper.e(TAG, "Unable to delete file: " + mTmpLogFilePath);
                        return;
                    }
                }
                
                if (!file.createNewFile()) {
                    LogWrapper.e(TAG, "Unable to create new file: " + mTmpLogFilePath);
                    return;
                }
    
            } catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 检测是否可以写入数据及上传日志
     */
    protected void writeAndUpload() {
        synchronized (mLock) {
            try {
                if (mStringBuilder == null) {
                    return;
                }
    
                // 判断是否要写入文件
                if (mStringBuilder.length() > WRITE_FILE_SIZE) {
                    write(mUploading);
                    
                }
                
                // 判断是否要上传
                if (mLogFileLength > FILE_LENGTH_OUT) {
                    upload();
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
    String readLogText(boolean temp) {
        synchronized (mLock) {
            String logText = null;
            try {
                File file = new File(temp ? mTmpLogFilePath : mLogFilePath);
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
    boolean deleteLogFile(boolean temp) {
        synchronized (mLock) {
            boolean result = false;
            try {
                File file = new File(temp ? mTmpLogFilePath : mLogFilePath);
                if(file.exists()){
                    result = file.delete();
                } else {
                    result = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return result;
        }
    }
    
    /**
     * 若距离上次提交日志是否超过2分钟则提交
     * @param context
     * @param logUploadArray
     */
    public static void upload(final Context context, final LogUpload... logUploadArray) {
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
                    synchronized (logUpload.mLock) {
                        String logText = logUpload.readLogText(false);
                        long lastUploadTime = logUpload.mLastUploadTime;
                        if (logUpload.mUploading == false && lastUploadTime != 0 && currentTime - lastUploadTime > UPLOAD_TIME_OUT && TextUtils.isEmpty(logText) == false) {
                            logUpload.mUploading = true;
                            logUpload.mLastUploadTime = currentTime;
                            feedbackUpload.addParameter(logUpload.mServerParameterKey, logText);
                            logUploadList.add(logUpload);
                        }
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
                        synchronized (logUploadList.get(i).mLock) {
                            logUploadList.get(i).swapfile();
                            logUploadList.get(i).mUploading = false;
                        }
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
                if (mUploading || canUpload() == false) {
                    return;
                }
                String logText = readLogText(false);
                if (TextUtils.isEmpty(logText)) {
                    return;
                }
                final String log = logText + getLogOutToken();
                
                mUploading = true;
                mLastUploadTime = System.currentTimeMillis();
                mStringBuilder.append(getLogOutToken());
                write(true);
                
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        mLastUploadTime = System.currentTimeMillis();
                        
                        FeedbackUpload feedbackUpload = new FeedbackUpload(mContext);
                        feedbackUpload.addParameter(mServerParameterKey, log);
                        
                        feedbackUpload.query();
                        
                        synchronized (mLock) {
                            com.tigerknows.model.Response response = feedbackUpload.getResponse();
                            // 提交成功或日志文件已经超长最大长度
                            if ((response != null && response.getResponseCode() == com.tigerknows.model.Response.RESPONSE_CODE_OK) ||
                                    mLogFileLength > FILE_MAX_LENGTH) {
                                swapfile();
                            }
                            
                            mUploading = false;
                        }
                    }
                }).start();
            } catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    boolean swapfile() {
        synchronized (mLock) {
            boolean result = false;
            try {
                if (deleteLogFile(false) == false) {
                    return result;
                }
                
                File file = new File(mLogFilePath);
                if (!file.createNewFile()) {
                    LogWrapper.e(TAG, "Unable to create new file: " + mLogFilePath);
                    return result;
                }
                mLogFileLength = 0;
                
                StringBuilder newStringBuilder = new StringBuilder();
                
                String oldLogText = readLogText(true);
                if (oldLogText != null) {
                    newStringBuilder.append(oldLogText);
                }
                if (mStringBuilder != null) {
                    newStringBuilder.append(mStringBuilder);
                }

                mStringBuilder = newStringBuilder;
                
                write(false);

                if (deleteLogFile(true) == false) {
                    return result;
                }
                
                file = new File(mTmpLogFilePath);
                if (!file.createNewFile()) {
                    LogWrapper.e(TAG, "Unable to create new file: " + mTmpLogFilePath);
                    return result;
                }
                mTmpLogFileLength = 0;
                
                result = true;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
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
     * 在Acitivty执行onCreate()时调用
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
            write(mUploading);
            onCreate = false;
        }
    }
    
    /**
     * 在Application执行onTerminate()时调用
     */
    public void onTerminate() {
        synchronized (mLock) {
            write(mUploading);
        }
    }
    
    /**
     * 将缓存区的数据以追加方式写入到日志文件
     */
    protected boolean write(boolean temp) {
        synchronized (mLock) {
            boolean result = false;
            
            if (mStringBuilder == null) {
                return result;
            }
            
            if (mStringBuilder.length() > 0) {
                try {
                    if (temp) {
                        Utility.writeFile(mTmpLogFilePath, mStringBuilder.toString().getBytes(), false);
                        File file = new File(mTmpLogFilePath);
                        mTmpLogFileLength = file.length();
                    } else {
                        Utility.writeFile(mLogFilePath, mStringBuilder.toString().getBytes(), false);
                        File file = new File(mLogFilePath);
                        mLogFileLength = file.length();
                    }
                    
                    mStringBuilder = new StringBuilder();
                    result = true;
                    
                } catch (Exception e) {
                    mStringBuilder = null;
                    e.printStackTrace();
                }
            }
            
            return result;
        }
    }
}

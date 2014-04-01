
package com.tigerknows.service.download;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.model.AppPush;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.AppPushResponse;
import com.tigerknows.model.DataQuery.AppPushResponse.AppPushList;
import com.tigerknows.model.Response;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.util.Utility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

/**
 * 此类要独立运行,尽量不要与其他任何类共用代码
 * @author xupeng
 *
 */
public class AppService extends IntentService {
    
    static final String TAG = "AppService";

    public static final String EXTRA_URL = "extra_url";
    
    public static final String DEFAULT_T_RANGE = "7";
    
    public static final String SAVED_DATA_FILE = "App";
    
    static final long RETRY_TIME = 6 *1000;
    
    private static ArrayList<String> sDownloadList = new ArrayList<String>();
   
    // 发现网络不是wifi的时候停止下载
    public static void stopDownload(Context ctx) {
        Intent service = new Intent(ctx, AppService.class);
        ctx.stopService(service);
    }

    private boolean stop = false; // 目前不可取消，因此没有设置取消接口
    
    private HttpClient httpClient;
    
    public AppService() {
        super(TAG);
    }
        
    @Override
    protected void onHandleIntent(Intent intent) {
        // 当时正在下载的，此时已下载完成。
        String url = intent.getStringExtra(EXTRA_URL);
        LogWrapper.d(TAG, "handle url:" + url);
        File tempFile = null;
        do {
            tempFile = downFile(url);
            if (!stop) {
                try {
                    Thread.sleep(RETRY_TIME);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        } while (tempFile == null && !stop);
        if(tempFile != null && !stop) {
            //已下载完成
            TKConfig.setPref(getApplicationContext(), TKConfig.PREFS_APP_PUSH_FINISHED_APP, tempFile.getName());
            TKConfig.setPref(getApplicationContext(), TKConfig.PREFS_APP_PUSH_FINISHED_TIME, String.valueOf(System.currentTimeMillis()));
            resetT(getApplicationContext());
            LogWrapper.d(TAG, "download finished");
        }
        synchronized (sDownloadList) {
            sDownloadList.remove(url);
        }
    }
     
    @Override
    public void onDestroy() {
        stop = true;
        LogWrapper.d(TAG, "onDestroy");
        super.onDestroy();
    }
    
    // 下载更新文件，成功返回File对象，否则返回null
    private File downFile(String url) {
        try {
            if (BaseQueryTest.UnallowedAccessNetwork) {
                Thread.sleep(5000);
                throw new IOException("Unallowed access network");
            }
            File tempFile = createFileByUrl(url);
            long fileSize = tempFile.length();
            LogWrapper.d(TAG, "fileSize:"+fileSize);
            httpClient = HttpManager.getNewHttpClient();
            HttpUriRequest request = new HttpGet(url);
            
            // 支持断点续传
            // Range:(unit=first byte pos)-[last byte pos] 
            // 指定第一个字节的位置和最后一个字节的位置
            // 在http请求头加入RANGE指定第一个字节的位置
            if (fileSize > 0) {
                request.addHeader("RANGE", "bytes="+fileSize+"-");
            }
            HttpResponse response = HttpUtils.execute(getApplicationContext(), httpClient, request, url, "downloadService");
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();   //TODO: getContentLength()某些服务器可能返回-1
            LogWrapper.d(TAG, "length:"+length);
            InputStream is = entity.getContent();
            StatusLine status = response.getStatusLine();
            LogWrapper.d(TAG, "status.getStatusCode():"+status.getStatusCode());
            if(status != null) {
                int statusCode = status.getStatusCode();
               
                if (statusCode == 416 && fileSize > 0) {
                    if (!stop) {
                        return tempFile;
                    } else {
                        return null;
                    }
                } else if(((statusCode == 200 && fileSize == 0) || (statusCode == 206 && fileSize > 0)) && is != null) {
                    BufferedInputStream bis = new BufferedInputStream(is);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile, true));
                    int read = 0;
                    long count = fileSize;
                    int percent = 0;
                    int counted_percent = 0;
                    byte[] buffer = new byte[1024];
                    while (!stop && (read = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, read);
                        count += read;
                        percent = (int)(((double)count / length) * 100);
                        if(percent - counted_percent >= 1) {
                            counted_percent = percent;
                        }
                    }
                    bos.flush();
                    bos.close();
                    is.close();
                    bis.close();
                    if (!stop) {
                        return tempFile;
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
     
    private static void download(Context context, String url) {
        if (!TextUtils.isEmpty(url)) {
            Intent service = new Intent(context, AppService.class);
            service.putExtra(EXTRA_URL, url);
            synchronized (sDownloadList) {
                if(!sDownloadList.contains(url)) {// 若已正在下载，则必然已注册过，否则可新注册一个processor
                    sDownloadList.add(url);
                    context.startService(service);
                    LogWrapper.d(TAG, "start service:");
                }
            }
        }
    }
     
    public static void resetAppPush(Context ctx) {
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_FINISHED_APP, "");
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_FINISHED_TIME, "");
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_NOTIFY, "");
    }
    
    public static String getAppPath() {
        String path = null;
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            path = externalStorageDirectory.getAbsolutePath() + "/app/";
        }
        LogWrapper.d(TAG, "get down path:" + path);
        return path;
    }
    
    // 去服务器获取apppush列表
    public static AppPushList checkAppPushList(Context ctx) {
        DataQuery dataQuery = new DataQuery(ctx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_APP_PUSH);
        dataQuery.query();
        Response response = dataQuery.getResponse();
        if (response != null && response instanceof AppPushResponse) {
            int resCode = response.getResponseCode();
            AppPushResponse appResponse = (AppPushResponse) response;
            if (resCode == 200) {
                return appResponse.getList();
            }
        }
        return null;
    }
    
    public static void checkAndDown(Context ctx) {
       new CheckDownRunnable(ctx).run();
    }
    
    private static File createFileByUrl(String url) throws IOException {
        File tempFile = null;
        String path = getAppPath();
        if (path == null) {
            return tempFile;
        }
        File rootFile = new File(path);
        if (!rootFile.exists() && !rootFile.isDirectory())
            rootFile.mkdir();
        tempFile = new File(path, url.substring(url.lastIndexOf("/") + 1).replaceAll("[?]", "@"));
        if (tempFile.exists() == false) {
            tempFile.createNewFile();
        }
        return tempFile;
    }
   
    static class CheckDownRunnable implements Runnable {

        Context ctx;
        
        public CheckDownRunnable (Context c) {
            ctx = c;
        }
        
        @Override
        public void run() {
            LogWrapper.d(TAG, "checking");
            AppPushList list = checkAppPushList(ctx);
            LogWrapper.d(TAG, "list:" + list);
            if (list == null) {
                return;
            }
            //TODO:如何决定下载哪个
            AppPush app = list.getList().get(0);
            if (app == null) {
                return;
            }
            XMap xmap = app.getData();
            try {
                byte[] b = ByteUtil.xobjectToByte(xmap);
                Utility.writeFile(TKConfig.getSavePath() + SAVED_DATA_FILE, b, true);
                download(ctx, app.getDownloadUrl());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    // t为下载完成到弹出通知的间隔时间
    public final static void resetT(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T_RANGE, DEFAULT_T_RANGE));
        Random r = new Random(System.currentTimeMillis());
        int t = r.nextInt(T) + 1;
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T, String.valueOf(t));
    }
    
    // T为弹出时间的范围，下次弹出通知的时间t为[1,T]中的随机值
    public final static void increaseTRange(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T_RANGE, DEFAULT_T_RANGE));
        T = Math.min(T * 2, 14);
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T_RANGE, String.valueOf(T));
    }
    
    public final static void decreaseTRange(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T_RANGE, DEFAULT_T_RANGE));
        T = Math.max(T / 2, 2);
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T_RANGE, String.valueOf(T));
    }
    
}

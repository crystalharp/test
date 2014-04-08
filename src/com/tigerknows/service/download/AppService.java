
package com.tigerknows.service.download;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.app.TKService;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.model.AppPush;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.AppPushResponse;
import com.tigerknows.model.DataQuery.AppPushResponse.AppPushList;
import com.tigerknows.model.Response;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.provider.PackageInfoTable;
import com.tigerknows.provider.PackageInfoTable.RecordPackageInfo;
import com.tigerknows.radar.AppPushNotify;
import com.tigerknows.util.HttpUtils;
import com.weibo.sdk.android.WeiboParameters;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 此类要独立运行,尽量不要与其他任何类共用代码
 * @author xupeng
 *
 */
public class AppService extends TKService {

    static final String TAG = "AppService";
    
    static final long RETRY_TIME = 6 *1000;
    
    private static PackageInfoTable sRecordPkgTable = null;
    
    private boolean stop = false; // 目前不可取消，因此没有设置取消接口
    
    private HttpClient httpClient;
    
    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable(){

            @Override
            public void run() {
                LogWrapper.d(TAG, "checking");
                Context ctx = getApplicationContext();
                if (sRecordPkgTable == null) {
                    sRecordPkgTable = PackageInfoTable.getInstance(ctx);
                }

                AppPush app = null;
                String url = null;
                try {
                    // 扫描本地包，对比数据库，不在本地数据库的包插入本地数据库
                	LogWrapper.d(TAG, "checkLocalInstalled");
                    checkLocalInstalled(ctx);

                    // 找出文件名不为空且通知(时间超过了时间间隔或未记录通知时间)的记录删除
                    LogWrapper.d(TAG, "checkUserDeleted");
                    resetUserDeleted();

                    // 找出文件名不为空且已通知(但时间超过了时间间隔)的记录删除
                    LogWrapper.d(TAG, "resetIgnoredNotify");
                    resetIgnoredNotify();
                    
                    AppPushList list = queryAppPushList(ctx);
                    LogWrapper.d(TAG, "list:" + list);
                    if (list == null) {
                        return;
                    }
                    // 更新T，list.getMessage();
                    String tRange = list.getMessage();
                    if (!TextUtils.isEmpty(tRange)) {
                        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T, tRange);
                    }

                    // 找一个不在本地数据库中的优先级最高的包来进行下载, appList已在服务器端排好序
                    List<AppPush> appList = list.getList();
                    LogWrapper.d(TAG, "selectApp");
                    app = selectApp(appList);
                } catch (APIException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (app == null) {
                    return;
                }
                url = app.getDownloadUrl();
                String imgUrl = app.getIcon();
                if (url == null || imgUrl == null) {
                    return;
                }

                File tempFile = null;
                do {
                    tempFile = downFile(ctx, url);
                    if (!stop) {
                        try {
                            Thread.sleep(RETRY_TIME);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                } while (tempFile == null && !stop);
                
                File imgFile = null;
                do {
                	imgFile = downFile(ctx, imgUrl);
                    if (!stop) {
                        try {
                            Thread.sleep(RETRY_TIME);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                } while (imgFile == null && !stop);                

                if(tempFile != null && imgFile != null && !stop) {
                    try {
                        RecordPackageInfo p = new RecordPackageInfo(app.getPackageName(), tempFile.getName(), app);
                        sRecordPkgTable.addPackageInfo(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (APIException e) {
                        e.printStackTrace();
                    }
                    LogWrapper.d(TAG, "download finished");
                }
            }}).start();

    }
    
    // 发现网络不是wifi的时候停止下载
    public static void stopDownload(Context ctx) {
        Intent service = new Intent(ctx, AppService.class);
        ctx.stopService(service);
    }
     
    @Override
    public void onDestroy() {
        stop = true;
        LogWrapper.d(TAG, "onDestroy");
        super.onDestroy();
    }
    
    // 下载更新文件，成功返回File对象，否则返回null
    private File downFile(Context context, String url) {
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
                    
//                    File imgFile = createFileByUrl(imageUrl);
//                    HttpClient httpClient = HttpManager.getNewHttpClient();
//                    byte[] data = HttpManager.openUrl(context, httpClient, imageUrl, "GET", new WeiboParameters());
//                    File file=new File(getAppPath(), imgFile.getName());
//                    FileOutputStream out=new FileOutputStream(file);
//                    out.write(data);
//                    out.close();
                    
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
    public static AppPushList queryAppPushList(Context ctx) {
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
    
    public static void checkAndDown(Context ctx, int status) {
        if (status == 1) {
            Intent service = new Intent(ctx, AppService.class);
            ctx.startService(service);
        }
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

    private void checkLocalInstalled(Context ctx) throws APIException, IOException{
        PackageManager manager = ctx.getPackageManager();
        List <PackageInfo> pkgList = manager.getInstalledPackages(0);
        List <RecordPackageInfo> rPkgList = new ArrayList<RecordPackageInfo>();
        int n = sRecordPkgTable.readPackageInfo(rPkgList);
        if (n > 0) {
            // TODO: pwy:是否可以优化一下？，另外可能需要sync加锁？
            for (PackageInfo pI : pkgList){
                boolean found = false;
                for(RecordPackageInfo pkg : rPkgList){
                    if (TextUtils.equals(pI.packageName, pkg.package_name)){
                    	if(pkg.installed != 1){
                    		// 尝试减少更新数据库的次数
                    		pkg.installed = 1;
                    		sRecordPkgTable.updateDatabase(pkg);
                    	}
                        if(pkg.notify_time != 0){
                            // TODO: 将已通知且已安装的条目记录一下
                        }
                        found = true;
                        break;
                    }
                }
                if(found == false){
                    RecordPackageInfo p = new RecordPackageInfo(pI.packageName, 1, 0);
                    sRecordPkgTable.addPackageInfo(p);
                }
            }
            // TODO: 将已通知且已安装的条目上传
        }
    }

    private void resetUserDeleted() throws APIException, IOException{
        List <RecordPackageInfo> rPkgList = new ArrayList<RecordPackageInfo>();
        int n = sRecordPkgTable.readPackageInfo(rPkgList, -1, 0);
        long now = System.currentTimeMillis();
        if (n > 0) {
            for (RecordPackageInfo pkg : rPkgList) {
                if (!TextUtils.isEmpty(pkg.file_name)) {
                    sRecordPkgTable.deletePackageInfo(pkg);
                }
            }
        }            	
    }

    private void resetIgnoredNotify() throws APIException, IOException{
        List <RecordPackageInfo> rPkgList = new ArrayList<RecordPackageInfo>();
        int n = sRecordPkgTable.readPackageInfo(rPkgList, -1, 1);
        long now = System.currentTimeMillis();
        if (n > 0) {

            for (RecordPackageInfo pkg : rPkgList) {
                if (!TextUtils.isEmpty(pkg.file_name)) {
                    if ((now - pkg.notify_time)/1000 > AppPushNotify.DAY_SECS) {
                        File file = new File(getAppPath() + pkg.file_name);
                        if (file.exists()) {
                            file.delete();
                        }
                        AppPush app = pkg.app_push;
                        if(app != null){
                        	File imgFile = new File(getAppPath() + app.getIconFileName());
                        	if(imgFile.exists()){
                        		imgFile.delete();
                        	}
                        }
                        pkg.file_name = null;
                        sRecordPkgTable.updateDatabase(pkg);
                    }
                }
            }
        }        	
    }

    private AppPush selectApp(List<AppPush> appList) throws APIException, IOException {
        List <RecordPackageInfo> rPkgList = new ArrayList<RecordPackageInfo>();
        sRecordPkgTable.readPackageInfo(rPkgList);
        int n = appList.size();
        AppPush app = null;
        boolean found;
        for (int i = 0; i < n; i++) {
            AppPush tmpApp = appList.get(i);
            found = false;
            for (RecordPackageInfo pkg : rPkgList) {
                if (TextUtils.equals(pkg.package_name, tmpApp.getPackageName())) {
                    found = true;
                }
            }
            if (!found) {
                app = tmpApp;
                break;
            }
        }
        return app;
    }
    
}

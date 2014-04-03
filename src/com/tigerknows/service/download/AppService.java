
package com.tigerknows.service.download;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.example.AppUtil;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.model.AppPush;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.AppPushResponse;
import com.tigerknows.model.DataQuery.AppPushResponse.AppPushList;
import com.tigerknows.model.Response;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.PackageInfoTable;
import com.tigerknows.provider.PackageInfoTable.RecordPackageInfo;
import com.tigerknows.radar.AppPushNotify;
import com.tigerknows.util.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.app.IntentService;
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
public class AppService extends IntentService {
    
    static final String TAG = "AppService";

    public static final String EXTRA_URL = "extra_url";
    
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    
    public static final String EXTRA_NAME = "name";
    
    public static final String EXTRA_ICON = "icon";
    
    public static final String EXTRA_PRIOR = "prior";
    
    public static final String EXTRA_DESCRIPTION = "description";
    
    public static final String SAVED_DATA_FILE = "App";
    
    static final long RETRY_TIME = 6 *1000;
    
    private static ArrayList<String> sDownloadList = new ArrayList<String>();
    
    private static PackageInfoTable sRecordPkgTable = null;
   
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
        String pname = intent.getStringExtra(EXTRA_PACKAGE_NAME);
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
        	XMap xmap = new XMap();
        	xmap.put(AppPush.FIELD_NAME, intent.getStringExtra(EXTRA_NAME));
        	xmap.put(AppPush.FIELD_ICON, intent.getStringExtra(EXTRA_ICON));
        	xmap.put(AppPush.FIELD_DESCRIPTION, intent.getStringExtra(EXTRA_DESCRIPTION));
        	xmap.put(AppPush.FIELD_PRIOR, intent.getStringExtra(EXTRA_PRIOR));
        	xmap.put(AppPush.FIELD_PACKAGE_NAME, pname);
        	xmap.put(AppPush.FIELD_DOWNLOAD_URL, url);
            try {
            	AppPush app = new AppPush(xmap);
            	RecordPackageInfo p = new RecordPackageInfo(pname, tempFile.getName(), app);
				sRecordPkgTable.addPackageInfo(p);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (APIException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
     
    private static void download(Context context, AppPush app) {
    	String url = app.getDownloadUrl();
        if (!TextUtils.isEmpty(url)) {
            Intent service = new Intent(context, AppService.class);
            service.putExtra(EXTRA_URL, url);
            service.putExtra(EXTRA_PACKAGE_NAME, app.getPackageName());
            service.putExtra(EXTRA_NAME, app.getName());
            service.putExtra(EXTRA_PRIOR, app.getPrior());
            service.putExtra(EXTRA_ICON, app.getIcon());
            service.putExtra(EXTRA_DESCRIPTION, app.getDescription());
            synchronized (sDownloadList) {
                if(!sDownloadList.contains(url)) {// 若已正在下载，则必然已注册过，否则可新注册一个processor
                    sDownloadList.add(url);
                    context.startService(service);
                    LogWrapper.d(TAG, "start service:");
                }
            }
        }
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
            synchronized(sDownloadList) {
                if (sRecordPkgTable == null) {
                    sRecordPkgTable = new PackageInfoTable(ctx);
                }
            }
            Runnable r = new CheckDownRunnable(ctx);
            r.run();
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
   
    static class CheckDownRunnable implements Runnable {

        Context ctx;
        
        public CheckDownRunnable (Context c) {
            ctx = c;
        }
        
        @Override
        public void run() {
            LogWrapper.d(TAG, "checking");

			try {
            	// 扫描本地包，对比数据库，不在本地数据库的包插入本地数据库
				checkLocalInstalled();
				
				// 找出文件名不为空且通知(时间超过了时间间隔或未记录通知时间)的记录删除
				resetUserDeleted();
				
                // 找出文件名不为空且已通知(但时间超过了时间间隔)的记录删除
				resetIgnoredNotify();
	        } catch (APIException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

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
            //TODO:pwy: 需要找一个不在本地数据库中的优先级最高的包来进行下载
            AppPush app = list.getList().get(0);
            if (app == null) {
                return;
            }
            download(ctx, app);
        }
        
        private void checkLocalInstalled() throws APIException, IOException{
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
            				pkg.installed = 1;
            				if(pkg.notify_time != 0){
            					// TODO: 将已通知且已安装的条目记录一下
            				}
            				sRecordPkgTable.updateDatabase(pkg);
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
                            pkg.file_name = null;
                            sRecordPkgTable.updateDatabase(pkg);
                        }
                    }
                }
            }        	
        }
        
    }
    
}

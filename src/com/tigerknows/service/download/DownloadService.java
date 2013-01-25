
package com.tigerknows.service.download;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.net.Utility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.tigerknows.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DownloadService extends IntentService {
	
	private static Set<String> downloadingUrlSet = new HashSet<String>();
    
	private static Map<String, DownloadedProcessor> processorMap = new HashMap<String, DownloadedProcessor>();
    
	public static void download(Context context, String url, String showName, DownloadedProcessor processor) {
        if (!TextUtils.isEmpty(url)) {
            Intent service = new Intent(context, DownloadService.class);
            service.putExtra("url", url);
            service.putExtra("showName", showName);
            service.putExtra("ticket", context.getString(R.string.download_software_title));
            if(!downloadingUrlSet.contains(url)) {// 若已正在下载，则必然已注册过，否则可新注册一个processor
            	DownloadService.registerProcessor(url, processor);
	            context.startService(service);
	            LogWrapper.d("chen", "to download"+url);
            }
            else {
                Toast.makeText(context, R.string.file_downloading, com.tigerknows.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
    
	public static boolean registerProcessor(String url, DownloadedProcessor processor) {
    	if(processorMap.get(url) != null) {
    		return false;
    	}
    	else {
    		processorMap.put(url, processor);
    		return true;
    	}
    }
    
    public static void unRegisterProcessor(String url) {
    	processorMap.remove(url);
    }
    
	private NotificationManager nm = null;

    private Notification notification = null;

    private boolean cancelUpdate = false; // 目前不可取消，因此没有设置取消接口

    private RemoteViews views = null;

    private int notificationId = R.string.download_service;
    
    
    public DownloadService() {
		super("DownloadService");
	}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	String url = intent.getStringExtra("url");
    	LogWrapper.d("chen", "this: " + this + ", 1url=" + url);
    	// 由于外部可不掉用download静态方法来下载而直接调用startService来启动服务，因此这里仍需判断重复
    	if(downloadingUrlSet.contains(url)) {
    		intent.putExtra("isDownloading", true);
    		Toast.makeText(this, R.string.file_downloading, Toast.LENGTH_SHORT).show();
    	}
    	else {
        	downloadingUrlSet.add(url);
        	Toast.makeText(this, R.string.add_to_download_list, Toast.LENGTH_SHORT).show();
    	}
        return super.onStartCommand(intent, flags, startId);
    }
    
	@Override
	protected void onHandleIntent(Intent intent) {
		// 当时正在下载的，此时已下载完成。
		boolean isDownloaded = intent.getBooleanExtra("isDownloading", false);
		if(isDownloaded) {
			LogWrapper.d("chen", "this: " + this + ", the url has just been downloaded");
			return;
		}
		String url = intent.getStringExtra("url");
		String showName = intent.getStringExtra("showName");
		LogWrapper.d("chen", "this: " + this + ", 2url=" + url);
		String ticket = intent.getStringExtra("ticket");
		if (TextUtils.isEmpty(url) == false) {
		    LogWrapper.d("chen", "this: " + this + ", 3url=" + url);
		    createProcessView(ticket);
		    File tempFile = downFile(url, showName);
		    if(tempFile != null) {
			    DownloadedProcessor processor = processorMap.get(url);
			    if(processor != null)
			    	processor.process(tempFile, this);
                nm.cancel(notificationId);
		    }
		}
		downloadingUrlSet.remove(url);
	}
    
	// 在状态栏添加下载进度条
    private void createProcessView(String ticket) {
    	LogWrapper.d("chen", "create process view");
    	nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    notification = new Notification();
	    notification.icon = android.R.drawable.stat_sys_download;
	    notification.tickerText = ticket;
	    notification.when = System.currentTimeMillis();
	    notification.defaults = Notification.DEFAULT_LIGHTS;
	
	    // 设置任务栏中下载进程显示的views
	    views = new RemoteViews(getPackageName(), R.layout.download_progress);
	    notification.contentView = views;
	
	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Sphinx.class), 0);
	    notification.setLatestEventInfo(this, "", "", contentIntent);
	
	    // 将下载任务添加到任务栏中
	    nm.notify(notificationId, notification);
    }
    
    // 下载更新文件，成功返回File对象，否则返回null
    private File downFile(final String url, String showName) {
        try {
            HttpClient client = Utility.getNewHttpClient(getApplicationContext());
            HttpResponse response = client.execute(new HttpGet(url));
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();
            InputStream is = entity.getContent();
            if(is != null) {
	            File tempFile = createFileByUrl(url);
	            BufferedInputStream bis = new BufferedInputStream(is);
	            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile));
	            int read = 0;
	            long count = 0;
	            int percent = 0;
	            int counted_percent = 0;
	            byte[] buffer = new byte[1024];
	            notify(showName, 0);
	            while ((read = bis.read(buffer)) != -1 && !cancelUpdate) {
	                bos.write(buffer, 0, read);
	                count += read;
	                percent = (int)(((double)count / length) * 100);
	                if(percent - counted_percent >= 1) {
	                	notify(showName, percent);
	                	counted_percent = percent;
	                }
	            }
	            bos.flush();
	            bos.close();
	            is.close();
	            bis.close();
	            if (!cancelUpdate) {
	            	LogWrapper.d("chen", "download complete");
	                return tempFile;
	            } else {
	            	LogWrapper.d("chen", "download canceled");
	                tempFile.delete();
	                return null;
	            }
            }
        } catch (Exception e) {
        }
    	nm.cancel(notificationId);
        Toast.cancel();
        LogWrapper.d("chen", "download failed");
        return null;
    }
    
    private File createFileByUrl(String url) throws IOException {
    	File tempFile = null;
    	String path = MapEngine.getInstance().getMapPath();
        File rootFile = new File(path);
        if (!rootFile.exists() && !rootFile.isDirectory())
            rootFile.mkdir();
        tempFile = new File(path, url.substring(url.lastIndexOf("/") + 1));
        if (tempFile.exists())
            tempFile.delete();
        tempFile.createNewFile();
        return tempFile;
    }
    
    private void notify(String showName, int percent) {
        views.setTextViewText(R.id.process_txv, "已下载" + showName + percent + "%");
        views.setProgressBar(R.id.process_prb, 100, percent, false);
        notification.contentView = views;
        nm.notify(notificationId, notification);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogWrapper.d("chen", "this: " + this + ", to destroy");
    }
}

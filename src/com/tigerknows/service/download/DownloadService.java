
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
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DownloadService extends IntentService {
	private NotificationManager nm = null;

    private Notification notification = null;

    private boolean cancelUpdate = false;

    private RemoteViews views = null;

    private int notificationId = R.string.download_service;
    
    private static Map<String, DownloadedProcessor> processorMap = new HashMap<String, DownloadedProcessor>();

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
    
    public DownloadService() {
		super("DownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String url = intent.getStringExtra("url");
		String msg = intent.getStringExtra("msg");
		String ticket = intent.getStringExtra("ticket");
		LogWrapper.d("chen", url + ", " + msg + ", " + ticket);
		if (TextUtils.isEmpty(url) == false) {
		    LogWrapper.d("test", "url=" + url);
		    createProcessView(ticket);
		    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		    File tempFile = downFile(url);
		    if(tempFile != null) {
			    DownloadedProcessor processor = processorMap.get(url);
			    if(processor != null)
			    	processor.process(tempFile, this);
                nm.cancel(notificationId); // TODO: 如果下载失败，是否需要cancel？
		    }
		}
	}
    
	// 在状态栏添加下载进度条
    private void createProcessView(String ticket) {
    	LogWrapper.d("chen", "create process view");
    	nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    notification = new Notification();
	    notification.icon = android.R.drawable.stat_sys_download;
	    LogWrapper.d("chen", Integer.toHexString(android.R.drawable.stat_sys_download));
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
    private File downFile(final String url) {
        try {
            HttpClient client = Utility.getNewHttpClient(getApplicationContext());
            // params[0]代表连接的url
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();
            InputStream is = entity.getContent();
            File tempFile = null;
            if (is != null) {
                String path = MapEngine.getInstance().getMapPath();
                File rootFile = new File(path);
                if (!rootFile.exists() && !rootFile.isDirectory())
                    rootFile.mkdir();

                tempFile = new File(path, url.substring(url.lastIndexOf("/") + 1));
                if (tempFile.exists())
                    tempFile.delete();
                tempFile.createNewFile();

                // 已读出流作为参数创建一个带有缓冲的输出流
                BufferedInputStream bis = new BufferedInputStream(is);

                // 创建一个新的写入流，讲读取到的图像数据写入到文件中
                FileOutputStream fos = new FileOutputStream(tempFile);
                // 已写入流作为参数创建一个带有缓冲的写入流
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                int read;
                long count = 0;
                int percent = 0;
                int download_percent = 0;
                byte[] buffer = new byte[1024];
                while ((read = bis.read(buffer)) != -1 && !cancelUpdate) {
                    bos.write(buffer, 0, read);
                    count += read;
                    percent = (int)(((double)count / length) * 100);
                    if(percent - download_percent >= 1) {
	                    views.setTextViewText(R.id.process_txv, "已下载" + percent + "%");
	                    views.setProgressBar(R.id.process_prb, 100, percent, false);
	                    notification.contentView = views;
	                    nm.notify(notificationId, notification);
	                    download_percent = percent;
                    }
                }
                bos.flush();
                bos.close();
                fos.flush();
                fos.close();
                is.close();
                bis.close();
            }

            if (!cancelUpdate) {
            	LogWrapper.d("chen", "download complete");
                return tempFile;
            } else {
            	LogWrapper.d("chen", "download canceled");
                tempFile.delete();
                return null;
            }
        } catch (ClientProtocolException e) {
        	nm.cancel(notificationId);
        } catch (IOException e) {
        	nm.cancel(notificationId);
        } catch (Exception e) {
        	nm.cancel(notificationId);
        }
        LogWrapper.d("chen", "download failed");
        return null;
    }
}

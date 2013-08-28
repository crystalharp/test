
package com.tigerknows.service.download;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.test.BaseQueryTest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DownloadService extends IntentService {
    
    static final String TAG = "DownloadService";
    
    public static final String EXTRA_URL = "EXTRA_URL";
    
    public static final String EXTRA_TICKERTEXT = "EXTRA_TICKERTEXT";
    
    public static final String EXTRA_IS_DOWNLOADING = "EXTRA_IS_DOWNLOADING";
    
    static final long RETRY_TIME = 6 *1000;
	
	private static Set<String> downloadingUrlSet = new HashSet<String>();
    
	private static Map<String, DownloadedProcessor> processorMap = new HashMap<String, DownloadedProcessor>();
    
	public static void download(Context context, String url, String tickerText, DownloadedProcessor processor) {
        if (!TextUtils.isEmpty(url)) {
            Intent service = new Intent(context, DownloadService.class);
            service.putExtra(EXTRA_URL, url);
            service.putExtra(EXTRA_TICKERTEXT, tickerText);
            if(!downloadingUrlSet.contains(url)) {// 若已正在下载，则必然已注册过，否则可新注册一个processor
            	DownloadService.registerProcessor(url, processor);
	            context.startService(service);
            }
            else {
                Toast.makeText(context, R.string.file_downloading, Toast.LENGTH_SHORT).show();
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
		super(TAG);
	}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	String url = intent.getStringExtra(EXTRA_URL);
    	// 由于外部可不掉用download静态方法来下载而直接调用startService来启动服务，因此这里仍需判断重复
    	if(downloadingUrlSet.contains(url)) {
    		intent.putExtra(EXTRA_IS_DOWNLOADING, true);
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
		boolean isDownloaded = intent.getBooleanExtra(EXTRA_IS_DOWNLOADING, false);
		if(isDownloaded) {
			return;
		}
		String url = intent.getStringExtra(EXTRA_URL);
		String tickerText = intent.getStringExtra(EXTRA_TICKERTEXT);
		if (TextUtils.isEmpty(url) == false) {
		    notify(tickerText);
            notifyPercent(0);
		    File tempFile = null;
		    do {
		        tempFile = downFile(url);
		        // TODO 何时结束重试？
		        if (cancelUpdate) {
		            break;
		        }
		    } while (tempFile == null);
	        nm.cancel(notificationId);
		    if(tempFile != null) {
			    DownloadedProcessor processor = processorMap.get(url);
			    if(processor != null)
			    	processor.process(this, tempFile, tickerText);
		    }
		}
		downloadingUrlSet.remove(url);
	}
    
	// 在状态栏添加下载进度条
    private void notify(String tickerText) {
    	nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    notification = new Notification();
	    notification.icon = R.drawable.icon;
	    notification.tickerText = tickerText;
	    notification.when = System.currentTimeMillis();
	    notification.defaults = Notification.DEFAULT_LIGHTS;
	    notification.flags = Notification.FLAG_ONGOING_EVENT;
	
	    // 设置任务栏中下载进程显示的views
	    views = new RemoteViews(getPackageName(), R.layout.download_progress);
        views.setTextViewText(R.id.name_txv, tickerText);
	    notification.contentView = views;
	
	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, null, 0);
	    notification.setLatestEventInfo(this, tickerText, "已下载" + 0 + "%", contentIntent);
	
	    // 将下载任务添加到任务栏中
	    nm.notify(notificationId, notification);
    }
    
    // 下载更新文件，成功返回File对象，否则返回null
    private File downFile(final String url) {
        try {
            if (BaseQueryTest.UnallowedAccessNetwork) {
                Thread.sleep(5000);
                throw new IOException("Unallowed access network");
            }
            HttpClient client = HttpManager.getNewHttpClient();
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
	            while ((read = bis.read(buffer)) != -1 && !cancelUpdate) {
	                bos.write(buffer, 0, read);
	                count += read;
	                percent = (int)(((double)count / length) * 100);
	                if(percent - counted_percent >= 1) {
	                	notifyPercent(percent);
	                	counted_percent = percent;
	                }
	            }
	            bos.flush();
	            bos.close();
	            is.close();
	            bis.close();
	            if (!cancelUpdate) {
	                return tempFile;
	            } else {
	                tempFile.delete();
	                return null;
	            }
            } else {
                LogWrapper.d(TAG, "downFile() entity.getContent() is null");
                try {
                    Thread.sleep(RETRY_TIME);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.sleep(RETRY_TIME);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        return null;
    }
    
    private File createFileByUrl(String url) throws IOException {
    	File tempFile = null;
    	String path = MapEngine.getInstance().getMapPath();
        File rootFile = new File(path);
        if (!rootFile.exists() && !rootFile.isDirectory())
            rootFile.mkdir();
        tempFile = new File(path, url.substring(url.lastIndexOf("/") + 1).replaceAll("[?]", "@"));
        if (tempFile.exists())
            tempFile.delete();
        tempFile.createNewFile();
        return tempFile;
    }
    
    private void notifyPercent(int percent) {
        views.setTextViewText(R.id.process_txv, "已下载" + percent + "%");
        views.setProgressBar(R.id.process_prb, 100, percent, false);
        notification.contentView = views;
        nm.notify(notificationId, notification);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

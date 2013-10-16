
package com.tigerknows.service.download;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.util.HttpUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import java.util.Map;

/**
 * 添加的下载项显示在正在进行的通知栏，用url的hashCode作为通知id
 * 暂停或完成的下载项显示在普通的通知栏，用tickerText的hashCode作为通知id
 * @author pengwenyue
 *
 */
public class DownloadService extends IntentService {
    
    static final String TAG = "DownloadService";
    
    public static class DownloadItem {
        String url;
        String tickerText;
        RemoteViews remoteViews;
        Notification notification;
        int percent = 0;
        boolean stop = false;
        long timeMillis;
        
        DownloadItem(String url, String tickerText, RemoteViews remoteViews, Notification notification, int percent) {
            this.url = url;
            this.tickerText = tickerText;
            this.remoteViews = remoteViews;
            this.notification = notification;
            this.percent = percent;
            timeMillis = System.currentTimeMillis();
        }
    }
    
    public static final String EXTRA_URL = "extra_url";
    
    public static final String EXTRA_TICKERTEXT = "extra_tickertext";
    
    public static final String EXTRA_PERCENT = "extra_percent";
    
    public static final String EXTRA_FILE = "extra_file";
    
    public static final String EXTRA_IS_DOWNLOADED = "extra_is_downloaded";
    
    public static final String EXTRA_OPERATION_CODE = "extra_operation_code";
    
    /**
     * 添加
     */
    public static final String OPERATION_CODE_ADD = "add";
    
    /**
     * 清空
     */
    public static final String OPERATION_CODE_CLEAR = "clear";
    
    /**
     * 暂停
     */
    public static final String OPERATION_CODE_PAUSE = "pause";
    
    static final long RETRY_TIME = 6 *1000;
    
    private static HashMap<String, DownloadItem> sDownloadList = new HashMap<String, DownloadItem>();
    
    private static Map<String, DownloadedProcessor> processorMap = new HashMap<String, DownloadedProcessor>();
    
    public static void download(Context context, String url, String tickerText, DownloadedProcessor processor) {
        if (!TextUtils.isEmpty(url)) {
            Intent service = new Intent(context, DownloadService.class);
            service.putExtra(EXTRA_OPERATION_CODE, OPERATION_CODE_ADD);
            service.putExtra(EXTRA_URL, url);
            service.putExtra(EXTRA_TICKERTEXT, tickerText);
            if(!sDownloadList.containsKey(url)) {// 若已正在下载，则必然已注册过，否则可新注册一个processor
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

    private boolean stop = false; // 目前不可取消，因此没有设置取消接口
    
    private HttpClient httpClient;
    
    private DownloadItem currentDownloadItem;
    
    public DownloadService() {
        super(TAG);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String operationCode = intent.getStringExtra(EXTRA_OPERATION_CODE);
        String url = intent.getStringExtra(EXTRA_URL);
        LogWrapper.d(TAG, "onStartCommand() operationCode, url:" + operationCode + "," + url);
        if (OPERATION_CODE_PAUSE.equals(operationCode)) {
            DownloadItem downloadItem = sDownloadList.remove(url);
            if (downloadItem != null) {
                downloadItem.stop = true;
                if (currentDownloadItem != null && currentDownloadItem.url.equals(downloadItem.url)) {
                    if (httpClient != null) {
                        httpClient.getConnectionManager().shutdown();
                    }
                }
                notifyPause(url, downloadItem);
            }
            
            intent.putExtra(EXTRA_IS_DOWNLOADED, true);
        } else if (OPERATION_CODE_ADD.equals(operationCode)) {
            // 由于外部可不掉用download静态方法来下载而直接调用startService来启动服务，因此这里仍需判断重复
            if(sDownloadList.containsKey(url)) {
                intent.putExtra(EXTRA_IS_DOWNLOADED, true);
                Toast.makeText(this, R.string.file_downloading, Toast.LENGTH_SHORT).show();
            } else {
                
                String tickerText = intent.getStringExtra(EXTRA_TICKERTEXT);
                int percent = intent.getIntExtra(EXTRA_PERCENT, 0);
                DownloadItem downloadItem = notifyOnGoing(url, tickerText, percent);
                sDownloadList.put(url, downloadItem);
                Toast.makeText(this, R.string.add_to_download_list, Toast.LENGTH_SHORT).show();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        // 当时正在下载的，此时已下载完成。
        boolean isDownloaded = intent.getBooleanExtra(EXTRA_IS_DOWNLOADED, false);
        if(isDownloaded) {
            return;
        }
        String url = intent.getStringExtra(EXTRA_URL);
        String tickerText = intent.getStringExtra(EXTRA_TICKERTEXT);
        if (sDownloadList.containsKey(url)) {
            currentDownloadItem = sDownloadList.get(url);
            File tempFile = null;
            do {
                tempFile = downFile(currentDownloadItem, url);
                if (!stop && !currentDownloadItem.stop) {
                    try {
                        Thread.sleep(RETRY_TIME);
                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            } while (tempFile == null && !stop && !currentDownloadItem.stop);
            DownloadItem downloadItem = sDownloadList.get(url);
            if (downloadItem == null || downloadItem.timeMillis == currentDownloadItem.timeMillis) {
                nm.cancel(currentDownloadItem.url.hashCode());
                sDownloadList.remove(url);
            }
            if(tempFile != null) {
                DownloadedProcessor processor = processorMap.get(url);
                if(processor != null) {
                    processor.process(this, tempFile, url, tickerText);
                }
            }
        }
    }
    
    // 在状态栏添加下载进度条
    private DownloadItem notifyOnGoing(String url, String tickerText, int percent) {
        LogWrapper.d(TAG, "notifyOnGoing:"+url);
        Context context = getApplication();
        
        int id = url.hashCode();
        
        Intent service = new Intent(context, DownloadService.class);
        service.putExtra(EXTRA_OPERATION_CODE, OPERATION_CODE_PAUSE);
        service.putExtra(EXTRA_URL, url);
        PendingIntent pendingIntent = PendingIntent.getService(context, id, service, 0);
        
        Notification notification = new Notification();
        notification.icon = R.drawable.icon;
        notification.tickerText = tickerText;
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        // 设置任务栏中下载进程显示的views
        RemoteViews remoteViews = null;
        // 4.0以上android系统才支持contentIntent=null
        if (Build.VERSION.SDK_INT < 14) {
            notification.contentIntent = pendingIntent;
            remoteViews = new RemoteViews(getPackageName(), R.layout.notification_progress_v13);
        } else {
            remoteViews = new RemoteViews(getPackageName(), R.layout.notification_progress);
            remoteViews.setOnClickPendingIntent(R.id.control_btn, pendingIntent);
            remoteViews.setTextViewText(R.id.control_btn, context.getString(R.string.pause));
        }
        remoteViews.setImageViewResource(R.id.icon_imv, R.drawable.ic_notification_downloading);
        remoteViews.setTextViewText(R.id.name_txv, tickerText);
        remoteViews.setTextViewText(R.id.process_txv, getString(R.string.downloaded_percent, percent));
        remoteViews.setProgressBar(R.id.progress_prb, 100, percent, false);
        notification.contentView = remoteViews;
        
        // 取消此下载项可能存在的通知
        nm.cancel(tickerText.hashCode());
        nm.cancel(id);
    
        // 将下载任务添加到任务栏中
        nm.notify(id, notification);
        
        return new DownloadItem(url, tickerText, remoteViews, notification, percent);
    }
    
    // 下载更新文件，成功返回File对象，否则返回null
    private File downFile(DownloadItem downloadItem, String url) {
        try {
            if (BaseQueryTest.UnallowedAccessNetwork) {
                Thread.sleep(5000);
                throw new IOException("Unallowed access network");
            }
            if (downloadItem == null) {
                return null;
            }
            File tempFile = createFileByUrl(url);
            long fileSize = tempFile.length();
            httpClient = HttpManager.getNewHttpClient();
            HttpUriRequest request = new HttpGet(url);
            
            // 支持断点续传
            // Range:(unit=first byte pos)-[last byte pos] 
            // 指定第一个字节的位置和最后一个字节的位置
            // 在http请求头加入RANGE指定第一个字节的位置
            if (fileSize > 0) {
                request.addHeader("RANGE", "bytes="+fileSize+"-");
            }
//            HttpResponse response = client.execute(new HttpGet(url));
            HttpResponse response = HttpUtils.execute(getApplicationContext(), httpClient, request, url, "downloadService");
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();   //TODO: getContentLength()某些服务器可能返回-1
            LogWrapper.d(TAG, "length:"+length);
            if(length <= fileSize) {
                return tempFile;
            }
            InputStream is = entity.getContent();
            if(is != null) {
                BufferedInputStream bis = new BufferedInputStream(is);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile, true));
                int read = 0;
                long count = fileSize;
                length += fileSize;
                int percent = 0;
                int counted_percent = 0;
                byte[] buffer = new byte[1024];
                while (!stop && !downloadItem.stop && (read = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                    count += read;
                    percent = (int)(((double)count / length) * 100);
                    if(percent - counted_percent >= 1) {
                        notifyPercent(downloadItem, percent);
                        counted_percent = percent;
                    }
                }
                bos.flush();
                bos.close();
                is.close();
                bis.close();
                if (!stop && !downloadItem.stop) {
                    return tempFile;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static File createFileByUrl(String url) throws IOException {
        File tempFile = null;
        String path = TKConfig.getSavePath() + TKConfig.DOWNLOAD_FOLDER_NAME +"/";
        File rootFile = new File(path);
        if (!rootFile.exists() && !rootFile.isDirectory())
            rootFile.mkdir();
        tempFile = new File(path, url.substring(url.lastIndexOf("/") + 1).replaceAll("[?]", "@"));
        if (tempFile.exists() == false) {
            tempFile.createNewFile();
        }
        return tempFile;
    }
    
    private void notifyPercent(DownloadItem downloadItem, int percent) {
        downloadItem.percent = percent;
        if (downloadItem.stop) {
            return;
        }
        downloadItem.remoteViews.setTextViewText(R.id.process_txv, getString(R.string.downloaded_percent, percent));
        downloadItem.remoteViews.setProgressBar(R.id.progress_prb, 100, percent, false);
        downloadItem.notification.contentView = downloadItem.remoteViews;
        nm.notify(downloadItem.url.hashCode(), downloadItem.notification);
    }
    
    private void notifyPause(String url, DownloadItem downloadItem) {
        Context context = getApplication();
        
        Intent service = new Intent(context, DownloadService.class);
        service.putExtra(EXTRA_OPERATION_CODE, OPERATION_CODE_ADD);
        service.putExtra(EXTRA_URL, url);
        service.putExtra(EXTRA_TICKERTEXT, downloadItem.tickerText);
        service.putExtra(EXTRA_PERCENT, downloadItem.percent);
        
        int id = downloadItem.tickerText.hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getService(context, id, service, 0);
        
        Notification notification = new Notification();
        notification.icon = R.drawable.icon;
        notification.tickerText = downloadItem.tickerText;
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        
        // 设置任务栏中下载进程显示的views
        RemoteViews remoteViews = null;
        // 4.0以上android系统才支持contentIntent=null
        if (Build.VERSION.SDK_INT < 14) {
            notification.contentIntent = pendingIntent;
            remoteViews = new RemoteViews(getPackageName(), R.layout.notification_progress_v13);
        } else {
            remoteViews = new RemoteViews(getPackageName(), R.layout.notification_progress);
            remoteViews.setOnClickPendingIntent(R.id.control_btn, pendingIntent);
            remoteViews.setTextViewText(R.id.control_btn, context.getString(R.string.go_on));
        }
    
        LogWrapper.d(TAG, "notifyPause:"+url);
        remoteViews.setImageViewResource(R.id.icon_imv, R.drawable.ic_notification_pause);
        remoteViews.setTextViewText(R.id.name_txv, downloadItem.tickerText);
        remoteViews.setTextViewText(R.id.process_txv, getString(R.string.paused));
        remoteViews.setProgressBar(R.id.progress_prb, 100, downloadItem.percent, false);
        notification.contentView = remoteViews;
        
        // 取消正在下载的通知
        nm.cancel(downloadItem.url.hashCode());
        nm.cancel(id);
        
        // 将下载任务添加到任务栏中
        nm.notify(id, notification);
    }
    
    @Override
    public void onDestroy() {
        stop = true;
        super.onDestroy();
    }
}

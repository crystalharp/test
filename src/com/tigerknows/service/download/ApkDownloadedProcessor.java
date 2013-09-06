/**
 * 
 */
package com.tigerknows.service.download;

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.tigerknows.R;

/**
 * @author chenming
 *
 */
public class ApkDownloadedProcessor implements DownloadedProcessor {
    
    private static ApkDownloadedProcessor sApkDownloadedProcessor = null;
    
    public static ApkDownloadedProcessor getInstance() {
        if (sApkDownloadedProcessor == null) {
            sApkDownloadedProcessor = new ApkDownloadedProcessor();
        }
        
        return sApkDownloadedProcessor;
    }

	@Override
	public void process(Context context, File file, String tickerText) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        notify(context, tickerText, intent);
	}
	
	private void notify(Context context, String tickerText, Intent intent) {
	    NotificationManager nm = (NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	    Notification notification = new Notification();
        notification.icon = R.drawable.icon;
        notification.tickerText = tickerText;
        notification.when = System.currentTimeMillis();
        notification.defaults = Notification.DEFAULT_LIGHTS;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
    
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
        notification.setLatestEventInfo(context, tickerText, "下载完成", contentIntent);
    
        // 将下载任务添加到任务栏中
        nm.notify(tickerText.hashCode(), notification);
    }
}

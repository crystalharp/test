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
import android.widget.RemoteViews;

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
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        
	    NotificationManager nm = (NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	    Notification notification = new Notification();
        notification.icon = R.drawable.icon;
        notification.tickerText = tickerText;
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        // 设置任务栏中下载进程显示的views
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_progress);
        remoteViews.setTextViewText(R.id.name_txv, tickerText);
        int id = tickerText.hashCode();
        remoteViews.setTextViewText(R.id.control_btn, context.getString(R.string.install));
        remoteViews.setTextViewText(R.id.process_txv, context.getString(R.string.download_complete_and_install));
        remoteViews.setProgressBar(R.id.process_prb, 100, 100, false);
        remoteViews.setImageViewResource(R.id.icon_imv, R.drawable.ic_order_hotel);
        notification.contentView = remoteViews;

        PendingIntent pausePendingIntent = PendingIntent.getActivity(context, id, intent, 0);
        notification.contentIntent = pausePendingIntent;

        // 将下载任务添加到任务栏中
        nm.notify(id, notification);
    }
}

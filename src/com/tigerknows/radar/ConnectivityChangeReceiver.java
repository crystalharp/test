package com.tigerknows.radar;

import java.io.File;
import java.util.Calendar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.service.PullService;
import com.tigerknows.service.download.AppService;
import com.tigerknows.util.CalendarUtil;
import com.weibo.sdk.android.util.Utility;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.RemoteViews;

/**
 * 推送服务的联网触发
 * @author xupeng
 * 联网触发的说明见PullService.java
 */

public class ConnectivityChangeReceiver extends BroadcastReceiver{

    // android 中网络变化时所发的Intent的名字
    private static final String netACTION="android.net.conn.CONNECTIVITY_CHANGE";
    
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(netACTION)) {
            // Intent中ConnectivityManager.EXTRA_NO_CONNECTIVITY这个关键字表示着当前是否连接上了网络
            // true 代表网络断开   false 代表网络没有断开
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            LogWrapper.d("NetCheckReceiver", "onReceive() noConnectivity="+noConnectivity);
            // 网络已连接
            if (noConnectivity == false) {
                // 消息推送的网络触发
                if (PullService.TRIGGER_MODE_NET.equals(PullService.getTriggerMode(context))) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    cal = Alarms.alarmAddMinutes(cal, TKConfig.PullServiceNetTriggerDelayTime);
                    Alarms.enableAlarm(context, cal, PullService.alarmAction);
                }
                // 推送app的网络触发
                if (Utility.isWifi(context)){
                    String filename = TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_FINISHED_APP, "");
                    if (!TextUtils.isEmpty(filename)){
                    	File f = new File(AppService.getAppPath() + filename);
                    	checkNotification(context, f);
                    } else {
                        AppService.checkAndDown(context);
                    }
                } else {
                    // 网络变化后如果不是wifi就停止下载
                    AppService.stopDownload(context);
                }
            } else {
                // 网络断了就马上停止下载
                AppService.stopDownload(context);
            }
        }
    }
    
    private boolean checkNotification(Context context, File file){
    	int finishTime = Integer.parseInt(TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_FINISHED_TIME));
    	String last = TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, "");
    	int t = Integer.parseInt(TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_T));
    	long now_long = CalendarUtil.getExactTime(context);
    	Calendar now = Calendar.getInstance();
    	now.setTimeInMillis(now_long);
    	int hour = now.get(Calendar.HOUR_OF_DAY);
    	if(!TextUtils.isEmpty(last) && now_long - Integer.parseInt(last) > 86400000){
    		if(file.exists()){
    			file.delete();
    		}
    		AppService.increaseTRange(context);
    		AppService.resetAppPush(context);
    		AppService.checkAndDown(context);
    		return false;
    	}
    	if(TextUtils.isEmpty(last) && now_long - finishTime < t && hour >= 9 && hour <= 20){
    		TKConfig.setPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, String.valueOf(now_long));
    		showNotification(context, file);
    		return true;
    	}
    	return false;
    }

	private void showNotification(Context context, File f){

		if(f.exists() == false){
			return;
		}
		PackageManager pm = context.getPackageManager();
		PackageInfo pI = pm.getPackageArchiveInfo(f.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
		
		if(pI == null){
			return;
		}
		
		// TODO: 添加该包名到历史通知应用列表
		
		String tickerText = pm.getApplicationLabel(pI.applicationInfo).toString();

		Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        
	    NotificationManager nm = (NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	    Notification notification = new Notification();
        notification.icon = R.drawable.ic_releasetorefresh;
        notification.tickerText = tickerText;
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        // 设置任务栏中下载进程显示的views
        RemoteViews remoteViews = null;
        // 4.0以上android系统才支持contentIntent=null
        if (Build.VERSION.SDK_INT < 14) {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_progress_v13);
        } else {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_progress);
            remoteViews.setTextViewText(R.id.control_btn, context.getString(R.string.install));
        }
        int id = "AppPush".hashCode();
        remoteViews.setTextViewText(R.id.name_txv, tickerText);
        remoteViews.setTextViewText(R.id.process_txv, context.getString(R.string.download_complete_and_install));
        Drawable icon = com.tigerknows.util.Utility.getUninstallAPKIcon(context, f.getAbsolutePath());
        remoteViews.setImageViewBitmap(R.id.icon_imv, ((BitmapDrawable)icon).getBitmap());
        notification.contentView = remoteViews;

        PendingIntent pausePendingIntent = PendingIntent.getActivity(context, id, intent, 0);
        notification.contentIntent = pausePendingIntent;
        
        // 取消此下载项可能存在的通知
        nm.cancel(id);

        // 将下载任务添加到任务栏中
        nm.notify(id, notification);
	}

}



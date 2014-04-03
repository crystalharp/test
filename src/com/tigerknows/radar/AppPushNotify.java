package com.tigerknows.radar;

import java.io.File;
import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.service.download.ShowAppReceiver;

public class AppPushNotify {
	
	public static final int DAY_SECS = 86400;
	
	private static final String DEFAULT_T_RANGE = String.valueOf(7 * DAY_SECS);
    public static int checkNotification(Context context){

    	// TODO: if...return 1;
    	
    	long tempLongTime;
    	
    	String tempStr = TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, "");
    	int lastNotify = (int)(System.currentTimeMillis()/1000 - 5 * DAY_SECS);
    	if(!TextUtils.isEmpty(tempStr)){
    		tempLongTime = Long.parseLong(tempStr);
    		lastNotify = (int)(tempLongTime / 1000);
    	}

    	int t = Integer.parseInt(TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_T, String.valueOf(DAY_SECS * 7)));
    	if(t > 14 * DAY_SECS){
    		t = 14 * DAY_SECS;
    	}

    	tempLongTime = System.currentTimeMillis();
    	Calendar now = Calendar.getInstance();
    	now.setTimeInMillis(tempLongTime);
    	int now_sec = (int)(tempLongTime / 1000);
    	
    	// TODO: get file name;
    	File file = new File("/mnt/sdcard/app/Tiger.apk");

    	int hour = now.get(Calendar.HOUR_OF_DAY);
    	if(now_sec - lastNotify < t && hour >= 9 && hour <= 20){
    		if(showNotification(context, file)){
    	        TKConfig.setPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, String.valueOf(now_sec));
    			return 0;
    		}else{
    			return 1;
    		}
    	}else{
    		return 2;
    	}
    }	
	public static boolean showNotification(Context context, File f){

		if(f.exists() == false){
			return false;
		}
		PackageManager pm = context.getPackageManager();
		PackageInfo pI = pm.getPackageArchiveInfo(f.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
		
		if(pI == null){
			return false;
		}
		
		// TODO: 添加该包名到历史通知应用列表
		// TODO: 监听点击事件
		
		// 先将T翻倍，然后待监听到点击事件之后再除以4即可
		increaseTRange(context);
		
		String tickerText = pm.getApplicationLabel(pI.applicationInfo).toString();

        Intent intent = new Intent(ShowAppReceiver.ACTION);
        intent.setData(Uri.fromFile(f));
        
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

        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, id, intent, 0);
        notification.contentIntent = pausePendingIntent;
        
        // 取消此下载项可能存在的通知
        nm.cancel(id);

        // 将下载任务添加到任务栏中
        nm.notify(id, notification);

        return true;
	}

    // T为弹出时间的范围，下次弹出通知的时间t为[DAY_SECS,T]中的随机值
    private static void increaseTRange(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T, DEFAULT_T_RANGE));
        T = Math.min(T * 2, 28 * DAY_SECS);
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T, String.valueOf(T));
    }
    
    private static void decreaseTRange(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T, DEFAULT_T_RANGE));
        T = Math.max(T / 4, 2 * DAY_SECS);
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T, String.valueOf(T));
    }	
}

package com.tigerknows.radar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.common.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.AppPush;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.provider.PackageInfoTable;
import com.tigerknows.provider.PackageInfoTable.RecordPackageInfo;
import com.tigerknows.service.download.AppService;
import com.tigerknows.service.download.ShowAppReceiver;

public class AppPushNotify {
	
	private static final String DEFAULT_T_RANGE = "7";
	
	private static ActionLog sActionLog;
	
	/**
	 * @return TestMode ? 60 : 86400
	 */
	public static int get_DAY_SECS(){
		return TKConfig.UseFastAppPush ? 60 : 86400;
	}
	
    public static int checkNotification(Context context){
    	
        if(!TKConfig.isSwitch(BootstrapModel.FIELD_APP_PUSH)){
        	return 40003;
        }
    	
    	long tempLongTime;
    	
    	sActionLog = ActionLog.getInstance(context);
    	
    	String tempStr = TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, "");
    	int lastNotify = (int)(System.currentTimeMillis()/1000);
    	if(!TextUtils.isEmpty(tempStr)){
    		lastNotify = Integer.parseInt(tempStr);
    		LogWrapper.d("Trap", "Step 0: " + String.valueOf(lastNotify));
    	}else{
    		// 首次安装软件之后，记录当前时刻；于是首次弹出通知的时刻必须距首次安装（网络触发）的T时间以上
    		TKConfig.setPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, String.valueOf(lastNotify));
    	}

    	int t = Integer.parseInt(TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_T, DEFAULT_T_RANGE)) * get_DAY_SECS();
    	if(t > 14 * get_DAY_SECS()){
    		t = 14 * get_DAY_SECS();
    	}

    	tempLongTime = System.currentTimeMillis();
    	Calendar now = Calendar.getInstance();
    	now.setTimeInMillis(tempLongTime);
    	int now_sec = (int)(tempLongTime / 1000);
    	
    	List<RecordPackageInfo> pkgList = new ArrayList<RecordPackageInfo>();
    	int n;
		try {
			PackageInfoTable piTable = PackageInfoTable.getInstance(context);
			n = piTable.readPackageInfo(pkgList, 0, 0);
			if(n == 0){
				return 1;
			}
			RecordPackageInfo pkg = pkgList.get(0);
			
	    	File f = new File(AppService.getAppPath() + pkg.file_name);
	    	LogWrapper.d("Trap", "Step 1:" + f.getAbsolutePath());
	    	
	    	if(f.exists() == false){
	    		return 1;
	    	}
	    	LogWrapper.d("Trap", "Step 2");

	    	PackageManager pm = context.getPackageManager();
	    	PackageInfo pI = pm.getPackageArchiveInfo(f.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
	    	
	    	if(pI == null){
	    		return 1;
	    	}			
	    	LogWrapper.d("Trap", "Step 3: " + (t - (now_sec - lastNotify)) );
	    	int hour = now.get(Calendar.HOUR_OF_DAY);
			if(now_sec - lastNotify > t && hour >= 9 && hour <= 20){
				// 先将T翻倍，然后待监听到点击事件之后再除以4即可
				LogWrapper.d("Trap", "Step 4:" + TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_T, DEFAULT_T_RANGE));
				increaseTRange(context);
				showNotification(context, pkg, f);
				TKConfig.setPref(context, TKConfig.PREFS_APP_PUSH_NOTIFY, String.valueOf(now_sec));
				pkg.notify_time = System.currentTimeMillis();
				piTable.updateDatabase(pkg);
				return 0;
			}else{
				return 2;
			}
		} catch (APIException e) {
			e.printStackTrace();
			return 40001;
		} catch (IOException e) {
			e.printStackTrace();
			return 40002;
		}
    }	
    
	public static void showNotification(Context context, RecordPackageInfo pkg, File f){
		
		
		AppPush app = pkg.app_push;

        Intent intent = new Intent(context, ShowAppReceiver.class);
        intent.setData(Uri.fromFile(f));
        
	    NotificationManager nm = (NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
	    Notification notification = new Notification();
        notification.icon = R.drawable.transparent_ic;
        notification.tickerText = app.getDescription();
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        // 设置任务栏中下载进程显示的views
        RemoteViews remoteViews = null;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_normal_v13);

        int id = "AppPush".hashCode();
        remoteViews.setTextViewText(R.id.name_txv, app.getName());
        remoteViews.setTextViewText(R.id.process_txv, app.getDescription());
        File img = new File(AppService.getAppPath(), app.getIconFileName());

        if(img.exists()){
        	remoteViews.setImageViewBitmap(R.id.icon_imv, BitmapFactory.decodeFile(img.getAbsolutePath()));
        	LogWrapper.d("Trap", "RemoteImg");
        }else{
        	Drawable icon = com.tigerknows.util.Utility.getUninstallAPKIcon(context, f.getAbsolutePath());
        	remoteViews.setImageViewBitmap(R.id.icon_imv, ((BitmapDrawable)icon).getBitmap());
        	LogWrapper.d("Trap", "LocalImg");
        }
        
        notification.contentView = remoteViews;

        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, id, intent, 0);
        notification.contentIntent = pausePendingIntent;
        
        // 取消此下载项可能存在的通知
        nm.cancel(id);

        // 将下载任务添加到任务栏中
        nm.notify(id, notification);
        
        sActionLog.addAction(ActionLog.AppPush + ActionLog.AppPushNotify);

        return;
	}

    // T为弹出时间的范围，下次弹出通知的时间t为[DAY_SECS,T]中的随机值
    private static void increaseTRange(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T, DEFAULT_T_RANGE));
        T = Math.min(T * 2, 28);
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T, String.valueOf(T));
    }
    
    public static void decreaseTRange(Context ctx) {
        int T = Integer.parseInt(TKConfig.getPref(ctx, TKConfig.PREFS_APP_PUSH_T, DEFAULT_T_RANGE));
        T = Math.max(T / 4, 2);
        TKConfig.setPref(ctx, TKConfig.PREFS_APP_PUSH_T, String.valueOf(T));
    }	
}

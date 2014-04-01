package com.tigerknows.radar;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.service.PullService;
import com.tigerknows.service.download.AppService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
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
                // 推送的网络触发
                if (PullService.TRIGGER_MODE_NET.equals(PullService.getTriggerMode(context))) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    cal = Alarms.alarmAddMinutes(cal, TKConfig.PullServiceNetTriggerDelayTime);
                    Alarms.enableAlarm(context, cal, PullService.alarmAction);
                }
                String filename = TKConfig.getPref(context, TKConfig.PREFS_APP_PUSH_DOWNLOAD_FINISHED, "");
                if (!TextUtils.isEmpty(filename)){
                	showNotification(context, filename);

                	}

                } else {
                    AppService.checkAndDown(context);
                    LogWrapper.d("Trap", "Step 1");
                }
            }
        }

	private void showNotification(Context context, String filename){
    	String path = AppService.getAppPath();
    	LogWrapper.d("Trap", "Step 2");
    	if(path != null) {
    		File f = new File(path + filename, "");
    		LogWrapper.d("Trap", "Step 3");
    		if(f.exists()){
    			PackageManager pm = context.getPackageManager();
    			String archiveFilePath = f.getAbsolutePath();
				PackageInfo pI = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
				LogWrapper.d("Trap", "Step 4: " + archiveFilePath);
				if(pI != null){
					NotificationManager nm = (NotificationManager)context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
					Notification notification = new Notification();
					notification.tickerText = pm.getApplicationLabel(pI.applicationInfo).toString();
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
					LogWrapper.d("Trap", "Step 5");
					Drawable icon = getUninstallAPKIcon(context, archiveFilePath);
					remoteViews.setTextViewText(R.id.name_txv, pm.getApplicationLabel(pI.applicationInfo).toString());
					remoteViews.setTextViewText(R.id.process_txv, "12354");
					remoteViews.setImageViewBitmap(R.id.icon_imv, ((BitmapDrawable)icon).getBitmap());
					remoteViews.setProgressBar(R.id.progress_prb, 100, 100, false);
					notification.contentView = remoteViews;
					LogWrapper.d("Trap", "Step 6");
					//Intent pendingIntent = new Intent(Intent.ACTION_VIEW);
					//pendingIntent.setAction(android.content.Intent.ACTION_VIEW);
					//pendingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					//pendingIntent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
					//notification.contentIntent = PendingIntent.getActivity(context, filename.hashCode(), pendingIntent, 0);
					nm.notify(filename.hashCode(), notification);
				}
    	    }
    	}
	}
    private Drawable getUninstallAPKIcon(Context context, String apkPath) {  
        String PATH_PackageParser = "android.content.pm.PackageParser";  
        String PATH_AssetManager = "android.content.res.AssetManager";  
        try {  
                // apk包的文件路径  
                // 这是一个Package 解释器, 是隐藏的  
                // 构造函数的参数只有一个, apk文件的路径  
                // PackageParser packageParser = new PackageParser(apkPath);  
                Class<?> pkgParserCls = Class.forName(PATH_PackageParser);  
                Class[] typeArgs = new Class[1];  
                typeArgs[0] = String.class;  
                Constructor<?> pkgParserCt = pkgParserCls.getConstructor(typeArgs);  
                Object[] valueArgs = new Object[1];  
                valueArgs[0] = apkPath;  
                Object pkgParser = pkgParserCt.newInstance(valueArgs);  
                Log.d("ANDROID_LAB", "pkgParser:" + pkgParser.toString());  
                // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况  
                DisplayMetrics metrics = new DisplayMetrics();  
                metrics.setToDefaults();  
                // PackageParser.Package mPkgInfo = packageParser.parsePackage(new  
                // File(apkPath), apkPath,  
                // metrics, 0);  
                typeArgs = new Class[4];  
                typeArgs[0] = File.class;  
                typeArgs[1] = String.class;  
                typeArgs[2] = DisplayMetrics.class;  
                typeArgs[3] = Integer.TYPE;  
                Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage", typeArgs);  
                valueArgs = new Object[4];  
                valueArgs[0] = new File(apkPath);  
                valueArgs[1] = apkPath;  
                valueArgs[2] = metrics;  
                valueArgs[3] = 0;  
                Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);  
                // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开  
                // ApplicationInfo info = mPkgInfo.applicationInfo;  
                Field appInfoFld = pkgParserPkg.getClass().getDeclaredField("applicationInfo");  
                ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);  
                // uid 输出为"-1"，原因是未安装，系统未分配其Uid。  
                Log.d("ANDROID_LAB", "pkg:" + info.packageName + " uid=" + info.uid);  
                // Resources pRes = getResources();  
                // AssetManager assmgr = new AssetManager();  
                // assmgr.addAssetPath(apkPath);  
                // Resources res = new Resources(assmgr, pRes.getDisplayMetrics(),  
                // pRes.getConfiguration());  
                Class<?> assetMagCls = Class.forName(PATH_AssetManager);  
                Constructor<?> assetMagCt = assetMagCls.getConstructor((Class[]) null);  
                Object assetMag = assetMagCt.newInstance((Object[]) null);  
                typeArgs = new Class[1];  
                typeArgs[0] = String.class;  
                Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod("addAssetPath", typeArgs);  
                valueArgs = new Object[1];  
                valueArgs[0] = apkPath;  
                assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);  
                Resources res = context.getResources();  
                typeArgs = new Class[3];  
                typeArgs[0] = assetMag.getClass();  
                typeArgs[1] = res.getDisplayMetrics().getClass();  
                typeArgs[2] = res.getConfiguration().getClass();  
                Constructor<Resources> resCt = Resources.class.getConstructor(typeArgs);  
                valueArgs = new Object[3];  
                valueArgs[0] = assetMag;  
                valueArgs[1] = res.getDisplayMetrics();  
                valueArgs[2] = res.getConfiguration();  
                res = (Resources) resCt.newInstance(valueArgs);  
                CharSequence label = null;  
                if (info.labelRes != 0) {  
                        label = res.getText(info.labelRes);  
                }  
                // if (label == null) {  
                // label = (info.nonLocalizedLabel != null) ? info.nonLocalizedLabel  
                // : info.packageName;  
                // }  
                Log.d("ANDROID_LAB", "label=" + label);
                return res.getDrawable(info.icon);
                // 这里就是读取一个apk程序的图标  

        } catch (Exception e) {  
                e.printStackTrace();  
                return null;
        }  
}  
}



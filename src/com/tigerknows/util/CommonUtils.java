/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.view.SpringbackListView;
import com.tigerknows.view.StringArrayAdapter;
import com.tigerknows.view.TrafficQueryFragment;

/**
 * 
 * @author Peng Wenyue
 * @version 1.0
 * @since 1.0
 */
public class CommonUtils {
    
    private static final String TAG = "CommonUtils";
    
    public static final String STRING_EMPTY = "";
    
    public static void SendSms(Activity activity, String title, String body) {
        Intent send = new Intent();
        send.setAction(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TITLE, title);
        send.putExtra(Intent.EXTRA_TEXT, body);
        try {
            activity.startActivity(Intent.createChooser(send, title));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, R.string.no_way_to_share_message, Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void SendSms(Activity activity, String title, String body, String smsto) {

        Intent send = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"+smsto));
        send.addCategory(Intent.CATEGORY_DEFAULT);
        send.addCategory(Intent.CATEGORY_BROWSABLE);
        send.putExtra("sms_body", body);
        try {
            activity.startActivity(Intent.createChooser(send, title));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, R.string.no_way_to_share_message, Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void share(Activity activity, String title, String body, Uri uri) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, title);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra("sms_body", body);
        intent.putExtra("file_name", uri.toString());
        try {
            activity.startActivity(Intent.createChooser(intent, title));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, R.string.no_way_to_share_message, Toast.LENGTH_SHORT).show();
        }
    }
    
    public static double meter2kilometre(int total) {
        double temp = total/1000.0;
        BigDecimal b = new BigDecimal(temp);
        return b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public static String formatMeterString(int total) {
        if (total < 100) {
            return total+"m";
        } else if (total < 1000) {
            return (total - (total%10))+"m";
        } else {
            double temp = total/1000.0;
            BigDecimal b = new BigDecimal(temp);
            return b.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue() + "km";
        }
    }

    public static String formatTimeStampString(Context context, long when) {
        return formatTimeStampString(context, when, false);
    }

    public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;

        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }

        return DateUtils.formatDateTime(context, when, format_flags);
    }
    
    public static void deleteAllFile(String filePath) {

        LinkedList<File> list = new LinkedList<File>();
        LinkedList<File> fileList = new LinkedList<File>();
        LinkedList<File> dirList = new LinkedList<File>();

        File dir = new File(filePath);
        File file[] = dir.listFiles();
                
        if(file != null){
            
            for (int i = 0; i < file.length; i++) {
                if (file[i].isDirectory()) {
                    list.add(file[i]);
                    dirList.addFirst(file[i]);
                } else {
                    fileList.add(file[i]);
                }
            }
        }
        
        File tmp;

        while (!list.isEmpty()) {
            tmp = list.removeFirst();
            if (tmp.isDirectory()) {
                file = tmp.listFiles();
                if (file == null)
                    continue;

                for (int i = 0; i < file.length; i++) {
                    if (file[i].isDirectory()) {
                        list.add(file[i]);
                        dirList.addFirst(file[i]);
                    } else {
                        fileList.add(file[i]);
                    }
                }
            }
        }

        while (!fileList.isEmpty()) {
            tmp = fileList.removeFirst();
            tmp.delete();
        }

        while (!dirList.isEmpty()) {
            tmp = dirList.removeFirst();
            tmp.delete();
        }
    }
    
    /**
     * basic check of network connectivity
     * 
     * @param context
     *            reference to the calling Activity
     * @return is available.
     */
    public static boolean checkNetworkStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo[] allNetworkInfo = connectivityManager.getAllNetworkInfo();
        if (allNetworkInfo != null) {
            for(android.net.NetworkInfo networkInfo : allNetworkInfo) {
                if (networkInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        }
        return false;
        
//        boolean isAvailable = false;
//        if (connectivityManager.getActiveNetworkInfo() != null) {
//            isAvailable = connectivityManager.getActiveNetworkInfo().isAvailable();
//        }
//        
//        return isAvailable;
        
//        boolean result = false;
//        NetworkInfo netinfo = connectivityManager.getActiveNetworkInfo();
//        if (netinfo != null && netinfo.isConnected()) {
//         result=true;
//         Log.i(TAG, "The net was connected" );
//        }else{
//         result=false;
//         Log.i(TAG, "The net was bad!");
//        }
//        return result;
    }
    
    public static boolean checkMobileNetwork(Context ctx) {
        boolean resp = false;
        final ConnectivityManager connMgr = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null) {
            resp = (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE); 
        }
        return resp;
    }
    
    public static boolean checkGpsStatus(Context context) {
        boolean enableGps = false;
        String locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!TextUtils.isEmpty(locationProviders)) {
            enableGps = locationProviders.contains(LocationManager.GPS_PROVIDER);
        }
        return enableGps;
    }
    
    /**
     * 
     * @param distance
     * @return
     */
    public static String distanceToString(int distance) {
        String str;
        if (distance >= 1000) {
            if (distance % 1000 > 0) {
                distance -= (distance%100);
                str = ((float)distance / 1000 ) + "km";
            } else {
                str = (distance / 1000 ) + "km";
            }
        } else {
            str = distance + "m";
        }
        return str;
    }

    public static void unZipFile(AssetManager am, String fileName, String path) {

        try {
            File file = new File(path + fileName);
            if (file.exists()) {
                file.delete();
            }
            
            saveFile(am.open(fileName), fileName, path);
            
            ZipFile zipFile = new ZipFile(path + fileName);
            
            Enumeration<? extends ZipEntry> entries = zipFile.entries(); 
            while (entries.hasMoreElements()) 
            {
                 ZipEntry entry = (ZipEntry)entries.nextElement(); 
                 if (entry.isDirectory()) {
                     String filePath = entry.getName();
                     File folder = new File(path+"/"+filePath.substring(0, filePath.lastIndexOf("/")));
                     if (!folder.exists()) {
                         if (!folder.mkdirs()) {
                             LogWrapper.e(TAG, "Unable to create new folder: " + filePath);
                         }
                     }
                 } else {
                     saveFile(zipFile.getInputStream(entry), entry.getName(), path);
                 }
            }
            if (file.exists()) {
                file.delete();
            }
            zipFile.close();
        } catch (IOException e) {
            LogWrapper.e(TAG, "IOException caught while unZipFile");
        }
    }

    public static void saveFile(InputStream is, String filename, String path) throws IOException {

        StringBuilder filePath = new StringBuilder();
        filePath.append(path);
        
        if (filename.lastIndexOf("/") != -1) {
            filePath.append(filename.substring(0, filename.lastIndexOf("/")));
            // �����ļ���
            File folder = new File(filePath.toString());
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    LogWrapper.e(TAG, "Unable to create new folder: " + filePath.toString());
                }
            }
            filename = filename.substring(filename.lastIndexOf("/"));
        }
        filePath.append(filename);
        
        // �����ļ�
        File dataFile = new File(filePath.toString());
        FileOutputStream fout = null;
        try {
            if (dataFile.exists()) {
                if (dataFile.delete()) {
                    if (!dataFile.createNewFile()) {
                        LogWrapper.e(TAG, "Unable to create new file: " + filePath);
                        return;
                    }
                } else {
                    return;
                }
            } 
            fout = new FileOutputStream(dataFile);

            byte buf[]=new byte[1024];
            int len;
            while((len=is.read(buf))>0)
                fout.write(buf,0,len);
            fout.flush();
            fout.close();
            is.close();
        } catch (IOException e) {
            LogWrapper.e(TAG, "Unable to handle file: " + filePath);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                }
            }
        }
    }
    
    public static Bitmap viewToBitmap(View v) {
        boolean isDrawingCacheEnabled = v.isDrawingCacheEnabled();
        v.setDrawingCacheEnabled(true);
        if (!v.isDrawingCacheEnabled()) {
            return null;
        }

        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);
        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation

        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);
        if (color != 0) {
            v.destroyDrawingCache();
        }

        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();

        if (cacheBitmap == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        v.setDrawingCacheEnabled(isDrawingCacheEnabled);
        return bitmap;
    }
    
    public static Uri bitmap2Png(Bitmap bitmap, String fileName, String path) {

        Uri uri = null;
        if (bitmap != null) {
            StringBuilder filePath = new StringBuilder();
            filePath.append(path);
            filePath.append(fileName);

            // �����ļ�
            File file = new File(filePath.toString());
            FileOutputStream fout = null;
            try {
                if (file.exists()) {
                    if (file.delete()) {
                        if (!file.createNewFile()) {
                            Log.e(TAG, "Unable to create new file: " + filePath);
                        }
                    }
                }
                fout = new FileOutputStream(file, true);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fout);
                uri = Uri.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != fout) {
                    try {
                        fout.close();
                    } catch (IOException e) {
                        // Ignore
                        Log.e(TAG, "IOException caught while closing stream", e);
                    }
                }
            }
        }

        return uri;
    }
    
    /**
     * 查询手机内短信应用
     * @param context
     * @return
     */
    public static ResolveInfo getSmsApp(Context context){
        List<ResolveInfo> list = new ArrayList<ResolveInfo>();  
        Intent intent=new Intent(Intent.ACTION_SEND,null);  
        intent.addCategory(Intent.CATEGORY_DEFAULT);  
        intent.setType("text/plain");  
        PackageManager packageManager = context.getPackageManager();
        list = packageManager.queryIntentActivities(intent,PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);  
        for(ResolveInfo r : list) {
            if (r.activityInfo.name.contains("ComposeMessage")) {
                return r;
            }
        }
        return null;  
    }
    
    /**
     * 查询手机内非系统应用
     * @param context
     * @return
     */
    public static List<PackageInfo> getSystemApps(Context context) {
        List<PackageInfo> list = new ArrayList<PackageInfo>();
        PackageManager packageManager = context.getPackageManager();
        //获取手机内所有应用
        List<PackageInfo> paklist = packageManager.getInstalledPackages(0);
        for (int i = 0; i < paklist.size(); i++) {
            PackageInfo pak = (PackageInfo) paklist.get(i);
            //判断是否为非系统预装的应用程序
            if ((pak.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                // customs applications
                list.add(pak);
            }
        }
        return list;
    }
    
    public static boolean isEmpty(List<Object> list) {
        if (list == null) {
            return true;
        }
        
        return list.isEmpty();
    }

    public static int asu2dbm(int asu) {
        return (-113 + 2 * asu);
    }
    
    public static double doubleKeep(double value, int keep) {
        BigDecimal b = new BigDecimal(value);
        return b.setScale(keep, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    /**
     * 中国国内运营商MCC为460，MNC中国移动为00 02 07，中国联通是01  电信 03 05
     * @param mcc
     * @param mnc
     * @param lac
     * @param cid
     * @return
     */
    public static boolean mccMncLacCidValid(int mcc, int mnc, int lac, int cid) {
        if (mcc >= 0 && mcc <= 999 && mnc >= 0 && mnc <= 99 && lacCidValid(lac, cid)) {
            return true;
        }
        return false;
    }
    
    public static boolean lacCidValid(int lac, int cid) {
        if (lac != NeighboringCellInfo.UNKNOWN_CID && cid != NeighboringCellInfo.UNKNOWN_CID) {
            return true;
        }
        return false;
    }
    
    public static boolean writeFile(String path, byte[] data, boolean createNewFile) {

        // 创建文件
        File file = new File(path);
        FileOutputStream fout = null;
        try {
            if (createNewFile) {
                if (file.exists()) {
                    if (!file.delete()) {
                        LogWrapper.e("Utils", "writeFile() Unable to delete the file: " + path);
                        return false;
                    }
                }
                
                if (!file.createNewFile()) {
                    LogWrapper.e("Utils", "writeFile() Unable to create new file: " + path);
                    return false;
                }
            }
            
            fout = new FileOutputStream(file);
            fout.write(data);
            fout.flush();
            
            return true;
            
        } catch (IOException e) {
            LogWrapper.e("Utils", "writeFile() throw IOException, the path: " + path);
        } finally {
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    LogWrapper.e("Utils", "writeFile() IOException caught while closing stream");
                }
            }
        }
        
        return false;
    }
    
    public static Dialog getDialog(Activity activity, View custom) {
        return getDialog(activity, null, null, custom, null, null, null);
    }
    
    public static Dialog showNormalDialog(Activity activity, View custom) {
        return showNormalDialog(activity, null, null, custom, null, null, null);
    }
    
    public static Dialog showNormalDialog(Activity activity, String title, View custom) {
        return showNormalDialog(activity, title, null, custom, activity.getString(R.string.confirm), activity.getString(R.string.cancel), null);
    }
    
    public static Dialog showNormalDialog(Activity activity, String title, View custom, DialogInterface.OnClickListener onClickListener) {
        return showNormalDialog(activity, title, null, custom, activity.getString(R.string.confirm), activity.getString(R.string.cancel), onClickListener);
    }

    public static Dialog showNormalDialog(Activity activity, String message) {
        return showNormalDialog(activity, activity.getString(R.string.prompt), message, activity.getString(R.string.confirm), null, null);
    }

    public static Dialog showNormalDialog(Activity activity, String message, DialogInterface.OnClickListener onClickListener) {
        return showNormalDialog(activity, activity.getString(R.string.prompt), message, activity.getString(R.string.confirm), activity.getString(R.string.cancel), onClickListener);
    }

    public static Dialog showNormalDialog(Activity activity, String title, String message, DialogInterface.OnClickListener onClickListener) {
        return showNormalDialog(activity, title, message, activity.getString(R.string.confirm), activity.getString(R.string.cancel), onClickListener);
    }

    public static Dialog showNormalDialog(Activity activity, String title, String message, String leftButtonText, String rightButtonText, DialogInterface.OnClickListener onClickListener) {
        return showNormalDialog(activity, title, message, null, leftButtonText, rightButtonText, onClickListener);
    }

    public static Dialog showNormalDialog(Activity activity, String title, String message, View custom, String leftButtonText, String rightButtonText, final DialogInterface.OnClickListener onClickListener) {
        
        Dialog dialog = getDialog(activity, title, message, custom, leftButtonText, rightButtonText, onClickListener);
        dialog.show();
        
        return dialog;
    }

    public static Dialog getDialog(Activity activity, String title, String message, View custom, String leftButtonText, String rightButtonText, final DialogInterface.OnClickListener onClickListener) {
        
        LayoutInflater layoutInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        final Dialog dialog = new Dialog(activity, R.style.AlertDialog);
        
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view  = layoutInflater.inflate(R.layout.alert_dialog, null);
        dialog.setContentView(view);
        
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        
        View titleView = view.findViewById(R.id.titlePanel);
        View contentPanel = view.findViewById(R.id.contentPanel);
        ViewGroup customPanel = (ViewGroup) view.findViewById(R.id.customPanel);
        View bottonView = view.findViewById(R.id.buttonPanel);
        
        if (title != null) {
            titleView.setVisibility(View.VISIBLE);
            ((TextView) titleView.findViewById(R.id.title_txv)).setText(title);
        } else {
            titleView.setVisibility(View.GONE);
        }
        
        if (message != null) {
            contentPanel.setVisibility(View.VISIBLE);
            ((TextView) contentPanel.findViewById(R.id.message)).setText(message);
        } else {
            contentPanel.setVisibility(View.GONE);
        }
        
        if (custom != null) {
            customPanel.setVisibility(View.VISIBLE);
            customPanel.addView(custom);
        } else {
            customPanel.setVisibility(View.GONE);
        }
        
        if (leftButtonText != null || rightButtonText != null) {
            bottonView.setVisibility(View.VISIBLE);
            View split = bottonView.findViewById(R.id.split_imv);
            Button leftBtn = (Button) bottonView.findViewById(R.id.button1);
            Button rightBtn = (Button) bottonView.findViewById(R.id.button2);
            View.OnClickListener listener = new View.OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    if (onClickListener != null)
                        onClickListener.onClick(dialog, view.getId() == R.id.button1 ? DialogInterface.BUTTON_POSITIVE : DialogInterface.BUTTON_NEGATIVE);
                }
            };
            if (leftButtonText == null) {
                split.setVisibility(View.GONE);
                leftBtn.setVisibility(View.GONE);
                rightBtn.setVisibility(View.VISIBLE);
                rightBtn.setText(rightButtonText);
                rightBtn.setOnClickListener(listener);
            } else if (rightButtonText == null) {
                split.setVisibility(View.GONE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.GONE);
                leftBtn.setText(leftButtonText);
                leftBtn.setOnClickListener(listener);
            } else {
                split.setVisibility(View.VISIBLE);
                leftBtn.setVisibility(View.VISIBLE);
                rightBtn.setVisibility(View.VISIBLE);
                leftBtn.setText(leftButtonText);
                leftBtn.setOnClickListener(listener);
                rightBtn.setText(rightButtonText);
                rightBtn.setOnClickListener(listener);
            }
        } else {
            bottonView.setVisibility(View.GONE);
        }
        
        return dialog;
    }
    
    public static ListView makeListView(Context context) {
        ListView listView = new ListView(context);
        listView.setFadingEdgeLength(0);
        listView.setScrollingCacheEnabled(false);
        listView.setFooterDividersEnabled(false);
        listView.setDivider(context.getResources().getDrawable(R.drawable.bg_real_line));
        return listView;
    }
    
    public static byte[] getDrawableResource(Context context, int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
        ByteArrayOutputStream outStream =new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        bitmap.recycle();
        return outStream.toByteArray();
    }
    
    /**   
    * 读取文件内容   
    * @param filename  文件名称   
    * @return  文件内容   
    * @throws Exception   
    */   
   public static String readFile(FileInputStream inStream) throws Exception{  
       return new String(readFileToByte(inStream));   
   }  
   
   public static byte[] readFileToByte(FileInputStream inStream) throws Exception{   
       //创建一个往内存输出流对象   
       ByteArrayOutputStream outStream =new ByteArrayOutputStream();   
       byte[] buffer =new byte[1024];   
       int len =0;   
       while((len=inStream.read(buffer))!=-1){   
           //把每次读到的数据写到内存中   
           outStream.write(buffer,0,len);   
       }   
       //得到存放在内存中的所有的数据    
       return outStream.toByteArray();   
   }  

   public static CharSequence renderColorToPartOfString(Context context, int colorResId, String str, String subStr) {
	   if(TextUtils.isEmpty(str) || TextUtils.isEmpty(subStr))
           return str;

	   int start = -1, end = -1;
	   
       start = str.indexOf(subStr);
       if(start != -1)
    	   end = start + subStr.length();
       if (start == -1 || end == -1) 
    	   return str;

       SpannableStringBuilder style=new SpannableStringBuilder(str);
       style.setSpan(new ForegroundColorSpan(context.getResources().getColor(colorResId)), start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

       return style;
   }
   
    
    @SuppressWarnings("unchecked")
    public static void keepListSize(List list, int max) {
        if (list == null || max <= 0) {
            return;
        }
        int size = list.size();
        for(int index = size-1; index >= max; index--) {
            list.remove(index);
        }
    }
    
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */ 
    public static int dip2px(Context context, float dpValue) { 
        final float scale = context.getResources().getDisplayMetrics().density; 
        return (int) (dpValue * scale + 0.5f); 
    } 

    /**  
     * @param date1 需要比较的时间 不能为空(null),需要正确的日期格式  
     * @param date2 被比较的时间  为空(null)则为当前时间  
     * @param stype 返回值类型   0为多少天，1为多少个月，2为多少年  
     * @return  
     */ 
    public static int compareDate(Calendar c1,Calendar c2,int stype){  
        int n = 0;  
          
        //List list = new ArrayList();  
        while (!c1.after(c2)) {                     // 循环对比，直到相等，n 就是所要的结果  
            //list.add(df.format(c1.getTime()));    // 这里可以把间隔的日期存到数组中 打印出来  
            n++;  
            if(stype==1){  
                c1.add(Calendar.MONTH, 1);          // 比较月份，月份+1  
            }  
            else{  
                c1.add(Calendar.DATE, 1);           // 比较天数，日期+1  
            }  
        }  
          
        n = n-1;  
          
        if(stype==2){  
            n = (int)n/365;  
        }     
          
        return n;  
    } 
    
    /**
     * 将字符以SHA-1加密, 并将加密结果转化成16进制字符串
     * @param password
     * @return
     */
    public static String encryptWithSHA1(String password) {
        String sha1 = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sha1;
    }

    /**
     * 将字节数组转化成16进制字符串
     * @param hash
     * @return
     */
    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    
    public static int[] makePage(SpringbackListView listView, int size, int firstVisiblePosition, int lastVisiblePosition) {
        return makePage(listView, size, firstVisiblePosition, lastVisiblePosition, false);
    }
    
    public static int[] makePage(SpringbackListView listView, int size, int firstVisiblePosition, int lastVisiblePosition, boolean isShowAPOI) {
        if (listView.isFooterSpringback() && lastVisiblePosition > size-1) {
            lastVisiblePosition -= 1;
        }
        if (isShowAPOI) {
            firstVisiblePosition += 1;
        }
        int pageSize = TKConfig.getPageSize()/2;
        int startPage = firstVisiblePosition/(pageSize);
        int endPage = lastVisiblePosition/(pageSize)+1;
        int diff = endPage - startPage; 
        if (diff < 2) {
            if (lastVisiblePosition >= size-1) {
                startPage-=(2-diff);
            } else if (lastVisiblePosition < size-1) {
                endPage+=(2-diff);
            }
        }
        
        if (startPage < 0) {
            startPage = 0;
        }
        
        int minIndex = startPage * pageSize + (isShowAPOI ? 1 : 0);
        int maxIndex = endPage * pageSize + (isShowAPOI ? 1 : 0);
        return new int[]{minIndex, maxIndex, (firstVisiblePosition-(isShowAPOI ? 1 : 0)+(startPage%2 != 0 ? pageSize : 0)) % TKConfig.getPageSize()};
    }
    
    public static void pageIndicatorInit(Context context, ViewGroup viewPoints, int size) {
        int margin = (int)(Globals.g_metrics.density * 2);
        for(int i=0;i<size;i++){
            ImageView imageView = new ImageView(context);            
            //默认选中的是第一张图片，此时第一个小圆点是选中状态，其他不是
            if(i==0){
                imageView.setBackgroundResource(R.drawable.ic_learn_dot_selected);
            }else{
                imageView.setBackgroundResource(R.drawable.ic_learn_dot_normal);
            }
            
            //将imageviews添加到小圆点视图组
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            viewPoints.addView(imageView, layoutParams);
        }

    }
    
    public static void pageIndicatorChanged(Context context, ViewGroup viewPoints, int index) {
        for(int i=0, size = viewPoints.getChildCount();i<size;i++){
            ImageView imageView = (ImageView) viewPoints.getChildAt(i);
            
            //默认选中的是第一张图片，此时第一个小圆点是选中状态，其他不是
            if(i==index){
                imageView.setBackgroundResource(R.drawable.ic_learn_dot_selected);
            }else{
                imageView.setBackgroundResource(R.drawable.ic_learn_dot_normal);
            }
        }

    }
    
    public static void telephone(final Activity activity, TextView textView) {
        String split = activity.getString(R.string.dunhao);
        final String[] list = textView.getText().toString().split(split);
        if (list.length == 1) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+list[0]));
            activity.startActivity(intent);
        } else {
            final ArrayAdapter<String> adapter = new StringArrayAdapter(activity, list, null);
            ListView listView = CommonUtils.makeListView(activity);
            listView.setAdapter(adapter);
            
            final Dialog dialog = showNormalDialog(activity,
                    activity.getString(R.string.app_name),
                    null,
                    listView,
                    null,
                    null,
                    null);
            
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+list[which]));
                    activity.startActivity(intent);
                    dialog.dismiss();
                }
            });
        }
    }

    public static void queryTraffic(final Sphinx sphinx, final POI poi) {
        queryTraffic(sphinx, poi, TrafficQueryFragment.END);
    }
    
    public static void queryTraffic(final Sphinx sphinx, final POI poi, final int location) {
        
        String[] list = sphinx.getResources().getStringArray(R.array.goto_here);
        int[] leftCompoundResIdList = new int[] {R.drawable.ic_bus, R.drawable.ic_drive, R.drawable.ic_walk, R.drawable.ic_start};
        final ArrayAdapter<String> adapter = new StringArrayAdapter(sphinx, list, leftCompoundResIdList);
        
        ListView listView = CommonUtils.makeListView(sphinx);
        listView.setAdapter(adapter);
        
        final Dialog dialog = CommonUtils.showNormalDialog(sphinx, 
                sphinx.getString(R.string.come_here), 
                null,
                listView,
                null,
                null,
                null);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                dialog.dismiss();
                int queryType = -1;
                switch (index) {
                    case 0:
                        queryType = TrafficQuery.QUERY_TYPE_TRANSFER;
                        break;
                        
                    case 1:
                        queryType = TrafficQuery.QUERY_TYPE_DRIVE;
                        break;
                        
                    case 2:
                        queryType = TrafficQuery.QUERY_TYPE_WALK;
                        break;
                        
                    case 3:
                        break;
                }
                
                POI myLocationPOI = sphinx.getPOI(true);
                TrafficQueryFragment trafficQueryFragment = sphinx.getTrafficQueryFragment();
                if (queryType != -1
                        && myLocationPOI.getSourceType() == POI.SOURCE_TYPE_MY_LOCATION) {
                    POI start;
                    POI end;
                    if (location == TrafficQueryFragment.START) {
                        start = poi;
                        end = myLocationPOI;
                    } else {
                        start = myLocationPOI;
                        end = poi;
                    }
                    TrafficQueryFragment.submitTrafficQuery(sphinx, start, end, queryType);
                } else {
                    trafficQueryFragment.setData(poi, location);
                    sphinx.showView(R.id.view_traffic_query);
                }
            }
        });
    }
        
    public static boolean isConnectionFast(Context ctx){  
        ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        int type = 0;
        int subType = 0;
        if (networkInfo != null) {
            type = networkInfo.getType();
            subType = networkInfo.getSubtype();
        }
        if(type==ConnectivityManager.TYPE_WIFI){  
            return true;  
        }else if(type==ConnectivityManager.TYPE_MOBILE){  
            switch(subType){  
            case TelephonyManager.NETWORK_TYPE_1xRTT:  
                return false; // ~ 50-100 kbps  
            case TelephonyManager.NETWORK_TYPE_CDMA:  
                return false; // ~ 14-64 kbps  
            case TelephonyManager.NETWORK_TYPE_EDGE:  
                return false; // ~ 50-100 kbps  
            case TelephonyManager.NETWORK_TYPE_EVDO_0:  
                return true; // ~ 400-1000 kbps  
            case TelephonyManager.NETWORK_TYPE_EVDO_A:  
                return true; // ~ 600-1400 kbps  
            case TelephonyManager.NETWORK_TYPE_GPRS:  
                return false; // ~ 100 kbps  
            case TelephonyManager.NETWORK_TYPE_HSDPA:  
                return true; // ~ 2-14 Mbps  
            case TelephonyManager.NETWORK_TYPE_HSPA:  
                return true; // ~ 700-1700 kbps  
            case TelephonyManager.NETWORK_TYPE_HSUPA:  
                return true; // ~ 1-23 Mbps  
            case TelephonyManager.NETWORK_TYPE_UMTS:  
                return true; // ~ 400-7000 kbps  
            // NOT AVAILABLE YET IN API LEVEL 7  
//            case Connectivity.NETWORK_TYPE_EHRPD:  
//                return true; // ~ 1-2 Mbps  
//            case Connectivity.NETWORK_TYPE_EVDO_B:  
//                return true; // ~ 5 Mbps  
//            case Connectivity.NETWORK_TYPE_HSPAP:  
//                return true; // ~ 10-20 Mbps  
//            case Connectivity.NETWORK_TYPE_IDEN:  
//                return false; // ~25 kbps   
//            case Connectivity.NETWORK_TYPE_LTE:  
//                return true; // ~ 10+ Mbps  
            // Unknown  
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:  
                return false;   
            default:  
                return false;  
            }  
        }else{  
            return false;  
        }  
    }  
    
    public static void formatText(TextView textView, String word, String key, int color) {
        if (key != null && word != null && key.length() > 0 && word.length() > 0 && word.indexOf(key)!=-1){
            int keyLength = key.length();
            int wordLength = word.length();
            SpannableStringBuilder style=new SpannableStringBuilder(word);  
            style.setSpan(new ForegroundColorSpan(color),word.indexOf(key), keyLength > wordLength ? wordLength : keyLength,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(style);
        } else {
            textView.setText(word);
        }
    }
}

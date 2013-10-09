/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import android.widget.Toast;

import com.tigerknows.android.app.TKActivity;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.ErrorDialogActivity;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.StringArrayAdapter;

/**
 * 
 * @author Peng Wenyue
 * @version 1.0
 * @since 1.0
 */
public class Utility {
    
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
        if (title != null) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
            intent.putExtra("sms_body", body);
        }
        if (uri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra("file_name", uri.toString());
        }
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
        unZipFile(am, fileName, path, null);
    }

    public static void unZipFile(AssetManager am, String fileName, String path, String specifyFileName) {

        try {
            File file = new File(path + fileName);
            if (file.exists()) {
                file.delete();
            }
            
            saveFile(am.open(fileName), fileName, path);
            
            unZipFile(path + fileName, specifyFileName, path);
            
            if (file.exists()) {
                file.delete();
            }
        } catch (IOException e) {
            LogWrapper.e(TAG, "IOException caught while unZipFile");
        }
    }
    
    public static void unZipFile(String file, String specifyFileName, String path) {

        try {
            ZipFile zipFile = new ZipFile(file);
            
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
                 } else if (specifyFileName == null) {
                     saveFile(zipFile.getInputStream(entry), entry.getName(), path);
                 } else if (entry.getName().endsWith(specifyFileName)) {
                     saveFile(zipFile.getInputStream(entry), entry.getName(), path);
                     break;
                 }
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
    
    public static String formatHotelPrice(double value){
    	String str = Double.toString(doubleKeep(value, 2));
    	String[] a = str.split("\\.");
    	if(a.length > 1 && Pattern.compile("^0+$").matcher(a[1]).matches()){
    		return a[0];
    	}else return str;
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
            
            fout = new FileOutputStream(file, true);
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

    public static void removeLineFromFile(String file, String[] lineToRemove) {

        try {

            File inFile = new File(file);

            if (!inFile.isFile()) {
                LogWrapper.d(TAG, "removeLineFromFile() Parameter is not an existing file");
                return;
            }

            // Construct the new file that will later be renamed to the original
            // filename.
            File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

            BufferedReader br = new BufferedReader(new FileReader(file));
            PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;

            // Read from the original file and write to the new
            // unless content matches data to be removed.
            while ((line = br.readLine()) != null) {

                String lineTrim = line.trim();
                boolean exist = false;
                for(int i = 0, l = lineToRemove.length; i < l; i++) {
                    if (lineTrim.equals(lineToRemove[i])) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {

                    pw.println(line);
                    pw.flush();
                }
            }
            pw.close();
            br.close();

            // Delete the original file
            if (!inFile.delete()) {
                LogWrapper.d(TAG, "removeLineFromFile() Could not delete file");
                return;
            }

            // Rename the new file to the filename the original file had.
            if (!tempFile.renameTo(inFile)) {
                LogWrapper.d(TAG, "removeLineFromFile() Could not rename file");
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static Dialog getDialog(Activity activity, View custom) {
        return getDialog(activity, null, null, custom, null, null, null);
    }
    
    public static Dialog showNormalDialog(Activity activity, View custom) {
        return showNormalDialog(activity, null, null, custom, null, null, null);
    }
    
    public static Dialog showNormalDialog(Activity activity, String title, View custom) {
        return showNormalDialog(activity, title, null, custom, null, null, null);
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
        return showNormalDialog(activity, title, message, custom, leftButtonText, rightButtonText, onClickListener, true);
    }
    
    public static Dialog showNormalDialog(Activity activity, String title, String message, View custom, String leftButtonText, String rightButtonText, final DialogInterface.OnClickListener onClickListener, boolean dismiss) {
        if (title != null || message != null) {
            ActionLog.getInstance(activity).addAction(ActionLog.Dialog, title, message);
        }
        Dialog dialog = getDialog(activity, title, message, custom, leftButtonText, rightButtonText, onClickListener, dismiss);
        dialog.show();
        
        return dialog;
    }
    
    public static Dialog getDialog(final Activity activity, String title, String message, View custom, String leftButtonText, String rightButtonText, final DialogInterface.OnClickListener onClickListener) {
        return getDialog(activity, title, message, custom, leftButtonText, rightButtonText, onClickListener, true);
    }

    public static Dialog getDialog(final Activity activity, String title, String message, View custom, String leftButtonText, String rightButtonText, final DialogInterface.OnClickListener onClickListener, final boolean dismiss) {
        
        LayoutInflater layoutInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        final Dialog dialog = new Dialog(activity, R.style.AlertDialog);
        
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view  = layoutInflater.inflate(R.layout.alert_dialog, null);
        dialog.setContentView(view);
        
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        
        View titleView = view.findViewById(R.id.titlePanel);
        View contentPanel = view.findViewById(R.id.contentPanel);
        ViewGroup customPanel = (ViewGroup) view.findViewById(R.id.customPanel);
        View buttonView = view.findViewById(R.id.buttonPanel);
        
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
            buttonView.setVisibility(View.VISIBLE);
            final Button leftBtn = (Button) buttonView.findViewById(R.id.button1);
            final Button rightBtn = (Button) buttonView.findViewById(R.id.button2);
            View leftView = buttonView.findViewById(R.id.button1_view);
            View rightView = buttonView.findViewById(R.id.button2_view);
            View.OnClickListener listener = new View.OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    if (dismiss) {
                        dialog.dismiss();
                    }
                    if (onClickListener != null) {
                        if (view.getId() == R.id.button1) {
                            ActionLog.getInstance(activity).addAction(ActionLog.DialogLeftBtn, leftBtn.getText());
                        } else {
                            ActionLog.getInstance(activity).addAction(ActionLog.DialogRightBtn, rightBtn.getText());
                        }
                        onClickListener.onClick(dialog, view.getId() == R.id.button1 ? DialogInterface.BUTTON_POSITIVE : DialogInterface.BUTTON_NEGATIVE);
                    }
                }
            };
            if (leftButtonText == null) {
                leftView.setVisibility(View.GONE);
                rightView.setVisibility(View.VISIBLE);
                rightBtn.setText(rightButtonText);
                rightBtn.setOnClickListener(listener);
            } else if (rightButtonText == null) {
                leftView.setVisibility(View.VISIBLE);
                rightView.setVisibility(View.GONE);
                leftBtn.setText(leftButtonText);
                leftBtn.setOnClickListener(listener);
            } else {
                leftView.setVisibility(View.VISIBLE);
                rightView.setVisibility(View.VISIBLE);
                leftBtn.setText(leftButtonText);
                leftBtn.setOnClickListener(listener);
                rightBtn.setText(rightButtonText);
                rightBtn.setOnClickListener(listener);
            }
        } else {
            buttonView.setVisibility(View.GONE);
        }
        
        // 下面这个判断，是为了避免点击切换城市对话框中的确认按钮结果仍然有其它对话框在显示的问题
        if (activity instanceof Sphinx
                && (message == null
                        || message.startsWith(activity.getString(R.string.are_your_change_to_location_city).substring(0, 4)) == false)) {
            ((Sphinx)activity).setDialog(dialog);
        }
        
        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                ActionLog.getInstance(activity).addAction(ActionLog.Dialog + ActionLog.Dismiss);
            }
        });
        
        return dialog;
    }
    
    public static Dialog getChoiceDialog(final Activity activity, View custom, int theme) {
        
        final Dialog dialog = new Dialog(activity, theme);
        
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(custom);
        
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity(Gravity.CENTER);
        
        custom.findViewById(R.id.paddingPanel).setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dialog.dismiss();
                return false;
            }
        });
        
        return dialog;
    }

    public static ListView makeListView(Context context) {
    	return makeListView(context, R.drawable.bg_real_line);
    }
    
    public static ListView makeListView(Context context, int dividerResId) {
        ListView listView = new ListView(context);
        listView.setFadingEdgeLength(0);
        listView.setScrollingCacheEnabled(false);
        listView.setFooterDividersEnabled(false);
        listView.setDivider(context.getResources().getDrawable(dividerResId));
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
   
    
    @SuppressWarnings("rawtypes")
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
     * 根据手机的分辨率从 px 的单位 转成为 dp
     */
    public static int px2dip(Context context, int px) {
    	final float scale = context.getResources().getDisplayMetrics().density;
    	return (int) (px / scale + 0.5f);
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
    
    public static int[] makePagedIndex(SpringbackListView listView, int size, int firstVisiblePosition) {
        
        int minIndex, maxIndex, firstPosition;
        
        int configPageSize = TKConfig.getPageSize();
        
        if (size <= configPageSize) {
            minIndex = 0;
            maxIndex = size-1;
            firstPosition = firstVisiblePosition;
        } else if (firstVisiblePosition + configPageSize - 1 < size) {
            minIndex = firstVisiblePosition;
            maxIndex = firstVisiblePosition + configPageSize - 1;
            firstPosition = 0;
        } else {
            minIndex = size - configPageSize;
            firstPosition = firstVisiblePosition - minIndex;
            maxIndex = size -1;
        }
        
        return new int[]{minIndex, maxIndex, firstPosition};
    }
    
    public static void pageIndicatorInit(Context context, ViewGroup viewPoints, int size) {
        pageIndicatorInit(context, viewPoints, size, 0, R.drawable.ic_viewpage_indicator_normal, R.drawable.ic_viewpage_indicator_selected);
    }
    
    public static void pageIndicatorInit(Context context, ViewGroup viewPoints, int size, int selectedIndex, int normalResId, int selectedResId) {
        int margin = (int)(Globals.g_metrics.density * 2);
        for(int i=0;i<size;i++){
            ImageView imageView = new ImageView(context);            
            //默认选中的是第一张图片，此时第一个小圆点是选中状态，其他不是
            if(i==selectedIndex){
                imageView.setBackgroundResource(selectedResId);
            }else{
                imageView.setBackgroundResource(normalResId);
            }
            
            //将imageviews添加到小圆点视图组
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            viewPoints.addView(imageView, layoutParams);
        }

    }
    
    public static void pageIndicatorChanged(Context context, ViewGroup viewPoints, int selectedIndex) {
        pageIndicatorChanged(context, viewPoints, selectedIndex, R.drawable.ic_viewpage_indicator_normal, R.drawable.ic_viewpage_indicator_selected);
    }
    
    public static void pageIndicatorChanged(Context context, ViewGroup viewPoints, int selectedIndex, int normalResId, int selectedResId) {
        for(int i=0, size = viewPoints.getChildCount();i<size;i++){
            ImageView imageView = (ImageView) viewPoints.getChildAt(i);
            
            //默认选中的是第一张图片，此时第一个小圆点是选中状态，其他不是
            if(i==selectedIndex){
                imageView.setBackgroundResource(selectedResId);
            }else{
                imageView.setBackgroundResource(normalResId);
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
            ListView listView = Utility.makeListView(activity);
            listView.setAdapter(adapter);
            
            final Dialog dialog = showNormalDialog(activity,
                    activity.getString(R.string.call_telephone),
                    listView);
            
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

    public static void queryTraffic(final Sphinx sphinx, final POI poi, String actionTag) {
        queryTraffic(sphinx, poi, TrafficQueryFragment.END, actionTag);
    }
    
    public static void queryTraffic(final Sphinx sphinx, final POI poi, final int location, final String actionTag) {
        
        final POI poiForTraffic = poi.clone();

        View view = sphinx.getLayoutInflater().inflate(R.layout.alert_go_to_here, null, false);
        final Dialog dialog = Utility.getChoiceDialog(sphinx, view, R.style.AlterChoiceDialog);
        
        View button1 = view.findViewById(R.id.button1_view);
        View button2 = view.findViewById(R.id.button2_view);
        View button3 = view.findViewById(R.id.button3_view);
        View button4 = view.findViewById(R.id.button4_view);
        
        View.OnClickListener onClickListener = new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                int id = v.getId();
                dialog.dismiss();
                int queryType = -1;
                int index = 0;
                switch (id) {
                    case R.id.button1_view:
                        queryType = TrafficQuery.QUERY_TYPE_TRANSFER;
                        index = 0;
                        break;
                        
                    case R.id.button2_view:
                        queryType = TrafficQuery.QUERY_TYPE_DRIVE;
                        index = 1;
                        break;
                        
                    case R.id.button3_view:
                        queryType = TrafficQuery.QUERY_TYPE_WALK;
                        index = 2;
                        break;
                        
                    case R.id.button4_view:
                        index = 3;
                        break;
                }
                ActionLog.getInstance(sphinx).addAction(actionTag + ActionLog.GotoHere + ActionLog.ListViewItem, index);
                
                CityInfo locationCityInfo = Globals.g_My_Location_City_Info;
                TrafficQueryFragment trafficQueryFragment = sphinx.getTrafficQueryFragment();
                if (queryType != -1
                        && locationCityInfo != null
                        && MapEngine.getCityId(poi.getPosition()) == MapEngine.getCityId(locationCityInfo.getPosition())) {
                    POI locationPOI = new POI();
                    locationPOI.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
                    locationPOI.setPosition(locationCityInfo.getPosition());
                    locationPOI.setName(sphinx.getString(R.string.my_location));
                    POI start;
                    POI end;
                    if (location == TrafficQueryFragment.START) {
                        start = poiForTraffic;
                        end = locationPOI;
                    } else {
                        start = locationPOI;
                        end = poiForTraffic;
                    }
                    
                    sphinx.getTrafficQueryFragment().addHistoryWord(poiForTraffic, HistoryWordTable.TYPE_TRAFFIC);
                    TrafficQueryFragment.submitTrafficQuery(sphinx, start, end, queryType);
                } else {
                    trafficQueryFragment.setDataNoSuggest(poiForTraffic, location, queryType);
                    sphinx.uiStackRemove(R.id.view_result_map);   // 再回退时不出现地图界面
                    sphinx.showView(R.id.view_traffic_home);
                }
            }
        };
        button1.setOnClickListener(onClickListener);
        button2.setOnClickListener(onClickListener);
        button3.setOnClickListener(onClickListener);
        button4.setOnClickListener(onClickListener);
        
        dialog.show();
        
        ActionLog.getInstance(sphinx).addAction(actionTag + ActionLog.GotoHere);

        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                ActionLog.getInstance(sphinx).addAction(actionTag + ActionLog.GotoHere + ActionLog.Dismiss);
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
            case TelephonyManager.NETWORK_TYPE_IDEN:  
                return false; // ~25 kbps   
//            case Connectivity.NETWORK_TYPE_LTE:  
//                return true; // ~ 10+ Mbps  
            // Unknown  
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:  
                return false;   
            default:  
                return true;  
            }  
        }else{  
            return false;  
        }  
    }  
    
    public static void formatText(TextView textView, String word, String key, int color) {
        if (key != null && word != null){
            int keyLength = key.length();
            int wordLength = word.length();
            if (keyLength > 0
                    && wordLength > 0) {
                int index = word.indexOf(key);
                if (index != -1) {
                    SpannableStringBuilder style=new SpannableStringBuilder(word);  
                    style.setSpan(new ForegroundColorSpan(color),
                            index, 
                            index + (keyLength > wordLength ? wordLength : keyLength),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textView.setText(style);
                    return;
                }
            }
        }
        textView.setText(word);
    }
    
    public static String substring(String text, int length) {
        if (text == null) {
            return text;
        }
        if (text.length() > length) {
            text = text.substring(0, length) + "...";
        }
        return text;
    }
    
    public static void showDialogAcitvity(Activity activity, String msg) {
        if (activity == null || msg == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setClass(activity, ErrorDialogActivity.class);
        intent.putExtra(ErrorDialogActivity.ERROR_MSG, msg);
        activity.startActivity(intent);
    }
    
    /**
     * 根据指定的高宽生成图片文件名的访问路径
     * 例如:
     * 输入： http://www.tigerknows.com/logo.jpg
     * 输出： http://www.tigerknows.com/logo_320_480.jpg    
     * @param url
     * @param widthHeight
     * @return
     */
    public static String getPictureUrlByWidthHeight(String url, String widthHeight) {
        String result = url;
        if (TextUtils.isEmpty(url) == false && widthHeight != null) {
            int lastIndex = url.lastIndexOf(".");
            if (lastIndex > 0 && lastIndex < url.length()) {
                result = url.substring(0, lastIndex) + "_" + widthHeight + url.substring(lastIndex);
            }
        }
        return result;
    }
    
    /**
     * 对于编辑框为空或格式错出现提示的时候，需要弹出软键盘
     */
    public static void showEdittextErrorDialog(final TKActivity activity, String message, final View source){
    	showNormalDialog(activity, 
    			activity.getString(R.string.prompt),
    			message,
    			activity.getString(R.string.confirm),
    			null,
    			new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						source.requestFocus();
						activity.showSoftInput(source);
					}
    	});
    }

    public static void showTakePhotoDialog(final String actionTag, final Activity activity, final int pickRequestCode,
            final int captureRequestCode, final Uri captureUri) {
        final List<String> textList = new ArrayList<String>();
        textList.add(activity.getString(R.string.capture_photo));
        textList.add(activity.getString(R.string.pick_photo));
        final ArrayAdapter<String> adapter = new StringArrayAdapter(activity, textList,
                null);

        ListView listView = Utility.makeListView(activity);
        listView.setAdapter(adapter);

        final Dialog dialog = Utility.showNormalDialog(activity,
                activity.getString(R.string.storefront_photo), null, listView, null, null, null);

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View arg1, int which, long arg3) {
                ActionLog actionLog = ActionLog.getInstance(activity);
                if (which == 0) {
                    actionLog.addAction(actionTag+ActionLog.CameraPhoto);
                    capturePicture(activity, captureRequestCode, captureUri);
                } else if (which == 1) {
                    actionLog.addAction(actionTag+ActionLog.PickPhoto);
                    pickPicture(activity, pickRequestCode);
                }
                dialog.dismiss();
            }
        });
    }

    public static void capturePicture(Activity activity, int requestCode, Uri uri) {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // intent.putExtra("crop", "circle");
            if (uri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            }
            activity.startActivityForResult(intent, requestCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void pickPicture(Activity activity, int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(intent, requestCode);
    }

    public static String imageUri2FilePath(Activity activity, Uri uri) {
        String[] projection = {
            MediaStore.Images.Media.DATA
        };
        Cursor actualImageCursor = activity.managedQuery(uri, projection, null, null, null);
        if (actualImageCursor != null && actualImageCursor.getCount() == 1) {
            actualImageCursor.moveToFirst();
            int actualImageColumnIndex = actualImageCursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String imagePath = actualImageCursor.getString(actualImageColumnIndex);
            return imagePath;
        } else {
            return uri.toString().replace("file://", "");
        }
    }

    public static Bitmap imageUri2Bitmap(Activity activity, Uri uri) {
        ContentResolver contentResolver = activity.getContentResolver();
        try {
            LogWrapper.d("Utils", "imageUri2Bitmap() uri=" + uri);
            Bitmap bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri));
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Uri compressPhoto(Activity activity, Uri uri) {
        Bitmap bitmap = imageUri2Bitmap(activity, uri);
        File file = new File(imageUri2FilePath(activity, uri));
        FileOutputStream fout = null;
        try {
            if (file.exists()) {
                if (file.delete()) {
                    if (!file.createNewFile()) {
                        Log.e("Utils", "Unable to create new file: " + file.toString());
                    }
                }
            }
            fout = new FileOutputStream(file, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            uri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e("Utils", "IOException caught while closing stream", e);
                }
            }
        }

        return uri;
    }

    public static Bitmap getBitmapByUri(Activity activity, Uri uri, int width, int height) {
        String imageFilePath = imageUri2FilePath(activity, uri);
        if (TextUtils.isEmpty(imageFilePath)) {
            return null;
        }
        // Load up the image's dimensions not the image itself
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(imageFilePath, bmpFactoryOptions);

        int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
        int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);

        // If both of the ratios are greater than 1,
        // one of the sides of the image is greater than the screen
        if ((heightRatio > 1) || (widthRatio > 1)) {
            if (heightRatio > widthRatio) {
                // Height ratio is larger, scale according to it
                bmpFactoryOptions.inSampleSize = heightRatio;
            } else {
                // Width ratio is larger, scale according to it
                bmpFactoryOptions.inSampleSize = widthRatio;
            }
        }

        // Decode it for real
        bmpFactoryOptions.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeFile(imageFilePath, bmpFactoryOptions);
        return bmp;
    }

    public static Matrix resizeSqareMatrix(int width, int height, int maxLength) {
        int max = Math.max(width, height);
        float scale = ((float) maxLength) / max;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        float dx = 0, dy = 0;
        if (scale * width > width) {
            dx = (scale * width - maxLength) / 2;
        }
        if (scale * height > height) {
            dy = (scale * height - maxLength) / 2;
        }
        matrix.postTranslate(-dx, -dy);
        return matrix;
    }

    public static Matrix resizeMaxWidthMatrix(int width, int height, int maxWidth, int maxHeight) {
        Matrix matrix = new Matrix();
        float maxWidthScale = ((float) maxWidth) / width;
        float maxHeightScale = ((float) maxHeight) / height;
        float scale = maxWidthScale;
        if (maxWidthScale > maxHeightScale) {
            scale = maxHeightScale;
        }
        float dx = 0, dy = 0;
        if (scale * width > width) {
            dx = (scale * width - maxWidth) / 2;
        }
        if (scale * height > height) {
            dy = (scale * height - maxHeight) / 2;
        }
        matrix.postScale(scale, scale);
        matrix.postTranslate(-dx, -dy);
        return matrix;
    }

    public static boolean bitmapToFile(Bitmap bitmap, File file) {
        if (bitmap == null || file == null) {
            return false;
        }
        FileOutputStream fout = null;
        try {
            if (file.exists()) {
                if (file.delete()) {
                    if (!file.createNewFile()) {
                        Log.e("Utils", "Unable to create new file: " + file.toString());
                        return false;
                    }
                } else {
                    return false;
                }
            }
            fout = new FileOutputStream(file, true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, TKConfig.Photo_Compress_Ratio, fout);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e("Utils", "IOException caught while closing stream", e);
                }
            }
        }
        return false;
    }

    public static boolean copyFile(URI source, URI target) {
        return copyFile(new File(source), new File(target), true);
    }
    
    public static boolean copyFile(String source, String target) {
        return copyFile(new File(source), new File(target), true);
    }

    public static boolean copyFile(File sourceFile, File targetFile, boolean rewrite) {
        boolean result = false;
        if (sourceFile == null || targetFile == null) {
            return result;
        }
        if (!sourceFile.exists()) {
            return result;
        }

        if (!sourceFile.isFile()) {
            return result;
        }

        if (!sourceFile.canRead()) {
            return result;
        }

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        
        if (sourceFile.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
            result = true;
            return result;
        }

        if (targetFile.exists() && rewrite) {
            targetFile.delete();
        }
        
        try {
            if (targetFile.createNewFile()) {
                java.io.FileInputStream fosfrom = new java.io.FileInputStream(sourceFile);
                java.io.FileOutputStream fosto = new FileOutputStream(targetFile);
                byte bt[] = new byte[1024];
                int c;
                while ((c = fosfrom.read(bt)) > 0) {
                   fosto.write(bt, 0, c); //将内容写到新文件当中
                }
                fosfrom.close();
                fosto.close();
                result = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * Creates a centered bitmap of the desired size. Recycles the input.
     * 
     * @param source
     */
    public static Bitmap extractMiniThumb(Bitmap source, int width, int height) {
        return extractMiniThumb(source, width, height, true);
    }

    public static Bitmap extractMiniThumb(Bitmap source, int width, int height,
            boolean recycle) {
        if (source == null) {
            return null;
        }

        float scale;
        if (source.getWidth() < source.getHeight()) {
            scale = width / (float) source.getWidth();
        } else {
            scale = height / (float) source.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap miniThumbnail = transform(matrix, source, width, height, false);

        if (recycle && miniThumbnail != source) {
            source.recycle();
        }
        return miniThumbnail;
    }

    public static Bitmap transform(Matrix scaler, Bitmap source,
            int targetWidth, int targetHeight, boolean scaleUp) {
        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
             * In this case the bitmap is smaller, at least in one dimension,
             * than the target. Transform it by placing as much of the image as
             * possible into the target and leaving the top/bottom or left/right
             * (or both) black.
             */
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);

            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf
                    + Math.min(targetWidth, source.getWidth()), deltaYHalf
                    + Math.min(targetHeight, source.getHeight()));
            int dstX = (targetWidth - src.width()) / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight
                    - dstY);
            c.drawBitmap(source, src, dst, null);
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();

        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect = (float) targetWidth / targetHeight;

        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            float scale = targetWidth / bitmapWidthF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        }

        Bitmap b1;
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(),
                    source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }

        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);

        Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth,
                targetHeight);

        if (b1 != source) {
            b1.recycle();
        }

        return b2;
    }
    
    private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f' };
          
    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[b[i] & 0x0f]);
        }
        return sb.toString();
    }
          
    public static String md5sum(String filename) {
        InputStream fis;
        byte[] buffer = new byte[1024];
        int numRead = 0;
        MessageDigest md5;
        try{
            fis = new FileInputStream(filename);
            md5 = MessageDigest.getInstance("MD5");
            while((numRead=fis.read(buffer)) > 0) {
                md5.update(buffer,0,numRead);
            }
            fis.close();
            return toHexString(md5.digest());   
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 把若干个同类数组合并到一个数组中
     * @param arrays
     * @return
     */
    public static String[] mergeArray(String[] ...arrays) {
        List<String> tmp = new LinkedList<String>();
        for (String[] a : arrays) {
            if (a != null) {
                tmp.addAll(Arrays.asList(a));
            }
        }
        return (tmp.toArray(new String[]{}));
    }

    public static void printStackTrace() {
        try {
            int a = 1 / 0;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

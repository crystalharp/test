package com.tigerknows.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.tigerknows.widget.Toast;

import com.decarta.android.location.Position;
import com.decarta.android.map.MapView.MapScene;
import com.decarta.android.map.MapView.SnapMap;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.share.QZoneSend;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.share.WeiboSend;
import com.tigerknows.view.StringArrayAdapter;

/**
 * 一些关于为Widget附加的常用操作
 * ===== 是否要转化为自定义VIEW, 有待考量????? =====
 * 
 * @author linqingzu
 *
 */
public class WidgetUtils {

        
    /**
     * 生成分享对话框, 并传入数据
     * 
     * @param activity
     * @param data
     * @param smsContent
     * @param weiboContent
     * @param layerType
     * @return
     */
    public static void share(final Activity activity, final String smsContent, final String weiboContent, final String qzoneContent, final Position position) {
        share(activity, smsContent, weiboContent, qzoneContent, position, null);
    }
    public static void share(final Activity activity, final String smsContent, final String weiboContent, final String qzoneContent, final Position position, final MapScene mapScene) {
        
        final Sphinx sphinx = (Sphinx)activity;
        String[] list = activity.getResources().getStringArray(R.array.share);
        final ResolveInfo resolveInfo = sphinx.getSmsResolveInfo();
        int[] leftCompoundResIdList;
        if (resolveInfo != null) {
            leftCompoundResIdList = new int[] {R.drawable.ic_share_sina, R.drawable.ic_share_qzone, R.drawable.ic_share_sms, R.drawable.ic_share_mms, R.drawable.ic_share_more};
        } else {
            leftCompoundResIdList = new int[] {R.drawable.ic_share_sina, R.drawable.ic_share_qzone, R.drawable.ic_share_sms, R.drawable.ic_share_more};
            String[] newlist = new String[4];
            newlist[0] = list[0];
            newlist[1] = list[1];
            newlist[2] = list[2];
            newlist[3] = list[4];
            list = newlist;
        }
        final ArrayAdapter<String> adapter = new StringArrayAdapter(activity, list, leftCompoundResIdList);
        
        ListView listView = CommonUtils.makeListView(activity);
        listView.setAdapter(adapter);
        
        final Dialog dialog = CommonUtils.showNormalDialog(activity, 
                activity.getString(R.string.share), 
                null,
                listView,
                null,
                null,
                null);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                ActionLog actionLog = ActionLog.getInstance(activity);
                Intent intent;
                switch (index) {
                    case 0:
                        actionLog.addAction(ActionLog.ShareWeibo);
                        sphinx.snapMapView(new SnapMap() {
                            
                            @Override
                            public void finish(Uri uri) {
                                Intent intent = new Intent();
                                if(uri != null) {
                                    intent.putExtra(ShareAPI.EXTRA_SHARE_PIC_URI, uri.toString());
                                }
                                intent.putExtra(ShareAPI.EXTRA_SHARE_CONTENT, weiboContent);
                                intent.setClass(activity, WeiboSend.class);
                                activity.startActivity(intent);
                            }
                        }, position, mapScene);
                        break;
                    case 1:
                        actionLog.addAction(ActionLog.ShareQzone);
                        intent = new Intent();
                        intent.setClass(activity, QZoneSend.class);
                        intent.putExtra(ShareAPI.EXTRA_SHARE_CONTENT, qzoneContent);
                        activity.startActivity(intent);
                        break;
                    case 2:
                        actionLog.addAction(ActionLog.ShareSms);
                        intent = new Intent(Intent.ACTION_VIEW);    
                        intent.putExtra(Intent.EXTRA_TEXT, smsContent);
                        intent.putExtra("sms_body", smsContent);
                        intent.setType("vnd.android-dir/mms-sms");  
                        
                        try {
                            activity.startActivity(intent);
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(activity, R.string.no_way_to_share_message, Toast.LENGTH_SHORT).show();
                        }
                        break;
                        
                    case 3:
                        if (resolveInfo != null) {
                            actionLog.addAction(ActionLog.ShareMms);

                            sphinx.snapMapView(new SnapMap() {
                                
                                @Override
                                public void finish(Uri uri) {
                                    Intent intent = new Intent();
                                    intent = new Intent(Intent.ACTION_SEND, Uri.parse("mms://"));
                                    intent.setType("image/png");
                                    intent.putExtra(Intent.EXTRA_TEXT, smsContent);
                                    if(uri != null) {
                                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                                        intent.putExtra("file_name", uri.toString());
                                    }
                                    intent.putExtra("sms_body", smsContent);
                                    intent.putExtra("exit_on_sent", true);
                                    intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                                    try {
                                        activity.startActivity(intent);
                                    } catch (android.content.ActivityNotFoundException ex) {
                                        Toast.makeText(activity, R.string.no_way_to_share_message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }, position, mapScene);
                            break;
                        }  
                        
                    case 4:
                        actionLog.addAction(ActionLog.ShareMore);
                        sphinx.snapMapView(new SnapMap() {
                            
                            @Override
                            public void finish(Uri uri) {
                                if(uri != null) {
                                    CommonUtils.share(sphinx, activity.getString(R.string.share), smsContent, uri);
                                }}
                        }, position, mapScene);
                        break;
                }
                dialog.dismiss();
            }
        });
    }
}

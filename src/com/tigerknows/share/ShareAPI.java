package com.tigerknows.share;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.decarta.android.location.Position;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.crypto.Base64;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.POI;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.keep.AccessTokenKeeper;

/**
 * 统一管理新浪微博和腾讯QQ空间分享的用户信息
 * @author pengwenyue
 *
 */
public class ShareAPI {

    public static final String EXTRA_SHARE_CONTENT = "com.tigerknows.share.EXTRA_SHARE_CONTENT";

    public static final String EXTRA_SHARE_PIC_URI = "com.tigerknows.share.EXTRA_SHARE_PIC_URI";
    
    public static final String EXTRA_SHARE_USER_NAME = "com.tigerknows.share.EXTRA_SHARE_USER_NAME";
    
    public static final String EXTRA_SHARE_FINISH = "com.tigerknows.share.EXTRA_SHARE_FINISH";

    public static final String TYPE_WEIBO = "com_tigerknows_share_impl_SinaWeiboShare";
    
	public static final String TYPE_TENCENT = "com_tigerknows_share_impl_TencentOpenShare";
	
	private static final HashMap<String, UserAccessIdenty> sUserCache = new HashMap<String, UserAccessIdenty>();
    
	/**
	 * 用户授权操作后的回调接口
	 * @author pengwenyue
	 *
	 */
    public interface LoginCallBack {
        /**
         * 授权成功
         */
        public void onSuccess();
        
        /**
         * 授权失败
         */
        public void onFailed();
        
        /**
         * 取消授权
         */
        public void onCancel();
    }
	
	/**
	 * 读取用户信息
	 */
	public static UserAccessIdenty readIdentity(Activity activity, String shareType) {
		UserAccessIdenty identity = sUserCache.get(shareType);
		if (identity == null) {
    		try {
    			String shareIdenty = TKConfig.getPref(activity, shareType);
    			
    			if (!TextUtils.isEmpty(shareIdenty)) {
    				byte [] data = Base64.decode(shareIdenty, Base64.DEFAULT);
    //				ByteArrayInputStream byteArray = new ByteArrayInputStream(shareIdenty.getBytes());
    				ByteArrayInputStream byteArray = new ByteArrayInputStream(data);
    				ObjectInputStream in = new ObjectInputStream(byteArray);
    				identity = (UserAccessIdenty) in.readObject();
    				sUserCache.put(shareType, identity);
    				setToken(activity, shareType, identity);
    			}
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} 
		}
		
		return identity;
	}
	
	/**
	 * 存储用户信息
	 * @param activity
	 * @param shareType
	 * @param identity
	 */
	public static void writeIdentity(Activity activity, String shareType, UserAccessIdenty identity) {
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(); 
		ObjectOutputStream out;
		try {
		    out = new ObjectOutputStream(arrayOutputStream);
		    out.writeObject(identity);
		    out.close();
		    arrayOutputStream.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		TKConfig.setPref(activity, shareType, new String(Base64.encode(arrayOutputStream.toByteArray(), Base64.DEFAULT)));
        sUserCache.put(shareType, identity);
        setToken(activity, shareType, identity);
	}
	
	/**
	 * 删除用户信息
	 * @param activity
	 * @param shareType
	 */
	public static void clearIdentity(Activity activity, String shareType) {
		TKConfig.removePref(activity, shareType);
        sUserCache.remove(shareType);
        setToken(activity, shareType, null);
	}
	
	/**
	 * 设置用户授权的Token
	 * @param activity
	 * @param shareType
	 * @param identity
	 */
	private static void setToken(Activity activity, String shareType, UserAccessIdenty identity) {
	    if (TYPE_WEIBO.equals(shareType)) {
	        Oauth2AccessToken accessToken = AccessTokenKeeper.readAccessToken(activity);
	        if (identity != null && accessToken.isSessionValid()) {
                TKWeibo.accessToken = accessToken;
                Weibo.getInstance(TKWeibo.CONSUMER_KEY, TKWeibo.REDIRECT_URL).accessToken  = accessToken;
	        } else {
                TKWeibo.accessToken = null;
	            Weibo.getInstance(TKWeibo.CONSUMER_KEY, TKWeibo.REDIRECT_URL).accessToken = null;
	            AccessTokenKeeper.clear(activity);
	        }
        } else if (TYPE_TENCENT.equals(shareType)) {
            if (identity != null) {
                TKTencentOpenAPI.mAccessToken = identity.getAccessToken();
                TKTencentOpenAPI.mOpenId = identity.getSecret();
            } else {
                TKTencentOpenAPI.mAccessToken = null;
                TKTencentOpenAPI.mOpenId = null;
            }
        }
	}
    
    /**
     * 弹出分享对话框，选择某选项进行具体的分享操作
     * @param activity
     * @param data
     * @param position
     * @param actionTag
     * @return
     */
    public static void share(final Activity activity, final BaseData data, final Position position, String actionTag) {
        share(activity, data, position, null, actionTag);
    }
    
    public static void share(final Activity activity, final BaseData data, final Position position, final MapScene mapScene, final String actionTag) {
        if (activity == null
        		|| data == null) {
        	return;
        }
        final Sphinx sphinx = (Sphinx)activity;
        final String[] list = activity.getResources().getStringArray(R.array.share);
        final TKWeixin tkWeixin = TKWeixin.getInstance(activity);
        final List<String> textList = new ArrayList<String>();
        List<Integer> leftCompoundIconList = new ArrayList<Integer>();
        leftCompoundIconList.add(R.drawable.ic_share_sms);
        textList.add(list[0]);
        if (data instanceof POI) {
	        leftCompoundIconList.add(R.drawable.ic_share_weixin);
	        textList.add(list[1]);
	        leftCompoundIconList.add(R.drawable.ic_share_weixin);
	        textList.add(list[2]);
        }
        leftCompoundIconList.add(R.drawable.ic_share_sina);
        textList.add(list[3]);
        leftCompoundIconList.add(R.drawable.ic_share_qzone);
        textList.add(list[4]);
        leftCompoundIconList.add(R.drawable.ic_share_more);
        textList.add(list[5]);
        int size = leftCompoundIconList.size();
        int[] leftCompoundIconArray = new int[size];
        for(int i = 0; i < size; i++) {
            leftCompoundIconArray[i] = leftCompoundIconList.get(i);
        }
        final ArrayAdapter<String> adapter = new StringArrayAdapter(activity, textList, leftCompoundIconArray);
        
        ListView listView = Utility.makeListView(activity);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.showNormalDialog(activity, 
                activity.getString(R.string.share), 
                null,
                listView,
                null,
                null,
                null);
        
        final ActionLog actionLog = ActionLog.getInstance(activity);
        actionLog.addAction(actionTag + ActionLog.Share);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View arg1, int index, long arg3) {
                actionLog.addAction(actionTag + ActionLog.Share + ActionLog.ListViewItem, index, adapterView.getAdapter().getItem(index));
                String text = textList.get(index);

                if (list[0].equals(text)) {
                    String content = ShareTextUtil.makeText(activity, data, ShareTextUtil.SHARE_TYPE_SMS);
                    Intent intent = new Intent(Intent.ACTION_VIEW);    
                    intent.putExtra(Intent.EXTRA_TEXT, content);
                    intent.putExtra("sms_body", content);
                    intent.setType("vnd.android-dir/mms-sms");  
                    
                    try {
                        activity.startActivity(intent);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(activity, R.string.no_way_to_share_message, Toast.LENGTH_SHORT).show();
                    }                    
                } else if (list[1].equals(text) && data instanceof POI) {
                	if (tkWeixin.isSupportSend()) {
                	    tkWeixin.sendReq(TKWeixin.makePOIReq(activity, (POI) data), false);
                	}
                } else if (list[2].equals(text) && data instanceof POI) {
                	if (tkWeixin.isSupportTimeline()) {
                	    tkWeixin.sendReq(TKWeixin.makePOIReq(activity, (POI) data), true);
                	}          
                } else if (list[3].equals(text)) {
                    sphinx.snapMapView(new SnapMap() {
                        
                        @Override
                        public void finish(Uri uri) {
                            Intent intent = new Intent();
                            if(uri != null) {
                                intent.putExtra(ShareAPI.EXTRA_SHARE_PIC_URI, uri.toString());
                            }
                            intent.putExtra(ShareAPI.EXTRA_SHARE_CONTENT, ShareTextUtil.makeText(activity, data, ShareTextUtil.SHARE_TYPE_WEIBO));
                            intent.setClass(activity, WeiboSendActivity.class);
                            activity.startActivity(intent);
                        }
                    }, position, mapScene);
                        
                } else if (list[4].equals(text)) {
                    Intent intent = new Intent();
                    intent.setClass(activity, QZoneSendActivity.class);
                    intent.putExtra(ShareAPI.EXTRA_SHARE_CONTENT, ShareTextUtil.makeText(activity, data, ShareTextUtil.SHARE_TYPE_QZONE));
                    activity.startActivity(intent);              
                } else if (list[5].equals(text)) {
                    sphinx.snapMapView(new SnapMap() {
                        
                        @Override
                        public void finish(Uri uri) {
                        	String content = ShareTextUtil.makeText(activity, data, ShareTextUtil.SHARE_TYPE_SMS);
                        	Utility.share(sphinx, activity.getString(R.string.share), content, uri);
                        }
                    }, position, mapScene);
                    
                }
                dialog.setOnDismissListener(new OnDismissListener() {
                    
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        actionLog.addAction(actionTag + ActionLog.Share + ActionLog.Dismiss);
                    }
                });
                dialog.dismiss();
            }
        });
    }
}

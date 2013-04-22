package com.tigerknows.share;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import android.app.Activity;
import android.text.TextUtils;

import com.tigerknows.TKConfig;
import com.tigerknows.crypto.Base64;
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
}

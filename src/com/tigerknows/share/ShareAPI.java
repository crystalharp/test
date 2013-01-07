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
import com.weibo.net.AccessToken;
import com.weibo.net.Oauth2AccessTokenHeader;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;

public class ShareAPI {

    public static final String EXTRA_SHARE_CONTENT = "com.tigerknows.share.EXTRA_SHARE_CONTENT";

    public static final String EXTRA_SHARE_PIC_URI = "com.tigerknows.share.EXTRA_SHARE_PIC_URI";
    
    public static final String EXTRA_SHARE_USER_NAME = "com.tigerknows.share.EXTRA_SHARE_USER_NAME";
    
    public static final String EXTRA_SHARE_FINISH = "com.tigerknows.share.EXTRA_SHARE_FINISH";

    public static final String TYPE_WEIBO = "com_tigerknows_share_impl_SinaWeiboShare";
    
	public static final String TYPE_TENCENT = "com_tigerknows_share_impl_TencentOpenShare";
	
	private static final HashMap<String, UserAccessIdenty> sUserCache = new HashMap<String, UserAccessIdenty>();
    
    public interface LoginCallBack {
        public void onSuccess();
        public void onFailed();
        public void onCancel();
    }
	
	/*
	 * Storage Mediator
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
    				setToken(shareType, identity);
    			}
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} 
		}
		
		return identity;
	}
	
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
        setToken(shareType, identity);
	}
	
	public static void clearIdentity(Activity activity, String shareType) {
		TKConfig.removePref(activity, shareType);
        sUserCache.remove(shareType);
        setToken(shareType, null);
	}
	
	private static void setToken(String shareType, UserAccessIdenty identity) {
	    if (TYPE_WEIBO.equals(shareType)) {
	        if (identity != null) {
	            Utility.setAuthorization(new Oauth2AccessTokenHeader());
                AccessToken accessToken = new AccessToken(identity.getAccessToken(), Weibo.getAppSecret());
                accessToken.setExpiresIn(identity.getExpireIn());
                Weibo.getInstance().setAccessToken(accessToken);
	        } else {
	            Weibo.getInstance().setAccessToken(null);
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

package com.tigerknows.view.user;

import java.io.Serializable;

import android.content.Context;
import android.text.TextUtils;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.share.Base64;

public class User implements Serializable {

	// 用户登录后服务器返回的用户昵称
	private String nickName = "";
	
	// 用户登录后服务器返回的用户ID
	private long userId = Long.MIN_VALUE;

    // User对象在SharePreference存储的KEY值
    private static final String PREFS_KEY_USER_NICKNAME = "UserNickName";
    private static final String PREFS_KEY_USERID = "UserId";
    
    public boolean isNickNameDefault(Context context) {
    	return context.getString(R.string.default_nick_name).equals(nickName);
    }
    
	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	
	public long getUserId() {
        return userId;
    }
	
	public void setUserId(long userId) {
		this.userId = userId;
	}

	public User(String nickName, long userId) {
		super();
		this.nickName = nickName;
		this.userId = userId;
	}

	/**
	 * 存储当前User到SharePreference中
	 * @param context
	 */
	public void store(Context context) {
	    if (nickName != null) {
            TKConfig.setPref(context, PREFS_KEY_USER_NICKNAME, new String(Base64.encode(nickName.getBytes(), Base64.DEFAULT)));
            TKConfig.setPref(context, PREFS_KEY_USERID, new String(Base64.encode(String.valueOf(userId).getBytes(), Base64.DEFAULT)));
	    }
	}
	
	/**
	 * 从SharePreference中读取并保存到User对象
	 * @param context
	 */
	public static User loadDefault(Context context) {
		User user = null;
		try {
            String nickNameStr = TKConfig.getPref(context, PREFS_KEY_USER_NICKNAME);
            String userIdStr = TKConfig.getPref(context, PREFS_KEY_USERID);
            if (!TextUtils.isEmpty(nickNameStr) && !TextUtils.isEmpty(userIdStr)) {
                byte [] nickName = Base64.decode(nickNameStr, Base64.DEFAULT);
                byte [] userId = Base64.decode(userIdStr, Base64.DEFAULT);
                user = new User(new String(nickName), Long.parseLong(new String(userId)));
            }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return user;
	}
	
	public void clear(Context context) {
		TKConfig.removePref(context, PREFS_KEY_USER_NICKNAME);
        TKConfig.removePref(context, PREFS_KEY_USERID);
	}
}

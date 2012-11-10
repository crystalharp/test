package com.tigerknows.view.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private static final String PREFS_KEY = "User";
    
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
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(); 
		ObjectOutputStream out;
		try {
		    out = new ObjectOutputStream(arrayOutputStream);
		    out.writeObject(this);
		    out.close();
		    arrayOutputStream.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		TKConfig.setPref(context, PREFS_KEY, new String(Base64.encode(arrayOutputStream.toByteArray(), Base64.DEFAULT)));
	}
	
	/**
	 * 从SharePreference中读取并保存到User对象
	 * @param context
	 */
	public static User loadDefault(Context context) {
		User user = null;
		try {
			String prefSession = TKConfig.getPref(context, PREFS_KEY);
			
			if (!TextUtils.isEmpty(prefSession)) {
				byte [] data = Base64.decode(prefSession, Base64.DEFAULT);
				ByteArrayInputStream byteArray = new ByteArrayInputStream(data);
				ObjectInputStream in = new ObjectInputStream(byteArray);
				
				user = (User)in.readObject();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return user;
	}
	
	public void clear(Context context) {
		TKConfig.removePref(context, PREFS_KEY);
	}
}

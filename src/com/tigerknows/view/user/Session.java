package com.tigerknows.view.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.text.TextUtils;

import com.decarta.Globals;
import com.tigerknows.TKConfig;
import com.tigerknows.share.Base64;

public class Session implements Serializable{
	
	// 用户登录后获取的SessionId
	private String mSessionId = null;

	// Session有效期长度, ms(毫秒)表示
	private long timeout = 0;
	
	// Session有效期期限, ms(毫秒)表示
    private long mExpiresIn = 0;

    // Session对象在SharePreference存储的KEY值
    private static final String PREFS_KEY = "Session";
    
	public String getSessionId() {
		return mSessionId;
	}

	public void setTimeout(long timeout) {
		if (timeout != 0) {
			this.timeout = timeout;
			mExpiresIn = System.currentTimeMillis() + timeout * 1000;
        }
	}

	public void setTimeout(String timeout) {
		if (timeout != null && !timeout.equals("0")) {
			this.timeout = Integer.parseInt(timeout);
			setTimeout(Integer.parseInt(timeout));
        }
	}
	
	public Session(String mSessionId) {
		super();
		this.mSessionId = mSessionId;
	}

	public Session(String mSessionId, long timeout) {
		super();
		this.mSessionId = mSessionId;
		setTimeout(timeout);
	}
	
	public Session(String mSessionId, String timeout) {
		super();
		this.mSessionId = mSessionId;
		setTimeout(timeout);
	}
	
	/**
	 * 判断当前session是否有效
	 * @return
	 */
	public boolean isValid() {
		if (mSessionId != null && (mExpiresIn > System.currentTimeMillis() || mExpiresIn == 0)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 存储当前Session到SharePreference中
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
	 * 从SharePreference中读取并保存到Session对象
	 * @param context
	 */
	public static Session loadDefault(Context context) {
		Session session = null;
		try {
			String prefSession = TKConfig.getPref(context, PREFS_KEY);
			
			if (!TextUtils.isEmpty(prefSession)) {
				byte [] data = Base64.decode(prefSession, Base64.DEFAULT);
				ByteArrayInputStream byteArray = new ByteArrayInputStream(data);
				ObjectInputStream in = new ObjectInputStream(byteArray);
				
				session = (Session)in.readObject();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return session;
	}
	
	public void clear(Context context) {
		TKConfig.removePref(context, PREFS_KEY);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "sessionId: " + mSessionId + " expiresIn: " + mExpiresIn;
	}
	
}

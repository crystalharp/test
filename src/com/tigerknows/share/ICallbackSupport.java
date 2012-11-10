package com.tigerknows.share;

import android.app.Activity;

public interface ICallbackSupport {

	public void addCallBack(ICallBack callBack);
	
	/*
	 * Actually this is not very good!
	 * This Interface have two responsibility.
	 */
	public void endowIdentity(UserAccessIdenty identy);
	
	public void setActivity(Activity activity);
}

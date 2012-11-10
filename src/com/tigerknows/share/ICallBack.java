package com.tigerknows.share;

public interface ICallBack {

	public void onSuccess(int operate, String tip);
	
	public void onFail(int operate, String tip);
	
	public void onStore(UserAccessIdenty identy);

}

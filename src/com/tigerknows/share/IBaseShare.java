package com.tigerknows.share;

public interface IBaseShare {

	public static final int AUTH = 0x01;
	
	public static final int GET_USER_PROFILE = 0x02;
	
	public static final int UPLOAD = 0x03;
	
	public static final int LOGOUT = 0x04;
	
	public static final int OTHER = 0x00;
	
	public void auth();
	
	public String getUserName();
	
	public void requestUID();
	
	public void upload(String picPath, String content);
	
	public void logout();
	
	public boolean satisfyCondition();
	
	public Class getSenderName();
}

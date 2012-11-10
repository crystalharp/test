package com.tigerknows.share;

import java.io.Serializable;

public class UserAccessIdenty implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -857492274677957416L;

	private String userName;
	
	private String accessToken;
	
	private String secret;
	
	private String expireIn;
	
	public UserAccessIdenty() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UserAccessIdenty(String accessToken, String secret, String expireIn) {
		super();
		this.accessToken = accessToken;
		this.secret = secret;
		this.expireIn = expireIn;
	}

	public UserAccessIdenty(String userName, String accessToken, String secret, String expireIn) {
		super();
		this.userName = userName;
		this.accessToken = accessToken;
		this.secret = secret;
		this.expireIn = expireIn;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getExpireIn() {
		return expireIn;
	}

	public void setExpireIn(String expireIn) {
		this.expireIn = expireIn;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
	
}

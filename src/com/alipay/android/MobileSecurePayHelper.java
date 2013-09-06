/*
 * Copyright (C) 2010 The MobileSecurePay Project
 * All right reserved.
 * author: shiqun.shi@alipay.com
 */

package com.alipay.android;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * 检测安全支付服务是否正确安装，如果没有安装进行本地安装，或者下载安装， 检测安全支付服务版本，有新版本时进行下载。
 * 
 */
@SuppressLint("HandlerLeak")
public class MobileSecurePayHelper {
	static final String TAG = "MobileSecurePayHelper";

	Context mContext = null;

	public MobileSecurePayHelper(Context context) {
		mContext = context;
	}



	/**
	 * 遍历程序列表，判断是否安装安全支付服务
	 * 
	 * @return
	 */
	public boolean isMobile_spExist() {
		PackageManager manager = mContext.getPackageManager();
		List<PackageInfo> pkgList = manager.getInstalledPackages(0);
		for (int i = 0; i < pkgList.size(); i++) {
			PackageInfo pI = pkgList.get(i);
			if (pI.packageName.equalsIgnoreCase("com.alipay.android.app"))
				return true;
		}
		return false;
	}

}

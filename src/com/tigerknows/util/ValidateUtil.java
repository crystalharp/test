package com.tigerknows.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.decarta.android.util.LogWrapper;

import android.text.TextUtils;

public class ValidateUtil {

	private static final String TAG = "ValidateUtil";
	
	// 电话号码格式: 11个数字
	public static boolean isValidPhone(String phone) {
        Pattern pattern = Pattern.compile("^\\d{11}$");
        Matcher matcher = pattern.matcher(phone);
        
        return matcher.matches();
    }
	
	// 密码格式: 6到16个字符之间, 由字母及数字组成, 区分大小写
	public static boolean isValidPassword(String password) {
		Pattern pattern = Pattern.compile("^\\w{6,16}$");
        Matcher matcher = pattern.matcher(password);
        
        return matcher.matches();
	}
	
	// 昵称格式: 昵称由2-10个汉字或4-20个字符组成
	public static boolean isValidName(String nickname) {
		
		Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        Matcher matcher = pattern.matcher(nickname);
        
        int chineseCount = 0;
        while (matcher.find()) {  
        	chineseCount++;  
        }
        LogWrapper.d(TAG, "chineseCount: " + chineseCount);
        
        int totalCount = nickname.length() + chineseCount;
        LogWrapper.d(TAG, "totalCount: " + totalCount);
        if (totalCount < 4 || totalCount > 20) {
        	return false;
        }
        
        return true;
	}
}

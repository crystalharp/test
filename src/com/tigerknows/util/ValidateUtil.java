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
		
		int chineseCount = 0;
/* 下述的Unicode范围不完整，甚至连中文标点都未包含在内
 * 如仍需使用下述代码，建议将正则表达式设置为
 * [\\u2e80-\\u9fbf]——————————————————————————————————经测试仍然无效
 * 
 * ftx 2013/03/26
 */
//		Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
//        Matcher matcher = pattern.matcher(nickname);
//        while (matcher.find()) {  
//        	chineseCount++;  
//        }

		char[] nicknameChar = nickname.toCharArray();
		for(int i = 0; i < nicknameChar.length; i++){
			if((char)(byte)nicknameChar[i] != nicknameChar[i])chineseCount ++;
		}
        
        int totalCount = nickname.length() + chineseCount;
   
        if (totalCount < 4 || totalCount > 20) {
        	return false;
        }
        return true;
	}
}

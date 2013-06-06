package com.tigerknows.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public static boolean isValidElongName(String name){
		/*
		 * fengtianxiao: 2013/05/16
		 * 
		 * 艺龙有 艺龙旅行、艺龙酒店两种客户端，另有PC版本
		 *
		 * PC版本对姓名的限制是
		 * 不允许的
		 * 0001-007F 只有 002D-002F A-Z a-z 是允许的
		 * 0080-ffff 中以下不允许
		 * 00D7, 2018, 2019, 201C, 201D,
		 * 2026, 3001, 3002, 300A, 300B,
		 * 3014, 3015, FF01, FF05, FF08,
		 * FF09, FF0B, FF0C, FF1A, FF1B,
		 * FF1F, FF5B, FF5D, FF5E, FFE5
		 *
		 * 艺龙旅行对姓名的限制是
		 * 不做任何限制
		 *
		 * 艺龙酒店对姓名的限制是
		 * 限制输入字母和汉字，空格用斜杠'/'表示
		 * 即可输入 / A-Z a-z \u4e00-\u9fbf
		 *
		 * 注：汉字在Unicode中的分布大致如下表：
		 * 			首字编码	尾字编码	个数
		 * 基本汉字	U4E00	U9FBF	20928
		 * 异性字	UF900	UFAFF	512
		 * 扩展A		U3400	U4D8F	512
		 * 扩展B		U20000	U2A6DF	42720
		 * 补充		U2F800	U2FA1F	544
		 */
		
		// 依产品需求，按艺龙酒店的限制操作
		return Pattern.compile("^[\\A-Za-z\\u4e00-\\u9fbf]+$").matcher(name).matches();
	}
	
	public static boolean isValidCertCode(String code){
		return Pattern.compile("^\\w{1,30}$").matcher(code).matches();
	}
	
	public static boolean isValidCreditCard(String code){
		return Pattern.compile("^\\d{14,30}$").matcher(code).matches();
	}
	
	public static boolean isValidCreditCardVerify(String code){
		return Pattern.compile("^\\d{3}$").matcher(code).matches();
	}
	
	public static boolean isValidIdCardCode(String code){
		if(Pattern.compile("^\\d{17}[0-9Xx]$").matcher(code).matches() == false){
			return false;
		}else{
			int tempInt = 0;
			int factor = 1;
			if(code.charAt(17) == 'X' || code.charAt(17) == 'x')tempInt += 10;
			else tempInt += (int)(code.charAt(17) - '0');
			for (int i = 16; i >= 0 ; i--){
				factor *= 2;
				if (factor >= 11) factor -= 11;
				tempInt += (int)(code.charAt(i) - '0') * factor;
			}
			while(tempInt >= 11) tempInt -= 11;
			if (tempInt == 1) return true;
			else return false;
		}
	}
	
	public static boolean isValidHotelMobile(String code){
		return Pattern.compile("^1\\d{10}$").matcher(code).matches();
	}
}

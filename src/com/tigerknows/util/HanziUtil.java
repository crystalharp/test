package com.tigerknows.util;

import java.util.ArrayList;
import java.util.HashMap;

import android.text.TextUtils;

import com.android.providers.contacts.HanziToPinyin;
import com.android.providers.contacts.HanziToPinyin.Token;

public class HanziUtil {
	
	/**
	 * If the frist char is hanzi, first letter of its pinyin will be returned
	 * If the first char is letter, it will be returned
	 * Otherwise, '#' will be returned;
	 * @param input
	 * @return
	 */
	public static char getFirstPinYinChar(String input){
		ArrayList<Token> tokens = HanziToPinyin.getInstance().get(input);
		
		if(tokens!=null && tokens.size()!=0 ){
			Token firstToken = tokens.get(0);
			if( !TextUtils.isEmpty(firstToken.target) && Character.isLetter(firstToken.target.charAt(0)) ){
				return firstToken.target.charAt(0);
			}
		}
		return '#'; 
		
	}

}

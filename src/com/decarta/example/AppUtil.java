/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.example;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.android.location.Position;
import com.tigerknows.ui.common.ExceptionDialogActivity;

public class AppUtil {
	
	public static void alert(String msg, final Activity activity, final String level){
	    LogWrapper.e("Util.alert",msg+",activity:"+activity.getClass().getName());
	    Intent intent = new Intent(activity, ExceptionDialogActivity.class);
		intent.putExtra(activity.getPackageName()+".errorMsg", msg);
		intent.putExtra(activity.getPackageName()+".level", level);
		activity.startActivity(intent);
	}
	
	public static void setAutoCompleteTextViewAdapter(Context context, MultiAutoCompleteTextView view, String[] strings){
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_dropdown_item_1line, strings);
		view.setAdapter(adapter);
		view.setTokenizer(new Tokenizer() {
			@Override
			public CharSequence terminateToken(CharSequence text) {
				return text + " ";
			}

			@Override
			public int findTokenStart(CharSequence text, int cursor) {
				return 0;
			}

			@Override
			public int findTokenEnd(CharSequence text, int cursor) {
				return text.length();
			}
		});
	}
	
	/**
	 * return the direction angle clockwise relative to map north
	 * @param pos1
	 * @param pos2
	 * @return
	 */
	public static float getMovingDirection(Position pos1, Position pos2){
		double x=pos2.getLon()-pos1.getLon();
		double lat=(pos1.getLat()+pos2.getLat())/2;
		x*=Math.cos(lat*Math.PI/180);
		double y=pos2.getLat()-pos1.getLat();
		double angle=0;
		if(y==0){
			if(x>0) angle=90;
			else if(x<0) angle=-90;
			else angle=0;
		}else{
			angle=Math.atan(x/y)*180/Math.PI;
			if(x>0 && y<0){
				angle+=180;
			}else if(x<0 && y<0){
				angle-=180;
			}
		}
		return (float)(angle);
	}
}
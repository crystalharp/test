package com.decarta.android.util;

import java.util.LinkedList;

import android.util.Log;

import com.decarta.CONFIG;

/**
* 0: no log
* 1: error
* 2: warn
* 3: info
* 4: debug
*/
public class LogWrapper {
	private static LinkedList<String> content=new LinkedList<String>();
	public static LinkedList<String> getContent(){
		return content;
	}
	
	public static void dws(String tag, String tagm, byte[] data){
		if(CONFIG.LOG_LEVEL>=4){
			try{
				String msg=data==null?"null":(new String(data,"UTF-8"));
				dws(tag,tagm,msg);
			}catch(Exception e){
				
			}
			
		}
	}
	
	public static void dws(String tag, String tagm, String msg){
		if(CONFIG.LOG_LEVEL>=4){
			Log.d(tag,tagm+msg);
			
			content.addFirst("WS: "+tagm+msg);
			if(content.size()>CONFIG.LOG_SIZE){
				content.removeLast();
			}
		}
	}
	
	public static void d(String tag, String msg){
		if(CONFIG.LOG_LEVEL>=4){
			Log.d(tag,msg);
			
			content.addFirst("DEBUG: "+tag+" "+msg);
			if(content.size()>CONFIG.LOG_SIZE){
				content.removeLast();
			}
		}
	}
	
	public static void i(String tag, String msg){
		if(CONFIG.LOG_LEVEL>=3){
			Log.i(tag,msg);
			
			content.addFirst("INFO: "+tag+" "+msg);
			if(content.size()>CONFIG.LOG_SIZE){
				content.removeLast();
			}
		}
	}

	public static void w(String tag, String msg){
		if(CONFIG.LOG_LEVEL>=2){
			Log.w(tag,msg);
			
			content.addFirst("WARN: "+tag+" "+msg);
			if(content.size()>CONFIG.LOG_SIZE){
				content.removeLast();
			}
		}
	}
	
	public static void e(String tag, String msg){
		if(CONFIG.LOG_LEVEL>=1){
			if(msg==null) msg="";
			Log.e(tag,msg);
			
			content.addFirst("ERROR: "+tag+" "+msg);
			if(content.size()>CONFIG.LOG_SIZE){
				content.removeLast();
			}
		}
	}

}

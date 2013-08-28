package com.tigerknows.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.os.SystemClock;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;

public class CalendarUtil {
    public static boolean UseSystemTime = false;
    
	/**  
     * @param date1 需要比较的时间 不能为空(null),需要正确的日期格式  
     * @param date2 被比较的时间  为空(null)则为当前时间  
     * @param stype 返回值类型   0为多少天，1为多少个月，2为多少年  
     * @return  
     */ 
	
	// 格式ymd代表有y m d，8代表8位， h,s,c代表分隔符为-,/,中文字。
	public static final SimpleDateFormat ymd8 = new SimpleDateFormat("yyyyMMdd");
	public static final SimpleDateFormat ymd8h = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat ym6h = new SimpleDateFormat("yyyy-MM");
	public static final SimpleDateFormat ymd8s = new SimpleDateFormat("yyyy/MM/dd");
	public static final SimpleDateFormat y4mc = new SimpleDateFormat("yyyy年M月");
	public static final SimpleDateFormat ymd8c_hm4 = new SimpleDateFormat("yyyy年MM月dd日 hh:mm");
	
	static final String TAG = "CalendarUtil";
	
	static final String NTP_SERVER = "cn.pool.ntp.org";
	static final int NTP_TIMEOUT = 10000;
	
    public static int compareDate(Calendar c1,Calendar c2,int stype){  
        int n = 0;  
          
        //List list = new ArrayList();  
        while (!c1.after(c2)) {                     // 循环对比，直到相等，n 就是所要的结果  
            //list.add(df.format(c1.getTime()));    // 这里可以把间隔的日期存到数组中 打印出来  
            n++;  
            if(stype==1){  
                c1.add(Calendar.MONTH, 1);          // 比较月份，月份+1  
            }  
            else{  
                c1.add(Calendar.DATE, 1);           // 比较天数，日期+1  
            }  
        }  
          
        n = n-1;  
          
        if(stype==2){  
            n = (int)n/365;  
        }     
          
        return n;  
    } 

    public static long strDateToLong(SimpleDateFormat dateFormat, String str){
		Calendar cld = Calendar.getInstance();
		try {
			cld.setTime(dateFormat.parse(str));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return cld.getTimeInMillis();
	}
	
	public static int dateInterval(Calendar start, Calendar end){
		if(start.after(end)){
		    int days = start.get(Calendar.DAY_OF_YEAR) - end.get(Calendar.DAY_OF_YEAR);
	        int y2 = start.get(Calendar.YEAR);
	        if(end.get(Calendar.YEAR) != y2){
	            end = (Calendar) end.clone();
	            do{
	                days += end.getActualMaximum(Calendar.DAY_OF_YEAR);
	                end.add(Calendar.YEAR, 1);
	            }while(end.get(Calendar.YEAR) != y2);
	        }
	        return -days;
		}
		int days = end.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR);
		int y2 = end.get(Calendar.YEAR);
		if(start.get(Calendar.YEAR) != y2){
			start = (Calendar) start.clone();
			do{
				days += start.getActualMaximum(Calendar.DAY_OF_YEAR);
				start.add(Calendar.YEAR, 1);
			}while(start.get(Calendar.YEAR) != y2);
		}
		return days;
	}
	
	/**
	 * 自制时钟，通过NTP校时和时间调整广播来获取精准时间
	 * 对于无法监听时间变更广播和媒体扫描完毕广播的手机，这个方法返回的时间可能会不准确。
	 * 建议使用getNtpTime方法
	 * 
	 * 以下情况会返回不准确的时间：
	 * 1.程序无法监听时钟修改和媒体扫描完毕广播
	 * 2.用户在系统启动后，媒体扫描完毕之前就修改了时间
	 * 
	 * 原理是利用相对时钟不会乱，ntp校时成功的时候记录下一个ntp时间，它对应的绝对时间和
	 * 它对应的相对时间。然后ntp时间不变，一个时间变化的时候用另外一个时间来校准这个变化
	 * 的时间。
	 * 然后用户调整时间的时候根据相对时间来调整ntp时间对应的绝对时间，系统重启后根据绝对
	 * 时间来调整ntp时间对应的相对时间。
	 * 
	 * @param context
	 * @return current time
	 */
	public static long getExactTime(Context context) {
	    if (UseSystemTime) {
	        return Calendar.getInstance().getTimeInMillis();
	    }
	    long recordedSysTime = 0;
	    long recordedNtpTime = 0;
        String sSysTime = TKConfig.getPref(context, TKConfig.PREFS_RECORDED_SYS_ABS_TIME, null);
        if (sSysTime != null) {
            recordedSysTime = Long.parseLong(sSysTime);
        }
        String sNtpTime = TKConfig.getPref(context, TKConfig.PREFS_RECORDED_NTP_TIME, null);
        if (sNtpTime != null) {
            recordedNtpTime = Long.parseLong(sNtpTime);
        }
        
        Date date = new Date();
//        
//        date.setTime(recordedSysTime);
//        LogWrapper.d(TAG, "recordedSysTime:" + ymd8c_hm4.format(date));
//        
//        date.setTime(recordedNtpTime);
//        LogWrapper.d(TAG, "recordedNtpTime:" + ymd8c_hm4.format(date));
        
        long realTime = System.currentTimeMillis() - recordedSysTime + recordedNtpTime;
        date.setTime(realTime);
        LogWrapper.d(TAG, "exactTime:" + ymd8c_hm4.format(date));
	    
        return realTime;
	}
	
	/**
	 * 获取NTP时间，正常返回时间，请求失败返回0
	 * @return
	 */
	public static long getNtpTime() {
	    long ntpTime = 0;
	    SntpClient client = new SntpClient();
        if (client.requestTime(NTP_SERVER, NTP_TIMEOUT)) {
            ntpTime = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
            Date date = new Date();
            date.setTime(ntpTime);
            LogWrapper.d(TAG, "Ntp request succeed, time:" + ymd8c_hm4.format(date));
        }
        return ntpTime;
	}
	
	public static void initExactTime(Context context) {
	    long ntpTime = getNtpTime();
	    long absSysTime = System.currentTimeMillis();
	    long relSysTime = SystemClock.elapsedRealtime();
	    
	    if (ntpTime != 0) {
	        TKConfig.setPref(context, TKConfig.PREFS_RECORDED_NTP_TIME, String.valueOf(ntpTime));
	        TKConfig.setPref(context, TKConfig.PREFS_RECORDED_SYS_ABS_TIME, String.valueOf(absSysTime));
	        TKConfig.setPref(context, TKConfig.PREFS_RECORDED_SYS_REL_TIME, String.valueOf(relSysTime));
	    }
	}
	
	/**
	 * 获取当前时间坐标系下绝对时间对应的相对时间
	 * @param abs
	 * @return
	 */
	public static final long getRelTimeByAbsTime(long abs) {
	    if (abs <= 0) {
	        return 0;
	    } else {
	        return SystemClock.elapsedRealtime() - System.currentTimeMillis() + abs ;
	    }
	}
	
	/**
	 * 获取当前时间坐标系下相对时间对应的绝对时间
	 * @param rel
	 * @return
	 */
	public static final long getAbsTimeByRelTime(long rel) {
	    if (rel == 0) {
	        return 0;
	    } else {
	        return rel - SystemClock.elapsedRealtime() + System.currentTimeMillis();
	    }
	}
}

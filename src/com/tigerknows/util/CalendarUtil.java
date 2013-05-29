package com.tigerknows.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class CalendarUtil {
	private static final String TAG = "DateUtil";

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cld.getTimeInMillis();
	}
	
	public static int dateInterval(Calendar start, Calendar end){
		if(start.after(end)){
			return 0;
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
}

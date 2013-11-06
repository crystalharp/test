package com.tigerknows.util;

import java.util.Calendar;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;

public class SolarRadiation {

	private static double sin(double angle){
		return Math.sin(angle);
	}
	
	private static double cos(double angle){
		return Math.cos(angle);
	}
	
	public static double chiWeiJiao(double angle){
		return 0.3723 + 23.2567*sin(angle) + 0.1149*sin(2*angle) + -0.1712*sin(3*angle)-0.758*cos(angle)+0.3656*cos(2*angle)+0.0201*cos(3*angle);
	}
	
	public static double shiCha(double angle){
		return 0.0028 - 1.9857*sin(angle) + 9.9059*sin(2*angle) - 7.0924*cos(angle) - 0.6882*cos(2*angle);
	}
	
	public static double riJiao(Calendar calendar){
		int i = (calendar.get(Calendar.YEAR) - 1985)/4;
		double n0 = 79.6764 + 0.2422*(calendar.get(Calendar.YEAR) - 1985) - i;
		double t = calendar.get(Calendar.DAY_OF_YEAR) - n0;
		return 2*Math.PI*t/365.2422;
	}
	
	public static double shiJiao(Calendar calendar, double longitude){
		long sjMillis = (calendar.getTimeInMillis() + (long)(longitude * 240000) + (long)(shiCha(riJiao(calendar)) * 60000)) % 86400000;
		return Math.PI/ 180 * (sjMillis * 1.0 / 240000 - 180);
	}
	
	public static double gaoDuJiaoZhengXian(Calendar calendar, double longitude, double latitude){
		double rj = riJiao(calendar);
		double cwj = chiWeiJiao(rj);
		double sj = shiJiao(calendar, longitude);
		double wd = latitude * Math.PI / 180;
		return sin(cwj)*sin(wd) + cos(cwj)*cos(wd)*cos(sj);
	}
	
	public static double taiYangFangWeiJiao(Calendar calendar, double longitude, double latitude) throws APIException{
		double gdjzx = gaoDuJiaoZhengXian(calendar, longitude, latitude);
		double cwj = chiWeiJiao(riJiao(calendar));
		double wd = latitude * Math.PI / 180;
		if(Math.abs(gdjzx) > 1){
			throw new APIException("GaoDuJiaoZhengxian has invalid value: " + gdjzx);
		}
		if(gdjzx == 1){
			return 0;
		}
		double fwjyx = (gdjzx*sin(wd) - sin(cwj)) / (cos(wd) * Math.sqrt(1-gdjzx*gdjzx));
		if(Math.abs(fwjyx) > 1){
			throw new APIException("FangWeiJiaoYuXian has invalid value: " + fwjyx);
		}
		if(fwjyx == 1){
			return 0;
		}
		double sj = shiJiao(calendar, longitude);
		double tyfwj;
		if(sj<0){
			tyfwj = Math.acos(fwjyx);
		}else{
			tyfwj = 2*Math.PI - Math.acos(fwjyx);
		}
		return tyfwj * 180 / Math.PI;
	}
}

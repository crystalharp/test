package com.tigerknows.util;

import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.share.WeiboSend;

public class ShareTextUtil {
	
	public static final int WEIBO_MAX_LENGTH = 140;
	/*
	 * 
	 *  当前分享文本规则:
	 *	 1. POI微博文本
	 *	    #老虎地图分享# Poi名称+poi地址+联系电话+标题区（按顺序展示 评分:分 菜系: 均价:元 人气: 口味: 服务: 环境:） 来自@老虎地图 http://www.tigerknows.com/
	 *	    字数超过140时,自动截服务环境等
	 *   2. POI QQ空间文本
	 *      Poi名称+poi地址+联系电话+标题区（按顺序展示 评分:分 菜系: 均价:元 人气: 口味: 服务: 环境:） 来自@老虎地图 #老虎地图分享#
	 *	 3. POI短信文本
	 *	    POI名称, POI地址, 联系电话, 标题区(按顺序展示 评分:分 菜系: 均价:元 人气: 口味: 服务: 环境:)+来自老虎地图
	 *	 4. 公交换乘微博文本
	 *	    #老虎地图分享# 乘公交从XXX到XXX, 直达（换乘X次）, 全程XXX公里:（步骤列表） 来自@老虎地图 http://www.tigerknows.com/    
	 *	    如果超出125字的话，截取"(步骤列表中的文字)" 中的文本
	 *   5. 公交换乘 QQ空间文本:
	 *      乘公交从XXX到XXX, 直达（换乘X次）, 全程XXX公里:（步骤列表） 来自@老虎地图 #老虎地图分享#
	 *      如果超出125字的话，截取"(步骤列表中的文字)" 中的文本
	 *	 6.公交换乘短信文本
	 *	    乘公交从XXX到XXX, 直达（换乘X次）, 全程XXX公里:（步骤列表） 来自老虎地图
	 *	 7. 驾车微博文本
	 *	    #老虎地图分享# 驾车从XXX到XXX, 全程XXX公里. 来自@老虎地图 http://www.tigerknows.com/
	 *   8. 驾车 QQ空间文本
	 *   	驾车从XXX到XXX, 全程XXX公里. 来自@老虎地图 #老虎地图分享#
	 *	 9. 驾车短信文本
	 *	    驾车从XXX到XXX, 全程XXX公里. (步骤列表中的文字) 来自老虎地图 
	 *	 10.步行微博文本
	 *	    #老虎地图分享# 步行从XXX到XXX, 全程XXX公里. 来自@老虎地图 http://www.tigerknows.com/
	 *   11.步行 QQ空间文本
	 *      步行从XXX到XXX, 全程XXX公里. 来自@老虎地图 #老虎地图分享#
	 *	 12.步行短信文本
	 *	    步行从XXX到XXX, 全程XXX公里. (步骤列表中的文字) 来自老虎地图
	 *	 13.线路微博文本
	 *	    #老虎地图分享# +线路名(起点-终点)(终点-起点), 全程***, 途经**个站点. 来自@老虎地图 http://www.tigerknows.com/
	 *	 14.线路 QQ空间文本
     *   	线路名(起点-终点)(终点-起点), 全程***, 途经**个站点. 来自@老虎地图 #老虎地图分享#
	 *	 15.线路短信文本
     *		线路名(起点-终点)(终点-起点), 全程***, 途经**个站点. (线路站点列表) 来自老虎地图
     *
	 */
	
	/**
	 * #老虎地图分享# 乘公交从XXX到XXX（不能是我的当前位置），直达（换乘X次），全程XXX公里：（线路列表中的文字，如乘坐地铁1号线（M1），换乘地铁4号线（M4））。来自@老虎地图 http://www.tigerknows.com/
	 * 如果超出125字的话，截取"(步骤列表中的文字)" 中的文本
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficTransferWeiboContent(Plan plan, Context context) {
		
		String topic = context.getString(R.string.share_weibo_poi_topic) + context.getString(R.string.space);
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
		String body = "";
        int transferTimes = -1;

        for(Step step : plan.getStepList()) {
            if (Step.TYPE_TRANSFER == step.getType()) {
                if (!TextUtils.isEmpty(body)) {
                	body = body +context.getString(R.string.share_huancheng) + step.getTransferLineName();
                } else {
                	body = step.getTransferLineName();
                }
                transferTimes++;
            }
        }
        
        String summary = "";
        String length = getPlanLength(context, plan.getLength());
        if (transferTimes > 0) {
        	summary = context.getString(R.string.share_transfer_title, transferTimes, length);
        	body = context.getString(R.string.share_transfer_weibo_body, body);
        } else if(transferTimes == 0) {
        	summary = context.getString(R.string.share_nonstop_title, length);
        	body = context.getString(R.string.share_transfer_weibo_body, body);
        } else {
        	summary = context.getString(R.string.share_nonstop_title, length);
        	body = "";
        }
                
//        String tail = context.getString(R.string.share_weibo_text_tail);
        String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_tiger_official_website);
        String content = context.getString(R.string.share_transfer_weibo, topic, start, end, summary, body, tail);
        try{
	        if (content.length() > WEIBO_MAX_LENGTH) {
	        	String omission = context.getString(R.string.omission);
	        	int endIndex = content.length() - WEIBO_MAX_LENGTH - omission.length();
	        	if (endIndex > 0) {
	        		body = body.substring(0, body.length() - endIndex -2);
	        	}
	        	body += omission;
	        	
	        	content = context.getString(R.string.share_transfer_weibo, topic, start, end, summary, body, tail);
	        } 
        }catch(Exception e) {
        	e.printStackTrace();
        }
        
        return content;
        
	}
	
	/**
	 * 乘公交从XXX到XXX, 直达（换乘X次）, 全程XXX公里:（步骤列表） 来自@老虎地图 #老虎地图分享#
	 * 如果超出125字的话，截取"(步骤列表中的文字)" 中的文本
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficTransferQzoneContent(Plan plan, Context context) {
		
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
		String body = "";
        int transferTimes = -1;

        for(Step step : plan.getStepList()) {
            if (Step.TYPE_TRANSFER == step.getType()) {
                if (!TextUtils.isEmpty(body)) {
                	body = body +context.getString(R.string.share_huancheng) + step.getTransferLineName();
                } else {
                	body = step.getTransferLineName();
                }
                transferTimes++;
            }
        }
        
        String summary = "";
        String length = getPlanLength(context, plan.getLength());
        if (transferTimes > 0) {
        	summary = context.getString(R.string.share_transfer_title, transferTimes, length);
        	body = context.getString(R.string.share_transfer_weibo_body, body);
        } else if(transferTimes == 0) {
        	summary = context.getString(R.string.share_nonstop_title, length);
        	body = context.getString(R.string.share_transfer_weibo_body, body);
        } else {
        	summary = context.getString(R.string.share_nonstop_title, length);
        	body = "";
        }
                
//        String tail = context.getString(R.string.share_weibo_text_tail);
        String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_weibo_poi_topic);
        String content = context.getString(R.string.share_transfer_qzone, start, end, summary, body, tail);
        
        return content;
        
	}
	
	/**
	 * 乘公交从XXX到XXX, 直达（换乘X次）, 全程XXX公里:（步骤列表） 来自老虎地图
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficTransferSmsContent(Plan plan, Context context) {
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
		
        int transferTimes = -1;
        for(Step step : plan.getStepList()) {
            if (Step.TYPE_TRANSFER == step.getType()) {
                transferTimes++;
            }
        }
        
        StringBuilder body = new StringBuilder();
        List<CharSequence> strList = NavigationSplitJointRule.splitJoint(context, Step.TYPE_TRANSFER, plan);
        int i = 1;
        for(CharSequence str : strList) {
            body.append(i++);
            body.append(context.getString(R.string.period));
            body.append(str);
            if (i-1 != strList.size())
            	body.append(context.getString(R.string.nextline));
        }
                
        String summary = "";
        String length = getPlanLength(context, plan.getLength());
        
        if (transferTimes > 0) {
        	summary = context.getString(R.string.share_transfer_title, transferTimes, length);
        } else {
        	summary = context.getString(R.string.share_nonstop_title, length);
        }
        
        // 来自老虎地图
        String tail = context.getString(R.string.share_from_tigerknows);
        String content = context.getString(R.string.share_transfer_sms, start, end, summary, body, tail);

        return content;
        
	}
	
	/**
	 * #老虎地图分享# 驾车从XXX到XXX, 全程XXX公里. 来自@老虎地图 http://www.tigerknows.com/
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficDriveWeiboContent(Plan plan, Context context) {
		String topic = context.getString(R.string.share_weibo_poi_topic) + context.getString(R.string.space);
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
//		String tail = context.getString(R.string.share_weibo_text_tail);
		String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_tiger_official_website);
        String length = getPlanLength(context, plan.getLength());
		return context.getString(R.string.share_drive_weibo, topic, start, end, length, tail);
	}
	
	/**
	 * 驾车从XXX到XXX, 全程XXX公里. 来自@老虎地图 #老虎地图分享#
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficDriveQzoneContent(Plan plan, Context context) {
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
//		String tail = context.getString(R.string.share_weibo_text_tail);
		String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_weibo_poi_topic);
        String length = getPlanLength(context, plan.getLength());
		return context.getString(R.string.share_drive_qzone, start, end, length, tail);
	}
	
	/**
	 * 驾车从XXX到XXX, 全程XXX公里. (步骤列表中的文字) 来自老虎地图 
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficDriveSmsContent(Plan plan, Context context) {
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
		
		StringBuilder body = new StringBuilder();
        List<CharSequence> strList = NavigationSplitJointRule.splitJoint(context, Step.TYPE_DRIVE, plan);
        int i = 1;
        for(CharSequence str : strList) {
            body.append(i++);
            body.append(context.getString(R.string.period));
            body.append(str);
            if (i-1 != strList.size())
            	body.append(context.getString(R.string.nextline));
        }
		
        String length = getPlanLength(context, plan.getLength());
        // 来自老虎地图
        String tail = context.getString(R.string.share_from_tigerknows);
		return context.getString(R.string.share_drive_sms, start, end, length, body, tail);
	}
	
	/**
	 * #老虎地图分享# 步行从XXX到XXX, 全程XXX公里. 来自@老虎地图 http://www.tigerknows.com/
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficWalkWeiboContnet(Plan plan, Context context) {
		String topic = context.getString(R.string.share_weibo_poi_topic) + context.getString(R.string.space);
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
//		String tail = context.getString(R.string.share_weibo_text_tail);
		String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_tiger_official_website);
		String length = getPlanLength(context, plan.getLength());
		
		return context.getString(R.string.share_walk_weibo, topic, start, end, length, tail);
	}
	
	/**
	 * 步行从XXX到XXX, 全程XXX公里. 来自@老虎地图 #老虎地图分享#
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficWalkQzoneContnet(Plan plan, Context context) {
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
//		String tail = context.getString(R.string.share_weibo_text_tail);
		String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_weibo_poi_topic);
		String length = getPlanLength(context, plan.getLength());
		
		return context.getString(R.string.share_walk_qzone, start, end, length, tail);
	}
	
	/**
	 * 步行从XXX到XXX, 全程XXX公里. (步骤列表中的文字) 来自老虎地图
	 * @param plan
	 * @param context
	 * @return
	 */
	public static String shareTrafficWalkSmsContnet(Plan plan, Context context) {
		String start = plan.getStart().getName();
		String end = plan.getEnd().getName();
		
		StringBuilder body = new StringBuilder();
        List<CharSequence> strList = NavigationSplitJointRule.splitJoint(context, Step.TYPE_WALK, plan);
        int i = 1;
        for(CharSequence str : strList) {
            body.append(i++);
            body.append(context.getString(R.string.period));
            body.append(str);
            if (i-1 != strList.size())
            	body.append(context.getString(R.string.nextline));
        }
        String length = getPlanLength(context, plan.getLength());
        String tail = context.getString(R.string.share_from_tigerknows);
		return context.getString(R.string.share_walk_sms, start, end, length, body, tail);
	}
	
	/**
	 * #老虎地图分享# +线路名(起点-终点)(终点-起点), 全程***, 途经**个站点. 来自@老虎地图 http://www.tigerknows.com/
	 * @param line
	 * @param context
	 * @return
	 */
	public static String shareBuslineWeiboContent(Line line, Context context) {
		String topic = context.getString(R.string.share_weibo_poi_topic) + context.getString(R.string.space);
		int size = line.getStationList().size();
//		String tail = context.getString(R.string.share_weibo_text_tail);
		String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_tiger_official_website);
		String length = getPlanLength(context, line.getLength());
		
		return context.getString(R.string.share_busline_weibo, topic, line.getName(), length, size, tail);
	}
	
	/**
	 * 线路名(起点-终点)(终点-起点), 全程***, 途经**个站点. 来自@老虎地图 #老虎地图分享#
	 * @param line
	 * @param context
	 * @return
	 */
	public static String shareBuslineQzoneContent(Line line, Context context) {
		int size = line.getStationList().size();
//		String tail = context.getString(R.string.share_weibo_text_tail);
		String tail = context.getString(R.string.share_from_at_tigerknows) + context.getString(R.string.space)
        		+ context.getString(R.string.share_weibo_poi_topic);
		String length = getPlanLength(context, line.getLength());
		
		return context.getString(R.string.share_busline_qzone, line.getName(), length, size, tail);
	}
	
	/**
	 * 线路名(起点-终点)(终点-起点), 全程***, 途经**个站点. (线路站点列表) 来自老虎地图
	 * @param line
	 * @param context
	 * @return
	 */
	public static String shareBuslineSmsContent(Line line, Context context) {
        int size = line.getStationList().size();
		String tail = context.getString(R.string.share_from_tigerknows);
		String length = getPlanLength(context, line.getLength());
		StringBuilder body = new StringBuilder();
		int i = 0;
        for(Station station : line.getStationList()) {
            if (i > 0) {
                body.append(context.getString(R.string.semicolon));
            }
            body.append(i+1);
            body.append(context.getString(R.string.period));
            body.append(station.getName());
            i++;
        }
		
		return context.getString(R.string.share_busline_sms, line.getName(), length, size, body, tail);
	}
	
	/**
	 * #老虎地图分享#+Poi名称+poi地址+联系电话+标题区（按顺序展示 评分 菜系 均价 人气 口味 服务 环境）
	 * +来自@老虎地图+1个空格(字数超过140时,自动截服务环境等)
	 * @param poi
	 * @param engine
	 * @param context
	 * @return
	 */
    public static String sharePOIWeiboContent(POI poi, Context context) {
		
		if (poi == null ) {
			return "";
		}
		
		StringBuilder s = new StringBuilder();
        String str = poi.getName();
        if (!TextUtils.isEmpty(str)) {
        	// #老虎地图分享#
        	s.append(context.getString(R.string.share_weibo_poi_topic));
        	s.append(context.getString(R.string.space));
        	// Poi名称
        	s.append(str);

            // ,地址:***
            str = poi.getAddress();
            if (!TextUtils.isEmpty(str)) {
                s.append(context.getString(R.string.comma));
                s.append(context.getString(R.string.address));
                s.append(str);
            }
            
            // ,电话:****
            str = poi.getTelephone();
            if (!TextUtils.isEmpty(str)) {
                s.append(context.getString(R.string.comma));
                s.append(context.getString(R.string.phonenumber));
                s.append(str);
            }
            
            // .来自@老虎地图
//            String tail = context.getString(R.string.period) + context.getString(R.string.share_weibo_text_tail);
            String tail = context.getString(R.string.period) + context.getString(R.string.share_from_at_tigerknows) 
            		+ context.getString(R.string.space) + context.getString(R.string.share_tiger_official_website);
            int remaindCount = WeiboSend.MAX_LEN - (s.length() + tail.length());
            
            // 标题区（按顺序展示 评分 菜系 均价 人气 口味 服务 环境）
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_pingfen, 
            		String.valueOf(poi.getGrade()/2), context.getString(R.string.period));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_caixi, 
            		poi.getCookingStyle(), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_junjia, 
            		String.valueOf(poi.getPerCapity()), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_kouwei, 
            		poi.getTaste(), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_fuwu, 
            		poi.getService(), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_huanjing, 
            		poi.getEnvironment(), context.getString(R.string.comma));
            
            s.append(tail);

            return s.toString();
        }
        return "";
	}
    
    /**
     * 
     * @param poi
     * @param context
     * @return
     */
    public static String sharePOIQzoneContent(POI poi, Context context) {
		
		if (poi == null ) {
			return "";
		}
		
		StringBuilder s = new StringBuilder();
        String str = poi.getName();
        if (!TextUtils.isEmpty(str)) {
        	// Poi名称
        	s.append(str);

            // ,地址:***
            str = poi.getAddress();
            if (!TextUtils.isEmpty(str)) {
                s.append(context.getString(R.string.comma));
                s.append(context.getString(R.string.address));
                s.append(str);
            }
            
            // ,电话:****
            str = poi.getTelephone();
            if (!TextUtils.isEmpty(str)) {
                s.append(context.getString(R.string.comma));
                s.append(context.getString(R.string.phonenumber));
                s.append(str);
            }
            
            // .来自@老虎地图 #老虎地图分享#
//            String tail = context.getString(R.string.period) + context.getString(R.string.share_weibo_text_tail);
            String tail = context.getString(R.string.period) + context.getString(R.string.share_from_at_tigerknows) 
            		+ context.getString(R.string.space) + context.getString(R.string.share_weibo_poi_topic);
            int remaindCount = WeiboSend.MAX_LEN - (s.length() + tail.length());
            
            // 标题区（按顺序展示 评分 菜系 均价 人气 口味 服务 环境）
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_pingfen, 
            		String.valueOf(poi.getGrade()/2), context.getString(R.string.period));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_caixi, 
            		poi.getCookingStyle(), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_junjia, 
            		String.valueOf(poi.getPerCapity()), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_kouwei, 
            		poi.getTaste(), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_fuwu, 
            		poi.getService(), context.getString(R.string.comma));
            
            remaindCount = weiboTextAppendDescriptionWithLimit(context, remaindCount, s, R.string.share_poi_huanjing, 
            		poi.getEnvironment(), context.getString(R.string.comma));
            
            s.append(tail);

            return s.toString();
        }
        return "";
	}
	
    /**
     * POI名称, POI地址, 联系电话, 标题区(评分 菜系 均价	人气	服务	环境)+来自老虎地图
     * @param poi
     * @param context
     * @return
     */
    public static String sharePOISmsContent(POI poi, Context context) {
    	if (poi == null ) {
			return "";
		}
		
		StringBuilder s = new StringBuilder();
        String str = poi.getName();
        if (!TextUtils.isEmpty(str)) {
        	// Poi名称
        	s.append(str);

            // ,地址:***
            str = poi.getAddress();
            if (!TextUtils.isEmpty(str)) {
                s.append(context.getString(R.string.comma));
                s.append(context.getString(R.string.address));
                s.append(str);
            }
            
            // ,电话:****
            str = poi.getTelephone();
            if (!TextUtils.isEmpty(str)) {
                s.append(context.getString(R.string.comma));
                s.append(context.getString(R.string.phonenumber));
                s.append(str);
            }
            
            // 标题区（按顺序展示 评分 菜系 均价 人气 口味 服务 环境）
            weiboTextAppendDescription(context, s, R.string.share_poi_pingfen, String.valueOf(poi.getGrade()), 
            		context.getString(R.string.period));
            
            weiboTextAppendDescription(context, s, R.string.share_poi_caixi, poi.getCookingStyle(), 
            		context.getString(R.string.comma));
            
            weiboTextAppendDescription(context, s, R.string.share_poi_junjia, String.valueOf(poi.getPerCapity()), 
            		context.getString(R.string.comma));
            
            weiboTextAppendDescription(context, s, R.string.share_poi_kouwei, poi.getTaste(), 
            		context.getString(R.string.comma));
            
            weiboTextAppendDescription(context, s, R.string.share_poi_fuwu, poi.getService(), 
            		context.getString(R.string.comma));
            
            weiboTextAppendDescription(context, s, R.string.share_poi_huanjing, poi.getEnvironment(), 
            		context.getString(R.string.comma));
            
            // .来自老虎地图
            String tail = context.getString(R.string.nextline) + context.getString(R.string.share_from_tigerknows);
            s.append(tail);
            
        	return s.toString();
        }
        return "";
    }
    
    private static int weiboTextAppendDescriptionWithLimit(Context context, final int limit, StringBuilder text, final int descResId, 
    		final String append, final String prePunction) {
    	LogWrapper.d("weiboTextAppendDescriptionWithLimit", "limit:" + limit + "text:" + text + "description:" + descResId + "append:" + 
    			append + "prePunction:" +prePunction);
    	
    	int remind = limit;
    	String negativeValue = "-1";
    	if (remind <= 0 || TextUtils.isEmpty(append) || negativeValue.equals(append)) {
    		return remind;
    	}
    	
//        String str = description + context.getString(R.string.colon) + append;
        String str = context.getString(descResId, append);
        if (remind > str.length()) {
        	text.append(prePunction);
        	text.append(str);
        	remind = remind - str.length() - prePunction.length();
        	LogWrapper.d("weiboTextAppendDescriptionWithLimit", "after append: " + text + " remind: " + remind);
        }
    	
    	return remind;
    }
    
    private static void weiboTextAppendDescription(Context context, StringBuilder text, final int descResId, 
    		final String append, final String prePunction) {
    	LogWrapper.d("weiboTextAppendDescription", "text:" + text + "description:" + descResId + "append:" + 
    			append + "prePunction:" +prePunction);

    	String negativeValue = "-1";
    	if (TextUtils.isEmpty(append)|| negativeValue.equals(append)) {
    		return;
    	}
    	
//        String str = description + context.getString(R.string.colon) + append;
    	String str = context.getString(descResId, append);
    	text.append(prePunction);
    	text.append(str);
    }
    
	/*
	 * 传入全程数字, 返回
	 * 1. 全程:\u0020\u0020%1$s米
	 * 2. 全程:\u0020\u0020%1$s公里
	 */
	public static String getPlanLength(Context context, int oLength) {
		String length = "";
        if (oLength > 1000) {
            length = context.getString(R.string.length_str_km, CommonUtils.meter2kilometre(oLength));
        } else {
        	length = context.getString(R.string.length_str_m, oLength);
        }
        return length;
	}
}

package com.tigerknows.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.tigerknows.R;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;

public class NavigationSplitJointRule {

    // 用于标志该交通步骤(Step)是第一条
    private static final byte FLAG_START = 0x01;
    
    // 用于标志该交通步骤(Step)是最后一条
    private static final byte FLAG_END   = 0x00;

    // 用于计算标志值的魔数
    private static int mode = 0;
    
    // 标志是否对站点或线路名高亮显示
    private static boolean ishightLight = true;

    public static boolean isIshightLight() {
        return ishightLight;
    }

    public static void setIshightLight(boolean ishightLight) {
        NavigationSplitJointRule.ishightLight = ishightLight;
    }

    public static List<CharSequence> splitJoint(Context context, int type, Plan plan) {
        
        List<CharSequence> strList = new ArrayList<CharSequence>();
        
        mode = plan.getStepList().size();

        switch(type){
        case TrafficQuery.QUERY_TYPE_TRANSFER:
            for(int i = 0; i < mode; i++){
                Step preStep = (i > 0) ? plan.getStepList().get(i-1) : null;
                Step nextStep = (i < mode-1) ? plan.getStepList().get(i+1) : null;
                CharSequence tmp = splitJointTransfer(context, plan.getStepList().get(i), preStep, nextStep, (i+1+mode)%mode);
                if(tmp != null)
                    strList.add(tmp);
            }
            break;
        case TrafficQuery.QUERY_TYPE_DRIVE:
            for(int i = 0; i < mode; i++){
                Step nextStep = (i < mode-1) ? plan.getStepList().get(i+1) : null;
                CharSequence tmp = splitJointDrive(plan, context, plan.getStepList().get(i), nextStep, (i+1+mode)%mode);
                if(tmp != null){
                    strList.add(tmp);
                }
                    
            }
            break;
        case TrafficQuery.QUERY_TYPE_WALK:
            for(int i = 0; i < mode; i++){
                Step nextStep = (i < mode-1) ? plan.getStepList().get(i+1) : null;
                CharSequence tmp = splitJointWalk(plan, context, plan.getStepList().get(i), nextStep, (i+1+mode)%mode);
                if(tmp != null)
                    strList.add(tmp);
            }
            break;
        default:
                
        }
        
        return strList;
    }
    
    private static CharSequence splitJointTransfer(Context context, Step step, Step previousStep, Step nextStep, final int flag) {
        
    	if (step == null) {
    		return "";
    	}
    	
    	final int DONOTSHOWLENGTHLIMIT = 10;
    	
        String str = null;
        
        if (Step.TYPE_WALK == step.getType()) {
            if (flag != FLAG_END) {
                if (previousStep != null && previousStep.getType() == Step.TYPE_TRANSFER && nextStep != null && nextStep.getType() == Step.TYPE_TRANSFER) {
//                    if (step.getDistance() > Plan.TRANSFER_WALK_MIN_DISTANCE) {
                        str = context.getString(R.string.traffic_at_where_walk_how_to_where, previousStep.getTransferDownStopName(), step.getDistance(), nextStep.getTransferUpStopName());                                   
//                    }
                } else if (nextStep != null && nextStep.getType() == Step.TYPE_TRANSFER) {
//                    if (step.getDistance() > Plan.TRANSFER_WALK_MIN_DISTANCE) {
                        if (flag == FLAG_START) {
                            str = context.getString(R.string.traffic_at_where_walk_how_to_where, context.getString(R.string.start), step.getDistance(), nextStep.getTransferUpStopName());
                        } else {
                            str = context.getString(R.string.traffic_walk_how_to_where, step.getDistance(), nextStep.getTransferUpStopName());
                        }
//                    }
                } else if (previousStep != null && previousStep.getType() == Step.TYPE_TRANSFER) {
//                    if (step.getDistance() > Plan.TRANSFER_WALK_MIN_DISTANCE) {
                        str = context.getString(R.string.traffic_at_where_walk_how, previousStep.getTransferDownStopName(), step.getDistance());
//                    }
                }
                
                if (flag == FLAG_START && TextUtils.isEmpty(str)) {
                    str = context.getString(R.string.set_off_from_start);
                }
                
            } else {
                
                if (previousStep != null && previousStep.getType() == Step.TYPE_TRANSFER) {
//                    if (step.getDistance() > Plan.TRANSFER_WALK_MIN_DISTANCE) {
                	if (step.getDistance() > DONOTSHOWLENGTHLIMIT) {
                        str = context.getString(R.string.traffic_at_where_walk_how_to_where, previousStep.getTransferDownStopName(), step.getDistance(), context.getString(R.string.end));
                    } else {
                        str = context.getString(R.string.traffic_at_where_walk_to_where, previousStep.getTransferDownStopName(), context.getString(R.string.end));
                    }
                } else {
//                    if (step.getDistance() > Plan.TRANSFER_WALK_MIN_DISTANCE) {
                	if (step.getDistance() > DONOTSHOWLENGTHLIMIT) {
                        str = context.getString(R.string.traffic_walk_how_to_where, step.getDistance(), context.getString(R.string.end));
                    } else {
                        str = context.getString(R.string.traffic_walk_to_where, context.getString(R.string.end));
                    }
                }
            }
            
        } else if (Step.TYPE_TRANSFER == step.getType()) {
            int stopNumber = Math.abs(step.getTransferStopNumber()+1);
            if(stopNumber >= 2)
                str = context.getString(R.string.traffic_at_where_by_what_to_where_previous, step.getTransferUpStopName(), step.getTransferLineName(), stopNumber
                    , step.getTransferDownStopName(), step.getTransferPreviousStopName());
            else
                str = context.getString(R.string.traffic_at_where_by_what_to_where, step.getTransferUpStopName(), step.getTransferLineName(), stopNumber
                        , step.getTransferDownStopName());
        } 

        return transferResultRender(context, step, previousStep, nextStep, str);
    }
    
    private static CharSequence splitJointDrive(Plan plan, Context context, Step step, Step nextStep, int flag) {
        
    	if (step == null) {
    		return "";
    	}
    	
        if(step.getDistance() <= 0){
            return null;
        }
        
        StringBuilder s = new StringBuilder();
        // if(交通步骤第1条) 本条.msg += "从起点向" + 出发方向 + "方向出发，"
        if (flag == FLAG_START) {
        	if (!TextUtils.isEmpty(plan.getStartOutOrientation())) {
        		s.append(context.getString(R.string.begin_trun_to, plan.getStartOutOrientation()));
        	} else {
        		s.append(context.getString(R.string.set_off_from_start));
        	}
        }

        // if(!当前道路名称.trim().equals(""))
        if (!TextUtils.isEmpty(step.getDriveRoadName()) && !step.getDriveRoadName().trim().equals("")) {
            // 本条.msg += "沿" + 当前道路名称 + "行驶" + 本条.距离（做一定处理） + "，"
            s.append(context.getString(R.string.drive_along, step.getDriveRoadName(), CommonUtils.distanceToString(step.getDistance())));
            // else if(本条.是否环岛 == 1)
        } else if (step.getDriveWhetherRoundabout() == 1) {
            // 本条.msg += "沿环岛行驶" + 本条.距离（做一定处理） + "，"
            s.append(context.getString(R.string.drive_along_roundabout, CommonUtils.distanceToString(step.getDistance())));
            // else
        } else {
            // 本条.msg += "行驶" + 本条.距离（做一定处理） + "，"
            s.append(context.getString(R.string.drive_run, CommonUtils.distanceToString(step.getDistance())));
        }

        // if(本条.转向信息.equals("前"))
        if (context.getString(R.string.traffic_forward).equals(step.getDriveTurnTo())) {
            // 本条.msg += "直行"
            s.append(context.getString(R.string.drive_forward));
            // else if(本条.转向信息.equals("后"))
        } else if (context.getString(R.string.traffic_back).equals(step.getDriveTurnTo())) {
            // 本条.msg += "后转"
            s.append(context.getString(R.string.drive_back));
            // else if(本条.转向信息.equals("左"))
        } else if (context.getString(R.string.traffic_left).equals(step.getDriveTurnTo())) {
            // 本条.msg += "左转"
            s.append(context.getString(R.string.drive_left));
            // else
        } else if (context.getString(R.string.traffic_right).equals(step.getDriveTurnTo())) {
            // 本条.msg += "右转"
            s.append(context.getString(R.string.drive_right));
        } else if (!TextUtils.isEmpty(step.getDriveTurnTo())){
            // 旧版本转换过来的数据
            s.append(step.getDriveTurnTo());
        }
      
        if (flag != FLAG_START && flag != FLAG_END) {
            // if(!下条.当前道路名称.trim().equals(""))
            if (nextStep != null && !TextUtils.isEmpty(nextStep.getDriveRoadName().trim()) && !step.getDriveRoadName().trim().equals("")) {
                // 本条.msg += "进入" + 下条.当前道路名称
                s.append(context.getString(R.string.enter, nextStep.getDriveRoadName()));
                // else if(本条.是否环岛 == 1)
            } else if (nextStep != null && nextStep.getDriveWhetherRoundabout() == 1) {
                // 本条.msg += "进入环岛"
                s.append(context.getString(R.string.enter_roundabout));
                // else
            } else {
                // 本条.msg += ""
            }
        }
        // if(交通步骤最后1条) 本条.msg += "到达终点"
        if (flag == FLAG_END) {
            s.append(context.getString(R.string.traffic_goto_end_point));
        }

        return driveResultRender(context, step, nextStep, s.toString());
    }

    private static CharSequence splitJointWalk(Plan plan, Context context, Step step, Step nextStep, int flag) {

    	if (step == null) {
    		return "";
    	}
    	
        if(step.getDistance() <= 0){
            return null;
        }
        
        StringBuilder s = new StringBuilder();
//      s.append((index+1) + ". ");
      // if(交通步骤第1条) 本条.msg += "从起点向" + 出发方向 + "方向出发，" 
      if (flag == FLAG_START) {
          if (!TextUtils.isEmpty(plan.getStartOutOrientation())) {
        	  s.append(context.getString(R.string.begin_trun_to, plan.getStartOutOrientation()));
	      	} else {
	      		s.append(context.getString(R.string.set_off_from_start));
	      }
      }
      // if(!当前道路名称.trim().equals(""))
      if (!TextUtils.isEmpty(step.getWalkRoadName().trim()) && !step.getWalkRoadName().trim().equals("")) {
          // 本条.msg += "沿" + 当前道路名称 + "走" + 本条.距离（做一定处理） + "，"
          s.append(context.getString(R.string.walk_along, step.getWalkRoadName(), CommonUtils.distanceToString(step.getDistance())));
          // else if(本条.是否环岛 == 1)
      } else if (step.getWalkWhetherRoundabout() == 1) {
          // 本条.msg += "沿环岛走" + 本条.距离（做一定处理） + "，"
          s.append(context.getString(R.string.walk_along_roundabout, CommonUtils.distanceToString(step.getDistance())));
          // else
      } else {
          // 本条.msg += "走" + 本条.距离（做一定处理） + "，"
          s.append(context.getString(R.string.walk_run, CommonUtils.distanceToString(step.getDistance())));
      }

      // if(本条.转向信息.equals("前"))
      if (context.getString(R.string.traffic_forward).equals(step.getWalkTurnTo())) {
          // 本条.msg += "直行"
          s.append(context.getString(R.string.walk_forward));
          // else if(本条.转向信息.equals("后"))
      } else if (context.getString(R.string.traffic_back).equals(step.getWalkTurnTo())) {
          // 本条.msg += "后转"
          s.append(context.getString(R.string.walk_back));
          // else if(本条.转向信息.equals("左"))
      } else if (context.getString(R.string.traffic_left).equals(step.getWalkTurnTo())) {
          // 本条.msg += "左转"
          s.append(context.getString(R.string.walk_left));
          // else
      } else if (context.getString(R.string.traffic_right).equals(step.getWalkTurnTo())) {
          // 本条.msg += "右转"
          s.append(context.getString(R.string.walk_right));
      } else if (!TextUtils.isEmpty(step.getWalkTurnTo())){
          // 旧版本转换过来的数据
          s.append(step.getWalkTurnTo());
      }
      
      // if(本条.附加人行信息 != 0)
      if (step.getWalkPublicNudity() != 0) {
          // 本条.msg += 步行信息(通过附加人行信息生成)
          int[] types = context.getResources().getIntArray(R.array.walk_public_nudity_type);
            String[] values = context.getResources().getStringArray(R.array.walk_public_nudity_value);
            
            for(int i = 0 ; i < types.length; i++){
                if(step.getWalkPublicNudity() == types[i])
//                  s.append("经过" + values[i]);
                	s.append(context.getString(R.string.accross, values[i]));
            }

//          s.append("经过" + step.getWalkPublicNudity());
      }

      if (flag != FLAG_START && flag != FLAG_END) {
          // if(!下条.当前道路名称.trim().equals(""))
          if (nextStep != null && !TextUtils.isEmpty(nextStep.getWalkRoadName().trim()) && !step.getWalkRoadName().trim().equals("")) {
              // 本条.msg += "进入" + 下条.当前道路名称
              s.append(context.getString(R.string.enter, nextStep.getWalkRoadName()));
              // else if(本条.是否环岛 == 1)
          } else if (nextStep != null && nextStep.getWalkWhetherRoundabout() == 1) {
              // 本条.msg += "进入环岛"
              s.append(context.getString(R.string.enter_roundabout));
              // else
          } else {
              // 本条.msg += ""
          }
      }
      // if(交通步骤最后1条) 本条.msg += "到达终点" 
      if (flag == FLAG_END) {
          s.append(context.getString(R.string.traffic_goto_end_point));
      }
      
      return walkResultRender(context, step, nextStep, s.toString());
    }

    private static CharSequence transferResultRender(Context context, Step step, Step previousStep, Step nextStep, String content){
        
        // 站点名称在字符串的位置
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        
        if(ishightLight){
            if(previousStep != null){
                markHightlightInterval(content, previousStep.getTransferDownStopName(), indexList);
            }
            if(nextStep != null){
                markHightlightInterval(content, nextStep.getTransferUpStopName(), indexList);
            }       
            if(step != null){
                markHightlightInterval(content, step.getTransferUpStopName(), indexList);
                markHightlightInterval(content, step.getTransferDownStopName(), indexList);
            }
            if(!TextUtils.isEmpty(content)){
                markHightlightInterval(content, context.getString(R.string.start), indexList);
                markHightlightInterval(content, context.getString(R.string.end), indexList);
            }
            
            if(!TextUtils.isEmpty(content)){
                return hightLight(context, content, indexList);
            }
        }

        return content;
    }

    private static CharSequence driveResultRender(Context context, Step step, Step nextStep, String content){
        
        // 路线名在字符串的位置
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        
        if(ishightLight){
            if(step != null){
                markHightlightInterval(content.toString(), step.getDriveRoadName(), indexList);
            }
            if(nextStep != null){
                markHightlightInterval(content.toString(), nextStep.getDriveRoadName(), indexList);
            }
            if(!TextUtils.isEmpty(content)){
                markHightlightInterval(content.toString(), context.getString(R.string.start), indexList);
                markHightlightInterval(content.toString(), context.getString(R.string.end), indexList);
            }
            
            if(!TextUtils.isEmpty(content)) {
                return hightLight(context, content.toString(), indexList);
            }
        }

        return content;
    }
    
    private static CharSequence walkResultRender(Context context, Step step, Step nextStep, String content){
        
        // 路线名在字符串的位置
        ArrayList<Integer> indexList = new ArrayList<Integer>();
        
        if(ishightLight){
            if(step != null){
                markHightlightInterval(content.toString(), step.getWalkRoadName(), indexList);
            }
            if(nextStep != null){
                markHightlightInterval(content.toString(), nextStep.getWalkRoadName(), indexList);
            }
            if(!TextUtils.isEmpty(content)){
                markHightlightInterval(content.toString(), context.getString(R.string.start), indexList);
                markHightlightInterval(content.toString(), context.getString(R.string.end), indexList);
            }
            
            if(!TextUtils.isEmpty(content)) {
                return hightLight(context, content.toString(), indexList);
            }
        }

        return content;
    }
    
    private static void markHightlightInterval(String str, String subStr, ArrayList<Integer> indexList){
        if(TextUtils.isEmpty(str) || TextUtils.isEmpty(subStr) || indexList == null)
            return;
        
        int start = str.indexOf(subStr);
        int end = start + subStr.length();
        
        if(start >= 0){
            indexList.add(start);
            indexList.add(end);
        }
    }
    
    private static CharSequence hightLight(Context context, String text, ArrayList<Integer>indexs){
        if (TextUtils.isEmpty(text) || indexs == null || indexs.size() < 2) {
        	return text;
        }
    	
        SpannableStringBuilder style=new SpannableStringBuilder(text);
        
        for(int i = 0 ; i < indexs.size()-1; i+=2){
            style.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.text_forground_blue)),indexs.get(i),indexs.get(i+1),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        }
        
        return style;
    }
    
}

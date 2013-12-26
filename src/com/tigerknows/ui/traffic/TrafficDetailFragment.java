/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.widget.Toast;

import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.util.NavigationSplitJointRule;

public class TrafficDetailFragment extends BaseFragment implements View.OnClickListener{
    
    public TrafficDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private static final int SHOW_TYPE_TRANSFER = Plan.Step.TYPE_TRANSFER;
    
    private static final int SHOW_TYPE_DRVIE = Plan.Step.TYPE_DRIVE;
    
    private static final int SHOW_TYPE_WALK = Plan.Step.TYPE_WALK;
    
	private static final int TYPE_RESULT_LIST_START = 6;

	private static final int TYPE_RESULT_LIST_END = 7;
	
    private StringListAdapter mResultAdapter;

    private List<Integer> mTypes = new ArrayList<Integer>();
    
    private List<CharSequence> mStrList = new ArrayList<CharSequence>();
    
    private TextView mSubTitleTxv = null;
    
    private TextView mLengthTxv = null;
    
    private ListView mResultLsv = null;
    
    private ViewGroup mFavorateBtn = null;
    
    private ViewGroup mShareBtn = null;
    
    private ViewGroup mErrorRecoveryBtn = null;
    
    private int mShowType = -1;

    private Plan plan = null;
    
    private LinearLayout mBottomButtonsView;
    
    private int mChildLayoutId = R.layout.traffic_child_traffic;
    
    private List<Plan> mPlanList = new ArrayList<TrafficModel.Plan>();
    
    private int curLineNum = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_result, container, false);
        findViews();
        setListener();
        
        mFavorateBtn.setOnClickListener(new ResultOnClickListener());
        mShareBtn.setOnClickListener(new ResultOnClickListener());
        mErrorRecoveryBtn.setOnClickListener(new ResultOnClickListener());
        
        mResultAdapter = new StringListAdapter(mContext);
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        switch(mShowType) {
            case SHOW_TYPE_TRANSFER:
                mRightBtn.setText(R.string.map);
                mRightBtn.setOnClickListener(this);
                mTitleBtn.setText(getString(R.string.title_transfer_plan));
                mErrorRecoveryBtn.setVisibility(View.VISIBLE);
                mBottomButtonsView.setWeightSum(3);
            	mSubTitleTxv.setText(this.plan.getTitle(mSphinx));
            	mLengthTxv.setVisibility(View.VISIBLE);
            	mLengthTxv.setText(plan.getLengthStr(mSphinx));
                break;
            case SHOW_TYPE_DRVIE:
                mTitleBtn.setText(getString(R.string.title_drive_plan));
                mSubTitleTxv.setText(getString(R.string.length_str_title, plan.getLengthStr(mSphinx)));
                mErrorRecoveryBtn.setVisibility(View.GONE);
                mBottomButtonsView.setWeightSum(2);
            	mLengthTxv.setVisibility(View.GONE);
                break;
            case SHOW_TYPE_WALK:
                mTitleBtn.setText(getString(R.string.title_walk_plan));
                mSubTitleTxv.setText(getString(R.string.length_str_title, plan.getLengthStr(mSphinx)));
                mErrorRecoveryBtn.setVisibility(View.GONE);
                mBottomButtonsView.setWeightSum(2);
            	mLengthTxv.setVisibility(View.GONE);
                break;
            default:
        }
        
        if (mPlanList.size() > 0) {
            //有内容，需要弹出顶部切换菜单
            mTitleBtn.setText(getString(R.string.title_transfer_plan_popup, TrafficQuery.numToStr(mSphinx, curLineNum + 1)));
        }
        
        Utility.setFavoriteBtn(mSphinx, mFavorateBtn, plan.checkFavorite(mContext));

        if (mDismissed) {
            setSelectionFromTop();
        }
    }
    
    void setSelectionFromTop() {
      //TODO: 这 里为什么要用posDelayed来调用setSelectionFromTop
        mSphinx.getHandler().postDelayed(new Runnable() {
            
            @Override
            public void run() {
                mResultLsv.setSelectionFromTop(0, 0);
            }
        }, 200);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
   
    protected void findViews() {
    	mSubTitleTxv = (TextView)mRootView.findViewById(R.id.subtitle_txv);
        mLengthTxv = (TextView)mRootView.findViewById(R.id.length_txv);
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mRootView.findViewById(R.id.traffic_detail_sub_title).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.traffic_result_sub_title).setVisibility(View.GONE);
        mBottomButtonsView = (LinearLayout) mRootView.findViewById(R.id.bottom_buttons_view);
        mErrorRecoveryBtn = (ViewGroup) mRootView.findViewById(R.id.error_recovery_btn);
        mFavorateBtn = (ViewGroup) mRootView.findViewById(R.id.favorite_btn);
        mShareBtn = (ViewGroup) mRootView.findViewById(R.id.share_btn);
        mBottomButtonsView.findViewById(R.id.nearby_search_btn).setVisibility(View.GONE);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        	@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);

				// 绘制交通图层
                viewMap();
                // 将地图平移到某一item index, 并缩放至某一级别
            	TrafficOverlayHelper.panToPosition(mSphinx, position, mSphinx.getMapView());
            }
        });
    }
    
	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == R.id.right_btn) {
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
			// 绘制交通图层
			viewMap();
			// 将地图缩放至可以显示完整的交通路径, 并平移到交通路径中心点
			TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
		}
	}
	
    public void setData(Plan plan) {
        List<Plan> list = new ArrayList<TrafficModel.Plan>();
        list.add(plan);
        setData(list, 0);
    }
    
    public void setData(List<Plan> list, int curLine) {
    	
    	if (list == null || list.isEmpty() || curLine >= list.size())
    		return;

        plan = list.get(0);
        mShowType = plan.getType();
        curLineNum = curLine;

        mPlanList.clear();
        this.mPlanList.addAll(list);

        mStrList.clear();
        mTypes.clear();
        
        mStrList.addAll(NavigationSplitJointRule.splitJoint(mSphinx, plan));
        mTypes.addAll((ArrayList<Integer>)plan.getStepTypeList(mContext));

        //列表前面显示起点，后面显示终点
        mStrList.add(0, plan.getStart().getName());
        mTypes.add(0, TYPE_RESULT_LIST_START);
        mStrList.add(plan.getEnd().getName());
        mTypes.add(TYPE_RESULT_LIST_END);
        mResultAdapter.notifyDataSetChanged();

        plan.updateHistory(mContext);
    }

    public static class StepViewHolder {
    	public ImageView image;
    	public TextView textView;
    }

    class StringListAdapter extends BaseAdapter{

        public StringListAdapter(Context context) {
        	super();
        }

        @Override
        public int getCount() {
            return mStrList.size();
        }

        private CharSequence getItemContent(int position) {
            return mStrList.get(position);
        }

		@Override
        public View getView(final int position, View convertView, ViewGroup parent) {

			if(convertView == null) {
				convertView = mLayoutInflater.inflate(mChildLayoutId, parent, false);
				StepViewHolder stepHolder = new StepViewHolder();
				stepHolder.image = (ImageView)convertView.findViewById(R.id.image1);
				stepHolder.textView = (TextView)convertView.findViewById(R.id.text);
				convertView.setTag(stepHolder);
			}

			StepViewHolder stepHolder = (StepViewHolder)convertView.getTag();
			
			Integer stepType = mTypes.get(position);
            stepHolder.image.setBackgroundDrawable(getDrawable(stepType));
			stepHolder.textView.setText(getItemContent(position));
			stepHolder.textView.setTextColor(Color.parseColor("#000000"));
			//convertView.setBackgroundResource(R.drawable.btn_traffic_detail_end_normal);
				
			return convertView;

        }
        
        private Drawable getDrawable(int steptype){
            if (mShowType == SHOW_TYPE_TRANSFER) {
            	return getTransferDrawable(steptype);
            } else {
            	return getDirectionDrawable(steptype);
            }
        }
        
        private Drawable getTransferDrawable(int steptype) {
        	int res = R.drawable.icon_marker_bus;
            
            switch(steptype){
            case TrafficModel.Plan.Step.TYPE_TRANSFER:
                res = R.drawable.icon_marker_bus;
                break;
            case TrafficModel.Plan.Step.TYPE_WALK:
                res = R.drawable.icon_marker_walk;
                break;
			case TYPE_RESULT_LIST_START:
				res = R.drawable.icon_marker_start;
				break;
            case TYPE_RESULT_LIST_END:
            	res = R.drawable.icon_marker_end;
            	break;
            default:
                
            }
            return mContext.getResources().getDrawable(res);
        }
        
        private Drawable getDirectionDrawable(int steptype) {
        	int res = R.drawable.icon_marker_drive_forward;
            
            switch(steptype){
            
            case TrafficModel.Plan.Step.TYPE_FORWARD:
                res = R.drawable.icon_marker_drive_forward;
                break;
            case TrafficModel.Plan.Step.TYPE_BACK:
                res = R.drawable.icon_marker_drive_back;
                break;
            case TrafficModel.Plan.Step.TYPE_ROUND_LEFT:
                res = R.drawable.icon_marker_drive_round_left;
                break;
            case TrafficModel.Plan.Step.TYPE_LEFT:
                res = R.drawable.icon_marker_drive_left;
                break;
            case TrafficModel.Plan.Step.TYPE_ROUND_RIGHT:
                res = R.drawable.icon_marker_drive_round_right;
                break;
            case TrafficModel.Plan.Step.TYPE_RIGHT:
                res = R.drawable.icon_marker_drive_right;
                break;
            case TYPE_RESULT_LIST_START:
				res = R.drawable.icon_marker_start;
				break;
            case TYPE_RESULT_LIST_END:
            	res = R.drawable.icon_marker_end;
            	break;
            default:
                
            }
            return mContext.getResources().getDrawable(res);
        }

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
    }

    private class ResultOnClickListener implements View.OnClickListener {
    	
		@Override
		public void onClick(View v) {

            if (v.getId() == R.id.share_btn) {
                //弹出分享对话框
                mActionLog.addAction(mActionTag +  ActionLog.CommonShare);
                share(plan);
            } else if (v.getId() == R.id.favorite_btn) {
            	favorite(plan, v);
            } else if (v.getId() == R.id.error_recovery_btn) {
                mActionLog.addAction(mActionTag +  ActionLog.CommonErrorRecovery);
            	TrafficReportErrorActivity.addTarget(plan);
            	mSphinx.showView(R.id.activity_traffic_report_error);
            }
		}

		public void favorite(final BaseData data, final View v) {
			if (data == null)
				return ;
			
	        boolean isFavorite = data.checkFavorite(mContext);
            mActionLog.addAction(mActionTag +  ActionLog.CommonFavorite, String.valueOf(isFavorite));
	        if (isFavorite) {
	        	Utility.showNormalDialog(mSphinx, 
                        getString(R.string.prompt),
                        getString(R.string.cancel_favorite_tip),
                        getString(R.string.yes),
                        getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                if (id == DialogInterface.BUTTON_POSITIVE) {
                                    Utility.setFavoriteBtn(mSphinx, mFavorateBtn, false);
                                    data.deleteFavorite(mContext);
                                }
                            }
                        });
	        } else {
	            Utility.setFavoriteBtn(mSphinx, mFavorateBtn, true);
				data.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
                Toast.makeText(mSphinx, R.string.favorite_toast, Toast.LENGTH_LONG).show();
	        }
	    }
    }
    
    public void share(final Plan plan) {
        if(plan == null)
            return;
    	
    	MapScene mapScene = mSphinx.getMapView().getCurrentMapScene();
    	TrafficOverlayHelper.drawOverlay(mSphinx, mSphinx.getMapView(), plan);
    	Position position = TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
    	
    	ShareAPI.share(mSphinx, plan, position, mapScene, mActionTag);
    }
    
    public void viewMap() {

        if (plan != null) {
            String actionTag = "";
        	switch(mShowType) {
        	case SHOW_TYPE_TRANSFER:
        	    actionTag = ActionLog.TrafficTransferMap;
        		break;
        	case SHOW_TYPE_DRVIE:
                actionTag = ActionLog.TrafficDriveMap;
        		break;
        	case SHOW_TYPE_WALK:
                actionTag = ActionLog.TrafficWalkMap;
        		break;
        	default:
        			
        	}

        	
            mSphinx.getResultMapFragment().setData(getString(R.string.title_traffic_result_map), actionTag);
            String resultMapActionTag = mSphinx.getResultMapFragment().mActionTag;
            if (mSphinx.uiStackContains(R.id.view_result_map)
                    && (actionTag.equals(resultMapActionTag)
                            || actionTag.equals(resultMapActionTag)
                            || actionTag.equals(resultMapActionTag))) {
                dismiss();
                TrafficOverlayHelper.drawOverlay(mSphinx, mSphinx.getMapView(), plan);
            } else {
                ItemizedOverlayHelper.drawPlanListOverlay(mSphinx, mPlanList, curLineNum);
                mSphinx.showView(R.id.view_result_map);
            }
            
        }
    }
    
    public void viewPlanMap(){
        viewMap();
        mSphinx.getResultMapFragment().onResume();
        TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
    }

    public Plan getData() {
        return plan;
    }
    
    public int getCurLine(){
        return curLineNum;
    }
}

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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
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
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.PlanTag;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.common.ResultMapFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.util.NavigationSplitJointRule;

public class TrafficDetailFragment extends BaseFragment implements View.OnClickListener{
    
    public TrafficDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private static final int TYPE_RESULT_LIST_START = 6;

    private static final int TYPE_RESULT_LIST_END = 7;
    
    private StringListAdapter mResultAdapter;

    private List<Integer> mTypes = new ArrayList<Integer>();
    
    private List<CharSequence> mStrList = new ArrayList<CharSequence>();
    
    private ListView mResultLsv = null;
    
    private ViewGroup mFavorateBtn = null;
    
    private ViewGroup mShareBtn = null;
    
    private ViewGroup mErrorRecoveryBtn = null;
    
    private int mType = -1;

    private Plan plan = null;
    
    private LinearLayout mBottomButtonsView;
    
    private View mViewPagerLayout;
    
    private ViewPager mViewPager;
    
    private int mChildLayoutId = R.layout.traffic_child_traffic;
    
    private List<Plan> mPlanList = new ArrayList<TrafficModel.Plan>();
    
    private List<Plan> mTransferPlanList = new ArrayList<TrafficModel.Plan>();
    
    private List<Plan> mDrivePlanList = new ArrayList<TrafficModel.Plan>();
    
    private List<Plan> mWalkPlanList = new ArrayList<TrafficModel.Plan>();
    
    private int mIndex = -1;
    
    private TrafficQuery mTrafficQuery;
    
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
        
        switch(mType) {
            case Plan.Step.TYPE_TRANSFER:
                mTitleBtn.setText(getString(R.string.title_transfer_plan));
                mRightBtn.setText(R.string.map);
                mRightBtn.setOnClickListener(this);
                mRightBtn.setVisibility(View.VISIBLE);
                mErrorRecoveryBtn.setVisibility(View.VISIBLE);
                mBottomButtonsView.setWeightSum(3);
                break;
            case Plan.Step.TYPE_DRIVE:
                mTitleBtn.setText(getString(R.string.title_drive_plan));
                mRightBtn.setVisibility(View.GONE);
                mErrorRecoveryBtn.setVisibility(View.GONE);
                mBottomButtonsView.setWeightSum(2);
                break;
            case Plan.Step.TYPE_WALK:
                mTitleBtn.setText(getString(R.string.title_walk_plan));
                mRightBtn.setVisibility(View.GONE);
                mErrorRecoveryBtn.setVisibility(View.GONE);
                mBottomButtonsView.setWeightSum(2);
                break;
            default:
        }
        
        if (mPlanList.size() > 0) {
            //有内容，需要弹出顶部切换菜单
//            mTitleBtn.setText(getString(R.string.title_transfer_plan_popup, TrafficQuery.numToStr(mSphinx, curLineNum + 1)));
            mViewPagerLayout.setVisibility(View.VISIBLE);
//            Utility.pageIndicatorInit(mSphinx, mPageIndicatorView, mPagecount, 0, R.drawable.ic_notice_dot_normal, R.drawable.ic_notice_dot_selected);
//            mNoticeRly.setVisibility(View.VISIBLE);
//            mViewPager.setCurrentItem(mPagecount * VIEW_PAGE_LEFT);
//            mPosition = 0;
        } else {
            mViewPagerLayout.setVisibility(View.GONE);
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
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mBottomButtonsView = (LinearLayout) mRootView.findViewById(R.id.bottom_buttons_view);
        mErrorRecoveryBtn = (ViewGroup) mRootView.findViewById(R.id.error_recovery_btn);
        mFavorateBtn = (ViewGroup) mRootView.findViewById(R.id.favorite_btn);
        mShareBtn = (ViewGroup) mRootView.findViewById(R.id.share_btn);
        mBottomButtonsView.findViewById(R.id.nearby_search_btn).setVisibility(View.GONE);
        mViewPagerLayout = mRootView.findViewById(R.id.traffic_rly);
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);

                // 绘制交通图层
                viewMap(false);
                // 将地图平移到某一item index, 并缩放至某一级别
                TrafficOverlayHelper.panToPosition(mSphinx, position, mSphinx.getMapView());
            }
        });
        
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onPageSelected(int arg0) {
                updateResult(mPlanList.get(arg0));
            } });
    }
    
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.right_btn) {
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
            viewMap(true);
        }
    }
    
    public void setData(TrafficQuery trafficQuery, List<Plan> transferList, List<Plan> driveList, List<Plan> walkList, int type, int index) {

        mTrafficQuery = trafficQuery;
        
        if (mTransferPlanList != transferList) {
            mTransferPlanList.clear();
            if (transferList != null) {
                mTransferPlanList.addAll(transferList);
            }
        }
        if (mDrivePlanList != driveList) {
            mDrivePlanList.clear();
            if (driveList != null) {
                mDrivePlanList.addAll(driveList);
            }
        }
        if (mWalkPlanList != walkList) {
            mWalkPlanList.clear();
            if (walkList != null) {
                mWalkPlanList.addAll(walkList);
            }
        }
        
        mType = type;
        
        mPlanList.clear();
        if (type == Plan.Step.TYPE_TRANSFER) {
            mPlanList.addAll(mTransferPlanList);
        } else if (type == Plan.Step.TYPE_DRIVE) {
            mPlanList.addAll(mDrivePlanList);
        } else if (type == Plan.Step.TYPE_WALK) {
            mPlanList.addAll(mWalkPlanList);
        }
        
        mIndex = index;
        plan = mPlanList.get(mIndex);

        updateResult(plan);

        plan.updateHistory(mContext);
    }
    
    private void updateResult(Plan plan) {
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
    }

    public static class StepViewHolder {
        public ImageView image;
        public TextView textView;
    }

    class PlanListAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mPlanList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            // TODO Auto-generated method stub
            return arg0 == arg1;
        }
        
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
            if (mType == Plan.Step.TYPE_TRANSFER) {
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
        TrafficOverlayHelper.drawOverlay(mSphinx, plan);
        Position position = TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
        
        ShareAPI.share(mSphinx, plan, position, mapScene, mActionTag);
    }
    
    public void viewMap() {
        viewMap(false);
    }
    
    private void viewMap(boolean showList) {
        
        if (plan != null) {
            String title = null;
            String actionTag = "";
            switch (mType) {
                case Plan.Step.TYPE_TRANSFER:
                    if (showList) {
                        actionTag = ActionLog.TrafficTransferListMap;
                    } else {
                        actionTag = ActionLog.TrafficTransferMap;
                        title = getString(R.string.transfer_map);
                    }
                    break;
                case Plan.Step.TYPE_DRIVE:
                    actionTag = ActionLog.TrafficDriveMap;
                    break;
                case Plan.Step.TYPE_WALK:
                    actionTag = ActionLog.TrafficWalkMap;
                    break;
                default:

            }

            ResultMapFragment resultMapFragment = mSphinx.getResultMapFragment();
            resultMapFragment.setData(title, actionTag);
            mSphinx.showView(R.id.view_result_map);
            
            if (ActionLog.TrafficTransferListMap.equals(actionTag)) {
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, mPlanList, mIndex);
            } else {
                TrafficOverlayHelper.drawOverlay(mSphinx, plan);
                TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), mSphinx);
            }
            
        }
    }

    public List<Plan> getTransferPlanList() {
        return mTransferPlanList;
    }

    public List<Plan> getDrivePlanList() {
        return mDrivePlanList;
    }

    public List<Plan> getWalkPlanList() {
        return mWalkPlanList;
    }
    
    public TrafficQuery getTrafficQuery() {
        return mTrafficQuery;
    }

    public static class PlanViewHolder {
        public TextView title;
        public TextView txv1;
        public TextView txv4;
        public TextView txv3;
        public TextView txv2;
        public LinearLayout tags;
        public int position;
    }
    
    public static class TrafficDetailItem {
        View v;
        Plan plan;
        int resid = R.layout.traffic_group_traffic;
        PlanViewHolder planHolder;
        Sphinx mSphinx;
        private static final int MAX_TAG_NUM = 3;
        
        private static final int tagResList[] = {0, 
            R.drawable.bg_transfer_tag1,
            R.drawable.bg_transfer_tag2,
            R.drawable.bg_transfer_tag3,
            R.drawable.bg_transfer_tag4,
            R.drawable.bg_transfer_tag5,
            R.drawable.bg_transfer_tag6,
            R.drawable.bg_transfer_tag7
            }; 
        
        public TrafficDetailItem(Sphinx sphinx) {
            v = sphinx.getLayoutInflater().inflate(resid, null);
            mSphinx = sphinx;
            planHolder = new PlanViewHolder();
            planHolder.title = (TextView)v.findViewById(R.id.title_txv);
            planHolder.txv1 = (TextView)v.findViewById(R.id.txv1);
            planHolder.txv2 = (TextView) v.findViewById(R.id.txv2);
            planHolder.txv3 = (TextView) v.findViewById(R.id.txv3);
            planHolder.txv4 = (TextView) v.findViewById(R.id.txv4);
            planHolder.tags = (LinearLayout) v.findViewById(R.id.tags_view);
        }
        
        public void refresh(Plan p) {
            this.plan = p;
            planHolder.title.setText(plan.getTitle(mSphinx));
            if (p.getType() == TrafficQuery.QUERY_TYPE_TRANSFER) {
                planHolder.txv1.setText(plan.getExpectedBusTime());
                planHolder.txv2.setText(plan.getLengthStr(mSphinx));
                planHolder.txv3.setText(plan.getWalkDistance());
                planHolder.txv4.setText(plan.getBusstopNum());
            } else if (p.getType() == TrafficQuery.QUERY_TYPE_DRIVE) {
                planHolder.txv1.setText(plan.getDriveDistance());
                planHolder.txv2.setText(plan.getExpectedDriveTime());
                planHolder.txv3.setText(plan.getTrafficLightNum());
                planHolder.txv4.setText(plan.getTaxiCost());
            }
            planHolder.tags.removeAllViews();
            List<PlanTag> list = plan.getPlanTagList();
            if (list != null) {
                int tagNum = Math.min(MAX_TAG_NUM, plan.getPlanTagList().size());
                for (int i = 0; i < tagNum; i++) {
                    PlanTag tag = list.get(i);
                    if (tag.getBackgroundtype() < 1 || tag.getBackgroundtype() > 5) {
                        continue;
                    }
                    TextView txv = new TextView(mSphinx);
                    txv.setText(tag.getDescription());
                    int dpPadding2 = Utility.dip2px(mSphinx, 2);
                    txv.setPadding(dpPadding2, 0, dpPadding2, 0);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);  
                    int dpMargin = Utility.dip2px(mSphinx, 4);
                    lp.setMargins(dpMargin, 0, dpMargin, 0); 
                    txv.setLayoutParams(lp); 
                    txv.setBackgroundResource(tagResList[tag.getBackgroundtype()]);
                    txv.setTextSize(11f);
                    txv.setTextColor(Color.WHITE);
                    planHolder.tags.addView(txv);
                }
            }
        }
        
        public View getView() {
            return v;
        }
    }
}

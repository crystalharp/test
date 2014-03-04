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
import android.text.TextUtils;
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
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.PlanTag;
import com.tigerknows.model.TrafficModel.Plan.Step;
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
    
    private ViewGroup mAlarmBtn = null;
    
    //TODO:把mtype和mindex也重构到结果中
    private int mType = -1;

    private Plan mPlan = null;
    
    private LinearLayout mBottomButtonsView;
    
    private View mSummaryLayout;
    
    private int mChildLayoutId = R.layout.traffic_child_traffic;
    
    private TrafficResult mResult = new TrafficResult();
    
    private int mIndex = -1;
    
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
        
        mResultLsv.addHeaderView(mSummaryLayout);
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
                mAlarmBtn.setVisibility(View.VISIBLE);
                mErrorRecoveryBtn.setVisibility(View.VISIBLE);
                mBottomButtonsView.setWeightSum(4);
                break;
            case Plan.Step.TYPE_DRIVE:
                mTitleBtn.setText(getString(R.string.title_drive_plan));
                mAlarmBtn.setVisibility(View.GONE);
                mErrorRecoveryBtn.setVisibility(View.GONE);
                mBottomButtonsView.setWeightSum(2);
                break;
            case Plan.Step.TYPE_WALK:
                mTitleBtn.setText(getString(R.string.title_walk_plan));
                mAlarmBtn.setVisibility(View.GONE);
                mErrorRecoveryBtn.setVisibility(View.GONE);
                mBottomButtonsView.setWeightSum(2);
                break;
            default:
        }
        mRightBtn.setText(R.string.map);
        mRightBtn.setOnClickListener(this);
        mRightBtn.setVisibility(View.VISIBLE);
        
        Utility.setFavoriteBtn(mSphinx, mFavorateBtn, mPlan.checkFavorite(mContext));
        
        if (mDismissed) {
            setSelectionFromTop();
            //FIXME:basefragment的mDismissed没有被修改，先修改这里把。
            mDismissed = false;
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

    @Override
    protected void findViews() {
        super.findViews();
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mBottomButtonsView = (LinearLayout) mRootView.findViewById(R.id.bottom_buttons_view);
        mErrorRecoveryBtn = (ViewGroup) mRootView.findViewById(R.id.error_recovery_btn);
        mFavorateBtn = (ViewGroup) mRootView.findViewById(R.id.favorite_btn);
        mShareBtn = (ViewGroup) mRootView.findViewById(R.id.share_btn);
        mAlarmBtn = (ViewGroup) mBottomButtonsView.findViewById(R.id.nearby_search_btn);
        TextView textView = (TextView) mAlarmBtn.getChildAt(0);
        textView.setText(R.string.alarm_text);
        Drawable left = getResources().getDrawable(R.drawable.ic_alarm);
        left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        textView.setCompoundDrawables(left, null, null, null);
        mSummaryLayout = mLayoutInflater.inflate(R.layout.traffic_group_traffic, null);
        mSummaryLayout.setBackgroundResource(R.drawable.btn_filter2_normal);
        mSummaryLayout.setPadding(Utility.dip2px(mSphinx, 12), 0, Utility.dip2px(mSphinx, 12), Utility.dip2px(mSphinx, 16));
    }

    @Override
    protected void setListener() {
        super.setListener();
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int location, long id) {
                //添加的header不能点击
                if (location == 0) {
                    return;
                }
                int position = (int) id;
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);

                // 绘制交通图层
                viewMap();
                // 将地图平移到某一item index, 并缩放至某一级别
                TrafficOverlayHelper.panToPosition(mSphinx, position+1, mSphinx.getMapView());
            }
        });
     
        mAlarmBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.right_btn) {
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
            viewMap();
            TrafficOverlayHelper.panToViewWholeOverlay(mPlan, mSphinx);
            TrafficOverlayHelper.showPlanInfoWindow(mSphinx);
        } else if (viewId == R.id.nearby_search_btn) {
            List<POI> poiList = new ArrayList<POI>();
            List<Step> list = mPlan.getStepList();
            for(int i = 0, size = list.size(); i < size; i++) {
                Step step = list.get(i);
                if (step.getType() == Step.TYPE_TRANSFER) {
                    POI poi;
                    List<Position> posList = step.getPositionList();
                    if (posList != null && posList.size() > 0) {
                        String name = step.getTransferUpStopName();
                        if (name != null) {
                            poi = new POI();
                            poi.setName(name);
                            poi.setPosition(posList.get(0));
                            if (poiList.contains(poi) == false) {
                                poiList.add(poi);
                            }
                        }
                        name = step.getTransferDownStopName();
                        if (name != null) {
                            poi = new POI();
                            poi.setName(name);
                            poi.setPosition(posList.get(posList.size()-1));
                            if (poiList.contains(poi) == false) {
                                poiList.add(poi);
                            }
                        }
                    }
                }
            }
            
            BuslineResultLineFragment.showAlarmDialog(mSphinx, poiList);
        }
    }
    
    class TrafficResult {
        TrafficQuery query;
        List<ArrayList<Plan>> planLists;

        final static int TRANSTER = Step.TYPE_TRANSFER;
        final static int DRIVE = Step.TYPE_DRIVE;
        final static int WALK = Step.TYPE_WALK;
        
        public TrafficResult() {
            planLists = new ArrayList<ArrayList<Plan>>();
            planLists.add(null);
            planLists.add(new ArrayList<Plan>());
            planLists.add(new ArrayList<Plan>());
            planLists.add(new ArrayList<Plan>());
        }
        
        public void setResult(int type, List<Plan>plans) {
            if (plans == null || (type != TRANSTER && type != DRIVE && type !=WALK)) {
                return;
            }
            List<Plan> list = planLists.get(type);
            if (list == null) {
                list = new ArrayList<Plan>();
            }
            list.clear();
            list.addAll(plans);
        }
        
        public Plan findDriveType(int type) {
            List<Plan> list = planLists.get(DRIVE);
            for(int i = list.size()-1; i >= 0; i--) {
                if (list.get(i).getDrivePreference() == type) {
                    return list.get(i);
                }
            }
            return null;
        }
        
        public final int getTransferPlanIndex(Plan plan) {
            return planLists.get(TRANSTER).indexOf(plan);
        }
        
        public final void setQuery(TrafficQuery q) {
            query = q;
        }
        
        public TrafficQuery getQuery() {
            return query;
        }
        
        public void reset() {
            query = null;
            planLists.get(DRIVE).clear();
            planLists.get(TRANSTER).clear();
            planLists.get(WALK).clear();
            mType = 0;
        }
        
        public List<Plan> getResult(int type) {
            return planLists.get(type);
        }
    }
    
    public final void addResult(TrafficQuery query, int type, List<Plan>resultList) {
        mResult.setQuery(query);
        mResult.setResult(type, resultList);
    }
    
    public void refreshResult(int type, int index) {
        if (hasResult(type)) {
            mType = type;
            mIndex = index;
            mPlan = mResult.getResult(type).get(mIndex);
            updateResult(mPlan);
        }
    }
    
    public void refreshDrive(Plan plan) {
        if (plan != null) {
            mType = Step.TYPE_DRIVE;
            mIndex = -1;
            updateResult(plan);
        }
    }
    
    public void refreshResult(int type) {
        if (hasResult(type)) {
            if (mType != type) {
                mType = type;
                mIndex = 0;
                mPlan = mResult.getResult(type).get(mIndex);
                updateResult(mPlan);
            }
        }
    }
    
    public final List<Plan> getResult(int type) {
        return mResult.getResult(type);
    }
    
    public final boolean hasResult(int type) {
        return mResult.getResult(type).size() != 0;
    }
    
    public final Plan findDriveType(int type) {
        return mResult.findDriveType(type);
    }
    
    public void resetResult() {
        mResult.reset();
    }
    
    private void updateResult(Plan plan) {
        this.mPlan = plan;
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
        mDismissed = true;
        
        PlanItemRefresher.refresh(mSphinx, plan, mSummaryLayout, false, false);
        plan.updateHistory(mContext);
    }

    public static class StepViewHolder {
        public ImageView image;
        public TextView textView;
    }

    public CharSequence getStepDescription(int position) {
        return mStrList.get(position);
    }

    public Drawable getStepIcon(int position){
        Integer steptype = mTypes.get(position);
        if (mType == Plan.Step.TYPE_TRANSFER) {
            return getTransferDrawable(steptype);
        } else {
            return getDirectionDrawable(steptype);
        }
    }
    
    public Drawable getTransferDrawable(int steptype) {
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
    
    public Drawable getDirectionDrawable(int steptype) {
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
    
    class StringListAdapter extends BaseAdapter{

        public StringListAdapter(Context context) {
            super();
        }

        @Override
        public int getCount() {
            return mStrList.size();
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
            
            stepHolder.image.setBackgroundDrawable(getStepIcon(position));
            stepHolder.textView.setText(getStepDescription(position));
            stepHolder.textView.setTextColor(Color.parseColor("#000000"));
            //convertView.setBackgroundResource(R.drawable.btn_traffic_detail_end_normal);
                
            return convertView;

        }
        

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    private class ResultOnClickListener implements View.OnClickListener {
        
        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.share_btn) {
                //弹出分享对话框
                mActionLog.addAction(mActionTag +  ActionLog.CommonShare);
                share(mPlan);
            } else if (v.getId() == R.id.favorite_btn) {
                favorite(mPlan, v);
            } else if (v.getId() == R.id.error_recovery_btn) {
                mActionLog.addAction(mActionTag +  ActionLog.CommonErrorRecovery);
                TrafficReportErrorActivity.addTarget(mPlan);
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
        Position position = TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx);
        TrafficOverlayHelper.showPlanInfoWindow(mSphinx);
        
        ShareAPI.share(mSphinx, plan, position, mapScene, mActionTag);
    }
    
    private void viewMap() {
        
        if (mPlan != null) {
            String title = null;
            String actionTag = "";
            switch (mType) {
                case Plan.Step.TYPE_TRANSFER:
                    actionTag = ActionLog.TrafficTransferMap;
                    title = getString(R.string.transfer_map);
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
            
            TrafficOverlayHelper.drawOverlay(mSphinx, mPlan);
            
        }
    }

    public TrafficQuery getTrafficQuery() {
        return mResult.getQuery();
    }

    private static class PlanViewHolder {
        public TextView title;
        public TextView tagTitle;
        public TextView txv1;
        public TextView txv4;
        public TextView txv3;
        public TextView txv2;
        public LinearLayout tags;
        public int position;
    }
    
    public static class PlanItemRefresher {
        static int resid = R.layout.traffic_group_traffic;
        static PlanViewHolder planHolder = new PlanViewHolder();
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
        
        public static void refresh(Sphinx sphinx, Plan plan, View v, boolean titleSingleLine) {
            refresh(sphinx, plan, v, titleSingleLine, true);
        }
        
        public static void refresh(Sphinx sphinx, Plan plan, View v, boolean titleSingleLine, boolean showTags) {
            try {
                planHolder.title = (TextView)v.findViewById(R.id.title_txv);
                planHolder.tagTitle = (TextView) v.findViewById(R.id.tagtitle_txv);
                planHolder.txv1 = (TextView)v.findViewById(R.id.txv1);
                planHolder.txv2 = (TextView) v.findViewById(R.id.txv2);
                planHolder.txv3 = (TextView) v.findViewById(R.id.txv3);
                planHolder.txv4 = (TextView) v.findViewById(R.id.txv4);
                planHolder.tags = (LinearLayout) v.findViewById(R.id.tags_view);
            } catch (Exception e) {
                return;
            }
            v.setTag(plan);
            planHolder.title.setSingleLine(titleSingleLine);
            if (titleSingleLine) {
                planHolder.title.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                planHolder.title.setEllipsize(null);
            }
            planHolder.tagTitle.setVisibility(View.GONE);
            planHolder.tags.removeAllViews();
            List<PlanTag> tagList = plan.getPlanTagList();
            if (plan.getType() == TrafficQuery.QUERY_TYPE_TRANSFER) {
                setTxvText(planHolder.title, plan.getTitle(sphinx));
                setTxvText(planHolder.txv1, plan.getExpectedBusTime());
                setTxvText(planHolder.txv2, plan.getLengthStr(sphinx));
                setTxvText(planHolder.txv3, plan.getWalkDistance4Transfer());
                setTxvText(planHolder.txv4, plan.getBusstopNum());
                if (tagList != null && showTags) {
                    int tagNum = Math.min(MAX_TAG_NUM, plan.getPlanTagList().size());
                    for (int i = 0; i < tagNum; i++) {
                        PlanTag tag = tagList.get(i);
                        if (tag.getBackgroundtype() < 1 || tag.getBackgroundtype() > 5) {
                            continue;
                        }
                        TextView txv = new TextView(sphinx);
                        txv.setText(tag.getDescription());
                        int dpPadding2 = Utility.dip2px(sphinx, 2);
                        txv.setPadding(dpPadding2, 0, dpPadding2, 0);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);  
                        int dpMargin = Utility.dip2px(sphinx, 4);
                        lp.setMargins(dpMargin, 0, dpMargin, 0); 
                        txv.setLayoutParams(lp); 
                        txv.setBackgroundResource(tagResList[tag.getBackgroundtype()]);
                        txv.setTextSize(11f);
                        txv.setTextColor(Color.WHITE);
                        planHolder.tags.addView(txv);
                    }
                }
            } else if (plan.getType() == TrafficQuery.QUERY_TYPE_DRIVE) {
                setTxvText(planHolder.title, sphinx.getString(R.string.title_type_drive));
                setTxvText(planHolder.txv1, plan.getDriveDistance());
                setTxvText(planHolder.txv2, plan.getExpectedDriveTime());
                setTxvText(planHolder.txv3, plan.getTrafficLightNum());
                setTxvText(planHolder.txv4, plan.getTaxiCost4Drive());
                if (tagList != null && tagList.size() > 0 && showTags) {
                    planHolder.tagTitle.setVisibility(View.VISIBLE);
                    PlanTag tag = tagList.get(0);
                    planHolder.tagTitle.setText(tag.getDescription());
                    planHolder.tagTitle.setBackgroundResource(tagResList[tag.getBackgroundtype()]);
                }
            } else if (plan.getType() == TrafficQuery.QUERY_TYPE_WALK) {
                setTxvText(planHolder.title, sphinx.getString(R.string.title_type_walk));
                setTxvText(planHolder.txv1, plan.getWalkDistance4Walk());
                setTxvText(planHolder.txv2, plan.getExpectedWalkTime());
                setTxvText(planHolder.txv3, plan.getTaxiCost4Walk());
                setTxvText(planHolder.txv4, null);
            }
        }
        
    }

}

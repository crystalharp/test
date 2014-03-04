/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.AddtionalInfo;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.PlanTag;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficDetailFragment.PlanItemRefresher;
import com.tigerknows.ui.common.ResultMapFragment;

/**
 * 
 * 交通方案页，与交通详情共用布局文件
 *
 */

public class TrafficResultFragment extends BaseFragment {

    public TrafficResultFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private BaseAdapter mResultAdapter;
    
    private List<Plan> mPlanList = new ArrayList<TrafficModel.Plan>();

    private ListView mResultLsv = null;
    
    private LinearLayout mFootLayout = null;

    private TrafficQuery mTrafficQuery;
    
    private TrafficModel mTrafficModel = null;
    
    

    /*
     * 用于控制序号图片显示
     */
//    private ChildView downView;
    
    private View mFooterView;
    
    private View mHeaderView;
    
    private View mStartEndView;
    
    private View mSearchReturnView;
    
    private TextView mStartEndTxv;
    
    private TextView mDescriptionTxv;
    
    private View mTrafficTitieView;
    
    private RadioGroup mTrafficTitleRadioGroup;
    private RadioButton mTrafficTransferRbt;
    private RadioButton mTrafficDriveRbt;
    private RadioButton mTrafficWalkRbt;

    
    
    int focusedIndex = Integer.MAX_VALUE;
    
    View.OnTouchListener onTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            boolean result = false;
            if (action == MotionEvent.ACTION_UP) {
                if (R.id.traffic_drive_rbt == id) {
                    mActionLog.addAction(mActionTag + ActionLog.TrafficDriveTab);
                    result = !changeTrafficType(Plan.Step.TYPE_DRIVE);
                } else if (R.id.traffic_walk_rbt == id) {
                    mActionLog.addAction(mActionTag + ActionLog.TrafficWalkTab);
                    result = !changeTrafficType(Plan.Step.TYPE_WALK);
                }
            }
            return result;
        }
    };

    
    private static final String TAG = "TrafficResultFragment";
    
    public TrafficQuery getTrafficQuery() {
        return mTrafficQuery;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficResult;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_result, container, false);
        mFooterView = mLayoutInflater.inflate(R.layout.traffic_transfer_result_footer, null);
        mHeaderView = mLayoutInflater.inflate(R.layout.traffic_transfer_result_header, null);
        
//        mTrafficTitieView = mLayoutInflater.inflate(R.layout.traffic_query_bar, null);

        findViews();
        setListener();
        
        mResultAdapter = new TransferProjectListAdapter();
        mResultLsv.addFooterView(mFooterView, null, false);
        mResultLsv.addHeaderView(mHeaderView, null, false);
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mTitleBtn.setVisibility(View.GONE);
        mTrafficTitieView = mSphinx.getTrafficQueryFragment().getTitleView();
        ((ViewGroup)mSphinx.getTitleFragment().mRootView).addView(mTrafficTitieView, TrafficQueryFragment.sLayoutParams);
        
        mTrafficTitleRadioGroup = (RadioGroup) mTrafficTitieView.findViewById(R.id.traffic_rgp);
        mTrafficTransferRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_transfer_rbt);
        mTrafficDriveRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_drive_rbt);
        mTrafficWalkRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_walk_rbt);
        
        mTrafficTitleRadioGroup.check(R.id.traffic_transfer_rbt);
        TKConfig.setPref(mSphinx, TKConfig.PREFS_CHECKED_TRAFFIC_RADIOBUTTON, String.valueOf(mTrafficTransferRbt.getId()));
        mTrafficTransferRbt.setOnTouchListener(null);
        mTrafficDriveRbt.setOnTouchListener(onTouchListener);
        mTrafficWalkRbt.setOnTouchListener(onTouchListener);
        
        mRightBtn.setBackgroundResource(R.drawable.btn_back);
        mRightBtn.setVisibility(View.INVISIBLE);
        
        mFootLayout.setVisibility(View.GONE);
        AddtionalInfo info = mTrafficModel.getAddtionalInfo();
        String desc = (info == null) ? null : info.getDescription();
        setTxvText(mDescriptionTxv, desc);
        mStartEndTxv.setText(mTrafficModel.getStart().getName() 
                + mSphinx.getString(R.string.traffic_transfer_arrow) 
                + mTrafficModel.getEnd().getName());
        
        if (mDismissed) {
            mResultLsv.setSelectionFromTop(0, 0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void findViews() {
        super.findViews();
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mFootLayout = (LinearLayout)mRootView.findViewById(R.id.bottom_buttons_view);
        mStartEndView = mHeaderView.findViewById(R.id.search_return_view);
        mSearchReturnView = mHeaderView.findViewById(R.id.imageView2);
        mStartEndTxv = (TextView) mStartEndView.findViewById(R.id.textView1);
        mDescriptionTxv = (TextView) mFooterView.findViewById(R.id.description_txv);
    }

    @Override
    protected void setListener() {
        super.setListener();
        
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                int realPos = (int) id;
                if (realPos >= mPlanList.size()) {
                    return;
                }
                List<PlanTag> list = mPlanList.get(realPos).getPlanTagList();
                String tags;
                if (list != null) {
                    tags = mPlanList.get(realPos).getPlanTagList().toString();
                } else {
                    tags = "";
                }
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, realPos, tags);
                focusedIndex = realPos;
                mSphinx.getTrafficDetailFragment().addResult(mTrafficQuery, mPlanList.get(0).getType(), mPlanList);
                mSphinx.getTrafficDetailFragment().refreshResult(mPlanList.get(0).getType(), realPos);
                mSphinx.showView(R.id.view_traffic_result_detail);
            }

        });
        
        mSearchReturnView.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mActionLog.addAction(mActionTag + ActionLog.TrafficResultSearchReturn);
                mSphinx.getTrafficQueryFragment().switchStartEnd();
                mSphinx.getTrafficQueryFragment().query();
                mDismissed = true;
            }
        });
        
    }
    
    private boolean changeTrafficType(int type) {
        boolean result = false;
        
        TrafficDetailFragment trafficDetailFragment = mSphinx.getTrafficDetailFragment();
        TrafficQuery trafficQuery = trafficDetailFragment.getTrafficQuery();

        if (type != Plan.Step.TYPE_DRIVE && type != Plan.Step.TYPE_WALK) {
            return result;
        }
        
        if (!trafficDetailFragment.hasResult(type)) {
            TrafficQuery newTrafficQuery = new TrafficQuery(mContext);
            newTrafficQuery.setup(trafficQuery.getStart(),
                    trafficQuery.getEnd(),
                    type,
                    TrafficResultFragment.this.getId(),
                    getString(R.string.doing_and_wait));
            newTrafficQuery.setCityId(trafficQuery.getCityId());
            mSphinx.queryStart(newTrafficQuery);
        } else {
            List<Plan> list = trafficDetailFragment.getResult(type);
            trafficDetailFragment.refreshResult(type);
            mSphinx.uiStackRemove(this.getId());
            if (type == Plan.Step.TYPE_DRIVE) {
                ResultMapFragment resultMapFragment = mSphinx.getResultMapFragment();
                resultMapFragment.setData(null, ActionLog.TrafficDriveMap);
                mSphinx.showView(R.id.view_result_map);

                TrafficOverlayHelper.drawOverlay(mSphinx, list.get(0));
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx);
                TrafficOverlayHelper.showPlanInfoWindow(mSphinx);
                
                result = true;
            } else if (type == Plan.Step.TYPE_WALK) {
                ResultMapFragment resultMapFragment = mSphinx.getResultMapFragment();
                resultMapFragment.setData(null, ActionLog.TrafficWalkMap);
                mSphinx.showView(R.id.view_result_map);

                TrafficOverlayHelper.drawOverlay(mSphinx, list.get(0));
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx);
                TrafficOverlayHelper.showPlanInfoWindow(mSphinx);

                result = true;
            }
        }
        
        return result;
    }
    
    public void setData(TrafficQuery trafficQuery) {

        mTrafficQuery = trafficQuery;
        mTrafficModel = mTrafficQuery.getTrafficModel();
        
        focusedIndex = Integer.MAX_VALUE;
        
        mPlanList.clear();
        mPlanList.addAll(mTrafficModel.getPlanList());
        mResultAdapter.notifyDataSetChanged();
        if (mSphinx.uiStackPeek() == R.id.view_result_map) {
            mSphinx.uiStackClearTop(R.id.view_traffic_home);
        }
    }
    
    class TransferProjectListAdapter extends BaseAdapter{

        public TransferProjectListAdapter() {
            super();
        }

        @Override
        public int getCount() {
            return mPlanList.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlanList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
        	if(convertView == null) {
        	    convertView = mLayoutInflater.inflate(R.layout.traffic_group_traffic, null);
        	}
        	
        	Plan plan = (Plan) getItem(position);
        	PlanItemRefresher.refresh(mSphinx, plan, convertView, true);
        	
        	return convertView;
		}
    	
    }
        
    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        if (TrafficQueryFragment.dealWithTrafficResponse(mSphinx,
                mActionTag,
                (TrafficQuery) tkAsyncTask.getBaseQuery(),
                false)) {
            if (mSphinx.uiStackPeek() != this.getId()) {
                mSphinx.uiStackRemove(this.getId());
            }
        }
    }
}

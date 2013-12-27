/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.AddtionalInfo;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficDetailFragment.TrafficDetailItem;
import com.tigerknows.ui.common.ResultMapFragment;

/**
 * 
 * 交通方案页，与交通详情共用布局文件
 *
 */

public class TrafficResultFragment extends BaseFragment {

    public TrafficResultFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
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
    
    private View mSearchReturnView;
    
    private TextView mDescriptionTxv;
    
    private View mTrafficTitieView;
    
    private RadioGroup mTrafficTitleRadioGroup;
    
    int focusedIndex = Integer.MAX_VALUE;
    
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
        
        mTrafficTitieView = mLayoutInflater.inflate(R.layout.traffic_query_bar, null);

        findViews();
        setListener();
        
        mResultAdapter = new TransferProjectListAdapter();
        mResultLsv.addFooterView(mFooterView);
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mTitleBtn.setVisibility(View.GONE);
        mTitleView.addView(mTrafficTitieView);
        mTrafficTitleRadioGroup.check(R.id.traffic_transfer_rbt);
        mRightBtn.setVisibility(View.GONE);
        
        mFootLayout.setVisibility(View.GONE);
        AddtionalInfo info = mTrafficModel.getAddtionalInfo();
        if (info != null) {
            mDescriptionTxv.setText(info.getDescription());
            mDescriptionTxv.setVisibility(View.VISIBLE);
        } else {
            mDescriptionTxv.setVisibility(View.GONE);
        }
        
        if (mDismissed) {
            mResultLsv.setSelectionFromTop(0, 0);
        }
     
        // 下一行代码为避免以下操作会出现问题
        // 搜索-结果列表-详情-地图-点击气泡中的交通按钮-选择到这里去/公交-公交方案-公交详情-点击地图显示公交方案页(期望进入 地图界面)
        // 确保整个UI堆栈里不能出现重复的结果地图界面
        // Fixed: [And4.30-287] [确实有]【交通】公交详情界面点击“地图”返回到公交方案界面，地图不加载。
        mSphinx.uiStackRemove(R.id.view_result_map); 
    }

    @Override
    public void onPause() {
        super.onPause();
        mTitleView.removeView(mTrafficTitieView);
    }
   
    protected void findViews() {
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mFootLayout = (LinearLayout)mRootView.findViewById(R.id.bottom_buttons_view);
        mSearchReturnView = mFooterView.findViewById(R.id.search_return_view);
        mDescriptionTxv = (TextView) mFooterView.findViewById(R.id.description_txv);
        mTrafficTitleRadioGroup = (RadioGroup) mTrafficTitieView.findViewById(R.id.traffic_rgp);
    }

    protected void setListener() {
        mTrafficTitleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.traffic_drive_rbt) {
                    changeTrafficType(Plan.Step.TYPE_DRIVE);
                } else if (checkedId == R.id.traffic_walk_rbt) {
                    changeTrafficType(Plan.Step.TYPE_WALK);
                }
            }
        });
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
                focusedIndex = position;
                mSphinx.getTrafficDetailFragment().setData(mTrafficQuery,
                        mPlanList,
                        mSphinx.getTrafficDetailFragment().getDrivePlanList(),
                        mSphinx.getTrafficDetailFragment().getWalkPlanList(),
                        mPlanList.get(0).getType(),
                        position);
                mSphinx.showView(R.id.view_traffic_result_detail);
            }

        });
        
        mSearchReturnView.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mSphinx.getTrafficQueryFragment().switchStartEnd();
                mSphinx.getTrafficQueryFragment().query();
            }
        });
        
    }
    
    private void changeTrafficType(int type) {
        TrafficDetailFragment trafficDetailFragment = mSphinx.getTrafficDetailFragment();
        TrafficQuery trafficQuery = trafficDetailFragment.getTrafficQuery();
        List<Plan> list = null;

        if (type == Plan.Step.TYPE_DRIVE) {
            list = trafficDetailFragment.getDrivePlanList();
        } else if (type == Plan.Step.TYPE_WALK) {
            list = trafficDetailFragment.getWalkPlanList();
        } else {
            return;
        }
        
        if (list.isEmpty()) {
            TrafficQuery newTrafficQuery = new TrafficQuery(mContext);
            newTrafficQuery.setup(trafficQuery.getStart(),
                    trafficQuery.getEnd(),
                    type,
                    TrafficResultFragment.this.getId(),
                    getString(R.string.doing_and_wait));
            newTrafficQuery.setCityId(trafficQuery.getCityId());
            mSphinx.queryStart(newTrafficQuery);
        } else {
            trafficDetailFragment.setData(trafficQuery,
                    trafficDetailFragment.getTransferPlanList(),
                    trafficDetailFragment.getDrivePlanList(), 
                    trafficDetailFragment.getWalkPlanList(),
                    type,
                    0);
            if (type == Plan.Step.TYPE_DRIVE) {
                ResultMapFragment resultMapFragment = mSphinx.getResultMapFragment();
                resultMapFragment.setData(null, ActionLog.TrafficDriveMap);
                mSphinx.showView(R.id.view_result_map);
                
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx.getMapView(), mSphinx);
            } else if (type == Plan.Step.TYPE_WALK) {
                ResultMapFragment resultMapFragment = mSphinx.getResultMapFragment();
                resultMapFragment.setData(null, ActionLog.TrafficWalkMap);
                mSphinx.showView(R.id.view_result_map);
                
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx.getMapView(), mSphinx);
            }
        }
    }
    
    public void setData(TrafficQuery trafficQuery) {

        mTrafficQuery = trafficQuery;
        mTrafficModel = mTrafficQuery.getTrafficModel();
        
        focusedIndex = Integer.MAX_VALUE;
        
        mPlanList.clear();
        mPlanList.addAll(mTrafficModel.getPlanList());
        mResultAdapter.notifyDataSetChanged();
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
			TrafficDetailItem item = null;
        	if(convertView == null) {
        		item = new TrafficDetailItem(mSphinx);
        		convertView = item.getView();
        		convertView.setTag(item);
        	}else {
        		item = (TrafficDetailItem)convertView.getTag();
        	}
        	
        	Plan plan = (Plan) getItem(position);
        	item.refresh(plan);
        	
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
        
        TrafficQueryFragment.dealWithTrafficResponse(mSphinx,
                mActionTag,
                (TrafficQuery) tkAsyncTask.getBaseQuery(),
                false);
    }
}

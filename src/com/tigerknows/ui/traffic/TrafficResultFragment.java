/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.ui.BaseFragment;

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

    private TextView mStartTxv = null;

    private TextView mEndTxv = null;
    
    private TextView mLengthTxv = null;
    
    private ListView mResultLsv = null;
    
    private LinearLayout mFootLayout = null;

    private TrafficQuery mTrafficQuery;
    
    private TrafficModel mTrafficModel = null;

    /*
     * 用于控制序号图片显示
     */
    private ChildView downView;
    
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

        findViews();
        setListener();
        
        mResultAdapter = new TransferProjectListAdapter();
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEndTxv.setVisibility(View.VISIBLE);
        
        mStartTxv.setText(getString(R.string.start_text, mTrafficModel.getStart().getName()));
        mEndTxv.setText(getString(R.string.end_text, mTrafficModel.getEnd().getName()));

        mTitleBtn.setText(getString(R.string.title_type_transfer));
        mLengthTxv.setVisibility(View.GONE);
        
        mFootLayout.setVisibility(View.GONE);
        
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
    }
   
    protected void findViews() {
        mStartTxv = (TextView)mRootView.findViewById(R.id.start_txv);
        mEndTxv = (TextView)mRootView.findViewById(R.id.end_txv);
        mLengthTxv = (TextView)mRootView.findViewById(R.id.length_txv);
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mFootLayout = (LinearLayout)mRootView.findViewById(R.id.bottom_buttons_view);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
				focusedIndex = position;
				mSphinx.getTrafficDetailFragment().setData(mTrafficModel.getPlanList().get(position), position);
				mSphinx.showView(R.id.view_traffic_result_detail);
			}

        });
        
        mResultLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView arg0, int arg1) {
            	if (downView != null) {
            		PlanViewHolder viewHandler = (PlanViewHolder)downView.getTag();
                	if (viewHandler.position != focusedIndex) {
//                		viewHandler.index.setBackgroundResource(R.drawable.btn_index);
                	}
            	}
            }

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
			}
        });
        
        mResultLsv.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ((event.getAction() == MotionEvent.ACTION_UP) && (downView != null)) {
					if (downView.isSelected() || downView.isFocused() || downView.isPressed()) {
						LogWrapper.d(TAG, "set index bg: " + "bg_index_focused");
						PlanViewHolder planHolder = (PlanViewHolder)downView.getTag();
//		        		planHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
					}
				}
				return false;
			}
		});
    }
    
    public void setData(TrafficQuery trafficQuery) {

    	mTrafficQuery = trafficQuery;
        mTrafficModel = mTrafficQuery.getTrafficModel();
        
        focusedIndex = Integer.MAX_VALUE;
        
        mPlanList.clear();
        mPlanList.addAll(mTrafficModel.getPlanList());
        mResultAdapter.notifyDataSetChanged();
    }

    public static class PlanViewHolder {
    	public TextView text;
    	public TextView summary;
    	public TextView index;
        public int position;
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
			PlanViewHolder planHolder = null;
        	if(convertView == null) {
        		convertView = new ChildView(mContext);
        		
        		planHolder = new PlanViewHolder();
        		planHolder.summary = (TextView)convertView.findViewById(R.id.text1);
        		planHolder.text = (TextView)convertView.findViewById(R.id.text2);
        		planHolder.index = (TextView)convertView.findViewById(R.id.index);
        		planHolder.position = position;
        		
        		convertView.setTag(planHolder);
        	}else {
        		planHolder = (PlanViewHolder)convertView.getTag();
        	}
        	
        	Plan plan = (Plan) getItem(position);
        	planHolder.text.setText(plan.getTitle(mSphinx));

        	planHolder.summary.setText(plan.getLengthStr(mSphinx));
        	planHolder.index.setText(String.valueOf(position+1));
        	
        	if (focusedIndex == position) {
				LogWrapper.d(TAG, "position: " + position + "bg_index_focused");
//        		planHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
			} else {
				LogWrapper.d(TAG, "position: " + position + "bg_index");
//				planHolder.index.setBackgroundResource(R.drawable.btn_index);
			}
        	
        	planHolder.position = position;
        	
        	return convertView;
		}
    	
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                mTrafficModel = null;
                break;
                
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    class ChildView extends RelativeLayout {

    	public ChildView(Context context) {
			super(context);
			mLayoutInflater.inflate(R.layout.traffic_group_traffic, this, true);
			setBackgroundResource(R.drawable.list_selector_background_gray_light);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				LogWrapper.d(TAG, "ChildView ACTION_DOWN");
				downView = this;
				
				if (Globals.g_ApiVersion <= android.os.Build.VERSION_CODES.FROYO) {
					setSelected(true);
				}
			}
			
			return super.onTouchEvent(event);
		}
		
    }

    public List<Plan> getData() {
        if (mTrafficModel != null) {
            return mTrafficModel.getPlanList();
        }
        return null;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mTrafficModel = null;
    }
}

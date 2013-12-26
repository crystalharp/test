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
import com.tigerknows.model.TrafficModel.Plan.PlanTag;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

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

        mTitleBtn.setText(getString(R.string.title_type_transfer));
        mRightBtn.setVisibility(View.GONE);
        
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
				mSphinx.getTrafficDetailFragment().setData(mTrafficModel.getPlanList(), position);
				mSphinx.showView(R.id.view_traffic_result_detail);
			}

        });
        
//        mResultLsv.setOnScrollListener(new OnScrollListener() {
//            
//            @Override
//            public void onScrollStateChanged(AbsListView arg0, int arg1) {
//            	if (downView != null) {
//            		PlanViewHolder viewHandler = (PlanViewHolder)downView.getTag();
//                	if (viewHandler.position != focusedIndex) {
////                		viewHandler.index.setBackgroundResource(R.drawable.btn_index);
//                	}
//            	}
//            }
//
//			@Override
//			public void onScroll(AbsListView view, int firstVisibleItem,
//					int visibleItemCount, int totalItemCount) {
//			}
//        });
//        
//        mResultLsv.setOnTouchListener(new OnTouchListener() {
//			
//			@Override
//			public boolean onTouch(View v, MotionEvent event) {
//				if ((event.getAction() == MotionEvent.ACTION_UP) && (downView != null)) {
//					if (downView.isSelected() || downView.isFocused() || downView.isPressed()) {
//						LogWrapper.d(TAG, "set index bg: " + "bg_index_focused");
//						PlanViewHolder planHolder = (PlanViewHolder)downView.getTag();
////		        		planHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
//					}
//				}
//				return false;
//			}
//		});
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
    	public TextView title;
    	public TextView distance;
    	public TextView walkDistance;
    	public TextView bustime;
    	public TextView busstop;
    	public LinearLayout tags;
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
        		planHolder.distance = (TextView)convertView.findViewById(R.id.distance_txv);
        		planHolder.title = (TextView)convertView.findViewById(R.id.title_txv);
        		planHolder.busstop = (TextView) convertView.findViewById(R.id.busstop_txv);
        		planHolder.bustime = (TextView) convertView.findViewById(R.id.bustime_txv);
        		planHolder.walkDistance = (TextView) convertView.findViewById(R.id.walk_distance_txv);
        		planHolder.tags = (LinearLayout) convertView.findViewById(R.id.tags_view);
        		planHolder.position = position;
        		
        		convertView.setTag(planHolder);
        	}else {
        		planHolder = (PlanViewHolder)convertView.getTag();
        	}
        	
        	Plan plan = (Plan) getItem(position);
        	planHolder.title.setText(plan.getTitle(mSphinx));
        	planHolder.distance.setText(plan.getLengthStr(mSphinx));
        	planHolder.busstop.setText(plan.getBusstopNum());
        	planHolder.bustime.setText(plan.getExpectedBusTime());
        	planHolder.walkDistance.setText(plan.getWalkDistance());
        	planHolder.tags.removeAllViews();
        	List<PlanTag> list = plan.getPlanTagList();
        	int tagNum = Math.min(MAX_TAG_NUM, plan.getPlanTagList().size());
        	for (int i = 0; i < tagNum; i++) {
        	    PlanTag tag = list.get(i);
        	    if (tag.getBackgroundtype() < 1 || tag.getBackgroundtype() > 5) {
        	        continue;
        	    }
        	    TextView txv = new TextView(mSphinx);
        	    txv.setText(tag.getDescription());
        	    int dpPadding2 = Utility.dip2px(mSphinx, 2);
        	    int dpPadding1 = Utility.dip2px(mSphinx, 1);
        	    txv.setPadding(dpPadding2, dpPadding1, dpPadding2, dpPadding1);
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
        	
        	if (focusedIndex == position) {
				LogWrapper.d(TAG, "position: " + position + "bg_index_focused");
			} else {
				LogWrapper.d(TAG, "position: " + position + "bg_index");
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

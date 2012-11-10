/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.util.CommonUtils;

public class TrafficResultFragment extends BaseFragment {

    public TrafficResultFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private BaseAdapter mResultAdapter;

    private TextView mStartTxv = null;

    private TextView mEndTxv = null;
    
    private TextView mLengthTxv = null;
    
    private ListView mResultLsv = null;

    private TrafficQuery mTrafficQuery;
    
    private TrafficModel mTrafficModel = null;

    /*
     * 用于控制序号图片显示
     */
    private ChildView downView;
    
    int focusedIndex = Integer.MAX_VALUE;
    
    private static final String TAG = "TrafficResultFragment";
    
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
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRightTxv.setVisibility(View.INVISIBLE);
        
        mStartTxv.setText(mTrafficModel.getStart().getName());
        mEndTxv.setText(mTrafficModel.getEnd().getName());

        mTitleTxv.setText(mContext.getString(R.string.title_transfer_plan_list));
        mLengthTxv.setVisibility(View.GONE);
        
        mResultAdapter = new TransferProjectListAdapter(mTrafficQuery.getTrafficModel().getPlanList());
        mResultLsv.setAdapter(mResultAdapter);
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
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub

				mActionLog.addAction(ActionLog.TrafficResultSelect, position);
				focusedIndex = position;
				mSphinx.getTrafficDetailFragment().setData(mTrafficModel.getPlanList().get(position));
				mSphinx.showView(R.id.view_traffic_result_detail);
			}

        });
        
        mResultLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView arg0, int arg1) {
            	if (downView != null) {
            		PlanViewHolder viewHandler = (PlanViewHolder)downView.getTag();
                	if (viewHandler.position != focusedIndex) {
                		viewHandler.index.setBackgroundResource(R.drawable.btn_index);
                	}
            	}
            }

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
			}
        });
        
        mResultLsv.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if ((event.getAction() == MotionEvent.ACTION_UP) && (downView != null)) {
					if (downView.isSelected() || downView.isFocused() || downView.isPressed()) {
						LogWrapper.d(TAG, "set index bg: " + "bg_index_focused");
						PlanViewHolder planHolder = (PlanViewHolder)downView.getTag();
		        		planHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
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
    }

    public static class PlanViewHolder {
    	public TextView text;
    	public TextView summary;
    	public TextView index;
        public int position;
    }
    
    class TransferProjectListAdapter extends BaseAdapter{

    	private static final int mResource = R.layout.traffic_group_traffic;
    	
    	private ArrayList<String> mGroups = new ArrayList<String>();
    	
		public TransferProjectListAdapter(List<Plan> planList) {
			super();
			// TODO Auto-generated constructor stub
			
			for(Plan plan : planList)
				genGroupTitle(plan);
		}

		private void genGroupTitle(Plan plan) {
            
            String busName = "";
            int transferTimes = -1;

            for(Step step : plan.getStepList()) {
                if (Step.TYPE_TRANSFER == step.getType()) {
                    if (!TextUtils.isEmpty(busName)) {
                        busName = busName + " --> " + step.getTransferLineName();
                    } else {
                        busName = step.getTransferLineName();
                    }
                    transferTimes++;
                }
            }
            
            String length = "";
            if (plan.getLength() > 1000) {
                length = mContext.getString(R.string.traffic_result_length_km, CommonUtils.meter2kilometre(plan.getLength()));
            } else {
            	length = mContext.getString(R.string.traffic_result_length_m, plan.getLength());
            }
            
            if (transferTimes > 0) {
                mGroups.add(busName + "\n" + mContext.getString(R.string.traffic_transfer_title, transferTimes, length));
            } else {
            	if(busName.equals(""))
            		busName = mContext.getString(R.string.traffic_noneed_transfer);
                mGroups.add(busName + "\n" + mContext.getString(R.string.traffic_nonstop_title, length));
            }
        }

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mGroups.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return mGroups.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
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
        	
        	String[] temp = mGroups.get(position).split("\n");
        	planHolder.text.setText(temp[0]);

        	if (temp.length > 1) {
        		planHolder.summary.setText(temp[1]);
        	}
        	planHolder.index.setText(String.valueOf(position+1));
        	
        	if (focusedIndex == position) {
				LogWrapper.d(TAG, "position: " + position + "bg_index_focused");
        		planHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
			} else {
				LogWrapper.d(TAG, "position: " + position + "bg_index");
				planHolder.index.setBackgroundResource(R.drawable.btn_index);
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
			// TODO Auto-generated constructor stub
			mLayoutInflater.inflate(R.layout.traffic_group_traffic, this, true);
			setBackgroundResource(R.drawable.list_selector_background_gray_light);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			// TODO Auto-generated method stub
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
}

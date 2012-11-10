/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.BuslineResultLineFragment.ViewHolder;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;
import com.tigerknows.view.TrafficResultFragment.ChildView;

public class BuslineResultStationFragment extends BaseFragment {

    public BuslineResultStationFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }


    private SpringbackListView mResultLsv;
    
    private StationAdapter mStationAdapter;
    
    private List<Station> mStationList = new ArrayList<Station>();

    private TextView mCommentTxv = null;

    private BuslineQuery mBuslineQuery;
    
    private BuslineModel mBuslineModel;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                mResultLsv.getView(false).performClick();
            }
        }
    };

    /*
     * 用于控制序号图片显示
     */
    private ChildView downView;
    
    int focusedIndex = Integer.MAX_VALUE;
    
    private static final String TAG = "BuslineResultStationFragment";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficStation;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.busline_result, container, false);
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mRightTxv.setVisibility(View.GONE);

        mCommentTxv.setText(mContext.getString(R.string.busline_result_title, mBuslineQuery.getKeyword(), 
        		mBuslineModel.getTotal()));
    	mTitleTxv.setText(mContext.getString(R.string.title_station_result));
    	
    	mStationAdapter.notifyDataSetChanged();
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
    /**
     * 找出布局中将要使用的控件
     */
    protected void findViews() {
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View v =  mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mCommentTxv = (TextView)mRootView.findViewById(R.id.comment_txv);
    }
    
    protected void setListener() {
        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader) {
                    return;
                }
            	if (mStationList.size() >= mBuslineModel.getTotal()) {
                    return;
                }
                turnPage();
            }
        });
        
    	mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                // TODO Auto-generated method stub
				focusedIndex = position;
				mStationAdapter.notifyDataSetChanged();
                final Station station = (Station)mStationList.get(position);
                String busLine = station.getName();
                if (!TextUtils.isEmpty(busLine)) {
                    mActionLog.addAction(ActionLog.TrafficStationSelect, position);
                    submitBuslineQuery(busLine);
                }
            }
            
        });
        
    	mResultLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView arg0, int arg1) {
            	if (downView != null) {
                	ViewHolder viewHandler = (ViewHolder)downView.getTag();
    				if (viewHandler.position != focusedIndex) {
                		viewHandler.index.setBackgroundResource(R.drawable.btn_index);
                	}
            	}
            }
            
            @Override
            public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
            }
        });
    	
    	mResultLsv.setOnTouchListener(new OnTouchListener() {
 			
 			@Override
 			public boolean onTouch(View v, MotionEvent event) {
 				// TODO Auto-generated method stub
 				if ((event.getAction() == MotionEvent.ACTION_UP) && (downView != null)) {
 					if (downView.isSelected() || downView.isFocused() || downView.isPressed()) {
 						LogWrapper.d(TAG, "set index bg: " + "bg_index_focused");
 						ViewHolder viewHandler = (ViewHolder)downView.getTag();
 						viewHandler.index.setBackgroundResource(R.drawable.bg_index_focused);
 					}
 				}
 				return false;
 			}
 		});
    }
    
    /**
     * 翻页请求
     * 根据当前状态来区分是 站点翻页请求 或是 线路翻页请求
     */
    private void turnPage() {
        synchronized (this) {
        if (mBuslineQuery == null) {
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
    	BuslineQuery buslineQuery = new BuslineQuery(mContext);
    	buslineQuery.setup(mBuslineQuery.getCityId(), mBuslineQuery.getKeyword(), mStationList.size(), true, getId(), null);
    	mSphinx.queryStart(buslineQuery);
        mActionLog.addAction(ActionLog.TrafficStationPage);
        }
    }

    /**
     * 判断有无数据返回并根据内容设置当前状态
     * @param poiQuery 查询结果
     */
    public void setData(BuslineQuery buslineQuery) {
        mResultLsv.onRefreshComplete(false);
        mResultLsv.setFooterSpringback(false);
        mBuslineQuery = buslineQuery;
        mBuslineModel = mBuslineQuery.getBuslineModel();
        
        if (mBuslineQuery.isTurnPage()) {
            List<Station> list = mBuslineModel.getStationList();
            if (list != null) {
                mStationList.addAll(list);
                mStationAdapter.notifyDataSetChanged();
            }
        }else{
            mStationAdapter = null;
            mStationList = null;
            List<Station> list = mBuslineModel.getStationList();
            if (list != null) {
                mStationList = list;
                mStationAdapter = new StationAdapter();
                mResultLsv.setAdapter(mStationAdapter);
            }

            focusedIndex = Integer.MAX_VALUE;
        }

        if (mStationList.size() >= mBuslineModel.getTotal()) {
            mResultLsv.setFooterSpringback(false);
        } else {
            mResultLsv.setFooterSpringback(true);
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }


    public static class ViewHolder {
        public TextView text;
        public TextView summary;
        public TextView index;
        public int position;
    }

    /**
     * 站点列表数据源
     * @author linqingzu
     */
    private class StationAdapter extends BaseAdapter{
    	
        private static final int sResource = R.layout.traffic_group_busline;
        
        public StationAdapter() {
            super();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            //======为什么会跑出一个padding来呢?======
        	ViewHolder stationHolder = null;
            if(convertView == null) {
//                convertView = mLayoutInflater.inflate(sResource, parent, false);
            	convertView = new ChildView(mContext);
                
                stationHolder = new ViewHolder();
                stationHolder.text = (TextView)convertView.findViewById(R.id.text1);
                stationHolder.summary = (TextView)convertView.findViewById(R.id.text2);
                stationHolder.index = (TextView)convertView.findViewById(R.id.index);
                stationHolder.position = position;
                
                convertView.setTag(stationHolder);
            }else {
                stationHolder = (ViewHolder)convertView.getTag();
            }
            
            final Station station = (Station)getItem(position);

            stationHolder.text.setText(station.getName());
                        
            //在List中每条线路(除环路外)会出现两次,表示往返,使用Set作为中间转化去重.
            List<String> list = station.getLineList();
            Set<String> lines = new HashSet<String>(list);
            list = new ArrayList<String>(lines);
            
            StringBuilder s = new StringBuilder();
            if (list != null) {
                for (String str : list) {
                    s.append(",");
                    s.append(str);
                }
            }
            if (s.length() > 1) {
                stationHolder.summary.setText(s.substring(1));
            } else {
                stationHolder.summary.setVisibility(View.GONE);
            }

            stationHolder.index.setText(String.valueOf(position+1));
            
            if (focusedIndex == position) {
            	stationHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
			} else {
				stationHolder.index.setBackgroundResource(R.drawable.btn_index);
			}
            
            stationHolder.position = position;
            
            return convertView;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mStationList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mStationList.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    
    /**
     * 在站点列表中点击某站点名后重新发出线路查询请求
     * @param keyword 站点名
     */
    protected void submitBuslineQuery(String keyword) {
        
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(mBuslineQuery.getCityId(), keyword, 0, false, getId(), mContext.getString(R.string.doing_and_wait));
        mSphinx.queryStart(buslineQuery); 
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BuslineQuery baseQuery = (BuslineQuery)tkAsyncTask.getBaseQuery();
        queryBuslineEnd(baseQuery);
    }
    
    class ChildView extends RelativeLayout {

    	public ChildView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
			mLayoutInflater.inflate(R.layout.traffic_group_busline, this, true);
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
    
    public void queryBuslineEnd(BuslineQuery buslineQuery) {
    	
        BuslineModel buslineModel = buslineQuery.getBuslineModel();
        
        if (buslineModel == null) {
        	mActionLog.addAction(ActionLog.TrafficQueryNoResultB);
        	mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        } if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE 
        		|| buslineModel.getType() == BuslineModel.TYPE_STATION){
        	if (((buslineModel.getLineList() == null || buslineModel.getLineList().size() <= 0) && 
            (buslineModel.getStationList() == null || buslineModel.getStationList().size() <= 0))) {
        		mActionLog.addAction(ActionLog.TrafficQueryNoResultB);
        		mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        	} else {
        		if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
        			mActionLog.addAction(ActionLog.TrafficQueryResultB, buslineModel.getLineList().size());
        			mSphinx.getBuslineResultLineFragment().setData(buslineQuery);
        			mSphinx.showView(R.id.view_busline_line_result);
        		} else if (buslineModel.getType() == BuslineModel.TYPE_STATION) {
        			mActionLog.addAction(ActionLog.TrafficQueryResultB, buslineModel.getStationList().size());
        			mSphinx.getBuslineResultStationFragment().setData(buslineQuery);
        		}        		
        	}
        }
    }
}

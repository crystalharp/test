/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

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
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.POIResultFragment;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

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
    
    private View mLoadingView = null;
    
    private View mAddMerchantFootView = null;
    
    private View mCurrentFootView;
    
    private DataQuery mDataQuery;
    
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
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_busline_result, container, false);
        findViews();
        setListener();
        
        mStationAdapter = new StationAdapter();
        mResultLsv.setAdapter(mStationAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mRightBtn.setVisibility(View.GONE);

        mCommentTxv.setText(mContext.getString(R.string.busline_result_title, mBuslineQuery.getKeyword(), 
        		mBuslineModel.getTotal()));
    	mTitleBtn.setText(mContext.getString(R.string.title_station_result));
    	
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        
        if (mDismissed) {
            mResultLsv.setSelectionFromTop(0, 0);
        }
    }
    
    /**
     * 找出布局中将要使用的控件
     */
    protected void findViews() {
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        mLoadingView =  mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(mLoadingView);
        mCurrentFootView = mLoadingView;
        mAddMerchantFootView = mLayoutInflater.inflate(R.layout.poi_list_item_add_merchant, null);
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
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
                    submitBuslineQuery(busLine);
                } else if (mResultLsv.isFooterSpringback() == false && mDataQuery != null) {
                    mSphinx.uiStackRemove(R.id.view_traffic_busline_station_result);
                    int poiResultFragmentId = mSphinx.getPOIResultFragmentID();
                    POIResultFragment poiResultFragment = (POIResultFragment) mSphinx.getFragment(poiResultFragmentId);
                    poiResultFragment.setData(mDataQuery);
                    mSphinx.showView(poiResultFragmentId);
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
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);
        }
    }
    
    public void setData(BuslineQuery buslineQuery) {
        setData(buslineQuery, null);
    }

    /**
     * 判断有无数据返回并根据内容设置当前状态
     * @param poiQuery 查询结果
     */
    public void setData(BuslineQuery buslineQuery, DataQuery dataQuery) {
        mResultLsv.onRefreshComplete(false);
        mResultLsv.setFooterSpringback(false);
        mBuslineQuery = buslineQuery;
        mBuslineModel = mBuslineQuery.getBuslineModel();
        mDataQuery = dataQuery;
        
        if (mBuslineQuery.isTurnPage()) {
            if (mBuslineQuery.getBuslineModel() == null) {
                mResultLsv.setFooterLoadFailed(true);
                return;
            }
            List<Station> list = mBuslineModel.getStationList();
            if (list != null) {
                mStationList.addAll(list);
                mStationAdapter.notifyDataSetChanged();
            }
        }else{
            mStationList.clear();
            List<Station> list = mBuslineModel.getStationList();
            if (list != null) {
                mStationList.addAll(list);
            }

            focusedIndex = Integer.MAX_VALUE;
        }

        mStationAdapter.notifyDataSetChanged();
        if (mStationList.size() >= mBuslineModel.getTotal()) {
            mResultLsv.setFooterSpringback(false);
            if (mDataQuery != null) {
                if (mCurrentFootView != mAddMerchantFootView) {
                    mResultLsv.removeFooterView(mAddMerchantFootView);
                    mResultLsv.removeFooterView(mLoadingView);
                    mResultLsv.addFooterView(mAddMerchantFootView, false);
                    mCurrentFootView = mAddMerchantFootView;
                }
            } else {
                if (mCurrentFootView != mLoadingView) {
                    int state = mResultLsv.getState(false);
                    mResultLsv.removeFooterView(mAddMerchantFootView);
                    mResultLsv.removeFooterView(mLoadingView);
                    mResultLsv.addFooterView(mLoadingView);
                    mCurrentFootView = mLoadingView;
                    mResultLsv.changeHeaderViewByState(false, state);
                }
            }
        } else {
            mResultLsv.setFooterSpringback(true);
            if (mCurrentFootView != mLoadingView) {
                int state = mResultLsv.getState(false);
                mResultLsv.removeFooterView(mAddMerchantFootView);
                mResultLsv.removeFooterView(mLoadingView);
                mResultLsv.addFooterView(mLoadingView);
                mCurrentFootView = mLoadingView;
                mResultLsv.changeHeaderViewByState(false, state);
            }
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
    
    void queryBuslineEnd(BuslineQuery buslineQuery) {
    	
        BuslineModel buslineModel = buslineQuery.getBuslineModel();

        
        if (buslineQuery.isTurnPage() && buslineModel == null) {
            mResultLsv.setFooterLoadFailed(true);
            return;
        }
        
        if (buslineModel == null) {
        	mSphinx.showTip(R.string.network_failed, Toast.LENGTH_SHORT);
        } else if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE 
        		|| buslineModel.getType() == BuslineModel.TYPE_STATION){
        	if (((buslineModel.getLineList() == null || buslineModel.getLineList().size() <= 0) && 
            (buslineModel.getStationList() == null || buslineModel.getStationList().size() <= 0))) {
        		mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        	} else {
        		if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
        			mSphinx.getBuslineResultLineFragment().setData(buslineQuery, mDataQuery);
        			mSphinx.showView(R.id.view_traffic_busline_line_result);
        		} else if (buslineModel.getType() == BuslineModel.TYPE_STATION) {
        			mSphinx.getBuslineResultStationFragment().setData(buslineQuery, mDataQuery);
        		}        		
        	}
        }
    }
}

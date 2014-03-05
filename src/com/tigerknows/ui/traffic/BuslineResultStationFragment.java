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
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Alarm;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.service.AlarmService;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.ShareTextUtil;
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
        
        if (mBuslineQuery != null && TextUtils.isEmpty(mBuslineQuery.getKeyword()) == false) {
            mTitleBtn.setText(getString(R.string.title_station_result));
            mCommentTxv.setText(getString(R.string.busline_result_title, mBuslineQuery.getKeyword(), 
                    mBuslineModel.getTotal()));
            mCommentTxv.setVisibility(View.VISIBLE);
        } else {
            mTitleBtn.setText(getString(R.string.nearby_bus_station));
            mCommentTxv.setVisibility(View.GONE);
        }
        
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
    @Override
    protected void findViews() {
        super.findViews();
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View v =  mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mCommentTxv = (TextView)mRootView.findViewById(R.id.comment_txv);
    }

    @Override
    protected void setListener() {
        super.setListener();
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
                    if (mSphinx.uiStackContains(R.id.view_alarm_add)) {
                        Alarm alarm = new Alarm(mSphinx);
                        alarm.setName(station.getName());
                        alarm.setPosition(station.getPosition());
                        Alarm.writeAlarm(mSphinx, alarm, true);
                        AlarmService.start(mSphinx, true);
                        mSphinx.uiStackClearTop(R.id.view_alarm_list);
                    } else {
                        submitBuslineQuery(busLine);
                    }
                }
            }
            
        });
        
    	mResultLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView arg0, int arg1) {
            	if (downView != null) {
                	ViewHolder viewHandler = (ViewHolder)downView.getTag();
    				if (viewHandler.position != focusedIndex) {
//                		viewHandler.index.setBackgroundResource(R.drawable.btn_index);
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
// 						viewHandler.index.setBackgroundResource(R.drawable.bg_index_focused);
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
    	buslineQuery.setup(mBuslineQuery.getKeyword(), mStationList.size(), true, getId(), null);
    	buslineQuery.setType(mBuslineQuery.getType());
        buslineQuery.setPosition(mBuslineQuery.getPosition());
    	buslineQuery.setCityId(mBuslineQuery.getCityId());
    	mSphinx.queryStart(buslineQuery);
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);
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
        } else {
            mResultLsv.setFooterSpringback(true);
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }


    public static class ViewHolder {
        public TextView distance;
        public TextView text;
        public TextView summary;
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
                stationHolder.distance = (TextView)convertView.findViewById(R.id.text0);
                stationHolder.text = (TextView)convertView.findViewById(R.id.text1);
                stationHolder.summary = (TextView)convertView.findViewById(R.id.text2);
                stationHolder.position = position;
                
                convertView.setTag(stationHolder);
            }else {
                stationHolder = (ViewHolder)convertView.getTag();
            }
            
            final Station station = (Station)getItem(position);

            stationHolder.text.setText(station.getName());
            
            Position pos1 = mBuslineQuery.getPosition();
            Position pos2 = station.getPosition();
            if (pos1 != null && pos2 != null) {
                stationHolder.distance.setText(ShareTextUtil.getPlanLength(mSphinx, (int) Position.distanceBetween(pos1, pos2)));
                stationHolder.distance.setVisibility(View.VISIBLE);
            } else {
                stationHolder.distance.setVisibility(View.GONE);
            }
                        
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

            if (focusedIndex == position) {
//            	stationHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
			} else {
//				stationHolder.index.setBackgroundResource(R.drawable.btn_index);
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
        buslineQuery.setup(keyword, 0, false, getId(), getString(R.string.doing_and_wait));
        buslineQuery.setCityId(mBuslineQuery.getCityId());
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
        dealWithBuslineResponse(mSphinx, baseQuery, mActionTag, mResultLsv);
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
}

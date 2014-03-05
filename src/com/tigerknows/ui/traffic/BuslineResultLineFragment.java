/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Alarm;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.service.AlarmService;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.StringArrayAdapter;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

public class BuslineResultLineFragment extends BaseFragment {
    
    public static final String BUSLINE_TURNPAGE = "BUSLINE_TURNPAGE";

    public BuslineResultLineFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private SpringbackListView mResultLsv;
        
    private LineAdapter mLineAdapter = null;

    private List<Line> mLineList = new ArrayList<Line>();
    
    private TextView mCommentTxv = null;

    private BuslineQuery mBuslineQuery;
    
    private BuslineModel mBuslineModel;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                turnPage();
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
    
    private static final String TAG = "BuslineResultLineFragment";
    
    public List<Line> getLineList() {
        return  mLineList;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficBusline;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_busline_result, container, false);
        findViews();
        setListener();
        
        mLineAdapter = new LineAdapter();
        mResultLsv.setAdapter(mLineAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBuslineQuery != null && TextUtils.isEmpty(mBuslineQuery.getKeyword()) == false) {
            mTitleBtn.setText(getString(R.string.title_busline_result));
            mCommentTxv.setText(getString(R.string.busline_result_title, mBuslineQuery.getKeyword(), 
                    mBuslineModel.getTotal()));
            mCommentTxv.setVisibility(View.VISIBLE);
        } else {
            mTitleBtn.setText(getString(R.string.nearby_bus_line));
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
        mLoadingView =  mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(mLoadingView);
        mCurrentFootView = mLoadingView;
        mAddMerchantFootView = mLayoutInflater.inflate(R.layout.traffic_busline_list_item_more_poi, null);
        mCommentTxv = (TextView)mRootView.findViewById(R.id.comment_txv);
    }

    @Override
    protected void setListener() {
        super.setListener();
		mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				if (mLineList != null && mLineList.size() > position) {
					mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);

                    if (mSphinx.uiStackContains(R.id.view_alarm_add)) {
                        showAlarmDialog(mSphinx, lineToPOIList(mLineList.get(position)));
                    } else {
                        focusedIndex = position;
    					mSphinx.getBuslineDetailFragment().setData(mLineList.get(position), position, mBuslineQuery.getKeyword());
    					mSphinx.showView(R.id.view_traffic_busline_detail);
                    }
				} else if (mResultLsv.isFooterSpringback() == false && mDataQuery != null) {
                    mActionLog.addAction(mActionTag + ActionLog.TrafficBuslineViewPOI);
                    DataQuery poiQuery = new DataQuery(mDataQuery);
                    poiQuery.removeParameter(DataQuery.SERVER_PARAMETER_EXT);
                    poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false, false, mDataQuery.getPOI());
                    mSphinx.queryStart(poiQuery);
                }
			}

        });

    	mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader || mBuslineModel == null) {
                    return;
                }
                turnPage();
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
        
	}

    /**
     * 翻页请求
     * 根据当前状态来区分是 站点翻页请求 或是 线路翻页请求
     */
    private void turnPage() {
    	synchronized (this) {
        if (mBuslineModel == null) {
            return;
        }
        if (mLineList.size() >= mBuslineModel.getTotal()) {
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        if (mDataQuery != null) {
            DataQuery dataQuery = new DataQuery(mDataQuery);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(mLineList.size()));
            dataQuery.addLocalParameter(BUSLINE_TURNPAGE, BUSLINE_TURNPAGE);
            dataQuery.setup(getId(), getId(), null, true, false, mDataQuery.getPOI());
            mSphinx.queryStart(dataQuery);
        } else {
            int cityId = mBuslineQuery.getCityId();
            BuslineQuery buslineQuery = new BuslineQuery(mContext);
            buslineQuery.setup(mBuslineQuery.getKeyword(), mLineList.size(), true, getId(), null);
            buslineQuery.setType(mBuslineQuery.getType());
            buslineQuery.setPosition(mBuslineQuery.getPosition());
            buslineQuery.setCityId(cityId);
            mSphinx.queryStart(buslineQuery);
    	}
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
        mDataQuery = dataQuery;
        if (mDataQuery != null) {
            buslineQuery = new BuslineQuery(mSphinx);
            buslineQuery.setup(dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_KEYWORD), 0, false, R.id.view_traffic_busline_line_result, null);
            Response response = mDataQuery.getResponse();
            if (response != null && response instanceof POIResponse) {
                buslineQuery.setBuslineModel(((POIResponse) response).getBuslineModel());
            }
            buslineQuery.setTurnPage(mDataQuery.isTurnPage());
        }
        mResultLsv.onRefreshComplete(false);
        mResultLsv.setFooterSpringback(false);
        mBuslineQuery = buslineQuery;
        BuslineModel buslineModel = mBuslineQuery.getBuslineModel();
        
        if (buslineModel != null) {
            mBuslineModel = buslineModel;
        }
        
        if (mBuslineQuery.isTurnPage()) {
            if (buslineModel == null) {
                mResultLsv.setFooterLoadFailed(true);
                return;
            }
        	List<Line> list = mBuslineModel.getLineList();
            if (list != null) {
            	mLineList.addAll(list);
                mLineAdapter.notifyDataSetChanged();
            }
        }else{
        	mLineList.clear();
        	List<Line> list = mBuslineModel.getLineList();
            if (list != null) {
            	mLineList.addAll(list);
            	mLineAdapter.notifyDataSetChanged();
            }
            
            focusedIndex = Integer.MAX_VALUE;
        }

        if (mLineList.size() >= mBuslineModel.getTotal()) {
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
                    mResultLsv.setFooterSpringback(false);
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
                mResultLsv.setFooterSpringback(true);
            }
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }

    public static class ViewHolder {
        public TextView text;
        public TextView summary;
        public int position;
    }

    /**
     * 线路列表数据源
     * @author linqingzu
     */
    private class LineAdapter extends BaseAdapter {
        
        public LineAdapter() {
            super();
        }

        @Override
        public Object getItem(int position) {
            return mLineList.get(position);
        }

        @Override
        public int getCount() {
            return mLineList.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
        	ViewHolder lineHolder = null;
            if(convertView == null) {
//                convertView = mLayoutInflater.inflate(mResource, parent, false);
            	convertView = new ChildView(mContext);
                
                lineHolder = new ViewHolder();
                lineHolder.text = (TextView)convertView.findViewById(R.id.text1);
                lineHolder.summary = (TextView)convertView.findViewById(R.id.text2);
                lineHolder.position = position;
                
                convertView.setTag(lineHolder);
            }else {
                lineHolder = (ViewHolder)convertView.getTag();
            }
            final Line line = (Line)getItem(position);
            
            lineHolder.text.setText(line.getName());
            lineHolder.summary.setText(getString(R.string.busline_line_listitem_title, 
            		line.getTime(), line.getLengthStr(mSphinx)));
            lineHolder.position = position;
            
            if (focusedIndex == position) {
//            	lineHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
			} else {
//				lineHolder.index.setBackgroundResource(R.drawable.btn_index);
			}
            
            return convertView;
        }

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);

        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        String apiType = baseQuery.getAPIType();
        if (BaseQuery.API_TYPE_BUSLINE_QUERY.equals(apiType)) {
            dealWithBuslineResponse(mSphinx, (BuslineQuery) baseQuery, mActionTag, mResultLsv);
        } else if (BaseQuery.API_TYPE_DATA_QUERY.equals(apiType)) {
            dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);
        }
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
    
    public List<Line> getData() {
        return mLineList;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mLineList.clear();
    }
    
    public static List<POI> lineToPOIList(Line line) {
        List<POI> poiList = new ArrayList<POI>();
        List<Station> list = line.getStationList();
        for(int i = 0, size = list.size(); i < size; i++) {
            poiList.add(list.get(i).toPOI());
        }
        return poiList;
    }

    public static void showAlarmDialog(final Sphinx sphinx, final List<POI> poiList){
        if (poiList.size() == 0) {
            return;
        }
        
        List<String> list = new ArrayList<String>();
        for(int i = 0, size = poiList.size(); i < size; i++) {
            list.add(poiList.get(i).getName());
        }
        final ArrayAdapter<String> adapter = new StringArrayAdapter(sphinx, list);
        View alterListView = sphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
        
        final ListView listView = (ListView) alterListView.findViewById(R.id.listview);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.getChoiceDialog(sphinx, alterListView, R.style.AlterChoiceDialog);
        
        TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
        titleTxv.setText(R.string.setting_alarm_station);
        
        Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
        button.setVisibility(View.GONE);
        
        dialog.show();
        
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
                Alarm alarm = new Alarm(sphinx);
                POI station = poiList.get(which);
                alarm.setName(station.getName());
                alarm.setPosition(station.getPosition());
                Alarm.writeAlarm(sphinx, alarm, sphinx.uiStackContains(R.id.view_alarm_add) ? R.string.alarm_add_success : R.string.alarm_add_success_and_view);
                AlarmService.start(sphinx, true);
                dialog.dismiss();
                
                if (sphinx.uiStackContains(R.id.view_alarm_add)) {
                    sphinx.uiStackClearTop(R.id.view_alarm_list);
                }
            }
        });
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
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

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficResultFragment.ChildView;
import com.tigerknows.ui.traffic.TrafficResultFragment.PlanViewHolder;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

public class BuslineResultLineFragment extends BaseFragment {

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
                mResultLsv.getView(false).performClick();
            }
        }
    };
    
    /*
     * 用于控制序号图片显示
     */
    private ChildView downView;
    
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
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mRightBtn.setVisibility(View.GONE);

        mCommentTxv.setText(mContext.getString(R.string.busline_result_title, mBuslineQuery.getKeyword(), 
        		mBuslineModel.getTotal()));
    	mTitleBtn.setText(mContext.getString(R.string.title_busline_result));
    	
        mLineAdapter.notifyDataSetChanged();
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
		// TODO Auto-generated method stub
		mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				
				if (mLineList != null && mLineList.size() > position) {
					mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
					focusedIndex = position;
					mSphinx.getBuslineDetailFragment().setData(mLineList.get(position), position);
					mSphinx.showView(R.id.view_traffic_busline_detail);
	            }
			}

        });

    	mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader || mBuslineModel == null) {
                    return;
                }
            	if (mLineList.size() >= mBuslineModel.getTotal()) {
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
        if (mBuslineModel == null) {
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(mBuslineQuery.getCityId(), mBuslineQuery.getKeyword(), mLineList.size(), true, getId(), null);
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
        	List<Line> list = mBuslineModel.getLineList();
            if (list != null) {
            	mLineList.addAll(list);
                mLineAdapter.notifyDataSetChanged();
            }
        }else{
        	mLineAdapter = null;
        	mLineList = null;
        	List<Line> list = mBuslineModel.getLineList();
            if (list != null) {
            	mLineList = list;
            	mLineAdapter = new LineAdapter();
                mResultLsv.setAdapter(mLineAdapter);
            }
            
            focusedIndex = Integer.MAX_VALUE;
        }

        if (mLineList.size() >= mBuslineModel.getTotal()) {
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
     * 线路列表数据源
     * @author linqingzu
     */
    private class LineAdapter extends BaseAdapter {
        
        private static final int mResource = R.layout.traffic_group_busline;

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
                lineHolder.index = (TextView)convertView.findViewById(R.id.index);
                lineHolder.position = position;
                
                convertView.setTag(lineHolder);
            }else {
                lineHolder = (ViewHolder)convertView.getTag();
            }
            final Line line = (Line)getItem(position);
            
            lineHolder.text.setText(line.getName());
            lineHolder.summary.setText(mContext.getString(R.string.busline_line_listitem_title, 
            		line.getTime(), line.getLengthStr(mSphinx)));
            lineHolder.index.setText(String.valueOf(position+1));
            lineHolder.position = position;
            
            if (focusedIndex == position) {
            	lineHolder.index.setBackgroundResource(R.drawable.bg_index_focused);
			} else {
				lineHolder.index.setBackgroundResource(R.drawable.btn_index);
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
        
        if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE){
        	if (buslineModel.getLineList() == null || buslineModel.getLineList().size() <= 0) {
        		mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        	} else {
    			mSphinx.getBuslineResultLineFragment().setData(buslineQuery);
        	}
        }
    }
    
    public List<Line> getData() {
        return mLineList;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mLineList = null;
    }
}

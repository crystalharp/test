/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.User;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.BaseList;
import com.tigerknows.model.DataQuery.DianyingResponse;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.TuangouResponse;
import com.tigerknows.model.DataQuery.YanchuResponse;
import com.tigerknows.model.DataQuery.ZhanlanResponse;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList.DiscoverCategory;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.IPagerListCallBack;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupWindow.OnDismissListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class DiscoverListFragment extends DiscoverBaseFragment implements View.OnClickListener, FilterListView.CallBack, SpringbackListView.IPagerList, RetryView.CallBack {

    public DiscoverListFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    /**
     * Button for Dingdan in Tuangou list
     */
    private ImageButton mDingdanBtn;
    
    /**
     * View containing the filter criterias, parent and child.
     */
    private FilterListView mFilterListView = null;
    
    /**
     * The filter list control view containing three buttons
     */
    private ViewGroup mFilterControlView = null;

    /**
     * The list view containing the result of the list view
     */
    private SpringbackListView mResultLsv = null;

    /**
     * The view showing that the query is in progress
     */
    private QueryingView mQueryingView = null;
    
    /**
     * The text that shows the user that query is in progress
     */
    private TextView mQueryingTxv = null;
    
    /**
     * The view that shows the query result is empty
     */
    private View mEmptyView = null;
    
    /**
     * The text view that shows the query result is empty
     */
    private TextView mEmptyTxv = null;
    
    /**
     * The view the promote the user to tap the screen to retry
     */
    private RetryView mRetryView;
    
    private DataQuery mDataQuery;
    
    /**
     * Different state of the list fragment
     */
    static final int STATE_QUERYING = 0;
    static final int STATE_ERROR = 1;
    static final int STATE_EMPTY = 2;
    static final int STATE_LIST = 3;
    
    private int mState = STATE_QUERYING;
    
    /**
     * List of Discover entities and the corresponding adapter
     */
    private List<Tuangou> mTuangouList = new ArrayList<Tuangou>();
    
    private TuangouAdapter mTuangouAdapter;
    
    private List<Dianying> mDianyingList = new ArrayList<Dianying>();
    
    private DianyingAdapter mDianyingAdapter;
    
    private List<Yanchu> mYanchuList = new ArrayList<Yanchu>();
    
    private YanchuAdapter mYanchuAdapter;
    
    private List<Zhanlan> mZhanlanList = new ArrayList<Zhanlan>();
    
    private ZhanlanAdapter mZhanlanAdapter;
    
    /**
     * Filter list used in the current list fragment
     */
    private List<Filter> mFilterList = new ArrayList<Filter>();
    
    private String mFilterArea;
    
    private BaseList mList;
    
    private String mDataType;
    
    /**
     * 发现分类列表
     */
    private List<DiscoverCategory> mDiscoverCategoryList = new ArrayList<DiscoverCategory>();
    
    /**
     * Adapter used to choose between different discover categroy in list fragmenti
     */
    private TitlePopupArrayAdapter mTitlePopupArrayAdapter;
    
    private OnItemClickListener mTitlePopupOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
        	//close the popup window
            mTitleFragment.dismissPopupWindow();
            
            TitlePopupArrayAdapter titlePopupArrayAdapter = (TitlePopupArrayAdapter) adapterView.getAdapter();
            DiscoverCategory discoverCategory = titlePopupArrayAdapter.getItem(position);
            
            //Build the query
            DataQuery dataQuery = new DataQuery(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, discoverCategory.getType());
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(),
                    R.id.view_discover_home, R.id.view_discover_list, null, false, false,
                    mSphinx.getPOI());
            mSphinx.queryStart(dataQuery);
            
            setup();
            onResume();
            mActionLog.addAction(mActionTag + ActionLog.PopupWindowTitle + ActionLog.ListViewItem, position, discoverCategory.getType());
        }
    };
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                mResultLsv.getView(false).performClick();
            }
        }
    };
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
            mSphinx.getHandler().post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            if (mSphinx.uiStackPeek() == getId() && mSphinx.isFinishing() == false) {
                getAdapter().notifyDataSetChanged();
            }
        }
    };
    
    SpringbackListView.IPagerListCallBack mIPagerListCallBack = null;
    
    public void setup() {
        DataQuery lastDataQuery = (DataQuery) this.mTkAsyncTasking.getBaseQuery();
        if (lastDataQuery == null) {
            return;
        }
        if (lastDataQuery.getSourceViewId() != getId()
                || lastDataQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_DATA_TYPE).equals(mDataType) == false) {
            mDataQuery = null;
            mFilterControlView.setVisibility(View.GONE);
            mFilterList.clear();
        }
        
        String str = mContext.getString(R.string.loading);
        mQueryingTxv.setText(str);
        
        mDataType = lastDataQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            if (mTuangouAdapter == null) {
                mTuangouAdapter = new TuangouAdapter(mSphinx, mTuangouList);
            }
            mResultLsv.setAdapter(mTuangouAdapter);
            mDingdanBtn.setVisibility(View.INVISIBLE);
            mActionTag = ActionLog.TuangouList;
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            if (mDianyingAdapter == null) {
                mDianyingAdapter = new DianyingAdapter(mSphinx, mDianyingList);
            }
            mResultLsv.setAdapter(mDianyingAdapter);
            mDingdanBtn.setVisibility(View.INVISIBLE);
            mActionTag = ActionLog.DianyingList;
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            if (mYanchuAdapter == null) {
                mYanchuAdapter = new YanchuAdapter(mSphinx, mYanchuList);
            }
            mResultLsv.setAdapter(mYanchuAdapter);
            mDingdanBtn.setVisibility(View.INVISIBLE);
            mActionTag = ActionLog.YanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            if (mZhanlanAdapter == null) {
                mZhanlanAdapter = new ZhanlanAdapter(mSphinx, mZhanlanList);
            }
            mResultLsv.setAdapter(mZhanlanAdapter);
            mDingdanBtn.setVisibility(View.INVISIBLE);
            mActionTag = ActionLog.ZhanlanList;
        }

        mState = STATE_QUERYING;
        updateView();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.DianyingList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        mRootView = mLayoutInflater.inflate(R.layout.poi_result, container, false);

        findViews();
        setListener();
        
        //mResultLsv.setDivider(mSphinx.getResources().getDrawable(R.drawable.bg_broken_line));
        mTitlePopupArrayAdapter = new TitlePopupArrayAdapter(mSphinx, mDiscoverCategoryList);
        
        return mRootView;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void dismiss() {
        super.dismiss();
        List list = getList();
        if (list != null) {
            list.clear();
            ArrayAdapter adapter = getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        mDataQuery = null;
    }
    
    protected void findViews() {
        mDingdanBtn = (ImageButton) mRootView.findViewById(R.id.dingdan_btn);
        mFilterControlView = (ViewGroup)mRootView.findViewById(R.id.filter_control_view);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
    }

    private void updateChangeciOption(List<Dianying> list){
        int changciOption = Changci.OPTION_DAY_TODAY;
        for(Filter filter : mFilterList) {
            if (filter.getKey() == FilterCategoryOrder.FIELD_LIST_CATEGORY) {
                String str = getSelectFilterName(filter);
                if (str.contains(mSphinx.getString(R.string.tomorrow_char))) {
                    changciOption = Changci.OPTION_DAY_TOMORROW;
                } else if (str.contains(mSphinx.getString(R.string.after_tomorrow_char))) {
                    changciOption = Changci.OPTION_DAY_AFTER_TOMORROW;
                }
            }
        }
        for (int i = 0, size=list.size(); i < size; i++) {
        	list.get(i).getYingxun().setChangciOption(changciOption);
		}
    }
    
    protected void setListener() {
        mRetryView.setCallBack(this, mActionTag);
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    Object object = adapterView.getAdapter().getItem(position);
                    if (object != null) {
                        if (object instanceof Tuangou) {
                            Tuangou tagret = (Tuangou) object;
//                            tagret.getFendian().setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                            mSphinx.showView(R.id.view_discover_tuangou_detail);
                            mSphinx.getTuangouDetailFragment().setData(mTuangouList, position, DiscoverListFragment.this);
                        } else if (object instanceof Yanchu){
                            Yanchu tagret = (Yanchu) object;
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                            mSphinx.showView(R.id.view_discover_yanchu_detail);
                        	mSphinx.getYanchuDetailFragment().setData(mYanchuList, position, DiscoverListFragment.this);
                        } else if (object instanceof Dianying){
                            Dianying tagret = (Dianying) object;
//                            tagret.getYingxun().setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                        	mSphinx.showView(R.id.view_discover_dianying_detail);
                        	mSphinx.getDianyingDetailFragment().setData(mDianyingList, position, DiscoverListFragment.this);
                        } else if (object instanceof Zhanlan){
                        	Zhanlan tagret = (Zhanlan) object;
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                            mSphinx.showView(R.id.view_discover_zhanlan_detail);
                        	mSphinx.getZhanlanDetailFragment().setData(mZhanlanList, position, DiscoverListFragment.this);
                        }
                    }
                }
            }
            
        });

        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage(null);
            }
        });
        mDingdanBtn.setOnClickListener(this);
    }
    
    public static String getSelectFilterName(Filter filter) {
        String title = null;
        if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            int i = 0;
            for(Filter chidrenFilter : chidrenFilterList) {
                if (i == 0) {
                    title = chidrenFilter.getFilterOption().getName();
                }
                i++;
                if (chidrenFilter.isSelected()) {
                    title = chidrenFilter.getFilterOption().getName();
                    break;
                } else {
                    
                    List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                    for(Filter chidrenFilter1 : chidrenFilterList1) {
                        if (chidrenFilter1.isSelected()) {
                            title = chidrenFilter.getFilterOption().getName();
                            break;
                        }
                    }
                }
            }   

        }
        return title;
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            DataQuery lastDataQuery = mDataQuery;
            if (mState != STATE_QUERYING && mState != STATE_LIST && lastDataQuery != null) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                mState = STATE_LIST;
                updateView();
                refreshFilter(lastDataQuery.getFilterList());
                return true;
            }
        }
        return false;
    }
    
    private void refreshResultText(DataQuery dataQuery) {
        String str = null;
        if (dataQuery != null && mList != null) {
            str = mList.getMessage();
        }

        if (TextUtils.isEmpty(str)) {
            str = mContext.getString(R.string.no_result);
        }
        mEmptyTxv.setText(str);
    }
    
    private void refreshFilter(List<Filter> filterList) {
        synchronized (mFilterList) {
            if (mFilterList != filterList) {
                mFilterList.clear();
                if (filterList != null) {
                    for(Filter filter : filterList) {
                        mFilterList.add(filter.clone());
                    }
                }
                FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onResume() {
        super.onResume();
        mIPagerListCallBack = null;
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            mTitleBtn.setText(R.string.tuangou_list);
            mRightBtn.setVisibility(View.VISIBLE);
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            mTitleBtn.setText(R.string.dianying_list);
            mRightBtn.setVisibility(View.GONE);
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            mTitleBtn.setText(R.string.yanchu_list);
            mRightBtn.setVisibility(View.VISIBLE);
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            mTitleBtn.setText(R.string.zhanlan_list);
            mRightBtn.setVisibility(View.VISIBLE);
        }
        mRightBtn.setBackgroundResource(R.drawable.btn_view_map);
        mRightBtn.setOnClickListener(this);
        
        List<DiscoverCategory> list = mSphinx.getDiscoverFragment().getDiscoverCategoryList();
        mDiscoverCategoryList.clear();
        for(int i = 0, size=list.size(); i < size; i++) {
            DiscoverCategory discoverCategory = list.get(i);
            if (discoverCategory.getNumCity() > 0 || discoverCategory.getNumNearby() > 0) {
                if (mDiscoverCategoryList.contains(discoverCategory) == false) {
                    mDiscoverCategoryList.add(discoverCategory);
                }
            }
        }
        mTitleBtn.setBackgroundResource(R.drawable.btn_title_popup);
        mTitleBtn.setOnClickListener(this);

        if (isReLogin()) {
            return;
        }
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 500);
        }
        ArrayAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    /**
     * In state querying, the button shouldn't be clickable 
     */
    private void updateTitleButtonState(){
    	if(mTitleBtn == null){
    		return;
    	}
        if (mState == STATE_QUERYING) {
            mTitleBtn.setClickable(false);
            mTitleBtn.setBackgroundResource(R.color.transparent);
        } else {
            mTitleBtn.setClickable(true);
            mTitleBtn.setBackgroundResource(R.drawable.btn_title_popup);
        }
    }
    
    private void updateView() {
    	
    	updateTitleButtonState();
    	
        if (mState == STATE_QUERYING) {
            mQueryingView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
        } else {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
            if (mRightBtn != null) {
                if (getList().size() > 0) {
                    if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType) && mSphinx.uiStackPeek() == R.id.view_discover_list) {
                        mRightBtn.setVisibility(View.GONE);
                    }else{
                    	mRightBtn.setVisibility(View.VISIBLE);
                    }
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
                        if (TKConfig.getPref(mSphinx, TKConfig.PREFS_HINT_POI_LIST) == null) {
                            TKConfig.setPref(mSphinx, TKConfig.PREFS_HINT_POI_LIST, "1");
                            TKConfig.setPref(mSphinx, TKConfig.PREFS_HINT_DISCOVER_TUANGOU_DINGDAN, "1");
                            mSphinx.showHint(TKConfig.PREFS_HINT_DISCOVER_TUANGOU_LIST, R.layout.hint_discover_tuangou_list);
                        } else {
                            mSphinx.showHint(TKConfig.PREFS_HINT_DISCOVER_TUANGOU_DINGDAN, R.layout.hint_discover_tuangou_dingdan);
                        }
                    } else {
                        mSphinx.showHint(TKConfig.PREFS_HINT_POI_LIST, R.layout.hint_poi_list);
                    }
                } else {
                    mRightBtn.setVisibility(View.GONE);
                }
            }
        }
    }
    
    private void turnPage(String tip){
        synchronized (this) {
        DataQuery lastDataQuery = mDataQuery;
        if (mState != STATE_LIST || mResultLsv.isFooterSpringback() == false || lastDataQuery == null || mList == null || mList.getTotal() <= getList().size()) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);

        DataQuery dataQuery = new DataQuery(mContext);
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(getList().size()));
        int dianyingSize = mDianyingList.size();
        if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType) && dianyingSize > 0) {
            criteria.put(DataQuery.SERVER_PARAMETER_DIANYING_UUID, mDianyingList.get(dianyingSize-1).getUid());
        }
        dataQuery.setup(criteria, lastDataQuery.getCityId(), getId(), getId(), tip, true, true, lastDataQuery.getPOI());
        mSphinx.queryStart(dataQuery);
        }
    }

    public void viewMap(int firstVisiblePosition, int lastVisiblePosition) {
        if (mList == null) {
            return;            
        }
        int size = getList().size();
        int[] page = Utility.makePagedIndex(mResultLsv, size, firstVisiblePosition);
        
        int minIndex = page[0];
        int maxIndex = page[1];
        List<BaseData> dataList = new ArrayList<BaseData>();
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mTuangouList.size(); minIndex++) {
                Tuangou data = mTuangouList.get(minIndex);
                dataList.add(data);
            }
            mSphinx.getTuangouDetailFragment().setData(mTuangouList, page[2], DiscoverListFragment.this);
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mYanchuList.size(); minIndex++) {
                Yanchu data = mYanchuList.get(minIndex);
                dataList.add(data);
            }
            mSphinx.getYanchuDetailFragment().setData(mYanchuList, page[2], DiscoverListFragment.this);
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mZhanlanList.size(); minIndex++) {
                Zhanlan data = mZhanlanList.get(minIndex);
                dataList.add(data);
            }
            mSphinx.getZhanlanDetailFragment().setData(mZhanlanList, page[2], DiscoverListFragment.this);
        }
        if (dataList.isEmpty()) {
            return;
        }
        int name = R.string.tuangou_ditu;
        String actionTag = ActionLog.ResultMapTuangouList;
        if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            name = R.string.yanchu_ditu;
            actionTag = ActionLog.ResultMapYanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            name = R.string.zhanlan_ditu;
            actionTag = ActionLog.ResultMapZhanlanList;
        }
        mSphinx.showPOI(dataList, page[2]);
        mSphinx.getResultMapFragment().setData(mContext.getString(name), actionTag);
        mSphinx.showView(R.id.view_result_map);   
    }
    
    public void doFilter(String name) {
        FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
        
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            dismissPopupWindow();
            return;
        }

        DataQuery dataQuery = new DataQuery(mContext);

        POI requestPOI = lastDataQuery.getPOI();
        int cityId = lastDataQuery.getCityId();
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        dataQuery.setup(criteria, cityId, getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(dataQuery);
        setup();
        
        /*
         * First set up the views that is under the filter list
         * Then dismiss the PopupWindow to prevent the blink effect 
         * Of the reverse order.
         */
        dismissPopupWindow();
    }
    
    public void cancelFilter() {
        dismissPopupWindow();
        if (mResultLsv.isFooterSpringback() && mFilterListView.isTurnPaging()) {
            turnPage(null);
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.title_btn:
                if (mDiscoverCategoryList.size() > 0) {
                    mTitlePopupArrayAdapter.mSelectDataType = mDataType;
                    mTitleFragment.showPopupWindow(mTitlePopupArrayAdapter, mTitlePopupOnItemClickListener, mActionTag);
                    mTitlePopupArrayAdapter.notifyDataSetChanged();
                }
                break;
                
            case R.id.right_btn:
                if (mDataQuery == null || mState != STATE_LIST) {
                    return;
                }
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                viewMap(mResultLsv.getFirstVisiblePosition(), mResultLsv.getLastVisiblePosition());
                break;
                
            case R.id.dingdan_btn:
                mActionLog.addAction(mActionTag +  ActionLog.TuangouListDingdan);
                User user = Globals.g_User;
                if (user != null) {
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID, getId());
                    mSphinx.showView(R.id.activity_discover_shangjia_list, intent);
                } else {
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.activity_discover_shangjia_list);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
                    mSphinx.showView(R.id.activity_user_login, intent);
                }
                break;
                
            default:

                boolean turnPageing = (mResultLsv.getState(false) == SpringbackListView.REFRESHING);
                if (mState == STATE_QUERYING && turnPageing == false) {
                    return;
                }
                
                if (mTkAsyncTasking != null) {
                    mTkAsyncTasking.stop();
                }
                mResultLsv.onRefreshComplete(false);
                
                showFilterListView(mTitleFragment);

                byte key = (Byte)view.getTag();
                mFilterListView.setData(mFilterList, key, DiscoverListFragment.this, turnPageing, mActionTag);
        }
    }
    
    public class TuangouAdapter extends ArrayAdapter<Tuangou>{
        private static final int RESOURCE_ID = R.layout.discover_tuangou_list_item;
        String rmb;
        
        public TuangouAdapter(Context context, List<Tuangou> list) {
            super(context, RESOURCE_ID, list);
            rmb = context.getString(R.string.rmb_text);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            ImageView shangjiaMarkerImv = (ImageView) view.findViewById(R.id.shangjia_marker_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView orgPriceTxv = (TextView) view.findViewById(R.id.org_price_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView buyerNumTxv = (TextView) view.findViewById(R.id.buyer_num_txv);

            Tuangou tuangou = getItem(position);
            Drawable drawable = tuangou.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
            	if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
            		pictureImv.setBackgroundDrawable(null);
            	}
            	pictureImv.setBackgroundDrawable(drawable);
            } else {
                pictureImv.setBackgroundDrawable(null);
            }
            
            Shangjia shangjia = Shangjia.getShangjiaById(tuangou.getSource(), mSphinx, mLoadedDrawableRun);
            if (shangjia != null) {
                shangjiaMarkerImv.setImageDrawable(shangjia.getMarker());
            } else {
                shangjiaMarkerImv.setImageDrawable(null);
            }
            nameTxv.setText(tuangou.getShortDesc());
            priceTxv.setText(tuangou.getPrice());
            orgPriceTxv.setText(tuangou.getOrgPrice()+rmb);
            orgPriceTxv.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            String distance = tuangou.getFendian().getDistance();
            if (TextUtils.isEmpty(distance)) {
                distanceTxv.setVisibility(View.GONE);
            } else {
                distanceTxv.setText(distance);
                distanceTxv.setVisibility(View.VISIBLE);
            }
            buyerNumTxv.setText(String.valueOf(tuangou.getBuyerNum())+mSphinx.getString(R.string.people));
            
            return view;
        }
    }
    
    public class DianyingAdapter extends ArrayAdapter<Dianying>{
        private static final int RESOURCE_ID = R.layout.discover_dianying_list_item;
        
        public DianyingAdapter(Context context, List<Dianying> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.category_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);

            Dianying dianying = getItem(position);
            Drawable drawable = dianying.getPicture().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
            	if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
            		pictureImv.setBackgroundDrawable(null);
            	}
            	pictureImv.setBackgroundDrawable(drawable);
            } else {
                pictureImv.setBackgroundDrawable(null);
            }
            
            nameTxv.setText(dianying.getName());
            starsRtb.setProgress((int) dianying.getRank());
            String distance = dianying.getYingxun().getDistance();
            if (TextUtils.isEmpty(distance)) {
                distanceTxv.setVisibility(View.GONE);
            } else {
                //distanceTxv.setText(mSphinx.getString(R.string.dianying_detail_nearest, String.valueOf(dianying.getYingxun().getDistance())));
            	  String distanceStr = mSphinx.getString(R.string.dianying_detail_nearest, String.valueOf(dianying.getYingxun().getDistance()));

                Utility.formatText(distanceTxv, distanceStr, mSphinx.getString(R.string.dianying_detail_nearest, ""), R.color.black_dark);
                distanceTxv.setVisibility(View.VISIBLE);
            }
            
            addressTxv.setText(dianying.getTag());
            if (TextUtils.isEmpty(dianying.getLength())) {
            	dateTxv.setText(R.string.dianying_no_length_now);
            } else {
            	dateTxv.setText(String.valueOf(dianying.getLength()));
            }
            return view;
        }
    }
    
    public class YanchuAdapter extends ArrayAdapter<Yanchu>{
        private static final int RESOURCE_ID = R.layout.discover_yanchu_list_item;
        
        public YanchuAdapter(Context context, List<Yanchu> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);

            Yanchu yanchu = getItem(position);
            Drawable drawable = yanchu.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
            	if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
            		pictureImv.setBackgroundDrawable(null);
            	}
            	pictureImv.setBackgroundDrawable(drawable);
            } else {
                pictureImv.setBackgroundDrawable(null);
            }
            
            nameTxv.setText(yanchu.getName());
            starsRtb.setProgress((int) yanchu.getHot());
            addressTxv.setText(yanchu.getAddress());
            distanceTxv.setText(yanchu.getDistance());
            dateTxv.setText(yanchu.getTimeDesc());
            
            return view;
        }
    }
    
    public class ZhanlanAdapter extends ArrayAdapter<Zhanlan>{
        private static final int RESOURCE_ID = R.layout.discover_yanchu_list_item;
        
        public ZhanlanAdapter(Context context, List<Zhanlan> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

        	View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);

            Zhanlan yanchu = getItem(position);
            Drawable drawable = yanchu.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
            	if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
            		pictureImv.setBackgroundDrawable(null);
            	}
            	pictureImv.setBackgroundDrawable(drawable);
            } else {
                pictureImv.setBackgroundDrawable(null);
            }
            
            nameTxv.setText(yanchu.getName());
            starsRtb.setProgress((int) yanchu.getHot());
            addressTxv.setText(yanchu.getAddress());
            distanceTxv.setText(yanchu.getDistance());
            dateTxv.setText(yanchu.getTimeDesc());
            
            return view;
        }
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.PULL_TO_REFRESH);
        invokeIPagerListCallBack();
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        
        mResultLsv.onRefreshComplete(false);
        if (dataQuery.isStop()) {
            invokeIPagerListCallBack();
            return;
        }

        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else {
            Response response = dataQuery.getResponse();
            if (response != null) {
                
                int responsCode = response.getResponseCode();
                if (responsCode != Response.RESPONSE_CODE_OK) {
                    if (responsCode == 701) {
                    } else {
                        if (dataQuery.isTurnPage()) {
                            invokeIPagerListCallBack();
                            return;
                        }
                        int resid = BaseActivity.getResponseResId(dataQuery);
                        mRetryView.setText(resid, true);
                        mState = STATE_ERROR;
                        updateView();
                    }

                    invokeIPagerListCallBack();
                    return;
                }
            } else {
                if (dataQuery.isTurnPage()) {
                    invokeIPagerListCallBack();
                    return;
                }
                mRetryView.setText(R.string.touch_screen_and_retry, true);
                mState = STATE_ERROR;
                updateView();
                invokeIPagerListCallBack();
                return;
            }
        }

        mResultLsv.setFooterSpringback(false);
        setDataQuery(dataQuery);
        invokeIPagerListCallBack();
    }
    
    private void setDataQuery(DataQuery dataQuery) {
        Response response = dataQuery.getResponse();
        if (response instanceof TuangouResponse) {
            TuangouResponse tuangouResponse = (TuangouResponse)dataQuery.getResponse();
            
            if (tuangouResponse.getList() != null 
                    && tuangouResponse.getList().getList() != null 
                    && tuangouResponse.getList().getList().size() > 0) {
                mState = STATE_LIST;
                mDataQuery = dataQuery;
                mList = tuangouResponse.getList();

                if (dataQuery.isTurnPage() == false) {
                    getList().clear();
                    mSphinx.getHandler().post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mResultLsv.setSelectionFromTop(0, 0);
                        }
                    });
                }
                
                List<Filter> filterList = mDataQuery.getFilterList();
                if (filterList != null && filterList.size() > 0) {
                    refreshFilter(filterList);
                }
                makeFilterArea(dataQuery);
                
                List<Tuangou> list = tuangouResponse.getList().getList();
                for(Tuangou item : list) {
                    item.setFilterArea(mFilterArea);
                }
                mTuangouList.addAll(list);
                
                mDingdanBtn.setVisibility(View.VISIBLE);
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
                
            } else {
                if (dataQuery.isTurnPage()) {
                    return;
                }
                mState = STATE_EMPTY;
            }
        } else if (response instanceof DianyingResponse) {
            DianyingResponse dianyingResponse = (DianyingResponse)dataQuery.getResponse();
            
            if (dianyingResponse.getList() != null 
                    && dianyingResponse.getList().getList() != null 
                    && dianyingResponse.getList().getList().size() > 0) {
                mState = STATE_LIST;
                mDataQuery = dataQuery;
                mList = dianyingResponse.getList();

                if (dataQuery.isTurnPage() == false) {
                    getList().clear();
                    mSphinx.getHandler().post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mResultLsv.setSelectionFromTop(0, 0);
                        }
                    });
                }

                refreshFilter(mDataQuery.getFilterList());
                makeFilterArea(dataQuery);
                
                List<Dianying> list = dianyingResponse.getList().getList();
                updateChangeciOption(list);
                for(Dianying item : list) {
                    item.setFilterArea(mFilterArea);
                }
                mDianyingList.addAll(list);

                mDingdanBtn.setVisibility(View.INVISIBLE);
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
                
            } else {
                if (dataQuery.isTurnPage()) {
                    return;
                }
                mState = STATE_EMPTY;
            }
        } else if (response instanceof YanchuResponse) {
            YanchuResponse yanchuResponse = (YanchuResponse)dataQuery.getResponse();
            
            if (yanchuResponse.getList() != null 
                    && yanchuResponse.getList().getList() != null 
                    && yanchuResponse.getList().getList().size() > 0) {
                mState = STATE_LIST;
                mDataQuery = dataQuery;
                mList = yanchuResponse.getList();

                if (dataQuery.isTurnPage() == false) {
                    getList().clear();
                    mSphinx.getHandler().post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mResultLsv.setSelectionFromTop(0, 0);
                        }
                    });
                }
                
                mYanchuList.addAll(yanchuResponse.getList().getList());

                mDingdanBtn.setVisibility(View.INVISIBLE);
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
                refreshFilter(mDataQuery.getFilterList());
                makeFilterArea(dataQuery);
            } else {
                if (dataQuery.isTurnPage()) {
                    return;
                }
                mState = STATE_EMPTY;
            }
        } else if (response instanceof ZhanlanResponse) {
            ZhanlanResponse zhanlanResponse = (ZhanlanResponse)dataQuery.getResponse();
            
            if (zhanlanResponse.getList() != null 
                    && zhanlanResponse.getList().getList() != null 
                    && zhanlanResponse.getList().getList().size() > 0) {
                mState = STATE_LIST;
                mDataQuery = dataQuery;
                mList = zhanlanResponse.getList();

                if (dataQuery.isTurnPage() == false) {
                    getList().clear();
                    mSphinx.getHandler().post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mResultLsv.setSelectionFromTop(0, 0);
                        }
                    });
                }
                
                mZhanlanList.addAll(zhanlanResponse.getList().getList());
                mDingdanBtn.setVisibility(View.INVISIBLE);
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
                refreshFilter(mDataQuery.getFilterList());
                makeFilterArea(dataQuery);
            } else {
                if (dataQuery.isTurnPage()) {
                    return;
                }
                mState = STATE_EMPTY;
            }
        }

        refreshResultText(dataQuery);
        updateView();
        getAdapter().notifyDataSetChanged();
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
    private void makeFilterArea(DataQuery dataQuery) {
        if (dataQuery.isTurnPage() == false) {
            dataQuery.getCriteria().put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
            if (mFilterList.size() > 0) {
                mFilterArea = FilterListView.getFilterTitle(mSphinx, mFilterList.get(0));
            }
        }
    }
    
    private boolean invokeIPagerListCallBack() {
        if (mIPagerListCallBack != null) {
            mIPagerListCallBack.turnPageEnd(false, getList().size());
            mIPagerListCallBack = null;
            return true;
        }
        return false;
    }
    
    @SuppressWarnings("rawtypes")
    private List getList() {
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            return mTuangouList;
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            return mDianyingList;
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            return mYanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            return mZhanlanList;
        }
        return null;
    }
    
    @SuppressWarnings("rawtypes")
    private ArrayAdapter getAdapter() {
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            return mTuangouAdapter;
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            return mDianyingAdapter;
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            return mYanchuAdapter;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            return mZhanlanAdapter;
        }
        return null;
    }    
    
    public Hashtable<String, String> getCriteria() {
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            return null;
        }
        return lastDataQuery.getCriteria();
    }
    
    private void showFilterListView(View parent) {
        mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter);
        if (mPopupWindow == null) {
            mFilterListView = new FilterListView(mSphinx);
            mPopupWindow = new PopupWindow(mFilterListView);
            mPopupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            mPopupWindow.setFocusable(true);
            // 设置允许在外点击消失
            mPopupWindow.setOutsideTouchable(true);

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter + ActionLog.Dismiss);
                }
            });
            
        }
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }
    
    public class TitlePopupArrayAdapter extends ArrayAdapter<DiscoverCategory> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.discover_title_popup_list_item;
        
        private LayoutInflater mLayoutInflater;
        
        public String mSelectDataType;

        public TitlePopupArrayAdapter(Context context, List<DiscoverCategory> list) {
            super(context, TEXTVIEW_RESOURCE_ID, list);
            mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView iconTxv = (ImageView)view.findViewById(R.id.icon_imv);
            TextView textTxv = (TextView)view.findViewById(R.id.name_txv);
            TextView numTxv = (TextView)view.findViewById(R.id.num_txv);
            
            DiscoverCategory discoverCategory = getItem(position);
            if (discoverCategory.getType().equals(mSelectDataType)) {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                iconTxv.setVisibility(View.VISIBLE);
                textTxv.setTextColor(TKConfig.COLOR_ORANGE);
                numTxv.setTextColor(TKConfig.COLOR_ORANGE);
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                iconTxv.setVisibility(View.INVISIBLE);
                textTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
                numTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
            }
            
            String name = null;
            String dataType = discoverCategory.getType();
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                name = mSphinx.getString(R.string.tuangou);
            } else if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                name = mSphinx.getString(R.string.dianying);
            } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                name = mSphinx.getString(R.string.yanchu);
            } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                name = mSphinx.getString(R.string.zhanlan);
            }
            
            textTxv.setText(name);
            numTxv.setText(String.valueOf(discoverCategory.getNumCity()));

            return view;
        }
    }

    @Override
    public boolean canTurnPage(boolean isHeader) {
        DataQuery dataQuery = mDataQuery;
        BaseList list = mList;
        if (mState != STATE_LIST || mResultLsv.isFooterSpringback() == false || dataQuery == null || list == null || list.getTotal() <= getList().size()) {
            return false;
        }
        return true;
    }

    @Override
    public void turnPageStart(boolean isHeader, IPagerListCallBack iPagerListCallBack) {
        mIPagerListCallBack = iPagerListCallBack;
        turnPage(mSphinx.getString(R.string.doing_and_wait));
    }

    @Override
    public void retry() {
        if (mBaseQuerying != null) {
            for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                mBaseQuerying.get(i).setResponse(null);
            }
            mSphinx.queryStart(mBaseQuerying);
        }
        setup();
    } 
}

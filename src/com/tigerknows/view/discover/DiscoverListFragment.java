/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;

import com.decarta.Globals;
import com.decarta.android.map.InfoWindow.TextAlign;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.BaseList;
import com.tigerknows.model.DataQuery.DianyingResponse;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.model.DataQuery.TuangouResponse;
import com.tigerknows.model.DataQuery.YanchuResponse;
import com.tigerknows.model.DataQuery.ZhanlanResponse;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList.DiscoverCategory;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.FilterExpandableListAdapter;
import com.tigerknows.view.SpringbackListView;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;
import com.tigerknows.view.user.User;
import com.tigerknows.view.user.UserBaseActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class DiscoverListFragment extends DiscoverBaseFragment implements View.OnClickListener {

    public DiscoverListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private ImageButton mDingdanBtn;
    
    private Dialog mFilterDialog = null;

    private LinearLayout mFilterLnl = null;
    
    private TextView mResultTxv;

    private SpringbackListView mResultLsv = null;

    private View mQueryingView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private DataQuery mDataQuery;
    
    private DataQuery mDataQuerying;
    
    private List<Tuangou> mTuangouList = new ArrayList<Tuangou>();
    
    private TuangouAdapter mTuangouAdapter;
    
    private List<Dianying> mDianyingList = new ArrayList<Dianying>();
    
    private DianyingAdapter mDianyingAdapter;
    
    private List<Yanchu> mYanchuList = new ArrayList<Yanchu>();
    
    private YanchuAdapter mYanchuAdapter;
    
    private List<Zhanlan> mZhanlanList = new ArrayList<Zhanlan>();
    
    private ZhanlanAdapter mZhanlanAdapter;
    
    private List<Filter> mFilterList = new ArrayList<Filter>();
    
    private String mFilterArea;
    
    private BaseList mList;
    
    private String mDataType;
    
    private List<DiscoverCategory> mDiscoverCategoryList = new ArrayList<DiscoverCategory>();
    
    private TitlePopupArrayAdapter mTitlePopupArrayAdapter;
    
    private OnItemClickListener mTitlePopupOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
            mTitleFragment.dismissPopupWindow();
            TitlePopupArrayAdapter titlePopupArrayAdapter = (TitlePopupArrayAdapter) adapterView.getAdapter();
            DiscoverCategory discoverCategory = titlePopupArrayAdapter.getItem(position);
            DataQuery dataQuery = new DataQuery(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, discoverCategory.getType());
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(),
                    R.id.view_discover, R.id.view_discover_list, null, false, false,
                    mSphinx.getPOI());
            mSphinx.queryStart(dataQuery);
            setup();
            onResume();
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
    
    public void setup() {
        this.mDataQuerying = (DataQuery) this.mTkAsyncTasking.getBaseQuery();
        this.mList = null;
        this.mResultLsv.setFooterSpringback(false);

        DataQuery lastDataQuerying = mDataQuerying;
        if (lastDataQuerying != null) {
            String str = mContext.getString(R.string.loading);
    
            if (lastDataQuerying.getSourceViewId() != getId()) {
                mDataQuery = null;
                mFilterLnl.setVisibility(View.GONE);
            }
            
            if (lastDataQuerying.isTurnPage() == false) {
                mResultTxv.setText(str);
                mQueryingTxv.setText(str);
                updateView();
            }
            mDataType = lastDataQuerying.getCriteria().get(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
                if (mTuangouAdapter == null) {
                    mTuangouAdapter = new TuangouAdapter(mSphinx, mTuangouList);
                }
                mResultLsv.setAdapter(mTuangouAdapter);
                mDingdanBtn.setVisibility(View.VISIBLE);
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
        }
        
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
        
        mTitlePopupArrayAdapter = new TitlePopupArrayAdapter(mSphinx, mDiscoverCategoryList);
        
        return mRootView;
    }
    
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
        mDataQuerying = null;
    }
    
    protected void findViews() {
        mDingdanBtn = (ImageButton) mRootView.findViewById(R.id.dingdan_btn);
        mResultTxv = (TextView) mRootView.findViewById(R.id.result_txv);
        mResultTxv.setVisibility(View.GONE);
        mFilterLnl = (LinearLayout)mRootView.findViewById(R.id.filter_lnl);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mQueryingView = mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    Object object = adapterView.getAdapter().getItem(position);
                    if (object != null) {
                        if (object instanceof Tuangou) {
                            mSphinx.showView(R.id.view_tuangou_detail);
                            Tuangou tagret = (Tuangou) object;
                            tagret.setOrderNumber(position+1);
//                            tagret.getFendian().setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag+ActionLog.DiscoverListSelectItem, position+1, tagret.getUid());
                            mSphinx.getTuangouDetailFragment().setData(tagret);
                        } else if (object instanceof Yanchu){
                            mSphinx.showView(R.id.view_yanchu_detail);
                            Yanchu tagret = (Yanchu) object;
                            tagret.setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag+ActionLog.DiscoverListSelectItem, position+1, tagret.getUid());
                        	mSphinx.getYanchuDetailFragment().setData(tagret);
                        } else if (object instanceof Dianying){
                            mSphinx.showView(R.id.view_dianying_detail);
                            Dianying tagret = (Dianying) object;
                            tagret.setOrderNumber(position+1);
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
                            tagret.getYingxun().setChangciOption(changciOption);
//                            tagret.getYingxun().setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag+ActionLog.DiscoverListSelectItem, position+1, tagret.getUid());
                        	mSphinx.getDianyingDetailFragment().setData(tagret);
                        } else if (object instanceof Zhanlan){
                            mSphinx.showView(R.id.view_zhanlan_detail);
                        	Zhanlan tagret = (Zhanlan) object;
                            tagret.setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag+ActionLog.DiscoverListSelectItem, position+1, tagret.getUid());
                        	mSphinx.getZhanlanDetailFragment().setData(tagret);
                        }
                    }
                }
            }
            
        });

        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                mActionLog.addAction(mActionTag+ActionLog.DiscoverListLoading);
                turnPage();
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
            if (mDataQuerying == null && lastDataQuery != null && mEmptyView.getVisibility() == View.VISIBLE) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                setDataQuery(lastDataQuery);
                return true;
            }
        }
        return false;
    }
    
    private void refreshResultTitleText(DataQuery dataQuery) {
        String str = null;
        if (dataQuery != null && mList != null) {
            str = mList.getMessage();
        }

        if (TextUtils.isEmpty(str)) {
            str = mContext.getString(R.string.no_result);
        }
        
        mResultTxv.setText(str);
        mEmptyTxv.setText(str);
    }
    
    private void refreshFilter(DataQuery dataQuery) {
        mFilterList.clear();
        if (dataQuery != null) {
            List<Filter> filterList = dataQuery.getFilterList();
            for(Filter filter : filterList) {
                mFilterList.add(filter.clone());
            }
        }
        FilterExpandableListAdapter.refreshFilterButton(mFilterLnl, mFilterList, mSphinx, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            mTitleBtn.setText(R.string.tuangou_list);
            mRightImv.setVisibility(View.VISIBLE);
            mRightBtn.setVisibility(View.VISIBLE);
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            mTitleBtn.setText(R.string.dianying_list);
            mRightImv.setVisibility(View.GONE);
            mRightBtn.setVisibility(View.GONE);
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            mTitleBtn.setText(R.string.yanchu_list);
            mRightImv.setVisibility(View.VISIBLE);
            mRightBtn.setVisibility(View.VISIBLE);
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            mTitleBtn.setText(R.string.zhanlan_list);
            mRightImv.setVisibility(View.VISIBLE);
            mRightBtn.setVisibility(View.VISIBLE);
        }
        mRightImv.setImageResource(R.drawable.ic_view_map);
        mRightBtn.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightBtn.setOnClickListener(this);
        
        List<DiscoverCategory> list = mSphinx.getDiscoverFragment().getDiscoverCategoryList();
        mDiscoverCategoryList.clear();
        for(int i = list.size()-1; i >= 0; i--) {
            DiscoverCategory discoverCategory = list.get(i);
            if (discoverCategory.getNumCity() > 0 || discoverCategory.getNumNearby() > 0) {
                if (mDiscoverCategoryList.contains(discoverCategory) == false) {
                    mDiscoverCategoryList.add(discoverCategory);
                }
            }
        }
        mTitleBtn.setBackgroundResource(R.drawable.btn_default);
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
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    private void updateView() {
        if (mDataQuerying != null) {
            mQueryingView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
        } else if (mList == null) {
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
        } else {
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
        }
    }
    
    private void turnPage(){
        synchronized (this) {
        DataQuery lastDataQuery = mDataQuery;
        if (mList == null || mDataQuerying != null || mResultLsv.isFooterSpringback() == false || lastDataQuery == null) {
            return;
        }
        if (mList.getTotal() <= getList().size()) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);

        DataQuery dataQuery = new DataQuery(mContext);
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(getList().size()));
        int dianyingSize = mDianyingList.size();
        if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType) && dianyingSize > 0) {
            criteria.put(DataQuery.SERVER_PARAMETER_DIANYING_UUID, mDianyingList.get(dianyingSize-1).getUid());
        }
        dataQuery.setup(criteria, lastDataQuery.getCityId(), getId(), getId(), null, true, true, lastDataQuery.getPOI());
        mSphinx.queryStart(dataQuery);
        }
    }

    public void viewMap(int firstVisiblePosition, int lastVisiblePosition) {
        if (mList == null) {
            return;            
        }
        int size = getList().size();
        int[] page = CommonUtils.makePage(mResultLsv, size, firstVisiblePosition, lastVisiblePosition);
        
        int minIndex = page[0];
        int maxIndex = page[1];
        List<POI> poiList = new ArrayList<POI>();
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex < maxIndex && minIndex < mTuangouList.size(); minIndex++) {
                POI poi = mTuangouList.get(minIndex).getPOI();
                poi.setName(mTuangouList.get(minIndex).getShortDesc());
//                poi.setOrderNumber(minIndex+1);
                poiList.add(poi);
            }
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex < maxIndex && minIndex < mYanchuList.size(); minIndex++) {
                Yanchu yanchu = mYanchuList.get(minIndex);
                yanchu.setOrderNumber(minIndex+1);
                POI poi = yanchu.getPOI();
                poi.setName(yanchu.getName());
                poi.setAddress(null);
                poiList.add(poi);
            }
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex < maxIndex && minIndex < mZhanlanList.size(); minIndex++) {
                Zhanlan zhanlan = mZhanlanList.get(minIndex);
                zhanlan.setOrderNumber(minIndex+1);
                POI poi = zhanlan.getPOI();
                poi.setName(zhanlan.getName());
                poi.setAddress(null);
                poiList.add(poi);
            }
        }
        if (poiList.isEmpty()) {
            return;
        }
        int name = R.string.tuangou_ditu;
        String actionTag = ActionLog.MapTuangouList;
        if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            name = R.string.yanchu_ditu;
            actionTag = ActionLog.MapYanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            name = R.string.zhanlan_ditu;
            actionTag = ActionLog.MapZhanlanList;
        }
        mSphinx.showPOI(poiList, page[2], TextAlign.LEFT);
        mSphinx.getResultMapFragment().setData(mContext.getString(name), actionTag);
        mSphinx.showView(R.id.view_result_map);   
    }
    
    private void filter() {

        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
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
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.title_btn:
                if (mDataQuery == null || getList().isEmpty() || mResultLsv.getVisibility() != View.VISIBLE || mDataQuerying != null || mList == null) {
                    return;
                }
                if (mDiscoverCategoryList.size() > 0) {
                    mTitlePopupArrayAdapter.mSelectDataType = mDataType;
                    mTitleFragment.showPopupWindow(mTitlePopupArrayAdapter, mTitlePopupOnItemClickListener);
                    mTitlePopupArrayAdapter.notifyDataSetChanged();
                }
                break;
                
            case R.id.right_btn:
                if (mDataQuery == null || getList().isEmpty() || mResultLsv.getVisibility() != View.VISIBLE || mDataQuerying != null || mList == null) {
                    return;
                }
                mActionLog.addAction(ActionLog.Title_Right_Button, mActionTag);
                viewMap(mResultLsv.getFirstVisiblePosition(), mResultLsv.getLastVisiblePosition());
                break;
                
            case R.id.dingdan_btn:
                mActionLog.addAction(ActionLog.TuangouListDingdan);
                User user = Globals.g_User;
                if (user != null) {
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID, getId());
                    mSphinx.showView(R.id.activity_tuangou_shangjia_list, intent);
                } else {
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.activity_tuangou_shangjia_list);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
                    mSphinx.showView(R.id.activity_user_login, intent);
                }
                break;
                
            default:
                if (mDataQuerying != null) {
                    return;
                }
                
                if (mFilterDialog != null && mFilterDialog.isShowing()) {
                    return;
                }
                
                if (mTkAsyncTasking != null) {
                    mTkAsyncTasking.stop();
                }
                mResultLsv.onRefreshComplete(false);

                final Filter filter = (Filter)view.getTag();
                final int key = filter.getKey();
                if (key == FilterResponse.FIELD_FILTER_AREA_INDEX) {
                    mActionLog.addAction(mActionTag+ActionLog.DiscoverListFilterArea);
                } else if (key == FilterResponse.FIELD_FILTER_CATEGORY_INDEX) {
                    mActionLog.addAction(mActionTag+ActionLog.DiscoverListFilterCategory);
                } else if (key == FilterResponse.FIELD_FILTER_ORDER_INDEX) {
                    mActionLog.addAction(mActionTag+ActionLog.DiscoverListFilterOrder);
                }
                
                final ExpandableListView filterElv = new ExpandableListView(mContext);
                filterElv.setScrollingCacheEnabled(false);
                filterElv.setFadingEdgeLength(0);
                filterElv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                
                final FilterExpandableListAdapter filterExpandableListAdapter = new FilterExpandableListAdapter(mSphinx, filter);
                filterElv.setAdapter(filterExpandableListAdapter);
                filterElv.setGroupIndicator(null);
                Drawable drawable = mContext.getResources().getDrawable(R.drawable.divider);
                filterElv.setDivider(drawable);
                filterElv.setChildDivider(drawable);
                filterElv.setPadding(0, 0, 0, 0);
                filterElv.setFadingEdgeLength(0);
                filterElv.setOnGroupClickListener(new OnGroupClickListener() {
                    
                    @Override
                    public boolean onGroupClick(ExpandableListView arg0, View arg1, final int position, long arg3) {
                        List<Filter> list = filter.getChidrenFilterList();
                        if (list.get(position).getChidrenFilterList().size() == 0) {
                            mFilterDialog.dismiss();
                            if (!list.get(position).isSelected() || mList == null) {
                                
                                for(Filter filter : list) {
                                    filter.setSelected(false);
                                    List<Filter> chidrenFilterList1 = filter.getChidrenFilterList();
                                    for(Filter childrenFilter1 : chidrenFilterList1) {
                                        childrenFilter1.setSelected(false);
                                    }
                                }
                                list.get(position).setSelected(true);
                                
                                ((Button)view).setText(FilterExpandableListAdapter.getFilterTitle(mSphinx, filter));
                                mActionLog.addAction(mActionTag+ActionLog.DiscoverListFilterSelect, key, DataQuery.makeFilterRequest(mFilterList));
                                filter();
                            }
                        }
                        return false;
                    }
                });
                filterElv.setOnChildClickListener(new OnChildClickListener() {
                    
                    @Override
                    public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4) {
                        mFilterDialog.dismiss();
                        List<Filter> list = filter.getChidrenFilterList();
                        if (!list.get(arg2).getChidrenFilterList().get(arg3).isSelected() || mList == null) {
                            for(Filter filter : list) {
                                filter.setSelected(false);
                                List<Filter> childrenList1 = filter.getChidrenFilterList();
                                for(Filter childrenFilter1 : childrenList1) {
                                    childrenFilter1.setSelected(false);
                                }
                            }
                            list.get(arg2).getChidrenFilterList().get(arg3).setSelected(true);
                            
                            ((Button)view).setText(FilterExpandableListAdapter.getFilterTitle(mSphinx, filter));
                            mActionLog.addAction(mActionTag+ActionLog.DiscoverListFilterSelect, key, DataQuery.makeFilterRequest(mFilterList));
                            filter();
                        }
                        return false;
                    }
                });
                
                mFilterDialog = new AlertDialog.Builder(mSphinx).setTitle(FilterExpandableListAdapter.getFilterTitle(mSphinx, filter))
                .setView(filterElv)
                .setCancelable(true)
                .setOnCancelListener(new OnCancelListener() {
                    
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        if (mResultLsv.isFooterSpringback()) {
                            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
                        }
                    }
                })
                .create();
                
                mFilterDialog.setCanceledOnTouchOutside(false);
                mFilterDialog.show();

                int position = FilterExpandableListAdapter.getFilterSelectParentPosition(filter);
                if (position < filter.getChidrenFilterList().size()) {
                    filterElv.expandGroup(position);
                    filterElv.setSelectionFromTop(position, 0);
                }
        }
    }
    
    public class TuangouAdapter extends ArrayAdapter<Tuangou>{
        private static final int RESOURCE_ID = R.layout.tuangou_list_item;
        
        public TuangouAdapter(Context context, List<Tuangou> list) {
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
            ImageView shangjiaMarkerImv = (ImageView) view.findViewById(R.id.shangjia_marker_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView orgPriceTxv = (TextView) view.findViewById(R.id.org_price_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView buyerNumTxv = (TextView) view.findViewById(R.id.buyer_num_txv);

            Tuangou tuangou = getItem(position);
            Drawable drawable = tuangou.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
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
            priceTxv.setText(mSphinx.getString(R.string.rmb) + tuangou.getPrice());
            orgPriceTxv.setText(mSphinx.getString(R.string.rmb)+tuangou.getOrgPrice());
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
        private static final int RESOURCE_ID = R.layout.dianying_list_item;
        
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
            Drawable drawable = dianying.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
                pictureImv.setImageDrawable(drawable);
            } else {
                pictureImv.setImageDrawable(null);
            }
            nameTxv.setText(dianying.getName());
            starsRtb.setProgress((int) dianying.getRank());
            String distance = dianying.getYingxun().getDistance();
            if (TextUtils.isEmpty(distance)) {
                distanceTxv.setVisibility(View.GONE);
            } else {
                distanceTxv.setText(mSphinx.getString(R.string.dianying_detail_nearest, String.valueOf(dianying.getYingxun().getDistance())));
                distanceTxv.setVisibility(View.VISIBLE);
            }
            addressTxv.setText(dianying.getTag());
            if (TextUtils.isEmpty(dianying.getLength())) {
            	dateTxv.setVisibility(View.GONE);
            } else {
            	dateTxv.setVisibility(View.VISIBLE);
            	dateTxv.setText(String.valueOf(dianying.getLength()));
            }
            return view;
        }
    }
    
    public class YanchuAdapter extends ArrayAdapter<Yanchu>{
        private static final int RESOURCE_ID = R.layout.yanchu_list_item;
        
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
                pictureImv.setImageDrawable(drawable);
            } else {
                pictureImv.setImageDrawable(null);
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
        private static final int RESOURCE_ID = R.layout.yanchu_list_item;
        
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
                pictureImv.setImageDrawable(drawable);
            } else {
                pictureImv.setImageDrawable(null);
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
        mResultLsv.onRefreshComplete(false);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();

        boolean turnpage = false;
        boolean exit = true;
        if (dataQuery.getCriteria().containsKey(BaseQuery.SERVER_PARAMETER_INDEX)) {
            int index = Integer.parseInt(dataQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_INDEX));
            if (index > 0) {
                mResultLsv.onRefreshComplete(false);
                mResultLsv.setFooterSpringback(true);
                exit = false;
                turnpage = true;
            }
        }

        boolean filter = false;
        if (dataQuery.getCriteria().containsKey(DataQuery.SERVER_PARAMETER_FILTER) && exit) {
            filter = true;
            exit = false;
        }
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mSphinx, new int[]{701}, !filter, this, exit)) {
            Response response = dataQuery.getResponse();
            if (turnpage) {
                if (response != null) {
                    if (response.getResponseCode() == 701) {
                        mResultLsv.setFooterSpringback(false);
                    }
                }
            } else if (filter) {
                mDataQuerying = null;
//                updateView();
                final AlertDialog alertDialog = CommonUtils.getAlertDialog(mSphinx);
                alertDialog.setCancelable(false);
                alertDialog.setMessage(mSphinx.getString(BaseActivity.getResponseResId(dataQuery)));
                alertDialog.setButton(mSphinx.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        alertDialog.dismiss();
                        setDataQuery(mDataQuery);
                    }
                });
                alertDialog.show();

            }
            return;
        }
        if (mDataQuerying != null && dataQuery != mDataQuerying) {
            return;
        }
        mResultLsv.onRefreshComplete(false);
        mResultLsv.setFooterSpringback(false);
        
        mDataQuerying = null;
        setDataQuery(dataQuery);
    }
    
    private void setDataQuery(DataQuery dataQuery) {
        Response response = dataQuery.getResponse();
        refreshFilter(dataQuery);
        
        if (dataQuery.isTurnPage() == false) {
            dataQuery.getCriteria().put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
            mFilterArea = FilterExpandableListAdapter.getFilterTitle(mSphinx, mFilterList.get(0));
        }
        if (response instanceof TuangouResponse) {
            TuangouResponse tuangouResponse = (TuangouResponse)dataQuery.getResponse();
            
            if (tuangouResponse.getList() != null 
                    && tuangouResponse.getList().getList() != null 
                    && tuangouResponse.getList().getList().size() > 0) {
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
                
                List<Tuangou> list = tuangouResponse.getList().getList();
                for(Tuangou item : list) {
                    item.setFilterArea(mFilterArea);
                }
                mTuangouList.addAll(list);
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
            }
        } else if (response instanceof DianyingResponse) {
            DianyingResponse dianyingResponse = (DianyingResponse)dataQuery.getResponse();
            
            if (dianyingResponse.getList() != null 
                    && dianyingResponse.getList().getList() != null 
                    && dianyingResponse.getList().getList().size() > 0) {
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
                
                List<Dianying> list = dianyingResponse.getList().getList();
                for(Dianying item : list) {
                    item.setFilterArea(mFilterArea);
                }
                mDianyingList.addAll(list);
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
            }
        } else if (response instanceof YanchuResponse) {
            YanchuResponse yanchuResponse = (YanchuResponse)dataQuery.getResponse();
            
            if (yanchuResponse.getList() != null 
                    && yanchuResponse.getList().getList() != null 
                    && yanchuResponse.getList().getList().size() > 0) {
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
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
            }
        } else if (response instanceof ZhanlanResponse) {
            ZhanlanResponse zhanlanResponse = (ZhanlanResponse)dataQuery.getResponse();
            
            if (zhanlanResponse.getList() != null 
                    && zhanlanResponse.getList().getList() != null 
                    && zhanlanResponse.getList().getList().size() > 0) {
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
                
                if (getList().size() < mList.getTotal()) {
                    mResultLsv.setFooterSpringback(true);
                }
            }
        }
        
        updateView();
        getAdapter().notifyDataSetChanged();
        refreshResultTitleText(dataQuery);
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
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
            
            DiscoverCategory discoverCategory = getItem(position);
            if (discoverCategory.getType().equals(mSelectDataType)) {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
            }
            
            TextView textTxv = (TextView)view.findViewById(R.id.name_txv);
            TextView numTxv = (TextView)view.findViewById(R.id.num_txv);
            
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
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.hotel.NavigationWidget;
import com.tigerknows.ui.more.AddMerchantActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class POIResultFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack, RetryView.CallBack {

    public POIResultFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private FilterListView mFilterListView = null;
    
    private String mTitleText;

    private ViewGroup mFilterControlView = null;
    
    private SpringbackListView mResultLsv = null;

    private QueryingView mQueryingView = null;
    
    private View mLoadingView = null;
    
    private View mAddMerchantFootView = null;
    
    private View mCurrentFootView;
    
    private View mAddMerchantView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private RetryView mRetryView;
    
    private NavigationWidget mNavigationWidget;
    
    private TextView mLocationTxv;
    
    private POIAdapter mResultAdapter = null;
    
    private DataQuery mDataQuery;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
    private String mInputText;
    
    private long mBTotal;
    
    private POI mAPOI = null;
    
    private List<Filter> mFilterList = new ArrayList<Filter>();
    
    static final int STATE_QUERYING = 0;
    static final int STATE_ERROR = 1;
    static final int STATE_EMPTY = 2;
    static final int STATE_LIST = 3;
    
    private int mTitleBtnPaddingLeft;
    
    private int mTitleBtnPaddingTop;
    
    private int mState = STATE_QUERYING;
    
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
            mResultAdapter.notifyDataSetChanged();
        }
    };
    
    public List<POI> getPOIList() {
        return mPOIList;
    }
    
    /**
     * 显示查询状态
     * @param dataQuery
     */
    private void showQuering(DataQuery dataQuery) {
        if (dataQuery == null) {
            return;
        }

        mQueryingTxv.setText(R.string.searching);
        mTitleText = getString(R.string.searching_title);
        
        this.mState = STATE_QUERYING;
        updateView();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mActionTag = ActionLog.POIList;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        
        mTitleBtnPaddingLeft = Utility.dip2px(mSphinx, 16);
        mTitleBtnPaddingTop = Utility.dip2px(mSphinx, 14.5f);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_result, container, false);

        findViews();
        setListener();
        
        mResultAdapter = new POIAdapter(mSphinx, mPOIList, mLoadedDrawableRun, POIResultFragment.this.toString());
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        if (mPOIList != null) {
            mPOIList.clear();
            mResultAdapter.notifyDataSetChanged();
        }
        mDataQuery = null;
    }
    
    @Override
    protected void findViews() {
        super.findViews();
        mFilterControlView = (ViewGroup)mRootView.findViewById(R.id.filter_control_view);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View nearbySearchBarView = mLayoutInflater.inflate(R.layout.poi_nearby_search_bar, null);
        mResultLsv.addHeaderView(nearbySearchBarView, false);
        mLocationTxv = (TextView) nearbySearchBarView.findViewById(R.id.location_txv);
        mLoadingView = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(mLoadingView);
        mCurrentFootView = mLoadingView;
        mAddMerchantFootView = mLayoutInflater.inflate(R.layout.poi_list_item_add_merchant, null);
        mAddMerchantView = mRootView.findViewById(R.id.add_merchant_view);
        mAddMerchantView.findViewById(R.id.add_merchant_item_view).setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                showAddMerchant();
            }
        });
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
        mNavigationWidget = (NavigationWidget) mRootView.findViewById(R.id.navigation_widget);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mRetryView.setCallBack(this, mActionTag);
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int location, long id) {
                int position = (int)id;
                if (position >= 0 && position < mPOIList.size()) {
                    POI poi = mPOIList.get(position);
                    if (poi != null) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, poi.getUUID(), poi.getName());
                        mSphinx.showView(R.id.view_poi_detail);
                        mSphinx.getPOIDetailFragment().setData(poi, position);
                    }
                } else if (location > 0 && mResultLsv.isFooterSpringback() == false) {
                    showAddMerchant();
                }
            }
            
        });

        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader) {
                    return;
                }
                turnPage();
            }
        });
    }
    
    /**
     * 跳转到添加商户界面
     */
    private void showAddMerchant() {
        mActionLog.addAction(mActionTag + ActionLog.POIListAddMerchanty);
        Intent intent = new Intent();
        if (TextUtils.isEmpty(mInputText) == false) {
            intent.putExtra(AddMerchantActivity.EXTRA_INPUT_TEXT, mInputText);
        }
        mSphinx.showView(R.id.activity_more_add_merchant, intent);
    }
    
    /**
     * 刷新标题栏文字
     * @param dataQuery
     */
    private void refreshResultTitleText(DataQuery dataQuery) {
        String str = getString(R.string.no_result);
        if (dataQuery != null) {
            POIResponse poiModel = (POIResponse)dataQuery.getResponse();
            if (poiModel != null) {
                POIList poiList = poiModel.getAPOIList();
                if (poiList != null) {
                    mTitleText = Utility.substring(poiList.getShortMessage(), 5)
                    + getString(R.string.double_bracket, poiList.getTotal());
                }
        
                poiList = poiModel.getBPOIList();
                if (poiList != null) {
                    mTitleText = Utility.substring(poiList.getShortMessage(), 5)
                    + getString(R.string.double_bracket, poiList.getTotal());
                }
            }
        }
        
        if (isShowing()) {
            if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(mResultAdapter.getSubDataType())) {
                mTitleBtn.setText(mTitleText);
                mTitleBtn.setHint(null);
            } else {
                mTitleBtn.setText(null);
                mTitleBtn.setHint(mTitleText);
            }
        }

        if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(mResultAdapter.getSubDataType())) {
            mEmptyTxv.setText(R.string.can_not_found_result_and_retry);

            if (isShowing()) {
                mTitleBtn.setOnClickListener(null);
                mTitleBtn.setBackgroundDrawable(null);
                mTitleBtn.setPadding(0, 0, 0, 0);
                mTitleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            }
            
        } else {
            mEmptyTxv.setText(str);
            
            if (isShowing()) {
                mTitleBtn.setOnClickListener(this);
                mTitleBtn.setBackgroundResource(R.drawable.textfield);
                mTitleBtn.setPadding(mTitleBtnPaddingLeft, mTitleBtnPaddingTop, mTitleBtnPaddingLeft, mTitleBtnPaddingTop);
                mTitleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }
        }
    }
    
    /**
     * 刷新筛选项
     * @param filterList
     */
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

    @Override
    public void onResume() {
        super.onResume();

        // 5.8.0 2.2.2 2)从搜索结果列表页返回时，不应再返回搜索输入页，应跨过搜索输入页直接跳回搜索输入页的上一页
        mSphinx.uiStackRemove(R.id.view_poi_input_search);

        mRightBtn.setText(R.string.map);
        mRightBtn.setOnClickListener(this);
        
        if (isReLogin()) {
            return;
        }
        
        refreshStamp();

        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        
        updateView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    /**
     * 根据状态显示的内容
     */
    private void updateView() {
        if (mState == STATE_QUERYING) {
            mQueryingView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            mNavigationWidget.setVisibility(View.GONE);
            if (isShowing()) {
                mRightBtn.setVisibility(View.INVISIBLE);
            }
            mAddMerchantView.setVisibility(View.GONE);
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            if (isShowing()) {
                mRightBtn.setVisibility(View.INVISIBLE);
            }
            mAddMerchantView.setVisibility(View.GONE);
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
            mNavigationWidget.setVisibility(View.GONE);
            if (isShowing()) {
                mRightBtn.setVisibility(View.INVISIBLE);
            }
            
            if (BaseQuery.SUB_DATA_TYPE_POI.equals(mResultAdapter.getSubDataType())) {
                mAddMerchantView.setVisibility(View.VISIBLE);
            } else {
                mAddMerchantView.setVisibility(View.GONE);
            }
        } else {
            DataQuery dataQuery = mDataQuery;
            if (dataQuery != null &&
                    BaseQuery.SUB_DATA_TYPE_HOTEL.equals(dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE)) &&
                    mPOIList.size() > 0) {
                int bottom = Util.dip2px(Globals.g_metrics.density, 32);
                mResultLsv.setPadding(0, 0, 0, bottom);
                mNavigationWidget.setVisibility(View.VISIBLE);
            } else {
                mResultLsv.setPadding(0, 0, 0, 0);
                mNavigationWidget.setVisibility(View.GONE);
            }
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
            if (isShowing()) {
                mRightBtn.setVisibility(mPOIList.size() > 0 ? View.VISIBLE : View.INVISIBLE);
            }
            
            boolean footerSpringback = mResultLsv.isFooterSpringback();
            View v = mCurrentFootView;
            boolean addMerchant = false;
            if (BaseQuery.SUB_DATA_TYPE_POI.equals(mResultAdapter.getSubDataType())) {
                if (footerSpringback == false) {
                    if (v != mAddMerchantFootView) {
                        mResultLsv.removeFooterView(mAddMerchantFootView);
                        mResultLsv.removeFooterView(mLoadingView);
                        mResultLsv.addFooterView(mAddMerchantFootView, false);
                        mCurrentFootView = mAddMerchantFootView;
                    }
                    addMerchant = true;
                }
            }
            
            if (addMerchant == false) {
                if (v != mLoadingView) {
                    int state = mResultLsv.getState(false);
                    mResultLsv.removeFooterView(mAddMerchantFootView);
                    mResultLsv.removeFooterView(mLoadingView);
                    mResultLsv.addFooterView(mLoadingView);
                    mResultLsv.setFooterSpringback(footerSpringback);
                    mResultLsv.changeHeaderViewByState(false, state);
                    mCurrentFootView = mLoadingView;
                }
            }
            mAddMerchantView.setVisibility(View.GONE);
            
            if (mAPOI != null) {
                this.mResultLsv.changeHeaderViewByState(true, SpringbackListView.PULL_TO_REFRESH);
                this.mLocationTxv.setText(this.mAPOI.getName());
            } else {
                this.mResultLsv.changeHeaderViewByState(true, SpringbackListView.DONE);
                this.mLocationTxv.setText(null);
            }
        }
        refreshResultTitleText(null);
    }
    
    /**
     * 翻页
     */
    private void turnPage(){
        synchronized (this) {
        DataQuery lastDataQuery = mDataQuery;
        if (mState != STATE_LIST || canTurnPage() == false || lastDataQuery == null || mResultLsv.isFooterSpringback() == false) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);

        DataQuery poiQuery = new DataQuery(lastDataQuery);
        POI requestPOI = lastDataQuery.getPOI();
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(mPOIList.size()));
        if (poiQuery.hasParameter(DataQuery.SERVER_PARAMETER_FILTER)) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        }
        poiQuery.setup(getId(), getId(), null, true, false, requestPOI);
        mTkAsyncTasking = mSphinx.queryStart(poiQuery);
        }
    }

    /**
     * 查看地图
     * @param firstVisiblePosition
     */
    private void viewMap(int firstVisiblePosition) {
        if (firstVisiblePosition < 0 || mPOIList.size() < firstVisiblePosition || mPOIList.isEmpty()) {
            return;            
        }

        // 此处判断减1，是因为第0个是A类POI
        if (firstVisiblePosition > 0) {
            firstVisiblePosition--;
        }
        
        int size = mPOIList.size();
        List<POI> poiList = new ArrayList<POI>();
        int[] page = Utility.makePagedIndex(mResultLsv, size, firstVisiblePosition);
        
        int minIndex = page[0];
        int maxIndex = page[1];
        for(;minIndex <= maxIndex && minIndex < size; minIndex++) {
            POI poi = mPOIList.get(minIndex);
            poiList.add(poi);
        }
        int firstIndex = page[2];
        mSphinx.getResultMapFragment().setData(getString(R.string.result_map), BaseQuery.SUB_DATA_TYPE_HOTEL.equals(mResultAdapter.getSubDataType()) ? ActionLog.POIHotelListMap : ActionLog.POIListMap);
        mSphinx.showView(R.id.view_result_map);   
        ItemizedOverlayHelper.drawPOIOverlay(mSphinx, poiList, firstIndex, mAPOI);
    }
    
    /**
     * 筛选操作
     */
    @Override
    public void doFilter(String name) {
        FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
        
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            dismissPopupWindow();
            return;
        }
        
        TKAsyncTask tkAsyncTask = mTkAsyncTasking;
        if (tkAsyncTask != null) {
            BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
            if (baseQuery != null) {
                baseQuery.stop();
            }
            tkAsyncTask.stop();
        }

        DataQuery poiQuery = new DataQuery(lastDataQuery);

        POI requestPOI = lastDataQuery.getPOI();
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        poiQuery.setup(getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(poiQuery);
        showQuering(poiQuery);
        dismissPopupWindow();
    }
    
    /**
     * 取消筛选
     */
    @Override
    public void cancelFilter() {
        dismissPopupWindow();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.title_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleCenterButton);
                DataQuery dataQuery = new DataQuery(mDataQuery);
                dataQuery.removeParameter(DataQuery.SERVER_PARAMETER_INFO);
                dataQuery.removeParameter(DataQuery.SERVER_PARAMETER_FILTER);
                dataQuery.removeParameter(DataQuery.SERVER_PARAMETER_FILTER_STRING);
                mSphinx.getInputSearchFragment().setData(dataQuery,
                		mInputText,
                		InputSearchFragment.MODE_POI);
                mSphinx.showView(R.id.view_poi_input_search);
                break;
                
            case R.id.right_btn:
                if (mPOIList.isEmpty() || mState != STATE_LIST) {
                    return;
                }
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                viewMap(mResultLsv.getFirstVisiblePosition());
                break;
                
            default:
                
                boolean turnPageing = (mResultLsv.getState(false) == SpringbackListView.REFRESHING);
                if (mState == STATE_QUERYING && turnPageing == false) {
                    return;
                }
                
                showFilterListView(mTitleFragment);

                byte key = (Byte)view.getTag();
                mFilterListView.setData(mFilterList, key, POIResultFragment.this, mActionTag);
        }
    }
    
    /**
     * POI的Apdater（普通POI和酒店POI）
     * @author pengwenwue
     *
     */
    public static class POIAdapter extends ArrayAdapter<POI>{
        private static final int RESOURCE_ID = R.layout.poi_list_item;
        
        private Drawable icDish;
        private String commentTitle;
        private String addressTitle;
        private Activity activity;
        private LayoutInflater layoutInflater;
        private boolean showStamp = true;
        private Runnable loadedDrawableRun;
        private String viewToken;
        private int hotelPicWidth = 0;
        private int padding = 0;
        private String subDataType = BaseQuery.SUB_DATA_TYPE_POI;
        
        private List<String> DPOITypeList = new ArrayList<String>();
        private int dynamicPOIIconWidth;
        private int nameMaxWidth;
        
        public String getSubDataType() {
            return this.subDataType;
        }
        
        public void setSubDataType(String subDataType) {
            this.subDataType = subDataType;
        }
        
        public void setShowStamp(boolean showStamp) {
            this.showStamp = showStamp;
        }

        public POIAdapter(Activity activity, List<POI> list, Runnable loadedDrawableRun, String viewToken) {
            super(activity, RESOURCE_ID, list);
            this.activity = activity;
            this.loadedDrawableRun = loadedDrawableRun;
            this.viewToken = viewToken;
            layoutInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Resources resources = activity.getResources();
            icDish = resources.getDrawable(R.drawable.ic_dynamicpoi_dish);
            commentTitle = resources.getString(R.string.comment) + " : ";
            addressTitle = resources.getString(R.string.address) + " : ";
            hotelPicWidth = Util.dip2px(Globals.g_metrics.density, 68);
            padding = Util.dip2px(Globals.g_metrics.density, 4);
            dynamicPOIIconWidth = resources.getDrawable(R.drawable.ic_dynamicpoi_tuangou).getIntrinsicWidth();
            nameMaxWidth = Globals.g_metrics.widthPixels - (6*Util.dip2px(Globals.g_metrics.density, 8));
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = layoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }

            View pictureView = view.findViewById(R.id.picture_view);
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView canReserveTxv = (TextView) view.findViewById(R.id.can_reserve_txv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView categoryTxv = (TextView) view.findViewById(R.id.category_txv);
            TextView moneyTxv = (TextView) view.findViewById(R.id.money_txv);
            TextView commentTxv = (TextView) view.findViewById(R.id.comment_txv);
            RatingBar startsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);

            POI poi = getItem(position);
            int aTotal = 0;
            for(int i = 0, count = getCount(); i < count && i < 2; i++) {
                if (getItem(i).getResultType() == POIResponse.FIELD_A_POI_LIST) {
                    aTotal++;
                    if (aTotal > 1) {
                        break;
                    }
                }
            }
            
            int hotelPicWidth = 0;
            Hotel hotel = poi.getHotel();
            if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType) && poi.getSubType() == POI.SUB_TYPE_HOTEL) {
                TKDrawable tkDrawable = hotel.getImageThumb();
                if (tkDrawable != null) {
                    Drawable drawable = tkDrawable.loadDrawable(activity, loadedDrawableRun, viewToken);
                    if(drawable != null) {
                        //To prevent the problem of size change of the same pic 
                        //After it is used at a different place with smaller size
                        Rect bounds = drawable.getBounds();
                        if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
                            pictureImv.setBackgroundDrawable(null);
                        }
                        pictureImv.setBackgroundDrawable(drawable);
                    } else {
                        pictureImv.setBackgroundDrawable(null);
                    }
                    
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture_none);
                }
                
                if (hotel.getCanReserve() > 0) {
                    canReserveTxv.setVisibility(View.GONE);
                } else {
                    canReserveTxv.setVisibility(View.VISIBLE);
                }
                pictureView.setVisibility(View.VISIBLE);
                hotelPicWidth = this.hotelPicWidth;
            } else {
                pictureView.setVisibility(View.GONE);
                canReserveTxv.setVisibility(View.VISIBLE);
            }
            
            nameTxv.setText(poi.getName());
            
            showDistance(activity, distanceFromTxv, distanceTxv, showStamp ? poi.getToCenterDistance() : null);
            
            boolean dish = false;
            List<DynamicPOI> list = poi.getDynamicPOIList();
            if (list != null) {
                for(int i = 0, size = list.size(); i < size; i++) {
                    String dataType = list.get(i).getType();
                    if (BaseQuery.DATA_TYPE_DISH.equals(dataType)) {
                        dish = true;
                        break;
                    }
                }
            }
            if (dish) {
                icDish.setBounds(0, 0, icDish.getIntrinsicWidth(), icDish.getIntrinsicHeight());
                categoryTxv.setCompoundDrawables(null, null, icDish, null);
                categoryTxv.setCompoundDrawablePadding(padding);
            } else {
                categoryTxv.setCompoundDrawables(null, null, null, null);
                categoryTxv.setCompoundDrawablePadding(0);
            }
            
            String str = poi.getCategory();
            if (!TextUtils.isEmpty(str)) {
                categoryTxv.setText(str);
            } else {
                categoryTxv.setText("");
            }
            
            if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                String price = poi.getPrice();
                if (TextUtils.isEmpty(price)) {
                    moneyTxv.setText("");
                } else {
                    moneyTxv.setText(price);
                }
            } else {
                long money = poi.getPerCapity();
                if (money > -1) {
                    moneyTxv.setText(activity.getString(R.string.yuan, money));
                } else {
                    moneyTxv.setText("");
                }
            }

            if (poi.containsDescription(POI.Description.FIELD_LINE)) {
                String name = poi.getDescriptionName(activity, POI.Description.FIELD_LINE);
                String value = poi.getDescriptionValue(POI.Description.FIELD_LINE);
                commentTxv.setText(name + ": " + value);
                commentTxv.setCompoundDrawables(null, null, null, null);
                commentTxv.setVisibility(View.VISIBLE);
            } else {
                str = null;
                Comment lastComment = poi.getLastComment();
                if (lastComment != null) {
                    str = lastComment.getContent();
                }
                if (!TextUtils.isEmpty(str)) {
                    commentTxv.setText(commentTitle+str);
                    commentTxv.setVisibility(View.VISIBLE);
                } else {
                    str = poi.getAddress();
                    if (!TextUtils.isEmpty(str)) {
                        commentTxv.setText(addressTitle+str);
                        commentTxv.setVisibility(View.VISIBLE);
                    } else {
                        commentTxv.setVisibility(View.INVISIBLE);
                    }
                }
            }

            float star = poi.getGrade();
            startsRtb.setRating(star/2.0f);

            ImageView stampImv = (ImageView) view.findViewById(R.id.stamp_imv);
            int from = poi.getFrom();
             
            if (showStamp
                    // 跟马然沟通，本地搜藏或历史浏览的在我要点评是没有戳的，但是在加载出来的非本地poi若用户点评过是应该有戳的。但是现在没有戳。
                    || from == POI.FROM_ONLINE) {
                if (poi.isGoldStamp()) {
                    stampImv.setImageResource(R.drawable.ic_stamp_gold);
                    stampImv.setVisibility(View.VISIBLE);
                } else if (poi.isSilverStamp()) {
                    stampImv.setImageResource(R.drawable.ic_stamp_silver);
                    stampImv.setVisibility(View.VISIBLE);
                } else {
                    stampImv.setVisibility(View.GONE);
                }
            } else {
                stampImv.setVisibility(View.GONE);
            }
            
            ViewGroup dynamicPOIListView = (ViewGroup) view.findViewById(R.id.dynamic_poi_list_view);
            int viewIndex = refresDynamicPOI(list, dynamicPOIListView);
            int dynamicPOIWidth = viewIndex * dynamicPOIIconWidth;
            
            int viewCount = dynamicPOIListView.getChildCount();
            for(int i = viewIndex; i < viewCount; i++) {
                dynamicPOIListView.getChildAt(i).setVisibility(View.GONE);
            }
            if (dynamicPOIWidth > 0) {
                nameTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int nameTxvWidth = nameTxv.getMeasuredWidth();
                int width = nameMaxWidth-dynamicPOIWidth-hotelPicWidth;
                if (nameTxvWidth > width) {
                    nameTxv.getLayoutParams().width = width;
                } else {
                    nameTxv.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
                }
            } else {
                nameTxv.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
            }
            
            return view;
        }
        
        public static void showDistance(Context context, TextView distanceFromTxv, TextView distanceTxv, String distance) {
            if (!TextUtils.isEmpty(distance)) {
                String distanceA = context.getString(R.string.distanceA);
                if (distance.startsWith(distanceA)) {
                    Drawable icAPOI = context.getResources().getDrawable(R.drawable.ic_location_nearby);
                    icAPOI.setBounds(0, 0, icAPOI.getIntrinsicWidth(), icAPOI.getIntrinsicHeight());
                    distanceFromTxv.setCompoundDrawables(null, null, icAPOI, null);
                    distanceFromTxv.setText(context.getString(R.string.distance));
                    distanceTxv.setText(distance.replace(distanceA, ""));
                } else {
                    distanceFromTxv.setText("");
                    distanceFromTxv.setCompoundDrawables(null, null, null, null);
                    distanceTxv.setText(distance);
                }
            } else {
                distanceFromTxv.setText("");
                distanceFromTxv.setCompoundDrawables(null, null, null, null);
                distanceTxv.setText("");
            }
        }
        
        int refresDynamicPOI(List<DynamicPOI> list, ViewGroup listView) {

            if (list == null || list.isEmpty()) {
                return 0;
            }
            
            DPOITypeList.clear();
            int viewCount = listView.getChildCount();
            int viewIndex = 0;
            for(int i = 0, size = list.size(); i < size; i++) {
                final DynamicPOI dynamicPOI = list.get(i);
                final String dataType = dynamicPOI.getType();
                if (!DPOITypeList.contains(dataType)) {
                    DPOITypeList.add(dataType);
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                        ImageView iconImv = getImageView(viewIndex, viewCount, listView);
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
                    } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                        ImageView iconImv = getImageView(viewIndex, viewCount, listView);
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_yanchu);
                    } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                        ImageView iconImv = getImageView(viewIndex, viewCount, listView);
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_zhanlan);
                    } else if (DynamicPOI.TYPE_HOTEL.equals(dataType) && !BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        ImageView iconImv = getImageView(viewIndex, viewCount, listView);
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_hotel);
                    } else if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                        ImageView iconImv = getImageView(viewIndex, viewCount, listView);
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_dianying);
                    } else if (BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                        ImageView iconImv = getImageView(viewIndex, viewCount, listView);
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_coupon);
                    } else {
                        continue;
                    }
                    viewIndex++;
                }
            }
            
            return viewIndex;
        }
        
        ImageView getImageView(int viewIndex, int viewCount, ViewGroup listView) {
            ImageView imageView;
            if (viewIndex < viewCount) {
                imageView = (ImageView) listView.getChildAt(viewIndex);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView = new ImageView(activity);
                listView.addView(imageView);
            }
            return imageView;
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
        setData((DataQuery) (tkAsyncTask.getBaseQuery()), false);
    }
    
    public void setSelectionFromTop() {
        mSphinx.getHandler().post(new Runnable() {

            @Override
            public void run() {
                mResultLsv.setSelectionFromTop(0, 0);
            }
        });
    }
    
    public void setData(DataQuery dataQuery, boolean resetFilter) {
        String subDataType = dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE);
        mResultAdapter.setSubDataType(subDataType);
        if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
            mRetryView.setText(R.string.can_not_found_result_and_retry, false);
        }
        
        mResultLsv.onRefreshComplete(false);
        if (dataQuery.isStop()) {
            return;
        }

        mResultLsv.setFooterSpringback(false);
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else {
            Response response = dataQuery.getResponse();
            if (response != null) {
                int responsCode = response.getResponseCode();
                if (responsCode != Response.RESPONSE_CODE_OK) {
                    if (dataQuery.isTurnPage()) {
                        return;
                    }
                    int resId = R.id.view_invalid;
                    if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        int responseCode = response.getResponseCode();
                        if (responseCode == 702) {
                            resId = R.string.response_code_702;
                        } else if (responseCode == 703) {
                            resId = R.string.response_code_703;
                        }
                        
                    }
                    
                    if (resId == R.id.view_invalid) {
                        int resid = BaseActivity.getResponseResId(dataQuery);
                        mRetryView.setText(resid, true);
                    } else {
                        mRetryView.setText(resId, false);
                    }
                    mState = STATE_ERROR;
                    updateView();
                    return;
                } else {
                    if (dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_FILTER_STRING)) {
                        dataQuery.delParameter(DataQuery.SERVER_PARAMETER_FILTER_STRING);
                    }
                }
            } else {
                if (dataQuery.isTurnPage()) {
                    mResultLsv.setFooterLoadFailed(true);
                    return;
                }
                mRetryView.setText(R.string.touch_screen_and_retry, true);
                mState = STATE_ERROR;
                updateView();
                return;
            }
        }
        
        if (dataQuery.isTurnPage() == false &&
                ((dataQuery.getFilterList() != null && dataQuery.getFilterList().size() > 0)
                    || resetFilter)) {
            List<Filter> filterList = dataQuery.getFilterList();
            refreshFilter(filterList);
        }

        POIResponse poiResponse = (POIResponse)dataQuery.getResponse();
        
        POIList poiList = poiResponse.getBPOIList();
        
        mDataQuery = dataQuery;
        if (poiList != null && 
                poiList.getList() != null && 
                poiList.getList().size() > 0) {
            mState = STATE_LIST;

            List<POI> bPOIList = null;
            if (poiList != null) {
                mBTotal = poiList.getTotal();
                bPOIList = poiList.getList();
            }
            
            if (!dataQuery.isTurnPage()) {
                mInputText = dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_KEYWORD);
                mSphinx.getHandler().post(new Runnable() {

                    @Override
                    public void run() {
                        mResultLsv.setSelectionFromTop(0, 0);
                    }
                });
                mPOIList.clear();
                mAPOI = dataQuery.getPOI();
                Position centerPosition = poiList.getPosition();
                if (centerPosition != null) {
                    if (mAPOI != null && !centerPosition.equals(mAPOI.getPosition())) {
                        mAPOI = null;
                    }
                    if (mAPOI == null &&
                            dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE) &&
                            dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE)) {
                        Position position = new Position(Double.valueOf(dataQuery.getParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE)), 
                                Double.valueOf(dataQuery.getParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE)));
                        if (!centerPosition.equals(position)) {
                            mAPOI = new POI();
                            mAPOI.setName(getString(R.string.my_location));
                            mAPOI.setPosition(position);
                        }
                    }
                } else {
                    mAPOI = null;
                }
                
                if (mAPOI != null) {
                    mAPOI.setOnlyAPOI(true);
                }
            }

            if (bPOIList != null) {
                if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                    for(int i = bPOIList.size()-1; i >= 0; i--) {
                        bPOIList.get(i).setSourceType(POI.SOURCE_TYPE_HOTEL);
                    }
                }
                mPOIList.addAll(bPOIList);
            }
            
            mResultLsv.setFooterSpringback(canTurnPage());
        } else {
            if (dataQuery.isTurnPage()) {
                return;
            }else{
            	mInputText = dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_KEYWORD);
            }
            mState = STATE_EMPTY;
        }

        refreshResultTitleText(dataQuery);
        updateView();
        mResultAdapter.notifyDataSetChanged();
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        
    }

    /**
     * 取消翻页
     * @return
     */
    private boolean canTurnPage() {
        return mPOIList.size() < mBTotal;
    }

    /**
     * 刷新POI列表的点评戳
     */
    public void refreshStamp() {
        if (mResultAdapter != null) {
            mResultAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * 显示筛选项列表
     * @param parent
     */
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
            mPopupWindow.setAnimationStyle(-1);
            mPopupWindow.update();
            mPopupWindow.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter + ActionLog.Dismiss);
                }
            });
            
        }
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }

    @Override
    public void retry() {
        if (mBaseQuerying != null && mBaseQuerying.size() > 0) {
        	for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                mBaseQuerying.get(i).setResponse(null);
        	}
            mSphinx.queryStart(mBaseQuerying);
            showQuering((DataQuery) mBaseQuerying.get(0));
        }
    }
    
    public DataQuery getDataQuery() {
        return mDataQuery;
    }
    
    public List<Filter> getFilterList() {
        return mFilterList;
    }
    
    public POI getAPOI() {
        return mAPOI;
    }
}

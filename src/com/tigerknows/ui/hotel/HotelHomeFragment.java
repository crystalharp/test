/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.HotelVendor;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterArea;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.ChangeCityActivity;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * 酒店搜索主页
 * @author Peng Wenyue
 */
public class HotelHomeFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack, DateListView.CallBack, PickLocationFragment.Invoker {

    public HotelHomeFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "HotelHomeFragment";
    
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    CityInfo mLocationCityInfo = null;
    
    boolean mRefreshFilterArea = false;
    
    Response mResponse = null;
    
    List<Filter> mFilterList = new ArrayList<DataQuery.Filter>();

    private ScrollView mScrollView;
    private View mCityView;
    private TextView mCityTxv;
    private View mCheckInTimeView;
    private Calendar mCheckInDat;
    private Calendar mCheckOutDat;
    private TextView mCheckInTimeTxv;
    private View mLocationView;
    private TextView mLocationTxv;
    private View mPriceView;
    private TextView mPriceTxv;
    private Button mQueryBtn;
    private View mDingdanView;
    private POI mPOI;
    private View mQueryAllHotelView;
    
    private FilterListView mFilterCategoryListView = null;
    
    private DateListView mDateListView = null;
    
    private ViewGroup mPopupWindowContain = null;
    
    PopupWindow.OnDismissListener mDateListViewDismiss = new PopupWindow.OnDismissListener() {
        
        @Override
        public void onDismiss() {
            mActionLog.addAction(mActionTag + ActionLog.HotelDate + ActionLog.Dismiss);
        }
    };
    
    PopupWindow.OnDismissListener mCategoryFilterViewDismiss = new PopupWindow.OnDismissListener() {
        
        @Override
        public void onDismiss() {
            mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter + ActionLog.Dismiss);
        }
    };
        
    private Dialog mProgressDialog = null;
    
    private boolean mSelectedLocation = false;
    
    public void setSelectedLocation(boolean selectedLocation) {
        mSelectedLocation = selectedLocation;
        mPOI = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.HotelQuery;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_home, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.hotel_reserve);
        refreshTitleRightBtn();
        refreshDate();
        
        if (mPOI != null) {
            FilterListView.selectedFilter(getFilter(getFilterList(), FilterArea.FIELD_LIST), -1);
        }
        if (mRefreshFilterArea) {
            mRefreshFilterArea = false;
            refreshFilterArea(false);
        }
        
        if (mResponse == null) {
            queryFilter();
        }
        
        if (TKConfig.getPref(mSphinx, TKConfig.PREFS_HINT_POI_HOME_HOTEL_RESERVE) == null) {
        }
        if (isReLogin()) {
            return;
        }
        
        smoothScrollToTop();
    }
    
    void refreshTitleRightBtn() {
        HotelVendor hotelVendor = HotelVendor.getHotelVendorById(HotelVendor.SOURCE_DEFAULT, mSphinx, null);
        if (hotelVendor != null && hotelVendor.getReserveTel() != null) {
            mRightBtn.setText(R.string.tel_reserve);
            mRightBtn.setTag(hotelVendor.getReserveTel());
            mRightBtn.setOnClickListener(this);
            mRightBtn.setBackgroundResource(R.drawable.btn_title);
        } else {
            mRightBtn.setText(null);
            mRightBtn.setOnClickListener(null);
            mRight2Btn.setBackgroundDrawable(null);
        }
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void queryFilter() {
        stopQuery();
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_HOTEL);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_APPENDACTION, DataQuery.APPENDACTION_NOSEARCH);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CHECKIN, SIMPLE_DATE_FORMAT.format(mCheckInDat.getTime()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CHECKOUT, SIMPLE_DATE_FORMAT.format(mCheckOutDat.getTime()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.setup(getId(), getId(), null, true);
        mSphinx.queryStart(dataQuery);
    }
    
    void stopQuery() {
        List<BaseQuery> list = mBaseQuerying;
        if (list != null && list.size() > 0) {
            for(int i = 0, size = list.size(); i < size; i++) {
                list.get(i).stop();
            }
        }
        TKAsyncTask tkAsyncTask = mTkAsyncTasking;
        if (tkAsyncTask != null) {
            tkAsyncTask.stop();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    public void smoothScrollToTop() {
        mScrollView.smoothScrollTo(0, 0);
    }

    protected void findViews() {
        mPopupWindowContain = new LinearLayout(mSphinx);
        mScrollView = (ScrollView) mRootView.findViewById(R.id.scroll_view);
        mCityView = mRootView.findViewById(R.id.city_view);
        mCityTxv = (TextView) mRootView.findViewById(R.id.city_txv);
        mCheckInTimeView = mRootView.findViewById(R.id.check_in_time_view);
        mCheckInTimeTxv = (TextView) mRootView.findViewById(R.id.check_in_time_txv);
        mLocationView = mRootView.findViewById(R.id.location_view);
        mLocationTxv = (TextView) mRootView.findViewById(R.id.location_txv);
        mPriceView = mRootView.findViewById(R.id.price_view);
        mPriceTxv = (TextView) mRootView.findViewById(R.id.price_txv);
        mQueryBtn = (Button) mRootView.findViewById(R.id.query_btn);
        mDingdanView = mRootView.findViewById(R.id.dingdan_view);
        mQueryAllHotelView = mRootView.findViewById(R.id.query_all_hotel_view);
    }

    protected void setListener() {
        mCityView.setOnClickListener(this);
        mCheckInTimeView.setOnClickListener(this);
        mLocationView.setOnClickListener(this);
        mPriceView.setOnClickListener(this);
        mQueryBtn.setOnClickListener(this);
        mDingdanView.setOnClickListener(this);
        mQueryAllHotelView.setOnClickListener(this);
    }
        
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.city_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryCity);
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                mSphinx.showView(R.id.activity_more_change_city, intent);
                break;
                
            case R.id.location_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryLocation);
                mSphinx.getPickLocationFragment().setInvoker(this);
                mSphinx.getPickLocationFragment().reset();
                Filter filter = getFilter(mFilterList, FilterArea.FIELD_LIST);
                if (filter == null || filter.getVersion().equals("0.0.0")) {
                    queryFilter();
                    showProgressDialog();
                } else {
                    mSphinx.getPickLocationFragment().setTitle(getString(R.string.hotel_select_location));
                    mSphinx.showView(R.id.view_hotel_pick_location);
                }
                break;
                
            case R.id.price_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryCategory);
                showFilterCategory(mSkylineView);
                break;
                
            case R.id.check_in_time_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryDate);
                showDateListView(mSkylineView);
                break;
                
            case R.id.query_btn:
                mActionLog.addAction(mActionTag + ActionLog.HotelQuerySubmit);
                if (isSupportHotelQuery()) {
                    mRefreshFilterArea = true;
                    submit();
                }
                break;
                
            case R.id.dingdan_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryOrder);
            	mSphinx.getHotelOrderListFragment().clearOrders();
            	mSphinx.getHotelOrderListFragment().syncOrder();
                mSphinx.showView(R.id.view_hotel_order_list);
            	break;
                
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                String tel = mRightBtn.getTag().toString();
                if (tel != null) {
                    Utility.telephone(mSphinx, tel);
                }
                break;
                
            case R.id.query_all_hotel_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryAll);
                DataQuery dataQuery = mSphinx.getHomeFragment().getDataQuery(getString(R.string.hotel));
                dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false, false, mSphinx.getCenterPOI());
                mSphinx.queryStart(dataQuery);
                break;
                
            default:
                break;
        }
    }
    
    public void resetDate() {
        getDateListView().refresh(null, null);
    }
    
    public static Filter getFilter(List<Filter> list, byte key) {
        synchronized (list) {
            Filter result = null;
            if (list != null)  {
                for(int i = list.size()-1; i >= 0; i--) {
                    if (list.get(i).getKey() == key) {
                        result = list.get(i);
                        break;
                    }
                }
            }
            return result;
        }
    }
    
    public static boolean checkFilter(List<Filter> list, byte key) {
        synchronized (list) {
            boolean result = false;
            if (list != null)  {
                for(int i = list.size()-1; i >= 0; i--) {
                    if (list.get(i).getKey() == key) {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        }
    }
    
    public static boolean deleteFilter(List<Filter> list, byte key) {
        synchronized (list) {
            boolean result = false;
            if (list != null)  {
                for(int i = list.size()-1; i >= 0; i--) {
                    if (list.get(i).getKey() == key) {
                        list.remove(i);
                        result = true;
                    }
                }
            }
            return result;
        }
    }
    
    public void setCityInfo(CityInfo cityInfo) {
        mCityTxv.setText(cityInfo.getCName());

        DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_HOTEL, mSphinx, cityInfo.getId());
        
        refreshFilterCategory();
        
        mResponse = null;
        mSelectedLocation = false;
        mRefreshFilterArea = true;
        refreshFilterArea(true);
        mPOI = null;
    }
    
    void refreshFilterCategory() {
        synchronized (mFilterList) {
            if (checkFilter(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY) == false) {
                FilterCategoryOrder filterCategory = DataQuery.getHotelFilterCategoryOrder();
                if (filterCategory != null) {
                    List<FilterOption> filterOptionList = filterCategory.getCategoryFilterOption();
                    List<Long> indexList = new ArrayList<Long>();
                    indexList.add(0l);
                    for(int i = 0, size = filterOptionList.size(); i < size; i++) {
                        long id = filterOptionList.get(i).getId();
                        indexList.add(id);
                    }
                    Filter filter = DataQuery.makeFilterResponse(mSphinx, indexList, filterCategory.getVersion(), filterOptionList, FilterCategoryOrder.FIELD_LIST_CATEGORY, false);
                    mFilterList.add(filter);
                }
            }  
        }
    	FilterListView.selectedFilter(getFilter(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY), 0);
        getFilterCategoryListView().setData(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY, this, false, false, mActionTag);
        refreshFilterCategoryView();
    }
    
    void refreshFilterArea(boolean reset) {
        synchronized (mFilterList) {
            int filterAreaState;
            CityInfo locationCityInfo = Globals.g_My_Location_City_Info;
            if (locationCityInfo != null &&
            		locationCityInfo.getId() == Globals.getCurrentCityInfo(mSphinx).getId()) {
                filterAreaState = 1;
                mLocationCityInfo = locationCityInfo;
            } else {
            	filterAreaState = 0;
            	mLocationCityInfo = null;
            }
                    
            FilterArea filterArea = DataQuery.getFilterArea();
            if (filterArea != null && filterArea.getAreaFilterOption().size() > 0) {
                List<FilterOption> filterOptionList = filterArea.getAreaFilterOption();
                List<Long> indexList = new ArrayList<Long>();
                if (filterAreaState == 0) {
                    long id = 0;
                    if (reset || mSelectedLocation == false) {
                        
                    } else {
                        Filter filter = getFilter(mFilterList, FilterArea.FIELD_LIST);
                        if (filter != null) {
                            id = FilterListView.getSelectedIdByFilter(filter);
                        }
                        if (id <= 10) {
                            id = 0;
                        }
                    }
                    indexList.add(id);
                } else if (filterAreaState == 1) {
                    long id = 10;
                    if (reset || mSelectedLocation == false) {
                        
                    } else {
                        Filter filter = getFilter(mFilterList, FilterArea.FIELD_LIST);
                        if (filter != null) {
                            id = FilterListView.getSelectedIdByFilter(filter);
                        }
                    }
                    indexList.add(id);
                }
                for(int i = 0, size = filterOptionList.size(); i < size; i++) {
                    long id = filterOptionList.get(i).getId();
                    if (filterAreaState == 0) {
                        if (id >= 1 && id <= 10) {
                            continue;
                        }
                    } else if (filterAreaState == 1) {
                        if (id >= 1 && id <= 5) {
                            continue;
                        }
                    }
                    indexList.add(id);
                }
    
                deleteFilter(mFilterList, FilterArea.FIELD_LIST);
                Filter filter = DataQuery.makeFilterResponse(mSphinx, indexList, filterArea.getVersion(), filterOptionList, FilterArea.FIELD_LIST);
                mFilterList.add(filter);
            } else {
                FilterArea quanguoFilterArea = DataQuery.getQuanguoFilterArea(mSphinx);
                if (quanguoFilterArea != null && quanguoFilterArea.getAreaFilterOption().size() > 0) {
                	List<FilterOption> filterOptionList = quanguoFilterArea.getAreaFilterOption();
                    List<Long> indexList = new ArrayList<Long>();
                    if (filterAreaState == 0) {
                        indexList.add(0l);
                        indexList.add(0l);
                    } else if (filterAreaState == 1) {
                        long id = 10;
                        if (reset || mSelectedLocation == false) {
                            
                        } else {
                            Filter filter = getFilter(mFilterList, FilterArea.FIELD_LIST);
                            if (filter != null) {
                                id = FilterListView.getSelectedIdByFilter(filter);
                            }
                        }
                        indexList.add(id);
                        indexList.add(0l);
                        indexList.add(6l);
                        indexList.add(7l);
                        indexList.add(8l);
                        indexList.add(9l);
                        indexList.add(10l);
                    }
                    
                    deleteFilter(mFilterList, FilterArea.FIELD_LIST);
                    Filter filter = DataQuery.makeFilterResponse(mSphinx, indexList, quanguoFilterArea.getVersion(), filterOptionList, FilterArea.FIELD_LIST);
                    mFilterList.add(filter);
                }
            }
        }
        
        if (mPOI != null) {
            Filter filter = getFilter(mFilterList, FilterArea.FIELD_LIST);
            if (filter != null) {
                FilterListView.selectedFilter(filter, -1);
            }
        }
        mSphinx.getPickLocationFragment().setData(mFilterList);
        refreshFilterAreaView();
    }
    
    /**
     * 显示进度对话框
     * @param id
     */
    void showProgressDialog() {
        if (mProgressDialog == null) {
            View custom = mSphinx.getLayoutInflater().inflate(R.layout.loading, null);
            TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
            loadingTxv.setText(getString(R.string.doing_and_wait));
            mProgressDialog = Utility.showNormalDialog(mSphinx, custom);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        if (mProgressDialog.isShowing() == false) {
            mProgressDialog.show();
        }
        
    }
    
    /**
     * 关闭进度对话框
     */
    void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (DataQuery.APPENDACTION_NOSEARCH.contains(baseQuery.getParameter(DataQuery.SERVER_PARAMETER_APPENDACTION))) {
            Response response = baseQuery.getResponse();
            mResponse = response;
            if (response == null) {
//                queryFilter();
                return;
            } else if (isSupportHotelQuery() == false) {
                return;
            }
            List<Filter> filterList = ((DataQuery) baseQuery).getFilterList();
            if (filterList != null) {
                
                boolean refresh = false;
                Filter filter = getFilter(filterList, FilterCategoryOrder.FIELD_LIST_CATEGORY);
                if (filter != null) {
                    synchronized (mFilterList) {
                        Filter presetFilter = getFilter(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY);
                        if (presetFilter != null) {
                            // TODO:这里是否需要比较两个筛选里面的各个子项，因为可能每个城市的筛选子项不一定全部相同
                            if (presetFilter.getVersion().equals(filter.getVersion()) == false) {
                                deleteFilter(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY);
                                mFilterList.add(filter);
                                refresh = true;
                            }
                        } else {
                            mFilterList.add(filter);
                            refresh = true;
                        }
                    }
                }
                
                if (refresh) {
                	getFilterCategoryListView().setData(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY, this, false, false, mActionTag);
                	refreshFilterCategoryView();
                }
                
                refresh = false;
                filter = getFilter(filterList, FilterArea.FIELD_LIST);
                if (filter != null) {
                	Filter presetFilter = getFilter(mFilterList, FilterArea.FIELD_LIST);
                	if (presetFilter != null) {
	                	if (presetFilter.getVersion().equals(filter.getVersion())) {
                            int id = FilterListView.getSelectedIdByFilter(filter);
                            int presetId = FilterListView.getSelectedIdByFilter(presetFilter);
                            if (id != presetId) {
                                refresh = true;
                            }
	                	} else {
	                		refresh = true;
	                	}
                	} else {
                		refresh = true;
                	}
                }
                
                if (refresh) {
                    refreshFilterArea(false);
                }
                if (mPOI != null) {
                    FilterListView.selectedFilter(getFilter(getFilterList(), FilterArea.FIELD_LIST), -1);
                }
                
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mSphinx.getPickLocationFragment().setTitle(getString(R.string.hotel_select_location));
                    mSphinx.showView(R.id.view_hotel_pick_location);
                    dismissProgressDialog();
                }
            }
            
        } else {
            InputSearchFragment.dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);
        }
    }
    
    boolean isSupportHotelQuery() {
        boolean result = true;
        Response response = mResponse;
        if (response != null && response.getResponseCode() != Response.RESPONSE_CODE_OK) {
            int responseCode = response.getResponseCode();
            if (responseCode == 702) {
                
                if (mSphinx.uiStackPeek() == getId()) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        dismissProgressDialog();
                    }
                    Utility.showNormalDialog(mSphinx, getString(R.string.response_code_702));
                }
                result = false;
            } else if (responseCode == 703) {
                if (mSphinx.uiStackPeek() == getId()) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        dismissProgressDialog();
                    }
                    Utility.showNormalDialog(mSphinx, getString(R.string.response_code_703));
                }
                result = false;
            }
        }
        
        return result;
    }
    
    /**
     * 刷新位置栏的内容
     */
    public void refreshFilterAreaView() {

        boolean result = false;
        List<Filter> filterList = getFilterList();
        POI poi = mPOI;
        if (poi != null) {
            mLocationTxv.setText(poi.getName());
            result = true;
        } else if (filterList != null){
            for(int i = 0, size = filterList.size(); i < size; i++) {
                Filter filter = filterList.get(i);
                if (filter.getKey() == FilterResponse.FIELD_FILTER_AREA_INDEX) {
                    mLocationTxv.setText(FilterListView.getFilterTitle(mSphinx, filter));
                    result = true;
                    break;
                }
            }
        }
        
        if (result == false) {
            mLocationTxv.setText(R.string.hotel_select_location);
        }
    }
    
    /**
     * 刷新星级/品牌/价格栏的内容
     */
    public void refreshFilterCategoryView() {
        boolean result = false;
        List<Filter> filterList = getFilterList();
        if (filterList != null) {
            for(int i = 0, size = filterList.size(); i < size; i++) {
                Filter filter = filterList.get(i);
                if (filter.getKey() == FilterResponse.FIELD_FILTER_CATEGORY_INDEX) {
                    mPriceTxv.setText(FilterListView.getFilterTitle(mSphinx, filter));
                    result = true;
                    break;
                }
            }
        }
        
        if (result == false) {
            mPriceTxv.setText(R.string.hotel_not_limit);
        }
    }
    
    private void makePopupWindow(View parent) {
        if (mPopupWindow == null) {
            
            mPopupWindow = new PopupWindow(mPopupWindowContain);
            mPopupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            mPopupWindow.setFocusable(true);
            // 设置允许在外点击消失
            mPopupWindow.setOutsideTouchable(true);

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setAnimationStyle(R.style.AlterImageDialog);
            mPopupWindow.update();
        }
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }
    
    private void showFilterCategory(View parent) {
        makePopupWindow(parent);
        mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter);
        mPopupWindow.setOnDismissListener(mCategoryFilterViewDismiss);
        mPopupWindowContain.removeAllViews();
        mPopupWindowContain.addView(getFilterCategoryListView());
        mPopupWindow.showAsDropDown(parent, 0, 0);

        mFilterCategoryListView.setData(mFilterList, FilterCategoryOrder.FIELD_LIST_CATEGORY, this, false, false, mActionTag);
    }
    
    FilterListView getFilterCategoryListView() {
        if (mFilterCategoryListView == null) {
            FilterListView view = new FilterListView(mSphinx);
            view.findViewById(R.id.body_view).setPadding(0, Globals.g_metrics.heightPixels-((int) (320*Globals.g_metrics.density)), 0, 0);
            View titleView = mLayoutInflater.inflate(R.layout.hotel_category_list_header, view, false);
            ((ViewGroup) view.findViewById(R.id.title_view)).addView(titleView);
            mFilterCategoryListView = view;
        }
        return mFilterCategoryListView;
    }
    
    private void showDateListView(View parent) {
        makePopupWindow(parent);
        mActionLog.addAction(mActionTag + ActionLog.HotelDate);
        mPopupWindow.setOnDismissListener(mDateListViewDismiss);
        DateListView view = getDateListView();
        mPopupWindowContain.removeAllViews();
        mPopupWindowContain.addView(getDateListView());
        mPopupWindow.showAsDropDown(parent, 0, 0);
        view.refresh(getCheckin(), getCheckout());
    }
    
    DateListView getDateListView() {
        if (mDateListView == null) {
            DateListView view = new DateListView(mSphinx);
            View v = mLayoutInflater.inflate(R.layout.time_list_item, this, false);
            v.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int h = v.getMeasuredHeight();
            view.findViewById(R.id.body_view).getLayoutParams().height = h*6;
            ((ViewGroup) view.findViewById(R.id.selected_view)).setPadding(0, h, 0, 0);
            view.setData(this, mActionTag);
            mDateListView = view;
        }
        return mDateListView;
    }

    @Override
    public void doFilter(String name) {
        mPriceTxv.setText(name);
        refreshFilterCategoryView();
        dismissPopupWindow();
    }

    @Override
    public void cancelFilter() {
        dismissPopupWindow();
    }
    
    void submit() {
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_HOTEL);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CHECKIN, SIMPLE_DATE_FORMAT.format(getDateListView().getCheckin().getTime()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CHECKOUT, SIMPLE_DATE_FORMAT.format(getDateListView().getCheckout().getTime()));
        
        POI poi = mPOI;
        if (poi != null) {
            Position position = poi.getPosition();
            if (position != null) {
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
            }
        } else {
            poi = new POI();
        }
        
        byte key = Byte.MIN_VALUE;
        List<Filter> filterList = getFilterList();
        if (dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_LONGITUDE)) {
            key = FilterResponse.FIELD_FILTER_AREA_INDEX;
        } else {
            key = Byte.MIN_VALUE;
            CityInfo locationCityInfo = mLocationCityInfo;
            int id = FilterListView.getSelectedIdByFilter(getFilter(mFilterList, FilterArea.FIELD_LIST));
            if (locationCityInfo != null &&
                    locationCityInfo.getId() == Globals.getCurrentCityInfo(mSphinx).getId() &&
                    id >= 6 &&
                    id <= 10) {
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_CITY, String.valueOf(locationCityInfo.getId()));
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(locationCityInfo.getPosition().getLon()));
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(locationCityInfo.getPosition().getLat()));
            }
        }
        
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(filterList, key));
                
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false, false, poi);
        mSphinx.queryStart(dataQuery);
    }
    
    public List<Filter> getFilterList() {
        return mFilterList;
    }

    @Override
    public void confirm() {
        refreshDate();
        dismissPopupWindow();
    }

    @Override
    public void cancel() {
        dismissPopupWindow();
    }
    
    public void refreshDate() {
        DateListView dateListView = getDateListView();
        mCheckInDat = dateListView.getCheckin();
        mCheckOutDat = dateListView.getCheckout();
        mCheckInTimeTxv.setText(dateListView.getCheckDescription(true).toString());
    }
    
    public Calendar getCheckin(){
        return mCheckInDat;
    }
    
    public Calendar getCheckout(){
        return mCheckOutDat;
    }

    @Override
    public void setPOI(POI poi) {
        mPOI = poi;
    }
}

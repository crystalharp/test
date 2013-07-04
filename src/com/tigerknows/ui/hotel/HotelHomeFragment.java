/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterArea;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.ChangeCityActivity;
import com.tigerknows.ui.poi.POIResultFragment;
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
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;


/**
 * 酒店搜索主页
 * @author Peng Wenyue
 */
public class HotelHomeFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack, DateListView.CallBack {

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

    private Button mCityBtn;
    private View mCheckView;
    private DateWidget mCheckInDat;
    private DateWidget mCheckOutDat;
    private Button mLocationBtn;
    private ViewGroup mPriceView;
    private TextView mPriceTxv;
    private Button mQueryBtn;
    private Button mDingdanBtn;
    
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
        
        DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_HOTEL, MapEngine.SW_ID_QUANGUO, mSphinx);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.hotel_reserve);
        refreshDate();
        
        mSphinx.showHint(TKConfig.PREFS_HINT_HOTEL_HOME, R.layout.hint_hotel_home);
        
        if (mSphinx.getPickLocationFragment().getPOI() != null) {
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
            TKConfig.setPref(mSphinx, TKConfig.PREFS_HINT_POI_HOME_HOTEL_RESERVE, "1");
            mSphinx.getPOIHomeFragment().getCategoryAdapter().notifyDataSetChanged();
        }
        if (isReLogin()) {
            return;
        }
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void queryFilter() {
        stopQuery();
        DataQuery dataQuery = new DataQuery(mSphinx);
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
        criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_HOTEL);
        criteria.put(DataQuery.SERVER_PARAMETER_APPENDACTION, DataQuery.APPENDACTION_NOSEARCH);
        criteria.put(DataQuery.SERVER_PARAMETER_CHECKIN, SIMPLE_DATE_FORMAT.format(mCheckInDat.getCalendar().getTime()));
        criteria.put(DataQuery.SERVER_PARAMETER_CHECKOUT, SIMPLE_DATE_FORMAT.format(mCheckOutDat.getCalendar().getTime()));
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null, true);
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

    protected void findViews() {
        mPopupWindowContain = new LinearLayout(mSphinx);
        mCityBtn = (Button) mRootView.findViewById(R.id.city_btn);
        mCheckView = (ViewGroup) mRootView.findViewById(R.id.check_view);
        mCheckInDat = (DateWidget) mRootView.findViewById(R.id.checkin_dat);
        mCheckOutDat = (DateWidget) mRootView.findViewById(R.id.checkout_dat);
        mLocationBtn = (Button) mRootView.findViewById(R.id.location_btn);
        mPriceView = (ViewGroup) mRootView.findViewById(R.id.price_view);
        mPriceTxv = (TextView) mRootView.findViewById(R.id.price_txv);
        mQueryBtn = (Button) mRootView.findViewById(R.id.query_btn);
        mDingdanBtn = (Button) mRootView.findViewById(R.id.dingdan_btn);
    }

    protected void setListener() {
        mCityBtn.setOnClickListener(this);
        mCheckView.setOnClickListener(this);
        mLocationBtn.setOnClickListener(this);
        mPriceView.setOnClickListener(this);
        mQueryBtn.setOnClickListener(this);
        mDingdanBtn.setOnClickListener(this);
    }
        
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.city_btn:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryCity);
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                mSphinx.showView(R.id.activity_more_change_city, intent);
                break;
                
            case R.id.location_btn:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryLocation);
                mSphinx.getPickLocationFragment().reset();
                Filter filter = getFilter(mFilterList, FilterArea.FIELD_LIST);
                if (filter == null || filter.getVersion().equals("0.0.0")) {
                    queryFilter();
                    showProgressDialog();
                } else {
                    mSphinx.showView(R.id.view_hotel_pick_location);
                }
                break;
                
            case R.id.price_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryCategory);
                showFilterCategory(mTitleFragment);
                break;
                
            case R.id.check_view:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryDate);
                showDateListView(mTitleFragment);
                break;
                
            case R.id.query_btn:
                mActionLog.addAction(mActionTag + ActionLog.HotelQuerySubmit);
                if (isSupportHotelQuery()) {
                    mRefreshFilterArea = true;
                    submit();
                }
                break;
                
            case R.id.dingdan_btn:
                mActionLog.addAction(mActionTag + ActionLog.HotelQueryOrder);
            	mSphinx.getHotelOrderListFragment().clearOrders();
            	mSphinx.getHotelOrderListFragment().syncOrders();
                mSphinx.showView(R.id.view_hotel_order_list);
            	break;
                
            default:
                break;
        }
    }
    
    public void resetDate() {
        getDateListView().refresh(null, null);
    }
    
    static Filter getFilter(List<Filter> list, byte key) {
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
    
    boolean checkFilter(List<Filter> list, byte key) {
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
    
    boolean deleteFilter(List<Filter> list, byte key) {
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
        Globals.setHotelCityInfo(cityInfo);
        mCityBtn.setText(cityInfo.getCName());

        DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_HOTEL, cityInfo.getId(), mSphinx);
        
        refreshFilterCategory();
        
        mResponse = null;
        mSelectedLocation = false;
        mRefreshFilterArea = true;
        refreshFilterArea(true);
        mSphinx.getPickLocationFragment().resetPOI();

        int cityId = Globals.getCurrentCityInfo().getId();
        MapEngine.getInstance().suggestwordCheck(mSphinx, cityId);
        HistoryWordTable.readHistoryWord(mSphinx, cityId, HistoryWordTable.TYPE_TRAFFIC);
        
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
            		locationCityInfo.getId() == Globals.getCurrentCityInfo().getId()) {
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
            loadingTxv.setText(mSphinx.getString(R.string.doing_and_wait));
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
        } else {
            Response response = baseQuery.getResponse();
            mResponse = response;
            if (response == null) {
                queryFilter();
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
                if (mSphinx.getPickLocationFragment().getPOI() != null) {
                    FilterListView.selectedFilter(getFilter(getFilterList(), FilterArea.FIELD_LIST), -1);
                }
                
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mSphinx.showView(R.id.view_hotel_pick_location);
                    dismissProgressDialog();
                }
            }
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
                    Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.response_code_702));
                }
                result = false;
            } else if (responseCode == 703) {
                if (mSphinx.uiStackPeek() == getId()) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        dismissProgressDialog();
                    }
                    Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.response_code_703));
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
        POI poi = mSphinx.getPickLocationFragment().getPOI();
        if (poi != null) {
            mLocationBtn.setText(poi.getName());
            result = true;
        } else if (filterList != null){
            for(int i = 0, size = filterList.size(); i < size; i++) {
                Filter filter = filterList.get(i);
                if (filter.getKey() == FilterResponse.FIELD_FILTER_AREA_INDEX) {
                    mLocationBtn.setText(FilterListView.getFilterTitle(mSphinx, filter));
                    result = true;
                    break;
                }
            }
        }
        
        if (result == false) {
            mLocationBtn.setText(R.string.hotel_select_location);
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
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_HOTEL);
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_CHECKIN, SIMPLE_DATE_FORMAT.format(getDateListView().getCheckin().getTime()));
        criteria.put(DataQuery.SERVER_PARAMETER_CHECKOUT, SIMPLE_DATE_FORMAT.format(getDateListView().getCheckout().getTime()));
        
        POI poi = mSphinx.getPickLocationFragment().getPOI();
        if (poi != null) {
            Position position = poi.getPosition();
            if (position != null) {
                criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
            }
        } else {
            poi = new POI();
        }
        
        byte key = Byte.MIN_VALUE;
        List<Filter> filterList = getFilterList();
        if (criteria.containsKey(DataQuery.SERVER_PARAMETER_LONGITUDE)) {
            key = FilterResponse.FIELD_FILTER_AREA_INDEX;
        } else {
            key = Byte.MIN_VALUE;
            CityInfo locationCityInfo = mLocationCityInfo;
            int id = FilterListView.getSelectedIdByFilter(getFilter(mFilterList, FilterArea.FIELD_LIST));
            if (locationCityInfo != null &&
                    locationCityInfo.getId() == Globals.getCurrentCityInfo().getId() &&
                    id >= 6 &&
                    id <= 10) {
                criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_CITY, String.valueOf(locationCityInfo.getId()));
                criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(locationCityInfo.getPosition().getLon()));
                criteria.put(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(locationCityInfo.getPosition().getLat()));
            }
        }
        
        criteria.put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(filterList, key));
                
        int targetViewId = mSphinx.getPOIResultFragmentID();
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), targetViewId, null, false, false, poi);
        BaseFragment baseFragment = mSphinx.getFragment(targetViewId);
        if (baseFragment != null && baseFragment instanceof POIResultFragment) {
            mSphinx.queryStart(dataQuery);
            ((POIResultFragment)mSphinx.getFragment(targetViewId)).setup();
            mSphinx.showView(targetViewId);
        }
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
        mCheckInDat.setCalendar(dateListView.getCheckin());
        mCheckOutDat.setCalendar(dateListView.getCheckout());
    }
    
    public Calendar getCheckin(){
        return mCheckInDat.getCalendar();
    }
    
    public Calendar getCheckout(){
        return mCheckOutDat.getCalendar();
    }
}

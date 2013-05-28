/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.ChangeCityActivity;
import com.tigerknows.ui.poi.POIResultFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;

import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.Toast;
import android.widget.PopupWindow.OnDismissListener;

import java.text.SimpleDateFormat;
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

    private Button mCityBtn;
    private View mCheckView;
    private DateWidget mCheckInDat;
    private DateWidget mCheckOutDat;
    private Button mLocationBtn;
    private ViewGroup mPriceView;
    private TextView mPriceTxv;
    private Button mQueryBtn;
    private Button mDingdanBtn;
    
    private FilterListView mFilterListView = null;
    
    private DateListView mDateListView = null;
    
    private ViewGroup mPopupWindowContain = null;
    
    /**
     * 筛选数据
     * 每次进入打开此页面时，检查筛选数据是否为空，若为空则发起查询以获取筛选数据，此查询会重试
     * 点击位置或价格时，若此时无筛选数据，则弹出进度对话框，直到筛选数据返回时取消进度对话框，
     * 然后再显示选择位置或价格的页面
     */
    private List<Filter> mFilterList;
    
    private int mCityId = MapEngine.CITY_ID_INVALID;
    
    private Dialog mProgressDialog = null;

    private int mClickedViewId;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIHome;
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
        mTitleBtn.setText("酒店预订");
        refreshDate();
        if (mFilterList == null ||
                mCityId != Globals.getCurrentCityInfo().getId()) {
            mFilterList = null;
            mLocationBtn.setText("选择位置");
            mPriceTxv.setText("不限");
            queryFilter();
        }
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void queryFilter() {
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
        List<Filter> filterList = mFilterList;
        int id = view.getId();
        switch (id) {
            case R.id.city_btn:
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                mSphinx.showView(R.id.activity_more_change_city, intent);
                break;
                
            case R.id.location_btn:
                if (filterList == null) {
                    showProgressDialog(id);
                } else {
                    mSphinx.showView(R.id.view_hotel_pick_location);
                }
                break;
                
            case R.id.price_view:
                if (filterList == null) {
                    showProgressDialog(id);
                } else {
                    showFilterCategory(mTitleFragment);
                    getFilterListView().setData(filterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
                }
                break;
                
            case R.id.check_view:
                showDateListView(mTitleFragment);
                break;
                
            case R.id.query_btn:
                if (filterList == null) {
                    Toast.makeText(mSphinx, "请填写位置", Toast.LENGTH_LONG).show();
                } else {
                    submit();
                }
                break;
                
            case R.id.dingdan_btn:
            	mSphinx.getHotelOrderListFragment().clearOrders();
                mSphinx.showView(R.id.view_hotel_order_list);
            	break;
                
            case R.id.confirm_btn:
                dismissPopupWindow();
                break;
                
            default:
                break;
        }
    }
    
    public void resetDate() {
        Calendar checkOut = Calendar.getInstance();
        checkOut.add(Calendar.DAY_OF_YEAR, 1);
        getDateListView().onResume(Calendar.getInstance(), checkOut);
    }
    
    public void setCityInfo(CityInfo cityInfo) {
        Globals.setHotelCityInfo(cityInfo);
        mCityBtn.setText(cityInfo.getCName());
        if (cityInfo.getId() != mCityId) {
            mFilterList = null;
            mLocationBtn.setText("选择位置");
            mPriceTxv.setText("不限");
        }
    }
    
    /**
     * 显示进度对话框
     * @param id
     */
    void showProgressDialog(int id) {
        mClickedViewId = id;
        if (mProgressDialog == null) {
            View custom = mSphinx.getLayoutInflater().inflate(R.layout.loading, null);
            TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
            loadingTxv.setText(mSphinx.getString(R.string.doing_and_wait));
            mProgressDialog = Utility.showNormalDialog(mSphinx, custom);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mClickedViewId = -1;
                }
            });
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
        Response response = baseQuery.getResponse();
        if (response != null && response.getResponseCode() == Response.RESPONSE_CODE_OK) {
            mFilterList = ((DataQuery) baseQuery).getFilterList();
            List<Filter> filterList = mFilterList;
            if (filterList != null) {
                mCityId = Globals.getCurrentCityInfo().getId();
                mSphinx.getPickLocationFragment().setData(filterList);
                refreshFilterArea();
                refreshFilterCategory();
                
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    if (mClickedViewId == R.id.location_btn) {
                        mSphinx.showView(R.id.view_hotel_pick_location);
                    } else if (mClickedViewId == R.id.price_view) {
                        showFilterCategory(mTitleFragment);
                        mFilterListView.setData(filterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
                    }
                    dismissProgressDialog();
                }
            }
        }
    }
    
    /**
     * 刷新位置栏的内容
     */
    public void refreshFilterArea() {

        boolean result = false;
        List<Filter> filterList = mFilterList;
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
            mLocationBtn.setText("选择位置");
        }
    }
    
    /**
     * 刷新星级/品牌/价格栏的内容
     */
    public void refreshFilterCategory() {
        boolean result = false;
        List<Filter> filterList = mFilterList;
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
            mPriceTxv.setText("不限");
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
            mPopupWindow.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter + ActionLog.Dismiss);
                }
            });
            
        }
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }
    
    private void showFilterCategory(View parent) {
        makePopupWindow(parent);
        mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter);
        mPopupWindowContain.removeAllViews();
        mPopupWindowContain.addView(getFilterListView());
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }
    
    FilterListView getFilterListView() {
        if (mFilterListView == null) {
            FilterListView view = new FilterListView(mSphinx);
            view.setDeleteFirstChild(true);
            view.findViewById(R.id.body_view).setPadding(0, Globals.g_metrics.heightPixels-((int) (320*Globals.g_metrics.density)), 0, 0);
            View titleView = mLayoutInflater.inflate(R.layout.hotel_category_list_header, view, false);
            ((ViewGroup) view.findViewById(R.id.title_view)).addView(titleView);
            mFilterListView = view;
        }
        return mFilterListView;
    }
    
    private void showDateListView(View parent) {
        makePopupWindow(parent);
        DateListView view = getDateListView();
        mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter);
        mPopupWindowContain.removeAllViews();
        mPopupWindowContain.addView(getDateListView());
        mPopupWindow.showAsDropDown(parent, 0, 0);
        view.onResume(getCheckin(), getCheckout());
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
        refreshFilterCategory();
        dismissPopupWindow();
    }

    @Override
    public void cancelFilter() {
        // TODO Auto-generated method stub
        
    }
    
    void submit() {
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_HOTEL);
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_CHECKIN, SIMPLE_DATE_FORMAT.format(getDateListView().getCheckin().getTime()));
        criteria.put(DataQuery.SERVER_PARAMETER_CHECKOUT, SIMPLE_DATE_FORMAT.format(getDateListView().getCheckout().getTime()));
        
        POI poi = mSphinx.getPickLocationFragment().getPOI();
        byte key = FilterResponse.FIELD_FILTER_AREA;
        if (poi != null) {
            Position position = poi.getPosition();
            if (position != null) {
                criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
                key = Byte.MIN_VALUE;
            }
        }
        if (key == FilterResponse.FIELD_FILTER_AREA) {
            criteria.put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList, key));
            Filter filterArea = null;
            for(int i = this.mFilterList.size()-1; i >= 0; i--) {
                Filter filter = this.mFilterList.get(i);
                if (filter.getKey() == key) {
                    filterArea = filter;
                }
            }
            poi = new POI();
            poi.setName(FilterListView.getFilterTitle(mSphinx, filterArea));
        }
                
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

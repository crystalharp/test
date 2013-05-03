/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.HotelResponse;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.ChangeCityActivity;
import com.tigerknows.util.Utility;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Hashtable;
import java.util.List;


/**
 * 酒店搜索主页
 * @author Peng Wenyue
 */
public class HotelHomeFragment extends BaseFragment implements View.OnClickListener {

    public HotelHomeFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "HotelHomeFragment";

    private Button mCityBtn;
    private DateWidget mCheckInDat;
    private DateWidget mCheckOutDat;
    private Button mLocationBtn;
    private ViewGroup mPriceView;
    private TextView mPriceTxv;
    private Button mQueryBtn;
    private Button mDingdanBtn;
    
    /**
     * 酒店查询的目标城市
     * 打开此页面时，默认设为当前所选城市
     * 用户在此页面切换酒店查询的目标城市时并不影响当前所选城市
     */
    private CityInfo mCityInfo;
    
    /**
     * 星级/品牌/价格
     */
    private int mPrice;
    
    /**
     * 筛选数据
     * 每次进入打开此页面时，检查筛选数据是否为空，若为空则发起查询以获取筛选数据，此查询会重试
     * 点击位置或价格时，若此时无筛选数据，则弹出进度对话框，直到筛选数据返回时取消进度对话框，
     * 然后再显示选择位置或价格的页面
     */
    private HotelResponse mHotelResponse;
    
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
        mClickedViewId = -1;
        if (mHotelResponse == null) {
            query();
        }
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void query() {
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
        dataQuery.setup(criteria, mCityInfo.getId(), getId(), getId(), null, true);
        mSphinx.queryStart(dataQuery);
    }

    @Override
    public void onPause() {
        super.onPause();
        mClickedViewId = -1;
    }

    protected void findViews() {
        mCityBtn = (Button) mRootView.findViewById(R.id.city_btn);
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
        mCheckInDat.setOnClickListener(this);
        mCheckOutDat.setOnClickListener(this);
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
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                mSphinx.showView(R.id.activity_more_change_city, intent);
                break;
                
            case R.id.location_btn:
                if (mHotelResponse == null) {
                    showProgressDialog(id);
                } else {
                    
                }
                
            case R.id.price_view:
                if (mHotelResponse == null) {
                    showProgressDialog(id);
                } else {
                    
                }
                
            default:
                break;
        }
    }
    
    public void setCityInfo(CityInfo cityInfo) {
        if (cityInfo == null) {
            return;
        }
        mCityInfo = cityInfo;
        mCityBtn.setText(cityInfo.getCName());
    }
    
    public CityInfo getCityInfo() {
        return mCityInfo;
    }
    
    void showProgressDialog(int id) {
        mClickedViewId = id;
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
            mHotelResponse = (HotelResponse) response;
            if (mClickedViewId == R.id.location_btn) {
                
            } else if (mClickedViewId == R.id.price_view) {
                
            }
        }
    }
    
}

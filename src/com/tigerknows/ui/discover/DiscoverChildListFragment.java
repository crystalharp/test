/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.DataQuery.BaseList;
import com.tigerknows.model.DataQuery.FendianResponse;
import com.tigerknows.model.DataQuery.YingxunResponse;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIResultFragment.POIAdapter;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Peng Wenyue
 */
public class DiscoverChildListFragment extends DiscoverBaseFragment implements View.OnClickListener {
    
    static final String TAG = "DiscoverChildListFragment";

    public DiscoverChildListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private TextView mResultTxv;

    private SpringbackListView mResultLsv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private DataQuery mDataQuery;
    
    private FendianAdapter mFendianAdapter = null;
    
    private YingxunAdapter mYingxunAdapter;
    
    private List<Fendian> mFendianList = new ArrayList<Fendian>();
    
    private List<Yingxun> mYingxunList = new ArrayList<Yingxun>();
    
    private Tuangou mTuangou;
    
    private Dianying mDianying;
    
    private BaseList mBaseList;
    
    private String mDataType;
    
    private int toScrollDownPos = 0;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                mResultLsv.getView(false).performClick();
            }
        }
    };
    
    public void setup(Object object, String resultStr, String actionTag) {
        mResultLsv.setFooterSpringback(false);
        mDataQuery = null;
        mBaseList = null;
        mActionTag = actionTag;
        
        String filter = DataQuery.makeFilterRequest(mSphinx.getDiscoverListFragment().getFilterList());
        DataQuery dataQuery = new DataQuery(mSphinx.getDiscoverListFragment().getLastQuery());
        
        if (object instanceof Tuangou) {
            mTuangou = (Tuangou) object;
            mDataType = BaseQuery.DATA_TYPE_FENDIAN;
            if (mFendianAdapter == null) {
                mFendianAdapter = new FendianAdapter(mSphinx, mFendianList);
            }
            mResultLsv.setAdapter(mFendianAdapter);
            mResultTxv.setText(resultStr);

            mFendianList.clear();
            mFendianAdapter.notifyDataSetChanged();

            DataQuery fendianQuery = mTuangou.getFendianQuery();
            if (fendianQuery != null && filter.equals(fendianQuery.getParameter(DataQuery.SERVER_PARAMETER_FILTER))) {
                setDataQuery(fendianQuery, true);
                return;
            }
            
            mTuangou.setFendianQuery(null);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_TUANGOU_UUID, mTuangou.getUid());
            
        } else if (object instanceof Dianying) {
            mDianying = (Dianying) object;
            mDataType = BaseQuery.DATA_TYPE_YINGXUN;
            if (mYingxunAdapter == null) {
                mYingxunAdapter = new YingxunAdapter(mSphinx, mYingxunList);
            }
            mResultLsv.setAdapter(mYingxunAdapter);
            mResultTxv.setText(resultStr);

            mYingxunList.clear();
            mYingxunAdapter.notifyDataSetChanged();
            
            DataQuery yingxunQuery = mDianying.getYingxunQuery();
            if (yingxunQuery != null && filter.equals(yingxunQuery.getParameter(DataQuery.SERVER_PARAMETER_FILTER))) {
                setDataQuery(yingxunQuery, true);
                return;
            }
            
            mDianying.setYingxunQuery(null);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DIANYING_UUID, mDianying.getUid());
        }
        
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(0));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, mDataType);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_FILTER, filter);
        dataQuery.delParameter(DataQuery.SERVER_PARAMETER_PICTURE);
        dataQuery.setup(getId(), getId(), null, false, true, null);
        
        mSphinx.queryStart(dataQuery);
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.FendianList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.discover_tuangou_fendian_list, container, false);

        findViews();
        setListener();
        
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
        mBaseList = null;
    }
    
    @Override
    protected void findViews() {
        super.findViews();
        mResultTxv = (TextView) mRootView.findViewById(R.id.result_txv);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage();
            }
        });
    }
    
    private void refreshResultTxv(DataQuery dataQuery) {
        String str = null;
        
        if (getList().size() > 0) {
            str = mResultTxv.getText().toString();
        }

        if (TextUtils.isEmpty(str)) {
            str = getString(R.string.no_result);
        }
        
        mResultTxv.setText(str);
        mEmptyTxv.setText(str);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            mTitleBtn.setText(R.string.fendian_list);
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            mTitleBtn.setText(R.string.dianyingyuan_list);
        }
        
        if (isReLogin()) {
            return;
        }
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        refreshRightBtn();
    }
    
    private void refreshRightBtn() {
//        if (getList().isEmpty()) {
//            mRightBtn.setVisibility(View.GONE);
//        } else {
//            mRightBtn.setVisibility(View.VISIBLE);
//        }
    }
    
    private void turnPage(){
        synchronized (this) {
        DataQuery lastDataQuery = mDataQuery;
        BaseList lastBaseList = mBaseList;
        if (lastBaseList == null || mResultLsv.isFooterSpringback() == false || lastDataQuery == null) {
            return;
        }
        if (lastBaseList.getTotal() <= getList().size()) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);

        DataQuery dataQuery = new DataQuery(lastDataQuery);
        //FIXME:获取参数，修改，然后重新请求。加个函数
        dataQuery.setParameter(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(getList().size()));
        dataQuery.setup(getId(), getId(), null, true, false, dataQuery.getPOI());
        mSphinx.queryStart(dataQuery);
        }
    }

    private void viewMap(int firstVisiblePosition, int lastVisiblePosition) {
        int size = getList().size();
        int[] page = Utility.makePagedIndex(mResultLsv, size, firstVisiblePosition);        
        int minIndex = page[0];
        int maxIndex = page[1];
        List<POI> poiList = new ArrayList<POI>();
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mFendianList.size(); minIndex++) {
                Fendian fendian = mFendianList.get(minIndex);
                poiList.add(fendian.getPOI(POI.SOURCE_TYPE_FENDIAN));
            }
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mYingxunList.size(); minIndex++) {
                Yingxun yingxun = mYingxunList.get(minIndex); 
                poiList.add(yingxun.getPOI(POI.SOURCE_TYPE_YINGXUN));
            }
        }
        if (poiList.isEmpty()) {
            return;
        }

        viewMap(poiList, page[2]);
    }
    
    private void viewMap(List<POI> poiList, int index) {
        boolean yingxun = BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType);
        mSphinx.getResultMapFragment().setData(getString(yingxun ? R.string.dianying_ditu : R.string.shanghu_ditu), yingxun ? ActionLog.ResultMapDianyingBranchList : ActionLog.ResultMapTuangouBranchList);
        mSphinx.showView(R.id.view_result_map);   
        ItemizedOverlayHelper.drawPOIOverlay(mSphinx, poiList, index);
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_btn:
                if (getList().isEmpty()) {
                    return;
                }
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                viewMap(mResultLsv.getFirstVisiblePosition(), mResultLsv.getLastVisiblePosition());
                break;
                
            default:
                break;
        }
    }
    
    public class FendianAdapter extends ArrayAdapter<Fendian>{
        private static final int RESOURCE_ID = R.layout.discover_tuangou_fendian_list_item;
        
        public FendianAdapter(Context context, List<Fendian> list) {
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
            
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            View addressView = view.findViewById(R.id.address_view);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            final TextView telephoneTxv = (TextView) view.findViewById(R.id.telephone_txv);
            View telephoneView = view.findViewById(R.id.telephone_view);

            final Fendian fendian = getItem(position);
            String distance = fendian.getDistance();
            String address = fendian.getAddress();
            String phone = fendian.getPlacePhone();
            showPOI(mSphinx, position+1, fendian.getPlaceName(), distance, address, phone, 
                    nameTxv, distanceFromTxv, distanceTxv, addressView, telephoneView, addressTxv, telephoneTxv, 
                    R.drawable.list_middle, R.drawable.list_footer, R.drawable.list_footer, null, null);
            
            View.OnClickListener onClickListener = new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    int id = view.getId();
                    if (id == R.id.address_view) {
                        mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);
                        POI poi = fendian.getPOI(POI.SOURCE_TYPE_FENDIAN);
                        Utility.queryTraffic(mSphinx, poi, mActionTag);
                    } else if (id == R.id.telephone_view) {
                        mActionLog.addAction(mActionTag +  ActionLog.CommonTelphone);
                        Utility.telephone(mSphinx, telephoneTxv);
                    }
                }
            };
            
            addressView.setOnClickListener(onClickListener);
            telephoneView.setOnClickListener(onClickListener);
            
            return view;
        }
    }
    
    public static void showPOI(Context context, String name, String distance, String address, String phone, TextView nameTxv, TextView distanceFromTxv, TextView distanceTxv, View addressView, View telephoneView, TextView addressTxv, TextView telephoneTxv, int headerResId, int footerResId, int singleResId) {
        showPOI(context, 0, name, distance, address, phone, nameTxv, distanceFromTxv, distanceTxv, addressView, telephoneView, addressTxv, telephoneTxv, headerResId, footerResId, singleResId, null, null);
    }
    
    public static void showPOI(Context context, int order, String name, String distance, String address, String phone, TextView nameTxv, TextView distanceFromTxv, TextView distanceTxv, View addressView, View telephoneView, TextView addressTxv, TextView telephoneTxv, int headerResId, int footerResId, int singleResId, String addressTitle, String telephoneTitle) {
        
        nameTxv.setText(order > 0 ? order+". "+name : name);
        
        POIAdapter.showDistance(context, distanceFromTxv, distanceTxv, distance);
        
        addressTxv.setText(address);
        if (addressTitle != null) {
//            ((TextView) addressView.findViewById(R.id.address_title_txv)).setText(addressTitle);
        }
        if (telephoneTitle != null) {
//            ((TextView) telephoneView.findViewById(R.id.telephone_title_txv)).setText(telephoneTitle);
        }
        
        boolean addressEmpty = TextUtils.isEmpty(address);
        boolean phoneEmpty = TextUtils.isEmpty(phone);
        if (phoneEmpty == false) {
            telephoneTxv.setText(phone.replace("|", context.getString(R.string.dunhao)));
        }
        
        if (addressEmpty == false && phoneEmpty == false) {
            addressView.setBackgroundResource(headerResId);
            telephoneView.setBackgroundResource(footerResId);
            addressView.setVisibility(View.VISIBLE);
            telephoneView.setVisibility(View.VISIBLE);
        } else if (addressEmpty && phoneEmpty) {
            addressView.setVisibility(View.GONE);
            telephoneView.setVisibility(View.GONE);
        } else if (addressEmpty == false) {
            addressView.setBackgroundResource(singleResId);
            addressView.setVisibility(View.VISIBLE);
            telephoneView.setVisibility(View.GONE);
        } else if (phoneEmpty == false) {
            telephoneView.setBackgroundResource(singleResId);
            addressView.setVisibility(View.GONE);
            telephoneView.setVisibility(View.VISIBLE);
        }

    }
    
    public class YingxunAdapter extends ArrayAdapter<Yingxun>{
        private static final int RESOURCE_ID = R.layout.discover_dianying_fendian_list_item;
        
        public YingxunAdapter(Context context, List<Yingxun> list) {
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

            view.findViewById(R.id.tuangou_fendian_list_item).setPadding(0, 0, 0, 0);
            
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            final TextView telephoneTxv = (TextView) view.findViewById(R.id.telephone_txv);
            final Button todayBtn = (Button) view.findViewById(R.id.today_btn);
            final Button tomorrowBtn = (Button) view.findViewById(R.id.tomorrow_btn);
            final Button afterTomorrowBtn = (Button) view.findViewById(R.id.after_tomorrow_btn);
            ImageView showTimeDividerImv = (ImageView) view.findViewById(R.id.show_time_divider_imv);
            ViewGroup showTimeView = (ViewGroup) view.findViewById(R.id.show_time_view);
            ViewGroup changciListView = (ViewGroup) view.findViewById(R.id.changci_list_view);
            View addressView = view.findViewById(R.id.address_view);
            View telephoneView = view.findViewById(R.id.telephone_view);
            View notimeView = view.findViewById(R.id.no_time_view);
            
            layoutShowTimeView(showTimeView, showTimeDividerImv);
            
            final Yingxun yingxun = getItem(position);
            String distance = yingxun.getDistance();
            String address = yingxun.getAddress();
            String phone = yingxun.getPhone();
            showPOI(mSphinx, position+1, yingxun.getName(), distance, address, phone, 
                    nameTxv, distanceFromTxv, distanceTxv, addressView, telephoneView, addressTxv, telephoneTxv, 
                    R.drawable.list_middle, R.drawable.list_middle, R.drawable.list_middle, null, null);
            
            makeChangciView(yingxun, mSphinx, mLayoutInflater, changciListView);
            refreshDayView(yingxun, todayBtn, tomorrowBtn, afterTomorrowBtn, showTimeDividerImv, notimeView);
            
            if(toScrollDownPos != 0){
            	mResultLsv.setSelectionFromTop(toScrollDownPos, 0);
            	toScrollDownPos = 0;
            }
            
            View.OnClickListener onClickListener = new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    int id = view.getId();
                    if (id  == R.id.today_btn) {
                        mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonDianyingToday);
                        if (Changci.OPTION_DAY_TODAY == yingxun.getChangciOption()) {
                            yingxun.setChangciOption(0);
                        } else {
                            yingxun.setChangciOption(Changci.OPTION_DAY_TODAY);
                            nodifyScrollDown(position);
                        }
                    } else if (id  == R.id.tomorrow_btn) {
                        mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonDianyingTomorrow);
                        if (Changci.OPTION_DAY_TOMORROW == yingxun.getChangciOption()) {
                            yingxun.setChangciOption(0);
                        } else {
                            yingxun.setChangciOption(Changci.OPTION_DAY_TOMORROW);
                            nodifyScrollDown(position);
                        }
                    } else if (id  == R.id.after_tomorrow_btn) {
                        mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonDianyingAfterTomorrow);
                        if (Changci.OPTION_DAY_AFTER_TOMORROW == yingxun.getChangciOption()) {
                            yingxun.setChangciOption(0);
                        } else {
                            yingxun.setChangciOption(Changci.OPTION_DAY_AFTER_TOMORROW);
                            nodifyScrollDown(position);
                        }
                    } else if (id == R.id.telephone_view) {
                        mActionLog.addAction(mActionTag +  ActionLog.CommonTelphone);
                        Utility.telephone(mSphinx, telephoneTxv);
                    } else if (id == R.id.address_view) {
                        mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);
                        /* 交通界面的显示 */
                        POI poi = yingxun.getPOI(POI.SOURCE_TYPE_YINGXUN);
                        Utility.queryTraffic(mSphinx, poi, mActionTag);
                    }
                    getAdapter().notifyDataSetChanged();
                }
            };
            todayBtn.setOnClickListener(onClickListener);
            tomorrowBtn.setOnClickListener(onClickListener);
            afterTomorrowBtn.setOnClickListener(onClickListener);
            
            addressView.setOnClickListener(onClickListener);
            telephoneView.setOnClickListener(onClickListener);
            
            return view;
        }
    }
    
    private void nodifyScrollDown(int pos){
    	if(pos >= mResultLsv.getCount() - 2 && mResultLsv.getState(false) == SpringbackListView.DONE){
    		toScrollDownPos = pos;
    	}
    }
    
    public static void layoutShowTimeView(ViewGroup showTimeView, ImageView showTimeDividerImv) {
        showTimeView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int showTimeViewHeight = showTimeView.getMeasuredHeight() - Util.dip2px(Globals.g_metrics.density, 1);
        
        ((RelativeLayout.LayoutParams) (showTimeDividerImv.getLayoutParams())).topMargin = showTimeViewHeight;
    }
    
    public static void refreashDayBtnView(int day, Yingxun yingxun, Button todayBtn, Button tomorrowBtn, Button afterTomorrowBtn) {
        if (day == Changci.OPTION_DAY_TODAY) {
            if (yingxun.getChangciToday() > 0)
                todayBtn.setBackgroundResource(R.drawable.btn_today_focused);
            else
                todayBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
            if (yingxun.getChangciTomorrow() > 0)
                tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_normal);
            else
                tomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
            if (yingxun.getChangciAfterTomorrow() > 0) 
                afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_normal);
            else
                afterTomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
        }
        if (day == Changci.OPTION_DAY_TOMORROW) {
            if (yingxun.getChangciToday() > 0)
                todayBtn.setBackgroundResource(R.drawable.btn_today_normal);
            else
                todayBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
            if (yingxun.getChangciTomorrow() > 0)
                tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_focused);
            else
                tomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
            if (yingxun.getChangciAfterTomorrow() > 0) 
                afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_normal);
            else
                afterTomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);          
        }
        if (day == Changci.OPTION_DAY_AFTER_TOMORROW) {
            if (yingxun.getChangciToday() > 0)
                todayBtn.setBackgroundResource(R.drawable.btn_today_normal);
            else
                todayBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
            if (yingxun.getChangciTomorrow() > 0)
                tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_normal);
            else
                tomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);
            
            if (yingxun.getChangciAfterTomorrow() > 0) 
                afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_focused);
            else
                afterTomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);
        }        
    }
    
    public static void refreshDayView(Yingxun yingxun, Button todayBtn, Button tomorrowBtn, Button afterTomorrowBtn, ImageView showTimeDividerImv, View mNotimeView) {

        int changciOption = yingxun.getChangciOption();
        if (yingxun.getChangciToday() > 0) {
            //todayBtn.setBackgroundResource(R.drawable.btn_today_normal);
            todayBtn.setEnabled(true);
        } else {
            //todayBtn.setBackgroundResource(R.drawable.ic_discover_money);
            todayBtn.setEnabled(true);
        }
        if (yingxun.getChangciTomorrow() > 0) {
            //tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_normal);
            tomorrowBtn.setEnabled(true);
        } else {
            //tomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);;
            tomorrowBtn.setEnabled(true);
        }
        if (yingxun.getChangciAfterTomorrow() > 0) {
            //afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_normal);
            afterTomorrowBtn.setEnabled(true);
        } else {
            //afterTomorrowBtn.setBackgroundResource(R.drawable.ic_discover_money);
            afterTomorrowBtn.setEnabled(true);
        }
        if ((changciOption & Changci.OPTION_DAY_TODAY) > 0 ) {
            todayBtn.setBackgroundResource(R.drawable.btn_today_focused);
            tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_normal);
            afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_normal);
            if (yingxun.getChangciToday() > 0) {
                showTimeDividerImv.setVisibility(View.VISIBLE);
                mNotimeView.setVisibility(View.GONE);
            } else {
                mNotimeView.setVisibility(View.VISIBLE);
                showTimeDividerImv.setVisibility(View.VISIBLE);
            }
        } else if ((changciOption & Changci.OPTION_DAY_TOMORROW) > 0 ) {
            todayBtn.setBackgroundResource(R.drawable.btn_today_normal);
            tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_focused);
            afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_normal);
            if (yingxun.getChangciTomorrow() > 0) {
                showTimeDividerImv.setVisibility(View.VISIBLE);
                mNotimeView.setVisibility(View.GONE);
            } else {
                mNotimeView.setVisibility(View.VISIBLE);
                showTimeDividerImv.setVisibility(View.VISIBLE);
            }
        } else if ((changciOption & Changci.OPTION_DAY_AFTER_TOMORROW) > 0 ) {
            todayBtn.setBackgroundResource(R.drawable.btn_today_normal);
            tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_normal);
            afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_focused);
            if (yingxun.getChangciAfterTomorrow() > 0) {
                showTimeDividerImv.setVisibility(View.VISIBLE);
                mNotimeView.setVisibility(View.GONE);
            } else {
                mNotimeView.setVisibility(View.VISIBLE);
                showTimeDividerImv.setVisibility(View.VISIBLE);
            }
        } else {
            todayBtn.setBackgroundResource(R.drawable.btn_today_normal);
            tomorrowBtn.setBackgroundResource(R.drawable.btn_tomorrow_normal);
            afterTomorrowBtn.setBackgroundResource(R.drawable.btn_after_tomorrow_normal);
            mNotimeView.setVisibility(View.GONE);
            showTimeDividerImv.setVisibility(View.GONE);
        }
    }
    
    private static ViewGroup changciTemplateView = null;
    
    /**
     * Get the maximum Changci View width for the changci info in yingxun
     * @param yingxun
     * @param layoutInflater
     * @param parent
     * @return
     */
    private static int getChangciListItemWidth(Yingxun yingxun, LayoutInflater layoutInflater, ViewGroup parent){

        int RESOURCE_ID = R.layout.discover_changci_list_item;
        
        int option = yingxun.getChangciOption();

        int maxWidth = 0;
        int curWidth = 0;
        
        // Inflate a new view for it.
        if(changciTemplateView==null){
        	changciTemplateView = (ViewGroup)layoutInflater.inflate(RESOURCE_ID, parent, false);
        }
        
        //Get the minimum possible width first
    	((TextView)changciTemplateView.findViewById(R.id.version_txv)).setText("");
    	changciTemplateView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    	maxWidth = changciTemplateView.getMeasuredWidth() + Util.dip2px(Globals.g_metrics.density, 6);

    	//Loop through the changci list to get the maximum possible width 
        List<Changci> list = yingxun.getChangciList();
        int size = (list != null ? list.size() : 0);
        for(int k = 0;k < size; k++) {

        	// We only care about the current changci option for the yingxun
            if ((list.get(k).getOption() & option) > 0) {
            	
	        	//Set the content and get the width
	        	((TextView)changciTemplateView.findViewById(R.id.version_txv)).setText(list.get(k).getVersion());
	        	changciTemplateView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
	        	curWidth = changciTemplateView.getMeasuredWidth() + Util.dip2px(Globals.g_metrics.density, 6);
	            
	        	// Update the max
	        	if(curWidth > maxWidth){
	        		maxWidth = curWidth;
	        	}
            }
        	
        }
        
		return maxWidth;
    	
    }

    private static int lastProcessedChangciIndex = 0;
    
    /**
     * A queue that resues the column views
     * Since fillChangciRow will only be called in UI thread,
     * 	no need to synchronize it.
     */
    public static Queue< SoftReference<ViewGroup> > viewQueueForChangciItems = null;
    
    /**
     * Fill the changci row with the changci info of yingxun, starting at index {@linkplain lastProcessedChangciIndex}
     * @param row
     * @param columNum
     * @param layoutInflater
     * @param changciListItemWidth
     * @param list
     * @param yingxun
     * @return return the newly filled changcin item
     */
    public static int fillChangciRow(ViewGroup row, int columNum, LayoutInflater layoutInflater, int changciListItemWidth,
    		Yingxun yingxun){

    	List<Changci> list = yingxun.getChangciList();
        int RESOURCE_ID = R.layout.discover_changci_list_item;
        int size = (list != null ? list.size() : 0);
        int option = yingxun.getChangciOption();
        int newvisibleChangciCount = 0;
        
        int rowViewChildCount = row.getChildCount();

        /**
         * To solve a problem:
         * 		When we set the changci from "3D" to "IMAX3D", the size of the textView doesn't change.
         * After trying the methods that i can think up, 
         * I found we must first remove the views from the row and then add them back
         * 	to force the ViewGroup to reconsider the size of all childs.
         */
        
        // Allocate the queue when first allocated
        if(viewQueueForChangciItems == null){
        	viewQueueForChangciItems = new LinkedList<SoftReference< ViewGroup> >();
        }
        
        // Get all the views from the row and remove them from the row
        for (int i = 0; i < rowViewChildCount; i++) {
        	viewQueueForChangciItems.add( new SoftReference<ViewGroup>( (ViewGroup) row.getChildAt(i) ) );
		}
        row.removeAllViews();
        
        for(int j = 0; j < columNum; j++) {
            
            Changci changci = null;
            for(int k = lastProcessedChangciIndex;k < size; k++) {
                lastProcessedChangciIndex++;
                if ((list.get(k).getOption() & option) > 0) {
                    changci = list.get(k);
                    break;
                }
            }

            ViewGroup colum = null;
            // get or allocate the View for this column
            if(!viewQueueForChangciItems.isEmpty()){
            	colum = viewQueueForChangciItems.remove().get();
            }
            if(colum == null){
            	colum = (ViewGroup) layoutInflater.inflate(RESOURCE_ID, row, false);
            }
            if (changci != null) {
                // Set changci info into views
                TextView timeTxv = (TextView) colum.findViewById(R.id.time_txv);
                TextView versionTxv = (TextView) colum.findViewById(R.id.version_txv);

                //Set start time
                String stratTime = changci.getStartTime();
                timeTxv.setText(stratTime.length() < 5 ? "  " +stratTime : stratTime);
                if ((changci.getOption() & Changci.OPTION_FILTER) > 0) {
                    timeTxv.setTextColor(0xff199dbe);
                } else {
                    timeTxv.setTextColor(0x99000000);
                }
                
                //Set version
                String version = changci.getVersion();
                if (TextUtils.isEmpty(version) || version.toLowerCase().equals("2d")) {
                    versionTxv.setVisibility(View.INVISIBLE);
                } else {
                    versionTxv.setText(version);
                    versionTxv.setVisibility(View.VISIBLE);
                }
                
                // Make it visible and Add to row
                colum.setVisibility(View.VISIBLE);
                newvisibleChangciCount++;
            }else {
                colum.setVisibility(View.INVISIBLE);
			}
            // We have to add invisible colums to fill the rest space of incomplete rows.
            row.addView(colum, new LayoutParams(changciListItemWidth, LayoutParams.WRAP_CONTENT));
            
        }
        
        return newvisibleChangciCount;
    }
    

    public static Queue<SoftReference<ViewGroup>> viewQueueForChangciRows = null;
    
    public static void makeChangciView(Yingxun yingxun, Context context, LayoutInflater layoutInflater, ViewGroup parent) {
        
        int changciListItemWidth = getChangciListItemWidth(yingxun, layoutInflater, parent);
        
        List<Changci> list = yingxun.getChangciList();
        int size = (list != null ? list.size() : 0);
        int colums = (Globals.g_metrics.widthPixels-Util.dip2px(Globals.g_metrics.density, 24))/changciListItemWidth;
        int rows = size/colums + (size%colums == 0 ? 0 : 1);
        int viewChildCount = parent.getChildCount();
        
        //Allocate the queue
        if( viewQueueForChangciRows == null){
        	viewQueueForChangciRows = new LinkedList<SoftReference< ViewGroup > >();
        }

        // Get all the views from the row and remove them from the row
        for (int i = 0; i < viewChildCount; i++) {
        	viewQueueForChangciRows.add( new SoftReference<ViewGroup>((ViewGroup) parent.getChildAt(i)) );
		}
        parent.removeAllViews();
        
        // Set the last processed changci index to 0
        lastProcessedChangciIndex = 0;
        for(int i = 0; i < rows; i++) {
        	ViewGroup row = null;
        	// Get or allocate a row
        	if (!viewQueueForChangciRows.isEmpty()) {
        		row = viewQueueForChangciRows.remove().get();
			}
        	
            if(row == null){
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                row = linearLayout;
            }
            
            //Set the content of the row
            row.setVisibility(View.VISIBLE);
            if( fillChangciRow(row, colums, layoutInflater, changciListItemWidth, yingxun) != 0){
            	parent.addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }else {
            	viewQueueForChangciRows.add(new SoftReference<ViewGroup>(row));
            	break;
			}
        }
        
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();

        boolean exit = true;
        if (dataQuery.hasParameter(BaseQuery.SERVER_PARAMETER_INDEX)) {
            int index = Integer.parseInt(dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_INDEX));
            if (index > 0) {
                mResultLsv.onRefreshComplete(false);
                mResultLsv.setFooterSpringback(true);
                exit = false;
            }
        }
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.hasAbnormalResponseCode(dataQuery, mSphinx, BaseActivity.SHOW_DIALOG, this, exit)) {
            if (dataQuery.isTurnPage() && dataQuery.getResponse() == null) {
                mResultLsv.setFooterLoadFailed(true);
            }
            return;
        }
        mResultLsv.onRefreshComplete(false);
        mResultLsv.setFooterSpringback(false);

        setDataQuery(dataQuery, false);
    }
    
    private void setDataQuery(DataQuery dataQuery, boolean setup) {
        Response response = dataQuery.getResponse();
        mDataQuery = dataQuery;

        if (dataQuery.isTurnPage() == false) {
            getList().clear();
            mSphinx.getHandler().post(new Runnable() {
                
                @Override
                public void run() {
                    mResultLsv.setSelectionFromTop(0, 0);
                }
            });
        }
        
        if (response instanceof FendianResponse) {
            FendianResponse fendianResponse = (FendianResponse)response;
            mBaseList = fendianResponse.getList();
            
            if (fendianResponse.getList() != null 
                    && fendianResponse.getList().getList() != null 
                    && fendianResponse.getList().getList().size() > 0) {
                List<Fendian> list = fendianResponse.getList().getList();
                mFendianList.addAll(list);
                
                DataQuery fendianQuery = mTuangou.getFendianQuery();
                if (fendianQuery == null && dataQuery.isTurnPage() == false) {
                    mTuangou.setFendianQuery(dataQuery);
                } else if (dataQuery.isTurnPage() && setup == false){
                    fendianResponse = (FendianResponse) fendianQuery.getResponse();
                    fendianResponse.getList().getList().addAll(list);
                }
            }
        } else if (response instanceof YingxunResponse) {
            YingxunResponse yingxunResponse = (YingxunResponse)response;
            mBaseList = yingxunResponse.getList();
            
            if (yingxunResponse.getList() != null 
                    && yingxunResponse.getList().getList() != null 
                    && yingxunResponse.getList().getList().size() > 0) {
                List<Yingxun> list = yingxunResponse.getList().getList();
                mYingxunList.addAll(list);
                for(Yingxun yingxun : list) {
                    yingxun.setChangciOption(0);
                }
                
                DataQuery yingxunQuery = mDianying.getYingxunQuery();
                if (yingxunQuery == null && dataQuery.isTurnPage() == false) {
                    mDianying.setYingxunQuery(dataQuery);
                } else if (dataQuery.isTurnPage() && setup == false){
                    yingxunResponse = (YingxunResponse) yingxunQuery.getResponse();
                    yingxunResponse.getList().getList().addAll(list);
                }
            }
        }
        
        getAdapter().notifyDataSetChanged();
        if (mBaseList != null && getList().size() < mBaseList.getTotal()) {
            mResultLsv.setFooterSpringback(true);
        }
        
        if (getList().isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }

        refreshRightBtn();
        refreshResultTxv(dataQuery);
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
    @SuppressWarnings("rawtypes")
    private List getList() {
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            return mFendianList;
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            return mYingxunList;
        }
        return null;
    }
    
    @SuppressWarnings("rawtypes")
    private ArrayAdapter getAdapter() {
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            return mFendianAdapter;
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            return mYingxunAdapter;
        }
        return null;
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
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
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.SpringbackListView;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;

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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
        
        DataQuery dataQuery = new DataQuery(mContext);
        int cityId = Globals.g_Current_City_Info.getId();
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        
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
            if (fendianQuery != null && mSphinx.getDiscoverListFragment().getCriteria().get(DataQuery.SERVER_PARAMETER_FILTER).equals(fendianQuery.getCriteria().get(DataQuery.SERVER_PARAMETER_FILTER))) {
                setDataQuery(fendianQuery, true);
                return;
            }
            
            mTuangou.setFendianQuery(null);
            criteria.put(DataQuery.SERVER_PARAMETER_TUANGOU_UUID, mTuangou.getUid());
            
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
            if (yingxunQuery != null && mSphinx.getDiscoverListFragment().getCriteria().get(DataQuery.SERVER_PARAMETER_FILTER).equals(yingxunQuery.getCriteria().get(DataQuery.SERVER_PARAMETER_FILTER))) {
                setDataQuery(yingxunQuery, true);
                return;
            }
            
            mDianying.setYingxunQuery(null);
            criteria.put(DataQuery.SERVER_PARAMETER_DIANYING_UUID, mDianying.getUid());
        }
        
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(0));
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, mDataType);
        BaseQuery.passLocationToCriteria(mSphinx.getDiscoverListFragment().getCriteria(), criteria);
        dataQuery.setup(criteria, cityId, getId(), getId(), null, false, true, null);
        
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
        
        mRootView = mLayoutInflater.inflate(R.layout.tuangou_fendian_list, container, false);

        findViews();
        setListener();
        
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
        mBaseList = null;
    }
    
    protected void findViews() {
        mResultTxv = (TextView) mRootView.findViewById(R.id.result_txv);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
    }

    protected void setListener() {
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
            str = mContext.getString(R.string.no_result);
        }
        
        mResultTxv.setText(str);
        mEmptyTxv.setText(str);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            mTitleTxv.setText(R.string.fendian_list);
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            mTitleTxv.setText(R.string.dianyingyuan_list);
        }
        mRightBtn.setImageResource(R.drawable.ic_view_map);
        mRightTxv.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightTxv.setOnClickListener(this);
        
        if (isReLogin()) {
            return;
        }
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
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
        mActionLog.addAction(ActionLog.SearchResultNextPage);

        DataQuery dataQuery = new DataQuery(mContext);
        int cityId = lastDataQuery.getCityId();
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(getList().size()));
        dataQuery.setup(criteria, cityId, getId(), getId(), null, true, true, dataQuery.getPOI());
        mSphinx.queryStart(dataQuery);
        }
    }

    public void viewMap(List<POI> list, int index) {
        if (list.isEmpty()) {
            return;
        }

        mSphinx.showPOI(list, index);
        boolean yingxun = BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType);
        mSphinx.getResultMapFragment().setData(mContext.getString(yingxun ? R.string.dianying_ditu : R.string.shanghu_ditu), yingxun ? ActionLog.MapDianyingBranchList : ActionLog.MapTuangouBranchList);
        mSphinx.showView(R.id.view_result_map);   
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_txv:
                if (getList().isEmpty()) {
                    return;
                }
                mActionLog.addAction(ActionLog.Title_Right_Button, mActionTag);
                if (mResultLsv.getLastVisiblePosition() >= getList().size()-1) {
                    viewMap(getPOIList(getList().size()-1), mResultLsv.getFirstVisiblePosition() % TKConfig.getPageSize()); 
                } else {
                    viewMap(getPOIList(mResultLsv.getFirstVisiblePosition()), mResultLsv.getFirstVisiblePosition() % TKConfig.getPageSize());
                }
                break;
                
            default:
                break;
        }
    }
    
    public class FendianAdapter extends ArrayAdapter<Fendian>{
        private static final int RESOURCE_ID = R.layout.tuangou_fendian_list_item;
        
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
            Button distanceBtn = (Button) view.findViewById(R.id.distance_btn);
            View addressView = view.findViewById(R.id.address_view);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            TextView telephoneTxv = (TextView) view.findViewById(R.id.telephone_txv);
            View telephoneView = view.findViewById(R.id.telephone_view);
            View dividerView = view.findViewById(R.id.divider_imv);

            final Fendian fendian = getItem(position);
            String distance = fendian.getDistance();
            String address = fendian.getAddress();
            String phone = fendian.getPlacePhone();
            showPOI(mSphinx, position+1, fendian.getPlaceName(), distance, address, phone, 
                    nameTxv, distanceBtn, addressView, dividerView, telephoneView, addressTxv, telephoneTxv, 
                    R.drawable.list_header, R.drawable.list_footer, R.drawable.list_single, null, null);
            
            View.OnClickListener onClickListener = new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    int id = view.getId();
                    if (id == R.id.distance_btn) {
                        mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailDistance);
                        POI poi = fendian.getPOI(POI.SOURCE_TYPE_FENDIAN, null);
                        poi.setOrderNumber(position+1);
                        mSphinx.getTrafficQueryFragment().setData(poi);
                        mSphinx.showView(R.id.view_traffic_query);
                    } else if (id == R.id.address_view) {
                        mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailAddress);
                        POI poi = fendian.getPOI(POI.SOURCE_TYPE_FENDIAN, null);
                        poi.setOrderNumber(position+1);
                        List<POI> list = new ArrayList<POI>();
                        list.add(poi);
                        viewMap(list, 0); 
                    } else if (id == R.id.telephone_txv) {
                        mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailTelphone);
                    }
                }
            };
            
            distanceBtn.setOnClickListener(onClickListener);
            addressView.setOnClickListener(onClickListener);
            telephoneTxv.setOnClickListener(onClickListener);
            
            return view;
        }
    }
    
    public static void showPOI(Context context, String name, String distance, String address, String phone, TextView nameTxv, TextView distanceTxv, View addressView, View dividerView, View telephoneView, TextView addressTxv, TextView telephoneTxv, int headerResId, int footerResId, int singleResId) {
        showPOI(context, 0, name, distance, address, phone, nameTxv, distanceTxv, addressView, dividerView, telephoneView, addressTxv, telephoneTxv, headerResId, footerResId, singleResId, null, null);
    }
    
    public static void showPOI(Context context, int order, String name, String distance, String address, String phone, TextView nameTxv, TextView distanceTxv, View addressView, View dividerView, View telephoneView, TextView addressTxv, TextView telephoneTxv, int headerResId, int footerResId, int singleResId, String addressTitle, String telephoneTitle) {
        
        nameTxv.setText(order > 0 ? order+". "+name : name);
        
        if (TextUtils.isEmpty(distance)) {
            distanceTxv.setText(R.string.come_here);
        } else {
            distanceTxv.setText(distance);
        }
        
        addressTxv.setText(address);
        if (addressTitle != null) {
            ((TextView) addressView.findViewById(R.id.address_title_txv)).setText(addressTitle);
        }
        if (telephoneTitle != null) {
            ((TextView) telephoneView.findViewById(R.id.telephone_title_txv)).setText(telephoneTitle);
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
            dividerView.setVisibility(View.VISIBLE);
            addressView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
            telephoneView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
        } else if (addressEmpty && phoneEmpty) {
            addressView.setVisibility(View.GONE);
            telephoneView.setVisibility(View.GONE);
            dividerView.setVisibility(View.GONE);
        } else if (addressEmpty == false) {
            addressView.setBackgroundResource(singleResId);
            addressView.setVisibility(View.VISIBLE);
            telephoneView.setVisibility(View.GONE);
            dividerView.setVisibility(View.GONE);
            addressView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
        } else if (phoneEmpty == false) {
            telephoneView.setBackgroundResource(singleResId);
            addressView.setVisibility(View.GONE);
            telephoneView.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.GONE);
            telephoneView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
        }
    }
    
    public class YingxunAdapter extends ArrayAdapter<Yingxun>{
        private static final int RESOURCE_ID = R.layout.dianying_fendian_list_item;
        
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
            
            TextView nameTxv = (TextView) view.findViewById(R.id.fendian_name_txv);
            Button distanceBtn = (Button) view.findViewById(R.id.distance_btn);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            TextView telephoneTxv = (TextView) view.findViewById(R.id.telephone_txv);
            final Button todayBtn = (Button) view.findViewById(R.id.today_btn);
            final Button tomorrowBtn = (Button) view.findViewById(R.id.tomorrow_btn);
            final Button afterTomorrowBtn = (Button) view.findViewById(R.id.after_tomorrow_btn);
            ImageView showTimeDividerImv = (ImageView) view.findViewById(R.id.show_time_divider_imv);
            ViewGroup showTimeView = (ViewGroup) view.findViewById(R.id.show_time_view);
            ViewGroup changciListView = (ViewGroup) view.findViewById(R.id.changci_list_view);
            View addressView = view.findViewById(R.id.address_view);
            View telephoneView = view.findViewById(R.id.telephone_view);
            View notimeView = view.findViewById(R.id.no_time_view);
            View dividerView = view.findViewById(R.id.divider_imv);
            
            layoutShowTimeView(showTimeView, showTimeDividerImv);
            
            final Yingxun yingxun = getItem(position);
            String distance = yingxun.getDistance();
            String address = yingxun.getAddress();
            String phone = yingxun.getPhone();
            showPOI(mSphinx, position+1, yingxun.getName(), distance, address, phone, 
                    nameTxv, distanceBtn, addressView, dividerView, telephoneView, addressTxv, telephoneTxv, 
                    R.drawable.list_header, R.drawable.list_middle, R.drawable.list_header, null, null);
            
            makeChangciView(yingxun, mSphinx, mLayoutInflater, changciListView);
            refreshDayView(yingxun, todayBtn, tomorrowBtn, afterTomorrowBtn, showTimeDividerImv, notimeView);
            
            View.OnClickListener onClickListener = new OnClickListener() {
                
                @Override
                public void onClick(View view) {
                    int id = view.getId();
                    if (id  == R.id.today_btn) {
                        mActionLog.addAction(mActionTag+ActionLog.DianyingToday);
                        if (Changci.OPTION_DAY_TODAY == yingxun.getChangciOption()) {
                            yingxun.setChangciOption(0);
                        } else {
                            yingxun.setChangciOption(Changci.OPTION_DAY_TODAY);
                        }
                    } else if (id  == R.id.tomorrow_btn) {
                        mActionLog.addAction(mActionTag+ActionLog.DianyingTomorrow);
                        if (Changci.OPTION_DAY_TOMORROW == yingxun.getChangciOption()) {
                            yingxun.setChangciOption(0);
                        } else {
                            yingxun.setChangciOption(Changci.OPTION_DAY_TOMORROW);
                        }
                    } else if (id  == R.id.after_tomorrow_btn) {
                        mActionLog.addAction(mActionTag+ActionLog.DianyingAfterTomorrow);
                        if (Changci.OPTION_DAY_AFTER_TOMORROW == yingxun.getChangciOption()) {
                            yingxun.setChangciOption(0);
                        } else {
                            yingxun.setChangciOption(Changci.OPTION_DAY_AFTER_TOMORROW);
                        }
                    } else if (id  == R.id.distance_btn) {
                        mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailDistance);
                        /* 交通界面的显示 */
                        POI poi = yingxun.getPOI(POI.SOURCE_TYPE_YINGXUN, null);
                        poi.setOrderNumber(position+1);
                        mSphinx.getTrafficQueryFragment().setData(poi);
                        mSphinx.showView(R.id.view_traffic_query);
                    } else if (id == R.id.telephone_txv) {
                        mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailTelphone);
                    } else if (id == R.id.address_view) {
                        mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailAddress);
                		POI poi = yingxun.getPOI(POI.SOURCE_TYPE_YINGXUN, null);
                        poi.setOrderNumber(position+1);
                        List<POI> list = new ArrayList<POI>();
                        list.add(poi);
                        viewMap(list, 0);
                    }
                    getAdapter().notifyDataSetChanged();
                }
            };
            todayBtn.setOnClickListener(onClickListener);
            tomorrowBtn.setOnClickListener(onClickListener);
            afterTomorrowBtn.setOnClickListener(onClickListener);
            
            distanceBtn.setOnClickListener(onClickListener);
            
            addressView.setOnClickListener(onClickListener);
            telephoneTxv.setOnClickListener(onClickListener);
            
            return view;
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
    
    public static void makeChangciView(Yingxun yingxun, Context context, LayoutInflater layoutInflater, ViewGroup parent) {
        int RESOURCE_ID = R.layout.changci_list_item;
        
        int option = yingxun.getChangciOption();
        View view = layoutInflater.inflate(RESOURCE_ID, parent, false);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int changciListItemWidth = view.getMeasuredWidth() + Util.dip2px(Globals.g_metrics.density, 6);
        
        List<Changci> list = yingxun.getChangciList();
        int size = list.size();
        int colums = (Globals.g_metrics.widthPixels-Util.dip2px(Globals.g_metrics.density, 12))/changciListItemWidth;
        int rows = size/colums + (size%colums == 0 ? 0 : 1);
        int viewChildCount = parent.getChildCount();
        int changciIndex = 0;
        int visibleChangciCount = 0;
        for(int i = 0; i < rows; i++) {
            ViewGroup row;
            if (i < viewChildCount) {
                row = (ViewGroup) parent.getChildAt(i);
                row.setVisibility(View.VISIBLE);
            } else {
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                row = linearLayout;
                parent.addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            }
            int rowViewChildCount = row.getChildCount();
            for(int j = 0; j < colums; j++) {
                ViewGroup colum;
                if (j < rowViewChildCount) {
                    colum = (ViewGroup) row.getChildAt(j);
                } else {
                    colum = (ViewGroup) layoutInflater.inflate(RESOURCE_ID, row, false);
                    row.addView(colum, new LayoutParams(changciListItemWidth, LayoutParams.WRAP_CONTENT));
                }
                Changci changci = null;
                for(int k = changciIndex;k < size; k++) {
                    changciIndex++;
                    if ((list.get(k).getOption() & option) > 0) {
                        changci = list.get(k);
                        break;
                    }
                }
                if (changci != null) {
                    TextView timeTxv = (TextView) colum.findViewById(R.id.time_txv);
                    TextView versionTxv = (TextView) colum.findViewById(R.id.version_txv);

                    String stratTime = changci.getStartTime();
                    timeTxv.setText(stratTime.length() < 5 ? "  " +stratTime : stratTime);
                    if ((changci.getOption() & Changci.OPTION_FILTER) > 0) {
                        timeTxv.setTextColor(0xff199dbe);
                    } else {
                        timeTxv.setTextColor(0x99000000);
                    }
                    String version = changci.getVersion();
                    if (TextUtils.isEmpty(version) || version.toLowerCase().equals("2d")) {
                        versionTxv.setVisibility(View.INVISIBLE);
                    } else {
                        versionTxv.setText(version);
                        versionTxv.setVisibility(View.VISIBLE);
                    }
                    colum.setVisibility(View.VISIBLE);
                    visibleChangciCount++;
                } else {
                    colum.setVisibility(View.INVISIBLE);
                }
            }
        }
        
        viewChildCount = parent.getChildCount();
        int visibleRows = visibleChangciCount/colums + (visibleChangciCount%colums == 0 ? 0 : 1);
        for(int i = visibleRows; i < viewChildCount; i++) {
            parent.getChildAt(i).setVisibility(View.GONE);
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
        if (dataQuery.getCriteria().containsKey(BaseQuery.SERVER_PARAMETER_INDEX)) {
            int index = Integer.parseInt(dataQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_INDEX));
            if (index > 0) {
                mResultLsv.onRefreshComplete(false);
                mResultLsv.setFooterSpringback(true);
                exit = false;
            }
        }
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mSphinx, null, true, this, exit)) {
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
        refreshResultTxv(dataQuery);
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
    private List<POI> getPOIList(int index) {
        int minIndex = index - (index % (TKConfig.getPageSize()));
        int maxIndex = minIndex + (TKConfig.getPageSize());
        List<POI> poiList = new ArrayList<POI>();
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex < maxIndex && minIndex < mFendianList.size(); minIndex++) {
                Fendian fendian = mFendianList.get(minIndex);
                fendian.setOrderNumber(minIndex+1);
                poiList.add(fendian.getPOI(POI.SOURCE_TYPE_FENDIAN, null));
            }
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex < maxIndex && minIndex < mYingxunList.size(); minIndex++) {
                Yingxun yingxun = mYingxunList.get(minIndex); 
                yingxun.setOrderNumber(minIndex+1);
                poiList.add(yingxun.getPOI(POI.SOURCE_TYPE_YINGXUN, null));
            }
        }
        return poiList;
    }
    
    private List getList() {
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            return mFendianList;
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            return mYingxunList;
        }
        return null;
    }
    
    private ArrayAdapter getAdapter() {
        if (BaseQuery.DATA_TYPE_FENDIAN.equals(mDataType)) {
            return mFendianAdapter;
        } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(mDataType)) {
            return mYingxunAdapter;
        }
        return null;
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.HotelOrder;
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
import com.tigerknows.ui.BaseFragment;
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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Peng Wenyue
 */
public class HotelOrderListFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "DiscoverChildListFragment";

    public HotelOrderListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private TextView mResultTxv;

    private SpringbackListView mResultLsv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;

    private List<HotelOrder> hotelOrders = new ArrayList<HotelOrder>();
    
    private DataQuery mDataQuery;

	private HotelOrderAdapter hotelOrderAdapter;
    
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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_list, container, false);

        findViews();
        setListener();
        List<HotelOrder> orders = new ArrayList<HotelOrder>();
        orders.add(new HotelOrder());
        orders.add(new HotelOrder());
        hotelOrderAdapter = new HotelOrderAdapter(mContext, orders);
        mResultLsv.setAdapter(hotelOrderAdapter);
        return mRootView;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void dismiss() {
        super.dismiss();
        if (hotelOrders != null) {
            hotelOrders.clear();
            if (hotelOrderAdapter != null) {
                hotelOrderAdapter.notifyDataSetChanged();
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
    
    @Override
    public void onResume() {
        super.onResume();
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(mContext.getString(R.string.hotel_order));
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
    private void turnPage(){
        synchronized (this) {
        	
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_btn:
                
            default:
                break;
        }
    }

    public class HotelOrderAdapter extends ArrayAdapter<HotelOrder>{
        private static final int RESOURCE_ID = R.layout.hotel_order_list_item;
        
        public HotelOrderAdapter(Context context, List<HotelOrder> list) {
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

            HotelOrder hotelOrder = new HotelOrder();
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView roomTypeTxv = (TextView) view.findViewById(R.id.room_type_txv);
            TextView checkinDateTxv = (TextView) view.findViewById(R.id.checkin_date_txv);
            TextView checkoutDateTxv = (TextView) view.findViewById(R.id.checkout_date_txv);
            TextView dayCountView = (TextView) view.findViewById(R.id.day_count_txv);

            return view;
        }
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
    }
}

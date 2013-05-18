/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.ui.BaseFragment;

/**
 * @author Peng Wenyue
 */
public class HotelOrderDetailFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "DiscoverChildListFragment";

    public HotelOrderDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private HotelOrder mHotelOrder;

    private int state;
    
    private static final int STATE_LOADING = 1;
    private static final int STATE_LIST = 2;
    private static final int STATE_EMPTY = 3;
    private static final int STATE_LOADING_MORE = 4;

    private TextView mHotelNameTxv;
    private TextView mDistanceTxv;
    private TextView mHotelAddressTxv;
    private TextView mHotelTelTxv;
    private TextView mOrderIdTxv;
    private TextView mOrderStateTxv;
    private TextView mOrderTimeTxv;
    private TextView mTotalFeeTxv;
    private TextView mPayTypeTxv;
    private TextView mCheckinDateTxv;
    private TextView mCheckoutDateTxv;
    private TextView mRetentionDateTxv;
    private TextView mRoomTypeTxv;
    private TextView mCheckinPersonTxv;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_detail, container, false);

        findViews();
        setListener();
        mDistanceTxv.setVisibility(View.INVISIBLE);
        return mRootView;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void dismiss() {
        super.dismiss();
    }
    
    protected void findViews() {

		mHotelNameTxv = (TextView) mRootView.findViewById(R.id.name_txv);
		mDistanceTxv = (TextView) mRootView.findViewById(R.id.distance_txv);
		mHotelAddressTxv = (TextView) mRootView.findViewById(R.id.address_txv);
		mHotelTelTxv = (TextView) mRootView.findViewById(R.id.telephone_txv);
		mOrderIdTxv = (TextView) mRootView.findViewById(R.id.order_id_txv);
		mOrderStateTxv = (TextView) mRootView.findViewById(R.id.order_state_txv);
		mOrderTimeTxv = (TextView) mRootView.findViewById(R.id.order_time_txv);
		mTotalFeeTxv = (TextView) mRootView.findViewById(R.id.order_total_fee_txv);
		mPayTypeTxv = (TextView) mRootView.findViewById(R.id.pay_type_txv);
		mCheckinDateTxv = (TextView) mRootView.findViewById(R.id.checkin_date_txv);
		mCheckoutDateTxv = (TextView) mRootView.findViewById(R.id.checkout_date_txv);
		mRetentionDateTxv = (TextView) mRootView.findViewById(R.id.retention_date_txv);
		mRoomTypeTxv = (TextView) mRootView.findViewById(R.id.room_type_txv);
		mCheckinPersonTxv = (TextView) mRootView.findViewById(R.id.checkin_person_txv);

    }

    protected void setListener() {
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(mContext.getString(R.string.hotel_order));
        
    }
    

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_btn:
                
            default:
                break;
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
    
    public void setData(HotelOrder order){
    	mHotelOrder = order;
    	if(mHotelOrder==null){
    		return;
    	}
    	
    	mHotelNameTxv.setText(order.getHotelName());
    	mHotelAddressTxv.setText(order.getHotelAddress());
    	mHotelTelTxv.setText(order.getHotelTel());
    	mOrderIdTxv.setText(order.getId());
    	mOrderStateTxv.setText(getOrderStateDesc(order.getState()));
    	mOrderTimeTxv.setText(formatOrderTime(order.getCreateTime()));
    	mTotalFeeTxv.setText( "" + order.getTotalFee());
    	mPayTypeTxv.setText(mContext.getString(R.string.hotel_order_default_pay_type));
    	mCheckinDateTxv.setText(formatOrderTime(order.getCheckinTime()));
    	mCheckoutDateTxv.setText(formatOrderTime(order.getCheckoutTime()));
    	mRetentionDateTxv.setText(formatOrderTime(order.getRetentionTime()));
    	mRoomTypeTxv.setText(order.getRoomType());
    	mCheckinPersonTxv.setText(order.getGuestName());

    	
    	
    }
    
    private static int[] orderStateDescResId = new int[]{
    	R.string.order_state_processing,
    	R.string.order_state_success,
    	R.string.order_state_canceled,
    	R.string.order_state_post_due,
    	R.string.order_state_checked_in
    };
    
    public String getOrderStateDesc(int state){
		return mContext.getString(orderStateDescResId[state]);
    }
    
    public static String formatOrderTime(long millis){
    	Date date = new Date(millis);
    	SimpleDateFormat dateformat=new SimpleDateFormat("yyyy-MM-dd");
		return dateformat.format(date);
    }
    
}

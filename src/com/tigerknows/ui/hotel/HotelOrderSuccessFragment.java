package com.tigerknows.ui.hotel;

import java.util.Date;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.CalendarUtil;

public class HotelOrderSuccessFragment extends BaseFragment{

	public HotelOrderSuccessFragment(Sphinx sphinx) {
		super(sphinx);
		// TODO Auto-generated constructor stub
	}
	
	private HotelOrder mOrder;
	private TextView mHotelNameTxv;
	private TextView mHotelRoomLatestTxv;
	private TextView mHotelRoomtypeTxv;
	private RelativeLayout mOrderDetailRly;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_success, container, false);

        findViews();
        setListener();
        mActionTag = ActionLog.HotelOrderSuccess;
    	
        return mRootView;
    }

    @Override
    protected void findViews() {
        super.findViews();
		mHotelNameTxv = (TextView) mRootView.findViewById(R.id.hotel_name_txv);
		mHotelRoomtypeTxv = (TextView) mRootView.findViewById(R.id.hotel_roomtype_txv);
		c = (TextView) mRootView.findViewById(R.id.room_latest_txv);
		mOrderDetailRly = (RelativeLayout) mRootView.findViewById(R.id.order_detail_rly);

    }

    @Override
    protected void setListener() {
    	super.setListener();
    	mOrderDetailRly.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addActionLog(ActionLog.HotelOrderSuccessDetail);
	            mSphinx.getHotelOrderDetailFragment().setData(mOrder, -1);
	            mSphinx.getHotelOrderDetailFragment().setStageIndicatorVisible(true);
	            dismiss();
	            mSphinx.uiStackRemove(getId());
	            mSphinx.showView(R.id.view_hotel_order_detail);
			}
		});
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	mTitleBtn.setText(mSphinx.getString(R.string.order_status));
    	mHotelNameTxv.setText(mOrder.getHotelName());
		mHotelRoomLatestTxv.setText(mSphinx.getString(
				R.string.hotel_room_latest, 
				CalendarUtil.ymd8c_Hm4.format(new Date(mOrder.getRetentionTime()))
				));
    	mHotelRoomtypeTxv.setText(mOrder.getRoomType());
    }
    
    public void setData(HotelOrder order){
    	mOrder = order;
    }
    
}

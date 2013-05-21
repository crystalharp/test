package com.tigerknows.ui.hotel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.Hotel;
import com.tigerknows.ui.BaseFragment;

public class HotelIntroFragment extends BaseFragment {

    TextView longDescriptionTxv;
    TextView roomDescriptionTxv;
    TextView hotelServiceTxv;
    View longDescriptionBlock;
    View roomDescriptionBlock;
    View hotelServiceBlock;
    
    Hotel mHotel;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = mLayoutInflater.inflate(R.layout.hotel_intro, container, false);
		longDescriptionTxv = (TextView) mRootView.findViewById(R.id.hotel_long_description);
		roomDescriptionTxv = (TextView) mRootView.findViewById(R.id.hotel_room_description);
		hotelServiceTxv = (TextView) mRootView.findViewById(R.id.hotel_service);
		longDescriptionBlock = mRootView.findViewById(R.id.hotel_long_desc_block);
		roomDescriptionBlock = mRootView.findViewById(R.id.hotel_room_desc_block);
		hotelServiceBlock = mRootView.findViewById(R.id.hotel_service_block);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		mTitleBtn.setText("hotel introduction");
		String longDescription = mHotel.getLongDescription();
		String roomDescription = mHotel.getRoomDescription();
		String hotelService = mHotel.getService();
		if (longDescription == null){
		    longDescriptionBlock.setVisibility(View.GONE);
		} else {
		    longDescriptionTxv.setText(mHotel.getLongDescription());
		    longDescriptionBlock.setVisibility(View.VISIBLE);
		}
		if (roomDescription == null) {
		    roomDescriptionBlock.setVisibility(View.GONE);
		} else {
		    roomDescriptionBlock.setVisibility(View.VISIBLE);
		    roomDescriptionTxv.setText(mHotel.getRoomDescription());
		}
		if (hotelService == null) {
		    hotelServiceBlock.setVisibility(View.GONE);
		} else {
		    hotelServiceBlock.setVisibility(View.VISIBLE);
		    hotelServiceTxv.setText(mHotel.getService());
		}
	}

	public HotelIntroFragment(Sphinx sphinx) {
		super(sphinx);
	}
	
	public void setData(Hotel hotel) {
	    mHotel = hotel;
	}

}

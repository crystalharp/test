package com.tigerknows.ui.hotel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.ui.BaseFragment;

public class HotelImageGridFragment extends BaseFragment {

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mTitleBtn.setText("Hotel Images");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mRootView = mLayoutInflater.inflate(R.layout.hotel_image_grid, null);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	public HotelImageGridFragment(Sphinx sphinx) {
		super(sphinx);
		// TODO Auto-generated constructor stub
	}

}

package com.tigerknows.ui.hotel;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.ui.BaseFragment;

public class HotelImageGridFragment extends BaseFragment {

	List<HotelTKDrawable> mImageList;
	GridView mHotelGrid;
	
	private final int[] mCategoryResIdList = {R.drawable.category_eat,
	        R.drawable.category_play,
	        R.drawable.category_buy,
	        R.drawable.category_hotel,
	        R.drawable.category_tour,
	        R.drawable.category_beauty,
	        R.drawable.category_sport,
	        R.drawable.category_bank,
	        R.drawable.category_traffic,
	        R.drawable.category_hospital
	        };
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mTitleBtn.setText("Hotel Images");
		mHotelGrid = (GridView) mRootView.findViewById(R.id.hotel_grid);
		mHotelGrid.setAdapter(new GridAdapter());
		mHotelGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Toast.makeText(getContext(), "clicked", Toast.LENGTH_SHORT).show();
				
			}
		});
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

	public void setData(List<HotelTKDrawable> imageList) {
		mImageList = imageList;
	}
	
	private class GridAdapter extends BaseAdapter {

		@Override
		public int getCount() {
//			return mImageList.size();
			return mCategoryResIdList.length;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imv = (ImageView)convertView;
			if (convertView == null) {  
                imv = new ImageView(mSphinx);  
            } else {  
                imv = (ImageView) convertView;  
            }
//			Drawable image = mImageList.get(position).getTKDrawable().loadDrawable(mSphinx, null, this.toString());
//			imv.setBackgroundDrawable(image);
			imv.setBackgroundResource(mCategoryResIdList[position]);
			return imv;
		}
		
	}
}

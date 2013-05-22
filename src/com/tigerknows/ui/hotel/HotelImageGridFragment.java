package com.tigerknows.ui.hotel;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.discover.TKGallery;

public class HotelImageGridFragment extends BaseFragment {
    
    /**
     * 网格状态
     */
    static final int STATE_GRID = 0;
    
    /**
     * 全屏状态
     */
    static final int STATE_GALLERY = 1;
    
    int mState =  STATE_GRID;

	List<HotelTKDrawable> mImageList = new ArrayList<HotelTKDrawable>();
	GridView mGridView;
	GridAdapter mGridAdapter;
    TKGallery mTKGallery;
    GalleryAdapter mGalleryAdapter;
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
            mSphinx.getHandler().post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            if (mSphinx.uiStackPeek() == getId() && mSphinx.isFinishing() == false) {
                if (mState == STATE_GRID) {
                    mGridAdapter.notifyDataSetChanged();
                } else if (mState == STATE_GALLERY) {
                    mGalleryAdapter.notifyDataSetChanged();
                }
            }
        }
    };
	
	@Override
	public void onResume() {
		super.onResume();
        mTitleBtn.setText("酒店图片（"+ mImageList.size()+"）张");
		setState(mState);
	}
	
	void setState(int state) {
	    this.mState = state;
	    if (this.mState == STATE_GRID) {
	        if (mTitleFragment != null) {
                mTitleFragment.display();
	        }
            mGridView.setVisibility(View.VISIBLE);
            mTKGallery.setVisibility(View.GONE);
            mGridAdapter.notifyDataSetChanged();
        } else if (this.mState == STATE_GALLERY) {
            if (mTitleFragment != null) {
                mTitleFragment.hide();
            }
            mGridView.setVisibility(View.GONE);
            mTKGallery.setVisibility(View.VISIBLE);
            mGalleryAdapter.notifyDataSetChanged();
        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = mLayoutInflater.inflate(R.layout.hotel_image_grid, container, false);
		
		findViews();
		
        mGridAdapter = new GridAdapter(mSphinx, mImageList);
        mGridView.setAdapter(mGridAdapter);
        
        mGalleryAdapter = new GalleryAdapter(mSphinx, mImageList);
        mTKGallery.setAdapter(mGalleryAdapter);
        
		setListener();
		
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	void findViews() {
	    mGridView = (GridView)mRootView.findViewById(R.id.hotel_grid);
        mTKGallery = (TKGallery)mRootView.findViewById(R.id.gallery_view);
	}
	
	void setListener() {
	    mGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                setState(STATE_GALLERY);
                mTKGallery.setSelection(position);
            }
        });
	}

	public HotelImageGridFragment(Sphinx sphinx) {
		super(sphinx);
	}

	public void setData(List<HotelTKDrawable> imageList) {
	    mImageList.clear();
	    if (imageList != null) {
		    mImageList.addAll(imageList);
	    }
	    setState(STATE_GRID);
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	        if (mState == STATE_GALLERY) {
	            setState(STATE_GRID);
	            return true;
	        }
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	private class GridAdapter extends ArrayAdapter<HotelTKDrawable> {
	    
	    static final int WIDTH = 70;

        public GridAdapter(Context context, List<HotelTKDrawable> list) {
            super(context, 0, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                ViewGroup viewGroup = new LinearLayout(getContext());
                ImageView iconImv = new ImageView(getContext());
                iconImv.setId(R.id.icon_imv);
                iconImv.setScaleType(ImageView.ScaleType.FIT_XY);
                iconImv.setBackgroundResource(R.drawable.icon);
                viewGroup.addView(iconImv);
                view = viewGroup;
            } else {
                view = convertView;
            }
            
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            
            HotelTKDrawable hotelTKDrawable = getItem(position);
            Drawable image = hotelTKDrawable.getTKDrawable().loadDrawable(mSphinx, mLoadedDrawableRun, HotelImageGridFragment.this.toString());
            int newWidth = (int) (Globals.g_metrics.density*WIDTH);
            ViewGroup.LayoutParams layoutParams = iconImv.getLayoutParams();
            layoutParams.width = newWidth;
            if (image != null) {
                float scale = ((float) newWidth) / image.getIntrinsicWidth();
                layoutParams.height = (int) (scale*image.getIntrinsicHeight());
                iconImv.setImageDrawable(image);
            } else {
                iconImv.setImageDrawable(null);
            }
            return view;
        }
        
	}
    
    private class GalleryAdapter extends ArrayAdapter<HotelTKDrawable> {
        
        static final int RES_ID = R.layout.hotel_image_gallery_item;

        public GalleryAdapter(Context context, List<HotelTKDrawable> list) {
            super(context, RES_ID, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RES_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
            
            HotelTKDrawable hotelTKDrawable = getItem(position);
            
            Drawable image = hotelTKDrawable.getTKDrawable().loadDrawable(mSphinx, mLoadedDrawableRun, HotelImageGridFragment.this.toString());
            if (image != null) {
                float scale = ((float) Globals.g_metrics.widthPixels) / image.getIntrinsicWidth();
                ViewGroup.LayoutParams layoutParams = iconImv.getLayoutParams();
                layoutParams.width = Globals.g_metrics.widthPixels;
                layoutParams.height = (int) (scale*image.getIntrinsicHeight());
                iconImv.setBackgroundDrawable(image);
            } else {
                iconImv.setBackgroundDrawable(null);
            }
            textTxv.setText(hotelTKDrawable.getName()+" "+(position+1)+"/"+(getCount()));
            return view;
        }
        
    }
}

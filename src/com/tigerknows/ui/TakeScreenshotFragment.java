/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * @author Peng Wenyue
 */
public class TakeScreenshotFragment extends BaseFragment implements View.OnClickListener {
    
    private int mVisibilityLocation;
    private int mVisibilityPreviousNext;
    private int mVisibilityZoom;
    private int mVisibilityMore;
    
    public TakeScreenshotFragment(Sphinx sphinx) {
        super(sphinx);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TakeScreenshot;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.take_screenshot, container, false);;
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.take_screenshot);
        
        mRight2Btn.setText(R.string.share);
        mRight2Btn.setVisibility(View.VISIBLE);
        mRight2Btn.setOnClickListener(this);
        mRightBtn.setText(R.string.save);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        
        mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 18), 0, 0);
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + Util.dip2px(Globals.g_metrics.density, 18);

        mVisibilityPreviousNext = mSphinx.getPreviousNextView().getVisibility();
        mVisibilityLocation = mSphinx.getLocationView().getVisibility();
        mVisibilityZoom = mSphinx.getZoomView().getVisibility();
        mVisibilityMore = mSphinx.getMoreBtn().getVisibility();
        mSphinx.getPreviousNextView().setVisibility(View.GONE);
        mSphinx.getLocationView().setVisibility(View.GONE);
        mSphinx.getZoomView().setVisibility(View.GONE);
        mSphinx.getMoreBtn().setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + mSphinx.getCityViewHeight() + Util.dip2px(Globals.g_metrics.density, 18);
        mSphinx.getPreviousNextView().setVisibility(mVisibilityPreviousNext);
        mSphinx.getLocationView().setVisibility(mVisibilityLocation);
        mSphinx.getZoomView().setVisibility(mVisibilityZoom);
        mSphinx.getMoreBtn().setVisibility(mVisibilityMore);
    }
    
    protected void findViews() {
    }
    
    protected void setListener() {
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
        case R.id.right_btn:
        	mActionLog.addAction(mActionTag + ActionLog.TakeScreenshotShare);
        	mSphinx.snapMapView(new SnapMap() {
        	    
        	    @Override
        	    public void finish(Uri uri) {
        	        Toast.makeText(mSphinx, uri.toString(), Toast.LENGTH_LONG).show();
        	    }
        	}, mSphinx.getMapView().getCenterPosition(), null);
        	break;

        case R.id.right2_btn:
            mSphinx.snapMapView(new SnapMap() {
                
                @Override
                public void finish(Uri uri) {
                    if(uri != null) {
                        Utility.share(mSphinx, mSphinx.getString(R.string.share), null, uri);
                    }
                }
            }, mSphinx.getMapView().getCenterPosition(), null);
            break;
            
    	default:
    		break;
        }

    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mSphinx.setTouchMode(TouchMode.NORMAL);
    }
    
    public static class TitlePopupArrayAdapter extends ArrayAdapter<String> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.result_map_title_popup_list_item;
        
        private LayoutInflater mLayoutInflater;
        
        public String mSelectedItem;

        public TitlePopupArrayAdapter(Context context, List<String> list) {
            super(context, TEXTVIEW_RESOURCE_ID, list);
            mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView iconTxv = (ImageView)view.findViewById(R.id.icon_imv);
            TextView textTxv = (TextView)view.findViewById(R.id.name_txv);
            
            String name = getItem(position);
            if (name.equals(mSelectedItem)) {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                iconTxv.setVisibility(View.VISIBLE);
                textTxv.setTextColor(TKConfig.COLOR_ORANGE);
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                iconTxv.setVisibility(View.INVISIBLE);
                textTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
            }            
            textTxv.setText(name);

            return view;
        }
    }
}

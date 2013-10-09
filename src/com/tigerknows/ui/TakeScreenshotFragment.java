/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.util.Utility;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
        mRight2Btn.setBackgroundResource(R.drawable.btn_title);
        mRightBtn.setText(R.string.save);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        mRightBtn.setBackgroundResource(R.drawable.btn_title);
        
        mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 18), 0, 0);
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + Util.dip2px(Globals.g_metrics.density, 18);
        
        mSphinx.getPreviousNextView().setVisibility(View.INVISIBLE);
        mSphinx.getLocationView().setVisibility(View.INVISIBLE);
        mSphinx.getZoomView().setVisibility(View.INVISIBLE);
        mSphinx.getMoreBtn().setVisibility(View.INVISIBLE);
    }

    @Override
    public void dismiss() {
        super.dismiss();
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
    
    public void setData() {
        mVisibilityPreviousNext = mSphinx.getPreviousNextView().getVisibility();
        mVisibilityLocation = mSphinx.getLocationView().getVisibility();
        mVisibilityZoom = mSphinx.getZoomView().getVisibility();
        mVisibilityMore = mSphinx.getMoreBtn().getVisibility();
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
        	        if (uri != null) {
        	            Toast.makeText(mSphinx, uri.toString(), Toast.LENGTH_LONG).show();
        	        }
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
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.MapView.SnapMap;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.POI;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * @author Peng Wenyue
 */
public class ResultMapFragment extends BaseFragment implements View.OnClickListener {

    private String mTitle;
    private View mSnapView;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    
    public ResultMapFragment(Sphinx sphinx) {
        super(sphinx);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.MapPOI;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.result_map, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(mTitle);
        mRightBtn.setVisibility(View.INVISIBLE);
        mRootView.setOnTouchListener(null);
        mTitleBtn.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        if (mSphinx.mSnapMap) {
            mSnapView.setVisibility(View.VISIBLE);
            if (mSphinx.mIntoSnap == 0) {
                mLeftBtn.setText(R.string.home);
            }
            mSphinx.mIntoSnap++;
        } else {
            mSnapView.setVisibility(View.GONE);
        }
        mSphinx.getDownloadView().setPadding(0, Util.dip2px(Globals.g_metrics.density, 10), 0, 0);
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + Util.dip2px(Globals.g_metrics.density, 10);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSphinx.getDownloadView().setPadding(0, Util.dip2px(Globals.g_metrics.density, 70), 0, 0);
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + mSphinx.getCityViewHeight() + Util.dip2px(Globals.g_metrics.density, 10);
    }
    
    public void setData(String title, String actionTag) {
        mTitle = title;
        mActionTag = actionTag;
        
        /*
         * setData方法被调用时, 表明将要进入结果地图, 
         * 此时请重置定位点状态
         */
        mSphinx.resetLoactionButtonState();
    }
    
    protected void findViews() {
        mSnapView = mRootView.findViewById(R.id.snap_view);
        mCancelBtn = (Button) mRootView.findViewById(R.id.cancel_btn);
        mConfirmBtn = (Button) mRootView.findViewById(R.id.confirm_btn);
    }
    
    protected void setListener() {
        mCancelBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.confirm_btn) {
            mSphinx.snapMapView(new SnapMap() {
                
                @Override
                public void finish(Uri uri) {
                    if(uri != null) {
                        Intent intent = new Intent();
                        intent.setData(uri);  // uri 是 地图(包括标注层) 的一个snapshot图片
                        ItemizedOverlay itemizedOverlay = mSphinx.getMapView().getOverlaysByName(ItemizedOverlay.POI_OVERLAY);
                        POI poi = null;
                        if (itemizedOverlay != null) {
                            OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
                            if (overlayItem != null && overlayItem.getAssociatedObject() instanceof POI) {
                                poi = (POI) (overlayItem.getAssociatedObject());
                            }
                        }
            
                        Position position = null;
                        String description = null;
                        if (poi != null) {
                            position = poi.getPosition();
                            description = poi.getName().replace('(', ' ').replace(')', ' ');
                            String address = poi.getAddress();
                            if (!TextUtils.isEmpty(address)) {
                                description += "("+address.replace('(', ' ').replace(')', ' ')+")";
                            }
                        } else {
                            position = mSphinx.getMapView().getCenterPosition();
                            description = mSphinx.getMapEngine().getPositionName(position);
                            if (TextUtils.isEmpty(description)) {
                                description = mContext.getString(R.string.select_point);
                            }
                            description = description.replace('(', ' ').replace(')', ' ');
                        }
                        intent.putExtra("location", "<a href='http://maps.tigerknows.com/?latlon="+
                                position.getLat()+","+
                                position.getLon()+"&z="+
                                mSphinx.getMapView().getZoomLevel()+"&n="+
                                description.replace('\'', ' ')+"'>" +
                                description + "</a>"); // address 是选定位置(当前定位的位置或者search后选择的某个地址)的详细地址描述 加上 该地址的 URL link，最终要插入MMS的内容中
                        LogWrapper.d("Sphinx", "intent.getStringExtra('location')="+intent.getStringExtra("location"));
                        mSphinx.setResult(Activity.RESULT_OK, intent);
                    }
                }
            }, mSphinx.getMapView().getCenterPosition());
        }
        mSphinx.finish();
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mSphinx.clearMap();
    }
}

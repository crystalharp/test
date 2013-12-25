/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.common;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseFragment;

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
    private ItemizedOverlay mItemizedOverlay;
    
    public ResultMapFragment(Sphinx sphinx) {
        super(sphinx);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIListMap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.result_map, container, false);
        
        mBottomFrament = mSphinx.getInfoWindowFragment();
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSphinx.getMapView().setStopRefreshMyLocation(false);
        mTitleBtn.setText(mTitle);
        setOnTouchListener(null);
        mRightBtn.setVisibility(View.GONE);
        
        int fromThirdPartye = mSphinx.getFromThirdParty();
        if (fromThirdPartye == Sphinx.THIRD_PARTY_SONY_MY_LOCATION ||
                fromThirdPartye == Sphinx.THIRD_PARTY_SONY_QUERY_POI) {
            mSnapView.setVisibility(View.VISIBLE);
        } else {
            mSnapView.setVisibility(View.GONE);
        }
        
        //如果是驾车和步行，需要在这里可以切换到详情页
        if (mActionTag == ActionLog.TrafficDriveMap || mActionTag == ActionLog.TrafficWalkMap) {
            mRightBtn.setBackgroundResource(R.drawable.btn_view_detail);
            mRightBtn.setVisibility(View.VISIBLE);
            mRightBtn.setOnClickListener(this);
        	if (mActionTag == ActionLog.TrafficDriveMap) {
        	    mTitleBtn.setText(getString(R.string.title_type_drive));
        	} else {
        	    mTitleBtn.setText(getString(R.string.title_type_walk));
        	}
        }
        mSphinx.showHint(TKConfig.PREFS_HINT_LOCATION, R.layout.hint_location_map);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    public void setData(String title, String actionTag) {
        mTitle = title;
        mItemizedOverlay = mSphinx.getMapView().getCurrentOverlay();
        mActionTag = actionTag;
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
        int id = view.getId();
        switch (id) {

        case R.id.confirm_btn:
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
	        					description = getString(R.string.select_point);
	        				}
	        				description = description.replace('(', ' ').replace(')', ' ');
	        			}
	        			intent.putExtra("location", "<a href='http://maps.tigerknows.com/?latlon="+
	        					position.getLat()+","+
	        					position.getLon()+"&z="+
	        					mSphinx.getMapView().getZoomLevel()+"&n="+
	        					description.replace('\'', ' ')+"'>" +
	        					description + "</a>"); // address 是选定位置(当前定位的位置或者search后选择的某个地址)的详细地址描述 加上 该地址的 URL link，最终要插入MMS的内容中
	        			mSphinx.setResult(Activity.RESULT_OK, intent);
	        			mSphinx.finish();
	        		}
	        	}
	        }, mSphinx.getMapView().getCenterPosition(), mSphinx.getMapView().getCurrentMapScene());
	        break;
        case R.id.right_btn:
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
        	mSphinx.showView(R.id.view_traffic_result_detail);
        	break;
    	default:
    		break;
        }

    }
    
    public ItemizedOverlay getItemizedOverlay() {
        return mItemizedOverlay;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mSphinx.clearMap();
    }
}

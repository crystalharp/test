/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.common;

import com.decarta.android.map.Icon;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.Polyline;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.Shape;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.InfoWindowHelper;
import com.tigerknows.map.MapView;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class MeasureDistanceFragment extends BaseFragment implements View.OnClickListener {

    private MapView mMapView;
    private int mVisibilityLocation;
    private int mVisibilityCleanMap;
    private TouchMode mTouchMode;
    private ViewGroup mInfoWindowView;
    private TextView mInfoWindowTxv;
    private Button mRevocationBtn;
    private LayoutParams mRevocationBtnLayoutParams;
    
    private ViewGroup getInfoWindowView() {
        if (mInfoWindowView == null) {
            mInfoWindowView = (ViewGroup) mSphinx.getLayoutInflater().inflate(R.layout.info_window, null);
            mInfoWindowTxv = (TextView)mInfoWindowView.findViewById(R.id.name_txv);
        }
        
        return mInfoWindowView;
    }
    
    /**
     * 添加一个点
     * @param position
     * @param newLine 是否需要创建新线路
     * @return
     */
    public boolean addPoint(Position position) {
        
        boolean result = false;
        
        String overlayName = ItemizedOverlay.MEASURE_DISTANCE_OVERLAY;
        String shapeName = Shape.MEASURE_DISTANCE;
        
        ItemizedOverlay overlay= mMapView.getOverlaysByName(overlayName);
        
        try {

            Resources resources = mContext.getResources();
            Icon icon = Icon.getIcon(resources, R.drawable.icon_map_drive, Icon.OFFSET_LOCATION_CENTER);
            RotationTilt rt=new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
            OverlayItem overlayItem=new OverlayItem(position, icon, icon, null,rt);
            
            overlay= mMapView.getOverlaysByName(overlayName);
            Shape shape = mMapView.getShapesByName(shapeName);
            List<Position> positions = null;
            if (overlay == null) {
                overlay = new ItemizedOverlay(overlayName);
                mMapView.addOverlay(overlay);
                
                positions = new ArrayList<Position>();
                positions.add(position);
                Polyline polyline = new Polyline(positions, shapeName);
                mMapView.addShape(polyline);
            } else {
                Polyline polyline = (Polyline) shape;
                positions = polyline.getPositions();
                positions.add(position);
//                polyline.setPositions(positions);
                // TODO:需要重新生成一个shape?
                mMapView.deleteShapeByName(shapeName);
                polyline = new Polyline(positions, shapeName);
                mMapView.addShape(polyline);
            }
            
            overlay.addOverlayItem(overlayItem);
            
            int length = 0;
            Position prev = null;
            Position cur = null;
            for(int i = 0, size = positions.size(); i < size; i++) {
                cur = positions.get(i);
                if (i == 0) {
                    prev = cur;
                    continue;
                }
                length += Position.distanceBetween(prev, cur);
                prev = cur;
            }
            ViewGroup viewGroup = getInfoWindowView();
            mInfoWindowTxv.setText(Utility.formatMeterString(length));
            InfoWindowHelper.showInfoWindow(mMapView, overlayItem, viewGroup);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * 移除最近生成的线路中最近添加的点，当最近生成的线路只剩下一个点时则删除此线路
     * @return
     */
    public boolean removeLastPoint() {
        
        boolean result = false;
        
        String overlayName = ItemizedOverlay.MEASURE_DISTANCE_OVERLAY;
        String shapeName = Shape.MEASURE_DISTANCE;
        
        try {

            ItemizedOverlay overlay= mMapView.getOverlaysByName(overlayName);
            if (overlay == null) {
                return result;
            }
            
            overlay= mMapView.getOverlaysByName(overlayName);
            overlay.remove(overlay.size()-1);
            if (overlay.size() == 0) {
                mMapView.deleteOverlaysByName(overlayName);
                mMapView.deleteShapeByName(shapeName);
                InfoWindowHelper.hideInfoWindow(mMapView);
                return result;
            }
            
            Shape shape = mMapView.getShapesByName(shapeName);
            Polyline polyline = (Polyline) shape;
            List<Position> positions = polyline.getPositions();
            positions.remove(positions.size()-1);

//            polyline.setPositions(positions);
            // TODO:需要重新生成一个shape?
            mMapView.deleteShapeByName(shapeName);
            polyline = new Polyline(positions, shapeName);
            mMapView.addShape(polyline);
            
            int length = 0;
            Position prev = null;
            Position cur = null;
            for(int i = 0, size = positions.size(); i < size; i++) {
                cur = positions.get(i);
                if (i == 0) {
                    prev = cur;
                    continue;
                }
                length += Position.distanceBetween(prev, cur);
                prev = cur;
            }
            
            OverlayItem overlayItem = overlay.get(overlay.size()-1);
            ViewGroup viewGroup = getInfoWindowView();
            mInfoWindowTxv.setText(Utility.formatMeterString(length));
            InfoWindowHelper.showInfoWindow(mMapView, overlayItem, viewGroup);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 清空测距生成的overylay和shape
     */
    public void clean() {
        mMapView.deleteOverlaysByName(ItemizedOverlay.MEASURE_DISTANCE_OVERLAY);
        mMapView.deleteShapeByName(Shape.MEASURE_DISTANCE );
        InfoWindowHelper.hideInfoWindow(mMapView);
    }
    
    public MeasureDistanceFragment(Sphinx sphinx) {
        super(sphinx);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.MeasureDistance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.home, container, false);
        
        mMapView = mSphinx.getMapView();
        
        mRevocationBtn = new Button(mSphinx);
        mRevocationBtn.setText(R.string.revocation);
        mRevocationBtn.setBackgroundResource(R.drawable.btn_confirm);
        mRevocationBtn.setOnClickListener(this);
        mRevocationBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mRevocationBtnLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mRevocationBtnLayoutParams.rightMargin = Utility.dip2px(mSphinx, 12);
        
        findViews();
        setListener();
        
        return mRootView;
    }
    
    public void setData() {
        Sphinx.TouchMode touchMode = mSphinx.getTouchMode();
        if (touchMode != Sphinx.TouchMode.MEASURE_DISTANCE) {
            mVisibilityCleanMap = mSphinx.getMapCleanBtn().getVisibility();
            mVisibilityLocation = mSphinx.getLocationView().getVisibility();
            InfoWindowHelper.hideInfoWindow(mMapView);
            mTouchMode = touchMode;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.setStopRefreshMyLocation(false);
        mTitleBtn.setText(R.string.measure);
        setOnTouchListener(null);
        
        mRevocationBtn.setTextColor(mTitleFragment.mRightBtnConfirmColor);
        ViewGroup mRightView = (ViewGroup) mRightBtn.getParent();
        mRightView.addView(mRevocationBtn, mRevocationBtnLayoutParams);
        
        mRightBtn.setText(R.string.clear);
        mRightBtn.setOnClickListener(this);
        
        mSphinx.getMapCleanBtn().setVisibility(View.INVISIBLE);
        mSphinx.getLocationView().setVisibility(View.INVISIBLE);
        
        mSphinx.setTouchMode(TouchMode.MEASURE_DISTANCE);
    }


    @Override
    public void onPause() {
        super.onPause();
        ViewGroup mRightView = (ViewGroup) mRightBtn.getParent();
        mRightView.removeView(mRevocationBtn);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
        case R.id.right_btn:
            mActionLog.addAction(mActionTag + ActionLog.MeasureDistanceClear);
            clean();
        	break;

        default:
            mActionLog.addAction(mActionTag + ActionLog.MeasureDistanceRevocation);
            removeLastPoint();
            break;
            
        }

    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        clean();
        mSphinx.setTouchMode(mTouchMode);
        
        mSphinx.getMapCleanBtn().setVisibility(mVisibilityCleanMap);
        mSphinx.getLocationView().setVisibility(mVisibilityLocation);
    }
}

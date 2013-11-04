/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.common;

import com.decarta.Globals;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.Polyline;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.Shape;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class MeasureDistanceFragment extends BaseFragment implements View.OnClickListener {

    private MapView mMapView;
    private int mIndex;
    private int mVisibilityLocation;
    private int mVisibilityPreviousNext;
    private OverlayItem mOtherOverlayItem;
    private OverlayItem mLastOverlayItem;
    private TouchMode mTouchMode;
    
    public int getIndex() {
        return mIndex;
    }
    
    public void setIndex(int index) {
        mIndex = index;
    }
    
    /**
     * 添加一个点
     * @param position
     * @param newLine 是否需要创建新线路
     * @return
     */
    public boolean addPoint(Position position, boolean newLine) {
        
        boolean result = false;
        
        String overlayName = ItemizedOverlay.MEASURE_DISTANCE_OVERLAY + mIndex;
        String shapeName = Shape.MEASURE_DISTANCE + mIndex;
        
        ItemizedOverlay overlay= mMapView.getOverlaysByName(overlayName);
        if (newLine && overlay != null && overlay.size() > 1) {
            mIndex++;
            overlayName = ItemizedOverlay.MEASURE_DISTANCE_OVERLAY + mIndex;
            shapeName = Shape.MEASURE_DISTANCE + mIndex;
        }
        
        try {

            Resources resources = mContext.getResources();
            Icon icon = Icon.getIcon(resources, R.drawable.icon_map_drive, Icon.OFFSET_LOCATION_CENTER);
            RotationTilt rt=new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
            OverlayItem overlayItem=new OverlayItem(position, icon, null,rt);
            overlayItem.setAssociatedObject(TouchMode.MEASURE_DISTANCE);
            
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
            overlayItem.setMessage(Utility.formatMeterString(length));
            mLastOverlayItem = overlayItem;
            mSphinx.showInfoWindow(overlayItem);
            
            mMapView.refreshMap();
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
        
        String overlayName = ItemizedOverlay.MEASURE_DISTANCE_OVERLAY + mIndex;
        String shapeName = Shape.MEASURE_DISTANCE + mIndex;
        
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
                mMapView.getInfoWindow().setVisible(false);
                mMapView.refreshMap();
                mLastOverlayItem = null;
                mMapView.refreshMap();
                if (mIndex > 0) {
                    mIndex--;
                }
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
            overlayItem.setMessage(Utility.formatMeterString(length));
            mLastOverlayItem = overlayItem;
            mSphinx.showInfoWindow(overlayItem);
            
            mMapView.refreshMap();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 清空测距生成的overylay和shape
     */
    public void clearLine() {
        for(; mIndex >= 0; mIndex--) {
            mMapView.deleteOverlaysByName(ItemizedOverlay.MEASURE_DISTANCE_OVERLAY + mIndex);
            mMapView.deleteShapeByName(Shape.MEASURE_DISTANCE + mIndex);
        }
        mIndex = 0;
        mLastOverlayItem = null;
        mMapView.getInfoWindow().setVisible(false);
        mMapView.refreshMap();
    }
    
    public MeasureDistanceFragment(Sphinx sphinx) {
        super(sphinx);
        mMapView = sphinx.getMapView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.MeasureDistance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.result_map, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }
    
    public void setData() {
        Sphinx.TouchMode touchMode = mSphinx.getTouchMode();
        if (touchMode != Sphinx.TouchMode.MEASURE_DISTANCE) {
            mVisibilityPreviousNext = mSphinx.getPreviousNextView().getVisibility();
            mVisibilityLocation = mSphinx.getLocationView().getVisibility();
            MapView mapView = mSphinx.getMapView();
            InfoWindow infoWindow = mapView.getInfoWindow();
            if (infoWindow.isVisible()) {
                mOtherOverlayItem = infoWindow.getAssociatedOverlayItem();
                infoWindow.setVisible(false);
                mapView.refreshMap();
            } else {
                mOtherOverlayItem = null;
            }
            mLastOverlayItem = null;
            mTouchMode = touchMode;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSphinx.getMapView().setStopRefreshMyLocation(false);
        mTitleBtn.setText(R.string.measure);
        mRootView.setOnTouchListener(null);
        
        mRight2Btn.setText(R.string.revocation);
        mRight2Btn.setVisibility(View.VISIBLE);
        mRight2Btn.setOnClickListener(this);
        mRight2Btn.setBackgroundResource(R.drawable.btn_title);
        mRightBtn.setText(R.string.clear);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        mRightBtn.setBackgroundResource(R.drawable.btn_title);
        
        mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 18), 0, 0);

        mSphinx.getPreviousNextView().setVisibility(View.INVISIBLE);
        mSphinx.getLocationView().setVisibility(View.INVISIBLE);
        
        mSphinx.setTouchMode(TouchMode.MEASURE_DISTANCE);
        
        if (mLastOverlayItem != null) {
            mSphinx.showInfoWindow(mLastOverlayItem);
        } else  {
            mMapView.getInfoWindow().setVisible(false);
            mMapView.refreshMap();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
            mActionLog.addAction(mActionTag + ActionLog.MeasureDistanceClear);
            clearLine();
        	break;

        case R.id.right2_btn:
            mActionLog.addAction(mActionTag + ActionLog.MeasureDistanceRevocation);
            removeLastPoint();
            break;
            
    	default:
    		break;
        }

    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        clearLine();
        mSphinx.setTouchMode(mTouchMode);
        
        mSphinx.getPreviousNextView().setVisibility(mVisibilityPreviousNext);
        mSphinx.getLocationView().setVisibility(mVisibilityLocation);
        mSphinx.showInfoWindow(mOtherOverlayItem);
    }
}

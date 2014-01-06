package com.tigerknows.map;

import com.decarta.Globals;
import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.exception.APIException;
import com.decarta.android.map.Icon;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.location.Position;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ItemizedOverlayHelper {

    public static void drawClickSelectPointOverlay(Sphinx sphinx, String title) {
        
        try {
            sphinx.clearMap();
            
            MapView mapView = sphinx.getMapView();
            Position position = mapView.getCenterPosition();
            POI poi = new POI();
            poi.setPosition(position);
            String name = sphinx.getMapEngine().getPositionName(position);
            if (name == null) {
                name = sphinx.getString(R.string.map_point);
            }
            poi.setName(name);
            poi.setSourceType(POI.SOURCE_TYPE_CLICKED_SELECT_POINT);
            
            OverlayItem overlayItem = new OverlayItem(poi.getPosition(), null, null, title, null);
            overlayItem.setAssociatedObject(poi);
            
            ItemizedOverlay itemizedOverlay = new ItemizedOverlay(ItemizedOverlay.CLICKED_OVERLAY);
            itemizedOverlay.addOverlayItem(overlayItem);
            
            sphinx.showInfoWindow(overlayItem);
            
            sphinx.getCenterTokenView().setVisibility(View.VISIBLE);
            sphinx.getMapCleanBtn().setVisibility(View.INVISIBLE);
            sphinx.getMapToolsView().setVisibility(View.INVISIBLE);
            sphinx.getLocationView().setVisibility(View.INVISIBLE);
            
            sphinx.setTouchMode(TouchMode.CLICK_SELECT_POINT);
            
        } catch(APIException e) {
            e.printStackTrace();
        }
        
    }
    
    public static ItemizedOverlay drawPOIOverlay(String overlayName, final Sphinx sphinx, POI poi) {
        
        ItemizedOverlay itemizedOverlay = null;
        final MapView mapView = sphinx.getMapView();
        
        try {
            
            ItemizedOverlay overlay = mapView.getOverlaysByName(overlayName);
            OverlayItem overlayItem;
            
            if (overlay == null) {
                overlay = new ItemizedOverlay(overlayName);
                RotationTilt rt = new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
                overlayItem = new OverlayItem(poi.getPosition(), Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM), 
                        Icon.getIcon(sphinx.getResources(), R.drawable.btn_bubble_b_focused, Icon.OFFSET_LOCATION_CENTER_BOTTOM),
                        poi.getName(), rt);
                
                overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
                    @Override
                    public void onTouchEvent(EventSource eventSource) {
                        if (sphinx.getTouchMode().equals(TouchMode.CLICK_SELECT_POINT)) {
                            return;
                        }
                        if (sphinx.getTouchMode().equals(TouchMode.MEASURE_DISTANCE)) {
                            return;
                        }
                        OverlayItem overlayItem=(OverlayItem) eventSource;
                        sphinx.showInfoWindow(sphinx.uiStackPeek(), overlayItem);
                    }
                });
                overlay.addOverlayItem(overlayItem);
                mapView.addOverlay(overlay);
            } else {
                overlayItem = overlay.get(0);
                overlayItem.setPosition(poi.getPosition());
            }
            
            overlayItem.isFoucsed = true;
            overlayItem.setMessage(poi.getName());
            overlayItem.setAssociatedObject(poi);
            mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
            
            sphinx.showInfoWindow(sphinx.uiStackPeek(), overlayItem);
            
            itemizedOverlay = overlay;
        } catch(APIException e) {
            e.printStackTrace();
        }
        
        return itemizedOverlay;
    }
    
    /**
     * 将地图中心移至当前OverlayItem, 并显示InfoWindow
     */
    public static void centerShowCurrentOverlayFocusedItem(Sphinx sphinx) {
        
        MapView mapView = sphinx.getMapView();
        ItemizedOverlay itemizedOverlay = mapView.getCurrentOverlay();
        
        if (itemizedOverlay != null) {
            final OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
            if (overlayItem != null) {
//            if (overlayItem.hasPreferZoomLevel() && (overlayItem.getPreferZoomLevel() > mMapView.getZoomLevel())) {
            if (itemizedOverlay.isShowInPreferZoom) {
                if (overlayItem.hasPreferZoomLevel() && (overlayItem.getPreferZoomLevel() > mapView.getZoomLevel())) {
                    mapView.setZoomLevel(overlayItem.getPreferZoomLevel());
                    mapView.panToPosition(overlayItem.getPosition());
                } else {
                    mapView.panToPosition(overlayItem.getPosition());
                }
                itemizedOverlay.isShowInPreferZoom = false;
            } else {
                mapView.panToPosition(overlayItem.getPosition());
            }
            sphinx.showInfoWindow(sphinx.uiStackPeek(), overlayItem);
            }
        }
        
        // TODO: 这边写死的判断不大好, 抽象出来...
        if (itemizedOverlay.getName().equals(ItemizedOverlay.TRAFFIC_OVERLAY)) {
            TrafficOverlayHelper.highlightNextTwoOverlayItem(mapView);
        }
    }
    
    public static ItemizedOverlay drawPOIOverlay(final Sphinx sphinx, List dataList, int index) {
        return drawPOIOverlay(sphinx, dataList, index, null, true);
    }
    
    public static ItemizedOverlay drawPOIOverlay(final Sphinx sphinx, List dataList, int index, POI nearbyPOI) {
        return drawPOIOverlay(sphinx, dataList, index, nearbyPOI, true);
    }
    
    public static ItemizedOverlay drawPOIOverlay(final Sphinx sphinx, List dataList, int index, POI nearbyPOI, boolean showInfoWindow) {
        
        ItemizedOverlay itemizedOverlay = null;
        MapView mapView = sphinx.getMapView();
        
        try {
            sphinx.clearMap();
            
            Resources resources = sphinx.getResources();
            if (nearbyPOI != null) {
                RotationTilt rt=new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
                Icon icon = Icon.getIcon(resources, R.drawable.ic_bubble_nearby, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
                OverlayItem overlayItem = new OverlayItem(nearbyPOI.getPosition(), icon, icon, null, rt);
                overlayItem.setAssociatedObject(nearbyPOI);
                
                ItemizedOverlay nearbyPOIOverlay = new ItemizedOverlay(ItemizedOverlay.POI_NEARBY_OVERLAY);
                nearbyPOIOverlay.addOverlayItem(overlayItem);
                
                mapView.addOverlay(nearbyPOIOverlay);
            } else {
                mapView.deleteOverlaysByName(ItemizedOverlay.POI_NEARBY_OVERLAY);
            }
            
            //add pins
            mapView.deleteOverlaysByName(ItemizedOverlay.POI_OVERLAY);
            if (dataList == null || dataList.isEmpty()) {
                mapView.refreshMap();
                return itemizedOverlay;
            }
            final ItemizedOverlay overlay=new ItemizedOverlay(ItemizedOverlay.POI_OVERLAY);
            
            Icon icon = Icon.getIcon(resources, R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
            Icon iconFocused = Icon.getIcon(resources, R.drawable.btn_bubble_b_focused, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
            
            ArrayList<Position> positions = new ArrayList<Position>();
            Position srcreenPosition = null;
            OverlayItem focus = null;
            for(int i=0;i<dataList.size();i++){
                RotationTilt rt=new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
                Object data = dataList.get(i);
                OverlayItem overlayItem = null;
                if (data instanceof POI) {
                    POI target = (POI) data;
                    positions.add(target.getPosition());
                    overlayItem=new OverlayItem(target.getPosition(), icon, iconFocused, target.getName(),rt);
                    overlayItem.setAssociatedObject(target);
                    overlayItem.setPreferZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                    if (index == i) {
                        overlayItem.isFoucsed = true;
                        focus = overlayItem;
                        srcreenPosition = target.getPosition();
                    } else {
                        overlayItem.isFoucsed = false;
                    }
                } else if (data instanceof Dianying || data instanceof Zhanlan || data instanceof Yanchu) {
                    POI poi;
                    if (data instanceof Zhanlan) {
                        Zhanlan target = (Zhanlan) data;
                        poi = target.getPOI();
                    } else if (data instanceof Yanchu) {
                        Yanchu target = (Yanchu) data;
                        poi = target.getPOI();
                    } else {
                        Dianying target = (Dianying) data;
                        poi = target.getPOI();
                    }
                    positions.add(poi.getPosition());
                    overlayItem = new OverlayItem(poi.getPosition(), icon, iconFocused, poi.getName(), rt);
                    overlayItem.setAssociatedObject(data);
                    overlayItem.setPreferZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                    if (index == i) {
                        overlayItem.isFoucsed = true;
                        focus = overlayItem;
                        srcreenPosition = poi.getPosition();
                    } else {
                        overlayItem.isFoucsed = false;
                    }
                } else if (data instanceof Tuangou) {
                    Tuangou target = (Tuangou) data;
                    POI poi = target.getPOI();
                    positions.add(poi.getPosition());
                    overlayItem = new OverlayItem(poi.getPosition(), icon, iconFocused, target.getShortDesc(), rt);
                    overlayItem.setAssociatedObject(target);
                    overlayItem.setPreferZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                    if (index == i) {
                        overlayItem.isFoucsed = true;
                        focus = overlayItem;
                        srcreenPosition = poi.getPosition();
                    } else {
                        overlayItem.isFoucsed = false;
                    }
                }
                overlay.addOverlayItem(overlayItem);
                
                overlayItem.addEventListener(EventType.TOUCH,
                        new OverlayItem.TouchEventListener() {
                            @Override
                            public void onTouchEvent(EventSource eventSource) {
                                if (sphinx.getTouchMode().equals(TouchMode.MEASURE_DISTANCE)) {
                                    return;
                                }
                                
                                OverlayItem overlayItem = (OverlayItem) eventSource;
                                overlayItem.getOwnerOverlay().focuseOverlayItem(overlayItem);
                                sphinx.showInfoWindow(overlayItem);
                            }
                        });
            }
            mapView.addOverlay(overlay);
            if (focus != null && showInfoWindow) {
                sphinx.showInfoWindow(focus);
            }
            if (dataList.size() > 1) {
                overlay.isShowInPreferZoom = true;
                int screenX = Globals.g_metrics.widthPixels;
                int screenY = Globals.g_metrics.heightPixels;
                Rect rect = mapView.getPadding();
                int fitZoomLevle = Util.getZoomLevelToFitPositions(screenX, screenY, rect, positions, srcreenPosition);
                mapView.setZoomLevel(fitZoomLevle);
                mapView.panToPosition(srcreenPosition);
            } else {
                mapView.setZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                mapView.panToPosition(srcreenPosition);
            }
            
            sphinx.getCenterTokenView().setVisibility(View.INVISIBLE);
            sphinx.getMapToolsView().setVisibility(View.VISIBLE);
            sphinx.getMapCleanBtn().setVisibility(View.VISIBLE);
            sphinx.getLocationView().setVisibility(View.VISIBLE);
            
            itemizedOverlay = overlay;
        } catch (APIException e) {
            e.printStackTrace();
        }
        mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
        
        return itemizedOverlay;
    }
    
}

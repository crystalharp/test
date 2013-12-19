package com.tigerknows.map;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.util.XYFloat;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class InfoWindowHelper {

    public static void showInfoWindow(MapView mapView, OverlayItem overlayItem, ViewGroup viewGroup) {

        try {
            InfoWindow infoWindow = mapView.getInfoWindow();
            infoWindow.setPosition(overlayItem.getPosition());
            
            layoutInfoWindow(viewGroup.getChildAt(0), Globals.g_metrics.widthPixels);
            
            infoWindow.setViewGroup(viewGroup);
            infoWindow.setOffset(new XYFloat((float)(overlayItem.getIcon().getOffset().x - overlayItem.getIcon().getSize().x/2), 
                    (float)(overlayItem.getIcon().getOffset().y)),
                    overlayItem.getRotationTilt());
            infoWindow.setVisible(true);
            
            mapView.refreshMap();
            
        } catch (APIException e) {
            e.printStackTrace();
        }
    }
    
    public static void hideInfoWindow(MapView mapView) {
        InfoWindow infoWindow = mapView.getInfoWindow();
        if (infoWindow.isVisible()) {
            infoWindow.setVisible(false);
            mapView.refreshMap();
        }
    }
    
    private static void layoutInfoWindow(View view, int max) {
        view.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width =  view.getMeasuredWidth();
        if (width > max) {
            view.getLayoutParams().width = max;
        } else {
            view.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
        }
    }

}

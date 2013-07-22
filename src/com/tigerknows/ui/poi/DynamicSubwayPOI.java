package com.tigerknows.ui.poi;

import java.util.List;

import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.POI.Description;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;

public class DynamicSubwayPOI extends DynamicPOIView {

    public DynamicSubwayPOI() {
        
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }
    
    @Override
    public boolean isExist() {
        if (mPOI.getXDescription().containsKey(Description.FIELD_SUBWAY_EXITS)) {
            return true;
        }
        return false;
    }

}

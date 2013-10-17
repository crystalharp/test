package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.DataQuery;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;

public class DynamicDishPOI extends DynamicPOIView {

    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    
    DynamicPOIViewBlock mViewBlock;
    LinearLayout mDynamicDishView;
    
    BlockRefresher mDishRefresher = new BlockRefresher() {

        @Override
        public void refresh() {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    public DynamicDishPOI(POIDetailFragment poiFragment, LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, mDishRefresher);
        mDynamicDishView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_movie_poi, null);
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
        blockList.clear();
        blockList.add(mViewBlock);
        return blockList;
    }

    @Override
    public void refresh() {
        mViewBlock.refresh();
    }

    @Override
    public void loadData(int fromType) {

        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DISH);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_RECOMMEND_DISH);
        queryStart(dataQuery);
        mPOIDetailFragment.addLoadingView();
    }

}

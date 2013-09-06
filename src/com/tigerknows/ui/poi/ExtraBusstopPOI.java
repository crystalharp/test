package com.tigerknows.ui.poi;

import java.util.LinkedList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

public class ExtraBusstopPOI extends DynamicPOIView {
    
    @Override
    public boolean isExist() {
        XMap description = mPOI.getXDescription();
        //包含有线路字段，但是不包含地铁信息字段才会显示
        if (description != null && 
                (description.containsKey(Description.FIELD_LINE)) &&
                !(description.containsKey(Description.FIELD_SUBWAY_EXITS)) && 
                !(description.containsKey(Description.FIELD_SUBWAY_PRESET_TIMES))) {
            return true;
        }
        return false;
    }

    DynamicPOIViewBlock mBusstopBlock;
    
    LinearLayout mBusstopView;
    LinearLayout mBuslineListView;

    LinearListView mBuslinelsv;
    
    List<Line> mBuslineList = null;
    
    BlockRefresher mBusstopRefresher = new BlockRefresher() {

        @Override
        public void refresh() {
            mBuslinelsv.refreshList(mBuslineList);
            refreshBackground(mBuslinelsv, mBuslineList.size());
        }
        
    };
    
    ItemInitializer buslineInit = new ItemInitializer() {

        @Override
        public void initItem(Object data, View v) {
            final Line busline = (Line) data;
            TextView txv = (TextView) v.findViewById(R.id.busline_name);
            txv.setText(busline.getName());
            v.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    mSphinx.getBuslineDetailFragment().setData(busline, 0, mPOI.getName());
                    mSphinx.showView(mSphinx.getBuslineDetailFragment().getId());
                }
            });
        }
        
    };
    
    void refreshBackground(LinearListView lsv, int size) {
        for(int i = 0; i < size; i++) {
            View child = mBuslineListView.getChildAt(i);
            if (i == (size-1)) {
                child.setBackgroundResource(R.drawable.list_footer);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
            }
        }
    }

    
    public ExtraBusstopPOI(POIDetailFragment poiFragment,
            LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        mBusstopBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, mBusstopRefresher);
        mBusstopView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_busstop, null);
        mBuslineListView = (LinearLayout) mBusstopView.findViewById(R.id.busline_lsv);
        mBusstopBlock.mOwnLayout = mBusstopView;
        mBuslinelsv = new LinearListView(mSphinx, mBuslineListView, buslineInit, R.layout.poi_dynamic_busline_item);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        BuslineQuery query = (BuslineQuery) tkAsyncTask.getBaseQuery();
        BuslineModel model = query.getBuslineModel();
        if (model != null && model.getType() == BuslineModel.TYPE_BUSLINE) {
            mBuslineList = model.getLineList();
            if (mBuslineList != null && mBuslineList.size() != 0) {
                refresh();
            }
        }
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {

    }

    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        List<DynamicPOIViewBlock> lst = new LinkedList<DynamicPOIViewBlock>();
        lst.add(mBusstopBlock);
        return lst;
    }

    @Override
    public void refresh() {
        mBusstopBlock.refresh();
    }

    @Override
    public void loadData(int fromType) {
        
        if (mPOI == null) {
            return;
        }
        
        MapEngine mapEngine = MapEngine.getInstance();
        int cityId = mapEngine.getCityId(mPOI.getPosition());
        
        BuslineQuery buslineQuery = new BuslineQuery(mSphinx);
        buslineQuery.setup(cityId, mPOI.getName(), 0, false, R.id.view_traffic_home, null);
        
        queryStart(buslineQuery);

    }

}

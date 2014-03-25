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
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

public class ExtraBusstopPOI extends DynamicPOIView {
    
    @Override
    public boolean isExist() {
        if (mPOI == null) {
            return false;
        }
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

    LinearListAdapter mBuslinelsv;
    
    List<Line> mBuslineList = null;
            
    void refreshBackground(LinearListAdapter lsv, int size) {
        for(int i = 0; i < size; i++) {
            View child = mBuslineListView.getChildAt(i);
            if (i == (size-1)) {
                child.setBackgroundResource(R.drawable.list_footer);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
            }
        }
    }

    
    public ExtraBusstopPOI(POIDetailFragment poiFragment,
            LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mFragmentManager = mSphinx.mFragmentManager;
        mInflater = inflater;
        mBusstopView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_busstop, null);
        mBuslineListView = (LinearLayout) mBusstopView.findViewById(R.id.busline_lsv);
        mBusstopBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, mBusstopView) {

            @Override
            public void refresh() {
                mBuslinelsv.refreshList(mBuslineList);
                refreshBackground(mBuslinelsv, mBuslineList.size());
                show();
            }
        };
        mBuslinelsv = new LinearListAdapter(mSphinx, mBuslineListView, R.layout.poi_dynamic_busline_item) {

            @Override
            public View getView(Object data, View child, int pos) {
                // TODO Auto-generated method stub
                final Line busline = (Line) data;
                TextView txv = (TextView) child.findViewById(R.id.busline_name);
                txv.setText(busline.getName());
                child.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        mFragmentManager.getBuslineDetailFragment().setData(busline, -1, mPOI.getName());
                        mSphinx.showView(mFragmentManager.getBuslineDetailFragment().getId());
                    }
                });
                return null;
            }
            
        };
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        mPOIDetailFragment.minusLoadingView();
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
        
        int cityId = MapEngine.getCityId(mPOI.getPosition());
        
        BuslineQuery buslineQuery = new BuslineQuery(mSphinx);
        buslineQuery.addParameter(BaseQuery.SERVER_PARAMETER_SIZE, "10000");
        buslineQuery.setup(mPOI.getName(), 0, false, mPOIDetailFragment.getId(), null);
        buslineQuery.setCityId(cityId);
        
        queryStart(buslineQuery);
        mPOIDetailFragment.addLoadingView();

    }

}

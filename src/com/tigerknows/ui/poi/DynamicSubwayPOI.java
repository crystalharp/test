package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.PresetTime;
import com.tigerknows.model.POI.Station;
import com.tigerknows.model.POI.SubwayExit;
import com.tigerknows.model.POI.SubwayPresetTime;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

public class DynamicSubwayPOI extends DynamicPOIView {

    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListView lsv;
    DynamicPOIViewBlock mViewBlock;
    List<SubwayPresetTime> mPresetTimeList = new ArrayList<SubwayPresetTime>();
    List<SubwayExit> mExitList = new ArrayList<SubwayExit>();
    
    DynamicPOIViewBlock mSubwayBlock;
    
    LinearLayout mSubwayView;
    LinearLayout mSubwayTimeView;
    LinearLayout mSubwayExitView;
    
    LinearListView mSubwayTimeLsv;
    LinearListView mSubwayExitLsv;
    
    BlockRefresher mSubwayRefresher = new BlockRefresher() {

        @Override
        public void refresh() {
            mSubwayExitLsv.refreshList(mExitList);
            mSubwayTimeLsv.refreshList(mPresetTimeList);
        }
        
    };
    
    ItemInitializer timeInit = new ItemInitializer() {

        @Override
        public void initItem(Object data, View v) {
            
            SubwayPresetTime time = (SubwayPresetTime)data;
            TextView linenameTxv = (TextView) v.findViewById(R.id.line_name_txv);
            TextView timeDetailTxv = (TextView) v.findViewById(R.id.time_detail_txv);
            
            String timeDetail = "";
            for (PresetTime ptime : time.getPresetTimes()) {
                timeDetail += ptime.getDirection();
                timeDetail += "\t";
                timeDetail += ptime.getStartTime();
                timeDetail += "-";
                timeDetail += ptime.getEndTime();
                timeDetail += "\n";
            }
            timeDetail = timeDetail.substring(0, timeDetail.length() - 1);
            
            timeDetailTxv.setText(timeDetail);
            linenameTxv.setText(time.getName());
        }
        
    };
    
    ItemInitializer exitInit = new ItemInitializer() {

        @Override
        public void initItem(Object data, View v) {
            
            SubwayExit exit = (SubwayExit) data;
            
            TextView exitTxv = (TextView) v.findViewById(R.id.exit_name_txv);
            TextView landmarkTxv = (TextView) v.findViewById(R.id.landmark_txv);
            final TextView stationTxv = (TextView) v.findViewById(R.id.station_txv);
            
            exitTxv.setText(exit.getExit());
            landmarkTxv.setText(exit.getLandmark());
            
            View stationView = v.findViewById(R.id.station_view);
            List<Station> s = exit.getStations();
            if (s != null && s.size() > 0) {
                stationView.setVisibility(View.VISIBLE);
                stationTxv.setText(exit.getStations().get(0).getStation());
                stationTxv.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        TrafficQueryFragment.submitBuslineQuery(mSphinx, stationTxv.getText().toString());

                    }
                });
            } else {
                stationView.setVisibility(View.GONE);
            }
            
        }
        
    };
    
    public DynamicSubwayPOI(POIDetailFragment poiFragment, LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        mSubwayBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, mSubwayRefresher);
        mSubwayView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_subway, null);
        mSubwayTimeView = (LinearLayout) mSubwayView.findViewById(R.id.subway_time_lst);
        mSubwayExitView = (LinearLayout) mSubwayView.findViewById(R.id.subway_exit_lst);
        mSubwayBlock.mOwnLayout = mSubwayView;
        
        mSubwayTimeLsv = new LinearListView(mSphinx, mSubwayTimeView, timeInit, R.layout.poi_dynamic_subway_time_item);
        mSubwayExitLsv = new LinearListView(mSphinx, mSubwayExitView, exitInit, R.layout.poi_dynamic_subway_exit_item);
        
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {

    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {

    }

    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        List<DynamicPOIViewBlock> lst = new ArrayList<DynamicPOIViewBlock>();
        lst.add(mSubwayBlock);
        return lst;
    }

    @Override
    public void refresh() {
        mPresetTimeList = mPOI.getSubwayPresetTimeList();
        mExitList = mPOI.getSubwayExitList();
        mSubwayBlock.refresh();
    }
    
    @Override
    public boolean isExist() {
        if (mPOI.getXDescription() != null && mPOI.getXDescription().containsKey(Description.FIELD_SUBWAY_EXITS)) {
            return true;
        }
        return false;
    }

}

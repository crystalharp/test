package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.PresetTime;
import com.tigerknows.model.POI.Busstop;
import com.tigerknows.model.POI.SubwayExit;
import com.tigerknows.model.POI.SubwayPresetTime;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

public class ExtraSubwayPOI extends DynamicPOIView {

    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListView lsv;
    DynamicPOIViewBlock mViewBlock;
    List<SubwayPresetTime> mPresetTimeList = new ArrayList<SubwayPresetTime>();
    List<SubwayExit> mExitList = new ArrayList<SubwayExit>();
    
    DynamicPOIViewBlock mSubwayBlock;
    
    LinearLayout mSubwayView;
    LinearLayout mSubwayTimeInfoView;
    LinearLayout mSubwayExitInfoView;
    View mSubwayExitView;
    View mSubwayPresetTimeView;
    
    LinearListView mSubwayTimeLsv;
    LinearListView mSubwayExitLsv;
    
    BlockRefresher mSubwayRefresher = new BlockRefresher() {

        @Override
        public void refresh() {
            if (mExitList == null || mExitList.size() == 0) {
                mSubwayExitView.setVisibility(View.GONE);
            } else {
                mSubwayExitView.setVisibility(View.VISIBLE);
                mSubwayExitLsv.refreshList(mExitList);
            }
            if (mPresetTimeList == null || mPresetTimeList.size() == 0) {
                mSubwayPresetTimeView.setVisibility(View.GONE);
            } else {
                mSubwayPresetTimeView.setVisibility(View.VISIBLE);
                mSubwayTimeLsv.refreshList(mPresetTimeList);
            }
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
                timeDetail += "  ";
                if (ptime.getStartTime() != null || ptime.getEndTime() != null) {
                    timeDetail += ptime.getStartTime();
                    timeDetail += "-";
                    timeDetail += ptime.getEndTime();
                } else {
                    timeDetail += mSphinx.getString(R.string.subway_no_time_info);
                }
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
            final TextView busstopTxv = (TextView) v.findViewById(R.id.busstop_txv);
            
            exitTxv.setText(exit.getExit());
            String landmark = exit.getLandmark();
            if (TextUtils.isEmpty(landmark)) {
                landmarkTxv.setVisibility(View.GONE);
            } else {
                landmarkTxv.setText(exit.getLandmark());
            }
            
            View busstopView = v.findViewById(R.id.busstop_view);
            List<Busstop> s = exit.getBusstops();
            if (s != null && s.size() > 0 && !TextUtils.isEmpty(s.get(0).getBusstop())) {
                busstopView.setVisibility(View.VISIBLE);
                busstopTxv.setText(s.get(0).getBusstop());
                busstopTxv.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailBusstop, busstopTxv.getText().toString());
                        int cityId = mSphinx.getMapEngine().getCityId(mPOI.getPosition());
                        TrafficQueryFragment.submitBuslineQuery(mSphinx, busstopTxv.getText().toString(), cityId);

                    }
                });
            } else {
                busstopView.setVisibility(View.GONE);
            }
            
        }
        
    };
    
    public ExtraSubwayPOI(POIDetailFragment poiFragment, LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        mSubwayBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, mSubwayRefresher);
        mSubwayView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_subway, null);
        mSubwayTimeInfoView = (LinearLayout) mSubwayView.findViewById(R.id.subway_time_lst);
        mSubwayExitInfoView = (LinearLayout) mSubwayView.findViewById(R.id.subway_exit_lst);
        mSubwayExitView = mSubwayView.findViewById(R.id.subway_exit);
        mSubwayPresetTimeView = mSubwayView.findViewById(R.id.subway_preset_time);
        mSubwayBlock.mOwnLayout = mSubwayView;
        
        mSubwayTimeLsv = new LinearListView(mSphinx, mSubwayTimeInfoView, timeInit, R.layout.poi_dynamic_subway_time_item);
        mSubwayExitLsv = new LinearListView(mSphinx, mSubwayExitInfoView, exitInit, R.layout.poi_dynamic_subway_exit_item);
        
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
        if (mPOI.getXDescription() != null && 
                (mPOI.getXDescription().containsKey(Description.FIELD_SUBWAY_EXITS) ||
                 mPOI.getXDescription().containsKey(Description.FIELD_SUBWAY_PRESET_TIMES))) {
            return true;
        }
        return false;
    }

    @Override
    public void loadData(int fromType) {
        refresh();
    }

}

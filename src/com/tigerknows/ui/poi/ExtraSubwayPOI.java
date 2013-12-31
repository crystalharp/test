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
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.PresetTime;
import com.tigerknows.model.POI.Busstop;
import com.tigerknows.model.POI.SubwayExit;
import com.tigerknows.model.POI.SubwayPresetTime;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

public class ExtraSubwayPOI extends DynamicPOIView {

    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListAdapter lsv;
    DynamicPOIViewBlock mViewBlock;
    List<SubwayPresetTime> mPresetTimeList = new ArrayList<SubwayPresetTime>();
    List<SubwayExit> mExitList = new ArrayList<SubwayExit>();
    
    DynamicPOIViewBlock mSubwayBlock;
    
    LinearLayout mSubwayView;
    LinearLayout mSubwayTimeInfoView;
    LinearLayout mSubwayExitInfoView;
    View mSubwayExitView;
    View mSubwayPresetTimeView;
    
    LinearListAdapter mSubwayTimeLsv;
    LinearListAdapter mSubwayExitLsv;
            
    public ExtraSubwayPOI(POIDetailFragment poiFragment, LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        mSubwayView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_subway, null);
        mSubwayTimeInfoView = (LinearLayout) mSubwayView.findViewById(R.id.subway_time_lst);
        mSubwayExitInfoView = (LinearLayout) mSubwayView.findViewById(R.id.subway_exit_lst);
        mSubwayExitView = mSubwayView.findViewById(R.id.subway_exit);
        mSubwayPresetTimeView = mSubwayView.findViewById(R.id.subway_preset_time);
        mSubwayBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, mSubwayView) {

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
                show();
            }
        };
        
        mSubwayTimeLsv = new LinearListAdapter(mSphinx, mSubwayTimeInfoView, R.layout.poi_dynamic_subway_time_item) {

            @Override
            public View getView(Object data, View child, int pos) {
                SubwayPresetTime time = (SubwayPresetTime)data;
                TextView linenameTxv = (TextView) child.findViewById(R.id.line_name_txv);
                TextView timeDetailTxv = (TextView) child.findViewById(R.id.time_detail_txv);
                
                String timeDetail = "";
                for (PresetTime ptime : time.getPresetTimes()) {
                    if (!TextUtils.isEmpty(ptime.getDirection())) {
                        timeDetail += ptime.getDirection();
                        timeDetail += "  ";
                    }
                    if (!TextUtils.isEmpty(ptime.getStartTime()) && !TextUtils.isEmpty(ptime.getEndTime())) {
                        timeDetail += ptime.getStartTime();
                        timeDetail += "-";
                        timeDetail += ptime.getEndTime();
                    } else {
                        timeDetail += getString(R.string.subway_no_time_info);
                    }
                    timeDetail += "\n";
                }
                timeDetail = timeDetail.substring(0, timeDetail.length() - 1);
                
                timeDetailTxv.setText(timeDetail);
                linenameTxv.setText(time.getName());
                return null;
            }};
        mSubwayExitLsv = new LinearListAdapter(mSphinx, mSubwayExitInfoView, R.layout.poi_dynamic_subway_exit_item){

            @Override
            public View getView(Object data, View child, int pos) {
                SubwayExit exit = (SubwayExit) data;
                
                TextView exitTxv = (TextView) child.findViewById(R.id.exit_name_txv);
                TextView landmarkTxv = (TextView) child.findViewById(R.id.landmark_txv);
                final TextView busstopTxv = (TextView) child.findViewById(R.id.busstop_txv);
                
                exitTxv.setText(exit.getExit());
                String landmark = exit.getLandmark();
                if (TextUtils.isEmpty(landmark)) {
                    landmarkTxv.setVisibility(View.GONE);
                } else {
                    landmarkTxv.setText(exit.getLandmark());
                }
                
                View busstopView = child.findViewById(R.id.busstop_view);
                List<Busstop> s = exit.getBusstops();
                if (s != null && s.size() > 0 && !TextUtils.isEmpty(s.get(0).getBusstop())) {
                    busstopView.setVisibility(View.VISIBLE);
                    busstopTxv.setText(s.get(0).getBusstop());
                    busstopTxv.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailBusstop, busstopTxv.getText().toString());
                            int cityId = MapEngine.getCityId(mPOI.getPosition());
                            String keyword = busstopTxv.getText().toString();
                            POI poi = new POI();
                            poi.setName(keyword);
                            mSphinx.getTrafficQueryFragment().addHistoryWord(poi, HistoryWordTable.TYPE_BUSLINE);
                            BuslineQuery buslineQuery = new BuslineQuery(mPOIDetailFragment.mSphinx);
                            buslineQuery.setup(keyword, 0, false, R.id.view_traffic_home, getString(R.string.doing_and_wait));
                            buslineQuery.setCityId(cityId);
                            
                            queryStart(buslineQuery);

                        }
                    });
                } else {
                    busstopView.setVisibility(View.GONE);
                }
                return null;
            }};
        
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        String apiType = baseQuery.getAPIType();
        if (BaseQuery.API_TYPE_BUSLINE_QUERY.equals(apiType)) {
            InputSearchFragment.dealWithBuslineResponse(mSphinx, (BuslineQuery)baseQuery, mPOIDetailFragment.mActionTag, null);
        }
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

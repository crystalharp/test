/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.List;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import com.tigerknows.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficModel.Station;
import com.tigerknows.model.TrafficQuery;

/**
 * @author Peng Wenyue
 */
public class TrafficAlternativesDialog extends BaseDialog {

    private static final String TAG = "TrafficAlternativesDialog";

    private ListView mStardLsv = null;

    private ListView mEndLsv = null;

    private POI mStartPOI = null;
    
    private POI mEndPOI = null;
    
    private TrafficQuery mTrafficQuery;
    
    private List<Station> mStartStations = null;
    
    private List<Station> mEndStations = null;

    private TrafficQueryFragment mPreQueryView;
    
    public TrafficAlternativesDialog(Context context) {
        this(context, R.style.Theme_Dialog);
    }

    public TrafficAlternativesDialog(Context context, int theme) {
        super(context, theme);
        mActionTag = ActionLog.TrafficAlternative;
		mId = R.id.dialog_traffic_alternatives;

        setContentView(R.layout.traffic_alternatives);
        findViews();
        setListener();

        mRightBtn.setVisibility(View.GONE);
    }

    protected void findViews() {
    	super.findViews();
        mStardLsv = (ListView)findViewById(R.id.start_lsv);
        mEndLsv = (ListView)findViewById(R.id.end_lsv);
    }

    protected void setListener() {
    	super.setListener();
        mStardLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                mStartPOI = mStartStations.get(arg2).toPOI();
                
            	mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "start", arg2);
                
                if(POI.isNameEqual(mStartPOI, mEndPOI)){
    	            mSphinx.showTip(R.string.start_equal_to_end_select, Toast.LENGTH_SHORT);
    	            return;
                }
                mPreQueryView.setData(mStartPOI, TrafficQueryFragment.START);

                if(mStartPOI != null && mEndPOI != null){
                    makeQuery();
                }else{
                    setViewVisibility(mEndLsv);
                }
            }
        });
        
        mEndLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {            	
            	mEndPOI = mEndStations.get(arg2).toPOI();
            	
            	mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "end", arg2);
            	
            	if(POI.isNameEqual(mStartPOI, mEndPOI)){
    	            mSphinx.showTip(R.string.start_equal_to_end_select, Toast.LENGTH_SHORT);
    	            return;
                }
                mPreQueryView.setData(mEndPOI, TrafficQueryFragment.END);
                
                if(mStartPOI != null && mEndPOI != null){
                    makeQuery();
                }else{
                    LogWrapper.d(TAG, "mStartPOI is null");
                }

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                mActionLog.addAction(ActionLog.KEYCODE, "back");
                if (mEndLsv.getVisibility() == View.VISIBLE) {

                    if(mStartStations == null || mStartStations.size() == 1) {
                        dismiss();
                    } else {
                        setViewVisibility(mStardLsv);
                    }
                } else {
                    dismiss();
                }
                return true;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                mActionLog.addAction(ActionLog.KEYCODE, "back");
                return true;

            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void setData(TrafficQuery trafficQuery, TrafficQueryFragment queryView) {
        mStartPOI = null;
        mEndPOI = null;
        
        
        mPreQueryView = queryView;      
        setViewVisibility(mStardLsv);
        
        mTrafficQuery = trafficQuery;
        mStartStations = trafficQuery.getTrafficModel().getStartAlternativesList();
        mEndStations = trafficQuery.getTrafficModel().getEndAlternativesList();

        initStartStations();
        initEndStations();
        
        if(mStartPOI != null && mEndPOI != null){
            makeQuery();
        }else if (!isShowing()) {
            mSphinx.showView(mId);
        }

    }
    
    @Override
	public void show() {
		// TODO Auto-generated method stub
        super.show();
//        if (mSphinx.uiStackContains(mId)) {
//        	mSphinx.uiStackPop();
//        }
	}

	private void initStartStations(){
        if(mStartStations == null){
            // get Start POI from previous view
            mStartPOI = mPreQueryView.getStartPOI();
            if(mEndPOI == null)
                setViewVisibility(mEndLsv);
        } else if (mStartStations != null && mStartStations.size() >= 1){
        	setAdapter(mStardLsv, mStartStations);
        	setViewVisibility(mStardLsv);
        }
    }
    
    private void initEndStations(){
    	if(mEndStations == null){
            // get End POI from previous view
            mEndPOI = mPreQueryView.getEndPOI();
        }else if(mEndStations != null && mEndStations.size() >= 1){
            setAdapter(mEndLsv, mEndStations);
        }
    }
    
    private void setAdapter(ListView listView, List<Station> stations){
        if(stations != null && stations.size() != 0){
            int len = stations.size();
            int i = 0;

            String names[] = new String[len];
            for(Station station:stations) {
                names[i++] =  station.getName();
            }

            listView.setAdapter(new StringArrayAdapter(mContext, names));
        }
    }
    
    private void setViewVisibility(View view){    
        if(R.id.start_lsv == view.getId()){
            mStardLsv.setVisibility(View.VISIBLE);
            mEndLsv.setVisibility(View.GONE);
            mTitleBtn.setText(mContext.getString(R.string.confirm_start_station));
            
        }else if(R.id.end_lsv == view.getId()){
            mStardLsv.setVisibility(View.GONE);
            mEndLsv.setVisibility(View.VISIBLE);
            mTitleBtn.setText(mContext.getString(R.string.confirm_end_station));
        }
    }
    
    private void makeQuery(){

    	
        mPreQueryView.submitTrafficQuery();
        dismiss();
    }

}

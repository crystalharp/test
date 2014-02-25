package com.tigerknows.ui.alarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.Alarm;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.InputSearchFragment;

public class AlarmAddFragment extends BaseFragment implements View.OnClickListener {

	static final String TAG = "AlarmAddFragment";

    private Button mBusStationBtn = null;

    private Button mBusLineBtn = null;
    
    public AlarmAddFragment(Sphinx sphinx) {
        super(sphinx);
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficFetchFavorite;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.alarm_add, container, false);
        findViews();
        setListener();
        
        return mRootView;
	}
	
    @Override
	protected void findViews() {
        super.findViews();
        mBusStationBtn = (Button)mRootView.findViewById(R.id.bus_station_btn);
        mBusLineBtn = (Button)mRootView.findViewById(R.id.bus_line_btn);
    }
	
	@Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();

        mTitleBtn.setText(getString(R.string.add_alarm));
    }

    @Override
    protected void setListener() {
        super.setListener();

        mBusStationBtn.setOnClickListener(this);
        mBusLineBtn.setOnClickListener(this);
	}


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bus_station_btn) {
            DataQuery dataQuery = new DataQuery(mSphinx);
            mSphinx.getInputSearchFragment().setData(dataQuery,
                    null,
                    InputSearchFragment.MODE_TRAFFIC,
                    new InputSearchFragment.IResponsePOI() {
                        
                        @Override
                        public void responsePOI(POI poi) {
                            Alarm alarm = new Alarm(mSphinx);
                            alarm.setName(poi.getName());
                            alarm.setPosition(poi.getPosition());
                            Alarm.writeAlarm(mSphinx, alarm);
                            mSphinx.uiStackClearTop(R.id.view_alarm_list);
                        }
                    },
                    InputSearchFragment.REQUEST_COMMON_PLACE);
            mSphinx.showView(R.id.view_poi_input_search);
        } else if (id == R.id.bus_line_btn) {

            DataQuery dataQuery = new DataQuery(mSphinx);
            mSphinx.getInputSearchFragment().setData(dataQuery,
                    null,
                    InputSearchFragment.MODE_BUELINE);
            mSphinx.showView(R.id.view_poi_input_search);
        }
    }

}

package com.tigerknows.ui.alarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKFragmentManager;
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
        mActionTag = ActionLog.AlarmAdd;
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
            addActionLog(ActionLog.AlarmAddBusStops);
            DataQuery dataQuery = new DataQuery(mSphinx);
            mSphinx.getInputSearchFragment().setData(dataQuery,
                    null,
                    InputSearchFragment.MODE_BUSLINE,
                    null,
                    InputSearchFragment.REQUEST_ONLY_BUS_STATION);
            mSphinx.showView(TKFragmentManager.ID_view_poi_input_search);
        } else if (id == R.id.bus_line_btn) {
            addActionLog(ActionLog.AlarmAddBusLine);
            DataQuery dataQuery = new DataQuery(mSphinx);
            mSphinx.getInputSearchFragment().setData(dataQuery,
                    null,
                    InputSearchFragment.MODE_BUSLINE,
                    null,
                    InputSearchFragment.REQUEST_ONLY_BUS_LINE);
            mSphinx.showView(TKFragmentManager.ID_view_poi_input_search);
        }
    }

}

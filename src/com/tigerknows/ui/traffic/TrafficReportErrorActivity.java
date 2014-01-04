package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.tigerknows.R;
import com.tigerknows.ui.BaseActivity;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.model.TrafficModel.Station;

public class TrafficReportErrorActivity extends BaseActivity {

	private LinearLayout mStationErrorLnl;
	
	private LinearLayout mLineErrorLnl;
	
	private EditText mContentEdt;
	
	private EditText mPhoneEdt;
	
	private Plan mPlan;
	
	private List<Station> mStationList;
	
	private List<String> mStationNoLineList;
	
	private List<Station> mStationCheckedList = new ArrayList<Station>();
	
	private List<String> mLinecheckedList = new ArrayList<String>();
	
	private static List<Object> sTargetList = new ArrayList<Object>();
	
	private static final String TAG = "TransferErrorRecovery";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mActionTag = ActionLog.TransferErrorRecovery;
		setContentView(R.layout.traffic_error_recovery);
		
		findViews();
		setListener();
		
        mTitleBtn.setText(getString(R.string.erreport_title));
        mRightBtn.setText(R.string.submit);
        mRightBtn.setVisibility(View.VISIBLE);
		
		synchronized (sTargetList) {
            int size = sTargetList.size();
            if (size > 0) {
                Object target = sTargetList.get(size-1);
                if (target instanceof Plan) {
                	mPlan = (Plan) target;
                }
            }
        }
		
		init();
		mPhoneEdt.getParent().requestLayout();
	}

	@Override
	protected void findViews() {
		// TODO Auto-generated method stub
		super.findViews();
		mStationErrorLnl = (LinearLayout)findViewById(R.id.station_error_lnl);
		mLineErrorLnl = (LinearLayout)findViewById(R.id.line_error_lnl);
		mContentEdt = (EditText)findViewById(R.id.content_edt);
		mPhoneEdt = (EditText)findViewById(R.id.mobile_phone_edt);
	}
	
	@Override
	protected void setListener() {
		// TODO Auto-generated method stub
		super.setListener();
		
		mRightBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				StringBuilder result = new StringBuilder();
                result.append("T");
                result.append("_C" + MapEngine.getCityId(mPlan.getEnd().getPosition()));
				result.append("_S");
				
				String stationError = "";
				if (mStationCheckedList.size() > 0) {
					for(int i = 0; i < mStationCheckedList.size(); i++) {
						Station station = mStationCheckedList.get(i);
						if (i == 0) {
							stationError += station.getName() + "-" + station.getPosition().getLon() 
									+ "-" + station.getPosition().getLat();
						} else {
							stationError += "-" + station.getName() + "-" + station.getPosition().getLon() 
									+ "-" + station.getPosition().getLat();
						}
					}
				}
				result.append(stationError);
				
				result.append("_L");
				String lineError = "";
				if (mLinecheckedList.size() > 0) {
					for(int i = 0; i < mLinecheckedList.size(); i++) {
						String s = mLinecheckedList.get(i);
						if (i == 0) {
							lineError += s;
						} else {
							lineError += "-" + s;
						}
					}
				}
				result.append(lineError);
				
				result.append("_D");
				String descript = mContentEdt.getText().toString().trim().replaceAll("\n", "\\n");
				if (!TextUtils.isEmpty(descript)) {
					result.append(descript);
				}
				
				result.append("_P");
				String phone = mPhoneEdt.getText().toString().trim().replaceAll("\n", "\\n");
				if (!TextUtils.isEmpty(phone)) {
					result.append(phone);
				}
				
				if (TextUtils.isEmpty(stationError) && TextUtils.isEmpty(lineError) && TextUtils.isEmpty(descript)) {
					Toast.makeText(TrafficReportErrorActivity.this, R.string.errorrecovery_empty, Toast.LENGTH_LONG).show();
					return;
				}
				
//				String result = "T" + stationError + lineError + descript + phone;
				LogWrapper.d(TAG, "errorrecovery: " + result);
				mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
                feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY, result.toString());
                feedbackUpload.setup(-1, -1, mThis.getString(R.string.doing_and_wait));
                queryStart(feedbackUpload);
			}
		});
	}
	
	private void init() {
		mStationList = getTransferStationList();
		mStationNoLineList = getTransferStationNoLineList();
		
		if (mStationList.size() == 0) {
			mStationErrorLnl.setVisibility(View.GONE);
		} else {
			mStationErrorLnl.setVisibility(View.VISIBLE);
			generateViewForErrorStation();
		}
		
		if (mStationNoLineList.size() == 0) {
			mLineErrorLnl.setVisibility(View.GONE);
		} else {
			mLineErrorLnl.setVisibility(View.VISIBLE);
			generateViewForErrorLine();
		}
	}
	
	public static void addTarget(Object obejct) {
        synchronized (sTargetList) {
            sTargetList.add(obejct);
        }
    }
	
	private void generateViewForErrorStation() {
		
		for(int i = 0; i < mStationList.size(); i++) {
			
			final Station station = mStationList.get(i);
			RelativeLayout view = (RelativeLayout)mLayoutInflater.inflate(R.layout.traffic_error_recovery_item, null, false);
			CheckBox checkbox = (CheckBox)view.findViewById(R.id.checkbox);
			checkbox.setText(station.getName());
			
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					if (isChecked) {
						mStationCheckedList.add(station);
					} else {
						mStationCheckedList.remove(station);
					}
				}
			});
			
			mStationErrorLnl.addView(view);
		}
		mStationErrorLnl.invalidate();
		LogWrapper.d(TAG, "mStationErrorLnl.getChildCount(): " +mStationErrorLnl.getChildCount());
	}
	
	private void generateViewForErrorLine() {
		for(int i = 0; i < mStationNoLineList.size(); i++) {
			
			final String s = mStationNoLineList.get(i);
			View view = mLayoutInflater.inflate(R.layout.traffic_error_recovery_item, null, false);
			final CheckBox checkbox = (CheckBox)view.findViewById(R.id.checkbox);
			checkbox.setText(s);
			
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					if (isChecked) {
						mLinecheckedList.add(s);
					} else {
						mLinecheckedList.remove(s);
					}
				}
			});
			
			mLineErrorLnl.addView(view);
		}
		mLineErrorLnl.invalidate();
		LogWrapper.d(TAG, "mLineErrorLnl.getChildCount(): " +mLineErrorLnl.getChildCount());
	}
	
    private List<Station> getTransferStationList() {
    	List<Station> stationList = new ArrayList<Station>();
    	for(Step step : mPlan.getStepList()) {
    		if (Step.TYPE_TRANSFER == step.getType() && Step.TYPE_LINE_BUS == step.getLineType()) {
    			Station upStation = new Station(step.getTransferUpStopName(), step.getPositionList().get(0));
	            Position endPosition = step.getPositionList().get(step.getPositionList().size()-1);
    			Station downStation = new Station(step.getTransferDownStopName(), endPosition);
    			
    			addStationToListBasedonName(stationList, upStation);
    			addStationToListBasedonName(stationList, downStation);
    		}
    	}
    	
    	return stationList;
    }
    
    private void addStationToListBasedonName(List<Station> stationList, Station station) {
    	boolean exits = false;
    	for (Station s : stationList) {
    		if (!TextUtils.isEmpty(s.getName()) && s.getName().equals(station.getName())) {
    			exits = true;
    		}
    	}
    	
    	if (!exits) {
    		stationList.add(station);
    	}
    }
    
    private List<String> getTransferStationNoLineList() {
    	List<String> stationNoLineList = new ArrayList<String>();
    	String or = '('+getString(R.string.or);
    	for(Step step : mPlan.getStepList()) {
    		if (Step.TYPE_TRANSFER == step.getType() && Step.TYPE_LINE_BUS == step.getLineType()) {
    			String lineName = step.getTransferLineName();
    			if (lineName.indexOf(or) == -1) {
    				stationNoLineList.add(getString(R.string.staion_no_line, step.getTransferUpStopName(), lineName));
    				stationNoLineList.add(getString(R.string.staion_no_line, step.getTransferDownStopName(), lineName));
    			} else {
    				stationNoLineList.add(getString(R.string.staion_no_line, 
    						step.getTransferUpStopName(), lineName.substring(0, lineName.indexOf(or))));
    				stationNoLineList.add(getString(R.string.staion_no_line, 
    						step.getTransferDownStopName(), lineName.substring(0, lineName.indexOf(or))));
    				
    				String extraLines = lineName.substring(lineName.indexOf(or)+2, lineName.length()-1);
    				StringTokenizer tokenizer = new StringTokenizer(extraLines, ",");
    				while(tokenizer.hasMoreTokens()) {
    					String element = tokenizer.nextToken();
    					stationNoLineList.add(getString(R.string.staion_no_line, 
    							step.getTransferUpStopName(), element));
    					stationNoLineList.add(getString(R.string.staion_no_line, 
    							step.getTransferDownStopName(), element));
    				}
    			}
    		}
    	}
    	return stationNoLineList;
    }
	
	@Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, this, false)) {
            return;
        }
        Toast.makeText(mThis, R.string.error_recovery_success, Toast.LENGTH_LONG).show();
        finish();
    }
	
	public void finish() {
        synchronized (sTargetList) {
            int size = sTargetList.size();
            if (size > 0) {
                sTargetList.remove(size-1);
            }
        }
        super.finish();        
    }
	
}

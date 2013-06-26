package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.opengl.Visibility;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.tigerknows.ui.BaseActivity;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;

public class POIReportErrorActivity extends BaseActivity implements View.OnClickListener{
    
	private Button mNameBtn;
	private Button mTelBtn;
	private Button mNotexistBtn;
	private Button mAddressBtn;
	private Button mRedundancyBtn;
	private Button mLocationBtn;
	private Button mOtherBtn;


    private POI mPOI;
    
    private static List<Object> sTargetList = new ArrayList<Object>();

	private static final String TAG = "ErrorRecovery";
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.POIErrorRecovery;
        setContentView(R.layout.poi_report_error);
        
        findViews();
        setListener();

        mTitleBtn.setText(R.string.error_recovery);
        mRightBtn.setBackgroundResource(R.drawable.btn_submit_comment);
        
        synchronized (sTargetList) {
            int size = sTargetList.size();
            if (size > 0) {
                Object target = sTargetList.get(size-1);
                if (target instanceof POI) {
                    mPOI = (POI) target;
                }
            }
        }
        
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();
        mNameBtn = (Button) findViewById(R.id.name_btn);
        mTelBtn = (Button) findViewById(R.id.tel_btn);
        mNotexistBtn = (Button) findViewById(R.id.notexist_btn);
        mAddressBtn = (Button) findViewById(R.id.address_btn);
        mRedundancyBtn = (Button) findViewById(R.id.redundancy_btn);
        mLocationBtn = (Button) findViewById(R.id.location_btn);
        mOtherBtn = (Button) findViewById(R.id.other_btn);
    }

    /**
     * Set the listeners for the commit and cancel button
     */
    protected void setListener() {
        super.setListener();
        mRightBtn.setOnClickListener(this);
        
        mNameBtn.setOnClickListener(this);
        mTelBtn.setOnClickListener(this);
        mNotexistBtn.setOnClickListener(this);
        mAddressBtn.setOnClickListener(this);
        mRedundancyBtn.setOnClickListener(this);
        mLocationBtn.setOnClickListener(this);
        mOtherBtn.setOnClickListener(this);
    }
    
    public static void addTarget(Object obejct) {
        synchronized (sTargetList) {
            sTargetList.add(obejct);
        }
    }
    @Override
    public void onResume(){
    	super.onResume();
    	mRightBtn.setVisibility(View.GONE);
    }
    
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                // md=$uid-$errcode[-$detail]_$uid-$errcode[-$detail]_$uid-$errcode[-$detail]_ 
                // md=edc1e4c9-5081-428a-935d-1d31fd3848f5-502_8d5cc821-c130-4f9e-acfa-d4906e82c016-404

//                Hashtable<String, String> criteria = new Hashtable<String, String>();
//                criteria.put(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY, s.toString());
//                FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
//                feedbackUpload.setup(criteria, Globals.getCurrentCityInfo().getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
//                queryStart(feedbackUpload);
                break;
            default:
                
        }
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

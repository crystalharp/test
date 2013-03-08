package com.tigerknows;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.tigerknows.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;
import com.tigerknows.util.TKAsyncTask;

public class POIErrorRecovery extends BaseActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    
    private RadioGroup mPOIRgp;
    private RadioButton mBaseInfomationErrorRbt;
    private RadioButton mPlaceDuplicationErrorRbt;
    private RadioButton mPlaceAbsentErrorRbt;
    private RadioButton mOtherErrorRbt;
    private CheckBox mNameErrorChb = null;
    private CheckBox mAddressErrorChb = null;
    private CheckBox mTelephoneErrorChb = null;
    private CheckBox mCoordinateErrorChb = null;

    private POI mPOI;
    private EditText mContentEdt = null;
    private EditText mMobilePhoneEdt = null;
    
    private static List<Object> sTargetList = new ArrayList<Object>();

	private static final String TAG = "ErrorRecovery";
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.POIErrorRecovery;
        setContentView(R.layout.poi_error_recovery);
        
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
        
        mPOIRgp.requestFocus();
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();
        mPOIRgp = (RadioGroup)findViewById(R.id.poi_rgp);
        mBaseInfomationErrorRbt = (RadioButton)findViewById(R.id.base_infomation_error_rbt);
        mPlaceAbsentErrorRbt = (RadioButton)findViewById(R.id.place_absent_error_rbt);
        mPlaceDuplicationErrorRbt = (RadioButton)findViewById(R.id.place_duplication_error_rbt);
        mOtherErrorRbt = (RadioButton)findViewById(R.id.other_error_rbt);
        mNameErrorChb = (CheckBox)findViewById(R.id.name_error_chb);
        mAddressErrorChb = (CheckBox)findViewById(R.id.address_error_chb);
        mTelephoneErrorChb = (CheckBox)findViewById(R.id.telephone_error_chb);
        mCoordinateErrorChb = (CheckBox)findViewById(R.id.coordinate_error_chb);
        mContentEdt = (EditText)findViewById(R.id.content_edt);
        mMobilePhoneEdt = (EditText)findViewById(R.id.mobile_phone_edt);
    }

    /**
     * Set the listeners for the commit and cancel button
     */
    protected void setListener() {
        super.setListener();
        mRightBtn.setOnClickListener(this);
        
        mBaseInfomationErrorRbt.setOnClickListener(this);
        mPlaceDuplicationErrorRbt.setOnClickListener(this);
        mPlaceAbsentErrorRbt.setOnClickListener(this);
        mOtherErrorRbt.setOnClickListener(this);
        mNameErrorChb.setOnClickListener(this);
        mAddressErrorChb.setOnClickListener(this);
        mTelephoneErrorChb.setOnClickListener(this);
        mCoordinateErrorChb.setOnClickListener(this);
    }
    
    public static void addTarget(Object obejct) {
        synchronized (sTargetList) {
            sTargetList.add(obejct);
        }
    }
    
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "titleRight");
                // md=$uid-$errcode[-$detail]_$uid-$errcode[-$detail]_$uid-$errcode[-$detail]_ 
                // md=edc1e4c9-5081-428a-935d-1d31fd3848f5-502_8d5cc821-c130-4f9e-acfa-d4906e82c016-404
                StringBuilder s = new StringBuilder();
                s.append("P_" + mPOI.getUUID());
                s.append("_C" + mMapEngine.getCityId(mPOI.getPosition()));
                StringBuilder errorCode = new StringBuilder();
                switch (mPOIRgp.getCheckedRadioButtonId()) {
                    case R.id.base_infomation_error_rbt:
                    	List<String> tmp = new ArrayList<String>();
                        if (mNameErrorChb.isChecked()) {
                        	tmp.add(mThis.getString(R.string.poi_name_error));
                        }
                        if (mAddressErrorChb.isChecked()) {
                        	tmp.add(mThis.getString(R.string.poi_address_error));
                        }
                        if (mTelephoneErrorChb.isChecked()) {
                        	tmp.add(mThis.getString(R.string.poi_phone_error));
                        }
                        if (mCoordinateErrorChb.isChecked()) {
                        	tmp.add(mThis.getString(R.string.poi_mark_error));
                        }
                        for(int i = 0; i < tmp.size(); i++) {
                        	if (i == 0) {
                        		errorCode.append(tmp.get(i));
                        	} else {
                        		errorCode.append("-" + tmp.get(i));
                        	}
                        }
                        
                        break;
                        
                    case R.id.place_duplication_error_rbt:
                    	errorCode.append(mThis.getString(R.string.poi_address_duplicate_error));
                        break;
                        
                    case R.id.place_absent_error_rbt:
                    	errorCode.append(mThis.getString(R.string.poi_address_noexist_error));                            
                        break;
                        
                    case R.id.other_error_rbt:
                    	errorCode.append(mThis.getString(R.string.poi_ohter_error));
                        break;
    
                    default:
                        break;
                }
                s.append("_E");
                if (!TextUtils.isEmpty(errorCode.toString())) {
                	s.append(errorCode.toString());
                }

                s.append("_D");
                String content = mContentEdt.getText().toString().trim().replace("\n", "\\n").replace("_", "#");
                if (!TextUtils.isEmpty(content)) {
                    s.append(content);
                }
                
                s.append("_P");
                String mobilePhone = mMobilePhoneEdt.getText().toString().trim().replace("\n", "\\n").replace("_", "#");
                if (!TextUtils.isEmpty(mobilePhone)) {
                    s.append(mobilePhone);
                }
                
                LogWrapper.d(TAG, "errorrecovery: " + s);
                
//                if (TextUtils.isEmpty(errorCode.toString()) && TextUtils.isEmpty(content)) {
//					Toast.makeText(POIErrorRecovery.this, R.string.errorrecovery_empty, 3000).show();
//                	return;
//                }
                
                hideSoftInput();
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY, s.toString());
                FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
                feedbackUpload.setup(criteria, Globals.g_Current_City_Info.getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
                queryStart(feedbackUpload);
                break;
                
            case R.id.name_error_chb:
            case R.id.address_error_chb:
            case R.id.telephone_error_chb:
            case R.id.coordinate_error_chb:
                mPOIRgp.check(R.id.base_infomation_error_rbt);
                break;
            
            case R.id.base_infomation_error_rbt:
            case R.id.place_absent_error_rbt:
            case R.id.place_duplication_error_rbt:
            case R.id.other_error_rbt:
                if (mPOIRgp.getCheckedRadioButtonId() != R.id.base_infomation_error_rbt) {
                    mNameErrorChb.setChecked(false);
                    mAddressErrorChb.setChecked(false);
                    mTelephoneErrorChb.setChecked(false);
                    mCoordinateErrorChb.setChecked(false);
                }                 
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

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean checked) {
        if (checked) {
            mPOIRgp.check(R.id.base_infomation_error_rbt);
        }
    }
}

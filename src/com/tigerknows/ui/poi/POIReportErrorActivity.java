package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

import com.tigerknows.R;
import com.tigerknows.R.id;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;

public class POIReportErrorActivity extends BaseActivity implements View.OnClickListener{
	
	private static final int HOME_PAGE = 0100;	//01000000
	private static final int NAME_ERR = 015;	//00001101
	private static final int TEL_ERR = 0251;	//10101001
	private static final int TEL_ADD = 0211;	//10001001
	private static final int NOT_EXIST = 021;	//00010001
	private static final int NE_OTHER = 023;	//00010011
	private static final int ADDRESS_ERR = 011;	//00001001
	private static final int OTHER_ERR = 03;	//00000011
	// 8位：是否添加区号
	
	private static final int HOME_LLY = 64;
	private static final int TEL_LLY = 32;
	private static final int NOTEXIST_LLY = 16;
	private static final int MAIN_LLY = 8;
	private static final int TYPE_LLY = 4;
	private static final int DESCRIPTION_LLY = 2;
	private static final int CONTACT_LLY = 1;
    
	private LinearLayout mHomeLly;
	private LinearLayout mTelLly;
	private LinearLayout mNotExistLly;
	private LinearLayout mMainLly;
	private LinearLayout mTypeLly;
	private LinearLayout mDescriptionLly;
	private LinearLayout mContactLly;
	
	private Button mNameBtn;
	private Button mTelBtn;
	private Button mNotexistBtn;
	private Button mAddressBtn;
	private Button mRedundancyBtn;
	private Button mLocationBtn;
	private Button mOtherBtn;
	
	private RadioButton mTelConnectRbt;
	private RadioButton mTelNotthisRbt;
	private RadioButton mNeStopRbt;
	private RadioButton mNeChaiRbt;
	private RadioButton mNeMoveRbt;
	private RadioButton mNeFindRbt;
	private RadioButton mNeOtherRbt;
	
	private TextView mStarTxv;
	private TextView mMainTxv;
	private EditText mMainEdt;
	private EditText mTypeEdt;
	private EditText mDescriptionEdt;
	private EditText mContactEdt;
	
	private int mStatus;
	private String mOrigin;

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
        mStatus = HOME_PAGE;
        
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
        mHomeLly = (LinearLayout) findViewById(R.id.home_lly);
        mTelLly = (LinearLayout) findViewById(R.id.tel_lly);
        mNotExistLly = (LinearLayout) findViewById(R.id.notexist_lly);
        mMainLly = (LinearLayout) findViewById(R.id.main_lly);
        mTypeLly = (LinearLayout) findViewById(R.id.type_lly);
        mDescriptionLly = (LinearLayout) findViewById(R.id.description_lly);
        mContactLly = (LinearLayout) findViewById(R.id.contact_lly);
        mNameBtn = (Button) findViewById(R.id.name_btn);
        mTelBtn = (Button) findViewById(R.id.tel_btn);
        mNotexistBtn = (Button) findViewById(R.id.notexist_btn);
        mAddressBtn = (Button) findViewById(R.id.address_btn);
        mRedundancyBtn = (Button) findViewById(R.id.redundancy_btn);
        mLocationBtn = (Button) findViewById(R.id.location_btn);
        mOtherBtn = (Button) findViewById(R.id.other_btn);
        mTelConnectRbt = (RadioButton) findViewById(R.id.tel_connect_rbt);
        mTelNotthisRbt = (RadioButton) findViewById(R.id.tel_notthis_rbt);
        mNeStopRbt = (RadioButton) findViewById(R.id.ne_stop_rbt);
        mNeChaiRbt = (RadioButton) findViewById(R.id.ne_chai_rbt);
        mNeMoveRbt = (RadioButton) findViewById(R.id.ne_move_rbt);
        mNeFindRbt = (RadioButton) findViewById(R.id.ne_find_rbt);
        mNeOtherRbt = (RadioButton) findViewById(R.id.ne_other_rbt);
        mStarTxv = (TextView) findViewById(R.id.star_txv);
        mMainTxv = (TextView) findViewById(R.id.main_txv);
        mMainEdt = (EditText) findViewById(R.id.main_edt);
        mTypeEdt = (EditText) findViewById(R.id.type_edt);
        mDescriptionEdt = (EditText) findViewById(R.id.description_edt);
        mContactEdt = (EditText) findViewById(R.id.contact_edt);
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
        mTelConnectRbt.setOnClickListener(this);
        mTelNotthisRbt.setOnClickListener(this);
        mNeStopRbt.setOnClickListener(this);
        mNeChaiRbt.setOnClickListener(this);
        mNeMoveRbt.setOnClickListener(this);
        mNeFindRbt.setOnClickListener(this);
        mNeOtherRbt.setOnClickListener(this);
    }
    
    public static void addTarget(Object obejct) {
        synchronized (sTargetList) {
            sTargetList.add(obejct);
        }
    }
    @Override
    public void onResume(){
    	super.onResume();
    	refreshData();
    }
    
    private void refreshData(){
    	if((mStatus & HOME_LLY) != 0)mHomeLly.setVisibility(View.VISIBLE);
    	else mHomeLly.setVisibility(View.GONE);
    	if((mStatus & TEL_LLY) != 0)mTelLly.setVisibility(View.VISIBLE);
    	else mTelLly.setVisibility(View.GONE);
    	if((mStatus & NOTEXIST_LLY) != 0)mNotExistLly.setVisibility(View.VISIBLE);
    	else mNotExistLly.setVisibility(View.GONE);
    	if((mStatus & MAIN_LLY) != 0)mMainLly.setVisibility(View.VISIBLE);
    	else mMainLly.setVisibility(View.GONE);
    	if((mStatus & TYPE_LLY) != 0)mTypeLly.setVisibility(View.VISIBLE);
    	else mTypeLly.setVisibility(View.GONE);
    	if((mStatus & DESCRIPTION_LLY) != 0)mDescriptionLly.setVisibility(View.VISIBLE);
    	else mDescriptionLly.setVisibility(View.GONE);
    	if((mStatus & CONTACT_LLY) != 0)mContactLly.setVisibility(View.VISIBLE);
    	else mContactLly.setVisibility(View.GONE);
    	switch(mStatus){
    	case TEL_ERR:
    		mMainTxv.setText("商户电话");
    		break;
    	case ADDRESS_ERR:
    		mMainTxv.setText("地　　址");
    		break;
    	case NAME_ERR:
    		mMainTxv.setText("商户名称");
    		break;
    	case TEL_ADD:
    		mMainTxv.setText("商户电话");
    		break;
    	}
    	if(mStatus == HOME_PAGE){
        	mRightBtn.setVisibility(View.GONE);
    	}else{
    		mRightBtn.setVisibility(View.VISIBLE);
    		mRightBtn.setText("提交");
    		mRightBtn.setTextColor(getResources().getColor(R.color.white));
    		mRightBtn.setBackgroundColor(getResources().getColor(R.color.transparent));
    	}
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
                finish();
                break;
            case R.id.name_btn:
            	mStatus = NAME_ERR;
            	refreshData();
            	break;
            case R.id.tel_btn:
            	mStatus = TEL_ERR;
            	refreshData();
            	break;
            case R.id.notexist_btn:
            	mStatus = NOT_EXIST;
            	refreshData();
            	break;
            case R.id.address_btn:
            	mStatus = ADDRESS_ERR;
            	refreshData();
            	break;
            case R.id.redundancy_btn:
                Utility.showNormalDialog(mThis,
                        getString(R.string.prompt), 
                        getString(R.string.erreport_redundancy_confirm),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                switch (id) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                    	finish();
                                        break;

                                    default:
                                        break;
                                }
                            }
                        });
                break;
            case R.id.location_btn:
                Utility.showNormalDialog(mThis,
                        getString(R.string.prompt), 
                        getString(R.string.erreport_location_confirm),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                switch (id) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                    	finish();
                                        break;

                                    default:
                                        break;
                                }
                            }
                        });
                break;
            case R.id.other_btn:
            	mStatus = OTHER_ERR;
            	refreshData();
            	break;
            	
            default:
                break;
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

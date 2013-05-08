/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.ui.BaseFragment;

public class HotelOrderCreditFragment extends BaseFragment implements View.OnClickListener{

    public HotelOrderCreditFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    static final String TAG = "HotelOrderCreditFragment";
    
    private ScrollView mCreditAssureScv;
    private Button mCreditBankBtn;
    private EditText mCreditCodeEdt;
    private EditText mCreditOwnerEdt;
    private EditText mCreditVerifyEdt;
    private Button mCreditValidityBtn;
    private Button mCreditCertTypeBtn;
    private EditText mCreditCertCodeEdt;
    private Button mCreditConfirmBtn;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //mActionTag = ActionLog.HotelOrderCredit;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_credit, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }

    
    public void onResume(){
        super.onResume();
    }
    
    public void onPause(){
        super.onPause();
    }
    
    protected void findViews() {
    	mCreditAssureScv = (ScrollView) mRootView.findViewById(R.id.credit_assure_scv);
    	mCreditBankBtn = (Button) mRootView.findViewById(R.id.credit_bank_btn);
    	mCreditCodeEdt = (EditText) mRootView.findViewById(R.id.credit_code_edt);
    	mCreditOwnerEdt = (EditText) mRootView.findViewById(R.id.credit_owner_edt);
    	mCreditVerifyEdt = (EditText) mRootView.findViewById(R.id.credit_verify_edt);
    	mCreditValidityBtn = (Button) mRootView.findViewById(R.id.credit_validity_btn);
    	mCreditCertTypeBtn = (Button) mRootView.findViewById(R.id.credit_cert_type_btn);
    	mCreditCertCodeEdt = (EditText) mRootView.findViewById(R.id.credit_cert_code_edt);
    	mCreditConfirmBtn = (Button) mRootView.findViewById(R.id.credit_confirm_btn);
    }

    protected void setListener() {
    	mCreditAssureScv.setOnClickListener(this);
    	mCreditBankBtn.setOnClickListener(this);
    	mCreditValidityBtn.setOnClickListener(this);
    	mCreditCertTypeBtn.setOnClickListener(this);
    	mCreditConfirmBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view){
        int id = view.getId();
        switch(id){
        case R.id.left_btn:
            break;
        case R.id.credit_bank_btn:
        	break;
        case R.id.credit_validity_btn:
        	break;
        case R.id.credit_cert_type_btn:
        	break;
        case R.id.credit_confirm_btn:
        	break;
        default:
            break;
        }
    }
    
}
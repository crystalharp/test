/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ValidateUtil;
import com.tigerknows.widget.StringArrayAdapter;

public class HotelOrderCreditFragment extends BaseFragment implements View.OnClickListener, ValidityListView.CallBack {

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
    private TextView mCreditAssurePriceTxv;
    private TextView mCreditNoteTxv;
    
    private Dialog mValidityDialog = null;
    private ValidityListView mValidityListView = null;
    
    private String mSumPrice;
    private Calendar mDate = null;
    private String mOrderModifyDeadline;
    private List<String> mBankList;
    
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
        mCreditAssurePriceTxv.setText(mSphinx.getString(R.string.credit_assure_price, mSumPrice));
        mCreditNoteTxv.setText(mSphinx.getString(R.string.credit_note, mOrderModifyDeadline));
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
    	mCreditAssurePriceTxv = (TextView) mRootView.findViewById(R.id.credit_assure_price_txv);
    	mCreditNoteTxv = (TextView) mRootView.findViewById(R.id.credit_note_txv);
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
        	showCreditBankDialog();
        	break;
        case R.id.credit_validity_btn:
        	showValidDialog();
        	break;
        case R.id.credit_cert_type_btn:
        	showCertTypeDialog();
        	break;
        case R.id.credit_confirm_btn:
        	String str = mCreditBankBtn.getText().toString();
        	List<String> list = new ArrayList<String>();
        	if(TextUtils.isEmpty(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_bank_empty_tip));
        		return;
        	}
        	
        	str = mCreditCodeEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_code_empty_tip));
        		return;
        	}else if(!ValidateUtil.isValidCreditCard(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_code_format));
        		return;
        	}
        	list.add(str);
        	
        	str = mCreditOwnerEdt.getText().toString().trim();
        	if(TextUtils.isEmpty(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_owner_empty_tip));
        		return;
        	}else if(!ValidateUtil.isValidElongName(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_person_name_format));
        		return;
        	}
        	list.add(str);
        	
        	str = mCreditVerifyEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_verify_empty_tip));
        		return;
        	}else if(!ValidateUtil.isValidCreditCardVerify(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_verify_format));
        		return;
        	}
        	list.add(str);
        	if(mDate == null){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_validity_empty_tip));
        		return;
        	}
        	list.add(mDate.get(Calendar.YEAR) + "");
        	list.add(1+mDate.get(Calendar.MONTH) + "");
        	LogWrapper.d("Trap", CalendarUtil.ym6h.format(mDate.getTime()));
        	str = mCreditCertTypeBtn.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_cert_type_empty_tip));
        		return;
        	}
        	list.add(str);
        	str = mCreditCertCodeEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_cert_code_empty_tip));
        		return;
        	}else if(!ValidateUtil.isValidCertCode(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_certcard_number_format));
        		return;
        	}
        	list.add(str);
        	mSphinx.getHotelOrderWriteFragment().setCredit(list);
        	break;
        default:
            break;
        }
    }
    public void showCreditBankDialog(){
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, mBankList);
        ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        //TODO: ActionLog
        final Dialog dialog = Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.choose_credit_bank), null, listView, null, null, null);
        listView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
        		mCreditBankBtn.setText(mBankList.get(which));
        		mCreditBankBtn.setTextColor(Color.BLACK);
        		dialog.dismiss();
        	}
		});
    }

    public void showValidDialog() {
        if (mValidityListView == null) {
            mValidityListView = new ValidityListView(mSphinx);
        }
        
        if (mValidityDialog == null) {
            mValidityDialog = Utility.showNormalDialog(mSphinx, "title", null, mValidityListView, null, null, null);
            mValidityDialog.setCancelable(true);
            mValidityDialog.setCanceledOnTouchOutside(false);
        }else if(mValidityDialog.isShowing() == false){
        	mValidityDialog.show();
        }
        
        mValidityListView.setData(mDate, this, mActionTag);
    }
    
    public void showCertTypeDialog(){
    	final List<String> list = new ArrayList<String>();
    	list.add("身份证");
    	list.add("护照");
    	list.add("其他");
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list);
        ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        //TODO: ActionLog
        final Dialog dialog = Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.choose_cert_type), null, listView, null, null, null);
        listView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
        		mCreditCertTypeBtn.setText(list.get(which));
        		mCreditCertTypeBtn.setTextColor(Color.BLACK);
        		dialog.dismiss();
        	}
		});
    }
	public void setData(String sumPrice, String additionMessage) {
		mSumPrice = sumPrice;
		String[] sArray = additionMessage.split("#");
		mOrderModifyDeadline = sArray[0];
		mBankList = new ArrayList<String>();
		for (int i = 1; i < sArray.length; i++){
			mBankList.add(sArray[i]);
		}
		if(mBankList.isEmpty()){
			mBankList.add("服务器错误：银行列表为空");
		}
	}

    @Override
    public void selected(Calendar calendar) {
        // TODO 设置选择的日期
    	mDate = Calendar.getInstance();
    	mDate.setTime(calendar.getTime());
    	mCreditValidityBtn.setText(CalendarUtil.y4mc.format(mDate.getTime()));
    	mCreditValidityBtn.setTextColor(Color.BLACK);
        if (mValidityDialog != null && mValidityDialog.isShowing()) {
            mValidityDialog.dismiss();
        }
    }
    
}
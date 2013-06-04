/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ValidateUtil;
import com.tigerknows.widget.SingleChoiceArrayAdapter;

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
    private List<String> mCertTypeList;
    
    private int mGetBankPosition;
    private int mGetCreditCertPosition;
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
        mTitleBtn.setText(mSphinx.getString(R.string.credit_assure_title));
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
    
    private void showCreditErrorDialog(String message, final View source){
    	Utility.showNormalDialog(mSphinx, 
    			mSphinx.getString(R.string.prompt), 
    			message, 
    			mSphinx.getString(R.string.confirm),
    			null,
    			new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						source.requestFocus();
						mSphinx.showSoftInput(source);
					}
    		});
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
        	// 判断选择银行
        	if(TextUtils.isEmpty(str) || TextUtils.equals(str, mSphinx.getString(R.string.credit_bank_hint))){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_bank_empty_tip));
        		return;
        	}
        	
        	// 判断银行卡号
        	str = mCreditCodeEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.credit_code_empty_tip), mCreditCodeEdt);
        		return;
        	}else if(!ValidateUtil.isValidCreditCard(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.credit_code_format), mCreditCodeEdt);
        		return;
        	}
        	list.add(str);
        	
        	// 判断持卡人姓名
        	str = mCreditOwnerEdt.getText().toString().trim();
        	if(TextUtils.isEmpty(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.credit_owner_empty_tip), mCreditOwnerEdt);
        		return;
        	}else if(!ValidateUtil.isValidElongName(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.hotel_person_name_format), mCreditOwnerEdt);
        		return;
        	}
        	list.add(str);
        	
        	// 判断信用卡验证码
        	str = mCreditVerifyEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.credit_verify_empty_tip), mCreditVerifyEdt);
        		return;
        	}else if(!ValidateUtil.isValidCreditCardVerify(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.credit_verify_format), mCreditVerifyEdt);
        		return;
        	}
        	list.add(str);
        	
        	// 判断信用卡有效期
        	if(mDate == null){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.credit_validity_empty_tip));
        		return;
        	}
        	list.add(mDate.get(Calendar.YEAR) + "");
        	list.add(1+mDate.get(Calendar.MONTH) + "");
        	
        	// 判断证件类型(这个不用判断)
        	str = mCreditCertTypeBtn.getText().toString();
        	list.add(str);
        	
        	// 判断证件号码
        	str = mCreditCertCodeEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.credit_cert_code_empty_tip), mCreditCertCodeEdt);
        		return;
        	}else if(!ValidateUtil.isValidCertCode(str)){
        		showCreditErrorDialog(mSphinx.getString(R.string.hotel_certcard_number_format), mCreditCertCodeEdt);
        		return;
        	}
        	list.add(str);
        	
        	mSphinx.getHotelOrderWriteFragment().setCredit(list);
        	break;
        default:
            break;
        }
    }
    
    public class CreditBankAdapter extends SingleChoiceArrayAdapter{
    	
		public CreditBankAdapter(Context context, List<String> list) {
			super(context, list);
			// TODO Auto-generated constructor stub
			// 招商银行 中国建设银行 中国工商银行 中国银行 交通银行 中信银行 广发银行 中国民生银行 兴业银行 上海浦东发展银行 中国光大银行     中国农业银行 平安银行 深圳发展银行 北京银行 上海银行 华夏银行 中国邮政储蓄银行 宁波银行
		}
		@Override
		public View getView(final int position, View convertView, ViewGroup parent){
			View view = super.getView(position, convertView, parent);
			ImageView singleIconImv = (ImageView)view.findViewById(R.id.single_icon_imv);
			if(position == (int)mGetBankPosition){
				singleIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_checked));
			}else{
				singleIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_default));
			}
			return view;
		}
		
    }
    
    public class CertTypeAdapter extends SingleChoiceArrayAdapter{
    	
		public CertTypeAdapter(Context context, List<String> list) {
			super(context, list);
			// TODO Auto-generated constructor stub
		}
		@Override
		public View getView(final int position, View convertView, ViewGroup parent){
			View view = super.getView(position, convertView, parent);
			ImageView singleIconImv = (ImageView)view.findViewById(R.id.single_icon_imv);
			if(position == (int)mGetCreditCertPosition){
				singleIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_checked));
			}else{
				singleIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_default));
			}
			return view;
		}
    }
    
    public void showCreditBankDialog(){
        final ArrayAdapter<String> adapter = new CreditBankAdapter(mSphinx, mBankList);
        final ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        //TODO: ActionLog
        final Dialog dialog = Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.choose_credit_bank), null, listView, null, null, null);
        listView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
            	listView.setAdapter(adapter);
        		mGetBankPosition = which;
        		mCreditBankBtn.setText(mBankList.get(which));
        		mCreditBankBtn.setTextColor(getResources().getColor(R.color.black_dark));
        		dialog.dismiss();
        	}
		});
    }

    public void showValidDialog() {
        if (mValidityListView == null) {
            mValidityListView = new ValidityListView(mSphinx);
        }
        
        if (mValidityDialog == null) {
            mValidityDialog = Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.choose_credit_validity), null, mValidityListView, null, null, null);
            mValidityDialog.setCancelable(true);
            mValidityDialog.setCanceledOnTouchOutside(false);
        }else if(mValidityDialog.isShowing() == false){
        	mValidityDialog.show();
        }
        
        mValidityListView.setData(mDate, this, mActionTag);
    }
    
    public void showCertTypeDialog(){
    	final List<String> list = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.cert_type)));
        final ArrayAdapter<String> adapter = new CertTypeAdapter(mSphinx, list);
        final ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        //TODO: ActionLog
        final Dialog dialog = Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.choose_cert_type), null, listView, null, null, null);
        listView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
            	listView.setAdapter(adapter);
        		mGetCreditCertPosition = which;
        		mCreditCertTypeBtn.setText(list.get(which));
        		mCreditCertTypeBtn.setTextColor(getResources().getColor(R.color.black_dark));
        		dialog.dismiss();
        	}
		});
    }
	public void setData(String additionMessage, String oneNightPrice, String sumPrice, int assureType, String name) {
		mSumPrice = sumPrice;
		int assureTypeFromServer = 0;
		String[] sArray = additionMessage.split("#");
		if(sArray[0].length() < 2){
			assureType = Integer.parseInt(sArray[0]);
			assureTypeFromServer = 1;
		}
		mOrderModifyDeadline = sArray[assureTypeFromServer];
		mBankList = new ArrayList<String>();
		for (int i = assureTypeFromServer + 1; i < sArray.length; i++){
			mBankList.add(sArray[i]);
		}
		if(mBankList.isEmpty()){
			mBankList.add("服务器错误：银行列表为空");
		}
		mGetBankPosition = -1;
		mCertTypeList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.cert_type)));
		mCreditCertTypeBtn.setText(mCertTypeList.get(0));
		mGetCreditCertPosition = 0;
        mCreditAssurePriceTxv.setText(mSphinx.getString(R.string.credit_assure_price, (assureType == 2 ) ? mSumPrice : oneNightPrice));
        Calendar c1 = Calendar.getInstance();
        try {
			c1.setTime(CalendarUtil.ymd8c_hm4.parse(mOrderModifyDeadline));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			// do nothing
		}
        Calendar c2 = Calendar.getInstance();
        c2.set(2013, 0, 1);
        if(c1.after(c2) == false){
        	mCreditNoteTxv.setText(Utility.renderColorToPartOfString(mContext,
        			R.color.orange,
        			mSphinx.getString(R.string.credit_note_detail_2, mSphinx.getString(R.string.credit_cannot_cancel)),
        			mSphinx.getString(R.string.credit_cannot_cancel)));
        }else{
        	mCreditNoteTxv.setText(Utility.renderColorToPartOfString(mContext,
        			R.color.orange,
        			mSphinx.getString(R.string.credit_note_detail, mOrderModifyDeadline).trim(),
        			mOrderModifyDeadline));
        }
        mCreditOwnerEdt.setText(name);
	}

    @Override
    public void selected(Calendar calendar) {
        // TODO 设置选择的日期
    	mDate = Calendar.getInstance();
    	mDate.setTime(calendar.getTime());
    	mCreditValidityBtn.setText(CalendarUtil.y4mc.format(mDate.getTime()));
    	mCreditValidityBtn.setTextColor(getResources().getColor(R.color.black_dark));
        if (mValidityDialog != null && mValidityDialog.isShowing()) {
            mValidityDialog.dismiss();
        }
    }
    
}
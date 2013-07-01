package com.tigerknows.view.user;

import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;

import com.decarta.Globals;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.CommonUtils;

public class UserUpdatePhoneActivity extends UserBaseActivity {

	private ExtValidationEditText phoneEdt;
	
	private ExtValidationEditText valiNumEdt;
	
	private CountDownButton valiNumBtn;

	private ExtValidationEditText passwordEdt;
	
	private Button confirmBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mActionTag = ActionLog.UserUpdatePhone;
		mId = R.id.activity_user_update_phone;
		setContentView(R.layout.user_update_phone);
        findViews();
        setListener();
        
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(getString(R.string.title_update_phone));
	}

	protected void findViews() {
		super.findViews();

		phoneEdt = (ExtValidationEditText)findViewById(R.id.phone_edt);
		valiNumEdt = (ExtValidationEditText)findViewById(R.id.vali_num_edt);
		valiNumBtn = (CountDownButton)findViewById(R.id.vali_num_btn);
		passwordEdt = (ExtValidationEditText)findViewById(R.id.password_edt);
		confirmBtn = (Button)findViewById(R.id.confirm_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		valiNumBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 获取验证码
				mActionLog.addAction(mActionTag +  ActionLog.UserCommonValidNumBtn);
				if (!phoneEdt.isValid()) {
					validationAction(phoneEdt);
					return;
				}
				valiNumBtn.startCountDown();
				requestValiNum();
			}
		});
		
		confirmBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 成功后, 跳转到个人帐户页面
                mActionLog.addAction(mActionTag +  ActionLog.UserCommonConfirmBtn);
				if (!mForm.isValid()) {
					validationAction(mForm.getErrorSource());
					return;
				}
				
				requestChangePhone();
			}
		});
		
		phoneEdt.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				//  [And4.30-206] 【更换手机号、重置密码、注册界面】点击获取验证码后，倒计时至少于30秒时，
				//  重新输入原手机号最后一位，点击获取验证码，可收到验证码
				// valiNumBtn.reset(getString(R.string.reqest_validate_num));
			}
		});
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		valiNumBtn.reset(getString(R.string.reqest_validate_num));
	}
	
	/**
	 * 请求验证码.
	 * ResponseCode: 200, 400
	 */
	private void requestValiNum() {
		AccountManage accountManage = new AccountManage(this);

		String phone = phoneEdt.getText().toString().trim();
		
		Hashtable<String, String> criteria = new Hashtable<String, String>();
		criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_BIND_TELEPHONE);
		criteria.put(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		
		sendRequest(accountManage, criteria);
	}
	
	/**
	 * 发出注册请求
	 * ResponseCode: 200, 300, 301, 400, 402, 500
	 */
	private void requestChangePhone() {

		AccountManage accountManage = new AccountManage(this);

		if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
		
			String phone = phoneEdt.getText().toString().trim();
			String valiNum = valiNumEdt.getText().toString().trim();
			String password = passwordEdt.getText().toString().trim();
			
			Hashtable<String, String> criteria = new Hashtable<String, String>();
			criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_UPDATE_TELEPHONE);
			criteria.put(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
			criteria.put(AccountManage.SERVER_PARAMETER_VALIDATE_CODE, valiNum);
			criteria.put(AccountManage.SERVER_PARAMETER_PASSWORD, CommonUtils.encryptWithSHA1(password));
			criteria.put(BaseQuery.SERVER_PARAMETER_SESSION_ID, Globals.g_Session_Id);
			
			sendRequest(accountManage, criteria);
		} 
		
	}

	private void validationAction(final ExtValidationEditText source) {
		CommonUtils.showNormalDialog(UserUpdatePhoneActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					if (source.validErrorType == ExtValidationEditText.ValidErrorType.PhoneFormatError) {
						source.selectAll();
					} else if (source.validErrorType == ExtValidationEditText.ValidErrorType.PasswordFormatError) {
                        if (source.hasUnionView()) {
                            source.getUnionView().setText("");
                        }
                        source.setText("");
                    }

					source.requestFocus();
					showSoftInput(source);
				}
			}
		});
	}

	@Override
	protected void responseCodeAction(AccountManage accountManage) {
		
		String operationCode = accountManage.getCriteria().get(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
		if (AccountManage.OPERATION_CODE_BIND_TELEPHONE.equals(operationCode)){
			// 200, 400, 503
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				showToast(R.string.request_validcode_success);
				break;
			case Response.RESPONSE_CODE_MOBILE_PHONE_EXIST:
				dealWith400();
				break;
			case Response.RESPONSE_CODE_SEND_SMS_FAILED:
				dealWith503();
				break;
			default:
			}
		} else if (AccountManage.OPERATION_CODE_UPDATE_TELEPHONE.equals(operationCode)) {
			//  200, (400, )402, 404
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				showToast(R.string.update_phone_success);
				Globals.clearSessionAndUser(this);
				TKConfig.setPref(UserUpdatePhoneActivity.this, TKConfig.PREFS_PHONENUM, "");
				startActivityForResult(new Intent(UserUpdatePhoneActivity.this, UserLoginActivity.class), 0);
				break;
			case Response.RESPONSE_CODE_MOBILE_PHONE_EXIST:
				dealWith400();
				break;
			case Response.RESPONSE_CODE_SECURITY_CODE_ERROR:
				dealWith402();
				break;
			case Response.RESPONSE_CODE_PASWORD_ERROR:
				dealWith404();
				break;
			default:
			}
		}
		
	}
	
	private void dealWith400() {
		CommonUtils.showNormalDialog(UserUpdatePhoneActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_400), 
                getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					phoneEdt.selectAll();
					phoneEdt.requestFocus();
					showSoftInput(phoneEdt);
				}
				valiNumBtn.reset(getString(R.string.reqest_validate_num));
			}
			
		});
	}
	
	private void dealWith402() {
		CommonUtils.showNormalDialog(UserUpdatePhoneActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_402), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumEdt.selectAll();
					valiNumEdt.requestFocus();
					showSoftInput(valiNumEdt);
				}
				//valiNumBtn.reset(getString(R.string.request_validcode_retry));
			}
			
		});
	}
	
	private void dealWith404() {
		CommonUtils.showNormalDialog(UserUpdatePhoneActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_404), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					passwordEdt.setText("");
					passwordEdt.requestFocus();
					showSoftInput(passwordEdt);
				}
			}
			
		});
	}
	
	private void dealWith503() {
		CommonUtils.showNormalDialog(UserUpdatePhoneActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_503), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
//				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumBtn.reset(getString(R.string.request_validcode_retry));
//				}
			}
			
		});
	}
	
	@Override
	protected void responseBadNetwork() {
		// TODO Auto-generated method stub
		super.responseBadNetwork();
		valiNumBtn.reset(getString(R.string.reqest_validate_num));
	}
}

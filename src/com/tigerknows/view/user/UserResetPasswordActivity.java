package com.tigerknows.view.user;

import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import com.decarta.Globals;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.CommonUtils;

public class UserResetPasswordActivity extends UserBaseActivity {

	private ExtValidationEditText phoneEdt;
	
	private ExtValidationEditText valiNumEdt;
	
	private CountDownButton valiNumBtn;

	private ExtValidationEditText passwordEdt;

//	private ExtValidationEditText rePasswordEdt;
	
	private Button confirmBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mActionTag = ActionLog.UserResetPassword;
		mId = R.id.activity_user_reset_password;
		setContentView(R.layout.user_reset_password);
        findViews();
        setListener();
        
        mRightTxv.setVisibility(View.GONE);
        mTitleTxv.setText(getString(R.string.title_reset_password));
	}

	protected void findViews() {
		super.findViews();

		phoneEdt = (ExtValidationEditText)findViewById(R.id.phone_edt);
		valiNumEdt = (ExtValidationEditText)findViewById(R.id.vali_num_edt);
		valiNumBtn = (CountDownButton)findViewById(R.id.vali_num_btn);
		passwordEdt = (ExtValidationEditText)findViewById(R.id.password_edt);
//		rePasswordEdt = (ExtValidationEditText)findViewById(R.id.re_password_edt);
		confirmBtn = (Button)findViewById(R.id.confirm_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		valiNumBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 获取验证码
				mActionLog.addAction(ActionLog.UserResetPasswordValidNumBtn);
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
				// TODO 重置成功后, 跳转到登录页面? 个人中心页面?
				if (!mForm.isValid()) {
					validationAction(mForm.getErrorSource());
					return;
				}
				
				requestResetPasswrod();
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
				valiNumBtn.reset(getString(R.string.reqest_validate_num));
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
		criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_GET_VALIDATE_CODE);
		criteria.put(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		
		sendRequest(accountManage, criteria);
	}
	
	/**
	 * 发出注册请求
	 * ResponseCode: 200, 402
	 */
	private void requestResetPasswrod() {

		AccountManage accountManager = new AccountManage(this);

		String phone = phoneEdt.getText().toString().trim();
		String password = passwordEdt.getText().toString().trim();
		String valiNum = valiNumEdt.getText().toString().trim();
		
		Hashtable<String, String> criteria = new Hashtable<String, String>();
		criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_RESET_PASSWORD);
		criteria.put(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		criteria.put(AccountManage.SERVER_PARAMETER_PASSWORD, CommonUtils.encryptWithSHA1(password));
		criteria.put(AccountManage.SERVER_PARAMETER_VALIDATE_CODE, valiNum);
		
		sendRequest(accountManager, criteria);
	}
	
	private void validationAction(final ExtValidationEditText source) {
		CommonUtils.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), source.msg, 
				new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					if (source.validErrorType == ExtValidationEditText.ValidErrorType.PhoneFormatError) {
						source.selectAll();
					} else if (source.validErrorType == ExtValidationEditText.ValidErrorType.PasswordFormatError
							|| source.validErrorType == ExtValidationEditText.ValidErrorType.UnionDifferentError) {
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
		// TODO Auto-generated method stub
		String operationCode = accountManage.getCriteria().get(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
		if (AccountManage.OPERATION_CODE_GET_VALIDATE_CODE.equals(operationCode)){
			// 200, 403, 503
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				showToast(R.string.request_validcode_success);
				break;
			case Response.RESPONSE_CODE_NO_MOBILE_PHONE:
				dealWith403();
				break;
			case Response.RESPONSE_CODE_SEND_SMS_FAILED:
				dealWith503();
				break;
			default:
			}
		} else if (AccountManage.OPERATION_CODE_RESET_PASSWORD.equals(operationCode)) {
			// 200, 402, (403, 500)
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				showToast(R.string.reset_password_success);
				
				Globals.clearSessionAndUser(this);
				
				Intent intent = new Intent(UserResetPasswordActivity.this, UserLoginActivity.class);
		        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        startActivityForResult(intent, 0);
				
				break;
			case Response.RESPONSE_CODE_NO_MOBILE_PHONE:
				dealWith403();
			case Response.RESPONSE_CODE_SECURITY_CODE_ERROR:
				dealWith402();
				break;
			default:
			}
		}
	}
	
	private void dealWith402() {
		CommonUtils.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_402), 
				new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumEdt.requestFocus();
					showSoftInput(valiNumEdt);
					valiNumEdt.selectAll();
				}
				valiNumBtn.reset(getString(R.string.request_validcode_retry));
			}
			
		});
	}
	
	private void dealWith403() {
		CommonUtils.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_403_resetpassword), 
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
	
	private void dealWith503() {
		CommonUtils.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_503), 
				new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
//				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumBtn.reset(getString(R.string.reqest_validate_num));
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

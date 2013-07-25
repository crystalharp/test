package com.tigerknows.ui.user;

import java.util.Hashtable;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.Utility;

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
        
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(getString(R.string.title_reset_password));
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
			    mActionLog.addAction(mActionTag +  ActionLog.UserCommonConfirmBtn);
				// TODO 重置成功后, 跳转到登录页面? 个人中心页面?
				if (!mForm.isValid()) {
					validationAction(mForm.getErrorSource());
					return;
				}
				
				requestResetPasswrod();
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
		
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_GET_VALIDATE_CODE);
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		
		sendRequest(accountManage);
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
		
		accountManager.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_RESET_PASSWORD);
		accountManager.addParameter(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		accountManager.addParameter(AccountManage.SERVER_PARAMETER_PASSWORD, Utility.encryptWithSHA1(password));
		accountManager.addParameter(AccountManage.SERVER_PARAMETER_VALIDATE_CODE, valiNum);
		
		sendRequest(accountManager);
	}
	
	private void validationAction(final ExtValidationEditText source) {
		Utility.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
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
		String operationCode = accountManage.getParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
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
		Utility.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_402), 
		        getString(R.string.confirm),
                null,
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
				//valiNumBtn.reset(getString(R.string.request_validcode_retry));
			}
			
		});
	}
	
	private void dealWith403() {
		Dialog dlg=Utility.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_403_resetpassword), 
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
		dlg.setOnDismissListener(new OnDismissListener(){
			@Override
			public void onDismiss(DialogInterface dialog) {
				valiNumBtn.reset(getString(R.string.reqest_validate_num));
			}
		});
	}
	
	private void dealWith503() {
		Utility.showNormalDialog(UserResetPasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_503), 
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

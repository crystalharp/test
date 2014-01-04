package com.tigerknows.ui.user;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.Utility;

public class UserUpdatePasswordActivity extends UserBaseActivity {

	private EditText oldPasswordEdt;
	
	private EditText newPasswordEdt;

	private TextView forgetPasswordTxt;
	
	private Button confirmBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mActionTag = ActionLog.UserUpdatePassword;
		mId = R.id.activity_user_update_password;
		setContentView(R.layout.user_update_password);
        findViews();
        setListener();
        
        mTitleBtn.setText(getString(R.string.title_update_password));
	}

	protected void findViews() {
		super.findViews();
		
		oldPasswordEdt = (EditText)findViewById(R.id.old_password_edt);
		newPasswordEdt = (EditText)findViewById(R.id.new_password_edt);
		forgetPasswordTxt = (TextView)findViewById(R.id.forget_password_txt);
		confirmBtn = (Button)findViewById(R.id.confirm_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		forgetPasswordTxt.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// 跳转到重置密码页面
				mActionLog.addAction(mActionTag +  ActionLog.UserCommonForgetPasswordBtn);
				startActivity(new Intent(UserUpdatePasswordActivity.this, UserResetPasswordActivity.class));
			}
		});
		
		confirmBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// 发出请求, 若成功中转到帐户管理页面
			    mActionLog.addAction(mActionTag +  ActionLog.UserCommonConfirmBtn);
				if (!mForm.isValid()) {
					doAction(mForm.getErrorSource());
					return;
				}
				
				requestChangePassword();
			}
		});
	}

	/**
	 * 发出注册请求
	 * ResponseCode: 200, 300, 301, 400, 402, 500
	 */
	private void requestChangePassword() {

		AccountManage accountManage = new AccountManage(this);

		String oldPassword = oldPasswordEdt.getText().toString().trim();
		String newPassword = newPasswordEdt.getText().toString().trim();
		
		if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
			accountManage.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_UPDATE_PASSWORD);
			accountManage.addParameter(AccountManage.SERVER_PARAMETER_OLD_PASSWORD, Utility.encryptWithSHA1(oldPassword));
			accountManage.addParameter(AccountManage.SERVER_PARAMETER_PASSWORD, Utility.encryptWithSHA1(newPassword));
			accountManage.addParameter(BaseQuery.SERVER_PARAMETER_SESSION_ID, Globals.g_Session_Id);
			
			sendRequest(accountManage);
		} 
	}
	
	private void doAction(final ExtValidationEditText source) {
		Utility.showNormalDialog(UserUpdatePasswordActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					if (source.validErrorType == ExtValidationEditText.ValidErrorType.PasswordFormatError
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
		String operationCode = accountManage.getParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
		if (AccountManage.OPERATION_CODE_UPDATE_PASSWORD.equals(operationCode)) {
			//  200, 405
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				showToast(R.string.update_password_success);
				finish();
				break;
			case Response.RESPONSE_CODE_ORIGINAL_PASSWORD_ERROR:
				dealWith405();
				break;
			default:
			}
		}
	}
	
	private void dealWith405() {
		Utility.showNormalDialog(UserUpdatePasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_405), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					oldPasswordEdt.setText("");
					oldPasswordEdt.clearFocus();
					mHandler.postDelayed(mShowSoftInput, 512);
				}
			}
			
		});
	}
    
    private Runnable mShowSoftInput = new Runnable() {
        
        @Override
        public void run() {
            oldPasswordEdt.requestFocus();
            showSoftInput(oldPasswordEdt);
        }
    };
}

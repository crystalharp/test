package com.tigerknows.view.user;

import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.CommonUtils;

public class UserUpdatePasswordActivity extends UserBaseActivity {

	private EditText oldPasswordEdt;
	
	private EditText newPasswordEdt;

	private EditText newRePasswordEdt;
	
	private TextView forgetPasswordTxt;
	
	private Button confirmBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mActionTag = ActionLog.UserUpdatePassword;
		mId = R.id.activity_user_update_password;
		setContentView(R.layout.user_update_password);
        findViews();
        setListener();
        
        mRightTxv.setVisibility(View.GONE);
        mTitleTxv.setText(getString(R.string.title_update_password));
	}

	protected void findViews() {
		super.findViews();
		
		oldPasswordEdt = (EditText)findViewById(R.id.old_password_edt);
		newPasswordEdt = (EditText)findViewById(R.id.new_password_edt);
		newRePasswordEdt = (EditText)findViewById(R.id.new_re_password_edt);
		forgetPasswordTxt = (TextView)findViewById(R.id.forget_password_txt);
		confirmBtn = (Button)findViewById(R.id.confirm_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		oldPasswordEdt.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mActionLog.addAction(ActionLog.UserClickPasswordEdt);
				}
				
				return false;
			}
		});
		
		forgetPasswordTxt.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 跳转到重置密码页面
				mActionLog.addAction(ActionLog.UserUpdatePasswordForgetPassword);
				startActivity(new Intent(UserUpdatePasswordActivity.this, UserResetPasswordActivity.class));
			}
		});
		
		confirmBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 发出请求, 若成功中转到帐户管理页面
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
			Hashtable<String, String> criteria = new Hashtable<String, String>();
			criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_UPDATE_PASSWORD);
			criteria.put(AccountManage.SERVER_PARAMETER_OLD_PASSWORD, CommonUtils.encryptWithSHA1(oldPassword));
			criteria.put(AccountManage.SERVER_PARAMETER_PASSWORD, CommonUtils.encryptWithSHA1(newPassword));
			criteria.put(BaseQuery.SERVER_PARAMETER_SESSION_ID, Globals.g_Session_Id);
			
			sendRequest(accountManage, criteria);
		} 
	}
	
	private void doAction(final ExtValidationEditText source) {
		CommonUtils.showNormalDialog(UserUpdatePasswordActivity.this, getString(R.string.title_error_tip), source.msg, 
				new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
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
		String operationCode = accountManage.getCriteria().get(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
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
		CommonUtils.showNormalDialog(UserUpdatePasswordActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_405), 
				new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
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

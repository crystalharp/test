package com.tigerknows.view.user;

import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.decarta.Globals;
import com.tigerknows.ActionLog;
import com.tigerknows.POIComment;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.CommonUtils;

public class UserRegistActivity extends UserBaseActivity {

	private ExtValidationEditText phoneEdt;
	
	private EditText valiNumEdt;
	
	private CountDownButton valiNumBtn;

	private ExtValidationEditText passwordEdt;

	private Button registBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		mActionTag = ActionLog.UserRegist;
		mId = R.id.activity_user_regist;
		setContentView(R.layout.user_regist);
        findViews();
        setListener();
        
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(getString(R.string.title_user_regist));
	}

	protected void findViews() {
		super.findViews();

		phoneEdt = (ExtValidationEditText)findViewById(R.id.phone_edt);
		valiNumEdt = (EditText)findViewById(R.id.vali_num_edt);
		valiNumBtn = (CountDownButton)findViewById(R.id.vali_num_btn);
		passwordEdt = (ExtValidationEditText)findViewById(R.id.password_edt);
		registBtn = (Button)findViewById(R.id.regist_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		valiNumBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 获取验证码
				mActionLog.addAction(ActionLog.UserRegistValidNumBtn);
				if (!phoneEdt.isValid()) {
					validationAction(phoneEdt);
					return;
				}
					
				valiNumBtn.startCountDown();
				requestValiNum();
			}
		});
		
		registBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 注册成功后, 跳转到输入昵称页面
				if (!mForm.isValid()) {
					validationAction(mForm.getErrorSource());
					return;
				}
				
				requestRegist();
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
			}
		});
	}
	
	@Override
	public Intent getIntent() {
		// TODO Auto-generated method stub
		return super.getIntent();
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		valiNumBtn.reset(getString(R.string.reqest_validate_num));
	}

	/**
	 * 请求验证码.
	 * ResponseCode: 200, 400, 503
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
	 * ResponseCode: 200, 402
	 */
	private void requestRegist() {

		AccountManage accountManage = new AccountManage(this);

		String phone = phoneEdt.getText().toString().trim();
		String password = passwordEdt.getText().toString().trim();
		String valiNum = valiNumEdt.getText().toString().trim();
		
		Hashtable<String, String> criteria = new Hashtable<String, String>();
		criteria.put(AccountManage.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_CREATE);
		criteria.put(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		criteria.put(AccountManage.SERVER_PARAMETER_PASSWORD, CommonUtils.encryptWithSHA1(password));
		criteria.put(AccountManage.SERVER_PARAMETER_VALIDATE_CODE, valiNum);
		
		sendRequest(accountManage, criteria);
	}
	
	private void validationAction(final ExtValidationEditText source) {
		CommonUtils.showNormalDialog(UserRegistActivity.this, getString(R.string.title_error_tip), source.msg, 
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

	private void onSuccess() {
        mActionLog.addAction(ActionLog.UserRegisterSuccess);
		showToast(R.string.regist_success);
		if (mSourceViewIdLogin != R.id.activity_poi_comment) {
    		Intent intent = new Intent(UserRegistActivity.this, UserUpdateNickNameActivity.class);
    		startActivityForResult(intent, 0);
		} else {
            Intent intent = new Intent(UserRegistActivity.this, POIComment.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
		}
		
		finish();
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
		} else if (AccountManage.OPERATION_CODE_CREATE.equals(operationCode)) {
			// 200, (400, 401,) 402, 500
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				UserRespnose userResponse = (UserRespnose)accountManage.getResponse();
				Globals.storeSessionAndUser(mThis, userResponse);
				
				// 注册成功后记录用户曾经输入的手机号，以便上次显示
				TKConfig.setPref(UserRegistActivity.this, TKConfig.PREFS_PHONENUM, phoneEdt.getText().toString().trim());
				onSuccess();
				break;
			case Response.RESPONSE_CODE_MOBILE_PHONE_EXIST:
				dealWith400();
                break;
			case Response.RESPONSE_CODE_SECURITY_CODE_ERROR:
				dealWith402();
				break;
			default:
			}
		} 
		
	}
	
	private void dealWith400() {
		CommonUtils.showNormalDialog(UserRegistActivity.this, 
		        getString(R.string.title_error_tip), 
				getString(R.string.response_code_400), 
				getString(R.string.confirm),
				null,
				new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					if (mSourceViewIdLogin == R.id.activity_poi_comment) {
						// 点评-点评后登录页-注册
						// 点评-点评后登录页-登录-注册
						Intent intent = new Intent(UserRegistActivity.this, UserLoginActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra(TKConfig.PREFS_PHONENUM, phoneEdt.getText().toString().trim());
						startActivityForResult(intent, 0);
						finish();
					} else {
						// 更多-登录-注册
						Intent intent = new Intent();
						intent.putExtra(TKConfig.PREFS_PHONENUM, phoneEdt.getText().toString().trim());
						setResult(RESULT_OK, intent);
						finish();
					}
				} else {
					valiNumBtn.reset(getString(R.string.reqest_validate_num));
				}
			}
			
		});
	}
	
	private void dealWith402() {
		CommonUtils.showNormalDialog(UserRegistActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_402), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumEdt.requestFocus();
					valiNumEdt.selectAll();
					showSoftInput(valiNumEdt);
				}
				valiNumBtn.reset(getString(R.string.request_validcode_retry));
			}
			
		});
	}
	
	private void dealWith503() {
		CommonUtils.showNormalDialog(UserRegistActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_503), 
		        getString(R.string.confirm),
                null,
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

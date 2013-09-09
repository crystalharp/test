package com.tigerknows.ui.user;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.util.Utility;

public class UserLoginRegistActivity extends UserBaseActivity implements View.OnClickListener {

	private ExtValidationEditText loginPhoneEdt;
	private EditText valiNumEdt;
	private CountDownButton valiNumBtn;
	private ExtValidationEditText loginPasswordEdt;
	private Button titleRegistBtn;
	private Button titleLoginBtn;

	public static final int REQUEST_CODE_REGIST = 1;
	private ExtValidationEditText registPhoneEdt;
	private ExtValidationEditText registPasswordEdt;
	private TextView forgetPasswordTxt;
	private Button loginBtn;
	private Button registBtn;
	
	private ScrollView loginScv;
	private ScrollView registScv;
	
	private boolean isRegist;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mId = R.id.activity_user_login_regist;
		setContentView(R.layout.user_login_regist);
		findViews();
		setListener();
		changeMode(true);
		setPrefsPhoneNum(TKConfig.getPref(mThis, TKConfig.PREFS_PHONENUM, ""));
	}

	private void setPrefsPhoneNum(String prefsPhoneNum) {
		if (!TextUtils.isEmpty(prefsPhoneNum)) {
            loginPhoneEdt.setText(prefsPhoneNum);
            loginPhoneEdt.setSelection(loginPhoneEdt.getText().toString().length());
        }
	}	
	
	@Override
	protected void findViews() {
		super.findViews();
		loginScv = (ScrollView)findViewById(R.id.login_scv);
		registScv = (ScrollView)findViewById(R.id.regist_scv);
		registPhoneEdt = (ExtValidationEditText)findViewById(R.id.regist_phone_edt);
		valiNumEdt = (EditText)findViewById(R.id.vali_num_edt);
		valiNumBtn = (CountDownButton)findViewById(R.id.vali_num_btn);
		registPasswordEdt = (ExtValidationEditText)findViewById(R.id.regist_password_edt);
		registBtn = (Button)findViewById(R.id.regist_btn);
		loginPhoneEdt = (ExtValidationEditText)findViewById(R.id.login_phone_edt);
		loginPasswordEdt = (ExtValidationEditText)findViewById(R.id.login_password_edt);
		forgetPasswordTxt = (TextView)findViewById(R.id.forget_password_txt);
		loginBtn = (Button)findViewById(R.id.login_btn);
		titleLoginBtn = (Button)findViewById(R.id.title_login_btn);
		titleRegistBtn = (Button)findViewById(R.id.title_regist_btn);
	}
	
	@Override
	protected void setListener(){
		mLeftBtn.setOnClickListener(this);
		valiNumBtn.setOnClickListener(this);
		registBtn.setOnClickListener(this);
		forgetPasswordTxt.setOnClickListener(this);
		loginBtn.setOnClickListener(this);
		titleLoginBtn.setOnClickListener(this);
		titleRegistBtn.setOnClickListener(this);
	}
	
    private void changeMode(boolean isLeft) {
    	isRegist = !isLeft;
        if (isLeft) {
        	mActionTag = ActionLog.UserLogin;
            registScv.setVisibility(View.GONE);
            loginScv.setVisibility(View.VISIBLE);
            titleLoginBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
            titleRegistBtn.setBackgroundResource(R.drawable.btn_hot_comment);
        } else {
        	mActionTag = ActionLog.UserRegist;
            loginScv.setVisibility(View.GONE);
            registScv.setVisibility(View.VISIBLE);
            titleLoginBtn.setBackgroundResource(R.drawable.btn_all_comment);
            titleRegistBtn.setBackgroundResource(R.drawable.btn_hot_comment_focused);
        }
        hideSoftInput();
    }
    
    @Override
	public void hideSoftInput(){
    	super.hideSoftInput();
    	clearAllFocus();
    }
    /**
     * 	//利用让隐藏的Edt获取焦点的办法，来使当前焦点消失
     * @return 
     */
    private void clearAllFocus(){
    	if(isRegist){
    		loginPhoneEdt.requestFocus();
    	}else{
    		registPhoneEdt.requestFocus();
    	}
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		if(!isRegist){
			if (UserLoginRegistActivity.class.getName().equals(getCallingActivity().getClassName())) {
				Intent intent = getIntent();
				String tmpPhone = intent.getStringExtra(TKConfig.PREFS_PHONENUM);
				if (!TextUtils.isEmpty(tmpPhone)) {
					setPrefsPhoneNum(tmpPhone);
				}
			}
		}
	}	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.left_btn:
            mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
            hideSoftInput();
            onBack();
            break;
		case R.id.vali_num_btn:
			mActionLog.addAction(mActionTag +  ActionLog.UserCommonValidNumBtn);
			if (!registPhoneEdt.isValid()) {
				validationAction(registPhoneEdt);
				return;
			}
			valiNumBtn.startCountDown();
			requestValiNum();
			break;
		case R.id.regist_btn:
            mActionLog.addAction(mActionTag +  ActionLog.UserCommonRegisterBtn);
			if (!mForm.isValid()) {
				validationAction(mForm.getErrorSource());
				return;
			}
			requestRegist();
			break;
		case R.id.forget_password_txt:
			mActionLog.addAction(mActionTag +  ActionLog.UserCommonForgetPasswordBtn);
			startActivityForResult(new Intent(UserLoginRegistActivity.this, UserResetPasswordActivity.class), 0);
			break;
		case R.id.login_btn:
		    mActionLog.addAction(mActionTag +  ActionLog.UserCommonLoginBtn);
            // 发出登录请求, 若成功中转到个人中心页面, 记录到SharedPreference
			if (!mForm.isValid()) {
				doAction(mForm.getErrorSource());
				return;
			}
			requestLogin();
			break;
		case R.id.title_login_btn:
			if(isRegist){
				mActionLog.addAction(mActionTag + ActionLog.UserGoLogin);
				changeMode(true);
			}
			break;
		case R.id.title_regist_btn:
			if(!isRegist){
				mActionLog.addAction(mActionTag + ActionLog.UserGoRegist);
				changeMode(false);
			}
			break;
		}
	}
	
	@Override
	public void finish() {
		super.finish();
		valiNumBtn.reset(getString(R.string.reqest_validate_num));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!isRegist && requestCode == REQUEST_CODE_REGIST && resultCode == RESULT_OK && data != null 
				&& !TextUtils.isEmpty(data.getStringExtra(TKConfig.PREFS_PHONENUM))) {
			String prefsPhoneNum = data.getStringExtra(TKConfig.PREFS_PHONENUM);
			setPrefsPhoneNum(prefsPhoneNum);
			if (TextUtils.isEmpty(prefsPhoneNum) == false) {
                loginPasswordEdt.requestFocus();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!isRegist && keyCode == KeyEvent.KEYCODE_BACK) {
            mActionLog.addAction(ActionLog.KeyCodeBack);
			onBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * 发出登录请求
	 * ResponseCode: 200, 403, 404
	 */
	private void requestLogin() {
		AccountManage accountManage = new AccountManage(this);
		
		String phone = loginPhoneEdt.getText().toString().trim();
		String password = loginPasswordEdt.getText().toString().trim();
		
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_LOGIN);
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		accountManage.addParameter(AccountManage.SERVER_PARAMETER_PASSWORD, Utility.encryptWithSHA1(password));
		
		sendRequest(accountManage);
	}

	private void doAction(final ExtValidationEditText source) {
		Utility.showNormalDialog(UserLoginRegistActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					source.requestFocus();
					
					if (source.validErrorType == ExtValidationEditText.ValidErrorType.PhoneFormatError) {
						source.selectAll();
					} else if (source.validErrorType == ExtValidationEditText.ValidErrorType.PasswordFormatError) {
                        if (source.hasUnionView()) {
                            source.getUnionView().setText("");
                        }
                        source.setText("");
                    }
					showSoftInput(source);
				}
			}
		});
	}

	private void dealWith403() {
		mResponseHandler.showErrorThenSelectAllAndShowSoftkeyboard(R.string.title_error_tip, 
				R.string.response_code_403_login, loginPhoneEdt);
		
	}
	
	private void dealWith404() {
		mResponseHandler.showErrorThenClearAndShowSoftkeyboard(R.string.title_error_tip, 
				R.string.response_code_404, loginPasswordEdt);
	}
	
	private void onBack() {
        if (mSourceUserHome && Globals.g_User == null) {
            Intent intent = new Intent(UserLoginRegistActivity.this, Sphinx.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }
		if (mSourceViewIdLogin != R.id.activity_poi_edit_comment
		        && mSourceViewIdLogin != R.id.activity_poi_comment_list) {
			Intent intent = new Intent(UserLoginRegistActivity.this, Sphinx.class);
	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
		}
		finish();
	}	
	
	private void validationAction(final ExtValidationEditText source) {
		Utility.showNormalDialog(UserLoginRegistActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
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
	/**
	 * 请求验证码.
	 * ResponseCode: 200, 400, 503
	 */
	private void requestValiNum() {
		AccountManage accountManage = new AccountManage(this);

		String phone = registPhoneEdt.getText().toString().trim();
		
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_BIND_TELEPHONE);
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		
		sendRequest(accountManage);
	}
	/**
	 * 发出注册请求
	 * ResponseCode: 200, 402
	 */
	private void requestRegist() {

		AccountManage accountManage = new AccountManage(this);

		String phone = registPhoneEdt.getText().toString().trim();
		String password = registPasswordEdt.getText().toString().trim();
		String valiNum = valiNumEdt.getText().toString().trim();
		
		accountManage.addParameter(AccountManage.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_CREATE);
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		accountManage.addParameter(AccountManage.SERVER_PARAMETER_PASSWORD, Utility.encryptWithSHA1(password));
		accountManage.addParameter(AccountManage.SERVER_PARAMETER_VALIDATE_CODE, valiNum);
		
		sendRequest(accountManage);
	}
	private void onSuccess() {
		if(isRegist){
			showToast(R.string.regist_success);
			if (mSourceViewIdLogin != R.id.activity_poi_edit_comment) {
				Intent intent = new Intent(UserLoginRegistActivity.this, UserUpdateNickNameActivity.class);
				startActivityForResult(intent, 0);
			} else {
				Intent intent = new Intent(UserLoginRegistActivity.this, EditCommentActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
			
			finish();
		}
		else{
			if (mSourceViewIdLogin == R.id.activity_poi_edit_comment) {
	            Intent intent = new Intent(UserLoginRegistActivity.this, EditCommentActivity.class);
	            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
			} else if (mSourceViewIdLogin != R.id.activity_poi_comment_list){
				Intent intent = new Intent(UserLoginRegistActivity.this, Sphinx.class);
		        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        startActivity(intent);
			}
		}
	}
	
	@Override
	protected void responseCodeAction(AccountManage accountManage) {
		String operationCode = accountManage.getParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
		if(isRegist){
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
					TKConfig.setPref(UserLoginRegistActivity.this, TKConfig.PREFS_PHONENUM, registPhoneEdt.getText().toString().trim());
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
		}else{
			if (AccountManage.OPERATION_CODE_LOGIN.equals(operationCode)){
				// 200, 403, 404(, 500)
				switch(accountManage.getResponse().getResponseCode()){
				case Response.RESPONSE_CODE_OK:

					UserRespnose userResponse = (UserRespnose)accountManage.getResponse();
					Globals.storeSessionAndUser(mThis, userResponse);
					
					// 登录成功后记录用户曾经输入的手机号，以便上次显示
					TKConfig.setPref(UserLoginRegistActivity.this, TKConfig.PREFS_PHONENUM, loginPhoneEdt.getText().toString().trim());
					onSuccess();
					break;
				case Response.RESPONSE_CODE_NO_MOBILE_PHONE:
					dealWith403();
					break;
				case Response.RESPONSE_CODE_PASWORD_ERROR:
					dealWith404();
					break;
				default:
				}
			}			
		}
		
	}
	
	private void dealWith400() {
		Utility.showNormalDialog(UserLoginRegistActivity.this, 
		        getString(R.string.title_error_tip), 
				getString(R.string.response_code_400), 
				getString(R.string.confirm),
				null,
				new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					setPrefsPhoneNum(registPhoneEdt.getText().toString().trim());
					changeMode(true);
				} 
			}
		
		});
	}
	
	private void dealWith402() {
		Utility.showNormalDialog(UserLoginRegistActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_402), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumEdt.requestFocus();
					valiNumEdt.selectAll();
					showSoftInput(valiNumEdt);
				}
				//valiNumBtn.reset(getString(R.string.request_validcode_retry));
			}
			
		});
	}
	
	private void dealWith503() {
		Utility.showNormalDialog(UserLoginRegistActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_503), 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
//				if (which == DialogInterface.BUTTON_POSITIVE) {
					valiNumBtn.reset(getString(R.string.request_validcode_retry));
//				}
			}
			
		});
	}

	@Override
	protected void responseBadNetwork() {
		super.responseBadNetwork();
		if(isRegist)valiNumBtn.reset(getString(R.string.reqest_validate_num));
	}	
}

package com.tigerknows.ui.user;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.decarta.Globals;
//import com.tigerknows.POIComment;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.AccountManage.UserRespnose;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.util.Utility;

public class UserLoginActivity extends UserBaseActivity {

	public static final int REQUEST_CODE_REGIST = 1;
	
	private ExtValidationEditText phoneEdt;
	
	private ExtValidationEditText passwordEdt;
	
	private TextView forgetPasswordTxt;
	
	private Button loginBtn;
	
	private Button registerBtn;
	
	static final String TAG = "UserLoginActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mActionTag = ActionLog.UserLogin;
		mId = R.id.activity_user_login;

		setContentView(R.layout.user_login);
        findViews();
        setListener();
        mRightBtn.setWillNotDraw(true);
//        mRightBtn.setText(R.string.regist);
		mTitleBtn.setText(getString(R.string.title_user_login));
		
		String prefsPhoneNum = TKConfig.getPref(UserLoginActivity.this, TKConfig.PREFS_PHONENUM, "");
		setPrefsPhoneNum(prefsPhoneNum);
	}

	private void setPrefsPhoneNum(String prefsPhoneNum) {
		if (!TextUtils.isEmpty(prefsPhoneNum)) {
            phoneEdt.setText(prefsPhoneNum);
            phoneEdt.setSelection(phoneEdt.getText().toString().length());
        }
	}

	protected void findViews() {
		super.findViews();
		
		phoneEdt = (ExtValidationEditText)findViewById(R.id.phone_edt);
		passwordEdt = (ExtValidationEditText)findViewById(R.id.password_edt);
		forgetPasswordTxt = (TextView)findViewById(R.id.forget_password_txt);
		registerBtn = (Button)findViewById(R.id.register_btn);
		loginBtn = (Button)findViewById(R.id.login_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
                    hideSoftInput();
                    onBack();
                }
            });
        }
		
		forgetPasswordTxt.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 跳转到重置密码页面
				mActionLog.addAction(mActionTag +  ActionLog.UserCommonForgetPasswordBtn);
				startActivityForResult(new Intent(UserLoginActivity.this, UserResetPasswordActivity.class), 0);
			}
		});
		
		loginBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
			    mActionLog.addAction(mActionTag +  ActionLog.UserCommonLoginBtn);
                // TODO 发出登录请求, 若成功中转到个人中心页面, 记录到SharedPreference
				if (!mForm.isValid()) {
					doAction(mForm.getErrorSource());
					return;
				}
				
				requestLogin();
			}
		});
		
		registerBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 跳转到注册页面
				mActionLog.addAction(mActionTag +  ActionLog.UserCommonRegisterBtn);
				Intent intent = new Intent(UserLoginActivity.this, UserRegistActivity.class);
				startActivityForResult(intent, REQUEST_CODE_REGIST);
			}
		});
		
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		if (UserRegistActivity.class.getName().equals(getCallingActivity().getClassName())) {
			Intent intent = getIntent();
			String tmpPhone = intent.getStringExtra(TKConfig.PREFS_PHONENUM);
			if (!TextUtils.isEmpty(tmpPhone)) {
				setPrefsPhoneNum(tmpPhone);
			}
		}
		
		// 若是一个有效的电话号码， 则光标停留在密码框;
		// 反之，停留在电话框
		if (phoneEdt.simpleCheck()) {
			passwordEdt.requestFocus();
		} else {
			phoneEdt.requestFocus();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
		if (requestCode == REQUEST_CODE_REGIST && resultCode == RESULT_OK && data != null 
				&& !TextUtils.isEmpty(data.getStringExtra(TKConfig.PREFS_PHONENUM))) {
			String prefsPhoneNum = data.getStringExtra(TKConfig.PREFS_PHONENUM);
			setPrefsPhoneNum(prefsPhoneNum);
			if (TextUtils.isEmpty(prefsPhoneNum) == false) {
                passwordEdt.requestFocus();
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
            mActionLog.addAction(ActionLog.KeyCodeBack);
			onBack();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}

	private void onBack() {
        if (mSourceUserHome && Globals.g_User == null) {
            Intent intent = new Intent(UserLoginActivity.this, Sphinx.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }
		if (mSourceViewIdLogin != R.id.activity_poi_edit_comment
		        && mSourceViewIdLogin != R.id.activity_poi_comment_list) {
			Intent intent = new Intent(UserLoginActivity.this, Sphinx.class);
	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
		}
		
		finish();
	}
	
	private void onSuccess() {
		if (mSourceViewIdLogin == R.id.activity_poi_edit_comment) {
            Intent intent = new Intent(UserLoginActivity.this, EditCommentActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
		} else if (mSourceViewIdLogin != R.id.activity_poi_comment_list){
			Intent intent = new Intent(UserLoginActivity.this, Sphinx.class);
	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
		}
		
		finish();
	}
	
	/**
	 * 发出登录请求
	 * ResponseCode: 200, 403, 404
	 */
	private void requestLogin() {
		AccountManage accountManage = new AccountManage(this);
		
		String phone = phoneEdt.getText().toString().trim();
		String password = passwordEdt.getText().toString().trim();
		
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_LOGIN);
		accountManage.addParameter(BaseQuery.SERVER_PARAMETER_TELEPHONE, phone);
		accountManage.addParameter(AccountManage.SERVER_PARAMETER_PASSWORD, Utility.encryptWithSHA1(password));
		
		sendRequest(accountManage);
	}

	private void doAction(final ExtValidationEditText source) {
		Utility.showNormalDialog(UserLoginActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
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

	@Override
	protected void responseCodeAction(AccountManage accountManage) {
		// TODO Auto-generated method stub
		String operationCode = accountManage.getParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
		if (AccountManage.OPERATION_CODE_LOGIN.equals(operationCode)){
			// 200, 403, 404(, 500)
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:

				UserRespnose userResponse = (UserRespnose)accountManage.getResponse();
				Globals.storeSessionAndUser(mThis, userResponse);
				
				// 登录成功后记录用户曾经输入的手机号，以便上次显示
				TKConfig.setPref(UserLoginActivity.this, TKConfig.PREFS_PHONENUM, phoneEdt.getText().toString().trim());
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
	
	private void dealWith403() {
		mResponseHandler.showErrorThenSelectAllAndShowSoftkeyboard(R.string.title_error_tip, 
				R.string.response_code_403_login, phoneEdt);
		
	}
	
	private void dealWith404() {
		mResponseHandler.showErrorThenClearAndShowSoftkeyboard(R.string.title_error_tip, 
				R.string.response_code_404, passwordEdt);
	}
}

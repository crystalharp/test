package com.tigerknows.ui.user;

import java.net.URLEncoder;
import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import com.tigerknows.common.ActionLog;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.User;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.Utility;

public class UserUpdateNickNameActivity extends UserBaseActivity {
	
	private ExtValidationEditText nickNameEdt;
	
	private ImageView nickNameImg;
	
	private Button confirmBtn;
	
	private final int NickNameMaxLength = 20;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mActionTag = ActionLog.UserUpdateNickName;
		mId = R.id.activity_user_update_nickname;
		setContentView(R.layout.user_update_nickname);
        findViews();
        setListener();
		User user = User.loadDefault(this);
        if (user == null) {
            onBack();
        } else if (!TextUtils.equals(user.getNickName(), getString(R.string.default_nick_name))) {
			nickNameEdt.setText(user.getNickName());
			nickNameEdt.setSelection(nickNameEdt.getText().toString().length());
			nickNameImg.setVisibility(View.VISIBLE);
		} else {
			nickNameImg.setVisibility(View.GONE);
		}
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(getString(R.string.title_update_nickname));
	}

	protected void findViews() {
		super.findViews();
		
		nickNameEdt = (ExtValidationEditText)findViewById(R.id.nickname_edt);
		nickNameImg = (ImageView)findViewById(R.id.nickname_img);
		confirmBtn = (Button)findViewById(R.id.confirm_btn);
	}
	
	protected void setListener() {
		super.setListener();

		if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
                    onBack();
                }
            });
        }
		
		confirmBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
                mActionLog.addAction(mActionTag +  ActionLog.UserCommonConfirmBtn);
				// TODO 关闭页面, 并跳转到个人中心页面
				if (!mForm.isValid()) {
					doAction(mForm.getErrorSource());
					return;
				}
				
				String nickName = null;
				User user = Globals.g_User;
				if (user == null) {
				    onBack();
				} else {
				    nickName = user.getNickName();
				}
				if (!UserRegistActivity.class.getName().equals(getCallingActivity().getClassName())
						&& !TextUtils.equals(nickNameEdt.getText().toString().trim(), getString(R.string.default_nick_name))
						&& TextUtils.equals(Globals.g_User.getNickName(), nickNameEdt.getText().toString().trim())) {
					Utility.showNormalDialog(UserUpdateNickNameActivity.this, getString(R.string.title_error_tip), getString(R.string.nickname_no_modify), 
					        getString(R.string.confirm),
			                null,
			                new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								nickNameEdt.selectAll();
								nickNameEdt.requestFocus();
								showSoftInput(nickNameEdt);
							}
						}
						
					});
					return;
				}
				
				requestChangeNickName();
			}
		});
		
		nickNameEdt.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
		        Editable editable = nickNameEdt.getText();  
		        int len = ByteUtil.getCharArrayLength(editable.toString());
		        if(len > NickNameMaxLength){
		        	int selEndIndex = Selection.getSelectionEnd(editable); 
		        	String str = editable.toString();
		        	while(len > NickNameMaxLength){  
		        		//截取新字符串
		        		str = str.substring(0,selEndIndex-1) + str.substring(selEndIndex,str.length());
		        		//新字符串的长度
		        		len = ByteUtil.getCharArrayLength(str);
		        		selEndIndex--;
		        	}  
		        	nickNameEdt.setText(str);  
		        	editable = nickNameEdt.getText();  
		    		//设置新光标所在的位置
		        	Selection.setSelection(editable, selEndIndex);  
		        }
		    }
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!TextUtils.isEmpty(nickNameEdt.getText())) {
							nickNameImg.setVisibility(View.VISIBLE);
						} else {
							nickNameImg.setVisibility(View.GONE);
						}
					}
				});
			}
		});
		
		nickNameImg.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mActionLog.addAction(mActionTag+ActionLog.EditTextDelete);
				nickNameEdt.setText("");
				showSoftInput(nickNameEdt);
			}
		});
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
            Intent intent = new Intent(mThis, Sphinx.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }
		if (mSourceViewIdLogin == R.id.activity_poi_edit_comment) {
		    if (mSourceViewId == R.id.activity_poi_edit_comment) {
                setResult(RESULT_OK);
            } else {
                Intent intent = new Intent(UserUpdateNickNameActivity.this, EditCommentActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
		} else if (mSourceViewIdLogin != R.id.activity_poi_comment_list &&
		        mSourceViewIdLogin != R.id.activity_discover_shangjia_list){
			Intent intent = new Intent(UserUpdateNickNameActivity.this, Sphinx.class);
	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
		}
		
		finish();
	}
	
	private void onSuccess() {
		
		if (UserRegistActivity.class.getName().equals(getCallingActivity().getClassName())) {
			showToast(R.string.update_nicknam_success1);
		} else {
			showToast(R.string.update_nicknam_success2);
		}
		
		if (mSourceViewIdLogin == R.id.activity_poi_edit_comment) {
		    if (mSourceViewId == R.id.activity_poi_edit_comment) {
                setResult(RESULT_OK);
            } else {
                Intent intent = new Intent(UserUpdateNickNameActivity.this, EditCommentActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
		} else if (mSourceViewIdLogin != R.id.activity_poi_comment_list){
			// 从此页返回个人中心页时, 需刷新用户名
			Intent intent = new Intent(UserUpdateNickNameActivity.this, Sphinx.class);
	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(intent);
		}
		
		finish();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		User user = User.loadDefault(this);

        if (user == null) {
            onBack();
        }
	}

	/**
	 * 发出登录请求
	 * ResponseCode: 200, 403, 404
	 */
	private void requestChangeNickName() {
		AccountManage accountManage = new AccountManage(this);
		
//		String nickName = nickNameEdt.getText().toString().trim();
		String nickName = URLEncoder.encode(nickNameEdt.getText().toString().trim());
		if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
			Hashtable<String, String> criteria = new Hashtable<String, String>();
			criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_UPDATE_NICKNAME);
			criteria.put(AccountManage.SERVER_PARAMETER_NICKNAME, nickName);
			criteria.put(BaseQuery.SERVER_PARAMETER_SESSION_ID, Globals.g_Session_Id);
			
			sendRequest(accountManage, criteria);
		} 
	}
	
	private void doAction(final ExtValidationEditText source) {
		Utility.showNormalDialog(UserUpdateNickNameActivity.this, getString(R.string.title_error_tip), source.msg, 
		        getString(R.string.confirm),
                null,
                new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					if (source.validErrorType == ExtValidationEditText.ValidErrorType.NameFormatError) {
						source.selectAll();
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
		if (AccountManage.OPERATION_CODE_UPDATE_NICKNAME.equals(operationCode)) {
			//  200, (300, 301, )401(, 500)
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				User user = User.loadDefault(this);
				user.setNickName(nickNameEdt.getText().toString().trim());
				user.store(this);	
				Globals.g_User = user;

				onSuccess();
				break;
			case Response.RESPONSE_CODE_NICKNAME_EXIST:
				dealWith401();
				break;
			default:
			}
		}
	}
	
	private void dealWith401() {
		Utility.showNormalDialog(UserUpdateNickNameActivity.this, getString(R.string.title_error_tip), getString(R.string.response_code_401), 
                getString(R.string.confirm),
                null,
                new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog,
					int which) {
				// TODO Auto-generated method stub
				if (which == DialogInterface.BUTTON_POSITIVE) {
					nickNameEdt.selectAll();
					nickNameEdt.requestFocus();
					showSoftInput(nickNameEdt);
				}
			}
			
		});
	}
}

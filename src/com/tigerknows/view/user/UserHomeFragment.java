package com.tigerknows.view.user;

import java.util.Hashtable;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.BaseFragment;

public class UserHomeFragment extends UserBaseFragment {

	public UserHomeFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
	
	private Button nickNameTxv;
	
	private Button myCommentBtn;
    
    private Button dingdanBtn;
	
	private Button updatePhoneBtn;
	
//	private Button updateNicknameBtn;
	
	private Button updatePasswordBtn;
	
	private Button logoutBtn;
	
	private static final String TAG = "UserHomeFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mActionTag = ActionLog.UserHome;
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.user_home, container, false);
        
        findViews();        
        setListener();
        
        return mRootView;
    }

	protected void findViews() {
		super.findViews();

		nickNameTxv = (Button)mRootView.findViewById(R.id.nikename_txv);
		myCommentBtn = (Button)mRootView.findViewById(R.id.my_comment_btn);
        dingdanBtn = (Button)mRootView.findViewById(R.id.dingdan_btn);
		updatePhoneBtn = (Button)mRootView.findViewById(R.id.update_mobile_phone_btn);
//		updateNicknameBtn = nickNameTxv;
		updatePasswordBtn = (Button)mRootView.findViewById(R.id.update_password_btn);
		logoutBtn = (Button)mRootView.findViewById(R.id.logout_btn);
	}
	
	protected void setListener() {
		
		if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(ActionLog.Title_Left_Back, mActionTag);
                    onBack();
                }
            });
        }
		
		updatePhoneBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 跳转到修改电话页面
				mActionLog.addAction(ActionLog.UserHomeToUpdatePhone);
                Intent intent = new Intent(mSphinx, UserUpdatePhoneActivity.class);
                intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
                intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, R.id.view_more);
				mSphinx.showView(R.id.activity_user_update_phone, intent);
			}
		});
		
		nickNameTxv.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 跳转到修改昵称页面
				mActionLog.addAction(ActionLog.UserHomeToUpdateName);
				Intent intent = new Intent(mSphinx, UserUpdateNickNameActivity.class);
                mSphinx.showView(R.id.activity_user_update_nickname, intent);
			}
		});		
		
		updatePasswordBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 跳转到修改密码页面
				mActionLog.addAction(ActionLog.UserHomeToUpdatePassword);
				Intent intent = new Intent(mSphinx, UserUpdatePasswordActivity.class);
				intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
                intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, R.id.view_more);
                mSphinx.showView(R.id.activity_user_update_password, intent);
			}
		});
		
		logoutBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO 注销登录, 跳转"更多"页面
				mActionLog.addAction(ActionLog.UserHomeLogout);
				CommonUtils.showNormalDialog(mSphinx, mContext.getString(R.string.title_error_tip), 
						mContext.getString(R.string.logout_account_tip), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE) {
							requestLougout();
						}
					}
					
				});
				
			}
		});
        
        myCommentBtn.setOnClickListener(new android.view.View.OnClickListener(){

            @Override
            public void onClick(View v) {
				mActionLog.addAction(ActionLog.UserHomeToMyComment);
                mSphinx.showView(R.id.view_my_comment_list);
            }
        });
        
        dingdanBtn.setOnClickListener(new android.view.View.OnClickListener(){

            @Override
            public void onClick(View v) {
                mActionLog.addAction(ActionLog.UserHomeDingdan);
                Intent intent = new Intent();
                intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID, getId());
                mSphinx.showView(R.id.activity_tuangou_shangjia_list, intent);
            }
        });
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		LogWrapper.d(TAG, "onResume()");
		
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(mContext.getString(R.string.title_user_home));
        
		if (Globals.g_User != null) {
			nickNameTxv.setText(Globals.g_User.getNickName());
			LogWrapper.d(TAG, "onResume() set username: " + Globals.g_User.getNickName());
		} else {
		    dismiss();
		}
        
        if (mLeftBtn != null) {
            mLeftBtn.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(ActionLog.Title_Left_Back, mActionTag);
                    onBack();
                }
            });
        }
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
            mActionLog.addAction(ActionLog.KeyCodeBack, mActionTag);
			onBack();
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	private void onBack() {
		int preferViewId = R.id.view_more;
		if (mSphinx.uiStackContains(preferViewId)) {
			mSphinx.uiStackClearTop(preferViewId);
        } 
		mSphinx.showView(preferViewId);
	}
	
	/**
	 * 发出注销请求
	 * ResponseCode: 200
	 */
	private void requestLougout() {
		AccountManage accountManage = new AccountManage(mContext);
		
		if (TextUtils.isEmpty(Globals.g_Session_Id) == false) {
			Hashtable<String, String> criteria = new Hashtable<String, String>();
			criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, AccountManage.OPERATION_CODE_LOGOUT);
			criteria.put(BaseQuery.SERVER_PARAMETER_SESSION_ID, Globals.g_Session_Id);
			
			sendRequest(accountManage, criteria);
		} 
	}

	@Override
	protected void responseCodeAction(AccountManage accountManage) {
		// TODO Auto-generated method stub
		String operationCode = accountManage.getCriteria().get(BaseQuery.SERVER_PARAMETER_OPERATION_CODE);
		if (AccountManage.OPERATION_CODE_LOGOUT.equals(operationCode)){
			// 200, 400, 503
			switch(accountManage.getResponse().getResponseCode()){
			case Response.RESPONSE_CODE_OK:
				showToast(R.string.logout_success);
				Globals.clearSessionAndUser(mContext);
				dismiss();
				break;
			default:
			}
		}
	}

}

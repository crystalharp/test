package com.tigerknows.view.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.model.AccountManage;

public class UserCommentAfterActivity extends UserBaseActivity {

	private Button loginBtn;
	
	private Button registBtn;
	
	static final String TAG = "UserCommentAfterActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mActionTag = ActionLog.UserCommentAfter;
		mId = R.id.activity_user_comment_after;

		setContentView(R.layout.user_comment_after);
        findViews();
        setListener();

        mRightBtn.setVisibility(View.GONE);
		mTitleBtn.setText(getString(R.string.title_update_comment_after));
	}

	protected void findViews() {
		super.findViews();
		loginBtn = (Button)findViewById(R.id.login_btn);
		registBtn = (Button)findViewById(R.id.regist_btn);
	}
	
	protected void setListener() {
		super.setListener();
		
		loginBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
			    mActionLog.addAction(ActionLog.UserCommentAfterLogin);
				Intent intent = new Intent(UserCommentAfterActivity.this, UserLoginActivity.class);
				startActivityForResult(intent, 0);
			}
		});
		
		registBtn.setOnClickListener(new android.view.View.OnClickListener(){

			@Override
			public void onClick(View v) {
                mActionLog.addAction(ActionLog.UserCommentAfterRegist);
				Intent intent = new Intent(UserCommentAfterActivity.this, UserRegistActivity.class);
				startActivityForResult(intent, 0);
			}
		});
	}
	
	@Override
	protected void responseCodeAction(AccountManage accountManage) {
		// TODO Auto-generated method stub
	}
}

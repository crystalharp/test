package com.tigerknows.share.impl;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.share.ShareMessageCenter;

public class AlterImageDialog extends Dialog {

	ImageView mPic;
	
	Button mBack;
	
	Button mDelete;
	
	Context mContext;
	
	ActionLog mActionLog;
	
	public AlterImageDialog(Context context, Bitmap bitmap) {
		super(context, R.style.Theme_Dialog);
		// TODO Auto-generated constructor stub
		mContext = context;
		
		mActionLog = ActionLog.getInstance(context);
		mActionLog.addAction(ActionLog.WeiboImage);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
        
		setContentView(R.layout.weibo_alter_image);
		findViews();
		setListeners();
		mPic.setImageBitmap(bitmap);
		
		getWindow().getAttributes().windowAnimations = R.style.Animation_UpAndDown;
	}

	protected void findViews() {
		mPic = (ImageView)findViewById(R.id.pic_imv);
		mBack = (Button)findViewById(R.id.back_btn);
		mDelete = (Button)findViewById(R.id.delete_btn);
	}
	
	protected void setListeners() {
		mBack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		mDelete.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mActionLog.addAction(ActionLog.WeiboImageClickedDelPic);
				Intent intent = new Intent(ShareMessageCenter.ACTION_SHARE_IMAGE_REMOVE);
				mContext.sendBroadcast(intent);
				dismiss();
			}
		});
	}

	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		mActionLog.addAction(ActionLog.Dismiss, ActionLog.WeiboImage);
		super.dismiss();
	}
	
	
}

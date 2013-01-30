package com.tigerknows.share;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.tigerknows.ActionLog;
import com.tigerknows.R;

public class AlterImageDialog extends Dialog {

	ImageView mPic;
	
	Button mBack;
	
	Button mDelete;
	
	WeiboSend mContext;
	
	ActionLog mActionLog;
	
	public AlterImageDialog(WeiboSend context, Bitmap bitmap) {
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
		
		getWindow().getAttributes().windowAnimations = R.style.AlterImageDialog;
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
				mContext.removeImage();
				dismiss();
			}
		});
	}

	@Override
	public void dismiss() {
		super.dismiss();
	}
	
	
}

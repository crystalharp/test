package com.tigerknows.ui.common;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.ui.BaseFragment;

public class EditTextFragment extends BaseFragment{

	public EditTextFragment(Sphinx sphinx) {
		super(sphinx);
	}
	
	private TKEditText mBodyEdt;
	private TextInputListener mCallBack;
	private String mEmptyTip;
	private String mInitText;

    public interface TextInputListener {
        public void AfterTextInput(String str);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.edit_text, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }
    
    @Override
    protected void findViews(){
    	mBodyEdt = (TKEditText) mRootView.findViewById(R.id.body_edt);
    }
    
    @Override
    protected void setListener(){
    	mBodyEdt.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				String str = mBodyEdt.getText().toString();
				if(TextUtils.isEmpty(str) && !TextUtils.isEmpty(mEmptyTip)){
					Toast.makeText(mContext, mEmptyTip, Toast.LENGTH_SHORT).show();
				}else{
					mCallBack.AfterTextInput(mBodyEdt.getText().toString());
					dismiss();
				}
				return false;
			}
		});
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	mBodyEdt.setText(mInitText);
    	mBodyEdt.setHint(mSphinx.getString(R.string.set_common_place_alias));
    	mTitleBtn.setText(R.string.common_place_alias);
    	mRightBtn.setText(R.string.confirm);
    	mRightBtn.setVisibility(View.VISIBLE);
    	mRightBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String str = mBodyEdt.getText().toString();
				if(TextUtils.isEmpty(str) && !TextUtils.isEmpty(mEmptyTip)){
					Toast.makeText(mContext, mEmptyTip, Toast.LENGTH_SHORT).show();
				}else{
					mCallBack.AfterTextInput(mBodyEdt.getText().toString());
					dismiss();
				}
			}
		});
    }
    
    public void setDataNoInit(TextInputListener callBack, String emptyTip){
    	mCallBack = callBack;
    	mEmptyTip = emptyTip;
    }
    
    public void setData(String text, TextInputListener callBack, String emptyTip){
    	mInitText = text;
    	mCallBack = callBack;
    	mEmptyTip = emptyTip;
    }

}

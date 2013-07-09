/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 * 2013.07
 */
package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;


public class SatisfyRateActivity extends BaseActivity implements View.OnClickListener{
    
	private static final int NUM_OF_RATINGBAR = 7;
	private static final int[] mRatingBarID={
		R.id.satisfy_1_rbt,
		R.id.satisfy_2_rbt,
		R.id.satisfy_3_rbt,
		R.id.satisfy_4_rbt,
		R.id.satisfy_5_rbt,
		R.id.satisfy_6_rbt,
		R.id.satisfy_7_rbt
	};
	private static final int[] mRateTxvID={
	    R.id.rate_1_txv,
	    R.id.rate_2_txv,
	    R.id.rate_3_txv,
	    R.id.rate_4_txv,
	    R.id.rate_5_txv,
	    R.id.rate_6_txv,
	    R.id.rate_7_txv
	};

	private RatingBar[] mSatisfyRbt;
	private TextView[] mRateTxv;
	private List<String> mRateList;
	private Button mSubmitBtn;
	private boolean mChanged;
	
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.SatisfyRate;
        setContentView(R.layout.more_satisfy_rate);
    	mSatisfyRbt = new RatingBar[NUM_OF_RATINGBAR];
    	mRateTxv = new TextView[NUM_OF_RATINGBAR];
    	mRateList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.satisfy_star)));
        findViews();
        setListener();    
        mTitleBtn.setText(getString(R.string.satisfy_rate));
        mChanged = false;
    }
    
    protected void findViews() {
    	super.findViews();
    	mSubmitBtn = (Button)findViewById(R.id.submit_btn);
    	for (int i=0; i<NUM_OF_RATINGBAR; i++){
    		mSatisfyRbt[i] = (RatingBar)findViewById(mRatingBarID[i]);
    		mRateTxv[i] = (TextView)findViewById(mRateTxvID[i]);
    	}
    }
    
    @Override
    protected void onResume(){
    	super.onResume();
    	hideSoftInput();
    }
    
    protected void setListener() {
    	super.setListener();
    	mSubmitBtn.setOnClickListener(this);
    	mLeftBtn.setOnClickListener(this);
    	for (int i=0; i<NUM_OF_RATINGBAR; i++){
    		final int j=i;
    		LogWrapper.d("Trap", j+"");
    		mSatisfyRbt[j].setOnRatingBarChangeListener(new OnRatingBarChangeListener(){

				@Override
				public void onRatingChanged(RatingBar ratingBar, float rating,
						boolean fromUser) {
					// TODO Auto-generated method stub
					if (fromUser){
						int iRating = Math.round(rating);
						mRateTxv[j].setText(mRateList.get(iRating));
						refreshSubmitBtn();
						mSubmitBtn.setTextColor(mSubmitBtn.isEnabled()
								? getResources().getColor(R.color.orange)
								: getResources().getColor(R.color.black_light));
					}
				}
    		});
    	}
    }
    
    private void refreshSubmitBtn(){
    	boolean status = true;
    	mChanged = false;
    	for (int i=0; i<NUM_OF_RATINGBAR; i++){
    		if (Math.round(mSatisfyRbt[i].getRating())== 0){
    			status = false;
    			break;
    		}else mChanged = true;
    	}
    	mSubmitBtn.setEnabled(status);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (showDiscardDialog() == false) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    private boolean showDiscardDialog(){
        boolean show = mChanged;
        if (show == false)return false;
        Utility.showNormalDialog(mThis, getString(R.string.satisfy_abandon), new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE){
                    finish();
                }
            }
        });
        return show;
    }
    
    @Override
    public void onClick(View view) {
    	if(view.getId() == R.id.submit_btn){
    		submit();
    	}else if(view.getId() == R.id.left_btn){
    		mActionLog.addAction(mActionTag, ActionLog.TitleLeftButton);
    		if (showDiscardDialog() == false){
    			finish();
    		}
    	}
    }
    
    private void submit(){
    	StringBuilder s = new StringBuilder();
    	for (int i = 0; i<NUM_OF_RATINGBAR; i++){
    		s.append('_');
    		s.append(Math.round(mSatisfyRbt[i].getRating())+"");
    	}
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(FeedbackUpload.SERVER_PARAMETER_SATISFY_RATE, s.toString());
        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
        feedbackUpload.setup(criteria, Globals.getCurrentCityInfo().getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
        queryStart(feedbackUpload);
    }
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, this, false)) {
            return;
        }
        Toast.makeText(mThis, R.string.satisfy_success, Toast.LENGTH_LONG).show();
        finish();
    }
}
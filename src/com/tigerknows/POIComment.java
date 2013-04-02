/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tencent.tauth.TAuthView;
import com.tigerknows.R;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataOperation.CommentCreateResponse;
import com.tigerknows.model.DataOperation.CommentUpdateResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.share.TKTencentOpenAPI;
import com.tigerknows.share.TKWeibo;
import com.tigerknows.share.UserAccessIdenty;
import com.tigerknows.share.ShareAPI.LoginCallBack;
import com.tigerknows.share.TKTencentOpenAPI.AuthReceiver;
import com.tigerknows.share.TKWeibo.AuthDialogListener;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.user.User;
import com.tigerknows.view.user.UserBaseActivity;
import com.tigerknows.view.user.UserCommentAfterActivity;
import com.tigerknows.view.user.UserUpdateNickNameActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RatingBar.OnRatingBarChangeListener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class POIComment extends BaseActivity implements View.OnClickListener {
    
    static final String TAG = "POIComment";
    
    public static final int STATUS_NEW = 1;
    public static final int STATUS_MODIFY = 2;

    public static final int MIN_CHAR = 30;

    private TextView mTextNumTxv;
    
    private int mStatus;
    
    private EditText mContentEdt;
    
    private CheckBox mSyncQZoneChb;
    
    private CheckBox mSyncSinaChb;
    
    private ViewGroup mExpandView;
    
    private ViewGroup mExpandFoodView;
    
    private TextView mMoreTxv;
    
    private RatingBar mGradeRtb;
    
    private TextView mGradeTipTxv;
    
    private EditText mFoodAvgEdt;
    
    private RatingBar mTasteRtb;
    
    private RatingBar mFoodEnvironmentRtb;
    
    private RatingBar mFoodQosRtb;
    
    private Button mRestairBtn;
    
    private String[] mRestairArray;
    
    private boolean[] mRestairChecked;
    
    private EditText mRecommendEdt;
    
    private ViewGroup mExpandHotelView;
    
    private EditText mHotelAvgEdt;
    
    private RatingBar mHotelQosRtb;
    
    private RatingBar mHotelEnvironmentRtb;
    
    private ViewGroup mExpandCinemaView;
    
    private EditText mCinemaAvgEdt;
    
    private RatingBar mEffectRtb;
    
    private RatingBar mCinemaQosRtb;
    
    private ViewGroup mExpandHospitalView;
    
    private EditText mHospitalAvgEdt;
    
    private RatingBar mHospitalQosRtb;
    
    private RatingBar mLevelRtb;
    
    private ViewGroup mExpandBuyView;
    
    private EditText mBuyAvgEdt;
    
    private Comment mComment = new Comment();
    
    private static List<POI> sPOIList = new ArrayList<POI>();
    private static List<Integer> sFromViewIdList = new ArrayList<Integer>();
    private static List<Integer> sStatusList = new ArrayList<Integer>();
    
    private POI mPOI = null;
    
    private int mFromViewId;
    
    private class MyLoginCallBack implements LoginCallBack {

        String type;
        
        public MyLoginCallBack(String type) {
            this.type = type;
        }
        
        @Override
        public void onSuccess() {
            POIComment.this.runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                    if (ShareAPI.TYPE_WEIBO.equals(type)) {
                        UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(mThis, ShareAPI.TYPE_WEIBO);
                        if (userAccessIdenty != null) {
                            mSyncSinaChb.setChecked(true);
                        } else {
                            mSyncSinaChb.setChecked(false);
                        }
                    } if (ShareAPI.TYPE_TENCENT.equals(type)) {
                        UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(mThis, ShareAPI.TYPE_TENCENT);
                        if (userAccessIdenty != null) {
                            mSyncQZoneChb.setChecked(true);
                        } else {
                            mSyncQZoneChb.setChecked(false);
                        }
                    }
                }
            });
        }

        @Override
        public void onFailed() {
        }

        @Override
        public void onCancel() {
        }
    }
    
    TKWeibo mTKWeibo = null;
    
    private AuthDialogListener mSinaAuthDialogListener;
    
    private AuthReceiver mTencentAuthReceiver;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIComment;
        mId = R.id.activity_poi_comment;
        
        synchronized (sPOIList) {
            int size = sPOIList.size();
            if (size > 0) {
                mPOI = sPOIList.get(size-1);
                mFromViewId = sFromViewIdList.get(size-1);
                mStatus = sStatusList.get(size-1);
            }
            if (mPOI == null) {
                finish();
                return;
            }
        }

        setContentView(R.layout.comment);
        
        findViews();
        setListener();
        
        mTKWeibo = new TKWeibo(mThis, true, false);
        mSinaAuthDialogListener = new AuthDialogListener(mTKWeibo, new MyLoginCallBack(ShareAPI.TYPE_WEIBO));
        mTencentAuthReceiver = new AuthReceiver(mThis, new MyLoginCallBack(ShareAPI.TYPE_TENCENT));
        registerIntentReceivers();
        
        mTitleBtn.setText(mStatus == STATUS_NEW ? R.string.publish_comment : R.string.modify_comment);
        mRightBtn.setBackgroundResource(R.drawable.btn_submit_comment);
        Comment comment = mPOI.getMyComment();
        if (Comment.isAuthorMe(comment) <= 0) {
            comment = new Comment();
            mPOI.setMyComment(comment);
        }
        mComment = comment;
        long commentPattern = mPOI.getCommentPattern();

        int grade = (int) (mComment.getGrade()/2);
        mGradeRtb.setRating(grade);
        
        String content = mComment.getContent();
        if (!TextUtils.isEmpty(content)) {
            mContentEdt.setText(content);
            if (mStatus == STATUS_MODIFY) {
                mContentEdt.setSelection(content.length());
            }
        } else {
            mContentEdt.setText(null);
        }
        mContentEdt.setFilters(new InputFilter[] { new InputFilter.LengthFilter(800) });
        
    	UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(mThis, ShareAPI.TYPE_TENCENT);
        if (userAccessIdenty != null) {
            mSyncQZoneChb.setChecked(true);
        } else {
            mSyncQZoneChb.setChecked(false);
        }
        userAccessIdenty = ShareAPI.readIdentity(mThis, ShareAPI.TYPE_WEIBO);
        if (userAccessIdenty != null) {
            mSyncSinaChb.setChecked(true);
        } else {
            mSyncSinaChb.setChecked(false);
        }

        mExpandFoodView.setVisibility(View.GONE);
        mExpandHotelView.setVisibility(View.GONE);
        mExpandCinemaView.setVisibility(View.GONE);
        mExpandHospitalView.setVisibility(View.GONE);
        mExpandBuyView.setVisibility(View.GONE);
        if (POI.COMMENT_PATTERN_FOOD == commentPattern) {
            mExpandView.setVisibility(View.VISIBLE);
            mExpandFoodView.setVisibility(View.VISIBLE);
            mMoreTxv.setText(R.string.comment_food_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mFoodAvgEdt.setText(String.valueOf(avg));
            } else {
                mFoodAvgEdt.setText(null);
            }
            mTasteRtb.setRating((int)mComment.getTaste());
            mFoodEnvironmentRtb.setRating((int)mComment.getEnvironment());
            mFoodQosRtb.setRating((int)mComment.getQos());
            String restair = mComment.getRestair();
            if (!TextUtils.isEmpty(restair)) {
                mRestairBtn.setText(restair);
            } else {
                mRestairBtn.setText(null);
            }
            
            String recommend = mComment.getRecommend();
            if (!TextUtils.isEmpty(recommend)) {
                mRecommendEdt.setText(recommend);
            } else {
                mRecommendEdt.setText(null);
            }
        } else if (POI.COMMENT_PATTERN_HOTEL == commentPattern) {
            mExpandView.setVisibility(View.VISIBLE);
            mExpandHotelView.setVisibility(View.VISIBLE);
            mMoreTxv.setText(R.string.comment_hotel_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mHotelAvgEdt.setText(String.valueOf(avg));
            } else {
                mHotelAvgEdt.setText(null);
            }
            mHotelQosRtb.setRating((int)mComment.getQos());
            mHotelEnvironmentRtb.setRating((int)mComment.getEnvironment());
        } else if (POI.COMMENT_PATTERN_CINEMA == commentPattern) {
            mExpandView.setVisibility(View.VISIBLE);
            mExpandCinemaView.setVisibility(View.VISIBLE);
            mMoreTxv.setText(R.string.comment_cinema_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mCinemaAvgEdt.setText(String.valueOf(avg));
            } else {
                mCinemaAvgEdt.setText(null);
            }
            mEffectRtb.setRating((int)mComment.getEffect());
            mCinemaQosRtb.setRating((int)mComment.getQos());
        } else if (POI.COMMENT_PATTERN_HOSPITAL == commentPattern) {
            mExpandView.setVisibility(View.VISIBLE);
            mExpandHospitalView.setVisibility(View.VISIBLE);
            mMoreTxv.setText(R.string.comment_hospital_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mHospitalAvgEdt.setText(String.valueOf(avg));
            } else {
                mHospitalAvgEdt.setText(null);
            }
            mLevelRtb.setRating((int)mComment.getLevel());
            mHospitalQosRtb.setRating((int)mComment.getQos());
        } else if (POI.COMMENT_PATTERN_BUY == commentPattern) {
            mExpandView.setVisibility(View.VISIBLE);
            mExpandBuyView.setVisibility(View.VISIBLE);
            mMoreTxv.setText(R.string.comment_buy_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mBuyAvgEdt.setText(String.valueOf(avg));
            } else {
                mBuyAvgEdt.setText(null);
            }
        } else {
            mExpandView.setVisibility(View.GONE);
        }
    }
    
    protected boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        return isRelogin;
    }
    
    @Override
    public void onResume() {
        super.onResume();        
        if (isReLogin()) {
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    private void save() {
        mComment.setGrade((int)(mGradeRtb.getRating()*2));
        String content = mContentEdt.getEditableText().toString();
        if (!TextUtils.isEmpty(content)) {
            mComment.setContent(content);
        }
        long commentPattern = mPOI.getCommentPattern();
        if (POI.COMMENT_PATTERN_FOOD == commentPattern) {
            String avg = mFoodAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
            mComment.setTaste((int) mTasteRtb.getRating());
            mComment.setEnvironment((int)mFoodEnvironmentRtb.getRating());
            mComment.setQos((int)mFoodQosRtb.getRating());
            
            String restair = mRestairBtn.getText().toString().trim();
            if (!TextUtils.isEmpty(restair)) {
                mComment.setRestair(restair);
            }
            
            String recommend = mRecommendEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(recommend)) {
                mComment.setRecommend(recommend);
            }
        } else if (POI.COMMENT_PATTERN_HOTEL == commentPattern) {
            String avg = mHotelAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
            mComment.setQos((int)mHotelQosRtb.getRating());
            mComment.setEnvironment((int)mHotelEnvironmentRtb.getRating());
        } else if (POI.COMMENT_PATTERN_CINEMA == commentPattern) {
            String avg = mCinemaAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
            mComment.setEffect((int)mEffectRtb.getRating());
            mComment.setQos((int)mCinemaQosRtb.getRating());
        } else if (POI.COMMENT_PATTERN_HOSPITAL == commentPattern) {
            String avg = mHospitalAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
            mComment.setQos((int)mHospitalQosRtb.getRating());
            mComment.setLevel((int)mLevelRtb.getRating());
        } else if (POI.COMMENT_PATTERN_BUY == commentPattern) {
            String avg = mBuyAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
        }
        
        long userId = Long.MIN_VALUE;
        User user = Globals.g_User;            
        if (user != null) {
            userId = user.getUserId();
            mComment.setUserId(userId);
        } else {
            mComment.setUserId(-1);
        }
        mComment.setClientUid(Globals.g_ClientUID);
    }
    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mTencentAuthReceiver != null) {
            unregisterIntentReceivers();
        }
    }
    
    
    private void registerIntentReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(TAuthView.AUTH_BROADCAST);
        registerReceiver(mTencentAuthReceiver, filter);
    }
    
    private void unregisterIntentReceivers() {
        unregisterReceiver(mTencentAuthReceiver);
    }

	protected void findViews() {
        super.findViews();
        mGradeRtb = (RatingBar) findViewById(R.id.grade_rtb);
        mGradeTipTxv = (TextView)this.findViewById(R.id.grade_tip_txv);
        mContentEdt = (EditText) findViewById(R.id.content_edt);
        mTextNumTxv = (TextView)this.findViewById(R.id.text_limit_txv);
        mSyncQZoneChb = (CheckBox) findViewById(R.id.sync_qzone_chb);
        mSyncSinaChb = (CheckBox) findViewById(R.id.sync_sina_chb);

        mExpandView = (ViewGroup) findViewById(R.id.expand_view);
        mExpandFoodView = (ViewGroup) findViewById(R.id.food_view);
        mMoreTxv = (TextView) findViewById(R.id.more_txv);
        mFoodAvgEdt = (EditText) mExpandFoodView.findViewById(R.id.avg_edt);
        mTasteRtb = (RatingBar) mExpandFoodView.findViewById(R.id.taste_rbt);
        mFoodEnvironmentRtb = (RatingBar) mExpandFoodView.findViewById(R.id.environment_rbt);
        mFoodQosRtb = (RatingBar)mExpandFoodView.findViewById(R.id.qos_rbt);
        mRestairBtn = (Button) mExpandFoodView.findViewById(R.id.restair_btn);
        mRecommendEdt = (EditText) mExpandFoodView.findViewById(R.id.recommend_edt);
        
        mExpandHotelView = (ViewGroup) findViewById(R.id.hotel_view);
        mHotelAvgEdt = (EditText) mExpandHotelView.findViewById(R.id.avg_edt);
        mHotelQosRtb = (RatingBar)mExpandHotelView.findViewById(R.id.qos_rbt);
        mHotelEnvironmentRtb = (RatingBar) mExpandHotelView.findViewById(R.id.environment_rbt);
        
        mExpandCinemaView = (ViewGroup) findViewById(R.id.cinema_view);
        mCinemaAvgEdt = (EditText) mExpandCinemaView.findViewById(R.id.avg_edt);
        mEffectRtb = (RatingBar) mExpandCinemaView.findViewById(R.id.effect_rbt);
        mCinemaQosRtb = (RatingBar)mExpandCinemaView.findViewById(R.id.qos_rbt);
        
        mExpandHospitalView = (ViewGroup) findViewById(R.id.hospital_view);
        mHospitalAvgEdt = (EditText) mExpandHospitalView.findViewById(R.id.avg_edt);
        mHospitalQosRtb = (RatingBar)mExpandHospitalView.findViewById(R.id.qos_rbt);
        mLevelRtb = (RatingBar) mExpandHospitalView.findViewById(R.id.level_rbt);
        
        mExpandBuyView = (ViewGroup) findViewById(R.id.buy_view);
        mBuyAvgEdt = (EditText) mExpandBuyView.findViewById(R.id.avg_edt);
    }

    protected void setListener() {
        super.setListener();
        mLeftBtn.setOnClickListener(this);
        mRightBtn.setOnClickListener(this);
        mMoreTxv.setOnClickListener(this);
        mRestairBtn.setOnClickListener(this);
        OnTouchListener onTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                final int action = ev.getAction();

                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP: {
                        if (R.id.taste_rbt == v.getId()) {
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "taste");
                        } else if (R.id.environment_rbt == v.getId()) {
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "environment");
                        } else if (R.id.qos_rbt == v.getId()) {
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "qos");
                        }
                        break;
                    }
                }
                return false;
            }
        };
        mTasteRtb.setOnTouchListener(onTouchListener);
        mFoodEnvironmentRtb.setOnTouchListener(onTouchListener);
        mFoodQosRtb.setOnTouchListener(onTouchListener);
        mHotelEnvironmentRtb.setOnTouchListener(onTouchListener);
        mHotelQosRtb.setOnTouchListener(onTouchListener);
        mCinemaQosRtb.setOnTouchListener(onTouchListener);
        mHospitalQosRtb.setOnTouchListener(onTouchListener);
        OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View v, boolean value) {
                if (value) {
                    switch (v.getId()) {
                        case R.id.content_edt:
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "content");
                            break;
                            
                        case R.id.avg_edt:
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "avg");                        
                            break;
                            
                        case R.id.recommend_edt:
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "recommend");  
                            break;
    
                        default:
                            break;
                    }
                }
            }
        };
        mContentEdt.setOnFocusChangeListener(onFocusChangeListener);
        mFoodAvgEdt.setOnFocusChangeListener(onFocusChangeListener);
        mRecommendEdt.setOnFocusChangeListener(onFocusChangeListener);
        mHotelAvgEdt.setOnFocusChangeListener(onFocusChangeListener);
        mCinemaAvgEdt.setOnFocusChangeListener(onFocusChangeListener);
        mHospitalAvgEdt.setOnFocusChangeListener(onFocusChangeListener);
        mBuyAvgEdt.setOnFocusChangeListener(onFocusChangeListener);
        mContentEdt.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String mText = mContentEdt.getText().toString().trim();
                int len = mText.length();
                if (len >= MIN_CHAR) {
                    String lenStr = String.valueOf(len);
                    mTextNumTxv.setText(mThis.getString(R.string.poi_comment_sum, lenStr));
                } else {
                    String lenStr = String.valueOf(30-len);
                    SpannableStringBuilder style = new SpannableStringBuilder(mThis.getString(R.string.poi_comment_must, lenStr));
                    style.setSpan(new ForegroundColorSpan(0xffff0000),2,2+lenStr.length(),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                    mTextNumTxv.setText(style);
                }
            }
        });
        mSyncSinaChb.setOnClickListener(this);
        mSyncQZoneChb.setOnClickListener(this);
        mGradeRtb.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftInput();
                return false;
            }
        });
        mTasteRtb.setOnRatingBarChangeListener(new OnRatingBarChangeListener(){
        	@Override
        	public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromTouch){
        		if(fromTouch && rating==0){
        			mTasteRtb.setRating(1);
        		}
        	}
        });
        mFoodEnvironmentRtb.setOnRatingBarChangeListener(new OnRatingBarChangeListener(){
        	@Override
        	public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromTouch){
        		if(fromTouch && rating==0){
        			mFoodEnvironmentRtb.setRating(1);
        		}
        	}
        });
        
        mFoodQosRtb.setOnRatingBarChangeListener(new OnRatingBarChangeListener(){
        	@Override
        	public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromTouch){
        		if(fromTouch && rating==0){
        			mFoodQosRtb.setRating(1);
        		}
        	}
        });
        
        
        mGradeRtb.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
            
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromTouch) {
                if (fromTouch) {
                    Animation animation = AnimationUtils.loadAnimation(mThis, R.anim.grade_tip);
                    int resId = R.string.poi_comment_share_grade1;
                    if (rating == 2) {
                        resId = R.string.poi_comment_share_grade2;
                    } else if (rating == 3) {
                        resId = R.string.poi_comment_share_grade3;
                    } else if (rating == 4) {
                        resId = R.string.poi_comment_share_grade4;
                    } else if (rating == 5) {
                        resId = R.string.poi_comment_share_grade5;
                    } else if (rating == 0) {
                    	resId = R.string.poi_comment_share_grade1;
                    	mGradeRtb.setRating(1);
                    }
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "grade", rating);
                    mGradeTipTxv.setText(resId);
                    mGradeTipTxv.setVisibility(View.VISIBLE);
                    mGradeTipTxv.startAnimation(animation);
                    animation.setAnimationListener(new AnimationListener() {
                        
                        @Override
                        public void onAnimationStart(Animation arg0) {
                            // TODO Auto-generated method stub
                            
                        }
                        
                        @Override
                        public void onAnimationRepeat(Animation arg0) {
                            // TODO Auto-generated method stub
                            
                        }
                        
                        @Override
                        public void onAnimationEnd(Animation arg0) {
                            mGradeTipTxv.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
    
    public static void setPOI(POI poi, int fromViewId, int status) {
        synchronized (sPOIList) {
            sPOIList.add(poi);
            sFromViewIdList.add(fromViewId);
            sStatusList.add(status);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mOnActivityResultSubmitByLogin = false;
        if (requestCode == R.id.activity_user_update_nickname) {
            submit();
        } else if (requestCode == R.id.activity_user_comment_after) {
            submitByLogin(data);
            mOnActivityResultSubmitByLogin = true;
        }
    }
    
    private void readySubmit() {
        User user = Globals.g_User;
        if (user == null && mStatus != STATUS_MODIFY) {
            Intent intent = new Intent();
            intent.setClass(mThis, UserCommentAfterActivity.class);
            intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, mId);
            startActivityForResult(intent, R.id.activity_user_comment_after);
        } else if (user != null && user.isNickNameDefault(mThis)) {
            Intent intent = new Intent();
            intent.setClass(mThis, UserUpdateNickNameActivity.class);
            intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, mId);
            startActivityForResult(intent, R.id.activity_user_update_nickname);
        } else {
            submit();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case R.id.dialog_share_doing:
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            ((ProgressDialog)dialog).setMessage(getString(R.string.doing_and_wait));
            break;
        }
        
        return dialog;
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (R.id.right_btn == viewId) {
            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "titleRight");
            if (mContentEdt.getEditableText().toString().trim().length() < MIN_CHAR) {
                CommonUtils.showNormalDialog(mThis, 
                        mThis.getString(R.string.prompt), 
                        mThis.getString(R.string.comment_prompt_input_content), 
                        mThis.getString(R.string.confirm),
                        null,
                        new DialogInterface.OnClickListener() {
                    
		                    @Override
		                    public void onClick(DialogInterface arg0, int arg1) {
		                        mContentEdt.requestFocus();
		                    }
		                });
                return;
            }
            if (mStatus == STATUS_MODIFY) {
                CommonUtils.showNormalDialog(mThis, 
                        mThis.getString(R.string.prompt), 
                        mThis.getString(R.string.poi_comment_override_tip), 
                        mThis.getString(R.string.confirm),
                        null,
                        new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        readySubmit();
                    }
                });
            } else {
                readySubmit();
            }
            
        } else if (R.id.restair_btn == viewId) {
            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "restair");
            if (mRestairArray == null) {
                mRestairArray = mThis.getResources().getStringArray(R.array.comment_restair);
                mRestairChecked = new boolean[mRestairArray.length];
            }
            for(int i = 0, length = mRestairArray.length; i < length; i++) {
                mRestairChecked[i] = false;
            }
            String restair = mRestairBtn.getText().toString().trim();
            if (!TextUtils.isEmpty(restair)) {
                int i = 0;
                for(String str : mRestairArray) {
                    if (restair.contains(str)) {
                        mRestairChecked[i] = true;
                    }
                    i++;
                }
            }
            
            final MultichoiceArrayAdapter multichoiceArrayAdapter = new MultichoiceArrayAdapter(mThis, mRestairArray);
            ListView listView = CommonUtils.makeListView(mThis);
            listView.setAdapter(multichoiceArrayAdapter);
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    mRestairChecked[position] = !mRestairChecked[position];
                    multichoiceArrayAdapter.notifyDataSetChanged();
                }
            });
            
            CommonUtils.showNormalDialog(mThis, 
                    mThis.getString(R.string.comment_food_restair), 
                    listView,
                    new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int id) {

                            if (id == DialogInterface.BUTTON_POSITIVE) {
                                StringBuilder s = new StringBuilder();
                                for(int i = 0, length = mRestairChecked.length; i < length; i++) {
                                    if (mRestairChecked[i]) {
                                        if (s.length() > 0) {
                                            s.append(',');
                                        }
                                        s.append(mRestairArray[i]);
                                    }
                                }
                                mRestairBtn.setText(s.toString());
                            }
                        }
                    });
        } else if (viewId == R.id.left_btn) {
            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "titleLeft");
            if (showDiscardDialog() == false) {
                finish();
            }
        } else if (viewId == R.id.sync_qzone_chb) { 
            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "qzone", mSyncQZoneChb.isChecked());
            if (mSyncQZoneChb.isChecked() == true) {
                UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(mThis, ShareAPI.TYPE_TENCENT);
                if (userAccessIdenty != null) {
                    mSyncQZoneChb.setChecked(true);
                } else {
                    mSyncQZoneChb.setChecked(false);
                    TKTencentOpenAPI.login(mThis);
                }
            } else {
                mSyncQZoneChb.setChecked(false);
            }
        } else if (viewId == R.id.sync_sina_chb) {
            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "sina", mSyncSinaChb.isChecked());
            if (mSyncSinaChb.isChecked() == true) {
                UserAccessIdenty userAccessIdenty = ShareAPI.readIdentity(mThis, ShareAPI.TYPE_WEIBO);
                if (userAccessIdenty != null) {
                    mSyncSinaChb.setChecked(true);
                } else {
                    mSyncSinaChb.setChecked(false);
                    TKWeibo.authorize(mTKWeibo, mSinaAuthDialogListener);
                }
            } else {
                mSyncSinaChb.setChecked(false);
            }
        }
    }
    
    private void submit() {
        hideSoftInput();
        int resId = R.string.poi_comment_share_grade1;
        int rating = (int)mGradeRtb.getRating();
        if (rating == 2) {
            resId = R.string.poi_comment_share_grade2;
        } else if (rating == 3) {
            resId = R.string.poi_comment_share_grade3;
        } else if (rating == 4) {
            resId = R.string.poi_comment_share_grade4;
        } else if (rating == 5) {
            resId = R.string.poi_comment_share_grade5;
        } else {
            resId = R.string.poi_comment_share_grade1;
        }
        String shareGrade = getString(resId);
        
        String recommendCook = "";
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_DIANPING);
        StringBuilder s = new StringBuilder();
        s.append(Util.byteToHexString(Comment.FIELD_GRADE));
        s.append(':');
        s.append(rating*2);
        s.append(',');
        String content = mContentEdt.getEditableText().toString().trim();
        if (!TextUtils.isEmpty(content)) {
            s.append(Util.byteToHexString(Comment.FIELD_CONTENT));
            s.append(':');
            try {
                s.append(URLEncoder.encode(content, TKConfig.getEncoding()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(mThis, R.string.app_name, Toast.LENGTH_LONG).show();
        }
        long commentPattern = mPOI.getCommentPattern();
        if (POI.COMMENT_PATTERN_FOOD == commentPattern) {
            String avg = mFoodAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_AVG));
                s.append(':');
                s.append(avg);
            }
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_TASTE));
            s.append(':');
            s.append((int)mTasteRtb.getRating());
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_ENVIRONMENT));
            s.append(':');
            s.append((int)mFoodEnvironmentRtb.getRating());
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_QOS));
            s.append(':');
            s.append((int)mFoodQosRtb.getRating());
            String restair = mRestairBtn.getText().toString();
            if (!TextUtils.isEmpty(restair)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_RESTAIR));
                s.append(':');
                s.append(URLEncoder.encode(restair));
            }
            
            recommendCook = mRecommendEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(recommendCook)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_RECOMMEND));
                s.append(':');
                try {
                    s.append(URLEncoder.encode(recommendCook, TKConfig.getEncoding()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (POI.COMMENT_PATTERN_HOTEL == commentPattern) {
            String avg = mHotelAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_AVG));
                s.append(':');
                s.append(avg);
            }
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_QOS));
            s.append(':');
            s.append((int)mHotelQosRtb.getRating());
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_ENVIRONMENT));
            s.append(':');
            s.append((int)mHotelEnvironmentRtb.getRating());
        } else if (POI.COMMENT_PATTERN_CINEMA == commentPattern) {
            String avg = mCinemaAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_AVG));
                s.append(':');
                s.append(avg);
            }
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_EFFECT));
            s.append(':');
            s.append((int)mEffectRtb.getRating());
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_QOS));
            s.append(':');
            s.append((int)mCinemaQosRtb.getRating());
        } else if (POI.COMMENT_PATTERN_HOSPITAL == commentPattern) {
            String avg = mHospitalAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_AVG));
                s.append(':');
                s.append(avg);
            }
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_QOS));
            s.append(':');
            s.append((int)mHospitalQosRtb.getRating());
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_LEVEL));
            s.append(':');
            s.append((int)mLevelRtb.getRating());
        } else if (POI.COMMENT_PATTERN_BUY == commentPattern) {
            String avg = mBuyAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                s.append(',');
                s.append(Util.byteToHexString(Comment.FIELD_AVG));
                s.append(':');
                s.append(avg);
            }
        }
        s.append(',');
        s.append(Util.byteToHexString(Comment.FIELD_PUID));
        s.append(':');
        s.append(mPOI.getUUID());
        s.append(',');
        s.append(Util.byteToHexString(Comment.FIELD_SOURCE));
        s.append(':');
        s.append(TKConfig.sCommentSource);
        if (mStatus == STATUS_MODIFY && TextUtils.isEmpty(mComment.getUid()) == false) {
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_UPDATE);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mComment.getUid());
        } else {
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_CREATE);
        }
        criteria.put(DataOperation.SERVER_PARAMETER_ENTITY, s.toString());
        DataOperation dataOperation = new DataOperation(mThis);
        dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), -1, mFromViewId, mThis.getString(R.string.comment_publishing_and_wait));

        if (mSyncSinaChb.isChecked()) {
            // http://open.weibo.com/wiki/2/statuses/update
            // status  true  string  要发布的微博文本内容，必须做URLencode，内容不超过140个汉字。   
            String shareSina = "";
            shareSina = mThis.getString(R.string.poi_comment_share_sina, mPOI.getName(), shareGrade, content, TextUtils.isEmpty(recommendCook) ? "" : mThis.getString(R.string.recommend_cooking, recommendCook));
            if (shareSina.length() > 115) {
                shareSina = shareSina.subSequence(0, 112) + "...";
            }
            shareSina = shareSina + "http://www.tigerknows.com";   // 25个 TODO: 但是这个网址被微博删除了
            criteria.put(DataOperation.SERVER_PARAMETER_SHARE_SINA, shareSina);
        }
        
        if (mSyncQZoneChb.isChecked()) {
            // http://wiki.opensns.qq.com/wiki/%E3%80%90QQ%E7%99%BB%E5%BD%95%E3%80%91add_share
            // summary   string  所分享的网页资源的摘要内容，或者是网页的概要描述，对应上文接口说明的3。最长80个中文字，超出部分会被截断。  
            String shareQzone = "";
            shareQzone = mThis.getString(R.string.poi_comment_share_qzone, mPOI.getName(), shareGrade, content, TextUtils.isEmpty(recommendCook) ? "" : mThis.getString(R.string.recommend_cooking, recommendCook));
            if (shareQzone.length() > 63) {
                shareQzone = shareQzone.subSequence(0, 60) + "...";
            }
            shareQzone = shareQzone + mThis.getString(R.string.poi_comment_share_qzone_source); // 17个
            criteria.put(DataOperation.SERVER_PARAMETER_SHARE_QZONE, shareQzone);
        }
        queryStart(dataOperation, false);
    }
    
    private boolean showDiscardDialog() {
        boolean show = false;
        if (mStatus == STATUS_NEW) {
            if (mContentEdt.getEditableText().toString().trim().length() > 0) {
                show = true;
            }
        } else if (mStatus == STATUS_MODIFY && isModify()) {
            show = true;
        }
        
        if (show == true) {
            CommonUtils.showNormalDialog(mThis,
                    getString(mStatus == STATUS_NEW ? R.string.comment_discard_tip_new : R.string.comment_discard_tip_modify),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (whichButton == DialogInterface.BUTTON_POSITIVE) {
                                Intent data = new Intent();
                                data.putExtra(Sphinx.REFRESH_POI_DETAIL, true);
                                setResult(Activity.RESULT_CANCELED, data);
                                finish();
                            }
                        }
                    });
        }
        return show;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        submitByLogin(intent);
    }

    boolean mOnActivityResultSubmitByLogin = false;
    private void submitByLogin(Intent data) {
        if (mOnActivityResultSubmitByLogin) {
            mOnActivityResultSubmitByLogin = false;
            return;
        }
        User user = Globals.g_User;
        if (user != null && user.isNickNameDefault(mThis) && (data == null || data.getIntExtra(SOURCE_VIEW_ID, R.id.view_invalid) != R.id.activity_user_update_nickname)) {
            Intent intent = new Intent();
            intent.setClass(mThis, UserUpdateNickNameActivity.class);
            intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, mId);
            startActivityForResult(intent, R.id.activity_user_update_nickname);
        } else {
            submit();
        }
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        final DataOperation commentOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        final Response response = commentOperation.getResponse();
        if (BaseActivity.checkReLogin(commentOperation, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            mStatus = STATUS_NEW;
            mTitleBtn.setText(R.string.publish_comment);
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(commentOperation, mThis, new int[] {201, 601, 602, 603}, true, mThis, false)) {
            if (response != null) {
                if (response.getResponseCode() == 201) {
                    BaseActivity.showErrorDialog(mThis, mThis.getString(R.string.response_code_201), mThis, true);
                } else if (response.getResponseCode() == 601 && response instanceof CommentCreateResponse) {
                    mPOI.setAttribute(POI.ATTRIBUTE_COMMENT_USER);
                    mPOI.setMyComment(mComment);
                    mStatus = STATUS_MODIFY;
                    mTitleBtn.setText(R.string.modify_comment);
                    mComment.setUid(((CommentCreateResponse)response).getUid());
                    CommonUtils.showNormalDialog(mThis, 
                            mThis.getString(R.string.prompt), 
                            mThis.getString(R.string.response_code_601), 
                            mThis.getString(R.string.confirm),
                            null,
                            new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            User user = Globals.g_User;
                            if (user != null) {
                            Hashtable<String, String> criteria = commentOperation.getCriteria();
                            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_UPDATE);
                            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mComment.getUid());
                            DataOperation dataOperation = new DataOperation(mThis);
                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), -1, mFromViewId, mThis.getString(R.string.doing_and_wait));
                            queryStart(dataOperation);
                            }
                        }
                    });
                } else if (response.getResponseCode() == 602) {
                    mStatus = STATUS_NEW;
                    mTitleBtn.setText(R.string.publish_comment);
                    Hashtable<String, String> criteria = commentOperation.getCriteria();
                    criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_CREATE);
                    DataOperation dataOperation = new DataOperation(mThis);
                    dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), -1, mFromViewId, mThis.getString(R.string.doing_and_wait));
                    queryStart(dataOperation);
                } else if (response.getResponseCode() == 603) {
                    BaseActivity.showErrorDialog(mThis, mThis.getString(R.string.response_code_603), mThis, true);
                }
            }
            return;
        }

        save();
        if (response instanceof CommentCreateResponse) {
            CommentCreateResponse commentCreateResponse = (CommentCreateResponse)response;
            mComment.setUid(commentCreateResponse.getUid());
            mComment.setTime(commentCreateResponse.getTimeStamp());
        } else if (response instanceof CommentUpdateResponse) {
            CommentUpdateResponse commentUpdateResponse = (CommentUpdateResponse)response;
            mComment.setTime(commentUpdateResponse.getTimeStamp());
        }
        
        User user = Globals.g_User;            
        if (user != null) {
            long userId = Long.MIN_VALUE;
            userId = user.getUserId();
            mComment.setUserId(userId);
            mComment.getPOI().setAttribute(POI.ATTRIBUTE_COMMENT_USER);
            mPOI.setAttribute(POI.ATTRIBUTE_COMMENT_USER);
            mComment.setUser(user.getNickName());
        } else {
            mComment.getPOI().setAttribute(POI.ATTRIBUTE_COMMENT_ANONYMOUS);
            mPOI.setAttribute(POI.ATTRIBUTE_COMMENT_ANONYMOUS);
            mComment.setUserId(-1);
            mComment.setUser(mThis.getString(R.string.default_guest_name));
        }
        mComment.setClientUid(Globals.g_ClientUID);
        
        mPOI.setMyComment(mComment);
        
        // 如果没有最近点评或者最近这条点评是我的，则更新它
        if (mPOI.getLastComment() == null || Comment.isAuthorMe(mPOI.getLastComment()) > 0) {
            mPOI.setLastComment(mComment);
            mComment.setData(null);
            mPOI.setData(null);
            mPOI.updateData(mThis, mPOI.getData());
        }
        
        // 如果以前查看过点评列表，则更新列表中属于我的那条点评信息
        DataQuery dataQuery = mPOI.getCommentQuery();
        if (dataQuery != null) {
            CommentResponse commentResponse = (CommentResponse)dataQuery.getResponse();
            if (commentResponse != null) {
                CommentList commentList = commentResponse.getList();
                if (commentList != null) {
                    List<Comment> list = commentList.getList();
                    if (list != null) {
                        for(int i = list.size()-1; i >= 0; i--) {
                            // 如果列表中已经有我的点评则将其删除
                            if (Comment.isAuthorMe(list.get(i)) > 0) {
                                list.remove(i);
                                break;
                            }
                        }
                        // 将我的点评插入为第一条
                        list.add(0, mComment);
                    }
                }
            }
        }
        
        mPOI.update(mThis, mPOI.getStoreType());
        Intent intent = new Intent(POIComment.this, Sphinx.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID, mId);
        startActivity(intent);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (showDiscardDialog() == false) {
                mActionLog.addAction(ActionLog.KEYCODE, "back");
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void finish() {
        synchronized (sPOIList) {
            int size = sPOIList.size();
            if (size > 0) {
                sPOIList.remove(size-1);
                sFromViewIdList.remove(size-1);
                sStatusList.remove(size-1);
            }
        }
        super.finish();        
    }
    
    private boolean isModify() {
        if ((int)mGradeRtb.getRating()*2 != mComment.getGrade()) {
            return true;
        }
        String content = mContentEdt.getEditableText().toString();
        if (TextUtils.isEmpty(content) == false) {
            if (content.equals(mComment.getContent()) == false) {
                return true;
            }
        } else if (TextUtils.isEmpty(mComment.getContent()) == false) {
            return true;
        }
        
        long commentPattern = mPOI.getCommentPattern();
        if (POI.COMMENT_PATTERN_FOOD == commentPattern) {
            String avg = mFoodAvgEdt.getEditableText().toString().trim();
            long avgLong = -1;
            if (TextUtils.isEmpty(avg) == false) {
                avgLong = Long.parseLong(avg);
            }
            if (avgLong != mComment.getAvg()) {
                return true;
            }
            if ((int)mTasteRtb.getRating() != mComment.getTaste()) {
                return true;
            }
            if ((int)mFoodEnvironmentRtb.getRating() != mComment.getEnvironment()) {
                return true;
            }
            if ((int)mFoodQosRtb.getRating() != mComment.getQos()) {
                return true;
            }
            
            String restair = mRestairBtn.getText().toString().trim();
            if (TextUtils.isEmpty(restair) == false) {
                if (restair.equals(mComment.getRestair()) == false) {
                    return true;
                }
            } else if (TextUtils.isEmpty(mComment.getRestair()) == false) {
                return true;
            }
            
            String recommend = mRecommendEdt.getEditableText().toString().trim();
            if (TextUtils.isEmpty(recommend) == false) {
                if (recommend.equals(mComment.getRecommend()) == false) {
                    return true;
                }
            } else if (TextUtils.isEmpty(mComment.getRecommend()) == false) {
                return true;
            }
        } else if (POI.COMMENT_PATTERN_HOTEL == commentPattern) {
            String avg = mHotelAvgEdt.getEditableText().toString().trim();
            long avgLong = -1;
            if (TextUtils.isEmpty(avg) == false) {
                avgLong = Long.parseLong(avg);
            }
            if (avgLong != mComment.getAvg()) {
                return true;
            }
            if ((int)mHotelQosRtb.getRating() != mComment.getQos()) {
                return true;
            }
            if ((int)mHotelEnvironmentRtb.getRating() != mComment.getEnvironment()) {
                return true;
            }
        } else if (POI.COMMENT_PATTERN_CINEMA == commentPattern) {
            String avg = mCinemaAvgEdt.getEditableText().toString().trim();
            long avgLong = -1;
            if (TextUtils.isEmpty(avg) == false) {
                avgLong = Long.parseLong(avg);
            }
            if (avgLong != mComment.getAvg()) {
                return true;
            }
            if ((int)mEffectRtb.getRating() != mComment.getEffect()) {
                return true;
            }
            if ((int)mCinemaQosRtb.getRating() != mComment.getQos()) {
                return true;
            }
        } else if (POI.COMMENT_PATTERN_HOSPITAL == commentPattern) {
            String avg = mHospitalAvgEdt.getEditableText().toString().trim();
            long avgLong = -1;
            if (TextUtils.isEmpty(avg) == false) {
                avgLong = Long.parseLong(avg);
            }
            if (avgLong != mComment.getAvg()) {
                return true;
            }
            if ((int)mHospitalQosRtb.getRating() != mComment.getQos()) {
                return true;
            }
            if ((int)mLevelRtb.getRating() != mComment.getLevel()) {
                return true;
            }
        } else if (POI.COMMENT_PATTERN_BUY == commentPattern) {
            String avg = mBuyAvgEdt.getEditableText().toString().trim();
            long avgLong = -1;
            if (TextUtils.isEmpty(avg) == false) {
                avgLong = Long.parseLong(avg);
            }
            if (avgLong != mComment.getAvg()) {
                return true;
            }
        }
        return false;
    }
    
    class MultichoiceArrayAdapter extends ArrayAdapter<String> {
        
        static final int RESOURCE_ID = R.layout.select_dialog_multichoice;
        
        public MultichoiceArrayAdapter(Context context, String[] list) {
            super(context, RESOURCE_ID, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            CheckedTextView textView = (CheckedTextView)view.findViewById(R.id.text1);
            textView.setText(mRestairArray[position]);
            textView.setChecked(mRestairChecked[position]);

            return view;
        }
    }
}

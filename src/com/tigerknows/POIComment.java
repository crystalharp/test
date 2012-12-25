/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataOperation.CommentCreateResponse;
import com.tigerknows.model.DataOperation.CommentUpdateResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.share.IBaseShare;
import com.tigerknows.share.ShareEntrance;
import com.tigerknows.share.ShareMessageCenter;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.user.User;
import com.tigerknows.view.user.UserBaseActivity;
import com.tigerknows.view.user.UserCommentAfterActivity;
import com.tigerknows.view.user.UserUpdateNickNameActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
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
    
    private ViewGroup mGradeView;
    
    private ViewGroup mContentView;
    
    private EditText mContentEdt;
    
    private CheckBox mSyncQZoneChb;
    
    private CheckBox mSyncSinaChb;
    
    private ViewGroup mExpandView;
    
    private ViewGroup mCurrentExpendView;
    
    private ViewGroup mExpandFoodView;
    
    private TextView mExpandTxv;
    
    private ImageView mExpandImv;
    
    private RadioGroup mGradeRgp;
    
    private boolean mClickGradeRgp = false;
    
    private EditText mFoodAvgEdt;
    
    private SeekBar mTasteSkb;
    
    private SeekBar mFoodEnvironmentSkb;
    
    private SeekBar mFoodQosSkb;
    
    private Button mRestairBtn;
    
    private String[] mRestairArray;
    
    private boolean[] mRestairChecked;
    
    private EditText mRecommendEdt;
    
    private ViewGroup mExpandHotelView;
    
    private EditText mHotelAvgEdt;
    
    private SeekBar mHotelQosSkb;
    
    private SeekBar mHotelEnvironmentSkb;
    
    private ViewGroup mExpandCinemaView;
    
    private EditText mCinemaAvgEdt;
    
    private SeekBar mEffectSkb;
    
    private SeekBar mCinemaQosSkb;
    
    private ViewGroup mExpandHospitalView;
    
    private EditText mHospitalAvgEdt;
    
    private SeekBar mHospitalQosSkb;
    
    private SeekBar mLevelSkb;
    
    private ViewGroup mExpandBuyView;
    
    private EditText mBuyAvgEdt;
    
    private Comment mComment = new Comment();
    
    private static List<POI> sPOIList = new ArrayList<POI>();
    private static List<Integer> sFromViewIdList = new ArrayList<Integer>();
    private static List<Integer> sStatusList = new ArrayList<Integer>();
    
    private POI mPOI = null;
    
    private int mFromViewId;
    
    private BroadcastReceiver mSinaWeiboReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) { 

        	IBaseShare iBaseShare = ShareEntrance.getShareObject(mThis, ShareEntrance.SINAWEIBO_ENTRANCE);
                if (mSyncSinaChb.getTag().equals(Boolean.TRUE)) {
                    if (iBaseShare.satisfyCondition()) {
                        mSyncSinaChb.setChecked(true);
                    }
                }
                mSyncSinaChb.setTag(Boolean.FALSE);
            }
    };
    
    private BroadcastReceiver mTencentReceiver = new BroadcastReceiver(){
        
        @Override
        public void onReceive(Context context, Intent intent) {

        	IBaseShare iBaseShare = ShareEntrance.getShareObject(mThis, ShareEntrance.TENCENT_ENTRANCE);
            if (Boolean.TRUE.equals(mSyncQZoneChb.getTag())) {
                if (iBaseShare.satisfyCondition()) {
                    mSyncQZoneChb.setChecked(true);
                }
            }
            mSyncQZoneChb.setTag(Boolean.FALSE);
        }
        
    };
    
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
            }
        }

        setContentView(R.layout.comment);
        
        findViews();
        setListener();
        
        mGradeView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int gredeHeight = mGradeView.getMeasuredHeight()-Util.dip2px(Globals.g_metrics.density, 8);
        
        mContentView.setPadding(Util.dip2px(Globals.g_metrics.density, 2), gredeHeight, Util.dip2px(Globals.g_metrics.density, 2), 0);
        
        mTitleBtn.setText(mStatus == STATUS_NEW ? R.string.publish_comment : R.string.modify_comment);
        mRightBtn.setText(R.string.publish);
        Comment comment = mPOI.getMyComment();
        long userId = Long.MIN_VALUE;
        User user = Globals.g_User;            
        if (user != null) {
            userId = user.getUserId();
            if (comment.getUserId() != userId) {
                mPOI.setMyComment(new Comment());
            }
        } else {
            if (!Globals.g_ClientUID.equals(comment.getClientUid())) {
                mPOI.setMyComment(new Comment());
            }
        }
        mComment = mPOI.getMyComment();
        long commentPattern = mPOI.getCommentPattern();
        mExpandView.setVisibility(View.VISIBLE);

        int grade = (int) mComment.getGrade();
        switch (grade) {
            case 2:
                mGradeRgp.check(R.id.grade_1_rbt);
                break;
            case 4:
                mGradeRgp.check(R.id.grade_2_rbt);
                break;
            case 6:
                mGradeRgp.check(R.id.grade_3_rbt);
                break;
            case 8:
                mGradeRgp.check(R.id.grade_4_rbt);
                break;
            case 10:
                mGradeRgp.check(R.id.grade_5_rbt);
                break;

            default:
                break;
        }
        
        String content = mComment.getContent();
        if (BaseQuery.Test) {
            content = "123456789012345678901234567890";
        }
        if (!TextUtils.isEmpty(content)) {
            mContentEdt.setText(content);
            if (mStatus == STATUS_MODIFY) {
                mContentEdt.setSelection(content.length());
            }
        } else {
            mContentEdt.setText(null);
        }
        
    	IBaseShare iBaseShare = ShareEntrance.getShareObject(mThis, ShareEntrance.TENCENT_ENTRANCE);
        if (iBaseShare.satisfyCondition()) {
            mSyncQZoneChb.setChecked(true);
        } else {
            mSyncQZoneChb.setChecked(false);
        }
    	iBaseShare = ShareEntrance.getShareObject(mThis, ShareEntrance.SINAWEIBO_ENTRANCE);
        if (iBaseShare.satisfyCondition()) {
            mSyncSinaChb.setChecked(true);
        } else {
            mSyncSinaChb.setChecked(false);
        }
        
        mSyncQZoneChb.setTag(Boolean.FALSE);
        mSyncSinaChb.setTag(Boolean.FALSE);

        mExpandFoodView.setVisibility(View.GONE);
        mExpandHotelView.setVisibility(View.GONE);
        mExpandCinemaView.setVisibility(View.GONE);
        mExpandHospitalView.setVisibility(View.GONE);
        mExpandBuyView.setVisibility(View.GONE);
        mExpandImv.setBackgroundResource(R.drawable.icon_arrow_down);
        if (POI.COMMENT_PATTERN_FOOD == commentPattern) {
            mCurrentExpendView = mExpandFoodView;
            mExpandTxv.setText(R.string.comment_food_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mFoodAvgEdt.setText(String.valueOf(avg));
            } else {
                mFoodAvgEdt.setText(null);
            }
            mTasteSkb.setProgress((int)mComment.getTaste()-1);
            mFoodEnvironmentSkb.setProgress((int)mComment.getEnvironment()-1);
            mFoodQosSkb.setProgress((int)mComment.getQos()-1);
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
            mCurrentExpendView = mExpandHotelView;
            mExpandTxv.setText(R.string.comment_hotel_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mHotelAvgEdt.setText(String.valueOf(avg));
            } else {
                mHotelAvgEdt.setText(null);
            }
            mHotelQosSkb.setProgress((int)mComment.getQos()-1);
            mHotelEnvironmentSkb.setProgress((int)mComment.getEnvironment()-1);
        } else if (POI.COMMENT_PATTERN_CINEMA == commentPattern) {
            mCurrentExpendView = mExpandCinemaView;
            mExpandTxv.setText(R.string.comment_cinema_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mCinemaAvgEdt.setText(String.valueOf(avg));
            } else {
                mCinemaAvgEdt.setText(null);
            }
            mEffectSkb.setProgress((int)mComment.getEffect()-1);
            mCinemaQosSkb.setProgress((int)mComment.getQos()-1);
        } else if (POI.COMMENT_PATTERN_HOSPITAL == commentPattern) {
            mCurrentExpendView = mExpandHospitalView;
            mExpandTxv.setText(R.string.comment_hospital_title_text);
            long avg = mComment.getAvg();
            if (avg > 0) {
                mHospitalAvgEdt.setText(String.valueOf(avg));
            } else {
                mHospitalAvgEdt.setText(null);
            }
            mLevelSkb.setProgress((int)mComment.getLevel()-1);
            mHospitalQosSkb.setProgress((int)mComment.getQos()-1);
        } else if (POI.COMMENT_PATTERN_BUY == commentPattern) {
            mCurrentExpendView = mExpandBuyView;
            mExpandTxv.setText(R.string.comment_buy_title_text);
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
    
    boolean onResume = false;
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter= new IntentFilter(ShareMessageCenter.ACTION_SHARE_AUTH_SUCCESS);
        registerReceiver(mSinaWeiboReceiver, intentFilter);
        intentFilter= new IntentFilter(ShareMessageCenter.ACTION_SHARE_PROFILE_SUCCESS);
        registerReceiver(mTencentReceiver, intentFilter);
        
    	IBaseShare iBaseShare = ShareEntrance.getShareObject(mThis, ShareEntrance.TENCENT_ENTRANCE);
        if (Boolean.TRUE.equals(mSyncQZoneChb.getTag())) {
            if (iBaseShare.satisfyCondition()) {
                mSyncQZoneChb.setChecked(true);
            }
        }
        onResume = true;
        if (isReLogin()) {
            return;
        }
    }

    @Override
    public void onPause() {
        unregisterReceiver(mSinaWeiboReceiver);
        unregisterReceiver(mTencentReceiver);
        onResume = false;
        super.onPause();
    }
    
    private void save() {
        switch (mGradeRgp.getCheckedRadioButtonId()) {
            case R.id.grade_1_rbt:
                mComment.setGrade(2);
                break;
            case R.id.grade_2_rbt:
                mComment.setGrade(4);
                break;
            case R.id.grade_3_rbt:
                mComment.setGrade(6);
                break;
            case R.id.grade_4_rbt:
                mComment.setGrade(8);
                break;
            case R.id.grade_5_rbt:
                mComment.setGrade(10);
                break;

            default:
                break;
        }
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
            mComment.setTaste(mTasteSkb.getProgress()+1);
            mComment.setEnvironment(mFoodEnvironmentSkb.getProgress()+1);
            mComment.setQos(mFoodQosSkb.getProgress()+1);
            
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
            mComment.setQos(mHotelQosSkb.getProgress()+1);
            mComment.setEnvironment(mHotelEnvironmentSkb.getProgress()+1);
        } else if (POI.COMMENT_PATTERN_CINEMA == commentPattern) {
            String avg = mCinemaAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
            mComment.setEffect(mEffectSkb.getProgress()+1);
            mComment.setQos(mCinemaQosSkb.getProgress()+1);
        } else if (POI.COMMENT_PATTERN_HOSPITAL == commentPattern) {
            String avg = mHospitalAvgEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(avg)) {
                mComment.setAvg(Long.parseLong(avg));
            }
            mComment.setQos(mHospitalQosSkb.getProgress()+1);
            mComment.setLevel(mLevelSkb.getProgress()+1);
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
        sendBroadcast(new Intent(ShareMessageCenter.EXTRA_SHARE_FINISH));
	}

	protected void findViews() {
        super.findViews();
        mGradeView = (ViewGroup) findViewById(R.id.grade_view);
        mGradeRgp = (RadioGroup) findViewById(R.id.grade_rgp);
        mContentView = (ViewGroup) findViewById(R.id.content_view);
        mContentEdt = (EditText) findViewById(R.id.content_edt);
        mTextNumTxv = (TextView)this.findViewById(R.id.text_limit_txv);
        mSyncQZoneChb = (CheckBox) findViewById(R.id.sync_qzone_chb);
        mSyncSinaChb = (CheckBox) findViewById(R.id.sync_sina_chb);

        mExpandView = (ViewGroup) findViewById(R.id.expand_view);
        mExpandFoodView = (ViewGroup) findViewById(R.id.food_view);
        mExpandTxv = (TextView) findViewById(R.id.comment_expand_txv);
        mExpandImv = (ImageView) findViewById(R.id.comment_expand_imv);
        mFoodAvgEdt = (EditText) mExpandFoodView.findViewById(R.id.avg_edt);
        mTasteSkb = (SeekBar) mExpandFoodView.findViewById(R.id.taste_skb);
        mFoodEnvironmentSkb = (SeekBar) mExpandFoodView.findViewById(R.id.environment_skb);
        mFoodQosSkb = (SeekBar)mExpandFoodView.findViewById(R.id.qos_skb);
        mRestairBtn = (Button) mExpandFoodView.findViewById(R.id.restair_btn);
        mRecommendEdt = (EditText) mExpandFoodView.findViewById(R.id.recommend_edt);
        
        mExpandHotelView = (ViewGroup) findViewById(R.id.hotel_view);
        mHotelAvgEdt = (EditText) mExpandHotelView.findViewById(R.id.avg_edt);
        mHotelQosSkb = (SeekBar)mExpandHotelView.findViewById(R.id.qos_skb);
        mHotelEnvironmentSkb = (SeekBar) mExpandHotelView.findViewById(R.id.environment_skb);
        
        mExpandCinemaView = (ViewGroup) findViewById(R.id.cinema_view);
        mCinemaAvgEdt = (EditText) mExpandCinemaView.findViewById(R.id.avg_edt);
        mEffectSkb = (SeekBar) mExpandCinemaView.findViewById(R.id.effect_skb);
        mCinemaQosSkb = (SeekBar)mExpandCinemaView.findViewById(R.id.qos_skb);
        
        mExpandHospitalView = (ViewGroup) findViewById(R.id.hospital_view);
        mHospitalAvgEdt = (EditText) mExpandHospitalView.findViewById(R.id.avg_edt);
        mHospitalQosSkb = (SeekBar)mExpandHospitalView.findViewById(R.id.qos_skb);
        mLevelSkb = (SeekBar) mExpandHospitalView.findViewById(R.id.level_skb);
        
        mExpandBuyView = (ViewGroup) findViewById(R.id.buy_view);
        mBuyAvgEdt = (EditText) mExpandBuyView.findViewById(R.id.avg_edt);
    }

    protected void setListener() {
        super.setListener();
        mLeftBtn.setOnClickListener(this);
        mRightBtn.setOnClickListener(this);
        mExpandTxv.setOnClickListener(this);
        mRestairBtn.setOnClickListener(this);
        OnTouchListener onTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                final int action = ev.getAction();

                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP: {
                        if (R.id.taste_skb == v.getId()) {
                            mActionLog.addAction(ActionLog.POICommentTaste);
                        } else if (R.id.environment_skb == v.getId()) {
                            mActionLog.addAction(ActionLog.POICommentEnvironment);
                        } else if (R.id.qos_skb == v.getId()) {
                            mActionLog.addAction(ActionLog.POICommentQos);
                        }
                        break;
                    }
                }
                return false;
            }
        };
        mTasteSkb.setOnTouchListener(onTouchListener);
        mFoodEnvironmentSkb.setOnTouchListener(onTouchListener);
        mFoodQosSkb.setOnTouchListener(onTouchListener);
        mHotelEnvironmentSkb.setOnTouchListener(onTouchListener);
        mHotelQosSkb.setOnTouchListener(onTouchListener);
        mCinemaQosSkb.setOnTouchListener(onTouchListener);
        mHospitalQosSkb.setOnTouchListener(onTouchListener);
        OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View v, boolean value) {
                switch (v.getId()) {
                    case R.id.content_edt:
                        mActionLog.addAction(ActionLog.POICommentContent);
                        break;
                        
                    case R.id.avg_edt:
                        mActionLog.addAction(ActionLog.POICommentAvg);                        
                        break;
                        
                    case R.id.recommend_edt:
                        mActionLog.addAction(ActionLog.POICommentRecommend);  
                        break;

                    default:
                        break;
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
        mGradeRgp.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int viewId) {
                mClickGradeRgp = true;
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.grade_1_rbt:
                        if (onResume)
                            mActionLog.addAction(ActionLog.POICommentClickGrade, 1);
                        mContentEdt.setHint(R.string.comment_content_hit1);
                        break;
                    case R.id.grade_2_rbt:
                        if (onResume)
                            mActionLog.addAction(ActionLog.POICommentClickGrade, 2);
                        mContentEdt.setHint(R.string.comment_content_hit2);
                        break;
                    case R.id.grade_3_rbt:
                        if (onResume)
                            mActionLog.addAction(ActionLog.POICommentClickGrade, 3);
                        mContentEdt.setHint(R.string.comment_content_hit3);
                        break;
                    case R.id.grade_4_rbt:
                        if (onResume)
                            mActionLog.addAction(ActionLog.POICommentClickGrade, 4);
                        mContentEdt.setHint(R.string.comment_content_hit4);
                        break;
                    case R.id.grade_5_rbt:
                        if (onResume)
                            mActionLog.addAction(ActionLog.POICommentClickGrade, 5);
                        mContentEdt.setHint(R.string.comment_content_hit5);
                        break;

                    default:
                        mClickGradeRgp = false;
                        break;
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
    public void onClick(View view) {
        int viewId = view.getId();
        if (R.id.right_btn == viewId) {
            mActionLog.addAction(ActionLog.POICommentClickSubmit);
            if (mContentEdt.getEditableText().toString().trim().length() < MIN_CHAR) {
                AlertDialog alertDialog = CommonUtils.getAlertDialog(mThis);
                alertDialog.setMessage(mThis.getString(R.string.comment_prompt_input_content));
                alertDialog.setButton(mThis.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mContentEdt.requestFocus();
                    }
                });
                alertDialog.show();
                return;
            } else if (mClickGradeRgp == false) {
                AlertDialog alertDialog = CommonUtils.getAlertDialog(mThis);
                alertDialog.setMessage(mThis.getString(R.string.comment_prompt_input_grade));
                alertDialog.setButton(mThis.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mGradeRgp.requestFocus();
                    }
                });
                alertDialog.show();
                return;
            }
            if (mStatus == STATUS_MODIFY) {
                AlertDialog alertDialog = CommonUtils.getAlertDialog(mThis);
                alertDialog.setMessage(mThis.getString(R.string.poi_comment_override_tip));
                alertDialog.setButton(mThis.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        readySubmit();
                    }
                });
                alertDialog.show();
            } else {
                readySubmit();
            }
            
        } else if (R.id.restair_btn == viewId) {
            mActionLog.addAction(ActionLog.POICommentRestair);
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
            
            Dialog dialog = new AlertDialog.Builder(mThis)
            .setTitle(R.string.comment_food_restair)
            .setMultiChoiceItems(R.array.comment_restair,
                    mRestairChecked,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton,
                                boolean isChecked) {
                            mRestairChecked[whichButton] = isChecked;
                        }
                    })
            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
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
                    mActionLog.addAction(ActionLog.POICommentClickRestairOK);
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mActionLog.addAction(ActionLog.POICommentClickRestairCancel);
                }
            })
           .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } else if (R.id.comment_expand_txv == viewId) {
            if (mCurrentExpendView.getVisibility() == View.GONE) {
                mActionLog.addAction(ActionLog.POICommentClickExpand);
                hideSoftInput();
                mCurrentExpendView.setVisibility(View.VISIBLE);
                mExpandImv.setBackgroundResource(R.drawable.icon_arrow_up);
//                Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1.0f);
//                animation.setDuration(2*1000);
//                animation.setInterpolator(new AccelerateInterpolator());
//                animation.setAnimationListener(new AnimationListener() {
//                    
//                    @Override
//                    public void onAnimationStart(Animation arg0) {
//                        // TODO Auto-generated method stub
//                        
//                    }
//                    
//                    @Override
//                    public void onAnimationRepeat(Animation arg0) {
//                        // TODO Auto-generated method stub
//                        
//                    }
//                    
//                    @Override
//                    public void onAnimationEnd(Animation arg0) {
////                        ((LinearLayout.LayoutParams)mExpandFoodView.getLayoutParams()).topMargin = 0;
//                        mExpandFoodView.getLayoutParams().height = mExpandViewHeight;
//                        mExpand = false;
//                    }
//                });
//                mExpandFoodView.setAnimation(animation);
//                animation.startNow();
            } else {
                mActionLog.addAction(ActionLog.POICommentClickCollapse);
                mCurrentExpendView.setVisibility(View.GONE);
                mExpandImv.setBackgroundResource(R.drawable.icon_arrow_down);
//                Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -1.0f);
//                animation.setDuration(2*1000);
//                animation.setInterpolator(new AccelerateInterpolator());
//                animation.setAnimationListener(new AnimationListener() {
//                    
//                    @Override
//                    public void onAnimationStart(Animation arg0) {
//                        // TODO Auto-generated method stub
//                        
//                    }
//                    
//                    @Override
//                    public void onAnimationRepeat(Animation arg0) {
//                        // TODO Auto-generated method stub
//                        
//                    }
//                    
//                    @Override
//                    public void onAnimationEnd(Animation arg0) {
////                        ((LinearLayout.LayoutParams)mExpandFoodView.getLayoutParams()).topMargin = -mExpandViewHeight;
//                        mExpandFoodView.getLayoutParams().height = 0;
//                        mExpand = true;
//                    }
//                });
//                mExpandFoodView.setAnimation(animation);
//                animation.startNow();
            }
        } else if (viewId == R.id.left_btn) {
            mActionLog.addAction(ActionLog.Title_Left_Back, mActionTag);
            if (showDiscardDialog() == false) {
                finish();
            }
        } else if (viewId == R.id.sync_qzone_chb) { 
            if (mSyncQZoneChb.isChecked() == true) {
                mActionLog.addAction(ActionLog.POICommentClickQZone, 0);
                mSyncQZoneChb.setTag(Boolean.TRUE);
                ShareEntrance shareEntrance = ShareEntrance.getInstance();
                shareEntrance.injectOrAuth(mThis, ShareEntrance.TENCENT_ENTRANCE);
                IBaseShare iBaseShare = shareEntrance.getCurrentShareObject();
                
                if (iBaseShare.satisfyCondition()) {
                    mSyncQZoneChb.setTag(Boolean.FALSE);
                    mSyncQZoneChb.setChecked(true);
                } else {
                    mSyncQZoneChb.setChecked(false);
                }
            } else {
                mActionLog.addAction(ActionLog.POICommentClickQZone, 1);
                mSyncQZoneChb.setTag(Boolean.FALSE);
                mSyncQZoneChb.setChecked(false);
            }
        } else if (viewId == R.id.sync_sina_chb) {
            if (mSyncSinaChb.isChecked() == true) {
                mActionLog.addAction(ActionLog.POICommentClickSina, 0);
                mSyncSinaChb.setTag(Boolean.TRUE);
                ShareEntrance shareEntrance = ShareEntrance.getInstance();
                shareEntrance.injectOrAuth(mThis, ShareEntrance.SINAWEIBO_ENTRANCE);
                IBaseShare iBaseShare = shareEntrance.getCurrentShareObject();
                if (iBaseShare.satisfyCondition()) {
                    mSyncSinaChb.setTag(Boolean.FALSE);
                    mSyncSinaChb.setChecked(true);
                } else {
                    mSyncSinaChb.setChecked(false);
                }
            } else {
                mActionLog.addAction(ActionLog.POICommentClickSina, 1);
                mSyncSinaChb.setTag(Boolean.FALSE);
                mSyncSinaChb.setChecked(false);
            }
        }
    }
    
    private void submit() {
        hideSoftInput();
        String shareGrade = "";
        String recommendCook = "";
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_DIANPING);
        StringBuilder s = new StringBuilder();
        s.append(Util.byteToHexString(Comment.FIELD_GRADE));
        s.append(':');
        switch (mGradeRgp.getCheckedRadioButtonId()) {
            case R.id.grade_1_rbt:
                s.append("2");
                shareGrade = mThis.getString(R.string.poi_comment_share_grade1);
                break;
            case R.id.grade_2_rbt:
                s.append("4");
                shareGrade = mThis.getString(R.string.poi_comment_share_grade2);
                break;
            case R.id.grade_3_rbt:
                s.append("6");
                shareGrade = mThis.getString(R.string.poi_comment_share_grade3);
                break;
            case R.id.grade_4_rbt:
                s.append("8");
                shareGrade = mThis.getString(R.string.poi_comment_share_grade4);
                break;
            case R.id.grade_5_rbt:
                s.append("10");
                shareGrade = mThis.getString(R.string.poi_comment_share_grade5);
                break;

            default:
                break;
        }
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
            s.append(mTasteSkb.getProgress()+1);
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_ENVIRONMENT));
            s.append(':');
            s.append(mFoodEnvironmentSkb.getProgress()+1);
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_QOS));
            s.append(':');
            s.append(mFoodQosSkb.getProgress()+1);
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
            s.append(mHotelQosSkb.getProgress()+1);
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_ENVIRONMENT));
            s.append(':');
            s.append(mHotelEnvironmentSkb.getProgress()+1);
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
            s.append(mEffectSkb.getProgress()+1);
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_QOS));
            s.append(':');
            s.append(mCinemaQosSkb.getProgress()+1);
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
            s.append(mHospitalQosSkb.getProgress()+1);
            s.append(',');
            s.append(Util.byteToHexString(Comment.FIELD_LEVEL));
            s.append(':');
            s.append(mLevelSkb.getProgress()+1);
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
            String shareSina = "";
            shareSina = mThis.getString(R.string.poi_comment_share_sina, mPOI.getName(), shareGrade, content, TextUtils.isEmpty(recommendCook) ? "" : mThis.getString(R.string.recommend_cooking, recommendCook));
            if (shareSina.length() > 120) {
                shareSina = shareSina.subSequence(0, 117) + "...";
            }
            shareSina = shareSina + "http://www.tigerknows.com";
            criteria.put(DataOperation.SERVER_PARAMETER_SHARE_SINA, shareSina);
        }
        
        if (mSyncQZoneChb.isChecked()) {
            String shareQzone = "";
            shareQzone = mThis.getString(R.string.poi_comment_share_qzone, mPOI.getName(), shareGrade, content, TextUtils.isEmpty(recommendCook) ? "" : mThis.getString(R.string.recommend_cooking, recommendCook));
            if (shareQzone.length() > 123) {
                shareQzone = shareQzone.subSequence(0, 120) + "...";
            }
            shareQzone = shareQzone + mThis.getString(R.string.poi_comment_share_qzone_source);
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
            Dialog dialog = new AlertDialog.Builder(mThis)
            .setTitle(R.string.prompt)
            .setMessage(mStatus == STATUS_NEW ? R.string.comment_discard_tip_new : R.string.comment_discard_tip_modify)
            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mActionLog.addAction(ActionLog.POICommentClickExitOK);
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mActionLog.addAction(ActionLog.POICommentClickExitCancel);
                }
            })
           .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
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
                    final AlertDialog alertDialog = CommonUtils.getAlertDialog(mThis);
                    alertDialog.setMessage(mThis.getString(R.string.response_code_601));
                    alertDialog.setButton(mThis.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            alertDialog.dismiss();
                            mStatus = STATUS_MODIFY;
                            mTitleBtn.setText(R.string.modify_comment);
                            Hashtable<String, String> criteria = commentOperation.getCriteria();
                            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_UPDATE);
                            mComment.setUid(((CommentCreateResponse)response).getUid());
                            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mComment.getUid());
                            DataOperation dataOperation = new DataOperation(mThis);
                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), -1, mFromViewId, mThis.getString(R.string.doing_and_wait));
                            queryStart(dataOperation);
                        }
                    });
                    alertDialog.show();
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
        
        long userId = Long.MIN_VALUE;
        User user = Globals.g_User;            
        if (user != null) {
            userId = user.getUserId();
            mComment.setUserId(userId);
            mComment.getPOI().setAttribute(POI.ATTRIBUTE_COMMENT_USER);
            mComment.setUser(user.getNickName());
        } else {
            mComment.getPOI().setAttribute(POI.ATTRIBUTE_COMMENT_ANONYMOUS);
            mComment.setUserId(-1);
            mComment.setUser(mThis.getString(R.string.default_guest_name));
        }
        mComment.setClientUid(Globals.g_ClientUID);
        
        mPOI.setMyComment(mComment);
        DataQuery dataQuery = mPOI.getCommentQuery();
        
        List<Comment> commentArrayList = null;
        // 正常不会出现commentQuery为空的情况
        if (dataQuery != null) {
            Response commentResponse = dataQuery.getResponse();
            if (commentResponse != null && commentResponse instanceof CommentResponse) {
                CommentList commentList = ((CommentResponse)commentResponse).getList();
                if (commentList != null) {
                    commentArrayList = commentList.getList();
                    if (commentArrayList != null) {
                        if (commentArrayList.size() > 0) {
                            mPOI.updateAttribute();
                            Collections.sort(commentArrayList, Comment.COMPARATOR);
                            long attribute = mPOI.getAttribute();
                            if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0 || (attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
                                commentArrayList.remove(0);
                                commentArrayList.add(0, mComment);
                            } else {
                                commentArrayList.add(0, mComment);
                                commentList.setTotal(1);
                            }
                        } else {
                            commentArrayList.add(mComment);
                            commentList.setTotal(1);
                        }
                    }
                }
            }
        }
        
        if (commentArrayList == null || commentArrayList.isEmpty()) {
            try {
                XMap data = new XMap();
                CommentList commentList = new CommentList(data);
                CommentResponse commentResponse = new CommentResponse(data);
                commentArrayList = new ArrayList<Comment>();
                commentArrayList.add(mComment);
                commentList.setTotal(1);
                commentList.setList(commentArrayList);
                commentResponse.setList(commentList);
                DataQuery commentQuery = Comment.createPOICommentQuery(mThis, mPOI, -1, -1);
                commentQuery.setResponse(commentResponse);
                mPOI.setCommentQuery(commentQuery);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        mPOI.updateAttribute();
        mComment.setData(null);
        mPOI.updateComment(mThis);
        
        if (userId != Long.MIN_VALUE) {
            mPOI.setAttribute(POI.ATTRIBUTE_COMMENT_USER);
        } else {
            mPOI.setAttribute(POI.ATTRIBUTE_COMMENT_ANONYMOUS);
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
                mActionLog.addAction(ActionLog.KeyCodeBack);
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
        int grade = -1;
        switch (mGradeRgp.getCheckedRadioButtonId()) {
            case R.id.grade_1_rbt:
                grade = 2;
                break;
            case R.id.grade_2_rbt:
                grade = 4;
                break;
            case R.id.grade_3_rbt:
                grade = 6;
                break;
            case R.id.grade_4_rbt:
                grade = 8;
                break;
            case R.id.grade_5_rbt:
                grade = 10;
                break;

            default:
                break;
        }
        if (grade != mComment.getGrade()) {
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
            if (mTasteSkb.getProgress()+1 != mComment.getTaste()) {
                return true;
            }
            if (mFoodEnvironmentSkb.getProgress()+1 != mComment.getEnvironment()) {
                return true;
            }
            if (mFoodQosSkb.getProgress()+1 != mComment.getQos()) {
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
            if (mHotelQosSkb.getProgress()+1 != mComment.getQos()) {
                return true;
            }
            if (mHotelEnvironmentSkb.getProgress()+1 != mComment.getEnvironment()) {
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
            if (mEffectSkb.getProgress()+1 != mComment.getEffect()) {
                return true;
            }
            if (mCinemaQosSkb.getProgress()+1 != mComment.getQos()) {
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
            if (mHospitalQosSkb.getProgress()+1 != mComment.getQos()) {
                return true;
            }
            if (mLevelSkb.getProgress()+1 != mComment.getLevel()) {
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
}

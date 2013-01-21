/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.tigerknows.R;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.TKAsyncTask;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import java.util.Hashtable;

/**
 * @author Peng Wenyue
 */
public class Feedback extends BaseActivity implements View.OnClickListener {

    private ScrollView mBodyScv;
    private EditText mFeedbackEdt = null;
    private EditText mTelephoneEdt = null;    
//    private TextView mWeibtoTxv;
    private Button feedback_send;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.Feedback;

        setContentView(R.layout.feedback);
        findViews();
        setListener();

        mTitleBtn.setText(R.string.feedback);
        feedback_send.setEnabled(false);
//        SpannableString ss = new SpannableString(mThis.getString(R.string.feedback_text2));
//        ss.setSpan(new ClickableSpan() {
//            @Override
//            public void updateDrawState(TextPaint ds) {
//                ds.setColor(Color.BLUE);// 链接文字蓝色
//                ds.setUnderlineText(false);// 去掉链接的下划线
//            }
//
//            @Override
//            public void onClick(final View widget) {
//                mActionLog.addAction(ActionLog.FeedbackWeibo);
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri
//                        .parse("http://weibo.com/tigermap"));
//                mThis.startActivity(intent);
//            }
//        }, 4, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//        mWeibtoTxv.setText(ss);
//
//        //这条语句不可少，否则点击链接不会打开Activity窗口
//        mWeibtoTxv.setMovementMethod(LinkMovementMethod.getInstance()); 
    }

    protected void findViews() {
        super.findViews();
        mBodyScv = (ScrollView) findViewById(R.id.body_scv);
        mFeedbackEdt = (EditText)findViewById(R.id.feedback_edt);
        mTelephoneEdt = (EditText)findViewById(R.id.telephone_edt);
//        mWeibtoTxv = (TextView)findViewById(R.id.weibo_txv);
        feedback_send = (Button)findViewById(R.id.feedback_send);
    }

    protected void setListener() {
        super.setListener();
        feedback_send.setOnClickListener(this);
        mBodyScv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mBodyScv.getScrollY() < (mFeedbackEdt.getBottom()-mFeedbackEdt.getTop()) && 
                        event.getX() > mFeedbackEdt.getLeft() &&
                        event.getX() < mFeedbackEdt.getRight() &&
                        event.getY() < mFeedbackEdt.getBottom() &&
                        mFeedbackEdt.onTouchEvent(event)) {
                    return true; 
                }
                return false;
            }
        });
        mFeedbackEdt.setOnFocusChangeListener(new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View arg0, boolean focused) {
                if (focused) {
                    mActionLog.addAction(ActionLog.FeedBackContent);
                }
            }
        });
        mTelephoneEdt.setOnFocusChangeListener(new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View arg0, boolean focused) {
                if (focused) {
                    mActionLog.addAction(ActionLog.FeedbackMobile);
                }
            }
        });
        mTelephoneEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    send();
                    return true;
                }
                return false;
            }
        });
        mFeedbackEdt.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }
            
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void afterTextChanged(Editable arg0) {
                if (TextUtils.isEmpty(mFeedbackEdt.getEditableText().toString())) {
                	feedback_send.setEnabled(false);
                } else {
                	feedback_send.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.feedback_send:
                mActionLog.addAction(ActionLog.FeedbackSubmit);
                send();
                break;
            case R.id.weibo_txv:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://weibo.com/tigermap"));
                mThis.startActivity(intent);
                break;
            default:
                break;
        }
    }
    
    private void send() {
        String feedContent = mFeedbackEdt.getText().toString().trim().replace("\n", "\\n");
        if (!TextUtils.isEmpty(feedContent)) {
            if (!TextUtils.isEmpty(mTelephoneEdt.getText().toString())) {
                feedContent += mThis.getString(R.string.mobile_number_) + mTelephoneEdt.getText().toString();
            }
            hideSoftInput();

            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(FeedbackUpload.SERVER_PARAMETER_FEEDBACK, feedContent);
            FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
            feedbackUpload.setup(criteria, -1, -1, -1, mThis.getString(R.string.doing_and_wait));
            queryStart(feedbackUpload);
        } else {
            Toast.makeText(mThis, R.string.feedback_no_content_tip, Toast.LENGTH_LONG).show();
        }
    }
    
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, this, false)) {
            return;
        }
        Toast.makeText(mThis, R.string.feedback_success, Toast.LENGTH_LONG).show();
        finish();
    }
}

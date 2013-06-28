package com.tigerknows.ui.more;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;

import com.tigerknows.R;
import com.tigerknows.TKConfig;

import com.decarta.Globals;
import com.tigerknows.R.array;
import com.tigerknows.R.drawable;
import com.tigerknows.R.id;
import com.tigerknows.R.layout;
import com.tigerknows.R.string;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

public class AddMerchantActivity extends BaseActivity implements View.OnClickListener {
    
    public static final String EXTRA_INPUT_TEXT = "input_text";
    
	private ScrollView mAddMerchantScv;
    private Button mShanghuleixingBtn;
    private EditText mShanghumingchengEdt = null;
    private EditText mShanghudizhiEdt = null;
    private EditText mShanghudianhuaEdt = null;
    private EditText mYingyeshijianEdt = null;
    private EditText mNingdedianhuaEdt = null;
    
    private String[] mAddMerchantTypes;
    
	static final String TAG = "AddMerchant";
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mId = R.id.activity_more_add_merchant;
        mActionTag = ActionLog.AddMerchant;
        setContentView(R.layout.more_add_merchant);
        
        findViews();
        setListener();

        mTitleBtn.setText(R.string.add_merchant);
        mRightBtn.setBackgroundResource(R.drawable.btn_submit_comment);
        
        mAddMerchantTypes = mThis.getResources().getStringArray(R.array.add_merchant_type);
        mShanghumingchengEdt.requestFocus();
        
        mShanghudianhuaEdt.setText(Utility.getAreaCodeByCityId(Globals.getCurrentCityInfo().getId())+"-");
        
        Intent intent = getIntent();
        if (intent != null) {
            String intputText = intent.getStringExtra(EXTRA_INPUT_TEXT);
            if (intputText != null) {
                mShanghumingchengEdt.setText(intputText);
                mShanghumingchengEdt.setSelection(intputText.length());
            }
        }
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();
        mAddMerchantScv = (ScrollView)findViewById(R.id.add_merchant_scv);
        mShanghumingchengEdt = (EditText)findViewById(R.id.shanghumingcheng_edt);
        mShanghuleixingBtn = (Button)findViewById(R.id.shanghuleixing_btn);
        mShanghudizhiEdt = (EditText)findViewById(R.id.shanghudizhi_edt);
        mShanghudianhuaEdt = (EditText)findViewById(R.id.shanghudianhua_edt);
        mYingyeshijianEdt = (EditText)findViewById(R.id.yingyeshijian_edt);
        mNingdedianhuaEdt = (EditText)findViewById(R.id.ningdedianhua_edt);
    }

    /**
     * Set the listeners for the commit and cancel button
     */
    protected void setListener() {
        super.setListener();
        mLeftBtn.setOnClickListener(this);
        mRightBtn.setOnClickListener(this);
        
        mShanghuleixingBtn.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                final int action = ev.getAction();

                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP: {
                        mActionLog.addAction(mActionTag +  ActionLog.AddMerchantType);
                        StringArrayAdapter adapter =  new StringArrayAdapter(mThis, mAddMerchantTypes);
                        ListView listView = Utility.makeListView(mThis);
                        listView.setAdapter(adapter);
                        
                        final Dialog dialog = Utility.showNormalDialog(mThis,
                                mThis.getString(R.string.shanghuleixing),
                                null,
                                listView,
                                null,
                                null,
                                null);
                        listView.setOnItemClickListener(new OnItemClickListener() {
                            
                            @Override
                            public void onItemClick(AdapterView<?> arg0, View arg1, int index,
                                    long arg3) {
                                mShanghuleixingBtn.setText(mAddMerchantTypes[index]);
                                mShanghuleixingBtn.setTextColor(0xff000000);
                                dialog.dismiss();
                            }
                            
                        });
                        break;
                    }
                }
                return false;
            }
        });
        OnTouchListener onTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    switch (v.getId()) {
                        case R.id.shanghumingcheng_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantName);
                            break;
                            
                        case R.id.shanghudizhi_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantAddress);
                            break;
                            
                        case R.id.shanghudianhua_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantTelephone);
                            break;
                            
                        case R.id.yingyeshijian_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantTime);
                            break;
                            
                        case R.id.ningdedianhua_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantMobile);
                            break;
                            
                        default:
                            break;
                    }
                }
                return false;
            }
        };
        mShanghumingchengEdt.setOnTouchListener(onTouchListener);
        mShanghudizhiEdt.setOnTouchListener(onTouchListener);
        mShanghudianhuaEdt.setOnTouchListener(onTouchListener);
        mYingyeshijianEdt.setOnTouchListener(onTouchListener);
        mNingdedianhuaEdt.setOnTouchListener(onTouchListener);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void exit() {
        boolean notEmpty = !TextUtils.isEmpty(mShanghumingchengEdt.getEditableText().toString());
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mShanghuleixingBtn.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mShanghudizhiEdt.getEditableText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mShanghudianhuaEdt.getEditableText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mYingyeshijianEdt.getEditableText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mNingdedianhuaEdt.getEditableText().toString());
        }
        
        if (notEmpty == false) {
            finish();
        }
        Utility.showNormalDialog(mThis,
                getString(R.string.are_you_exit_add_merchart),
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            finish();
                        }
                    }
                });
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            case R.id.left_btn:
                exit();
                break;
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                StringBuilder s = new StringBuilder();
                try {
                    String str = mShanghumingchengEdt.getEditableText().toString().trim();
                    if (!TextUtils.isEmpty(str)) {
                        s.append("shanghumingcheng");
                        s.append('=');
                        s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
                    } else {
                        mShanghumingchengEdt.requestFocus();
                        mAddMerchantScv.scrollTo(0, 0);
                        Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.shanghumingcheng)+"!", Toast.LENGTH_SHORT).show();
                        showSoftInput();
                        return;
                    }
                    
                    str = mShanghuleixingBtn.getText().toString().trim();
                    if (!TextUtils.isEmpty(str) && str.equals(mThis.getString(R.string.bitian)) == false) {
                        if (s.length() > 0) {
                            s.append('&');
                        }
                        s.append("shanghuleixing");
                        s.append('=');
                        s.append(URLEncoder.encode(str.toString(), TKConfig.getEncoding()));
                    } else {
                        mShanghuleixingBtn.requestFocus();
                        Toast.makeText(mThis, mThis.getString(R.string.please_select)+mThis.getString(R.string.shanghuleixing)+"!", Toast.LENGTH_SHORT).show();
                        hideSoftInput();
                        return;
                    }
                    
                    str = mShanghudizhiEdt.getEditableText().toString().trim();
                    if (!TextUtils.isEmpty(str)) {
                        if (s.length() > 0) {
                            s.append('&');
                        }
                        s.append("shanghudizhi");
                        s.append('=');
                        s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
                    } else {
                        mShanghudizhiEdt.requestFocus();
                        Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.shanghudizhi)+"!", Toast.LENGTH_SHORT).show();
                        showSoftInput();
                        return;
                    }
                    
                    str = mShanghudianhuaEdt.getEditableText().toString().trim();
                    if (!TextUtils.isEmpty(str)) {
                        if (s.length() > 0) {
                            s.append('&');
                        }
                        s.append("shanghudianhua");
                        s.append('=');
                        s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
                    }
                    
                    str = mYingyeshijianEdt.getEditableText().toString().trim();
                    if (!TextUtils.isEmpty(str)) {
                        if (s.length() > 0) {
                            s.append('&');
                        }
                        s.append("yingyeshijian");
                        s.append('=');
                        s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
                    }
                    
                    str = mNingdedianhuaEdt.getEditableText().toString().trim();
                    if (!TextUtils.isEmpty(str)) {
                        if (s.length() > 0) {
                            s.append('&');
                        }
                        s.append("ningdedianhua");
                        s.append('=');
                        s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                
                hideSoftInput();
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(FeedbackUpload.SERVER_PARAMETER_ADD_MERCHANT, s.toString());
                FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
                feedbackUpload.setup(criteria, Globals.getCurrentCityInfo().getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
                queryStart(feedbackUpload);
                break;
                
            default:
                
        }
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
        Toast.makeText(mThis, R.string.add_merchant_success, Toast.LENGTH_LONG).show();
        finish();
    }
}

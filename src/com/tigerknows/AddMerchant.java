package com.tigerknows;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.decarta.Globals;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.StringArrayAdapter;

public class AddMerchant extends BaseActivity implements View.OnClickListener {
    
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
        
        mId = R.id.activity_add_merchant;
        mActionTag = ActionLog.AddMerchant;
        setContentView(R.layout.add_merchant);
        
        findViews();
        setListener();

        mTitleBtn.setText(R.string.add_merchant);
        mRightBtn.setBackgroundResource(R.drawable.btn_submit_comment);
        
        mAddMerchantTypes = mThis.getResources().getStringArray(R.array.add_merchant_type);
        mShanghuleixingBtn.setFocusable(true);
        mShanghuleixingBtn.setFocusableInTouchMode(true);
        mShanghumingchengEdt.requestFocus();
        new Handler().postDelayed(new Runnable() {
            
            @Override
            public void run() {
                showSoftInput(mShanghumingchengEdt);
            }
        }, 512);
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();
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
        mRightBtn.setOnClickListener(this);
        
        mShanghuleixingBtn.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                final int action = ev.getAction();

                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP: {
                        mActionLog.addAction(ActionLog.AddMerchantType);
                        StringArrayAdapter adapter =  new StringArrayAdapter(mThis, mAddMerchantTypes);
                        ListView listView = CommonUtils.makeListView(mThis);
                        listView.setAdapter(adapter);
                        
                        final Dialog dialog = CommonUtils.showNormalDialog(mThis,
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
        OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View v, boolean value) {
                switch(v.getId()){
                    case R.id.shanghumingcheng_edt:
                        if (value)
                            mActionLog.addAction(ActionLog.AddMerchantName);
                        break;
                        
                    case R.id.shanghudizhi_edt:
                        if (value)
                            mActionLog.addAction(ActionLog.AddMerchantAddress);
                        break;
                        
                    case R.id.shanghudianhua_edt:
                        if (value)
                            mActionLog.addAction(ActionLog.AddMerchantTelephone);
                        break;
                        
                    case R.id.yingyeshijian_edt:
                        if (value)
                            mActionLog.addAction(ActionLog.AddMerchantTime);
                        break;
                        
                    case R.id.ningdedianhua_edt:
                        if (value)
                            mActionLog.addAction(ActionLog.AddMerchantMobile);
                        break;
                }
            }
        };
        mShanghumingchengEdt.setOnFocusChangeListener(onFocusChangeListener);
        mShanghudizhiEdt.setOnFocusChangeListener(onFocusChangeListener);
        mShanghudianhuaEdt.setOnFocusChangeListener(onFocusChangeListener);
        mYingyeshijianEdt.setOnFocusChangeListener(onFocusChangeListener);
        mNingdedianhuaEdt.setOnFocusChangeListener(onFocusChangeListener);
    }
    
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.Title_Right_Button, mActionTag);
                StringBuilder s = new StringBuilder();
                try {
                    String str = mShanghumingchengEdt.getEditableText().toString().trim();
                    if (!TextUtils.isEmpty(str)) {
                        s.append("shanghumingcheng");
                        s.append('=');
                        s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
                    } else {
                        mShanghumingchengEdt.requestFocus();
                        Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.shanghumingcheng)+"!", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(mThis, mThis.getString(R.string.please_select)+mThis.getString(R.string.shanghuleixing)+"!", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.shanghudizhi)+"!", Toast.LENGTH_LONG).show();
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
                feedbackUpload.setup(criteria, Globals.g_Current_City_Info.getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
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

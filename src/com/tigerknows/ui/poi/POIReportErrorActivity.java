package com.tigerknows.ui.poi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;

public class POIReportErrorActivity extends BaseActivity implements View.OnClickListener{
    
    private static final int HOME_PAGE = 1;
    private static final int DETAIL_PAGE = 2;
    
    // 后六位控制详情页的页面显示
    // 前三位控制POI纠错码的十位数，并且需要加一
    private static final int NAME_ERR = 0215;        //2 001101
    private static final int TEL_ERR = 0451;        //4 101001
    private static final int TEL_ADD = 0511;        //5 001001
    private static final int NOT_EXIST = 021;        //0 010001
    private static final int NE_OTHER = 023;        //0 010011
    private static final int ADDRESS_ERR = 0111;    //1 001001
    private static final int OTHER_ERR = 0703;        //7 000011
    private static final int LOCATION_ERR = 0300;    //3 000000
    private static final int REDUNDANCY = 0600;        //6 000000
    
    // 若状态的后六位均为0，则代表是直接提交的地点重复或地图标记错误
    private static final int DIRECT_SUBMIT = 63;
    
    // 若状态的后六为不全为0，则在详情页面需要选择性显示
    private static final int TEL_LLY = 32;
    private static final int NOTEXIST_RGP = 16;
    private static final int MAIN_LLY = 8;
    private static final int TYPE_LLY = 4;
    private static final int DESCRIPTION_LLY = 2;
    private static final int CONTACT_LLY = 1;
    
    // 用来控制纠错码的个位数
    private static final int NOT_CHECKED = 0;
    
    private static final int NE_STOP = 1;
    private static final int NE_CHAI = 2;
    private static final int NE_MOVE = 3;
    private static final int NE_FIND = 4;
    private static final int NE_OTHER_CHECK = 5;
    
    private static final int TEL_CONNECT = 1;
    private static final int TEL_NOTTHIS = 2;
    
    private View vErreportMain = null;
    private View vErreportDetail = null;
    
    private Context mContext;
    
    private LinearLayout mBodyLly;
    private LinearLayout mTelLly;
    private RadioGroup mNotExistRgp;
    private LinearLayout mMainLly;
    private LinearLayout mTypeLly;
    private LinearLayout mDescriptionLly;
    private LinearLayout mContactLly;
    
    private Button mNameBtn;
    private Button mTelBtn;
    private Button mNotexistBtn;
    private Button mAddressBtn;
    private Button mRedundancyBtn;
    private Button mLocationBtn;
    private Button mOtherBtn;
    private Button mSubmitBtn;
    
    private RadioButton mTelConnectRbt;
    private RadioButton mTelNotthisRbt;
    private RadioButton mNeStopRbt;
    private RadioButton mNeChaiRbt;
    private RadioButton mNeMoveRbt;
    private RadioButton mNeFindRbt;
    private RadioButton mNeOtherRbt;
    
    private TextView mMainTxv;
    private EditText mMainEdt;
    private EditText mTypeEdt;
    private EditText mDescriptionEdt;
    private EditText mContactEdt;
    
    private int mPage;
    private int mChecked;
    private int mRbtChecked;
    private boolean mHasTel;
    private String mNextTitle;
    // POI纠错码 = 510 + (mChecked>>6)*10 + mRbtChecked 
    
    private String mOrigin;

    private POI mPOI;
    
    private static List<Object> sTargetList = new ArrayList<Object>();

    private static final String TAG = "ErrorRecovery";
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.POIErrorRecovery;
        vErreportMain = this.getLayoutInflater().inflate(R.layout.poi_report_error, null);
        vErreportDetail = this.getLayoutInflater().inflate(R.layout.poi_report_error_detail, null);
        setContentView(vErreportMain);
        
        findViewsMain();
        setListenerMain();

        mTitleBtn.setText(R.string.erreport_title);
        mChecked = HOME_PAGE;
        mPage = HOME_PAGE;
        
        mContext = getBaseContext();
        
        synchronized (sTargetList) {
            int size = sTargetList.size();
            if (size > 0) {
                Object target = sTargetList.get(size-1);
                if (target instanceof POI) {
                    mPOI = (POI) target;
                }
            }
        }
        
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViewsMain() {
        super.findViews();
        mNameBtn = (Button) findViewById(R.id.name_btn);
        mTelBtn = (Button) findViewById(R.id.tel_btn);
        mNotexistBtn = (Button) findViewById(R.id.notexist_btn);
        mAddressBtn = (Button) findViewById(R.id.address_btn);
        mRedundancyBtn = (Button) findViewById(R.id.redundancy_btn);
        mLocationBtn = (Button) findViewById(R.id.location_btn);
        mOtherBtn = (Button) findViewById(R.id.other_btn);
        mSubmitBtn = (Button) findViewById(R.id.submit_btn);
    }
    protected void findViewsDetail() {
        super.findViews();
        mBodyLly = (LinearLayout) findViewById(R.id.body_lly);
        mTelLly = (LinearLayout) findViewById(R.id.tel_lly);
        mNotExistRgp = (RadioGroup) findViewById(R.id.notexist_rgp);
        mMainLly = (LinearLayout) findViewById(R.id.main_lly);
        mTypeLly = (LinearLayout) findViewById(R.id.type_lly);
        mDescriptionLly = (LinearLayout) findViewById(R.id.description_lly);
        mContactLly = (LinearLayout) findViewById(R.id.contact_lly);
        mTelConnectRbt = (RadioButton) findViewById(R.id.tel_connect_rbt);
        mTelNotthisRbt = (RadioButton) findViewById(R.id.tel_notthis_rbt);
        mNeStopRbt = (RadioButton) findViewById(R.id.ne_stop_rbt);
        mNeChaiRbt = (RadioButton) findViewById(R.id.ne_chai_rbt);
        mNeMoveRbt = (RadioButton) findViewById(R.id.ne_move_rbt);
        mNeFindRbt = (RadioButton) findViewById(R.id.ne_find_rbt);
        mNeOtherRbt = (RadioButton) findViewById(R.id.ne_other_rbt);
        mMainTxv = (TextView) findViewById(R.id.main_txv);
        mMainEdt = (EditText) findViewById(R.id.main_edt);
        mTypeEdt = (EditText) findViewById(R.id.type_edt);
        mDescriptionEdt = (EditText) findViewById(R.id.description_edt);
        mContactEdt = (EditText) findViewById(R.id.contact_edt);
        mSubmitBtn = (Button) findViewById(R.id.submit_detail_btn);
    }

    /**
     * Set the listeners for the commit and cancel button
     */
    protected void setListenerMain() {
        super.setListener();
        mNameBtn.setOnClickListener(this);
        mTelBtn.setOnClickListener(this);
        mNotexistBtn.setOnClickListener(this);
        mAddressBtn.setOnClickListener(this);
        mRedundancyBtn.setOnClickListener(this);
        mLocationBtn.setOnClickListener(this);
        mOtherBtn.setOnClickListener(this);
        mSubmitBtn.setOnClickListener(this);
    }
    protected void setListenerDetail() {
        super.setListener();
        mLeftBtn.setOnClickListener(this);
        mBodyLly.setOnTouchListener(new OnTouchListener(){
        
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    postHideSoftInput();
                }
                return true;
            }
        });
        mTelConnectRbt.setOnClickListener(this);
        mTelNotthisRbt.setOnClickListener(this);
        mNeStopRbt.setOnClickListener(this);
        mNeChaiRbt.setOnClickListener(this);
        mNeMoveRbt.setOnClickListener(this);
        mNeFindRbt.setOnClickListener(this);
        mNeOtherRbt.setOnClickListener(this);
        mSubmitBtn.setOnClickListener(this);
        TextWatcher textWatcher = new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                refreshDataDetail();
                
            }
        };
        mMainEdt.addTextChangedListener(textWatcher);
        mDescriptionEdt.addTextChangedListener(textWatcher);
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
        boolean show = mSubmitBtn.isEnabled();
        if (mPage == HOME_PAGE || show == false)return false;
        Utility.showNormalDialog(mThis, getString(R.string.erreport_abandon), new DialogInterface.OnClickListener(){

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
    
    protected void jumpToDetail(){
        setContentView(vErreportDetail);
        mPage = DETAIL_PAGE;
        findViewsDetail();
        setListenerDetail();
        setDataDetail();
    }
    
    public static void addTarget(Object obejct) {
        synchronized (sTargetList) {
            sTargetList.add(obejct);
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        if(mPage == HOME_PAGE)setDataMain();
    }
    private void setDataMain(){
        mHasTel = ( mPOI.getTelephone() != null && !TextUtils.isEmpty(mPOI.getTelephone())) ? true : false;
        mTelBtn.setText(mHasTel ? getString(R.string.erreport_tel_error) : getString(R.string.erreport_tel_add));
    }
    private void refreshDataMain(){
        Drawable drawable = getResources().getDrawable(R.drawable.btn_confirm_hotel_normal);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mNameBtn.setCompoundDrawables(null, null, (mChecked == NAME_ERR) ? drawable : null, null);
        mTelBtn.setCompoundDrawables(null, null, (mChecked == TEL_ADD || mChecked == TEL_ERR) ? drawable : null, null);
        mNotexistBtn.setCompoundDrawables(null, null, (mChecked == NOT_EXIST) ? drawable : null, null);
        mAddressBtn.setCompoundDrawables(null, null, (mChecked == ADDRESS_ERR) ? drawable : null, null);
        mRedundancyBtn.setCompoundDrawables(null, null, (mChecked == REDUNDANCY) ? drawable : null, null);
        mLocationBtn.setCompoundDrawables(null, null, (mChecked == LOCATION_ERR) ? drawable : null, null);
        mOtherBtn.setCompoundDrawables(null, null, (mChecked == OTHER_ERR) ? drawable : null, null);
        mSubmitBtn.setVisibility( (mChecked != HOME_PAGE) ? View.VISIBLE : View.GONE);
        mSubmitBtn.setText( ((mChecked & DIRECT_SUBMIT) ==0) ? getString(R.string.submit) : getString(R.string.next_procedure));
    }
    
    private void setDataDetail(){
        mTelLly.setVisibility(((mChecked & TEL_LLY) !=0) ? View.VISIBLE : View.GONE);
        mNotExistRgp.setVisibility(((mChecked & NOTEXIST_RGP) !=0) ? View.VISIBLE : View.GONE);
        mMainLly.setVisibility(((mChecked & MAIN_LLY) !=0) ? View.VISIBLE : View.GONE);
        mTypeLly.setVisibility(((mChecked & TYPE_LLY) !=0) ? View.VISIBLE : View.GONE);
        mDescriptionLly.setVisibility(((mChecked & DESCRIPTION_LLY) !=0) ? View.VISIBLE : View.GONE);
        mContactLly.setVisibility(((mChecked & CONTACT_LLY) !=0) ? View.VISIBLE : View.GONE);
        mTitleBtn.setText(mNextTitle);
        switch(mChecked){
        case TEL_ERR:
            mMainTxv.setText(getString(R.string.erreport_merchant_tel));
            mOrigin = MapEngine.getAreaCodeByCityId(MapEngine.getInstance().getCityId(mPOI.getPosition())) + '-';
            mMainEdt.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
            mMainEdt.setHint(getString(R.string.tel_or_mobile));
            break;
        case ADDRESS_ERR:
            mMainTxv.setText(getString(R.string.erreport_address));
            mOrigin = mPOI.getAddress();
            break;
        case NAME_ERR:
            mMainTxv.setText(getString(R.string.erreport_merchant_name));
            mMainLly.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_header));
            mOrigin = mPOI.getName();
            break;
        case TEL_ADD:
            mMainTxv.setText(getString(R.string.erreport_merchant_tel));
            mOrigin = MapEngine.getAreaCodeByCityId(MapEngine.getInstance().getCityId(mPOI.getPosition())) + '-';
            mMainEdt.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
            mMainEdt.setHint(getString(R.string.tel_or_mobile));
            break;
        }
        if(mMainLly.getVisibility() == View.VISIBLE){
            mMainEdt.setText(mOrigin);
            if(mChecked != TEL_ERR){
                mMainEdt.requestFocus();
                Selection.setSelection(mMainEdt.getText(), mMainEdt.length());
                showSoftInput();
            }
        }
        if(mDescriptionLly.getVisibility() == View.VISIBLE){
            mDescriptionEdt.requestFocus();
            showSoftInput();
        }
        refreshDataDetail();
    }
    
    private void refreshDataDetail(){
        if((mChecked & DESCRIPTION_LLY) !=0){
            mDescriptionLly.setVisibility(View.VISIBLE);
        }else if (mChecked == NOT_EXIST){
            mDescriptionEdt.clearFocus();
            postHideSoftInput();
            mDescriptionLly.setVisibility(View.GONE);
        }
        refreshSubmitBtn();
        mSubmitBtn.setTextColor(mSubmitBtn.isEnabled() 
                ? getResources().getColor(R.color.orange) 
                : getResources().getColor(R.color.black_light));
    }
    public void refreshSubmitBtn(){
        if(mPage == HOME_PAGE) return;
        switch(mChecked){
        case TEL_ERR:
            if(mRbtChecked != NOT_CHECKED) {
                mSubmitBtn.setEnabled(true);
                return;
            }    //不能break
        case NAME_ERR:
        case ADDRESS_ERR:
        case TEL_ADD:
            if(TextUtils.isEmpty(mMainEdt.getText()) || TextUtils.equals(mMainEdt.getText(), mOrigin) )
                mSubmitBtn.setEnabled(false);
            else mSubmitBtn.setEnabled(true);
            break;
        case NOT_EXIST:
            mSubmitBtn.setEnabled( (mRbtChecked != NOT_CHECKED) ? true : false);
            break;
        case NE_OTHER:
        case OTHER_ERR:
            mSubmitBtn.setEnabled( (!TextUtils.isEmpty(mDescriptionEdt.getText()) ? true : false));
            break;
        }
    }
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
        case R.id.left_btn:
            mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
            if (showDiscardDialog() == false) {
                finish();
            }
            break;
        case R.id.name_btn:
            mChecked = NAME_ERR;
            mNextTitle = mNameBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.tel_btn:
            mChecked = mHasTel ? TEL_ERR : TEL_ADD;
            mNextTitle = mTelBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.notexist_btn:
            mChecked = NOT_EXIST;
            mNextTitle = mNotexistBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.address_btn:
            mChecked = ADDRESS_ERR;
            mNextTitle = mAddressBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.redundancy_btn:
            mChecked = REDUNDANCY;
            refreshDataMain();
            break;
        case R.id.location_btn:
            mChecked = LOCATION_ERR;
            refreshDataMain();
            break;
        case R.id.other_btn:
            mChecked = OTHER_ERR;
            mNextTitle = mOtherBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.submit_btn:
            if((mChecked & DIRECT_SUBMIT) == 0) submit();
            else jumpToDetail();
            break;
        case R.id.submit_detail_btn:
            submit();
            break;
        case R.id.ne_stop_rbt:
            mChecked = NOT_EXIST;
            mRbtChecked = NE_STOP;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.ne_chai_rbt:
            mChecked = NOT_EXIST;
            mRbtChecked = NE_CHAI;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.ne_move_rbt:
            mChecked = NOT_EXIST;
            mRbtChecked = NE_MOVE;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.ne_find_rbt:
            mChecked = NOT_EXIST;
            mRbtChecked = NE_FIND;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.ne_other_rbt:
            mChecked = NE_OTHER;
            mRbtChecked = NE_OTHER_CHECK;
            mDescriptionEdt.requestFocus();
            refreshDataDetail();
            showSoftInput();
            break;
        case R.id.tel_connect_rbt:
            mRbtChecked = TEL_CONNECT;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.tel_notthis_rbt:
            mRbtChecked = TEL_NOTTHIS;
            refreshDataDetail();
            hideSoftInput();
            break;
        default:
            break;
        }
    }
    private void submit(){
        StringBuilder s = new StringBuilder();
        try{
            s.append(mPOI.getUUID());
            s.append('_');
            int errcode = 510 + (mChecked >> 6)*10 + mRbtChecked;
            s.append(errcode + "");
            if( (mChecked & MAIN_LLY) != 0 && !TextUtils.isEmpty(mMainEdt.getText()) && !TextUtils.equals(mMainEdt.getText(), mOrigin)){
                s.append('-');
                s.append(URLEncoder.encode(mMainEdt.getText().toString(), TKConfig.getEncoding()));
            }
            if( (mChecked & TYPE_LLY) != 0 && !TextUtils.isEmpty(mTypeEdt.getText())){
                s.append('-');
                s.append(URLEncoder.encode(mTypeEdt.getText().toString(), TKConfig.getEncoding()));
            }
            if( (mChecked & DESCRIPTION_LLY) != 0){
                s.append('-');
                s.append(URLEncoder.encode(mDescriptionEdt.getText().toString(), TKConfig.getEncoding()));
            }
            if( (mChecked & DIRECT_SUBMIT) != 0 && !TextUtils.isEmpty(mContactEdt.getText())){
                s.append('-');
                s.append(URLEncoder.encode(mContactEdt.getText().toString(), TKConfig.getEncoding()));
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        hideSoftInput();
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY, s.toString());
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
        Toast.makeText(mThis, R.string.error_recovery_success, Toast.LENGTH_LONG).show();
        finish();
    }
    
    public void finish() {
        synchronized (sTargetList) {
            int size = sTargetList.size();
            if (size > 0) {
                sTargetList.remove(size-1);
            }
        }
        super.finish();
    }

}

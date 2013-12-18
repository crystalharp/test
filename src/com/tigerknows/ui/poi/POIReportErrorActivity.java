/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 * 2013.06
 */
package com.tigerknows.ui.poi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.FilterResponse;


public class POIReportErrorActivity extends BaseActivity implements View.OnClickListener, FilterListView.CallBack {
    
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
    
    private LinearLayout mBodyLly;
    private LinearLayout mTelLly;
    private RadioGroup mTelRgp;
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
    private Button mTypeBtn;
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
    
    private FilterListView mFilterListView;
    private List<Filter> mFilterList;
    
    private static List<Object> sTargetList = new ArrayList<Object>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.POIReportError;
        vErreportMain = this.getLayoutInflater().inflate(R.layout.poi_report_error, null);
        vErreportDetail = this.getLayoutInflater().inflate(R.layout.poi_report_error_detail, null);
        setContentView(vErreportMain);
        
        findViewsMain();
        setListenerMain();

        mChecked = HOME_PAGE;
        mPage = HOME_PAGE;
        
        getBaseContext();
        
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
        mTelRgp = (RadioGroup) findViewById(R.id.tel_rgp);
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
        mTypeBtn = (Button) findViewById(R.id.type_btn);
        mDescriptionEdt = (EditText) findViewById(R.id.description_edt);
        mContactEdt = (EditText) findViewById(R.id.contact_edt);
        mSubmitBtn = (Button) findViewById(R.id.submit_detail_btn);
        mTelConnectRbt.setChecked(false);
        mTelNotthisRbt.setChecked(false);
        mNotExistRgp.clearCheck();
        mTelRgp.clearCheck();
        mTypeBtn.setText("");
        mDescriptionEdt.setText("");
        mContactEdt.setText("");
        mSubmitBtn.setEnabled(false);
        mRbtChecked = 0;
        mFilterListView = (FilterListView) findViewById(R.id.filter_list_view);
        mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);
        
        if (mFilterList != null) {
            FilterListView.selectedFilter(mFilterList.get(0), -1);
        } else {
            DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, Globals.getCurrentCityInfo().getId(), mThis);
            FilterCategoryOrder filterCategory = DataQuery.getPOIFilterCategoryOrder();
            if (filterCategory != null) {
                List<FilterOption> filterOptionList = new ArrayList<DataQuery.FilterOption>();
                List<FilterOption> online = filterCategory.getCategoryFilterOption();
                if (online != null) {
                    for(int i = 0, size = online.size(); i < size; i++) {
                        filterOptionList.add(online.get(i).clone());
                    }
                }
                List<Long> indexList = new ArrayList<Long>();
                indexList.add(0l);
                for(int i = 0, size = filterOptionList.size(); i < size; i++) {
                    long id = filterOptionList.get(i).getId();
                    indexList.add(id);
                }
                
                // 每个分类下面添加其他
                String otherText = getString(R.string.other);
                
                Filter categoryFilter = DataQuery.makeFilterResponse(mThis, indexList, filterCategory.getVersion(), filterOptionList, FilterCategoryOrder.FIELD_LIST_CATEGORY, false);
                Filter other = categoryFilter.getChidrenFilterList().remove(0);
                other.getFilterOption().setName(otherText);
                
                List<Filter> list = categoryFilter.getChidrenFilterList();
                int endId = Integer.MIN_VALUE;
                for(int i = 0, size = list.size(); i < size; i++) {
                    Filter filter = list.get(i);
                    List<Filter> childrenList = filter.getChidrenFilterList();
                    if (childrenList.size() > 0) {
                        Filter end = childrenList.get(childrenList.size()-1);
                        Filter other1 = end.clone();
                        FilterOption filterOption = other1.getFilterOption();
                        filterOption.setName(otherText);
                        filterOption.setId(endId+i+1);
                        childrenList.add(other1);
                    }
                }
                
                other.setSelected(false);
                list.add(other);
                
                mFilterList = new ArrayList<Filter>();
                mFilterList.add(categoryFilter);
            }
            mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
        }
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
                    hideSoftInput();
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
        OnTouchListener edtTouchListener = new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP){
					switch (v.getId()){
					case R.id.main_edt:
						mActionLog.addAction(mActionTag + ActionLog.POIErrorDetailMain);
						break;
					case R.id.description_edt:
						mActionLog.addAction(mActionTag + ActionLog.POIErrorDetailMain);
						break;
					case R.id.contact_edt:
						mActionLog.addAction(mActionTag + ActionLog.POIErrorDetailContact);
						break;
					}
				}
				return false;
			}
		};
		mMainEdt.setOnTouchListener(edtTouchListener);
		mDescriptionEdt.setOnTouchListener(edtTouchListener);
		mContactEdt.setOnTouchListener(edtTouchListener);
		mTypeBtn.setOnClickListener(this);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mPage == DETAIL_PAGE && mFilterListView.getVisibility() == View.VISIBLE) {
                backHome();
                return true;
            }
            if (showDiscardDialog() == false) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                if(mPage == HOME_PAGE)finish();
                else jumpToMain();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    private boolean showDiscardDialog(){
        boolean show = mSubmitBtn.isEnabled() || (mPage == DETAIL_PAGE && (mChecked & MAIN_LLY)!=0 && TextUtils.isEmpty(mMainEdt.getText().toString().trim()) && !TextUtils.isEmpty(mOrigin.trim()));
        if (mPage == HOME_PAGE || show == false)return false;
        Utility.showNormalDialog(mThis, getString(R.string.erreport_abandon), new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (which == DialogInterface.BUTTON_POSITIVE){
                    jumpToMain();
                }
                
            }
            
        });
        return show;
    }
    protected void jumpToMain(){
    	mActionTag = ActionLog.POIReportError;
    	setContentView(vErreportMain);
    	findViewsMain();
    	setListenerMain();
        mChecked = HOME_PAGE;
        mPage = HOME_PAGE;
    	setDataMain();
    	refreshDataMain();
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
        mTitleBtn.setText(R.string.erreport_title);
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
        InputFilter[] phoneFilter = {new InputFilter.LengthFilter(15)};
        InputFilter[] normalFilter = {new InputFilter.LengthFilter(80)};
        switch(mChecked){
        case TEL_ERR:
        	mActionTag = ActionLog.POITelError;
        	mMainEdt.clearFocus();
        	mMainEdt.clearComposingText();
        	mMainEdt.setInputType(InputType.TYPE_CLASS_PHONE);
        	mMainEdt.setFilters(phoneFilter);
            mMainTxv.setText(getString(R.string.erreport_merchant_tel));
            mOrigin = MapEngine.getAreaCodeByCityId(MapEngine.getCityId(mPOI.getPosition())) + '-';
            mMainEdt.setHint(getString(R.string.tel_or_mobile));
            break;
        case ADDRESS_ERR:
        	mActionTag = ActionLog.POIAddressError;
        	mMainEdt.clearFocus();
        	mMainEdt.clearComposingText();
        	mMainEdt.setInputType(InputType.TYPE_CLASS_TEXT);
        	mMainEdt.setFilters(normalFilter);
            mMainTxv.setText(getString(R.string.erreport_address));
            mMainEdt.setHint(getString(R.string.erreport_address_hint));
            mOrigin = mPOI.getAddress();
            break;
        case NAME_ERR:
        	mActionTag = ActionLog.POINameError;
        	mMainEdt.clearFocus();
        	mMainEdt.clearComposingText();
        	mMainEdt.setInputType(InputType.TYPE_CLASS_TEXT);
        	mMainEdt.setFilters(normalFilter);
            mMainTxv.setText(getString(R.string.erreport_merchant_name));
            mMainEdt.setHint(getString(R.string.erreport_name_hint));
            mMainLly.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_header));
            mTypeBtn.setText( TextUtils.isEmpty(mPOI.getCategory()) ? getString(R.string.erreport_xuantian) : mPOI.getCategory());
            mOrigin = mPOI.getName();
            break;
        case TEL_ADD:
        	mActionTag = ActionLog.POIAddTel;
        	mMainEdt.clearFocus();
        	mMainEdt.clearComposingText();
        	mMainEdt.setInputType(InputType.TYPE_CLASS_PHONE);
        	mMainEdt.setFilters(phoneFilter);
            mMainTxv.setText(getString(R.string.erreport_merchant_tel));
            mOrigin = MapEngine.getAreaCodeByCityId(MapEngine.getCityId(mPOI.getPosition())) + '-';
            mMainEdt.setHint(getString(R.string.tel_or_mobile));
            break;
        case NOT_EXIST:
        case NE_OTHER:
        	mActionTag = ActionLog.POINotExist;
        	break;
        case OTHER_ERR:
        	mActionTag = ActionLog.POIOtherError;
        	break;
        }
        if(mMainLly.getVisibility() == View.VISIBLE){
            mMainEdt.setText(mOrigin);
            if(mChecked == TEL_ERR){
            	Selection.setSelection(mMainEdt.getText(), mMainEdt.length());
            	mDescriptionEdt.requestFocus();		//无法正常清除光标，故让隐藏的Edt获取光标的方式以实现清除光标
            }else{
            	mMainEdt.requestFocus();
                Selection.setSelection(mMainEdt.getText(), mMainEdt.length());
                showSoftInput(mMainEdt);
            }
        }
        if(mDescriptionLly.getVisibility() == View.VISIBLE){
        	mMainEdt.requestFocus();
            mDescriptionEdt.requestFocus();
            showSoftInput();
        }
        mActionLog.addAction(mActionTag);
        refreshDataDetail();
    }
    
    private void refreshDataDetail(){
        if((mChecked & DESCRIPTION_LLY) !=0){
            mDescriptionLly.setVisibility(View.VISIBLE);
        }else if (mChecked == NOT_EXIST){
            mDescriptionEdt.clearFocus();
            hideSoftInput();
            mDescriptionLly.setVisibility(View.GONE);
            mDescriptionEdt.requestFocus();		//无法正常清除光标，故让隐藏的Edt获取光标的方式以实现清除光标
        }
        refreshSubmitBtn();
        mSubmitBtn.setTextColor(mSubmitBtn.isEnabled() 
                ? getResources().getColor(R.color.orange) 
                : getResources().getColor(R.color.black_light));
        if((mChecked & TYPE_LLY)!=0) refreshTypeBtn();
    }
    
    public void refreshTypeBtn(){
        if(TextUtils.isEmpty(mTypeBtn.getText())){
        	mTypeBtn.setText(getString(R.string.erreport_xuantian));
        	mTypeBtn.setTextColor(getResources().getColor(R.color.black_light));
        }else if(TextUtils.equals(mTypeBtn.getText(), getString(R.string.erreport_xuantian))){
        	mTypeBtn.setTextColor(getResources().getColor(R.color.black_light));
        }else{
        	mTypeBtn.setTextColor(getResources().getColor(R.color.black_dark));
        }
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
            if(TextUtils.isEmpty(mMainEdt.getText().toString().trim()) || TextUtils.equals(mMainEdt.getText().toString().trim(), mOrigin) )
                mSubmitBtn.setEnabled(false);
            else mSubmitBtn.setEnabled(true);
            break;
        case NOT_EXIST:
            mSubmitBtn.setEnabled( (mRbtChecked != NOT_CHECKED) ? true : false);
            break;
        case NE_OTHER:
        case OTHER_ERR:
            mSubmitBtn.setEnabled( (!TextUtils.isEmpty(mDescriptionEdt.getText().toString().trim()) ? true : false));
            break;
        }
    }
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
        case R.id.left_btn:
            if (mFilterListView.getVisibility() == View.VISIBLE) {
                backHome();
                return;
            }
            if (showDiscardDialog() == false) {
            	mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
                if(mPage == HOME_PAGE)finish();
                else {
                	hideSoftInput();
                	jumpToMain();
                }
            }
            break;
        case R.id.name_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIReportErrorName);
            mChecked = NAME_ERR;
            mNextTitle = mNameBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.tel_btn:
        	mActionLog.addAction(mActionTag + (mHasTel ? ActionLog.POIReportErrorTel : ActionLog.POIReportErrorTelAdd));
            mChecked = mHasTel ? TEL_ERR : TEL_ADD;
            mNextTitle = mTelBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.notexist_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIReportErrorNotExist);
            mChecked = NOT_EXIST;
            mNextTitle = mNotexistBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.address_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIReportErrorAddress);
            mChecked = ADDRESS_ERR;
            mNextTitle = mAddressBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.redundancy_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIReportErrorRedundancy);
            mChecked = REDUNDANCY;
            refreshDataMain();
            break;
        case R.id.location_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIReportErrorLocation);
            mChecked = LOCATION_ERR;
            refreshDataMain();
            break;
        case R.id.other_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIReportErrorOther);
            mChecked = OTHER_ERR;
            mNextTitle = mOtherBtn.getText().toString();
            refreshDataMain();
            break;
        case R.id.submit_btn:
        	mActionLog.addAction(mActionTag + ((mPage == DETAIL_PAGE || (mChecked & DIRECT_SUBMIT) == 0) ? ActionLog.POIReportErrorSubmit : ActionLog.POIReportErrorNext));
            if((mChecked & DIRECT_SUBMIT) == 0) submitDetail();
            else {
            	submitMain();
            	jumpToDetail();
            }
            break;
        case R.id.submit_detail_btn:
        	mActionLog.addAction(mActionTag + ActionLog.POIErrorDetailSubmit);
            submitDetail();
            break;
        case R.id.ne_stop_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POINotExistStop);
            mChecked = NOT_EXIST;
            mRbtChecked = NE_STOP;
            hideSoftInput(mDescriptionEdt);
            refreshDataDetail();
            break;
        case R.id.ne_chai_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POINotExistChai);
            mChecked = NOT_EXIST;
            mRbtChecked = NE_CHAI;
            hideSoftInput(mDescriptionEdt);
            refreshDataDetail();
            break;
        case R.id.ne_move_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POINotExistMove);
            mChecked = NOT_EXIST;
            mRbtChecked = NE_MOVE;
            hideSoftInput(mDescriptionEdt);
            refreshDataDetail();
            break;
        case R.id.ne_find_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POINotExistFind);
            mChecked = NOT_EXIST;
            mRbtChecked = NE_FIND;
            hideSoftInput(mDescriptionEdt);
            refreshDataDetail();
            break;
        case R.id.ne_other_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POINotExistOther);
            mChecked = NE_OTHER;
            mRbtChecked = NE_OTHER_CHECK;
            mDescriptionEdt.requestFocus();
            refreshDataDetail();
            showSoftInput(mDescriptionEdt);
            break;
        case R.id.tel_connect_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POITelErrorConnect);
            mRbtChecked = TEL_CONNECT;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.tel_notthis_rbt:
        	mActionLog.addAction(mActionTag + ActionLog.POITelErrorNotthis);
            mRbtChecked = TEL_NOTTHIS;
            refreshDataDetail();
            hideSoftInput();
            break;
        case R.id.type_btn:
            mActionLog.addAction(mActionTag + ActionLog.POINameErrorType);
            mTitleBtn.setText(R.string.merchant_type);
            mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
            mFilterListView.setVisibility(View.VISIBLE);
            postHideSoftInput();
            break;
        default:
            break;
        }
    }
    private void submitMain(){
    	StringBuilder s = new StringBuilder();
    	s.append(mPOI.getUUID());
    	s.append('_');
    	int errcode = 510 + (mChecked >> 6)*10;
    	s.append(errcode + "");
        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY, s.toString());
        feedbackUpload.addLocalParameter(FeedbackUpload.LOCAL_PARAMETER_POIERROR_IGNORE, "true");
        feedbackUpload.setup(Globals.getCurrentCityInfo().getId());
        queryStart(feedbackUpload);
    }

    private void submitDetail(){
        StringBuilder s = new StringBuilder();
        try{
            s.append(mPOI.getUUID());
            s.append('_');
            int errcode = 510 + (mChecked >> 6)*10 + mRbtChecked;
            s.append(errcode + "");
            if( (mChecked & MAIN_LLY) != 0 && !TextUtils.isEmpty(mMainEdt.getText()) && !TextUtils.equals(mMainEdt.getText(), mOrigin)){
                s.append('-');
                s.append(mMainTxv.getText());
                s.append(URLEncoder.encode(mMainEdt.getText().toString().trim(), TKConfig.getEncoding()));
            }
            if( (mChecked & TYPE_LLY) != 0 && !TextUtils.isEmpty(mTypeBtn.getText())){
                s.append('-');
                s.append(getString(R.string.erreport_merchant_type));
                s.append(URLEncoder.encode(mTypeBtn.getText().toString(), TKConfig.getEncoding()));
            }
            if( (mChecked & DESCRIPTION_LLY) != 0){
                s.append('-');
                s.append(getString(R.string.other_colon));
                s.append(URLEncoder.encode(mDescriptionEdt.getText().toString().trim(), TKConfig.getEncoding()));
            }
            if( (mChecked & DIRECT_SUBMIT) != 0 && !TextUtils.isEmpty(mContactEdt.getText())){
                s.append('-');
                s.append(getString(R.string.erreport_contact));
                s.append(URLEncoder.encode(mContactEdt.getText().toString(), TKConfig.getEncoding()));
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        hideSoftInput();
        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_ERROR_RECOVERY, s.toString());
        feedbackUpload.setup(Globals.getCurrentCityInfo().getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
        queryStart(feedbackUpload);
    }
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        }
        
        if(! baseQuery.hasLocalParameter(FeedbackUpload.LOCAL_PARAMETER_POIERROR_IGNORE)){
        	if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, this, false)) {
        		return;
        	}
        	Toast.makeText(mThis, R.string.error_recovery_success, Toast.LENGTH_LONG).show();
        	finish();
        }
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

    @Override
    public void doFilter(String name) {
        backHome();
        Filter categoryFitler = mFilterList.get(0);
        List<Filter> list = categoryFitler.getChidrenFilterList();
        for(int i = 0, size = list.size(); i < size; i++) {
            Filter filter = list.get(i);
            if (filter.isSelected()) {
                mTypeBtn.setText(filter.getFilterOption().getName());
                refreshTypeBtn();
                return;
            }
            List<Filter> chidrenList = filter.getChidrenFilterList();
            for(int j = 0, count = chidrenList.size(); j < count; j++) {
                Filter chidren = chidrenList.get(j);
                if (chidren.isSelected()) {
                    mTypeBtn.setText(filter.getFilterOption().getName()+"-"+chidren.getFilterOption().getName());
                    refreshTypeBtn();
                    return;
                }
            }
        }
    }

    @Override
    public void cancelFilter() {
        backHome();
    }
    
    void backHome() {
        mFilterListView.setVisibility(View.GONE);
        mTitleBtn.setText(R.string.erreport_name_error);
    }

}

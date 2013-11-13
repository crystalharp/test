package com.tigerknows.ui.more;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;

import com.tigerknows.R;
import com.tigerknows.TKConfig;

import com.decarta.Globals;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.FileUpload;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.common.AddPictureActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.MultichoiceArrayAdapter;
import com.tigerknows.widget.TimeListView;

public class AddMerchantActivity extends BaseActivity implements View.OnClickListener, FilterListView.CallBack {
    
    public static final String EXTRA_INPUT_TEXT = "input_text";
    
    public static final int REQUEST_CODE_PICK_PICTURE = 1;
    
    public static final int REQUEST_CODE_CHANGE_CITY = 2;
    
    private String mPhotoMD5;
    
    private ImageButton mTakePhotoBtn;
    
	private ScrollView mAddMerchantScv;
    private Button mType;
    private FilterListView mFilterListView;
    private List<Filter> mFilterList;
    
    private EditText mNameEdt;
    private Button mCityBtn;
    private EditText mAddressEdt;
    private EditText mAddressDescriptionEdt;
    private EditText mTelephoneEdt;
    private String mLastAreaCode;
    
    public static final int DATE_EVERY_DAY = 127;
    public static final int DATE_WORKING_DAYS = 31;
    public static final int DATE_WEEKEND = 96;
    
    private Button mDateBtn;
    private MultichoiceArrayAdapter mPickDateArrayAdapter;
    private String[] mWeekDays;
    private boolean[] mDateChecked;
    private int mDateSelected = DATE_EVERY_DAY;
    private Dialog mPickDateDialog;
    
    private Button mTimeBtn;
    private View mPickTimeView;
    private TimeListView mStartTimeListView;
    private TimeListView mEndTimeListView;
    private int mStartHourPosition = 12;
    private int mStartMinutePosition = 2;
    private int mEndHourPosition = 24;
    private int mEndMinutePosition = 2;
    private Button mTimeConfirmBtn;
    
    protected PopupWindow mPopupWindow;
    
    void dismissPopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }
    
    private EditText mYourTelephoneEdt = null;
    
    private Button mDeletePhotoBtn;

    private TextView mPhotoTitleTxv;
    
    private TextView mPhotoDescriptionTxv;
    
    private Dialog mProgressDialog = null;
    
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
        
        CityInfo cityInfo = Globals.getCurrentCityInfo();
        
        mCityBtn.setText(cityInfo.getCName());
        mLastAreaCode = MapEngine.getAreaCodeByCityId(cityInfo.getId())+"-";
        mTelephoneEdt.setText(mLastAreaCode);
        mTelephoneEdt.setSelection(mLastAreaCode.length());
        
        mDeletePhotoBtn.setVisibility(View.GONE);

        mPickTimeView = mLayoutInflater.inflate(R.layout.more_add_merchant_pick_time, null, false);
        View v = mLayoutInflater.inflate(R.layout.time_list_item, (ViewGroup)mPickTimeView, false);
        v.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
        v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int h = v.getMeasuredHeight();
        mPickTimeView.findViewById(R.id.body_view).getLayoutParams().height = h*5;
        mStartTimeListView = (TimeListView) mPickTimeView.findViewById(R.id.start_tlv);
        mEndTimeListView = (TimeListView) mPickTimeView.findViewById(R.id.end_tlv);
        mTimeConfirmBtn = (Button) mPickTimeView.findViewById(R.id.time_confirm_btn);
        mTimeConfirmBtn.setOnClickListener(this);
        mPickTimeView.findViewById(R.id.root_view).setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dismissPopupWindow();
                return false;
            }
        });
        mPickTimeView.findViewById(R.id.body_view).setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mHandler = new Handler();
        
        setFilterCategoryOrder(true);
        showSoftInput(mNameEdt);
    }
    
    boolean setFilterCategoryOrder(boolean updateNameEdt) {
        boolean result = false;
        DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, Globals.getCurrentCityInfo().getId(), mThis);
        FilterCategoryOrder filterCategory = DataQuery.getPOIFilterCategoryOrder();
        Filter categoryFitler = null;
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
            
            categoryFitler = DataQuery.makeFilterResponse(mThis, indexList, filterCategory.getVersion(), filterOptionList, FilterCategoryOrder.FIELD_LIST_CATEGORY, false);
            Filter other = categoryFitler.getChidrenFilterList().remove(0);
            other.getFilterOption().setName(otherText);
            
            List<Filter> list = categoryFitler.getChidrenFilterList();
            int endId = Integer.MIN_VALUE;
            for(int i = 0, size = list.size(); i < size; i++) {
                Filter filter = list.get(i);
                List<Filter> chidrenList = filter.getChidrenFilterList();
                if (chidrenList.size() > 0) {
                    Filter end = chidrenList.get(chidrenList.size()-1);
                    Filter other1 = end.clone();
                    FilterOption filterOption = other1.getFilterOption();
                    filterOption.setName(otherText);
                    filterOption.setId(endId+i+1);
                    chidrenList.add(other1);
                }
            }
            
            other.setSelected(false);
            list.add(other);
            
            mFilterList = new ArrayList<Filter>();
            mFilterList.add(categoryFitler);
            mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
            
            result = true;
        }
        
        if (updateNameEdt) {
            Intent intent = getIntent();
            if (intent != null) {
                String intputText = intent.getStringExtra(EXTRA_INPUT_TEXT);
                if (intputText != null) {
                    intputText = intputText.trim();
                    if (categoryFitler != null) {
                        List<Filter> list = categoryFitler.getChidrenFilterList();
                        for(int i = 0, size = list.size(); i < size; i++) {
                            Filter filter = list.get(i);
                            List<Filter> chidrenList = filter.getChidrenFilterList();
                            for(int j = 0, s = chidrenList.size(); j < s; j++) {
                                if (chidrenList.get(j).getFilterOption().getName().equals(intputText)) {
                                    intputText = null;
                                    break;
                                }
                            }
                            
                            if (filter.getFilterOption().getName().equals(intputText)) {
                                intputText = null;
                            }
                            
                            if (intputText == null) {
                                break;
                            }
                        }
                    }
                    if (TextUtils.isEmpty(intputText) == false) {
                        mNameEdt.append(intputText);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();
        mAddMerchantScv = (ScrollView)findViewById(R.id.add_merchant_scv);
        mNameEdt = (EditText)findViewById(R.id.name_edt);
        mType = (Button)findViewById(R.id.type_btn);
        mFilterListView = (FilterListView) findViewById(R.id.filter_list_view);
        mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);
        mCityBtn = (Button)findViewById(R.id.city_btn);
        mAddressEdt = (EditText)findViewById(R.id.address_edt);
        mAddressDescriptionEdt = (EditText)findViewById(R.id.address_description_edt);
        mTelephoneEdt = (EditText)findViewById(R.id.telephone_edt);
        mDateBtn = (Button)findViewById(R.id.date_btn);
        mTimeBtn = (Button)findViewById(R.id.time_btn);
        mYourTelephoneEdt = (EditText)findViewById(R.id.your_telephone_edt);
        mTakePhotoBtn = (ImageButton) findViewById(R.id.take_photo_btn);
        mDeletePhotoBtn = (Button) findViewById(R.id.delete_photo_btn);
        mPhotoTitleTxv = (TextView) findViewById(R.id.upload_image_title_txv);
        mPhotoDescriptionTxv = (TextView) findViewById(R.id.upload_image_description_txv);
    }

    /**
     * Set the listeners for the commit and cancel button
     */
    protected void setListener() {
        super.setListener();
        mLeftBtn.setOnClickListener(this);
        mRightBtn.setOnClickListener(this);
        mTakePhotoBtn.setOnClickListener(this);
        mDeletePhotoBtn.setOnClickListener(this);
        mCityBtn.setOnClickListener(this);
        mDateBtn.setOnClickListener(this);
        mTimeBtn.setOnClickListener(this);
        
        mType.setOnClickListener(this);
        
        OnTouchListener onTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    switch (v.getId()) {
                        case R.id.name_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantName);
                            break;
                            
                        case R.id.address_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantAddress);
                            break;
                            
                        case R.id.telephone_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantTelephone);
                            break;
                            
                        case R.id.address_description_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantAddressDescription);
                            break;
                            
                        case R.id.your_telephone_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantMobile);
                            break;
                            
                        default:
                            break;
                    }
                }
                return false;
            }
        };
        mNameEdt.setOnTouchListener(onTouchListener);
        mAddressEdt.setOnTouchListener(onTouchListener);
        mTelephoneEdt.setOnTouchListener(onTouchListener);
        mAddressDescriptionEdt.setOnTouchListener(onTouchListener);
        mYourTelephoneEdt.setOnTouchListener(onTouchListener);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        dismissPopupWindow();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mFilterListView.getVisibility() == View.VISIBLE) {
                mFilterListView.setVisibility(View.GONE);
                backHome();
            } else {
                exit();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    void backHome() {
        mTitleBtn.setText(R.string.add_merchant);
        mRightBtn.setVisibility(View.VISIBLE);
    }
    
    private void exit() {
        boolean notEmpty = !TextUtils.isEmpty(mNameEdt.getText().toString());
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mType.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mAddressEdt.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !(mTelephoneEdt.getText().toString().equals(mLastAreaCode));
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mAddressDescriptionEdt.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mDateBtn.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mTimeBtn.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = (mPhotoMD5 != null);
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mYourTelephoneEdt.getText().toString());
        }
        
        if (notEmpty == false) {
            finish();
            return;
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
                if (mFilterListView.getVisibility() == View.VISIBLE) {
                    mFilterListView.setVisibility(View.GONE);
                    backHome();
                } else {
                    mActionLog.addAction(mActionTag +  ActionLog.TitleLeftButton);
                    exit();
                }
                break;
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                submit();
                break;
                
            case R.id.take_photo_btn:
                if (mPhotoMD5 == null) {
                    mActionLog.addAction(mActionTag +  ActionLog.AddMerchantPhoto);
                    hideSoftInput(false);

                    Intent intent = new Intent(mThis, AddPictureActivity.class);
                    intent.putExtra(FileUpload.SERVER_PARAMETER_REF_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
                    startActivityForResult(intent, REQUEST_CODE_PICK_PICTURE);
                }
                break;
                
            case R.id.delete_photo_btn:

                if (mPhotoMD5 != null) {
                    mActionLog.addAction(mActionTag +  ActionLog.AddMerchantDeletePhoto);
                    Utility.showNormalDialog(mThis, getString(R.string.add_merchant_delete_photo_tip), new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                mPhotoMD5 = null;
                                mTakePhotoBtn.setScaleType(ScaleType.FIT_XY);
                                mTakePhotoBtn.setImageResource(R.drawable.btn_take_photo);
                                mDeletePhotoBtn.setVisibility(View.GONE);
                                mPhotoTitleTxv.setText(R.string.add_merchant_upload_photo);
                                mPhotoDescriptionTxv.setText(R.string.add_merchant_upload_photo_not_added);
                                mTakePhotoBtn.setBackgroundResource(R.drawable.btn_take_photo);
                            }
                        }
                    });
                }
                break;
                
            case R.id.city_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddMerchantCity);
                hideSoftInput(false);
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                intent.setClass(mThis, ChangeCityActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CHANGE_CITY);
                break;
                
            case R.id.date_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddMerchantDate);
                hideSoftInput(false);
                if (mWeekDays == null) {
                    mWeekDays = mThis.getResources().getStringArray(R.array.week_days);
                    String sunday = mWeekDays[0];
                    int length = mWeekDays.length;
                    for(int i = 1; i < length; i++) {
                        mWeekDays[i-1] = mWeekDays[i];
                    }
                    mWeekDays[length-1] = sunday;
                    mDateChecked = new boolean[mWeekDays.length];
                }

                if (mPickDateDialog == null) {
                    
                    mPickDateArrayAdapter = new MultichoiceArrayAdapter(mThis, mWeekDays, mDateChecked);
                    
                    View alterListView = mLayoutInflater.inflate(R.layout.alert_listview, null, false);
                    
                    ListView listView = (ListView) alterListView.findViewById(R.id.listview);
                    listView.setAdapter(mPickDateArrayAdapter);
                    
                    mPickDateDialog = Utility.getChoiceDialog(mThis, alterListView, R.style.AlterChoiceDialog);
                    
                    listView.setOnItemClickListener(new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                            mDateChecked[position] = !mDateChecked[position];
                            mPickDateArrayAdapter.notifyDataSetChanged();
                        }
                    });
                    
                    TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
                    titleTxv.setText(R.string.select_business_hours_day);
                    
                    Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
                    button.setOnClickListener(new View.OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            int dateSelected = 0;
                            for(int i = mDateChecked.length-1; i >= 0; i--) {
                                dateSelected = (dateSelected << 1) + (mDateChecked[i] ? 1 : 0);
                            }
                            
                            mDateSelected = dateSelected;
                            if (mDateSelected == 0) {
                                mDateBtn.setText(null);
                            } else if (mDateSelected == DATE_EVERY_DAY) {
                                mDateBtn.setText(R.string.every_day);
                            } else if (mDateSelected == DATE_WORKING_DAYS) {
                                mDateBtn.setText(R.string.working_days);
                            } else if (mDateSelected == DATE_WEEKEND) {
                                mDateBtn.setText(R.string.weekend);
                            } else {
                                StringBuilder s = new StringBuilder();
                                for(int i = 0, length = mDateChecked.length; i < length; i++) {
                                    if (mDateChecked[i]) {
                                        if (s.length() > 0) {
                                            s.append(',');
                                        }
                                        s.append(mWeekDays[i]);
                                    }
                                }
                                mDateBtn.setText(s.toString());
                            }
                            mPickDateDialog.dismiss();
                        }
                    });
                }
                if (mPickDateDialog.isShowing() == false) {
                    mPickDateDialog.show();
                }

                int dateSelected = mDateSelected;
                for(int i = 0, length = mDateChecked.length; i < length; i++) {
                    if (i > 0) {
                        dateSelected = (dateSelected >>> 1);
                    }
                    if ((dateSelected & 0x1) > 0) {
                        mDateChecked[i] = true;
                    } else {
                        mDateChecked[i] = false;
                    }
                }
                mPickDateArrayAdapter.notifyDataSetChanged();
                break;
                
            case R.id.time_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddMerchantTime);
                hideSoftInput(false);
                showPopupWindow();
                break;
                
            case R.id.type_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddMerchantType);
                hideSoftInput(false);
                if (setFilterCategoryOrder(false)) {
                    showFilterListView();
                } else {
                    queryFilter();
                    showProgressDialog();
                }
                break;
                
            case R.id.time_confirm_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddMerchantConfirmTime);
                mStartHourPosition = mStartTimeListView.getHourPosition();
                mStartMinutePosition = mStartTimeListView.getMinutePosition();
                mEndHourPosition = mEndTimeListView.getHourPosition();
                mEndMinutePosition = mEndTimeListView.getMinutePosition();
                mTimeBtn.setText(mStartTimeListView.getHour()+":"+mStartTimeListView.getMinute()+"-"+mEndTimeListView.getHour()+":"+mEndTimeListView.getMinute());
                dismissPopupWindow();
                break;
                
            default:
                
        }
    }
    
    void showFilterListView() {
        mTitleBtn.setText(R.string.merchant_type);
        mRightBtn.setVisibility(View.GONE);
        mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
        mFilterListView.setVisibility(View.VISIBLE);
    }
    
    /**
     * 显示进度对话框
     * @param id
     */
    void showProgressDialog() {
        if (mProgressDialog == null) {
            View custom = mThis.getLayoutInflater().inflate(R.layout.loading, null);
            TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
            loadingTxv.setText(mThis.getString(R.string.doing_and_wait));
            mProgressDialog = Utility.showNormalDialog(mThis, custom);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        if (mProgressDialog.isShowing() == false) {
            mProgressDialog.show();
        }
        
    }
    
    /**
     * 关闭进度对话框
     */
    void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void queryFilter() {
        DataQuery dataQuery = new DataQuery(mThis);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_FILTER);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CONFIGINFO, DataQuery.CONFIGINFO_POI_CATEGORY_ORDER);
        dataQuery.setup(Globals.getCurrentCityInfo(false).getId(), mId, mId, null, true);
        mThis.queryStart(dataQuery);
    }
    
    void submit() {
        StringBuilder s = new StringBuilder();
        try {
            char splitChar = '~';
            String nullStr = "null";
            
            String name = mNameEdt.getText().toString().trim();
            
            if (!TextUtils.isEmpty(name)) {
                s.append(URLEncoder.encode(name, TKConfig.getEncoding()));
            } else {
                mNameEdt.requestFocus();
                mAddMerchantScv.scrollTo(0, 0);
                Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.merchant_name)+"!", Toast.LENGTH_SHORT).show();
                showSoftInput();
                return;
            }
            
            String type = mType.getText().toString().trim();
            if (!TextUtils.isEmpty(type)) {
                s.append(splitChar);
                s.append(URLEncoder.encode(type.toString(), TKConfig.getEncoding()));
            } else {
                mType.requestFocus();
                Toast.makeText(mThis, mThis.getString(R.string.please_select)+mThis.getString(R.string.merchant_type)+"!", Toast.LENGTH_SHORT).show();
                return;
            }

            String address = mAddressEdt.getText().toString().trim();
            if (!TextUtils.isEmpty(address)) {
                s.append(splitChar);
                s.append(URLEncoder.encode(mCityBtn.getText()+address, TKConfig.getEncoding()));
            } else {
                mAddressEdt.requestFocus();
                Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.address)+"!", Toast.LENGTH_SHORT).show();
                showSoftInput();
                return;
            }
            
            s.append(splitChar);
            String str = mTelephoneEdt.getText().toString().trim();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mAddressDescriptionEdt.getText().toString().trim();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mDateBtn.getText().toString().trim();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mTimeBtn.getText().toString().trim();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            if (mPhotoMD5 != null) {
                s.append(URLEncoder.encode(mPhotoMD5, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mYourTelephoneEdt.getEditableText().toString().trim();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        hideSoftInput(false);
        
        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_ADD_MERCHANT, s.toString());
        feedbackUpload.setup(Globals.getCurrentCityInfo().getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
        
        queryStart(feedbackUpload);
    }
    
    protected boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
                List<BaseQuery> list = new ArrayList<BaseQuery>();
                for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                    BaseQuery baseQuery = mBaseQuerying.get(i);
                    Response response = baseQuery.getResponse();
                    if (response == null ||
                            response.getResponseCode() == Response.RESPONSE_CODE_SESSION_INVALID ||
                            response.getResponseCode() == Response.RESPONSE_CODE_LOGON_EXIST) {
                        baseQuery.setResponse(null);
                        list.add(baseQuery);
                    }
                }
                queryStart(list);;
            }
        }
        return isRelogin;
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        dismissProgressDialog();
        
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, this, false)) {
            return;
        }
        
        if (baseQuery instanceof DataQuery) {
            if (setFilterCategoryOrder(false)) {
                showFilterListView();
            }
            return;
        }
        Toast.makeText(mThis, R.string.add_merchant_success, Toast.LENGTH_LONG).show();
        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_PICTURE) {
                Uri uri = data.getData();
                setPhoto(uri);
            } else if (requestCode == REQUEST_CODE_CHANGE_CITY) {
                if (data != null && RESULT_OK == resultCode) {
                    CityInfo cityInfo = data.getParcelableExtra(ChangeCityActivity.EXTRA_CITYINFO);
                    mCityBtn.setText(cityInfo.getCName());

                    String areaCode = MapEngine.getAreaCodeByCityId(cityInfo.getId())+"-";
                    String lastAreaCode = mTelephoneEdt.getEditableText().toString();
                    if (lastAreaCode.equals(mLastAreaCode) || TextUtils.isEmpty(lastAreaCode)) {
                        mTelephoneEdt.setText(areaCode);
                        mTelephoneEdt.setSelection(areaCode.length());
                        mLastAreaCode = areaCode;
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public void setPhoto(final Uri uri) {
        if (uri == null) {
            return;
        }
        
        String fileName = Utility.imageUri2FilePath(mThis, uri);
        mPhotoMD5 = Utility.md5sum(fileName);
        mTakePhotoBtn.setScaleType(ScaleType.CENTER_INSIDE);
        int wh = Utility.dip2px(mThis, 72);
        Bitmap bm = Utility.getBitmapByUri(mThis, uri, wh, wh);
        mTakePhotoBtn.setImageBitmap(bm);
        mTakePhotoBtn.setBackgroundDrawable(null);
        mDeletePhotoBtn.setVisibility(View.VISIBLE);
        mPhotoTitleTxv.setText(R.string.add_merchant_upload_photo_added);
        mPhotoDescriptionTxv.setText(R.string.wait_submit);   
    }

    @Override
    public void doFilter(String name) {
        mFilterListView.setVisibility(View.GONE);
        backHome();
        Filter categoryFitler = mFilterList.get(0);
        List<Filter> list = categoryFitler.getChidrenFilterList();
        for(int i = 0, size = list.size(); i < size; i++) {
            Filter filter = list.get(i);
            if (filter.isSelected()) {
                mType.setText(filter.getFilterOption().getName());
                return;
            }
            List<Filter> chidrenList = filter.getChidrenFilterList();
            for(int j = 0, count = chidrenList.size(); j < count; j++) {
                Filter chidren = chidrenList.get(j);
                if (chidren.isSelected()) {
                    mType.setText(filter.getFilterOption().getName()+"-"+chidren.getFilterOption().getName());
                    return;
                }
            }
        }
    }

    @Override
    public void cancelFilter() {
        mFilterListView.setVisibility(View.GONE);
        backHome();
    }
    
    void showPopupWindow() {
        if (mPopupWindow == null) {
            
            mPopupWindow = new PopupWindow(mPickTimeView);
            mPopupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            mPopupWindow.setFocusable(true);
            // 设置允许在外点击消失
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mStartTimeListView.reset();
                    mEndTimeListView.reset();
                }
            });

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setAnimationStyle(R.style.AlterImageDialog);
            mPopupWindow.update();
        }
        
        mPopupWindow.showAsDropDown(findViewById(R.id.skyline_view), 0, 0);
        
        mStartTimeListView.setData(mStartHourPosition, mStartMinutePosition);
        mEndTimeListView.setData(mEndHourPosition, mEndMinutePosition);
    }
}

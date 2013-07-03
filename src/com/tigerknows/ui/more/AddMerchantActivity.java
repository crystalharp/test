package com.tigerknows.ui.more;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ScrollView;

import com.tigerknows.R;
import com.tigerknows.TKConfig;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.MultichoiceArrayAdapter;
import com.tigerknows.widget.TimeListView;

public class AddMerchantActivity extends BaseActivity implements View.OnClickListener, FilterListView.CallBack {
    
    public static final String EXTRA_INPUT_TEXT = "input_text";

    public static final int REQUEST_CODE_PICK_PHOTO = 0;

    public static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    
    public static final int REQUEST_CODE_CHANGE_CITY = 2;
    
    private String mCacheFilePath;
    
    private String mCameraFilePath;
    
    private Uri mCaptureUri;
    private Uri mPhotoUri;
    private Uri mUploadUri;
    
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
    
    private Button mDateBtn;
    private ListView mPickDateView;
    private MultichoiceArrayAdapter mPickDateArrayAdapter;
    private String[] mWeekDays;
    private boolean[] mDateChecked;
    private Dialog mPickDateDialog;
    
    private Button mTimeBtn;
    private View mPickTimeView;
    private TimeListView mStartTimeListView;
    private TimeListView mEndTimeListView;
    private Dialog mPickTimeDialog;
    
    private EditText mYourTelephoneEdt = null;
    
    private ImageView mImageImv;
    
    private View mImageView;
    
    private Button mDeletePhotoBtn;

    private TextView mPhotoTitleTxv;
    
    private TextView mPhotoDescriptionTxv;
    
    private Button mCancelBtn;
    
    private Button mConfirmBtn;
    
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
        
        mNameEdt.requestFocus();
        
        mCacheFilePath = Environment.getExternalStorageDirectory()+ "/Android/data/" + getPackageName() + "/files"+"/cache.jpg";
        mCameraFilePath = Environment.getExternalStorageDirectory()+ "/Android/data/" + getPackageName() + "/files"+"/camera.jpg";
        
        CityInfo cityInfo = Globals.getCurrentCityInfo();
        
        mCityBtn.setText(cityInfo.getCName());
        mLastAreaCode = MapEngine.getAreaCodeByCityId(cityInfo.getId())+"-";
        mTelephoneEdt.setText(mLastAreaCode);
        mTelephoneEdt.setSelection(mLastAreaCode.length());
        
        Intent intent = getIntent();
        if (intent != null) {
            String intputText = intent.getStringExtra(EXTRA_INPUT_TEXT);
            if (intputText != null) {
                mNameEdt.setText(intputText);
                mNameEdt.setSelection(intputText.length());
            }
        }
        
        mImageView.setVisibility(View.GONE);
        mDeletePhotoBtn.setVisibility(View.GONE);

        mPickTimeView = mLayoutInflater.inflate(R.layout.more_add_merchant_pick_time, null, false);
        mStartTimeListView = (TimeListView) mPickTimeView.findViewById(R.id.start_tlv);
        mEndTimeListView = (TimeListView) mPickTimeView.findViewById(R.id.end_tlv);
        
        mStartTimeListView.setData(9, 6);
        mEndTimeListView.setData(21, 6);
        
        mPickTimeDialog = Utility.getDialog(mThis, 
                mThis.getString(R.string.comment_food_restair), 
                null,
                mPickTimeView,
                mThis.getString(R.string.confirm), 
                mThis.getString(R.string.cancel), 
                new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface arg0, int id) {
                mTimeBtn.setText(mStartTimeListView.getHour()+":"+mStartTimeListView.getMinute()+"-"+mEndTimeListView.getHour()+":"+mEndTimeListView.getMinute());
            }
        });

        DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, Globals.getCurrentCityInfo().getId(), mThis);
        FilterCategoryOrder filterCategory = DataQuery.getPOIFilterCategoryOrder();
        if (filterCategory != null) {
            List<FilterOption> filterOptionList = filterCategory.getCategoryFilterOption();
            List<Long> indexList = new ArrayList<Long>();
            indexList.add(0l);
            for(int i = 0, size = filterOptionList.size(); i < size; i++) {
                long id = filterOptionList.get(i).getId();
                indexList.add(id);
            }
            
            // 每个分类下面添加其他
            String otherText = getString(R.string.poi_ohter_error);
            
            Filter categoryFitler = DataQuery.makeFilterResponse(mThis, indexList, filterCategory.getVersion(), filterOptionList, FilterCategoryOrder.FIELD_LIST_CATEGORY, false);
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
        }
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
        
        mImageView = findViewById(R.id.image_view);
        mImageImv = (ImageView) findViewById(R.id.image_imv);
        mCancelBtn = (Button) findViewById(R.id.cancel_btn);
        mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
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
        mCancelBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
        mCityBtn.setOnClickListener(this);
        mDateBtn.setOnClickListener(this);
        mTimeBtn.setOnClickListener(this);
        mImageView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        
        mType.setOnClickListener(this);
        
        OnTouchListener onTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    switch (v.getId()) {
                        case R.id.name_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantName);
                            break;
                            
                        case R.id.address_btn:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantAddress);
                            break;
                            
                        case R.id.telephone_edt:
                            mActionLog.addAction(mActionTag +  ActionLog.AddMerchantTelephone);
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
        mYourTelephoneEdt.setOnTouchListener(onTouchListener);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mImageView.getVisibility() == View.VISIBLE) {
                mImageView.setVisibility(View.GONE);
                backHome();
            } else if (mFilterListView.getVisibility() == View.VISIBLE) {
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
        boolean notEmpty = !TextUtils.isEmpty(mNameEdt.getEditableText().toString());
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mType.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mAddressEdt.getEditableText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !(mTelephoneEdt.getText().equals(MapEngine.getAreaCodeByCityId(Globals.getCurrentCityInfo().getId())+"-"));
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mAddressDescriptionEdt.getEditableText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mDateBtn.getEditableText().toString());
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mTimeBtn.getText().toString());
        }
        if (notEmpty == false) {
            notEmpty = (mUploadUri != null);
        }
        if (notEmpty == false) {
            notEmpty = !TextUtils.isEmpty(mYourTelephoneEdt.getEditableText().toString());
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
                if (mImageView.getVisibility() == View.VISIBLE) {
                    mImageView.setVisibility(View.GONE);
                    backHome();
                } else if (mFilterListView.getVisibility() == View.VISIBLE) {
                    mFilterListView.setVisibility(View.GONE);
                    backHome();
                } else {
                    exit();
                }
                break;
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                submit();
                break;
                
            case R.id.take_photo_btn:
                hideSoftInput();
                showTakePhotoDialog(REQUEST_CODE_PICK_PHOTO, REQUEST_CODE_CAPTURE_PHOTO);
                break;
                
            case R.id.delete_photo_btn:
                mUploadUri = null;
                mPhotoUri = null;
                mTakePhotoBtn.setScaleType(ScaleType.FIT_XY);
                mTakePhotoBtn.setImageResource(R.drawable.btn_take_photo);
                mDeletePhotoBtn.setVisibility(View.GONE);
                mPhotoTitleTxv.setText(R.string.storefront_photo);
                mPhotoDescriptionTxv.setText(R.string.add_merchant_upload_photo);
                break;
                
            case R.id.cancel_btn:
                mImageView.setVisibility(View.GONE);
                backHome();
                mUploadUri = null;
                break;
                
            case R.id.confirm_btn:
                mImageView.setVisibility(View.GONE);
                backHome();
                confrimUploadUri(mImageImv.getDrawable());
                break;
                
            case R.id.city_btn:
                hideSoftInput();
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                intent.setClass(mThis, ChangeCityActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CHANGE_CITY);
                break;
                
            case R.id.date_btn:
                hideSoftInput();
                if (mWeekDays == null) {
                    mWeekDays = mThis.getResources().getStringArray(R.array.week_days);
                    String sunday = mWeekDays[0];
                    int length = mWeekDays.length;
                    for(int i = 1; i < length; i++) {
                        mWeekDays[i-1] = mWeekDays[i];
                    }
                    mWeekDays[length-1] = sunday;
                    mDateChecked = new boolean[mWeekDays.length];
                    for(int i = 0; i < length; i++) {
                        mDateChecked[i] = false;
                    }
                }
                String date = mDateBtn.getText().toString();
                if (!TextUtils.isEmpty(date)) {
                    int i = 0;
                    for(String str : mWeekDays) {
                        if (date.contains(str)) {
                            mDateChecked[i] = true;
                        }
                        i++;
                    }
                }
                
                
                if (mPickDateView == null) {
                    mPickDateArrayAdapter = new MultichoiceArrayAdapter(mThis, mWeekDays, mDateChecked);
                    mPickDateView = Utility.makeListView(mThis);
                    mPickDateView.setAdapter(mPickDateArrayAdapter);
                    mPickDateView.setOnItemClickListener(new OnItemClickListener() {
                        
                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                            mDateChecked[position] = !mDateChecked[position];
                            mPickDateArrayAdapter.notifyDataSetChanged();
                        }
                    });
                    
                    mPickDateDialog = Utility.showNormalDialog(mThis, 
                            mThis.getString(R.string.comment_food_restair), 
                            mPickDateView,
                            new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int id) {
                            
                            if (id == DialogInterface.BUTTON_POSITIVE) {

                                int checked = 0;
                                for(int i = 0, length = mDateChecked.length; i < length; i++) {
                                    checked = (checked << 1) + (mDateChecked[i] ? 1 : 0);
                                }
                                if (checked == 0) {
                                    mDateBtn.setText(R.string.select_business_hours_day);
                                } else if (checked == 127) {
                                    mDateBtn.setText(R.string.every_day);
                                } else if (checked == 124) {
                                    mDateBtn.setText(R.string.working_days);
                                } else if (checked == 3) {
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
                            }
                        }
                    });
                } else {
                    mPickDateDialog.show();
                }
                break;
                
            case R.id.time_btn:
                hideSoftInput();
                mPickTimeDialog.show();
                break;
                
            case R.id.type_btn:
                hideSoftInput();
                mTitleBtn.setText(R.string.merchant_type);
                mRightBtn.setVisibility(View.GONE);
                mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
                mFilterListView.setVisibility(View.VISIBLE);
                break;
                
            default:
                
        }
    }
    
    void submit() {
        StringBuilder s = new StringBuilder();
        try {
            char splitChar = '~';
            String nullStr = "null";
            
            String name = mNameEdt.getText().toString();
            
            if (!TextUtils.isEmpty(name)) {
                s.append(URLEncoder.encode(name, TKConfig.getEncoding()));
            } else {
                mNameEdt.requestFocus();
                mAddMerchantScv.scrollTo(0, 0);
                Toast.makeText(mThis, mThis.getString(R.string.please_input)+mThis.getString(R.string.merchant_name)+"!", Toast.LENGTH_SHORT).show();
                showSoftInput();
                return;
            }
            
            String type = mType.getText().toString();
            if (!TextUtils.isEmpty(type)) {
                s.append(splitChar);
                s.append(URLEncoder.encode(type.toString(), TKConfig.getEncoding()));
            } else {
                mType.requestFocus();
                Toast.makeText(mThis, mThis.getString(R.string.please_select)+mThis.getString(R.string.merchant_type)+"!", Toast.LENGTH_SHORT).show();
                hideSoftInput();
                return;
            }

            String address = mAddressEdt.getText().toString();
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
            String str = mTelephoneEdt.getText().toString();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mAddressDescriptionEdt.getText().toString();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mDateBtn.getText().toString();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mTimeBtn.getText().toString();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            if (mUploadUri != null) {
                s.append(URLEncoder.encode(mUploadUri.toString(), TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
            }
            
            s.append(splitChar);
            str = mYourTelephoneEdt.getEditableText().toString();
            if (!TextUtils.isEmpty(str)) {
                s.append(URLEncoder.encode(str, TKConfig.getEncoding()));
            } else {
                s.append(nullStr);
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
    }
    
    void confrimUploadUri(Drawable drawable) {
        if (mPhotoUri != null) {
            mUploadUri = mPhotoUri;
            mTakePhotoBtn.setScaleType(ScaleType.MATRIX);
            mTakePhotoBtn.setImageMatrix(Utility.resizeSqareMatrix(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Util.dip2px(Globals.g_metrics.density, 112)));
            mTakePhotoBtn.setImageDrawable(drawable);
            mDeletePhotoBtn.setVisibility(View.VISIBLE);
            mPhotoTitleTxv.setText(R.string.add_merchant_upload_photo_added);
            mPhotoDescriptionTxv.setText(R.string.wait_submit);
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

    public void showTakePhotoDialog(int pickRequestCode, int captureRequestCode) {
        File file = new File(mCameraFilePath);
        file.delete();
        mCaptureUri = Uri.fromFile(file);
        Utility.showTakePhotoDialog(this, pickRequestCode,
                captureRequestCode, mCaptureUri);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_PHOTO) {
                Uri uri = data.getData();
                setPhoto(uri, true);
            } else if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
                setPhoto(mCaptureUri, false);
            } else if (requestCode == REQUEST_CODE_CHANGE_CITY) {
                if (data != null && RESULT_OK == resultCode) {
                    CityInfo cityInfo = data.getParcelableExtra(ChangeCityActivity.EXTRA_CITYINFO);
                    mCityBtn.setText(cityInfo.getCName());

                    String areaCode = MapEngine.getAreaCodeByCityId(cityInfo.getId())+"-";
                    String lastAreaCode = mTelephoneEdt.getEditableText().toString();
                    if (lastAreaCode.equals(mLastAreaCode)) {
                        mTelephoneEdt.setText(areaCode);
                        mTelephoneEdt.setSelection(areaCode.length());
                        mLastAreaCode = areaCode;
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public void setPhoto(final Uri uri, final boolean isPick) {
        if (uri == null) {
            mPhotoUri = null;
            return;
        }
        View custom = getLayoutInflater().inflate(R.layout.loading, null);
        TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
        loadingTxv.setText(R.string.doing_and_wait);
        final Dialog tipProgressDialog = Utility.showNormalDialog(this, custom);
        tipProgressDialog.setCancelable(false);
        tipProgressDialog.setCanceledOnTouchOutside(false);
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                Utility.copyFile(Utility.imageUri2FilePath(mThis, uri), mCacheFilePath);
                final File cacheFile = new File(mCacheFilePath);
                Uri cache = Uri.fromFile(cacheFile);
                Bitmap bm = Utility.getBitmapByUri(mThis, cache, 800, 800);
                try {
                    String path = Utility.imageUri2FilePath(mThis, cache);
                    ExifInterface exifInterface = new ExifInterface(path);
                    int tag = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                    float degrees = 0;
                    if (tag == ExifInterface.ORIENTATION_ROTATE_90) {
                        degrees = 90;
                    } else if (tag == ExifInterface.ORIENTATION_ROTATE_180) {
                        degrees = 180;
                    } else if (tag == ExifInterface.ORIENTATION_ROTATE_270) {
                        degrees = 270;  
                    }
                    if (degrees != 0) {
                        Matrix matrix = new Matrix();
                        int w = bm.getWidth();
                        int h = bm.getHeight();
                        matrix.preRotate(degrees, w/2, h/2);
                        Bitmap newBitmap = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
                        bm.recycle();
                        bm = newBitmap;
                    }
                    
                    Utility.bitmapToFile(bm, cacheFile);
                    
                    final Bitmap resultBitmap = bm;
                    mThis.runOnUiThread(new Runnable() {
                        
                        @Override
                        public void run() {
                            if (resultBitmap != null && Utility.bitmapToFile(resultBitmap, cacheFile)) {
                                mPhotoUri = Uri.fromFile(cacheFile);
                                if (isPick) {
                                    confrimUploadUri(new BitmapDrawable(resultBitmap));
                                } else {
                                    mTitleBtn.setText(R.string.storefront_photo);
                                    mRightBtn.setVisibility(View.GONE);
                                    mImageView.setVisibility(View.VISIBLE);
                                    mImageImv.setScaleType(ScaleType.MATRIX);
                                    mImageImv.setImageMatrix(Utility.resizeMaxWidthMatrix(resultBitmap.getWidth(), resultBitmap.getHeight(), Globals.g_metrics.widthPixels));
                                    mImageImv.setImageBitmap(resultBitmap);
                                }
                            } else {
                                mPhotoUri = null;
                            }
                            if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
                                tipProgressDialog.dismiss();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
//        UploadImageUtils.revitionPostImageSize(Utility.imageUri2FilePath(this, uri));
        
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
}

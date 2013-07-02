package com.tigerknows.ui.more;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;

import android.R.drawable;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.MultichoiceArrayAdapter;
import com.tigerknows.widget.StringArrayAdapter;
import com.tigerknows.widget.TimeListView;

public class AddMerchantActivity extends BaseActivity implements View.OnClickListener {
    
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
    private Button mShanghuleixingBtn;
    private EditText mShanghumingchengEdt = null;
    private Button mCityBtn;
    private EditText mShanghudizhiEdt = null;
    private EditText mShanghudianhuaEdt = null;
    private String mLastAreaCode;
    
    private Button mDateBtn;
    private ListView mPickDateView;
    private MultichoiceArrayAdapter mPickDateArrayAdapter;
    private Dialog mPickDateDialog;
    
    private Button mTimeBtn;
    private View mPickTimeView;
    private TimeListView mStartTimeListView;
    private TimeListView mEndTimeListView;
    private Dialog mPickTimeDialog;
    
    private EditText mNingdedianhuaEdt = null;
    
    private String[] mWeekDays;
    
    private boolean[] mDateChecked;
    
    private View mImageView;
    
    private ImageView mImageImv;
    
    private Button mCancelBtn;
    
    private Button mConfirmBtn;
    
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
        
        mCacheFilePath = Environment.getExternalStorageDirectory()+ "/Android/data/" + getPackageName() + "/files"+"/cache.jpg";
        mCameraFilePath = Environment.getExternalStorageDirectory()+ "/Android/data/" + getPackageName() + "/files"+"/camera.jpg";
        
        CityInfo cityInfo = Globals.getCurrentCityInfo();
        
        mCityBtn.setText(cityInfo.getCName());
        mLastAreaCode = MapEngine.getAreaCodeByCityId(cityInfo.getId())+"-";
        mShanghudianhuaEdt.setText(mLastAreaCode);
        
        Intent intent = getIntent();
        if (intent != null) {
            String intputText = intent.getStringExtra(EXTRA_INPUT_TEXT);
            if (intputText != null) {
                mShanghumingchengEdt.setText(intputText);
                mShanghumingchengEdt.setSelection(intputText.length());
            }
        }
        
        mImageView.setVisibility(View.GONE);
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();
        mAddMerchantScv = (ScrollView)findViewById(R.id.add_merchant_scv);
        mShanghumingchengEdt = (EditText)findViewById(R.id.shanghumingcheng_edt);
        mShanghuleixingBtn = (Button)findViewById(R.id.shanghuleixing_btn);
        mCityBtn = (Button)findViewById(R.id.city_btn);
        mShanghudizhiEdt = (EditText)findViewById(R.id.shanghudizhi_edt);
        mShanghudianhuaEdt = (EditText)findViewById(R.id.shanghudianhua_edt);
        mDateBtn = (Button)findViewById(R.id.date_btn);
        mTimeBtn = (Button)findViewById(R.id.time_btn);
        mNingdedianhuaEdt = (EditText)findViewById(R.id.ningdedianhua_edt);
        mTakePhotoBtn = (ImageButton) findViewById(R.id.take_photo_btn);
        
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
        mNingdedianhuaEdt.setOnTouchListener(onTouchListener);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mImageView.getVisibility() == View.VISIBLE) {
                mImageView.setVisibility(View.GONE);
            } else {
                exit();
            }
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
            notEmpty = !TextUtils.isEmpty(mTimeBtn.getText().toString());
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
                    
                    str = mTimeBtn.getText().toString().trim();
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
                
            case R.id.take_photo_btn:
                showTakePhotoDialog(REQUEST_CODE_PICK_PHOTO, REQUEST_CODE_CAPTURE_PHOTO);
                break;
                
            case R.id.cancel_btn:
                mImageView.setVisibility(View.GONE);
                mUploadUri = null;
                break;
                
            case R.id.confirm_btn:
                mImageView.setVisibility(View.GONE);
                if (mPhotoUri != null) {
                    mUploadUri = mPhotoUri;
                    Drawable drawable = mImageImv.getDrawable();
                    mTakePhotoBtn.setScaleType(ScaleType.MATRIX);
                    mTakePhotoBtn.setImageMatrix(Utility.resizeSqareMatrix(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Util.dip2px(Globals.g_metrics.density, 152)));
                    mTakePhotoBtn.setImageDrawable(drawable);
                }
                break;
                
            case R.id.city_btn:
                Intent intent = new Intent();
                intent.putExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, true);
                intent.setClass(mThis, ChangeCityActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CHANGE_CITY);
                break;
                
            case R.id.date_btn:
                LogWrapper.d("111111", "date_btn");
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
                for(int i = 0, length = mWeekDays.length; i < length; i++) {
                    mDateChecked[i] = false;
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
                    });
                } else {
                    mPickDateDialog.show();
                }
                break;
                
            case R.id.time_btn:
                LogWrapper.d("111111", "time_btn");
                if (mPickTimeView == null) {
                    View pickTimeView = mLayoutInflater.inflate(R.layout.more_add_merchant_pick_time, null, false);
                    mPickTimeView = pickTimeView;
                    mStartTimeListView = (TimeListView) mPickTimeView.findViewById(R.id.start_tlv);
                    mEndTimeListView = (TimeListView) mPickTimeView.findViewById(R.id.end_tlv);
                    
                    mStartTimeListView.setData(9, 6);
                    mEndTimeListView.setData(21, 6);
                    
                    mPickTimeDialog = Utility.showNormalDialog(mThis, 
                            mThis.getString(R.string.comment_food_restair), 
                            mPickTimeView,
                            new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int id) {
                            
                            
                        }
                    });
                } else {
                    mPickTimeDialog.show();
                }
                
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
                setPhoto(uri);
            } else if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
                setPhoto(mCaptureUri);
            } else if (requestCode == REQUEST_CODE_CHANGE_CITY) {
                if (data != null && RESULT_OK == resultCode) {
                    CityInfo cityInfo = data.getParcelableExtra(ChangeCityActivity.EXTRA_CITYINFO);
                    mCityBtn.setText(cityInfo.getCName());

                    String areaCode = MapEngine.getAreaCodeByCityId(cityInfo.getId())+"-";
                    String lastAreaCode = mShanghudianhuaEdt.getEditableText().toString();
                    if (lastAreaCode.equals(mLastAreaCode)) {
                        mShanghudianhuaEdt.setText(areaCode);
                        mLastAreaCode = areaCode;
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public void setPhoto(final Uri uri) {
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
                                mImageView.setVisibility(View.VISIBLE);
                                mImageImv.setScaleType(ScaleType.MATRIX);
                                mImageImv.setImageMatrix(Utility.resizeMaxWidthMatrix(resultBitmap.getWidth(), resultBitmap.getHeight(), Globals.g_metrics.widthPixels));
                                mImageImv.setImageBitmap(resultBitmap);
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
}

package com.tigerknows.ui.common;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.tigerknows.R;
import com.tigerknows.TKConfig;

import com.decarta.Globals;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.FileUpload;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

public class AddPictureActivity extends BaseActivity implements View.OnClickListener {
    
    public static final int REQUEST_CODE_PICK_PHOTO = 0;

    public static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
    
    private String mCacheFilePath;
    
    private String mCameraFilePath;
    
    private Uri mCaptureUri;
    private Uri mPhotoUri;
    private Uri mUploadUri;
    private String mPhotoMD5;
    public boolean mSelected = false;
    
    private View mRootView;
    
    private ImageView mImageImv;
    
    private Button mCancelBtn;
    
    private Button mConfirmBtn;
    
    private String mRefId;
    
    private String mRefDty;
    
	static final String TAG = "AddPictureActivity";
	
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mId = R.id.activity_common_add_picture;
        mActionTag = ActionLog.AddPicture;
        setContentView(R.layout.common_add_picture);
        
        findViews();
        setListener();
        
        Intent intent = getIntent();
        mRefId = intent.getStringExtra(FileUpload.SERVER_PARAMETER_REF_ID);
        mRefDty = intent.getStringExtra(FileUpload.SERVER_PARAMETER_REF_DATA_TYPE);
        
        if (mRefDty == null) {
            finish();
        }

        mTitleBtn.setText(R.string.add_picture);
        mRootView.setVisibility(View.INVISIBLE);
        
        mCacheFilePath = TKConfig.getDataPath(true) + "cache.jpg";
        mCameraFilePath = TKConfig.getDataPath(true) + "camera.jpg";
        
        showTakePhotoDialog();
    }
    
    /**
     * Find all the views from XML files
     */
    protected void findViews() {
        super.findViews();        
        mRootView = findViewById(R.id.root_view);
        mImageImv = (ImageView) findViewById(R.id.image_imv);
        mCancelBtn = (Button) findViewById(R.id.cancel_btn);
        mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
    }

    /**
     * Set the listeners for the commit and cancel button
     */
    protected void setListener() {
        super.setListener();
        mCancelBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){                
            case R.id.cancel_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddPictureCancel);
                showTakePhotoDialog();
                break;
                
            case R.id.confirm_btn:
                mActionLog.addAction(mActionTag +  ActionLog.AddPictureConfirm);
                submit();
                break;
                
            default:
                break;
        }
    }
    
    void submit() {
        if (mUploadUri != null && mPhotoMD5 != null && mRefDty != null) {
            String filePath = Utility.imageUri2FilePath(mThis, mUploadUri);
            FileUpload fileUpload = new FileUpload(mThis);
            fileUpload.addParameter(FileUpload.SERVER_PARAMETER_REF_DATA_TYPE, mRefDty);
            if (mRefId != null) {
                fileUpload.addParameter(FileUpload.SERVER_PARAMETER_REF_ID, mRefId);
            }
            fileUpload.addParameter(FileUpload.SERVER_PARAMETER_FILE_TYPE, FileUpload.FILE_TYPE_IMAGE);
            fileUpload.addParameter(FileUpload.SERVER_PARAMETER_CHECKSUM, mPhotoMD5);
            fileUpload.addParameter(FileUpload.SERVER_PARAMETER_FILENAME, mPhotoMD5+filePath.substring(filePath.lastIndexOf(".")));
            fileUpload.addParameter(FileUpload.SERVER_PARAMETER_UPFILE, filePath);
            fileUpload.setup(Globals.getCurrentCityInfo().getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
            queryStart(fileUpload);
        }
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
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, this, false)) {
            return;
        }
        
        Toast.makeText(mThis, R.string.add_picture_success, Toast.LENGTH_LONG).show();
        Intent data = new Intent();
        data.setData(mUploadUri);
        setResult(RESULT_OK, data);
        finish();
    }

    void showTakePhotoDialog() {
        File file = new File(mCameraFilePath);
        file.delete();
        mCaptureUri = Uri.fromFile(file);
        Dialog dialog = Utility.showTakePhotoDialog(mActionTag, this, REQUEST_CODE_PICK_PHOTO,
                REQUEST_CODE_CAPTURE_PHOTO, mCaptureUri);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mRootView.getVisibility() != View.VISIBLE && mSelected == false) {
                    finish();
                }
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CODE_PICK_PHOTO) {
                Uri uri = data.getData();
                setPhoto(uri);
            } else if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
                setPhoto(mCaptureUri);
            } else {
                finish();
            }
        } else {
            finish();
        }
    }
    
    public void setPhoto(final Uri uri) {
        mPhotoUri = null;
        if (uri == null) {
            return;
        }
        
        mRootView.setVisibility(View.VISIBLE);
        View custom = getLayoutInflater().inflate(R.layout.loading, null);
        TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
        loadingTxv.setText(R.string.doing_and_wait);
        final Dialog tipProgressDialog = Utility.showNormalDialog(this, custom);
        tipProgressDialog.setCancelable(false);
        tipProgressDialog.setCanceledOnTouchOutside(false);
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    final String imagePath = URLDecoder.decode(Utility.imageUri2FilePath(mThis, uri));
                    if (Utility.copyFile(imagePath, mCacheFilePath)) {
                        final File cacheFile = new File(mCacheFilePath);
                        Uri cache = Uri.fromFile(cacheFile);
                        Bitmap bm = Utility.getBitmapByUri(mThis, cache, TKConfig.Photo_Max_Width_Height, TKConfig.Photo_Max_Width_Height);
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

                        if (resultBitmap != null && Utility.bitmapToFile(resultBitmap, cacheFile)) {
                            mPhotoUri = Uri.fromFile(cacheFile);
                        }
                        
                        mThis.runOnUiThread(new Runnable() {
                            
                            @Override
                            public void run() {
                                if (mPhotoUri != null) {
                                    
                                    mUploadUri = mPhotoUri;
                                    String fileName = Utility.imageUri2FilePath(mThis, mUploadUri);
                                    mPhotoMD5 = Utility.md5sum(fileName);
                                    
                                    mImageImv.setScaleType(ScaleType.CENTER_INSIDE);
                                    mImageImv.setImageBitmap(resultBitmap);
                                }
                                if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
                                    tipProgressDialog.dismiss();
                                }
                            }
                        });
                    }
                
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mThis.runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (mPhotoUri == null) {
                            Toast.makeText(mThis, getString(R.string.get_photo_failed)+uri, Toast.LENGTH_LONG).show();
                        }
                        if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
                            tipProgressDialog.dismiss();
                        }
                    }
                });
            }
        }).start();        
    }
}

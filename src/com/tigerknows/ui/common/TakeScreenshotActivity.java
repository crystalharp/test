/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.common;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Peng Wenyue
 */
public class TakeScreenshotActivity extends BaseActivity implements View.OnClickListener {
    
    static final String TAG = "TakeScreenshotActivity";
    
    private ImageView mImageImv;
    private Uri mUri;
    private Button mShareBtn;
    private LayoutParams mShareLayoutParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TakeScreenshot;
        
        setContentView(R.layout.take_screenshot);
        
        findViews();
        setListener();
        
        mShareBtn = new Button(mThis);
        mShareBtn.setText(R.string.share);
        mShareBtn.setBackgroundResource(R.drawable.btn_confirm);
        mShareBtn.setOnClickListener(this);
        mShareBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mShareLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mShareLayoutParams.leftMargin = Utility.dip2px(mThis, 12);
        
        mShareBtn.setTextColor(mRightBtn.getTextColors());
        mShareBtn.setPadding(mRightBtn.getPaddingLeft(),
                mRightBtn.getPaddingTop(),
                mRightBtn.getPaddingRight(),
                mRightBtn.getPaddingBottom());
        ViewGroup mRightView = (ViewGroup) mRightBtn.getParent();
        mRightView.addView(mShareBtn, mShareLayoutParams);
        
        mUri = getIntent().getData();
        if (mUri == null) {
            finish();
        }
        
        Toast.makeText(mThis, R.string.has_take_screenshot, Toast.LENGTH_LONG).show();

        mTitleBtn.setText(R.string.take_screenshot);
        
        mRightBtn.setText(R.string.save);
        mRightBtn.setOnClickListener(this);
        
        Bitmap bm = Utility.imageUri2Bitmap(mThis, mUri);
        mImageImv.setImageBitmap(bm);
    }
    
    protected void findViews() {
        super.findViews();
        mImageImv = (ImageView) findViewById(R.id.image_imv);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
        case R.id.right_btn:
            mActionLog.addAction(mActionTag + ActionLog.TakeScreenshotSave);
            // 检查扩展存储卡
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_MOUNTED)) {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                String path = externalStorageDirectory.getAbsolutePath() + "/DCIM/Screenshots/";
                File file = new File(path);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        LogWrapper.e(TAG, "getSavePath() Unable to create new folder: " + path);
                        path = null;
                    }
                }
                
                if (path != null) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                    String filePath = path+simpleDateFormat.format(Calendar.getInstance().getTime())+".png";
                    Utility.copyFile(new File(Utility.imageUri2FilePath(mThis, mUri)), new File(filePath), true);
                    Toast.makeText(mThis, getString(R.string.take_screenshot_save_to_, filePath), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Toast.makeText(mThis, R.string.save_falied_tip, Toast.LENGTH_LONG).show();
        	break;

    	default:
            mActionLog.addAction(mActionTag + ActionLog.TakeScreenshotShare);
            Utility.share(mThis, getString(R.string.share), null, mUri);
    		break;
        }

    }
}

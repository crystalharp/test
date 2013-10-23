/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.util.Utility;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Peng Wenyue
 */
public class TakeScreenshotActivity extends BaseActivity implements View.OnClickListener {
    
    private ImageView mImageImv;
    private Uri mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TakeScreenshot;
        
        setContentView(R.layout.take_screenshot);
        
        findViews();
        setListener();
        
        mUri = getIntent().getData();
        if (mUri == null) {
            finish();
        }
        
        Toast.makeText(mThis, R.string.has_take_screenshot, Toast.LENGTH_LONG).show();

        mTitleBtn.setText(R.string.take_screenshot);
        
        mRight2Btn.setText(R.string.share);
        mRight2Btn.setVisibility(View.VISIBLE);
        mRight2Btn.setOnClickListener(this);
        mRight2Btn.setBackgroundResource(R.drawable.btn_title);
        mRightBtn.setText(R.string.save);
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        mRightBtn.setBackgroundResource(R.drawable.btn_title);
        
        mImageImv.setImageURI(mUri);
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
                    Utility.copyFile(new File(TKConfig.getDataPath(true)+Utility.imageUri2FilePath(mThis, mUri)), new File(filePath), true);
                    Toast.makeText(mThis, getString(R.string.take_screenshot_save_to_, filePath), Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Toast.makeText(mThis, R.string.save_falied_tip, Toast.LENGTH_LONG).show();
        	break;

        case R.id.right2_btn:
            mActionLog.addAction(mActionTag + ActionLog.TakeScreenshotShare);
            Utility.share(mThis, getString(R.string.share), null, mUri);
            break;
            
    	default:
    		break;
        }

    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.Globals;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Peng Wenyue
 */
public class AboutUs extends BaseActivity implements View.OnClickListener {
    
    private TextView mAppVersionTxv;
    private TextView mReleaseDateTxv;
    private TextView mQuanguoSuggestVersionTxv;
    private TextView mCitySuggestVersionTxv;
//    private ImageView mQuanguoSuggestVersionImv;
//    private ImageView mCitySuggestVersionImv;

    private TextView mWebTxv;
    private TextView mWeiboTxv;
    private TextView mBBSTxv;
//    private TextView mWapTxv;
//    private TextView mTelephoneTxv;
    private Button mPhoneButton;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.AboutUs;

        setContentView(R.layout.about_us);
        findViews();
        setListener();
        
        mTitleBtn.setText(R.string.about_us);
        mRightBtn.setVisibility(View.GONE);
        
        mAppVersionTxv.setText(mThis.getString(R.string.about_version, TKConfig.CLIENT_SOFT_VERSION));
//        mAppVersionTxv.setShadowLayer(2.0f, 1.0f, -1.0f, 0xffffffff);
        mReleaseDateTxv.setText(mThis.getString(R.string.about_release_date, TKConfig.CLIENT_SOFT_RELEASE_DATE));
        
        int swNationVersion = MapEngine.getInstance().getrevision(MapEngine.SW_ID_QUANGUO);
        if (swNationVersion > 0) {
            mQuanguoSuggestVersionTxv.setVisibility(View.VISIBLE);
//            mQuanguoSuggestVersionImv.setVisibility(View.VISIBLE);
            mQuanguoSuggestVersionTxv.setText(mThis.getString(R.string.about_nation_sw_version, Integer.toString(swNationVersion)));
        } else {
            mQuanguoSuggestVersionTxv.setVisibility(View.GONE);
//            mQuanguoSuggestVersionImv.setVisibility(View.GONE);
        }

        int cityId = Globals.g_Current_City_Info.getId();
        int swCurrentCityVersion = 0;
        //当前城市为全国时返回0，调用引擎会出错
        if (cityId != 0) {
            swCurrentCityVersion = MapEngine.getInstance().getrevision(cityId);
        }
        if (swCurrentCityVersion > 0) {
            mCitySuggestVersionTxv.setVisibility(View.VISIBLE);
//            mCitySuggestVersionImv.setVisibility(View.VISIBLE);
            mCitySuggestVersionTxv.setText(mThis.getString(R.string.about_current_city_sw_version, Integer.toString(swCurrentCityVersion)));
        } else {
            mCitySuggestVersionTxv.setVisibility(View.GONE);
//            mCitySuggestVersionImv.setVisibility(View.GONE);
        }
    }

    protected void findViews() {
        super.findViews();
        mAppVersionTxv = (TextView) findViewById(R.id.app_version_txv);
        mReleaseDateTxv = (TextView) findViewById(R.id.release_date_txv);
        mQuanguoSuggestVersionTxv = (TextView) findViewById(R.id.quanguo_suggest_version_txv);
        mCitySuggestVersionTxv = (TextView) findViewById(R.id.city_suggest_version_txv);
//        mQuanguoSuggestVersionImv = (ImageView) findViewById(R.id.quanguo_suggest_version_imv);
//        mCitySuggestVersionImv = (ImageView) findViewById(R.id.city_suggest_version_imv);
        mWebTxv = (TextView) findViewById(R.id.web_txv);
        mWeiboTxv = (TextView) findViewById(R.id.weibo_txv);
        mBBSTxv = (TextView) findViewById(R.id.bbs_txv);
//        mWapTxv = (TextView) findViewById(R.id.wap_txv);
        mPhoneButton = (Button) findViewById(R.id.telephone_btn);
    }
    
    protected void setListener() {
        super.setListener();
        mWebTxv.setOnClickListener(this);
        mWeiboTxv.setOnClickListener(this);
        mBBSTxv.setOnClickListener(this);
//        mWapTxv.setOnClickListener(this);
        mPhoneButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.web_txv:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "web");
                break;
            case R.id.weibo_txv:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "weibo");
                break;
            case R.id.bbs_txv:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "bbs");
                break;
//            case R.id.wap_txv:
//                mActionLog.addAction(ActionLog.AboutUsWap);
//                break;
            case R.id.telephone_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "telephone");
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + getString(R.string.telephone_num)));
                startActivity(intent);
                break;

            default:
                break;
        }
    }
}

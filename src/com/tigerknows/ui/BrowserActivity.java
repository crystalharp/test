/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import java.net.URLDecoder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alipay.android.MobileSecurePayer;
import com.alipay.android.MobileSecurePayHelper;
import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;

import android.widget.Toast;

import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataOperation.DingdanCreateResponse;
import com.tigerknows.model.Response;
import com.tigerknows.model.Tuangou;
import com.tigerknows.ui.discover.TuangouDetailView;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.ui.user.UserLoginRegistActivity;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 */
public class BrowserActivity extends BaseActivity implements View.OnClickListener {
    
    static final String TAG = "BrowserActivity";
    
    public static final String URL = "url";

    public static final String TITLE = "title";

    public static final String LEFT = "left";

    public static final String TIP = "tip";
    
    class MyHandler {
        public void show(String data) {
            Utility.showNormalDialog(BrowserActivity.this, data);
        }
    }
 
    private WebView mWebWbv = null;
    private ProgressBar mProgressBar;
    private Button mBackBtn;
    private Button mForwardtn;
    private Button mRefreshBtn;
    private Button mStopBtn;
    private String mURL;
    
    private View mButtonView = null;
    
    /**
     * The bar view embeded in the details
     */
    private View mBarView = null;
    
    /**
     * The price textView
     */
    private TextView mPriceTxv;
    
    /**
     * The original price TextView
     */
    private TextView mOrgPriceTxv;
    
    /**
     * The discount textView
     */
    private TextView mDiscountTxv;

    /**
     * Buy buton
     */
    private Button mBuyBtn = null;
    
    private static Tuangou sTuangou;
    
    private String mFinishedUrl;
    private boolean mReload = false;
    
    private static Activity mActivity;
    private static String mPayInfo;
    private static String mAlipayActionLog;
    
    /**
     * 检测url，满足特殊条件时调用支付宝快捷支付
     * @param activity
     * @param url
     */
    public static void checkFastAlipay(Activity activity, String url, ActionLog actionLog, String actionTag) {
        String info = URLDecoder.decode(url);
        LogWrapper.d("Trap", info);
        String clientGoAlipay = TKConfig.getPref(activity, TKConfig.PREFS_CLIENT_GO_ALIPAY, "on");
        if(! "on".equalsIgnoreCase(clientGoAlipay)){
        	return;
        }
        if(info.contains("wappaygw.alipay") && info.contains("authAndExecute")){
            int c = "<request_token>".length();
            int i = info.indexOf("<request_token>");
            int j = info.indexOf("</request_token>");
            StringBuilder sb = new StringBuilder();
            sb.append("ordertoken=\"");
            if(i >= 0 && i+c <= j){
                sb.append(info.substring(i+c, j));
            }else return;
            sb.append("\"");
            MobileSecurePayHelper mspHelper = new MobileSecurePayHelper(activity);
            mAlipayActionLog = actionTag + ActionLog.BrowserReadyAlipay;
            if (!mspHelper.isMobile_spExist()) {
                return;
            }
            if(mActivity != null){
            	synchronized (mActivity) {
            		mActivity = activity;
            	}
            }else{
            	mActivity = activity;
            }
            if(!TextUtils.isEmpty(mPayInfo)){
            	synchronized (mPayInfo) {
            		mPayInfo = sb.toString();
            	}
            }else{
            	mPayInfo = sb.toString();
            }
        }else if (info.contains("wappaygw.alipay") && (info.contains("wapcashier_confirm_login") || info.contains("wapcashier_login"))){
        	if(!TextUtils.isEmpty(mAlipayActionLog)){
        		actionLog.addAction(mAlipayActionLog);
        		mAlipayActionLog = null;
        	}
    		if(mActivity == null || TextUtils.isEmpty(mPayInfo)){
    			return;
    		}
    		synchronized (mPayInfo) {
    			synchronized (mActivity) {
    				actionLog.addAction(actionTag + ActionLog.BrowserLaunchAlipay);
    				new MobileSecurePayer().pay(mPayInfo, null, 1, mActivity);
    	        }
			}
    		mPayInfo = null;
    		mActivity = null;
        }
    }
   
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.Browser;
        mId = R.id.activity_browser;

        if (mIntent == null) {
            finish();
        }
        
        mURL = mIntent.getStringExtra(URL);
        if (TextUtils.isEmpty(mURL)) {
            finish();
        }
        LogWrapper.d(TAG, "mURL="+mURL);

        setContentView(R.layout.browser);
        
        findViews();
        setListener();

        String title = mIntent.getStringExtra(TITLE);
        if (TextUtils.isEmpty(title)) {
            mTitleBtn.setText(mThis.getString(R.string.buy));
        } else {
            mTitleBtn.setText(title);
        }
        @SuppressWarnings("unused")
		String left = mIntent.getStringExtra(LEFT);
        if (TextUtils.isEmpty(title) == false) {
            //mLeftBtn.setText(left);
        }
        mRightBtn.setVisibility(View.GONE);
        
        String tip = mIntent.getStringExtra(TIP);
        if (TextUtils.isEmpty(tip) == false) {
            Toast.makeText(mThis, mThis.getString(R.string.going_to_, tip), Toast.LENGTH_LONG).show();
        }

        Tuangou tuangou = sTuangou;
        if (tuangou == null) {
            mBarView.setVisibility(View.GONE);
            mButtonView.setVisibility(View.VISIBLE);
        } else {
            mBarView.setVisibility(View.VISIBLE);
            mButtonView.setVisibility(View.GONE);
            
            mPriceTxv.setText(tuangou.getPrice()+getString(R.string.rmb_text));
            mOrgPriceTxv.setText(tuangou.getOrgPrice()+getString(R.string.rmb_text));
            mOrgPriceTxv.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            mDiscountTxv.setText(tuangou.getDiscount());
        }
        
        mBackBtn.setEnabled(false);
        mForwardtn.setEnabled(false);
        mWebWbv.loadUrl(mURL);
        mWebWbv.getSettings().setJavaScriptEnabled(true);
        mWebWbv.setScrollBarStyle(0);
        mWebWbv.addJavascriptInterface(new MyHandler(), "handler");
        mWebWbv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Tuangou tuangou = sTuangou;
                if (tuangou != null && mTitleBtn.getText().toString().equals(mThis.getString(R.string.tuanguo_picture_text_detail))) {
                    return true;
                } else {
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 结束
                super.onPageFinished(view, url);
                if ((mFinishedUrl == null || mFinishedUrl.equals(url)) && mReload == false) {
                    view.clearHistory();
                }
                mReload = false;
                mFinishedUrl = url;
                mProgressBar.setVisibility(View.GONE);
                mBackBtn.setEnabled(mWebWbv.canGoBack());
                mForwardtn.setEnabled(mWebWbv.canGoForward());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            	mProgressBar.setVisibility(View.VISIBLE);
            	checkFastAlipay(mThis, url, mActionLog, mActionTag);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // handler.cancel(); //默认的处理方式，WebView变成空白页
                handler.proceed();
                // handleMessage(Message mug); //其他处理
            }
        });
        mWebWbv.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int progress) {
                mProgressBar.setProgress(progress);
                if (progress >= 100) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });


    }

    protected void findViews() {
        super.findViews();
        mWebWbv = (WebView)findViewById(R.id.web_wbv);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_prb);
        mBackBtn = (Button)findViewById(R.id.back_btn);
        mForwardtn = (Button)findViewById(R.id.forward_btn);
        mRefreshBtn = (Button)findViewById(R.id.refresh_btn);
        mStopBtn = (Button)findViewById(R.id.stop_btn);
        mButtonView = findViewById(R.id.button_view);

        mPriceTxv = (TextView)findViewById(R.id.price_txv);
        mBarView = findViewById(R.id.bar_view);
        mBuyBtn = (Button) findViewById(R.id.buy_btn);
        mOrgPriceTxv = (TextView)findViewById(R.id.org_price_txv);
        mDiscountTxv = (TextView) findViewById(R.id.discount_txv);
    }
    
    protected void setListener() {
        super.setListener();
        mBackBtn.setOnClickListener(this);
        mForwardtn.setOnClickListener(this);
        mRefreshBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mBuyBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.back_btn) {
            mActionLog.addAction(mActionTag +  ActionLog.BrowserBack);
            if (mWebWbv.canGoBack()) {
                mWebWbv.goBack();
            }
        } else if (id == R.id.forward_btn) {
            mActionLog.addAction(mActionTag +  ActionLog.BrowserForward);
            if (mWebWbv.canGoForward()) {
                mWebWbv.goForward();
            }
        } else if (id == R.id.refresh_btn) {
            mActionLog.addAction(mActionTag +  ActionLog.BrowserRefresh);
            mReload = true;
            mWebWbv.reload();
        } else if (id == R.id.stop_btn) {
            mWebWbv.stopLoading();
        } else if (id == R.id.buy_btn) {
            mActionLog.addAction(mActionTag +  ActionLog.BrowserBuy);
            buy(true);
        }
        
    }
    
    public static void setTuangou(Tuangou tuangou) {
        sTuangou = tuangou;
    }

    public void finish() {
        sTuangou = null;
        super.finish();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mWebWbv.canGoBack() && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            mWebWbv.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    void buy(boolean login) {
        Tuangou tuangou = sTuangou;
        if (tuangou != null) {
            String sessionId = Globals.g_Session_Id;
            if (TextUtils.isEmpty(sessionId) && TextUtils.isEmpty(tuangou.getUrl())) {
                if (login) {
                    Intent intent = new Intent(mThis, UserLoginRegistActivity.class);
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, mId);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, mId);
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, mId);
                    startActivityForResult(intent, R.id.activity_user_login_regist);
                }
            } else {
                DataOperation dataOperation = TuangouDetailView.makeDingdanQuery(mThis, tuangou, false, mId, mId);
                if (dataOperation != null) {
                    queryStart(dataOperation);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        buy(false);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        Tuangou tuangou = sTuangou;
        if (tuangou == null) {
            return;
        }
        
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        if (BaseActivity.checkReLogin(dataOperation, mThis, false, mId, mId, mId, mCancelLoginListener, true)) {
            isReLogin = true;
            return;
        }

        final Response response = dataOperation.getResponse();
        String dataType = dataOperation.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
        if (BaseQuery.DATA_TYPE_DINGDAN.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mThis, null, true, this, false)) {
                return;
            }
            DingdanCreateResponse dingdanCreateResponse = (DingdanCreateResponse) response;
            
            tuangou.setDingdanCreateResponse(dingdanCreateResponse);
            
            loadDiandan(dingdanCreateResponse.getUrl());
        }
    }
    
    void loadDiandan(String url) {
        mFinishedUrl = null;
        mReload = false;
        mWebWbv.clearHistory();
        mWebWbv.loadUrl(url);
        mButtonView.setVisibility(View.VISIBLE);
        mBarView.setVisibility(View.GONE);
        mTitleBtn.setText(R.string.buy);
    }
}

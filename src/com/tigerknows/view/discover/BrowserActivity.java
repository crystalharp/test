/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import com.tigerknows.widget.Toast;

import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.util.CommonUtils;

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
            CommonUtils.showNormalDialog(BrowserActivity.this, data);
        }
    }
    
    private WebView mWebWbv = null;
    private ProgressBar mProgressBar;
    private Button mBackBtn;
    private Button mForwardtn;
    private Button mRefreshBtn;
    private Button mStopBtn;
    private String mURL;
    
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

        setContentView(R.layout.browser);
        
        findViews();
        setListener();

        String title = mIntent.getStringExtra(TITLE);
        if (TextUtils.isEmpty(title)) {
            mTitleBtn.setText(mThis.getString(R.string.buy));
        } else {
            mTitleBtn.setText(title);
        }
        String left = mIntent.getStringExtra(LEFT);
        if (TextUtils.isEmpty(title) == false) {
            //mLeftBtn.setText(left);
        }
        mRightBtn.setVisibility(View.GONE);
        
        String tip = mIntent.getStringExtra(TIP);
        if (TextUtils.isEmpty(tip) == false) {
            Toast.makeText(mThis, mThis.getString(R.string.going_to_, tip), Toast.LENGTH_LONG).show();
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
                // 在这里也如同onPageStarted这样处理，详见新浪的demo
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 结束
                super.onPageFinished(view, url);
                mProgressBar.setVisibility(View.GONE);
                mBackBtn.setEnabled(mWebWbv.canGoBack());
                mForwardtn.setEnabled(mWebWbv.canGoForward());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressBar.setVisibility(View.VISIBLE);
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
    }
    
    protected void setListener() {
        super.setListener();
        mBackBtn.setOnClickListener(this);
        mForwardtn.setOnClickListener(this);
        mRefreshBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
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
            mWebWbv.reload();
        } else if (id == R.id.stop_btn) {
            mWebWbv.stopLoading();
        }
        
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mWebWbv.canGoBack() && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            mWebWbv.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.widget.Toast;

import com.tigerknows.common.ActionLog;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 */
public class BrowserFragment extends BaseFragment implements View.OnClickListener {
    
    public BrowserFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    

    static final String TAG = "BrowserFragment";
    
    class MyHandler {
        public void show(String data) {
            Utility.showNormalDialog(mSphinx, data);
        }
    }
    
    private WebView mWebWbv = null;
    private ProgressBar mProgressBar;
    private Button mBackBtn;
    private Button mForwardtn;
    private Button mRefreshBtn;
    private Button mStopBtn;
    private String mURL;
    private String mTitle;
    private String mFinishedUrl;
    private String mLastUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.Browser;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.browser, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (TextUtils.isEmpty(mTitle)) {
            mTitleBtn.setText(mSphinx.getString(R.string.buy));
        } else {
            mTitleBtn.setText(mTitle);
        }

        mRightBtn.setVisibility(View.GONE);
        mWebWbv.requestFocusFromTouch();
    }
    
    public void setData(String title, String url, String tip){
        if (url == null || (url.equals(mURL) && url.equals(mLastUrl))) {
            return;
        }

        if (TextUtils.isEmpty(tip) == false) {
            Toast.makeText(mSphinx, mSphinx.getString(R.string.going_to_, tip), Toast.LENGTH_LONG).show();
        }
        
        mWebWbv.stopLoading();
        mWebWbv.clearView();
        mTitle = title;
        mURL = url;
        mFinishedUrl = null;
        mLastUrl = null;

        LogWrapper.d(TAG, "mURL="+mURL);
        
        mBackBtn.setEnabled(false);
        mForwardtn.setEnabled(false);
        mWebWbv.loadUrl(mURL);
    }
    
    public void stopLoading() {
        mWebWbv.stopLoading();
        mWebWbv.clearView();
        mURL = null;
    }

    protected void findViews() {
        mWebWbv = (WebView)mRootView.findViewById(R.id.web_wbv);
        mProgressBar = (ProgressBar) mRootView.findViewById(R.id.progress_prb);
        mBackBtn = (Button)mRootView.findViewById(R.id.back_btn);
        mForwardtn = (Button)mRootView.findViewById(R.id.forward_btn);
        mRefreshBtn = (Button)mRootView.findViewById(R.id.refresh_btn);
        mStopBtn = (Button)mRootView.findViewById(R.id.stop_btn);
        mRootView.findViewById(R.id.title_view).setVisibility(View.GONE);
    }
    
    protected void setListener() {
        mBackBtn.setOnClickListener(this);
        mForwardtn.setOnClickListener(this);
        mRefreshBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        
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
                if (mFinishedUrl == null || mFinishedUrl.equals(url)) {
                    view.clearHistory();
                    mLastUrl = mURL;
                } else {
                    mLastUrl = url;
                }
                mFinishedUrl = url;
                mProgressBar.setVisibility(View.GONE);
                mBackBtn.setEnabled(mWebWbv.canGoBack());
                mForwardtn.setEnabled(mWebWbv.canGoForward());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            	mProgressBar.setVisibility(View.VISIBLE);
                BrowserActivity.checkFastAlipay(mSphinx, url);
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

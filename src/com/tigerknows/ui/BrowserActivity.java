/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import java.net.URLDecoder;

import android.graphics.Bitmap;
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

import com.alipay.android.MobileSecurePayer;
import com.alipay.android.MobileSecurePayHelper;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
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
    
    private static final int ALIX_PAY = 1;
    
    class MyHandler {
        public void show(String data) {
            Utility.showNormalDialog(BrowserActivity.this, data);
        }
    }
	//
	// the handler use to receive the pay result.
	// 这里接收支付结果，支付宝手机端同步通知
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			try {
				String ret = (String) msg.obj;
				switch (msg.what) {
				case ALIX_PAY: {
					//
					// 处理交易结果
					try {
						// 获取交易状态码，具体状态代码请参看文档
						String tradeStatus = "resultStatus={";
						int imemoStart = ret.indexOf("resultStatus=");
						imemoStart += tradeStatus.length();
						int imemoEnd = ret.indexOf("};memo=");
						tradeStatus = ret.substring(imemoStart, imemoEnd);

							if (tradeStatus.equals("9000"))// 判断交易状态码，只有9000表示交易成功
								Toast.makeText(mThis, "支付成功", Toast.LENGTH_SHORT).show();
							else
								Toast.makeText(mThis, "支付失败，状态码："+tradeStatus, Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
					break;
				}

				super.handleMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};    
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
                String info = URLDecoder.decode(url);
                LogWrapper.i("Trap", URLDecoder.decode(url));
            	if(info.contains("wappaygw") && info.contains("authAndExecute") && info.contains("request_token")){
            		int i = info.indexOf("<request_token>");
            		int j = info.indexOf("</request_token>");
            		StringBuilder sb = new StringBuilder();
            		sb.append("ordertoken=\"");
            		if(i > 0 && i < j){
            			sb.append(info.substring(i+15, j));
            		}else return;
            		sb.append("\"");
            		MobileSecurePayer msp = new MobileSecurePayer();
            		MobileSecurePayHelper mspHelper = new MobileSecurePayHelper(mThis.getBaseContext());
            		boolean isMobile_spExist = mspHelper.detectMobile_sp();
            		if (!isMobile_spExist) {
            			return;
            		}
            		boolean bRet = msp.pay(sb.toString(), mHandler, ALIX_PAY, mThis);
            	}
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

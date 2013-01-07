package com.weibo.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.decarta.android.util.LogWrapper;

public class WeiboDialog extends Dialog {

    static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    static final int MARGIN = 4;
    static final int PADDING = 2;

    private final Weibo mWeibo;
    private String mUrl;
    private WeiboDialogListener mListener;
    private ProgressDialog mSpinner;
    private ImageView mBtnClose;
    private WebView mWebView;
    private RelativeLayout webViewContainer;
    private RelativeLayout mContent;

    private final static String TAG = "Weibo-WebView";

    public WeiboDialog(final Weibo weibo, Context context, String url, WeiboDialogListener listener) {
        super(context, R.style.WeiboDialog);
        mWeibo = weibo;
        mUrl = url;
        mListener = listener;
        setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface arg0) {
                mListener.onCancel();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage(getContext().getString(R.string.weibo_loading));
        mSpinner.setCancelable(true);
        setUpSpinner();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mContent = new RelativeLayout(getContext());

        setUpWebView();

        // setUpCloseBtn();

        addContentView(mContent, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void setUpSpinner() {
    	mSpinner.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				LogWrapper.d("eric", "mSpinner.onKey keyCode: " + keyCode);
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					mWebView.stopLoading();
					dismiss();
				}
				return false;
			}
		});
    }
    
	private void setUpWebView() {
    	ActionLog.getInstance(getContext()).addAction(ActionLog.WeiboAuthorize);
        webViewContainer = new RelativeLayout(getContext());

        mWebView = new WebView(getContext());
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setSaveFormData(false);
        mWebView.setWebViewClient(new WeiboDialog.WeiboWebViewClient());
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(FILL);
        mWebView.setVisibility(View.INVISIBLE);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(false);
        
        webViewContainer.addView(mWebView);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        Resources resources = getContext().getResources();
        lp.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_left_margin);
        lp.topMargin = resources.getDimensionPixelSize(R.dimen.dialog_top_margin);
        lp.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_right_margin);
        lp.bottomMargin = resources.getDimensionPixelSize(R.dimen.dialog_bottom_margin);
        mContent.addView(webViewContainer, lp);
    }

    private void setUpCloseBtn() {
        mBtnClose = new ImageView(getContext());
        mBtnClose.setClickable(true);
        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCancel();
                dismiss();
            }
        });

        mBtnClose.setImageResource(R.drawable.close_selector);
        mBtnClose.setVisibility(View.INVISIBLE);

        RelativeLayout.LayoutParams closeBtnRL = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        closeBtnRL.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        closeBtnRL.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        closeBtnRL.topMargin = getContext().getResources().getDimensionPixelSize(
                R.dimen.dialog_btn_close_right_margin);
        closeBtnRL.rightMargin = getContext().getResources().getDimensionPixelSize(
                R.dimen.dialog_btn_close_top_margin);

        webViewContainer.addView(mBtnClose, closeBtnRL);
    }

    private class WeiboWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Redirect URL: " + url);
            // 待后台增加对默认重定向地址的支持后修改下面的逻辑
            if (url.startsWith(mWeibo.getRedirectUrl())) {
                handleRedirectUrl(view, url);
                dismiss();
                return true;
            }
            // launch non-dialog URLs in a full browser
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
        	Log.d(TAG, "onReceivedError");
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(new DialogError(description, errorCode, failingUrl));
			dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "onPageStarted URL: " + url);
            // google issue. shouldOverrideUrlLoading not executed

            if (url.startsWith(mWeibo.getRedirectUrl())) {
                handleRedirectUrl(view, url);
                view.stopLoading();
                Log.d(TAG, "WeiboDialog.this.dismiss()");
                dismiss();
                
                return;
            }
            
            super.onPageStarted(view, url, favicon);
            mSpinner.show();

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "onPageFinished URL: " + url);
            super.onPageFinished(view, url);
            mSpinner.dismiss();

            mContent.setBackgroundColor(Color.TRANSPARENT);
            webViewContainer.setBackgroundResource(R.drawable.dialog_bg);
            // mBtnClose.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.VISIBLE);
        }

        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        	Log.d(TAG, "onReceivedSslError");
            handler.proceed();
        }

    }

    private void handleRedirectUrl(WebView view, String url) {
    	Log.d(TAG, "handleRedirectUrl: " + url);
    	/*
    	 * 有时在获得token后, 还会再重定向到这个地址, 从而使token清空
    	 */
    	if (url.equals("http://www.sina.com.cn/")) {
    		return;
    	}
    	
        Bundle values = Utility.parseUrl(url);

        String error = values.getString("error");
        String error_code = values.getString("error_code");

        if (error == null && error_code == null) {
        	LogWrapper.d("eric", "WeiboDialog.handleRedirectUrl: " + url);
            mListener.onComplete(values);
        } else if (error.equals("access_denied")) {
            // 用户或授权服务器拒绝授予数据访问权限
            mListener.onCancel();
        } else {
            mListener.onWeiboException(new WeiboException(error, Integer.parseInt(error_code)));
        }
    }

    private static String getHtml(String urlString) {

        try {

            StringBuffer html = new StringBuffer();

            SocketAddress sa = new InetSocketAddress("10.75.0.103", 8093);
            Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP, sa);

            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);

            InputStreamReader isr = new InputStreamReader(conn.getInputStream());

            BufferedReader br = new BufferedReader(isr);

            String temp;

            while ((temp = br.readLine()) != null) {

                html.append(temp);

            }

            br.close();

            isr.close();
            return html.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return null;

        }

    }

	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		super.dismiss();
		CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
	}
}

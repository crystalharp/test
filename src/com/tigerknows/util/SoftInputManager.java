package com.tigerknows.util;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class SoftInputManager {

    Activity activity;
    InputMethodManager inputMethodManager;
    Handler handler;
    View showView;
    boolean isShow = true;
    
    public SoftInputManager(Activity activity) {
        this.activity = activity;
        this.inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        this.handler = new Handler(activity.getMainLooper());
    }
    
    private Runnable showSoftInput = new Runnable() {
        
        @Override
        public void run() {
            inputMethodManager.showSoftInput(showView, InputMethodManager.SHOW_IMPLICIT);
        }
    };
    
    public void showSoftInput(View showView) {
        this.showView = showView;
        handler.post(showSoftInput);
    }
    
    private Runnable hideSoftInput = new Runnable() {
        
        @Override
        public void run() {
            if (activity.getCurrentFocus() != null) {
                inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    };    
    
    public void hideSoftInput() {
        handler.post(hideSoftInput);
    }    
}

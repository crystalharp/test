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
    
    private class HideSoftInput implements Runnable {

        View view;
        
        @Override
        public void run() {
            if (view != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        
    }
    
    private HideSoftInput hideSoftInput = new HideSoftInput();    
    
    public void hideSoftInput() {
        hideSoftInput.view = activity.getCurrentFocus();
        handler.post(hideSoftInput);
    }     
    
    public void hideSoftInput(View view) {
        hideSoftInput.view = view;
        handler.post(hideSoftInput);
    }    
}

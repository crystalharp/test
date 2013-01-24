package com.tigerknows.util;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class SoftInputManager {

    Activity activity;
    InputMethodManager inputMethodManager;
    Handler handler;
    
    public SoftInputManager(Activity activity) {
        this.activity = activity;
        this.inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        this.handler = new Handler(activity.getMainLooper());
    }

    
    private class ShowSoftInput implements Runnable {

        View view;
        
        @Override
        public void run() {
            if (view != null) {
                view.requestFocus();
                inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
        
    }
    
    private ShowSoftInput showSoftInput = new ShowSoftInput();
    
    public void showSoftInput(View view) {
        if (view != null) {
            showSoftInput.view = view;
            handler.postDelayed(showSoftInput, 200);
        }
    }
    
    public void showSoftInput() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            showSoftInput.view = view;
            handler.postDelayed(showSoftInput, 200);
        }
    }
    
    private class HideSoftInput implements Runnable {

        View view;
        
        @Override
        public void run() {
            if (view != null) {
                view.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        
    }
    
    private HideSoftInput hideSoftInput = new HideSoftInput();    
    
    public void hideSoftInput() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }     
    
    public void postHideSoftInput() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            hideSoftInput.view = view;
            handler.post(hideSoftInput);
        }
    }  
    
    public void hideSoftInput(View view) {

        if (view != null) {
            hideSoftInput.view = view;
            handler.post(hideSoftInput);
        }
    }  
}

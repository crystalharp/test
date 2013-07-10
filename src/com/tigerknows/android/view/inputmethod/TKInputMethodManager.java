package com.tigerknows.android.view.inputmethod;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * 管理虚拟键盘
 * 显示或隐藏
 * @author pengwenyue
 *
 */
public class TKInputMethodManager {

    Activity activity;
    InputMethodManager inputMethodManager;
    Handler handler;
    
    public TKInputMethodManager(Activity activity) {
        this.activity = activity;
        this.inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        this.handler = new Handler(activity.getMainLooper());
    }

    /**
     * 显示虚拟键盘
     * @author pengwenyue
     *
     */
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
    
    /**
     * 显示虚拟键盘
     * @param view 指定需要显示虚拟键盘的view
     */
    public void showSoftInput(View view) {
        postShowSoftInput(view);
    }
    
    /**
     * 显示虚拟键盘
     * @param view 指定需要显示虚拟键盘的view
     */
    public void postShowSoftInput(View view) {
        if (view != null) {
            showSoftInput.view = view;
            handler.postDelayed(showSoftInput, 200);
        }
    }
    
    /**
     * 显示虚拟键盘
     */
    public void showSoftInput() {
        postShowSoftInput();
    }
    
    /**
     * 显示虚拟键盘
     */
    public void postShowSoftInput() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            showSoftInput.view = view;
            handler.postDelayed(showSoftInput, 200);
        }
    }
    
    /**
     * 隐藏虚拟键盘
     * @author pengwenyue
     *
     */
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
    
    /**
     * 隐藏虚拟键盘
     */
    public void hideSoftInput() {
        hideSoftInput(true);
    }
    
    public void hideSoftInput(boolean clearFocus) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            if (clearFocus) {
                view.clearFocus();
            }
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }     
    
    /**
     * 隐藏虚拟键盘
     */
    public void postHideSoftInput() {
        View view = activity.getCurrentFocus();
        if (view != null) {
            hideSoftInput.view = view;
            handler.post(hideSoftInput);
        }
    }  
    
    /**
     * 隐藏虚拟键盘
     * @param view 指定需要隐藏虚拟键盘的view
     */
    public void hideSoftInput(View view) {
        if (view != null) {
            view.clearFocus();
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }      
    
    /**
     * 隐藏虚拟键盘
     * @param view 指定需要隐藏虚拟键盘的view
     */
    public void postHideSoftInput(View view) {

        if (view != null) {
            hideSoftInput.view = view;
            handler.post(hideSoftInput);
        }
    }  
}

/*
 * Copyright (C) 2007 2010 Beijing TigerKnows Science and Technology Ltd. All rights reserved.
 */
package com.tigerknows.util;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Response;
import com.tigerknows.share.TKWeibo;
import com.tigerknows.share.TKTencentOpenAPI;
import com.weibo.net.Weibo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


public class TKAsyncTask extends AsyncTask<Void, Integer, Void> {
    
    static final String TAG = "TKAsyncTask";
    
    private EventListener eventListener;
    
    private Activity activity;
    
    private List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
    
    private Runnable cancelTask;
    
    private ProgressDialog tipProgressDialog;
    
    private boolean cancelable = true;
    
    public Runnable getCancelTask() {
        return cancelTask;
    }

    public void stop() {
        LogWrapper.d(TAG, "stop()");
        for(BaseQuery baseQuery : baseQueryList) {
            baseQuery.stop();
        }
        cancel(true);
    }
    
    public TKAsyncTask(Activity activity, BaseQuery baseQuery, EventListener eventListener, Runnable cancelTask) {
        this(activity, baseQuery, eventListener, cancelTask, true);
    }
    
    public TKAsyncTask(Activity activity, BaseQuery baseQuery, EventListener eventListener, Runnable cancelTask, boolean cancelable) {
        this.activity = activity;
        this.baseQueryList.add(baseQuery);
        this.eventListener = eventListener;
        this.cancelTask = cancelTask;
        this.cancelable = cancelable;
    }
    
    public TKAsyncTask(Activity activity, List<BaseQuery> baseQueryList, EventListener eventListener, Runnable cancelTask) {
        this.activity = activity;
        this.baseQueryList = baseQueryList;
        this.eventListener = eventListener;
        this.cancelTask = cancelTask;
        this.cancelable = true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        String tipText = null;
        for(BaseQuery baseQuery : baseQueryList) {
            tipText = baseQuery.getTipText();
            if (!TextUtils.isEmpty(tipText)) {
                break;
            }
        }
        
        if (TextUtils.isEmpty(tipText)) {
            return;
        }
        
        tipProgressDialog = new ProgressDialog(activity);
        tipProgressDialog.setCanceledOnTouchOutside(false);
        tipProgressDialog.setMessage(tipText);
        tipProgressDialog.setIndeterminate(true);
        tipProgressDialog.setCancelable(cancelable);
        tipProgressDialog.setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface arg0) {
                TKAsyncTask.this.stop();
            }
        });
        tipProgressDialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        for(BaseQuery baseQuery : baseQueryList) {
            baseQuery.query();
            if (baseQuery.getAPIType().equals(BaseQuery.API_TYPE_DATA_OPERATION)) {
                Response response = baseQuery.getResponse();
                if (response != null && response.getResponseCode() == Response.RESPONSE_CODE_OK) {
                    Hashtable<String, String> criteria =  baseQuery.getCriteria();
                    String shareSina = criteria.get(DataOperation.SERVER_PARAMETER_SHARE_SINA);
                    if (!TextUtils.isEmpty(shareSina)) {
                        TKWeibo tkweibo = new TKWeibo(activity, false, false);
                        TKWeibo.update(tkweibo, Weibo.getInstance(), Weibo.getAppKey(), shareSina, "", "");
                    }
                    String shareQzone = criteria.get(DataOperation.SERVER_PARAMETER_SHARE_QZONE);
                    if (!TextUtils.isEmpty(shareQzone)) {
                        TKTencentOpenAPI.addShare(activity, shareQzone, false, false);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        return;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
            tipProgressDialog.dismiss();
        }
        if (this.eventListener != null) {
            this.eventListener.onPostExecute(this);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (tipProgressDialog != null && tipProgressDialog.isShowing()) {
            tipProgressDialog.dismiss();
        }
        if (this.eventListener != null) {
            this.eventListener.onCancelled(this);
        }
    }
    
    public BaseQuery getBaseQuery() {
        return baseQueryList.get(0);
    }
    
    public List<BaseQuery> getBaseQueryList() {
        return baseQueryList;
    }
    
    public interface EventListener {
        public void onPostExecute(TKAsyncTask tkAsyncTask);
        public void onCancelled(TKAsyncTask tkAsyncTask);
    }
}
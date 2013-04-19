/*
 * Copyright (C) 2007 2010 Beijing TigerKnows Science and Technology Ltd. All rights reserved.
 */
package com.tigerknows.android.os;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Response;
import com.tigerknows.share.TKWeibo;
import com.tigerknows.share.TKTencentOpenAPI;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.util.Hashtable;
import java.util.List;


public class TKAsyncTask extends AsyncTask<Void, Integer, Void> {
    
    static final String TAG = "TKAsyncTask";
    
//    boolean isCancel = false;
    private EventListener eventListener;
    
    private Activity activity;
    
    private List<BaseQuery> baseQueryList;
    
    private Runnable cancelTask;
    
    private Dialog tipProgressDialog;
    
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
//        if (isCancel == false) {
//            isCancel = true;
//            if (this.eventListener != null) {
//                eventListener.onCancelled(this);
//            }
//        }
    }
    
    public TKAsyncTask(Activity activity, List<BaseQuery> baseQueryList, EventListener eventListener, Runnable cancelTask) {
    	this(activity, baseQueryList, eventListener, cancelTask, true);
    }
    
    public TKAsyncTask(Activity activity, List<BaseQuery> baseQueryList, EventListener eventListener, Runnable cancelTask, boolean cancelable) {
        this.activity = activity;
        this.baseQueryList = baseQueryList;
        this.eventListener = eventListener;
        this.cancelTask = cancelTask;
        this.cancelable = cancelable;
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
        
        View custom = activity.getLayoutInflater().inflate(R.layout.loading, null);
        TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
        loadingTxv.setText(tipText);
        ActionLog.getInstance(activity).addAction(ActionLog.Dialog, tipText);
        tipProgressDialog = Utility.showNormalDialog(activity, custom);
        tipProgressDialog.setCancelable(cancelable);
        tipProgressDialog.setCanceledOnTouchOutside(false);
        tipProgressDialog.setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface arg0) {
                TKAsyncTask.this.stop();
            }
        });
        tipProgressDialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                ActionLog.getInstance(activity).addAction(ActionLog.Dialog + ActionLog.Dismiss);
            }
        });

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
                        TKWeibo.update(tkweibo, shareSina, "", "");
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
//        if (isCancel) {
//            return;
//        }

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
//        if (isCancel == false) {
//            isCancel = true;
            if (this.eventListener != null) {
                this.eventListener.onCancelled(this);
            }
//        }
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
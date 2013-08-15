/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.DataQuery.ShangjiaResponse;
import com.tigerknows.model.DataQuery.ShangjiaResponse.ShangjiaList;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BrowserActivity;

/**
 * @author Peng Wenyue
 */
public class ShangjiaListActivity extends BaseActivity {
    
    static final String TAG = "TuangouShopListActivity";

    private DataQuery mDataQuery = null;
    
    private ListView mResultLsv = null;
    private List<Shangjia> mResultList = new ArrayList<Shangjia>();
    private ResultAdapter mResultAdapter;
    
    private int mPictureHeight;
    
    private String sessionId;
    
    protected DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            finish();
        }
    };
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.DingdanList;
        mId = R.id.activity_discover_shangjia_list;

        setContentView(R.layout.discover_shangjia_list);
        
        findViews();
        setListener();
        
        mPictureHeight = (int)(Globals.g_metrics.density*28);

        mResultAdapter = new ResultAdapter(mThis, mResultList);
        mResultLsv.setAdapter(mResultAdapter);
        
        mTitleBtn.setText(R.string.tuangou_shop_list);
        mRightBtn.setVisibility(View.GONE);
    }

    protected void findViews() {
        super.findViews();
        mResultLsv = (ListView)findViewById(R.id.result_lsv);
    }
    
    protected void setListener() {
        super.setListener();
        mResultLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Adapter adapter = adapterView.getAdapter();
                if (position < adapter.getCount()) {
                    Shangjia shangjia = (Shangjia) adapter.getItem(position);
                    if (shangjia != null && TextUtils.isEmpty(shangjia.getUrl()) == false) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, shangjia.getName());
                        Intent intent = new Intent();
                        intent.setClass(mThis, BrowserActivity.class);
                        intent.putExtra(BrowserActivity.TITLE, mThis.getString(R.string.wodedingdan));
                        intent.putExtra(BrowserActivity.LEFT, mThis.getString(R.string.tuangou_shop_list));
                        intent.putExtra(BrowserActivity.URL, shangjia.getUrl());
                        intent.putExtra(BrowserActivity.TIP, shangjia.getName());
                        startActivityForResult(intent, R.id.activity_browser);
                    }
                }
            }
        });
    }
    
    protected void onResume() {
        super.onResume();
        sessionId = Globals.g_Session_Id;
        if (TextUtils.isEmpty(sessionId)) {
            finish();
            return;
        }
        if (mDataQuery == null || mDataQuery.getParameter(BaseQuery.SERVER_PARAMETER_SESSION_ID).equals(sessionId) == false) {
            DataQuery dataQuery = new DataQuery(mThis);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_SHANGJIA);
            dataQuery.setup(Globals.getCurrentCityInfo().getId(), mId, mId, mThis.getString(R.string.doing_and_wait));
            queryStart(dataQuery);
        } else {
            setData(mDataQuery);
        }
    }

    private void setData(DataQuery dataQuery) {
        mResultList.clear();
        Response response = dataQuery.getResponse();
        ShangjiaResponse shangjiaResponse = (ShangjiaResponse)response;
        ShangjiaList shangjiaList = shangjiaResponse.getList();
        if (shangjiaList != null) {
            List<Shangjia> shangjiaArrayList = shangjiaList.getList();
            if (shangjiaArrayList != null && shangjiaArrayList.size() > 0) {
                mDataQuery = dataQuery;
                mDataQuery.setParameter(BaseQuery.SERVER_PARAMETER_SESSION_ID, sessionId);
                mResultList.addAll(shangjiaArrayList);
                mResultAdapter.notifyDataSetChanged();
            }
        }
        
        if (mResultList.isEmpty()) {
            finish();
        }
    }
    
    private class ResultAdapter extends ArrayAdapter<Shangjia>{
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.discover_shangjia_list_item;

        public ResultAdapter(Context context, List<Shangjia> list) {
            super(context, TEXTVIEW_RESOURCE_ID, list);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView logoImv = (ImageView) view.findViewById(R.id.logo_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            
            Shangjia shangjia = getItem(position);
            Drawable drawable = shangjia.getLogo();
            if (drawable != null) {
                logoImv.setBackgroundDrawable(drawable);
                ViewGroup.LayoutParams layoutParams = logoImv.getLayoutParams();
                layoutParams.height = mPictureHeight;
                layoutParams.width = (int)(mPictureHeight*((float) drawable.getIntrinsicWidth()/drawable.getIntrinsicHeight()));
            } else {
                logoImv.setBackgroundDrawable(null);
            }
            nameTxv.setText(shangjia.getMessage());
            
            return view;
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        if (dataQuery.isStop()) {
            finish();
        }
        if (BaseActivity.checkReLogin(dataQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mThis, mThis)) {
            return;
        }
        
        setData(dataQuery);
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        finish();
    }
}

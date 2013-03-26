/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import com.decarta.Globals;
import com.snda.recommend.api.RecommendAPI;
import com.tigerknows.R;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.BootstrapModel.Recommend;
import com.tigerknows.model.BootstrapModel.Recommend.RecommendApp;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class AppRecommend extends BaseActivity implements View.OnClickListener {

    private ListView mAppRecommendLsv = null;
    private Button mSndaBtn;

    private RecommdAppAdapter mRecommdAppAdapter;
    private List<RecommendApp> mRecommendAppList = new ArrayList<RecommendApp>();
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mHandler.removeCallbacks(mActualLoadedDrawableRun);
            mHandler.post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            if (isFinishing() == false) {
                mRecommdAppAdapter.notifyDataSetChanged();
            }
        }
    };
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.AppRecommend;

        setContentView(R.layout.app_recommend);
        findViews();
        setListener();
        
        mTitleBtn.setText(R.string.app_recommend);
        mRightBtn.setVisibility(View.GONE);

        mRecommdAppAdapter = new RecommdAppAdapter(mThis, mRecommendAppList);
        mAppRecommendLsv.setAdapter(mRecommdAppAdapter);
        
        BootstrapModel bootstrapModel = Globals.g_Bootstrap_Model;
        if (bootstrapModel != null) {
            Recommend recommend = bootstrapModel.getRecommend();
            if (recommend != null) {
                initRecommendAppList(recommend.getRecommendAppList());
                return;
            }
        }
        
        Bootstrap bootstrap = new Bootstrap(mThis);
        bootstrap.setup(null, Globals.g_Current_City_Info.getId(), -1, -1, mThis.getString(R.string.doing_and_wait));
        queryStart(bootstrap);
    }

    protected void findViews() {
        super.findViews();
        mSndaBtn = (Button) findViewById(R.id.snda_btn);
        mAppRecommendLsv = (ListView)findViewById(R.id.app_recommend_lsv);
    }
    
    public void setListener() {
        super.setListener();
        mSndaBtn.setOnClickListener(this);
        mAppRecommendLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int postion, long arg3) {
                final RecommendApp recommendApp = mRecommendAppList.get(postion);
                if (recommendApp != null) {
                    mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "list", postion, recommendApp.getName());
                    final String uri = recommendApp.getUrl();
                    if (!TextUtils.isEmpty(uri)) {
                        CommonUtils.showNormalDialog(mThis, mThis.getString(R.string.prompt), 
                                mThis.getString(R.string.are_you_sure_download_this_software), 
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface arg0, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                                            mThis.startActivity(intent);
                                        }
                                    }
                        });
                    }
                }
            }});
    }
    
    private void initRecommendAppList(List<RecommendApp> recommendApps) {
        if (recommendApps == null) {
            return;
        }
        mRecommendAppList.clear();
        mRecommendAppList.addAll(recommendApps);
        mRecommdAppAdapter.notifyDataSetChanged();
    }
    
    private class RecommdAppAdapter extends ArrayAdapter<RecommendApp> {
        
        public RecommdAppAdapter(Context context, List<RecommendApp> recommendApps) {
            super(context, R.layout.app_recommend_list_item, recommendApps);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.app_recommend_list_item, parent, false);
            } else {
                view = convertView;
            }

            RecommendApp recommdApp = getItem(position);
            
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            nameTxv.setText(recommdApp.getName());
            TextView descriptionTxv = (TextView) view.findViewById(R.id.description_txv);
            descriptionTxv.setText(recommdApp.getBody());
            ImageView iconImv = (ImageView) view.findViewById(R.id.icon_imv);
            TKDrawable tkDrawable = recommdApp.getIcon();
            Drawable drawable = null;
            if (tkDrawable != null) {
                drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, mThis.toString());
            }
            if (drawable == null) {
                drawable = getResources().getDrawable(R.drawable.ic_app_icon_loading);
            }
            iconImv.setImageDrawable(drawable);
            return view;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.snda_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "snda");
                boolean bRet = RecommendAPI.init(mThis, "800109436", "ANDsnda");
                RecommendAPI.setSdid("");
                RecommendAPI.setPhoneNum("");
                RecommendAPI.setFromPos(mThis, RecommendAPI.MAIN_TOP);
                if (bRet == true) {
                    RecommendAPI.openRecommendActivity(mThis);
                }
                break;

            default:
                break;
        }
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (baseQuery instanceof Bootstrap) {
            BootstrapModel bootstrapModel = ((Bootstrap) baseQuery).getBootstrapModel();
            if (bootstrapModel != null) {
                Globals.g_Bootstrap_Model = bootstrapModel;
                Recommend recommend = bootstrapModel.getRecommend();
                if (recommend != null) {
                    initRecommendAppList(recommend.getRecommendAppList());
                    return;
                }
            }
            Toast.makeText(mThis, R.string.get_app_recommend_failed, Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        finish();
    }
}

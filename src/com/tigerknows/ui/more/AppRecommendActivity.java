/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.BootstrapModel.Recommend;
import com.tigerknows.model.BootstrapModel.Recommend.RecommendApp;
import com.tigerknows.service.download.ApkDownloadedProcessor;
import com.tigerknows.service.download.DownloadService;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class AppRecommendActivity extends BaseActivity {

    private ListView mAppRecommendLsv = null;

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
            mRecommdAppAdapter.notifyDataSetChanged();
        }
    };
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.AppRecommend;

        setContentView(R.layout.more_app_recommend);
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
        bootstrap.setup(-1, -1, mThis.getString(R.string.doing_and_wait));
        queryStart(bootstrap);
    }

    protected void findViews() {
        super.findViews();
        mAppRecommendLsv = (ListView)findViewById(R.id.app_recommend_lsv);
    }
    
    public void setListener() {
        super.setListener();
        mAppRecommendLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int postion, long arg3) {
                final RecommendApp recommendApp = mRecommendAppList.get(postion);
                if (recommendApp != null) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItem, postion, recommendApp.getName());
                    final String url = recommendApp.getUrl();
                    if (!TextUtils.isEmpty(url)) {
                        Utility.showNormalDialog(mThis, getString(R.string.are_you_sure_download_this_software), new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    DownloadService.download(mThis, url, recommendApp.getName(), ApkDownloadedProcessor.getInstance());
                                }
                            }
                        });
                    }
                }
            }
        });
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
        
        static final int RES_ID = R.layout.more_app_recommend_list_item;
        
        public RecommdAppAdapter(Context context, List<RecommendApp> recommendApps) {
            super(context, RES_ID, recommendApps);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RES_ID, parent, false);
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
                drawable = getResources().getDrawable(R.drawable.bg_picture_small);
            }
			Rect bounds = drawable.getBounds();
			if(bounds != null && (bounds.width() != iconImv.getWidth() || bounds.height() != iconImv.getHeight())){
				iconImv.setBackgroundDrawable(null);
			}
            iconImv.setImageDrawable(drawable);
            return view;
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

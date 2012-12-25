/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.BaseFragment;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class DianyingDetailFragment extends DiscoverBaseFragment implements View.OnClickListener {
    
    public DianyingDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private ScrollView mBodyScv;
    
    private ImageView mPictureImv = null;

    private TextView mNameTxt = null;
    
    private TextView mAliasTxv;
    
    private RatingBar mRankRtb;
    
    private TextView mDirectorTxv;

    private TextView mMainActorTxv = null;

    private TextView mTagTxv = null;

    private TextView mCountryTxv = null;

    private TextView mLengthTxv;

    private TextView mStartTimeTxv;
    
    private View mNearbyFendianView;

    private TextView mNearbyFendianTxv;
    
    private TextView mFendianNameTxv = null;
    
    private Button mDistanceBtn = null;
    
    private TextView mAddressTxv = null;
    
    private TextView mTelephoneTxv = null;
    
    private Button mTodayBtn = null;
    
    private Button mTomorrowBtn = null;
    
    private Button mAfterTomorrowBtn = null;

    //private CollapseTextView mDetailMessageTxv;
    private TextView mDescriptionTxv;
    
    private Dianying mDianying;
    
    private String mFilterArea;
    
    private Yingxun mYingxun;
    
    private LinearLayout mChangciListView;

    private ImageView mShowTimeDividerImv;
    
    private ViewGroup mShowTimeView;
    
    private View mAddressView = null;
    
    private View mDividerView;
    
    private View mTelephoneView = null;
    
    private View mNotimeView = null;
    
    private View mLoadingView;
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
            mSphinx.getHandler().post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            Drawable drawable = mDianying.getPicturesDetail().loadDrawable(null, null, null);
            if(drawable != null) {
                mPictureImv.setImageDrawable(drawable);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.DianyingXiangqing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);        
        mRootView = mLayoutInflater.inflate(R.layout.dianying_detail, container, false);
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.dianying_detail);
        mRightImv.setImageResource(R.drawable.ic_view_map);
        mRightBtn.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightBtn.setOnClickListener(this);   
        mBodyScv.scrollTo(0, 0);
        
        refreshDrawable();
        
        if (isReLogin()) {
            return;
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mPictureImv.setImageDrawable(null);
    }
    
    public void setData(Dianying dianying) {
        mDianying = dianying;
        if (mDianying == null) {
            return;
        }
        mFilterArea = mDianying.getFilterArea();
        
        mNameTxt.setText(mDianying.getName());
        //mAliasTxv.setText(String.valueOf(mDianying.getAlias()));
        mRankRtb.setProgress((int) mDianying.getRank());
        
        if (TextUtils.isEmpty(mDianying.getDirector()) == false) {
        	//mDirectorTxv.setText(mSphinx.getString(R.string.dianying_detail_director,mDianying.getDirector()));
        	mDirectorTxv.setVisibility(View.VISIBLE);
        	String colortxt = mSphinx.getString(R.string.dianying_detail_director,mDianying.getDirector());
        	SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(0x99000000),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);  
            mDirectorTxv.setText(style);
        }
        else
        	mDirectorTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mDianying.getMainActor()) == false) {
        	//mMainActorTxv.setText(mSphinx.getString(R.string.dianying_detail_main_actor,mDianying.getMainActor()));
        	mMainActorTxv.setVisibility(View.VISIBLE);
        	String colortxt = mSphinx.getString(R.string.dianying_detail_main_actor,mDianying.getMainActor());
        	SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(0x99000000),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mMainActorTxv.setText(style);
        }
        else
        	mMainActorTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mDianying.getTag()) == false) {
        	//mTagTxv.setText(mSphinx.getString(R.string.dianying_detail_tag,mDianying.getTag()));
        	mTagTxv.setVisibility(View.VISIBLE);
        	String colortxt = mSphinx.getString(R.string.dianying_detail_tag,mDianying.getTag());
        	SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(0x99000000),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTagTxv.setText(style);
        }
        else
        	mTagTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mDianying.getCountry()) == false) {
        	//mCountryTxv.setText(mSphinx.getString(R.string.dianying_detail_country,mDianying.getCountry()));
        	mCountryTxv.setVisibility(View.VISIBLE);
        	String colortxt = mSphinx.getString(R.string.dianying_detail_country,mDianying.getCountry());
        	SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(0x99000000),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mCountryTxv.setText(style);
        }
        else
        	mCountryTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mDianying.getLength()) == false) {
        	//mLengthTxv.setText(mSphinx.getString(R.string.dianying_detail_length,mDianying.getLength()));
        	mLengthTxv.setVisibility(View.VISIBLE);
        	String colortxt = mSphinx.getString(R.string.dianying_detail_length,mDianying.getLength());
        	SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(0x99000000),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mLengthTxv.setText(style);
        }
        else
        	mLengthTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mDianying.getStartTime()) == false) {
        	//mStartTimeTxv.setText(mSphinx.getString(R.string.dianying_detail_start_time,mDianying.getStartTime()));
        	mStartTimeTxv.setVisibility(View.VISIBLE);
        	String colortxt = mSphinx.getString(R.string.dianying_detail_start_time,mDianying.getStartTime());
        	SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(0x99000000),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mStartTimeTxv.setText(style);
        }
        else
        	mStartTimeTxv.setVisibility(View.GONE);
        if (TextUtils.isEmpty(mFilterArea) || mDianying.getNum() < 2) {
            mNearbyFendianView.setVisibility(View.GONE);
        } else {
            mNearbyFendianTxv.setText(mSphinx.getString(R.string.dianying_detail_nearby,mFilterArea,mDianying.getNum()));
            mNearbyFendianView.setVisibility(View.VISIBLE);
            mNearbyFendianView.setBackgroundResource(R.drawable.list_single);
        }
        
        refreshDescription(true);
        
        mYingxun = mDianying.getYingxun();
        DiscoverChildListFragment.showPOI(mSphinx, mYingxun.getName(), mYingxun.getDistance(), mYingxun.getAddress(), mYingxun.getPhone(), 
                mFendianNameTxv, mDistanceBtn, mAddressView, mDividerView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_header, R.drawable.list_middle, R.drawable.list_header);
        
        mNotimeView.setVisibility(View.GONE);
        refreshShowTime();
        refreshDrawable();
    }
    
    private void refreshDrawable() {
        if (mDianying == null) {
            return;
        }
        TKDrawable tkDrawable = mDianying.getPicturesDetail();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, this.toString());
            if(drawable != null) {
                mPictureImv.setImageDrawable(drawable);
            } else {
                mPictureImv.setImageDrawable(null);
            }
        } else {
            mPictureImv.setImageDrawable(null);
        }
    }
    
    private void refreshShowTime() {
        DiscoverChildListFragment.makeChangciView(mYingxun, mSphinx, mLayoutInflater, mChangciListView);
        DiscoverChildListFragment.refreshDayView(mYingxun, mTodayBtn, mTomorrowBtn, mAfterTomorrowBtn, mShowTimeDividerImv, mNotimeView);
    }
    
    private void refreshDescription(boolean query) {
        if (mDianying == null) {
            return;
        }
        String str = mDianying.getDescription();
        if (!TextUtils.isEmpty(str)) {
            mDescriptionTxv.setText(str);
            mDescriptionTxv.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        } else if (query){
            mDescriptionTxv.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.VISIBLE);
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_DIANYING);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mDianying.getUid());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), null, true);
            mSphinx.queryStart(dataOperation);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mBodyScv = (ScrollView) mRootView.findViewById(R.id.body_scv);
        mPictureImv = (ImageView) mRootView.findViewById(R.id.picture_imv);
        mNameTxt = (TextView)mRootView.findViewById(R.id.name_txv);
        //mAliasTxv = (TextView)mRootView.findViewById(R.id.alias_txv);
        mRankRtb = (RatingBar) mRootView.findViewById(R.id.stars_rtb);
        mDirectorTxv = (TextView)mRootView.findViewById(R.id.director_txv);
        mMainActorTxv = (TextView) mRootView.findViewById(R.id.main_actor_txv);
        mTagTxv = (TextView) mRootView.findViewById(R.id.tag_txv);
        mCountryTxv = (TextView) mRootView.findViewById(R.id.country_txv);
        mLengthTxv = (TextView)mRootView.findViewById(R.id.length_txv);
        mStartTimeTxv = (TextView)mRootView.findViewById(R.id.start_time_txv);
        //mToadyTxv = (Button)mRootView.findViewById(R.id.today_txv);
        
        View view =  mRootView.findViewById(R.id.dianying_fendian_list_item);
       // no_time_view
        
        mChangciListView = (LinearLayout) view.findViewById(R.id.changci_list_view);
        mTodayBtn = (Button)view.findViewById(R.id.today_btn);
        mTomorrowBtn = (Button)view.findViewById(R.id.tomorrow_btn);
        mAfterTomorrowBtn = (Button)view.findViewById(R.id.after_tomorrow_btn);
        mShowTimeDividerImv = (ImageView) view.findViewById(R.id.show_time_divider_imv);
        mShowTimeView = (ViewGroup) view.findViewById(R.id.show_time_view);
        DiscoverChildListFragment.layoutShowTimeView(mShowTimeView, mShowTimeDividerImv);
        
        mNotimeView = view.findViewById(R.id.no_time_view);
        mAddressView = view.findViewById(R.id.address_view);
        mDividerView = view.findViewById(R.id.divider_imv);
        mTelephoneView = view.findViewById(R.id.telephone_view);
        mFendianNameTxv = (TextView) view.findViewById(R.id.fendian_name_txv);
        mDistanceBtn = (Button)view.findViewById(R.id.distance_btn);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mNearbyFendianView = mRootView.findViewById(R.id.nearby_fendian_view);
        mNearbyFendianTxv = (TextView)mRootView.findViewById(R.id.nearby_fendian_txv);
        mDescriptionTxv = (TextView) mRootView.findViewById(R.id.description_txv);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
    }

    protected void setListener() {
        mNearbyFendianView.setOnClickListener(this);
        mChangciListView.setOnClickListener(this);
        mTodayBtn.setOnClickListener(this);
        mTomorrowBtn.setOnClickListener(this);
        mAfterTomorrowBtn.setOnClickListener(this);
        mDistanceBtn.setOnClickListener(this);
        
        mAddressView.setOnClickListener(this);
        mTelephoneTxv.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.Title_Right_Button, mActionTag);
                viewMap();
                break;
            case R.id.telephone_txv:
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailTelphone);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailAddress);
                viewMap();
                break;
                
            case R.id.after_tomorrow_btn:
                mActionLog.addAction(mActionTag+ActionLog.DianyingAfterTomorrow);
                if (Changci.OPTION_DAY_AFTER_TOMORROW == mYingxun.getChangciOption()) {
                    mYingxun.setChangciOption(0);
                } else {
            	    mYingxun.setChangciOption(Changci.OPTION_DAY_AFTER_TOMORROW);
                }
            	refreshShowTime();
                break;
            case R.id.tomorrow_btn:
                mActionLog.addAction(mActionTag+ActionLog.DianyingTomorrow);
                if (Changci.OPTION_DAY_TOMORROW == mYingxun.getChangciOption()) {
                    mYingxun.setChangciOption(0);
                } else {
                    mYingxun.setChangciOption(Changci.OPTION_DAY_TOMORROW);
                }
                refreshShowTime();
                break;
            case R.id.today_btn:
                mActionLog.addAction(mActionTag+ActionLog.DianyingToday);
                if (Changci.OPTION_DAY_TODAY == mYingxun.getChangciOption()) {
                    mYingxun.setChangciOption(0);
                } else {
                    mYingxun.setChangciOption(Changci.OPTION_DAY_TODAY);
                }
                refreshShowTime();
                break;    
                
            case R.id.nearby_fendian_view:        
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailBranch);   	
            	mSphinx.getDiscoverChildListFragment().setup(mDianying, mNearbyFendianTxv.getText().toString(), ActionLog.YingxunList);
                mSphinx.showView(R.id.view_discover_child_list);
                break;
            case R.id.distance_btn:
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailDistance);
                /* 交通界面的显示 */
                mSphinx.getTrafficQueryFragment().setData(mYingxun.getPOI(POI.SOURCE_TYPE_DIANYING,mDianying));
                mSphinx.showView(R.id.view_traffic_query);
                break;    

        }
    }
    
    public void viewMap() {
        List<POI> list = new ArrayList<POI>();
        POI poi = mYingxun.getPOI(POI.SOURCE_TYPE_DIANYING, mDianying);
        list.add(poi);
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.dianying_ditu), ActionLog.MapDianyingXiangqing);
        mSphinx.showView(R.id.view_result_map);
    }    

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        if (BaseActivity.checkReLogin(dataOperation, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, false, this, false)) {
            return;
        }

        final Response response = dataOperation.getResponse();
        
        DianyingQueryResponse targetResponse = (DianyingQueryResponse) response;
        Dianying target = targetResponse.getDianying();
        if (target != null && dataOperation.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_UID).equals(mDianying.getUid())) {
            try {
                mDianying.init(target.getData());
                refreshDescription(false);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

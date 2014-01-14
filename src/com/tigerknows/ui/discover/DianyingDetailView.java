/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;


import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.DataOperation.YingxunQueryResponse;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class DianyingDetailView extends BaseDetailView implements View.OnClickListener {
    
    private ImageView mPictureImv = null;

    private TextView mNameTxt = null;
    
    private RatingBar mRankRtb;
    
    private TextView mDirectorTxv;

    private TextView mMainActorTxv = null;

    private TextView mTagTxv = null;

    private TextView mCountryTxv = null;

    private TextView mLengthTxv;

    private TextView mStartTimeTxv;
    
    private View mNearbyFendianView;

    private TextView mNearbyFendianTxv;
    
    private View mFendianNameView = null;
    
    private TextView mFendianNameTxv = null;
    
    private TextView mDistanceTxv = null;
    
    private TextView mAddressTxv = null;
    
    private TextView mTelephoneTxv = null;
    
    private Button mTodayBtn = null;
    
    private Button mTomorrowBtn = null;
    
    private Button mAfterTomorrowBtn = null;

    //private CollapseTextView mDetailMessageTxv;
    private TextView mDescriptionTxv;
    
    private Dianying mData;
    
    private String mFilterArea;
    
    private Yingxun mYingxun;
    
    private LinearLayout mChangciListView;

    private ImageView mShowTimeDividerImv;
    
    private ViewGroup mShowTimeView;
    
    private View mAddressView = null;
    
    private View mTelephoneView = null;
    
    private View mNotimeView = null;
    
    public DianyingDetailView(Sphinx sphinx, DianyingDetailFragment parentFragment) {
        super(sphinx, parentFragment, R.layout.discover_dianying_detail);
        findViews();
        mActionTag = ActionLog.DianyingDetail;
        
        mActualLoadedDrawableRun = new Runnable() {
            
            @Override
            public void run() {
                Dianying data = mData;
                if (data == null) {
                    return;
                }
                Drawable drawable = data.getPicturesDetail().loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
                if(drawable != null) {
                    mPictureImv.setImageDrawable(drawable);
                }
            }
        };
        
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mData = null;
        mPictureImv.setImageDrawable(null);
    }
    
    @Override
    public void setData(BaseData data, int position) {
        super.setData(data, position);
        if (data == null || (data instanceof Dianying) == false || mData==data) {
            return;
        }
        mData = (Dianying)data;
        
        mFilterArea = mData.getFilterArea();
        
        mNameTxt.setText(mData.getName());
        //mAliasTxv.setText(String.valueOf(mDianying.getAlias()));
        mRankRtb.setProgress((int) mData.getRank());
        
        if (TextUtils.isEmpty(mData.getDirector()) == false) {
            //mDirectorTxv.setText(getString(R.string.dianying_detail_director,mDianying.getDirector()));
            mDirectorTxv.setVisibility(View.VISIBLE);
            String colortxt = getString(R.string.dianying_detail_director,mData.getDirector());
            SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_DARK),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);  
            mDirectorTxv.setText(style);
        }
        else
            mDirectorTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mData.getMainActor()) == false) {
            //mMainActorTxv.setText(getString(R.string.dianying_detail_main_actor,mDianying.getMainActor()));
            mMainActorTxv.setVisibility(View.VISIBLE);
            String colortxt = getString(R.string.dianying_detail_main_actor,mData.getMainActor());
            SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_DARK),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mMainActorTxv.setText(style);
        }
        else
            mMainActorTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mData.getTag()) == false) {
            //mTagTxv.setText(getString(R.string.dianying_detail_tag,mDianying.getTag()));
            mTagTxv.setVisibility(View.VISIBLE);
            String colortxt = getString(R.string.dianying_detail_tag,mData.getTag());
            SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_DARK),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mTagTxv.setText(style);
        }
        else
            mTagTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mData.getCountry()) == false) {
            //mCountryTxv.setText(getString(R.string.dianying_detail_country,mDianying.getCountry()));
            mCountryTxv.setVisibility(View.VISIBLE);
            String colortxt = getString(R.string.dianying_detail_country,mData.getCountry());
            SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_DARK),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mCountryTxv.setText(style);
        }
        else
            mCountryTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mData.getLength()) == false) {
            //mLengthTxv.setText(getString(R.string.dianying_detail_length,mDianying.getLength()));
            mLengthTxv.setVisibility(View.VISIBLE);
            String colortxt = getString(R.string.dianying_detail_length,mData.getLength());
            SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_DARK),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mLengthTxv.setText(style);
        }
        else
            mLengthTxv.setVisibility(View.GONE);
        
        if (TextUtils.isEmpty(mData.getStartTime()) == false) {
            //mStartTimeTxv.setText(getString(R.string.dianying_detail_start_time,mDianying.getStartTime()));
            mStartTimeTxv.setVisibility(View.VISIBLE);
            String colortxt = getString(R.string.dianying_detail_start_time,mData.getStartTime());
            SpannableStringBuilder style=new SpannableStringBuilder(colortxt);  
            style.setSpan(new ForegroundColorSpan(TKConfig.COLOR_BLACK_DARK),3,colortxt.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mStartTimeTxv.setText(style);
        }
        else
            mStartTimeTxv.setVisibility(View.GONE);
        if (TextUtils.isEmpty(mFilterArea) || mData.getNum() < 2) {
            mNearbyFendianTxv.setVisibility(View.GONE);
            mNearbyFendianView.setClickable(false);
        } else {
            mNearbyFendianTxv.setText(getString(R.string.dianying_detail_nearby,mFilterArea,mData.getNum()));
            mNearbyFendianTxv.setVisibility(View.VISIBLE);
            mNearbyFendianView.setClickable(true);
        }
        
        refreshDescription(true);
        
        mYingxun = mData.getYingxun();
        DiscoverChildListFragment.showPOI(mSphinx, mYingxun.getName(), mYingxun.getDistance(), mYingxun.getAddress(), mYingxun.getPhone(), 
                mFendianNameTxv, mDistanceTxv, mAddressView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_middle, R.drawable.list_middle, R.drawable.list_middle);
        
        mNotimeView.setVisibility(View.GONE);
        refreshShowTime();
        refreshDrawable();
    }
    
    @Override
    protected void refreshDrawable() {
        super.refreshDrawable();
        if (mData == null) {
            return;
        }
        TKDrawable tkDrawable = mData.getPicturesDetail();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
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
        DiscoverChildListFragment.makeChangciView(mYingxun, mSphinx, mSphinx.getLayoutInflater(), mChangciListView);
        DiscoverChildListFragment.refreshDayView(mYingxun, mTodayBtn, mTomorrowBtn, mAfterTomorrowBtn, mShowTimeDividerImv, mNotimeView);
    }
    
    @Override
    protected void refreshDescription(boolean query) {
        super.refreshDescription(query);
        if (mData == null) {
            return;
        }
        String str = mData.getDescription();
        if (!TextUtils.isEmpty(str)) {
            mDescriptionTxv.setText(str);
            mDescriptionTxv.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        } else if (query){
        	if (!mAsyncTaskExecuting) {
        		mDescriptionTxv.setVisibility(View.GONE);
        		mLoadingView.setVisibility(View.VISIBLE);
        		DataOperation dataOperation = new DataOperation(mSphinx);
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_DIANYING);
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, mData.getUid());
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
        		dataOperation.setup(mParentFragment.getId(), mParentFragment.getId(), null, true);
        		mTKAsyncTasking = mSphinx.queryStart(dataOperation);
        		mAsyncTaskExecuting = true;
        		mBaseQuerying = mTKAsyncTasking.getBaseQueryList();
			}
        }
    }
    
    @Override
    protected void findViews() {
        super.findViews();
        mPictureImv = (ImageView) findViewById(R.id.picture_imv);
        mNameTxt = (TextView)findViewById(R.id.name_txv);
        //mAliasTxv = (TextView)findViewById(R.id.alias_txv);
        mRankRtb = (RatingBar) findViewById(R.id.stars_rtb);
        mDirectorTxv = (TextView)findViewById(R.id.director_txv);
        mMainActorTxv = (TextView) findViewById(R.id.main_actor_txv);
        mTagTxv = (TextView) findViewById(R.id.tag_txv);
        mCountryTxv = (TextView) findViewById(R.id.country_txv);
        mLengthTxv = (TextView)findViewById(R.id.length_txv);
        mStartTimeTxv = (TextView)findViewById(R.id.start_time_txv);
        //mToadyTxv = (Button)findViewById(R.id.today_txv);
        
        View view =  findViewById(R.id.dianying_fendian_list_item);
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
        mTelephoneView = view.findViewById(R.id.telephone_view);
        mFendianNameView = view.findViewById(R.id.name_view);
        mFendianNameView.setBackgroundResource(R.drawable.list_middle);
        view.setPadding(0, 0, 0, 0);
        view.findViewById(R.id.tuangou_fendian_list_item).setPadding(0, 0, 0, 0);
        mFendianNameTxv = (TextView) view.findViewById(R.id.name_txv);
        mDistanceTxv = (TextView)view.findViewById(R.id.distance_txv);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mNearbyFendianView = findViewById(R.id.nearby_fendian_view);
        mNearbyFendianTxv = (TextView)findViewById(R.id.nearby_fendian_txv);
        mDescriptionTxv = (TextView) findViewById(R.id.description_txv);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mNearbyFendianView.setOnClickListener(this);
        mChangciListView.setOnClickListener(this);
        mTodayBtn.setOnClickListener(this);
        mTomorrowBtn.setOnClickListener(this);
        mAfterTomorrowBtn.setOnClickListener(this);
        
        mAddressView.setOnClickListener(this);
        mTelephoneView.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.telephone_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonTelphone);
                Utility.telephone(mSphinx, mTelephoneTxv);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);
                Utility.queryTraffic(mSphinx, mYingxun.getPOI(POI.SOURCE_TYPE_DIANYING), mActionTag);
                break;
                
            case R.id.after_tomorrow_btn:
                mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonDianyingAfterTomorrow);
                if (Changci.OPTION_DAY_AFTER_TOMORROW == mYingxun.getChangciOption()) {
                    mYingxun.setChangciOption(0);
                } else {
                    mYingxun.setChangciOption(Changci.OPTION_DAY_AFTER_TOMORROW);
                }
                refreshShowTime();
                break;
            case R.id.tomorrow_btn:
                mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonDianyingTomorrow);
                if (Changci.OPTION_DAY_TOMORROW == mYingxun.getChangciOption()) {
                    mYingxun.setChangciOption(0);
                } else {
                    mYingxun.setChangciOption(Changci.OPTION_DAY_TOMORROW);
                }
                refreshShowTime();
                break;
            case R.id.today_btn:
                mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonDianyingToday);
                if (Changci.OPTION_DAY_TODAY == mYingxun.getChangciOption()) {
                    mYingxun.setChangciOption(0);
                } else {
                    mYingxun.setChangciOption(Changci.OPTION_DAY_TODAY);
                }
                refreshShowTime();
                break;    
                
            case R.id.nearby_fendian_view:        
                mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonBranch);       
                mSphinx.getDiscoverChildListFragment().setup(mData, mNearbyFendianTxv.getText().toString(), ActionLog.YingxunList);
                mSphinx.showView(R.id.view_discover_child_list);
                break;

        }
    }

    @Override
    public boolean onPostExecute(TKAsyncTask tkAsyncTask) {
        if (super.onPostExecute(tkAsyncTask) == false) {
            return false;
        }
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        boolean isPulledDynamicPOIRequest = dataOperation.isPulledDynamicPOIRequest();

        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        for (BaseQuery baseQuery : baseQueryList) {

            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.mCancelLoginListener, super.mParentFragment.mViewPager.getCurrentItem()==mPosition)) {
                mParentFragment.isReLogin = true;
                return true;
            } else {
                if (isPulledDynamicPOIRequest) {
                    if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_ERROR_MSG_TOAST, mParentFragment, true)) {
                        return true;
                    }
                } else {
                    if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_ERROR_MSG_NO, mParentFragment, false)) {
                        return true;
                    }
                }
            }
        }

        final Response response = dataOperation.getResponse();
        
        if (isPulledDynamicPOIRequest) {
            Dianying dianying = null;
            
            for (BaseQuery baseQuery : baseQueryList) {
                String dataType = baseQuery.getParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE);
                if(BaseQuery.DATA_TYPE_DIANYING.equals(dataType)){
                    // Dianying query response
                    DianyingQueryResponse dianyingQueryResponse = (DianyingQueryResponse) baseQuery.getResponse();
                    dianying = dianyingQueryResponse.getDianying();
                }else{
                    // Yinxun query repsonse
                    YingxunQueryResponse yingxunQueryResponse = (YingxunQueryResponse) baseQuery.getResponse();
                    Yingxun yingxun = yingxunQueryResponse.getYingxun();
                    dianying.setYingxun(yingxun);
                    List<Dianying> list = new ArrayList<Dianying>();
                    list.add(dianying);
                    mSphinx.getDianyingDetailFragment().setData(list, mParentFragment.position, null);
                }
                
            }
        } else {
            DianyingQueryResponse targetResponse = (DianyingQueryResponse) response;
            Dianying target = targetResponse.getDianying();
            if (target != null && dataOperation.getParameter(DataOperation.SERVER_PARAMETER_DATA_UID).equals(mData.getUid())) {
                try {
                    mData.init(target.getData(), false);
                    refreshDescription(false);
                } catch (APIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public void setPulledDynamicPOI(PulledDynamicPOI dynamicPOI) {
        if(dynamicPOI==null || dynamicPOI.getMasterUID()==null
                || dynamicPOI.getMasterType()==0){
            return;
        }

        mParentFragment.setViewsVisibility(View.INVISIBLE);
        
        // Set up the master poi query
        DataOperation dataOperation = new DataOperation(mSphinx);
        dataOperation.addParameter(BaseQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, BaseQuery.REQUSET_SOURCE_TYPE_PULLED_DYNAMIC_POI);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_DIANYING);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getMasterUID());
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                Dianying.NEED_FIELD_ONLY_DIANYING + Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
                Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                Util.byteToHexString(Dianying.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
        dataOperation.addLocalParameter(BaseQuery.RESPONSE_CODE_ERROR_MSG_PREFIX + 410, ""+R.string.response_code_410_pulled);
        dataOperation.setup(mParentFragment.getId(), mParentFragment.getId(), getString(R.string.doing_and_wait));
        List<BaseQuery> list = new ArrayList<BaseQuery>();
        list.add(dataOperation);

        // Set up the slave poi query
        dataOperation = new DataOperation(mSphinx);
        dataOperation.addParameter(BaseQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, BaseQuery.REQUSET_SOURCE_TYPE_PULLED_DYNAMIC_POI);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YINGXUN);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getSlaveUID());
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, Yingxun.NEED_FIELD);
        dataOperation.setup(mParentFragment.getId(), mParentFragment.getId(), getString(R.string.doing_and_wait));
        list.add(dataOperation);

        //Start the query
        mTKAsyncTasking = mSphinx.queryStart(list);
        mAsyncTaskExecuting = true;
        mBaseQuerying = mTKAsyncTasking.getBaseQueryList();
    }
}

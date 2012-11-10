/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.BaseFragment;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class YanchuDetailFragment extends DiscoverBaseFragment implements View.OnClickListener {
    
    public YanchuDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private ScrollView mBodyScv;
    
    private ImageView mPictureImv = null;

    private TextView mNameTxt = null;

    private RatingBar mStartsRtb;
    
    private TextView mDateTxv;
    
    private TextView mPriceTxv;

    private TextView mFendianNameTxv = null;
    
    private Button mDistanceBtn = null;
    
    private View mAddressView = null;
    
    private View mTelephoneView = null;
    
    private TextView mAddressTxv = null;
    
    private View mDividerView;
    
    private TextView mTelephoneTxv = null;
    
    private TextView mDescriptionTitle;
    
    private TextView mDescriptionTxv;
    
    private ImageView mPriceImv = null;
    
    private View mLoadingView;
    
    private Yanchu mYanchu;
    
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
            Drawable drawable = mYanchu.getPicturesDetail().loadDrawable(null, null, null);
            if(drawable != null) {
                mPictureImv.setImageDrawable(drawable);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.YanchuXiangqing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        //mRootView = mLayoutInflater.inflate(R.layout.poi_detail, container, false);
        mRootView = mLayoutInflater.inflate(R.layout.yanchu_detail, container, false);
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleTxv.setText(R.string.yanchu_detail);
        mRightBtn.setImageResource(R.drawable.ic_view_map);
        mRightTxv.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightTxv.setOnClickListener(this);   
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
    
    public void setData(Yanchu yanchu) {
        mYanchu = yanchu;
        if (mYanchu == null) {
            return;
        }
        
        mNameTxt.setText(mYanchu.getName());
        mStartsRtb.setProgress((int) mYanchu.getHot());
        mDateTxv.setText(mYanchu.getTimeDesc());
        mDescriptionTitle.setText(R.string.yanchu_detail_introduce);
        if (!TextUtils.isEmpty(mYanchu.getPrice())) {
        	mPriceImv.setVisibility(View.VISIBLE);
        	mPriceTxv.setVisibility(View.VISIBLE);
        	mPriceTxv.setText(String.valueOf(mYanchu.getPrice()));
        } else {
        	mPriceImv.setVisibility(View.GONE);
        	mPriceTxv.setVisibility(View.GONE);
        }

        String telephoneTitle = null;
        String telephone = null;
        if (mYanchu != null) {
            telephone = mYanchu.getContactTel();
            telephoneTitle = mSphinx.getString(R.string.lianxidianhua);
            if (TextUtils.isEmpty(telephone)) {
                telephone = mYanchu.getOrderTel();
                telephoneTitle = mSphinx.getString(R.string.dingpiaozixun);
            }
        }
        String name = mYanchu.getPlaceName();
        DiscoverChildListFragment.showPOI(mSphinx, 0, TextUtils.isEmpty(name) ? mContext.getString(R.string.yanchu_didian) : name, mYanchu.getDistance(), mYanchu.getAddress(), telephone, 
                mFendianNameTxv, mDistanceBtn, mAddressView, mDividerView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_header, R.drawable.list_footer, R.drawable.list_single, mSphinx.getString(R.string.xiangxidizhi), telephoneTitle);
        
        refreshDescription(true);
        refreshDrawable();
    }
    
    private void refreshDrawable() {
        if (mYanchu == null) {
            return;
        }
        TKDrawable tkDrawable = mYanchu.getPicturesDetail();
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
    
    private void refreshDescription(boolean query) {
        if (mYanchu == null) {
            return;
        }
        String str = mYanchu.getDescription();
        if (!TextUtils.isEmpty(str)) {
            mDescriptionTxv.setText(str);
            mDescriptionTxv.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        } else if (query){
            mDescriptionTxv.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.VISIBLE);
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_YANCHU);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mYanchu.getUid());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
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
        mStartsRtb = (RatingBar) mRootView.findViewById(R.id.stars_rtb);
        mDateTxv = (TextView)mRootView.findViewById(R.id.date_txv);
        mPriceImv = (ImageView)mRootView.findViewById(R.id.price_imv);
        mPriceTxv = (TextView)mRootView.findViewById(R.id.price_txv);
        
        View view =  mRootView.findViewById(R.id.tuangou_fendian_list_item);
        
        mFendianNameTxv = (TextView) view.findViewById(R.id.name_txv);
        mDistanceBtn = (Button)view.findViewById(R.id.distance_btn);
        mAddressView = view.findViewById(R.id.address_view);
        mTelephoneView = view.findViewById(R.id.telephone_view);    
        mDividerView = view.findViewById(R.id.divider_imv);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mDescriptionTitle = (TextView)mRootView.findViewById(R.id.description_title);
        mDescriptionTxv = (TextView)mRootView.findViewById(R.id.description_txv);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
    }

    protected void setListener() {
        mDistanceBtn.setOnClickListener(this);
        //mAddressTxv.setOnClickListener(this);
       // mTelephoneTxv.setOnClickListener(this);      
        mAddressView.setOnClickListener(this);
        mTelephoneTxv.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_txv:
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
                
            case R.id.distance_btn:
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailDistance);
                /* 交通界面的显示 */
                POI poi = mYanchu.getPOI();
                poi.setName(mYanchu.getPlaceName());
                poi.setAddress(mYanchu.getAddress());
                mSphinx.getTrafficQueryFragment().setData(poi);
                mSphinx.showView(R.id.view_traffic_query);
                break;
        }
    }
    
    public void viewMap() {
        List<POI> list = new ArrayList<POI>();
        POI poi = mYanchu.getPOI();
        poi.setName(mYanchu.getPlaceName());
        poi.setAddress(mYanchu.getAddress());
        list.add(poi);
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.yanchu_didian_ditu), ActionLog.MapYanchuXiangqing);
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
        YanchuQueryResponse targetResponse = (YanchuQueryResponse) response;
        Yanchu target = targetResponse.getYanchu();
        if (target != null && dataOperation.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_UID).equals(mYanchu.getUid())) {
            try {
                mYanchu.init(target.getData());
                refreshDescription(false);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

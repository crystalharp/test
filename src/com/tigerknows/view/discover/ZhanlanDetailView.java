/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.Hashtable;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class ZhanlanDetailView extends BaseDetailView implements View.OnClickListener {
    
    private ImageView mPictureImv = null;

    private TextView mNameTxt = null;

    private RatingBar mStartsRtb;
    
    private TextView mDateTxv;
    
    private TextView mPriceTxv;

    private TextView mFendianNameTxv = null;
    
    private TextView mDistanceTxv = null;
    
    private View mAddressView = null;
    
    private View mTelephoneView = null;
    
    private TextView mAddressTxv = null;
    
    private View mDividerView;
    
    private TextView mTelephoneTxv = null;
    
    private TextView mDescriptionTitle;
    
    private TextView mDescriptionTxv;
    
    private ImageView mPriceImv = null;
    
    private Zhanlan mData;
    
    public ZhanlanDetailView(Sphinx sphinx, ZhanlanDetailFragment parentFragment) {
        super(sphinx, parentFragment, R.layout.yanchu_detail);
        findViews();
        mActionTag = ActionLog.ZhanlanXiangqing;
        
        mActualLoadedDrawableRun = new Runnable() {
            
            @Override
            public void run() {
                Drawable drawable = mData.getPicturesDetail().loadDrawable(null, null, null);
                if(drawable != null) {
                    mPictureImv.setImageDrawable(drawable);
                }
            }
        };
        
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mPictureImv.setImageDrawable(null);
    }
    
    @Override
    public void setData(BaseData data) {
        super.setData(data);
        if (data == null || (data instanceof Zhanlan) == false) {
            return;
        }
        mData = (Zhanlan)data;
        
        mNameTxt.setText(mData.getName());
        mStartsRtb.setProgress((int) mData.getHot());
        mDateTxv.setText(mData.getTimeDesc());
        mDescriptionTitle.setText(R.string.zhanlan_detail_introduce);
        if (!TextUtils.isEmpty(mData.getPrice())) {
        	mPriceImv.setVisibility(View.VISIBLE);
        	mPriceTxv.setVisibility(View.VISIBLE);
        	mPriceTxv.setText(String.valueOf(mData.getPrice()));
        } else {
        	mPriceImv.setVisibility(View.GONE);
        	mPriceTxv.setVisibility(View.GONE);
        }
       
        String name = mData.getPlaceName();
        DiscoverChildListFragment.showPOI(mSphinx, 0, TextUtils.isEmpty(name) ? mSphinx.getString(R.string.zhanlan_didian) : name, mData.getDistance(), mData.getAddress(), mData.getContactTel(), 
                mFendianNameTxv, mDistanceTxv, mAddressView, mDividerView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_middle, R.drawable.list_footer, R.drawable.list_footer, mSphinx.getString(R.string.xiangxidizhi), mSphinx.getString(R.string.lianxidianhua));
        
        refreshDescription(true);
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
            mDescriptionTxv.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.VISIBLE);
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_ZHANLAN);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mData.getUid());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Util.byteToHexString(Zhanlan.FIELD_DESCRIPTION));
            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mParentFragment.getId(), mParentFragment.getId(), null, true);
            mBaseQuerying = dataOperation;
            mSphinx.queryStart(dataOperation);
        }
    }
    
    @Override
    protected void findViews() {
        super.findViews();
        mPictureImv = (ImageView) findViewById(R.id.picture_imv);
        mNameTxt = (TextView)findViewById(R.id.name_txv);
        mStartsRtb = (RatingBar) findViewById(R.id.stars_rtb);
        mDateTxv = (TextView)findViewById(R.id.date_txv);
        mPriceImv = (ImageView)findViewById(R.id.price_imv);
        mPriceTxv = (TextView)findViewById(R.id.price_txv);
        
        View view =  findViewById(R.id.tuangou_fendian_list_item);
        
        mFendianNameTxv = (TextView) view.findViewById(R.id.name_txv);
        mDistanceTxv = (TextView)view.findViewById(R.id.distance_txv);
        mAddressView = view.findViewById(R.id.address_view);
        mTelephoneView = view.findViewById(R.id.telephone_view);    
        mDividerView = view.findViewById(R.id.divider_imv);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mDescriptionTitle = (TextView)findViewById(R.id.description_title);
        mDescriptionTxv = (TextView)findViewById(R.id.description_txv);
    }

    @Override
    protected void setListener() {
        super.setListener();
        //mAddressTxv.setOnClickListener(this);
       // mTelephoneTxv.setOnClickListener(this);      
        mAddressView.setOnClickListener(this);
        mTelephoneView.setOnClickListener(this);
        mBodyScv.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mParentFragment.updateNextPrevControls();
                    mParentFragment.scheduleDismissOnScreenControls();
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.telephone_view:
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailTelphone);
                CommonUtils.telephone(mSphinx, mTelephoneTxv);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(mActionTag+ActionLog.DiscoverDetailAddress);
                POI poi = mData.getPOI();
                poi.setName(mData.getPlaceName());
                poi.setAddress(mData.getAddress());
                CommonUtils.queryTraffic(mSphinx, poi);
                break;
        }
    }
    
    @Override
    public boolean onPostExecute(TKAsyncTask tkAsyncTask) {
        if (super.onPostExecute(tkAsyncTask) == false) {
            return false;
        }
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        if (BaseActivity.checkReLogin(dataOperation, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.mCancelLoginListener)) {
            mParentFragment.isReLogin = true;
            return true;
        } else if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, false, mParentFragment, false)) {
            return true;
        }

        final Response response = dataOperation.getResponse();
        ZhanlanQueryResponse targetResponse = (ZhanlanQueryResponse) response;
        Zhanlan data = targetResponse.getZhanlan();
        if (data != null && dataOperation.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_UID).equals(mData.getUid())) {
            try {
                mData.init(data.getData());
                refreshDescription(false);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return true;
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;


import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
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
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class YanchuDetailView extends BaseDetailView implements View.OnClickListener {
    
    private ImageView mPictureImv = null;

    private TextView mNameTxt = null;

    private RatingBar mStartsRtb;
    
    private TextView mDateTxv;
    
    private TextView mPriceTxv;

    private TextView mFendianNameTxv = null;
    
    private TextView mDistanceTxv = null;
    
    private TextView mDistanceFromTxv = null;
    
    private View mAddressView = null;
    
    private View mTelephoneView = null;
    
    private TextView mAddressTxv = null;
    
    private TextView mTelephoneTxv = null;
    
    private TextView mDescriptionTitle;
    
    private TextView mDescriptionTxv;
    
    private ImageView mPriceImv = null;
    
    private Yanchu mData;
    
    public YanchuDetailView(Sphinx sphinx, YanchuDetailFragment parentFragment) {
        super(sphinx, parentFragment, R.layout.discover_yanchu_detail);
        findViews();
        mActionTag = ActionLog.YanchuDetail;
        
        mActualLoadedDrawableRun = new Runnable() {
            
            @Override
            public void run() {
                Yanchu data = mData;
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
        if (data == null || (data instanceof Yanchu) == false || mData == data) {
            return;
        }
        mData = (Yanchu)data;
        
        mNameTxt.setText(mData.getName());
        mStartsRtb.setProgress((int) mData.getHot());
        mDateTxv.setText(mData.getTimeDesc());
        mDescriptionTitle.setText(R.string.yanchu_detail_introduce);
        if (!TextUtils.isEmpty(mData.getPrice())) {
            mPriceImv.setVisibility(View.VISIBLE);
            mPriceTxv.setVisibility(View.VISIBLE);
            mPriceTxv.setText(String.valueOf(mData.getPrice()));
        } else {
            mPriceImv.setVisibility(View.GONE);
            mPriceTxv.setVisibility(View.GONE);
        }

        String telephoneTitle = null;
        String telephone = null;
        if (mData != null) {
            telephone = mData.getContactTel();
            telephoneTitle = getString(R.string.lianxidianhua);
            if (TextUtils.isEmpty(telephone)) {
                telephone = mData.getOrderTel();
                telephoneTitle = getString(R.string.dingpiaozixun);
            }
        }
        String name = mData.getPlaceName();
        DiscoverChildListFragment.showPOI(mSphinx, 0, TextUtils.isEmpty(name) ? getString(R.string.yanchu_didian) : name, mData.getDistance(), mData.getAddress(), telephone, 
                mFendianNameTxv, mDistanceFromTxv, mDistanceTxv, mAddressView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_middle, R.drawable.list_footer, R.drawable.list_footer, getString(R.string.xiangxidizhi), telephoneTitle);
        
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
        	if(!mAsyncTaskExecuting){
        		mDescriptionTxv.setVisibility(View.GONE);
        		mLoadingView.setVisibility(View.VISIBLE);
        		DataOperation dataOperation = new DataOperation(mSphinx);
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_YANCHU);
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, mData.getUid());
        		dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
        		dataOperation.setup(mParentFragment.getId(), mParentFragment.getId(), null, true);
        		mAsyncTaskExecuting = true;
        		mTKAsyncTasking = mSphinx.queryStart(dataOperation);
        		mBaseQuerying = mTKAsyncTasking.getBaseQueryList();
        	}
        }
    }
    
    public void setPulledDynamicPOI(PulledDynamicPOI dynamicPOI){
        
        if(dynamicPOI==null || dynamicPOI.getMasterUID()==null
                || dynamicPOI.getMasterType()==0){
            return;
        }

        mParentFragment.setViewsVisibility(View.INVISIBLE);
        
        DataOperation dataOperation = new DataOperation(mSphinx);
        dataOperation.addParameter(BaseQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, BaseQuery.REQUSET_SOURCE_TYPE_PULLED_DYNAMIC_POI);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YANCHU);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getMasterUID());
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                Yanchu.NEED_FIELD + Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
               Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
               Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
        dataOperation.addLocalParameter(BaseQuery.RESPONSE_CODE_ERROR_MSG_PREFIX + 410, ""+R.string.response_code_410_pulled);
        dataOperation.setup(mParentFragment.getId(), mParentFragment.getId(), getString(R.string.doing_and_wait));
        mTKAsyncTasking = mSphinx.queryStart(dataOperation);
        mAsyncTaskExecuting = true;
        mBaseQuerying = mTKAsyncTasking.getBaseQueryList();
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
        mDistanceFromTxv = (TextView)view.findViewById(R.id.distance_from_txv);
        mAddressView = view.findViewById(R.id.address_view);
        mTelephoneView = view.findViewById(R.id.telephone_view);    
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
                POI poi = mData.getPOI();
                poi.setName(mData.getPlaceName());
                poi.setAddress(mData.getAddress());
                Utility.queryTraffic(mSphinx, poi, mActionTag);
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
        if (BaseActivity.checkReLogin(dataOperation, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.mCancelLoginListener, super.mParentFragment.mViewPager.getCurrentItem()==mPosition)) {
            mParentFragment.isReLogin = true;
            return true;
        } else {
            if (isPulledDynamicPOIRequest) {
                if (BaseActivity.hasAbnormalResponseCode(dataOperation, mSphinx, BaseActivity.SHOW_TOAST, mParentFragment, true)) {
                    return true;
                }
            } else {
                if (BaseActivity.hasAbnormalResponseCode(dataOperation, mSphinx, BaseActivity.SHOW_NOTHING, mParentFragment, false)) {
                    return true;
                }
            }
        }

        final Response response = dataOperation.getResponse();
        YanchuQueryResponse targetResponse = (YanchuQueryResponse) response;
        Yanchu target = targetResponse.getYanchu();
        if (isPulledDynamicPOIRequest) {
            if (target != null) {
                List<Yanchu> list = new ArrayList<Yanchu>();
                list.add(target);
                ((YanchuDetailFragment) mParentFragment).setData(list, mParentFragment.position, null);
            } else {
                mParentFragment.dismiss();
            }
        } else {
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
}

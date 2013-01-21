/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Hashtable;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Dingdan;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.DataOperation.DingdanCreateResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.user.User;
import com.tigerknows.view.user.UserBaseActivity;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class TuangouDetailView extends BaseDetailView implements View.OnClickListener {
    
    private ImageView mPictureImv = null;
    
    private ImageView mShangjiaMarkerImv;
    
    
    private View mBarView = null;
    
    private TextView mPriceTxv;
    
    private TextView mOrgPriceTxv;
    
    private TextView mDiscountTxv;

    private Button mBuyBtn = null;

    
    private View mBarView_2 = null;
    
    private TextView mPriceTxv_2;
    
    private TextView mOrgPriceTxv_2;
    
    private TextView mDiscountTxv_2;

    private Button mBuyBtn_2 = null;
    
    private TextView mNameTxt = null;
    
    private ImageView mRefundImv = null;

    private TextView mRefundTxv = null;

    private TextView mBuyerNumTxv = null;
    
    private View mFendianNameView = null;
    
    private TextView mFendianNameTxv = null;
    
    private TextView mDistanceTxv = null;
    
    private TextView mAddressTxv = null;
    
    private TextView mTelephoneTxv = null;
    
    private View mAddressView = null;
    
    private View mDividerView;
    
    private View mTelephoneView = null;
    
    private View mNearbyFendianView = null;
    
    private TextView mNearbyFendianTxv = null;
    
    private View mContentView;
    
    private TextView mContentTxv;
    
    private View mNoticedView;

    private TextView mNoticedTxv;

    private View mServiceHotlineView;

    private TextView mServiceHotlineTitleTxv;

    private TextView mServiceHotlineTxv;
    
    private Tuangou mData;
    
    private String mFilterArea;
    
    private String noRefundStr;
    
    private int mPictureDetailWidth;
    
	int[] locationScv = new int[]{0, 1};
	int[] locationBar = new int[]{0, 2};
	  
    protected DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            if (mBaseQuerying != null && BaseQuery.API_TYPE_DATA_OPERATION.equals(mBaseQuerying.getAPIType())) {
                Hashtable<String, String> criteria = mBaseQuerying.getCriteria();
                if (criteria != null && criteria.containsKey(BaseQuery.SERVER_PARAMETER_DATA_TYPE)) {
                    String dataType = criteria.get(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
                    if (DataOperation.DATA_TYPE_DINGDAN.equals(dataType)) {
                        mBaseQuerying = null;
                    }
                }
            }
            mParentFragment.isReLogin();
        }
    };
    
    public TuangouDetailView(Sphinx sphinx, TuangouDetailFragment parentFragment) {
        super(sphinx, parentFragment, R.layout.tuangou_detail);
        
        findViews();
        mActionTag = ActionLog.TuangouXiangqing;
        
        noRefundStr = mSphinx.getString(R.string.tuangou_no_refund);

        int width = (int)(Globals.g_metrics.widthPixels);
        int height = (int) (width*((float)168/276));
        ViewGroup.LayoutParams layoutParams;
        layoutParams = mPictureImv.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        
        mPictureDetailWidth = (int)(Globals.g_metrics.widthPixels);
        
        mActualLoadedDrawableRun = new Runnable() {
            
            @Override
            public void run() {
                Drawable drawable = mData.getPicturesDetail().loadDrawable(null, null, null);
                if(drawable != null) {
                    mPictureImv.setBackgroundDrawable(drawable);
                }
                TKDrawable tkDrawable = mData.getContentPic();
                if (tkDrawable != null) {
                    drawable = tkDrawable.loadDrawable(null, null, null);
                    if(drawable != null) {
                        mContentTxv.setText(null);
                        mContentTxv.setBackgroundDrawable(drawable);
                        ViewGroup.LayoutParams layoutParams = mContentTxv.getLayoutParams();
                        layoutParams.width = mPictureDetailWidth;
                        layoutParams.height = (int) (mPictureDetailWidth*((float)drawable.getIntrinsicHeight()/drawable.getIntrinsicWidth()));
                    }
                }
                Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), null, null);
                if (shangjia != null) {
                    mShangjiaMarkerImv.setImageDrawable(shangjia.getMarker());
                } else {
                    mShangjiaMarkerImv.setImageDrawable(null);
                }
            }
        };
        
    }

    private final int FLOATING_BAR_MSG_DELAY = 100; 
    
    @Override
    public void onResume() {
        super.onResume();
        handler.sendMessageDelayed(handler.obtainMessage(), FLOATING_BAR_MSG_DELAY);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mPictureImv.setBackgroundDrawable(null);
        mContentTxv.setBackgroundDrawable(null);
    }
    
    @Override
    public void setData(BaseData data) {
        super.setData(data);
        if (data == null || (data instanceof Tuangou) == false) {
            return;
        }
        mData = (Tuangou)data;
        
        mFilterArea = mData.getFilterArea();
        
        Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
        if (shangjia != null) {
            mShangjiaMarkerImv.setImageDrawable(shangjia.getMarker());
        } else {
            mShangjiaMarkerImv.setImageDrawable(null);
        }
        
        mPriceTxv.setText(mData.getPrice()+mSphinx.getString(R.string.rmb_text));
        mOrgPriceTxv.setText(mData.getOrgPrice()+mSphinx.getString(R.string.rmb_text));
        mOrgPriceTxv.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mDiscountTxv.setText(mData.getDiscount());

        mPriceTxv_2.setText(mData.getPrice()+mSphinx.getString(R.string.rmb_text));
        mOrgPriceTxv_2.setText(mData.getOrgPrice()+mSphinx.getString(R.string.rmb_text));
        mOrgPriceTxv_2.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mDiscountTxv_2.setText(mData.getDiscount());
        
        mBuyerNumTxv.setText(mSphinx.getString(R.string.tuangou_detail_buyer_num, mData.getBuyerNum()));
        
        String refund = mData.getRefund();
        if (!TextUtils.isEmpty(refund) && !refund.contains(noRefundStr)) {
            mRefundImv.setImageResource(R.drawable.ic_is_refund);
        } else {
            mRefundImv.setImageResource(R.drawable.ic_not_refund);
        }
        mRefundTxv.setText(refund);
        
        if (TextUtils.isEmpty(mFilterArea) || mData.getBranchNum() < 2) {
            mNearbyFendianTxv.setVisibility(View.GONE);
            mNearbyFendianView.setClickable(false);
        } else {
            mNearbyFendianTxv.setText(mFilterArea + mSphinx.getString(R.string.tuangou_detail_nearby, mData.getBranchNum()));
            mNearbyFendianTxv.setVisibility(View.VISIBLE);
            mNearbyFendianView.setClickable(true);
        }

        String description = mData.getDescription();
        if (TextUtils.isEmpty(description)) {
            mNameTxt.setText(null);
        } else {
            mNameTxt.setText(description);
        }
        refreshDescription(true);

        String serviceTel = null;
        String name = null;
        if (shangjia != null) {
            serviceTel = shangjia.getServiceTel();
            name = shangjia.getName();
        }
        if (TextUtils.isEmpty(serviceTel) || TextUtils.isEmpty(name)) {
            mServiceHotlineView.setVisibility(View.GONE);
        } else {
            mServiceHotlineTitleTxv.setText(name+mSphinx.getString(R.string.service_hotline));
            mServiceHotlineTxv.setText(serviceTel);
            mServiceHotlineView.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
            mServiceHotlineView.setVisibility(View.VISIBLE);
        }
        
        refreshFendian();
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
                mPictureImv.setBackgroundDrawable(drawable);
            } else {
                mPictureImv.setBackgroundDrawable(null);
            }
        } else {
            mPictureImv.setBackgroundDrawable(null);
        }
        tkDrawable = mData.getContentPic();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
            if(drawable != null) {
                mContentTxv.setBackgroundDrawable(drawable);
                mContentTxv.setText(null);
            } else {
                mContentTxv.setBackgroundDrawable(null);
                mContentTxv.setText(R.string.loading);
            }
        } else {
            mContentTxv.setBackgroundDrawable(null);
        }
    }
    
    private void refreshFendian() {
        Fendian fendian = mData.getFendian();
        DiscoverChildListFragment.showPOI(mSphinx, fendian.getPlaceName(), fendian.getDistance(), fendian.getAddress(), fendian.getPlacePhone(), 
                mFendianNameTxv, mDistanceTxv, mAddressView, mDividerView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_middle, R.drawable.list_footer, R.drawable.list_footer);
    }
    
    @Override
    protected void refreshDescription(boolean query) {
        super.refreshDescription(query);
        if (mData == null) {
            return;
        }
        StringBuilder needFiled = new StringBuilder();   
        StringBuilder pic = new StringBuilder();   
        
        String contextText = mData.getContentText();
        ViewGroup.LayoutParams layoutParams = mContentTxv.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.FILL_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (TextUtils.isEmpty(contextText)) {
            mContentTxv.setText(null);
            mContentTxv.setBackgroundDrawable(null);
            TKDrawable tkDrawable = mData.getContentPic();
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
                if (drawable != null) {
                    mContentTxv.setBackgroundDrawable(drawable);
                    layoutParams = mContentTxv.getLayoutParams();
                    layoutParams.width = mPictureDetailWidth;
                    layoutParams.height = (int) (mPictureDetailWidth*((float)drawable.getIntrinsicHeight()/drawable.getIntrinsicWidth()));
                }
                mContentView.setVisibility(View.VISIBLE);
            } else {
                needFiled.append(Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT));
                needFiled.append(Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
                pic.append(Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
                pic.append(':');
                pic.append(Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN));
                pic.append("_[0]");
                mContentView.setVisibility(View.GONE);
            }            
        } else {
            mContentTxv.setText(contextText);
            mContentTxv.setBackgroundDrawable(null);
            mContentView.setVisibility(View.VISIBLE);
        }
        
        String noticed = mData.getNoticed();
        if (TextUtils.isEmpty(noticed)) {
            needFiled.append(Util.byteToHexString(Tuangou.FIELD_NOTICED));
            mNoticedView.setVisibility(View.GONE);
        } else {
            mNoticedTxv.setText(noticed);
            mNoticedView.setVisibility(View.VISIBLE);
        }
        
        if (needFiled.length() > 0 && query) {
            mLoadingView.setVisibility(View.VISIBLE);
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_TUANGOU);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mData.getUid());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, needFiled.toString());
            if (pic.length() > 0) {
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE, pic.toString());
            }
            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mParentFragment.getId(), mParentFragment.getId(), null, true);
            mBaseQuerying = dataOperation;
            mSphinx.queryStart(dataOperation);
        } else {
            mLoadingView.setVisibility(View.GONE);
        }
    }

	  
    @Override
    protected void findViews() {
        super.findViews();
        mPictureImv = (ImageView) findViewById(R.id.picture_imv);
        mShangjiaMarkerImv = (ImageView)findViewById(R.id.shangjia_marker_imv);

        mPriceTxv = (TextView)findViewById(R.id.price_txv);
        mBarView = findViewById(R.id.bar_view);
        mBuyBtn = (Button) findViewById(R.id.buy_btn);
        mOrgPriceTxv = (TextView)findViewById(R.id.org_price_txv);
        mDiscountTxv = (TextView) findViewById(R.id.discount_txv);
        
        mPriceTxv_2 = (TextView)findViewById(R.id.price_txv_2);
        mBarView_2 = findViewById(R.id.bar_view_2);
        mBuyBtn_2 = (Button) findViewById(R.id.buy_btn_2);
        mOrgPriceTxv_2 = (TextView)findViewById(R.id.org_price_txv_2);
        mDiscountTxv_2 = (TextView) findViewById(R.id.discount_txv_2);

        
        mNameTxt = (TextView)findViewById(R.id.name_txv);
        mRefundImv = (ImageView) findViewById(R.id.refund_imv);
        mRefundTxv = (TextView) findViewById(R.id.refund_txv);
        mBuyerNumTxv = (TextView) findViewById(R.id.buyer_num_txv);

        View view = findViewById(R.id.tuangou_fendian_list_item);
        mFendianNameView = view.findViewById(R.id.name_view);
        mFendianNameView.setBackgroundResource(R.drawable.list_middle);
        view.findViewById(R.id.tuangou_fendian_list_item).setPadding(0, 0, 0, 0);
        mFendianNameTxv = (TextView) view.findViewById(R.id.name_txv);
        mDistanceTxv = (TextView)view.findViewById(R.id.distance_txv);
        mAddressView = view.findViewById(R.id.address_view);
        mDividerView = view.findViewById(R.id.divider_imv);
        mTelephoneView = view.findViewById(R.id.telephone_view);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mNearbyFendianView = findViewById(R.id.nearby_fendian_view);
        mNearbyFendianTxv = (TextView) findViewById(R.id.nearby_fendian_txv);
        mContentView =  findViewById(R.id.content_view);
        mContentTxv = (TextView) findViewById(R.id.content_txv);
        mNoticedView = findViewById(R.id.noticed_view);
        mNoticedTxv = (TextView)findViewById(R.id.noticed_txv);
        mServiceHotlineView = findViewById(R.id.service_hotline_view);
        mServiceHotlineTxv = (TextView) findViewById(R.id.service_hotline_txv);
        mServiceHotlineTitleTxv = (TextView) findViewById(R.id.service_hotline_title_txv);
    }

    private int lastY = 0;

    private int touchEventId = R.id.view_invalid;

  	  
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        		super.handleMessage(msg);
        		if( !updateBarViewVisibility(true)){
        			
                	if (lastY != locationBar[1]) {
                		handler.sendMessageDelayed(handler.obtainMessage(touchEventId, null), FLOATING_BAR_MSG_DELAY);
                		lastY = locationBar[1];
                	}//end if
                	
        		}
             
        }
    };
	  
    @Override
    protected void setListener() {
        super.setListener();
        mBuyBtn.setOnClickListener(this);
        mBuyBtn_2.setOnClickListener(this);
        mAddressView.setOnClickListener(this);
        mTelephoneView.setOnClickListener(this);
        mNearbyFendianView.setOnClickListener(this);
        mServiceHotlineView.setOnClickListener(this);
        
        mBodyScv.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	 
            	//Deal with floating bar
	          	  if(event.getAction() == MotionEvent.ACTION_UP){
	          		  //user finger up, deal with possible animation
	          		  handler.sendMessageDelayed(handler.obtainMessage(touchEventId, null), FLOATING_BAR_MSG_DELAY);
	          	  }else{//Scroll moved
	          		  updateBarViewVisibility(true);
	          	  }
	          	  
	          	  //
	          	  if (event.getAction() == MotionEvent.ACTION_DOWN) {
	          		  mParentFragment.updateNextPrevControls();
                    mParentFragment.scheduleDismissOnScreenControls();
	          	  }
                return false;
            }
        });
        mBarView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return true;
            }
        });
    }

    private boolean updateBarViewVisibility(boolean renew){

    	if(renew){
    		//Get view position
	      	mBodyScv.getLocationInWindow(locationScv);
	      	mBarView.getLocationInWindow(locationBar);
    	}
      	
      	if(locationBar[1]<=locationScv[1]){
      		
      		//Judge original visibility to avoid unnecessary message loops
      		if(mBarView_2.getVisibility()==View.INVISIBLE){
            		mBarView_2.setVisibility(View.VISIBLE);
            		return true;
      		}
      		
      	}else{
      		if(mBarView_2.getVisibility()==View.VISIBLE){
            		mBarView_2.setVisibility(View.INVISIBLE);
            		return true;
      		}
      	}
      	
      	return false;
    	
    }
    
    @Override
    public void onClick(View view) {
        switch (view.getId()) {                     
	        case R.id.buy_btn:                  
	        case R.id.buy_btn_2:
                mActionLog.addAction(ActionLog.TuangouXiangqingBuy);
                String sessionId = Globals.g_Session_Id;
                if (TextUtils.isEmpty(sessionId)) {
                    ((TuangouDetailFragment) mParentFragment).isRequsetBuy = true;
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
                    mSphinx.showView(R.id.activity_user_login, intent);
                } else {
                    ((TuangouDetailFragment) mParentFragment).isRequsetBuy = false;
                    buy();
                }
                break;
                
            case R.id.telephone_view:
                mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailTelphone);
                CommonUtils.telephone(mSphinx, mTelephoneTxv);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailAddress);
                Fendian fendian = mData.getFendian();
                if (fendian == null) {
                    return;
                }
                CommonUtils.queryTraffic(mSphinx, fendian.getPOI(POI.SOURCE_TYPE_TUANGOU));
                break;
                
            case R.id.nearby_fendian_view:
                if (mNearbyFendianTxv.getVisibility() == View.VISIBLE) {
                    mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailBranch);
                    mSphinx.getDiscoverChildListFragment().setup(mData, mNearbyFendianTxv.getText().toString(), ActionLog.FendianList);
                    mSphinx.showView(R.id.view_discover_child_list);
                }
                break;
            case R.id.service_hotline_view:
                mActionLog.addAction(ActionLog.TuangouXiangqingCustomService);
                CommonUtils.telephone(mSphinx, mServiceHotlineTxv);
                break;
        }
    }
    
    void buy() {
        User user = Globals.g_User;
        if (user == null) {
            return;
        }
        StringBuilder s = new StringBuilder();
        try {
            s.append(Util.byteToHexString(Dingdan.FIELD_UID));
            s.append(':');
            s.append(URLEncoder.encode(mData.getGoodsId(), TKConfig.getEncoding()));
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_PNAME));
            s.append(':');
            s.append(URLEncoder.encode(mData.getName(), TKConfig.getEncoding()));
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_SJ_ID));
            s.append(':');
            s.append(mData.getSource());
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_TYPE));
            s.append(':');
            s.append(Dingdan.TYPE_TUANGOU);
        } catch (UnsupportedEncodingException e) {
            s = null;
            e.printStackTrace();
        }
        if (s != null) {
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_DINGDAN);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_CREATE);
            criteria.put(DataOperation.SERVER_PARAMETER_ENTITY, s.toString());
            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mParentFragment.getId(), mParentFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
            mBaseQuerying = dataOperation;
            mSphinx.queryStart(dataOperation);
        }
    }

    @Override
    public boolean onPostExecute(TKAsyncTask tkAsyncTask) {
        if (super.onPostExecute(tkAsyncTask) == false) {
            return false;
        }
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        if (BaseActivity.checkReLogin(dataOperation, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.getId(), mCancelLoginListener)) {
            mParentFragment.isReLogin = true;
            return true;
        }

        final Response response = dataOperation.getResponse();
        String dataType = dataOperation.getCriteria().get(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
        if (BaseQuery.DATA_TYPE_DINGDAN.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, true, mParentFragment, false)) {
                return true;
            }
            DingdanCreateResponse dingdanCreateResponse = (DingdanCreateResponse) response;
            Intent intent = new Intent(); 
            intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.buy));
            intent.putExtra(BrowserActivity.LEFT, mSphinx.getString(R.string.tuangou_detail));
            intent.putExtra(BrowserActivity.URL, dingdanCreateResponse.getUrl());
            Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
            if (shangjia != null) {
                intent.putExtra(BrowserActivity.TIP, shangjia.getName());
            }
            mSphinx.showView(R.id.activity_browser, intent);
        } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, false, mParentFragment, false)) {
                return true;
            }
            TuangouQueryResponse targetResponse = (TuangouQueryResponse) response;
            Tuangou target = targetResponse.getTuangou();
            if (target != null && dataOperation.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_UID).equals(mData.getUid())) {
                try {
                    mData.init(target.getData());
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

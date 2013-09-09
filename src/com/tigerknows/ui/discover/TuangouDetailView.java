/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
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
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
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
import com.tigerknows.model.User;
import com.tigerknows.model.DataOperation.DingdanCreateResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class TuangouDetailView extends BaseDetailView implements View.OnClickListener {
    
    private ImageView mPictureImv = null;
    
    private ImageView mShangjiaMarkerImv;
    
    /**
     * The bar view embeded in the details
     */
    private View mBarView = null;
    
    /**
     * The price textView
     */
    private TextView mPriceTxv;
    
    /**
     * The original price TextView
     */
    private TextView mOrgPriceTxv;
    
    /**
     * The discount textView
     */
    private TextView mDiscountTxv;

    /**
     * Buy buton
     */
    private Button mBuyBtn = null;
    
    /**
     * Duplicate bar view that is to be floating at the top of this detail view<br> 
     * If the top the embeded bar view become invisible 
     */
    private View mBarView_2 = null;
    
    private TextView mPriceTxv_2;
    
    private TextView mOrgPriceTxv_2;
    
    private TextView mDiscountTxv_2;

    private Button mBuyBtn_2 = null;
    
    private TextView mNameTxt = null;
    
    private View mViewurlView = null;
    
    private Drawable mRefundIcon;
    
    private Drawable mNotRefundIcon;
    
    private Drawable mUpIcon;
    
    private Drawable mDownIcon;
    
    private View mRefundView = null;

    /**
     * TextView representing whether this tuangou support give back your money
     */
    private TextView mRefundTxv = null;
    
    private TextView mRefundDetailTxv = null;

    /**
     * Presenting the numbers of customers who has bought this Tuangou
     */
    private TextView mBuyerNumTxv = null;
    
    /**
     * The name view group of the nearest fendian
     */
    private View mFendianNameView = null;
    
    /**
     * Name textView representing the name of the Fendian
     */
    private TextView mFendianNameTxv = null;
    
    /**
     * Distance at the right side of {@link mFendianNameView}
     */
    private TextView mDistanceTxv = null;
    
    /**
     * TextView representing the address
     */
    private TextView mAddressTxv = null;
    
    /**
     * TextView containing the telephones of the Fendian
     */
    private TextView mTelephoneTxv = null;
    
    /**
     * View group enclosing the {@link mAddressTxv}
     */
    private View mAddressViewGroup = null;
    
    /**
     * Divider view between Address View and telephone view
     */
    private View mDividerView;
    
    /**
     * View group containing the {@link mTelephoneTxv}
     */
    private View mTelephoneView = null;
    
    /**
     * View group containing nearByFendian stats info
     */
    private View mNearbyFendianView = null;
    
    /**
     * TextView containing the number of nearby Fendian
     */
    private TextView mNearbyFendianTxv = null;
    
    /**
     * View group containing the {@link mContentTxv}
     */
    private View mContentView;
    
    private View mContentContainerView;
    
    /**
     * TextView presenting the content
     */
    private TextView mContentTxv;
    
    /**
     * View group associated with the {@link mNoticedTxv}
     */
    private View mNoticedView;

    /**
     * TextView presenting the special notice the user need to notice 
     */
    private TextView mNoticedTxv;

    /**
     * View group containing {@link mServiceHotlineTitleTxv} and {@link mServiceHotlineTxv}
     */
    private View mServiceHotlineView;

    /**
     * TextView presenting which Tuangou source the hotline belongs to
     */
    private TextView mServiceHotlineTitleTxv;

    /**
     * Hotline number
     */
    private TextView mServiceHotlineTxv;
    
    /**
     * Tuangou data associcated this detain fragment is for
     */
    private Tuangou mData;
    
    /**
     * Filter area by which this tuangou is searched.
     */
    private String mFilterArea;
    
    /**
     * String value in the res xml of R.string.tuangou_no_refund
     */
    private String noRefundStr;
    
    /**
     * Width of pixes of the tuangou main picture
     */
    private int mPictureDetailWidth;
    
    /**
     * Location of the scroll view content
     * 0:x, 1:y
     */
    int[] locationScv = new int[]{0, 1};
    
    /**
     * Location of the the embeded bar
     * 0:x, 1:y
     */
    int[] locationBar = new int[]{0, 2};
    
    protected DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            if (mBaseQuerying != null && mBaseQuerying.size() > 0) {
                BaseQuery baseQuery = mBaseQuerying.get(0);
                if (BaseQuery.API_TYPE_DATA_OPERATION.equals(baseQuery.getAPIType())) {
                    if (baseQuery.hasParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE)) {
                        String dataType = baseQuery.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
                        if (DataOperation.DATA_TYPE_DINGDAN.equals(dataType)) {
                            mBaseQuerying = null;
                        }
                    }
                }
            }
            mParentFragment.isReLogin();
        }
    };
    
    public TuangouDetailView(Sphinx sphinx, TuangouDetailFragment parentFragment) {
        super(sphinx, parentFragment, R.layout.discover_tuangou_detail);
        
        findViews();
        mActionTag = ActionLog.TuangouDetail;
        
        noRefundStr = mSphinx.getString(R.string.tuangou_no_refund);
        Resources resources = mSphinx.getResources();
        mRefundIcon = resources.getDrawable(R.drawable.ic_is_refund);
        mNotRefundIcon = resources.getDrawable(R.drawable.ic_not_refund);
        mUpIcon = resources.getDrawable(R.drawable.icon_arrow_up);
        mDownIcon = resources.getDrawable(R.drawable.icon_arrow_down);

        mRefundIcon.setBounds(0, 0, mRefundIcon.getIntrinsicWidth(), mRefundIcon.getIntrinsicHeight());
        mNotRefundIcon.setBounds(0, 0, mNotRefundIcon.getIntrinsicWidth(), mNotRefundIcon.getIntrinsicHeight());
        mUpIcon.setBounds(0, 0, mUpIcon.getIntrinsicWidth(), mUpIcon.getIntrinsicHeight());
        mDownIcon.setBounds(0, 0, mDownIcon.getIntrinsicWidth(), mDownIcon.getIntrinsicHeight());

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
                Drawable drawable = mData.getPicturesDetail().loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
                if(drawable != null) {
                    mPictureImv.setBackgroundDrawable(drawable);
                }else{
                    mPictureImv.setBackgroundResource(R.drawable.bg_picture_detail);
                }
                TKDrawable tkDrawable = mData.getContentPic();
                if (tkDrawable != null) {
                    drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
                    if(drawable != null) {
                        mContentTxv.setText(null);
                        mContentTxv.setBackgroundDrawable(drawable);
                        ViewGroup.LayoutParams layoutParams = mContentTxv.getLayoutParams();
                        layoutParams.width = mPictureDetailWidth;
                        layoutParams.height = (int) (mPictureDetailWidth*((float)drawable.getIntrinsicHeight()/drawable.getIntrinsicWidth()));
                    }
                }
                Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
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
        mPictureImv.setBackgroundResource(R.drawable.bg_picture_detail);
        mContentTxv.setBackgroundDrawable(null);
    }
    
    @Override
    public void setData(BaseData data, int position) {
        super.setData(data, position);
        if (data == null || (data instanceof Tuangou) == false || mData == data) {
            if (mData != null && mParentFragment.position == position) {
                if (mData.getUrl() != null) {
                    mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getUrl(), null);
                } else {
                    String sessionId = Globals.g_Session_Id;
                    if (TextUtils.isEmpty(sessionId) == false) {
                        if (mData.getDingdanCreateResponse() == null) {
                            buy(true);
                        } else {
                            mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getDingdanCreateResponse().getUrl(), null);
                        }
                    }
                }
            }
            return;
        }
        mData = (Tuangou)data;
        if (mParentFragment.position == position) {
            if (mData.getUrl() != null) {
                mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getUrl(), null);
            } else {
                String sessionId = Globals.g_Session_Id;
                if (TextUtils.isEmpty(sessionId) == false) {
                    if (mData.getDingdanCreateResponse() == null) {
                        buy(true);
                    } else {
                        mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getDingdanCreateResponse().getUrl(), null);
                    }
                }
            }
        }

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
            mRefundTxv.setCompoundDrawables(mRefundIcon, null, mDownIcon, null);
        } else {
            mRefundTxv.setCompoundDrawables(mNotRefundIcon, null, null, null);
        }
        mRefundDetailTxv.setVisibility(View.GONE);
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
            }else{
                mPictureImv.setBackgroundResource(R.drawable.bg_picture_detail);
            }
        }
        tkDrawable = mData.getContentPic();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, mParentFragment.toString());
            if(drawable != null) {
            	mContentTxv.setVisibility(View.VISIBLE);
                mContentTxv.setBackgroundDrawable(drawable);
                mContentTxv.setText(null);
            }
        } else {
            mContentTxv.setBackgroundDrawable(null);
        }
    }
    
    private void refreshFendian() {
        Fendian fendian = mData.getFendian();
        DiscoverChildListFragment.showPOI(mSphinx, fendian.getPlaceName(), fendian.getDistance(), fendian.getAddress(), fendian.getPlacePhone(), 
                mFendianNameTxv, mDistanceTxv, mAddressViewGroup, mDividerView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
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
                    mContentView.setVisibility(View.VISIBLE);
                }else{
                    mContentView.setVisibility(View.GONE);
                }
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
        
        if (mContentView.getVisibility() == View.VISIBLE) {
            String detailUrl = mData.getDetailUrl();
            if (detailUrl != null) {
                mContentContainerView.setBackgroundResource(R.drawable.list_middle);
                mViewurlView.setVisibility(View.VISIBLE);
            } else {
                mContentContainerView.setBackgroundResource(R.drawable.list_footer);
                mViewurlView.setVisibility(View.GONE);
            }
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
        	if(!mAsyncTaskExecuting){
	            mLoadingView.setVisibility(View.VISIBLE);
	            DataOperation dataOperation = new DataOperation(mSphinx);
	            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_TUANGOU);
	            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
	            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, mData.getUid());
	            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, needFiled.toString());
	            if (pic.length() > 0) {
	                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE, pic.toString());
	            }
	            dataOperation.setup(Globals.getCurrentCityInfo().getId(), mParentFragment.getId(), mParentFragment.getId(), null, true);
	            mAsyncTaskExecuting = true;
	            mTKAsyncTasking = mSphinx.queryStart(dataOperation);
	            mBaseQuerying = mTKAsyncTasking.getBaseQueryList();
        	}
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
        mViewurlView = findViewById(R.id.viewurl_view);
        mRefundView = findViewById(R.id.refund_view);
        mRefundTxv = (TextView) findViewById(R.id.refund_txv);
        mRefundDetailTxv = (TextView) findViewById(R.id.refund_detail_txv);
        mBuyerNumTxv = (TextView) findViewById(R.id.buyer_num_txv);

        View view = findViewById(R.id.tuangou_fendian_list_item);
        mFendianNameView = view.findViewById(R.id.name_view);
        mFendianNameView.setBackgroundResource(R.drawable.list_middle);
        view.findViewById(R.id.tuangou_fendian_list_item).setPadding(0, 0, 0, 0);
        mFendianNameTxv = (TextView) view.findViewById(R.id.name_txv);
        mDistanceTxv = (TextView)view.findViewById(R.id.distance_txv);
        mAddressViewGroup = view.findViewById(R.id.address_view);
        mDividerView = view.findViewById(R.id.divider_imv);
        mTelephoneView = view.findViewById(R.id.telephone_view);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mNearbyFendianView = findViewById(R.id.nearby_fendian_view);
        mNearbyFendianTxv = (TextView) findViewById(R.id.nearby_fendian_txv);
        mContentView =  findViewById(R.id.content_view);
        mContentContainerView =  findViewById(R.id.content_container_view);
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
        mAddressViewGroup.setOnClickListener(this);
        mTelephoneView.setOnClickListener(this);
        mNearbyFendianView.setOnClickListener(this);
        mServiceHotlineView.setOnClickListener(this);
        mViewurlView.setOnClickListener(this);
        mRefundView.setOnClickListener(this);
        
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
        
        /**
         * If the listener is not set, the click event
         * will be dispatched to the view that's under it, 
         * which is not visible, and should not be click-able.
         */
        mBarView_2.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
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
                mBarView.setVisibility(View.INVISIBLE);
                mBarView_2.setVisibility(View.VISIBLE);
                    return true;
              }
              
          }else{
              if(mBarView_2.getVisibility()==View.VISIBLE){                                                                                                                              
                mBarView.setVisibility(View.VISIBLE);
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
                mActionLog.addAction(mActionTag +  ActionLog.TuangouDetailBuy);
                String sessionId = Globals.g_Session_Id;
                if (TextUtils.isEmpty(sessionId)) {
                    ((TuangouDetailFragment) mParentFragment).isRequsetBuy = true;
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
                    mSphinx.showView(R.id.activity_user_login_regist, intent);
                } else {
                    if (mData.getDingdanCreateResponse() != null) {

                        String tip = null;
                        Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
                        if (shangjia != null) {
                            tip = shangjia.getName();
                        }
                        mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getDingdanCreateResponse().getUrl(), tip);
                        mParentFragment.mSphinx.showView(R.id.view_browser);
                    } else {
                        ((TuangouDetailFragment) mParentFragment).isRequsetBuy = false;
                        buy();
                    }
                }
                break;
                
            case R.id.telephone_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonTelphone);
                Utility.telephone(mSphinx, mTelephoneTxv);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);
                Fendian fendian = mData.getFendian();
                if (fendian == null) {
                    return;
                }
                Utility.queryTraffic(mSphinx, fendian.getPOI(POI.SOURCE_TYPE_TUANGOU), mActionTag);
                break;
                
            case R.id.nearby_fendian_view:
                if (mNearbyFendianTxv.getVisibility() == View.VISIBLE) {
                    mActionLog.addAction(mActionTag +  ActionLog.DiscoverCommonBranch);
                    mSphinx.getDiscoverChildListFragment().setup(mData, mNearbyFendianTxv.getText().toString(), ActionLog.FendianList);
                    mSphinx.showView(R.id.view_discover_child_list);
                }
                break;
            case R.id.service_hotline_view:
                mActionLog.addAction(mActionTag +  ActionLog.TuangouDetailCustomService);
                Utility.telephone(mSphinx, mServiceHotlineTxv);
                break;
                
            case R.id.viewurl_view:
                BrowserActivity.setTuangou(mData);
                Intent intent = new Intent();
                intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.tuanguo_picture_text_detail));
                intent.putExtra(BrowserActivity.URL, mData.getDetailUrl());
                mSphinx.showView(R.id.activity_browser, intent);
                break;
                
            case R.id.refund_view:
                if (mRefundDetailTxv.getVisibility() == View.VISIBLE) {
                    mActionLog.addAction(mActionTag +  ActionLog.TuangouDetailRefund, "1");
                    mRefundDetailTxv.setVisibility(View.GONE); 
                    mRefundTxv.setCompoundDrawables(mRefundIcon, null, mDownIcon, null);
                } else {
                    String refundDetail = null;
                    Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
                    if (shangjia != null) {
                        refundDetail = shangjia.getRefundService();
                    }
                    String refund = mData.getRefund();
                    if (!TextUtils.isEmpty(refund) && !refund.contains(noRefundStr) && !TextUtils.isEmpty(refundDetail)) {
                        mActionLog.addAction(mActionTag +  ActionLog.TuangouDetailRefund, "0");
                        if (refundDetail != null) {
                            mRefundDetailTxv.setText(refundDetail);
                            mRefundDetailTxv.setVisibility(View.VISIBLE);
                            mRefundTxv.setCompoundDrawables(mRefundIcon, null, mUpIcon, null);
                        } else {
                            mRefundDetailTxv.setVisibility(View.GONE); 
                            mRefundTxv.setCompoundDrawables(mRefundIcon, null, mDownIcon, null);
                        }
                    } else {
                        mRefundDetailTxv.setVisibility(View.GONE); 
                    }
                }
                break;
        }
    }
    
    void buy() {
        buy(false);
    }
    
    void buy(boolean hide) {
        DataOperation dataOperation = makeDingdanQuery(mSphinx, mData, hide, mParentFragment.getId(), mParentFragment.getId());
        if (dataOperation != null) {
            mTKAsyncTasking = mSphinx.queryStart(dataOperation);
            mBaseQuerying = mTKAsyncTasking.getBaseQueryList();
        }
    }
    
    public static DataOperation makeDingdanQuery(Activity activity, Tuangou tuangou, boolean hide, int sourceViewId, int targetViewId) {
        DataOperation dataOperation = null;
        User user = Globals.g_User;
        if (user == null) {
            return dataOperation;
        }
        StringBuilder s = new StringBuilder();
        try {
            s.append(Util.byteToHexString(Dingdan.FIELD_UID));
            s.append(':');
            s.append(URLEncoder.encode(tuangou.getGoodsId(), TKConfig.getEncoding()));
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_PNAME));
            s.append(':');
            s.append(URLEncoder.encode(tuangou.getName(), TKConfig.getEncoding()));
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_SJ_ID));
            s.append(':');
            s.append(tuangou.getSource());
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_TYPE));
            s.append(':');
            s.append(Dingdan.TYPE_TUANGOU);
        } catch (UnsupportedEncodingException e) {
            s = null;
            e.printStackTrace();
        }
        if (s != null) {
            dataOperation = new DataOperation(activity);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_DINGDAN);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_CREATE);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_ENTITY, s.toString());
            if (hide) {
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_FLAG, DataOperation.FLAG_NOCREATE);
            }
            dataOperation.setup(Globals.getCurrentCityInfo().getId(), sourceViewId, targetViewId, hide ? null : activity.getString(R.string.doing_and_wait));
        }
        
        return dataOperation;
    }

    @Override
    public boolean onPostExecute(TKAsyncTask tkAsyncTask) {
        if (super.onPostExecute(tkAsyncTask) == false) {
            return false;
        }
        
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        if (BaseActivity.checkReLogin(dataOperation, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mParentFragment.getId(), mParentFragment.getId(), mParentFragment.getId(), mCancelLoginListener, super.mParentFragment.mViewPager.getCurrentItem()==mPosition)) {
            mParentFragment.isReLogin = true;
            return true;
        }

        final Response response = dataOperation.getResponse();
        String dataType = dataOperation.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
        if (BaseQuery.DATA_TYPE_DINGDAN.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, true, mParentFragment, false)) {
                return true;
            }
            DingdanCreateResponse dingdanCreateResponse = (DingdanCreateResponse) response;
            if (false) {
            Intent intent = new Intent(); 
            intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.buy));
            intent.putExtra(BrowserActivity.LEFT, mSphinx.getString(R.string.tuangou_detail));
            intent.putExtra(BrowserActivity.URL, dingdanCreateResponse.getUrl());
            Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
            if (shangjia != null) {
                intent.putExtra(BrowserActivity.TIP, shangjia.getName());
            }
            mSphinx.showView(R.id.activity_browser, intent);
            return true;
            }
            
            mData.setDingdanCreateResponse(dingdanCreateResponse);
            if (TextUtils.isEmpty(dataOperation.getTipText()) == false) {
                String tip = null;
                Shangjia shangjia = Shangjia.getShangjiaById(mData.getSource(), mSphinx, mLoadedDrawableRun);
                if (shangjia != null) {
                    tip = shangjia.getName();
                }
                mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getDingdanCreateResponse().getUrl(), tip);
                mParentFragment.mSphinx.showView(R.id.view_browser);
            } else {
                mParentFragment.mSphinx.getBrowserFragment().setData(mSphinx.getString(R.string.buy), mData.getDingdanCreateResponse().getUrl(), null);
            }
        } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, false, mParentFragment, false)) {
                return true;
            }
            TuangouQueryResponse targetResponse = (TuangouQueryResponse) response;
            Tuangou target = targetResponse.getTuangou();
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

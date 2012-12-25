/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
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
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.CollapseTextView;
import com.tigerknows.view.user.User;
import com.tigerknows.view.user.UserBaseActivity;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class TuangouDetailFragment extends DiscoverBaseFragment implements View.OnClickListener {
    
    public TuangouDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private ScrollView mBodyScv;
    
    private ImageView mPictureImv = null;
    
    private ImageView mShangjiaMarkerImv;
    
    private TextView mPriceTxv;
    
    private TextView mOrgPriceTxv;
    
    private TextView mDiscountTxv;

    private View mBarView = null;

    private Button mBuyBtn = null;
    
    private TextView mNameTxt = null;
    
    private ImageView mRefundImv = null;

    private TextView mRefundTxv = null;

    private TextView mBuyerNumTxv = null;
    
    private TextView mFendianNameTxv = null;
    
    private Button mDistanceBtn = null;
    
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

    private CollapseTextView mNoticedTxv;

    private View mServiceHotlineView;

    private TextView mServiceHotlineTitleTxv;

    private TextView mServiceHotlineTxv;
    
    private View mLoadingView;
    
    private Tuangou mTuangou;
    
    private String mFilterArea;
    
    private boolean isRequsetBuy = false;
    
    private String noRefundStr;
    
    private int mPictureHeight;
    
    private int mPictureDetailWidth;
    
    private TextView mPaddingTxv;
    
    private int mPaddingHeight;
    
    private boolean isRequsetBuy() {
        boolean isRequsetBuy = this.isRequsetBuy;
        this.isRequsetBuy = false;
        return isRequsetBuy;
    }
    
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
            Drawable drawable = mTuangou.getPicturesDetail().loadDrawable(null, null, null);
            if(drawable != null) {
                mPictureImv.setBackgroundDrawable(drawable);
            }
            TKDrawable tkDrawable = mTuangou.getContentPic();
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
            Shangjia shangjia = Shangjia.getShangjiaById(mTuangou.getSource(), null, null);
            if (shangjia != null) {
                mShangjiaMarkerImv.setImageDrawable(shangjia.getMarker());
            } else {
                mShangjiaMarkerImv.setImageDrawable(null);
            }
        }
    };
    
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
            isReLogin();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TuangouXiangqing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.tuangou_detail, container, false);
        findViews();
        setListener();
        
        noRefundStr = mSphinx.getString(R.string.tuangou_no_refund);

        int width = (int)(Globals.g_metrics.widthPixels-(Globals.g_metrics.density*32));
        int height = (int) (width*((float)168/276));
        ViewGroup.LayoutParams layoutParams = mRootView.findViewById(R.id.bg_imv).getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        layoutParams = mPictureImv.getLayoutParams();
        layoutParams.width = width-((int) Globals.g_metrics.density*16);
        layoutParams.height = height-((int) Globals.g_metrics.density*16);
        mPictureHeight = height+Util.dip2px(Globals.g_metrics.density, 16);
        
        mPictureDetailWidth = (int)(Globals.g_metrics.widthPixels-(Globals.g_metrics.density*48));
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.tuangou_detail);
        mRightImv.setImageResource(R.drawable.ic_view_map);
        mRightBtn.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightBtn.setOnClickListener(this);   
        mPaddingTxv.setPadding(0, (mPictureHeight-mPaddingHeight), 0, 0);
        refreshDrawable();
        mBodyScv.smoothScrollTo(0, 0);
        if (isRequsetBuy() && Globals.g_User != null) {
            buy();
            return;
        }
        
        if (isReLogin()) {
            return;
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mPictureImv.setBackgroundDrawable(null);
        mContentTxv.setBackgroundDrawable(null);
    }
    
    public void setData(Tuangou tuangou) {
        mTuangou = tuangou;
        if (mTuangou == null) {
            return;
        }
        mFilterArea = mTuangou.getFilterArea();
        
        Shangjia shangjia = Shangjia.getShangjiaById(tuangou.getSource(), mSphinx, mLoadedDrawableRun);
        if (shangjia != null) {
            mShangjiaMarkerImv.setImageDrawable(shangjia.getMarker());
        } else {
            mShangjiaMarkerImv.setImageDrawable(null);
        }
        
        mPriceTxv.setText(mSphinx.getString(R.string.rmb) + mTuangou.getPrice());
        mOrgPriceTxv.setText(mSphinx.getString(R.string.rmb)+mTuangou.getOrgPrice());
        mOrgPriceTxv.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mDiscountTxv.setText(mTuangou.getDiscount());
        
        mBuyerNumTxv.setText(mSphinx.getString(R.string.tuangou_detail_buyer_num, mTuangou.getBuyerNum()));
        
        String refund = mTuangou.getRefund();
        if (!TextUtils.isEmpty(refund) && !refund.contains(noRefundStr)) {
            mRefundImv.setImageResource(R.drawable.ic_is_refund);
        } else {
            mRefundImv.setImageResource(R.drawable.ic_not_refund);
        }
        mRefundTxv.setText(refund);
        
        if (TextUtils.isEmpty(mFilterArea) || mTuangou.getBranchNum() < 2) {
            mNearbyFendianView.setVisibility(View.GONE);
        } else {
            mNearbyFendianTxv.setText(mFilterArea + mSphinx.getString(R.string.tuangou_detail_nearby, mTuangou.getBranchNum()));
            mNearbyFendianView.setVisibility(View.VISIBLE);
            mNearbyFendianView.setBackgroundResource(R.drawable.list_single);
        }

        String description = mTuangou.getDescription();
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
    
    private void refreshDrawable() {
        if (mTuangou == null) {
            return;
        }
        TKDrawable tkDrawable = mTuangou.getPicturesDetail();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, this.toString());
            if(drawable != null) {
                mPictureImv.setBackgroundDrawable(drawable);
            } else {
                mPictureImv.setBackgroundDrawable(null);
            }
        } else {
            mPictureImv.setBackgroundDrawable(null);
        }
        tkDrawable = mTuangou.getContentPic();
        if (tkDrawable != null) {
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, this.toString());
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
        Fendian fendian = mTuangou.getFendian();
        DiscoverChildListFragment.showPOI(mSphinx, fendian.getPlaceName(), fendian.getDistance(), fendian.getAddress(), fendian.getPlacePhone(), 
                mFendianNameTxv, mDistanceBtn, mAddressView, mDividerView, mTelephoneView, mAddressTxv, mTelephoneTxv, 
                R.drawable.list_header, R.drawable.list_footer, R.drawable.list_single);
    }
    
    private void refreshDescription(boolean query) {
        if (mTuangou == null) {
            return;
        }
        StringBuilder needFiled = new StringBuilder();   
        StringBuilder pic = new StringBuilder();   
        
        String contextText = mTuangou.getContentText();
        ViewGroup.LayoutParams layoutParams = mContentTxv.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.FILL_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (TextUtils.isEmpty(contextText)) {
            mContentTxv.setText(null);
            mContentTxv.setBackgroundDrawable(null);
            TKDrawable tkDrawable = mTuangou.getContentPic();
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, this.toString());
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
        
        String noticed = mTuangou.getNoticed();
        if (TextUtils.isEmpty(noticed)) {
            needFiled.append(Util.byteToHexString(Tuangou.FIELD_NOTICED));
            mNoticedView.setVisibility(View.GONE);
        } else {
            mNoticedTxv.setText(noticed);
            mNoticedTxv.reset();
            mNoticedView.setVisibility(View.VISIBLE);
        }
        
        if (needFiled.length() > 0 && query) {
            mLoadingView.setVisibility(View.VISIBLE);
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_TUANGOU);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, mTuangou.getUid());
            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, needFiled.toString());
            if (pic.length() > 0) {
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE, pic.toString());
            }
            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), null, true);
            mSphinx.queryStart(dataOperation);
        } else {
            mLoadingView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mBodyScv = (ScrollView) mRootView.findViewById(R.id.body_scv);
        mPictureImv = (ImageView) mRootView.findViewById(R.id.picture_imv);
        mShangjiaMarkerImv = (ImageView)mRootView.findViewById(R.id.shangjia_marker_imv);
        mPriceTxv = (TextView)mRootView.findViewById(R.id.price_txv);
        mBarView = mRootView.findViewById(R.id.bar_view);
        mBuyBtn = (Button) mRootView.findViewById(R.id.buy_btn);
        mOrgPriceTxv = (TextView)mRootView.findViewById(R.id.org_price_txv);
        mDiscountTxv = (TextView) mRootView.findViewById(R.id.discount_txv);
        mPaddingTxv = (TextView) mRootView.findViewById(R.id.padding_txv);
        mPaddingTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mPaddingHeight = mPaddingTxv.getMeasuredHeight();
        mNameTxt = (TextView)mRootView.findViewById(R.id.name_txv);
        mRefundImv = (ImageView) mRootView.findViewById(R.id.refund_imv);
        mRefundTxv = (TextView) mRootView.findViewById(R.id.refund_txv);
        mBuyerNumTxv = (TextView) mRootView.findViewById(R.id.buyer_num_txv);

        View view = mRootView.findViewById(R.id.tuangou_fendian_list_item);
        mFendianNameTxv = (TextView) view.findViewById(R.id.name_txv);
        mDistanceBtn = (Button)view.findViewById(R.id.distance_btn);
        mAddressView = view.findViewById(R.id.address_view);
        mDividerView = view.findViewById(R.id.divider_imv);
        mTelephoneView = view.findViewById(R.id.telephone_view);
        mAddressTxv = (TextView)view.findViewById(R.id.address_txv);
        mTelephoneTxv = (TextView)view.findViewById(R.id.telephone_txv);
        
        mNearbyFendianView = mRootView.findViewById(R.id.nearby_fendian_view);
        mNearbyFendianTxv = (TextView) mRootView.findViewById(R.id.nearby_fendian_txv);
        mContentView =  mRootView.findViewById(R.id.content_view);
        mContentTxv = (TextView) mRootView.findViewById(R.id.content_txv);
        mNoticedView = mRootView.findViewById(R.id.noticed_view);
        mNoticedTxv = (CollapseTextView)mRootView.findViewById(R.id.noticed_txv);
        mNoticedTxv.setTextColor(0x99000000);
        mNoticedTxv.setActionTag(ActionLog.TuangouXiangqingNotice);
        mServiceHotlineView = mRootView.findViewById(R.id.service_hotline_view);
        mServiceHotlineTxv = (TextView) mRootView.findViewById(R.id.service_hotline_txv);
        mServiceHotlineTitleTxv = (TextView) mRootView.findViewById(R.id.service_hotline_title_txv);
        mLoadingView = mRootView.findViewById(R.id.loading_view);
    }

    protected void setListener() {
        mBuyBtn.setOnClickListener(this);
        mDistanceBtn.setOnClickListener(this);
        mAddressView.setOnClickListener(this);
        mTelephoneTxv.setOnClickListener(this);
        mNearbyFendianView.setOnClickListener(this);
        mServiceHotlineTxv.setOnClickListener(this);
        mBodyScv.setOnTouchListener(new OnTouchListener() {
            private int lastY = 0;

            private int touchEventId = R.id.view_invalid;

            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    View scroller = (View)msg.obj;
                    mPaddingTxv.setPadding(0, (mPictureHeight-scroller.getScrollY()-mPaddingHeight), 0, 0);
                    if (msg.what == touchEventId) {
                        int y = scroller.getScrollY();
                        if (lastY != y) {
                            handler.sendMessageDelayed(handler.obtainMessage(touchEventId, scroller), 128);
                            lastY = y;
                        } else {
                            handler.sendMessageDelayed(handler.obtainMessage(R.id.view_discover, scroller), 1024);
                        }
                    } else if (msg.what == R.id.view_discover) {
                        int y = scroller.getScrollY();
                        if (lastY != y) {
                            handler.sendMessageDelayed(handler.obtainMessage(R.id.view_discover, scroller), 128);
                            lastY = y;
                        }
                    }
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                handler.sendMessageDelayed(handler.obtainMessage(touchEventId, v), 128);
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

    public void onClick(View view) {
        switch (view.getId()) {                     
            case R.id.buy_btn:
                mActionLog.addAction(ActionLog.TuangouXiangqingBuy);
                String sessionId = Globals.g_Session_Id;
                if (TextUtils.isEmpty(sessionId)) {
                    isRequsetBuy = true;
                    Intent intent = new Intent();
                    intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
                    intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
                    mSphinx.showView(R.id.activity_user_login, intent);
                } else {
                    isRequsetBuy = false;
                    buy();
                }
                break;           
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.Title_Right_Button, mActionTag);
                viewMap();
                break;
            case R.id.telephone_txv:
                mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailTelphone);
                break;
                
            case R.id.address_view:
                mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailAddress);
                viewMap();
                break;
                
            case R.id.distance_btn:
                mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailDistance);
                Fendian fendian = mTuangou.getFendian();
                if (fendian == null) {
                    return;
                }
                /* 交通界面的显示 */
                mSphinx.getTrafficQueryFragment().setData(fendian.getPOI(POI.SOURCE_TYPE_TUANGOU, mTuangou));
                mSphinx.showView(R.id.view_traffic_query);
                break;
                
            case R.id.nearby_fendian_view:
                mActionLog.addAction(ActionLog.TuangouXiangqing+ActionLog.DiscoverDetailBranch);
                mSphinx.getDiscoverChildListFragment().setup(mTuangou, mNearbyFendianTxv.getText().toString(), ActionLog.FendianList);
                mSphinx.showView(R.id.view_discover_child_list);
                break;
            case R.id.service_hotline_txv:
                mActionLog.addAction(ActionLog.TuangouXiangqingCustomService);
                break;
        }
    }
    
    private void buy() {
        User user = Globals.g_User;
        if (user == null) {
            return;
        }
        StringBuilder s = new StringBuilder();
        try {
            s.append(Util.byteToHexString(Dingdan.FIELD_UID));
            s.append(':');
            s.append(URLEncoder.encode(mTuangou.getGoodsId(), TKConfig.getEncoding()));
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_PNAME));
            s.append(':');
            s.append(URLEncoder.encode(mTuangou.getName(), TKConfig.getEncoding()));
            s.append(',');
            s.append(Util.byteToHexString(Dingdan.FIELD_SJ_ID));
            s.append(':');
            s.append(mTuangou.getSource());
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
            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
            mSphinx.queryStart(dataOperation);
        }
    }
    
    public void viewMap() {
        Fendian fendian = mTuangou.getFendian();
        if (fendian == null) {
            return;
        }
        List<POI> list = new ArrayList<POI>();
        POI poi = fendian.getPOI(POI.SOURCE_TYPE_TUANGOU, mTuangou);
//        poi.setOrderNumber(mTuangou.getOrderNumber());
        list.add(poi);
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.shanghu_ditu), ActionLog.MapTuangouXiangqing);
        mSphinx.showView(R.id.view_result_map);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        final DataOperation dataOperation = (DataOperation)(tkAsyncTask.getBaseQuery());
        if (BaseActivity.checkReLogin(dataOperation, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        }

        final Response response = dataOperation.getResponse();
        String dataType = dataOperation.getCriteria().get(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
        if (BaseQuery.DATA_TYPE_DINGDAN.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, true, this, false)) {
                return;
            }
            DingdanCreateResponse dingdanCreateResponse = (DingdanCreateResponse) response;
            Intent intent = new Intent(); 
            intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.buy));
            intent.putExtra(BrowserActivity.LEFT, mSphinx.getString(R.string.tuangou_detail));
            intent.putExtra(BrowserActivity.URL, dingdanCreateResponse.getUrl());
            Shangjia shangjia = Shangjia.getShangjiaById(mTuangou.getSource(), mSphinx, mLoadedDrawableRun);
            if (shangjia != null) {
                intent.putExtra(BrowserActivity.TIP, shangjia.getName());
            }
            mSphinx.showView(R.id.activity_browser, intent);
        } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
            if (BaseActivity.checkResponseCode(dataOperation, mSphinx, null, false, this, false)) {
                return;
            }
            TuangouQueryResponse targetResponse = (TuangouQueryResponse) response;
            Tuangou target = targetResponse.getTuangou();
            if (target != null && dataOperation.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_UID).equals(mTuangou.getUid())) {
                try {
                    mTuangou.init(target.getData());
                    refreshDescription(false);
                } catch (APIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}

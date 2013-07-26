/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Coupon;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.CouponResponse;
import com.tigerknows.model.DataQuery.CouponResponse.CouponList;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;
import com.tigerknows.widget.SpringbackListView;

/**
 * @author Peng Wenyue
 */
public class CouponListFragment extends BaseFragment implements RetryView.CallBack {
    
    static final String TAG = "CouponListFragment";

    public CouponListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    static final int STATE_QUERYING = 0;
    static final int STATE_ERROR = 1;
    static final int STATE_EMPTY = 2;
    static final int STATE_LIST = 3;
    
    private int mState = STATE_QUERYING;
    
    private SpringbackListView mResultLsv = null;

    private QueryingView mQueryingView = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private RetryView mRetryView;
    
    private List<Coupon> mCouponArrayList = new ArrayList<Coupon>();
    private CouponAdapter mCouponAdapter;
    private POI mPOI;
    
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
            if (mSphinx.uiStackPeek() == getId() && mSphinx.isFinishing() == false) {
                mCouponAdapter.notifyDataSetChanged();
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mActionTag = ActionLog.CouponList;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_coupon_list, container, false);
        
        findViews();
        setListener();

        mEmptyTxv.setText(R.string.coupon_no_result);
        mRetryView.setCallBack(this, mActionTag);
        
        mCouponAdapter = new CouponAdapter(mSphinx, mCouponArrayList);
        mResultLsv.setAdapter(mCouponAdapter);
        
        return mRootView;
    }

    protected void findViews() {
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
//        mCommentLsv.addHeaderView(v);
//        v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
    }
    
    protected void setListener() {

        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
                    Coupon data = (Coupon) adapterView.getAdapter().getItem(position);
                    if (data != null) {
                        mSphinx.showView(R.id.view_coupon_detail);
                        mSphinx.getCouponDetailFragment().setData(mCouponArrayList, position);
                    }
                }
            }
        });
    }
        
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setText(R.string.coupon_list);
        mRightBtn.setVisibility(View.INVISIBLE);
        
        mResultLsv.setFooterSpringback(false);
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
        
        if (isReLogin()) {
            return;
        }
        
        mLoadedDrawableRun.run();
    }
    
    public void setData(POI poi) {
        mPOI = poi;
        mCouponArrayList.clear();
        mCouponAdapter.notifyDataSetChanged();
        DataQuery dataQuery = mPOI.getCouponQuery();
        if (dataQuery != null) {
            setDataQuery(dataQuery);
        } else {
            this.mState = STATE_QUERYING;
            updateView();
            
            dataQuery = new DataQuery(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_COUPON);
            criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
            criteria.put(DataQuery.SERVER_PARAMETER_NEED_FIELD, Coupon.NEED_FIELD);
            dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null);
            mSphinx.queryStart(dataQuery);
        }
    }
    
    private class CouponAdapter extends ArrayAdapter<Coupon>{
        private static final int Resource_Id = R.layout.poi_coupon_list_item;

        public CouponAdapter(Context context, List<Coupon> list) {
            super(context, Resource_Id, list);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(Resource_Id, parent, false);
            } else {
                view = convertView;
            }

            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView)view.findViewById(R.id.name_txv);
            TextView hotTxv = (TextView)view.findViewById(R.id.hot_txv);
            
            final Coupon coupon = getItem(position);
            
            nameTxv.setText(coupon.getDescription());
            hotTxv.setText(mSphinx.getString(R.string._used_sum_times, coupon.getHot()));
            
            TKDrawable tkDrawable = coupon.getBriefPicTKDrawable();
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mSphinx, mLoadedDrawableRun, CouponListFragment.this.toString());
                if(drawable != null) {
                    //To prevent the problem of size change of the same pic 
                    //After it is used at a different place with smaller size
                    if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
                        pictureImv.setBackgroundDrawable(null);
                    }
                    pictureImv.setBackgroundDrawable(drawable);
                } else {
                    pictureImv.setBackgroundDrawable(null);
                }
                
            } else {
                pictureImv.setBackgroundResource(R.drawable.bg_picture_hotel_none);
            }
                
            return view;
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mCouponArrayList.clear();
        mCouponAdapter.notifyDataSetChanged();
        mEmptyTxv.setVisibility(View.GONE);
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        dismiss();
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {   
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();   
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), R.id.view_user_home, R.id.view_more_home, null)) {
            isReLogin = true;
            return;
        }
        
        if (baseQuery instanceof DataQuery) { 
            int resId = R.id.view_invalid;
            Response response = baseQuery.getResponse();
            if (response == null) {
                resId = R.string.touch_screen_and_retry;
            } else if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                resId = BaseActivity.getResponseResId(baseQuery);
            }
            
            if (resId != R.id.view_invalid) {
                mRetryView.setText(resId, true);
                mState = STATE_ERROR;
                updateView();
                return;
            }

            setDataQuery((DataQuery)baseQuery);
        }
    }
    
    void setDataQuery(DataQuery dataQuery) {
        if (dataQuery == null) {
            return;
        }
        mPOI.setCouponQuery(dataQuery);
        CouponResponse couponResponse = (CouponResponse)dataQuery.getResponse();
        CouponList couponList = couponResponse.getList();
        
        if (couponList != null && couponList.getList() != null && couponList.getList().size() > 0) {
            mCouponArrayList.clear();
            List<Coupon> list = couponList.getList();
            if (list != null) {
                mCouponArrayList.addAll(list);
            }
            mCouponAdapter.notifyDataSetChanged();
        }
        
        if (mCouponArrayList.isEmpty()) {
            mState = STATE_EMPTY;
        } else {
            mState = STATE_LIST;
            mSphinx.getHandler().post(new Runnable() {

                @Override
                public void run() {
                    mResultLsv.setSelectionFromTop(0, 0);
                }
            });
        }
        
        updateView();
    }
    
    private void updateView() {
        if (mState == STATE_QUERYING) {
            mQueryingView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
        } else {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void retry() {
        if (mBaseQuerying != null) {
            for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                mBaseQuerying.get(i).setResponse(null);
            }
            mSphinx.queryStart(mBaseQuerying);
        }
        mState = STATE_QUERYING;
        updateView();
    }
}

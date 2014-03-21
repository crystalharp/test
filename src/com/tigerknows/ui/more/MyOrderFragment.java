/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 * 2013.08
 */

package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKFragmentManager;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.DataQuery.ShangjiaResponse;
import com.tigerknows.model.DataQuery.ShangjiaResponse.ShangjiaList;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.common.BrowserActivity;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.util.Utility;

public class MyOrderFragment extends BaseFragment{

	public MyOrderFragment(Sphinx sphinx) {
		super(sphinx);
	}
	
	private LinearLayout mTuangouTitleLly;
	private LinearLayout mTuangouDingdanLly;
	private Button mHotelOrderBtn;
	
	private List<Shangjia> mResultList = new ArrayList<Shangjia>();
	private Shangjia mRequestLogin = null;
	
	private boolean mFromTuangou;
	private boolean mActualOnCreate;
	
	private Thread mThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mActionTag = ActionLog.MyAllOrder;
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.more_my_order, container, false);
        
        findViews();
        setListener();
        return mRootView;
    }

    @Override
    protected void findViews() {
        super.findViews();
    	mTuangouTitleLly = (LinearLayout)mRootView.findViewById(R.id.tuangou_title_lly);
    	mTuangouDingdanLly = (LinearLayout)mRootView.findViewById(R.id.tuangou_dingdan_lly);
    	mHotelOrderBtn = (Button)mRootView.findViewById(R.id.hotel_order_btn);
    	
    }

    @Override
    protected void setListener() {
        super.setListener();
		mHotelOrderBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mActionLog.addAction(mActionTag + ActionLog.MyAllOrderHotel);
            	mSphinx.getHotelOrderListFragment().clearOrders();
            	mSphinx.getHotelOrderListFragment().syncOrder();
                mSphinx.showView(TKFragmentManager.ID_view_hotel_order_list);
			}
		});
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mActualOnCreate = false;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mTitleBtn.setText(getString(R.string.wodedingdan));
		
        if (isReLogin()) {
            return;
        }
        
        LogWrapper.d("Trap", mActualOnCreate == true ? "Create" : "Resume");
        
        List<Shangjia> list = new ArrayList<Shangjia>();
        List<Shangjia> shangjiaList = Shangjia.getShangjiaList();
        synchronized (shangjiaList) {
            list.addAll(shangjiaList);
        }
        
		synchronized (MyOrderFragment.this) {
		    
			if (list.size() != mResultList.size()) {
				mResultList.clear();
				mResultList.addAll(list);
				createShangjiaListView();
			}
		}
		if(mThread == null || mThread.isAlive() == false){
		
			mThread = new Thread(new Runnable(){
				
				@Override
				public void run() {
					if(!mActualOnCreate){
						return;
					}
					Shangjia.readShangjiaList(mContext);

			        final List<Shangjia> list = new ArrayList<Shangjia>();
			        List<Shangjia> shangjiaList = Shangjia.getShangjiaList();
			        synchronized (shangjiaList) {
			            list.addAll(shangjiaList);
			        }
			        
					mSphinx.getHandler().post(new Runnable(){
						
						@Override
						public void run() {
							synchronized (MyOrderFragment.this) {
								mResultList.clear();
								mResultList.addAll(list);
								createShangjiaListView();
							}
						}
					});
				}
			});
			mThread.start();
		}

		if (mRequestLogin != null) {
		    if (Globals.g_User != null) {
		        requestUrl(mRequestLogin);
		    }
		    mRequestLogin = null;
		}
		
		if (mFromTuangou){
			mTitleBtn.setText(getString(R.string.tuangoudingdan));
			mTuangouTitleLly.setVisibility(View.GONE);
			mHotelOrderBtn.setVisibility(View.GONE);
		}else{
			mTitleBtn.setText(getString(R.string.wodedingdan));
			mTuangouTitleLly.setVisibility(View.VISIBLE);
			mHotelOrderBtn.setVisibility(View.VISIBLE);
		}
	}
    
    protected void createShangjiaListView(){
        mTuangouDingdanLly.removeAllViews();
    	for(int i=0; i<mResultList.size(); i++){
    		final Button btn = new Button(mContext);
    		final Shangjia shangjia = mResultList.get(i);
    		btn.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    		btn.setTextSize(16);
    		btn.setTextColor(mSphinx.getResources().getColor(R.color.black_dark));
    		btn.setText(getString(R.string.view) + shangjia.getMessage());
    		btn.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
    		if (i == mResultList.size() - 1){
    			btn.setBackgroundDrawable(mSphinx.getResources().getDrawable(R.drawable.list_footer));
    		}else if(i == 0 && mFromTuangou == true){
    			btn.setBackgroundDrawable(mSphinx.getResources().getDrawable(R.drawable.list_header));
    		}else{
    			btn.setBackgroundDrawable(mSphinx.getResources().getDrawable(R.drawable.list_middle));
    		}
    		btn.setPadding(Utility.dip2px(mContext, 16), 0, Utility.dip2px(mContext, 16), 0);
    		Drawable arrowRight = getResources().getDrawable(R.drawable.icon_arrow_right);
    		arrowRight.setBounds(0, 0, arrowRight.getIntrinsicWidth(), arrowRight.getIntrinsicHeight());
    		Drawable logo = shangjia.getLogo();
    		if (logo != null){
    			int hh = Utility.dip2px(mContext, 28);
    			int ww = Math.round(hh * logo.getIntrinsicWidth() / logo.getIntrinsicHeight());
    			logo.setBounds(0, 0, ww, hh);
    		}
    		btn.setCompoundDrawablePadding(Utility.dip2px(mContext, 16));
    		btn.setCompoundDrawables(logo, null, arrowRight, null);
    		btn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mActionLog.addAction(mActionTag + ActionLog.MyAllOrderTuangou, shangjia.getName());
	                if (!TextUtils.isEmpty(shangjia.getUrl()) && shangjia.getFastPurchase() == 1) {
	                	startShangjia(shangjia);
	                } else if (shangjia.getFastPurchase() == 1 || Globals.g_User != null) {
	                    requestUrl(shangjia);
	                } else {
	                    mRequestLogin = shangjia;
	                	Intent intent = new Intent();
	                	intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
	                	intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
	                	intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
	                	mSphinx.showView(R.id.activity_user_login_regist, intent);
	                }
				}
			});
    		mTuangouDingdanLly.addView(btn);
    	}
    }
    
    @Override
    public boolean isReLogin(){
        if (Globals.g_User == null) {
            mBaseQuerying = null;
        }
        return super.isReLogin();
    }
    
    private void startShangjia(Shangjia shangjia){
    	
        Intent intent = new Intent();
        intent.setClass(mSphinx, BrowserActivity.class);
        intent.putExtra(BrowserActivity.TITLE, getString(R.string.wodedingdan));
        intent.putExtra(BrowserActivity.LEFT, getString(R.string.tuangou_shop_list));
        intent.putExtra(BrowserActivity.URL, shangjia.getUrl());
        intent.putExtra(BrowserActivity.TIP, shangjia.getName());
        mSphinx.startActivity(intent);
    }
    private void requestUrl(Shangjia shangjia) {
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_SHANGJIA);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_IDS, String.valueOf(shangjia.getSource()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, Shangjia.NEED_FIELD);
        dataQuery.addParameter(BaseQuery.SERVER_PARAMETER_SESSION_ID, Globals.g_Session_Id);
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        mSphinx.queryStart(dataQuery);
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        // TODO Auto-generated method stub
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (baseQuery.isStop()){
        	return;
        }
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(TKFragmentManager.ID_view_user_home), getId(), getId(), getId(), mCancelLoginListener)){
        	isReLogin = true;
        	return;
        } else if(BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_NOTHING, MyOrderFragment.this, false)){
        	return;
        }
        Response response = baseQuery.getResponse();
        ShangjiaResponse shangjiaResponse = null;
        if (response instanceof ShangjiaResponse) {
        	shangjiaResponse = (ShangjiaResponse) response;
        }else{
        	return;
        }
        if(Response.RESPONSE_CODE_OK != response.getResponseCode()){
        	return;
        }
        ShangjiaList shangjiaList = shangjiaResponse.getList();
        if(shangjiaList != null && shangjiaList.getList()!= null && shangjiaList.getList().size() == 1){
        	Shangjia shangjia = shangjiaList.getList().get(0);
        	if(shangjia.getUrl()!= null && !TextUtils.isEmpty(shangjia.getUrl())){
        		startShangjia(shangjia);
        	}
        }
    }
    
    public void setData(boolean fromTuangou){
    	if(fromTuangou){
    		mFromTuangou = true;
    	}else{
    		mFromTuangou = false;
    	}
    	mActualOnCreate = true;
    }
}

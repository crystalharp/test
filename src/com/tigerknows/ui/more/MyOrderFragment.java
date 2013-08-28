package com.tigerknows.ui.more;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.User;
import com.tigerknows.model.DataQuery.ShangjiaResponse;
import com.tigerknows.model.DataQuery.ShangjiaResponse.ShangjiaList;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.util.Utility;

public class MyOrderFragment extends BaseFragment{

	public MyOrderFragment(Sphinx sphinx) {
		super(sphinx);
	}
	
	private LinearLayout mTuangouDingdanLly;
	private Button mHotelOrderBtn;
	
	private List<Shangjia> mResultList;
	private String sessionId;
	private DataQuery mDataQuery;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		// TODO: mActionTag
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.more_my_order, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }
    
    protected void findViews() {
    	mTuangouDingdanLly = (LinearLayout)mRootView.findViewById(R.id.tuangou_dingdan_lly);
    	mHotelOrderBtn = (Button)mRootView.findViewById(R.id.hotel_order_btn);
    	
    }

	protected void setListener() {
		mHotelOrderBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO mActionLog.addAction(mActionTag + ActionLog.HotelQueryOrder);
            	mSphinx.getHotelOrderListFragment().clearOrders();
            	mSphinx.getHotelOrderListFragment().syncOrder();
                mSphinx.showView(R.id.view_hotel_order_list);
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

    private void setData(DataQuery dataQuery) {
        mResultList.clear();
        Response response = dataQuery.getResponse();
        ShangjiaResponse shangjiaResponse = (ShangjiaResponse)response;
        ShangjiaList shangjiaList = shangjiaResponse.getList();
        if (shangjiaList != null) {
            List<Shangjia> shangjiaArrayList = shangjiaList.getList();
            if (shangjiaArrayList != null && shangjiaArrayList.size() > 0) {
                mDataQuery = dataQuery;
                mDataQuery.setParameter(BaseQuery.SERVER_PARAMETER_SESSION_ID, sessionId);
                mResultList.addAll(shangjiaArrayList);
                createShangjiaListView();
            }
        }
        
        if (mResultList.isEmpty()) {
            dismiss();
        }
    }
    
    protected void createShangjiaListView(){
    	for(int i=0; i<mResultList.size(); i++){
    		final Button btn = new Button(mContext);
    		final Shangjia shangjia = mResultList.get(i);
    		btn.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    		btn.setTextSize(16);
    		btn.setText(mSphinx.getString(R.string.view) + shangjia.getMessage());
    		if (i == mResultList.size() - 1){
    			btn.setBackgroundDrawable(mSphinx.getResources().getDrawable(R.drawable.list_footer));
    		}else{
    			btn.setBackgroundDrawable(mSphinx.getResources().getDrawable(R.drawable.list_middle));
    		}
    		btn.setPadding(Utility.dip2px(mContext, 8), 0, Utility.dip2px(mContext, 16), 0);
    		btn.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, shangjia.getName());
					// TODO: 需要区分是否为快捷购买的商家，然后给予不同的跳转逻辑
	                User user = Globals.g_User;
	                if (user != null && shangjia.getFastPurchase() == 0) {
	                	Intent intent = new Intent();
	                	intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
	                	intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.activity_browser);
	                	intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
	                	mSphinx.showView(R.id.activity_user_login, intent);
	                } else {
	                	Intent intent = new Intent();
	                	intent.setClass(mSphinx, BrowserActivity.class);
	                	intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.wodedingdan));
	                	intent.putExtra(BrowserActivity.LEFT, mSphinx.getString(R.string.tuangou_shop_list));
	                	intent.putExtra(BrowserActivity.URL, shangjia.getUrl());
	                	intent.putExtra(BrowserActivity.TIP, shangjia.getName());
	                	mSphinx.startActivity(intent);
	                }

				}
			});
    		mTuangouDingdanLly.addView(btn);
    	}
    }
}

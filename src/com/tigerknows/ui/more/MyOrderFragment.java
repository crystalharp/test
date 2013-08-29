package com.tigerknows.ui.more;

import java.util.ArrayList;
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
import com.tigerknows.android.os.TKAsyncTask;
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
	
	private List<Shangjia> mResultList = new ArrayList<Shangjia>();
	private String sessionId;
	private DataQuery mDataQuery;
	private Shangjia mRequestLogin = null;
	
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
		List<Shangjia> list = Shangjia.getShangjiaList();
		if (list.size() != mResultList.size()) {
		    mResultList.clear();
		    mResultList.addAll(list);
		    createShangjiaListView();
		}
		
		if (mRequestLogin != null) {
		    if (Globals.g_User != null) {
		        requestUrl(mRequestLogin);
		    }
		    mRequestLogin = null;
		}
	}
    
    protected void createShangjiaListView(){
        mTuangouDingdanLly.removeAllViews();
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
	                if (shangjia.getUrl() != null) {
                        Intent intent = new Intent();
                        intent.setClass(mSphinx, BrowserActivity.class);
                        intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.wodedingdan));
                        intent.putExtra(BrowserActivity.LEFT, mSphinx.getString(R.string.tuangou_shop_list));
                        intent.putExtra(BrowserActivity.URL, shangjia.getUrl());
                        intent.putExtra(BrowserActivity.TIP, shangjia.getName());
                        mSphinx.startActivity(intent);
	                } else if (shangjia.getFastPurchase() == 1 || (Globals.g_User != null)) {
	                    requestUrl(shangjia);
	                } else {
	                    mRequestLogin = shangjia;
	                	Intent intent = new Intent();
	                	intent.putExtra(UserBaseActivity.SOURCE_VIEW_ID_LOGIN, getId());
	                	intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, getId());
	                	intent.putExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, getId());
	                	mSphinx.showView(R.id.activity_user_login, intent);
	                }

				}
			});
    		mTuangouDingdanLly.addView(btn);
    	}
    }
    
    void requestUrl(Shangjia shangjia) {
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_SHANGJIA);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SHANGJIA_IDS, String.valueOf(shangjia.getId()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, Shangjia.NEED_FIELD_ONLY_URL);
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
        
    }
    
}

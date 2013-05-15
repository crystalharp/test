/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.provider.HotelOrderTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

/**
 * @author Peng Wenyue
 */
public class HotelOrderListFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "DiscoverChildListFragment";

    public HotelOrderListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private TextView mResultTxv;

    private SpringbackListView mResultLsv = null;

    private View mEmptyView = null;
    
    private View mQueryingView = null;
    
    private TextView mEmptyTxv = null;

    private List<HotelOrder> hotelOrders = new ArrayList<HotelOrder>();
    
    private DataQuery mDataQuery;

	private HotelOrderAdapter hotelOrderAdapter;
    
	private int orderTotal = 0;
	
	private List<HotelOrder> orders = new ArrayList<HotelOrder>();
    
    private int state;
    
    
    
    private static final int STATE_LOADING = 1;
    private static final int STATE_LIST = 2;
    private static final int STATE_EMPTY = 3;
    private static final int STATE_LOADING_MORE = 4;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                mResultLsv.getView(false).performClick();
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_list, container, false);

        findViews();
        setListener();
        hotelOrderAdapter = new HotelOrderAdapter(mContext, orders);
        mResultLsv.setAdapter(hotelOrderAdapter);

        mQueryingView.setVisibility(View.INVISIBLE);
        mEmptyView.setVisibility(View.INVISIBLE);
        mResultLsv.setVisibility(View.VISIBLE);
        
        return mRootView;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void dismiss() {
        super.dismiss();
        if (hotelOrders != null) {
            hotelOrders.clear();
            if (hotelOrderAdapter != null) {
                hotelOrderAdapter.notifyDataSetChanged();
            }
        }
        mDataQuery = null;
    }
    
    protected void findViews() {
        mResultTxv = (TextView) mRootView.findViewById(R.id.result_txv);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingView = mRootView.findViewById(R.id.querying_view);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
    }

    protected void setListener() {
        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage();
            }
        });
        
        mResultLsv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mSphinx.showView(R.id.view_hotel_order_detail);
				mSphinx.getHotelOrderDetailFragment().setData(orders.get(position));
			}
        	
		});
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(mContext.getString(R.string.hotel_order));
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        //fillOrderDb();
        new LoadThread(0, 1000).start();
    }
    
    public void fillOrderDb(){
    	HotelOrderTable table = new HotelOrderTable(mContext);
    	try {
    		long start = System.currentTimeMillis();
    		List<HotelOrder> list = table.read(0, 100);
    		System.out.println("Time used for read: " + (System.currentTimeMillis()-start)/1000.0);
    		System.out.println("OrderDB count: " + list.size());
    		
    		if(list.size() < 100){
    			HotelOrder order = new HotelOrder("11111", System.currentTimeMillis(), 1, "ssss", "HotelName", "hotelAddress", new Position(111,111), "13581704277", 
    					mContext.getString(R.string.app_name), 3, 390, 
    					System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(),2, 
    					"GuestName", "13581704277");
    			for (int i = list.size(); i < 100; i++) {
    				order.setId("" + i + i + i + i);
    				table.write(order);
    			}
    		}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (APIException e) {
			e.printStackTrace();
		}finally{
			
			if(table != null){
				table.close();
			}
			
		}
    }
    
    private void turnPage(){
        synchronized (this) {
        	
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_btn:
                
            default:
                break;
        }
    }

    public class HotelOrderAdapter extends ArrayAdapter<HotelOrder>{
        private static final int RESOURCE_ID = R.layout.hotel_order_list_item;
        
        public HotelOrderAdapter(Context context, List<HotelOrder> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }

            HotelOrder hotelOrder = new HotelOrder();
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView roomTypeTxv = (TextView) view.findViewById(R.id.room_type_txv);
            TextView checkinDateTxv = (TextView) view.findViewById(R.id.checkin_date_txv);
            TextView checkoutDateTxv = (TextView) view.findViewById(R.id.checkout_date_txv);
            TextView dayCountView = (TextView) view.findViewById(R.id.day_count_txv);

            return view;
        }
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
    }
    
    Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			List<HotelOrder> ordersLoaded = (List<HotelOrder>) msg.obj;
			if( ordersLoaded != null ){
				System.out.println("Orders loaded: " + ordersLoaded.size());
				orders.addAll(ordersLoaded);
				hotelOrderAdapter.notifyDataSetChanged();
			}
			
		}
    	
    };

    /**
     * Thread used to load HotelOrder from database.
     * @author jiangshaui
     *
     */
    class LoadThread extends Thread{

    	int startIndex = 0;
    	int loadCount = 100;
    	
        public LoadThread(int startIndex, int loadCount) {
			super();
			this.startIndex = startIndex;
			this.loadCount = loadCount;
		}


		@Override
        public void run(){
            Message msg = Message.obtain();
            List<HotelOrder> orderList = null;
            HotelOrderTable table = null;
    		try {
    			table = new HotelOrderTable(mContext);
    			long startTime = System.currentTimeMillis();
    			orderTotal = table.count();
    			System.out.println("Count time: " + (System.currentTimeMillis()-startTime));
    			startTime = System.currentTimeMillis();
				orderList = table.read(startIndex, loadCount);
    			System.out.println("Read time: " + (System.currentTimeMillis()-startTime));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (APIException e) {
				e.printStackTrace();
			}finally{
				if(table!=null){
					table.close();
				}
			}
        		
            msg.obj = orderList;
            mHandler.sendMessage(msg);
        }
		

		public int getStartIndex() {
			return startIndex;
		}

		public void setStartIndex(int startIndex) {
			this.startIndex = startIndex;
		}

		public int getLoadCount() {
			return loadCount;
		}

		public void setLoadCount(int loadCount) {
			this.loadCount = loadCount;
		}
		
    }// end LoadThread class
    
    
    
}

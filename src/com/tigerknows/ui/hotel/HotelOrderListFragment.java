/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.model.HotelOrderOperation;
import com.tigerknows.model.HotelOrderOperation.HotelOrderSyncResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.HotelOrderOperation.HotelOrderStatesResponse;
import com.tigerknows.provider.HotelOrderTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

/**
 * @author Peng Wenyue
 */
public class HotelOrderListFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "HotelOrderListFragment";

    public HotelOrderListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private TextView mResultTxv;

    private SpringbackListView mResultLsv = null;

    private View mEmptyView = null;
    
    private View mQueryingView = null;

    private TextView mEmptyTxv = null;
    
    private ImageView mEmptyImv = null;

    /**
     * View group containing {@link mServiceHotlineTitleTxv} and {@link mServiceHotlineTxv}
     */
    private View mServiceHotlineView;

    /**
     * Hotline number
     */
    private TextView mServiceHotlineTxv;

    private DataQuery mDataQuery;

	private HotelOrderAdapter hotelOrderAdapter;
    
	private int orderTotal = 0;
	
	private List<HotelOrder> orders = new ArrayList<HotelOrder>();
    
    private int fragmentState;
    
    /**
     * 需要查询状态的订单的buffer
     */
    private LinkedList<HotelOrder> ordersToQuery = new LinkedList<HotelOrder>();
    
    /**
     * 正在查询状态的订单列表
     */
    private LinkedList<HotelOrder> ordersQuerying = new LinkedList<HotelOrder>();
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.HotelOrderList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        logd("onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_list, container, false);

        findViews();
        setListener();
        hotelOrderAdapter = new HotelOrderAdapter(mContext, orders);
        mResultLsv.setHeaderSpringback(false);
        mResultLsv.setAdapter(hotelOrderAdapter);
        mResultLsv.changeHeaderViewByState(true, SpringbackListView.PULL_TO_REFRESH);
        mEmptyTxv.setText( mContext.getString(R.string.no_order) );
        mEmptyImv.setBackgroundResource(R.drawable.bg_order_empty);
        
        return mRootView;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void dismiss() {
        super.dismiss();
        clearOrders();
        mDataQuery = null;
    }
    
    /**
     * 清除列表界面的内容，列表页onResume时会重新加载订单。
     */
    public void clearOrders(){
        if (orders != null) {
        	orders.clear();
            if (hotelOrderAdapter != null) {
                hotelOrderAdapter.notifyDataSetChanged(); 
            }
        }
    }
    
    protected void findViews() {
        mResultTxv = (TextView) mRootView.findViewById(R.id.result_txv);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mEmptyImv = (ImageView)mEmptyView.findViewById(R.id.icon_imv);
        mQueryingView = mRootView.findViewById(R.id.querying_view);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        v = mLayoutInflater.inflate(R.layout.hotel_hot_line, null);
        LinearLayout.LayoutParams params = (LayoutParams) v.findViewById(R.id.service_hotline_view).getLayoutParams();//set(0, Util.dip2px(Globals.g_metrics.density, 8), 0, 0);
        params.topMargin = Util.dip2px(Globals.g_metrics.density, 8);
        v.findViewById(R.id.service_hotline_view).setLayoutParams(params);
        mResultLsv.addHeaderView(v);
        mServiceHotlineView = mRootView.findViewById(R.id.service_hotline_view);
        mServiceHotlineTxv = (TextView) mRootView.findViewById(R.id.service_hotline_txv);
    }

    protected void setListener() {
        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
				if(isHeader){
					mResultLsv.onRefreshComplete(isHeader);
					mResultLsv.changeHeaderViewByState(true, SpringbackListView.PULL_TO_REFRESH);
					return;
				}
				turnPage();
            }
        });
        
        mResultLsv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mActionLog.addAction(mActionTag+ActionLog.HotelOrderListItemClick, position);
				mSphinx.getHotelOrderDetailFragment().setData(orders.get(position-1) , position-1);
				mSphinx.getHotelOrderDetailFragment().setStageIndicatorVisible(false);
				mSphinx.showView(R.id.view_hotel_order_detail);
			}
        	
		});
        mServiceHotlineView.setOnClickListener(this);
    }
    
    private void reloadOrders(){
            mQueryingView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
//            mResultLsv.setVisibility(View.INVISIBLE);
        	new LoadThread(0, TKConfig.getPageSize()).start();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        mRightBtn.setVisibility(View.GONE);
        mTitleBtn.setText(mContext.getString(R.string.hotel_ordered));

//        fillOrderDb();

        /**
         * if fragment is previously closed, size of orders will be 0
         * if fragment is opened and in stack
         */
        if(orders.size() == 0 ){
        	reloadOrders();
        }
        
        if(couldAnomalyExists()){
        	logi("Anomaly could exists. Send list sync query.");
        	sendOrderSyncQuery();
        }
        
    }

    /**
     * Check the preference to see whether there's possibility
     * that an anomaly exists.
     * @return
     */
    private boolean couldAnomalyExists(){
    	String orderSubmited = TKConfig.getPref(mContext, TKConfig.PREFS_HOTEL_ORDER_COULD_ANOMALY_EXISTS, "no");
    	logi("orderSubmited: " + orderSubmited);
		return "yes".equals(orderSubmited);
    }
    
	private void sendOrderSyncQuery(){
		
		String ids = null;
		HotelOrderTable table = new HotelOrderTable(mContext);
		ids = table.getAllIds();
		table.close();
    	logi("send order sync query for: " + ids + "$");
    	
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_SYNC);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_ORDER_ID_FILTER, ids);
    	criteria.put(BaseQuery.SERVER_PARAMETER_NEED_FIELD, HotelOrder.NEED_FIELDS);
    	HotelOrderOperation hotelOrderOperation = new HotelOrderOperation(mSphinx);
    	hotelOrderOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), mContext.getString(R.string.query_loading_tip));
    	mTkAsyncTasking = mSphinx.queryStart(hotelOrderOperation);
    	mBaseQuerying = mTkAsyncTasking.getBaseQueryList();
		
	}
	
    public void removeOrder(HotelOrder order){
    	orders.remove(order);
    	if(hotelOrderAdapter!=null){
    		hotelOrderAdapter.notifyDataSetChanged();
    	}
    }
    
    private void fillOrderDb(){
    	int maxDbSize = 60;
    	HotelOrderTable table = new HotelOrderTable(mContext);
    	try {
    		long start = System.currentTimeMillis();
    		int totalCount = table.count();
    		List<HotelOrder> list = table.read(0, maxDbSize);
    		System.out.println("Time used for read: " + (System.currentTimeMillis()-start)/1000.0);
    		System.out.println("OrderDB count: " + list.size());
    		System.out.println("Total count: " + totalCount);
    		
    		if(list.size() < maxDbSize){
    			HotelOrder order = new HotelOrder("11111", System.currentTimeMillis(), 1, "0F2B4330-906A-11E2-A511-06973B18DA73", "HotelName", "hotelAddress", new Position(39.88, 116.3), "13581704277", 
    					mContext.getString(R.string.app_name), 3, 390, 
    					System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(),2, 
    					"GuestName", "13581704277");
    			for (int i = list.size(); i < maxDbSize; i++) {
    				System.out.println("I: " + i);
    				order.setId("" + i + i + i + i);
    				order.setHotelPoiUUID("0F2B4330-906A-11E2-A511-06973B18DA73");
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
        	new LoadThread(orders.size(), TKConfig.getPageSize()).start();
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.service_hotline_view:
                Utility.telephone(mSphinx, mServiceHotlineTxv);
                break;
                
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

            HotelOrder order = getItem(position);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView roomTypeTxv = (TextView) view.findViewById(R.id.room_type_txv);
            TextView checkinDateTxv = (TextView) view.findViewById(R.id.checkin_date_txv);
            TextView checkoutDateTxv = (TextView) view.findViewById(R.id.checkout_date_txv);
            TextView dayCountView = (TextView) view.findViewById(R.id.day_count_txv);

            nameTxv.setText(order.getHotelName());
            priceTxv.setText(Utility.formatHotelPrice(order.getTotalFee()));
            roomTypeTxv.setText(order.getRoomType());
            checkinDateTxv.setText(formatOrderListItemMonthDay(order.getCheckinTime() ) );
            checkoutDateTxv.setText(formatOrderListItemMonthDay(order.getCheckoutTime() ) );
            dayCountView.setText(mContext.getString(R.string.hotel_total_nights, Integer.valueOf(order.getDayCount())));
            view.findViewById(R.id.name_view).setOnClickListener(new GoThereListener(order));
            
            return view;
        }
        
    }
    
    class GoThereListener implements OnClickListener{

    	private HotelOrder mOrder;
    	
    	public GoThereListener(HotelOrder order){
    		mOrder = order;
    	}
    	
		@Override
		public void onClick(View v) {
			mActionLog.addAction(mActionTag+ActionLog.HotelOrderListGoThere);
	    	POI poi = mOrder.getPoi();
	        Utility.queryTraffic(mSphinx, poi, mActionTag);
		}
    	
    	
    	
    }
    
    private String formatOrderListItemMonthDay(long millis){
    	Date date = new Date(millis);
    	SimpleDateFormat dateformat=new SimpleDateFormat(mContext.getString(R.string.simple_month_day_format));
		return dateformat.format(date);
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        logi("onCancelled");
    }
    
    Handler mLoadOrderHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mResultLsv.onRefreshComplete(false);
			
			List<HotelOrder> ordersLoaded = (List<HotelOrder>) msg.obj;
			int ordersSize = 0;
			
			synchronized (orders) {
				orders.addAll(ordersLoaded);
				ordersSize = orders.size();
			}
			logi("OrderTotal: " + orderTotal);
			logi("OrdersSize: " + ordersSize);
			if(orderTotal > ordersSize){
				mResultLsv.changeHeaderViewByState(false, SpringbackListView.PULL_TO_REFRESH);
				mResultLsv.setFooterSpringback(true);
			}else{
				mResultLsv.setFooterSpringback(false);
			}
			
//			mResultLsv.setVisibility(View.VISIBLE);
			if(ordersSize > 0){
				mQueryingView.setVisibility(View.INVISIBLE);
				mEmptyView.setVisibility(View.INVISIBLE);
				hotelOrderAdapter.notifyDataSetChanged();
//				launchStateQuery();
			}else{
				System.out.println("No orders");
//				mResultLsv.setVisibility(View.INVISIBLE);
				mQueryingView.setVisibility(View.INVISIBLE);
				mEmptyView.setVisibility(View.VISIBLE);
			}
			
		}
    	
    };
    
    private static final int ORDER_STATE_QUREY_SIZE = 10;
    
    
    /**
     *  Get first at most {@code ORDER_STATE_QUREY_SIZE} orders from the list, put them into the query buffer
     *  concate their ids
     * @param ordersToStorage
     * @return
     */
    private String prepareIds(){
    	
    	StringBuffer sb = new StringBuffer();
    	int ordersAdded = 0;
    	synchronized (ordersToQuery) {
    		synchronized (ordersQuerying) {
    			for (int i=ordersQuerying.size()-1; i >= 0; i--) {
    				HotelOrder order = ordersQuerying.get(i);
    				if(ordersAdded>0){
    					sb.append("_");
    				}
    				sb.append(order.getId());
    				ordersAdded++;
				}
    			
    			for (int i=ordersQuerying.size(); 
    					i<ORDER_STATE_QUREY_SIZE && ordersToQuery.size()>0; 
    					i++) {
    				HotelOrder order = ordersToQuery.removeFirst();
    				ordersQuerying.add(order);
    				
    				if(ordersAdded>0){
    					sb.append("_");
    				}
    				sb.append(order.getId());
    				ordersAdded++;
    			}
    		}
    	}
		return sb.toString();
    	
    }
    
    
    /**
     * Launch make and launch state query from list {@code ordersQuerying}
     */
    private void launchStateQuery(){

    	System.out.println("launchStateQuery");
    	
    	String ids = prepareIds();
    	logi("Ids: " + ids);
    	sendStateQuery(ids);
        
    }

    /**
     * Set up state query
     * @param ids
     */
    protected void sendStateQuery(String ids){
    	if(TextUtils.isEmpty(ids)){
    		return;
    	}
    	logi("send state query for: " + ids);
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_QUERY);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_ORDER_IDS, ids);
    	HotelOrderOperation hotelOrderOperation = new HotelOrderOperation(mSphinx);
    	hotelOrderOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null);
    	mTkAsyncTasking = mSphinx.queryStart(hotelOrderOperation);
    	mBaseQuerying = mTkAsyncTasking.getBaseQueryList();
    }
    
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
            List<HotelOrder> ordersLoaded = null;
            HotelOrderTable table = null;
    		try {
    			table = new HotelOrderTable(mContext);
    			
    			orderTotal = table.count();
    			
				ordersLoaded = table.read(startIndex, loadCount);
    			logi("Order total: " + orderTotal);
    			logi("Orders loaded: " + ordersLoaded.size());
    			checkOrderStateForQuery(ordersLoaded);
 
			} catch (IOException e) {
				e.printStackTrace();
			} catch (APIException e) {
				e.printStackTrace();
			}finally{
				if(table!=null){
					table.close();
				}
			}
        		
            msg.obj = ordersLoaded;
            mLoadOrderHandler.sendMessage(msg);
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
    
    /**
     * Add orders whoes state need querying to list {@code ordersToQuery}
     * @param ordersLoaded
     */
    private void checkOrderStateForQuery(List<HotelOrder> ordersLoaded){
    	
		for (HotelOrder hotelOrder : ordersLoaded) {
			int state = hotelOrder.getState();
			switch (state) {
				case HotelOrder.STATE_PROCESSING:	//订单处理中
				case HotelOrder.STATE_SUCCESS:	//预订成功哦你
					ordersToQuery.addLast(hotelOrder);
					break;
			}
		}
		System.out.println("OrdersToQuery size: " + ordersToQuery.size());
    }
    
	@Override
	public void onPause() {
		super.onPause();
		if(mTkAsyncTasking != null){
			mTkAsyncTasking.stop();
		}
	}

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        System.out.println("onPostExecute");
        
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        

        if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, BaseActivity.SHOW_ERROR_MSG_DIALOG, this, true)) {
            return;
        }
        
        final HotelOrderOperation hotelOrderOperation = (HotelOrderOperation)(tkAsyncTask.getBaseQuery());
        String opCode = hotelOrderOperation.getCriteria().get(HotelOrderOperation.SERVER_PARAMETER_OPERATION_CODE);
        if(opCode.equals(HotelOrderOperation.OPERATION_CODE_SYNC)){
        	// Get the orders loaded from server
        	HotelOrderSyncResponse response = (HotelOrderSyncResponse) hotelOrderOperation.getResponse();
        	List<HotelOrder> orders = response.getOrders();
        	
        	// If there exists orders to sync
        	if(orders!=null){
        		logi("Orders got: " + orders.size());
        		// write to database
        		boolean isExceptExists = false;
        		
        		if(orders.size()!=0){
        			
        			HotelOrderTable table = null;
        			try {
        				table = new HotelOrderTable(mContext);
        				for (HotelOrder hotelOrder : orders) {
        					logi("Write A Order. Id: " + hotelOrder.getId());
        					table.write(hotelOrder);
        				}
        				
        				// update the UI
        				clearOrders();
        				reloadOrders();
        				
        			} catch (Exception e) {
        				isExceptExists = true;
        				e.printStackTrace();
        			}finally{
        				if(table !=null){
        					table.close();
        				}
        			}
        		}
        		
        		if(!isExceptExists){
        			//clear the anomaly exists flag
        			TKConfig.setPref(mContext, TKConfig.PREFS_HOTEL_ORDER_COULD_ANOMALY_EXISTS, "no");
        		}
        		
        	}
        	
        }else if(opCode.equals(HotelOrderOperation.OPERATION_CODE_QUERY)){
        	HotelOrderStatesResponse response = (HotelOrderStatesResponse) hotelOrderOperation.getResponse();
        	List<Long> states = response.getStates();
        	String ids = baseQuery.getCriteria().get(HotelOrderOperation.SERVER_PARAMETER_ORDER_IDS);
        	updateOrderState(states, ids, ordersQuerying);
        }
        
        
        /**
         * Query remaining orders～
         */
//        launchStateQuery();
        
    }
    
    private LinkedList<HotelOrder> ordersToUpdateToDB = new LinkedList<HotelOrder>();
    
    /**
     * Update the states of the orders using states
     * @param states
     * @param ids
     * @param orders
     */
    public void updateOrderState(List<Long> states, String ids, List<HotelOrder> orders){
    	
    	updateOrderInMemory(states, ids, orders);
    	
    	new DBUpdateThread(ordersToUpdateToDB).start();
    }
	
    /**
     * Update orders in memory
     * Put orders updated to {@code ordersToUpdateToDB} 
     * @param states
     * @param ids
     * @param orders
     */
    private void updateOrderInMemory(List<Long> states, String ids, List<HotelOrder> orders){

        long stateUpdateTime = System.currentTimeMillis();
    	String[] idArray = ids.split("_");

    	// for each order get the its id position 
    	// set the state and update time, put it into db thread
    	synchronized (ordersQuerying) {
    		synchronized (ordersToUpdateToDB) {
    			
    			for(int i=orders.size()-1; i>=0; i--){
    				
    				HotelOrder order = orders.get(i);
    				
    				//Get order id index and update order
    				for (int j = idArray.length-1; j >= 0; j--) {
    					if(idArray[j].equals( order.getId()) ){
    						if(states.get(j)!=HotelOrder.STATE_NONE && j<states.size()){
    							order.setState(states.get(j).intValue());
    							order.setStateUpdateTime(stateUpdateTime);
    							ordersToUpdateToDB.addLast(order);
    							ordersQuerying.remove(order);
    						}
    						break;
    					}
    					
    				}//end j for
    				
    			}// end i for
    			
    		}//end sync
		}
    	
    }// end updateOrderMemory
    

    /**
     * Update the storage for orders whoes state is updated
     * @param ordersToUpdateToDB
     */
    protected void updateOrderStorage(List<HotelOrder> ordersToUpdateToDB){
    	
    	HotelOrderTable table = null;
    	try {
    		table = new HotelOrderTable(mContext);
    		for (HotelOrder order : ordersToUpdateToDB) {
    			table.update(order);
    		}
    	} catch (Exception e) {
    	}finally{
    		if(table!=null){
    			table.close();
    		}
    	}
    }// end updateOrderStorage
    
    
    private class DBUpdateThread extends Thread{

    	private List<HotelOrder> ordersToStorage;
    	
		public DBUpdateThread(List<HotelOrder> ordersToUpdateToDB) {
			super();
			this.ordersToStorage = ordersToUpdateToDB;
		}

		@Override
		public void run() {
			
			if(ordersToStorage !=null){
				logi("Update thread");
				synchronized (ordersToStorage) {
					updateOrderStorage(ordersToStorage);
					ordersToStorage.clear();
				}
			}
			
		}//end run
    	
    }//end DBUpdateThread

	@Override
	public String getLogTag() throws Exception {
		return TAG;
	}
    
}

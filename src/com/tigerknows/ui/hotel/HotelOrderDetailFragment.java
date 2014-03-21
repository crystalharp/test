/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKFragmentManager;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.model.HotelOrderOperation;
import com.tigerknows.model.HotelOrderOperation.HotelOrderStatesResponse;
import com.tigerknows.model.HotelVendor;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.provider.HotelOrderTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.discover.DiscoverChildListFragment;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;

/**
 * @author Peng Wenyue
 */
public class HotelOrderDetailFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "HotelOrderDetailFragment";

    public HotelOrderDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private HotelOrder mOrder;
    private int mPosition;

    // One minute for order update
	private static final long ORDER_UPDATE_INTEVAL = 60*1000;

    private TextView mHotelNameTxv;
    private TextView mDistanceTxv;
    private TextView mDistanceFromTxv = null;
    private TextView mHotelAddressTxv;
    private TextView mHotelTelTxv;
    private TextView mOrderIdTxv;
    private TextView mOrderStateTxv;
    private TextView mOrderTimeTxv;
    private TextView mTotalFeeTxv;
    private TextView mPayTypeTxv;
    private TextView mCheckinDateTxv;
    private TextView mCheckoutDateTxv;
    private TextView mRetentionDateTxv;
    private TextView mRoomTypeTxv;
    private TextView mCheckinPersonTxv;
    
    private TextView mComeFromTxv;
    
    private Button mBtnCancel;
    private Button mBtnOrderAgain;
    private Button mBtnIssueComment;
    private Button mBtnDeleteOrder;

    private View mNavigationBar;
    
	private View mHotelTelView;

	private View mHotelAddressView;
	
	private ScrollView mScrollView;

	private View mNameView;
	
	private HotelVendor mHotelVendor;
	
	private Handler mHandler = null;

    Drawable icOrderAgain;
    Drawable icOrderAgainDisabled;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        Resources resources = mSphinx.getResources();
        icOrderAgain = resources.getDrawable(R.drawable.ic_order_again);
        icOrderAgainDisabled = resources.getDrawable(R.drawable.ic_order_again_disabled);
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_detail, container, false);

        findViews();
        setListener();
        mDistanceTxv.setVisibility(View.INVISIBLE);
        mNameView.setBackgroundResource(R.drawable.list_header_title);
        mActionTag = ActionLog.HotelOrderDetail;

    	mHotelNameTxv.setSingleLine(false);
    	
        return mRootView;
    }
    @Override
    public void dismiss() {
        super.dismiss();
    }
    
    @Override
    protected void findViews() {
        super.findViews();
		mHotelNameTxv = (TextView) mRootView.findViewById(R.id.name_txv);
		mDistanceTxv = (TextView) mRootView.findViewById(R.id.distance_txv);
        mDistanceFromTxv = (TextView) mRootView.findViewById(R.id.distance_from_txv);
		mHotelAddressTxv = (TextView) mRootView.findViewById(R.id.address_txv);
		mHotelTelTxv = (TextView) mRootView.findViewById(R.id.telephone_txv);
		mOrderIdTxv = (TextView) mRootView.findViewById(R.id.order_id_txv);
		mOrderStateTxv = (TextView) mRootView.findViewById(R.id.order_state_txv);
		mOrderTimeTxv = (TextView) mRootView.findViewById(R.id.order_time_txv);
		mTotalFeeTxv = (TextView) mRootView.findViewById(R.id.order_total_fee_txv);
		mPayTypeTxv = (TextView) mRootView.findViewById(R.id.pay_type_txv);
		mCheckinDateTxv = (TextView) mRootView.findViewById(R.id.checkin_date_txv);
		mCheckoutDateTxv = (TextView) mRootView.findViewById(R.id.checkout_date_txv);
		mRetentionDateTxv = (TextView) mRootView.findViewById(R.id.retention_date_txv);
		mRoomTypeTxv = (TextView) mRootView.findViewById(R.id.room_type_txv);
		mCheckinPersonTxv = (TextView) mRootView.findViewById(R.id.checkin_person_txv);
		mComeFromTxv = (TextView) mRootView.findViewById(R.id.come_from_txv);
		
		mHotelAddressView = mRootView.findViewById(R.id.address_view);
		mHotelTelView = mRootView.findViewById(R.id.telephone_view);
		
		mBtnCancel = (Button) mRootView.findViewById(R.id.btn_cancel_order);
		mBtnIssueComment = (Button) mRootView.findViewById(R.id.btn_issue_comment);
		mBtnOrderAgain = (Button) mRootView.findViewById(R.id.btn_order_again);
		mBtnDeleteOrder = (Button) mRootView.findViewById(R.id.btn_delete_order);
		
		mNavigationBar = mRootView.findViewById(R.id.navigation_widget);
		
		mScrollView = (ScrollView) mRootView.findViewById(R.id.body_scv);
		mNameView = mRootView.findViewById(R.id.name_view);
		mNameView.setBackgroundResource(R.drawable.list_header_title);
    }

    @Override
    protected void setListener() {
    	super.setListener();
    	mBtnCancel.setOnClickListener(mCancelOnClickListener);
    	mBtnIssueComment.setOnClickListener(mIssueCommentOnClickListener);
    	mBtnOrderAgain.setOnClickListener(mJumpToPOIClickListener);
    	mBtnDeleteOrder.setOnClickListener(mDeleteOrderOnClickListener);
    	
    	mHotelAddressView.setOnClickListener(this);
    	mHotelTelView.setOnClickListener(this);
    	
    	mNameView.setOnClickListener(mJumpToPOIClickListener);
    }
    
    private OnClickListener mCancelOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			LogWrapper.i(TAG, "Cancel order. Id: " + mOrder.getId());
			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailCancel);
			Utility.showNormalDialog(mSphinx, getString(R.string.hotel_order_cancel_confirm), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

                    if (which == DialogInterface.BUTTON_POSITIVE) {
            			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailCancelDialogYes);
                    	// Send cancel order request to server
                    	sendCancelRequest();
                    }else if(which == DialogInterface.BUTTON_NEGATIVE ){
            			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailCancelDialogNo);
                    	
                    }
				}
				
			});
			
		}
	};
	
	private void sendCancelRequest(){

    	HotelOrderOperation hotelOrderOperation = new HotelOrderOperation(mSphinx);
    	hotelOrderOperation.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_UPDATE);
    	hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_ORDER_ID, mOrder.getId());
    	hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_UPDATE_ACTION, HotelOrderOperation.ORDER_UPDATE_ACTION_CANCEL);
    	hotelOrderOperation.setup(getId(), getId(), getString(R.string.doing_and_wait));
    	mTkAsyncTasking = mSphinx.queryStart(hotelOrderOperation);
    	mBaseQuerying = mTkAsyncTasking.getBaseQueryList();

	}
    
	private DataQuery createCommentQuery(){

        DataQuery commentQuery = new DataQuery(mSphinx);
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mOrder.getHotelPoiUUID());
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
        commentQuery.addParameter(DataQuery.SERVER_PARAMETER_SIZE, "1");
        commentQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false);
        
		return commentQuery;
        
	}
	
    private OnClickListener mIssueCommentOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			LogWrapper.i(TAG, "Issue Comment");
			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailIssueComment);
			List<BaseQuery> queries = new ArrayList<BaseQuery>(2);
			queries.add(createPOIQuery());
			queries.add(createCommentQuery());
        	mTkAsyncTasking = mSphinx.queryStart(queries);
        	mBaseQuerying = mTkAsyncTasking.getBaseQueryList();
			
		}
	};
    
    private OnClickListener mJumpToPOIClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			LogWrapper.i(TAG, "Order Again");
        	mTkAsyncTasking = mSphinx.queryStart(createPOIQuery());
        	mBaseQuerying = mTkAsyncTasking.getBaseQueryList();
        	if(v==mNameView){
    			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailClickName);
        	}else if(v==mBtnOrderAgain){
    			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailOrderAgain);
        	}
		}
	};
    
    private OnClickListener mDeleteOrderOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			LogWrapper.i(TAG, "Delete order action");

			mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailDelete);
			
			Utility.showNormalDialog(mSphinx, getString(R.string.hotel_order_delete_confirm), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

                    if (which == DialogInterface.BUTTON_POSITIVE) {
                    	mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailDeleteDialogYes);
            			deleteOrder();
                    }else {
                    	mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailDeleteDialogNo);
                    }
				}
				
			});
		}
	};
	
	private void deleteOrder(){
		try {
			HotelOrderTable table = new HotelOrderTable(mContext);
			table.delete(mOrder.getId());
			table.close();
			if(getId() == TKFragmentManager.ID_view_hotel_order_detail_2 && mSphinx.uiStackContains(TKFragmentManager.ID_view_hotel_order_detail)){
				Message msg = new Message();
				msg.what = Sphinx.HOTEL_ORDER_DELETE_SYNC;
				msg.obj = mOrder.getId();
				mSphinx.getHandler().sendMessage(msg);
			}
			mSphinx.getHotelOrderListFragment().removeOrder(mOrder);
			dismiss();
			Toast.makeText(mSphinx, R.string.hotel_order_delete_success, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(mSphinx, R.string.hotel_order_delete_success, Toast.LENGTH_SHORT).show();
		}
	}
    
	private void updateCancelBtn(){
		// show or hide cancel order button according to order state
		int state = mOrder.getState();
        if(CalendarUtil.getExactTime(mContext) > mOrder.getCancelDeadline()){
        	mBtnCancel.setVisibility(View.GONE);
        	return;
        }
        switch (state) {
        case 0:
        	// do nothing
        	break;
		case HotelOrder.STATE_PROCESSING:
		case HotelOrder.STATE_SUCCESS:
			mBtnCancel.setVisibility(View.VISIBLE);
			break;

		default:
			mBtnCancel.setVisibility(View.GONE);
			break;
		}
	}
	
    @Override
    public void onResume() {
        super.onResume();
        
        // set title fragment content
        mTitleBtn.setText(getString(R.string.hotel_order_detail));
        if(mHotelVendor == null || TextUtils.isEmpty(mHotelVendor.getServiceName())){
        }else{
            mRightBtn.setVisibility(View.VISIBLE);
            mRightBtn.setText(mHotelVendor.getServiceName());
            mRightBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailServiceTel);
                    Utility.telephone(mSphinx, mHotelVendor.getServiceTel());
				}
			});
        }
        if(getId() == TKFragmentManager.ID_view_hotel_order_detail_2){
            icOrderAgainDisabled.setBounds(0, 0, icOrderAgainDisabled.getIntrinsicWidth(), icOrderAgainDisabled.getIntrinsicHeight());
        	mBtnOrderAgain.setCompoundDrawables(null, icOrderAgainDisabled, null, null);
        	mBtnOrderAgain.setEnabled(false);
        }else{
            icOrderAgain.setBounds(0, 0, icOrderAgain.getIntrinsicWidth(), icOrderAgain.getIntrinsicHeight());
            mBtnOrderAgain.setCompoundDrawables(null, icOrderAgain, null, null);
        	mBtnOrderAgain.setEnabled(true);
        }
        updateCancelBtn();
        
        // If order state if out-of-date, query the state of the current order
        long curTime = CalendarUtil.getExactTime(mContext);
        if(( curTime - mOrder.getStateUpdateTime() ) > ORDER_UPDATE_INTEVAL){
        	sendStateQuery(""+mOrder.getId());
        }

        mScrollView.smoothScrollTo(0, 0);
    }

    /**
     * Set up state query
     * @param ids
     */
    protected void sendStateQuery(String ids){
    	if(TextUtils.isEmpty(ids)){
    		return;
    	}
    	LogWrapper.i(TAG, "send state query for: " + ids);
    	HotelOrderOperation hotelOrderOperation = new HotelOrderOperation(mSphinx);
    	hotelOrderOperation.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_QUERY);
    	hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_ORDER_IDS, ids);
    	hotelOrderOperation.setup(getId(), getId(), null);
    	mTkAsyncTasking = mSphinx.queryStart(hotelOrderOperation);
    	mBaseQuerying = mTkAsyncTasking.getBaseQueryList();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.telephone_view:
            	mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailPhone);
            	Utility.telephone(mSphinx, mHotelTelTxv);
            	break;
            	
            case R.id.address_view:
            	mActionLog.addAction(mActionTag +  ActionLog.CommonAddress);
            	POI poi = mOrder.getPoi();
                Utility.queryTraffic(mSphinx, poi, mActionTag);
            	break;
            	
            case R.id.right_btn:
            	mActionLog.addAction(mActionTag + ActionLog.HotelOrderDetailServiceTel);
                Utility.telephone(mSphinx, mRightBtn);
                break;
            	
            default:
                break;
        }
    }

    /**
     * Before showing the view, {@code setData()} first. 
     * @param order
     */
    public void setData(HotelOrder order, int position){
    	mOrder = order;
    	mPosition = position;
    	if(mOrder==null){
    		return;
    	}
    	
    	DiscoverChildListFragment.showPOI(mContext, order.getHotelName(), null, order.getHotelAddress(), mOrder.getHotelTel(), mHotelNameTxv, mDistanceFromTxv, mDistanceTxv, 
    			mRootView.findViewById(R.id.address_view), mRootView.findViewById(R.id.telephone_view)
    			, mHotelAddressTxv, mHotelTelTxv, R.drawable.list_middle, R.drawable.list_footer, R.drawable.list_footer);
    	mOrderIdTxv.setText(order.getId().split("\\$")[0]);
    	mOrderStateTxv.setText(getOrderStateDesc(order.getState()));
    	mOrderTimeTxv.setText(formatOrderTime(order.getCreateTime()));
    	mTotalFeeTxv.setText(Utility.formatHotelPrice(order.getTotalFee()) + getString(R.string.rmb_text) );
    	mPayTypeTxv.setText(getString(R.string.hotel_order_default_pay_type));
    	mCheckinDateTxv.setText(formatOrderTime(order.getCheckinTime()));
    	mCheckoutDateTxv.setText(formatOrderTime(order.getCheckoutTime()));
    	mRetentionDateTxv.setText(CalendarUtil.ymd8c_Hm4.format(new Date(order.getRetentionTime())));
    	mRoomTypeTxv.setText(order.getRoomType());
    	mCheckinPersonTxv.setText(order.getGuestName());
    	
        mHotelVendor = HotelVendor.getHotelVendorById(mOrder.getVendorID(), mSphinx, null);
        if(mHotelVendor == null || TextUtils.isEmpty(mHotelVendor.getName())){
        	mComeFromTxv.setVisibility(View.GONE);
        }else{
        	mComeFromTxv.setText(getString(R.string.this_come_from_colon, mHotelVendor.getName()));
        	mComeFromTxv.setVisibility(View.VISIBLE);
        }
    }
    
    private static int[] orderStateDescResId = new int[]{
    	R.string.order_state_processing,
    	R.string.order_state_success,
    	R.string.order_state_canceled,
    	R.string.order_state_post_due,
    	R.string.order_state_checked_in
    };
    
    public String getOrderStateDesc(int state){
        if (state < 1 || state > 5) {
            state = 1;
        }
        
        return getString(orderStateDescResId[state-1]);
    }
    
    public static String formatOrderTime(long millis){
    	Date date = new Date(millis);
    	SimpleDateFormat dateformat=new SimpleDateFormat("yyyy-MM-dd");
		return dateformat.format(date);
    }

    /**
     * Update the storage for orders whoes state is updated
     * @param ordersToUpdateToDB
     */
    protected void updateOrderStorage(HotelOrder ordersToUpdateToDB){
    	
    	HotelOrderTable table = null;
    	try {
    		table = new HotelOrderTable(mContext);
			table.update(ordersToUpdateToDB);
    	}catch (Exception e) {
    	}finally{
    		if(table!=null){
    			table.close();
    		}
    	}
    }// end updateOrderStorage

	@Override
	public void onPause() {
		if(getId() == TKFragmentManager.ID_view_hotel_order_detail_2){
			Message msg = new Message();
			msg.what = Sphinx.HOTEL_ORDER_OLD_SYNC;
			msg.arg1 = mOrder.getState();
			msg.obj = mOrder.getId();
			mSphinx.getHandler().sendMessage(msg);
		}
		super.onPause();
	}

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        
        LogWrapper.i(TAG, "On cancel.");
        
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        LogWrapper.i(TAG, "On postExecute");
        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        LogWrapper.i(TAG, "Response: " + tkAsyncTask.getBaseQuery().getResponse());
        for (BaseQuery baseQuery : baseQueryList) {
        	// TODO: if query is state query, don't show dialogs?
	        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(TKFragmentManager.ID_view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
	            isReLogin = true;
	            return;
	        } else {
	        	
	        	int msgType = TKActivity.SHOW_TOAST;
	        	if(BaseQuery.API_TYPE_HOTEL_ORDER.equals(tkAsyncTask.getBaseQuery().getAPIType()) &&
	        			HotelOrderOperation.OPERATION_CODE_QUERY.equals(tkAsyncTask.getBaseQuery().getParameter(HotelOrderOperation.SERVER_PARAMETER_OPERATION_CODE))){
	        		msgType = TKActivity.SHOW_NOTHING;					// 状态查询不需要提示。
	        	}
	        	
	            if(BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, msgType, this, false)) {
	                return;
	            }
	        }
        }
        
        if(baseQueryList.size() == 2){			//Now only comment action will have 2 queries
        	
        	POI onlinePOI = null;
        	
        	for (BaseQuery baseQuery : baseQueryList) {
        		
				String at = baseQuery.getAPIType();

            	String dty = baseQuery.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
            	
	        	if(BaseQuery.API_TYPE_DATA_QUERY.equals(at)){			//数据操作
	            	
	            	if(BaseQuery.DATA_TYPE_DIANPING.equals(dty) && onlinePOI!=null){			//点评返回数据
	            		CommentResponse response = (CommentResponse) baseQuery.getResponse();
	            		
	            		if(response!=null){	//jump to comment editing activity
	            			
	            			// Check and get my comment
	            	        Comment myComment = null;
	        	            CommentList commentList = response.getList();
	        	            if (commentList != null) {
	        	                List<Comment> list = commentList.getList();
	        	                if (list != null) {
	    	                        Comment comment = list.get(0);
	    	                        if (Comment.isAuthorMe(comment) > 0) {
	    	                            myComment = comment;
	    	                        }
	        	                }
	        	            }
	        	            onlinePOI.setMyComment(myComment);
	    	            	// jump to edit comment
        	            	EditCommentActivity.setPOI(onlinePOI, getId(), myComment != null ? EditCommentActivity.STATUS_MODIFY : EditCommentActivity.STATUS_NEW);
        	            	mSphinx.showView(R.id.activity_poi_edit_comment);
	            			
	            		}else{
	            			// No data is get
	            			// TODO show error to user
	            			Toast.makeText(mSphinx, TAG+" Network error", Toast.LENGTH_SHORT).show();
	            		}
	            		
	            	}
	
	            }else if(BaseQuery.API_TYPE_DATA_OPERATION.equals(at)){			//数据操作, 对应酒店poi
	            	LogWrapper.i(TAG, "Hotel response");
            		if (BaseQuery.DATA_TYPE_POI.equals(dty)) {
                    	POIQueryResponse response = (POIQueryResponse) baseQuery.getResponse();
                    	onlinePOI = response.getPOI();
                    }
            		
            	}// end API type decisions
        	
			}
        }else if(baseQueryList.size() == 1){
        	
        	BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
			String at = baseQuery.getAPIType();
            if(BaseQuery.API_TYPE_HOTEL_ORDER.equals(at)){
            	
            	HotelOrderOperation hotelOrderOperation = (HotelOrderOperation)baseQuery;
            	String opCode = hotelOrderOperation.getParameter(HotelOrderOperation.SERVER_PARAMETER_OPERATION_CODE);
            	
            	if(HotelOrderOperation.OPERATION_CODE_QUERY.equals(opCode)){	// Query operation corresponds to state query
            		HotelOrderStatesResponse response = (HotelOrderStatesResponse) hotelOrderOperation.getResponse();
            		if(response == null){
                    	LogWrapper.e(TAG, "Response null!");
            			return;
            		}
            		
            		List<Long> states = response.getStates();
            		// here only one order is queried, so exactly state will be returned
            		if(states != null && states.size()!=0 && states.get(0) != -1){
            			mOrder.setState(states.get(0).intValue());
            			mOrder.setStateUpdateTime(CalendarUtil.getExactTime(mContext));
            			updateOrderStorage(mOrder);
            			mOrderStateTxv.setText(getOrderStateDesc(mOrder.getState()));
            	        updateCancelBtn();
            		}
            		
            	}else if( HotelOrderOperation.OPERATION_CODE_UPDATE.equals(opCode) ){
            		
            		String action = baseQuery.getParameter(HotelOrderOperation.SERVER_PARAMETER_UPDATE_ACTION);
            		if(HotelOrderOperation.ORDER_UPDATE_ACTION_CANCEL.equals(action)){
            			Response response = hotelOrderOperation.getResponse();
            			if(response.getResponseCode() == 200){
            				// 取消成功
            				/* 更新订单
            				 * 更新界面
            				 * 提示用户
            				 */
            				mOrder.setState(HotelOrder.STATE_CANCELED);
            				mOrderStateTxv.setText(getOrderStateDesc(mOrder.getState()));
            				mBtnCancel.setVisibility(View.GONE);
                			updateOrderStorage(mOrder);
                			if(mHandler != null){
                				new Thread(){
                					public void run(){
                        				Message msg = new Message();
                        				msg.what = HotelOrderListFragment.MSG_CANCEL_ORDER;
                        				msg.arg1 = mPosition;
                        				mHandler.sendMessage(msg);                						
                					}
                				}.start();
                			}
            				Toast.makeText(mContext, R.string.hotel_order_cancel_success, Toast.LENGTH_LONG).show(); 
            				
            			}else{
            				// 取消失败
            				Toast.makeText(mContext, R.string.hotel_order_cancel_failed, Toast.LENGTH_LONG).show(); 
            			}
            			
            		}// end if
            		
            	}// end else if
            	
            	
            }else if(BaseQuery.API_TYPE_DATA_OPERATION.equals(at)){
            	// 一个数据操作的情况是： 再订一单。跳转到酒店POI详情界面。
            	POIQueryResponse response = (POIQueryResponse) baseQuery.getResponse();
            	POI hotelPoi = response.getPOI();
                mSphinx.getPOIDetailFragment().setData(hotelPoi, mPosition);
                mSphinx.showView(TKFragmentManager.ID_view_poi_detail);

            }//end API type decision
            
        }//end query count decision
        
    }

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private DataOperation createPOIQuery(){
        DataOperation poiQuery = new DataOperation(mSphinx);
        poiQuery.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_HOTEL);
        poiQuery.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        poiQuery.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, mOrder.getHotelPoiUUID());
        poiQuery.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(CalendarUtil.getExactTime(mContext));
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_CHECKIN, SIMPLE_DATE_FORMAT.format(today.getTime()));
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_CHECKOUT, SIMPLE_DATE_FORMAT.format(tomorrow.getTime()));
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_HOTEL_SOURCE, HotelVendor.ALL);
        
        int cityId= MapEngine.getCityId(mOrder.getPosition());
        poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        poiQuery.setCityId(cityId);
        return poiQuery;
    }
    
    public void setStageIndicatorVisible(boolean visible){
   		mNavigationBar.setVisibility( visible? View.VISIBLE:View.GONE );
    }

	@Override
	public String getLogTag() throws Exception {
		return TAG;
	}
	
	public void handleMessage(Message msg){
		if(msg.what == Sphinx.HOTEL_ORDER_OLD_SYNC){
			String id = msg.obj.toString();
			if(TextUtils.equals(id, mOrder.getId())){
				mOrder.setState(msg.arg1);
			}
		}else if(msg.what == Sphinx.HOTEL_ORDER_DELETE_SYNC){
			String id = msg.obj.toString();
			if(TextUtils.equals(id, mOrder.getId())){
				mSphinx.uiStackRemove(getId());
			}
		}
	}
    
}

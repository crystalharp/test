/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.HotelOrderOperation;
import com.tigerknows.model.HotelOrderOperation.HotelOrderCreateResponse;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.model.POI;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic.DanbaoGuize;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic.DanbaoGuize.RetentionTime;
import com.tigerknows.model.Response;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

public class HotelOrderWriteFragment extends BaseFragment implements View.OnClickListener{

    public HotelOrderWriteFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    static final String TAG = "HotelOrderWriteFragment";
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    private ScrollView mHotelOrderWriteScv;
    private TextView mHotelNameTxv;
    private TextView mRoomtypeTxv;
    private TextView mRoomtypeDetailTxv;
    private TextView mRoomDateTxv;
    private Button mRoomHowmanyBtn;
    private Button mRoomReserveBtn;
    private EditText mRoomPersonEdt;
    private EditText mRoomMobileNumberEdt;
    private Button mSubmitOrderBtn;
    
    private POI mPOI;
    private Hotel mHotel;
    private HotelOrder mHotelOrder;
    private RoomType mRoomType;
    private RoomTypeDynamic mRoomtypeDynamic;
    private Calendar mCheckIn;
    private Calendar mCheckOut;

    private static final long MAX_ROOM_HOWMANY = 5;
    
    private int mRTimeWhich;
    private String mRTime;
    private String mRTimeDetail;
    private long mRoomHowmany;
    private String mTotalPrice;
    private String mUsername;
    private String mMobile;
    private long mNeedCreditAssure;
    private long mTypeCreditAssure;
    private String mCreditCardNo;
    private String mVerifyCode;
    private String mValidYear;
    private String mValidMonth;
    private String mCardHoldName;
    private String mIdCardType;
    private String mIdCardNo;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //mActionTag = ActionLog.HotelOrderWrite;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_write, container, false);
        
        findViews();
        setListener();
        mRoomHowmany = 1;
        mRTimeWhich = 0;
        return mRootView;
    }
    
    public void onResume(){
    	super.onResume();
    	refreshData();

    }
    
    public void onPause(){
        super.onPause();
    }

    protected void findViews() {
        mHotelOrderWriteScv = (ScrollView) mRootView.findViewById(R.id.hotel_order_write_scv);
        mHotelNameTxv = (TextView) mRootView.findViewById(R.id.hotel_name_txv);
        mRoomtypeTxv = (TextView) mRootView.findViewById(R.id.roomtype_txv);
        mRoomtypeDetailTxv = (TextView) mRootView.findViewById(R.id.roomtype_detail_txv);
        mRoomDateTxv = (TextView) mRootView.findViewById(R.id.room_date_txv);
        mRoomHowmanyBtn = (Button) mRootView.findViewById(R.id.room_howmany_btn);
        mRoomReserveBtn = (Button) mRootView.findViewById(R.id.room_reserve_btn);
        mRoomPersonEdt = (EditText) mRootView.findViewById(R.id.room_person_edt);
        mRoomMobileNumberEdt = (EditText) mRootView.findViewById(R.id.room_mobile_number_edt);
        mSubmitOrderBtn = (Button) mRootView.findViewById(R.id.submit_order_btn);
    }

    protected void setListener() {
        // TODO Auto-generated method stub
        mHotelOrderWriteScv.setOnClickListener(this);
        mHotelNameTxv.setOnClickListener(this);
        mRoomtypeTxv.setOnClickListener(this);
        mRoomtypeDetailTxv.setOnClickListener(this);
        mRoomDateTxv.setOnClickListener(this);
        mRoomHowmanyBtn.setOnClickListener(this);
        mRoomReserveBtn.setOnClickListener(this);
        mRoomPersonEdt.setOnClickListener(this);
        mRoomMobileNumberEdt.setOnClickListener(this);
        mSubmitOrderBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view){
        int id = view.getId();
        switch (id) {
        case R.id.left_btn:
        	exit();
        	break;
        case R.id.room_howmany_btn:
        	showRoomHowmanyDialog();
            break;
        case R.id.room_reserve_btn:
        	showRoomReserveDialog();
            break;
        case R.id.submit_order_btn:
        	String str = mRoomPersonEdt.getText().toString().trim();
        	if(TextUtils.isEmpty(str)){
        		mRoomPersonEdt.requestFocus();
        		Toast.makeText(mContext, mSphinx.getString(R.string.hotel_room_person_empty_tip), Toast.LENGTH_SHORT).show();
        		mSphinx.showSoftInput();
        		return;
        	}else{
        		mUsername = str;
        	}
        	str = mRoomMobileNumberEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		mRoomMobileNumberEdt.requestFocus();
        		Toast.makeText(mContext, mSphinx.getString(R.string.hotel_room_mobile_empty_tip), Toast.LENGTH_SHORT).show();
        		mSphinx.showSoftInput();
        		return;
        	}else{
        		mMobile = str;
        	}
        	submit(false);

            break;
        default:
        	mSphinx.hideSoftInput();
            break;
        }
    }
    private void refreshData(){
    	mTotalPrice = (mRoomtypeDynamic.getPrice() * mRoomHowmany) + "";
    	mRoomHowmanyBtn.setText(mSphinx.getString(R.string.room_howmany_item, mRoomHowmany, mTotalPrice));
    	final List<RetentionTime> rtList = findRTimeByRoomHowmany(mRoomHowmany);
    	if(rtList.isEmpty()){
    		mNeedCreditAssure = 0;
    		mTypeCreditAssure = 0;
    		mRTime = "24点之前";
    		mRTimeDetail = SIMPLE_DATE_FORMAT.format(mCheckIn.getTime()) + " 23:59:00";
    	}else{
    		mRTime = rtList.get(mRTimeWhich).getTime();
    		mRTimeDetail = rtList.get(mRTimeWhich).getTimeDetail();
    		mNeedCreditAssure = rtList.get(mRTimeWhich).getNeed();
    		mTypeCreditAssure = rtList.get(mRTimeWhich).getType();
    	}
    	mRoomReserveBtn.setText(mRTime);
    }
    
    public void setData(POI poi, RoomType roomtype, RoomTypeDynamic roomTypeDynamic, Calendar checkIn, Calendar checkOut ) {

    	mPOI = poi;
    	mHotel = poi.getHotel();
    	mRoomType = roomtype;
    	mRoomtypeDynamic = roomTypeDynamic;
    	mHotelNameTxv.setText(mPOI.getName());
    	mRoomtypeTxv.setText(mRoomType.getRoomType());
    	String roomTypeDetail="";
    	String appendContent;
        appendContent = mRoomType.getBedType();
        roomTypeDetail += appendContent;
        roomTypeDetail += (appendContent != null) ? " " : null;
        appendContent = mRoomType.getBreakfast();
        roomTypeDetail += appendContent;
        roomTypeDetail += (appendContent != null) ? " " : null;
        appendContent = mRoomType.getNetService();
        roomTypeDetail += appendContent;
        roomTypeDetail += (appendContent != null) ? " " : null;
        appendContent = mRoomType.getFloor();
        roomTypeDetail += appendContent;
        roomTypeDetail += (appendContent != null) ? " " : null;
        appendContent = mRoomType.getArea();
        roomTypeDetail += appendContent;
        roomTypeDetail += (appendContent != null) ? " " : null;
        mRoomtypeDetailTxv.setText(roomTypeDetail);
        mRoomDateTxv.setText(mSphinx.getString(R.string.hotel_room_date,
        	    checkIn.get(Calendar.MONTH),
        		checkIn.get(Calendar.DATE),
        	    checkOut.get(Calendar.MONTH),
        		checkOut.get(Calendar.DATE)
        		));
        mCheckIn = checkIn;
        mCheckOut = checkOut;
    }

	private void exit() {
		// TODO Auto-generated method stub
		
	}
    private void showRoomHowmanyDialog(){
        final List<String> list = new ArrayList<String>();
        //TODO: mActionlog
        
        long listsize = (mRoomtypeDynamic.getNum() > MAX_ROOM_HOWMANY) ? MAX_ROOM_HOWMANY : mRoomtypeDynamic.getNum();
        String listitem;
        for(long i = 1; i <= listsize; i++){
            listitem = mSphinx.getString(R.string.room_howmany_item, i, mRoomtypeDynamic.getPrice()*i + "");
        	list.add(listitem);
        }
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list);
        ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        final Dialog dialog = Utility.showNormalDialog(mSphinx, 
        		mSphinx.getString(R.string.choose_room_howmany), 
        		null,
        		listView,
        		null,
        		null,
        		null);
        listView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
            	mRoomHowmany = which + 1;
            	mRTimeWhich = 0;
            	refreshData();
            	dialog.dismiss();
            }
        });
    }
    private void showRoomReserveDialog(){
    	List<String> list = new ArrayList<String>();
    	final List<RetentionTime> rtList = findRTimeByRoomHowmany(mRoomHowmany);
        DanbaoGuize dbgz = mRoomtypeDynamic.getDanbaoGuize();
        if(rtList.isEmpty()){
        	Toast.makeText(mContext, "该酒店不需要设置房间保留信息", Toast.LENGTH_LONG).show();
        	return;
        }
        for (int i = 0, size = rtList.size();i < size; i++ ){
        	list.add(rtList.get(i).getTime());
        }
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list);
        ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        //TODO: ActionLog
        final Dialog dialog = Utility.showNormalDialog(mSphinx, 
        		mSphinx.getString(R.string.choose_room_reserve), 
        		null,
        		listView,
        		null,
        		null,
        		null);
        listView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
            	mRTimeWhich = which;
            	refreshData();
                dialog.dismiss();
            }
        });        
    }

    public void submit(boolean HasCreditInfo) {
    	HotelOrderOperation dataOperation = new HotelOrderOperation(mSphinx);
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_CREATE);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_HOTEL_ID, mHotel.getUuid());
    	if(mHotel.getBrand() != null){
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_BRAND, mHotel.getBrand());
    	}
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_ROOMTYPE, mRoomType.getRoomId());
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_PKGID, mRoomType.getRateplanId());
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_CHECKIN_DATE, 
    			HotelHomeFragment.SIMPLE_DATE_FORMAT.format(mCheckIn.getTime()));
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_CHECKOUT_DATE, 
    			HotelHomeFragment.SIMPLE_DATE_FORMAT.format(mCheckOut.getTime()));
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_RESERVE_TIME, mRTimeDetail);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_NUMROOMS, mRoomHowmany + "");
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_TOTAL_PRICE, mTotalPrice);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_USERNAME, mUsername);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_MOBILE, mMobile);
    	if(HasCreditInfo){
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_CREDIT_CARD_NO, mCreditCardNo);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_VERIFY_CODE, mVerifyCode);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_VALID_YEAR, mValidYear);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_VALID_MONTH, mValidMonth);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_CARD_HOLDER_NAME, mCardHoldName);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_IDCARD_TYPE, mIdCardType);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_IDCARD_NO, mIdCardNo);
    	}
    	dataOperation.setup(criteria, mSphinx.getHotelHomeFragment().getCityInfo().getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
    	mSphinx.queryStart(dataOperation);
    }
    
	@Override
	public void onCancelled(TKAsyncTask tkAsyncTask) {
		super.onCancelled(tkAsyncTask);
	}

	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
		if (baseQuery.isStop()) {
            return;
        }

        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, HotelOrderWriteFragment.this, true)) {
            return;
        }
    	Response response = baseQuery.getResponse();
    	switch(response.getResponseCode()){
    	case Response.RESPONSE_CODE_OK:
    		Toast.makeText(mContext, mSphinx.getString(R.string.order_submit_success), Toast.LENGTH_LONG).show();
//    		if (response instanceof HotelOrderCreateResponse) {
			HotelOrderCreateResponse hotelOrderCreateResponse = (HotelOrderCreateResponse) response;
			Calendar cld = Calendar.getInstance();
			try {
				cld.setTime(SIMPLE_DATE_FORMAT.parse(mRTimeDetail));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mHotelOrder = new HotelOrder(
					hotelOrderCreateResponse.getOrderId(),
					Calendar.getInstance().getTimeInMillis(),
					1,
					mHotel.getUuid(),
					mPOI.getName(),
					mPOI.getAddress(),
					mPOI.getPosition(),
					mPOI.getTelephone(),
					mRoomType.getRoomType(),
					mRoomHowmany,
					Double.parseDouble(mTotalPrice),
					cld.getTimeInMillis(),
					mCheckIn.getTimeInMillis(),
					mCheckOut.getTimeInMillis(),
					-1,
					mUsername,
					mMobile
					);
			mSphinx.getHotelOrderDetailFragment().setData(mHotelOrder);
			mSphinx.showView(R.id.view_hotel_order_detail);
			dismiss();
			mSphinx.destroyHotelOrderWriteFragment();
			
    		break;
    	case Response.RESPONSE_CODE_HOTEL_ORDER_CREATE_FAILED:
    		Utility.showNormalDialog(mSphinx,mSphinx.getString(R.string.hotel_network_bad));
    		break;
    	case Response.RESPONSE_CODE_HOTEL_NEED_REGIST:
    		Toast.makeText(mContext, "需要注册酒店会员，目前系统暂不支持此功能", Toast.LENGTH_LONG).show();
    		break;
    	case Response.RESPONSE_CODE_HOTEL_NEED_CREDIT_ASSURE:
       		if(mTypeCreditAssure ==2){
       			mSphinx.getHotelOrderCreditFragment().setData(mTotalPrice, Calendar.getInstance(), response.getDescription());
       		}else {
        		mSphinx.getHotelOrderCreditFragment().setData(mRoomtypeDynamic.getPrice()+"", Calendar.getInstance(), response.getDescription());
       		}
       		mSphinx.showView(R.id.view_hotel_credit_assure);
    	}
	}
        
	public void setCredit(List<String> credit) {
		mCreditCardNo = credit.get(0);
		mVerifyCode = credit.get(2);
		mValidYear = credit.get(3);
		mValidMonth = credit.get(4);
		mCardHoldName = credit.get(1);
		mIdCardType = credit.get(5);
		mIdCardNo = credit.get(6);
		submit(true);
	}
	
	public List<RetentionTime> findRTimeByRoomHowmany(long roomhowmany){
		final List<RetentionTime> list;
		DanbaoGuize dbgz = mRoomtypeDynamic.getDanbaoGuize();
		if(roomhowmany > dbgz.getNum()){
			list = dbgz.getGreaterList();
		}else{
			list = dbgz.getLessList();
		}
		return list;
	}
}

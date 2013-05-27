/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.HotelOrderOperation;
import com.tigerknows.model.HotelOrderOperation.HotelOrderCreateResponse;
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
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ValidateUtil;
import com.tigerknows.widget.StringArrayAdapter;

public class HotelOrderWriteFragment extends BaseFragment implements View.OnClickListener{

    public HotelOrderWriteFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    static final String TAG = "HotelOrderWriteFragment";
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final int[] idArray = {R.id.room_person_edt, R.id.room_person_edt_2, R.id.room_person_edt_3, R.id.room_person_edt_4, R.id.room_person_edt_5};
    
    private ScrollView mHotelOrderWriteScv;
    private LinearLayout mPersonNameLly;
    private TextView mHotelNameTxv;
    private TextView mRoomtypeTxv;
    private TextView mRoomtypeDetailTxv;
    private TextView mRoomDateTxv;
    private TextView mRoomNightsTxv;
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
    
    // 酒店订单相关数据
    private int mRTimeWhich;
    private String mRTime;
    private String mRTimeDetail;
    private long mRoomHowmany;
    private double mOneNightPrice;
    private String mUsername;
    private String mMobile;
    private int mNights;
    private double mTotalPrice;
    
    // 7天酒店会员号
    private String mMemberNum = "";
    
    // 信用卡担保相关数据
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
    	mTitleBtn.setText(mSphinx.getString(R.string.hotel_room_title));
    	refreshData();

    }
    
    public void onPause(){
        super.onPause();
    }

    protected void findViews() {
        mHotelOrderWriteScv = (ScrollView) mRootView.findViewById(R.id.hotel_order_write_scv);
        mPersonNameLly = (LinearLayout) mRootView.findViewById(R.id.person_name_lly);
        mHotelNameTxv = (TextView) mRootView.findViewById(R.id.hotel_name_txv);
        mRoomtypeTxv = (TextView) mRootView.findViewById(R.id.roomtype_txv);
        mRoomtypeDetailTxv = (TextView) mRootView.findViewById(R.id.roomtype_detail_txv);
        mRoomDateTxv = (TextView) mRootView.findViewById(R.id.room_date_txv);
        mRoomNightsTxv = (TextView) mRootView.findViewById(R.id.room_nights_txv);
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
        mRoomNightsTxv.setOnClickListener(this);
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
        	String str = "";
        	for (int i = 1; i <= mRoomHowmany; i++){
        		if(i != 1) str += ";";
        		EditText thisPersonEdt = (EditText) mRootView.findViewById(idArray[i-1]);
        		String tempStr = thisPersonEdt.getText().toString();
        		if(TextUtils.isEmpty(tempStr)){
        			thisPersonEdt.requestFocus();
        			Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_room_person_empty_tip));
        			mSphinx.showSoftInput();
        			return;
        		}else if(!ValidateUtil.isValidElongName(tempStr)){
        			Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_person_name_format));
        			return;
        		}
        		str += tempStr;
        	}
        	mUsername = str;
        	str = mRoomMobileNumberEdt.getText().toString();
        	if(TextUtils.isEmpty(str)){
        		mRoomMobileNumberEdt.requestFocus();
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_room_mobile_empty_tip));
        		mSphinx.showSoftInput();
        		return;
        	}else if(!ValidateUtil.isValidPhone(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.phone_format_error_tip));
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
    	mOneNightPrice = mRoomtypeDynamic.getPrice() * mRoomHowmany;
    	mTotalPrice = mOneNightPrice * mNights;
    	mRoomHowmanyBtn.setText(mSphinx.getString(R.string.room_howmany_item, mRoomHowmany, Utility.doubleKeep(mTotalPrice, 2)+""));
    	RefreshPersonView();
    	final List<RetentionTime> rtList = findRTimeByRoomHowmany(mRoomHowmany);
    	if(rtList.isEmpty()){
    		mNeedCreditAssure = 0;
    		mTypeCreditAssure = 0;
    		mRTime = "23:59之前";
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
        	    checkIn.get(Calendar.MONTH)+1,
        		checkIn.get(Calendar.DATE),
        	    checkOut.get(Calendar.MONTH)+1,
        		checkOut.get(Calendar.DATE)
        		));
        mCheckIn = checkIn;
        mCheckOut = checkOut;
        mNights = CalendarUtil.dateInterval(mCheckIn, mCheckOut);
        mRoomNightsTxv.setText(mSphinx.getString(R.string.hotel_total_nights, mNights));
        mRoomHowmany = 1;
        mRTimeWhich = 0;
    }

	private void exit() {
		// TODO Auto-generated method stub
		
	}
	
	protected View createPerson(int id){
		LinearLayout person2 = new LinearLayout(mContext);
		person2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		person2.setOrientation(LinearLayout.HORIZONTAL);
		person2.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
		person2.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_header));
		
		TextView txv = new TextView(mSphinx);
		txv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		txv.setPadding(Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8));
		txv.setTextColor(getResources().getColor(R.color.black_dark));
		txv.setTextSize(16);
		txv.setText(mSphinx.getString(R.string.hotel_room_person_name));
		txv.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
		
		EditText edt = new EditText(mContext);
		edt.setId(id);
		edt.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		edt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
		edt.setTextSize(16);
		edt.setPadding(Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8));
		edt.setHint(mSphinx.getString(R.string.hotel_room_person_name_hint));
		edt.setHintTextColor(getResources().getColor(R.color.black_light));
		edt.setFocusable(true);
		edt.setFocusableInTouchMode(true);
		edt.setSingleLine(true);
		edt.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
		edt.setBackgroundColor(getResources().getColor(R.color.transparent));
		
		person2.addView(txv);
		person2.addView(edt);
		return person2;
	}
	
	private void RefreshPersonView(){
		// 这个函数用于在房间数变化时，入住人的编辑框数量相应变化
		int count = mPersonNameLly.getChildCount();
		if(count > mRoomHowmany){
			for (int i = count-1; i >= mRoomHowmany; i--){
				mPersonNameLly.removeViewAt(i);
			}
		}else if(count < mRoomHowmany){
			for (int i = count+1; i <= mRoomHowmany; i++){
				mPersonNameLly.addView(createPerson(idArray[i-1]));
			}
		}
	}
    private void showRoomHowmanyDialog(){
        final List<String> list = new ArrayList<String>();
        //TODO: mActionlog
        
        long listsize = (mRoomtypeDynamic.getNum() > MAX_ROOM_HOWMANY) ? MAX_ROOM_HOWMANY : mRoomtypeDynamic.getNum();
        String listitem;
        for(long i = 1; i <= listsize; i++){
            listitem = mSphinx.getString(R.string.room_howmany_item, i, Utility.doubleKeep(mRoomtypeDynamic.getPrice()*mNights*i, 2) + "");
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
        if(rtList.isEmpty()){
        	Toast.makeText(mContext, "该酒店不需要设置房间保留信息", Toast.LENGTH_LONG).show();
        	return;
        }
        for (int i = 0, size = rtList.size();i < size; i++ ){
        	String str = rtList.get(i).getTime();
        	if(rtList.get(i).getNeed() == 1){
        		str += " 担保";
        	}
        	list.add(str);
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
    	HotelOrderOperation hotelOrderOperation = new HotelOrderOperation(mSphinx);
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	criteria.put(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_CREATE);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_HOTEL_ID, mHotel.getUuid());
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_BRAND, String.valueOf(mHotel.getBrand()));
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_ROOMTYPE, mRoomType.getRoomId());
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_PKGID, mRoomType.getRateplanId());
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_CHECKIN_DATE, 
    			HotelHomeFragment.SIMPLE_DATE_FORMAT.format(mCheckIn.getTime()));
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_CHECKOUT_DATE, 
    			HotelHomeFragment.SIMPLE_DATE_FORMAT.format(mCheckOut.getTime()));
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_RESERVE_TIME, mRTimeDetail);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_NUMROOMS, mRoomHowmany + "");
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_TOTAL_PRICE, Utility.doubleKeep(mTotalPrice, 2)+ "");
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_USERNAME, mUsername);
    	criteria.put(HotelOrderOperation.SERVER_PARAMETER_MOBILE, mMobile);
    	if(!TextUtils.isEmpty(mMemberNum)){
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_MEMBERNUM, mMemberNum);
    	}
    	if(HasCreditInfo){
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_CREDIT_CARD_NO, mCreditCardNo);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_VERIFY_CODE, mVerifyCode);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_VALID_YEAR, mValidYear);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_VALID_MONTH, mValidMonth);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_CARD_HOLDER_NAME, mCardHoldName);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_IDCARD_TYPE, mIdCardType);
    		criteria.put(HotelOrderOperation.SERVER_PARAMETER_IDCARD_NO, mIdCardNo);
    	}
    	hotelOrderOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
    	mSphinx.queryStart(hotelOrderOperation);
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
        } else if (BaseActivity.checkResponseCode(baseQuery, mSphinx, new int[] {822, 825, 826}, BaseActivity.SHOW_ERROR_MSG_DIALOG, HotelOrderWriteFragment.this, false, true)) {
            return;
        }
    	Response response = baseQuery.getResponse();
    	HotelOrderCreateResponse hotelOrderCreateResponse = null;
    	if (response instanceof HotelOrderCreateResponse) {
    		hotelOrderCreateResponse = (HotelOrderCreateResponse) response;
    	}
    	switch(response.getResponseCode()){
    	case Response.RESPONSE_CODE_OK:
    		Toast.makeText(mContext, mSphinx.getString(R.string.order_submit_success), Toast.LENGTH_LONG).show();
			if (hotelOrderCreateResponse == null){
				return;
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
					Utility.doubleKeep(mTotalPrice, 2),
					CalendarUtil.strDateToLong(SIMPLE_TIME_FORMAT, mRTimeDetail),
					mCheckIn.getTimeInMillis(),
					mCheckOut.getTimeInMillis(),
					mNights,
					mUsername,
					mMobile
					);
			mSphinx.getHotelOrderDetailFragment().setData(mHotelOrder);
			mSphinx.getHotelOrderDetailFragment().setStageIndicatorVisible(true);
			mSphinx.showView(R.id.view_hotel_order_detail);
			destroyFragments(true, true);
			dismiss();
			mSphinx.uiStackRemove(R.id.view_hotel_order_write);
			mSphinx.destroyHotelOrderWriteFragment();
			
    		break;
    	case Response.RESPONSE_CODE_HOTEL_NEED_REGIST:
    		mSphinx.getHotelSeveninnRegistFragment().setData(mMobile);
    		mSphinx.showView(R.id.view_hotel_seveninn_regist);
    		break;
    	case Response.RESPONSE_CODE_HOTEL_NEED_CREDIT_ASSURE:
       		if(mTypeCreditAssure ==2){
       			mSphinx.getHotelOrderCreditFragment().setData(Utility.doubleKeep(mTotalPrice, 2)+"", response.getDescription());
       		}else {
        		mSphinx.getHotelOrderCreditFragment().setData(Utility.doubleKeep(mOneNightPrice, 2)+"", response.getDescription());
       		}
       		mSphinx.showView(R.id.view_hotel_credit_assure);
       		break;
    	case Response.RESPONSE_CODE_HOTEL_OTHER_ERROR:
    		Utility.showNormalDialog(mSphinx, response.getDescription());
    		break;
    	}
	}

	private void destroyFragments(boolean seven, boolean credit){
		
		if(seven == true && mSphinx.uiStackContains(R.id.view_hotel_seveninn_regist)){
			mSphinx.getHotelSeveninnRegistFragment().dismiss();
			mSphinx.uiStackRemove(R.id.view_hotel_seveninn_regist);
			mSphinx.destroyHotelSeveninnRegistFragment();
		}
		if(credit == true && mSphinx.uiStackContains(R.id.view_hotel_credit_assure)){
			mSphinx.getHotelOrderCreditFragment().dismiss();
			mSphinx.uiStackRemove(R.id.view_hotel_credit_assure);
			mSphinx.destroyHotelOrderCreditFragment();
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
	public void setMember(String memberNum){
		mMemberNum = memberNum;
		submit(false);
	}
		
	public List<RetentionTime> findRTimeByRoomHowmany(long roomhowmany){
		List<RetentionTime> list;
		DanbaoGuize dbgz = mRoomtypeDynamic.getDanbaoGuize();
		if(roomhowmany >= dbgz.getNum()){
			list = dbgz.getGreaterList();
			if(list.isEmpty()) list = dbgz.getLessList();
		}else{
			list = dbgz.getLessList();
			if(list.isEmpty()) list = dbgz.getGreaterList();
		}
		return list;
	}

}

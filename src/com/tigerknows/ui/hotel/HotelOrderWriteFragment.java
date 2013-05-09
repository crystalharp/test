/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import android.app.Dialog;
import android.os.Bundle;
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
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataOperation.HotelOrderCreateResponse;
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

    private static final long MAX_ROOM_HOWMANY = 5;
    
    private String mHotelID;
    private String mHotelBrand;
    private String mRoomTypeID;
    private String mRoomTypePkgId;
    private String mCheckInTime;
    private String mCheckOutTime;
    private long mRoomHowmany = 1;
    private String mTotalPrice;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //mActionTag = ActionLog.HotelOrderWrite;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_write, container, false);
        
        findViews();
        setListener();
        
        return mRootView;
    }
    
    public void onResume(){
        super.onResume();
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
            break;
        case R.id.submit_order_btn:
        	if (true) {
        		mSphinx.getHotelOrderCreditFragment().setData(0, Calendar.getInstance());
        		mSphinx.showView(R.id.view_hotel_credit_assure);
        	} else {
        		submit();
        	}
            break;
        default:
            break;
        }
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
    }

	private void exit() {
		// TODO Auto-generated method stub
		
	}
    private void showRoomHowmanyDialog(){
        List<String> list = new ArrayList<String>();
        //TODO: mActionlog
        
        long listsize = (mRoomtypeDynamic.getNum() > MAX_ROOM_HOWMANY) ? MAX_ROOM_HOWMANY : mRoomtypeDynamic.getNum();
        String listitem;
        for(long i = 1; i <= listsize; i++){
            listitem = mSphinx.getString(R.string.room_howmany_item, i + "", mRoomtypeDynamic.getPrice()*i + "");
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
            	dialog.dismiss();	
            }
        });
    }
    private void showRoomReserveDialog(){
    	List<String> list = new ArrayList<String>();
    	List<RetentionTime> rtList;
        DanbaoGuize dbgz = mRoomtypeDynamic.getDanbaoGuize();
        if(mRoomHowmany >= dbgz.getNum()){
        	rtList = dbgz.getGreaterList();
        }else{
        	rtList = dbgz.getLessList();
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
            	dialog.dismiss();	
            }
        });        
    }

    public void submit() {
    	DataOperation dataOperation = new DataOperation(mSphinx);
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	dataOperation.setup(criteria);
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
    	if (response instanceof HotelOrderCreateResponse) {
    		HotelOrderCreateResponse hotelOrderCreateResponse = (HotelOrderCreateResponse) response;
    		
    	}
	}
        
	public void setCredit(Object o) {
		//TODO
		submit();
	}
}

/*
 * Copyright (C) 2013 fengtianxiao@tigerknows.com
 */
package com.tigerknows.ui.hotel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.HotelOrderOperation;
import com.tigerknows.model.HotelOrderOperation.HotelOrderCreateResponse;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.HotelOrder;
import com.tigerknows.model.HotelVendor;
import com.tigerknows.model.POI;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic.DanbaoGuize;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic.DanbaoGuize.RetentionTime;
import com.tigerknows.model.Response;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.HotelOrderTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ValidateUtil;
import com.tigerknows.widget.SingleChoiceArrayAdapter;

public class HotelOrderWriteFragment extends BaseFragment implements View.OnClickListener{

    public HotelOrderWriteFragment(Sphinx sphinx) {
        super(sphinx);
        // Auto-generated constructor stub
    }
    
    static final String TAG = "HotelOrderWriteFragment";
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat SIMPLE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final int[] idArray = {R.id.room_person_edt, 
    	R.id.room_person_edt_2, 
    	R.id.room_person_edt_3, 
    	R.id.room_person_edt_4, 
    	R.id.room_person_edt_5, 
    	R.id.room_person_edt_6, 
    	R.id.room_person_edt_7, 
    	R.id.room_person_edt_8, 
    	R.id.room_person_edt_9, 
    	R.id.room_person_edt_A
    	};
    
    private ScrollView mHotelOrderWriteScv;
    private LinearLayout mHotelOrderWriteLly;
    private LinearLayout mPersonNameLly;
    private TextView mHotelNameTxv;
    private TextView mRoomtypeTxv;
    private TextView mRoomtypeSubtitleTxv;
    private TextView mRoomtypeDetailTxv;
    private TextView mRoomDateTxv;
    private TextView mRoomNightsTxv;
    private TextView mDanbaoHintTxv;
    private TextView mRoomTypeComeFromTxv;
    private Button mRoomHowmanyBtn;
    private Button mRoomReserveBtn;
    private EditText mRoomMobileNumberEdt;
    private Button mSubmitOrderBtn;
    private EditText mBookUsernameEdt;
    
    private POI mPOI;
    private Hotel mHotel;
    private HotelOrder mHotelOrder;
    private RoomType mRoomType;
    private RoomTypeDynamic mRoomtypeDynamic;
    private Calendar mCheckIn;
    private Calendar mCheckOut;

    private static final long MAX_ROOM_HOWMANY = 10;
    private static final long MAX_NAME_LENGTH = 50;
    
    // 酒店订单相关数据
    private int mRTimeWhich;
    private String mRTime;
    private String mRTimeDetail;
    private long mRoomHowmany;
    private double mOneNightPrice;
    private String mBookUsername;
    private String mUsername;
    private String mMobile;
    private int mNights;
    private double mTotalPrice;
    
    // 信用卡担保相关数据
    private long mNeedCreditAssure;
    private long mTypeCreditAssure;
    private String mBankName;
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
        mActionTag = ActionLog.HotelOrderWrite;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.hotel_order_write, container, false);
        
        findViews();
        setListener();
        return mRootView;
    }
    
    @Override
    public void onResume(){
        super.onResume();
        mTitleBtn.setText(getString(R.string.hotel_room_title));
        mLeftBtn.setOnClickListener(this);
        final HotelVendor hotelVendor = HotelVendor.getHotelVendorById(mRoomType.getVendorID(), mSphinx, null);
        if(hotelVendor != null){
        	LogWrapper.d("Trap", "~" + hotelVendor.getReserveTel());
        }
        if(hotelVendor == null || TextUtils.isEmpty(hotelVendor.getReserveTel())){
            mRightBtn.setVisibility(View.GONE);
        }else{
            mRightBtn.setText(getString(R.string.tel_reserve));
            mRightBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mActionLog.addAction(mActionTag + ActionLog.HotelOrderWriteReserveTel);
                    Utility.telephone(mSphinx, hotelVendor.getReserveTel());
				}
			});
            mRightBtn.setVisibility(View.VISIBLE);
        }
        refreshData();
    }
    
    @Override
    public void onPause(){
        super.onPause();
        TKConfig.setPref(mContext, TKConfig.PREFS_HOTEL_LAST_BOOKNAME, mBookUsernameEdt.getText().toString());
        TKConfig.setPref(mContext, TKConfig.PREFS_HOTEL_LAST_MOBILE, mRoomMobileNumberEdt.getText().toString());
    }

    @Override
    protected void findViews() {
        super.findViews();
        mHotelOrderWriteScv = (ScrollView) mRootView.findViewById(R.id.hotel_order_write_scv);
        mHotelOrderWriteLly = (LinearLayout)  mRootView.findViewById(R.id.hotel_order_write_lly);
        mPersonNameLly = (LinearLayout) mRootView.findViewById(R.id.person_name_lly);
        mHotelNameTxv = (TextView) mRootView.findViewById(R.id.hotel_name_txv);
        mRoomtypeTxv = (TextView) mRootView.findViewById(R.id.roomtype_txv);
        mRoomtypeSubtitleTxv = (TextView) mRootView.findViewById(R.id.roomtype_subtitle_txv);
        mRoomtypeDetailTxv = (TextView) mRootView.findViewById(R.id.roomtype_detail_txv);
        mRoomDateTxv = (TextView) mRootView.findViewById(R.id.room_date_txv);
        mRoomNightsTxv = (TextView) mRootView.findViewById(R.id.room_nights_txv);
        mDanbaoHintTxv = (TextView) mRootView.findViewById(R.id.danbao_hint_txv);
        mRoomTypeComeFromTxv = (TextView) mRootView.findViewById(R.id.roomtype_come_from_txv);
        mRoomHowmanyBtn = (Button) mRootView.findViewById(R.id.room_howmany_btn);
        mRoomReserveBtn = (Button) mRootView.findViewById(R.id.room_reserve_btn);
        mRoomMobileNumberEdt = (EditText) mRootView.findViewById(R.id.room_mobile_number_edt);
        mSubmitOrderBtn = (Button) mRootView.findViewById(R.id.submit_order_btn);
        mBookUsernameEdt = (EditText) mRootView.findViewById(idArray[0]);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mHotelOrderWriteLly.setOnTouchListener(new OnTouchListener(){
        
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSphinx.hideSoftInput();
                }
                return true;
            }
        });
        mHotelNameTxv.setOnClickListener(this);
        mRoomtypeTxv.setOnClickListener(this);
        mRoomtypeSubtitleTxv.setOnClickListener(this);
        mRoomtypeDetailTxv.setOnClickListener(this);
        mRoomDateTxv.setOnClickListener(this);
        mRoomNightsTxv.setOnClickListener(this);
        mRoomHowmanyBtn.setOnClickListener(this);
        mRoomReserveBtn.setOnClickListener(this);
        mSubmitOrderBtn.setOnClickListener(this);
        
        OnTouchListener edtTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    switch (v.getId()){
                    case R.id.room_person_edt:
                        mActionLog.addAction(mActionTag + ActionLog.HotelOrderWriteBookName);
                        break;
                    case R.id.room_mobile_number_edt:
                        mActionLog.addAction(mActionTag + ActionLog.HotelOrderWriteMobile);
                        break;
                    }
                }
                return false;
            }
        };
        mBookUsernameEdt.setOnTouchListener(edtTouchListener);
        mBookUsernameEdt.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
		        Editable editable = mBookUsernameEdt.getText();  
		        int len = ByteUtil.getCharArrayLength(editable.toString());
		        if(len > MAX_NAME_LENGTH){
		        	int selEndIndex = Selection.getSelectionEnd(editable); 
		        	String str = editable.toString();
		        	while(len > MAX_NAME_LENGTH){  
		        		//截取新字符串
		        		str = str.substring(0,selEndIndex-1) + str.substring(selEndIndex,str.length());
		        		//新字符串的长度
		        		len = ByteUtil.getCharArrayLength(str);
		        		selEndIndex--;
		        	}  
		        	mBookUsernameEdt.setText(str);  
		        	editable = mBookUsernameEdt.getText();  
		    		//设置新光标所在的位置
		        	Selection.setSelection(editable, selEndIndex);  
		        }				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
        mRoomMobileNumberEdt.setOnTouchListener(edtTouchListener);
    }

    @Override
    public void onClick(View view){
        int id = view.getId();
        switch (id) {
        case R.id.left_btn:
            showDiscardDialog(mActionTag + ActionLog.TitleLeftButton);
            break;
        case R.id.room_howmany_btn:
            mActionLog.addAction(mActionTag + ActionLog.HotelOrderWriteHowmany);
            showRoomHowmanyDialog();
            break;
        case R.id.room_reserve_btn:
            mActionLog.addAction(mActionTag + ActionLog.HotelOrderWriteReserve);
            showRoomReserveDialog();
            break;
        case R.id.submit_order_btn:
            TKConfig.setPref(mContext, TKConfig.PREFS_HOTEL_ORDER_COULD_ANOMALY_EXISTS, "yes");
            mActionLog.addAction(mActionTag + ActionLog.HOTELOrderWriteSubmit);
            String str = "";
            for (int i = 1; i <= mRoomHowmany; i++){
                final EditText thisPersonEdt = (EditText) mRootView.findViewById(idArray[i-1]);
                String tempStr = thisPersonEdt.getText().toString();
                if(i != 1){
                    str += ";";
                }
                else{
                    mBookUsername = tempStr;
                }
                if(TextUtils.isEmpty(tempStr)){
                    Utility.showEdittextErrorDialog(mSphinx, getString(R.string.hotel_room_person_empty_tip), thisPersonEdt);
                    return;
                }else if(!ValidateUtil.isValidElongName(tempStr)){
                    Utility.showEdittextErrorDialog(mSphinx, getString(R.string.hotel_person_name_format), thisPersonEdt);
                    return;
                }
                str += tempStr;
            }
            mUsername = str;
            str = mRoomMobileNumberEdt.getText().toString();
            if(TextUtils.isEmpty(str)){
                Utility.showEdittextErrorDialog(mSphinx, getString(R.string.hotel_room_mobile_empty_tip), mRoomMobileNumberEdt);
                 return;
            }else if(!ValidateUtil.isValidHotelMobile(str)){
                Utility.showEdittextErrorDialog(mSphinx, getString(R.string.hotel_mobile_format), mRoomMobileNumberEdt);
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
        mTotalPrice = mRoomtypeDynamic.getPrice() * mRoomHowmany;
        mOneNightPrice = mRoomtypeDynamic.getFirstNightPrice() * mRoomHowmany;
        mRoomHowmanyBtn.setText(getString(R.string.room_howmany_item, mRoomHowmany, Utility.formatHotelPrice(mTotalPrice)));
        RefreshPersonView();
        clearFocus();
        final List<RetentionTime> rtList = findRTimeByRoomHowmany(mRoomHowmany);
        mRTime = rtList.get(mRTimeWhich).getTime();
        mRTimeDetail = rtList.get(mRTimeWhich).getTimeDetail();
        rtList.get(mRTimeWhich).getNeed();
        mNeedCreditAssure = rtList.get(mRTimeWhich).getNeed();
        mTypeCreditAssure = rtList.get(mRTimeWhich).getType();
        mRoomReserveBtn.setText(mRTime + ((mNeedCreditAssure == 1) ? getString(R.string.hotel_room_need_credit_assure) : "") );
        mSubmitOrderBtn.setText((mNeedCreditAssure == 1) ? getString(R.string.go_credit_assure) : getString(R.string.submit_order));
        mDanbaoHintTxv.setVisibility((mNeedCreditAssure == 1) ? View.VISIBLE : View.GONE);
    }
    
    public void setData(POI poi, RoomType roomtype, RoomTypeDynamic roomTypeDynamic, Calendar checkIn, Calendar checkOut ) {

        mBookUsernameEdt.setText(TKConfig.getPref(mContext, TKConfig.PREFS_HOTEL_LAST_BOOKNAME, ""));
        mBookUsernameEdt.requestFocus();
        Selection.setSelection(mBookUsernameEdt.getText(), mBookUsernameEdt.length());
        String lastMobile = TKConfig.getPref(mContext, TKConfig.PREFS_HOTEL_LAST_MOBILE, "FirstFirstFirst");
        if(TextUtils.equals(lastMobile, "FirstFirstFirst")){
        	mRoomMobileNumberEdt.setText(Globals.g_User != null ? TKConfig.getPref(mContext, TKConfig.PREFS_PHONENUM, "") : "");
        }else{
        	mRoomMobileNumberEdt.setText(lastMobile);
        }
        mRoomMobileNumberEdt.requestFocus();
        Selection.setSelection(mRoomMobileNumberEdt.getText(), mRoomMobileNumberEdt.length());
        mPOI = poi;
        mHotel = poi.getHotel();
        mRoomType = roomtype;
        mRoomtypeDynamic = roomTypeDynamic;
        mHotelNameTxv.setText(mPOI.getName());
        mRoomtypeTxv.setText(mRoomType.getRoomType());
        String appendContent;
        appendContent = mRoomType.getSubtitle();
        if(TextUtils.isEmpty(appendContent)){
        	mRoomtypeSubtitleTxv.setVisibility(View.GONE);
        }else{
        	mRoomtypeSubtitleTxv.setVisibility(View.VISIBLE);
        	mRoomtypeSubtitleTxv.setText(appendContent);
        }
        mRoomtypeDetailTxv.setText(mRoomType.generateDescription());
        mRoomDateTxv.setText(getString(R.string.hotel_room_date,
                checkIn.get(Calendar.MONTH)+1,
                checkIn.get(Calendar.DATE),
                checkOut.get(Calendar.MONTH)+1,
                checkOut.get(Calendar.DATE)
                ));
        mCheckIn = checkIn;
        mCheckOut = checkOut;
        mNights = CalendarUtil.dateInterval(mCheckIn, mCheckOut);
        mRoomNightsTxv.setText(getString(R.string.hotel_total_nights, mNights));
        mRoomTypeComeFromTxv.setText(getString(R.string.this_come_from_colon, mRoomType.getVendorName()));
        mRoomHowmany = mRoomtypeDynamic.getMinimum();
        mRTimeWhich = 0;
        mHotelOrderWriteScv.smoothScrollTo(0, 0);
    }

    private void exit(String actionLogInfo) {
        synchronized (mSphinx.mUILock) {
    	    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
        	dismiss();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	showDiscardDialog(ActionLog.KeyCodeBack);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void showDiscardDialog(final String actionLogInfo) {
    	Utility.showNormalDialog(mSphinx,
    			mSphinx.getString(R.string.prompt),
    			"您的订单尚未填写完成",
    			"继续预订",
    			"稍后再订",
    			new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(which == DialogInterface.BUTTON_NEGATIVE){
							exit(actionLogInfo);
						}
					}
				});
	}

	protected View createPerson(int id){
        LinearLayout person2 = new LinearLayout(mContext);
        person2.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        person2.setOrientation(LinearLayout.HORIZONTAL);
        person2.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        person2.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_middle));
        
        TextView txv = new TextView(mSphinx);
        txv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        txv.setPadding(Utility.dip2px(mContext, 8), 0, 0, 0);
        txv.setTextColor(getResources().getColor(R.color.black_dark));
        txv.setTextSize(16);
        txv.setText(getString(R.string.hotel_room_person_name));
        txv.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        
        final EditText edt = new EditText(mContext);
        edt.setId(id);
        edt.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        edt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        edt.setTextSize(16);
        edt.setPadding(Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8), Utility.dip2px(mContext, 8));
        edt.setHint(getString(R.string.hotel_room_person_name_hint));
        edt.setHintTextColor(getResources().getColor(R.color.black_light));
        edt.setFocusable(true);
        edt.setFocusableInTouchMode(true);
        edt.setSingleLine(true);
        edt.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
        edt.setBackgroundColor(getResources().getColor(R.color.transparent));
        edt.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
		        Editable editable = edt.getText();  
		        int len = ByteUtil.getCharArrayLength(editable.toString());
		        if(len > MAX_NAME_LENGTH){
		        	int selEndIndex = Selection.getSelectionEnd(editable); 
		        	String str = editable.toString();
		        	while(len > MAX_NAME_LENGTH){  
		        		//截取新字符串
		        		str = str.substring(0,selEndIndex-1) + str.substring(selEndIndex,str.length());
		        		//新字符串的长度
		        		len = ByteUtil.getCharArrayLength(str);
		        		selEndIndex--;
		        	}  
		        	edt.setText(str);  
		        	editable = edt.getText();  
		    		//设置新光标所在的位置
		        	Selection.setSelection(editable, selEndIndex);  
		        }				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
        
        
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
            for (int i = count; i < mRoomHowmany; i++){
                mPersonNameLly.addView(createPerson(idArray[i]));
            }
        }
    }
    public class HotelRoomHowmanyAdapter extends SingleChoiceArrayAdapter{

        public HotelRoomHowmanyAdapter(Context context, List<String> list) {
            super(context, list);
            // Auto-generated constructor stub
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent){
            View view = super.getView(position, convertView, parent);
            ImageView singleIconImv = (ImageView)view.findViewById(R.id.single_icon_imv);
            if(position == (int)mRoomHowmany - mRoomtypeDynamic.getMinimum()){
                singleIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_checked));
            }else{
                singleIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_default));
            }

            return view;
        }
        
    }
    public class HotelReserveListAdapter extends ArrayAdapter<RetentionTime>{
        private static final int RESOURCE_ID = R.layout.hotel_room_reserve_list_item;
        
        public HotelReserveListAdapter(Context context, List<RetentionTime> list) {
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

            ImageView roomHowmanyIconImv = (ImageView) view.findViewById(R.id.room_reserve_icon_imv);
            TextView roomReserveTxv = (TextView) view.findViewById(R.id.room_reserve_txv);
            RetentionTime rt = getItem(position);
            if(rt.getNeed() == 1){
                roomReserveTxv.setText(Utility.renderColorToPartOfString(mContext,
                        R.color.orange,
                        rt.getTime() + getString(R.string.hotel_room_credit_note),
                        getString(R.string.hotel_room_credit_note)));
            }else{
                roomReserveTxv.setText(rt.getTime());
            }
            if(position == mRTimeWhich){
                roomHowmanyIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_checked));
            }else{
                roomHowmanyIconImv.setImageDrawable(getResources().getDrawable(R.drawable.rdb_recovery_default));
            }

            int count = getCount();
            if (count == 1) {
                view.setBackgroundResource(R.drawable.list_single);
            } else if (position == 0) {
                view.setBackgroundResource(R.drawable.list_header);
            } else if (position == count-1) {
                view.setBackgroundResource(R.drawable.list_footer);
            } else {
                view.setBackgroundResource(R.drawable.list_middle);
            }
            return view;
        }
        
    }
    private void showRoomHowmanyDialog(){
        final List<String> list = new ArrayList<String>();
        
        long listsize = (mRoomtypeDynamic.getNum() > MAX_ROOM_HOWMANY) ? MAX_ROOM_HOWMANY : mRoomtypeDynamic.getNum();
        String listitem;
        for(long i = mRoomtypeDynamic.getMinimum(); i <= listsize; i++){
            listitem = getString(R.string.room_howmany_item, i, Utility.formatHotelPrice(mRoomtypeDynamic.getPrice()*i));
            list.add(listitem);
        }
        final ArrayAdapter<String> adapter = new HotelRoomHowmanyAdapter(mSphinx, list);
        View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
        
        final ListView listView = (ListView) alterListView.findViewById(R.id.listview);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
        
        TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
        titleTxv.setText(R.string.choose_room_howmany);
        
        Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
        button.setVisibility(View.GONE);
        
        dialog.show();
        
        listView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3){
                listView.setAdapter(adapter);
                mRoomHowmany = which + mRoomtypeDynamic.getMinimum();
                mActionLog.addAction(mActionTag + ActionLog.HotelOrderWriteHowmanyChoose, mRoomHowmany);
                mRTimeWhich = 0;
                refreshData();
                dialog.dismiss();
            }
        });

    }
    private void showRoomReserveDialog(){
        final List<RetentionTime> rtList = findRTimeByRoomHowmany(mRoomHowmany);
        HotelReserveListAdapter hotelReserveListAdapter = new HotelReserveListAdapter(mContext, rtList);

        View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
        
        final ListView listView = (ListView) alterListView.findViewById(R.id.listview);
        listView.setAdapter(hotelReserveListAdapter);
        
        final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
        
        TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
        titleTxv.setText(R.string.choose_room_reserve);
        
        Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
        button.setVisibility(View.GONE);
        
        dialog.show();

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
        hotelOrderOperation.addParameter(BaseQuery.SERVER_PARAMETER_OPERATION_CODE, HotelOrderOperation.OPERATION_CODE_CREATE);
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_HOTEL_ID, mRoomType.getHotelID());
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_BRAND, String.valueOf(mHotel.getBrand()));
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_ROOMTYPE, mRoomType.getRoomId());
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_PKGID, mRoomType.getRateplanId());
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_CHECKIN_DATE, 
                HotelHomeFragment.SIMPLE_DATE_FORMAT.format(mCheckIn.getTime()));
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_CHECKOUT_DATE, 
                HotelHomeFragment.SIMPLE_DATE_FORMAT.format(mCheckOut.getTime()));
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_RESERVE_TIME, mRTimeDetail);
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_NUMROOMS, mRoomHowmany + "");
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_TOTAL_PRICE, Utility.formatHotelPrice(mTotalPrice));
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_USERNAME, mBookUsername);
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_MOBILE, mMobile);
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_GUESTS, mUsername);
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_GUESTTYPE, mRoomtypeDynamic.getGuesttype());
        hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_VENDORID, String.valueOf(mRoomType.getVendorID()));
        if(HasCreditInfo){
        	hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_BANK_NAME, mBankName);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_CREDIT_CARD_NO, mCreditCardNo);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_VERIFY_CODE, mVerifyCode);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_VALID_YEAR, mValidYear);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_VALID_MONTH, mValidMonth);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_CARD_HOLDER_NAME, mCardHoldName);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_IDCARD_TYPE, mIdCardType);
            hotelOrderOperation.addParameter(HotelOrderOperation.SERVER_PARAMETER_IDCARD_NO, mIdCardNo);
        }
        hotelOrderOperation.setup(getId(), getId(), getString(R.string.doing_and_wait));
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
        }else if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_DIALOG, HotelOrderWriteFragment.this, false, new int[] {821, 825, 826})) {
            return;
        }
        Response response = baseQuery.getResponse();
        HotelOrderCreateResponse hotelOrderCreateResponse = null;
        if (response instanceof HotelOrderCreateResponse) {
            hotelOrderCreateResponse = (HotelOrderCreateResponse) response;
        }
        switch(response.getResponseCode()){
        case Response.RESPONSE_CODE_OK:
            Toast.makeText(mSphinx, getString(R.string.order_submit_success), Toast.LENGTH_LONG).show();
            if (hotelOrderCreateResponse == null){
                return;
            }

            mHotelOrder = new HotelOrder(
                    hotelOrderCreateResponse.getOrderId(),
                    CalendarUtil.getExactTime(mContext),
                    1,
                    mPOI.getUUID(),
                    mPOI.getName(),
                    mPOI.getAddress(),
                    mPOI.getPosition(),
                    mPOI.getTelephone(),
                    mRoomType.getRoomType(),
                    mRoomHowmany,
                    Utility.doubleKeep(mTotalPrice, 2),
                    mRoomType.getVendorID(),
                    CalendarUtil.strDateToLong(SIMPLE_TIME_FORMAT, mRTimeDetail),
                    mCheckIn.getTimeInMillis(),
                    mCheckOut.getTimeInMillis(),
                    mNights,
                    mUsername,
                    mMobile,
                    hotelOrderCreateResponse.getCancelDeadline()
                    );
            TKConfig.setPref(mContext, TKConfig.PREFS_HOTEL_LAST_BOOKNAME, mBookUsername);
            TKConfig.setPref(mContext, TKConfig.PREFS_HOTEL_LAST_MOBILE, mMobile);
            if(mSphinx.uiStackContains(R.id.view_hotel_credit_assure)){
                if(mSphinx.getHotelOrderCreditFragment().isShowing()){
                	mSphinx.getHotelOrderCreditFragment().dismiss();
                }
                mSphinx.uiStackRemove(R.id.view_hotel_credit_assure);
            }
            mSphinx.getHotelOrderSuccessFragment().setData(mHotelOrder);
            HotelOrderTable hotelOrderTable = new HotelOrderTable(mSphinx);
            try {
                hotelOrderTable.write(mHotelOrder);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (APIException e) {
                e.printStackTrace();
            } finally {
                hotelOrderTable.close();
            }
            //订单列表已经发生了变化，在此处清除列表界面的内容，列表页onResume时会重新加载订单。
            mSphinx.getHotelOrderListFragment().clearOrders();
            mSphinx.getHotelOrderListFragment().syncOrder();
            dismiss();
            mSphinx.uiStackRemove(R.id.view_hotel_order_write);
            mSphinx.showView(R.id.view_hotel_order_success);
            break;
        case Response.RESPONSE_CODE_HOTEL_NEED_CREDIT_ASSURE:
            mSphinx.getHotelOrderCreditFragment().setData(response.getDescription(),
                    Utility.formatHotelPrice(mOneNightPrice),
                    Utility.formatHotelPrice(mTotalPrice),
                    (int)mTypeCreditAssure,
                    mBookUsername);
            mSphinx.showView(R.id.view_hotel_credit_assure);
            break;
        case Response.RESPONSE_CODE_HOTEL_OTHER_ERROR:
        	String description = response.getDescription().split("!")[0].split("！")[0];
        	switch(analysisDescription(description)){
        	case 1:
        		Utility.showNormalDialog(mSphinx, 
        				getString(R.string.prompt), 
        				description, 
        				getString(R.string.view_order), 
        				getString(R.string.know_la), 
        				new DialogInterface.OnClickListener() {

        			        @Override
							public void onClick(DialogInterface dialog,
									int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                	mSphinx.getHotelOrderListFragment().clearOrders();
                                	mSphinx.getHotelOrderListFragment().syncOrder();
                                	if(mSphinx.uiStackContains(R.id.view_hotel_credit_assure)){
                                		mSphinx.getHotelOrderCreditFragment().clearVerifyEdt();
                                	}
                                	mSphinx.showView(R.id.view_hotel_order_list);
                                }
                            }								
						});
        		break;
        	default:
        	    Utility.showNormalDialog(mSphinx, description);
        	}
        	break;
        case Response.RESPONSE_CODE_HOTEL_ORDER_CREATE_FAILED:
        	Utility.showNormalDialog(mSphinx, getString(R.string.response_code_821));
            break;
        }
    }
    private int analysisDescription(String description){
    	if(description.contains(getString(R.string.hotel_duplicate_order))){
    		return 1;
    	}else {
    		return 0;
    	}
    }

    public void setCredit(List<String> credit) {
    	mBankName = credit.get(0);
        mCreditCardNo = credit.get(1);
        mVerifyCode = credit.get(3);
        mValidYear = credit.get(4);
        mValidMonth = credit.get(5);
        mCardHoldName = credit.get(2);
        mIdCardType = credit.get(6);
        mIdCardNo = credit.get(7);
        submit(true);
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
        if(list.isEmpty()){
            XMap data = new XMap();
            data.put(RetentionTime.FIELD_TIME, getString(R.string.before_23_59));
            data.put(RetentionTime.FIELD_NEED, 0);
            data.put(RetentionTime.FIELD_TYPE, 0);
            data.put(RetentionTime.FIELD_TIME_DETAIL, SIMPLE_DATE_FORMAT.format(mCheckIn.getTime()) + " 23:59:00");
            try {
                RetentionTime rt = new RetentionTime(data);
                list.add(rt);
            } catch (APIException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}

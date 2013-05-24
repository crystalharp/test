package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.Description;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.ProxyQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.ui.hotel.DateListView;
import com.tigerknows.ui.hotel.DateWidget;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.hotel.HotelImageGridFragment;
import com.tigerknows.ui.hotel.HotelIntroFragment;
import com.tigerknows.ui.poi.DynamicHotelPOI.MoreRoomTypeClickListener;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

public class DynamicHotelPOI extends DynamicPOIView {

    static DynamicHotelPOI instance = null;
    DynamicPOIViewBlock mUpperBlock;
    DynamicPOIViewBlock mLowerBlock;
    List<DynamicPOIViewBlock> blockList = new LinkedList<DynamicPOIViewBlock>();
    
    Hotel mHotel;
    Calendar checkin;
    Calendar checkout;
    Button mClickedBookBtn;
    RoomType mClickedRoomType;
    POI mPOI;
    List<RoomType> mShowingRoomList = new ArrayList<RoomType>();
    List<RoomType> mAllRoomList = new ArrayList<RoomType>();
    final int SHOW_DYNAMIC_HOTEL_MAX = 3;
    MoreRoomTypeClickListener moreRoomTypeClickListener;
    
    private LinearLayout mDynamicRoomTypeListView;
    
    private LinearLayout mDynamicRoomTypeMoreView;
    
    LinearListView roomTypeList;
    ImageView hotelImage;
    TextView hotelSummary;
    private DateWidget mCheckInDat;
    private DateWidget mCheckOutDat;
    TextView moreTxv;
    TextView imageNumTxv;
    
    final static String TAG = "DynamicHotelPOI";
    
    ItemInitializer i = new ItemInitializer(){

        @Override
        public void initItem(Object data, View v) {
            RoomType roomType = (RoomType)data;
            TextView priceTxv = (TextView) v.findViewById(R.id.price_txv);
            TextView roomTypeTxv = (TextView) v.findViewById(R.id.room_type_txv);
            TextView roomDetailTxv = (TextView) v.findViewById(R.id.room_detail_txv);
            Button bookBtn = (Button) v.findViewById(R.id.book_btn);
            priceTxv.setText(roomType.getPrice());
            roomTypeTxv.setText(roomType.getRoomType());
            roomDetailTxv.setText(roomType.getBedType() + " " + roomType.getBreakfast() + " " + roomType.getNetService()
                    + " " + roomType.getFloor() + " " + roomType.getArea());
            if (roomType.getCanReserve() == 0) {
                bookBtn.setEnabled(false);
            } else {
                bookBtn.setEnabled(true);
                bookBtn.setTag(roomType);
                bookBtn.setOnClickListener(new roomTypeClickListener());
            }
        }
            
    };
    
    
    public static DynamicHotelPOI getInstance(POIDetailFragment poiFragment, LayoutInflater inflater){
        if (instance == null) {
            instance = new DynamicHotelPOI(poiFragment, inflater);
        }
        return instance;
    }
    
    private DynamicHotelPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        mUpperBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, POIDetailFragment.DPOIType.HOTEL);
        mLowerBlock = new DynamicPOIViewBlock(poiFragment.mBelowCommentLayout, POIDetailFragment.DPOIType.HOTEL);
        mUpperBlock.mOwnLayout = mInflater.inflate(R.layout.poi_dynamic_hotel_upper, null);
        mLowerBlock.mOwnLayout = mInflater.inflate(R.layout.poi_dynamic_hotel_below_feature, null);
        findViews();
        roomTypeList = new LinearListView(mSphinx, mDynamicRoomTypeListView, i, R.layout.poi_dynamic_hotel_room_item);
        
        moreRoomTypeClickListener = new MoreRoomTypeClickListener();
        mDynamicRoomTypeMoreView.setOnClickListener(moreRoomTypeClickListener);
    }
    
    void findViews(){
        mDynamicRoomTypeListView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_list_view);
        mDynamicRoomTypeMoreView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_more_view);
        hotelImage = (ImageView)mLowerBlock.mOwnLayout.findViewById(R.id.icon_imv);
        hotelSummary = (TextView)mLowerBlock.mOwnLayout.findViewById(R.id.short_summary);
        mDynamicRoomTypeMoreView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_more_view);
//        mDynamicHotelChooseTimeView = (LinearLayout) mRootView.findViewById(R.id.dynamic_hotel_choosetime_view);
        mCheckInDat = (DateWidget) mUpperBlock.mOwnLayout.findViewById(R.id.checkin_dat);
        mCheckOutDat = (DateWidget) mUpperBlock.mOwnLayout.findViewById(R.id.checkout_dat);
        imageNumTxv = (TextView) mLowerBlock.mOwnLayout.findViewById(R.id.image_num_txv);
        moreTxv = (TextView) mDynamicRoomTypeMoreView.findViewById(R.id.more_txv);
    }
    
    class MoreRoomTypeClickListener implements View.OnClickListener{
        boolean mShowingAll = false;
        @Override
        public void onClick(View v) {
            if (mShowingAll) {
                roomTypeList.refreshList(mShowingRoomList);
                refreshBackground(roomTypeList, mShowingRoomList);
                moreTxv.setText(mSphinx.getString(R.string.hotel_expand_roomtype));
                mShowingAll = false;
            } else {
                roomTypeList.refreshList(mAllRoomList);
                refreshBackground(roomTypeList, mAllRoomList);
                moreTxv.setText(mSphinx.getString(R.string.fold));
                mShowingAll = true;
            }
        }
        
    }
    
    final public void setDate() {
        if (mSphinx.uiStackContains(R.id.view_hotel_home)) {
            checkin = mSphinx.getHotelHomeFragment().getCheckin();
            checkout = mSphinx.getHotelHomeFragment().getCheckout();
        } else {
            checkin = Calendar.getInstance();
            checkin.setTimeInMillis(System.currentTimeMillis());
            checkout = (Calendar) checkin.clone();
            checkout.add(Calendar.DAY_OF_YEAR, 1);
        }
    }
    
    final public void refreshDate() {
//        DateListView dateListView = getDateListView();
        mCheckInDat.setCalendar(checkin);
        mCheckOutDat.setCalendar(checkout);
    }

    @Override
    public List<DynamicPOIViewBlock> getViewList(POI poi) {
        blockList.clear();
        if (poi == null || poi.getHotel() == null || poi.getHotel().getRoomTypeList() == null) {
            LogWrapper.i(TAG, "poi or hotel or roomTypeList is null, nothing to show for DynamicHotel");
            return blockList;
        }
        mPOI = poi;
        mHotel = poi.getHotel();
        refreshDate();
        
        moreTxv.setText(mSphinx.getString(R.string.hotel_expand_roomtype));
        moreRoomTypeClickListener.mShowingAll = false;

        mAllRoomList.clear();
        mShowingRoomList.clear();
        mAllRoomList.addAll(mHotel.getRoomTypeList());
        int size = (mAllRoomList != null? mAllRoomList.size() : 0);
        if (size == 0) {
            LogWrapper.i(TAG, "size of roomTypeList is 0.");
            moreTxv.setText(mSphinx.getString(R.string.hotel_no_roomtype));
            mDynamicRoomTypeMoreView.setOnClickListener(null);
        } else if (size > SHOW_DYNAMIC_HOTEL_MAX) {
            for(int i = 0; i < SHOW_DYNAMIC_HOTEL_MAX; i++) {
                mShowingRoomList.add(mAllRoomList.get(i));
            }
            mDynamicRoomTypeMoreView.setVisibility(View.VISIBLE);
        } else {
            mShowingRoomList.addAll(mAllRoomList);
            mDynamicRoomTypeMoreView.setVisibility(View.GONE);
        }
        roomTypeList.refreshList(mShowingRoomList);
        refreshBackground(roomTypeList, mShowingRoomList);
        
        List<HotelTKDrawable> b = mHotel.getHotelTKDrawableList();
        
        String value = mPOI.getDescriptionValue(Description.FIELD_SYNOPSIS);
        hotelSummary.setText(value);
        LinearLayout hotelSummaryBlock = (LinearLayout) mLowerBlock.mOwnLayout.findViewById(R.id.hotel_summary);
        hotelSummaryBlock.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				HotelIntroFragment hotelIntro = mSphinx.getHotelIntroFragment();
				hotelIntro.setData(mPOI);
				mSphinx.showView(R.id.view_hotel_intro);
			}
		});
        int picNum = (b == null ? 0 : b.size());
        imageNumTxv.setText(mSphinx.getString(R.string.pictures, picNum));
        //FIXME:如何获取这个Image?
//        final TKDrawable tkDrawable = mHotel.getImageThumb();
        if (b != null) {
            final TKDrawable tkDrawable = b.get(0).getTKDrawable();
            if (tkDrawable != null) {
                Drawable hotelImageDraw = tkDrawable.loadDrawable(mSphinx, new Runnable() {
    
                    @Override
                    public void run() {
    //                    hotelImage.setMaxHeight(Util.dip2px(, dpValue))
                        hotelImage.setBackgroundDrawable(tkDrawable.loadDrawable(null, null, null));
                    }
                    
                }, mPOIDetailFragment.toString());
                if (hotelImageDraw != null) {
                    hotelImage.setBackgroundDrawable(hotelImageDraw);
                } else {
                    hotelImage.setImageResource(R.drawable.icon);
                }
            }
        }
        hotelImage.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Hotel hotel = mPOI.getHotel();
				if (hotel != null) {
				    mSphinx.getHotelImageGridFragment().setData(hotel);
				    mSphinx.showView(R.id.view_hotel_image_grid);
				}
			}
		});
        
        blockList.add(mUpperBlock);
        blockList.add(mLowerBlock);
        LogWrapper.i(TAG, "Hotel viewBlock is:" + blockList);
        return blockList;
    }

    class roomTypeClickListener implements View.OnClickListener{
        
        @Override
        public void onClick(View v) {
            RoomType mData;
            mData = (RoomType) v.getTag();
            mClickedBookBtn = (Button)v;
            mClickedRoomType = mData;
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            baseQueryList.add(buildRoomTypeDynamicQuery(mHotel.getUuid(), mData.getRoomId(), mData.getRateplanId(), checkin, checkout));
            query(mPOIDetailFragment, baseQueryList);
            
        }
        
    }

    BaseQuery buildHotelQuery(Calendar checkin, Calendar checkout, POI poi, String needFiled){
        String checkinTime = HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkin.getTime());
        String checkoutTime = HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkout.getTime());
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
        criteria.put(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_HOTEL);
        criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
        criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, needFiled+"01");   // 01表示poi的uuid
        criteria.put(DataOperation.SERVER_PARAMETER_CHECKIN, checkinTime);
        criteria.put(DataOperation.SERVER_PARAMETER_CHECKOUT, checkoutTime);
        DataOperation dataOpration = new DataOperation(mSphinx);
        dataOpration.setup(criteria, Globals.g_Current_City_Info.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId());
        return dataOpration;
    }
    
    BaseQuery buildRoomTypeDynamicQuery(String hotelId, String roomId, String pkgId, Calendar checkin, Calendar checkout){
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(ProxyQuery.SERVER_PARAMETER_CHECKIN_DATE, HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkin.getTime()));
        criteria.put(ProxyQuery.SERVER_PARAMETER_CHECKOUT_DATE, HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkout.getTime()));
        criteria.put(ProxyQuery.SERVER_PARAMETER_HOTELID, hotelId);
        criteria.put(ProxyQuery.SERVER_PARAMETER_ROOMID, roomId);
        criteria.put(ProxyQuery.SERVER_PARAMETER_ROOM_TYPE_TAOCANID, pkgId);
        criteria.put(ProxyQuery.SERVER_PARAMETER_TASK, "1");
        ProxyQuery query = new ProxyQuery(mSphinx);
        query.setup(criteria, Globals.getCurrentCityId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
        return query;
    }
    @Override
    public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response) {
        if (response instanceof RoomTypeDynamic) {
            RoomTypeDynamic roomInfo = ((RoomTypeDynamic)response);
            if (roomInfo.getNum() > 0){
                //如果有房，跳转
                //TODO:使用实际参数
                mSphinx.getHotelOrderWriteFragment().setData(mPOI, mClickedRoomType, roomInfo, checkin, checkout);
                mSphinx.showView(R.id.view_hotel_order_write);
            } else {
                //更新按钮状态
                mClickedRoomType.setCanReserve(0);
//                mClickedBookBtn.setText(mSphinx.getString(R.string.hotel_btn_sold_out));
                mClickedBookBtn.setClickable(false);
            }
            
        }
    }
    @Override
    public boolean checkExistence(POI poi) {
        List<DynamicPOI> list = poi.getDynamicPOIList();
        mHotel = poi.getHotel();
        int size = (list == null ? 0 : list.size()); 
        for (int i = 0; i < size; i++) {
            DynamicPOI iter = list.get(i);
            if (iter.getType().equals(DynamicPOI.TYPE_HOTEL)) {
                LogWrapper.i(TAG, "Dynamic Hotel info exists.");
                return true;
            }
        }
        return false;
    }
        
    public List<BaseQuery> generateQuery(POI poi) {
        mPOI = poi;
        List<BaseQuery> baseQueryList = new LinkedList<BaseQuery>();
        setDate();
        if (mHotel == null) {
            LogWrapper.i(TAG, "hotel is null, generate Query.");
            BaseQuery baseQuery = buildHotelQuery(checkin, checkout, poi, Hotel.NEED_FILED_DETAIL+Hotel.NEED_FILED_LIST);
            baseQueryList.add(baseQuery);
        } else if (mHotel != null && mHotel.getRoomTypeList() == null) {
            LogWrapper.i(TAG, "hotel.roomtype is null, generate Query.");
            BaseQuery baseQuery = buildHotelQuery(checkin, checkout, poi, Hotel.NEED_FILED_DETAIL);
            baseQueryList.add(baseQuery);
        }
        LogWrapper.i(TAG, "Generated query in hotel:" + baseQueryList);
        return baseQueryList;
    }
    
    private void refreshBackground(LinearListView lsv, List<RoomType> list) {
        int size = list.size();
        for(int i = 0; i < size; i++) {
            View child = lsv.getChildView(i);
            if (i == (size-1) && size <= SHOW_DYNAMIC_HOTEL_MAX) {
                child.setBackgroundResource(R.drawable.list_footer);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
            }
            child.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
        }
 
    }
}

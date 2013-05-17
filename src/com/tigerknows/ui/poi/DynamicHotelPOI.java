package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.ProxyQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

public class DynamicHotelPOI extends DynamicPOIView<Hotel> {

    static DynamicHotelPOI instance = null;
    DynamicPOIViewBlock mUpperBlock;
    DynamicPOIViewBlock mLowerBlock;
    
    Hotel mHotel;
    Calendar checkin;
    Calendar checkout;
    Button mClickedBookBtn;
    RoomType mClickedRoomType;
    POI mPOI;
    List<RoomType> mShowingRoomList = new ArrayList<RoomType>();
    List<RoomType> mAllRoomList = new ArrayList<RoomType>();
    final int SHOW_DYNAMIC_HOTEL_MAX = 3;
    
    private LinearLayout mDynamicRoomTypeListView;
    
    private LinearLayout mDynamicRoomTypeMoreView;
    
    LinearListView roomTypeList;
    
    ItemInitializer i = new ItemInitializer(){

        @Override
        public void initItem(Object data, View v) {
            // TODO Auto-generated method stub
            RoomType roomType = (RoomType)data;
            TextView priceTxv = (TextView) v.findViewById(R.id.price_txv);
            TextView roomTypeTxv = (TextView) v.findViewById(R.id.room_type_txv);
            TextView roomDetailTxv = (TextView) v.findViewById(R.id.room_detail_txv);
            Button bookBtn = (Button) v.findViewById(R.id.book_btn);
            priceTxv.setText(mSphinx.getString(R.string.price_show, roomType.getPrice()));
            roomTypeTxv.setText(roomType.getRoomType());
            roomDetailTxv.setText(roomType.getBedType() + " " + roomType.getBreakfast() + " " + roomType.getNetService()
                    + " " + roomType.getFloor() + " " + roomType.getArea());
            if (roomType.getCanReserve() == 0) {
                bookBtn.setText(mSphinx.getString(R.string.hotel_btn_sold_out));
                bookBtn.setClickable(false);
            } else {
                bookBtn.setText(mSphinx.getString(R.string.hotel_btn_book));
                bookBtn.setClickable(true);
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
    }
    
    void findViews(){
        mDynamicRoomTypeListView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_list_view);
        mDynamicRoomTypeMoreView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_more_view);
//        mDynamicHotelChooseTimeView = (LinearLayout) mRootView.findViewById(R.id.dynamic_hotel_choosetime_view);
//        mDynamicHotelLowerView = (LinearLayout) mRootView.findViewById(R.id.layout_below_feature);
    }
    
    public void setData(Hotel hotel, Calendar in, Calendar out){
        this.checkin = in;
        this.checkout = out;
        mHotel = hotel;
    }
    
    @Override
    protected void addDynamicPOIViewBlock(LinearLayout belongsLayout) {
    }

    @Override
    public List<DynamicPOIViewBlock> getViewList(List<Hotel> dataList) {
        if (dataList == null || dataList.size() == 0) {
            return null;
        }
        mHotel = dataList.get(0);

        mAllRoomList.clear();
        mAllRoomList.addAll(mHotel.getRoomTypeList());
        int size = (mAllRoomList != null? mAllRoomList.size() : 0);
        if (size == 0) {
            return null;
        } else if (size > SHOW_DYNAMIC_HOTEL_MAX) {
            for(int i = 0; i < SHOW_DYNAMIC_HOTEL_MAX; i++) {
                mShowingRoomList.add(mAllRoomList.get(i));
            }
        } else {
            mShowingRoomList.addAll(mAllRoomList);
        }
        roomTypeList.refreshList(mShowingRoomList);
        
//        for(int i = 0; i < viewCount; i++) {
//            View child = mDynamicRoomTypeListView.getChildAt(i);
//            if (i == (viewCount-1) && mDynamicRoomTypeMoreView.getVisibility() == View.GONE) {
//                child.setBackgroundResource(R.drawable.list_footer);
//                child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
//            } else {
//                child.setBackgroundResource(R.drawable.list_middle);
//                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
//            }
//        }
        
        List<DynamicPOIViewBlock> blockList = new LinkedList<DynamicPOIViewBlock>();
        blockList.add(mUpperBlock);
        return blockList;
    }

    //TODO:移走
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
        LogWrapper.d("conan", "buildingHotelQuery:" + criteria);
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
        query.setup(criteria, Globals.getCurrentCityId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId());
        return query;
    }
    @Override
    public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response) {
        // TODO Auto-generated method stub
        if (response instanceof RoomTypeDynamic) {
            RoomTypeDynamic roomInfo = ((RoomTypeDynamic)response);
            if (roomInfo.getNum() > 0){
                //如果有房，跳转
                //TODO:使用实际参数
                mSphinx.getHotelOrderWriteFragment().setData(mPOI, mClickedRoomType, roomInfo, checkin, checkout);
                mSphinx.showView(R.id.view_hotel_order_write);
            } else {
                //更新按钮状态
                mClickedBookBtn.setText(mSphinx.getString(R.string.hotel_btn_sold_out));
                mClickedBookBtn.setClickable(false);
            }
            
        }
    }
    @Override
    public boolean checkExistence(POI poi) {
        List<DynamicPOI> list = poi.getDynamicPOIList();
        for (int i = 0, size = list.size(); i < size; i++) {
            DynamicPOI iter = list.get(i);
            if (iter.getType().equals(DynamicPOI.TYPE_HOTEL)) {
                return true;
            }
        }
        return false;
    }
        
    public List<BaseQuery> generateQuery(POI poi) {
        mPOI = poi;
        List<BaseQuery> baseQueryList = new LinkedList<BaseQuery>();
        if (mHotel == null) {
            BaseQuery baseQuery = buildHotelQuery(checkin, checkout, poi, Hotel.NEED_FILED_DETAIL+Hotel.NEED_FILED_LIST);
            baseQueryList.add(baseQuery);
        } else if (mHotel != null && mHotel.getRoomTypeList() == null) {
            BaseQuery baseQuery = buildHotelQuery(checkin, checkout, poi, Hotel.NEED_FILED_DETAIL);
            baseQueryList.add(baseQuery);
        }
        LogWrapper.d("conan", "query in hotel:" + baseQueryList);
        return baseQueryList;
    }
    
}

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow.OnDismissListener;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
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
import com.tigerknows.ui.hotel.HotelIntroFragment;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

public class DynamicHotelPOI extends DynamicPOIView implements DateListView.CallBack {

    final static int PARENT_VIEW = 1;
    final static int DATA = 0;
    DynamicPOIViewBlock mUpperBlock;
    DynamicPOIViewBlock mLowerBlock;
    List<DynamicPOIViewBlock> blockList = new LinkedList<DynamicPOIViewBlock>();
    
    Hotel mHotel;
    Calendar checkin;
    Calendar checkout;
    Button mClickedBookBtn;
    View mClickedChild;
    RoomType mClickedRoomType;
    POI mPOI;
    List<RoomType> mShowingRoomList = new ArrayList<RoomType>();
    List<RoomType> mAllRoomList = new ArrayList<RoomType>();
    final int SHOW_DYNAMIC_HOTEL_MAX = 3;
    MoreRoomTypeClickListener moreRoomTypeClickListener;
    roomTypeClickListener mItemClickListener = new roomTypeClickListener();
    
    private LinearLayout mDynamicRoomTypeListView;
    
    private LinearLayout mDynamicRoomTypeMoreView;
    
    LinearListView roomTypeList;
    ImageView hotelImage;
    ImageView moreRoomTypeArrow;
    TextView hotelSummary;
    private DateWidget mCheckInDat;
    private DateWidget mCheckOutDat;
    View mCheckView;
    TextView moreTxv;
    TextView imageNumTxv;
    
    private DateListView mDateListView = null;
    
    DateListView getDateListView() {
        if (mDateListView == null) {
            DateListView view = new DateListView(mSphinx);
            view.setData(this, mPOIDetailFragment.mActionTag);
            mDateListView = view;
        }
        return mDateListView;
    }
    
    private void showDateListView(View parent) {
        mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.PopupWindowFilter);
        DateListView view = getDateListView();
        PopupWindow popupWindow = mPOIDetailFragment.getPopupWindow();
        if (popupWindow == null) {
            popupWindow = new PopupWindow(view);
            popupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            popupWindow.setFocusable(true);
            // 设置允许在外点击消失
            popupWindow.setOutsideTouchable(true);

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            popupWindow.setBackgroundDrawable(new BitmapDrawable());
            popupWindow.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.PopupWindowFilter + ActionLog.Dismiss);
                }
            });
            mPOIDetailFragment.setPopupWindow(popupWindow);
        }
        popupWindow.showAsDropDown(parent, 0, 0);
    }
    
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
                v.setClickable(false);
            } else {
                bookBtn.setEnabled(true);
                try {
                    bookBtn.setTag(R.id.tag_hotel_room_type_data, roomType);
                    bookBtn.setTag(R.id.tag_hotel_room_child_view, v);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bookBtn.setOnClickListener(mItemClickListener);
                v.setTag(roomType);
                v.setOnClickListener(mItemClickListener);
            }
        }
            
    };
    
    public DynamicHotelPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
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
        
        View.OnClickListener dateListener = new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                showDateListView(mPOIDetailFragment.mTitleFragment);
            }
        };
        mCheckView.setOnClickListener(dateListener);
    }
    
    void findViews(){
        mDynamicRoomTypeListView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_list_view);
        mDynamicRoomTypeMoreView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_more_view);
        hotelImage = (ImageView)mLowerBlock.mOwnLayout.findViewById(R.id.icon_imv);
        hotelSummary = (TextView)mLowerBlock.mOwnLayout.findViewById(R.id.short_summary);
        mDynamicRoomTypeMoreView = (LinearLayout) mUpperBlock.mOwnLayout.findViewById(R.id.dynamic_roomtype_more_view);
        mCheckInDat = (DateWidget) mUpperBlock.mOwnLayout.findViewById(R.id.checkin_dat);
        mCheckOutDat = (DateWidget) mUpperBlock.mOwnLayout.findViewById(R.id.checkout_dat);
        imageNumTxv = (TextView) mLowerBlock.mOwnLayout.findViewById(R.id.image_num_txv);
        moreTxv = (TextView) mDynamicRoomTypeMoreView.findViewById(R.id.more_txv);
        moreRoomTypeArrow = (ImageView) mDynamicRoomTypeMoreView.findViewById(R.id.more_imv);
        mCheckView = mUpperBlock.mOwnLayout.findViewById(R.id.check_view);
    }
    
    class MoreRoomTypeClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            if (mUpperBlock.mLoadSucceed) {
                roomTypeList.refreshList(mAllRoomList);
                refreshBackground(roomTypeList, mAllRoomList);
                mDynamicRoomTypeMoreView.setVisibility(View.GONE);
            } else {
                BaseQuery baseQuery = buildHotelQuery(checkin, checkout, mPOI, Hotel.NEED_FILED_DETAIL);
                baseQuery.setTipText(mSphinx.getString(R.string.doing_and_wait));
                List<BaseQuery> list = new ArrayList<BaseQuery>();
                list.add(baseQuery);
                query(mPOIDetailFragment, list);
            }
        }
        
    }

    @Override
    public void confirm() {
        Calendar in = getDateListView().getCheckin();
        Calendar out = getDateListView().getCheckout();
        if (out.get(Calendar.DAY_OF_YEAR) == checkout.get(Calendar.DAY_OF_YEAR)
                && in.get(Calendar.DAY_OF_YEAR ) == checkin.get(Calendar.DAY_OF_YEAR)) {
            mPOIDetailFragment.dismissPopupWindow();
            return;
        } else {
            checkin = in;
            checkout = out;
        }
        refreshDate();
        BaseQuery baseQuery = buildHotelQuery(checkin, checkout, mPOI, Hotel.NEED_FILED_DETAIL);
        baseQuery.setTipText(mSphinx.getString(R.string.doing_and_wait));
        mPOIDetailFragment.mSphinx.queryStart(baseQuery);
        mPOIDetailFragment.dismissPopupWindow();
    }

    @Override
    public void cancel() {
        mPOIDetailFragment.dismissPopupWindow();
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
        getDateListView().refresh(checkin, checkout);
    }
    
    final public void refreshDate() {
//        DateListView dateListView = getDateListView();
        mCheckInDat.setCalendar(checkin);
        mCheckOutDat.setCalendar(checkout);
    }

    @Override
    public List<DynamicPOIViewBlock> getViewList(POI poi) {
        blockList.clear();
        if (!mUpperBlock.mLoadSucceed) {
            String value = mPOI.getDescriptionValue(Description.FIELD_SYNOPSIS);
            hotelSummary.setText(value);
            moreTxv.setText(mSphinx.getString(R.string.hotel_click_to_reload));
            moreRoomTypeArrow.setVisibility(View.GONE);
            mDynamicRoomTypeMoreView.setClickable(true);
            mDynamicRoomTypeMoreView.setVisibility(View.VISIBLE);
            roomTypeList.refreshList(null);
            blockList.add(mUpperBlock);
            blockList.add(mLowerBlock);
            LogWrapper.i(TAG, "Hotel viewBlock is:" + blockList);
            return blockList;
        }
        if (poi == null || poi.getHotel() == null || poi.getHotel().getRoomTypeList() == null) {
            LogWrapper.i(TAG, "poi or hotel or roomTypeList is null, nothing to show for DynamicHotel");
            return blockList;
        }
        mPOI = poi;
        mHotel = poi.getHotel();
        refreshDate();
        getDateListView().refresh(checkin, checkout);

        mAllRoomList.clear();
        mShowingRoomList.clear();
        mAllRoomList.addAll(mHotel.getRoomTypeList());
        Collections.sort(mAllRoomList, new RoomTypeCMP());
        int size = (mAllRoomList != null? mAllRoomList.size() : 0);
        if (size == 0) {
            LogWrapper.i(TAG, "size of roomTypeList is 0.");
            moreTxv.setText(mSphinx.getString(R.string.hotel_no_roomtype));
            moreRoomTypeArrow.setVisibility(View.GONE);
            mDynamicRoomTypeMoreView.setClickable(false);
            mDynamicRoomTypeMoreView.setVisibility(View.VISIBLE);
        } else if (size > SHOW_DYNAMIC_HOTEL_MAX) {
            for(int i = 0; i < SHOW_DYNAMIC_HOTEL_MAX; i++) {
                mShowingRoomList.add(mAllRoomList.get(i));
            }
            moreRoomTypeArrow.setVisibility(View.VISIBLE);
            moreTxv.setText(mSphinx.getString(R.string.hotel_expand_roomtype));
            mDynamicRoomTypeMoreView.setClickable(true);
            mDynamicRoomTypeMoreView.setVisibility(View.VISIBLE);
        } else {
            mShowingRoomList.addAll(mAllRoomList);
            mDynamicRoomTypeMoreView.setVisibility(View.GONE);
        }
        roomTypeList.refreshList(mShowingRoomList);
        refreshBackground(roomTypeList, mShowingRoomList);
        
        List<HotelTKDrawable> b = mHotel.getHotelTKDrawableList();
        
        String value = mPOI.getDescriptionValue(Description.FIELD_SYNOPSIS);
        if (TextUtils.isEmpty(value)) {
            value = mSphinx.getString(R.string.hotel_no_summary);
        }
        hotelSummary.setText(value);
        LinearLayout hotelSummaryBlock = (LinearLayout) mLowerBlock.mOwnLayout.findViewById(R.id.hotel_summary);
        hotelSummaryBlock.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				HotelIntroFragment hotelIntro = mSphinx.getHotelIntroFragment();
				hotelIntro.setData(mPOI, checkin, checkout);
				mSphinx.showView(R.id.view_hotel_intro);
			}
		});
        int picNum = (b == null ? 0 : b.size());
        imageNumTxv.setText(mSphinx.getString(R.string.pictures, picNum));
        //FIXME:如何获取这个Image?
        if (b != null) {
            final TKDrawable tkDrawable = b.get(0).getTKDrawable();
            if (tkDrawable != null) {
                Drawable hotelImageDraw = tkDrawable.loadDrawable(mSphinx, new Runnable() {
    
                    @Override
                    public void run() {
                        hotelImage.setMaxHeight(Util.dip2px(Globals.g_metrics.density, 80));
                        hotelImage.setMaxWidth(Util.dip2px(Globals.g_metrics.density, 80));
                        hotelImage.setBackgroundDrawable(tkDrawable.loadDrawable(null, null, null));
                    }
                    
                }, mPOIDetailFragment.toString());
                if (hotelImageDraw != null) {
                    hotelImage.setBackgroundDrawable(hotelImageDraw);
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
            if (v instanceof Button) {
                mClickedBookBtn = (Button)v;
                mClickedChild = (View) v.getTag(R.id.tag_hotel_room_child_view);
            } else {
                mClickedBookBtn = (Button) v.findViewById(R.id.book_btn);
                mClickedChild = v;
            }
            mData = (RoomType) mClickedBookBtn.getTag(R.id.tag_hotel_room_type_data);
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
        dataOpration.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId());
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
        query.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
        return query;
    }
    @Override
    public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response) {
        if (response instanceof RoomTypeDynamic) {
            RoomTypeDynamic roomInfo = ((RoomTypeDynamic)response);
            if (roomInfo.getNum() > 0){
                //如果有房，跳转
                mSphinx.getHotelOrderWriteFragment().setData(mPOI, mClickedRoomType, roomInfo, checkin, checkout);
                mSphinx.showView(R.id.view_hotel_order_write);
            } else {
                //更新按钮状态
                //FIXME:提示已定满
                mClickedRoomType.setCanReserve(0);
                mClickedBookBtn.setEnabled(false);
                mClickedChild.setClickable(false);
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
    
    final public void loadSucceed(boolean s) {
        mUpperBlock.mLoadSucceed = s;
        mLowerBlock.mLoadSucceed = s;
    }
    
    private class RoomTypeCMP implements Comparator<RoomType> {

        @Override
        public int compare(RoomType lhs, RoomType rhs) {
            return (int) (rhs.getCanReserve() - lhs.getCanReserve());
        }
    }
       
}


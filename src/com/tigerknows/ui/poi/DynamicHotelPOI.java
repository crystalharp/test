package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.HotelVendor;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.model.POI;
import com.tigerknows.model.ProxyQuery.RoomTypeDynamic;
import com.tigerknows.model.ProxyQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.TKDrawable.LoadImageRunnable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.common.ViewImageActivity;
import com.tigerknows.ui.hotel.DateListView;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.hotel.HotelIntroActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.LinearListAdapter;

public class DynamicHotelPOI extends DynamicPOIView implements DateListView.CallBack {

    final static String TAG = "DynamicHotelPOI";
    
    final static int PARENT_VIEW = 1;
    final static int DATA = 0;
    DynamicPOIViewBlock mUpperBlock;
    DynamicPOIViewBlock mLowerBlock;
    List<DynamicPOIViewBlock> blockList = new LinkedList<DynamicPOIViewBlock>();
    
    int mHotelCityId;
    Hotel mHotel;
    Calendar checkin;
    Calendar checkout;
    Button mClickedBookBtn;
    View mClickedChild;
    RoomType mClickedRoomType;
    List<RoomType> mShowingRoomList = new ArrayList<RoomType>();
    List<RoomType> mAllRoomList = new ArrayList<RoomType>();
    final int SHOW_DYNAMIC_HOTEL_MAX = 6;
    MoreRoomTypeClickListener moreRoomTypeClickListener;
    roomTypeClickListener mItemClickListener = new roomTypeClickListener();
    
    View upperView;
    View lowerView;
    private LinearLayout mDynamicRoomTypeListView;
    private LinearLayout mDynamicRoomTypeMoreView;
    LinearLayout mRetryView;
    LinearLayout hotelSummaryBlock;
    LinearListAdapter roomTypeAdapter;
    ImageView hotelImage;
    ImageView moreRoomTypeArrow;
    TextView hotelSummary;
    View mCheckInTimeView;
    TextView moreTxv;
    TextView imageNumTxv;
    TextView retryTxv;
    private TextView mCheckInTimeTxv;
    
    private DateListView mDateListView = null;
    
    //记录现在缓存的日期所属的POI的id
    public String mInitDatePOIid;
        
    DateListView getDateListView() {
        if (mDateListView == null) {
            DateListView view = new DateListView(mSphinx);
            View v = mInflater.inflate(R.layout.time_list_item, view, false);
            v.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int h = v.getMeasuredHeight();
            view.findViewById(R.id.body_view).getLayoutParams().height = h*6;
            ((ViewGroup) view.findViewById(R.id.selected_view)).setPadding(0, h, 0, 0);
            view.setData(this, mPOIDetailFragment.mActionTag);
            mDateListView = view;
        }
        return mDateListView;
    }
    
    final int STATE_LOAD_FAILED = 1;
    final int STATE_NO_DATA = 2;
    final int STATE_DATA_LT_MAX = 3;
    final int STATE_DATA_GT_MAX = 4;
    void setState(int s) {
        if (s == STATE_DATA_GT_MAX) {
            mDynamicRoomTypeMoreView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
        } else if (s == STATE_LOAD_FAILED) {
            retryTxv.setText(Html.fromHtml(mSphinx.getString(R.string.hotel_click_to_reload)));
            mDynamicRoomTypeMoreView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mRetryView.setClickable(true);
        } else if (s == STATE_NO_DATA) {
            retryTxv.setText(mSphinx.getString(R.string.hotel_no_roomtype));
            mDynamicRoomTypeMoreView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mRetryView.setClickable(false);
        } else if (s == STATE_DATA_LT_MAX) {
            mDynamicRoomTypeMoreView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
        }
    }
    
    private void showDateListView(View parent) {
        mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.HotelDate);
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
                    mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.HotelDate + ActionLog.Dismiss);
                }
            });
            popupWindow.setAnimationStyle(R.style.AlterImageDialog);
            popupWindow.update();
            mPOIDetailFragment.setPopupWindow(popupWindow);
        }
        view.refresh(checkin, checkout);
        popupWindow.showAsDropDown(parent, 0, 0);
    }
    
    public DynamicHotelPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        upperView = mInflater.inflate(R.layout.poi_dynamic_hotel_upper, null);
        lowerView = mInflater.inflate(R.layout.poi_dynamic_hotel_lower, null);
        findViews();
        
        mUpperBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, upperView) {

            @Override
            public void refresh() {
                refreshDate();
                if (!mUpperBlock.mLoadSucceed
                        || mHotel == null || mHotel.getRoomTypeList() == null) {
                    setState(STATE_LOAD_FAILED);
                    roomTypeAdapter.refreshList(null);
                    return;
                }
                
                mAllRoomList.clear();
                mShowingRoomList.clear();
                mAllRoomList.addAll(mHotel.getRoomTypeList());
                int size = (mAllRoomList != null? mAllRoomList.size() : 0);
                if (size == 0) {
                    LogWrapper.i(TAG, "size of roomTypeList is 0.");
                    setState(STATE_NO_DATA);
                } else if (size > SHOW_DYNAMIC_HOTEL_MAX) {
                    for(int i = 0; i < SHOW_DYNAMIC_HOTEL_MAX; i++) {
                        mShowingRoomList.add(mAllRoomList.get(i));
                    }
                    setState(STATE_DATA_GT_MAX);
                } else {
                    mShowingRoomList.addAll(mAllRoomList);
                    setState(STATE_DATA_LT_MAX);
                }
                roomTypeAdapter.refreshList(mShowingRoomList);
                refreshBackground(roomTypeAdapter, mShowingRoomList);
                show();
            }
            
        };
        
        mLowerBlock = new DynamicPOIViewBlock(poiFragment.mBelowCommentLayout, lowerView) {

            @Override
            public void refresh() {
                String value = mHotel.getLongDescription();
                if (TextUtils.isEmpty(value)) {
                    value = mSphinx.getString(R.string.hotel_no_summary);
                    hotelSummaryBlock.setClickable(false);
                } else {
                    hotelSummaryBlock.setClickable(true);
                }
                hotelSummary.setText(value);

                List<HotelTKDrawable> picList = mHotel.getHotelTKDrawableList();
                int picNum = (picList == null ? 0 : picList.size());
                refreshPicture();
                if (picNum == 0) {
                    hotelImage.setClickable(false);
                } else {
                    hotelImage.setClickable(true);
                }
                show();
            }
            
        };
        
        roomTypeAdapter = new LinearListAdapter(mSphinx, mDynamicRoomTypeListView, R.layout.poi_dynamic_hotel_room_item) {

            @Override
            public View getView(Object data, View child, int pos) {
                RoomType roomType = (RoomType)data;
                TextView priceTxv = (TextView) child.findViewById(R.id.price_txv);
                TextView roomTypeTxv = (TextView) child.findViewById(R.id.room_type_txv);
                TextView roomDetailTxv = (TextView) child.findViewById(R.id.room_detail_txv);
                TextView roomGuaranteeTxv = (TextView) child.findViewById(R.id.guarantee_txv);
                TextView roomSubtitleTxv = (TextView) child.findViewById(R.id.room_subtitle);
                TextView roomSourceTxv = (TextView) child.findViewById(R.id.room_source_txv);
                Button bookBtn = (Button) child.findViewById(R.id.book_btn);
                priceTxv.setText(roomType.getPrice());
                roomTypeTxv.setText(roomType.getRoomType());
                roomDetailTxv.setText(roomType.generateDescription());
                roomGuaranteeTxv.setVisibility(roomType.getNeedGuarantee() == 0 ? View.GONE : View.VISIBLE);
                if (roomType.getSubtitle() != null) {
                    roomSubtitleTxv.setVisibility(View.VISIBLE);
                    roomSubtitleTxv.setText(roomType.getSubtitle());
                } else {
                    roomSubtitleTxv.setVisibility(View.GONE);
                }
                if (!TextUtils.isEmpty(roomType.getVendorName())) {
                    roomSourceTxv.setVisibility(View.VISIBLE);
                    roomSourceTxv.setText(mSphinx.getString(R.string.come_from_colon, roomType.getVendorName()));
                } else {
                    roomSourceTxv.setVisibility(View.GONE);
                }
                if (roomType.getCanReserve() == 0) {
                    refreshBookBtn(bookBtn, roomType.getCanReserve());
                    child.setClickable(false);
                } else {
                    refreshBookBtn(bookBtn, roomType.getCanReserve());
                    try {
                        bookBtn.setTag(R.id.tag_hotel_room_type_data, roomType);
                        bookBtn.setTag(R.id.tag_hotel_room_child_view, child);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bookBtn.setOnClickListener(mItemClickListener);
                    child.setTag(roomType);
                    child.setOnClickListener(mItemClickListener);
                }
                return null;
            }};
        
        moreRoomTypeClickListener = new MoreRoomTypeClickListener();
        mDynamicRoomTypeMoreView.setOnClickListener(moreRoomTypeClickListener);
        
        View.OnClickListener dateListener = new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelClickDate);
                showDateListView(mPOIDetailFragment.mSkylineView);
            }
        };
        mCheckInTimeView.setOnClickListener(dateListener);
        
        View.OnClickListener retryListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelFailRetry);
                //不能直接重发上一个请求，否则无网络的时候进详情页，再改个日期，所发的请求nf字段就只有房态信息了。
                BaseQuery baseQuery = buildHotelQuery(checkin, checkout, mPOI, Hotel.NEED_FIELD_DETAIL + Hotel.NEED_FIELD_LIST);
                baseQuery.setTipText(mSphinx.getString(R.string.doing_and_wait));
                queryStart(baseQuery);
            }
        };
        mRetryView.setOnClickListener(retryListener);
        
        hotelSummaryBlock.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mPOI.getHotel() != null) {
                    mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelIntro);
                    Intent intent = new Intent();
                    intent.putExtra(HotelIntroActivity.EXTRA_NAME, mPOI.getName());
                    Hotel hotel = mPOI.getHotel();
                    intent.putExtra(HotelIntroActivity.EXTRA_LONG_DESCRIPTION, hotel.getLongDescription());
                    intent.putExtra(HotelIntroActivity.EXTRA_ROOM_DESCRIPTION, hotel.getRoomDescription());
                    mSphinx.showView(R.id.activity_hotel_intro, intent);
                }
            }
        });
        hotelImage.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Hotel hotel = mPOI.getHotel();
                if (hotel != null && hotel.getHotelTKDrawableList() != null && hotel.getHotelTKDrawableList().size() > 0) {
                    mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelImage);
                    Intent intent = new Intent();
                    ArrayList<HotelTKDrawable> list = (ArrayList<HotelTKDrawable>)hotel.getHotelTKDrawableList();
                    intent.putExtra(ViewImageActivity.EXTRA_TITLE, mSphinx.getString(R.string.hotel_picture_title, list.size()));
                    intent.putParcelableArrayListExtra(ViewImageActivity.EXTRA_IMAGE_LIST, list);
                    intent.putParcelableArrayListExtra(ViewImageActivity.EXTRA_ORIGINAL_IMAGE_LIST, (ArrayList<HotelTKDrawable>)hotel.getOriginalHotelTKDrawableList());
                    mSphinx.showView(R.id.activity_common_view_image, intent);
                }
            }
        });
    }
    
    void findViews(){
        mDynamicRoomTypeListView = (LinearLayout) upperView.findViewById(R.id.dynamic_roomtype_list_view);
        mDynamicRoomTypeMoreView = (LinearLayout) upperView.findViewById(R.id.dynamic_roomtype_more_view);
        hotelImage = (ImageView)lowerView.findViewById(R.id.icon_imv);
        hotelSummary = (TextView)lowerView.findViewById(R.id.short_summary);
        mDynamicRoomTypeMoreView = (LinearLayout) upperView.findViewById(R.id.dynamic_roomtype_more_view);
        mRetryView = (LinearLayout) upperView.findViewById(R.id.dynamic_roomtype_retry_view);
        imageNumTxv = (TextView) lowerView.findViewById(R.id.image_num_txv);
        moreTxv = (TextView) mDynamicRoomTypeMoreView.findViewById(R.id.more_txv);
        retryTxv = (TextView)mRetryView.findViewById(R.id.retry_txv);
        moreRoomTypeArrow = (ImageView) mDynamicRoomTypeMoreView.findViewById(R.id.more_imv);
        mCheckInTimeView = upperView.findViewById(R.id.check_in_time_view);
        hotelSummaryBlock = (LinearLayout) lowerView.findViewById(R.id.hotel_summary);
        mCheckInTimeTxv = (TextView) upperView.findViewById(R.id.check_in_time_txv);
    }
    
    class MoreRoomTypeClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelMoreRoom);
            roomTypeAdapter.refreshList(mAllRoomList);
            mDynamicRoomTypeMoreView.setVisibility(View.GONE);
            refreshBackground(roomTypeAdapter, mAllRoomList);
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
        BaseQuery baseQuery = buildHotelQuery(checkin, checkout, mPOI, Hotel.NEED_FIELD_ROOM_INFO);
        baseQuery.setTipText(mSphinx.getString(R.string.doing_and_wait));
        queryStart(baseQuery);
        mPOIDetailFragment.dismissPopupWindow();
    }

    @Override
    public void cancel() {
        mPOIDetailFragment.dismissPopupWindow();
    }
    
    final public void initDate() {
        mInitDatePOIid = (mPOI != null ? mPOI.getUUID() : null);
        checkin = Calendar.getInstance();
        checkin.setTimeInMillis(CalendarUtil.getExactTime(mSphinx));
        int hour = checkin.get(Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour <= 4) {
            checkin.add(Calendar.DAY_OF_YEAR, -1);
        }
        checkout = (Calendar) checkin.clone();
        checkout.add(Calendar.DAY_OF_YEAR, 1);
        refreshDate();
    }
    
    final public void initDate(Calendar in, Calendar out) {
        mInitDatePOIid = (mPOI != null ? mPOI.getUUID() : null);
        checkout = out;
        checkin = in;
        refreshDate();
    }
    
    final public void clearDateCache() {
        mInitDatePOIid = null;
    }
    
    final public void refreshDate() {
        DateListView dateListView = getDateListView();
        dateListView.refresh(checkin, checkout);
        mCheckInTimeTxv.setText(dateListView.getCheckDescription().toString());
    }

    public List<DynamicPOIViewBlock> getViewList() {
        blockList.clear();
        blockList.add(mUpperBlock);
        blockList.add(mLowerBlock);
        return blockList;
    }
    
    public void refresh() {
        mHotel = mPOI.getHotel();
        mUpperBlock.refresh();
        if (mLowerBlock.mLoadSucceed) {
            mLowerBlock.refresh();
        }
        refreshPicture();
    }
    
    public void refreshPicture() {
        List<HotelTKDrawable> picList = mHotel.getHotelTKDrawableList();
        int picNum = (picList == null ? 0 : picList.size());
        imageNumTxv.setText(mSphinx.getString(R.string.pictures, picNum));
        boolean setDefault = true;
        if (picList != null && picList.size() > 0) {
            TKDrawable tkDrawable = picList.get(0).getTKDrawable();
            if (tkDrawable != null) {
                LoadImageRunnable loadImageRunnable = new LoadImageRunnable(mSphinx, tkDrawable, hotelImage, R.drawable.bg_picture_detail, mPOIDetailFragment.toString());
                Drawable hotelImageDraw = tkDrawable.loadDrawable(mSphinx, loadImageRunnable, mPOIDetailFragment.toString());
                if (hotelImageDraw != null) {
                    hotelImage.setImageDrawable(hotelImageDraw);
                    hotelImage.setBackgroundResource(R.drawable.bg_picture_detail);
                    setDefault = false;
                }
            }
        } else {
            hotelImage.setImageDrawable(null);
            hotelImage.setBackgroundResource(R.drawable.bg_picture_none);
            setDefault = false;
        }
        if (setDefault)  {
            hotelImage.setImageDrawable(null);
            hotelImage.setBackgroundResource(R.drawable.bg_picture_detail);
        }
    }
    
    void refreshBookBtn(Button btn, long restRoom) {
        if (restRoom > 3) {
            btn.setText(mSphinx.getString(R.string.hotel_btn_book));
            btn.setEnabled(true);
        } else if (restRoom > 0) {
            btn.setText(mSphinx.getString(R.string.hotel_btn_x_room_left, restRoom));
            btn.setEnabled(true);
        } else {
            btn.setText(mSphinx.getString(R.string.hotel_btn_sold_out));
            btn.setEnabled(false);
        }
    }

    class roomTypeClickListener implements View.OnClickListener{
        
        @Override
        public void onClick(View v) {
            RoomType mData;
            if (v instanceof Button) {
                mClickedBookBtn = (Button)v;
                mClickedChild = (View) v.getTag(R.id.tag_hotel_room_child_view);
                mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelRoomBookBtn);
            } else {
                mClickedBookBtn = (Button) v.findViewById(R.id.book_btn);
                mClickedChild = v;
                mPOIDetailFragment.mActionLog.addAction(ActionLog.POIDetail + ActionLog.POIDetailHotelClickRoomItem);
            }
            mData = (RoomType) mClickedBookBtn.getTag(R.id.tag_hotel_room_type_data);
            mClickedRoomType = mData;
            List<BaseQuery> baseQueryList = new ArrayList<BaseQuery>();
            baseQueryList.add(buildRoomTypeDynamicQuery(mData.getVendorID(), mData.getHotelID(), mData.getRoomId(), mData.getRateplanId(), checkin, checkout));
            queryStart(baseQueryList);
            
        }
        
    }

    private BaseQuery buildHotelQuery(Calendar checkin, Calendar checkout, POI poi, String needFiled){
        mHotelCityId = MapEngine.getCityId(mPOI.getPosition());
        String checkinTime = HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkin.getTime());
        String checkoutTime = HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkout.getTime());
        DataOperation dataOperation = new DataOperation(mSphinx);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_HOTEL);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, "01" + needFiled);   // 01表示poi的uuid
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_CHECKIN, checkinTime);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_CHECKOUT, checkoutTime);
        dataOperation.addParameter(DataOperation.SERVER_PARAMETER_HOTEL_SOURCE, HotelVendor.ALL);
        dataOperation.setup(mHotelCityId, mPOIDetailFragment.getId(), mPOIDetailFragment.getId());
        return dataOperation;
    }
    
    private BaseQuery buildRoomTypeDynamicQuery(long vendorID, String hotelId, String roomId, String pkgId, Calendar checkin, Calendar checkout){
        mHotelCityId = MapEngine.getCityId(mPOI.getPosition());
        ProxyQuery query = new ProxyQuery(mSphinx);
        query.addParameter(ProxyQuery.SERVER_PARAMETER_VENDORID, String.valueOf(vendorID));
        query.addParameter(ProxyQuery.SERVER_PARAMETER_CHECKIN_DATE, HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkin.getTime()));
        query.addParameter(ProxyQuery.SERVER_PARAMETER_CHECKOUT_DATE, HotelHomeFragment.SIMPLE_DATE_FORMAT.format(checkout.getTime()));
        query.addParameter(ProxyQuery.SERVER_PARAMETER_HOTELID, hotelId);
        query.addParameter(ProxyQuery.SERVER_PARAMETER_ROOMID, roomId);
        query.addParameter(ProxyQuery.SERVER_PARAMETER_ROOM_TYPE_TAOCANID, pkgId);
        query.addParameter(ProxyQuery.SERVER_PARAMETER_TASK, "1");
        query.setup(mHotelCityId, mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
        return query;
    }
        
    public List<BaseQuery> generateQuery(POI poi) {
        mPOI = poi;
        mHotel = mPOI.getHotel();
        List<BaseQuery> baseQueryList = new LinkedList<BaseQuery>();
        Calendar checkinInHoteHome = mSphinx.getHotelHomeFragment().getCheckin();
        Calendar checkoutInHoteHome = mSphinx.getHotelHomeFragment().getCheckout();
        boolean updateCanReserve = false;
        if (this.checkin.equals(checkinInHoteHome) && this.checkout.equals(checkoutInHoteHome)) {
            updateCanReserve = true;
        }
        String nf = Hotel.NEED_FIELD_DETAIL;
        if (mHotel != null && mHotel.getRoomTypeList() == null) {
            if (updateCanReserve) {
                nf += Util.byteToHexString(Hotel.FIELD_CAN_RESERVE);
            }
            BaseQuery baseQuery = buildHotelQuery(this.checkin, this.checkout, poi, nf);
            baseQueryList.add(baseQuery);
            LogWrapper.i(TAG, "hotel.roomtype is null, generate Query:" + baseQueryList);
        } else {
            nf += Hotel.NEED_FIELD_APPEND_DETAIL;
            if (updateCanReserve) {
                nf += Util.byteToHexString(Hotel.FIELD_CAN_RESERVE);
            }
            BaseQuery baseQuery = buildHotelQuery(this.checkin, this.checkout, poi, nf);
            baseQueryList.add(baseQuery);
            LogWrapper.i(TAG, "hotel is null, generate Query:" + baseQueryList);
        }
        return baseQueryList;
    }
    
    private void refreshBackground(LinearListAdapter lsv, List<RoomType> list) {
        int size = list.size();
        for(int i = 0; i < size; i++) {
            View child = lsv.getChildView(i);
            if (i == (size-1) && mDynamicRoomTypeMoreView.getVisibility() == View.GONE) {
                child.setBackgroundResource(R.drawable.list_footer);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
            }
            child.setPadding(Util.dip2px(Globals.g_metrics.density, 8), 0, Util.dip2px(Globals.g_metrics.density, 8), 0);
        }
 
    }
    
    final public void loadSucceed(boolean s) {
        mUpperBlock.mLoadSucceed = s;
        mLowerBlock.mLoadSucceed = s;
    }
    
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
	    mPOIDetailFragment.minusLoadingView();
	    POI poi = mPOI;
        if (poi == null) {
            return;
        }
        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        int mPOIFragmentId = mPOIDetailFragment.getId();
        for(BaseQuery baseQuery : baseQueryList) {
            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIFragmentId, mPOIFragmentId, mPOIFragmentId, null)) {
                mPOIDetailFragment.isReLogin = true;
                return;
            }
            Response response = baseQuery.getResponse();

            if (baseQuery instanceof DataOperation) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                    Toast.makeText(mSphinx, mSphinx.getString(R.string.network_failed), Toast.LENGTH_SHORT).show();
                    loadSucceed(false);
                    refresh();
                    return;
                }
                POI onlinePOI = ((POIQueryResponse)response).getPOI();
                if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(poi.getUUID())) {
                    try {
                        poi.init(onlinePOI.getData(), false);
                        loadSucceed(true);
                        refresh();
                        mPOIDetailFragment.refreshDetail();
                    } catch (APIException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (baseQuery instanceof ProxyQuery) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, new int[]{824}, true, this, false)) {
                    if (response != null) {
                        if (response.getResponseCode() == 824) {
                            mClickedRoomType.setCanReserve(0);
                            refreshBookBtn(mClickedBookBtn, 0);
                            mClickedChild.setClickable(false);
                            Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_no_room_left));
                        }
                    }
                    return;
                }
                RoomTypeDynamic roomInfo = ((RoomTypeDynamic)response);
                if (roomInfo.getNum() > 0){
                    //如果有房，跳转
                    mSphinx.getHotelOrderWriteFragment().setData(mPOI, mClickedRoomType, roomInfo, checkin, checkout);
                    mSphinx.showView(R.id.view_hotel_order_write);
                } else {
                    //更新按钮状态
                    mClickedRoomType.setCanReserve(0);
                    refreshBookBtn(mClickedBookBtn, 0);
                    mClickedChild.setClickable(false);
                    Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_no_room_left));
                }
            }
        }
	}

	@Override
	public void onCancelled(TKAsyncTask tkAsyncTask) {
		
	}

    @Override
    public void loadData(int fromType) {
        if (fromType == FROM_FAV_HISTORY) {
            initDate();
        } else if (fromType == FROM_ONRESUME) {
            if (!mPOI.getUUID().equals(mInitDatePOIid)) {
                if (mSphinx.uiStackContains(R.id.view_hotel_home)) {
                    HotelHomeFragment hotelFragment = mSphinx.getHotelHomeFragment();
                    initDate(hotelFragment.getCheckin(), hotelFragment.getCheckout());
                } else {
                    initDate();
                }
            }
        }
        queryStart(generateQuery(mPOI));
        mPOIDetailFragment.addLoadingView();
        
    }
       
}


/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.map.MapView;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.POI;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.ui.discover.CycleViewPager;
import com.tigerknows.ui.discover.CycleViewPager.CycleOnPageChangeListener;
import com.tigerknows.ui.discover.CycleViewPager.CyclePagerAdapter;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.util.Utility;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class InfoWindowFragment extends BaseFragment implements View.OnClickListener, CycleViewPager.CycleOnPageChangeListener.IRefreshViews {

    public static final int TYPE_MESSAGE = 0;    
    
    public static final int TYPE_POI = 1;    

    public static final int TYPE_TUANGUO = 2;

    public static final int TYPE_DYNAMIC_POI = 3;

    public static final int TYPE_HOTEL = 4;

    public static final int TYPE_CLICKED_SELECT_POINT = 5;

    public static final int TYPE_MY_LOCATION = 6;

    public static final int TYPE_LONG_CLIKED_SELECT_POINT = 7;

    /**
     * 下面这些ViewGroup用于InfoWindow
     * 最初是想避免重新创建它们，但是重用时会出现文字不对齐、高宽计算不协调情况？
     */
    private List<View> mInfoWindow = null;
    
    private ItemizedOverlay mItemizedOverlay;
    private int mOwerFragmentId;
    private int mType;
    private int mPostion;

    private int mTotalHeight;
    private int mTitleHeight;
    private int mPOIHeight;
    private int mHotelHeight;
    private int mTuangouHeight;
    private int mBottomHeight;
    
    public InfoWindowFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        setBackgroundColor(mSphinx.getResources().getColor(R.color.normal_background));
        
        mViewPager = new ViewPager(mSphinx);
        mCyclePagerAdapter = new CyclePagerAdapter();
        mCycleOnPageChangeListener = new CycleOnPageChangeListener(mSphinx, this, null, mActionTag);
        mViewPager.setOnPageChangeListener(mCycleOnPageChangeListener);
        mViewPager.setAdapter(mCyclePagerAdapter);
        mRootView = mViewPager;
        
        findViews();
        setListener();
        
        return mRootView;
    }

    protected void findViews() {
        super.findViews();
    }

    protected void setListener() {
        super.setListener();
    }

    @Override
    public void onClick(View v) {
        
        if (mItemizedOverlay == null) {
            return;
        }
        
        OverlayItem overlayItem=mItemizedOverlay.getItemByFocused();
        if (overlayItem == null) {
            return;
        }
        
        int id = v.getId();
        if (id == R.id.poi_btn) {
            mSphinx.getPOINearbyFragment().setData((POI) overlayItem.getAssociatedObject());
            mSphinx.showView(R.id.view_poi_nearby_search);
        } else if (id == R.id.traffic_btn) {
            Utility.queryTraffic(mSphinx, (POI) overlayItem.getAssociatedObject(), mActionTag);
        } else if (id == R.id.detail_btn) {
            infoWindowClicked();
        }
        
    }
    
    public void setData(int fragmentId, ItemizedOverlay itemizedOverlay, String actionTag) {
        mOwerFragmentId = fragmentId;
        mItemizedOverlay = itemizedOverlay;
        mActionTag = actionTag;
        
        OverlayItem currentOverlayItem = mItemizedOverlay.getItemByFocused();
        
        if (currentOverlayItem == null) {
            currentOverlayItem = itemizedOverlay.get(0);
            currentOverlayItem.isFoucsed = true;
        }

        mPostion = itemizedOverlay.getPositionByFocused();
        mCyclePagerAdapter.count = itemizedOverlay.size();
        mCyclePagerAdapter.notifyDataSetChanged();
        mCycleOnPageChangeListener.count = mCyclePagerAdapter.count;
        
        List<View> list = getInfoWindow();
        Object object = currentOverlayItem.getAssociatedObject();
        
        if (object instanceof POI) {
            final POI poi = (POI) object;
            int sourceType = poi.getSourceType();
            
            if (poi.getSourceType() == POI.SOURCE_TYPE_HOTEL) {
                mType = TYPE_HOTEL;
            } else if (sourceType == POI.SOURCE_TYPE_CLICKED_SELECT_POINT) {
                mType = TYPE_CLICKED_SELECT_POINT;
            } else if (sourceType == POI.SOURCE_TYPE_MY_LOCATION) {
                mType = TYPE_MY_LOCATION;
            } else if (sourceType == POI.SOURCE_TYPE_LONG_CLICKED_SELECT_POINT) {
                mType = TYPE_LONG_CLIKED_SELECT_POINT;
            } else {
                mType = TYPE_POI;
            }
            
        } else if (object instanceof Zhanlan || object instanceof Yanchu) {
            mType = TYPE_DYNAMIC_POI;
        } else if (object instanceof Tuangou) {
            mType = TYPE_TUANGUO;
        } else {
            mType = TYPE_MESSAGE;
        }

        mCyclePagerAdapter.viewList = list;
        layoutInfoWindowView();
        refreshViews(mPostion);

    }
    
    public int getOwerFragmentId() {
        return mOwerFragmentId;
    }
    
    private List<View> getInfoWindow() {
        if (mInfoWindow == null) {
            List<View> viewList = new ArrayList<View>();
            View view;
            view = mLayoutInflater.inflate(R.layout.info_window_fragment, this, false);
            setListenerToView(view);
            viewList.add(view);
            
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTotalHeight = view.getMeasuredHeight();
            View v = view.findViewById(R.id.title_txv);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTitleHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.poi_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mPOIHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.tuangou_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTuangouHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.hotel_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mHotelHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.bottom_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mBottomHeight = v.getMeasuredHeight();
            
            view = mLayoutInflater.inflate(R.layout.info_window_fragment, this, false);
            setListenerToView(view);
            viewList.add(view);
            
            view = mLayoutInflater.inflate(R.layout.info_window_fragment, this, false);
            setListenerToView(view);
            viewList.add(view);
            mInfoWindow = viewList;
        }

        return mInfoWindow;
    }
    
    void setListenerToView(View v) {
        v.findViewById(R.id.detail_btn).setOnClickListener(this);
        v.findViewById(R.id.poi_btn).setOnClickListener(this);
        v.findViewById(R.id.traffic_btn).setOnClickListener(this);
    }
    
    /**
     * ViewPager
     */
    ViewPager mViewPager = null;
    
    /**
     * PagerAdapter for ViewPager.<br />
     * 他会循环使用3个View提供给ViewPager
     */
    protected CycleViewPager.CyclePagerAdapter mCyclePagerAdapter;
    
    protected CycleViewPager.CycleOnPageChangeListener mCycleOnPageChangeListener;

    @Override
    public void refreshViews(int position) {
        MapView mapView = mSphinx.getMapView();
        if (position < mPostion) {
            mActionLog.addAction(ActionLog.MapStepUp);
            mItemizedOverlay.switchItem(false);
            ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(mSphinx);
            mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
        } else if (position > mPostion) {
            mActionLog.addAction(ActionLog.MapStepDown);
            mItemizedOverlay.switchItem(true);
            ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(mSphinx);
            mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
        }
        mPostion = position;
        if (mType == TYPE_POI) {
            
            mHeight = mTotalHeight - mHotelHeight - mTuangouHeight - mTitleHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setPOIToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setPOIToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setPOIToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            }
        } else if (mType == TYPE_MY_LOCATION) {

            mHeight = mTotalHeight - mPOIHeight - mHotelHeight - mTuangouHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setMyLocationToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setMyLocationToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setMyLocationToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            }
        } else if (mType == TYPE_MESSAGE
                || mType == TYPE_MY_LOCATION
                || mType == TYPE_LONG_CLIKED_SELECT_POINT) {

            mHeight = mTotalHeight - mPOIHeight - mHotelHeight - mTuangouHeight - mTitleHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setMessageToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject(), true);
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setMessageToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject(), true);
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setMessageToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject(), true);
            }
        } else if (mType == TYPE_HOTEL) {

            mHeight = mTotalHeight - mPOIHeight - mTuangouHeight - mTitleHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setHotelToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setHotelToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setHotelToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            }
        } else if (mType == TYPE_TUANGUO) {

            mHeight = mTotalHeight - mPOIHeight - mHotelHeight - mTuangouHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setTuangouToView(view, (Tuangou) mItemizedOverlay.get(position).getAssociatedObject());
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setTuangouToView(view, (Tuangou) mItemizedOverlay.get(position).getAssociatedObject());
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setTuangouToView(view, (Tuangou) mItemizedOverlay.get(position).getAssociatedObject());
            }
        } else if (mType == TYPE_DYNAMIC_POI) {

            mHeight = mTotalHeight - mHotelHeight - mTuangouHeight - mTitleHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setDynamicPOIToView(view, mItemizedOverlay.get(position).getAssociatedObject());
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setDynamicPOIToView(view, mItemizedOverlay.get(position).getAssociatedObject());
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setDynamicPOIToView(view, mItemizedOverlay.get(position).getAssociatedObject());
            }
        } else if (mType == TYPE_CLICKED_SELECT_POINT) {

            mHeight = mTotalHeight - mPOIHeight - mHotelHeight - mTuangouHeight - mBottomHeight - mTitleHeight;
            mSphinx.setMapViewPaddingBottom(mHeight);
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setMessageToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject(), false);
            
            if (position - 1 >= 0) {
                view = mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
                setMessageToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject(), false);
            }
            if (position + 1 < mCyclePagerAdapter.count) {
                view = mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
                setMessageToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject(), false);
            }
        }
        mViewPager.getLayoutParams().height = mHeight;
    }
    
    private void layoutInfoWindowView() {
        for(int i = mCyclePagerAdapter.viewList.size()-1; i >= 0; i--) {
            View v = mCyclePagerAdapter.viewList.get(i);
            if (mType == TYPE_POI) {
                v.findViewById(R.id.poi_view).setVisibility(View.VISIBLE);
                v.findViewById(R.id.hotel_view).setVisibility(View.GONE);
                v.findViewById(R.id.tuangou_view).setVisibility(View.GONE);
            } else if (mType == TYPE_HOTEL) {
                v.findViewById(R.id.poi_view).setVisibility(View.GONE);
                v.findViewById(R.id.hotel_view).setVisibility(View.VISIBLE);
                v.findViewById(R.id.tuangou_view).setVisibility(View.GONE);
            } else if (mType == TYPE_TUANGUO) {
                v.findViewById(R.id.poi_view).setVisibility(View.GONE);
                v.findViewById(R.id.hotel_view).setVisibility(View.GONE);
                v.findViewById(R.id.tuangou_view).setVisibility(View.VISIBLE);
            } else {
                v.findViewById(R.id.poi_view).setVisibility(View.GONE);
                v.findViewById(R.id.hotel_view).setVisibility(View.GONE);
                v.findViewById(R.id.tuangou_view).setVisibility(View.GONE);
            }
            v.findViewById(R.id.title_txv).setVisibility(View.GONE);
            v.findViewById(R.id.bottom_view).setVisibility(View.VISIBLE);
        }
    }
    
    private void setPOIToView(View v, POI poi) {
        TextView nameTxv=(TextView)v.findViewById(R.id.name_txv);
        RatingBar starsRtb = (RatingBar) v.findViewById(R.id.poi_stars_rtb);
        TextView priceTxv= (TextView)v.findViewById(R.id.poi_price_txv);
        priceTxv.setVisibility(View.VISIBLE);
        
        nameTxv.setText(poi.getName());
        starsRtb.setRating(poi.getGrade()/2.0f);
        long money = poi.getPerCapity();
        if (money > -1) {
            priceTxv.setText(getString(R.string.yuan, money));
        } else {
            priceTxv.setText("");
        }
    }
    
    private void setMyLocationToView(View v, POI poi) {
        
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(getString(R.string.my_location_with_accuracy, Utility.formatMeterString((int)poi.getPosition().getAccuracy())));
        
        setMessageToView(v, poi, true);
    }
    
    private void setMessageToView(View v, POI poi, boolean showBottomView) {
        
        TextView nameTxv=(TextView)v.findViewById(R.id.name_txv);
        nameTxv.setText(poi.getName());
        
        if (showBottomView) {
            v.findViewById(R.id.bottom_view).setVisibility(View.VISIBLE);
        } else {
            v.findViewById(R.id.bottom_view).setVisibility(View.GONE);
        }
    }
    
    private void setHotelToView(View v, POI poi) {

        TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
        TextView canReserveTxv = (TextView) v.findViewById(R.id.hotel_can_reserve_txv);
        TextView priceTxv = (TextView) v.findViewById(R.id.hotel_price_txv);

        Hotel hotel = poi.getHotel();
        nameTxv.setText(poi.getName());
        priceTxv.setText(poi.getPrice());

        if (hotel.getCanReserve() > 0) {
            canReserveTxv.setVisibility(View.GONE);
        } else {
            canReserveTxv.setVisibility(View.VISIBLE);
        }
    }
    
    private void setTuangouToView(View v, Tuangou tuangou) {
        String price = tuangou.getPrice();

        TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
        TextView priceTxv = (TextView) v.findViewById(R.id.tuangou_price_txv);

        nameTxv.setText(tuangou.getShortDesc());
        priceTxv.setText(price);
    }
    
    private void setDynamicPOIToView(View v, Object object) {
        long rating;
        String name;
        if (object instanceof Zhanlan) {
            Zhanlan target = (Zhanlan) object;
            name = target.getName();
            rating = target.getHot();
        } else {
            Yanchu target = (Yanchu) object;
            name = target.getName();
            rating = target.getHot();
        }

        TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
        RatingBar starsRtb = (RatingBar) v.findViewById(R.id.poi_stars_rtb);
        TextView priceTxv= (TextView)v.findViewById(R.id.poi_price_txv);
        priceTxv.setVisibility(View.GONE);

        nameTxv.setText(name);
        starsRtb.setProgress((int) rating);
    }
    
    private void infoWindowClicked() {
        mActionLog.addAction(ActionLog.MapInfoWindowBody);
        TouchMode touchMode = mSphinx.getTouchMode();
        
        if (mItemizedOverlay == null) {
            return;
        }
        
        OverlayItem overlayItem=mItemizedOverlay.getItemByFocused();
        if (overlayItem == null) {
            return;
        }
        
        String overlayName = overlayItem.getOwnerOverlay().getName();
        if(mType == TYPE_CLICKED_SELECT_POINT) {
            
            POI poi = (POI) overlayItem.getAssociatedObject();
            mSphinx.getFragment(mOwerFragmentId).dismiss();
            mSphinx.getPOIQueryFragment().onConfirmed(poi);
            
        } else if(touchMode.equals(TouchMode.LONG_CLICK)){
            POI poi = (POI) overlayItem.getAssociatedObject();
            if (mSphinx.uiStackContains(R.id.view_poi_detail)) {
                mSphinx.dismissView(R.id.view_result_map);
            } else {
                mSphinx.showView(R.id.view_poi_detail);
            }
            mSphinx.getPOIDetailFragment().setData(poi);
        } else if (overlayItem != null) {
            if (overlayName.equals(ItemizedOverlay.POI_OVERLAY)) {
                Object object = overlayItem.getAssociatedObject();
                if (object instanceof POI) {
                    POI target = (POI) object;
                    int sourceType = target.getSourceType();
                    if (sourceType == POI.SOURCE_TYPE_FENDIAN
                            || sourceType == POI.SOURCE_TYPE_YINGXUN
                            || sourceType == POI.SOURCE_TYPE_ZHANLAN
                            || sourceType == POI.SOURCE_TYPE_YANCHU
                            || sourceType == POI.SOURCE_TYPE_DIANYING
                            || sourceType == POI.SOURCE_TYPE_TUANGOU) {
                        mSphinx.dismissView(R.id.view_result_map);
                    } else {
                        if (mSphinx.uiStackContains(R.id.view_poi_detail)) {
                            mSphinx.dismissView(R.id.view_result_map);
                        } else {
                            mSphinx.showView(R.id.view_poi_detail);
                        }
                        mSphinx.getPOIDetailFragment().setData(target);
                    }
                } else if (object instanceof Tuangou) {
                    Tuangou target = (Tuangou) object;
                    if (mSphinx.uiStackContains(R.id.view_discover_tuangou_detail)) {
                        mSphinx.dismissView(R.id.view_result_map);
                    } else {
                        mSphinx.showView(R.id.view_discover_tuangou_detail);
                    }
                    mSphinx.getTuangouDetailFragment().setData(target);
                } else if (object instanceof Dianying) {
                    Dianying target = (Dianying) object;
                    if (mSphinx.uiStackContains(R.id.view_discover_dianying_detail)) {
                        mSphinx.dismissView(R.id.view_result_map);
                    } else {
                        mSphinx.showView(R.id.view_discover_dianying_detail);
                    }
                    mSphinx.getDianyingDetailFragment().setData(target);
                } else if (object instanceof Zhanlan) {
                    Zhanlan target = (Zhanlan) object;
                    if (mSphinx.uiStackContains(R.id.view_discover_zhanlan_detail)) {
                        mSphinx.dismissView(R.id.view_result_map);
                    } else {
                        mSphinx.showView(R.id.view_discover_zhanlan_detail);
                    }
                    mSphinx.getZhanlanDetailFragment().setData(target);
                } else if (object instanceof Yanchu) {
                    Yanchu target = (Yanchu) object;
                    if (mSphinx.uiStackContains(R.id.view_discover_yanchu_detail)) {
                        mSphinx.dismissView(R.id.view_result_map);
                    } else {
                        mSphinx.showView(R.id.view_discover_yanchu_detail);
                    }
                    mSphinx.getYanchuDetailFragment().setData(target);
                }
            } else if (overlayName.equals(ItemizedOverlay.TRAFFIC_OVERLAY)) {
                if (mSphinx.uiStackContains(R.id.view_traffic_result_detail)) {
                    mSphinx.dismissView(R.id.view_result_map);
                } else {
                    mSphinx.showView(R.id.view_traffic_result_detail);
                }
            } else if (overlayName.equals(ItemizedOverlay.BUSLINE_OVERLAY)) {
                if (mSphinx.uiStackContains(R.id.view_traffic_busline_detail)) {
                    mSphinx.dismissView(R.id.view_result_map);
                } else {
                    mSphinx.showView(R.id.view_traffic_busline_detail);
                }
            }
        }
        mSphinx.setTouchMode(TouchMode.NORMAL);
    }

    public ItemizedOverlay getItemizedOverlay() {
        return mItemizedOverlay;
    }
    
    public int getType() {
        return mType;
    }
}

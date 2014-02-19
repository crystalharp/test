/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import com.decarta.android.exception.APIException;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.GeoCoderResponse;
import com.tigerknows.model.DataQuery.GeoCoderResponse.GeoCoderList;
import com.tigerknows.ui.discover.CycleViewPager;
import com.tigerknows.ui.discover.CycleViewPager.CycleOnPageChangeListener;
import com.tigerknows.ui.discover.CycleViewPager.CyclePagerAdapter;
import com.tigerknows.ui.traffic.TrafficDetailFragment;
import com.tigerknows.ui.traffic.TrafficDetailFragment.PlanItemRefresher;
import com.tigerknows.util.Utility;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

    public static final int TYPE_LONG_CLICKED_SELECT_POINT = 7;

    public static final int TYPE_MAP_POI = 8;

    public static final int TYPE_PLAN_LIST = 9;
    
    public static final int TYPE_STEP = 10;

    /**
     * 下面这些ViewGroup用于InfoWindow
     * 最初是想避免重新创建它们，但是重用时会出现文字不对齐、高宽计算不协调情况？
     */
    private List<View> mInfoWindow = null;
    
    private ItemizedOverlay mItemizedOverlay;
    private int mOwerFragmentId;
    private int mType;
    private int mPosition;

    private int mTitleHeight;
    private int mPOIHeight;
    private int mHotelHeight;
    private int mTuangouHeight;
    private int mBottomHeight;
    private int mTrafficPlanHeight;
    private int mTrafficStepHeight;
    
    private final static String TAG = "InfoWindowFragment";
    
    public InfoWindowFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
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
            mItemizedOverlay.focuseOverlayItem(0);
            overlayItem = mItemizedOverlay.getItemByFocused();
        }
        
        int id = v.getId();
        if (id == R.id.poi_btn) {
            Object object = overlayItem.getAssociatedObject();
            POI poi = null;
            if (object instanceof POI) {
                poi = (POI) object;
            } else if (object instanceof Tuangou) {
                poi = ((Tuangou) object).getPOI();
            } else if (object instanceof Dianying) {
                poi = ((Dianying) object).getPOI();
            } else if (object instanceof Yanchu) {
                poi = ((Yanchu) object).getPOI();
            } else if (object instanceof Zhanlan) {
                poi = ((Zhanlan) object).getPOI();
            }
            mActionLog.addAction(mActionTag + ActionLog.InfoWindowPOI);
            mSphinx.getPOINearbyFragment().setData(mSphinx.buildDataQuery(poi));
            mSphinx.showView(R.id.view_poi_nearby_search);
        } else if (id == R.id.traffic_btn) {
            mActionLog.addAction(mActionTag + ActionLog.InfoWindowTraffic);
            if (mType == TYPE_MY_LOCATION) {
                mSphinx.showView(R.id.view_traffic_home);
            } else {
                Object object = overlayItem.getAssociatedObject();
                POI poi = null;
                if (object instanceof POI) {
                    poi = (POI) object;
                } else if (object instanceof Tuangou) {
                    poi = ((Tuangou) object).getPOI();
                } else if (object instanceof Dianying) {
                    poi = ((Dianying) object).getPOI();
                } else if (object instanceof Yanchu) {
                    poi = ((Yanchu) object).getPOI();
                } else if (object instanceof Zhanlan) {
                    poi = ((Zhanlan) object).getPOI();
                }
                Utility.queryTraffic(mSphinx, poi , mActionTag);
            }
        } else if (id == R.id.detail_btn) {
            mActionLog.addAction(mActionTag + ActionLog.InfoWindowDetail);
            infoWindowClicked();
        } else if (id == R.id.traffic_plan_item) {
            mActionLog.addAction(mActionTag + ActionLog.InfoWindowDetail);
            Plan plan = (Plan) v.getTag();
            if (plan == null) {
                return;
            }
            TrafficDetailFragment f = mSphinx.getTrafficDetailFragment();
            if (plan.getType() == Step.TYPE_DRIVE ) {
                f.refreshDrive(plan);
                mSphinx.showView(R.id.view_traffic_result_detail);
            } else if (plan.getType() == Step.TYPE_WALK) {
                f.refreshResult(plan.getType());
                mSphinx.showView(R.id.view_traffic_result_detail);
            } else if (plan.getType() == Step.TYPE_TRANSFER) {
                mSphinx.getFragment(this.getOwerFragmentId()).dismiss();
            }
        }
        
    }
    
    public void setData(int fragmentId, ItemizedOverlay itemizedOverlay, String actionTag) {
        mOwerFragmentId = fragmentId;
        mActionTag = actionTag;
        mCycleOnPageChangeListener.setActionTag(actionTag);
        
        OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
        
        if (overlayItem == null) {
            itemizedOverlay.focuseOverlayItem(0);
            overlayItem = itemizedOverlay.getItemByFocused();
        }
        
        mItemizedOverlay = itemizedOverlay;
        mPosition = mItemizedOverlay.getPositionByFocused();
        
        Object object = overlayItem.getAssociatedObject();
        
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
                mType = TYPE_LONG_CLICKED_SELECT_POINT;
            } else if (sourceType == POI.SOURCE_TYPE_MAP_POI) {
                mType = TYPE_MAP_POI;
            } else {
                mType = TYPE_POI;
            }
            
        } else if (object instanceof Zhanlan || object instanceof Yanchu || object instanceof Dianying) {
            mType = TYPE_DYNAMIC_POI;
        } else if (object instanceof Tuangou) {
            mType = TYPE_TUANGUO;
        } else if (object instanceof Plan) {
            mType = TYPE_PLAN_LIST;
        } else if (object instanceof Step) {
            mType = TYPE_STEP;
        } else {
            mType = TYPE_MESSAGE;
        }
        
        LogWrapper.d(TAG, "mType = " + mType);

        List<View> list = getInfoWindowViewList();
        mCyclePagerAdapter.viewList = list;
        mCyclePagerAdapter.count = itemizedOverlay.size();
        mCyclePagerAdapter.notifyDataSetChanged();
        mCycleOnPageChangeListener.count = mCyclePagerAdapter.count;
        layoutInfoWindowView();
        refreshViews(mPosition);
        mViewPager.setCurrentItem(mPosition, false);

    }
    
    public int getOwerFragmentId() {
        return mOwerFragmentId;
    }
    
    private List<View> getInfoWindowViewList() {
        if (mInfoWindow == null) {
            List<View> viewList = new ArrayList<View>();
            View view;
            view = mLayoutInflater.inflate(R.layout.info_window_fragment, this, false);
            setListenerToView(view);
            viewList.add(view);
            
            View v = view.findViewById(R.id.message_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTitleHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.poi_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mPOIHeight = v.getMeasuredHeight()-Utility.dip2px(mSphinx, 16);
            v = view.findViewById(R.id.tuangou_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTuangouHeight = v.getMeasuredHeight()-Utility.dip2px(mSphinx, 16);
            v = view.findViewById(R.id.hotel_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mHotelHeight = v.getMeasuredHeight()-Utility.dip2px(mSphinx, 16);
            v = view.findViewById(R.id.bottom_view);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mBottomHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.traffic_plan_item);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTrafficPlanHeight = v.getMeasuredHeight();
            v = view.findViewById(R.id.traffic_step_item);
            v.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mTrafficStepHeight = v.getMeasuredHeight();
            LogWrapper.d(TAG, "step height:" + mTrafficStepHeight);
            
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
        v.findViewById(R.id.traffic_plan_item).setOnClickListener(this);
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
        if (position < mPosition) {
            mActionLog.addAction(mActionTag, ActionLog.InfoWindowFlingLeft);
            mItemizedOverlay.switchItem(false);
            ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(mSphinx);
            mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
        } else if (position > mPosition) {
            mActionLog.addAction(mActionTag, ActionLog.InfoWindowFlingRight);
            mItemizedOverlay.switchItem(true);
            ItemizedOverlayHelper.centerShowCurrentOverlayFocusedItem(mSphinx);
            mapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
        }
        mPosition = position;
        mHeight = mTitleHeight + mBottomHeight;
        int prevPosition = -1;
        int nextPosition = -1;
        if (position - 1 >= 0) {
            prevPosition = position-1;
        }
        if (position + 1 < mCyclePagerAdapter.count) {
            nextPosition = position+1;
        }
        if (prevPosition != -1 || nextPosition != -1) {
            mSphinx.resetLoactionButtonState();
        }
        if (mType == TYPE_POI) {
            
            mHeight += mPOIHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setPOIToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setPOIToView(view, (POI) mItemizedOverlay.get(prevPosition).getAssociatedObject());
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setPOIToView(view, (POI) mItemizedOverlay.get(nextPosition).getAssociatedObject());
            }
        } else if (mType == TYPE_MY_LOCATION) {

            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setMyLocationToView(view, mItemizedOverlay.get(position));
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setMyLocationToView(view, mItemizedOverlay.get(prevPosition));
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setMyLocationToView(view, mItemizedOverlay.get(nextPosition));
            }
        } else if (mType == TYPE_MESSAGE) {

            mHeight -= mBottomHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setMessageToView(view, mItemizedOverlay.get(position).getMessage(), null, false);
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setMessageToView(view, mItemizedOverlay.get(prevPosition).getMessage(), null, false);
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setMessageToView(view, mItemizedOverlay.get(nextPosition).getMessage(), null, false);
            }
        } else if (mType == TYPE_LONG_CLICKED_SELECT_POINT) {

            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            String name = ((POI) mItemizedOverlay.get(position).getAssociatedObject()).getName();
            String selectPoint = getString(R.string.select_point);
            if (selectPoint.equals(name) == false) {
            	name += getString(R.string.nearby);
            }
            setMessageToView(view, name, null, true);
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                name = ((POI) mItemizedOverlay.get(prevPosition).getAssociatedObject()).getName();
                if (selectPoint.equals(name) == false) {
                	name += getString(R.string.nearby);
                }
                setMessageToView(view, name, null, true);
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                name = ((POI) mItemizedOverlay.get(nextPosition).getAssociatedObject()).getName();
                if (selectPoint.equals(name) == false) {
                	name += getString(R.string.nearby);
                }
                setMessageToView(view, name, null, true);
            }
        } else if (mType == TYPE_MAP_POI) {
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            POI poi = (POI) mItemizedOverlay.get(position).getAssociatedObject();
            String name = poi.getName();
            setMessageToView(view, poi.getName(), poi.getUUID() != null ? getString(R.string.detail) : null, true);
            if (poi.getFrom() == POI.FROM_LOCAL && name != null) {
                DataQuery dataQuery = new DataQuery(mSphinx);
                dataQuery.setup(getId(), getId(), null, false, false, poi);
                dataQuery.addParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_GEOCODER);
                dataQuery.addParameter(BaseQuery.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, name);
                Position pos = poi.getPosition();
                dataQuery.addParameter(BaseQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(pos.getLon()));
                dataQuery.addParameter(BaseQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(pos.getLat()));
                dataQuery.setCityId(MapEngine.getCityId(poi.getPosition()));
                mSphinx.queryStart(dataQuery);
            }
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                poi = (POI) mItemizedOverlay.get(prevPosition).getAssociatedObject();
                setMessageToView(view, poi.getName(), poi.getUUID() != null ? getString(R.string.detail) : null, true);
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                poi = (POI) mItemizedOverlay.get(nextPosition).getAssociatedObject();
                setMessageToView(view, poi.getName(), poi.getUUID() != null ? getString(R.string.detail) : null, true);
            }
        } else if (mType == TYPE_HOTEL) {

            mHeight += mHotelHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setHotelToView(view, (POI) mItemizedOverlay.get(position).getAssociatedObject());
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setHotelToView(view, (POI) mItemizedOverlay.get(prevPosition).getAssociatedObject());
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setHotelToView(view, (POI) mItemizedOverlay.get(nextPosition).getAssociatedObject());
            }
        } else if (mType == TYPE_TUANGUO) {

            mHeight += mTuangouHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            Object object = mItemizedOverlay.get(position).getAssociatedObject();
            if (object instanceof Tuangou) {
                setTuangouToView(view, (Tuangou) object);
            } else {
                setPOIToView(view, (POI) object);
            }
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                object = mItemizedOverlay.get(prevPosition).getAssociatedObject();
                if (object instanceof Tuangou) {
                    setTuangouToView(view, (Tuangou) object);
                } else {
                    setPOIToView(view, (POI) object);
                }
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                object = mItemizedOverlay.get(nextPosition).getAssociatedObject();
                if (object instanceof Tuangou) {
                    setTuangouToView(view, (Tuangou) object);
                } else {
                    setPOIToView(view, (POI) object);
                }
            }
        } else if (mType == TYPE_DYNAMIC_POI) {

            mHeight += mPOIHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setDynamicPOIToView(view, mItemizedOverlay.get(position).getAssociatedObject());
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setDynamicPOIToView(view, mItemizedOverlay.get(prevPosition).getAssociatedObject());
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setDynamicPOIToView(view, mItemizedOverlay.get(nextPosition).getAssociatedObject());
            }
        } else if (mType == TYPE_CLICKED_SELECT_POINT) {

            mHeight -= mBottomHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setClickSelectPointToView(view, mItemizedOverlay.get(position));
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setClickSelectPointToView(view, mItemizedOverlay.get(prevPosition));
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setClickSelectPointToView(view, mItemizedOverlay.get(nextPosition));
            }
        } else if (mType == TYPE_PLAN_LIST) {

            mHeight = mTrafficPlanHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            Plan plan = (Plan) mItemizedOverlay.get(position).getAssociatedObject();
            if (plan.getType() == Step.TYPE_TRANSFER) {
                TrafficDetailFragment f = mSphinx.getTrafficDetailFragment();
                if (f.getResult(Step.TYPE_TRANSFER).size() != 0) {
                    f.refreshResult(Step.TYPE_TRANSFER, f.getResult(Step.TYPE_TRANSFER).indexOf(plan));
                }
            }
            setPlanListToView(view, plan);
            
            TrafficOverlayHelper.drawOverlay(mSphinx, plan);
            TrafficOverlayHelper.panToViewWholeOverlay(plan, mapView, mSphinx);
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setPlanListToView(view, (Plan) mItemizedOverlay.get(prevPosition).getAssociatedObject());
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setPlanListToView(view, (Plan) mItemizedOverlay.get(nextPosition).getAssociatedObject());
            }
        } else if (mType == TYPE_STEP) {
            mHeight = mTrafficStepHeight;
            
            View view = (View) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
            setStepToView(view, position);
            
            if (prevPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(prevPosition % mCyclePagerAdapter.viewList.size());
                setStepToView(view, prevPosition);
            }
            if (nextPosition != -1) {
                view = mCyclePagerAdapter.viewList.get(nextPosition % mCyclePagerAdapter.viewList.size());
                setStepToView(view, nextPosition);
            }
        }
        if (mSphinx.getBottomFragment() == this) {
            mSphinx.setMapViewPaddingBottom(mHeight);
        }
        mViewPager.getLayoutParams().height = mHeight;
    }
    
    private void layoutInfoWindowView() {
        for(int i = mCyclePagerAdapter.viewList.size()-1; i >= 0; i--) {
            View v = mCyclePagerAdapter.viewList.get(i);
            v.findViewById(R.id.poi_view).setVisibility(View.GONE);
            v.findViewById(R.id.hotel_view).setVisibility(View.GONE);
            v.findViewById(R.id.tuangou_view).setVisibility(View.GONE);
            v.findViewById(R.id.title_txv).setVisibility(View.GONE);
            v.findViewById(R.id.traffic_plan_item).setVisibility(View.GONE);
            v.findViewById(R.id.traffic_step_item).setVisibility(View.GONE);
            v.findViewById(R.id.detail_btn).setVisibility(View.VISIBLE);
            v.findViewById(R.id.bottom_view).setVisibility(View.VISIBLE);
            v.findViewById(R.id.message_view).setVisibility(View.VISIBLE);
            TextView titleTxv = (TextView)v.findViewById(R.id.title_txv);
            TextView nameTxv = (TextView)v.findViewById(R.id.name_txv);
            Resources resources = mSphinx.getResources();
            int blackDark = resources.getColor(R.color.black_dark);
            int blackLight = resources.getColor(R.color.black_light);
            titleTxv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            titleTxv.setTextColor(blackDark);
            nameTxv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            nameTxv.setTextColor(blackLight);
            if (mType == TYPE_POI || mType == TYPE_DYNAMIC_POI) {
                v.findViewById(R.id.poi_view).setVisibility(View.VISIBLE);
            } else if (mType == TYPE_HOTEL) {
                v.findViewById(R.id.hotel_view).setVisibility(View.VISIBLE);
            } else if (mType == TYPE_TUANGUO) {
                v.findViewById(R.id.tuangou_view).setVisibility(View.VISIBLE);
            } else if (mType == TYPE_PLAN_LIST) {
                v.findViewById(R.id.traffic_plan_item).setVisibility(View.VISIBLE);
                v.findViewById(R.id.message_view).setVisibility(View.GONE);
            } else if (mType == TYPE_STEP) {
                v.findViewById(R.id.traffic_step_item).setVisibility(View.VISIBLE);
                v.findViewById(R.id.message_view).setVisibility(View.GONE);
            } else if (mType == TYPE_CLICKED_SELECT_POINT) {
                titleTxv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                titleTxv.setTextColor(blackLight);
                nameTxv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                nameTxv.setTextColor(blackDark);
            }
        }
    }
    
    private void setPOIToView(View v, POI poi) {
        TextView nameTxv=(TextView)v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.GONE);
        
        RatingBar starsRtb = (RatingBar) v.findViewById(R.id.poi_stars_rtb);
        TextView priceTxv= (TextView)v.findViewById(R.id.poi_price_txv);
        Button detaileBtn= (Button)v.findViewById(R.id.detail_btn);
        priceTxv.setVisibility(View.VISIBLE);
        
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(poi.getName());
        
        starsRtb.setRating(poi.getGrade()/2.0f);
        long money = poi.getPerCapity();
        if (money > -1) {
            priceTxv.setText(getString(R.string.yuan, money));
        } else {
            priceTxv.setText("");
        }
        
        if (poi.getUUID() != null) {
            detaileBtn.setText(R.string.detail);
            detaileBtn.setVisibility(View.VISIBLE);
        } else {
            detaileBtn.setVisibility(View.INVISIBLE);
        }
    }
    
    private void setPlanListToView(View v, Plan plan) {
        
        PlanItemRefresher.refresh(mSphinx, plan, v.findViewById(R.id.traffic_plan_item), true);

        v.findViewById(R.id.detail_btn).setVisibility(View.GONE);
        v.findViewById(R.id.bottom_view).setVisibility(View.GONE);
    }
    
    private void setStepToView(View v, int position) {
        ImageView img = (ImageView) v.findViewById(R.id.step_icon);
        TextView txv = (TextView) v.findViewById(R.id.step_txv);
        img.setBackgroundDrawable(mSphinx.getTrafficDetailFragment().getStepIcon(position));
        txv.setText(mSphinx.getTrafficDetailFragment().getStepDescription(position));
    }
    
    private void setClickSelectPointToView(View v, OverlayItem overlayItem) {
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(overlayItem.getMessage());
        
        TextView nameTxv=(TextView)v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.VISIBLE);
        nameTxv.setText(((POI) overlayItem.getAssociatedObject()).getName());
        
        Button detailBtn = (Button) v.findViewById(R.id.detail_btn);
        detailBtn.setText(R.string.confirm);
        detailBtn.setVisibility(View.VISIBLE);
        
        v.findViewById(R.id.bottom_view).setVisibility(View.GONE);
    }
    
    private void setMyLocationToView(View v, OverlayItem overlayItem) {
        
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(getString(R.string.my_location_with_accuracy, Utility.formatMeterString((int)((POI) overlayItem.getAssociatedObject()).getPosition().getAccuracy())));
        
        TextView nameTxv=(TextView)v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.VISIBLE);
        nameTxv.setText(overlayItem.getMessage());
        
        Button detailBtn = (Button) v.findViewById(R.id.detail_btn);
        detailBtn.setVisibility(View.GONE);
    
        v.findViewById(R.id.bottom_view).setVisibility(View.VISIBLE);
    }
    
    private void setMessageToView(View v, String name, String detailBtnText, boolean showBottomView) {
        
        TextView nameTxv=(TextView)v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.GONE);
        
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(name);
        
        Button detailBtn = (Button) v.findViewById(R.id.detail_btn);
        if (detailBtnText != null) {
            detailBtn.setText(detailBtnText);
            detailBtn.setVisibility(View.VISIBLE);
        } else {
            detailBtn.setVisibility(View.INVISIBLE);
        }
        
        if (showBottomView) {
            v.findViewById(R.id.bottom_view).setVisibility(View.VISIBLE);
        } else {
            v.findViewById(R.id.bottom_view).setVisibility(View.GONE);
        }
    }
    
    private void setHotelToView(View v, POI poi) {

        TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.GONE);
        TextView priceTxv = (TextView) v.findViewById(R.id.hotel_price_txv);
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(poi.getName());
        priceTxv.setText(poi.getPrice());
        Button detailBtn = (Button) v.findViewById(R.id.detail_btn);
        detailBtn.setText(R.string.detail);
        detailBtn.setVisibility(View.VISIBLE);
    }
    
    private void setTuangouToView(View v, Tuangou tuangou) {
        String price = tuangou.getPrice();

        TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.GONE);
        TextView priceTxv = (TextView) v.findViewById(R.id.tuangou_price_txv);
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setVisibility(View.VISIBLE);
        titleTxv.setText(tuangou.getShortDesc());
        if (price != null) {
            priceTxv.setText(price+getString(R.string.rmb_text));
        } else {
            priceTxv.setText("");
        }
        Button detailBtn = (Button) v.findViewById(R.id.detail_btn);
        detailBtn.setText(R.string.detail);
        detailBtn.setVisibility(View.VISIBLE);
    }
    
    private void setDynamicPOIToView(View v, Object object) {
        long rating;
        String name;
        if (object instanceof Zhanlan) {
            Zhanlan target = (Zhanlan) object;
            name = target.getName();
            rating = target.getHot();
        } else if (object instanceof Yanchu) {
            Yanchu target = (Yanchu) object;
            name = target.getName();
            rating = target.getHot();
        } else {
            Dianying target = (Dianying) object;
            name = target.getName();
            rating = target.getRank();
        }

        TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
        nameTxv.setVisibility(View.GONE);
        RatingBar starsRtb = (RatingBar) v.findViewById(R.id.poi_stars_rtb);
        TextView priceTxv= (TextView)v.findViewById(R.id.poi_price_txv);
        priceTxv.setVisibility(View.GONE);
        TextView titleTxv = (TextView) v.findViewById(R.id.title_txv);
        titleTxv.setText(name);
        titleTxv.setVisibility(View.VISIBLE);
        starsRtb.setProgress((int) rating);
        Button detailBtn = (Button) v.findViewById(R.id.detail_btn);
        detailBtn.setText(R.string.detail);
        detailBtn.setVisibility(View.VISIBLE);
    }
    
    private void infoWindowClicked() {
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
            mSphinx.getInputSearchFragment().responsePOI(poi);
            
        } else if(mType == TYPE_LONG_CLICKED_SELECT_POINT
                || mType == TYPE_MY_LOCATION) {
            
            POI poi = (POI) overlayItem.getAssociatedObject();
            if (mSphinx.uiStackContains(R.id.view_poi_detail)) {
                mSphinx.dismissView(R.id.view_result_map);
            } else {
                mSphinx.showView(R.id.view_poi_detail);
            }
            mSphinx.getPOIDetailFragment().setData(poi);
        } else if(mType == TYPE_MAP_POI) {
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
            } else if (overlayName.equals(ItemizedOverlay.TRAFFIC_PLAN_LIST_OVERLAY)) {
                if (mSphinx.uiStackContains(R.id.view_traffic_result_detail)) {
                    mSphinx.dismissView(R.id.view_result_map);
                } else {
                    mSphinx.showView(R.id.view_traffic_result_detail);
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

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        }
            
        if (baseQuery instanceof DataQuery) {
            DataQuery dataQuery = (DataQuery) baseQuery;
            Response response = dataQuery.getResponse();
            if (response != null
                    && response instanceof GeoCoderResponse
                    && response.getResponseCode() == Response.RESPONSE_CODE_OK) {
                GeoCoderResponse geoCoderResponse = (GeoCoderResponse) response;
                GeoCoderList geoCoderList = geoCoderResponse.getList();
                if (geoCoderList != null) {
                    List<POI> list = geoCoderList.getPOIList();
                    if (list != null && list.size() > 0) {
                        POI poi = dataQuery.getPOI();
                        try {
                            Position position = poi.getPosition();
                            poi.init(list.get(0).getData(), true);
                            poi.setPosition(position);
                            poi.setFrom(POI.FROM_ONLINE);
                            refreshViews(mPosition);
                        } catch (APIException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}

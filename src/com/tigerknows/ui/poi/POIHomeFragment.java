/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class POIHomeFragment extends BaseFragment implements View.OnClickListener {

    public POIHomeFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "HomeFragment";

    private Button mCityBtn;
    private Button mInputBtn;
    private TextView mMyLoactionTxv;
    private Position mLastPosition;
    private String mLocationName;
    private long mLastTime = 0;
    private SpringbackListView mCategoryLsv;
    private CategoryAdapter mCategoryAdapter;
    private int mCategoryTop;
    private int mMyLocationViewHeight;
    private int mCategoryBtnPadding;
    private int mCategoryPadding = 0;

    private View mHeaderView;
    private View mFooterView;
    
    private String[] mCategoryNameList;
    private final int[] mCategoryResIdList = {R.drawable.category_eat,
        R.drawable.category_play,
        R.drawable.category_buy,
        R.drawable.category_hotel,
        R.drawable.category_tour,
        R.drawable.category_beauty,
        R.drawable.category_sport,
        R.drawable.category_bank,
        R.drawable.category_traffic,
        R.drawable.category_hospital
        };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIHome;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);        
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_home, container, false);
        
        findViews();
        setListener();
        
        mCategoryNameList = getResources().getStringArray(R.array.home_category);

        int screenWidth = Math.min(Globals.g_metrics.widthPixels, Globals.g_metrics.heightPixels);
        mCategoryBtnPadding = Util.dip2px(Globals.g_metrics.density, 2);
        
        View view = getImageButton();
        view.setBackgroundResource(mCategoryResIdList[0]);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int viewWidth = view.getMeasuredWidth()+2*mCategoryBtnPadding;

        int columnWidth = screenWidth / viewWidth;
        if (columnWidth > 2) {
            columnWidth = 2;
        }
        List<List<Category>> categoryList = new ArrayList<List<Category>>();
        for(int i = 0, length = mCategoryResIdList.length; (i*columnWidth) < length; i++) {
            int index = (i*columnWidth);
            List<Category> categorylist = new ArrayList<Category>();
            for(int j = 0; j < columnWidth && index+j < length; j++) {
                Category category = new Category();
                category.resId = mCategoryResIdList[index+j];
                category.name = mCategoryNameList[index+j];
                categorylist.add(category);
            }
            categoryList.add(categorylist);
        }
        
        mHeaderView = new LinearLayout(mContext);
        mFooterView = new LinearLayout(mContext);
        mCategoryLsv.addHeaderView(mHeaderView);
        mCategoryLsv.addFooterView(mFooterView);
        
        mCategoryAdapter = new CategoryAdapter(mContext, categoryList);
        mCategoryLsv.setAdapter(mCategoryAdapter);
        
        mMyLoactionTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mCategoryPadding = Util.dip2px(Globals.g_metrics.density, 8);
        mMyLocationViewHeight = mMyLoactionTxv.getMeasuredHeight();
        mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
        mCategoryAdapter.notifyDataSetChanged();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMenuFragment.updateMenuStatus(R.id.poi_btn);
        mTitleFragment.hide();
        mCityBtn.setText(Globals.g_Current_City_Info.getCName());
        
        mMenuFragment.display();

        refreshLocationView();
        // 将mCategoryLsv滚动到最顶
        mCategoryLsv.changeHeaderViewByState(true, SpringbackListView.DONE);
        mCategoryLsv.setSelectionFromTop(0, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mCategoryLsv = (SpringbackListView) mRootView.findViewById(R.id.category_lsv);
        mInputBtn = (Button) mRootView.findViewById(R.id.input_btn);
        mCityBtn = (Button) mRootView.findViewById(R.id.city_btn);
        mMyLoactionTxv = (TextView) mRootView.findViewById(R.id.my_location_txv);
    }

    protected void setListener() {
        mInputBtn.setOnClickListener(this);
        mCityBtn.setOnClickListener(this);
        mCategoryLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                mCategoryLsv.onRefreshComplete(isHeader);
            }
        });  
    }
        
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.city_btn) {
            mActionLog.addAction(mActionTag +  ActionLog.POIHomeChangeCityBtn, Globals.g_Current_City_Info.getCName());
            mSphinx.showView(R.id.activity_more_change_city);
        } else if (id == R.id.input_btn) {
            mActionLog.addAction(mActionTag +  ActionLog.POIHomeInputEdt);
            mSphinx.showView(R.id.view_poi_input_search);
        }
    }
    
    public void refreshCity(String cityName) {
        if (mTitleFragment == null) {
            return;
        }
        mCityBtn.setText(cityName);
    }
    
    public void refreshLocationView() {
        CityInfo currentCityInfo = Globals.g_Current_City_Info;
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        int categoryTop = mCategoryTop;
        if (myLocationCityInfo != null) {
            if (myLocationCityInfo.getId() == currentCityInfo.getId()) {
                refreshLoactionTxv();
                mMyLoactionTxv.setVisibility(View.VISIBLE);
                mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
            } else {
                mMyLoactionTxv.setVisibility(View.GONE);
                mCategoryTop = mCategoryPadding;
            }
        } else {
            mLastTime = 0;
            mMyLoactionTxv.setText(mContext.getString(R.string.location_doing));
            mMyLoactionTxv.setVisibility(View.VISIBLE);
            mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
        }
        if (categoryTop != mCategoryTop) {
            mCategoryAdapter.notifyDataSetChanged();
        }
        mSphinx.getDiscoverFragment().refreshLocationView(mLocationName, myLocationCityInfo);
    }
    
    private void refreshLoactionTxv() {
        Location myLocation = null;
        Position myLocationPosition = null;
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null) {
            myLocationPosition = myLocationCityInfo.getPosition();
            myLocation = Globals.g_My_Location;
        }
        long currentTime = System.currentTimeMillis();
        int distance = Position.distanceBetween(myLocationPosition, mLastPosition);

        String name = mSphinx.getMapEngine().getPositionName(myLocationPosition);
        
        boolean isUpdate = false;
        if (!TextUtils.isEmpty(name) && name.length() > 1) {
            if ((myLocation != null && myLocation.getProvider().equals(LocationManager.GPS_PROVIDER) && distance > 100)) {
                isUpdate = true;
            } else if (currentTime - mLastTime > 30*1000 && distance > 600) {
                isUpdate = true;
            } else if (mLastTime == 0) {
                isUpdate = true;
            } else if (!TextUtils.isEmpty(mLocationName) && mLocationName.startsWith("U") && name.startsWith("G")) {
                isUpdate = true;
            }
        }
        
        if (isUpdate) {
            mLocationName = name;
            mMyLoactionTxv.setText(mContext.getString(R.string.current_location, mLocationName.substring(1)));
            mLastPosition = myLocationPosition;
            mLastTime = currentTime;
        }
            
    }

    public ImageButton getImageButton() {
        ImageButton imageButton = new ImageButton(mContext);
        imageButton.setScaleType(ScaleType.CENTER);
        imageButton.setImageResource(R.drawable.btn_category);
        return imageButton;
    }
    
    private static class Category {
        int resId;
        String name;
    }
    
    public class CategoryAdapter extends ArrayAdapter<List<Category>> {

        public CategoryAdapter(Context context, List<List<Category>> list) {
            super(context, 0, list);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LinearLayout view;
            if (convertView == null) {
                int size = getItem(0).size();
                view = new LinearLayout(mContext);
                view.setOrientation(LinearLayout.HORIZONTAL);
                view.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams layoutParamsNormalRow = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                layoutParamsNormalRow.leftMargin = mCategoryBtnPadding;
                layoutParamsNormalRow.topMargin = mCategoryBtnPadding;
                layoutParamsNormalRow.rightMargin = mCategoryBtnPadding;
                layoutParamsNormalRow.gravity = Gravity.LEFT;
                for(int i = 0; i < size; i++) {
                    view.addView(getImageButton(), layoutParamsNormalRow);
                }
            } else {
                view = (LinearLayout)convertView;
            }

            final List<Category> categoryList = getItem(position);
            int categorySize = categoryList.size();
            int size = view.getChildCount();
            for(int i = 0; i < size; i++) {
                if (i < categorySize) {
                    final Category category = categoryList.get(i);
                    ImageButton categoryImb = (ImageButton)view.getChildAt(i);
                    categoryImb.setVisibility(View.VISIBLE);
                    categoryImb.setBackgroundResource(category.resId);
                    categoryImb.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            mActionLog.addAction(mActionTag +  ActionLog.POIHomeCategory, category.name);

                            DataQuery poiQuery = new DataQuery(mContext);
                            POI requestPOI = mSphinx.getPOI();
                            int cityId = Globals.g_Current_City_Info.getId();
                            Hashtable<String, String> criteria = new Hashtable<String, String>();
                            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
                            criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
                            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
                            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD, category.name);
                            Position position = requestPOI.getPosition();
                            if (position != null) {
                                criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                                criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
                            }
                            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD_TYPE, DataQuery.KEYWORD_TYPE_TAG);
                            poiQuery.setup(criteria, cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
                            BaseFragment baseFragment = mSphinx.getFragment(poiQuery.getTargetViewId());
                            if (baseFragment != null && baseFragment instanceof POIResultFragment) {
                                mSphinx.queryStart(poiQuery);
                                ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup();
                                mSphinx.showView(poiQuery.getTargetViewId());
                            }
                        }
                    });
                } else {
                    view.getChildAt(i).setVisibility(View.INVISIBLE);
                }
            }

            if (position == 0) {
                view.setPadding(0, mCategoryPadding, 0, 0);
            } else if (position == getCount()-1){
                view.setPadding(0, 0, 0, mCategoryTop);
            } else {
                view.setPadding(0, 0, 0, 0);
            }
            return view;
        }
    }
}

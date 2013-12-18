/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.FilterListView;

/**
 * 周边搜索界面
 * 在指定的POI周边进行搜索
 * @author Peng Wenyue
 */
public class NearbySearchFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack{
    
    public NearbySearchFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    /**
     * 在此POI周边进行搜索
     */
    private POI mPOI = new POI();
    
    private TextView mLocationTxv;
    
    /**
     * 分类名称列表
     */
    private String[] mCategoryNames;
    
    private Button[][] mCategoryBtns;
    
    private static final int[] CATEGORY_ID = {R.id.food_category,
    	R.id.hotel_category,
    	R.id.entertainment_category,
    	R.id.shopping_category,
    	R.id.travel_category,
    	R.id.traffic_category,
    	R.id.beauty_category,
    	R.id.bank_category,
    	R.id.sports_category,
    	R.id.hospital_category
    };
    
    private static final int NUM_OF_SUB_CATEGORY = 5;
    
    private View[] mCategoryViews;

    private FilterListView mFilterListView;
    private List<Filter> mFilterList;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POINearbySearch;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_nearby_search, container, false);
        Resources resources = mContext.getResources();
        mCategoryNames = resources.getStringArray(R.array.home_category);
        
        findViews();
        setListener();
        
        setFilterListView();
                
        mLocationTxv.setVisibility(View.VISIBLE);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setVisibility(View.GONE);
        String name = mPOI.getName();
        String title = mSphinx.getString(R.string.at_where_search, name);
        SpannableStringBuilder style = new SpannableStringBuilder(title);
        int focusedColor = mSphinx.getResources().getColor(R.color.black_dark);
        style.setSpan(new ForegroundColorSpan(focusedColor), 0, 2, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        style.setSpan(new ForegroundColorSpan(focusedColor), 2+name.length(), title.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        mLocationTxv.setText(style);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mLocationTxv = (TextView) mRootView.findViewById(R.id.location_txv);
        mCategoryBtns = new Button[CATEGORY_ID.length][];
        mCategoryViews = new View[CATEGORY_ID.length];
        for(int i = 0; i < CATEGORY_ID.length; i++){
        	mCategoryViews[i] = (View)mRootView.findViewById(CATEGORY_ID[i]);
        	mCategoryBtns[i] = new Button[NUM_OF_SUB_CATEGORY + 2];
        	mCategoryBtns[i][0] = (Button) mCategoryViews[i].findViewById(R.id.category_btn);
        	mCategoryBtns[i][1] = (Button) mCategoryViews[i].findViewById(R.id.sub_1_btn);
        	mCategoryBtns[i][2] = (Button) mCategoryViews[i].findViewById(R.id.sub_2_btn);
        	mCategoryBtns[i][3] = (Button) mCategoryViews[i].findViewById(R.id.sub_3_btn);
        	mCategoryBtns[i][4] = (Button) mCategoryViews[i].findViewById(R.id.sub_4_btn);
        	mCategoryBtns[i][5] = (Button) mCategoryViews[i].findViewById(R.id.sub_5_btn);
        	mCategoryBtns[i][6] = (Button) mCategoryViews[i].findViewById(R.id.sub_more_btn);
        }
    }

    protected void setListener() {
    	for(int i = 0; i < CATEGORY_ID.length; i++){
    		for(int j = 0; j < NUM_OF_SUB_CATEGORY + 2; j++){
    			mCategoryBtns[i][j].setOnClickListener(this);
    		}
    	}
    }
    
    private void setFilterListView() {
        //mFilterListView = (FilterListView) findViewById(R.id.filter_list_view);
        //mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);    	
        if (mFilterList != null) {
            FilterListView.selectedFilter(mFilterList.get(0), -1);
        } else {
            DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, Globals.getCurrentCityInfo().getId(), mContext);
            FilterCategoryOrder filterCategory = DataQuery.getPOIFilterCategoryOrder();
            if (filterCategory != null) {
                List<FilterOption> filterOptionList = new ArrayList<DataQuery.FilterOption>();
                List<FilterOption> online = filterCategory.getCategoryFilterOption();
                if (online != null) {
                    for(int i = 0, size = online.size(); i < size; i++) {
                        filterOptionList.add(online.get(i).clone());
                    }
                }
                List<Long> indexList = new ArrayList<Long>();
                indexList.add(0l);
                for(int i = 0, size = filterOptionList.size(); i < size; i++) {
                    long id = filterOptionList.get(i).getId();
                    indexList.add(id);
                }
                Filter categoryFilter = DataQuery.makeFilterResponse(mContext, indexList, filterCategory.getVersion(), filterOptionList, FilterCategoryOrder.FIELD_LIST_CATEGORY, false);
                List<Filter> list = categoryFilter.getChidrenFilterList();
                for(int i = 0 ; i < CATEGORY_ID.length; i++){
                	int position = -1;
                	for(int k = 0; k < list.size(); k++){
                		if(TextUtils.equals(mCategoryNames[i], list.get(k).getFilterOption().getName())){
                			position = k;
                			break;
                		}
                	}
                	if(position >= 0){
                		mCategoryBtns[i][0].setText(list.get(position).getFilterOption().getName());
                		Filter filter = list.get(position);
                		List<Filter> childrenList = filter.getChidrenFilterList();
                		for(int j = 0, size = childrenList.size(); j < NUM_OF_SUB_CATEGORY && j < size; j++){
                			mCategoryBtns[i][j+1].setText(childrenList.get(j).getFilterOption().getName());
                		}
                	}else{
                		mCategoryViews[i].setVisibility(View.GONE);
                	}
                }
                
                mFilterList = new ArrayList<Filter>();
                mFilterList.add(categoryFilter);
            }
            //mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.sub_1_btn:
        case R.id.sub_2_btn:
        case R.id.sub_3_btn:
        case R.id.sub_4_btn:
        case R.id.sub_5_btn:
        	submitQuery(((Button)view).getText().toString());
        	break;
        case R.id.sub_more_btn:
        	break;
        default:
            break;
        }
    }
    
    
    /**
     * 查询
     * @param keyword
     */
    private void submitQuery(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            DataQuery poiQuery = new DataQuery(mContext);
            POI requestPOI = mPOI;
            int cityId = MapEngine.getCityId(requestPOI.getPosition());
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, requestPOI.getUUID());
            Position position = requestPOI.getPosition();
            if (position != null) {
                poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
            }
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INFO, DataQuery.INFO_TYPE_TAG);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_EXT, DataQuery.EXT_BUSLINE);
            poiQuery.setup(cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
            mSphinx.queryStart(poiQuery);
            ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup(null);
            mSphinx.showView(poiQuery.getTargetViewId());
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }

    public void setData(POI poi) {
        reset();
        mPOI = poi;
    }
    
    /**
     * 将UI及内容重置为第一次进入页面时的状态
     */
    public void reset() {
    	// TODO:
    }

	@Override
	public void doFilter(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancelFilter() {
		// TODO Auto-generated method stub
		
	}
}

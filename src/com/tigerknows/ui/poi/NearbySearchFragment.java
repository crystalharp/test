/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;

import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.CategoryProperty;
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
    protected POI mPOI = new POI();
    
    protected TextView mLocationTxv;
    
    /**
     * 分类名称列表
     */
    private String[] mHotNames;
    private String[] mCategoryNames;
    private String[] mSubCategoryNames;
    private LinearLayout[][] mCategoryLlys;
    private Button[][] mCategoryBtns;

    private LinearLayout mHotBaseLly;
    private LinearLayout[] mHotLlys;
    private View[] mHotBtnViews;
    
    private boolean mFromPOI;
    
    private CategoryProperty[] mCategoryList;
    
    private static final int NUM_OF_CATGEGORY = 10;
    private static final int NUM_OF_HOT = 15;
    private static final int NUM_OF_HOT_LLY = 4;
    
    private static final int FOOD = 0;
    private static final int HOTEL = 1;
    private static final int ENTERTAINMENT = 2;
    private static final int TRAFFIC = 3;

    public void launchCategoryPropertyList(){
        final int[][] SPECIAL_OP = {
        	{FOOD, 0, CategoryProperty.OP_DISH},
        	{ENTERTAINMENT, 0, CategoryProperty.OP_DIANYING},
        	{ENTERTAINMENT, 6, CategoryProperty.OP_YANCHU},
        	{ENTERTAINMENT, 7, CategoryProperty.OP_ZHANLAN},
        	{TRAFFIC, 3, CategoryProperty.OP_SUBWAY}
        };
    	mCategoryList = new CategoryProperty[]{
        	new CategoryProperty(R.id.food_category, 7),
        	new CategoryProperty(R.id.hotel_category, 6),
        	new CategoryProperty(R.id.entertainment_category, 7),
        	new CategoryProperty(R.id.traffic_category, 3),
        	new CategoryProperty(R.id.shopping_category, 3),
        	new CategoryProperty(R.id.travel_category, 1),
        	new CategoryProperty(R.id.beauty_category, 1), 
        	new CategoryProperty(R.id.bank_category, 3),
        	new CategoryProperty(R.id.sports_category, 1),
        	new CategoryProperty(R.id.hospital_category, 1)
    	};
    	for(int i=0; i<SPECIAL_OP.length; i++){
    		mCategoryList[SPECIAL_OP[i][0]].setOperationType(SPECIAL_OP[i][1], SPECIAL_OP[i][2]);
    	}
    	mHotNames = mContext.getResources().getStringArray(R.array.custom_category);
    	mCategoryNames = mContext.getResources().getStringArray(R.array.home_category);
    	mSubCategoryNames = mContext.getResources().getStringArray(R.array.home_sub_category);
    	for(int i=0; i<NUM_OF_CATGEGORY; i++){
    		mCategoryList[i].setName(mCategoryNames[i]);
    		mCategoryList[i].setButtonText(mSubCategoryNames[i].split(";"));
    	}
    }
    
    private static final int[] HOT_LLY_ID = {R.id.hot_0_lly, R.id.hot_1_lly, R.id.hot_2_lly, R.id.hot_3_lly};
    private static final int[] HOT_BTN_ID = {R.id.hot_0_0_btn, R.id.hot_0_1_btn, R.id.hot_0_2_btn, R.id.hot_0_3_btn,
    	R.id.hot_1_0_btn, R.id.hot_1_1_btn, R.id.hot_1_2_btn, R.id.hot_1_3_btn,
    	R.id.hot_2_0_btn, R.id.hot_2_1_btn, R.id.hot_2_2_btn, R.id.hot_2_3_btn,
    	R.id.hot_3_0_btn, R.id.hot_3_1_btn, R.id.hot_3_2_btn, R.id.hot_3_3_btn
    };
    
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
        launchCategoryPropertyList();
        
        findViews();
        setListener();
        setButtonView();
        setFilterListView();
                
        mLocationTxv.setVisibility(View.VISIBLE);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHotView(TKConfig.getPref(mContext, TKConfig.PREFS_CUSTOM_CATEGORY, "111111100000000"));
        mLeftBtn.setOnClickListener(this);
        if(mFilterListView.getVisibility() == View.GONE){
        	mTitleBtn.setText(R.string.nearby_search);
        }else{
        	mTitleBtn.setText(R.string.merchant_type);
        }
        mRightBtn.setVisibility(View.GONE);
        String name = mPOI.getName();
        String title = getString(R.string.at_where_search, name);
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
        mHotBaseLly = (LinearLayout) mRootView.findViewById(R.id.hot_lly);
        mHotLlys = new LinearLayout[NUM_OF_HOT_LLY];
        mHotBtnViews = new View[NUM_OF_HOT + 1];
        for(int i = 0; i < NUM_OF_HOT_LLY; i++){
        	mHotLlys[i] = (LinearLayout) mRootView.findViewById(HOT_LLY_ID[i]);
        }
        for(int i = 0; i < NUM_OF_HOT + 1; i++){
        	mHotBtnViews[i] = (View)mRootView.findViewById(HOT_BTN_ID[i]);
        }
        mCategoryBtns = new Button[NUM_OF_CATGEGORY][];
        mCategoryLlys = new LinearLayout[NUM_OF_CATGEGORY][];
        mCategoryViews = new View[NUM_OF_CATGEGORY];
        for(int i = 0; i < NUM_OF_CATGEGORY; i++){
        	mCategoryViews[i] = (View)mRootView.findViewById(mCategoryList[i].getID());
        	mCategoryLlys[i] = new LinearLayout[CategoryProperty.LINEAR_ARRAY.length];
        	mCategoryLlys[i][0] = (LinearLayout) mCategoryViews[i].findViewById(R.id.sub_alpha_lly);
        	mCategoryLlys[i][1] = (LinearLayout) mCategoryViews[i].findViewById(R.id.sub_beta_lly);
        	mCategoryLlys[i][2] = (LinearLayout) mCategoryViews[i].findViewById(R.id.sub_more_lly);
        	mCategoryBtns[i] = new Button[CategoryProperty.NUM_OF_SUBBUTTONS + 1];
        	mCategoryBtns[i][0] = (Button) mCategoryViews[i].findViewById(R.id.sub_0_btn);
        	mCategoryBtns[i][1] = (Button) mCategoryViews[i].findViewById(R.id.sub_1_btn);
        	mCategoryBtns[i][2] = (Button) mCategoryViews[i].findViewById(R.id.sub_2_btn);
        	mCategoryBtns[i][3] = (Button) mCategoryViews[i].findViewById(R.id.sub_3_btn);
        	mCategoryBtns[i][4] = (Button) mCategoryViews[i].findViewById(R.id.sub_4_btn);
        	mCategoryBtns[i][5] = (Button) mCategoryViews[i].findViewById(R.id.sub_5_btn);
        	mCategoryBtns[i][6] = (Button) mCategoryViews[i].findViewById(R.id.sub_6_btn);
        	mCategoryBtns[i][7] = (Button) mCategoryViews[i].findViewById(R.id.sub_7_btn);
        	mCategoryBtns[i][8] = (Button) mCategoryViews[i].findViewById(R.id.sub_more_btn);
        	mCategoryBtns[i][9] = (Button) mCategoryViews[i].findViewById(R.id.category_btn);
        }
    }

    protected void setListener() {
    	for(int i = 0; i < NUM_OF_CATGEGORY; i++){
    		for(int j = 0; j < CategoryProperty.NUM_OF_SUBBUTTONS + 1; j++){
    			mCategoryBtns[i][j].setOnClickListener(this);
    		}
    	}
    	for(int i = 0; i < NUM_OF_HOT; i++){
    		mHotBtnViews[i].setOnClickListener(this);
    	}
    }
    
    private void refreshHotView(String customPrefs){
    	int countBtnView = 0;
    	int countLly = 0;
    	for(int i = 0; i < NUM_OF_HOT_LLY; i++){
    		mHotLlys[i].setVisibility(View.GONE);
    	}
    	for(int i = 0; i < NUM_OF_HOT + 1; i++){
    		mHotBtnViews[i].setVisibility(View.GONE);
    	}
    	for(int i = 0; i < customPrefs.length(); i++){
    		if(customPrefs.charAt(i) == '1'){
    			String[] str = mHotNames[i].split(";");
    			mHotBtnViews[countBtnView].setContentDescription(mHotNames[i]);
    			((TextView)mHotBtnViews[countBtnView].findViewById(R.id.app_name_txv)).setText(str[0]);
    			mHotBtnViews[countBtnView].setVisibility(View.VISIBLE);
    			countBtnView++;
    			if(countLly * 4 < countBtnView){
    				mHotLlys[countLly].setVisibility(View.VISIBLE);
    				countLly++;
    			}
    		}
    	}
    	mHotBtnViews[countBtnView].setContentDescription("自定义;" + CategoryProperty.OP_CUSTOM);
    	((TextView)mHotBtnViews[countBtnView].findViewById(R.id.app_name_txv)).setText("自定义");
    	mHotBtnViews[countBtnView].setVisibility(View.VISIBLE);
    	countBtnView++;
		if(countLly * 4 < countBtnView){
			mHotLlys[countLly].setVisibility(View.VISIBLE);
			countLly++;
		}
    	for(int i = countBtnView; i<countLly * 4; i++){
    		mHotBtnViews[i].setVisibility(View.INVISIBLE);
    	}

    }
    
    private void setButtonView(){
    	for(int i = 0; i < NUM_OF_CATGEGORY; i++){
    		CategoryProperty cp = mCategoryList[i];
    		mCategoryBtns[i][CategoryProperty.NUM_OF_SUBBUTTONS].setText(cp.getName());
    		mCategoryBtns[i][CategoryProperty.NUM_OF_SUBBUTTONS].setContentDescription(cp.getName() + ";" + String.valueOf(CategoryProperty.OP_SEARCH));
    		for(int j = 0; j < CategoryProperty.LINEAR_ARRAY.length; j++){
    			if((CategoryProperty.LINEAR_ARRAY[j] & mCategoryList[i].getLlyVisibility()) == 0){
    				mCategoryLlys[i][j].setVisibility(View.GONE);
    			}else{
    				mCategoryLlys[i][j].setVisibility(View.VISIBLE);
    			}
    		}
    		for(int j = 0; j < CategoryProperty.NUM_OF_SUBBUTTONS; j++){
    			mCategoryBtns[i][j].setText(cp.getButtonText(j));
    			int OP = mCategoryList[i].getOperationType(j);
    			mCategoryBtns[i][j].setContentDescription(cp.getButtonText(j) + ";" + String.valueOf(OP));
    		}
    	}
    }
    
    private void setFilterListView() {
        mFilterListView = (FilterListView) mRootView.findViewById(R.id.filter_list_view);
        mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);    	
        if (mFilterList != null) {
            FilterListView.selectedFilter(mFilterList.get(0), -1);
        } else {
            DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, mContext, MapEngine.getCityId(mPOI.getPosition()));
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
                categoryFilter.getChidrenFilterList().remove(0);
                
                mFilterList = new ArrayList<Filter>();
                mFilterList.add(categoryFilter);
            }
            mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.left_btn:
        	if(mFilterListView.getVisibility() == View.VISIBLE){
        		backHome();
        	}else{
                synchronized (mSphinx.mUILock) {
            	    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
                	dismiss();
                }
        	}
        	break;
        case R.id.category_btn:
        case R.id.sub_0_btn:
        case R.id.sub_1_btn:
        case R.id.sub_2_btn:
        case R.id.sub_3_btn:
        case R.id.sub_4_btn:
        case R.id.sub_5_btn:
        case R.id.sub_6_btn:
        case R.id.sub_7_btn:
        case R.id.sub_more_btn:
        	//TODO: mActionLog
        	btnClickProcess(view);
        	break;
        default:
        	for(int i = 0; i < HOT_BTN_ID.length; i++){
        		if(HOT_BTN_ID[i] == view.getId()){
        			btnClickProcess(view);
        			break;
        		}
        	}
            break;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(mFilterListView.getVisibility() == View.VISIBLE){
    		backHome();
    	}else{
            synchronized (mSphinx.mUILock) {
        	    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
            	dismiss();
            }
    	}
    	return true;
    }
    
    private void btnClickProcess(View btn){
		String[] str = btn.getContentDescription().toString().split(";");
    	LogWrapper.d("Trap", "cd: "+str.length + btn.getContentDescription());
		if(str.length < 2){
			return;
		}
		int operationCode = Integer.parseInt(str[1]);
    	switch(operationCode){
    	case CategoryProperty.OP_SEARCH:
    		submitQuery(str[0]);
    		break;
    	case CategoryProperty.OP_HOTEL:
    		mSphinx.getHotelHomeFragment().resetDate();
    		mSphinx.getHotelHomeFragment().setCityInfo(Globals.getCurrentCityInfo(mContext));
    		mSphinx.showView(R.id.view_hotel_home);
    		uiStackAdjust();
    		break;
    	case CategoryProperty.OP_SUBWAY:
    		mSphinx.getSubwayMapFragment().setData(Globals.getCurrentCityInfo(mContext));
    		mSphinx.showView(R.id.view_subway_map);
    		uiStackAdjust();
    		break;
    	case CategoryProperty.OP_TUANGOU:
    		//TODO: mSphinx.getDiscoverListFragment();
    		//mSphinx.showView(R.id.view_discover_list);
    		break;
    	case CategoryProperty.OP_MORE:
        	mTitleBtn.setText(R.string.merchant_type);
            mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, false, mActionTag);
            mFilterListView.setVisibility(View.VISIBLE);
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
            poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false, false, requestPOI);
            poiQuery.setCityId(cityId);
            mSphinx.queryStart(poiQuery);
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }
    
    public void setData(POI poi){
    	setData(poi, true);
    }

    public void setData(POI poi, boolean fromPOI) {
        reset();
        mPOI = poi;
        mFromPOI = fromPOI;
        if(fromPOI && mSphinx.uiStackContains(R.id.view_poi_nearby_search)){
        	mSphinx.uiStackRemove(R.id.view_poi_nearby_search);
        }
        setFilterListView();
    }
    
    /**
     * 将UI及内容重置为第一次进入页面时的状态
     */
    public void reset() {
        if (mFilterListView != null) {
    	    mFilterListView.setVisibility(View.GONE);
        }
    	mTitleBtn.setText(R.string.nearby_search);
    }

	@Override
	public void doFilter(String name) {
		backHome();
        Filter categoryFitler = mFilterList.get(0);
        List<Filter> list = categoryFitler.getChidrenFilterList();
        for(int i = 0, size = list.size(); i < size; i++) {
            Filter filter = list.get(i);
            if (filter.isSelected()) {
            }
            List<Filter> chidrenList = filter.getChidrenFilterList();
            for(int j = 0, count = chidrenList.size(); j < count; j++) {
                Filter chidren = chidrenList.get(j);
                if (chidren.isSelected()) {
                	submitQuery(chidren.getFilterOption().getName());
                    return;
                }
            }
        }		
	}

	@Override
	public void cancelFilter() {
		backHome();
		
	}
    
	protected void backHome() {
        mFilterListView.setVisibility(View.GONE);
        mTitleBtn.setText(R.string.nearby_search);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        
        String apiType = baseQuery.getAPIType();
        if (BaseQuery.API_TYPE_DATA_QUERY.equals(apiType)) {
            int result = InputSearchFragment.dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);
            if(mFromPOI && result > 0){
            	uiStackAdjust();
            }
        }
    }
    
    
    public void uiStackAdjust() {
        if (mSphinx.uiStackContains(R.id.view_more_favorite)) {
            mSphinx.uiStackClearBetween(R.id.view_more_favorite, getId());
        } else if (mSphinx.uiStackContains(R.id.view_more_history)) {
            mSphinx.uiStackClearBetween(R.id.view_more_history, getId());
        } else if (mSphinx.uiStackContains(R.id.view_more_go_comment)) {
            mSphinx.uiStackClearBetween(R.id.view_more_go_comment, getId());
        } else if (mSphinx.uiStackContains(R.id.view_user_my_comment_list)) {
            mSphinx.uiStackClearBetween(R.id.view_user_my_comment_list, getId());
        } else if (mSphinx.uiStackContains(R.id.view_more_my_order)) {
            mSphinx.uiStackClearBetween(R.id.view_more_my_order, getId());
        } else {
            if (mSphinx.uiStackContains(R.id.view_home) == false) {
                mSphinx.uiStackInsert(R.id.view_home, 0);
            }
            mSphinx.uiStackClearBetween(R.id.view_home, getId());
        }
    }
}

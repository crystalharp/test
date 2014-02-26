/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;

import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
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
    private Button mHotFoldBtn;
    
    private String mCurrentMoreCategory;

    private ScrollView mBodyScv;
    private LinearLayout mHotBaseLly;
    private LinearLayout[] mHotLlys;
    private View[] mHotBtnViews;
    
    private DataQuery mDataQuery;
    
    private String mIsFold;
    private int mCountLly;
    
    private CategoryProperty[] mCategoryList;
    
    private static final int NUM_OF_CATGEGORY = 10;
    public static final int NUM_OF_HOT = 15;
    private static final int NUM_OF_HOT_LLY = 4;
    
    private static final int FOOD = 0;
    private static final int HOTEL = 1;
    private static final int ENTERTAINMENT = 2;
    private static final int TRAFFIC = 3;
    
    private static final int STATUS_HOME = 0;
    private static final int STATUS_NEARBY = 1;
    private static final int STATUS_MORE = 2;

    public void launchCategoryPropertyList(){
        final int[][] SPECIAL_OP = {
        	{FOOD, 0, CategoryProperty.OP_DISH},
        	{HOTEL, 3, CategoryProperty.OP_HOTEL},
        	{ENTERTAINMENT, 0, CategoryProperty.OP_DIANYING},
        	{ENTERTAINMENT, 6, CategoryProperty.OP_YANCHU},
        	{ENTERTAINMENT, 7, CategoryProperty.OP_ZHANLAN},
        	{TRAFFIC, 3, CategoryProperty.OP_SUBWAY}
        };
    	mCategoryList = new CategoryProperty[]{
        	new CategoryProperty(R.id.food_category, 7),
        	new CategoryProperty(R.id.hotel_category, 3),
        	new CategoryProperty(R.id.entertainment_category, 7),
        	new CategoryProperty(R.id.traffic_category, 3),
        	new CategoryProperty(R.id.shopping_category, 3),
        	new CategoryProperty(R.id.travel_category, 1),
        	new CategoryProperty(R.id.bank_category, 3),
        	new CategoryProperty(R.id.beauty_category, 1), 
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
    		mCategoryList[i].setDrawableID(CATEGORY_DRAWABLE_ID[i]);
    		mCategoryList[i].setColorID(CATEGORY_COLOR_ID[i]);
    	}
    }
    private static final int[] CATEGORY_DRAWABLE_ID = {R.drawable.ic_arrow_food,
    	R.drawable.ic_arrow_hotel,
    	R.drawable.ic_arrow_entertainment,
    	R.drawable.ic_arrow_traffic,
    	R.drawable.ic_arrow_shopping,
    	R.drawable.ic_arrow_travel,
    	R.drawable.ic_arrow_bank,
    	R.drawable.ic_arrow_beauty,
    	R.drawable.ic_arrow_sports,
    	R.drawable.ic_arrow_hospital
    };
    
    private static final int[] CATEGORY_COLOR_ID = {R.color.nearby_food,
    	R.color.nearby_hotel,
    	R.color.nearby_entertainment,
    	R.color.nearby_traffic,
    	R.color.nearby_shopping,
    	R.color.nearby_travel,
    	R.color.nearby_bank,
    	R.color.nearby_beauty,
    	R.color.nearby_sports,
    	R.color.nearby_hospital
    };
    
    private static final int[] HOT_LLY_ID = {R.id.hot_0_lly, R.id.hot_1_lly, R.id.hot_2_lly, R.id.hot_3_lly};
    private static final int[] HOT_BTN_ID = {R.id.hot_0_0_btn, R.id.hot_0_1_btn, R.id.hot_0_2_btn, R.id.hot_0_3_btn,
    	R.id.hot_1_0_btn, R.id.hot_1_1_btn, R.id.hot_1_2_btn, R.id.hot_1_3_btn,
    	R.id.hot_2_0_btn, R.id.hot_2_1_btn, R.id.hot_2_2_btn, R.id.hot_2_3_btn,
    	R.id.hot_3_0_btn, R.id.hot_3_1_btn, R.id.hot_3_2_btn, R.id.hot_3_3_btn
    };
    private static final int[] HOT_DRAWABLE_ID = {R.drawable.ic_custom_tuangou,
    	R.drawable.ic_custom_hotel,
    	R.drawable.ic_custom_movie,
    	R.drawable.ic_custom_subway,
    	R.drawable.ic_custom_bus,
    	R.drawable.ic_custom_toilet,
    	R.drawable.ic_custom_yanchu,
    	R.drawable.ic_custom_zhanlan,
    	R.drawable.ic_custom_ktv,
    	R.drawable.ic_custom_netbar,
    	R.drawable.ic_custom_bath,
    	R.drawable.ic_custom_atm,
    	R.drawable.ic_custom_market,
    	R.drawable.ic_custom_mall,
    	R.drawable.ic_custom_sale,
    	R.drawable.ic_custom_add
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
        mSphinx.showHint(TKConfig.PREFS_HINT_NEARBY_SEARCH, R.layout.hint_nearby_search);
        mIsFold = TKConfig.getPref(mContext, TKConfig.PREFS_CUSTOM_FOLD, "No");
        refreshHotView(TKConfig.getPref(mContext, TKConfig.PREFS_CUSTOM_CATEGORY, "111100110001000"));
        doFold();
        mLeftBtn.setOnClickListener(this);
        if(mFilterListView.getVisibility() == View.VISIBLE){
        	refreshTitleView(STATUS_MORE);
        }else if(mPOI.getSourceType() != POI.SOURCE_TYPE_MAP_CENTER && mPOI.getSourceType() != POI.SOURCE_TYPE_MY_LOCATION){
        	refreshTitleView(STATUS_NEARBY);
        }else{
        	refreshTitleView(STATUS_HOME);
        }
        String name = mPOI.getName();
        String title = getString(R.string.at_where_search, name);
        SpannableStringBuilder style = new SpannableStringBuilder(title);
        int focusedColor = mSphinx.getResources().getColor(R.color.black_dark);
        style.setSpan(new ForegroundColorSpan(focusedColor), 0, 2, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        TextView nearbyTxv = (TextView)mRootView.findViewById(R.id.nearby_txv);
        nearbyTxv.setVisibility(View.VISIBLE);
        LinearLayout nearbyLly = (LinearLayout)mRootView.findViewById(R.id.nearby_bar_lly);
        mLocationTxv.setMaxWidth(
        		(int) (
        				mContext.getResources().getDisplayMetrics().widthPixels
        				- nearbyLly.getPaddingLeft()
        				- nearbyLly.getPaddingRight()
        				- nearbyTxv.getPaint().measureText(nearbyTxv.getText().toString())
        				));
        mLocationTxv.setText(style);
    }
    
    private void refreshTitleView(int status){
    	//状态HOME不可能跳转到NEARBY，反之亦然，但其他4种可能的跳转均会发生
    	switch(status){
    	case STATUS_HOME:
    		mTitleBtn.setText(R.string.nearby_search);
        	mRightBtn.setVisibility(View.VISIBLE);
        	mRightBtn.setBackgroundResource(R.drawable.btn_nearby_search);
        	mRightBtn.setOnClickListener(this);
        	mRightBtn.setPadding(0, 0, 0, 0);
        	break;
    	case STATUS_NEARBY:
            mTitleBtn.setVisibility(View.GONE);
            mKeywordEdt.setVisibility(View.VISIBLE);
            
            mKeywordEdt.mActionTag = mActionTag;
            mKeywordEdt.setText("");
            mSphinx.hideSoftInput();
            mKeywordEdt.setOnTouchListener(new OnTouchListener() {
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    	mActionLog.addAction(mActionTag + ActionLog.TitleCenterButton);
                    	mSphinx.getInputSearchFragment().setData(mDataQuery, null, InputSearchFragment.MODE_POI);        	
                        mSphinx.showView(R.id.view_poi_input_search);
                    }
                    return false;
                }
            });
        	mRightBtn.setVisibility(View.VISIBLE);
        	mRightBtn.setOnClickListener(this);
        	mRightBtn.setText(R.string.cancel);
        	mRightBtn.setTextColor(mSphinx.getResources().getColor(R.color.black_dark));
        	mRightBtn.setBackgroundResource(R.drawable.btn_cancel);
        	break;
    	case STATUS_MORE:
    		mTitleBtn.setVisibility(View.VISIBLE);
    		mTitleBtn.setText(R.string.more);
    		mKeywordEdt.setVisibility(View.GONE);
    		mRightBtn.setVisibility(View.GONE);
    	}
    }

    @Override
    public void onPause() {
        TKConfig.setPref(mContext, TKConfig.PREFS_CUSTOM_FOLD, mIsFold);
        super.onPause();
    }

    @Override
    protected void findViews() {
        super.findViews();
        mBodyScv = (ScrollView) mRootView.findViewById(R.id.body_scv);
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
        mHotFoldBtn = (Button) mRootView.findViewById(R.id.hot_fold_btn);
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

    @Override
    protected void setListener() {
        super.setListener();
    	for(int i = 0; i < NUM_OF_CATGEGORY; i++){
    		for(int j = 0; j < CategoryProperty.NUM_OF_SUBBUTTONS + 1; j++){
    			mCategoryBtns[i][j].setOnClickListener(this);
    		}
    	}
    	for(int i = 0; i < NUM_OF_HOT + 1; i++){
    		mHotBtnViews[i].setOnClickListener(this);
    	}
    	mHotFoldBtn.setOnClickListener(this);
    }
    
    private void refreshHotView(String customPrefs){
    	int countBtnView = 0;
    	mCountLly = 0;
    	for(int i = 0; i < NUM_OF_HOT_LLY; i++){
    		mHotLlys[i].setVisibility(View.GONE);
    	}
    	for(int i = 0; i < NUM_OF_HOT + 1; i++){
    		mHotBtnViews[i].setVisibility(View.GONE);
    	}
    	for(int i = 0; i < customPrefs.length() + 1; i++){
    		if(i == customPrefs.length() || 	//“自定义”按钮
    				customPrefs.charAt(i) == '1'){
    			String[] str = mHotNames[i].split(";");
    			mHotBtnViews[countBtnView].setContentDescription(mHotNames[i]);
    			((TextView)mHotBtnViews[countBtnView].findViewById(R.id.app_name_txv)).setText(str[0]);
    			((ImageView)mHotBtnViews[countBtnView].findViewById(R.id.app_icon_imv)).setBackgroundResource(HOT_DRAWABLE_ID[i]);
    			mHotBtnViews[countBtnView].setVisibility(View.VISIBLE);
    			countBtnView++;
    			if(mCountLly * 4 < countBtnView){
    				mHotLlys[mCountLly].setVisibility(View.VISIBLE);
    				mCountLly++;
    			}
    		}
    	}
    	for(int i = countBtnView; i<mCountLly * 4; i++){
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
    			if(OP == CategoryProperty.OP_MORE){
    				mCategoryBtns[i][j].setContentDescription(cp.getName() + ";" + String.valueOf(OP));
    			}else{
    				mCategoryBtns[i][j].setContentDescription(cp.getButtonText(j) + ";" + String.valueOf(OP));
    			}
    		}
    		Drawable drawable = mSphinx.getResources().getDrawable(cp.getDrawableID());
    		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    		mCategoryBtns[i][CategoryProperty.NUM_OF_SUBBUTTONS].setCompoundDrawables(null, null, drawable, null);
    		mCategoryBtns[i][CategoryProperty.NUM_OF_SUBBUTTONS].setTextColor(mSphinx.getResources().getColor(cp.getColorID()));
    	}
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void queryFilter() {
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_FILTER);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CONFIGINFO, DataQuery.CONFIGINFO_POI_CATEGORY_ORDER);
        dataQuery.setup(getId(), getId(), null, true);
        mSphinx.queryStart(dataQuery);
    }
    
    private boolean setFilterListView() {
    	boolean result = false;
        mFilterListView = (FilterListView) mRootView.findViewById(R.id.filter_list_view);
        mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);    	
        if (mFilterList != null && mFilterList.size() > 0) {
            FilterListView.selectedFilter(mFilterList.get(0), -1);
            result = true;
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
                Filter categoryFilter = DataQuery.makeFilterResponse(mContext, indexList, filterCategory.getVersion(), filterOptionList, FilterCategoryOrder.FIELD_LIST_CATEGORY, true);
                categoryFilter.getChidrenFilterList().remove(0);
                
                mFilterList = new ArrayList<Filter>();
                mFilterList.add(categoryFilter);

                mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, mActionTag);
                result = true;
            }
        }
        return result;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.left_btn:
        	if(mFilterListView.getVisibility() == View.VISIBLE){
        		backHome();
        	}else{
                synchronized (mSphinx.mUILock) {
                	mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_CANCEL);
                	dismiss();
                }
        	}
        	break;
        case R.id.right_btn:
        	if(mPOI.getSourceType() != POI.SOURCE_TYPE_MAP_CENTER && mPOI.getSourceType() != POI.SOURCE_TYPE_MY_LOCATION){
                synchronized (mSphinx.mUILock) {
                	mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                	dismiss();
                }        		
        	}else{
        		mSphinx.getInputSearchFragment().setData(mDataQuery,
        				null,
        				InputSearchFragment.MODE_POI);
        		mSphinx.showView(R.id.view_poi_input_search);
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
        	btnClickProcess(view, ActionLog.POINearbySearchSub);
        	break;
        case R.id.sub_more_btn:
        	btnClickProcess(view, ActionLog.POINearbySearchMore);
        	break;
        case R.id.hot_fold_btn:
        	if(TextUtils.equals("No", mIsFold)){
        		mActionLog.addAction(mActionTag + ActionLog.POINearbySearchFold, "false");
        		mIsFold = "Yes";
        	}else{
        		mActionLog.addAction(mActionTag + ActionLog.POINearbySearchFold, "true");
        		mIsFold = "No";
        	}
        	doFold();
        	break;
        default:
        	for(int i = 0; i < HOT_BTN_ID.length; i++){
        		if(HOT_BTN_ID[i] == view.getId()){
        			btnClickProcess(view, ActionLog.POINearbySearchHot);
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
            	dismiss();
            }
    	}
    	return true;
    }
    
    private void btnClickProcess(View btn, String actionLogInfo){
		String[] str = btn.getContentDescription().toString().split(";");
		if(str.length < 2){
			return;
		}

		int operationCode = Integer.parseInt(str[1]);
		DataQuery dataQuery;
		int cityId = mDataQuery.getCityId();
    	switch(operationCode){
    	case CategoryProperty.OP_SEARCH:
    		mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
    		submitQuery(str[0]);
    		break;
    	case CategoryProperty.OP_HOTEL:
    		mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
    		mSphinx.getHotelHomeFragment().resetDate();
    		mSphinx.getHotelHomeFragment().setCityInfo(Globals.getCurrentCityInfo(mContext));
    		mSphinx.showView(R.id.view_hotel_home);
    		mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
    		break;
    	case CategoryProperty.OP_SUBWAY:
    		mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
    		mSphinx.getSubwayMapFragment().setData(Globals.getCurrentCityInfo(mContext, false));
    		mSphinx.showView(R.id.view_subway_map);
    		// 再次进地铁图时，调整的堆栈与其他情况不同
            Message msg = new Message();
            msg.what = Sphinx.UI_STACK_ADJUST_READY;
            msg.arg1 = getId();
            mSphinx.getHandler().sendMessage(msg);    		
    		mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
    		break;
        case CategoryProperty.OP_DISH:
        	mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
        	if (cityId != CityInfo.CITY_ID_BEIJING) {
        		Toast.makeText(mSphinx, R.string.this_city_not_support_dish, Toast.LENGTH_LONG).show();
        		return;
        	}
            dataQuery = getDiscoverDataQuery(BaseQuery.DATA_TYPE_POI);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, getString(R.string.cate));
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_DISH);
            dataQuery.setCityId(cityId);
            mSphinx.queryStart(dataQuery);
            break;
        case CategoryProperty.OP_TUANGOU:
        	mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
            if (DataQuery.checkDiscoveryCity(cityId, Long.parseLong(BaseQuery.DATA_TYPE_TUANGOU)) == false) {
                Toast.makeText(mSphinx, R.string.this_city_not_support_tuangou, Toast.LENGTH_LONG).show();
                return;
            }
            mSphinx.queryStart(getDiscoverDataQuery(String.valueOf(operationCode)));
            mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
            break;
        case CategoryProperty.OP_DIANYING:
        	mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
            if (DataQuery.checkDiscoveryCity(cityId, Long.parseLong(BaseQuery.DATA_TYPE_DIANYING)) == false) {
                Toast.makeText(mSphinx, R.string.this_city_not_support_dianying, Toast.LENGTH_LONG).show();
                return;
            }
            mSphinx.queryStart(getDiscoverDataQuery(String.valueOf(operationCode)));
            mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
            break;
        case CategoryProperty.OP_YANCHU:
        	mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
            if (DataQuery.checkDiscoveryCity(cityId, Long.parseLong(BaseQuery.DATA_TYPE_YANCHU)) == false) {
                Toast.makeText(mSphinx, R.string.this_city_not_support_yanchu, Toast.LENGTH_LONG).show();
                return;
            }
            mSphinx.queryStart(getDiscoverDataQuery(String.valueOf(operationCode)));
            mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
            break;
        case CategoryProperty.OP_ZHANLAN:
        	mActionLog.addAction(mActionTag + actionLogInfo, str[0]);
            if (DataQuery.checkDiscoveryCity(cityId, Long.parseLong(BaseQuery.DATA_TYPE_ZHANLAN)) == false) {
                Toast.makeText(mSphinx, R.string.this_city_not_support_zhanlan, Toast.LENGTH_LONG).show();
                return;
            }
            mSphinx.queryStart(getDiscoverDataQuery(String.valueOf(operationCode)));
            mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
            break;
    	case CategoryProperty.OP_MORE:
    		mActionLog.addAction(mActionTag + actionLogInfo);
    		mCurrentMoreCategory = str[0];
    		if(setFilterListView()){
    			setFilterOrder();
    		}else{
    			queryFilter();
    		}
            break;
    	case CategoryProperty.OP_CUSTOM:
    		mActionLog.addAction(mActionTag + ActionLog.POINearbySearchCustom);
    		mSphinx.showView(R.id.view_poi_custom_category);
    		break;
    	default:
    		break;
    	}    	
    }
    
    private void setFilterOrder() {
		mTitleBtn.setText(R.string.more);
		List<Filter> categoryFilterList = mFilterList.get(0).getChidrenFilterList();
		Filter currentFilter = null;
		for(int i = 0; i < categoryFilterList.size(); i++){
			if(TextUtils.equals(mCurrentMoreCategory, categoryFilterList.get(i).getFilterOption().getName())){
				currentFilter = categoryFilterList.get(i);
				break;
			}
		}
		if(currentFilter != null){
			FilterListView.selectedFilter(mFilterList.get(0), currentFilter);
		}
		mFilterListView.setData(mFilterList, FilterResponse.FIELD_FILTER_CATEGORY_INDEX, this, false, mActionTag);
		mFilterListView.setVisibility(View.VISIBLE);
		refreshTitleView(STATUS_MORE);
	}

	private void doFold(){
		if(mCountLly == 1){
			mHotFoldBtn.setVisibility(View.GONE);
			mIsFold = "No";
		}else if(TextUtils.equals("No", mIsFold)){
    		for(int i = 1; i < mCountLly; i++){
    			mHotLlys[i].setVisibility(View.VISIBLE);
    			mHotFoldBtn.setBackgroundResource(R.drawable.btn_to_fold);
    			mHotFoldBtn.setVisibility(View.VISIBLE);
    		}
    	}else{
    		for(int i = 1; i < NUM_OF_HOT_LLY; i++){
    			mHotLlys[i].setVisibility(View.GONE);
    			mHotFoldBtn.setBackgroundResource(R.drawable.btn_to_expand);
    			mHotFoldBtn.setVisibility(View.VISIBLE);
    		}
    	}
    	TKConfig.setPref(mContext, TKConfig.PREFS_CUSTOM_FOLD, mIsFold);
    }
    
    private DataQuery getDiscoverDataQuery(String dataType) {
        DataQuery dataQuery = new DataQuery(mDataQuery);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, dataType);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.delParameter(DataQuery.SERVER_PARAMETER_POI_ID);
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        return dataQuery;
    }
    
    /**
     * 查询
     * @param keyword
     */
    private void submitQuery(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            DataQuery poiQuery = new DataQuery(mDataQuery);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INFO, DataQuery.INFO_TYPE_TAG);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_EXT, DataQuery.EXT_BUSLINE);
            poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
            mSphinx.queryStart(poiQuery);
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }
    
    public void setData(DataQuery dataQuery) {
        reset();
        refreshDiscoverCities();
        mDataQuery = dataQuery;
        mPOI = mDataQuery.getPOI();
        if(mPOI.getSourceType() != POI.SOURCE_TYPE_MAP_CENTER && mPOI.getSourceType() != POI.SOURCE_TYPE_MY_LOCATION){
        	if(mSphinx.uiStackContains(R.id.view_poi_nearby_search)){
        		mSphinx.uiStackRemove(R.id.view_poi_nearby_search);
        	}
        	Message msg = new Message();
        	msg.what = Sphinx.UI_STACK_ADJUST_READY;
        	msg.arg1 = (mSphinx.uiStackPeek() == R.id.view_subway_map) ? mSphinx.uiStackPeek() : getId();
        	mSphinx.getHandler().sendMessage(msg);
        }else{
        	mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_CANCEL);
        }
        mBodyScv.scrollTo(0, 0);
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
                Filter children = chidrenList.get(j);
                if (children.isSelected()) {
                	if(children.getFilterOption().getName().contains(mSphinx.getString(R.string.all))){
                		submitQuery(filter.getFilterOption().getName());
                	}else{
                		submitQuery(children.getFilterOption().getName());
                	}
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
        if(mPOI.getSourceType() != POI.SOURCE_TYPE_MAP_CENTER && mPOI.getSourceType() != POI.SOURCE_TYPE_MY_LOCATION){
        	refreshTitleView(STATUS_NEARBY);
        }else{
        	refreshTitleView(STATUS_HOME);
        }
    }


    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        
        String apiType = baseQuery.getAPIType();
        if (BaseQuery.API_TYPE_DATA_QUERY.equals(apiType)) {
            String dataType = baseQuery.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
            if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);
            } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) ||
                    BaseQuery.DATA_TYPE_DIANYING.equals(dataType) ||
                    BaseQuery.DATA_TYPE_YANCHU.equals(dataType) ||
                    BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                
            	dealWithDynamicPOIResponse((DataQuery) baseQuery, mSphinx, this);
            } else if(BaseQuery.DATA_TYPE_FILTER.equals(dataType)){
            	if(setFilterListView()){
            		setFilterOrder();
            	}
            }
        }
    }
}

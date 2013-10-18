/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterArea;
import com.tigerknows.model.DataQuery.FilterOption;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.hotel.PickLocationFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Peng Wenyue
 */
@SuppressLint("ValidFragment")
public class POIHomeFragment extends BaseFragment implements View.OnClickListener, PickLocationFragment.Invoker {

    public POIHomeFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "HomeFragment";
    static final String CATEGORY_FILE_NAME = "poi_category.xml";
    static final String ELEMENT_CATEGORY = "category";
    static final String ELEMENT_SUB_CATEGORY = "sub";
    static final String ELEMENT_VALUE_ATTR = "word";
    static final String ELEMENT_HIGH_LIGHT_ATTR = "highLight";
    
    /**
     * 美食在列表中的下标
     */
    static final int FOOD_INDEX = 0;
    
    /**
     * 酒店在列表中的下标
     */
    static final int HOTEL_INDEX = 1;
    
    /**
     * 交通在列表中的下标
     */
    static final int TRAFFIC_INDEX = 8;

    private Button mCityBtn;
    private Button mInputBtn;
    private TextView mMyLoactionTxv;
    
    private View mDragViewParent;
    private View mDragView;
    private GridView mSubCategoryGrid;
    private View mTransPaddingView;
    private ImageView mCategoryTagImv;
    private int mScreenWidth;
    private int mDragViewWidth;
    private int mDragViewHeight;
    private int mDragViewParentX;
    private int mDragViewParentY;
    private int mTransPaddingWidth;
    
    private ArrayAdapter<String> mSubCategoryAdapter;
    
    private Position mLastPosition;
    private String mLocationName;
    private long mLastTime = 0;
    private ListView mCategoryLsv;
    private CategoryAdapter mCategoryAdapter;
    private int mCategoryTop;
    private int mMyLocationViewHeight;
    private int mCategoryPadding = 0;
    private int mCityId = MapEngine.CITY_ID_INVALID;

    private View mSubwayMapView;
    private Button mSubwayMapBtn;
    private ImageView mSubwayMapImv;

    private String[] mCategoryNameList;
    List<Category> mCategorylist = new ArrayList<Category>();
    private final int[] mCategoryResIdList = {
            R.drawable.category_food,
            R.drawable.category_hotel,
            R.drawable.category_play,
            R.drawable.category_shopping,
            R.drawable.category_travel,
            R.drawable.category_beauty,
            R.drawable.category_sports,
            R.drawable.category_bank,
            R.drawable.category_traffic,
            R.drawable.category_hospital
            };
    private final int[] mCategoryTagResIdList = {
            R.drawable.category_tag_food,
            R.drawable.category_tag_hotel,
            R.drawable.category_tag_play,
            R.drawable.category_tag_shopping,
            R.drawable.category_tag_travel,
            R.drawable.category_tag_beauty,
            R.drawable.category_tag_sports,
            R.drawable.category_tag_bank,
            R.drawable.category_tag_traffic,
            R.drawable.category_tag_hospital
            };

	ArrayList< ArrayList<String> > subCategories = new ArrayList< ArrayList<String> >();
	String[] mHighLightedSubs;
    
    private Dialog mProgressDialog = null;
    
    private POI mPOI = null;
    
    private boolean mSelectedLocation = false;
    
    @Override
    public void setSelectedLocation(boolean selectedLocation) {
        mSelectedLocation = selectedLocation;
        mPOI = null;
        mMyLoactionTxv.setText(mSphinx.getString(R.string.appoint_area) + FilterListView.getFilterTitle(mSphinx, HotelHomeFragment.getFilter(mFilterList, FilterArea.FIELD_LIST)));
        mMyLoactionTxv.setVisibility(View.VISIBLE);
        mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
        if (mSelectedLocation == false) {
            mMyLoactionTxv.setBackgroundResource(R.drawable.btn_my_location);
            Filter filter = HotelHomeFragment.getFilter(mFilterList, FilterArea.FIELD_LIST);
            if (filter != null) {
                FilterListView.selectedFilter(filter, Integer.MIN_VALUE);
            }
        } else {
            mMyLoactionTxv.setBackgroundResource(R.drawable.btn_appointed_area);
        }
    }
    
    public void setPOI(POI poi) {
        mPOI = poi;
        mMyLoactionTxv.setBackgroundResource(R.drawable.btn_appointed_area);
        mMyLoactionTxv.setText(mSphinx.getString(R.string.appoint_location) + mPOI.getName());
        mMyLoactionTxv.setVisibility(View.VISIBLE);
        mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
    }
    
    public POI getPOI() {
        return mPOI;
    }
    
    Response mResponse = null;
	
    List<Filter> mFilterList = new ArrayList<DataQuery.Filter>();
    
    @Override
    public List<Filter> getFilterList() {
        return mFilterList;
    }

    @Override
    public void refreshFilterAreaView() {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * 显示进度对话框
     * @param id
     */
    void showProgressDialog() {
        if (mProgressDialog == null) {
            View custom = mSphinx.getLayoutInflater().inflate(R.layout.loading, null);
            TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
            loadingTxv.setText(mSphinx.getString(R.string.doing_and_wait));
            mProgressDialog = Utility.showNormalDialog(mSphinx, custom);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        if (mProgressDialog.isShowing() == false) {
            mProgressDialog.show();
        }
        
    }
    
    /**
     * 关闭进度对话框
     */
    void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
    
    /**
     * 查询筛选数据，在查询之前需要将之前的查询停止
     */
    private void queryFilter() {
        stopQuery();
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_FILTER);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CONFIGINFO, "{\"0\":\"0.0.0\"}");
        dataQuery.setup(Globals.getCurrentCityInfo(false).getId(), getId(), getId(), null, true);
        mSphinx.queryStart(dataQuery);
    }
    
    void stopQuery() {
        List<BaseQuery> list = mBaseQuerying;
        if (list != null && list.size() > 0) {
            for(int i = 0, size = list.size(); i < size; i++) {
                list.get(i).stop();
            }
        }
        TKAsyncTask tkAsyncTask = mTkAsyncTasking;
        if (tkAsyncTask != null) {
            tkAsyncTask.stop();
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else {
            boolean result = true;
            FilterArea filterArea = DataQuery.getFilterArea();
            if (filterArea == null || filterArea.getVersion().equals("0.0.0")) {
//                queryFilter();
//                return;
                result = false;
            }
                
            if (result) {
                setDataToPickLocationFragment();
            }
            
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                if (result) {
                    mSphinx.getPickLocationFragment().setTitle(mSphinx.getString(R.string.poi_change_location));
                    mSphinx.showView(R.id.view_hotel_pick_location);
                }
                dismissProgressDialog();
            }
        }
    }
    
    void setDataToPickLocationFragment() {
        FilterArea filterArea = DataQuery.getFilterArea();
        if (filterArea != null && filterArea.getAreaFilterOption().size() > 0) {
            List<FilterOption> filterOptionList = filterArea.getAreaFilterOption();
            List<Long> indexList = new ArrayList<Long>();
            for(int i = 0, size = filterOptionList.size(); i < size; i++) {
                long id = filterOptionList.get(i).getId();
                if (id > 0 && id <= 10) {
                    continue;
                }
                indexList.add(id);
            }

            if (indexList.size() > 0) {
                indexList.add(0, Long.MIN_VALUE);
                HotelHomeFragment.deleteFilter(mFilterList, FilterArea.FIELD_LIST);
                Filter filter = DataQuery.makeFilterResponse(mSphinx, indexList, filterArea.getVersion(), filterOptionList, FilterArea.FIELD_LIST);
                mFilterList.add(filter);

                mSphinx.getPickLocationFragment().setData(mFilterList);
            }
        }
    }
	
	public CategoryAdapter getCategoryAdapter() {
	    return mCategoryAdapter;
	}
	
	public ListView getCategoryLsv() {
        return mCategoryLsv;
	}
	
    class CategoryXMLHandler extends DefaultHandler{
    	
    	private int curIndex = -1;
    	
		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
			mHighLightedSubs = new String[mCategoryResIdList.length];
			subCategories.clear();
			curIndex = -1;
		}

		@Override
		public void endDocument() throws SAXException {
			// TODO Auto-generated method stub
			super.endDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			
			if(localName.equals(ELEMENT_CATEGORY)){
				curIndex++;
				subCategories.add(new ArrayList<String>());
				subCategories.get(curIndex).add(attributes.getValue(ELEMENT_VALUE_ATTR));
				mHighLightedSubs[curIndex] = attributes.getValue(ELEMENT_HIGH_LIGHT_ATTR);
			}else if(localName.equals(ELEMENT_SUB_CATEGORY)){
				subCategories.get(curIndex).add( attributes.getValue(ELEMENT_VALUE_ATTR));				
			}
		}
    	
    	
    }
    
    private void loadCategories(){
    	try {
			SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
			CategoryXMLHandler handler = new CategoryXMLHandler();
			
			sp.parse(mContext.getAssets().open(CATEGORY_FILE_NAME), handler);
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIHome;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        loadCategories();
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_home, container, false);
        
        findViews();
        setListener();
        
        mScreenWidth = Globals.g_metrics.widthPixels;

        mSubCategoryAdapter = new ArrayAdapter<String>(mContext, R.layout.poi_home_drag_view_item, new ArrayList<String>());
        mSubCategoryGrid.setAdapter(mSubCategoryAdapter);
        
        mCategoryNameList = getResources().getStringArray(R.array.home_category);

        for(int i = 0, length = mCategoryResIdList.length; i < length; i++) {
                Category category = new Category();
                category.resId = mCategoryResIdList[i];
                category.name = mCategoryNameList[i];
                mCategorylist.add(category);
        }
        
        mCategoryAdapter = new CategoryAdapter(mContext, mCategorylist);
        mCategoryLsv.setAdapter(mCategoryAdapter);
        
        mMyLoactionTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mCategoryPadding = Util.dip2px(Globals.g_metrics.density, 8);
        mMyLocationViewHeight = mMyLoactionTxv.getMeasuredHeight();
        mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
        mCategoryAdapter.notifyDataSetChanged();
        

        
        return mRootView;
    }

    private boolean mIsResumeCalled = false;
    
    @Override
    public void onResume() {
        super.onResume();
        mMenuFragment.updateMenuStatus(R.id.poi_btn);
        mTitleFragment.hide();
        mCityBtn.setText(Globals.getCurrentCityInfo().getCName());
        
        mMenuFragment.display();

        refreshLocationView();
        // 将mCategoryLsv滚动到最顶
        mCategoryLsv.setSelectionFromTop(0, 0);
        
        //If resume hasn't been called
        if(!mIsResumeCalled){
        	mIsResumeCalled = true;
        	mRootView.post(new Runnable() {
	        	
				@Override
				public void run() {
					
					int[] loc = new int[2];
					mDragViewParent.getLocationInWindow(loc);
					mDragViewParentX = loc[0];
					mDragViewParentY = loc[1];
					
					mDragViewWidth = mDragViewParent.getRight() - mDragViewParent.getLeft();
					mDragViewHeight = mDragViewParent.getBottom() - mDragViewParent.getTop();
					
					mTransPaddingWidth = mTransPaddingView.getRight() - mTransPaddingView.getLeft();
					
				}//end run
				
			});
        }else{
        }
        mSphinx.showHomeDragHint();

        int cityId = Globals.getCurrentCityInfo().getId();
        if (mCityId != cityId) {
            mCityId = cityId;
            if (MapEngine.checkSupportSubway(Globals.getCurrentCityInfo().getId())) {
                mHighLightedSubs[TRAFFIC_INDEX] = mSphinx.getString(R.string.traffic_highLight_subwaymap);
            } else {
                mHighLightedSubs[TRAFFIC_INDEX] = mSphinx.getString(R.string.traffic_highLight_normal);
            }
            mCategoryAdapter.notifyDataSetChanged();
        }
        refreshSubCategoryListView();
		mDragView.setVisibility(View.INVISIBLE);
		mIsSubCategoryExpanded = false;
		
		refreshFilterArea();
    }
    
    void refreshFilterArea() {
        DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, Globals.getCurrentCityInfo(false).getId());
        FilterArea filterArea = DataQuery.getFilterArea();
        Filter filter = HotelHomeFragment.getFilter(mFilterList, FilterArea.FIELD_LIST);
        if (filterArea == null || filterArea.getVersion().equals("0.0.0")) {
            queryFilter();
        } else if (filter == null) {
            setDataToPickLocationFragment();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mDragView.setVisibility(View.GONE);
    }

    protected void findViews() {
        mCategoryLsv = (ListView) mRootView.findViewById(R.id.category_lsv);
        mInputBtn = (Button) mRootView.findViewById(R.id.input_btn);
        mCityBtn = (Button) mRootView.findViewById(R.id.city_btn);
        mMyLoactionTxv = (TextView) mRootView.findViewById(R.id.my_location_txv);
        mSubCategoryGrid = (GridView) mRootView.findViewById(R.id.sub_category_grid);
        mTransPaddingView = mRootView.findViewById(R.id.trans_padding);
        mDragView = mRootView.findViewById(R.id.drag_view);
        mDragViewParent = mRootView.findViewById(R.id.drag_view_parent);
        mCategoryTagImv = (ImageView) mRootView.findViewById(R.id.imv_category_tag);

        mSubwayMapView = mRootView.findViewById(R.id.subway_map_view);
        mSubwayMapBtn = (Button)mRootView.findViewById(R.id.subway_map_btn);
        mSubwayMapImv = (ImageView)mRootView.findViewById(R.id.subway_map_imv);
    }

    protected void setListener() {
        mInputBtn.setOnClickListener(this);
        mCityBtn.setOnClickListener(this);
        
       mTransPaddingView.setOnTouchListener(mDragViewExpanedOnTouchListener);
       mTransPaddingView.setOnClickListener(mTransPaddingOnClickListener);
       
       mSubCategoryGrid.setOnItemClickListener( mSubCategoryOnClickListener);
       mSubCategoryGrid.setOnTouchListener(mDragViewExpanedOnTouchListener);
       
       mMyLoactionTxv.setOnClickListener(this);
       
       mSubwayMapView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
       mSubwayMapBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.city_btn) {
            mActionLog.addAction(mActionTag + ActionLog.POIHomeChangeCityBtn, Globals.getCurrentCityInfo().getCName());
            mSphinx.showView(R.id.activity_more_change_city);
        } else if (id == R.id.input_btn) {
            mActionLog.addAction(mActionTag + ActionLog.POIHomeInputEdt);
            mSphinx.getPOIQueryFragment().reset();
            mSphinx.showView(R.id.view_poi_input_search);
        } else if (id == R.id.my_location_txv) {
            CityInfo myLoaction = Globals.g_My_Location_City_Info;
            if (myLoaction == null ||
                    Globals.getCurrentCityInfo(false).getId() != myLoaction.getId() ||
                    mPOI != null ||
                    mSelectedLocation) {
                mActionLog.addAction(mActionTag + ActionLog.POIHomeAppointedArea);
                setSelectedLocation(false);
                refreshLocationView(true);
                return;
            }
            mActionLog.addAction(mActionTag + ActionLog.POIHomeMyLocation);
            
            mSphinx.getPickLocationFragment().setInvoker(this);
            mSphinx.getPickLocationFragment().reset();
            DataQuery.initStaticField(BaseQuery.DATA_TYPE_POI, BaseQuery.SUB_DATA_TYPE_POI, Globals.getCurrentCityInfo(false).getId());
            FilterArea filterArea = DataQuery.getFilterArea();
            Filter filter = HotelHomeFragment.getFilter(mFilterList, FilterArea.FIELD_LIST);
            if (filterArea == null || filterArea.getVersion().equals("0.0.0")) {
                queryFilter();
                showProgressDialog();
            } else {
                if (filter == null) {
                    setDataToPickLocationFragment();
                }
                mSphinx.getPickLocationFragment().setTitle(mSphinx.getString(R.string.poi_change_location));
                mSphinx.showView(R.id.view_hotel_pick_location);
            }
        } else if (id == R.id.subway_map_btn) {
            if (mCurrentCategoryIndex == TRAFFIC_INDEX) {
                mActionLog.addAction(mActionTag + ActionLog.POIHomeSubwayMap);
                TKConfig.setPref(mSphinx, TKConfig.PREFS_SUBWAY_MAP, "1");
                mSphinx.getSubwayMapFragment().setData(Globals.getCurrentCityInfo());
                mSphinx.showView(R.id.view_subway_map);
            } else if (mCurrentCategoryIndex == FOOD_INDEX) {
                mActionLog.addAction(mActionTag + ActionLog.POIHomeDish);
                TKConfig.setPref(mSphinx, TKConfig.PREFS_DISH, "1");
                
                //TODO：搜索所有菜单商户
                jumpToPOIResult("菜单");
            }
            mSubwayMapImv.setVisibility(View.GONE);
        }
    }
    
    public void refreshCity(String cityName) {
        if (mTitleFragment == null) {
            return;
        }
        mCityBtn.setText(cityName);

        mSelectedLocation = false;
        mPOI = null;
        mMyLoactionTxv.setBackgroundResource(R.drawable.btn_my_location);
        refreshLocationView(true);
        HotelHomeFragment.deleteFilter(mFilterList, FilterArea.FIELD_LIST);
        refreshFilterArea();
        Filter filter = HotelHomeFragment.getFilter(mFilterList, FilterArea.FIELD_LIST);
        if (filter != null) {
            FilterListView.selectedFilter(filter, Integer.MIN_VALUE);
        }
    }
    
    public void refreshLocationView() {
        refreshLocationView(false);
    }
    
    public void refreshLocationView(boolean isUpdate) {
        CityInfo currentCityInfo = Globals.getCurrentCityInfo();
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        int categoryTop = mCategoryTop;
        if (myLocationCityInfo != null) {
            if (myLocationCityInfo.getId() == currentCityInfo.getId()) {
                refreshLoactionTxv(isUpdate);
                mMyLoactionTxv.setVisibility(View.VISIBLE);
                mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
            } else {
                if (mSelectedLocation == false) {
                    mMyLoactionTxv.setVisibility(View.GONE);
                    mCategoryTop = mCategoryPadding;
                }
            }
        } else {
            mLastTime = 0;
            if (mSelectedLocation == false) {
                mMyLoactionTxv.setBackgroundResource(R.drawable.bg_location);
                mMyLoactionTxv.setText(mContext.getString(R.string.location_doing));
                mMyLoactionTxv.setVisibility(View.VISIBLE);
                mCategoryTop = mMyLocationViewHeight+mCategoryPadding;
            }
        }
        if (categoryTop != mCategoryTop) {
            mCategoryAdapter.notifyDataSetChanged();
        }
        mSphinx.getDiscoverFragment().refreshLocationView(mLocationName, myLocationCityInfo);
    }
    
    private void refreshLoactionTxv(boolean isUpdate) {
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
        
        if (!TextUtils.isEmpty(name) && name.length() > 0) {
            if ((myLocation != null && myLocation.getProvider().equals(LocationManager.GPS_PROVIDER) && distance > 100)) {
                isUpdate = true;
            } else if (currentTime - mLastTime > 30*1000 && distance > 600) {
                isUpdate = true;
            } else if (mLastTime == 0) {
                isUpdate = true;
            } else if (!TextUtils.isEmpty(mLocationName) && !mLocationName.equals(name)) {
                isUpdate = true;
            }
        }
        
        if (isUpdate) {
            mLocationName = name;
            if (mSelectedLocation == false) {
                mMyLoactionTxv.setBackgroundResource(R.drawable.btn_my_location);
                mMyLoactionTxv.setText(mContext.getString(R.string.current_location, mLocationName));
            }
            mLastPosition = myLocationPosition;
            mLastTime = currentTime;
        }
            
    }
    
    private static class Category {
        int resId;
        String name;
    }
    
    public class CategoryAdapter extends ArrayAdapter<Category> {

        public CategoryAdapter(Context context, List<Category> list) {
            super(context, 0, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	
        	if(convertView == null){
        		convertView = mLayoutInflater.inflate(R.layout.poi_category_list_item, parent, false);
        	}
        	
        	//Setup category button
        	Button btnCategory = (Button) convertView.findViewById(R.id.btn_category);
        	btnCategory.setText(getItem(position).name);
        	Drawable subCategoryIcon = getContext().getResources().getDrawable(mCategoryResIdList[position]);
        	subCategoryIcon.setBounds(0, 0, subCategoryIcon.getIntrinsicWidth(), subCategoryIcon.getIntrinsicHeight());
        	btnCategory.setCompoundDrawables(subCategoryIcon, null, null, null);
        	
        	Button btnSubCategory = (Button) convertView.findViewById(R.id.btn_sub_category);
        	btnSubCategory.setText(mHighLightedSubs[position]);
        	
        	btnCategory.setOnClickListener(new CategoryBtnOnClickListener(position));
        	
        	//setup sub-category button
        	btnSubCategory.setOnClickListener(new SubCategoryOnclickListener(position));
        	btnSubCategory.setOnTouchListener(new SubCategoryBtnOnTouchListener(position));
        	if (position == 0) {
        	    convertView.setPadding(0, 0, 0, 0);
            } else if (position == getCount()-1){
                convertView.setPadding(0, 0, 0, mMyLocationViewHeight);
            } else {
                convertView.setPadding(0, 0, 0, 0);
            }
        	if (mSubCategoryListView.contains(btnSubCategory) == false) {
        	    mSubCategoryListView.add(btnSubCategory);
        	}
        	
        	ImageView icon = (ImageView) convertView.findViewById(R.id.hotel_tip_reserve_imv);
        	if (position == HOTEL_INDEX) {
        	    icon.setVisibility(View.VISIBLE);
        	    setHotelOrderAnimation(icon);
        	} else {
        		icon.setAnimation(null);
        	    icon.setVisibility(View.GONE);
        	}
        	
        	//Id will be used in setupDragView.
        	convertView.setId(position);
        	return convertView;
        }
    }

    private RotateAnimation anim;
    
    private void setHotelOrderAnimation(ImageView icon){
    	if(anim == null){
    		anim = new RotateAnimation(-20, 30, 12, 9);
    		anim.setDuration(1000);
    		anim.setRepeatCount(Integer.MAX_VALUE);
    		anim.setRepeatMode(Animation.REVERSE);
    		anim.setInterpolator(new AccelerateDecelerateInterpolator());
    	}
    	icon.startAnimation(anim);
    }
    
    private boolean mIsSubCategoryExpanded = false;
    
    public boolean isSubCategoryExpanded(){
    	return mIsSubCategoryExpanded;
    }
    
    class CategoryBtnOnClickListener implements OnClickListener {
    	
		private int position;
		
		public CategoryBtnOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			String keyWord = subCategories.get(position).get(0);
			mActionLog.addAction(mActionTag + ActionLog.POIHomeCategory, keyWord);
			jumpToPOIResult(keyWord);
		}
	};
    
	class SubCategoryOnclickListener implements OnClickListener{

		private int position;
		
		public SubCategoryOnclickListener(int position) {
			this.position = position;
		}
		
		@Override
		public void onClick(View v) {
		    
		    if (position == HOTEL_INDEX) {
		        mSphinx.getHotelHomeFragment().resetDate();
		        mSphinx.getHotelHomeFragment().setCityInfo(Globals.getCurrentCityInfo());
		        mSphinx.showView(R.id.view_hotel_home);
		        return;
		    }
			
			//if user is not dragging the view and dragView not expanded already, expand it.
			if(!isDragStarts && !mIsSubCategoryExpanded){
			    mActionLog.addAction(mActionTag + ActionLog.POIHomeSubcategoryOpenedOnClick, mCategorylist.get(mCurrentCategoryIndex));
			    
				//set it visible to guard first show of the drag view
				setUpDragView(position);

				mDragView.setVisibility(View.VISIBLE);
				
				//mTransPaddingView.setBackgroundResource(R.color.black_transparent_half);
				
				//animate the drag view
				animateDragView(true);
				
				//set expanded flag
				mIsSubCategoryExpanded = true;
			}
			
		}
	};
	
	private void animateDragView(boolean toLeft)
	{
		Animation animation = new TranslateAnimation(toLeft?mDragViewWidth:0, toLeft?0:mDragViewWidth, 0, 0);
		//animation.setInterpolator(new AccelerateDecelerateInterpolator());
		//animation.setInterpolator(new );
		animation.setStartOffset(0);
		animation.setDuration(toLeft?300:500);
		animation.setFillAfter(false);
		animation.setAnimationListener(mDragOpenAnimationListener);
		mDragView.startAnimation(animation);
	}
	
	OnClickListener mTransPaddingOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!isDragStarts){
				mActionLog.addAction(mActionTag + ActionLog.POIHomeSubcategoryClosedOnClick, mCategorylist.get(mCurrentCategoryIndex));
				closeDragView();
			}
		}
	};
	
	public void closeDragView(){
		//Animate close the drag view
		animateDragView(false);
		mTransPaddingView.setBackgroundResource(R.color.transparent);
		mIsSubCategoryExpanded = false;
		refreshSubCategoryListView();
	}
	
	void refreshSubCategoryListView() {
        for(int i = 0, size = mSubCategoryListView.size(); i < size; i++) {
            View view = mSubCategoryListView.get(i);
            view.setPressed(false);
            view.clearFocus();
            view.postInvalidate();
        }
	}
	
	OnTouchListener mDragViewExpanedOnTouchListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			onDragViewCloseTouchEvent(v, event, -1);
			return isDragging;
			
		}
	};
	
	private int[] touchedControlLoc = new int[2];
	
	private int xDown;
	private int yDown;

	boolean isDragStarts = false;
	boolean isDragging = false;
	boolean isPreventDrag = false;
	long lastTouchUpTimeMillis = 0;
	int minDragDistance = Util.dip2px(Globals.g_metrics.density, 10);
	List<View> mSubCategoryListView = new ArrayList<View>();
	
	private boolean onDragViewCloseTouchEvent(View v, MotionEvent event, int position){
		
		int x = (int) event.getX();
		int y = (int) event.getY();

		v.getLocationInWindow( touchedControlLoc );
		
		switch (event.getAction() & MotionEvent.ACTION_MASK) {

		case MotionEvent.ACTION_DOWN:
			//To prevent Category from intercept touch event
			//When down is for sub-cate button
			if( v instanceof Button && v!=mTransPaddingView){
				mCategoryLsv.requestDisallowInterceptTouchEvent(true);
			}
			
			xDown = x;
			yDown = y;
			
			isPreventDrag = isDragging = isDragStarts = false;
			
			break;

		case MotionEvent.ACTION_MOVE:
			if(isPreventDrag){
				break;
			}
			
			if( !isDragStarts){
				if( (v instanceof Button && v != mTransPaddingView)
						|| (v==mTransPaddingView && x > v.getWidth())		//the touch point has moved out of the TransPadding view range
						|| (v instanceof GridView) && (xDown + touchedControlLoc[0] < mScreenWidth*2/3) ) //the touch point has moved more than 10 pixels
				{
					if(( ( v instanceof Button && v!=mTransPaddingView) || v == mSubCategoryGrid)
						&& Math.abs(yDown - y) > minDragDistance){
						
						isPreventDrag = true;
						
						if(v instanceof Button && v!=mTransPaddingView){
							mCategoryLsv.requestDisallowInterceptTouchEvent(false);
						}
						
					}
					if(!isPreventDrag && 
							( (!mIsSubCategoryExpanded && xDown - x > minDragDistance) 
								|| (mIsSubCategoryExpanded && x -xDown > minDragDistance)) ){

						isDragStarts = true;

		                if (position == HOTEL_INDEX) {
		                    mSphinx.getHotelHomeFragment().resetDate();
		                    mSphinx.getHotelHomeFragment().setCityInfo(Globals.getCurrentCityInfo());
		                    mSphinx.showView(R.id.view_hotel_home);
		                    return true;
		                }
						isDragging = true;
						mTransPaddingView.setBackgroundResource(R.color.transparent);
						moveDragView(x, y);
						if(v == mSubCategoryGrid){
							mSubCategoryGrid.onTouchEvent(
									MotionEvent.obtain(event.getDownTime(), event.getEventTime(), 
											MotionEvent.ACTION_UP, 0, 0, event.getMetaState()));
						}
					}
					
				}
				
			}else {	//put dragging in else to prevent a flash of the dragView when the drag starts
				mDragView.setVisibility(View.VISIBLE);
				moveDragView(x,y);
			}
			
			

			break;

		case MotionEvent.ACTION_UP:
			if(!isDragStarts){
				break;
			}
			
			lastTouchUpTimeMillis = System.currentTimeMillis();
			x += touchedControlLoc[0];

			mDragView.layout(0, 0, mDragViewWidth, mDragViewHeight);
			
			if(mIsSubCategoryExpanded && x < mScreenWidth/3 
					|| !mIsSubCategoryExpanded && x < mScreenWidth*2/3 ){
				mDragView.setVisibility(View.VISIBLE);
				animateTranspadding(0, 1);
				if(mIsSubCategoryExpanded == false){
					mActionLog.addAction(mActionTag + ActionLog.POIHomeSubcategoryOpenedOnFling, mCategorylist.get(mCurrentCategoryIndex));
				}
				mIsSubCategoryExpanded= true;
			}else{
			    refreshSubCategoryListView();
				mDragView.setVisibility(View.INVISIBLE);
				mTransPaddingView.setBackgroundResource(R.color.transparent);
				if(mIsSubCategoryExpanded == true){
					mActionLog.addAction(mActionTag + ActionLog.POIHomeSubcategoryClosedOnFling, mCategorylist.get(mCurrentCategoryIndex));
				}
				mIsSubCategoryExpanded = false;
			}
			
			mCategoryLsv.setBackgroundResource(R.color.home_list_bg);
			isDragging = false;
			
			break;
			
			default:
				break;
		}

		return isDragStarts;
	}
    
	private void moveDragView(int x, int y){
		int dragViewLeft = 0;
		
		x += touchedControlLoc[0] - mDragViewParentX ;
		
		if(x>mTransPaddingWidth){
			dragViewLeft = x - mTransPaddingWidth;
		}else{
			dragViewLeft = 0;
		}
		mDragView.layout(dragViewLeft, 0, dragViewLeft + mDragViewWidth, mDragViewHeight);
		mDragView.invalidate();
	}
	
	class SubCategoryBtnOnTouchListener implements OnTouchListener {

		private int position;
		
		public SubCategoryBtnOnTouchListener(int position) {
			this.position = position;
		}
		
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			boolean oldIsDragStart=isDragging;
			
			
			boolean result = onDragViewCloseTouchEvent(v, event, position);
			
			if(!oldIsDragStart && isDragging){
				setUpDragView(position);
			}
			
			return mIsSubCategoryExpanded;
			
		}
	};

	private int mCurrentCategoryIndex = 0;

	void setUpDragView(int position)
	{

		mCurrentCategoryIndex = position;
		
		mSubCategoryAdapter.clear();
		ArrayList<String> subs = subCategories.get(position);
		if (position == TRAFFIC_INDEX && MapEngine.checkSupportSubway(Globals.getCurrentCityInfo().getId())) {
            mSubwayMapBtn.setText(R.string.subway_map);
            Drawable drawable = mSphinx.getResources().getDrawable(R.drawable.ic_subway_map);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            mSubwayMapBtn.setCompoundDrawables(drawable, null, null, null);
            mSubwayMapView.setVisibility(View.VISIBLE);
            if (TKConfig.getPref(mSphinx, TKConfig.PREFS_SUBWAY_MAP) == null) {
                mSubwayMapImv.setVisibility(View.VISIBLE);
            } else {
                mSubwayMapImv.setVisibility(View.GONE);
            }
		} else if (position == FOOD_INDEX) {
		    mSubwayMapBtn.setText(R.string.dish_merchant);
		    Drawable drawable = mSphinx.getResources().getDrawable(R.drawable.ic_dish);
		    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		    mSubwayMapBtn.setCompoundDrawables(drawable, null, null, null);
		    mSubwayMapView.setVisibility(View.VISIBLE);
            if (TKConfig.getPref(mSphinx, TKConfig.PREFS_DISH) == null) {
                mSubwayMapImv.setVisibility(View.VISIBLE);
            } else {
                mSubwayMapImv.setVisibility(View.GONE);
            }
		} else {
		    mSubwayMapView.setVisibility(View.GONE);
		}

		mSubCategoryAdapter.add(subs.get(0) + mContext.getString(R.string.all));
		for(int i=1, size=subs.size(); i<size; i++){
			mSubCategoryAdapter.add(subs.get(i));
		}
		mSubCategoryGrid.setAdapter(mSubCategoryAdapter);
		mCategoryTagImv.setImageResource(mCategoryTagResIdList[position]);
		
		/*
		 * Animate the tag
		 */
		View tagView = mRootView.findViewById(R.id.category_tag_view);
		View item = mCategoryLsv.findViewById(position);
		View normalItem = item;
		if(position == mCategoryAdapter.getCount()-1){
			normalItem = mCategoryLsv.findViewById(position-1);
		}
		int tagY = (int) (item.getTop() + normalItem.getHeight()/2) - tagView.getHeight()/2;
		
		Animation anim = new TranslateAnimation(0, 0, 0, tagY);
		anim.setDuration(300);
		anim.setFillAfter(true);
		tagView.startAnimation(anim);
		
	}
    
	OnItemClickListener mSubCategoryOnClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (animation || isDragging) {
				return;
			}
			if(isDragStarts && System.currentTimeMillis() < lastTouchUpTimeMillis + 200){
				return;
			}
			String keyWord=mSubCategoryAdapter.getItem(position);
			if(position==0){
				keyWord = subCategories.get(mCurrentCategoryIndex).get(0);
			}
			mActionLog.addAction(mActionTag + ActionLog.POIHomeSubcategoryPressed, keyWord);
			jumpToPOIResult(keyWord);
		}
		
	};
	
	private void jumpToPOIResult(String keyWord){
       POI requestPOI = mSphinx.getPOI();
       if (mPOI != null) {
           requestPOI = mPOI;
       }
       int cityId = Globals.getCurrentCityInfo().getId();
       DataQuery poiQuery = getDataQuery();
       poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyWord);
       poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INFO, DataQuery.INFO_TYPE_TAG);
       poiQuery.setup(cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
       BaseFragment baseFragment = mSphinx.getFragment(poiQuery.getTargetViewId());
       
       if (baseFragment != null && baseFragment instanceof POIResultFragment) {
    	   mSphinx.queryStart(poiQuery);
    	   ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup();
    	   mSphinx.showView(poiQuery.getTargetViewId());
       	}
	}
	
	public DataQuery getDataQuery() {
	       POI requestPOI = mSphinx.getPOI();
	       DataQuery poiQuery = new DataQuery(mContext);
	       poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
	       poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
	       poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
	       Position position = requestPOI.getPosition();
	       if (position != null) {
	           poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
	           poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
	        }
	       if (mPOI != null) {
	           requestPOI = mPOI;
	           position = mPOI.getPosition();
	           poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
	           poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
                   poiQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
	       } else if (mSelectedLocation) {
	           Filter[] filters = FilterListView.getSelectedFilter(HotelHomeFragment.getFilter(mFilterList, FilterArea.FIELD_LIST));
	           if (filters != null) {
	               StringBuilder s = new StringBuilder();
	               s.append(Util.byteToHexString(FilterArea.FIELD_LIST));
	               s.append(':');
	               for(int i = 0, length = filters.length; i < length; i++) {
	                   if (i > 0) {
	                       s.append('_');
	                   }
	                   s.append(filters[i].getFilterOption().getName());
	               }
	               poiQuery.addParameter(DataQuery.SERVER_PARAMETER_FILTER_STRING, s.toString());
	           }
	       }
	       
	       return poiQuery;
	}
	
	boolean animation = false;
	AnimationListener mDragOpenAnimationListener = new AnimationListener() {
		
		@Override
		public void onAnimationStart(Animation animation) {
			POIHomeFragment.this.animation = true;
		}
		
		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
			if ( !mIsSubCategoryExpanded){
			    refreshSubCategoryListView();
				mDragView.setVisibility(View.INVISIBLE);
			}else{
				mDragView.setVisibility(View.VISIBLE);
				animateTranspadding(0,1);
			}
			POIHomeFragment.this.animation = false;
		}
	};
    
	
	private void animateTranspadding(final float fromAlpha, final float toAlpha){
		
		mTransPaddingView.setBackgroundResource(R.color.black_transparent_half);
		Animation ani = new AlphaAnimation(fromAlpha, toAlpha);
		ani.setDuration(200);
		ani.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				if(toAlpha > fromAlpha){
					mTransPaddingView.setBackgroundResource(R.color.black_transparent_half);
				}
			}
		});
		mTransPaddingView.startAnimation(ani);
		
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                isSubCategoryExpanded()) {
            mActionLog.addAction(mActionTag + ActionLog.POIHomeSubcategoryClosedOnBack, mCategorylist.get(mCurrentCategoryIndex));
            closeDragView();
            return true;
        }
        return super.onKeyDown(keyCode, event);
	}
}

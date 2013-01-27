/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.DiscoverResponse;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverConfigList;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverConfigList.DiscoverConfig;
import com.tigerknows.model.DataQuery.DiscoverResponse.DiscoverCategoryList.DiscoverCategory;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.TKAsyncTask;

/**
 * @author Peng Wenyue
 */
public class DiscoverFragment extends DiscoverBaseFragment {
    
    public DiscoverFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    static final String TAG = "DiscoverFragment";
    
    public static final float DISCOVER_WIDHT_RATE = 0.85f;

    private TextView mMyLoactionTxv;
    
    private TKGallery mViewPager;
    
    private ViewGroup mOverView;
    
    private List<DiscoverCategory> mDiscoverCategoryList = new ArrayList<DiscoverCategory>();
    
    private DiscoverCategoryAdapter mDiscoverCategoryAdapter;
    
    private DiscoverTopIndicator mDiscoverTopIndicator;

    private TextView mNoSupportTitleTxv;
    
    private TextView mNoSupportMessageTxv;
    
    private int mCityId = -1;
    
    private DataQuery mDataQuery = null;
    
    private boolean mSupport = true;
    
    private Rect rect = new Rect();
    
    private int screenWidth;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.DiscoverHome;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        DataQuery.initStaticField(DataQuery.DATA_TYPE_DISCOVER, Globals.g_Current_City_Info.getId());
        mRootView = mLayoutInflater.inflate(R.layout.discover, container, false);
        screenWidth = (Globals.g_metrics.widthPixels > Globals.g_metrics.heightPixels ? Globals.g_metrics.heightPixels : Globals.g_metrics.widthPixels);
        
        findViews();        
        setListener();

        initViews();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMenuFragment.updateMenuStatus(R.id.discover_btn);
        mTitleFragment.hide();
        mMenuFragment.display();
        mSphinx.getHomeFragment().refreshLocationView();
        if (mCityId != Globals.g_Current_City_Info.getId()) {
            initViews();
        }
        int count = mViewPager.getChildCount();
        for(int i =0; i < count; i ++) {
            ((DiscoverCategoryView) (mViewPager.getChildAt(i))).onResume();
        }
        if (isReLogin()) {
            return;
        }
        refreshData();
        if (mViewPager.getVisibility() == View.VISIBLE && mDiscoverCategoryAdapter != null) {
            try {
            int position = mViewPager.getSelectedItemPosition();
            int size = mDiscoverCategoryAdapter.list.size();
            String leftLeftStr = mDiscoverCategoryAdapter.listView.get((position-2)%size).getTitleText();
            String leftStr = mDiscoverCategoryAdapter.listView.get((position-1)%size).getTitleText();
            String centerStr = mDiscoverCategoryAdapter.listView.get((position)%size).getTitleText();
            String rightStr = mDiscoverCategoryAdapter.listView.get((position+1)%size).getTitleText();
            String rightRightStr = mDiscoverCategoryAdapter.listView.get((position+2)%size).getTitleText();
            mDiscoverTopIndicator.onPageSelected(position, leftStr, centerStr, rightStr, leftLeftStr, rightRightStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mTkAsyncTasking != null) {
            mTkAsyncTasking.stop();
        }
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        int count = mViewPager.getChildCount();
        for(int i =0; i < count; i ++) {
            ((DiscoverCategoryView) (mViewPager.getChildAt(i))).dismiss();
        }
    }
    
    protected void findViews() {
        mViewPager = (TKGallery)mRootView.findViewById(R.id.scroll_screen);
        mOverView = (ViewGroup) mRootView.findViewById(R.id.over_view);
        mDiscoverTopIndicator = (DiscoverTopIndicator)mRootView.findViewById(R.id.discover_top_indicator);
        mMyLoactionTxv = (TextView) mRootView.findViewById(R.id.my_location_txv);
        mNoSupportTitleTxv = (TextView)mRootView.findViewById(R.id.no_support_title_txv);
        mNoSupportMessageTxv = (TextView)mRootView.findViewById(R.id.no_support_message_txv);
    }
    
    private class MySimpleGesture extends GestureDetector.SimpleOnGestureListener {   
        public boolean onDoubleTap(MotionEvent e) {   
            return super.onDoubleTap(e);   
        }   
           
        public boolean onDoubleTapEvent(MotionEvent e) {   
            return super.onDoubleTapEvent(e);   
        }   
           
        public boolean onDown(MotionEvent e) {   
            return super.onDown(e);   
        }   
           
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);   
        }   
           
        public void onLongPress(MotionEvent e) {   
            super.onLongPress(e);   
        }   
           
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {   
            return super.onScroll(e1, e2, distanceX, distanceY);   
        }   
           
        public void onShowPress(MotionEvent e) {   
            super.onShowPress(e);   
        }   
  
        public boolean onSingleTapConfirmed(MotionEvent e) {   
            return super.onSingleTapConfirmed(e);   
        }   
        public boolean onSingleTapUp(MotionEvent e) {   
            return true;   
        }   
    }

    private GestureDetector mGestureDetector;

    boolean isBegingDrag=false;
    float lastx = 0;
    float downx = 0;
    int mPosition = -1;
    int mSelectPosition = -1;
    protected void setListener() {
        mOverView.setClickable(true);
        mGestureDetector = new GestureDetector(mSphinx, new MySimpleGesture());  
        mOverView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent ev) {
                int x = (int)ev.getX();
                int y = (int)ev.getY();
                int action = ev.getAction() & MotionEvent.ACTION_MASK;
                if (mGestureDetector.onTouchEvent(ev) && mSupport && rect.contains(x, y)) {
                    int position = mViewPager.getSelectedItemPosition();
                    DiscoverCategory discoverCategory = mDiscoverCategoryList.get(position
                            % mDiscoverCategoryList.size());
                    if (discoverCategory.getNumCity() > 0 || discoverCategory.getNumNearby() > 0) {
                        mActionLog.addAction(ActionLog.DiscoverHomeSelectItem, discoverCategory.getType());
                        DataQuery dataQuery = new DataQuery(mSphinx);
                        Hashtable<String, String> criteria = new Hashtable<String, String>();
                        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, discoverCategory.getType());
                        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
                        dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(),
                                R.id.view_discover, R.id.view_discover_list, null, false, false,
                                mSphinx.getPOI());
                        mSphinx.queryStart(dataQuery);
                        mSphinx.getDiscoverListFragment().setup();
                        mSphinx.showView(R.id.view_discover_list);
                        mTitleBtn.setClickable(false);
                        isBegingDrag = false;
                        return false;
                    }
                }
                mViewPager.onTouchEvent(ev);
                if (action == MotionEvent.ACTION_DOWN) {
                    if (isBegingDrag == false) {
                        mSelectPosition = -1;
                        downx = ev.getX();
                    }
                    lastx = ev.getX();
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (isBegingDrag == false) {
                        downx = ev.getX();
                        downx = lastx;
                        isBegingDrag = true;
                        mSelectPosition = mViewPager.getFirstVisiblePosition();
                    }

                    lastx = ev.getX();
                    final int widthWithMargin = (int) (screenWidth*DISCOVER_WIDHT_RATE);
                    final int offsetPixels = ((int)Math.abs(lastx-downx)) % ((int) screenWidth);
                    final float offset = (float) (offsetPixels > widthWithMargin ? widthWithMargin : offsetPixels) / widthWithMargin;
                    mDiscoverTopIndicator.onPageScrolled((int)(lastx-downx), offset, (int)(offsetPixels*0.5));
                } else if (action == MotionEvent.ACTION_UP) {
                    isBegingDrag = false;
                    mSelectPosition = -1;
                    downx = lastx;
                    if (mDiscoverCategoryAdapter != null) {
                        try {
                        int size = mDiscoverCategoryAdapter.list.size();
                        int position = mViewPager.getSelectedItemPosition();
                        String leftLeftStr = mDiscoverCategoryAdapter.listView.get((position-2)%size).getTitleText();
                        String leftStr = mDiscoverCategoryAdapter.listView.get((position-1)%size).getTitleText();
                        String centerStr = mDiscoverCategoryAdapter.listView.get((position)%size).getTitleText();
                        String rightStr = mDiscoverCategoryAdapter.listView.get((position+1)%size).getTitleText();
                        String rightRightStr = mDiscoverCategoryAdapter.listView.get((position+2)%size).getTitleText();
                        mDiscoverTopIndicator.onPageSelected(position, leftStr, centerStr, rightStr, leftLeftStr, rightRightStr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return false;
            }
        });
        mViewPager.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (isBegingDrag == false) {
                    if (mPosition != -1) {
                        if (position > mPosition) {
                            mActionLog.addAction(ActionLog.DiscoverHomeRightFling);
                        } else {
                            mActionLog.addAction(ActionLog.DiscoverHomeLeftFling);
                        }
                    }
                    mPosition = position;
                    if (mDiscoverCategoryAdapter != null) {
                        try {
                        int size = mDiscoverCategoryAdapter.list.size();
                        String leftLeftStr = mDiscoverCategoryAdapter.listView.get((position-2)%size).getTitleText();
                        String leftStr = mDiscoverCategoryAdapter.listView.get((position-1)%size).getTitleText();
                        String centerStr = mDiscoverCategoryAdapter.listView.get((position)%size).getTitleText();
                        String rightStr = mDiscoverCategoryAdapter.listView.get((position+1)%size).getTitleText();
                        String rightRightStr = mDiscoverCategoryAdapter.listView.get((position+2)%size).getTitleText();
                        mDiscoverTopIndicator.onPageSelected(position, leftStr, centerStr, rightStr, leftLeftStr, rightRightStr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
                
            }
        });
//        mViewPager.setOnItemClickListener(new OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapter, View arg1, int position, long arg3) {
//                DiscoverCategory discoverCategory = mDiscoverCategoryList.get(position%mDiscoverCategoryList.size());
//                if (discoverCategory.getNumCity() == 0 && discoverCategory.getNumNearby() == 0) {
//                    return;
//                }
//                DataQuery dataQuery = new DataQuery(mSphinx);
//                Hashtable<String, String> criteria = new Hashtable<String, String>();
//                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, discoverCategory.getType());
//                criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
//                dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(), R.id.view_discover,
//                        R.id.view_discover_list, null, false, false, mSphinx.getPOI());
//                mSphinx.queryStart(dataQuery);
//                mSphinx.getDiscoverListFragment().setup();
//                mSphinx.showView(R.id.view_discover_list);
//            }
//        });
    }
    
//    private void onPageSelected(boolean right) {
//        if (mDiscoverCategoryAdapter != null && mSelectPosition != -1) {
//            try {
//            int size = mDiscoverCategoryAdapter.list.size();
//            int position;
//            if (right == false) {
//                position = mSelectPosition+1;
//            } else {
//                position = mSelectPosition-1;
//            }
//            String leftStr = mDiscoverCategoryAdapter.listView.get((position)%size).getTitleText();
//            String centerStr = mDiscoverCategoryAdapter.listView.get((position+1)%size).getTitleText();
//            String rightStr = mDiscoverCategoryAdapter.listView.get((position+2)%size).getTitleText();
//            mDiscoverTopIndicator.onPageSelected(position, leftStr, centerStr, rightStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
    
    public void refreshLocationView(String locationName, CityInfo myLocationCityInfo) {
        CityInfo currentCityInfo = Globals.g_Current_City_Info;
        if (myLocationCityInfo != null) {
            if (myLocationCityInfo.getId() == currentCityInfo.getId()) {
                mMyLoactionTxv.setText(mContext.getString(R.string.current_location, TextUtils.isEmpty(locationName) ? "" : locationName.substring(1)));
                mMyLoactionTxv.setVisibility(View.VISIBLE);
            } else {
                mMyLoactionTxv.setVisibility(View.GONE);
            }
        } else {
            mMyLoactionTxv.setText(mContext.getString(R.string.location_doing));
            mMyLoactionTxv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        DiscoverResponse response = (DiscoverResponse) dataQuery.getResponse();
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        }

        try {
            for(int i = mDiscoverCategoryList.size()-1; i >= 0; i--) {
                mDiscoverCategoryList.get(i).init(new XMap());
            }
            if (mDiscoverCategoryAdapter != null) {
                for(int i = 0, size = mDiscoverCategoryAdapter.getList().size(); i < size; i++) {
                    DiscoverCategory discoverCategory = getDiscoverCategoryByType(mDiscoverCategoryList, mDiscoverCategoryAdapter.getList().get(i).getType());
                    mDiscoverCategoryAdapter.getListView().get(i).setData(discoverCategory);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (response != null) {
            if (response.getResponseCode() == Response.RESPONSE_CODE_DISCOVER_NO_SUPPORT){
                noSupportDiscover();
                return;
            } else if (response.getResponseCode() != Response.RESPONSE_CODE_OK) {
                return;
            }
        } else {
            return;
        }
        
        
        DiscoverConfigList configList = response.getConfigList();
        if (configList != null && configList.getList() != null && configList.getList().size() > 0) {
            initViews();
        }
        
        DiscoverCategoryList discoverCategoryList = response.getDiscoverCategoryList();
        if (discoverCategoryList != null && discoverCategoryList.getList() != null && discoverCategoryList.getList().size() > 0) {
            List<DiscoverCategory> list = discoverCategoryList.getList();
            try {
                for(int i = 0, size = mDiscoverCategoryAdapter.getList().size(); i < size; i++) {
                    DiscoverCategory discoverCategory = mDiscoverCategoryAdapter.getList().get(i);
                    DiscoverCategory discoverCategoryNow = getDiscoverCategoryByType(list, discoverCategory.getType());
                    discoverCategory.init(discoverCategoryNow.getData());
                    mDiscoverCategoryAdapter.getListView().get(i).setData(discoverCategory);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mDataQuery = dataQuery;
    }
    
    private void initViews() {
        mDataQuery = null;
        DiscoverConfigList discoverConfigList = DataQuery.getDiscoverConfigList();

        CityInfo cityInfo = Globals.g_Current_City_Info;
        mCityId = cityInfo.getId();
        if (discoverConfigList != null) {
            List<DiscoverConfig> list = discoverConfigList.getList();
            if (list != null) {
                for(DiscoverConfig discoverConfig : list) {
                    if (discoverConfig.getSeqId() == mCityId) {
                        mDiscoverCategoryList = new ArrayList<DiscoverCategory>();
                        List<Long> valueList = discoverConfig.getList(); 
                        long value = Long.parseLong(BaseQuery.DATA_TYPE_TUANGOU);
                        if (valueList.contains(value)) {
                            DiscoverCategory discoverCategory = new DiscoverCategory(value);
                            mDiscoverCategoryList.add(discoverCategory);
                        }
                        value = Long.parseLong(BaseQuery.DATA_TYPE_DIANYING);
                        if (valueList.contains(value)) {
                            DiscoverCategory discoverCategory = new DiscoverCategory(value);
                            mDiscoverCategoryList.add(discoverCategory);
                        }
                        value = Long.parseLong(BaseQuery.DATA_TYPE_YANCHU);
                        if (valueList.contains(value)) {
                            DiscoverCategory discoverCategory = new DiscoverCategory(value);
                            mDiscoverCategoryList.add(discoverCategory);
                        }
                        value = Long.parseLong(BaseQuery.DATA_TYPE_ZHANLAN);
                        if (valueList.contains(value)) {
                            DiscoverCategory discoverCategory = new DiscoverCategory(value);
                            mDiscoverCategoryList.add(discoverCategory);
                        }
                        if (mDiscoverCategoryList.size() > 0) {
                            mNoSupportTitleTxv.setVisibility(View.GONE);
                            mNoSupportMessageTxv.setVisibility(View.GONE);
                            mViewPager.setVisibility(View.VISIBLE);
                            mDiscoverTopIndicator.setVisibility(View.VISIBLE);
                            
                            mDiscoverCategoryAdapter = new DiscoverCategoryAdapter(mDiscoverCategoryList);
                            mViewPager.setAdapter(mDiscoverCategoryAdapter);
                            mDiscoverCategoryAdapter.notifyDataSetChanged();
                            mDiscoverTopIndicator.setGallery(mViewPager);
                            mViewPager.setSelection(mDiscoverCategoryList.size()*100);
                            mSupport = true;
                            return;
                        }
                    }
                }
            }
        }
        
        // 此城市不支持发现
        noSupportDiscover();
    }
    
    private void noSupportDiscover() {
        mSupport = false;
        CityInfo cityInfo = Globals.g_Current_City_Info;
        mDiscoverTopIndicator.setVisibility(View.GONE);
        mViewPager.setVisibility(View.GONE);
        mNoSupportTitleTxv.setVisibility(View.VISIBLE);
        mNoSupportMessageTxv.setText(mSphinx.getString(R.string.discover_no_support_tip, cityInfo.getCName()));
        mNoSupportMessageTxv.setVisibility(View.VISIBLE);
    }

    public class DiscoverCategoryAdapter extends BaseAdapter {
        
        List<DiscoverCategory> list;
        List<DiscoverCategoryView> listView = new ArrayList<DiscoverCategoryView>();
        
        public DiscoverCategoryAdapter(List<DiscoverCategory> list) {
            this.list = list;
            int width = (int) (screenWidth*DISCOVER_WIDHT_RATE);
            rect.left = (int) (screenWidth*(1-DISCOVER_WIDHT_RATE))/2;
            rect.right = rect.left+width;
            int size = list.size();
            if (size > 0) {
                if (size == 1) {
                    list.add(list.get(0));
                    list.add(list.get(0));
                } else if (size == 2) {
                    list.add(list.get(0));
                    list.add(list.get(1));
                }
            }
            for(DiscoverCategory discoverCategory : list) {
                DiscoverCategoryView discoverCategoryView = new DiscoverCategoryView(mSphinx);
                discoverCategoryView.setup(discoverCategory.getType());
                listView.add(discoverCategoryView);
                ViewGroup viewGroup = (ViewGroup) discoverCategoryView.getChildAt(0);
                ViewGroup.LayoutParams layoutParams = viewGroup.getLayoutParams();
                layoutParams.width = width;
                viewGroup.findViewById(R.id.picture_imv).getLayoutParams().height = (int) ((width-Util.dip2px(Globals.g_metrics.density, 48)) * ((float) 191.0/215.0));
                int height = Globals.g_metrics.heightPixels - Util.dip2px(Globals.g_metrics.density, 96);
                viewGroup.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int actualHeight = viewGroup.getMeasuredHeight();
                rect.top = (height - actualHeight)/2;
                rect.bottom = actualHeight+rect.top;
            }
        }

        @Override
        public int getCount() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View arg1, ViewGroup arg2) {
            // TODO Auto-generated method stub
            return listView.get(position%listView.size());
        }
        
        public List<DiscoverCategory> getList() {
            return list;
        }

        public List<DiscoverCategoryView> getListView() {
            return listView;
        }
        
    }
    
    private void refreshData() {
        if (mDataQuery == null || mDataQuery.getCityId() != mCityId) {
            DataQuery dataQuery = new DataQuery(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DISCOVER);
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(), R.id.view_discover, R.id.view_discover, null, false, true, mSphinx.getPOI());
            mSphinx.queryStart(dataQuery);
        }
    }
    
    public void resetDataQuery() {
        this.mDataQuery = null;
    }
    
    public void setCurrentItem(int position) {
        mViewPager.setSelection(position+(128*mDiscoverCategoryList.size()));
    }
    
    private static DiscoverCategory getDiscoverCategoryByType(List<DiscoverCategory> list, String type) {
        if (list == null || TextUtils.isEmpty(type)) {
            return null;
        }
        for(int i = list.size()-1; i >= 0; i--) {
            if (type.equals(list.get(i).getType())) {
                return list.get(i);
            }
        }
        return null;
    }
    
    public List<DiscoverCategory> getDiscoverCategoryList() {
        return mDiscoverCategoryList;
    }
}

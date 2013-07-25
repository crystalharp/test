/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.hotel.NavigationWidget;
import com.tigerknows.ui.more.AddMerchantActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class POIResultFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack, RetryView.CallBack {

    public POIResultFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private FilterListView mFilterListView = null;
    
    private String mTitleText;

    private ViewGroup mFilterControlView = null;
    
    private SpringbackListView mResultLsv = null;

    private QueryingView mQueryingView = null;
    
    private View mLoadingView = null;
    
    private View mAddMerchantFootView = null;
    
    private View mCurrentFootView;
    
    private View mAddMerchantView = null;
    
    private View mAddMerchantItemView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private RetryView mRetryView;
    
    private NavigationWidget mNavigationWidget;
    
    private POIAdapter mResultAdapter = null;
    
    private DataQuery mDataQuery;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
    private String mInputText;
    
    private long mATotal;
    
    private long mBTotal;
    
    private boolean mShowAPOI = false;
    
    private List<Filter> mFilterList = new ArrayList<Filter>();
    
    static final int STATE_QUERYING = 0;
    static final int STATE_ERROR = 1;
    static final int STATE_EMPTY = 2;
    static final int STATE_LIST = 3;
    
    private int mState = STATE_QUERYING;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                mResultLsv.getView(false).performClick();
            }
        }
    };
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mSphinx.getHandler().removeCallbacks(mActualLoadedDrawableRun);
            mSphinx.getHandler().post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            if (mSphinx.uiStackPeek() == getId() && mSphinx.isFinishing() == false) {
                mResultAdapter.notifyDataSetChanged();
            }
        }
    };
    
    public List<POI> getPOIList() {
        return mPOIList;
    }
    
    public void setup() {
        setup(null);
    }
    
    public void setup(String inputText) {
        mInputText = inputText;
        
        DataQuery lastDataQuerying = (DataQuery) this.mTkAsyncTasking.getBaseQuery();
        if (lastDataQuerying == null) {
            return;
        }
        
        if (lastDataQuerying.getSourceViewId() != getId()) {
            mDataQuery = null;
            mFilterControlView.setVisibility(View.GONE);
            mFilterList.clear();
        }

        POI poi = lastDataQuerying.getPOI();
        int sourceType = poi.getSourceType();
        String str;
        if (sourceType == POI.SOURCE_TYPE_MY_LOCATION) {
            str = mContext.getString(R.string.searching);
        } else if (sourceType == POI.SOURCE_TYPE_CITY_CENTER) {
            str = mContext.getString(R.string.at_city_searching, mSphinx.getMapEngine().getCityInfo(lastDataQuerying.getCityId()).getCName());
        } else {
            str = mContext.getString(R.string.at_location_searching);
        }
        mQueryingTxv.setText(str);
        mTitleText = mSphinx.getString(R.string.searching_title);
        
        this.mState = STATE_QUERYING;
        updateView();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_result, container, false);

        findViews();
        setListener();
        
        mResultAdapter = new POIAdapter(mSphinx, mPOIList, mLoadedDrawableRun, POIResultFragment.this.toString());
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        if (mPOIList != null) {
            mPOIList.clear();
            mResultAdapter.notifyDataSetChanged();
        }
        mDataQuery = null;
    }
    
    protected void findViews() {
        mFilterControlView = (ViewGroup)mRootView.findViewById(R.id.filter_control_view);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        mLoadingView = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(mLoadingView);
        mCurrentFootView = mLoadingView;
        mAddMerchantFootView = mLayoutInflater.inflate(R.layout.poi_list_item_add_merchant, null);
        mAddMerchantView = mRootView.findViewById(R.id.add_merchant_view);
        mAddMerchantItemView = mRootView.findViewById(R.id.add_merchant_item_view);
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
        mNavigationWidget = (NavigationWidget) mRootView.findViewById(R.id.navigation_widget);
    }

    protected void setListener() {
        mRetryView.setCallBack(this, mActionTag);
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    POI poi = (POI) adapterView.getAdapter().getItem(position);
                    if (poi != null) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, poi.getUUID(), poi.getName());
                        mSphinx.showView(R.id.view_poi_detail);
                        mSphinx.getPOIDetailFragment().setData(poi, position);
                    } else if (mResultLsv.isFooterSpringback() == false) {
                        showAddMerchant();
                    }
                } 
            }
            
        });

        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader) {
                    return;
                }
                turnPage();
            }
        });
        
        mAddMerchantItemView.setOnClickListener(this);
    }
    
    void showAddMerchant() {
        mActionLog.addAction(mActionTag + ActionLog.POIListAddMerchanty);
        Intent intent = new Intent();
        if (TextUtils.isEmpty(mInputText) == false) {
            intent.putExtra(AddMerchantActivity.EXTRA_INPUT_TEXT, mInputText);
        }
        mSphinx.showView(R.id.activity_more_add_merchant, intent);
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            DataQuery lastDataQuery = mDataQuery;
            if (mState != STATE_QUERYING && mState != STATE_LIST && lastDataQuery != null) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                mState = STATE_LIST;
                updateView();
                refreshFilter(lastDataQuery.getFilterList());
                refreshResultTitleText(lastDataQuery);
                return true;
            }
        }
        return false;
    }
    
    private void refreshResultTitleText(DataQuery dataQuery) {
        String str = mContext.getString(R.string.no_result);
        if (dataQuery != null) {
            POIResponse poiModel = (POIResponse)dataQuery.getResponse();
            if (poiModel != null) {
                POIList poiList = poiModel.getAPOIList();
                if (poiList != null) {
                    mTitleText = Utility.substring(poiList.getShortMessage(), 5)
                    + mSphinx.getString(R.string.double_bracket, poiList.getTotal());
                }
        
                poiList = poiModel.getBPOIList();
                if (poiList != null) {
                    mTitleText = Utility.substring(poiList.getShortMessage(), 5)
                    + mSphinx.getString(R.string.double_bracket, poiList.getTotal());
                }
            }
        }
        
        if (mTitleBtn != null) {
            mTitleBtn.setText(mTitleText);
        }

        if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(mResultAdapter.getSubDataType())) {
            mEmptyTxv.setText(R.string.can_not_found_result_and_retry);
        } else {
            mEmptyTxv.setText(str);
        }
    }
    
    private void refreshFilter(List<Filter> filterList) {
        synchronized (mFilterList) {
            if (mFilterList != filterList) {
                mFilterList.clear();
                if (filterList != null) {
                    for(Filter filter : filterList) {
                        mFilterList.add(filter.clone());
                    }
                }
                FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mRightBtn.setBackgroundResource(R.drawable.btn_view_map);
        mRightBtn.setOnClickListener(this);
        
        if (isReLogin()) {
            return;
        }
        
        refreshStamp();

        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        
        updateView();
        
        if (mSphinx.uiStackContains(R.id.view_hotel_home)) {
            HotelHomeFragment hotelFragment = mSphinx.getHotelHomeFragment();
            mSphinx.getPOIDetailFragment().getDynamicHotelPOI().initDate(hotelFragment.getCheckin(), hotelFragment.getCheckout());
        } else {
            mSphinx.getPOIDetailFragment().getDynamicHotelPOI().initDate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    private void updateView() {
        if (mState == STATE_QUERYING) {
            mQueryingView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            mNavigationWidget.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
            mAddMerchantView.setVisibility(View.GONE);
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
            mAddMerchantView.setVisibility(View.GONE);
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
            
            if (BaseQuery.SUB_DATA_TYPE_POI.equals(mResultAdapter.getSubDataType())) {
                mAddMerchantView.setVisibility(View.VISIBLE);
            } else {
                mAddMerchantView.setVisibility(View.GONE);
            }
        } else {
            DataQuery dataQuery = mDataQuery;
            if (dataQuery != null &&
                    BaseQuery.SUB_DATA_TYPE_HOTEL.equals(dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE)) &&
                    mPOIList.size() > 0) {
                int bottom = Util.dip2px(Globals.g_metrics.density, 32);
                mResultLsv.setPadding(0, 0, 0, bottom);
                mNavigationWidget.setVisibility(View.VISIBLE);
            } else {
                mResultLsv.setPadding(0, 0, 0, 0);
                mNavigationWidget.setVisibility(View.GONE);
            }
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(mPOIList.size() > 0 ? View.VISIBLE : View.GONE);
            
            mSphinx.showHint(TKConfig.PREFS_HINT_POI_LIST, R.layout.hint_poi_list);
            
            boolean footerSpringback = mResultLsv.isFooterSpringback();
            View v = mCurrentFootView;
            boolean addMerchant = false;
            if (BaseQuery.SUB_DATA_TYPE_POI.equals(mResultAdapter.getSubDataType())) {
                if (footerSpringback == false) {
                    if (v != mAddMerchantFootView) {
                        mResultLsv.removeFooterView(mAddMerchantFootView);
                        mResultLsv.removeFooterView(mLoadingView);
                        mResultLsv.addFooterView(mAddMerchantFootView, false);
                        mCurrentFootView = mAddMerchantFootView;
                    }
                    addMerchant = true;
                }
            }
            
            if (addMerchant == false) {
                if (v != mLoadingView) {
                    int state = mResultLsv.getState(false);
                    mResultLsv.removeFooterView(mAddMerchantFootView);
                    mResultLsv.removeFooterView(mLoadingView);
                    mResultLsv.addFooterView(mLoadingView);
                    mResultLsv.setFooterSpringback(footerSpringback);
                    mResultLsv.changeHeaderViewByState(false, state);
                    mCurrentFootView = mLoadingView;
                }
            }
            mAddMerchantView.setVisibility(View.GONE);
            
        }
        refreshResultTitleText(null);
    }
    
    private void turnPage(){
        synchronized (this) {
        DataQuery lastDataQuery = mDataQuery;
        if (mState != STATE_LIST || canTurnPage() == false || lastDataQuery == null || mResultLsv.isFooterSpringback() == false) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);

        DataQuery poiQuery = new DataQuery(mContext);
        POI requestPOI = lastDataQuery.getPOI();
        int cityId = lastDataQuery.getCityId();
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(mPOIList.size() - (mShowAPOI ? 1 : 0)));
        poiQuery.setup(criteria, cityId, getId(), getId(), null, true, true, requestPOI);
        mSphinx.queryStart(poiQuery);
        }
    }

    public void viewMap(int firstVisiblePosition, int lastVisiblePosition) {
        if (firstVisiblePosition < 0 || mPOIList.size() < firstVisiblePosition || lastVisiblePosition < 0 || mPOIList.size() < lastVisiblePosition || mPOIList.isEmpty()) {
            return;            
        }
        int size = mPOIList.size();
        List<POI> poiList = new ArrayList<POI>();
        int[] page = Utility.makePagedIndex(mResultLsv, mShowAPOI ? size - 1 : size, (mShowAPOI && firstVisiblePosition > 0) ? firstVisiblePosition-1 : firstVisiblePosition);
        POI poi;
        if (mShowAPOI) {
            poi = mPOIList.get(0);
            poiList.add(poi);
        }
        
        int minIndex = page[0]+(mShowAPOI ? 1 : 0);
        int maxIndex = page[1]+(mShowAPOI ? 1 : 0);
        for(;minIndex <= maxIndex && minIndex < size; minIndex++) {
            poi = mPOIList.get(minIndex);
            poiList.add(poi);
        }
        int firstIndex = page[2]+(mShowAPOI ? 1 : 0);
        if (mShowAPOI) {
            if (firstVisiblePosition == 0) {
                firstIndex = 0;
            }
        }
        mSphinx.showPOI(poiList, firstIndex);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.POIListMap);
        mSphinx.showView(R.id.view_result_map);   
    }
    
    public void doFilter(String name) {
        FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
        
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            dismissPopupWindow();
            return;
        }

        DataQuery poiQuery = new DataQuery(mContext);

        POI requestPOI = lastDataQuery.getPOI();
        int cityId = lastDataQuery.getCityId();
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        poiQuery.setup(criteria, cityId, getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(poiQuery);
        setup(mInputText);
        dismissPopupWindow();
    }
    
    public void cancelFilter() {
        dismissPopupWindow();
        if (mResultLsv.isFooterSpringback() && mFilterListView.isTurnPaging()) {
            turnPage();
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_btn:
                if (mPOIList.isEmpty() || mState != STATE_LIST) {
                    return;
                }
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                viewMap(mResultLsv.getFirstVisiblePosition(), mResultLsv.getLastVisiblePosition());
                break;
                
            case R.id.add_merchant_item_view:
                showAddMerchant();
                break;
                
            default:
                
                boolean turnPageing = (mResultLsv.getState(false) == SpringbackListView.REFRESHING);
                if (mState == STATE_QUERYING && turnPageing == false) {
                    return;
                }
                
                if (mTkAsyncTasking != null) {
                    mTkAsyncTasking.stop();
                }
                mResultLsv.onRefreshComplete(false);
                
                showFilterListView(mTitleFragment);

                byte key = (Byte)view.getTag();
                mFilterListView.setData(mFilterList, key, POIResultFragment.this, turnPageing, mActionTag);
        }
    }
    
    public static class POIAdapter extends ArrayAdapter<POI>{
        private static final int RESOURCE_ID = R.layout.poi_list_item;
        
        private Drawable icAPOI;
        private String distanceA;
        private Drawable icComment;
        private Drawable icAddress;
        private int aColor;
        private int bColor;
        private Activity activity;
        private LayoutInflater layoutInflater;
        private boolean showStamp = true;
        private Runnable loadedDrawableRun;
        private String viewToken;
        private int hotelPicWidth = 0;
        private String subDataType = BaseQuery.SUB_DATA_TYPE_POI;
        
        public String getSubDataType() {
            return this.subDataType;
        }
        
        public void setSubDataType(String subDataType) {
            this.subDataType = subDataType;
        }
        
        public void setShowStamp(boolean showStamp) {
            this.showStamp = showStamp;
        }

        public POIAdapter(Activity activity, List<POI> list, Runnable loadedDrawableRun, String viewToken) {
            super(activity, RESOURCE_ID, list);
            this.activity = activity;
            this.loadedDrawableRun = loadedDrawableRun;
            this.viewToken = viewToken;
            layoutInflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Resources resources = activity.getResources();
            icAPOI = resources.getDrawable(R.drawable.ic_a_poi);
            icComment = resources.getDrawable(R.drawable.ic_comment);
            icAddress = resources.getDrawable(R.drawable.ic_discover_address);
            aColor = resources.getColor(R.color.blue);
            bColor = resources.getColor(R.color.black_dark);
            distanceA = activity.getString(R.string.distanceA);
            hotelPicWidth = Util.dip2px(Globals.g_metrics.density, 98);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = layoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }

            View pictureView = view.findViewById(R.id.picture_view);
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView canReserveTxv = (TextView) view.findViewById(R.id.can_reserve_txv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView categoryTxv = (TextView) view.findViewById(R.id.category_txv);
            TextView moneyTxv = (TextView) view.findViewById(R.id.money_txv);
            TextView commentTxv = (TextView) view.findViewById(R.id.comment_txv);
            RatingBar startsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);

            POI poi = getItem(position);
            int aTotal = 0;
            for(int i = 0, count = getCount(); i < count && i < 2; i++) {
                if (getItem(i).getResultType() == POIResponse.FIELD_A_POI_LIST) {
                    aTotal++;
                    if (aTotal > 1) {
                        break;
                    }
                }
            }
            
            int hotelPicWidth = 0;
            Hotel hotel = poi.getHotel();
            if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType) && hotel.getUuid() != null) {
                if (hotel.getImageThumb() != null) {
                    TKDrawable tkDrawable = hotel.getImageThumb();
                    Drawable drawable = tkDrawable.loadDrawable(activity, loadedDrawableRun, viewToken);
                    if(drawable != null) {
                        //To prevent the problem of size change of the same pic 
                        //After it is used at a different place with smaller size
                        if( drawable.getBounds().width() != pictureImv.getWidth() || drawable.getBounds().height() != pictureImv.getHeight() ){
                            pictureImv.setBackgroundDrawable(null);
                        }
                        pictureImv.setBackgroundDrawable(drawable);
                    } else {
                        pictureImv.setBackgroundDrawable(null);
                    }
                    
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture_hotel_none);
                }
                
                if (hotel.getCanReserve() > 0) {
                    canReserveTxv.setVisibility(View.GONE);
                } else {
                    canReserveTxv.setVisibility(View.VISIBLE);
                }
                pictureView.setVisibility(View.VISIBLE);
                hotelPicWidth = this.hotelPicWidth;
            } else {
                pictureView.setVisibility(View.GONE);
                canReserveTxv.setVisibility(View.VISIBLE);
            }
            
            if (position == 0 && poi.getResultType() == POIResponse.FIELD_A_POI_LIST && aTotal == 1) {
                nameTxv.setTextColor(aColor);
                icAPOI.setBounds(0, 0, icAPOI.getIntrinsicWidth(), icAPOI.getIntrinsicHeight());
                nameTxv.setCompoundDrawables(icAPOI, null, null, null);
                nameTxv.setCompoundDrawablePadding(Util.dip2px(Globals.g_metrics.density, 4));
            } else {
                nameTxv.setCompoundDrawables(null, null, null, null);
                nameTxv.setTextColor(bColor);
            }
            
            nameTxv.setText(poi.getName());
            
            String distance = poi.getToCenterDistance();
            if (!TextUtils.isEmpty(distance)) {
                if (distance.startsWith(distanceA)) {
                    icAPOI.setBounds(0, 0, icAPOI.getIntrinsicWidth(), icAPOI.getIntrinsicHeight());
                    distanceFromTxv.setCompoundDrawables(null, null, icAPOI, null);
                    distanceFromTxv.setText(activity.getString(R.string.distance));
                    distanceTxv.setText(distance.replace(distanceA, ""));
                } else {
                    distanceFromTxv.setText("");
                    distanceFromTxv.setCompoundDrawables(null, null, null, null);
                    distanceTxv.setText(distance);
                }
            } else {
                distanceFromTxv.setText("");
                distanceFromTxv.setCompoundDrawables(null, null, null, null);
                distanceTxv.setText("");
            }
            
            String str = poi.getCategory();
            if (!TextUtils.isEmpty(str)) {
                categoryTxv.setText(str);
            } else {
                categoryTxv.setText("");
            }
            
            if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                String price = poi.getPrice();
                if (TextUtils.isEmpty(price)) {
                    moneyTxv.setText("");
                } else {
                    moneyTxv.setText(price);
                }
            } else {
                long money = poi.getPerCapity();
                if (money > -1) {
                    moneyTxv.setText(activity.getString(R.string.yuan, money));
                } else {
                    moneyTxv.setText("");
                }
            }

            str = null;
            Comment lastComment = poi.getLastComment();
            if (lastComment != null) {
                str = lastComment.getContent();
            }
            if (!TextUtils.isEmpty(str)) {
                commentTxv.setText(str);
                icComment.setBounds(0, 0, icComment.getIntrinsicWidth(), icComment.getIntrinsicHeight());
                commentTxv.setCompoundDrawablePadding(Util.dip2px(Globals.g_metrics.density, 4));
                commentTxv.setCompoundDrawables(icComment, null, null, null);
                commentTxv.setVisibility(View.VISIBLE);
            } else {
                str = poi.getAddress();
                if (!TextUtils.isEmpty(str)) {
                    commentTxv.setText(str);
                    icAddress.setBounds(0, 0, icAddress.getIntrinsicWidth(), icAddress.getIntrinsicHeight());
                    commentTxv.setCompoundDrawablePadding(Util.dip2px(Globals.g_metrics.density, 4));
                    commentTxv.setCompoundDrawables(icAddress, null, null, null);
                    commentTxv.setVisibility(View.VISIBLE);
                } else {
                    commentTxv.setVisibility(View.INVISIBLE);
                }
            }

            float star = poi.getGrade();
            startsRtb.setRating(star/2.0f);

            ImageView stampImv = (ImageView) view.findViewById(R.id.stamp_imv);
            int from = poi.getFrom();
             
            if (showStamp
                    // 跟马然沟通，本地搜藏或历史浏览的在我要点评是没有戳的，但是在加载出来的非本地poi若用户点评过是应该有戳的。但是现在没有戳。
                    || from == POI.FROM_ONLINE) {
                if (poi.isGoldStamp()) {
                    stampImv.setImageResource(R.drawable.ic_stamp_gold);
                    stampImv.setVisibility(View.VISIBLE);
                } else if (poi.isSilverStamp()) {
                    stampImv.setImageResource(R.drawable.ic_stamp_silver);
                    stampImv.setVisibility(View.VISIBLE);
                } else {
                    stampImv.setVisibility(View.GONE);
                }
            } else {
                stampImv.setVisibility(View.GONE);
            }
            
            ViewGroup dynamicPOIListView = (ViewGroup) view.findViewById(R.id.dynamic_poi_list_view);
            List<DynamicPOI> list = poi.getDynamicPOIList();
            int viewIndex = refresDynamicPOI(list, dynamicPOIListView);
            int dynamicPOIWidth = viewIndex * activity.getResources().getDrawable(R.drawable.ic_dynamicpoi_tuangou).getIntrinsicWidth();
            
            int viewCount = dynamicPOIListView.getChildCount();
            for(int i = viewIndex; i < viewCount; i++) {
                dynamicPOIListView.getChildAt(i).setVisibility(View.GONE);
            }
            if (dynamicPOIWidth > 0) {
                nameTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int nameTxvWidth = nameTxv.getMeasuredWidth();
                int width = Globals.g_metrics.widthPixels-(4*Util.dip2px(Globals.g_metrics.density, 8));
                if (nameTxvWidth > width-dynamicPOIWidth-hotelPicWidth) {
                    nameTxv.getLayoutParams().width = (width-dynamicPOIWidth-hotelPicWidth);
                } else {
                    nameTxv.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
            } else {
                nameTxv.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
            }
            
            return view;
        }
        
        int refresDynamicPOI(List<DynamicPOI> list, ViewGroup listView) {

            if (list == null || list.isEmpty()) {
                return 0;
            }
            
            List<String> DPOITypeList = new ArrayList<String>();
            
            int viewCount = listView.getChildCount();
            int viewIndex = 0;
            for(int i = 0, size = list.size(); i < size; i++) {
                final DynamicPOI dynamicPOI = list.get(i);
                final String dataType = dynamicPOI.getType();
                if (!DPOITypeList.contains(dataType)) {
                    DPOITypeList.add(dataType);
                    View child;
                    if (viewIndex < viewCount) {
                        child = listView.getChildAt(viewIndex);
                        child.setVisibility(View.VISIBLE);
                    } else {
                        child = new ImageView(activity);
                        listView.addView(child, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    }
                    ImageView iconImv = (ImageView) child;
                    iconImv.setPadding(Util.dip2px(Globals.g_metrics.density, 2), 0, 0, 0);
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
                    } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_yanchu);
                    } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_zhanlan);
                    } else if (DynamicPOI.TYPE_HOTEL.equals(dataType) && !BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_hotel);
                    } else if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_dianying);
                    } else if (BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_coupon);
                    } else {
                        continue;
                    }
                    viewIndex++;
                }
            }
            
            return viewIndex;
        }
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        mResultLsv.onRefreshComplete(false);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        String subDataType = dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_SUB_DATA_TYPE);
        mResultAdapter.setSubDataType(subDataType);
        if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
            mRetryView.setText(R.string.can_not_found_result_and_retry, false);
        }
        
        mResultLsv.onRefreshComplete(false);
        if (dataQuery.isStop()) {
            return;
        }

        mResultLsv.setFooterSpringback(false);
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else {
            Response response = dataQuery.getResponse();
            if (response != null) {
                int responsCode = response.getResponseCode();
                if (responsCode != Response.RESPONSE_CODE_OK) {
                    if (dataQuery.isTurnPage()) {
                        return;
                    }
                    int resId = R.id.view_invalid;
                    if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                        int responseCode = response.getResponseCode();
                        if (responseCode == 702) {
                            resId = R.string.response_code_702;
                        } else if (responseCode == 703) {
                            resId = R.string.response_code_703;
                        }
                        
                    }
                    
                    if (resId == R.id.view_invalid) {
                        int resid = BaseActivity.getResponseResId(dataQuery);
                        mRetryView.setText(resid, true);
                    } else {
                        mRetryView.setText(resId, false);
                    }
                    mState = STATE_ERROR;
                    updateView();
                    return;
                }
            } else {
                if (dataQuery.isTurnPage()) {
                    mResultLsv.setFooterSpringback(true);
                    return;
                }
                mRetryView.setText(R.string.touch_screen_and_retry, true);
                mState = STATE_ERROR;
                updateView();
                return;
            }
        }

        POIResponse poiResponse = (POIResponse)dataQuery.getResponse();
        if ((poiResponse.getAPOIList() != null && 
                poiResponse.getAPOIList().getList() != null && 
                poiResponse.getAPOIList().getList().size() > 0) || 
           (poiResponse.getBPOIList() != null && 
                poiResponse.getBPOIList().getList() != null && 
                poiResponse.getBPOIList().getList().size() > 0)) {
      
            mDataQuery = dataQuery;
            mState = STATE_LIST;

            POIList poiList = poiResponse.getBPOIList();
            List<POI> bPOIList = null;
            if (poiList != null) {
                mBTotal = poiList.getTotal();
                bPOIList = poiList.getList();
            } else {
                mBTotal = 0;
            }
            
            poiList = poiResponse.getAPOIList();
            List<POI> aPOIList = null;
            if (!dataQuery.isTurnPage()) {
                this.mATotal = 0;
                this.mShowAPOI = false;
                mPOIList.clear();
                mSphinx.getHandler().post(new Runnable() {

                    @Override
                    public void run() {
                        mResultLsv.setSelectionFromTop(0, 0);
                    }
                });
                if (poiList != null) {
                    mATotal = poiList.getTotal();
                    aPOIList = poiList.getList();
                }
                
                POI poi = dataQuery.getPOI();
                if (mATotal == 0 && TextUtils.isEmpty(poi.getAddress()) == false &&
                        BaseQuery.SUB_DATA_TYPE_POI.equals(subDataType)) { // 若POI存在地址且搜索结果的子类型为普通POI则将其列为A类POI
                    mATotal = 1;
                    poi.setResultType(POIResponse.FIELD_A_POI_LIST);
                    aPOIList = new ArrayList<POI>();
                    aPOIList.add(poi);
                }
                
                if (mATotal == 1 && mBTotal > 0) {
                    mShowAPOI = true;
                    aPOIList.get(0).setOnlyAPOI(true);
                    mPOIList.addAll(aPOIList);
                } else if (aPOIList != null){
                    mPOIList.addAll(aPOIList);
                }
            }

            if (bPOIList != null) {
                if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
                    for(int i = bPOIList.size()-1; i >= 0; i--) {
                        bPOIList.get(i).setSourceType(POI.SOURCE_TYPE_HOTEL);
                    }
                }
                mPOIList.addAll(bPOIList);
            }

            if (dataQuery.getSourceViewId() == R.id.view_poi_nearby_search) {
                if (this.mSphinx.uiStackContains(R.id.view_more_favorite)) {
                    mSphinx.uiStackClose(new int[]{R.id.view_more_home, R.id.view_more_favorite, getId()});
                } else if (this.mSphinx.uiStackContains(R.id.view_more_history)) {
                    mSphinx.uiStackClose(new int[]{R.id.view_more_home, R.id.view_more_history, getId()});
                } else {
                    mSphinx.uiStackClose(new int[]{R.id.view_poi_home, getId()});
                    if (mSphinx.uiStackContains(R.id.view_poi_home) == false) {
                        mSphinx.uiStackInsert(R.id.view_poi_home, 0);
                    }
                }
            }

            mResultLsv.setFooterSpringback(canTurnPage());
            List<Filter> filterList = mDataQuery.getFilterList();
            if (filterList != null && filterList.size() > 0) {
                refreshFilter(filterList);
            }
        } else {
            if (dataQuery.isTurnPage()) {
                return;
            }
            mState = STATE_EMPTY;
        }

        refreshResultTitleText(dataQuery);
        updateView();
        mResultAdapter.notifyDataSetChanged();
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        
    }
    
    private boolean canTurnPage() {
        if (mShowAPOI) {
            return mPOIList.size()-1 < mBTotal;
        } else if (mATotal > 1) {
            return mPOIList.size() < mATotal;
        } else {
            return mPOIList.size() < mBTotal;
        }
    }

    public void refreshStamp() {
        if (mResultAdapter != null) {
            mResultAdapter.notifyDataSetChanged();
        }
    }
    
    private void showFilterListView(View parent) {
        mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter);
        if (mPopupWindow == null) {
            mFilterListView = new FilterListView(mSphinx);
            
            mPopupWindow = new PopupWindow(mFilterListView);
            mPopupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            mPopupWindow.setFocusable(true);
            // 设置允许在外点击消失
            mPopupWindow.setOutsideTouchable(true);

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            mPopupWindow.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss() {
                    mActionLog.addAction(mActionTag + ActionLog.PopupWindowFilter + ActionLog.Dismiss);
                }
            });
            
        }
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }

    @Override
    public void retry() {
        if (mBaseQuerying != null) {
        	for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                mBaseQuerying.get(i).setResponse(null);
        	}
            mSphinx.queryStart(mBaseQuerying);
        }
        setup(mInputText);
    }
}

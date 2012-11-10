/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;
import com.tigerknows.view.user.User;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class POIResultFragment extends BaseFragment implements View.OnClickListener {

    public POIResultFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private Dialog mFilterDialog = null;

    private LinearLayout mFilterLnl = null;
    
    private TextView mResultTxv;

    private SpringbackListView mResultLsv = null;

    private View mQueryingView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private POIAdapter mResultAdapter = null;
    
    private DataQuery mPOIQuery;
    
    private DataQuery mPOIQuerying;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
    private long mATotal;
    
    private long mBTotal;
    
    private List<Filter> mFilterList = new ArrayList<Filter>();
    
    private boolean mNotResult = true;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mResultLsv.getLastVisiblePosition() >= mResultLsv.getCount()-2 &&
                    mResultLsv.getFirstVisiblePosition() == 0) {
                mResultLsv.getView(false).performClick();
            }
        }
    };
    
    public void setup() {
        this.mPOIQuerying = (DataQuery) this.mTkAsyncTasking.getBaseQuery();
        this.mNotResult = true;

        if (mPOIQuerying != null) {
            POI poi = mPOIQuerying.getPOI();
            String str;
            if (poi.getSourceType() == POI.SOURCE_TYPE_MY_LOCATION) {
                str = mContext.getString(R.string.searching);
            } else if (poi.getSourceType() == POI.SOURCE_TYPE_CITY_CENTER) {
                str = mContext.getString(R.string.at_city_searching, mSphinx.getMapEngine().getCityInfo(mPOIQuerying.getCityId()).getCName());
            } else {
                str = mContext.getString(R.string.at_location_searching);
            }
    
            if (this.mPOIQuerying.getSourceViewId() != getId()) {
                mPOIQuery = null;
                mFilterLnl.setVisibility(View.GONE);
            }
            
            if (mPOIQuerying.isTurnPage() == false) {
                mResultTxv.setText(str);
                mQueryingTxv.setText(str);
                updateView();
            }
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.SearchResult;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_result, container, false);

        findViews();
        setListener();
        
        mResultAdapter = new POIAdapter(mContext, mPOIList);
        mResultLsv.setAdapter(mResultAdapter);
        
        return mRootView;
    }
    
    protected void findViews() {
        mResultTxv = (TextView) mRootView.findViewById(R.id.result_txv);
        mFilterLnl = (LinearLayout)mRootView.findViewById(R.id.filter_lnl);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mQueryingView = mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    POI poi = (POI) adapterView.getAdapter().getItem(position);
                    if (poi != null) {
                        mActionLog.addAction(ActionLog.SearchResultSelect, poi.getUUID(), poi.getName(), position+1-mATotal);
                        poi.setOnlyAPOI((position == 0 && mATotal == 1 && mBTotal > 0));
                        mSphinx.getPOIDetailFragment().setData(poi);
                        mSphinx.showView(R.id.view_poi_detail);
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
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mPOIQuerying == null && mPOIQuery != null && mEmptyView.getVisibility() == View.VISIBLE) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                mNotResult = false;
                updateView();
                refreshFilter();
                refreshResultTitleText(mPOIQuery);
                return true;
            }
        }
        return false;
    }
    
    private List<POI> getPagePOIList(int index) {
        boolean isShowAPOI = (mATotal == 1 && mBTotal > 0);
        List<POI> poiList = new ArrayList<POI>();
        if (isShowAPOI) {
            mPOIList.get(0).setOnlyAPOI(true);
            poiList.add(mPOIList.get(0));
            if (index >= mPOIList.size()-1) {
                index--;
            }
         }
        int minIndex = index - (index % TKConfig.getPageSize()) + (isShowAPOI ? 1 : 0);
        int maxIndex = minIndex + TKConfig.getPageSize();
        for(;minIndex >= 0 && minIndex < maxIndex && minIndex < mPOIList.size(); minIndex++) {
            mPOIList.get(minIndex).setOrderNumber(minIndex+ (isShowAPOI ? 0 : 1));
            poiList.add(mPOIList.get(minIndex));
        }
        return poiList;
    }
    
    private void refreshResultTitleText(DataQuery poiQuery) {
        String str = "";
        POIResponse poiModel = (POIResponse)poiQuery.getResponse();
            if (poiModel != null) {
            POIList poiList = poiModel.getAPOIList();
            if (poiList != null) {
                str = poiList.getMessage();
            }
    
            poiList = poiModel.getBPOIList();
            if (TextUtils.isEmpty(str) && poiList != null) {
                str = poiList.getMessage();
            }
        }

        if (TextUtils.isEmpty(str)) {
            str = mContext.getString(R.string.no_result);
        }
        
        mResultTxv.setText(str);
        mEmptyTxv.setText(str);
    }
    
    private void refreshFilter() {
        mFilterList.clear();
        if (mPOIQuery != null) {
            List<Filter> filterList = mPOIQuery.getFilterList();
            for(Filter filter : filterList) {
                mFilterList.add(filter.clone());
            }
        }
        LogWrapper.d("POIResultFragment", "refreshFilter() mFilterList="+mFilterList.size());
        FilterExpandableListAdapter.refreshFilterButton(mFilterLnl, mFilterList, mSphinx, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleTxv.setText(R.string.result_list);
        mRightBtn.setImageResource(R.drawable.ic_view_map);
        mRightTxv.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightTxv.setOnClickListener(this);
        
        if (isReLogin()) {
            return;
        }
        
        if (mSphinx.mSnapMap) {
            if (mSphinx.mIntoSnap == 0) {
                mLeftBtn.setVisibility(View.INVISIBLE);
                mLeftTxv.setText(R.string.home);
            }
            mSphinx.mIntoSnap++;
        }
        refreshStamp();

        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    private void updateView() {
        if (mPOIQuerying != null) {
            mQueryingView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
        } else if (mNotResult) {
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
        } else {
            mQueryingView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
        }
    }
    
    private void turnPage(){
        synchronized (this) {
        if (mPOIQuerying != null || mATotal+mBTotal <= mPOIList.size()) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        mActionLog.addAction(ActionLog.SearchResultNextPage);

        DataQuery poiQuery = new DataQuery(mContext);
        POI requestPOI = mPOIQuery.getPOI();
        int cityId = mPOIQuery.getCityId();
        Hashtable<String, String> criteria = mPOIQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(mPOIList.size() - mATotal));
        poiQuery.setup(criteria, cityId, getId(), getId(), null, true, true, requestPOI);
        mSphinx.queryStart(poiQuery);
        }
    }

    public void viewMap(int position) {
        if (position < 0 || mPOIList.size() < position) {
            return;            
        }
        mSphinx.showPOI(getPagePOIList(position), position % TKConfig.getPageSize());
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.MapPOI);
        mSphinx.showView(R.id.view_result_map);   
    }
    
    private void filter() {
        
        DataQuery poiQuery = new DataQuery(mContext);

        POI requestPOI = mPOIQuery.getPOI();
        int cityId = mPOIQuery.getCityId();
        Hashtable<String, String> criteria = mPOIQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        poiQuery.setup(criteria, cityId, getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(poiQuery);
        setup();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_txv:
                if (mPOIQuery == null || mPOIList.isEmpty() || mResultLsv.getVisibility() != View.VISIBLE || mNotResult == true || mPOIQuerying != null) {
                    return;
                }
                mActionLog.addAction(ActionLog.SearchResultMap);
                if (mResultLsv.getLastVisiblePosition() >= mPOIList.size()-1) {
                    viewMap(mPOIList.size()-1); 
                } else {
                    viewMap(mResultLsv.getFirstVisiblePosition());
                }
                break;
                
            default:
                if (mPOIQuerying != null) {
                    return;
                }
                
                if (mFilterDialog != null && mFilterDialog.isShowing()) {
                    return;
                }
                
                if (mTkAsyncTasking != null) {
                    mTkAsyncTasking.stop();
                }
                mResultLsv.onRefreshComplete(false);

                final Filter filter = (Filter)view.getTag();
                final int key = filter.getKey();
                if (key == POIResponse.FIELD_FILTER_AREA_INDEX) {
                    mActionLog.addAction(ActionLog.SearchResultFilterArea);
                } else if (key == POIResponse.FIELD_FILTER_CATEGORY_INDEX) {
                    mActionLog.addAction(ActionLog.SearchResultFilterCategory);
                } else if (key == POIResponse.FIELD_FILTER_ORDER_INDEX) {
                    mActionLog.addAction(ActionLog.SearchResultFilterOrder);
                }
                
                
                final ExpandableListView filterElv = new ExpandableListView(mContext);
                filterElv.setScrollingCacheEnabled(false);
                filterElv.setFadingEdgeLength(0);
                filterElv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                
                final FilterExpandableListAdapter filterExpandableListAdapter = new FilterExpandableListAdapter(mSphinx, filter);
                filterElv.setAdapter(filterExpandableListAdapter);
                filterElv.setGroupIndicator(null);
                Drawable drawable = mContext.getResources().getDrawable(R.drawable.divider);
                filterElv.setDivider(drawable);
                filterElv.setChildDivider(drawable);
                filterElv.setPadding(0, 0, 0, 0);
                filterElv.setFadingEdgeLength(0);
                filterElv.setOnGroupClickListener(new OnGroupClickListener() {
                    
                    @Override
                    public boolean onGroupClick(ExpandableListView arg0, View arg1, final int position, long arg3) {
                        List<Filter> list = filter.getChidrenFilterList();
                        if (list.get(position).getChidrenFilterList().size() == 0) {
                            mFilterDialog.dismiss();
                            if (!list.get(position).isSelected() || mNotResult) {
                                
                                for(Filter filter : list) {
                                    filter.setSelected(false);
                                    List<Filter> chidrenFilterList1 = filter.getChidrenFilterList();
                                    for(Filter childrenFilter1 : chidrenFilterList1) {
                                        childrenFilter1.setSelected(false);
                                    }
                                }
                                list.get(position).setSelected(true);
                                
                                ((Button)view).setText(FilterExpandableListAdapter.getFilterTitle(mSphinx, filter));
                                mActionLog.addAction(ActionLog.SearchResultFilter, key, ((Button)view).getText());
                                filter();
                            }
                        }
                        return false;
                    }
                });
                filterElv.setOnChildClickListener(new OnChildClickListener() {
                    
                    @Override
                    public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4) {
                        mFilterDialog.dismiss();
                        List<Filter> list = filter.getChidrenFilterList();
                        if (!list.get(arg2).getChidrenFilterList().get(arg3).isSelected() || mNotResult) {
                            for(Filter filter : list) {
                                filter.setSelected(false);
                                List<Filter> childrenList1 = filter.getChidrenFilterList();
                                for(Filter childrenFilter1 : childrenList1) {
                                    childrenFilter1.setSelected(false);
                                }
                            }
                            list.get(arg2).getChidrenFilterList().get(arg3).setSelected(true);
                            
                            ((Button)view).setText(FilterExpandableListAdapter.getFilterTitle(mSphinx, (filter)));
                            mActionLog.addAction(ActionLog.SearchResultFilter, key, ((Button)view).getText());
                            filter();
                        }
                        return false;
                    }
                });
                
                mFilterDialog = new AlertDialog.Builder(mSphinx).setTitle(FilterExpandableListAdapter.getFilterTitle(mSphinx, (filter)))
                .setView(filterElv)
                .setCancelable(true)
                .setOnCancelListener(new OnCancelListener() {
                    
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
                    }
                })
                .create();
                
                mFilterDialog.setCanceledOnTouchOutside(false);
                mFilterDialog.show();

                int position = FilterExpandableListAdapter.getFilterSelectParentPosition(filter);
                if (position < filter.getChidrenFilterList().size()) {
                    filterElv.expandGroup(position);
                    filterElv.setSelectionFromTop(position, 0);
                }
        }
    }
    
    public static class POIAdapter extends ArrayAdapter<POI>{
        private static final int RESOURCE_ID = R.layout.poi_list_item;
        
        private Drawable icAPOI;
        private String distanceA;
        private Drawable icComment;
        private Context context;
        private LayoutInflater layoutInflater;
        private boolean showStamp = true;
        
        public void setShowStamp(boolean showStamp) {
            this.showStamp = showStamp;
        }

        public POIAdapter(Context context, List<POI> list) {
            super(context, RESOURCE_ID, list);
            this.context = context;
            layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            icAPOI = context.getResources().getDrawable(R.drawable.ic_a_poi);
            distanceA = context.getString(R.string.distanceA);
            icComment = context.getResources().getDrawable(R.drawable.ic_comment);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = layoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            TextView numberTxv = (TextView) view.findViewById(R.id.number_txv);
            ImageView numberImv = (ImageView) view.findViewById(R.id.number_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView categoryTxv = (TextView) view.findViewById(R.id.category_txv);
            TextView moneyTxv = (TextView) view.findViewById(R.id.money_txv);
            TextView commentTxv = (TextView) view.findViewById(R.id.comment_txv);
            RatingBar startsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);

            POI poi = getItem(position);
            int aTotal = 0;
            for(int i = 0, count = getCount(); i < count; i++) {
                if (getItem(i).getResultType() == POIResponse.FIELD_A_POI_LIST) {
                    aTotal++;
                    if (aTotal > 1) {
                        break;
                    }
                }
            }
            
            if (position == 0 && poi.getResultType() == POIResponse.FIELD_A_POI_LIST && aTotal == 1) {
                numberTxv.setText("99");
                numberTxv.setVisibility(View.INVISIBLE);
                numberImv.setVisibility(View.VISIBLE);
            } else {
                numberTxv.setVisibility(View.VISIBLE);
                numberImv.setVisibility(View.GONE);
                int orderNumber = position+(aTotal == 1 ? 0 : 1);
                poi.setOrderNumber(orderNumber);
                if (orderNumber < 10) {
                    numberTxv.setText("  "+orderNumber); 
                } else {
                    numberTxv.setText(String.valueOf(orderNumber)); 
                }
                numberTxv.setBackgroundDrawable(null);
            }
            
            nameTxv.setText(poi.getName());
            
            String distance = poi.getToCenterDistance();
            if (!TextUtils.isEmpty(distance)) {
                if (distance.startsWith(distanceA)) {
                    icAPOI.setBounds(0, 0, icAPOI.getIntrinsicWidth(), icAPOI.getIntrinsicHeight());
                    distanceFromTxv.setCompoundDrawables(null, null, icAPOI, null);
                    distanceFromTxv.setText(context.getString(R.string.distance));
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
            
            int money = poi.getPerCapity();
            if (money > -1) {
                moneyTxv.setText(context.getString(R.string.yuan, money));
            } else {
                moneyTxv.setText("");
            }

            str = poi.getCommentSummary();            
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
                    commentTxv.setCompoundDrawablePadding(Util.dip2px(Globals.g_metrics.density, 4));
                    commentTxv.setCompoundDrawables(null, null, null, null);
                    commentTxv.setVisibility(View.VISIBLE);
                } else {
                    commentTxv.setVisibility(View.INVISIBLE);
                }
            }

            float star = poi.getGrade();
            startsRtb.setRating(star/2);

            ImageView stampImv = (ImageView) view.findViewById(R.id.stamp_imv);
            if (showStamp) {
                long attribute = poi.getAttribute();
                User user = Globals.g_User;
                if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0 && user != null) {
                    stampImv.setImageResource(R.drawable.ic_stamp_gold);
                    stampImv.setVisibility(View.VISIBLE);
                } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0 && user == null) {
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
            int viewCount = dynamicPOIListView.getChildCount();
            int viewIndex = 0;
            int size = list.size();
            int dynamicPOIWidth = 0;
            List<String> types = new ArrayList<String>();
            for(int i = 0; i < size; i++) {
                final DynamicPOI dynamicPOI = list.get(i);
                final String dataType = dynamicPOI.getType();
                if ((BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) ||
                        BaseQuery.DATA_TYPE_YANCHU.equals(dataType) ||
                        BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType))
                        && types.contains(dataType) == false) {
                    types.add(dataType);
                    View child;
                    if (viewIndex < viewCount) {
                        child = dynamicPOIListView.getChildAt(viewIndex);
                        child.setVisibility(View.VISIBLE);
                    } else {
                        child = new ImageView(context);
                        dynamicPOIListView.addView(child, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    }
                    ImageView iconImv = (ImageView) child;
                    iconImv.setPadding(Util.dip2px(Globals.g_metrics.density, 4), 0, 0, 0);
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(dynamicPOI.getType())) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
                    } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dynamicPOI.getType())) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_yanchu);
                    } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dynamicPOI.getType())) {
                        iconImv.setImageResource(R.drawable.ic_dynamicpoi_zhanlan);
                    }
                    dynamicPOIWidth += iconImv.getDrawable().getIntrinsicWidth();
                    viewIndex++;
                }
            }
            
            viewCount = dynamicPOIListView.getChildCount();
            for(int i = viewIndex; i < viewCount; i++) {
                dynamicPOIListView.getChildAt(i).setVisibility(View.GONE);
            }
            if (dynamicPOIWidth > 0) {
                numberTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int numberTxvWidth = numberTxv.getMeasuredWidth();
                nameTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int nameTxvWidth = nameTxv.getMeasuredWidth();
                distanceTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int distanceTxvWidth = distanceTxv.getMeasuredWidth();
                distanceFromTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int distanceFromTxvWidth = distanceFromTxv.getMeasuredWidth();
                int width = Globals.g_metrics.widthPixels-distanceTxvWidth-distanceFromTxvWidth-numberTxvWidth-(5*Util.dip2px(Globals.g_metrics.density, 8));
                if (nameTxvWidth > width-dynamicPOIWidth) {
                    nameTxv.getLayoutParams().width = (width-dynamicPOIWidth);
                } else {
                    nameTxv.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
            } else {
                nameTxv.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
            }
            
            return view;
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
        DataQuery poiQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        
        if (mPOIQuerying != null && poiQuery != mPOIQuerying) {
            return;
        }

        boolean exit = true;
        if (poiQuery.getCriteria().containsKey(BaseQuery.SERVER_PARAMETER_INDEX)) {
            int index = Integer.parseInt(poiQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_INDEX));
            if (index > 0) {
                mResultLsv.onRefreshComplete(false);
                mResultLsv.setFooterSpringback(true);
                exit = false;
            }
        }

        boolean filter = false;
        if (poiQuery.getCriteria().containsKey(DataQuery.SERVER_PARAMETER_FILTER) && exit) {
            filter = true;
            exit = false;
        }
        if (BaseActivity.checkReLogin(poiQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(poiQuery, mSphinx, null, !filter, this, exit)) {
            if (filter) {
                mPOIQuerying = null;
                mNotResult = true;
//                updateView();
                final AlertDialog alertDialog = CommonUtils.getAlertDialog(mSphinx);
                alertDialog.setCancelable(false);
                alertDialog.setMessage(mSphinx.getString(BaseActivity.getResponseResId(poiQuery)));
                alertDialog.setButton(mSphinx.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        alertDialog.dismiss();
                        mNotResult = false;
                        updateView();
                        refreshFilter();
                        refreshResultTitleText(mPOIQuery);
                    }
                });
                alertDialog.show();
                
            }
            return;
        }
        mResultLsv.onRefreshComplete(false);
        mResultLsv.setFooterSpringback(false);
        
        mPOIQuerying = null;
        POIResponse poiResponse = (POIResponse)poiQuery.getResponse();
        if ((poiResponse.getAPOIList() != null && 
                poiResponse.getAPOIList().getList() != null && 
                poiResponse.getAPOIList().getList().size() > 0) || 
           (poiResponse.getBPOIList() != null && 
                poiResponse.getBPOIList().getList() != null && 
                poiResponse.getBPOIList().getList().size() > 0)) {
      
            mPOIQuery = poiQuery;
            mNotResult = false;

            updateView();

            POIList poiList = poiResponse.getAPOIList();
            List<POI> aPOIList = null;
            if (!mPOIQuery.isTurnPage()) {
                mATotal = 0;
                mBTotal = 0;
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
            }

            poiList = poiResponse.getBPOIList();
            List<POI> bPOIList = null;
            if (poiList != null) {
                mBTotal = poiList.getTotal();
                bPOIList = poiList.getList();
            }

            if (mATotal == 1 && mBTotal > 0) {
                if (!mPOIQuery.isTurnPage()) {
                    mPOIList.addAll(aPOIList);
                }
            } else if (aPOIList != null) {
                mPOIList.addAll(aPOIList);
            }

            if (bPOIList != null) {
                mPOIList.addAll(bPOIList);
            }

            mResultAdapter.notifyDataSetChanged();

            refreshResultTitleText(mPOIQuery);

            if (this.mPOIQuery.getSourceViewId() == R.id.view_poi_nearby) {
                if (this.mSphinx.uiStackContains(R.id.view_favorite)) {
                    mSphinx.uiStackClear();
                    mSphinx.uiStackPush(R.id.view_more);
                    mSphinx.uiStackPush(R.id.view_favorite);
                    mSphinx.uiStackPush(getId());
                } else if (this.mSphinx.uiStackContains(R.id.view_history)) {
                    mSphinx.uiStackClear();
                    mSphinx.uiStackPush(R.id.view_more);
                    mSphinx.uiStackPush(R.id.view_history);
                    mSphinx.uiStackPush(getId());
                } else {
                    mSphinx.uiStackClear();
                    mSphinx.uiStackPush(R.id.view_home);
                    mSphinx.uiStackPush(getId());
                }
            }

            if (mBTotal > 0 && (mFilterDialog == null || !mFilterDialog.isShowing())) {
                refreshFilter();
            }
            if (mPOIQuery.isTurnPage() == false) {
                mPOIQuery.getCriteria().put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
            }

            if (mPOIList.size() < mATotal + mBTotal) {
                mResultLsv.setFooterSpringback(true);
            }
        } else {
            if (!poiQuery.isStop()) {
                refreshResultTitleText(poiQuery);

                if (!poiQuery.isTurnPage()) {
                    updateView();
                }
            }
        }
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        
    }

    public void refreshStamp() {
        if (mResultAdapter != null) {
            mResultAdapter.notifyDataSetChanged();
        }
    }
}

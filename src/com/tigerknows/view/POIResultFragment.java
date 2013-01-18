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
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;
import com.tigerknows.view.user.User;

import android.content.Context;
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
    
    private PopupWindow mPopupWindow;
    
    private String mTitleText;

    private ViewGroup mFilterControlView = null;
    
    private SpringbackListView mResultLsv = null;

    private QueryingView mQueryingView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private RetryView mRetryView;
    
    private POIAdapter mResultAdapter = null;
    
    private DataQuery mDataQuery;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
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
    
    public void setup() {
        DataQuery lastDataQuerying = (DataQuery) this.mTkAsyncTasking.getBaseQuery();
        if (lastDataQuerying == null) {
            return;
        }
        
        if (lastDataQuerying.getSourceViewId() != getId()) {
            mDataQuery = null;
            mFilterControlView.setVisibility(View.GONE);
        }

        POI poi = lastDataQuerying.getPOI();
        String str;
        if (poi.getSourceType() == POI.SOURCE_TYPE_MY_LOCATION) {
            str = mContext.getString(R.string.searching);
        } else if (poi.getSourceType() == POI.SOURCE_TYPE_CITY_CENTER) {
            str = mContext.getString(R.string.at_city_searching, mSphinx.getMapEngine().getCityInfo(lastDataQuerying.getCityId()).getCName());
        } else {
            str = mContext.getString(R.string.at_location_searching);
        }
        mQueryingTxv.setText(str);
        mTitleText = lastDataQuerying.getCriteria().get(BaseQuery.SERVER_PARAMETER_KEYWORD);
        
        this.mState = STATE_QUERYING;
        updateView();
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
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
    }

    protected void setListener() {
        mRetryView.setCallBack(this);
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    POI poi = (POI) adapterView.getAdapter().getItem(position);
                    if (poi != null) {
                        mActionLog.addAction(ActionLog.SearchResultSelect, poi.getUUID(), poi.getName(), position+1);
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
            DataQuery lastDataQuery = mDataQuery;
            if (mState != STATE_QUERYING && mState != STATE_LIST && lastDataQuery != null) {
                mActionLog.addAction(ActionLog.KeyCodeBack);
                mState = STATE_LIST;
                updateView();
                refreshFilter(lastDataQuery);
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
                    str = poiList.getMessage();
                }
                long aTotal = poiList.getTotal();
        
                poiList = poiModel.getBPOIList();
                if (poiList != null) {
                    str = poiList.getMessage();
                }
                long bTotal = poiList.getTotal();
                
                mTitleText = mSphinx.getString(R.string.search_result, dataQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_KEYWORD), aTotal > 1 ? aTotal : bTotal);
            }
        }
        
        if (mTitleBtn != null) {
            mTitleBtn.setText(mTitleText);
        }
        mEmptyTxv.setText(str);
    }
    
    private void refreshFilter(DataQuery dataQuery) {
        if (dataQuery == null) {
            return;
        }
        mFilterList.clear();

        List<Filter> filterList = dataQuery.getFilterList();
        for(Filter filter : filterList) {
            mFilterList.add(filter.clone());
        }
        FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
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
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(View.GONE);
        } else {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
            if (mRightBtn != null)
                mRightBtn.setVisibility(mPOIList.size() > 0 ? View.VISIBLE : View.GONE);
            
            mSphinx.showHint(TKConfig.PREFS_HINT_POI_LIST, R.layout.hint_poi_list);
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
        mActionLog.addAction(ActionLog.SearchResultNextPage);

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
        int[] page = CommonUtils.makePage(mResultLsv, size, firstVisiblePosition, lastVisiblePosition, mShowAPOI);
        POI poi;
        if (mShowAPOI) {
            poi = mPOIList.get(0);
            poiList.add(poi);
        }
        
        int minIndex = page[0];
        int maxIndex = page[1];
        for(;minIndex <= maxIndex && minIndex < size; minIndex++) {
            poi = mPOIList.get(minIndex);
            poiList.add(poi);
        }
        mSphinx.showPOI(poiList, page[2]);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.MapPOI);
        mSphinx.showView(R.id.view_result_map);   
    }
    
    public void doFilter(String name) {
        FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
        
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            return;
        }

        mActionLog.addAction(mActionTag+ActionLog.SearchResultFilter, name, DataQuery.makeFilterRequest(this.mFilterList));
        
        DataQuery poiQuery = new DataQuery(mContext);

        POI requestPOI = lastDataQuery.getPOI();
        int cityId = lastDataQuery.getCityId();
        Hashtable<String, String> criteria = lastDataQuery.getCriteria();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        poiQuery.setup(criteria, cityId, getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(poiQuery);
        setup();
    }
    
    public void cancelFilter() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        if (mResultLsv.isFooterSpringback() && mFilterListView.isTurnPaging()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.right_btn:
                if (mPOIList.isEmpty() || mState != STATE_LIST) {
                    return;
                }
                mActionLog.addAction(ActionLog.SearchResultMap);
                viewMap(mResultLsv.getFirstVisiblePosition(), mResultLsv.getLastVisiblePosition());
                break;
                
            default:
                if (mState != STATE_LIST || mDataQuery == null) {
                    return;
                }
                
                if (mTkAsyncTasking != null) {
                    mTkAsyncTasking.stop();
                }
                mResultLsv.onRefreshComplete(false);
                
                showFilterListView(mTitleFragment);

                byte key = (Byte)view.getTag();
                if (key == POIResponse.FIELD_FILTER_AREA_INDEX) {
                    mActionLog.addAction(ActionLog.SearchResultFilterArea);
                } else if (key == POIResponse.FIELD_FILTER_CATEGORY_INDEX) {
                    mActionLog.addAction(ActionLog.SearchResultFilterCategory);
                } else if (key == POIResponse.FIELD_FILTER_ORDER_INDEX) {
                    mActionLog.addAction(ActionLog.SearchResultFilterOrder);
                }
                mFilterListView.setData(mFilterList, key, POIResultFragment.this, mResultLsv.getState(false) == SpringbackListView.REFRESHING);
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
            Resources resources = context.getResources();
            icAPOI = resources.getDrawable(R.drawable.ic_a_poi);
            icComment = resources.getDrawable(R.drawable.ic_comment);
            icAddress = resources.getDrawable(R.drawable.ic_discover_address);
            aColor = resources.getColor(R.color.blue);
            bColor = resources.getColor(R.color.black_dark);
            distanceA = context.getString(R.string.distanceA);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = layoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView bubbleImv = (ImageView) view.findViewById(R.id.bubble_imv);
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
            
            if (position == 0 && poi.getResultType() == POIResponse.FIELD_A_POI_LIST && aTotal == 1) {
                bubbleImv.setVisibility(View.VISIBLE);
                nameTxv.setTextColor(aColor);
            } else {
                bubbleImv.setVisibility(View.GONE);
                nameTxv.setTextColor(bColor);
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
                    icAddress.setBounds(0, 0, icAddress.getIntrinsicWidth(), icAddress.getIntrinsicHeight());
                    commentTxv.setCompoundDrawablePadding(Util.dip2px(Globals.g_metrics.density, 4));
                    commentTxv.setCompoundDrawables(icAddress, null, null, null);
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
                int bubbleImvWidth = 0;
                if (bubbleImv.getVisibility() == View.VISIBLE) {
                    bubbleImv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    bubbleImvWidth = bubbleImv.getMeasuredWidth();
                }
                nameTxv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int nameTxvWidth = nameTxv.getMeasuredWidth();
                int width = Globals.g_metrics.widthPixels-bubbleImvWidth-(4*Util.dip2px(Globals.g_metrics.density, 8));
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
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        
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
                    int resid = BaseActivity.getResponseResId(dataQuery);
                    mRetryView.setText(resid);
                    mState = STATE_ERROR;
                    updateView();
                    return;
                }
            } else {
                if (dataQuery.isTurnPage()) {
                    mResultLsv.setFooterSpringback(true);
                    return;
                }
                mRetryView.setText(R.string.network_failed);
                mState = STATE_ERROR;
                updateView();
                return;
            }
        }

        refreshFilter(dataQuery);
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
                if (mATotal == 0 && TextUtils.isEmpty(poi.getAddress()) == false) { // 来自周边搜索界面
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
                mPOIList.addAll(bPOIList);
            }

            if (dataQuery.getSourceViewId() == R.id.view_poi_nearby) {
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

            mResultLsv.setFooterSpringback(canTurnPage());
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
        if (mPopupWindow == null) {
            mFilterListView = new FilterListView(mSphinx);
            
            mPopupWindow = new PopupWindow(mFilterListView);
            mPopupWindow.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            mPopupWindow.setFocusable(true);
            // 设置允许在外点击消失
            mPopupWindow.setOutsideTouchable(true);

            // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
            
        }
        mPopupWindow.showAsDropDown(parent, 0, 0);

    }

    @Override
    public void retry() {
        if (mBaseQuerying != null) {
            mBaseQuerying.setResponse(null);
            mSphinx.queryStart(mBaseQuerying);
        }
        setup();
    }
}

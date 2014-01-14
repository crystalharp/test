/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.discover;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.BaseList;
import com.tigerknows.model.DataQuery.DiscoverCategoreResponse;
import com.tigerknows.model.DataQuery.DiscoverResult;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterCategoryOrder;
import com.tigerknows.model.Yingxun.Changci;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIResultFragment.POIAdapter;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.IPagerListCallBack;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.PopupWindow.OnDismissListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class DiscoverListFragment extends DiscoverBaseFragment implements View.OnClickListener, FilterListView.CallBack, SpringbackListView.IPagerList, RetryView.CallBack {

    public DiscoverListFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    /**
     * Button for Dingdan in Tuangou list
     */
    private ImageButton mDingdanBtn;
    
    /**
     * View containing the filter criterias, parent and child.
     */
    private FilterListView mFilterListView = null;
    
    /**
     * The filter list control view containing three buttons
     */
    private ViewGroup mFilterControlView = null;

    /**
     * The list view containing the result of the list view
     */
    private SpringbackListView mResultLsv = null;

    /**
     * The view showing that the query is in progress
     */
    private QueryingView mQueryingView = null;
    
    /**
     * The text that shows the user that query is in progress
     */
    private TextView mQueryingTxv = null;
    
    /**
     * The view that shows the query result is empty
     */
    private View mEmptyView = null;
    
    /**
     * The text view that shows the query result is empty
     */
    private TextView mEmptyTxv = null;
    
    /**
     * The view the promote the user to tap the screen to retry
     */
    private RetryView mRetryView;
    
    private DataQuery mDataQuery;
    
    /**
     * Different state of the list fragment
     */
    static final int STATE_QUERYING = 0;
    static final int STATE_ERROR = 1;
    static final int STATE_EMPTY = 2;
    static final int STATE_LIST = 3;
    
    private int mState = STATE_QUERYING;
    
    /**
     * List of Discover entities and the corresponding adapter
     */
    private List<Tuangou> mTuangouList = new ArrayList<Tuangou>();
    
    private TuangouAdapter mTuangouAdapter;
    
    private List<Dianying> mDianyingList = new ArrayList<Dianying>();
    
    private DianyingAdapter mDianyingAdapter;
    
    private List<Yanchu> mYanchuList = new ArrayList<Yanchu>();
    
    private YanchuAdapter mYanchuAdapter;
    
    private List<Zhanlan> mZhanlanList = new ArrayList<Zhanlan>();
    
    private ZhanlanAdapter mZhanlanAdapter;
    
    /**
     * Filter list used in the current list fragment
     */
    private List<Filter> mFilterList = new ArrayList<Filter>();
    
    private String mFilterArea;
    
    private DiscoverResult mList;
    
    private ArrayAdapter mArrayAdapter;
    
    private String mDataType;
    
    private TextView mLocationTxv;
    
    private POI mAPOI = null;
    
    private String mTitleText;
    
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
            ArrayAdapter adapter = getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };
    
    SpringbackListView.IPagerListCallBack mIPagerListCallBack = null;
    
    /**
     * 显示查询状态
     * @param dataQuery
     */
    private void showQuering(DataQuery dataQuery) {
        if (dataQuery == null) {
            return;
        }
        
        String str = getString(R.string.searching);
        mQueryingTxv.setText(str);
        mTitleText = getString(R.string.searching_title);

        mState = STATE_QUERYING;
        updateView();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mActionTag = ActionLog.DianyingList;
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {  
        mRootView = mLayoutInflater.inflate(R.layout.poi_result, container, false);

        findViews();
        setListener();
        
        return mRootView;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void dismiss() {
        super.dismiss();
        List list = getList();
        if (list != null) {
            list.clear();
            ArrayAdapter adapter = getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        mDataQuery = null;
    }
    
    @Override
    protected void findViews() {
        super.findViews();
        mDingdanBtn = (ImageButton) mRootView.findViewById(R.id.dingdan_btn);
        mFilterControlView = (ViewGroup)mRootView.findViewById(R.id.filter_control_view);
        mResultLsv = (SpringbackListView)mRootView.findViewById(R.id.result_lsv);
        View nearbySearchBarView = mLayoutInflater.inflate(R.layout.poi_nearby_search_bar, null);
        mResultLsv.addHeaderView(nearbySearchBarView, false);
        mLocationTxv = (TextView) nearbySearchBarView.findViewById(R.id.location_txv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mResultLsv.addFooterView(v);
        mQueryingView = (QueryingView)mRootView.findViewById(R.id.querying_view);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) mRootView.findViewById(R.id.retry_view);
    }

    private void updateChangeciOption(List<Dianying> list){
        int changciOption = Changci.OPTION_DAY_TODAY;
        for(Filter filter : mFilterList) {
            if (filter.getKey() == FilterCategoryOrder.FIELD_LIST_CATEGORY) {
                String str = getSelectFilterName(filter);
                if (str.contains(getString(R.string.tomorrow_char))) {
                    changciOption = Changci.OPTION_DAY_TOMORROW;
                } else if (str.contains(getString(R.string.after_tomorrow_char))) {
                    changciOption = Changci.OPTION_DAY_AFTER_TOMORROW;
                }
            }
        }
        for (int i = 0, size=list.size(); i < size; i++) {
        	list.get(i).getYingxun().setChangciOption(changciOption);
		}
    }
    
    @Override
    protected void setListener() {
        super.setListener();
        mRetryView.setCallBack(this, mActionTag);
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int location, long id) {
                int position = (int)id;
                List list = getList();
                if (list != null && position >= 0 && position < list.size()) {
                    Object object = list.get(position);
                    if (object != null) {
                        if (object instanceof Tuangou) {
                            Tuangou tagret = (Tuangou) object;
//                            tagret.getFendian().setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                            mSphinx.showView(R.id.view_discover_tuangou_detail);
                            mSphinx.getTuangouDetailFragment().setData(mTuangouList, position, DiscoverListFragment.this);
                        } else if (object instanceof Yanchu){
                            Yanchu tagret = (Yanchu) object;
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                            mSphinx.showView(R.id.view_discover_yanchu_detail);
                        	mSphinx.getYanchuDetailFragment().setData(mYanchuList, position, DiscoverListFragment.this);
                        } else if (object instanceof Dianying){
                            Dianying tagret = (Dianying) object;
//                            tagret.getYingxun().setOrderNumber(position+1);
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                        	mSphinx.showView(R.id.view_discover_dianying_detail);
                        	mSphinx.getDianyingDetailFragment().setData(mDianyingList, position, DiscoverListFragment.this);
                        } else if (object instanceof Zhanlan){
                        	Zhanlan tagret = (Zhanlan) object;
                            mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, tagret.getUid());
                            mSphinx.showView(R.id.view_discover_zhanlan_detail);
                        	mSphinx.getZhanlanDetailFragment().setData(mZhanlanList, position, DiscoverListFragment.this);
                        }
                    }
                }
            }
            
        });

        mResultLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage(null);
            }
        });
        mDingdanBtn.setOnClickListener(this);
    }
    
    private static String getSelectFilterName(Filter filter) {
        String title = null;
        if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            int i = 0;
            for(Filter chidrenFilter : chidrenFilterList) {
                if (i == 0) {
                    title = chidrenFilter.getFilterOption().getName();
                }
                i++;
                if (chidrenFilter.isSelected()) {
                    title = chidrenFilter.getFilterOption().getName();
                    break;
                } else {
                    
                    List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                    for(Filter chidrenFilter1 : chidrenFilterList1) {
                        if (chidrenFilter1.isSelected()) {
                            title = chidrenFilter.getFilterOption().getName();
                            break;
                        }
                    }
                }
            }   

        }
        return title;
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

    @SuppressWarnings("rawtypes")
    @Override
    public void onResume() {
        super.onResume();
        mIPagerListCallBack = null;
        
        if (isReLogin()) {
            return;
        }
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 500);
        }
        ArrayAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            mDingdanBtn.setVisibility(View.VISIBLE);
        } else {
            mDingdanBtn.setVisibility(View.GONE);
        }
        mRightBtn.setText(R.string.map);
        mRightBtn.setOnClickListener(this);
        
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
            if (isShowing()) {
                mRightBtn.setVisibility(View.INVISIBLE);
            }
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.GONE);
            if (isShowing()) {
                mRightBtn.setVisibility(View.INVISIBLE);
            }
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            mResultLsv.setVisibility(View.GONE);
            if (isShowing()) {
                mRightBtn.setVisibility(View.INVISIBLE);
            }
        } else {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mResultLsv.setVisibility(View.VISIBLE);
            if (getList().size() > 0) {
                if (isShowing()) {
                    if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType) == false) {
                        mRightBtn.setVisibility(View.VISIBLE);
                    } else {
                        mRightBtn.setVisibility(View.INVISIBLE);
                    }
                }
            } else {
                if (isShowing()) {
                    mRightBtn.setVisibility(View.INVISIBLE);
                }
            }
            
            if (mAPOI != null) {
                this.mResultLsv.changeHeaderViewByState(true, SpringbackListView.PULL_TO_REFRESH);
                this.mLocationTxv.setText(this.mAPOI.getName());
            } else {
                this.mResultLsv.changeHeaderViewByState(true, SpringbackListView.DONE);
                this.mLocationTxv.setText(null);
            }
        }
        
        refreshResultTitleText(null);
    }
    
    /**
     * 刷新标题栏文字
     * @param dataQuery
     */
    private void refreshResultTitleText(DataQuery dataQuery) {
        String str = getString(R.string.no_result);
        if (dataQuery != null) {
            DiscoverCategoreResponse response = (DiscoverCategoreResponse)dataQuery.getResponse();
            if (response != null) {
                DiscoverResult list = response.getDiscoverResult();
                if (list != null) {
                    String name = "";
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
                        name = getString(R.string.tuangou);
                    } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
                        name = getString(R.string.film);
                    } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
                        name = getString(R.string.yanchu);
                    } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
                        name = getString(R.string.zhanlan);
                    }
                    mTitleText = name + getString(R.string.double_bracket, list.getTotal());
                }
            }
        }
        
        if (isShowing()) {
            mTitleBtn.setHint(mTitleText);
        }

        mEmptyTxv.setText(str);
    }
    
    private void turnPage(String tip){
        synchronized (this) {
        DataQuery lastDataQuery = mDataQuery;
        if (mState != STATE_LIST || mResultLsv.isFooterSpringback() == false || lastDataQuery == null || mList == null || mList.getTotal() <= getList().size()) {
            mResultLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            return;
        }
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);

        DataQuery dataQuery = new DataQuery(lastDataQuery);
        dataQuery.setParameter(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(getList().size()));
        int dianyingSize = mDianyingList.size();
        if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType) && dianyingSize > 0) {
            dataQuery.setParameter(DataQuery.SERVER_PARAMETER_DIANYING_UUID, mDianyingList.get(dianyingSize-1).getUid());
        }
        if (dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_FILTER)) {
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        }
        dataQuery.setup(getId(), getId(), tip, true, false, lastDataQuery.getPOI());
        mSphinx.queryStart(dataQuery);
        }
    }

    private void viewMap(int firstVisiblePosition) {
        if (mList == null) {
            return;            
        }

        // 此处判断减1，是因为第0个是A类POI
        if (firstVisiblePosition > 0) {
            firstVisiblePosition--;
        }
        
        int size = getList().size();
        int[] page = Utility.makePagedIndex(mResultLsv, size, firstVisiblePosition);
        List<BaseData> dataList = new ArrayList<BaseData>();
        
        int minIndex = page[0];
        int maxIndex = page[1];
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mTuangouList.size(); minIndex++) {
                Tuangou data = mTuangouList.get(minIndex);
                dataList.add(data);
            }
            mSphinx.getTuangouDetailFragment().setData(mTuangouList, page[2], DiscoverListFragment.this);
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mYanchuList.size(); minIndex++) {
                Yanchu data = mYanchuList.get(minIndex);
                dataList.add(data);
            }
            mSphinx.getYanchuDetailFragment().setData(mYanchuList, page[2], DiscoverListFragment.this);
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            for(;minIndex >= 0 && minIndex <= maxIndex && minIndex < mZhanlanList.size(); minIndex++) {
                Zhanlan data = mZhanlanList.get(minIndex);
                dataList.add(data);
            }
            mSphinx.getZhanlanDetailFragment().setData(mZhanlanList, page[2], DiscoverListFragment.this);
        }
        if (dataList.isEmpty()) {
            return;
        }
        int name = R.string.tuangou_ditu;
        String actionTag = ActionLog.ResultMapTuangouList;
        if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            name = R.string.yanchu_ditu;
            actionTag = ActionLog.ResultMapYanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            name = R.string.zhanlan_ditu;
            actionTag = ActionLog.ResultMapZhanlanList;
        }
        mSphinx.getResultMapFragment().setData(getString(name), actionTag);
        mSphinx.showView(R.id.view_result_map);   
        int firstIndex = page[2];
        ItemizedOverlayHelper.drawPOIOverlay(mSphinx, dataList, firstIndex, mAPOI);
    }
    
    public void doFilter(String name) {
        FilterListView.refreshFilterButton(mFilterControlView, mFilterList, mSphinx, this);
        
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            dismissPopupWindow();
            return;
        }

        DataQuery dataQuery = new DataQuery(lastDataQuery);

        POI requestPOI = lastDataQuery.getPOI();
        dataQuery.setParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.setParameter(DataQuery.SERVER_PARAMETER_FILTER, DataQuery.makeFilterRequest(mFilterList));
        dataQuery.setup(getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(dataQuery);
        showQuering(dataQuery);
        
        /*
         * First set up the views that is under the filter list
         * Then dismiss the PopupWindow to prevent the blink effect 
         * Of the reverse order.
         */
        dismissPopupWindow();
    }
    
    public void cancelFilter() {
        dismissPopupWindow();
        if (mResultLsv.isFooterSpringback() && mFilterListView.isTurnPaging()) {
            turnPage(null);
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            
            case R.id.right_btn:
                if (mDataQuery == null || mState != STATE_LIST) {
                    return;
                }
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                viewMap(mResultLsv.getFirstVisiblePosition());
                break;
                
            case R.id.dingdan_btn:
                mActionLog.addAction(mActionTag +  ActionLog.TuangouListDingdan);
                mSphinx.getMyOrderFragment().setData(true);
                mSphinx.showView(R.id.view_more_my_order);
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
                mFilterListView.setData(mFilterList, key, DiscoverListFragment.this, turnPageing, mActionTag);
        }
    }
    
    public class TuangouAdapter extends ArrayAdapter<Tuangou>{
        private static final int RESOURCE_ID = R.layout.discover_tuangou_list_item;
        String rmb;
        
        public TuangouAdapter(Context context, List<Tuangou> list) {
            super(context, RESOURCE_ID, list);
            rmb = context.getString(R.string.rmb_text);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            ImageView shangjiaMarkerImv = (ImageView) view.findViewById(R.id.shangjia_marker_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView orgPriceTxv = (TextView) view.findViewById(R.id.org_price_txv);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView buyerNumTxv = (TextView) view.findViewById(R.id.buyer_num_txv);
            TextView fastPurchaseTxv = (TextView) view.findViewById(R.id.fast_purchase_txv);
            TextView appointmentTxv = (TextView) view.findViewById(R.id.appointment_txv);

            Tuangou tuangou = getItem(position);
            Drawable drawable = tuangou.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
                Rect bounds = drawable.getBounds();
            	if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
            		pictureImv.setBackgroundDrawable(null);
            	}
            	pictureImv.setBackgroundDrawable(drawable);
            } else {
                pictureImv.setBackgroundDrawable(null);
            }
            
            Shangjia shangjia = Shangjia.getShangjiaById(tuangou.getSource(), mSphinx, mLoadedDrawableRun);
            if (shangjia != null) {
                shangjiaMarkerImv.setImageDrawable(shangjia.getMarker());
            } else {
                shangjiaMarkerImv.setImageDrawable(null);
            }
            nameTxv.setText(tuangou.getShortDesc());
            priceTxv.setText(tuangou.getPrice());
            orgPriceTxv.setText(tuangou.getOrgPrice()+rmb);
            orgPriceTxv.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
            POIAdapter.showDistance(mSphinx, distanceFromTxv, distanceTxv, tuangou.getFendian().getDistance());
            buyerNumTxv.setText(String.valueOf(tuangou.getBuyerNum())+getString(R.string.people));
            
            if (tuangou.getAppointment() == 1) {
                appointmentTxv.setVisibility(View.VISIBLE);
            } else {
                appointmentTxv.setVisibility(View.GONE);
            }

            if (tuangou.getUrl() != null) {
                fastPurchaseTxv.setVisibility(View.VISIBLE);
            } else {
                fastPurchaseTxv.setVisibility(View.GONE);
            }
            
            return view;
        }
    }
    
    public class DianyingAdapter extends ArrayAdapter<Dianying>{
        private static final int RESOURCE_ID = R.layout.discover_dianying_list_item;
        
        public DianyingAdapter(Context context, List<Dianying> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.category_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);

            Dianying dianying = getItem(position);
            Drawable drawable = dianying.getPicture().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
            	pictureImv.setImageDrawable(drawable);
            } else {
                pictureImv.setImageDrawable(null);
            }
            
            nameTxv.setText(dianying.getName());
            starsRtb.setProgress((int) dianying.getRank());
            POIAdapter.showDistance(mSphinx, distanceFromTxv, distanceTxv, dianying.getYingxun().getDistance());
            
            addressTxv.setText(dianying.getTag());
            if (TextUtils.isEmpty(dianying.getLength())) {
            	dateTxv.setText(R.string.dianying_no_length_now);
            } else {
            	dateTxv.setText(String.valueOf(dianying.getLength()));
            }
            return view;
        }
    }
    
    public class YanchuAdapter extends ArrayAdapter<Yanchu>{
        private static final int RESOURCE_ID = R.layout.discover_yanchu_list_item;
        
        public YanchuAdapter(Context context, List<Yanchu> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);

            Yanchu yanchu = getItem(position);
            Drawable drawable = yanchu.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
                Rect bounds = drawable.getBounds();
            	if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
            		pictureImv.setImageDrawable(null);
            	}
            	pictureImv.setImageDrawable(drawable);
            } else {
                pictureImv.setImageDrawable(null);
            }
            
            nameTxv.setText(yanchu.getName());
            starsRtb.setProgress((int) yanchu.getHot());
            addressTxv.setText(yanchu.getAddress());
            POIAdapter.showDistance(mSphinx, distanceFromTxv, distanceTxv, yanchu.getDistance());
            dateTxv.setText(yanchu.getTimeDesc());
            
            return view;
        }
    }
    
    public class ZhanlanAdapter extends ArrayAdapter<Zhanlan>{
        private static final int RESOURCE_ID = R.layout.discover_yanchu_list_item;
        
        public ZhanlanAdapter(Context context, List<Zhanlan> list) {
            super(context, RESOURCE_ID, list);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

        	View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView pictureImv = (ImageView) view.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) view.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) view.findViewById(R.id.distance_txv);
            TextView distanceFromTxv = (TextView) view.findViewById(R.id.distance_from_txv);
            TextView addressTxv = (TextView) view.findViewById(R.id.address_txv);
            TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);

            Zhanlan yanchu = getItem(position);
            Drawable drawable = yanchu.getPictures().loadDrawable(mSphinx, mLoadedDrawableRun, DiscoverListFragment.this.toString());
            if(drawable != null) {
            	//To prevent the problem of size change of the same pic 
            	//After it is used at a different place with smaller size
                Rect bounds = drawable.getBounds();
            	if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
            		pictureImv.setImageDrawable(null);
            	}
            	pictureImv.setImageDrawable(drawable);
            } else {
                pictureImv.setImageDrawable(null);
            }
            
            nameTxv.setText(yanchu.getName());
            starsRtb.setProgress((int) yanchu.getHot());
            addressTxv.setText(yanchu.getAddress());
            POIAdapter.showDistance(mSphinx, distanceFromTxv, distanceTxv, yanchu.getDistance());
            dateTxv.setText(yanchu.getTimeDesc());
            
            return view;
        }
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        mResultLsv.changeHeaderViewByState(false, SpringbackListView.PULL_TO_REFRESH);
        invokeIPagerListCallBack();
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        
        mResultLsv.onRefreshComplete(false);
        if (dataQuery.isStop()) {
            invokeIPagerListCallBack();
            return;
        }

        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else {
            Response response = dataQuery.getResponse();
            if (response != null) {
                
                int responsCode = response.getResponseCode();
                if (responsCode != Response.RESPONSE_CODE_OK) {
                    if (responsCode == 701) {
                    } else {
                        if (dataQuery.isTurnPage()) {
                            invokeIPagerListCallBack();
                            return;
                        }
                        int resid = BaseActivity.getResponseResId(dataQuery);
                        mRetryView.setText(resid, true);
                        mState = STATE_ERROR;
                        updateView();
                    }

                    invokeIPagerListCallBack();
                    return;
                }
            } else {
                if (dataQuery.isTurnPage()) {
                    mResultLsv.setFooterLoadFailed(true);
                    invokeIPagerListCallBack();
                    return;
                }
                mRetryView.setText(R.string.touch_screen_and_retry, true);
                mState = STATE_ERROR;
                updateView();
                invokeIPagerListCallBack();
                return;
            }
        }

        mResultLsv.setFooterSpringback(false);
        setData(dataQuery);
        invokeIPagerListCallBack();
    }
    
    public void setData(DataQuery dataQuery) {
        Response response = dataQuery.getResponse();

        dealWithList(dataQuery, (DiscoverCategoreResponse) response);
        
        refreshResultTitleText(dataQuery);
        updateView();
        getAdapter().notifyDataSetChanged();
        
        if (mResultLsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
    
    private void dealWithList(DataQuery dataQuery, DiscoverCategoreResponse discoverCategoreResponse) {
        String dataType = dataQuery.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
        mDataType = dataType;

        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            mActionTag = ActionLog.TuangouList;
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            mActionTag = ActionLog.DianyingList;
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            mActionTag = ActionLog.YanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            mActionTag = ActionLog.ZhanlanList;
        }

        DiscoverResult discoverResult = discoverCategoreResponse.getDiscoverResult();
        
        if (discoverCategoreResponse.getDiscoverResult() != null 
                && discoverCategoreResponse.getDiscoverResult().getList() != null 
                && discoverCategoreResponse.getDiscoverResult().getList().size() > 0) {
            
            mState = STATE_LIST;
            mDataQuery = dataQuery;
            mList = discoverResult;
            
            if (dataQuery.isTurnPage() == false) {
                getList().clear();
                mResultLsv.setAdapter(getAdapter());
                getAdapter().notifyDataSetInvalidated();
                mAPOI = dataQuery.getPOI();
                Position centerPosition = discoverResult.getCenterPosition();
                if (centerPosition != null) {
                    if (mAPOI != null && !centerPosition.equals(mAPOI.getPosition())) {
                        mAPOI = null;
                    }
                    if (mAPOI == null &&
                            dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE) &&
                            dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE)) {
                        Position position = new Position(Double.valueOf(dataQuery.getParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE)), 
                                Double.valueOf(dataQuery.getParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE)));
                        if (!centerPosition.equals(position)) {
                            mAPOI = new POI();
                            mAPOI.setName(getString(R.string.my_location));
                            mAPOI.setPosition(position);
                        }
                    }
                } else {
                    mAPOI = null;
                }
                
                if (mAPOI != null) {
                    mAPOI.setOnlyAPOI(true);
                }
            }
            
            refreshFilter(mDataQuery.getFilterList());
            makeFilterArea(dataQuery);
            
            List discoverResultList = discoverResult.getList();
            
            List list = getList();
            ArrayAdapter arrayAdapter = getAdapter();
            
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
                List<Tuangou> tuangouList = discoverResultList;
                for(Tuangou item : tuangouList) {
                    item.setFilterArea(mFilterArea);
                }
            } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
                List<Dianying> dianyingList = discoverResultList;
                updateChangeciOption(dianyingList);
                for(Dianying item : dianyingList) {
                    item.setFilterArea(mFilterArea);
                }
            }
            list.addAll(discoverResultList);
            
            if (mArrayAdapter != arrayAdapter) {
                mResultLsv.setAdapter(arrayAdapter);
            }
            mArrayAdapter = arrayAdapter;
            
            if (getList().size() < mList.getTotal()) {
                mResultLsv.setFooterSpringback(true);
            } else {
                mResultLsv.setFooterSpringback(false);
            }
        } else {
            if (dataQuery.isTurnPage()) {
                return;
            }
            mState = STATE_EMPTY;
        }
    }
    
    private void makeFilterArea(DataQuery dataQuery) {
        if (dataQuery.isTurnPage() == false) {
            if (mFilterList.size() > 0) {
                mFilterArea = FilterListView.getFilterTitle(mSphinx, mFilterList.get(0));
            }
        }
    }
    
    private boolean invokeIPagerListCallBack() {
        if (mIPagerListCallBack != null) {
            mIPagerListCallBack.turnPageEnd(false, getList().size());
            mIPagerListCallBack = null;
            return true;
        }
        return false;
    }
    
    @SuppressWarnings("rawtypes")
    private List getList() {
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            return mTuangouList;
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            return mDianyingList;
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            return mYanchuList;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            return mZhanlanList;
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private ArrayAdapter getAdapter() {
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(mDataType)) {
            if (mTuangouAdapter == null) {
                mTuangouAdapter = new TuangouAdapter(mSphinx, mTuangouList);
            }
            return mTuangouAdapter;
        } else if (BaseQuery.DATA_TYPE_DIANYING.equals(mDataType)) {
            if (mDianyingAdapter == null) {
                mDianyingAdapter = new DianyingAdapter(mSphinx, mDianyingList);
            }
            return mDianyingAdapter;
        } else if (BaseQuery.DATA_TYPE_YANCHU.equals(mDataType)) {
            if (mYanchuAdapter == null) {
                mYanchuAdapter = new YanchuAdapter(mSphinx, mYanchuList);
            }
            return mYanchuAdapter;
        } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(mDataType)) {
            if (mZhanlanAdapter == null) {
                mZhanlanAdapter = new ZhanlanAdapter(mSphinx, mZhanlanList);
            }
            return mZhanlanAdapter;
        }
        return null;
    }    
    
    public DataQuery getLastQuery() {
        return mDataQuery;
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
            mPopupWindow.setAnimationStyle(-1);
            mPopupWindow.update();
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
    public boolean canTurnPage(boolean isHeader) {
        DataQuery dataQuery = mDataQuery;
        BaseList list = mList;
        if (mState != STATE_LIST || mResultLsv.isFooterSpringback() == false || dataQuery == null || list == null || list.getTotal() <= getList().size()) {
            return false;
        }
        return true;
    }

    @Override
    public void turnPageStart(boolean isHeader, IPagerListCallBack iPagerListCallBack) {
        mIPagerListCallBack = iPagerListCallBack;
        turnPage(getString(R.string.doing_and_wait));
    }

    @Override
    public void retry() {
        if (mBaseQuerying != null && mBaseQuerying.size() > 0) {
            for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                mBaseQuerying.get(i).setResponse(null);
            }
            mSphinx.queryStart(mBaseQuerying);
            showQuering((DataQuery) mBaseQuerying.get(0));
        }
    } 
    
    public List<Filter> getFilterList() {
        return mFilterList;
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.History;
import com.tigerknows.model.POI;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.SqliteWrapper;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class HistoryFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "HistoryFragment";
    
    public HistoryFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    public static final int MENU_DELETE             = 0;
    
    private POIAdapter mPOIAdapter = null;

    private TrafficAdapter mTrafficAdapter = null;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
    private List<History> mTrafficList = new ArrayList<History>();
    
    private Button mPOIBtn;
    
    private Button mTrafficBtn;
    
    private ViewPager mViewPager;
    
    private SpringbackListView mPOILsv = null;
    
    private TextView mEmptyView;
    
    private SpringbackListView mTrafficLsv = null;
    
    private String mLayerType = ItemizedOverlay.POI_OVERLAY;
    
    private String mPOIWhere;
    
    private boolean mDismiss = true;
    
    private Runnable mTurnPageRunPOI = new Runnable() {
        
        @Override
        public void run() {
            if (mPOILsv.getLastVisiblePosition() >= mPOILsv.getCount()-2 &&
                    mPOILsv.getFirstVisiblePosition() == 0) {
                turnPagePOI();
            }
        }
    };
    
    private Runnable mTurnPageRunTraffic = new Runnable() {
        
        @Override
        public void run() {
            if (mTrafficLsv.getLastVisiblePosition() >= mTrafficLsv.getCount()-2 &&
                    mTrafficLsv.getFirstVisiblePosition() == 0) {
                turnPageTraffic();
            }
        }
    };
    
    @SuppressWarnings("unchecked")
    private Comparator mComparator = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            BaseData baseData1 = (BaseData) object1;
            BaseData baseData2 = (BaseData) object2;
            return (int)(baseData2.getDateTime() - baseData1.getDateTime());
        };
    };

    private int mSelectIndex;
    private OnCreateContextMenuListener mContextMenuListener = new OnCreateContextMenuListener() {
        
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (info.position > -1 && info.position < (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? mPOIAdapter.getCount() : mTrafficAdapter.getCount())) {
                mSelectIndex = info.position;
                menu.add(0, MENU_DELETE, 0, R.string.delete);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.History;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.favorite, container, false);
        
        findViews();
        setListener();
        
        StringBuilder s = new StringBuilder();
        s.append("(");
        s.append(Tigerknows.POI.STORE_TYPE);
        s.append("=");
        s.append(Tigerknows.STORE_TYPE_HISTORY);
        s.append(")");
        mPOIWhere = s.toString();
        
        mPOIAdapter = new POIAdapter(mContext, mPOIList);
        mPOILsv.setAdapter(mPOIAdapter);
        mTrafficAdapter = new TrafficAdapter(mContext, mTrafficList);
        mTrafficLsv.setAdapter(mTrafficAdapter);
                
        return mRootView;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.history_browse);
        mRightBtn.setOnClickListener(this);
        mRightBtn.setText(R.string.clear_all);

        if (mDismiss) {
            mDismiss = false;
            readPOI(mPOIList, Long.MAX_VALUE, 0, false);
            readTraffic(mTrafficList, Long.MAX_VALUE, 0, false);
            if (mPOIList.isEmpty() && mTrafficList.isEmpty() == false) {
                mLayerType = ItemizedOverlay.TRAFFIC_OVERLAY;
            }
        } else {
            refresh(ItemizedOverlay.POI_OVERLAY);
            refresh(ItemizedOverlay.TRAFFIC_OVERLAY);
            
            if (mPOIList.size() > 0) {
                readPOI(mPOIList, Long.MAX_VALUE, mPOIList.get(0).getDateTime(), true);
            } else {
    //            readPOI(mPOIList, Long.MAX_VALUE, 0, false);
                mPOILsv.setFooterSpringback(true);
                turnPagePOI();
            }
            Collections.sort(mPOIList, mComparator);
            CommonUtils.keepListSize(mPOIList, Tigerknows.HISTORY_MAX_SIZE);
            mPOIAdapter.notifyDataSetChanged();
            
            if (mTrafficList.size() > 0) {
                readTraffic(mTrafficList, Long.MAX_VALUE, mTrafficList.get(0).getDateTime(), true);
            } else {
    //            readTraffic(mTrafficList, Long.MAX_VALUE, 0, false);
                mTrafficLsv.setFooterSpringback(true);
                turnPageTraffic();
            }
            Collections.sort(mTrafficList, mComparator);
            CommonUtils.keepListSize(mTrafficList, Tigerknows.HISTORY_MAX_SIZE);
            mTrafficAdapter.notifyDataSetChanged();
        }
        
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            mViewPager.setCurrentItem(0);
        } else {
            mViewPager.setCurrentItem(1);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mPOIList.clear();
        mTrafficList.clear();
        mPOIAdapter.notifyDataSetChanged();
        mTrafficAdapter.notifyDataSetChanged();
        mLayerType = ItemizedOverlay.POI_OVERLAY;
        mDismiss = true;
    }

    protected void findViews() {
        mPOIBtn = (Button) mRootView.findViewById(R.id.poi_btn);
        mTrafficBtn = (Button) mRootView.findViewById(R.id.traffic_btn);
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        
        List<View> viewList = new ArrayList<View>();
        Drawable divider = mSphinx.getResources().getDrawable(R.drawable.divider);
        mPOILsv = new SpringbackListView(mSphinx, null);
        mPOILsv.setFadingEdgeLength(0);
        mPOILsv.setScrollingCacheEnabled(false);
        mPOILsv.setDivider(divider);
        viewList.add(mPOILsv);
        mTrafficLsv = new SpringbackListView(mSphinx, null);
        mTrafficLsv.setFadingEdgeLength(0);
        mTrafficLsv.setScrollingCacheEnabled(false);
        mTrafficLsv.setDivider(divider);
        viewList.add(mTrafficLsv);
        mViewPager.setOnPageChangeListener(new MyPageChangeListener());
        mViewPager.setAdapter(new FavoriteFragment.MyAdapter(viewList));
        
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mPOILsv.addFooterView(v);
        v = mLayoutInflater.inflate(R.layout.loading, null);
        mTrafficLsv.addFooterView(v);
        mEmptyView = (TextView)mRootView.findViewById(R.id.empty_txv);
    }

    protected void setListener() {
        mPOIBtn.setOnClickListener(this);
        mTrafficBtn.setOnClickListener(this);
        
        mTrafficLsv.setOnCreateContextMenuListener(mContextMenuListener);
        mTrafficLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    History traffic = (History) adapterView.getAdapter().getItem(position);
                    if (traffic != null) {
                        mActionLog.addAction(ActionLog.HistorySelectTraffic, position);
                        showTrafficDetail(traffic);
                    }
                }
            }
        });

        mPOILsv.setOnCreateContextMenuListener(mContextMenuListener);
        mPOILsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    POI poi = (POI) adapterView.getAdapter().getItem(position);
                    if (poi != null) {
                        mActionLog.addAction(ActionLog.HistorySelectPOI, position);
                        mSphinx.getPOIDetailFragment().setData(poi);
                        mSphinx.showView(R.id.view_poi_detail);
                    }
                }
            }
        });

        mPOILsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader) {
                    return;
                }
                turnPagePOI();
            }
        });

        mTrafficLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader) {
                    return;
                }
                turnPageTraffic();
            }
        });
    }
    
    private void turnPagePOI() {
        synchronized (this) {
        mPOILsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        long maxId = Long.MAX_VALUE;
        int size = mPOIList.size();
        if (size > 0) {
            maxId = mPOIList.get(size-1).getDateTime();
        }
        LoadThread loadThread = new LoadThread();
        loadThread.layerType = ItemizedOverlay.POI_OVERLAY;
        loadThread.maxId = maxId;
        loadThread.start();
        mActionLog.addAction(ActionLog.HistoryMore, 0);
        }
    }

    private void turnPageTraffic() {
        synchronized (this) {
            mTrafficLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            long maxId = Long.MAX_VALUE;
            int size = mTrafficList.size();
            if (size > 0) {
                maxId = mTrafficList.get(size-1).getDateTime();
            }
            LoadThread loadThread = new LoadThread();
            loadThread.layerType = ItemizedOverlay.TRAFFIC_OVERLAY;
            loadThread.maxId = maxId;
            loadThread.start();
            mActionLog.addAction(ActionLog.HistoryMore, 1);
        }
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (mSelectIndex > -1) {
            switch (item.getItemId()) {
                case MENU_DELETE:
                    CommonUtils.showNormalDialog(mSphinx,
                            mContext.getString(R.string.prompt),
                            mContext.getString(mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? R.string.delete_all_history_poi : R.string.delete_all_history_traffic),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                                            mPOIList.get(mSelectIndex).deleteHistory(mSphinx);
                                            mPOILsv.setFooterSpringback(mPOIList.size() == Tigerknows.HISTORY_MAX_SIZE);
                                        } else {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.History.CONTENT_URI, "_id="+mTrafficList.get(mSelectIndex).getId(), null);
                                            mTrafficLsv.setFooterSpringback(mTrafficList.size() == Tigerknows.HISTORY_MAX_SIZE);
                                        }
                                        refresh(mLayerType);
                                        refreshContent();
                                        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                                            if (mPOIList.isEmpty()) {
                                                turnPagePOI();
                                            }
                                        } else {
                                            if (mTrafficList.isEmpty()) {
                                                turnPageTraffic();
                                            }
                                        }
                                    }
                                }
                            });
                    return true;
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.HistoryRightDelete, (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? "0" : "1"));

                int count = 0;
                if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                    count = mPOIList.size();
                } else {
                    count = mTrafficList.size();
                }
                if (count > 0) {
                    CommonUtils.showNormalDialog(mSphinx,
                            mContext.getString(R.string.prompt),
                            mContext.getString(mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? R.string.delete_all_history_poi : R.string.delete_all_history_traffic),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, Tigerknows.POI.STORE_TYPE + "=" + Tigerknows.STORE_TYPE_HISTORY, null);
                                            mPOIList.clear();
                                            mPOIAdapter.notifyDataSetChanged();
                                        } else {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.History.CONTENT_URI, null, null);
                                            mTrafficList.clear();
                                            mTrafficAdapter.notifyDataSetChanged();
                                        }
                                        refresh(mLayerType);
                                        refreshContent();
                                    }
                                }
                            });
                }
                break;
            case R.id.poi_btn:
                mActionLog.addAction(ActionLog.HistoryPOI);
                mViewPager.setCurrentItem(0);
                break;
                
            case R.id.traffic_btn:
                mActionLog.addAction(ActionLog.HistoryTraffic);
                mViewPager.setCurrentItem(1);
                break;
                
            default:
                break;
        }
    }
    
    private void showTrafficDetail(History traffic) {
        int type = traffic.getHistoryType();
        switch (type) {
            case Tigerknows.History.HISTORY_BUSLINE:
            	mSphinx.getBuslineDetailFragment().setData(traffic.getBuslineQuery().getBuslineModel().getLineList().get(0));
                mSphinx.showView(R.id.view_busline_result_detail);
                break;
                
            case Tigerknows.History.HISTORY_TRANSFER:
            case Tigerknows.History.HISTORY_DRIVE:
            case Tigerknows.History.HISTORY_WALK:
                mSphinx.getTrafficDetailFragment().setData(traffic.getTrafficQuery().getTrafficModel().getPlanList().get(0));
                mSphinx.showView(R.id.view_traffic_result_detail);
                break;

            default:
                break;
        }
    }
    
    private void readPOI(List<POI> list, long maxId, long minId, boolean next){
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, null, mPOIWhere + " AND (" + com.tigerknows.provider.Tigerknows.POI.DATETIME + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.POI.DATETIME + "<" + maxId+")", null, "_datetime DESC");
        int count = 0;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                POI poi;
                c.moveToFirst();
                maxId = 0;
                for(int i = 0; i<count; i++) {
                    poi = POI.readFormCursor(mContext, c);
                    if (list.contains(poi)) {
                        list.remove(poi);
                    }
                    list.add(poi);
                    maxId = poi.getDateTime();
                    c.moveToNext();
                }
                if (next)
                    readPOI(list, maxId, minId, next);
            }
            c.close();
        }
    }
    
    private void readTraffic(List<History> list, long maxId, long minId, boolean next){
        int count;
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.History.CONTENT_URI, null, "(" + com.tigerknows.provider.Tigerknows.History.DATETIME + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.History.DATETIME + "<" + maxId+")", null, "_datetime DESC");
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                History traffic;
                c.moveToFirst();
                maxId = 0;
                for(int i = 0; i<count; i++) {
                    traffic = History.readFormCursor(mContext, c);
                    if (traffic != null) {
                        if (list.contains(traffic)) {
                            list.remove(traffic);
                        }
                        list.add(traffic);
                        maxId = traffic.getDateTime();
                    }
                    c.moveToNext();
                }
                if (next)
                    readTraffic(list, maxId, minId, next);
            }
            c.close();
        }
    }
    
    public class TrafficAdapter extends ArrayAdapter<History>{
        private static final int sResource = R.layout.favorite_list_item;

        public TrafficAdapter(Context context, List<History> apps) {
            super(context, sResource, apps);
        }
     
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.favorite_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            final History traffic = this.getItem(position);
            
            TextView textView = (TextView) view.findViewById(R.id.text_txv);
            ImageView iconImv = (ImageView) view.findViewById(R.id.icon_imv);
            iconImv.setVisibility(View.VISIBLE);
            if (traffic.getHistoryType() == Tigerknows.History.HISTORY_BUSLINE) {
                iconImv.setBackgroundResource(R.drawable.rdb_busline_default);
            } else if (traffic.getHistoryType() == Tigerknows.History.HISTORY_TRANSFER) {
                iconImv.setBackgroundResource(R.drawable.rdb_bus_default);
            } else if (traffic.getHistoryType() == Tigerknows.History.HISTORY_DRIVE) {
                iconImv.setBackgroundResource(R.drawable.rdb_drive_default);
            } else if (traffic.getHistoryType() == Tigerknows.History.HISTORY_WALK) {
                iconImv.setBackgroundResource(R.drawable.rdb_walk_default);
            }
            
            String s = getTrafficName(traffic);
            textView.setText(s);
            
            return view;
        }
    }
    
    private String getTrafficName(History traffic) {
        StringBuilder s = new StringBuilder();
        if (traffic.getHistoryType() == com.tigerknows.provider.Tigerknows.History.HISTORY_BUSLINE) {
            BuslineQuery buslineQuery = traffic.getBuslineQuery();
            if (buslineQuery != null) {
                Line line = buslineQuery.getBuslineModel().getLineList().get(0);
                s.append(line.getName());
            }
        } else {
            TrafficQuery trafficQuery = traffic.getTrafficQuery();
            if (trafficQuery != null) {

                s.append(trafficQuery.getTrafficModel().getStartName());
                s.append(">");
                s.append(trafficQuery.getTrafficModel().getEndName());
            }
        }
        return s.toString();
    }
    
    private class POIAdapter extends ArrayAdapter<POI>{

        public POIAdapter(Context context, List<POI> poiList) {
            super(context, R.layout.favorite_list_item, poiList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.favorite_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            final POI poi = getItem(position);
            
            TextView textView = (TextView) view.findViewById(R.id.text_txv);

            poi.setOrderNumber(position+1);
            textView.setText(poi.getOrderNumber()+". "+poi.getName());

            return view;
        }
    }
    
    Handler mHandler = new Handler(){
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            if (0 == msg.what) {
                mPOILsv.onRefreshComplete(false);
                List<POI> poiList = (List<POI>)msg.obj;
                if (poiList.size() > 0) {
                    mRightBtn.setEnabled(true);
                    for(POI poi : poiList) {
                        if (mPOIList.contains(poi)) {
                            mPOIList.remove(poi);
                        }
                        mPOIList.add(poi);
                    }
                    Collections.sort(mPOIList, mComparator);
                    CommonUtils.keepListSize(mPOIList, Tigerknows.HISTORY_MAX_SIZE);
                    mPOIAdapter.notifyDataSetChanged();
                    mPOILsv.setFooterSpringback(mPOIList.size() < Tigerknows.HISTORY_MAX_SIZE);
                    if (mPOILsv.isFooterSpringback()) {
                        mSphinx.getHandler().postDelayed(mTurnPageRunPOI, 1000);
                    }
                } else {
                    mPOILsv.setFooterSpringback(false);
                }
                refreshContent();
            } else if (1 == msg.what) {
                mTrafficLsv.onRefreshComplete(false);
                List<History> trafficList = (List<History>)msg.obj;
                if (trafficList.size() > 0) {
                    mRightBtn.setEnabled(true);
                    for(History traffic : trafficList) {
                        if (mTrafficList.contains(traffic)) {
                            mTrafficList.remove(traffic);
                        }
                        mTrafficList.add(traffic);
                    }
                    Collections.sort(mTrafficList, mComparator);
                    CommonUtils.keepListSize(mTrafficList, Tigerknows.HISTORY_MAX_SIZE);
                    mTrafficAdapter.notifyDataSetChanged();
                    mTrafficLsv.setFooterSpringback(mTrafficList.size() < Tigerknows.HISTORY_MAX_SIZE);
                    if (mTrafficLsv.isFooterSpringback()) {
                        mSphinx.getHandler().postDelayed(mTurnPageRunTraffic, 1000);
                    }
                } else {
                    mTrafficLsv.setFooterSpringback(false);
                }
                refreshContent();
            }
        }
    };
    
    class LoadThread extends Thread{
        
        long maxId;
        String layerType;
        
        @Override
        public void run(){
            Message msg = Message.obtain();
            int type;
            if (layerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                List<POI> poiList = new ArrayList<POI>();
                readPOI(poiList, maxId, 0, false);
                type = 0;
                msg.obj = poiList;
            } else {
                List<History> trafficList = new ArrayList<History>();
                readTraffic(trafficList, maxId, 0, false);
                type = 1;
                msg.obj = trafficList;
            }
            msg.what = type;
            mHandler.sendMessage(msg);
        }
    }
    
    @SuppressWarnings("unchecked")
    public synchronized void refresh(String trafficType) {

        if (trafficType.equals(ItemizedOverlay.POI_OVERLAY)) {
            for(int i = mPOIList.size() - 1; i >= 0; i--) {
                if (!mPOIList.get(i).checkHistory(mContext)) {
                    mPOIList.remove(i);
                }
            }
            Collections.sort(mPOIList, mComparator);
            mPOIAdapter.notifyDataSetChanged();
        } else {
            for(int i = mTrafficList.size() - 1; i >= 0; i--) {
                if (!mTrafficList.get(i).checkHistory(mContext)) {
                    mTrafficList.remove(i);
                }
            }
            Collections.sort(mTrafficList, mComparator);
            mTrafficAdapter.notifyDataSetChanged();
        }
    }
    
    private void refreshContent() {

        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            mRightBtn.setEnabled(!mPOIList.isEmpty());
            
            if (mPOIList.isEmpty() && mPOILsv.isFooterSpringback() == false) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            mRightBtn.setEnabled(!mTrafficList.isEmpty());

            if (mTrafficList.isEmpty() && mTrafficLsv.isFooterSpringback() == false) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }

        mPOIAdapter.notifyDataSetChanged();
        mTrafficAdapter.notifyDataSetChanged();
    }
    
    private void refreshTab(String layerType) {

        mLayerType = layerType;
        refreshContent();
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            mPOIBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mTrafficBtn.setBackgroundResource(R.drawable.btn_tab);
            if (mPOILsv.isFooterSpringback()) {
                mHandler.postDelayed(mTurnPageRunPOI, 1000);
            } else {
                mPOILsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            }
        } else {
            mPOIBtn.setBackgroundResource(R.drawable.btn_tab);
            mTrafficBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            if (mTrafficLsv.isFooterSpringback()) {
                mHandler.postDelayed(mTurnPageRunTraffic, 1000);
            } else {
                mTrafficLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
            }
        }
    }

    @Override
    public boolean canTurnPage() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void turnPageStart() {
        // TODO Auto-generated method stub
        
    }
    
    class MyPageChangeListener implements OnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                refreshTab(ItemizedOverlay.POI_OVERLAY);
            } else {
                refreshTab(ItemizedOverlay.TRAFFIC_OVERLAY);
            }
        }
        
    }
}
/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.Favorite;
import com.tigerknows.model.POI;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.SqliteWrapper;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class FavoriteFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "FavoriteFragment";

    public FavoriteFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    public static final int MENU_DELETE             = 0;
    public static final int MENU_RENAME             = 1;
    
    private POIAdapter mPOIAdapter = null;

    private TrafficAdapter mTrafficAdapter = null;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
    private List<Favorite> mTrafficList = new ArrayList<Favorite>();
    
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
            return (int)(baseData2.getId() - baseData1.getId());
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
                menu.add(0, MENU_RENAME, 0, R.string.rename);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.Favorite;
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
        s.append(Tigerknows.STORE_TYPE_FAVORITE);
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
        mTitleBtn.setText(R.string.favorite);
        mRightBtn.setOnClickListener(this);
        mRightBtn.setText(R.string.clear_all);

        if (mDismiss) {
            mDismiss = false;
            readPOI(mPOIList, Long.MAX_VALUE, 0, false);
            readTraffic(mTrafficList, Long.MAX_VALUE, 0, false);
            mPOILsv.setFooterSpringback(mPOIList.size() >= TKConfig.getPageSize());
            mTrafficLsv.setFooterSpringback(mTrafficList.size() >= TKConfig.getPageSize());
            if (mPOIList.isEmpty() && mTrafficList.isEmpty() == false) {
                mLayerType = ItemizedOverlay.TRAFFIC_OVERLAY;
            }
            Toast.makeText(mContext, R.string.favorite_long_click_tip, 3000).show();
        } else {
            checkData(ItemizedOverlay.POI_OVERLAY);
            checkData(ItemizedOverlay.TRAFFIC_OVERLAY);
            if (mPOILsv.isFooterSpringback() == false) {
                long min = 0;
                if (mPOIList.size() > 0) {
                    min = mPOIList.get(0).getId();
                }
                readPOI(mPOIList, Long.MAX_VALUE, min, true);
            }
            
            if (mTrafficLsv.isFooterSpringback() == false) {
                long min = 0;
                if (mTrafficList.size() > 0) {
                    min = mTrafficList.get(0).getId();
                }
                readTraffic(mTrafficList, Long.MAX_VALUE, min, true);
            }
        }
        
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            if (mViewPager.getCurrentItem() != 0) {
                mViewPager.setCurrentItem(0);
            } else {
                changeTab(ItemizedOverlay.POI_OVERLAY);
            }
        } else {
            if (mViewPager.getCurrentItem() != 1) {
                mViewPager.setCurrentItem(1);
            } else {
                changeTab(ItemizedOverlay.TRAFFIC_OVERLAY);
            }
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
        mPOILsv.setFooterSpringback(false);
        mPOILsv.setDivider(divider);
        mPOILsv.setFooterDividersEnabled(false);
        viewList.add(mPOILsv);
        mTrafficLsv = new SpringbackListView(mSphinx, null);
        mTrafficLsv.setFadingEdgeLength(0);
        mTrafficLsv.setScrollingCacheEnabled(false);
        mTrafficLsv.setFooterSpringback(false);
        mTrafficLsv.setDivider(divider);
        mTrafficLsv.setFooterDividersEnabled(false);
        viewList.add(mTrafficLsv);
        mViewPager.setOnPageChangeListener(new MyPageChangeListener());
        mViewPager.setAdapter(new MyAdapter(viewList));
        
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
                    Favorite traffic = (Favorite) adapterView.getAdapter().getItem(position);
                    if (traffic != null) {
                        mActionLog.addAction(ActionLog.FavoriteSelectTraffic, position);
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
                        mActionLog.addAction(ActionLog.FavoriteSelectPOI, position);
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
            maxId = mPOIList.get(size-1).getId();
        }
        LoadThread loadThread = new LoadThread();
        loadThread.layerType = ItemizedOverlay.POI_OVERLAY;
        loadThread.maxId = maxId;
        loadThread.start();
        mActionLog.addAction(ActionLog.FavoriteMore, 0);
        }
    }

    private void turnPageTraffic() {
        synchronized (this) {
            mTrafficLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            long maxId = Long.MAX_VALUE;
            int size = mTrafficList.size();
            if (size > 0) {
            maxId = mTrafficList.get(size-1).getId();
            }
            LoadThread loadThread = new LoadThread();
            loadThread.layerType = ItemizedOverlay.TRAFFIC_OVERLAY;
            loadThread.maxId = maxId;
            loadThread.start();
        mActionLog.addAction(ActionLog.FavoriteMore, 1);
        }
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (mSelectIndex > -1) {
            switch (item.getItemId()) {
                case MENU_DELETE:
                    CommonUtils.showNormalDialog(mSphinx,
                            mContext.getString(R.string.prompt),
                            mContext.getString(mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? R.string.delete_all_favorite_poi : R.string.delete_all_favorite_traffic),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                                            mPOIList.get(mSelectIndex).deleteFavorite(mSphinx);
                                        } else {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, "_id="+mTrafficList.get(mSelectIndex).getId(), null);
                                        }
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
                case MENU_RENAME:
                    mActionLog.addAction(ActionLog.FavoriteRename);
                    showRenameDialog(mSelectIndex);
                    return true;
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {                
            case R.id.right_btn:
                mActionLog.addAction(ActionLog.FavoriteRightDelete, (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? "0" : "1"));

                int count = 0;
                if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                    count = mPOIList.size();
                } else {
                    count = mTrafficList.size();
                }
                if (count > 0) {
                    CommonUtils.showNormalDialog(mSphinx,
                            mContext.getString(R.string.prompt),
                            mContext.getString(mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? R.string.delete_all_favorite_poi : R.string.delete_all_favorite_traffic),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, Tigerknows.POI.STORE_TYPE + "="+Tigerknows.STORE_TYPE_FAVORITE, null);
                                            mPOIList.clear();
                                            mPOIAdapter.notifyDataSetChanged();
                                        } else {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, null, null);
                                            mTrafficList.clear();
                                            mTrafficAdapter.notifyDataSetChanged();
                                        }
                                        refreshContent();
                                    }
                                }
                            });
                }
                break;
            case R.id.poi_btn:
                mActionLog.addAction(ActionLog.FavoritePOI);
                mViewPager.setCurrentItem(0);
                break;
                
            case R.id.traffic_btn:
                mActionLog.addAction(ActionLog.FavoriteTraffic);
                mViewPager.setCurrentItem(1);
                break;
                
            default:
                break;
        }
    }
    
    private void showTrafficDetail(Favorite traffic) {
        int type = traffic.getFavoriteType();
        switch (type) {
            case Tigerknows.Favorite.FAVORITE_BUSLINE:
            	mSphinx.getBuslineDetailFragment().setData(traffic.getBuslineQuery().getBuslineModel().getLineList().get(0));
                mSphinx.showView(R.id.view_busline_result_detail);
                break;
                
            case Tigerknows.Favorite.FAVORITE_TRANSFER:
            case Tigerknows.Favorite.FAVORITE_DRIVE:
            case Tigerknows.Favorite.FAVORITE_WALK:
                mSphinx.getTrafficDetailFragment().setData(traffic.getTrafficQuery().getTrafficModel().getPlanList().get(0));
                mSphinx.showView(R.id.view_traffic_result_detail);
                break;

            default:
                break;
        }
    }
    
    private void readPOI(List<POI> list, long maxId, long minId, boolean next){
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, null, mPOIWhere + " AND (" + com.tigerknows.provider.Tigerknows.POI._ID + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.POI._ID + "<" + maxId+")", null, "_id DESC");
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
                    maxId = poi.getId();
                    c.moveToNext();
                }
                if (next)
                    readPOI(list, maxId, minId, next);
            }
            c.close();
        }
    }
    
    private void readTraffic(List<Favorite> list, long maxId, long minId, boolean next){
        int count;
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.Favorite.CONTENT_URI, null, "(" + com.tigerknows.provider.Tigerknows.Favorite._ID + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.Favorite._ID + "<" + maxId+")", null, "_id DESC");
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                Favorite traffic;
                c.moveToFirst();
                maxId = 0;
                for(int i = 0; i<count; i++) {
                    traffic = Favorite.readFormCursor(mContext, c);
                    if (traffic != null) {
                        if (list.contains(traffic)) {
                            list.remove(traffic);
                        }
                        list.add(traffic);
                        maxId = traffic.getId();
                    }
                    c.moveToNext();
                }
                if (next)
                    readTraffic(list, maxId, minId, next);
            }
            c.close();
        }
    }
    
    public class TrafficAdapter extends ArrayAdapter<Favorite>{
        private static final int sResource = R.layout.favorite_list_item;

        public TrafficAdapter(Context context, List<Favorite> apps) {
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
            
            final Favorite traffic = this.getItem(position);
            
            TextView textView = (TextView) view.findViewById(R.id.text_txv);
            ImageView iconImv = (ImageView) view.findViewById(R.id.icon_imv);
            iconImv.setVisibility(View.VISIBLE);
            if (traffic.getFavoriteType() == Tigerknows.Favorite.FAVORITE_BUSLINE) {
                iconImv.setBackgroundResource(R.drawable.rdb_busline_default);
            } else if (traffic.getFavoriteType() == Tigerknows.Favorite.FAVORITE_TRANSFER) {
                iconImv.setBackgroundResource(R.drawable.rdb_bus_default);
            } else if (traffic.getFavoriteType() == Tigerknows.Favorite.FAVORITE_DRIVE) {
                iconImv.setBackgroundResource(R.drawable.rdb_drive_default);
            } else if (traffic.getFavoriteType() == Tigerknows.Favorite.FAVORITE_WALK) {
                iconImv.setBackgroundResource(R.drawable.rdb_walk_default);
            }

            String s;
            if (TextUtils.isEmpty(traffic.getAlise())) {
                s = getTrafficName(traffic);
            } else {
                s = traffic.getAlise();
            }
            textView.setText(s);
            
            return view;
        }
    }
    
    private String getTrafficName(Favorite traffic) {
        StringBuilder s = new StringBuilder();
        if (traffic.getFavoriteType() == com.tigerknows.provider.Tigerknows.Favorite.FAVORITE_BUSLINE) {
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
            String name = poi.getAlise();
            if (TextUtils.isEmpty(name)) {
                name = poi.getName();
            }
            textView.setText(poi.getOrderNumber()+". "+name);

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
                    mPOIAdapter.notifyDataSetChanged();
                    mPOILsv.setFooterSpringback(poiList.size() >= TKConfig.getPageSize());
                    if (mPOILsv.isFooterSpringback()) {
                        mSphinx.getHandler().postDelayed(mTurnPageRunPOI, 1000);
                    }
                } else {
                    mPOILsv.setFooterSpringback(false);
                }
                refreshContent();
            } else if (1 == msg.what) {
                mTrafficLsv.onRefreshComplete(false);
                List<Favorite> trafficList = (List<Favorite>)msg.obj;
                if (trafficList.size() > 0) {
                    mRightBtn.setEnabled(true);
                    for(Favorite traffic : trafficList) {
                        if (mTrafficList.contains(traffic)) {
                            mTrafficList.remove(traffic);
                        }
                        mTrafficList.add(traffic);
                    }
                    Collections.sort(mTrafficList, mComparator);
                    mTrafficAdapter.notifyDataSetChanged();
                    mTrafficLsv.setFooterSpringback(trafficList.size() >= TKConfig.getPageSize());
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
                List<Favorite> trafficList = new ArrayList<Favorite>();
                readTraffic(trafficList, maxId, 0, false);
                type = 1;
                msg.obj = trafficList;
            }
            msg.what = type;
            mHandler.sendMessage(msg);
        }
    }
    
    @SuppressWarnings("unchecked")
    public synchronized void checkData(String trafficType) {

        if (trafficType.equals(ItemizedOverlay.POI_OVERLAY)) {
            for(int i = mPOIList.size() - 1; i >= 0; i--) {
                if (!mPOIList.get(i).checkFavorite(mContext)) {
                    mPOIList.remove(i);
                }
            }
            Collections.sort(mPOIList, mComparator);
            mPOIAdapter.notifyDataSetChanged();
        } else {
            for(int i = mTrafficList.size() - 1; i >= 0; i--) {
                if (!mTrafficList.get(i).checkFavorite(mContext)) {
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
                mEmptyView.setText(R.string.favorite_empty_poi);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            mRightBtn.setEnabled(!mTrafficList.isEmpty());

            if (mTrafficList.isEmpty() && mTrafficLsv.isFooterSpringback() == false) {
                mEmptyView.setText(R.string.favorite_empty_traffic);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }

        mPOIAdapter.notifyDataSetChanged();
        mTrafficAdapter.notifyDataSetChanged();
    }
    
    private void changeTab(String layerType) {

        mLayerType = layerType;
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            mPOIBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mTrafficBtn.setBackgroundResource(R.drawable.btn_tab);

            if (mPOIList.size() > 0) {
                readPOI(mPOIList, Long.MAX_VALUE, mPOIList.get(0).getDateTime(), true);
            } else {
                readPOI(mPOIList, Long.MAX_VALUE, 0, false);
                mPOILsv.setFooterSpringback(mPOIList.size() >= TKConfig.getPageSize());
            }
            Collections.sort(mPOIList, mComparator);
            mPOIAdapter.notifyDataSetChanged();
        } else {
            mPOIBtn.setBackgroundResource(R.drawable.btn_tab);
            mTrafficBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            
            if (mTrafficList.size() > 0) {
                readTraffic(mTrafficList, Long.MAX_VALUE, mTrafficList.get(0).getDateTime(), true);
            } else {
                readTraffic(mTrafficList, Long.MAX_VALUE, 0, false);
                mTrafficLsv.setFooterSpringback(mTrafficList.size() >= TKConfig.getPageSize());
            }
            Collections.sort(mTrafficList, mComparator);
            mTrafficAdapter.notifyDataSetChanged();
        }
        refreshContent();
        mPOILsv.changeHeaderViewByState(false, SpringbackListView.DONE);
        mTrafficLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
    }
    
    private void showRenameDialog(final int position) {
        View textEntryView = mLayoutInflater.inflate(R.layout.alert_favorite_rename, null);
        final EditText nameEdt = (EditText) textEntryView.findViewById(R.id.name_edt);
        String name;
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            POI poi = mPOIList.get(position);
            String aliseName = poi.getAlise();
            if (TextUtils.isEmpty(aliseName)) {
                name = poi.getName();
            } else {
                name = aliseName;
            }
        } else {
            Favorite traffic = mTrafficList.get(position);
            String aliseName = traffic.getAlise();
            if (TextUtils.isEmpty(aliseName)) {
                name = getTrafficName(traffic);
            } else {
                name = aliseName;
            }
        }
        nameEdt.append(name);
        nameEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    mSphinx.hideSoftInput(nameEdt.getWindowToken());
                    return true;
                }
                return false;
            }
        });
        Dialog dialog = new AlertDialog.Builder(mSphinx)
            .setTitle(R.string.rename)
            .setView(textEntryView)
            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mSphinx.hideSoftInput(nameEdt.getWindowToken());
                    if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                        POI poi = mPOIList.get(position);
                        poi.setAlise(nameEdt.getEditableText().toString());
                        poi.updateAlias(mContext);
                        mPOIAdapter.notifyDataSetChanged();
                    } else {
                        Favorite traffic = mTrafficList.get(position);
                        traffic.setAlise(nameEdt.getEditableText().toString());
                        traffic.updateAlias(mContext);
                        mTrafficAdapter.notifyDataSetChanged();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mSphinx.hideSoftInput(nameEdt.getWindowToken());
                }
            })
            .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
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
                changeTab(ItemizedOverlay.POI_OVERLAY);
            } else {
                changeTab(ItemizedOverlay.TRAFFIC_OVERLAY);
            }
        }
        
    }
    
    public static class MyAdapter extends PagerAdapter {
        
        List<View> mViewList;
        
        public MyAdapter(List<View> viewList) {
            mViewList = viewList;
        }
        
        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public void destroyItem(View contain, int position, Object arg2) {
             ((ViewPager) contain).removeView(mViewList.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup contain, int position) {
            contain.addView(mViewList.get(position), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            return mViewList.get(position);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
            // TODO Auto-generated method stub
        }

        @Override
        public Parcelable saveState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void finishUpdate(View arg0) {
            // TODO Auto-generated method stub
        }

    }
}
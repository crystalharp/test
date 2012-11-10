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
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Plan.Step;
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
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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

    private static final int STATE_NORMAL = 0;
    private static final int STATE_DELETE = 1;
    
    public static final int MENU_DELETE             = 0;
    
    private int mState = STATE_NORMAL;

    private POIAdapter mPOIAdapter = null;

    private TrafficAdapter mTrafficAdapter = null;
    
    private List<POI> mPOIList = new ArrayList<POI>();
    
    private List<History> mTrafficList = new ArrayList<History>();
    
    private Button mPOIBtn;
    
    private Button mTrafficBtn;
    
    private View mBottomView;
    
    private Button mDeleteBtn;
    
    private Button mAllSelectBtn;
    
    private SpringbackListView mPOILsv = null;
    
    private TextView mPOIEmptyView;
    
    private TextView mTrafficEmptyView;
    
    private View mPOIView;
    
    private View mTrafficView;

    private SpringbackListView mTrafficLsv = null;
    
    private String mLayerType = ItemizedOverlay.POI_OVERLAY;
    
    private String mPOIWhere;
    
    private Drawable mAllSelectDrawable;    
    private Drawable mCancelAllSelectDrawable;
    
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

            if (mState == STATE_NORMAL) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                if (info.position > -1 && info.position < (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? mPOIAdapter.getCount() : mTrafficAdapter.getCount())) {
                    mSelectIndex = info.position;
                    menu.add(0, MENU_DELETE, 0, R.string.delete);
                }
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.History;
        mAllSelectDrawable = getResources().getDrawable(R.drawable.ic_all_select);
        mCancelAllSelectDrawable = getResources().getDrawable(R.drawable.ic_cancel_all_select);
        mAllSelectDrawable.setBounds(0, 0, mAllSelectDrawable.getIntrinsicWidth(), mAllSelectDrawable.getIntrinsicHeight());
        mCancelAllSelectDrawable.setBounds(0, 0, mCancelAllSelectDrawable.getIntrinsicWidth(), mCancelAllSelectDrawable.getIntrinsicHeight());
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
        mLeftTxv.setOnClickListener(this);
        mRightTxv.setOnClickListener(this);
        mRightTxv.setText(R.string.delete);
        
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
        
        refreshTab(mLayerType);
        setState(STATE_NORMAL, false);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void findViews() {
        mPOIBtn = (Button) mRootView.findViewById(R.id.poi_btn);
        mTrafficBtn = (Button) mRootView.findViewById(R.id.traffic_btn);
        mPOILsv = (SpringbackListView) mRootView.findViewById(R.id.poi_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mPOILsv.addFooterView(v);
        mTrafficLsv = (SpringbackListView) mRootView.findViewById(R.id.traffic_lsv);
        v = mLayoutInflater.inflate(R.layout.loading, null);
        mTrafficLsv.addFooterView(v);
        mBottomView = (View) mRootView.findViewById(R.id.bottom_view);
        mDeleteBtn = (Button) mRootView.findViewById(R.id.delete_btn);
        mAllSelectBtn = (Button) mRootView.findViewById(R.id.all_select_btn);
        mPOIEmptyView = (TextView)mRootView.findViewById(R.id.poi_empty_txv);
        mTrafficEmptyView = (TextView)mRootView.findViewById(R.id.traffic_empty_txv);
        mPOIView = mRootView.findViewById(R.id.poi_view);
        mTrafficView = mRootView.findViewById(R.id.traffic_view);
    }

    protected void setListener() {
        mPOIBtn.setOnClickListener(this);
        mTrafficBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mAllSelectBtn.setOnClickListener(this);
        
        mTrafficLsv.setOnCreateContextMenuListener(mContextMenuListener);
        mTrafficLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    History traffic = (History) adapterView.getAdapter().getItem(position);
                    if (traffic != null) {
                        mActionLog.addAction(ActionLog.HistorySelectTraffic, position);
                        if (mState == STATE_DELETE) {
                            traffic.setSelected(!traffic.isSelected());
                            mTrafficAdapter.notifyDataSetChanged();
                            refreshAllSelectBtn();
                        } else {
                            showTrafficDetail(traffic);
                        }
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
                        if (mState == STATE_DELETE) {
                            poi.setSelected(!poi.isSelected());
                            mPOIAdapter.notifyDataSetChanged();
                            refreshAllSelectBtn();
                        } else {
                            mSphinx.getPOIDetailFragment().setData(poi);
                            mSphinx.showView(R.id.view_poi_detail);
                        }
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
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, "_id="+mPOIList.get(mSelectIndex).getId(), null);
                                            mPOILsv.setFooterSpringback(true);
                                        } else {
                                            SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.History.CONTENT_URI, "_id="+mTrafficList.get(mSelectIndex).getId(), null);
                                            mTrafficLsv.setFooterSpringback(true);
                                        }
                                        refresh(mLayerType);
                                        setState(mState, false);
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
        boolean allSelcet = true;
        switch (view.getId()) {
            case R.id.left_txv:
                if (mState == STATE_NORMAL) {
                    mSphinx.uiStackBack();
                } else {
                    setState(STATE_NORMAL, true);
                }
                break;
                
            case R.id.right_txv:
                if (mState == STATE_NORMAL) {
                    mActionLog.addAction(ActionLog.HistoryRightDelete, (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? "0" : "1"));
                    setState(STATE_DELETE, false);
                }
                break;
            case R.id.poi_btn:
                mActionLog.addAction(ActionLog.HistoryPOI);
                refreshTab(ItemizedOverlay.POI_OVERLAY);
                setState(STATE_NORMAL, true);
                if (mPOILsv.isFooterSpringback()) {
                    mHandler.postDelayed(mTurnPageRunPOI, 1000);
                } else {
                    mPOILsv.changeHeaderViewByState(false, SpringbackListView.DONE);
                }
                break;
                
            case R.id.traffic_btn:
                mActionLog.addAction(ActionLog.HistoryTraffic);
                refreshTab(ItemizedOverlay.TRAFFIC_OVERLAY);
                setState(STATE_NORMAL, true);
                if (mTrafficLsv.isFooterSpringback()) {
                    mHandler.postDelayed(mTurnPageRunTraffic, 1000);
                } else {
                    mTrafficLsv.changeHeaderViewByState(false, SpringbackListView.DONE);
                }
                break;
                
            case R.id.delete_btn:
                mActionLog.addAction(ActionLog.HistoryDelete, (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? "0" : "1"));
                int selectCount = 0;
                if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                    for(POI poi : mPOIList) {
                        if (poi.isSelected()) {
                            selectCount++;
                        }
                    }
                } else {
                    for(History traffic : mTrafficList) {
                        if (traffic.isSelected()) {
                            selectCount++;
                        }
                    }
                }
                
                if (selectCount > 0) {
                    CommonUtils.showNormalDialog(mSphinx,
                            mContext.getString(R.string.prompt),
                            mContext.getString(mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? R.string.delete_all_history_poi : R.string.delete_all_history_traffic),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
                                            for(POI poi : mPOIList) {
                                                if (poi.isSelected()) {
                                                    SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, "_id="+poi.getId(), null);
                                                }
                                            }
                                        } else {
                                            for(History traffic : mTrafficList) {
                                                if (traffic.isSelected()) {
                                                    SqliteWrapper.delete(mContext, mContext.getContentResolver(), Tigerknows.History.CONTENT_URI, "_id="+traffic.getId(), null);
                                                }
                                            }
                                        }
                                        refresh(mLayerType);
                                        setState(mState, false);
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
                } else {
                    Toast.makeText(mSphinx, R.string.no_select_tip, Toast.LENGTH_SHORT).show();
                }
                break;
                
            case R.id.all_select_btn:
                mActionLog.addAction(ActionLog.HistoryAllSelect, (mLayerType.equals(ItemizedOverlay.POI_OVERLAY) ? "0" : "1"));
                if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {

                    for(POI poi1 : mPOIList) {
                        if (!poi1.isSelected()) {
                            allSelcet = false;
                            break;
                        }
                    }
                    
                    for(POI poi : mPOIList) {
                        poi.setSelected(!allSelcet);
                    }
                    mPOIAdapter.notifyDataSetChanged();
                    refreshAllSelectBtn();
                } else {

                    for(History traffic : mTrafficList) {
                        if (!traffic.isSelected()) {
                            allSelcet = false;
                            break;
                        }
                    }
                    
                    for(History traffic : mTrafficList) {
                        traffic.setSelected(!allSelcet);
                    }
                    mTrafficAdapter.notifyDataSetChanged();
                    refreshAllSelectBtn();
                }  
                break;
                
            default:
                break;
        }
    }
    
    private void showTrafficDetail(History traffic) {
        if (mBottomView.getVisibility() == View.VISIBLE) {
            return;
        }
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
        private String[] typeNames;

        public TrafficAdapter(Context context, List<History> apps) {
            super(context, sResource, apps);
            typeNames = context.getResources().getStringArray(R.array.favorite_traffic_type);
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
            final CheckBox selectChb = (CheckBox) view.findViewById(R.id.select_chb);
            ImageView renameImv = (ImageView) view.findViewById(R.id.rename_imv);
            renameImv.setVisibility(View.INVISIBLE);
            
            StringBuilder s = new StringBuilder();
            if (traffic.getHistoryType() == com.tigerknows.provider.Tigerknows.History.HISTORY_BUSLINE) {
            	BuslineQuery buslineQuery = traffic.getBuslineQuery();
                if (buslineQuery != null) {
                    Line line = buslineQuery.getBuslineModel().getLineList().get(0);
                    s.append(line.getName());
                    s.append(",");
                    s.append(mContext.getString(R.string.busline_line_listitem_title, CommonUtils.meter2kilometre(line.getLength()), line.getStationList().size()));
                }
            } else {
                TrafficQuery trafficQuery = traffic.getTrafficQuery();
                if (trafficQuery != null) {

                    s.append(trafficQuery.getTrafficModel().getStartName());
                    s.append(">");
                    s.append(trafficQuery.getTrafficModel().getEndName());
                    s.append(",");
                    
                    TrafficModel trafficModel = trafficQuery.getTrafficModel();
                    int type = trafficModel.getType();
                    Plan plan = trafficModel.getPlanList().get(0);
                    if (type == Step.TYPE_TRANSFER) {
                        String busName = "";
                        int transferTimes = -1;

                        List<Step> stepList = plan.getStepList();
                        for(Step step : stepList) {
                            if (Step.TYPE_TRANSFER == step.getType()) {
                                if (!TextUtils.isEmpty(busName)) {
                                    busName = busName + ">" + step.getTransferLineName();
                                } else {
                                    busName = step.getTransferLineName();
                                }
                                transferTimes++;
                            }
                        }

                        s.append(busName);
                        s.append(",");
                        if (transferTimes > 0) { 
                            s.append(mContext.getString(R.string.traffic_transfer_title, transferTimes, CommonUtils.meter2kilometre(plan.getLength())));
                        } else {
                            s.append(mContext.getString(R.string.traffic_nonstop_title, CommonUtils.meter2kilometre(plan.getLength())));
                        }
                    } else {
//                        s.append(mContext.getString(R.string.traffic_drive_title, CommonUtils.meter2kilometre(plan.getLength())));
                        
                        if (plan.getLength() > 1000) {
                        	s.append(mContext.getString(R.string.traffic_result_length_km, CommonUtils.meter2kilometre(plan.getLength())));
                    	} else {
                    		s.append(mContext.getString(R.string.traffic_result_length_m, plan.getLength()));
                    	}
                    }
                }
            }
            textView.setText((position+1)+". "+typeNames[traffic.getHistoryType()-2]+": "+s.toString());
            
            if (mState == STATE_DELETE) {
                selectChb.setVisibility(View.VISIBLE);
                selectChb.setChecked(traffic.isSelected());
                selectChb.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View arg0) {
                        traffic.setSelected(selectChb.isChecked());
                        refreshAllSelectBtn();
                    }
                });
            } else {
                selectChb.setVisibility(View.INVISIBLE);
            }
            
            return view;
        }
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
            final CheckBox selectChb = (CheckBox) view.findViewById(R.id.select_chb);
            ImageView renameImv = (ImageView) view.findViewById(R.id.rename_imv);
            renameImv.setVisibility(View.INVISIBLE);

            poi.setOrderNumber(position+1);
            textView.setText(poi.getOrderNumber()+". "+poi.getName());
            
            if (mState == STATE_DELETE) {
                selectChb.setVisibility(View.VISIBLE);
                selectChb.setChecked(poi.isSelected());
                selectChb.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View arg0) {
                        poi.setSelected(selectChb.isChecked());
                        refreshAllSelectBtn();
                    }
                });
            } else {
                selectChb.setVisibility(View.INVISIBLE);
            }

            return view;
        }
    }
    
    private void refreshAllSelectBtn() {
        boolean allSelcet = true;
        boolean enabled = false;
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            for(POI poi1 : mPOIList) {
                if (!poi1.isSelected()) {
                    allSelcet = false;
                    break;
                }
            }
            enabled = !mPOIList.isEmpty();
        } else {
            for(History traffic : mTrafficList) {
                if (!traffic.isSelected()) {
                    allSelcet = false;
                    break;
                }
            }
            enabled = !mTrafficList.isEmpty();
        }
        mAllSelectBtn.setEnabled(enabled);
        mAllSelectBtn.setText(allSelcet ? R.string.cancel_all_select : R.string.all_select);
        mAllSelectBtn.setCompoundDrawables(allSelcet ? mCancelAllSelectDrawable : mAllSelectDrawable, null, null, null);
    }
    
    Handler mHandler = new Handler(){
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            if (0 == msg.what) {
                mPOILsv.onRefreshComplete(false);
                List<POI> poiList = (List<POI>)msg.obj;
                if (poiList.size() > 0) {
                    mRightTxv.setEnabled(true);
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
                setState(mState, false);
            } else if (1 == msg.what) {
                mTrafficLsv.onRefreshComplete(false);
                List<History> trafficList = (List<History>)msg.obj;
                if (trafficList.size() > 0) {
                    mRightTxv.setEnabled(true);
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
                setState(mState, false);
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

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mState == STATE_DELETE) { 
                mActionLog.addAction(ActionLog.KeyCodeBack);
                setState(STATE_NORMAL, true);
                return true;
            }
        }
        return false;
    }  
    
    private void setState(int state, boolean cancelSelected) {
        
        mState = state;
        if (mState == STATE_NORMAL) {
            mBottomView.setVisibility(View.GONE);
            mTitleTxv.setText(R.string.history_browse);
            mRightTxv.setVisibility(View.VISIBLE);
        } else if (mState == STATE_DELETE) {            
            mBottomView.setVisibility(View.VISIBLE);
            mTitleTxv.setText(R.string.delete);
            mRightTxv.setVisibility(View.INVISIBLE);
        }

        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            if (cancelSelected) {
                for(POI poi1 : mPOIList) {
                    poi1.setSelected(false);
                }
            }
            mRightTxv.setEnabled(!mPOIList.isEmpty());
            mDeleteBtn.setEnabled(!mPOIList.isEmpty());
            
            if (mPOIList.isEmpty() && mPOILsv.isFooterSpringback() == false) {
                mPOIEmptyView.setVisibility(View.VISIBLE);
            } else {
                mPOIEmptyView.setVisibility(View.GONE);
            }
        } else {
            if (cancelSelected) {
                for(History traffic : mTrafficList) {
                    traffic.setSelected(false);
                }
            }
            mRightTxv.setEnabled(!mTrafficList.isEmpty());
            mDeleteBtn.setEnabled(!mTrafficList.isEmpty());

            if (mTrafficList.isEmpty() && mTrafficLsv.isFooterSpringback() == false) {
                mTrafficEmptyView.setVisibility(View.VISIBLE);
            } else {
                mTrafficEmptyView.setVisibility(View.GONE);
            }
        }

        mPOIAdapter.notifyDataSetChanged();
        mTrafficAdapter.notifyDataSetChanged();
        refreshAllSelectBtn();
    }
    
    private void refreshTab(String layerType) {

        mLayerType = layerType;
        if (mLayerType.equals(ItemizedOverlay.POI_OVERLAY)) {
            mPOIBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mTrafficBtn.setBackgroundResource(R.drawable.btn_tab);
            mPOIView.setVisibility(View.VISIBLE);
            mTrafficView.setVisibility(View.GONE);
        } else {
            mPOIBtn.setBackgroundResource(R.drawable.btn_tab);
            mTrafficBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mPOIView.setVisibility(View.GONE);
            mTrafficView.setVisibility(View.VISIBLE);
        }
    }    
}
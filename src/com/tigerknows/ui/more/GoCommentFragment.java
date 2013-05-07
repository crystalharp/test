/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.POIResultFragment.POIAdapter;
import com.tigerknows.util.SqliteWrapper;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class GoCommentFragment extends BaseFragment implements View.OnClickListener {
    
    static final String TAG = "GoCommentFragment";
    
    private static final int REQUEST_FAVORITE = 0;
    private static final int REQUEST_HISTORY = 1;
    private static final int REQUEST_OTHER = 2;
    
    public GoCommentFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private POIAdapter mPOIAdapter = null;

    private List<POI> mPOIList = new ArrayList<POI>();
    
    private SpringbackListView mPOILsv = null;
    
    private View mEmptyView;
    
    private Button mInputBtn;
    
    private static final String FAVORITE_WHERE = "("+Tigerknows.POI.STORE_TYPE+"="+Tigerknows.STORE_TYPE_FAVORITE+")";
    
    private static final String HISTORY_WHERE = "("+Tigerknows.POI.STORE_TYPE+"="+Tigerknows.STORE_TYPE_HISTORY+")";
    
    private int request = REQUEST_FAVORITE;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mPOILsv.getLastVisiblePosition() >= mPOILsv.getCount()-2 &&
                    mPOILsv.getFirstVisiblePosition() == 0) {
                mPOILsv.getView(false).performClick();
            }
        }
    };
    
    private DataQuery mDataQuery;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.GoComment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = mLayoutInflater.inflate(R.layout.more_go_comment, container, false);
        
        findViews();
        setListener();
        
        mPOIAdapter = new POIAdapter(mSphinx, mPOIList, null, GoCommentFragment.this.toString());
        mPOIAdapter.setShowStamp(false);
        mPOILsv.setAdapter(mPOIAdapter);
                
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.go_comment);
        mRightBtn.setVisibility(View.GONE);
        if (isReLogin()) {
            return;
        }
        mEmptyView.setVisibility(View.GONE);
        if (mPOIList.isEmpty()) {
            request = REQUEST_FAVORITE;         
            mPOILsv.setFooterSpringback(false);
            turnPage();
        } else if (mPOILsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
        mPOIAdapter.notifyDataSetChanged();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mPOIList.clear();
        mPOIAdapter.notifyDataSetChanged();
        request = REQUEST_FAVORITE;
        mDataQuery = null;
        mEmptyView.setVisibility(View.GONE);
    }

    protected void findViews() {
        mInputBtn = (Button) mRootView.findViewById(R.id.input_btn);
        mPOILsv = (SpringbackListView) mRootView.findViewById(R.id.poi_lsv);
        mEmptyView = mRootView.findViewById(R.id.empty_view);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
        mPOILsv.addFooterView(v);
    }

    protected void setListener() {
        mInputBtn.setOnClickListener(this);
        
        mPOILsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position < mPOIList.size()) {
                    POI poi = mPOIList.get(position);
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position, poi.getUUID(), poi.getName());
                    mSphinx.getPOIDetailFragment().setData(poi);
                    mSphinx.showView(R.id.view_poi_detail);
                }
            }
        });

        mPOILsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                if (isHeader) {
                    return;
                }
                mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);
                turnPage();
            }
        });
    }

    @Override
    public void onClick(View view) {
        mActionLog.addAction(mActionTag +  ActionLog.GoCommentInput);
        mSphinx.showView(R.id.view_poi_input_search);
    }
    
    private void queryPOIByFavorite(List<POI> list, long maxId, long minId, boolean next){
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, null, FAVORITE_WHERE + " AND (" + com.tigerknows.provider.Tigerknows.POI._ID + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.POI._ID + "<" + maxId+")", null, "_id DESC");
        int count = 0;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                POI poi;
                c.moveToFirst();
                maxId = 0;
                int add = 0;
                for(int i = 0; i<count; i++) {
                    poi = POI.readFormCursor(mContext, c);
                    if (!mPOIList.contains(poi)) {
                        list.add(poi);
                        add++;
                    }
                    maxId = poi.getId();
                    c.moveToNext();
                }
                if (next || add == 0)
                    queryPOIByFavorite(list, maxId, minId, next);
            }
            c.close();
        }
    }
    
    private void queryPOIByHistory(List<POI> list, long maxId, long minId, boolean next) {
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, null, HISTORY_WHERE + " AND (" + com.tigerknows.provider.Tigerknows.POI.DATETIME + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.POI.DATETIME + "<" + maxId+")", null, "_datetime DESC");
        int count = 0;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                POI poi;
                c.moveToFirst();
                maxId = 0;
                int add = 0;
                for(int i = 0; i<count; i++) {
                    poi = POI.readFormCursor(mContext, c);
                    if (!mPOIList.contains(poi)) {
                        list.add(poi);
                        add++;
                    }
                    maxId = poi.getDateTime();
                    c.moveToNext();
                }
                if (next || add == 0)
                    queryPOIByHistory(list, maxId, minId, next);
            }
            c.close();
        }
    }

    private void turnPage(){
        synchronized (this) {
        mPOILsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        if (REQUEST_FAVORITE == request) {
            long maxId = Long.MAX_VALUE;
            int size = mPOIList.size();
            if (size > 0) {
                maxId = mPOIList.get(size-1).getId();
            }
            LoadThread loadThread = new LoadThread();
            loadThread.maxId = maxId;
            loadThread.request = request;
            loadThread.start();
        } else if (REQUEST_HISTORY == request) {
            long maxId = Long.MAX_VALUE;
            int size = mPOIList.size();
            if (size > 0) {
                maxId = mPOIList.get(size-1).getDateTime();
            }
            LoadThread loadThread = new LoadThread();
            loadThread.maxId = maxId;
            loadThread.request = request;
            loadThread.start();
        } else if (REQUEST_OTHER == request) {
            DataQuery dataQuery = mDataQuery;
            if (dataQuery == null) {
                DataQuery poiQuery = new DataQuery(mContext);
                POI requestPOI = mSphinx.getPOI();
                int cityId = Globals.g_Current_City_Info.getId();
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
                criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
                criteria.put(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_MY_COMMENT);
                Position position = requestPOI.getPosition();
                if (position != null) {
                    criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                    criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
                }
                poiQuery.setup(criteria, cityId, getId(), getId(), null, false, false, requestPOI);
                mSphinx.queryStart(poiQuery); 
            } else {
                POIList poiList = ((POIResponse)dataQuery.getResponse()).getBPOIList(); 
                Hashtable<String, String> criteria = dataQuery.getCriteria();
                long total = poiList.getTotal();
                int index = Integer.parseInt(criteria.get(BaseQuery.SERVER_PARAMETER_INDEX));
                if (index + poiList.getList().size() < total) {
                    DataQuery poiQuery = new DataQuery(mContext);
                    POI requestPOI = dataQuery.getPOI();
                    int cityId = dataQuery.getCityId();
                    criteria.put(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(poiList.getList().size()));
                    BaseQuery.passLocationToCriteria(dataQuery.getCriteria(), criteria);
                    poiQuery.setup(criteria, cityId, getId(), getId(), null, true, true, requestPOI);
                    mSphinx.queryStart(poiQuery); 
                }
            }       
        }
        }
    }
    
    private void queryPOIByOnline() {
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
        criteria.put(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_MY_COMMENT);
        POI requestPOI = mSphinx.getPOI();
        Position position = requestPOI.getPosition();
        if (position != null) {
            criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
        int cityId = Globals.g_Current_City_Info.getId();
        DataQuery poiQuery = new DataQuery(mContext);
        poiQuery.setup(criteria, cityId, getId(), getId(), null, false, false, requestPOI);
        mSphinx.queryStart(poiQuery); 
    }
    
    Handler mHandler = new Handler(){
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            List<POI> poiList = (List<POI>)msg.obj;
            if (poiList.size() > 0) {
                mPOIList.addAll(poiList);
                mPOIAdapter.notifyDataSetChanged();
                mPOILsv.setFooterSpringback(true);
                mPOILsv.onRefreshComplete(false);
                if (poiList.size() < TKConfig.getPageSize()) {
                    turnPage();
                } else {
                    mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
                }
            } else {
                if (msg.what == REQUEST_FAVORITE) {
                    request = REQUEST_HISTORY;
                    LoadThread loadThread = new LoadThread();
                    loadThread.maxId = Long.MAX_VALUE;
                    loadThread.request = request;
                    mPOILsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
                    loadThread.start();
                } else if (msg.what == REQUEST_HISTORY) {
                    request = REQUEST_OTHER;
                    mPOILsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
                    queryPOIByOnline();
                }
            }
        }
    };
    
    class LoadThread extends Thread{
        
        long maxId;
        int request;
        
        @Override
        public void run(){
            Message msg = Message.obtain();
            msg.what = request;
            if (request == REQUEST_FAVORITE) {
                List<POI> poiList = new ArrayList<POI>();
                queryPOIByFavorite(poiList, maxId, 0, false);
                poiList = refreshPOIStatus(poiList);
                msg.obj = poiList;
            } else if (request == REQUEST_HISTORY) {
                List<POI> poiList = new ArrayList<POI>();
                queryPOIByHistory(poiList, maxId, 0, false);
                poiList = refreshPOIStatus(poiList);
                msg.obj = poiList;
            }
            mHandler.sendMessage(msg);
        }
    }
    
    private List<POI> refreshPOIStatus(List<POI> poiList) {
        List<POI> list = new ArrayList<POI>();
        StringBuilder idList =  new StringBuilder();
        for(int i = 0, size = poiList.size(); i < size; i++) {
            POI poi = poiList.get(i);
            String uuid = poi.getUUID();
            if (TextUtils.isEmpty(uuid) == false) {
                if (idList.length() > 0) {
                    idList.append(',');
                }
                idList.append(uuid);
            }
        }
        if (idList.length() > 0) {
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            criteria.put(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            int cityId = Globals.g_Current_City_Info.getId();
            DataQuery poiQuery = new DataQuery(mContext);
            criteria.put(DataQuery.SERVER_PARAMETER_ID_LIST, idList.toString());
            poiQuery.setup(criteria, cityId, GoCommentFragment.this.getId(), GoCommentFragment.this.getId(), null, false, false, null);
            poiQuery.query();
            Response response = poiQuery.getResponse();
            if (response == null || response.getResponseCode() != Response.RESPONSE_CODE_OK) {
                return list;
            }
            POIResponse poiResponse = (POIResponse) poiQuery.getResponse();
            List<Integer> statusList = poiResponse.getIdList();
            if (statusList != null && poiList.size() == statusList.size()) {
                int i = 0;
                for(int status : statusList) {
                    if ((status & 1) == 0 && (status & 2) == 0) {
                        list.add(poiList.get(i));
                    }
                    i++;
                }
            }
        }
        return list;
    }
    
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        mPOILsv.onRefreshComplete(false);
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = ((DataQuery)tkAsyncTask.getBaseQuery());

        boolean exit = true;
        if (dataQuery.getCriteria().containsKey(BaseQuery.SERVER_PARAMETER_INDEX)) {
            int index = Integer.parseInt(dataQuery.getCriteria().get(BaseQuery.SERVER_PARAMETER_INDEX));
            if (index > 0) {
                mPOILsv.onRefreshComplete(false);
                mPOILsv.setFooterSpringback(true);
                exit = false;
            }
        }
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mSphinx, null, true, this, exit)) {
            return;
        }
        mPOILsv.onRefreshComplete(false);
        mPOILsv.setFooterSpringback(false);

        if (dataQuery.isTurnPage() == false) {
            mDataQuery = dataQuery;
        }
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            return;
        }
        POIResponse poiResponse = (POIResponse)dataQuery.getResponse();
        if (poiResponse.getBPOIList() != null && 
                      poiResponse.getBPOIList().getList() != null && 
                      poiResponse.getBPOIList().getList().size() > 0) {
            List<POI> poiList = poiResponse.getBPOIList().getList();
            if (dataQuery.isTurnPage()) {
                ((POIResponse)lastDataQuery.getResponse()).getBPOIList().getList().addAll(poiList);
            }
            for(POI poi : poiList) {
                if (!mPOIList.contains(poi)) {
                	mPOIList.add(poi);
                }
            }
            mPOIAdapter.notifyDataSetChanged();
            
            if (((POIResponse)lastDataQuery.getResponse()).getBPOIList().getList().size() < ((POIResponse)lastDataQuery.getResponse()).getBPOIList().getTotal()) {
            	mPOILsv.setFooterSpringback(true);
            }
        }
        
        if (mPOIList.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
        }
        
        if (mPOILsv.isFooterSpringback()) {
            mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
        }
    }
}
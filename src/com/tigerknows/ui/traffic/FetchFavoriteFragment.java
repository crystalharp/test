package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.POI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.SqliteWrapper;

public class FetchFavoriteFragment extends BaseFragment {

	static final String TAG = "FetchFavoriteDialog";

    private ListView mFavoriteLsv = null;
    
    private FavoriteAdapter mFavoriteAdapter = null;
    
    private List<POI> mFavoriteList = new ArrayList<POI>();
    
    private List<POI> mTempFavoriteList = new ArrayList<POI>();

    TrafficQueryFragment queryBaseView = null;
    //
    private boolean mFavoriteCanTurnPage = true;
    //
    private View mFavoriteLoadingView = null;
    
    private TextView mEmptyView;
    
    @SuppressWarnings("unchecked")
    private Comparator mComparator = new Comparator() {

        @Override
        public int compare(Object object1, Object object2) {
            BaseData baseData1 = (BaseData) object1;
            BaseData baseData2 = (BaseData) object2;
            return (int)(baseData2.getId() - baseData1.getId());
        };
    };
    
    public FetchFavoriteFragment(Sphinx sphinx) {
        super(sphinx);
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficFetchFavorite;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_fetch_favorite, container, false);
        findViews();
        setListener();
        
        mFavoriteAdapter = new FavoriteAdapter(mContext, mFavoriteList);
        mFavoriteLsv.setAdapter(mFavoriteAdapter);
        
        return mRootView;
	}
	
	protected void findViews() {
        mFavoriteLsv = (ListView)mRootView.findViewById(R.id.favorite_lsv);
        mFavoriteLoadingView = (LinearLayout)mRootView.findViewById(R.id.loading_lnl);
//        mFavoriteLsv.addFooterView(mFavoriteLoadingView);
        mEmptyView = (TextView)mRootView.findViewById(R.id.empty_txv);
    }
	
	@Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }


    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        mTitleBtn.setText(mContext.getString(R.string.favorite));
    }


    protected void setListener() {
		mFavoriteLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                int mPosition = arg2;
                if (getPOI(mPosition) != null) {
                	mActionLog.addAction(mActionTag + ActionLog.ListViewItem, mPosition);
                	queryBaseView.setData(getPOI(mPosition), TrafficQueryFragment.SELECTED);
                	dismiss();
                }
            }
        });
		
		mFavoriteLsv.setOnScrollListener(new OnScrollListener(){

            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {

//                if (totalItemCount > 0 && (firstVisibleItem + visibleItemCount) == totalItemCount && mFavoriteCanTurnPage){
            	if (mFavoriteList.size() > 0 && (firstVisibleItem + visibleItemCount) == mFavoriteList.size() && mFavoriteCanTurnPage){
                	
                	mFavoriteCanTurnPage = false;
                    long maxId = 0;
                    for(POI traffic : mFavoriteList) {
                        if (traffic.getId() > maxId) {
                            maxId = traffic.getId();
                        }
                    }
                	loadData(maxId);
                }
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
            }});
	}
	
	public void setData(TrafficQueryFragment queryBaseView) {
		this.queryBaseView = queryBaseView;
	}
	
	private POI getPOI(int location) {
        POI poi = null;
        if (mFavoriteList.size() > location) {
            poi = mFavoriteList.get(location);
        }
        return poi;
    }

	protected void loadData(long maxId) {
		LoadThread loadThread = new LoadThread();
//        loadThread.maxId = maxId;
        loadThread.start();
        mFavoriteLoadingView.setVisibility(View.VISIBLE);
	}
	
	Handler mHandler = new Handler(){
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
                if (mTempFavoriteList.size() > 0) {
                    mFavoriteList.addAll(mTempFavoriteList);
                    Collections.sort(mFavoriteList, mComparator);
                    mFavoriteAdapter.notifyDataSetChanged();
                    mFavoriteCanTurnPage = true;
                    
                    mEmptyView.setVisibility(View.GONE);
                    
//                    LogWrapper.d("eric", "Handler.handleMessage mTempFavoriteList: " + mTempFavoriteList);
//                    LogWrapper.d("eric", "Handler.handleMessage mFavoriteList: " + mFavoriteList);
                } else {
                	LogWrapper.d("eric", "mFavoriteList.isEmpty(): " + mFavoriteList.isEmpty());
                    if (mFavoriteList.isEmpty()) {
                        mEmptyView.setText(R.string.favorite_empty_poi);
                        mEmptyView.setVisibility(View.VISIBLE);
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                    }
                }
                mFavoriteLoadingView.setVisibility(View.GONE);
            } 
    };
	
    private final int LOAD_MESSAGE = 1;
    
	class LoadThread extends Thread{
        
//        long maxId;
        
        @Override
        public void run(){
//            readFavoritePOI(maxId);
            
            if (mFavoriteList.size() > 0) {
//            	readFavoritePOI(Long.MAX_VALUE, mFavoriteList.get(0).getId());
            	POI last = mFavoriteList.get(mFavoriteList.size()-1);
            	readFavoritePOI(last.getId(), 0);
            } else {
            	readFavoritePOI(Long.MAX_VALUE, 0);
            }
            
            mHandler.sendEmptyMessage(LOAD_MESSAGE);
        }
    }
	
	private void readFavoritePOI(long maxId, long minId){

		StringBuilder s = new StringBuilder();
        s.append("(");
        s.append(Tigerknows.POI.STORE_TYPE);
        s.append("=");
        s.append(Tigerknows.STORE_TYPE_FAVORITE);
        s.append(")");
        String mPOIWhere = s.toString();
        
		mTempFavoriteList.clear();
        Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), Tigerknows.POI.CONTENT_URI, null, mPOIWhere + " AND (" + com.tigerknows.provider.Tigerknows.POI._ID + ">" + minId+")" + " AND (" + com.tigerknows.provider.Tigerknows.POI._ID + "<" + maxId+")", null, "_id DESC");

        int count;
        if (c != null) {
            count = c.getCount();
            if (count > 0) {
                POI poi;
                c.moveToFirst();
                for(int i = 0; i<count; i++) {
                    poi = POI.readFormCursor(mContext, c);
                    
                    LogWrapper.d("eric", "readFavoritePOI poi's ID: " + poi.getId());
                    mTempFavoriteList.add(poi);
                    c.moveToNext();
                }
            }
            c.close();
        }

    }

	@Override
	public void show() {
		// TODO Auto-generated method stub
		super.show();
		mFavoriteList.clear();
		mFavoriteAdapter.notifyDataSetChanged();
		loadData(0);
		LogWrapper.d("eric", "FetchFavoriteDialog.show()");
	}

	private class FavoriteAdapter extends ArrayAdapter<POI>{

        public FavoriteAdapter(Context context, List<POI> poiList) {
            super(context, R.layout.more_history_list_item, poiList);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.more_history_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            POI poi = getItem(position);
            
            TextView textView = (TextView) view.findViewById(R.id.text_txv);
            textView.setText((position+1)+". " + poi.getName());
            textView.setPadding(10, 10, 10, 10);
            return view;
        }

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mFavoriteList.size();
		}
        
    }

}

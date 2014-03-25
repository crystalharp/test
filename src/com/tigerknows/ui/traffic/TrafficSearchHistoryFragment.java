package com.tigerknows.ui.traffic;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.provider.TrafficSearchHistoryTable;
import com.tigerknows.provider.TrafficSearchHistoryTable.SearchHistory;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

public class TrafficSearchHistoryFragment extends BaseFragment {
    
    ListView mHistoryLsv;
    
    TextView mEmptyTxv;
    
    SearchHistoryList mList = new SearchHistoryList(mSphinx);
    
    HistoryAdapter mAdapter = new HistoryAdapter();
    
    int clickedPos;
    
    View.OnClickListener mRightListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            mList.clearHistory();
            mAdapter.notifyDataSetChanged();
            refresh();
            mActionLog.addAction(mActionTag + ActionLog.TrafficSearchHistoryClear);
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficSearchHistory;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_transfer_history, container, false);
        
        findView();
        setListener();
        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.title_traffic_search_history);
        mList.initData();
        mAdapter.notifyDataSetChanged();
        refresh();
        mRightBtn.setVisibility(View.VISIBLE);
        Utility.refreshButton(mSphinx, mRightBtn, null, getString(R.string.clear), false);
        mRightBtn.setOnClickListener(mRightListener);
        if(mDismissed){
        	mDismissed = false;
        	mHistoryLsv.setSelectionFromTop(0, 0);
        }
    }

    public TrafficSearchHistoryFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private void findView() {
        mHistoryLsv = (ListView) mRootView.findViewById(R.id.traffic_history_lsv);
        
        mEmptyTxv = (TextView) mRootView.findViewById(R.id.empty_txv);
        
        mHistoryLsv.setAdapter(mAdapter);
        mHistoryLsv.setOnItemClickListener(new OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, arg2);
                TrafficQueryFragment f = mFragmentManager.getTrafficQueryFragment();
                f.setStart(mList.get(arg2).start);
                f.setEnd(mList.get(arg2).end);
                f.autoStartQuery(true);
                mList.updateHistory(mList.get(arg2));
                mAdapter.notifyDataSetChanged();
                dismiss();
            }
        });
    }
    
    private void refresh() {
        if (mList.size() == 0) {
            mHistoryLsv.setVisibility(View.GONE);
            mEmptyTxv.setVisibility(View.VISIBLE);
        } else {
            mHistoryLsv.setVisibility(View.VISIBLE);
            mEmptyTxv.setVisibility(View.GONE);
        }
    }
    
    public static class SearchHistoryList {
        
        Context mCtx;
        int mShowNum;
        private List<SearchHistory> mList = new LinkedList<SearchHistory>();
        private TrafficSearchHistoryTable table;
        
        public SearchHistoryList(Context ctx) {
            this(ctx, TrafficSearchHistoryTable.MAX_COUNT);
        }
        
        public SearchHistoryList(Context ctx, int showNum) {
            mShowNum = showNum;
            mCtx = ctx;
            table = new TrafficSearchHistoryTable(ctx);
        }
        
        public void initData() {
            mList.clear();
            table.readTrafficSearchHistory(mList);
        }
        
        public void addHistory(SearchHistory sh) {
            if (mList.contains(sh)) {
                //各种各样看起来名字一样的历史需要先删除掉
                int index = mList.indexOf(sh);
                SearchHistory oldsh = mList.get(index);
                mList.remove(oldsh);
                table.delete(oldsh);
            }
            mList.add(0, sh);
            table.add(sh);
        }
        
        public void delHistory(SearchHistory sh) {
            mList.remove(sh);
            table.delete(sh);
        }
        
        public void clearHistory() {
            table.clear();
            mList.clear();
        }
        
        public void updateHistory(SearchHistory sh) {
            if (sh.getId() != -1) {
                mList.remove(sh);
                mList.add(0, sh);
                table.update(sh);
            }
        }
        
        public final int indexOf(SearchHistory sh) {
            return mList.indexOf(sh);
        }
        
        public final SearchHistory get(int index) {
            return mList.get(index);
        }
        
        public final int size() {
            return mList.size();
        }
        
        public final List<SearchHistory> getList() {
            if (mList.size() > mShowNum) {
                return mList.subList(0, mShowNum);
            } else {
                return mList;
            }
        }
    
    }
    
    class HistoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position) ;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.traffic_transfer_his_item, null);
            }
            TextView txv = (TextView) convertView.findViewById(R.id.his_txv);
            txv.setText(((SearchHistory)getItem(position)).genDescription());
            return convertView;
        }
        
    }

}

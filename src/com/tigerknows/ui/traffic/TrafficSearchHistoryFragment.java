package com.tigerknows.ui.traffic;

import java.util.LinkedList;
import java.util.List;

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
import com.tigerknows.provider.TrafficSearchHistoryTable;
import com.tigerknows.provider.TrafficSearchHistoryTable.SearchHistory;
import com.tigerknows.ui.BaseFragment;

public class TrafficSearchHistoryFragment extends BaseFragment {
    
    ListView mHistoryLsv;
    
    List<SearchHistory> mList = new LinkedList<SearchHistory>();
    
    TrafficSearchHistoryTable table = new TrafficSearchHistoryTable(mSphinx);
    
    HistoryAdapter mAdapter = new HistoryAdapter();
    
    int clickedPos;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_transfer_history, container, false);
        
        findView();
        return mRootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText("Search History");
        initData();
        mRightBtn.setBackgroundResource(R.drawable.btn_close);
        mRightBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                clearHistory();
            }
        });
    }

    public TrafficSearchHistoryFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private void findView() {
        mHistoryLsv = (ListView) mRootView.findViewById(R.id.traffic_history_lsv);
        
        mHistoryLsv.setAdapter(mAdapter);
        mHistoryLsv.setOnItemClickListener(new OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                TrafficQueryFragment f = mSphinx.getTrafficQueryFragment();
                f.setStart(mList.get(arg2).start);
                f.setEnd(mList.get(arg2).end);
                f.autoStartQuery(true);
                updateHistory(mList.get(arg2));
                dismiss();
            }
        });
    }
    
    private void initData() {
        mList.clear();
        table.readTrafficSearchHistory(mList);
        mAdapter.notifyDataSetChanged();
    }
    
    private void clearHistory() {
        table.clear();
        mList.clear();
        mAdapter.notifyDataSetChanged();
    }
    
    private void updateHistory(SearchHistory sh) {
        mList.remove(sh);
        mList.add(0, sh);
        mAdapter.notifyDataSetChanged();
        table.update(sh);
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

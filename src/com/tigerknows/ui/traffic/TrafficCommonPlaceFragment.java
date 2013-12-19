package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.poi.InputSearchFragment.Callback;;

public class TrafficCommonPlaceFragment extends BaseFragment{

    ListView mCommonPlaceLsv;
    
    List<CommonPlace> mList = new LinkedList<CommonPlace>();
    
    CommonPlaceAdapter mAdapter = new CommonPlaceAdapter();
    
    int clickedPos;
    
    Callback a = new Callback() {

        @Override
        public void onConfirmed(POI p) {
            if (clickedPos != 0) {
                mList.add(new CommonPlace("common", p, false));
            } else {
                mList.remove(0);
                mList.add(0, new CommonPlace("home", p, false));
            }
            mAdapter.notifyDataSetChanged();
            
        }};
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_common_places, container, false);
        
        findView();
        return mRootView;
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
        updateData();
        mTitleBtn.setText("Common Place");
    }

    public TrafficCommonPlaceFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private void findView() {
        mCommonPlaceLsv = (ListView) mRootView.findViewById(R.id.common_place_lsv);
        
        mCommonPlaceLsv.setAdapter(mAdapter);
        mCommonPlaceLsv.setOnItemClickListener(new OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                // TODO Auto-generated method stub
                clickedPos = arg2;
                if (arg2 == mAdapter.getCount() - 1 || 
                        (arg2 == 0 && mList.get(0).empty)) {
                    mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_TRANSFER);
                    mSphinx.getPOIQueryFragment().setConfirmedCallback(a);
                    mSphinx.showView(mSphinx.getPOIQueryFragment().getId());
                }
            }
        });
    }
    
    static class CommonPlace {
        String alias;
        boolean empty;
        POI poi;
        
        public CommonPlace(String a, POI p) {
            alias = a;
            poi = p;
            empty = false;
        }
        
        public CommonPlace(String a, POI p, boolean b) {
            alias = a;
            poi = p;
            empty = b;
        }
    }

    private void updateData() {
        if (mList.size() == 0) {
            mList.add(new CommonPlace("home", null, true));
        }
    }
    
    OnClickListener l = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_TRANSFER);
            mSphinx.getPOIQueryFragment().setConfirmedCallback(a);
            mSphinx.showView(mSphinx.getPOIQueryFragment().getId());
        }
    };
    
    class CommonPlaceAdapter extends BaseAdapter {

        boolean delMode = false;
        
        @Override
        public int getCount() {
            return mList.size() + 1;
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
                convertView = mLayoutInflater.inflate(R.layout.traffic_common_places_item, null);
            }
            TextView descTxv = (TextView) convertView.findViewById(R.id.description_txv);
            TextView aliasTxv = (TextView) convertView.findViewById(R.id.alias_txv);
            Button delBtn = (Button) convertView.findViewById(R.id.del_btn);
            // the last one is "add place"
            if (position == mList.size()) {
                aliasTxv.setText("add place");
                descTxv.setVisibility(View.GONE);
                delBtn.setVisibility(View.GONE);
            } else {
                descTxv.setVisibility(View.VISIBLE);
                CommonPlace c = (CommonPlace) getItem(position);
                delBtn.setVisibility(delMode ? View.VISIBLE : View.GONE);
                descTxv.setText(c.empty ? "click to set" : c.poi.getName());
                aliasTxv.setText(c.alias);
            }
            return convertView;
        }
        
    }
}

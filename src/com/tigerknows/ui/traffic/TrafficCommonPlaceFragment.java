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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.POI;
import com.tigerknows.provider.CommonPlaceTable;
import com.tigerknows.provider.CommonPlaceTable.CommonPlace;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.poi.InputSearchFragment.Callback;

public class TrafficCommonPlaceFragment extends BaseFragment{

    ListView mCommonPlaceLsv;
    
    CommonPlaceList mList = new CommonPlaceList();
    
    CommonPlaceAdapter mAdapter = new CommonPlaceAdapter();
    
    int clickedPos;
    
    Callback a = new Callback() {

        @Override
        public void onConfirmed(POI p) {
//            if (clickedPos == mList.size() && clickedPos != 0) {
//                //点到了新增
//                mList.add(new CommonPlace("common", p, CommonPlace.TYPE_NORMAL));
//            } else {
                mList.setPOI(clickedPos, p);
//            }
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
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText("Common Place");
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setBackgroundResource(R.drawable.btn_delete);
        mRightBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mAdapter.delMode = !mAdapter.delMode;
                mAdapter.notifyDataSetChanged();
            }
        });
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
                clickedPos = arg2;
                CommonPlace c = mList.get(arg2);
                mSphinx.getInputSearchFragment().setMode(InputSearchFragment.MODE_TRANSFER);
                mSphinx.getInputSearchFragment().setConfirmedCallback(a);
                if (c.poi != null) {
                    mSphinx.getInputSearchFragment().setData(c.poi.getName());
                }
                mSphinx.showView(mSphinx.getInputSearchFragment().getId());
            }
        });
    }
    
    //和数据库有关的操作全部封在这个类中
    class CommonPlaceList {
        List<CommonPlace> mList = new LinkedList<CommonPlace>();
        CommonPlaceTable table = new CommonPlaceTable(mSphinx);
                
        public CommonPlaceList() {
            table.readCommonPlace(mList);
        }
        
        void add(CommonPlace c) {
            mList.add(c);
            table.addCommonPlace(c);
        }
        
        void setPOI(int pos, POI p) {
            if (pos < mList.size()) {
                mList.get(pos).poi = p;
                table.updateDatabase(mList.get(pos));
            }
        }
        
        void remove(int pos) {
            if (pos < mList.size()) {
                mList.get(pos).delete();
                if (mList.get(pos).type == CommonPlace.TYPE_NORMAL) {
                    table.deleteCommonPlace(mList.get(pos));
                    mList.remove(pos);
                } else {
                    table.updateDatabase(mList.get(pos));
                }
            }
        }
                
        boolean contains(CommonPlace c) {
            return mList.contains(c);
        }
        
        int size() {
            return mList.size();
        }
        
        CommonPlace get(int pos) {
            if (pos < mList.size()) {
                return mList.get(pos);
            } else {
                return null;
            }
        }
        
    }
        
    class CommonPlaceAdapter extends BaseAdapter {

        boolean delMode = false;
        
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
                convertView = mLayoutInflater.inflate(R.layout.traffic_common_places_item, null);
            }
            TextView descTxv = (TextView) convertView.findViewById(R.id.description_txv);
            TextView aliasTxv = (TextView) convertView.findViewById(R.id.alias_txv);
            Button delBtn = (Button) convertView.findViewById(R.id.del_btn);
            delBtn.setOnClickListener(l);
            // the last one is "add place"
//            if (position == mList.size()) {
//                aliasTxv.setText("add place");
//                descTxv.setVisibility(View.GONE);
//                delBtn.setVisibility(View.GONE);
//            } else {
            descTxv.setVisibility(View.VISIBLE);
            CommonPlace c = (CommonPlace) getItem(position);
            delBtn.setVisibility(delMode ? View.VISIBLE : View.GONE);
            descTxv.setText(c.isEmptyFixedPlace() ? "click to set" : c.poi.getName());
            aliasTxv.setText(c.alias);
            delBtn.setTag(position);
//            }
            return convertView;
        }
        
    }
    
    OnClickListener l = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int pos = (Integer) v.getTag();
            mList.remove(pos);
            mAdapter.notifyDataSetChanged();
        }
    };
    
//    public final void initData() {
//        //TODO：如果没有表，则建表，并且创建一个第一个是“家”的列表，否则直接读就行了
//        mList.init();
//    }

//    final private void delCommonPlace(int pos) {
//        if (pos == 0) {
//            mList.get(0).empty = true;
//            mList.setPOI(0, null);
//        } else {
//            mList.remove(pos);
//        }
//    }
}

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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.provider.CommonPlaceTable;
import com.tigerknows.provider.CommonPlaceTable.CommonPlace;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.poi.InputSearchFragment.IResponsePOI;

public class TrafficCommonPlaceFragment extends BaseFragment{

    private ListView mCommonPlaceLsv;
    
    private CommonPlaceList mList = new CommonPlaceList(mSphinx);
    
    private CommonPlaceAdapter mAdapter = new CommonPlaceAdapter();
    
    private int mClickedPos;
    
    private IResponsePOI mCallback = new IResponsePOI() {

        @Override
        public void responsePOI(POI poi) {
            mList.setPOI(mClickedPos, poi);
            mAdapter.notifyDataSetChanged();
        }
    };
        
    private OnClickListener mDeleteBtnOnClickListenter = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            int pos = (Integer) v.getTag();
            mActionLog.addAction(mActionTag + ActionLog.TrafficCommonPlaceDel);
            mList.remove(pos);
            mAdapter.notifyDataSetChanged();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficCommonPlace;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_common_places, container, false);
        
        findViews();
        setListener();
        
        mCommonPlaceLsv.setAdapter(mAdapter);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.set_common_place);
        mList.updateData();
        mAdapter.notifyDataSetChanged();
    }

    public TrafficCommonPlaceFragment(Sphinx sphinx) {
        super(sphinx);
    }

    @Override
    protected void findViews() {
        super.findViews();
        mCommonPlaceLsv = (ListView) mRootView.findViewById(R.id.common_place_lsv);
    }
    
    @Override
    protected void setListener() {
        super.setListener();
        mCommonPlaceLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mClickedPos = position;
                CommonPlace c = mList.get(position);
                mActionLog.addAction(mActionTag + ActionLog.TrafficCommonPlaceClick);

                String keyWord = null;
                if (c.poi != null) {
                    keyWord = c.poi.getName();
                }

                DataQuery dataQuery = new DataQuery(mSphinx);
                mSphinx.getInputSearchFragment().setData(dataQuery,
                		keyWord,
                        InputSearchFragment.MODE_TRAFFIC,
                        mCallback,
                        InputSearchFragment.REQUEST_COMMON_PLACE);
                mSphinx.showView(R.id.view_poi_input_search);
            }
        });
    }
    
    //和数据库有关的操作全部封在这个类中
    public static class CommonPlaceList {
        
        Context mCtx;
        private List<CommonPlace> mList = new LinkedList<CommonPlace>();
        private CommonPlaceTable mTable;
                
        public CommonPlaceList(Context ctx) {
            mCtx = ctx;
            mTable = new CommonPlaceTable(ctx);
        }
        
        public void updateData() {
            mList.clear();
            int total = mTable.readCommonPlace(mList);

            if (total == 0) {
                CommonPlace cp = new CommonPlace(mCtx.getString(R.string.common_place_home), null, CommonPlace.TYPE_FIXED);
                add(cp);
            }
        }
        
        void add(CommonPlace c) {
            mList.add(c);
            mTable.addCommonPlace(c);
        }
        
        void setPOI(int pos, POI p) {
            if (pos < mList.size()) {
                mList.get(pos).poi = p;
                mTable.updateDatabase(mList.get(pos));
            }
        }
        
        void remove(int pos) {
            if (pos < mList.size()) {
                mList.get(pos).delete();
                if (mList.get(pos).type == CommonPlace.TYPE_NORMAL) {
                    mTable.deleteCommonPlace(mList.get(pos));
                    mList.remove(pos);
                } else {
                    mTable.updateDatabase(mList.get(pos));
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
        
        List<CommonPlace> getList() {
            return mList;
        }
        
    }
        
    private class CommonPlaceAdapter extends BaseAdapter {

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
            
            TextView aliasTxv = (TextView) convertView.findViewById(R.id.alias_txv);
            TextView nameTxv = (TextView) convertView.findViewById(R.id.name_txv);
            Button deleteBtn = (Button) convertView.findViewById(R.id.delete_btn);
            
            CommonPlace c = (CommonPlace) getItem(position);
            
            aliasTxv.setText(c.alias);
            
            deleteBtn.setTag(position);
            deleteBtn.setOnClickListener(mDeleteBtnOnClickListenter);
            if (c.isEmptyFixedPlace()){
                deleteBtn.setVisibility(View.INVISIBLE);
                nameTxv.setText(R.string.click_set_place);
            } else {
                nameTxv.setText(c.poi.getName());
                deleteBtn.setVisibility(View.VISIBLE);
            }
            
            return convertView;
        }
        
    }
}

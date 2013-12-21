package com.tigerknows.ui.traffic;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.poi.InputSearchFragment.Callback;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.SqliteWrapper;

public class TrafficCommonPlaceFragment extends BaseFragment{

    ListView mCommonPlaceLsv;
    
    CommonPlaceList mList = new CommonPlaceList();
    
    CommonPlaceAdapter mAdapter = new CommonPlaceAdapter();
    
    int clickedPos;
    
    Callback a = new Callback() {

        @Override
        public void onConfirmed(POI p) {
            if (clickedPos == mList.size()) {
                //点到了新增
                mList.add(new CommonPlace("common", p, false));
            } else {
                mList.setPOI(clickedPos, p);
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
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
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
    
    //和数据库有关的操作全部封在这个类中
    class CommonPlaceList {
        List<CommonPlace> mList = null;
        
        int init() {
            mList = new LinkedList<CommonPlace>();
            readCommonPlace(mContext, mList);
            if (mList.size() == 0) {
                add(new CommonPlace("home", null, true));
            }
            return mList.size();
        }
        
        void add(CommonPlace c) {
            mList.add(c);
            c.writeToDatabases(mContext);
        }
        
        void setPOI(int pos, POI p) {
            if (pos < mList.size()) {
                mList.get(pos).poi = p;
                mList.get(pos).updateDatabase(mContext);
            }
        }
        
        void remove(int pos) {
            if (pos < mList.size()) {
                mList.get(pos).deleteFromDatabase(mContext);
                mList.remove(pos);
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
        
        public int readCommonPlace(Context context, List<CommonPlace> list){
            int total = 0;
            Cursor c = SqliteWrapper.query(context, context.getContentResolver(), Tigerknows.CommonPlace.CONTENT_URI, null, "", null, "_id");
            if (c != null) {
                total = c.getCount();
                if (total > 0) {
                    CommonPlace data;
                    c.moveToFirst();
                    for(int i = 0; i<total; i++) {
                        data = CommonPlace.readFromCursor(context, c);
                        if (data != null) {
                            if (list.contains(data)) {
                                list.remove(data);
                            }
                            list.add(data);
                        }
                        c.moveToNext();
                    }
                }
                c.close();
            }
            
            return total;
        }
    }
    
    static class CommonPlace {
        String alias;
        boolean empty;
        POI poi;
        long id;
        
        public CommonPlace(String a, POI p) {
            this(a, p, false);
        }
        
        public CommonPlace(String a, POI p, boolean b) {
            alias = a;
            poi = p;
            empty = b;
        }
        
        public static CommonPlace readFromCursor(Context context, Cursor c) {
            CommonPlace data = null;
            if (c != null) {
                if (c.getCount() > 0) {
                    String a = c.getString(c.getColumnIndex(Tigerknows.CommonPlace.ALIAS));
                    byte[] t = c.getBlob(c.getColumnIndex(Tigerknows.CommonPlace.POI));
                    int b = c.getInt(c.getColumnIndex(Tigerknows.CommonPlace.EMPTY));
                    boolean empty = (b == Tigerknows.CommonPlace.TYPE_EMPTY ? true : false);
                    POI p = new POI();
                    try {
                        XMap xmap = (XMap) ByteUtil.byteToXObject(t);
                        p.init(xmap, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    data = new CommonPlace(a, p, empty);
                    data.id = c.getLong(c.getColumnIndex(Tigerknows.CommonPlace._ID));
                }
            }
            return data;
        }

        public Uri writeToDatabases(Context context) {
            boolean isFailed = false;
            ContentValues values = new ContentValues();
            values.put(Tigerknows.CommonPlace.ALIAS, alias);
            values.put(Tigerknows.CommonPlace.EMPTY, empty ? Tigerknows.CommonPlace.TYPE_EMPTY : Tigerknows.CommonPlace.TYPE_NO_EMPTY);
            if (empty) {
                //空的就把POI信息删掉，目前用于第一条“家”的内容
                values.put(Tigerknows.CommonPlace.POI, "");
            } else {
                try {
                    byte[] data = ByteUtil.xobjectToByte(poi.getData());
                    if (data.length > 0) {
                        values.put(Tigerknows.CommonPlace.POI, data);
                    }
                } catch (Exception e) {
                    isFailed = true;
                }
            }
            Uri uri = null;
            if (!isFailed) {
                uri = SqliteWrapper.insert(context, context.getContentResolver(), Tigerknows.CommonPlace.CONTENT_URI, values);
            }
            if (uri != null) {
                this.id = Integer.parseInt(uri.getPathSegments().get(1));
            }
            
            return uri;
        }
        

        public int deleteFromDatabase(Context context) {
            int count = SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.CommonPlace.CONTENT_URI, "_id="+id, null);
            this.id = -1;
            return count;
        }

        public int updateDatabase(Context context) {
            int count = 0;
            boolean isFailed = false;
            ContentValues values = new ContentValues();
            if (empty) {
                //空的就把POI信息删掉，目前用于第一条“家”的内容
                values.put(Tigerknows.CommonPlace.POI, "");
            } else {
                try {
                    byte[] data = ByteUtil.xobjectToByte(poi.getData());
                    if (data.length > 0) {
                        values.put(Tigerknows.CommonPlace.POI, data);
                    }
                } catch (Exception e) {
                    isFailed = true;
                }
            }
            if (!isFailed) {
                count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.CommonPlace.CONTENT_URI, id), values, null, null);
            }
            return count;
        }
    }
    
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
    
    public final void initData() {
        //TODO：如果没有表，则建表，并且创建一个第一个是“家”的列表，否则直接读就行了
        mList.init();
    }

    //TODO:删除函数
    final private void delCommonPlace(int pos) {
        if (pos == 0) {
            mList.get(0).empty = true;
            mList.setPOI(0, null);
        } else {
            mList.remove(pos);
        }
    }
}

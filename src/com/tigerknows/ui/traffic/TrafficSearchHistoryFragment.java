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
import android.widget.ListView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.POI;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficCommonPlaceFragment.CommonPlace;
import com.tigerknows.util.ByteUtil;
import com.tigerknows.util.SqliteWrapper;

public class TrafficSearchHistoryFragment extends BaseFragment {
    
    ListView mHistoryLsv;
    
    List<SearchHistory> mList = new LinkedList<SearchHistory>();
    
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
        updateData();
        mTitleBtn.setText("Search History");
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
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    static class SearchHistory {
        POI start;
        POI end;
        long id;
        
        public SearchHistory(POI s, POI e) {
            start = s;
            end = e;
        }
        
        public String genDescription() {
            return start.getName() + "->" + end.getName();
        }
        
        public static SearchHistory readFromCursor(Context context, Cursor c) {
            SearchHistory data = null;
            if (c != null) {
                if (c.getCount() > 0) {
                    byte[] bs = c.getBlob(c.getColumnIndex(Tigerknows.TrafficSearchHistory.START));
                    byte[] be = c.getBlob(c.getColumnIndex(Tigerknows.TrafficSearchHistory.END));
                    POI os = new POI();
                    POI oe = new POI();
                    try {
                        XMap xs = (XMap) ByteUtil.byteToXObject(bs);
                        os.init(xs, true);
                        XMap xe = (XMap) ByteUtil.byteToXObject(be);
                        oe.init(xe, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    data = new SearchHistory(os, oe);
                    data.id = c.getLong(c.getColumnIndex(Tigerknows.TrafficSearchHistory._ID));
                }
            }
            return data;
        }

        public Uri writeToDatabases(Context context) {
            boolean isFailed = false;
            ContentValues values = new ContentValues();
            long dateTime = System.currentTimeMillis();
            values.put(Tigerknows.History.DATETIME, dateTime);
            try {
                byte[] s = ByteUtil.xobjectToByte(start.getData());
                if (s.length > 0) {
                    values.put(Tigerknows.TrafficSearchHistory.START, s);
                }
                byte[] e = ByteUtil.xobjectToByte(end.getData());
                if (e.length > 0) {
                    values.put(Tigerknows.TrafficSearchHistory.END, e);
                }
            } catch (Exception e) {
                isFailed = true;
            }
            Uri uri = null;
            if (!isFailed) {
                uri = SqliteWrapper.insert(context, context.getContentResolver(), Tigerknows.TrafficSearchHistory.CONTENT_URI, values);
            }
            if (uri != null) {
                this.id = Integer.parseInt(uri.getPathSegments().get(1));
            }
            
            return uri;
        }
        

        public int deleteFromDatabase(Context context) {
            int count = SqliteWrapper.delete(context, context.getContentResolver(), Tigerknows.TrafficSearchHistory.CONTENT_URI, "_id="+id, null);
            this.id = -1;
            return count;
        }

        public int updateDatabase(Context context) {
            int count = 0;
            ContentValues values = new ContentValues();
            long dateTime = System.currentTimeMillis();
            values.put(Tigerknows.History.DATETIME, dateTime);
            count = SqliteWrapper.update(context, context.getContentResolver(), ContentUris.withAppendedId(Tigerknows.TrafficSearchHistory.CONTENT_URI, id), values, null, null);
            return count;
        }
    }

    private void updateData() {
        //TODO:读取数据库
    }
    
    class HistoryAdapter extends BaseAdapter {

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
                convertView = mLayoutInflater.inflate(R.layout.traffic_transfer_his_item, null);
            }
//            TextView descTxv = (TextView) convertView.findViewById(R.id.description_txv);
//            TextView aliasTxv = (TextView) convertView.findViewById(R.id.alias_txv);
//            Button delBtn = (Button) convertView.findViewById(R.id.del_btn);
//            if (position == mList.size()) {
//                aliasTxv.setText("add place");
//                descTxv.setVisibility(View.GONE);
//                delBtn.setVisibility(View.GONE);
//            } else {
//                descTxv.setVisibility(View.VISIBLE);
//                CommonPlace c = (CommonPlace) getItem(position);
//                delBtn.setVisibility(delMode ? View.VISIBLE : View.GONE);
//                descTxv.setText(c.empty ? "click to set" : c.poi.getName());
//                aliasTxv.setText(c.alias);
//            }
            return convertView;
        }
        
    }

//  public static int readCommonPlace(Context context, List<CommonPlace> list, long maxId, long minId, boolean next){
  public static int readTrafficSearchHistory(Context context, List<SearchHistory> list){
      int total = 0;
      int count;
      Cursor c = SqliteWrapper.query(context, context.getContentResolver(), Tigerknows.CommonPlace.CONTENT_URI, null, "", null, "_datetime DESC");
      if (c != null) {
          count = c.getCount();
          if (count > 0) {
              SearchHistory data;
              c.moveToFirst();
//              maxId = 0;
              for(int i = 0; i<count; i++) {
                  data = SearchHistory.readFromCursor(context, c);
                  if (data != null) {
                      if (list.contains(data)) {
                          list.remove(data);
                      }
                      list.add(data);
//                      maxId = traffic.getDateTime();
                  }
                  c.moveToNext();
              }
              Cursor c1 = SqliteWrapper.query(context, context.getContentResolver(), Tigerknows.CommonPlace.CONTENT_URI_COUNT, null, null, null, null);
              if (c1 != null) {
                  total = c1.getCount();
                  c1.close();
              }
          }
          c.close();
      }
      
      return total;
  }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.maps.BuslineOverlayHelper;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.POI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.view.ResultMapFragment.TitlePopupArrayAdapter;

public class BuslineDetailFragment extends BaseFragment implements View.OnClickListener {

    public BuslineDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private int curLineNum = -1;

    private ListAdapter mResultAdapter;
   
    private TextView mNameTxv = null;
    
    private TextView mLengthTxv = null;
    
    private TextView mTimeTxv = null;
    
    private ListView mResultLsv = null;
    
    private Button mFavorateBtn = null;
    
    private Button mShareBtn = null;
    
    private Line line = null;

	private List<Line> mLineList = new ArrayList<Line>();
	
	private List<String> mTitlePopupList = new ArrayList<String>();
    
    private TitlePopupArrayAdapter mTitlePopupArrayAdapter;
    
    private OnItemClickListener mTitlePopupOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
            mTitleFragment.dismissPopupWindow();
            Line clickedLine = mLineList.get(position);
            mActionLog.addAction(mActionTag + ActionLog.PopupWindowTitle + ActionLog.ListViewItem, position);
            if (clickedLine.equals(line)) {
            	return;
            } else {
            	setData(clickedLine, position);
            	onResume();            	
            }

        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficLineDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.busline_result_detail, container, false);
        findViews();
        setListener();
        
        mTitlePopupArrayAdapter = new TitlePopupArrayAdapter(mSphinx, mTitlePopupList);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mRightBtn.setBackgroundResource(R.drawable.btn_view_map);
        mRightBtn.setOnClickListener(this);

        mResultAdapter = new StringListAdapter(mContext);
        mResultLsv.setAdapter(mResultAdapter);

        mLengthTxv.setText(mSphinx.getString(R.string.length_str_title, line.getLengthStr(mSphinx)));
        mNameTxv.setText(line.getName());
        if (!TextUtils.isEmpty(line.getTime())) {
        	mTimeTxv.setText(mContext.getString(R.string.time_str_title, line.getTime()));
        	mTimeTxv.setVisibility(View.VISIBLE);
        } else {
        	mTimeTxv.setVisibility(View.GONE);
        }
        
        if (mLineList != null) {
        	mTitleBtn.setText(mSphinx.getString(R.string.title_busline_line_popup, TrafficQuery.numToStr(mSphinx, curLineNum + 1)));
        	if (mLineList.size() > 1) {
    	        mTitleBtn.setBackgroundResource(R.drawable.btn_title_popup);
    	        mTitleBtn.setOnClickListener(new View.OnClickListener(){
    				@Override
    				public void onClick(View v) {
    			        mTitleFragment.showPopupWindow(mTitlePopupArrayAdapter, mTitlePopupOnItemClickListener, mActionTag);
    			        mTitlePopupArrayAdapter.notifyDataSetChanged();
    				}
    	        });
        	}
        } else {
        	//不用顶部弹出切换
        	mTitleBtn.setText(mContext.getString(R.string.title_busline_line));
        }
        setFavoriteState(mFavorateBtn, line.checkFavorite(mContext));

        history();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
   
    protected void findViews() {
        mNameTxv = (TextView)mRootView.findViewById(R.id.name_txv);
        mLengthTxv = (TextView)mRootView.findViewById(R.id.length_txv);
        mTimeTxv = (TextView)mRootView.findViewById(R.id.time_txv);
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mFavorateBtn = (Button)mRootView.findViewById(R.id.favorite_btn);
        mShareBtn = (Button)mRootView.findViewById(R.id.share_btn);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        	@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
                // 绘制线路图层
                viewMap();
                // 将地图平移到某一坐标点, 并缩放至某一级别
                BuslineOverlayHelper.panToPosition(mSphinx.getHandler(), position, mSphinx.getMapView());
            }
        });
        mFavorateBtn.setOnClickListener(new ResultOnClickListener());
        mShareBtn.setOnClickListener(new ResultOnClickListener());

    }
    
    public void setData(Line line) {
        setData(line, -1);
    }
    
    public void setData(Line line, int position) {
    	
    	if (line == null)
    		return;

        this.line = line;
        this.mLineList = mSphinx.getBuslineResultLineFragment().getData();
        this.curLineNum = position;
        
        mTitlePopupList.clear();
        if (this.mLineList != null) {
            mTitlePopupArrayAdapter.mSelectedItem = mSphinx.getString(R.string.title_popup_content, curLineNum + 1, this.line.getName());
            for(int i = 0, size = mLineList.size(); i < size; i++) {
                mTitlePopupList.add(mSphinx.getString(R.string.title_popup_content,i + 1, mLineList.get(i).getName()));
            }
        }
    }

    public static class StationViewHolder {
        public ImageView image;
        public TextView textView;
    }
    
    public static class ActionViewHolder {
        public RelativeLayout favorite;
        public RelativeLayout share;
    }

    class StringListAdapter extends BaseAdapter{
    	        
        private List<CharSequence> strList = new ArrayList<CharSequence>();

        public StringListAdapter(Context context) {
        	super();
        	
        	List<Station> stationList = line.getStationList();
            for(Station station : stationList) {
                strList.add(station.getName());
            }

        }

        @Override
        public int getCount() {
            return strList.size();
        }

		@Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if(convertView == null) {
                    convertView = mLayoutInflater.inflate(R.layout.traffic_child_busline, parent, false);
                    StationViewHolder stationHolder = new StationViewHolder();
                    stationHolder.image = (ImageView)convertView.findViewById(R.id.image1);
                    stationHolder.textView = (TextView)convertView.findViewById(R.id.text);
                    convertView.setTag(stationHolder);
            }

            StationViewHolder stationHolder = (StationViewHolder)convertView.getTag();
            
            String txt = mContext.getString(R.string.bus_index_station, position+1, (String)getItem(position));
            int index = txt.indexOf((String)getItem(position))-1; 

            SpannableStringBuilder style=new SpannableStringBuilder(txt);               
            style.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.text_forground_blue)),0, index, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            
            stationHolder.textView.setText(style);

            return convertView;

        }

		@Override
		public Object getItem(int position) {
			return strList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
    }

    private class ResultOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.share_btn){
            	mActionLog.addAction(mActionTag +  ActionLog.CommonShare);
            	share(line);
            }else if(v.getId() == R.id.favorite_btn){
                favorite(line, v);
            }
        }
        
        public void favorite(final BaseData data, final View v) {
            if (data == null)
                return ;
            
            boolean isFavorite = data.checkFavorite(mContext);
            mActionLog.addAction(mActionTag +  ActionLog.CommonFavorite, String.valueOf(isFavorite));
            if (isFavorite) {
            	CommonUtils.showNormalDialog(mSphinx, 
                        mContext.getString(R.string.prompt),
                        mContext.getString(R.string.cancel_favorite_tip),
                        mContext.getString(R.string.yes),
                        mContext.getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                if (id == DialogInterface.BUTTON_POSITIVE) {
                                    setFavoriteState(v, false);
                                    data.deleteFavorite(mContext);
                                }
                            }
                        });
            } else {
            	setFavoriteState(v, true);
                data.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
                Toast.makeText(mSphinx, R.string.favorite_toast, Toast.LENGTH_LONG).show();
            }
        }

    }
    
    public void share(final Line line) {
    	
        if(line == null)
            return;
        
        List<POI> poiList = new ArrayList<POI>();
        for(Station station : line.getStationList()) {
            poiList.add(station.toPOI());
        }

        mSphinx.clearMap();
        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), line);
        Position position = BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx.getMapView(), (Activity)mSphinx);
        
        ShareAPI.share(mSphinx, line, position, mActionTag);
    }
    
    private void setFavoriteState(View v, boolean favoriteYet) {
    	    	
    	if (favoriteYet) {
    		mFavorateBtn.setBackgroundResource(R.drawable.btn_cancel_favorite);
    	} else {
    		mFavorateBtn.setBackgroundResource(R.drawable.btn_favorite);
    	}
    }
    
    private void viewMap() {

        if (line == null) {
            return;
        }

        mSphinx.clearMap();
        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), line);
        mSphinx.setPreviousNextViewVisible();
        
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.title_busline_result_map), ActionLog.TrafficBuslineMap);
        mSphinx.showView(R.id.view_result_map);
    }

    private void history() {
        if (line == null)
        	return;
        
        if (line != null) {
            line.updateHistory(mContext);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.right_btn) {
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
        	// 绘制交通图层
			viewMap();
			// 将地图缩放至可以显示完整的交通路径, 并平移到交通路径中心点
			BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx.getMapView(), (Activity)mSphinx);
        }
    }
    
    public Line getData() {
        return line;
    }
    
    public int getCurLine(){
        return curLineNum;
    }
}

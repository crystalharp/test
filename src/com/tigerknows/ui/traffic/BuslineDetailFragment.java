/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.widget.Toast;

import com.tigerknows.android.location.Position;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.BuslineOverlayHelper;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.POI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.share.ShareAPI;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

public class BuslineDetailFragment extends BaseFragment implements View.OnClickListener {

    public BuslineDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }
    
    private int curLineNum = -1;
    
    private int highlightIndex = -1;
    
    private String highlightStation = null;

    private StringListAdapter mResultAdapter;
    
    private List<CharSequence> mStrList = new ArrayList<CharSequence>();
   
    private TextView mNameTxv = null;
    
    private TextView mLengthTxv = null;
    
    private TextView mTimeTxv = null;
    
    private ListView mResultLsv = null;
    
    private LinearLayout mBottomButtonsView;
    
    private ViewGroup mFavorateBtn = null;
    
    private ViewGroup mShareBtn = null;
    
    private ViewGroup mAlarmBtn = null;
    
    private Line line = null;

	private List<Line> mLineList = new ArrayList<Line>();
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficLineDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_busline_result_detail, container, false);
        findViews();
        setListener();
        
        mResultAdapter = new StringListAdapter(mContext);
        mResultLsv.setAdapter(mResultAdapter);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setText(R.string.map);
        mRightBtn.setOnClickListener(this);

        mLengthTxv.setText(getString(R.string.length_str_title, line.getLengthStr(mSphinx)));
        mNameTxv.setText(line.getName());
        if (!TextUtils.isEmpty(line.getTime())) {
        	mTimeTxv.setText(getString(R.string.time_str_title, line.getTime()));
        	mTimeTxv.setVisibility(View.VISIBLE);
        } else {
        	mTimeTxv.setVisibility(View.GONE);
        }
        
        mTitleBtn.setText(getString(R.string.title_busline_line));
        Utility.setFavoriteBtn(mSphinx, mFavorateBtn, line.checkFavorite(mContext));

        history();

        if (mDismissed) {
            if (highlightIndex != -1) {
                highlightStation(highlightIndex);
            } else {
                setSelectionFromTop(0, 0);
            }
        }
    }
    
    void setSelectionFromTop(final int pos, final int y) {
      //TODO: 这 里为什么要用posDelayed来调用setSelectionFromTop
        mSphinx.getHandler().postDelayed(new Runnable() {
            
            @Override
            public void run() {
                mResultLsv.setSelectionFromTop(pos, y);
            }
        }, 200);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void findViews() {
        super.findViews();
        mNameTxv = (TextView)mRootView.findViewById(R.id.name_txv);
        mLengthTxv = (TextView)mRootView.findViewById(R.id.length_txv);
        mTimeTxv = (TextView)mRootView.findViewById(R.id.time_txv);
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mFavorateBtn = (ViewGroup)mRootView.findViewById(R.id.favorite_btn);
        mShareBtn = (ViewGroup)mRootView.findViewById(R.id.share_btn);
        mBottomButtonsView = ((LinearLayout) (mRootView.findViewById(R.id.bottom_buttons_view)));
        mAlarmBtn = (ViewGroup) mBottomButtonsView.findViewById(R.id.nearby_search_btn);
        TextView textView = (TextView) mAlarmBtn.getChildAt(0);
        textView.setText(R.string.alarm_text);
        Drawable left = getResources().getDrawable(R.drawable.ic_alarm);
        left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        textView.setCompoundDrawables(left, null, null, null);
        mBottomButtonsView.findViewById(R.id.error_recovery_btn).setVisibility(View.GONE);
        mBottomButtonsView.setWeightSum(3);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        	@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
                mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
                // 绘制线路图层
                viewMap();
                // 将地图平移到某一坐标点, 并缩放至某一级别
                BuslineOverlayHelper.panToPosition(mSphinx, position+1, mSphinx.getMapView());
            }
        });
        mFavorateBtn.setOnClickListener(new ResultOnClickListener());
        mShareBtn.setOnClickListener(new ResultOnClickListener());
        mAlarmBtn.setOnClickListener(this);
    }
    
    public void setData(Line line) {
        setData(line, -1);
    }
    
    public void setData(Line line, int position) {
        setData(line, position, null);
    }
    	
    public void setData(Line line, int position, String highlightStation) {
    
    	if (line == null)
    		return;

        this.line = line;
        this.mLineList = mSphinx.getBuslineResultLineFragment().getData();
        this.curLineNum = position;
        
        setHighlightStation(highlightStation);
        
        mStrList.clear();
        List<Station> stationList = line.getStationList();
        for(int i = 0, size = stationList.size(); i < size; i++) {
            mStrList.add(stationList.get(i).getName());
        }
        mResultAdapter.notifyDataSetChanged();
    }
    
    public void setHighlightStation(String s) {
        highlightIndex = -1;
        mResultAdapter.setHighlight(-1);
        this.highlightStation = s;
        if (s != null) {
            highlightIndex = findStation(s, line.getStationList());
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

    class StringListAdapter extends BaseAdapter {

        int highlightIndex;
        public StringListAdapter(Context context) {
        	super();
        	highlightIndex = -1;
        }
        
        final public void setHighlight(int index) {
            highlightIndex = index;
        }

        @Override
        public int getCount() {
            return mStrList.size();
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
            
            String txt = getString(R.string.bus_index_station, position+1, (String)getItem(position));
            int index = txt.indexOf((String)getItem(position))-1; 

            SpannableStringBuilder style=new SpannableStringBuilder(txt);               
            if (position == highlightIndex) {
                style.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.orange)),0, txt.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            } else {
                style.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.text_forground_blue)),0, index, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }
            
            stationHolder.textView.setText(style);
            

            return convertView;

        }

		@Override
		public Object getItem(int position) {
			return mStrList.get(position);
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
            	Utility.showNormalDialog(mSphinx, 
                        getString(R.string.prompt),
                        getString(R.string.cancel_favorite_tip),
                        getString(R.string.yes),
                        getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                if (id == DialogInterface.BUTTON_POSITIVE) {
                                    Utility.setFavoriteBtn(mSphinx, mFavorateBtn, false);
                                    data.deleteFavorite(mContext);
                                }
                            }
                        });
            } else {
                Utility.setFavoriteBtn(mSphinx, mFavorateBtn, true);
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

        mSphinx.resetLoactionButtonState();
        mSphinx.resetMapDegree();
        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getMapView(), line);
        Position position = BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx);
        
        ShareAPI.share(mSphinx, line, position, mActionTag);
    }
    
    private void viewMap() {

        if (line == null) {
            return;
        }
        mSphinx.getResultMapFragment().setData(getString(R.string.title_busline_result_map), ActionLog.TrafficBuslineMap);
        mSphinx.showView(R.id.view_result_map);

        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getMapView(), line);
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
			BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx);
			ItemizedOverlay itemizedOverlay = mSphinx.getMapView().getOverlaysByName(ItemizedOverlay.BUSLINE_OVERLAY);
			itemizedOverlay.focuseOverlayItem(0);
            OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
            mSphinx.showInfoWindow(overlayItem);
        } else if (id == R.id.nearby_search_btn) {
            addActionLog(ActionLog.AlarmBtn);
            BuslineResultLineFragment.showAlarmDialog(mSphinx, BuslineResultLineFragment.lineToPOIList(line));
        }
    }
    
    public Line getData() {
        return line;
    }
    
    public int getCurLine(){
        return curLineNum;
    }
    
    private int findStation(String key, List<Station> stations) {
        for (Station station : stations) {
            if (key.equals(station.getName())) {
                return station.getIndex();
            }
        } 
        
        String poiSuffix1 = "(公交站)";
        String poiSuffix2 = "（公交站）";
        
        if (key.contains(poiSuffix1)) {
            key = key.replace(poiSuffix1, "");
        } else if (key.contains(poiSuffix2)) {
            key = key.replace(poiSuffix2, "");
        } else {
            key = key.replace("公交站", "");
            key = key.replace("公车站", "");
        }
        
        for (Station station : stations) {
            if (key.equals(station.getName())) {
                return station.getIndex();
            }
        }
        
        return -1;
    }
    
    private void highlightStation(int highlightIndex) {
        if (highlightIndex < 0) {
            return;
        }
        mResultAdapter.setHighlight(highlightIndex);
        mResultAdapter.notifyDataSetChanged();
        if (highlightIndex >= 1) {
            highlightIndex--;
        }
        setSelectionFromTop(highlightIndex, 0);
    }
}

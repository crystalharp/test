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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.maps.BuslineOverlayHelper;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.BuslineModel.Station;
import com.tigerknows.model.POI;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.WidgetUtils;

public class BuslineDetailFragment extends BaseFragment implements View.OnClickListener {

    public BuslineDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private ListAdapter mResultAdapter;
   
    private TextView mNameTxv = null;
    
    private TextView mLengthTxv = null;
    
    private TextView mTimeTxv = null;
    
    private ListView mResultLsv = null;
    
    private Line line = null;

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
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mRightImv.setImageResource(R.drawable.ic_view_map);
        mRightBtn.getLayoutParams().width = Util.dip2px(Globals.g_metrics.density, 72);
        mRightBtn.setOnClickListener(this);

        mResultAdapter = new StringListAdapter(mContext);
        mResultLsv.setAdapter(mResultAdapter);

        if (line.getLength() > 1000) {
    		mLengthTxv.setText(mContext.getString(R.string.busline_detail_subtitle1_km, CommonUtils.meter2kilometre(line.getLength())));
    	} else {
    		mLengthTxv.setText(mContext.getString(R.string.busline_detail_subtitle1_m, line.getLength()));
    	}
        mNameTxv.setText(line.getName());
        mTitleBtn.setText(mContext.getString(R.string.title_busline_line));
        if (!TextUtils.isEmpty(line.getTime())) {
        	mTimeTxv.setText(mContext.getString(R.string.busline_detail_subtitle2, line.getTime()));
        	mTimeTxv.setVisibility(View.VISIBLE);
        } else {
        	mTimeTxv.setVisibility(View.GONE);
        }

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
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        	@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
                mActionLog.addAction(ActionLog.TrafficLineDetailStation, position);
                // 绘制线路图层
                viewMap();
                // 将地图平移到某一坐标点, 并缩放至某一级别
                BuslineOverlayHelper.panToPosition(mSphinx.getHandler(), line.getStationList().get(position).getPosition(), mSphinx.getMapView());
            }
        });

    }
    
    public void setData(Line line) {
    	
    	if (line == null)
    		return;

        this.line = line;
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

    	private static final int TYPE_STATION = 0;
        
        private static final int TYPE_ACTION = TYPE_STATION + 1;
        
        private static final int TYPE_COUNT = TYPE_ACTION + 1;
    	        
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
            // TODO Auto-generated method stub
            return strList.size() + 1;
        }

        @Override
		public int getItemViewType(int position) {
			// TODO Auto-generated method stub
        	if(position == getCount() - 1) {
        		return TYPE_ACTION;
        	}
			return TYPE_STATION;
		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return TYPE_COUNT;
		}

		@Override
        public View getView(final int position, View convertView, ViewGroup parent) {

			int type = getItemViewType(position);
            if(convertView == null) {
                switch(type){
                case TYPE_STATION:
                    convertView = mLayoutInflater.inflate(R.layout.traffic_child_busline, parent, false);
                    StationViewHolder stationHolder = new StationViewHolder();
                    stationHolder.image = (ImageView)convertView.findViewById(R.id.image1);
                    stationHolder.textView = (TextView)convertView.findViewById(R.id.text);
                    convertView.setTag(stationHolder);
                    break;
                    
                case TYPE_ACTION:
                    convertView = mLayoutInflater.inflate(R.layout.traffic_fav_share, parent, false);
                    ActionViewHolder actionHolder = new ActionViewHolder();
                    actionHolder.favorite = (RelativeLayout)convertView.findViewById(R.id.favorite_rll);
                    actionHolder.share = (RelativeLayout)convertView.findViewById(R.id.share_rll);
                    convertView.setTag(actionHolder);
                    break;
                default:
                
                }
            }
            
            switch(type){
            case TYPE_STATION:
                StationViewHolder stationHolder = (StationViewHolder)convertView.getTag();
                
                String txt = mContext.getString(R.string.bus_index_station, position+1, (String)getItem(position));
                int index = txt.indexOf((String)getItem(position))-1; 

                SpannableStringBuilder style=new SpannableStringBuilder(txt);               
                style.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.text_forground_blue)),0, index, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                
                stationHolder.textView.setText(style);

                break;

            case TYPE_ACTION:
                ActionViewHolder actionHolder = (ActionViewHolder)convertView.getTag();
                            
                actionHolder.favorite.setOnClickListener(new ResultOnClickListener());
                actionHolder.share.setOnClickListener(new ResultOnClickListener());
                
                setFavoriteState(actionHolder.favorite, line.checkFavorite(mContext));

                break;
            default:
            }

            return convertView;

        }

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return strList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public boolean isEnabled(int position) {
			// TODO Auto-generated method stub
			if(position == this.getCount()-1)
              return false;
          return true;
		}
    }

    private class ResultOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

            if(v.getId() == R.id.share_rll){
            	mActionLog.addAction(ActionLog.TrafficLineDetailShareBtn);
            	share(line);
            }else if(v.getId() == R.id.favorite_rll){
                favorite(line, v);
            }
        }
        
        public void favorite(final BaseData data, final View v) {
            if (data == null)
                return ;
            
            boolean isFavorite = data.checkFavorite(mContext);
            if (isFavorite) {
            	CommonUtils.showNormalDialog(mSphinx, 
                        mContext.getString(R.string.prompt),
                        mContext.getString(R.string.cancel_favorite_tip),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                if (id == DialogInterface.BUTTON_POSITIVE) {
                                	mActionLog.addAction(ActionLog.TrafficLineDetailCancelFav);
                                    setFavoriteState(v, false);
                                    data.deleteFavorite(mContext);
                                }
                            }
                        });
            } else {
            	mActionLog.addAction(ActionLog.TrafficLineDetailFavorite);
            	setFavoriteState(v, true);
                data.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
                Toast.makeText(mSphinx, R.string.favorite_toast, Toast.LENGTH_LONG).show();
            }
        }

    }
    
    public void share(final Line line) {
    	
        if(line == null)
            return;
    	
        String smsContent = ShareTextUtil.shareBuslineSmsContent(line, mContext);
        String weiboContent = ShareTextUtil.shareBuslineWeiboContent(line, mContext);
        String qZoneContent = ShareTextUtil.shareBuslineQzoneContent(line, mContext);
        
        List<POI> poiList = new ArrayList<POI>();
        for(Station station : line.getStationList()) {
            poiList.add(station.toPOI());
        }

        mSphinx.clearMap();
        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), line);
        Position position = BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx.getMapView(), (Activity)mSphinx);
        
        WidgetUtils.share(mSphinx, smsContent, weiboContent, qZoneContent, position);
    }
    
    private void setFavoriteState(View v, boolean favoriteYet) {
    	
    	ImageView favorite = (ImageView)v.findViewById(R.id.image);
    	TextView text = (TextView)v.findViewById(R.id.favorite);
    	
    	if (favoriteYet) {
    	    favorite.setBackgroundResource(R.drawable.ic_favorite);
        	text.setText(mContext.getResources().getString(R.string.favorite_yet));
    	} else {
    	    favorite.setBackgroundResource(R.drawable.ic_favorite_cancel);
        	text.setText(mContext.getResources().getString(R.string.favorite));
    	}
    }
    
    private void viewMap() {

        if (line == null) {
            return;
        }

        mSphinx.clearMap();
        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), line);
        mSphinx.setPreviousNextViewVisible();
        
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.title_busline_result_map), ActionLog.MapBusline);
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
        	mActionLog.addAction(ActionLog.TrafficLineDetailMapBtn);
        	// 绘制交通图层
			viewMap();
			// 将地图缩放至可以显示完整的交通路径, 并平移到交通路径中心点
			BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx.getMapView(), (Activity)mSphinx);
        }
    }
}

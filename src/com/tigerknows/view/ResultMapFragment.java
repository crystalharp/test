/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.BuslineOverlayHelper;
import com.tigerknows.maps.TrafficOverlayHelper;
import com.tigerknows.maps.MapView.SnapMap;
import com.tigerknows.model.POI;
import com.tigerknows.model.BuslineModel.Line;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficQuery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class ResultMapFragment extends BaseFragment implements View.OnClickListener {

    private String mTitle;
    private View mSnapView;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    
    private TitlePopupArrayAdapter mTitlePopupArrayAdapter;
    
    private List<String> mTitlePopupList = new ArrayList<String>();
    
    private OnItemClickListener mTitlePopupOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
            mTitleFragment.dismissPopupWindow();
            mTitlePopupArrayAdapter.mSelectedItem = mTitlePopupArrayAdapter.getItem(position);
            mActionLog.addAction(mActionTag + ActionLog.PopupWindowTitle + ActionLog.ListViewItem, position);
            if (mActionTag.equals(ActionLog.TrafficBuslineMap)) {
                List<Line> list = mSphinx.getBuslineResultLineFragment().getData();
                for(int i = 0, size = list.size(); i < size; i++) {
                    Line line = list.get(i);
                    if (mTitlePopupArrayAdapter.mSelectedItem.equals(mSphinx.getString(R.string.title_popup_content, i + 1, line.getName()))) {
                        mSphinx.clearMap();
                        BuslineOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), line);
                        mSphinx.setPreviousNextViewVisible();
                        mSphinx.resetLoactionButtonState();
                        mSphinx.getBuslineDetailFragment().setData(line, position);
                        mTitleBtn.setText(mSphinx.getString(R.string.title_busline_line_popup, 
                                TrafficQuery.numToStr(mSphinx, mSphinx.getBuslineDetailFragment().getCurLine() + 1)));
                        BuslineOverlayHelper.panToViewWholeOverlay(line, mSphinx.getMapView(), (Activity)mSphinx);
                        break;
                    }
                }
            } else if (mActionTag.equals(ActionLog.TrafficTransferMap)) {
                List<Plan> list = mSphinx.getTrafficResultFragment().getData();
                for(int i = 0, size = list.size(); i < size; i++) {
                    Plan plan = list.get(i);
                    if (mTitlePopupArrayAdapter.mSelectedItem.equals(mSphinx.getString(R.string.title_popup_content, i + 1, plan.getTitle(mSphinx)))) {
                        mSphinx.clearMap();
                        TrafficOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), plan, plan.getType());
                        mSphinx.setPreviousNextViewVisible();
                        mSphinx.resetLoactionButtonState();
                        mSphinx.getTrafficDetailFragment().setData(plan, position);
                        mTitleBtn.setText(mSphinx.getString(R.string.title_transfer_plan_popup, 
                                TrafficQuery.numToStr(mSphinx, mSphinx.getTrafficDetailFragment().getCurLine() + 1)));
                        TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
                        break;
                    }
                }
            }
        }
    };
    
    public ResultMapFragment(Sphinx sphinx) {
        super(sphinx);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIListMap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.result_map, container, false);
        
        findViews();
        setListener();
        
        mTitlePopupArrayAdapter = new TitlePopupArrayAdapter(mSphinx, mTitlePopupList);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSphinx.getMapView().setStopRefreshMyLocation(false);
        mTitleBtn.setText(mTitle);
        mRightBtn.setVisibility(View.INVISIBLE);
        mRootView.setOnTouchListener(null);
        int fromThirdPartye = mSphinx.getFromThirdParty();
        if (fromThirdPartye == Sphinx.THIRD_PARTY_SONY_MY_LOCATION ||
                fromThirdPartye == Sphinx.THIRD_PARTY_SONY_MY_LOCATION) {
            mSnapView.setVisibility(View.VISIBLE);
        } else {
            mSnapView.setVisibility(View.GONE);
        }
        mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 18), 0, 0);
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + Util.dip2px(Globals.g_metrics.density, 18);
        
        //如果顶端切换list不为空，设置切换列表
        if (mTitlePopupList.size() > 0) {
            if (mActionTag.equals(ActionLog.TrafficBuslineMap)) {
                mTitleBtn.setText(mSphinx.getString(R.string.title_busline_line_popup, 
                        TrafficQuery.numToStr(mSphinx, mSphinx.getBuslineDetailFragment().getCurLine() + 1)));
            } else if (mActionTag.equals(ActionLog.TrafficTransferMap)) {
                mTitleBtn.setText(mSphinx.getString(R.string.title_transfer_plan_popup, 
                        TrafficQuery.numToStr(mSphinx, mSphinx.getTrafficDetailFragment().getCurLine() + 1)));
			}
            if (mTitlePopupList.size() > 1) {
                mTitleBtn.setBackgroundResource(R.drawable.btn_title_popup);
                mTitleBtn.setOnClickListener(this);
            }
        }
        //如果是驾车和步行，需要在这里可以切换到详情页
        if (mActionTag == ActionLog.TrafficDriveMap || mActionTag == ActionLog.TrafficWalkMap) {
        	mRightBtn.setVisibility(View.VISIBLE);
        	mRightBtn.setBackgroundResource(R.drawable.btn_view_detail);
        	mRightBtn.setOnClickListener(this);
        	if (mActionTag == ActionLog.TrafficDriveMap) {
        		mTitleBtn.setText(mSphinx.getString(R.string.title_type_drive));
        	} else {
        		mTitleBtn.setText(mSphinx.getString(R.string.title_type_walk));
        	}
        }
        mSphinx.showHint(TKConfig.PREFS_HINT_LOCATION, R.layout.hint_location_map);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 78), 0, 0);
        mSphinx.getMapView().getPadding().top = mSphinx.getTitleViewHeight() + mSphinx.getCityViewHeight() + Util.dip2px(Globals.g_metrics.density, 18);
    }
    
    public void setData(String title, String actionTag) {
        mTitle = title;
        mActionTag = actionTag;
        
        /*
         * setData方法被调用时, 表明将要进入结果地图, 
         * 此时请重置定位点状态
         */
        mSphinx.resetLoactionButtonState();
        
        mTitlePopupArrayAdapter.mSelectedItem = null;
        mTitlePopupList.clear();
        
        if (mSphinx.uiStackContains(R.id.view_favorite) == false
                && mSphinx.uiStackContains(R.id.view_history) == false) {
            if (mActionTag.equals(ActionLog.TrafficBuslineMap)) {
                mTitlePopupArrayAdapter.mSelectedItem = mSphinx.getString(R.string.title_popup_content,
                        mSphinx.getBuslineDetailFragment().getCurLine() + 1, mSphinx.getBuslineDetailFragment().getData().getName());
                List<Line> list = mSphinx.getBuslineResultLineFragment().getData();
                for(int i = 0, size = list.size(); i < size; i++) {
                    mTitlePopupList.add(mSphinx.getString(R.string.title_popup_content, i + 1, list.get(i).getName()));
                }
            } else if (mActionTag.equals(ActionLog.TrafficTransferMap)) {
                mTitlePopupArrayAdapter.mSelectedItem = mSphinx.getString(R.string.title_popup_content,
                        mSphinx.getTrafficDetailFragment().getCurLine() + 1, mSphinx.getTrafficDetailFragment().getData().getTitle(mSphinx));
                List<Plan> list = mSphinx.getTrafficResultFragment().getData();
                for(int i = 0, size = list.size(); i < size; i++) {
                    mTitlePopupList.add(mSphinx.getString(R.string.title_popup_content, i + 1, list.get(i).getTitle(mSphinx)));
                }
            }
        }
    }
    
    protected void findViews() {
        mSnapView = mRootView.findViewById(R.id.snap_view);
        mCancelBtn = (Button) mRootView.findViewById(R.id.cancel_btn);
        mConfirmBtn = (Button) mRootView.findViewById(R.id.confirm_btn);
    }
    
    protected void setListener() {
        mCancelBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
        case R.id.title_btn:
        	if (mTitlePopupList.size() > 0) {
        		mTitleFragment.showPopupWindow(mTitlePopupArrayAdapter, mTitlePopupOnItemClickListener, mActionTag);
        		mTitlePopupArrayAdapter.notifyDataSetChanged();
        	}
        	break;
        case R.id.confirm_btn:
	        mSphinx.snapMapView(new SnapMap() {
	        	
	        	@Override
	        	public void finish(Uri uri) {
	        		if(uri != null) {
	        			Intent intent = new Intent();
	        			intent.setData(uri);  // uri 是 地图(包括标注层) 的一个snapshot图片
	        			ItemizedOverlay itemizedOverlay = mSphinx.getMapView().getOverlaysByName(ItemizedOverlay.POI_OVERLAY);
	        			POI poi = null;
	        			if (itemizedOverlay != null) {
	        				OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
	        				if (overlayItem != null && overlayItem.getAssociatedObject() instanceof POI) {
	        					poi = (POI) (overlayItem.getAssociatedObject());
	        				}
	        			}
	        			
	        			Position position = null;
	        			String description = null;
	        			if (poi != null) {
	        				position = poi.getPosition();
	        				description = poi.getName().replace('(', ' ').replace(')', ' ');
	        				String address = poi.getAddress();
	        				if (!TextUtils.isEmpty(address)) {
	        					description += "("+address.replace('(', ' ').replace(')', ' ')+")";
	        				}
	        			} else {
	        				position = mSphinx.getMapView().getCenterPosition();
	        				description = mSphinx.getMapEngine().getPositionName(position);
	        				if (TextUtils.isEmpty(description)) {
	        					description = mContext.getString(R.string.select_point);
	        				}
	        				description = description.replace('(', ' ').replace(')', ' ');
	        			}
	        			intent.putExtra("location", "<a href='http://maps.tigerknows.com/?latlon="+
	        					position.getLat()+","+
	        					position.getLon()+"&z="+
	        					mSphinx.getMapView().getZoomLevel()+"&n="+
	        					description.replace('\'', ' ')+"'>" +
	        					description + "</a>"); // address 是选定位置(当前定位的位置或者search后选择的某个地址)的详细地址描述 加上 该地址的 URL link，最终要插入MMS的内容中
	        			mSphinx.setResult(Activity.RESULT_OK, intent);
	        			mSphinx.finish();
	        		}
	        	}
	        }, mSphinx.getMapView().getCenterPosition(), null);
	        break;
        case R.id.right_btn:
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
        	mSphinx.showView(R.id.view_traffic_result_detail);
        	break;
    	default:
    		break;
        }

    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mSphinx.clearMap();
    }
    
    public static class TitlePopupArrayAdapter extends ArrayAdapter<String> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.result_map_title_popup_list_item;
        
        private LayoutInflater mLayoutInflater;
        
        public String mSelectedItem;

        public TitlePopupArrayAdapter(Context context, List<String> list) {
            super(context, TEXTVIEW_RESOURCE_ID, list);
            mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            ImageView iconTxv = (ImageView)view.findViewById(R.id.icon_imv);
            TextView textTxv = (TextView)view.findViewById(R.id.name_txv);
            
            String name = getItem(position);
            if (name.equals(mSelectedItem)) {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                iconTxv.setVisibility(View.VISIBLE);
                textTxv.setTextColor(TKConfig.COLOR_ORANGE);
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                iconTxv.setVisibility(View.INVISIBLE);
                textTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
            }            
            textTxv.setText(name);

            return view;
        }
    }
}

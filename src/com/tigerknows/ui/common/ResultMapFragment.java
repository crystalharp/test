/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.common;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseFragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

/**
 * @author Peng Wenyue
 */
public class ResultMapFragment extends BaseFragment implements View.OnClickListener {

    private String mTitle;
    private View mSnapView;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    
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
        
        mBottomFragment = mSphinx.getInfoWindowFragment();
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSphinx.getHomeFragment().reset();
        mSphinx.getMapView().setStopRefreshMyLocation(false);
        mTitleBtn.setText(mTitle);
        setOnTouchListener(null);
        
        int fromThirdPartye = mSphinx.getFromThirdParty();
        if (fromThirdPartye == Sphinx.THIRD_PARTY_SONY_MY_LOCATION ||
                fromThirdPartye == Sphinx.THIRD_PARTY_SONY_QUERY_POI) {
            mSnapView.setVisibility(View.VISIBLE);
        } else {
            mSnapView.setVisibility(View.GONE);
        }

        if (ActionLog.ResultMapTuangouList.equals(mActionTag) ||
                ActionLog.ResultMapYanchuList.equals(mActionTag) ||
                ActionLog.ResultMapZhanlanList.equals(mActionTag) ||
                ActionLog.POIListMap.equals(mActionTag) ||
                ActionLog.POIHotelListMap.equals(mActionTag)){
            mSphinx.showHint(TKConfig.PREFS_HINT_RESULT_MAP, R.layout.hint_result_map);
        }

        if (ActionLog.TrafficDriveMap.equals(mActionTag)) {
            mTitleBtn.setText(getString(R.string.title_drive_result_map));
        } else if (ActionLog.TrafficWalkMap.equals(mActionTag)) {
            mTitleBtn.setText(getString(R.string.title_walk_result_map));
        } else if (ActionLog.TrafficTransferMap.equals(mActionTag) ||
                ActionLog.TrafficTransferListMap.equals(mActionTag)) {
            mTitleBtn.setText(getString(R.string.title_transfer_result_map));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    
    public void setData(String title, String actionTag) {
        saveResultData(actionTag);
        mTitle = title;
        mActionTag = actionTag;
    }
    
    @Override
    protected void findViews() {
        super.findViews();
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
                                description = getString(R.string.select_point);
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
            }, mSphinx.getMapView().getCenterPosition(), mSphinx.getMapView().getCurrentMapScene());
            break;
            
        default:
            break;
        }

    }
    
    @Override
    public void dismiss() {
        super.dismiss();

        View view = mSphinx.getCenterTokenView();
        mSphinx.setTouchMode(TouchMode.NORMAL);
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.INVISIBLE);
            mSphinx.replaceBottomUI(null);
        }
        if (mResultData != null) {
            boolean existPOI = false;
            List<ItemizedOverlay> list = mResultData.mapScene.itemizedOverlayList;
            for(int i = list.size() - 1; i >= 0; i--) {
                if (ItemizedOverlay.POI_OVERLAY.contains(list.get(i).getName())) {
                    existPOI = true;
                    break;
                }
            }
            
            if (existPOI) {
                restoreDataBean();
            }
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
    }
    
    // 在进入到交通选点界面和交通结果地图界面时，保存之前的地图相关信息
    private ResultData mResultData;
    
    private void saveResultData(String actionTag) {

        if (ActionLog.TrafficSelectPoint.equals(actionTag) ||
                ActionLog.TrafficTransferMap.equals(actionTag) ||
                ActionLog.TrafficDriveMap.equals(actionTag) ||
                ActionLog.TrafficWalkMap.equals(actionTag) ||
                ActionLog.TrafficTransferListMap.equals(actionTag)) {
            
            if (ActionLog.TrafficSelectPoint.equals(mActionTag) == false &&
                    ActionLog.TrafficTransferMap.equals(mActionTag) == false &&
                    ActionLog.TrafficDriveMap.equals(mActionTag) == false &&
                    ActionLog.TrafficWalkMap.equals(mActionTag) == false &&
                    ActionLog.TrafficTransferListMap.equals(mActionTag) == false) {
                
                mResultData = new ResultData();
                mResultData.title = mTitle;
                mResultData.actionTag = mActionTag;
                mResultData.mapScene = mSphinx.getMapView().getCurrentMapScene();
            }
        }
    }
    
    private void restoreDataBean() {
        if (ActionLog.TrafficSelectPoint.equals(mActionTag) ||
                ActionLog.TrafficTransferMap.equals(mActionTag) ||
                ActionLog.TrafficDriveMap.equals(mActionTag) ||
                ActionLog.TrafficWalkMap.equals(mActionTag) ||
                ActionLog.TrafficTransferListMap.equals(mActionTag)) {
            
            if (mResultData != null) {
                if (ActionLog.TrafficSelectPoint.equals(mResultData.actionTag) == false &&
                        ActionLog.TrafficTransferMap.equals(mResultData.actionTag) == false &&
                        ActionLog.TrafficDriveMap.equals(mResultData.actionTag) == false &&
                        ActionLog.TrafficWalkMap.equals(mResultData.actionTag) == false &&
                        ActionLog.TrafficTransferListMap.equals(mResultData.actionTag) == false) {
                    
                    if (mActionTag.equals(mResultData.actionTag) == false) {
                        mSphinx.clearMap();
                        setData(mResultData.title, mResultData.actionTag);
                        mSphinx.getMapView().restoreScene(mResultData.mapScene);
                        mResultData = null;
                    }
                }
            }
        }
    }
    
    public class ResultData {
        public String title;
        public String actionTag;
        public MapScene mapScene;
    }
}

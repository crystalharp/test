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
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficDetailFragment;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

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

    private View mTrafficTitieView;
    private RadioGroup mTrafficTitleRadioGroup;
    private RadioButton mTrafficTransferRbt;
    private RadioButton mTrafficDriveRbt;
    private RadioButton mTrafficWalkRbt;
    
    View.OnTouchListener onTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();
            if (mTrafficTitleRadioGroup.getCheckedRadioButtonId() == id) {
                return true;
            }
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            boolean result = false;
            if (action == MotionEvent.ACTION_UP) {
                if (R.id.traffic_transfer_rbt == id) {
                    mActionLog.addAction(mActionTag, ActionLog.TrafficTransferTab);
                    result = !changeTrafficType(Plan.Step.TYPE_TRANSFER);
                } else if (R.id.traffic_drive_rbt == id) {
                    mActionLog.addAction(mActionTag, ActionLog.TrafficDriveTab);
                    result = !changeTrafficType(Plan.Step.TYPE_DRIVE);
                } else if (R.id.traffic_walk_rbt == id) {
                    mActionLog.addAction(mActionTag, ActionLog.TrafficWalkTab);
                    result = !changeTrafficType(Plan.Step.TYPE_WALK);
                }
            }
            return result;
        }
    };
    
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
        
        restoreResultData();
        mSphinx.resetLoactionButtonState();
        mSphinx.resetMapDegree();
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
        
        int type = 0;
        if (ActionLog.TrafficTransferMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_TRANSFER;
        } else if (ActionLog.TrafficDriveListMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_DRIVE;
        } else if (ActionLog.TrafficWalkListMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_WALK;
        }
        
        if (ActionLog.TrafficDriveListMap.equals(mActionTag)) {
            mSphinx.showHint(TKConfig.PREFS_HINT_TRAFFIC_PREFERENCE, R.layout.hint_traffic_preference);
        } else if (ActionLog.ResultMapTuangouList.equals(mActionTag) ||
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
        } else if (ActionLog.TrafficTransferMap.equals(mActionTag)) {
            mTitleBtn.setText(getString(R.string.title_transfer_result_map));
        }
        if (ActionLog.TrafficDriveListMap.equals(mActionTag) ||
                ActionLog.TrafficWalkListMap.equals(mActionTag)) {
            mTitleBtn.setVisibility(View.GONE);
            mTrafficTitieView = mSphinx.getTrafficQueryFragment().getTitleView();
            mTrafficTitleRadioGroup = (RadioGroup) mTrafficTitieView.findViewById(R.id.traffic_rgp);
            ((ViewGroup)mSphinx.getTitleFragment().mRootView).addView(mTrafficTitieView, TrafficQueryFragment.sLayoutParams);
    
            mTrafficTransferRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_transfer_rbt);
            mTrafficDriveRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_drive_rbt);
            mTrafficWalkRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_walk_rbt);
            mTrafficTransferRbt.setOnTouchListener(onTouchListener);
            mTrafficDriveRbt.setOnTouchListener(onTouchListener);
            mTrafficWalkRbt.setOnTouchListener(onTouchListener);
            checkRadioBtn(mActionTag);
    
            mRightBtn.setVisibility(View.INVISIBLE);
            changeTrafficType(type, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveResultData();
    }
    
    public void setData(String title, String actionTag) {
        mTitle = title;
        mActionTag = actionTag;
        if (ActionLog.TrafficDriveListMap.equals(mActionTag) ||
                ActionLog.TrafficWalkListMap.equals(mActionTag)) {
            mResultDataForPlanList = null;
        } else if (ActionLog.TrafficTransferMap.equals(mActionTag) ||
                ActionLog.TrafficDriveMap.equals(mActionTag) ||
                ActionLog.TrafficWalkMap.equals(mActionTag)) {
            mResultDataForPlanDetail = null;
        }
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
            
        case R.id.right_btn:
            mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
            String[] list = mSphinx.getResources().getStringArray(R.array.drvie_search_option);
            final int[] driveTypeList = {1,2,3};
            final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list);
            
            View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
            
            ListView listView = (ListView) alterListView.findViewById(R.id.listview);
            listView.setAdapter(adapter);
            
            final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
            
            TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
            titleTxv.setText(R.string.preference);
            
            Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
            button.setVisibility(View.GONE);
            
            dialog.show();
            
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View arg1, int index, long arg3) {

                    mActionLog.addAction(mActionTag + ActionLog.TrafficDriveListMapPreference, index);
                    TrafficDetailFragment f = mSphinx.getTrafficDetailFragment();
                    TrafficQuery trafficQuery = f.getTrafficQuery();
                    Plan plan = f.findDriveType(driveTypeList[index]);

                    if (plan == null) {
                        TrafficQuery newTrafficQuery = new TrafficQuery(mContext);
                        newTrafficQuery.setup(trafficQuery.getStart(),
                                trafficQuery.getEnd(),
                                TrafficQuery.QUERY_TYPE_DRIVE,
                                ResultMapFragment.this.getId(),
                                getString(R.string.doing_and_wait));
                        newTrafficQuery.setCityId(trafficQuery.getCityId());
                        newTrafficQuery.addParameter(TrafficQuery.SERVER_PARAMETER_BIAS, String.valueOf(driveTypeList[index]));
                        mSphinx.queryStart(newTrafficQuery);
                    } else {
                        f.refreshDrive(plan);
                        TrafficOverlayHelper.drawOverlay(mSphinx, plan);
                        TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx);
                        TrafficOverlayHelper.showPlanInfoWindow(mSphinx);
                    }
                    
                    dialog.setOnDismissListener(new OnDismissListener() {
                        
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    dialog.dismiss();
                }
            });
            break;
        default:
            break;
        }

    }
    
    public boolean changeTrafficType(int type) {
        return changeTrafficType(type, true);
    }
    
    private boolean changeTrafficType(int type, boolean jumpTransferResultFragment) {
        boolean result = false;
        TrafficDetailFragment trafficDetailFragment = mSphinx.getTrafficDetailFragment();
        TrafficQuery trafficQuery = trafficDetailFragment.getTrafficQuery();

        if (type != Plan.Step.TYPE_TRANSFER && type != Plan.Step.TYPE_DRIVE && type != Plan.Step.TYPE_WALK) {
            return result;
        }
        
        if (!trafficDetailFragment.hasResult(type)) {
            TrafficQuery newTrafficQuery = new TrafficQuery(mContext);
            newTrafficQuery.setup(trafficQuery.getStart(),
                    trafficQuery.getEnd(),
                    type,
                    this.getId(),
                    getString(R.string.doing_and_wait));
            newTrafficQuery.setCityId(trafficQuery.getCityId());
            mSphinx.queryStart(newTrafficQuery);
        } else {
            
            trafficDetailFragment.refreshResult(type);
            List<Plan> list = trafficDetailFragment.getResult(type);
            if (type == Plan.Step.TYPE_TRANSFER) {
                if (jumpTransferResultFragment) {
                    if (mSphinx.uiStackPeek() == R.id.view_result_map) {
                        mSphinx.uiStackRemove(R.id.view_result_map);
                    }
                    TrafficQuery newTrafficQuery = mSphinx.getTrafficResultFragment().getTrafficQuery();
                    mSphinx.getTrafficResultFragment().setData(newTrafficQuery);
                    mSphinx.showView(R.id.view_traffic_result_transfer);
                } else {
                    mActionTag = ActionLog.TrafficTransferMap;
                    mRightBtn.setVisibility(View.INVISIBLE);
                }
                result = true;
            } else if (type == Plan.Step.TYPE_DRIVE) {
                mActionTag = ActionLog.TrafficDriveListMap;
                mRightBtn.setText(R.string.preference);
                mRightBtn.setVisibility(View.VISIBLE);
                mRightBtn.setOnClickListener(this);

                trafficDetailFragment.refreshDrive(list.get(0));

                TrafficOverlayHelper.drawOverlay(mSphinx, list.get(0));
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx);
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                
                result = true;
            } else if (type == Plan.Step.TYPE_WALK) {
                mActionTag = ActionLog.TrafficWalkListMap;
                mRightBtn.setVisibility(View.INVISIBLE);

                TrafficOverlayHelper.drawOverlay(mSphinx, list.get(0));
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx);
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                
                result = true;
            }
        }
        return result;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        
        if (mSphinx.uiStackContains(getId()) == false) {
            mResultData = null;
            mResultDataForSelectPoint = null;
            mResultDataForShowMap = null;
            mResultDataForPlanList = null;
            mResultDataForPlanDetail = null;
        }

        View view = mSphinx.getCenterTokenView();
        mSphinx.setTouchMode(TouchMode.NORMAL);
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.INVISIBLE);
            mSphinx.replaceBottomUI(null);
        }
    }
    
    private void checkRadioBtn(String tag) {
        if (ActionLog.TrafficDriveListMap.equals(tag)) {
            mTrafficDriveRbt.setChecked(true);
            TKConfig.setPref(mSphinx, TKConfig.PREFS_CHECKED_TRAFFIC_RADIOBUTTON, String.valueOf(Plan.Step.TYPE_DRIVE));
        } else if (ActionLog.TrafficWalkListMap.equals(tag)){
            mTrafficWalkRbt.setChecked(true);
            TKConfig.setPref(mSphinx, TKConfig.PREFS_CHECKED_TRAFFIC_RADIOBUTTON, String.valueOf(Plan.Step.TYPE_WALK));
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        if (TrafficQueryFragment.dealWithTrafficResponse(mSphinx,
                mActionTag,
                (TrafficQuery) tkAsyncTask.getBaseQuery(),
                false) && this.isShowing()) {
            checkRadioBtn(mActionTag);
        }
    }
    
    private ResultData mResultData;
    private ResultData mResultDataForShowMap; // 仅用于服务器要求将结果直接显示在地图上的情况，用来保存离开此页面时地图信息
    private ResultData mResultDataForPlanList; // 仅用于自驾或步行方案列表显示在地图上的情况，用来保存离开此页面时地图信息
    private ResultData mResultDataForPlanDetail; // 仅用于交通方案详情显示在地图上的情况，用来保存离开此页面时地图信息
    private ResultData mResultDataForSelectPoint; // 仅用于交通选点显示在地图上的情况，用来保存离开此页面时地图信息
    
    private void saveResultData() {
        MapScene mapScene = mSphinx.getMapView().getCurrentMapScene();
        mapScene.overlayItem = mSphinx.getInfoWindowFragment().getItemizedOverlay().getItemByFocused();
        ResultData resultData = new ResultData();
        resultData.mapScene =mapScene;
        resultData.actionTag = mActionTag;
        resultData.title = mTitle;
        if (mSphinx.uiStackSize() == 2 &&
                mSphinx.uiStackPeekBottom() == R.id.view_home &&
                mSphinx.uiStackPeek() == R.id.view_result_map) {
            mResultDataForShowMap = resultData;
        } else if ((mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_traffic_home ||
                mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_more_favorite ||
                mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_more_history) &&
                mSphinx.uiStackPeek() == R.id.view_result_map) {
            mResultDataForPlanList = resultData;
        } else if (mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_traffic_result_detail &&
                mSphinx.uiStackPeek() == R.id.view_result_map) {
            mResultDataForPlanDetail = resultData;
        } else if (mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_poi_input_search &&
                mSphinx.uiStackPeek() == R.id.view_result_map) {
            mResultDataForSelectPoint = resultData;
        } else {
            mResultData = resultData;
        }
    }
    
    private void restoreResultData() {
        ResultData resultData = null;
        if (mSphinx.uiStackSize() == 2 &&
                mSphinx.uiStackPeekBottom() == R.id.view_home &&
                mSphinx.uiStackPeek() == R.id.view_result_map &&
                mResultDataForShowMap != null) {
            resultData = mResultDataForShowMap;
        } else if ((mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_traffic_home ||
                mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_more_favorite ||
                mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_more_history) &&
                mSphinx.uiStackPeek() == R.id.view_result_map &&
                mResultDataForPlanList != null) {
            resultData = mResultDataForPlanList;
        } else if (mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_traffic_result_detail &&
                mSphinx.uiStackPeek() == R.id.view_result_map &&
                mResultDataForPlanDetail != null) {
            resultData = mResultDataForPlanDetail;
        } else if (mSphinx.uiStackGet(mSphinx.uiStackSize()-2) == R.id.view_poi_input_search &&
                mSphinx.uiStackPeek() == R.id.view_result_map) {
            resultData = mResultDataForSelectPoint;
        } else if (mResultData != null) {
            resultData = mResultData;
        }
        
        if (resultData != null) {
            mActionTag = resultData.actionTag;
            mTitle = resultData.title;
            mSphinx.clearMap();
            mSphinx.getMapView().restoreScene(resultData.mapScene);
        }
    }
    
    static class ResultData {
        MapScene mapScene;
        String actionTag;
        String title;
    }
}

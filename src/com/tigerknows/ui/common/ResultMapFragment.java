/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.common;

import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.OverlayItem;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.POI;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan.Step;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficDetailFragment;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
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
import android.widget.AdapterView.OnItemClickListener;

import java.util.List;

/**
 * @author Peng Wenyue
 */
public class ResultMapFragment extends BaseFragment implements View.OnClickListener {

    private String mTitle;
    private View mSnapView;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    private View mTrafficTitieView;
    private RadioGroup mTrafficTitleRadioGroup;
    private RadioButton mTrafficTransferRbt;
    private RadioButton mTrafficDriveRbt;
    private RadioButton mTrafficWalkRbt;
    
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
        
        mBottomFrament = mSphinx.getInfoWindowFragment();
        
        mTrafficTitieView = mLayoutInflater.inflate(R.layout.traffic_query_bar, null);
        
        findViews();
        setListener();
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
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
        if (ActionLog.TrafficTransferListMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_TRANSFER;
        } else if (ActionLog.TrafficDriveMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_DRIVE;
        } else if (ActionLog.TrafficWalkMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_WALK;
        }
        
        if (type > 0) {
            mTitleBtn.setVisibility(View.GONE);
            mTitleView.removeAllViews();
            mTitleView.addView(mTrafficTitieView);
            mRightBtn.setVisibility(View.INVISIBLE);
            changeTrafficType(type, false);
        }
        
        mSphinx.showHint(TKConfig.PREFS_HINT_LOCATION, R.layout.hint_location_map);
    }

    @Override
    public void onPause() {
        super.onPause();
        mTitleView.removeView(mTrafficTitieView);
    }
    
    public void setData(String title, String actionTag) {
        saveResultData(actionTag);
        mTitle = title;
        mActionTag = actionTag;
    }
    
    protected void findViews() {
        mSnapView = mRootView.findViewById(R.id.snap_view);
        mCancelBtn = (Button) mRootView.findViewById(R.id.cancel_btn);
        mConfirmBtn = (Button) mRootView.findViewById(R.id.confirm_btn);

        mTrafficTitleRadioGroup = (RadioGroup) mTrafficTitieView.findViewById(R.id.traffic_rgp);
        mTrafficTransferRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_transfer_rbt);
        mTrafficDriveRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_drive_rbt);
        mTrafficWalkRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_walk_rbt);
    }
    
    protected void setListener() {
        mCancelBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);

        View.OnTouchListener onTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int id = v.getId();
                if (mTrafficTitleRadioGroup.getCheckedRadioButtonId() == id) {
                    return true;
                }
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                if (action == MotionEvent.ACTION_UP) {
                    if (R.id.traffic_transfer_rbt == id) {
                        return !changeTrafficType(Plan.Step.TYPE_TRANSFER);
                    } else if (R.id.traffic_drive_rbt == id) {
                        return !changeTrafficType(Plan.Step.TYPE_DRIVE);
                    } else if (R.id.traffic_walk_rbt == id) {
                        return !changeTrafficType(Plan.Step.TYPE_WALK);
                    }
                }
                return false;
            }
        };
        mTrafficTransferRbt.setOnTouchListener(onTouchListener);
        mTrafficDriveRbt.setOnTouchListener(onTouchListener);
        mTrafficWalkRbt.setOnTouchListener(onTouchListener);
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
            int[] leftCompoundIconList = new int[3];
            leftCompoundIconList[0] = R.drawable.ic_share_sina;
            leftCompoundIconList[1] = R.drawable.ic_share_sina;
            leftCompoundIconList[2] = R.drawable.ic_share_sina;
            final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list, leftCompoundIconList);
            
            View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
            
            ListView listView = (ListView) alterListView.findViewById(R.id.listview);
            listView.setAdapter(adapter);
            
            final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
            
            TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
            titleTxv.setText(R.string.share);
            
            Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
            button.setVisibility(View.GONE);
            
            dialog.show();
            
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View arg1, int index, long arg3) {

                    TrafficDetailFragment trafficDetailFragment = mSphinx.getTrafficDetailFragment();
                    TrafficQuery trafficQuery = trafficDetailFragment.getTrafficQuery();
                    List<Plan> list = trafficDetailFragment.getResult(Step.TYPE_DRIVE);

                    int position = -1;
                    for(int i = list.size()-1; i >= 0; i--) {
                        if (list.get(i).drivePreference == index) {
                            position = i;
                        }
                    }
                    
                    if (position == -1) {
                        TrafficQuery newTrafficQuery = new TrafficQuery(mContext);
                        newTrafficQuery.setup(trafficQuery.getStart(),
                                trafficQuery.getEnd(),
                                TrafficQuery.QUERY_TYPE_DRIVE,
                                ResultMapFragment.this.getId(),
                                getString(R.string.doing_and_wait));
                        newTrafficQuery.setCityId(trafficQuery.getCityId());
                        newTrafficQuery.addParameter(TrafficQuery.SERVER_PARAMETER_BAIS, String.valueOf(index));
                        mSphinx.queryStart(newTrafficQuery);
                    } else {
                        trafficDetailFragment.addResult(trafficQuery, Step.TYPE_DRIVE, list);
                        trafficDetailFragment.refreshResult(Step.TYPE_DRIVE, position);
                        trafficDetailFragment.viewMap();
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
        mSphinx.resetLoactionButtonState();
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
                    ResultMapFragment.this.getId(),
                    getString(R.string.doing_and_wait));
            newTrafficQuery.setCityId(trafficQuery.getCityId());
            mSphinx.queryStart(newTrafficQuery);
        } else {
            trafficDetailFragment.refreshResult(type);
            List<Plan> list = trafficDetailFragment.getResult(type);
            if (type == Plan.Step.TYPE_TRANSFER) {
                if (jumpTransferResultFragment) {
                    TrafficQuery newTrafficQuery = mSphinx.getTrafficResultFragment().getTrafficQuery();
                    mSphinx.getTrafficResultFragment().setData(newTrafficQuery);
                    mSphinx.showView(R.id.view_traffic_result_transfer);
                } else {
                    mActionTag = ActionLog.TrafficTransferListMap;
                    mTrafficTransferRbt.setChecked(true);
                    mRightBtn.setVisibility(View.GONE);
                }
                result = true;
            } else if (type == Plan.Step.TYPE_DRIVE) {
                mActionTag = ActionLog.TrafficDriveMap;
                mTrafficDriveRbt.setChecked(true);
                mRightBtn.setText(R.string.preference);
                mRightBtn.setVisibility(View.VISIBLE);
                mRightBtn.setOnClickListener(this);
                
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx.getMapView(), mSphinx);
                result = true;
            } else if (type == Plan.Step.TYPE_WALK) {
                mActionTag = ActionLog.TrafficWalkMap;
                mTrafficWalkRbt.setChecked(true);
                mRightBtn.setVisibility(View.GONE);

                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx.getMapView(), mSphinx);
                result = true;
            }
        }
        return result;
    }
    
    @Override
    public void dismiss() {
        super.dismiss();

        View view = mSphinx.getCenterTokenView();
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.INVISIBLE);
            mSphinx.replaceBottomUI(null);
        }
        restoreDataBean();
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        TrafficQueryFragment.dealWithTrafficResponse(mSphinx,
                mActionTag,
                (TrafficQuery) tkAsyncTask.getBaseQuery(),
                false);
    }
    
    // 在进入到交通选点界面和交通结果地图界面时，保存之前的地图相关信息
    private ResultData mResultData;
    
    private void saveResultData(String actionTag) {

        if (ActionLog.ResultMapSelectPoint.equals(actionTag) ||
                ActionLog.TrafficTransferMap.equals(actionTag) ||
                ActionLog.TrafficDriveMap.equals(actionTag) ||
                ActionLog.TrafficWalkMap.equals(actionTag) ||
                ActionLog.TrafficTransferListMap.equals(actionTag)) {
            
            if (ActionLog.ResultMapSelectPoint.equals(mActionTag) == false &&
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
        if (ActionLog.ResultMapSelectPoint.equals(mActionTag) ||
                ActionLog.TrafficTransferMap.equals(mActionTag) ||
                ActionLog.TrafficDriveMap.equals(mActionTag) ||
                ActionLog.TrafficWalkMap.equals(mActionTag) ||
                ActionLog.TrafficTransferListMap.equals(mActionTag)) {
            
            if (mResultData != null) {
                if (ActionLog.ResultMapSelectPoint.equals(mResultData.actionTag) == false &&
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

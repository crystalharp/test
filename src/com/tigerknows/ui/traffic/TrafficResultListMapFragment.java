package com.tigerknows.ui.traffic;

import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
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

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

public class TrafficResultListMapFragment extends BaseFragment implements View.OnClickListener{

    //只用来显示驾车和步行概要地图
    public TrafficResultListMapFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private String mTitle;
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
            if (action == MotionEvent.ACTION_UP) {
                if (R.id.traffic_transfer_rbt == id) {
                    mActionLog.addAction(mActionTag, ActionLog.TrafficTransferTab);
                    return !changeTrafficType(Plan.Step.TYPE_TRANSFER);
                } else if (R.id.traffic_drive_rbt == id) {
                    mActionLog.addAction(mActionTag, ActionLog.TrafficDriveTab);
                    return !changeTrafficType(Plan.Step.TYPE_DRIVE);
                } else if (R.id.traffic_walk_rbt == id) {
                    mActionLog.addAction(mActionTag, ActionLog.TrafficWalkTab);
                    return !changeTrafficType(Plan.Step.TYPE_WALK);
                }
            }
            return false;
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
        mSphinx.getHomeFragment().reset();
        mSphinx.getMapView().setStopRefreshMyLocation(false);
        mTitleBtn.setText(mTitle);
        setOnTouchListener(null);
                
        int type = 0;
        if (ActionLog.TrafficDriveListMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_DRIVE;
        } else if (ActionLog.TrafficWalkListMap.equals(mActionTag)) {
            type = Plan.Step.TYPE_WALK;
        }
        
        mTitleBtn.setVisibility(View.GONE);
        mTitleView.removeAllViews();
        mTrafficTitieView = mSphinx.getTrafficQueryFragment().getTitleView();
        mTrafficTitleRadioGroup = (RadioGroup) mTrafficTitieView.findViewById(R.id.traffic_rgp);
        mTitleView.addView(mTrafficTitieView);

        mTrafficTransferRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_transfer_rbt);
        mTrafficDriveRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_drive_rbt);
        mTrafficWalkRbt = (RadioButton) mTrafficTitieView.findViewById(R.id.traffic_walk_rbt);
        mTrafficTransferRbt.setOnTouchListener(onTouchListener);
        mTrafficDriveRbt.setOnTouchListener(onTouchListener);
        mTrafficWalkRbt.setOnTouchListener(onTouchListener);
        if (ActionLog.TrafficDriveListMap.equals(mActionTag)) {
            mTrafficDriveRbt.setChecked(true);
        } else {
            mTrafficWalkRbt.setChecked(true);
        }

        mRightBtn.setVisibility(View.INVISIBLE);
        changeTrafficType(type, false);
    }

    @Override
    public void onPause() {
        if (this.isShowing()) {
            mTitleView.removeView(mTrafficTitieView);
            if (mTrafficTitleRadioGroup != null) {
                mTrafficTransferRbt.setOnTouchListener(null);
                mTrafficDriveRbt.setOnTouchListener(null);
                mTrafficWalkRbt.setOnTouchListener(null);
            }
        }
        super.onPause();
    }
    
    public void setData(String title, String actionTag) {
        mTitle = title;
        mActionTag = actionTag;
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {

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
            
            listView.setOnItemClickListener(new OnItemClickListener() {

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
                                TrafficResultListMapFragment.this.getId(),
                                getString(R.string.doing_and_wait));
                        newTrafficQuery.setCityId(trafficQuery.getCityId());
                        newTrafficQuery.addParameter(TrafficQuery.SERVER_PARAMETER_BIAS, String.valueOf(driveTypeList[index]));
                        mSphinx.queryStart(newTrafficQuery);
                    } else {
                        f.refreshDrive(plan);
                        TrafficOverlayHelper.drawOverlay(mSphinx, plan);
                        TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), mSphinx);
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
                    TrafficQuery newTrafficQuery = mSphinx.getTrafficResultFragment().getTrafficQuery();
                    mSphinx.getTrafficResultFragment().setData(newTrafficQuery);
                    mSphinx.showView(R.id.view_traffic_result_transfer);
                } else {
                    mActionTag = ActionLog.TrafficTransferListMap;
                    mRightBtn.setVisibility(View.INVISIBLE);
                }
                result = true;
            } else if (type == Plan.Step.TYPE_DRIVE) {
                mActionTag = ActionLog.TrafficDriveListMap;
                mRightBtn.setText(R.string.preference);
                mRightBtn.setVisibility(View.VISIBLE);
                mRightBtn.setOnClickListener(this);
                
                TrafficOverlayHelper.drawTrafficPlanListOverlay(mSphinx, list, 0);
                TrafficOverlayHelper.panToViewWholeOverlay(list.get(0), mSphinx.getMapView(), mSphinx);
                result = true;
            } else if (type == Plan.Step.TYPE_WALK) {
                mActionTag = ActionLog.TrafficWalkListMap;
                mRightBtn.setVisibility(View.INVISIBLE);

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
        mSphinx.setTouchMode(TouchMode.NORMAL);
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.INVISIBLE);
            mSphinx.replaceBottomUI(null);
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        if (TrafficQueryFragment.dealWithTrafficResponse(mSphinx,
                mActionTag,
                (TrafficQuery) tkAsyncTask.getBaseQuery(),
                false) && this.isShowing()) {
            if (ActionLog.TrafficDriveListMap.equals(mActionTag)) {
                mTrafficDriveRbt.setChecked(true);
            } else if (ActionLog.TrafficWalkListMap.equals(mActionTag)){
                mTrafficWalkRbt.setChecked(true);
            }
        }
    }
}

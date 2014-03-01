/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.ui;

import java.util.ArrayList;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.common.ActionLog;
import com.tigerknows.common.AsyncImageLoader;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.DiscoverCategoreResponse;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.DataQuery.POIResponse.CityIdAndResultTotal;
import com.tigerknows.model.DataQuery.POIResponse.MapCenterAndBorderRange;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.ui.poi.POIResultFragment;
import com.tigerknows.ui.traffic.BuslineResultLineFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.StringArrayAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Peng Wenyue
 */
public class BaseFragment extends LinearLayout {

    protected static final String TAG = "BaseFragment";
    
    public Sphinx mSphinx;
    
    public Context mContext;
    
    public ActionLog mActionLog;
    
    public String mActionTag;
    
    protected LayoutInflater mLayoutInflater;
    
    public View mRootView = null;
    
    public TitleFragment mTitleFragment = null;
    
    public BaseFragment mBottomFragment = null;
    
    public TKEditText mKeywordEdt;
    
    public Button mTitleBtn;
    
    public Button mLeftBtn;
    
    public Button mRightBtn;
    
    protected static boolean sRefreshedDiscoverCities;
    
    /**
     * 最顶的View，其高度为0
     */
    public View mSkylineView;
    
    public boolean isReLogin = false;
    
    protected List<BaseQuery> mBaseQuerying;
    
    protected TKAsyncTask mTkAsyncTasking;
    
    protected PopupWindow mPopupWindow;
    
    public boolean mDismissed = true;
    
    public int mHeight;
    
    public PopupWindow getPopupWindow() {
        return mPopupWindow;
    }

    public void setPopupWindow(PopupWindow popupWindow) {
        this.mPopupWindow = popupWindow;
    }

    public DialogInterface.OnClickListener mCancelLoginListener = new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface arg0, int arg1) {
            isReLogin();
        }
    };
    
    public boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
            	for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
	                mBaseQuerying.get(i).setResponse(null);
            	}
                mSphinx.queryStart(mBaseQuerying);
            }
        }
        return isRelogin;
    }
    
    private View.OnClickListener mLeftBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View arg0) {
            synchronized (mSphinx.mUILock) {
        	    mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
            	dismiss();
            }
        }
    };
    
    public BaseFragment(Sphinx sphinx) {
        super(sphinx);
        setId(R.id.view_invalid);
        mSphinx = sphinx;
    }
    
    public void onCreate(Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreate()"+mActionTag);
        mContext = mSphinx.getBaseContext();
        mActionLog = ActionLog.getInstance(mContext);
        mLayoutInflater = mSphinx.getLayoutInflater();
        
        int id = getId();
        if (id != R.id.view_invalid) {
            mTitleFragment = mSphinx.getTitleFragment();            
            mTitleBtn = mTitleFragment.mTitleBtn;
            mKeywordEdt = mTitleFragment.mKeywordEdt;
            mLeftBtn = mTitleFragment.mLeftBtn;
            mRightBtn = mTitleFragment.mRightBtn;
            mSkylineView = mTitleFragment.mSkylineView;
        }
        
        onCreateView(null, this, null);
        if (mRootView != null) {
            mRootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mHeight = mRootView.getMeasuredHeight();
            addView(mRootView);
        }
    }
    
    public void dismiss() {
        onPause();
        mSphinx.uiStackDismiss(getId());
        
        if (mTkAsyncTasking != null) {
            mTkAsyncTasking.stop();
        }
        mDismissed = true;
    }
    
    public void show() {    
        mSphinx.uiStackPush(getId());  
        onResume();
        mDismissed = false;
    }
    
    public void hide() {    
        this.setVisibility(View.GONE);
        onPause();
    }
    
    public void display() {  
        this.setVisibility(View.VISIBLE);
        onResume();
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onActivityCreated()"+mActionTag);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogWrapper.d(TAG, "onActivityResult()"+mActionTag);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        return null;
    }
    
    protected void findViews() {
    }

    protected void setListener() {
        setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    public void onAttach(Activity activity) {
        LogWrapper.d(TAG, "onAttach()"+mActionTag);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogWrapper.d(TAG, "onConfigurationChanged()"+mActionTag);
        super.onConfigurationChanged(newConfig);
    }

    public boolean onContextItemSelected(MenuItem item) {
        LogWrapper.d(TAG, "onContextItemSelected()"+mActionTag);
        return false;
    }

    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        LogWrapper.d(TAG, "onCreateAnimator()"+mActionTag);
        return null;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        LogWrapper.d(TAG, "onCreateContextMenu()"+mActionTag);
    }

    public void onDestroy() {
        LogWrapper.d(TAG, "onDestroy()"+mActionTag);
    }

    public void onDestroyOptionsMenu() {
        LogWrapper.d(TAG, "onDestroyOptionsMenu()"+mActionTag);
    }

    public void onDestroyView() {
        LogWrapper.d(TAG, "onDestroyView()"+mActionTag);
    }

    public void onDetach() {
        LogWrapper.d(TAG, "onDetach()"+mActionTag);
    }

    public void onHiddenChanged(boolean hidden) {
        LogWrapper.d(TAG, "onHiddenChanged()"+mActionTag);
    }

    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onInflate()"+mActionTag);
    }

    public void onLowMemory() {
        LogWrapper.d(TAG, "onLowMemory()"+mActionTag);
    }

    public void onOptionsMenuClosed(Menu menu) {
        LogWrapper.d(TAG, "onOptionsMenuClosed()"+mActionTag);
    }

    public void onPause() {
        LogWrapper.d(TAG, "onPause()"+mActionTag);
        int id = getId();
        if (id != R.id.view_invalid && id != R.id.view_infowindow) {
            dismissPopupWindow();
            mSphinx.hideSoftInput();
            mSphinx.resetLoactionButtonState();
            if (!TextUtils.isEmpty(mActionTag)) {
                mActionLog.addAction(mActionTag + ActionLog.Dismiss);
            }
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        LogWrapper.d(TAG, "onPrepareOptionsMenu()"+mActionTag);
    }

    public void onResume() {
        LogWrapper.d(TAG, "onResume()"+mActionTag);
        int id = getId();
        if (id != R.id.view_invalid && id != R.id.view_infowindow) { 
            if (!TextUtils.isEmpty(mActionTag)) {
                mActionLog.addAction(mActionTag);
            }
            
            if (R.id.view_home == getId()) {
                mSphinx.getTitleView().getChildAt(0).setVisibility(View.VISIBLE);
                mSphinx.getTitleView().getChildAt(1).setVisibility(View.GONE);
            } else {
                mSphinx.getTitleView().getChildAt(0).setVisibility(View.GONE);
                mSphinx.getTitleView().getChildAt(1).setVisibility(View.VISIBLE);
            }
            mSphinx.replaceBodyUI(this);
            mSphinx.replaceBottomUI(this);
            mTitleFragment.reset();
            mTitleFragment.mLeftBtn.setOnClickListener(mLeftBtnOnClickListener);
            
            AsyncImageLoader.getInstance().setViewToken(toString());
        	mSphinx.getMapView().setStopRefreshMyLocation(true);
        }
        correctUIStack();
    }
    
    public void correctUIStack(){
    	// DO NOT DELETE virtual method
    }
    
    public void onSaveInstanceState(Bundle outState) {
        LogWrapper.d(TAG, "onSaveInstanceState()"+mActionTag);
    }

    public void onStart() {
        LogWrapper.d(TAG, "onStart()"+mActionTag);
    }

    public void onStop() {
        LogWrapper.d(TAG, "onStop()"+mActionTag);
    }
    
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        LogWrapper.d(TAG, "onCreateOptionsMenu()"+mActionTag);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        LogWrapper.d(TAG, "onOptionsItemSelected()"+mActionTag);
        return false;
    }
    
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return false;
    }

    public void onCancelled(TKAsyncTask tkAsyncTask) {
        mTkAsyncTasking = null;
    }

    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        mTkAsyncTasking = null;
    }
    
    public boolean isShowing() {
        return mSphinx.uiStackPeek() == getId();
    }
    
    Runnable dismissPopupWindowTask = new Runnable() {
		@Override
		public void run() {
            mPopupWindow.dismiss();
		}
	};
    
    public void dismissPopupWindow() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
        	mRootView.post(dismissPopupWindowTask);
        }
    }
    
    public void setTkAsyncTasking(TKAsyncTask tkAsyncTask) {
    	this.mTkAsyncTasking = tkAsyncTask;
    }
    
    public void setBaseQuerying(List<BaseQuery> baseQuerying) {
    	this.mBaseQuerying = baseQuerying;
    }
    
    public String getLogTag() throws Exception{
    	throw new Exception("getTag must be overrided by subclasss to use log*!");
    }

    public void logI(String msg){
    	try {
			LogWrapper.i(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void logD(String msg){
    	try {
			LogWrapper.d(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void logW(String msg){
    	try {
			LogWrapper.w(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void logE(String msg){
    	try {
			LogWrapper.e(getLogTag(), msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public final String getString(int resId) {
        return mContext.getString(resId);
    }
    
    public final String getString(int resId, Object... formatArgs) {
        return mContext.getString(resId, formatArgs);
    }
    
    public static final void setTxvText(TextView txv, String s) {
        if (TextUtils.isEmpty(s)) {
            txv.setVisibility(View.GONE);
        } else {
            txv.setVisibility(View.VISIBLE);
            txv.setText(s);
        }
    }

    

    /**
     * 处理POI搜索结果，实现界面跳转和给出相应提示
     */
    public static int dealWithPOIResponse(final DataQuery dataQuery, final Sphinx sphinx, BaseFragment baseFragment) {
        int result = -4;
        if (dataQuery.isStop()) {
            result = -3;
            return result;
        }
        
        if (BaseActivity.checkReLogin(dataQuery, sphinx, sphinx.uiStackContains(R.id.view_user_home), baseFragment.getId(), baseFragment.getId(), baseFragment.getId(), baseFragment.mCancelLoginListener)) {
            baseFragment.isReLogin = true;
            result = -2;
            return result;
        }
        
        boolean isBuslineTurnPage = dataQuery.hasLocalParameter(BuslineResultLineFragment.BUSLINE_TURNPAGE);
        if (BaseActivity.hasAbnormalResponseCode(dataQuery, sphinx, isBuslineTurnPage ? BaseActivity.SHOW_NOTHING : BaseActivity.SHOW_DIALOG, baseFragment, false)) {
            if (isBuslineTurnPage) {
                sphinx.getBuslineResultLineFragment().setData(null, dataQuery);
            }
            result = -1;
            return result;
        }

        POIResponse poiResponse = (POIResponse)dataQuery.getResponse();
        
        // 公交线路数据
        // 存在此数据则跳转到公交线路列表结果界面
        BuslineModel buslineModel = poiResponse.getBuslineModel();
        if (buslineModel != null) {
            if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
                sphinx.getBuslineResultLineFragment().setData(null, dataQuery);
                if (dataQuery.isTurnPage() == false) {
                    sphinx.showView(R.id.view_traffic_busline_line_result);
                    sphinx.uiStackRemove(R.id.view_poi_input_search);
                }
                result = POIResponse.FIELD_EXT_BUSLINE;
                return result;
            }
        }
        
        // 地图的中心和边界范围数据
        // 若存在此数据则设置地图中心及比例尺，跳转到首页
        final MapView mapView = sphinx.getMapView();
        MapCenterAndBorderRange mapCenterAndBorderRange = poiResponse.getMapCenterAndBorderRange();
        if (mapCenterAndBorderRange != null) {
            
            Position mapCenter = mapCenterAndBorderRange.getMapCenter();
            float zoomLevel = mapView.getZoomLevel();
            
            ArrayList<Position> borderRange = mapCenterAndBorderRange.getBorderRange();
            if (borderRange != null
                    && borderRange.size() > 0) {
                
                DisplayMetrics displayMetrics = Globals.g_metrics;
                try {
                    Rect rect = mapView.getPadding();
                    zoomLevel = Util.getZoomLevelToFitPositions(displayMetrics.widthPixels,
                            displayMetrics.heightPixels,
                            rect,
                            borderRange);
                } catch (APIException e) {
                    e.printStackTrace();
                }
                
            }
            
            if (mapCenter != null) {
                mapView.centerOnPosition(mapCenter, zoomLevel);
                sphinx.getHomeFragment().reset();
                sphinx.uiStackClearTop(R.id.view_home);
                result = POIResponse.FIELD_MAP_CENTER_AND_BORDER_RANGE;
                return result;
            }
        }
        
        // 城市及结果数量列表数据
        final List<CityIdAndResultTotal> cityIdAndResultTotalList = poiResponse.getCityIdAndResultTotalList();
        if (cityIdAndResultTotalList != null && cityIdAndResultTotalList.size() > 0) {
            // 若只存在一个城市及结果数量时，以城市中心位置发起搜索，不移动地图
            if (cityIdAndResultTotalList.size() == 1) {
                int cityId = (int) cityIdAndResultTotalList.get(0).getCityId();
                CityInfo cityInfo = MapEngine.getCityInfo(cityId);
                if (cityInfo.isAvailably()) {
                    DataQuery newDataQuery = new DataQuery(dataQuery);
                    Position position = cityInfo.getPosition();
                    newDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_CENTER_LONGITUDE, String.valueOf(position.getLon()));
                    newDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_CENTER_LATITUDE, String.valueOf(position.getLat()));
                    newDataQuery.setCityId(cityId);
                    sphinx.queryStart(newDataQuery);
                    result = POIResponse.FIELD_CITY_ID_AND_RESULT_TOTAL;
                    return result;
                }
                
            // 若存在多个城市及结果时，提示城市列表，用户选择后以城市中心位置发起搜索，不移动地图
            } else {
                final List<CityInfo> cityList = new ArrayList<CityInfo>();
                List<String> cityNameList = new ArrayList<String>();
                for(int i = 0, size = cityIdAndResultTotalList.size(); i < size; i++) {
                    CityIdAndResultTotal cityIdAndResultTotal = cityIdAndResultTotalList.get(i);
                    CityInfo cityInfo = MapEngine.getCityInfo((int) cityIdAndResultTotal.getCityId());
                    if (cityInfo.isAvailably()) {
                        cityList.add(cityInfo);
                        cityNameList.add(cityInfo.getCName()+"("+cityIdAndResultTotal.getResultTotal()+")");
                    }
                }
                
                StringArrayAdapter adapter = new StringArrayAdapter(sphinx, cityNameList);
                
                View alterListView = sphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
                
                final Dialog dialog = Utility.getChoiceDialog(sphinx, alterListView, R.style.AlterChoiceDialog);
                
                ListView listView = (ListView) alterListView.findViewById(R.id.listview);
                listView.setAdapter(adapter);
                
                listView.setOnItemClickListener(new OnItemClickListener() {
        
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
                        CityIdAndResultTotal cityIdAndResultTotal = cityIdAndResultTotalList.get(position);
                        DataQuery newDataQuery = new DataQuery(dataQuery);
                        Position pos = cityList.get(position).getPosition();
                        newDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_CENTER_LONGITUDE, String.valueOf(pos.getLon()));
                        newDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_CENTER_LATITUDE, String.valueOf(pos.getLat()));
                        newDataQuery.setCityId((int) cityIdAndResultTotal.getCityId());
                        sphinx.queryStart(newDataQuery);
                        dialog.dismiss();
                    }
                });
                
                
                TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
                titleTxv.setText(R.string.cityid_and_result_total_list);
                
                Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
                button.setVisibility(View.GONE);
                
                dialog.show();

                result = POIResponse.FIELD_CITY_ID_AND_RESULT_TOTAL;
                return result;
            }
        }        
        
        // POI结果列表
        POIList bPOIList = poiResponse.getBPOIList();
        String subDataType = dataQuery.getParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE);
        if (DataQuery.SUB_DATA_TYPE_HOTEL.equals(subDataType)) {
            if (bPOIList == null || bPOIList.getList() == null || bPOIList.getList().isEmpty()) {
                Toast.makeText(sphinx, sphinx.getString(R.string.no_result), Toast.LENGTH_LONG).show();
                return 2;
            }
        }
        POIResultFragment poiResultFragment = sphinx.getPOIResultFragment();
        // 跳转到POI结果列表界面
        if (bPOIList.getShowType() == 0) {
            sphinx.uiStackRemove(R.id.view_poi_result);
            poiResultFragment.setData(dataQuery, true);
            sphinx.showView(R.id.view_poi_result);
            poiResultFragment.setSelectionFromTop();
        
        // 跳转到POI结果地图界面
        } else if (bPOIList.getShowType() == 1) {
            if (bPOIList == null || bPOIList.getList() == null || bPOIList.getList().isEmpty()) {
                Toast.makeText(sphinx, sphinx.getString(R.string.no_result), Toast.LENGTH_LONG).show();
                return 2;
            } else {
                poiResultFragment.setData(dataQuery, true);

                sphinx.uiStackClearTop(R.id.view_home);
                sphinx.getResultMapFragment().setData(sphinx.getString(R.string.result_map), ActionLog.POIListMap);
                sphinx.showView(R.id.view_result_map);
                // 下面这行代码本来是应该在sphinx.getResultMapFragment().setData（）之前执行，
                // 由于sphinx.uiStackClearTop(R.id.view_home)的特殊逻辑将其移到此处
                ItemizedOverlayHelper.drawPOIOverlay(sphinx, bPOIList.getList(), 0, poiResultFragment.getAPOI());
            }
        }
        
        // 若从公交线路结果列表界面跳转过来，则将其从UI堆栈中移除
        sphinx.uiStackRemove(R.id.view_traffic_busline_line_result);
        
        // 若是在周边搜索页或者从周边搜索页过来，则需要处理UI堆栈信息
        sphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
        
        result = 3;
        return result;
    }
    
    /**
     * 处理公交线路结果，实现界面跳转和给出相应提示
     * @param sphinx
     * @param buslineQuery
     * @param actionTag
     * @param listView
     */
    public static void dealWithBuslineResponse(Sphinx sphinx, BuslineQuery buslineQuery, String actionTag, SpringbackListView listView) {
        
        ActionLog actionLog = ActionLog.getInstance(sphinx);
        BuslineModel buslineModel = buslineQuery.getBuslineModel();
        
        if (buslineModel == null) {
            
            actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, -1);
            if (buslineQuery.isTurnPage() && listView != null) {
                listView.setFooterLoadFailed(true);
            } else if (buslineQuery.getStatusCode() == BaseQuery.STATUS_CODE_NONE) {
                sphinx.showTip(R.string.network_failed, Toast.LENGTH_SHORT);
            } else {
                sphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
            }
            
        } else if (buslineModel.getType() == BuslineModel.TYPE_EMPTY) {
            
            actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, -2);
            sphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
            
        } else if (buslineModel.getType() == BuslineModel.TYPE_UNSUPPORT) {
            
            actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, -3);
            sphinx.showTip(R.string.busline_not_support, Toast.LENGTH_SHORT);
            
        } else if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE 
                || buslineModel.getType() == BuslineModel.TYPE_STATION) {
            
            if ((buslineModel.getLineList() == null || buslineModel.getLineList().size() <= 0)
                    &&  (buslineModel.getStationList() == null || buslineModel.getStationList().size() <= 0)) {
                
                sphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
                actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, 0);
                
            } else {
                
                if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
                    actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, buslineQuery.getBuslineModel().getLineList().size());
                    sphinx.getBuslineResultLineFragment().setData(buslineQuery);
                    if (buslineQuery.isTurnPage() == false) {
                        sphinx.showView(R.id.view_traffic_busline_line_result);
                    }
                } else if (buslineModel.getType() == BuslineModel.TYPE_STATION) {
                    actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, buslineQuery.getBuslineModel().getStationList().size());
                    sphinx.getBuslineResultStationFragment().setData(buslineQuery);
                    if (buslineQuery.isTurnPage() == false) {
                        sphinx.showView(R.id.view_traffic_busline_station_result);
                    }
                }               
            }
        }
    }

    /**
     * 处理发现频道动态POI搜索结果，实现界面跳转和给出相应提示
     */    
    public static void dealWithDynamicPOIResponse(final DataQuery dataQuery, Sphinx sphinx, BaseFragment baseFragment) {
        Response response = dataQuery.getResponse();
        // check whether the use has loged in at another device
        if (BaseActivity.checkReLogin(dataQuery, sphinx, sphinx.uiStackContains(R.id.view_user_home), baseFragment.getId(), baseFragment.getId(), baseFragment.getId(), baseFragment.mCancelLoginListener)) {
        	baseFragment.isReLogin = true;
            return;
        }
        
        if (BaseActivity.hasAbnormalResponseCode(dataQuery, sphinx, BaseActivity.SHOW_DIALOG, baseFragment, false, new int[]{Response.RESPONSE_CODE_DISCOVER_NO_SUPPORT})) {
            return;
        }
        
        int responseCode = response.getResponseCode();
        if (responseCode == Response.RESPONSE_CODE_DISCOVER_NO_SUPPORT){
            int resId = R.string.no_result;
            String dataType = dataQuery.getParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE);
            if (DataQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                        resId = R.string.this_city_not_support_tuangou;
                    } else if (DataQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                        resId = R.string.this_city_not_support_dianying;
            } else if (DataQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                resId = R.string.this_city_not_support_yanchu;
            } else if (DataQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                resId = R.string.this_city_not_support_zhanlan;
            }
            Toast.makeText(sphinx, resId, Toast.LENGTH_LONG).show();
            return;
        }
         
        boolean noResult = true;
        if (response instanceof DiscoverCategoreResponse) {
            DiscoverCategoreResponse discoverCategoreResponse = (DiscoverCategoreResponse)dataQuery.getResponse();
            if (discoverCategoreResponse.getDiscoverResult() != null 
                    && discoverCategoreResponse.getDiscoverResult().getList() != null 
                    && discoverCategoreResponse.getDiscoverResult().getList().size() > 0) {
                
                noResult = false;
            }
            
        }
        
        if (noResult) {
            Toast.makeText(sphinx, R.string.no_result, Toast.LENGTH_LONG).show();
        } else {
        	sphinx.getDiscoverListFragment().setData(dataQuery, true);
        	sphinx.showView(R.id.view_discover_list);
        	sphinx.getDiscoverListFragment().setSelectionFromTop();
        }
    }
    
	
	protected void refreshDiscoverCities(){
	    if (sRefreshedDiscoverCities == false) {
	        sRefreshedDiscoverCities = true;
            DataQuery dataQuery = new DataQuery(mSphinx);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DISCOVER);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
            dataQuery.setup(-1, -1, null, true);
            mSphinx.queryStart(dataQuery);
	    }
	}
        
	public void addAction(String actionTag, Object... args) {
	    mActionLog.addAction(mActionTag + actionTag, args);
	}
}

/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tigerknows.R;

import com.decarta.Globals;
import com.decarta.android.event.EventRegistry;
import com.decarta.android.event.EventSource;
import com.decarta.android.exception.APIException;
import com.decarta.android.map.Circle;
import com.decarta.android.map.Compass;
import com.decarta.android.map.Icon;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.MyLocation;
import com.decarta.android.map.OverlayItem;
import com.decarta.android.map.RotationTilt;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.map.Shape;
import com.decarta.android.scale.Length;
import com.decarta.android.scale.Length.UOM;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYFloat;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.common.AsyncImageLoader;
import com.tigerknows.common.ImageCache;
import com.tigerknows.common.LogUpload;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.map.MapView.DownloadEventListener;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.map.label.Label;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.HotelVendor;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.model.NoticeQuery;
import com.tigerknows.model.PullMessage;
import com.tigerknows.model.PullMessage.Message.PulledProductMessage;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.User;
import com.tigerknows.model.BootstrapModel.StartupDisplay;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.radar.TKNotificationManager;
import com.tigerknows.service.MapDownloadService;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.service.SuggestLexiconService;
import com.tigerknows.share.TKWeixin;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.HomeBottomFragment;
import com.tigerknows.ui.HomeFragment;
import com.tigerknows.ui.InfoWindowFragment;
import com.tigerknows.ui.TitleFragment;
import com.tigerknows.ui.common.BrowserActivity;
import com.tigerknows.ui.common.BrowserFragment;
import com.tigerknows.ui.common.HintActivity;
import com.tigerknows.ui.common.MeasureDistanceFragment;
import com.tigerknows.ui.common.ResultMapFragment;
import com.tigerknows.ui.common.TakeScreenshotActivity;
import com.tigerknows.ui.common.ViewImageActivity;
import com.tigerknows.ui.discover.DianyingDetailFragment;
import com.tigerknows.ui.discover.DiscoverChildListFragment;
import com.tigerknows.ui.discover.DiscoverListFragment;
import com.tigerknows.ui.discover.TuangouDetailFragment;
import com.tigerknows.ui.discover.YanchuDetailFragment;
import com.tigerknows.ui.discover.ZhanlanDetailFragment;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.hotel.HotelIntroActivity;
import com.tigerknows.ui.hotel.HotelOrderCreditFragment;
import com.tigerknows.ui.hotel.HotelOrderDetailFragment;
import com.tigerknows.ui.hotel.HotelOrderListFragment;
import com.tigerknows.ui.hotel.HotelOrderSuccessFragment;
import com.tigerknows.ui.hotel.HotelOrderWriteFragment;
import com.tigerknows.ui.hotel.PickLocationFragment;
import com.tigerknows.ui.more.AboutUsActivity;
import com.tigerknows.ui.more.AddMerchantActivity;
import com.tigerknows.ui.more.AppRecommendActivity;
import com.tigerknows.ui.more.ChangeCityActivity;
import com.tigerknows.ui.more.FavoriteFragment;
import com.tigerknows.ui.more.FeedbackActivity;
import com.tigerknows.ui.more.GoCommentFragment;
import com.tigerknows.ui.more.HistoryFragment;
import com.tigerknows.ui.more.MapDownloadActivity;
import com.tigerknows.ui.more.MoreHomeFragment;
import com.tigerknows.ui.more.MyOrderFragment;
import com.tigerknows.ui.more.SatisfyRateActivity;
import com.tigerknows.ui.more.SettingActivity;
import com.tigerknows.ui.poi.CommentListActivity;
import com.tigerknows.ui.poi.CouponDetailFragment;
import com.tigerknows.ui.poi.CouponListFragment;
import com.tigerknows.ui.poi.CustomCategoryFragment;
import com.tigerknows.ui.poi.DishActivity;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.ui.poi.POIReportErrorActivity;
import com.tigerknows.ui.poi.POIDetailFragment;
import com.tigerknows.ui.poi.NearbySearchFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.poi.POIResultFragment;
import com.tigerknows.ui.traffic.BuslineDetailFragment;
import com.tigerknows.ui.traffic.BuslineResultLineFragment;
import com.tigerknows.ui.traffic.BuslineResultStationFragment;
import com.tigerknows.ui.traffic.FetchFavoriteFragment;
import com.tigerknows.ui.traffic.SubwayMapFragment;
import com.tigerknows.ui.traffic.TrafficCommonPlaceFragment;
import com.tigerknows.ui.traffic.TrafficCompassActivity;
import com.tigerknows.ui.traffic.TrafficDetailFragment;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.ui.traffic.TrafficReportErrorActivity;
import com.tigerknows.ui.traffic.TrafficResultFragment;
import com.tigerknows.ui.traffic.TrafficResultListMapFragment;
import com.tigerknows.ui.traffic.TrafficSearchHistoryFragment;
import com.tigerknows.ui.user.MyCommentListFragment;
import com.tigerknows.ui.user.UserBaseActivity;
import com.tigerknows.ui.user.UserHomeFragment;
import com.tigerknows.ui.user.UserLoginRegistActivity;
import com.tigerknows.ui.user.UserUpdateNickNameActivity;
import com.tigerknows.ui.user.UserUpdatePasswordActivity;
import com.tigerknows.ui.user.UserUpdatePhoneActivity;
import com.tigerknows.util.CalendarUtil;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.ZoomControls;

/**
 * 此类是应用程序的主类
 *
 * @author pengwenyue
 *
 */
public class Sphinx extends TKActivity implements TKAsyncTask.EventListener {
    
    public static final String ACTION_ONRESUME = "com.tigerknows.Sphinx.onResume";

	/**
	 * 是否来自微信
	 */
    public static final String EXTRA_WEIXIN = "extra_weixin";

    /**
     * 动态POI信息
     */
    public static final String EXTRA_PULL_MESSAGE = "extra_pull_message";

    /**
     * 请求快照的类型
     */
    public static final String EXTRA_SNAP_TYPE = "type";

    /**
     * 请求当前位置的快照
     */
    static final int SNAP_TYPE_MYLOCATION = 0;

    /**
     * 请求搜索结果POI的快照
     */
    static final int SNAP_TYPE_QUERY_POI = 3;

	public static final String REFRESH_POI_DETAIL = "refresh_poi_detail";

	private MapView mMapView;
    private View mMapOverView;
    private View mCenterTokenView;
	private CityInfo mInitCityInfo;
    private View mCompassView;
	private LinearLayout mZoomView;
    private View mLocationView=null;
    private ImageButton mLocationBtn=null;
    private TextView mLocationTxv=null;
    private ImageButton mMapCleanBtn=null;
    private View mMapToolsView;
    private ImageButton mMapToolsBtn;
    private ImageView mMapToolsImv;
    private PopupWindow mPopupWindowTools;

	private TouchMode touchMode=TouchMode.NORMAL;
	public enum TouchMode{
		NORMAL, CLICK_SELECT_POINT, LONG_CLICK_SELECT_POINT, MEASURE_DISTANCE;
	}

	// Handler message code
    public static int MAP_LONG_CLICKED = 0x01;
    public static int MAP_MOVEEND = 0x02;
	public static int MAP_ZOOMEND = 0x03;

    public static int DOWNLOAD_ERROR = 0x14;

    public static int SHOW_MAPVIEW = 0x15;
    
    public static int UI_STACK_ADJUST_READY = 0x1a;
    public static int UI_STACK_ADJUST_EXECUTE = 0x1b;
    public static int UI_STACK_ADJUST_CANCEL = 0x1c;
    
    public static int HOTEL_ORDER_DELETE_SYNC = 0x1e;
    public static int HOTEL_ORDER_OLD_SYNC = 0x1f;

    private static final String TAG = "Sphinx";
    private static final int REQUEST_CODE_LOCATION_SETTINGS = 15;

    private ViewGroup mTitleView;
    private ViewGroup mBodyView;
    private ViewGroup mBottomView;

    private BaseFragment mBodyFragment;
    private BaseFragment mBottomFragment;
    
    private int mUIStackAdjustReady = R.id.view_invalid;

    private static final int EXIT_APP_TIME = 2000;

    private long mLastBackKeyDown = -1;
    private boolean mResetLocatoinBtnWhenTouch = false;

    /**
     * 来自第三方的调用
     */
    private int mFromThirdParty = 0;

    public int getFromThirdParty() {
        return mFromThirdParty;
    }

    public static final int THIRD_PARTY_HTTP = 2;
    public static final int THIRD_PARTY_GEO = 3;
    public static final int THIRD_PARTY_SONY_MY_LOCATION = 4;
    public static final int THIRD_PARTY_SONY_QUERY_POI = 5;
    public static final int THIRD_PARTY_WENXIN_WEB = 6;
    public static final int THIRD_PARTY_WENXIN_REQUET = 7;
    public static final int THIRD_PARTY_PULL = 8;

    private Context mContext;

    private Bundle mBundle = null;

    public Bundle getBundle() {
    	return mBundle;
    }
    
    private long mOnCreateTimeMillis;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOnCreateTimeMillis = System.currentTimeMillis();
        BaseQuery.sClentStatus = BaseQuery.CLIENT_STATUS_START;
//        Debug.startMethodTracing("spinxTracing");

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (mMapView == null) {
                    return;
                }
                if (msg.what == MAP_MOVEEND) {

                    if(touchMode.equals(TouchMode.CLICK_SELECT_POINT)) {
                        InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
                        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
                        if (itemizedOverlay != null
                            && itemizedOverlay.size() == 1) {

                            ItemizedOverlayHelper.drawClickSelectPointOverlay(Sphinx.this, itemizedOverlay.get(0).getMessage());
                        }
                    }

                } else if (msg.what == MAP_LONG_CLICKED) {

                    Position position = (Position) msg.obj;

                    if(touchMode.equals(TouchMode.CLICK_SELECT_POINT)) {
                        return;
                    }

                    if(touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
                        return;
                    }

                    InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
                    if (infoWindowFragment == mBottomFragment) {
                        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
                        String overlayName = itemizedOverlay.getName();
                        if (ItemizedOverlay.BUSLINE_OVERLAY.equals(overlayName)||
                                ItemizedOverlay.TRAFFIC_PLAN_LIST_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_OVERLAY.equals(overlayName)) {
                            return;
                        }
                    }

                    mMapView.deleteOverlaysByName(ItemizedOverlay.LONG_CLICKED_OVERLAY);
                    mMapView.deleteOverlaysByName(ItemizedOverlay.MAP_POI_OVERLAY);

                    setTouchMode(TouchMode.LONG_CLICK_SELECT_POINT);

                    POI poi = getPOI(position, getString(R.string.select_point));
                    poi.setSourceType(POI.SOURCE_TYPE_LONG_CLICKED_SELECT_POINT);

                    //ItemizedOverlayHelper.drawPOIOverlay(ItemizedOverlay.LONG_CLICKED_OVERLAY, Sphinx.this, poi);
                    ItemizedOverlayHelper.drawFallingPOI(Sphinx.this, poi);//TODO 文越 review

                    mActionLog.addAction(ActionLog.MapLongClick);
                } else if (msg.what == MAP_ZOOMEND) {
                    mMapView.setZoomControlsState(msg.arg1);
                } else if (msg.what == DOWNLOAD_ERROR) {
                    int id = Sphinx.this.uiStackPeek();
                    if (id == R.id.view_traffic_home ||
                            id == R.id.view_result_map ||
                            id == R.id.view_measure_distance) {
                        Toast.makeText(Sphinx.this, R.string.network_failed, Toast.LENGTH_LONG).show();
                    }
                } else if (msg.what == SHOW_MAPVIEW) {
                    checkLocation(true);
                    mMapOverView.setBackgroundDrawable(null);
                } else if (msg.what == UI_STACK_ADJUST_READY){
                	mUIStackAdjustReady = msg.arg1;
                } else if (msg.what == UI_STACK_ADJUST_EXECUTE){
                	if(mUIStackAdjustReady != R.id.view_invalid){
                		uiStackAdjust(mUIStackAdjustReady);
                	}
                	mUIStackAdjustReady = R.id.view_invalid;
                } else if (msg.what == UI_STACK_ADJUST_CANCEL){
                	mUIStackAdjustReady = R.id.view_invalid;
                } else if (msg.what == HOTEL_ORDER_OLD_SYNC || msg.what == HOTEL_ORDER_DELETE_SYNC){
                	getHotelOrderDetailFragment().handleMessage(msg);
                }

            }
        };

        MoreHomeFragment.CurrentDownloadCity = null;

        mContext = getBaseContext();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        Label.init(Globals.g_metrics.widthPixels, Globals.g_metrics.heightPixels);

        mMapEngine = MapEngine.getInstance();

        CityInfo cityInfo = MapEngine.getCityInfo(CityInfo.CITY_ID_BEIJING);
        if (cityInfo.isAvailably() == false) {
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }

        try{
            setContentView(R.layout.sphinx);
            findViews();
            setListener();

            mMapView.setSphinx(this);

            if (mSensor != null) {
                mSensorOrientation = true;
            }

            Resources resources = getResources();
            Icon icon = Icon.getIcon(resources, mSensorOrientation ? R.drawable.icon_orientation1 : R.drawable.ic_bubble_my_location);
            Icon iconFocused = Icon.getIcon(resources, mSensorOrientation ? R.drawable.icon_orientation2 : R.drawable.ic_bubble_my_location2);
            Icon icFaceToNormal = Icon.getIcon(resources, R.drawable.ic_face_to_normal);
            Icon icFaceToFocused = Icon.getIcon(resources, R.drawable.ic_face_to_focused);
            RotationTilt rt=new RotationTilt(RotateReference.MAP,TiltReference.MAP);
            mMyLocation = new MyLocation(null, icon, iconFocused, icFaceToNormal, icFaceToFocused, null, rt);
            POI poi = new POI();
            poi.setName(getString(R.string.my_location));
            poi.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
            mMyLocation.setAssociatedObject(poi);
            mMyLocation.isFoucsed = true;

            mMyLocationOverlay=new ItemizedOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY);
            mMyLocationOverlay.addOverlayItem(mMyLocation);
            
            mMyLocationCircle = new Circle(cityInfo.getPosition(), new Length(0,UOM.M), Shape.MY_LOCATION);
            mMyLocationCircle.setVisible(false);
            mMapView.addShape(mMyLocationCircle);

            mMapView.addOverlay(mMyLocationOverlay);

            CityInfo lastCityInfo = Globals.getCurrentCityInfo(getApplicationContext());
            if(lastCityInfo != null && lastCityInfo.isAvailably()){
                cityInfo = lastCityInfo;
            }
            if (Globals.g_User != null) {
                mActionLog.addAction(ActionLog.LifecycleUserReadSuccess);
            }
            mActionLog.addAction(ActionLog.LifecycleSelectCity, cityInfo.getCName());
            mInitCityInfo = cityInfo;
            changeCity(cityInfo);

            Intent intent = getIntent();
            mFirstStartup = intent.getBooleanExtra(GuideScreenActivity.APP_FIRST_START, false);
            mUpgrade = intent.getBooleanExtra(GuideScreenActivity.APP_UPGRADE, false);
            checkFromThirdParty(true);
            initView();

            new Thread(new Runnable() {

                @Override
                public void run() {

                    // 初始化存储历史词的数据库表结构，读取历史词数据
                    readHistoryWord();

                    CalendarUtil.initExactTime(mContext);
                    Shangjia.readShangjiaList(Sphinx.this);
                    HotelVendor.readHotelVendorList(Sphinx.this);
                }
            }).start();

            mHandler.postDelayed(new StartUpDelayRunnable(this), 20000);

            mLocationListener = new MyLocationListener(this, mLocationChangedRun);

            ZoomControls zoomControls = mMapView.getZoomControls();
            zoomControls.setOnZoomInClickListener(new OnClickListener(){
                public void onClick(View view){
                    mActionLog.addAction(ActionLog.MapZoomIn);
                    mMapView.zoomIn();
                    resetShowInPreferZoom();
                }
            });
            zoomControls.setOnZoomOutClickListener(new OnClickListener(){
                public void onClick(View view){
                    mActionLog.addAction(ActionLog.MapZoomOut);
                    mMapView.zoomOut();
                    resetShowInPreferZoom();
                }
            });

            // add zoom controller to zoom view
            mZoomView.addView(zoomControls);

            mMapCleanBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(ActionLog.MapCleanMap);
                    clearMap();
                    uiStackClearTop(R.id.view_home);
                }
            });

            mLocationView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    mActionLog.addAction(ActionLog.MapLocation, String.valueOf(mMyLocation.mode));
                    requestLocation();
                }
            });

            // examples of adding Events through the API
            EventRegistry.addEventListener(mMapView, MapView.EventType.SURFACECREATED, new MapView.SurfaceCreatedEventListener(){
                @Override
                public void onSurfaceCreatedEvent() {
                	runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	if(mInitCityInfo != null) {
                        		initPosition(mInitCityInfo);
                        		mInitCityInfo = null;
                        	}
                        }
                    });
                }
            });
            EventRegistry.addEventListener(mMapView, MapView.EventType.DRAWFRAME, new MapView.DrawFrameEventListener() {

                @Override
                public void onDrawFrameEvent() {
                    mHandler.sendEmptyMessage(SHOW_MAPVIEW);
                }
            });
            EventRegistry.addEventListener(mMapView, MapView.EventType.UPDATEPOIPOSITION, new MapView.UpdatePoiPositionListener() {

				@Override
				public void onUpdatePoiPosition(EventSource eventSource, Position pos, String name) {
                    ItemizedOverlay overlay = mMapView.getOverlaysByName(ItemizedOverlay.MAP_POI_OVERLAY);
                    if (overlay != null) {
                    	OverlayItem item = overlay.getItemByFocused();
                    	try {
                    		item.setPosition(pos);
                    		mMapView.refreshMap();
                    	} catch (Exception e) {

                    	}
                    }
				}
			});


            EventRegistry.addEventListener(mMapView, MapView.EventType.CLICKPOI, new MapView.ClickPOIEventListener() {

                @Override
                public void onClickPOIEvent(EventSource eventSource, Position position, String name, Position touchPosition) {
                    if (mResetLocatoinBtnWhenTouch) {
                        return;
                    }

                    if(touchMode.equals(TouchMode.CLICK_SELECT_POINT)) {
                        return;
                    }

                    if(touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
                        getMeasureDistanceFragment().addPoint(touchPosition);
                        return;
                    }

                    mMapView.deleteOverlaysByName(ItemizedOverlay.LONG_CLICKED_OVERLAY);
                    mMapView.deleteOverlaysByName(ItemizedOverlay.MAP_POI_OVERLAY);

                    InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
                    if (infoWindowFragment == mBottomFragment) {
                        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
                        String overlayName = itemizedOverlay.getName();
                        if (ItemizedOverlay.BUSLINE_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_PLAN_LIST_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_OVERLAY.equals(overlayName)) {
                            return;
                        }
                    }
                    setTouchMode(TouchMode.NORMAL);

                    POI poi = new POI();
                    poi.setPosition(position);
                    poi.setName(name);
                    poi.setFrom(POI.FROM_LOCAL);
                    poi.setSourceType(POI.SOURCE_TYPE_MAP_POI);

                    //ItemizedOverlayHelper.drawPOIOverlay(ItemizedOverlay.MAP_POI_OVERLAY, Sphinx.this, poi);
                    ItemizedOverlayHelper.drawPopupPOI(Sphinx.this, poi); //TODO 文越 review
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.MOVEEND, new MapView.MoveEndEventListener(){
                @Override
                public void onMoveEndEvent(MapView mapView, Position position) {
                    mActionLog.addAction(ActionLog.MapMove);
                    Message msg = Message.obtain();
                    msg.what = MAP_MOVEEND;
                    msg.obj = position;
                    mHandler.sendMessage(msg);
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.DOWNLOAD, new MapView.DownloadEventListener(){
                @Override
                public void onDownloadEvent(final int state) {
                    if (state == DownloadEventListener.STATE_DOWNLOAD_ERROR) {
                        mHandler.sendEmptyMessage(DOWNLOAD_ERROR);
                    }
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.ZOOMEND, new MapView.ZoomEndEventListener(){
                @Override
                public void onZoomEndEvent(MapView mapView, final int newZoomLevel) {
                    Message message = Message.obtain();
                    message.what = MAP_ZOOMEND;
                    message.arg1 = newZoomLevel;
                    mHandler.sendMessage(message);
                }
            });

//            EventRegistry.addEventListener(mMapView, MapView.EventType.ROTATEEND, new MapView.RotateEndEventListener(){
//                @Override
//                public void onRotateEndEvent(MapView mapView, float rotation) {
//                    LogWrapper.i(TAG,"MapView.RotateEndEventListener rotation:"+rotation);
//                    if(rotation!=0 && mMapView.getCompass()!=null) {
//                        mMapView.refreshMap();
//                    }
//                }
//            });

//            EventRegistry.addEventListener(mMapView, MapView.EventType.TILTEND, new MapView.TiltEndEventListener(){
//                @Override
//                public void onTiltEndEvent(MapView mapView, float tilt) {
//                    LogWrapper.i(TAG,"MapView.TiltEndEventListener tilt:"+tilt);
//                    if(tilt!=0 && mMapView.getCompass()!=null) {
//                        mMapView.refreshMap();
//                    }
//                }
//            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.TOUCHPIN, new MapView.TouchEventListener(){
                @Override
                public void onTouchEvent(EventSource eventSource, Position position) {

                    if(touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
                        getMeasureDistanceFragment().addPoint(position);
                        return;
                    }
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.TOUCH, new MapView.TouchEventListener(){
                @Override
                public void onTouchEvent(EventSource eventSource, Position position) {

                    if(touchMode.equals(TouchMode.CLICK_SELECT_POINT)) {
                        return;
                    }

                    if(touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
                        getMeasureDistanceFragment().addPoint(position);
                        return;
                    }

                    InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
                    if (infoWindowFragment == mBottomFragment) {
                        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
                        String overlayName = itemizedOverlay.getName();
                        if (ItemizedOverlay.BUSLINE_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_PLAN_LIST_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_OVERLAY.equals(overlayName)) {
                            return;
                        }
                    }

                    if (infoWindowBackInHome(ItemizedOverlay.LONG_CLICKED_OVERLAY)
                            || infoWindowBackInHome(ItemizedOverlay.MAP_POI_OVERLAY)) {
                        return;
                    }


                    if (infoWindowBackInResultMap(ItemizedOverlay.LONG_CLICKED_OVERLAY)
                            || infoWindowBackInResultMap(ItemizedOverlay.MAP_POI_OVERLAY)) {
                        return;
                    }
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.TOUCHDOWN, new MapView.TouchEventListener(){
                @Override
                public void onTouchEvent(EventSource eventSource, Position position) {

                    if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                        mResetLocatoinBtnWhenTouch = true;
                    }
                    resetLoactionButtonState();

                    if (infoWindowBackInHome(ItemizedOverlay.MY_LOCATION_OVERLAY)) {
                        return;
                    }

                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.TOUCHUP, new MapView.TouchEventListener(){
                @Override
                public void onTouchEvent(EventSource eventSource, Position position) {
                    
                    mResetLocatoinBtnWhenTouch = false;

                    if(touchMode.equals(TouchMode.CLICK_SELECT_POINT)) {
                        return;
                    }

                    if(touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
                        return;
                    }

                    InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
                    if (infoWindowFragment == mBottomFragment) {
                        ItemizedOverlay itemizedOverlay = infoWindowFragment.getItemizedOverlay();
                        String overlayName = itemizedOverlay.getName();
                        if (ItemizedOverlay.BUSLINE_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_PLAN_LIST_OVERLAY.equals(overlayName) ||
                                ItemizedOverlay.TRAFFIC_OVERLAY.equals(overlayName)) {
                            return;
                        }
                    }
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.DOUBLECLICK, new MapView.DoubleClickEventListener(){
                @Override
                public void onDoubleClickEvent(final MapView mapView, final Position position) {
                    mActionLog.addAction(ActionLog.MapDoubleClick);
                	mMapView.zoomInAtPosition(position);
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.MULTITOUCHZOOM, new MapView.MultiTouchZoomEventListener(){
                @Override
                public void onMultiTouchZoomEvent(MapView mapView, final float newZoomLevel) {
                    resetShowInPreferZoom();
                    mActionLog.addAction(ActionLog.MapMultiTouchZoom);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMapView.setZoomControlsState(newZoomLevel);
                        }
                    });
                }
            });

            EventRegistry.addEventListener(mMapView, MapView.EventType.LONGCLICK, new MapView.LongClickEventListener(){
                @Override
                public void onLongClickEvent(MapView mapView, Position position) {
                    if (mResetLocatoinBtnWhenTouch) {
                        return;
                    }
                    Message message = Message.obtain();
                    message.what = MAP_LONG_CLICKED;
                    message.obj = position;
                    mHandler.sendMessage(message);
                }
            });

            Compass compass = mMapView.getCompass();
            if(compass!=null){
                compass.setVisible(false);
//                compass.addEventListener(EventType.TOUCH, new Compass.TouchEventListener() {
//                    @Override
//                    public void onTouchEvent(EventSource eventSource) {
//                        mMapView.refreshMap();
//                    }
//                });
            }

        }catch(Exception e){
            e.printStackTrace();
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }

        TKConfig.setPref(mContext, TKConfig.PREFS_VERSION_NAME, TKConfig.getClientSoftVersion());

        initWeibo(false, false);

        List<BaseQuery> list = new ArrayList<BaseQuery>();

        Bootstrap bootstrap = new Bootstrap(this);
        if (mIntent != null && mIntent.getBooleanExtra(Sphinx.EXTRA_WEIXIN, false)) {
            bootstrap.addParameter(BaseQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, BaseQuery.REQUSET_SOURCE_TYPE_WEIXIN);
        }
        if (mFirstStartup) {
            bootstrap.addParameter(Bootstrap.SERVER_PARAMETER_FIRST_LOGIN, Bootstrap.FIRST_LOGIN_NEW);
        } else if (mUpgrade) {
            bootstrap.addParameter(Bootstrap.SERVER_PARAMETER_FIRST_LOGIN, Bootstrap.FIRST_LOGIN_UPGRADE);
        }
        list.add(bootstrap);
        try {
            synchronized (Globals.StartupDisplayLogFile) {
                File startupDisplayLogFile = new File(Globals.StartupDisplayLogFile);
                if (startupDisplayLogFile.exists() && startupDisplayLogFile.isFile() && startupDisplayLogFile.length() > 0) {
                    String startupDisplayLog = Utility.readFile(new FileInputStream(startupDisplayLogFile));

                    if (startupDisplayLog != null && startupDisplayLog.length() > 0) {
                        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
                        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_DISPLAY, startupDisplayLog.substring(1));
                        list.add(feedbackUpload);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        queryStart(list);
	}

	private void resetShowInPreferZoom() {
        ItemizedOverlay overlay = mMapView.getCurrentOverlay();
        if (overlay != null) {
            overlay.isShowInPreferZoom = false;
        }
	}

    @Override
    //为了防止万一程序被销毁的风险，这个方法可以保证重要数据的正确性
    //不写这个方法并不意味着一定出错，但是一旦遇到了一些非常奇怪的数据问题的时候
    //可以看看是不是由于某些重要的数据没有保存，在程序被销毁时被重置
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.

      savedInstanceState.putIntegerArrayList("uistack", mUIStack);
      super.onSaveInstanceState(savedInstanceState);
      LogWrapper.d(TAG, "onSaveInstanceState");

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      ArrayList<Integer> uiStack = null;
      if (savedInstanceState != null) {
          uiStack = savedInstanceState.getIntegerArrayList("uistack");
      }
      LogWrapper.d(TAG, "onRestoreInstanceState() uiStack="+uiStack);
      if (uiStack != null) {
          initView();
      }

    }

    private void initView() {

        if (TKConfig.getPref(mThis, TKConfig.PREFS_MAP_TOOLS) == null) {
            mMapToolsImv.setVisibility(View.VISIBLE);
        } else {
            mMapToolsImv.setVisibility(View.GONE);
        }

        mUIStack.clear();
        getTitleFragment();
        getMoreFragment().refreshMoreData();
        showView(R.id.view_home);

        Sphinx.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        checkFromThirdParty(false);
    }

    @Override
	protected Dialog onCreateDialog(int id) {
	    return getDialog(id);
	}

	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {
	    super.onPrepareDialog(id, dialog);
	    if (id == R.id.dialog_prompt_setting_location) {
            mActionLog.addAction(ActionLog.Dialog, getString(R.string.location_failed_and_jump_settings));
        }
    }

    /* Creates the menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//	    menu.add(0, CONFIG_SERVER, 8, "config");
//	    menu.add(0, PROFILE, 11, "profile");
	    return true;
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return false;
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		LogWrapper.d(TAG, "onActivityResult() requestCode="+requestCode+" resultCode="+resultCode+ "data="+data);
		mActivityResult = true;
		mOnActivityResultLoginBack = false;
		if (R.id.activity_more_app_recommend == requestCode) {
        } else if (R.id.activity_more_change_city == requestCode) {
            if (data != null && RESULT_OK == resultCode) {
                CityInfo cityInfo = data.getParcelableExtra(ChangeCityActivity.EXTRA_CITYINFO);
                boolean changeHotelCity = data.getBooleanExtra(ChangeCityActivity.EXTRA_ONLY_CHANGE_HOTEL_CITY, false);
                if (changeHotelCity) {
                    getHotelHomeFragment().setCityInfo(cityInfo);
                } else {
                    changeCity(cityInfo);
                }
            }
        } else if (R.id.activity_setting_location == requestCode) {
        } else if (R.id.activity_more_setting == requestCode) {
            boolean request = TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_ACQUIRE_WAKELOCK));
            if (request) {
                if (mWakeLock.isHeld() == false) {
                    mWakeLock.acquire();
                }
            } else {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        } else if (R.id.activity_more_map_download == requestCode) {
            if (data != null && RESULT_OK == resultCode) {
                CityInfo cityInfo = data.getParcelableExtra(MapDownloadActivity.EXTRA_CITYINFO);
                if (cityInfo != null) {
                    changeCity(cityInfo);
                }
            }
        } else if (R.id.activity_user_login_regist == requestCode) {
            if (data != null) {
                loginBack(data);
                mOnActivityResultLoginBack = true;
            }
		} else if (R.id.activity_poi_edit_comment == requestCode) {
			if (resultCode == RESULT_CANCELED
					&& data != null
					&& data.getBooleanExtra(REFRESH_POI_DETAIL, false)) {
				getPOIDetailFragment().refreshDetail();
				getPOIDetailFragment().refreshComment();
			}
        } else if (R.id.activity_hint == requestCode) {

        } else if (R.id.activity_poi_dish == requestCode) {
            getPOIDetailFragment().refreshRecommendCook();
        }

        if (REQUEST_CODE_LOCATION_SETTINGS == requestCode) {
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (isFinishing()) {
                        return;
                    }
                    if (Globals.g_My_Location_City_Info == null) {
                        Toast.makeText(Sphinx.this, getString(R.string.location_failed), Toast.LENGTH_LONG).show();
                    }
                }
            }, 30 *1000);
        }

	}

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
        mActivityResult = true;
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        mActivityResult = true;
    }

	@Override
	protected void onResume() {
		super.onResume();
		mOnPause = false;
        sendBroadcast(new Intent(ACTION_ONRESUME));
		LogWrapper.i(TAG,"onResume()");
        refreshZoomView();
		mActionLog.onResume();
		mMapView.resume();
		if (mMapView.isStopRefreshMyLocation() == false) {
		    mMapView.refreshMap();
		}

		boolean ensureThreadsRunning = mMapView.ensureThreadRunning();
		// TODO mTrafficQueryFragment实例的成员变量在地图的背景线程被重新生成后会造成丢失的问题
		if (ensureThreadsRunning) {
		    mTrafficQueryFragment = null;
		}

        if (mSensorOrientation) {
            mSensorManager.registerListener(mSensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        Globals.setConnectionFast(Utility.isConnectionFast(this));

        TKConfig.updateIMSI(mConnectivityManager);

        IntentFilter intentFilter= new IntentFilter(MapStatsService.ACTION_STATS_CURRENT_DOWNLOAD_CITY_COMPLATE);
        registerReceiver(mCountCurrentDownloadCityBroadcastReceiver, intentFilter);

        int id = uiStackPeek();
        BaseFragment baseFragment = getFragment(id);
        if (baseFragment != null) {
            baseFragment.onResume();
        }

        if (mActivityResult) {
            mActivityResult = false;
            return;
        }

        if (mFirstStartup) {
            mFirstStartup = false;
            OnSetup();
        }
        LogWrapper.e("Fake","onResume heap size:"+android.os.Debug.getNativeHeapAllocatedSize());
	}

	private void checkLocation(boolean tipGps) {
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;

        if (myLocationCityInfo != null && myLocationCityInfo.isAvailably()) {
            mMapView.centerOnPosition(myLocationCityInfo.getPosition(), TKConfig.ZOOM_LEVEL_LOCATION);
        } else if (tipGps) {
            boolean gps = Utility.checkGpsStatus(mContext);
            if (!gps) {
                showPromptSettingLocationDialog();
            }
        }
	}

	@Override
	protected void onStart() {
		super.onStart();
		BaseQuery.sClentStatus = BaseQuery.CLIENT_STATUS_START;

        IntentFilter intentFilter= new IntentFilter(MapEngine.ACTION_REMOVE_CITY_MAP_DATA);
        registerReceiver(mRemoveCityMapDataBroadcastReceiver, intentFilter);
	}

	@Override
	protected void onDestroy() {
        ImageCache.getInstance().stopWritingAndRemoveOldTiles();
        AsyncImageLoader.getInstance().onDestory();
        mTKLocationManager.onDestroy();
        TKWeixin.getInstance(this).onDestory();
        Globals.onDestory();
        synchronized (this) {
            HistoryWordTable.onDestroy();
        }
		LogWrapper.i(TAG,"onDestroy()");
        if(DiscoverChildListFragment.viewQueueForChangciItems!=null){
        	DiscoverChildListFragment.viewQueueForChangciItems.clear();
        }
        if(DiscoverChildListFragment.viewQueueForChangciRows != null){
        	DiscoverChildListFragment.viewQueueForChangciRows.clear();
        }
        mActionLog.onDestroy();
        Intent service = new Intent(Sphinx.this, MapStatsService.class);
        stopService(service);
        service = new Intent(Sphinx.this, MapDownloadService.class);
        stopService(service);
        super.onDestroy();
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        ActivityManager am = (ActivityManager)getSystemService(
//                Context.ACTIVITY_SERVICE);
//        am.restartPackage("com.tigerknows");
//        Process.killProcess(Process.myPid());
//        Debug.stopMethodTracing();
	}
	/**
	 * store the last known position and zoom level then close the database.
	 */
	@Override
	public void finish() {
		super.finish();
	}

	public TouchMode getTouchMode() {
		return touchMode;
	}

	public void setTouchMode(TouchMode touchMode) {
		this.touchMode = touchMode;
	}

	public MapView getMapView() {
		return mMapView;
	}

    //接受地图统计完成信息。
    private BroadcastReceiver mCountCurrentDownloadCityBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            MoreHomeFragment moreHomeFragment = getMoreFragment();
            moreHomeFragment.refreshMapDownloadData();
        }
    };

    private BroadcastReceiver mRemoveCityMapDataBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null
                    && intent.hasExtra(MapEngine.EXTRA_CITY_ID)) {
                int cityId = intent.getIntExtra(MapEngine.EXTRA_CITY_ID, CityInfo.CITY_ID_INVALID);
                if (Globals.getCurrentCityInfo(mThis).getId()==cityId || mViewedCityInfoList.contains(cityId)) {
                    if (mMapView != null) {
                        mMapView.clearTileTextures();
                    }
                }
            }
        }
    };

    public MapEngine getMapEngine() {
        return mMapEngine;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (mMapView != null) {
            mMapView.moveView((int)event.getX()*Globals.g_metrics.widthPixels, (int)event.getY()*Globals.g_metrics.heightPixels);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mMapView == null) {
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_MENU:
                LogWrapper.e("Fake","heap size:"+android.os.Debug.getNativeHeapAllocatedSize());
                BaseQueryTest.showSetResponseCode(mLayoutInflater, this);
                return true;
            case KeyEvent.KEYCODE_BACK:

                mActionLog.addAction(ActionLog.KeyCodeBack);
                BaseFragment baseFragment = getFragment(uiStackPeek());
                if (baseFragment != null && baseFragment.onKeyDown(keyCode, event)) {
                    return true;
                }

                if(!touchMode.equals(TouchMode.CLICK_SELECT_POINT)
                        && !touchMode.equals(TouchMode.MEASURE_DISTANCE)) {

                    if (infoWindowBackInHome(ItemizedOverlay.MY_LOCATION_OVERLAY)
                            || infoWindowBackInHome(ItemizedOverlay.LONG_CLICKED_OVERLAY)
                            || infoWindowBackInHome(ItemizedOverlay.MAP_POI_OVERLAY)) {

                        resetLoactionButtonState();
                        return true;
                    }
                }

                if (!uiStackBack()) {
                    long time = System.currentTimeMillis();
                    if (mLastBackKeyDown != -1 && time - mLastBackKeyDown < EXIT_APP_TIME){
                        if(checkUploadApp()){
                            mHandler.post(new Runnable(){
                                @Override
                                public void run() {
                                    submitUploadApp();
                                }
                            });
                        }
                        baseFragment = getFragment(uiStackPeek());
                        if (baseFragment != null) {
                            baseFragment.dismiss();
                        }
                        Sphinx.this.finish();
                    } else {
                        mLastBackKeyDown = time;
                        Toast.makeText(mThis, R.string.exit_app, Toast.LENGTH_SHORT).show();
                    }

                    return true;
                }
                return true;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void findViews() {
        mTitleView = (ViewGroup)findViewById(R.id.title_view);
        mBodyView = (ViewGroup)findViewById(R.id.body_view);
        mBottomView = (ViewGroup)findViewById(R.id.bottom_view);
        mZoomView = (LinearLayout) findViewById(R.id.zoomview);
        mMapView = (MapView) findViewById(R.id.mapview);
        mMapOverView = findViewById(R.id.map_over_view);
        mCenterTokenView = findViewById(R.id.center_token_view);
        mMapCleanBtn=(ImageButton)(findViewById(R.id.map_clean_btn));
        mLocationView=(findViewById(R.id.location_view));
        mLocationBtn=(ImageButton)(findViewById(R.id.location_btn));
        mLocationTxv=(TextView)(findViewById(R.id.location_txv));
        mCompassView = findViewById(R.id.compass_imv);

        mMapToolsView = findViewById(R.id.map_tools_view);
        mMapToolsBtn = (ImageButton)findViewById(R.id.map_tools_btn);
        mMapToolsImv = (ImageView)findViewById(R.id.map_tools_imv);
    }

    private void setListener() {

    	mMapToolsBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mActionLog.addAction(ActionLog.MapMore);
                TKConfig.setPref(mThis, TKConfig.PREFS_MAP_TOOLS, "1");
                mMapToolsImv.setVisibility(View.GONE);

                if (mPopupWindowTools == null) {
                    View view = mLayoutInflater.inflate(R.layout.alert_map_tools, null, false);
                    mPopupWindowTools = new PopupWindow(view);
                    mPopupWindowTools.setWindowLayoutMode(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
                    mPopupWindowTools.setFocusable(true);
                    // 设置允许在外点击消失
                    mPopupWindowTools.setOutsideTouchable(true);

                    // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
                    mPopupWindowTools.setBackgroundDrawable(new BitmapDrawable());
                    mPopupWindowTools.setAnimationStyle(-1);
                    mPopupWindowTools.update();

                    View button0 = view.findViewById(R.id.button0_view);
                    View button1 = view.findViewById(R.id.button1_view);
                    View button2 = view.findViewById(R.id.button2_view);
                    View button3 = view.findViewById(R.id.button3_view);
                    View showZoomView = view.findViewById(R.id.show_zoom_button_view);
                    final CheckBox showZoonChb = (CheckBox) view.findViewById(R.id.show_zoom_button_chb);

                    View.OnClickListener onClickListener = new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            int id = v.getId();
                            if (uiStackPeek() == R.id.view_traffic_home) {
                                getTrafficQueryFragment().priorityMyLocation = false;
                            }
                            if (id == R.id.button0_view) {

                                mActionLog.addAction(ActionLog.MapSubway);
                                getSubwayMapFragment().setData(Globals.getCurrentCityInfo(mThis, false));
                                showView(R.id.view_subway_map);
                                mPopupWindowTools.dismiss();
                            } else if (id == R.id.button1_view) {
                                mActionLog.addAction(ActionLog.MapTakeScreenshot);

                                snapMapView(new SnapMap() {

                                    @Override
                                    public void finish(Uri uri) {
                                        if (uri == null) {
                                            return;
                                        }
                                        Intent intent = new Intent();
                                        intent.setData(uri);
                                        showView(R.id.activity_take_screenshot, intent);
                                    }
                                }, mMapView.getCenterPosition(), mMapView.getCurrentMapScene());

                                mPopupWindowTools.dismiss();

                            } else if (id == R.id.button2_view) {
                                mActionLog.addAction(ActionLog.MapDistance);

                                getMeasureDistanceFragment().setData();
                                showView(R.id.view_measure_distance);

                                Toast.makeText(mThis, R.string.measure_distance_tip, Toast.LENGTH_LONG).show();
                                mPopupWindowTools.dismiss();
                            } else if (id == R.id.button3_view) {
                                mActionLog.addAction(ActionLog.MapCompass);

                                showView(R.id.activity_traffic_compass);
                                mPopupWindowTools.dismiss();
                            } else if (id == R.id.show_zoom_button_view || id == R.id.show_zoom_button_chb) {
                                mActionLog.addAction(ActionLog.MapShowZoomBtn, String.valueOf(showZoonChb.isChecked()));
                                TKConfig.reversePref(mThis, TKConfig.PREFS_SHOW_ZOOM_BUTTON);
                                showZoonChb.setChecked(!TKConfig.isPref(mThis, TKConfig.PREFS_SHOW_ZOOM_BUTTON));
                                refreshZoomView();
                            }
                        }
                    };
                    button0.setOnClickListener(onClickListener);
                    button1.setOnClickListener(onClickListener);
                    button2.setOnClickListener(onClickListener);
                    button3.setOnClickListener(onClickListener);
                    showZoomView.setOnClickListener(onClickListener);
                    showZoonChb.setOnClickListener(onClickListener);

                    view.findViewById(R.id.bottomPanel).setOnTouchListener(new OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            mPopupWindowTools.dismiss();
                            return false;
                        }
                    });
                }
                CheckBox showZoonChb = (CheckBox) mPopupWindowTools.getContentView().findViewById(R.id.show_zoom_button_chb);
                showZoonChb.setChecked(!TKConfig.isPref(mThis, TKConfig.PREFS_SHOW_ZOOM_BUTTON));

                mPopupWindowTools.showAsDropDown(mMapToolsBtn, 0, 0);

            }
        });
    }

    boolean checkFromThirdParty(boolean onlyCheck) {
        boolean result = false;
        Intent intent = getIntent();
        if (intent == null) {
            return result;
        }
        Uri uri = intent.getData();
        String scheme = intent.getScheme();
        // 来自索爱的调用，为其提供地图快照作为返回
        int type = intent.getIntExtra(EXTRA_SNAP_TYPE, -1);

        // 拦截通过URL查看指定经纬度的位置信息的Intent，在地图显示位置信息
        int fromThirdParty = 0;
        if ("http".equals(scheme)) {
            mFromThirdParty = THIRD_PARTY_HTTP;
            fromThirdParty = THIRD_PARTY_HTTP;
            if (onlyCheck) {
                return true;
            }
            uiStackClose(new int[]{R.id.view_home});
            showView(R.id.view_home);
            try {
                String uriStr = uri.toString();
                String[] parms = uriStr.substring(uriStr.indexOf("?")+1).split("&");
                String[] parm = parms[0].split("=");
                POI poi = null;
                if ("latlon".equals(parm[0])) {
                    Position position = new Position(parm[1]);
                    mMapView.setZoomLevel(TKConfig.ZOOM_LEVEL_LOCATION);
                    mMapView.panToPosition(position);
                    poi = getPOI(position, getString(R.string.select_point));
                }
                boolean query = false;
                try {
                    parm = parms[1].split("=");
                    if ("z".equals(parm[0])) {
                        int zoomLevel = Integer.parseInt(parm[1]);
                        mMapView.zoomTo(zoomLevel, new MapView.ZoomEndEventListener() {
                            @Override
                            public void onZoomEndEvent(MapView mapView, final int newZoomLevel) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                    	mMapView.setZoomControlsState(newZoomLevel);
                                    }
                                });
                            }
                        });
                    }
                    parm = parms[2].split("=");
                    if ("n".equals(parm[0])) {
                        poi.setName(parm[1]);
                    } else if ("q".equals(parm[0])) {
                        String keyword = parm[1];
                        if (!TextUtils.isEmpty(keyword)) {
                            getInputSearchFragment().setData(buildDataQuery(),
                            		keyword,
                                    InputSearchFragment.MODE_POI);
                            showView(R.id.view_poi_input_search);
                            getInputSearchFragment().submitPOIQuery(keyword);
                            query = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (query == false && Util.inChina(poi.getPosition())) {
                    getResultMapFragment().setData(getString(R.string.result_map), ActionLog.POIDetailMap);
                    showView(R.id.view_result_map);
                    List<POI> list = new ArrayList<POI>();
                    list.add(poi);
                    ItemizedOverlayHelper.drawPOIOverlay(this, list, 0);
                } else {

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("geo".equals(scheme)) {   // 拦截通过URL查看指定经纬度的位置信息的Intent，在地图显示位置信息
            mFromThirdParty = THIRD_PARTY_GEO;
            fromThirdParty = THIRD_PARTY_GEO;
            if (onlyCheck) {
                return true;
            }
            uiStackClose(new int[]{R.id.view_home});
            showView(R.id.view_home);
            // geo:latitude,longitude
            // geo:latitude,longitude?z=zoom，z表示zoom级别，值为数字1到23
            // geo:0,0?q=my+street+address
            // geo:0,0?q=business+near+city
            String uriStr = uri.toString();
            POI poi = new POI();
            try {
                uriStr = uriStr.substring(uriStr.indexOf(":")+1);
                double lat = Double.parseDouble(uriStr.substring(0, uriStr.indexOf(",")));
                uriStr = uriStr.substring(uriStr.indexOf(",")+1);
                double lon;
                boolean query = false;
                if (uriStr.indexOf("?") > -1) {
                    lon = Double.parseDouble(uriStr.substring(0, uriStr.indexOf("?")));
                    Position position = new Position(lat, lon);
                    poi = getPOI(position, getString(R.string.select_point));
                    mMapView.centerOnPosition(position, TKConfig.ZOOM_LEVEL_LOCATION);
                    uriStr = uriStr.substring(uriStr.indexOf("?")+1);
                    String[] parm = uriStr.split("=");
                    if ("z".equals(parm[0])) {
                        mMapView.setZoomLevel(Integer.parseInt(parm[1]));
                    } else if ("q".equals(parm[0])) {
                        String keyword = parm[1];
                        if (!TextUtils.isEmpty(keyword)) {
                            getInputSearchFragment().setData(buildDataQuery(),
                            		keyword,
                                    InputSearchFragment.MODE_POI);
                            showView(R.id.view_poi_input_search);
                            getInputSearchFragment().submitPOIQuery(keyword);
                        }
                        query = true;
                    }
                } else {
                    lon = Double.parseDouble(uriStr);
                    Position position = new Position(lat, lon);
                    poi = getPOI(position, getString(R.string.select_point));
                    mMapView.centerOnPosition(position, TKConfig.ZOOM_LEVEL_LOCATION);
                }
                if (query == false && Util.inChina(poi.getPosition())) {
                    String name = mMapEngine.getPositionName(poi.getPosition());
                    if (!TextUtils.isEmpty(name)) {
                        poi.setName(name);
                    } else {
                        poi.setName(getString(R.string.select_point));
                    }
                    getResultMapFragment().setData(getString(R.string.result_map), ActionLog.POIDetailMap);
                    showView(R.id.view_result_map);
                    List<POI> list = new ArrayList<POI>();
                    list.add(poi);
                    ItemizedOverlayHelper.drawPOIOverlay(this, list, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (type == SNAP_TYPE_MYLOCATION) {
            mFromThirdParty = THIRD_PARTY_SONY_MY_LOCATION;
            fromThirdParty = THIRD_PARTY_SONY_MY_LOCATION;
            if (onlyCheck) {
                return true;
            }
            uiStackClose(new int[]{R.id.view_home});
            showView(R.id.view_home);

            POI poi = null;
            if (mMyLocation.getPosition() != null) {
                poi = ((POI) mMyLocation.getAssociatedObject()).clone();
                poi.setName(getString(R.string.my_location)+":"+mMyLocation.getMessage());
                poi.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
            }
            if (poi != null && poi.getPosition() != null) {
                getResultMapFragment().setData(getString(R.string.result_map), ActionLog.POIDetailMap);
                showView(R.id.view_result_map);
                List<POI> list = new ArrayList<POI>();
                list.add(poi);
                ItemizedOverlayHelper.drawPOIOverlay(this, list, 0);
            } else {
                requestLocation();
            }
        } else if (type == SNAP_TYPE_QUERY_POI) {
            mFromThirdParty = THIRD_PARTY_SONY_QUERY_POI;
            fromThirdParty = THIRD_PARTY_SONY_QUERY_POI;
            if (onlyCheck) {
                return true;
            }
            uiStackClose(new int[]{R.id.view_home});
            getInputSearchFragment().setData();
            showView(R.id.view_poi_input_search);
        } else if (intent.getBooleanExtra(EXTRA_WEIXIN, false)) { //   来自微信的调用，为其提供POI数据作为返回
        	mFromThirdParty = THIRD_PARTY_WENXIN_REQUET;
            fromThirdParty = THIRD_PARTY_WENXIN_REQUET;
            if (onlyCheck) {
                return true;
            }
            mActionLog.addAction(ActionLog.LifecycleWeixinRequest);
        	mBundle = intent.getExtras();
            uiStackClose(new int[]{R.id.view_home});
            showView(R.id.view_home);
        } else if (checkFromWeixin(intent, onlyCheck)) {
            fromThirdParty = THIRD_PARTY_WENXIN_WEB;
        } else if (checkFromPullMessage(intent, onlyCheck)) {
            fromThirdParty = THIRD_PARTY_PULL;
        }

        result = fromThirdParty > 0;
        if (result && onlyCheck == false) {
            setIntent(null);
        }

        return result;
    }

    /**
     * 来自微信的调用，在其中浏览POI的WAP页时点击打开按钮后进入到本应用，显示其POI的详情信息
     * @param intent
     */
    boolean checkFromWeixin(Intent intent, boolean onlyCheck) {
        boolean result = false;
        if (intent == null) {
            return result;
        }
        Uri uri = intent.getData();
        String scheme = intent.getScheme();
        if ("tigerknows".equals(scheme)) {
            mFromThirdParty = THIRD_PARTY_WENXIN_WEB;
            if (onlyCheck) {
                return true;
            }
            mActionLog.addAction(ActionLog.LifecycleWeixinWeb);
            uiStackClose(new int[]{R.id.view_home});
            showView(R.id.view_home);
            if (uri != null) {
                String uriStr = uri.toString();
                String[] parms = uriStr.substring(uriStr.indexOf("?")+1).split("&");
                if (parms.length >= 2) {
                    String[] keyValue = parms[0].split("=");
                    int cityId = CityInfo.CITY_ID_INVALID;
                    if (keyValue.length == 2) {
                        if (keyValue[0].equals("c")) {
                            cityId = Integer.parseInt(keyValue[1]);

                            keyValue = parms[1].split("=");
                            if (keyValue.length == 2) {
                                if (keyValue[0].equals("uid")) {
                                    String poiuid = keyValue[1];
                                    if (poiuid != null) {
                                        POI poi = new POI();
                                        poi.setUUID(poiuid);
                                        poi.ciytId =  cityId;
                                        getPOIDetailFragment().setData(poi);
                                        showView(R.id.view_poi_detail);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private Runnable mOnNewIntentStamp = new Runnable() {

        @Override
        public void run() {
            getPOIDetailFragment().refreshStamp();
            getPOIResultFragment().refreshStamp();
            getMyCommentListFragment().refreshComment();
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (checkFromThirdParty(false)) {
            return;
        }

        if (intent != null) {
            int sourceViewId = intent.getIntExtra(BaseActivity.SOURCE_VIEW_ID, R.id.view_invalid);
            if (sourceViewId == R.id.activity_poi_edit_comment) {
                mHandler.post(mOnNewIntentStamp);

            // 登录之后的返回
            } else {
                loginBack(intent);
            }
        }
    }

    boolean checkFromPullMessage(Intent newIntent, boolean onlyCheck) {
        boolean result = false;
        if (newIntent != null) {
            com.tigerknows.model.PullMessage.Message message = newIntent.getParcelableExtra(Sphinx.EXTRA_PULL_MESSAGE);
            if (message != null) {
                mFromThirdParty = THIRD_PARTY_PULL;
                if (onlyCheck) {
                    return true;
                }
            	TKNotificationManager.cancel(mThis);
            	PulledProductMessage productMessage = message.getProductMsg();
                if (message.getDynamicPOI() != null) {
                    uiStackClose(new int[]{R.id.view_home});
                    showView(R.id.view_home);
                    //Show the corresponding view
                    showPulledDynamicPOI(message.getDynamicPOI());
                } else if (message.getType() == PullMessage.Message.TYPE_ACTIVITY &&
                        productMessage != null) {
                    uiStackClose(new int[]{R.id.view_more_home});
                    showView(R.id.view_more_home);
                    Intent intent = new Intent();
                    intent.putExtra(BrowserActivity.TITLE, productMessage.getTitle());
                    intent.putExtra(BrowserActivity.URL, productMessage.getDownloadUrl());
                    showView(R.id.activity_browser, intent);
                } else {
                    uiStackClose(new int[]{R.id.view_home});
                    showView(R.id.view_home);
                }

                mActionLog.addAction(ActionLog.RadarClick, message.getType(), message.getDynamicPOI()==null?"none":(""+message.getDynamicPOI().getMasterType()));
                result = true;
            }
        }
        return result;
    }

    /**
     * Show the dynamic poi using the info in {@link PulledDynamicPOI}
     * @param pulledDynamicPOI
     */
    void showPulledDynamicPOI(PulledDynamicPOI pulledDynamicPOI){
    	String masterType = ""+pulledDynamicPOI.getMasterType();

    	if( BaseQuery.DATA_TYPE_YANCHU.equals(masterType)){

    		// show yanchu dynamic poi
            showView(R.id.view_discover_yanchu_detail);
            getYanchuDetailFragment().setPulledDynamicPOI(pulledDynamicPOI);

    	}else if(BaseQuery.DATA_TYPE_ZHANLAN.equals(masterType)){

    		// show zhanlan dynamic poi
            showView(R.id.view_discover_zhanlan_detail);
            getZhanlanDetailFragment().setPulledDynamicPOI(pulledDynamicPOI);

    	}else if(BaseQuery.DATA_TYPE_DIANYING.equals(masterType)){

    		// show dianying dynamic poi
            showView(R.id.view_discover_dianying_detail);
            getDianyingDetailFragment().setPulledDynamicPOI(pulledDynamicPOI);

    	}

    }

    boolean mOnActivityResultLoginBack = false;
    private void loginBack(Intent intent) {
        if (mOnActivityResultLoginBack) {
            mOnActivityResultLoginBack = false;
            return;
        }
        boolean sourceUserHome = intent.getBooleanExtra(BaseActivity.SOURCE_USER_HOME, false);
        if (sourceUserHome) {
            if (Globals.g_User != null) {
                uiStackClearTop(R.id.view_user_home);
            } else {
                uiStackClearTop(R.id.view_more_home);
            }
            return;
        }
        int loginSuccessToViewId = R.id.view_invalid;
        int loginFailedToViewId = R.id.view_invalid;
        loginSuccessToViewId = intent.getIntExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_SUCCESS, R.id.view_invalid);
        loginFailedToViewId = intent.getIntExtra(UserBaseActivity.TARGET_VIEW_ID_LOGIN_FAILED, R.id.view_invalid);
        if (loginSuccessToViewId != R.id.view_invalid
                && loginFailedToViewId != R.id.view_invalid) {
            User user = Globals.g_User;
            if (user != null) {
                uiStackClearTop(loginSuccessToViewId);
                showView(loginSuccessToViewId);
            } else {
                uiStackClearTop(loginFailedToViewId);
                showView(loginFailedToViewId);
            }
        }
    }

    @Override
    protected void onPause() {
        LogWrapper.i(TAG, "onPause");
        mActionLog.onPause();
        mOnPause = true;

        if (mPopupWindowTools != null) {
            mPopupWindowTools.dismiss();
        }

        LogUpload.upload(mThis, mActionLog, mTKLocationManager.getGPSLocationUpload(), mTKLocationManager.getNetworkLocationUpload());

        if (mSensorOrientation) {
            mSensorManager.unregisterListener(mSensorListener);
        }

        int id = uiStackPeek();
        BaseFragment baseFragment = getFragment(id);
        if (baseFragment != null) {
            baseFragment.onPause();
        }

        CityInfo cityInfo = Globals.getCurrentCityInfo(mThis);
        if (cityInfo != null && cityInfo.isAvailably()){
            Position position = cityInfo.getPosition();
            if (position != null) {
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LON, String.valueOf(position.getLon()));
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LAT, String.valueOf(position.getLat()));
                int zoom = (int)mMapView.getZoomLevel();
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(zoom));
            }
        }

        unregisterReceiver(mCountCurrentDownloadCityBroadcastReceiver);
        System.gc();
        Intent service = new Intent(Sphinx.this, SuggestLexiconService.class);
        stopService(service);

        super.onPause();
    }

    @Override
    protected void onStop() {
        LauncherActivity.LastActivityClassName = null;
        BaseQuery.sClentStatus = BaseQuery.CLIENT_STATUS_STOP;
        LogWrapper.i(TAG, "onStop");

        if(mMapView != null)
        	mMapView.pause();
        MapEngine.cleanEngineCache();
        System.gc();

        mActionLog.onStop();
        unregisterReceiver(mRemoveCityMapDataBroadcastReceiver);
        super.onStop();
    }

    public void snapMapView(SnapMap snapMap, Position position, MapScene mapScene) {
        mMapView.snapMapView(this, snapMap, position, mapScene);
    }

    private void initPosition(CityInfo cityInfo) {
    	if(cityInfo == null)
    		return;
    	if (cityInfo.isAvailably()) {
	        mMapView.centerOnPosition(cityInfo.getPosition(), cityInfo.getLevel(), true);
    	}
    }

    public void changeCity(CityInfo cityInfo) {
        if (cityInfo.isAvailably()) {

            CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
            if (myLocationCityInfo != null &&
                    myLocationCityInfo.getId() == cityInfo.getId()) {
                mMapView.centerOnPosition(myLocationCityInfo.getPosition(), TKConfig.ZOOM_LEVEL_LOCATION,true);
            } else {
                mMapView.centerOnPosition(cityInfo.getPosition(), cityInfo.getLevel(), true);
            }
            updateCityInfo(cityInfo);
            if (!mViewedCityInfoList.contains(cityInfo)) {
                mViewedCityInfoList.add(cityInfo);
            }
            mActionLog.addAction(ActionLog.LifecycleSelectCity, cityInfo.getCName());
            Position position = cityInfo.getPosition();
            TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LON, String.valueOf(position.getLon()));
            TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LAT, String.valueOf(position.getLat()));
            TKConfig.setPref(mContext, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(cityInfo.getLevel()));

            Intent service = new Intent(MapStatsService.ACTION_STATS_CURRENT_DOWNLOAD_CITY);
            service.setClass(mThis, MapStatsService.class);
            startService(service);
        }
    }

    private void readHistoryWord() {
        synchronized (this) {
            HistoryWordTable.readHistoryWord(this, HistoryWordTable.TYPE_POI);
            HistoryWordTable.readHistoryWord(this, HistoryWordTable.TYPE_TRAFFIC);
            HistoryWordTable.readHistoryWord(this, HistoryWordTable.TYPE_BUSLINE);
        }
    }

    public void showTip(int resId, int duration) {
        showTip(getString(resId), duration);
    }

    public void showTip(String tip, int duration) {
        Toast.makeText(Sphinx.this, tip, duration).show();
    }

    public Handler getHandler() {
        return mHandler;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        BaseFragment baseFragment = getFragment(uiStackPeek());
        if (baseFragment != null && baseFragment.onMenuItemSelected(featureId, item)) {
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        int targetViewId = baseQuery.getTargetViewId();

        BaseFragment baseFragment = getFragment(targetViewId);
        if (baseFragment != null) {
            baseFragment.onCancelled(tkAsyncTask);
        }
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        int targetViewId = baseQuery.getTargetViewId();
        BaseFragment baseFragment = getFragment(targetViewId);
        if (baseFragment != null) {
            baseFragment.onPostExecute(tkAsyncTask);
        }

        List<BaseQuery> list = tkAsyncTask.getBaseQueryList();
        for(int i = list.size()-1; i >= 0; i--) {
            baseQuery = list.get(i);
            if (baseQuery instanceof Bootstrap) {
                BootstrapModel bootstrapModel = ((Bootstrap) baseQuery).getBootstrapModel();
                if (bootstrapModel != null) {
                    Globals.g_Bootstrap_Model = bootstrapModel;

                    List<StartupDisplay> startupDisplayList = bootstrapModel.getStartupDisplayList();
                    TKDrawable tkDrawable = LauncherActivity.getStartupDisplayDrawable(mThis, startupDisplayList);
                    if (tkDrawable != null) {
                        tkDrawable.loadDrawable(mThis, null, AsyncImageLoader.SUPER_VIEW_TOKEN);
                    }
                }
                if (uiStackPeek() == R.id.view_more_home) {
                    getMoreFragment().refreshBootStrapData(true);
                }else{
                	getMoreFragment().refreshBootStrapData(false);
                }

            } else if (baseQuery instanceof NoticeQuery) {
            	Response response = baseQuery.getResponse();
            	if (response instanceof NoticeResultResponse) {
            		NoticeResultResponse noticeResultResponse = (NoticeResultResponse) response;
            		if(noticeResultResponse != null){
            			getMoreFragment().refreshMoreNotice(noticeResultResponse);
            		}
            	}
            } else if (baseQuery instanceof FeedbackUpload){
            	Response response = baseQuery.getResponse();
            	if(response != null) {
            		int resId = getResponseResId(baseQuery);
            		if(resId == R.string.response_code_200){
            		    if (baseQuery.hasParameter(FeedbackUpload.SERVER_PARAMETER_DISPLAY)) {

            		    } else {
            			    TKConfig.setPref(mThis, TKConfig.PREFS_LAST_UPLOAD_APPLIST, String.valueOf(CalendarUtil.getExactTime(mThis)));
            			    LogWrapper.d("Trap", "AppList Upload Success");
            		    }
            		}
            	}
            }
        }
    }

    public void clearMap() {
        // clear pins
        try {
            mMapView.clearMap();
        } catch (APIException e) {
            e.printStackTrace();
        }
    }

    public boolean showInfoWindow(OverlayItem overlayItem) {
        return showInfoWindow(R.id.view_result_map, overlayItem);
    }

    public boolean showInfoWindow(int fragmentId, OverlayItem overlayItem) {
        return showInfoWindow(fragmentId, overlayItem, true);
    }

    public boolean showInfoWindow(int fragmentId, OverlayItem overlayItem, boolean replaceBottomUI) {

        if (overlayItem == null) {
            return false;
        }

        BaseFragment fragment = getFragment(fragmentId);
        InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
        if (fragment != null && infoWindowFragment != null) {
            infoWindowFragment.setData(fragmentId, overlayItem.getOwnerOverlay(), fragment.mActionTag);
            fragment.mBottomFragment = infoWindowFragment;
            if (replaceBottomUI) {
                replaceBottomUI(fragment);
            }
        }

        return true;
    }

    // TODO: cityinfo start
    private List<CityInfo> mViewedCityInfoList = new ArrayList<CityInfo>();

    private void updateCityInfo(CityInfo cityInfo) {
        if (cityInfo == null
                || cityInfo.isAvailably() == false) {
            return;
        }
        String cName = cityInfo.getCName();
        if (mMoreFragment != null) {
            getMoreFragment().refreshCity(cName);
            getMoreFragment().refreshMapDownloadData();
        }
    };

    // TODO: cityinfo end

    // TODO: initMapCenter begin
    private void OnSetup() {
        Position myLocationPosition = null;
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null) {
            myLocationPosition = myLocationCityInfo.getPosition();
        }
        final Position myPosition = myLocationPosition;

        if (myPosition != null) {
            int cityId = MapEngine.getCityId(myPosition);
            CityInfo cityInfo = MapEngine.getCityInfo(cityId);
            if(cityInfo != null) {
	            cityInfo.setPosition(myPosition);
	            cityInfo.setLevel(TKConfig.ZOOM_LEVEL_LOCATION);
	            changeCity(cityInfo);
            }
        } else {
            final boolean gps = Utility.checkGpsStatus(mContext);
            if (!gps) {
                showSettingLocationDialog();
            }
        }
    }

    private void showPromptSettingLocationDialog() {
        boolean showLocationSettingsTip = TextUtils.isEmpty(TKConfig.getPref(Sphinx.this, TKConfig.PREFS_SHOW_LOCATION_SETTINGS_TIP));
        if (showLocationSettingsTip == false) {
            return;
        }
        showSettingLocationDialog();
    }

    public boolean showSettingLocationDialog() {

        Dialog dialog;

        dialog = getDialog(R.id.dialog_prompt_setting_location);
        if (dialog.isShowing()) {
            return false;
        }

        showDialog(R.id.dialog_prompt_setting_location);
        return true;
    }
    // TODO: initMapCenter end

    // TODO: query begin
    public TKAsyncTask queryStart(List<BaseQuery> baseQuerying, boolean cancelable) {
    	TKAsyncTask tkAsyncTask = super.queryStart(baseQuerying, cancelable);
    	if (tkAsyncTask != null) {
	        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
	        int targetViewId = baseQuery.getTargetViewId();
	        BaseFragment baseFragment = getFragment(targetViewId);
	        if (baseFragment != null) {
	            baseFragment.setTkAsyncTasking(tkAsyncTask);
	            baseFragment.setBaseQuerying(baseQuerying);
	        }
    	}
        return tkAsyncTask;
    }
    // TODO: query end

    // TODO: ui stack begin
    public Object mUILock = new Object();
    public boolean mUIProcessing = false;
    boolean mUIPreventDismissCallBack = false;
    private ArrayList<Integer> mUIStack = new ArrayList<Integer>();

    public int uiStackPeekBottom(){
        synchronized (mUILock) {
        	if(mUIStack.size()>0){
        		return mUIStack.get(0);
        	}else{
        		return R.id.view_home;
        	}
        }
    }

    public boolean uiStackInsert(int id, int index) {
        synchronized (mUILock) {
            boolean result = false;
            int oldId = uiStackPeek();
            if (oldId != id && id != R.id.view_invalid) {
                mUIStack.add(index, id);
                result = true;
            }
            return result;
        }
    }

    public boolean uiStackPush(int id) {
        synchronized (mUILock) {
            boolean result = false;
            int oldId = uiStackPeek();
            if (oldId != id && id != R.id.view_invalid) {
                mUIStack.add(id);
                result = true;
            }
            return result;
        }
    }

    public boolean uiStackContains(int id) {
        synchronized (mUILock) {
            boolean result = mUIStack.contains(id);
            return result;
        }
    }

    public int uiStackPeek() {
        synchronized (mUILock) {
            int size = mUIStack.size();
            if (size > 0) {
                return mUIStack.get(size-1);
            }
            return R.id.view_invalid;
        }
    }

    public boolean uiStackRemove(int id) {
        synchronized (mUILock) {
            mUIPreventDismissCallBack = true;
            boolean result = false;
            for(int i = mUIStack.size()-1; i >= 0; i--) {
                if (mUIStack.get(i) == id) {
                    mUIStack.remove(i);
                    BaseFragment baseFragment = getFragment(id);
                    if (baseFragment != null) {
                        baseFragment.dismiss();
                    }
                    result = true;
                    break;
                }
            }
            mUIPreventDismissCallBack = false;
            return result;
        }
    }

    public boolean uiStackDismiss(int id) {
        synchronized (mUILock) {
            boolean result = false;
            if (mUIPreventDismissCallBack) {
                return result;
            }
            if (id == R.id.view_invalid || id != uiStackPeek()) {
                return result;
            }

            mUIProcessing = true;
            int size = mUIStack.size();
            if (size > 0 && uiStackPeek() == id) {
                mUIStack.remove(size-1);
                if (uiStackPeek() == R.id.view_result_map) {
                    if (id == R.id.view_poi_detail) {
                        if (uiStackContains(R.id.view_poi_result) || uiStackContains(R.id.view_discover_list)) {
                            uiStackRemove(R.id.view_result_map);
                        }
                    } else if (id == R.id.view_discover_tuangou_detail ||
                            id == R.id.view_discover_dianying_detail ||
                            id == R.id.view_discover_yanchu_detail ||
                            id == R.id.view_discover_zhanlan_detail ||
                            id == R.id.view_traffic_busline_detail ||
                            id == R.id.view_traffic_result_detail) {
                        uiStackRemove(R.id.view_result_map);
                    } else if (id == R.id.view_traffic_result_transfer) {
                        uiStackClearTop(R.id.view_traffic_home);
                    }
                } else if (uiStackPeek() == R.id.view_poi_detail) {
                    if (id == R.id.view_hotel_order_detail) {
                    	if(uiStackContains(R.id.view_hotel_order_list)){
                    		uiStackClearTop(R.id.view_hotel_order_list);
                    	}else{
                    		uiStackClearTop(R.id.view_poi_detail);
                    	}
                    }
                } else if (uiStackPeek() == R.id.view_hotel_order_list) {
                    if (id == R.id.view_hotel_order_detail) {
                        uiStackClearTop(R.id.view_hotel_order_list);
                    }
                } else if (uiStackPeek() == R.id.view_poi_nearby_search) {
                	if (id == R.id.view_poi_input_search) {
                		mHandler.sendEmptyMessage(UI_STACK_ADJUST_CANCEL);
                	}
                }

                id = uiStackPeek();
                BaseFragment fragment = getFragment(id);

                if (fragment != null) {
                    fragment.onResume();
                    result = true;
                } else {
                    boolean isNormalExit = false;
                    int count = mBodyView.getChildCount();
                    if (count == 1) {
                        View v = mBodyView.getChildAt(0);
                        if (v instanceof HomeFragment ||
                                v instanceof TrafficQueryFragment ||
                                v instanceof MoreHomeFragment) {
                            isNormalExit = true;
                        }
                    }

                    if (isNormalExit == false) {
                        uiStackClose(null);
                        showView(R.id.view_home);
                    }
                }
            } else {
                boolean isNormalExit = false;
                int count = mBodyView.getChildCount();
                if (count == 1) {
                    View v = mBodyView.getChildAt(0);
                    if (v instanceof HomeFragment ||
                            v instanceof TrafficQueryFragment ||
                            v instanceof MoreHomeFragment) {
                        isNormalExit = true;
                    }
                }

                if (isNormalExit == false) {
                    uiStackClose(null);
                    showView(R.id.view_home);
                }
            }
            LogWrapper.d("UIStack", "Dismiss:" + mUIStack);
            mUIProcessing = false;
            return result;
        }
    }

    public boolean uiStackPop(int id) {
        synchronized (mUILock) {
            boolean result = false;
            if (uiStackPeek() != id || mUIStack.size() < 1) {
                return result;
            }
            mUIPreventDismissCallBack = true;
            mUIStack.remove(mUIStack.size()-1);
            BaseFragment baseFragment = getFragment(id);
            if (baseFragment != null) {
                baseFragment.dismiss();
                result = true;
            }
            mUIPreventDismissCallBack = false;
            return result;
        }
    }

    public boolean uiStackBack() {
        synchronized (mUILock) {
            boolean result = false;
            if (mUIStack.size() > 1) {
                int id = uiStackPeek();

                if (id != R.id.view_invalid) {
                    dismissView(id);
                    result = true;
                }
            }
            return result;
        }
    }

    public void uiStackClose(int[] filterIds) {
        synchronized (mUILock) {
            mUIPreventDismissCallBack = true;
            // 从最上面的窗口往下逐渐关闭
            for(int i = mUIStack.size()-1; i >= 0; i--) {
                int id = mUIStack.get(i);
                boolean dismiss = true;
                if (filterIds != null) {
                    for(int filterId : filterIds) {
                        if (filterId == id) {
                            dismiss = false;
                            break;
                        }
                    }
                }
                if (dismiss) {
                    mUIStack.remove(i);
                    BaseFragment baseFragment = getFragment(id);
                    if (baseFragment != null) {
                        baseFragment.dismiss();
                    }
                }
            }
            mUIPreventDismissCallBack = false;
        }
    }

    public boolean uiStackClearTop(int preferTop) {
    	LogWrapper.d(TAG, "uiStackClearTop");
    	synchronized (mUILock) {
            boolean result = false;
            if (uiStackContains(preferTop) == false) {
                return result;
            }
            int[] filterIds = null;
            int index = mUIStack.indexOf(preferTop);
            int size = mUIStack.size();
            if (index > -1 && index < size) {
                filterIds = new int[index+1];
                for(int i = 0; i <= index; i++) {
                    filterIds[i] = mUIStack.get(i);
                }
            }
            uiStackClose(filterIds);
            BaseFragment baseFragment = getFragment(preferTop);
            if (baseFragment != null) {
                baseFragment.onResume();
                result = true;
            }

            for(int i = mUIStack.size()-1; i > index; i--) {
                mUIStack.remove(i);
            }

            return result;
        }
    }

    public boolean uiStackClearBetween(int preferLower, int preferUpper){
    	LogWrapper.d(TAG, "uiStackClearBetween");
    	synchronized (mUILock) {
			int[] filterIds = null;
			int lowIndex = mUIStack.indexOf(preferLower);
			if(lowIndex < 0){
				return false;
			}
			int uppIndex = -1;
			int size = mUIStack.size();
			for(int i = size -1; i > lowIndex; i--){
				if(mUIStack.get(i) == preferUpper){
					uppIndex = i;
					break;
				}
			}
			if(uppIndex < 0){
				return false;
			}
			filterIds = new int[size - uppIndex + lowIndex +1];
			int p = 0;
			for(int i = 0; i<= lowIndex; i++){
				filterIds[p] = mUIStack.get(i);
				p++;
			}
			for(int i = size-1 ; i>=uppIndex; i--){
				filterIds[p] = mUIStack.get(i);
				p++;
			}
			uiStackClose(filterIds);
			for(int i = uppIndex - (size-mUIStack.size()) - 1; i>lowIndex; i--){
				mUIStack.remove(i);
			}
			return true;
		}
    }

    private void uiStackAdjust(int upperID) {
    	LogWrapper.d(TAG, "uiStackAdjust");
    	synchronized (mUILock) {
	        if (uiStackContains(R.id.view_more_favorite)) {
	            uiStackClearBetween(R.id.view_more_favorite, upperID);
	        } else if (uiStackContains(R.id.view_more_history)) {
	            uiStackClearBetween(R.id.view_more_history, upperID);
	        } else if (uiStackContains(R.id.view_more_go_comment)) {
	            uiStackClearBetween(R.id.view_more_go_comment, upperID);
	        } else if (uiStackContains(R.id.view_user_my_comment_list)) {
	            uiStackClearBetween(R.id.view_user_my_comment_list, upperID);
	        } else if (uiStackContains(R.id.view_more_my_order)) {
	            uiStackClearBetween(R.id.view_more_my_order, upperID);
	        } else {
	            if (uiStackContains(R.id.view_home) == false) {
	                uiStackInsert(R.id.view_home, 0);
	            }
	            uiStackClearBetween(R.id.view_home, upperID);
	        }
	        LogWrapper.d("UIStack", "Adjust:" + mUIStack);
    	}
    }    
    
    public int uiStackSize() {
        synchronized (mUILock) {
            return mUIStack.size();
        }
    }

    public boolean showHint(String key, int layoutResId) {
        boolean showView = false;
        if (TKConfig.getPref(mContext, key) != null) {
            showView = false;
        } else {
            showView = showHint(new String[] {key}, new int[] {layoutResId});
        }
        return showView;
    }

    public boolean showHint(String[] keyList, int[] layoutResIdList) {
        if (mFromThirdParty > 0) {
            return false;
        }
        boolean showView = false;
        if (TKConfig.getPref(mContext, keyList[0]) != null) {
            showView = false;
        } else {
            Intent intent = new Intent();
            intent.putExtra(HintActivity.EXTRA_KEY_LIST, keyList);
            intent.putExtra(HintActivity.EXTRA_LAYOUT_RES_ID_LIST, layoutResIdList);
            showView = showView(R.id.activity_hint, intent);
        }
        return showView;
    }

    public boolean showView(int viewId) {
        return showView(viewId, null);
    }

    public boolean showView(int viewId, Intent intent) {
        synchronized (mUILock) {
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra(BaseActivity.SOURCE_USER_HOME, uiStackContains(R.id.view_user_home));
            if (R.id.activity_hint == viewId) {
                intent.setClass(this, HintActivity.class);
                startActivityForResult(intent, R.id.activity_hint);
                return true;
            } else if (R.id.activity_more_about_us == viewId) {
                intent.setClass(this, AboutUsActivity.class);
                startActivityForResult(intent, R.id.activity_more_about_us);
                return true;
            } else if (R.id.activity_more_app_recommend == viewId) {
                intent.setClass(this, AppRecommendActivity.class);
                startActivityForResult(intent, R.id.activity_more_app_recommend);
                return true;
            } else if (R.id.activity_poi_report_error == viewId) {
                intent.setClass(this, POIReportErrorActivity.class);
                startActivityForResult(intent, R.id.activity_poi_report_error);
                return true;
            } else if (R.id.activity_traffic_report_error == viewId) {
                intent.setClass(this, TrafficReportErrorActivity.class);
                startActivityForResult(intent, R.id.activity_traffic_report_error);
                return true;
            } else if (R.id.activity_traffic_compass == viewId) {
                intent.setClass(this, TrafficCompassActivity.class);
                startActivityForResult(intent, R.id.activity_traffic_compass);
                return true;
            } else if (R.id.activity_more_satisfy == viewId) {
                intent.setClass(this, SatisfyRateActivity.class);
                startActivityForResult(intent, R.id.activity_more_satisfy);
                return true;
            } else if (R.id.activity_more_feedback == viewId) {
                intent.setClass(this, FeedbackActivity.class);
                startActivityForResult(intent, R.id.activity_more_feedback);
                return true;
            } else if (R.id.activity_more_change_city == viewId) {
                intent.setClass(this, ChangeCityActivity.class);
                startActivityForResult(intent, R.id.activity_more_change_city);
                return true;
            } else if (R.id.activity_more_setting == viewId) {
                intent.setClass(this, SettingActivity.class);
                startActivityForResult(intent, R.id.activity_more_setting);
                return true;
            } else if (R.id.activity_poi_edit_comment == viewId) {
                intent.setClass(this, EditCommentActivity.class);
                startActivityForResult(intent, R.id.activity_poi_edit_comment);
                return true;
            } else if (R.id.activity_poi_comment_list == viewId) {
                intent.setClass(this, CommentListActivity.class);
                startActivityForResult(intent, R.id.activity_poi_comment_list);
                return true;
            } else if (R.id.activity_more_map_download == viewId) {
                ArrayList<Integer> cityIdList = new ArrayList<Integer>();
                List<CityInfo> cityInfoList = mViewedCityInfoList;
                for(CityInfo cityInfo : cityInfoList) {
                    cityIdList.add(cityInfo.getId());
                }
                cityInfoList.clear();
                intent.putIntegerArrayListExtra(MapDownloadActivity.EXTRA_VIEWED_CITY_ID_LIST, cityIdList);
                intent.setClass(this, MapDownloadActivity.class);
                startActivityForResult(intent, R.id.activity_more_map_download);
                return true;
            } else if (R.id.activity_user_login_regist == viewId) {
                intent.setClass(this, UserLoginRegistActivity.class);
                startActivityForResult(intent, R.id.activity_user_login_regist);
                return true;
            } else if (R.id.activity_browser == viewId) {
                intent.setClass(this, BrowserActivity.class);
                startActivityForResult(intent, R.id.activity_browser);
                return true;
            } else if (R.id.activity_more_add_merchant == viewId) {
                intent.setClass(this, AddMerchantActivity.class);
                startActivityForResult(intent, R.id.activity_more_add_merchant);
                return true;
            } else if (R.id.activity_user_update_phone == viewId) {
                intent.setClass(this, UserUpdatePhoneActivity.class);
                startActivityForResult(intent, R.id.activity_user_update_phone);
                return true;
            } else if (R.id.activity_user_update_nickname == viewId) {
                intent.setClass(this, UserUpdateNickNameActivity.class);
                startActivityForResult(intent, R.id.activity_user_update_nickname);
                return true;
            } else if (R.id.activity_user_update_password == viewId) {
                intent.setClass(this, UserUpdatePasswordActivity.class);
                startActivityForResult(intent, R.id.activity_user_update_password);
                return true;
            } else if (R.id.activity_setting_location == viewId) {
                intent.setAction("android.settings.LOCATION_SOURCE_SETTINGS");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                startActivityForResult(intent, R.id.activity_setting_location);
                return true;
            } else if (R.id.activity_hotel_intro == viewId) {
                intent.setClass(this, HotelIntroActivity.class);
                startActivityForResult(intent, R.id.activity_hotel_intro);
                return true;
            } else if (R.id.activity_take_screenshot == viewId) {
                intent.setClass(this, TakeScreenshotActivity.class);
                startActivityForResult(intent, R.id.activity_take_screenshot);
                return true;
            } else if (R.id.activity_poi_dish == viewId) {
                intent.setClass(this, DishActivity.class);
                startActivityForResult(intent, R.id.activity_poi_dish);
                return true;
            } else if (R.id.activity_common_view_image == viewId) {
                intent.setClass(this, ViewImageActivity.class);
                startActivityForResult(intent, R.id.activity_common_view_image);
                return true;
            }

            mUIProcessing = true;
            boolean show = false;
            int currentId = uiStackPeek();
            BaseFragment baseFragment = getFragment(viewId);
            if (currentId != viewId) {
                if (baseFragment != null) {

                    BaseFragment backBaseFragment = getFragment(currentId);
                    if (backBaseFragment != null) {
                        backBaseFragment.onPause();
                    }

                    baseFragment.show();
                    show = true;
                }
            } else {
                if (baseFragment != null) {
                	baseFragment.onResume();
                    show = true;
                }
            }

            if(show){
            	LogWrapper.d("UIStack", "Show:" + mUIStack);
            }
            mUIProcessing = false;
            return show;
        }
    }

    public boolean dismissView(int viewId) {
        synchronized (mUILock) {
            boolean dismiss = false;
            BaseFragment baseFragment = getFragment(viewId);
            if (baseFragment != null && baseFragment.isShown()) {
                baseFragment.dismiss();
                dismiss = true;
            }
            return dismiss;
        }
    }

    public void replaceBodyUI(BaseFragment baseFragment) {
        synchronized (mUILock) {
            mBodyFragment = baseFragment;

            if (mBodyView.getChildAt(0) != mBodyFragment) {
                mBodyView.removeAllViews();
                if (mBodyFragment != null) {
                    mBodyView.addView(mBodyFragment);
                }
            }
        }
    }

    public void replaceBottomUI(BaseFragment fragment) {
        synchronized (mUILock) {
            if (fragment != null) {
                mBottomFragment = fragment.mBottomFragment;
            } else {
                mBottomFragment = null;
            }

            if (mBottomFragment != null) {
                setMapViewPaddingBottom(mBottomFragment.mHeight);
            } else {
                setMapViewPaddingBottom(0);
            }

            if (mBottomView.getChildAt(0) != mBottomFragment) {
                mBottomView.removeAllViews();
                if (mBottomFragment != null &&
                        (fragment != null &&
                        (fragment.getId() == R.id.view_home ||
                            fragment.getId() == R.id.view_result_map))) {
                    mBottomView.addView(mBottomFragment);
                }
            }
        }
    }
    
    public BaseFragment getBottomFragment() {
        return mBottomFragment;
    }
    // TODO: ui stack end

    // TODO: get dialog begin

    public Dialog getDialog(int id) {
        Dialog dialog = null;
        CheckBox checkChb;
        switch (id) {
            case R.id.dialog_share_doing:
                dialog = new ProgressDialog(this);
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                ((ProgressDialog)dialog).setMessage(getString(R.string.doing_and_wait));
                dialog.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        mActionLog.addAction(ActionLog.Dialog + ActionLog.Dismiss);
                    }
                });
                break;

            case R.id.dialog_prompt_setting_location:
                View settingLocationView = mLayoutInflater.inflate(R.layout.alert_setting_location, null, false);
                dialog = Utility.getDialog(Sphinx.this,
                        getString(R.string.advice_open_gps_title),
                        null,
                        settingLocationView,
                        Sphinx.this.getString(R.string.i_know),
                        Sphinx.this.getString(R.string.settings),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                if (id == DialogInterface.BUTTON_NEGATIVE) {
                                    showView(R.id.activity_setting_location);
                                }
                            }
                        });


                checkChb = (CheckBox) dialog.findViewById(R.id.check_chb);
                checkChb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                        TKConfig.setPref(Sphinx.this, TKConfig.PREFS_SHOW_LOCATION_SETTINGS_TIP, checked ? "1" : "");
                    }
                });
                dialog.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        showHint(TKConfig.PREFS_HINT_HOME, R.layout.hint_home);
                    }
                });
                break;

            default:
                break;
        }

        return dialog;
    }
    // TODO: get dialog end

    // TODO: get fragment start
    private TitleFragment mTitleFragment;

    private InfoWindowFragment mInfoWindowFragment;

    private HomeBottomFragment mHomeBottomFragment;
    private HomeFragment mHomeFragment;
    private MoreHomeFragment mMoreFragment;
    private MyOrderFragment mMyOrderFragment;
    private GoCommentFragment mGoCommentFragment;
    private ResultMapFragment mResultMapFragment;
    private BrowserFragment mBrowserFragment;
    private FavoriteFragment mFavoriteFragment;
    private HistoryFragment mHistoryFragment;
    private POIDetailFragment mPOIDetailFragment;
    private POIResultFragment mPOIResultFragment;
    private InputSearchFragment mInputSearchFragment;
    private NearbySearchFragment mPOINearbyFragment;
    private CustomCategoryFragment mCustomCategoryFragment;
    private TrafficDetailFragment mTrafficDetailFragment = null;
    private TrafficResultFragment mTrafficResultFragment = null; 
    private TrafficResultListMapFragment mTrafficResultListMapFragment = null;
    private TrafficCommonPlaceFragment mTrafficCommonAddressFragment = null;
    private TrafficSearchHistoryFragment mTrafficSearchHistoryFragment = null;
    private BuslineResultLineFragment mBuslineResultLineFragment = null;
    private BuslineResultStationFragment mBuslineResultStationFragment = null;
    private BuslineDetailFragment mBuslineDetailFragment = null;
    private TrafficQueryFragment mTrafficQueryFragment = null;
    private SubwayMapFragment mSubwayMapFragment = null;
    private FetchFavoriteFragment mFetchFavoriteFragment = null;
    private MyCommentListFragment mMyCommentListFragment;
    private UserHomeFragment mUserHomeFragment;

    private DiscoverListFragment mDiscoverListFragment;
    private TuangouDetailFragment mTuangouDetailFragment;
    private YanchuDetailFragment mYanchuDetailFragment;
    private ZhanlanDetailFragment mZhanlanDetailFragment;
    private DianyingDetailFragment mDianyingDetailFragment;
    private DiscoverChildListFragment mDiscoverChildListFragment;

    private HotelHomeFragment mHotelHomeFragment;
    private PickLocationFragment mPickLocationFragment;
    private HotelOrderWriteFragment mHotelOrderWriteFragment;
    private HotelOrderCreditFragment mHotelOrderCreditFragment;

    private HotelOrderSuccessFragment mHotelOrderSuccessFragment;
    private HotelOrderDetailFragment mHotelOrderDetailFragment;
    private HotelOrderDetailFragment mHotelOrderDetailFragment2;
    private HotelOrderListFragment mHotelOrderListFragment;

    private CouponListFragment mCouponListFragment;
    private CouponDetailFragment mCouponDetailFragment;

    private MeasureDistanceFragment mMeasureDistanceFragment;

    public BaseFragment getFragment(int id) {
        BaseFragment baseFragment = null;

        switch (id) {
            case R.id.view_infowindow:
                baseFragment = getInfoWindowFragment();
                break;
            case R.id.view_more_home:
                baseFragment = getMoreFragment();
                break;

            case R.id.view_home:
                baseFragment = getHomeFragment();
                break;

            case R.id.view_more_my_order:
                baseFragment = getMyOrderFragment();
                break;

            case R.id.view_result_map:
                baseFragment = getResultMapFragment();
                break;

            case R.id.view_browser:
                baseFragment = getBrowserFragment();
                break;

            case R.id.view_more_favorite:
                baseFragment = getFavoriteFragment();
                break;

            case R.id.view_more_history:
                baseFragment = getHistoryFragment();
                break;

            case R.id.view_poi_result:
                baseFragment = getPOIResultFragment();
                break;

            case R.id.view_poi_detail:
                baseFragment = getPOIDetailFragment();
                break;

            case R.id.view_poi_input_search:
                baseFragment = getInputSearchFragment();
                break;

            case R.id.view_poi_nearby_search:
                baseFragment = getPOINearbyFragment();
                break;

            case R.id.view_poi_custom_category:
            	baseFragment = getCustomCategoryFragment();
            	break;

            case R.id.view_traffic_result_transfer:
                baseFragment = getTrafficResultFragment();
                break;

            case R.id.view_traffic_result_detail:
                baseFragment = getTrafficDetailFragment();
                break;

            case R.id.view_traffic_busline_line_result:
                baseFragment = getBuslineResultLineFragment();
                break;

            case R.id.view_traffic_busline_station_result:
                baseFragment = getBuslineResultStationFragment();
                break;

            case R.id.view_traffic_busline_detail:
                baseFragment = getBuslineDetailFragment();
                break;

            case R.id.view_traffic_home:
                baseFragment = getTrafficQueryFragment();
                break;

            case R.id.view_traffic_common_places:
                baseFragment = getTrafficCommonAddressFragment();
                break;

            case R.id.view_traffic_search_history:
                baseFragment = getTrafficSearchHistoryFragment();
                break;

            case R.id.view_traffic_fetch_favorite_poi:
                baseFragment = getFetchFavoriteFragment();
                break;

            case R.id.view_subway_map:
                baseFragment = getSubwayMapFragment();
                break;

            case R.id.view_user_home:
                baseFragment = getUserHomeFragment();
                break;

            case R.id.view_user_my_comment_list:
                baseFragment = getMyCommentListFragment();
                break;

            case R.id.view_more_go_comment:
                baseFragment = getGoCommentFragment();
                break;

            case R.id.view_discover_list:
                baseFragment = getDiscoverListFragment();
                break;

            case R.id.view_discover_tuangou_detail:
                baseFragment = getTuangouDetailFragment();
                break;

            case R.id.view_discover_child_list:
                baseFragment = getDiscoverChildListFragment();
                break;

            case R.id.view_discover_yanchu_detail:
                baseFragment = getYanchuDetailFragment();
                break;
            case R.id.view_discover_dianying_detail:
            	baseFragment = getDianyingDetailFragment();
                break;
            case R.id.view_discover_zhanlan_detail:
                baseFragment = getZhanlanDetailFragment();
                break;

            case R.id.view_hotel_home:
                baseFragment = getHotelHomeFragment();
                break;

            case R.id.view_hotel_pick_location:
                baseFragment = getPickLocationFragment();
                break;

            case R.id.view_hotel_order_write:
                baseFragment = getHotelOrderWriteFragment();
                break;

            case R.id.view_hotel_credit_assure:
                baseFragment = getHotelOrderCreditFragment();
                break;

            case R.id.view_hotel_order_list:
            	baseFragment = getHotelOrderListFragment();
            	break;
            	
            case R.id.view_hotel_order_success:
            	baseFragment = getHotelOrderSuccessFragment();
            	break;

            case R.id.view_hotel_order_detail:
            	baseFragment = getHotelOrderDetailFragment();
            	break;
            	
            case R.id.view_hotel_order_detail_2:
            	baseFragment = getHotelOrderDetailFragmentTwo();
            	break;

            case R.id.view_coupon_list:
                baseFragment = getCouponListFragment();
                break;

            case R.id.view_coupon_detail:
                baseFragment = getCouponDetailFragment();
                break;

            case R.id.view_measure_distance:
                baseFragment = getMeasureDistanceFragment();
                break;
                
            case R.id.view_traffic_result_list_map:
                baseFragment = getTrafficResultListMapFragment();
                break;

            default:
                break;
        }
        return baseFragment;
    }

    public ViewGroup getTitleView() {
        synchronized (mUILock) {
            return mTitleView;
        }
    }

    public TitleFragment getTitleFragment() {
        synchronized (mUILock) {
            if (mTitleFragment == null) {
                TitleFragment fragment = new TitleFragment(Sphinx.this);
                fragment.onCreate(null);
                fragment.setVisibility(View.GONE);
                mTitleView.addView(fragment);
                mTitleFragment = fragment;
            }
            return mTitleFragment;
        }
    }

    public InfoWindowFragment getInfoWindowFragment() {
        synchronized (mUILock) {
            if (mInfoWindowFragment == null) {
                InfoWindowFragment fragment = new InfoWindowFragment(Sphinx.this);
                fragment.setId(R.id.view_infowindow);
                fragment.onCreate(null);
                mInfoWindowFragment = fragment;
            }
            return mInfoWindowFragment;
        }
    }

    public HomeBottomFragment getHomeBottomFragment() {
        synchronized (mUILock) {
            if (mHomeBottomFragment == null) {
                HomeBottomFragment fragment = new HomeBottomFragment(Sphinx.this);
                fragment.onCreate(null);
                mHomeBottomFragment = fragment;
            }
            return mHomeBottomFragment;
        }
    }

    public HomeFragment getHomeFragment() {
        synchronized (mUILock) {
            if (mHomeFragment == null) {
                HomeFragment fragment = new HomeFragment(Sphinx.this);
                fragment.setId(R.id.view_home);
                fragment.onCreate(null);
                mHomeFragment = fragment;
            }
            return mHomeFragment;
        }
    }

    public MyOrderFragment getMyOrderFragment() {
        synchronized (mUILock) {
            if (mMyOrderFragment == null) {
                MyOrderFragment fragment = new MyOrderFragment(Sphinx.this);
                fragment.setId(R.id.view_more_my_order);
                fragment.onCreate(null);
                mMyOrderFragment = fragment;
            }
            return mMyOrderFragment;
        }
    }

    public MoreHomeFragment getMoreFragment() {
        synchronized (mUILock) {
            if (mMoreFragment == null) {
                MoreHomeFragment moreFragment = new MoreHomeFragment(Sphinx.this);
                moreFragment.setId(R.id.view_more_home);
                moreFragment.onCreate(null);
                mMoreFragment = moreFragment;
            }
            return mMoreFragment;
        }
    }

    public ResultMapFragment getResultMapFragment() {
        synchronized (mUILock) {
            if (mResultMapFragment == null) {
                ResultMapFragment resultMapFragment = new ResultMapFragment(Sphinx.this);
                resultMapFragment.setId(R.id.view_result_map);
                resultMapFragment.onCreate(null);
                mResultMapFragment = resultMapFragment;
            }
            return mResultMapFragment;
        }
    }

    public BrowserFragment getBrowserFragment() {
        synchronized (mUILock) {
            if (mBrowserFragment == null) {
                BrowserFragment fragment = new BrowserFragment(Sphinx.this);
                fragment.setId(R.id.view_browser);
                fragment.onCreate(null);
                mBrowserFragment = fragment;
            }
            return mBrowserFragment;
        }
    }

    public FavoriteFragment getFavoriteFragment() {
        synchronized (mUILock) {
            if (mFavoriteFragment == null) {
                FavoriteFragment favoriteFragment = new FavoriteFragment(Sphinx.this);
                favoriteFragment.setId(R.id.view_more_favorite);
                favoriteFragment.onCreate(null);
                mFavoriteFragment = favoriteFragment;
            }
            return mFavoriteFragment;
        }
    }

    public HistoryFragment getHistoryFragment() {
        synchronized (mUILock) {
            if (mHistoryFragment == null) {
                HistoryFragment historyFragment = new HistoryFragment(Sphinx.this);
                historyFragment.setId(R.id.view_more_history);
                historyFragment.onCreate(null);
                mHistoryFragment = historyFragment;
            }
            return mHistoryFragment;
        }
    }

    public TrafficSearchHistoryFragment getTrafficSearchHistoryFragment() {
        synchronized (mUILock) {
            if (mTrafficSearchHistoryFragment == null) {
                TrafficSearchHistoryFragment f = new TrafficSearchHistoryFragment(Sphinx.this);
                f.setId(R.id.view_traffic_search_history);
                f.onCreate(null);
                mTrafficSearchHistoryFragment = f;
            }
            return mTrafficSearchHistoryFragment;
        }
    }

    public POIDetailFragment getPOIDetailFragment() {
        synchronized (mUILock) {
            if (mPOIDetailFragment == null) {
                POIDetailFragment poiDetailFragment = new POIDetailFragment(Sphinx.this);
                poiDetailFragment.setId(R.id.view_poi_detail);
                poiDetailFragment.onCreate(null);
                mPOIDetailFragment = poiDetailFragment;
            }
            return mPOIDetailFragment;
        }
    }

    public POIResultFragment getPOIResultFragment() {
        synchronized (mUILock) {
            if (mPOIResultFragment == null) {
                POIResultFragment poiResultFragment = new POIResultFragment(Sphinx.this);
                poiResultFragment.setId(R.id.view_poi_result);
                poiResultFragment.onCreate(null);
                mPOIResultFragment = poiResultFragment;
            }
            return mPOIResultFragment;
        }
    }

    public InputSearchFragment getInputSearchFragment() {
        synchronized (mUILock) {
            if (mInputSearchFragment == null) {
                InputSearchFragment poiQueryFragment = new InputSearchFragment(Sphinx.this);
                poiQueryFragment.setId(R.id.view_poi_input_search);
                poiQueryFragment.onCreate(null);
                mInputSearchFragment = poiQueryFragment;
            }
            return mInputSearchFragment;
        }
    }

    public NearbySearchFragment getPOINearbyFragment() {
        synchronized (mUILock) {
            if (mPOINearbyFragment == null) {
                NearbySearchFragment poiNearbyFragment = new NearbySearchFragment(Sphinx.this);
                poiNearbyFragment.setId(R.id.view_poi_nearby_search);
                poiNearbyFragment.onCreate(null);
                mPOINearbyFragment = poiNearbyFragment;
            }
            return mPOINearbyFragment;
        }
    }

    public CustomCategoryFragment getCustomCategoryFragment() {
        synchronized (mUILock) {
            if (mCustomCategoryFragment == null) {
            	CustomCategoryFragment customCategoryFragment = new CustomCategoryFragment(Sphinx.this);
            	customCategoryFragment.setId(R.id.view_poi_custom_category);
            	customCategoryFragment.onCreate(null);
                mCustomCategoryFragment = customCategoryFragment;
            }
            return mCustomCategoryFragment;
        }
    }

    public TrafficResultFragment getTrafficResultFragment() {
        synchronized (mUILock) {
            if (mTrafficResultFragment == null) {
                TrafficResultFragment trafficResultFragment = new TrafficResultFragment(Sphinx.this);
                trafficResultFragment.setId(R.id.view_traffic_result_transfer);
                trafficResultFragment.onCreate(null);
                mTrafficResultFragment = trafficResultFragment;
            }
            return mTrafficResultFragment;
        }
    }
    
    public TrafficResultListMapFragment getTrafficResultListMapFragment(){

        synchronized (mUILock) {
            if (mTrafficResultListMapFragment == null) {
                TrafficResultListMapFragment fragment = new TrafficResultListMapFragment(Sphinx.this);
                fragment.setId(R.id.view_traffic_result_list_map);
                fragment.onCreate(null);
                mTrafficResultListMapFragment = fragment;
            }
            return mTrafficResultListMapFragment;
        }
    }

    public TrafficDetailFragment getTrafficDetailFragment() {
        synchronized (mUILock) {
            if (mTrafficDetailFragment == null) {
                TrafficDetailFragment trafficDetailFragment = new TrafficDetailFragment(Sphinx.this);
                trafficDetailFragment.setId(R.id.view_traffic_result_detail);
                trafficDetailFragment.onCreate(null);
                mTrafficDetailFragment = trafficDetailFragment;
            }
            return mTrafficDetailFragment;
        }
    }

    public TrafficCommonPlaceFragment getTrafficCommonAddressFragment() {
        synchronized (mUILock) {
            if (mTrafficCommonAddressFragment == null) {
                TrafficCommonPlaceFragment f = new TrafficCommonPlaceFragment(Sphinx.this);
                f.setId(R.id.view_traffic_common_places);
                f.onCreate(null);
                mTrafficCommonAddressFragment = f;
            }
            return mTrafficCommonAddressFragment;
        }
    }

    public BuslineResultLineFragment getBuslineResultLineFragment() {
        synchronized (mUILock) {
            if (mBuslineResultLineFragment == null) {
                BuslineResultLineFragment buslineResultLineFragment = new BuslineResultLineFragment(Sphinx.this);
                buslineResultLineFragment.setId(R.id.view_traffic_busline_line_result);
                buslineResultLineFragment.onCreate(null);
                mBuslineResultLineFragment = buslineResultLineFragment;
            }
            return mBuslineResultLineFragment;
        }
    }

    public BuslineResultStationFragment getBuslineResultStationFragment() {
        synchronized (mUILock) {
            if (mBuslineResultStationFragment == null) {
                BuslineResultStationFragment buslineResultStationFragment = new BuslineResultStationFragment(Sphinx.this);
                buslineResultStationFragment.setId(R.id.view_traffic_busline_station_result);
                buslineResultStationFragment.onCreate(null);
                mBuslineResultStationFragment = buslineResultStationFragment;
            }
            return mBuslineResultStationFragment;
        }
    }

    public BuslineDetailFragment getBuslineDetailFragment() {
        synchronized (mUILock) {
            if (mBuslineDetailFragment == null) {
                BuslineDetailFragment buslineDetailFragment = new BuslineDetailFragment(Sphinx.this);
                buslineDetailFragment.setId(R.id.view_traffic_busline_detail);
                buslineDetailFragment.onCreate(null);
                mBuslineDetailFragment = buslineDetailFragment;
            }
            return mBuslineDetailFragment;
        }
    }

    public TrafficQueryFragment getTrafficQueryFragment() {
        synchronized (mUILock) {
            if (mTrafficQueryFragment == null) {
                TrafficQueryFragment trafficQueryFragment = new TrafficQueryFragment(Sphinx.this);
                trafficQueryFragment.setId(R.id.view_traffic_home);
                trafficQueryFragment.onCreate(null);
                mTrafficQueryFragment = trafficQueryFragment;
            }
            return mTrafficQueryFragment;
        }
    }

    public SubwayMapFragment getSubwayMapFragment() {
        synchronized (mUILock) {
            if (mSubwayMapFragment == null) {
                SubwayMapFragment subwayMapFragment = new SubwayMapFragment(Sphinx.this);
                subwayMapFragment.setId(R.id.view_subway_map);
                subwayMapFragment.onCreate(null);
                mSubwayMapFragment = subwayMapFragment;
            }
            return mSubwayMapFragment;
        }
    }

    public FetchFavoriteFragment getFetchFavoriteFragment() {
        synchronized (mUILock) {
            if (mFetchFavoriteFragment == null) {
                FetchFavoriteFragment fetchFavoriteFragment = new FetchFavoriteFragment(Sphinx.this);
                fetchFavoriteFragment.setId(R.id.view_traffic_fetch_favorite_poi);
                fetchFavoriteFragment.onCreate(null);
                mFetchFavoriteFragment = fetchFavoriteFragment;
            }
            return mFetchFavoriteFragment;
        }
    }

    public UserHomeFragment getUserHomeFragment() {
        synchronized (mUILock) {
            if (mUserHomeFragment == null) {
                UserHomeFragment userHomeFragment = new UserHomeFragment(Sphinx.this);
                userHomeFragment.setId(R.id.view_user_home);
                userHomeFragment.onCreate(null);
                mUserHomeFragment = userHomeFragment;
            }
            return mUserHomeFragment;
        }
    }


    public MyCommentListFragment getMyCommentListFragment() {
        synchronized (mUILock) {
            if (mMyCommentListFragment == null) {
                MyCommentListFragment myCommentListFragment = new MyCommentListFragment(Sphinx.this);
                myCommentListFragment.setId(R.id.view_user_my_comment_list);
                myCommentListFragment.onCreate(null);
                mMyCommentListFragment = myCommentListFragment;
            }
            return mMyCommentListFragment;
        }
    }

    public GoCommentFragment getGoCommentFragment() {
        synchronized (mUILock) {
            if (mGoCommentFragment == null) {
                GoCommentFragment goCommentFragment = new GoCommentFragment(Sphinx.this);
                goCommentFragment.setId(R.id.view_more_go_comment);
                goCommentFragment.onCreate(null);
                mGoCommentFragment = goCommentFragment;
            }
            return mGoCommentFragment;
        }
    }

    public DiscoverListFragment getDiscoverListFragment() {
        synchronized (mUILock) {
            if (mDiscoverListFragment == null) {
                DiscoverListFragment discoverListFragment = new DiscoverListFragment(Sphinx.this);
                discoverListFragment.setId(R.id.view_discover_list);
                discoverListFragment.onCreate(null);
                mDiscoverListFragment = discoverListFragment;
            }
            return mDiscoverListFragment;
        }
    }

    public TuangouDetailFragment getTuangouDetailFragment() {
        synchronized (mUILock) {
            if (mTuangouDetailFragment == null) {
                TuangouDetailFragment tuangouDetailFragment = new TuangouDetailFragment(Sphinx.this);
                tuangouDetailFragment.setId(R.id.view_discover_tuangou_detail);
                tuangouDetailFragment.onCreate(null);
                mTuangouDetailFragment = tuangouDetailFragment;
            }
            return mTuangouDetailFragment;
        }
    }

    public DiscoverChildListFragment getDiscoverChildListFragment() {
        synchronized (mUILock) {
            if (mDiscoverChildListFragment == null) {
                DiscoverChildListFragment discoverChildListFragment = new DiscoverChildListFragment(Sphinx.this);
                discoverChildListFragment.setId(R.id.view_discover_child_list);
                discoverChildListFragment.onCreate(null);
                mDiscoverChildListFragment = discoverChildListFragment;
            }
            return mDiscoverChildListFragment;
        }
    }

    public YanchuDetailFragment getYanchuDetailFragment() {
        synchronized (mUILock) {
            if (mYanchuDetailFragment == null) {
                YanchuDetailFragment yanchuDetailFragment = new YanchuDetailFragment(Sphinx.this);
                yanchuDetailFragment.setId(R.id.view_discover_yanchu_detail);
                yanchuDetailFragment.onCreate(null);
                mYanchuDetailFragment = yanchuDetailFragment;
            }
            return mYanchuDetailFragment;
        }
    }

    public ZhanlanDetailFragment getZhanlanDetailFragment() {
        synchronized (mUILock) {
            if (mZhanlanDetailFragment == null) {
            	ZhanlanDetailFragment zhanlanDetailFragment = new ZhanlanDetailFragment(Sphinx.this);
                zhanlanDetailFragment.setId(R.id.view_discover_zhanlan_detail);
                zhanlanDetailFragment.onCreate(null);
                mZhanlanDetailFragment = zhanlanDetailFragment;
            }
            return mZhanlanDetailFragment;
        }
    }

    public DianyingDetailFragment getDianyingDetailFragment() {
        synchronized (mUILock) {
            if (mDianyingDetailFragment == null) {
                DianyingDetailFragment dianyingDetailFragment = new DianyingDetailFragment(Sphinx.this);
                dianyingDetailFragment.setId(R.id.view_discover_dianying_detail);
                dianyingDetailFragment.onCreate(null);
                mDianyingDetailFragment = dianyingDetailFragment;
            }
            return mDianyingDetailFragment;
        }
    }

    public HotelHomeFragment getHotelHomeFragment() {
        synchronized (mUILock) {
            if (mHotelHomeFragment == null) {
                HotelHomeFragment fragment = new HotelHomeFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_home);
                fragment.onCreate(null);
                mHotelHomeFragment = fragment;
            }
            return mHotelHomeFragment;
        }
    }

    public PickLocationFragment getPickLocationFragment() {
        synchronized (mUILock) {
            if (mPickLocationFragment == null) {
                PickLocationFragment fragment = new PickLocationFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_pick_location);
                fragment.onCreate(null);
                mPickLocationFragment = fragment;
            }
            return mPickLocationFragment;
        }
    }

    public HotelOrderWriteFragment getHotelOrderWriteFragment() {
        synchronized (mUILock) {
            if (mHotelOrderWriteFragment == null) {
                HotelOrderWriteFragment fragment = new HotelOrderWriteFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_order_write);
                fragment.onCreate(null);
                mHotelOrderWriteFragment = fragment;
            }
            return mHotelOrderWriteFragment;
        }
    }

    public HotelOrderCreditFragment getHotelOrderCreditFragment() {
        synchronized (mUILock) {
            if (mHotelOrderCreditFragment == null) {
            	HotelOrderCreditFragment fragment = new HotelOrderCreditFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_credit_assure);
                fragment.onCreate(null);
                mHotelOrderCreditFragment = fragment;
            }
            return mHotelOrderCreditFragment;
        }
    }

    public HotelOrderListFragment getHotelOrderListFragment() {
        synchronized (mUILock) {
            if (mHotelOrderListFragment == null) {
            	HotelOrderListFragment fragment = new HotelOrderListFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_order_list);
                fragment.onCreate(null);
                mHotelOrderListFragment = fragment;
            }
            return mHotelOrderListFragment;
        }
    }
    
    public HotelOrderSuccessFragment getHotelOrderSuccessFragment(){

        synchronized (mUILock) {
            if (mHotelOrderSuccessFragment == null) {
            	HotelOrderSuccessFragment fragment = new HotelOrderSuccessFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_order_success);
                fragment.onCreate(null);
                mHotelOrderSuccessFragment = fragment;
            }
            return mHotelOrderSuccessFragment;
        }
    }    

    public HotelOrderDetailFragment getHotelOrderDetailFragment(){

        synchronized (mUILock) {
            if (mHotelOrderDetailFragment == null) {
            	HotelOrderDetailFragment fragment = new HotelOrderDetailFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_order_detail);
                fragment.onCreate(null);
                mHotelOrderDetailFragment = fragment;
            }
            return mHotelOrderDetailFragment;
        }
    }
    
    public HotelOrderDetailFragment getHotelOrderDetailFragmentTwo(){

        synchronized (mUILock) {
            if (mHotelOrderDetailFragment2 == null) {
            	HotelOrderDetailFragment fragment = new HotelOrderDetailFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_order_detail_2);
                fragment.onCreate(null);
                mHotelOrderDetailFragment2 = fragment;
            }
            return mHotelOrderDetailFragment2;
        }
    }

    public CouponListFragment getCouponListFragment(){

        synchronized (mUILock) {
            if (mCouponListFragment == null) {
                CouponListFragment fragment = new CouponListFragment(Sphinx.this);
                fragment.setId(R.id.view_coupon_list);
                fragment.onCreate(null);
                mCouponListFragment = fragment;
            }
            return mCouponListFragment;
        }
    }

    public CouponDetailFragment getCouponDetailFragment(){

        synchronized (mUILock) {
            if (mCouponDetailFragment == null) {
                CouponDetailFragment fragment = new CouponDetailFragment(Sphinx.this);
                fragment.setId(R.id.view_coupon_detail);
                fragment.onCreate(null);
                mCouponDetailFragment = fragment;
            }
            return mCouponDetailFragment;
        }
    }

    public MeasureDistanceFragment getMeasureDistanceFragment(){

        synchronized (mUILock) {
            if (mMeasureDistanceFragment == null) {
                MeasureDistanceFragment fragment = new MeasureDistanceFragment(Sphinx.this);
                fragment.setId(R.id.view_measure_distance);
                fragment.onCreate(null);
                mMeasureDistanceFragment = fragment;
            }
            return mMeasureDistanceFragment;
        }
    }

    // TODO: get fragment end

    // TODO: my location begin
    private boolean mFirstStartup = false;
    private boolean mUpgrade = false;
    private MyLocation mMyLocation;
    private Position mMyPosition;
    private String mMyName;
    private ItemizedOverlay mMyLocationOverlay;
    private Circle mMyLocationCircle;
    private Runnable mUpdateRunnable = new Runnable() {
        
        @Override
        public void run() {
            updateMyLocationOverlay();
        }
    };
    private Runnable mLocationChangedRun = new Runnable() {

        @Override
        public void run() {

            CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
            Position myPosition = null;
            String name = null;
            if (myLocationCityInfo != null) {
                myPosition = myLocationCityInfo.getPosition();
                name = mMapEngine.getPositionName(myPosition);
                if (name == null) {
                    name = myLocationCityInfo.getCName();
                }
            }

            if (myPosition != null && name != null) {
                if (myPosition.equals(mMyPosition) && name.equals(mMyName)) {
                    return;
                }
            } else if (myPosition != null && name == null) {
                if (myPosition.equals(mMyPosition) && mMyName == null) {
                    return;
                }
            } else if (myPosition == null) {
                if (mMyPosition == null) {
                    return;
                }
            }

            mMyPosition = myPosition;
            mMyName = name;
            if (mMyPosition != null && mMyName != null) {
                try {
                    mMyLocation.setPosition(mMyPosition);
                    mMyLocation.setMessage(mMyName);
                } catch (APIException e) {
                    e.printStackTrace();
                }
            }

            updateMyLocation();
        }
    };

    public void updateMyLocation() {
        updateMyLocationOverlay();

        Position myPosition = mMyPosition;
        if (myPosition != null) {
            if (MyLocation.MODE_NONE == mMyLocation.mode) {
                updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                mMapView.setZoomLevel(TKConfig.ZOOM_LEVEL_LOCATION);
                mMapView.panToPosition(myPosition);
                resetShowInPreferZoom();
            } else if (MyLocation.MODE_NAVIGATION == mMyLocation.mode || MyLocation.MODE_ROTATION == mMyLocation.mode) {
                mMapView.panToPosition(myPosition);
            }
        } else {
            resetLoactionButtonState();
        }

    }

    private boolean mActivityResult = false;
    private boolean mOnPause = true;
    public boolean isOnPause() {
        return mOnPause;
    }

    protected boolean mSensorOrientation = false;
    private SensorManager mSensorManager=null;
    private Sensor mSensor;
    private float rotateZ = 365;
    private SensorEventListener mSensorListener=new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(mMapView!=null
                    && mMapView.isStopRefreshMyLocation() == false
                    && Math.abs(rotateZ-event.values[0]) >= 3){
                rotateZ = event.values[0];
                mMapView.rotateLocationZToDegree(-rotateZ);
                if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                    mMapView.rotateZToDegree(-rotateZ);
                }
            }
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private boolean mRequestLocation = false;
    private Runnable mLocationResponseRun = new Runnable() {

        @Override
        public void run() {
            mRequestLocation = false;
        }
    };

    private boolean updateMyLocationOverlay(){
        try {
            Position myLocation = mMyPosition;
            if(myLocation==null) {
                mMapView.deleteOverlaysByName(ItemizedOverlay.MY_LOCATION_OVERLAY);
                mMyLocationCircle.setVisible(false);
                mMapView.refreshMap();

                InfoWindowFragment fragment = getInfoWindowFragment();
                if (fragment == mBottomFragment && fragment.getItemizedOverlay() == mMyLocationOverlay) {
                    fragment.setData(fragment.getOwerFragmentId(), mMyLocationOverlay, fragment.mActionTag);
                }

            } else {
                if (mMapView.getOverlaysByName(ItemizedOverlay.MY_LOCATION_OVERLAY) == null) {
                    mMapView.addOverlay(mMyLocationOverlay);
                }

                boolean isVisible = mMyLocationCircle.isVisible() && (System.currentTimeMillis() - mOnCreateTimeMillis > 3000);
                mMyLocationCircle.setPosition(myLocation);
                mMyLocationCircle.setRadius(new Length(isVisible ? myLocation.accuracy : 0, UOM.M));
                mMyLocationCircle.setVisible(true);
                if (isVisible == false) {
                    mHandler.postDelayed(mUpdateRunnable, 3000);
                }

                POI poi = (POI) mMyLocation.getAssociatedObject();
                poi.setPosition(myLocation);

                if (mRequestLocation) {
                    mRequestLocation = false;
                    if (uiStackPeek() == R.id.view_home) {
                        showInfoWindow(R.id.view_home, mMyLocation);
                    }
                } else {

                    InfoWindowFragment fragment = getInfoWindowFragment();
                    if (fragment == mBottomFragment && fragment.getItemizedOverlay() == mMyLocationOverlay) {
                        fragment.setData(fragment.getOwerFragmentId(), mMyLocationOverlay, fragment.mActionTag);
                    }
                }

                mMapView.refreshMap();
            }

            return true;
        } catch (APIException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void requestLocation() {
        Position position = mMyPosition;
        if (position == null) {
            updateLoactionButtonState(MyLocation.MODE_NONE);
            showTip(R.string.location_waiting, 3000);
            mRequestLocation = true;
            mHandler.removeCallbacks(mLocationResponseRun);
            mHandler.postDelayed(mLocationResponseRun, 20000);
        } else {
            try {
                if (mMyLocation.mode == MyLocation.MODE_NONE || mMyLocation.mode == MyLocation.MODE_NORMAL) {
                    updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                    int zoomLevel = (int)mMapView.getZoomLevel();
                    if(zoomLevel < TKConfig.ZOOM_LEVEL_LOCATION) {
                    	mMapView.setZoomLevel(TKConfig.ZOOM_LEVEL_LOCATION);
                    	mMapView.panToPosition(position);
                    }
                    else {
                    	mMapView.panToPosition(position);
                    }
                    resetShowInPreferZoom();
                } else if (mMyLocation.mode == MyLocation.MODE_NAVIGATION && mSensorOrientation) {
                    updateLoactionButtonState(MyLocation.MODE_ROTATION);
                    mMapView.centerOnPosition(position);
                } else if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                    updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                    mMapView.centerOnPosition(position);
                }
                mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, true);
            } catch (APIException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateLoactionButtonState(int locationButtonState) {
        mMyLocation.mode = locationButtonState;
        Compass compass = mMapView.getCompass();
        if (mMyLocation.mode == MyLocation.MODE_NONE) {
            compass.setVisible(false);
            mMapView.refreshMap();

            if (uiStackSize() > 0)
                mCompassView.setVisibility(View.VISIBLE);
            mLocationBtn.setImageResource(R.drawable.progress_location);
            mLocationTxv.setText(R.string.location_text_doing);
            Animatable animationDrawable = (Animatable)(mLocationBtn.getDrawable());
            animationDrawable.start();
        } else {
            int resid;
            int text;
            Drawable animationDrawable = mLocationBtn.getDrawable();
            if (animationDrawable != null && animationDrawable instanceof AnimationDrawable) {
                ((AnimationDrawable)animationDrawable).stop();
            }
            mLocationBtn.setImageDrawable(null);

            if (mMyLocation.mode == MyLocation.MODE_NAVIGATION) {
                compass.setVisible(false);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.VISIBLE);
                rotateZ = 365;
                mMapView.rotateZToDegree(0);
                resid = R.drawable.ic_location_navigation;
                text = R.string.location_text_navigation;

                if (uiStackPeek() == R.id.view_home) {
                    showInfoWindow(R.id.view_home, mMyLocation);
                }
            } else if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                compass.setVisible(true);
                mMapView.refreshMap();

                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.INVISIBLE);
                rotateZ = 365;
                resid = R.drawable.ic_location_rotation;
                text = R.string.location_text_compass;

                if (uiStackPeek() == R.id.view_home) {
                    showInfoWindow(R.id.view_home, mMyLocation);
                }
            } else {
                compass.setVisible(false);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.VISIBLE);
                resid = R.drawable.ic_location_normal;
                text = R.string.location_text;
                rotateZ = 365;
                mMapView.rotateZToDegree(0);
            }
            mLocationBtn.setImageResource(resid);
            mLocationTxv.setText(text);
        }
    }

    public void resetLoactionButtonState() {
        if (mMyLocation.mode != MyLocation.MODE_NONE && mMyLocation.mode != MyLocation.MODE_NORMAL) {
            updateLoactionButtonState(MyLocation.MODE_NORMAL);
        }
        rotateZ = 365;
        mMapView.rotateZToDegree(0);
    }
    // TODO: my location end

    // TODO: upload app list start

    private boolean checkUploadApp(){
    	String time_s = TKConfig.getPref(mThis, TKConfig.PREFS_LAST_UPLOAD_APPLIST, "0");
    	long time_l = Long.parseLong(time_s);
    	long curTime = CalendarUtil.getExactTime(mThis);
    	if(curTime < time_l){
    		TKConfig.setPref(mThis, TKConfig.PREFS_LAST_UPLOAD_APPLIST, String.valueOf(curTime));
    		return false;
    	}else if(curTime - time_l > 86400 * 7 * 1000){
    		return true;
    	}else{
    		return false;
    	}
    }

    private void submitUploadApp(){
    	PackageManager manager = mThis.getPackageManager();
    	List <PackageInfo> pkgList = manager.getInstalledPackages(0);
    	StringBuilder s = new StringBuilder();
    	for (int i = 0; i < pkgList.size(); i++){
    		PackageInfo pI = pkgList.get(i);
    		if(!pI.packageName.contains("com.android") && !pI.packageName.contains("com.google.android")){
    			//LogWrapper.d("Trap", pI.packageName + "__" + manager.getApplicationLabel(pI.applicationInfo).toString() + "__" + pI.versionName + "__" + CalendarUtil.ymd8h.format(pI.firstInstallTime) + "__" + CalendarUtil.ymd8h.format(pI.lastUpdateTime) + "__" + String.valueOf(pI.applicationInfo.flags & 447));
    			s.append(Utility.joinFields("~",
    					safePlainTextEncode(pI.packageName),
    					safePlainTextEncode(manager.getApplicationLabel(pI.applicationInfo).toString()),
    					String.valueOf(pI.versionCode),
    					safePlainTextEncode(pI.versionName),
    					(Build.VERSION.SDK_INT >= 9) ? String.valueOf(pI.firstInstallTime) : "null",
    					(Build.VERSION.SDK_INT >= 9) ? String.valueOf(pI.lastUpdateTime) : "null",
    					String.valueOf(pI.applicationInfo.flags)
    					));
    			s.append(';');
    		}
    	}
        FeedbackUpload feedbackUpload = new FeedbackUpload(mThis);
        feedbackUpload.addParameter(FeedbackUpload.SERVER_PARAMETER_APPLIST, s.toString());
        queryStart(feedbackUpload);
    }

    private String safePlainTextEncode(String source){
    	String newString = Utility.plainTextEncode(source, ";", "~");
    	if(TextUtils.isEmpty(newString)){
    		return "null";
    	}else{
    		return newString;
    	}
    }
    // TODO: upload app list end

    private void refreshZoomView() {
        if (TKConfig.isPref(mThis, TKConfig.PREFS_SHOW_ZOOM_BUTTON)) {
            mZoomView.setVisibility(View.GONE);
        } else {
            mZoomView.setVisibility(View.VISIBLE);
        }
    }

    public View getLocationView() {
        return mLocationView;
    }

    public View getMapToolsView() {
        return mMapToolsView;
    }

    public View getMapCleanBtn() {
        return mMapCleanBtn;
    }

    public void setMapViewPaddingBottom(int bottom) {
        Rect rect = mMapView.getPadding();
        if (rect.bottom != bottom) {
            rect.bottom = bottom;
            mMapView.refreshMap();
        }
    }

    private boolean infoWindowBackInHome(String overlayName) {

        boolean result = false;

        BaseFragment bottomfragment = mBottomFragment;
        InfoWindowFragment infoWindowFragment = getInfoWindowFragment();
        HomeFragment homeFragment = getHomeFragment();

        int id = uiStackPeek();
        if (id == R.id.view_home
                && bottomfragment != null
                && bottomfragment == infoWindowFragment
                && overlayName.equals(infoWindowFragment.getItemizedOverlay().getName())) {

            result = true;
            homeFragment.mBottomFragment = getHomeBottomFragment();
            replaceBottomUI(homeFragment);

            if (ItemizedOverlay.MY_LOCATION_OVERLAY.equals(overlayName) == false) {
                mMapView.deleteOverlaysByName(overlayName);
            }
        }

        return result;
    }

    private boolean infoWindowBackInResultMap(String overlayName) {

        boolean result = false;
        InfoWindowFragment infoWindowFragment = getInfoWindowFragment();

        int id = uiStackPeek();
        if (id == R.id.view_result_map
                && overlayName.equals(infoWindowFragment.getItemizedOverlay().getName())) {
            ItemizedOverlay itemizedOverlay = mMapView.deleteOverlaysByName(overlayName);
            if (itemizedOverlay != null) {
                result = true;
                mMapView.refreshMap();
                itemizedOverlay = mMapView.getCurrentOverlay();
                if (itemizedOverlay != null) {
                    showInfoWindow(R.id.view_result_map, itemizedOverlay.getItemByFocused());
                }
            }
        }

        return result;
    }

    public View getCenterTokenView() {
        return mCenterTokenView;
    }

    public POI getPOI(Position position, String defaultName) {

        POI poi = new POI();
        String name = mMapEngine.getPositionName(position);
        if (name == null) {
            name = defaultName;
        }
        poi.setName(name);
        poi.setPosition(position);

        return poi;
    }

    /**
     * 指定点是否在屏幕显示可视区域内
     * @return
     */
    private boolean positionInScreen(Position position) {
        boolean result = false;

        try {
            float zoomLevel = mMapView.getZoomLevel();
            XYFloat xyFloat = mMapView.mercXYToScreenXYConv(Util.posToMercPix(position, zoomLevel), zoomLevel);
            Rect padding = mMapView.getPadding();
            DisplayMetrics displayMetrics = Globals.g_metrics;
            if (xyFloat.x >= padding.left
                    && xyFloat.x <= displayMetrics.widthPixels - padding.right
                    && xyFloat.y >= padding.top
                    && xyFloat.y <= displayMetrics.heightPixels - padding.bottom) {

                result = true;
            }
        } catch (APIException e) {
            e.printStackTrace();
        }

        return result;
    }
    
    /**
     * 创建一个DataQuery，添加cx,cy,lx,cy参数
     * @param poi
     * @return
     */
    public DataQuery buildDataQuery() {
        return buildDataQuery(null);
    }
    
    /**
     * 创建一个DataQuery，添加x,y,cx,cy,lx,cy参数
     * @param poi
     * @return
     */
    public DataQuery buildDataQuery(POI poi) {
        DataQuery dataQuery = new DataQuery(mThis);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, String.valueOf(0));
        if (poi == null) {
            poi = addCeterPositionParameter(dataQuery);
        } else if (poi.getSourceType() == POI.SOURCE_TYPE_MY_LOCATION){
            poi = poi.clone();
            poi.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
            addMyLocationParameter(dataQuery, poi.getPosition());
        } else {
            if (poi.getUUID() != null) {
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
            }
            Position position = poi.getPosition();
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
        dataQuery.setPOI(poi);
        dataQuery.setCityId(MapEngine.getCityId(poi.getPosition()));
        return dataQuery;
    }
    
    private POI addCeterPositionParameter(DataQuery dataQuery) {
        POI poi = new POI();
        // 我的定位显示在屏幕可视区域，并且比例尺小于或等于1千米时，则请求中包含lx和ly且不包含cx和cy，否则请求参数中包含cx和cy
        Position position = null;
        CityInfo cityInfo = Globals.g_My_Location_City_Info;
        if(cityInfo != null){
            position = cityInfo.getPosition();
        }
        float zoomLevel = mMapView.getZoomLevel();
        if (position != null) {
            addMyLocationParameter(dataQuery, position);

            if (positionInScreen(position) &&
                zoomLevel >= 12) { // 12级别是1千米

                poi.setName(getString(R.string.my_location));
                poi.setPosition(position);
                poi.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);

                return poi;         
            }
        }
        position = mMapView.getCenterPosition();
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CENTER_LONGITUDE, String.valueOf(position.getLon()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_CENTER_LATITUDE, String.valueOf(position.getLat()));

        poi.setName(getString(R.string.map_center));
        poi.setPosition(position);
        poi.setSourceType(POI.SOURCE_TYPE_MAP_CENTER);

        return poi;
    }
    
    private void addMyLocationParameter(DataQuery dataQuery, Position position) {
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_LATITUDE, String.valueOf(position.getLat()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_LONGITUDE, String.valueOf(position.getLon()));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_CITY, String.valueOf(MapEngine.getCityId(position)));
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_LOCATION_TYPE, String.valueOf(position.getType()));
    }
}

/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows;

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
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.TextUtils;
import android.view.Display;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.tigerknows.R;

import com.decarta.Globals;
import com.decarta.android.event.EventRegistry;
import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.exception.APIException;
import com.decarta.android.map.Circle;
import com.decarta.android.map.Compass;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
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
import com.tigerknows.common.LogUpload;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.map.PinOverlayHelper;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.map.MapView.DownloadEventListener;
import com.tigerknows.map.MapView.MapScene;
import com.tigerknows.map.MapView.SnapMap;
import com.tigerknows.map.label.Label;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.NoticeQuery.NoticeResultResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.NoticeQuery;
import com.tigerknows.model.PullMessage;
import com.tigerknows.model.PullMessage.Message.PulledProductMessage;
import com.tigerknows.model.Response;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Bootstrap;
import com.tigerknows.model.BootstrapModel;
import com.tigerknows.model.User;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.radar.TKNotificationManager;
import com.tigerknows.service.MapDownloadService;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.service.SuggestLexiconService;
import com.tigerknows.share.TKWeixin;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.BrowserFragment;
import com.tigerknows.ui.HintActivity;
import com.tigerknows.ui.MeasureDistanceFragment;
import com.tigerknows.ui.MenuFragment;
import com.tigerknows.ui.ResultMapFragment;
import com.tigerknows.ui.TakeScreenshotActivity;
import com.tigerknows.ui.TitleFragment;
import com.tigerknows.ui.discover.DianyingDetailFragment;
import com.tigerknows.ui.discover.DiscoverChildListFragment;
import com.tigerknows.ui.discover.DiscoverHomeFragment;
import com.tigerknows.ui.discover.DiscoverListFragment;
import com.tigerknows.ui.discover.TuangouDetailFragment;
import com.tigerknows.ui.discover.YanchuDetailFragment;
import com.tigerknows.ui.discover.ZhanlanDetailFragment;
import com.tigerknows.ui.hotel.HotelHomeFragment;
import com.tigerknows.ui.hotel.HotelImageGridFragment;
import com.tigerknows.ui.hotel.HotelIntroActivity;
import com.tigerknows.ui.hotel.HotelOrderCreditFragment;
import com.tigerknows.ui.hotel.HotelOrderDetailFragment;
import com.tigerknows.ui.hotel.HotelOrderListFragment;
import com.tigerknows.ui.hotel.HotelOrderWriteFragment;
import com.tigerknows.ui.hotel.PickLocationFragment;
import com.tigerknows.ui.more.AboutUsActivity;
import com.tigerknows.ui.more.AddMerchantActivity;
import com.tigerknows.ui.more.AppRecommendActivity;
import com.tigerknows.ui.more.ChangeCityActivity;
import com.tigerknows.ui.more.FavoriteFragment;
import com.tigerknows.ui.more.FeedbackActivity;
import com.tigerknows.ui.more.GoCommentFragment;
import com.tigerknows.ui.more.HelpActivity;
import com.tigerknows.ui.more.HistoryFragment;
import com.tigerknows.ui.more.MapDownloadActivity;
import com.tigerknows.ui.more.MoreHomeFragment;
import com.tigerknows.ui.more.MyOrderFragment;
import com.tigerknows.ui.more.SatisfyRateActivity;
import com.tigerknows.ui.more.SettingActivity;
import com.tigerknows.ui.poi.CommentListActivity;
import com.tigerknows.ui.poi.CouponDetailFragment;
import com.tigerknows.ui.poi.CouponListFragment;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.ui.poi.POIReportErrorActivity;
import com.tigerknows.ui.poi.POIHomeFragment;
import com.tigerknows.ui.poi.POIDetailFragment;
import com.tigerknows.ui.poi.NearbySearchFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.poi.POIResultFragment;
import com.tigerknows.ui.traffic.BuslineDetailFragment;
import com.tigerknows.ui.traffic.BuslineResultLineFragment;
import com.tigerknows.ui.traffic.BuslineResultStationFragment;
import com.tigerknows.ui.traffic.FetchFavoriteFragment;
import com.tigerknows.ui.traffic.SubwayMapFragment;
import com.tigerknows.ui.traffic.TrafficCompassActivity;
import com.tigerknows.ui.traffic.TrafficDetailFragment;
import com.tigerknows.ui.traffic.TrafficQueryFragment;
import com.tigerknows.ui.traffic.TrafficReportErrorActivity;
import com.tigerknows.ui.traffic.TrafficResultFragment;
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
     * 第一次打开本应用时会发送的广播的Action
     */
    public static final String ACTION_FIRST_STARTUP = "action.com.tigerknows.first.startup";

    /**
     * 请求当前位置的快照
     */
    static final int SNAP_TYPE_MYLOCATION = 0;
    
    /**
     * 请求搜索结果POI的快照
     */
    static final int SNAP_TYPE_QUERY_POI = 3;
    
	public static final int CONFIG_SERVER_CODE = 14;
	public static final String REFRESH_POI_DETAIL = "refresh_poi_detail";

	private MapView mMapView;
	private CityInfo mInitCityInfo;
    private TextView mDownloadView;
    private View mCompassView;
	private LinearLayout mZoomView;
    private View mLocationView=null;
    private ImageButton mLocationBtn=null;
    private TextView mLocationTxv=null;
    private View mPreviousNextView=null;
    private ImageButton mPreviousBtn=null;
    private ImageButton mNextBtn=null;
    private int mTitleViewHeight;
    private int mMenuViewHeiht;
    private int mCityViewHeight;
    
    private ImageButton mMoreBtn;
    private ImageView mMoreImv;
    
	private ViewGroup mDragHintView;
	
	private TouchMode touchMode=TouchMode.NORMAL;
	public enum TouchMode{
		NORMAL, CHOOSE_ROUTING_START_POINT, CHOOSE_ROUTING_END_POINT, LONG_CLICK, MEASURE_DISTANCE;
	}
	
	// Handler message code
	public static int PREVIOUS_NEXT_HIDE = 0x01;
	public static int PREVIOUS_NEXT_SHOW = 0x02;
	public static int LOCATION_CLICKED = 0x03;
	public static int ZOOM_CLICKED = 0x04;
	public static int MAP_TOUCH_DOWN = 0x05;
	public static int MAP_TOUCH = 0x06;
	public static int MAP_MOVE = 0x07;
	public static int MAP_ZOOMEND = 0x08;
	
	public static int ROOT_VIEW_INVALIDATE = 0x09;
	public static int CENTER_SHOW_FOCUSED_OVERLAYITEM=0x10;
	public static int ADJUST_SHOW_FOCUSED_OVERLAYITEM = 0x11;

    public static int DOWNLOAD_SHOW = 0x12;
    public static int DOWNLOAD_HIDE = 0x13;
    public static int DOWNLOAD_ERROR = 0x14;

    private static final String TAG = "Sphinx";
    private static final int REQUEST_CODE_LOCATION_SETTINGS = 15;
    
    private ViewGroup mRootView;
    private ViewGroup mTitleView;
    private ViewGroup mBodyView;
    private ViewGroup mMenuView;
    private ViewGroup mControlView;   
    private ViewGroup mDisableTouchView;

    /**
     * 下面这些ViewGroup用于InfoWindow
     * 最初是想避免重新创建它们，但是重用时会出现文字不对齐、高宽计算不协调情况？
     */
    private ViewGroup mInfoWindowPOI = null;
    private ViewGroup mInfoWindowLongClick = null;
    private ViewGroup mInfoWindowTuangouList = null;
    private ViewGroup mInfoWindowTuangouDetail = null;
    private ViewGroup mInfoWindowYanchuList = null;
    private ViewGroup mInfoWindowHotel = null;
    private ViewGroup mInfoWindowMessage = null;
    
    private Dialog mDialog = null;
    public void setDialog(Dialog dialog) {
        mDialog = dialog;
    }
    
    View.OnTouchListener mInfoWindowBodyViewListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, android.view.MotionEvent ev) {
            if(ev.getAction()==MotionEvent.ACTION_UP) {
                infoWindowClicked();
            }
            return true;
        }
    };
    
    class InfoWindowTrafficButtonListener implements View.OnTouchListener {
        
        POI poi;
        String actionLog;
        
        @Override
        public boolean onTouch(View v,android.view.MotionEvent ev) {
            if(ev.getAction()==MotionEvent.ACTION_UP){
                mActionLog.addAction(actionLog);
                Utility.queryTraffic(Sphinx.this, poi, ActionLog.Map);
            }
            return true;
        }
    }
    
    InfoWindowTrafficButtonListener mInfoWindowTrafficButtonListener = new InfoWindowTrafficButtonListener();
    
    Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mHandler.removeCallbacks(mActualLoadedDrawableRun);
            mHandler.post(mActualLoadedDrawableRun);
        }
    };
    
    Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
        	InfoWindow infoWindow = mMapView.getInfoWindow();
            OverlayItem overlayItem = infoWindow.getAssociatedOverlayItem();
              if (infoWindow.isVisible() && overlayItem != null) {
            	  showInfoWindow(overlayItem);
              }
        }
    };

    
    class InfoWindowLongListener implements View.OnTouchListener {
        
        POI poi;
        String actionLog;
        int index;
        
        @Override
        public boolean onTouch(View v,android.view.MotionEvent ev) {
            if(ev.getAction()==MotionEvent.ACTION_UP){
                mActionLog.addAction(actionLog);
                getTrafficQueryFragment().setDataForLongClick(poi, index);
            }
            return true;
        }
    }
    InfoWindowLongListener mInfoWindowLongStartListener = new InfoWindowLongListener();
    InfoWindowLongListener mInfoWindowLongEndListener = new InfoWindowLongListener();
    
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
    
    // 老虎动画时间
    private static final int LOGO_ANIMATION_TIME = 1000;

    private Context mContext;
    
    private Bundle mBundle = null;
    
    public Bundle getBundle() {
    	return mBundle;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BaseQuery.sClentStatus = BaseQuery.CLIENT_STATUS_START;
//        Debug.startMethodTracing("spinxTracing");
        
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (mMapView == null) {
                    return;
                }
                if (msg.what == PREVIOUS_NEXT_HIDE) {
                    mPreviousNextView.setVisibility(View.INVISIBLE);
                } else if (msg.what == PREVIOUS_NEXT_SHOW) {
                    mPreviousNextView.setVisibility(View.VISIBLE);
                } else if (msg.what == LOCATION_CLICKED) {
                    onMapTouch(false);
                } else if (msg.what == ZOOM_CLICKED) {
                    onMapTouch(false);
                } else if (msg.what == MAP_TOUCH_DOWN) {
                    onMapTouch(true);
                } else if (msg.what == MAP_MOVE) {
                    onMapCenterChanged();
                } else if (msg.what == MAP_ZOOMEND) {
                    onMapCenterChanged();
                } else if (msg.what == DOWNLOAD_SHOW) {
                    mDownloadView.setVisibility(View.VISIBLE); 
                } else if (msg.what == DOWNLOAD_HIDE) {
                    mDownloadView.setVisibility(View.GONE);
                } else if (msg.what == DOWNLOAD_ERROR) {
                    int id = Sphinx.this.uiStackPeek();
                    if (id == R.id.view_traffic_home ||
                            id == R.id.view_result_map ||
                            id == R.id.view_measure_distance) {
                        Toast.makeText(Sphinx.this, R.string.network_failed, Toast.LENGTH_LONG).show();
                    }
                } 
                
                if (msg.what == ROOT_VIEW_INVALIDATE) {
                    mRootView.invalidate();
                } else if (msg.what == CENTER_SHOW_FOCUSED_OVERLAYITEM) {
                    centerShowCurrentOverlayFocusedItem();
                } else if (msg.what == ADJUST_SHOW_FOCUSED_OVERLAYITEM) {
                    adjustShowCurrentOverlayFocusedItem();
                    mMapView.refreshMap();
                }
                
            }
        };
        
        // 读取屏幕参数，如高宽、密度
        WindowManager winMan=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display display=winMan.getDefaultDisplay();
        display.getMetrics(Globals.g_metrics);
        LogWrapper.i(TAG,"onCreate()"+Globals.g_metrics.density);
//        CONFIG.TILE_SIZE = (int) (256 * Globals.g_metrics.density);
//        if(CONFIG.TILE_SIZE > 256)
//        	CONFIG.TILE_SIZE = 512;
//        else 
//        	CONFIG.TILE_SIZE = 256;
        Label.init(display.getWidth(), display.getHeight());
        
        // 根据屏幕密度计算出地图上显示Shape和OverlayItem内容的Padding
        TKConfig.sMap_Padding = (int)(56*Globals.g_metrics.density);
        TKConfig.readConfig();
        Globals.init(mThis);
        MoreHomeFragment.CurrentDownloadCity = null;
        
        mContext = getBaseContext();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        
        mMapEngine = MapEngine.getInstance();
        try {
            mMapEngine.initMapDataPath(getApplicationContext());
        } catch (APIException exception) {
            exception.printStackTrace();
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        
        CityInfo cityInfo = MapEngine.getCityInfo(MapEngine.CITY_ID_BEIJING);
        if (cityInfo.isAvailably() == false) {
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        
        mActionLog.onCreate();
        
        try{
            setContentView(R.layout.sphinx);
            findViews();
            setListener();

            mMapView.setSphinx(this);
            mMapView.setStopRefreshMyLocation(true);
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
            
            mMyLocationOverlay=new ItemizedOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY);
            mMyLocationOverlay.addOverlayItem(mMyLocation);
            
            mMapView.addOverlay(mMyLocationOverlay);
            
            CityInfo lastCityInfo = Globals.getLastCityInfo(getApplicationContext());
            if(lastCityInfo != null && lastCityInfo.isAvailably()){
                cityInfo = lastCityInfo;
            }
            if (Globals.g_User != null) {
                mActionLog.addAction(ActionLog.LifecycleUserReadSuccess);
            }
            mActionLog.addAction(ActionLog.LifecycleSelectCity, cityInfo.getCName());
            mInitCityInfo = cityInfo;
            changeCity(cityInfo, false);
            ArrayList<Integer> uiStack = null;
            if (savedInstanceState != null) {
                uiStack = savedInstanceState.getIntegerArrayList("uistack");
            }
            LogWrapper.d(TAG, "onCreate() uiStack="+uiStack);

            final boolean fristUse = TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_FIRST_USE));
            checkFromThirdParty(true);
            if (uiStack != null) {
                initView();
            } else {
                mTitleView.setVisibility(View.INVISIBLE);
                mMenuView.setVisibility(View.INVISIBLE);
                final boolean upgrade = TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_UPGRADE));
                mPreventShowChangeMyLocationDialog |= fristUse;
                mPreventShowChangeMyLocationDialog |= upgrade;
                mHandler.postDelayed(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (fristUse) {
                            sendFirstStartupBroadcast();
                            Intent intent = new Intent();
                            intent.putExtra(HelpActivity.APP_FIRST_START, fristUse);
                            showView(R.id.activity_more_help, intent);
                            
                            TKConfig.setPref(mContext, TKConfig.PREFS_FIRST_USE, "1");
                            TKConfig.setPref(mContext, TKConfig.PREFS_UPGRADE, "1");
                            mHandler.postDelayed(new Runnable() {
                                
                                @Override
                                public void run() {
                                    initView();
                                }
                            }, 1000);
                        } else {
                            if (upgrade) {
                                sendFirstStartupBroadcast();
                                Intent intent = new Intent();
                                intent.putExtra(HelpActivity.APP_UPGRADE, upgrade);
                                showView(R.id.activity_more_help, intent);
                                TKConfig.setPref(mContext, TKConfig.PREFS_UPGRADE, "1");
                                mHandler.postDelayed(new Runnable() {
                                    
                                    @Override
                                    public void run() {
                                        initView();
                                    }
                                }, 1000);
                            } else {
                                initView();
                            }
                        }
                    }
                }, (fristUse || upgrade) ? 0 : LOGO_ANIMATION_TIME);
            }
            
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    // 初始化存储历史词的数据库表结构，读取历史词数据
                    int cityId = Globals.getCurrentCityInfo().getId();
                    HistoryWordTable.readHistoryWord(mContext, cityId, HistoryWordTable.TYPE_POI);
                    TrafficQueryFragment.TrafficOnCityChanged(Sphinx.this, cityId);
                    
                    CalendarUtil.initExactTime(mContext);

                    Shangjia.readShangjiaList(Sphinx.this);
                    try {
                        Globals.getImageCache().init(Sphinx.this);
                    } catch (APIException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    Globals.initOptimalAdaptiveScreenSize();
                    do {
                        try {
                            Thread.sleep(LOGO_ANIMATION_TIME*2);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if (isFinishing()) {
                            break;
                        }
                    } while (Globals.g_My_Location_State == Globals.LOCATION_STATE_NONE &&
                            fristUse == false);

                    if (isFinishing() == false) {
                        mHandler.post(mCheckLocationCityRun);
                    }
                }
            }).start();
            
            mHandler.postDelayed(new StartUpDelayRunnable(this), 20000);

            mTKLocationManager.onCreate();
            mLocationListener = new MyLocationListener(this, mLocationChangedRun);
            
            mZoomView.setVisibility(View.INVISIBLE);
            
            ZoomControls zoomControls = mMapView.getZoomControls();
            zoomControls.setOnZoomInClickListener(new OnClickListener(){
                public void onClick(View view){
                    mActionLog.addAction(ActionLog.MapZoomIn);
                    mHandler.sendEmptyMessage(ZOOM_CLICKED);
                    mMapView.zoomIn();
                    resetShowInPreferZoom();
                }
            });
            zoomControls.setOnZoomOutClickListener(new OnClickListener(){
                public void onClick(View view){
                    mActionLog.addAction(ActionLog.MapZoomOut);
                    mHandler.sendEmptyMessage(ZOOM_CLICKED);
                    mMapView.zoomOut();
                    resetShowInPreferZoom();
                }
            });
                        
            // add zoom controller to zoom view
            mZoomView.addView(zoomControls);
            
            mPreviousBtn.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    ItemizedOverlay itemizedOverlay = mMapView.getCurrentOverlay();
                    if (itemizedOverlay != null) {
                        mActionLog.addAction(ActionLog.MapStepUp);
                        itemizedOverlay.switchItem(false);
                        centerShowCurrentOverlayFocusedItem();
                        resetLoactionButtonState();
                        mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
                    }
                }
            });
            
            mNextBtn.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    ItemizedOverlay itemizedOverlay = mMapView.getCurrentOverlay();
                    if (itemizedOverlay != null) {
                        mActionLog.addAction(ActionLog.MapStepDown);
                        itemizedOverlay.switchItem(true);
                        centerShowCurrentOverlayFocusedItem();
                        resetLoactionButtonState();
                        mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
                    }
                }
            });
            
            mLocationView.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mHandler.sendEmptyMessage(LOCATION_CLICKED);
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
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.MOVEEND, new MapView.MoveEndEventListener(){
                @Override
                public void onMoveEndEvent(MapView mapView, Position position) {
                    mActionLog.addAction(ActionLog.MapMove);
                    mHandler.sendEmptyMessage(MAP_MOVE);
                }
            });

            final Runnable downloadViewHideRun = new Runnable() {
                
                @Override
                public void run() {
                    mHandler.sendEmptyMessage(DOWNLOAD_HIDE);
                }
            };
            EventRegistry.addEventListener(mMapView, MapView.EventType.DOWNLOAD, new MapView.DownloadEventListener(){
                @Override
                public void onDownloadEvent(final int state) {
                    if (state == DownloadEventListener.STATE_DOWNLOADING) {
                        mHandler.removeCallbacks(downloadViewHideRun);
                        mHandler.sendEmptyMessage(DOWNLOAD_SHOW);
                    } else if (state == DownloadEventListener.STATE_DOWNLOADED) {
                        mHandler.postDelayed(downloadViewHideRun, 3000);
                    } else {
                        mHandler.sendEmptyMessage(DOWNLOAD_ERROR);
                    }
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.ZOOMEND, new MapView.ZoomEndEventListener(){
                @Override
                public void onZoomEndEvent(MapView mapView, final int newZoomLevel) {
                    mHandler.sendEmptyMessage(MAP_ZOOMEND);
                    runOnUiThread(new Runnable() {
                        
                        @Override
                        public void run() {
                            mMapView.setZoomControlsState(newZoomLevel);
//                            LogWrapper.i("zoom", "zoom end at: " + newZoomLevel);
                        }
                    });
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.ROTATEEND, new MapView.RotateEndEventListener(){
                @Override
                public void onRotateEndEvent(MapView mapView, float rotation) {
                    LogWrapper.i(TAG,"MapView.RotateEndEventListener rotation:"+rotation);
                    if(rotation!=0 && mMapView.getCompass()!=null) {
                        mMapView.refreshMap();
                    }
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.TILTEND, new MapView.TiltEndEventListener(){
                @Override
                public void onTiltEndEvent(MapView mapView, float tilt) {
                    LogWrapper.i(TAG,"MapView.TiltEndEventListener tilt:"+tilt);
                    if(tilt!=0 && mMapView.getCompass()!=null) {
                        mMapView.refreshMap();
                    }
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.TOUCHDOWN, new MapView.TouchDownEventListener() {
                
                @Override
                public void onTouchDownEvent(EventSource eventSource, Position pos) {
                    mHandler.sendEmptyMessage(MAP_TOUCH_DOWN);
                    resetLoactionButtonState();
                }
            });
            
            /*
             * 此为TOUCH UP事件
             */
            EventRegistry.addEventListener(mMapView, EventType.TOUCH, new MapView.TouchEventListener(){
                @Override
                public void onTouchEvent(EventSource eventSource, Position position) {
//                  viewHandler.sendEmptyMessage(MAP_TOUCH);
                    resetLoactionButtonState();
                    
                    mZoomView.setVisibility(View.VISIBLE);
//                  MapView mapView=(MapView)eventSource;
                    if(touchMode.equals(TouchMode.CHOOSE_ROUTING_END_POINT)
                            || touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT)){

                        String positionName = MapEngine.getPositionName(position, (int)mMapView.getZoomLevel());
                        if (TextUtils.isEmpty(positionName)) {
                            positionName = mContext.getString(R.string.select_has_point);
                        }
                        clearMap();
                        PinOverlayHelper.drawSelectPointOverlay(Sphinx.this, mHandler, mMapView, touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT), positionName, position);
                    } else if(touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
                        getMeasureDistanceFragment().addPoint(position, false);
                    }
                }
            });
            
            EventRegistry.addEventListener(mMapView.getInfoWindow(), EventType.TOUCH, new InfoWindow.TouchEventListener(){
                @Override
                public void onTouchEvent(EventSource eventSource) {
                    infoWindowClicked();
                }
                
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.DOUBLECLICK, new MapView.DoubleClickEventListener(){
                @Override
                public void onDoubleClickEvent(final MapView mapView, final Position position) {
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
                    if (touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT) || touchMode.equals(TouchMode.CHOOSE_ROUTING_END_POINT)) {
                        String positionName = MapEngine.getPositionName(position, (int)mMapView.getZoomLevel());
                        if (TextUtils.isEmpty(positionName)) {
                            positionName = mContext.getString(R.string.select_has_point);
                        }
                        clearMap();
                        PinOverlayHelper.drawSelectPointOverlay(Sphinx.this, mHandler, mMapView, touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT), positionName, position);
                        return;
                    } else if (touchMode.equals(TouchMode.MEASURE_DISTANCE)){
                        getMeasureDistanceFragment().addPoint(position, false);
                        return;
                    } else if (!touchMode.equals(TouchMode.LONG_CLICK)) {
                        return;
                    }
                    mActionLog.addAction(ActionLog.MapLongClick);
                    String positionName = MapEngine.getPositionName(position, (int)mMapView.getZoomLevel());
                    if (TextUtils.isEmpty(positionName)) {
                        positionName = mContext.getString(R.string.select_has_point);
                    }

                    clearMap();
                    PinOverlayHelper.drawLongClickOverlay(Sphinx.this, mHandler, mMapView, positionName, position);
                }
            });
            
            Compass compass = mMapView.getCompass();
            if(compass!=null){
                compass.setVisible(false);
                compass.addEventListener(EventType.TOUCH, new Compass.TouchEventListener() {
                    @Override
                    public void onTouchEvent(EventSource eventSource) {
                        mMapView.refreshMap();
                    }
                });
            }
            
        }catch(Exception e){
            e.printStackTrace();
            Utility.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }

        List<BaseQuery> list = new ArrayList<BaseQuery>();
        
        LocationQuery locationQuery = LocationQuery.getInstance(mThis);
        list.add(locationQuery);
        
        Bootstrap bootstrap = new Bootstrap(mContext);
        if (mFromThirdParty == THIRD_PARTY_WENXIN_REQUET) {
            bootstrap.addParameter(BaseQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, BaseQuery.REQUSET_SOURCE_TYPE_WEIXIN);
        }
        String versionName = TKConfig.getPref(mThis, TKConfig.PREFS_VERSION_NAME, null);
        if (TextUtils.isEmpty(versionName)) {
            bootstrap.addParameter(Bootstrap.SERVER_PARAMETER_FIRST_LOGIN, Bootstrap.FIRST_LOGIN_NEW);
        } else if (!TKConfig.getClientSoftVersion().equals(versionName)) {
            bootstrap.addParameter(Bootstrap.SERVER_PARAMETER_FIRST_LOGIN, Bootstrap.FIRST_LOGIN_UPGRADE);
        }
        TKConfig.setPref(mContext, TKConfig.PREFS_VERSION_NAME, TKConfig.getClientSoftVersion());
        list.add(bootstrap);
        
        queryStart(list);
        
        initWeibo(false, false);
	}
	
	void resetShowInPreferZoom() {
        ItemizedOverlay overlay = mMapView.getCurrentOverlay();
        if (overlay != null) {
            overlay.isShowInPreferZoom = false;
        }
	}
	
	private void sendFirstStartupBroadcast() {
        Intent intent = new Intent(ACTION_FIRST_STARTUP);
        sendBroadcast(intent);
	}
	
	private void checkCitySupportDiscover(int cityId) {
	    boolean support = DataQuery.checkDiscoveryCity(cityId);
        if (support == false) {
            getMenuFragment().setDiscover(View.GONE);
            return;
        }
	    String discover = TKConfig.getPref(this, TKConfig.PREFS_HINT_DISCOVER_HOME);
	    boolean show = TextUtils.isEmpty(discover);
	    if (show == false) {
	        return;
	    }
        if (support) {
            getMenuFragment().setDiscover(View.VISIBLE);
        } else {
            getMenuFragment().setDiscover(View.GONE);
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
        mMapView.setVisibility(View.VISIBLE);
        if (TKConfig.getPref(mThis, TKConfig.PREFS_MAP_TOOLS) == null) {
            mMoreImv.setVisibility(View.VISIBLE);
        } else {
            mMoreImv.setVisibility(View.GONE);
        }
        mUIStack.clear();
        getTitleFragment();
        getMenuFragment();
        getMoreFragment().refreshMoreData();
        showView(R.id.view_poi_home);
        
        mMenuView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mMenuViewHeiht = mMenuView.getMeasuredHeight();
        mControlView.setPadding(0, 0, 0, mMenuViewHeiht);
        mTitleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mTitleViewHeight = mTitleView.getMeasuredHeight();
        mCityViewHeight = Util.dip2px(Globals.g_metrics.density, 30);
        mMapView.getPadding().top = mTitleViewHeight + mCityViewHeight;

        mTitleView.setVisibility(View.VISIBLE);
        mBodyView.setVisibility(View.VISIBLE);
        mMenuView.setVisibility(View.VISIBLE);
        mCompassView.setVisibility(View.VISIBLE);
        mControlView.setVisibility(View.VISIBLE);
        Sphinx.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Sphinx.this.getWindow().setBackgroundDrawable(null);
        
        checkFromThirdParty(false);
    }

    @Override
	protected Dialog onCreateDialog(int id) {
	    return getDialog(id);
	}
	
	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {
	    super.onPrepareDialog(id, dialog);
	    if (id == R.id.dialog_prompt_change_to_my_location_city) {
	        String currentCityName = Globals.getCurrentCityInfo(false).getCName();
	        String locationCityName;
	        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
	        if (myLocationCityInfo != null) {
	            locationCityName = myLocationCityInfo.getCName();
	        } else {
	            locationCityName = currentCityName;
	        }
	        
	        View view = dialog.findViewById(R.id.alter_change_city_view);
	        
            TextView messageTxv =(TextView)view.findViewById(R.id.message);
            messageTxv.setText(getString(R.string.are_your_change_to_location_city, locationCityName));

            Button button1 = (Button) view.findViewById(R.id.button1);
            Button button2 = (Button) view.findViewById(R.id.button2);

            button1.setText(getString(R.string.prompt_location_return_to_, locationCityName));
            button2.setText(getString(R.string.prompt_location_stay_in_, currentCityName));
            
            mActionLog.addAction(ActionLog.Dialog, getString(R.string.prompt_location));
	    } else if (id == R.id.dialog_prompt_choose_city
	            || id == R.id.dialog_prompt_setting_location_first) {
            TextView messageTxv =(TextView)dialog.findViewById(R.id.message);
            mActionLog.addAction(ActionLog.Dialog, messageTxv.getText());
	    } else if (id == R.id.dialog_prompt_setting_location) {
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
		if (R.id.activity_more_help == requestCode) {
		    if (data != null) {
		        if (data.getBooleanExtra(HelpActivity.APP_FIRST_START, false)) {
                    mPreventShowChangeMyLocationDialog = false;
		            OnSetup();
		        } else if (data.getBooleanExtra(HelpActivity.APP_UPGRADE, false)) {
                    mPreventShowChangeMyLocationDialog = false;
		        }
		    }
		} else if (R.id.activity_more_app_recommend == requestCode) {
        } else if (R.id.activity_more_change_city == requestCode) {
            if (data != null && RESULT_OK == resultCode) {
                mPreventShowChangeMyLocationDialog = true;
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

        }
		
        if (REQUEST_CODE_LOCATION_SETTINGS == requestCode) {
            mHandler.postDelayed(new Runnable() {
                
                @Override
                public void run() {
                    if (isFinishing()) {
                        return;
                    }
                    if (Globals.g_My_Location_City_Info == null) {
                        Toast.makeText(Sphinx.this, mContext.getString(R.string.location_failed), Toast.LENGTH_LONG).show();
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
		LogWrapper.i(TAG,"onResume()");
		mActionLog.onResume();
		mMapView.resume();
		if (mPOIHomeFragment != null && mMapView.isStopRefreshMyLocation() == false) {
		    mMapView.refreshMap();
		}
		interceptTouchEnd();
		
		boolean ensureThreadsRunning = mMapView.ensureThreadRunning();
		// TODO mTrafficQueryFragment实例的成员变量在地图的背景线程被重新生成后会造成丢失的问题
		if (ensureThreadsRunning) {
		    mTrafficQueryFragment = null;
		}

        if (mSensorOrientation) {
            mSensorManager.registerListener(mSensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        Globals.setConnectionFast(Utility.isConnectionFast(this));
        Globals.getAsyncImageLoader().onResume();
        
        TKConfig.updateIMSI(mConnectivityManager);
        
        IntentFilter intentFilter= new IntentFilter(MapStatsService.ACTION_STATS_CURRENT_DOWNLOAD_CITY_COMPLATE);
        registerReceiver(mCountCurrentDownloadCityBroadcastReceiver, intentFilter);
        
        if (mActivityResult == false) {
            if (mDiscoverFragment != null) {
                getDiscoverFragment().resetDataQuery();
            }
        }
        int id = uiStackPeek();
        BaseFragment baseFragment = getFragment(id);
        if (baseFragment != null) {
            baseFragment.onResume();
        }
        
        if (mActivityResult) {
            mActivityResult = false;
            return;
        }
        mPreventShowChangeMyLocationDialog = false;
        if (Globals.g_My_Location_State == Globals.LOCATION_STATE_SHOW_CHANGE_CITY_DIALOG) {
            Globals.g_My_Location_State = Globals.LOCATION_STATE_FIRST_SUCCESS;
        }
        
        checkLocationCity(false);
	}
	
	private void checkLocationCity(boolean tipGps) {
	    CityInfo cityInfo = Globals.getCurrentCityInfo(false);
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        
        if (myLocationCityInfo != null) {
            if (myLocationCityInfo.getId() != cityInfo.getId()) {
                showPromptChangeToMyLocationCityDialog(myLocationCityInfo);
            }
        } else if (tipGps) {
            final boolean gps = Utility.checkGpsStatus(mContext);
            if (!gps) {
                showPromptSettingLocationDialog();
            }
        }
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		BaseQuery.sClentStatus = BaseQuery.CLIENT_STATUS_START;
		mZoomView.setVisibility(View.VISIBLE);
        
        IntentFilter intentFilter= new IntentFilter(MapEngine.ACTION_REMOVE_CITY_MAP_DATA);
        registerReceiver(mRemoveCityMapDataBroadcastReceiver, intentFilter);
	}
	
	@Override
	protected void onDestroy() {
        Globals.getAsyncImageLoader().onDestory();
        mTKLocationManager.onDestroy();
        TKWeixin.getInstance(this).onDestory();
        Globals.getImageCache().stopWritingAndRemoveOldTiles();
        HistoryWordTable.onDestroy();
		LogWrapper.i("Sphinx","onDestroy()");
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
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
	    LogWrapper.d(TAG, "setTouchMode() touchMode="+touchMode);
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
                int cityId = intent.getIntExtra(MapEngine.EXTRA_CITY_ID, MapEngine.CITY_ID_INVALID);
                if (Globals.getCurrentCityInfo().getId()==cityId || mViewedCityInfoList.contains(cityId)) {
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
                BaseQueryTest.showSetResponseCode(mLayoutInflater, this);
                return true;
            case KeyEvent.KEYCODE_BACK:

                mActionLog.addAction(ActionLog.KeyCodeBack);
                BaseFragment baseFragment = getFragment(uiStackPeek());
                if (baseFragment != null && baseFragment.onKeyDown(keyCode, event)) {
                    return true;
                }
                
                if (mMenuView == null || mMenuView.getVisibility() != View.VISIBLE) {
                    return true;
                }
                
                if (mDragHintView.getVisibility() == View.VISIBLE) {
                    hideHomeDragHint();
                	return true;
                }
                
                if (!uiStackBack()) {
                    Utility.showNormalDialog(Sphinx.this,
                            mContext.getString(R.string.prompt), 
                            mContext.getString(R.string.exit_app),
                            new DialogInterface.OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            switch (id) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    BaseFragment baseFragment = getFragment(uiStackPeek());
                                    if (baseFragment != null) {
                                        baseFragment.dismiss();
                                    }
                                    Sphinx.this.finish();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    return true;
                }
                return true;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void findViews() {
    	mRootView = (ViewGroup)findViewById(R.id.root_view);
        mTitleView = (ViewGroup)findViewById(R.id.title_view);
        mBodyView = (ViewGroup)findViewById(R.id.body_view);
        mMenuView = (ViewGroup)findViewById(R.id.menu_view);
        mControlView = (ViewGroup)findViewById(R.id.control_view);
        mZoomView = (LinearLayout) findViewById(R.id.zoomview);
        mMapView = (MapView) findViewById(R.id.mapview);
        mPreviousNextView = findViewById(R.id.previous_next_view);
        mPreviousBtn=(ImageButton)(findViewById(R.id.previous_btn));
        mNextBtn=(ImageButton)(findViewById(R.id.next_btn));
        mLocationView=(findViewById(R.id.location_view));
        mLocationBtn=(ImageButton)(findViewById(R.id.location_btn));
        mLocationTxv=(TextView)(findViewById(R.id.location_txv));
        mDownloadView = (TextView)findViewById(R.id.download_txv);
        mCompassView = findViewById(R.id.compass_imv);
        mDisableTouchView = (ViewGroup) findViewById(R.id.disable_touch_view);
        mDragHintView = (ViewGroup) findViewById(R.id.hint_root_view);
        
        mMoreBtn = (ImageButton)findViewById(R.id.more_btn);
        mMoreImv = (ImageView)findViewById(R.id.more_imv);
    }

    private void setListener() {
        mDisableTouchView.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 拦截所有Touch事件
                return true;
            }
        });
    	
    	mDragHintView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
			    if (event.getAction() == MotionEvent.ACTION_UP) {
			        hideHomeDragHint();
			    }
				return true;
			}
		});
    	
    	mMoreBtn.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mActionLog.addAction(ActionLog.MapMore);
                TKConfig.setPref(mThis, TKConfig.PREFS_MAP_TOOLS, "1");
                mMoreImv.setVisibility(View.GONE);

                View view = mLayoutInflater.inflate(R.layout.alert_map_tools, null, false);
                final Dialog dialog = Utility.getChoiceDialog(mThis, view, R.style.AlterChoiceDialog);
                
                View button1 = view.findViewById(R.id.button1_view);
                View button2 = view.findViewById(R.id.button2_view);
                View button3 = view.findViewById(R.id.button3_view);
                
                View.OnClickListener onClickListener = new OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        int id = v.getId();
                        if (uiStackPeek() == R.id.view_traffic_home) {
                            getTrafficQueryFragment().priorityMyLocation = false;
                        }
                        if (id == R.id.button1_view) {
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
                            
                        } else if (id == R.id.button2_view) {
                            mActionLog.addAction(ActionLog.MapDistance);

                            getMeasureDistanceFragment().setData();
                            getMeasureDistanceFragment().setIndex(0);
                            showView(R.id.view_measure_distance);
                            
                            Toast.makeText(mThis, R.string.measure_distance_tip, Toast.LENGTH_LONG).show();
                        } else if (id == R.id.button3_view) {
                            mActionLog.addAction(ActionLog.MapCompass);
                            
                            showView(R.id.activity_traffic_compass);
                        }
                        dialog.dismiss();
                    }
                };
                button1.setOnClickListener(onClickListener);
                button2.setOnClickListener(onClickListener);
                button3.setOnClickListener(onClickListener);
                
                dialog.show();
                setShowingDialog(dialog);
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
            uiStackClose(new int[]{R.id.view_poi_home});
            showView(R.id.view_poi_home);
            try {
                String uriStr = uri.toString();
                String[] parms = uriStr.substring(uriStr.indexOf("?")+1).split("&");
                String[] parm = parms[0].split("=");
                POI poi = new POI();
                if ("latlon".equals(parm[0])) {
                    Position position = new Position(parm[1]);
                    mMapView.setZoomLevel(TKConfig.ZOOM_LEVEL_LOCATION);
                    mMapView.panToPosition(position);
                    poi.setPosition(position);
                    String name = MapEngine.getPositionName(poi.getPosition(), (int)mMapView.getZoomLevel());
                    if (!TextUtils.isEmpty(name)) {
                        poi.setName(name);
                    } else {
                        poi.setName(mContext.getString(R.string.select_point));
                    }
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
                            DataQuery dataQuery = getPOIQuery(keyword);
                            queryStart(dataQuery);
                            ((POIResultFragment)getFragment(dataQuery.getTargetViewId())).setup();
                            showView(dataQuery.getTargetViewId());
                            query = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (query == false && Util.inChina(poi.getPosition())) {
                    List<POI> list = new ArrayList<POI>();
                    list.add(poi);
                    showPOI(list, 0);
                    getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.POIDetailMap);
                    showView(R.id.view_result_map);
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
            uiStackClose(new int[]{R.id.view_poi_home});
            showView(R.id.view_poi_home);
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
                    LogWrapper.d("Sphinx", "initIntent() lon="+lon);
                    mMapView.centerOnPosition(new Position(lat, lon), TKConfig.ZOOM_LEVEL_LOCATION);
                    poi.setPosition(new Position(lat, lon));
                    uriStr = uriStr.substring(uriStr.indexOf("?")+1);
                    String[] parm = uriStr.split("=");
                    if ("z".equals(parm[0])) {
                        mMapView.setZoomLevel(Integer.parseInt(parm[1]));
                    } else if ("q".equals(parm[0])) {
                        String keyword = parm[1];
                        if (!TextUtils.isEmpty(keyword)) {
                            DataQuery dataQuery = getPOIQuery(keyword);
                            queryStart(dataQuery);
                            ((POIResultFragment)getFragment(dataQuery.getTargetViewId())).setup();
                            showView(dataQuery.getTargetViewId());
                        }
                        query = true;
                    }
                } else {
                    lon = Double.parseDouble(uriStr);
                    poi.setPosition(new Position(lat, lon));
                    mMapView.centerOnPosition(new Position(lat, lon), TKConfig.ZOOM_LEVEL_LOCATION);
                }
                if (query == false && Util.inChina(poi.getPosition())) {
                    String name = MapEngine.getPositionName(poi.getPosition(), (int)mMapView.getZoomLevel());
                    if (!TextUtils.isEmpty(name)) {
                        poi.setName(name);
                    } else {
                        poi.setName(mContext.getString(R.string.select_point));
                    }
                    List<POI> list = new ArrayList<POI>();
                    list.add(poi);
                    showPOI(list, 0);
                    getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.POIDetailMap);
                    showView(R.id.view_result_map); 
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
            uiStackClose(new int[]{R.id.view_poi_home});
            showView(R.id.view_poi_home);
            POI poi = getPOI(true);
            if (poi.getSourceType() == POI.SOURCE_TYPE_MY_LOCATION) {
                poi.setName(MapEngine.getPositionName(poi.getPosition(), (int)mMapView.getZoomLevel()));
                List<POI> list = new ArrayList<POI>();
                list.add(poi);
                showPOI(list, 0);
            } else {
                requestLocation();
            }
            getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.POIDetailMap);
            showView(R.id.view_result_map);
        } else if (type == SNAP_TYPE_QUERY_POI) {
            mFromThirdParty = THIRD_PARTY_SONY_QUERY_POI;
            fromThirdParty = THIRD_PARTY_SONY_QUERY_POI;
            if (onlyCheck) {
                return true;
            }
            uiStackClose(new int[]{R.id.view_poi_home});
            getPOIQueryFragment().reset();
            showView(R.id.view_poi_input_search);
        } else if (intent.getBooleanExtra(EXTRA_WEIXIN, false)) { //   来自微信的调用，为其提供POI数据作为返回
        	mFromThirdParty = THIRD_PARTY_WENXIN_REQUET;
            fromThirdParty = THIRD_PARTY_WENXIN_REQUET;
            if (onlyCheck) {
                return true;
            }
            mActionLog.addAction(ActionLog.LifecycleWeixinRequest);
        	mBundle = intent.getExtras();
            uiStackClose(new int[]{R.id.view_poi_home});
            showView(R.id.view_poi_home);
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
            uiStackClose(new int[]{R.id.view_poi_home});
            showView(R.id.view_poi_home);
            if (uri != null) {
                String uriStr = uri.toString();
                String[] parms = uriStr.substring(uriStr.indexOf("?")+1).split("&");
                if (parms.length >= 2) {
                    String[] keyValue = parms[0].split("=");
                    int cityId = MapEngine.CITY_ID_INVALID;
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
    
    private DataQuery getPOIQuery(String keyword) {
        DataQuery poiQuery = new DataQuery(mContext);
        POI requestPOI = getPOI();
        int cityId = Globals.getCurrentCityInfo().getId();
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SIZE, String.valueOf(TKConfig.getPageSize()));
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_CITY, String.valueOf(cityId));
        Position position = requestPOI.getPosition();
        if (position != null) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
        poiQuery.setup(cityId, uiStackPeek(), getPOIResultFragmentID(), null, false, false, requestPOI);
        return poiQuery;
    }
    
    private Runnable mOnNewIntentStamp = new Runnable() {
        
        @Override
        public void run() {
            getPOIDetailFragment().refreshStamp();
            getPOIResultFragment().refreshStamp();
            getPOIResultFragment2().refreshStamp();
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
                    uiStackClose(new int[]{R.id.view_discover_home});
                    showView(R.id.view_discover_home);
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
                    uiStackClose(new int[]{R.id.view_poi_home});
                    showView(R.id.view_poi_home);
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
        
        LogUpload.uploadLog(mThis, mActionLog, mTKLocationManager.getGPSLocationUpload(), mTKLocationManager.getNetworkLocationUpload());
        
        if (mSensorOrientation) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        
        int id = uiStackPeek();
        BaseFragment baseFragment = getFragment(id);
        if (baseFragment != null) {
            baseFragment.onPause();
        }
        if (mTitleFragment != null) {
            mTitleFragment.dismissPopupWindow();
        }

        if (null != mMapView && null != mMapEngine) {
            Position position = mMapView.getCenterPosition();
            // 地图显示的位置在当前城市范围内才更新最后一次位置信息
            CityInfo cityInfo = Globals.getCurrentCityInfo(false);
            if (cityInfo != null
                    && MapEngine.getCityId(position) == cityInfo.getId()) {
                int zoom = (int)mMapView.getZoomLevel();
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LON, String.valueOf(position.getLon()));
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LAT, String.valueOf(position.getLat()));
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
        super.onStop();
        BaseQuery.sClentStatus = BaseQuery.CLIENT_STATUS_STOP;
        LogWrapper.i(TAG, "onStop");

        if(mMapView != null)
        	mMapView.pause();
        MapEngine.cleanEngineCache();
        System.gc();
        
        mActionLog.onStop();
        unregisterReceiver(mRemoveCityMapDataBroadcastReceiver);
    }
        
    public void snapMapView(SnapMap snapMap, Position position, MapScene mapScene) {
        mMapView.snapMapView(this, snapMap, position, mapScene);
    }
    
    private void initPosition(CityInfo cityInfo) {
    	if(cityInfo == null)
    		return;
    	if (cityInfo.isAvailably()) {
	        Globals.setCurrentCityInfo(cityInfo);
	        Globals.setHotelCityInfo(null);
	        
	        mMapView.centerOnPosition(cityInfo.getPosition(), cityInfo.getLevel(), true);
    	}
    }
    
    public void changeCity(CityInfo cityInfo) {
        changeCity(cityInfo, true);
    }
    
    public void changeCity(CityInfo cityInfo, boolean centerOnPosition) {
        if (cityInfo.isAvailably()) {
            Globals.setCurrentCityInfo(cityInfo);
            Globals.setHotelCityInfo(null);
            
            int cityId = cityInfo.getId();
            if (centerOnPosition) {
                mMapView.centerOnPosition(cityInfo.getPosition(), cityInfo.getLevel(), false);
            }
            updateCityInfo(cityInfo);
            checkCitySupportDiscover(cityId);
            if (!mViewedCityInfoList.contains(cityInfo)) {
                mViewedCityInfoList.add(cityInfo);
            }
            mActionLog.addAction(ActionLog.LifecycleSelectCity, cityInfo.getCName());
            Position position = cityInfo.getPosition();
            TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LON, String.valueOf(position.getLon()));
            TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LAT, String.valueOf(position.getLat()));
            TKConfig.setPref(mContext, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(cityInfo.getLevel()));
            if (mPOIHomeFragment != null) {
                HistoryWordTable.readHistoryWord(mContext, cityId, HistoryWordTable.TYPE_POI);
                TrafficQueryFragment.TrafficOnCityChanged(this, cityId);
            }
            
            Intent service = new Intent(MapStatsService.ACTION_STATS_CURRENT_DOWNLOAD_CITY);
            service.setClass(mThis, MapStatsService.class);
            startService(service);
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
    
    public POI getPOI() {
        return getPOI(false);
    }
    
    public POI getPOI(boolean needPosition) {
        POI poi = new POI();
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null && myLocationCityInfo.getId() == Globals.getCurrentCityInfo().getId()) {
            poi.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
            if (needPosition) {
                poi.setPosition(myLocationCityInfo.getPosition());
            }
            poi.setName(mContext.getString(R.string.my_location));
        } else {
            CityInfo cityInfo = Globals.getCurrentCityInfo();
            poi.setSourceType(POI.SOURCE_TYPE_CITY_CENTER);
            poi.setName(cityInfo.getCName());
        }
        return poi;
    }
    
    public void onMapCenterChanged() {
        CityInfo cityInfo = MapEngine.getCityInfo(mMapView.getCenterCityId());
        if (cityInfo != null) {
            if (!mViewedCityInfoList.contains(cityInfo)) {
                mViewedCityInfoList.add(cityInfo);
            }
        }
        if (uiStackPeek() != R.id.view_traffic_home) {
            return;
        }
        LogWrapper.d(TAG, "onMapCenterChanged()");  
        
    	getTrafficQueryFragment().onMapCenterChanged(cityInfo);
    }
    
    public void onMapTouch(boolean isShowTip) {
    	if (uiStackPeek() != R.id.view_traffic_home) {
            return;
        }
    	getTrafficQueryFragment().onMapTouch(isShowTip);
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
            }
        }
    }
    
    @SuppressWarnings("rawtypes")
    public void showPOI(List dataList, int index) {
        try {
        mMapView.setStopRefreshMyLocation(false);
        clearMap();
        //add pins
        mMapView.deleteOverlaysByName(ItemizedOverlay.POI_OVERLAY);
        mMapView.getInfoWindow().setVisible(false);
        if (dataList == null || dataList.isEmpty()) {
            mMapView.refreshMap();
            return;
        }
        ItemizedOverlay overlay=new ItemizedOverlay(ItemizedOverlay.POI_OVERLAY);
        
        Resources resources = getResources();
        Icon icon = Icon.getIcon(resources, R.drawable.btn_bubble_b_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
        Icon iconA = Icon.getIcon(resources, R.drawable.btn_bubble_a_normal, Icon.OFFSET_LOCATION_CENTER_BOTTOM);
        
        ArrayList<Position> positions = new ArrayList<Position>();
        Position srcreenPosition = null;
        OverlayItem focus = null;
        for(int i=0;i<dataList.size();i++){
            RotationTilt rt=new RotationTilt(RotateReference.SCREEN,TiltReference.SCREEN);
            Object data = dataList.get(i);
            OverlayItem overlayItem = null;
            if (data instanceof POI) {
                POI target = (POI) data;
                positions.add(target.getPosition());
                overlayItem=new OverlayItem(target.getPosition(),target.getResultType() == POIResponse.FIELD_A_POI_LIST ? iconA : icon, target.getName(),rt);
                overlayItem.setAssociatedObject(target);
                overlayItem.setPreferZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                if (index == i) {
                    overlayItem.isFoucsed = true;
                    focus = overlayItem;
                    srcreenPosition = target.getPosition();
                } else {
                    overlayItem.isFoucsed = false;
                }
                overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
                    @Override
                    public void onTouchEvent(EventSource eventSource) {
                        OverlayItem overlayItem=(OverlayItem) eventSource;   
                        showInfoWindow(overlayItem);
                    }
                });
            } else if (data instanceof Dianying || data instanceof Zhanlan || data instanceof Yanchu) {
                POI poi;
                if (data instanceof Zhanlan) {
                    Zhanlan target = (Zhanlan) data;
                    poi = target.getPOI();
                } else if (data instanceof Yanchu) {
                    Yanchu target = (Yanchu) data;
                    poi = target.getPOI();
                } else {
                    Dianying target = (Dianying) data;
                    poi = target.getYingxun().getPOI(POI.SOURCE_TYPE_DIANYING);
                }
                positions.add(poi.getPosition());
                overlayItem=new OverlayItem(poi.getPosition(), icon, poi.getName(),rt);
                overlayItem.setAssociatedObject(data);
                overlayItem.setPreferZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                if (index == i) {
                    overlayItem.isFoucsed = true;
                    focus = overlayItem;
                    srcreenPosition = poi.getPosition();
                } else {
                    overlayItem.isFoucsed = false;
                }
                overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
                    @Override
                    public void onTouchEvent(EventSource eventSource) {
                        OverlayItem overlayItem=(OverlayItem) eventSource;   
                        showInfoWindow(overlayItem);
                    }
                });
            } else if (data instanceof Tuangou) {
                Tuangou target = (Tuangou) data;
                POI poi = target.getPOI();
                positions.add(poi.getPosition());
                overlayItem=new OverlayItem(poi.getPosition(), icon, target.getShortDesc(), rt);
                overlayItem.setAssociatedObject(target);
                overlayItem.setPreferZoomLevel(TKConfig.ZOOM_LEVEL_POI);
                if (index == i) {
                    overlayItem.isFoucsed = true;
                    focus = overlayItem;
                    srcreenPosition = poi.getPosition();
                } else {
                    overlayItem.isFoucsed = false;
                }
                overlayItem.addEventListener(EventType.TOUCH, new OverlayItem.TouchEventListener() {
                    @Override
                    public void onTouchEvent(EventSource eventSource) {
                        OverlayItem overlayItem=(OverlayItem) eventSource;   
                        showInfoWindow(overlayItem);
                    }
                });
            }
            overlay.addOverlayItem(overlayItem);
        }
        mMapView.addOverlay(overlay);
        if (focus != null) {
            showInfoWindow(focus);
        }
        if (dataList.size() > 1) {
            overlay.isShowInPreferZoom = true;
            int screenX = Globals.g_metrics.widthPixels;
            int screenY = Globals.g_metrics.heightPixels;
            int fitZoomLevle = Util.getZoomLevelToFitPositions(screenX, screenY, TKConfig.sMap_Padding, positions, srcreenPosition);
            mMapView.setZoomLevel(fitZoomLevle);
            mMapView.panToPosition(srcreenPosition);
            mPreviousNextView.setVisibility(View.VISIBLE);
        } else {
            mPreviousNextView.setVisibility(View.INVISIBLE);
            mMapView.setZoomLevel(TKConfig.ZOOM_LEVEL_POI);
            mMapView.panToPosition(srcreenPosition);
        }
        } catch (APIException e) {
            e.printStackTrace();
        }
        mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, false);
    }
    
    public void clearMap() {
        // clear pins
        try {
            mMapView.clearMap();
        } catch (APIException e) {
            e.printStackTrace();
        }
        mPreviousNextView.setVisibility(View.INVISIBLE);
    }
    
    public ViewGroup getControlView() {
        return mControlView;
    }
    
    public void interceptTouchEnd() {
        mDisableTouchView.setVisibility(View.GONE);
    }
    
    public void interceptTouchBegin() {
        mDisableTouchView.setVisibility(View.VISIBLE);
    }
    
    public void layoutTopViewPadding(int left, int top, int right, int bottom) {
        mDownloadView.setPadding(left, top, right, bottom);
        mCompassView.setPadding(left, top, right, bottom);
    }
    
    public int getTitleViewHeight() {
        return mTitleViewHeight;
    }

    public int getMenuViewHeiht() {
        return mMenuViewHeiht;
    }

    public int getCityViewHeight() {
        return mCityViewHeight;
    }
    
    private void layoutInfoWindow(View view, int max) {
        view.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width =  view.getMeasuredWidth();
        if (width > max) {
            view.getLayoutParams().width = max;
        } else {
            view.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
        }
    }

    public void showInfoWindow(OverlayItem overlayItem) {
        
        if (overlayItem == null) {
        	return;
        }
        
        if (touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
            if (!(TouchMode.MEASURE_DISTANCE.equals(overlayItem.getAssociatedObject()))) {
                return;
            }
        }
        
        final InfoWindow infoWindow = mMapView.getInfoWindow();
        infoWindow.setAssociatedOverlayItem(overlayItem);
        try {
            infoWindow.setPosition(overlayItem.getPosition());
        } catch (APIException e) {
            e.printStackTrace();
            return;
        }

        Object object = overlayItem.getAssociatedObject();
        if (object instanceof POI) {
            final POI poi = (POI) object;
            int sourceType = poi.getSourceType();
            if (sourceType == POI.SOURCE_TYPE_TUANGOU
                    || sourceType == POI.SOURCE_TYPE_DIANYING
                    || sourceType == POI.SOURCE_TYPE_YANCHU
                    || sourceType == POI.SOURCE_TYPE_ZHANLAN
                    || sourceType == POI.SOURCE_TYPE_FENDIAN
                    || sourceType == POI.SOURCE_TYPE_YINGXUN) {
//                if (mInfoWindowTuangouDetail == null) {
                    mInfoWindowTuangouDetail = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_tuangou_detail, null);
//                }
                
                TextView nameTxv=(TextView)mInfoWindowTuangouDetail.findViewById(R.id.name_txv);
                TextView addressTxv= (TextView)mInfoWindowTuangouDetail.findViewById(R.id.address_txv);
                ImageButton trafficBtn=(ImageButton)mInfoWindowTuangouDetail.findViewById(R.id.traffic_btn);

                nameTxv.setText(poi.getName());
                addressTxv.setText(poi.getAddress());
                int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(96));
                layoutInfoWindow(nameTxv, max);
                layoutInfoWindow(addressTxv, max);
                
                ViewGroup bodyView=(ViewGroup)mInfoWindowTuangouDetail.findViewById(R.id.body_view);
                bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
                
                mInfoWindowTrafficButtonListener.poi = poi;
                mInfoWindowTrafficButtonListener.actionLog = ActionLog.MapInfoWindowTraffic;
                trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
                
                infoWindow.setViewGroup(mInfoWindowTuangouDetail);
            } else if (sourceType == POI.SOURCE_TYPE_LONG_CLICK_SELECT_POINT) {
//                if (mInfoWindowLongClick == null) {
                    mInfoWindowLongClick = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_long_click, null);
//                }
                
                TextView nameTxv=(TextView)mInfoWindowLongClick.findViewById(R.id.name_txv);
                ImageButton startBtn= (ImageButton)mInfoWindowLongClick.findViewById(R.id.start_btn);
                ImageButton endBtn=(ImageButton)mInfoWindowLongClick.findViewById(R.id.end_btn);
                
                nameTxv.setText(poi.getName());
                
                int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(128));
                layoutInfoWindow(nameTxv, max);
                
                ViewGroup bodyView=(ViewGroup)mInfoWindowLongClick.findViewById(R.id.body_view);
                bodyView.setOnTouchListener(mInfoWindowBodyViewListener);

                mInfoWindowLongStartListener.actionLog = ActionLog.MapInfoWindowStart;
                mInfoWindowLongStartListener.poi = poi;
                mInfoWindowLongStartListener.index = TrafficQueryFragment.START;
                startBtn.setOnTouchListener(mInfoWindowLongStartListener);
                
                mInfoWindowLongEndListener.actionLog = ActionLog.MapInfoWindowEnd;
                mInfoWindowLongEndListener.poi = poi;
                mInfoWindowLongEndListener.index = TrafficQueryFragment.END;
                endBtn.setOnTouchListener(mInfoWindowLongEndListener);
                
                infoWindow.setViewGroup(mInfoWindowLongClick);
            } else if (poi.getSourceType() == POI.SOURCE_TYPE_HOTEL) {
//              if (mInfoWindowHotel == null) {
                mInfoWindowHotel = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_hotel, null);
//            }
                ImageView pictureImv = (ImageView)mInfoWindowHotel.findViewById(R.id.picture_imv);
                TextView nameTxv=(TextView)mInfoWindowHotel.findViewById(R.id.name_txv);
                TextView canReserveTxv = (TextView) mInfoWindowHotel.findViewById(R.id.can_reserve_txv);
                TextView priceTxv = (TextView) mInfoWindowHotel.findViewById(R.id.price_txv);
                ImageButton trafficBtn=(ImageButton)mInfoWindowHotel.findViewById(R.id.traffic_btn);

                Hotel hotel = poi.getHotel();
                nameTxv.setText(poi.getName());
                priceTxv.setText(poi.getPrice());
                
                int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(188));
                layoutInfoWindow(nameTxv, max);
                TKDrawable tkDrawable = hotel.getImageThumb();
                if (tkDrawable != null) {
                    Drawable drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, getResultMapFragment().toString());
                    Drawable imageInfoWindow = hotel.getImageInfoWindow(); 
                    if(drawable != null) {
                        if (imageInfoWindow == null) {
                            BitmapDrawable bm = (BitmapDrawable) drawable;
                            imageInfoWindow = new BitmapDrawable(bm.getBitmap());
                            hotel.setImageInfoWindow(imageInfoWindow);
                        }
                    }
                    
                    if (imageInfoWindow == null) {
                        pictureImv.setBackgroundResource(R.drawable.bg_picture);
                    } else {
                        pictureImv.setBackgroundDrawable(imageInfoWindow);
                    }
                    
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture_none);
                }
                
                if (hotel.getCanReserve() > 0) {
                    canReserveTxv.setVisibility(View.GONE);
                } else {
                    canReserveTxv.setVisibility(View.VISIBLE);
                }
                
                ViewGroup bodyView=(ViewGroup)mInfoWindowHotel.findViewById(R.id.body_view);
                bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
                
                mInfoWindowTrafficButtonListener.poi = poi;
                mInfoWindowTrafficButtonListener.actionLog = ActionLog.MapInfoWindowTraffic;
                trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
                
                infoWindow.setViewGroup(mInfoWindowHotel);
            } else if (sourceType != POI.SOURCE_TYPE_CLICK_SELECT_POINT
                    && sourceType != POI.SOURCE_TYPE_MY_LOCATION) {
//                if (mInfoWindowPOI == null) {
                    mInfoWindowPOI = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_poi, null);
//                }
                
                TextView nameTxv=(TextView)mInfoWindowPOI.findViewById(R.id.name_txv);
                RatingBar starsRtb = (RatingBar) mInfoWindowPOI.findViewById(R.id.stars_rtb);
                TextView priceTxv= (TextView)mInfoWindowPOI.findViewById(R.id.price_txv);
                ImageButton trafficBtn=(ImageButton)mInfoWindowPOI.findViewById(R.id.traffic_btn);
                
                nameTxv.setText(poi.getName());
                starsRtb.setRating(poi.getGrade()/2.0f);
                long money = poi.getPerCapity();
                if (money > -1) {
                    priceTxv.setText(mContext.getString(R.string.yuan, money));
                } else {
                    priceTxv.setText("");
                }
                
                int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(96));
                layoutInfoWindow(nameTxv, max);
                
                ViewGroup bodyView=(ViewGroup)mInfoWindowPOI.findViewById(R.id.body_view);
                bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
                
                mInfoWindowTrafficButtonListener.poi = poi;
                mInfoWindowTrafficButtonListener.actionLog = ActionLog.MapInfoWindowTraffic;
                trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
                
                infoWindow.setViewGroup(mInfoWindowPOI);
            } else {
                setInfoWindowMessage(overlayItem.getMessage());
                infoWindow.setViewGroup(mInfoWindowMessage);
            }
        } else if (object instanceof Zhanlan || object instanceof Yanchu) {
            POI poi;
            TKDrawable tkDrawable; 
            long rating;
            String name;
            if (object instanceof Zhanlan) {
                Zhanlan target = (Zhanlan) object;
                name = target.getName();
                poi = target.getPOI();
                tkDrawable = target.getPictures();
                rating = target.getHot();
            } else {
                Yanchu target = (Yanchu) object;
                name = target.getName();
                poi = target.getPOI();
                tkDrawable = target.getPictures();
                rating = target.getHot();
            }

//            if (mInfoWindowYanchuList == null) {
                mInfoWindowYanchuList = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_yanchu_list, null);
//            }
            
            ImageView pictureImv = (ImageView)mInfoWindowYanchuList.findViewById(R.id.icon_imv);
            TextView nameTxv=(TextView)mInfoWindowYanchuList.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) mInfoWindowYanchuList.findViewById(R.id.stars_rtb);
            ImageButton trafficBtn=(ImageButton)mInfoWindowYanchuList.findViewById(R.id.traffic_btn);
            
            nameTxv.setText(name);
            starsRtb.setProgress((int)rating);
            
            int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(148));
            layoutInfoWindow(nameTxv, max);
            
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, getResultMapFragment().toString());
                if(drawable != null) {
                	//To prevent the problem of size change of the same pic 
                	//After it is used at a different place with smaller size
                    Rect bounds = drawable.getBounds();
                    if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
                        pictureImv.setBackgroundDrawable(null);
                    }
                	pictureImv.setBackgroundDrawable(drawable);
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture);
                }
            } else {
                pictureImv.setBackgroundResource(R.drawable.bg_picture);
            }
            
            ViewGroup bodyView=(ViewGroup)mInfoWindowYanchuList.findViewById(R.id.body_view);
            bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
            
            mInfoWindowTrafficButtonListener.poi = poi;
            mInfoWindowTrafficButtonListener.actionLog = ActionLog.MapInfoWindowTraffic;
            trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
            
            infoWindow.setViewGroup(mInfoWindowYanchuList);
        } else if (object instanceof Tuangou) {
            POI poi;
            TKDrawable tkDrawable; 
            Tuangou target = (Tuangou) object;
            poi = target.getPOI();
            tkDrawable = target.getPictures();
            String price = target.getPrice();

//            if (mInfoWindowTuangouList == null) {
                mInfoWindowTuangouList = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_tuangou_list, null);
//            }
            
            ImageView pictureImv = (ImageView)mInfoWindowTuangouList.findViewById(R.id.icon_imv);
            TextView nameTxv=(TextView)mInfoWindowTuangouList.findViewById(R.id.name_txv);
            TextView priceTxv= (TextView)mInfoWindowTuangouList.findViewById(R.id.price_txv);
            ImageButton trafficBtn=(ImageButton)mInfoWindowTuangouList.findViewById(R.id.traffic_btn);
            
            nameTxv.setText(target.getShortDesc());
            priceTxv.setText(price);
            
            int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(186));
            layoutInfoWindow(nameTxv, max);
            
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, getResultMapFragment().toString());
                if(drawable != null) {
                	//To prevent the problem of size change of the same pic 
                	//After it is used at a different place with smaller size
                    Rect bounds = drawable.getBounds();
                    if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
                        pictureImv.setBackgroundDrawable(null);
                    }
                    pictureImv.setBackgroundDrawable(drawable);
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture);
                }
            } else {
                pictureImv.setBackgroundResource(R.drawable.bg_picture);
            }
            
            ViewGroup bodyView=(ViewGroup)mInfoWindowTuangouList.findViewById(R.id.body_view);
            bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
            
            mInfoWindowTrafficButtonListener.poi = poi;
            mInfoWindowTrafficButtonListener.actionLog = ActionLog.MapInfoWindowTraffic;
            trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
            
            infoWindow.setViewGroup(mInfoWindowTuangouList);
        } else {
            setInfoWindowMessage(overlayItem.getMessage());
            infoWindow.setViewGroup(mInfoWindowMessage);
        }

        infoWindow.setOffset(new XYFloat((float)(overlayItem.getIcon().getOffset().x - overlayItem.getIcon().getSize().x/2), 
        		(float)(overlayItem.getIcon().getOffset().y)),
                overlayItem.getRotationTilt());
        infoWindow.setVisible(true);
        
        mMapView.refreshMap();
    }
    
    /**
     * 设置mInfoWindowMessage中的文本内容
     * @param message
     * @return
     */
    void setInfoWindowMessage(String message) {
//        if (mInfoWindowMessage == null) {
            // 初始化
            mInfoWindowMessage = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_message, null);
//        }
        
        TextView nameTxv=(TextView)mInfoWindowMessage.findViewById(R.id.name_txv);
        
        nameTxv.setText(message);
        
        int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(96));
        layoutInfoWindow(nameTxv, max);
        
        ViewGroup bodyView=(ViewGroup)mInfoWindowMessage.findViewById(R.id.body_view);
        bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
    }
    
    private void infoWindowClicked() {
        mActionLog.addAction(ActionLog.MapInfoWindowBody);
        if (touchMode.equals(TouchMode.MEASURE_DISTANCE)) {
            return;
        }
        InfoWindow infoWindow=mMapView.getInfoWindow();
        OverlayItem overlayItem=infoWindow.getAssociatedOverlayItem();
        String overlayName = overlayItem.getOwnerOverlay().getName();
        if(touchMode.equals(TouchMode.CHOOSE_ROUTING_END_POINT)){
            if (ItemizedOverlay.MY_LOCATION_OVERLAY.equals(overlayName)) {
                return;
            }
            infoWindow.setVisible(false);
            POI poi = (POI) infoWindow.getAssociatedOverlayItem().getAssociatedObject();
            getTrafficQueryFragment().setDataForSelectPoint(poi, TrafficQueryFragment.SELECTED);
        }else if(touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT)){
            if (ItemizedOverlay.MY_LOCATION_OVERLAY.equals(overlayName)) {
                return;
            }
            infoWindow.setVisible(false);
            POI poi = (POI) infoWindow.getAssociatedOverlayItem().getAssociatedObject();
            getTrafficQueryFragment().setDataForSelectPoint(poi, TrafficQueryFragment.SELECTED);
        } else if(touchMode.equals(TouchMode.LONG_CLICK)){
            // 什么也不用做
            return;
        } else if (overlayItem != null) {
            infoWindow.setVisible(false);
            if (overlayName.equals(ItemizedOverlay.POI_OVERLAY)) {
                Object object = overlayItem.getAssociatedObject();
                if (object instanceof POI) {
                    POI target = (POI) object;
                    int sourceType = target.getSourceType();
                    if (sourceType == POI.SOURCE_TYPE_FENDIAN
                            || sourceType == POI.SOURCE_TYPE_YINGXUN
                            || sourceType == POI.SOURCE_TYPE_ZHANLAN
                            || sourceType == POI.SOURCE_TYPE_YANCHU
                            || sourceType == POI.SOURCE_TYPE_DIANYING
                            || sourceType == POI.SOURCE_TYPE_TUANGOU) {
                        dismissView(R.id.view_result_map);
                    } else {
                        if (uiStackContains(R.id.view_poi_detail)) {
                            dismissView(R.id.view_result_map);
                        } else {
                            showView(R.id.view_poi_detail);
                        }
                        getPOIDetailFragment().setData(target);
                    }
                } else if (object instanceof Tuangou) {
                    Tuangou target = (Tuangou) object;
                    if (uiStackContains(R.id.view_discover_tuangou_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_discover_tuangou_detail);
                    }
                    getTuangouDetailFragment().setData(target);
                } else if (object instanceof Dianying) {
                    Dianying target = (Dianying) object;
                    if (uiStackContains(R.id.view_discover_dianying_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_discover_dianying_detail);
                    }
                    getDianyingDetailFragment().setData(target);
                } else if (object instanceof Zhanlan) {
                    Zhanlan target = (Zhanlan) object;
                    if (uiStackContains(R.id.view_discover_zhanlan_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_discover_zhanlan_detail);
                    }
                    getZhanlanDetailFragment().setData(target);
                } else if (object instanceof Yanchu) {
                    Yanchu target = (Yanchu) object;
                    if (uiStackContains(R.id.view_discover_yanchu_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_discover_yanchu_detail);
                    }
                    getYanchuDetailFragment().setData(target);
                }
            } else if (overlayName.equals(ItemizedOverlay.TRAFFIC_OVERLAY)) {
                if (uiStackContains(R.id.view_traffic_result_detail)) {
                    dismissView(R.id.view_result_map);
                } else {
                    showView(R.id.view_traffic_result_detail);
                }
            } else if (overlayName.equals(ItemizedOverlay.LINE_OVERLAY)) {
                if (uiStackContains(R.id.view_traffic_busline_detail)) {
                    dismissView(R.id.view_result_map);
                } else {
                    showView(R.id.view_traffic_busline_detail);
                }
            }
        }
        setTouchMode(TouchMode.NORMAL);
    }
    
    public void hideInfoWindow(String overlayName) {
        OverlayItem overlayItem = mMapView.getInfoWindow().getAssociatedOverlayItem();
        if (mMapView.getInfoWindow().isVisible() && 
                overlayItem != null && 
                overlayName.equals(overlayItem.getOwnerOverlay().getName())) {
            mMapView.getInfoWindow().setVisible(false);
        }
    }

    // TODO: cityinfo start
    private List<CityInfo> mViewedCityInfoList = new ArrayList<CityInfo>();
    
    private void updateCityInfo(CityInfo cityInfo) {
        if (cityInfo == null
                || cityInfo.isAvailably() == false) {
            return;
        }
        String cName = cityInfo.getCName();
        if (mPOIHomeFragment != null) {
            getPOIHomeFragment().refreshCity(cName);
            getPOIHomeFragment().refreshLocationView();
        }
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
            if (gps) {
                showLocationDialog(R.id.dialog_prompt_choose_city);
            } else {
                showLocationDialog(R.id.dialog_prompt_setting_location_first);
            }
        }
    }
    
    private boolean showPromptChangeToMyLocationCityDialog(final CityInfo locationCity) {
        
        // 仅在频道首页时才提示切换城市的对话框
        int viewId = uiStackPeek();
        int size = uiStackSize();
        if (size != 0 && !(size == 1 &&
                (viewId == R.id.view_poi_home ||
                 viewId == R.id.view_discover_home ||
                 (viewId == R.id.view_traffic_home && getTrafficQueryFragment().isNormalState()) ||
                 viewId == R.id.view_more_home))) {
            return false;
        }
        
        if (mPreventShowChangeMyLocationDialog) {
            return false;
        }
        
        if (Globals.g_My_Location_State == Globals.LOCATION_STATE_SHOW_CHANGE_CITY_DIALOG) {
            return false;
        }
        
        CityInfo myLocationCity = Globals.g_My_Location_City_Info;
        if (myLocationCity == null) {
            return false;
        }
        
        if (showLocationDialog(R.id.dialog_prompt_change_to_my_location_city)) {
            Globals.g_My_Location_State = Globals.LOCATION_STATE_SHOW_CHANGE_CITY_DIALOG;
            return true;
        }
        return false;
    }
    
    private void showPromptSettingLocationDialog() {
        boolean showLocationSettingsTip = TextUtils.isEmpty(TKConfig.getPref(Sphinx.this, TKConfig.PREFS_SHOW_LOCATION_SETTINGS_TIP));
        if (showLocationSettingsTip == false) {
            return;
        }
        showLocationDialog(R.id.dialog_prompt_setting_location);
    }
    
    int dialogId = -1;
    public boolean showLocationDialog(int id) {
        if (mFromThirdParty > 0 && mFromThirdParty != THIRD_PARTY_WENXIN_REQUET) {
            return false;
        }
        
        if (mDragHintView.getVisibility() == View.VISIBLE) {
        	dialogId = id;
        	return false;
        }
        
        if (uiStackSize() <= 0) {
            return false;
        }
        
        Dialog dialog;
        
        if (id != R.id.dialog_prompt_choose_city) {
            dialog = getDialog(R.id.dialog_prompt_choose_city);
            if (dialog.isShowing()) {
                return false;
            }     
        }
        
        if (id != R.id.dialog_prompt_setting_location_first) {
            dialog = getDialog(R.id.dialog_prompt_setting_location_first);
            if (dialog.isShowing()) {
                return false;
            }     
            dialog = getDialog(R.id.dialog_prompt_setting_location);
            if (dialog.isShowing()) {
                return false;
            }    
        }
        
        if (id != R.id.dialog_prompt_setting_location) {
            dialog = getDialog(R.id.dialog_prompt_setting_location_first);
            if (dialog.isShowing()) {
                return false;
            }     
            dialog = getDialog(R.id.dialog_prompt_setting_location);
            if (dialog.isShowing()) {
                return false;
            }     
        }
        
        if (id != R.id.dialog_prompt_change_to_my_location_city) {
            dialog = getDialog(R.id.dialog_prompt_change_to_my_location_city);
            if (dialog.isShowing()) {
                return false;
            }     
        }
        
        showDialog(id);
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
    
    public int getPOIResultFragmentID() {
        int targetViewId = R.id.view_invalid;
        if (!uiStackContains(R.id.view_poi_result)) {
            targetViewId = R.id.view_poi_result;
        } else if (!uiStackContains(R.id.view_poi_result2)) {
            targetViewId = R.id.view_poi_result2;
        }
        
        return targetViewId;
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
        		return R.id.view_poi_home;
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
            boolean result = false;
            for(int i = mUIStack.size()-1; i >= 0; i--) {
                if (mUIStack.get(i) == id) {
                    mUIStack.remove(i);
                    result = true;
                }
            }
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
                    if (id == R.id.view_poi_detail || 
                            id == R.id.view_discover_tuangou_detail || 
                            id == R.id.view_discover_dianying_detail || 
                            id == R.id.view_discover_yanchu_detail ||
                            id == R.id.view_discover_zhanlan_detail) {
                        uiStackRemove(R.id.view_result_map);
                    }
                } else if (uiStackPeek() == R.id.view_poi_detail) {
                    if (id == R.id.view_hotel_order_detail) {
                        uiStackClearTop(R.id.view_poi_detail);
                    }
                } else if (uiStackPeek() == R.id.view_hotel_order_list) {
                    if (id == R.id.view_hotel_order_detail && !uiStackContains(R.id.view_hotel_order_write)) {
                        uiStackClearTop(R.id.view_hotel_order_list);
                    }
                } else if (uiStackPeek() == R.id.view_poi_home) {
                    if (id == R.id.view_poi_result || 
                            id == R.id.view_poi_result2 ||
                            id == R.id.view_hotel_home) {
                        Globals.setHotelCityInfo(null);
                        int cityId = Globals.getCurrentCityInfo().getId();
                        mMapEngine.suggestwordCheck(this, cityId);
                        HistoryWordTable.readHistoryWord(this, cityId, HistoryWordTable.TYPE_TRAFFIC);
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
                        if (v instanceof POIHomeFragment ||
                                v instanceof TrafficQueryFragment ||
                                v instanceof DiscoverHomeFragment ||
                                v instanceof MoreHomeFragment) {
                            isNormalExit = true;
                        }
                    }
                    
                    if (isNormalExit == false) {
                        uiStackClose(null);
                        showView(R.id.view_poi_home);
                    }
                }
            } else {
                boolean isNormalExit = false;
                int count = mBodyView.getChildCount();
                if (count == 1) {
                    View v = mBodyView.getChildAt(0);
                    if (v instanceof POIHomeFragment ||
                            v instanceof TrafficQueryFragment ||
                            v instanceof DiscoverHomeFragment ||
                            v instanceof MoreHomeFragment) {
                        isNormalExit = true;
                    }
                }
                
                if (isNormalExit == false) {
                    uiStackClose(null);
                    showView(R.id.view_poi_home);
                }
            }
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
            LogWrapper.d(TAG, "preferTop: " + preferTop);
            LogWrapper.d(TAG, "mUIStack: " + mUIStack);
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

            for(int i = mUIStack.size()-1; i >= 1; i--) {
                if (mUIStack.get(i) == preferTop && mUIStack.get(i).equals(mUIStack.get(i-1))) {
                    mUIStack.remove(i);
                }
            }
            
            LogWrapper.d(TAG, "mUIStack after cleartop: " + mUIStack);
            return result;
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
    
    public boolean showHomeDragHint(){
        if (mFromThirdParty > 0) {
            return false;
        }
        CityInfo cityInfo = Globals.g_My_Location_City_Info;
        if (cityInfo != null && cityInfo.getId() == Globals.getCurrentCityInfo(false).getId() &&
                TKConfig.getPref(this, TKConfig.PREFS_HINT_POI_HOME_LOCATION) == null &&
                mDragHintView.getVisibility() != View.VISIBLE) {
            TKConfig.setPref(mThis, TKConfig.PREFS_HINT_POI_HOME_LOCATION, "1");
            mDragHintView.removeAllViews();
            mDragHintView.setVisibility(View.VISIBLE);
            mLayoutInflater.inflate(R.layout.hint_poi_home_location, mDragHintView, true);
        }
        return true;
    }
    
    void hideHomeDragHint() {
        mDragHintView.removeAllViews();
        mDragHintView.setVisibility(View.GONE);
        if (dialogId != -1) {
            showLocationDialog(dialogId);
            dialogId = -1;
        }
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
            } else if (R.id.activity_more_help == viewId) {
                intent.setClass(this, HelpActivity.class);
                startActivityForResult(intent, R.id.activity_more_help);
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

    public void replace(BaseFragment baseView) {
        synchronized (mUILock) {
            mBodyView.removeAllViews();
            mBodyView.addView(baseView);
        }
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
            case R.id.dialog_prompt_change_to_my_location_city:

                View changeCityView = mLayoutInflater.inflate(R.layout.alert_change_city, null, false);
                dialog = Utility.getDialog(Sphinx.this,
                        getString(R.string.prompt_location),
                        null,
                        changeCityView,
                        null,
                        null,
                        null);
                
                View customPanel = dialog.findViewById(R.id.customPanel);
                customPanel.setBackgroundDrawable(null);
                customPanel.setPadding(0, 0, 0, 0);
                
                Button button1 = (Button) changeCityView.findViewById(R.id.button1);
                Button button2 = (Button) changeCityView.findViewById(R.id.button2);
                
                final Dialog finalDialog = dialog;
                View.OnClickListener onClickListener = new OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        int id = v.getId();
                        if (id == R.id.button1) {

                            CityInfo locationCity = Globals.g_My_Location_City_Info;
                            if (locationCity != null && locationCity.isAvailably()) {
                                Dialog dialg = mDialog;
                                if (dialg != null && dialg.isShowing()) {
                                    dialg.dismiss();
                                }
                                getTitleFragment().dismissPopupWindow();
                                BaseFragment baseFragment = getFragment(uiStackPeek());
                                if (baseFragment != null) {
                                    baseFragment.dismissPopupWindow();
                                }
                                uiStackClose(null);
                                showView(R.id.view_poi_home);
                                changeCity(locationCity);
                                mHandler.post(new Runnable() {
                                    
                                    @Override
                                    public void run() {
                                        getPOIHomeFragment().getCategoryLsv().setSelectionFromTop(0, 0);
                                    }
                                });
                                mActionLog.addAction(ActionLog.LifecycleSelectCity, Globals.getCurrentCityInfo(false).getCName());
                            }
                        }
                        finalDialog.dismiss();
                    }
                };
                button1.setOnClickListener(onClickListener);
                button2.setOnClickListener(onClickListener);
                
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
                break;
                
            case R.id.dialog_prompt_setting_location_first:
                View settingLocationView1 = mLayoutInflater.inflate(R.layout.alert_setting_location, null, false);
                dialog = Utility.getDialog(Sphinx.this,
                        getString(R.string.advice_open_gps_title),
                        null,
                        settingLocationView1,
                        Sphinx.this.getString(R.string.i_know),
                        Sphinx.this.getString(R.string.settings),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                switch (id) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        showLocationDialog(R.id.dialog_prompt_choose_city);
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        showView(R.id.activity_setting_location);
                                        break;

                                    default:
                                        break;
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
                break;
                
            case R.id.dialog_prompt_choose_city:
                dialog = Utility.getDialog(Sphinx.this,
                        mContext.getString(R.string.prompt),
                        mContext.getString(R.string.location_failed_and_change_city),
                        null,
                        mContext.getString(R.string.confirm),
                        mContext.getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                switch (id) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        showView(R.id.activity_more_change_city);
                                        break;

                                    default:
                                        break;
                                }
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
    private MenuFragment mMenuFragment;
    private TitleFragment mTitleFragment;
    private POIHomeFragment mPOIHomeFragment;
    private MoreHomeFragment mMoreFragment;
    private MyOrderFragment mMyOrderFragment;
    private GoCommentFragment mGoCommentFragment;
    private ResultMapFragment mResultMapFragment;
    private BrowserFragment mBrowserFragment;
    private FavoriteFragment mFavoriteFragment;
    private HistoryFragment mHistoryFragment;
    private POIDetailFragment mPOIDetailFragment;
    private POIResultFragment mPOIResultFragment;
    private POIResultFragment mPOIResultFragment2;
    private InputSearchFragment mPOIQueryFragment;
    private NearbySearchFragment mPOINearbyFragment;
    private TrafficDetailFragment mTrafficDetailFragment = null;
    private TrafficResultFragment mTrafficResultFragment = null;
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
    private DiscoverHomeFragment mDiscoverFragment;
    
    private HotelHomeFragment mHotelHomeFragment;
    private PickLocationFragment mPickLocationFragment;
    private HotelOrderWriteFragment mHotelOrderWriteFragment;
    private HotelOrderCreditFragment mHotelOrderCreditFragment;
    
    private HotelOrderDetailFragment mHotelOrderDetailFragment;
    private HotelOrderListFragment mHotelOrderListFragment;
    private HotelImageGridFragment mHotelImageGridFragment;
    
    private CouponListFragment mCouponListFragment;
    private CouponDetailFragment mCouponDetailFragment;

    private MeasureDistanceFragment mMeasureDistanceFragment;
    
    public BaseFragment getFragment(int id) {
        BaseFragment baseFragment = null;

        switch (id) {                
            case R.id.view_more_home:
                baseFragment = getMoreFragment();
                break;
                
            case R.id.view_poi_home:
                baseFragment = getPOIHomeFragment();
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
                
            case R.id.view_poi_result2:
                baseFragment = getPOIResultFragment2();
                break;
                
            case R.id.view_poi_detail:
                baseFragment = getPOIDetailFragment();
                break;
                
            case R.id.view_poi_input_search:
                baseFragment = getPOIQueryFragment();
                break;
                
            case R.id.view_poi_nearby_search:
                baseFragment = getPOINearbyFragment();
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
                
            case R.id.view_discover_home:
                baseFragment = getDiscoverFragment();
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
            	
            case R.id.view_hotel_order_detail:
            	baseFragment = getHotelOrderDetailFragment();
            	break;
            	
            case R.id.view_hotel_image_grid:
            	baseFragment = getHotelImageGridFragment();
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
                
            default:
                break;
        }
        return baseFragment;
    }
    
    public TitleFragment getTitleFragment() {
        synchronized (mUILock) {
            if (mTitleFragment == null) {
                TitleFragment titleFragment = new TitleFragment(Sphinx.this);
                titleFragment.setId(R.id.view_title);
                titleFragment.onCreate(null);
                mTitleView.addView(titleFragment);
                mTitleFragment = titleFragment;
            }
            return mTitleFragment;
        }
    }
    
    public MenuFragment getMenuFragment() {
        synchronized (mUILock) {
            if (mMenuFragment == null) {
                MenuFragment menuFragment = new MenuFragment(Sphinx.this);
                menuFragment.setId(R.id.view_menu);
                menuFragment.onCreate(null);
                mMenuView.addView(menuFragment);
                mMenuFragment = menuFragment;
            }
            return mMenuFragment;
        }
    }
    
    public POIHomeFragment getPOIHomeFragment() {
        synchronized (mUILock) {
            if (mPOIHomeFragment == null) {
                POIHomeFragment fragment = new POIHomeFragment(Sphinx.this);
                fragment.setId(R.id.view_poi_home);
                fragment.onCreate(null);
                mPOIHomeFragment = fragment;
            }
            return mPOIHomeFragment;
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
    
    public POIResultFragment getPOIResultFragment2() {
        synchronized (mUILock) {
            if (mPOIResultFragment2 == null) {
                POIResultFragment poiResultFragment = new POIResultFragment(Sphinx.this);
                poiResultFragment.setId(R.id.view_poi_result2);
                poiResultFragment.onCreate(null);
                mPOIResultFragment2 = poiResultFragment;
            }
            return mPOIResultFragment2;
        }
    }
    
    public InputSearchFragment getPOIQueryFragment() {
        synchronized (mUILock) {
            if (mPOIQueryFragment == null) {
                InputSearchFragment poiQueryFragment = new InputSearchFragment(Sphinx.this);
                poiQueryFragment.setId(R.id.view_poi_input_search);
                poiQueryFragment.onCreate(null);
                mPOIQueryFragment = poiQueryFragment;
            }
            return mPOIQueryFragment;
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

    public DiscoverHomeFragment getDiscoverFragment() {
        synchronized (mUILock) {
            if (mDiscoverFragment == null) {
                DiscoverHomeFragment discoverFragment = new DiscoverHomeFragment(Sphinx.this);
                discoverFragment.setId(R.id.view_discover_home);
                discoverFragment.onCreate(null);
                mDiscoverFragment = discoverFragment;
            }
            return mDiscoverFragment;
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
    
    public HotelImageGridFragment getHotelImageGridFragment(){
    	
        synchronized (mUILock) {
            if (mHotelImageGridFragment == null) {
            	HotelImageGridFragment fragment = new HotelImageGridFragment(Sphinx.this);
                fragment.setId(R.id.view_hotel_image_grid);
                fragment.onCreate(null);
                mHotelImageGridFragment = fragment;
            }
            return mHotelImageGridFragment;
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
    private boolean mPreventShowChangeMyLocationDialog = false;
    private MyLocation mMyLocation;
    private ItemizedOverlay mMyLocationOverlay;
    private Runnable mCheckLocationCityRun = new Runnable() {
        
        @Override
        public void run() {
            checkLocationCity(true);
        }
    };
    private Position lastMyPosition;
    private Runnable mLocationChangedRun = new Runnable() {
        
        @Override
        public void run() {
            LogWrapper.d(TAG, "mLocationChangedRun");
            
            CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
            Position myPosition = null;
            if (myLocationCityInfo != null) {
                myPosition = myLocationCityInfo.getPosition();
            }
            
            if (lastMyPosition == myPosition || (lastMyPosition != null && lastMyPosition.equals(myPosition))) {
                return;
            }
            lastMyPosition = myPosition;

            LogWrapper.d(TAG, "mLocationChangedRun refresh");
            if (uiStackPeek() == R.id.view_poi_home || uiStackPeek() == R.id.view_discover_home) {
                getPOIHomeFragment().refreshLocationView();
            }
            
            if (Globals.g_My_Location_State == Globals.LOCATION_STATE_FIRST_SUCCESS
                    && myLocationCityInfo != null) {
                CityInfo currentCity = Globals.getCurrentCityInfo(false);
                if (currentCity.getId() != myLocationCityInfo.getId()) {
                    showPromptChangeToMyLocationCityDialog(myLocationCityInfo);
                }
            }
            
            if (mPOIHomeFragment == null || mMapView.isStopRefreshMyLocation()) {
                return;
            }
            
            updateMyLocation();
        }
    };
    
    public void updateMyLocation() {
        LogWrapper.d(TAG, "updateMyLocation");
        Position myLocationPosition = null;
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo != null) {
            myLocationPosition = myLocationCityInfo.getPosition();
        }
        final Position myPosition = myLocationPosition;
        if (myPosition != null) {
            updateMyLocation(myPosition);
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
        updateMyLocationOverlay(myPosition); 
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
                    && mPOIHomeFragment != null
                    && mMapView.isStopRefreshMyLocation() == false
                    && Math.abs(rotateZ-event.values[0]) >= 3){
                rotateZ = event.values[0];
                mMapView.rotateLocationZToDegree(-rotateZ);
                if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                    mMapView.rotateZToDegree(-rotateZ);
                }
                mMapView.refreshMap();
            }
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
        
    private Runnable mLocationResponseRun = new Runnable() {
        
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
            if (myLocationCityInfo != null) {
                updateMyLocationOverlay(myLocationCityInfo.getPosition());
            }
        }
    };
    
    private boolean updateMyLocation(Position myPosition) {
    	if(myPosition == null) {
    		return false;
    	}
    	else {
    		if(!myPosition.equals(mMyLocation.getPosition())){
                try{
                    mMyLocation.setPosition(myPosition);
                }catch(Exception e){
                    e.printStackTrace();
                }
                String msg=getString(R.string.my_location);
                String positionName = MapEngine.getPositionName(myPosition);
                if (positionName != null && positionName.length() > 0) {
                    msg += "\n" + positionName;
                }
                LogWrapper.d("positionbug", positionName);
                mMyLocation.setMessage(msg);
             // 定位图层显示InfoWindow时, 若定位点变化, InfoWindow也会变化
                InfoWindow infoWindow = mMapView.getInfoWindow();
                OverlayItem overlayItem = infoWindow.getAssociatedOverlayItem();
                if (infoWindow.isVisible() && overlayItem != null) {
                    if (ItemizedOverlay.MY_LOCATION_OVERLAY.equals(overlayItem.getOwnerOverlay().getName())) {
                        showInfoWindow(mMyLocation);
                    }
                }
    		}
            return true;
    	}
    }
    
    private boolean updateMyLocationOverlay(Position myLocation){
        if(myLocation==null) {
            mMapView.deleteOverlaysByName(ItemizedOverlay.MY_LOCATION_OVERLAY);
            mMapView.deleteShapeByName(Shape.MY_LOCATION);
            InfoWindow infoWindow = mMapView.getInfoWindow();
            OverlayItem overlayItem = infoWindow.getAssociatedOverlayItem();
            if (infoWindow.isVisible() && overlayItem != null) {
                if (ItemizedOverlay.MY_LOCATION_OVERLAY.equals(overlayItem.getOwnerOverlay().getName())) {
                    infoWindow.setVisible(false);   
                }
            }
            mMapView.refreshMap();
            return false;
        }
        if(!myLocation.equals(mMyLocation.getPosition())){
            try{
                mMyLocation.setPosition(myLocation);
            }catch(Exception e){
                e.printStackTrace();
            }
            String msg=getString(R.string.my_location);
            String positionName = MapEngine.getPositionName(myLocation);
            if (positionName != null && positionName.length() > 0) {
                msg += "\n" + positionName;
            }
            mMyLocation.setMessage(msg);
            
            // 定位图层显示InfoWindow时, 若定位点变化, InfoWindow也会变化
            InfoWindow infoWindow = mMapView.getInfoWindow();
            OverlayItem overlayItem = infoWindow.getAssociatedOverlayItem();
            if (infoWindow.isVisible() && overlayItem != null) {
                if (ItemizedOverlay.MY_LOCATION_OVERLAY.equals(overlayItem.getOwnerOverlay().getName())) {
                    showInfoWindow(mMyLocation);
                }
            }
        }
        if (mMapView.getOverlaysByName(ItemizedOverlay.MY_LOCATION_OVERLAY) == null) {
            try {
                mMapView.addOverlay(mMyLocationOverlay);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        Circle myLocationRadiusCircle=(Circle)mMapView.getShapesByName(Shape.MY_LOCATION);
        if(myLocationRadiusCircle==null){
            try{
                myLocationRadiusCircle=new Circle(myLocation,new Length(myLocation.getAccuracy(),UOM.M),Shape.MY_LOCATION);
                mMapView.addShape(myLocationRadiusCircle);
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            try{
                myLocationRadiusCircle.setPosition(myLocation);
                myLocationRadiusCircle.setRadius(new Length(myLocation.getAccuracy(),UOM.M));
            }catch(Exception e){
                e.printStackTrace();
            }
            
        }
        mMapView.refreshMap();
        return true;
    }
        
    public void requestLocation() {
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo == null || mMyLocation.getPosition() == null) {
            updateLoactionButtonState(MyLocation.MODE_NONE);
            showTip(R.string.location_waiting, 3000);
            mHandler.removeCallbacks(mLocationResponseRun);
            mHandler.postDelayed(mLocationResponseRun, 20000);
        } else {
            try {
                if (mMyLocation.mode == MyLocation.MODE_NONE || mMyLocation.mode == MyLocation.MODE_NORMAL) {
                    showTip(getString(R.string.location_success, Utility.formatMeterString((int)myLocationCityInfo.getPosition().getAccuracy())), 3000);
                    updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                    int zoomLevel = (int)mMapView.getZoomLevel();
                    Position pos = myLocationCityInfo.getPosition();
                    if(zoomLevel < TKConfig.ZOOM_LEVEL_LOCATION) {
                    	mMapView.setZoomLevel(TKConfig.ZOOM_LEVEL_LOCATION);
                    	mMapView.panToPosition(pos);
                    }
                    else {
                    	mMapView.panToPosition(pos);
                    }
                    resetShowInPreferZoom();
                } else if (mMyLocation.mode == MyLocation.MODE_NAVIGATION && mSensorOrientation) {
                    updateLoactionButtonState(MyLocation.MODE_ROTATION);
                    mMapView.centerOnPosition(myLocationCityInfo.getPosition());
                } else if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                    updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                    mMapView.centerOnPosition(myLocationCityInfo.getPosition());
                }
                mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, true);
            } catch (APIException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private void updateLoactionButtonState(int locationButtonState) {
        mMyLocation.mode = locationButtonState;
        Compass compass = mMapView.getCompass();
        if (mMyLocation.mode == MyLocation.MODE_NONE) {
            compass.setVisible(false);
            if (uiStackSize() > 0)
                mCompassView.setVisibility(View.VISIBLE);
            mMapView.refreshMap();
            hideInfoWindow(ItemizedOverlay.MY_LOCATION_OVERLAY);
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
                mMapView.refreshMap();
                rotateZ = 365;
                mMapView.rotateZToDegree(0);
                resid = R.drawable.ic_location_navigation;
                text = R.string.location_text_navigation;
                showInfoWindow(mMyLocation);
            } else if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
            	//只有这种情况下要指南针。
                compass.setVisible(true);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.INVISIBLE);
                mMapView.refreshMap();
                hideInfoWindow(ItemizedOverlay.MY_LOCATION_OVERLAY);
                rotateZ = 365;
                mMapView.refreshMap();
                resid = R.drawable.ic_location_rotation;
                text = R.string.location_text_compass;
            } else {
                compass.setVisible(false);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.VISIBLE);
                mMapView.refreshMap();
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
    
    // TODO: view handler action start
    
    /**
     * 将地图中心移至当前OverlayItem, 并显示InfoWindow
     */
    private void centerShowCurrentOverlayFocusedItem() {
    	ItemizedOverlay itemizedOverlay = mMapView.getCurrentOverlay();
    	
        if (itemizedOverlay != null) {
            final OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
            if (overlayItem != null) {
//            if (overlayItem.hasPreferZoomLevel() && (overlayItem.getPreferZoomLevel() > mMapView.getZoomLevel())) {
            if (itemizedOverlay.isShowInPreferZoom) {
            	if (overlayItem.hasPreferZoomLevel() && (overlayItem.getPreferZoomLevel() > mMapView.getZoomLevel())) {
            		mMapView.setZoomLevel(overlayItem.getPreferZoomLevel());
            		mMapView.panToPosition(overlayItem.getPosition());
            	} else {
                	mMapView.panToPosition(overlayItem.getPosition());
                }
                itemizedOverlay.isShowInPreferZoom = false;
            } else {
            	mMapView.panToPosition(overlayItem.getPosition());
            }
            showInfoWindow(overlayItem);
            }
        }
        
        // TODO: 这边写死的判断不大好, 抽象出来...
        if (itemizedOverlay.getName().equals(ItemizedOverlay.TRAFFIC_OVERLAY)) {
        	TrafficOverlayHelper.highlightNextTwoOverlayItem(mMapView);
        }
    }
    
    /**
     * 在当前OverlayItem位置显示InfoWindow,
     * 若Infowindow不能在屏幕内完全显示, 则移动地图
     */
    private void adjustShowCurrentOverlayFocusedItem() {
    	LogWrapper.d(TAG, "adjustShowCurrentOverlayFocusedItem");
    	ItemizedOverlay itemizedOverlay = mMapView.getCurrentOverlay();
    	
        if (itemizedOverlay != null) {
            final OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
            showInfoWindow(overlayItem);
            
            mHandler.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					// 得到气泡的Rect, Rect的坐标点以气泡底线中心点为坐标原点
					// 如rect.left: -106.0 rect.right: 106.0 rect.top: -55.0 rect.bottom: 0.0
					RectF rect = mMapView.getInfoWindowRecF();
					
					// 将气泡的Rect加上气泡所在的屏幕位置, 得到气泡在屏幕上所占区域
					XYFloat infoXY = mMapView.positionToScreenXY(overlayItem.getPosition());
					rect.left += infoXY.x;
					rect.right += infoXY.x;
					rect.top += infoXY.y - mMapView.getInfoWindow().getOffset().y;
					rect.bottom += infoXY.y;
					
					int mapviewPaddingTop = mMapView.getPadding().top;
					
					LogWrapper.d(TAG, "rect.left: " + rect.left + "rect.right: " + rect.right + "rect.top: " + rect.top + "rect.bottom: " + rect.bottom);
					// 若气泡的上下左右四边不能完全显示在屏幕内, 则移动地图
					// TODO:  1 MapView的Padding
			        if (rect.left < 0 || rect.right > Globals.g_metrics.widthPixels 
			        		|| rect.top < mapviewPaddingTop || rect.bottom > Globals.g_metrics.heightPixels) {
			        	Position centerPos = mMapView.getCenterPosition();
			        	XYFloat centerPoint = mMapView.positionToScreenXY(centerPos);
			        	XYFloat newCenterPoint = centerPoint;
			        	
			        	LogWrapper.d(TAG, "screenWidth: " + Globals.g_metrics.widthPixels + "screenHeight: " + Globals.g_metrics.heightPixels);
			        	LogWrapper.d(TAG, "current point.x: " + centerPoint.x + "current point.y: " + centerPoint.y);
			        	if (rect.left < 0) {
			        		newCenterPoint.x += rect.left - Util.dip2px(Globals.g_metrics.density, 5);
			        	}
			        	if (rect.right > Globals.g_metrics.widthPixels) {
			        		newCenterPoint.x += (rect.right - Globals.g_metrics.widthPixels) + Util.dip2px(Globals.g_metrics.density, 5);
			        	}
			        	if (rect.top < mapviewPaddingTop) {
			        		newCenterPoint.y += (rect.top - mapviewPaddingTop) - Util.dip2px(Globals.g_metrics.density, 5);
			        	}
			        	if (rect.bottom > Globals.g_metrics.heightPixels) {
			        		newCenterPoint.y += (rect.bottom - Globals.g_metrics.heightPixels) + Util.dip2px(Globals.g_metrics.density, 5);
			        	}
			        	LogWrapper.d(TAG, "newCenterPoint.x: " + newCenterPoint.x + "newCenterPoint.y: " + centerPoint.y);
			        	
			        	Position newCenter = mMapView.screenXYToPos(newCenterPoint);
			        	mMapView.panToPosition(newCenter);
			        }
				}
			});

        }

    }
    
    // TODO: view handler action end
    
    /*
     * 在handler中使mPreviousNextView可见的操作可能不起作用, BUG 49
     * 暂时使用这个方法, 阿门
     */
    public void setPreviousNextViewVisible() {
    	mPreviousNextView.setVisibility(View.VISIBLE);
    }

    public View getPreviousNextView() {
        return mPreviousNextView;
    }

    public View getLocationView() {
        return mLocationView;
    }

    public View getZoomView() {
        return mZoomView;
    }

    public View getMoreBtn() {
        return mMoreBtn;
    }

    public View getMoreImv() {
        return mMoreImv;
    }
}

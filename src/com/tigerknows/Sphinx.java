/*
 * Copyright (C) 2010 lihong@tigerknows.com
 */

package com.tigerknows;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import com.tigerknows.widget.Toast;

import com.decarta.CONFIG;
import com.decarta.Globals;
import com.decarta.Profile;
import com.decarta.android.event.EventRegistry;
import com.decarta.android.event.EventSource;
import com.decarta.android.event.EventType;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.decarta.android.map.Circle;
import com.decarta.android.map.Compass;
import com.decarta.android.map.Icon;
import com.decarta.android.map.InfoWindow;
import com.decarta.android.map.ItemizedOverlay;
import com.decarta.android.map.MapText;
import com.decarta.android.map.MapView;
import com.decarta.android.map.MyLocation;
import com.decarta.android.map.MapView.DownloadEventListener;
import com.decarta.android.map.MapView.MapScene;
import com.decarta.android.map.MapView.SnapMap;
import com.decarta.android.map.MapView.ZoomEndEventListener;
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
import com.decarta.example.AppUtil;
import com.decarta.example.ConfigActivity;
import com.decarta.example.ProfileResultActivity;
import com.tigerknows.MapDownload.DownloadCity;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.maps.PinOverlayHelper;
import com.tigerknows.maps.TrafficOverlayHelper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.FeedbackUpload;
import com.tigerknows.model.POI;
import com.tigerknows.model.Shangjia;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.UserLogon;
import com.tigerknows.model.UserLogonModel;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.service.SuggestLexiconService;
import com.tigerknows.service.TKLocationManager.TKLocationListener;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.BaseDialog;
import com.tigerknows.view.BaseFragment;
import com.tigerknows.view.BuslineDetailFragment;
import com.tigerknows.view.BuslineResultLineFragment;
import com.tigerknows.view.BuslineResultStationFragment;
import com.tigerknows.view.FavoriteFragment;
import com.tigerknows.view.FetchFavoriteDialog;
import com.tigerknows.view.GoCommentFragment;
import com.tigerknows.view.HistoryFragment;
import com.tigerknows.view.HomeFragment;
import com.tigerknows.view.MenuFragment;
import com.tigerknows.view.MoreFragment;
import com.tigerknows.view.MyCommentListFragment;
import com.tigerknows.view.POIDetailFragment;
import com.tigerknows.view.POINearbyFragment;
import com.tigerknows.view.POIQueryFragment;
import com.tigerknows.view.POIResultFragment;
import com.tigerknows.view.ResultMapFragment;
import com.tigerknows.view.ScaleView;
import com.tigerknows.view.TitleFragment;
import com.tigerknows.view.TrafficAlternativesDialog;
import com.tigerknows.view.TrafficDetailFragment;
import com.tigerknows.view.TrafficQueryFragment;
import com.tigerknows.view.TrafficResultFragment;
import com.tigerknows.view.ZoomControls;
import com.tigerknows.view.discover.BrowserActivity;
import com.tigerknows.view.discover.DianyingDetailFragment;
import com.tigerknows.view.discover.DiscoverFragment;
import com.tigerknows.view.discover.DiscoverListFragment;
import com.tigerknows.view.discover.TuangouDetailFragment;
import com.tigerknows.view.discover.DiscoverChildListFragment;
import com.tigerknows.view.discover.TuangouShangjiaListActivity;
import com.tigerknows.view.discover.YanchuDetailFragment;
import com.tigerknows.view.discover.ZhanlanDetailFragment;
import com.tigerknows.view.user.User;
import com.tigerknows.view.user.UserBaseActivity;
import com.tigerknows.view.user.UserHomeFragment;
import com.tigerknows.view.user.UserLoginActivity;
import com.tigerknows.view.user.UserUpdateNickNameActivity;
import com.tigerknows.view.user.UserUpdatePasswordActivity;
import com.tigerknows.view.user.UserUpdatePhoneActivity;

public class Sphinx extends TKActivity implements TKAsyncTask.EventListener {
    
    public static final String ACTION_FIRST_STARTUP = "action.com.tigerknows.first.startup";
    
	private static final int CONFIG_SERVER = 8;
	private static final int PROFILE = 7;
	
	public static final int CONFIG_SERVER_CODE = 14;
	public static final String REFRESH_POI_DETAIL = "refresh_poi_detail";

	private MapView mMapView;
    private TextView mDownloadView;
    private View mCompassView;
	private LinearLayout mZoomView;
	private ImageButton mLocationBtn=null;
    private View mPreviousNextView=null;
    private Button mPreviousBtn=null;
    private Button mNextBtn=null;
    private ScaleView mScaleView=null;
    private int mTitleViewHeight;
    private int mMenuViewHeiht;
    private int mCityViewHeight;
	
	private TouchMode touchMode=TouchMode.NORMAL;
	public enum TouchMode{
		NORMAL, CHOOSE_ROUTING_START_POINT, CHOOSE_ROUTING_END_POINT, LONG_CLICK;
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
    private ResolveInfo mSMSResolveInfo;
    
    private ViewGroup mRootView;
    private ViewGroup mTitleView;
    private ViewGroup mBodyView;
    private ViewGroup mMenuView;
    private ViewGroup mControlView;   

    private ViewGroup mInfoWindowPOI = null;
    private ViewGroup mInfoWindowLongClick = null;
    private ViewGroup mInfoWindowTuangouList = null;
    private ViewGroup mInfoWindowTuangouDetail = null;
    private ViewGroup mInfoWindowYanchuList = null;
    
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
                CommonUtils.queryTraffic(Sphinx.this, poi);
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
    
    private boolean mFromIntent = false;
    public boolean mSnapMap = false;
    
    // 老虎动画时间
    private static final int LOGO_ANIMATION_TIME = 2000;

    private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager winMan=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display display=winMan.getDefaultDisplay();
        display.getMetrics(Globals.g_metrics);
        LogWrapper.i(TAG,"onCreate()"+Globals.g_metrics.density);
        
        TKConfig.readConfig();
        Globals.init(mThis);
        
        mContext = getBaseContext();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        
        mMapEngine = MapEngine.getInstance();
        try {
            mMapEngine.initMapDataPath(mThis, false);
        } catch (APIException exception) {
            exception.printStackTrace();
            CommonUtils.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        mMapEngine.resetFontSize(1.5f+(Globals.g_metrics.scaledDensity > 1.0f ? Globals.g_metrics.scaledDensity-1.0f : 0));
        mMapEngine.resetIconSize(Globals.g_metrics.densityDpi >= DisplayMetrics.DENSITY_HIGH ? 3 : 2);
        CityInfo cityInfo = mMapEngine.getCityInfo(MapEngine.CITY_ID_BEIJING);
        if (cityInfo.isAvailably() == false) {
            CommonUtils.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        
        mActionLog.onCreate();
        
        try{
            setContentView(R.layout.sphinx);
            findViews();
            setListener();

            if (sensor != null) {
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

            if (mSensorOrientation) {
                mSensorManager.registerListener(mSensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            
            final double lastLon = Double.parseDouble(TKConfig.getPref(mContext, TKConfig.PREFS_LAST_LON, "361"));
            final double lastLat = Double.parseDouble(TKConfig.getPref(mContext, TKConfig.PREFS_LAST_LAT, "361"));
            final int lastZoomLevel = Integer.parseInt(TKConfig.getPref(mContext, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(TKConfig.ZOOM_LEVEL_DEFAULT)));

            final Position lastPosition = new Position(lastLat, lastLon);
            Log.i(TAG,"onCreate positon,zoomLevel:"+lastPosition+","+lastZoomLevel);
            if(Util.inChina(lastPosition)){
                int cityId = mMapEngine.getCityId(lastPosition);
                cityInfo = mMapEngine.getCityInfo(cityId);
                cityInfo.setPosition(lastPosition);
                cityInfo.setLevel(lastZoomLevel);
            }
            changeCity(cityInfo);
            if (Globals.g_User != null) {
                mActionLog.addAction(ActionLog.UserReadSuccess);
            }
            mActionLog.addAction(ActionLog.LifecycleSelectCity, cityInfo.getCName());
            
            mHandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {
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
                        Toast.makeText(Sphinx.this, R.string.network_failed, Toast.LENGTH_LONG).show(); 
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
            
            ArrayList<Integer> uiStack = null;
            if (savedInstanceState != null) {
                uiStack = savedInstanceState.getIntegerArrayList("uistack");
            }
            LogWrapper.d(TAG, "onCreate() uiStack="+uiStack);

            initIntent(getIntent());
            if (mFromIntent || mSnapMap) {
                // 来第三方的调用
            } else if (uiStack != null) {
                initView(uiStack);
            } else {
                mTitleView.setVisibility(View.INVISIBLE);
                mMenuView.setVisibility(View.INVISIBLE);
                final boolean fristUse = TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_FIRST_USE));
                final boolean upgrade = TextUtils.isEmpty(TKConfig.getPref(mContext, TKConfig.PREFS_UPGRADE));
                mPreventShowChangeMyLocationDialog |= fristUse;
                mPreventShowChangeMyLocationDialog |= upgrade;
                mHandler.postDelayed(new Runnable() {
                    
                    @Override
                    public void run() {
                        mMapView.setStopRefreshMyLocation(true);
                        if (fristUse) {
                            sendFirstStartupBroadcast();
                            Intent intent = new Intent();
                            intent.putExtra(Help.APP_FIRST_START, fristUse);
                            showView(R.id.activity_help, intent);
                            
                            TKConfig.setPref(mContext, TKConfig.PREFS_FIRST_USE, "1");
                            TKConfig.setPref(mContext, TKConfig.PREFS_UPGRADE, "1");
                            mHandler.postDelayed(new Runnable() {
                                
                                @Override
                                public void run() {
                                    initView(null);
                                }
                            }, 1000);
                        } else {
                            if (upgrade) {
                                sendFirstStartupBroadcast();
                                Intent intent = new Intent();
                                intent.putExtra(Help.APP_UPGRADE, upgrade);
                                showView(R.id.activity_help, intent);
                                TKConfig.setPref(mContext, TKConfig.PREFS_UPGRADE, "1");
                                mHandler.postDelayed(new Runnable() {
                                    
                                    @Override
                                    public void run() {
                                        initView(null);
                                    }
                                }, 1000);
                            } else {
                                initView(null);
                                checkLocationCity();
                            }
                        }
                    }
                }, LOGO_ANIMATION_TIME);
            }
            
            new Thread(new Runnable() {
                
                @Override
                public void run() {

                    Shangjia.readShangjiaList(Sphinx.this);
                    try {
                        Globals.getImageCache().init(Sphinx.this);
                    } catch (APIException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    Globals.initOptimalAdaptiveScreenSize();
                }
            }).start();
            
            
            mTKLocationManager.onCreate();
            mLocationListener = new MyLocationListener(this, mLocationChangedRun);
            
            mZoomView.setVisibility(View.INVISIBLE);
            
            ZoomControls zoomControls = mMapView.getZoomControls();
            zoomControls.setOnZoomInClickListener(new OnClickListener(){
                public void onClick(View view){
                    mActionLog.addAction(ActionLog.MapClickZoomIn);
                    mHandler.sendEmptyMessage(ZOOM_CLICKED);
                    mMapView.zoomIn();
                }
            });
            zoomControls.setOnZoomOutClickListener(new OnClickListener(){
                public void onClick(View view){
                    mActionLog.addAction(ActionLog.MapClickZoomOut);
                    mHandler.sendEmptyMessage(ZOOM_CLICKED);
                    mMapView.zoomOut();
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
            
            mLocationBtn.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    mHandler.sendEmptyMessage(LOCATION_CLICKED);
                    requestLocation();
                }
            });
            
            // examples of adding Events through the API
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.MOVEEND, new MapView.MoveEndEventListener(){
                @Override
                public void onMoveEndEvent(MapView mapView, Position position) {
                    mActionLog.addAction(ActionLog.MapMove);
                    mHandler.sendEmptyMessage(MAP_MOVE);
                    int zoomLevel = Math.round(mapView.getZoomLevel());
                    mScaleView.setMetersPerPixelAtZoom((float)Util.metersPerPixelAtZoom(CONFIG.TILE_SIZE, zoomLevel, mapView.getCenterPosition().getLat()), zoomLevel);
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
                            mScaleView.setMetersPerPixelAtZoom((float)Util.metersPerPixelAtZoom(CONFIG.TILE_SIZE, newZoomLevel, mMapView.getCenterPosition().getLat()), newZoomLevel);
                            if (newZoomLevel >= CONFIG.ZOOM_UPPER_BOUND) {
                                mMapView.getZoomControls().setIsZoomInEnabled(false);
                            } else if (newZoomLevel <= CONFIG.ZOOM_LOWER_BOUND) {
                                mMapView.getZoomControls().setIsZoomOutEnabled(false);
                            } else {
                                mMapView.getZoomControls().setIsZoomInEnabled(true);
                                mMapView.getZoomControls().setIsZoomOutEnabled(true);
                            }
                        }
                    });
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.ROTATEEND, new MapView.RotateEndEventListener(){
                @Override
                public void onRotateEndEvent(MapView mapView, float rotation) {
                    Log.i(TAG,"MapView.RotateEndEventListener rotation:"+rotation);
                    if(rotation!=0 && mMapView.getCompass()!=null) {
                        mMapView.refreshMap();
                    }
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.TILTEND, new MapView.TiltEndEventListener(){
                @Override
                public void onTiltEndEvent(MapView mapView, float tilt) {
                    Log.i(TAG,"MapView.TiltEndEventListener tilt:"+tilt);
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
                        LogWrapper.i(TAG,"CHOOSE_ROUTING_END_POINT || CHOOSE_ROUTING_START_POINT position:"+position.toString());

                        String positionName = mMapEngine.getPositionName(position, (int)mMapView.getZoomLevel());
                        if (TextUtils.isEmpty(positionName)) {
                            positionName = mContext.getString(R.string.select_has_point);
                        }
                        clearMap();
                        PinOverlayHelper.drawSelectPointOverlay(Sphinx.this, mHandler, mMapView, touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT), positionName, position);
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
            		int zoomLevel = (int) mapView.getZoomLevel();
            		int newZoomLevel = zoomLevel+1;
            		if (zoomLevel < CONFIG.ZOOM_UPPER_BOUND) {
	            		XYFloat xy = mapView.positionToScreenXY(position);
	            		Position centerPos = mapView.getCenterPosition();
						XYFloat center = mapView.positionToScreenXY(centerPos);
						final XYFloat offset = new XYFloat(xy.x-center.x, xy.y-center.y);
						if (newZoomLevel == CONFIG.ZOOM_JUMP) {
						    try {
                                mapView.setZoomLevel(newZoomLevel);
                                mapView.refreshMap();
                                XYFloat nowxy = mapView.positionToScreenXY(position);
                                Position nowcenterPos = mapView.getCenterPosition();
                                XYFloat nowcenter = mapView.positionToScreenXY(nowcenterPos);
                                XYFloat nowOffset = new XYFloat(nowxy.x-nowcenter.x, nowxy.y-nowcenter.y);
                                mapView.moveView(offset.x-nowOffset.x, offset.y-nowOffset.y);
                            } catch (APIException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
						} else {
    						mapView.zoomTo(newZoomLevel, centerPos, -1, new ZoomEndEventListener() {
    							
    							@Override
    							public void onZoomEndEvent(MapView mapView, int newZoomLevel) {
    								XYFloat nowxy = mapView.positionToScreenXY(position);
    								Position nowcenterPos = mapView.getCenterPosition();
    								XYFloat nowcenter = mapView.positionToScreenXY(nowcenterPos);
    								XYFloat nowOffset = new XYFloat(nowxy.x-nowcenter.x, nowxy.y-nowcenter.y);
    								mapView.moveView(offset.x-nowOffset.x, offset.y-nowOffset.y);
    							}
    						});
						}
						mActionLog.addAction(ActionLog.MapDoubleClick);
            		}
                	
                }
            });
            
            EventRegistry.addEventListener(mMapView, MapView.EventType.LONGCLICK, new MapView.LongClickEventListener(){
                @Override
                public void onLongClickEvent(MapView mapView, Position position) {
                    if (touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT) || touchMode.equals(TouchMode.CHOOSE_ROUTING_END_POINT)) {
                        String positionName = mMapEngine.getPositionName(position, (int)mMapView.getZoomLevel());
                        if (TextUtils.isEmpty(positionName)) {
                            positionName = mContext.getString(R.string.select_has_point);
                        }
                        clearMap();
                        PinOverlayHelper.drawSelectPointOverlay(Sphinx.this, mHandler, mMapView, touchMode.equals(TouchMode.CHOOSE_ROUTING_START_POINT), positionName, position);
                        return;
                    } else if (!touchMode.equals(TouchMode.LONG_CLICK)) {
                        return;
                    }
                    mActionLog.addAction(ActionLog.MapLongClick);
                    Log.i(TAG,"MapView.onLongClickEvent position:"+position.toString());
                    String positionName = mMapEngine.getPositionName(position, (int)mMapView.getZoomLevel());
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
            String msg=e.getMessage();
            CommonUtils.showDialogAcitvity(mThis, getString(R.string.not_enough_space_and_please_clear));
            finish();
            return;
        }
        
        if (TKConfig.sCountMapWhenOnCreate) {
            mMapEngine.statsMapEnd(new ArrayList<DownloadCity>(), false);
            Intent service = new Intent(Sphinx.this, MapStatsService.class);
            startService(service);
        }

        UserLogon userLogon = new UserLogon(mContext);
        queryStart(userLogon, false);
        
        checkCitySupportDiscover(Globals.g_Current_City_Info.getId());
	}
	
	private void sendFirstStartupBroadcast() {
        Intent intent = new Intent(ACTION_FIRST_STARTUP);
        sendBroadcast(intent);
	}
	
	private void checkCitySupportDiscover(int cityId) {
	    String discover = TKConfig.getPref(this, TKConfig.PREFS_HINT_DISCOVER_HOME);
	    boolean show = TextUtils.isEmpty(discover);
	    if (show == false) {
	        return;
	    }
        if (DataQuery.checkDiscoveryCity(cityId)) {
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
      Log.d(TAG, "onSaveInstanceState");

    }  

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      ArrayList<Integer> uiStack = null;
      if (savedInstanceState != null) {
          uiStack = savedInstanceState.getIntegerArrayList("uistack");
      }
      LogWrapper.d(TAG, "onCreate() uiStack="+uiStack);
      if (uiStack != null) {
          initView(uiStack);
      }
      Log.d(TAG, "onRestoreInstanceState+()");

    }
    
    private void initView(ArrayList<Integer> uiStack) {
        mUIStack.clear();
        getTitleFragment();
        getMenuFragment();
        getMoreFragment().refreshMoreBtn(true);
        showView(R.id.view_home);
        
        mMenuView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mMenuViewHeiht = mMenuView.getMeasuredHeight();
        mControlView.setPadding(0, 0, 0, mMenuViewHeiht);
        mTitleView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mTitleViewHeight = mTitleView.getMeasuredHeight();
        mCityViewHeight = Util.dip2px(Globals.g_metrics.density, 30);
        mMapView.getPadding().top = mTitleViewHeight + mCityViewHeight;

        mMapView.setVisibility(View.VISIBLE);
        mTitleView.setVisibility(View.VISIBLE);
        mBodyView.setVisibility(View.VISIBLE);
        mMenuView.setVisibility(View.VISIBLE);
        mControlView.setVisibility(View.VISIBLE);
        Sphinx.this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
	protected Dialog onCreateDialog(int id) {
	    return getDialog(id);
	}
	
	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {
	    super.onPrepareDialog(id, dialog);
	    if (id == R.id.dialog_prompt_change_to_my_location_city) {
	        TextView messageTxv =(TextView)dialog.findViewById(R.id.message);
	        String currentCityName = Globals.g_Current_City_Info.getCName();
	        String locationCityName;
	        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
	        if (myLocationCityInfo != null) {
	            locationCityName = myLocationCityInfo.getCName();
	        } else {
	            locationCityName = currentCityName;
	        }
	        messageTxv.setText(getString(R.string.are_your_change_to_location_city, currentCityName, locationCityName));
            mActionLog.addAction(ActionLog.DIALOG, messageTxv.getText());
	    } else if (id == R.id.dialog_prompt_choose_city
	            || id == R.id.dialog_prompt_setting_location_first) {
            TextView messageTxv =(TextView)dialog.findViewById(R.id.message);
            mActionLog.addAction(ActionLog.DIALOG, messageTxv.getText());
	    } else if (id == R.id.dialog_prompt_setting_location) {
            mActionLog.addAction(ActionLog.DIALOG, getString(R.string.location_failed_and_jump_settings));
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
		try{
	    	switch (item.getItemId()) {
		    case CONFIG_SERVER:
		    	startActivityForResult(new Intent(this, ConfigActivity.class), CONFIG_SERVER_CODE);
		        return true;
		    	
		    case PROFILE:
		    	String profile="profile:";
		    	profile+="\ntiles network size:"+Profile.tiles_network_size+",avg tiles network time:"+(Profile.avgTilesNetwork()/(float)1000000);
		    	profile+="\nget tile buffer size:"+Profile.get_tile_buffer_size+",avg get tile buffer time:"+(Profile.avgGetTileBuffer()/(float)1000000);
		    	profile+="\ndecode byte array size:"+Profile.decode_byte_array_size+",avg decode byte array time:"+(Profile.avgDecodeByteArray()/(float)1000000);
		    	profile+="\ndraw size:"+Profile.draw_size+",avg draw time:"+(Profile.avgDraw()/(float)1000000);
		    	profile+="\nrotate size:"+Profile.rotate_size+",avg rotate time:"+(Profile.avgRotate()/(float)1000000);
		    	
		    	profile+="\ndraw method:"+(CONFIG.DRAW_BY_OPENGL?"opengl":"canvas")+",num of thread:"+CONFIG.TILE_THREAD_COUNT+",tile size:"+CONFIG.TILE_SIZE;
		    	profile+="\nnetwork type:"+((super.networkType==ConnectivityManager.TYPE_WIFI)?"wifi":"mobile")+",subtype:"+super.subNetworkType;
		    	Intent intent = new Intent(this, ProfileResultActivity.class);
		    	intent.putExtra(getPackageName()+".profile", profile);
		    	Profile.reset();
		    	startActivity(intent);
		    	return true;
		    }
		}catch(Exception e){
			e.printStackTrace();
			AppUtil.alert(e.getMessage(), this, "WARN");
		}
		return false;
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		super.onActivityResult(requestCode, resultCode, data);
		LogWrapper.d(TAG, "onActivityResult() requestCode="+requestCode+" resultCode="+resultCode+ "data="+data); 
		mOnActivityResultLoginBack = false;
		if (R.id.activity_help == requestCode) {
		    if (data != null) {
		        if (data.getBooleanExtra(Help.APP_FIRST_START, false)) {
                    mPreventShowChangeMyLocationDialog = false;
		            OnSetup();
		        } else if (data.getBooleanExtra(Help.APP_UPGRADE, false)) {
                    mPreventShowChangeMyLocationDialog = false;
                    checkLocationCity();
		        }
		    }
		} else if (R.id.activity_app_recommend == requestCode) {
        } else if (R.id.activity_change_city == requestCode) {
            if (data != null && RESULT_OK == resultCode) {
                mPreventShowChangeMyLocationDialog = true;
                changeCity(data.getIntExtra("cityId", Globals.g_Current_City_Info.getId()));
            }
        } else if (R.id.activity_setting_location == requestCode) {
        } else if (R.id.activity_setting == requestCode) {
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
        } else if (R.id.activity_map_download == requestCode) {
            
        } else if (R.id.activity_user_login == requestCode) {
            if (data != null) {
                loginBack(data);
                mOnActivityResultLoginBack = true;
            }
		} else if (R.id.activity_poi_comment == requestCode) {
			if (resultCode == RESULT_CANCELED
					&& data != null
					&& data.getBooleanExtra(REFRESH_POI_DETAIL, false)) {
				getPOIDetailFragment().refreshDetail();
			}
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
        
        if(requestCode == CONFIG_SERVER_CODE){
            if (resultCode == Activity.RESULT_OK) {
                try {
                    long tileThreadCount=data.getLongExtra(getPackageName()+".tile_thread_count", CONFIG.TILE_THREAD_COUNT);
                    boolean snapToClosestLevel=data.getBooleanExtra(getPackageName()+".snap_to_closest_zoomlevel", true);
                    long fadingTime=data.getLongExtra(getPackageName()+".fading_time", CONFIG.FADING_TIME);
                    long border=data.getLongExtra(getPackageName()+".border", CONFIG.BORDER);
                    boolean drawByOpengl=data.getBooleanExtra(getPackageName()+".draw_by_opengl", true);
                    
                    CONFIG.SNAP_TO_CLOSEST_ZOOMLEVEL=snapToClosestLevel;
                    CONFIG.FADING_TIME=(int)fadingTime;
                    CONFIG.BORDER=(int)border;
                                        
                    boolean restart=false;
                    
                    if((drawByOpengl ^ CONFIG.DRAW_BY_OPENGL) || (int)tileThreadCount!=CONFIG.TILE_THREAD_COUNT){
                        TKConfig.setPref(mContext, "draw_by_opengl", ""+drawByOpengl);
                        LogWrapper.i(TAG,"onDestroy draw by opengl:"+""+drawByOpengl);
                        
                        CONFIG.TILE_THREAD_COUNT=(int)tileThreadCount;
                        LogWrapper.i(TAG,"onDestroy tile thread count:"+""+tileThreadCount);
                        
                        restart=true;
                    }
                    
                    if(restart) this.finish();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    AppUtil.alert(e.getMessage(), this, "WARNING");
                                        
                }
            }
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
		Log.i(TAG,"onResume()");
		mActionLog.onResume();

        Globals.setConnectionFast(CommonUtils.isConnectionFast(this));
        Globals.getAsyncImageLoader().onResume();
        
        TKConfig.updateIMSI(mConnectivityManager);
        
        IntentFilter intentFilter= new IntentFilter(MapStatsService.ACTION_MAP_STATS_COMPLATE);
        registerReceiver(mMapStatsBroadcastReceiver, intentFilter);
        
        if (mActivityResult == false) {
            getDiscoverFragment().resetDataQuery();
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
        checkLocationCity();
	}
	
	private void checkLocationCity() {
	    CityInfo cityInfo = Globals.g_Current_City_Info;
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        final boolean gps = CommonUtils.checkGpsStatus(mContext);
        
        if (myLocationCityInfo != null) {
            if (myLocationCityInfo.getId() != cityInfo.getId()) {
                showPromptChangeToMyLocationCityDialog(myLocationCityInfo);
            }
        } else {
            if (!gps) {
                showPromptSettingLocationDialog();
            }
        }
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mZoomView.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onDestroy() {
        Globals.getAsyncImageLoader().onDestory();
        mTKLocationManager.onDestroy();
        mMapEngine.writeLastRegionIdList(mContext);
        if (mMapView != null) {
            MapText mapText = mMapView.getMapText();
            if (mapText != null) {
                Bitmap bm = mapText.bitmap;
                if (bm != null && bm.isRecycled() == false) {
                    bm.recycle();
                }
        }
        }
        Globals.getImageCache().stopWritingAndRemoveOldTiles();
		LogWrapper.i("Sphinx","onDestroy()");
        if (mSensorOrientation) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        mActionLog.onDestroy();
        mMenuFragment = null;
        mTitleFragment = null;
        mHomeFragment = null;
        mMoreFragment = null;
        mGoCommentFragment = null;
        mResultMapFragment = null;
        mFavoriteFragment = null;
        mHistoryFragment = null;
        mPOIDetailFragment = null;
        mPOIResultFragment = null;
        mPOIResultFragment2 = null;
        mPOIQueryFragment = null;
        mPOINearbyFragment = null;
        mTrafficDetailFragment = null;
        mTrafficResultFragment = null;
        mBuslineResultLineFragment = null;
        mBuslineResultStationFragment = null;
        mBuslineDetailFragment = null;
        mTrafficQueryFragment = null;
        mMyCommentListFragment = null;
        mUserHomeFragment = null;

        mDiscoverListFragment = null;
        mTuangouDetailFragment = null;
        mYanchuDetailFragment = null;
        mZhanlanDetailFragment = null;
        mDianyingDetailFragment = null;
        mDiscoverChildListFragment = null;
        mDiscoverFragment = null;
        Intent service = new Intent(Sphinx.this, MapStatsService.class);
        stopService(service);
        super.onDestroy();
        ActivityManager am = (ActivityManager)getSystemService(
                Context.ACTIVITY_SERVICE);
        am.restartPackage("com.tigerknows");
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
    private BroadcastReceiver mMapStatsBroadcastReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) { 
            getMoreFragment().refreshMoreBtn(false);
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

                BaseFragment baseFragment = getFragment(uiStackPeek());
                if (baseFragment != null && baseFragment.onKeyDown(keyCode, event)) {
                    return true;
                }
                
                if (mMenuView == null || mMenuView.getVisibility() != View.VISIBLE) {
                    return true;
                }
                
                mActionLog.addAction(ActionLog.KeyCodeBack);
                if (!uiStackBack()) {
                    if (mFromIntent || mSnapMap) {
                        Sphinx.this.finish();
                    } else {
                        CommonUtils.showNormalDialog(Sphinx.this,
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
        mScaleView = (ScaleView)findViewById(R.id.scale_view);
        mZoomView = (LinearLayout) findViewById(R.id.zoomview);
        mMapView = (MapView) findViewById(R.id.mapview);
        mPreviousNextView = findViewById(R.id.previous_next_view);
        mPreviousBtn=(Button)(findViewById(R.id.previous_btn));
        mNextBtn=(Button)(findViewById(R.id.next_btn));
        mLocationBtn=(ImageButton)(findViewById(R.id.location_btn));
        mDownloadView = (TextView)findViewById(R.id.download_txv);
        mCompassView = findViewById(R.id.compass_imv);
    }

    private void setListener() {
    }
    
    private void initIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Uri uri = intent.getData();
        if ("http".equals(intent.getScheme())) {
            initView(null);
            mFromIntent = true;
            try {
                String uriStr = uri.toString();
                String[] parms = uriStr.substring(uriStr.indexOf("?")+1).split("&");
                String[] parm = parms[0].split("=");
                POI poi = new POI();
                if ("latlon".equals(parm[0])) {
                    Position position = new Position(parm[1]);
                    mMapView.zoomTo(TKConfig.ZOOM_LEVEL_LOCATION, position);
                    poi.setPosition(position);
                    String name = mMapEngine.getPositionName(poi.getPosition(), (int)mMapView.getZoomLevel());
                    if (!TextUtils.isEmpty(name)) {
                        poi.setName(name);
                    } else {
                        poi.setName(mContext.getString(R.string.select_point));
                    }
                }
                try {
                    parm = parms[1].split("=");
                    if ("z".equals(parm[0])) {
                        int zoomLevel = Integer.parseInt(parm[1]);
                        mMapView.zoomTo(zoomLevel);
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
                        }
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (Util.inChina(poi.getPosition())) {
                    List<POI> list = new ArrayList<POI>();
                    list.add(poi);
                    showPOI(list, 0);
                    getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.MapPOI);
                    showView(R.id.view_result_map);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        } else if ("geo".equals(intent.getScheme())) {
            initView(null);
            mFromIntent = true;
            // geo:latitude,longitude
            // geo:latitude,longitude?z=zoom，z表示zoom级别，值为数字1到23
            // geo:0,0?q=my+street+address
            // geo:0,0?q=business+near+city
            String uriStr = uri.toString();
            POI poi = new POI();
            try {
                uriStr = uriStr.substring(uriStr.indexOf(":")+1);
                double lat = Double.parseDouble(uriStr.substring(0, uriStr.indexOf(",")));
                LogWrapper.d("Sphinx", "initIntent() lat="+lat);
                uriStr = uriStr.substring(uriStr.indexOf(",")+1);
                double lon;
                if (uriStr.indexOf("?") > -1) {
                    lon = Double.parseDouble(uriStr.substring(0, uriStr.indexOf("?")));
                    LogWrapper.d("Sphinx", "initIntent() lon="+lon);
                    mMapView.zoomTo(TKConfig.ZOOM_LEVEL_LOCATION, new Position(lat, lon));
                    poi.setPosition(new Position(lat, lon));
                    uriStr = uriStr.substring(uriStr.indexOf("?")+1);
                    String[] parm = uriStr.split("=");
                    if ("z".equals(parm[0])) {
                        mMapView.zoomTo(Integer.parseInt(parm[1]));
                    } else if ("q".equals(parm[0])) {
                        String keyword = parm[1];
                        if (!TextUtils.isEmpty(keyword)) {
                            DataQuery dataQuery = getPOIQuery(keyword);
                            queryStart(dataQuery);
                            ((POIResultFragment)getFragment(dataQuery.getTargetViewId())).setup();
                            showView(dataQuery.getTargetViewId());
                        }
                        return;
                    }
                } else {
                    lon = Double.parseDouble(uriStr);
                    poi.setPosition(new Position(lat, lon));
                    mMapView.zoomTo(TKConfig.ZOOM_LEVEL_LOCATION, new Position(lat, lon));
                }
                if (Util.inChina(poi.getPosition())) {
                    String name = mMapEngine.getPositionName(poi.getPosition(), (int)mMapView.getZoomLevel());
                    if (!TextUtils.isEmpty(name)) {
                        poi.setName(name);
                    } else {
                        poi.setName(mContext.getString(R.string.select_point));
                    }
                    List<POI> list = new ArrayList<POI>();
                    list.add(poi);
                    showPOI(list, 0);
                    getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.MapPOI);
                    showView(R.id.view_result_map); 
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
        int type = intent.getIntExtra("type", -1);
        if (type == 0) {
            initView(null);
            mSnapMap = true;
            POI poi = getPOI();
            if (poi.getSourceType() == POI.SOURCE_TYPE_MY_LOCATION) {
                poi.setName(mMapEngine.getPositionName(poi.getPosition(), (int)mMapView.getZoomLevel()));
                List<POI> list = new ArrayList<POI>();
                list.add(poi);
                showPOI(list, 0);
            } else {
                requestLocation();
            }
            getResultMapFragment().setData(mContext.getString(R.string.result_map), ActionLog.MapPOI);
            showView(R.id.view_result_map);
            return;
        } else if (type == 3) {
            initView(null);
            mSnapMap = true;
            showView(R.id.view_poi_query);
            return;
        }
        final String mimetype = intent.resolveType(this);
        if ("vnd.android.cursor.item/postal-address_v2".equals(mimetype)) {
            initView(null);
            mFromIntent = true;
            if (uri != null) {
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor!=null && cursor.getCount() > 0) {
                    if (uiStackPeek() != R.id.view_home) {
                        uiStackClose(new int[]{R.id.view_home});
                        showView(R.id.view_home);
                    }
                    cursor.moveToFirst();
                    
                    int city1 = mMapEngine.getCityid(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)));
                    int city2 = mMapEngine.getCityid(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)));
                    if (city1 > 0) {
                        changeCity(city1);
                    } else if (city2 > 0) {
                        changeCity(city2);
                    }
                    String keyword = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)) + " " + cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD));
                    if (!TextUtils.isEmpty(keyword)) {
                        DataQuery dataQuery = getPOIQuery(keyword);
                        queryStart(dataQuery);
                        ((POIResultFragment)getFragment(dataQuery.getTargetViewId())).setup();
                        showView(dataQuery.getTargetViewId());
                    }
                }
            }
            return;
        }
    }
    
    private DataQuery getPOIQuery(String keyword) {
        DataQuery poiQuery = new DataQuery(mContext);
        POI requestPOI = getPOI();
        int cityId = Globals.g_Current_City_Info.getId();
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataQuery.SERVER_PARAMETER_SIZE, String.valueOf(TKConfig.getPageSize()));
        criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
        criteria.put(DataQuery.SERVER_PARAMETER_CITY, String.valueOf(cityId));
        Position position = requestPOI.getPosition();
        if (position != null) {
            criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
        criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD_TYPE, DataQuery.KEYWORD_TYPE_INPUT);
        poiQuery.setup(criteria, cityId, uiStackPeek(), getPOIResultFragmentID(), null, false, false, requestPOI);
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
        
        if (intent != null) {
            int sourceViewId = intent.getIntExtra(BaseActivity.SOURCE_VIEW_ID, R.id.view_invalid);
            if (sourceViewId == R.id.activity_poi_comment) {
                TKConfig.setPref(this, TKConfig.PREFS_SHOW_UPGRADE_COMMENT_TIP, String.valueOf(MoreFragment.SHOW_COMMENT_TIP_TIMES));
                getMoreFragment().refreshMoreBtn(false);
                mHandler.post(mOnNewIntentStamp);
            
            // 登录之后的返回
            } else {
                loginBack(intent);
            }
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
                uiStackClearTop(R.id.view_more);
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
        int id = uiStackPeek();
        BaseFragment baseFragment = getFragment(id);
        if (baseFragment != null) {
            baseFragment.onPause();
        }

        if (null != mMapView && null != mMapEngine) {
            Position position = mMapView.getCenterPosition();
            // 地图显示的位置在当前城市范围内才更新最后一次位置信息
            CityInfo cityInfo = Globals.g_Current_City_Info;
            if (cityInfo != null
                    && mMapEngine.getCityId(position) == cityInfo.getId()) {
                int zoom = (int)mMapView.getZoomLevel();
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LON, String.valueOf(position.getLon()));
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LAT, String.valueOf(position.getLat()));
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(zoom));
            }
        }

        // 需要一直监听联想词下载服务的下载完成信息，来解压联想词。
        unregisterReceiver(mMapStatsBroadcastReceiver);
        
        Intent service = new Intent(Sphinx.this, SuggestLexiconService.class);
        stopService(service);
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        LogWrapper.i(TAG, "onStop");
        
        mActionLog.onStop();
    }
        
    public void snapMapView(SnapMap snapMap, Position position, MapScene mapScene) {
        mMapView.snapMapView(this, snapMap, position, mapScene);
    }
    
    public void changeCity(long cityId) {
        int id = (int)cityId;
        CityInfo cityInfo = getMapEngine().getCityInfo(id);
        changeCity(cityInfo);
    }
    
    public void changeCity(CityInfo cityInfo) {
        if (cityInfo.isAvailably()) {
            CityInfo currentCityInfo = Globals.g_Current_City_Info;
            
            int cityId = cityInfo.getId();
            if (currentCityInfo != null
                    && cityId == currentCityInfo.getId()) {
                mMapView.zoomTo(cityInfo.getLevel(), cityInfo.getPosition(), -1, null);
            } else {
                if (currentCityInfo == null) { // 刚打开软件时，Globals.g_Current_City_Info被初始化为null
                    Globals.g_Current_City_Info = cityInfo;
                }
                mMapView.centerOnPosition(cityInfo.getPosition(), cityInfo.getLevel(), true);
                updateCityInfo(cityInfo);
                checkCitySupportDiscover(cityId);
                mActionLog.addAction(ActionLog.LifecycleSelectCity, cityInfo.getCName());

                Position position = cityInfo.getPosition();
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LON, String.valueOf(position.getLon()));
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_LAT, String.valueOf(position.getLat()));
                TKConfig.setPref(mContext, TKConfig.PREFS_LAST_ZOOM_LEVEL, String.valueOf(cityInfo.getLevel()));

                HistoryWordTable.readHistoryWord(mContext, cityId, HistoryWordTable.TYPE_POI);
            }
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
        if (myLocationCityInfo != null && myLocationCityInfo.getId() == Globals.g_Current_City_Info.getId()) {
            poi.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
            if (needPosition) {
                poi.setPosition(myLocationCityInfo.getPosition());
            }
            poi.setName(mContext.getString(R.string.my_location));
        } else {
            CityInfo cityInfo = Globals.g_Current_City_Info;
            poi.setSourceType(POI.SOURCE_TYPE_CITY_CENTER);
            poi.setName(cityInfo.getCName());
        }
        return poi;
    }
    
    public ResolveInfo getSmsResolveInfo() {
        if (mSMSResolveInfo == null) {
            mSMSResolveInfo = CommonUtils.getSmsApp(mContext);
        }
        
        return mSMSResolveInfo;
    }
    
    public void onMapCenterChanged() {
        CityInfo cityInfo = mMapEngine.getCityInfo(mMapView.getCenterCityId());
        if (cityInfo != null) {
            mMapEngine.setLastCityId(cityInfo.getId());
            if (!mViewedCityInfoList.contains(cityInfo)) {
                mViewedCityInfoList.add(cityInfo);
            }
        }
        if (uiStackPeek() != R.id.view_traffic_query) {
            return;
        }
        LogWrapper.d(TAG, "onMapCenterChanged()");  
        
    	getTrafficQueryFragment().onMapCenterChanged(cityInfo);
    }
    
    public void onMapTouch(boolean isShowTip) {
    	if (uiStackPeek() != R.id.view_traffic_query) {
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
        
        if (baseQuery instanceof UserLogon) {
            UserLogonModel userLogonModel = ((UserLogon) baseQuery).getUserLogonModel();
            if (userLogonModel != null) {
                Globals.g_User_Logon_Model = userLogonModel;
            }
            getMoreFragment().refreshMoreBtn(false);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void showPOI(List dataList, int index) {
        try {
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
            icon = icon.clone();
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
            mMapView.zoomTo(fitZoomLevle, srcreenPosition, -1, null);
            mPreviousNextView.setVisibility(View.VISIBLE);
        } else {
            mPreviousNextView.setVisibility(View.INVISIBLE);
            mMapView.zoomTo(TKConfig.ZOOM_LEVEL_POI, srcreenPosition, -1, null);
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
            AppUtil.alert(e.getMessage(), this, "WARNING");
        }
        mPreviousNextView.setVisibility(View.INVISIBLE);
    }
    
    public ViewGroup getControlView() {
        return mControlView;
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

    private void showInfoWindow(OverlayItem overlayItem) {
        
        if (overlayItem == null) {
        	return;
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
                if (mInfoWindowTuangouDetail == null) {
                    mInfoWindowTuangouDetail = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_tuangou_detail, null);
                }
                
                TextView nameTxv=(TextView)mInfoWindowTuangouDetail.findViewById(R.id.name_txv);
                TextView addressTxv= (TextView)mInfoWindowTuangouDetail.findViewById(R.id.address_txv);
                Button trafficBtn=(Button)mInfoWindowTuangouDetail.findViewById(R.id.traffic_btn);

                nameTxv.setText(poi.getName());
                addressTxv.setText(poi.getAddress());
                int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(96));
                layoutInfoWindow(nameTxv, max);
                layoutInfoWindow(addressTxv, max);
                
                ViewGroup bodyView=(ViewGroup)mInfoWindowTuangouDetail.findViewById(R.id.body_view);
                bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
                
                mInfoWindowTrafficButtonListener.poi = poi;
                mInfoWindowTrafficButtonListener.actionLog = ActionLog.AboutUs;
                trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
                
                infoWindow.setViewGroup(mInfoWindowTuangouDetail);
            } else if (sourceType == POI.SOURCE_TYPE_LONG_CLICK_SELECT_POINT) {
                if (mInfoWindowLongClick == null) {
                    mInfoWindowLongClick = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_long_click, null);
                }
                
                TextView nameTxv=(TextView)mInfoWindowLongClick.findViewById(R.id.name_txv);
                Button startBtn= (Button)mInfoWindowLongClick.findViewById(R.id.start_btn);
                Button endBtn=(Button)mInfoWindowLongClick.findViewById(R.id.end_btn);
                
                nameTxv.setText(poi.getName());
                
                int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(128));
                layoutInfoWindow(nameTxv, max);
                
                ViewGroup bodyView=(ViewGroup)mInfoWindowLongClick.findViewById(R.id.body_view);
                bodyView.setOnTouchListener(mInfoWindowBodyViewListener);

                mInfoWindowLongStartListener.actionLog = ActionLog.MapSelectStart;
                mInfoWindowLongStartListener.poi = poi;
                mInfoWindowLongStartListener.index = TrafficQueryFragment.START;
                startBtn.setOnTouchListener(mInfoWindowLongStartListener);
                
                mInfoWindowLongEndListener.actionLog = ActionLog.MapSelectEnd;
                mInfoWindowLongEndListener.poi = poi;
                mInfoWindowLongEndListener.index = TrafficQueryFragment.END;
                endBtn.setOnTouchListener(mInfoWindowLongEndListener);
                
                infoWindow.setViewGroup(mInfoWindowLongClick);
            } else if (sourceType != POI.SOURCE_TYPE_CLICK_SELECT_POINT
                    && sourceType != POI.SOURCE_TYPE_MY_LOCATION) {
                if (mInfoWindowPOI == null) {
                    mInfoWindowPOI = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_poi, null);
                }
                
                TextView nameTxv=(TextView)mInfoWindowPOI.findViewById(R.id.name_txv);
                RatingBar starsRtb = (RatingBar) mInfoWindowPOI.findViewById(R.id.stars_rtb);
                TextView priceTxv= (TextView)mInfoWindowPOI.findViewById(R.id.price_txv);
                Button trafficBtn=(Button)mInfoWindowPOI.findViewById(R.id.traffic_btn);
                
                nameTxv.setText(poi.getName());
                starsRtb.setRating(poi.getGrade()/2.0f);
                int money = poi.getPerCapity();
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
                mInfoWindowTrafficButtonListener.actionLog = ActionLog.AboutUs;
                trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
                
                infoWindow.setViewGroup(mInfoWindowPOI);
            } else {
                infoWindow.setMessage(overlayItem.getMessage());
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

            if (mInfoWindowYanchuList == null) {
                mInfoWindowYanchuList = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_yanchu_list, null);
            }
            
            ImageView pictureImv = (ImageView)mInfoWindowYanchuList.findViewById(R.id.icon_imv);
            TextView nameTxv=(TextView)mInfoWindowYanchuList.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) mInfoWindowYanchuList.findViewById(R.id.stars_rtb);
            Button trafficBtn=(Button)mInfoWindowYanchuList.findViewById(R.id.traffic_btn);
            
            nameTxv.setText(name);
            starsRtb.setProgress((int)rating);
            
            int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(148));
            layoutInfoWindow(nameTxv, max);
            
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, getResultMapFragment().toString());
                if(drawable != null) {
                	//To prevent the problem of size change of the same pic 
                	//After it is used at a different place with smaller size
                	pictureImv.setBackgroundDrawable(null);
                	pictureImv.setBackgroundDrawable(drawable);
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture_dianying);
                }
            } else {
                pictureImv.setBackgroundResource(R.drawable.bg_picture_dianying);
            }
            
            ViewGroup bodyView=(ViewGroup)mInfoWindowYanchuList.findViewById(R.id.body_view);
            bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
            
            mInfoWindowTrafficButtonListener.poi = poi;
            mInfoWindowTrafficButtonListener.actionLog = ActionLog.AboutUs;
            trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
            
            infoWindow.setViewGroup(mInfoWindowYanchuList);
        } else if (object instanceof Tuangou) {
            POI poi;
            TKDrawable tkDrawable; 
            Tuangou target = (Tuangou) object;
            poi = target.getPOI();
            tkDrawable = target.getPictures();
            String price = target.getPrice();

            if (mInfoWindowTuangouList == null) {
                mInfoWindowTuangouList = (LinearLayout) mLayoutInflater.inflate(R.layout.info_window_tuangou_list, null);
            }
            
            ImageView pictureImv = (ImageView)mInfoWindowTuangouList.findViewById(R.id.icon_imv);
            TextView nameTxv=(TextView)mInfoWindowTuangouList.findViewById(R.id.name_txv);
            TextView priceTxv= (TextView)mInfoWindowTuangouList.findViewById(R.id.price_txv);
            Button trafficBtn=(Button)mInfoWindowTuangouList.findViewById(R.id.traffic_btn);
            
            nameTxv.setText(target.getShortDesc());
            priceTxv.setText(price);
            
            int max = Globals.g_metrics.widthPixels - (int)(Globals.g_metrics.density*(186));
            layoutInfoWindow(nameTxv, max);
            
            if (tkDrawable != null) {
                Drawable drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, getResultMapFragment().toString());
                if(drawable != null) {
                	//To prevent the problem of size change of the same pic 
                	//After it is used at a different place with smaller size
                    pictureImv.setBackgroundDrawable(null);
                    pictureImv.setBackgroundDrawable(drawable);
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture_tuangou);
                }
            } else {
                pictureImv.setBackgroundResource(R.drawable.bg_picture_tuangou);
            }
            
            ViewGroup bodyView=(ViewGroup)mInfoWindowTuangouList.findViewById(R.id.body_view);
            bodyView.setOnTouchListener(mInfoWindowBodyViewListener);
            
            mInfoWindowTrafficButtonListener.poi = poi;
            mInfoWindowTrafficButtonListener.actionLog = ActionLog.AboutUs;
            trafficBtn.setOnTouchListener(mInfoWindowTrafficButtonListener);
            
            infoWindow.setViewGroup(mInfoWindowTuangouList);
        } else {
        	infoWindow.setMessage(overlayItem.getMessage());
        }

        infoWindow.setOffset(new XYFloat((float)(overlayItem.getIcon().getOffset().x - overlayItem.getIcon().getSize().x/2), 
        		(float)(overlayItem.getIcon().getOffset().y)),
                overlayItem.getRotationTilt());
        infoWindow.setVisible(true);
        
        mMapView.refreshMap();
    }
    
    private void infoWindowClicked() {
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
            // TODO: 什么也不用做
        } else if (overlayItem != null) {
            infoWindow.setVisible(false);
            if (overlayName.equals(ItemizedOverlay.POI_OVERLAY)) {
                mActionLog.addAction(ActionLog.MapPOIBubble);
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
                        getPOIDetailFragment().setData(target);
                        if (uiStackContains(R.id.view_poi_detail)) {
                            dismissView(R.id.view_result_map);
                        } else {
                            showView(R.id.view_poi_detail);
                        }
                    }
                } else if (object instanceof Tuangou) {
                    Tuangou target = (Tuangou) object;
                    if (uiStackContains(R.id.view_tuangou_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_tuangou_detail);
                    }
                    getTuangouDetailFragment().setData(target);
                } else if (object instanceof Dianying) {
                    Dianying target = (Dianying) object;
                    if (uiStackContains(R.id.view_dianying_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_dianying_detail);
                    }
                    getDianyingDetailFragment().setData(target);
                } else if (object instanceof Zhanlan) {
                    Zhanlan target = (Zhanlan) object;
                    if (uiStackContains(R.id.view_zhanlan_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_zhanlan_detail);
                    }
                    getZhanlanDetailFragment().setData(target);
                } else if (object instanceof Yanchu) {
                    Yanchu target = (Yanchu) object;
                    if (uiStackContains(R.id.view_yanchu_detail)) {
                        dismissView(R.id.view_result_map);
                    } else {
                        showView(R.id.view_yanchu_detail);
                    }
                    getYanchuDetailFragment().setData(target);
                }
            } else if (overlayName.equals(ItemizedOverlay.TRAFFIC_OVERLAY)) {
                mActionLog.addAction(ActionLog.MapTrafficBubble);
                if (uiStackContains(R.id.view_traffic_result_detail)) {
                    dismissView(R.id.view_result_map);
                } else {
                    showView(R.id.view_traffic_result_detail);
                }
            } else if (overlayName.equals(ItemizedOverlay.LINE_OVERLAY)) {
                mActionLog.addAction(ActionLog.MapTrafficBubble);
                if (uiStackContains(R.id.view_busline_result_detail)) {
                    dismissView(R.id.view_result_map);
                } else {
                    showView(R.id.view_busline_result_detail);
                }
            }
        }
        setTouchMode(TouchMode.NORMAL);
    }
    
    private void hideInfoWindow(String overlayName) {
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
        Globals.g_Current_City_Info = cityInfo;
        String cName = cityInfo.getCName();
        getMoreFragment().refreshCity(cName);
        getHomeFragment().refreshCity(cName);
        getTrafficQueryFragment().refreshCity(cName);
        getHomeFragment().refreshLocationView();
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
        final boolean gps = CommonUtils.checkGpsStatus(mContext);

        if (myPosition != null) {
            int cityId = mMapEngine.getCityId(myPosition);
            CityInfo cityInfo = mMapEngine.getCityInfo(cityId);
            cityInfo.setPosition(myPosition);
            cityInfo.setLevel(TKConfig.ZOOM_LEVEL_LOCATION);
            changeCity(cityInfo);
        } else {
            if (gps) {
                showLocationDialog(R.id.dialog_prompt_choose_city);
            } else {
                showLocationDialog(R.id.dialog_prompt_setting_location_first);
            }
        }
    }
    
    private boolean showPromptChangeToMyLocationCityDialog(final CityInfo locationCity) {
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
            mActionLog.addAction(ActionLog.ChangeToMyLocationCityDialog);
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
    
    private boolean showLocationDialog(int id) {
        if (mFromIntent || mSnapMap) {
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
        }
        
        if (id != R.id.dialog_prompt_setting_location) {
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
    public TKAsyncTask queryStart(List<BaseQuery> baseQueryList) {        
        TKAsyncTask tkAsyncTask = new TKAsyncTask(Sphinx.this, baseQueryList, Sphinx.this, null);
        tkAsyncTask.execute();
        return tkAsyncTask;
    }
    
    public void queryStart(BaseQuery baseQuery) {        
        queryStart(baseQuery, true);
    }
    
    public void queryStart(BaseQuery baseQuery, boolean isUITask) {     
    	queryStart(baseQuery, isUITask, true);
    }
    
    public void queryStart(BaseQuery baseQuery, boolean isUITask, boolean cancelable) {     
        TKAsyncTask tkAsyncTask = new TKAsyncTask(Sphinx.this, baseQuery, Sphinx.this, null, cancelable);
        tkAsyncTask.execute();
        int targetViewId = baseQuery.getTargetViewId();
        BaseFragment baseFragment = getFragment(targetViewId);
        if (baseFragment != null) {
            baseFragment.mTkAsyncTasking = tkAsyncTask;
            baseFragment.mBaseQuerying = baseQuery;
        }
    }
    
    public int getPOIResultFragmentID() {
        int targetDialogId = R.id.view_invalid;
        if (!uiStackContains(R.id.view_poi_result)) {
            targetDialogId = R.id.view_poi_result;
        } else if (!uiStackContains(R.id.view_poi_result2)) {
            targetDialogId = R.id.view_poi_result2;
        }
        
        return targetDialogId;
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
            boolean result = false;
            for(int i : mUIStack) {
                if (i == id) {
                    result = true;
                }
            }
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
    
    private boolean uiStackRemove(int id) {
        synchronized (mUILock) {
            boolean result = false;
            int index = 0;
            for(int i : mUIStack) {
                if (i == id) {
                    break;
                }
                index++;
            }
            
            int size = mUIStack.size();
            if (size > 0 && index < size) {
                mUIStack.remove(index);
                result = true;
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
                            id == R.id.view_tuangou_detail || 
                            id == R.id.view_dianying_detail || 
                            id == R.id.view_yanchu_detail ||
                            id == R.id.view_zhanlan_detail) {
                        uiStackRemove(R.id.view_result_map);
                    }
                }

                id = uiStackPeek();
                BaseFragment fragment = getFragment(id);
                
                if (fragment != null) {
                    fragment.onResume();
                    result = true;
                } else {
                    Dialog backDialog = getDialog(id);
                    if (backDialog != null) {
                        if (!backDialog.isShowing()) {
                            showView(id);
                            result = true;
                        } else if (backDialog instanceof BaseDialog) {
                            ((BaseDialog)backDialog).onResume();
                            result = true;
                        }
                    }
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
            } else {
                Dialog dialog = getDialog(id);
                if (dialog != null) {
                    if (dialog.isShowing()) {
                        dismissDialog(id);
                        result = true;
                    }
                }
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
                    } else {
                        Dialog dialog = getDialog(id);
                        if (dialog != null) {
                            if (dialog.isShowing()) {
                                dismissDialog(id);
                            }
                        }
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
            } else {
                Dialog dialog = getDialog(preferTop);
                if (dialog != null) {
                    if (dialog instanceof BaseDialog) {
                        ((BaseDialog)dialog).onResume();
                        result = true;
                    }
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
        if (TKConfig.getPref(this, key) == null) {
            Intent intent = new Intent();
            intent.putExtra(Hint.LAYOUT_RES_ID, layoutResId);
            showView = showView(R.id.activity_hint, intent);
            TKConfig.setPref(mThis, key, "1");
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
                intent.setClass(this, Hint.class);
                startActivityForResult(intent, R.id.activity_hint);
                return true;
            } else if (R.id.activity_help == viewId) {
                intent.setClass(this, Help.class);
                startActivityForResult(intent, R.id.activity_help);
                return true;
            } else if (R.id.activity_about_us == viewId) {
                intent.setClass(this, AboutUs.class);
                startActivityForResult(intent, R.id.activity_about_us);
                return true;
            } else if (R.id.activity_app_recommend == viewId) {
                intent.setClass(this, AppRecommend.class);
                startActivityForResult(intent, R.id.activity_app_recommend);
                return true;
            } else if (R.id.activity_poi_error_recovery == viewId) {
                intent.setClass(this, POIErrorRecovery.class);
                startActivityForResult(intent, R.id.activity_poi_error_recovery);
                return true;
            } else if (R.id.activity_traffic_error_recovery == viewId) {
                intent.setClass(this, TransferErrorRecovery.class);
                startActivityForResult(intent, R.id.activity_traffic_error_recovery);
                return true;
            } else if (R.id.activity_feedback == viewId) {
                intent.setClass(this, Feedback.class);
                startActivityForResult(intent, R.id.activity_feedback);
                return true;
            } else if (R.id.activity_change_city == viewId) {
                intent.setClass(this, ChangeCity.class);
                startActivityForResult(intent, R.id.activity_change_city);
                return true;
            } else if (R.id.activity_setting == viewId) {
                intent.setClass(this, Setting.class);
                startActivityForResult(intent, R.id.activity_setting);
                return true;
            } else if (R.id.activity_poi_comment == viewId) {
                intent.setClass(this, POIComment.class);
                startActivityForResult(intent, R.id.activity_poi_comment);
                return true;
            } else if (R.id.activity_poi_comment_list == viewId) {
                intent.setClass(this, POICommentList.class);
                startActivityForResult(intent, R.id.activity_poi_comment_list);
                return true;
            } else if (R.id.activity_map_download == viewId) {
                ArrayList<Integer> cityIdList = new ArrayList<Integer>();
                List<CityInfo> cityInfoList = mViewedCityInfoList; 
                for(CityInfo cityInfo : cityInfoList) {
                    cityIdList.add(cityInfo.getId());
                }
                cityInfoList.clear();
                intent.putIntegerArrayListExtra("cityIdList", cityIdList);
                intent.setClass(this, MapDownload.class);
                startActivityForResult(intent, R.id.activity_map_download);
                return true;
            } else if (R.id.activity_user_login == viewId) {
                intent.setClass(this, UserLoginActivity.class);
                startActivityForResult(intent, R.id.activity_user_login);
                return true;
            } else if (R.id.activity_tuangou_shangjia_list == viewId) {
                intent.setClass(this, TuangouShangjiaListActivity.class);
                startActivityForResult(intent, R.id.activity_tuangou_shangjia_list);
                return true;
            } else if (R.id.activity_browser == viewId) {
                intent.setClass(this, BrowserActivity.class);
                startActivityForResult(intent, R.id.activity_browser);
                return true;
            } else if (R.id.activity_add_merchant == viewId) {
                intent.setClass(this, AddMerchant.class);
                startActivityForResult(intent, R.id.activity_add_merchant);
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
            }
            mUIProcessing = true;
            boolean show = false;
            int currentId = uiStackPeek();
            if (currentId != viewId) {
                BaseFragment baseFragment = getFragment(viewId);
                
                if (baseFragment != null) {
                    
                    BaseFragment backBaseFragment = getFragment(currentId);
                    if (backBaseFragment != null) {
                        backBaseFragment.onPause();
                    } else {
                        Dialog backDialog = getDialog(currentId);
                        if (backDialog != null) {
                            if (backDialog instanceof BaseDialog) {
                                ((BaseDialog)backDialog).onPause();
                            }
                        }
                    }
                    
                    baseFragment.show();
                    show = true;
                } else {
                    Dialog  dialog = getDialog(viewId);
                    if (dialog != null) {
                        BaseFragment backFragment = getFragment(currentId);
                        if (backFragment != null) {
                            backFragment.onPause();
                        } else {
                            Dialog backDialog = getDialog(currentId);
                            if (backDialog != null) {
                                if (backDialog instanceof BaseDialog) {
                                    ((BaseDialog)backDialog).onPause();
                                }
                            }
                        }
                        
                        dialog.show();
                        show = true;
                    }
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
            } else {
                Dialog dialog = getDialog(viewId);
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    dismiss = true;
                }
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
    private TrafficAlternativesDialog mTrafficAlternativesDialog = null;
    private FetchFavoriteDialog mFetchFavoriteDialog = null;
    
    public Dialog getDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case R.id.dialog_traffic_alternatives:
                dialog = getTrafficAlternativesDialog();
                break;

            case R.id.dialog_fetch_favorite_poi:
                dialog = getFetchFavoriteDialog();
                break;
                
            case R.id.dialog_prompt_change_to_my_location_city:
                dialog = CommonUtils.getDialog(mThis,
                            getString(R.string.prompt),
                            getString(R.string.are_your_change_to_location_city),
                            null,
                            getString(R.string.yes),
                            getString(R.string.no),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    switch (id) {                                            
                                        case DialogInterface.BUTTON_POSITIVE:
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
                                                String mylocationCName = locationCity.getCName();
                                                mActionLog.addAction(ActionLog.ChangeToMyLocationCityDialogYes, mylocationCName);
                                                uiStackClose(null);
                                                showView(R.id.view_home);
                                                changeCity(locationCity);
                                                mActionLog.addAction(ActionLog.LifecycleSelectCity, Globals.g_Current_City_Info.getCName());
                                            }
                                            break;          
                                            
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            mActionLog.addAction(ActionLog.ChangeToMyLocationCityDialogNo);
                                            break;    
                                            
                                        default:
                                            break;
                                    }
                                }
                            });
                break;
                
            case R.id.dialog_prompt_setting_location:
                View settingLocationView = mLayoutInflater.inflate(R.layout.alert_setting_location, null, false);
                dialog = CommonUtils.getDialog(Sphinx.this,
                        getString(R.string.prompt),
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

                
                final CheckBox checkChb = (CheckBox) dialog.findViewById(R.id.check_chb);
                checkChb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    
                    @Override
                    public void onCheckedChanged(CompoundButton arg0, boolean checked) {
                        TKConfig.setPref(Sphinx.this, TKConfig.PREFS_SHOW_LOCATION_SETTINGS_TIP, checked ? "1" : "");
                    }
                });
                break;
                
            case R.id.dialog_prompt_setting_location_first:
                dialog = CommonUtils.getDialog(Sphinx.this,
                        mContext.getString(R.string.prompt),
                        mContext.getString(R.string.location_failed_and_jump_settings),
                        null,
                        mContext.getString(R.string.confirm),
                        mContext.getString(R.string.settings),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                switch (id) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        showLocationDialog(R.id.dialog_prompt_choose_city);
                                        dialog.dismiss();
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        showView(R.id.activity_setting_location);
                                        dialog.dismiss();
                                        break;

                                    default:
                                        break;
                                }
                            }
                        });
                break;
                
            case R.id.dialog_prompt_choose_city:
                dialog = CommonUtils.getDialog(Sphinx.this,
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
                                        showView(R.id.activity_change_city);
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
    
    public FetchFavoriteDialog getFetchFavoriteDialog() {
        if (mFetchFavoriteDialog == null) {
            mFetchFavoriteDialog = new FetchFavoriteDialog(Sphinx.this);
            mFetchFavoriteDialog.setId(R.id.dialog_fetch_favorite_poi);
        }
        return mFetchFavoriteDialog;
    }
    
    public TrafficAlternativesDialog getTrafficAlternativesDialog() {
        if (mTrafficAlternativesDialog == null) {
            mTrafficAlternativesDialog = new TrafficAlternativesDialog(Sphinx.this);
            mTrafficAlternativesDialog.setId(R.id.dialog_traffic_alternatives);
        }
        return mTrafficAlternativesDialog;
    }
    
    // TODO: get dialog end
    
    // TODO: get fragment start
    private MenuFragment mMenuFragment;
    private TitleFragment mTitleFragment;
    private HomeFragment mHomeFragment;
    private MoreFragment mMoreFragment;
    private GoCommentFragment mGoCommentFragment;
    private ResultMapFragment mResultMapFragment;
    private FavoriteFragment mFavoriteFragment;
    private HistoryFragment mHistoryFragment;
    private POIDetailFragment mPOIDetailFragment;
    private POIResultFragment mPOIResultFragment;
    private POIResultFragment mPOIResultFragment2;
    private POIQueryFragment mPOIQueryFragment;
    private POINearbyFragment mPOINearbyFragment;
    private TrafficDetailFragment mTrafficDetailFragment = null;
    private TrafficResultFragment mTrafficResultFragment = null;
    private BuslineResultLineFragment mBuslineResultLineFragment = null;
    private BuslineResultStationFragment mBuslineResultStationFragment = null;
    private BuslineDetailFragment mBuslineDetailFragment = null;
    private TrafficQueryFragment mTrafficQueryFragment = null;
    private MyCommentListFragment mMyCommentListFragment;
    private UserHomeFragment mUserHomeFragment;

    private DiscoverListFragment mDiscoverListFragment;
    private TuangouDetailFragment mTuangouDetailFragment;
    private YanchuDetailFragment mYanchuDetailFragment;
    private ZhanlanDetailFragment mZhanlanDetailFragment;
    private DianyingDetailFragment mDianyingDetailFragment;
    private DiscoverChildListFragment mDiscoverChildListFragment;
    private DiscoverFragment mDiscoverFragment;
    
    public BaseFragment getFragment(int id) {
        BaseFragment baseFragment = null;

        switch (id) {                
            case R.id.view_more:
                baseFragment = getMoreFragment();
                break;
                
            case R.id.view_home:
                baseFragment = getHomeFragment();
                break;
                
            case R.id.view_result_map:
                baseFragment = getResultMapFragment();
                break;
                
            case R.id.view_favorite:
                baseFragment = getFavoriteFragment();
                break;
                
            case R.id.view_history:
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
                
            case R.id.view_poi_query:
                baseFragment = getPOIQueryFragment();
                break;
                
            case R.id.view_poi_nearby:
                baseFragment = getPOINearbyFragment();
                break;
                
            case R.id.view_traffic_result_transfer:
                baseFragment = getTrafficResultFragment();
                break;
                
            case R.id.view_traffic_result_detail:
                baseFragment = getTrafficDetailFragment();
                break;
                
            case R.id.view_busline_line_result:
                baseFragment = getBuslineResultLineFragment();
                break;
                
            case R.id.view_busline_station_result:
                baseFragment = getBuslineResultStationFragment();
                break;
                
            case R.id.view_busline_result_detail:
                baseFragment = getBuslineDetailFragment();
                break;
                
            case R.id.view_traffic_query:
                baseFragment = getTrafficQueryFragment();
                break;    
                
            case R.id.view_user_home:
                baseFragment = getUserHomeFragment();
                break;
                
            case R.id.view_my_comment_list:
                baseFragment = getMyCommentListFragment();
                break;
                
            case R.id.view_go_comment:
                baseFragment = getGoCommentFragment();
                break;
                
            case R.id.view_discover_list:
                baseFragment = getDiscoverListFragment();
                break;
                
            case R.id.view_tuangou_detail:
                baseFragment = getTuangouDetailFragment();
                break;
                
            case R.id.view_discover_child_list:
                baseFragment = getDiscoverChildListFragment();
                break;
                
            case R.id.view_discover:
                baseFragment = getDiscoverFragment();
                break;
            case R.id.view_yanchu_detail:
                baseFragment = getYanchuDetailFragment();
                break;
            case R.id.view_dianying_detail:
            	baseFragment = getDianyingDetailFragment();
                break;
            case R.id.view_zhanlan_detail:
                baseFragment = getZhanlanDetailFragment();
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
    
    public HomeFragment getHomeFragment() {
        synchronized (mUILock) {
            if (mHomeFragment == null) {
                HomeFragment homeFragment = new HomeFragment(Sphinx.this);
                homeFragment.setId(R.id.view_home);
                homeFragment.onCreate(null);
                mHomeFragment = homeFragment;
            }
            return mHomeFragment;
        }
    }
    
    public MoreFragment getMoreFragment() {
        synchronized (mUILock) {
            if (mMoreFragment == null) {
                MoreFragment moreFragment = new MoreFragment(Sphinx.this);
                moreFragment.setId(R.id.view_more);
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
    
    public FavoriteFragment getFavoriteFragment() {
        synchronized (mUILock) {
            if (mFavoriteFragment == null) {
                FavoriteFragment favoriteFragment = new FavoriteFragment(Sphinx.this);
                favoriteFragment.setId(R.id.view_favorite);
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
                historyFragment.setId(R.id.view_history);
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
    
    public POIQueryFragment getPOIQueryFragment() {
        synchronized (mUILock) {
            if (mPOIQueryFragment == null) {
                POIQueryFragment poiQueryFragment = new POIQueryFragment(Sphinx.this);
                poiQueryFragment.setId(R.id.view_poi_query);
                poiQueryFragment.onCreate(null);
                mPOIQueryFragment = poiQueryFragment;
            }
            return mPOIQueryFragment;
        }
    }
    
    public POINearbyFragment getPOINearbyFragment() {
        synchronized (mUILock) {
            if (mPOINearbyFragment == null) {
                POINearbyFragment poiNearbyFragment = new POINearbyFragment(Sphinx.this);
                poiNearbyFragment.setId(R.id.view_poi_nearby);
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
                buslineResultLineFragment.setId(R.id.view_busline_line_result);
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
                buslineResultStationFragment.setId(R.id.view_busline_station_result);
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
                buslineDetailFragment.setId(R.id.view_busline_result_detail);
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
                trafficQueryFragment.setId(R.id.view_traffic_query);
                trafficQueryFragment.onCreate(null);
                mTrafficQueryFragment = trafficQueryFragment;
            }
            return mTrafficQueryFragment;
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
                myCommentListFragment.setId(R.id.view_my_comment_list);
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
                goCommentFragment.setId(R.id.view_go_comment);
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
                tuangouDetailFragment.setId(R.id.view_tuangou_detail);
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
                yanchuDetailFragment.setId(R.id.view_yanchu_detail);
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
                zhanlanDetailFragment.setId(R.id.view_zhanlan_detail);
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
                dianyingDetailFragment.setId(R.id.view_dianying_detail);
                dianyingDetailFragment.onCreate(null);
                mDianyingDetailFragment = dianyingDetailFragment;
            }
            return mDianyingDetailFragment;
        }
    }

    public DiscoverFragment getDiscoverFragment() {
        synchronized (mUILock) {
            if (mDiscoverFragment == null) {
                DiscoverFragment discoverFragment = new DiscoverFragment(Sphinx.this);
                discoverFragment.setId(R.id.view_discover);
                discoverFragment.onCreate(null);
                mDiscoverFragment = discoverFragment;
            }
            return mDiscoverFragment;
        }
    }
    // TODO: get fragment end

    // TODO: my location begin    
    private boolean mPreventShowChangeMyLocationDialog = false;
    private MyLocation mMyLocation;
    private ItemizedOverlay mMyLocationOverlay;
    private Runnable mLocationChangedRun = new Runnable() {
        
        @Override
        public void run() {
            Position myLocationPosition = null;
            CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
            if (myLocationCityInfo != null) {
                myLocationPosition = myLocationCityInfo.getPosition();
            }
            final Position myPosition = myLocationPosition;
            updateMyLocationOverlay(myPosition);  
            if (uiStackPeek() == R.id.view_home || uiStackPeek() == R.id.view_discover) {
                getHomeFragment().refreshLocationView();
            }
            
            if (myPosition != null) {
                if (MyLocation.MODE_NONE == mMyLocation.mode) {
                    updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                    mMapView.zoomTo(ZOOM_LEVEL_LOCATION, myPosition);
                } else if (MyLocation.MODE_NAVIGATION == mMyLocation.mode || MyLocation.MODE_ROTATION == mMyLocation.mode) {
                    mMapView.panToPosition(myPosition);
                }
            } else {
                resetLoactionButtonState();
            }
            
            if (Globals.g_My_Location_State == Globals.LOCATION_STATE_FIRST_SUCCESS
                    && myLocationCityInfo != null) {
                CityInfo currentCity = Globals.g_Current_City_Info;
                if (currentCity.getId() != myLocationCityInfo.getId()) {
                    showPromptChangeToMyLocationCityDialog(myLocationCityInfo);
                }
            }
        }
    };
    
    public static class MyLocationListener implements TKLocationListener {
        
        private Activity activity;
        private Runnable runnable;
        public MyLocationListener(Activity activity, Runnable runnable) {
            this.activity = activity;
            this.runnable = runnable;
        }

        @Override
        public void onLocationChanged(final Location location) {  
            if (location != null) {
                Position myLocationPosition = new Position(location.getLatitude(), location.getLongitude());
                MapEngine mapEngine = MapEngine.getInstance();
                myLocationPosition = mapEngine.latlonTransform(myLocationPosition);
                if (myLocationPosition == null) {
                    Globals.g_My_Location = null;
                    Globals.g_My_Location_City_Info = null;
                } else {
                    myLocationPosition.setAccuracy(location.getAccuracy());
                    myLocationPosition.setProvider(location.getProvider());
                    int cityId = mapEngine.getCityId(myLocationPosition);
                    CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
                    if (myLocationCityInfo != null && myLocationCityInfo.getId() == cityId) {
                        myLocationCityInfo.setPosition(myLocationPosition);
                        Globals.g_My_Location = location;
                    } else {
                        CityInfo cityInfo = mapEngine.getCityInfo(cityId);
                        cityInfo.setPosition(myLocationPosition);
                        if (cityInfo.isAvailably()) {
                            Globals.g_My_Location = location;
                            Globals.g_My_Location_City_Info = cityInfo;
                        } else {
                            Globals.g_My_Location = null;
                            Globals.g_My_Location_City_Info = null;
                        }
                    }
                }
            } else {
                Globals.g_My_Location = null;
                Globals.g_My_Location_City_Info = null;
            } 
            if (Globals.g_My_Location_State == Globals.LOCATION_STATE_NONE && Globals.g_My_Location_City_Info != null) {
                ActionLog.getInstance(activity).addAction(ActionLog.MapFirstLocation, Globals.g_My_Location_City_Info.getCName());
                final FeedbackUpload feedbackUpload = new FeedbackUpload(activity);
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                feedbackUpload.setup(criteria);
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        feedbackUpload.query();
                    }
                }).start();
                Globals.g_My_Location_State = Globals.LOCATION_STATE_FIRST_SUCCESS;
            }
            if (this.activity != null && this.runnable != null) {
                this.activity.runOnUiThread(runnable);
            }
        }
    }
    
    private boolean mActivityResult = false;

    public static final int ZOOM_LEVEL_DEFAULT = 5; // 200km
    public static final int ZOOM_LEVEL_CITY = 11; // 2km
    public static final int ZOOM_LEVEL_LOCATION = 14; // 200m
    
    protected boolean mSensorOrientation = false;
    private SensorManager mSensorManager=null;
    private float rotateZ = 365;
    private SensorEventListener mSensorListener=new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if(mMapView!=null && Math.abs(rotateZ-event.values[0]) >= 3){
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
    
    private Circle getMyLocationRadiusCircle(){
        Circle circle=(Circle)mMapView.getShapesByName(Shape.MY_LOCATION);
        return circle;
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
//              RotationTilt rt=myLocationPin.getRotationTilt();
//              float rotation=AppUtil.getMovingDirection(myLocationPin.getPosition(), myLocation);
//              rt.setRotation(rotation);
                mMyLocation.setPosition(myLocation);
            }catch(Exception e){
                e.printStackTrace();
            }
            String msg=getString(R.string.my_location);
            String positionName = mMapEngine.getPositionName(myLocation);
            if (positionName != null && positionName.length() > 1) {
                msg += "\n" + positionName.substring(1);
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
        
        Circle myLocationRadiusCircle=getMyLocationRadiusCircle();
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
        return true;
    }
        
    public void requestLocation() {
        CityInfo myLocationCityInfo = Globals.g_My_Location_City_Info;
        if (myLocationCityInfo == null) {
            updateLoactionButtonState(MyLocation.MODE_NONE);
            mActionLog.addAction(ActionLog.MapLocation, "U");
            showTip(R.string.location_waiting, 3000);
            mHandler.removeCallbacks(mLocationResponseRun);
            mHandler.postDelayed(mLocationResponseRun, 20000);
        } else {
            if (mMyLocation.mode == MyLocation.MODE_NONE || mMyLocation.mode == MyLocation.MODE_NORMAL) {
                showTip(getString(R.string.location_success, CommonUtils.formatMeterString((int)myLocationCityInfo.getPosition().getAccuracy())), 3000);
                updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                mActionLog.addAction(ActionLog.MapLocation, "N");
                int zoomLevel = (int)mMapView.getZoomLevel();
                mMapView.zoomTo(zoomLevel < ZOOM_LEVEL_LOCATION ? ZOOM_LEVEL_LOCATION : zoomLevel, myLocationCityInfo.getPosition());
                mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, true);
            } else if (mMyLocation.mode == MyLocation.MODE_NAVIGATION && mSensorOrientation) {
                updateLoactionButtonState(MyLocation.MODE_ROTATION);
                mActionLog.addAction(ActionLog.MapLocation, "R");
            } else if (mMyLocation.mode == MyLocation.MODE_ROTATION) {
                updateLoactionButtonState(MyLocation.MODE_NAVIGATION);
                mMapView.showOverlay(ItemizedOverlay.MY_LOCATION_OVERLAY, true);
                mActionLog.addAction(ActionLog.MapLocation, "N");
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
            Animatable animationDrawable = (Animatable)(mLocationBtn.getDrawable());
            animationDrawable.start();
            mLocationBtn.setBackgroundResource(R.drawable.btn_location_white);
        } else {
            int resid;
            Drawable animationDrawable = mLocationBtn.getDrawable();
            if (animationDrawable != null && animationDrawable instanceof AnimationDrawable) {
                ((AnimationDrawable)animationDrawable).stop();
            }
            mLocationBtn.setImageDrawable(null);
            
            if (mMyLocation.mode == MyLocation.MODE_NORMAL) {
                compass.setVisible(false);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.VISIBLE);
                mMapView.refreshMap();
                resid = R.drawable.btn_location_location;
                rotateZ = 365;
                mMapView.rotateZToDegree(0);
            } else if (mMyLocation.mode == MyLocation.MODE_NAVIGATION) {
                compass.setVisible(false);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.VISIBLE);
                mMapView.refreshMap();
                rotateZ = 365;
                mMapView.rotateZToDegree(0);
                resid = R.drawable.btn_location_navigation;
                showInfoWindow(mMyLocation);
            } else {
            	//只有这种情况下要指南针。
                compass.setVisible(true);
                if (uiStackSize() > 0)
                    mCompassView.setVisibility(View.INVISIBLE);
                mMapView.refreshMap();
                hideInfoWindow(ItemizedOverlay.MY_LOCATION_OVERLAY);
                rotateZ = 365;
                mMapView.refreshMap();
                resid = R.drawable.btn_location_compass;
            }
            mLocationBtn.setBackgroundResource(resid);
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
    	LogWrapper.d(TAG, "centerShowCurrentOverlayFocusedItem");
    	ItemizedOverlay itemizedOverlay = mMapView.getCurrentOverlay();
    	
        if (itemizedOverlay != null) {
            final OverlayItem overlayItem = itemizedOverlay.getItemByFocused();
            if (overlayItem != null) {
//            if (overlayItem.hasPreferZoomLevel() && (overlayItem.getPreferZoomLevel() > mMapView.getZoomLevel())) {
            if (itemizedOverlay.isShowInPreferZoom) {
            	if (overlayItem.hasPreferZoomLevel() && (overlayItem.getPreferZoomLevel() > mMapView.getZoomLevel())) {
            		mMapView.zoomTo(overlayItem.getPreferZoomLevel(), overlayItem.getPosition(), -1, null);
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
					
					LogWrapper.d(TAG, "rect.left: " + rect.left + "rect.right: " + rect.right 
		        			+ "rect.top: " + rect.top + "rect.bottom: " + rect.bottom);
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

}

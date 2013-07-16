package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.tigerknows.android.widget.TKEditText;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.service.MapDownloadService;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;

/**
 * 管理下载城市，主要分为3个界面：
 * 1、提示添加城市界面，当用户没有添加过城市到下载列表且当前所有城市的地图数据大小都小于50%才显示此界面；
 * 2、下载城市列表界面，显示所有下载城市的大小及状态，并可以对其进行操作（停止、下载、更新、删除）；
 *    2.1、更新操作时会先删除地图数据文件再下载
 *    2.2、下载地图时启动@MapDownloadService，在@MapDownloadService中实现地图下载并广播通知当前下载进度情况
 *    2.3、
 * 3、添加城市界面，显示全国所有城市的列表，在输入框中输入文本可以搜索城市；
 * 
 * 每次onResume()都重新统计，一共分为三步：
 * 首先统计已经保存的下载城市列表（此所有下载城市都是标识未统计），完成后再统计未出现在下载列表中但是tigermap/map文件夹下已经存在地图数据文件的城市列表（此所有下载城市都是标识未统计）
 * 然后，在DownloadCityAdapter的getView（）方法中判断此城市是否标识未统计，如果未统计则再统计此下载城市，统计完成后标识为已统计
 * @author pengwenyue
 */
public class MapDownloadActivity extends BaseActivity implements View.OnClickListener {
    
    /*
     * 浏览过的城市Id列表
     */
    public static final String EXTRA_VIEWED_CITY_ID_LIST = "viewed_city_id_list";
    
    /*
     * 地图引擎统计已经下载的地图数据信息有误差，这里约定大于或等于98%为已经下载完成状态
     */
    public static final float PERCENT_COMPLETE = 0.98f;
    
    /*
     * 用户在浏览地图时可能会下载邻近城市的部分地图数据，为了避免在下载列表出现那些不是用户主动添加的城市，
     * 这里约定如果不是用户主动添加的城市，那它的已下载地图数据大于或等于50%才能显示在下载列表中
     */
    public static final float PERCENT_VISIBILITY = 0.5f;
    
    /*
     * 地图更新（下载列表）或当前城市（城市列表）列表项的orderId
     */
    public static final int ORDER_ID_TITLE_ONE = 1000;
    
    /*
     * 正在下载（下载列表）或直辖市（城市列表）列表项的orderId
     */
    public static final int ORDER_ID_TITLE_TWO = 2000;
    
    /*
     * 下载完成（下载列表）或按省份查找（城市列表）列表项的orderId
     */
    public static final int ORDER_ID_TITLE_THREE = 3000;
    
    /*
     * 地图更新列表项的数据
     */
    private static DownloadCity sUpdateTitle = null;
    
    /*
     * 正在下载列表项的数据
     */
    private static DownloadCity sDownloadingTitle = null;
    
    /*
     * 下载完成列表项的数据
     */
    private static DownloadCity sCompleteTitle = null;

    /*
     * 获取地图更新列表项的数据
     */
    public static DownloadCity getUpdateTitle(Context context) {
        if (sUpdateTitle == null) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setId(MapDownloadActivity.ORDER_ID_TITLE_ONE);
            cityInfo.setCName(context.getString(R.string.map_upgrade));
            sUpdateTitle = new DownloadCity(cityInfo);
            sUpdateTitle.cityInfo.order = ORDER_ID_TITLE_ONE;
        }
        return sUpdateTitle;
    }

    /*
     * 获取正在下载列表项的数据
     */
    public static DownloadCity getDownloadingTitle(Context context) {
        if (sDownloadingTitle == null) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setId(MapDownloadActivity.ORDER_ID_TITLE_TWO);
            cityInfo.setCName(context.getString(R.string.file_downloading));
            sDownloadingTitle = new DownloadCity(cityInfo);
            sDownloadingTitle.cityInfo.order = ORDER_ID_TITLE_TWO;
        }
        return sDownloadingTitle;
    }

    /*
     * 获取下载完成列表项的数据
     */
    public static DownloadCity getCompleteTitle(Context context) {
        if (sCompleteTitle == null) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setId(MapDownloadActivity.ORDER_ID_TITLE_THREE);
            cityInfo.setCName(context.getString(R.string.download_complete));
            sCompleteTitle = new DownloadCity(cityInfo);
            sCompleteTitle.cityInfo.order = ORDER_ID_TITLE_THREE;
        }
        return sCompleteTitle;
    }
    
    static final String TAG= "MapDownload";
    
    static final int STATE_EMPTY = 0;
    static final int STATE_DOWNLOAD = 1;
    static final int STATE_ADD = 2;
    
    private int mColorBlackDark;
    
    private int mColorBlackLight;
    
    private LinearLayout mAddBtn;
    
    private View mWifiView;

    private TextView mWifiTxv;
    
    private Button mCloseBtn;
    
    private int mState = 0;
    
    private View mDownloadView;
    
    private View mAddView;
    
    private View mEmptyView;
    
    private ExpandableListView mDownloadCityLsv;
    private DownloadCityAdapter mDownloadAdapter;
    private List<DownloadCity> mDownloadCityList = new ArrayList<DownloadCity>();
    
    private View mInputView;    
    private TKEditText mKeywordEdt;    
    private ExpandableListView mAddCityElv;
    private static List<CityInfo> sAllAddCityInfoList = null;
    private AddCityExpandableListAdapter mAddCityExpandableListAdapter;

    private ListView mSuggestCityLsv;
    private SuggestCityAdapter mSuggestCityAdapter;    
    private List<CityInfo> mSuggestCityItemList = new ArrayList<CityInfo>();
    
    private MapEngine mMapEngine;
    
    private ProgressDialog mTipProgressDialog = null;
    private boolean mLoadData = false;
    private String mNotFindCity;
    
    protected void onMediaChanged() {
        super.onMediaChanged();
        saveDownloadCityList();
        startStats();
    }
    
    private View.OnClickListener  mUpdateBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            mActionLog.addAction(mActionTag +  ActionLog.MapDownloadAllUpdate);
            final List<DownloadCity> list = new ArrayList<DownloadCity>();
            for(int i = 0; i < mDownloadCityList.size(); i++) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_ONE) {
                    continue;
                } else if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_TWO || downloadCity.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                    break;
                } else if (downloadCity.state == DownloadCity.STATE_CAN_BE_UPGRADE) {
                    list.add(downloadCity);
                }
            }
            
            View custom = getLayoutInflater().inflate(R.layout.loading, null);
            TextView loadingTxv = (TextView)custom.findViewById(R.id.loading_txv);
            loadingTxv.setText(R.string.deleteing_map);
            ActionLog.getInstance(mThis).addAction(ActionLog.Dialog, loadingTxv.getText());
            final Dialog tipProgressDialog = Utility.showNormalDialog(mThis, custom);
            tipProgressDialog.setCancelable(true);
            tipProgressDialog.setCanceledOnTouchOutside(false);
            tipProgressDialog.setOnDismissListener(new OnDismissListener() {
                
                @Override
                public void onDismiss(DialogInterface dialog) {
                    ActionLog.getInstance(mThis).addAction(ActionLog.Dialog + ActionLog.Dismiss);
                }
            });
            
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    final List<DownloadCity> addList = new ArrayList<MapDownloadActivity.DownloadCity>();
                    for(int i = 0; i < list.size(); i++) {
                        DownloadCity downloadCity = list.get(i);
                        mMapEngine.removeCityData(downloadCity.cityInfo.getCName());
                        downloadCity.state = DownloadCity.STATE_WAITING;
                        downloadCity.downloadedSize = 0;
                        addList.add(downloadCity);
                        if (tipProgressDialog.isShowing() == false) {
                            break;
                        }
                    }
                    
                    mHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            for(int i = 0; i < addList.size(); i++) {
                                DownloadCity dt = addList.get(i);
                                operateDownloadCity(dt.cityInfo, MapDownloadService.OPERATION_CODE_ADD);
                                addDownloadCity(mThis, mDownloadCityList, dt, true);
                            }
                            notifyDataSetChanged();
                            
                            if (tipProgressDialog.isShowing()) {
                                tipProgressDialog.dismiss();
                            }
                        }
                    });
                }
            }).start();
            
        }
    };
    
    private View.OnClickListener mStartBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            mActionLog.addAction(mActionTag +  ActionLog.MapDownloadAllStart);
            if (mMapEngine.isExternalStorage() == false) {
                Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                return;
            }
            for(int i = 0; i < mDownloadCityList.size(); i++) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_ONE || downloadCity.cityInfo.getId() == ORDER_ID_TITLE_TWO) {
                    continue;
                } else if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                    break;
                } else if (downloadCity.state == DownloadCity.STATE_STOPPED){
                    downloadCity.state = DownloadCity.STATE_WAITING;
                    operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_ADD);
                }
            }
            notifyDataSetChanged();
        }
    };

    private View.OnClickListener mPauseBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            mActionLog.addAction(mActionTag +  ActionLog.MapDownloadAllPause);
            operateDownloadCity(null, MapDownloadService.OPERATION_CODE_CLEAR);
            for(int i = 0; i < mDownloadCityList.size(); i++) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_ONE || downloadCity.cityInfo.getId() == ORDER_ID_TITLE_TWO) {
                    continue;
                } else if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                    break;
                } else if (downloadCity.state == DownloadCity.STATE_WAITING || downloadCity.state == DownloadCity.STATE_DOWNLOADING){
                    downloadCity.state = DownloadCity.STATE_STOPPED;
                    operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_REMOVE);
                }
            }
            notifyDataSetChanged();
        }
    };
    
    /**
     * 通知MapDownloadService添加、删除或清空下载城市
     * @param cityInfo
     * @param operationCode
     */
    void operateDownloadCity(CityInfo cityInfo, String operationCode) {
        Intent service = new Intent(mThis, MapDownloadService.class);
        if (cityInfo != null) {
            service.putExtra(MapDownloadService.EXTRA_CITY_INFO, cityInfo);
        }
        service.putExtra(MapDownloadService.EXTRA_OPERATION_CODE, operationCode);
        startService(service);
    }
    
    private Comparator<CityInfo> mCityComparator = new Comparator<CityInfo>() {

        @Override
        public int compare(CityInfo cityInfo1, CityInfo cityInfo2) {
            return cityInfo1.order - cityInfo2.order;
        };
    };
    
    public static Comparator<DownloadCity> sDownloadCityComparator = new Comparator<DownloadCity>() {

        @Override
        public int compare(DownloadCity downloadCity1, DownloadCity downloadCity2) {
            return downloadCity1.cityInfo.order - downloadCity2.cityInfo.order;
        };
    };
    
    private BroadcastReceiver mStatsDownloadCityListBroadcastReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) { 
            if (intent != null
                    && intent.hasExtra(MapStatsService.EXTRA_DOWNLOAD_CITY_LIST)) {
                ArrayList<DownloadCity> list = intent.getParcelableArrayListExtra(MapStatsService.EXTRA_DOWNLOAD_CITY_LIST);
                loadData(list);
                
                Intent service = new Intent(MapStatsService.ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP);
                service.setClass(mThis, MapStatsService.class);
                service.putParcelableArrayListExtra(MapStatsService.EXTRA_DOWNLOAD_CITY_LIST, getDownloadCityList());
                startService(service);
            }
        }
    };
    
    private BroadcastReceiver mStatsDownloadCityListForExistBroadcastReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) { 
            if (intent != null
                    && intent.hasExtra(MapStatsService.EXTRA_DOWNLOAD_CITY_LIST)) {
                ArrayList<DownloadCity> list = intent.getParcelableArrayListExtra(MapStatsService.EXTRA_DOWNLOAD_CITY_LIST);
                for(int i = list.size()-1; i >= 0; i--) {
                    DownloadCity downloadCity = list.get(i);
                    if (mDownloadCityList.contains(downloadCity) == false) {
                        addDownloadCity(mThis, mDownloadCityList, downloadCity, false);
                    }
                }
                
                if (mState == STATE_EMPTY && mDownloadCityList.isEmpty() == false) {
                    changeState(STATE_DOWNLOAD);
                }
                notifyDataSetChanged();
                
                Intent service = new Intent(MapStatsService.ACTION_STATS_DOWNLOAD_CITY);
                service.setClass(mThis, MapStatsService.class);
                service.putParcelableArrayListExtra(MapStatsService.EXTRA_DOWNLOAD_CITY_LIST, getDownloadCityList());
                startService(service);
            }
        }
    };
    
    private BroadcastReceiver mMapDownloadReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) { 
            if (intent != null
                    && intent.hasExtra(MapDownloadService.EXTRA_CITY_INFO)
                    && intent.hasExtra(MapDownloadService.EXTRA_TOTAL_SIZE)
                    && intent.hasExtra(MapDownloadService.EXTRA_DOWNLOAD_SIZE)) {
                CityInfo cityInfo = intent.getParcelableExtra(MapDownloadService.EXTRA_CITY_INFO);
                int totalSize = intent.getIntExtra(MapDownloadService.EXTRA_TOTAL_SIZE, 0);
                int downloadSize = intent.getIntExtra(MapDownloadService.EXTRA_DOWNLOAD_SIZE, 0);
                for(int i = 0, size = mDownloadCityList.size(); i < size; i++) {
                    DownloadCity downloadCity = mDownloadCityList.get(i);
                    CityInfo cityInfo2 = downloadCity.cityInfo;
                    if (cityInfo.getId() == cityInfo2.getId() && cityInfo.getType() == cityInfo2.getType()) {
                        downloadCity.totalSize = totalSize;
                        downloadCity.downloadedSize = downloadSize;
                        if (downloadCity.downloadedSize/(float)downloadCity.totalSize >= PERCENT_COMPLETE) {
                            downloadCity.state = DownloadCity.STATE_COMPLETED;
                            addDownloadCity(mThis, mDownloadCityList, downloadCity, true);
                        }
                        notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    };
    
    private BroadcastReceiver mStatsDownloadCityReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) { 
            if (intent != null
                    && intent.hasExtra(MapStatsService.EXTRA_DOWNLOAD_CITY)) {
                DownloadCity statsDownloadCity = intent.getParcelableExtra(MapStatsService.EXTRA_DOWNLOAD_CITY);
                CityInfo statsCityInfo = statsDownloadCity.cityInfo;
                for(int i = 0, size = mDownloadCityList.size(); i < size; i++) {
                    DownloadCity downloadCity = mDownloadCityList.get(i);
                    CityInfo cityInfo = downloadCity.cityInfo;
                    if (statsCityInfo.getType() == cityInfo.getType()) {
                        if (statsCityInfo.getId() == cityInfo.getId()) {
                            refreshDownloadCity(statsDownloadCity, downloadCity);
                            return;
                        }
                    } else {
                        // 遍历省份下的城市列表
                        List<DownloadCity> childList = downloadCity.childList;
                        for(int j = 0, childSize = childList.size(); j < childSize; j++) {
                            DownloadCity childDownloadCity = childList.get(j);
                            CityInfo childCityInfo = childDownloadCity.cityInfo;
                            if (statsCityInfo.getId() == childCityInfo.getId()) {
                                refreshDownloadCity(statsDownloadCity, childDownloadCity);
                                return;
                            }
                        }
                    }
                }
            }
        }
    };
    
    /**
     * 统计某个DownloadCity完成后刷新下载列表中对应的DownloadCity
     * @param statsDownloadCity
     * @param refreshDownloadCity
     */
    void refreshDownloadCity(DownloadCity statsDownloadCity, DownloadCity refreshDownloadCity) {
        refreshDownloadCity.isStatsed = true;
        
        refreshDownloadCity.totalSize = statsDownloadCity.totalSize;
        refreshDownloadCity.downloadedSize = statsDownloadCity.downloadedSize;
        
        if (statsDownloadCity.state != refreshDownloadCity.state) {
        	refreshDownloadCity.state = statsDownloadCity.state;
            addDownloadCity(mThis, mDownloadCityList, refreshDownloadCity, true);
        }
        notifyDataSetChanged();
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.MapDownload;
        
        setContentView(R.layout.more_map_download);
        findViews();
        setListener();
        
        Resources resources = mThis.getResources();
        mColorBlackDark = resources.getColor(R.color.black_dark);
        mColorBlackLight = resources.getColor(R.color.black_light);
        
        mTitleBtn.setText(R.string.download_map);
        mRightBtn.setVisibility(View.GONE);
        mLeftBtn.setOnClickListener(this);
        
        mDownloadAdapter = new DownloadCityAdapter();
        mDownloadCityLsv.setAdapter(mDownloadAdapter);
        mDownloadCityLsv.setGroupIndicator(null);

        mNotFindCity = mThis.getString(R.string.not_find_city);
        
        mSuggestCityAdapter = new SuggestCityAdapter(mThis, mSuggestCityItemList);
        mSuggestCityLsv.setAdapter(mSuggestCityAdapter);
        
        mMapEngine = MapEngine.getInstance();
        
        if (sAllAddCityInfoList == null) {
            sAllAddCityInfoList = new ArrayList<CityInfo>();
            List<CityInfo> allCityInfoList = mMapEngine.getAllProvinceCityList(mThis);  
            for(int i = allCityInfoList.size()-1; i >= 0; i--) {
                CityInfo cityInfo1 = allCityInfoList.get(i).clone();
                cityInfo1.order = cityInfo1.getId();
                List<CityInfo> childCityInfoList = cityInfo1.getCityList();
                if (childCityInfoList.size() > 1) {
                    for(int ii = childCityInfoList.size()-1; ii >= 0; ii--) {
                        CityInfo cityInfo2 = childCityInfoList.get(ii);
                        cityInfo2.order = cityInfo2.getId();
                    }
                    CityInfo cityInfo3 = new CityInfo();
                    cityInfo3.setCName(mThis.getString(R.string.all_province));
                    cityInfo3.setType(CityInfo.TYPE_PROVINCE);
                    cityInfo3.getCityList().addAll(childCityInfoList);
                    Collections.sort(childCityInfoList, mCityComparator);
                    childCityInfoList.add(0, cityInfo3);
                }
                Collections.sort(childCityInfoList, mCityComparator);
                sAllAddCityInfoList.add(cityInfo1);
            }
            
    //        CityInfo cityInfo = mMapEngine.getCityInfo(MapEngine.CITY_ID_QUANGUO);
    //        cityInfo.setType(CityInfo.TYPE_PROVINCE);
    //        CityInfo cityInfo1 = mMapEngine.getCityInfo(MapEngine.CITY_ID_QUANGUO);
    //        cityInfo.getCityList().add(cityInfo1);
    //        mAllAddCityInfoList.add(cityInfo);
            
            Collections.sort(sAllAddCityInfoList, mCityComparator);
            
            CityInfo currentCityTitle = new CityInfo();
            currentCityTitle.setCName(mThis.getString(R.string.current_city_text));
            currentCityTitle.setId(ORDER_ID_TITLE_ONE);
            
            CityInfo cityTitle = new CityInfo();
            cityTitle.setCName(mThis.getString(R.string.municipality));
            cityTitle.setId(ORDER_ID_TITLE_TWO);
            
            CityInfo provinceTitle = new CityInfo();
            provinceTitle.setCName(mThis.getString(R.string.search_by_province));
            provinceTitle.setId(ORDER_ID_TITLE_THREE);
            
            sAllAddCityInfoList.add(0, currentCityTitle);
            sAllAddCityInfoList.add(1, cityTitle);
            sAllAddCityInfoList.add(2, cityTitle);
            sAllAddCityInfoList.add(7, provinceTitle);
        }
        mAddCityExpandableListAdapter = new AddCityExpandableListAdapter();
        mAddCityElv.setAdapter(mAddCityExpandableListAdapter);
        mAddCityElv.setGroupIndicator(null);
    }
        
    @Override
    public void onResume() {
        super.onResume();
        WifiManager wifiManager = (WifiManager)mThis.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            mWifiTxv.setText(R.string.wifi_enabled);
        } else {
            mWifiTxv.setText(R.string.wifi_disabled);
        }

        IntentFilter intentFilter= new IntentFilter(MapStatsService.ACTION_STATS_DOWNLOAD_CITY_LIST_COMPLATE);
        registerReceiver(mStatsDownloadCityListBroadcastReceiver, intentFilter);
        intentFilter= new IntentFilter(MapDownloadService.ACTION_MAP_DOWNLOAD_PROGRESS);
        registerReceiver(mMapDownloadReceiver, intentFilter);
        intentFilter= new IntentFilter(MapStatsService.ACTION_STATS_DOWNLOAD_CITY_COMPLATE);
        registerReceiver(mStatsDownloadCityReceiver, intentFilter);
        intentFilter= new IntentFilter(MapStatsService.ACTION_STATS_DOWNLOAD_CITY_LIST_FOR_EXIST_MAP_COMPLATE);
        registerReceiver(mStatsDownloadCityListForExistBroadcastReceiver, intentFilter);
        
        startStats();
    }
    
    /*
     * 显示统计对话框，启动MapStatsService统计下载城市列表
     */
    void startStats() {
        mTipProgressDialog = new ProgressDialog(mThis);
        mTipProgressDialog.setMessage(mThis.getString(R.string.map_static_waiting_tip));
        mTipProgressDialog.setIndeterminate(true);
        mTipProgressDialog.setCancelable(true);
        mTipProgressDialog.setCanceledOnTouchOutside(false);
        mTipProgressDialog.setOnCancelListener(new OnCancelListener() {
            
            @Override
            public void onCancel(DialogInterface arg0) {
                finish();
            }
        });
        mTipProgressDialog.show();
        
        Intent service = new Intent(MapStatsService.ACTION_STATS_DOWNLOAD_CITY_LIST);
        service.setClass(mThis, MapStatsService.class);
        startService(service);
    }
    
    private void loadData(List<DownloadCity> list) {
        if (mTipProgressDialog != null && mTipProgressDialog.isShowing()) {
            mTipProgressDialog.dismiss();
        }
        
        CityInfo cityInfo = Globals.getCurrentCityInfo();
        if (cityInfo != null) {
            if (cityInfo.getId() != sAllAddCityInfoList.get(1).getId()) {
                sAllAddCityInfoList.remove(1);
                cityInfo.getCityList().add(cityInfo.clone());
                sAllAddCityInfoList.add(1, cityInfo);
            }
        } else {
            finish();
        }
        mDownloadCityList.clear();
        mDownloadCityList.addAll(list);
        
        initIntent();
        
        if (mMapEngine.isExternalStorage() == false) {
            for(int i = mDownloadCityList.size()-1; i >= 0; i--) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.state == DownloadCity.STATE_WAITING || downloadCity.state == DownloadCity.STATE_DOWNLOADING) {
                    downloadCity.state =DownloadCity.STATE_STOPPED;
                }
                if (downloadCity.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                    break;
                }
            }
        }
        
        // v4.20 将之前正在下载中的城市加入到下载队列
//        for(int i = 0, size = mDownloadCityList.size(); i < size; i++) {
//            DownloadCity downloadCity = mDownloadCityList.get(i);
//            if (downloadCity.state == DownloadCity.STATE_WAITING || downloadCity.state == DownloadCity.STATE_DOWNLOADING) {
//                operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_ADD);
//            }
//        }
        
        // 检查是否正在MapDownloadService的下载队列里，如果是则将其设为正在下载
        List<CityInfo> downloadingList = new ArrayList<CityInfo>();
        downloadingList.addAll(MapDownloadService.CityInfoList);
        for(int j = downloadingList.size()-1; j >= 0; j--) {
            CityInfo downloading = downloadingList.get(j);
            for(int i = mDownloadCityList.size()-1; i >= 0; i--) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == downloading.getId()) {
                    downloadCity.state =DownloadCity.STATE_DOWNLOADING;
                    break;
                }
            }
        }
        
        if (mDownloadCityList.isEmpty()) {
            Toast.makeText(mThis, R.string.please_add_city, Toast.LENGTH_LONG).show();
            changeState(STATE_EMPTY);
        } else {
            changeState(STATE_DOWNLOAD);
        }
        notifyDataSetChanged();
        
        mLoadData = true;
    }
    
    private void initIntent() {
        if (mIntent != null) {
            ArrayList<Integer> cityIdList = mIntent.getIntegerArrayListExtra(EXTRA_VIEWED_CITY_ID_LIST);
            if (cityIdList != null) {
                for(int j = cityIdList.size()-1; j >= 0; j--) {
                	int cityId = cityIdList.get(j);
                    boolean exist = false;
                    for(int i = 0, size = mDownloadCityList.size(); i < size; i++) {
                        DownloadCity downloadCity = mDownloadCityList.get(i);
                        if (downloadCity.cityInfo.getId() == cityId) {
                            exist = true;
                            break;
                        }
                    }
                    // 如果已经在下载城市列表中，则不用重复添加，免得引起下载城市的状态被改变
                    if (exist == false) {
                        CityInfo cityInfo = mMapEngine.getCityInfo(cityId);
                        addDownloadCityInternal(cityInfo, false, DownloadCity.STATE_STOPPED);
                        notifyDataSetChanged();
                    }
                }
            }
        }
    }
        
    @Override
    public void onPause() {
        super.onPause();
        saveDownloadCityList();
        
        unregisterReceiver(mStatsDownloadCityListBroadcastReceiver);
        unregisterReceiver(mMapDownloadReceiver);
        unregisterReceiver(mStatsDownloadCityReceiver);
        unregisterReceiver(mStatsDownloadCityListForExistBroadcastReceiver);
    }
    
    /*
     * 关闭统计进度对话框，保存下载列表
     */
    void saveDownloadCityList() {
        if (mTipProgressDialog != null && mTipProgressDialog.isShowing()) {
            mTipProgressDialog.dismiss();
        }
        
        if (mLoadData) {
            mLoadData = false;
            StringBuilder s = new StringBuilder();
            List<DownloadCity> list = getDownloadCityList();
            for (int i = 0, size = list.size(); i < size; i++) {
                DownloadCity downloadCity = list.get(i);
                recordDownloadCity(downloadCity, s);
            }
            TKConfig.setPref(mThis, TKConfig.PREFS_MAP_DOWNLOAD_CITYS, s.toString());
        }
    }
    
    /*
     * 记录城市的地图信息及状态
     * 数据结构如下:
     * 城市中文名,地图数据总大小,已经下载数据的大小,状态;城市中文名,地图数据总大小,已经下载数据的大小,状态
     * es:
     * 北京,561200,562400,3;广州,485655,385521,1
     */
    private void recordDownloadCity(DownloadCity downloadCity, StringBuilder s) {
        CityInfo cityInfo = downloadCity.cityInfo;
        int id = cityInfo.getId();
        if (cityInfo.getType() == CityInfo.TYPE_CITY
                && id > 0
                && id != ORDER_ID_TITLE_ONE
                && id != ORDER_ID_TITLE_TWO
                && id != ORDER_ID_TITLE_THREE) {        
            if (s.length() > 0) {
                s.append(";"); 
            }
            s.append(cityInfo.getCName());
            s.append(","); 
            s.append(downloadCity.totalSize); 
            s.append(","); 
            s.append(downloadCity.downloadedSize); 
            s.append(","); 
            s.append(downloadCity.state); 
            DownloadCity current = MoreHomeFragment.CurrentDownloadCity;
            if (current != null) {
                if (current.cityInfo != null && current.cityInfo.getId() == cityInfo.getId()) {
                    if (downloadCity.state != DownloadCity.STATE_CAN_BE_UPGRADE) {
                        MoreHomeFragment.CurrentDownloadCity = null;
                    }
                }
            }
        }
    }
    
    protected void findViews() {
        super.findViews();
        mEmptyView = findViewById(R.id.empty_view);
        mAddBtn = (LinearLayout) findViewById(R.id.add_btn);
        mDownloadView = findViewById(R.id.download_view);
        mAddView = findViewById(R.id.add_view);
        mDownloadCityLsv = (ExpandableListView) findViewById(R.id.download_city_lsv);
        mInputView = (RelativeLayout) findViewById(R.id.input_view);
        mKeywordEdt = (TKEditText) findViewById(R.id.input_edt);
        mKeywordEdt.mActionTag = mActionTag;
        mAddCityElv = (ExpandableListView) findViewById(R.id.add_city_elv);
        mSuggestCityLsv = (ListView) findViewById(R.id.suggest_city_lsv);
        mWifiView = findViewById(R.id.wifi_view);
        mWifiTxv = (TextView) findViewById(R.id.wifi_txv);
        mCloseBtn = (Button) findViewById(R.id.close_btn);
    }

    protected void setListener() {
        super.setListener();
        mAddBtn.setOnClickListener(this);
        mCloseBtn.setOnClickListener(this);

        mDownloadCityLsv.setOnGroupCollapseListener(new OnGroupCollapseListener() {
            
            @Override
            public void onGroupCollapse(int groupPosition) {
                DownloadCity downloadCity = mDownloadCityList.get(groupPosition);
                if (downloadCity.childList.size() == 0) {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListCity, groupPosition, 0, mDownloadCityList.get(groupPosition).cityInfo.getCName());
                    clickDownloadCity(downloadCity);
                } else {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadDownloadListProvince, groupPosition, mDownloadCityList.get(groupPosition).cityInfo.getCName(), "0");
                }
            }
        });
        
        mDownloadCityLsv.setOnGroupExpandListener(new OnGroupExpandListener() {
            
            @Override
            public void onGroupExpand(int groupPosition) {
                DownloadCity downloadCity = mDownloadCityList.get(groupPosition);
                if (downloadCity.childList.size() == 0) {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadDownloadListCity, groupPosition, 0, downloadCity.cityInfo.getCName());
                    clickDownloadCity(downloadCity);
                } else {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadDownloadListProvince, groupPosition, downloadCity.cityInfo.getCName(), "1");
                }
            }
        });

        mDownloadCityLsv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
                DownloadCity downloadCity = mDownloadCityList.get(groupPosition).childList.get(childPosition);
                mActionLog.addAction(mActionTag + ActionLog.MapDownloadDownloadListCity, groupPosition, childPosition, downloadCity.cityInfo.getCName());
                clickDownloadCity(downloadCity);
                return false;
            }
        });

        mAddCityElv.setOnGroupCollapseListener(new OnGroupCollapseListener() {
            
            @Override
            public void onGroupCollapse(int groupPosition) {
                CityInfo cityInfo = sAllAddCityInfoList.get(groupPosition);

                List<CityInfo> cityInfoList = cityInfo.getCityList();
                if (cityInfoList.size() == 1) {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListCity, groupPosition, 0, cityInfo.getCName());
                } else {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListProvince, groupPosition, cityInfo.getCName(), "0");
                }
            }
        });
        
        mAddCityElv.setOnGroupExpandListener(new OnGroupExpandListener() {
            
            @Override
            public void onGroupExpand(int groupPosition) {
                CityInfo cityInfo = sAllAddCityInfoList.get(groupPosition);

                List<CityInfo> cityInfoList = cityInfo.getCityList();
                if (cityInfoList.size() == 1) {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListCity, groupPosition, 0, cityInfo.getCName());
                    addDownloadCity(cityInfoList.get(0), DownloadCity.STATE_WAITING);
                } else {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListProvince, groupPosition, cityInfo.getCName(), "1");
                }
            }
        });
        
        mAddCityElv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childrenPosition, long arg4) {
                CityInfo cityInfo = sAllAddCityInfoList.get(groupPosition).getCityList().get(childrenPosition);
                if (cityInfo.getType() == CityInfo.TYPE_CITY) {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListCity, groupPosition, childrenPosition, cityInfo.getCName());
                    addDownloadCity(cityInfo, DownloadCity.STATE_WAITING);
                } else if (cityInfo.getType() == CityInfo.TYPE_PROVINCE) {
                    mActionLog.addAction(mActionTag + ActionLog.MapDownloadAddListCity, groupPosition, childrenPosition, sAllAddCityInfoList.get(groupPosition).getCName());
                    List<CityInfo> cityInfoList = cityInfo.getCityList();
                    addDownloadCityList(cityInfoList, DownloadCity.STATE_WAITING);
                }
                return false;
            }
        });
        
        mAddCityElv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideSoftInput();
                }
                return false;
            }
        });
        
        mSuggestCityLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                CityInfo cityInfo = mSuggestCityItemList.get(position);
                mActionLog.addAction(mActionTag + ActionLog.MapDownloadSuggest, position, cityInfo.getCName());
                DownloadCity downloadCity = getDownloadCity(mDownloadCityList, cityInfo);
                if (downloadCity == null && !mNotFindCity.equals(cityInfo.getCName())) {
                    addDownloadCity(cityInfo, DownloadCity.STATE_WAITING);
                }
            }
        });
        
        mKeywordEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String userInput = s.toString().trim();
                showAddCityOrSuggestCity(userInput);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        mKeywordEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    hideSoftInput();
                    return true;
                }
                return false;
            }
        });
        
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mActionLog.addAction(mActionTag +  ActionLog.MapDownloadInput);
                    showAddCityOrSuggestCity(mKeywordEdt.getText().toString().trim());
                }
                return false;
            }
        });
        
        mSuggestCityLsv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideSoftInput();
                }
                return false;
            }
        });
    }
    
    private void clickDownloadCity(final DownloadCity downloadCity) {
        final CityInfo cityInfo = downloadCity.cityInfo;
        if (cityInfo.getType() == CityInfo.TYPE_PROVINCE || 
                cityInfo.getId() == ORDER_ID_TITLE_ONE || 
                cityInfo.getId() == ORDER_ID_TITLE_TWO || 
                cityInfo.getId() == ORDER_ID_TITLE_THREE) {
            return;
        }
        final String cityName = cityInfo.getCName();
        final List<String> list = new ArrayList<String>();
        int state = downloadCity.state;
        if (state == DownloadCity.STATE_DOWNLOADING ||
                state == DownloadCity.STATE_WAITING) {
            list.add(mThis.getString(R.string.pause_download));
        } else if (state == DownloadCity.STATE_STOPPED) {
            list.add(mThis.getString(R.string.download_map));
        } else if (state == DownloadCity.STATE_CAN_BE_UPGRADE) {
            list.add(mThis.getString(R.string.upgrade_map));
        }
        list.add(mThis.getString(R.string.delete_map));
        
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mThis, list);
        ListView listView = Utility.makeListView(mThis);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.showNormalDialog(this,
                getString(R.string.select_action),
                listView);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
                String str = list.get(which);
                if (str.equals(mThis.getString(R.string.download_map))) {
                    mActionLog.addAction(mActionTag +  ActionLog.MapDownloadOpertorDownload);
                    if (mMapEngine.isExternalStorage()) {
                        downloadCity.state = DownloadCity.STATE_WAITING;
                        notifyDataSetChanged();
                        operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_ADD);
                    } else {
                        Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                    }
                } else if (str.equals(mThis.getString(R.string.upgrade_map))) {
                    mActionLog.addAction(mActionTag +  ActionLog.MapDownloadOpertorUpgrade);
                    mMapEngine.removeCityData(cityName);
                    downloadCity.downloadedSize = 0;
                    downloadCity.state = DownloadCity.STATE_WAITING;
                    addDownloadCity(mThis, mDownloadCityList, downloadCity, true);
                    notifyDataSetChanged();
                    operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_ADD);
                } else if (str.equals(mThis.getString(R.string.pause_download))) {
                    mActionLog.addAction(mActionTag +  ActionLog.MapDownloadOpertorPause);
                    downloadCity.state = DownloadCity.STATE_STOPPED;
                    notifyDataSetChanged();      
                    operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_REMOVE);
                } else if (str.equals(mThis.getString(R.string.delete_map))) {
                    mActionLog.addAction(mActionTag +  ActionLog.MapDownloadOpertorDelete);
                    Utility.showNormalDialog(mThis,
                            mThis.getString(R.string.prompt),
                            mThis.getString(R.string.delete_city_map_tip),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        operateDownloadCity(downloadCity.cityInfo, MapDownloadService.OPERATION_CODE_REMOVE);
                                        downloadCity.state = DownloadCity.STATE_STOPPED;
                                        
                                        mMapEngine.removeCityData(cityName);
                                        List <CityInfo> cityInfoList = cityInfo.getCityList();
                                        for(int i = 0, size = cityInfoList.size(); i < size; i++) {
                                            mMapEngine.removeLastRegionId(cityInfoList.get(i).getId());
                                        }

                                        deleteDownloadCity(mThis, mDownloadCityList,downloadCity);
                                        notifyDataSetChanged();
                                        
                                        if (mDownloadCityList.size() < 1) {
                                            Toast.makeText(mThis, R.string.please_add_city, Toast.LENGTH_LONG).show();
                                            changeState(STATE_ADD);
                                        }
                                    }
                                }
                            });
                }
                dialog.dismiss();
            }
            
        });
    }
    
    private void showAddCityOrSuggestCity(String userInput) {
        if (mInputView.getVisibility() != View.VISIBLE) {
            return;
        }
        ChangeCityActivity.makeSuggestCityList(sAllAddCityInfoList, mSuggestCityItemList, userInput, mNotFindCity, mAddCityElv, mSuggestCityLsv);
        mSuggestCityAdapter.notifyDataSetChanged();
        mSuggestCityLsv.setSelectionFromTop(0, 0);
    }
    
    private void addDownloadCity(CityInfo addCityInfo, int state) {
        DownloadCity addDownloadCity = getDownloadCity(mDownloadCityList, addCityInfo);
        if (addDownloadCity == null) {
            if (mMapEngine.isExternalStorage() == false && state == DownloadCity.STATE_WAITING) {
                Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                return;
            }
            
            addDownloadCityInternal(addCityInfo, true, state);
        }
        notifyDataSetChanged();
        Toast.makeText(mThis, R.string.exist_download_list, Toast.LENGTH_LONG).show();
    }
    
    private void addDownloadCityList(List<CityInfo> list, int state) {
        
        for(int i = list.size()-1; i >= 0; i--) {
            CityInfo cityInfo = list.get(i);
            DownloadCity addDownloadCity = getDownloadCity(mDownloadCityList, cityInfo);
            if (addDownloadCity != null && addDownloadCity.state == state) {
                MapStatsService.statsDownloadCity(addDownloadCity, MapStatsService.getServerRegionDataInfoMap());
                continue;
            } else {
                if (mMapEngine.isExternalStorage() == false && state == DownloadCity.STATE_WAITING) {
                    Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                    return;
                }
                addDownloadCityInternal(cityInfo, true, DownloadCity.STATE_WAITING);
            }
        }
        
        notifyDataSetChanged();
        Toast.makeText(mThis, R.string.exist_download_list, Toast.LENGTH_LONG).show();
    }
    
    private void notifyDataSetChanged() {
        mDownloadAdapter.notifyDataSetChanged();
        mAddCityExpandableListAdapter.notifyDataSetChanged();
        mSuggestCityAdapter.notifyDataSetChanged();
    }
    
    private void addDownloadCityInternal(CityInfo cityInfo, boolean isManual, int state) {
        DownloadCity downloadCity = getDownloadCity(mDownloadCityList, cityInfo);
        if (downloadCity != null && downloadCity.state == state) {
            return;
        }
        downloadCity = new DownloadCity(cityInfo);
        downloadCity.state = state;
        MapStatsService.statsDownloadCity(downloadCity, MapStatsService.getServerRegionDataInfoMap());
        addDownloadCity(mThis, mDownloadCityList, downloadCity, isManual);
        if (state == DownloadCity.STATE_WAITING
                || state == DownloadCity.STATE_DOWNLOADING) {
            operateDownloadCity(cityInfo, MapDownloadService.OPERATION_CODE_ADD);
        }
    }
    
    private class DownloadCityAdapter extends BaseExpandableListAdapter {

        public DownloadCityAdapter() {
            super();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return ((DownloadCity) getGroup(groupPosition)).childList.get(childPosition);
        }

        @Override
        public long getChildId(int arg0, int arg1) {
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.more_map_download_city_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            DownloadCity downloadCity = (DownloadCity) getChild(groupPosition, childPosition);

            setDownloadCityView(downloadCity, view);
            
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            DownloadCity downloadCity = (DownloadCity)getGroup(groupPosition);
            CityInfo cityInfo = downloadCity.cityInfo;
            int count = mDownloadCityList.get(groupPosition).childList.size();
            return (MapEngine.hasMunicipality(cityInfo.getId()) == false) ? count : 0;
        }

        @Override
        public Object getGroup(int position) {
            return mDownloadCityList.get(position);
        }

        @Override
        public int getGroupCount() {
            return mDownloadCityList.size();
        }

        @Override
        public long getGroupId(int arg0) {
            return 0;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.more_map_download_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            DownloadCity downloadCity = (DownloadCity) getGroup(groupPosition);
            CityInfo cityInfo = downloadCity.cityInfo;
            
            View provinceView = view.findViewById(R.id.province_view);
            View cityView = view.findViewById(R.id.city_view);
            TextView titleTxv = (TextView) provinceView.findViewById(R.id.title_txv);
            ImageView iconImv = (ImageView) provinceView.findViewById(R.id.icon_imv);
            Button startBtn = (Button)provinceView.findViewById(R.id.start_btn);
            Button pauseBtn = (Button)provinceView.findViewById(R.id.pause_btn);
            if (cityInfo.getId() == ORDER_ID_TITLE_ONE) {
                provinceView.setBackgroundResource(R.drawable.bg_expandablelistview_group);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                TextView textTxv = (TextView) provinceView.findViewById(R.id.text_txv);
                titleTxv.setText(cityInfo.getCName());
                textTxv.setVisibility(View.GONE);
                titleTxv.setVisibility(View.VISIBLE);
                iconImv.setVisibility(View.GONE);
                startBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.VISIBLE);
                pauseBtn.setBackgroundResource(R.drawable.ic_map_upgrade);
                pauseBtn.setOnClickListener(mUpdateBtnOnClickListener);
            } else if (cityInfo.getId() == ORDER_ID_TITLE_TWO) {
                provinceView.setBackgroundResource(R.drawable.bg_expandablelistview_group);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                TextView textTxv = (TextView) provinceView.findViewById(R.id.text_txv);
                titleTxv.setText(cityInfo.getCName());
                textTxv.setVisibility(View.GONE);
                titleTxv.setVisibility(View.VISIBLE);
                iconImv.setVisibility(View.GONE);
                startBtn.setVisibility(View.VISIBLE);
                pauseBtn.setVisibility(View.VISIBLE);
                
                boolean start = false;
                boolean pasue = false;
                for(int i = 0; i < mDownloadCityList.size(); i++) {
                    DownloadCity temp = mDownloadCityList.get(i);
                    if (temp.cityInfo.getId() == ORDER_ID_TITLE_ONE || temp.cityInfo.getId() == ORDER_ID_TITLE_TWO) {
                        continue;
                    } else if (temp.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                        break;
                    } else {
                        if (temp.state == DownloadCity.STATE_STOPPED){
                            start = true;
                        } else if (temp.state == DownloadCity.STATE_WAITING || temp.state == DownloadCity.STATE_DOWNLOADING){
                            pasue = true;
                        }
                    }
                }
                
                if (start) {
                    startBtn.setBackgroundResource(R.drawable.btn_play_normal);
                    startBtn.setOnClickListener(mStartBtnOnClickListener);
                } else {
                    startBtn.setBackgroundResource(R.drawable.btn_play_disabled);
                    startBtn.setOnClickListener(null);
                }
                
                if (pasue) {
                    pauseBtn.setBackgroundResource(R.drawable.btn_pause_normal);
                    pauseBtn.setOnClickListener(mPauseBtnOnClickListener);
                } else {
                    pauseBtn.setBackgroundResource(R.drawable.btn_pause_disabled);
                    pauseBtn.setOnClickListener(null);
                }
            } else if (cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                provinceView.setBackgroundResource(R.drawable.bg_expandablelistview_group);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                TextView textTxv = (TextView) provinceView.findViewById(R.id.text_txv);
                titleTxv.setText(cityInfo.getCName());
                textTxv.setVisibility(View.GONE);
                titleTxv.setVisibility(View.VISIBLE);
                iconImv.setVisibility(View.GONE);
                startBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.GONE);
            } else if (cityInfo.getType() == CityInfo.TYPE_PROVINCE) {
                provinceView.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                TextView textTxv = (TextView) provinceView.findViewById(R.id.text_txv);
                textTxv.setText(cityInfo.getCName());
                textTxv.setVisibility(View.VISIBLE);
                titleTxv.setVisibility(View.GONE);
                iconImv.setVisibility(View.VISIBLE);
                if (isExpanded) {            
                    iconImv.setBackgroundResource(R.drawable.icon_arrow_up);
                } else {
                    iconImv.setBackgroundResource(R.drawable.icon_arrow_down);
                }
                startBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.GONE);
            } else {
                provinceView.setVisibility(View.GONE);
                cityView.setVisibility(View.VISIBLE);
                setDownloadCityView(downloadCity, cityView);
            }
            return view;
        }
        
        private void setDownloadCityView(DownloadCity downloadCity, View cityView) {
            if (downloadCity.isStatsed == false) {
                Intent intent = new Intent(MapStatsService.ACTION_STATS_DOWNLOAD_CITY);
                intent.setClass(mThis, MapStatsService.class);
                intent.putExtra(MapStatsService.EXTRA_DOWNLOAD_CITY, downloadCity);
                startService(intent);
            }
            CityInfo cityInfo = downloadCity.cityInfo;
            TextView nameTxv = (TextView) cityView.findViewById(R.id.text_txv);
            TextView percentTxv = (TextView) cityView.findViewById(R.id.percent_txv);
            ProgressBar progressBar = (ProgressBar) cityView.findViewById(R.id.progress_prb);
            String size = (downloadCity.totalSize > 0 ? mThis.getString(R.string.brackets, mThis.getString(R.string._m, downloadCity.getTotalSizeTip())) : "");
            nameTxv.setText(cityInfo.getCName()+size);
            
            if (downloadCity.isStatsed == false) {
                percentTxv.setText(mThis.getString(R.string.counting_tip));
                progressBar.setProgress(0);
            } else {
                if (downloadCity.state == DownloadCity.STATE_CAN_BE_UPGRADE) {
                    percentTxv.setText(mThis.getString(R.string.may_upgrade));
                } else if (downloadCity.state == DownloadCity.STATE_DOWNLOADING || downloadCity.state == DownloadCity.STATE_WAITING) {
                    percentTxv.setText(mThis.getString(R.string.downloading_, String.valueOf(downloadCity.getDownloadPercent())));
                } else if (downloadCity.state == DownloadCity.STATE_COMPLETED) {
                    percentTxv.setText(mThis.getString(R.string.completed));
                } else {
                	percentTxv.setText(mThis.getString(R.string.downloaded_, String.valueOf(downloadCity.getDownloadPercent())));
                }
                progressBar.setProgress((int)Float.parseFloat(downloadCity.getDownloadPercent()));
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int arg0, int arg1) {
            return true;
        }
    }
    
    public static class DownloadCity implements Parcelable {
        /**
         * 省份下的城市列表
         */
    	public ArrayList<DownloadCity> childList = new ArrayList<DownloadCity>();
    	
    	/**
    	 * 下载城市信息
    	 */
        public CityInfo cityInfo;
        
        /**
         * 当前状态
         */
        public int state = STATE_STOPPED;
        
        /**
         * 总大小
         */
        public int totalSize = 0;
        
        /**
         * 已下载大小
         */
        public int downloadedSize = 0;
        
        /**
         * 是否被统计过
         */
        public boolean isStatsed = false;
        
        public static final int STATE_WAITING = 0; // 等待下载
        public static final int STATE_DOWNLOADING = 1; // 下载中
        public static final int STATE_STOPPED = 2; // 暂停
        public static final int STATE_COMPLETED = 3; // 完成 
        public static final int STATE_CAN_BE_UPGRADE = 4; // 可更新
        
        public DownloadCity(CityInfo cityInfo) {
            this(cityInfo, STATE_STOPPED);
        }

        public DownloadCity(CityInfo cityInfo, int state) {
            this.cityInfo = cityInfo;
            this.state = state;
        }
        
        /**
         * 以文本形式描述已下载大小所占的百分比
         * @return
         */
        public String getDownloadPercent() {
            if (downloadedSize < 1 || totalSize < 1) {
                return "0";
            }
            float downloadPercent = (downloadedSize*100f)/totalSize;
            if (downloadedSize >= totalSize || downloadPercent > 100.0) {
                downloadPercent = 100;
                return "100";
            }
            if (downloadPercent < 0.0) {
                downloadPercent = 0;
                return "0";
            }
            if (!(downloadPercent > 0.0 && downloadPercent < 100.1)) {
                downloadPercent = 0;
                return "0";
            }
            String str = Float.toString(downloadPercent);
            return str.substring(0, str.indexOf(".")+2);
        }
        
        /**
         * 以文本形式描述总大小
         * @return
         */
        public String getTotalSizeTip() {
            float size = totalSize*1f/(1024*1024);
            if (size < 0.1f) {
                size = 0.1f;
            }
            String str = Float.toString(size);
            return str.substring(0, str.indexOf(".")+2);
        }
        
        /**
         * 已下载大小所占比率
         * @return
         */
        public float getPercent() {
            float percent = 0;
            if (downloadedSize > 0 && totalSize > 0) {
                percent = (float)downloadedSize/totalSize;
            }
            return percent;
        }
        
        public int hashCode() {
            return cityInfo.hashCode();
        }
        
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof DownloadCity) {
                DownloadCity other = (DownloadCity) object;
                CityInfo cityInfo = other.cityInfo;
                if ((this.cityInfo != null && this.cityInfo.equals(cityInfo)) || (this.cityInfo == null && cityInfo == null)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeParcelable(cityInfo, flags);
            parcel.writeInt(totalSize);
            parcel.writeInt(downloadedSize);
            parcel.writeInt(state);     
            DownloadCity[] list = CREATOR.newArray(childList.size());
            childList.toArray(list);
            parcel.writeParcelableArray(list, flags);
        }

        public static final Parcelable.Creator<DownloadCity> CREATOR
                = new Parcelable.Creator<DownloadCity>() {
            public DownloadCity createFromParcel(Parcel in) {
                return new DownloadCity(in);
            }

            public DownloadCity[] newArray(int size) {
                return new DownloadCity[size];
            }
        };
        
        private DownloadCity(Parcel in) {
            cityInfo = in.readParcelable(CityInfo.class.getClassLoader());
            totalSize = in.readInt();
            downloadedSize = in.readInt();
            state = in.readInt();
            Parcelable[] parcelables = in.readParcelableArray(DownloadCity.class.getClassLoader());
            for(int i = 0, length = parcelables.length; i < length; i++) {
                childList.add((DownloadCity)parcelables[i]);
            }
        }
        
        public DownloadCity clone() {
        	DownloadCity downloadCity = new DownloadCity(cityInfo.clone());
        	downloadCity.totalSize = totalSize;
        	downloadCity.downloadedSize = downloadedSize;
        	downloadCity.state = state;
        	if (this.childList != null) {
        		ArrayList<DownloadCity> childList = new ArrayList<DownloadCity>();
        		for(int i = 0, size = this.childList.size(); i < size; i++) {
        			childList.add(this.childList.get(i).clone());
        		}
        		downloadCity.childList = childList;
        	}
        	return downloadCity;
        }
    }
    
    private class SuggestCityAdapter extends ArrayAdapter<CityInfo> {
        
        public SuggestCityAdapter(Context context, List<CityInfo> cityList) {
            super(context, R.layout.more_map_download_suggest_list_item, cityList);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.more_map_download_suggest_list_item, parent, false);
            } else {
                view = convertView;
            }

            CityInfo cityInfo = getItem(position);
            
            setAddCityView(view, cityInfo, true);
            
            return view;
        }
    }
    
    public class AddCityExpandableListAdapter extends BaseExpandableListAdapter {

        public Object getChild(int groupPosition, int childPosition) {
            return sAllAddCityInfoList.get(groupPosition).getCityList().get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public int getChildrenCount(int groupPosition) {
            int count = sAllAddCityInfoList.get(groupPosition).getCityList().size();
            return count > 1 ? count : 0;
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.more_map_download_suggest_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            CityInfo cityInfo = (CityInfo)getChild(groupPosition, childPosition);
            setAddCityView(view, cityInfo, true);
            
            return view;
        }

        public Object getGroup(int groupPosition) {
            return sAllAddCityInfoList.get(groupPosition);
        }

        public int getGroupCount() {
            return sAllAddCityInfoList.size();
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            
            CityInfo cityInfo = (CityInfo) getGroup(groupPosition);
            List<CityInfo> cityInfoList = cityInfo.getCityList();
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.more_map_download_add_list_item, parent, false);
            } else {
                view = convertView;
            }
            View provinceView = view.findViewById(R.id.province_view);
            View cityView = view.findViewById(R.id.city_view);
            if (cityInfoList.size() == 0) {
                String cname = cityInfo.getCName();
                int cityId = cityInfo.getId();
                if (cityId == ORDER_ID_TITLE_ONE || cityId == ORDER_ID_TITLE_TWO || cityId == ORDER_ID_TITLE_THREE) {
                    provinceView.setBackgroundResource(R.drawable.bg_expandablelistview_group);
                    provinceView.setVisibility(View.VISIBLE);
                    cityView.setVisibility(View.GONE);
                    TextView textTxv = (TextView)provinceView.findViewById(R.id.text_txv);
                    TextView titleTxv = (TextView)provinceView.findViewById(R.id.title_txv);
                    ImageView iconImv = (ImageView)provinceView.findViewById(R.id.icon_imv);
                    titleTxv.setText(cname);
                    textTxv.setVisibility(View.GONE);
                    titleTxv.setVisibility(View.VISIBLE);
                    iconImv.setVisibility(View.GONE);
                } else {
                    provinceView.setVisibility(View.GONE);
                    cityView.setVisibility(View.VISIBLE);
                    setAddCityView(cityView, cityInfo, false);
                }
            } else if (cityInfoList.size() != 1) {
                provinceView.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                
                String cname = cityInfo.getCName();
                TextView textTxv = (TextView)provinceView.findViewById(R.id.text_txv);
                TextView titleTxv = (TextView)provinceView.findViewById(R.id.title_txv);
                ImageView iconImv = (ImageView)provinceView.findViewById(R.id.icon_imv);
                textTxv.setText(cname);
                textTxv.setVisibility(View.VISIBLE);
                titleTxv.setVisibility(View.GONE);
                iconImv.setVisibility(View.VISIBLE);
                
                if (isExpanded) {            
                    iconImv.setBackgroundResource(R.drawable.icon_arrow_up);
                } else {
                    iconImv.setBackgroundResource(R.drawable.icon_arrow_down);
                }
            } else {
                provinceView.setVisibility(View.GONE);
                cityView.setVisibility(View.VISIBLE);
                cityInfo = cityInfoList.get(0);
                setAddCityView(cityView, cityInfo, false);
            }
            
            return view;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }
    }
    
    private void setAddCityView(View view, CityInfo cityInfo, boolean appendSpace) {
        String cname = cityInfo.getCName();
        TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
        TextView statusTxv = (TextView)view.findViewById(R.id.status_txv);
        
        if (cityInfo.getType() == CityInfo.TYPE_CITY) {
            textTxv.setText((appendSpace ? "    " : "")+cname);
            DownloadCity downloadCity = getDownloadCity(mDownloadCityList, cityInfo);
            if (downloadCity != null) {
                textTxv.setTextColor(mColorBlackLight);
                view.setBackgroundResource(R.color.gray_light);
                statusTxv.setText(R.string.added);
                statusTxv.setVisibility(View.VISIBLE);
            } else {
                if (mNotFindCity.equals(cityInfo.getCName())) {
                    textTxv.setTextColor(mColorBlackLight);
                    view.setBackgroundResource(R.color.gray_light);
                } else {
                    textTxv.setTextColor(mColorBlackDark);
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                }
                statusTxv.setVisibility(View.GONE);
            }
        } else {
            textTxv.setText((appendSpace ? "    " : "" )+cname);
            List<CityInfo> cityInfoList = cityInfo.getCityList();
            boolean isAllExist = true;
            for(CityInfo cityInfo1 : cityInfoList) {
                DownloadCity downloadCity = getDownloadCity(mDownloadCityList, cityInfo1);
                if (downloadCity == null) {
                    isAllExist = false;
                    break;
                }
            }
            
            if (isAllExist) {
                textTxv.setTextColor(mColorBlackLight);
                view.setBackgroundResource(R.color.gray_light);
                statusTxv.setVisibility(View.VISIBLE);
                statusTxv.setText(R.string.added);
            } else {
                textTxv.setTextColor(mColorBlackDark);
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                statusTxv.setVisibility(View.GONE);
            }
        }            
    }
    
    private void onBack() {
        hideSoftInput();
        if (STATE_ADD == mState) {
            if (mDownloadCityList.isEmpty()) {
                changeState(STATE_EMPTY);
            } else {
                changeState(STATE_DOWNLOAD);
            }
        } else {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {                
            case KeyEvent.KEYCODE_BACK:
                mActionLog.addAction(ActionLog.KeyCodeBack);
                onBack();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleLeftButton);
                onBack();
                break;
                
            case R.id.add_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MapDownloadAddCityBtn);
                changeState(STATE_ADD);
                break;

            case R.id.close_btn:
                mActionLog.addAction(mActionTag +  ActionLog.MapDownloadCloseWifi);
                mWifiView.setVisibility(View.GONE);
                break;
                
            default:
                break;
        }
    }
    
    private void changeState(int state) {
        mState = state;
        if (mState == STATE_EMPTY) {
            mTitleBtn.setText(R.string.download_map);
            mAddBtn.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.VISIBLE);
            mDownloadView.setVisibility(View.GONE);
            mAddView.setVisibility(View.GONE);
            mKeywordEdt.setText("");
            hideSoftInput();
            mCloseBtn.requestFocus();
        } else if (mState == STATE_DOWNLOAD) {
            mTitleBtn.setText(R.string.download_map);
            mAddBtn.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            mDownloadView.setVisibility(View.VISIBLE);
            mAddView.setVisibility(View.GONE);
            mKeywordEdt.setText("");
            hideSoftInput();
            mDownloadCityLsv.requestFocus();
        } else {
            mTitleBtn.setText(R.string.add_city);
            mAddBtn.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.GONE);
            mDownloadView.setVisibility(View.GONE);
            mAddView.setVisibility(View.VISIBLE);
            mKeywordEdt.setText("");
            showAddCityOrSuggestCity("");
            hideSoftInput();
            mAddCityElv.requestFocus();
        }
    }
    
    ArrayList<DownloadCity> getDownloadCityList() {
        ArrayList<DownloadCity> list = new ArrayList<DownloadCity>();
        for(int i = 0, size = mDownloadCityList.size(); i < size; i++) {
            DownloadCity downloadCity = mDownloadCityList.get(i);
            CityInfo cityInfo = downloadCity.cityInfo;
            int id  = cityInfo.getId();
            if (cityInfo.getType() == CityInfo.TYPE_CITY
                    && id > 0
                    && id != ORDER_ID_TITLE_ONE
                    && id != ORDER_ID_TITLE_TWO
                    && id != ORDER_ID_TITLE_THREE) {
                list.add(downloadCity);
            } else {
                // 遍历省份下的城市列表
                List<DownloadCity> childList = downloadCity.childList;
                list.addAll(childList);
            }
        }
        return list;
    }
    
    /**
     * 生成用于DownloadCityAdpater的GroupItem（省份）
     * @param pName
     * @param id
     * @return
     */
    public static DownloadCity makeProvinceDownloadCity(String pName, int id) {
        CityInfo provinceCityInfo = new CityInfo();
        provinceCityInfo.setCName(pName);
        provinceCityInfo.setId(id);
        provinceCityInfo.setType(CityInfo.TYPE_PROVINCE);
        DownloadCity provinceDownloadCity = new DownloadCity(provinceCityInfo);
        return provinceDownloadCity;
    }
    
    /**
     * 从DownloadCity列表中根据CityInfo查找出相应的DownloadCity
     * @param list
     * @param cityInfo
     * @return
     */
    public static DownloadCity getDownloadCity(List<DownloadCity> list, CityInfo cityInfo) {
        DownloadCity downloadCity = null;
        for(int i = list.size() - 1; i >= 0; i--) {
            DownloadCity downloadCity1 = list.get(i);
            if (cityInfo.equals(downloadCity1.cityInfo)) {
                downloadCity = downloadCity1;
                break;
            } else {
                List<DownloadCity> childList = downloadCity1.childList;
                for(int j = childList.size() - 1; j >= 0; j--) {
                    DownloadCity downloadCity2 = childList.get(j);
                    if (cityInfo.equals(downloadCity2.cityInfo)) {
                        downloadCity = downloadCity2;
                        break;
                    }
                }
                if (downloadCity != null) {
                    break;
                }
            }
        }
        
        return downloadCity;
    }
    
    /**
     * 添加DownloadCity到列表，添加之前先将其删除，确保列表中不能出现重复项
     * @param context
     * @param list
     * @param downloadCity
     * @param isManualAdd
     */
    public static void addDownloadCity(Context context, List<DownloadCity> list, DownloadCity downloadCity, boolean isManualAdd) {
        deleteDownloadCity(context, list, downloadCity);
        
        CityInfo cityInfo = downloadCity.cityInfo;
        int cityId = cityInfo.getId();
        if (cityId < 0) {
            cityId = 1;
        }

        String hongkong = context.getString(R.string.hongkong);
        String macao = context.getString(R.string.macao);
        String pName = cityInfo.getCProvinceName();
        if (macao.equals(pName) || hongkong.equals(pName)) {
            return;
        }
        boolean exist = false;
        float percent = downloadCity.getPercent();
        if ((isManualAdd || percent > PERCENT_VISIBILITY) && downloadCity.state == DownloadCity.STATE_CAN_BE_UPGRADE) {
            downloadCity.cityInfo.order = ORDER_ID_TITLE_ONE+cityId;
            if (list.size() > 0) {
                DownloadCity downloadCity1 = list.get(0);
                if (downloadCity1.cityInfo.getId() == ORDER_ID_TITLE_ONE) {
                    exist = true;
                }
            }
            if (exist == false) {
                DownloadCity downloadCityUpdate = getUpdateTitle(context);
                list.add(0, downloadCityUpdate);
            }
            list.add(1, downloadCity);
        } else if ((isManualAdd || percent > PERCENT_VISIBILITY) && percent < PERCENT_COMPLETE) {
            downloadCity.cityInfo.order = ORDER_ID_TITLE_TWO+cityId;
            int positionComplete = list.size();
            int positionDownload = 0;
            for(int i = 0, size = list.size(); i < size; i++) {
                DownloadCity downloadCity1 = list.get(i);
                if (downloadCity1.cityInfo.getId() == ORDER_ID_TITLE_TWO) {
                    exist = true;
                    positionDownload = i;
                } else if (downloadCity1.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                    positionComplete = i;
                    break;
                }
            }
            if (exist == false) {
                DownloadCity downloadCityUpdate = getDownloadingTitle(context);
                list.add(positionComplete, downloadCityUpdate);
                list.add(positionComplete+1, downloadCity);
            } else {
                list.add(positionDownload+1, downloadCity);
            }
        } else if (percent >= PERCENT_COMPLETE) {
            downloadCity.cityInfo.order = ORDER_ID_TITLE_THREE+cityId;
            DownloadCity province = null;
            for(int i = 0, size = list.size(); i < size; i++) {
                DownloadCity downloadCity1 = list.get(i);
                if (downloadCity1.cityInfo.getId() == ORDER_ID_TITLE_THREE) {
                    exist = true;
                } else if (exist) {
                    if (downloadCity1.cityInfo.getType() == CityInfo.TYPE_PROVINCE && pName.equals(downloadCity1.cityInfo.getCName())) {
                        province = downloadCity1;
                        break;
                    }
                }
            }
            if (exist == false) {
                DownloadCity downloadCityUpdate = getCompleteTitle(context);
                list.add(list.size(), downloadCityUpdate);
            }
            if (MapEngine.hasMunicipality(cityId)) {
                list.add(list.size(), downloadCity);
            } else if (province == null) {
                MapEngine mapEngine = MapEngine.getInstance();
                List<String> cityNameList = mapEngine.getCitylist(pName);
                if (cityNameList.size() > 0) {
                    CityInfo cityInfo2 = mapEngine.getCityInfo(mapEngine.getCityid(cityNameList.get(cityNameList.size()-1)));
                    if (cityInfo2.isAvailably()) {
                        cityId = cityInfo2.getId();
                    }
                }
                province = makeProvinceDownloadCity(pName, cityId);
                province.cityInfo.order = ORDER_ID_TITLE_THREE+cityId+100; // +100是因为广州作为广东的省会，其id小于天津和重庆
                province.childList.add(downloadCity);
                list.add(list.size(), province);
            } else {
                province.childList.add(downloadCity);
                Collections.sort(province.childList, sDownloadCityComparator);
            }
        }
        Collections.sort(list, sDownloadCityComparator);
    }
    
    /**
     * 从列表中删除指定的DownloadCity，如果删除之后其所属类别下无子项，则删除其所属类别
     * @param context
     * @param list
     * @param downloadCity
     */
    public static void deleteDownloadCity(Context context, List<DownloadCity> list, DownloadCity downloadCity) {
        boolean delete = false;
        
        // 检测是否为直辖市，是则删除
        if (list.contains(downloadCity)) {
            list.remove(downloadCity);
            delete = true;
        } else {
            // 遍历所有省份下的所有城市，找到匹配则删除
            for(int i = list.size()-1; i >= 0; i--) {
                DownloadCity downloadCity1 = list.get(i);
                List<DownloadCity> childList = downloadCity1.childList; 
                if (childList.contains(downloadCity)) {
                    childList.remove(downloadCity);
                    
                    // 如果当前省份下没有任何下载城市，则删除省份
                    if (childList.isEmpty()) {
                        list.remove(i);
                    }
                    delete = true;
                    break;
                }
            }
        }
        if (delete == false) {
            return;
        }
        int sizeUpdate = -1;
        int sizeDownload = -1;
        int sizeComplete = -1;
        int state = -1;
        for(int i = 0, size = list.size(); i < size; i++) {
            DownloadCity downloadCity1 = list.get(i);
            CityInfo cityInfo1 = downloadCity1.cityInfo;
            if (cityInfo1.getId() == ORDER_ID_TITLE_ONE) {
                state = ORDER_ID_TITLE_ONE;
                sizeUpdate = 0;
            } else if (cityInfo1.getId() == ORDER_ID_TITLE_TWO) {
                state = ORDER_ID_TITLE_TWO;
                sizeDownload = 0;
            } else if (cityInfo1.getId() == ORDER_ID_TITLE_THREE) {
                state = ORDER_ID_TITLE_THREE;
                sizeComplete = 0;
            } else {
                if (state == ORDER_ID_TITLE_ONE && sizeUpdate == 0) {
                    sizeUpdate = 1;
                }
                if (state == ORDER_ID_TITLE_TWO && sizeDownload == 0) {
                    sizeDownload = 1;
                }
                if (state == ORDER_ID_TITLE_THREE && sizeComplete == 0) {
                    sizeComplete = 1;
                }
            }
        }
        
        // 如果升级地图类别下无子项，则删除升级地图类别
        if (sizeUpdate == 0) {
            list.remove(getUpdateTitle(context));
        }

        // 如果正在下载类别下无子项，则删除正在下载类别
        if (sizeDownload == 0) {
            list.remove(getDownloadingTitle(context));
        }
        
        // 如果下载完成类别下无子项，则删除下载完成类别
        if (sizeComplete == 0) {
            list.remove(getCompleteTitle(context));
        }
    }
}

package com.tigerknows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.maps.MapEngine.RegionMetaVersion;
import com.tigerknows.maps.RegionMapInfo;
import com.tigerknows.maps.TileDownload;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.MapMetaFileDownload;
import com.tigerknows.model.MapTileDataDownload;
import com.tigerknows.model.MapVersionQuery;
import com.tigerknows.model.MapVersionQuery.RegionDataInfo;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.view.StringArrayAdapter;
import com.tigerknows.view.TKEditText;

/**
 * 分为两个界面：
 * 下载界面mDownloadListView：显示手机上的地图信息，百分比，下载、停止、删除、更新等操作；
 * 搜索添加界面mAddListView：给出推荐的省市添加，搜索添加；
 * @author zhouwentao
 *
 */
public class MapDownload extends BaseActivity implements View.OnClickListener {
    
    public static final float PERCENT_COMPLETE = 0.98f;
    public static final float PERCENT_VISIBILITY = 0.5f;
    
    public static final int CITY_ID_TITLE_ONE = 1000;
    public static final int CITY_ID_TITLE_TWO = 2000;
    public static final int CITY_ID_TITLE_THREE = 3000;
    
    private static DownloadCity sUpdateTitle = null;
    private static DownloadCity sDownloadingTitle = null;
    private static DownloadCity sCompleteTitle = null;

    public static DownloadCity getUpdateTitle(Context context) {
        if (sUpdateTitle == null) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setId(MapDownload.CITY_ID_TITLE_ONE);
            cityInfo.setCName(context.getString(R.string.map_upgrade));
            sUpdateTitle = new DownloadCity(cityInfo);
            sUpdateTitle.cityInfo.order = CITY_ID_TITLE_ONE;
        }
        return sUpdateTitle;
    }

    public static DownloadCity getDownloadingTitle(Context context) {
        if (sDownloadingTitle == null) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setId(MapDownload.CITY_ID_TITLE_TWO);
            cityInfo.setCName(context.getString(R.string.file_downloading));
            sDownloadingTitle = new DownloadCity(cityInfo);
            sDownloadingTitle.cityInfo.order = CITY_ID_TITLE_TWO;
        }
        return sDownloadingTitle;
    }

    public static DownloadCity getCompleteTitle(Context context) {
        if (sCompleteTitle == null) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setId(MapDownload.CITY_ID_TITLE_THREE);
            cityInfo.setCName(context.getString(R.string.download_complete));
            sCompleteTitle = new DownloadCity(cityInfo);
            sCompleteTitle.cityInfo.order = CITY_ID_TITLE_THREE;
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
    
    private HashMap<String, DownloadAsyncTask> mDownloadCityTaskMap = new HashMap<String, DownloadAsyncTask>();
    
    private Timer mTimer;
    private ProgressDialog mTipProgressDialog = null;
    private boolean mLoadData = false;
    private String mNotFindCity;
    
    private View.OnClickListener  mUpdateBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            List<DownloadCity> list = new ArrayList<DownloadCity>();
            for(int i = 0; i < mDownloadCityList.size(); i++) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_ONE) {
                    continue;
                } else if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_TWO || downloadCity.cityInfo.getId() == CITY_ID_TITLE_THREE) {
                    break;
                } else if (downloadCity.state == DownloadCity.MAYUPDATE) {
                    list.add(downloadCity);
                }
            }
            
            for(int i = 0; i < list.size(); i++) {
                DownloadCity downloadCity = list.get(i);
                mMapEngine.removeCityData(downloadCity.cityInfo.getCName());
                deleteDownloadCity(mThis, mDownloadCityList, downloadCity);
                downloadCity.setState(DownloadCity.WAITING);
                addDownloadCity(mThis, mDownloadCityList, downloadCity, true);
            }
            notifyDataSetChanged();
            download();
        }
    };
    
    private View.OnClickListener mStartBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            if (mMapEngine.isExternalStorage() == false) {
                Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                return;
            }
            for(int i = 0; i < mDownloadCityList.size(); i++) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_ONE || downloadCity.cityInfo.getId() == CITY_ID_TITLE_TWO) {
                    continue;
                } else if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_THREE) {
                    break;
                } else if (downloadCity.state == DownloadCity.STOPPED){
                    downloadCity.state = DownloadCity.WAITING;
                }
            }
            notifyDataSetChanged();
            download();
        }
    };

    private View.OnClickListener mPauseBtnOnClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            stopAllDownload(DownloadCity.STOPPED);
            for(int i = 0; i < mDownloadCityList.size(); i++) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_ONE || downloadCity.cityInfo.getId() == CITY_ID_TITLE_TWO) {
                    continue;
                } else if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_THREE) {
                    break;
                } else if (downloadCity.state == DownloadCity.WAITING || downloadCity.state == DownloadCity.DOWNLOADING){
                    downloadCity.state = DownloadCity.STOPPED;
                }
            }
            notifyDataSetChanged();
        }
    };
    
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
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.DownloadMap;
        
        setContentView(R.layout.map_download);
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
            currentCityTitle.setId(CITY_ID_TITLE_ONE);
            
            CityInfo cityTitle = new CityInfo();
            cityTitle.setCName(mThis.getString(R.string.municipality));
            cityTitle.setId(CITY_ID_TITLE_TWO);
            
            CityInfo provinceTitle = new CityInfo();
            provinceTitle.setCName(mThis.getString(R.string.search_by_province));
            provinceTitle.setId(CITY_ID_TITLE_THREE);
            
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
        if (mTipProgressDialog == null) {
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
            
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    while (mMapEngine.isStatsFinish() == false) {
                    }

                    List<DownloadCity> downloadCityList = mMapEngine.getDownloadCityList();
                    List<Integer> list = new ArrayList<Integer>();
                    for(DownloadCity downloadCity : downloadCityList) {
                        list.addAll(downloadCity.getRegionIdList());
                    }
                    MapVersionQuery mapVersionQuery = new MapVersionQuery(getBaseContext());
                    mapVersionQuery.setup(list);
                    mapVersionQuery.query();
                    HashMap<Integer, RegionDataInfo> regionVersionMap = mapVersionQuery.getRegionVersion();
                    synchronized (mMapEngine) {
                        for(DownloadCity downloadCity : downloadCityList) {
                            MapStatsService.countDownloadCity(downloadCity, regionVersionMap);
                        }
                    }
                    
                    mHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            loadData();
                            if (mTipProgressDialog != null && mTipProgressDialog.isShowing()) {
                                mTipProgressDialog.dismiss();
                                mTipProgressDialog = null;
                            }
                        }
                    });
                }
            }).start();
        }
    }
    
    private void loadData() {
        CityInfo cityInfo = Globals.g_Current_City_Info;
        if (cityInfo != null) {
            if (cityInfo.getId() != sAllAddCityInfoList.get(1).getId()) {
                sAllAddCityInfoList.remove(1);
                cityInfo.getCityList().add(cityInfo.clone());
                sAllAddCityInfoList.add(1, cityInfo);
            }
        } else {
            finish();
        }
        List<DownloadCity> list = mMapEngine.getDownloadCityList();
        mDownloadCityList.clear();
        mDownloadCityList.addAll(list);
        
        initIntent();
        if (mMapEngine.isExternalStorage() == false) {
            for(int i = mDownloadCityList.size()-1; i >= 0; i--) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.getState() == DownloadCity.WAITING || downloadCity.getState() == DownloadCity.DOWNLOADING) {
                    downloadCity.setState(DownloadCity.STOPPED);
                }
                if (downloadCity.cityInfo.getId() == CITY_ID_TITLE_THREE) {
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
        
        mTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                updateDownloadCityElv();
            }
        };
        mTimer.schedule(timerTask, 0, 3000);
        
        download();
        mLoadData = true;
    }
    
    private void initIntent() {
        if (mIntent != null) {
            ArrayList<Integer> cityIdList = mIntent.getIntegerArrayListExtra("cityIdList");
            if (cityIdList != null) {
                for(int cityId: cityIdList) {
                    CityInfo cityInfo = mMapEngine.getCityInfo(cityId);
                    addDownloadCityInternal(cityInfo, false, DownloadCity.STOPPED);
                    notifyDataSetChanged();
                }
            }
            boolean upgradeAll = mIntent.getBooleanExtra("upgradeAll", false);
            if (upgradeAll) {
                for (DownloadCity downloadCity : mDownloadCityList) {
                    if (downloadCity.getState() == DownloadCity.MAYUPDATE) {
                        mMapEngine.removeCityData(downloadCity.getCityInfo().getCName());
                        downloadCity.setDownloadedSize(0);
                        downloadCity.setState(DownloadCity.WAITING);
                    }
                }
                upgradeAll = false;
            }
        }
    }
    
    private void stopAllDownload(int state) {
        Iterator<DownloadAsyncTask> iterator = mDownloadCityTaskMap.values().iterator();
        while (iterator.hasNext()) {
            DownloadAsyncTask tkAsyncTask = iterator.next();
            if (tkAsyncTask != null && !tkAsyncTask.mIsStop) {
                tkAsyncTask.stop();
                tkAsyncTask.mDownloadCity.setState(state);
            }
        }
        mDownloadCityTaskMap.clear();
    }
        
    @Override
    public void onPause() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
        
        stopAllDownload(DownloadCity.WAITING);
        
        if (mLoadData) {
            StringBuilder s = new StringBuilder();
            for (int i = mDownloadCityList.size()-1; i >= 0; i--) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                recordDownloadCity(downloadCity, s);
                List <DownloadCity> list = downloadCity.childList;
                for(int j = list.size()-1; j >= 0; j--) {
                    recordDownloadCity(list.get(j), s);
                }
            }
            TKConfig.setPref(mThis, TKConfig.PREFS_MAP_DOWNLOAD_CITYS, s.toString());
            mMapEngine.statsMapEnd(mDownloadCityList, true);
        }
        super.onPause();
    }
    
    private void recordDownloadCity(DownloadCity downloadCity, StringBuilder s) {
        CityInfo cityInfo = downloadCity.cityInfo;
        int id = cityInfo.getId();
        if (cityInfo.getType() == CityInfo.TYPE_CITY
                && id > 0
                && id != CITY_ID_TITLE_ONE
                && id != CITY_ID_TITLE_TWO
                && id != CITY_ID_TITLE_THREE) {        
            if (s.length() > 0) {
                s.append(";"); 
            }
            s.append(cityInfo.getCName());
            s.append(","); 
            s.append(downloadCity.getTotalSize()); 
            s.append(","); 
            s.append(downloadCity.getDownloadedSize()); 
            s.append(","); 
            s.append(downloadCity.getState()); 
        }
    }
    
    private Runnable updateDownloadCityElv = new Runnable() {
        @Override
        public void run() {
            mDownloadAdapter.notifyDataSetChanged();
        }
    };
    
    private void updateDownloadCityElv() {
        if (mDownloadCityLsv.getVisibility() == View.VISIBLE) {
            runOnUiThread(updateDownloadCityElv);
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
        mDownloadCityLsv.setOnGroupClickListener(new OnGroupClickListener() {
            
            @Override
            public boolean onGroupClick(ExpandableListView arg0, View arg1, int position, long arg3) {
                DownloadCity downloadCity = mDownloadCityList.get(position);
                CityInfo cityInfo = downloadCity.getCityInfo();
                if (cityInfo.getType() == CityInfo.TYPE_PROVINCE || 
                        cityInfo.getId() == CITY_ID_TITLE_ONE || 
                        cityInfo.getId() == CITY_ID_TITLE_TWO || 
                        cityInfo.getId() == CITY_ID_TITLE_THREE) {
                    return false;
                }
                onClickDownloadCity(downloadCity);
                return false;
            }
        });
        mDownloadCityLsv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
                onClickDownloadCity(mDownloadCityList.get(groupPosition).childList.get(childPosition));
                return false;
            }
        });
        
        mAddCityElv.setOnGroupClickListener(new OnGroupClickListener() {
            
            @Override
            public boolean onGroupClick(ExpandableListView arg0, View arg1, int groupPosition, long arg3) {
                CityInfo cityInfo = sAllAddCityInfoList.get(groupPosition);

                List<CityInfo> cityInfoList = cityInfo.getCityList();
                if (cityInfoList.size() == 1) {
                    addDownloadCity(cityInfoList.get(0), DownloadCity.WAITING);
                }
                return false;
            }
        });
        
        mAddCityElv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childrenPosition, long arg4) {
                CityInfo cityInfo = sAllAddCityInfoList.get(groupPosition).getCityList().get(childrenPosition);
                if (cityInfo.getType() == CityInfo.TYPE_CITY) {
                    addDownloadCity(cityInfo, DownloadCity.WAITING);
                } else if (cityInfo.getType() == CityInfo.TYPE_PROVINCE) {
                    List<CityInfo> cityInfoList = cityInfo.getCityList();
                    addDownloadCityList(cityInfoList, DownloadCity.WAITING);
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
                DownloadCity downloadCity = getDownloadCity(mDownloadCityList, cityInfo);
                if (downloadCity == null && !mNotFindCity.equals(cityInfo.getCName())) {
                    addDownloadCity(cityInfo, DownloadCity.WAITING);
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
                    mActionLog.addAction(ActionLog.DownloadMapInputBox);
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
    
    private void onClickDownloadCity(final DownloadCity downloadCity) {
        final CityInfo cityInfo = downloadCity.cityInfo;
        final String cityName = cityInfo.getCName();
        mActionLog.addAction(ActionLog.DownloadMapSelect, cityName);
        final List<String> list = new ArrayList<String>();
        int state = downloadCity.getState();
        if (state == DownloadCity.DOWNLOADING ||
                state == DownloadCity.WAITING) {
            list.add(mThis.getString(R.string.pause_download));
        } else if (state == DownloadCity.STOPPED) {
            list.add(mThis.getString(R.string.download_map));
        } else if (state == DownloadCity.MAYUPDATE) {
            list.add(mThis.getString(R.string.upgrade_map));
        }
        list.add(mThis.getString(R.string.delete_map));
        
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mThis, list);
        ListView listView = CommonUtils.makeListView(mThis);
        listView.setAdapter(adapter);
        
        final Dialog dialog = CommonUtils.showNormalDialog(this,
                getString(R.string.select_action),
                null,
                listView,
                null,
                null,
                null);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
                String str = list.get(which);
                if (str.equals(mThis.getString(R.string.download_map))) {
                    if (mMapEngine.isExternalStorage()) {
                        mActionLog.addAction(ActionLog.DownloadMapDownload, cityName);
                        downloadCity.setState(DownloadCity.WAITING);
                        notifyDataSetChanged();
                        download();
                    } else {
                        Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                    }
                } else if (str.equals(mThis.getString(R.string.upgrade_map))) {
                    mActionLog.addAction(ActionLog.DownloadMapUpgrade, cityName);
                    mMapEngine.removeCityData(cityName);
                    downloadCity.setDownloadedSize(0);
                    downloadCity.setState(DownloadCity.WAITING);
                    deleteDownloadCity(mThis, mDownloadCityList, downloadCity);
                    addDownloadCity(mThis, mDownloadCityList, downloadCity, true);
                    notifyDataSetChanged();
                    download();
                } else if (str.equals(mThis.getString(R.string.pause_download))) {
                    mActionLog.addAction(ActionLog.DownloadMapPause, cityName);
                    DownloadAsyncTask task = mDownloadCityTaskMap.remove(cityInfo.getCName());
                    if (task != null && !task.mIsStop)
                        task.stop();
                    downloadCity.setState(DownloadCity.STOPPED);
                    
                    notifyDataSetChanged();      
                    download();                      
                } else if (str.equals(mThis.getString(R.string.delete_map))) {
                    CommonUtils.showNormalDialog(mThis,
                            mThis.getString(R.string.prompt),
                            mThis.getString(R.string.delete_city_map_tip),
                            new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface arg0, int id) {
                                    if (id == DialogInterface.BUTTON_POSITIVE) {
                                        String cityName = cityInfo.getCName();
                                        mActionLog.addAction(ActionLog.DownloadMapDelete, cityName);
                                        DownloadAsyncTask tkAsyncTask = mDownloadCityTaskMap.remove(cityInfo.getCName());
                                        if (tkAsyncTask != null && !tkAsyncTask.mIsStop) {
                                            tkAsyncTask.stop();
                                        }
                                        downloadCity.setState(DownloadCity.STOPPED);
                                        
                                        mMapEngine.removeCityData(cityName);
                                        List <CityInfo> cityInfoList = cityInfo.getCityList();
                                        for(int i = 0, size = cityInfoList.size(); i < size; i++) {
                                            mMapEngine.removeLastRegionId(cityInfoList.get(i).getId());
                                        }

                                        deleteDownloadCity(mThis, mDownloadCityList,downloadCity);
                                        notifyDataSetChanged();
                                        download();
                                        
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
        ChangeCity.makeSuggestCityList(sAllAddCityInfoList, mSuggestCityItemList, userInput, mNotFindCity, mAddCityElv, mSuggestCityLsv);
        mSuggestCityAdapter.notifyDataSetChanged();
        mSuggestCityLsv.setSelectionFromTop(0, 0);
    }
    
    private void addDownloadCity(CityInfo addCityInfo, int state) {
        DownloadCity addDownloadCity = getDownloadCity(mDownloadCityList, addCityInfo);
        if (addDownloadCity == null) {
            if (mMapEngine.isExternalStorage() == false && state == DownloadCity.WAITING) {
                Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                return;
            }
            
            addDownloadCityInternal(addCityInfo, true, state);
        }
        notifyDataSetChanged();
        Toast.makeText(mThis, R.string.exist_download_list, Toast.LENGTH_LONG).show();
        download();
    }
    
    private void addDownloadCityList(List<CityInfo> list, int state) {
        
        for(int i = 0, size = list.size(); i < size; i++) {
            CityInfo cityInfo = list.get(i);
            DownloadCity addDownloadCity = getDownloadCity(mDownloadCityList, cityInfo);
            if (addDownloadCity != null) {
                MapStatsService.countDownloadCity(addDownloadCity, null);
                continue;
            } else {
                if (mMapEngine.isExternalStorage() == false && state == DownloadCity.WAITING) {
                    Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                    return;
                }
                addDownloadCityInternal(cityInfo, true, DownloadCity.WAITING);
            }
        }
        
        notifyDataSetChanged();
        Toast.makeText(mThis, R.string.exist_download_list, Toast.LENGTH_LONG).show();
        download();
    }
    
    private void notifyDataSetChanged() {
        mDownloadAdapter.notifyDataSetChanged();
        mAddCityExpandableListAdapter.notifyDataSetChanged();
        mSuggestCityAdapter.notifyDataSetChanged();
    }
    private void addDownloadCityInternal(CityInfo cityInfo, boolean isManual, int state) {
        if (getDownloadCity(mDownloadCityList, cityInfo) != null) {
            return;
        }
        DownloadCity downloadCity = new DownloadCity(cityInfo);
        downloadCity.setState(state);
        MapStatsService.countDownloadCity(downloadCity, null);
        addDownloadCity(mThis, mDownloadCityList, downloadCity, isManual);
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
                view = mLayoutInflater.inflate(R.layout.download_city_list_item, parent, false);
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
            return (MapEngine.hasCity(cityInfo.getId()) == false) ? count : 0;
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
                view = mLayoutInflater.inflate(R.layout.map_download_list_item, parent, false);
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
            if (cityInfo.getId() == CITY_ID_TITLE_ONE) {
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
            } else if (cityInfo.getId() == CITY_ID_TITLE_TWO) {
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
                    if (temp.cityInfo.getId() == CITY_ID_TITLE_ONE || temp.cityInfo.getId() == CITY_ID_TITLE_TWO) {
                        continue;
                    } else if (temp.cityInfo.getId() == CITY_ID_TITLE_THREE) {
                        break;
                    } else if (temp.state == DownloadCity.STOPPED){
                        start = true;
                    } else if (temp.state == DownloadCity.WAITING || temp.state == DownloadCity.DOWNLOADING){
                        pasue = true;
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
            } else if (cityInfo.getId() == CITY_ID_TITLE_THREE) {
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
            CityInfo cityInfo = downloadCity.cityInfo;
            TextView nameTxv = (TextView) cityView.findViewById(R.id.text_txv);
            TextView percentTxv = (TextView) cityView.findViewById(R.id.percent_txv);
            ProgressBar progressBar = (ProgressBar) cityView.findViewById(R.id.progress_prb);
            String size = (downloadCity.getTotalSize() > 0 ? mThis.getString(R.string.brackets, mThis.getString(R.string._m, downloadCity.getTotalSizeTip())) : "");
            nameTxv.setText(cityInfo.getCName()+size);
            
            if (downloadCity.getState() == DownloadCity.MAYUPDATE) {
                percentTxv.setText(mThis.getString(R.string.may_upgrade));
            } else {
                percentTxv.setCompoundDrawables(null, null, null, null);
                percentTxv.setText((downloadCity.state == DownloadCity.DOWNLOADING || downloadCity.state == DownloadCity.WAITING) ? mThis.getString(R.string.downloading_, String.valueOf(downloadCity.getDownloadPercent())) : 
                    mThis.getString(R.string.downloaded_, String.valueOf(downloadCity.getDownloadPercent())));
                
                if (downloadCity.getPercent() >= PERCENT_COMPLETE) {
                    downloadCity.setState(DownloadCity.COMPLETED);
                    percentTxv.setText(mThis.getString(R.string.completed));
                }
            }
            progressBar.setProgress((int)Float.parseFloat(downloadCity.getDownloadPercent()));
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
    
    public static class DownloadCity {
        private CityInfo cityInfo;
        public List<DownloadCity> childList = new ArrayList<DownloadCity>();
        private List<Integer> mRegionIdList = null;
        
        private int state = STOPPED;
        private int mTotalSize = 0;
        private int mDownloadedSize = 0;
        
        public int isManual = 0;
        
        public static final int WAITING = 0; //在等待下载队列中
        public static final int DOWNLOADING = 1; //downloading
        public static final int STOPPED = 2; //paused, stopped 0% start, need to download metefile
        public static final int COMPLETED = 3; //100% 
        public static final int MAYUPDATE = 4; //can be updated
        
        public DownloadCity(CityInfo cityInfo) {
            this(cityInfo, STOPPED);
        }

        public DownloadCity(CityInfo cityInfo, int state) {
            this.cityInfo = cityInfo;
            this.state = state;
        }
        
        public CityInfo getCityInfo() {
            return cityInfo;
        }
        
        public List<Integer> getRegionIdList() {
            if (mRegionIdList == null) {
                mRegionIdList = MapEngine.getInstance().getRegionIdListByCityName(cityInfo.getCName());
            }
            return mRegionIdList;
        }
        
        public String getDownloadPercent() {
            if (mDownloadedSize < 1 || mTotalSize < 1) {
                return "0";
            }
            float downloadPercent = (mDownloadedSize*100f)/mTotalSize;
            if (mDownloadedSize >= mTotalSize || downloadPercent > 100.0) {
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
        
        public String getTotalSizeTip() {
            float size = mTotalSize*1f/(1024*1024);
            if (size < 0.1f) {
                size = 0.1f;
            }
            String str = Float.toString(size);
            return str.substring(0, str.indexOf(".")+2);
        }
        
        public int getState() {
            return state;
        }
        
        public void setState(int state) {
            if (state == WAITING || state == DOWNLOADING || state == COMPLETED || state == STOPPED || state == MAYUPDATE) {
                this.state = state;
            }
        }
        
        public float getPercent() {
            float percent = 0;
            if (mDownloadedSize > 0 && mTotalSize > 0) {
                percent = (float)mDownloadedSize/mTotalSize;
            }
            return percent;
        }
        
        public int getDownloadedSize() {
            return mDownloadedSize;
        }
        
        public void setDownloadedSize(final int downloadedSize) {
            this.mDownloadedSize = downloadedSize;
        }
        
        public void addDownloadedSize(final int downloadedSize) {
            this.mDownloadedSize += downloadedSize;
        }

        public int getTotalSize() {
            return mTotalSize;
        }
        
        public void setTotalSize(final int totalSize) {
            this.mTotalSize = totalSize;
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
    }

    private class DownloadAsyncTask extends AsyncTask<Void, Integer, Void> implements
            HttpUtils.TKHttpClient.ProgressUpdate, MapTileDataDownload.ITileDownload {

        private DownloadCity mDownloadCity;
        private MapMetaFileDownload mMapMetaFileDownload;
        private MapTileDataDownload mMapTileDataDownload;
        boolean mIsStop = false;
        private int statusCode = BaseQuery.STATUS_CODE_NETWORK_OK;
        private Runnable toast = new Runnable() {
            
            @Override
            public void run() {
                Toast.makeText(mThis, R.string.network_failed, Toast.LENGTH_LONG).show();
            }
        };
        
        public synchronized void stop() {
            mMapTileDataDownload.stop();
            mMapMetaFileDownload.stop();
            cancel(true);
            mIsStop = true;
        }

        public DownloadAsyncTask(DownloadCity downloadCity) {
            mDownloadCity = downloadCity;
            mMapMetaFileDownload = new MapMetaFileDownload(mThis, mMapEngine);
            mMapTileDataDownload = new MapTileDataDownload(mThis, mMapEngine);
            
            mMapTileDataDownload.setProgressUpdate(DownloadAsyncTask.this);
            mMapTileDataDownload.setFillMapTile(DownloadAsyncTask.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            int totalSize = 0;
            int downloadedSize = 0;
            List<Integer> regionIdList = mDownloadCity.getRegionIdList();
            //检查更新
            while (mDownloadCity.state != DownloadCity.COMPLETED && mIsStop == false) {
                statusCode = BaseQuery.STATUS_CODE_NETWORK_OK;
                MapVersionQuery mapVersionQuery = new MapVersionQuery(mThis);
                mapVersionQuery.setup(regionIdList);
                mapVersionQuery.query();
                if (mapVersionQuery.isStop() == false) {
                    statusCode = mapVersionQuery.getStatusCode();
                }
                HashMap<Integer, RegionDataInfo> regionVersionMap = mapVersionQuery.getRegionVersion();
                if (regionVersionMap != null) {
                    for (int regionId : regionIdList) {
                        RegionMapInfo regionMapInfo = mMapEngine.getRegionStat(regionId);
                        if (null != regionMapInfo && regionVersionMap.containsKey(regionId)) {
                            String curVersion = "";
                            RegionMetaVersion version = mMapEngine.getRegionVersion(regionId);
                            if (null != version) {
                                curVersion = version.toString();
                                String serverVersion = regionVersionMap.get(regionId).getRegionVersion();
                                List<TileDownload> lostDatas = regionMapInfo.getLostDatas();
                                //下载或更新meta数据
                                if (mIsStop) {
                                    return null;
                                } else if ((lostDatas != null && lostDatas.size() > 0 && lostDatas.get(0).getOffset() == 0) ||
                                        (serverVersion != null && !serverVersion.equalsIgnoreCase(curVersion))) {
                                    mMapMetaFileDownload.setup(regionId);
                                    mMapMetaFileDownload.query();
                                    if (mMapMetaFileDownload.isStop() == false) {
                                        statusCode = mMapMetaFileDownload.getStatusCode();
                                    }
                                }
                                //计算下载百分比
                                regionMapInfo = mMapEngine.getRegionStat(regionId);
                                if (null != regionMapInfo) {
                                    totalSize += regionMapInfo.getTotalSize();
                                    downloadedSize += regionMapInfo.getDownloadedSize();
                                }
                            }
                        }
                    }
                    mDownloadCity.setTotalSize(totalSize);
                    mDownloadCity.setDownloadedSize(downloadedSize);
    
                    //下载tile数据
                    for (int regionId : regionIdList) {
                        RegionMapInfo regionMapInfo = mMapEngine.getRegionStat(regionId);
                        while (regionMapInfo != null && regionMapInfo.getLostDataNum() > 0 && !mIsStop) {
                            List<TileDownload> lostDatas = regionMapInfo.getLostDatas();
                            downloadTileData(lostDatas);
                            regionMapInfo = mMapEngine.getRegionStat(regionId);
                        }
                        
                    }
                    MapStatsService.countDownloadCity(mDownloadCity, regionVersionMap);
                } else {
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
                if (mIsStop == false
                        && mapVersionQuery.isStop() == false 
                        && statusCode != BaseQuery.STATUS_CODE_NETWORK_OK) {
                    mHandler.post(toast);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            stop();
            mDownloadCityTaskMap.remove(mDownloadCity.getCityInfo().getCName());
            if (mDownloadCity.mDownloadedSize >= mDownloadCity.mTotalSize) {
                deleteDownloadCity(mThis, mDownloadCityList,mDownloadCity);
                addDownloadCity(mThis, mDownloadCityList, mDownloadCity, true);
                notifyDataSetChanged();
            }
            download();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        public void onProgressUpdate(int len) {
            if (len > 0) {
                mDownloadCity.addDownloadedSize(len);
            }
        }
        
        @Override
        public int fillMapTile(List<TileDownload> tileDownloadList, int rid, byte[] data, int start) {
            if (tileDownloadList == null || tileDownloadList.isEmpty() || data == null) {
                return -1;
            }
            
            int remainDataLenth = data.length - start;
            for(TileDownload tileInfo:tileDownloadList) {
                if (remainDataLenth <= 0) {
                    break;
                } 
                int tileLen = tileInfo.getLength();
                if (tileLen < 1 || rid != tileInfo.getRid()) {
                    continue;
                }
                if (tileLen <= remainDataLenth) {
                    byte[] dest = new byte[tileLen];
                    System.arraycopy(data, start, dest, 0, tileLen);
                    int ret = mMapEngine.writeRegion(tileInfo.getRid(), tileInfo.getOffset(), dest, tileInfo.getVersion());
                    if (ret != 0) {
                        return -1;
                    }
                    //let the tile be empty
                    tileInfo.setLength(-1);
                    start += tileLen;
                    remainDataLenth -= tileLen;
                } else {
                    byte[] dest = new byte[remainDataLenth];
                    System.arraycopy(data, start, dest, 0, remainDataLenth);
                    int ret = mMapEngine.writeRegion(tileInfo.getRid(), tileInfo.getOffset(), dest, tileInfo.getVersion());
                    if (ret != 0) {
                        return -1;
                    }
                    tileInfo.setOffset(tileInfo.getOffset() + remainDataLenth);
                    tileInfo.setLength(tileLen - remainDataLenth); 
                    remainDataLenth = 0;
                }
            }
            return 0;
        }
        
        @Override
        public void upgradeRegion(int rid) {
            mMapEngine.removeRegion(rid);
            mMapMetaFileDownload.setup(rid);
            mMapMetaFileDownload.query();
            if (mMapMetaFileDownload.isStop() == false) {
                statusCode = mMapTileDataDownload.getStatusCode();
            }
        }
        
        private void downloadTileData(List<TileDownload> lostDatas) {
            if (lostDatas == null) {
                return;
            }
            // download tile
            for (TileDownload tileInfo : lostDatas) {
                if (tileInfo.getLength() <= 0) {
                    // when len is 0, this tile has been downloaded
                    continue;
                }
                if (mIsStop) {
                    break;
                }
                mMapTileDataDownload.setup(lostDatas, tileInfo.getRid());
                mMapTileDataDownload.query();
                if (mMapMetaFileDownload.isStop() == false) {
                    statusCode = mMapTileDataDownload.getStatusCode();
                }
            } // end for
        }
    }

    private synchronized void download() {
        if (mMapEngine.isExternalStorage() == false) {
            return;
        }
        if (mDownloadCityTaskMap.size() < 1) {
            DownloadCity downloadCity = getFirstWaitingDownloadCity();
            if (downloadCity != null) {
                downloadCity.setState(DownloadCity.DOWNLOADING);
                DownloadAsyncTask downloadAsyncTask = new DownloadAsyncTask(downloadCity);
                mDownloadCityTaskMap.put(downloadCity.getCityInfo().getCName(), downloadAsyncTask);
                downloadAsyncTask.execute();
            }
        }
        updateDownloadCityElv();
    }

    private DownloadCity getFirstWaitingDownloadCity() {
        for (DownloadCity downloadCity : mDownloadCityList) {
            if (downloadCity.getState() == DownloadCity.WAITING && downloadCity.getCityInfo().getType() == CityInfo.TYPE_CITY) {
                return downloadCity;
            }
        }
        return null;
    }
    
    private class SuggestCityAdapter extends ArrayAdapter<CityInfo> {
        
        public SuggestCityAdapter(Context context, List<CityInfo> cityList) {
            super(context, R.layout.add_city_list_item, cityList);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.add_city_list_item, parent, false);
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
                view = mLayoutInflater.inflate(R.layout.add_city_list_item, parent, false);
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
                view = mLayoutInflater.inflate(R.layout.map_add_list_item, parent, false);
            } else {
                view = convertView;
            }
            View provinceView = view.findViewById(R.id.province_view);
            View cityView = view.findViewById(R.id.city_view);
            if (cityInfoList.size() == 0) {
                String cname = cityInfo.getCName();
                int cityId = cityInfo.getId();
                if (cityId == CITY_ID_TITLE_ONE || cityId == CITY_ID_TITLE_TWO || cityId == CITY_ID_TITLE_THREE) {
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
                mActionLog.addAction(ActionLog.Title_Left_Back, mActionTag);
                onBack();
                break;
                
            case R.id.add_btn:
                mActionLog.addAction(ActionLog.DownloadMapAdd);
                changeState(STATE_ADD);
                break;

            case R.id.close_btn:
                mWifiView.setVisibility(View.GONE);
                break;
                
            default:
                break;
        }
    }
    
    private void changeState(int state) {
        mState = state;
        if (mState == STATE_EMPTY) {
            mTitleBtn.setText(R.string.map_download);
            mAddBtn.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.VISIBLE);
            mDownloadView.setVisibility(View.GONE);
            mAddView.setVisibility(View.GONE);
            mKeywordEdt.setText("");
            hideSoftInput();
            mCloseBtn.requestFocus();
        } else if (mState == STATE_DOWNLOAD) {
            mTitleBtn.setText(R.string.map_download);
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
    
    public static DownloadCity getProvinceDownloadCity(String pName, int id) {
        CityInfo provinceCityInfo = new CityInfo();
        provinceCityInfo.setCName(pName);
        provinceCityInfo.setId(id);
        provinceCityInfo.setType(CityInfo.TYPE_PROVINCE);
        DownloadCity provinceDownloadCity = new DownloadCity(provinceCityInfo);
        return provinceDownloadCity;
    }
    
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
    
    public static void addDownloadCity(Context context, List<DownloadCity> list, DownloadCity downloadCity, boolean isManual) {
        CityInfo cityInfo = downloadCity.getCityInfo();
        int orderCityId = cityInfo.getId();
        if (orderCityId < 0) {
            orderCityId = 1;
        }
        if (getDownloadCity(list, cityInfo) != null) {
            return;
        }
        String pName = cityInfo.getCProvinceName();
        boolean exist = false;
        float percent = downloadCity.getPercent();
        if (downloadCity.getState() == DownloadCity.MAYUPDATE) {
            downloadCity.cityInfo.order = CITY_ID_TITLE_ONE+orderCityId;
            if (list.size() > 0) {
                DownloadCity downloadCity1 = list.get(0);
                if (downloadCity1.getCityInfo().getId() == CITY_ID_TITLE_ONE) {
                    exist = true;
                }
            }
            if (exist == false) {
                DownloadCity downloadCityUpdate = getUpdateTitle(context);
                list.add(0, downloadCityUpdate);
            }
            list.add(1, downloadCity);
        } else if ((isManual || percent > PERCENT_VISIBILITY) && percent < PERCENT_COMPLETE) {
            downloadCity.cityInfo.order = CITY_ID_TITLE_TWO+orderCityId;
            int positionComplete = list.size();
            int positionDownload = 0;
            for(int i = 0, size = list.size(); i < size; i++) {
                DownloadCity downloadCity1 = list.get(i);
                if (downloadCity1.getCityInfo().getId() == CITY_ID_TITLE_TWO) {
                    exist = true;
                    positionDownload = i;
                } else if (downloadCity1.getCityInfo().getId() == CITY_ID_TITLE_THREE) {
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
            downloadCity.cityInfo.order = CITY_ID_TITLE_THREE+orderCityId;
            DownloadCity province = null;
            for(int i = 0, size = list.size(); i < size; i++) {
                DownloadCity downloadCity1 = list.get(i);
                if (downloadCity1.getCityInfo().getId() == CITY_ID_TITLE_THREE) {
                    exist = true;
                } else if (exist) {
                    if (downloadCity1.getCityInfo().getType() == CityInfo.TYPE_PROVINCE && pName.equals(downloadCity1.getCityInfo().getCName())) {
                        province = downloadCity1;
                        break;
                    }
                }
            }
            if (exist == false) {
                DownloadCity downloadCityUpdate = getCompleteTitle(context);
                list.add(list.size(), downloadCityUpdate);
            }
            if (MapEngine.hasCity(cityInfo.getId())) {
                list.add(list.size(), downloadCity);
            } else if (province == null) {
                orderCityId = cityInfo.getId();
                MapEngine mapEngine = MapEngine.getInstance();
                List<String> cityNameList = mapEngine.getCitylist(pName);
                if (cityNameList.size() > 0) {
                    CityInfo cityInfo2 = mapEngine.getCityInfo(mapEngine.getCityid(cityNameList.get(cityNameList.size()-1)));
                    if (cityInfo2.isAvailably()) {
                        orderCityId = cityInfo2.getId();
                    }
                }
                province = getProvinceDownloadCity(pName, orderCityId);
                province.cityInfo.order = CITY_ID_TITLE_THREE+orderCityId+100; // +100是因为广州作为广东的省会，其id小于天津和重庆
                province.childList.add(downloadCity);
                list.add(list.size(), province);
            } else {
                province.childList.add(downloadCity);
                Collections.sort(province.childList, sDownloadCityComparator);
            }
        }
        Collections.sort(list, sDownloadCityComparator);
    }
    
    public static void deleteDownloadCity(Context context, List<DownloadCity> list, DownloadCity downloadCity) {
        boolean delete = false;
        if (list.contains(downloadCity)) {
            list.remove(downloadCity);
            delete = true;
        }
        if (delete == false) {
            for(int i = list.size()-1; i >= 0; i--) {
                DownloadCity downloadCity1 = list.get(i);
                List<DownloadCity> childList = downloadCity1.childList; 
                if (childList.contains(downloadCity)) {
                    childList.remove(downloadCity);
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
            CityInfo cityInfo1 = downloadCity1.getCityInfo();
            if (cityInfo1.getId() == CITY_ID_TITLE_ONE) {
                state = CITY_ID_TITLE_ONE;
                sizeUpdate = 0;
            } else if (cityInfo1.getId() == CITY_ID_TITLE_TWO) {
                state = CITY_ID_TITLE_TWO;
                sizeDownload = 0;
            } else if (cityInfo1.getId() == CITY_ID_TITLE_THREE) {
                state = CITY_ID_TITLE_THREE;
                sizeComplete = 0;
            } else {
                if (state == CITY_ID_TITLE_ONE && sizeUpdate == 0) {
                    sizeUpdate = 1;
                }
                if (state == CITY_ID_TITLE_TWO && sizeDownload == 0) {
                    sizeDownload = 1;
                }
                if (state == CITY_ID_TITLE_THREE && sizeComplete == 0) {
                    sizeComplete = 1;
                }
            }
        }
        if (sizeUpdate == 0) {
            list.remove(getUpdateTitle(context));
        }
        if (sizeDownload == 0) {
            list.remove(getDownloadingTitle(context));
        }
        if (sizeComplete == 0) {
            list.remove(getCompleteTitle(context));
        }
    }
}

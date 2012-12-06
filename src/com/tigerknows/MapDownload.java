package com.tigerknows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.RegionMapInfo;
import com.tigerknows.maps.TileDownload;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.maps.MapEngine.RegionInfo;
import com.tigerknows.maps.MapEngine.RegionMetaVersion;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.MapMetaFileDownload;
import com.tigerknows.model.MapTileDataDownload;
import com.tigerknows.model.MapVersionQuery;
import com.tigerknows.model.Response;
import com.tigerknows.model.MapVersionQuery.RegionDataInfo;
import com.tigerknows.service.MapStatsService;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.view.StringArrayAdapter;

/**
 * 分为两个界面：
 * 下载界面mDownloadListView：显示手机上的地图信息，百分比，下载、停止、删除、更新等操作；
 * 搜索添加界面mAddListView：给出推荐的省市添加，搜索添加；
 * @author zhouwentao
 *
 */
public class MapDownload extends BaseActivity implements View.OnClickListener {
    
    static final String TAG= "MapDownload";
    
    private Button mDownloadBtn;
    
    private Button mAddBtn;
    
    private boolean mShowDownload;
    
    private View mDownloadView;
    
    private View mAddView;
    
    private ListView mDownloadCityLsv;
    private DownloadCityAdapter mDownloadAdapter;
    private List<DownloadCity> mDownloadCityList = new ArrayList<DownloadCity>();
    private HashMap<Integer, RegionDataInfo> mRegionVersionMap = null;
    private TextView mOffLineTxv;
    
    private View mInputView;    
    private EditText mKeywordEdt;    
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
    
    private Comparator<CityInfo> mCityComparator = new Comparator<CityInfo>() {

        @Override
        public int compare(CityInfo cityInfo1, CityInfo cityInfo2) {
            return cityInfo1.getId() - cityInfo2.getId();
        };
    };
    
//    private Comparator<DownloadCity> mDownloadCityComparator = new Comparator<DownloadCity>() {
//
//        @Override
//        public int compare(DownloadCity downloadCity1, DownloadCity downloadCity2) {
//
//            CityInfo cityInfo1 = ((DownloadCity) downloadCity1).getCityInfo();
//            CityInfo cityInfo2 = ((DownloadCity) downloadCity2).getCityInfo();
//            return cityInfo1.getId() - cityInfo2.getId();
//        };
//    };
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.DownloadMap;
        
        setContentView(R.layout.map_download);
        findViews();
        setListener();
        
        mTitleTxv.setText(R.string.download_map);
        mRightTxv.setVisibility(View.GONE);
        
        mDownloadAdapter = new DownloadCityAdapter();
        mDownloadCityLsv.setAdapter(mDownloadAdapter);
        mAddCityElv.setGroupIndicator(null);
        
        mSuggestCityAdapter = new SuggestCityAdapter(mThis, mSuggestCityItemList);
        mSuggestCityLsv.setAdapter(mSuggestCityAdapter);
        
        mMapEngine = MapEngine.getInstance();
        
        if (sAllAddCityInfoList == null) {
            sAllAddCityInfoList = new ArrayList<CityInfo>();
            List<CityInfo> allCityInfoList = mMapEngine.getAllProvinceCityList(mThis);  
            for(CityInfo cityInfo : allCityInfoList) {
                CityInfo cityInfo1 = cityInfo.clone();
                List<CityInfo> cityInfoList = cityInfo1.getCityList();
                if (cityInfoList.size() > 1) {
                    CityInfo cityInfo2 = new CityInfo();
                    cityInfo2.setCName(mThis.getString(R.string.all_province));
                    cityInfo2.setType(CityInfo.TYPE_PROVINCE);
                    cityInfo2.getCityList().addAll(cityInfoList);
                    Collections.sort(cityInfoList, mCityComparator);
                    cityInfoList.add(0, cityInfo2);
                }
                Collections.sort(cityInfoList, mCityComparator);
                sAllAddCityInfoList.add(cityInfo1);
            }
            
    //        CityInfo cityInfo = mMapEngine.getCityInfo(MapEngine.CITY_ID_QUANGUO);
    //        cityInfo.setType(CityInfo.TYPE_PROVINCE);
    //        CityInfo cityInfo1 = mMapEngine.getCityInfo(MapEngine.CITY_ID_QUANGUO);
    //        cityInfo.getCityList().add(cityInfo1);
    //        mAllAddCityInfoList.add(cityInfo);
            
            Collections.sort(sAllAddCityInfoList, mCityComparator);
        }
        mAddCityExpandableListAdapter = new AddCityExpandableListAdapter();
        mAddCityElv.setAdapter(mAddCityExpandableListAdapter);
    }
        
    @Override
    public void onResume() {
        super.onResume();

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
                    mRegionVersionMap = mapVersionQuery.getRegionVersion();
                    for(DownloadCity downloadCity : downloadCityList) {
                        MapStatsService.countDownloadCity(downloadCity, mRegionVersionMap);
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
        refreshDownloadAdapter(mMapEngine.getDownloadCityList());
        initIntent();
        if (mMapEngine.isExternalStorage() == false) {
            for(int i = mDownloadCityList.size()-1; i >= 0; i--) {
                DownloadCity downloadCity = mDownloadCityList.get(i);
                if (downloadCity.getState() == DownloadCity.WAITING || downloadCity.getState() == DownloadCity.DOWNLOADING) {
                    downloadCity.setState(DownloadCity.STOPPED);
                }
            }
        }
        if (mDownloadCityList.isEmpty()) {
            Toast.makeText(mThis, R.string.please_add_city, Toast.LENGTH_LONG).show();
            refreshTab(false);
        } else {
            refreshTab(true);
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
                List<CityInfo> cityInfoList = new ArrayList<CityInfo>();
                for(int cityId: cityIdList) {
                    cityInfoList.add(mMapEngine.getCityInfo(cityId));
                }
                addDownloadCityList(cityInfoList, DownloadCity.STOPPED, false);
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
        
    @Override
    public void onPause() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
        
        Iterator<DownloadAsyncTask> iterator = mDownloadCityTaskMap.values().iterator();
        while (iterator.hasNext()) {
            DownloadAsyncTask tkAsyncTask = iterator.next();
            if (tkAsyncTask != null && !tkAsyncTask.mIsStop) {
                tkAsyncTask.stop();
                tkAsyncTask.mDownloadCity.setState(DownloadCity.WAITING);
            }
        }
        mDownloadCityTaskMap.clear();
        
        if (mLoadData) {
            StringBuilder s = new StringBuilder();
            for (DownloadCity downloadCity : mDownloadCityList) {
                if (downloadCity.getCityInfo().getType() == CityInfo.TYPE_CITY) {                            
                    s.append(";"); 
                    s.append(downloadCity.getCityInfo().getCName());
                    s.append(","); 
                    s.append(downloadCity.getTotalSize()); 
                    s.append(","); 
                    s.append(downloadCity.getDownloadedSize()); 
                    s.append(","); 
                    s.append(downloadCity.getState()); 
                }
            }
            if (s.length() > 1) {
                TKConfig.setPref(mThis, TKConfig.PREFS_MAP_DOWNLOAD_CITYS, s.substring(1));
            } else {
                TKConfig.setPref(mThis, TKConfig.PREFS_MAP_DOWNLOAD_CITYS, "");
            }
            mMapEngine.statsMapEnd(mDownloadCityList, true);
        }
        super.onPause();
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
        mDownloadBtn = (Button) findViewById(R.id.download_btn);
        mAddBtn = (Button) findViewById(R.id.add_btn);
        mDownloadView = findViewById(R.id.download_view);
        mAddView = findViewById(R.id.add_view);
        mDownloadCityLsv = (ListView) findViewById(R.id.download_city_lsv);
        mOffLineTxv = (TextView) findViewById(R.id.off_line_txv);
        mInputView = (RelativeLayout) findViewById(R.id.input_view);
        mKeywordEdt = (EditText) findViewById(R.id.input_edt);
        mAddCityElv = (ExpandableListView) findViewById(R.id.add_city_elv);
        mSuggestCityLsv = (ListView) findViewById(R.id.suggest_city_lsv);
    }

    protected void setListener() {
        super.setListener();
        mDownloadBtn.setOnClickListener(this);
        mAddBtn.setOnClickListener(this);
        mDownloadCityLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int position, long id) {
                final DownloadCity downloadCity = mDownloadCityList.get(position);
                if (downloadCity.getCityInfo().getType() == CityInfo.TYPE_PROVINCE) {
                    return;
                }
                final String cityName = downloadCity.getCityInfo().getCName();
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

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mThis);
        
                DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                    public final void onClick(DialogInterface dialog, int which) {
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
                            notifyDataSetChanged();
                            download();
                        } else if (str.equals(mThis.getString(R.string.pause_download))) {
                            mActionLog.addAction(ActionLog.DownloadMapPause, cityName);
                            DownloadAsyncTask task = mDownloadCityTaskMap.remove(downloadCity.getCityInfo().getCName());
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
                                                String cityName = downloadCity.getCityInfo().getCName();
                                                mActionLog.addAction(ActionLog.DownloadMapDelete, cityName);
                                                DownloadAsyncTask tkAsyncTask = mDownloadCityTaskMap.remove(downloadCity.getCityInfo().getCName());
                                                if (tkAsyncTask != null && !tkAsyncTask.mIsStop) {
                                                    tkAsyncTask.stop();
                                                }
                                                downloadCity.setState(DownloadCity.STOPPED);
                                                
                                                mMapEngine.removeCityData(cityName);
                                                List <CityInfo> cityInfoList = downloadCity.getCityInfo().getCityList();
                                                for(int i = 0, size = cityInfoList.size(); i < size; i++) {
                                                    mMapEngine.removeLastRegionId(cityInfoList.get(i).getId());
                                                }
                                                mDownloadCityList.remove(downloadCity);
                                                boolean existOtherCity = false;
                                                for(DownloadCity downloadCity1 : mDownloadCityList) {
                                                    if (downloadCity1.getCityInfo().getType() == CityInfo.TYPE_CITY &&
                                                            downloadCity1.getCityInfo().getCProvinceName().equals(downloadCity.getCityInfo().getCProvinceName())) {
                                                        existOtherCity = true;
                                                        break;
                                                    }
                                                }
                                                
                                                if (!existOtherCity) {
                                                    int index = 0;
                                                    existOtherCity = false; 
                                                    for(DownloadCity downloadCity1 : mDownloadCityList) {
                                                        if (downloadCity1.getCityInfo().getType() == CityInfo.TYPE_PROVINCE &&
                                                                downloadCity1.getCityInfo().getCName().equals(downloadCity.getCityInfo().getCProvinceName())) {
                                                            existOtherCity = true;
                                                            break;
                                                        }
                                                        index++;
                                                    }
                                                    
                                                    if (existOtherCity && index >= 0 && index < mDownloadCityList.size()) {
                                                        mDownloadCityList.remove(index);
                                                    }
                                                }
                                                
                                                notifyDataSetChanged();
                                                download();
                                                
                                                if (mDownloadCityList.size() < 1) {
                                                    Toast.makeText(mThis, R.string.please_add_city, Toast.LENGTH_LONG).show();
                                                    refreshTab(false);
                                                }
                                            }
                                        }
                                    });
                        }
                        
                    }
                };
        
                alertDialog.setTitle(R.string.select_action);
                alertDialog.setCancelable(true);
                alertDialog.setAdapter(adapter, click);
        
                alertDialog.show();
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
                    addDownloadCityList(cityInfoList, DownloadCity.WAITING, true);
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
                DownloadCity downloadCity = getDownloadCity(cityInfo);
                if (downloadCity == null && !cityInfo.getCName().equals(mThis.getString(R.string.not_find_city))) {
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
        mOffLineTxv.setOnClickListener(this);
    }
    
    private boolean properCity(CityInfo cityInfo, String userInput) {
        if (cityInfo.getType() != CityInfo.TYPE_CITY) {
            return false;
        }
        String lowerEName = cityInfo.getEName().toLowerCase();
        String lowerInput = userInput.toLowerCase();
        String cName = cityInfo.getCName();
        if (cName.contains(userInput)) {
            return true;
        }
        String firstInputStr = lowerInput.substring(0, 1);
        String otherUserInput = lowerInput.substring(1, lowerInput.length());
        String otherENameStr = lowerEName.substring(1, lowerEName.length());
        if (lowerEName.startsWith(firstInputStr) && otherENameStr.contains(otherUserInput)) {
            return true;
        }
        return false;
    }
    
    private void showAddCityOrSuggestCity(String userInput) {
        if (mInputView.getVisibility() != View.VISIBLE) {
            return;
        }
        ArrayList<CityInfo> suggestCityList = new ArrayList<CityInfo>();
        if (!TextUtils.isEmpty(userInput)) {
            for (CityInfo cityInfo : sAllAddCityInfoList) {
                List<CityInfo> cityInfoList = cityInfo.getCityList();
                for(CityInfo cityInfo1 : cityInfoList) {
                    if (properCity(cityInfo1, userInput)) {
                        suggestCityList.add(cityInfo1);
                    }
                }
            }
        }
        mSuggestCityItemList.clear();
        mSuggestCityItemList.addAll(suggestCityList);
        if (mSuggestCityItemList.size() == 0 && !TextUtils.isEmpty(userInput)) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setCName(mThis.getString(R.string.not_find_city));
            mSuggestCityItemList.add(cityInfo);
        }
        mSuggestCityAdapter.notifyDataSetChanged();

        if (mSuggestCityItemList.size() > 0) {
            mAddCityElv.setVisibility(View.GONE);
            mSuggestCityLsv.setVisibility(View.VISIBLE);
        } else {
            mAddCityElv.setVisibility(View.VISIBLE);
            mSuggestCityLsv.setVisibility(View.GONE);
        }
    }
    
    private void addDownloadCity(CityInfo addCityInfo, int state) {
        DownloadCity addDownloadCity = getDownloadCity(addCityInfo);
        if (addDownloadCity == null) {
            if (mMapEngine.isExternalStorage() == false && state == DownloadCity.WAITING) {
                Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                return;
            }
            List<DownloadCity> downloadCityList = new ArrayList<DownloadCity>();
            for(DownloadCity downloadCity1 : mDownloadCityList) {
                if (downloadCity1.getCityInfo().getType() == CityInfo.TYPE_CITY) {
                    downloadCityList.add(downloadCity1);
                }
            }
            
            addDownloadCity = new DownloadCity(addCityInfo, state);
            downloadCityList.add(addDownloadCity);
            refreshDownloadAdapter(downloadCityList);
        }

        MapStatsService.countDownloadCity(addDownloadCity, mRegionVersionMap);
        
//        Collections.sort(mDownloadCityList, mDownloadCityComparator);
        notifyDataSetChanged();
        Toast.makeText(mThis, R.string.exist_download_list, Toast.LENGTH_LONG).show();
        download();
    }
    
    private void addDownloadCityList(List<CityInfo> addCityInfo, int state, boolean showTip) {   
        List<DownloadCity> downloadCityList = new ArrayList<DownloadCity>();
        for(DownloadCity downloadCity1 : mDownloadCityList) {
            if (downloadCity1.getCityInfo().getType() == CityInfo.TYPE_CITY) {
                downloadCityList.add(downloadCity1);
            }
        }
        
        for(CityInfo cityInfo : addCityInfo) {
            DownloadCity addDownloadCity = getDownloadCity(cityInfo);
            if (addDownloadCity != null) {
                MapStatsService.countDownloadCity(addDownloadCity, mRegionVersionMap);
                continue;
            } else {
                if (mMapEngine.isExternalStorage() == false && state == DownloadCity.WAITING) {
                    if (showTip) {
                        Toast.makeText(mThis, R.string.not_enough_space, Toast.LENGTH_LONG).show();
                    }
                    return;
                }
            }
            addDownloadCity = new DownloadCity(cityInfo, state);
            MapStatsService.countDownloadCity(addDownloadCity, mRegionVersionMap);
            downloadCityList.add(addDownloadCity);
        }
        
        refreshDownloadAdapter(downloadCityList);
//        Collections.sort(mDownloadCityList, mDownloadCityComparator);
        notifyDataSetChanged();
        if (showTip) {
            Toast.makeText(mThis, R.string.exist_download_list, Toast.LENGTH_LONG).show();
        }
        download();
    }
    
    private void notifyDataSetChanged() {
        mDownloadAdapter.notifyDataSetChanged();
        mAddCityExpandableListAdapter.notifyDataSetChanged();
        mSuggestCityAdapter.notifyDataSetChanged();
    }
    
    private void refreshDownloadAdapter(List<DownloadCity> downloadCityList) {
        mDownloadCityList.clear();
        for(CityInfo cityInfo : sAllAddCityInfoList) {
            List<CityInfo> childCityInfoList = cityInfo.getCityList();
            String provinceName = cityInfo.getCName();
            if (childCityInfoList.size() == 1) {
                for(DownloadCity downloadCity : downloadCityList) {
                    if (provinceName.equals(downloadCity.getCityInfo().getCName())) {
                        mDownloadCityList.add(downloadCity);
                    }
                }
            } else {
                for(CityInfo childCityInfo : childCityInfoList) {
                    for(DownloadCity downloadCity : downloadCityList) {
                        if (childCityInfo.getCName().equals(downloadCity.getCityInfo().getCName())) {
                            boolean isExist = false;
                            for(DownloadCity existDownloadCity : mDownloadCityList) {
                                String existProvinceName = existDownloadCity.getCityInfo().getCProvinceName();
                                if (existProvinceName != null && existProvinceName.equals(provinceName)) {
                                    isExist = true;
                                    break;
                                }
                            }
                            
                            if (!isExist) {
                                CityInfo provinceCityInfo = new CityInfo();
                                provinceCityInfo.setCName(childCityInfo.getCProvinceName());
                                provinceCityInfo.setType(CityInfo.TYPE_PROVINCE);
                                DownloadCity provinceDownloadCity = new DownloadCity(provinceCityInfo);
                                mDownloadCityList.add(provinceDownloadCity);
                            }
                            mDownloadCityList.add(downloadCity);
                        }
                    }
                }
            }
        }
    }
    
    private class DownloadCityAdapter extends ArrayAdapter<DownloadCity> {

        Drawable drawable;
        public DownloadCityAdapter() {
            super(mThis, R.layout.map_download_list_item, mDownloadCityList);
            drawable = mThis.getResources().getDrawable(R.drawable.ic_map_upgrade);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.map_download_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            DownloadCity downloadCity = (DownloadCity)getItem(position);
            
            if (downloadCity.getCityInfo().getType() == CityInfo.TYPE_PROVINCE) {
                View provinceView = view.findViewById(R.id.province_view);
                View cityView = view.findViewById(R.id.city_view);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                TextView nameTxv = (TextView) provinceView.findViewById(R.id.text_txv);
                nameTxv.setText(downloadCity.getCityInfo().getCName());
            } else {
                View provinceView = view.findViewById(R.id.province_view);
                View cityView = view.findViewById(R.id.city_view);
                provinceView.setVisibility(View.GONE);
                cityView.setVisibility(View.VISIBLE);
                TextView nameTxv = (TextView) cityView.findViewById(R.id.text_txv);
                TextView percentTxv = (TextView) cityView.findViewById(R.id.percent_txv);
                ProgressBar progressBar = (ProgressBar) cityView.findViewById(R.id.progress_prb);
                String size = (downloadCity.getTotalSize() > 0 ? mThis.getString(R.string.brackets, mThis.getString(R.string._m, downloadCity.getTotalSizeTip())) : "");
                nameTxv.setText(downloadCity.getCityInfo().getCName()+size);
                
                if (downloadCity.getState() == DownloadCity.MAYUPDATE) {
                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                    percentTxv.setCompoundDrawablePadding(0);
                    percentTxv.setCompoundDrawables(drawable, null, null, null);
                    percentTxv.setText(mThis.getString(R.string.may_upgrade));
                } else {
                    percentTxv.setCompoundDrawables(null, null, null, null);
                    percentTxv.setText((downloadCity.mState == DownloadCity.DOWNLOADING || downloadCity.mState == DownloadCity.WAITING) ? mThis.getString(R.string.downloading_, String.valueOf(downloadCity.getDownloadPercent())) : 
                        mThis.getString(R.string.downloaded_, String.valueOf(downloadCity.getDownloadPercent())));
                    
                    if (downloadCity.getTotalSize() > 0 
                            && downloadCity.getDownloadedSize() >= downloadCity.getTotalSize()) {
                        downloadCity.setState(DownloadCity.COMPLETED);
                        percentTxv.setText(mThis.getString(R.string.completed));
                    }
                }
                progressBar.setProgress((int)Float.parseFloat(downloadCity.getDownloadPercent()));
            }
            
            return view;
        }
    }
    
    public static class DownloadCity {
        private CityInfo mCityInfo;
        private List<Integer> mRegionIdList = null;
        
        private int mState = STOPPED;
        private int mTotalSize = 0;
        private int mDownloadedSize = 0;
        
        public static final int WAITING = 0; //在等待下载队列中
        public static final int DOWNLOADING = 1; //downloading
        public static final int STOPPED = 2; //paused, stopped 0% start, need to download metefile
        public static final int COMPLETED = 3; //100% 
        public static final int MAYUPDATE = 4; //can be updated
        
        public DownloadCity(CityInfo cityInfo) {
            this(cityInfo, STOPPED);
        }

        public DownloadCity(CityInfo cityInfo, int state) {
            this.mCityInfo = cityInfo;
            this.mState = state;
        }
        
        public CityInfo getCityInfo() {
            return mCityInfo;
        }
        
        public List<Integer> getRegionIdList() {
            if (mRegionIdList == null) {
                mRegionIdList = MapEngine.getInstance().getRegionIdListByCityName(mCityInfo.getCName());
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
            return mState;
        }
        
        public void setState(int state) {
            if (state == WAITING || state == DOWNLOADING || state == COMPLETED || state == STOPPED || state == MAYUPDATE) {
                this.mState = state;
            }
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
            while (mDownloadCity.mState != DownloadCity.COMPLETED && mIsStop == false) {
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
            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            textView.setText(cname);
            
            DownloadCity downloadCity = getDownloadCity(cityInfo);
            if (downloadCity != null || cityInfo.getCName().equals(mThis.getString(R.string.not_find_city))) {
                textView.setTextColor(mThis.getResources().getColor(R.color.text_gray));
                view.setBackgroundResource(R.color.gray_light);
            } else {
                textView.setTextColor(mThis.getResources().getColor(android.R.color.black));
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
            }
            
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
            view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
            
            CityInfo cityInfo = (CityInfo)getChild(groupPosition, childPosition);
            setDataToCityView(view, cityInfo, true);
            
            return view;
        }
        
        private void setDataToCityView(View view, CityInfo cityInfo, boolean appendSpace) {
            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            
            if (cityInfo.getType() == CityInfo.TYPE_CITY) {
                textView.setText((appendSpace ? "    " : "")+cname);
                DownloadCity downloadCity = getDownloadCity(cityInfo);
                if (downloadCity != null) {
                    textView.setTextColor(mThis.getResources().getColor(R.color.text_gray));
                    view.setBackgroundResource(R.color.gray_light);
                } else {
                    textView.setTextColor(mThis.getResources().getColor(android.R.color.black));
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                }
            } else {
                textView.setText((appendSpace ? "    " : "" )+cname);
                List<CityInfo> cityInfoList = cityInfo.getCityList();
                boolean isAllExist = true;
                for(CityInfo cityInfo1 : cityInfoList) {
                    DownloadCity downloadCity = getDownloadCity(cityInfo1);
                    if (downloadCity == null) {
                        isAllExist = false;
                        break;
                    }
                }
                
                if (isAllExist) {
                    textView.setTextColor(mThis.getResources().getColor(R.color.text_gray));
                    view.setBackgroundResource(R.color.gray_light);
                } else {
                    textView.setTextColor(mThis.getResources().getColor(android.R.color.black));
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                }
            }            
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
            if (cityInfoList.size() != 1) {
                View provinceView = view.findViewById(R.id.province_view);
                View cityView = view.findViewById(R.id.city_view);
                provinceView.setVisibility(View.VISIBLE);
                cityView.setVisibility(View.GONE);
                
                String cname = cityInfo.getCName();
                TextView textView = (TextView)provinceView.findViewById(R.id.text_txv);
                textView.setText(cname);
                
                ImageView imageView = (ImageView)provinceView.findViewById(R.id.icon_imv);
                if (isExpanded) {            
                    imageView.setBackgroundResource(R.drawable.icon_arrow_up);
                } else {
                    imageView.setBackgroundResource(R.drawable.icon_arrow_down);
                }
            } else {
                View provinceView = view.findViewById(R.id.province_view);
                View cityView = view.findViewById(R.id.city_view);
                provinceView.setVisibility(View.GONE);
                cityView.setVisibility(View.VISIBLE);
                cityInfo = cityInfoList.get(0);
                setDataToCityView(cityView, cityInfo, false);
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
    
    private DownloadCity getDownloadCity(CityInfo cityInfo) {
        DownloadCity downloadCity = null;
        for(DownloadCity download : mDownloadCityList) {
            if (download.getCityInfo().getCName().equals(cityInfo.getCName()) && download.getCityInfo().getType() == CityInfo.TYPE_CITY) {
                downloadCity = download;
                break;
            }
        }
        
        return downloadCity;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.off_line_txv:
                mActionLog.addAction(ActionLog.DownloadMapOffOnline);
                CommonUtils.showNormalDialog(mThis,
                        mThis.getString(R.string.off_line_download_dialog_title),
                        mThis.getString(R.string.off_line_download_dialog_message),
                        mThis.getString(R.string.i_know),
                        null,
                        null);
                break;
                
            case R.id.download_btn:
                mActionLog.addAction(ActionLog.DownloadMapManage);
                refreshTab(true);
                break;
                
            case R.id.add_btn:
                mActionLog.addAction(ActionLog.DownloadMapAdd);
                refreshTab(false);
                break;

            default:
                break;
        }
    }
    
    private void refreshTab(boolean showDownload) {
        mShowDownload = showDownload;
        if (mShowDownload) {
            mDownloadBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mAddBtn.setBackgroundResource(R.drawable.btn_tab);
            mDownloadView.setVisibility(View.VISIBLE);
            mAddView.setVisibility(View.GONE);
            mKeywordEdt.setText("");
            hideSoftInput();
        } else {
            mDownloadBtn.setBackgroundResource(R.drawable.btn_tab);
            mAddBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mDownloadView.setVisibility(View.GONE);
            mAddView.setVisibility(View.VISIBLE);
            mKeywordEdt.setText("");
            showAddCityOrSuggestCity("");
        }
    }
}

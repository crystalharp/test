/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.ItemizedOverlayHelper;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.DataQuery.POIResponse.CityIdAndResultTotal;
import com.tigerknows.model.DataQuery.POIResponse.MapCenterAndBorderRange;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;
import com.tigerknows.widget.SuggestWordListManager;

/**
 * @author Peng Wenyue
 */
public class InputSearchFragment extends BaseFragment implements View.OnClickListener {
    
    final public static int MODE_POI = HistoryWordTable.TYPE_POI;
    final public static int MODE_TRAFFIC = HistoryWordTable.TYPE_TRAFFIC;
    final public static int MODE_BUELINE = HistoryWordTable.TYPE_BUSLINE;
    
    //用来标记请求的是哪个按钮
    final public static int REQUEST_NONE = 0;
    final public static int REQUEST_TRAFFIC_START = 1;
    final public static int REQUEST_TRAFFIC_END = 2;
    final public static int REQUEST_COMMON_PLACE = 3;
    
    private int mCurMode;
    private int mCurHisWordType;
    private int mRequestInput;

    private ListView mSuggestLsv = null;
    private LinearLayout mTrafficBtnGroup;
    private View mMapSelectPointBtn;
    private Button mFavBtn;
    
    private SuggestWordListManager mSuggestWordListManager;
    
    private Callback callback;
    
    private TKWord mTKWord;
    
    //页面的数据输出回调接口，这个页面主要用来获取一个POI，所以用POI来做参数
    public interface Callback {
        public void onConfirmed(POI p);
    }
    
    public InputSearchFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
        
        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                onConfirmed(mKeywordEdt.getText().toString().trim());
                return true;
            }
            return false;
        }
    };
    
    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mSphinx.showSoftInput(mKeywordEdt.getInput());
            }
            return false;
        }
    };
    
    private final TextWatcher mFindEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSuggestWordListManager.refresh(mKeywordEdt, mCurHisWordType);
        }

        public void afterTextChanged(Editable s) {
            mTitleFragment.refreshRightBtn(s.toString());
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_input_search, container, false);
        
        findViews();
        setListener();
        
        BtnEventHandler btnHandler = new BtnEventHandler() {
            
            @Override
            public void onBtnClicked(TKWord tkWord, int position) {
                mKeywordEdt.setText(tkWord.word);
                mTKWord = tkWord;
                mActionLog.addAction(mActionTag + ActionLog.HistoryWordInput, position, tkWord.word, tkWord.attribute);
            }
        };
        mSuggestWordListManager = new SuggestWordListManager(mSphinx, mSuggestLsv, mKeywordEdt, btnHandler, HistoryWordTable.TYPE_POI);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setVisibility(View.GONE);
        mKeywordEdt.setVisibility(View.VISIBLE);
        
        mKeywordEdt.mActionTag = mActionTag;
        mKeywordEdt.setOnEditorActionListener(mOnEditorActionListener);
        mKeywordEdt.addTextChangedListener(mFindEdtWatcher);
        mKeywordEdt.setOnTouchListener(mOnTouchListener);

        mRightBtn.setOnClickListener(this);
        
        mSphinx.showSoftInput(mKeywordEdt.getInput());
        mKeywordEdt.getInput().requestFocus();
        mTitleFragment.refreshRightBtn(mKeywordEdt.getText().toString());
        
        mSuggestWordListManager.refresh(mKeywordEdt, mCurHisWordType);
        
        switch (mCurMode) {
        //TODO:add actiontag
        case MODE_BUELINE:
            mTrafficBtnGroup.setVisibility(View.GONE);
            break;
        case MODE_TRAFFIC:
            mTrafficBtnGroup.setVisibility(View.VISIBLE);
            break;
        case MODE_POI:
            mTrafficBtnGroup.setVisibility(View.GONE);
            mActionTag = ActionLog.POIHomeInputQuery;
            break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeywordEdt.clearFocus();
        mKeywordEdt.removeTextChangedListener(mFindEdtWatcher);
    }
    
    public void setMode(int m) {
        mCurMode = m;
        mCurHisWordType = m;
    }
    
    public void setConfirmedCallback(Callback c) {
        setConfirmedCallback(c, REQUEST_NONE);
    }
    
    public void setConfirmedCallback(Callback c, int request) {
        callback = c;
        mRequestInput = request;
    }

    protected void findViews() {
        mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
        mTrafficBtnGroup = (LinearLayout) mRootView.findViewById(R.id.traffic_btn_group);
        mMapSelectPointBtn = mTrafficBtnGroup.findViewById(R.id.btn_map_position);
        mFavBtn = (Button) mRootView.findViewById(R.id.btn_fav_position);
    }

    protected void setListener() {
        
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistoryClear);
                    HistoryWordTable.clearHistoryWord(mSphinx, mCurHisWordType);
                    mSuggestWordListManager.refresh(mKeywordEdt, mCurHisWordType);
                } else {
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistory, position, tkWord.word);
                    } else {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemSuggest, position, tkWord.word);
                    }
                    mKeywordEdt.setText(tkWord.word); //处理光标问题
                    if (mCurMode == MODE_TRAFFIC) {
//                        POI poi = new POI();
//                        poi.setName(tkWord.word);
//                        poi.setPosition(tkWord.position);
                        onConfirmed(tkWord.toPOI());
                    } else {
                        onConfirmed(tkWord.word);
                    }
                }
            }
        });
        mSuggestLsv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSphinx.hideSoftInput();
                    mSuggestLsv.requestFocus();
                }
                return false;
            }
        });
        
        mMapSelectPointBtn.setOnClickListener(this);
        
        mFavBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mSphinx.showView(mSphinx.getFetchFavoriteFragment().getId());
            }
        });
        
    }

    public void onClick(View view) {
        switch (view.getId()) {
            
            case R.id.btn_map_position:
                String title;
                if (mRequestInput == REQUEST_TRAFFIC_START) {
                    title = getString(R.string.select_start_station);
                } else if (mRequestInput == REQUEST_TRAFFIC_END) {
                    title = getString(R.string.select_end_station);
                } else {
                    title = getString(R.string.select_point);
                }
                ItemizedOverlayHelper.drawClickSelectPointOverlay(mSphinx, title);
                break;
                
            case R.id.right_btn:
                String keyword = mKeywordEdt.getText().toString().trim();
                if (keyword.length() > 0) {
                    if (mCurMode == MODE_TRAFFIC) {
                        if (mTKWord != null && keyword.equals(mTKWord.word)) {
                            onConfirmed(mTKWord.toPOI());
                        } else {
                            onConfirmed(keyword);
                        }
                    } else {
                        onConfirmed(mKeywordEdt.getText().toString().trim());
                    }
                } else {
                    dismiss();
                }
                break;
            default:
        }
    }
    
    public void onConfirmed(String key) {
        switch (mCurMode) {
        case MODE_BUELINE:
            submitBuslineQuery(key);
            break;
        case MODE_TRAFFIC:
            //TODO:提交公交站点查询，并且返回原页面
            POI poi = new POI();
            poi.setName(key);
            onConfirmed(poi);
//            callback.onConfirmed(poi);
//            mSphinx.getTrafficQueryFragment().autoStartQuery(true);
//            dismiss();
            break;
        case MODE_POI:
            submitPOIQuery(key);
            break;
        default:
            break;
        }
    }
    
    public void onConfirmed(POI poi) {
        if (mCurMode == MODE_TRAFFIC) {
            callback.onConfirmed(poi);
            mSphinx.getTrafficQueryFragment().autoStartQuery(true);
            dismiss();
        }
    }
    
    public void submitPOIQuery(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            mSphinx.hideSoftInput(mKeywordEdt.getInput());
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), HistoryWordTable.TYPE_POI);
            mActionLog.addAction(mActionTag +  ActionLog.POIHomeInputQueryBtn, keyword);

            POI requestPOI = mSphinx.getCenterPOI();
            DataQuery poiQuery = mSphinx.getHomeFragment().getDataQuery(keyword);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_EXT, DataQuery.EXT_BUSLINE);
            poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false, false, requestPOI);
            mSphinx.queryStart(poiQuery);
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }

    /*TODO:放个合适的地方*/
    public static void submitBuslineQuery(Sphinx sphinx, String key, int cityId) {
        
        if (key == null) {
            return;
        }
        
        POI poi = new POI();
        poi.setName(key);
        sphinx.getTrafficQueryFragment().addHistoryWord(poi, HistoryWordTable.TYPE_BUSLINE);
        BuslineQuery buslineQuery = new BuslineQuery(sphinx);
        buslineQuery.setup(key, 0, false, R.id.view_traffic_home, sphinx.getString(R.string.doing_and_wait));
        buslineQuery.setCityId(cityId);
//        mActionLog.addAction(mActionTag +  ActionLog.TrafficBuslineBtn, key);
        sphinx.queryStart(buslineQuery);

    }
        
    public void submitBuslineQuery(String searchword) {

//        String searchword = mKeywordEdt.getText().toString().trim();
        if (TextUtils.isEmpty(searchword)){
            mSphinx.showTip(R.string.busline_name_, Toast.LENGTH_SHORT);
            return;
        }

        addHistoryWord(searchword, HistoryWordTable.TYPE_BUSLINE);
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(searchword, 0, false, getId(), getString(R.string.doing_and_wait));

        mActionLog.addAction(mActionTag +  ActionLog.TrafficBuslineBtn, searchword);
        mSphinx.queryStart(buslineQuery);
    }
        
    private void addHistoryWord(String name, int type) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, name), type);
    }

    //还原为第一次进入的状态
    public void reset() {
        mKeywordEdt.setText(null);
        mTKWord = null;
    }
    
    public void setData(String text) {
        mKeywordEdt.setText(text);
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        String apiType = baseQuery.getAPIType();
        if (BaseQuery.API_TYPE_BUSLINE_QUERY.equals(apiType)) {
            this.queryBuslineEnd((BuslineQuery)baseQuery);
        } else if (BaseQuery.API_TYPE_DATA_QUERY.equals(apiType)) {            
            dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);
        }
    }
    
    public static int dealWithPOIResponse(final DataQuery dataQuery, final Sphinx sphinx, BaseFragment baseFragment) {
        int result = 0;
        if (dataQuery.isStop()) {
            result = -1;
            return result;
        }
        
        if (BaseActivity.checkReLogin(dataQuery, sphinx, sphinx.uiStackContains(R.id.view_user_home), baseFragment.getId(), baseFragment.getId(), baseFragment.getId(), baseFragment.mCancelLoginListener)) {
            baseFragment.isReLogin = true;
            result = -2;
            return result;
        }
        
        if (BaseActivity.checkResponseCode(dataQuery, sphinx, null, true, baseFragment, false)) {
            result = -3;
            return result;
        }

        POIResponse poiResponse = (POIResponse)dataQuery.getResponse();
        mapCenterAndBorderRange(sphinx, poiResponse);
        
        BuslineModel buslineModel = poiResponse.getBuslineModel();
        if (buslineModel != null) {
            if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
                sphinx.getBuslineResultLineFragment().setData(null, dataQuery);
                sphinx.showView(R.id.view_traffic_busline_line_result);
                
                result = 1;
                return result;
            }
            
        }
        
        List<POI> poiList = null;
        POIList bPOIList = poiResponse.getBPOIList();
        if (bPOIList != null) {
            poiList = bPOIList.getList();
        }
        
        final List<CityIdAndResultTotal> cityIdAndResultTotalList = poiResponse.getCityIdAndResultTotalList();
        if (cityIdAndResultTotalList != null && cityIdAndResultTotalList.size() > 0) {
            
            if (cityIdAndResultTotalList.size() == 1) {
                if (poiList != null && poiList.size() > 0) {
                    sphinx.getResultMapFragment().setData(sphinx.getString(R.string.result_map), ActionLog.POIListMap);
                    sphinx.showView(R.id.view_result_map);
                    
                    ItemizedOverlayHelper.drawPOIOverlay(sphinx, poiList, 0);
                    
                    result = 2;
                    return result;
                }
                
            } else {
                final List<CityInfo> cityList = new ArrayList<CityInfo>();
                List<String> cityNameList = new ArrayList<String>();
                for(int i = 0, size = cityIdAndResultTotalList.size(); i < size; i++) {
                    CityIdAndResultTotal cityIdAndResultTotal = cityIdAndResultTotalList.get(i);
                    CityInfo cityInfo = MapEngine.getCityInfo((int) cityIdAndResultTotal.getCityId());
                    cityList.add(cityInfo);
                    cityNameList.add(cityInfo.getCName()+"("+cityIdAndResultTotal.getResultTotal()+")");
                }
                
                StringArrayAdapter adapter = new StringArrayAdapter(sphinx, cityNameList);
                
                View alterListView = sphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
                
                ListView listView = (ListView) alterListView.findViewById(R.id.listview);
                listView.setAdapter(adapter);
                
                listView.setOnItemClickListener(new OnItemClickListener() {
        
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
                        MapView mapView = sphinx.getMapView();
                        try {
                            mapView.centerOnPosition(cityList.get(position).getPosition());
                            CityIdAndResultTotal cityIdAndResultTotal = cityIdAndResultTotalList.get(position);
                            DataQuery newDataQuery = new DataQuery(dataQuery);
                            newDataQuery.setCityId((int) cityIdAndResultTotal.getCityId());
                            sphinx.queryStart(newDataQuery);
                        } catch (APIException e) {
                            e.printStackTrace();
                        }
                    }
                });
                
                Dialog dialog = Utility.getChoiceDialog(sphinx, alterListView, R.style.AlterChoiceDialog);
                
                TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
                titleTxv.setText(R.string.app_name);
                
                Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
                button.setVisibility(View.GONE);
                
                dialog.show();
                
                result = 3;
                return result;
            }
            
        }
        
        if (poiList != null
                && poiList.size() > 0) {
            
            POIResultFragment poiResultFragment = sphinx.getPOIResultFragment();
            poiResultFragment.setData(dataQuery);
            sphinx.showView(R.id.view_poi_result);
            sphinx.uiStackRemove(R.id.view_traffic_busline_line_result);
            
            result = 4;
            return result;
            
        } else {
            Toast.makeText(sphinx, sphinx.getString(R.string.no_result), Toast.LENGTH_LONG).show();
        }
        
        return result;
    }
    
    public static void mapCenterAndBorderRange(Sphinx sphinx, POIResponse poiResponse) {
        if (sphinx == null || poiResponse == null) {
            return;
        }
        
        MapCenterAndBorderRange mapCenterAndBorderRange = poiResponse.getMapCenterAndBorderRange();
        if (mapCenterAndBorderRange != null) {
            MapView mapView = sphinx.getMapView();
            
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
            }
        }
    }
    
    public void queryBuslineEnd(BuslineQuery buslineQuery) {
        
        BuslineModel buslineModel = buslineQuery.getBuslineModel();
        
        if (buslineModel == null) {
            mActionLog.addAction(mActionTag + ActionLog.TrafficResultBusline, -1);
            if (buslineQuery.getStatusCode() == BaseQuery.STATUS_CODE_NONE) {
                mSphinx.showTip(R.string.network_failed, Toast.LENGTH_SHORT);
            } else {
                mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
            }
        } else if (buslineModel.getType() == BuslineModel.TYPE_EMPTY) {
            mActionLog.addAction(mActionTag + ActionLog.TrafficResultBusline, -2);
            mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        } else if (buslineModel.getType() == BuslineModel.TYPE_UNSUPPORT) {
            mActionLog.addAction(mActionTag + ActionLog.TrafficResultBusline, -3);
            mSphinx.showTip(R.string.busline_not_support, Toast.LENGTH_SHORT);
        } else if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE 
                || buslineModel.getType() == BuslineModel.TYPE_STATION){
            if (((buslineModel.getLineList() == null || buslineModel.getLineList().size() <= 0) && 
            (buslineModel.getStationList() == null || buslineModel.getStationList().size() <= 0))) {
                mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
                mActionLog.addAction(mActionTag + ActionLog.TrafficResultBusline, 0);
            } else {
                if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
                    mActionLog.addAction(mActionTag + ActionLog.TrafficResultBusline, buslineQuery.getBuslineModel().getLineList().size());
                    mSphinx.getBuslineResultLineFragment().setData(buslineQuery);
                    mSphinx.showView(R.id.view_traffic_busline_line_result);
                } else if (buslineModel.getType() == BuslineModel.TYPE_STATION) {
                    mActionLog.addAction(mActionTag + ActionLog.TrafficResultBusline, buslineQuery.getBuslineModel().getStationList().size());
                    mSphinx.getBuslineResultStationFragment().setData(buslineQuery);
                    mSphinx.showView(R.id.view_traffic_busline_station_result);
                }               
            }
        }
    }
}

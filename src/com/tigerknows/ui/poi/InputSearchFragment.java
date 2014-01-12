/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
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
import android.widget.BaseAdapter;
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
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.GeoCoderResponse.GeoCoderList;
import com.tigerknows.model.DataQuery.POIResponse.CityIdAndResultTotal;
import com.tigerknows.model.DataQuery.POIResponse.MapCenterAndBorderRange;
import com.tigerknows.model.DataQuery.POIResponse.POIList;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.DataQuery.GeoCoderResponse;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
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
    
    private DataQuery mDataQuery;
    private int mCurMode;
    private int mCurHisWordType;
    private int mRequest;

    private ListView mSuggestLsv = null;
    private LinearLayout mTrafficBtnGroup;
    private View mMapSelectPointBtn;
    private Button mFavBtn;
    private Button mMyPosBtn;
    
    private SuggestWordListManager mSuggestWordListManager;
    
    private IResponsePOI mIResponsePOI;
    
    private TKWord mTKWord;
    
    /**
     * 返回POI的接口
     */
    public interface IResponsePOI {
        /**
         * 返回POI
         * @param poi
         */
        public void responsePOI(POI poi);
    }
    
    public InputSearchFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private OnEditorActionListener mKeywordEdtOnEditorActionListener = new OnEditorActionListener() {
        
        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
            if (event == null) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && 
                    (actionId == EditorInfo.IME_ACTION_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String keyword = mKeywordEdt.getText().toString().trim();
                if (mTKWord != null && keyword.equals(mTKWord.word)) {
                    submit(mTKWord);
                } else {
                    submit(keyword);
                }
                return true;
            }
            return false;
        }
    };
    
    private OnTouchListener mKeywordEdtOnTouchListener = new OnTouchListener() {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mSphinx.showSoftInput(mKeywordEdt.getInput());
            }
            return false;
        }
    };
    
    private final TextWatcher mKeywordEdtTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSuggestWordListManager.refresh(mKeywordEdt, mCurHisWordType);
        }

        public void afterTextChanged(Editable s) {
            Utility.refreshButton(mSphinx, 
                    mRightBtn, 
                    (mRequest == REQUEST_TRAFFIC_END || mRequest == REQUEST_TRAFFIC_START) ? getString(R.string.confirm) : getString(R.string.search),
                    getString(R.string.cancel), 
                    (s.toString().trim().length() > 0));
            if (mTKWord != null &&
                    s.toString().trim().equals(mTKWord.word) == false) {
                mTKWord = null;
            }
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
                mTKWord = tkWord.clone();
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
        mKeywordEdt.setOnEditorActionListener(mKeywordEdtOnEditorActionListener);
        mKeywordEdt.addTextChangedListener(mKeywordEdtTextWatcher);
        mKeywordEdt.setOnTouchListener(mKeywordEdtOnTouchListener);

        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        
        if (mDismissed) {
            mSphinx.promptShowSoftInput(mKeywordEdt.getInput());
            mKeywordEdt.getInput().requestFocus();
        }
        Utility.refreshButton(mSphinx,
                mRightBtn,
                (mRequest == REQUEST_TRAFFIC_END || mRequest == REQUEST_TRAFFIC_START) ? getString(R.string.confirm) : getString(R.string.search),
                getString(R.string.cancel),
                mKeywordEdt.isEmpty());
        
        mSuggestWordListManager.refresh(mKeywordEdt, mCurHisWordType);
        
        switch (mCurMode) {
        //TODO:add actiontag
        case MODE_BUELINE:
            mActionTag = ActionLog.TrafficInput;
            mTrafficBtnGroup.setVisibility(View.GONE);
            mKeywordEdt.setHint(getString(R.string.busline_search_hint));
            break;
        case MODE_TRAFFIC:
            mActionTag = ActionLog.TrafficInput;
            mTrafficBtnGroup.setVisibility(View.VISIBLE);
            if (mRequest == REQUEST_TRAFFIC_END) {
                mKeywordEdt.setHint(getString(R.string.end_));
            } else if (mRequest == REQUEST_TRAFFIC_START) {
                mKeywordEdt.setHint(getString(R.string.start_));
            } else if (mRequest == REQUEST_COMMON_PLACE) {
                mKeywordEdt.setHint(getString(R.string.traffic_search_hint));
            }
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
        mKeywordEdt.removeTextChangedListener(mKeywordEdtTextWatcher);
    }

    @Override
    protected void findViews() {
        super.findViews();
        mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
        mTrafficBtnGroup = (LinearLayout) mRootView.findViewById(R.id.traffic_btn_group);
        mMapSelectPointBtn = mTrafficBtnGroup.findViewById(R.id.btn_map_position);
        mFavBtn = (Button) mRootView.findViewById(R.id.btn_fav_position);
        mMyPosBtn = (Button) mRootView.findViewById(R.id.btn_my_position);
    }

    @Override
    protected void setListener() {
        super.setListener();
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistoryClear);
                    HistoryWordTable.clearHistoryWord(mSphinx, mCurHisWordType);
                    mSuggestWordListManager.refresh(mKeywordEdt, mCurHisWordType);
                } else {
                    mTKWord = tkWord.clone();
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistory, position, tkWord.word);
                    } else {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemSuggest, position, tkWord.word);
                    }
                    
                    mKeywordEdt.setText(tkWord.word); //处理光标问题
                    submit(tkWord);
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
        
        mMyPosBtn.setOnClickListener(this);
        
        mFavBtn.setOnClickListener(this);
        
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            
            case R.id.btn_map_position:
                mActionLog.addAction(mActionTag + ActionLog.TrafficInputMapPosition);
                
                String title;
                if (mRequest == REQUEST_TRAFFIC_START) {
                    title = getString(R.string.select_start_station);
                } else if (mRequest == REQUEST_TRAFFIC_END) {
                    title = getString(R.string.select_end_station);
                } else {
                    title = getString(R.string.confirm_place);
                }
                
                Toast.makeText(mSphinx, R.string.move_map_select_point, Toast.LENGTH_LONG).show();
                mSphinx.getResultMapFragment().setData(getString(R.string.map_select_point), ActionLog.ResultMapSelectPoint);
                mSphinx.showView(R.id.view_result_map);
                
                ItemizedOverlayHelper.drawClickSelectPointOverlay(mSphinx, title);
                
                break;
                
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton);
                String keyword = mKeywordEdt.getText().toString().trim();
                if (keyword.length() > 0) {
                    if (mTKWord != null && keyword.equals(mTKWord.word)) {
                        submit(mTKWord);
                    } else {
                        submit(keyword);
                    }
                } else {
                    dismiss();
                }
                break;
                
            case R.id.btn_my_position:
                mActionLog.addAction(mActionTag + ActionLog.TrafficInputMyPosition);
                CityInfo c = Globals.g_My_Location_City_Info;
                if (c != null && c.getPosition() != null) {
                    POI p = new POI();
                    p.setPosition(c.getPosition());
                    if (mRequest == REQUEST_COMMON_PLACE) {
                        p.setName(MapEngine.getInstance().getPositionName(c.getPosition()));
                    } else {
                        p.setName(getString(R.string.my_location));
                    }
                    responsePOI(p);
                    dismiss();
                } else {
                    mSphinx.showTip(R.string.location_failed, Toast.LENGTH_SHORT);
                }
                break;
                
            case R.id.btn_fav_position:
                mActionLog.addAction(mActionTag + ActionLog.TrafficInputFavPosition);
                mSphinx.showView(mSphinx.getFetchFavoriteFragment().getId());
                break;
            default:
        }
    }
    
    private void submit(String keyword) {
        switch (mCurMode) {
            case MODE_BUELINE:
                submitBuslineQuery(keyword);
                break;
            case MODE_TRAFFIC:

                if (mRequest == REQUEST_TRAFFIC_START || mRequest == REQUEST_TRAFFIC_END) {
                    POI poi = new POI();
                    poi.setName(keyword);
                    responsePOI(poi);
                } else if (mRequest == REQUEST_COMMON_PLACE) {
                    submitGeoCoderQuery(keyword);
                }
                break;
            case MODE_POI:
                submitPOIQuery(keyword);
                break;
            default:
                break;
        }
    }
    
    private void submit(TKWord tkWord) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
            return;
        }
        
        switch (mCurMode) {
            case MODE_BUELINE:
                submitBuslineQuery(tkWord.word);
                break;
            case MODE_TRAFFIC:

                if (mRequest == REQUEST_TRAFFIC_START || mRequest == REQUEST_TRAFFIC_END) {
                    POI poi = tkWord.toPOI();
                    responsePOI(poi);
                } else if (mRequest == REQUEST_COMMON_PLACE) {
                    if (tkWord.position != null) {
                        POI poi = tkWord.toPOI();
                        responsePOI(poi);
                    } else {
                        submitGeoCoderQuery(tkWord.word);
                    }
                }
                break;
            case MODE_POI:
                submitPOIQuery(tkWord.word);
                break;
            default:
                break;
        }
    }
    
    /**
     * 返回POI，目前仅用于交通模式
     * @param poi
     */
    public void responsePOI(POI poi) {
        if (poi == null || TextUtils.isEmpty(poi.getName())) {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }
        
        if (mCurMode == MODE_TRAFFIC) {
            mSphinx.getTrafficQueryFragment().addHistoryWord(poi, HistoryWordTable.TYPE_TRAFFIC);

            if (mRequest == REQUEST_TRAFFIC_START || mRequest == REQUEST_TRAFFIC_END) {
                mIResponsePOI.responsePOI(poi);
                dismiss();
            } else if (mRequest == REQUEST_COMMON_PLACE) {
                if (poi.getPosition() != null) {
                    mIResponsePOI.responsePOI(poi);
                    mSphinx.showTip(R.string.setting_success, Toast.LENGTH_SHORT);
                    dismiss();
                } else {
                    submitGeoCoderQuery(poi.getName());
                }
            }
        }
    }
    
    /**
     * POI搜索
     * @param keyword
     */
    public void submitPOIQuery(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }
        
        HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), HistoryWordTable.TYPE_POI);

        DataQuery dataQuery = new DataQuery(mDataQuery);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_POI);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_EXT, DataQuery.EXT_BUSLINE);
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(dataQuery);
    }
    
    /**
     * 公交线路搜索
     * @param keyword
     */
    private void submitBuslineQuery(String keyword) {
        if (TextUtils.isEmpty(keyword)){
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }

        HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), HistoryWordTable.TYPE_BUSLINE);
        
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(keyword, 0, false, getId(), getString(R.string.doing_and_wait));

        mSphinx.queryStart(buslineQuery);
    }
    
    /**
     * 反向定位POI
     * @param keyword
     */
    private void submitGeoCoderQuery(String keyword) {
        if (TextUtils.isEmpty(keyword)){
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }

        HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), HistoryWordTable.TYPE_TRAFFIC);

        DataQuery dataQuery = new DataQuery(mDataQuery);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_GEOCODER);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD_SIMPLE);
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(dataQuery);
    }
    
    public void setData() {
    	setData(mSphinx.buildDataQuery(), null, MODE_POI);
    }
    
    public void setData(DataQuery dataQuery, String text, int mode) {
        setData(dataQuery, text, mode, null, REQUEST_NONE);
    }
    
    public void setData(DataQuery dataQuery, String text, int mode, IResponsePOI iResponsePOI, int request) {
        mKeywordEdt.setText(text);

        mDataQuery = dataQuery;
        mCurMode = mode;
        mCurHisWordType = mode;
        mIResponsePOI = iResponsePOI;
        mRequest = request;
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        String apiType = baseQuery.getAPIType();
        if (BaseQuery.API_TYPE_BUSLINE_QUERY.equals(apiType)) {
            dealWithBuslineResponse(mSphinx, (BuslineQuery)baseQuery, mActionTag, null);
        } else if (BaseQuery.API_TYPE_DATA_QUERY.equals(apiType)) {
            
            String dataType = baseQuery.getParameter(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
            if (BaseQuery.DATA_TYPE_GEOCODER.equals(dataType)) {
                
                if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
                    isReLogin = true;
                } else if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                    return;
                }
                
                DataQuery dataQuery = (DataQuery) baseQuery;
                Response response = dataQuery.getResponse();
                if (response != null
                        && response instanceof GeoCoderResponse
                        && response.getResponseCode() == Response.RESPONSE_CODE_OK) {
                    GeoCoderResponse geoCoderResponse = (GeoCoderResponse) response;
                    GeoCoderList geoCoderList = geoCoderResponse.getList();
                    if (geoCoderList != null) {
                        List<POI> list = geoCoderList.getPOIList();
                        if (list != null && list.size() > 0) {
                            showPlaceListDialog(list);
                            return;
                        }
                    }
                }
                
                Toast.makeText(mSphinx, getString(R.string.no_result), Toast.LENGTH_LONG).show();
                
            } else if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                int result = dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);
                if(result > 0){
                	mSphinx.getHandler().sendEmptyMessage(Sphinx.UI_STACK_ADJUST_EXECUTE);
                }
            }
        }
    }

    /**
     * 处理POI搜索结果，实现界面跳转和给出相应提示
     * @param sphinx
     * @param poiResponse
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
        
        if (BaseActivity.checkResponseCode(dataQuery, sphinx, null, true, baseFragment, false)) {
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
                sphinx.showView(R.id.view_traffic_busline_line_result);
                sphinx.uiStackRemove(R.id.view_poi_input_search);
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
                DataQuery newDataQuery = new DataQuery(dataQuery);
                Position position = cityInfo.getPosition();
                newDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_CENTER_LONGITUDE, String.valueOf(position.getLon()));
                newDataQuery.addParameter(BaseQuery.SERVER_PARAMETER_CENTER_LATITUDE, String.valueOf(position.getLat()));
                newDataQuery.setCityId(cityId);
                sphinx.queryStart(newDataQuery);
                result = POIResponse.FIELD_CITY_ID_AND_RESULT_TOTAL;
                return result;
                
            // 若存在多个城市及结果时，提示城市列表，用户选择后以城市中心位置发起搜索，不移动地图
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
        
        // 跳转到POI结果地图界面
        } else if (bPOIList.getShowType() == 1) {
            if (bPOIList == null || bPOIList.getList() == null || bPOIList.getList().isEmpty()) {
                Toast.makeText(sphinx, sphinx.getString(R.string.no_result), Toast.LENGTH_LONG).show();
                return 2;
            } else {
                poiResultFragment.setData(dataQuery, true);

                sphinx.uiStackRemove(R.id.view_poi_input_search);
                ItemizedOverlayHelper.drawPOIOverlay(sphinx, bPOIList.getList(), 0, poiResultFragment.getAPOI());
                sphinx.getResultMapFragment().setData(sphinx.getString(R.string.result_map), ActionLog.POIListMap);
                sphinx.showView(R.id.view_result_map);
            }
        }
        
        // 若从公交线路结果列表界面跳转过来，则将其从UI堆栈中移除
        sphinx.uiStackRemove(R.id.view_traffic_busline_line_result);
        
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
                    sphinx.showView(R.id.view_traffic_busline_line_result);
                } else if (buslineModel.getType() == BuslineModel.TYPE_STATION) {
                    actionLog.addAction(actionTag + ActionLog.TrafficResultBusline, buslineQuery.getBuslineModel().getStationList().size());
                    sphinx.getBuslineResultStationFragment().setData(buslineQuery);
                    sphinx.showView(R.id.view_traffic_busline_station_result);
                }               
            }
        }
    }

    /**
     * 反向定位POI的Apapter
     * @author pengwenwue
     *
     */
    private class PlaceAdapter extends BaseAdapter {
        List<POI> mList;

        public PlaceAdapter(List<POI> list) {
            mList = list;
        }
        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.traffic_alternative_station_item, null);
            }
            
            POI poi = mList.get(position);
            
            TextView name = (TextView) convertView.findViewById(R.id.name_txv);
            TextView address = (TextView) convertView.findViewById(R.id.address_txv);
            
            name.setText(poi.getName());
            address.setText(poi.getAddress());
            
            return convertView;
        }
        
    }
    
    /**
     * 显示反向定位POI结果列表对话框
     * @param list
     */
    private void showPlaceListDialog(final List<POI> list) {
        
        PlaceAdapter adapter = new PlaceAdapter(list);
        View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
        ListView listView = (ListView) alterListView.findViewById(R.id.listview);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
        
        TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
        titleTxv.setText(R.string.confirm_place);
        
        Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
        button.setVisibility(View.GONE);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                
                dialog.dismiss();
                responsePOI(list.get(position));
            }
            
        });
        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                mActionLog.addAction(ActionLog.TrafficAlternative + ActionLog.Dismiss);
            }
        });
        
        dialog.show();
    }
}

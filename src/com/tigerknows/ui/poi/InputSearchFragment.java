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
import com.tigerknows.model.DataQuery.DiscoverCategoreResponse;
import com.tigerknows.model.DataQuery.GeoCoderResponse;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.BuslineResultLineFragment;
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
    final public static int MODE_BUSLINE = HistoryWordTable.TYPE_BUSLINE;
    
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
    private LinearLayout mPoiBtnGroup;
    private LinearLayout mTrafficBtnGroup;
    private View mMapSelectPointBtn;
    private Button mFavBtn;
    private Button mMyPosBtn;
    private Button mFoodBtn;
    private Button mTuangouBtn;
    private Button mHotelBtn;
    private Button mBusStationBtn;
    private Button mMoreBtn;
    
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
                mSphinx.hideSoftInput();
                String keyword = mKeywordEdt.getText().toString().trim();
                if (mTKWord != null && keyword.equals(mTKWord.word)) {
                    submit(mTKWord);
                } else {
                    TKWord tkWord = new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword);
                    submit(tkWord);
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
        mActionTag = ActionLog.InputQuery;
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
        case MODE_BUSLINE:
        	mPoiBtnGroup.setVisibility(View.GONE);
            mTrafficBtnGroup.setVisibility(View.GONE);
            mKeywordEdt.setHint(getString(R.string.busline_search_hint));
            break;
        case MODE_TRAFFIC:
        	mPoiBtnGroup.setVisibility(View.GONE);
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
        	mPoiBtnGroup.setVisibility(View.VISIBLE);
            mTrafficBtnGroup.setVisibility(View.GONE);
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
        mPoiBtnGroup = (LinearLayout)mRootView.findViewById(R.id.poi_btn_group);
        mTrafficBtnGroup = (LinearLayout) mRootView.findViewById(R.id.traffic_btn_group);
        mMapSelectPointBtn = mTrafficBtnGroup.findViewById(R.id.btn_map_position);
        mFavBtn = (Button) mRootView.findViewById(R.id.btn_fav_position);
        mMyPosBtn = (Button) mRootView.findViewById(R.id.btn_my_position);
        mFoodBtn = (Button) mRootView.findViewById(R.id.btn_food);
        mTuangouBtn = (Button) mRootView.findViewById(R.id.btn_tuangou);
        mHotelBtn = (Button) mRootView.findViewById(R.id.btn_hotel);
        mBusStationBtn = (Button) mRootView.findViewById(R.id.btn_bus_station);
        mMoreBtn = (Button) mRootView.findViewById(R.id.btn_more);
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
        
        mFoodBtn.setOnClickListener(this);
        mTuangouBtn.setOnClickListener(this);
        mHotelBtn.setOnClickListener(this);
        mBusStationBtn.setOnClickListener(this);
        mMoreBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            
            case R.id.btn_map_position:
                mActionLog.addAction(ActionLog.InputQueryMapPosition);
                
                String title;
                if (mRequest == REQUEST_TRAFFIC_START) {
                    title = getString(R.string.select_start_station);
                } else if (mRequest == REQUEST_TRAFFIC_END) {
                    title = getString(R.string.select_end_station);
                } else {
                    title = getString(R.string.confirm_place);
                }
                
                Toast.makeText(mSphinx, R.string.move_map_select_point, Toast.LENGTH_LONG).show();
                mSphinx.getResultMapFragment().setData(getString(R.string.map_select_point), ActionLog.TrafficSelectPoint);
                mSphinx.showView(R.id.view_result_map);
                
                ItemizedOverlayHelper.drawClickSelectPointOverlay(mSphinx, title);
                
                break;
                
            case R.id.right_btn:
                String keyword = mKeywordEdt.getText().toString().trim();
                mActionLog.addAction(mActionTag + ActionLog.TitleRightButton, keyword.length() > 0, keyword);
                if (keyword.length() > 0) {
                    if (mTKWord != null && keyword.equals(mTKWord.word)) {
                        submit(mTKWord);
                    } else {
                        TKWord tkWord = new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword);
                        submit(tkWord);
                    }
                } else {
                    dismiss();
                }
                break;
                
            case R.id.btn_my_position:
                mActionLog.addAction(ActionLog.InputQueryMyPosition);
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
                mActionLog.addAction(ActionLog.InputQueryFavPosition);
                mSphinx.showView(mSphinx.getFetchFavoriteFragment().getId());
                break;
            case R.id.btn_food:
            	// TODO: actionLog
            	submitPOIQuery(new TKWord(TKWord.ATTRIBUTE_HISTORY, mSphinx.getString(R.string.cate)));
            	break;
            case R.id.btn_tuangou:
                if (DataQuery.checkDiscoveryCity(mDataQuery.getCityId(), Long.parseLong(BaseQuery.DATA_TYPE_TUANGOU)) == false) {
                    Toast.makeText(mSphinx, R.string.this_city_not_support_tuangou, Toast.LENGTH_LONG).show();
                    return;
                }
                mSphinx.queryStart(getDiscoverDataQuery(BaseQuery.DATA_TYPE_TUANGOU));
                break;
            case R.id.btn_hotel:
        		mSphinx.getHotelHomeFragment().resetDate();
        		mSphinx.getHotelHomeFragment().setCityInfo(Globals.getCurrentCityInfo(mContext));
        		mSphinx.showView(R.id.view_hotel_home);
        		break;
            case R.id.btn_bus_station:
            	submitPOIQuery(new TKWord(TKWord.ATTRIBUTE_HISTORY, mSphinx.getString(R.string.bus_station)));
            	break;
            case R.id.btn_more:
            	break;
            default:
        }
    }
    
    private void submit(TKWord tkWord) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
            return;
        }
        
        switch (mCurMode) {
            case MODE_BUSLINE:
                submitBuslineQuery(tkWord);
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
                        submitGeoCoderQuery(tkWord);
                    }
                }
                break;
            case MODE_POI:
                submitPOIQuery(tkWord);
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

            if (mRequest == REQUEST_TRAFFIC_START || mRequest == REQUEST_TRAFFIC_END) {
                mIResponsePOI.responsePOI(poi);
                dismiss();
            } else if (mRequest == REQUEST_COMMON_PLACE) {
                if (poi.getPosition() != null) {
                    mIResponsePOI.responsePOI(poi);
                    mSphinx.showTip(R.string.setting_success, Toast.LENGTH_SHORT);
                    dismiss();
                } else {
                    TKWord tkWord = new TKWord(TKWord.ATTRIBUTE_CLEANUP, poi.getName(), poi.getPosition(), poi.getAddress());
                    submitGeoCoderQuery(tkWord);
                }
            }
        }
    }
    
    /**
     * POI搜索
     * @param keyword
     */
    public void submitPOIQuery(TKWord tkWord) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }

        HistoryWordTable.addHistoryWord(mSphinx, tkWord, HistoryWordTable.TYPE_POI);
        DataQuery dataQuery = new DataQuery(mDataQuery);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, tkWord.word);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_POI);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_EXT, DataQuery.EXT_BUSLINE);
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(dataQuery);
    }
    
    /**
     * 发现频道(团购)搜索
     * @param String dataType
     */
    private DataQuery getDiscoverDataQuery(String dataType) {
        DataQuery dataQuery = new DataQuery(mDataQuery);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, dataType);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.delParameter(DataQuery.SERVER_PARAMETER_POI_ID);
        dataQuery.setup(getId(), getId(), getString(R.string.doing_and_wait));
        return dataQuery;
    }
    
    /**
     * 公交线路搜索
     * @param keyword
     */
    private void submitBuslineQuery(TKWord tkWord) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)){
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }

        HistoryWordTable.addHistoryWord(mSphinx, tkWord, HistoryWordTable.TYPE_BUSLINE);
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(tkWord.word, 0, false, getId(), getString(R.string.doing_and_wait));

        mSphinx.queryStart(buslineQuery);
    }
    
    /**
     * 反向定位POI
     * @param keyword
     */
    private void submitGeoCoderQuery(TKWord tkWord) {
        if (tkWord == null || TextUtils.isEmpty(tkWord.word)) {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        }

        HistoryWordTable.addHistoryWord(mSphinx, tkWord, HistoryWordTable.TYPE_TRAFFIC);
        DataQuery dataQuery = new DataQuery(mDataQuery);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, tkWord.word);
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
        if(MODE_POI == mode){
        	refreshDiscoverCities();
        }
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
                } else if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_DIALOG, this, false)) {
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
                
                Toast.makeText(mSphinx, getString(R.string.can_not_found_target_location), Toast.LENGTH_LONG).show();
                
            } else if (BaseQuery.DATA_TYPE_POI.equals(dataType)) {
                dealWithPOIResponse((DataQuery) baseQuery, mSphinx, this);

            } else if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
            	dealWithDynamicPOIResponse((DataQuery) baseQuery, mSphinx, this);
            } else if (BaseQuery.DATA_TYPE_FILTER.equals(dataType)) {
            	//TODO: 
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

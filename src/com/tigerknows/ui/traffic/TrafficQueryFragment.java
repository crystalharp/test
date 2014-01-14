package com.tigerknows.ui.traffic;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.TrafficOverlayHelper;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Station;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.CommonPlaceTable.CommonPlace;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.provider.TrafficSearchHistoryTable;
import com.tigerknows.provider.TrafficSearchHistoryTable.SearchHistory;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
import com.tigerknows.ui.traffic.TrafficCommonPlaceFragment.CommonPlaceList;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.LinearListAdapter;
import com.tigerknows.widget.StringArrayAdapter;

/**
 * 此类包含 交通首页、交通输入页、交通全屏页、交通选点页，合称“交通频道首页”
 * 因为界面相似，且若分开会导致十分复杂的参数传递，所以写成一个类
 * 
 * 主要功能包括：
 * 1. 控制状态转换(界面转换)
 * 2. 动画效果
 * 3. 控件事件处理
 * 4. 行为日志处理
 * 5. 控制显示地图中心及查询地图中心
 * 6. 控制历史词及联想词
 * 
 * @author linqingzu
 *
 */
public class TrafficQueryFragment extends BaseFragment implements View.OnClickListener{

	public static final int START = 1;
	
	public static final int END = 2;
	
	public static final int LINE = 3;
	
	public static final int MAX_QUERY_HISTORY = 10;
	
	public String MY_LOCATION;
	
	public String MAP_LOCATION;
	
	private int mSettedRadioBtn = 0;
	
	boolean showStartMyLocation = true;
	
	private boolean autoStartQuery = false;
	
//	private boolean useEndCityId = false;

	FrameLayout mTitle;
	
	Button mBackBtn;
	
	Button mTrafficSwitchBtn;
	
	RadioGroup mRadioGroup = null;
	
	LinearLayout mBlock;
	
	Button mSelectStartBtn = null;
	
	Button mSelectEndBtn = null;

    private RadioButton mTrafficTransferRbt;
    private RadioButton mTrafficDriveRbt;
    private RadioButton mTrafficWalkRbt;
    
	/**
	 * 注：因为起终点对应的poi直接setTag给了按钮，所以不要直接给这两个按钮直接赋值，
	 * 而是要使用setPOI，getPOI，getStartEndText函数
	 */
	private InputBtn mStart;
    
	private InputBtn mEnd;
	
//	LinearLayout mAddCommonPlace;
	
	LinearLayout mCommonPlaceLst;
	
	ScrollView mScrollView;
	
	View mQueryHistory;
	
	LinearLayout mQueryHistoryLst;
	
	View mTitleBar;
	
	public static ViewGroup.LayoutParams sLayoutParams = new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	
	LinearListAdapter mQueryHistoryAdapter;
	LinearListAdapter mCommonPlaceAdapter;
            
    CommonPlaceList mCommonPlaces = new CommonPlaceList(mSphinx);

    List<SearchHistory> mQueryHistorys = new LinkedList<SearchHistory>();
    List<SearchHistory> mShowingQueryHistorys = new LinkedList<SearchHistory>();
    
    TrafficSearchHistoryTable mHistoryTable = new TrafficSearchHistoryTable(mSphinx);
    
    RadioGroup.OnCheckedChangeListener mRadioCheckedChangedListener = new RadioGroup.OnCheckedChangeListener() {
        
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
            case R.id.traffic_transfer_rbt:
                mActionLog.addAction(mActionTag + ActionLog.TrafficTransferTab);
                break;
            case R.id.traffic_drive_rbt:
                mActionLog.addAction(mActionTag + ActionLog.TrafficDriveTab);
                break;
            case R.id.traffic_walk_rbt:
                mActionLog.addAction(mActionTag + ActionLog.TrafficWalkTab);
                break;
            }
        }
    };
    
//	int oldCheckButton;
    List<String> keywordList;
	
	private boolean isKeyword(String input) {
	    return keywordList.contains(input);
	}

	public static final String TAG = "TrafficQueryFragment";
	
	final class InputBtn {
	    Button btn;
	    POI poi;
	    String type;
	    
	    public InputBtn(Button b) {
	        btn = b;
	        poi = null;
	    }
	    
	    public final String getText() {
	        return btn.getText().toString().trim();
	    }
	    
	    public final boolean textEmpty() {
	        return TextUtils.isEmpty(btn.getText().toString().trim());
	    }
	    
	    public final void setPOI(POI p) {
	        if (p != null && !TextUtils.isEmpty(p.getName())) {
	            btn.setText(p.getName());
	        } else {
	            btn.setText(null);
	        }
	        if (MY_LOCATION.equals(p.getName())) {
	            btn.setTextColor(getResources().getColor(R.color.blue));
	        } else {
	            btn.setTextColor(Color.BLACK);
	        }
	        poi = p;
	        refreshRightBtn();
	    }
	    
	    public POI getPOI() {
	        
	        if (TextUtils.isEmpty(btn.getText())) {
	            poi = new POI();
	        }

	        return poi;
	    }
	    
	    public POI getClonedPOI() {
	        if (TextUtils.isEmpty(btn.getText())) {
	            return new POI();
	        }
	        return poi.clone();
	    }
	    
	    public final void setHint(String s) {
	        btn.setHint(s);
	    }
	    
	    public final void clear() {
	        btn.setText(null);
	        poi = null;
	    }
	    
	    public void setOnClickListener(View.OnClickListener l) {
	        btn.setOnClickListener(l);
	    }
	}
	/**
	 * 是否优先使用当前位置
	 * 当从截图、测距、指南针界面返回时，此时地图中心点所在城市与之前在交通界面的地图所在城市一致则不移动地图
	 */
	public boolean priorityMyLocation = true;
	
	public TrafficQueryFragment(Sphinx sphinx) {
        super(sphinx);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficHomeNormal;
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_query, container, false);

        findViews();
        MY_LOCATION = getString(R.string.my_location);
        MAP_LOCATION = getString(R.string.map_point);
        
        mStart.setHint(getString(R.string.start_));
        mEnd.setHint(getString(R.string.end_));
        
        String[] KEYWORDS = new String[]{
                MY_LOCATION,
                MAP_LOCATION,
        };
        keywordList = Arrays.asList(KEYWORDS);
		
        return mRootView;
    }
	
	@Override
    protected void findViews() {
    	super.findViews();
        mBlock = (LinearLayout)mRootView.findViewById(R.id.content_lnl);
		
    	mBackBtn = (Button)mRootView.findViewById(R.id.back_btn);
    	mTrafficSwitchBtn = (Button)mRootView.findViewById(R.id.traffic_switch_btn);
        mTitleBar = mLayoutInflater.inflate(R.layout.traffic_query_bar, null);
    	mRadioGroup = (RadioGroup)mTitleBar.findViewById(R.id.traffic_rgp);
    	mScrollView = (ScrollView) mRootView.findViewById(R.id.traffic_scroll);
    	
    	Button startBtn, endBtn;
    	startBtn = (Button)mRootView.findViewById(R.id.start_btn);
    	endBtn = (Button)mRootView.findViewById(R.id.end_btn);
    	mStart = new InputBtn(startBtn);
    	mEnd = new InputBtn(endBtn);
    	
    	mQueryHistory = mRootView.findViewById(R.id.query_history_title);
    	mQueryHistoryLst = (LinearLayout)mRootView.findViewById(R.id.query_history_lst);
//    	mAddCommonPlace = (LinearLayout)mRootView.findViewById(R.id.add_common_place);
    	mCommonPlaceLst = (LinearLayout)mRootView.findViewById(R.id.common_place_lsv);
		
    	setListener();
    }
	
    @Override
	protected void setListener() {
	    super.setListener();
	    mStart.setOnClickListener(this);
	    mEnd.setOnClickListener(this);
//	    mAddCommonPlace.setOnClickListener(this);
        mQueryHistory.setOnClickListener(this);
        mTrafficSwitchBtn.setOnClickListener(this);
	//---------------------history start-----------------------//
	    final OnClickListener historyItemListener = new View.OnClickListener() {

	        @Override
	        public void onClick(View v) {
	            SearchHistory h = (SearchHistory) v.getTag();
	            mActionLog.addAction(mActionTag + ActionLog.TrafficHomeHistoryItem, mQueryHistorys.indexOf(h));
	            mStart.setPOI(h.start);
	            mEnd.setPOI(h.end);
	            updateSearchHistory(h);
	            submitTrafficQuery(h.start.clone(), h.end.clone());
	        }
	    };
	    mQueryHistoryAdapter = new LinearListAdapter(mSphinx, mQueryHistoryLst, R.layout.traffic_transfer_his_item) {

	        @Override
	        public View getView(Object data, View child, int pos) {
	            SearchHistory h = (SearchHistory)data;
	            TextView t = (TextView) child.findViewById(R.id.his_txv);
	            child.setTag(data);
	            if (pos == mQueryHistorys.size() - 1) {
	                child.setBackgroundResource(R.drawable.list_footer);
	            } else {
	                child.setBackgroundResource(R.drawable.list_middle);
	            }
	            child.setPadding(Utility.dip2px(mSphinx, 12), 0, 0, 0);
	            t.setText(h.genDescription());
	            child.setOnClickListener(historyItemListener);
	            return null;
	        }
	    };

    //----------------------history end------------------------//
	    
	    final OnClickListener placeItemListener = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                CommonPlace c = (CommonPlace) v.getTag();
                mActionLog.addAction(mActionTag + ActionLog.TrafficHomeClickCommonPlace);
                if (c.isEmptyFixedPlace()) {
                    DataQuery dataQuery = new DataQuery(mSphinx);
                    dataQuery.setCityId(getCityId(mStart.getPOI(), mEnd.getPOI()));
                    mSphinx.getInputSearchFragment().setData(dataQuery,
                    		null,
                            InputSearchFragment.MODE_TRAFFIC,
                            new InputSearchFragment.IResponsePOI(){

                                @Override
                                public void responsePOI(POI p) {
                                    mCommonPlaces.setPOI(0, p);
                                    mCommonPlaceAdapter.refreshList(mCommonPlaces.getList());
                                }
                            },
                            InputSearchFragment.REQUEST_COMMON_PLACE);
                    mSphinx.showView(R.id.view_poi_input_search);
                } else {
                    mEnd.setPOI(c.poi);
                    if (!mStart.textEmpty()) {
                        query();
                    }
                }
            }
        };

	    mCommonPlaceAdapter = new LinearListAdapter(mSphinx, mCommonPlaceLst, R.layout.traffic_common_places_item) {

	        @Override
	        public View getView(Object data, View child, int pos) {
	            TextView aliasTxv = (TextView) child.findViewById(R.id.alias_txv);
	            TextView nameTxv = (TextView) child.findViewById(R.id.name_txv);
	            child.setTag(data);
	            child.setOnClickListener(placeItemListener);
	            CommonPlace c = (CommonPlace) data;
	            aliasTxv.setText(c.alias);
	            child.setBackgroundResource(R.drawable.list_single);
	            if (c.isEmptyFixedPlace()) {
	                nameTxv.setText(R.string.click_set_place);
	            } else {
	                nameTxv.setText(c.poi.getName());
	            }
	            return null;
	        }
	    };
	}
    /**
     * 退出该页面时, 发生于从交通输入页返回POI详情页时
     */
    @Override
    public void dismiss() {
    	super.dismiss();
    	clearAllText();
    	showStartMyLocation = true; 
//    	useEndCityId = false;
    	
    }
    
    public void onPause() {
        if (this.isShowing()) {
            mRadioGroup.setOnCheckedChangeListener(null);
        }
    	mSettedRadioBtn = 0;
    	autoStartQuery = false;
    	super.onPause();
    }
    
	@Override
	public void onResume() {
	    super.onResume();
         
        mRightBtn.setVisibility(View.VISIBLE);
	    mRightBtn.setOnClickListener(this);
	    /*
         * 由于在一个“session”中会多次调用onresume，导致在地图选点和收藏夹选点之后返回本页面都会调用initstart
         * 这里引入mDissmissed变量，在整个页面被dismiss的时候设为true，onresume的时候把变量设为false，并把所有
         * 应该初始化的内容初始化。这个概念以后应该被扩展到其他的页面。
         */
        if (mDismissed) {
            mDismissed = false;
            initStartContent();
        }
        
        mTitleBtn.setVisibility(View.GONE);
        ((ViewGroup)mSphinx.getTitleFragment().mRootView).addView(mTitleBar, sLayoutParams);
    	mRadioGroup.setOnCheckedChangeListener(mRadioCheckedChangedListener);
    	mTrafficTransferRbt = (RadioButton) mRadioGroup.findViewById(R.id.traffic_transfer_rbt);
    	mTrafficDriveRbt = (RadioButton) mRadioGroup.findViewById(R.id.traffic_drive_rbt);
    	mTrafficWalkRbt = (RadioButton) mRadioGroup.findViewById(R.id.traffic_walk_rbt);
    	mTrafficTransferRbt.setOnTouchListener(null);
    	mTrafficDriveRbt.setOnTouchListener(null);
    	mTrafficWalkRbt.setOnTouchListener(null);
        updateCommonPlace();
        initHistory();
        refreshRightBtn();
        mScrollView.scrollTo(0, 0);
        if (autoStartQuery && !mStart.textEmpty() && !mEnd.textEmpty()) {
            query();
        }
	}
    	
	void refreshRightBtn() {
	    if (!mStart.textEmpty() 
                && !mEnd.textEmpty()) {
            mRightBtn.setBackgroundResource(R.drawable.btn_confirm);
            mRightBtn.setText(R.string.search);
        } else {
            mRightBtn.setText("");
            mRightBtn.setBackgroundResource(R.drawable.btn_traffic_more_type);
            mRightBtn.setPadding(0, 0, 0, 0);
        }
	}
	
//	public void useEndCityId(boolean b) {
//	    useEndCityId = b;
//	}
	
	public void setShowStartMyLocation(boolean showMyLocation) {
	    this.showStartMyLocation = showMyLocation;
	}
	
	public final void autoStartQuery(boolean b) {
	    autoStartQuery = b;
	}
	
	public void initStartContent() {

	    if (showStartMyLocation) {
	        //如果要求显示起点，在起点框填写上当前位置。
	        if (mStart.textEmpty()) {
	            POI poi = getMylocationPOI();
	            if (poi != null) {
	                mStart.setPOI(poi);
	            }
	        }
	        //否则什么都不做，不影响当前起点框内容
	    } else {
	        //否则清空起点框的内容。
	        mStart.clear();
	    }
		
    }
	
	POI getMylocationPOI() {
	    POI p = new POI();
	    p.setName(MY_LOCATION);
	    return p;
	}
	
	void switchStartEnd() {
	    POI start, end;
	    start = mStart.getPOI();
	    end = mEnd.getPOI();
	    mStart.setPOI(end);
	    mEnd.setPOI(start);
	}

	public void clearAllText() {
		mStart.clear();
		mEnd.clear();
	}
		
	public final void setStart(POI p) {
	    mStart.setPOI(p);
	}
	
	public final void setEnd(POI p) {
	    mEnd.setPOI(p);
	}
	
	public final void query() {
	    POI start = mStart.getClonedPOI();
        POI end = mEnd.getClonedPOI();
        
        if (start == null || end == null)
            return;
        
        if (checkIsInputEmpty())
            return;

        if (checkIsInputEqual()) {
            return;
        }
        
        addSearchHistory(new SearchHistory(start.clone(), end.clone()));
        
        submitTrafficQuery(start, end);

	}

	public void submitTrafficQuery(POI start, POI end) {
        
        addHistoryWord(start, HistoryWordTable.TYPE_TRAFFIC);
        addHistoryWord(end, HistoryWordTable.TYPE_TRAFFIC);
        mActionLog.addAction(mActionTag +  ActionLog.TrafficTrafficSearch, getQueryType(), mStart.getText(), mEnd.getText());

        if (!processMyLocation(start)) {
            return;
        }
        if (!processMyLocation(end)) {
            return;
        }
        
        TrafficQuery trafficQuery = new TrafficQuery(mContext);
    		
        trafficQuery.setup(start, end, getQueryType(), getId(), getString(R.string.doing_and_wait));
        trafficQuery.setCityId(getCityId(start, end));
        
        mSphinx.queryStart(trafficQuery);
	}
	
	/**
	 * 目前确定本页面的请求都在地图城市进行查询
	 * @param start
	 * @param end
	 * @return
	 */
	private int getCityId(POI start, POI end) {
	    int cityId = Globals.getCurrentCityInfo(mSphinx).getId();
//	    if (useEndCityId) {
//	        if (end != null && end.getPosition() != null) {
//	            cityId = MapEngine.getCityId(end.getPosition());
//	        }
//	    }
//        if (start != null && start.getPosition() != null) {
//            cityId = MapEngine.getCityId(start.getPosition());
//        }
//        if (end != null && end.getPosition() != null) {
//            cityId = MapEngine.getCityId(end.getPosition());
//        }
        return cityId;
	}
    
    public void addHistoryWord(POI poi, int type) {
        if (poi == null) {
            return;
        }
        String name = poi.getName();
        if (name != null && !isKeyword(name.trim())) {
            Position position = poi.getPosition();
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, name, position), type);
        }
    }
    
    //用来处理新生成的历史
    private void addSearchHistory(SearchHistory sh) {
        if (mQueryHistorys.contains(sh)) {
            //各种各样看起来名字一样的历史需要先删除掉
            int index = mQueryHistorys.indexOf(sh);
            SearchHistory oldsh = mQueryHistorys.get(index);
            mQueryHistorys.remove(oldsh);
            mHistoryTable.delete(oldsh);
        }
        mQueryHistorys.add(0, sh);
        mHistoryTable.add(sh);
        mShowingQueryHistorys = mQueryHistorys.subList(0, Math.min(mQueryHistorys.size(), MAX_QUERY_HISTORY));

        if (mShowingQueryHistorys.size() == 1) {
            mQueryHistory.setVisibility(View.VISIBLE);
            mQueryHistoryLst.setVisibility(View.VISIBLE);
        }
        mQueryHistoryAdapter.refreshList(mShowingQueryHistorys);
    }
    
    //只用来处理有id的搜索历史
    private void updateSearchHistory(SearchHistory sh) {
        mQueryHistorys.remove(sh);
        mQueryHistorys.add(0, sh);
        mHistoryTable.update(sh);
        mShowingQueryHistorys = mQueryHistorys.subList(0, Math.min(mQueryHistorys.size(), MAX_QUERY_HISTORY));
        mQueryHistoryAdapter.refreshList(mShowingQueryHistorys);
    }
    
    private void delSearchHistory(SearchHistory sh) {
        mQueryHistorys.remove(sh);
        mHistoryTable.delete(sh);
        mQueryHistoryAdapter.refreshList(mQueryHistorys);
    }
    
    public void initHistory() {
        mQueryHistorys.clear();
        mHistoryTable.readTrafficSearchHistory(mQueryHistorys);
        mQueryHistory.setVisibility(View.VISIBLE);
        mQueryHistoryLst.setVisibility(View.VISIBLE);
        if (mQueryHistorys.size() == 0) {
            mQueryHistory.setVisibility(View.GONE);
            mQueryHistoryLst.setVisibility(View.GONE);
        }
        mShowingQueryHistorys = mQueryHistorys.subList(0, Math.min(mQueryHistorys.size(), MAX_QUERY_HISTORY));
        mQueryHistoryAdapter.refreshList(mShowingQueryHistorys);
    }
    
	public static String getMyLocationName(Sphinx mSphinx, Position position) {
		String mLocationName = mSphinx.getMapEngine().getPositionName(position);
		return mLocationName;
	}
	
	private boolean checkIsInputEmpty() {
		
		String start = mStart.getText();
		String end = mEnd.getText();
		
        if (TextUtils.isEmpty(start)) {
            mSphinx.showTip(R.string.forget_start_tip, Toast.LENGTH_SHORT);
            return true;
        } else if (TextUtils.isEmpty(end)) {
            mSphinx.showTip(R.string.forget_end_tip, Toast.LENGTH_SHORT);
            return true;
        }
		
		return false;
	}
	
	private boolean checkIsInputEqual() {
		/*
		 * 对于"选定的位置"
		 * 若坐标点不同, 则认为二者不同, 
		 * 若坐标点相同, 则认为二者相同.
		 */
	    POI start, end;
	    start = mStart.getPOI();
	    end = mEnd.getPOI();
		if (MAP_LOCATION.equals(mStart.getText())
				&& MAP_LOCATION.equals(mEnd.getText())) {
			if (POI.isPositionEqual(start, end)) {
				mSphinx.showTip(R.string.start_equal_to_end, Toast.LENGTH_SHORT);
				return true;
			} else {
				return false;
			}
 		}
		
		/*
		 * 对于其余的输入
		 * 若名字相同, 则认为二者相同
		 */
		if (POI.isNameEqual(start, end)) {
			mSphinx.showTip(R.string.start_equal_to_end, Toast.LENGTH_SHORT);
			return true;
		} 
		
		return false;
	}
	
	private boolean processMyLocation(POI p) {
	    if (p.getName().equals(MY_LOCATION)) {
	        p.setSourceType(POI.SOURCE_TYPE_MY_LOCATION);
            if (p.getPosition() != null) {
                p.setName(getMyLocationName(mSphinx, p.getPosition()));
                return true;
            } else {
                //是“我的位置”且没有定位点则定位
                CityInfo cityInfo = Globals.g_My_Location_City_Info;
                
                if (cityInfo != null) {
                    Position position = cityInfo.getPosition();
                    p.setPosition(position);
                    p.setName(getMyLocationName(mSphinx, p.getPosition()));
                    return true;
                } else {
                    //定位失败
	                mSphinx.showTip(R.string.mylocation_failed, Toast.LENGTH_SHORT);
                    return false;
                }
            }
        } else {
            return true;
        }
	}
	
	protected int getQueryType(){
        int queryType = TrafficQuery.QUERY_TYPE_TRANSFER;
        
        switch(mRadioGroup.getCheckedRadioButtonId()){
        case R.id.traffic_transfer_rbt:
            queryType = TrafficQuery.QUERY_TYPE_TRANSFER;
            break;
        case R.id.traffic_drive_rbt:
            queryType = TrafficQuery.QUERY_TYPE_DRIVE;
            break;
        case R.id.traffic_walk_rbt:
            queryType = TrafficQuery.QUERY_TYPE_WALK;
            break;
        default:
            break;
        }
        
        return queryType;
    }
	    
    public void setQueryType(int queryType) {
        
        mSettedRadioBtn = R.id.traffic_transfer_rbt;
        switch (queryType) {
        case TrafficQuery.QUERY_TYPE_DRIVE:
            mSettedRadioBtn = R.id.traffic_drive_rbt;
            break;
        case TrafficQuery.QUERY_TYPE_TRANSFER:
            mSettedRadioBtn = R.id.traffic_transfer_rbt;
            break;
        case TrafficQuery.QUERY_TYPE_WALK:
            mSettedRadioBtn = R.id.traffic_walk_rbt;
            break;
        }
        mRadioGroup.check(mSettedRadioBtn);
        
    }    
    
    public View getTitleView() {
        return mTitleBar;
    }
	
	public void onBack() {
	    dismiss();
	    clearAllText();
	}
		
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
		if (BaseQuery.API_TYPE_TRAFFIC_QUERY.equals(baseQuery.getAPIType())) {
            dealWithTrafficResponse(mSphinx,
                    mActionTag,
                    (TrafficQuery)baseQuery,
                    true);
        } 
	}

	public static boolean dealWithTrafficResponse(Sphinx sphinx, String actionTag, TrafficQuery trafficQuery, boolean reset) {
	    
        TrafficModel trafficModel = trafficQuery.getTrafficModel();
        ActionLog actionLog = ActionLog.getInstance(sphinx);
        
        if (trafficModel == null) {
            actionLog.addAction(actionTag + ActionLog.TrafficResultTraffic, -1);
            showTrafficErrorTip(sphinx, trafficQuery);
            return false;
        } else if (trafficModel.getType() == TrafficModel.TYPE_EMPTY) {
            actionLog.addAction(actionTag + ActionLog.TrafficResultTraffic, -2);
            showTrafficErrorTip(sphinx, trafficQuery);
            return false;
        } else if (trafficModel.getType() == TrafficModel.TYPE_ALTERNATIVES 
        		|| trafficModel.getType() == TrafficModel.TYPE_PROJECT) {
            
            if (trafficModel.getType() == TrafficModel.TYPE_ALTERNATIVES) {
        		sphinx.getTrafficQueryFragment().showAlternativeDialog(trafficQuery.getTrafficModel().getStartAlternativesList(), trafficQuery.getTrafficModel().getEndAlternativesList());
        		
            } else if (trafficModel.getPlanList() == null
                    || trafficModel.getPlanList().size() <= 0) {
                
                actionLog.addAction(actionTag + ActionLog.TrafficResultTraffic, 0);
            	showTrafficErrorTip(sphinx, trafficQuery);
            	
            } else if (trafficModel.getType() == TrafficModel.TYPE_PROJECT) {
                List<Plan> planList = trafficModel.getPlanList();
                if (reset) {
                    sphinx.getTrafficDetailFragment().resetResult();
                }
                
                actionLog.addAction(actionTag + ActionLog.TrafficResultTraffic, planList.size());
            	
            	int type = planList.get(0).getType();
            	TrafficDetailFragment f = sphinx.getTrafficDetailFragment();
            	f.addResult(trafficQuery, type, planList);
            	if (type == Plan.Step.TYPE_TRANSFER) {
            	    // 换乘方式
            	    sphinx.getTrafficResultFragment().setData(trafficQuery);
            	    sphinx.showView(R.id.view_traffic_result_transfer);
            	} else if (type == Plan.Step.TYPE_DRIVE) {
            	    // 驾车
            	    f.refreshResult(type);
            		if (sphinx.uiStackPeek() == R.id.view_result_map) {
                        sphinx.getResultMapFragment().changeTrafficType(type);
                    } else {
                        sphinx.getResultMapFragment().setData(null, ActionLog.TrafficDriveListMap);
                        sphinx.showView(R.id.view_result_map);
                        TrafficOverlayHelper.drawTrafficPlanListOverlay(sphinx, planList, 0);
                        TrafficOverlayHelper.panToViewWholeOverlay(planList.get(0), sphinx.getMapView(), sphinx);
                    }
            	} else if (type == Plan.Step.TYPE_WALK) {
                    // 步行方式
            	    f.refreshResult(type);
                    if (sphinx.uiStackPeek() == R.id.view_result_map) {
                        sphinx.getResultMapFragment().changeTrafficType(type);
                    } else {
                        sphinx.getResultMapFragment().setData(null, ActionLog.TrafficWalkListMap);
                        sphinx.showView(R.id.view_result_map);
                        TrafficOverlayHelper.drawTrafficPlanListOverlay(sphinx, planList, 0);
                        TrafficOverlayHelper.panToViewWholeOverlay(planList.get(0), sphinx.getMapView(), sphinx);
                    }
                }
            }
        } 
        return true;
    }
    
    private static void showTrafficErrorTip(Sphinx sphinx, TrafficQuery trafficQuery) {
        if (!trafficQuery.isStop()) {
            
            TrafficModel trafficModel = trafficQuery.getTrafficModel();
            if (trafficModel == null && trafficQuery.getStatusCode() == BaseQuery.STATUS_CODE_NONE) {
                sphinx.showTip(R.string.network_failed, Toast.LENGTH_SHORT);
                return;
            }
            
            POI startPOI = trafficQuery.getStart();
            POI endPOI = trafficQuery.getEnd();
            Position start = startPOI.getPosition();
            Position end = endPOI.getPosition();
            
            boolean isSameCity = true;
            if (Util.inChina(start) && Util.inChina(end)) {
                int cityId = MapEngine.getCityId(start);
                isSameCity &= (cityId == MapEngine.getCityId(end));
            }
            if (!isSameCity) {
                if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_TRANSFER) {
                    sphinx.showTip(R.string.transfer_non_support_city_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_DRIVE) {
                    sphinx.showTip(R.string.drive_non_support_city_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_WALK) {
                    sphinx.showTip(R.string.walk_non_support_city_tip, Toast.LENGTH_SHORT);
                }
            } else if (trafficQuery.getResponseXMap() == null) {
                sphinx.showTip(R.string.no_result, Toast.LENGTH_SHORT);
            } else {
                if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_TRANSFER) {
                    sphinx.showTip(R.string.transfer_non_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_DRIVE) {
                    sphinx.showTip(R.string.drive_non_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_WALK) {
                    sphinx.showTip(R.string.walk_non_tip, Toast.LENGTH_SHORT);
                }
            }
        }
    }

	class StationAdapter extends BaseAdapter {
	    List<Station> mList;

	    public StationAdapter(List<Station> list) {
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
            TextView name = (TextView) convertView.findViewById(R.id.name_txv);
            TextView address = (TextView) convertView.findViewById(R.id.address_txv);
            setTxvText(name, mList.get(position).getName());
            setTxvText(address, mList.get(position).getAddress());
            return convertView;
        }
	    
	}
	
    private void showAlternativeDialog(final List<Station> startStationList, final List<Station> endStationList) {
    	final boolean start = (startStationList != null);
    	final boolean end = (endStationList != null);
    	final StationAdapter adapter;
    	//如果有start，就先处理start的对话框，处理完后如果还有end则递归，参数传（null，end）
    	if (start) {
    	    adapter = new StationAdapter(startStationList);
    	} else 	if (end) {
    	    adapter = new StationAdapter(endStationList);
    	} else {
    	    return;
    	}
        
        mActionLog.addAction(ActionLog.TrafficAlternative);
        
        View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
        
        ListView listView = (ListView) alterListView.findViewById(R.id.listview);
        listView.setAdapter(adapter);
        
        final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
        
        TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
        titleTxv.setText(start ? R.string.select_start_station : R.string.select_end_station);
        
        Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
        button.setVisibility(View.GONE);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
            	Station station = null;
            	//如果点到这里来，第一条肯定是刚才搜索的。
            	SearchHistory sh_old = mQueryHistorys.get(0);
            	delSearchHistory(sh_old);
            	if (start) {
            	    station = startStationList.get(which);
            	    setStart(station.toPOI());
            	    addSearchHistory(new SearchHistory(station.toPOI(), sh_old.end));
            	} else if (end) {
            		station = endStationList.get(which);
            	    setEnd(station.toPOI());
            	    addSearchHistory(new SearchHistory(sh_old.start, station.toPOI()));
            	}
                dialog.dismiss();
                if (station != null) {
                    mActionLog.addAction(ActionLog.TrafficAlternative + (start ? ActionLog.TrafficAlterStart : ActionLog.TrafficAlterEnd), which, station.getName());
                }
                if (start == false || end == false) {
                    submitTrafficQuery(mStart.getClonedPOI(), mEnd.getClonedPOI());
                } else {
                	showAlternativeDialog(null, endStationList);
                }
            }
            
        });
        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                mActionLog.addAction(ActionLog.TrafficAlternative + ActionLog.Dismiss);
            }
        });
        
        dialog.show();
        mSphinx.setShowingDialog(dialog);
    }
    
    private void updateCommonPlace() {
        //暂时不能新增常用地址，只有一项
        mCommonPlaces.updateData();
        mCommonPlaceAdapter.refreshList(mCommonPlaces.getList());
    }
			    	
	public void postTask(Runnable r) {
		mSphinx.getHandler().post(r);
	}

    @Override
    public void onClick(View v) {
        DataQuery dataQuery;
        switch (v.getId()) {
        case R.id.right_btn:
            if (!mStart.textEmpty() && !mEnd.textEmpty()) {
                mActionLog.addAction(mActionTag + ActionLog.TrafficHomeClickSearchBtn);
                query();
            } else {
                mActionLog.addAction(mActionTag + ActionLog.TrafficHomeClickMoreBtn);
                String[] list = mSphinx.getResources().getStringArray(R.array.traffic_search_option);
                int[] leftCompoundIconList = new int[3];
                leftCompoundIconList[0] = R.drawable.ic_search_busline;
                leftCompoundIconList[1] = R.drawable.ic_search_busstop;
                leftCompoundIconList[2] = R.drawable.ic_map_tools_subway;
                final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list, leftCompoundIconList);
                
                View alterListView = mSphinx.getLayoutInflater().inflate(R.layout.alert_listview, null, false);
                
                ListView listView = (ListView) alterListView.findViewById(R.id.listview);
                listView.setAdapter(adapter);
                
                final Dialog dialog = Utility.getChoiceDialog(mSphinx, alterListView, R.style.AlterChoiceDialog);
                
                TextView titleTxv = (TextView)alterListView.findViewById(R.id.title_txv);
                titleTxv.setText(R.string.more);
                
                Button button = (Button)alterListView.findViewById(R.id.confirm_btn);
                button.setVisibility(View.GONE);
                
                dialog.show();
                
                listView.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View arg1, int index, long arg3) {

                        mActionLog.addAction(mActionTag + ActionLog.TrafficHomeClickMoreAlert, index);
                        if (index == 0 || index == 1) {
                            DataQuery dataQuery = new DataQuery(mSphinx);
                            dataQuery.setCityId(getCityId(mStart.getPOI(), mEnd.getPOI()));
                            mSphinx.getInputSearchFragment().setData(dataQuery,
                            		null,
                                    InputSearchFragment.MODE_BUELINE);
                            mSphinx.showView(R.id.view_poi_input_search);
                        } else if (index == 2) {
                            mSphinx.getSubwayMapFragment().setData(Globals.getCurrentCityInfo(mSphinx, false));
                            mSphinx.showView(R.id.view_subway_map);
                        }
                        dialog.setOnDismissListener(new OnDismissListener() {
                            
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        dialog.dismiss();
                    }
                });
            }
            break;
            
            //TODO
        case R.id.end_btn:
            mActionLog.addAction(mActionTag + ActionLog.TrafficEndEdt);
            String e = null;
            if (!isKeyword(mEnd.getText())) {
                e = mEnd.getText();
            }
            dataQuery = new DataQuery(mSphinx);
            dataQuery.setCityId(getCityId(mStart.getPOI(), mEnd.getPOI()));
            mSphinx.getInputSearchFragment().setData(dataQuery,
            		e,
                    InputSearchFragment.MODE_TRAFFIC,
                    new InputSearchFragment.IResponsePOI(){

                        @Override
                        public void responsePOI(POI p) {
                            mEnd.setPOI(p);
                            autoStartQuery(true);
                        }
                    },
                    InputSearchFragment.REQUEST_TRAFFIC_END);
            mSphinx.showView(R.id.view_poi_input_search);
            break;
            
        case R.id.start_btn:
            mActionLog.addAction(mActionTag + ActionLog.TrafficStartEdt);
            String s = null;
            if (!isKeyword(mStart.getText())) {
                s = mStart.getText();
            }
            dataQuery = new DataQuery(mSphinx);
            dataQuery.setCityId(getCityId(mStart.getPOI(), mEnd.getPOI()));
            mSphinx.getInputSearchFragment().setData(dataQuery,
            		s,
                    InputSearchFragment.MODE_TRAFFIC,
                    new InputSearchFragment.IResponsePOI(){

                        @Override
                        public void responsePOI(POI p) {
                            mStart.setPOI(p);
                            autoStartQuery(true);
                        }
                    },
                    InputSearchFragment.REQUEST_TRAFFIC_START);
            mSphinx.showView(R.id.view_poi_input_search);
            break;
            
        case R.id.add_common_place:
//            mActionLog.addAction(mActionTag, );
            mSphinx.showView(R.id.view_traffic_common_places);
            break;
        
        case R.id.query_history_title:
            mActionLog.addAction(mActionTag + ActionLog.TrafficHomeHistoryTitle);
            mSphinx.showView(R.id.view_traffic_search_history);
            break;
            
        case R.id.traffic_switch_btn:
            mActionLog.addAction(mActionTag + ActionLog.TrafficSwitchStartEnd);
            switchStartEnd();
            break;
            
        }
    }
    
}

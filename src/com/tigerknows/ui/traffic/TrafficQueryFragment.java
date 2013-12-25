package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.LocationQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Station;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.CommonPlaceTable;
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
	
	public String MY_LOCATION;
	
	private int mSettedRadioBtn = 0;
	
	boolean showStartMyLocation = true;
	
	private boolean autoStartQuery = false;

	FrameLayout mTitle;
	
	Button mBackBtn;
	
	Button mTrafficSwitchBtn;
	
	Button mBuslineQueryBtn;
	
	RadioGroup mRadioGroup = null;
	
	LinearLayout mBlock;
	
	Button mSelectStartBtn = null;
	
	Button mSelectEndBtn = null;

	/**
	 * 注：因为起终点对应的poi直接setTag给了按钮，所以不要直接给这两个按钮直接赋值，
	 * 而是要使用setPOI，getPOI，getStartEndText函数
	 */
	private InputBtn mStart;
    
	private InputBtn mEnd;
		
//	RelativeLayout mTrafficLayout;
//
//	RelativeLayout mBuslineLayout;
	
//	LinearLayout mAddCommonPlace;
	
	LinearLayout mCommonPlaceLst;
	
	LinearLayout mQueryHistory;
	
	LinearLayout mQueryHistoryLst;
	
	View mTitleBar;
	
	LinearListAdapter mQueryHistoryAdapter;
	LinearListAdapter mCommonPlaceAdapter;
            
    CommonPlaceList mCommonPlaces = new CommonPlaceList(mSphinx);

    List<SearchHistory> mQueryHistorys = new LinkedList<SearchHistory>();
    
    TrafficSearchHistoryTable mHistoryTable = new TrafficSearchHistoryTable(mSphinx);
    
//	int oldCheckButton;

	/*
	 * 行为日志辅助类
	 */
	TrafficQueryLogHelper mLogHelper;
	
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
	        poi = p;
	    }
	    
	    public POI getPOI() {
	        
	        if (TextUtils.isEmpty(btn.getText())) {
	            poi = new POI();
	        }

	        return poi;
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
//    	KEYWORDS = new String[]{
//    		getString(R.string.my_location),
//    		getString(R.string.select_has_point),
//    	};
//    	
//    	keywordList = Arrays.asList(KEYWORDS);
    	
//        mEventHelper = new TrafficQueryEventHelper(this);
//        mAnimationHelper = new TrafficQueryAnimationHelper(this);
//        mStateHelper = new TrafficQueryStateHelper(this);
//        mStateTransitionTable = new TrafficViewSTT(mStateHelper);
        mLogHelper = new TrafficQueryLogHelper(this);
//        mSuggestWordHelper = new TrafficQuerySuggestWordHelper(mContext, this, mSuggestLsv);
        
        mStart.setHint(getString(R.string.start_));
        mEnd.setHint(getString(R.string.end_));
		
        return mRootView;
    }
	
    protected void findViews() {
    	
//    	mTitle = (FrameLayout)mRootView.findViewById(R.id.title_lnl);
        mBlock = (LinearLayout)mRootView.findViewById(R.id.content_lnl);
		
    	mBackBtn = (Button)mRootView.findViewById(R.id.back_btn);
    	mTrafficSwitchBtn = (Button)mRootView.findViewById(R.id.traffic_switch_btn);
    	mBuslineQueryBtn = (Button)mRootView.findViewById(R.id.busline_query_btn);
        mTitleBar = mLayoutInflater.inflate(R.layout.traffic_query_bar, null);
    	mRadioGroup = (RadioGroup)mTitleBar.findViewById(R.id.traffic_rgp);
    	
    	Button startBtn, endBtn;
    	startBtn = (Button)mRootView.findViewById(R.id.start_btn);
    	endBtn = (Button)mRootView.findViewById(R.id.end_btn);
    	mStart = new InputBtn(startBtn);
    	mEnd = new InputBtn(endBtn);
    	
    	mQueryHistory = (LinearLayout) mRootView.findViewById(R.id.query_history_title);
    	mQueryHistoryLst = (LinearLayout)mRootView.findViewById(R.id.query_history_lst);
//    	mAddCommonPlace = (LinearLayout)mRootView.findViewById(R.id.add_common_place);
    	mCommonPlaceLst = (LinearLayout)mRootView.findViewById(R.id.common_place_lsv);
		
//		mSuggestLnl = (LinearLayout)mRootView.findViewById(R.id.suggest_lnl);
//		mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
    	setListeners();
    }
	
	void setListeners() {
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
	            mStart.setPOI(h.start);
	            mEnd.setPOI(h.end);
	            updateSearchHistory(h);
	            submitTrafficQuery(h.start, h.end);
	        }
	    };
	    mQueryHistoryAdapter = new LinearListAdapter(mSphinx, mQueryHistoryLst, R.layout.traffic_transfer_his_item) {

	        @Override
	        public View getView(Object data, View child, int pos) {
	            SearchHistory h = (SearchHistory)data;
	            TextView t = (TextView) child.findViewById(R.id.his_txv);
	            child.setTag(data);
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
                if (c.isEmptyFixedPlace()) {
                    mSphinx.getInputSearchFragment().setMode(InputSearchFragment.MODE_TRAFFIC);
                    mSphinx.getInputSearchFragment().setConfirmedCallback(new InputSearchFragment.Callback(){

                        @Override
                        public void onConfirmed(POI p) {
                            mCommonPlaces.setPOI(0, p);
                            mCommonPlaceAdapter.refreshList(mCommonPlaces.getList());
                            
                        }}, InputSearchFragment.REQUEST_COMMON_PLACE);
                    mSphinx.showView(mSphinx.getInputSearchFragment().getId());
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
	            //FIXME:进行严格检查
	            TextView aliasTxv = (TextView) child.findViewById(R.id.alias_txv);
	            TextView descTxv = (TextView) child.findViewById(R.id.description_txv);
	            child.setTag(data);
	            child.setOnClickListener(placeItemListener);
	            CommonPlace c = (CommonPlace) data;
	            aliasTxv.setText(c.alias);
	            if (c.isEmptyFixedPlace()) {
	                descTxv.setText("click to set");
	            } else {
	                descTxv.setText(c.poi.getName());
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
    	
    }
    
    public void onPause() {
    	super.onPause();
    	mSettedRadioBtn = 0;
    	autoStartQuery = false;
    }
    
	@Override
	public void onResume() {
	    if (TKConfig.getPref(mSphinx, TKConfig.PREFS_HINT_LOCATION) == null) {
	        mSphinx.showHint(TKConfig.PREFS_HINT_LOCATION, R.layout.hint_location);
	    }
	    super.onResume();
         
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
        
        mTitleView.removeAllViews();
        mTitleView.addView(mTitleBar);
        mRightBtn.setBackgroundResource(R.drawable.btn_view_detail);
        initHistory();
        updateCommonPlace();
        if (!mStart.textEmpty() 
                && !mEnd.textEmpty()
                && autoStartQuery) {
            query();
        }
	}
    
	void makeToast(String s) {
	    Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
	}
	
	public void setShowStartMyLocation(boolean showMyLocation) {
	    this.showStartMyLocation = showMyLocation;
	}
	
	public final void autoStartQuery(boolean b) {
	    autoStartQuery = b;
	}
	
	public void initStartContent() {

	    if (showStartMyLocation) {
	        //如果要求显示起点，定位为设定城市，在起点框填写上当前位置。
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
	    //TODO:有定位信息则返回
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
	    POI start = mStart.getPOI().clone();
        POI end = mEnd.getPOI().clone();
        
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

        if (!processMyLocation(start)) {
            return;
        }
        if (!processMyLocation(end)) {
            return;
        }
        
        TrafficQuery trafficQuery = new TrafficQuery(mContext);
        
        addHistoryWord(start, HistoryWordTable.TYPE_TRAFFIC);
        addHistoryWord(end, HistoryWordTable.TYPE_TRAFFIC);
    		
        mActionLog.addAction(mActionTag +  ActionLog.TrafficTrafficBtn, getQueryType(), mStart.getText(), mEnd.getText());
        trafficQuery.setup(start, end, getQueryType(), getId(), getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(trafficQuery);
	}
    
    public void addHistoryWord(POI poi, int type) {
        if (poi == null) {
            return;
        }
        String name = poi.getName();
        if (name != null && name.trim().equals(MY_LOCATION) == false) {
            Position position = poi.getPosition();
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, name, position), type);
        }
    }
    
    public void addSearchHistory(SearchHistory sh) {
        if (!mQueryHistorys.contains(sh)) {
            mHistoryTable.add(sh);
            mQueryHistorys.add(0, sh);
            mQueryHistoryAdapter.refreshList(mQueryHistorys);
        } else {
            updateSearchHistory(sh);
        }
    }
    
    public void updateSearchHistory(SearchHistory sh) {
        mQueryHistorys.remove(sh);
        mQueryHistorys.add(0, sh);
        mHistoryTable.update(sh);
        mQueryHistoryAdapter.refreshList(mQueryHistorys);
    }
    
    public void initHistory() {
        mQueryHistorys.clear();
        mHistoryTable.readTrafficSearchHistory(mQueryHistorys);
        mQueryHistoryAdapter.refreshList(mQueryHistorys);
    }
    
    public static void submitTrafficQuery(Sphinx sphinx, POI start, POI end, int queryType) {
        
        if (start == null || end == null)
            return;
        //以下内容出现在两个submit中，以后重构的时候注意写在一块
        if (start.getName().equals(sphinx.getString(R.string.my_location))) {
            start.setName(getMyLocationName(sphinx, start.getPosition()));
        } 
        if (end.getName().equals(sphinx.getString(R.string.my_location))) {
            end.setName(getMyLocationName(sphinx, end.getPosition()));
        }
        
        TrafficQuery trafficQuery = new TrafficQuery(sphinx);
            
        trafficQuery.setup(start, end, queryType, R.id.view_traffic_home, sphinx.getString(R.string.doing_and_wait));
        
        sphinx.queryStart(trafficQuery);
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
		if (getString(R.string.select_has_point).equals(mStart.getText())
				&& getString(R.string.select_has_point).equals(mEnd.getText())) {
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
            if (p.getPosition() != null) {
                p.setName(getMyLocationName(mSphinx, p.getPosition()));
                return true;
            } else {
                //是“我的位置”且没有定位点则定位
                LocationQuery locationQuery = LocationQuery.getInstance(getContext());
                Location location = locationQuery.getLocation();
                Position position = null;
                if (location != null) {
                    position = MapEngine.getInstance().latlonTransform(new Position(location.getLatitude(), location.getLongitude()));
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
	    
    //TODO:把这个函数也重写，mSettedRadioBtn不要这么用
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
	
	public void onBack() {
//		if (mStateTransitionTable.event(Event.Back)) {
//			
//		} else {
			dismiss();
//		}
			clearAllText();
	}
		
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
		if (BaseQuery.API_TYPE_TRAFFIC_QUERY.equals(baseQuery.getAPIType())) {
            this.queryTrafficEnd((TrafficQuery)baseQuery);
        } 
	}

	public void queryTrafficEnd(TrafficQuery trafficQuery) {
        TrafficModel trafficModel = trafficQuery.getTrafficModel();
        if (trafficModel == null) {
            mActionLog.addAction(mActionTag + ActionLog.TrafficResultTraffic, -1);
            showTrafficErrorTip(trafficQuery);
        } else if (trafficModel.getType() == TrafficModel.TYPE_EMPTY) {
            mActionLog.addAction(mActionTag + ActionLog.TrafficResultTraffic, -2);
            showTrafficErrorTip(trafficQuery);
        } else if (trafficModel.getType() == TrafficModel.TYPE_ALTERNATIVES 
        		|| trafficModel.getType() == TrafficModel.TYPE_PROJECT){
            if (trafficModel.getType() == TrafficModel.TYPE_ALTERNATIVES) {
        		showAlternativeDialog(trafficQuery.getTrafficModel().getStartAlternativesList(), trafficQuery.getTrafficModel().getEndAlternativesList());
            } else if (trafficModel.getPlanList() == null || trafficModel.getPlanList().size() <= 0){
                mActionLog.addAction(mActionTag + ActionLog.TrafficResultTraffic, 0);
            	showTrafficErrorTip(trafficQuery);
            } else if (trafficModel.getType() == TrafficModel.TYPE_PROJECT) {
                mActionLog.addAction(mActionTag + ActionLog.TrafficResultTraffic, trafficModel.getPlanList().size());
            	// 若之前发出的请求中的起终点被服务器修改, 此处要修改客户端显示
//            	if (trafficQuery.isPOIModified()) {
//            		modifyData(trafficQuery.getStart(), TrafficQueryFragment.START);
//            		modifyData(trafficQuery.getEnd(), TrafficQueryFragment.END);
//            	} 
            	
            	// 下一行代码为避免以下操作会出现问题
            	// 搜索-结果列表-详情-地图-点击气泡中的交通按钮-选择到这里去/自驾-点击左上按钮无反应(期望进入 详情界面)
            	// 确保整个UI堆栈里不能出现重复的结果地图界面
                // Fixed: [And4.30-287] [确实有]【交通】公交详情界面点击“地图”返回到公交方案界面，地图不加载。
            	mSphinx.uiStackRemove(R.id.view_result_map);
            	if (trafficModel.getPlanList().get(0).getType() == Plan.Step.TYPE_TRANSFER) {
            	    // 换乘方式
            		mSphinx.getTrafficResultFragment().setData(trafficQuery);
            		mSphinx.showView(R.id.view_traffic_result_transfer);
            	} else {
            	    //驾车或步行方式
            	    Plan plan = trafficModel.getPlanList().get(0);
            		mSphinx.getTrafficDetailFragment().setData(plan);
            		mSphinx.getTrafficDetailFragment().viewPlanMap();
            	}
            }
        } 
    }

    private void showAlternativeDialog(final List<Station> startStationList, final List<Station> endStationList) {
    	List<String> list = new ArrayList<String>();
    	final boolean start = (startStationList != null);
    	final boolean end = (endStationList != null);
    	//如果有start，就先处理start的对话框，处理完后如果还有end则递归，参数传（null，end）
    	if (start) {
			for(int i = 0, size = startStationList.size(); i < size; i++) {
				list.add(startStationList.get(i).getName());
			}
    	} else 	if (end) {
    		for(int i = 0, size = endStationList.size(); i < size; i++) {
				list.add(endStationList.get(i).getName());
			}
    	}
		final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, list);
        
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
            	if (start) {
            	    station = startStationList.get(which);
            	    setStart(station.toPOI());
            	} else if (end) {
            		station = endStationList.get(which);
            	    setEnd(station.toPOI());
            	}
                dialog.dismiss();
                if (station != null) {
                    mActionLog.addAction(ActionLog.TrafficAlternative + (start ? ActionLog.TrafficAlterStart : ActionLog.TrafficAlterEnd), which, station.getName());
                }
                if (start == false || end == false) {
                    submitTrafficQuery(mStart.getPOI(), mEnd.getPOI());
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
    
    public void showTrafficErrorTip(TrafficQuery trafficQuery) {
        if (!trafficQuery.isStop()) {
        	
        	TrafficModel trafficModel = trafficQuery.getTrafficModel();
        	if (trafficModel == null && trafficQuery.getStatusCode() == BaseQuery.STATUS_CODE_NONE) {
        		mSphinx.showTip(R.string.network_failed, Toast.LENGTH_SHORT);
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
                	mSphinx.showTip(R.string.transfer_non_support_city_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_DRIVE) {
                	mSphinx.showTip(R.string.drive_non_support_city_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_WALK) {
                	mSphinx.showTip(R.string.walk_non_support_city_tip, Toast.LENGTH_SHORT);
                }
            } else if (trafficQuery.getResponseXMap() == null) {
            	mSphinx.showTip(R.string.no_result, Toast.LENGTH_SHORT);
            } else {
                if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_TRANSFER) {
                	mSphinx.showTip(R.string.transfer_non_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_DRIVE) {
                	mSphinx.showTip(R.string.drive_non_tip, Toast.LENGTH_SHORT);
                } else if (trafficQuery.getQueryType() == TrafficQuery.QUERY_TYPE_WALK) {
                	mSphinx.showTip(R.string.walk_non_tip, Toast.LENGTH_SHORT);
                }
            }
        }
    }
    
    private void updateCommonPlace() {
        //暂时不能新增常用地址，只有一项
        mCommonPlaces.updateData();
        mCommonPlaceAdapter.refreshList(mCommonPlaces.getList());
    }
    
    private void setCommonPlace() {
        
    }
			    	
	public void postTask(Runnable r) {
		mSphinx.getHandler().post(r);
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.right_btn:
            // TODO:弹出三个按钮或者变成确定按钮
            if (!mStart.textEmpty() && !mEnd.textEmpty()) {
                query();
            } else {
                mSphinx.getInputSearchFragment().reset();
                mSphinx.getInputSearchFragment().setMode(InputSearchFragment.MODE_BUELINE);
                mSphinx.showView(R.id.view_poi_input_search);
            }
            break;
            
        case R.id.end_btn:
            mSphinx.getInputSearchFragment().reset();
            mSphinx.getInputSearchFragment().setMode(InputSearchFragment.MODE_TRAFFIC);
            mSphinx.getInputSearchFragment().setConfirmedCallback(new InputSearchFragment.Callback() {
                
                @Override
                public void onConfirmed(POI p) {
                    mEnd.setPOI(p);
                }
            }, InputSearchFragment.REQUEST_TRAFFIC_END);
            mSphinx.showView(R.id.view_poi_input_search);
            break;
            
        case R.id.start_btn:
            mSphinx.getInputSearchFragment().reset();
            mSphinx.getInputSearchFragment().setMode(InputSearchFragment.MODE_TRAFFIC);
            mSphinx.getInputSearchFragment().setConfirmedCallback(new InputSearchFragment.Callback() {
                
                @Override
                public void onConfirmed(POI p) {
                    mStart.setPOI(p);
                }
            }, InputSearchFragment.REQUEST_TRAFFIC_START);
            mSphinx.showView(R.id.view_poi_input_search);
            break;
            
        case R.id.add_common_place:
            mSphinx.showView(R.id.view_traffic_common_places);
            break;
        
        case R.id.query_history_title:
            mSphinx.showView(R.id.view_traffic_search_history);
            break;
            
        case R.id.traffic_switch_btn:
            switchStartEnd();
            break;
            
        }
    }
    
}

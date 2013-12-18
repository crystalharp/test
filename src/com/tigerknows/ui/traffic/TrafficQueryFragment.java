package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.android.widget.TKEditText;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.History;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Station;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.more.HistoryFragment;
import com.tigerknows.ui.poi.InputSearchFragment;
//import com.tigerknows.ui.traffic.TrafficViewSTT.Event;
//import com.tigerknows.ui.traffic.TrafficViewSTT.State;
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
public class TrafficQueryFragment extends BaseFragment {

//	public static final int TRAFFIC_MODE = 1;
//	
//	public static final int BUSLINE_MODE = 2;
	
	public static final int START = 1;
	
	public static final int END = 2;
	
	public static final int LINE = 3;
	
	//TODO:不记得这个变量和选中的框什么意思了，先注掉
	public static final int SELECTED = 4;
	
//	protected int mode = TRAFFIC_MODE;
	
	private int mSettedRadioBtn = 0;
	
	boolean showStartMyLocation = true;
	
	private boolean autoStartQuery = false;

	FrameLayout mTitle;
	
	Button mBackBtn;
	
	Button mTrafficQueryBtn;
	
	Button mBuslineQueryBtn;
	
	RadioGroup mRadioGroup = null;
	
	LinearLayout mBlock;
	
	Button mSelectStartBtn = null;
	
	Button mSelectEndBtn = null;
    
//	QueryEditText mBusline;

	/**
	 * 注：因为起终点对应的poi直接setTag给了按钮，所以不要直接给这两个按钮直接赋值，
	 * 而是要使用setPOI，getPOI，getStartEndText函数
	 */
	private InputBtn mStart;
    
	private InputBtn mEnd;
	
//	POI mStart;
	
//	POI mEnd;
    
	QueryEditText mSelectedEdt;
	    
//    LinearLayout mSuggestLnl;
    
//	ListView mSuggestLsv;
	
	RelativeLayout mTrafficLayout;

	RelativeLayout mBuslineLayout;
	
	LinearLayout mAddFavPlace;
	
	LinearLayout mFavPlaceLst;
	
	LinearLayout mQueryHistory;
	
	LinearLayout mQueryHistoryLst;
	
	View mTitleBar;
	
	LinearListAdapter mQueryHistoryAdapter;
	//TODO:添加resid
	LinearListAdapter mFavPlaceAdapter;
        
    class FavPlace {
        String alias;
        POI poi;
        
        public FavPlace(String a, POI p) {
            alias = a;
            poi = p;
        }
    }
    
    List<FavPlace> mFavPlaces = new LinkedList<FavPlace>();

    List<History> mQueryHistorys = new LinkedList<History>();
//	int oldCheckButton;

	String[] KEYWORDS;
	
	List<String> keywordList = null;
	
	/*
	 * 控制此页面所有控件的事件
	 */
//	TrafficQueryEventHelper mEventHelper;
	/*
	 * 状态转化表 
	 */
//	TrafficViewSTT mStateTransitionTable;
	
	/*
	 * 动画辅助类
	 */
//	TrafficQueryAnimationHelper mAnimationHelper;
	
	/*
	 * 地图中心点辅助类
	 */
	TrafficQueryMapAndLocationHelper mMapLocationHelper;
	
	/*
	 * 状态切换控制类
	 */
//	TrafficQueryStateHelper mStateHelper;
	
	/*
	 * 行为日志辅助类
	 */
	TrafficQueryLogHelper mLogHelper;
	
	/*
	 * 历史词及联想词辅助类
	 */
//	TrafficQuerySuggestWordHelper mSuggestWordHelper;
	
	public static final String TAG = "TrafficQueryFragment";
	
	//疑似无用
//	public CityInfo getQueryCityInfo() {
//		return mMapLocationHelper.getQueryCityInfo();
//	}
	
	final class InputBtn {
	    Button btn;
	    POI poi;
	    
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
	        } else if (mContext.getString(R.string.my_location).equals(btn.getText().toString())) {
	            POI location = mMapLocationHelper.getMyLocation();
	            if (location != null) {
	                poi = new POI();
	                poi.setName(btn.getText().toString());
	                poi.setPosition(location.getPosition());
	            } else {
	                mSphinx.showTip(R.string.mylocation_failed, Toast.LENGTH_SHORT);
	                return null;
	            }
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
    	
    	KEYWORDS = new String[]{
    		mContext.getString(R.string.my_location),
    		mContext.getString(R.string.select_has_point),
    	};
    	
    	keywordList = Arrays.asList(KEYWORDS);
    	
//        mEventHelper = new TrafficQueryEventHelper(this);
//        mAnimationHelper = new TrafficQueryAnimationHelper(this);
        mMapLocationHelper = new TrafficQueryMapAndLocationHelper(this);
//        mStateHelper = new TrafficQueryStateHelper(this);
//        mStateTransitionTable = new TrafficViewSTT(mStateHelper);
        mLogHelper = new TrafficQueryLogHelper(this);
//        mSuggestWordHelper = new TrafficQuerySuggestWordHelper(mContext, this, mSuggestLsv);
        
        mStart.setHint(mContext.getString(R.string.start_));
        mEnd.setHint(mContext.getString(R.string.end_));
		
        return mRootView;
    }
	
    protected void findViews() {
    	
//    	mTitle = (FrameLayout)mRootView.findViewById(R.id.title_lnl);
        mBlock = (LinearLayout)mRootView.findViewById(R.id.content_lnl);
		
		mTrafficLayout = (RelativeLayout)mRootView.findViewById(R.id.traffic_rll);
		mBuslineLayout = (RelativeLayout)mRootView.findViewById(R.id.busline_rll);
        
    	mBackBtn = (Button)mRootView.findViewById(R.id.back_btn);
    	mTrafficQueryBtn = (Button)mRootView.findViewById(R.id.traffic_query_btn);
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
    	mAddFavPlace = (LinearLayout)mRootView.findViewById(R.id.add_fav_place);
    	mFavPlaceLst = (LinearLayout)mRootView.findViewById(R.id.fav_place_lst);
		
//		mSuggestLnl = (LinearLayout)mRootView.findViewById(R.id.suggest_lnl);
//		mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
    	setListeners();
    }
	
	void setListeners() {
	    mAddFavPlace.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
//                showView();
                //跳转到添加常用地址
                makeToast("showView add fav place");
            }
        });
	    
	    mQueryHistory.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //跳转到查询线路历史
                mSphinx.showView(mSphinx.getHistoryFragment().getId());
            }
        });
	    
	    mTrafficQueryBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                switchStartEnd();
            }
        });
	    
	    mStart.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //转到起终点输入页
                mSphinx.getPOIQueryFragment().reset();
                mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_TRANSFER);
                mSphinx.getPOIQueryFragment().setConfirmedCallback(new InputSearchFragment.Callback() {
                    
                    @Override
                    public void onConfirmed(POI p) {
                        mStart.setPOI(p);
                    }
                });
                mSphinx.showView(R.id.view_poi_input_search);
            }
        });
	    
	    mEnd.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //转到起终点输入页，怎么做个通用接口来返回POI？
                mSphinx.getPOIQueryFragment().reset();
                mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_TRANSFER);
                mSphinx.getPOIQueryFragment().setConfirmedCallback(new InputSearchFragment.Callback() {
                    
                    @Override
                    public void onConfirmed(POI p) {
                        mEnd.setPOI(p);
                    }
                });
                mSphinx.showView(R.id.view_poi_input_search);
            }
        });
	   
	//---------------------history start-----------------------//
	    final OnClickListener historyItemListener = new View.OnClickListener() {

	        @Override
	        public void onClick(View v) {
	            // TODO Auto-generated method stub
	            History h = (History) v.getTag();
	            HistoryFragment.showTrafficDetail(mSphinx, h);
	        }
	    };
	    mQueryHistoryAdapter = new LinearListAdapter(mSphinx, mQueryHistoryLst, R.layout.traffic_transfer_his_item) {

	        @Override
	        public View getView(Object data, View child, int pos) {
	            // TODO Auto-generated method stub
	            History h = (History)data;
	            TextView t = (TextView) child.findViewById(R.id.his_text);
	            child.setTag(data);
	            t.setText(HistoryFragment.getTrafficName(h));
	            child.setOnClickListener(historyItemListener);
	            return null;
	        }
	    };

    //----------------------history end------------------------//

	    mFavPlaceAdapter = new LinearListAdapter(mSphinx, mFavPlaceLst, 0) {

	        @Override
	        public View getView(Object data, View child, int pos) {
	            // TODO Auto-generated method stub
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
//    	mMapLocationHelper.mTargetCityInfo = null;
//    	mSphinx.setTouchMode(TouchMode.NORMAL);
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
         
	    // TODO:这个listener需要重用，经常刷新
	    mRightBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                // 弹出三个按钮或者编程确定
                makeToast("right btn clicked");
                mSphinx.getPOIQueryFragment().reset();
                mSphinx.getPOIQueryFragment().setMode(InputSearchFragment.MODE_BUELINE);
                mSphinx.showView(R.id.view_poi_input_search);
            }
        });
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
        HistoryFragment.readTraffic(mContext, mQueryHistorys, Long.MAX_VALUE, 0, false);
        mQueryHistoryAdapter.refreshList(mQueryHistorys);
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
	            POI poi = mMapLocationHelper.getMyLocation();
	            mStart.setPOI(poi);
	        }
	        //否则什么都不做，不影响当前起点框内容
	    } else {
	        //否则清空起点框的内容。
	        mStart.clear();
	    }
		
    }
	
	void switchStartEnd() {
	    //TODO:switch start end content
	    makeToast("switch start end");
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
	
	public void setPOI(POI poi, int index) {
	    switch(index) {
	    case START:
	        mStart.setPOI(poi);
	        break;
	    case END:
	        mEnd.setPOI(poi);
	        break;
	    case SELECTED:
	        //            mSelectedEdt.setPOI(poi);
	    default:

	    }
	}

	public View getContentView() {
//		if (mode == TRAFFIC_MODE) 
			return mTrafficLayout;
//		else 
//			return mBuslineLayout;
	}
	
	public void query() {
//		mSphinx.hideSoftInput();
		
//		if (mode == TRAFFIC_MODE) {
			submitTrafficQuery();
//		} else {
//			submitBuslineQuery();
//		}

	}

	public void submitTrafficQuery() {
		
		POI start = mStart.getPOI();
		POI end = mEnd.getPOI();
		
		if (start == null || end == null)
			return;
		
		if (checkIsInputEmpty())
        	return;

		if (checkIsInputEqual()) {
			return;
		}
		
        if (start.getName().equals(mContext.getString(R.string.my_location))) {
        	start.setName(getMyLocationName(mSphinx, start.getPosition()));
        } 
        if (end.getName().equals(mContext.getString(R.string.my_location))) {
        	end.setName(getMyLocationName(mSphinx, end.getPosition()));
        }

        TrafficQuery trafficQuery = new TrafficQuery(mContext);
        
        //和产品确认所查询的换乘所属的城市是当前所设置的城市，而不是地图所在的城市
        int cityId = mMapLocationHelper.getQueryCityInfo().getId();
        
        addHistoryWord(start, HistoryWordTable.TYPE_TRAFFIC);
        addHistoryWord(end, HistoryWordTable.TYPE_TRAFFIC);
    		
        mActionLog.addAction(mActionTag +  ActionLog.TrafficTrafficBtn, getQueryType(), mStart.getText(), mEnd.getText());
        trafficQuery.setup(cityId, start, end, getQueryType(), getId(), mContext.getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(trafficQuery);
	}
    
    public void addHistoryWord(POI poi, int type) {
        if (poi == null) {
            return;
        }
        int cityId = mMapLocationHelper.getQueryCityInfo().getId();
        String name = poi.getName();
        if (name != null && name.trim().equals(mSphinx.getString(R.string.my_location)) == false) {
            Position position = poi.getPosition();
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, name, position), cityId, type);
        }
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
            
        trafficQuery.setup(Globals.getCurrentCityInfo().getId(), start, end, queryType, R.id.view_traffic_home, sphinx.getString(R.string.doing_and_wait));
        
        sphinx.queryStart(trafficQuery);
    }
    
	public static String getMyLocationName(Sphinx mSphinx, Position position) {
		String mLocationName = MapEngine.getPositionName(position);
		return mLocationName;
	}
	
	private boolean isKeyword(String input) {
		if (keywordList.contains(input)) {
			return true;
		}
		return false;
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
		if (mContext.getString(R.string.select_has_point).equals(mStart.getText())
				&& mContext.getString(R.string.select_has_point).equals(mEnd.getText())) {
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
	
	/*
     * 点击书签选择"当前位置"或"收藏夹" SELECTED
     */
    public void setData(POI poi, int index) {
    	if (index != START && index != END && index != SELECTED) 
    		return;
    	
    	setPOI(poi.clone(), index);
//    	mStateTransitionTable.event(TrafficViewSTT.Event.ClickSelectStartEndBtn);
    }
    
    //TODO:把这个函数也重写，mSettedRadioBtn不要这么用
    public void setDataNoSuggest(POI poi, int index, int queryType) {
        if (index != START && index != END && index != SELECTED) 
            return;

        if (poi.getPosition() != null) {
            mMapLocationHelper.mTargetCityInfo = MapEngine.getCityInfo(MapEngine.getCityId(poi.getPosition()));
        }
        
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
//        setState(TrafficViewSTT.State.Input);
        setPOI(poi.clone(), index);
        
//        changeToMode(TRAFFIC_MODE);
        //自定起起点功能要求不显示起点，用这个变量进行标识，onpause的时候恢复
        setShowStartMyLocation(false);
    }    
    
    /*
     * 长按地图
     */
    public void setDataForLongClick(POI poi, int index) {
//    	if (index != START && index != END && index != SELECTED) 
//    		return;
//    	
//    	setPOI(poi.clone(), index);
//    	mStateTransitionTable.event(TrafficViewSTT.Event.LongClick);
//    	/*
//    	 * 长按选点设置CheckButton
//    	 */
//    	mLogHelper.checkedRadioButton(oldCheckButton);
//    	/*
//    	 * 到这里去 NOT 线路查询
//    	 */
//    	preferTrafficThanBusline();
    }
    
    /*
     * 单击选点
     */
    //TODO:把这个函数换个名字，setdata怎么都不像要发event的
    public void setDataForSelectPoint(POI poi, int index) {
//    	mStateTransitionTable.event(TrafficViewSTT.Event.PointSelected);
    	setData(poi.clone(), index);
    }
	
    public void modifyData(POI poi, int index) {
    	if (index != START && index != END ) 
    		return;
    	if (index == START && mContext.getString(R.string.my_location).equals(mStart.getText())) {
    		LogWrapper.d("eric", "getTrafficQueryFragment modifyData mylocation skip");
    		return;
    	}
    	
    	if (index == END && mContext.getString(R.string.my_location).equals(mEnd.getText())) {
    		LogWrapper.d("eric", "getTrafficQueryFragment modifyData mylocation skip");
    		return;
    	}
    	
    	setPOI(poi.clone(), index);
    }
	
	public void onBack() {
//		if (mStateTransitionTable.event(Event.Back)) {
//			
//		} else {
			dismiss();
//		}
			clearAllText();
	}
	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			if (mStateTransitionTable.event(Event.Back)) {
//			    mActionLog.addAction(ActionLog.KeyCodeBack);
//				return true;
//			} 
//		}
//		return super.onKeyDown(keyCode, event);
//	}
	
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
            	if (trafficQuery.isPOIModified()) {
            		modifyData(trafficQuery.getStart(), TrafficQueryFragment.START);
            		modifyData(trafficQuery.getEnd(), TrafficQueryFragment.END);
            	} 
            	
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
            	    setPOI(station.toPOI(), START);
            	} else if (end) {
            		station = endStationList.get(which);
            	    setPOI(station.toPOI(), END);
            	}
                dialog.dismiss();
                if (station != null) {
                    mActionLog.addAction(ActionLog.TrafficAlternative + (start ? ActionLog.TrafficAlterStart : ActionLog.TrafficAlterEnd), which, station.getName());
                }
                if (start == false || end == false) {
                	submitTrafficQuery();
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
    

		
	/*TODO:扔到输入页去*/
	public class QueryEditText {

		protected TKEditText mEdt;

		protected POI mPOI = new POI();
		
		public TKEditText getEdt() {
			return mEdt;
		}

		public boolean isEmpty() {
			return TextUtils.isEmpty(mEdt.getText().toString());
		}
		
		public POI getPOI() {
			
			if (TextUtils.isEmpty(mEdt.getText())) {
				mPOI = new POI();
			}
			
			if (!TextUtils.isEmpty(mEdt.getText())
					&& !mEdt.getText().toString().equals(mPOI.getName())
					&& !mEdt.getText().toString().equals(mContext.getString(R.string.my_location))) {
				mPOI = new POI();
				mPOI.setName(mEdt.getText().toString().trim());
			}
			
			if (mEdt.getText().toString().equals(mContext.getString(R.string.my_location))) {
				POI location = mMapLocationHelper.getMyLocation();
				if (location != null) {
					mPOI.setName(mEdt.getText().toString());
					mPOI.setPosition(location.getPosition());
				} else {
					mSphinx.showTip(R.string.mylocation_failed, Toast.LENGTH_SHORT);
					return null;
				}
			} 
			
			return mPOI;
		}

		public void setPOI(POI mPOI) {
			this.mPOI = mPOI;
			if (this.mPOI == null) {
			    setText("");
			} else {
			    setText(mPOI.getName());
			}
		}

		protected void setText(String text) {
			mEdt.setText(text);
			mEdt.clearFocus();
		}

		public void clear() {
			setText("");
			mPOI = new POI();
		}
		
		public QueryEditText(TKEditText tkEditText) {
			mEdt = tkEditText;
		}
		
		public void setHint(String hint) {
			mEdt.setHint(hint);
		}
	}
	
    /**
     * 在交通首页时, 点击地图-定位按钮-缩放按钮, 进入交通全屏地图
     * @param isShowTip
     */
    public void onMapTouch(boolean isShowTip) {
//    	if (isShowTip && mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.Normal) {
//    		mSphinx.showTip(R.string.tip_longpress_select_traffic, Toast.LENGTH_SHORT);
//    	}
//    	mStateTransitionTable.event(TrafficViewSTT.Event.TouchMap);
    }
	
    /**
     * 地图移动时根据当前地图中心点设置当前城市名, 并设置交通查询中心点
     * @param newCenter
     */
    public void onMapCenterChanged(CityInfo cityInfo) {
//    	mMapLocationHelper.onMapCenterChanged(cityInfo);
    }
	
    /**
     * 更新"当前城市"文本
     * @param currentCity
     */
    public void setCurrentCity(String currentCity) {
//		mCityTxt.setText(mContext.getString(R.string.current_city, currentCity));
	}
    	
	public void postTask(Runnable r) {
		mSphinx.getHandler().post(r);
	}
	
}

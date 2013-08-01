package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.android.widget.TKEditText;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.map.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.model.TrafficModel.Station;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.traffic.TrafficViewSTT.Event;
import com.tigerknows.ui.traffic.TrafficViewSTT.State;
import com.tigerknows.util.Utility;
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

	public static final int TRAFFIC_MODE = 1;
	
	public static final int BUSLINE_MODE = 2;
	
	public static final int START = 1;
	
	public static final int END = 2;
	
	public static final int LINE = 3;
	
	public static final int SELECTED = 4;
	
	protected int mode = TRAFFIC_MODE;
	
	private int mSettedRadioBtn = 0;
	
	boolean showStartMyLocation = true;

	FrameLayout mTitle;
	
	Button mBackBtn;
	
	Button mTrafficQueryBtn;
	
	Button mBuslineQueryBtn;
	
	RadioGroup mRadioGroup = null;
	
	LinearLayout mBlock;
	
	Button mSelectStartBtn = null;
	
	Button mSelectEndBtn = null;
    
	QueryEditText mBusline;

	QueryEditText mStart;
    
	QueryEditText mEnd;
    
	QueryEditText mSelectedEdt;
	
    TextView mCityTxt;
    
    View mCityView;

    View mShadowWhite;
    
    LinearLayout mSuggestLnl;
    
	ListView mSuggestLsv;
	
	RelativeLayout mTrafficLayout;

	RelativeLayout mBuslineLayout;

	//TODO:有空把这个变量清掉
	int oldCheckButton;

	String[] KEYWORDS;
	
	List<String> keywordList = null;
	
	/*
	 * 控制此页面所有控件的事件
	 */
	TrafficQueryEventHelper mEventHelper;
	/*
	 * 状态转化表 
	 */
	TrafficViewSTT mStateTransitionTable;
	
	/*
	 * 动画辅助类
	 */
	TrafficQueryAnimationHelper mAnimationHelper;
	
	/*
	 * 地图中心点辅助类
	 */
	TrafficQueryMapAndLocationHelper mMapLocationHelper;
	
	/*
	 * 状态切换控制类
	 */
	TrafficQueryStateHelper mStateHelper;
	
	/*
	 * 行为日志辅助类
	 */
	TrafficQueryLogHelper mLogHelper;
	
	/*
	 * 历史词及联想词辅助类
	 */
	TrafficQuerySuggestWordHelper mSuggestWordHelper;
	
	public static final String TAG = "TrafficQueryFragment";
	
	//疑似无用
//	public CityInfo getQueryCityInfo() {
//		return mMapLocationHelper.getQueryCityInfo();
//	}
	
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
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_query_input, container, false);

        findViews();
    	
    	KEYWORDS = new String[]{
    		mContext.getString(R.string.my_location),
    		mContext.getString(R.string.select_has_point),
    	};
    	
    	keywordList = Arrays.asList(KEYWORDS);
    	
        mEventHelper = new TrafficQueryEventHelper(this);
        mAnimationHelper = new TrafficQueryAnimationHelper(this);
        mMapLocationHelper = new TrafficQueryMapAndLocationHelper(this);
        mStateHelper = new TrafficQueryStateHelper(this);
        mStateTransitionTable = new TrafficViewSTT(mStateHelper);
        mLogHelper = new TrafficQueryLogHelper(this);
        mSuggestWordHelper = new TrafficQuerySuggestWordHelper(mContext, this, mSuggestLsv);
        
        mStart.setHint(mContext.getString(R.string.start_));
        mEnd.setHint(mContext.getString(R.string.end_));
        mBusline.setHint(mContext.getString(R.string.busline_name_, ""));
        mSelectedEdt = mStart;
		
        return mRootView;
    }
	
    protected void findViews() {
    	
    	mTitle = (FrameLayout)mRootView.findViewById(R.id.title_lnl);
        mBlock = (LinearLayout)mRootView.findViewById(R.id.content_lnl);
		
		mTrafficLayout = (RelativeLayout)mRootView.findViewById(R.id.traffic_rll);
		mBuslineLayout = (RelativeLayout)mRootView.findViewById(R.id.busline_rll);
        
    	mBackBtn = (Button)mRootView.findViewById(R.id.back_btn);
    	mTrafficQueryBtn = (Button)mRootView.findViewById(R.id.traffic_query_btn);
    	mBuslineQueryBtn = (Button)mRootView.findViewById(R.id.busline_query_btn);
    	mRadioGroup = (RadioGroup)mRootView.findViewById(R.id.traffic_rgp);
    	
    	mSelectStartBtn = (Button)mRootView.findViewById(R.id.select_start_btn);
    	mSelectEndBtn = (Button)mRootView.findViewById(R.id.select_end_btn);
    	mBusline = new QueryEditText((TKEditText)mRootView.findViewById(R.id.busline_edt));
		mEnd     = new QueryEditText((TKEditText)mRootView.findViewById(R.id.end_edt));
		mStart   = new QueryEditText((TKEditText)mRootView.findViewById(R.id.start_edt));
		mCityTxt = (TextView)mRootView.findViewById(R.id.cur_city_txt);
		mCityView = (View)mRootView.findViewById(R.id.cur_city_view);
		mShadowWhite = (View)mRootView.findViewById(R.id.shadow_input);
		
		mSuggestLnl = (LinearLayout)mRootView.findViewById(R.id.suggest_lnl);
		mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
    }
	
	public POI getStartPOI() {
		return mStart.getPOI();
	}
	
	public POI getEndPOI() {
		return mEnd.getPOI();
	}

    /**
     * 退出该页面时, 发生于从交通输入页返回POI详情页时
     */
    @Override
    public void dismiss() {
    	super.dismiss();
    	mMapLocationHelper.mTargetCityInfo = null;
    	mSphinx.setTouchMode(TouchMode.NORMAL);
    	mStart.clear();
    	mEnd.clear();
    	mBusline.clear();
    	showStartMyLocation = true; 
    	mSelectedEdt = mStart;
    	
    	/*
    	 * BUG 187
    	 */   			
		if (mSphinx.uiStackPeek() == R.id.view_poi_detail) {
    		/*
    		 * 此时交通输入页已经从栈中清除, 也由于地图中心点变化通知延时性,
    		 * 此处主动更新当前城市文本
    		 */
    		CityInfo currentCity = mSphinx.getMapEngine().getCityInfo(mSphinx.getMapView().getCenterCityId());
        	if (currentCity != null) {
        		setCurrentCity(currentCity.getCName());
        	}
    	}
    }
    
    public void onPause() {
    	super.onPause();
    	mSettedRadioBtn = 0;
    	mStart.mEdt.getInput().clearFocus();
    	mEnd.mEdt.getInput().clearFocus();
    	mBusline.mEdt.getInput().clearFocus();
    	//xupeng:添加状态机的运转状态是为了防止在别的界面弹出菜单触发状态机的状态转换。
    	if (mSphinx.uiStackPeek() != R.id.view_traffic_home) {
    	    mStateTransitionTable.setRunning(false);
    	}
    	mSphinx.setTouchMode(TouchMode.NORMAL);
    }
    
	@Override
	public void onResume() {
	    TrafficViewSTT.State currentState = mStateTransitionTable.getCurrentState();
	    if (TKConfig.getPref(mSphinx, TKConfig.PREFS_HINT_LOCATION) == null
	            && currentState != TrafficViewSTT.State.Input) {
	        mSphinx.showHint(TKConfig.PREFS_HINT_LOCATION, R.layout.hint_location);
	    }
	    mStateTransitionTable.setRunning(true);
		if (currentState != TrafficViewSTT.State.SelectPoint) { // 选点操作时, 按HOME键, 再进入应用 
			super.onResume();
			hideCommonTitle();
		} else {
            if (!TextUtils.isEmpty(mActionTag)) {
                mActionLog.addAction(mActionTag);
            }
		}
		
		MapView mapView = mSphinx.getMapView();
        mapView.setStopRefreshMyLocation(false);
      
        LogWrapper.d("eric", "TrafficQueryView.show() currentState: " + currentState);

        if (currentState == TrafficViewSTT.State.Input) {
            mMenuFragment.hide();
            if (mode == TRAFFIC_MODE) {
                mSphinx.showSoftInput(mSelectedEdt.getEdt().getInput());
            } else {
                mSphinx.showSoftInput(mBusline.getEdt().getInput());
            }
            mSuggestWordHelper.refresh(mContext, mSelectedEdt.getEdt(), mode);
        } else if (currentState == TrafficViewSTT.State.Normal) {
            /*
             * 点击"收藏的点"后, 弹出的对话框, 在onPause中将mShowPromptOnLongClick设为false
             */
            mSphinx.setTouchMode(TouchMode.LONG_CLICK);
            
            mSphinx.clearMap();
            mMenuFragment.updateMenuStatus(R.id.traffic_btn);
            mMenuFragment.display();
        } else if (currentState == TrafficViewSTT.State.Map) {
        	mSphinx.setTouchMode(TouchMode.LONG_CLICK);
        } else if (currentState == TrafficViewSTT.State.SelectPoint) {
			mSphinx.setTouchMode(R.id.start_edt == mSelectedEdt.getEdt().getId() ? 
					TouchMode.CHOOSE_ROUTING_START_POINT : TouchMode.CHOOSE_ROUTING_END_POINT);
        }
        
        mMapLocationHelper.resetMapCenter();
        /*
         * 由于在一个“session”中会多次调用onresume，导致在地图选点和收藏夹选点之后返回本页面都会调用initstart
         * 这里引入mDissmissed变量，在整个页面被dismiss的时候设为true，onresume的时候把变量设为false，并把所有
         * 应该初始化的内容初始化。这个概念以后应该被扩展到其他的页面。
         */
        if (mDismissed) {
            mDismissed = false;
            initStartContent();
        }
        
	}
    
    /**
     * 在交通首页时, 点击地图-定位按钮-缩放按钮, 进入交通全屏地图
     * @param isShowTip
     */
    public void onMapTouch(boolean isShowTip) {
    	if (isShowTip && mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.Normal) {
    		mSphinx.showTip(R.string.tip_longpress_select_traffic, Toast.LENGTH_SHORT);
    	}
    	mStateTransitionTable.event(TrafficViewSTT.Event.TouchMap);
    }
	
    /**
     * 地图移动时根据当前地图中心点设置当前城市名, 并设置交通查询中心点
     * @param newCenter
     */
    public void onMapCenterChanged(CityInfo cityInfo) {
    	mMapLocationHelper.onMapCenterChanged(cityInfo);
    }
	
    /**
     * 更新"当前城市"文本
     * @param currentCity
     */
    public void setCurrentCity(String currentCity) {
		mCityTxt.setText(mContext.getString(R.string.current_city, currentCity));
	}
    
	public void setState(TrafficViewSTT.State state) {
		mStateTransitionTable.resetInitState(state);
		/*
		 * No state transition.
		 */
		switch(state) {
		case Normal:
			mEventHelper.applyListenersInNormalState();
			break;
		case Input:
			mEventHelper.applyListenersInInputState();
			break;
		default:
		}
		
		mStateHelper.applyInnateProperty(mStateTransitionTable.getCurrentState());
		checkQueryState();
//		mStateTransitionTable.clearTransitionStack();
	}
	
	public void postTask(Runnable r) {
		mSphinx.getHandler().post(r);
	}
	
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
	
	public void clearAllText() {
		mStart.clear();
		mEnd.clear();
		mBusline.clear();
	}
	
	public void setShowStartMyLocation(boolean showMyLocation) {
	    this.showStartMyLocation = showMyLocation;
	}
	
	public void initStartContent() {

	    if (showStartMyLocation) {
	        //如果要求显示起点，定位为设定城市，在起点框填写上当前位置。
	        if (mStart.isEmpty() && mMapLocationHelper.isMyLocationLocateCurrentCity()) {
	            POI poi = mMapLocationHelper.getMyLocation();
	            setPOI(poi, START);
	        }
	        //否则什么都不做，不影响当前起点框内容
	    } else {
	        //否则清空起点框的内容。
	        mStart.clear();
	    }
		
		//TODO:这些东西从这里抽出去
		if (mStateTransitionTable.getCurrentState() == State.Input) {
		    mSelectedEdt.mEdt.requestFocus();
		} else {
		    mSelectedEdt.mEdt.clearFocus();
		}
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
			mSelectedEdt.setPOI(poi);
		default:
			
		}
	}

	public boolean isEditTextEmpty(TKEditText edittext) {
		return TextUtils.isEmpty(edittext.getText());
	}
	
	public View getContentView() {
		if (mode == TRAFFIC_MODE) 
			return mTrafficLayout;
		else 
			return mBuslineLayout;
	}
	
	/**
	 * 在"公交换乘"和"线路查询"间切换
	 * @param newMode
	 */
	public void changeToMode(int newMode) {
		mode = newMode;
		if (mode == TRAFFIC_MODE) {
			mTrafficLayout.setVisibility(View.VISIBLE);
			mBuslineLayout.setVisibility(View.GONE);
			mSelectedEdt = isEditTextEmpty(mStart.getEdt()) ? mStart : mEnd;
			mSuggestWordHelper.refresh(mSphinx, mSelectedEdt.mEdt, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC);
		} else {
			mTrafficLayout.setVisibility(View.GONE);
			mBuslineLayout.setVisibility(View.VISIBLE);
			mSelectedEdt = mBusline;
			mSuggestWordHelper.refresh(mSphinx, mSelectedEdt.mEdt, TrafficQuerySuggestWordHelper.TYPE_BUSLINE);
		}
		if (mStateTransitionTable.getCurrentState() == State.Input) {
		    mSelectedEdt.mEdt.requestFocus();
		} else {
		    mSelectedEdt.mEdt.clearFocus();
		}
	}
	
	public void checkQueryState() {
		
		if (mode == TRAFFIC_MODE) {
			if(!isEditTextEmpty(mStart.getEdt()) && !isEditTextEmpty(mEnd.getEdt())) {
				enableQueryBtn(mTrafficQueryBtn, true);
	        }else{
	        	enableQueryBtn(mTrafficQueryBtn, false);
	        }
		} else {
			if(!isEditTextEmpty(mBusline.getEdt())){
				enableQueryBtn(mBuslineQueryBtn, true);
	        }else{
	        	enableQueryBtn(mBuslineQueryBtn, false);
	        }
		}
    }
	
	public void enableQueryBtn(Button mButton, boolean enable) {
		mButton.setEnabled(enable);
	}
	
	public void query() {
//		mSphinx.hideSoftInput();
		
		if (mode == TRAFFIC_MODE) {
			submitTrafficQuery();
		} else {
			submitBuslineQuery();
		}

	}
	
	public void submitBuslineQuery() {
		
		String searchword = mBusline.getEdt().getText().toString().trim();
		if (TextUtils.isEmpty(searchword)){
			mSphinx.showTip(R.string.input_busline_name_tip, Toast.LENGTH_SHORT);
			return;
		}

		int cityId = mMapLocationHelper.getQueryCityInfo().getId();
//        int cityId = Globals.getCurrentCityInfo().getId();
        addHistoryWord(mBusline, HistoryWordTable.TYPE_BUSLINE);
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(cityId, searchword, 0, false, getId(), mContext.getString(R.string.doing_and_wait));
        
        mActionLog.addAction(mActionTag +  ActionLog.TrafficBuslineBtn, searchword);
        mSphinx.queryStart(buslineQuery);
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
//        int cityId = Globals.getCurrentCityInfo().getId();
        
        addHistoryWord(mStart, HistoryWordTable.TYPE_TRAFFIC);
        addHistoryWord(mEnd, HistoryWordTable.TYPE_TRAFFIC);
    		
        mActionLog.addAction(mActionTag +  ActionLog.TrafficTrafficBtn, getQueryType(), mStart.getEdt().getText().toString(), mEnd.getEdt().getText().toString());
        trafficQuery.setup(cityId, start, end, getQueryType(), getId(), mContext.getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(trafficQuery);
	}
	
	private void addHistoryWord(QueryEditText queryEditText, int type) {
	    if (!isKeyword(queryEditText.getEdt().getText().toString())) {
            POI poi = queryEditText.getPOI();
            addHistoryWord(poi, type);
        }
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
		String mLocationName = mSphinx.getMapEngine().getPositionName(position);
		
		if (!TextUtils.isEmpty(mLocationName)) {
			return mLocationName.substring(1);
		}
		
		return mLocationName;
	}
	
	private boolean isKeyword(String input) {
		if (keywordList.contains(input)) {
			return true;
		}
		return false;
	}
	
	private boolean checkIsInputEmpty() {
		
		String start = mStart.getEdt().getText().toString().trim();
		String end = mEnd.getEdt().getText().toString().trim();
		
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
		if (mStart.getEdt().getText().toString().equals(mContext.getString(R.string.select_has_point))
				&& mEnd.getEdt().getText().toString().equals(mContext.getString(R.string.select_has_point))) {
			if (POI.isPositionEqual(mStart.getPOI(), mEnd.getPOI())) {
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
		if (POI.isNameEqual(mStart.getPOI(), mEnd.getPOI())) {
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
	
	//1"POI详情-到这里去"默认显示"当前位置"
//    public void setData(POI poi) {
//    	mMapLocationHelper.showNormalStateMap();
//    	setState(TrafficViewSTT.State.Input);
//    	mEventHelper.clearSuggestWatcherInInputState();
//    	clearAllText();
//    	initStartContent();
//    	setPOI(poi.clone(), END);
//    	mSelectedEdt = mEnd;
//    	preferTrafficThanBusline();
//    	mEventHelper.addSuggestWatcherInInputState();
//    }	
	
	/*
     * 点击书签选择"当前位置"或"收藏夹" SELECTED
     */
    public void setData(POI poi, int index) {
    	if (index != START && index != END && index != SELECTED) 
    		return;
    	
    	setPOI(poi.clone(), index);
    	mStateTransitionTable.event(TrafficViewSTT.Event.ClickSelectStartEndBtn);
    }
    
    //TODO:把这个函数也重写，mSettedRadioBtn不要这么用
    public void setDataNoSuggest(POI poi, int index, int queryType) {
        if (index != START && index != END && index != SELECTED) 
            return;

        if (poi.getPosition() != null) {
            MapEngine mapEngine = MapEngine.getInstance();
            mMapLocationHelper.mTargetCityInfo = mapEngine.getCityInfo(mapEngine.getCityId(poi.getPosition()));
        }
        
        changeToMode(TRAFFIC_MODE);
        //自定起起点功能要求不显示起点，用这个变量进行标识，onpause的时候恢复
        setShowStartMyLocation(false);
        
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
        setState(TrafficViewSTT.State.Input);
        setPOI(poi.clone(), index);
    }    
    
    /*
     * 长按地图
     */
    public void setDataForLongClick(POI poi, int index) {
    	if (index != START && index != END && index != SELECTED) 
    		return;
    	
    	setPOI(poi.clone(), index);
    	mStateTransitionTable.event(TrafficViewSTT.Event.LongClick);
    	/*
    	 * 长按选点设置CheckButton
    	 */
    	mLogHelper.checkedRadioButton(oldCheckButton);
    	/*
    	 * 到这里去 NOT 线路查询
    	 */
    	preferTrafficThanBusline();
    }
    
    /*
     * 单击选点
     */
    //TODO:把这个函数换个名字，setdata怎么都不像要发event的
    public void setDataForSelectPoint(POI poi, int index) {
    	mStateTransitionTable.event(TrafficViewSTT.Event.PointSelected);
    	setData(poi.clone(), index);
//    	mStateTransitionTable.mergeFirstTwoTranstion(TrafficViewSTT.Event.ClickSelectStartEndBtn, mStateHelper.createNormalToInputAction());
    }
	
    public void modifyData(POI poi, int index) {
    	if (index != START && index != END ) 
    		return;
    	if (index == START && mStart.getEdt().getText().toString().equals(mContext.getString(R.string.my_location))) {
    		LogWrapper.d("eric", "getTrafficQueryFragment modifyData mylocation skip");
    		return;
    	}
    	
    	if (index == END && mEnd.getEdt().getText().toString().equals(mContext.getString(R.string.my_location))) {
    		LogWrapper.d("eric", "getTrafficQueryFragment modifyData mylocation skip");
    		return;
    	}
    	
    	setPOI(poi.clone(), index);
    }
	
	public void onBack() {
		if (mStateTransitionTable.event(Event.Back)) {
			
		} else {
			dismiss();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mStateTransitionTable.event(Event.Back)) {
			    mActionLog.addAction(ActionLog.KeyCodeBack);
				return true;
			} 
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void preferTrafficThanBusline() {
		if (mRadioGroup.getCheckedRadioButtonId() == R.id.traffic_busline_rbt) {
//			mRadioGroup.check(R.id.traffic_transfer_rbt);
			mLogHelper.checkedRadioButton(R.id.traffic_transfer_rbt);
		}
	}
	
	public void hideCommonTitle() {
		/*
		 * 在结果地图时, 若地图卡住了(比如多次放大缩小时), 此时点数次返回键, 
		 * 会多次dismiss页面, 使得栈被清空. 
		 * 在BaseFragment中加了对只有左上角返回按钮可见时才响应点击事件, 
		 * 避开了这个问题.
		 */
        mLeftBtn.setVisibility(View.INVISIBLE);
		mTitleFragment.hide();
	}
	
	public void displayCommonTitle() {
        mLeftBtn.setVisibility(View.VISIBLE);
		mTitleFragment.display();
	}
	
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
		if (BaseQuery.API_TYPE_TRAFFIC_QUERY.equals(baseQuery.getAPIType())) {
            this.queryTrafficEnd((TrafficQuery)baseQuery);
        } else if (BaseQuery.API_TYPE_BUSLINE_QUERY.equals(baseQuery.getAPIType())) {
            this.queryBuslineEnd((BuslineQuery)baseQuery);
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
        ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);
        
        mActionLog.addAction(ActionLog.TrafficAlternative);
        final Dialog dialog = Utility.showNormalDialog(mSphinx,
                mSphinx.getString(start ? R.string.select_start_station : R.string.select_end_station),
                listView);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
            	Station station = null;
            	if (start) {
            	    station = startStationList.get(which);
            	    mStart.setPOI(station.toPOI());
            	} else if (end) {
            		station = endStationList.get(which);
            		mEnd.setPOI(station.toPOI());
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
                int cityId = mSphinx.getMapEngine().getCityId(start);
                isSameCity &= (cityId == mSphinx.getMapEngine().getCityId(end));
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

    public void TrafficOnCityChanged(Sphinx sphinx, int cityId) {
        MapEngine.getInstance().suggestwordCheck(sphinx, cityId);
        HistoryWordTable.readHistoryWord(sphinx, cityId, HistoryWordTable.TYPE_TRAFFIC);
        HistoryWordTable.readHistoryWord(sphinx, cityId, HistoryWordTable.TYPE_BUSLINE);
    }
}
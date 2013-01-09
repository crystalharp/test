package com.tigerknows.view;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BuslineModel;
import com.tigerknows.model.BuslineQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficQuery;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.util.TKAsyncTask;

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

	boolean modeChange = false;
	
	FrameLayout mTitle;
	
	ImageButton mBackBtn;
	
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

    LinearLayout mSuggestLnl;
    
	ListView mSuggestLsv;
	
	RelativeLayout mTrafficLayout;

	RelativeLayout mBuslineLayout;

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
	TrafficQuerySuggestHistoryHelper mSuggestHistoryHelper;
	
	public static final String TAG = "TrafficQueryFragment";
	
	public CityInfo getQueryCityInfo() {
		return mMapLocationHelper.getQueryCityInfo();
	}
	
	public TrafficQueryFragment(Sphinx sphinx) {
        super(sphinx);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mSuggestHistoryHelper = new TrafficQuerySuggestHistoryHelper(mContext, this, mSuggestLsv);
        
        mStart.init(START, mContext.getString(R.string.start_));
        mEnd.init(END, mContext.getString(R.string.end_));
        mBusline.init(LINE, mContext.getString(R.string.busline_name_, ""));
        
        mBlock.setFocusable(true);
		mBlock.setFocusableInTouchMode(true);
		mBlock.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		
		mMapLocationHelper.getCurrentMapInfo();
        
        return mRootView;
    }
	
    protected void findViews() {
    	
    	mTitle = (FrameLayout)mRootView.findViewById(R.id.title_lnl);
        mBlock = (LinearLayout)mRootView.findViewById(R.id.content_lnl);
		
		mTrafficLayout = (RelativeLayout)mRootView.findViewById(R.id.traffic_rll);
		mBuslineLayout = (RelativeLayout)mRootView.findViewById(R.id.busline_rll);
        
    	mBackBtn = (ImageButton)mRootView.findViewById(R.id.back_btn);
    	mTrafficQueryBtn = (Button)mRootView.findViewById(R.id.traffic_query_btn);
    	mBuslineQueryBtn = (Button)mRootView.findViewById(R.id.busline_query_btn);
    	mRadioGroup = (RadioGroup)mRootView.findViewById(R.id.traffic_rgp);
    	
    	//mExchangeBtn = (Button)mRootView.findViewById(R.id.exchange_btn);
    	mSelectStartBtn = (Button)mRootView.findViewById(R.id.select_start_btn);
    	mSelectEndBtn = (Button)mRootView.findViewById(R.id.select_end_btn);
    	mBusline = new QueryEditText((RelativeLayout)mRootView.findViewById(R.id.busline_edt));
		mBusline.setOnlyInput();
		mEnd     = new QueryEditText((RelativeLayout)mRootView.findViewById(R.id.end_line));
		mStart   = new QueryEditText((RelativeLayout)mRootView.findViewById(R.id.start_line));
		mCityTxt = (TextView)mRootView.findViewById(R.id.cur_city_txt);
		
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
    	mSphinx.hideSoftInput(mBackBtn.getWindowToken());
    	mSphinx.setTouchMode(TouchMode.NORMAL);
    	
    	/*
    	 * BUG 187
    	 */   			
		if (mSphinx.uiStackPeek() == R.id.view_poi_detail) {
    		mMapLocationHelper.showNormalStateMap();
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
    	mSphinx.setTouchMode(TouchMode.NORMAL);
    }
    
	@Override
	public void onResume() {
		if (mStateTransitionTable.getCurrentState() != TrafficViewSTT.State.SelectPoint) { // 选点操作时, 按HOME键, 再进入应用 
			super.onResume();
			hideCommonTitle();
		}
      
        LogWrapper.d("eric", "TrafficQueryView.show() currentState: " + mStateTransitionTable.getCurrentState());

        if (mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.Input) {
        	mMapLocationHelper.resetMapStateMap();
            mMenuFragment.hide();
        }
        if (mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.Normal) {
        	mMapLocationHelper.centerOnMyLocation();
        	mMapLocationHelper.showNormalStateMap();
            /*
             * 点击"收藏的点"后, 弹出的对话框, 在onPause中将mShowPromptOnLongClick设为false
             */
            mSphinx.setTouchMode(TouchMode.LONG_CLICK);
            
            mSphinx.clearMap();
    		mMenuFragment.updateMenuStatus(R.id.traffic_btn);
            mMenuFragment.display();
        }
        
        if (mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.Map) {
        	mSphinx.setTouchMode(TouchMode.LONG_CLICK);
        }
        
        if (mStateTransitionTable.getCurrentState() == TrafficViewSTT.State.SelectPoint) {
			mSphinx.setTouchMode(TrafficQueryFragment.START == mSelectedEdt.getPosition() ? 
					TouchMode.CHOOSE_ROUTING_START_POINT : TouchMode.CHOOSE_ROUTING_END_POINT);
        }
	}

	
	/**
     * 切换城市时设置当前城市名
     */
    public void refreshCity(String city) {
    	mMapLocationHelper.getCurrentMapInfo();
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
		mStateTransitionTable.setCurrentState(state);
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
		mStateTransitionTable.clearTransitionStack();
	}
	
	public void postTask(Runnable r) {
		mSphinx.getHandler().post(r);
	}
	
	public class QueryEditText {

//		protected ImageView mLeftImg;
		
		protected ImageView mRightImg;
		
		protected EditText mEdt;

		protected POI mPOI = new POI();
		
		protected int mPosition;		
		
		public ImageView getRightImg() {
			return mRightImg;
		}

		public EditText getEdt() {
			return mEdt;
		}

		public int getPosition() {
			return mPosition;
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
			
//			Position position;
//			position = new Position(1,1);
//			mPOI.setPosition(position);
			//xupeng：想在这里添加个如果有查找过的poi信息则统一填上坐标点
			
			return mPOI;
		}

		public void setPOI(POI mPOI) {
			this.mPOI = mPOI;
			setText(mPOI.getName());
		}

		protected void setText(String text) {
			mEdt.setText(text);
			mEdt.clearFocus();
		}

		public void clear() {
			setText("");
			mPOI = new POI();
		}
		
		public QueryEditText(RelativeLayout queryline) {
			super();
			// TODO Auto-generated constructor stub
			
			findView(queryline);
		}
		
		public void init(int position, String hint) {
			mPosition = position;
			mEdt.setHint(hint);
//			if (mPosition == START) {
//				this.mLeftImg.setBackgroundResource(R.drawable.icon_start_point);
//			} else {
//				this.mLeftImg.setBackgroundResource(R.drawable.icon_end_point);
//			}
			mRightImg.setBackgroundResource(R.drawable.btn_bookmark);
		}
		
		protected void findView(RelativeLayout queryline) {
//			mLeftImg = (ImageView)queryline.findViewById(R.id.left_img);
			mRightImg = (ImageView)queryline.findViewById(R.id.right_img);
			mEdt = (EditText)queryline.findViewById(R.id.address_edt);
		}
		

		public void setOnlyInput() {
//			mLeftImg.setVisibility(View.GONE);
			mRightImg.setVisibility(View.GONE);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mEdt.getLayoutParams();
			lp.setMargins(Util.dip2px(Globals.g_metrics.density, 5), 0, Util.dip2px(Globals.g_metrics.density, 5), 0);
		}
	}
	
	public void clearAllText() {
		mStart.clear();
		mEnd.clear();
		mBusline.clear();
	}
	
	public void showStart() {

		//定位为设定城市，或者自驾模式下定位城市与设定城市不同，均在起点框填写上当前位置。
		if (mMapLocationHelper.isMyLocationLocateCurrentCity() || 
				(getQueryType() == TrafficQuery.QUERY_TYPE_DRIVE && Globals.g_My_Location_City_Info != null)) {
			POI location = mMapLocationHelper.getMyLocation();
			setPOI(location, START);
		} else {
			mStart.clear();
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

	private boolean isEditTextEmpty(EditText edittext) {
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
		if (mode == newMode) {
			modeChange = false;
			return;
		}
		modeChange = true;
		mode = newMode;
		if (mode == TRAFFIC_MODE) {
			mTrafficLayout.setVisibility(View.VISIBLE);
			mBuslineLayout.setVisibility(View.GONE);
		} else {
			mTrafficLayout.setVisibility(View.GONE);
			mBuslineLayout.setVisibility(View.VISIBLE);
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
		if (enable) {
			mButton.setTextColor(0xFFFFFFFF);
			mButton.setEnabled(true);
		} else {
			mButton.setTextColor(0xFFEFBA82);
			mButton.setEnabled(false);
		}
	}
	
	public void query() {
		mSphinx.hideSoftInput(mTrafficQueryBtn.getWindowToken());
		mBlock.requestFocus();
		
		if (mode == TRAFFIC_MODE) {
			submitTrafficQuery();
		} else {
			submitBuslineQuery();
		}

	}
	
	private void submitBuslineQuery() {
		
		String searchword = mBusline.getEdt().getText().toString().trim();
		if (TextUtils.isEmpty(searchword)){
			mSphinx.showTip(R.string.input_busline_name_tip, Toast.LENGTH_SHORT);
			return;
		}

		int cityId = mMapLocationHelper.getQueryCityInfo().getId();
		if (!isKeyword(searchword)) {
		    HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, searchword), cityId, HistoryWordTable.TYPE_BUSLINE);
		}
        BuslineQuery buslineQuery = new BuslineQuery(mContext);
        buslineQuery.setup(cityId, searchword, 0, false, getId(), mContext.getString(R.string.doing_and_wait));
        
        mActionLog.addAction(ActionLog.TrafficQueryBtnB, searchword);
        mSphinx.queryStart(buslineQuery);
    }
	
	public void submitTrafficQuery() {
		
		//xupeng:预计在这里修改没有坐标点的问题。
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
        	start.setName(getMyLocationName(start.getPosition()));
        } 
        if (end.getName().equals(mContext.getString(R.string.my_location))) {
        	end.setName(getMyLocationName(end.getPosition()));
        }

        TrafficQuery trafficQuery = new TrafficQuery(mContext);
        
        int cityId = mMapLocationHelper.getQueryCityInfo().getId();
        
        //xupeng:怎么算是history word？搜索北大，实际用北京大学搜索，哪个是？
        if (!isKeyword(mStart.getEdt().getText().toString())) {
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, mStart.getEdt().getText().toString()), cityId, HistoryWordTable.TYPE_TRAFFIC);
        }

        if (!isKeyword(mEnd.getEdt().getText().toString())) {
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, mEnd.getEdt().getText().toString()), cityId, HistoryWordTable.TYPE_TRAFFIC);
        }
    		
        mActionLog.addAction(ActionLog.TrafficQueryBtnT, mStart.getEdt().getText().toString(), mEnd.getEdt().getText().toString());
        trafficQuery.setup(cityId, start, end, getQueryType(), getId(), mContext.getString(R.string.doing_and_wait));
        
        mSphinx.queryStart(trafficQuery);
	}
	
	private String getMyLocationName(Position position) {
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
		
		if (TextUtils.isEmpty(start) && TextUtils.isEmpty(end) ) {
            mSphinx.showTip(R.string.forget_all_tip, Toast.LENGTH_SHORT);
            return true;
        } else if (TextUtils.isEmpty(start)) {
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
    public void setData(POI poi) {
    	mMapLocationHelper.showNormalStateMap();
    	setState(TrafficViewSTT.State.Input);
    	mEventHelper.clearSuggestWatcherInInputState();
    	clearAllText();
    	showStart();
    	setPOI(poi.clone(), END);
    	mSelectedEdt = mEnd;
    	preferTrafficThanBusline();
    	mEventHelper.addSuggestWatcherInInputState();
    }	
	
	/*
     * 点击书签选择"当前位置"或"收藏夹" SELECTED
     */
    public void setData(POI poi, int index) {
    	if (index != START && index != END && index != SELECTED) 
    		return;
    	
    	setPOI(poi.clone(), index);
    	mStateTransitionTable.event(TrafficViewSTT.Event.ClickBookmark);
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
    public void setDataForSelectPoint(POI poi, int index) {
    	mLogHelper.logForSelectPoint(index);
    	mStateTransitionTable.event(TrafficViewSTT.Event.SelectPoint);
    	setData(poi.clone(), index);
    	mStateTransitionTable.mergeFirstTwoTranstion(TrafficViewSTT.Event.ClickBookmark, mStateHelper.createNormalToInputAction());
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
		if (mStateTransitionTable.rollback()) {
			
		} else {
			dismiss();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		// FIXME 这里进行键盘功能变更
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mStateTransitionTable.rollback()) {
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
		// TODO Auto-generated method stub
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
            mActionLog.addAction(ActionLog.TrafficQueryNoResultT);
            showTrafficErrorTip(trafficQuery);
        } else if (trafficModel.getType() == TrafficModel.TYPE_EMPTY) {
        	mActionLog.addAction(ActionLog.TrafficQueryNoResultT);
            showTrafficErrorTip(trafficQuery);
        } else if (trafficModel.getType() == TrafficModel.TYPE_ALTERNATIVES 
        		|| trafficModel.getType() == TrafficModel.TYPE_PROJECT){
            if (trafficModel.getType() == TrafficModel.TYPE_ALTERNATIVES) {
                mSphinx.getTrafficAlternativesDialog().setData(trafficQuery, this);
            } else if (trafficModel.getPlanList() == null || trafficModel.getPlanList().size() <= 0){
            	mActionLog.addAction(ActionLog.TrafficQueryNoResultT);
            	showTrafficErrorTip(trafficQuery);
            } else if (trafficModel.getType() == TrafficModel.TYPE_PROJECT) {
            	mActionLog.addAction(ActionLog.TrafficQueryResultT, trafficModel.getPlanList().size());
            	// 若之前发出的请求中的起终点被服务器修改, 此处要修改客户端显示
            	if (trafficQuery.isPOIModified()) {
            		modifyData(trafficQuery.getStart(), TrafficQueryFragment.START);
            		modifyData(trafficQuery.getEnd(), TrafficQueryFragment.END);
            	} 
            	
            	if (trafficModel.getPlanList().get(0).getType() == Plan.Step.TYPE_TRANSFER) {
            		mSphinx.getTrafficResultFragment().setData(trafficQuery);
            		mSphinx.showView(R.id.view_traffic_result_transfer);
            	} else {
            		//xupeng:如果不是换乘，这里判断了查询类型，切换到了detail页面
            		mSphinx.getTrafficDetailFragment().setData(trafficModel.getPlanList().get(0));
            		mSphinx.getTrafficDetailFragment().viewMap();
            	}
            }
        } 
    }

    public void queryBuslineEnd(BuslineQuery buslineQuery) {
    	
        BuslineModel buslineModel = buslineQuery.getBuslineModel();
        
        if (buslineModel == null) {
        	mActionLog.addAction(ActionLog.TrafficQueryNoResultB);
        	if (buslineQuery.getStatusCode() == BaseQuery.STATUS_CODE_NONE) {
        		mSphinx.showTip(R.string.network_failed, Toast.LENGTH_SHORT);
        	} else {
        		mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        	}
        } else if (buslineModel.getType() == BuslineModel.TYPE_EMPTY) {
        	mActionLog.addAction(ActionLog.TrafficQueryNoResultB);
        	mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        } else if (buslineModel.getType() == BuslineModel.TYPE_UNSUPPORT) {
        	mSphinx.showTip(R.string.busline_not_support, Toast.LENGTH_SHORT);
        } else if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE 
        		|| buslineModel.getType() == BuslineModel.TYPE_STATION){
        	if (((buslineModel.getLineList() == null || buslineModel.getLineList().size() <= 0) && 
            (buslineModel.getStationList() == null || buslineModel.getStationList().size() <= 0))) {
        		mActionLog.addAction(ActionLog.TrafficQueryNoResultB);
        		mSphinx.showTip(R.string.busline_non_tip, Toast.LENGTH_SHORT);
        	} else {
        		if (buslineModel.getType() == BuslineModel.TYPE_BUSLINE) {
        			mActionLog.addAction(ActionLog.TrafficQueryResultB, buslineModel.getLineList().size());
        			mSphinx.getBuslineResultLineFragment().setData(buslineQuery);
        			mSphinx.showView(R.id.view_busline_line_result);
        		} else if (buslineModel.getType() == BuslineModel.TYPE_STATION) {
        			mActionLog.addAction(ActionLog.TrafficQueryResultB, buslineModel.getStationList().size());
        			mSphinx.getBuslineResultStationFragment().setData(buslineQuery);
        			mSphinx.showView(R.id.view_busline_station_result);
        		}        		
        	}
        }
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
}
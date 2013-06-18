package com.tigerknows.ui.traffic;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import android.widget.Toast;

import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.PinOverlayHelper;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.traffic.TrafficQueryFragment.QueryEditText;
import com.tigerknows.ui.traffic.TrafficViewSTT.Event;
import com.tigerknows.ui.traffic.TrafficViewSTT.State;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;
import com.tigerknows.widget.SuggestArrayAdapter;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控件事件处理]]
 * @author linqingzu
 * TODO:重新整理一下所有的listener
 *   RadioButton的触发不再用onCheckedChangedListener,改为使用OnClickedListener.
 * 使用RadioGroup的CheckedChanged触发会导致一些不期望的触发,比如进入到地图模式时
 * 取消上面button被check的状态
 */
public class TrafficQueryEventHelper {
	
	public static final String TAG = "TrafficQueryEventHelper";
	
	private TrafficQueryFragment mQueryFragment;
	
	InputEditTextSuggestWordTextWatcher startSuggestWatcher;
	InputEditTextSuggestWordTextWatcher endSuggestWatcher;
	InputEditTextSuggestWordTextWatcher buslineSuggestWatcher;
	OnClickListener mNormalRadioOnClickedListener;
	OnClickListener mInputRadioOnClickedListener;
	OnClickListener mMapRadioOnClickedListener; 
	
	public TrafficQueryEventHelper(TrafficQueryFragment queryFragment) {
		this.mQueryFragment = queryFragment;
		startSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mStart, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC);
		endSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mEnd, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC);
		buslineSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mBusline, TrafficQuerySuggestWordHelper.TYPE_BUSLINE);
        mNormalRadioOnClickedListener = new NormalRadioOnClickedListener();
		mInputRadioOnClickedListener = new InputRadioOnClickedListener();
		mMapRadioOnClickedListener = new MapRadioOnClickedListener();
		
		mQueryFragment.mStart.getEdt().setOnFocusChangeListener(new TrafficEditFocusListener(mQueryFragment.mStart, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC));
		mQueryFragment.mEnd.getEdt().setOnFocusChangeListener(new TrafficEditFocusListener(mQueryFragment.mEnd, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC));
		mQueryFragment.mBusline.getEdt().setOnFocusChangeListener(new TrafficEditFocusListener(mQueryFragment.mBusline, TrafficQuerySuggestWordHelper.TYPE_BUSLINE));
//	}
	
	/*
	 * Do not remove common Listeners
	 */
//	public void clearListenersFromTargets() {
//		mQueryFragment.mStart.getEdt().setOnClickListener(null);
//		mQueryFragment.mEnd.getEdt().setOnClickListener(null);
//		mQueryFragment.mBusline.getEdt().setOnClickListener(null);
//		
//		mQueryFragment.mStart.getEdt().setOnTouchListener(null);
//		mQueryFragment.mEnd.getEdt().setOnTouchListener(null);
//		mQueryFragment.mBusline.getEdt().setOnTouchListener(null);
//		
//		clearSuggestWatcherInInputState();
//		
//		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(null);
//		mQueryFragment.mSelectStartBtn.setOnClickListener(null);
//		mQueryFragment.mSelectEndBtn.setOnClickListener(null);
//		mQueryFragment.mBackBtn.setOnClickListener(null);
//		mQueryFragment.mTrafficQueryBtn.setOnClickListener(null);
//		mQueryFragment.mBuslineQueryBtn.setOnClickListener(null);
//		mQueryFragment.mSuggestLsv.setOnItemClickListener(null);
//		mQueryFragment.mSuggestLsv.setOnTouchListener(null);
//		mQueryFragment.mSuggestLnl.setOnTouchListener(null);
//		mQueryFragment.mRootView.setOnTouchListener(null);
//	}
		
//  public void addSuggestWatcherInInputState() {
        mQueryFragment.mStart.getEdt().addTextChangedListener(startSuggestWatcher);
        mQueryFragment.mEnd.getEdt().addTextChangedListener(endSuggestWatcher);
        mQueryFragment.mBusline.getEdt().addTextChangedListener(buslineSuggestWatcher);
//  }
	
//	public void applyCommonListeners() {
		mQueryFragment.mStart.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mStart));
		mQueryFragment.mEnd.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mEnd));
		mQueryFragment.mBusline.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mBusline));
		
		mQueryFragment.mStart.getEdt().setOnEditorActionListener(new StartEndEdtClickListener());
		mQueryFragment.mEnd.getEdt().setOnEditorActionListener(new StartEndEdtClickListener());
		mQueryFragment.mBusline.getEdt().setOnEditorActionListener(new EditorActionListener(mQueryFragment.mBusline));

//		mQueryFragment.mRootView.setOnTouchListener(new RootViewTouchListener());
		
		mQueryFragment.mStart.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mStart, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC));
		mQueryFragment.mEnd.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mEnd, TrafficQuerySuggestWordHelper.TYPE_TRAFFIC));
		mQueryFragment.mBusline.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mBusline, TrafficQuerySuggestWordHelper.TYPE_BUSLINE));
		mQueryFragment.mSelectStartBtn.setOnClickListener(new SelectStartEndBtnClickListener(mQueryFragment.mStart));
		mQueryFragment.mSelectEndBtn.setOnClickListener(new SelectStartEndBtnClickListener(mQueryFragment.mEnd));
	}
	
	public void applyListenersInNormalState() {
		LogWrapper.d("eric", "applyListenersInNormalState");
		
//		clearListenersFromTargets();
//		applyCommonListeners();
		
		for (int i = 0; i < mQueryFragment.mRadioGroup.getChildCount(); i++) {
		    mQueryFragment.mRadioGroup.getChildAt(i).setOnClickListener(mNormalRadioOnClickedListener);
		}
	}
//	
	public void applyListenersInInputState() {
		LogWrapper.d("eric", "applyListenersInInputState");
		
//		clearListenersFromTargets();
//		applyCommonListeners();
		
		mQueryFragment.mBackBtn.setOnClickListener(new InputBackClickListener());
		mQueryFragment.mTrafficQueryBtn.setOnClickListener(new InputQueryClickListener());
		mQueryFragment.mBuslineQueryBtn.setOnClickListener(new InputQueryClickListener());
		mQueryFragment.mSuggestLsv.setOnItemClickListener(new InputSuggestOnItemClickListener());
		mQueryFragment.mSuggestLsv.setOnTouchListener(new InputSuggestOnTouchListener());
		mQueryFragment.mSuggestLnl.setOnTouchListener(new InputSuggestOnTouchListener());
		for (int i = 0; i < mQueryFragment.mRadioGroup.getChildCount(); i++) {
		    mQueryFragment.mRadioGroup.getChildAt(i).setOnClickListener(mInputRadioOnClickedListener);
		}
		
//		addSuggestWatcherInInputState();
	}
	
	public void applyListenersInMapState() {
		LogWrapper.d("eric", "applyListenersInMapState");
		
//		clearListenersFromTargets();
//		applyCommonListeners();
		
		mQueryFragment.mBackBtn.setOnClickListener(new MapBackClickListener());
		for (int i = 0; i < mQueryFragment.mRadioGroup.getChildCount(); i++) {
		    mQueryFragment.mRadioGroup.getChildAt(i).setOnClickListener(mMapRadioOnClickedListener);
		}
//		mQueryFragment.mRootView.setOnTouchListener(new RootViewTouchListener());
	}
//	
	public void applyListenersInSelectPointState() {
		mQueryFragment.mLeftBtn.setOnClickListener(new SelectPointLeftBtnOnClickListener());
	}
//	
//	public void clearSuggestWatcherInInputState() {
//		mQueryFragment.mStart.getEdt().removeTextChangedListener(startSuggestWatcher);
//		mQueryFragment.mEnd.getEdt().removeTextChangedListener(endSuggestWatcher);
//		mQueryFragment.mBusline.getEdt().removeTextChangedListener(buslineSuggestWatcher);
//		//清除显示的历史词和建议词
//		mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mSphinx, null, 0);
//	}
	
	protected class StartEndEdtClickListener implements OnEditorActionListener {
		
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_NEXT
                    || actionId == EditorInfo.IME_ACTION_SEARCH 
			        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
				if (!mQueryFragment.mStart.isEmpty() && !mQueryFragment.mEnd.isEmpty()) {
					//都有内容，搜索
					mQueryFragment.query();
					return true;
				} else if (mQueryFragment.mSelectedEdt == mQueryFragment.mStart) {
				    mQueryFragment.mSelectedEdt = mQueryFragment.mEnd;
				    mQueryFragment.mEnd.mEdt.requestFocus();
                    return true;
				} else if (mQueryFragment.mSelectedEdt == mQueryFragment.mEnd) {
				    mQueryFragment.mSelectedEdt = mQueryFragment.mStart;
                    mQueryFragment.mStart.mEdt.requestFocus();
                    return true;
				}
			}
			
			return false;
		}
	}

	protected class SelectStartEndBtnClickListener implements OnClickListener {

		private QueryEditText mQueryEdt;
		
		public SelectStartEndBtnClickListener(QueryEditText queryEdt) {
			mQueryEdt = queryEdt;
		}
		
		@Override
		public void onClick(View v) {
			mQueryFragment.mLogHelper.logForClickBookmarkOnEditText(mQueryEdt);
			mQueryFragment.mSelectedEdt = mQueryEdt;
			
			mQueryFragment.mSphinx.hideSoftInput();
        		/*
        		 * 显示选择对话框
        		 */
        		String title = (R.id.start_edt == mQueryEdt.getEdt().getId()) ? mQueryFragment.mContext.getString(R.string.select_start_station) : mQueryFragment.mContext.getString(R.string.select_end_station);
        		boolean hasMyLocation = (Globals.g_My_Location_City_Info != null);
        		
        		showSelectOptionDialog(mQueryFragment.mSphinx, mQueryEdt, title, hasMyLocation);
        		
        	}
		
	}
	
	protected class EditTextContentTextWatcher implements TextWatcher {

		private QueryEditText mQueryEdt;
		
		public EditTextContentTextWatcher(QueryEditText queryEdt) {
			mQueryEdt = queryEdt;
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start,
				int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start,
				int before, int count) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			mQueryFragment.checkQueryState();
			if (mQueryEdt.getEdt().getText().toString().equals(mQueryFragment.mContext.getString(R.string.my_location))
			        && mQueryEdt != mQueryFragment.mBusline) {
                mQueryEdt.getEdt().setTextColor(0xFF009CFF);
                //和马然确认，去掉所有自动选中的状态
//                mQueryEdt.getEdt().setSelectAllOnFocus(true);
            } else {
                mQueryEdt.getEdt().setTextColor(0xFF000000);
                mQueryEdt.getEdt().setEnabled(true);
//                mQueryEdt.getEdt().setSelectAllOnFocus(false);
            }
			
			if (mQueryFragment.mStart == mQueryEdt) {
			    if (mQueryFragment.isEditTextEmpty(mQueryFragment.mStart.mEdt) == false) {
			        mQueryFragment.mEnd.mEdt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
			    } else {
			        mQueryFragment.mEnd.mEdt.setImeOptions(EditorInfo.IME_ACTION_NEXT);
			    }
			} else {
			    if (mQueryFragment.isEditTextEmpty(mQueryFragment.mEnd.mEdt) == false) {
                    mQueryFragment.mStart.mEdt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                } else {
                    mQueryFragment.mStart.mEdt.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                }
			}
		}
		
	}
	
	protected class EditorActionListener implements OnEditorActionListener {

		private QueryEditText mQueryEdt;
		
		public EditorActionListener(QueryEditText queryEdt) {
			mQueryEdt = queryEdt;
		}
        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            	mQueryFragment.mSphinx.hideSoftInput();
            	mQueryFragment.submitBuslineQuery();
                return true;
            }
            return false;
        }
    }
	
//	protected class RootViewTouchListener implements OnTouchListener {
//
//		@Override
//		public boolean onTouch(View v, MotionEvent event) {
//			return true;
//		}
//		
//	}
	
	void checkRadioButton(int checkedId) {
	    switch(checkedId){
        case R.id.traffic_transfer_rbt:
            if (mQueryFragment.mLogHelper.logForTabChange)
                mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficTransferTab);
            mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            break;
        case R.id.traffic_drive_rbt:
            if (mQueryFragment.mLogHelper.logForTabChange)
                mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficDriveTab);
            mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            break;
        case R.id.traffic_walk_rbt:
            if (mQueryFragment.mLogHelper.logForTabChange)
                mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficWalkTab);
            mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            break;
        case R.id.traffic_busline_rbt:
            if (mQueryFragment.mLogHelper.logForTabChange)
                mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficBusLineTab);
            mQueryFragment.changeToMode(TrafficQueryFragment.BUSLINE_MODE);
        default:
            break;
        }
    	mQueryFragment.checkQueryState();
	}
	
	protected class NormalRadioOnClickedListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            mQueryFragment.mStateTransitionTable.event(Event.ClickRadioGroup);
            checkRadioButton(v.getId());
            mQueryFragment.mSphinx.showSoftInput(mQueryFragment.mSelectedEdt.getEdt().getInput());
        }
	    
	}

	protected class EditTextTouchListener implements OnTouchListener {
		
		private QueryEditText mQueryEdt;
		
		private int suggestWordsType;
		
		public EditTextTouchListener(QueryEditText queryEdt, final int suggestsType) {
			mQueryEdt = queryEdt;
			suggestWordsType = suggestsType;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mQueryFragment.mSelectedEdt = mQueryEdt;
				mQueryFragment.mLogHelper.logForClickOnEditText(mQueryEdt);
				mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.ClickEditText);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mQueryEdt.getEdt().requestFocus();
				mQueryFragment.mSphinx.showSoftInput(mQueryEdt.getEdt().getInput());
				
//				mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mContext, mQueryEdt.getEdt(), suggestWordsType);
				
			} 
			
			return false;
		}
	}
	
	protected class TrafficEditFocusListener implements OnFocusChangeListener {

	    private QueryEditText mQueryEdt;
	    
	    private int suggestWordsType;
	    
	    public TrafficEditFocusListener(QueryEditText queryEdt, final int type) {
	        mQueryEdt = queryEdt;
	        suggestWordsType = type;
	    }
	    
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            /*
             *  写这个函数的目的目前是为了保证建议词点击的时候会把建议词填到正确的输入框中
             *  以后需要把所有在focus的时候产生的变化都转移到这里来。
             */
            if (hasFocus) {
                mQueryFragment.mSelectedEdt = mQueryEdt;
                mQueryFragment.mSuggestWordHelper.refresh(mQueryFragment.mContext, mQueryEdt.getEdt(), suggestWordsType);
            }
        }
	    
	}

	protected class InputBackClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
//			onBack();
			synchronized (mQueryFragment.mSphinx.mUILock) {
                if (!mQueryFragment.mSphinx.mUIProcessing) {
                	mQueryFragment.onBack();
                }
			}
			mQueryFragment.mSphinx.hideSoftInput();
		}
		
	}
	
	protected class InputRadioOnClickedListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
        	checkRadioButton(v.getId());
        	/*
        	 * 切换TAB时, 若三个输入框都没有获得焦点, 则隐藏输入法
        	 */
//			if (!mQueryFragment.mStart.getEdt().isFocused() && !mQueryFragment.mEnd.getEdt().isFocused() 
//					&& !mQueryFragment.mBusline.getEdt().isFocused()) {
//				mQueryFragment.mSphinx.hideSoftInput();
//			}
        }
	    
	}

	protected class InputQueryClickListener implements OnClickListener {
	    
	    String log;
	    InputQueryClickListener() {
	    }

		@Override
		public void onClick(View v) {
			mQueryFragment.query();
		}
		
	}
	
	protected class InputSuggestOnItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			TKWord tkWord = ((SuggestArrayAdapter) parent.getAdapter()).getItem(position);

			String log = "Start";
            if (mQueryFragment.mSelectedEdt == mQueryFragment.mEnd) {
                log = "End";
            } else if (mQueryFragment.mSelectedEdt == mQueryFragment.mBusline) {
                log = "Busline";
            }
            
			if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag + ActionLog.ListViewItemHistoryClear);
	            if (mQueryFragment.mode == TrafficQueryFragment.TRAFFIC_MODE) {
	                HistoryWordTable.clearHistoryWord(mQueryFragment.mSphinx, mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId(), HistoryWordTable.TYPE_TRAFFIC);
	                mQueryFragment.mSuggestWordHelper.refresh(mQueryFragment.mContext, mQueryFragment.mSelectedEdt.getEdt(), TrafficQuerySuggestWordHelper.TYPE_TRAFFIC);
	            } else if (mQueryFragment.mode == TrafficQueryFragment.BUSLINE_MODE){
	                HistoryWordTable.clearHistoryWord(mQueryFragment.mSphinx, mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId(), HistoryWordTable.TYPE_BUSLINE);
	                mQueryFragment.mSuggestWordHelper.refresh(mQueryFragment.mContext, mQueryFragment.mSelectedEdt.getEdt(), TrafficQuerySuggestWordHelper.TYPE_BUSLINE);
	            }
			} else  if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST) {
			    POI poi = tkWord.toPOI();
				Position wordLonLat = mQueryFragment.mSphinx.getMapEngine().getwordslistStringWithPosition(tkWord.word, 0);
				if (null != wordLonLat && Util.inChina(wordLonLat)) {
					poi.setPosition(wordLonLat);
				}
				mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag + ActionLog.ListViewItemSuggest, mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, position), tkWord.word);
				mQueryFragment.mSuggestWordHelper.suggestSelect(poi, position);
			} else  if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
			    mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag + ActionLog.ListViewItemHistory, mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, position), tkWord.word);
			    mQueryFragment.mSuggestWordHelper.suggestSelect(tkWord.toPOI(), position);
			}
		}
		
	}	
	
	protected class InputSuggestOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
//			LogWrapper.d("eric", "SuggestLsv.onTouch");
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mQueryFragment.mSphinx.hideSoftInput();
            }
            return false;
		}
		
	}
	
	protected class InputEditTextSuggestWordTextWatcher implements TextWatcher {

		private QueryEditText mQueryEdt;
		
		private int suggestWordsType;
		
		public InputEditTextSuggestWordTextWatcher(QueryEditText queryEdt, final int suggestsType) {
			mQueryEdt = queryEdt;
			suggestWordsType = suggestsType;
		}

		@Override
        public void afterTextChanged(Editable s) {
            mQueryFragment.mSuggestWordHelper.refresh(mQueryFragment.mContext, mQueryEdt.getEdt(), suggestWordsType);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }
	}
	
	protected class MapBackClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			mQueryFragment.mRadioGroup.setOnCheckedChangeListener(null);
//			onBack();
			synchronized (mQueryFragment.mSphinx.mUILock) {
                if (!mQueryFragment.mSphinx.mUIProcessing) {
                	mQueryFragment.onBack();
                }
			}
		}
		
	}
	
	protected class MapRadioOnClickedListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.ClickRadioGroup);
            checkRadioButton(v.getId());
            mQueryFragment.mSphinx.showSoftInput(mQueryFragment.mSelectedEdt.getEdt().getInput());
        }
	    
	}

	protected class SelectPointLeftBtnOnClickListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			mQueryFragment.mStateTransitionTable.event(Event.Back);
		}
	}
	
	/**
	 * 
	 * 点击书签弹出选择对话框
	 * @author linqingzu
	 *
	 */
		
		public void showSelectOptionDialog(Activity activity, final QueryEditText queryEdt, String title, final boolean hasMyLocation) {
		    String[] strList = mQueryFragment.mContext.getResources().getStringArray(R.array.select_location_option);
            final List<String> selectOptionList = new ArrayList<String>();
            int[] resIdList;
            for(String str : strList) {
                selectOptionList.add(str);
            }
            if (!hasMyLocation) {
                selectOptionList.remove(0);
                resIdList = new int[] {R.drawable.ic_select_point, R.drawable.ic_favorite, R.drawable.ic_swap_start_end};
            } else {
                resIdList = new int[] {R.drawable.ic_mylocation, R.drawable.ic_select_point, R.drawable.ic_favorite, R.drawable.ic_swap_start_end};
            }
            ArrayAdapter<String> adapter = new StringArrayAdapter(mQueryFragment.mContext, selectOptionList, resIdList);
            
		    ListView listView = Utility.makeListView(activity);
		    listView.setAdapter(adapter);
            final Dialog dialog = Utility.showNormalDialog(mQueryFragment.mSphinx,
                    title,
                    null,
                    listView,
                    null,
                    null,
                    null);
            
            dialog.setCancelable(true);
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
                    dialog.dismiss();
                    mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag + ActionLog.ListViewItem, selectOptionList.get(which));
                    mQueryFragment.mSelectedEdt = queryEdt;
                    //有定位的情况下四个选项为“当前位置”，“地图选点”，“收藏夹”，“交换起终点”
                    //如果没有定位，则正常第一个选择“当前位置”不显示，which加一才能对应到到正确操作。
                    if (hasMyLocation == false) {
                        which++;
                    }
                    switch (which) {
                    case 0:
                        //当前位置
                        performSelectMyLocation();
                        break;
                    case 1:
                        //地图选点
                        performMapSelectPoint(queryEdt);
                        break;
                    case 2:
                        //收藏夹
                        performSelectFavorite();
                        break;
                    case 3:
                        //交换起终点
                        performSwitchStartEnd();
                        break;
                    default:
                    }
                }
            });
        }
			
		    private void performSwitchStartEnd() {
		        mQueryFragment.mSuggestWordHelper.clearSuggestList();
//                clearSuggestWatcherInInputState();
                POI temp = mQueryFragment.mStart.getPOI();
                mQueryFragment.mStart.setPOI(mQueryFragment.mEnd.getPOI());
                mQueryFragment.mEnd.setPOI(temp);
                mQueryFragment.mSuggestWordHelper.clearSuggestList();
//                addSuggestWatcherInInputState();
//                mQueryFragment.mBlock.requestFocus();
		    }
		    
			private void performSelectMyLocation() {
				LogWrapper.d("eric", "performSelectMyLocation()");

	        		POI poi = new POI();
	        		poi.setName(mQueryFragment.mContext.getString(R.string.my_location));
	        		mQueryFragment.setData(poi, TrafficQueryFragment.SELECTED);

			}
			
			private void performMapSelectPoint(QueryEditText queryEditText) {
				LogWrapper.d("eric", "performMapSelectPoint()");

				mQueryFragment.mSphinx.setTouchMode(R.id.start_edt == queryEditText.getEdt().getId() ? TouchMode.CHOOSE_ROUTING_START_POINT : TouchMode.CHOOSE_ROUTING_END_POINT);
				
				Position center = mQueryFragment.mSphinx.getMapView().getCenterPosition();
				String positionName = mQueryFragment.mSphinx.getMapEngine().getPositionName(center, (int)mQueryFragment.mSphinx.getMapView().getZoomLevel());
				if (TextUtils.isEmpty(positionName)) {
					positionName = mQueryFragment.mContext.getString(R.string.select_has_point);
				}
				
				mQueryFragment.mSphinx.clearMap();
				PinOverlayHelper.drawSelectPointOverlay(mQueryFragment.mSphinx, mQueryFragment.mSphinx.getHandler(), mQueryFragment.mSphinx.getMapView(), R.id.start_edt == queryEditText.getEdt().getId(), positionName, center);
				mQueryFragment.mSphinx.getMapView().refreshMap();
				
				mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.ClicktoSelectPoint);
	            
	            String tip = mQueryFragment.mContext.getString(R.id.start_edt == queryEditText.getEdt().getId() ? R.string.click_map_as_start : R.string.click_map_as_end);
	            mQueryFragment.mSphinx.showTip(tip, Toast.LENGTH_SHORT);
			}
			
			private void performSelectFavorite() {
				LogWrapper.d("eric", "performSelectFavorite()");

				mQueryFragment.mSphinx.getFetchFavoriteFragment().setData(mQueryFragment);
				mQueryFragment.mSphinx.showView(R.id.view_traffic_fetch_favorite_poi);
			}
}

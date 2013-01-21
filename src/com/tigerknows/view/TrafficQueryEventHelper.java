package com.tigerknows.view;

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
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.tigerknows.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.maps.PinOverlayHelper;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.TrafficQueryFragment.QueryEditText;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控件事件处理]]
 * @author linqingzu
 *
 */
public class TrafficQueryEventHelper {
	
	public static final String TAG = "TrafficQueryEventHelper";
	
	private TrafficQueryFragment mQueryFragment;
	
	InputEditTextSuggestWordTextWatcher startSuggestWatcher;
	InputEditTextSuggestWordTextWatcher endSuggestWatcher;
	InputEditTextSuggestWordTextWatcher buslineSuggestWatcher;
	
	public TrafficQueryEventHelper(TrafficQueryFragment queryFragment) {
		this.mQueryFragment = queryFragment;
		startSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mStart, TrafficQuerySuggestHistoryHelper.TYPE_TRAFFIC);
		endSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mEnd, TrafficQuerySuggestHistoryHelper.TYPE_TRAFFIC);
		buslineSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mBusline, TrafficQuerySuggestHistoryHelper.TYPE_BUSLINE);
		
	}
	
	/*
	 * Do not remove common Listeners
	 */
	public void clearListenersFromTargets() {
		mQueryFragment.mStart.getEdt().setOnClickListener(null);
		mQueryFragment.mEnd.getEdt().setOnClickListener(null);
		mQueryFragment.mBusline.getEdt().setOnClickListener(null);
		
		mQueryFragment.mStart.getEdt().setOnTouchListener(null);
		mQueryFragment.mEnd.getEdt().setOnTouchListener(null);
		mQueryFragment.mBusline.getEdt().setOnTouchListener(null);
		
		clearSuggestWatcherInInputState();
		
		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(null);
		//mQueryFragment.mExchangeBtn.setOnClickListener(null);
		mQueryFragment.mSelectStartBtn.setOnClickListener(null);
		mQueryFragment.mSelectEndBtn.setOnClickListener(null);
		mQueryFragment.mBackBtn.setOnClickListener(null);
		mQueryFragment.mTrafficQueryBtn.setOnClickListener(null);
		mQueryFragment.mBuslineQueryBtn.setOnClickListener(null);
		mQueryFragment.mSuggestLsv.setOnItemClickListener(null);
		mQueryFragment.mSuggestLsv.setOnTouchListener(null);
		mQueryFragment.mSuggestLnl.setOnTouchListener(null);
		mQueryFragment.mRootView.setOnTouchListener(null);
	}
	
	public void applyCommonListeners() {
		mQueryFragment.mStart.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mStart));
		mQueryFragment.mEnd.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mEnd));
		mQueryFragment.mBusline.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mBusline));
		
		mQueryFragment.mStart.getEdt().setOnEditorActionListener(new StartEndEdtClickListener());
		mQueryFragment.mEnd.getEdt().setOnEditorActionListener(new StartEndEdtClickListener());
		mQueryFragment.mBusline.getEdt().setOnEditorActionListener(new EditorActionListener(mQueryFragment.mBusline));

		mQueryFragment.mRootView.setOnTouchListener(new RootViewTouchListener());
		
		mQueryFragment.mStart.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mStart, TrafficQuerySuggestHistoryHelper.TYPE_TRAFFIC));
		mQueryFragment.mEnd.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mEnd, TrafficQuerySuggestHistoryHelper.TYPE_TRAFFIC));
		mQueryFragment.mBusline.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mBusline, TrafficQuerySuggestHistoryHelper.TYPE_BUSLINE));
	}
	
	public void applyListenersInNormalState() {
		LogWrapper.d("eric", "applyListenersInNormalState");
		
		clearListenersFromTargets();
		applyCommonListeners();
		
		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(new NormalOnCheckedChangeListener());
		//mQueryFragment.mExchangeBtn.setOnClickListener(new NormalExchangeClickListener());
		mQueryFragment.mSelectStartBtn.setOnClickListener(new SelectStartEndBtnClickListener(mQueryFragment.mStart));
		mQueryFragment.mSelectEndBtn.setOnClickListener(new SelectStartEndBtnClickListener(mQueryFragment.mEnd));
	}
	
	public void applyListenersInInputState() {
		LogWrapper.d("eric", "applyListenersInInputState");
		
		clearListenersFromTargets();
		applyCommonListeners();
		
		mQueryFragment.mBackBtn.setOnClickListener(new InputBackClickListener());
		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(new InputOnCheckedChangeListener());
		//mQueryFragment.mExchangeBtn.setOnClickListener(new InputExchangeClickListener());
		mQueryFragment.mSelectStartBtn.setOnClickListener(new SelectStartEndBtnClickListener(mQueryFragment.mStart));
		mQueryFragment.mSelectEndBtn.setOnClickListener(new SelectStartEndBtnClickListener(mQueryFragment.mEnd));
		mQueryFragment.mTrafficQueryBtn.setOnClickListener(new InputQueryClickListener());
		mQueryFragment.mBuslineQueryBtn.setOnClickListener(new InputQueryClickListener());
		mQueryFragment.mSuggestLsv.setOnItemClickListener(new InputSuggestOnItemClickListener());
		mQueryFragment.mSuggestLsv.setOnTouchListener(new InputSuggestOnTouchListener());
		mQueryFragment.mSuggestLnl.setOnTouchListener(new InputSuggestOnTouchListener());
		
		addSuggestWatcherInInputState();
	}
	
	public void applyListenersInMapState() {
		LogWrapper.d("eric", "applyListenersInMapState");
		
		clearListenersFromTargets();
		applyCommonListeners();
		
		mQueryFragment.mBackBtn.setOnClickListener(new MapBackClickListener());
		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(new MapRadioOnCheckedChangeListener());
		mQueryFragment.mRootView.setOnTouchListener(new RootViewTouchListener());
	}
	
	public void applyListenersInSelectPointState() {
		mQueryFragment.mLeftBtn.setOnClickListener(new SelectPointLeftBtnOnClickListener());
	}
	
	public void addSuggestWatcherInInputState() {
		mQueryFragment.mStart.getEdt().addTextChangedListener(startSuggestWatcher);
		mQueryFragment.mEnd.getEdt().addTextChangedListener(endSuggestWatcher);
		mQueryFragment.mBusline.getEdt().addTextChangedListener(buslineSuggestWatcher);
	}
	
	public void clearSuggestWatcherInInputState() {
		mQueryFragment.mStart.getEdt().removeTextChangedListener(startSuggestWatcher);
		mQueryFragment.mEnd.getEdt().removeTextChangedListener(endSuggestWatcher);
		mQueryFragment.mBusline.getEdt().removeTextChangedListener(buslineSuggestWatcher);
	}
	
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
			// TODO Auto-generated method stub
			mQueryFragment.mSelectedEdt = mQueryEdt;
			
        		/*
        		 * 显示选择对话框
        		 */
        		String title = (R.id.start_edt == mQueryEdt.getEdt().getId()) ? mQueryFragment.mContext.getString(R.string.select_start_station) : mQueryFragment.mContext.getString(R.string.select_end_station);
        		boolean hasMyLocation = (Globals.g_My_Location_City_Info != null);
        		
        		showSelectOptionDialog(mQueryFragment.mSphinx, mQueryEdt, title, hasMyLocation);
        		
        		mQueryFragment.mSphinx.hideSoftInput();
        		mQueryFragment.mLogHelper.logForClickBookmarkOnEditText(mQueryEdt);
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
			// TODO Auto-generated method stub
		}

		@Override
		public void onTextChanged(CharSequence s, int start,
				int before, int count) {
			// TODO Auto-generated method stub
		}

		@Override
		public void afterTextChanged(Editable s) {
			mQueryFragment.checkQueryState();
			if (mQueryEdt.getEdt().getText().toString().equals(mQueryFragment.mContext.getString(R.string.my_location))) {
                mQueryEdt.getEdt().setTextColor(0xFF009CFF);
                mQueryEdt.getEdt().setSelectAllOnFocus(true);
            } else {
                mQueryEdt.getEdt().setTextColor(0xFF000000);
                mQueryEdt.getEdt().setEnabled(true);
                mQueryEdt.getEdt().setSelectAllOnFocus(false);
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
	
	protected class RootViewTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			return true;
		}
		
	}

	protected class NormalOnCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {
    	
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // TODO Auto-generated method stub
        	switch(checkedId){
            case R.id.traffic_transfer_rbt:
            	if (mQueryFragment.mLogHelper.logForTabChange)
            		mQueryFragment.mActionLog.addAction(ActionLog.TrafficBusTab);
            	mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            	mQueryFragment.initStartContent();
                break;
            case R.id.traffic_drive_rbt:
            	if (mQueryFragment.mLogHelper.logForTabChange)
            		mQueryFragment.mActionLog.addAction(ActionLog.TrafficDriveTab);
            	mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            	mQueryFragment.initStartContent();
                break;
            case R.id.traffic_walk_rbt:
            	if (mQueryFragment.mLogHelper.logForTabChange)
            		mQueryFragment.mActionLog.addAction(ActionLog.TrafficWalkTab);
            	mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            	mQueryFragment.initStartContent();
                break;
            case R.id.traffic_busline_rbt:
            	if (mQueryFragment.mLogHelper.logForTabChange)
            		mQueryFragment.mActionLog.addAction(ActionLog.TrafficLineTab);
            	mQueryFragment.changeToMode(TrafficQueryFragment.BUSLINE_MODE);
            default:
                break;
            }
        	mQueryFragment.checkQueryState();
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
			// TODO Auto-generated method stub
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mQueryFragment.mSelectedEdt = mQueryEdt;
				mQueryFragment.mLogHelper.logForClickOnEditText(mQueryEdt);
				mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.ClickEditText);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				mQueryEdt.getEdt().requestFocus();
				mQueryFragment.mSphinx.showSoftInput(mQueryEdt.getEdt().getInput());
				
				mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mContext, mQueryEdt.getEdt(), suggestWordsType);
				
			} 
			
			return false;
		}
	}
	
	protected class NormalExchangeClickListener implements OnClickListener {

		@Override
		//xupeng:这是交换按钮的处理函数。
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficExchangeBtn);
			POI temp = mQueryFragment.mStart.getPOI();
			mQueryFragment.mStart.setPOI(mQueryFragment.mEnd.getPOI());
			mQueryFragment.mEnd.setPOI(temp);
		}
		
	}

	protected class InputBackClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
//			onBack();
			synchronized (mQueryFragment.mSphinx.mUILock) {
                if (!mQueryFragment.mSphinx.mUIProcessing) {
                	mQueryFragment.onBack();
                }
			}
			mQueryFragment.mSphinx.hideSoftInput();
		}
		
	}
	
	protected class InputOnCheckedChangeListener extends NormalOnCheckedChangeListener {
    	
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
        	super.onCheckedChanged(group, checkedId);
        	
			/*
			 * 切换TAB时, 若三个输入框都没有获得焦点, 则隐藏输入法
			 */
			if (!mQueryFragment.mStart.getEdt().isFocused() && !mQueryFragment.mEnd.getEdt().isFocused() 
					&& !mQueryFragment.mBusline.getEdt().isFocused()) {
				mQueryFragment.mSphinx.hideSoftInput();
			}
        }
	}
	
	protected class InputExchangeClickListener extends NormalExchangeClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.mSphinx.hideSoftInput();
			clearSuggestWatcherInInputState();
			super.onClick(v);
			addSuggestWatcherInInputState();
		}
	}

	protected class InputQueryClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.query();
		}
		
	}
	
	protected class InputSuggestOnItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			// TODO Auto-generated method stub
			LogWrapper.d("eric", "SuggestLsv.onItemClick");
			TKWord tkWord = ((SuggestArrayAdapter) parent.getAdapter()).getItem(position);
			
			if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
			    mQueryFragment.mActionLog.addAction(ActionLog.TrafficClearHistory);
	            if (mQueryFragment.mode == TrafficQueryFragment.TRAFFIC_MODE) {
	                HistoryWordTable.clearHistoryWord(mQueryFragment.mSphinx, mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId(), HistoryWordTable.TYPE_TRAFFIC);
	                mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mContext, mQueryFragment.mSelectedEdt.getEdt(), TrafficQuerySuggestHistoryHelper.TYPE_TRAFFIC);
	            } else if (mQueryFragment.mode == TrafficQueryFragment.BUSLINE_MODE){
	                HistoryWordTable.clearHistoryWord(mQueryFragment.mSphinx, mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId(), HistoryWordTable.TYPE_BUSLINE);
	                mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mContext, mQueryFragment.mSelectedEdt.getEdt(), TrafficQuerySuggestHistoryHelper.TYPE_BUSLINE);
	            }
			} else  if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST) {
			    POI poi = tkWord.toPOI();
				Position wordLonLat = mQueryFragment.mSphinx.getMapEngine().getwordslistStringWithPosition(tkWord.word, 0);
				if (null != wordLonLat && Util.inChina(wordLonLat)) {
					poi.setPosition(wordLonLat);
				}
				mQueryFragment.mSuggestHistoryHelper.suggestSelect(poi, position);
				
			} else  if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
			    mQueryFragment.mSuggestHistoryHelper.suggestSelect(tkWord.toPOI(), position);
			}
		}
		
	}	
	
	protected class InputSuggestOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
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
            mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mContext, mQueryEdt.getEdt(), suggestWordsType);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
            // TODO Auto-generated method stub
        }
	}
	
	protected class MapBackClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.mRadioGroup.setOnCheckedChangeListener(null);
//			onBack();
			synchronized (mQueryFragment.mSphinx.mUILock) {
                if (!mQueryFragment.mSphinx.mUIProcessing) {
                	mQueryFragment.onBack();
                }
			}
		}
		
	}
	
	protected class MapRadioOnCheckedChangeListener extends NormalOnCheckedChangeListener {
		
		@Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
        	super.onCheckedChanged(group, checkedId);
        	mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.ClickRadioGroup);
        }
	}

	protected class SelectPointLeftBtnOnClickListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.mStateTransitionTable.rollback();
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
            List<String> selectOptionList = new ArrayList<String>();
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
            
		    ListView listView = CommonUtils.makeListView(activity);
		    listView.setAdapter(adapter);
            final Dialog dialog = CommonUtils.showNormalDialog(mQueryFragment.mSphinx,
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
                    mQueryFragment.mSelectedEdt = queryEdt;
                    switch (which) {
                    case 0:
                        if (hasMyLocation) {
                            performSelectMyLocation();
                        } else {
                            performMapSelectPoint(queryEdt);
                        }
                        break;
                    case 1:
                        if (hasMyLocation) {
                            performMapSelectPoint(queryEdt);
                        } else {
                            performSelectFavorite();
                        }
                        break;
                    case 2:
                        performSelectFavorite();
                        break;
                    case 3:
                        //交换起点终点。
                        mQueryFragment.mSuggestHistoryHelper.refresh(mQueryFragment.mSphinx, null, TrafficQuerySuggestHistoryHelper.TYPE_TRAFFIC);
                        clearSuggestWatcherInInputState();
                        mQueryFragment.mActionLog.addAction(ActionLog.TrafficExchangeBtn);
                        POI temp = mQueryFragment.mStart.getPOI();
                        mQueryFragment.mStart.setPOI(mQueryFragment.mEnd.getPOI());
                        mQueryFragment.mEnd.setPOI(temp);
                        addSuggestWatcherInInputState();
//                        mQueryFragment.mBlock.requestFocus();
                        break;
                    default:
                    }
                }
            });
        }
			
			private void performSelectMyLocation() {
				LogWrapper.d("eric", "performSelectMyLocation()");

				mQueryFragment.mActionLog.addAction(ActionLog.TrafficBookmarkFirst);
	        		POI poi = new POI();
	        		poi.setName(mQueryFragment.mContext.getString(R.string.my_location));
	        		mQueryFragment.setData(poi, TrafficQueryFragment.SELECTED);

			}
			
			private void performMapSelectPoint(QueryEditText queryEditText) {
				LogWrapper.d("eric", "performMapSelectPoint()");

				mQueryFragment.mActionLog.addAction(ActionLog.TrafficBookmarkSecond);
				
				mQueryFragment.mSphinx.setTouchMode(R.id.start_edt == queryEditText.getEdt().getId() ? TouchMode.CHOOSE_ROUTING_START_POINT : TouchMode.CHOOSE_ROUTING_END_POINT);
				
				Position center = mQueryFragment.mSphinx.getMapView().getCenterPosition();
				String positionName = mQueryFragment.mSphinx.getMapEngine().getPositionName(center, (int)mQueryFragment.mSphinx.getMapView().getZoomLevel());
				if (TextUtils.isEmpty(positionName)) {
					positionName = mQueryFragment.mContext.getString(R.string.select_has_point);
				}
				
				mQueryFragment.mSphinx.clearMap();
				PinOverlayHelper.drawSelectPointOverlay(mQueryFragment.mContext, mQueryFragment.mSphinx.getHandler(), mQueryFragment.mSphinx.getMapView(), R.id.start_edt == queryEditText.getEdt().getId(), positionName, center);
				mQueryFragment.mSphinx.getMapView().refreshMap();
				
				mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.Point);
	            
	            String tip = mQueryFragment.mContext.getString(R.id.start_edt == queryEditText.getEdt().getId() ? R.string.click_map_as_start : R.string.click_map_as_end);
	            mQueryFragment.mSphinx.showTip(tip, Toast.LENGTH_SHORT);
			}
			
			private void performSelectFavorite() {
				LogWrapper.d("eric", "performSelectFavorite()");

				mQueryFragment.mActionLog.addAction(ActionLog.TrafficBookmarkThird);
				
				mQueryFragment.mSphinx.getFetchFavoriteDialog().setData(mQueryFragment);
				mQueryFragment.mSphinx.showView(R.id.dialog_fetch_favorite_poi);
			}
}

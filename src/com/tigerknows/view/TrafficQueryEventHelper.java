package com.tigerknows.view;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.maps.PinOverlayHelper;
import com.tigerknows.model.POI;
import com.tigerknows.view.TrafficQueryFragment.QueryEditText;
import com.tigerknows.view.TrafficQuerySuggestHistoryHelper.SuggestAndHistoryAdapter;

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
		startSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mStart, SuggestAndHistoryAdapter.HISTORY_TRAFFIC_TYPE);
		endSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mEnd, SuggestAndHistoryAdapter.HISTORY_TRAFFIC_TYPE);
		buslineSuggestWatcher = new InputEditTextSuggestWordTextWatcher(mQueryFragment.mBusline, SuggestAndHistoryAdapter.HISTORY_BUSLINE_TYPE);
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
		mQueryFragment.mExchangeBtn.setOnClickListener(null);
		mQueryFragment.mBackBtn.setOnClickListener(null);
		mQueryFragment.mQueryBtn.setOnClickListener(null);
		mQueryFragment.mSuggestLsv.setOnItemClickListener(null);
		mQueryFragment.mSuggestLsv.setOnTouchListener(null);
		mQueryFragment.mSuggestLnl.setOnTouchListener(null);
		mQueryFragment.mRootView.setOnTouchListener(null);
	}
	
	public void applyCommonListeners() {
		mQueryFragment.mStart.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mStart));
		mQueryFragment.mEnd.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mEnd));
		mQueryFragment.mBusline.getEdt().addTextChangedListener(new EditTextContentTextWatcher(mQueryFragment.mBusline));
		
		mQueryFragment.mStart.getRightImg().setOnClickListener(new BookmarkClickListener(mQueryFragment.mStart));
		mQueryFragment.mEnd.getRightImg().setOnClickListener(new BookmarkClickListener(mQueryFragment.mEnd));
		
		mQueryFragment.mStart.getEdt().setOnEditorActionListener(new EditorActionListener(mQueryFragment.mStart));
		mQueryFragment.mEnd.getEdt().setOnEditorActionListener(new EditorActionListener(mQueryFragment.mEnd));
		mQueryFragment.mBusline.getEdt().setOnEditorActionListener(new EditorActionListener(mQueryFragment.mBusline));

		mQueryFragment.mRootView.setOnTouchListener(new RootViewTouchListener());
		
		mQueryFragment.mStart.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mStart, SuggestAndHistoryAdapter.HISTORY_TRAFFIC_TYPE));
		mQueryFragment.mEnd.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mEnd, SuggestAndHistoryAdapter.HISTORY_TRAFFIC_TYPE));
		mQueryFragment.mBusline.getEdt().setOnTouchListener(new EditTextTouchListener(mQueryFragment.mBusline, SuggestAndHistoryAdapter.HISTORY_BUSLINE_TYPE));
	}
	
	public void applyListenersInNormalState() {
		LogWrapper.d("eric", "applyListenersInNormalState");
		
		clearListenersFromTargets();
		applyCommonListeners();
		
		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(new NormalOnCheckedChangeListener());
		mQueryFragment.mExchangeBtn.setOnClickListener(new NormalExchangeClickListener());
	}
	
	public void applyListenersInInputState() {
		LogWrapper.d("eric", "applyListenersInInputState");
		
		clearListenersFromTargets();
		applyCommonListeners();
		
		mQueryFragment.mBackBtn.setOnClickListener(new InputBackClickListener());
		mQueryFragment.mRadioGroup.setOnCheckedChangeListener(new InputOnCheckedChangeListener());
		mQueryFragment.mExchangeBtn.setOnClickListener(new InputExchangeClickListener());
		mQueryFragment.mQueryBtn.setOnClickListener(new InputQueryClickListener());
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

	protected class BookmarkClickListener implements OnClickListener {

		private QueryEditText mQueryEdt;
		
		public BookmarkClickListener(QueryEditText queryEdt) {
			mQueryEdt = queryEdt;
		}
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.mSelectedEdt = mQueryEdt;
			
        	if (TextUtils.isEmpty(mQueryEdt.getEdt().getText())) {
        		mQueryFragment.mBlock.requestFocus();
        		//若文本为空, 右边图片显示为"小词典", 点击后提供三个选项
        		/*
        		 * 显示选择对话框
        		 */
        		String title = (mQueryEdt.getPosition() == TrafficQueryFragment.START) ? mQueryFragment.mContext.getString(R.string.select_start_station) : mQueryFragment.mContext.getString(R.string.select_end_station);
        		boolean hasMyLocation = (Globals.g_My_Location_City_Info != null);
        		
        		SelectionDialogBuilder dialog = new SelectionDialogBuilder(mQueryFragment.mSphinx, mQueryEdt, title, hasMyLocation);
        		dialog.show();
        		
        		mQueryFragment.mSphinx.hideSoftInput(mQueryEdt.getEdt().getWindowToken());
        		mQueryFragment.mLogHelper.logForClickBookmarkOnEditText(mQueryEdt);
        	} else {
        		//若文本不为空, 右边图片显示为"删除", 点击后删除输入框内文字
        		mQueryEdt.clear();
        		mQueryEdt.getEdt().requestFocus();
        		mQueryFragment.mLogHelper.logForClickDeleteOnEditText(mQueryEdt);
        	}
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
			// TODO Auto-generated method stub
			mQueryFragment.checkQueryState();
			mQueryFragment.postTask(new CheckInputStateTask(mQueryEdt));
		}
		
	}
	
	protected class EditorActionListener implements OnEditorActionListener {

		private QueryEditText mQueryEdt;
		
		public EditorActionListener(QueryEditText queryEdt) {
			mQueryEdt = queryEdt;
		}
        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            	mQueryFragment.mSphinx.hideSoftInput(mQueryEdt.getEdt().getWindowToken());
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
            	mQueryFragment.showStart();
                break;
            case R.id.traffic_drive_rbt:
            	if (mQueryFragment.mLogHelper.logForTabChange)
            		mQueryFragment.mActionLog.addAction(ActionLog.TrafficDriveTab);
            	mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            	mQueryFragment.showStart();
                break;
            case R.id.traffic_walk_rbt:
            	if (mQueryFragment.mLogHelper.logForTabChange)
            		mQueryFragment.mActionLog.addAction(ActionLog.TrafficWalkTab);
            	mQueryFragment.changeToMode(TrafficQueryFragment.TRAFFIC_MODE);
            	mQueryFragment.showStart();
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
				mQueryFragment.mSphinx.showSoftInput(mQueryEdt.getEdt());
				
				if (mQueryEdt.getEdt().getText().toString().equals(mQueryFragment.mContext.getString(R.string.my_location))) {
					mQueryFragment.mSuggestLsv.setVisibility(View.GONE);
				} else {
					mQueryFragment.mSphinx.getHandler().post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							mQueryFragment.mSuggestHistoryHelper.setAdapterForSuggest(mQueryFragment.mContext, mQueryEdt.getEdt(), mQueryFragment.mSuggestLsv, suggestWordsType);
						}
						
					});
				}
				
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
			mQueryFragment.mSphinx.hideSoftInput(mQueryFragment.mBackBtn.getWindowToken());
		}
		
	}
	
	protected class InputOnCheckedChangeListener extends NormalOnCheckedChangeListener {
    	
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
        	super.onCheckedChanged(group, checkedId);
        	
        	if (mQueryFragment.modeChange) {
        		mQueryFragment.mSuggestLsv.setAdapter(null);
        		LogWrapper.d("eric", "InputOnCheckedChangeListener set SuggestLsv GONE");
        		mQueryFragment.mSuggestLsv.setVisibility(View.GONE);
        	}
        	
			/*
			 * 切换TAB时, 若三个输入框都没有获得焦点, 则隐藏输入法
			 */
			if (!mQueryFragment.mStart.getEdt().isFocused() && !mQueryFragment.mEnd.getEdt().isFocused() 
					&& !mQueryFragment.mBusline.getEdt().isFocused()) {
				mQueryFragment.mSphinx.hideSoftInput(group.getWindowToken());
			}
        }
	}
	
	protected class InputExchangeClickListener extends NormalExchangeClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			mQueryFragment.mSphinx.hideSoftInput(mQueryFragment.mBlock.getWindowToken());
			mQueryFragment.mSuggestLsv.setVisibility(View.GONE);
			clearSuggestWatcherInInputState();
			super.onClick(v);
			addSuggestWatcherInInputState();
			mQueryFragment.mBlock.requestFocus();
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
			String word = (String) parent.getAdapter().getItem(position);
			
			POI poi = new POI();
			poi.setName(word);
			
			if (((SuggestAndHistoryAdapter)parent.getAdapter()).getItemType(position) == SuggestAndHistoryAdapter.SUGGEST_ENTRY ||
					((SuggestAndHistoryAdapter)parent.getAdapter()).getItemType(position) == SuggestAndHistoryAdapter.HISTORY_ENTRY) {
				Position wordLonLat = mQueryFragment.mSphinx.getMapEngine().getwordslistStringWithPosition(word, 0);
				if (null != wordLonLat && Util.inChina(wordLonLat)) {
					poi.setPosition(wordLonLat);
				}
			//} else if (((SuggestAndHistoryAdapter)parent.getAdapter()).getItemType(position) == SuggestAndHistoryAdapter.HISTORY_ENTRY) {
				//xupeng:添加历史词position
				//直接在上面取position的话对于服务器上有坐标但是建议词里面没有的还是不能存点。
			}
			mQueryFragment.mSuggestHistoryHelper.suggestSelect(poi, position);
		}
		
	}	
	
	protected class InputSuggestOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
//			LogWrapper.d("eric", "SuggestLsv.onTouch");
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mQueryFragment.mSphinx.hideSoftInput(mQueryFragment.mSuggestLsv.getWindowToken());
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
            // TODO Auto-generated method stub
			mQueryFragment.mSphinx.getHandler().post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mQueryFragment.mSuggestHistoryHelper.setAdapterForSuggest(mQueryFragment.mContext, mQueryEdt.getEdt(), mQueryFragment.mSuggestLsv, suggestWordsType);
				}
				
			});
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
	
	private class CheckInputStateTask implements Runnable {

		private QueryEditText mQueryEdt;
		
		public CheckInputStateTask(QueryEditText queryEdt) {
			mQueryEdt = queryEdt;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (!TextUtils.isEmpty(mQueryEdt.getEdt().getText())) {
				mQueryEdt.getRightImg().setBackgroundResource(R.drawable.btn_delete);
			} else {
				mQueryEdt.getRightImg().setBackgroundResource(R.drawable.btn_bookmark);
			}

//			checkQueryState();
			
			if (mQueryEdt.getEdt().getText().toString().equals(mQueryFragment.mContext.getString(R.string.my_location))) {
				mQueryEdt.getEdt().setTextColor(0xFF009CFF);
				mQueryEdt.getEdt().setSelectAllOnFocus(true);
			} else {
				mQueryEdt.getEdt().setTextColor(0xFF000000);
				mQueryEdt.getEdt().setEnabled(true);
				mQueryEdt.getEdt().setSelectAllOnFocus(false);
			}
			
		}
	}
	
	/**
	 * 
	 * 点击书签弹出选择对话框
	 * @author linqingzu
	 *
	 */
	public class SelectionDialogBuilder extends AlertDialog.Builder {

		private QueryEditText mQueryEdt;

		private ArrayList<String> selectOptionList;
		
		private boolean hasMyLocation;
		
		public SelectionDialogBuilder(Context context, QueryEditText queryEdt, String title, boolean hasMyLocation) {
			super(context);
			// TODO Auto-generated constructor stub
			mQueryEdt = queryEdt;
			
			String[] strList = mQueryFragment.mContext.getResources().getStringArray(R.array.select_location_option);
	        this.hasMyLocation = hasMyLocation;       
	        selectOptionList = new ArrayList<String>();
	        for(String str : strList) {
	            selectOptionList.add(str);
	        }
	        if (!hasMyLocation) {
	            selectOptionList.remove(0);
	        }
	        
	        setTitle(title);
	        setCancelable(true);
	        setAdapter(getArrayAdapter(), new DialogOnClickListener(mQueryEdt));
		}
		
		public ArrayAdapter<String> getArrayAdapter() {
            int[] resIdList;
            if (hasMyLocation) {
                resIdList = new int[] {R.drawable.ic_mylocation, R.drawable.ic_select_point, R.drawable.ic_favorite};
            } else {
                resIdList = new int[] {R.drawable.ic_select_point, R.drawable.ic_favorite};
            }
			ArrayAdapter<String> adapter = new StringArrayAdapter(mQueryFragment.mContext, selectOptionList, resIdList);
		    return adapter;
		}
		
		private class DialogOnClickListener implements DialogInterface.OnClickListener {

			private QueryEditText mQueryEdt;
			
			public DialogOnClickListener(QueryEditText queryEdt) {
				mQueryEdt = queryEdt;
			}
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
				mQueryFragment.mSelectedEdt = mQueryEdt;
                switch (which) {
                case 0:
                	if (hasMyLocation) {
                		performSelectMyLocation();
                	} else {
                		performMapSelectPoint();
                	}
                	break;
                case 1:
                	if (hasMyLocation) {
                		performMapSelectPoint();
                	} else {
                		performSelectFavorite();
                	}
                	break;
                case 2:
                	performSelectFavorite();
                	break;
                default:
                }
            }
			
			private void performSelectMyLocation() {
				LogWrapper.d("eric", "performSelectMyLocation()");

				mQueryFragment.mActionLog.addAction(ActionLog.TrafficBookmarkFirst);
	        		POI poi = new POI();
	        		poi.setName(mQueryFragment.mContext.getString(R.string.my_location));
	        		mQueryFragment.setData(poi, TrafficQueryFragment.SELECTED);

			}
			
			private void performMapSelectPoint() {
				LogWrapper.d("eric", "performMapSelectPoint()");

				mQueryFragment.mActionLog.addAction(ActionLog.TrafficBookmarkSecond);
				
				mQueryFragment.mSphinx.setTouchMode(TrafficQueryFragment.START == mQueryEdt.getPosition() ? TouchMode.CHOOSE_ROUTING_START_POINT : TouchMode.CHOOSE_ROUTING_END_POINT);
				
				Position center = mQueryFragment.mSphinx.getMapView().getCenterPosition();
				String positionName = mQueryFragment.mSphinx.getMapEngine().getPositionName(center, (int)mQueryFragment.mSphinx.getMapView().getZoomLevel());
				if (TextUtils.isEmpty(positionName)) {
					positionName = mQueryFragment.mContext.getString(R.string.select_has_point);
				}
				POI tmp = new POI();
				tmp.setPosition(center);
				tmp.setName(positionName);
				String clickTip = mQueryFragment.mContext.getString(TrafficQueryFragment.START == mQueryEdt.getPosition() 
						? R.string.select_where_as_start : R.string.select_where_as_end, positionName);

				mQueryFragment.mSphinx.clearMap();
				PinOverlayHelper.drawSelectPointOverlay(mQueryFragment.mContext, mQueryFragment.mSphinx.getHandler(), mQueryFragment.mSphinx.getMapView(), tmp, clickTip);
				mQueryFragment.mSphinx.getMapView().refreshMap();
				
				mQueryFragment.mStateTransitionTable.event(TrafficViewSTT.Event.Point);
	            
	            String tip = mQueryFragment.mContext.getString(TrafficQueryFragment.START == mQueryEdt.getPosition() ? R.string.click_map_as_start : R.string.click_map_as_end);
	            mQueryFragment.mSphinx.showTip(tip, Toast.LENGTH_SHORT);
			}
			
			private void performSelectFavorite() {
				LogWrapper.d("eric", "performSelectFavorite()");

				mQueryFragment.mActionLog.addAction(ActionLog.TrafficBookmarkThird);
				
				mQueryFragment.mSphinx.getFetchFavoriteDialog().setData(mQueryFragment);
				mQueryFragment.mSphinx.showView(R.id.dialog_fetch_favorite_poi);
			}
		}

	}
}

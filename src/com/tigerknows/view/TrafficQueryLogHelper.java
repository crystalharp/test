package com.tigerknows.view;

import android.text.TextUtils;

import com.tigerknows.ActionLog;
import com.tigerknows.view.TrafficQueryFragment.QueryEditText;
import com.tigerknows.view.TrafficQuerySuggestHistoryHelper.SuggestAndHistoryAdapter;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[行为日志处理]]
 * @author linqingzu
 *
 */
public class TrafficQueryLogHelper {

	private TrafficQueryFragment mQueryFragment;
	
	boolean logForTabChange = true;
	
	public TrafficQueryLogHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}
	
	public void checkedRadioButton(int checkId) {
		logForTabChange = false;
		mQueryFragment.mRadioGroup.check(checkId);
		logForTabChange = true;
	}
	
	/*
	 * methods for actionlog
	 */
	public void logForClickOnEditText(QueryEditText edt) {
		if (edt == mQueryFragment.mStart) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficStartEdt);
		} else if (edt == mQueryFragment.mEnd) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficEndEdt);
		} else if (edt == mQueryFragment.mBusline) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficLineEdt);
		}
	}
	
	public void logForClickBookmarkOnEditText(QueryEditText edt) {
		if (edt == mQueryFragment.mStart) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficStartBookmarkBtn);
		} else if (edt == mQueryFragment.mEnd) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficEndBookmarkBtn);
		}
	}
	
	public void logForClickDeleteOnEditText(QueryEditText edt) {
		if (edt == mQueryFragment.mStart) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficStartDeleteBtn);
		} else if (edt == mQueryFragment.mEnd) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficEndDeleteBtn);
		}
	}
	
	public void logForSuggestDispatch(QueryEditText edt, int index) {
		if (TextUtils.isEmpty(edt.getEdt().getText().toString())) {
			logForHistoryWordSelected(edt, index);
		} else {
			SuggestAndHistoryAdapter adapter = (SuggestAndHistoryAdapter)mQueryFragment.mSuggestLsv.getAdapter();
			int hisWordCount = adapter.getHistoryWordsList().size();
			int sugWordCount = adapter.getSuggestWordsList().size();
			if (index < hisWordCount) {
				logForSuggestWordSelected(edt, 0, index);
			} else if ((index - hisWordCount) < sugWordCount){
				logForSuggestWordSelected(edt, 1, index - hisWordCount);
			}
		}
	}
	
	public void logForHistoryWordSelected(QueryEditText edt, int index) {
		if (edt == mQueryFragment.mStart) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHistoryAsStart, index);
		} else if (edt == mQueryFragment.mEnd) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHistoryAsEnd, index);
		} else if (edt == mQueryFragment.mBusline) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHistoryAsLine, index);
		}
	}
	
	public void logForSuggestWordSelected(QueryEditText edt, int type, int index) {
		if (edt == mQueryFragment.mStart) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficSuggestAsStart, type, index);
		} else if (edt == mQueryFragment.mEnd) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficSuggestAsEnd, type, index);
		} else if (edt == mQueryFragment.mBusline) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficSuggestAsLine, type, index);
		}
	}
	
	public void logForSelectPoint(int index) {
		if (mQueryFragment.mSelectedEdt == mQueryFragment.mStart) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficClickAsStart);
		} else if (mQueryFragment.mSelectedEdt == mQueryFragment.mEnd) {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficClickAsEnd);
		}
	}
}

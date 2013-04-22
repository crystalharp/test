package com.tigerknows.ui.traffic;

import android.text.TextUtils;

import com.tigerknows.common.ActionLog;
import com.tigerknows.model.TKWord;
import com.tigerknows.ui.traffic.TrafficQueryFragment.QueryEditText;
import com.tigerknows.widget.SuggestArrayAdapter;

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
			mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficStartEdt);
		} else if (edt == mQueryFragment.mEnd) {
            mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficEndEdt);
		} else if (edt == mQueryFragment.mBusline) {
            mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficBusLineEdt);
		}
	}
	
	public void logForClickBookmarkOnEditText(QueryEditText edt) {
		if (edt == mQueryFragment.mStart) {
            mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficStartBtn);
		} else if (edt == mQueryFragment.mEnd) {
            mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag +  ActionLog.TrafficEndBtn);
		}
	}
	
	public int logForSuggestDispatch(QueryEditText edt, int index) {
		if (TextUtils.isEmpty(edt.getEdt().getText().toString())) {
			return index;
		} else {
			SuggestArrayAdapter adapter = (SuggestArrayAdapter)mQueryFragment.mSuggestLsv.getAdapter();
			TKWord tkWord =  adapter.getItem(index);
			if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
			    return index;
			} else if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST){
			    int hisWordCount = 0;
			    for(int i = 0, size = adapter.getCount(); i < size; i++) {
			        TKWord temp =  adapter.getItem(i);
			        if (temp.attribute == TKWord.ATTRIBUTE_HISTORY) {
			            hisWordCount++;
			        } else {
			            break;
			        }
			    }
			    return index - hisWordCount;
			}
		}
        return index;
	}
}

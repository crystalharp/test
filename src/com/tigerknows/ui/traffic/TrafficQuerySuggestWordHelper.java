package com.tigerknows.ui.traffic;


import android.content.Context;
import android.widget.ListView;

import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.widget.SuggestWordListManager;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制历史词及联想词]]
 * @author linqingzu
 * @comment xupeng
 * 这个类经过了一番清理，已经比较精简，如果需要进一步精简则在交通重构的时候根据需求清理掉
 */
public class TrafficQuerySuggestWordHelper {
    
    static final int TYPE_TRAFFIC = HistoryWordTable.TYPE_TRAFFIC;
    static final int TYPE_BUSLINE = HistoryWordTable.TYPE_BUSLINE;
    
    static final String TAG = "TrafficQueryFragment";

	TrafficQueryFragment mQueryFragment;
	
	ListView mSuggestLsv;
	
	SuggestWordListManager mSuggestListManager;
	
	InputBtnEventHandler inputBtnHandler = new InputBtnEventHandler();
	
	public TrafficQuerySuggestWordHelper(Context context, TrafficQueryFragment queryFragment, ListView listView) {
		mQueryFragment = queryFragment;
		mSuggestLsv = listView;
		mSuggestListManager = new SuggestWordListManager(mQueryFragment.mSphinx, mSuggestLsv, mQueryFragment.mStart.getEdt(), inputBtnHandler, HistoryWordTable.TYPE_TRAFFIC);
	}
    
	/**
	 * 右侧输入按钮的点击响应事件
	 * @author xupeng
	 * 
	 */
    private class InputBtnEventHandler implements BtnEventHandler {
        
        @Override
        public void onBtnClicked(TKWord tkWord, int position) {
            if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST) {
                POI poi = tkWord.toPOI();
                Position wordLonLat = mQueryFragment.mSphinx.getMapEngine().getwordslistStringWithPosition(tkWord.word, 0);
                if (null != wordLonLat && Util.inChina(wordLonLat)) {
                    poi.setPosition(wordLonLat);
                }
                mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, position);
                mQueryFragment.mSelectedEdt.setPOI(poi);
                mQueryFragment.mSelectedEdt.getEdt().requestFocus();
                
            } else  if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, position);
                mQueryFragment.mSelectedEdt.setPOI(tkWord.toPOI());
                mQueryFragment.mSelectedEdt.getEdt().requestFocus();
            }
            mQueryFragment.mActionLog.addAction(mQueryFragment.mActionTag + ActionLog.HistoryWordInput, position, tkWord.word, tkWord.attribute);
        }
    }
	
    /**
     * 清除建议词列表的所有内容
     * @param context
     */
	public void clearSuggestList() {
	    mSuggestListManager.clearSuggestWord();
	}
	
	/**
	 * 刷新建议词列表。清除列表内容使用clearSuggestList
	 * @param context
	 * @param tkEditText 输入框
	 * @param type 建议词类型
	 */
	public void refresh(final Context context, final TKEditText tkEditText, final int type) {
	    int suggestType = HistoryWordTable.TYPE_TRAFFIC;
	    switch(type){
	    case TYPE_TRAFFIC:
	        suggestType = HistoryWordTable.TYPE_TRAFFIC;
	        break;
	    case TYPE_BUSLINE:
	        suggestType = HistoryWordTable.TYPE_BUSLINE;
	        break;
	    }
	    mSuggestListManager.refresh(tkEditText, suggestType);
	}
    
    /**
     * listview被点击的响应事件。
     */
    public void suggestSelect(POI poi, int index) {
        
        mQueryFragment.mSelectedEdt.setPOI(poi);
        
        if (mQueryFragment.mTrafficQueryBtn.isEnabled() || mQueryFragment.mBuslineQueryBtn.isEnabled()) {
            mQueryFragment.query();
        }
    }
}

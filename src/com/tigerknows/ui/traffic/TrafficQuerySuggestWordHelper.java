package com.tigerknows.ui.traffic;


import android.content.Context;
import android.widget.ListView;

import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.widget.SuggestWordListManager;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制历史词及联想词]]
 * @author linqingzu
 * @comment xupeng
 * 名词定义：
 * 历史词，用户输入过的名字，history word
 * 联想词，地图引擎中取出来的带坐标的名字，associational word
 * 建议词，上面两种加起来，suggest word
 * 输入框文字改变的时候会调用到refresh函数，其中建议词的改变为先把符合输入词的历史词添入建议词列表，
 * 然后把联想词复制一份，把历史词相同的词删除再添入到建议词列表中。
 *
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

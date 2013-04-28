package com.tigerknows.ui.traffic;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ListView;

import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.widget.SuggestArrayAdapter;
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
    
    static final int TYPE_TRAFFIC = TrafficQueryFragment.TRAFFIC_MODE;
    static final int TYPE_BUSLINE = TrafficQueryFragment.BUSLINE_MODE;
    
    static final String TAG = "TrafficQueryFragment";

	TrafficQueryFragment mQueryFragment;
	
	SuggestArrayAdapter mSuggestArrayAdapter;
	
	ListView mSuggestLsv;
	
	List<TKWord> mSuggestWordList = new LinkedList<TKWord>();
	
	InputBtnEventHandler inputBtnHandler = new InputBtnEventHandler();
	
	//输入过程中匹配到的历史词表
    List<TKWord> matchedHistoryWordList = new LinkedList<TKWord>();
	
	public TrafficQuerySuggestWordHelper(Context context, TrafficQueryFragment queryFragment, ListView listView) {
		mQueryFragment = queryFragment;
		mSuggestArrayAdapter = new SuggestArrayAdapter(context, R.layout.suggest_list_item, mSuggestWordList);
		mSuggestLsv = listView;
		mSuggestArrayAdapter.setInputBtnEventHandler(inputBtnHandler);
		listView.setAdapter(mSuggestArrayAdapter);
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
	    mSuggestWordList.clear();
        mSuggestArrayAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 刷新建议词列表。清除列表内容使用clearSuggestList
	 * @param context
	 * @param tkEditText 输入框
	 * @param type 建议词类型
	 */
	public void refresh(final Context context, final TKEditText tkEditText, final int type) {
	    mSuggestWordList.clear();
	    if (tkEditText != null) {
    		String searchWord = tkEditText.getText().toString();
            
            LinkedList<TKWord> suggestWordList = null;
            switch(type) {
            case TYPE_TRAFFIC:
                //FIXME:此处是否应该用clone？
                suggestWordList = HistoryWordTable.History_Word_Traffic;
                break;
            case TYPE_BUSLINE:
                suggestWordList = HistoryWordTable.History_Word_Busline;
                break;
            default:
            }
            
            if (suggestWordList == null) {
                LogWrapper.d(TAG, "suggestWordList empty, stop refresh");
                return;
            }
            if (TextUtils.isEmpty(searchWord)) {
                // 显示历史词
                if (suggestWordList.size() > 0) {
                    for (int i=0; i<suggestWordList.size() && i < HistoryWordTable.HISTORY_WORD_FIRST_SIZE; i++) {
                        mSuggestWordList.add(suggestWordList.get(i));
                    }
                    mSuggestWordList.add(TKWord.getCleanupTKWord(mQueryFragment.mContext));
                }
            } else {
                // 显示历史词及联想词
                if (mQueryFragment.mSphinx.getMapEngine().isSuggestWordsInitSuccess()) {
                    List<TKWord> historyList = filterHistoryWords(searchWord, suggestWordList);
                    mSuggestWordList.addAll(0, historyList);
                    // 获得联想词，0代表只获得交通联想词
                    mSuggestWordList.addAll(HistoryWordTable.stringToTKWord(filterAssociationalWords(MapEngine.getInstance().getwordslistString(searchWord, 0)), TKWord.ATTRIBUTE_SUGGEST));
                }
            }
            
            mSuggestArrayAdapter.key = searchWord;
	    }
        mSuggestArrayAdapter.notifyDataSetChanged();
        mSuggestLsv.setSelectionFromTop(0, 0);
	}
    
	/**
	 * 把在历史词中出现过的联想词过滤掉
	 * @param wordList 联想词列表
	 * @return
	 */
	private final List<String> filterAssociationalWords(List<String> wordList) {
	    if (wordList == null || matchedHistoryWordList.size() == 0) {
	        return wordList;
	    }
	    //如果一个词同时在历史词和联想词中，那么排除联想词显示历史词
	    for (TKWord historyWord : matchedHistoryWordList) {
	        wordList.remove(historyWord.word);
	    }
	    return wordList;
	}
    
	/**
	 * 根据输入词过滤历史词，匹配的历史词数量最大不超过HISTORY_WORD_LIST_SIZE
	 * @param searchword 输入词
	 * @param historyWordList 加入到建议词表中的历史词表
	 * @return
	 */
    private List<TKWord> filterHistoryWords(String searchword, final List<TKWord> historyWordList) {
        if (historyWordList == null || TextUtils.isEmpty(searchword)) {
            return historyWordList;
        }
        
        //因此函数调用较多，使用一个List对象来进行重用，不再每次new对象
        matchedHistoryWordList.clear();
        List<TKWord> resultList = matchedHistoryWordList;
        
        int historyNum = 0;
        for (TKWord historyWord : historyWordList) {
            //xp:输入时如果历史词和建议词混排，历史词不超过HISTORY_WORD_LIST_SIZE
            if (historyWord.word.startsWith(searchword)) {
                resultList.add(historyWord);
                historyNum++;
            }
            if (historyNum >= HistoryWordTable.HISTORY_WORD_LIST_SIZE) {
                break;
            }
        }
        return resultList;
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

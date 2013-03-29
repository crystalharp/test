package com.tigerknows.view;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ListView;

import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.view.SuggestArrayAdapter.CallBack;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制历史词及联想词]]
 * @author linqingzu
 *
 */
public class TrafficQuerySuggestHistoryHelper {
    
    static final int TYPE_TRAFFIC = 1;
    static final int TYPE_BUSLINE = 2;

	TrafficQueryFragment mQueryFragment;
	
	SuggestArrayAdapter mSuggestArrayAdapter;
	
	ListView mSuggestLsv;
	
	List<TKWord> mTKWordList = new ArrayList<TKWord>();
	
	public TrafficQuerySuggestHistoryHelper(Context context, TrafficQueryFragment queryFragment, ListView listView) {
		mQueryFragment = queryFragment;
		mSuggestArrayAdapter = new SuggestArrayAdapter(context, R.layout.suggest_list_item, mTKWordList);
		mSuggestLsv = listView;
		mSuggestArrayAdapter.setCallBack(new CallBack() {
            
            @Override
            public void onItemClicked(TKWord tkWord, int position) {
                if (tkWord.attribute == TKWord.ATTRIBUTE_SUGGEST) {
                    POI poi = tkWord.toPOI();
                    Position wordLonLat = mQueryFragment.mSphinx.getMapEngine().getwordslistStringWithPosition(tkWord.word, 0);
                    if (null != wordLonLat && Util.inChina(wordLonLat)) {
                        poi.setPosition(wordLonLat);
                    }
                    mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, position);
                    mQueryFragment.mSelectedEdt.setPOI(poi);
                    
                } else  if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                    mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, position);
                    mQueryFragment.mSelectedEdt.setPOI(tkWord.toPOI());
                }
            }
        });
		listView.setAdapter(mSuggestArrayAdapter);
	}

	public void check() {
    	int cityId = mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId();
    	mQueryFragment.mSphinx.getMapEngine().suggestwordCheck(mQueryFragment.mSphinx, cityId);
        HistoryWordTable.readHistoryWord(mQueryFragment.mContext, cityId, HistoryWordTable.TYPE_TRAFFIC);
        HistoryWordTable.readHistoryWord(mQueryFragment.mContext, cityId, HistoryWordTable.TYPE_BUSLINE);
    }
	
	public void refresh(final Context context, final TKEditText tkEditText, final int type) {
	    mTKWordList.clear();
	    if (tkEditText != null) {
    		String searchWord = tkEditText.getText().toString();
            
            LinkedList<TKWord> tmp = new LinkedList<TKWord>();
            switch(type) {
            case TYPE_TRAFFIC:
                tmp = getHistoryWordList(HistoryWordTable.History_Word_Traffic);
                break;
            case TYPE_BUSLINE:
                tmp = getHistoryWordList(HistoryWordTable.History_Word_Busline);
                break;
            default:
            }
            int historyNum=0;
            if (TextUtils.isEmpty(searchWord)) {
                // 显示历史词
                if (tmp != null && tmp.size() > 0) {
                    for (int i=0; i<tmp.size() && i < HistoryWordTable.HISTORY_WORD_FIRST_SIZE; i++) {
                        mTKWordList.add(tmp.get(i));
                        historyNum++;
                    }
                }
            } else {
                // 显示历史词及联想词
                if (mQueryFragment.mSphinx.getMapEngine().isSuggestWordsInitSuccess()) {
                     // 获得联想词
                     // 0代表只获得交通联想词                  
                     mTKWordList.addAll(HistoryWordTable.stringToTKWord(filterSuggestWords(searchWord, MapEngine.getInstance().getwordslistString(searchWord, 0)), TKWord.ATTRIBUTE_SUGGEST));
                     List<TKWord> historyList = filterHistoryWords(searchWord, tmp);
                     historyNum = filterHistoryWords(searchWord, tmp).size();
                     mTKWordList.addAll(0, historyList);
                 }
            }
    
            if (historyNum > 0) {
                mTKWordList.add(TKWord.getCleanupTKWord(mQueryFragment.mContext));
            }
            
            mSuggestArrayAdapter.key = searchWord;
	    }
        mSuggestArrayAdapter.notifyDataSetChanged();
        mSuggestLsv.setSelectionFromTop(0, 0);
	}

	public void suggestSelect(POI poi, int index) {
		
        mQueryFragment.mSelectedEdt.setPOI(poi);
		
		if (mQueryFragment.mTrafficQueryBtn.isEnabled() || mQueryFragment.mBuslineQueryBtn.isEnabled()) {
			mQueryFragment.query();
		}
	}
    
    private LinkedList<TKWord> getHistoryWordList(List<TKWord> historyWordList) {
        LinkedList<TKWord> list = new LinkedList<TKWord>();
        for (int i = 0, size = historyWordList.size(); i < size; i++) {
            String word = historyWordList.get(i).word;
            if (!list.contains(word)) {
                list.add(historyWordList.get(i));
            }
        }
        return list;
    }
    
    private List<String> filterSuggestWords(String searchword, List<String> suggestWordList) {
        if (suggestWordList == null || TextUtils.isEmpty(searchword)) {
            return suggestWordList;
        }
        
        suggestWordList.remove(searchword);
        return suggestWordList;
    }
    
    private List<TKWord> filterHistoryWords(String searchword, List<TKWord> historyWordList) {
        if (historyWordList == null || TextUtils.isEmpty(searchword)) {
            return historyWordList;
        }
        
        List<TKWord> resultList = new LinkedList<TKWord>();
        
        int historyIndex = 0;
        for (TKWord historyWord : historyWordList) {
            if (historyIndex >= HistoryWordTable.HISTORY_WORD_LIST_SIZE) {
                 break;
            }
            if (historyWord.word.startsWith(searchword) && !historyWord.equals(searchword)) {
                // 若历史词及联想词都匹配, 排除历史词 
                if (!mTKWordList.contains(historyWord)) {
                    resultList.add(historyWord);
                    historyIndex++;
                }
            }
        }
        return resultList;
    }
}

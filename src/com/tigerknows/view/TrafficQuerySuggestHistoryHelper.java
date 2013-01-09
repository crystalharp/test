package com.tigerknows.view;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.android.location.Position;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
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

	private TrafficQueryFragment mQueryFragment;
	
	ListView mListView;

	SuggestArrayAdapter mAdapter;
	
	List<TKWord> suggestWordList = new ArrayList<TKWord>();
	
	public TrafficQuerySuggestHistoryHelper(Context context, TrafficQueryFragment queryFragment, ListView listView) {
		super();
		// TODO Auto-generated constructor stub
		this.mQueryFragment = queryFragment;
		mAdapter = new SuggestArrayAdapter(context, R.layout.suggest_list_item, suggestWordList);
		mAdapter.setCallBack(new CallBack() {
            
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
		mListView = listView;
		mListView.setAdapter(mAdapter);
	}

	public void checkSuggestAndHistory() {
    	int cityId = mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId();
    	mQueryFragment.mSphinx.getMapEngine().suggestwordCheck(mQueryFragment.mSphinx, cityId);
        HistoryWordTable.readHistoryWord(mQueryFragment.mContext, cityId, HistoryWordTable.TYPE_TRAFFIC);
        HistoryWordTable.readHistoryWord(mQueryFragment.mContext, cityId, HistoryWordTable.TYPE_BUSLINE);
    }
	
	public void refresh(final Context context, final EditText mSuggestSrc, final int type) {
		String searchWord = mSuggestSrc.getText().toString();
        suggestWordList.clear();
        
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
                    suggestWordList.add(tmp.get(i));
                    historyNum++;
                }
            }
        } else {
            // 显示历史词及联想词
            if (mQueryFragment.mSphinx.getMapEngine().isSuggestWordsInitSuccess()) {
                 // 获得联想词
                 // 0代表只获得交通联想词                  
                 suggestWordList.addAll(HistoryWordTable.stringToTKWord(filterSuggestWords(searchWord, MapEngine.getInstance().getwordslistString(searchWord, 0)), TKWord.ATTRIBUTE_SUGGEST));
                 List<TKWord> historyList = filterHistoryWords(searchWord, tmp);
                 historyNum = filterHistoryWords(searchWord, tmp).size();
                 suggestWordList.addAll(0, historyList);
             }
        }
        
        if (historyNum > 0) {
            suggestWordList.add(TKWord.getCleanupTKWord(mQueryFragment.mContext));
        }
        
        mAdapter.key = searchWord;
        mAdapter.notifyDataSetChanged();
		mListView.setVisibility(View.VISIBLE);
	}

	public void suggestSelect(POI poi, int index) {
		
        mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, index);
        mQueryFragment.mSelectedEdt.setPOI(poi);
		
		if (mQueryFragment.mTrafficQueryBtn.isEnabled() || mQueryFragment.mBuslineQueryBtn.isEnabled()) {
			mQueryFragment.query();
		}
	}
    
    public LinkedList<TKWord> getHistoryWordList(List<TKWord> historyWordList) {
        LinkedList<TKWord> list = new LinkedList<TKWord>();
        for (int i = 0, size = historyWordList.size(); i < size; i++) {
            String word = historyWordList.get(i).word;
            if (!list.contains(word)) {
                list.add(historyWordList.get(i));
            }
        }
        return list;
    }
    
    public List<String> filterSuggestWords(String searchword, List<String> suggestWordList) {
        if (suggestWordList == null || TextUtils.isEmpty(searchword)) {
            return suggestWordList;
        }
        
        suggestWordList.remove(searchword);
        return suggestWordList;
    }
    
    public List<TKWord> filterHistoryWords(String searchword, List<TKWord> historyWordList) {
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
                if (!suggestWordList.contains(historyWord)) {
                    resultList.add(historyWord);
                    historyIndex++;
                }
            }
        }
        return resultList;
    }
}

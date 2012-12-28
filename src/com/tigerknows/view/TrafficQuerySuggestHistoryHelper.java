package com.tigerknows.view;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.POI;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制历史词及联想词]]
 * @author linqingzu
 *
 */
public class TrafficQuerySuggestHistoryHelper {

	private TrafficQueryFragment mQueryFragment;

	SuggestAndHistoryAdapter mAdapter;
	
	public TrafficQuerySuggestHistoryHelper(Context context, TrafficQueryFragment queryFragment) {
		super();
		// TODO Auto-generated constructor stub
		this.mQueryFragment = queryFragment;
		mAdapter = new SuggestAndHistoryAdapter(context, R.layout.string_list_item);
	}

	public void checkSuggestAndHistory() {
    	int cityId = mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId();
    	mQueryFragment.mSphinx.getMapEngine().suggestwordCheck(mQueryFragment.mSphinx, cityId);
        TKConfig.readHistoryWord(mQueryFragment.mContext, TKConfig.History_Word_Traffic, String.format(TKConfig.PREFS_HISTORY_WORD_TRAFFIC, cityId));
        TKConfig.readHistoryWord(mQueryFragment.mContext, TKConfig.History_Word_Busline, String.format(TKConfig.PREFS_HISTORY_WORD_BUSLINE, cityId));
    }
	
	public void setAdapterForSuggest(final Context context, final EditText mSuggestSrc, final ListView mSuggestDst, 
			final int type) {
		String searchWord = mSuggestSrc.getText().toString();
		mAdapter.refresh(searchWord, type);
		mSuggestDst.setAdapter(mAdapter);
		mSuggestDst.setVisibility(View.VISIBLE);
	}

	public void suggestSelect(POI poi, int index) {
		
        if (poi.getName().equals(mQueryFragment.mContext.getString(R.string.clean_history))) {
        	mQueryFragment.mActionLog.addAction(ActionLog.TrafficClearHistory);
			if (mQueryFragment.mode == TrafficQueryFragment.TRAFFIC_MODE && TKConfig.History_Word_Traffic.size() > 0) {
				TKConfig.clearHistoryWord(mQueryFragment.mContext, TKConfig.History_Word_Traffic, String.format(TKConfig.PREFS_HISTORY_WORD_TRAFFIC, mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId()));
				setAdapterForSuggest(mQueryFragment.mContext, mQueryFragment.mSelectedEdt.getEdt(), mQueryFragment.mSuggestLsv, SuggestAndHistoryAdapter.HISTORY_TRAFFIC_TYPE);
			} else if (mQueryFragment.mode == TrafficQueryFragment.BUSLINE_MODE && TKConfig.History_Word_Busline.size() > 0){
				TKConfig.clearHistoryWord(mQueryFragment.mContext, TKConfig.History_Word_Busline, String.format(TKConfig.PREFS_HISTORY_WORD_BUSLINE, mQueryFragment.mMapLocationHelper.getQueryCityInfo().getId()));
				setAdapterForSuggest(mQueryFragment.mContext, mQueryFragment.mSelectedEdt.getEdt(), mQueryFragment.mSuggestLsv, SuggestAndHistoryAdapter.HISTORY_BUSLINE_TYPE);
			}
		} else {
			mQueryFragment.mLogHelper.logForSuggestDispatch(mQueryFragment.mSelectedEdt, index);
			mQueryFragment.mSelectedEdt.setPOI(poi);
		}
		
		if (mQueryFragment.mTrafficQueryBtn.isEnabled() || mQueryFragment.mBuslineQueryBtn.isEnabled()) {
			mQueryFragment.query();
		}
	}
	
	protected class SuggestAndHistoryAdapter extends ArrayAdapter<String> {
		
		public static final int TEXTVIEW_RESOURCE_ID = R.layout.string_list_item;
		
		private List<String> historyWords = new LinkedList<String>();
		
		private List<String> suggestWords = new LinkedList<String>();
		
		public static final int HISTORY_TRAFFIC_TYPE = 1;
		
		public static final int HISTORY_BUSLINE_TYPE = 2;
		
		public static final int HISTORY_ENTRY = 1;
		
		public static final int SUGGEST_ENTRY = 2;
		
		public List<String> getHistoryWordsList() {
			return historyWords;
		}
		
		public List<String> getSuggestWordsList() {
			return suggestWords;
		}
		
		public SuggestAndHistoryAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			// TODO Auto-generated constructor stub
		}
		
		public void refresh(String input, int hisType) {
			historyWords.clear();
			suggestWords.clear();
			
			LinkedList<String> tmp = new LinkedList<String>();
			switch(hisType) {
        	case HISTORY_TRAFFIC_TYPE:
        		tmp = getHistoryWordList(TKConfig.History_Word_Traffic);
        		break;
        	case HISTORY_BUSLINE_TYPE:
        		tmp = getHistoryWordList(TKConfig.History_Word_Busline);
        		break;
        	default:
        	}
			
			if (TextUtils.isEmpty(input)) {
				// 显示历史词
				if (tmp != null && tmp.size() > 0) {
	                for (int i=0; i<tmp.size() && i < TKConfig.HISTORY_WORD_FIRST_SIZE; i++) {
	                	historyWords.add(tmp.get(i));
	                }
	                historyWords.add(mQueryFragment.mContext.getString(R.string.clean_history));
				}
			} else {
				// 显示历史词及联想词
				if (mQueryFragment.mSphinx.getMapEngine().isSuggestWordsInitSuccess()) {
	                 // 获得联想词
	                 // 0代表只获得交通联想词					 
					 suggestWords.addAll(filterSuggestWords(input, MapEngine.getInstance().getwordslistString(input, 0)));
	                 historyWords.addAll(filterHistoryWords(input, tmp));
	             }
			}
			
			notifyDataSetChanged();
		}
		
		public LinkedList<String> getHistoryWordList(List<String> historyWordList) {
			LinkedList<String> list = new LinkedList<String>();
	        for (String str : historyWordList) {
	            if (!list.contains(str)) {
	                list.add(str.replaceAll("\n", " "));
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
		
		public List<String> filterHistoryWords(String searchword, List<String> historyWordList) {
	        if (historyWordList == null || TextUtils.isEmpty(searchword)) {
	            return historyWordList;
	        }
	        
	        List<String> resultList = new LinkedList<String>();
	        
	        int historyIndex = 0;
	        for (String historyWord : historyWordList) {
	            if (historyIndex >= TKConfig.HISTORY_WORD_LIST_SIZE) {
	                 break;
	            }
	            if (historyWord.startsWith(searchword) && !historyWord.equals(searchword)) {
	            	// 若历史词及联想词都匹配, 排除历史词 
	            	if (!suggestWords.contains(historyWord)) {
	            		resultList.add(historyWord);
	            		historyIndex++;
	            	}
	            }
	        }
	        return resultList;
	    }

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return historyWords.size() + suggestWords.size();
		}

		public int getItemType(int position) {
			// TODO Auto-generated method stub
			if (position < historyWords.size()) 
				return HISTORY_ENTRY;
			else if ((position - historyWords.size()) < suggestWords.size())
				return SUGGEST_ENTRY;
			
			return SUGGEST_ENTRY;
		}

		@Override
		public String getItem(int position) {
			// TODO Auto-generated method stub
			if (position < historyWords.size()) 
				return historyWords.get(position);
			else if ((position - historyWords.size()) < suggestWords.size())
				return suggestWords.get(position - historyWords.size()); 
			
			return super.getItem(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub

			if (convertView == null) {
				convertView = mQueryFragment.mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
	        }
	        
	        final TextView textView = (TextView)convertView.findViewById(R.id.text_txv);
	        textView.setText(getItem(position));
	        
			return convertView;
		}
		
	}
}

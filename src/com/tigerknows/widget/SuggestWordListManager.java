package com.tigerknows.widget;

import java.util.LinkedList;
import java.util.List;

import android.widget.ListView;

import com.decarta.Globals;
import com.tigerknows.Sphinx;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;

/**
 * 历史词建议词管理器
 * @author xupeng
 * 
 * 该类直接给传进来的ListView布上数据，不再对外暴露Adapter等中间过程。
 * 
 * 由于交通部分换乘和公交是同一个listview，切换模式的时候编辑框和历史词类型都会变化，
 * 所以设置了带有参数的refresh来对不同的编辑框和建议词类型刷新数据
 */

public class SuggestWordListManager {
    
    TKEditText mWatchingEdt;
    
    List<TKWord> mSuggestWordList = new LinkedList<TKWord>();
    
    SuggestArrayAdapter mSuggestAdapter;
    
    Sphinx mSphinx;
    
    ListView mSuggestLsv;
    
    BtnEventHandler mBtnEventHandler;
    
    int mSuggestType;
    
    public SuggestWordListManager(Sphinx sphinx, ListView suggestLsv, TKEditText watchingEdit, BtnEventHandler btnHandler, int type){
        mWatchingEdt = watchingEdit;
        mSuggestLsv = suggestLsv;
        mSphinx = sphinx;
        mSuggestType = type;
        mSuggestAdapter = new SuggestArrayAdapter(mSphinx, SuggestArrayAdapter.TEXTVIEW_RESOURCE_ID, mSuggestWordList);
        mSuggestLsv.setAdapter(mSuggestAdapter);
        mSuggestAdapter.setInputBtnEventHandler(btnHandler);
    }
    
    public void clearSuggestWord(){
        mSuggestWordList.clear();
        mSuggestAdapter.notifyDataSetChanged();
    }
    
    public void refresh(final TKEditText tkEditText, final int type){
        mWatchingEdt = tkEditText;
        mSuggestType = type;
        refresh();
    }
    
    public void refresh(){
        String key = mWatchingEdt.getText().toString();
        updateSuggestWord(mSphinx, mSuggestWordList, key);
        mSuggestAdapter.key = key;
        mSuggestAdapter.notifyDataSetChanged();
        mSuggestLsv.setSelectionFromTop(0, 0);
    }
    
    private void updateSuggestWord(Sphinx sphinx, List<TKWord> tkWordList, String searchWord) {
        //xupeng:为何要clear两次？
        tkWordList.clear();
        MapEngine mapEngine = MapEngine.getInstance();
        mapEngine.suggestwordCheck(sphinx, Globals.g_Current_City_Info.getId());
        tkWordList.clear();
        tkWordList.addAll(HistoryWordTable.generateSuggestWordList(sphinx, searchWord, mSuggestType));
    }
}

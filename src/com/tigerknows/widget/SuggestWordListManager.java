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
 * 需要5个参数，sphinx，展示用的ListView，监控的输入框，右侧按钮的处理函数，联想词类型
 * 根据输入框的内容和联想词类型去更新ListView，中间过程不再需要类外的内容干涉。
 * 
 * 由于交通部分换乘和公交是同一个listview，切换模式的时候编辑框和历史词类型都会变化，
 * 所以设置了带有参数的refresh来对不同的编辑框和建议词类型刷新数据
 */

public class SuggestWordListManager {
    
    //需要监控内容的输入框
    TKEditText mWatchingEdt;
    
    List<TKWord> mSuggestWordList = new LinkedList<TKWord>();
    
    SuggestArrayAdapter mSuggestAdapter;
    
    Sphinx mSphinx;
    
    //用来显示数据的listview
    ListView mSuggestLsv;
    
    //数据列表中右边的输入按钮的响应函数接口
    BtnEventHandler mBtnEventHandler;
    
    //建议词类别，与HistoryWordTable相同
    int mSuggestType;
    
    public SuggestWordListManager(Sphinx sphinx, ListView suggestLsv, TKEditText watchingEdit, BtnEventHandler btnHandler, int type){
        mWatchingEdt = watchingEdit;
        mSuggestLsv = suggestLsv;
        mSphinx = sphinx;
        mSuggestType = type;
        mSuggestAdapter = new SuggestArrayAdapter(mSphinx, mSuggestWordList);
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
        tkWordList.clear();
        MapEngine mapEngine = MapEngine.getInstance();
        mapEngine.suggestwordCheck(sphinx, Globals.g_Current_City_Info.getId());
        tkWordList.addAll(HistoryWordTable.generateSuggestWordList(sphinx, searchWord, mSuggestType));
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.widget.TKEditText;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.SuggestArrayAdapter;
import com.tigerknows.widget.SuggestArrayAdapter.CallBack;

/**
 * @author Peng Wenyue
 */
public class InputSearchFragment extends BaseFragment implements View.OnClickListener {
    
    public InputSearchFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private Button mQueryBtn = null;

    private TKEditText mKeywordEdt = null;

    private SuggestArrayAdapter mSuggestAdapter;

    private ListView mSuggestLsv = null;
    
    private List<TKWord> mSuggestWordList = new ArrayList<TKWord>();
    
    private final TextWatcher mFindEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String key = mKeywordEdt.getText().toString();
            makeSuggestWord(mSphinx, mSuggestWordList, key);
            mSuggestAdapter.key = key;
            mSuggestAdapter.notifyDataSetChanged();
        }

        public void afterTextChanged(Editable s) {
            if (s.toString().trim().length() > 0) {
                mQueryBtn.setEnabled(true);
            } else {
                mQueryBtn.setEnabled(false);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POIHomeInputQuery;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_input_search, container, false);

        findViews();
        setListener();
        
        mSuggestAdapter = new SuggestArrayAdapter(mContext, SuggestArrayAdapter.TEXTVIEW_RESOURCE_ID, mSuggestWordList);
        mSuggestAdapter.setCallBack(new CallBack() {
            
            @Override
            public void onInputBtnClicked(TKWord tkWord, int position) {
                mKeywordEdt.setText(tkWord.word);
            }
        });
        mSuggestLsv.setAdapter(mSuggestAdapter);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.search);
        mRightBtn.setVisibility(View.INVISIBLE);

        reset();
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeywordEdt.clearFocus();
    }

    protected void findViews() {
        mQueryBtn = (Button)mRootView.findViewById(R.id.query_btn);
        mKeywordEdt = (TKEditText)mRootView.findViewById(R.id.keyword_edt);
        mKeywordEdt.mActionTag = mActionTag;
        mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
        mKeywordEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            //xupeng:参考来修改输入法的键位
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    submitQuery();
                    return true;
                }
                return false;
            }
        });
    }

    protected void setListener() {
        mQueryBtn.setOnClickListener(this);
        mKeywordEdt.addTextChangedListener(mFindEdtWatcher);
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSphinx.showSoftInput(mKeywordEdt.getInput());
                }
                return false;
            }
        });
        
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistoryClear);
                    HistoryWordTable.clearHistoryWord(mSphinx, Globals.g_Current_City_Info.getId(), HistoryWordTable.TYPE_POI);
                    String key = mKeywordEdt.getText().toString();
                    makeSuggestWord(mSphinx, mSuggestWordList, key);
                    mSuggestAdapter.key = key;
                    mSuggestAdapter.notifyDataSetChanged();
                } else {
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistory, position, tkWord.word);
                    } else {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemSuggest, position, tkWord.word);
                    }
                    mKeywordEdt.setText(tkWord.word); //处理光标问题
                    submitQuery();
                }
            }
        });
        mSuggestLsv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSphinx.hideSoftInput();
                    mSuggestLsv.requestFocus();
                }
                return false;
            }
        });
    }

    public void onClick(View view) {
        switch (view.getId()) {
                
            case R.id.query_btn:
                submitQuery();
                break;
            default:
        }
    }
    
    private void submitQuery() {
        String keyword = mKeywordEdt.getText().toString().trim();
        if (!TextUtils.isEmpty(keyword)) {
            mSphinx.hideSoftInput(mKeywordEdt.getInput());
            int cityId = Globals.g_Current_City_Info.getId();
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), cityId, HistoryWordTable.TYPE_POI);
            mActionLog.addAction(mActionTag +  ActionLog.POIHomeInputQueryBtn, keyword);

            DataQuery poiQuery = new DataQuery(mContext);
            POI requestPOI = mSphinx.getPOI();
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
            Position position = requestPOI.getPosition();
            if (position != null) {
                criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
            }
            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD_TYPE, DataQuery.KEYWORD_TYPE_INPUT);
            poiQuery.setup(criteria, cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
            mSphinx.queryStart(poiQuery);
            ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup();
            mSphinx.showView(poiQuery.getTargetViewId());
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }
    
    //还原为第一次进入的状态
    public void reset() {
        mSuggestAdapter.key = null;
        mKeywordEdt.setText(null);
        mSphinx.showSoftInput(mKeywordEdt.getInput());
        mKeywordEdt.getInput().requestFocus();
        
        makeSuggestWord(mSphinx, mSuggestWordList, mKeywordEdt.getText().toString());
        mSuggestAdapter.notifyDataSetChanged();
        mQueryBtn.setEnabled(false);
    }
    
    public static void makeSuggestWord(Sphinx sphinx, List<TKWord> tkWordList, String searchWord) {
        tkWordList.clear();
        MapEngine mapEngine = MapEngine.getInstance();
        mapEngine.suggestwordCheck(sphinx, Globals.g_Current_City_Info.getId());
        tkWordList.clear();
        List<String> list = mapEngine.getwordslistString(searchWord, 2);
        
        if (list != null && list.size() > 0) {
            tkWordList.addAll(HistoryWordTable.mergeTKWordList(HistoryWordTable.stringToTKWord(list, TKWord.ATTRIBUTE_SUGGEST), searchWord, HistoryWordTable.TYPE_POI));
        } else {
            tkWordList.addAll(HistoryWordTable.getHistoryWordList(searchWord, HistoryWordTable.TYPE_POI));
            if (tkWordList.size() > 0) {
                tkWordList.add(TKWord.getCleanupTKWord(sphinx));
            }
        }
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.maps.MapEngine.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.view.SuggestArrayAdapter.CallBack;

/**
 * @author Peng Wenyue
 */
public class POIQueryFragment extends BaseFragment implements View.OnClickListener {
    
    public POIQueryFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private ImageButton mQueryBtn = null;

    private EditText mKeywordEdt = null;

    private String mKeyword;
    
    private SuggestArrayAdapter mSuggestAdapter;

    private ListView mSuggestLsv = null;
    
    private List<TKWord> mSuggestWordList = new ArrayList<TKWord>();
    
    private Runnable mShowSoftInput = new Runnable() {
        
        @Override
        public void run() {
            mKeywordEdt.requestFocus();
            mSphinx.showSoftInput(mKeywordEdt);
        }
    };
    
    private final TextWatcher mFindEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            showHistoryAndSuggestWord();
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
        mActionTag = ActionLog.SearchInput;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_query, container, false);

        findViews();
        setListener();
        
        mSuggestAdapter = new SuggestArrayAdapter(mContext, SuggestArrayAdapter.TEXTVIEW_RESOURCE_ID, mSuggestWordList);
        mSuggestAdapter.setCallBack(new CallBack() {
            
            @Override
            public void onItemClicked(String text) {
                mKeywordEdt.setText(text);
            }
        });
        mSuggestLsv.setAdapter(mSuggestAdapter);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleTxv.setText(R.string.search);
        mRightTxv.setVisibility(View.INVISIBLE);
        
        if (mSphinx.mSnapMap) {
            if (mSphinx.mIntoSnap == 0) {
                mLeftBtn.setVisibility(View.INVISIBLE);
                mLeftTxv.setText(R.string.home);
            }
            mSphinx.mIntoSnap++;
        }
        
        mKeywordEdt.clearFocus();
        mSphinx.getHandler().post(mShowSoftInput);
        showHistoryAndSuggestWord();
        mQueryBtn.setEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        clearState();
        mSphinx.hideSoftInput(mKeywordEdt.getWindowToken());
    }

    protected void findViews() {
        mQueryBtn = (ImageButton)mRootView.findViewById(R.id.query_btn);
        mKeywordEdt = (EditText)mRootView.findViewById(R.id.keyword_edt);
        mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
        mKeywordEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
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
                    mSphinx.showSoftInput(mKeywordEdt);
                    showHistoryAndSuggestWord();
                }
                return false;
            }
        });
        
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.type == TKWord.TYPE_CLEANUP) {
                    mActionLog.addAction(ActionLog.SearchInputCleanHistory);
                    TKConfig.clearHistoryWord(mContext, TKConfig.History_Word_POI, String.format(TKConfig.PREFS_HISTORY_WORD_POI, Globals.g_Current_City_Info.getId()));
                    showHistoryAndSuggestWord();
                } else {
                    if (tkWord.type == TKWord.TYPE_HISTORY) {
                        mActionLog.addAction(ActionLog.SearchInputHistoryWord, tkWord.word, position);
                    } else {
                        mActionLog.addAction(ActionLog.SearchInputSuggestWord, tkWord.word, position);
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
                    mSphinx.hideSoftInput(mKeywordEdt.getWindowToken());
                    mQueryBtn.requestFocus();
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
    
    //called by onTouch and onTextChanged event
    private void showHistoryAndSuggestWord() {
        mSphinx.getMapEngine().suggestwordCheck(mSphinx, Globals.g_Current_City_Info.getId());
        String searchWord = mKeywordEdt.getText().toString();
        mSuggestWordList.clear();
        List<String> list = mSphinx.getMapEngine().getwordslistString(searchWord, 2);
        
        if (list != null && list.size() > 0) {
            mSuggestWordList.addAll(TKConfig.mergeHistoryAndSuggestWord(list, searchWord, TKConfig.History_Word_POI));
        } else {
            mSuggestWordList.addAll(TKConfig.getHistoryWordList(TKConfig.History_Word_POI, searchWord));
            if (mSuggestWordList.size() > 0) {
                mSuggestWordList.add(new TKWord(TKWord.TYPE_CLEANUP, mContext.getString(R.string.clean_history)));
            }
        }
        mSuggestAdapter.notifyDataSetChanged();
        mSuggestLsv.setVisibility(View.VISIBLE);
    }
    
    private void submitQuery() {
        mKeyword = mKeywordEdt.getText().toString().trim();
        if (!TextUtils.isEmpty(mKeyword)) {
            int cityId = Globals.g_Current_City_Info.getId();
            TKConfig.addHistoryWord(mContext, TKConfig.History_Word_POI, String.format(TKConfig.PREFS_HISTORY_WORD_POI, cityId), mKeyword);
            mSphinx.hideSoftInput(mKeywordEdt.getWindowToken());
            mActionLog.addAction(ActionLog.SearchInputSubmit, mKeyword);

            DataQuery poiQuery = new DataQuery(mContext);
            POI requestPOI = mSphinx.getPOI();
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD, mKeyword);
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
    public void clearState() {
        mKeywordEdt.setText("");
    }
}

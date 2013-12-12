/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;
import com.tigerknows.widget.SuggestWordListManager;

/**
 * @author Peng Wenyue
 */
public class InputSearchFragment extends BaseFragment implements View.OnClickListener {
    
    public InputSearchFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
        
        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitQuery();
                return true;
            }
            return false;
        }
    };
    
    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mSphinx.showSoftInput(mKeywordEdt.getInput());
            }
            return false;
        }
    };

    private ListView mSuggestLsv = null;
    
    private SuggestWordListManager mSuggestWordListManager;
    
    private final TextWatcher mFindEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSuggestWordListManager.refresh();
        }

        public void afterTextChanged(Editable s) {
            if (s.toString().trim().length() > 0) {
                mRightBtn.setText(R.string.confirm);
            } else {
                mRightBtn.setText(R.string.cancel);
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
        
        BtnEventHandler a = new BtnEventHandler() {
            
            @Override
            public void onBtnClicked(TKWord tkWord, int position) {
                mKeywordEdt.setText(tkWord.word);
                mActionLog.addAction(mActionTag + ActionLog.HistoryWordInput, position, tkWord.word, tkWord.attribute);
            }
        };
        mSuggestWordListManager = new SuggestWordListManager(mSphinx, mSuggestLsv, mKeywordEdt, a, HistoryWordTable.TYPE_POI);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setVisibility(View.GONE);
        mKeywordEdt.setVisibility(View.VISIBLE);
        
        mKeywordEdt.mActionTag = mActionTag;
        mKeywordEdt.setOnEditorActionListener(mOnEditorActionListener);
        mKeywordEdt.addTextChangedListener(mFindEdtWatcher);
        mKeywordEdt.setOnTouchListener(mOnTouchListener);
        
        mRightBtn.setBackgroundResource(R.drawable.btn_title);
        mRightBtn.setOnClickListener(this);
        
        mSphinx.showSoftInput(mKeywordEdt.getInput());
        mKeywordEdt.getInput().requestFocus();
        
        if (mKeywordEdt.getText().toString().trim().length() > 0) {
            mRightBtn.setText(R.string.confirm);
        } else {
            mRightBtn.setText(R.string.cancel);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeywordEdt.clearFocus();
        mKeywordEdt.removeTextChangedListener(mFindEdtWatcher);
    }

    protected void findViews() {
        mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
    }

    protected void setListener() {
        
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistoryClear);
                    HistoryWordTable.clearHistoryWord(mSphinx, Globals.getCurrentCityInfo().getId(), HistoryWordTable.TYPE_POI);
                    mSuggestWordListManager.refresh();
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
                
            case R.id.right_btn:
                if (mKeywordEdt.getText().toString().trim().length() > 0) {
                    submitQuery();
                } else {
                    dismiss();
                }
                break;
            default:
        }
    }
    
    private void submitQuery() {
        String keyword = mKeywordEdt.getText().toString().trim();
        if (!TextUtils.isEmpty(keyword)) {
            mSphinx.hideSoftInput(mKeywordEdt.getInput());
            int cityId = Globals.getCurrentCityInfo().getId();
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), cityId, HistoryWordTable.TYPE_POI);
            mKeywordEdt.setText(null);
            mActionLog.addAction(mActionTag +  ActionLog.POIHomeInputQueryBtn, keyword);

            POI requestPOI = mSphinx.getPOI();
            POI poi = mSphinx.getPOI();
            if (poi != null) {
                requestPOI = poi;
            }
            DataQuery poiQuery = mSphinx.getHomeFragment().getDataQuery(keyword);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_EXT, DataQuery.EXT_BUSLINE);
            poiQuery.setup(cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
            mSphinx.queryStart(poiQuery);
            ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup(keyword);
            mSphinx.showView(poiQuery.getTargetViewId());
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }
    
    //还原为第一次进入的状态
    public void reset() {
        mKeywordEdt.setText(null);
    }
    
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import android.content.res.Resources;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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

/**
 * @author Peng Wenyue
 */
public class POINearbyFragment extends BaseFragment implements View.OnClickListener {
    
    public POINearbyFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private Button mCategorySearchBtn; 
    
    private Button mPrecisionSearchBtn;
    
    private View mPrecisionView;
    
    private POI mPOI = new POI();
    
    private String[] mCategoryName;
    
    private ListView mCategoryLsv = null;
    
    private ImageButton mQueryBtn = null;

    private EditText mKeywordEdt = null;

    private SuggestArrayAdapter mSuggestAdapter = null;

    private ListView mSuggestLsv = null;
    
    private List<TKWord> mSuggestWordList = new ArrayList<TKWord>();

    private final String[] mCategoryActionTag = {
            ActionLog.SearchNearbyFood, ActionLog.SearchNearbyYuLe, ActionLog.SearchNearbyBuy,
            ActionLog.SearchNearbyHotel, ActionLog.SearchNearbyLuYou, ActionLog.SearchNearbyLiRen,
            ActionLog.SearchNearbySprot, ActionLog.SearchNearbyBank, ActionLog.SearchNearbyTraffic,
            ActionLog.SearchNearbyYiLiao
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
        mActionTag = ActionLog.SearchNearby;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_nearby, container, false);
        findViews();
        setListener();
                
        Resources resources = mContext.getResources();
        mCategoryName = resources.getStringArray(R.array.home_category);
        mCategoryLsv.setAdapter(new StringArrayAdapter(mContext, mCategoryName));
        
        mSuggestAdapter = new SuggestArrayAdapter(mContext, SuggestArrayAdapter.TEXTVIEW_RESOURCE_ID, mSuggestWordList);
        mSuggestLsv.setAdapter(mSuggestAdapter);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleTxv.setText(R.string.nearby_search);
        mRightTxv.setVisibility(View.INVISIBLE);

        setState(R.id.category_search_btn);
        
        clearState();
        mQueryBtn.setEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSphinx.hideSoftInput(mKeywordEdt.getWindowToken());
    }

    protected void findViews() {
        mCategorySearchBtn = (Button) mRootView.findViewById(R.id.category_search_btn);
        mPrecisionSearchBtn = (Button) mRootView.findViewById(R.id.precision_search_btn);
        mPrecisionView = mRootView.findViewById(R.id.precision_view);
        mCategoryLsv = (ListView)mRootView.findViewById(R.id.category_lsv);
        mQueryBtn = (ImageButton)mRootView.findViewById(R.id.query_btn);
        mKeywordEdt = (EditText)mRootView.findViewById(R.id.keyword_edt);
        mSuggestLsv = (ListView)mRootView.findViewById(R.id.suggest_lsv);
    }

    protected void setListener() {
        mCategorySearchBtn.setOnClickListener(this);
        mPrecisionSearchBtn.setOnClickListener(this);
        mCategoryLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mActionLog.addAction(mCategoryActionTag[position]);
                submitQuery(mCategoryName[position], false);                
            }
        });
        mQueryBtn.setOnClickListener(this);
        mKeywordEdt.addTextChangedListener(mFindEdtWatcher);
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    showHistoryAndSuggestWord();
                }
                return false;
            }
        });
        
        mKeywordEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    submitQuery(mKeywordEdt.getText().toString().trim(), true);
                    return true;
                }
                return false;
            }
        });
        
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.type == TKWord.TYPE_CLEANUP) {
                    mActionLog.addAction(ActionLog.SearchNearbyCleanHistory);
                    TKConfig.clearHistoryWord(mContext, TKConfig.History_Word_POI, String.format(TKConfig.PREFS_HISTORY_WORD_POI, Globals.g_Current_City_Info.getId()));
                    showHistoryAndSuggestWord();
                } else {
                    if (tkWord.type == TKWord.TYPE_HISTORY) {
                        mActionLog.addAction(ActionLog.SearchNearHistoryWord, tkWord.word, position);
                    } else {
                        mActionLog.addAction(ActionLog.SearchNearSuggestWord, tkWord.word, position);
                    }
                    mKeywordEdt.setText(tkWord.word); //处理光标问题
                    submitQuery(mKeywordEdt.getText().toString().trim(), true);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.category_search_btn:
            case R.id.precision_search_btn:
                setState(view.getId());
                break;
                
            case R.id.query_btn:
                mActionLog.addAction(ActionLog.SearchNearbySubimt, mKeywordEdt.getText().toString().trim());
                submitQuery(mKeywordEdt.getText().toString().trim(), true);
                break;

            default:
                break;
        }
    }
    
    private void submitQuery(String keyword, boolean isInput) {
        if (!TextUtils.isEmpty(keyword)) {
            DataQuery poiQuery = new DataQuery(mContext);
            POI requestPOI = mPOI;
            int cityId = mSphinx.getMapEngine().getCityId(requestPOI.getPosition());
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
            Position position = requestPOI.getPosition();
            if (position != null) {
                criteria.put(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                criteria.put(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
            }
            if (isInput) {
                criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD_TYPE, DataQuery.KEYWORD_TYPE_INPUT);
                TKConfig.addHistoryWord(mContext, TKConfig.History_Word_POI, String.format(TKConfig.PREFS_HISTORY_WORD_POI, cityId), keyword);
            } else {
                criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD_TYPE, DataQuery.KEYWORD_TYPE_TAG);
            }
            poiQuery.setup(criteria, cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
            mSphinx.queryStart(poiQuery);
            ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup();
            mSphinx.showView(poiQuery.getTargetViewId());
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }

    public void setData(POI poi) {
        mPOI = poi;
    }
    
    private void showHistoryAndSuggestWord() {
        mSphinx.getMapEngine().suggestwordCheck(mSphinx, Globals.g_Current_City_Info.getId());
        String searchWord = mKeywordEdt.getText().toString();
        mSuggestWordList.clear();
        List<String> list = mSphinx.getMapEngine().getwordslistString(searchWord, 2);
        
        if (list != null && list.size() > 0) {
            mSuggestWordList.addAll(TKConfig.mergeHistoryAndSuggestWord(list, searchWord, TKConfig.History_Word_POI));
        } else {
            mSuggestWordList.addAll(TKConfig.getHistoryWordList(TKConfig.History_Word_POI, searchWord));
            if (mSuggestWordList != null && mSuggestWordList.size() > 0) {
                mSuggestWordList.add(new TKWord(TKWord.TYPE_CLEANUP, mContext.getString(R.string.clean_history)));
            }
        }
        mSuggestAdapter.notifyDataSetChanged();
        mSuggestLsv.setVisibility(View.VISIBLE);
    }
    
    //还原为第一次进入的状态
    public void clearState() {
        mKeywordEdt.setText("");
        mKeywordEdt.clearFocus();
    }
    
    private void setState(int id) {
        if (id == R.id.category_search_btn) {
            mActionLog.addAction(ActionLog.SearchNearbyCategory);
            mSphinx.hideSoftInput(mKeywordEdt.getWindowToken());
            mCategorySearchBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mPrecisionSearchBtn.setBackgroundResource(R.drawable.btn_tab);
            mCategoryLsv.setVisibility(View.VISIBLE);
            mPrecisionView.setVisibility(View.GONE);
            
        } else if (id == R.id.precision_search_btn) {
            mActionLog.addAction(ActionLog.SearchNearbyJingQue);
            mCategorySearchBtn.setBackgroundResource(R.drawable.btn_tab);
            mPrecisionSearchBtn.setBackgroundResource(R.drawable.btn_tab_focused);
            mCategoryLsv.setVisibility(View.GONE);
            mPrecisionView.setVisibility(View.VISIBLE);

            mKeywordEdt.requestFocus();
            mSphinx.showSoftInput(mKeywordEdt);
            showHistoryAndSuggestWord();
        }
    }
}

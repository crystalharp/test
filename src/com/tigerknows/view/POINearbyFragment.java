/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
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
import com.tigerknows.widget.Toast;
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
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.SuggestArrayAdapter.CallBack;

/**
 * 周边搜索界面
 * 在指定的POI周边进行搜索
 * @author Peng Wenyue
 */
public class POINearbyFragment extends BaseFragment implements View.OnClickListener {
    
    public POINearbyFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    /**
     * 在此POI周边进行搜索
     */
    private POI mPOI = new POI();
    
    private ViewPager mViewPager;
    
    private List<View> mViewList = new ArrayList<View>();
    
    private ListView mCategoryLsv = null;
    
    private Button mQueryBtn = null;

    private TKEditText mKeywordEdt = null;

    private SuggestArrayAdapter mSuggestAdapter = null;

    private ListView mSuggestLsv = null;
    
    private List<TKWord> mSuggestWordList = new ArrayList<TKWord>();
    
    private ViewGroup mPageIndicatorView;
    
    /**
     * 分类名称列表
     */
    private String[] mCategoryNames;

    /**
     * 分类图标资源Id列表
     */
    private final int[] mCategoryResIds = {R.drawable.ic_food_search_near,
        R.drawable.ic_amusement_search_near,
        R.drawable.ic_buy_search_near,
        R.drawable.ic_hotel_search_near,
        R.drawable.ic_tour_search_near,
        R.drawable.ic_beautiful_search_near,
        R.drawable.ic_sport_search_near,
        R.drawable.ic_bandk_search_near,
        R.drawable.ic_traffic_search_near,
        R.drawable.ic_hospital_search_near
        };
    
    private final TextWatcher mKeywordEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String key = mKeywordEdt.getText().toString();
            POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, key);
            mSuggestAdapter.key = key;
            refreshSuggest();
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
        mCategoryNames = resources.getStringArray(R.array.home_category);
        mCategoryLsv.setAdapter(new StringArrayAdapter(mContext, mCategoryNames, mCategoryResIds));
        
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
        mTitleBtn.setText(mSphinx.getString(R.string.at_where_search, CommonUtils.substring(mPOI.getName(), 6)));
        mRightBtn.setVisibility(View.INVISIBLE);
        
        reset();
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeywordEdt.getInput().clearFocus();
    }

    protected void findViews() {
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        mQueryBtn = (Button)mRootView.findViewById(R.id.query_btn);
        mKeywordEdt = (TKEditText)mRootView.findViewById(R.id.keyword_edt);
        
        mCategoryLsv = CommonUtils.makeListView(mSphinx, R.drawable.bg_line_split);
        mViewList.add(mCategoryLsv);
        mSuggestLsv = CommonUtils.makeListView(mSphinx, R.drawable.bg_line_split);
        mViewList.add(mSuggestLsv);
        mViewPager.setAdapter(new MyAdapter());
        
        mPageIndicatorView = (ViewGroup) mRootView.findViewById(R.id.page_indicator_view);
        CommonUtils.pageIndicatorInit(mSphinx, mPageIndicatorView, mViewList.size());
    }

    protected void setListener() {
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            
            @Override
            public void onPageSelected(int index) {
                CommonUtils.pageIndicatorChanged(mSphinx, mPageIndicatorView, index);
                if (index == 0) {
                    mActionLog.addAction(ActionLog.VIEWPAGER_SELECTED, "category");
                } else {
                    mActionLog.addAction(ActionLog.VIEWPAGER_SELECTED, "input");
                }
            }
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
            
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
        
        mCategoryLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "category", mCategoryNames[position]);
                submitQuery(mCategoryNames[position], false);                
            }
        });
        mQueryBtn.setOnClickListener(this);
        mKeywordEdt.addTextChangedListener(mKeywordEdtWatcher);
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    String key = mKeywordEdt.getText().toString();
                    POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, key);
                    mSuggestAdapter.key = key;
                    mViewPager.setCurrentItem(1);
                    refreshSuggest();
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
                if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                    mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "cleanHistoryWord");
                    HistoryWordTable.clearHistoryWord(mSphinx, Globals.g_Current_City_Info.getId(), HistoryWordTable.TYPE_POI);
                    String key = mKeywordEdt.getText().toString();
                    POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, key);
                    mSuggestAdapter.key = key;
                    refreshSuggest();
                } else {
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                        mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "historyWord", position+1, tkWord.word);
                    } else {
                        mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "suggestWord", position+1, tkWord.word);
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
                    mSphinx.hideSoftInput();
                    mViewPager.requestFocus();
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
                
            case R.id.query_btn:
                mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "search", mKeywordEdt.getText().toString().trim());
                submitQuery(mKeywordEdt.getText().toString().trim(), true);
                break;

            default:
                break;
        }
    }
    
    /**
     * 查询
     * @param keyword
     * @param isInput 是否为用户手动输入
     */
    private void submitQuery(String keyword, boolean isInput) {
        if (!TextUtils.isEmpty(keyword)) {
            mSphinx.hideSoftInput(mKeywordEdt.getInput());
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
                HistoryWordTable.addHistoryWord(mContext, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), cityId, HistoryWordTable.TYPE_POI);
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
    
    /**
     * 刷新联想词列表，当联想词列表为空时自动切换到分类列表
     */
    private void refreshSuggest() {
        mSuggestAdapter.notifyDataSetChanged();
//        if (mSuggestWordList.isEmpty()) {
//            mViewPager.setCurrentItem(0);
//        } else {
//            mViewPager.setCurrentItem(1);
//        }
    }
    
    /**
     * 将UI及内容重置为第一次进入页面时的状态
     */
    public void reset() {
        mCategoryLsv.setSelectionFromTop(0, 0);
        mSuggestLsv.setSelectionFromTop(0, 0);
        mKeywordEdt.setText(null);
        mKeywordEdt.clearFocus();
        mViewPager.setCurrentItem(0);
        mSuggestWordList.clear();
        POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, null);
        mSuggestAdapter.key = null;
        mSuggestAdapter.notifyDataSetChanged();
        mQueryBtn.setEnabled(false);
        mViewPager.requestFocus();
    }
    
    class MyAdapter extends PagerAdapter {
        
        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public void destroyItem(View contain, int position, Object arg2) {
             ((ViewPager) contain).removeView(mViewList.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup contain, int position) {
            contain.addView(mViewList.get(position));
            return mViewList.get(position);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
            // TODO Auto-generated method stub
        }

        @Override
        public Parcelable saveState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void finishUpdate(View arg0) {
            // TODO Auto-generated method stub
        }

    }
}

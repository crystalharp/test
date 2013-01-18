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
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.view.SuggestArrayAdapter.CallBack;

/**
 * @author Peng Wenyue
 */
public class POINearbyFragment extends BaseFragment implements View.OnClickListener {
    
    public POINearbyFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private POI mPOI = new POI();
    
    private String[] mCategoryName;
    
    private ViewPager mViewPager;
    
    private List<View> mViewList = new ArrayList<View>();
    
    private ListView mCategoryLsv = null;
    
    private Button mQueryBtn = null;

    private TKEditText mKeywordEdt = null;

    private SuggestArrayAdapter mSuggestAdapter = null;

    private ListView mSuggestLsv = null;
    
    private List<TKWord> mSuggestWordList = new ArrayList<TKWord>();
    
    private ViewGroup mPageIndicatorView;

    private final int[] mResIdList = {R.drawable.ic_food_search_near,
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
            String key = mKeywordEdt.getText().toString();
            POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, key);
            mSuggestAdapter.key = key;
            notifyDataSetChanged();
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
        mCategoryLsv.setAdapter(new StringArrayAdapter(mContext, mCategoryName, mResIdList));
        
        mSuggestAdapter = new SuggestArrayAdapter(mContext, SuggestArrayAdapter.TEXTVIEW_RESOURCE_ID, mSuggestWordList);
        mSuggestAdapter.setCallBack(new CallBack() {
            
            @Override
            public void onItemClicked(TKWord tkWord, int position) {
                mKeywordEdt.setText(tkWord.word);
            }
        });
        mSuggestLsv.setAdapter(mSuggestAdapter);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.nearby_search);
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
        
        mCategoryLsv = CommonUtils.makeListView(mSphinx);
        mViewList.add(mCategoryLsv);
        mSuggestLsv = CommonUtils.makeListView(mSphinx);
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
                    String key = mKeywordEdt.getText().toString();
                    POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, key);
                    mSuggestAdapter.key = key;
                    notifyDataSetChanged();
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
                    mActionLog.addAction(ActionLog.SearchNearbyCleanHistory);
                    HistoryWordTable.clearHistoryWord(mSphinx, Globals.g_Current_City_Info.getId(), HistoryWordTable.TYPE_POI);
                    String key = mKeywordEdt.getText().toString();
                    POIQueryFragment.makeSuggestWord(mSphinx, mSuggestWordList, key);
                    mSuggestAdapter.key = key;
                    notifyDataSetChanged();
                } else {
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
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
                mActionLog.addAction(ActionLog.SearchNearbySubimt, mKeywordEdt.getText().toString().trim());
                submitQuery(mKeywordEdt.getText().toString().trim(), true);
                break;

            default:
                break;
        }
    }
    
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
    
    private void notifyDataSetChanged() {
        mSuggestAdapter.notifyDataSetChanged();
        if (mSuggestWordList.isEmpty()) {
            mViewPager.setCurrentItem(0);
        } else {
            mViewPager.setCurrentItem(1);
        }
    }
    
    //还原为第一次进入的状态
    public void reset() {
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

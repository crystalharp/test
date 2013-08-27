/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.widget.TKEditText;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.StringArrayAdapter;
import com.tigerknows.widget.SuggestWordListManager;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;

/**
 * 周边搜索界面
 * 在指定的POI周边进行搜索
 * @author Peng Wenyue
 */
public class NearbySearchFragment extends BaseFragment implements View.OnClickListener {
    
    public NearbySearchFragment(Sphinx sphinx) {
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

    private ListView mSuggestLsv = null;
    
    private SuggestWordListManager mSuggestWordListManager;
    
    private ViewGroup mPageIndicatorView;
    
    private TextView mLocationTxv;
    
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
            mSuggestWordListManager.refresh();
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
        mActionTag = ActionLog.POINearbySearch;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_nearby_search, container, false);
        findViews();
        setListener();
                
        mLocationTxv.setVisibility(View.VISIBLE);
        Resources resources = mContext.getResources();
        mCategoryNames = resources.getStringArray(R.array.home_category);
        mCategoryLsv.setAdapter(new StringArrayAdapter(mContext, mCategoryNames, mCategoryResIds));
        
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
        mTitleBtn.setText(mSphinx.getString(R.string.nearby_search));
        mLocationTxv.setText(mSphinx.getString(R.string.at_where_search, mPOI.getName()));
        mRightBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeywordEdt.getInput().clearFocus();
    }

    protected void findViews() {
        mLocationTxv = (TextView) mRootView.findViewById(R.id.location_txv);
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        mQueryBtn = (Button)mRootView.findViewById(R.id.query_btn);
        mKeywordEdt = (TKEditText)mRootView.findViewById(R.id.keyword_edt);
        
        mCategoryLsv = Utility.makeListView(mSphinx, R.drawable.bg_line_split);
        mViewList.add(mCategoryLsv);
        mSuggestLsv = Utility.makeListView(mSphinx, R.drawable.bg_line_split);
        mViewList.add(mSuggestLsv);
        mViewPager.setAdapter(new MyAdapter());
        
        mPageIndicatorView = (ViewGroup) mRootView.findViewById(R.id.page_indicator_view);
        Utility.pageIndicatorInit(mSphinx, mPageIndicatorView, mViewList.size());
    }

    protected void setListener() {
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            
            @Override
            public void onPageSelected(int index) {
                Utility.pageIndicatorChanged(mSphinx, mPageIndicatorView, index);
                mActionLog.addAction(mActionTag+ActionLog.ViewPageSelected, index);
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
                mActionLog.addAction(mActionTag +  ActionLog.POINearbySearchCategory, mCategoryNames[position]);
                submitQuery(mCategoryNames[position], false);                
            }
        });
        mQueryBtn.setOnClickListener(this);
        mKeywordEdt.addTextChangedListener(mKeywordEdtWatcher);
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mActionLog.addAction(mActionTag +  ActionLog.POINearbySearchInput);
                    mSuggestWordListManager.refresh();
                    mViewPager.setCurrentItem(1);
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
                mActionLog.addAction(mActionTag +  ActionLog.POINearbySearchSubimt, mKeywordEdt.getText().toString().trim());
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
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_POI);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE, BaseQuery.SUB_DATA_TYPE_POI);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, keyword);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, requestPOI.getUUID());
            Position position = requestPOI.getPosition();
            if (position != null) {
                poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
                poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
            }
            if (isInput) {
                HistoryWordTable.addHistoryWord(mContext, new TKWord(TKWord.ATTRIBUTE_HISTORY, keyword), cityId, HistoryWordTable.TYPE_POI);
            } else {
                poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INFO, DataQuery.INFO_TYPE_TAG);
            }
            poiQuery.setup(cityId, getId(), mSphinx.getPOIResultFragmentID(), null, false, false, requestPOI);
            mSphinx.queryStart(poiQuery);
            ((POIResultFragment)mSphinx.getFragment(poiQuery.getTargetViewId())).setup(isInput ? keyword : null);
            mSphinx.showView(poiQuery.getTargetViewId());
        } else {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
        }
    }

    public void setData(POI poi) {
        reset();
        mPOI = poi;
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

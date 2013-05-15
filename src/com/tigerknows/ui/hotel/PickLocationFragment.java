/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.android.widget.TKEditText;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery.AlternativeResponse;
import com.tigerknows.model.DataQuery.AlternativeResponse.AlternativeList;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.model.DataQuery.AlternativeResponse.Alternative;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.StringArrayAdapter;
import com.tigerknows.widget.SuggestWordListManager;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;

/**
 * 选择位置界面
 * 
 * @author Peng Wenyue
 */
public class PickLocationFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack {
    
    public PickLocationFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private ViewPager mViewPager;
    
    private List<View> mViewList = new ArrayList<View>();
    
    private FilterListView mFilterListView = null;
    
    private Button mQueryBtn = null;

    private TKEditText mKeywordEdt = null;

    private ListView mSuggestLsv = null;
    
    private SuggestWordListManager mSuggestWordListManager;
    
    private ViewGroup mPageIndicatorView;
    
    private POI mPOI;
    
    private Filter mAlternativeFilter;
    
    private AlternativeResponse mAlternativeResponse;
    
    private TKWord mTKWord = new TKWord();
    
    private final TextWatcher mKeywordEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSuggestWordListManager.refresh();
            mTKWord.word = s.toString();
            mTKWord.position = null;
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
        
        mRootView = mLayoutInflater.inflate(R.layout.poi_nearby_search, container, false);
        findViews();
        setListener();
                
        BtnEventHandler a = new BtnEventHandler() {
            
            @Override
            public void onBtnClicked(TKWord tkWord, int position) {
                mTKWord = tkWord;
                mKeywordEdt.setText(tkWord.word);
            }
        };
        mSuggestWordListManager = new SuggestWordListManager(mSphinx, mSuggestLsv, mKeywordEdt, a, HistoryWordTable.TYPE_TRAFFIC);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText("选择位置");
        mRightBtn.setVisibility(View.INVISIBLE);
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
        
        mFilterListView = new FilterListView(mSphinx);
        mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);
        mViewList.add(mFilterListView);
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
        
        mQueryBtn.setOnClickListener(this);
        mKeywordEdt.addTextChangedListener(mKeywordEdtWatcher);
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mActionLog.addAction(ActionLog.POINearbySearchInput);
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
                    submit();
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
                    HistoryWordTable.clearHistoryWord(mSphinx, Globals.g_Current_City_Info.getId(), HistoryWordTable.TYPE_POI);
                    mSuggestWordListManager.refresh();
                } else {
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistory, position, tkWord.word);
                    } else {
                        tkWord.position = mSphinx.getMapEngine().getwordslistStringWithPosition(tkWord.word, 0);
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemSuggest, position, tkWord.word);
                    }
                    mTKWord = tkWord;
                    mKeywordEdt.setText(tkWord.word); //处理光标问题
                    submit();
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
                if (TextUtils.isEmpty(mKeywordEdt.getText().toString().trim())) {
                    mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
                } else {
                    submit();
                }
                break;

            default:
                break;
        }
    }
    
    /**
     * 确认
     */
    private void submit() {
        mAlternativeFilter = null;
        TKWord tkWord = mTKWord;
        String word = tkWord.word;
        if (TextUtils.isEmpty(word)) {
            return;
        } else if (tkWord.position == null) {
            mSphinx.hideSoftInput(mKeywordEdt.getInput());
            DataQuery poiQuery = new DataQuery(mContext);
            int cityId = mSphinx.getHotelHomeFragment().getCityInfo().getId();
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_ALTERNATIVE);
            criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
            criteria.put(DataQuery.SERVER_PARAMETER_KEYWORD, word);
            poiQuery.setup(criteria, cityId, getId(), getId(), mSphinx.getString(R.string.doing_and_wait), false, false, null);
            mSphinx.queryStart(poiQuery);
        } else {
            mPOI = tkWord.toPOI();
        }
    }
    
    /**
     * 将UI及内容重置为第一次进入页面时的状态
     */
    public void reset() {
        mSuggestLsv.setSelectionFromTop(0, 0);
        mTKWord.word = null;
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
    
    public void setData(List<Filter> filterList) {
        mFilterListView.setData(filterList, FilterResponse.FIELD_FILTER_AREA_INDEX, this, false, false, mActionTag);
    }

    @Override
    public void doFilter(String name) {
        mPOI = null;
        dismiss();
    }

    @Override
    public void cancelFilter() {
        
    }
    
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        Response response = dataQuery.getResponse();
        if (response != null && response.getResponseCode() == Response.RESPONSE_CODE_OK && response instanceof AlternativeResponse) {
            mAlternativeResponse = (AlternativeResponse) response;
            AlternativeList alternativeList = mAlternativeResponse.getList();
            List<String> nameList = new ArrayList<String>();
            if (alternativeList != null) {
                List<Alternative> list = alternativeList.getList();
                if (list != null && list.size() > 0) {
                    for(int i = 0, size = list.size(); i < size; i++) {
                        nameList.add(list.get(i).getName());
                    }
                }
            }
            if (nameList.isEmpty()) {
                List<Filter> filterList = dataQuery.getFilterList();

                if (filterList != null && filterList.size() == 1) {
                    long id = mAlternativeResponse.getPosition();
                    
                    List<Filter> filterListInHotelHome = mSphinx.getHotelHomeFragment().getFilterList();
                    Filter areaFilter = filterList.get(0);
                    for(int i = 0, size = filterListInHotelHome.size(); i < size; i++) {
                        Filter filter = filterListInHotelHome.get(i);
                        if (filter.getKey() == FilterResponse.FIELD_FILTER_AREA_INDEX) {
                            if (areaFilter != null) {
                                filterListInHotelHome.remove(i);
                                filterListInHotelHome.add(i, areaFilter);
                            } else {
                                areaFilter = filter;
                            }
                            break;
                        }
                    }
                    
                    if (areaFilter != null) {
                        List<Filter> list = areaFilter.getChidrenFilterList();
                        for(int i = 0, size = list.size(); i < size; i++) {
                            Filter filter = list.get(i);
                            if (filter.getFilterOption().getId() == id) {
                                mAlternativeFilter = filter;
                                break;
                            }
                            
                            List<Filter> childlist = areaFilter.getChidrenFilterList();
                            for(int j = 0, count = list.size(); j < count; j++) {
                                Filter chidrenFilter = childlist.get(j);
                                if (chidrenFilter.getFilterOption().getId() == id) {
                                    mAlternativeFilter = filter;
                                    break;
                                }
                            }
                            
                            if (mAlternativeFilter != null) {
                                break;
                            }
                        } 
                    }
                }
                
                if (mAlternativeFilter != null) {
                    nameList.add(mAlternativeFilter.getFilterOption().getName());
                }
            }
            
            if (nameList.size() > 0) {
                showAlternativeDialog(nameList);
            }
        }
        
    }
    
    private void showAlternativeDialog(final List<String> alternativeList) {
        final ArrayAdapter<String> adapter = new StringArrayAdapter(mSphinx, alternativeList);
        ListView listView = Utility.makeListView(mSphinx);
        listView.setAdapter(adapter);

        mActionLog.addAction(ActionLog.TrafficAlternative);
        final Dialog dialog = Utility.showNormalDialog(mSphinx,
                mSphinx.getString(R.string.app_name),
                null,
                listView,
                null,
                null,
                null);
        
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
                mPOI = null;
                AlternativeList alternativeList = mAlternativeResponse.getList();
                if (alternativeList != null) {
                    List<Alternative> list = alternativeList.getList();
                    if (list != null && list.size() > 0) {
                        for(int i = 0, size = list.size(); i < size; i++) {
                            mPOI = list.get(i).toPOI();
                        }
                    }
                }
                if (mPOI == null) {
                    Filter areaFilter = null;
                    List<Filter> filterListInHotelHome = mSphinx.getHotelHomeFragment().getFilterList();
                    for(int i = 0, size = filterListInHotelHome.size(); i < size; i++) {
                        Filter filter = filterListInHotelHome.get(i);
                        if (filter.getKey() == FilterResponse.FIELD_FILTER_AREA_INDEX) {
                            areaFilter = filter;
                            break;
                        }
                    }
                    
                    FilterListView.selectedFilter(areaFilter, mAlternativeFilter);
                }
                dialog.dismiss();
                dismiss();
            }
            
        });
        dialog.setOnDismissListener(new OnDismissListener() {
            
            @Override
            public void onDismiss(DialogInterface dialog) {
                mActionLog.addAction(ActionLog.TrafficAlternative + ActionLog.Dismiss);
            }
        });
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mSphinx.getHotelHomeFragment().refreshFilterArea();
    }
    
    public POI getPOI() {
        return mPOI;
    }
}

/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.hotel;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.InputFilter;
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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery.AlternativeResponse;
import com.tigerknows.model.DataQuery.FilterArea;
import com.tigerknows.model.DataQuery.AlternativeResponse.AlternativeList;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.FilterResponse;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.TKWord;
import com.tigerknows.provider.HistoryWordTable;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.FilterListView;
import com.tigerknows.widget.SuggestWordListManager;
import com.tigerknows.widget.SuggestArrayAdapter.BtnEventHandler;

/**
 * 选择位置界面
 * 
 * @author Peng Wenyue
 */
public class PickLocationFragment extends BaseFragment implements View.OnClickListener, FilterListView.CallBack {
    
    public interface Invoker {
        public List<Filter> getFilterList();
        public void setSelectedLocation(boolean selectedLocation);
        public void setPOI(POI poi);
        public void refreshFilterAreaView();
    }
    
    public PickLocationFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    private ViewPager mViewPager;
    
    private String mTitle;
    
    private List<View> mViewList = new ArrayList<View>();
    
    private FilterListView mFilterListView = null;
    
    private ListView mSuggestLsv = null;
    
    private ListView mAlternativeLsv = null;
    
    private AlternativeAdapter mAlternativeAdapter;
    
    private List<POI> mAlternativeList = new ArrayList<POI>();
    
    private SuggestWordListManager mSuggestWordListManager;
    
    private ViewGroup mPageIndicatorView;
    
    private AlternativeResponse mAlternativeResponse;
    
    private TKWord mTKWord = null;
    
    private CityInfo mCityInfo;
    
    private final TextWatcher mKeywordEdtWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSuggestWordListManager.refresh(mCityInfo.getId());
            if (mTKWord != null &&
                    s.toString().trim().equals(mTKWord.word)) {
                mTKWord = null;
            }
            mSuggestLsv.setVisibility(View.VISIBLE);
            mAlternativeLsv.setVisibility(View.GONE);
        }

        public void afterTextChanged(Editable s) {
            Utility.refreshButton(mSphinx, mRightBtn, getString(R.string.confirm), getString(R.string.cancel), s.toString().trim().length() > 0);
        }
    };
    
    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mActionLog.addAction(mActionTag+ActionLog.HotelPickLocationInput);
                mSuggestWordListManager.refresh(mCityInfo.getId());
                mViewPager.setCurrentItem(1);
            }
            return false;
        }
    };
    
    private OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
        
        @Override
        public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submit();
                return true;
            }
            return false;
        }
    };
    
    private Invoker mInvoker;
    
    public void setInvoker(Invoker invoker) {
        mInvoker = invoker;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.HotelPickLocation;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.hotel_pick_location, container, false);
        
        findViews();
        setListener();
                
        BtnEventHandler a = new BtnEventHandler() {
            
            @Override
            public void onBtnClicked(TKWord tkWord, int position) {
                mTKWord = tkWord.clone();
                mKeywordEdt.setText(tkWord.word);
                mActionLog.addAction(mActionTag + ActionLog.HistoryWordInput, position, tkWord.word, tkWord.attribute);
            }
        };

        mKeywordEdt.getInput().setFilters(new InputFilter[] { new InputFilter.LengthFilter(Integer.MAX_VALUE) });
        mSuggestWordListManager = new SuggestWordListManager(mSphinx, mSuggestLsv, mKeywordEdt, a, HistoryWordTable.TYPE_TRAFFIC);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setText(mTitle);
        mKeywordEdt.getInput().setHint(R.string.find_poi_merchant);
        mKeywordEdt.setVisibility(View.VISIBLE);
        
        mKeywordEdt.addTextChangedListener(mKeywordEdtWatcher);
        mKeywordEdt.setOnTouchListener(mOnTouchListener);
        mKeywordEdt.setOnEditorActionListener(mOnEditorActionListener);
        
        mRightBtn.setVisibility(View.VISIBLE);
        mRightBtn.setOnClickListener(this);
        Utility.refreshButton(mSphinx, mRightBtn, getString(R.string.confirm), getString(R.string.cancel), mKeywordEdt.getText().toString().trim().length() > 0);
        
        mCityInfo = mSphinx.getHotelHomeFragment().getCityInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        mKeywordEdt.getInput().clearFocus();
        mKeywordEdt.removeTextChangedListener(mKeywordEdtWatcher);
    }

    @Override
    protected void findViews() {
        super.findViews();
        mViewPager = (ViewPager) mRootView.findViewById(R.id.view_pager);
        
        mFilterListView = new FilterListView(mSphinx);
        mFilterListView.findViewById(R.id.body_view).setPadding(0, 0, 0, 0);
        mViewList.add(mFilterListView);
        mSuggestLsv = Utility.makeListView(mSphinx, R.drawable.bg_line_split);
        mAlternativeLsv = Utility.makeListView(mSphinx, R.drawable.bg_line_split);
        mAlternativeAdapter = new AlternativeAdapter(mSphinx, mAlternativeList);
        mAlternativeLsv.setAdapter(mAlternativeAdapter);
        FrameLayout frameLayout = new FrameLayout(mSphinx);
        frameLayout.addView(mSuggestLsv);
        frameLayout.addView(mAlternativeLsv);
        mViewList.add(frameLayout);
        mViewPager.setAdapter(new MyAdapter());
        
        mPageIndicatorView = (ViewGroup) mRootView.findViewById(R.id.page_indicator_view);
        Utility.pageIndicatorInit(mSphinx, mPageIndicatorView, mViewList.size());
    }

    @Override
    protected void setListener() {
        super.setListener();
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
        
        mSuggestLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                TKWord tkWord = (TKWord) arg0.getAdapter().getItem(position);
                if (tkWord.attribute == TKWord.ATTRIBUTE_CLEANUP) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistoryClear);
                    HistoryWordTable.clearHistoryWord(mSphinx, HistoryWordTable.TYPE_TRAFFIC);
                    mSuggestWordListManager.refresh(mCityInfo.getId());
                } else {
                    if (tkWord.attribute == TKWord.ATTRIBUTE_HISTORY) {
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemHistory, position, tkWord.word);
                    } else {
                        tkWord.position = mSphinx.getMapEngine().getwordslistStringWithPosition(tkWord.word, 0);
                        mActionLog.addAction(mActionTag + ActionLog.ListViewItemSuggest, position, tkWord.word);
                    }
                    mTKWord = tkWord.clone();
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
        
        mAlternativeLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position >= 0 && position < mAlternativeList.size()) {
                    mInvoker.setSelectedLocation(true);
                    Filter filter = HotelHomeFragment.getFilter(mInvoker.getFilterList(), FilterArea.FIELD_LIST);
                    if (filter != null) {
                        FilterListView.selectedFilter(filter, -1);
                    }
                    POI poi = mAlternativeList.get(position);
                    mInvoker.setPOI(poi);
                    mActionLog.addAction(mActionTag+ActionLog.HotelPickLocationAlternativeSelect, position, poi.getName());
                    HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, poi.getName(), poi.getPosition()), HistoryWordTable.TYPE_TRAFFIC);
                    dismiss();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
                
            case R.id.right_btn:
                mActionLog.addAction(mActionTag + ActionLog.HotelPickLocationSubmit);
                if (mKeywordEdt.getText().toString().trim().length() > 0) {
                    submit();
                } else {
                    dismiss();
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
        TKWord tkWord = mTKWord;
        String word = mKeywordEdt.getText().toString().trim();
        if (tkWord != null) {
            tkWord.word.trim();
        }
        if (TextUtils.isEmpty(word)) {
            mSphinx.showTip(R.string.search_input_keyword, Toast.LENGTH_SHORT);
            return;
        } else {
            mSphinx.hideSoftInput(mKeywordEdt.getInput());
            HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, word, null), HistoryWordTable.TYPE_TRAFFIC);
            DataQuery poiQuery = new DataQuery(mContext);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_ALTERNATIVE);
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, word);
            poiQuery.setup(getId(), getId(), getString(R.string.doing_and_wait), false, false, null);
            poiQuery.setCityId(mCityInfo.getId());
            
            mSphinx.queryStart(poiQuery);
        }
    }
    
    /**
     * 将UI及内容重置为第一次进入页面时的状态
     */
    public void reset() {
        mSuggestLsv.setSelectionFromTop(0, 0);
        mKeywordEdt.setText(null);
        mKeywordEdt.clearFocus();
        mViewPager.setCurrentItem(0);
        mViewPager.requestFocus();
        mSuggestLsv.setVisibility(View.VISIBLE);
        mAlternativeLsv.setVisibility(View.GONE);
        mFilterListView.setData(mInvoker.getFilterList(), FilterResponse.FIELD_FILTER_AREA_INDEX, this, false, false, mActionTag);
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
    
    public void setTitle(String title) {
        mTitle = title;
    }

    @Override
    public void doFilter(String name) {
        mInvoker.setSelectedLocation(true);
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
        boolean result = false;
        if (response != null && response.getResponseCode() == Response.RESPONSE_CODE_OK && response instanceof AlternativeResponse) {
            mAlternativeResponse = (AlternativeResponse) response;
            AlternativeList alternativeList = mAlternativeResponse.getList();
            List<POI> list = null;
            if (alternativeList != null) {
                list = alternativeList.getList();
            }
            if (list == null || list.isEmpty()) {
                long id = mAlternativeResponse.getPosition();
                
                List<Filter> filterListInHotelHome = mInvoker.getFilterList();
                Filter selected = null;
                if (filterListInHotelHome != null) {
                    for(int i = 0, size = filterListInHotelHome.size(); i < size; i++) {
                        Filter filter = filterListInHotelHome.get(i);
                        if (filter.getKey() == FilterResponse.FIELD_FILTER_AREA_INDEX) {
                            List<Filter> list1 = filter.getChidrenFilterList();
                            for(int j = 0, count = list1.size(); j < count; j++) {
                                Filter filter1 = list1.get(j);
                                if (filter1.getFilterOption().getId() == id) {
                                    FilterListView.selectedFilter(filter, filter1);
                                    selected = filter1;
                                    break;
                                }
                                List<Filter> list2 = filter1.getChidrenFilterList();
                                for(int m = 0, total = list2.size(); m < total; m++) {
                                    Filter filter2 = list2.get(m);
                                    if (filter2.getFilterOption().getId() == id) {
                                        FilterListView.selectedFilter(filter, filter2);
                                        selected = filter2;
                                        break;
                                    }
                                }
                                
                                if (selected != null) {
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                
                if (selected != null) {
                    result = true;
                    HistoryWordTable.addHistoryWord(mSphinx, new TKWord(TKWord.ATTRIBUTE_HISTORY, selected.getFilterOption().getName(), null), HistoryWordTable.TYPE_TRAFFIC);
                    mInvoker.setSelectedLocation(true);
                    dismiss();
                }
            }
            
            if (list != null && list.size() > 0) {
                mAlternativeList.clear();
                mAlternativeList.addAll(list);
                mSuggestLsv.setVisibility(View.GONE);
                mAlternativeLsv.setVisibility(View.VISIBLE);
                mAlternativeAdapter.notifyDataSetChanged();
                mAlternativeLsv.setSelectionFromTop(0, 0);
                mViewPager.setCurrentItem(1);
                result = true;
            }
        }
        
        if (result == false) {
            if (response != null) {
                Toast.makeText(mSphinx, R.string.can_not_found_target_location, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mSphinx, R.string.network_failed, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void dismiss() {
        super.dismiss();
        mInvoker.refreshFilterAreaView();
        mKeywordEdt.setText(null);
    }
    
    class AlternativeAdapter extends ArrayAdapter<POI> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.poi_alternative_list_item;
        
        private LayoutInflater mLayoutInflater;

        public AlternativeAdapter(Context context, List<POI> list) {
            super(context, TEXTVIEW_RESOURCE_ID, list);
            mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            POI alternative = getItem(position);
            
            TextView nameTxv = (TextView)view.findViewById(R.id.name_txv);
            nameTxv.setText(alternative.getName());
            TextView addressTxv = (TextView)view.findViewById(R.id.address_txv);
            addressTxv.setText(alternative.getAddress());

            return view;
        }
    }
}

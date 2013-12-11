/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView.OnEditorActionListener;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.widget.TKEditText;
import com.tigerknows.common.ActionLog;
import com.tigerknows.map.CityInfo;
import com.tigerknows.map.MapEngine;
import com.tigerknows.ui.BaseActivity;

/**
 * @author Peng Wenyue
 */
public class ChangeCityActivity extends BaseActivity {

    
    public static final String EXTRA_CITYINFO = "cityinfo";
    public static final String EXTRA_ONLY_CHANGE_HOTEL_CITY = "only_change_hotel_city";

    static final int ORDER_LOCATION_TITLE = 1;
    static final int ORDER_CITY_TITLE = 10;
    static final int ORDER_PROVINCE_TITLE =  100;
    
    private TKEditText mKeywordEdt;
    private ExpandableListView mCityElv;
    private ListView mSuggestCityLsv;
    private CityExpandableListAdapter mCityExpandableListAdapter;
    private SuggestCityAdapter mSuggestCityAdapter;    
    private List<CityInfo> mSuggestCityList = new ArrayList<CityInfo>();
    private CityInfo mLocationCity;
    private static CityInfo sLocationTitle = null;
    private static CityInfo sCityTitle = null;
    private static CityInfo sProvinceTitle = null;
    private static List<CityInfo> sAllCityInfoList = null;
    private String mNotFindCity;
    
    private Comparator<CityInfo> mCityComparator = new Comparator<CityInfo>() {

        @Override
        public int compare(CityInfo cityInfo1, CityInfo cityInfo2) {
            return cityInfo1.order - cityInfo2.order;
        };
    };
    
    private boolean mChangeHotelCity = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.ChangeCity;
        mNotFindCity = mThis.getString(R.string.not_find_city);

        setContentView(R.layout.more_change_city);
        findViews();
        setListener();
        
        mTitleBtn.setText(R.string.change_city);
        mRightBtn.setVisibility(View.GONE);

        mCityElv.setGroupIndicator(null);
        
        mSuggestCityAdapter = new SuggestCityAdapter(mThis, mSuggestCityList);
        mSuggestCityLsv.setAdapter(mSuggestCityAdapter);

        if (sAllCityInfoList ==  null) {
           sAllCityInfoList = MapEngine.getAllProvinceCityList(mThis);
           for(int i = sAllCityInfoList.size()-1; i >= 0; i--) {
               CityInfo cityInfo = sAllCityInfoList.get(i);
               List<CityInfo> cityInfoList = cityInfo.getCityList();
               if (cityInfoList.size() <= 1) {
                   cityInfo.order = cityInfo.getId()+ORDER_CITY_TITLE;
               } else {
                   cityInfo.order = cityInfo.getId()+ORDER_PROVINCE_TITLE;
               }
               for(int j = cityInfoList.size()-1; j >= 0; j--) {
                   cityInfoList.get(j).order = cityInfoList.get(j).getId();
               }
               Collections.sort(cityInfoList, mCityComparator);
           }
           sLocationTitle = new CityInfo();
           sLocationTitle.setCName(mThis.getString(R.string.location_city));
           sLocationTitle.order = ORDER_LOCATION_TITLE;
           sCityTitle = new CityInfo();
           sCityTitle.setCName(mThis.getString(R.string.municipality));
           sCityTitle.order = ORDER_CITY_TITLE;
           sAllCityInfoList.add(0, sCityTitle);
           sProvinceTitle = new CityInfo();
           sProvinceTitle.setCName(mThis.getString(R.string.search_by_province));
           sProvinceTitle.order = ORDER_PROVINCE_TITLE;
           sAllCityInfoList.add(0, sProvinceTitle);
           Collections.sort(sAllCityInfoList, mCityComparator);
        }
        
        mSuggestCityLsv.setVisibility(View.GONE);
        mCityElv.setVisibility(View.VISIBLE);

        
        mCityExpandableListAdapter = new CityExpandableListAdapter(mThis);
        mCityElv.setAdapter(mCityExpandableListAdapter);
        
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ONLY_CHANGE_HOTEL_CITY)) {
            mChangeHotelCity = intent.getBooleanExtra(EXTRA_ONLY_CHANGE_HOTEL_CITY, false);
            mTitleBtn.setText(R.string.select_city);
        }
    }

    protected void findViews() {
        super.findViews();
        mKeywordEdt = (TKEditText) findViewById(R.id.input_edt);
        mKeywordEdt.mActionTag = mActionTag;
        mCityElv = (ExpandableListView)findViewById(R.id.city_elv);
        mSuggestCityLsv = (ListView)findViewById(R.id.suggest_city_lsv);
    }

    protected void setListener() {
        super.setListener();
        mKeywordEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mActionLog.addAction(mActionTag +  ActionLog.ChangeCityInput);
                }
                return false;
            }
        });
        mKeywordEdt.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int arg1, int arg2, int arg3) {
                String userInput = s.toString().trim();
                showSuggestCity(userInput);
            }
            
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                
            }
        });
        mKeywordEdt.setOnEditorActionListener(new OnEditorActionListener() {
            
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    hideSoftInput();
                    return true;
                }
                return false;
            }
        });

        mCityElv.setOnGroupCollapseListener(new OnGroupCollapseListener() {
            
            @Override
            public void onGroupCollapse(int groupPosition) {
                mActionLog.addAction(mActionTag + ActionLog.ChangeCityProvince, groupPosition, sAllCityInfoList.get(groupPosition).getCName(), "0");
            }
        });
        
        mCityElv.setOnGroupExpandListener(new OnGroupExpandListener() {
            
            @Override
            public void onGroupExpand(int groupPosition) {
                if (sAllCityInfoList.get(groupPosition).getCityList().size() == 1) {
                    mActionLog.addAction(mActionTag + ActionLog.ChangeCityCity, groupPosition, 0, sAllCityInfoList.get(groupPosition).getCName());
                    changeCity(sAllCityInfoList.get(groupPosition).getCityList().get(0));
                } else {
                    mActionLog.addAction(mActionTag + ActionLog.ChangeCityProvince, groupPosition, sAllCityInfoList.get(groupPosition).getCName(), "1");
                }
            }
        });
        
        mCityElv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
                changeCity(sAllCityInfoList.get(groupPosition).getCityList().get(childPosition));
                mActionLog.addAction(mActionTag + ActionLog.ChangeCityCity, groupPosition, childPosition, sAllCityInfoList.get(groupPosition).getCityList().get(childPosition).getCName());
                return false;
            }
        });
        
        mSuggestCityLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                CityInfo cityInfo = (CityInfo)(arg1.getTag());
                if (!mNotFindCity.equals(cityInfo.getCName())) {
                    mActionLog.addAction(mActionTag + ActionLog.ChangeCitySuggest, arg2, cityInfo.getCName());
                    changeCity(cityInfo);
                }
                return;
            }
        });
        mSuggestCityLsv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideSoftInput(false);
                }
                return false;
            }
        });
        mCityElv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideSoftInput();
                }
                return false;
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mLocationCity = Globals.g_My_Location_City_Info;
        if (mLocationCity != null) {
            if (mLocationCity.getCityList().size() == 0) {
                mLocationCity.getCityList().add(mLocationCity.clone());
            }
            mLocationCity.order = ORDER_LOCATION_TITLE+1;
            if (checkLocationTitle()) {
                sAllCityInfoList.remove(1);
                sAllCityInfoList.add(1, mLocationCity);
            } else {
                sAllCityInfoList.add(0, sLocationTitle);
                sAllCityInfoList.add(1, mLocationCity);
            }
        } else {
            if (checkLocationTitle()) {
                sAllCityInfoList.remove(0);
                sAllCityInfoList.remove(0);
            }
        }
        mCityExpandableListAdapter.notifyDataSetChanged();
        mCityElv.requestFocus();
    }
    
    private boolean checkLocationTitle() {
        CityInfo cityInfo = sAllCityInfoList.get(0);
        if (cityInfo.order == sLocationTitle.order) {
            return true;
        }
        return false;
    }
    
    private void changeCity(CityInfo cityInfo) {
        if (cityInfo.isAvailably() == false || cityInfo.getId() == -5 || cityInfo.getId() == -6) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CITYINFO, cityInfo);
        intent.putExtra(EXTRA_ONLY_CHANGE_HOTEL_CITY, mChangeHotelCity);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    void showSuggestCity(String userInput) {
        makeSuggestCityList(sAllCityInfoList, mSuggestCityList, userInput, mNotFindCity, mCityElv, mSuggestCityLsv);
        mSuggestCityAdapter.notifyDataSetChanged();
        mSuggestCityLsv.setSelectionFromTop(0, 0);
    }
    
    public static void makeSuggestCityList(List<CityInfo> allCity, List<CityInfo> suggestList, String userInput
            , String notFindCity, ListView allCityLsv, ListView suggestLsv) {
        ArrayList<CityInfo> suggestCityList = new ArrayList<CityInfo>();
        if (!userInput.equals("")) {
            for (CityInfo province : allCity) {
                List<CityInfo> cityInfoList = province.getCityList();
                for(CityInfo cityInfo : cityInfoList) {
                    if (properCity(cityInfo, userInput)
                            && cityInfo.getType() == CityInfo.TYPE_CITY
                            && suggestCityList.contains(cityInfo) == false) {
                        suggestCityList.add(cityInfo);
                    }
                }
            }
        }
        suggestList.clear();
        suggestList.addAll(suggestCityList);
        if (suggestList.size() == 0 && !TextUtils.isEmpty(userInput)) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setCName(notFindCity);
            suggestList.add(cityInfo);
        }
        
        if (suggestList.size() > 0) {
            suggestLsv.setVisibility(View.VISIBLE);
            allCityLsv.setVisibility(View.GONE);
        } else {
            suggestLsv.setVisibility(View.GONE);
            allCityLsv.setVisibility(View.VISIBLE);
        }
    }
    
    public static boolean properCity(CityInfo cityInfo, String userInput) {
        if (userInput == null || userInput.length()<1) {
            return true;
        }
        if (cityInfo == null
                || cityInfo.isAvailably() == false) {
            return false;
        }
        String lowerEName = cityInfo.getEName();
        if (lowerEName == null) {
            return false;
        }
        lowerEName = lowerEName.toLowerCase();
        String lowerInput = userInput.toLowerCase();
        String cName = cityInfo.getCName();
        if (cName.contains(userInput)) {
            return true;
        }
        String firstInputStr = lowerInput.substring(0, 1);
        String otherUserInput = lowerInput.substring(1, lowerInput.length());
        String otherENameStr = lowerEName.substring(1, lowerEName.length());
        if (lowerEName.startsWith(firstInputStr) && otherENameStr.contains(otherUserInput)) {
            return true;
        }
        return false;
    }
    
    static final int RES_ID = R.layout.more_change_city_list_item;
    
    private class SuggestCityAdapter extends ArrayAdapter<CityInfo> {
        
        public SuggestCityAdapter(Context context, List<CityInfo> cityInfoList) {
            super(context, RES_ID, cityInfoList);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RES_ID, parent, false);
            } else {
                view = convertView;
            }

            CityInfo cityInfo = getItem(position);
            view.setTag(cityInfo);

            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            textView.setText(cname);
            ImageView imageView = (ImageView)view.findViewById(R.id.icon_imv);
            imageView.setBackgroundDrawable(null);
            if (mNotFindCity.equals(cname)) {
                textView.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                view.setBackgroundResource(R.color.gray_light);
            } else {
                textView.setTextColor(TKConfig.COLOR_BLACK_DARK);
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
            }
            
            return view;
        }
    }
    
    public class CityExpandableListAdapter extends BaseExpandableListAdapter {

        public CityExpandableListAdapter(Context context) {
        }

        public Object getChild(int groupPosition, int childPosition) {
            return sAllCityInfoList.get(groupPosition).getCityList().get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        public int getChildrenCount(int groupPosition) {
            int count = sAllCityInfoList.get(groupPosition).getCityList().size();
            return count > 1 ? count : 0;
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RES_ID, parent, false);
            } else {
                view = convertView;
            }
            view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);

            CityInfo cityInfo = (CityInfo) getChild(groupPosition, childPosition);
            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            textView.setText("    " + cname);
            ImageView imageView = (ImageView)view.findViewById(R.id.icon_imv);
            imageView.setBackgroundDrawable(null);
            return view;
        }

        public Object getGroup(int groupPosition) {
            return sAllCityInfoList.get(groupPosition);
        }

        public int getGroupCount() {
            return sAllCityInfoList.size();
        }

        public long getGroupId(int groupPosition) {
            return 0;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(RES_ID, parent, false);
            } else {
                view = convertView;
            }
            
            CityInfo cityInfo = (CityInfo) getGroup(groupPosition);
            String cname = cityInfo.getCName();
            TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
            TextView titleTxv = (TextView)view.findViewById(R.id.title_txv);
            
            ImageView imageView = (ImageView)view.findViewById(R.id.icon_imv);
            int order = cityInfo.order;
            if (order == ORDER_LOCATION_TITLE || order == ORDER_CITY_TITLE || order == ORDER_PROVINCE_TITLE) {
                view.setBackgroundResource(R.drawable.bg_expandablelistview_group);
                titleTxv.setText(cname);
                textTxv.setVisibility(View.GONE);
                titleTxv.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.INVISIBLE);
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                textTxv.setText(cname);
                textTxv.setVisibility(View.VISIBLE);
                titleTxv.setVisibility(View.GONE);
                if (getChildrenCount(groupPosition) > 1) {
                    imageView.setVisibility(View.VISIBLE);
                    if (isExpanded) {            
                        imageView.setBackgroundResource(R.drawable.icon_arrow_up);
                    } else {
                        imageView.setBackgroundResource(R.drawable.icon_arrow_down);
                    }
                } else {
                    imageView.setVisibility(View.INVISIBLE);
                }
            }
            
            return view;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }
    }
}

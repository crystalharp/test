/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

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
import android.widget.EditText;
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
import com.tigerknows.maps.MapEngine;
import com.tigerknows.maps.MapEngine.CityInfo;

/**
 * @author Peng Wenyue
 */
public class ChangeCity extends BaseActivity implements View.OnClickListener {

    private TextView mCurrentCityTxv;
    private EditText mKeywordEdt;
    private ExpandableListView mCityElv;
    private ListView mSuggestCityLsv;
    private CityExpandableListAdapter mCityExpandableListAdapter;
    private SuggestCityAdapter mSuggestCityAdapter;    
    private List<CityInfo> mSuggestCityList = new ArrayList<CityInfo>();
    private static List<CityInfo> sAllCityInfoList = null;
    
    private String mCurrentCityCName;
    
    private MapEngine mMapEngine;
    
    private Comparator<CityInfo> mCityComparator = new Comparator<CityInfo>() {

        @Override
        public int compare(CityInfo cityInfo1, CityInfo cityInfo2) {
            return cityInfo1.getId() - cityInfo2.getId();
        };
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.ChangeCity;

        setContentView(R.layout.change_city);
        findViews();
        setListener();
        
        mTitleTxv.setText(R.string.change_city);
        mRightTxv.setVisibility(View.GONE);

        mCityElv.setGroupIndicator(null);
        
        mSuggestCityAdapter = new SuggestCityAdapter(mThis, mSuggestCityList);
        mSuggestCityLsv.setAdapter(mSuggestCityAdapter);

        mMapEngine = MapEngine.getInstance();
        if (sAllCityInfoList ==  null) {
           sAllCityInfoList = mMapEngine.getAllProvinceCityList(mThis);
           Collections.sort(sAllCityInfoList, mCityComparator);
           for(CityInfo cityInfo : sAllCityInfoList) {
               List<CityInfo> cityInfoList = cityInfo.getCityList();
               Collections.sort(cityInfoList, mCityComparator);
           }
        }
        
        mSuggestCityLsv.setVisibility(View.GONE);
        mCityElv.setVisibility(View.VISIBLE);
        
        CityInfo cityInfo = Globals.g_Current_City_Info;
        mCurrentCityCName = (cityInfo != null ? cityInfo.getCName() : "");
        mCurrentCityTxv.setText(mThis.getString(R.string.current_city_, mCurrentCityCName));
        mCityExpandableListAdapter = new CityExpandableListAdapter(mThis);
        mCityElv.setAdapter(mCityExpandableListAdapter);
    }

    protected void findViews() {
        super.findViews();
        mKeywordEdt = (EditText) findViewById(R.id.input_edt);
        mCurrentCityTxv = (TextView) findViewById(R.id.current_city_txv);
        mCityElv = (ExpandableListView)findViewById(R.id.city_elv);
        mSuggestCityLsv = (ListView)findViewById(R.id.suggest_city_lsv);
    }

    protected void setListener() {
        super.setListener();
        mKeywordEdt.setOnClickListener(this);
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
                if (sAllCityInfoList.get(groupPosition).getCityList().size() == 1) {
                    changeCity(sAllCityInfoList.get(groupPosition));
                }
            }
        });
        
        mCityElv.setOnGroupExpandListener(new OnGroupExpandListener() {
            
            @Override
            public void onGroupExpand(int groupPosition) {
                if (sAllCityInfoList.get(groupPosition).getCityList().size() == 1) {
                    changeCity(sAllCityInfoList.get(groupPosition));
                }                
            }
        });
        
        mCityElv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
                changeCity(sAllCityInfoList.get(groupPosition).getCityList().get(childPosition));
                return false;
            }
        });
        
        mSuggestCityLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                CityInfo cityInfo = (CityInfo)(arg1.getTag());
                if (!cityInfo.getCName().equals(mThis.getString(R.string.not_find_city))) {
                    changeCity(cityInfo);
                }
                return;
            }
        });
        mSuggestCityLsv.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideSoftInput();
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
    
    private void changeCity(CityInfo cityInfo) {
        if (cityInfo.getId() == Globals.g_Current_City_Info.getId()) {
            mActionLog.addAction(ActionLog.ChangeCityCurrent, cityInfo.getCName());
        } else {
            mActionLog.addAction(ActionLog.ChangeCitySelect, cityInfo.getCName());
        }
        Intent intent = new Intent();
        intent.putExtra("cityId", cityInfo.getId());
        setResult(RESULT_OK, intent);
        finish();
    }
    
    private void showSuggestCity(String userInput) {
        ArrayList<CityInfo> suggestCityList = new ArrayList<CityInfo>();
        if (!userInput.equals("")) {
            for (CityInfo province : sAllCityInfoList) {
                List<CityInfo> cityInfoList = province.getCityList();
                for(CityInfo cityInfo : cityInfoList) {
                    if (properCity(cityInfo, userInput) && cityInfo.getType() == CityInfo.TYPE_CITY) {
                        suggestCityList.add(cityInfo);
                    }
                }
            }
        }
        mSuggestCityList.clear();
        mSuggestCityList.addAll(suggestCityList);
        if (mSuggestCityList.size() == 0 && !TextUtils.isEmpty(userInput)) {
            CityInfo cityInfo = new CityInfo();
            cityInfo.setCName(mThis.getString(R.string.not_find_city));
            mSuggestCityList.add(cityInfo);
        }
        mSuggestCityAdapter.notifyDataSetChanged();

        
        if (mSuggestCityList.size() > 0) {
            mSuggestCityLsv.setVisibility(View.VISIBLE);
            mCityElv.setVisibility(View.GONE);
        } else {
            mSuggestCityLsv.setVisibility(View.GONE);
            mCityElv.setVisibility(View.VISIBLE);
        }
    }
    
    private boolean properCity(CityInfo cityInfo, String userInput) {
        if (userInput == null || userInput.length()<1) {
            return true;
        }
        String lowerEName = cityInfo.getEName().toLowerCase();
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
    
    private class SuggestCityAdapter extends ArrayAdapter<CityInfo> {
        
        public SuggestCityAdapter(Context context, List<CityInfo> cityInfoList) {
            super(context, R.layout.change_city_list_item, cityInfoList);
        }
        
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.change_city_list_item, parent, false);
            } else {
                view = convertView;
            }

            CityInfo cityInfo = getItem(position);
            view.setTag(cityInfo);

            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            textView.setText(cname);
            ImageView imageView = (ImageView)view.findViewById(R.id.icon_imv);
            if (cname.equals(mCurrentCityCName)) {
                imageView.setBackgroundResource(R.drawable.icon_right);
            } else {
                imageView.setBackgroundDrawable(null);
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
                view = mLayoutInflater.inflate(R.layout.change_city_list_item, parent, false);
            } else {
                view = convertView;
            }
            view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);

            CityInfo cityInfo = (CityInfo) getChild(groupPosition, childPosition);
            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            textView.setText("    " + cname);
            ImageView imageView = (ImageView)view.findViewById(R.id.icon_imv);
            if (cname.equals(mCurrentCityCName)) {
                imageView.setBackgroundResource(R.drawable.icon_right);
            } else {
                imageView.setBackgroundDrawable(null);
            }            
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
                view = mLayoutInflater.inflate(R.layout.change_city_list_item, parent, false);
            } else {
                view = convertView;
            }
            
            CityInfo cityInfo = (CityInfo) getGroup(groupPosition);
            String cname = cityInfo.getCName();
            TextView textView = (TextView)view.findViewById(R.id.text_txv);
            textView.setText(cname);
            
            ImageView imageView = (ImageView)view.findViewById(R.id.icon_imv);
            if (getChildrenCount(groupPosition) > 1) {
                if (isExpanded) {            
                    imageView.setBackgroundResource(R.drawable.icon_arrow_up);
                } else {
                    imageView.setBackgroundResource(R.drawable.icon_arrow_down);
                }
            } else {
                if (cname.equals(mCurrentCityCName)) {
                    imageView.setBackgroundResource(R.drawable.icon_right);
                } else {
                    imageView.setBackgroundDrawable(null);
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

    @Override
    public void onClick(View view) {
        mActionLog.addAction(ActionLog.ChangeCityInputBox);
    }
}

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tigerknows.widget;

import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Custom time listview. */
public class TimeListView extends LinearLayout {
    
    static final int WHITE_LINE = 2;
    
    private ListView hourLsv;
    private ListView minuteLsv;
    
    private List<String> hourList = new ArrayList<String>();
    private List<String> minuteList = new ArrayList<String>();
    
    private MyAdapter hourAdapter;
    private MyAdapter minuteAdapter;
    
    Context context;
    
    SimpleDateFormat monthDayFormat;
    
    SimpleDateFormat dayFormat;
    
    int hourPosition = 0;
    
    int minutePosition = 0;
    
    ActionLog actionLog;
        
    public TimeListView(Context context) {
        this(context, null);
    }

    public TimeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.time_list, this, // we are the parent
                true);
        
        actionLog = ActionLog.getInstance(context);
        this.context = context;
        
        hourList = new ArrayList<String>();
        makeWhiteLines(hourList);
        for(int i = 0, size = 24; i < size; i++) {
            hourList.add(toTwoChar(i));
        }
        makeWhiteLines(hourList);
        
        minuteList = new ArrayList<String>();
        makeWhiteLines(minuteList);
        for(int i = 0, size = 6; i < size; i++) {
            minuteList.add(toTwoChar(i*10));
        }
        makeWhiteLines(minuteList);
        
        findViews();
        setListener();
        
        hourAdapter = new MyAdapter(context, hourList);
        hourAdapter.isParent = true;
        minuteAdapter = new MyAdapter(context, minuteList);
        minuteAdapter.isParent = false;
    }
    
    void makeWhiteLines(List<String> list) {
        if (list == null) {
            return;
        }
        
        for(int i = 0; i < WHITE_LINE; i++) {
            list.add("");
        }
    }

    protected void findViews() {
        hourLsv = (ListView) findViewById(R.id.parent_lsv);
        minuteLsv = (ListView) findViewById(R.id.child_lsv);
    }
    
    protected void setListener() {
        
        hourLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 < 2) {
                    arg2 = 2;
                } else if (arg2 > hourList.size()-3) {
                    arg2 = hourList.size()-3;
                }
                hourPosition = arg2;
                hourAdapter.notifyDataSetChanged();
                hourLsv.setSelectionFromTop(arg2-2, 0);
                
            }
        });
        
        minuteLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 < 2) {
                    arg2 = 2;
                } else if (arg2 > minuteList.size()-3) {
                    arg2 = minuteList.size()-3;
                }
                minutePosition = arg2;
                minuteAdapter.notifyDataSetChanged();
                minuteLsv.setSelectionFromTop(arg2-2, 0);
            }
        });
        
        hourLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
                    int firstPosition = hourLsv.getFirstVisiblePosition();
                    hourPosition = firstPosition+2;
                    hourAdapter.notifyDataSetChanged();
                    hourLsv.setSelectionFromTop(firstPosition, 0);
                }
            }
            
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // TODO Auto-generated method stub
                
            }
        });
        
        minuteLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
                    int firstPosition = minuteLsv.getFirstVisiblePosition();
                    minutePosition = firstPosition+2;
                    minuteAdapter.notifyDataSetChanged();
                    minuteLsv.setSelectionFromTop(firstPosition, 0);
                }
            }
            
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        /* Consume all touch events so they don't get dispatched to the view
         * beneath this view.
         */
        return true;
    }
    
    @Override
    public boolean hasFocus() {
        if (hourLsv == null || minuteLsv == null) {
            return false;
        }
        return hourLsv.hasFocus() || minuteLsv.hasFocus();
    }
    
    class MyAdapter extends ArrayAdapter<String> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.time_list_item;
        
        private LayoutInflater mLayoutInflater;
        
        boolean isParent = false;

        public MyAdapter(Context context, List<String> list) {
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
            
            ImageView iconImv = (ImageView)view.findViewById(R.id.icon_imv);
            iconImv.setVisibility(View.GONE);
            
            TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
            
            String name = getItem(position);
            
            view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
            
            if ((isParent && hourPosition == position) || (isParent == false && minutePosition == position)) {
                textTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
            } else {
                textTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
            }
            
            textTxv.setText(name);
            
            return view;
        }
    }
    
    public String getHour() {
        return hourList.get(hourPosition);
    }
    
    public String getMinute() {
        return minuteList.get(minutePosition);
    }
    
    public int getHourPosition() {
        return hourPosition;
    }
    
    public int getMinutePosition() {
        return minutePosition;
    }
    
    public void setData(int hourPosition, int minutePosition) {

        hourLsv.setAdapter(hourAdapter);
        minuteLsv.setAdapter(minuteAdapter);
        
        if (hourPosition >= 0 && hourPosition < hourList.size()) {
            this.hourPosition = hourPosition;
            hourAdapter.notifyDataSetChanged();
            
            hourLsv.setSelectionFromTop(this.hourPosition-2, 0);
        }
        
        if (minutePosition >= 0 && minutePosition < minuteList.size()) {
            this.minutePosition = minutePosition;
            minuteAdapter.notifyDataSetChanged();

            minuteLsv.setSelectionFromTop(this.minutePosition-2, 0);
        }
    }
    
    public void reset() {
        hourLsv.setAdapter(null);
        minuteLsv.setAdapter(null);
    }
    
    String toTwoChar(int value) {
        String result;
        if (value < 10) {
            result = "0" + value;
        } else {
            result = String.valueOf(value);
        }
        
        return result;
    }
}

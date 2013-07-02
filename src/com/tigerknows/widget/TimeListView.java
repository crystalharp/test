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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Custom time listview. */
public class TimeListView extends LinearLayout {
    
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
        for(int i = 1, size = 25; i < size; i++) {
            hourList.add(String.valueOf(i));
        }
        
        minuteList = new ArrayList<String>();
        for(int i = 0, size = 12; i < size; i++) {
            minuteList.add(String.valueOf(i*5));
        }
        
        findViews();
        setListener();
        
        hourAdapter = new MyAdapter(context, hourList);
        hourAdapter.isParent = true;
        minuteAdapter = new MyAdapter(context, minuteList);
        minuteAdapter.isParent = false;
        
        hourLsv.setAdapter(hourAdapter);
        minuteLsv.setAdapter(minuteAdapter);
    }

    protected void findViews() {
        hourLsv = (ListView) findViewById(R.id.parent_lsv);
        minuteLsv = (ListView) findViewById(R.id.child_lsv);
    }
    
    protected void setListener() {
        hourLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                hourPosition = position;
                minutePosition = 0;
                hourAdapter.notifyDataSetChanged();
            }
        });
        minuteLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                minutePosition = position;
                minuteAdapter.notifyDataSetChanged();
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
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.filter_list_item;
        
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
            
            if (isParent) {
                if (position == hourPosition) {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                    textTxv.setTextColor(TKConfig.COLOR_ORANGE);
                } else {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                    textTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
                }
            } else {
                if (position == minutePosition) {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                    textTxv.setTextColor(TKConfig.COLOR_ORANGE);
                } else {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                    textTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                }
            }
            
            textTxv.setText(name);
            
            return view;
        }
    }
    
    public String getHour() {
        return null;
    }
    
    public String getMinute() {
        return null;
    }
    
    public void setData(int hourPosition, int minutePosition) {
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
}

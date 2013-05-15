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

package com.tigerknows.ui.hotel;

import com.tigerknows.R;
import com.tigerknows.TKConfig;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Custom Filter listview. */
public class DateListView extends LinearLayout implements View.OnClickListener {
    
    static final int CHECKIN_MAX = 30;
    
    static final int CHECKOUT_MAX = 5;
    
    public interface CallBack {
        public void confirm();
        public void cancel();
    }
    
    private Button confirmBtn;
    
    private TextView titleTxv;
    
    private ListView checkinLsv;
    private ListView checkoutLsv;
    
    private CallBack callBack;
    
    private List<String> checkinList = new ArrayList<String>();
    private List<String> checkoutList = new ArrayList<String>();
    
    private MyAdapter checkinAdapter;
    private MyAdapter checkoutAdapter;
    
    String actionTag;
    
    Calendar today = null;
    
    int confirmCheckinPosition;
    
    int confirmCheckoutPosition;
    
    int checkinPosition;
    
    int checkoutPosition;
    
    String[] days;
    
    String[] weedDays;
    
    public void onResume() {
        Calendar now = Calendar.getInstance();
        checkinPosition = confirmCheckinPosition;
        checkoutPosition = confirmCheckoutPosition;
        if (today == null || now.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR)) {
            checkinPosition = 0;
            checkoutPosition = 0;
            confirmCheckinPosition = 0;
            confirmCheckoutPosition = 0;
            today = now;
            checkinList.clear();
            checkoutList.clear();
            for(int i = 0; i < CHECKIN_MAX; i++) {
                checkinList.add(makeCheckinDateString(today, i));
            }
            
            checkinAdapter.notifyDataSetChanged();
            refreshCheckout();
        }
    }
    
    void refreshCheckout() {
        checkoutList.clear();
        today.add(Calendar.DAY_OF_MONTH, checkinPosition);
        titleTxv.setText((today.get(Calendar.MONTH)+1)+"月"+(today.get(Calendar.DAY_OF_MONTH))+"日入住1晚");
        for(int i = 1, count = CHECKOUT_MAX+1; i < count; i++) {
            checkoutList.add(makeCheckoutDateString(today, i));
        }
        today.add(Calendar.DAY_OF_MONTH, -checkinPosition);
        checkoutAdapter.notifyDataSetChanged();
    }

    public void setData(CallBack callBack, String actionTag) {
        this.actionTag = actionTag;
        this.callBack = callBack;
        if (checkinPosition > -1) {
            checkinLsv.setSelectionFromTop(checkinPosition, 0);
        } else {
            checkinLsv.setSelectionFromTop(0, 0);
        }
        
        if (checkoutPosition > -1) {
            checkoutLsv.setSelectionFromTop(checkoutPosition, 0);
        } else {
            checkoutLsv.setSelectionFromTop(0, 0);
        }
    }
        
    public DateListView(Context context) {
        this(context, null);
    }

    public DateListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hotel_date_list, this, // we are the parent
                true);
        
        Resources resources = context.getResources();
        days = resources.getStringArray(R.array.days);
        weedDays = resources.getStringArray(R.array.week_days);
        
        findViews();
        setListener();
        
        checkinAdapter = new MyAdapter(context, checkinList);
        checkinAdapter.isParent = true;
        checkoutAdapter = new MyAdapter(context, checkoutList);
        checkoutAdapter.isParent = false;
        
        checkinLsv.setAdapter(checkinAdapter);
        checkoutLsv.setAdapter(checkoutAdapter);
        onResume();
    }

    protected void findViews() {
        confirmBtn = (Button) findViewById(R.id.confirm_btn);
        titleTxv = (TextView) findViewById(R.id.title_txv);
        checkinLsv = (ListView) findViewById(R.id.parent_lsv);
        checkoutLsv = (ListView) findViewById(R.id.child_lsv);
    }
    
    protected void setListener() {
        findViewById(R.id.parent_view).setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        findViewById(R.id.child_view).setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancel();
                return false;
            }
        });
        checkinLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                checkinPosition = position;
                checkinAdapter.notifyDataSetChanged();

                refreshCheckout();
            }
        });
        checkoutLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                checkoutPosition = position;
                checkoutAdapter.notifyDataSetChanged();
                today.add(Calendar.DAY_OF_MONTH, checkinPosition);
                titleTxv.setText((today.get(Calendar.MONTH)+1)+"月"+(today.get(Calendar.DAY_OF_MONTH))+"日入住"+(checkoutPosition+1)+"晚");
                today.add(Calendar.DAY_OF_MONTH, -checkinPosition);
            }
        });
        confirmBtn.setOnClickListener(this);
    }
    
    private void confirm() {
        confirmCheckinPosition = checkinPosition;
        confirmCheckoutPosition = checkoutPosition;
        if (callBack != null) {
            callBack.confirm();
        }
    }
    
    private void cancel() {
        if (callBack != null) {
            callBack.cancel();
        }
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
        if (checkinLsv == null || checkoutLsv == null) {
            return false;
        }
        return checkinLsv.hasFocus() || checkoutLsv.hasFocus();
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
                if (position == checkinPosition) {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                    textTxv.setTextColor(TKConfig.COLOR_ORANGE);
                } else {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                    textTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                }
            } else {
                if (position == checkoutPosition) {
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

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.confirm_btn:
                confirm();
                break;

            case R.id.cancel_btn:
                cancel();
                break;
                
            default:
                break;
        }
    }
    
    String makeCheckinDateString(Calendar calendar, int add) {
        String result = null;
        calendar.add(Calendar.DAY_OF_MONTH, add);
        result = calendar.get(Calendar.MONTH) + "月" + calendar.get(Calendar.DAY_OF_MONTH);
        if (add < 3) {
            result += days[add];
        } else {
            result += weedDays[calendar.get(Calendar.DAY_OF_WEEK)-1];
        }
        calendar.add(Calendar.DAY_OF_MONTH, -add);
        return result;
    }
    
    String makeCheckoutDateString(Calendar calendar, int add) {
        String result = null;
        int month = calendar.get(Calendar.MONTH);
        calendar.add(Calendar.DAY_OF_MONTH, add);
        result = (month == calendar.get(Calendar.MONTH) ? "" : month + "月" ) + calendar.get(Calendar.DAY_OF_MONTH) + "日离店";
        result += add;
        result += "晚";
        calendar.add(Calendar.DAY_OF_MONTH, -add);
        return result;
    }
    
    public Calendar getCheckin() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, confirmCheckinPosition);
        return calendar;
    }
    
    public Calendar getCheckout() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, confirmCheckinPosition+1+confirmCheckoutPosition);
        return calendar;
    }
}

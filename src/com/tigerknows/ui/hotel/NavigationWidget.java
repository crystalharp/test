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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * 酒店所有界面的底部导航控件
 */
public class NavigationWidget extends LinearLayout {
    
    /**
     * 当前状态的文字颜色
     */
    static final int TEXT_COLOR = 0xffffffff;

    public static final int STATE_SELECT_HOTEL = 0;

    public static final int STATE_SELECT_ROOM_TYPE = 1;

    public static final int STATE_WRITE_ORDER = 2;

    public static final int STATE_RESERVE_SUCCESS = 3;

    private TextView mSelectHotelTxv;
    private TextView mSelectRoomTypeTxv;
    private TextView mWriteOrderTxv;
    private TextView mReserveSuccessTxv;
    
    public NavigationWidget(Context context) {
        this(context, null);
    }

    public NavigationWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hotel_navigation, this, // we are the parent
                true);
        
        findViews();
        setListener();
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.hotelNavigationWidget);
        if (a != null) {
            int state = a.getInt(R.styleable.hotelNavigationWidget_navigationState, 0);
            
            if (state == STATE_SELECT_HOTEL) {
                mSelectHotelTxv.setBackgroundResource(R.drawable.bg_hotel_navigation_left);
                mSelectHotelTxv.setTextColor(TEXT_COLOR);
            } else if (state == STATE_SELECT_ROOM_TYPE) {
                mSelectRoomTypeTxv.setBackgroundResource(R.drawable.bg_hotel_navigation_middle);
                mSelectRoomTypeTxv.setTextColor(TEXT_COLOR);
            } else if (state == STATE_WRITE_ORDER) {
                mWriteOrderTxv.setBackgroundResource(R.drawable.bg_hotel_navigation_middle);
                mWriteOrderTxv.setTextColor(TEXT_COLOR);
            } else if (state == STATE_RESERVE_SUCCESS) {
                mReserveSuccessTxv.setBackgroundResource(R.drawable.bg_hotel_navigation_right);
                mReserveSuccessTxv.setTextColor(TEXT_COLOR);
            }
            a.recycle();
        }
    }

    protected void findViews() {
        mSelectHotelTxv = (TextView) findViewById(R.id.select_hotel_txv);
        mSelectRoomTypeTxv = (TextView) findViewById(R.id.select_room_type_txv);
        mWriteOrderTxv = (TextView) findViewById(R.id.write_order_txv);
        mReserveSuccessTxv = (TextView) findViewById(R.id.reserve_success_txv);
    }
    
    protected void setListener() {
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
        if (mSelectHotelTxv == null ||
                mSelectRoomTypeTxv == null ||
                mWriteOrderTxv == null ||
                mReserveSuccessTxv == null) {
            return false;
        }
        return mSelectHotelTxv.hasFocus() || mSelectRoomTypeTxv.hasFocus() || mWriteOrderTxv.hasFocus() || mReserveSuccessTxv.hasFocus();
    }
}

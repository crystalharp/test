package com.tigerknows.ui.hotel;

import com.tigerknows.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateWidget extends LinearLayout {

    private TextView mMonthTxv;
    
    private TextView mDayTxv;
    
    private TextView mWeekTxv;
    
    private TextView mCheckinTxv;
    
    private Calendar mCalendar;
    
    private SimpleDateFormat mMonthFormat;
    
    public DateWidget(Context context) {
        this(context, null);
    }

    public DateWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hotel_date_widget, this, // we are the parent
                true);
        
        mMonthFormat = new SimpleDateFormat(context.getString(R.string.simple_month_format));
        
        findViews();
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.hotelDateWidget);
        if (a != null) {
            CharSequence checkin = a.getText(R.styleable.hotelDateWidget_checkin);
            mCheckinTxv.setText(checkin);
            a.recycle();
        }
    }

    protected void findViews() {
        mMonthTxv = (TextView) findViewById(R.id.month_txv);
        mDayTxv = (TextView) findViewById(R.id.day_txv);
        mWeekTxv = (TextView) findViewById(R.id.week_txv);
        mCheckinTxv = (TextView) findViewById(R.id.checkin_txv);
    }
    
    public void setCalendar(Calendar calendar) {
        mCalendar = calendar;
        if (mCalendar == null) {
            return;
        }
        mMonthTxv.setText(mMonthFormat.format(mCalendar.getTime()));
        mDayTxv.setText(String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH)));
        mWeekTxv.setText(getContext().getResources().getStringArray(R.array.week_days)[mCalendar.get(Calendar.DAY_OF_WEEK)-1]);
    }
    
    public Calendar getCalendar() {
        return mCalendar;
    }
}

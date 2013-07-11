
package com.tigerknows.widget;

import com.tigerknows.R;
import com.tigerknows.common.ActionLog;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CollapseTextView extends LinearLayout implements View.OnClickListener {
    
    static final String TAG = "CollapseTextView";

    private TextView mLimitTxv;

    private TextView mFullTxv;

    private ImageView mIndicationImv;
    
    private String mActionTog;
    
    public CollapseTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.collapse_textview, this);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CollapseTextView);
        int maxLines = a.getInt(R.styleable.CollapseTextView_maxLines, -1);
        int textSize = a.getDimensionPixelSize(R.styleable.CollapseTextView_textSize, -1);
        a.recycle();
        
        findViews();
        setListener();
        
        if (maxLines > -1) {
            mLimitTxv.setMaxLines(maxLines);
        }
        
        if (textSize > -1) {
            mLimitTxv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            mFullTxv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }

    private void findViews() {
        mLimitTxv= (TextView) findViewById(R.id.limit_txv);
        mFullTxv= (TextView) findViewById(R.id.full_txv);
        mIndicationImv= (ImageView) findViewById(R.id.indication_imv);
    }
    
    private void setListener() {
        mLimitTxv.setOnClickListener(this);
        mFullTxv.setOnClickListener(this);
        mIndicationImv.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View view) {
        if (!TextUtils.isEmpty(mActionTog)) {
            ActionLog.getInstance(getContext()).addAction(mActionTog);
        }
        if (mLimitTxv.getVisibility() == View.VISIBLE) {
            mLimitTxv.setVisibility(View.GONE);
            mFullTxv.setVisibility(View.VISIBLE);
            mIndicationImv.setImageResource(R.drawable.icon_arrow_up);
        } else {
            mLimitTxv.setVisibility(View.VISIBLE);
            mFullTxv.setVisibility(View.GONE);
            mIndicationImv.setImageResource(R.drawable.icon_arrow_down);
        }
    }
    
    public void setMaxLines(int maxlines) {
        mLimitTxv.setMaxLines(maxlines);
    }
    
    public void setTextColor(int color) {
        mLimitTxv.setTextColor(color);
        mFullTxv.setTextColor(color);
    }
    
    public void reset() {
        mLimitTxv.setVisibility(View.VISIBLE);
        mFullTxv.setVisibility(View.GONE);
        mIndicationImv.setImageResource(R.drawable.icon_arrow_down);
    }
    
    public void setTextSize(int size) {
        mLimitTxv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        mFullTxv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }
    
    public void setText(String text) {
        mLimitTxv.setText(text);
        mFullTxv.setText(text);
    }
    
    public void setActionTag(String actionTag) {
        mActionTog = actionTag;
    }
}

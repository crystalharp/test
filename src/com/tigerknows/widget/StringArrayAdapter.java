package com.tigerknows.widget;

import com.tigerknows.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class StringArrayAdapter extends ArrayAdapter<String> {
    
    private static final int TEXTVIEW_RESOURCE_ID = R.layout.string_list_item;
    
    private LayoutInflater mLayoutInflater;
    private int[] mLeftCompoundResIdList;
    public boolean isRoundCorner = true;

    public StringArrayAdapter(Context context, String[] list) {
        this(context, list, null);
    }

    public StringArrayAdapter(Context context, List<String> list) {
        this(context, list, null);
    }

    public StringArrayAdapter(Context context, String[] list, int[] leftCompoundResIdList) {
        super(context, TEXTVIEW_RESOURCE_ID, list);
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLeftCompoundResIdList = leftCompoundResIdList;
    }

    public StringArrayAdapter(Context context, List<String> list, int[] leftCompoundResIdList) {
        super(context, TEXTVIEW_RESOURCE_ID, list);
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLeftCompoundResIdList = leftCompoundResIdList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mLayoutInflater.inflate(TEXTVIEW_RESOURCE_ID, parent, false);
        } else {
            view = convertView;
        }
        
        TextView textView = (TextView)view.findViewById(R.id.text_txv);
        textView.setText(getItem(position));
        
        if (mLeftCompoundResIdList != null && position < mLeftCompoundResIdList.length) {
            Drawable drawable = getContext().getResources().getDrawable(mLeftCompoundResIdList[position]);
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                textView.setCompoundDrawablePadding(10);
                textView.setCompoundDrawables(drawable, null, null, null);
            }
        }

        if (isRoundCorner) {
            int count = getCount();
            if (count == 1) {
                view.setBackgroundResource(R.drawable.list_single);
            } else if (position == 0) {
                view.setBackgroundResource(R.drawable.list_header);
            } else if (position == count-1) {
                view.setBackgroundResource(R.drawable.list_footer);
            } else {
                view.setBackgroundResource(R.drawable.list_middle);
            }
        }

        return view;
    }
}

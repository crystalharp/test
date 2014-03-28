package com.tigerknows.widget;

import com.tigerknows.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

public class MultichoiceArrayAdapter extends ArrayAdapter<String> {
    
    static final int RESOURCE_ID = R.layout.select_dialog_multichoice;
    
    private LayoutInflater mLayoutInflater;
    
    private boolean[] mStatuses;
    
    public MultichoiceArrayAdapter(Context context, String[] strings, boolean[] statuses) {
        super(context, RESOURCE_ID, strings);
        this.mStatuses = statuses;
        this.mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mLayoutInflater.inflate(RESOURCE_ID, parent, false);
        } else {
            view = convertView;
        }
        
        CheckedTextView textView = (CheckedTextView)view.findViewById(R.id.text1);
        textView.setText(getItem(position));
        textView.setChecked(mStatuses[position]);
        
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

        return view;
    }
}

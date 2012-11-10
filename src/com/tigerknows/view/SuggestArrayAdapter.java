package com.tigerknows.view;

import com.tigerknows.R;
import com.tigerknows.model.TKWord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SuggestArrayAdapter extends ArrayAdapter<TKWord> {
    public static final int TEXTVIEW_RESOURCE_ID = R.layout.string_list_item;
    
    private Context context;

    public SuggestArrayAdapter(Context context, int textViewResourceId, List<TKWord> objects) {
        super(context, TEXTVIEW_RESOURCE_ID, objects);
        this.context = context;
    }        

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(TEXTVIEW_RESOURCE_ID, parent, false);
        }
        
        TextView textTxv = (TextView)convertView.findViewById(R.id.text_txv);
        textTxv.setText(getItem(position).word);
        
        return convertView;
    }
}

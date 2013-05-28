package com.tigerknows.widget;

import java.util.List;

import com.tigerknows.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class SingleChoiceArrayAdapter extends ArrayAdapter<String> {
	
	protected static final int RESOURCE_ID = R.layout.single_choice_list_item;
	protected LayoutInflater mLayoutInflater;

	public SingleChoiceArrayAdapter(Context context, List<String> list) {
		super(context, RESOURCE_ID, list);
		mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// TODO Auto-generated constructor stub
	}
	@Override
	public View getView(final int position, View convertView, ViewGroup parent){
		View view;
		if(convertView == null) {
            view = this.mLayoutInflater.inflate(RESOURCE_ID, parent, false);
        } else {
            view = convertView;
        }
		TextView singleTextTxv = (TextView)view.findViewById(R.id.single_text_txv);
		singleTextTxv.setText(getItem(position));
		return view;
	}
}
package com.tigerknows.widget;

import java.util.List;

import com.tigerknows.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class SingleChoiceArrayAdapter extends ArrayAdapter<String> {
	
	protected static final int RESOURCE_ID = R.layout.single_choice_list_item;
	protected LayoutInflater mLayoutInflater;
	protected Context mContext;

	public SingleChoiceArrayAdapter(Context context, List<String> list) {
		super(context, RESOURCE_ID, list);
		mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mContext = context;
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
		view.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction() == MotionEvent.ACTION_UP){
					ImageView iv = (ImageView)v.findViewById(R.id.single_icon_imv);
					iv.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rdb_recovery_checked));
				}
				return false;
			}
		});

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
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.Sphinx;


/**
 * Custom Filter listview. */
public class ValidityListView extends LinearLayout {
    
    public interface CallBack {
        public void selected(Calendar calendar);
    }
    
    private Calendar calendar = Calendar.getInstance();
    
    private ListView parentLsv;
    private ListView childLsv;
    
    private CallBack callBack;
    private int selectedParentPosition = -1;
    private int selectedChildPosition = -1;
    
    private List<String> parentFilterList = new ArrayList<String>();
    private List<String> childFilterList = new ArrayList<String>();
    
    private MyAdapter parentAdapter;
    private MyAdapter childAdapter;
    private Calendar now;
    
    String actionTag;

    public void setData(Calendar calendar, CallBack callBack, String actionTag) {
        this.calendar = calendar;
        now = Calendar.getInstance();
        if (this.calendar == null) {
            this.calendar = now;
        }
        this.callBack = callBack;
        this.selectedParentPosition = -1;
        this.selectedChildPosition = -1;
        this.parentFilterList.clear();
        this.childFilterList.clear();

        for(int i = 0; i < 10; i++) {
            parentFilterList.add((now.get(Calendar.YEAR)+i)+"年");
        }

        this.selectedParentPosition = (this.calendar.get(Calendar.YEAR)-now.get(Calendar.YEAR));
        
        for(int i = (this.selectedParentPosition <= 0 ? now.get(Calendar.MONTH) : 0); i < 12; i++) {
            childFilterList.add((i+1)+"月");
        }
        
        this.selectedChildPosition = this.calendar.get(Calendar.MONTH) - (this.selectedParentPosition == 0 ? now.get(Calendar.MONTH) : 0);
        
        parentAdapter.notifyDataSetChanged();
        childAdapter.notifyDataSetChanged();
        if (selectedParentPosition > -1) {
            parentLsv.setSelectionFromTop(selectedParentPosition, 0);
        } else {
            parentLsv.setSelectionFromTop(0, 0);
        }
        
        if (this.selectedChildPosition > -1) {
            childLsv.setSelectionFromTop(selectedChildPosition, 0);
        } else {
            childLsv.setSelectionFromTop(0, 0);
        }
    }
    private Sphinx mSphinx;
    public ValidityListView(Sphinx sphinx) {
        this(sphinx, null);
    }

    public ValidityListView(Sphinx sphinx, AttributeSet attrs) {
        super(sphinx, attrs);
        mSphinx = sphinx;
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) sphinx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hotel_validity_list, this, // we are the parent
                true);
        
        findViews();
        setListener();
        
        parentAdapter = new MyAdapter(sphinx, parentFilterList);
        parentAdapter.isParent = true;
        childAdapter = new MyAdapter(sphinx, childFilterList);
        childAdapter.isParent = false;
        
        parentLsv.setAdapter(parentAdapter);
        childLsv.setAdapter(childAdapter);

        resizeLsv();
    }

    private void resizeLsv(){
        parentLsv.post(new Runnable() {
			
			@Override
			public void run() {
				setLsvHeigth(parentLsv, R.layout.hotel_validity_list_item, 5);
				setLsvHeigth(childLsv, R.layout.hotel_validity_list_item, 5);
			}
		});
    }
    
    protected void findViews() {
        parentLsv = (ListView) findViewById(R.id.parent_lsv);
        childLsv = (ListView) findViewById(R.id.child_lsv);
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
                return false;
            }
        });
        parentLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                if (position >= parentFilterList.size()) {
                    return;
                }
                selectedParentPosition = position;
                if(position != selectedParentPosition){
                	selectedChildPosition = 0;
                }
                parentAdapter.notifyDataSetChanged();
                
                Calendar now = Calendar.getInstance();
                
                childFilterList.clear();
                for(int i = (selectedParentPosition == 0 ? now.get(Calendar.MONTH) : 0); i < 12; i++) {
                    childFilterList.add((i+1)+"月");
                }
                selectedChildPosition = 0;
                childAdapter.notifyDataSetChanged();
                resizeLsv();
                childLsv.post(new Runnable() {
					@Override
					public void run() {
		                childLsv.smoothScrollToPositionFromTop(0, 0);
					}
				});
            }
        });
        childLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position >= childFilterList.size()) {
                    return;
                }
                selectedChildPosition = position;
                selected();
            }
        });
    }
    
    private void selected() {
    	calendar.set(Calendar.YEAR, now.get(Calendar.YEAR) + selectedParentPosition);
    	calendar.set(Calendar.MONTH, (selectedParentPosition == 0 ? now.get(Calendar.MONTH) : 0) + selectedChildPosition);
        this.callBack.selected(calendar);
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
        if (parentLsv == null || childLsv == null) {
            return false;
        }
        return parentLsv.hasFocus() || childLsv.hasFocus();
    }
    
    public void setLsvHeigth(ListView lsv, int itemResId, int maxCount){
    	if(maxCount<1){
    		return;
    	}
    	int itemHeight = getLsvItemHeight(lsv, R.layout.hotel_validity_list_item);
    	int count = lsv.getCount()>maxCount? maxCount:lsv.getCount();
    	int lsvHeigth = (itemHeight + lsv.getDividerHeight())*count;
//    	System.out.println("itemHeight: " + itemHeight);
//    	System.out.println("lsvHeigth: " + lsvHeigth);
    	ViewGroup.LayoutParams params = lsv.getLayoutParams();
    	params.height = lsvHeigth;
    	lsv.setLayoutParams(params);
    	
    }
    
    public int getLsvItemHeight(ListView parent, int itemResId){
    	ViewGroup templateView;
		// Inflate a new view for it.
       	templateView = (ViewGroup)mSphinx.getLayoutInflater().inflate(itemResId, parent, false);
    	((TextView)templateView.findViewById(R.id.text_txv)).setText("xx");
    	templateView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    	return templateView.getMeasuredHeight();
    }
    
    class MyAdapter extends ArrayAdapter<String> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.hotel_validity_list_item;
        
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
            
            TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
            
            String date = getItem(position);
            
            if (isParent) {
                if (position == selectedParentPosition) {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                    textTxv.setTextColor(getResources().getColor(R.color.orange));
                } else {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                    textTxv.setTextColor(getResources().getColor(R.color.black_dark));
                }
            } else if(position == selectedChildPosition){
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                textTxv.setTextColor(getResources().getColor(R.color.orange));
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                textTxv.setTextColor(getResources().getColor(R.color.black_dark));
            }
            
            textTxv.setText(date);
            
            return view;
        }
    }
}

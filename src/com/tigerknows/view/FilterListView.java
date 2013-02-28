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

package com.tigerknows.view;

import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.POIResponse;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
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
import java.util.List;


/**
 * Custom Filter listview. */
public class FilterListView extends LinearLayout implements View.OnClickListener {
    
    public interface CallBack {
        public void doFilter(String name);
        public void cancelFilter();
    }
    
    private ViewGroup controlView;
    
    private ListView parentLsv;
    private ListView childLsv;
    
    private CallBack callBack;
    private Filter selectedParentFilter;
    private Filter selectedChildFilter;
    private int selectedParentPosition = -1;
    
    private List<Filter> filterList;
    private Filter filter = null;
    private List<Filter> parentFilterList = new ArrayList<Filter>();
    private List<Filter> childFilterList = new ArrayList<Filter>();
    
    private MyAdapter parentAdapter;
    private MyAdapter childAdapter;
    
    private boolean isTurnPaging = false;
    
    public boolean isTurnPaging() {
        return isTurnPaging;
    }

    public void setData(List<Filter> filterList, byte key, CallBack callBack, boolean isTurnPaging) {
        if (filterList == null) {
            return;
        }
        if (key == POIResponse.FIELD_FILTER_AREA_INDEX) {
            ActionLog.getInstance(getContext()).addAction(ActionLog.FILTER_AREA_ONCLICK);
        } else if (key == POIResponse.FIELD_FILTER_CATEGORY_INDEX) {
            ActionLog.getInstance(getContext()).addAction(ActionLog.FILTER_CATEGORY_ONCLICK);
        } else if (key == POIResponse.FIELD_FILTER_ORDER_INDEX) {
            ActionLog.getInstance(getContext()).addAction(ActionLog.FILTER_ORDER_ONCLICK);
        }
        refreshFilterButton(controlView, filterList, getContext(), this);
        this.filterList = filterList;
        this.callBack = callBack;
        this.isTurnPaging = isTurnPaging;
        this.selectedParentFilter = null;
        this.selectedChildFilter = null;
        this.selectedParentPosition = -1;
        this.parentFilterList.clear();
        this.childFilterList.clear();

        for(int i = this.filterList.size()-1; i >= 0; i--) {
            Filter filter1 = this.filterList.get(i);
            if (filter1.getKey() == key) {
                this.filter = filter1;
            }
        }
        
        for(int i = 0, size = controlView.getChildCount(); i < size; i++) {
            View view = controlView.getChildAt(i);
            if (view instanceof Button) {
                Button button = (Button) view;
                if (key == (Byte)button.getTag()) {
                    button.setBackgroundResource(R.drawable.btn_tab_selected);
                } else {
                    button.setBackgroundResource(R.drawable.btn_tab);
                }
            }
        }
        
        List<Filter> filterList1 = this.filter.getChidrenFilterList();
        this.parentFilterList.addAll(filterList1);
        
        int selectedChiledPosition = -1;
        for(int i = filterList1.size()-1; i >= 0; i--) {
            Filter filter1 = filterList1.get(i);
            List<Filter> filterList2 = filter1.getChidrenFilterList();
            if (filter1.isSelected()) {
                selectedParentFilter = filter1;
                selectedParentPosition = i;
                this.childFilterList.addAll(filterList2);
            } else {
                for(int j = filterList2.size()-1; j >= 0; j--) {
                    Filter filter2 = filterList2.get(j);
                    if (filter2.isSelected()) {
                        selectedParentPosition = i;
                        selectedChiledPosition = j;
                        selectedParentFilter = filter1;
                        selectedChildFilter = filter2;
                        this.childFilterList.addAll(filterList2);
                    }
                }
                if (selectedChildFilter != null && selectedParentFilter != null) {
                    break;
                }
            }
        }
        parentAdapter.notifyDataSetChanged();
        childAdapter.notifyDataSetChanged();
        if (selectedParentPosition >= 0)
            parentLsv.setSelectionFromTop(selectedParentPosition, 0);
        
        if (selectedChiledPosition >= 0)
            childLsv.setSelectionFromTop(selectedChiledPosition, 0);
    }
        
    public FilterListView(Context context) {
        this(context, null);
    }

    public FilterListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.filter_list, this, // we are the parent
                true);
        
        findViews();
        setListener();
        
        parentAdapter = new MyAdapter(context, parentFilterList);
        parentAdapter.isParent = true;
        childAdapter = new MyAdapter(context, childFilterList);
        childAdapter.isParent = false;
        
        parentLsv.setAdapter(parentAdapter);
        childLsv.setAdapter(childAdapter);
    }

    protected void findViews() {
        controlView = (ViewGroup) findViewById(R.id.control_view);
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
                cancel();
                return false;
            }
        });
        parentLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                if (position >= parentFilterList.size()) {
                    return;
                }
                Filter filter = parentFilterList.get(position);
                List<Filter> filterList = filter.getChidrenFilterList();
                if (filterList.size() == 0) {
                    childFilterList.clear();
                    childAdapter.notifyDataSetChanged();
                    doFilter(filter);
                } else {
                    selectedParentPosition = position;
                    parentAdapter.notifyDataSetChanged();
                    childFilterList.clear();
                    childFilterList.addAll(filterList);
                    childAdapter.notifyDataSetChanged();
                    int selectedChiledPosition = -1;
                    for(int j = childFilterList.size()-1; j >= 0; j--) {
                        Filter filter2 = childFilterList.get(j);
                        if (filter2.isSelected()) {
                            selectedChiledPosition = j;
                            break;
                        }
                    }
                    
                    if (selectedChiledPosition > -1)
                        childLsv.setSelectionFromTop(selectedChiledPosition, 0);
                }
            }
        });
        childLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (position >= childFilterList.size()) {
                    return;
                }
                Filter filter = childFilterList.get(position);
                doFilter(filter);
            }
        });
    }
    
    private void doFilter(Filter filter) {
        if (selectedChildFilter != null) {
            selectedChildFilter.setSelected(false);
        }
        if (selectedParentFilter != null) {
            selectedParentFilter.setSelected(false);
        }
        filter.setSelected(true);
        
        if (callBack != null) {
            ActionLog.getInstance(getContext()).addAction(ActionLog.FILTER_SELECTED, filter.getFilterOption().getName(), DataQuery.makeFilterRequest(this.filterList));
            callBack.doFilter(filter.getFilterOption().getName());
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
        if (parentLsv == null || childLsv == null) {
            return false;
        }
        return parentLsv.hasFocus() || childLsv.hasFocus();
    }
    
    public void cancel() {
        if (callBack != null) {
            ActionLog.getInstance(getContext()).addAction(ActionLog.FILTER_CANCEL);
            callBack.cancelFilter();
        }
    }
    
    class MyAdapter extends ArrayAdapter<Filter> {
        
        private static final int TEXTVIEW_RESOURCE_ID = R.layout.filter_list_item;
        
        private LayoutInflater mLayoutInflater;
        
        boolean isParent = false;

        public MyAdapter(Context context, List<Filter> list) {
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
            TextView textTxv = (TextView)view.findViewById(R.id.text_txv);
            
            Filter filter = getItem(position);
            
            if (isParent) {
                if (position == selectedParentPosition) {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
                } else {
                    view.setBackgroundResource(R.drawable.list_selector_background_gray_dark);
                }
            } else {
                view.setBackgroundResource(R.drawable.list_selector_background_gray_light);
            }
            
            if (filter.isSelected()) {
                textTxv.setTextColor(TKConfig.COLOR_ORANGE);
            } else {
                if (isParent) {
                    textTxv.setTextColor(TKConfig.COLOR_BLACK_DARK);
                } else {
                    textTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                }
            }
            
            if (filter.getChidrenFilterList().size() > 0) {
                iconImv.setBackgroundResource(R.drawable.icon_arrow_right);
                iconImv.setVisibility(View.VISIBLE);
            } else {
                iconImv.setVisibility(View.INVISIBLE);
            }
            
            textTxv.setText(filter.getFilterOption().getName());
            
            return view;
        }
    }

    
    public static void refreshFilterButton(ViewGroup filterViewGroup, List<Filter> filterList, Context context, View.OnClickListener onClickListener) {
        if (filterList.isEmpty()) {
            filterViewGroup.setVisibility(View.GONE);
            return;
        }
        filterViewGroup.setVisibility(View.VISIBLE);
        
        Button button;
        int count = filterViewGroup.getChildCount();
        int size = filterList.size();
        
        for(int i = 0; i < count; i++) {
            filterViewGroup.getChildAt(i).setVisibility(View.GONE);
        }
        
        int j = 0;
        for(int i = 0; i < size; i++) {
            Filter filter = filterList.get(i);
            if (j < count) {
                if (i > 0) {
                    filterViewGroup.getChildAt(j++).setVisibility(View.VISIBLE);
                }
                button = (Button) filterViewGroup.getChildAt(j++);
                button.setVisibility(View.VISIBLE);
            } else {
                if (i > 0) {
                    ImageView imageView = new ImageView(context);
                    imageView.setImageResource(R.drawable.ic_split);
                    filterViewGroup.addView(imageView);
                }
                button = makeFitlerButton(context);
                filterViewGroup.addView(button);
            }
            button.setTag(filter.getKey());
            button.setText(FilterListView.getFilterTitle(context, filter));
            button.setOnClickListener(onClickListener);            
        }
    }

    public static Button makeFitlerButton(Context context) {
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);        
        layoutParams.weight = 1;
        
        Resources resources = context.getResources();
        Button button = new Button(context);
        button.setLayoutParams(layoutParams);
        button.setBackgroundResource(R.drawable.btn_tab);
        button.setTextColor(resources.getColor(R.color.black_dark));
        Drawable right = resources.getDrawable(R.drawable.ic_small_triangle_down);
        right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicWidth());
        button.setCompoundDrawablesWithIntrinsicBounds(null, null, right, null);
//        button.setPadding(Util.dip2px(Globals.g_metrics.density, 6), 0, Util.dip2px(Globals.g_metrics.density, 24), 0);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setSingleLine(true);
        button.setGravity(Gravity.CENTER);
        return button;
    }
    
    public static String getFilterTitle(Context context, Filter filter) {
        String title = null;
        if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            for(Filter chidrenFilter : chidrenFilterList) {
                if (chidrenFilter.isSelected()) {
                    title = chidrenFilter.getFilterOption().getName();
                    break;
                } else {
                    
                    List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                    for(Filter chidrenFilter1 : chidrenFilterList1) {
                        if (chidrenFilter1.isSelected()) {
                            title = chidrenFilter1.getFilterOption().getName();
                            break;
                        }
                    }
                    
                    if (!TextUtils.isEmpty(title)) {
                        String allAnyone = context.getString(R.string.all_anyone, "");
                        if (title.contains(allAnyone)) {
                            title = chidrenFilter.getFilterOption().getName();
                        }
                        break;
                    }
                }
            }   
            
            if (title == null && chidrenFilterList.size() > 0) {
                title = chidrenFilterList.get(0).getFilterOption().getName();
            }

        }
        return title;
    }

    @Override
    public void onClick(View view) {
        byte key = (Byte)view.getTag();
        setData(filterList, key, callBack, isTurnPaging);
    }
}

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

package com.tigerknows.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.model.DataQuery.FilterArea;
import com.tigerknows.model.DataQuery.POIResponse;


/**
 * Custom Filter listview. */
public class FilterListView extends LinearLayout implements View.OnClickListener {
    
    public interface CallBack {
        public void doFilter(String name);
        public void cancelFilter();
    }
    
    private ViewGroup controlView;
    
    private ListView parentLsv;
    private PinnedHeaderBladeListView childLsv;
    private BladeView bladeView;
    
    private CallBack callBack;
    private int selectedParentPosition = -1;
    private int selectedChiledPosition = -1;
    
    private List<Filter> filterList;
    private Filter filter = null;
    private List<Filter> parentFilterList = new ArrayList<Filter>();
    private List<Filter> childFilterList = new ArrayList<Filter>();
    
    private MyAdapter parentAdapter;
    
    private boolean isTurnPaging = false;
    
    private byte key = -1;
    
    String actionTag;
    Handler handler;
    
    boolean isAreaFilter = false;
    
    public boolean isTurnPaging() {
        return isTurnPaging;
    }

    public void setData(List<Filter> filterList, byte key, CallBack callBack, boolean isTurnPaging, String actionTag) {
        setData(filterList, key, callBack, isTurnPaging, true, actionTag);
    }

    public void setData(List<Filter> filterList, byte key, CallBack callBack, boolean isTurnPaging, boolean showFilterButton, String actionTag) {
        if (filterList == null) {
            return;
        }
        this.actionTag = actionTag;
        isAreaFilter = false;
        if (key == POIResponse.FIELD_FILTER_AREA_INDEX) {
            ActionLog.getInstance(getContext()).addAction(this.actionTag+ActionLog.FilterArea);
            isAreaFilter = true;
        } else if (key == POIResponse.FIELD_FILTER_CATEGORY_INDEX) {
            ActionLog.getInstance(getContext()).addAction(this.actionTag+ActionLog.FilterCategory);
        } else if (key == POIResponse.FIELD_FILTER_ORDER_INDEX) {
            ActionLog.getInstance(getContext()).addAction(this.actionTag+ActionLog.FilterOrder);
        }
        if (showFilterButton) {
            refreshFilterButton(controlView, filterList, getContext(), this);
        }
        this.filterList = filterList;
        this.callBack = callBack;
        this.isTurnPaging = isTurnPaging;
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

        if (this.filter != null) {
            List<Filter> parentFilterList2 = this.filter.getChidrenFilterList();
            this.parentFilterList.addAll(parentFilterList2);
            for(int i = parentFilterList2.size()-1; i >= 0; i--) {
                Filter tempParentFilter = parentFilterList2.get(i);
                List<Filter> childFilterList2 = tempParentFilter.getChidrenFilterList();
                if (tempParentFilter.isSelected()) {
                    selectedParentPosition = i;
                    selectedChiledPosition = 0;
                    this.childFilterList.addAll(childFilterList2);
                } else {
                	
                    for(int j = childFilterList2.size()-1; j >= 0; j--) {
                        Filter filter2 = childFilterList2.get(j);
                        // 此处忽略全部区域下边的子筛选项的选中情况。
                        if (filter2.isSelected()) {
                        	
                        	if(tempParentFilter.getFilterOption().getId()!=0){
	                            selectedParentPosition = i;
	                            selectedChiledPosition = j;
	                            this.childFilterList.addAll(childFilterList2);
                        	}else if(j==0){ //说明当前选中的是“全部区域”
	                            selectedParentPosition = i;
	                            selectedChiledPosition = j;
	                            this.childFilterList.addAll(childFilterList2);
                        	}
                        	
                        }
                    }
                }
            }
            
        }

        parentAdapter.notifyDataSetChanged();
        
        final int finalselectedChiledPosition = selectedChiledPosition;
        handler.post(new Runnable() {
            
            @Override
            public void run() {
                if (selectedParentPosition > 0 ) {
                	int topPosition = selectedParentPosition-1;
                    parentLsv.setSelectionFromTop(topPosition, 0);
                } else {
                    parentLsv.setSelectionFromTop(0, 0);
                }
                
                if (finalselectedChiledPosition > 0) {
                	int topPosition = finalselectedChiledPosition-1;
                    childLsv.getBaseListView().setSelectionFromTop(topPosition, 0);
                } else {
                    childLsv.getBaseListView().setSelectionFromTop(0, 0);
                }
            }
        });
        
        // 如果没有被选中的filter，则默认高亮显示 “全部区域”
        if (selectedParentPosition == -1) {
            selectedParentPosition = 0;
            parentAdapter.notifyDataSetChanged();
            
            childFilterList.clear();
            if (this.filter != null &&
                    this.filter.getChidrenFilterList() != null &&
                    this.filter.getChidrenFilterList().size() > 0 &&
                    this.filter.getChidrenFilterList().get(0) != null) {
                childFilterList.addAll(this.filter.getChidrenFilterList().get(0).getChidrenFilterList());
            }
        }
        
        // 如果父筛选项选中的是全部区域(id=0)，并且当权筛选项是位置筛选项的时候，设置pinnedModed为true
        boolean pinnedMode = false;
        if ( parentFilterList.get(selectedParentPosition).getFilterOption().getId() == 0  && isAreaFilter){
        	pinnedMode = true;
        }
        
        // 如果是区域筛选，并且当前选择的父筛选项位置是全部区域，即第0个， 则列表设置为pinnedMode。
       	childLsv.setData(childFilterList, pinnedMode, pinnedMode, selectedChiledPosition);
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

        handler = new Handler();
        parentAdapter = new MyAdapter(context, parentFilterList);
        parentAdapter.isParent = true;
        
        parentLsv.setAdapter(parentAdapter);
        childLsv.setData(childFilterList, false, false, -1);
    }

    protected void findViews() {
        controlView = (ViewGroup) findViewById(R.id.control_view);
        parentLsv = (ListView) findViewById(R.id.parent_lsv);
        childLsv = (PinnedHeaderBladeListView) findViewById(R.id.child_lsv);
        bladeView = (BladeView) findViewById(R.id.blade_view);
        bladeView.setSphinx(getContext());
        
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
                ActionLog.getInstance(getContext()).addAction(actionTag + ActionLog.PopupWindowFilterGroup, position, filter.getFilterOption().getName());
                List<Filter> filterList = filter.getChidrenFilterList();
                
                if (filterList.size() == 0) {
                    childFilterList.clear();
                    childLsv.getAdapter().notifyDataSetChanged();
                    doFilter(filter);
                } else {
                	int lastSelectedParentPosition = selectedParentPosition;
                    selectedParentPosition = position;
                    parentAdapter.notifyDataSetChanged();
                    childFilterList.clear();
                    childFilterList.addAll(filterList);
                    
                    if(isAreaFilter){
                    	//此处多做判断目的是减少setData里边的重复计算
                    	int lastId = parentFilterList.get(lastSelectedParentPosition).getFilterOption().getId();
                    	int curId = parentFilterList.get(position).getFilterOption().getId();
                    	if(curId == 0 && lastId != 0){
                    		childLsv.setData(childFilterList, true, true, -1);
                    	}else if(curId !=0 && lastId == 0){
                    		childLsv.setData(childFilterList, false, false, -1);
                    	}else{
                    		childLsv.getAdapter().notifyDataSetChanged();
                    	}
                    }else{
                		childLsv.getAdapter().notifyDataSetChanged();
                    }
                    
                    int selectedChiledPosition = -1;
                    for(int j = childFilterList.size()-1; j >= 0; j--) {
                        Filter filter2 = childFilterList.get(j);
                        if (filter2.isSelected()) {
                            selectedChiledPosition = j;
                            break;
                        }
                    }
                    
                    if (selectedParentPosition!=0 && selectedChiledPosition > -1) {
                        childLsv.getBaseListView().setSelectionFromTop(selectedChiledPosition, 0);
                    } else {
                        childLsv.getBaseListView().setSelectionFromTop(0, 0);
                    }
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
                ActionLog.getInstance(getContext()).addAction(actionTag + ActionLog.PopupWindowFilterChild, position, filter.getFilterOption().getName());
                doFilter(filter);

            }
        });
    }
    
    private void doFilter(Filter filter) {
        selectedFilter(this.filter, filter);
        
        if (callBack != null) {
            ActionLog.getInstance(getContext()).addAction(actionTag + ActionLog.FilterDo, filter.getFilterOption().getName(), DataQuery.makeFilterRequest(this.filterList));
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
            ActionLog.getInstance(getContext()).addAction(actionTag+ActionLog.FilterCancel);
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
        button.setPadding(0, 0, Util.dip2px(Globals.g_metrics.density, 2), 0);
        return button;
    }
    
    public static String getFilterTitle(Context context, Filter filter) {
        String title = null;
        if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            for(int i = 0, size = chidrenFilterList.size(); i < size; i++) {
                Filter chidrenFilter = chidrenFilterList.get(i);
                if (chidrenFilter.isSelected()) {
                    title = chidrenFilter.getFilterOption().getName();
                    break;
                } else {
                    
                    List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                    for(int j = 0, count = chidrenFilterList1.size(); j < count; j++) {
                        Filter chidrenFilter1 = chidrenFilterList1.get(j);
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
        setData(filterList, key, callBack, isTurnPaging, this.actionTag);
    }
    
    public static boolean selectedFilter(Filter filter, Filter selected) {
        return selectedFilter(filter, selected.getFilterOption().getId());
    }
    
    public static boolean selectedFilter(Filter filter, int id) {
    	boolean result = false;
    	if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            for(int i = 0, size = chidrenFilterList.size(); i < size; i++) {
                Filter chidrenFilter = chidrenFilterList.get(i);
                if (chidrenFilter.getFilterOption().getId() == id) {
                    chidrenFilter.setSelected(true);
                    result = true;
                } else {
                    chidrenFilter.setSelected(false);
                }
                List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                for(int j = 0, count = chidrenFilterList1.size(); j < count; j++) {
                    Filter chidrenFilter1 = chidrenFilterList1.get(j);
                    if (chidrenFilter1.getFilterOption().getId() == id) {
                        chidrenFilter1.setSelected(true);
                        result = true;   
                    } else {
                        chidrenFilter1.setSelected(false);
                    }
                }
            }
    	}
        return result;
    }
    
    public static int getSelectedIdByFilter(Filter filter) {
    	int result = 0;
    	if (filter != null) {
	        List<Filter> chidrenFilterList = filter.getChidrenFilterList();
	        if (chidrenFilterList != null) {
		        for(int i = 0, size = chidrenFilterList.size(); i < size; i++) {
		            Filter chidrenFilter = chidrenFilterList.get(i);
		            if (chidrenFilter.isSelected()) {
		            	result = chidrenFilter.getFilterOption().getId();
		            }
		            List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
		            for(int j = 0, count = chidrenFilterList1.size(); j < count; j++) {
		                Filter chidrenFilter1 = chidrenFilterList1.get(j);
		                if (chidrenFilter1.isSelected()) {
		                	result = chidrenFilter1.getFilterOption().getId();
		                }
		            }
		        }
	        }
    	}
        return result;
    }
    
    public static Filter[] getSelectedFilter(Filter filter) {
        Filter[] result = null;
        if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            if (chidrenFilterList != null) {
                for(int i = 0, size = chidrenFilterList.size(); i < size; i++) {
                    Filter chidrenFilter = chidrenFilterList.get(i);
                    if (filter.getKey() == FilterArea.FIELD_LIST &&
                            chidrenFilter.getFilterOption().getId() == 0) {
                        continue;
                    }
                    if (chidrenFilter.isSelected()) {
                        result = new Filter[1];
                        result[0] = chidrenFilter;
                        return result;
                    }
                    List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                    for(int j = 0, count = chidrenFilterList1.size(); j < count; j++) {
                        Filter chidrenFilter1 = chidrenFilterList1.get(j);
                        if (chidrenFilter1.isSelected()) {
                            result = new Filter[2];
                            result[0] = chidrenFilter;
                            result[1] = chidrenFilter1;
                            return result;
                        }
                    }
                }
            }
        }
        return result;
    }
}

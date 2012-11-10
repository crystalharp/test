package com.tigerknows.view;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.model.DataQuery.Filter;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import java.util.ArrayList;
import java.util.List;

public class FilterExpandableListAdapter extends BaseExpandableListAdapter {
    private static final int GROUP_RESOURCE_ID = R.layout.filter_list_group_item;
    private static final int CHILD_RESOURCE_ID = R.layout.filter_list_child_item;

    private LayoutInflater layoutInflater;
    
    private List<Filter> filterList;
    private List<String> groupStringList = new ArrayList<String>();
    private List<ArrayList<String>> childStringList = new ArrayList<ArrayList<String>>();

    public FilterExpandableListAdapter(Context context, Filter filter) {
        super();

        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        filterList = filter.getChidrenFilterList();
        
        if (filterList == null) {
            return;
        }
        
        for(Filter filterOption : filterList) {
            groupStringList.add(filterOption.getFilterOption().getName());
            ArrayList<String> list = new ArrayList<String>();
            List<Filter> childFilterOptionList = filterOption.getChidrenFilterList();
            for(Filter chlidFilterOption : childFilterOptionList) {
                list.add(chlidFilterOption.getFilterOption().getName());
            }
            childStringList.add(list);
        }
    }
    
    public Object getChild(int groupPosition, int childPosition) {
        return childStringList.get(groupPosition).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        return childStringList.get(groupPosition).size();
    }
    
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {

        View view;
        if (convertView == null) {
            view = layoutInflater.inflate(CHILD_RESOURCE_ID, parent, false);
        } else {
            view = convertView;
        }
        
        TextView textView = (TextView) view.findViewById(R.id.text_txv);
        textView.setText("     "+getChild(groupPosition, childPosition).toString());

        ImageView imageView = (ImageView) view.findViewById(R.id.select_imv);
        if (filterList.get(groupPosition).getChidrenFilterList().get(childPosition).isSelected()) {
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    public Object getGroup(int groupPosition) {
        return groupStringList.get(groupPosition);
    }

    public int getGroupCount() {
        return groupStringList.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {

        View view;
        if (convertView == null) {
            view = layoutInflater.inflate(GROUP_RESOURCE_ID, parent, false);
        } else {
            view = convertView;
        }

        TextView textView = (TextView) view.findViewById(R.id.text_txv);
        textView.setText(getGroup(groupPosition).toString());

        ImageView imageView = (ImageView) view.findViewById(R.id.icon_imv);
        if (getChildrenCount(groupPosition) > 0) {
            imageView.setVisibility(View.VISIBLE);
            if (isExpanded) {            
                imageView.setBackgroundResource(R.drawable.icon_arrow_up);
            } else {
                imageView.setBackgroundResource(R.drawable.icon_arrow_down);
            }
        } else {
            if (filterList.get(groupPosition).isSelected()) {
                imageView.setBackgroundResource(R.drawable.icon_right);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
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
        for(int i = 0; i < size; i++) {
            Filter filter = filterList.get(i);
            if (i < count) {
                button = (Button) filterViewGroup.getChildAt(i);
                button.setVisibility(View.VISIBLE);
            } else {
                button = FilterExpandableListAdapter.makeFitlerButton(context);
                filterViewGroup.addView(button);
            }
            button.setId(filter.getKey());
            button.setText(getFilterTitle(context, filter));
            button.setTag(filter);
            button.setOnClickListener(onClickListener);            
        }
        
        for(int i = size; i < count; i++) {
            filterViewGroup.getChildAt(i).setVisibility(View.GONE);
        }
    }

    public static Button makeFitlerButton(Context context) {
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = Util.dip2px(Globals.g_metrics.density, 4);
        layoutParams.topMargin = Util.dip2px(Globals.g_metrics.density, 4);
        layoutParams.rightMargin = Util.dip2px(Globals.g_metrics.density, 2);
        layoutParams.bottomMargin = Util.dip2px(Globals.g_metrics.density, 4);        
        layoutParams.weight = 1;
        
        Button button = new Button(context);
        button.setLayoutParams(layoutParams);
        button.setBackgroundResource(R.drawable.btn_spinner);
        button.setPadding(Util.dip2px(Globals.g_metrics.density, 6), 0, Util.dip2px(Globals.g_metrics.density, 24), 0);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setSingleLine(true);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(0xff000000);
        return button;
    }
    
    public static int getFilterSelectParentPosition(Filter filter) {
        int position = 0;
        if (filter != null) {
            List<Filter> chidrenFilterList = filter.getChidrenFilterList();
            for(Filter chidrenFilter : chidrenFilterList) {
                if (chidrenFilter.isSelected()) {
                    break;
                } else {
                    boolean isBreak = false;
                    List<Filter> chidrenFilterList1 = chidrenFilter.getChidrenFilterList();
                    for(Filter chidrenFilter1 : chidrenFilterList1) {
                        if (chidrenFilter1.isSelected()) {
                            isBreak = true;
                            break;
                        }
                    }
                    if (isBreak) {
                        break;
                    }
                }
                position++;
            }
            if (position >= chidrenFilterList.size()) {
                position = 0;
            }
        }
        
        return position;
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
}
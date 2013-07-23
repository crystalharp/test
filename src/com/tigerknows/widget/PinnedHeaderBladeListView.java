package com.tigerknows.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.util.HanziUtil;
import com.tigerknows.widget.BladeView.OnBladeItemClickListener;

public class PinnedHeaderBladeListView extends LinearLayout {

	private PinnedHeaderListView mListView;
	private BladeView mBladeView;
	private PinnedHeaderListViewAdapter mAdapter;
	private View mPinnedHeaderView;
	
	// 首字母集
	private List<String> mSections;
	// 根据首字母存放数据
	private Map<String, List<String>> mFirstChar2ItemsMap;
	// 首字母位置集
	private List<Integer> mPositions;
	// 首字母对应的位置
	private Map<String, Integer> mIndexer;

	private Context myContext;
	
	public PinnedHeaderBladeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		myContext = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pinned_header_blade_list_view, this, // we are the parent
                true);
        
        findViews();
        setListeners();
	}

	private TextView indicatorTxv;
	
	private void findViews() {
		
		mListView = (PinnedHeaderListView) findViewById(R.id.pinned_header_list_view);
        mPinnedHeaderView = LayoutInflater.from(myContext).inflate(R.layout.pinned_header_listview_head, mListView, false);
		mBladeView = (BladeView) findViewById(R.id.blade_view);
		indicatorTxv = (TextView) findViewById(R.id.slide_indicator);
		mBladeView.setPopupText(indicatorTxv);
		
	}
	
	private void setListeners(){

		mBladeView.setOnBladeItemClickListener(new OnBladeItemClickListener() {

			@Override
			public void onItemClick(String s) {
				if (mIndexer.get(s) != null) {
					mListView.setSelection(mIndexer.get(s));
				}
			}
		});

	}
	
	private List<Filter> mData;

	public void setData(List<Filter> data, boolean pinnedMode, boolean notPinFirst) {
		this.mData = data;
		
		mBladeView.setVisibility(pinnedMode?View.VISIBLE:View.GONE);
		
		if(pinnedMode){
			
			mSections = new ArrayList<String>();
			mFirstChar2ItemsMap = new HashMap<String, List<String>>();
			mPositions = new ArrayList<Integer>();
			mIndexer = new HashMap<String, Integer>();
			
			if(notPinFirst && mData.size()>0){
				String itemStr = mData.get(0).getFilterOption().getName();
				List<String> list = new ArrayList<String>();
				list.add(itemStr);
				mSections.add("#");
				mFirstChar2ItemsMap.put("#", list);
			}
			
			for (int i = notPinFirst?1:0, size=mData.size(); i < size; i++) {
				
				String itemStr = mData.get(i).getFilterOption().getName();
				String firstChar = String.valueOf( HanziUtil.getFirstPinYinChar(itemStr));
				
				if (mSections.contains(firstChar)) {
					mFirstChar2ItemsMap.get(firstChar).add(itemStr);
				} else {
					mSections.add(firstChar);
					List<String> list = new ArrayList<String>();
					list.add(itemStr);
					mFirstChar2ItemsMap.put(firstChar, list);
				}
			}
			
			Collections.sort(mSections);
			int position = 0;
			
			for (int i = 0; i < mSections.size(); i++) {
				mIndexer.put(mSections.get(i), position);// 存入map中，key为首字母字符串，value为首字母在listview中位置
				mPositions.add(position);// 首字母在listview中位置，存入list中
				position += mFirstChar2ItemsMap.get(mSections.get(i)).size();// 计算下一个首字母在listview的位置
			}
			
		}
		
		mAdapter = new PinnedHeaderListViewAdapter(myContext, mData, mSections, mPositions, pinnedMode, true);
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(mAdapter);
		mListView.setPinnedHeaderView(mPinnedHeaderView);
		
	}//end set data

	public void setOnItemClickListener(OnItemClickListener onItemClickListener){
		mListView.setOnItemClickListener(onItemClickListener);
	}
	
	public ListView getBaseListView(){
		return mListView;
	}
	
	public PinnedHeaderListViewAdapter getAdapter(){
		return mAdapter;
	}

}

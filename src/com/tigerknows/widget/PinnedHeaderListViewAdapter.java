package com.tigerknows.widget;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.model.DataQuery.Filter;
import com.tigerknows.widget.PinnedHeaderListView.PinnedHeaderAdapter;

public class PinnedHeaderListViewAdapter extends BaseAdapter implements SectionIndexer,
		PinnedHeaderAdapter, OnScrollListener {
	
	private int mLocationPosition = -1;
	private List<Filter> mDatas;
	// 首字母集
	private List<String> mFriendsSections;
	private List<Integer> mFriendsPositions;
	private LayoutInflater inflater;
	private boolean mPinnedMode;
	private boolean mNotPinFirst;

	private int selectedPosition = -1;
	
	public PinnedHeaderListViewAdapter(Context context, List<Filter> datas, List<String> friendsSections,
			List<Integer> friendsPositions, boolean pinnedMode, boolean notPinFirst, int selectedPosition) {
		inflater = LayoutInflater.from(context);
		mDatas = datas;
		mFriendsSections = friendsSections;
		mFriendsPositions = friendsPositions;
		mPinnedMode = pinnedMode;
		mNotPinFirst = notPinFirst;
		this.selectedPosition = selectedPosition;
	}

	@Override
	public int getCount() {
		return mDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return mDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.pinned_header_list_view_item, null);
		}
		LinearLayout mHeaderParent = (LinearLayout) convertView.findViewById(R.id.item_header_parent);
		
		if(!mPinnedMode || (mPinnedMode && mNotPinFirst && position==0)){
			mHeaderParent.setVisibility(View.GONE);
		}else{
			int section = getSectionForPosition(position);
			TextView mHeaderText = (TextView) convertView.findViewById(R.id.item_header_text);
			if (getPositionForSection(section) == position) {
				mHeaderParent.setVisibility(View.VISIBLE);
				mHeaderText.setText(mFriendsSections.get(section));
			} else {
				mHeaderParent.setVisibility(View.GONE);
			}
		}
		TextView textView = (TextView) convertView.findViewById(R.id.text_txv);
        ImageView iconImv = (ImageView)convertView.findViewById(R.id.icon_imv);
        
		textView.setText(mDatas.get(position).getFilterOption().getName());

		if(selectedPosition!=-1 && position == selectedPosition){
            iconImv.setVisibility(View.VISIBLE);
		}else{
		    iconImv.setVisibility(View.GONE);
		}
		
		return convertView;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		if (view instanceof PinnedHeaderListView) {
			((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
		}
	}

	@Override
	public int getPinnedHeaderState(int position) {
		if(!mPinnedMode){
			return PINNED_HEADER_GONE;
		}
		int realPosition = position;
		if (realPosition < 0
				|| (mLocationPosition != -1 && mLocationPosition == realPosition)) {
			return PINNED_HEADER_GONE;
		}
		mLocationPosition = -1;
		int section = getSectionForPosition(realPosition);
		if(section==0 && mNotPinFirst){
			return PINNED_HEADER_GONE;
		}
		int nextSectionPosition = getPositionForSection(section + 1);
		if (nextSectionPosition != -1
				&& realPosition == nextSectionPosition - 1) {
			return PINNED_HEADER_PUSHED_UP;
		}
		return PINNED_HEADER_VISIBLE;
	}

	@Override
	public void configurePinnedHeader(View header, int position, int alpha) {
		int realPosition = position;
		int section = getSectionForPosition(realPosition);
		if(section == -1){
			return;
		}
		String title = (String) getSections()[section];
		((TextView) header.findViewById(R.id.friends_list_header_text))
		.setText(title);
	}

	@Override
	public Object[] getSections() {
		return mFriendsSections.toArray();
	}

	@Override
	public int getPositionForSection(int section) {
		if (section < 0 || section >= mFriendsSections.size()) {
			return -1;
		}
		return mFriendsPositions.get(section);
	}

	@Override
	public int getSectionForPosition(int position) {
		
		if (position < 0 || position >= getCount()) {
			return -1;
		}
		int index = Arrays.binarySearch(mFriendsPositions.toArray(), position);
		return index >= 0 ? index : -index - 2;
	}

	public int getSelectedPosition() {
		return selectedPosition;
	}

	public void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

}

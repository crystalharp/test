package com.tigerknows.ui.poi;

import android.widget.LinearLayout;

public class DynamicMoviePOI extends POIDetailFragment.DynamicPOIView{
    
    DynamicMoviePOI instance = null;
    private DynamicMoviePOI(LinearLayout belongsLayout){
       mBelongsLayout = belongsLayout; 
    }
    
    public DynamicMoviePOI getInstance(LinearLayout belongsLayout){
       if (instance == null) {
           instance = new DynamicMoviePOI(belongsLayout);
       }
       return instance;
    }

//    onclick(){
//                
//            case R.id.dynamic_dianying_more_view:
//            	mShowDynamicDianyingMoreView = false;
//            	mDynamicDianyingMoreView.setVisibility(View.GONE);
//            	
//            	int size = mDynamicDianyingList.size();
//            	int viewCount = mDynamicDianyingListView.getChildCount();
//                for(int i = 0; i < viewCount && i < size; i++) {
//                	mDynamicDianyingListView.getChildAt(i).setVisibility(View.VISIBLE);
//                }
//                
//                for(int i = 0; i < viewCount; i++) {
//                    View child = mDynamicDianyingListView.getChildAt(i);
//                    if (i == (viewCount-1) && mDynamicDianyingMoreView.getVisibility() == View.GONE) {
//                        child.setBackgroundResource(R.drawable.list_footer);
//                        child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
//                    } else {
//                        child.setBackgroundResource(R.drawable.list_middle);
//                        child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
//                    }
//                }
//                break;
//    }
}

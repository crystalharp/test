package com.tigerknows.ui.poi;

import java.util.List;

import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.Response;
import com.tigerknows.model.XMapData;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;

import android.widget.LinearLayout;

public class DynamicMoviePOI extends POIDetailFragment.DynamicPOIView{
    
    DynamicMoviePOI instance = null;
    private DynamicMoviePOI(LinearLayout belongsLayout){
//       mBelongsLayout = belongsLayout; 
    }
    
    public DynamicMoviePOI getInstance(LinearLayout belongsLayout){
       if (instance == null) {
           instance = new DynamicMoviePOI(belongsLayout);
       }
       return instance;
    }

    @Override
    public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean checkExistence(POI poi) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<DynamicPOIViewBlock> getViewList(POI poi) {
        // TODO Auto-generated method stub
        return null;
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

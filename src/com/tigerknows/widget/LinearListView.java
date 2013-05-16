package com.tigerknows.widget;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Hotel.RoomType;
import com.tigerknows.model.POI.DynamicPOI;

/**
 * 由LinearLayout改的ListView
 * @author xupeng
 * 由于ListView不能用在ScrollView中,所以使用LinearLayout改出来了一个ListView来
 * 简化自己生成布局的代码.
 * ItemInitializer接口其实就是Adapter的getView函数,目前参数少个position,不知道
 * 会不会导致什么问题.
 * refreshList相当于ListView中修改了数据,然后notifyChanged.
 * 
 * 待优化,用对象缓冲池来解决child重复创建的问题.
 */
public class LinearListView {

    ItemInitializer initer;
    Sphinx mSphinx;
    LinearLayout parent;
    LayoutInflater mLayoutInflater;
    int mItemResId;
    
    public LinearListView(Sphinx sphinx, ItemInitializer i, int itemResId){
        this.initer = i;
        mSphinx = sphinx;
        parent = new LinearLayout(mSphinx);
        parent.setOrientation(LinearLayout.VERTICAL);
        mLayoutInflater = mSphinx.getLayoutInflater();
        mItemResId = itemResId;
    }
    
    public interface ItemInitializer{
        //v为传进去的可使用的view,函数不需要关心.实际上就是Adapter的getView
        void initItem(Object data, View v);
    }
    
    public LinearLayout getParentView(){
        return parent;
    }
    
    public void refreshList(List list) {
        int dataSize = (list != null ? list.size() : 0);
//        int childCount = contains.getChildCount();
//        int viewCount = 0;
        parent.removeAllViews();
        if(dataSize == 0){
            return;
        }else{
//            contains.setVisibility(View.VISIBLE);
            for(int i = 0; i < dataSize; i++) {
                Object data = list.get(i);
                View child;
//                if (viewCount < childCount) {
//                    child = contains.getChildAt(viewCount);
//                    child.setVisibility(View.VISIBLE);
//                } else {
//                    child = mLayoutInflater.inflate(mItemResId, parent, false);
                child = mLayoutInflater.inflate(mItemResId, null);
                    initer.initItem(data, child);
                    parent.addView(child, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                    child.setVisibility(View.VISIBLE);
//                parent.addView(child);
//                }
//                child.setTag(data);
//                child.setOnClickListener(mDynamicPOIListener);
//                if (data instanceof DynamicPOI) {
//                    initDynamicPOIItemView((DynamicPOI)data, child);
//                } else if (data instanceof Dianying) {
//                    initDianyingItemView((Dianying)data, child);
//                } else if (data instanceof RoomType) {
//                    initRoomTypeItemView((RoomType)data, child);
//                }
//                viewCount++;
            }
            
//            childCount = contains.getChildCount();
//            for(int i = viewCount; i < childCount; i++) {
//                contains.getChildAt(i).setVisibility(View.GONE);
//            }
        }
        
        return;
    }
    
    //TODO:再提供个foreach功能
    public int getSize(){
        return parent.getChildCount();
    }
    
    public View getChildView(int pos){
        return parent.getChildAt(pos);
    }
}

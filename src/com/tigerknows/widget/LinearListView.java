package com.tigerknows.widget;

import java.util.LinkedList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.tigerknows.Sphinx;

/**
 * 由LinearLayout改的ListView
 * @author xupeng
 * 由于ListView不能用在ScrollView中,所以使用LinearLayout改出来了一个ListView来
 * 简化自己生成布局的代码.
 * 
 * ItemInitializer接口其实就是Adapter的getView函数,目前参数少个position,不知道
 * 会不会导致什么问题.
 * 
 * 如果要进行和list的位置有关的操作,则可以配合使用getSize和getChildView来进行修改
 * 
 * refreshList相当于ListView中修改了数据,然后notifyChanged.
 * 
 * CackeableView是带有标记的View，目的是用对象缓冲池来解决child重复创建的问题.
 * 
 * 使用方法:
 * 布局文件:
 * 需要放一个LinearLayout
 * 代码里:
 * 首先获取到这个LinearLayout,假如mListView
 * 接着实现一下ItemInitializer接口,initItem传进去的两个参数是数据和inflate过的itemView.需要做的就是把数据赋给各个子View,
 * 对他们做一些其他需要的操作.
 * 然后创建一个LinearListView对象 
 * List = new LinearListView(mSphinx, mListView, initer, R.layout.item)
 * 这样就可以直接给这个LinearListView传数据来刷新显示内容了,做法是List.refreshList(dataList)
 */
public class LinearListView {

    ItemInitializer initer;
    Sphinx mSphinx;
    LinearLayout parentLayout;
    LayoutInflater mLayoutInflater;
    int mItemResId;
    //每个LinearListView对象配有一个缓冲池，用来缓冲item的view对象
    List<CacheableView> viewPool = new LinkedList<CacheableView>();
    
    public LinearListView(Sphinx sphinx, LinearLayout parent, ItemInitializer i, int itemResId){
        this.initer = i;
        mSphinx = sphinx;
        parentLayout = parent;
        mLayoutInflater = mSphinx.getLayoutInflater();
        mItemResId = itemResId;
    }
    
    public interface ItemInitializer{
        //v为传进去的可使用的view,函数不需要关心,已经做了对象缓冲,不会为null.这个接口实际上就是Adapter的getView
        void initItem(Object data, View v);
    }
    
    /**
     * 这个函数相当于ListView的notifyChanged,不同的是在使用时需要来回传递List,绑定List的做法需要来回复制数据,比较麻烦
     * 缺点是失去了类型安全,需要使用者注意,所传的List数据类型要能够被initItem处理
     * @param list
     */
    @SuppressWarnings("rawtypes")
	public void refreshList(List list) {
        int dataSize = (list != null ? list.size() : 0);
        parentLayout.removeAllViews();
//        List<View> tmp = new LinkedList<View>();
//        tmp.clear();
        for (CacheableView iter : viewPool) {
        	iter.showing = false;
        }
        if(dataSize == 0){
            return;
        }else{
            for(int i = 0; i < dataSize; i++) {
                Object data = list.get(i);
                View child;
                child = getInstance();
//                tmp.add(child);
                initer.initItem(data, child);
                parentLayout.addView(child, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                child.setVisibility(View.VISIBLE);
            }
            
        }
//        LogWrapper.d("conan", "LinearListView.refresh:" + tmp);
        
        return;
    }
    
    //TODO:再提供个foreach功能
    public int getSize(){
        return parentLayout.getChildCount();
    }
    
    public View getChildView(int pos){
        return parentLayout.getChildAt(pos);
    }
    
    private class CacheableView {
    	View v;
    	boolean showing = true;
    	
    	public CacheableView(int resId) {
    		v = mLayoutInflater.inflate(resId, null);
    	}
    }
    
	private View getInstance() {
	    CacheableView instance = null;
	    if (viewPool.size() == 0) {
	        instance = new CacheableView(mItemResId);
	        viewPool.add(instance);
	    } else {
	       //遍历缓冲池 
	        for (CacheableView iter : viewPool) {
	            //如果有不用的，则使用它
	            if (!iter.showing) {
	                instance = iter;
	                instance.showing = true;
	                break;
	            }
	        }
	        //遍历完发现都在用，则创建个新的
	        if (instance == null) {
		        instance = new CacheableView(mItemResId);
		        viewPool.add(instance);
	        }
	    }
	    return instance.v;
	}
}

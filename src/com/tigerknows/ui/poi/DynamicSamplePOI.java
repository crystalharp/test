package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.DataQuery;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

/**
 * 新建动态POI模板
 * 
 *   基于POI详情页写好的结构，所有新增加的动态POI的显示和查询相关代码不必出现在
 * POI详情页，而是在这样一个另外的类中嵌入到POI详情页。嵌入机制在POI详情页的注释
 * 中有说明。
 *   
 *   !!DynamicXXXPOI和ExtraXXXPOI的区别是这两类的初始化方式不同。Dynamic类的是
 * 出现在POI附加信息列表中的内容，初始化时会检测所有的附加信息并根据DPOIViewTable
 * 来进行初始化。Extra类的只会根据mExtraViewList来进行初始化。新增的时候根据需要
 * 来挑选用哪种。
 * 
 *   **新增动态POI需要在POI详情页新增的代码：
 *   增加一个本类变量并将其实例化，在initDynamicPOIEnv中添加对应的类别和实例。
 * @author xupeng
 *
 */
public class DynamicSamplePOI extends DynamicPOIView {

    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListAdapter xxxListAdapter;
    DynamicPOIViewBlock mViewBlock;
    
    LinearLayout xxxListView;
    
    public DynamicSamplePOI(POIDetailFragment poiFragment, LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        
        View layout = mInflater.inflate(R.layout.poi_dynamic_normal_poi, null);
        //该Block是个控制块，它的第一个参数是该Block要被加入的块区，第二个参数是Block要展示的layout
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, layout){

            @Override
            public void refresh() {
                if (mPOI == null) {
                    clear();
                }
                // TODO Auto-generated method stub
                /**
                 * 该函数是该block的刷新函数，会被该动态POI的refresh函数调用
                 * 是数据变化时的主要响应函数
                 */
                show();
            }
        };
        
        /**
         * 由于这个结构下所有的block都要被添加到ScrollView中，所以只能用一个LiearLayout 
         * 来改造成ListView，这就是一个适应这种LiearLayout的Adapter.
         * 相当于notifyDataSetChanged函数的为adapter.refreshList(xxxlist)
         * 不需要可以删掉
         */
        xxxListAdapter = new LinearListAdapter(mSphinx, xxxListView, 0) {

            @Override
            public View getView(Object data, View child, int pos) {
                // TODO Auto-generated method stub
                return null;
            }
            
        };
    }
    /**
     * 此函数为该类的查询返回处理函数，注意要用queryStart，
     * 不要用sphinx.queryStart.
     */
    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
	    mPOIDetailFragment.minusLoadingView();
        // TODO Auto-generated method stub

    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        // TODO Auto-generated method stub

    }

    /**
     * 此函数为POI详情页获取所有该类生成的ViewBlock的函数
     */
    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        blockList.clear();
        blockList.add(mViewBlock);
        return blockList;
    }

    /**
     * 此函数为该类型动态POI的整体刷新策略
     * 如果没有特殊需求，只要调用子Block的refresh方法即可
     */
    @Override
    public void refresh() {
        mViewBlock.refresh();
    }

    @Override
    public void loadData(int fromType) {
        // TODO Auto-generated method stub
        /**
         * 如果需要再次查询则进行query，不需要查询可以直接调用refresh方法
         */
        DataQuery dataQuery = new DataQuery(mSphinx);
        queryStart(dataQuery);
        mPOIDetailFragment.addLoadingView();
    }

}

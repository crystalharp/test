package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Dish;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataQuery.DishResponse;
import com.tigerknows.model.DataQuery.DishResponse.DishList;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BrowserActivity;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DynamicDishPOI extends POIDetailFragment.DynamicPOIView implements View.OnClickListener {
    
    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListView lsv;
    DynamicPOIViewBlock mViewBlock;
    List<Dish> mAllList = new ArrayList<Dish>();
    
    LinearLayout mDynamicDianyingView;
    LinearLayout mDynamicDianyingListView;
    
    ItemInitializer initer = new ItemInitializer(){

        @Override
        public void initItem(Object data, View view) {
            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            TextView commendTxv = (TextView)view.findViewById(R.id.commend_txv);

            Dish dish = (Dish) data;
            commendTxv.setText(String.valueOf(dish.getHitCount()));
            nameTxv.setText(dish.getName());
            priceTxv.setText(dish.getPrice());
        }
        
    };
    
    BlockRefresher mMovieRefresher = new BlockRefresher() {

        @Override
        public void refresh() {
            if (mPOI == null) {
                mViewBlock.clear();
                return;
            }
            
            mAllList.clear();
            
            List<Dish> list = mPOI.getDynamicDishList();        
            int size = (list != null ? list.size() : 0);
            if (size == 0) {
                mViewBlock.clear();
                return;
            }
            mAllList.addAll(list);
            
            lsv.refreshList(mAllList, 2);
        }
    };
    
    public DynamicDishPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, mMovieRefresher);
        mDynamicDianyingView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_dish, null);
        mDynamicDianyingListView = (LinearLayout) mDynamicDianyingView.findViewById(R.id.list_view);
        mViewBlock.mOwnLayout = mDynamicDianyingView;
        lsv = new LinearListView(mSphinx, mDynamicDianyingListView, initer, R.layout.poi_dynamic_dish_item);
        mDynamicDianyingView.findViewById(R.id.shopkeeper_txv).setOnClickListener(this);
    }

    @Override
    public void refresh() {
        mViewBlock.refresh();
    }
    
    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        blockList.clear();
        blockList.add(mViewBlock);
        return blockList;
    }

	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
	    mPOIDetailFragment.minusLoadingView();
	    POI poi = mPOI;
        if (poi == null) {
            return;
        }
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
        int mPOIFragmentId = mPOIDetailFragment.getId();
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIFragmentId, mPOIFragmentId, mPOIFragmentId, null)) {
            mPOIDetailFragment.isReLogin = true;
            return;
        }
        if (baseQuery instanceof DataQuery) {
            if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                return;
            }
            
            DataQuery dataQuery = (DataQuery) baseQuery;
            DishResponse dishResponse = (DishResponse) dataQuery.getResponse();
            DishList dishList = dishResponse.getList();
            if (dishList != null) {
                List<Dish> dishes = dishList.getDishList();
                if (dishes != null && dishes.size() > 0) {
                    mPOI.setDynamicDishList(dishes);
                    mPOI.setRecommendDishQuery(dataQuery);
                    refresh();
                    mPOIDetailFragment.mDishBtn.setVisibility(View.VISIBLE);
                }
            }
        }
	}

	@Override
	public void onCancelled(TKAsyncTask tkAsyncTask) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void loadData(int fromType) {
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DISH);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_RECOMMEND_DISH);
        queryStart(dataQuery);
        mPOIDetailFragment.addLoadingView();
    }

    @Override
    public void onClick(View v) {
        mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailShopkeeper);
        Intent intent = new Intent();
        intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.add_dish));
        intent.putExtra(BrowserActivity.URL, "http://www.tigerknows.com");
        mSphinx.showView(R.id.activity_browser, intent);
        
    }
}

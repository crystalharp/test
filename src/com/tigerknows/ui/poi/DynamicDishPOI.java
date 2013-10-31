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
import com.tigerknows.ui.common.BrowserActivity;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DynamicDishPOI extends POIDetailFragment.DynamicPOIView implements View.OnClickListener {
    
    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    DynamicPOIViewBlock mViewBlock;
    List<Dish> mAllList = new ArrayList<Dish>();
    View mDishTitleView;
    TextView mContentTxv;
    View mShopkeeperView;
    Animation mAnimation;
    BaseQuery mBaseQuery;
    
    BlockRefresher mRefresher = new BlockRefresher() {

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
            
            StringBuilder s = new StringBuilder();
            for(int i = 0, count = mAllList.size(); i < count; i++) {
                s.append(mAllList.get(i).getName());
                s.append("  ");
            }
            mContentTxv.setText(s.toString());
        }
    };
    
    public DynamicDishPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, mRefresher);

        LinearLayout layout = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_dish, null);
        mViewBlock.mOwnLayout = layout;
        
        mDishTitleView = layout.findViewById(R.id.dish_title_view);
        mDishTitleView.setOnClickListener(this);
        
        mContentTxv = (TextView) layout.findViewById(R.id.content_txv);
        mContentTxv.setOnClickListener(this);
        
        mShopkeeperView = layout.findViewById(R.id.shopkeeper_txv);
        mShopkeeperView.setOnClickListener(this);
        
        mPOIDetailFragment.mDishBtn.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mAnimation = new TranslateAnimation(mPOIDetailFragment.mDishBtn.getMeasuredWidth(), 0, 0, 0);
        mAnimation.setDuration(500);
        mAnimation.setFillAfter(true);
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
	    BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
	    mPOIDetailFragment.minusLoadingView();
	    POI poi = mPOI;
	    String uuid = baseQuery.getParameter(DataQuery.SERVER_PARAMETER_POI_ID);
        if (poi == null || uuid == null || uuid.equals(poi.getUUID()) == false || poi.equals(mPOIDetailFragment.mPOI) == false || baseQuery.isStop()) {
            return;
        }
        int mPOIFragmentId = mPOIDetailFragment.getId();
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIFragmentId, mPOIFragmentId, mPOIFragmentId, null)) {
            mPOIDetailFragment.isReLogin = true;
            return;
        }
        if (baseQuery instanceof DataQuery) {
            if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                return;
            }
            
            setDataQuery((DataQuery) baseQuery);
        }
	}
	
	void setDataQuery(DataQuery dataQuery) {
        DishResponse dishResponse = (DishResponse) dataQuery.getResponse();
        DishList dishList = dishResponse.getList();
        if (dishList != null) {
            List<Dish> dishes = dishList.getDishList();
            if (dishes != null && dishes.size() > 0) {
                mPOI.setDynamicDishList(dishes);
                mPOI.setRecommendDishQuery(dataQuery);
                refresh();
                mPOIDetailFragment.mDishBtn.setVisibility(View.VISIBLE);
                mPOIDetailFragment.mDishBtn.startAnimation(mAnimation);
            }
        }
	}

	@Override
	public void onCancelled(TKAsyncTask tkAsyncTask) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void loadData(int fromType) {
        if (mBaseQuery != null) {
            mBaseQuery.stop();
        }
        DataQuery dataQuery = mPOI.getRecommendDishQuery();
        if (dataQuery != null) {
            setDataQuery(dataQuery);
            return;
        }
        dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DISH);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_RECOMMEND_DISH);
        queryStart(dataQuery);
        mBaseQuery = dataQuery;
        mPOIDetailFragment.addLoadingView();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.shopkeeper_txv || id == R.id.dish_title_view) {
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailShopkeeper);
            Intent intent = new Intent();
            intent.putExtra(BrowserActivity.TITLE, mSphinx.getString(R.string.add_dish));
            intent.putExtra(BrowserActivity.URL, "http://www.tigerknows.com");
            mSphinx.showView(R.id.activity_browser, intent);
        } else if (id == R.id.content_txv) {
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.POIDetailDishList);
            DishActivity.setPOI(mPOI);
            Intent intent = new Intent();
            intent.putExtra(DishActivity.EXTRA_TAB, 1);
            mSphinx.showView(R.id.activity_poi_dish, intent);
        }
        
    }
}

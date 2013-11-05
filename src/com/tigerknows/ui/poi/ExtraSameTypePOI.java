package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

public class ExtraSameTypePOI extends DynamicPOIView {
    
    @Override
    public boolean isExist() {
        if (mPOI == null) {
            return false;
        }
        if (TextUtils.isEmpty(mPOI.getCategory()) == false && (mSphinx.uiStackContains(R.id.view_poi_result) || mSphinx.uiStackContains(R.id.view_poi_result2))) {
            return true;
        }
        return false;
    }

    DynamicPOIViewBlock mBlock;
    
    LinearLayout mRootView;
    LinearLayout mListView;
    LinearLayout mMoreView;
    TextView mTitleTxv;

    LinearListAdapter mAdapter;
    
    List<POI> mList = new ArrayList<POI>();
    
    private Drawable icAPOI;
    private String distanceA;
    private DataQuery dataQuery;
            
    void refreshBackground(LinearListAdapter lsv, int size) {
        for(int i = 0; i < size; i++) {
            View child = mListView.getChildAt(i);
            if (i == (size-1)) {
                child.setBackgroundResource(R.drawable.list_footer);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
            }
        }
    }

    
    public ExtraSameTypePOI(POIDetailFragment poiFragment,
            LayoutInflater inflater) {
        mPOIDetailFragment = poiFragment;
        mSphinx = poiFragment.mSphinx;
        mInflater = inflater;
        mRootView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_template, null);
        mTitleTxv = (TextView) mRootView.findViewById(R.id.title_txv);
        mTitleTxv.setText(R.string.dynamic_poi_title);
        mListView = (LinearLayout) mRootView.findViewById(R.id.list_view);
        mMoreView = (LinearLayout) mRootView.findViewById(R.id.more_view);
        mMoreView.setVisibility(View.GONE);
        mBlock = new DynamicPOIViewBlock(poiFragment.mBelowAddressLayout, mRootView) {

            @Override
            public void refresh() {
                if (mPOI == null || mList.size() == 0) {
                    clear();
                }
                mAdapter.refreshList(mList);
                refreshBackground(mAdapter, mList.size());
                show();
            }
        };
        mAdapter = new LinearListAdapter(mSphinx, mListView, R.layout.poi_dynamic_poi_list_item) {

            @Override
            public View getView(Object data, View child, final int pos) {
                // TODO Auto-generated method stub
                final POI target = (POI) data;
                
                TextView nameTxv = (TextView) child.findViewById(R.id.name_txv);
                TextView distanceTxv = (TextView) child.findViewById(R.id.distance_txv);
                TextView distanceFromTxv = (TextView) child.findViewById(R.id.distance_from_txv);
                TextView moneyTxv = (TextView) child.findViewById(R.id.money_txv);
                RatingBar startsRtb = (RatingBar) child.findViewById(R.id.stars_rtb);
                
                nameTxv.setText(target.getName());
                String distance = target.getToCenterDistance();
                if (!TextUtils.isEmpty(distance)) {
                    if (distance.startsWith(distanceA)) {
                        icAPOI.setBounds(0, 0, icAPOI.getIntrinsicWidth(), icAPOI.getIntrinsicHeight());
                        distanceFromTxv.setCompoundDrawables(null, null, icAPOI, null);
                        distanceFromTxv.setText(mSphinx.getString(R.string.distance));
                        distanceTxv.setText(distance.replace(distanceA, ""));
                    } else {
                        distanceFromTxv.setText("");
                        distanceFromTxv.setCompoundDrawables(null, null, null, null);
                        distanceTxv.setText(distance);
                    }
                } else {
                    distanceFromTxv.setText("");
                    distanceFromTxv.setCompoundDrawables(null, null, null, null);
                    distanceTxv.setText("");
                }
                
                if (BaseQuery.SUB_DATA_TYPE_HOTEL.equals(ExtraSameTypePOI.this.dataQuery.getParameter(DataQuery.SERVER_PARAMETER_SUB_DATA_TYPE))) {
                    String price = target.getPrice();
                    if (TextUtils.isEmpty(price)) {
                        moneyTxv.setText("");
                    } else {
                        moneyTxv.setText(price);
                    }
                } else {
                    long money = target.getPerCapity();
                    if (money > -1) {
                        moneyTxv.setText(mSphinx.getString(R.string.yuan, money));
                    } else {
                        moneyTxv.setText("");
                    }
                }
                
                float star = target.getGrade();
                startsRtb.setRating(star/2.0f);
                
                child.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        mSphinx.getPOIDetailFragment().setData(target, pos);
                    }
                });
                return null;
            }
            
        };
        Resources resources = mSphinx.getResources();
        icAPOI = resources.getDrawable(R.drawable.ic_location_nearby);
        distanceA = mSphinx.getString(R.string.distanceA);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        mPOIDetailFragment.minusLoadingView();
        
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), null)) {
            mPOIDetailFragment.isReLogin = true;
            return;
        }
        if (BaseActivity.checkResponseCode(dataQuery, mSphinx, null, false, this, false)) {
            return;
        }
        POIResponse poiResponse = (POIResponse) dataQuery.getResponse();
        if (poiResponse.getAPOIList() != null && 
                poiResponse.getAPOIList().getList() != null && 
                poiResponse.getAPOIList().getList().size() > 0) {
            mList.addAll(poiResponse.getAPOIList().getList());
        } else if (poiResponse.getBPOIList() != null && 
                    poiResponse.getBPOIList().getList() != null && 
                    poiResponse.getBPOIList().getList().size() > 0) {
            mList.addAll(poiResponse.getBPOIList().getList());
        }
        this.dataQuery= dataQuery;
        refresh();
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {

    }

    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        List<DynamicPOIViewBlock> lst = new LinkedList<DynamicPOIViewBlock>();
        lst.add(mBlock);
        return lst;
    }

    @Override
    public void refresh() {
        mBlock.refresh();
    }

    @Override
    public void loadData(int fromType) {

        mList.clear();
        
        if (mPOI == null) {
            return;
        }
        
        int id = R.id.view_poi_result;
        if (mSphinx.uiStackContains(id) == false) {
            id = R.id.view_poi_result2;
        }
        POIResultFragment poiResultFragment = (POIResultFragment) mSphinx.getFragment(id);
        
        DataQuery dataQuery = poiResultFragment.getDataQuery();
        if (dataQuery == null) {
            return;
        }
        
        DataQuery poiQuery = new DataQuery(dataQuery);

        POI requestPOI = dataQuery.getPOI();
        int cityId = dataQuery.getCityId();
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SIZE, "3");
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
        poiQuery.setup(cityId, mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), null, false, false, requestPOI);
        queryStart(poiQuery);
        
        mPOIDetailFragment.addLoadingView();

    }

}

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.android.location.Position;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.POIResponse;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

public class ExtraSameTypePOI extends DynamicPOIViewTemplate {
    
    @Override
    public boolean isExist() {
        if (mPOI == null || mPOI.isOnlyAPOI()) {
            return false;
        }
        if (TextUtils.isEmpty(mPOI.getCategory()) == false && mSphinx.uiStackContains(R.id.view_poi_result)) {
            return true;
        }
        return false;
    }

    LinearListAdapter mAdapter;
    
    List<POI> mList = new ArrayList<POI>();
    
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
        super(poiFragment, inflater);

        mTitleTxv.setText(R.string.dynamic_poi_title);
        mTitleRightTxv.setVisibility(View.GONE);
        mMoreView.setVisibility(View.GONE);
        mViewBlock = new DynamicPOIViewBlock(poiFragment.mBelowFeatureView, mRootView) {

            @Override
            public void refresh() {
                if (mPOI == null || mList.size() == 0) {
                    clear();
                    return;
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
                TextView moneyTxv = (TextView) child.findViewById(R.id.money_txv);
                RatingBar startsRtb = (RatingBar) child.findViewById(R.id.stars_rtb);
                
                nameTxv.setText(target.getName());
                
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
                        moneyTxv.setText(getString(R.string.yuan, money));
                    } else {
                        moneyTxv.setText("");
                    }
                }
                
                float star = target.getGrade();
                startsRtb.setRating(star/2.0f);
                
                child.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View v) {
                        mSphinx.getPOIDetailFragment().smoothScroolToTop();
                        mSphinx.getPOIDetailFragment().setData(target, pos);
                        mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.POIDetailSameCategory, target.getUUID(), target.getName());
                    }
                });
                return null;
            }
            
        };
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        mPOIDetailFragment.minusLoadingView();
        
        DataQuery dataQuery = (DataQuery) tkAsyncTask.getBaseQuery();
        if (BaseActivity.checkReLogin(dataQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), null)) {
            mPOIDetailFragment.isReLogin = true;
            return;
        }
        if (BaseActivity.hasAbnormalResponseCode(dataQuery, mSphinx, BaseActivity.SHOW_NOTHING, this, false)) {
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
        for(int i = mList.size() - 1; i >= 0; i--) {
            mList.get(i).setToCenterDistance(null);
        }
        this.dataQuery= dataQuery;
        refresh();
    }

    @Override
    public void loadData(int fromType) {

        mList.clear();
        
        if (mPOI == null) {
            return;
        }
        
        POIResultFragment poiResultFragment = (POIResultFragment) mSphinx.getPOIResultFragment();
        
        DataQuery dataQuery = poiResultFragment.getDataQuery();
        if (dataQuery == null) {
            return;
        }
        
        DataQuery poiQuery = new DataQuery(dataQuery);

        poiQuery.removeParameter(DataQuery.SERVER_PARAMETER_INFO);
        poiQuery.removeParameter(DataQuery.SERVER_PARAMETER_FILTER);
        poiQuery.removeParameter(DataQuery.SERVER_PARAMETER_FILTER_STRING);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_SIZE, "3");
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_KEYWORD, mPOI.getCategory().split(" ")[0]);
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
        poiQuery.addParameter(DataQuery.SERVER_PARAMETER_REQUSET_SOURCE_TYPE, "poidetail");
        Position position = mPOI.getPosition();
        if (position != null) {
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LONGITUDE, String.valueOf(position.getLon()));
            poiQuery.addParameter(DataQuery.SERVER_PARAMETER_LATITUDE, String.valueOf(position.getLat()));
        }
        poiQuery.setup(mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), null, false, false, mPOI);
        queryStart(poiQuery);
        
        mPOIDetailFragment.addLoadingView();

    }

}

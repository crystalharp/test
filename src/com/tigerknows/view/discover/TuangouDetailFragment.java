/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view.discover;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.Tuangou;
import com.tigerknows.view.SpringbackListView.IPagerList;
import com.tigerknows.view.discover.CycleViewPager.CyclePagerAdapter;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class TuangouDetailFragment extends BaseDetailFragment 
                                   implements View.OnClickListener, CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public TuangouDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private List<Tuangou> mDataList = null;
    
    public boolean isReLogin() {
        boolean isRelogin = this.isReLogin;
        this.isReLogin = false;
        if (isRelogin) {
            if (mBaseQuerying != null) {
                // 这里判断为创建订单操作时且没有登录成功时，将mBaseQuerying置为null，避免在没有用户登录的情况再次发出创建订单请求
                BaseQuery baseQuery = mBaseQuerying.get(0);
                if (BaseQuery.API_TYPE_DATA_OPERATION.equals(baseQuery.getAPIType())) {
                    Hashtable<String, String> criteria = baseQuery.getCriteria();
                    if (criteria != null && criteria.containsKey(BaseQuery.SERVER_PARAMETER_DATA_TYPE)) {
                        String dataType = criteria.get(BaseQuery.SERVER_PARAMETER_DATA_TYPE);
                        if (DataOperation.DATA_TYPE_DINGDAN.equals(dataType)) {
                            if (Globals.g_User == null) {
                                mBaseQuerying = null;
                            }
                        }
                    }
                }
                    
                if (mBaseQuerying != null) {
                    for(int i = 0, size = mBaseQuerying.size(); i < size; i++) {
                        mBaseQuerying.get(i).setResponse(null);
                    }
                    mSphinx.queryStart(mBaseQuerying);
                }
            }
        }
        return isRelogin;
    }
    
    /**
     * Flag, whether the user is requesting a buy action
     * ie. the user pressed the buy button
     */
    boolean isRequsetBuy = false;
    
    /**
     * Check if the buy button is clicked.
     * And clear the {@link isRequestBuy} flag
     * @return
     */
    private boolean isRequsetBuy() {
        boolean isRequsetBuy = this.isRequsetBuy;
        this.isRequsetBuy = false;
        return isRequsetBuy;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TuangouDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

    	// Create the views used in the ViewPager
        List<View> viewList = new ArrayList<View>();
        TuangouDetailView view;
        view = new TuangouDetailView(mSphinx, this);
        viewList.add(view);
        view = new TuangouDetailView(mSphinx, this);
        viewList.add(view);
        view = new TuangouDetailView(mSphinx, this);
        viewList.add(view);
        mCyclePagerAdapter = new CyclePagerAdapter(viewList);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.tuangou_detail);

        // On resume, check if user clicks the buy button ()
        // 	(This happends when user is not logged in and pressed the buy button, logged in and come back)
        // and the current user is not null
        if (isRequsetBuy() && Globals.g_User != null) {
            ((TuangouDetailView) mCyclePagerAdapter.viewList.get(mViewPager.getCurrentItem()%mCyclePagerAdapter.viewList.size())).buy();
            return;
        }
    }
    
    /**
     * Interface for info-window click event
     * For info window click to go back to Tuangou detail fragment
     * @param data
     */
    public void setData(Tuangou data) {
        int position = 0;
        for(int i = mDataList.size()-1; i >= 0; i--) {
            if (data == mDataList.get(i)) {
                position = i;
                break;
            }
        }
        setData(null, position, null);
    }
    
    /**
     * Set data list for this fragment
     * @param dataList
     * @param position
     * @param iPagerList
     */
    public void setData(List<Tuangou> dataList, int position, IPagerList iPagerList) {
        if (dataList == null) {
            dataList = mDataList;
        }
        
        this.mDataList = dataList;
        setData(dataList.size(), position, iPagerList);
        refreshViews(position);
        setViewsVisibility(View.VISIBLE);
    }
    
    /**
     * View fendian poi on map
     */
    public void viewMap() {
    	//Get Fendian poi
        Tuangou data = this.mDataList.get(mViewPager.getCurrentItem());
        Fendian fendian = data.getFendian();
        if (fendian == null) {
            return;
        }
        List<POI> list = new ArrayList<POI>();
        POI poi = fendian.getPOI(POI.SOURCE_TYPE_TUANGOU);
        list.add(poi);

        //Show it on map
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.shanghu_ditu), ActionLog.ResultMapTuangouDetail);
        super.viewMap();
    }
    
    /**
     * Refresh the content of the views in the current ViewPager if exists
     * The current view, the view to the left of it, and the view to the right of it.
     */
    public void refreshViews(int position) {
        super.refreshViews(position);
        TuangouDetailView view;

        view = (TuangouDetailView) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
        view.setData(mDataList.get(position));
        view.onResume();
        
        if (position - 1 >= 0) {
            view = (TuangouDetailView) mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position-1));
            view.onResume();
        }
        if (position + 1 < mDataList.size()) {
            view = (TuangouDetailView) mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position+1));
            view.onResume();
        }
    }
}

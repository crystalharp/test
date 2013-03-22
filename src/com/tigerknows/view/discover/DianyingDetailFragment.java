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
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.BaseActivity;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery.YingxunResponse;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.YingxunQueryResponse;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.POIDetailFragment;
import com.tigerknows.view.SpringbackListView.IPagerList;
import com.tigerknows.view.discover.CycleViewPager.CyclePagerAdapter;

/**
 * @author Peng Wenyue
 * </ul>
 * 
 */
public class DianyingDetailFragment extends BaseDetailFragment
                                    implements CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public DianyingDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    List<Dianying> mDataList;
    
    List<BaseQuery> mBaseQuerying;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.DianyingXiangqing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        List<View> viewList = new ArrayList<View>();
        DianyingDetailView view;
        view = new DianyingDetailView(mSphinx, this);
        viewList.add(view);
        view = new DianyingDetailView(mSphinx, this);
        viewList.add(view);
        view = new DianyingDetailView(mSphinx, this);
        viewList.add(view);
        mCyclePagerAdapter = new CyclePagerAdapter(viewList);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.dianying_detail);
    }
    
    public void setData(Dianying data) {
        int position = 0;
        for(int i = mDataList.size()-1; i >= 0; i--) {
            if (data == mDataList.get(i)) {
                position = i;
                break;
            }
        }
        setData(null, position, null);
    }
    
    public void setData(List<Dianying> dataList, int position, IPagerList iPagerList) {
        if (dataList == null) {
            dataList = mDataList;
        }
        mDataList = dataList;
        setData(dataList.size(), position, iPagerList);
        refreshViews(position);
        
    	setViewsVisibility(View.VISIBLE);
    }

    public void setPulledDynamicPOI(PulledDynamicPOI dynamicPOI){
    	
    	if(dynamicPOI==null || dynamicPOI.getMasterUID()==null
    			|| dynamicPOI.getMasterType()==0){
    	}

    	setViewsVisibility(View.INVISIBLE);
    	
    	// Set up the master poi query
    	DataOperation dataOperation = new DataOperation(mSphinx);
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_DIANYING);
    	criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
    	criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getMasterUID());
        criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                Dianying.NEED_FILELD_ONLY_DIANYING + Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
        criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                Util.byteToHexString(Dianying.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
    	criteria.put(BaseQuery.RESPONSE_CODE_ERROR_MSG_PREFIX + 410, ""+R.string.response_code_410_pulled);
        dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), DianyingDetailFragment.this.getId(), DianyingDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
        List<BaseQuery> list = new ArrayList<BaseQuery>();
        list.add(dataOperation);

    	// Set up the slave poi query
        dataOperation = new DataOperation(mSphinx);
        criteria = new Hashtable<String, String>();
        criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YINGXUN);
        criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getSlaveUID());
        criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Yingxun.NEED_FILELD);
        dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), DianyingDetailFragment.this.getId(), DianyingDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
        list.add(dataOperation);
        
        //Start the query
        mTkAsyncTasking = mSphinx.queryStart(list);
        //Remember the current
        mBaseQuerying = list;
        
    }
    
    public void viewMap() {
        Dianying data = mDataList.get(mViewPager.getCurrentItem());
        List<POI> list = new ArrayList<POI>();
        POI poi = data.getPOI();
        list.add(poi);
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.dianying_ditu), ActionLog.MapDianyingXiangqing);
        super.viewMap();
    }
    
    public void refreshViews(int position) {
        super.refreshViews(position);
        DianyingDetailView view;
        
        view = (DianyingDetailView) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
        view.setData(mDataList.get(position));
        view.onResume();
        
        if (position - 1 >= 0) {
            view = (DianyingDetailView) mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position-1));
            view.onResume();
        }
        if (position + 1 < mDataList.size()) {
            view = (DianyingDetailView) mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position+1));
            view.onResume();
        }
    }
    
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		super.onPostExecute(tkAsyncTask);
		
		Dianying dianying = null;
		List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
		
		for (BaseQuery baseQuery : baseQueryList) {

			// Do the check work
	        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
	            isReLogin = true;
	            return;
	        }
	        if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, BaseActivity.SHOW_ERROR_MSG_TOAST, this, true)) {
	            return;
	        }
	        
	        String dataType = baseQuery.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_TYPE);
	        if(BaseQuery.DATA_TYPE_DIANYING.equals(dataType)){
	        	// Dianying query response
		        DianyingQueryResponse response = (DianyingQueryResponse) baseQuery.getResponse();
		        dianying = response.getDianying();
	        }else{
	        	// Yinxun query repsonse
	        	YingxunQueryResponse yingxunQueryResponse = (YingxunQueryResponse) baseQuery.getResponse();
	        	Yingxun yingxun = yingxunQueryResponse.getYingxun();
	        	dianying.setYingxun(yingxun);
		        List<Dianying> list = new ArrayList<Dianying>();
		        list.add(dianying);
		        mSphinx.getDianyingDetailFragment().setData(list, 0, null);
	        }
	        
		}
        
	}
	

}

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
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.DataQuery.YanchuResponse;
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
public class YanchuDetailFragment extends BaseDetailFragment
                                  implements CycleViewPager.CycleOnPageChangeListener.IRefreshViews {
    
    public YanchuDetailFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    List<Yanchu> mDataList;
    private List<BaseQuery> mBaseQuerying;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.YanchuXiangqing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        List<View> viewList = new ArrayList<View>();
        YanchuDetailView view;
        view = new YanchuDetailView(mSphinx, this);
        viewList.add(view);
        view = new YanchuDetailView(mSphinx, this);
        viewList.add(view);
        view = new YanchuDetailView(mSphinx, this);
        viewList.add(view);
        mCyclePagerAdapter = new CyclePagerAdapter(viewList);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTitleBtn.setText(R.string.yanchu_detail);
    }
    
    public void setData(Yanchu data) {
        int position = 0;
        for(int i = mDataList.size()-1; i >= 0; i--) {
            if (data == mDataList.get(i)) {
                position = i;
                break;
            }
        }
        setData(null, position, null);
    }
    
    public void setData(List<Yanchu> dataList, int position, IPagerList iPagerList) {
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
    	
    	DataOperation dataOperation = new DataOperation(mSphinx);
    	Hashtable<String, String> criteria = new Hashtable<String, String>();
    	criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YANCHU);
    	criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
    	criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getMasterUID());
    	criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                Yanchu.NEED_FILELD + Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
    	criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
	           Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
	           Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
    	criteria.put(BaseQuery.RESPONSE_CODE_ERROR_MSG_PREFIX + 410, ""+R.string.response_code_410_pulled);
    	dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), YanchuDetailFragment.this.getId(), YanchuDetailFragment.this.getId(), mSphinx.getString(R.string.doing_and_wait));
    	List<BaseQuery> list = new ArrayList<BaseQuery>();
    	list.add(dataOperation);
    	mTkAsyncTasking = mSphinx.queryStart(list);
    	mBaseQuerying = list;

    }
    
    public void viewMap() {
        Yanchu data = mDataList.get(mViewPager.getCurrentItem());
        List<POI> list = new ArrayList<POI>();
        POI poi = data.getPOI();
        poi.setName(data.getPlaceName());
        poi.setAddress(data.getAddress());
        list.add(poi);
        mSphinx.showPOI(list, 0);
        mSphinx.getResultMapFragment().setData(mContext.getString(R.string.yanchu_didian_ditu), ActionLog.MapYanchuXiangqing);
        super.viewMap();
    }
    
    public void refreshViews(int position) {
        super.refreshViews(position);
        YanchuDetailView view;
        
        view = (YanchuDetailView) mCyclePagerAdapter.viewList.get((position) % mCyclePagerAdapter.viewList.size());
        view.setData(mDataList.get(position));
        view.onResume();
        
        if (position - 1 >= 0) {
            view = (YanchuDetailView) mCyclePagerAdapter.viewList.get((position-1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position-1));
            view.onResume();
        }
        if (position + 1 < mDataList.size()) {
            view = (YanchuDetailView) mCyclePagerAdapter.viewList.get((position+1) % mCyclePagerAdapter.viewList.size());
            view.setData(mDataList.get(position+1));
            view.onResume();
        }
    }

	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		
		super.onPostExecute(tkAsyncTask);
		
		List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
		BaseQuery baseQuery = baseQueryList.get(0);
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        }
        if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, BaseActivity.SHOW_ERROR_MSG_TOAST, this, true)) {
            return;
        }
        
        YanchuQueryResponse response = (YanchuQueryResponse) baseQuery.getResponse();
        Yanchu yanchu = response.getYanchu();
        List<Yanchu> list = new ArrayList<Yanchu>();
        list.add(yanchu);
        mSphinx.getYanchuDetailFragment().setData(list, 0, null);
		
	}
    
    
}

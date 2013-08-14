package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.DataQuery.DianyingResponse;
import com.tigerknows.model.DataQuery.DianyingResponse.DianyingList;
import com.tigerknows.model.Dianying;
import com.tigerknows.model.POI;
import com.tigerknows.model.TKDrawable.LoadImageRunnable;
import com.tigerknows.model.Yingxun;
import com.tigerknows.model.DataOperation.DianyingQueryResponse;
import com.tigerknows.model.DataOperation.YingxunQueryResponse;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.BlockRefresher;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

public class DynamicMoviePOI extends POIDetailFragment.DynamicPOIView{
    
	static final int SHOW_DYNAMIC_YINGXUN_MAX = 3;
    
    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListView lsv;
    DynamicPOIViewBlock mViewBlock;
    List<Dianying> mShowingList = new ArrayList<Dianying>();
    List<Dianying> mAllList = new ArrayList<Dianying>();
    
    LinearLayout mDynamicDianyingView;
    LinearLayout mDynamicDianyingListView;
    LinearLayout mDynamicDianyingMoreView;
    
    ItemInitializer initer = new ItemInitializer(){

        @Override
        public void initItem(Object data, View v) {
            
            final Dianying movie = (Dianying)data;
            final ImageView pictureImv = (ImageView) v.findViewById(R.id.picture_imv);
            TextView nameTxv = (TextView) v.findViewById(R.id.name_txv);
            RatingBar starsRtb = (RatingBar) v.findViewById(R.id.stars_rtb);
            TextView distanceTxv = (TextView) v.findViewById(R.id.distance_txv);
            TextView addressTxv = (TextView) v.findViewById(R.id.category_txv);
            TextView dateTxv = (TextView) v.findViewById(R.id.date_txv);
            
            v.findViewById(R.id.body_view).setBackgroundDrawable(null);
            v.setTag(data);
            v.setOnClickListener(mDynamicMovieListener);

            TKDrawable tkDrawable = movie.getPicture();
            LoadImageRunnable loadImageRunnable = new LoadImageRunnable(mSphinx, tkDrawable, pictureImv, -1, mPOIDetailFragment.toString());
            Drawable drawable = tkDrawable.loadDrawable(mSphinx, loadImageRunnable, mPOIDetailFragment.toString());
            if(drawable != null) {
                //To prevent the problem of size change of the same pic 
                //After it is used at a different place with smaller size
                Rect bounds = drawable.getBounds();
                if(bounds != null && bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight() ){
                    pictureImv.setBackgroundDrawable(null);
                }
                pictureImv.setBackgroundDrawable(drawable);
            } else {
                pictureImv.setBackgroundDrawable(null);
            }
            
            nameTxv.setText(movie.getName());
            starsRtb.setProgress((int) movie.getRank());
            distanceTxv.setVisibility(View.GONE);
            
            addressTxv.setText(movie.getTag());
            if (TextUtils.isEmpty(movie.getLength())) {
                dateTxv.setText(R.string.dianying_no_length_now);
            } else {
                dateTxv.setText(String.valueOf(movie.getLength()));
            }
        }
        
    };
    
    BlockRefresher mMovieRefresher = new BlockRefresher() {

        @Override
        public void refresh() {
            if (mPOI == null) {
                return;
            }
            
            mAllList.clear();
            mShowingList.clear();
            
            List<Dianying> list = mPOI.getDynamicDianyingList();        
            int size = (list != null ? list.size() : 0);
            if (size == 0) {
                mViewBlock.clear();
                return;
            }
            mAllList.addAll(list);
            
            if (size > SHOW_DYNAMIC_YINGXUN_MAX) {
                for(int i = 0; i < SHOW_DYNAMIC_YINGXUN_MAX; i++) {
                    mShowingList.add(mAllList.get(i));
                }
                mDynamicDianyingMoreView.setVisibility(View.VISIBLE);
            } else {
                mShowingList.addAll(mAllList);
                mDynamicDianyingMoreView.setVisibility(View.GONE);
            }
            
            lsv.refreshList(mShowingList);
            refreshBackground(lsv, mShowingList.size());
        }
        
    };
    
    public DynamicMoviePOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, mMovieRefresher);
        mDynamicDianyingView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_movie_poi, null);
        mDynamicDianyingListView = (LinearLayout) mDynamicDianyingView.findViewById(R.id.dynamic_dianying_list_view);
        mDynamicDianyingMoreView = (LinearLayout) mDynamicDianyingView.findViewById(R.id.dynamic_dianying_more_view);
        mViewBlock.mOwnLayout = mDynamicDianyingView;
        lsv = new LinearListView(mSphinx, mDynamicDianyingListView, initer, R.layout.poi_dynamic_movie_list_item);
        mDynamicDianyingMoreView.setOnClickListener(mMoreClickListener);
    }

//    @Override
//    public boolean checkExistence(POI poi) {
//        List<DynamicPOI> list = poi.getDynamicPOIList();
//        if (list != null) {
//            for(int i = 0, size = list.size(); i < size; i++) {
//                DynamicPOI dynamic = list.get(i);
//                if (BaseQuery.DATA_TYPE_DIANYING.equals(dynamic.getType())) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
    
    public DataQuery buildQuery(POI poi) {
        Hashtable<String, String> criteria = new Hashtable<String, String>();
        criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANYING);
        criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
        criteria.put(DataQuery.SERVER_PARAMETER_INDEX, "0");
        criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                Dianying.NEED_FIELD_POI_DETAIL);
        criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
               Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]");
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), null, false, false, poi);
        
        return dataQuery;
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
    
    private OnClickListener mDynamicMovieListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            Object object = view.getTag();
            if (object == null) {
                return;
            }
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailDianying);
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            if (object instanceof Dianying) {
                Dianying dynamic = (Dianying) object;
                List<BaseQuery> list = new ArrayList<BaseQuery>();

                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_DIANYING);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getUid());
                
                dataOperation = new DataOperation(mSphinx);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                        Dianying.NEED_FIELD_ONLY_DIANYING
                        + Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Dianying.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                dataOperation = new DataOperation(mSphinx);
                criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YINGXUN);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getYingxun().getUid());
                
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD, Yingxun.NEED_FIELD);
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
//                mTkAsyncTasking = mSphinx.queryStart(list);
//                mBaseQuerying = list;
                queryStart(list);
            }
            
        }
    };
    
    void refreshBackground(LinearListView lsv, int size) {
        for(int i = 0; i < size; i++) {
            View child = mDynamicDianyingListView.getChildAt(i);
            if (i == (size-1) && mDynamicDianyingMoreView.getVisibility() == View.GONE) {
                child.setBackgroundResource(R.drawable.list_footer);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
            }
        }
    }

    OnClickListener mMoreClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailDianyingMore);
            lsv.refreshList(mAllList);
            mDynamicDianyingMoreView.setVisibility(View.GONE);
            refreshBackground(lsv, mAllList.size());
            
        }
    };

	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
	    mPOIDetailFragment.minusLoadingView();
	    POI poi = mPOI;
        if (poi == null) {
            return;
        }
        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        int mPOIFragmentId = mPOIDetailFragment.getId();
        Dianying dianying = null;
        for(BaseQuery baseQuery : baseQueryList) {
            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIFragmentId, mPOIFragmentId, mPOIFragmentId, null)) {
                mPOIDetailFragment.isReLogin = true;
                return;
            }
            Hashtable<String, String> criteria = baseQuery.getCriteria();
            String dataType = criteria.get(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            Response response = baseQuery.getResponse();
            if (baseQuery instanceof DataQuery) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, false, this, false)) {
                    return;
                }
                DianyingResponse dianyingResponse = (DianyingResponse) response;
                DianyingList dianyingList = dianyingResponse.getList();
                if (dianyingList != null) {
                    mPOI.setDynamicDianyingList(dianyingList.getList());
                    refresh();
                }
            } else if (baseQuery instanceof DataOperation) {
                if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                    return;
                }
                if (BaseQuery.DATA_TYPE_DIANYING.equals(dataType)) {
                    dianying = ((DianyingQueryResponse) response).getDianying();
                } else if (BaseQuery.DATA_TYPE_YINGXUN.equals(dataType)) {
                    if (dianying != null) {
                        dianying.setYingxun(((YingxunQueryResponse) response).getYingxun());
                        List<Dianying> list = new ArrayList<Dianying>();
                        list.add(dianying);
                        dianying.getYingxun().setChangciOption(Yingxun.Changci.OPTION_DAY_TODAY);
                        mSphinx.showView(R.id.view_discover_dianying_detail);
                        mSphinx.getDianyingDetailFragment().setData(list, 0, null);
                    }
                }
            }
        }
	}

	@Override
	public void onCancelled(TKAsyncTask tkAsyncTask) {
		// TODO Auto-generated method stub
		
	}
}

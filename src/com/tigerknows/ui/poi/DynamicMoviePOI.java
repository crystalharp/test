package com.tigerknows.ui.poi;

import java.util.ArrayList;
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
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class DynamicMoviePOI extends DynamicPOIViewTemplate {
    
	static final int SHOW_DYNAMIC_YINGXUN_MAX = 3;
    
    LinearListAdapter dianyingListAdapter;
    List<Dianying> mShowingList = new ArrayList<Dianying>();
    List<Dianying> mAllList = new ArrayList<Dianying>();
        
    public DynamicMoviePOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        super(poiFragment, inflater);
        
        mTitleTxv.setText(R.string.play_dianying);
        mTitleRightTxv.setVisibility(View.GONE);
        mMoreTxv.setText(R.string.more_dianying);
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, mRootView) {

            @Override
            public void refresh() {
                if (mPOI == null) {
                    clear();
                    return;
                }
                
                mAllList.clear();
                mShowingList.clear();
                
                List<Dianying> list = mPOI.getDynamicDianyingList();        
                int size = (list != null ? list.size() : 0);
                if (size == 0) {
                    clear();
                    return;
                }
                mAllList.addAll(list);
                
                if (size > SHOW_DYNAMIC_YINGXUN_MAX) {
                    for(int i = 0; i < SHOW_DYNAMIC_YINGXUN_MAX; i++) {
                        mShowingList.add(mAllList.get(i));
                    }
                    mMoreView.setVisibility(View.VISIBLE);
                } else {
                    mShowingList.addAll(mAllList);
                    mMoreView.setVisibility(View.GONE);
                }
                
                dianyingListAdapter.refreshList(mShowingList);
                refreshBackground(dianyingListAdapter, mShowingList.size());
                show();
            }
        };
        dianyingListAdapter = new LinearListAdapter(mSphinx, mListView, R.layout.poi_dynamic_movie_list_item) {

            @Override
            public View getView(Object data, View child, int pos) {

                final Dianying movie = (Dianying)data;
                final ImageView pictureImv = (ImageView) child.findViewById(R.id.picture_imv);
                TextView nameTxv = (TextView) child.findViewById(R.id.name_txv);
                RatingBar starsRtb = (RatingBar) child.findViewById(R.id.stars_rtb);
                TextView distanceTxv = (TextView) child.findViewById(R.id.distance_txv);
                TextView addressTxv = (TextView) child.findViewById(R.id.category_txv);
                TextView dateTxv = (TextView) child.findViewById(R.id.date_txv);

                child.findViewById(R.id.body_view).setBackgroundDrawable(null);
                child.setTag(data);
                child.setOnClickListener(mDynamicMovieListener);

                TKDrawable tkDrawable = movie.getPicture();
                LoadImageRunnable loadImageRunnable = new LoadImageRunnable(mSphinx, tkDrawable, pictureImv, R.drawable.bg_picture, mPOIDetailFragment.toString());
                Drawable drawable = tkDrawable.loadDrawable(mSphinx, loadImageRunnable, mPOIDetailFragment.toString());
                if(drawable != null) {
                    //To prevent the problem of size change of the same pic 
                    //After it is used at a different place with smaller size
                    Rect bounds = drawable.getBounds();
                    if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
                        pictureImv.setBackgroundDrawable(null);
                    }
                    pictureImv.setBackgroundDrawable(drawable);
                } else {
                    pictureImv.setBackgroundResource(R.drawable.bg_picture);
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

                return null;
            }

        };
        mMoreView.setOnClickListener(mMoreClickListener);
    }
    
    public DataQuery buildQuery(POI poi) {
        DataQuery dataQuery = new DataQuery(mSphinx);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANYING);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, poi.getUUID());
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_INDEX, "0");
        dataQuery.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                Dianying.NEED_FIELD_POI_DETAIL);
        dataQuery.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
               Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]");
        dataQuery.setup(mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), null, false, false, poi);
        
        return dataQuery;
    }
    
    private OnClickListener mDynamicMovieListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            Object object = view.getTag();
            if (object == null) {
                return;
            }
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailDianying);
            DataOperation dataOperation;
            if (object instanceof Dianying) {
                Dianying dynamic = (Dianying) object;
                List<BaseQuery> list = new ArrayList<BaseQuery>();

                dataOperation = new DataOperation(mSphinx);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_DIANYING);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getUid());
                
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                        Dianying.NEED_FIELD_ONLY_DIANYING
                        + Util.byteToHexString(Dianying.FIELD_DESCRIPTION));
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Dianying.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Dianying.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                dataOperation = new DataOperation(mSphinx);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_YINGXUN);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, dynamic.getYingxun().getUid());
                
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, Yingxun.NEED_FIELD);
                dataOperation.setup(mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                queryStart(list);
            }
            
        }
    };
    
    void refreshBackground(LinearListAdapter lsv, int size) {
        for(int i = 0; i < size; i++) {
            View child = mListView.getChildAt(i);
            if (i == (size-1) && mMoreView.getVisibility() == View.GONE) {
                child.setBackgroundResource(R.drawable.list_footer);
            } else {
                child.setBackgroundResource(R.drawable.list_middle);
            }
        }
    }

    OnClickListener mMoreClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailDianyingMore);
            dianyingListAdapter.refreshList(mAllList);
            mMoreView.setVisibility(View.GONE);
            refreshBackground(dianyingListAdapter, mAllList.size());
            
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
            String dataType = baseQuery.getParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            Response response = baseQuery.getResponse();
            if (baseQuery instanceof DataQuery) {
                if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_ERROR_MSG_NO, this, false)) {
                    return;
                }
                DianyingResponse dianyingResponse = (DianyingResponse) response;
                DianyingList dianyingList = (DianyingList) dianyingResponse.getDiscoverResult();
                if (dianyingList != null) {
                    mPOI.setDynamicDianyingList(dianyingList.getList());
                    refresh();
                }
            } else if (baseQuery instanceof DataOperation) {
                if (BaseActivity.hasAbnormalResponseCode(baseQuery, mSphinx, BaseActivity.SHOW_ERROR_MSG_DIALOG, this, false)) {
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
    public void loadData(int fromType) {
        queryStart(buildQuery(mPOI));
        mPOIDetailFragment.addLoadingView();
    }
}

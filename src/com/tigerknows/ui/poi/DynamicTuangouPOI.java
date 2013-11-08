package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DynamicTuangouPOI extends POIDetailFragment.DynamicPOIView{
    
	static final int SHOW_DYNAMIC_COUNT = 3;
    
    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    LinearListAdapter mListAdapter;
    DynamicPOIViewBlock mViewBlock;
    List<DynamicPOI> mShowingList = new ArrayList<DynamicPOI>();
    List<DynamicPOI> mAllList = new ArrayList<DynamicPOI>();
    
    LinearLayout mRootView;
    LinearLayout mListView;
    LinearLayout mMoreView;
    TextView mTitleTxv;
    TextView mMoreTxv;
        
    public DynamicTuangouPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        mRootView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_template, null);
        mTitleTxv = (TextView) mRootView.findViewById(R.id.title_txv);
        mTitleTxv.setText(R.string.dynamic_tuangou_title);
        mListView = (LinearLayout) mRootView.findViewById(R.id.list_view);
        mMoreView = (LinearLayout) mRootView.findViewById(R.id.more_view);
        mMoreTxv = (TextView) mMoreView.findViewById(R.id.more_txv);
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, mRootView) {

            @Override
            public void refresh() {
                if (mPOI == null) {
                    clear();
                    return;
                }
                
                mAllList.clear();
                mShowingList.clear();
                
                List<DynamicPOI> dynamicPOIList = mPOI.getDynamicPOIList();
                List<DynamicPOI> list = new ArrayList<DynamicPOI>();
                for(int i = 0; i < dynamicPOIList.size(); i++) {
                    DynamicPOI dynamicPOI = dynamicPOIList.get(i);
                    String dataType = dynamicPOI.getType();
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                        list.add(dynamicPOI);
                    }
                }
                
                int size = (list != null ? list.size() : 0);
                if (size == 0) {
                    clear();
                    return;
                }
                mAllList.addAll(list);
                
                mMoreTxv.setText(mSphinx.getString(R.string.dynamic_tuangou_more, mAllList.size()));
                
                if (size > SHOW_DYNAMIC_COUNT) {
                    for(int i = 0; i < SHOW_DYNAMIC_COUNT; i++) {
                        mShowingList.add(mAllList.get(i));
                    }
                    mMoreView.setVisibility(View.VISIBLE);
                } else {
                    mShowingList.addAll(mAllList);
                    mMoreView.setVisibility(View.GONE);
                }
                
                mListAdapter.refreshList(mShowingList);
                refreshBackground(mListAdapter, mShowingList.size());
                show();
            }
        };
        mListAdapter = new LinearListAdapter(mSphinx, mListView, R.layout.poi_dynamic_tuangou_list_item) {

            @Override
            public View getView(Object data, View child, int pos) {
                DynamicPOI dynamicPOI = (DynamicPOI) data;
                try {
                    Tuangou target = new Tuangou(dynamicPOI.getRemark());
                    TextView nameTxv = (TextView) child.findViewById(R.id.name_txv);
                    TextView priceTxv = (TextView) child.findViewById(R.id.price_txv);
                    TextView orgPriceTxv = (TextView) child.findViewById(R.id.org_price_txv);
                    TextView sourceTxv = (TextView) child.findViewById(R.id.source_txv);
                    
                    child.setTag(dynamicPOI);
                    child.setOnClickListener(mOnItemClickedListener);
                    
                    nameTxv.setText(target.getName());
                    priceTxv.setText(target.getPrice());
                    orgPriceTxv.setText(target.getOrgPrice());
                    sourceTxv.setText(String.valueOf(target.getSource()));
                } catch (APIException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return null;
            }

        };
        mMoreView.setOnClickListener(mMoreClickListener);
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
    
    private OnClickListener mOnItemClickedListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            Object object = view.getTag();
            if (object == null) {
                return;
            }
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailDianying);

            DynamicPOI dynamicPOI = (DynamicPOI)view.getTag();
            final String dataType = dynamicPOI.getType();
            DataOperation dataOperation = new DataOperation(mSphinx);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            String DPOIMasterUid = dynamicPOI.getMasterUid();
            if (DPOIMasterUid != null && !TextUtils.isEmpty(DPOIMasterUid)) {
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, DPOIMasterUid);
            }
            
            List<BaseQuery> list = new ArrayList<BaseQuery>();
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailTuangou);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                         Tuangou.NEED_FIELD
                            + Util.byteToHexString(Tuangou.FIELD_NOTICED)
                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT)
                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
                    Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[0]" + ";" +
                    Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[0]" + ";" +
                    Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN)+"_[0]");
            dataOperation.setup(Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
            list.add(dataOperation);
            dataOperation = new DataOperation(mSphinx);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_FENDIAN);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getSlaveUid());
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, Fendian.NEED_FIELD);
            dataOperation.setup(Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
            list.add(dataOperation);
            queryStart(list);
        }
    };
    
    void refreshBackground(LinearListAdapter lsv, int size) {
        for(int i = 0; i < size; i++) {
            View child = mListView.getChildAt(i);
            if (i == (size-1) && mMoreView.getVisibility() == View.GONE) {
                child.setBackgroundResource(R.drawable.list_footer_separation_line);
            } else {
                child.setBackgroundResource(R.drawable.list_middle_separation_line);
            }
        }
    }

    OnClickListener mMoreClickListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag+ActionLog.POIDetailDianyingMore);
            mListAdapter.refreshList(mAllList);
            mMoreView.setVisibility(View.GONE);
            refreshBackground(mListAdapter, mAllList.size());
        }
    };

	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
	    POI poi = mPOI;
        if (poi == null) {
            return;
        }
        Tuangou tuangou = null;
        List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
        int mPOIFragmentId = mPOIDetailFragment.getId();
        for(BaseQuery baseQuery : baseQueryList) {
            if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), mPOIFragmentId, mPOIFragmentId, mPOIFragmentId, null)) {
                mPOIDetailFragment.isReLogin = true;
                return;
            }
            if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                return;
            }
            Response response = baseQuery.getResponse();
            String dataType = baseQuery.getParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                tuangou = ((TuangouQueryResponse) response).getTuangou();

                // 查询团购分店的结果
            } else if (BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)) {
                if (tuangou != null) {
                    tuangou.setFendian(((FendianQueryResponse) response).getFendian());
                    List<Tuangou> list = new ArrayList<Tuangou>();
                    list.add(tuangou);
                    mSphinx.showView(R.id.view_discover_tuangou_detail);
                    mSphinx.getTuangouDetailFragment().setData(list, 0, null);
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
        refresh();
    }
}

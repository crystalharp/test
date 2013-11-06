package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Coupon;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataOperation.CouponQueryResponse;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.Response;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListAdapter;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DynamicNormalPOI extends POIDetailFragment.DynamicPOIView{
    
    static DynamicNormalPOI instance = null;
    DynamicPOIViewBlock mViewBlock;
    static Tuangou tuangou = null;
    NormalDPOIClickListener clickListener = new NormalDPOIClickListener();
    LinearListAdapter listAdapter;
    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
        
    public DynamicNormalPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        LinearLayout poiListView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_normal_poi, null);
        listAdapter = new LinearListAdapter(mSphinx, poiListView, R.layout.poi_dynamic_normal_list_item) {

            @Override
            public View getView(Object data, View child, int pos) {
                DynamicPOI dynamicPOI = ((DynamicPOI)data);
                ImageView iconImv = (ImageView) child.findViewById(R.id.icon_imv);
                TextView textTxv = (TextView) child.findViewById(R.id.text_txv);
                if (BaseQuery.DATA_TYPE_TUANGOU.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
                } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_yanchu);
                } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_zhanlan);
                } else if (BaseQuery.DATA_TYPE_COUPON.equals(dynamicPOI.getType())) {
                    iconImv.setImageResource(R.drawable.ic_dynamicpoi_coupon);
                }
                textTxv.setText(dynamicPOI.getSummary());
                child.setTag(dynamicPOI);
                child.setOnClickListener(clickListener);
                child.setBackgroundResource(R.drawable.list_middle);
                child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
                return null;
            }
            
        };
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, poiListView) {

            @Override
            public void refresh() {
                if (mPOI == null) {
                    clear();
                    return;
                }
                List<DynamicPOI> list = mPOI.getDynamicPOIList();
                List<DynamicPOI> dataList = new LinkedList<DynamicPOI>();
                for(int i = 0; i < list.size(); i++) {
                    final DynamicPOI dynamicPOI = list.get(i);
                    final String dataType = dynamicPOI.getType();
                    if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) 
                            || BaseQuery.DATA_TYPE_YANCHU.equals(dataType)
                            || BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType) 
                            || BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                        dataList.add(dynamicPOI);
                    }
                }
                
                int size = dataList.size();
                if (size == 0) {
                    clear();
                    return;
                }
                
                listAdapter.refreshList(dataList);
                
                for (int i = 0; i < size; i++){
                    View child = listAdapter.getChildView(i);
                    if (size != 1) {
                        if (i == 0) {
                            child.setBackgroundResource(R.drawable.list_header);
                            child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
                        } else if (i == (size - 1)) {
                            child.setBackgroundResource(R.drawable.list_footer);
                            child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
                        } else {
                            child.setBackgroundResource(R.drawable.list_middle);
                            child.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
                        }
                    } else {
                        child.setBackgroundResource(R.drawable.list_single);
                        child.findViewById(R.id.list_separator_imv).setVisibility(View.GONE);
                    }
                }
                
                show();
            }
        };

    }

    @Override
    public List<DynamicPOIViewBlock> getViewList() {
        blockList.clear();
        if (mPOI == null) {
            return blockList;
        }
        
        blockList.add(mViewBlock);
        return blockList;
    }
    

    private class NormalDPOIClickListener implements View.OnClickListener {
        
        @Override
        public void onClick(View view) {
            DynamicPOI dynamicPOI = (DynamicPOI)view.getTag();
            final String dataType = dynamicPOI.getType();
            DataOperation dataOperation = new DataOperation(mSphinx);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
            dataOperation.addParameter(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            String DPOIMasterUid = dynamicPOI.getMasterUid();
            if (DPOIMasterUid != null && !TextUtils.isEmpty(DPOIMasterUid)) {
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_DATA_UID, DPOIMasterUid);
            }
            
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
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
            } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailYanchu);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                             Yanchu.NEED_FIELD + Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                queryStart(dataOperation);
            } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailZhanlan);
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                        Zhanlan.NEED_FIELD + Util.byteToHexString(Zhanlan.FIELD_DESCRIPTION));
                dataOperation.addParameter(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Zhanlan.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Zhanlan.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                queryStart(dataOperation);
            } else if (BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                // 1 coupon, d operation
                if (DPOIMasterUid != null && !TextUtils.isEmpty(DPOIMasterUid)) {
                	mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.POIDetailCouponSingle);
                    dataOperation.addParameter(DataOperation.SERVER_PARAMETER_NEED_FIELD, Coupon.NEED_FIELD);
                    dataOperation.setup(Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                    queryStart(dataOperation);
                // some coupons, s operation
                } else {
                	mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag + ActionLog.POIDetailCouponMulti);
                    mSphinx.showView(R.id.view_coupon_list);
                    mSphinx.getCouponListFragment().setData(mPOI);
                }
            }
        }
    }

    @Override
    public void refresh() {
        mViewBlock.refresh();
    }

	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
        POI poi = mPOI;
        if (poi == null) {
            return;
        }
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
                // 查询演出的结果
            } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                Yanchu yanchu = ((YanchuQueryResponse) response).getYanchu();
                List<Yanchu> list = new ArrayList<Yanchu>();
                list.add(yanchu);
                mSphinx.showView(R.id.view_discover_yanchu_detail);
                mSphinx.getYanchuDetailFragment().setData(list, 0, null);

                // 查询展览的结果
            } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                Zhanlan zhanlan = ((ZhanlanQueryResponse) response).getZhanlan();
                List<Zhanlan> list = new ArrayList<Zhanlan>();
                list.add(zhanlan);
                mSphinx.showView(R.id.view_discover_zhanlan_detail);
                mSphinx.getZhanlanDetailFragment().setData(list, 0, null);
                //查询单条优惠券的结果
            } else if (BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                Coupon coupon = ((CouponQueryResponse) response).getCoupon();
                List<Coupon> list = new ArrayList<Coupon>();
                list.add(coupon);
                mSphinx.showView(R.id.view_coupon_detail);
                mSphinx.getCouponDetailFragment().setData(list, 0);
            }
        }
		
	}

	@Override
	public void onCancelled(TKAsyncTask tkAsyncTask) {
		
	}

    @Override
    public void loadData(int fromType) {
        refresh();
    }
    
}

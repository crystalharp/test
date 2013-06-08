package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.Response;
import com.tigerknows.model.Tuangou;
import com.tigerknows.model.Yanchu;
import com.tigerknows.model.Zhanlan;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DPOIType;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIViewBlock;
import com.tigerknows.widget.LinearListView;
import com.tigerknows.widget.LinearListView.ItemInitializer;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.FendianQueryResponse;
import com.tigerknows.model.DataOperation.YanchuQueryResponse;
import com.tigerknows.model.DataOperation.ZhanlanQueryResponse;

import android.graphics.Color;
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
    LinearListView lsv;
    List<DynamicPOIViewBlock> blockList = new ArrayList<DynamicPOIViewBlock>();
    
    ItemInitializer initer = new ItemInitializer(){

        @Override
        public void initItem(Object data, View v) {
            DynamicPOI dynamicPOI = ((DynamicPOI)data);
            ImageView iconImv = (ImageView) v.findViewById(R.id.icon_imv);
            TextView textTxv = (TextView) v.findViewById(R.id.text_txv);
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
            v.setTag(dynamicPOI);
            v.setOnClickListener(clickListener);
            v.setBackgroundResource(R.drawable.list_middle);
            v.findViewById(R.id.list_separator_imv).setVisibility(View.VISIBLE);
        }
        
    };
    
    public DynamicNormalPOI(POIDetailFragment poiFragment, LayoutInflater inflater){
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        mInflater = inflater;
        mViewBlock = new DynamicPOIViewBlock(mPOIDetailFragment.mBelowAddressLayout, POIDetailFragment.DPOIType.GROUPBUY);
        LinearLayout poiListView = (LinearLayout) mInflater.inflate(R.layout.poi_dynamic_normal_poi, null);
        lsv = new LinearListView(mSphinx, poiListView, initer, R.layout.poi_dynamic_poi_list_item);
        mViewBlock.mOwnLayout = poiListView;
        
    }
    
//    private class SortDynamicPOI{
//        DynamicPOI dynamicPOI;
//        DPOIType type;
//        
//        public SortDynamicPOI(DynamicPOI dPOI, DPOIType t){
//            dynamicPOI = dPOI;
//            type = t;
//        }
//    }
//    
//    private class DPOICompare implements Comparator<SortDynamicPOI> {
//
//        @Override
//        public int compare(SortDynamicPOI lhs, SortDynamicPOI rhs) {
//            return lhs.type.ordinal() - rhs.type.ordinal();
//        }
//    }

    @Override
    public List<DynamicPOIViewBlock> getViewList(POI poi) {
        blockList.clear();
        if (poi == null) {
            return blockList;
        }
        
        List<DynamicPOI> list = poi.getDynamicPOIList();
        List<DynamicPOI> dataList = new LinkedList<DynamicPOI>();
        int dPOIsize = (list != null ? list.size() : 0);
        for(int i = 0; i < dPOIsize; i++) {
            final DynamicPOI dynamicPOI = list.get(i);
            final String dataType = dynamicPOI.getType();
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType) 
                    || BaseQuery.DATA_TYPE_YANCHU.equals(dataType)
                    || BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType) 
                    || BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                dataList.add(dynamicPOI);
            }
        }
        
//        Collections.sort(dataList, new DPOICompare());
        
        int size = dataList.size();
        if (size == 0) {
            return blockList;
        }
        
        lsv.refreshList(dataList);
        
        for (int i = 0; i < size; i++){
            View child = lsv.getChildView(i);
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
        blockList.add(mViewBlock);
        return blockList;
    }
    

    private class NormalDPOIClickListener implements View.OnClickListener {
        
        @Override
        public void onClick(View view) {
            DynamicPOI dynamicPOI = (DynamicPOI)view.getTag();
            final String dataType = dynamicPOI.getType();
            DataOperation dataOperation = new DataOperation(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getMasterUid());
            
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailTuangou);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                             Tuangou.NEED_FIELD
                                + Util.byteToHexString(Tuangou.FIELD_NOTICED)
                                + Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT)
                                + Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[0]" + ";" +
                        Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN)+"_[0]");
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                List<BaseQuery> list = new ArrayList<BaseQuery>();
                list.add(dataOperation);
                dataOperation = new DataOperation(mSphinx);
                criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_FENDIAN);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, dynamicPOI.getSlaveUid());
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD, Fendian.NEED_FIELD);
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                query(mPOIDetailFragment, list);
            } else if (BaseQuery.DATA_TYPE_YANCHU.equals(dataType)) {
                mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailYanchu);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                             Yanchu.NEED_FIELD + Util.byteToHexString(Yanchu.FIELD_DESCRIPTION));
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Yanchu.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Yanchu.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                List<BaseQuery> list = new ArrayList<BaseQuery>();
                list.add(dataOperation);
                query(mPOIDetailFragment, list);
            } else if (BaseQuery.DATA_TYPE_ZHANLAN.equals(dataType)) {
                mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailZhanlan);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD,
                        Zhanlan.NEED_FIELD + Util.byteToHexString(Zhanlan.FIELD_DESCRIPTION));
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Zhanlan.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Zhanlan.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_DIANYING_DETAIL)+"_[0]");
                dataOperation.setup(criteria, Globals.getCurrentCityInfo().getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                List<BaseQuery> list = new ArrayList<BaseQuery>();
                list.add(dataOperation);
                query(mPOIDetailFragment, list);
            } else if (BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
                //TODO:add coupon query. Two situations, query coupon list or query coupon.
                // 1 coupon, d operation
                
                // some coupons, s operation
            }
        }
    }

    @Override
    public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response) {
        String dataType = query.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_TYPE);
        if (BaseActivity.checkResponseCode(query, mSphinx, null, true, this, false)) {
            return;
        }
        if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
            tuangou = ((TuangouQueryResponse) response).getTuangou();
            
        // 查询团购分店的结果
        } else if (BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)) {
            tuangou.setFendian(((FendianQueryResponse) response).getFendian());
            List<Tuangou> list = new ArrayList<Tuangou>();
            list.add(tuangou);
            mSphinx.showView(R.id.view_discover_tuangou_detail);
            mSphinx.getTuangouDetailFragment().setData(list, 0, null);
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
        //查询优惠券的结果
        } else if (BaseQuery.DATA_TYPE_COUPON.equals(dataType)) {
            //TODO:直接跳到优惠券页
        }
        
    }

    @Override
    public boolean checkExistence(POI poi) {
        return false;
    }
    
}

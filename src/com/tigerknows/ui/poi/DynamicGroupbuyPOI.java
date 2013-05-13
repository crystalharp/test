package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.Response;
import com.tigerknows.model.Tuangou;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.poi.POIDetailFragment.DPOIQueryInterface;
import com.tigerknows.ui.poi.POIDetailFragment.DPOIViewInitializer;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;
import com.tigerknows.model.DataOperation.TuangouQueryResponse;
import com.tigerknows.model.DataOperation.FendianQueryResponse;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DynamicGroupbuyPOI extends POIDetailFragment.DynamicPOIView{
    
    ImageView iconImv;
    TextView textTxv;
    static Tuangou tuangou = null;
    public static ArrayList<DynamicPOIView> DPOIPool = new ArrayList<DynamicPOIView>();
    public static DPOIQueryInterface queryInterface = new DPOIQueryInterface(){

        @Override
        public void checkExistence() {
        }

        @Override
        public void msgReceived(Sphinx mSphinx, BaseQuery query, Response response) {
            String dataType = query.getCriteria().get(DataOperation.SERVER_PARAMETER_DATA_TYPE);
            if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
                if (BaseActivity.checkResponseCode(query, mSphinx, null, true, this, false)) {
                    return;
                }
                tuangou = ((TuangouQueryResponse) response).getTuangou();
                
            // 查询团购分店的结果
            } else if (BaseQuery.DATA_TYPE_FENDIAN.equals(dataType)) {
                if (BaseActivity.checkResponseCode(query, mSphinx, null, true, this, false)) {
                    return;
                }
                tuangou.setFendian(((FendianQueryResponse) response).getFendian());
                List<Tuangou> list = new ArrayList<Tuangou>();
                list.add(tuangou);
                mSphinx.showView(R.id.view_discover_tuangou_detail);
                mSphinx.getTuangouDetailFragment().setData(list, 0, null);
            }
        }
        
    };
    
    private DynamicGroupbuyPOI(POIDetailFragment poiFragment, LayoutInflater inflater, LinearLayout belongsLayout, DynamicPOI poiData){
        mBelongsLayout = belongsLayout; 
        mOwnLayout = (LinearLayout) inflater.inflate(R.layout.poi_dynamic_poi_list_item, null).findViewById(R.id.dynamic_poi_list_item);
        iconImv = (ImageView) mOwnLayout.findViewById(R.id.icon_imv);
        textTxv = (TextView) mOwnLayout.findViewById(R.id.text_txv);
        mType = POIDetailFragment.DPOIType.GROUPBUY;
        mPOIDetailFragment = poiFragment;
        mSphinx = mPOIDetailFragment.mSphinx;
        
        refreshData(poiData);
        final String dataType = data.getType();
        //TODO:del the following line
        mOwnLayout.setBackgroundResource(R.drawable.list_single);
       
        mOwnLayout.setOnClickListener(new View.OnClickListener() {
        
            @Override
           public void onClick(View view) {
                DataOperation dataOperation = new DataOperation(mSphinx);
                List<BaseQuery> list = new ArrayList<BaseQuery>();
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, data.getMasterUid());
                
                mPOIDetailFragment.mActionLog.addAction(mPOIDetailFragment.mActionTag +  ActionLog.POIDetailTuangou);
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
                             Tuangou.NEED_FILELD
                                + Util.byteToHexString(Tuangou.FIELD_NOTICED)
                                + Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT)
                                + Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
                criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
                        Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[0]" + ";" +
                        Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[0]" + ";" +
                        Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN)+"_[0]");
                dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                dataOperation = new DataOperation(mSphinx);
                criteria = new Hashtable<String, String>();
                criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_FENDIAN);
                criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, data.getSlaveUid());
                criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Fendian.NEED_FILELD);
                dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
                list.add(dataOperation);
                
                query(mPOIDetailFragment, list);
            }
        });
    }
    
    public static DPOIViewInitializer<DynamicGroupbuyPOI> Initializer = new DPOIViewInitializer<DynamicGroupbuyPOI>() {

        @Override
        public DynamicGroupbuyPOI init(POIDetailFragment poiFragment, LayoutInflater inflater,
                LinearLayout belongsLayout, DynamicPOI data) {
            return new DynamicGroupbuyPOI(poiFragment, inflater, belongsLayout, data);
        }
        
    };

    @Override
    public void refreshData(DynamicPOI poiData) {
        this.data = poiData; 
        textTxv.setText(data.getSummary());
        iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
    }
    

}

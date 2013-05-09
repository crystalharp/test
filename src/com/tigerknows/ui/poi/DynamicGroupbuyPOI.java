package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.Fendian;
import com.tigerknows.model.POI.DynamicPOI;
import com.tigerknows.model.Tuangou;
import com.tigerknows.ui.poi.POIDetailFragment.DPOIViewInitializer;
import com.tigerknows.ui.poi.POIDetailFragment.DynamicPOIView;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DynamicGroupbuyPOI extends POIDetailFragment.DynamicPOIView{
    
    DynamicGroupbuyPOI instance = null;
    Sphinx mSphinx;
    POIDetailFragment mPOIDetailFragment;
    public static ArrayList<DynamicPOIView> DPOIPool = new ArrayList<DynamicPOIView>();
//    public static ArrayList<DynamicGroupbuyPOI> DPOIPool = new ArrayList<DynamicGroupbuyPOI>();
    
    private DynamicGroupbuyPOI(LayoutInflater inflater, LinearLayout belongsLayout, DynamicPOI poiData){
        mBelongsLayout = belongsLayout; 
        mOwnLayout = (LinearLayout) inflater.inflate(R.layout.poi_dynamic_poi_list_item, null).findViewById(R.id.dynamic_poi_list_item);
        mType = POIDetailFragment.DPOIType.GROUPBUY;
        data = poiData;
        final String dataType = data.getType();
        ImageView iconImv = (ImageView) mOwnLayout.findViewById(R.id.icon_imv);
        TextView textTxv = (TextView) mOwnLayout.findViewById(R.id.text_txv);
        textTxv.setText(data.getSummary());
        iconImv.setImageResource(R.drawable.ic_dynamicpoi_tuangou);
        mOwnLayout.setBackgroundResource(R.drawable.list_single);
//       mSphinx = mPOIDetailFragment.mSphinx;
       
//       mOwnLayout.setOnClickListener(new View.OnClickListener() {
//        
//        @Override
//       public void onClick(View view) {
//                        DataOperation dataOperation = new DataOperation(mSphinx);
//                        Hashtable<String, String> criteria = new Hashtable<String, String>();
//                        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, dataType);
//                        criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
//                        criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, data.getMasterUid());
//                        
//                        if (BaseQuery.DATA_TYPE_TUANGOU.equals(dataType)) {
////                            mActionLog.addAction(mActionTag +  ActionLog.POIDetailTuangou);
//                            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD,
//                                         Tuangou.NEED_FILELD
//                                            + Util.byteToHexString(Tuangou.FIELD_NOTICED)
//                                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_TEXT)
//                                            + Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC));
//                            criteria.put(DataOperation.SERVER_PARAMETER_PICTURE,
//                                    Util.byteToHexString(Tuangou.FIELD_PICTURES)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_LIST)+"_[0]" + ";" +
//                                    Util.byteToHexString(Tuangou.FIELD_PICTURES_DETAIL)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_DETAIL)+"_[0]" + ";" +
//                                    Util.byteToHexString(Tuangou.FIELD_CONTENT_PIC)+":"+Globals.getPicWidthHeight(TKConfig.PICTURE_TUANGOU_TAOCAN)+"_[0]");
//                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
//                            List<BaseQuery> list = new ArrayList<BaseQuery>();
//                            list.add(dataOperation);
//                            dataOperation = new DataOperation(mSphinx);
//                            criteria = new Hashtable<String, String>();
//                            criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
//                            criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, BaseQuery.DATA_TYPE_FENDIAN);
//                            criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, data.getSlaveUid());
//                            criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Fendian.NEED_FILELD);
//                            dataOperation.setup(criteria, Globals.g_Current_City_Info.getId(), mPOIDetailFragment.getId(), mPOIDetailFragment.getId(), mSphinx.getString(R.string.doing_and_wait));
//                            list.add(dataOperation);
//                            mPOIDetailFragment.mTkAsyncTasking = mSphinx.queryStart(list);
//                            mPOIDetailFragment.mBaseQuerying = list;
//            
//        }
//    });
    }
    
    public static DPOIViewInitializer<DynamicGroupbuyPOI> Initializer = new DPOIViewInitializer<DynamicGroupbuyPOI>() {

        @Override
        public DynamicGroupbuyPOI init(LayoutInflater inflater,
                LinearLayout belongsLayout, DynamicPOI data) {
            return new DynamicGroupbuyPOI(inflater, belongsLayout, data);
        }
        
    };

}

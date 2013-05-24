package com.tigerknows.ui.hotel;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.Hotel;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.ui.BaseFragment;

public class HotelIntroFragment extends BaseFragment {

    TextView hotelNameTxv;
    TextView longDescriptionTxv;
    TextView roomDescriptionTxv;
    TextView hotelServiceTxv;
    View longDescriptionBlock;
    View roomDescriptionBlock;
    View hotelServiceBlock;
    
    Hotel mHotel;
    POI mPOI;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = mLayoutInflater.inflate(R.layout.hotel_intro, container, false);
		hotelNameTxv = (TextView) mRootView.findViewById(R.id.hotel_head);
		longDescriptionTxv = (TextView) mRootView.findViewById(R.id.hotel_long_description);
		roomDescriptionTxv = (TextView) mRootView.findViewById(R.id.hotel_room_description);
		hotelServiceTxv = (TextView) mRootView.findViewById(R.id.hotel_service);
		longDescriptionBlock = mRootView.findViewById(R.id.hotel_long_desc_block);
		roomDescriptionBlock = mRootView.findViewById(R.id.hotel_room_desc_block);
		hotelServiceBlock = mRootView.findViewById(R.id.hotel_service_block);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		mTitleBtn.setText(mSphinx.getString(R.string.hotel_description));
		hotelNameTxv.setText(mPOI.getName());
		String longDescription = mHotel.getLongDescription();
		String roomDescription = mHotel.getRoomDescription();
		String hotelService = mHotel.getService();
		LogWrapper.d("conan", mHotel.toString());
		if (longDescription == null){
		    longDescriptionBlock.setVisibility(View.GONE);
		} else {
		    longDescriptionTxv.setText(mHotel.getLongDescription());
		    longDescriptionBlock.setVisibility(View.VISIBLE);
		}
		if (roomDescription == null) {
		    roomDescriptionBlock.setVisibility(View.GONE);
		} else {
		    roomDescriptionBlock.setVisibility(View.VISIBLE);
		    roomDescriptionTxv.setText(mHotel.getRoomDescription());
		}
		if (hotelService == null) {
		    hotelServiceBlock.setVisibility(View.GONE);
		} else {
		    hotelServiceBlock.setVisibility(View.VISIBLE);
		    hotelServiceTxv.setText(mHotel.getService());
		}
	}

	public HotelIntroFragment(Sphinx sphinx) {
		super(sphinx);
	}
	
	public void setData(POI poi) {
	    mPOI = poi;
	    mHotel = poi.getHotel();
	    if (mHotel.getLongDescription() == null &&
	            mHotel.getService() == null &&
	            mHotel.getRoomDescription() == null) {
	        Hashtable<String, String> criteria = new Hashtable<String, String>();
	        criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_POI);
	        criteria.put(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataQuery.SUB_DATA_TYPE_HOTEL);
	        criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
	        criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
	        criteria.put(DataOperation.SERVER_PARAMETER_NEED_FEILD, Hotel.NEED_FILED_DESCRIPTION+"01");   // 01表示poi的uuid
	        //FIXME:服务器说这里可以不用checkin和checkout,等他们修改
            criteria.put(DataOperation.SERVER_PARAMETER_CHECKIN, "2013-05-25");
            criteria.put(DataOperation.SERVER_PARAMETER_CHECKOUT, "2013-05-26");
	        DataOperation dataOpration = new DataOperation(mSphinx);
	        dataOpration.setup(criteria, Globals.g_Current_City_Info.getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
	        this.mTkAsyncTasking = mSphinx.queryStart(dataOpration);
	    }
	}
	
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask){
	    super.onPostExecute(tkAsyncTask);
	    List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
	    BaseQuery baseQuery = baseQueryList.get(0);
	    Response response = baseQuery.getResponse();
	    POI onlinePOI = ((POIQueryResponse)response).getPOI();
        if (onlinePOI != null && onlinePOI.getUUID() != null && onlinePOI.getUUID().equals(mPOI.getUUID())) {
    	    try {
    	        if (mHotel == null) {
                    mPOI.init(onlinePOI.getData(), false);
                } else {
                    mHotel.init(onlinePOI.getData(), false);
                }
            } catch (APIException e) {
                e.printStackTrace();
            }
        }
        //TODO:如果三项数据都没有则显示暂无简介
	    onResume();
	}

}

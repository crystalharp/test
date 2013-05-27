package com.tigerknows.ui.hotel;

import java.util.Calendar;
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
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;

public class HotelIntroFragment extends BaseFragment {

    TextView hotelNameTxv;
    TextView longDescriptionTxv;
    TextView roomDescriptionTxv;
    TextView hotelServiceTxv;
    View longDescriptionBlock;
    View roomDescriptionBlock;
    View hotelServiceBlock;
    View hotelNoDescription;
    RetryView mRetryView;
    QueryingView mQueryingView;
    View mHotelScrollView;
    int mState;
    static final int STATE_QUERYING = 0;
    static final int STATE_ERROR = 1;
    static final int STATE_EMPTY = 2;
    static final int STATE_SHOWING = 3;
    DataOperation dataOpration = new DataOperation(mSphinx);
    
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
		hotelNoDescription = mRootView.findViewById(R.id.hotel_no_description);
		mRetryView = (RetryView) mRootView.findViewById(R.id.hotel_retry_view);
		mQueryingView = (QueryingView) mRootView.findViewById(R.id.querying_view);
		mHotelScrollView = mRootView.findViewById(R.id.hotel_scrollView);
		mRetryView.setCallBack(new RetryView.CallBack() {
            
            @Override
            public void retry() {
    	        mTkAsyncTasking = mSphinx.queryStart(dataOpration);
//    	        mQueryingView.setVisibility(View.VISIBLE);
//    	        mRetryView.setVisibility(View.GONE);
    	        mState = STATE_QUERYING;
    	        updateView();
            }
        }, mActionTag);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		mState = STATE_SHOWING;
		updateView();
		mTitleBtn.setText(mSphinx.getString(R.string.hotel_description));
		hotelNameTxv.setText(mPOI.getName());
		hotelNoDescription.setVisibility(View.GONE);
		String longDescription = mHotel.getLongDescription();
		String roomDescription = mHotel.getRoomDescription();
		String hotelService = mHotel.getService();
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
	
	public void setData(POI poi, Calendar in, Calendar out) {
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
            criteria.put(DataOperation.SERVER_PARAMETER_CHECKIN, HotelHomeFragment.SIMPLE_DATE_FORMAT.format(in.getTime()));
            criteria.put(DataOperation.SERVER_PARAMETER_CHECKOUT, HotelHomeFragment.SIMPLE_DATE_FORMAT.format(out.getTime()));
	        dataOpration.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null);
	        this.mTkAsyncTasking = mSphinx.queryStart(dataOpration);
	        mState = STATE_QUERYING;
	        updateView();
	    }
	}
	
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask){
	    super.onPostExecute(tkAsyncTask);
	    List<BaseQuery> baseQueryList = tkAsyncTask.getBaseQueryList();
	    BaseQuery baseQuery = baseQueryList.get(0);
	    Response response = baseQuery.getResponse();
	    if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
	        isReLogin = true;
	        return;
	    }
	    
	    if (response == null || response.getResponseCode() != Response.RESPONSE_CODE_OK) {
	        mState = STATE_ERROR;
	        updateView();
            return;
	    }
	    
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
        if (mHotel.getLongDescription() != null || mHotel.getRoomDescription() != null || mHotel.getService() != null) {
            onResume();
        } else {
            mState = STATE_EMPTY;
            updateView();
        }
	}

	private void updateView() {
        if (mState == STATE_QUERYING) {
            mQueryingView.setVisibility(View.VISIBLE);
            mRetryView.setVisibility(View.GONE);
            hotelNoDescription.setVisibility(View.GONE);
            mHotelScrollView.setVisibility(View.GONE);
        } else if (mState == STATE_ERROR) {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setText(R.string.touch_screen_and_retry);
            mRetryView.setVisibility(View.VISIBLE);
            hotelNoDescription.setVisibility(View.GONE);
            mHotelScrollView.setVisibility(View.GONE);
        } else if (mState == STATE_EMPTY){
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            hotelNoDescription.setVisibility(View.VISIBLE);
            mHotelScrollView.setVisibility(View.GONE);
        } else {
            mQueryingView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.GONE);
            hotelNoDescription.setVisibility(View.GONE);
            mHotelScrollView.setVisibility(View.VISIBLE);
        }
	}
}

package com.tigerknows.ui.hotel;

import java.util.Hashtable;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.ProxyQuery;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;
import com.tigerknows.util.ValidateUtil;

public class HotelSeveninnRegistFragment extends BaseFragment implements View.OnClickListener{

	public HotelSeveninnRegistFragment(Sphinx sphinx) {
		super(sphinx);
		// TODO Auto-generated constructor stub
	}
	
	static final String TAG = "HotelSeveninnRegistFragment";
	
	private TextView mSeveninnNoteTxv;
	private EditText mSeveninnNameEdt;
	private EditText mSeveninnPhoneEdt;
	private EditText mSeveninnIdcardCodeEdt;
	private Button mSeveninnRegistBtn;
	
	private String mGotMobile;
	private String mPersonName;
	private String mMobile;
	private String mIdcardNo;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //TODO: mActionTag;
    }
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.hotel_seveninn_regist, container, false);
        
        findViews();
        setListener();
        return mRootView;
    }
	
	public void onResume(){
		super.onResume();
	}

	protected void findViews() {
		// TODO Auto-generated method stub
		mSeveninnNoteTxv = (TextView) mRootView.findViewById(R.id.seveninn_note_txv);
		mSeveninnNameEdt = (EditText) mRootView.findViewById(R.id.seveninn_name_edt);
		mSeveninnPhoneEdt = (EditText) mRootView.findViewById(R.id.seveninn_phone_edt);
		mSeveninnIdcardCodeEdt = (EditText) mRootView.findViewById(R.id.seveninn_idcard_code_edt);
		mSeveninnRegistBtn = (Button) mRootView.findViewById(R.id.seveninn_regist_btn);
	}

	protected void setListener() {
		// TODO Auto-generated method stub
		
		mSeveninnNameEdt.setOnClickListener(this);
		mSeveninnPhoneEdt.setOnClickListener(this);
		mSeveninnIdcardCodeEdt.setOnClickListener(this);
		mSeveninnRegistBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		int id = view.getId();
		switch(id){
		case R.id.left_btn:
			exit();
			break;
		case R.id.seveninn_regist_btn:
			String str = mSeveninnNameEdt.getText().toString().trim();
			if(TextUtils.isEmpty(str)){
				mSeveninnNameEdt.requestFocus();
				Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_room_person_empty_tip));
				mSphinx.showSoftInput();
				return;
				// 注册7天酒店时，不校验姓名格式
			}else{
				mPersonName = str;
			}
			str = mSeveninnPhoneEdt.getText().toString();
			if(TextUtils.isEmpty(str)){
				mSeveninnPhoneEdt.requestFocus();
				Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_room_mobile_empty_tip));
				mSphinx.showSoftInput();
				return;
        	}else if(!ValidateUtil.isValidPhone(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.phone_format_error_tip));
        		return;
			}else{
				mMobile = str;
			}
			str = mSeveninnIdcardCodeEdt.getText().toString().trim();
			if(TextUtils.isEmpty(str)){
				mSeveninnIdcardCodeEdt.requestFocus();
				Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.seveninn_idcard_code_empty_error));
				mSphinx.showSoftInput();
				return;
        	}else if(!ValidateUtil.isValidIdCardCode(str)){
        		Utility.showNormalDialog(mSphinx, mSphinx.getString(R.string.hotel_idcard_code_format));
        		return;
			}else{
				mIdcardNo = str;
			}
			submit();
			break;
		}
	}

	private void submit() {
		// TODO Auto-generated method stub
		ProxyQuery proxyQuery = new ProxyQuery(mSphinx);
		Hashtable<String, String> criteria = new Hashtable<String, String>();
		criteria.put(ProxyQuery.SERVER_PARAMETER_TASK, ProxyQuery.TASK_REGISTER_7_DAY_MEMBER);
		criteria.put(ProxyQuery.SERVER_PARAMETER_USERNAME, mPersonName);
		criteria.put(ProxyQuery.SERVER_PARAMETER_MOBILE, mMobile);
		criteria.put(ProxyQuery.SERVER_PARAMETER_IDCARDNO, mIdcardNo);
		proxyQuery.setup(criteria, mSphinx.getHotelHomeFragment().getCityInfo().getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
		mSphinx.queryStart(proxyQuery);
	}

	private void exit() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask){
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
		if(baseQuery.isStop()){
			return;
		}
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), getId(), getId(), mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, HotelSeveninnRegistFragment.this, true)) {
            return;
        }
    	Response response = baseQuery.getResponse();
    	switch(response.getResponseCode()){
    	case Response.RESPONSE_CODE_OK:
    		break;
    	case Response.RESPONSE_CODE_HOTEL_REGIST_MEMBER_FAILED:
    		Utility.showNormalDialog(mSphinx,mSphinx.getString(R.string.seveninn_regist_failed));
    		break;
    	}
	}
	
	public void setData(String mobile){
		mGotMobile = mobile;
	}
}
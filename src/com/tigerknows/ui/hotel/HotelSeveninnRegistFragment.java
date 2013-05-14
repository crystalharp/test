package com.tigerknows.ui.hotel;

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
import com.tigerknows.ui.BaseFragment;

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
				Toast.makeText(mContext, mSphinx.getString(R.string.hotel_room_person_empty_tip), Toast.LENGTH_SHORT).show();
				mSphinx.showSoftInput();
				return;
			}else{
				mPersonName = str;
			}
			str = mSeveninnPhoneEdt.getText().toString();
			if(TextUtils.isEmpty(str)){
				mSeveninnPhoneEdt.requestFocus();
				Toast.makeText(mContext, mSphinx.getString(R.string.hotel_room_mobile_empty_tip), Toast.LENGTH_SHORT).show();
				mSphinx.showSoftInput();
				return;
			}else{
				mMobile = str;
			}
			str = mSeveninnIdcardCodeEdt.getText().toString().trim();
			if(TextUtils.isEmpty(str)){
				mSeveninnIdcardCodeEdt.requestFocus();
				Toast.makeText(mContext, mSphinx.getString(R.string.seveninn_idcard_code_empty_error), Toast.LENGTH_SHORT).show();
				mSphinx.showSoftInput();
				return;
			}else{
				mIdcardNo = str;
			}
			break;
		}
	}

	private void exit() {
		// TODO Auto-generated method stub
		
	}
	
	public void setDate(String mobile){
		mGotMobile = mobile;
	}
}
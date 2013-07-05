package com.tigerknows.ui.hotel;

import java.util.Hashtable;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Selection;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
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
    
    private LinearLayout mSeveninnRegistLly;
    private TextView mSeveninnNoteTxv;
    private EditText mSeveninnNameEdt;
    private EditText mSeveninnPhoneEdt;
    private EditText mSeveninnIdcardCodeEdt;
    private Button mSeveninnRegistBtn;
    
    private String mPersonName;
    private String mMobile;
    private String mIdcardNo;
    
    private String mActionTag;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.HotelSeveninnRegist;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mRootView = mLayoutInflater.inflate(R.layout.hotel_seveninn_regist, container, false);
        
        findViews();
        setListener();
        return mRootView;
    }
    
    public void onResume(){
        super.onResume();
        mTitleBtn.setText(mSphinx.getString(R.string.seveninn_regist_title));
    }

    protected void findViews() {
        // TODO Auto-generated method stub
        mSeveninnRegistLly = (LinearLayout) mRootView.findViewById(R.id.seveninn_regist_lly);
        mSeveninnNoteTxv = (TextView) mRootView.findViewById(R.id.seveninn_note_txv);
        mSeveninnNameEdt = (EditText) mRootView.findViewById(R.id.seveninn_name_edt);
        mSeveninnPhoneEdt = (EditText) mRootView.findViewById(R.id.seveninn_phone_edt);
        mSeveninnIdcardCodeEdt = (EditText) mRootView.findViewById(R.id.seveninn_idcard_code_edt);
        mSeveninnRegistBtn = (Button) mRootView.findViewById(R.id.seveninn_regist_btn);
    }

    protected void setListener() {
        // TODO Auto-generated method stub
        mSeveninnRegistBtn.setOnClickListener(this);
        mSeveninnRegistLly.setOnTouchListener(new OnTouchListener(){
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSphinx.hideSoftInput();
                }
                return true;
            }
        });
        OnTouchListener edtTouchListener = new OnTouchListener() {
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    switch (v.getId()){
                    case R.id.seveninn_name_edt:
                        mActionLog.addAction(mActionTag + ActionLog.HotelSeveninnRegistName);
                        break;
                    case R.id.seveninn_phone_edt:
                        mActionLog.addAction(mActionTag + ActionLog.HotelSeveninnRegistPhone);
                        break;
                    case R.id.seveninn_idcard_code_edt:
                        mActionLog.addAction(mActionTag + ActionLog.HotelSeveninnRegistIdcard);
                        break;
                    }
                }
                return false;
            }
        };
        mSeveninnNameEdt.setOnTouchListener(edtTouchListener);
        mSeveninnPhoneEdt.setOnTouchListener(edtTouchListener);
        mSeveninnIdcardCodeEdt.setOnTouchListener(edtTouchListener);    
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
            mActionLog.addAction(mActionTag + ActionLog.HotelSeveninnRegistSubmit);
            String str = mSeveninnNameEdt.getText().toString().trim();
            if(TextUtils.isEmpty(str)){
                Utility.showEdittextErrorDialog(mSphinx, mSphinx.getString(R.string.hotel_room_person_empty_tip), mSeveninnNameEdt);
                return;
            }else if(!ValidateUtil.isValidElongName(str)){
                Utility.showEdittextErrorDialog(mSphinx, mSphinx.getString(R.string.hotel_person_name_format), mSeveninnNameEdt);
                return;
            }else{
                mPersonName = str;
            }
            str = mSeveninnPhoneEdt.getText().toString();
            if(TextUtils.isEmpty(str)){
                Utility.showEdittextErrorDialog(mSphinx, mSphinx.getString(R.string.hotel_room_mobile_empty_tip), mSeveninnPhoneEdt);
                return;
            }else if(!ValidateUtil.isValidHotelMobile(str)){
                Utility.showEdittextErrorDialog(mSphinx, mSphinx.getString(R.string.hotel_mobile_format), mSeveninnPhoneEdt);
                return;
            }else{
                mMobile = str;
            }
            str = mSeveninnIdcardCodeEdt.getText().toString().trim();
            if(TextUtils.isEmpty(str)){
                Utility.showEdittextErrorDialog(mSphinx, mSphinx.getString(R.string.seveninn_idcard_code_empty_error), mSeveninnIdcardCodeEdt);
                return;
            }else if(!ValidateUtil.isValidIdCardCode(str)){
                Utility.showEdittextErrorDialog(mSphinx, mSphinx.getString(R.string.hotel_idcard_code_format), mSeveninnIdcardCodeEdt);
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
        proxyQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
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
        } else if (BaseActivity.checkResponseCode(baseQuery, mSphinx, new int[]{825}, BaseActivity.SHOW_ERROR_MSG_DIALOG, HotelSeveninnRegistFragment.this, false, true)) {
            return;
        }
        Response response = baseQuery.getResponse();
        switch(response.getResponseCode()){
        case Response.RESPONSE_CODE_OK:
            mSphinx.getHotelOrderWriteFragment().setMember(response.getDescription());
            break;
        default:
            mSphinx.getHotelOrderWriteFragment().setMember(response.getDescription());
            break;
        }
    }
    
    public void setData(String name, String mobile, String idcard){
        mSeveninnNameEdt.setText(name);
        mSeveninnNameEdt.requestFocus();
        Selection.setSelection(mSeveninnNameEdt.getText(), mSeveninnNameEdt.length());
        mSeveninnPhoneEdt.setText(mobile);
        mSeveninnPhoneEdt.requestFocus();
        Selection.setSelection(mSeveninnPhoneEdt.getText(), mSeveninnPhoneEdt.length());
        mSeveninnIdcardCodeEdt.setText(idcard);
        mSeveninnIdcardCodeEdt.requestFocus();
        Selection.setSelection(mSeveninnIdcardCodeEdt.getText(), mSeveninnIdcardCodeEdt.length());
        mSeveninnNoteTxv.setText(Utility.renderColorToPartOfString(mContext,
                R.color.orange,
                mSphinx.getString(R.string.seveninn_note, mobile),
                mobile));
    }
}
package com.tigerknows.ui.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKFragmentManager;
import com.tigerknows.android.os.TKAsyncTask;
import android.widget.Toast;

import com.tigerknows.model.AccountManage;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Response;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.util.Utility;

/**
 * 用户模块基础类
 * 
 * 1. 提供输入验证支持
 * 2. 提供公共参数支持
 * 3. 提供服务器响应码处理支持
 * 
 * @author linqingzu
 *
 */
public abstract class UserBaseFragment extends BaseFragment {

	public UserBaseFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
	
	public static final String ACTION_RUN_THEN_BACK = "com.tigerknows.ACTION_RUN_THEN_BACK";
	
	protected Form mForm;

	protected ResponseHandler mResponseHandler = new ResponseHandler();
	
	protected ViewGroup mFormView;
	
	private static final String TAG = "UserBaseDialog";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	protected void findViews() {
		// TODO Auto-generated method stub
//		mFormView = (ViewGroup)findViewById(R.id.root_view);
		mForm = new Form((ViewGroup)mRootView);
	}
	
	/**
	 * 提供输入验证支持
	 * 
	 * @author linqingzu
	 *
	 */
	class Form {

		private List<ExtValidationEditText> childEditTxts = new ArrayList<ExtValidationEditText>();
		
		private ExtValidationEditText mErrorSource = null;
		
	    private Comparator<ExtValidationEditText> mComparator = new Comparator<ExtValidationEditText>() {

	        @Override
	        public int compare(ExtValidationEditText object1, ExtValidationEditText object2) {
	            return (int)(object1.flag - object2.flag);
	        };
	    };
	    
	    private final static String TAG = "FormView";

		public Form(ViewGroup group) {
			mineValidationEdt(group);
		}
		
		public void mineValidationEdt(ViewGroup group) {
			for(int i = 0; i < group.getChildCount(); i++) {
				View view = group.getChildAt(i);
				if (view instanceof ExtValidationEditText) {
					childEditTxts.add((ExtValidationEditText)view);
				} else if (view instanceof ViewGroup) {
					mineValidationEdt((ViewGroup)view);
				}
			}
			LogWrapper.d(TAG, "childEditTxts: " + childEditTxts);
		}
		
		/**
		 * 验证所有的输入框是否符合要求
		 * @return
		 */
		public boolean isValid() {
			Collections.sort(childEditTxts, mComparator);

			for(ExtValidationEditText child : childEditTxts) {
				child.setUpRelation(childEditTxts);
				if(!child.isValid()) {
					mErrorSource = child;
					return false;
				}
			}
			
			return true;
		}
		
		public ExtValidationEditText getErrorSource() {
			return mErrorSource;
		}
		
	}
	
	/**
	 * 提供公共参数支持
	 */
	protected void sendRequest(AccountManage accountManage) {
		accountManage.setup(getId(), getId(), getString(R.string.doing_and_wait));
		mSphinx.queryStart(accountManage, false);
	}
	
	@Override
	public void onPostExecute(TKAsyncTask tkAsyncTask) {
		// TODO Auto-generated method stub
		super.onPostExecute(tkAsyncTask);
		BaseQuery baseQuery = tkAsyncTask.getBaseQuery();
		if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(TKFragmentManager.ID_view_user_home), getId(), TKFragmentManager.ID_view_user_home, TKFragmentManager.ID_view_more_home, null)) {
		    return;
        }

		Response response = baseQuery.getResponse();
		if (response != null) {
    		AccountManage accountManage = (AccountManage)baseQuery;
        	dealResponse(accountManage);
		} else {
            Toast.makeText(mSphinx, R.string.network_failed, Toast.LENGTH_LONG).show();
        }
	}
	
	/**
	 * 对外公开的处理服务返回的响应码
	 * @param accountManage
	 */
	public void dealResponse(AccountManage accountManage) {
		if (accountManage != null && accountManage.getResponse() != null) {
			responseCodeAction(accountManage);
		} else {
			LogWrapper.d(TAG, "response is null");
		}
	}
	
	/**
	 * 提供服务器响应码处理支持
	 * @author linqingzu
	 *
	 */
	public class ResponseHandler {
		
		/**
		 * 显示错误提示框, 点击确定后, 全选输入框的内容, 并弹出键盘
		 * @param title
		 * @param body
		 * @param edittext
		 */
		public void showErrorThenSelectAllAndShowSoftkeyboard(int titleResId, int bodyResId, final ExtValidationEditText edittext) {
			Utility.showNormalDialog(mSphinx, getString(titleResId), getString(bodyResId), getString(R.string.confirm),
	                null,
	                new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					// TODO Auto-generated method stub
					if (which == DialogInterface.BUTTON_POSITIVE) {
						edittext.selectAll();
						edittext.requestFocus();
						mSphinx.showSoftInput(edittext);
					}
				}
				
			});
		}
		
		/**
		 * 显示错误提示框, 点击确定后, 清除输入框的内容, 并弹出键盘
		 * @param title
		 * @param body
		 * @param edittext
		 */
		public void showErrorThenClearAndShowSoftkeyboard(int titleResId, int bodyResId, final ExtValidationEditText edittext) {
			Utility.showNormalDialog(mSphinx, getString(titleResId), getString(bodyResId), getString(R.string.confirm),
	                null,
	                new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					// TODO Auto-generated method stub
					if (which == DialogInterface.BUTTON_POSITIVE) {
						edittext.setText("");
						edittext.requestFocus();
						mSphinx.showSoftInput(edittext);
					}
				}
				
			});
		}
		
	}
	
	protected abstract void responseCodeAction(AccountManage accountManage);
	
	protected void showToast(int resId) {
		Toast.makeText(mSphinx, resId, 2000).show();
	}
}

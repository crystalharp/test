package com.tigerknows.ui.user;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.common.ActionLog;
import com.tigerknows.util.ValidateUtil;

public class ExtValidationEditText extends EditText {

	public List<ValidType> valids = new ArrayList<ValidType>();
	
	// 验证类型
	public enum ValidType {
		Phone, Password, Name, ValiNum, RePassword, Other;
	}
	
	// 错误类型
	public enum ValidErrorType {
		PhoneFormatError, PasswordFormatError, UnionDifferentError, NameFormatError, EmptyError, Other
	}
	
	// 验证类型
	public ValidType validType = ValidType.Other;

	// 错误类型
	public ValidErrorType validErrorType = ValidErrorType.Other;
	
	/*
	 * 显示的错误信息
	 */
	public String msg;

	/*
	 * 多个ExtValidationEditText的标志
	 * xml中标签名: flag
	 */
	public int flag = 0;
	
	/*
	 * 当且仅当mValideType为Union时有用
	 * xml中标签名: unionFlag
	 */
	public int mUnionViewFlag = -1;

	/*
	 * 当且仅当mValideType为Union时有用
	 */
	private ExtValidationEditText mUnionView = null;

	/*
	 * 验证出错的错误提示
	 * xml中标签名: formatErrorTip
	 */
	private String mFormatErrorTip;

	/*
	 * 输入为空时的错误提示
	 * xml中标签名: emptyErrorTip
	 */
	private String mEmptyErrorTip;
	
	/*
	 * 关联的两个输入不同时的错误提示
	 * xml中标签名: unionErrorTip
	 */
	private String mUnionErrortip;
	
	ActionLog mActionLog;
	
	private final static String TAG = "ExtValidationEditText";
	
	public boolean hasUnionView() {
		return mUnionView != null;
	}
	
	public ExtValidationEditText getUnionView() {
		return mUnionView;
	}
	
	public ExtValidationEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
		mActionLog = ActionLog.getInstance(context);
		
		mFormatErrorTip  = getAttributeStringResource(context, attrs, "formatErrorTip");
		mEmptyErrorTip  = getAttributeStringResource(context, attrs, "emptyErrorTip");
		mUnionErrortip  = getAttributeStringResource(context, attrs, "unionErrorTip");
		
		String mValidate = attrs.getAttributeValue(null, "validateType");
		if (!TextUtils.isEmpty(mValidate)) {
			if ("phone".equals(mValidate.toLowerCase())) {
				validType = ValidType.Phone;
			} else if ("password".equals(mValidate.toLowerCase())) {
				validType = ValidType.Password;
			} else if ("name".equals(mValidate.toLowerCase())) {
				validType = ValidType.Name;
			} else if ("validnum".equals(mValidate.toLowerCase())) {
				validType = ValidType.ValiNum;
			} else if ("repassword".equals(mValidate.toLowerCase())) {
				validType = ValidType.RePassword;
			} else {
				validType = ValidType.Other;
			}
		}
		LogWrapper.d(TAG, "validType: " + validType);

		flag = attrs.getAttributeIntValue(null, "flag", 0);
		LogWrapper.d(TAG, "flag: " + flag);
		mUnionViewFlag = attrs.getAttributeIntValue(null, "unionFlag", -1);
		LogWrapper.d(TAG, "unionFlag: " + mUnionViewFlag);
		
		setListener();
	}
	
	private String getAttributeStringResource(Context context, AttributeSet attrs, String tag) {
		int resourceId = attrs.getAttributeResourceValue(null, tag, 0);
		String value = null;
		if (resourceId > 0) {
			value = context.getResources().getText(resourceId).toString();
			LogWrapper.d(TAG, tag + ": " + value);
		}
		
		return value;
	}

	private void setListener() {
	}
	
	public void setUpRelation(List<ExtValidationEditText> others) {
		if (mUnionViewFlag != -1) {
			for(ExtValidationEditText other : others) {
				if (mUnionViewFlag == other.flag) {
					this.mUnionView = other;
				}
			}
		}
	}
	
	public boolean isValid() {
		if(this.getVisibility() == View.GONE || !this.isShown()){
			return true;
		}
		String text = this.getText().toString();
		
		if (TextUtils.isEmpty(text)) {
			msg = mEmptyErrorTip;
			validErrorType = ValidErrorType.EmptyError;
			return false;
		} 
		
		switch(validType) {
		case Phone:
			if (!ValidateUtil.isValidPhone(text)) {
				msg = mFormatErrorTip;
				validErrorType = ValidErrorType.PhoneFormatError;
				return false;
			}
			break;
		case Password:
			if (!ValidateUtil.isValidPassword(text)) {
				msg = mFormatErrorTip;
				validErrorType = ValidErrorType.PasswordFormatError;
				return false;
			} else if (mUnionView != null && !TextUtils.isEmpty(mUnionView.getText().toString()) &&
					!TextUtils.equals(mUnionView.getText().toString(), text)) {
				msg = mUnionErrortip;
				validErrorType = ValidErrorType.UnionDifferentError;
				return false;
			}
			break;
		case Name:
			if (TextUtils.isEmpty(text.trim()) || !ValidateUtil.isValidName(text.trim())) {
				msg = mFormatErrorTip;
				validErrorType = ValidErrorType.NameFormatError;
				return false;
			}
			break;
		default:
		}
		
		return true;
	}
	
	/**
	 * isValid 的简化版，仅返回当前输入框内输入合不合法
	 * @return
	 */
	public boolean simpleCheck() {
		
		String text = this.getText().toString();
		
		if (TextUtils.isEmpty(text)) {
			return false;
		} 
		
		switch(validType) {
		case Phone:
			if (!ValidateUtil.isValidPhone(text)) {
				return false;
			}
			break;
		case Password:
			if (!ValidateUtil.isValidPassword(text)) {
				return false;
			}
			break;
		case Name:
			if (TextUtils.isEmpty(text.trim()) || !ValidateUtil.isValidName(text.trim())) {
				return false;
			}
			break;
		default:
		}
		
		return true;
	}
}

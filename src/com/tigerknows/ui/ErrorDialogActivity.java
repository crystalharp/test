/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.ui;


import com.tigerknows.R;
import com.tigerknows.util.Utility;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ErrorDialogActivity extends Activity{
    
    public static final String ERROR_MSG = "tk_error_msg";
    
	private Dialog dialog=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent=getIntent();
		String errorMsg=intent.getStringExtra(ERROR_MSG);
		
		final Activity instance=this;
		dialog = Utility.showNormalDialog(this, 
		        getString(R.string.prompt), 
		        errorMsg, 
		        getString(R.string.confirm), 
		        null,
		        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        instance.finish();
                    }
                });
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(dialog!=null && dialog.isShowing()){
			dialog.dismiss();
		}
		
	}
	
}

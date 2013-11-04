/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.ui.common;


import com.tigerknows.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ExceptionDialogActivity extends Activity{
	private AlertDialog dialog=null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Intent intent=getIntent();
		String errorMsg=intent.getStringExtra(this.getPackageName()+".errorMsg");
		final String level=intent.getStringExtra(this.getPackageName()+".level");
		
		final Activity instance=this;
		//if(level.equals("Fatal")){
			//((Activity)instance).setContentView(new TextView(instance));
		//}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//if(level.equals("Fatal")) builder.setTitle("Fatal:");
		builder.setTitle(level);
	    builder.setMessage(errorMsg).setCancelable(false).setNegativeButton(instance.getString(R.string.ok),
	            new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog,int id) {
	                    dialog.cancel();
	                    //if(level.equals("Fatal")){
	                    	instance.finish();
	                    //}
	                }
	            });
	    dialog=builder.create();
	    dialog.show();
		
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(dialog!=null && dialog.isShowing()){
			dialog.dismiss();
		}
		
	}
	
}

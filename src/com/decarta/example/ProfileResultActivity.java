/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileResultActivity extends Activity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		TextView tv=new TextView(this);
		String profile=getIntent().getStringExtra(getPackageName()+".profile");
		tv.setText(profile);
		setContentView(tv);
	}
}

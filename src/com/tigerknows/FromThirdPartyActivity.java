package com.tigerknows;

import com.tigerknows.Sphinx;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class FromThirdPartyActivity extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        if (intent != null) {
            String scheme = intent.getScheme();
            if ("tigerknows".equals(scheme)) {
                Intent sphinx = (Intent) intent.clone();
                sphinx.setClass(getBaseContext(), Sphinx.class);
                sphinx.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(sphinx);
            }
        }
        finish();
    }
}
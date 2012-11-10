package com.decarta.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.Toast;

import com.decarta.CONFIG;
import com.tigerknows.R;
import com.tigerknows.TKConfig;

public class ConfigActivity extends Activity {
	private EditText tileThreadCount=null;
	private CheckBox snapToCloestLevel=null;
	private EditText fadingTime=null;
	private EditText border=null;
	private CheckBox drawByOpengl=null;
	
	private Button ok=null;
	private Button default_conf=null;
	
	private Tokenizer tokenizer=null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config);
		
		tokenizer=new Tokenizer() {
			@Override
			public CharSequence terminateToken(CharSequence text) {
				return text + " ";
			}

			@Override
			public int findTokenStart(CharSequence text, int cursor) {
				return 0;
			}

			@Override
			public int findTokenEnd(CharSequence text, int cursor) {
				return text.length();
			}
		};
		tileThreadCount=(EditText)findViewById(R.id.tile_thread_count);
		tileThreadCount.setText(""+CONFIG.TILE_THREAD_COUNT);
		snapToCloestLevel=(CheckBox)findViewById(R.id.snap_to_closest_zoom_level);
		snapToCloestLevel.setChecked(CONFIG.SNAP_TO_CLOSEST_ZOOMLEVEL);
		fadingTime=(EditText)findViewById(R.id.fading_time);
		fadingTime.setText(""+CONFIG.FADING_TIME);
		border=(EditText)findViewById(R.id.border);
		border.setText(""+CONFIG.BORDER);
		drawByOpengl=(CheckBox)findViewById(R.id.draw_by_opengl);
		drawByOpengl.setChecked(CONFIG.DRAW_BY_OPENGL);
		
		ok=(Button)findViewById(R.id.config_ok);
		default_conf=(Button)findViewById(R.id.config_default);
				
		ok.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finishInput();
			}
		});
		
		default_conf.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {				
				tileThreadCount.setText(""+TKConfig.TILE_THREAD_COUNT_default);
				snapToCloestLevel.setChecked(TKConfig.SNAP_TO_CLOSEST_ZOOMLEVEL_default);
				fadingTime.setText(""+TKConfig.FADING_TIME_default);
				border.setText(""+TKConfig.BORDER_default);
				drawByOpengl.setChecked(TKConfig.DRAW_BY_OPENGL_default);
				
			}
		});
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
	private void finishInput(){		
			Intent intent=new Intent();
			
			String tileThreadCountStr=tileThreadCount.getText().toString().trim();
			try{
				intent.putExtra(getPackageName()+".tile_thread_count",Long.parseLong(tileThreadCountStr));
			}catch(Exception e){
				Toast.makeText(this, R.string.input_valid_tile_thread_count, 500).show();
			}
			
			intent.putExtra(getPackageName()+".snap_to_closest_zoomlevel",snapToCloestLevel.isChecked());
			
			String fadingTimeStr=fadingTime.getText().toString().trim();
			try{
				intent.putExtra(getPackageName()+".fading_time",Long.parseLong(fadingTimeStr));
			}catch(Exception e){
				Toast.makeText(this, R.string.input_valid_fading_time, 500).show();
			}
			
			String borderStr=border.getText().toString().trim();
			try{
				intent.putExtra(getPackageName()+".border",Long.parseLong(borderStr));
			}catch(Exception e){
				Toast.makeText(this, R.string.input_valid_border, 500).show();
			}
			
			intent.putExtra(getPackageName()+".draw_by_opengl",drawByOpengl.isChecked());
			
			setResult(Activity.RESULT_OK, intent);
			finish();
		
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}

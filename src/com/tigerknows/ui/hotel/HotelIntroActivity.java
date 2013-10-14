package com.tigerknows.ui.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;

public class HotelIntroActivity extends BaseActivity {
    
    public static final String EXTRA_NAME = "EXTRA_NAME";
    
    public static final String EXTRA_LONG_DESCRIPTION = "EXTRA_LONG_DESCRIPTION";
    
    public static final String EXTRA_ROOM_DESCRIPTION = "EXTRA_ROOM_DESCRIPTION";
    
    public static final String EXTRA_SERVICE = "EXTRA_SERVICE";

    TextView hotelNameTxv;
    TextView longDescriptionTxv;
    TextView roomDescriptionTxv;
    TextView hotelServiceTxv;
    View longDescriptionBlock;
    View roomDescriptionBlock;
    View hotelServiceBlock;
    ScrollView hotelScrollView;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActionTag = ActionLog.HotelIntro;
		
		setContentView(R.layout.hotel_intro);
		
		findViews();
		setListener();
		
		Intent intent = getIntent();

        mTitleBtn.setText(getString(R.string.hotel_description));
        
        hotelNameTxv.setText(intent.getStringExtra(EXTRA_NAME));
        hotelScrollView.scrollTo(0, 0);
        String longDescription = intent.getStringExtra(EXTRA_LONG_DESCRIPTION);
        String roomDescription = intent.getStringExtra(EXTRA_ROOM_DESCRIPTION);
        String hotelService = intent.getStringExtra(EXTRA_SERVICE);
        if (longDescription == null){
            longDescriptionBlock.setVisibility(View.GONE);
        } else {
            longDescriptionTxv.setText(longDescription);
            longDescriptionBlock.setVisibility(View.VISIBLE);
        }
        if (roomDescription == null) {
            roomDescriptionBlock.setVisibility(View.GONE);
        } else {
            roomDescriptionBlock.setVisibility(View.VISIBLE);
            roomDescriptionTxv.setText(roomDescription);
        }
        if (hotelService == null) {
            hotelServiceBlock.setVisibility(View.GONE);
        } else {
            hotelServiceBlock.setVisibility(View.VISIBLE);
            hotelServiceTxv.setText(hotelService);
        }
	}
	
	protected void findViews() {
	    super.findViews();
		hotelNameTxv = (TextView) findViewById(R.id.hotel_head);
		longDescriptionTxv = (TextView) findViewById(R.id.hotel_long_description);
		roomDescriptionTxv = (TextView) findViewById(R.id.hotel_room_description);
		hotelServiceTxv = (TextView) findViewById(R.id.hotel_service);
		longDescriptionBlock = findViewById(R.id.hotel_long_desc_block);
		roomDescriptionBlock = findViewById(R.id.hotel_room_desc_block);
		hotelServiceBlock = findViewById(R.id.hotel_service_block);
		hotelScrollView = (ScrollView) findViewById(R.id.hotel_scrollView);
	}
	 
}
package com.tigerknows.view.user;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.Button;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;

public class CountDownButton extends Button {

	private CountDown mCountDown;
	
	private Handler mHandler;
	
	private String coundDownFinishText;
	
	private boolean isTimerRunning = false;
	
	private Context mContext;
	
	public CountDownButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		
		mContext = context;
		coundDownFinishText = context.getString(R.string.request_validcode_retry);
		
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				if (msg.arg1 == 2) {
					setEnabled(true);
				}
				setText((String)msg.obj);
				super.handleMessage(msg);
			}
			
		};
		
	}

	public void startCountDown() {
		setEnabled(false);
		mCountDown = new CountDown();
	}
	
	public void reset(String text) {
		if (isTimerRunning) {
			mCountDown.timer.cancel();
			isTimerRunning = false;
		}


		Message message = new Message();
		message.arg1=2;
		message.obj = text;
		mHandler.sendMessage(message);
	}
	
	public class CountDown {
		// 计时器
		Timer timer;
		
		private CountDown() {
			super();
			// TODO Auto-generated constructor stub
			timer = new Timer();
			timer.schedule(new CountDownTask(), 0, 1000);
			isTimerRunning = true;
		}
		
		class CountDownTask extends TimerTask {

			// 倒计计数次数
			int times = 60;
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (times >= 0) {
					Message message = new Message();
					message.obj =  mContext.getString(R.string.coundown_valinum, String.valueOf(times));
					mHandler.sendMessage(message);
					LogWrapper.d("CountDown", String.valueOf(times));
					times -= 1;
				} else {
					timer.cancel();
					isTimerRunning = false;
					Message message = new Message();
					message.arg1=2;
					message.obj = coundDownFinishText;
					mHandler.sendMessage(message);
				}
			}
			
		}
	}
}

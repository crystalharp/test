package com.tigerknows.widget;

import com.tigerknows.common.ActionLog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class BladeView extends View {
	
	private OnBladeItemClickListener mOnItemClickListener;
	
	private String mActionTag;
	
	String[] b = { "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
			"Y", "Z" };
	
	int choose = -1;
	Paint paint = new Paint();
	boolean showBkg = false;
	private TextView mPopupText;
	private Handler handler = new Handler();

	public BladeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public BladeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BladeView(Context context) {
		super(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (showBkg) {
			canvas.drawColor(Color.parseColor("#00000000"));
		}

		int height = getHeight();
		int width = getWidth();
		int singleHeight = height / b.length;
		for (int i = 0; i < b.length; i++) {
			paint.setColor(Color.rgb(0x32, 0x32, 0x32));
//			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setFakeBoldText(true);
			paint.setAntiAlias(true);
			paint.setTextSize(16);
			if (i == choose) {
				paint.setColor(Color.parseColor("#3399ff"));
			}
			float xPos = width / 2 - paint.measureText(b[i]) / 2;
			float yPos = singleHeight * i + singleHeight;
			canvas.drawText(b[i], xPos, yPos, paint);
			paint.reset();
		}

	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final float y = event.getY();
		final int oldChoose = choose;
		final int c = (int) (y / getHeight() * b.length);

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			showBkg = true;
			if (oldChoose != c) {
				if (c > 0 && c < b.length) {
					performItemClicked(c);
					choose = c;
					invalidate();
				}
			}

			break;
		case MotionEvent.ACTION_MOVE:
			if (oldChoose != c) {
				if (c > 0 && c < b.length) {
					performItemClicked(c);
					choose = c;
					invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			showBkg = false;
			choose = -1;
			dismissPopup();
			invalidate();
			break;
		}
		return true;
	}

	private void showPopup(int item) {

		String text = "";
		if (item == 0) {
			text = "#";
		} else {
			text = Character.toString((char) ('A' + item - 1));
		}
		mPopupText.setText(text);
		
		mPopupText.setVisibility(View.VISIBLE);
	}

	private void dismissPopup() {
		handler.postDelayed(dismissRunnable, 800);
	}

	Runnable dismissRunnable = new Runnable() {

		@Override
		public void run() {
			mPopupText.setVisibility(View.GONE);
		}
	};

	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	public void setOnBladeItemClickListener(OnBladeItemClickListener listener) {
		mOnItemClickListener = listener;
	}

	private void performItemClicked(int item) {
		if (mOnItemClickListener != null) {
		    ActionLog.getInstance(getContext()).addAction(mActionTag + ActionLog.FilterAreaChar, b[item]);
			mOnItemClickListener.onItemClick(b[item]);
			showPopup(item);
		}
	}

	public interface OnBladeItemClickListener {
		void onItemClick(String s);
	}

	public void setActionTag(String actionTag){
		this.mActionTag = actionTag;
	}

	public void setPopupText(TextView popupText){
		this.mPopupText = popupText;
	}
}

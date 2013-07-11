
package com.tigerknows.view.discover;


import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Gallery;

/**
 * ViewPager navigation
 * 
 * @author Grubby
 */
public class DiscoverTopIndicator extends View {
    private static final String TAG = "DiscoverTopIndicator";

    int[] mFocusedTextColor;

    int[] mUnfocusedTextColor;

    Context context;

    Gallery mViewPager;

    private String leftStr, centerStr, rightStr;

    private String leftLeftStr, rightRightStr;
    
    private float fontSize;
    
    private int width = 0, textWidth = 0;
    
    private int leftOffset = 0, rightOffset = 0;
    
    Paint mLeftPaint = new Paint();   
    Paint mCenterPaint = new Paint();   
    Paint mRightPaint = new Paint();   

    public DiscoverTopIndicator(Context context) {   
        super(context);   
        init(context);
    }   
       
    public DiscoverTopIndicator(Context context, AttributeSet attrs){   
        super(context, attrs);   
        init(context);
    }   
    
    private void init(Context context) {
        this.context = context;
        fontSize = Globals.g_metrics.scaledDensity*22;
        mLeftPaint.setTextSize(fontSize);
        mLeftPaint.setColor(Color.WHITE);
        mLeftPaint.setAntiAlias(true);  
        mLeftPaint.setDither(true); 
        mCenterPaint.setTextSize(fontSize);
        mCenterPaint.setColor(Color.WHITE);
        mCenterPaint.setAntiAlias(true);  
        mCenterPaint.setDither(true); 
        mRightPaint.setTextSize(fontSize);
        mRightPaint.setColor(Color.WHITE);
        mRightPaint.setAntiAlias(true);  
        mRightPaint.setDither(true); 
        width = Globals.g_metrics.widthPixels;
        textWidth = (int)mLeftPaint.measureText(this.context.getString(R.string.tuangou));
        mFocusedTextColor = new int[] {255, 255, 255};
        mUnfocusedTextColor = new int[] {255, 191, 100};
        updateColor(0);
    }
       
    public void onDraw(Canvas canvas){   
        super.onDraw(canvas);
        int discoverCategoryWidth = (int)(width * DiscoverFragment.DISCOVER_WIDHT_RATE);
        if (leftStr != null) {
            if (Math.abs(leftOffset) > (discoverCategoryWidth/2-textWidth/2)) {
                if (rightRightStr != null) {
                    canvas.drawText(rightRightStr, (width)-(Math.abs(leftOffset) - (discoverCategoryWidth/2-textWidth/2)), fontSize, mLeftPaint);
                }
            } else {
                canvas.drawText(leftStr, rightOffset+leftOffset, fontSize, mLeftPaint);   
            }
        }
        if (centerStr != null) {
            canvas.drawText(centerStr, (width/2)+(leftOffset+rightOffset)-(textWidth/2), fontSize, mCenterPaint);  
        }
        if (rightStr != null) {
            if (rightOffset > (discoverCategoryWidth/2-textWidth/2)) {
                if (leftLeftStr != null) {
                    canvas.drawText(leftLeftStr, (rightOffset-(discoverCategoryWidth/2-textWidth/2))-textWidth, fontSize, mRightPaint);
                }
            } else {
                canvas.drawText(rightStr, (width-textWidth)+leftOffset+rightOffset, fontSize, mRightPaint);   
            }
        }
    }   

    public void setGallery(Gallery mViewPager) {
        this.mViewPager = mViewPager;
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
         LogWrapper.i(TAG, "position: " + position + " positionOffset:" + positionOffset + " positionOffsetPixels:" + positionOffsetPixels);
        if (0 == position) {
            leftOffset = 0;
            rightOffset = 0;
            updateColor(0);
        } else if (position < 0) {
            leftOffset = (int) (-positionOffsetPixels);
            rightOffset = 0;
            updateColor(positionOffset);
        } else {
            leftOffset = 0;
            rightOffset = (int) (positionOffsetPixels);
            updateColor(positionOffset);
        }
        System.out.println("Top indicator OnPageScroll. leftOffset: " + leftOffset + " rightOffset: " + rightOffset);
        invalidate();
    }

    public void onPageSelected(int position, String leftStr, String centerStr, String rightStr, String leftLeftStr, String rightRightStr) {
        this.leftStr = leftStr;
        this.centerStr = centerStr;
        this.rightStr = rightStr;
        this.leftLeftStr = leftLeftStr;
        this.rightRightStr = rightRightStr;
        leftOffset = 0;
        rightOffset = 0;
        updateColor(0);
        System.out.println("On page selected: " + toString());
        invalidate();
    }

    void updateColor(float offset) {
        float fraction = offset;
        fraction = Math.min(1, fraction);
        int r = (int) (mUnfocusedTextColor[0] * fraction + mFocusedTextColor[0] * (1 - fraction));
        int g = (int) (mUnfocusedTextColor[1] * fraction + mFocusedTextColor[1] * (1 - fraction));
        int b = (int) (mUnfocusedTextColor[2] * fraction + mFocusedTextColor[2] * (1 - fraction));
        mCenterPaint.setColor(Color.argb(255, r, g, b));
        r = (int) (mFocusedTextColor[0] * fraction + mUnfocusedTextColor[0] * (1 - fraction));
        g = (int) (mFocusedTextColor[1] * fraction + mUnfocusedTextColor[1] * (1 - fraction));
        b = (int) (mFocusedTextColor[2] * fraction + mUnfocusedTextColor[2] * (1 - fraction));
        if (rightOffset != 0) {
            mLeftPaint.setColor(Color.argb(255, r, g, b));
        } else {
            mLeftPaint.setColor(Color.argb(255, mUnfocusedTextColor[0], mUnfocusedTextColor[1],
                    mUnfocusedTextColor[2]));
        }
        if (leftOffset != 0) {
            mRightPaint.setColor(Color.argb(255, r, g, b));
        } else {
            mRightPaint.setColor(Color.argb(255, mUnfocusedTextColor[0], mUnfocusedTextColor[1],
                    mUnfocusedTextColor[2]));
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getMeasuredLength(widthMeasureSpec, true), getMeasuredLength(heightMeasureSpec, false));
    }
    
    private int getMeasuredLength(int length, boolean isWidth) {
        int specMode = MeasureSpec.getMode(length);
        int specSize = MeasureSpec.getSize(length);
        int size;
        int padding = isWidth ? getPaddingLeft() + getPaddingRight() : getPaddingTop()
                + getPaddingBottom();
        if (specMode == MeasureSpec.EXACTLY) {
            size = specSize;
        } else {
            size = isWidth ? Globals.g_metrics.widthPixels : (int)(-mLeftPaint.ascent() + mLeftPaint.descent()) + padding;
            if (specMode == MeasureSpec.AT_MOST) {
                size = Math.min(size, specSize);
            }
        }
        return size;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        float x = event.getX();
        if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            if (x > 0 && x < textWidth) {
                mViewPager.setSelection(mViewPager.getSelectedItemPosition()-1, true);
            } else if (x > width - textWidth && x < width) {
                mViewPager.setSelection(mViewPager.getSelectedItemPosition()+1, true);
            }
        }
        return super.onTouchEvent(event);
    }
    
    public int getTextWidth() {
        return textWidth;
    }

	@Override
	public String toString() {
		return "DiscoverTopIndicator [leftStr=" + leftStr + ", centerStr="
				+ centerStr + ", rightStr=" + rightStr + ", leftLeftStr="
				+ leftLeftStr + ", rightRightStr=" + rightRightStr + ", width="
				+ width + ", textWidth=" + textWidth + ", leftOffset="
				+ leftOffset + ", rightOffset=" + rightOffset + "]";
	}
    
    
}

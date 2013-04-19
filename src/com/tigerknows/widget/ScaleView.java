
package com.tigerknows.widget;

import com.decarta.CONFIG;
import com.tigerknows.R;
import com.tigerknows.util.Utility;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * 比例尺控件
 * @author pengwenyue
 *
 */
public class ScaleView extends View {

    private static float STROKE_WIDTH = 2.0f;
    
    private Paint mLinePaint;
    
    private Paint mTextPaint;
    
    private int mTextAscent;
    
    private int mVerticalLineHeight = 8;
    
    private int mLineAndTextPadding = 4;
    
    private float metersPerPixelAtZoom = -1;
    
    private int zoom = -1;
    
    private String meter;
    
    private String km;

    private static int[] GENERALIZE_ZOOM_LEVEL_METERS={
        100000,  //zoom 0
        100000,  //zoom 1
        100000,  //zoom 2
        100000,  //zoom 3
        100000,  //zoom 4
        100000,  //zoom 5
        50000,  //zoom 6
        25000,  //zoom 7
        20000,  //zoom 8
        10000,  //zoom 9
        5000, //zoom 10
        2000, //zoom 11
        1000, //zoom 12
        500, //zoom 13
        200, //zoom 14
        100, //zoom 15
        50, //zoom 16
        25, //zoom 17
        10, //zoom 18
        10, //zoom 19
        10  //zoom 20
    };

    public ScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        meter = context.getString(R.string.meter);
        km = context.getString(R.string.km);
        
        initView();
    }

    private void initView() {
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setDither(true);
        
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true); //设置是否使用抗锯齿功能，会消耗较大资源，绘制图形速度会变慢。  
        mTextPaint.setDither(true); //设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰  
        mTextPaint.setTypeface(Typeface.DEFAULT); //设置字体Typeface包含了字体的类型，粗细，还有倾斜、颜色
        mTextPaint.setStrokeWidth(4f); //设置描边的宽度
        mTextPaint.setTextSize(16);
        
        setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (metersPerPixelAtZoom <= 0 || zoom < CONFIG.ZOOM_LOWER_BOUND || zoom > CONFIG.ZOOM_UPPER_BOUND) {
            return;
        }
        
        float scale =GENERALIZE_ZOOM_LEVEL_METERS[zoom]/metersPerPixelAtZoom;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(STROKE_WIDTH*2);
        // 左竖线
        canvas.drawLine(paddingLeft + STROKE_WIDTH/2, paddingTop, paddingLeft + STROKE_WIDTH/2, mVerticalLineHeight, mLinePaint);
        // 右竖线
        canvas.drawLine(scale + paddingLeft - STROKE_WIDTH/2, paddingTop, scale + paddingLeft - STROKE_WIDTH/2, mVerticalLineHeight, mLinePaint);
        // 直横线
        canvas.drawLine(paddingLeft, mVerticalLineHeight, scale+paddingLeft, mVerticalLineHeight, mLinePaint);

        mLinePaint.setColor(Color.BLACK);
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setStrokeWidth(STROKE_WIDTH);
        // 左竖线
        canvas.drawLine(paddingLeft + STROKE_WIDTH/2, paddingTop, paddingLeft + STROKE_WIDTH/2, mVerticalLineHeight, mLinePaint);
        // 右竖线
        canvas.drawLine(scale + paddingLeft - STROKE_WIDTH/2, paddingTop, scale + paddingLeft - STROKE_WIDTH/2, mVerticalLineHeight, mLinePaint);
        // 直横线
        canvas.drawLine(paddingLeft, mVerticalLineHeight, scale+paddingLeft, mVerticalLineHeight, mLinePaint);

        int distance = GENERALIZE_ZOOM_LEVEL_METERS[zoom];
        String text;
        if (distance >= 1000) {
            if (distance % 1000 > 0) {
                distance -= (distance%100);
                text = ((float)distance / 1000 ) + km;
            } else {
                text = (distance / 1000 ) + km;
            }
        } else {
            text = distance + meter;
        }
        
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setStyle(Paint.Style.STROKE);
        canvas.drawText(text, paddingLeft, - mTextAscent + mVerticalLineHeight + mLineAndTextPadding + paddingTop, mTextPaint);

        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, paddingLeft, - mTextAscent + mVerticalLineHeight + mLineAndTextPadding + paddingTop, mTextPaint);
    }
    
    /**
     * @see android.view.View#measure(int, int)
     */
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        if (widthMode != MeasureSpec.EXACTLY) {
//            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
//        }
//
//        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        if (heightMode != MeasureSpec.EXACTLY) {
//            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
//        }
//    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {

        final int width = MeasureSpec.getSize(measureSpec);
        final int widthMode = MeasureSpec.getMode(measureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        return width;
    }

    /**
     * Determines the height of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mTextAscent = (int)mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = mVerticalLineHeight + mLineAndTextPadding + (int)(-mTextAscent + mTextPaint.descent()) + getPaddingTop() + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    public void setMetersPerPixelAtZoom(float metersPerPixelAtZoom, int zoom) {
        this.metersPerPixelAtZoom = metersPerPixelAtZoom;
        this.zoom = zoom;
        postInvalidate();
    }
}

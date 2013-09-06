/**
 * 
 */
package com.tigerknows.map.label;

import static android.opengl.GLES10.glDeleteTextures;

import java.util.Iterator;
import java.util.LinkedHashMap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.GLES10;

import com.decarta.Globals;
import com.decarta.android.map.TilesView.Texture;
import com.decarta.android.util.*;
import com.tigerknows.map.Grid;
import com.tigerknows.map.MapWord;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * @author chenming
 *
 */
//struct _tk_label {
//    char *name;
//    tk_color_t text_color;
//    tk_color_t bg_color;
//    int icon_id;
//    tk_point_t *points;
//    int point_num;
//    int priority;
//    int type;
//    int font_size;
//    int point_start_idx;
//};
public abstract class Label {
	public static final int LABEL_STATE_PRIMITIVE = 0;
	public static final int LABEL_STATE_SHOWN = 1;
	public static final int LABEL_STATE_FADE_IN = 2;
	public static final int LABEL_STATE_FADE_OUT = 3;
	public static final int LABEL_STATE_CANT_BE_SHOWN = 4;
	public static final int LABEL_STATE_OUT_BOUND = 5;
	public static final int LABEL_STATE_WAITING = 6;
	public static final int LABEL_STATE_SHOW_ICON = 7;
	public static final float LABEL_FADING_DURATION = 200;
	public static final int INDEX_AREA = 35;
	public static int TK_LABEL_BOUND_SIZE = 8;// in fact 8 * density
	
	public int x;
	public int y;
	public int z;
	
	public String name;
	public int pointNum;
	public int color;
	public int bgColor;
	public int fontSize;
	
    public int type;
    public int priority;
    
    public double fadingStartTime;
    public float opacity;
    
    public int state;
    
    protected static Paint tilePText;
    
    public static void init() {
    	Label.TK_LABEL_BOUND_SIZE = (int) (8 * Globals.g_metrics.density);
        if(Label.TK_LABEL_BOUND_SIZE > 8) 
        	Label.TK_LABEL_BOUND_SIZE = 16;
        else
        	Label.TK_LABEL_BOUND_SIZE = 8;
    	
    	tilePText = new Paint();
    	tilePText.setAntiAlias(true); //设置是否使用抗锯齿功能，会消耗较大资源，绘制图形速度会变慢。  
    	//    tilePText.setDither(true); //设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰  
    	tilePText.setTypeface(Typeface.DEFAULT_BOLD); //设置字体Typeface包含了字体的类型，粗细，还有倾斜、颜色
//    	tilePText.setTypeface(Typeface.DEFAULT)
    	float strokeWidth = 2f * Globals.g_metrics.density;
//    	if(strokeWidth > 2f) 
//    		strokeWidth = 4f;
//    	else 
//    		strokeWidth = 2f;
    	tilePText.setStrokeWidth(strokeWidth); //设置描边的宽度
    }
    private static final int MAX_TEXT_LENGTH = 32;
    private static final int MAX_WORD_SIZE = 512;
    
    public static class IntegerRef {
    	public int value;
    	public IntegerRef(int value) {
    		this.value = value;
    	}
    }
    
    private static LinkedHashMap<XYInteger,Bitmap> textBitmapPool = new LinkedHashMap<XYInteger,Bitmap>(MAX_TEXT_LENGTH * 2,0.75f,true) {
        private static final long serialVersionUID = 1L;
        @Override
        protected boolean removeEldestEntry(
                java.util.Map.Entry<XYInteger, Bitmap> eldest) {
            if(size() > MAX_TEXT_LENGTH){
                Bitmap bm =eldest.getValue();
                if(bm!=null && bm.isRecycled() == false){
                    bm.recycle();
                }
                remove(eldest.getKey());
            }
            return false;
        }
    };
    
    protected static LinkedHashMap<String, Texture> textTexturePool = new LinkedHashMap<String, Texture>(MAX_TEXT_LENGTH * 2,0.75f,true) {
        private static final long serialVersionUID = 1L;
        @Override
        protected boolean removeEldestEntry(
                java.util.Map.Entry<String, Texture> eldest) {
            if(size()>MAX_WORD_SIZE){
                Texture texture=eldest.getValue();
                if(texture != null && texture.textureRef!=0){
                    IntBuffer textureRefBuf=IntBuffer.allocate(1);
                    textureRefBuf.clear();
                    textureRefBuf.put(0,texture.textureRef);
                    textureRefBuf.position(0);
                    glDeleteTextures(1, textureRefBuf);
                }
                remove(eldest.getKey());
            }
            return false;
        }
    };
    
    protected XYFloat calcTextRectSize(String text) {
    	XYFloat size = new XYFloat(0, 0);
    	tilePText.setTextSize(fontSize);
        int nameLength = text.length();
        if(nameLength >= 8) {
            String[] names = new String[2];
        	names[0] = text.substring(0, nameLength / 2);
        	names[1] = text.substring(nameLength / 2);
        	size.x = Math.max(tilePText.measureText(names[0]), tilePText.measureText(names[1]));
        	size.y = (2 * (-tilePText.ascent()+tilePText.descent()));
        }
        else {
        	size.x = tilePText.measureText(text);
        	size.y = (-tilePText.ascent()+tilePText.descent());
        }
    	return size;
    }
    
    protected Bitmap getTextBitmap(String text, int backGroundIdx) {
//    	LogWrapper.i("Label", "to generate text texture:" + text);
        tilePText.setTextSize(fontSize);
        String[] names = new String[2];
        names[0] = text;
        int nameLength = text.length();
        int seg = 1;
        float width, height, orgWidth, orgHeight, lineHeight;
        if(nameLength >= 8) {
        	names[0] = text.substring(0, nameLength / 2);
        	names[1] = text.substring(nameLength / 2);
        	seg = 2;
        	width=Math.max(tilePText.measureText(names[0]), tilePText.measureText(names[1]));
        	orgWidth = (int)width;
        	lineHeight = -tilePText.ascent()+tilePText.descent();
        	height = 2 * lineHeight;
        	orgHeight = (int)height;
        }
        else {
        	width = tilePText.measureText(text);
        	orgWidth = (int)width;
        	height = (-tilePText.ascent()+tilePText.descent());
        	lineHeight = height;
        	orgHeight = (int)height;
        }
        float x = 0;//todo: 寻找最优的缩进
        float y = fontSize;

        int pw2width = Util.getPower2(width);
        int pw2height = Util.getPower2(height);
        XYInteger size = new XYInteger(pw2width, pw2height);
        Bitmap bitmap = textBitmapPool.get(size);
        if (bitmap != null) {
            bitmap.eraseColor(0);
        } else {
            Bitmap.Config config = Bitmap.Config.ARGB_4444;
            bitmap = Bitmap.createBitmap(pw2width, pw2height, config);
            textBitmapPool.put(size, bitmap);
        }
        Canvas canvas = new Canvas(bitmap);
        if (backGroundIdx >= 0) {
			float padding = Globals.g_metrics.scaledDensity * 2;
			NinePatchDrawable drawable = SingleRectLabel
					.getNinePatchDrawable(backGroundIdx);
			if (drawable != null) {
				drawable.setBounds((int) (x), (int) (y - orgHeight),
						(int) (x + orgWidth), (int) (y + 2 * padding));
				drawable.draw(canvas);
			}
		}
        else {
        	tilePText.setColor(0xffffffff); //? ?RGB
        	tilePText.setStyle(Style.STROKE);
            for (int i = 0; i < seg; ++i) {
            	canvas.drawText(names[i], x, y + lineHeight * i, tilePText);
            }
        }
        tilePText.setColor(color | 0xff000000);
        tilePText.setStyle(Style.FILL);
        for (int i = 0; i < seg; ++i) {
        	canvas.drawText(names[i], x, y + lineHeight * i, tilePText);
        }
        return bitmap;
    }
    
    public static void clearTextTexture() {
        Iterator<Texture> iterator4=textTexturePool.values().iterator();
        while(iterator4.hasNext()){
            Texture texture = iterator4.next();
            if (texture != null && texture.textureRef != 0) {
                int textureRef=texture.textureRef;
                IntBuffer textureRefBuf=IntBuffer.allocate(1);
    			textureRefBuf.clear();
    			textureRefBuf.put(0,textureRef);
    			textureRefBuf.position(0);
    			GLES10.glDeleteTextures(1, textureRefBuf);
            }
        }
        textTexturePool.clear();
    }
    
    public static void clearTextBitmap() {
        Iterator<Bitmap> iterator6=textBitmapPool.values().iterator();
        while(iterator6.hasNext()){
            Bitmap bm = iterator6.next();
            if (bm != null && bm.isRecycled() == false) {
                bm.recycle();
            }
        }
        textBitmapPool.clear();
    }
    
    public abstract int draw(XYInteger center, XYZ centerXYZ, XYFloat centerDelta, 
    		float rotation, float sinRot, float cosRot, float scale, Grid grid, 
    		ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer, boolean needGenTexture, IntegerRef leftCountToDraw);
    
}

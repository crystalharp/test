/**
 * 
 */
package com.tigerknows.map.label;

import static android.opengl.GLES10.glDeleteTextures;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.GLES10;
import android.opengl.GLUtils;

import com.decarta.CONFIG;
import com.decarta.Globals;
import com.decarta.android.map.TilesView.Texture;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYZ;
import com.tigerknows.R;
import com.tigerknows.android.app.TKApplication;
import com.tigerknows.map.Grid;
import com.tigerknows.map.RectInteger;
import com.tigerknows.util.XYInteger;

/**
 * @author chenming
 *
 */
public class SingleRectLabel extends Label {
	
    public static final int STYLE_ICON_ON_LEFT = 0;
    public static final int STYLE_ICON_ON_RIGHT = 1;
    public static final int STYLE_NO_ICON_WITH_BACKGROUND = 2;
    public static final int STYLE_NO_ICON_WITHOUT_BACKGROUND = 3;

    public static final int[] RESOURCE_ID = {
    	R.drawable.icon, 
    	R.drawable.ic_map_train_station, 
    	R.drawable.ic_map_bus_station,
        R.drawable.ic_map_subway, 
        R.drawable.ic_map_airport2, 
        R.drawable.ic_map_hospital,
        R.drawable.ic_map_port, 
        R.drawable.ic_map_hotel, 
        R.drawable.ic_map_government1,
        R.drawable.ic_map_government2, 
        R.drawable.ic_map_market, 
        R.drawable.ic_map_building,
        R.drawable.ic_map_school, 
        R.drawable.ic_map_stadium, 
        R.drawable.ic_map_park,
        R.drawable.ic_map_museum, 
        R.drawable.ic_map_park_2, 
        R.drawable.ic_map_theater,
        R.drawable.ic_map_culture, 
        R.drawable.ic_map_library, 
        R.drawable.ic_map_bank,
        R.drawable.ic_map_tv_tower, 
        R.drawable.ic_map_residential, 
        R.drawable.ic_map_research,
        R.drawable.ic_map_hi_tech_park, 
        R.drawable.ic_map_golf, 
        R.drawable.ic_map_residential,
        R.drawable.ic_map_tv, 
        R.drawable.ic_map_capital, 
        R.drawable.icon,
        R.drawable.ic_map_provincial_capital, 
        R.drawable.ic_map_other_cities, 
        R.drawable.icon,
        R.drawable.icon, 
        R.drawable.ic_map_area, 
        R.drawable.icon
        };
    
    private static Bitmap[] BitmapPool = null;
    private static NinePatchDrawable[] NinePatchDrawablePool;
    
    public static void init(Context context) {
        if (BitmapPool != null) {
            return;
        }
        Resources resources = context.getResources();
        BitmapPool = new Bitmap[RESOURCE_ID.length];
        for(int i = 0; i < RESOURCE_ID.length; i++) {
            if (RESOURCE_ID[i] != R.drawable.icon) {
                BitmapPool[i] = BitmapFactory.decodeResource(resources, RESOURCE_ID[i]);
            }
        }
        NinePatchDrawablePool = new NinePatchDrawable[1];
        NinePatchDrawablePool[0] = (NinePatchDrawable)resources.getDrawable(R.drawable.ic_map_area);
    }
    
    public static NinePatchDrawable getNinePatchDrawable(int index) {
    	if(index < 0 || index > NinePatchDrawablePool.length) {
    		return null;
    	}
        return NinePatchDrawablePool[index];
    }
    
    public static synchronized void clearIcon() {
        //清空图片
        for(int i = 0, len = BitmapPool.length; i < len; ++i) {
        	if(BitmapPool[i] != null) {
        		BitmapPool[i].recycle();
        		BitmapPool[i] = null;
        	}
        }
    }


    
	public int iconId;
    public int style;
    public XYInteger point;
    public RectInteger rect;
    public XYInteger iconSize;
    
    /**
     * 此static的代码仅是为了避免其构造函数被优化
     */
    static {
        new SingleRectLabel("", 0, 0, 0, 0, 0, 0, null, 0, 0, 0);
    }

    public SingleRectLabel(
    		String name, 
    		int color,
    		int bgColor,
    		int iconId,
    		int fontSize,
    		int type,
    		int priority,
    		XYInteger point,
    		int tileX,
    		int tileY,
    		int zoom
    		) {
        super();
    	this.name = name;
    	this.color = color;
    	this.bgColor = bgColor;
    	this.iconId = iconId;
    	int realFontSize = (int) ((fontSize) * Globals.g_metrics.density) + 1;
    	this.fontSize = realFontSize > fontSize ? realFontSize : fontSize;
    	this.type = type;
    	this.priority = priority;
    	this.point = point;
    	this.x = tileX;
    	this.y = tileY;
    	this.z = zoom;
    	this.fadingStartTime = 0;
    	this.opacity = 0;
    	this.pointNum = 1;
    	this.state = Label.LABEL_STATE_PRIMITIVE;
        this.rect = new RectInteger();
        if (iconId == INDEX_AREA) {
        	this.style = STYLE_NO_ICON_WITH_BACKGROUND;
        	this.iconSize = null;
        }
        else if (RESOURCE_ID[iconId] == R.drawable.icon || iconId >= RESOURCE_ID.length || iconId < 0) {
        		this.style = STYLE_NO_ICON_WITHOUT_BACKGROUND;
            	this.iconSize = null;
        }
        else {
        	this.style = STYLE_ICON_ON_LEFT;
        	if(BitmapPool[iconId] == null) {
        		BitmapPool[iconId] = BitmapFactory.decodeResource(
        				TKApplication.getInstance().getApplicationContext().getResources(), RESOURCE_ID[iconId]);
        	}
        	this.iconSize = new XYInteger(BitmapPool[iconId].getWidth(), BitmapPool[iconId].getHeight());
        }
    }
    
    private void getIconSize() {
    	if(iconSize == null) {
    		Bitmap bm = BitmapPool[this.iconId];
    		iconSize = new XYInteger(bm.getWidth(), bm.getHeight());
    	}
    }
    
    private Texture getIconTexture(LinkedHashMap<Integer,Texture> mapWordIconPool) {
    	Texture texture = mapWordIconPool.get(iconId);
    	if(texture != null)
    		return texture;
    	IntBuffer bf = IntBuffer.allocate(1);
        GLES10.glGenTextures(1, bf);
        int textureRef = bf.get(0);
        
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureRef);
        try {
        	if(BitmapPool[iconId] == null) {
        		BitmapPool[iconId] = BitmapFactory.decodeResource(
        				TKApplication.getInstance().getApplicationContext().getResources(), RESOURCE_ID[iconId]);
        	}
        	Bitmap bm = BitmapPool[iconId];
            int width = Util.getPower2(bm.getWidth());
            int height = Util.getPower2(bm.getHeight());
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = Bitmap.createBitmap(width, height, config);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(bm, 0, 0, tilePText);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
            texture = new Texture();
            texture.textureRef = textureRef;
            texture.size.x = width;
            texture.size.y = height;
            if(iconSize == null) {
            	iconSize = new XYInteger(bm.getWidth(), bm.getHeight());
            }
            mapWordIconPool.put(iconId, texture);            
            return texture;
        } catch (Exception e) {
			IntBuffer textureRefBuf = IntBuffer.allocate(1);
			textureRefBuf.clear();
			textureRefBuf.put(0,textureRef);
			textureRefBuf.position(0);
			GLES10.glDeleteTextures(1, textureRefBuf);
            textureRef=0;
            return null;
        }
    }
    
    private void draw(float x, float y, float rot, float scale, int width, int height, Texture iconTexture, Texture textTexture,
    		ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer) {
    	GLES10.glPushMatrix();
    	GLES10.glTranslatef(x, y, 0);
    	if (scale != 1) {
    		GLES10.glScalef(1/scale, 1/scale, 1);
    	}
    	GLES10.glRotatef(-rot, 0, 0, 1);
    	GLES10.glEnable(GLES10.GL_BLEND);
    	GLES10.glTexEnvf(GLES10.GL_TEXTURE_ENV, GLES10.GL_TEXTURE_ENV_MODE, GLES10.GL_MODULATE);
    	GLES10.glBlendFunc(GLES10.GL_ONE, GLES10.GL_ONE_MINUS_SRC_ALPHA);
    	GLES10.glColor4f(opacity, opacity, opacity, opacity);
    	if(style == STYLE_NO_ICON_WITHOUT_BACKGROUND || style == STYLE_NO_ICON_WITH_BACKGROUND) {
        	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textTexture.textureRef);
        	x = - (width >> 1);
        	y = - (height >> 1);
        	vertexBuffer.clear();
        	vertexBuffer.put(x);
        	vertexBuffer.put(y);
        	vertexBuffer.put(x);
        	vertexBuffer.put(y + textTexture.size.y);
        	vertexBuffer.put(x + textTexture.size.x);
        	vertexBuffer.put(y);
        	vertexBuffer.put(x + textTexture.size.x);
        	vertexBuffer.put(y + textTexture.size.y);
        	TEXTURE_COORDS.position(0);
        	vertexBuffer.position(0);
        	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
    	}
    	else if (style == STYLE_ICON_ON_LEFT) {
        	float iconx = - iconSize.x >> 1, icony = - iconSize.y >> 1;
        	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, iconTexture.textureRef);
        	vertexBuffer.clear();
        	vertexBuffer.put(iconx);
        	vertexBuffer.put(icony);
        	vertexBuffer.put(iconx);
        	vertexBuffer.put(icony + iconTexture.size.y);
        	vertexBuffer.put(iconx + iconTexture.size.x);
        	vertexBuffer.put(icony);
        	vertexBuffer.put(iconx + iconTexture.size.x);
        	vertexBuffer.put(icony + iconTexture.size.y);
        	TEXTURE_COORDS.position(0);
        	vertexBuffer.position(0);
        	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
        	x = iconSize.x >> 1;
        	y = - (height >> 1);
        	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textTexture.textureRef);
        	vertexBuffer.clear();
        	vertexBuffer.put(x);
        	vertexBuffer.put(y);
        	vertexBuffer.put(x);
        	vertexBuffer.put(y + textTexture.size.y);
        	vertexBuffer.put(x + textTexture.size.x);
        	vertexBuffer.put(y);
        	vertexBuffer.put(x + textTexture.size.x);
        	vertexBuffer.put(y + textTexture.size.y);
        	TEXTURE_COORDS.position(0);
        	vertexBuffer.position(0);
        	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
    	}
    	else if (style == STYLE_ICON_ON_RIGHT) {
        	float iconx = - (iconSize.x >> 1), icony = - (iconSize.y >> 1);
        	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, iconTexture.textureRef);
        	vertexBuffer.clear();
        	vertexBuffer.put(iconx);
        	vertexBuffer.put(icony);
        	vertexBuffer.put(iconx);
        	vertexBuffer.put(icony + iconTexture.size.y);
        	vertexBuffer.put(iconx + iconTexture.size.x);
        	vertexBuffer.put(icony);
        	vertexBuffer.put(iconx + iconTexture.size.x);
        	vertexBuffer.put(icony + iconTexture.size.y);
        	TEXTURE_COORDS.position(0);
        	vertexBuffer.position(0);
        	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
        	x = - (iconSize.x >> 1) - width;
        	y = - (height >> 1);
        	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textTexture.textureRef);
        	vertexBuffer.clear();
        	vertexBuffer.put(x);
        	vertexBuffer.put(y);
        	vertexBuffer.put(x);
        	vertexBuffer.put(y + textTexture.size.y);
        	vertexBuffer.put(x + textTexture.size.x);
        	vertexBuffer.put(y);
        	vertexBuffer.put(x + textTexture.size.x);
        	vertexBuffer.put(y + textTexture.size.y);
        	TEXTURE_COORDS.position(0);
        	vertexBuffer.position(0);
        	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
    	}
    	GLES10.glPopMatrix();
    }
    
    private void drawOnlyIcon(float x, float y, RectInteger rect, float rot, float scale, Grid grid, 
    		ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer, LinkedHashMap<Integer,Texture> mapWordIconPool) {
    	if(grid.isRectOutBound(rect) || grid.isInterSectRect(rect)) {
    		return;
    	}
    	grid.addRect(rect);
    	Texture iconTexture = getIconTexture(mapWordIconPool);
    	GLES10.glPushMatrix();
    	GLES10.glTranslatef(x, y, 0);
    	if (scale != 1) {
    		GLES10.glScalef(1/scale, 1/scale, 1);
    	}
    	GLES10.glRotatef(-rot, 0, 0, 1);
    	GLES10.glTexEnvf(GLES10.GL_TEXTURE_ENV, GLES10.GL_TEXTURE_ENV_MODE, GLES10.GL_REPLACE);
    	float iconx = - (iconSize.x >> 1), icony = - (iconSize.y >> 1);
    	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, iconTexture.textureRef);
    	vertexBuffer.clear();
    	vertexBuffer.put(iconx);
    	vertexBuffer.put(icony);
    	vertexBuffer.put(iconx);
    	vertexBuffer.put(icony + iconTexture.size.y);
    	vertexBuffer.put(iconx + iconTexture.size.x);
    	vertexBuffer.put(icony);
    	vertexBuffer.put(iconx + iconTexture.size.x);
    	vertexBuffer.put(icony + iconTexture.size.y);
    	TEXTURE_COORDS.position(0);
    	vertexBuffer.position(0);
    	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
    	GLES10.glPopMatrix();
    	state = LABEL_STATE_SHOW_ICON;
    }
    
    private Texture genTextTextureRef(String key, LinkedHashMap<String, Texture> textTexturePool) {
    	IntBuffer textureRefBuf=IntBuffer.allocate(1);
        GLES10.glGenTextures(1, textureRefBuf);
        int textureRef = textureRefBuf.get(0);
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureRef);
        try {
        	Bitmap textBitmap = getTextBitmap(name, style == STYLE_NO_ICON_WITH_BACKGROUND ? 0 : -1, fontSize, color);
            int width = textBitmap.getWidth();
            int height = textBitmap.getHeight();
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
            GLES10.glTexParameterf(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, textBitmap, 0);
            Texture texture = new Texture();
            texture.size.x = Util.getPower2(width);
            texture.size.y = Util.getPower2(height);
            texture.textureRef = textureRef;
            textTexturePool.put(key, texture);
            return texture;
        } catch (Exception e) {
            textureRefBuf.clear();
            textureRefBuf.put(0, textureRef);
            textureRefBuf.position(0);
            glDeleteTextures(1, textureRefBuf);
            textureRef=0;
            return null;
        }
    }
    
    private int drawLabelWithIcon(RectInteger rect, float x, float y, int width, int height, float rot, float scale, Grid grid, 
    		boolean needDrawNew, IntegerRef maxLabelLeft, ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer, 
    		LinkedHashMap<String, Texture> textTexturePool, LinkedHashMap<Integer,Texture> mapWordIconPool) {
    	Texture textTexture, iconTexture;
    	if(!grid.isInterSectRect(rect)) {
    		grid.addRect(rect);
    		if(state == LABEL_STATE_PRIMITIVE || state == LABEL_STATE_CANT_BE_SHOWN) {
    			fadingStartTime = System.currentTimeMillis();
    			state = LABEL_STATE_FADE_IN;
    			opacity = 0.1f;
    		}
    		else {
    			if(state != LABEL_STATE_SHOWN) {
					double curTime = System.currentTimeMillis();
    				if(state == LABEL_STATE_FADE_IN) {
    					opacity += (float)((curTime - fadingStartTime)) / LABEL_FADING_DURATION;
    					fadingStartTime = curTime;
    				}
    				else {
    					fadingStartTime = curTime;
    					state = LABEL_STATE_FADE_IN;
    				}
    				if(opacity > 1) {
    					opacity = 1;
    					state = LABEL_STATE_SHOWN;
    				}
    			}
    		}
    		String key = name + fontSize + color;
    		iconTexture = getIconTexture(mapWordIconPool);
    		textTexture = textTexturePool.get(key);
    		if(textTexture == null) {
    			if(needDrawNew) {
    				if(maxLabelLeft.value <= 0) {
    					state = LABEL_STATE_WAITING;
    					opacity = 0;
    					return state;
    				}
    				textTexture = genTextTextureRef(key, textTexturePool);
    				--maxLabelLeft.value;
    			}
    		}
    		if(textTexture != null) {
    			if(state == LABEL_STATE_WAITING) {
    				fadingStartTime = System.currentTimeMillis();
    				state = LABEL_STATE_FADE_IN;
    				opacity = 0.1f;
    			}
    			this.draw(x, y, rot, scale, width, height, iconTexture, textTexture, TEXTURE_COORDS, vertexBuffer);
    		}
    		else {
    			state = LABEL_STATE_WAITING;
    			opacity = 0;
    		}
    		return state;
    	}
    	else {
    		if(state == LABEL_STATE_PRIMITIVE || state == LABEL_STATE_WAITING) {
    			state = LABEL_STATE_CANT_BE_SHOWN;
    			return state;
    		}
    		else if(state == LABEL_STATE_FADE_IN) {
    			state = LABEL_STATE_FADE_OUT;
    			fadingStartTime = System.currentTimeMillis();
    		}
    		else {
    			double curTime = System.currentTimeMillis();
    		    if (state == LABEL_STATE_SHOWN) {
    		        fadingStartTime = curTime;
    		    }
    		    state = LABEL_STATE_FADE_OUT;
    		    opacity -= (float)(curTime - fadingStartTime) / LABEL_FADING_DURATION;
    		    fadingStartTime = curTime;
    		    if (opacity <= 0) {
    		        opacity = 0;
    		        state = LABEL_STATE_CANT_BE_SHOWN;
    		        return state;
    		    }
    		}
    		String key = name + fontSize + color;
    		iconTexture = this.getIconTexture(mapWordIconPool);
    		if(textTexturePool.containsKey(key)) {
    			textTexture = textTexturePool.get(key);
    			this.draw(x, y, rot, scale, width, height, iconTexture, textTexture, TEXTURE_COORDS, vertexBuffer);
    		}
            return state;//LABEL_STATE_FADE_OUT
    	}
    }
    
    public int draw(XYInteger center, XYZ centerXYZ, XYFloat centerDelta, 
    		float rotation, float sinRot, float cosRot, float scale, Grid grid, 
    		ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer, boolean needGenTexture, IntegerRef leftCountToDraw, 
    		LinkedHashMap<String, Texture> textTexturePool, LinkedHashMap<Integer,Texture> mapWordIconPool) {
    	int width, height;
        int tileSize = CONFIG.TILE_SIZE;
        int cx = center.x;
        int cy = center.y;
        float refx = cx + centerDelta.x + (this.x - centerXYZ.x) * tileSize - (tileSize >> 1);//label所在tile的左上角坐标
        float refy = cy + centerDelta.y + (centerXYZ.y - this.y) * tileSize - (tileSize >> 1);
        Texture textTexture, iconTexture;
        if (z != centerXYZ.z) {
            state = LABEL_STATE_CANT_BE_SHOWN;
            return state;
        }
        RectInteger rect = new RectInteger();
        float sx = point.x + refx;//point.x + refx为实际原始坐标
        float sy = point.y + refy;
        float dx = scale == 1 ? (sx - cx) : scale * (sx - cx);
        float dy = scale == 1 ? (sy - cy) : scale * (sy - cy);
        float x = rotation == 0 ? (dx + cx) : (cosRot * (dx) - (dy) * sinRot + cx);//旋转变换
        float y = rotation == 0 ? (dy + cy) : ((dx) * sinRot + (dy) * cosRot + cy);
        XYFloat size = calcTextRectSize(name, fontSize);
        width = (int) size.x;
        height = (int) size.y;
        
        if (style == STYLE_NO_ICON_WITHOUT_BACKGROUND || style == STYLE_NO_ICON_WITH_BACKGROUND) {//没有icon
            rect.left = (int) (x - (width >> 1) - TK_LABEL_BOUND_SIZE);
            rect.right = rect.left + width + TK_LABEL_BOUND_SIZE;
            rect.top = (int) (y - (height >> 1) - TK_LABEL_BOUND_SIZE);
            rect.bottom = rect.top + height + TK_LABEL_BOUND_SIZE;
            if (grid.isRectOutBound(rect)) {
                state = LABEL_STATE_CANT_BE_SHOWN;
                return state;
            }
            if (!grid.isInterSectRect(rect)) {
            	grid.addRect(rect);
                if (state == LABEL_STATE_PRIMITIVE || state == LABEL_STATE_CANT_BE_SHOWN) {
                    fadingStartTime = System.currentTimeMillis();
                    state = LABEL_STATE_FADE_IN;
                    opacity = 0.1f;
                }
                else {
                    if (state != LABEL_STATE_SHOWN) {
                    	double curTime = System.currentTimeMillis();
                        if (state == LABEL_STATE_FADE_IN) {
                            opacity += (float)(curTime - fadingStartTime) / LABEL_FADING_DURATION;
                        }
                        fadingStartTime = curTime;
                        if (opacity > 1) {
                            opacity = 1;
                            state = LABEL_STATE_SHOWN;
                        }
                    }
                }
                String key = name + fontSize + color;
                textTexture = textTexturePool.get(key);
                if (textTexture == null && !needGenTexture) {
                    state = LABEL_STATE_WAITING;//未生成纹理，不显示
                    opacity = 0;
                    return state;
                }
                if (textTexture == null) {
                    if (leftCountToDraw.value <= 0) {
                        state = LABEL_STATE_WAITING;//不显示
                        opacity = 0;
                        return state;
                    }
                    textTexture = genTextTextureRef(key, textTexturePool);
                    --leftCountToDraw.value;
                }
                if (state == LABEL_STATE_WAITING) {
                    fadingStartTime = System.currentTimeMillis();
                    state = LABEL_STATE_FADE_IN;
                    opacity = 0.1f;
                }
                this.draw(sx, sy, rotation, scale, width, height, null, textTexture, TEXTURE_COORDS, vertexBuffer);
                return state;
            }
            else {
                if (state == LABEL_STATE_PRIMITIVE || state == LABEL_STATE_WAITING) {
                    state = LABEL_STATE_CANT_BE_SHOWN;
                    return LABEL_STATE_CANT_BE_SHOWN;
                }
                else if (state == LABEL_STATE_FADE_IN) {
                    state = LABEL_STATE_FADE_OUT;
                    fadingStartTime = System.currentTimeMillis();
                    if (opacity <= 0) {
                        opacity = 0;
                        state = LABEL_STATE_CANT_BE_SHOWN;
                        return LABEL_STATE_CANT_BE_SHOWN;
                    }
                    String key = name + fontSize + color;
                    iconTexture = getIconTexture(mapWordIconPool);
            		if(textTexturePool.containsKey(key)) {
            			textTexture = textTexturePool.get(key);
            			this.draw(sx, sy, rotation, scale, width, height, iconTexture, textTexture, TEXTURE_COORDS, vertexBuffer);
            			return state;//LABEL_STATE_FADE_OUT
            		}
                }
                else {
                	double curTime = System.currentTimeMillis();
                    if (state == LABEL_STATE_SHOWN) {
                        fadingStartTime = curTime;
                    }
                    state = LABEL_STATE_FADE_OUT;
                    opacity-= (float)(curTime - fadingStartTime) / LABEL_FADING_DURATION;
                    fadingStartTime = curTime;
                    if (opacity <= 0) {
                        opacity = 0;
                        state = LABEL_STATE_CANT_BE_SHOWN;
                        return LABEL_STATE_CANT_BE_SHOWN;
                    }
                }
            }
        }
        else if (style == STYLE_ICON_ON_LEFT) {//icon在左边
        	if(iconSize == null) {
        		getIconSize();
        	}
            rect.left = (int) (x - (iconSize.x >> 1) - TK_LABEL_BOUND_SIZE);
            rect.right = rect.left + width + iconSize.x + TK_LABEL_BOUND_SIZE;
            rect.top = (int) (y - (Math.max(iconSize.y, height) >> 1) - TK_LABEL_BOUND_SIZE);
            rect.bottom = rect.top + Math.max(iconSize.y, height) + TK_LABEL_BOUND_SIZE;

            if (grid.isRectOutBound(rect)) {
                state = LABEL_STATE_CANT_BE_SHOWN;
                return LABEL_STATE_CANT_BE_SHOWN;
            }
            if (drawLabelWithIcon(rect, sx, sy, width, height, rotation, scale, grid, 
            		needGenTexture, leftCountToDraw, TEXTURE_COORDS, vertexBuffer, textTexturePool, mapWordIconPool) == LABEL_STATE_CANT_BE_SHOWN) {//如果icon在左边画不了，试试把icon放右边
                rect.left = (int) (x - (iconSize.x >> 1) - width - TK_LABEL_BOUND_SIZE);
                rect.right = rect.left + width + iconSize.x + TK_LABEL_BOUND_SIZE;
                rect.top = (int) (y - (Math.max(iconSize.y, height) >> 1) - TK_LABEL_BOUND_SIZE);
                rect.bottom = rect.top + Math.max(iconSize.y, height) + TK_LABEL_BOUND_SIZE;
                
                if (grid.isRectOutBound(rect)) {
                    state = LABEL_STATE_CANT_BE_SHOWN;
                    return LABEL_STATE_CANT_BE_SHOWN;
                }
                style = STYLE_ICON_ON_RIGHT;
                if (drawLabelWithIcon(rect, sx, sy, width, height, rotation, scale, grid, 
                		needGenTexture, leftCountToDraw, TEXTURE_COORDS, vertexBuffer, textTexturePool, mapWordIconPool) == LABEL_STATE_CANT_BE_SHOWN) {
                    style = STYLE_ICON_ON_LEFT;
                }
            }
        }
        else if (style == STYLE_ICON_ON_RIGHT) {//icon在右边
        	if(iconSize == null) {
        		getIconSize();
        	}
            rect.left = (int) (x - (iconSize.x >> 1) - width - TK_LABEL_BOUND_SIZE);
            rect.right = rect.left + width + iconSize.x + TK_LABEL_BOUND_SIZE;
            rect.top = (int) (y - (Math.max(iconSize.y, height) >> 1) - TK_LABEL_BOUND_SIZE);
            rect.bottom = rect.top + Math.max(iconSize.y, height) + TK_LABEL_BOUND_SIZE;
            if (grid.isRectOutBound(rect)) {
                state = LABEL_STATE_CANT_BE_SHOWN;
                return LABEL_STATE_CANT_BE_SHOWN;
            }
            if (drawLabelWithIcon(rect, sx, sy, width, height, rotation, scale, grid, 
            		needGenTexture, leftCountToDraw, TEXTURE_COORDS, vertexBuffer, textTexturePool, mapWordIconPool) == LABEL_STATE_CANT_BE_SHOWN) {//如果icon在右边画不了，试试把icon放左边
            	rect.left = (int) (x - (iconSize.x >> 1) - TK_LABEL_BOUND_SIZE);
                rect.right = rect.left + width + iconSize.x + TK_LABEL_BOUND_SIZE;
                rect.top = (int) (y - (Math.max(iconSize.y, height) >> 1) - TK_LABEL_BOUND_SIZE);
                rect.bottom = rect.top + Math.max(iconSize.y, height) + TK_LABEL_BOUND_SIZE;
                if (grid.isRectOutBound(rect)) {
                    state = LABEL_STATE_CANT_BE_SHOWN;
                    return LABEL_STATE_CANT_BE_SHOWN;
                }
                style = STYLE_ICON_ON_LEFT;
                if (drawLabelWithIcon(rect, sx, sy, width, height, rotation, scale, grid, 
                		needGenTexture, leftCountToDraw, TEXTURE_COORDS, vertexBuffer, textTexturePool, mapWordIconPool) == LABEL_STATE_CANT_BE_SHOWN) {
                    style = STYLE_ICON_ON_RIGHT;
                }
            }
        }
        if ((state == LABEL_STATE_CANT_BE_SHOWN || state == LABEL_STATE_FADE_OUT) && type == 30) {//bank
            RectInteger bankrect = new RectInteger();
            bankrect.left = (int) (x - (iconSize.x >> 1) - (TK_LABEL_BOUND_SIZE>>2));
            bankrect.right = rect.left + iconSize.x + (TK_LABEL_BOUND_SIZE>>2);
            bankrect.top = (int) (y - (iconSize.y >> 1) - (TK_LABEL_BOUND_SIZE>>2));
            bankrect.bottom = rect.top + iconSize.y + (TK_LABEL_BOUND_SIZE>>2);
            this.drawOnlyIcon(sx, sy, rect, rotation, scale, grid, TEXTURE_COORDS, vertexBuffer, mapWordIconPool);
        }
        return state;
    }
    
    public SingleRectLabel clone() {
        return new SingleRectLabel(name, color, style, iconId, iconId, STYLE_NO_ICON_WITH_BACKGROUND, STYLE_NO_ICON_WITHOUT_BACKGROUND, point, x, y, z);
    }
}

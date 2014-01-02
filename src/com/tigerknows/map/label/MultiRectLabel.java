/**
 * 
 */
package com.tigerknows.map.label;

import static android.opengl.GLES10.glDeleteTextures;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;

import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLUtils;

import com.decarta.CONFIG;
import com.decarta.Globals;
import com.decarta.android.map.TilesView.Texture;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYZ;
import com.tigerknows.map.Grid;
import com.tigerknows.map.RectInteger;
import com.tigerknows.util.XYInteger;

/**
 * @author chenming
 *
 */
public class MultiRectLabel extends Label {
	public XYInteger[] points;
	public RectInteger[] rectArray;
	public XYFloat[] centerPointArray;
	public float[] rotationArray;
    public XYFloat startPoint;
    public int startIndex;
//    public XYFloat[] sizeArray;
//    public float[] halfCharWidthArray;
    public XYFloat charSize;
    public float halfCharWidth;
    private Texture[] textTextures;
    public static float SQRT2 = (float) Math.sqrt(2d);
    
    /**
     * 此static的代码仅是为了避免其构造函数被优化
     */
    static {
        new MultiRectLabel("", 0, 0, 0, 0, 0, 0, null, 0, 0, 0);
    }
    
    public MultiRectLabel(
    		String name, 
    		int pointNum,
    		int color,
    		int bgColor,
    		int fontSize,
    		int type,
    		int priority,
    		XYInteger[] points,
    		int tileX,
    		int tileY,
    		int zoom
    		) {
        super();
    	this.name = name;
    	this.pointNum = pointNum;
    	this.color = color;
    	this.bgColor = bgColor;
    	int realFontSize = (int) ((fontSize) * Globals.g_metrics.density) + 1;
    	this.fontSize = realFontSize > fontSize ? realFontSize : fontSize;
    	this.type = type;
    	this.priority = priority;
    	this.points = points;
    	this.x = tileX;
    	this.y = tileY;
    	this.z = zoom;
    	this.fadingStartTime = 0;
    	this.opacity = 0;
    	this.state = Label.LABEL_STATE_PRIMITIVE;
    	int length = name.length();
    	textTextures = new Texture[length];
    	rectArray = new RectInteger[length];//指针数组
    	centerPointArray = new XYFloat[length];//指针数组
        for (int i = 0; i < length; ++i) {
            centerPointArray[i] = new XYFloat(0.0f, 0.0f);
            rectArray[i] = new RectInteger();
        }
    	rotationArray = new float[length];
        startPoint = new XYFloat(0, 0);
        startIndex = 0;
        charSize = null;
        halfCharWidth = 0;
    }
    
    private void drawSubLabel(float x, float y, float rot, float scale, float width, float height, Texture texture, 
    		ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer) {
    	GLES10.glPushMatrix();
    	if (scale != 1) {
    		GLES10.glScalef(1/scale, 1/scale, 1);
        	GLES10.glTranslatef(Math.round(x*scale), Math.round(y*scale), 0);
    	}
    	else {
        	GLES10.glTranslatef(Math.round(x), Math.round(y), 0);
    	}
    	GLES10.glRotatef(rot, 0, 0, 1);
    	GLES10.glEnable(GLES10.GL_BLEND);
    	GLES10.glTexEnvf(GLES10.GL_TEXTURE_ENV, GLES10.GL_TEXTURE_ENV_MODE, GLES10.GL_MODULATE);
    	GLES10.glBlendFunc(GLES10.GL_ONE, GLES10.GL_ONE_MINUS_SRC_ALPHA);
    	GLES10.glColor4f(opacity, opacity, opacity, opacity);
    	int sizePower2X = texture.size.x;
    	int sizePower2Y = texture.size.y;
    	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, texture.textureRef);
    	x = 0 - width/2;
    	y = 0 - height/2;
    	vertexBuffer.clear();
    	vertexBuffer.put(x);
    	vertexBuffer.put(y);
    	vertexBuffer.put(x);
    	vertexBuffer.put(y + sizePower2Y);
    	vertexBuffer.put(x + sizePower2X);
    	vertexBuffer.put(y);
    	vertexBuffer.put(x + sizePower2X);
    	vertexBuffer.put(y + sizePower2Y);
    	TEXTURE_COORDS.position(0);
    	vertexBuffer.position(0);
    	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
    	GLES10.glPopMatrix();
    }
    
    private Texture genTextTextureRef(String subname, String key, LinkedHashMap<String, Texture> textTexturePool) {
    	IntBuffer textureRefBuf=IntBuffer.allocate(1);
        GLES10.glGenTextures(1, textureRefBuf);
        int textureRef = textureRefBuf.get(0);
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textureRef);
        try {
        	Bitmap textBitmap = getTextBitmap(subname, -1, fontSize, color);
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
    
    private void initSize() {
        String subname = name.substring(0, 1);
		charSize = calcTextRectSize(subname, fontSize);
		halfCharWidth = charSize.x / 2;
//    	sizeArray = new XYFloat[nameLength];
//    	halfCharWidthArray = new float[nameLength];
//    	for(int i = 0; i < nameLength; ++i) {
//    		String subname = name.substring(i, i + 1);
//    		sizeArray[i] = super.calcTextRectSize(subname);
//    		halfCharWidthArray[i] = sizeArray[i].x / 2;
//    	}
    }
    
    public int draw(XYInteger center, XYZ centerXYZ, XYFloat centerDelta, 
    		float rotation, float sinRot, float cosRot, float scale, Grid grid, 
    		ByteBuffer TEXTURE_COORDS, FloatBuffer vertexBuffer, boolean needGenTexture, IntegerRef leftCountToDraw, 
    		LinkedHashMap<String, Texture> textTexturePool, LinkedHashMap<Integer,Texture> mapWordIconPool) {
//    	if(this.name.equals("文慧园路")) {
//    		LogWrapper.d("labeldebug", "init opacity: " + opacity + this.toString());
//    	}
        int tileSize = CONFIG.TILE_SIZE;
        int cx = center.x;
        int cy = center.y;
        float refx = cx + centerDelta.x + (this.x - centerXYZ.x) * tileSize - (tileSize >> 1);//label所在tile的左上角坐标
        float refy = cy + centerDelta.y + (centerXYZ.y - this.y) * tileSize - (tileSize >> 1);
        Texture textTexture;
        if (z != centerXYZ.z) {
            state = LABEL_STATE_CANT_BE_SHOWN;
            return state;
        }
        if(charSize == null) {
        	initSize();
        }
        int num = name.length();
        int nextEndPointIndex = 1;
        XYFloat tempStartPoint = new XYFloat(points[0].x, points[0].y);
        XYFloat firstScreenPoint = new XYFloat(0, 0);
        XYFloat lastScreenPoint = new XYFloat(0, 0);
        XYInteger endPoint = null;
        int i = 0;
        boolean isNewStart = true, isTotalOutBound = true;
        float remainLength = 0.0f, invLength = 0.0f;
        int alphaFlag = 0;
        float stepx = 0, stepy = 0;
        float cosAlpha = 0, sinAlpha = 0, cosBeta = 0, sinBeta = 0;
//        if(sizeArray == null) {
//        	initSize(num);
//        }
        float charWidth = charSize.x / scale, radiusWidth = halfCharWidth * SQRT2;
		if (state != LABEL_STATE_CANT_BE_SHOWN
				&& state != LABEL_STATE_PRIMITIVE) {
			tempStartPoint.x = startPoint.x;
			tempStartPoint.y = startPoint.y;
			nextEndPointIndex = startIndex;
		} else {
			startPoint.x = (int) tempStartPoint.x;
			startPoint.y = (int) tempStartPoint.y;
			startIndex = nextEndPointIndex;
		}
//		charWidth = sizeArray[0].x / scale;
//		halfCharWidth = charWidth / 2;
//		radiusWidth = halfCharWidth * SQRT2;
		while (nextEndPointIndex < pointNum && i < num) {
			if (isNewStart) {
				endPoint = points[nextEndPointIndex];
				float dx = (endPoint.x - tempStartPoint.x);
				float dy = (endPoint.y - tempStartPoint.y);
				remainLength = (float) Math.sqrt(dx * dx + dy * dy);
//				charWidth = sizeArray[i].x / scale;
				if (remainLength < charWidth) {// length < width
					if (i == 0) {
						tempStartPoint.x = endPoint.x;
						tempStartPoint.y = endPoint.y;
						startPoint.x = (int) tempStartPoint.x;
						startPoint.y = (int) tempStartPoint.y;
						startIndex = nextEndPointIndex + 1;
					}
					++nextEndPointIndex;
					continue;
				}
				cosBeta = cosAlpha;
				sinBeta = sinAlpha;
				invLength = 1 / remainLength;
				cosAlpha = dx * invLength;
				sinAlpha = dy * invLength;
				float cosAngle = sinAlpha * sinBeta + cosAlpha * cosBeta;
				if (cosAngle < 0) {// 夹角太小, 若设为0，则表示文字拐角不能小于90°
					i = 0;
					isNewStart = true;
					tempStartPoint.x = endPoint.x;
					tempStartPoint.y = endPoint.y;
					startPoint.x = tempStartPoint.x;
					startPoint.y = tempStartPoint.y;
					startIndex = nextEndPointIndex;
					continue;
				}
				alphaFlag = 0;
				if (dy < 0) {
					alphaFlag = 3;
				}
				stepx = charWidth * cosAlpha;
				stepy = charWidth * sinAlpha;
				isNewStart = false;
			}

			if (remainLength < charWidth) {
				++nextEndPointIndex;// 起点不变，终点直接取下一个点
				if (i == 0) {
					startIndex = nextEndPointIndex;
				}
				isNewStart = false;
				continue;
			} else {
				remainLength = remainLength - charWidth;
				RectInteger rect = rectArray[i];// 留着用于设置bitset
				XYFloat labelCenter = centerPointArray[i];// 留着用于标识字的位置
				labelCenter.x = tempStartPoint.x + stepx / 2;
				labelCenter.y = tempStartPoint.y + stepy / 2;
				float dx = scale == 1 ? (labelCenter.x + refx - cx) : scale * (labelCenter.x + refx - cx);
				float dy = scale == 1 ? (labelCenter.y + refy - cy) : scale * (labelCenter.y + refy - cy);
				float x = rotation == 0 ? (dx + cx) : (cosRot * (dx) - (dy) * sinRot + cx);
				float y = rotation == 0 ? (dy + cy) : ((dx) * sinRot + (dy) * cosRot + cy);
				rect.left = Math.round(x - radiusWidth);
				rect.right = Math.round(x + radiusWidth);
				rect.top = Math.round(y - radiusWidth);
				rect.bottom = Math.round(y + radiusWidth);
				RectInteger addedRect = null;
				if (((state == LABEL_STATE_PRIMITIVE) && !grid.isContainRect(rect))
						|| (addedRect = grid.gridIntersect(rect)) != null) {
					i = 0;// 从头开始找位置
					tempStartPoint.x = tempStartPoint.x + stepx;
					tempStartPoint.y = tempStartPoint.y + stepy;
//					charWidth = sizeArray[i].x / scale;
//					stepx = charWidth * cosAlpha;
//					stepy = charWidth * sinAlpha;
					isTotalOutBound = true;
					while (((addedRect != null && addedRect.isPointInRectF(tempStartPoint)) || 
							(state == LABEL_STATE_PRIMITIVE && !grid.isContainPoint(tempStartPoint)))
							&& nextEndPointIndex < pointNum) {
						if (remainLength > charWidth) {
							tempStartPoint.x = tempStartPoint.x + stepx;
							tempStartPoint.y = tempStartPoint.y + stepy;
							remainLength -= charWidth;
						} else {
							tempStartPoint.x = endPoint.x;
							tempStartPoint.y = endPoint.y;
							++nextEndPointIndex;
							isNewStart = true;
							break;
						}
					}
					startPoint.x = tempStartPoint.x;
					startPoint.y = tempStartPoint.y;
					startIndex = nextEndPointIndex;
					continue;
				}
				if (!grid.isRectOutBound(rect) && isTotalOutBound) {
					isTotalOutBound = false;
				}
				rotationArray[i] = cosAlpha + alphaFlag;// 留着用于标识字的旋转量
				if (i == 0) {
					firstScreenPoint.x = x;
					firstScreenPoint.y = y;
				} else {
					if (i == num - 1) {
						lastScreenPoint.x = x;
						lastScreenPoint.y = y;
					}
				}
				++i;
				if (i < num) {// 找下一个字的位置
					tempStartPoint.x = tempStartPoint.x + stepx;
					tempStartPoint.y = tempStartPoint.y + stepy;
//					charWidth = sizeArray[i].x / scale;
//					stepx = charWidth * cosAlpha;
//					stepy = charWidth * sinAlpha;
					continue;
				} else {
					break;
				}
			}
		}
		if (i < num || isTotalOutBound) {
			state = LABEL_STATE_CANT_BE_SHOWN;
//			if(this.name.equals("文慧园路")) {
//	    		LogWrapper.d("labeldebug", "can not be shown opacity: " + opacity + this.toString());
//	    	}
			return LABEL_STATE_CANT_BE_SHOWN;
		}
//		if(this.name.equals("文慧园路")) {
//    		LogWrapper.d("labeldebug", "can shown opacity: " + opacity + this.toString());
//    	}
		//draw
//		LogWrapper.i("MultiRectLabel", "draw label: " + name + "at: " + startPoint.x + ", " + startPoint.y);
		for (RectInteger rect : rectArray) {
			grid.addRect(rect);
		}
		if (state == LABEL_STATE_PRIMITIVE
				|| state == LABEL_STATE_CANT_BE_SHOWN) {
			fadingStartTime = System.currentTimeMillis();
			state = LABEL_STATE_FADE_IN;
			opacity = 0.1f;
		} else {
			if (state != LABEL_STATE_SHOWN) {
				double currentTime = System.currentTimeMillis();
				if (state == LABEL_STATE_FADE_IN) {
					opacity += (float) (currentTime - fadingStartTime) / LABEL_FADING_DURATION;
				} else {// last state is LABEL_STATE_FADE_OUT，其实不会有fadeout状态
					fadingStartTime = currentTime;
				}
				if (opacity > 1) {
					opacity = 1;
					state = LABEL_STATE_SHOWN;
				}
			}
		}
		boolean is_label_drawn = false;
		String subLabelName = null;
		String key = null;
		for (int j = 0; j < num; ++j) {
			subLabelName = name.substring(j, j + 1);
			key = subLabelName + fontSize + color;
			textTexture = textTexturePool.get(key);
			if ((textTexture == null && !needGenTexture)) {
				state = LABEL_STATE_WAITING;
				opacity = 0;
				return state;
			} else {
				if (textTexture == null) {
					if (leftCountToDraw.value <= 0) {
						state = LABEL_STATE_WAITING;
						opacity = 0;
						return state;
					}
					textTextures[j] = this.genTextTextureRef(subLabelName, key, textTexturePool);
					is_label_drawn = true;
				} else {
					textTextures[j] = textTexture;
				}
			}
		}
		if (is_label_drawn) {
			--leftCountToDraw.value;
		}
		if (state == LABEL_STATE_WAITING) {
			fadingStartTime = System.currentTimeMillis();
			state = LABEL_STATE_FADE_IN;
			opacity = 0.1f;
		}
		double lastrot = 0;
		double rot = 0;
		for (int j = 0; j < num; ++j) {
			int texRefIdx = j;
			if (j > 0 && rotationArray[j] == rotationArray[j - 1]) {
				rot = lastrot;
			} else {
				if (rotationArray[j] > 1)
					rot = -Math.acos(rotationArray[j] - 3);
				else
					rot = Math.acos(rotationArray[j]);
				lastrot = rot;
			}
			if (lastScreenPoint.x - firstScreenPoint.x < 0) {
				texRefIdx = num - 1 - j;
				rot += Math.PI;
			}
			XYFloat labelCenter = centerPointArray[j];
			drawSubLabel(labelCenter.x + refx, labelCenter.y + refy, (float) (rot * 180 / Math.PI), scale,
					charSize.x, charSize.y, textTextures[texRefIdx], TEXTURE_COORDS, vertexBuffer);
		}
		return state;
    }
    
    public MultiRectLabel clone() {
        return new MultiRectLabel(name, startIndex, startIndex, startIndex, startIndex, startIndex, startIndex, points, x, y, z);
    }
}

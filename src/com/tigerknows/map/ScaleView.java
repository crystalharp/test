/**
 * 
 */
package com.tigerknows.map;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.Bitmap;
import android.opengl.GLES10;

import com.decarta.Globals;
import com.decarta.android.map.TilesView.Texture;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.XYFloat;
import com.tigerknows.map.label.Label;

/**
 * author chenming
 *
 */
public class ScaleView {
	Texture[] textures;
    String[] labelArray;
    int labelHeight;
    int scaleHeight;
    int fontSize;
    FloatBuffer vertexBuffer;
    float density;
    float lineWidth;
    float coreWidth;
    public ScaleView() {
    	labelArray = new String[] {
    			"",
                "",
                "",
                "",
                "",
                "200千米",//4
                "100千米",
                "50千米",
                "25千米",//7
                "20千米",//8
                "10千米",//9无用
                "5千米",//10
                "2千米",
                "1千米",
                "500米",
                "200米",//14
                "100米",
                "50米",
                "25米",
                "10米",
    	};
    	int length = labelArray.length;
        textures = new Texture[length];        
        density = Globals.g_metrics.density;
        fontSize = (int) (12 * density);
        XYFloat size = Label.calcTextRectSize("米", fontSize);
        labelHeight = (int) (size.y + 3);
        scaleHeight = (int) (5 * density);
		ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 2 * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer = vbb.asFloatBuffer();
		lineWidth = 4f * density;
		coreWidth = 1.5f * density;
    }
    
    public void clearTexture() {
    	for(Texture texture : textures) {
//    		GLES10.glDeleteTextures(n, textures)
    	}
    }
    
    private void drawScaleLine(FloatBuffer vertexBuffer, float x1, float y1, float x2, float y2) {
    	vertexBuffer.clear();
    	vertexBuffer.put(x1);
    	vertexBuffer.put(y1);
    	vertexBuffer.put(x2);
    	vertexBuffer.put(y2);
    	vertexBuffer.position(0);
    	GLES10.glDrawArrays(GLES10.GL_LINE_STRIP, 0, 2);
    }

    public void renderGL(XYFloat topLeftXY, float zoomLevel, float lat, int z, ByteBuffer TEXTURE_COORDS) {
    	Texture texture = textures[z];
    	if(texture == null) {
    		texture = Label.genNormalTextTextureRef(labelArray[z], fontSize, 0x00000000);
    		if(texture == null) {
    			LogWrapper.e("ScaleView", "generate scale view label texture failed");
    			return;
    		}
    		textures[z] = texture;
    	}
    	GLES10.glEnable(GLES10.GL_BLEND);
    	GLES10.glTexEnvf(GLES10.GL_TEXTURE_ENV, GLES10.GL_TEXTURE_ENV_MODE, GLES10.GL_MODULATE);
    	GLES10.glBlendFunc(GLES10.GL_ONE, GLES10.GL_ONE_MINUS_SRC_ALPHA);
    	GLES10.glColor4f(1, 1, 1, 1);
    	GLES10.glVertexPointer(2, GLES10.GL_FLOAT, 0, vertexBuffer);
    	vertexBuffer.clear();
    	vertexBuffer.put(topLeftXY.x + 0);
    	vertexBuffer.put(topLeftXY.y + 0);
    	vertexBuffer.put(topLeftXY.x + 0);
    	vertexBuffer.put(topLeftXY.y + texture.size.y);
    	vertexBuffer.put(topLeftXY.x + texture.size.x);
    	vertexBuffer.put(topLeftXY.y + 0);
    	vertexBuffer.put(topLeftXY.x + texture.size.x);
    	vertexBuffer.put(topLeftXY.y + texture.size.y);
    	TEXTURE_COORDS.position(0);
    	vertexBuffer.position(0);
    	GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, texture.textureRef);
    	GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, 4);
    	GLES10.glDisable(GLES10.GL_TEXTURE_2D);
        float scaleLength = Ca.tk_get_pix_count_of_scale(lat, z);
        scaleLength = (float) (scaleLength * Math.pow(2, zoomLevel - z));
        GLES10.glColor4f(1, 1, 1, 1);
        GLES10.glLineWidth(lineWidth);
        drawScaleLine(vertexBuffer, topLeftXY.x, topLeftXY.y + labelHeight, topLeftXY.x, topLeftXY.y + labelHeight + scaleHeight);
        drawScaleLine(vertexBuffer, topLeftXY.x, topLeftXY.y + labelHeight + scaleHeight, topLeftXY.x + scaleLength, topLeftXY.y + labelHeight + scaleHeight);
        drawScaleLine(vertexBuffer, topLeftXY.x + scaleLength, topLeftXY.y + labelHeight, topLeftXY.x + scaleLength, topLeftXY.y + labelHeight + scaleHeight);
        GLES10.glLineWidth(coreWidth);
        GLES10.glColor4f(0, 0, 0, 1);
        drawScaleLine(vertexBuffer, topLeftXY.x, topLeftXY.y + labelHeight, topLeftXY.x, topLeftXY.y + labelHeight + scaleHeight);
        drawScaleLine(vertexBuffer, topLeftXY.x, topLeftXY.y + labelHeight + scaleHeight, topLeftXY.x + scaleLength, topLeftXY.y + labelHeight + scaleHeight);
        drawScaleLine(vertexBuffer, topLeftXY.x + scaleLength, topLeftXY.y + labelHeight, topLeftXY.x + scaleLength, topLeftXY.y + labelHeight + scaleHeight);
    }
}

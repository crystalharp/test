/**
 * 
 */
package com.tigerknows.map;

import static android.opengl.GLES10.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES10.GL_TEXTURE_2D;
import static android.opengl.GLES10.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_T;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLUtils;

import com.tigerknows.map.label.*;

/**
 * @author chenming
 *
 */
public class TileInfo {
	public Label[] labels;
	public Bitmap bitmap = null;
	public int tileTextureRef = 0;
	
	private int genTextureRef(){
		IntBuffer textureRefBuf = IntBuffer.allocate(1);
		textureRefBuf.clear();
		textureRefBuf.position(0);
		GLES10.glGenTextures(1, textureRefBuf);
		return textureRefBuf.get(0);
	}
	
	private int genTextureRef(GL10 gl){
		IntBuffer textureRefBuf = IntBuffer.allocate(1);
		textureRefBuf.clear();
		textureRefBuf.position(0);
		gl.glGenTextures(1, textureRefBuf);
		return textureRefBuf.get(0);
	}
	
	private void deleteTextureRef(int textureRef){
		if(textureRef==0) return;
		IntBuffer textureRefBuf=IntBuffer.allocate(1);
		textureRefBuf.clear();
		textureRefBuf.put(0,textureRef);
		textureRefBuf.position(0);
		GLES10.glDeleteTextures(1, textureRefBuf);
	}
	
	private void deleteTextureRef(GL10 gl, int textureRef){
		if(textureRef==0) return;
		IntBuffer textureRefBuf=IntBuffer.allocate(1);
		textureRefBuf.clear();
		textureRefBuf.put(0,textureRef);
		textureRefBuf.position(0);
		gl.glDeleteTextures(1, textureRefBuf);
	}
	
	public int copyToTexture(int textureRef) throws Exception {
		if(tileTextureRef != 0) {
			return tileTextureRef;
		}
		if(textureRef == 0) {
			return 0;
		}
		GLES10.glBindTexture(GL_TEXTURE_2D, textureRef);
		GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
		GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
		GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		GLUtils.texSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap);
		bitmap.recycle();
		bitmap = null;//置空以便回收
		tileTextureRef = textureRef;
		return tileTextureRef;
	}
	public int generateTileTextureRefTest() {
		int tileTextureRefTest = 0;
		if (bitmap == null) {
			return 0;
		}
		tileTextureRefTest = genTextureRef();
		GLES10.glBindTexture(GL_TEXTURE_2D, tileTextureRefTest);
		try {
			GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
					GLES10.GL_LINEAR);
			GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
					GLES10.GL_LINEAR);
			GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
					GL_CLAMP_TO_EDGE);
			GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
					GL_CLAMP_TO_EDGE);
			GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
			// bitmap.recycle();
			// bitmap = null;//置空以便回收
		} catch (Exception e) {
			deleteTextureRef(tileTextureRefTest);
			tileTextureRefTest = 0;
		}
		return tileTextureRefTest;
	}
	public int generateTileTextureRef() {
		if (tileTextureRef == 0) {
			if(bitmap == null) {
				return 0;
			}
			tileTextureRef = genTextureRef();
			GLES10.glBindTexture(GL_TEXTURE_2D, tileTextureRef);
			try{
				GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
				GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
				GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				GLES10.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
				bitmap.recycle();
				bitmap = null;//置空以便回收
			}catch(Exception e){
				deleteTextureRef(tileTextureRef);
				tileTextureRef = 0;
			}
		}
		return tileTextureRef;
	}
	
	public int copyToTexture(GL10 gl, int textureRef) throws Exception {
		if(tileTextureRef != 0) {
			return tileTextureRef;
		}
		if(textureRef == 0) {
			return 0;
		}
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureRef);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, bitmap);
		bitmap.recycle();
		bitmap = null;//置空以便回收
		tileTextureRef = textureRef;
		return tileTextureRef;
	}
	
	public int generateTileTextureRef(GL10 gl) {
		if (tileTextureRef == 0) {
			if(bitmap == null) {
				return 0;
			}
			tileTextureRef = genTextureRef(gl);
			GLES10.glBindTexture(GL_TEXTURE_2D, tileTextureRef);
			try{
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
				gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
				GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
//				File myCaptureFile = new File(MapEngine.getInstance().getMapPath() + "test" + tileTextureRef + ".jpg");
//                BufferedOutputStream bos = new BufferedOutputStream(
//                                                         new FileOutputStream(myCaptureFile));
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
//                bos.flush();
//                bos.close();
				bitmap.recycle();
				bitmap = null;//置空以便回收
			}catch(Exception e){
				deleteTextureRef(gl, tileTextureRef);
				tileTextureRef = 0;
			}
		}
		return tileTextureRef;
	}
}

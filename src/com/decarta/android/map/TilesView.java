/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.map;

import static android.opengl.GLES10.GL_BLEND;
import static android.opengl.GLES10.GL_BYTE;
import static android.opengl.GLES10.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.GL_DITHER;
import static android.opengl.GLES10.GL_FASTEST;
import static android.opengl.GLES10.GL_FLOAT;
import static android.opengl.GLES10.GL_MODELVIEW;
import static android.opengl.GLES10.GL_NEAREST;
import static android.opengl.GLES10.GL_PERSPECTIVE_CORRECTION_HINT;
import static android.opengl.GLES10.GL_POINT_SMOOTH_HINT;
import static android.opengl.GLES10.GL_PROJECTION;
import static android.opengl.GLES10.GL_REPLACE;
import static android.opengl.GLES10.GL_TEXTURE_2D;
import static android.opengl.GLES10.GL_TEXTURE_COORD_ARRAY;
import static android.opengl.GLES10.GL_TEXTURE_ENV;
import static android.opengl.GLES10.GL_TEXTURE_ENV_MODE;
import static android.opengl.GLES10.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES10.GL_TRIANGLE_STRIP;
import static android.opengl.GLES10.GL_VERTEX_ARRAY;
import static android.opengl.GLES10.glDeleteTextures;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.decarta.CONFIG;
import com.decarta.Globals;
import com.decarta.Profile;
import com.decarta.android.exception.APIException;
import com.decarta.android.location.BoundingBox;
import com.decarta.android.location.Position;
import com.decarta.android.map.Compass.PlaceLocation;
import com.decarta.android.map.MapLayer.MapLayerProperty;
import com.decarta.android.map.MapLayer.MapLayerType;
import com.decarta.android.map.RotationTilt.RotateReference;
import com.decarta.android.map.RotationTilt.TiltReference;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYFloat;
import com.decarta.android.util.XYInteger;
import com.decarta.android.util.XYZ;
import com.tigerknows.map.Grid;
import com.tigerknows.map.MapEngine;
import com.tigerknows.map.MapView;
import com.tigerknows.map.ScaleView;
import com.tigerknows.map.TexturePool;
import com.tigerknows.map.TileDownload;
import com.tigerknows.map.MapView.MapType;
import com.tigerknows.map.MapView.MoveEndEventListener;
import com.tigerknows.map.TileInfo;
import com.tigerknows.map.label.Label;
import com.tigerknows.map.label.SingleRectLabel;
import com.tigerknows.util.Utility;

/**
 * This class provide all function implementation related to map. It's a child
 * view of MapView. The name of this class is a little misleading, basically it
 * means that any object that will move along with map/tiles should be included
 * in this class.
 */
public class TilesView extends GLSurfaceView {
	/******************************* static variable & initialization *******************************/
	private static final double ZOOM_PENALTY = 0.8;
	private static final int LONG_TOUCH_TIME_MIN = 1000 * 1000000;
	private static final int CLICK_DOWN_UP_TIME_MAX = 300 * 1000000;
	private static final int DOUBLE_CLICK_INTERVAL_TIME_MAX = 500 * 1000000;
	private static final float SAME_POINT_MOVED_DISTANCE_MAX = 30 / 1.5f;
	private static final float MIN_PINCH_DISTANCE = 150 / 1.5f;
	private static final float MIN_ROTATE_DISTANCE = 80 / 1.5f;
	private static final float CLONE_MAP_LAYER_DRAW_PERCENT = 0.4f;
	// private static final float DRAW_ZOOM_LAYER_DRAW_PERCENT=0.8f;
	private static final int FADING_START_ALPHA = 30;
	private static final float ABNORMAL_DRAGGING_DIST = 1600 / 1.5f;
	private static final float ABNORMAL_PINCH_CENTER_DIST = 300 / 1.5f;
	private static final int ABNORMAL_ZROTATION = 30;

	private static final int MAX_TILE_TEXTURE_REF_DEF = 128;

	private static final float XROTATION_YDIST = 300 / 1.5f;
	private static final int XROTATION_TIME = 300 * 1000000;
	private static final int ZROTATION_TIME = 300 * 1000000;

	private static Paint backgroundP;

	// private static float ZOOMING_LAG=0.1f;

	private static float Cos30 = (float) Math.cos(30 * Math.PI / 180);
	private static float Cos60 = (float) Math.cos(60 * Math.PI / 180);

	// Tigerknows begin
	public static class Texture {
		public int textureRef = 0;
		public XYInteger size = new XYInteger(0, 0);
	}

	private Timer drawMyLocationTimer;
	boolean isMyLocation = false;
	public boolean stopRefreshMyLocation = false;

	public void setRefreshMapText(boolean refresh) {
		synchronized (drawingLock) {
			this.mapText.refresh = refresh;
		}
	}

	private boolean paused = false;
	private MapRender mapRender;
	private MapText mapText = new MapText();
	private Grid labelGrid;
	private TileThread tileRunners[] = new TileThread[CONFIG.TILE_THREAD_COUNT];
	private DownloadThread downloadRunners[] = new DownloadThread[CONFIG.TILE_THREAD_COUNT];
	private LinkedList<TileDownload> tilesWaitForDownloading = new LinkedList<TileDownload>();

	float tkZRotation;

	public void tkRotateZToDegree(float zRotation) {
		synchronized (drawingLock) {
			tkZRotation = zRotation;

			refreshMap();
		}
	}

	public void showOverlay(String overlayName, boolean top) {
		synchronized (drawingLock) {
			for (int i = 0; i < overlays.size(); i++) {
				if (overlayName.equals(overlays.get(i).getName())) {
					ItemizedOverlay itemizedOverlay = overlays.remove(i);
					if (top) {
						overlays.add(itemizedOverlay);
					} else {
						overlays.add(0, itemizedOverlay);
					}
					break;
				}
			}
		}
		refreshMap();
	}

	private float tkJumpZoomLevel(float newZoomLevel) {
		if (newZoomLevel < CONFIG.ZOOM_JUMP + 1
				&& newZoomLevel >= CONFIG.ZOOM_JUMP) {
			if (zoomLevel > CONFIG.ZOOM_JUMP) {
				newZoomLevel -= 1;
			} else if (zoomLevel < CONFIG.ZOOM_JUMP) {
				newZoomLevel += 1;
			}
		}
		return newZoomLevel;
	}

	public LinkedList<TileDownload> getTilesWaitForDownloading() {
		return tilesWaitForDownloading;
	}

	public EasingRecord getEasingRecord() {
		return easingRecord;
	}

	public ZoomingRecord getZoomingRecord() {
		return zoomingRecord;
	}

	private Rect padding = new Rect();

	public Rect getPadding() {
		return padding;
	}

	public MapText getMapText() {
		return mapText;
	}

	public void noticeDownload(int state) {
		mParentMapView.executeDownloadListeners(state);
	}

	/**
	 * 是否取消快照
	 */
	public boolean isCancelSnap = false;

	/**
	 * 快照图片
	 */
	Bitmap snapBmp;

	/**
	 * 快照的中心位置
	 */
	Position snapCenterPos;

	/**
	 * 停止快照
	 */
	public void stopSnap() {
		snapCenterPos = null;
		refreshMap();
	}

	/**
	 * 请求快照地图
	 * 
	 * @param position
	 */
	public void requestSnap(Position snapCenterPos) {
		stopSnap();
		this.isCancelSnap = false;
		this.snapCenterPos = snapCenterPos;
		refreshMap();
	}

	public boolean isSnapFinish() {
		return this.snapCenterPos == null;
	}

	/**
	 * 获取快照地图
	 * 
	 * @return
	 */
	public Bitmap getSnapBitmap() {
		if (CONFIG.DRAW_BY_OPENGL) {
			Bitmap bm = snapBmp;
			snapBmp = null;
			return bm;
		} else {
			return Utility.viewToBitmap(this);
		}
	}

	/**
	 * 快照地图
	 * 
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param gl
	 * @return
	 */
	public static Bitmap savePixels(int x, int y, int w, int h, GL10 gl) {
		int b[] = new int[w * h];
		int bt[] = new int[w * h];
		IntBuffer ib = IntBuffer.wrap(b);
		ib.position(0);
		gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

		/*
		 * remember, that OpenGL bitmap is incompatible with Android bitmap and
		 * so, some correction need.
		 */
		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				int pix = b[i * w + j];
				int pb = (pix >> 16) & 0xff;
				int pr = (pix << 16) & 0x00ff0000;
				int pix1 = (pix & 0xff00ff00) | pr | pb;
				bt[(h - i - 1) * w + j] = pix1;
			}
		}
		Bitmap sb = Bitmap.createBitmap(bt, w, h, Config.ARGB_4444);
		return sb;
	}

	// Tigerknows end

	static {
		backgroundP = new Paint();
		backgroundP.setStrokeWidth(1);
		// backgroundP.setColor(Color.YELLOW);
		backgroundP.setColor(CONFIG.BACKGROUND_GRID_COLOR);
	}

	/*********************************** instance variables *******************************************/
	private int max_tile_texture_ref = Math.round(MAX_TILE_TEXTURE_REF_DEF
			* 256f / CONFIG.TILE_SIZE);
	private int visibleLayerNum = 0;
	/**
	 * parent {@link MapView}
	 */
	protected MapView mParentMapView;
	/**
	 * pins as overlays. each overlay contains their own pins.
	 */
	private List<ItemizedOverlay> overlays = new ArrayList<ItemizedOverlay>();
	/**
	 * shapes, including polyline
	 */
	private List<Shape> shapes = new ArrayList<Shape>();
	/**
	 * info window. There is only one instance of info window.
	 */
	private InfoWindow infoWindow = new InfoWindow();
	
	private ScaleView scaleView = new ScaleView();
	
	private XYInteger gridSize = new XYInteger(0, 0);
	private XYInteger displaySize = new XYInteger(0, 0);
	private double radiusX;
	private double radiusY;
	private XYInteger panDirection = new XYInteger(1, 1);

	private ArrayList<MapLayer> mapLayers = new ArrayList<MapLayer>();

	private LinkedHashMap<Tile, TileInfo> tileInfos = null;
	private TexturePool texturePool;
	private MapType mapType = MapType.STREET;
	private XYZ centerXYZ = new XYZ(0, 0, -1);
	private XYDouble centerXY = null;
	private XYFloat centerDelta = new XYFloat(0f, 0f);
	private long fadingStartTime = 0;
	/**
	 * thread pool to load tiles
	 */
	// tigerknows TileThreadPool tileThreadPool=new TileThreadPool(this);
	// tigerknows TileCache tileCache=new TileCache(this);
	/**
	 * linked list to contain all tiles that's waiting for loading.
	 */
	private LinkedList<Tile> tilesWaitForLoading = new LinkedList<Tile>();

	private float zoomLevel = 13;
	private boolean zooming = false;
	private boolean isManuelZoom = false;
	private ZoomingRecord zoomingRecord = new ZoomingRecord();
	private XYFloat lastCenterConv = new XYFloat(0f, 0f);
	private boolean multiTouch = false;
	private XYFloat lastTouchConv = new XYFloat(0f, 0f);
	private XYFloat lastTouch = new XYFloat(0f, 0f);
	private TouchRecord touchRecord1 = new TouchRecord(5);
	EasingRecord easingRecord = new EasingRecord();
	private boolean infoWindowClicked = false;
	private boolean longClicked = false;
	private long lastTouchDownTime = 0;
	private XYFloat lastTouchDown = new XYFloat(0f, 0f);
	private float maxMoveFromTouchDown = 0;
	private long lastMoveTime = 0;
	private boolean isTouchBegin = false;
	private boolean isBeginMoving = false;
	private int movingFrameCount = 0;

	private long lastTouchUpTime = 0;
	private XYFloat lastTouchUp = new XYFloat(0f, 0f);
	private float lastDistConv = 0;
	private XYFloat lastDirection = null;
	private XYFloat lastTouchY = null;
	private int touchMode = 0;
	private TouchRecord touchRecord2 = new TouchRecord(10);
	private Timer longTouchTimer = null;
	private Object longTouchLock = new Object();

	private float lastZoomLevel = -1;
	private float lastXRotation = 0;
	private float lastZRotation = 0;

	private boolean isLabelFading = false;
	private boolean isStaying = false;
	private boolean isLastLabelFading = false;

	private MapMode mapMode = new MapMode();

	/**
	 * contains information such as routeID, so we can show the route on map
	 */
	private MapPreference mapPreference = new MapPreference();

	/**
	 * synchronize on this lock so there is no breaking part when move
	 */
	private Object drawingLock = new Object();
	boolean touching = false;
	Object touchingLock = new Object();

	private ArrayList<Tile> drawingTiles = new ArrayList<Tile>();

	private Compass compass = null;

	/***************************************** public methods **************************************************/
	/**
	 * constructor, initialize all objects required.
	 * 
	 * @param context
	 * @param mapView
	 */
	public TilesView(Context context, MapView mapView) {
		super(context);
		LogWrapper.i("Sequence", "TilesView constructor");
		// LogWrapper.i("GLVersion","GLES20.VERSION: " +
		// GLES20.glGetString(GLES10.GL_VERSION));
		// tigerknows CONFIG.printConfig();

		this.mParentMapView = mapView;
		WindowManager winMan = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = winMan.getDefaultDisplay();
		display.getMetrics(Globals.g_metrics);
		LogWrapper.i("TilesView", "xdpi:" + Globals.g_metrics.xdpi + ",ydpi:"
				+ Globals.g_metrics.ydpi + ",widthPixels:"
				+ Globals.g_metrics.widthPixels + ",heightPixesl:"
				+ Globals.g_metrics.heightPixels + ",density:"
				+ Globals.g_metrics.density + ",densityDpi:"
				+ Globals.g_metrics.densityDpi);

		for (int i = 0; i < MapLayerType.values().length; i++) {
			MapLayerProperty mapLayerProperty = MapLayerProperty
					.getInstance(MapLayerType.values()[i]);
			MapLayer mapLayer = new MapLayer(mapLayerProperty);
			mapLayers.add(mapLayer);
		}

		configureMapLayer();
		configureTileGrid(display.getWidth(), display.getHeight());

		LogWrapper.i("TilesView", "displaySize:" + displaySize + ",gridSize:"
				+ gridSize);
		setFocusable(true);

		if (CONFIG.DRAW_BY_OPENGL) {
			mapRender = new MapRender(context);
			setRenderer(mapRender);
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			requestFocus();
			setFocusableInTouchMode(true);
		} else {
			setWillNotDraw(false);
		}
		// tigerknows add begin
		// TileThread.startAllThreads();
		for (int i = 0; i < tileRunners.length; i++) {
			tileRunners[i] = new TileThread(i, this);
			// tileRunners[i].start();
		}
		DownloadThread.startAllThreads();
		for (int i = 0; i < downloadRunners.length; i++) {
			downloadRunners[i] = new DownloadThread(i, this);
			downloadRunners[i].start();
		}
		mapText.canvasSize.x = displaySize.x;
		mapText.canvasSize.y = displaySize.y;
		XYFloat offset = new XYFloat(0, 0);
		mapText.setOffset(offset, new RotationTilt());

		drawMyLocationTimer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				if (stopRefreshMyLocation) {
					return;
				}
				if (isMyLocation) {
					refreshMap();
				}
			}
		};
		drawMyLocationTimer.schedule(timerTask, 0, 1000);
		// tigerknows add end
	}

	/**
	 * 若背景线程已经被其它的运行实例停止，则重新生成背景线程
	 * 
	 * @return 否重新生成背景线程
	 */
	public boolean ensureThreadsRunning() {
		boolean result = false;

		boolean restart = false;
		for (int i = 0; i < tileRunners.length; i++) {
			if (tileRunners[i].tilesView != this) {
				restart = true;
				break;
			}
		}

		if (TileThread.isStop() || restart) {
			TileThread.startAllThreads();
			for (int i = 0; i < tileRunners.length; i++) {
				tileRunners[i] = new TileThread(i, this);
				tileRunners[i].start();
			}

			result = true;
		}

		restart = false;
		for (int i = 0; i < downloadRunners.length; i++) {
			if (downloadRunners[i].tilesView != this) {
				restart = true;
				break;
			}
		}

		if (DownloadThread.isStop() || restart) {
			DownloadThread.startAllThreads();
			for (int i = 0; i < downloadRunners.length; i++) {
				downloadRunners[i] = new DownloadThread(i, this);
				downloadRunners[i].start();
			}

			result = true;
		}

		restart = false;
		// if (mapTextThread.tilesView != this) {
		// restart = true;
		// }
		//
		// if (MapTextThread.isStop() || restart) {
		// MapTextThread.startThread();
		// mapTextThread = new MapTextThread(this);
		// mapTextThread.start();
		//
		// result = true;
		// }

		return result;
	}

	/**
	 * to make the orientation change work, we need to reset the coordinate of
	 * each tile and pin.
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// TODO Auto-generated method stub
		LogWrapper.i("Sequence", "TilesView.surfaceChanged w,h:" + w + "," + h);
		configureTileGrid(w, h);

		if (CONFIG.DRAW_BY_OPENGL)
			super.surfaceChanged(holder, format, w, h);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		LogWrapper.i("Sequence", "TilesView.surfaceCreated");
		if (CONFIG.DRAW_BY_OPENGL)
			super.surfaceCreated(holder);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// Log.i("Sequence","TilesView.surfaceDestroyed");
		if (CONFIG.DRAW_BY_OPENGL)
			super.surfaceDestroyed(holder);
	}

	public void refreshMap() {
		if (CONFIG.DRAW_BY_OPENGL)
			requestRender();
		else
			postInvalidate();
	}

	/**
	 * it's a wrapper of redraw request. parameters are used to define a rect to
	 * be redrawn.
	 * 
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 */
	public void refreshMap(float left, float top, float right, float bottom) {
		if (CONFIG.DRAW_BY_OPENGL)
			requestRender();
		else
			postInvalidate((int) left, (int) top, (int) right, (int) bottom);
	}

	public void clearTileTextures() {
		this.queueEvent(new Runnable() {
			public void run() {
				mapRender.clearTileTextures();
			}
		});
	}

	public void resume() {
		paused = false;
	}

	public void clearAllTextures() {
		paused = true;
		this.queueEvent(new Runnable() {
			public void run() {
				mapRender.clearAllTextures();
			}
		});
	}

	public void changeMapType(MapType mapType) {
		if (this.mapType.equals(mapType))
			return;
		this.mapType = mapType;
		synchronized (drawingLock) {
			configureMapLayer();
		}
		// refreshTileRequests(1, 1);
		refreshMap();

	}

	public void rotateXToDegree(float xRotation) {
		if (!CONFIG.DRAW_BY_OPENGL)
			return;
		if (xRotation > 0 || xRotation < MapView.MAP_TILT_MIN) {
			LogWrapper.e("TilesView", "x rotation must be between "
					+ MapView.MAP_TILT_MIN + " to 0");
			return;
			// throw new
			// APIException("x rotation must be between "+MapView.MAP_TILT_MIN+" to 0");
		}
		synchronized (drawingLock) {
			mapMode.xRotating = true;
			mapMode.xRotationEndTime = System.nanoTime()
					+ (long) (XROTATION_TIME * Math.abs((xRotation - mapMode
							.getxRotation()) / MapView.MAP_TILT_MIN));
			mapMode.xRotationEnd = xRotation;

			refreshMap();
		}

	}

	public void rotateZToDegree(float zRotation) {
		synchronized (drawingLock) {
			zRotation = ((zRotation + 180) % 360 + 360) % 360 - 180;
			float diff = zRotation - mapMode.getzRotation();
			if (diff > 180)
				diff -= 360;
			else if (diff < -180)
				diff += 360;
			// mapMode.zRotating=true;
			// mapMode.zRotationEndTime=System.nanoTime()+(long)(ZROTATION_TIME*Math.abs((diff)/180));
			// mapMode.zRotationEnd=zRotation;
			mapMode.setZRotation(zRotation, displaySize);
			refreshMap();
		}

	}

	public MapPreference getMapPreference() {
		return mapPreference;
	}

	/**
	 * clear the routeId, overlays and infowindow
	 * 
	 * @throws APIException
	 */
	public void clearMap() throws APIException {
		// tigerknows overlays.clear();
		// tigerknows add begin
		for (int i = overlays.size() - 1; i >= 0; i--) {
			if (!overlays.get(i).getName()
					.equals(ItemizedOverlay.MY_LOCATION_OVERLAY)) {
				overlays.remove(i);
			}
		}
		// tigerknows add end
		shapes.clear();

		infoWindow.setAssociatedOverlayItem(null);
		infoWindow.setVisible(false);

		if (mapPreference.getRouteId() != null) {
			mapPreference.setRouteId(null);
			// tigerknows
			// if(!MapLayerProperty.getInstance(MapLayerType.STREET).sessionId.equals(""+CONFIG.STATELESS_SESSION_ID))
			// tigerknows centerOnPosition(getCenterPosition());
		}
		refreshMap();

	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		LogWrapper.i("Sequence", "TilesView.onDetachedFromWindow");
		if (CONFIG.DRAW_BY_OPENGL)
			super.onDetachedFromWindow();

		// tigerknows modify begin
		// tileThreadPool.stopAllThreads();
		// tileCache.stopWritingAndRemoveOldTiles();
		TileThread.stopAllThreads();
		DownloadThread.stopAllThreads();
		// MapTextThread.stopThread();
		// tigerknows modify end
		clearTilesWaitForLoading();

		resetLongTouchTimer();

		overlays.clear();
		shapes.clear();
		if (drawMyLocationTimer != null) {
			drawMyLocationTimer.cancel();
			drawMyLocationTimer.purge();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		LogWrapper.i("Sequence", "TilesView.finalize");
		super.finalize();
	}

	public void clearTilesWaitForLoading() {
		synchronized (tilesWaitForLoading) {
			tilesWaitForLoading.clear();
		}
	}

	/**
	 * main method handle touch related events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (centerXY == null)
			return true;
		float density = Globals.g_metrics.density;

		int action = event.getAction() & MotionEvent.ACTION_MASK;
		int pCount = event.getPointerCount();

		XYFloat xy0Conv = screenXYToScreenXYConv(event.getX(0), event.getY(0));

		if (action == MotionEvent.ACTION_DOWN) {

			synchronized (touchingLock) {
				touching = true;
			}

			touchRecord1.reset();
			touchRecord2.reset();

			synchronized (drawingLock) {
				mapMode.resetXEasing();
				mapMode.resetZEasing();
				easingRecord.reset();
				zoomingRecord.reset();
			}

			infoWindowClicked = false;
			resetLongTouchTimer();
			longClicked = false;

			multiTouch = false;
			lastDistConv = 0;
			lastTouchY = null;
			lastDirection = null;
			touchMode = 0;// zooming

			lastZoomLevel = zoomLevel;
			lastXRotation = mapMode.getxRotation();
			lastZRotation = mapMode.getzRotation();

			long time = System.nanoTime();
			touchRecord1.push(time, xy0Conv.x, xy0Conv.y);
			// Log.i("TilesView","onTouchEvent touch down x,y,pCount:"+(int)(time/1000000)+","+event.getX()+","+event.getY()+","+pCount);

			if (pCount == 1) {
				lastTouchDownTime = System.nanoTime();
				lastTouchDown.x = event.getX(0);
				lastTouchDown.y = event.getY(0);
				maxMoveFromTouchDown = 0;

				lastTouchConv.x = xy0Conv.x;
				lastTouchConv.y = xy0Conv.y;
				lastTouch.x = event.getX(0);
				lastTouch.y = event.getY(0);

				// tigerknows add begin
				Position position = screenXYConvToPos(xy0Conv.x, xy0Conv.y);
				mParentMapView.executeTouchDownListeners(position);
				// tigerknows add end
				if (infoWindow.isVisible() && infoWindow.getMercXY() != null) {
					XYFloat relativeXY = new XYFloat(0, 0);
					if (snapToInfoWindow(event.getX(0), event.getY(0),
							relativeXY)) {
						infoWindowClicked = true;
						event.setLocation(relativeXY.x
								- InfoWindow.INFO_BORDER_SIZE, relativeXY.y
								- InfoWindow.INFO_BORDER_SIZE);
						infoWindow.dispatchTouchEvent(event);
						infoWindow
								.setBackgroundColor(InfoWindow.BACKGROUND_COLOR_CLICKED);
						refreshMap();
					}

				}

				if (!infoWindowClicked) {
					setupLongTouchTimer(xy0Conv);
				}
			} else if (pCount > 1) {
				multiTouch = true;
			}
			isTouchBegin = true;
			LogWrapper.d("Label", "--------touch begin---------");
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (pCount > 1) {
				resetLongTouchTimer();
			}

			if (!multiTouch && pCount == 1) {
				synchronized (longTouchLock) {
					if (longClicked)
						return true;

				}
				if (infoWindowClicked) {
					if (snapToInfoWindow(event.getX(0), event.getY(0), null)) {
						int oriColor = infoWindow.getBackgroundColor();
						infoWindow
								.setBackgroundColor(InfoWindow.BACKGROUND_COLOR_CLICKED);
						if (oriColor != InfoWindow.BACKGROUND_COLOR_CLICKED)
							refreshMap();
					} else {
						int oriColor = infoWindow.getBackgroundColor();
						infoWindow
								.setBackgroundColor(InfoWindow.BACKGROUND_COLOR_UNCLICKED);
						if (oriColor != InfoWindow.BACKGROUND_COLOR_UNCLICKED)
							refreshMap();
					}
					return true;
				}

				float moveFromTouchDownX = event.getX(0) - lastTouchDown.x;
				float moveFromTouchDownY = event.getY(0) - lastTouchDown.y;
				float moveFromTouchDown = moveFromTouchDownX
						* moveFromTouchDownX + moveFromTouchDownY
						* moveFromTouchDownY;
				if (moveFromTouchDown > maxMoveFromTouchDown)
					maxMoveFromTouchDown = moveFromTouchDown;
				if (maxMoveFromTouchDown > SAME_POINT_MOVED_DISTANCE_MAX
						* density * SAME_POINT_MOVED_DISTANCE_MAX * density) {
					resetLongTouchTimer();
				}
				// if(moveFromTouchDown < 0.00001) {
				// if(staying == false) {
				// beginStaying = true;
				// staying = true;
				// }
				// LogWrapper.e("Moving","onTouchEvent staying");
				// }
				// else {
				// staying = false;
				// beginStaying = false;
				// LogWrapper.e("Moving","onTouchEvent moving");
				// }
				XYFloat draggingConv = new XYFloat(0f, 0f);
				draggingConv.x = xy0Conv.x - lastTouchConv.x;
				draggingConv.y = xy0Conv.y - lastTouchConv.y;
				// Log.i("Moving","onTouchEvent dragging:"+dragging);
				if (Math.abs(event.getX(0) - lastTouch.x)
						+ Math.abs(event.getY(0) - lastTouch.y) > ABNORMAL_DRAGGING_DIST
						* density) {
					LogWrapper.e("Moving", "onTouchEvent dragging abruptly:"
							+ draggingConv);
				} else {
					long time = System.nanoTime();
					touchRecord1.push(time, xy0Conv.x, xy0Conv.y);
					// Log.i("TilesView","onTouchEvent touchRecord1:"+(int)(time/1000000)+","+event.getX()+","+event.getY());
					lastTouchConv.x = xy0Conv.x;
					lastTouchConv.y = xy0Conv.y;
					lastTouch.x = event.getX(0);
					lastTouch.y = event.getY(0);
					synchronized (drawingLock) {
						if (isTouchBegin) {
							isBeginMoving = true;
							isTouchBegin = false;
						}
						lastMoveTime = System.currentTimeMillis();
//						LogWrapper.i("Moving", "drag: " + draggingConv.toString() + "centerXY: " + centerXY.toString());
						moveView(draggingConv.x, draggingConv.y);
//						LogWrapper.i("Moving", "after dragging centerXY: " + centerXY.toString());
					}
					refreshMap();
				}
			} else if (pCount > 1) {
				multiTouch = true;
				// Log.i("TilesView","onTouchEvent event.x0,event.y0,event.x1,event.y1:"+(int)event.getX(0)+","+(int)event.getY(0)+","+(int)event.getX(1)+","+(int)event.getY(1));

				touchRecord2.push(0, event.getX(0), event.getY(0));
				touchRecord2.push(0, event.getX(1), event.getY(1));
				if (touchRecord2.size >= 2 * 2) {
					int touchModeL = 0;// zooming

					int index = touchRecord2.index;
					int start = (index - (touchRecord2.size - 1) + touchRecord2.capacity)
							% touchRecord2.capacity;
					float x0 = touchRecord2.screenXYs[index - 1].x
							- touchRecord2.screenXYs[start].x;
					float y0 = touchRecord2.screenXYs[index - 1].y
							- touchRecord2.screenXYs[start].y;
					float x1 = touchRecord2.screenXYs[index].x
							- touchRecord2.screenXYs[start + 1].x;
					float y1 = touchRecord2.screenXYs[index].y
							- touchRecord2.screenXYs[start + 1].y;
					float vx = touchRecord2.screenXYs[start + 1].x
							- touchRecord2.screenXYs[start].x;
					float vy = touchRecord2.screenXYs[start + 1].y
							- touchRecord2.screenXYs[start].y;

					double r0 = Math.sqrt(x0 * x0 + y0 * y0);
					double r1 = Math.sqrt(x1 * x1 + y1 * y1);
					if (r0 == 0 || r1 == 0) {
						if (r0 != 0 || r1 != 0) {
							double r = Math.sqrt(vx * vx + vy * vy);
							if (Math.abs(x0 * vx + y0 * vy + x1 * vx + y1 * vy)
									/ ((r0 + r1) * r) < Cos60) {
								touchModeL = 1;// rotating
							}
						}
					} else if ((x0 * x1 + y0 * y1) / (r0 * r1) > Cos30) {
						if (Math.abs(y0) / r0 > Cos30
								&& Math.abs(y1) / r1 > Cos30)
							touchModeL = 2;// tilt
					} else if ((x0 * x1 + y0 * y1) / (r0 * r1) < -Cos30) {
						double r = Math.sqrt(vx * vx + vy * vy);
						if (Math.abs(x0 * vx + y0 * vy) / (r0 * r) < Cos60
								&& Math.abs(x1 * vx + y1 * vy) / (r1 * r) < Cos60) {
							touchModeL = 1;// rotating
						}
					}

					// Log.i("TilesView","onTouchEvent mode,x0,y0,x1,y1,vx,vy,index,start:"+touchModeL+","+x0+","+y0+","+x1+","+y1+","+vx+","+vy+","+index+","+start);

					if (touchModeL != touchMode) {
						lastDistConv = 0;
						lastDirection = null;
						lastTouchY = null;
						touchMode = touchModeL;
					}
				}
				if (touchMode == 0) {// zooming
					XYFloat xy1Conv = screenXYToScreenXYConv(event.getX(1),
							event.getY(1));
					float distXConv = xy0Conv.x - xy1Conv.x;
					float distYConv = xy0Conv.y - xy1Conv.y;
					float distConv = (float) Math.sqrt(distXConv * distXConv
							+ distYConv * distYConv);
					if (distConv < MIN_PINCH_DISTANCE * density) {
						lastDistConv = 0;
						return true;
					} else if (lastDistConv == 0) {
						lastCenterConv.x = (xy0Conv.x + xy1Conv.x) / 2;
						lastCenterConv.y = (xy0Conv.y + xy1Conv.y) / 2;
						lastDistConv = distConv;
					} else if (distConv != lastDistConv) {
						float centerDistXConv = Math
								.abs((xy0Conv.x + xy1Conv.x) / 2
										- lastCenterConv.x);
						float centerDistYConv = Math
								.abs((xy0Conv.y + xy1Conv.y) / 2
										- lastCenterConv.y);
						if ((centerDistXConv + centerDistYConv) > ABNORMAL_PINCH_CENTER_DIST
								* density) {
							lastDistConv = 0;
							LogWrapper
									.e("TilesView",
											"onTouchEvent pinch abnormal center dist:"
													+ (centerDistXConv + centerDistYConv));
						} else {
							lastCenterConv.x = (xy0Conv.x + xy1Conv.x) / 2;
							lastCenterConv.y = (xy0Conv.y + xy1Conv.y) / 2;
							float newZoomLevel = zoomLevel
									+ (float) (Math
											.log(distConv / lastDistConv) / Math
											.log(2));
							// Log.i("TilesView","onTouchEvent newZoom,lastDist,dist:"+newZoomLevel+","+lastDist+","+dist);
							lastDistConv = distConv;
							synchronized (drawingLock) {
								try {
									zoomView(newZoomLevel, lastCenterConv);
								} catch (Exception e) {
									e.printStackTrace();
									return true;
								}
							}
							refreshMap();
						}
					}
				} else if (touchMode == 1) {// rotating
					float distX = event.getX(1) - event.getX(0);
					float distY = event.getY(1) - event.getY(0);
					float dist = (float) Math.sqrt(distX * distX + distY
							* distY);
					if (dist < MIN_ROTATE_DISTANCE * density) {
						lastDirection = null;
						return true;
					}
					if (lastDirection == null) {
						lastDirection = getDirection(event.getX(0),
								event.getY(0), event.getX(1), event.getY(1));
						// tigerknows delete begin
						// }else{
						// synchronized(drawingLock){
						// XYFloat
						// newDirection=getDirection(event.getX(0),event.getY(0),event.getX(1),event.getY(1));
						// rotateZ(lastDirection,newDirection);
						// lastDirection=newDirection;
						// }
						// refreshMap();
						// tigerknows delete end
					}
				} else if (touchMode == 2) {// tilting
					if (lastTouchY == null) {
						lastTouchY = new XYFloat(event.getY(0), event.getY(1));
					} else {
						float delY0 = event.getY(0) - lastTouchY.x;
						float delY1 = event.getY(1) - lastTouchY.y;
						lastTouchY.x = event.getY(0);
						lastTouchY.y = event.getY(1);
						if (delY0 * delY1 > 0) {
							synchronized (drawingLock) {
								float delX = (delY0 + delY1) / 2
										* MapView.MAP_TILT_MIN
										/ (XROTATION_YDIST * density);
								rotateX(delX);
							}
							refreshMap();
						}
					}
				}
				return true;

			} else if (multiTouch && pCount == 1) {
				lastDistConv = 0;
				lastDirection = null;
				lastTouchY = null;
				touchRecord2.reset();
			}
			isTouchBegin = false;

		} else if (action == MotionEvent.ACTION_UP) {
			// Log.i("TilesView","onTouchEvent touchup pCount:"+pCount);

			isTouchBegin = false;
			resetLongTouchTimer();

			if (!multiTouch && pCount == 1) {
				synchronized (longTouchLock) {
					if (longClicked) {
						synchronized (touchingLock) {
							touching = false;
							touchingLock.notifyAll();
						}

						return true;
					}
				}

				long touchUpTime = System.nanoTime();
				Position position = screenXYConvToPos(xy0Conv.x, xy0Conv.y);
				infoWindow
						.setBackgroundColor(InfoWindow.BACKGROUND_COLOR_UNCLICKED);

				if (lastTouchDownTime != 0
						&& (touchUpTime - lastTouchDownTime) < CLICK_DOWN_UP_TIME_MAX
						&& (Math.abs(event.getX(0) - lastTouchDown.x) + Math
								.abs(event.getY(0) - lastTouchDown.y)) < SAME_POINT_MOVED_DISTANCE_MAX
								* density) {
					if (compass != null && compass.isVisible()) {
						if (compass.snapTo(
								new XYFloat(event.getX(0), event.getY(0)),
								displaySize, padding)) {
							compass.executeTouchListeners();
							synchronized (touchingLock) {
								touching = false;
								touchingLock.notifyAll();
							}

							return true;
						}
					}
				}

				if (infoWindowClicked) {
					XYFloat relativeXY = new XYFloat(0, 0);
					if (snapToInfoWindow(event.getX(0), event.getY(0),
							relativeXY)) {
						event.setLocation(relativeXY.x
								- InfoWindow.INFO_BORDER_SIZE, relativeXY.y
								- InfoWindow.INFO_BORDER_SIZE);
						infoWindow.executeTouchListeners(event);
						infoWindowClicked = false;
					}
					refreshMap();
					synchronized (touchingLock) {
						touching = false;
						touchingLock.notifyAll();
					}

					return true;
				}

				if (lastTouchDownTime != 0
						&& (touchUpTime - lastTouchDownTime) < CLICK_DOWN_UP_TIME_MAX
						&& (Math.abs(event.getX(0) - lastTouchDown.x) + Math
								.abs(event.getY(0) - lastTouchDown.y)) < SAME_POINT_MOVED_DISTANCE_MAX
								* density) {
					ArrayList<OverlayItem> cluster = null;
					int size = overlays.size();
					if (size > 0) {
						synchronized (drawingLock) {
							int start = new Random().nextInt(size);
							XYDouble mercXY = screenXYConvToMercXY(xy0Conv.x,
									xy0Conv.y, centerXYZ.z);
							for (int i = 0; i < size; i++) {
								ItemizedOverlay overlay = overlays
										.get((i + start) % size);
								if (!ItemizedOverlay.MY_LOCATION_OVERLAY
										.equals(overlay.getName())) {
									cluster = snapToOverlay(overlay, mercXY,
											event.getX(0), event.getY(0));
									if (cluster != null) {
										break;
									}
								}
							}

						}
					}

					if (cluster != null) {
						// Log.i("TilesView","onTouchEvent snap to:"+overlayItem.getPosition()+","+overlayItem.getMessage());
						if (cluster.size() == 0) {
							LogWrapper.e("TilesView",
									"onTouchEvent touchUp cluster size is 0");
						}
						ArrayList<OverlayItem> visiblePins = new ArrayList<OverlayItem>();
						for (int ii = 0; ii < cluster.size(); ii++) {
							OverlayItem pinL = cluster.get(ii);
							if (pinL.getIcon().getImage() != null
									&& pinL.getMercXY() != null
									&& pinL.isVisible()) {
								visiblePins.add(pinL);
							}
						}
						if (visiblePins.size() > 0) {
							ItemizedOverlay overlay = visiblePins.get(0)
									.getOwnerOverlay();
							if (overlay.isClustering()
									&& visiblePins.size() > 1) {
								if (overlay.getClusterTouchEventListener() != null) {
									overlay.getClusterTouchEventListener()
											.onTouchEvent(overlay, visiblePins);
								}
							} else {
								OverlayItem overlayItem = visiblePins
										.get(new Random().nextInt(visiblePins
												.size()));
								synchronized (drawingLock) {
									overlayItem.executeTouchListeners();
								}
							}
						}

					} else {
						/*
						 * 点击第一次InfoWindow消失, 第二次大头针消失
						 */
						if (infoWindow.isVisible()) {
							infoWindow.setVisible(false);
						} else if (mParentMapView.getCurrentOverlay() == mParentMapView
								.getOverlaysByName(ItemizedOverlay.PIN_OVERLAY)) {
							mParentMapView
									.deleteOverlaysByName(ItemizedOverlay.PIN_OVERLAY);
						}

						mParentMapView.executeTouchListeners(position);

						if (lastTouchUpTime != 0
								&& (touchUpTime - lastTouchUpTime) < DOUBLE_CLICK_INTERVAL_TIME_MAX
								&& (Math.abs(event.getX(0) - lastTouchUp.x) + Math
										.abs(event.getY(0) - lastTouchUp.y)) < SAME_POINT_MOVED_DISTANCE_MAX
										* density) {
							lastTouchUp.x = 0;
							lastTouchUp.y = 0;
							lastTouchUpTime = 0;
							mParentMapView
									.executeDoubleClickListeners(position);
						} else {
							lastTouchUp.x = event.getX(0);
							lastTouchUp.y = event.getY(0);
							lastTouchUpTime = touchUpTime;
						}
					}

					refreshMap();

					// Log.i("Thread","TilesView.onTouchEvent finish tapping notifyTilesWaitForLoading");
				} else {
					if (CONFIG.DECELERATE_RATE > 0 && touchRecord1.size >= 2) {
						float xDist = touchRecord1.screenXYs[touchRecord1.index].x
								- touchRecord1.screenXYs[(touchRecord1.index
										- (touchRecord1.size - 1) + touchRecord1.capacity)
										% touchRecord1.capacity].x;
						float yDist = touchRecord1.screenXYs[touchRecord1.index].y
								- touchRecord1.screenXYs[(touchRecord1.index
										- (touchRecord1.size - 1) + touchRecord1.capacity)
										% touchRecord1.capacity].y;
						double s = Math.sqrt(xDist * xDist + yDist * yDist);
						long timeInterval = touchRecord1.times[touchRecord1.index]
								- touchRecord1.times[(touchRecord1.index
										- (touchRecord1.size - 1) + touchRecord1.capacity)
										% touchRecord1.capacity];
						synchronized (drawingLock) {
							easingRecord.speed = s / timeInterval;
							if (easingRecord.speed > easingRecord.MAXIMUM_SPEED) {
								LogWrapper.d("Moving", "too high speed:"
										+ easingRecord.speed);
								easingRecord.speed = easingRecord.MAXIMUM_SPEED;
							}
							easingRecord.startMoveTime = touchUpTime;
							easingRecord.direction.x = (float) (xDist / s);
							easingRecord.direction.y = (float) (yDist / s);
							easingRecord.movedDistance = 0;
							easingRecord.listener = null;
						}
						// Log.i("Moving","onTouchEvent s:"+s+",t(ms):"+(int)(timeInterval/1000000));
//						LogWrapper.i("Moving","onTouchEvent speed:"+easingRecord.speed+",decelerate:"+easingRecord.decelerate_rate+",direction:"+easingRecord.direction);
					}
					if (CONFIG.DECELERATE_RATE <= 0 || easingRecord.speed <= 0) {
						Position center;
						synchronized (drawingLock) {
							easingRecord.reset();
							center = getCenterPosition();
						}
						if (center != null)
							mParentMapView.executeMoveEndListeners(center);
					}
					refreshMap();
				}
			} else if (multiTouch) {
				if (!CONFIG.SNAP_TO_CLOSEST_ZOOMLEVEL) {
					if (lastZoomLevel != zoomLevel) {
						mParentMapView.executeZoomEndListeners(Math
								.round(zoomLevel));
					}
				}

				if (lastZRotation != mapMode.getzRotation()) {
					mParentMapView.executeRotateEndListeners(mapMode
							.getzRotation());
				}

				if (lastXRotation != mapMode.getxRotation()) {
					mParentMapView.executeTiltEndListeners(mapMode
							.getxRotation());
				}
			}

			synchronized (touchingLock) {
				touching = false;
				touchingLock.notifyAll();
			}
		}

		return true;
	}

	/**
	 * main method for repaint. zoom layer and overlays have their center
	 * position stored, move canvas the distance between stored center and
	 * current map center before draw these items.
	 */
	@Override
	public void onDraw(Canvas canvas) {

	}

	public InfoWindow getInfoWindow() {
		return infoWindow;
	}

	public void setInfoWindow(InfoWindow infoWindow) {
		this.infoWindow = infoWindow;
	}

	/**
	 * centers the map on a given position and renders tiles
	 * 
	 * @param position
	 */
	public void centerOnPosition(Position position) throws APIException {
		centerOnPosition(position, zoomLevel, false);
	}

	public void centerOnPosition(Position position, float zoomLevel)
			throws APIException {
		centerOnPosition(position, zoomLevel, false);
	}

	public void centerOnPosition(Position position, float zoomLevel,
			boolean clear) throws APIException {
		if (zoomLevel < CONFIG.ZOOM_LOWER_BOUND
				|| zoomLevel > CONFIG.ZOOM_UPPER_BOUND) {
			throw new APIException("invalid zoom level: " + zoomLevel
					+ " must be between " + CONFIG.ZOOM_LOWER_BOUND + " - "
					+ CONFIG.ZOOM_UPPER_BOUND);
		}
		if (position == null) {
			return;
		}
		synchronized (drawingLock) {
			try {
				int z = Math.round(zoomLevel);
				LogWrapper.i("TilesView", "centerOnPosition z:" + z
						+ ",position:" + position);
				XYDouble centerXYL = Util.posToMercPix(position, z);
				LogWrapper.i("TilesView", "centerOnPosition centerXYL:"
						+ centerXYL);

				TileGridResponse resp = Util.handlePortrayMapRequest(centerXYL,
						z);
				LogWrapper.w("TilesView", "centerOnPosition(), centerXYL="
						+ centerXYL + ", zoomLevel= " + zoomLevel + ", clear="
						+ clear);

				this.zoomLevel = zoomLevel;
				renderMap(resp);

			} catch (Exception e) {
				overlays.clear();
				shapes.clear();
				infoWindow.setAssociatedOverlayItem(null);
				infoWindow.setVisible(false);
				this.mapPreference.routeId = null;
				this.mapPreference.realTimeTraffic = false;
				centerXY = null;
				throw APIException.wrapToAPIException(e);

			}
		}
		refreshMap();
		mParentMapView.executeMoveEndListeners(getCenterPosition());
		mParentMapView.executeZoomEndListeners(Math.round(zoomLevel));
	}

	/**
	 * hide all overlays
	 */
	public void hideOverlays() {
		for (int i = 0; i < overlays.size(); i++) {
			for (int j = 0; j < overlays.get(i).size(); j++) {
				overlays.get(i).get(j).setVisible(false);
			}
		}

	}

	/**
	 * show all overlays
	 */
	public void showOverlays() {
		for (int i = 0; i < overlays.size(); i++) {
			for (int j = 0; j < overlays.get(i).size(); j++) {
				overlays.get(i).get(j).setVisible(true);
			}
		}

	}

	public LinkedHashMap<Tile, TileInfo> getTileInfos() {
		return tileInfos;
	}

	/**
	 * moves the map to a given position
	 * 
	 * @param position
	 */
	public void panToPosition(Position position, long duration,
			MapView.MoveEndEventListener listener) throws APIException {
		if (position == null)
			return;

		boolean moveJustDown = false;
		if (duration == -1) {
			duration = MapView.PAN_TO_POSITION_TIME_DEF;
		}
		synchronized (drawingLock) {
			easingRecord.reset();
			XYDouble newMercXY = Util.posToMercPix(position, zoomLevel);
			double scale = Math.pow(2, zoomLevel - centerXYZ.z);
			float x = (float) (centerXY.x * scale - newMercXY.x);
			float y = (float) (newMercXY.y - centerXY.y * scale);
			if (CONFIG.DECELERATE_RATE > 0) {
				easingRecord.startMoveTime = System.nanoTime();
				double s = Math.sqrt(x * x + y * y);
				easingRecord.direction.x = (float) (x / s);
				easingRecord.direction.y = (float) (y / s);
				easingRecord.decelerate_rate = Math.pow(
						easingRecord.MINIMUM_SPEED_RATIO,
						easingRecord.TIME_SCALE / duration);
				easingRecord.speed = -s / easingRecord.TIME_SCALE
						* Math.log(easingRecord.decelerate_rate);
				easingRecord.listener = listener;
				LogWrapper.d("Moving", "panToPosition speed,decelerate,s:"
						+ easingRecord.speed + ","
						+ easingRecord.decelerate_rate + "," + s);

				refreshMap();

			} else {
				LogWrapper.d("Moving", "panToPosition: " + x + " , " + y);
				moveView(x, y);
				moveJustDown = true;
			}
		}

		if (moveJustDown) {
			mParentMapView.executeMoveEndListeners(position);
		}

	}

	/**
	 * calculate the current map center position
	 */
	public Position getCenterPosition() {
		if (centerXY == null)
			return null;
		return Util.mercPixToPos(centerXY, centerXYZ.z);

	}

	/**
	 * distance between center to left border of screen
	 */
	public double getRadiusX() {
		return radiusX;
	}

	/**
	 * distance between center to top border of screen
	 */
	public double getRadiusY() {
		return radiusY;
	}

	/**
	 * returns the current boundingbox for the viewable area of map on the
	 * screen, useful for clipping
	 * 
	 * @return BoundingBox
	 */
	public BoundingBox getScreenBoundingBox() {
		double scale = Math.pow(2, zoomLevel - centerXYZ.z);
		return new BoundingBox(Util.mercPixToPos(new XYDouble(centerXY.x
				* scale - displaySize.x / 2, centerXY.y * scale + displaySize.y
				/ 2), zoomLevel), Util.mercPixToPos(new XYDouble(centerXY.x
				* scale + displaySize.x / 2, centerXY.y * scale - displaySize.y
				/ 2), zoomLevel));
	}

	/**
	 * current zoom level, zoom level is represented in global explore format,
	 * higher zoom level means more zoom in
	 */
	public float getZoomLevel() {
		return zoomLevel;
	}

	public MapMode getMapMode() {
		return mapMode;
	}

	public XYInteger getDisplaySize() {
		return new XYInteger(displaySize.x, displaySize.y);
	}

	public XYFloat positionToScreenXY(Position pos) throws APIException {
		XYFloat xyConv = positionToScreenXYConv(pos);
		float xConv = xyConv.x - displaySize.x / 2;
		float yConv = xyConv.y - displaySize.y / 2;
		float cosZ = mapMode.getCosZ();
		float sinZ = mapMode.getSinZ();
		float xConv2 = cosZ * xConv - sinZ * yConv;
		float yConv2 = sinZ * xConv + cosZ * yConv;
		float cosX = mapMode.getCosX();
		float sinX = mapMode.getSinX();
		float y = yConv2 * mapMode.scale * cosX * mapMode.nearZ
				/ (mapMode.middleZ + yConv2 * mapMode.scale * sinX);
		float x = xConv2 * mapMode.scale * mapMode.nearZ
				/ (mapMode.middleZ + yConv2 * mapMode.scale * sinX);
		x += displaySize.x / 2;
		y += displaySize.y / 2;
		return new XYFloat(x, y);
	}

	public Position screenXYToPos(XYFloat screenXY) {
		XYFloat xyConv = screenXYToScreenXYConv(screenXY.x, screenXY.y);
		return screenXYConvToPos(xyConv.x, xyConv.y);
	}

	/**
	 * sets new zoom level for next request that centers the map.
	 * 
	 * @param newLevel
	 */
	public void setZoomLevel(float newLevel) throws APIException {
		newLevel = tkJumpZoomLevel(newLevel);
		if (newLevel < CONFIG.ZOOM_LOWER_BOUND
				&& newLevel > CONFIG.ZOOM_UPPER_BOUND) {
			throw new APIException("invalid zoom level: " + newLevel
					+ " must be between " + CONFIG.ZOOM_LOWER_BOUND + " - "
					+ CONFIG.ZOOM_UPPER_BOUND);
		}
		if (centerXY == null || displaySize == null || displaySize.x == 0
				|| displaySize.y == 0) {
			zoomLevel = newLevel;
		} else {
			zoomView(newLevel,
					new XYFloat(displaySize.x / 2, displaySize.y / 2));
		}

	}

	public void zoomTo(int newZoomLevel, final Position zoomCenter,
			long duration, final MapView.ZoomEndEventListener listener)
			throws APIException {
		newZoomLevel = (int) tkJumpZoomLevel(newZoomLevel);
		if (newZoomLevel < CONFIG.ZOOM_LOWER_BOUND
				|| newZoomLevel > CONFIG.ZOOM_UPPER_BOUND) {
			throw new APIException("invalid zoom level: " + newZoomLevel
					+ " must be between " + CONFIG.ZOOM_LOWER_BOUND + " - "
					+ CONFIG.ZOOM_UPPER_BOUND);
		}
		final XYFloat zc = new XYFloat(displaySize.x / 2, displaySize.y / 2);
		if (zoomCenter != null) {
			XYFloat screenXY = positionToScreenXYConv(zoomCenter);
			try {
				if (screenXY.equals(zc)) {
					if (duration == -1) {
						duration = Math
								.round(MapView.DIGITAL_ZOOMING_TIME_PER_LEVEL
										* Math.abs(newZoomLevel - zoomLevel));
					}
					zoomTo(newZoomLevel, zc, duration, listener);
				} else {
					final int finalnewZoomLevel = newZoomLevel;
					panToPosition(zoomCenter, duration,
							new MoveEndEventListener() {

								@Override
								public void onMoveEndEvent(MapView mapView,
										Position position) {
									try {
										centerOnPosition(zoomCenter,
												finalnewZoomLevel);
										if (listener != null) {
											listener.onZoomEndEvent(mapView,
													finalnewZoomLevel);
										}
									} catch (APIException e) {
										e.printStackTrace();
									}
								}
							});
				}
			} catch (APIException e) {
				e.printStackTrace();
			}
		} else if ((zoomLevel > CONFIG.ZOOM_JUMP && newZoomLevel == CONFIG.ZOOM_JUMP - 1)
				|| (zoomLevel < CONFIG.ZOOM_JUMP && newZoomLevel == CONFIG.ZOOM_JUMP + 1)) {
			setZoomLevel(newZoomLevel);
			refreshMap();
			if (listener != null) {
				listener.onZoomEndEvent(this.mParentMapView, newZoomLevel);
			}
			mParentMapView.executeZoomEndListeners(newZoomLevel);
		} else {
			if (duration == -1) {
				duration = Math.round(MapView.DIGITAL_ZOOMING_TIME_PER_LEVEL
						* Math.abs(newZoomLevel - zoomLevel));
			}
			zoomTo(newZoomLevel, zc, duration, listener);
		}
	}

	// int tileSize=g_config.TILE_SIZE;
	// if(ABS(_centerDelta.x)>tileSize || ABS(_centerDelta.y)>tileSize){
	// int numX=round(_centerDelta.x/tileSize);
	// int numY=round(_centerDelta.y/tileSize);
	// _centerXYZ.x-=numX;
	// _centerXYZ.x=[deCartaUtil indexXMod:_centerXYZ.x atZoom:_centerXYZ.z];
	// _centerXYZ.y+=numY;
	// _centerDelta.x-=(numX*tileSize);
	// _centerDelta.y-=(numY*tileSize);
	//
	// _panDirection.x=numX>=0?1:-1;
	// _panDirection.y=numY>=0?1:-1;
	// }
	private void adjustCenter() {
		int thresh = CONFIG.TILE_SIZE / 2;
		if (Math.abs(centerDelta.x) > thresh
				|| Math.abs(centerDelta.y) > thresh) {
			int numX = Math.round(centerDelta.x / CONFIG.TILE_SIZE);
			int numY = Math.round(centerDelta.y / CONFIG.TILE_SIZE);
			centerXYZ.x -= numX;
			centerXYZ.x = Util.indexXMod(centerXYZ.x, centerXYZ.z);
			centerXYZ.y += numY;
			centerDelta.x -= (numX * CONFIG.TILE_SIZE);
			centerDelta.y -= (numY * CONFIG.TILE_SIZE);
			// try {
			// TileGridResponse resp= Util.handlePortrayMapRequest(new
			// XYDouble(centerXY.x,centerXY.y), (int)zoomLevel);
			// if (resp.centerXY.equals(centerXY)) {
			// centerDelta.y = resp.getFixedGridPixelOffset().y;
			// }
			// } catch (APIException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			panDirection.x = numX >= 0 ? 1 : -1;
			panDirection.y = numY >= 0 ? 1 : -1;
		}
		LogWrapper.i("center", centerXYZ.toString() + "......" + centerXY.toString() + "......" + centerDelta.toString());
	}

	public void zoomView(float newZoomLevel, XYFloat zoomCenterXY)
			throws APIException {
		newZoomLevel = tkJumpZoomLevel(newZoomLevel);
		if (zooming || newZoomLevel == zoomLevel)
			return;
		if (newZoomLevel < CONFIG.ZOOM_LOWER_BOUND
				|| newZoomLevel > CONFIG.ZOOM_UPPER_BOUND) {
			LogWrapper.d("TilesView", "zoomTo invalid zoom level:"
					+ newZoomLevel + ", must between "
					+ CONFIG.ZOOM_LOWER_BOUND + "-" + CONFIG.ZOOM_UPPER_BOUND);
			return;
		}
		try {
			zooming = true;
			isManuelZoom = true;
			if (zoomCenterXY.x != displaySize.x / 2
					|| zoomCenterXY.y != displaySize.y / 2) {
				// Log.i("TilesView.zoomView","zoomCenter:"+zoomCenterXY.x+","+zoomCenterXY.y+",displaySize:"+displaySize.x+","+displaySize.y);
				double deltaX = (zoomCenterXY.x - displaySize.x / 2)
						* (Math.pow(2, newZoomLevel - zoomLevel) - 1);
				float moveX = -(float) (deltaX * Math.pow(2, centerXYZ.z
						- newZoomLevel));
				double deltaY = (zoomCenterXY.y - displaySize.y / 2)
						* (Math.pow(2, newZoomLevel - zoomLevel) - 1);
				float moveY = -(float) (deltaY * Math.pow(2, centerXYZ.z
						- newZoomLevel));

				if (!Util.inChina(new XYDouble(centerXY.x - moveX, centerXY.y
						+ moveY), centerXYZ.z)) {
					return;
				}

				centerXY.x -= moveX;
				centerXY.x = Util.mercXMod(centerXY.x, centerXYZ.z);

				if (moveY > 0) {
					if (centerXY.y >= Util.MERC_X_MODS[centerXYZ.z])
						moveY = 0;
					else if (centerXY.y + moveY > Util.MERC_X_MODS[centerXYZ.z])
						moveY = (float) (Util.MERC_X_MODS[centerXYZ.z] - centerXY.y);
				} else if (moveY < 0) {
					if (centerXY.y <= -Util.MERC_X_MODS[centerXYZ.z])
						moveY = 0;
					else if (centerXY.y + moveY < -Util.MERC_X_MODS[centerXYZ.z])
						moveY = (float) (-Util.MERC_X_MODS[centerXYZ.z] - centerXY.y);
				}

				centerXY.y += moveY;
				centerDelta.x += moveX;
				centerDelta.y += moveY;

				adjustCenter();
			}

			zoomLevel = newZoomLevel;

			if (zoomLevel > centerXYZ.z + 0.5 || zoomLevel < centerXYZ.z - 0.5) {
				int roundLevel = Math.round(zoomLevel);
				if (roundLevel > CONFIG.ZOOM_UPPER_BOUND
						|| roundLevel < CONFIG.ZOOM_LOWER_BOUND)
					return;

				// clearTilesWaitForLoading();
				for (int i = 0; i < mapLayers.size(); i++) {
					MapLayer mapLayer = mapLayers.get(i);
					if (!mapLayer.visible)
						continue;
					// Log.i("TilesView.zoomView","mapLayer percent,level;zoomLayer percent,level:"+mapLayer.mainLayerDrawPercent+","+centerXYZ.z+";"+mapLayer.zoomLayerDrawPercent+","+mapLayer.centerXYZ.z);

					double factorMain = 1.0f;
					double factorZoom = 1.0f;
					if (roundLevel > mapLayer.centerXYZ.z) {
						factorZoom = Math.pow(ZOOM_PENALTY, roundLevel
								- mapLayer.centerXYZ.z);
						if (roundLevel > centerXYZ.z) {
							factorMain = Math.pow(ZOOM_PENALTY, roundLevel
									- centerXYZ.z);
						}
					}
					if (mapLayer.mainLayerDrawPercent >= CLONE_MAP_LAYER_DRAW_PERCENT
							|| mapLayer.centerXY == null
							|| mapLayer.mainLayerDrawPercent * factorMain >= mapLayer.zoomLayerDrawPercent
									* factorZoom) {
						mapLayer.centerXY = new XYDouble(centerXY.x, centerXY.y);
						mapLayer.centerXYZ = new XYZ(centerXYZ.x, centerXYZ.y,
								centerXYZ.z);
						mapLayer.centerDelta = new XYFloat(centerDelta.x,
								centerDelta.y);
						mapLayer.zoomLayerDrawPercent = mapLayer.mainLayerDrawPercent;
						mapLayer.mainLayerDrawPercent = 0;
					}
				}

				double mapScale = Math.pow(2, roundLevel - centerXYZ.z);
				TileGridResponse resp = Util.handlePortrayMapRequest(
						new XYDouble(centerXY.x * mapScale, centerXY.y
								* mapScale), roundLevel);
				renderMap(resp);
				fadingStartTime = System.nanoTime();
				zooming = false;
			}

		} catch (Exception e) {
			throw APIException.wrapToAPIException(e);
		} finally {
			zooming = false;
		}

	}

	/**
	 * move all tiles as if they are inside one container
	 * 
	 * @param left
	 * @param top
	 * 
	 */
	public void moveView(float left, float top) {
		if (zooming)
			return;

		if (centerXY == null) {
			return;
		}
		double mapScale = Math.pow(2, centerXYZ.z - zoomLevel);
		float moveX = left * (float) mapScale;
		float moveY = top * (float) (mapScale);

		if (!Util.inChina(new XYDouble(centerXY.x - moveX, centerXY.y + moveY),
				centerXYZ.z)) {
//			LogWrapper.i("Moving", "out of China: centerXY: " + centerXY.toString() + "moveX: " + moveX + "moveY: " + moveY);
			return;
		}

		centerXY.x -= moveX;
		centerXY.x = Util.mercXMod(centerXY.x, centerXYZ.z);

		if (moveY > 0) {
			if (centerXY.y >= Util.MERC_X_MODS[centerXYZ.z])
				moveY = 0;
			else if (centerXY.y + moveY > Util.MERC_X_MODS[centerXYZ.z])
				moveY = (float) (Util.MERC_X_MODS[centerXYZ.z] - centerXY.y);
		} else if (moveY < 0) {
			if (centerXY.y <= -Util.MERC_X_MODS[centerXYZ.z])
				moveY = 0;
			else if (centerXY.y + moveY < -Util.MERC_X_MODS[centerXYZ.z])
				moveY = (float) (-Util.MERC_X_MODS[centerXYZ.z] - centerXY.y);
		}

		centerXY.y += moveY;

		centerDelta.x += moveX;
		centerDelta.y += moveY;

		adjustCenter();

	}

	public LinkedList<Tile> getTilesWaitForLoading() {
		return tilesWaitForLoading;
	}

	public List<ItemizedOverlay> getOverlays() {
		return overlays;
	}

	public List<Shape> getShapes() {
		return shapes;
	}

	public Object getDrawingLock() {
		return drawingLock;
	}

	public MapType getMapType() {
		return mapType;
	}

	public Compass getCompass() {
		return compass;
	}

	public void setCompass(Compass compass) {
		this.compass = compass;
	}

	// end of normal methods

	/******************************************** private methods ***************************************************/
	private void configureTileGrid(int w, int h) {
		displaySize.x = w;
		displaySize.y = h;

		// gridSize.x=(int) Math.ceil(displaySize.x/(float)CONFIG.TILE_SIZE) +
		// 2+ TILE_BUFFER;
		// if(gridSize.x%2==0) gridSize.x++;
		// gridSize.y=(int) Math.ceil(displaySize.y/(float)CONFIG.TILE_SIZE) +
		// 2+ TILE_BUFFER;
		// if(gridSize.y%2==0) gridSize.y++;

		// offset.x = (float)(displaySize.x - (CONFIG.TILE_SIZE * gridSize.x)) /
		// 2;
		// offset.y = (float)(displaySize.y - (CONFIG.TILE_SIZE * gridSize.y)) /
		// 2;
		// LogWrapper.i("TilesView","configureTileGrid displaySize,offset,gridSize:"+displaySize+","+offset+","+gridSize);

		mapMode.configViewDepth(displaySize);
		mapMode.configViewSize(displaySize);

	}

	private void configureMapLayer() {
		float sizeFactor = 0;
		visibleLayerNum = 0;
		for (int i = 0; i < mapLayers.size(); i++) {
			MapLayer mapLayer = mapLayers.get(i);
			MapLayerProperty mapLayerProperty = mapLayer.getMapLayerProperty();
			if (MapView.MapType_MapLayer_Visibility.get(mapType).contains(
					mapLayerProperty.mapLayerType)) {
				mapLayer.visible = true;
				if (mapLayerProperty.tileImageSizeFactor > sizeFactor)
					sizeFactor = mapLayerProperty.tileImageSizeFactor;
				visibleLayerNum++;
			} else {
				mapLayer.visible = false;
			}
		}

		LogWrapper.i("TilesView",
				"configureMapLayer visible num,max image,image size,max texture ref:"
						+ visibleLayerNum + ",");
	}

	private XYFloat screenXYToScreenXYConv(float left, float top) {
		if (mapMode.getzRotation() == 0 && mapMode.getxRotation() == 0) {
			return new XYFloat(left, top);
		}

		left -= displaySize.x / 2;
		top -= displaySize.y / 2;
		float cosX = mapMode.getCosX();
		float sinX = mapMode.getSinX();
		float y0 = (mapMode.middleZ * top)
				/ (mapMode.nearZ * mapMode.scale * cosX - top * mapMode.scale
						* sinX);
		float x0 = (left * mapMode.middleZ + left * mapMode.scale * y0 * sinX)
				/ (mapMode.nearZ * mapMode.scale);
		float cosZ = mapMode.getCosZ();
		float sinZ = -mapMode.getSinZ();
		float x = cosZ * x0 - sinZ * y0;
		float y = sinZ * x0 + cosZ * y0;
		x += displaySize.x / 2;
		y += displaySize.y / 2;

		return new XYFloat(x, y);
	}

	private XYFloat getDirection(float x0, float y0, float x1, float y1) {
		float vecX = x0 - x1;
		float vecY = y0 - y1;
		float mag = (float) Math.sqrt(vecX * vecX + vecY * vecY);
		if (mag == 0)
			return new XYFloat(0, 0);
		return new XYFloat(vecX / mag, vecY / mag);
	}

	private void rotateZ(XYFloat o, XYFloat n) {
		if ((o.x == 0 && o.y == 0) || (n.x == 0 && n.y == 0))
			return;

		double sin = (o.x * n.y - o.y * n.x);
		double asin = Math.asin(sin) * 180 / Math.PI;
		// Log.i("TilesView","rotateZ asin,o,n:"+asin+","+o+","+n);

		if (Math.abs(asin) > ABNORMAL_ZROTATION) {
			LogWrapper.e("TilesView", "rotateZ asin abnormal:" + asin);
			return;
		}

		float r = mapMode.getzRotation();
		r += (float) asin;
		r = ((r + 180) % 360 + 360) % 360 - 180;

		mapMode.setZRotation(r, displaySize);

	}

	private void rotateX(float delXRotation) {
		if (!CONFIG.DRAW_BY_OPENGL)
			return;
		float r = mapMode.getxRotation();
		r += (delXRotation);
		if (r > 0) {
			r = 0;
		} else if (r < MapView.MAP_TILT_MIN) {
			r = MapView.MAP_TILT_MIN;
		}
		mapMode.setXRotation(r, displaySize);
	}

	private XYDouble screenXYConvToMercXY(float convX, float convY,
			float zoomLevel) {
		double scale2 = Math.pow(2, this.zoomLevel - centerXYZ.z);
		double x = centerXY.x * scale2 - displaySize.x / 2 + convX;
		double y = centerXY.y * scale2 + displaySize.y / 2 - convY;
		double scale1 = Math.pow(2, zoomLevel - this.zoomLevel);
		return new XYDouble(x * scale1, y * scale1);
	}

	public XYFloat mercXYToScreenXYConv(XYDouble mercXY, float zoomLevel) {
		double scale1 = Math.pow(2, this.zoomLevel - zoomLevel);
		double scale2 = Math.pow(2, this.zoomLevel - centerXYZ.z);
		double x = mercXY.x * scale1 - centerXY.x * scale2 + displaySize.x / 2;
		double y = centerXY.y * scale2 + displaySize.y / 2 - mercXY.y * scale1;
		return new XYFloat((float) x, (float) y);
	}

	private ArrayList<OverlayItem> snapToOverlay(ItemizedOverlay overlay,
			XYDouble mercXY, float screenX, float screenY) {
		ArrayList<Tile> touchTiles = Util.getTouchTiles(mercXY, centerXYZ.z,
				ItemizedOverlay.TOUCH_RADIUS);
		ArrayList<ArrayList<OverlayItem>> clusters = overlay.getVisiblePins(
				centerXYZ.z, touchTiles);
		int size = clusters.size();
		if (size == 0)
			return null;

		int start = new Random().nextInt(size);
		float cosZ = mapMode.getCosZ();
		float sinZ = mapMode.getSinZ();
		float buffer = ItemizedOverlay.SNAP_BUFFER * Globals.g_metrics.density;
		for (int i = 0; i < size; i++) {
			ArrayList<OverlayItem> cluster = clusters.get((i + start) % size);
			if (cluster.size() == 0) {
				LogWrapper
						.e("ItemizedOverlay", "onSnapToItem cluster is empty");
			}
			OverlayItem pin = null;
			for (int ii = 0; ii < cluster.size(); ii++) {
				OverlayItem pinL = cluster.get(ii);
				if (pinL.getIcon().getImage() != null
						&& pinL.getMercXY() != null && pinL.isVisible()) {
					pin = pinL;
					break;
				}
			}
			if (pin == null)
				continue;

			XYFloat xyConv = mercXYToScreenXYConv(pin.getMercXY(),
					ItemizedOverlay.ZOOM_LEVEL);
			float x1 = xyConv.x - displaySize.x / 2;
			float y1 = xyConv.y - displaySize.y / 2;
			float x2 = cosZ * x1 - sinZ * y1;
			float y2 = sinZ * x1 + cosZ * y1;
			y2 *= (mapMode.scale);
			x2 *= (mapMode.scale);
			float sinX = mapMode.getSinX();
			float cosX = mapMode.getCosX();
			float z = y2 * sinX + mapMode.middleZ;
			y2 *= (cosX);

			RotationTilt rt = pin.getRotationTilt();
			float cosT = rt.getCosT();
			float sinT = rt.getSinT();
			if (rt.getTiltRelativeTo().equals(TiltReference.MAP)) {
				cosT = rt.getCosT() * mapMode.getCosX() - rt.getSinT()
						* mapMode.getSinX();
				sinT = rt.getSinT() * mapMode.getCosX() + rt.getCosT()
						* mapMode.getSinX();
			}
			float cosR = rt.getCosR();
			float sinR = rt.getSinR();
			if (rt.getRotateRelativeTo().equals(RotateReference.MAP)) {
				cosR = rt.getCosR() * mapMode.getCosZ() - rt.getSinR()
						* mapMode.getSinZ();
				sinR = rt.getSinR() * mapMode.getCosZ() + rt.getCosR()
						* mapMode.getSinZ();
			}
			float xt = (screenX - displaySize.x / 2);
			float yt = (screenY - displaySize.y / 2);
			float yd = (yt * z - y2 * mapMode.nearZ)
					/ (mapMode.nearZ * cosT - yt * sinT);
			float xd = xt * (z + yd * sinT) / mapMode.nearZ - x2;

			float xTouch2 = cosR * xd + sinR * yd;
			float yTouch2 = -sinR * xd + cosR * yd;

			float scaleToM = mapMode.middleZ / (float) mapMode.nearZ;
			// yTouch2/=mapMode.scale;
			// xTouch2/=mapMode.scale;
			yTouch2 /= scaleToM;
			xTouch2 /= scaleToM;

			XYInteger pinSize = pin.getIcon().getSize();
			XYInteger offset = pin.getIcon().getOffset();
			if (xTouch2 < -offset.x + pinSize.x + buffer
					&& xTouch2 > -offset.x - buffer
					&& yTouch2 < -offset.y + pinSize.y + buffer
					&& yTouch2 > -offset.y - buffer) {
				return cluster;
			}
		}
		return null;
	}

	private boolean snapToInfoWindow(float screenX, float screenY,
			XYFloat relativeXY) {
		float scaleToM = mapMode.middleZ / (float) mapMode.nearZ;

		XYFloat xyConv = mercXYToScreenXYConv(infoWindow.getMercXY(),
				InfoWindow.ZOOM_LEVEL);
		float x1 = xyConv.x - displaySize.x / 2;
		float y1 = xyConv.y - displaySize.y / 2;
		float cosZ = mapMode.getCosZ();
		float sinZ = mapMode.getSinZ();
		float x2 = cosZ * x1 - sinZ * y1;
		float y2 = sinZ * x1 + cosZ * y1;
		y2 *= (mapMode.scale);
		x2 *= (mapMode.scale);
		float sinX = mapMode.getSinX();
		float cosX = mapMode.getCosX();
		float z = y2 * sinX + mapMode.middleZ;
		y2 *= (cosX);

		RotationTilt rt = infoWindow.getOffsetRotationTilt();
		float cosR = rt.getCosR();
		float sinR = rt.getSinR();
		if (rt.getRotateRelativeTo().equals(RotateReference.MAP)) {
			cosR = rt.getCosR() * mapMode.getCosZ() - rt.getSinR()
					* mapMode.getSinZ();
			sinR = rt.getSinR() * mapMode.getCosZ() + rt.getCosR()
					* mapMode.getSinZ();
		}
		float xOff = -infoWindow.getOffset().x;
		float yOff = -infoWindow.getOffset().y;
		float xOff2 = cosR * xOff - sinR * yOff;
		float yOff2 = sinR * xOff + cosR * yOff;
		// xOff2*=(mapMode.scale);
		// yOff2*=(mapMode.scale);
		xOff2 *= scaleToM;
		yOff2 *= scaleToM;

		float cosT = rt.getCosT();
		float sinT = rt.getSinT();
		if (rt.getTiltRelativeTo().equals(TiltReference.MAP)) {
			cosT = rt.getCosT() * mapMode.getCosX() - rt.getSinT()
					* mapMode.getSinX();
			sinT = rt.getSinT() * mapMode.getCosX() + rt.getCosT()
					* mapMode.getSinX();
		}
		z += yOff2 * sinT;
		y2 += yOff2 * cosT;
		x2 += xOff2;

		float yTouch = (screenY - displaySize.y / 2) * z / mapMode.nearZ;
		float xTouch = (screenX - displaySize.x / 2) * z / mapMode.nearZ;
		yTouch -= y2;
		xTouch -= x2;
		// yTouch/=mapMode.scale;
		// xTouch/=mapMode.scale;
		yTouch /= scaleToM;
		xTouch /= scaleToM;

		RectF rect = infoWindow.getRect();
		if (relativeXY != null) {
			relativeXY.x = xTouch - rect.left;
			relativeXY.y = yTouch - rect.top;
		}
		if (xTouch > rect.left && xTouch < rect.right && yTouch > rect.top
				&& yTouch < rect.bottom) {
			return true;
		}

		return false;
	}

	// status[0]:moving, status[1]:movingJustDone, status[2]:zooming,
	// status[3]:zoomingJustDone
	// status[4]:rotatingZ, status[5]:rotatingZJustDone, status[6]:rotatingX
	// status[7]:rotatingXJustDone
	private void updateViewBeforeDraw(boolean status[]) {
		if (CONFIG.DRAW_BY_OPENGL && mapMode.xRotating) {
			long currentTime = System.nanoTime();
			float newRotationX;

			if (currentTime < mapMode.xRotationEndTime) {
				float rotateDirection = mapMode.xRotationEnd > mapMode
						.getxRotation() ? Math.abs(MapView.MAP_TILT_MIN)
						: -Math.abs(MapView.MAP_TILT_MIN);
				newRotationX = mapMode.xRotationEnd
						- (float) (mapMode.xRotationEndTime - currentTime)
						/ XROTATION_TIME * rotateDirection;
				status[6] = true;
			} else {
				newRotationX = mapMode.xRotationEnd;
				status[7] = true;
			}
			// Log.i("TilesView","onDraw rotatingX currentTime,endTime,newRotatinX,oldRotationX:"+currentTime/1000000+","+mapModeRecord.xRotationEndTime/1000000+","+newRotationX+","+mapModeRecord.getxRotation());

			lastXRotation = mapMode.getxRotation();
			mapMode.setXRotation(newRotationX, displaySize);

			if (currentTime >= mapMode.xRotationEndTime) {
				mapMode.resetXEasing();
			}
		}

		if (mapMode.zRotating) {
			long currentTime = System.nanoTime();
			float newRotationZ;

			if (currentTime < mapMode.zRotationEndTime) {
				float diff = mapMode.zRotationEnd - mapMode.getzRotation();
				if (diff > 180)
					diff -= 360;
				else if (diff < -180)
					diff += 360;
				float rotateDirection = (diff > 0 ? 180 : -180);
				newRotationZ = mapMode.zRotationEnd
						- (float) (mapMode.zRotationEndTime - currentTime)
						/ ZROTATION_TIME * rotateDirection;
				status[4] = true;
			} else {
				newRotationZ = mapMode.zRotationEnd;
				status[5] = true;
			}
			// Log.i("TilesView","onDraw rotatingZ currentTime,endTime,newRotatinZ,oldRotationZ:"+currentTime/1000000+","+mapModeRecord.zRotationEndTime/1000000+","+newRotationZ+","+mapModeRecord.getzRotation());
			lastZRotation = mapMode.getzRotation();
			mapMode.setZRotation(newRotationZ, displaySize);

			if (currentTime >= mapMode.zRotationEndTime) {
				mapMode.resetZEasing();
			}
		}

		if (zoomingRecord.digitalZooming) {
			long currentTime = System.nanoTime();
			float newZoomLevel;

			long timeLeft = zoomingRecord.digitalZoomEndTime - currentTime;
			if (timeLeft > 0) {
				// int zoomDirection=zoomingRecord.zoomToLevel>zoomLevel?1:-1;
				// newZoomLevel=zoomingRecord.zoomToLevel-(float)(zoomingRecord.digitalZoomEndTime-currentTime)/MapView.DIGITAL_ZOOMING_TIME_PER_LEVEL*zoomDirection;
				newZoomLevel = zoomingRecord.zoomToLevel
						- (float) (zoomingRecord.speed * timeLeft);
				status[2] = true;
			} else {
				newZoomLevel = zoomingRecord.zoomToLevel;
				status[3] = true;
			}
			// Log.i("TilesView.onDraw.zoomView","currentTime,endTime,newLevel,oldLevel:"+currentTime/1000000+","+zoomingRecord.digitalZoomEndTime/1000000+","+newZoomLevel+","+zoomLevel);
			try {
				zoomView(newZoomLevel, zoomingRecord.zoomCenterXY);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (currentTime >= zoomingRecord.digitalZoomEndTime) {
				zoomingRecord.reset();
			}

		} else if (CONFIG.DECELERATE_RATE > 0 && easingRecord.speed > 0) {
			long currentTime = System.nanoTime();
			long timeElapsed = currentTime - easingRecord.startMoveTime;
			double newSpeed = easingRecord.speed
					* Math.pow(easingRecord.decelerate_rate, timeElapsed
							/ easingRecord.TIME_SCALE);
			double distance = 0;
			// if(newSpeed<=easingRecord.speed*easingRecord.MINIMUM_SPEED_RATIO){
			if (newSpeed <= easingRecord.CUTOFF_SPEED) {
				distance = -easingRecord.TIME_SCALE
						* (easingRecord.speed - newSpeed)
						/ Math.log(easingRecord.decelerate_rate)
						- easingRecord.movedDistance;
				newSpeed = 0;
				status[1] = true;
			} else {
				distance = -easingRecord.TIME_SCALE
						* (easingRecord.speed - newSpeed)
						/ Math.log(easingRecord.decelerate_rate)
						- easingRecord.movedDistance;
				easingRecord.movedDistance += (float) distance;
				status[0] = true;
			}
			LogWrapper.i("centerXY","centerXY"+centerXY.toString());
			moveView((float) (distance) * easingRecord.direction.x,
					(float) (distance) * easingRecord.direction.y);
//			LogWrapper.i("centerXY","centerXY after move distance: " + distance + ", " + centerXY.toString());
			if (newSpeed <= 0) {
				easingRecord.reset();
			}
//			LogWrapper.i("Moving","TilesView.onDraw distance:"+(int)distance+",timeElapsed:"+(int)(timeElapsed/(1E6))+
//					",newSpeed:"+newSpeed + "decelerate: " + easingRecord.decelerate_rate);
		}
	}

	/**
	 * retrieve the screen coordinate from the lat lon based on current center
	 * and zoom level
	 * 
	 * @param pos
	 * 
	 */
	private XYFloat positionToScreenXYConv(Position pos) throws APIException {
		XYDouble mercXY = Util.posToMercPix(pos, zoomLevel);
		return mercXYToScreenXYConv(mercXY, zoomLevel);
	}

	/**
	 * retrieve the lat lon from the screen coordinate
	 * 
	 * @param screenXY
	 * 
	 */
	private Position screenXYConvToPos(float sx, float sy) {
		XYDouble xy = screenXYConvToMercXY(sx, sy, zoomLevel);
		return Util.mercPixToPos(xy, zoomLevel);
	}

	private void zoomTo(int newZoomLevel, XYFloat zoomCenterXYConv,
			long duration, MapView.ZoomEndEventListener listener) {
		newZoomLevel = (int) tkJumpZoomLevel(newZoomLevel);
		synchronized (drawingLock) {
			float zDif = newZoomLevel - zoomLevel;
			if (zDif != 0 && duration == 0) {
				LogWrapper.w("TilesView", "zoom from " + zoomLevel + " to "
						+ newZoomLevel + " duration==0");
				return;
			}
			zoomingRecord.digitalZooming = true;
			zoomingRecord.digitalZoomEndTime = System.nanoTime() + duration;
			zoomingRecord.speed = (duration == 0) ? 0 : zDif / duration;
			zoomingRecord.zoomToLevel = newZoomLevel;
			zoomingRecord.zoomCenterXY = zoomCenterXYConv;

			zoomingRecord.listener = listener;
			refreshMap();
		}
	}
	
	public void zoomInAtPosition(Position position) {
		int newZoom = Math.round(zoomLevel) + 1;
		long duration = Math.round(MapView.DIGITAL_ZOOMING_TIME_PER_LEVEL
				* Math.abs(newZoom - zoomLevel));
		try {
			XYFloat screenXY = positionToScreenXYConv(position);
			this.zoomTo(newZoom, screenXY, duration, null);
		} catch (Exception e) {
			LogWrapper.e("TilesView", "zoomInAtPosition to " + newZoom + "failed. Cause exception: " + e.getMessage());
		}
	}

//-(void)zoomInAtPosition:(deCartaPosition *)zoomCenter{
//	double duration=DIGITAL_ZOOMING_TIME_PER_LEVEL*ABS(roundf(_zoomLevel)+1-_zoomLevel);
//    
//    deCartaXYFloat * c=[self positionToScreenXYConv:zoomCenter];
//	[self zoomTo:roundf(_zoomLevel)+1 center:c duration:duration listener:nil];
//}
	/**
	 * initilize/reset tiles, set coordinate for each tile, put tiles for
	 * loading queue
	 * 
	 * @param resp
	 */
	private void renderMap(TileGridResponse resp) throws APIException {
		// Log.i("Sequence","TilesView renderMap start");
		try {
			easingRecord.reset();

			centerXY = new XYDouble(resp.centerXY.x, resp.centerXY.y);
			centerXYZ.x = resp.centerXYZ.x;

			centerXYZ.y = resp.centerXYZ.y;
			centerXYZ.z = resp.centerXYZ.z;
			centerDelta.x = resp.getFixedGridPixelOffset().x;
			centerDelta.y = resp.getFixedGridPixelOffset().y;

			this.radiusY = resp.getRadiusY().toMeters() * displaySize.y
					/ CONFIG.TILE_SIZE;
			this.radiusX = resp.getRadiusY().toMeters() * displaySize.x
					/ CONFIG.TILE_SIZE;

			for (int i = 0; i < mapLayers.size(); i++) {
				MapLayer mapLayer = mapLayers.get(i);
				mapLayer.mainLayerDrawPercent = 0;
			}

			// refreshTileRequests(1,1);
		} catch (Exception e) {
			throw APIException.wrapToAPIException(e);
		}

	}

	private void resetLongTouchTimer() {
		synchronized (longTouchLock) {
			if (longTouchTimer != null) {
				longTouchTimer.cancel();
				longTouchTimer.purge();
				longTouchTimer = null;
			}
		}
	}

	private void setupLongTouchTimer(final XYFloat xy0Conv) {
		synchronized (longTouchLock) {
			longTouchTimer = new Timer();
			longTouchTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					synchronized (longTouchLock) {
						longClicked = true;
						Position pos = TilesView.this.screenXYConvToPos(
								xy0Conv.x, xy0Conv.y);
						TilesView.this.mParentMapView
								.executeLongClickListeners(pos);
						resetLongTouchTimer();
					}
				}
			}, LONG_TOUCH_TIME_MIN / 1000000);
		}
	}

	/******************************************** classes belong to TilesView ***************************************/
	/**
	 * add the MapPreference class for map preference, it contains routeId and
	 * will be used when composing new portraymap request
	 * 
	 */
	public class MapPreference {
		// add the routeId to the map

		private String routeId;
		// real Time Traffic
		private boolean realTimeTraffic;

		public boolean isRealTimeTraffic() {
			return realTimeTraffic;
		}

		public void setRealTimeTraffic(boolean realTimeTraffic) {
			this.realTimeTraffic = realTimeTraffic;
		}

		public String getRouteId() {
			return routeId;
		}

		public void setRouteId(String routeId) {
			this.routeId = routeId;
		}
	}

	/**
	 * structure to store moving points
	 * 
	 */
	public class TouchRecord {
		private int capacity = 0;
		private long[] times;
		private XYFloat[] screenXYs;
		int index = -1;
		int size = 0;

		public TouchRecord(int capacity) {
			this.capacity = capacity;
			times = new long[capacity];
			screenXYs = new XYFloat[capacity];
			for (int i = 0; i < capacity; i++) {
				times[i] = 0;
				screenXYs[i] = new XYFloat(0f, 0f);
			}
		}

		public void push(long time, float x, float y) {
			index = (index + 1) % capacity;
			screenXYs[index].x = x;
			screenXYs[index].y = y;
			times[index] = time;

			size++;
			if (size > capacity)
				size = capacity;
		}

		public void reset() {
			index = -1;
			size = 0;
		}
	}

	/**
	 * 
	 * structure to store easing related parameters
	 * 
	 */
	public class EasingRecord {
		public double TIME_SCALE = 33 * 1E6; // 33 miniseconds
		public double MAXIMUM_SPEED = 6.8E-6;
		public double MINIMUM_SPEED_RATIO = 0.001; // ratio to original speed
		public double CUTOFF_SPEED = 200 * 1E-9;

		public double decelerate_rate = CONFIG.DECELERATE_RATE;
		public double speed = 0;
		public long startMoveTime = 0;
		public float movedDistance = 0;
		public XYFloat direction = new XYFloat(0f, 0f);

		public MapView.MoveEndEventListener listener = null;

		public void reset() {
			decelerate_rate = CONFIG.DECELERATE_RATE;
			speed = 0;
			synchronized (this) {
				this.notifyAll();
			}
			startMoveTime = 0;
			movedDistance = 0;
			direction.x = 0f;
			direction.y = 0f;

			listener = null;
		}
	}

	public class ZoomingRecord {
		public int zoomToLevel = -1;
		public long digitalZoomEndTime = 0;
		double speed = 0;
		public boolean digitalZooming = false;
		public XYFloat zoomCenterXY = new XYFloat(0, 0);

		public MapView.ZoomEndEventListener listener = null;

		public void reset() {
			zoomToLevel = -1;
			digitalZoomEndTime = 0;
			speed = 0;
			digitalZooming = false;
			zoomCenterXY.x = 0;
			zoomCenterXY.y = 0;

			listener = null;
		}

	}

	/**
	 * the render class for opengl drawing
	 * 
	 */
	public class MapRender implements GLSurfaceView.Renderer {

		/**
		 * coordinate for the square texture. it's never changed in the whole
		 * life cycle
		 */
		private ByteBuffer TEXTURE_COORDS;
		// private FloatBuffer bgTexCoords;
		/**
		 * store vertexes of the tile
		 */
		private FloatBuffer mVertexBuffer;

		// private int EMPTY_TILE_REF=0;
		private int MAX_ICON_SIZE = 20;
		private int MAX_CLUSTER_TEXT_SIZE = 30;

		private LinkedHashMap<Icon, Integer> iconPool = new LinkedHashMap<Icon, Integer>(
				MAX_ICON_SIZE * 2, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(
					java.util.Map.Entry<Icon, Integer> eldest) {
				// TODO Auto-generated method stub
				if (size() > MAX_ICON_SIZE) {
					int textureRef = eldest.getValue();
					if (textureRef != 0) {
						IntBuffer textureRefBuf = IntBuffer.allocate(1);
						textureRefBuf.clear();
						textureRefBuf.put(0, textureRef);
						textureRefBuf.position(0);
						glDeleteTextures(1, textureRefBuf);
						LogWrapper.i("TilesView",
								"iconPool removeEldestEntry texture:"
										+ textureRef);
					}
					remove(eldest.getKey());
				}
				return false;
			}
		};

		private LinkedHashMap<String, Integer> clusterTextPool = new LinkedHashMap<String, Integer>(
				MAX_CLUSTER_TEXT_SIZE * 2, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(
					java.util.Map.Entry<String, Integer> eldest) {
				// TODO Auto-generated method stub
				if (size() > MAX_CLUSTER_TEXT_SIZE) {
					int textureRef = eldest.getValue();
					if (textureRef != 0) {
						IntBuffer textureRefBuf = IntBuffer.allocate(1);
						textureRefBuf.clear();
						textureRefBuf.put(0, textureRef);
						textureRefBuf.position(0);
						glDeleteTextures(1, textureRefBuf);
						LogWrapper.i("TilesView",
								"clusterTextPool removeEldestEntry texture:"
										+ textureRef);
					}
					remove(eldest.getKey());
				}
				return false;
			}
		};

		private int maxLabelPriority = 0;
		ArrayList<Label>[] priorityLabels;
		ArrayList<Label>[] shownLabels;
		ArrayList<Label>[] lastShownLabels;

		private boolean isLabelShown(Label label) {
			ArrayList<Label> shownlabelList = shownLabels[label.priority];
			if (shownlabelList == null
					|| shownlabelList.size() == 0
					|| (label.pointNum == 1 && label.type != 0 && label.type != 1)) {
				return false;
			}
			for (int i = 0, shownNum = shownlabelList.size(); i < shownNum; ++i) {
				Label shownLabel = shownlabelList.get(i);
				if (shownLabel.type == label.type
						&& shownLabel.name.equals(label.name)) {
					return true;
				}
			}
			return false;
		}

		private void clearPriorityLabels() {
			for (int i = 0; i < maxLabelPriority; ++i) {
				ArrayList<Label> labelList = this.priorityLabels[i];
				if (labelList.size() > 0) {
					labelList.clear();
				}
			}
		}

		private void addPriorityLabel(Label[] labels) {
			int num = labels.length;
			for (int i = 0; i < num; ++i) {
				if (labels[i].priority >= maxLabelPriority) {
					LogWrapper.i("TilesViewException", "add label: "
							+ labels[i].name + "priority: "
							+ labels[i].priority);
					continue;
				}
				ArrayList<Label> labelList = this.priorityLabels[labels[i].priority];
				labelList.add(labels[i]);
			}
		}

		private static final int MAX_LABEL_DRAW_PER_FRAME = 8;

		private boolean shownLabels(float zoomScale, boolean isGetAll,
				boolean isDrawNew) {
			int i, j, labelNum, priorityNum = maxLabelPriority;
			boolean isFading = false;
			Label.IntegerRef maxLabelToDraw = new Label.IntegerRef(
					MAX_LABEL_DRAW_PER_FRAME);
			labelGrid.clean();
			for (i = 0; i < priorityNum; ++i) {
				ArrayList<Label> lastLabels = lastShownLabels[i];
				labelNum = lastLabels.size();
				ArrayList<Label> shownLabelList = shownLabels[i];
				shownLabelList.clear();
				for (j = 0; j < labelNum; ++j) {
					Label label = lastLabels.get(j);
					label.draw(new XYInteger(displaySize.x / 2,
							displaySize.y / 2), centerXYZ, centerDelta, mapMode
							.getzRotation(), mapMode.getSinZ(), mapMode
							.getCosZ(), zoomScale, labelGrid, TEXTURE_COORDS,
							mVertexBuffer, isDrawNew, maxLabelToDraw);
					if (label.state != Label.LABEL_STATE_CANT_BE_SHOWN) {
						shownLabelList.add(label);
					} else {
						if (label.state == Label.LABEL_STATE_CANT_BE_SHOWN)
							label.state = Label.LABEL_STATE_PRIMITIVE;
					}
					if (!isFading
							&& (label.state == Label.LABEL_STATE_FADE_IN
									|| label.state == Label.LABEL_STATE_FADE_OUT || label.state == Label.LABEL_STATE_WAITING)) {
						isFading = true;
					}
				}
			}
			int maxPriority = isGetAll ? priorityNum : 3;
			for (i = 0; i < maxPriority; ++i) {
				ArrayList<Label> labels = priorityLabels[i];
				labelNum = labels.size();
				ArrayList<Label> shownLabelList = shownLabels[i];
				for (j = 0; j < labelNum; ++j) {
					Label label = labels.get(j);
					if (label.state == Label.LABEL_STATE_CANT_BE_SHOWN) {
						label.state = Label.LABEL_STATE_PRIMITIVE;
						continue;
					}
					if (label.state == Label.LABEL_STATE_PRIMITIVE) {
						if (isLabelShown(label)) {
							continue;
						}

						label.draw(new XYInteger(displaySize.x / 2,
								displaySize.y / 2), centerXYZ, centerDelta,
								mapMode.getzRotation(), mapMode.getSinZ(),
								mapMode.getCosZ(), zoomScale, labelGrid,
								TEXTURE_COORDS, mVertexBuffer, isDrawNew,
								maxLabelToDraw);
						if (label.state != Label.LABEL_STATE_CANT_BE_SHOWN) {
							shownLabelList.add(label);
						} else {
							if (label.state == Label.LABEL_STATE_CANT_BE_SHOWN)
								label.state = Label.LABEL_STATE_PRIMITIVE;
						}
					}
					if (!isFading
							&& (label.state == Label.LABEL_STATE_FADE_IN
									|| label.state == Label.LABEL_STATE_FADE_OUT || label.state == Label.LABEL_STATE_WAITING)) {
						isFading = true;
					}
				}
			}
			ArrayList<Label>[] temp = lastShownLabels;
			lastShownLabels = shownLabels;
			shownLabels = temp;
			return isFading;
		}

		/********************************************* public methods ***********************************************/
		public MapRender(Context context) {
			ByteBuffer tbb = ByteBuffer.allocateDirect(1 * 2 * 4);
			tbb.order(ByteOrder.nativeOrder());
			TEXTURE_COORDS = tbb;
			TEXTURE_COORDS.put((byte) 0);
			TEXTURE_COORDS.put((byte) 0);
			TEXTURE_COORDS.put((byte) 0);
			TEXTURE_COORDS.put((byte) 1);
			TEXTURE_COORDS.put((byte) 1);
			TEXTURE_COORDS.put((byte) 0);
			TEXTURE_COORDS.put((byte) 1);
			TEXTURE_COORDS.put((byte) 1);

			ByteBuffer vbb = ByteBuffer.allocateDirect(4 * 2 * 4);
			vbb.order(ByteOrder.nativeOrder());
			mVertexBuffer = vbb.asFloatBuffer();

			MapEngine engine = MapEngine.getInstance();
			this.maxLabelPriority = engine.getMaxLabelPriority() + 1;
			this.shownLabels = new ArrayList[this.maxLabelPriority];
			this.priorityLabels = new ArrayList[this.maxLabelPriority];
			this.lastShownLabels = new ArrayList[this.maxLabelPriority];
			for (int i = 0; i < maxLabelPriority; ++i) {
				shownLabels[i] = new ArrayList<Label>();
				lastShownLabels[i] = new ArrayList<Label>();
				priorityLabels[i] = new ArrayList<Label>();
			}
			labelGrid = new Grid(displaySize.x, displaySize.y,
					4 + (int) (2 * Globals.g_metrics.scaledDensity));
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			LogWrapper.i("Sequence", "MapRender.onSurfaceCreated");
			gl.glClearColor(1f, 1f, 1f, 1);
			gl.glDisable(GL_DITHER);
			gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST);
			gl.glHint(GL_POINT_SMOOTH_HINT, GL_FASTEST);

			String extensions = gl.glGetString(GLES10.GL_EXTENSIONS);
			if (extensions.indexOf("npot") >= 0
					|| extensions.indexOf("non_power_of_two") >= 0) {// 有npot扩展
				CONFIG.TILE_SIZE = (int) (256 * Globals.g_metrics.density);
				if (CONFIG.TILE_SIZE > 256)
					CONFIG.TILE_SIZE = 384;
				else
					CONFIG.TILE_SIZE = 256;
			}
			Util.init();
			MapEngine.getInstance().setTileSize(CONFIG.TILE_SIZE);

			max_tile_texture_ref = (int) ((Math.ceil(displaySize.x
					/ (float) CONFIG.TILE_SIZE) + 1)
					* (Math.ceil(displaySize.y / (float) CONFIG.TILE_SIZE) + 1) * 4) + 1;
			LogWrapper.i("TextureCount", "max tile texture count: "
					+ max_tile_texture_ref);
			texturePool = new TexturePool(max_tile_texture_ref
					+ CONFIG.TILE_THREAD_COUNT);
			if (tileInfos == null) {
				tileInfos = new LinkedHashMap<Tile, TileInfo>(
						max_tile_texture_ref * 2, 0.75f, true) {
					private static final long serialVersionUID = 1L;
					@Override
					protected boolean removeEldestEntry(
							java.util.Map.Entry<Tile, TileInfo> eldest) {
						// TODO Auto-generated method stub
						if (size() > max_tile_texture_ref) {
							TileInfo info = eldest.getValue();
							if (info != null) {
								if (info.tileTextureRef != 0) {
									if (!texturePool
											.returnTexture(info.tileTextureRef)) {
										IntBuffer textureRefBuf = IntBuffer
												.allocate(1);
										textureRefBuf.clear();
										textureRefBuf.put(0,
												info.tileTextureRef);
										textureRefBuf.position(0);
										glDeleteTextures(1, textureRefBuf);
									}
								}
								if (info.bitmap != null) {
									info.bitmap.recycle();
								}
							}
							return true;
						}
						return false;
					}
				};
			}
			TileThread.startAllThreads();
			for (int i = 0; i < tileRunners.length; i++) {
				if (!tileRunners[i].isAlive())
					tileRunners[i].start();
			}
			gl.glEnable(GL_TEXTURE_2D);
		}

		protected void clearAllTextures() {
			clearTileTextures();
			synchronized (drawingLock) {
				Label.clearTextTexture();
				Label.clearTextBitmap();
				SingleRectLabel.clearIcon();
				scaleView.clearTexture();
			}
		}

		protected void clearTileTextures() {
			synchronized (drawingLock) {
				if (tileInfos != null) {
					Iterator<TileInfo> iterator1 = tileInfos.values()
							.iterator();
					while (iterator1.hasNext()) {
						TileInfo info = iterator1.next();
						if (info.tileTextureRef != 0) {
							if (!texturePool.returnTexture(info.tileTextureRef)) {
								IntBuffer textureRefBuf = IntBuffer.allocate(1);
								textureRefBuf.clear();
								textureRefBuf.put(0, info.tileTextureRef);
								textureRefBuf.position(0);
								glDeleteTextures(1, textureRefBuf);
							}
						} else {
							if (info.bitmap != null) {
								info.bitmap.recycle();
								info.bitmap = null;
							}
						}
					}
					tileInfos.clear();
				}
				if (texturePool != null)
					texturePool.clean();
			}
		}

		/**
		 * the main method to draw all the map elements
		 */
		public void onDrawFrame(GL10 gl) {
			if (paused)
				return;
			if (centerXY == null) {
				return;
			}

			boolean movingL = false;
			boolean movingJustDoneL = false;
			MapView.MoveEndEventListener movingListener = easingRecord.listener;
			boolean zoomingL = false;
			boolean zoomingJustDoneL = false;
			MapView.ZoomEndEventListener zoomingListener = zoomingRecord.listener;
			boolean rotatingX = false;
			boolean rotatingXJustDoneL = false;
			boolean rotatingZ = false;
			boolean rotatingZJustDoneL = false;
			// Tigerknows begin
			isMyLocation = false;
			boolean drawFullTile = false;
			// Tigerknows end
			isStaying = false;
			boolean isWating = false;
			synchronized (drawingLock) {
				// status[0]:moving, status[1]:movingJustDone,
				// status[2]:zooming, status[3]:zoomingJustDone
				// status[4]:rotatingZ, status[5]:rotatingZJustDone,
				// status[6]:rotatingX status[7]:rotatingXJustDone
				boolean[] status = new boolean[8];
				status[0] = movingL;
				status[1] = movingJustDoneL;
				status[2] = zooming;
				status[3] = zoomingJustDoneL;
				status[4] = rotatingZ;
				status[5] = rotatingZJustDoneL;
				status[6] = rotatingX;
				status[7] = rotatingXJustDoneL;

				updateViewBeforeDraw(status);

				movingL = status[0];
				movingJustDoneL = status[1];
				zoomingL = status[2];
				zoomingJustDoneL = status[3];
				rotatingZ = status[4];
				rotatingZJustDoneL = status[5];
				rotatingX = status[6];
				rotatingXJustDoneL = status[7];

				// Log.i("Thread","TilesView onDraw after synchronize to drawingLock");
				long drawStart = System.nanoTime();
				clearPriorityLabels();
				try {
					gl.glViewport(0, 0, displaySize.x, displaySize.y);

					gl.glMatrixMode(GL_MODELVIEW);
					gl.glLoadIdentity();
					gl.glTranslatef(-displaySize.x / 2f, displaySize.y / 2f,
							-mapMode.middleZ);
					gl.glRotatef(180, 1, 0, 0);

					gl.glTranslatef(displaySize.x / 2f, displaySize.y / 2f, 0);
					gl.glScalef(mapMode.scale, mapMode.scale, mapMode.scale);
					gl.glRotatef(mapMode.getxRotation(), 1, 0, 0);
					gl.glRotatef(mapMode.getzRotation(), 0, 0, 1);
					gl.glTranslatef(-displaySize.x / 2f, -displaySize.y / 2f, 0);
					// end of config matrix

					gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					// gl.glClearColor(0, 0, 0, 1);
					float bgr = ((CONFIG.BACKGROUND_COLOR_OPENGL & 0x00ff0000) >> 16) / 255.0f;
					float bgg = ((CONFIG.BACKGROUND_COLOR_OPENGL & 0x0000ff00) >> 8) / 255.0f;
					float bgb = (CONFIG.BACKGROUND_COLOR_OPENGL & 0x000000ff) / 255.0f;
					gl.glClearColor(bgr, bgg, bgb, 1);

					gl.glEnable(GL_TEXTURE_2D);
					gl.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
					gl.glEnableClientState(GL_VERTEX_ARRAY);
					gl.glVertexPointer(2, GL_FLOAT, 0, mVertexBuffer);
					gl.glTexCoordPointer(2, GL_BYTE, 0, TEXTURE_COORDS);
					gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE,
							GL_REPLACE);
					gl.glColor4f(1, 1, 1, 1);
					gl.glDisable(GL_BLEND);
					// gl.glDepthMask(false);

					float scaleToM = mapMode.middleZ
							/ (mapMode.nearZ * mapMode.scale);

					// draw empty tiles
					float displaySizeXR = mapMode.displaySizeConvXR;
					float displaySizeXL = mapMode.displaySizeConvXL;
					float displaySizeYB = mapMode.displaySizeConvYB;
					float displaySizeYT = mapMode.displaySizeConvYT;
					int gridSizeXR = mapMode.gridSizeConvXR;
					int gridSizeXL = mapMode.gridSizeConvXL;
					int gridSizeYB = mapMode.gridSizeConvYB;
					int gridSizeYT = mapMode.gridSizeConvYT;

					float centerScreenX = displaySize.x / 2f + centerDelta.x;
					float centerScreenY = displaySize.y / 2f + centerDelta.y;

					if (centerXY != null) {
						gl.glDisable(GL_TEXTURE_2D);
						// int fillColor=0x00ffff00;
						float red = ((CONFIG.BACKGROUND_GRID_COLOR & 0x00ff0000) >> 16)
								/ (float) 255;
						float green = ((CONFIG.BACKGROUND_GRID_COLOR & 0x0000ff00) >> 8)
								/ (float) 255;
						float blue = ((CONFIG.BACKGROUND_GRID_COLOR & 0x000000ff))
								/ (float) 255;
						gl.glColor4f(red, green, blue, 1);

						float tx = (((int) centerDelta.x % CONFIG.TILE_SIZE) - CONFIG.TILE_SIZE)
								% CONFIG.TILE_SIZE;
						float ty = (((int) centerDelta.y % CONFIG.TILE_SIZE) - CONFIG.TILE_SIZE)
								% CONFIG.TILE_SIZE;
						float displaySizeX = Math.max(displaySizeXR,
								displaySizeXL) * 2;
						float displaySizeY = Math.max(displaySizeYB,
								displaySizeYT) * 2;
						int sizeX = (int) Math.ceil((-tx + displaySizeX)
								/ CONFIG.TILE_SIZE);
						int sizeY = (int) Math.ceil((-ty + displaySizeY)
								/ CONFIG.TILE_SIZE);
						float oriX = (displaySize.x - displaySizeX) / 2;
						float oriY = (displaySize.y - displaySizeY) / 2;
						for (int i = 1; i < sizeY; i++) {
							mVertexBuffer.clear();
							mVertexBuffer.put(oriX);
							mVertexBuffer.put(oriY + CONFIG.TILE_SIZE * i + ty);
							mVertexBuffer.put(oriX + displaySizeX);
							mVertexBuffer.put(oriY + CONFIG.TILE_SIZE * i + ty);
							mVertexBuffer.position(0);
							gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 2);

						}
						for (int i = 1; i < sizeX; i++) {
							mVertexBuffer.clear();
							mVertexBuffer.put(oriX + CONFIG.TILE_SIZE * i + tx);
							mVertexBuffer.put(oriY);
							mVertexBuffer.put(oriX + CONFIG.TILE_SIZE * i + tx);
							mVertexBuffer.put(oriY + displaySizeY);
							mVertexBuffer.position(0);
							gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 2);

						}
						gl.glEnable(GL_TEXTURE_2D);
						gl.glColor4f(1, 1, 1, 1);

					}

					LinkedList<Tile> requestTiles = new LinkedList<Tile>();
					drawingTiles.clear();
					boolean haveDrawingTiles = false;
					boolean fading = false;
					double scaleF = Math.pow(2, zoomLevel - centerXYZ.z);
					double topLeftXf = centerXY.x * scaleF - displaySize.x / 2;
					double topLeftYf = centerXY.y * scaleF + displaySize.y / 2;

					gl.glEnable(GL10.GL_BLEND);
					gl.glBlendFunc(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA);
					for (int s = 0; s < mapLayers.size(); s++) {
						MapLayer mapLayer = mapLayers.get(s);
						if (!mapLayer.visible)
							continue;

						HashSet<XYZ> willDrawXYZs = new HashSet<XYZ>();
						ArrayList<Tile> willDrawTiles = new ArrayList<Tile>();
						ArrayList<XYInteger> willDrawXYs = new ArrayList<XYInteger>();
						double zoomScale = Math.pow(2, zoomLevel - centerXYZ.z);
						int leftDistO, rightDistO, topDistO, bottomDistO;
						int leftDist = leftDistO = (int) (Math
								.floor((-centerDelta.x * zoomScale - displaySizeXL)
										/ (CONFIG.TILE_SIZE * zoomScale) + 0.5));
						// if(leftDist<-(gridSize.x)/2)
						// leftDist=-(gridSize.x)/2;
						int rightDist = rightDistO = (int) (Math
								.ceil((-centerDelta.x * zoomScale + displaySizeXR)
										/ (CONFIG.TILE_SIZE * zoomScale) - 0.5));
						// if(rightDist>(gridSize.x)/2)
						// rightDist=(gridSize.x)/2;
						int topDist = topDistO = (int) (Math
								.floor((-centerDelta.y * zoomScale - displaySizeYT)
										/ (CONFIG.TILE_SIZE * zoomScale) + 0.5));
						// if(topDist<-(gridSize.y)/2) topDist=-(gridSize.y)/2;
						int bottomDist = bottomDistO = (int) (Math
								.ceil((-centerDelta.y * zoomScale + displaySizeYB)
										/ (CONFIG.TILE_SIZE * zoomScale) - 0.5));
						// if(bottomDist>(gridSize.y)/2)
						// bottomDist=(gridSize.y)/2;
						// Log.i("TilesView","onDraw mapLayerLevel,zoomLevel,LRTB:"+centerXYZ.z+","+zoomLevel+","+leftDist+","+rightDist+","+topDist+","+bottomDist);
						if (bottomDist >= topDist && rightDist >= leftDist) {
							// Log.i("TilesView.onDraw","zoomScale,moveX,moveY:"+zoomScale+","+mapLayer.tilesMove.x+","+mapLayer.tilesMove.y);
							for (int i = topDist - 1; i <= bottomDist + 1; i++) { // -4,+4
																					// 暴力：为了避免绘制线路或气泡在屏幕边缘时不能被判断为屏幕之内的问题
								int yi = centerXYZ.y - i;
								for (int j = leftDist - 1; j <= rightDist + 1; j++) { // -4,+4暴力：为了避免绘制线路或气泡在屏幕边缘时不能被判断为屏幕之内的问题
									int xi = centerXYZ.x + j;
									Tile requestTile = mapLayer.createTile();
									requestTile.xyz.x = xi;
									requestTile.xyz.y = yi;
									requestTile.xyz.z = centerXYZ.z;
									requestTile.xyz.x = Util.indexXMod(
											requestTile.xyz.x,
											requestTile.xyz.z);
									if (!haveDrawingTiles)
										drawingTiles.add(requestTile);

									TileInfo tileInfo = tileInfos
											.get(requestTile);

									// tigerknows begin
									// -4,+4暴力：为了避免绘制线路或气泡在屏幕边缘时不能被判断为屏幕之内的问题
									if (i < topDist || i > bottomDist
											|| j < leftDist || j > rightDist) {
										if (tileInfo == null) {
											requestTile.distanceFromCenter = Math
													.abs(i) + Math.abs(j);
											if (i * panDirection.y <= 0
													&& j * panDirection.x <= 0) {
												requestTiles
														.addFirst(requestTile);
											} else {
												requestTiles.add(requestTile);
//												LogWrapper.i("requestTiles", requestTile.xyz.toString());
											}
										}
										continue;
									}
									// tigerknows end
									// -4,+4暴力：为了避免绘制线路或气泡在屏幕边缘时不能被判断为屏幕之内的问题

									// long availTime=0;
									int textureRef = 0;
									if (tileInfo != null) {
										this.addPriorityLabel(tileInfo.labels);
										if (tileInfo.tileTextureRef != 0) {
											textureRef = tileInfo.tileTextureRef;
										} else {
											textureRef = texturePool
													.getAvailabelTexture();
											if (textureRef != 0) {
												try {
													textureRef = tileInfo
															.copyToTexture(textureRef);
												} catch (Exception e) {
													texturePool
															.removeTexture(textureRef);
													textureRef = 0;
												}
											} else {
												textureRef = tileInfo
														.generateTileTextureRef();
												if (textureRef != 0)
													texturePool
															.putTexture(textureRef);
											}
											if (textureRef == 0) {
												tileInfos.remove(requestTile);
											}
										}
									}

									if (textureRef == 0) {
										requestTile.distanceFromCenter = Math
												.abs(i) + Math.abs(j);
										if (i * panDirection.y <= 0
												&& j * panDirection.x <= 0) {
											requestTiles.addFirst(requestTile);
										} else {
											requestTiles.add(requestTile);
//											LogWrapper.i("requestTiles", requestTile.xyz.toString());
										}
										continue;
									} else {
										willDrawXYZs.add(requestTile.xyz);
										willDrawTiles.add(requestTile);
										willDrawXYs.add(new XYInteger(j, i));
									}
								}
							}
							mapLayer.mainLayerDrawPercent = (float) willDrawTiles
									.size()
									/ ((bottomDistO - topDistO + 1) * (rightDistO
											- leftDistO + 1));

						} else {
							mapLayer.mainLayerDrawPercent = 0;
						}
						haveDrawingTiles = true;

						if (mapLayer.centerXY != null
								&& (mapLayer.mainLayerDrawPercent < 1.0f || drawStart
										- fadingStartTime < CONFIG.FADING_TIME * 1E6)) {
							int blX = centerXYZ.x + leftDist;
							int trX = centerXYZ.x + rightDist;
							int blY = centerXYZ.y - bottomDist;
							int trY = centerXYZ.y - topDist;

							double xxZL = 0, yyZL = 0;
							double mapScaleZL = Math.pow(2,
									mapLayer.centerXYZ.z - centerXYZ.z);
							xxZL = mapLayer.centerXY.x - centerXY.x
									* mapScaleZL;
							yyZL = -(mapLayer.centerXY.y - centerXY.y
									* mapScaleZL);

							float mapLayerCenterScreenX = displaySize.x / 2f
									+ mapLayer.centerDelta.x;
							float mapLayerCenterScreenY = displaySize.y / 2f
									+ mapLayer.centerDelta.y;

							double zoomScaleZL = Math.pow(2, zoomLevel
									- mapLayer.centerXYZ.z);
							int leftDistOZL, rightDistOZL, topDistOZL, bottomDistOZL;
							int leftDistZL = leftDistOZL = (int) (Math
									.floor((-(xxZL + mapLayer.centerDelta.x)
											* zoomScaleZL - displaySizeXL)
											/ (CONFIG.TILE_SIZE * zoomScaleZL)
											+ 0.5));
							if (leftDistZL < -gridSizeXL)
								leftDistZL = -gridSizeXL;
							int rightDistZL = rightDistOZL = (int) (Math
									.ceil((-(xxZL + mapLayer.centerDelta.x)
											* zoomScaleZL + displaySizeXR)
											/ (CONFIG.TILE_SIZE * zoomScaleZL)
											- 0.5));
							if (rightDistZL > gridSizeXR)
								rightDistZL = gridSizeXR;
							int topDistZL = topDistOZL = (int) (Math
									.floor((-(yyZL + mapLayer.centerDelta.y)
											* zoomScaleZL - displaySizeYT)
											/ (CONFIG.TILE_SIZE * zoomScaleZL)
											+ 0.5));
							if (topDistZL < -gridSizeYT)
								topDistZL = -gridSizeYT;
							int bottomDistZL = bottomDistOZL = (int) (Math
									.ceil((-(yyZL + mapLayer.centerDelta.y)
											* zoomScaleZL + displaySizeYB)
											/ (CONFIG.TILE_SIZE * zoomScaleZL)
											- 0.5));
							if (bottomDistZL > gridSizeYB)
								bottomDistZL = gridSizeYB;
							// Log.i("TilesView","onDraw zoomLayerLevel,zoomLevel,LRTB:"+mapLayer.centerXYZ.z+","+zoomLevel+","+leftDist+","+rightDist+","+topDist+","+bottomDist);
							if (bottomDistZL >= topDistZL
									&& rightDistZL >= leftDistZL) {
								gl.glPushMatrix();
								gl.glTranslatef(displaySize.x / 2f,
										displaySize.y / 2f, 0);
								gl.glScalef((float) zoomScaleZL,
										(float) zoomScaleZL, 1);
								gl.glTranslatef(-displaySize.x / 2f
										+ (float) xxZL, -displaySize.y / 2f
										+ (float) yyZL, 0);
								int drawNum = 0;
								for (int i = topDistZL; i <= bottomDistZL; i++) {
									int yi = mapLayer.centerXYZ.y - i;
									for (int j = leftDistZL; j <= rightDistZL; j++) {
										int xi = mapLayer.centerXYZ.x + j;
										Tile requestTile = mapLayer
												.createTile();
										requestTile.xyz.x = xi;
										requestTile.xyz.y = yi;
										requestTile.xyz.z = mapLayer.centerXYZ.z;
										requestTile.xyz.x = Util.indexXMod(
												requestTile.xyz.x,
												requestTile.xyz.z);
										int textureRef = 0;
										TileInfo tileInfo = tileInfos
												.get(requestTile);
										if (tileInfo != null) {
											if (tileInfo.tileTextureRef != 0) {
												textureRef = tileInfo.tileTextureRef;
											} else {
												textureRef = texturePool
														.getAvailabelTexture();
												if (textureRef != 0) {
													try {
														textureRef = tileInfo
																.copyToTexture(
																		gl,
																		textureRef);
													} catch (Exception e) {
														texturePool
																.removeTexture(textureRef);
														textureRef = 0;
													}
												} else {
													textureRef = tileInfo
															.generateTileTextureRef(gl);
													if (textureRef != 0)
														texturePool
																.putTexture(textureRef);
												}
												if (textureRef == 0) {
													tileInfos
															.remove(requestTile);
												}
											}
										}
										if (textureRef == 0) {
											requestTile.distanceFromCenter = Math
													.abs(i) + Math.abs(j);
											if (i * panDirection.y <= 0
													&& j * panDirection.x <= 0) {
												requestTiles
														.addFirst(requestTile);
											} else {
												requestTiles.add(requestTile);
//												LogWrapper.i("requestTiles", requestTile.xyz.toString());
											}
											continue;
										}

										if (drawStart - fadingStartTime < CONFIG.FADING_TIME * 1E6
												|| !Util.coveredByTiles(
														new XYZ(
																xi,
																yi,
																mapLayer.centerXYZ.z),
														blX, blY, trX, trY,
														centerXYZ.z,
														willDrawXYZs)) {
											gl.glBindTexture(GL_TEXTURE_2D,
													textureRef);

											float sx = mapLayerCenterScreenX
													+ (j) * CONFIG.TILE_SIZE;
											float sy = mapLayerCenterScreenY
													+ (i) * CONFIG.TILE_SIZE;
											mVertexBuffer.clear();
											mVertexBuffer.put(sx
													- CONFIG.TILE_SIZE / 2f
													+ CONFIG.BORDER / 2f);
											mVertexBuffer.put(sy
													- CONFIG.TILE_SIZE / 2f
													+ CONFIG.BORDER / 2f);
											mVertexBuffer.put(sx
													- CONFIG.TILE_SIZE / 2f
													+ CONFIG.BORDER / 2f);
											mVertexBuffer.put(sy
													+ CONFIG.TILE_SIZE / 2f
													- CONFIG.BORDER / 2f);
											mVertexBuffer.put(sx
													+ CONFIG.TILE_SIZE / 2f
													- CONFIG.BORDER / 2f);
											mVertexBuffer.put(sy
													- CONFIG.TILE_SIZE / 2f
													+ CONFIG.BORDER / 2f);
											mVertexBuffer.put(sx
													+ CONFIG.TILE_SIZE / 2f
													- CONFIG.BORDER / 2f);
											mVertexBuffer.put(sy
													+ CONFIG.TILE_SIZE / 2f
													- CONFIG.BORDER / 2f);
											mVertexBuffer.position(0);
											TEXTURE_COORDS.position(0);
											gl.glDrawArrays(GL_TRIANGLE_STRIP,
													0, 4);
										}
										drawNum++;
									}
								}
								mapLayer.zoomLayerDrawPercent = (float) drawNum
										/ ((bottomDistOZL - topDistOZL + 1) * (rightDistOZL
												- leftDistOZL + 1));
								gl.glPopMatrix();
							} else {
								mapLayer.zoomLayerDrawPercent = 0;
							}

							// Log.i("TilesView.onDraw","zoom layer take:"+(System.nanoTime()-drawStart)/1000000);
						} else
							mapLayer.zoomLayerDrawPercent = 0;

						// draw the normal tiles
						gl.glPushMatrix();
						gl.glTranslatef(displaySize.x / 2f, displaySize.y / 2f,
								0);
						gl.glScalef((float) zoomScale, (float) zoomScale, 1);
						gl.glTranslatef(-displaySize.x / 2f,
								-displaySize.y / 2f, 0);

						for (int ii = 0; ii < willDrawTiles.size(); ii++) {
							Tile requestTile = willDrawTiles.get(ii);
							int textureRef = tileInfos.get(requestTile).tileTextureRef;
							if (textureRef == 0) {
								LogWrapper.e("TilesView",
										"onDrawFrame tile texture 0: "
												+ requestTile.toString());
								continue;
							}
							gl.glBindTexture(GL_TEXTURE_2D, textureRef);

							float sx = centerScreenX + (willDrawXYs.get(ii).x)
									* CONFIG.TILE_SIZE;
							float sy = centerScreenY + (willDrawXYs.get(ii).y)
									* CONFIG.TILE_SIZE;
							mVertexBuffer.clear();
							mVertexBuffer.put(sx - CONFIG.TILE_SIZE / 2f
									+ CONFIG.BORDER / 2f);
							mVertexBuffer.put(sy - CONFIG.TILE_SIZE / 2f
									+ CONFIG.BORDER / 2f);
							mVertexBuffer.put(sx - CONFIG.TILE_SIZE / 2f
									+ CONFIG.BORDER / 2f);
							mVertexBuffer.put(sy + CONFIG.TILE_SIZE / 2f
									- CONFIG.BORDER / 2f);
							mVertexBuffer.put(sx + CONFIG.TILE_SIZE / 2f
									- CONFIG.BORDER / 2f);
							mVertexBuffer.put(sy - CONFIG.TILE_SIZE / 2f
									+ CONFIG.BORDER / 2f);
							mVertexBuffer.put(sx + CONFIG.TILE_SIZE / 2f
									- CONFIG.BORDER / 2f);
							mVertexBuffer.put(sy + CONFIG.TILE_SIZE / 2f
									- CONFIG.BORDER / 2f);
							mVertexBuffer.position(0);
							TEXTURE_COORDS.position(0);

							if (CONFIG.FADING_TIME > 0) {
								long fadeTime = fadingStartTime;// Math.max(fadingStartTime,
																// availTime);
								long curTime = drawStart;
								float fadingAnim = (float) ((curTime - fadeTime) / (CONFIG.FADING_TIME * 1E6));
								if (fadingAnim > 1)
									fadingAnim = 1;
								if (fadingAnim < 1)
									fading = true;
								gl.glColor4f(1, 1, 1,
										(FADING_START_ALPHA + fadingAnim
												* (255 - FADING_START_ALPHA))
												/ (float) 255);
								if (fadingAnim < 1) {
									// Log.i("TilesView","onDraw tileP.alpha,fadingStartTime,availTime:"+tileP.getAlpha()+","+(curTime-fadingStartTime)/1E6+","+(curTime-tileResponse.availTime)/1E6);
								}
							}
							gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
						}
						long start = System.currentTimeMillis();
						isWating = (start - lastMoveTime) < 10;
						isStaying = touching && !isWating;
						isLastLabelFading = isLabelFading;
						boolean refreshText = (!zoomingL
								&& ((easingRecord.startMoveTime == 0
										&& zoomingRecord.digitalZoomEndTime == 0 && !touching))
								|| zoomingJustDoneL || isManuelZoom
								|| rotatingZJustDoneL || rotatingXJustDoneL || (
								!isTouchBegin && !isBeginMoving && !movingL));// isStaying
																					// &&

						isLabelFading = this.shownLabels((float) zoomScale,
								refreshText, refreshText);
						if (refreshText || zoomingJustDoneL) {
							isManuelZoom = false;
						}
//						long end = System.currentTimeMillis();
//						if (refreshText) {
//							isStaying = false;
//							LogWrapper.d("Label", "------draw all label cost: "
//									+ (end - start));
//						} else {
//							LogWrapper.d("Label", "+++draw label cost: "
//									+ (end - start));
//						}
						gl.glPopMatrix();
						gl.glColor4f(1, 1, 1, 1);

						int drawNum = willDrawTiles.size();
						drawFullTile = (drawNum == ((bottomDist - topDist + 1) * (rightDist
								- leftDist + 1)));
						int mapTextZoomLevel = mapText.getZoomLevel();
						XYDouble mapTextMercXY = mapText.getMercXY();
						boolean same = true;
						long time = System.nanoTime();
						if (Math.abs(mapTextMercXY.x - centerXY.x) > 8
								|| Math.abs(mapTextMercXY.y - centerXY.y) > 8
								|| mapTextZoomLevel != centerXYZ.z
								|| mapText.refresh) {
							same = false;
						}
						if (!same && refreshText) {
							mapText.mercXYGetting.x = centerXY.x;
							mapText.mercXYGetting.y = centerXY.y;
							mapText.zoomLevelGetting = centerXYZ.z;
							mapText.refresh = false;
							mapText.screenTextGetting = true;
							mapText.lastTime = time;
						}
					}
					gl.glDisable(GL_BLEND);

					if (isBeginMoving) {
						if (movingFrameCount < 0 || movingFrameCount > 4) {
							movingFrameCount = 0;
						}
						if (movingFrameCount == 4) {
							isBeginMoving = false;
							movingFrameCount = 0;
						} else {
							++movingFrameCount;
						}
					}
					
					//draw scale view
					gl.glPushMatrix();
					gl.glVertexPointer(2, GL_FLOAT, 0, mVertexBuffer);
					float density = Globals.g_metrics.density;
			        Position centerPos = getCenterPosition();
			        XYFloat leftTop = new XYFloat(10 * density, displaySize.y - 50 * density);
					scaleView.renderGL(leftTop, zoomLevel, (float)centerPos.getLat(), centerXYZ.z, TEXTURE_COORDS);
					gl.glPopMatrix();
					
					// draw the shapes
					gl.glEnable(GL10.GL_BLEND);
					gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
					gl.glDisable(GL_TEXTURE_2D);
					for (int i = 0; i < shapes.size(); i++) {
						Shape shape = shapes.get(i);
						if (!shape.isVisible()) {
							continue;
						}
						if (shape instanceof Polyline) {
							Polyline polyline = (Polyline) shape;
							if (polyline.getPositions() == null)
								continue;
							polyline.renderGL(gl, new XYDouble(topLeftXf,
									topLeftYf), zoomLevel, centerXYZ.z,
									drawingTiles);
						} else if (shape instanceof Circle) {
							Circle circle = (Circle) shape;
							if (circle.getPosition() == null) {
								continue;
							}
							circle.renderGL(gl, new XYDouble(topLeftXf,
									topLeftYf), zoomLevel);
						} else if (shape instanceof Polygon) {
							Polygon polygon = (Polygon) shape;
							if (polygon.getPositions() == null)
								continue;
							polygon.renderGL(gl, new XYDouble(topLeftXf,
									topLeftYf), zoomLevel);
						}
					}
					gl.glColor4f(1, 1, 1, 1);
					gl.glDisable(GL_BLEND);
					gl.glEnable(GL_TEXTURE_2D);
					gl.glVertexPointer(2, GL_FLOAT, 0, mVertexBuffer);

					// draw the overlays
					double overlayZoomScale = Math.pow(2, zoomLevel
							- ItemizedOverlay.ZOOM_LEVEL);
					gl.glEnable(GL10.GL_BLEND);
					gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
					for (int i = 0; i < overlays.size(); i++) {
						ItemizedOverlay overlay = overlays.get(i);

						ArrayList<ArrayList<OverlayItem>> clusters = overlay
								.getVisiblePins(centerXYZ.z, drawingTiles);
						OverlayItem focusedPin = null;
						// Log.i("TilesView","onDraw pins length,visible length,zoomLevel:"+overlay.size()+","+pins.size()+","+centerXYZ.z);
						for (int j = clusters.size() - 1; j >= 0; j--) {
							ArrayList<OverlayItem> cluster = clusters.get(j);
							ArrayList<OverlayItem> visiblePins = new ArrayList<OverlayItem>();
							for (int ii = 0; ii < cluster.size(); ii++) {
								OverlayItem pinL = cluster.get(ii);
								if (pinL.getIcon().getImage() != null
										&& pinL.getMercXY() != null
										&& pinL.isVisible()) {
									visiblePins.add(pinL);
								}
							}
							if (visiblePins.size() == 0)
								continue;

							OverlayItem pin = visiblePins.get(0);

							float x = (float) (pin.getMercXY().x
									* overlayZoomScale - topLeftXf);
							float y = (float) (-pin.getMercXY().y
									* overlayZoomScale + topLeftYf);

							RotationTilt rt = pin.getRotationTilt();
							float zRot = rt.getRotation();
							if (rt.getRotateRelativeTo().equals(
									RotateReference.MAP)) {
								zRot += mapMode.getzRotation();
							}
							if (pin instanceof MyLocation) {
								isMyLocation = true;
								zRot -= tkZRotation;
								MyLocation myLocation = (MyLocation) pin;
								long time = System.nanoTime();
								if (isMyLocation
										&& (time - myLocation.refreshTime) > MyLocation.REFRESH_TIME) {
									myLocation.refreshTime = time;
									pin.isFoucsed = !pin.isFoucsed;
								}
							} else if (pin.isFoucsed) {
								focusedPin = pin;
							}
							float xRot = rt.getTilt();
							if (rt.getTiltRelativeTo().equals(
									TiltReference.SCREEN)) {
								xRot -= mapMode.getxRotation();
							}
							gl.glPushMatrix();
							gl.glTranslatef(x, y, 0);
							gl.glRotatef(-mapMode.getzRotation(), 0, 0, 1);
							gl.glRotatef(xRot, 1, 0, 0);
							gl.glRotatef(zRot, 0, 0, 1);
							gl.glScalef(scaleToM, scaleToM, 1);
							drawOverlayItemOpenGL(gl, pin, 0, 0);
							if (overlay.isClustering() && cluster.size() > 1) {
								drawClusterTextOpenGL(gl, pin,
										"" + cluster.size(), 0, 0);
							}
							gl.glPopMatrix();

						}
						// 将焦点的Pin绘制在上面
						if (focusedPin != null) {
							float x = (float) (focusedPin.getMercXY().x
									* overlayZoomScale - topLeftXf);
							float y = (float) (-focusedPin.getMercXY().y
									* overlayZoomScale + topLeftYf);

							RotationTilt rt = focusedPin.getRotationTilt();
							float zRot = rt.getRotation();
							if (rt.getRotateRelativeTo().equals(
									RotateReference.MAP)) {
								zRot += mapMode.getzRotation();
							}

							float xRot = rt.getTilt();
							if (rt.getTiltRelativeTo().equals(
									TiltReference.SCREEN)) {
								xRot -= mapMode.getxRotation();
							}
							gl.glPushMatrix();
							gl.glTranslatef(x, y, 0);
							gl.glRotatef(-mapMode.getzRotation(), 0, 0, 1);
							gl.glRotatef(xRot, 1, 0, 0);
							gl.glRotatef(zRot, 0, 0, 1);
							gl.glScalef(scaleToM, scaleToM, 1);
							drawOverlayItemOpenGL(gl, focusedPin, 0, 0);
							gl.glPopMatrix();
						}

					}
					gl.glDisable(GL10.GL_BLEND);
					gl.glBlendFunc(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA);

					// draw the info window
					if (infoWindow.isVisible()
							&& infoWindow.getMercXY() != null) {
						double infoZoomScale = (float) Math.pow(2, zoomLevel
								- InfoWindow.ZOOM_LEVEL);
						float x = (float) (infoWindow.getMercXY().x
								* infoZoomScale - topLeftXf);
						float y = (float) (-infoWindow.getMercXY().y
								* infoZoomScale + topLeftYf);

						RotationTilt rt = infoWindow.getOffsetRotationTilt();
						float zRot = rt.getRotation();
						if (rt.getRotateRelativeTo()
								.equals(RotateReference.MAP)) {
							zRot += mapMode.getzRotation();
						}
						float xRot = rt.getTilt();
						if (rt.getTiltRelativeTo().equals(TiltReference.SCREEN)) {
							xRot -= mapMode.getxRotation();
						}
						gl.glPushMatrix();
						gl.glTranslatef(x, y, 0);
						gl.glRotatef(-mapMode.getzRotation(), 0, 0, 1);
						gl.glRotatef(xRot, 1, 0, 0);
						gl.glRotatef(zRot, 0, 0, 1);
						gl.glTranslatef(-infoWindow.getOffset().x * scaleToM,
								-infoWindow.getOffset().y * scaleToM, 0);

						gl.glRotatef(-zRot, 0, 0, 1);
						gl.glRotatef(-mapMode.getxRotation() - xRot, 1, 0, 0);
						gl.glScalef(scaleToM, scaleToM, 1);
						infoWindow.drawInfoWindowOpenGL(gl, new XYFloat(0, 0));
						gl.glPopMatrix();
					}

					// draw the compass
					if (compass != null && compass.isVisible()) {
						XYInteger screenXY = compass.getScreenXY(displaySize);
						// gl.glDisable(GL_TEXTURE_2D);

						int paddingTop = 0;
						if (compass.getPlaceLocation().equals(
								PlaceLocation.CENTER) == false) {
							paddingTop = padding.top;
						}
						gl.glLoadIdentity();
						gl.glTranslatef(-displaySize.x / 2f, displaySize.y / 2f
								+ paddingTop, -mapMode.middleZ);
						gl.glRotatef(180, 1, 0, 0);

						gl.glTranslatef(displaySize.x / 2f, displaySize.y / 2f
								+ paddingTop, 0);
						float scale = mapMode.middleZ / mapMode.nearZ;
						gl.glScalef(scale, scale, scale);
						gl.glTranslatef(screenXY.x - displaySize.x / 2f,
								screenXY.y - displaySize.y / 2f + paddingTop, 0);
						gl.glRotatef(mapMode.getxRotation(), 1, 0, 0);
						gl.glRotatef(tkZRotation, 0, 0, 1); // mapMode.getzRotation()
						compass.drawCompassOpenGL(gl);
						// compass.renderGL(gl);
					}
				    
					// end of draw

					// if(visibleLayerNum>1 && requestTiles.size()>0){
					if (requestTiles.size() > 0) {
						Collections.sort(requestTiles);
						synchronized (tilesWaitForLoading) {
							tilesWaitForLoading.clear();
							tilesWaitForLoading.addAll(0, requestTiles);
							tilesWaitForLoading.notifyAll();
						}
					}
					// if (mapText.screenTextGetting &&
					// Math.abs(System.currentTimeMillis()-MapTextThread.time) >
					// 512 &&
					// refreshText) {
					// synchronized (mapText) {
					// mapText.notifyAll();
					// }
					// }

					if (zoomingL || isLastLabelFading || isWating
							|| isLabelFading || fading || movingL || rotatingX
							|| rotatingZ) {
						requestRender();
					} else if (isCancelSnap == false && snapCenterPos != null
							&& mapText.texImageChanged == false
							&& mapText.screenTextGetting == false
							&& drawFullTile) {
						XYFloat xy = mercXYToScreenXYConv(Util.posToMercPix(
								snapCenterPos, getZoomLevel()), getZoomLevel());
						// 确保快照地图时，地图已经移动到指定的中心位置，误差为32像素?
						if (Math.abs(xy.x - displaySize.x / 2) < 32
								&& Math.abs(xy.y - displaySize.y / 2) < 32) {
							snapBmp = savePixels(0, 0, displaySize.x,
									displaySize.y, gl);
							synchronized (snapCenterPos) {
								snapCenterPos.notifyAll();
							}
							snapCenterPos = null;
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				Profile.drawInc(System.nanoTime() - drawStart);

				if (movingJustDoneL) {
					try {
						Position cp = getCenterPosition();
						if (movingListener != null) {
							movingListener.onMoveEndEvent(mParentMapView, cp);
						}
						mParentMapView.executeMoveEndListeners(cp);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (zoomingJustDoneL) {
					// MapTextThread.time = 0;
					if (zoomingListener != null) {
						zoomingListener.onZoomEndEvent(mParentMapView,
								Math.round(zoomLevel));
					}
					mParentMapView.executeZoomEndListeners(Math
							.round(zoomLevel));
				}

				if (rotatingZJustDoneL
						&& lastZRotation != mapMode.getzRotation()) {
					mParentMapView.executeRotateEndListeners(mapMode
							.getzRotation());
				}

				if (rotatingXJustDoneL
						&& lastXRotation != mapMode.getxRotation()) {
					mParentMapView.executeTiltEndListeners(mapMode
							.getxRotation());
				}
			}

		}

		/**
		 * set the view port and do the top level coordinate matrix operation
		 * here, it will convert the coordinate from the normal canvas
		 * coordinate to coordinate system relative to screen center.
		 */
		public void onSurfaceChanged(GL10 gl, int w, int h) {
			LogWrapper.i("Sequence", "MapRender.onSurfaceChanged " + "w:" + w
					+ ",h:" + h + ",displaySize:" + displaySize);

			gl.glMatrixMode(GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glFrustumf(-displaySize.x / 2f, displaySize.x / 2f,
					-displaySize.y / 2f, displaySize.y / 2f, mapMode.nearZ,
					mapMode.farZ + 1);

			clearTexRefs(gl);
		}

		/**************************************************** private methods *************************************************/
		private void clearTexRefs(GL10 gl) {
			LogWrapper.i(
					"TilesView",
					"clean tilePool total texture and image num:"
							+ tileInfos.size());
			Iterator<TileInfo> iterator1 = tileInfos.values().iterator();
			while (iterator1.hasNext()) {
				TileInfo info = iterator1.next();
				if (info.tileTextureRef != 0) {
					if (!texturePool.returnTexture(info.tileTextureRef)) {
						IntBuffer textureRefBuf = IntBuffer.allocate(1);
						textureRefBuf.clear();
						textureRefBuf.put(0, info.tileTextureRef);
						textureRefBuf.position(0);
						glDeleteTextures(1, textureRefBuf);
					}
				}
			}
			tileInfos.clear();
			texturePool.clean();

			LogWrapper.i("TilesView", "clean iconPool total texture num:"
					+ iconPool.size());
			Iterator<Integer> iterator2 = iconPool.values().iterator();
			while (iterator2.hasNext()) {
				int textureRef = iterator2.next();
				deleteTextureRef(gl, textureRef);
			}
			iconPool.clear();

			LogWrapper.i("TilesView",
					"clean clusterTextPool total texture num:"
							+ clusterTextPool.size());
			Iterator<Integer> iterator3 = clusterTextPool.values().iterator();
			while (iterator3.hasNext()) {
				int textureRef = iterator3.next();
				deleteTextureRef(gl, textureRef);
			}
			clusterTextPool.clear();

			infoWindow.clearTextureRef(gl);

			SingleRectLabel.clearIcon();
			Label.clearTextBitmap();
			Label.clearTextTexture();

			compass.clearTextureRef(gl);

			mapText.screenTextGetting = true;
			mapText.lastTime = 0;
			mapText.refresh = false;
		}

		private int genTextureRef(GL10 gl) {
			IntBuffer textureRefBuf = IntBuffer.allocate(1);
			textureRefBuf.clear();
			textureRefBuf.position(0);
			gl.glGenTextures(1, textureRefBuf);
			return textureRefBuf.get(0);

		}

		private void deleteTextureRef(GL10 gl, int textureRef) {
			if (textureRef == 0)
				return;
			IntBuffer textureRefBuf = IntBuffer.allocate(1);
			textureRefBuf.clear();
			textureRefBuf.put(0, textureRef);
			textureRefBuf.position(0);
			gl.glDeleteTextures(1, textureRefBuf);
		}

		private void drawClusterTextOpenGL(GL10 gl, OverlayItem pin,
				String clusterText, float x, float y) {
			if (clusterText == null || clusterText.equals("")
					|| pin.getOwnerOverlay() == null)
				return;

			ItemizedOverlay overlay = pin.getOwnerOverlay();
			int textureRef = 0;
			XYInteger offset = pin.getIcon().getOffset();
			XYInteger clusterOff = overlay.getClusterTextOffset();

			float borderSize = ItemizedOverlay.OVERLAY_CLUSTER_BORDER_SIZE
					* Globals.g_metrics.density;
			float roundRadius = ItemizedOverlay.OVERLAY_CLUSTER_ROUND_RADIUS
					* Globals.g_metrics.density;
			float textSize = ItemizedOverlay.OVERLAY_CLUSTER_TEXT_SIZE
					* Globals.g_metrics.density;
			Paint textPaint = new Paint();
			textPaint.setColor(overlay.getClusterTextColor());
			textPaint.setTextAlign(Align.CENTER);
			textPaint.setTextSize(textSize);
			textPaint
					.setAntiAlias(ItemizedOverlay.OVERLAY_CLUSTER_TEXT_ANTIALIAS);
			float textOffsetX = ItemizedOverlay.OVERLAY_CLUSTER_TEXT_OFFSET_X
					* Globals.g_metrics.density;
			float textOffsetY = ItemizedOverlay.OVERLAY_CLUSTER_TEXT_OFFSET_Y
					* Globals.g_metrics.density;
			float textHeight = -textPaint.ascent() + textPaint.descent();
			float textLength = textPaint.measureText(clusterText);
			float totalTextHeight = textHeight + 2 * textOffsetY;
			float totalTextWidth = textLength + 2 * textOffsetX;

			int bmSizeX = Util.getPower2(totalTextWidth);
			int bmSizeY = Util.getPower2(totalTextHeight);

			String key = clusterText + "|"
					+ overlay.getClusterBackgroundColor() + "|"
					+ overlay.getClusterBorderColor() + "|"
					+ overlay.getClusterTextColor();
			if (clusterTextPool.containsKey(key)) {
				textureRef = clusterTextPool.get(key);
			} else {
				textureRef = genTextureRef(gl);
				gl.glBindTexture(GL_TEXTURE_2D, textureRef);
				try {

					Paint innerPaint = new Paint();
					innerPaint.setColor(overlay.getClusterBackgroundColor());
					innerPaint
							.setAntiAlias(ItemizedOverlay.OVERLAY_CLUSTER_INNER_ANTIALIAS);
					Paint borderPaint = new Paint();
					borderPaint.setColor(overlay.getClusterBorderColor());
					borderPaint.setStyle(Style.STROKE);
					borderPaint.setStrokeWidth(borderSize);
					borderPaint
							.setAntiAlias(ItemizedOverlay.OVERLAY_CLUSTER_BORDER_ANTIALIAS);

					Bitmap.Config config = Config.ARGB_8888;
					Bitmap bm = Bitmap.createBitmap(bmSizeX, bmSizeY, config);
					Canvas canvas = new Canvas(bm);
					bm.eraseColor(0);

					RectF rect = new RectF(0, 0, totalTextWidth,
							totalTextHeight);
					canvas.drawRoundRect(rect, roundRadius, roundRadius,
							innerPaint);
					canvas.drawRoundRect(rect, roundRadius, roundRadius,
							borderPaint);
					canvas.drawText(clusterText, (rect.left + rect.right) / 2,
							rect.top + textOffsetY + 0.8f * textHeight,
							textPaint);

					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
							GL_NEAREST);
					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
							GL_NEAREST);
					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
							GL_CLAMP_TO_EDGE);
					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
							GL_CLAMP_TO_EDGE);
					GLUtils.texImage2D(GL_TEXTURE_2D, 0, bm, 0);
					clusterTextPool.put(key, textureRef);
					LogWrapper.d("TilesView",
							"clusterTextPool put textureRef,key:" + textureRef
									+ "," + key);

				} catch (Exception e) {
					deleteTextureRef(gl, textureRef);
					textureRef = 0;
				}
			}

			gl.glBindTexture(GL_TEXTURE_2D, textureRef);

			x -= offset.x;
			y -= offset.y;
			x += clusterOff.x;
			y += clusterOff.y;
			ItemizedOverlay.OffsetReference relativeTo = overlay
					.getClusterTextOffsetRelativeTo();
			if (relativeTo.equals(ItemizedOverlay.OffsetReference.BOTTOM_RIGHT)) {
				x -= totalTextWidth;
				y -= totalTextHeight;
			} else if (relativeTo
					.equals(ItemizedOverlay.OffsetReference.BOTTOM_LEFT)) {
				y -= totalTextHeight;
			} else if (relativeTo
					.equals(ItemizedOverlay.OffsetReference.TOP_RIGHT)) {
				x -= totalTextWidth;
			}

			mVertexBuffer.clear();
			mVertexBuffer.put(x);
			mVertexBuffer.put(y);
			mVertexBuffer.put(x);
			mVertexBuffer.put(y + bmSizeY);
			mVertexBuffer.put(x + bmSizeX);
			mVertexBuffer.put(y);
			mVertexBuffer.put(x + bmSizeX);
			mVertexBuffer.put(y + bmSizeY);
			mVertexBuffer.position(0);
			TEXTURE_COORDS.position(0);
			gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}

		private void drawOverlayItemOpenGL(GL10 gl, OverlayItem pin, float x,
				float y) {
			int textureRef = 0;
			Icon icon = pin.getIcon();
			if (pin instanceof MyLocation) {
				MyLocation myLocation = (MyLocation) pin;
				if (myLocation.mode == MyLocation.MODE_ROTATION) {
					if (pin.isFoucsed) {
						icon = myLocation.faceToFocused;
					} else {
						icon = myLocation.faceToNormal;
					}
				} else {
					if (pin.isFoucsed) {
						icon = myLocation.focused;
					}
				}
			}

			XYInteger size = icon.getSize();
			XYInteger offset = icon.getOffset();
			int bmSizeX = Util.getPower2(size.x);
			int bmSizeY = Util.getPower2(size.y);

			if (iconPool.containsKey(icon)) {
				textureRef = iconPool.get(icon);
			} else {
				textureRef = genTextureRef(gl);
				gl.glBindTexture(GL_TEXTURE_2D, textureRef);
				try {
					Bitmap.Config config = Config.ARGB_8888;
					Bitmap bm = Bitmap.createBitmap(bmSizeX, bmSizeY, config);
					Canvas canvas = new Canvas(bm);
					bm.eraseColor(0);

					// canvas.drawRect(new RectF(0, 0,size.x,size.y), new
					// Paint());
					canvas.drawBitmap(icon.getImage(), null, new RectF(0, 0,
							size.x, size.y), null);
					// int order = icon.getOrder();
					// if (order > 0) {
					// String orderStr = String.valueOf(order);
					// int width = Math.round(orderP.measureText(orderStr));
					// int height=Math.round(-orderP.ascent()+orderP.descent());
					// canvas.drawText(orderStr, size.x/2-width/2,
					// size.y/8+height, orderP);
					// }

					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
							GL_NEAREST);
					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
							GL_NEAREST);
					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,
							GL_CLAMP_TO_EDGE);
					gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,
							GL_CLAMP_TO_EDGE);
					GLUtils.texImage2D(GL_TEXTURE_2D, 0, bm, 0);
					iconPool.put(icon.clone(), textureRef);
					// Log.d("TilesView","iconPool put textureRef,pin:"+textureRef+","+pin.getMessage());
				} catch (Exception e) {
					deleteTextureRef(gl, textureRef);
					textureRef = 0;
				}
			}
			gl.glBindTexture(GL_TEXTURE_2D, textureRef);

			x -= offset.x;
			y -= offset.y;
			mVertexBuffer.clear();
			mVertexBuffer.put(x);
			mVertexBuffer.put(y);
			mVertexBuffer.put(x);
			mVertexBuffer.put(y + bmSizeY);
			mVertexBuffer.put(x + bmSizeX);
			mVertexBuffer.put(y);
			mVertexBuffer.put(x + bmSizeX);
			mVertexBuffer.put(y + bmSizeY);
			mVertexBuffer.position(0);
			TEXTURE_COORDS.position(0);
			gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

		}
	}

}

/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.tigerknows.common;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.decarta.CONFIG;
import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;

/**
 * 此类实现缓存图片到扩展存储卡的功能
 * 如果扩展存储卡不可用，则不缓存
 */
public class ImageCache {
	
	private class ImageData{
		String name=null;
		byte[] bmBytes=null;
		ImageData(String name, byte[] bmBytes){
			this.name=name;
			this.bmBytes=bmBytes;
		}
	}
	
	private String appPath = "";
	private boolean externalStorage=true;
	//private static final float CACHE_FILL_RATIO=0.6f;
	
	private boolean stopWriting=false;
	private boolean stopRemoveCache=true;
	private LinkedList<ImageData> writingList=new LinkedList<ImageData>();
	private final int MAX_WRITING_LIST_SIZE=300;
	
	private LinkedHashMap<String,Boolean> cacheTileFileNames=new LinkedHashMap<String,Boolean>(TKConfig.IMAGE_CACHE_SIZE_SDCARD,0.75f,true);
	
	public ImageCache(){
	}
	
	public void init (Context context) throws APIException{
		LogWrapper.d("TileTable", "init");
		externalStorage = true;
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			externalStorage=false;
			LogWrapper.i("TileTable","external storage is not mounted");
		}
		if (externalStorage) {
			try {
				appPath = Environment.getExternalStorageDirectory()+ "/Android/data/" + context.getPackageName() + "/files";
				File appFile = new File(appPath);
				if (!appFile.exists() || !appFile.isDirectory()) {
					appFile.mkdirs();
				}
				new File(appPath, "try.txt").createNewFile();
				new File(appPath, "try.txt").delete();
				
				stopRemoveCache=true;
				startWritingThread();
			} catch (Exception e) {
				externalStorage=false;
				throw new APIException("Can't write/read cache. Please Grand permission");
			}

		}

		LogWrapper.i("TileTable", "app path:"+ appPath + ",exist:" + new File(appPath).exists());
	}
	
	/**
	 * 启动一个线程专门负责图片数据文件写入到扩展存储卡
	 */
	void startWritingThread(){
		stopWriting=false;
		synchronized(writingList){
			writingList.clear();
		}
		
		if (externalStorage && appPath!=null && !appPath.equals("")){
			File cacheDir = new File(appPath);
			if(cacheDir.exists() && cacheDir.isDirectory()) {
				File[] children = cacheDir.listFiles();
				LogWrapper.i("TileTable","writingThread cache length:"+children.length);
				/*Arrays.sort(children, new Comparator<File>() {
		            public int compare(File f1, File f2) {
		                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
		            }
		        });*/
				synchronized(cacheTileFileNames){
					cacheTileFileNames.clear();
					for(int i=0;i<children.length;i++){
						cacheTileFileNames.put(children[i].getName(),true);
						//LogWrapper.i("TileTable", "startWritingThread sort stage put:"+children[i].getName());
					}	
				}
			}
		}
		LogWrapper.i("TileTable", "startWritingThread done with files sorting");
		
		LogWrapper.d("TileTable","startWritingThread");
		Thread t=new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				LogWrapper.i("TileTable","writing thread started");
				while (true) {
					if(stopWriting){
						LogWrapper.i("TileTable","writing thread break at beginning");
						break;
					}
					
					ImageData tileFileEntry=null;
					synchronized(writingList){
						if(writingList.isEmpty()){
							if(stopWriting){
								LogWrapper.i("TileTable","writing thread break before writingList wait");
								break;
							}
							try{
								//LogWrapper.d("TileTable", "before writingList.wait");
								writingList.wait();
								//LogWrapper.d("TileTable", "after writingList.wait");
								
							}catch(Exception e){
								
							}
							if(stopWriting){
								LogWrapper.i("TileTable","writing thread break after writingList wait");
								break;
							}
						}
						
						if(writingList.size()>0) {
							tileFileEntry= writingList.poll();
							//LogWrapper.i("TileTable", "writing thread poll size:"+writingList.size());
						}
					}
					
					if(stopWriting){
						LogWrapper.i("TileTable","writing thread break before write");
						break;
					}
								
					if (tileFileEntry!=null) {
						try {
							//LogWrapper.d("TileTable", "write thread for:"+tileFileEntry.requestTile.toString());
							String fileName=tileFileEntry.name.toString()+"."+CONFIG.IMAGE_FORMAT;
							File file=new File(appPath,fileName);
							FileOutputStream out=new FileOutputStream(file);
							out.write(tileFileEntry.bmBytes);
							out.close();
							
							synchronized(cacheTileFileNames){
								cacheTileFileNames.put(fileName,true);
								//LogWrapper.i("TileTable", "startWritingThread write stage put:"+fileName);
							}
							
							if(cacheTileFileNames.size()>TKConfig.IMAGE_CACHE_SIZE_SDCARD){
								if(stopWriting){
									LogWrapper.i("TileTable","writing thread break after drawingLock remove stage");
									break;
								}
								
								String fileNameDel=null;
								synchronized(cacheTileFileNames){
									fileNameDel=cacheTileFileNames.entrySet().iterator().next().getKey(); 
									cacheTileFileNames.remove(fileNameDel);
									//LogWrapper.i("TileTable", "startWritingThread write stage remove:"+fileNameDel);
								}
								if(fileNameDel!=null){
									File fileDel=new File(appPath,fileNameDel);
									fileDel.delete();
								}
								
							}
										
						} catch (Exception e) {
							LogWrapper.e("TileTable", e.getMessage());
						}
					}
				}
				LogWrapper.i("TileTable", "writing thread end");
			}
		};
		t.start();
	
	}
	
	/**
	 * 根据文件名返回Bitmap
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public Bitmap getImage(String name) throws Exception {
		if (TKConfig.IMAGE_CACHE_SIZE_SDCARD<=0 || !externalStorage)
			return null;
		Bitmap bm = null;
		
		try {
			String fileName=name+"."+CONFIG.IMAGE_FORMAT;
			synchronized(cacheTileFileNames){
				Boolean v=cacheTileFileNames.get(fileName);
				//LogWrapper.i("TileTable", "getTile:"+fileName+", exist:"+((v==null||!v)?"false":true));
				if(v==null || !v){
					return null;
				}
			}
			
			File file=new File(appPath,fileName);
			if (file.exists()) {
				//bm = BitmapFactory.decodeStream(new FileInputStream(file));
				bm=BitmapFactory.decodeFile(file.getAbsolutePath());
			}
		} catch (Exception e) {
			LogWrapper.e("TileTable", e.getMessage());
		}

		return bm;
	}
	
	/**
	 * 将图片数据写入指定名称的文件
	 * @param name
	 * @param bmBytes
	 * @throws Exception
	 */
	public void putImage(String name, byte[] bmBytes) throws Exception {
		if (TKConfig.IMAGE_CACHE_SIZE_SDCARD<=0 || !externalStorage)
			return;

		synchronized(writingList){
			writingList.addFirst(new ImageData(name,bmBytes));
			//LogWrapper.i("TileTable", "putTile size:"+writingList.size());
			
			if(writingList.size()>MAX_WRITING_LIST_SIZE){
				//LogWrapper.i("TileTable", "putTile size:"+writingList.size()+" exceeds maximum:"+MAX_WRITING_LIST_SIZE);
				writingList.removeLast();
			}
			writingList.notify();
		}
		
	}

	/**
	 * 清除所有缓存图片文件
	 */
	public void clearImages() {
		if (!externalStorage)
			return;

		try {
			File file = new File(appPath);
			if (!appPath.equals("") && file.exists() && file.isDirectory()) {
				File[] children = file.listFiles();
				for (int i = 0; i < children.length; i++) {
					children[i].delete();
				}
			}
			cacheTileFileNames.clear();
		} catch (Exception e) {
			LogWrapper.e("TileTable", e.getMessage());
		}

	}
	
	/**
	 * 结束写图片文件线程，当缓存的图片文件数目超过限制时，则删除其访问时间离当前较久的一些图片文件
	 */
	public void stopWritingAndRemoveOldTiles() {
		stopWriting=true;
		
		synchronized(writingList){
			writingList.notifyAll();
		}
		
		stopRemoveCache=false;
		
		if (!externalStorage)
			return;
		
		if(appPath==null || appPath.equals("")){
			return;
		}

		LogWrapper.d("TileTable", "stopWritingAndRemoveOldTiles begin");
		new Thread(){
			@Override
			public void run() {
				try {
					LogWrapper.i("TileTable", "stopWritingAndRemoveOldTiles thread start, cache length:"+cacheTileFileNames.size());
					while(cacheTileFileNames.size()>TKConfig.IMAGE_CACHE_SIZE_SDCARD){
						if(stopRemoveCache){
							LogWrapper.i("TileTable", "stopWritingAndRemoveOldTiles break, cache length:"+cacheTileFileNames.size());
							break;
						}
						
						String fileName=cacheTileFileNames.entrySet().iterator().next().getKey();
						cacheTileFileNames.remove(fileName);
						if(fileName!=null){
							File file=new File(appPath,fileName);
							file.delete();
						}
					}
				} catch (Exception e) {
					LogWrapper.e("TileTable", e.getMessage());
				}
				
				LogWrapper.i("TileTable", "stopWritingAndRemoveOldTiles thread end");
			}
		}.start();
	}

	/*private boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return dir.delete();
	}*/
}

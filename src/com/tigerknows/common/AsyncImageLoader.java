
package com.tigerknows.common;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.weibo.sdk.android.WeiboParameters;

import org.apache.http.client.HttpClient;

import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;

public class AsyncImageLoader {
    
    
    // 为了加快速度，在内存中开启缓存（主要应用于重复图片较多时，或者同一个图片要多次被访问，比如在ListView时来回滚动）
    public LinkedHashMap<String, SoftReference<BitmapDrawable>> imageCache = new LinkedHashMap<String, SoftReference<BitmapDrawable>>(TKConfig.IMAGE_CACHE_SIZE_MEMORY,0.75f,true);

    private ExecutorService executorService = null; // 固定五个线程来执行任务

    private final Handler handler = new Handler();
    
    /*
     * 网络异常时，建立HttpClient的时间间隔
     */
    static final int NEW_HTTP_CLIENT_TIME_INTERVAL = 500;
    
    private HttpClient mHttpClient;
    
    private String viewToken = "";

    class CallbackBitmapRunnable implements Runnable{
    	
    	ImageCallback mCallback;
    	BitmapDrawable mDrawable;
    	
    	CallbackBitmapRunnable(ImageCallback callback, BitmapDrawable drawable){
    		this.mCallback=callback;
    		this.mDrawable = drawable;
    	}
    	
		public void run() {
			mCallback.imageLoaded(mDrawable);
		}
		
	}
    
    private BitmapDrawable checkMemoryAndDisk(final Context context, final TKURL imageUrl) {
    	// 如果缓存过就从缓存中取出数据
        if (TKConfig.CACHE_BITMAP_TO_MEMORY) {
            if (imageCache.containsKey(imageUrl.url)) {
                SoftReference<BitmapDrawable> softReference = imageCache.get(imageUrl.url);
                BitmapDrawable bm = softReference.get();
                if (bm != null) {
                    return bm;
                }
            }
        }
        final ImageCache imageCache1 = Globals.getImageCache();
        final String name = imageUrl.url.substring(imageUrl.url.lastIndexOf("/")+1);
        Bitmap bitmap = null;
        try {
            bitmap = imageCache1.getImage(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
            bitmapDrawable.setTargetDensity(Globals.g_metrics);
            if (TKConfig.CACHE_BITMAP_TO_MEMORY) {
                imageCache.put(imageUrl.url, new SoftReference<BitmapDrawable>(bitmapDrawable));
            }
            return bitmapDrawable;
        }
        return null;
    }
    
    /**
     * @param imageUrl 图像url地址
     * @param callback 回调接口
     * @return 返回内存中缓存的图像，第一次加载返回null
     */
    public BitmapDrawable loadDrawable(final Context context, final TKURL imageUrl, final ImageCallback callback) {
        
    	BitmapDrawable b = checkMemoryAndDisk(context, imageUrl);
    	if (b != null) {
    		return b;
    	}

        if (viewToken.equals(imageUrl.viewToken) == false || executorService == null || executorService.isShutdown() || context == null || callback == null) {
            return null;
        }
        
        // 缓存中没有图像，则从网络上取出数据，并将取出的数据缓存到内存中
        executorService.submit(new Runnable() {
            public void run() {
            	
                try {
                	  BitmapDrawable bitmapDrawable;
                	  bitmapDrawable = checkMemoryAndDisk(context, imageUrl);
                	  if(bitmapDrawable == null){
                		  String signal = testAndPutUrlInList(imageUrl.url);
                		  if (signal != null) {
                			  synchronized (signal) {
                    			  signal.wait();	
                    			  bitmapDrawable = checkMemoryAndDisk(context, imageUrl);
							  }
                		  }else{
                			  
                			  //load image from url
                			  bitmapDrawable = loadImageFromUrl(context, imageUrl);
                			  //Put the image into the cache
                			  if (bitmapDrawable != null) {
                				  if (TKConfig.CACHE_BITMAP_TO_MEMORY) {
                					  imageCache.put(imageUrl.url, new SoftReference<BitmapDrawable>(bitmapDrawable));
                				  }
                			  }

                			  removeDownloadTaskAndNotifyOthers(imageUrl.url);
                			  //Everything is ok and notify others

                			  synchronized (imageUrl.url) {
                    			  imageUrl.url.notifyAll();	
							}
                			  
                		  }
                	  }
              	      handler.post(new CallbackBitmapRunnable(callback, bitmapDrawable));
              	  
                   
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return null;
    }
    
    private List<String> mDwonloadList = new ArrayList<String>(10);

    /**
     * test whether the url has been in the download list
     * If it's in the download list, return the corrresponding url in the list
     * If it's not in the download list, return null and put the url in the list;
     * @param url
     */
    private String testAndPutUrlInList(String url){
    	int index = 0;
    	synchronized (mDwonloadList) {
    		index = mDwonloadList.indexOf(url);
    		if(index == -1){
    			mDwonloadList.add(url);
    			return null;
    		}else{
        		return mDwonloadList.get(index);	
    		}
		}
    }

    private void removeDownloadTaskAndNotifyOthers(String url){
    	
    	synchronized (mDwonloadList) {
    		mDwonloadList.remove(url);
		}
    	
    }
    
    // 从网络上取数据方法
    protected BitmapDrawable loadImageFromUrl(Context context, TKURL imageUrl) {
        try {
            if (viewToken.equals(imageUrl.viewToken) == false) {
                return null;
            }
            
            if (mHttpClient == null) {
                mHttpClient = HttpManager.getNewHttpClient();
            }
            
            try {
            	long start = System.currentTimeMillis();
            	LogWrapper.d("AsyncImageLoader", "imageUrl.url="+imageUrl.url);
				byte[] data = HttpManager.openUrl(context, mHttpClient, imageUrl.url, "GET", new WeiboParameters());
				ImageCache imageCache1 = Globals.getImageCache();
				final String name = imageUrl.url.substring(imageUrl.url.lastIndexOf("/")+1);
				imageCache1.putImage(name, data);
				BitmapDrawable bm = (BitmapDrawable) BitmapDrawable.createFromStream(new ByteArrayInputStream(data), "image.png");
            	
//				URL url = new URL(imageUrl.url);
//				BitmapDrawable bm = (BitmapDrawable) BitmapDrawable.createFromStream(url.openConnection().getInputStream(), "image.png");

                LogWrapper.d("AsyncImageLoader", "Downloaded imageUrl.url="+imageUrl.url);
                
                LogWrapper.d("AsyncImageLoader", "Downloaded Time: " + (System.currentTimeMillis()-start)/1000.0);
                
                bm.setTargetDensity(Globals.g_metrics);
                return bm;
            } catch (Exception e) {
                LogWrapper.d("AsyncImageLoader", "Failed="+imageUrl.url);
                mHttpClient = null;
                e.printStackTrace();
            }
            
            if (mHttpClient == null) {
                Thread.sleep(NEW_HTTP_CLIENT_TIME_INTERVAL);
            }
            
            return null;

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    // 对外界开放的回调接口
    public interface ImageCallback {
        // 注意 此方法是用来设置目标对象的图像资源
        public void imageLoaded(BitmapDrawable imageDrawable);
    }
    
    public void onResume() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(1); // 固定五个线程来执行任务
        }
    }
    
    public void onDestory() {
        stop("");
        if (executorService != null && executorService.isShutdown() == false) {
            executorService.shutdown();
        }
        executorService = null;
        imageCache.clear();
    }
    
    public void setViewToken(String viewToken) {
        this.viewToken = viewToken;
    }
    
    public void stop(String viewToken) {
        this.viewToken = "";
        HttpClient httpClient = mHttpClient;
        if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
            mHttpClient = null;
        }
    }
    
    public static class TKURL {
        public String url;
        public String viewToken;
        
        public TKURL(String url, String viewToken) {
            this.url = url;
            this.viewToken = viewToken;
        }
    }
    
    public void put(final String url, BitmapDrawable bitmapDrawable) {
        if (TKConfig.CACHE_BITMAP_TO_MEMORY) {
            if (imageCache.containsKey(url) == false) {
                imageCache.put(url, new SoftReference<BitmapDrawable>(bitmapDrawable));
            }
        }
    }
}

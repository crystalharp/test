
package com.tigerknows.util;

import com.decarta.Globals;
import com.tigerknows.ImageCache;
import com.tigerknows.net.TKParameters;
import com.tigerknows.net.Utility;

import org.apache.http.client.HttpClient;

import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;

public class AsyncImageLoader {
    
    public static int CACHE_SIZE = 32;
    
    // 为了加快速度，在内存中开启缓存（主要应用于重复图片较多时，或者同一个图片要多次被访问，比如在ListView时来回滚动）
    public LinkedHashMap<String, SoftReference<BitmapDrawable>> imageCache = new LinkedHashMap<String, SoftReference<BitmapDrawable>>(CACHE_SIZE,0.75f,true);

    private ExecutorService executorService = null; // 固定五个线程来执行任务

    private final Handler handler = new Handler();
    
    private HttpClient mHttpClient;
    
    private String viewToken = "";

    /**
     * @param imageUrl 图像url地址
     * @param callback 回调接口
     * @return 返回内存中缓存的图像，第一次加载返回null
     */
    public BitmapDrawable loadDrawable(final Context context, final TKURL imageUrl, final ImageCallback callback) {
        // 如果缓存过就从缓存中取出数据
        if (imageCache.containsKey(imageUrl.url)) {
            SoftReference<BitmapDrawable> softReference = imageCache.get(imageUrl.url);
            if (softReference.get() != null) {
                return softReference.get();
            }
        }
        final ImageCache imageCache1 = Globals.getImageCache();
        final String name = imageUrl.url.substring(imageUrl.url.lastIndexOf("/")+1);
        Bitmap bitmap = null;
        try {
            bitmap = imageCache1.getTile(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
            imageCache.put(imageUrl.url, new SoftReference<BitmapDrawable>(bitmapDrawable));
            return bitmapDrawable;
        }

        if (viewToken.equals(imageUrl.viewToken) == false || executorService == null || executorService.isShutdown() || context == null || callback == null) {
            return null;
        }
        
        // 缓存中没有图像，则从网络上取出数据，并将取出的数据缓存到内存中
        executorService.submit(new Runnable() {
            public void run() {
                try {
                    final BitmapDrawable bitmapDrawable = loadImageFromUrl(context, imageUrl);

                    if (bitmapDrawable != null) {
                        imageCache.put(imageUrl.url, new SoftReference<BitmapDrawable>(bitmapDrawable));
                    }
                    
                    handler.post(new Runnable() {
                        public void run() {
                            callback.imageLoaded(bitmapDrawable);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return null;
    }

    // 从网络上取数据方法
    protected BitmapDrawable loadImageFromUrl(Context context, TKURL imageUrl) {
        try {
            if (viewToken.equals(imageUrl.viewToken) == false) {
                return null;
            }
            if (mHttpClient == null) {
                mHttpClient = Utility.getNewHttpClient(context);
            }
            try {
                byte[] data = Utility.openUrl(context, mHttpClient, imageUrl.url, "GET", new TKParameters());
                ImageCache imageCache1 = Globals.getImageCache();
                final String name = imageUrl.url.substring(imageUrl.url.lastIndexOf("/")+1);
                imageCache1.putTile(name, data);
                return (BitmapDrawable) BitmapDrawable.createFromStream(new ByteArrayInputStream(data), "image.png");
            } catch (Exception e) {
                mHttpClient = null;
                e.printStackTrace();
            }
            return null;
//            return (BitmapDrawable) BitmapDrawable.createFromStream(new URL(imageUrl).openStream(), "image.png");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        if (imageCache.containsKey(url) == false) {
            imageCache.put(url, new SoftReference<BitmapDrawable>(bitmapDrawable));
        }
    }
}

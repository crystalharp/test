package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.common.ImageCache;
import com.tigerknows.common.AsyncImageLoader.ImageCallback;
import com.tigerknows.common.AsyncImageLoader.TKURL;
import com.tigerknows.model.xobject.XMap;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;

public class TKDrawable extends XMapData implements Parcelable {
    
    // 0x01 x_string    图片的url 
    public static final byte FIELD_URL = 0x01;
    
    // 0x02 x_binary_data   图片的内容 
    public static final byte FIELD_DATA = 0x02;
    
    private String url;
    
    public TKDrawable() {
        this.data = new XMap();
    }
    
    public TKDrawable(XMap data) throws APIException {
        super(data);
        
        this.url = getStringFromData(FIELD_URL);
        byte[] image = getBytesFromData(FIELD_DATA);
        if (image != null) {
            ImageCache imageCache1 = Globals.getImageCache();
            final String name = url.substring(url.lastIndexOf("/")+1);
            try {
                imageCache1.putImage(name, image);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            BitmapDrawable bitmapDrawable = (BitmapDrawable) BitmapDrawable.createFromStream(new ByteArrayInputStream(image), "image.png");
            Globals.getAsyncImageLoader().put(url, bitmapDrawable);
        }
    }

    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        getData().put(FIELD_URL, url);
        this.url = url;
    }
    
    public TKDrawable clone() {
        TKDrawable tkDrawable = null;
        try {
            tkDrawable = new TKDrawable(getData());
        } catch (APIException e) {
            e.printStackTrace();
        }
        return tkDrawable;
    }

    public Drawable loadDrawable(final Activity activity, final Runnable action, final String viewToken) {
        Drawable drawable = null;
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        // 如果缓存过就会从缓存中取出图像，ImageCallback接口中方法也不会被执行
        BitmapDrawable cacheImage = Globals.getAsyncImageLoader().loadDrawable(activity, new TKURL(url, viewToken),
                new ImageCallback() {
                    // 请参见实现：如果第一次加载url时下面方法会执行
                    public void imageLoaded(BitmapDrawable imageDrawable) {
                        if (activity != null && action != null) {
                            activity.runOnUiThread(action);
                        }
                    }
                });
        drawable = cacheImage;
        return drawable;
    }

    public static final Parcelable.Creator<TKDrawable> CREATOR
            = new Parcelable.Creator<TKDrawable>() {
        public TKDrawable createFromParcel(Parcel in) {
            return new TKDrawable(in);
        }

        public TKDrawable[] newArray(int size) {
            return new TKDrawable[size];
        }
    };

    
    private TKDrawable(Parcel in) {
        url = in.readString();
    }
    
    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(url);
    }
    
    public static XMapInitializer<TKDrawable> Initializer = new XMapInitializer<TKDrawable>() {

        @Override
        public TKDrawable init(XMap data) throws APIException {
            return new TKDrawable(data);
        }
    };
    
    public static class LoadImageRunnable implements Runnable {

        public Activity activity;
        public TKDrawable tkDrawable;
        public ImageView pictureImv;
        public int backgroundResId = -1;
        public String token;
        
        public LoadImageRunnable(Activity activity, TKDrawable tkDrawable, ImageView pictureImv, int backgroundResId, String token) {
            this.activity = activity;
            this.tkDrawable = tkDrawable;
            this.pictureImv = pictureImv;
            this.backgroundResId = backgroundResId;
            this.token = token;
        }
        
        @Override
        public void run() {
            Drawable drawable = tkDrawable.loadDrawable(activity, LoadImageRunnable.this, token);
            if(drawable != null) {
                Rect bounds = drawable.getBounds();
                if (bounds != null && bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight()) {
                    pictureImv.setBackgroundDrawable(null);
                }
                pictureImv.setBackgroundDrawable(drawable);
            } else {
                if (backgroundResId != -1) {
                    pictureImv.setBackgroundResource(backgroundResId);
                } else {
                    pictureImv.setBackgroundDrawable(null);
                }
            }
            
        }
        
    }
}

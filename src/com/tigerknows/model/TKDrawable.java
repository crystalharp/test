package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.ImageCache;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.AsyncImageLoader.ImageCallback;
import com.tigerknows.util.AsyncImageLoader.TKURL;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;

public class TKDrawable extends XMapData implements Parcelable {
    
    // 0x01 x_string    图片的url 
    public static final byte FIELD_URL = 0x01;
    
    // 0x02 x_binary_data   图片的内容 
    public static final byte FIELD_DATA = 0x02;
    
    private String url;
    
    public TKDrawable(XMap data) throws APIException {
        super(data);
        
        if (this.data.containsKey(FIELD_URL)) {
            this.url = this.data.getString(FIELD_URL);
        }
        
        if (this.data.containsKey(FIELD_DATA)) {
            byte[] bm = this.data.getBytes(FIELD_DATA);
            ImageCache imageCache1 = Globals.getImageCache();
            final String name = url.substring(url.lastIndexOf("/")+1);
            try {
                imageCache1.putImage(name, bm);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            BitmapDrawable bitmapDrawable = (BitmapDrawable) BitmapDrawable.createFromStream(new ByteArrayInputStream(bm), "image.png");
            Globals.getAsyncImageLoader().put(url, bitmapDrawable);
        }
    }

    public String getUrl() {
        return url;
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
                        if (imageDrawable == null) {
                            return;
                        }
                        if (activity != null && action != null)
                            activity.runOnUiThread(action);
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
}

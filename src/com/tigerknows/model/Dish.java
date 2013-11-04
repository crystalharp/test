package com.tigerknows.model;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.android.app.TKApplication;
import com.tigerknows.model.Comment.LocalMark;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜品
 * @author pengwenyue
 *
 */
public class Dish extends XMapData {
    
    // 0x01     x_int   菜品id    dish_id
    public static final byte FIELD_DISH_ID = 0x01;
    
    // 0x02     x_string    菜品所属商会的uid  link_uid
    public static final byte FIELD_LINK_UID = 0x02;
    
    // 0x03     x_string    商户城市id  city_id
    public static final byte FIELD_CITY_ID = 0x03;
    
    // 0x04     x_string    菜品名称    food_name
    public static final byte FIELD_FOOD_NAME = 0x04;
    
    // 0x05     x_string    菜品价格描述，多个价格用';'隔开   
    public static final byte FIELD_PRICE = 0x05;
    
    // 0x06     x_string    菜品价格，reserved，近期直接用0x05的字符串表示价格就好   food_prices, json array[food_price]格式,
    public static final byte FIELD_PRICE_JSON = 0x06;
    
    // 0x07     x_map   默认图片    default_picture
    public static final byte FIELD_DEFAULT_PICTURE = 0x07;
    
    // 0x08     x_int   图片总数    解析food_pictures，可得
    public static final byte FIELD_FOOD_PICTURES = 0x08;
    
    // 0x09     x_int   喜欢数     hit_count 
    public static byte FIELD_HIT_COUNT = 0x09;

    protected static final String NEED_FIELD = "010203040506070809";

    private long dishId;
    private String linkUid;
    private String cityId;
    private String name;
    private String price;
    private HotelTKDrawable picture;
    private HotelTKDrawable pictureAll;
    private HotelTKDrawable pictureRecommend;
    private long pictureCount;
    private long hitCount;
    private boolean isLike = false;
    private static LocalMark sLocalMark = null;
    
    private List<HotelTKDrawable> pictureList;
    private List<HotelTKDrawable> originalPictureList;
    public int categoryIndex = 0;
    public long likeTimeStamp = 0;
    
    public static LocalMark getLocalMark() {
        if (sLocalMark == null) {
            sLocalMark = new LocalMark("dish", BaseQuery.DATA_TYPE_DISH);
        }
        return sLocalMark;
    }
    
    public boolean isLike() {
        return isLike;
    }

    public void addLike(boolean refreshTimeStamp) {
        if (this.isLike == false) {
            hitCount += 1;
        }
        this.isLike = true;
        if (refreshTimeStamp) {
            this.likeTimeStamp = System.currentTimeMillis();
        }
    }

    public void deleteLike() {
        if (this.isLike) {
            hitCount -= 1;
        }
        this.isLike = false;
    }

    public void setLike(boolean isLike) {
        this.isLike = isLike;
    }

    public Dish() {
    }

    public Dish (XMap data) throws APIException {
        super(data);
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        super.init(data, reset);
        this.dishId = getLongFromData(FIELD_DISH_ID, reset ? 0 : this.dishId);
        this.linkUid = getStringFromData(FIELD_LINK_UID, reset ? null : this.linkUid);
        this.cityId = getStringFromData(FIELD_CITY_ID, reset ? null : this.cityId);
        this.name = getStringFromData(FIELD_FOOD_NAME, reset ? null : this.name);
        this.price = getStringFromData(FIELD_PRICE, reset ? null : this.price);
        this.picture = getObjectFromData(FIELD_DEFAULT_PICTURE, HotelTKDrawable.Initializer, reset ? null : this.picture);
        if (this.picture != null) {
            HotelTKDrawable hotelTKDrawable = new HotelTKDrawable();
            hotelTKDrawable.setName(this.picture.getName());
            TKDrawable tkDrawable = new TKDrawable();
            tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.picture.getTKDrawable().getUrl(), Globals.getPicWidthHeight(TKConfig.PICTURE_DISH_ALL)));
            hotelTKDrawable.setTkDrawable(tkDrawable);
            this.pictureAll = hotelTKDrawable;
            hotelTKDrawable = new HotelTKDrawable();
            hotelTKDrawable.setName(this.picture.getName());
            tkDrawable = new TKDrawable();
            tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(this.picture.getTKDrawable().getUrl(), Globals.getPicWidthHeight(TKConfig.PICTURE_DISH_RECOMMEND)));
            hotelTKDrawable.setTkDrawable(tkDrawable);
            this.pictureRecommend = hotelTKDrawable;
        } else {
            this.pictureAll = null;
            this.pictureRecommend = null;
        }
        this.pictureCount = getLongFromData(FIELD_FOOD_PICTURES, reset ? 0 : this.pictureCount);
        this.hitCount = getLongFromData(FIELD_HIT_COUNT, reset ? 0 : this.hitCount);
        
        if (reset == false) {
            this.data = null;
        }
        
        likeTimeStamp = getLocalMark().findCommend(TKApplication.getInstance(), String.valueOf(dishId), false);
        if (likeTimeStamp > 0) {
            addLike(false);
        } else {
            likeTimeStamp = getLocalMark().findCommend(TKApplication.getInstance(), String.valueOf(dishId), true);
            isLike = (likeTimeStamp > 0);
        }
    }

    public String getLinkUid() {
        return linkUid;
    }

    public String getCityId() {
        return cityId;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public HotelTKDrawable getPicture() {
        return picture;
    }

    public HotelTKDrawable getPictureAll() {
        return pictureAll;
    }

    public HotelTKDrawable getPictureRecommend() {
        return pictureRecommend;
    }

    public long getDishId() {
        return dishId;
    }

    public long getPictureCount() {
        return pictureCount;
    }

    public long getHitCount() {
        return hitCount;
    }

    public List<HotelTKDrawable> getPictureList() {
        return pictureList;
    }

    public List<HotelTKDrawable> getOriginalPictureList() {
        return originalPictureList;
    }

    public void setOriginalPictureList(List<HotelTKDrawable> originalPictureList) {
        this.originalPictureList = originalPictureList;
        if (this.originalPictureList != null) {
            pictureList = new ArrayList<Hotel.HotelTKDrawable>();
            for(int i = 0, size = originalPictureList.size(); i < size; i++) {
                HotelTKDrawable originalHotelTKDrawable = originalPictureList.get(i);
                HotelTKDrawable hotelTKDrawable = new HotelTKDrawable();
                hotelTKDrawable.setName(originalHotelTKDrawable.getName());
                if (originalHotelTKDrawable.getTKDrawable() != null) {
                    TKDrawable tkDrawable = new TKDrawable();
                    tkDrawable.setUrl(Utility.getPictureUrlByWidthHeight(originalHotelTKDrawable.getTKDrawable().getUrl(), Globals.getPicWidthHeight(TKConfig.PICTURE_HOTEL_LIST)));
                    hotelTKDrawable.setTkDrawable(tkDrawable);
                }
                pictureList.add(hotelTKDrawable);
            }
        } else {
            pictureList = null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        
        if (object instanceof Dish) {
            Dish other = (Dish) object;
            if(this.dishId != other.dishId) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    public static XMapInitializer<Dish> Initializer = new XMapInitializer<Dish>() {

        @Override
        public Dish init(XMap data) throws APIException {
            return new Dish(data);
        }
    };
}

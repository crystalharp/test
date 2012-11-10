package com.tigerknows.maps;

import com.decarta.android.util.Util;
import com.decarta.android.util.XYDouble;
import com.decarta.android.util.XYInteger;
import com.tigerknows.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.NinePatchDrawable;

/**
 * struct label
{
    char *name;                //标注名字
    int font_color;      //颜色 
    int font_size;             //大小
    float slope;                 //角度,正弦值
    int outline_color;          //描边颜色
    int x;
    int y;     //显示位置 

};
 * @author zhouwentao
 *
 */
public class MapWord {
    
    public static class Icon {
        
        public static final int TYPE_NORMAL = 0;
        public static final int TYPE_EXPEND =1;

        public static final int INDEX_AREA = 35;

        public static final int[] RESOURCE_ID = {R.drawable.ic_map_residential, R.drawable.ic_map_train_station, R.drawable.ic_map_bus_station,
            R.drawable.ic_map_subway, R.drawable.ic_map_airport2, R.drawable.ic_map_hospital,
            R.drawable.ic_map_port, R.drawable.ic_map_hotel, R.drawable.ic_map_government1,
            R.drawable.ic_map_government2, R.drawable.ic_map_market, R.drawable.ic_map_building,
            R.drawable.ic_map_school, R.drawable.ic_map_stadium, R.drawable.ic_map_park,
            R.drawable.ic_map_museum, R.drawable.ic_map_park_2, R.drawable.ic_map_theater,
            R.drawable.ic_map_culture, R.drawable.ic_map_library, R.drawable.ic_map_bank,
            R.drawable.ic_map_tv_tower, R.drawable.ic_map_residential, R.drawable.ic_map_research,
            R.drawable.ic_map_hi_tech_park, R.drawable.ic_map_golf, R.drawable.icon,
            R.drawable.ic_map_tv, R.drawable.ic_map_capital, R.drawable.icon,
            R.drawable.ic_map_provincial_capital, R.drawable.ic_map_other_cities, R.drawable.icon,
            R.drawable.icon, R.drawable.ic_map_area, R.drawable.icon};
        
        private static Bitmap[] BitmapPool;
        private static NinePatchDrawable[] NinePatchDrawablePool;
        
        public static void init(Context context) {
            Resources resources = context.getResources();
            BitmapPool = new Bitmap[RESOURCE_ID.length];
            for(int i = 0; i < RESOURCE_ID.length; i++) {
                if (RESOURCE_ID[i] != R.drawable.icon) {
                    BitmapPool[i] = Util.decodeResource(resources, RESOURCE_ID[i]);
                }
            }
            NinePatchDrawablePool = new NinePatchDrawable[1];
            NinePatchDrawablePool[0] = (NinePatchDrawable)resources.getDrawable(R.drawable.ic_map_area);
        }
        
        public int index = -1;
        public int x;
        public int y;
        public XYDouble mercXY;
        public int type = TYPE_NORMAL;
        
        public Icon(int index, int x, int y) {
            this.index = index;
            this.x = x;
            this.y = y;
            if (INDEX_AREA == this.index) {
                type = TYPE_EXPEND;
            } else {
                type = TYPE_NORMAL;
            }
        }
        
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (null == object) return false;
            if (object instanceof Icon) {
                Icon other = (Icon) object;
                if (other.index != index) {
                    return false;
                } else if((null != other.mercXY && !other.mercXY.equals(this.mercXY)) || (null == other.mercXY && other.mercXY != this.mercXY)){
                    return false;
                } else {
                    return true;
                }
            }
            
            return false;
        }
        
        public Bitmap getBitmap() {
            return getBitmap(index);
        }
        
        public static Bitmap getBitmap(int index) {
            if (index < 0 || BitmapPool == null || index >= BitmapPool.length) {
                return null;
            }
            return BitmapPool[index];
        }
        
        public static NinePatchDrawable getNinePatchDrawable(int index) {
            if (NinePatchDrawablePool == null || NinePatchDrawablePool.length < 1) {
                return null;
            }
            if (index == INDEX_AREA) {
                return NinePatchDrawablePool[0];
            }
            return null;
        }
        
        @Override
        public String toString() {
            return "Icon [index=" + index + ", x=" + x + ", y=" + y
                    + "]";
        }
        
        private volatile int hashCode = 0;
        public int hashCode() {
            if (hashCode == 0) {
            int result = 17;
            result = 37*result + index;
            if (mercXY != null) {
                long value = Double.doubleToRawLongBits(mercXY.x);
                result += (int) (value ^ (value >>> 32));
                value = Double.doubleToRawLongBits(mercXY.y);
                result += (int) (value ^ (value >>> 32));
            }
            hashCode = result;
            }
            return hashCode;
        }
    }

    private String name;
    private int fontColor;
    private int fontSize;
    private int slope;
    private int outlineColor;
    private int x;
    private int y;
    public Icon icon;
    public XYDouble mercXY;
    
    public MapWord(String name, int fontColor, int fontSize, float slope, int outlineColor, int x, int y, Icon icon) {
        super();
        this.name = name;
        this.fontColor = fontColor;
        this.fontSize = fontSize;
        this.slope = (int) slope;
        if (this.slope < 0 || this.slope > 179) {
            this.slope = 0;
        }
        this.outlineColor = outlineColor;
        this.x = x;
        this.y = y;
        this.icon = icon;
    }
    public int getSlope() {
        return slope;
    }
    public void setSlope(int slope) {
        this.slope = slope;
    }
    public int getOutlineColor() {
        return outlineColor;
    }
    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getFontColor() {
        return fontColor;
    }
    public void setFontColor(int fontColor) {
        this.fontColor = fontColor;
    }
    public int getFontSize() {
        return fontSize;
    }
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "MapText [fontColor=" + fontColor + ", fontSize=" + fontSize + ", name=" + name
                + ", outlineColor=" + outlineColor + ", slope=" + slope + ", x=" + x + ", y=" + y
                + ", icon=" + icon
                + "]";
    }
    
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (null == object) return false;
        if (object instanceof MapWord) {
            MapWord other = (MapWord) object;
            if(other.fontColor != fontColor || other.fontSize != fontSize || 
                    other.slope != slope || other.outlineColor != outlineColor) {
                return false;
            } else if((null != other.name && !other.name.equals(this.name)) || (null == other.name && other.name != this.name)) {
                return false;
//            } else if((null != other.icon && !other.icon.equals(this.icon)) || (null == other.icon && other.icon != this.icon)) {
//                return false;
            } else if((null != other.mercXY && !other.mercXY.equals(this.mercXY)) || (null == other.mercXY && other.mercXY != this.mercXY)){
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }
    
    private volatile int hashCode = 0;
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37*result + fontColor;
            result = 37*result + fontSize;
            result = 37*result + slope;
            result = 37*result + outlineColor;
            if (name != null) {
                result += name.hashCode();
            }
    //        if (icon != null) {
    //            result += icon.hashCode();
    //        }
            if (mercXY != null) {
                long value = Double.doubleToRawLongBits(mercXY.x);
                result += (int) (value ^ (value >>> 32));
                value = Double.doubleToRawLongBits(mercXY.y);
                result += (int) (value ^ (value >>> 32));
            }
            hashCode = result;
        }
        return hashCode; 
    }
}

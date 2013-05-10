
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.decarta.android.location.Position;
import com.tigerknows.TKConfig;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pengwenyue
 */
public class XMapData {
    
    protected XMap data;
    
    public XMapData() {
    }
    
    public XMapData(XMap data) throws APIException {
        init(data, true);
    }
    
    public void init(XMap data, boolean reset) throws APIException {
        setData(data);
        if (this.data == null) {
            throw new APIException(APIException.RESPONSE_DATA_IS_EMPTY);
        }
    }
    
    public XMap getData() {
        return data;
    }
    
    public void setData(XMap data) {
        this.data = data;
    }
    
    
    public interface XMapInitializer<T> {
        public T init(XMap data) throws APIException;
    }
    
    /**
     * 根据key从data中获取List<T>类型的值
     * @param filedKey
     * @param initer
     * @return
     * @throws APIException
     */
    <T> List<T> getListFromData(byte filedKey, XMapInitializer<T> initer) throws APIException{
        return getListFromData(filedKey, initer, null);
        
    }
    
    /**
     * 根据key从data中获取List<T>类型的值
     * @param filedKey
     * @param initer
     * @return
     * @throws APIException
     */
    <T> List<T> getListFromData(byte filedKey, XMapInitializer<T> initer, List<T> defaultValue) throws APIException{

        List<T> result = null;
        if (data.containsKey(filedKey)) {
            @SuppressWarnings("unchecked")
            XArray<XMap> xarray = data.getXArray(filedKey);
            result = new ArrayList<T>();
            if (xarray != null) {
                for(int i = 0; i < xarray.size(); i++) {
                    result.add(initer.init(xarray.get(i)));
                }
            }
        } else {
            result = defaultValue;
        }
        return result;
        
    }
    
    /**
     * 根据key从data中获取T类型的值
     * @param filedKey
     * @param initer
     * @return
     * @throws APIException
     */
    <T> T getObjectFromData(byte filedKey, XMapInitializer<T> initer) throws APIException{
        return getObjectFromData(filedKey, initer, null);
    }
    
    /**
     * 根据key从data中获取T类型的值
     * @param filedKey
     * @param initer
     * @return
     * @throws APIException
     */
    <T> T getObjectFromData(byte filedKey, XMapInitializer<T> initer, T defaultValue) throws APIException{

        T result = null;
        if (data.containsKey(filedKey)) {
            result = initer.init(data.getXMap(filedKey));
        } else {
            result = defaultValue;
        }
        return result;
        
    }
    
    /**
     * 根据key从data中获取long类型的值    
     * @param key
     * @return
     */
    long getLongFromData(byte key) {
        return getLongFromData(key, 0);
    }
    
    /**
     * 根据key从data中获取long类型的值    
     * @param key
     * @return
     */
    long getLongFromData(byte key, long defaultValue) {
        return getLongFromData(this.data, key, defaultValue);
    }
    
    /**
     * 根据key从data中获取字符串类型的值
     * @param key
     * @return
     */
    String getStringFromData(byte key) {
        return getStringFromData(this.data, key, null);
    }
    
    /**
     * 根据key从data中获取字符串类型的值
     * @param key
     * @return
     */
    String getStringFromData(byte key, String defalut) {
        return getStringFromData(this.data, key, defalut);
    }
    
    /**
     * 根据key从data中获取Position类型的值
     * @param lonKey
     * @param latKey
     * @return
     */
    Position getPositionFromData(byte lonKey, byte latKey) {
        return getPositionFromData(lonKey, latKey, null);
    }
    
    /**
     * 根据key从data中获取Position类型的值
     * @param lonKey
     * @param latKey
     * @return
     */
    Position getPositionFromData(byte lonKey, byte latKey, Position defaultValue) {
        Position result = null;
        if (data.containsKey(lonKey) && data.containsKey(latKey)) {
            result = new Position(((double)data.getInt(latKey))/TKConfig.LON_LAT_DIVISOR, ((double)data.getInt(lonKey))/TKConfig.LON_LAT_DIVISOR);
        } else {
            result = defaultValue;
        }
        return result;
    }
    
    /**
     * 根据key从data中获取byte[]类型的值    
     * @param key
     * @return
     */
    byte[] getBytesFromData(byte key) {
        return getBytesFromData(key, null);
    }
    
    /**
     * 根据key从data中获取byte[]类型的值    
     * @param key
     * @return
     */
    byte[] getBytesFromData(byte key, byte[] defaultValue) {
        byte[] result = null;
        if (data.containsKey(key)) {
            result = this.data.getBytes(key);
        } else {
            result = defaultValue;
        }
        return result;
    }
    
    /**
     * 根据key从data中获取double类型的值
     * @param key
     * @param defaultValue
     * @return
     */
    double getDoubleFromData(byte key) {
        return getDoubleFromData(key, 0);
    }
    
    /**
     * 根据key从data中获取double类型的值
     * @param key
     * @param defaultValue
     * @return
     */
    double getDoubleFromData(byte key, double defaultValue) {
        double result = 0;
        if (this.data.containsKey(key)) {
            result = this.data.getDouble(key);
        } else {
            result = defaultValue;
        }
        return result;
    }

    /**
     * 根据key从xmap中获取String类型的值
     * @param xmap
     * @param key
     * @return
     */
    public static String getStringFromData(XMap xmap, byte key, String defaultValue) {
        String result = null;
        if (xmap.containsKey(key)) {
            result = xmap.getString(key);
        } else {
            result = defaultValue;
        }
        return result;
    }
    
    /**
     * 根据key从xmap中获取long类型的值
     * @param xmap
     * @param key
     * @return
     */
    public static long getLongFromData(XMap xmap, byte key, long defaultValue) {
        long result = 0;
        if (xmap.containsKey(key)) {
            result = xmap.getInt(key);
        } else {
            result = defaultValue;
        }
        return result;
    }
}

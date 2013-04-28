
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XArray;
import com.tigerknows.model.xobject.XMap;

import java.util.ArrayList;
import java.util.Comparator;
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
        this.data = data;
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
    
    public <T> List<T> getListFromXArray(XMapInitializer<T> initer, XMap data, byte filedKey) throws APIException{

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
            return result;
        }
        return result;
        
    }
    
}


package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XMap;

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
            throw new APIException(APIException.RESPONSE_DATA_IS_EMPTY, BaseQuery.STATUS_CODE_RESPONSE_EMPTY);
        }
    }
    
    public XMap getData() {
        return data;
    }
    
    public void setData(XMap data) {
        this.data = data;
    }
}

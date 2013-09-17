
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.model.xobject.XBinaryData;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.model.xobject.XObject;

import android.content.Context;

/**
 * 文件下载服务类
 * 
 * @author pengwenyue
 */
public final class FileDownload extends BaseQuery {

    public static final String SERVER_PARAMETER_FILE_TYPE = "type";

    public static final String SERVER_PARAMETER_VERSION = "version";

    public static final String SERVER_PARAMETER_OPTION = "option";

    // subway   地铁图
    public static final String FILE_TYPE_SUBWAY = "subway";

    public FileDownload(Context context) {
        super(context, API_TYPE_FILE_DOWNLOAD);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        DataResponse dataResponse = new DataResponse(responseXMap);
        this.response = dataResponse;
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        String[] ekeys = new String[]{SERVER_PARAMETER_FILE_TYPE, SERVER_PARAMETER_VERSION};
        String[] okeys = new String[]{SERVER_PARAMETER_OPTION};
        debugCheckParameters(ekeys, okeys);
    }
    
    public static class DataResponse extends Response {
        
        // 0x02     x_binary_data   文件数据，0x00为200时有效 
        static final byte FIELD_DATA = 0x02;
        
        private byte[] binaryData;
        
        public byte[] getBinaryData() {
            return binaryData;
        }

        public DataResponse(XMap data) throws APIException {
            super(data);

            if (data.containsKey(FIELD_DATA)) {
                XObject xobject = data.getXObject(FIELD_DATA);
                if (xobject instanceof XBinaryData) {
                    binaryData = ((XBinaryData) xobject).getValue();
                }
            }
        }
        
    }
}

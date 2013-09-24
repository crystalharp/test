
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
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

    // subway   地铁图
    public static final String FILE_TYPE_SUBWAY = "subway";

    public FileDownload(Context context) {
        super(context, API_TYPE_FILE_DOWNLOAD);
    }

    @Override
    protected void createHttpClient() {
        super.createHttpClient();
        String url = String.format(TKConfig.getFileDownloadUrl(), TKConfig.getFileDownloadHost());
        httpClient.setURL(url);
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        DataResponse dataResponse = new DataResponse(responseXMap);
        this.response = dataResponse;
    }

    @Override
    protected void checkRequestParameters() throws APIException {
        String[] ekeys = new String[]{SERVER_PARAMETER_FILE_TYPE};
        String[] okeys = new String[]{SERVER_PARAMETER_VERSION};
        debugCheckParameters(ekeys, okeys);
    }
    
    public static class DataResponse extends Response {
        
        // 0x02     x_binary_data   文件数据，0x00为200时有效 
        static final byte FIELD_DATA = 0x02;
        
        private FileData fileData;
        
        public FileData getFileData() {
            return fileData;
        }

        public DataResponse(XMap data) throws APIException {
            super(data);

            if (data.containsKey(FIELD_DATA)) {
                fileData = new FileData(data.getXMap(FIELD_DATA));
            }
        }
        
        public static class FileData extends XMapData {

            // 0x01     x_string    文件数据版本号 
            static final byte FIELD_VERSION = 0x01;
            
            // 0x02     x_string    url，即文件数据的下载地址 
            static final byte FIELD_URL = 0x02;
            
            private String version;
            
            private String url;
            
            public String getVersion() {
                return version;
            }

            public String getUrl() {
                return url;
            }

            public FileData(XMap data) throws APIException {
                super(data);
                
                version = getStringFromData(FIELD_VERSION);
                url = getStringFromData(FIELD_URL);
            }
        }
        
    }
}

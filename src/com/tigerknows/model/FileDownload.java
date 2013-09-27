
package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.map.MapEngine;
import com.tigerknows.model.FileDownload.DataResponse.FileData;
import com.tigerknows.model.test.BaseQueryTest;
import com.tigerknows.model.xobject.XMap;
import com.tigerknows.service.download.DownloadService;
import com.tigerknows.util.HttpUtils;
import com.tigerknows.util.Utility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    
    private boolean complete = false;
    
    public boolean isComplete() {
        return complete;
    }

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
        
        if (FILE_TYPE_SUBWAY.equals(getParameter(SERVER_PARAMETER_FILE_TYPE))) {
            if (dataResponse != null) {
                FileData fileData = dataResponse.getFileData();
                if (fileData != null && fileData.url != null) {
                    File file = downFile(fileData.url);
                    
                    if (file != null && !isStop) {
                        Utility.unZipFile(file.getAbsolutePath(), null, MapEngine.cityId2Floder(cityId));
                        
                        complete = true;
                    }
                }
            }
        }
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
    

    
    private File downFile(String url) {
        try {
            if (BaseQueryTest.UnallowedAccessNetwork) {
                Thread.sleep(5000);
                throw new IOException("Unallowed access network");
            }
            File tempFile = DownloadService.createFileByUrl(url);
            long fileSize = tempFile.length();
            HttpClient httpClient = HttpManager.getNewHttpClient();
            HttpUriRequest request = new HttpGet(url);
            
            // Range:(unit=first byte pos)-[last byte pos] 
            // 指定第一个字节的位置和最后一个字节的位置
            // 在http请求头加入RANGE指定第一个字节的位置
            if (fileSize > 0) {
                request.addHeader("RANGE", "bytes="+fileSize+"-");
            }
//            HttpResponse response = client.execute(new HttpGet(url));
            HttpResponse response = HttpUtils.execute(context, httpClient, request, url, "downloadService");
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();
            if(length == fileSize) {
                return tempFile;
            }
            InputStream is = entity.getContent();
            if(is != null) {
                BufferedInputStream bis = new BufferedInputStream(is);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile, true));
                int read = 0;
                byte[] buffer = new byte[1024];
                while (!isStop && (read = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                bos.flush();
                bos.close();
                is.close();
                bis.close();
                if (!isStop) {
                    return tempFile;
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}


package com.tigerknows.model;

import com.decarta.android.exception.APIException;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.android.net.HttpManager;
import com.tigerknows.crypto.DataEncryptor;
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
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    protected void addCommonParameters() {
        super.addCommonParameters();
        addSessionId();
    }

    @Override
    protected void translateResponse(byte[] data) throws APIException {
        super.translateResponse(data);
        DataResponse dataResponse = new DataResponse(responseXMap);
        this.response = dataResponse;
        
        if (FILE_TYPE_SUBWAY.equals(getParameter(SERVER_PARAMETER_FILE_TYPE))) {
            if (dataResponse != null) {
                FileData fileData = dataResponse.getFileData();
                String ver = TKConfig.getPref(context, TKConfig.getSubwayMapVersionPrefs(cityId), "");
                if (!TextUtils.isEmpty(ver) && !ver.equals(fileData.version)) {
                     TKConfig.setPref(context, TKConfig.getSubwayMapUpdatedPrefs(cityId), "true");
                }
                if (fileData != null && fileData.url != null && !ver.equals(fileData.version)) {
                    SubwayMapDownloadManager manager = SubwayMapDownloadManager.getInstance();
                    if (!manager.checkRunning(fileData.url)) {
                        manager.download(context, fileData.url, isStop, cityId);
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
    

    
    private static File downFile(Context context, HttpClient httpClient, String url, boolean isStop) {
        try {
            if (BaseQueryTest.UnallowedAccessNetwork) {
                Thread.sleep(5000);
                throw new IOException("Unallowed access network");
            }
            File tempFile = DownloadService.createFileByUrl(url);
            long fileSize = tempFile.length();
            HttpUriRequest request = new HttpGet(url);
            
            // 支持断点续传
            // Range:(unit=first byte pos)-[last byte pos] 
            // 指定第一个字节的位置和最后一个字节的位置
            // 在http请求头加入RANGE指定第一个字节的位置
            if (fileSize > 0) {
                request.addHeader("RANGE", "bytes="+fileSize+"-");
            }
//            HttpResponse response = client.execute(new HttpGet(url));
            HttpResponse response = HttpUtils.execute(context, httpClient, request, url, "fileDownload");
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();   //TODO: getContentLength()某些服务器可能返回-1
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
    
    static private class SubwayMapDownloadManager {
        
        static SubwayMapDownloadManager instance = null;
        String url = null;
        boolean downing = false;
        HttpClient httpClient;
        
        private SubwayMapDownloadManager() {
        }
        
        public static SubwayMapDownloadManager getInstance() {
            if (instance == null) {
                instance = new SubwayMapDownloadManager();
            }
            return instance;
        }
        
        public boolean checkRunning(String downUrl) {
            synchronized(this) { 
                //url相同则是同城市的第二个请求,直接返回
                if (downUrl.equals(url)) {
                    return true;
                }
                //url不同,且正在下载,则取消现在正在下载的文件
                if (downing) {
                    httpClient.getConnectionManager().shutdown();
                    url = null;
                }
            }
            return false;
        }
        
        private void recordDataInfo(Context context, int cityId) {
            String path = MapEngine.getSubwayMapPath(cityId);
            if (path == null) {
                return;
            }
            String versionFilePath = path + "version.txt";
            String ver = "error";
            long size = 0;
            try {
                FileInputStream fis = new FileInputStream(versionFilePath);
                byte[] buff = new byte[fis.available()];
                fis.read(buff);
                ver = new String(buff);
                size = MapEngine.calcSubwayDataSize(cityId);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            LogWrapper.d("conan", "subway map version:" + ver);
            LogWrapper.d("conan", "subway map size:" + size);
            TKConfig.setPref(context, TKConfig.getSubwayMapVersionPrefs(cityId), ver);
            TKConfig.setPref(context, TKConfig.getSubwayMapSizePrefs(cityId), String.valueOf(size));
        }
        
        public void download(Context context, String downUrl, boolean isStop, int cityId) {

            synchronized (this) {
                url = downUrl;
                downing = true;
                httpClient = HttpManager.getNewHttpClient();
            }
            File file = downFile(context, httpClient, downUrl, isStop);
            synchronized (this) {
                downing = false;
            }
            if (file != null && !isStop) {
                FileInputStream fis;
                FileOutputStream fos;
                String encFilePath = file.getAbsolutePath();
                String decFilePath = encFilePath + ".dec";
                try {
                    DataEncryptor encryptor = DataEncryptor.getInstance();
                    fis = new FileInputStream(encFilePath);
                    fos = new FileOutputStream(decFilePath);
                    byte[] filedata = new byte[4096];
                    int n = 0;
                    while ((n = fis.read(filedata)) != -1) {
                        encryptor.decrypt(filedata);
                        fos.write(filedata, 0, n);
                    }
                    Utility.unZipFile(decFilePath, null, MapEngine.cityId2Floder(cityId));
                    recordDataInfo(context, cityId);
                    fis.close();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    file.delete();
                    (new File(decFilePath)).delete();
                }
            }
            synchronized (this) {
                url = null;
            }
        }
    }
}

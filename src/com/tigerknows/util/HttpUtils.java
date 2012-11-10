/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.util;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.TKConfig;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author Peng Wenyue
 */
public class HttpUtils {
    private static final String TAG = "HttpUtils";

    private HttpUtils() {
        // To forbidden instantiate this class.
    }
    
    public static class TKHttpClient {
        private static int BUFFER_SIZE = 1024;
        private static int REAL_TIME_BUFFER_SIZE = 1024;
    
        public interface RealTimeRecive {
            public void reciveData(byte[] data);    
        }
        
        public interface ProgressUpdate {
            public void onProgressUpdate(int value);
        }
        
        private boolean keepAlive = false;
        
        private RealTimeRecive realTimeRecive;
        
        private byte[] data;
        
        private boolean receivedAllData;
                
        private AndroidHttpClient client = null;
        
        private String url;
        
        private int statusCode = 0;
        
        private boolean isStop = false;
        
        private String apiType;
        
        private List<NameValuePair> parameters;
        
        private ProgressUpdate progressUpdate;
        
        private boolean isEncrypt = false;
        
        public void setIsEncrypt(boolean isEncrypt) {
            this.isEncrypt = isEncrypt;
        }
        
        public void setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
        }
        
        public boolean isReceivedAllData() {
            return receivedAllData;
        }
        
        public void stop() {
            isStop = true;
            progressUpdate = null;
            close();
        }
        
        public void setRealTimeRecive(RealTimeRecive realTimeRecive) {
            this.realTimeRecive = realTimeRecive;
        }
        
        public void setProgressUpdate(ProgressUpdate progressUpdate) {
            this.progressUpdate = progressUpdate;
        }
        public int getStatusCode() {
            return statusCode;
        }
        
        public void setURL(String url) {
            this.url = url;
        }
        
        public void setApiType(String apiType) {
            this.apiType = apiType;
        }
        
        public void setParameters(List<NameValuePair> parameters) {
            this.parameters = parameters;
        }
        
        public TKHttpClient() {
        } 
        
        private void close() {
            if (client != null) {
                client.close();
                client = null;
            }
        }
        
        /**
         * 返回最近一次搜索结果的字节流.保证本结果不为null,如果无结果或出现异常，则结果为大小为0的字节数组.
         * @return the data
         */
        public byte[] getData() {
            return data;
        }
        
        public void launchTest(byte[] data, int statusCode) {
            this.data = data;
            this.statusCode = statusCode;
        }
        
        public void execute(Context context) throws IOException {
            statusCode = 0;
            if (url == null) {
                throw new IllegalArgumentException("URL must not be null.");
            }
            
            if (keepAlive) {
                isStop = false;
            }

            long reqTime = 0;
            long revTime = 0;
            long resTime = 0;
            String fail = "";
            try {
                HttpPost post = new HttpPost(url);
                
                if (null != parameters) {
                    
                    /**
                     * Revert BasicNameValue List to String by the default toString function;
                     * The default toString function use ',' as the separator;
                     * Base on the protocol with server side, we use '\n' as our separator;
                     * Encrypt the whole parameters string to byte array.
                     */
                        
                    HttpEntity reqEntity;
                    if (isEncrypt) {
                        StringBuilder s = new StringBuilder();
                        for(NameValuePair nameValuePair : parameters) {
                            if (s.length() > 0) {
                                s.append('&');
                            }
                            s.append(nameValuePair.getName());
                            s.append('=');
                            s.append(nameValuePair.getValue());
                        } 
                        /* A flag to indicate the interface version. */
                        post.addHeader("tkServiceType", "at=s&v=13");
                        post.addHeader("Content-Type", "multipart/form-data");               
                        
                        byte[] encryptParameters = DataEncryptor.encrypt(s.toString().getBytes(TKConfig.getEncoding()));
                        
                        reqEntity = new ByteArrayEntity(encryptParameters);
                    } else {
                        reqEntity = new UrlEncodedFormEntity(parameters, TKConfig.getEncoding());
                    }
                    
                    post.setEntity(reqEntity);
                    LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", url="+url);
                    LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", parameters="+parameters);
                }

                if (client == null) {
                    client = createHttpClient(context);
                }
                
                HttpResponse response = null;
                reqTime = System.currentTimeMillis();
                
                String proxyHost = android.net.Proxy.getDefaultHost();
                if (proxyHost != null) {
                    if (CommonUtils.checkMobileNetwork(context)) {
                        LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", proxyHost="+proxyHost);
                        HttpHost proxy = new HttpHost(android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort(), "http");
                        String domain = url.replace("http://", "");
                        domain = domain.substring(0, domain.indexOf("/"));
                        HttpHost target = new HttpHost(domain, 80, "http");
                        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                        response = client.execute(target, post);
                    } else {
                        response = client.execute(post);
                    }
                } else {
                    response = client.execute(post);
                }
                
                StatusLine status = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                
                statusCode = status.getStatusCode();
                LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", statusCode="+statusCode +", entity="+entity.getContentLength());
                if (status.getStatusCode() != 200) { // HTTP 200 is success.
                    throw new IOException("HTTP error: " + status.getStatusCode());
                }

                revTime = System.currentTimeMillis();
                if (entity != null) {
                    try {
                        // 从联想词汇服务器返回来的数据其长度总是-1，尽管其实是有实际数据内容的
//                        if (entity.getContentLength() > 0) {
                            DataInputStream dis = new DataInputStream(entity.getContent());

                            try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int len = 0;
                                long receivedSize = 1;
                                long size = entity.getContentLength();
                                if (null != progressUpdate && size > 0) {
                                    progressUpdate.onProgressUpdate((int)-size);
                                }
                                while (isStop == false && (len = dis.read(buffer, 0, BUFFER_SIZE)) > 0) {
                                    receivedSize += len;
                                    baos.write(buffer, 0, len);
                                    if (realTimeRecive != null) {
                                        if (receivedSize >= REAL_TIME_BUFFER_SIZE) {
                                            receivedSize = 0;
                                            realTimeRecive.reciveData(baos.toByteArray());
                                            baos.reset();
                                        }
                                    } 
                                    if (null != progressUpdate  && size > 0) {
                                      progressUpdate.onProgressUpdate(len);
                                    }
                                }
                                if (isStop) {
                                    LogWrapper.d(TAG, "TKHttpClient->sendAndRecive():apiType="+apiType+", stop http connection!");
                                }
                                if (realTimeRecive != null) {
                                    if (baos.size() > 0) {
                                        realTimeRecive.reciveData(baos.toByteArray());
                                    }
                                } else {
                                    progressUpdate = null;
                                    data = baos.toByteArray();
                                }
                                if (receivedSize >= size) {
                                    receivedAllData = true;
                                } else {
                                    receivedAllData = false;
                                }
                            } finally {
                                try {
                                    dis.close();
                                } catch (IOException e) {
                                    LogWrapper.e(TAG, "TKHttpClient->sendAndRecive():apiType="+apiType+", Error closing input stream: " + e.getMessage());
                                }
                            }
//                        }
                    } finally {
                        if (entity != null) {
                            entity.consumeContent();
                        }
                    }
                }
                resTime = System.currentTimeMillis();
            } catch (IOException e) {
                fail = e.toString();
                if (keepAlive) {
                    close();
                    client = createHttpClient(context);
                }
                throw e;
            } finally {
                if (!TextUtils.isEmpty(apiType)) {
                    ActionLog.getInstance(context).addNetworkAction(apiType, reqTime, revTime, resTime, fail);
                }
                if (!keepAlive) {
                    close();
                }
            }
        }
    }

    private static AndroidHttpClient createHttpClient(Context context) {
        String userAgent = TKConfig.getUserAgent();
        AndroidHttpClient client = AndroidHttpClient.newInstance(userAgent);
        HttpParams params = client.getParams();
        HttpProtocolParams.setContentCharset(params, TKConfig.getEncoding());

        // set the socket timeout
        int soTimeout = TKConfig.getHttpSocketTimeout();

        // Set the timeout in milliseconds until a connection is established.
        int timeoutConnection = TKConfig.getHttpSocketTimeout(); 
        HttpConnectionParams.setConnectionTimeout(params, timeoutConnection); 
        
        HttpConnectionParams.setSocketBufferSize(params, TKHttpClient.BUFFER_SIZE);

        // Log.d(TAG, "[HttpUtils] createHttpClient w/ socket timeout " + soTimeout + " ms, "
        //         + ", UA=" + userAgent);

        HttpConnectionParams.setSoTimeout(params, soTimeout);

//        WifiManager wifiManager = (WifiManager) context
//                .getSystemService(Context.WIFI_SERVICE);
//        if (!wifiManager.isWifiEnabled()) {
//            // 获取当前正在使用的APN接入点
//            Uri uri = Uri.parse("content://telephony/carriers/preferapn"); 
//            Cursor mCursor = context.getContentResolver().query(uri,
//                    null, null, null, null);
//            if (mCursor != null && mCursor.moveToFirst()) {
//                // 游标移至第一条记录，当然也只有一条
//                String proxyStr = mCursor.getString(mCursor
//                        .getColumnIndex("proxy"));
//                if (proxyStr != null && proxyStr.trim().length() > 0) {
//                    HttpHost proxy = new HttpHost(proxyStr, 80);
//                    client.getParams().setParameter(
//                            ConnRouteParams.DEFAULT_PROXY, proxy);
//                }
//            }
//        }
        return client;
    }
    
}

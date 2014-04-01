/*
 * Copyright (C) pengwenyue@tigerknows.com
 */

package com.tigerknows.util;

import com.decarta.android.util.LogWrapper;
import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.crypto.DataEncryptor;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.BaseQuery.IRequestParameters;
import com.tigerknows.model.test.BaseQueryTest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ScrollView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Peng Wenyue
 */
public class HttpUtils {
    private static final String TAG = "HttpUtils";
    
    public static final String CONTENT_ENCODING = "Content-Encoding";
    
    /** The Content-Type */
    public static final String CONTENT_TYPE = "Content-Type";
    
    /** The Content-Type for application/x-www-form-urlencoded. */
    public static final String APPLICATION_FORM_CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";
    
    /** The Content-Type for multipart/form-data. */
    public static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";
    
    /** Tigerknows服务器接口定义的Header */
    public static final String TK_SERVICE_TYPE = "tkServiceType";
    
    /** Tigerknows服务器接口定义的用于s的v13的Header */
    public static final String TK_SERVICE_TYPE_VALUE = "lakers";
    
    public static final String PARAMETER_SEPARATOR = "&";
    public static final String NAME_VALUE_SEPARATOR = "=";
    
    /**
     * 是否使用代理
     */
    public static boolean ProxyIsAvailable = true;

    private HttpUtils() {
        // To forbidden instantiate this class.
    }
    
    public static class TKHttpClient {
        
        private static int BUFFER_SIZE = 1024*5;
    
        public interface RealTimeRecive {
            public void reciveData(byte[] data);    
        }
        
        public interface ProgressUpdate {
            public void onProgressUpdate(int value);
        }
        
        private boolean keepAlive = false;
        
        private RealTimeRecive realTimeRecive;
        
        private byte[] data = null;
        
        private boolean receivedAllData = false;
                
        private AndroidHttpClient client = null;
        
        private String url;
        
        private int statusCode = 0;
        
        private boolean isStop = false;
        
        private String apiType;
        
        private boolean isReTry = false;
        
        private IRequestParameters parameters;
        
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
        
        public void setApiType(String apiType, boolean isReTry) {
            this.apiType = apiType;
            this.isReTry = isReTry;
        }
        
        public void setParameters(IRequestParameters parameters) {
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
        
        public static void modifyRequestData(final IRequestParameters parameters) {
            final Activity activity = BaseQueryTest.getActivity();
            if (TKConfig.ModifyRequestData && activity != null) {
                activity.runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {

                        ViewGroup viewGroup = BaseQueryTest.genViewToModifyParam(activity, parameters);
                        ScrollView scrollView = new ScrollView(activity);
                        scrollView.addView(viewGroup);
                        
                        Dialog dialog = Utility.showNormalDialog(activity, scrollView);
                        dialog.setOnDismissListener(new OnDismissListener() {
                            
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                parameters.add("test", "test");
                            }
                        });
                    }
                });
                while (true) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (parameters.getValue("test") != null) {
                        parameters.remove("test");
                        break;
                    }
                }
            }
        }
        
        public void execute(Context context) throws IOException {
            
            modifyRequestData(parameters);
            
            receivedAllData = false;
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
            long reqSize = 0;
            long revSize = 0;
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
                    byte[] data = null;
                    String postParam;
                    if (isEncrypt) {
                        // 服务器接收加密的数据时不需要URLEncoder
//                        StringBuilder buf = new StringBuilder();
//                        int j = 0;
//                        for (int loc = 0; loc < parameters.size(); loc++) {
//                            String key = parameters.getKey(loc);
//                            if (j != 0) {
//                                buf.append(PARAMETER_SEPARATOR);
//                            }
//                            buf.append(key).append(NAME_VALUE_SEPARATOR).append(parameters.getValue(loc));
//                            j++;
//                        }
                        /* A flag to indicate the interface version. */
                        post.addHeader(TK_SERVICE_TYPE, TK_SERVICE_TYPE_VALUE);
                        post.addHeader(CONTENT_TYPE, MULTIPART_FORM_CONTENT_TYPE);
                        postParam = parameters.getPostParam();
                        data = postParam.getBytes(TKConfig.getEncoding());
                        data = ZLibUtils.compress(data);
                        DataEncryptor.getInstance().encrypt(data);
                    } else {
                        post.addHeader(CONTENT_TYPE, APPLICATION_FORM_CONTENT_TYPE_VALUE);
//                        postParam = encodeParameters(parameters, TKConfig.getEncoding());
                        postParam = parameters.getEncodedPostParam(TKConfig.getEncoding());
                        data = postParam.getBytes(TKConfig.getEncoding());
                    }
                    reqSize = data.length;
                    reqEntity = new ByteArrayEntity(data);
                    
                    post.setEntity(reqEntity);
                    LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", url="+url);
                    LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", parameters="+postParam+", TKConfig.getEncoding()="+TKConfig.getEncoding());
                }
                
                if (BaseQueryTest.UnallowedAccessNetwork) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    throw new IOException("Unallowed access network");
                }

                if (client == null) {
                    client = createHttpClient(context);
                }
                
                reqTime = System.currentTimeMillis();
                HttpResponse response = HttpUtils.execute(context, client, post, url, apiType);
                
                StatusLine status = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                
                statusCode = status.getStatusCode();
                LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", statusCode="+statusCode +", entity="+entity.getContentLength()+",ProxyIsAvailable="+ProxyIsAvailable);
                if (status.getStatusCode() != 200) { // HTTP 200 is success.
                    throw new IOException("HTTP error: " + status.getStatusCode());
                }

                revTime = System.currentTimeMillis();
                if (entity != null) {
                    try {
                        // 从联想词汇服务器返回来的数据其长度总是-1，尽管其实是有实际数据内容的
                        long size = entity.getContentLength();
                        revSize = size;
//                        if (size > 0) {
                            DataInputStream dis = new DataInputStream(entity.getContent());
                            try {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int len = 0;
                                if (null != progressUpdate) {
                                    progressUpdate.onProgressUpdate((int)-size);
                                }
                                while (isStop == false && (len = dis.read(buffer, 0, BUFFER_SIZE)) > 0) {
                                    baos.write(buffer, 0, len);
                                    // 边看边下载，首先返回的数据是通用响应码和通用原始数据包，此两个cake估计其数据之和最大为256字节
                                    // 通用响应码 
                                    // 类型码   信息长度   响应码   描述
                                    // 0x00 0x15   uint2   uint2   string
                                    // 通用原始数据包
                                    // 类型码   信息长度   数据
                                    // 0x20 0x01   uint4   字节流 
                                    if (realTimeRecive != null && baos.size() > 256) {
                                        realTimeRecive.reciveData(baos.toByteArray());
                                        baos.reset();
                                    }
                                    if (null != progressUpdate) {
                                      progressUpdate.onProgressUpdate(len);
                                    }
                                }
                                if (isStop) {
                                    LogWrapper.d(TAG, "TKHttpClient->sendAndRecive():apiType="+apiType+", stop http connection!");
                                } else {
                                    if (baos.size() > 0) {
                                        if (realTimeRecive == null) {
                                            data = baos.toByteArray();
                                            if (isStop == false) { // receivedSize == size, 此处判断是否下载完返回的数据
                                                receivedAllData = true;
                                            } else {
                                                receivedAllData = false;
                                            }
                                        } else {
                                            realTimeRecive.reciveData(baos.toByteArray());
                                        }
                                    }
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
                progressUpdate = null;
            } catch (IOException e) {
                fail = e.toString();
                if (TextUtils.isEmpty(fail)) {
                    fail="fail";
                }
                if (keepAlive) {
                    close();
                    client = createHttpClient(context);
                }
                throw e;
            } finally {
                if (!TextUtils.isEmpty(apiType)) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    String networkInfoDetail = "none";
                    if (networkInfo != null) {
                        networkInfoDetail = networkInfo.getDetailedState().toString();
                    }
                    String uuid = this.parameters.getValue(BaseQuery.SERVER_PARAMETER_UUID);
                    ActionLog.getInstance(context).addNetworkAction(apiType, reqTime, revTime, resTime, fail, networkInfoDetail, TKConfig.getSignalStrength(), TKConfig.getConnectivityType(context), isStop, uuid, reqSize, revSize, this.isReTry);
                }
                if (!keepAlive) {
                    close();
                }
            }
        }
    }
    
    /**
     * 根据是否设置代理的情况，执行Http请求并重试一次
     * @param context
     * @param client
     * @param request
     * @param url
     * @param apiType
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public static HttpResponse execute(Context context, HttpClient client, HttpUriRequest request, String url, String apiType) throws ClientProtocolException, IOException {
        HttpResponse response;
        String proxyHost = android.net.Proxy.getDefaultHost();
        int proxyPort = android.net.Proxy.getDefaultPort();
        boolean isMobile = Utility.checkMobileNetwork(context);
        if (isMobile && proxyHost != null && ProxyIsAvailable) {
            LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", proxyHost="+proxyHost);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
            String domain = url.replace("http://", "");
            domain = domain.substring(0, domain.indexOf("/"));
            HttpHost target = new HttpHost(domain, 80, "http");
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            try {
                response = client.execute(target, request);
                ProxyIsAvailable = true;
            } catch (Exception e) {
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, null);
                LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", proxyHost="+null);
                response = client.execute(request);
                ProxyIsAvailable = false;
            }
        } else {
            try {
                LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", proxyHost="+null);
                response = client.execute(request);
                ProxyIsAvailable = false;
            } catch (IOException e) {
                if (isMobile && proxyHost != null) {
                    LogWrapper.i("HttpUtils", "TKHttpClient->sendAndRecive():apiType="+apiType+", proxyHost="+proxyHost);
                    HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
                    String domain = url.replace("http://", "");
                    domain = domain.substring(0, domain.indexOf("/"));
                    HttpHost target = new HttpHost(domain, 80, "http");
                    client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                    
                    response = client.execute(target, request);
                    ProxyIsAvailable = true;
                } else {
                    throw e;
                }
            }
        }
        
        return response;
    }

    /**
     * 将查询参数进行URLEnecode编码处理后组合成字符串
     * @param httpParams
     * @param enc
     * @return
     */
//    public static String encodeParameters(RequestParameters httpParams, String enc) {
//        if (null == httpParams || httpParams.isEmpty()) {
//            return "";
//        }
//        StringBuilder buf = new StringBuilder();
//        int j = 0;
//        for (int loc = 0; loc < httpParams.size(); loc++) {
//            String key = httpParams.getKey(loc);
//            if (j != 0) {
//                buf.append(PARAMETER_SEPARATOR);
//            }
//            try {
//                buf.append(URLEncoder.encode(key, enc)).append(NAME_VALUE_SEPARATOR)
//                        .append(URLEncoder.encode(httpParams.getValue(loc), enc));
//            } catch (java.io.UnsupportedEncodingException neverHappen) {
//            }
//            j++;
//        }
//        return buf.toString();
//
//    }

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

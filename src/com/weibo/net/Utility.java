/*
 * Copyright 2011 Sina.
 *
 * Licensed under the Apache License and Weibo License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.open.weibo.com
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weibo.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;

import com.decarta.android.util.LogWrapper;

/**
 * Utility class for Weibo object.
 * 
 * @author ZhangJie (zhangjie2@staff.sina.com.cn)
 */

public class Utility {

    private static WeiboParameters mRequestHeader = new WeiboParameters();
    private static HttpHeaderFactory mAuth;
    private static Token mToken = null;

    public static final String BOUNDARY = "7cd4a6d158c";
    public static final String MP_BOUNDARY = "--" + BOUNDARY;
    public static final String END_MP_BOUNDARY = "--" + BOUNDARY + "--";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String endLine = "\r\n";
    
    public static final String HTTPMETHOD_POST = "POST";
    public static final String HTTPMETHOD_GET = "GET";
    public static final String HTTPMETHOD_DELETE = "DELETE";

    private static final int SET_CONNECTION_TIMEOUT = 50000;
    private static final int SET_SOCKET_TIMEOUT = 200000;

    // 设置Token
    public static void setTokenObject(Token token) {
        mToken = token;
    }

    public static void setAuthorization(HttpHeaderFactory auth) {
        mAuth = auth;
    }

    // 设置http头,如果authParam不为空，则表示当前有token认证信息需要加入到头中
    public static void setHeader(String httpMethod, HttpURLConnection connection,
            WeiboParameters authParam, String url, Token token) throws WeiboException {
        if (!isBundleEmpty(mRequestHeader)) {
            for (int loc = 0; loc < mRequestHeader.size(); loc++) {
                String key = mRequestHeader.getKey(loc);
                connection.setRequestProperty(key, mRequestHeader.getValue(key));
            }
        }
        if (!isBundleEmpty(authParam) && mAuth != null) {
            String authHeader = mAuth.getWeiboAuthHeader(httpMethod, url, authParam,
                    Weibo.getAppKey(), Weibo.getAppSecret(), token);
            if (authHeader != null) {
                connection.setRequestProperty("Authorization", authHeader);
            }
        }
        connection.setRequestProperty("User-Agent", System.getProperties().getProperty("http.agent")
                + " WeiboAndroidSDK");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Charset", "utf-8");
        connection.setConnectTimeout(SET_CONNECTION_TIMEOUT);
        connection.setReadTimeout(SET_SOCKET_TIMEOUT);
    }

    public static boolean isBundleEmpty(WeiboParameters bundle) {
        if (bundle == null || bundle.size() == 0) {
            return true;
        }
        return false;
    }

    // 填充request bundle
    public static void setRequestHeader(String key, String value) {
        // mRequestHeader.clear();
        mRequestHeader.add(key, value);
    }

    public static void setRequestHeader(WeiboParameters params) {
        mRequestHeader.addAll(params);
    }

    public static void clearRequestHeader() {
        mRequestHeader.clear();

    }

    public static String encodePostBody(Bundle parameters, String boundary) {
        if (parameters == null)
            return "";
        StringBuilder sb = new StringBuilder();

        for (String key : parameters.keySet()) {
            if (parameters.getByteArray(key) != null) {
                continue;
            }

            sb.append("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n"
                    + parameters.getString(key));
            sb.append("\r\n" + "--" + boundary + "\r\n");
        }

        return sb.toString();
    }

    public static String encodeUrl(WeiboParameters parameters) {
        if (parameters == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int loc = 0; loc < parameters.size(); loc++) {
            if (first)
                first = false;
            else
                sb.append("&");
            if (!TextUtils.isEmpty(parameters.getKey(loc)) && !TextUtils.isEmpty(parameters.getValue(loc))) {
            	sb.append(URLEncoder.encode(parameters.getKey(loc)) + "="
                    + URLEncoder.encode(parameters.getValue(loc)));
            }
        }
        return sb.toString();
    }

    public static Bundle decodeUrl(String s) {
        Bundle params = new Bundle();
        if (s != null) {
            String array[] = s.split("&");
            for (String parameter : array) {
                String v[] = parameter.split("=");
                params.putString(URLDecoder.decode(v[0]), URLDecoder.decode(v[1]));
            }
        }
        return params;
    }

    /**
     * Parse a URL query and fragment parameters into a key-value bundle.
     * 
     * @param url
     *            the URL to parse
     * @return a dictionary bundle of keys and values
     */
    public static Bundle parseUrl(String url) {
        // hack to prevent MalformedURLException
        url = url.replace("weiboconnect", "http");
        try {
            URL u = new URL(url);
            Bundle b = decodeUrl(u.getQuery());
            b.putAll(decodeUrl(u.getRef()));
            return b;
        } catch (MalformedURLException e) {
            return new Bundle();
        }
    }

    /**
     * Construct a url encoded entity by parameters .
     * 
     * @param bundle
     *            :parameters key pairs
     * @return UrlEncodedFormEntity: encoed entity
     */
    public static UrlEncodedFormEntity getPostParamters(Bundle bundle) throws WeiboException {
        if (bundle == null || bundle.isEmpty()) {
            return null;
        }
        try {
            List<NameValuePair> form = new ArrayList<NameValuePair>();
            for (String key : bundle.keySet()) {
                form.add(new BasicNameValuePair(key, bundle.getString(key)));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, "UTF-8");
            return entity;
        } catch (UnsupportedEncodingException e) {
            throw new WeiboException(e);
        }
    }

    /**
     * Implement a weibo http request and return results .
     * 
     * @param context
     *            : context of activity
     * @param url
     *            : request url of open api
     * @param method
     *            : HTTP METHOD.GET, POST, DELETE
     * @param params
     *            : Http params , query or postparameters
     * @param Token
     *            : oauth token or accesstoken
     * @return UrlEncodedFormEntity: encoed entity
     */

    public static String openUrl(Context context, String url, String method,
            WeiboParameters params, Token token) throws WeiboException, IOException {
        String rlt = "";
        String file = "";
        for (int loc = 0; loc < params.size(); loc++) {
            String key = params.getKey(loc);
            if (key.equals("pic")) {
                file = params.getValue(key);
                params.remove(key);
            }
        }
        if (TextUtils.isEmpty(file)) {
            rlt = openUrl(context, url, method, params, null, token);
        } else {
            rlt = openUrl(context, url, method, params, file, token);
        }
        return rlt;
    }

    public static String openUrl(Context context, String url, String method,
            WeiboParameters params, String file, Token token) throws WeiboException, IOException {
    	try {
	    	if (method.equals("GET")) {
	            url = url + "?" + encodeUrl(params);
	            LogWrapper.d("eric", "Utility.test: " + url);
	    	}
	    	HttpURLConnection conn;
	    	if (url.startsWith("https")) {
	            conn = (HttpsURLConnection)(new URL(url)).openConnection();
	            try{
	                TrustManager easyTrustManager = new X509TrustManager() {
	
	                    public void checkClientTrusted(X509Certificate ax509certificate[], String s)
	                        throws CertificateException {
	                    }
	
	                    public void checkServerTrusted(X509Certificate ax509certificate[], String s)
	                        throws CertificateException {
	                    }
	
	                    public X509Certificate[] getAcceptedIssuers() {
	                        return null;
	                    }
	
	                };
	                SSLContext sslcontext = SSLContext.getInstance("TLS");
	                sslcontext.init(null, new TrustManager[] {
	                    easyTrustManager
	                }, null);
	                ((HttpsURLConnection)conn).setSSLSocketFactory(sslcontext.getSocketFactory());
	            } catch(Exception exception) { }
	            
	        } else {
	            conn = (HttpURLConnection)(new URL(url)).openConnection();
	        }
	
	    	setHeader(method, conn, params, url, token);
	    	
	        if (method.equals("GET")) {
	        	conn.setRequestMethod("GET");
	        } else if (method.equals("POST")){
	        	conn.setRequestMethod("POST");
	
	            if (!TextUtils.isEmpty(file)) {
	            	conn.setRequestProperty("Content-Type", (new StringBuilder("multipart/form-data;boundary=")).append(BOUNDARY).toString());
	            } else {
	            	conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	            }
	            conn.setDoOutput(true);
	            conn.setDoInput(true);
	            conn.connect();
	            OutputStream os = new BufferedOutputStream(conn.getOutputStream());
	            if (!TextUtils.isEmpty(file)) {
		            paramToUpload(os, params);
		            Bitmap bf = BitmapFactory.decodeFile(file);
	                imageContentToUpload(os, bf);
	            } else {
	                String postParam = encodeParameters(params);
	                byte[] data = postParam.getBytes("UTF-8");
	                os.write(data);
	            }
	            os.flush();
	        }
	        String response = null;
	        try {
	            response = read(conn.getInputStream(), conn.getContentEncoding());
	        } catch (FileNotFoundException e) {
	        	e.printStackTrace();
	        	response = read(conn.getErrorStream(), conn.getContentEncoding());
	            LogWrapper.d("eric", "FileNotFoundException response: " + response);
	        	generateErrorObject(response, conn.getResponseCode());
	        	conn.disconnect();
	        }
	        LogWrapper.d("eric", "response: " + response);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                LogWrapper.d("eric", "ErrorResult: " + response);
                generateErrorObject(response, conn.getResponseCode());
            }

        	conn.disconnect();
            return response;
    	} catch (IOException e) {
    		e.printStackTrace();
            throw e;
    	}
    }
    
    private static void generateErrorObject(String response, int statusCode) throws WeiboException, IOException {
    	if (!TextUtils.isEmpty(response)) {
        	WeiboException error = new WeiboException();
        	try {
				JSONObject json = new JSONObject(response);
				error.setError(json.getString("error"));
				error.setErrorCode(json.getInt("error_code"));
				error.seRequest(json.getString("request"));
				error.setStatusCode(statusCode);
			} catch (JSONException je) {
				je.printStackTrace();
				throw new IOException(je.getMessage());
			}
        	LogWrapper.d("eric", "WeiboException: " + error);
            throw error;
    	}
    }


    /**
     * Upload image into output stream .
     * 
     * @param out
     *            : output stream for uploading weibo
     * @param imgpath
     *            : bitmap for uploading
     * @return void
     */
    private static void imageContentToUpload(OutputStream out, Bitmap imgpath)
            throws WeiboException {
        StringBuilder temp = new StringBuilder();

        temp.append(MP_BOUNDARY).append("\r\n");
        temp.append("Content-Disposition: form-data; name=\"pic\"; filename=\"")
                .append("news_image").append("\"\r\n");
        String filetype = "image/png";
        temp.append("Content-Type: ").append(filetype).append("\r\n\r\n");
        byte[] res = temp.toString().getBytes();
        BufferedInputStream bis = null;
        try {
            out.write(res);
            imgpath.compress(CompressFormat.PNG, 75, out);
            out.write("\r\n".getBytes());
            out.write(("\r\n" + END_MP_BOUNDARY).getBytes());
        } catch (IOException e) {
            throw new WeiboException(e);
        } finally {
            if (null != bis) {
                try {
                    bis.close();
                } catch (IOException e) {
                    throw new WeiboException(e);
                }
            }
        }
    }

    /**
     * Upload weibo contents into output stream .
     * 
     * @param baos
     *            : output stream for uploading weibo
     * @param params
     *            : post parameters for uploading
     * @return void
     */
    private static void paramToUpload(OutputStream baos, WeiboParameters params)
            throws WeiboException {
        String key = "";
        for (int loc = 0; loc < params.size(); loc++) {
            key = params.getKey(loc);
            StringBuilder temp = new StringBuilder(10);
            temp.setLength(0);
            temp.append(MP_BOUNDARY).append("\r\n");
            temp.append("content-disposition: form-data; name=\"").append(key).append("\"\r\n\r\n");
            temp.append(params.getValue(key)).append("\r\n");
            byte[] res = temp.toString().getBytes();
            try {
                baos.write(res);
            } catch (IOException e) {
                throw new WeiboException(e);
            }
        }
    }

    /**
     * POST encode parameters
     * @param httpParams
     * @return
     */
    public static String encodeParameters(WeiboParameters httpParams) {
        if (null == httpParams || Utility.isBundleEmpty(httpParams)) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        int j = 0;
        for (int loc = 0; loc < httpParams.size(); loc++) {
            String key = httpParams.getKey(loc);
            if (j != 0) {
                buf.append("&");
            }
            try {
                buf.append(URLEncoder.encode(key, "UTF-8")).append("=")
                        .append(URLEncoder.encode(httpParams.getValue(key), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException neverHappen) {
            }
            j++;
        }
        return buf.toString();

    }
    

    /**
     * Read http requests result from inputstream .
     * 
     * @param inputstream
     *            : http inputstream from HttpConnection
     * 
     * @return String : http response content
     */
    private static String read(InputStream inputStream, String contentEncoding) throws WeiboException {
    	
    	String result = "";
        try {
            ByteArrayOutputStream content = new ByteArrayOutputStream();

            if (contentEncoding != null && contentEncoding.toLowerCase().indexOf("gzip") > -1) {
                inputStream = new GZIPInputStream(inputStream);
            }

            // Read response into a buffered stream
            int readBytes = 0;
            byte[] sBuffer = new byte[512];
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            // Return result from buffered stream
            result = new String(content.toByteArray());
            return result;
        } catch (IllegalStateException e) {
            throw new WeiboException(e);
        } catch (IOException e) {
            throw new WeiboException(e);
        }
    }

}

package com.tigerknows.android.net;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.tigerknows.TKConfig;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.FileUpload;
import com.tigerknows.util.HttpUtils;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.WeiboParameters;
import com.weibo.sdk.android.net.NetStateManager;
import com.weibo.sdk.android.util.Utility;

/**
 * 
 * @author luopeng (luopeng@staff.sina.com.cn)
 */
public class HttpManager {

//	private static final String BOUNDARY = "7cd4a6d158c";
    private static final String BOUNDARY=getBoundry();
	private static final String MP_BOUNDARY = "--" + BOUNDARY;
	private static final String END_MP_BOUNDARY = "--" + BOUNDARY + "--";
	private static final String MULTIPART_FORM_DATA = "multipart/form-data";

	public static final String HTTPMETHOD_POST = "POST";
	public static final String HTTPMETHOD_GET = "GET";

	private static final int SET_CONNECTION_TIMEOUT = 60 * 1000;
	private static final int SET_SOCKET_TIMEOUT = 20 * 1000;
	/**
	 * 
	 * @param url 服务器地址
	 * @param method "GET"or “POST”
	 * @param params   存放参数的容器
	 * @param file 文件路径，如果 是发送带有照片的微博的话，此参数为图片在sdcard里的绝对路径
	 * @return 响应结果
	 * @throws WeiboException
	 */
    public static byte[] openUrl(Context context, HttpClient client, String url, String method,
            WeiboParameters params) throws WeiboException {
        return openUrl(context, client, url, method, params, null, null);
    }
    
    public static byte[] openUrl(Context context, HttpClient client, String url, String method,
            WeiboParameters params, String apiType, String uuid) throws WeiboException {
        byte[] rlt = null;
        String file = "";
        for (int loc = 0; loc < params.size(); loc++) {
            String key = params.getKey(loc);
            if (key.equals(FileUpload.SERVER_PARAMETER_UPFILE)) {
                file = params.getValue(key);
                params.remove(key);
            }
        }
        if (TextUtils.isEmpty(file)) {
            rlt = openUrl(context, client, url, method, params, null, apiType, uuid);
        } else {
            rlt = openUrl(context, client, url, method, params, file, apiType, uuid);
        }
        return rlt;
    }
    
	public static byte[] openUrl(Context context, HttpClient client, String url, String method, WeiboParameters params, String file, String apiType, String uuid) throws WeiboException {
	    byte[] result = null;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        String networkInfoDetail = "none";
        if (networkInfo != null) {
            networkInfoDetail = networkInfo.getDetailedState().toString();
        }
        
        long reqTime = 0;
        long revTime = 0;
        long resTime = 0;
        String fail = "";
        long reqSize = 0;
        long revSize = 0;
		try {
			HttpUriRequest request = null;
			ByteArrayOutputStream bos = null;
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, NetStateManager.getAPN());
			if (method.equals(HTTPMETHOD_GET)) {
				url = url + "?" + Utility.encodeUrl(params);
				HttpGet get = new HttpGet(url);
				request = get;
			} else if (method.equals(HTTPMETHOD_POST)) {
				HttpPost post = new HttpPost(url);
				request = post;
				byte[] data = null;
				String _contentType=params.getValue("content-type");
				
				bos = new ByteArrayOutputStream();
				if (!TextUtils.isEmpty(file)) {
                    post.addHeader(HttpUtils.CONTENT_ENCODING, "UTF-8");
				    post.addHeader(HttpUtils.TK_SERVICE_TYPE, HttpUtils.TK_SERVICE_TYPE_VALUE);
					paramToUpload(bos, params);
					post.setHeader("Content-Type", MULTIPART_FORM_DATA + "; boundary=" + BOUNDARY);
//					Utility.UploadImageUtils.revitionPostImageSize(  file);
					imageContentToUpload(bos, file, params.getValue(FileUpload.SERVER_PARAMETER_FILENAME));
				} else {
				    if(_contentType!=null){
				        params.remove("content-type");
				        post.setHeader("Content-Type", _contentType);
				    }
				    else{
				        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
				    }
					
					String postParam = Utility.encodeParameters(params);
					data = postParam.getBytes("UTF-8");
					bos.write(data);
				}
				data = bos.toByteArray();
				reqSize = data.length;
				bos.close();
				ByteArrayEntity formEntity = new ByteArrayEntity(data);
				post.setEntity(formEntity);
			} else if (method.equals("DELETE")) {
				request = new HttpDelete(url);
			}
			reqTime = System.currentTimeMillis();
			HttpResponse response = client.execute(request);
			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();
	        revTime = System.currentTimeMillis();
			
			if (statusCode != 200) {
				result = readHttpResponse(response);
				revSize = result.length;
				throw new WeiboException(new String(result), statusCode);
			}
			result = readHttpResponse(response);
			resTime = System.currentTimeMillis();

			return result;
		} catch (IOException e) {
		    fail = e.toString();
            if (TextUtils.isEmpty(fail)) {
                fail="fail";
            }
			throw new WeiboException(e);
		} finally {
		    if (apiType != null && uuid != null) {
		        ActionLog.getInstance(context).addNetworkAction(apiType, reqTime, revTime, resTime, fail, networkInfoDetail, TKConfig.getSignalStrength(), TKConfig.getConnectivityType(context), false, uuid, reqSize, revSize);
		    }
		}
	}
	
	public static HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();

			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			HttpConnectionParams.setConnectionTimeout(params, SET_CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, SET_SOCKET_TIMEOUT);
			HttpClient client = new DefaultHttpClient(ccm, params);
//			if (NetState.Mobile == NetStateManager.CUR_NETSTATE) {
//				// 获取当前正在使用的APN接入点
//				HttpHost proxy = NetStateManager.getAPN();
//				if (null != proxy) {
//					client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy);
//				}
//			}
			return client;
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

	private static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
				KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
				throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	private static void paramToUpload(OutputStream baos, WeiboParameters params)
			throws WeiboException {
		String key = "";
		for (int loc = 0; loc < params.size(); loc++) {
			key = params.getKey(loc);
			if (key.equals(FileUpload.SERVER_PARAMETER_FILENAME) ||
			        key.equals(FileUpload.SERVER_PARAMETER_UPFILE)) {
			    continue;
			}
			StringBuilder temp = new StringBuilder(10);
			temp.setLength(0);
			temp.append(MP_BOUNDARY).append("\r\n");
			temp.append("content-disposition: form-data; name=\"").append(key).append("\"\r\n\r\n");
			temp.append(params.getValue(key)).append("\r\n");
			try {
			    byte[] res = temp.toString().getBytes("UTF-8");
				baos.write(res);
			} catch (IOException e) {
				throw new WeiboException(e);
			}
		}
	}

	private static void imageContentToUpload(OutputStream out, String imgpath, String fileName) throws WeiboException {
		if(imgpath==null){
		    return;
		}
	    StringBuilder temp = new StringBuilder();
		
		temp.append(MP_BOUNDARY).append("\r\n");
		temp.append("Content-Disposition: form-data; name=\""+FileUpload.SERVER_PARAMETER_UPFILE+"\"; filename=\"")
				.append(fileName).append("\"\r\n");
		String filetype = "image/png";
		temp.append("Content-Type: ").append(filetype).append("\r\n\r\n");
		FileInputStream input = null;
		try {
		    byte[] res = temp.toString().getBytes("UTF-8");
			out.write(res);
			 input = new FileInputStream(imgpath);
			byte[] buffer=new byte[1024*50];
			while(true){
				int count=input.read(buffer);
				if(count==-1){
					break;
				}
				out.write(buffer, 0, count);
			}
//			out.write("\r\n".getBytes("UTF-8"));
			out.write(("\r\n" + END_MP_BOUNDARY).getBytes("UTF-8"));
		} catch (IOException e) {
			throw new WeiboException(e);
		} finally {
			if (null != input) {
				try {
					input.close();
				} catch (IOException e) {
					throw new WeiboException(e);
				}
			}
		}
	}

	/**
	 * 读取HttpResponse数据
	 * 
	 * @param response
	 * @return
	 */
	private static byte[] readHttpResponse(HttpResponse response) {
	    byte[] result = null;
		HttpEntity entity = response.getEntity();
		InputStream inputStream;
		try {
			inputStream = entity.getContent();
			ByteArrayOutputStream content = new ByteArrayOutputStream();

			Header header = response.getFirstHeader("Content-Encoding");
			if (header != null && header.getValue().toLowerCase().indexOf("gzip") > -1) {
				inputStream = new GZIPInputStream(inputStream);
			}

			int readBytes = 0;
			byte[] sBuffer = new byte[512];
			while ((readBytes = inputStream.read(sBuffer)) != -1) {
				content.write(sBuffer, 0, readBytes);
			}
			result = content.toByteArray();
			return result;
		} catch (IllegalStateException e) {
		} catch (IOException e) {
		}
		return result;
	}
	   /**
     * 产生11位的boundary
     */
    static String getBoundry() {
        StringBuffer _sb = new StringBuffer();
        for (int t = 1; t < 12; t++) {
            long time = System.currentTimeMillis() + t;
            if (time % 3 == 0) {
                _sb.append((char) time % 9);
            } else if (time % 3 == 1) {
                _sb.append((char) (65 + time % 26));
            } else {
                _sb.append((char) (97 + time % 26));
            }
        }
        return _sb.toString();
    }

}


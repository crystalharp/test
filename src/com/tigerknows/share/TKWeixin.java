package com.tigerknows.share;

import net.sourceforge.simcpux.Constants;

import com.tencent.mm.sdk.openapi.GetMessageFromWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tigerknows.R;
import com.tigerknows.maps.MapEngine;
import com.tigerknows.model.POI;
import com.tigerknows.util.CommonUtils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 封装对微信的操作
 * @author pengwenyue
 *
 */
public class TKWeixin {
	
	static final String TAG = "TKWeixin";
	
	static final String POI_URL = "http://propagate.tigerknows.net/tk_spread?v=1&c=%s&uid=%s&from=message";
    
	/**
	 * 支持朋友圈的最小版本值
	 */
    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
    
    private static final String URL = "http://weixin.qq.com/m";
    
    private static TKWeixin instance = null;
    
    public static TKWeixin getInstance(Activity activity) {
        if (instance == null) {
            TKWeixin tkWeixin = new TKWeixin();
            
            tkWeixin.activity = activity;
            // 通过WXAPIFactory工厂，获取IWXAPI的实例
            tkWeixin.api = WXAPIFactory.createWXAPI(activity, Constants.APP_ID, false);
            tkWeixin.api.registerApp(Constants.APP_ID);
            
            instance = tkWeixin;
        }
        return instance;
    }

    private Activity activity;
    
    // IWXAPI 是第三方app和微信通信的openapi接口
    private IWXAPI api;
    
	public boolean isWXAppInstalled() {
        return api.isWXAppInstalled();
    }
    
    public boolean isWXAppSupportAPI() {
        return api.getWXAppSupportAPI() >= TIMELINE_SUPPORTED_VERSION;
    }
    
    private TKWeixin() {
        
    }
    
    /**
     * 检查是否安装微信，若没有安装则提示下载微信软件对话框
     * @return
     */
    public boolean isSupportSend() {

        boolean result = isWXAppInstalled();
        if (result == false) {
            CommonUtils.showNormalDialog(activity, activity.getString(R.string.uninstall_weixin_tip), new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                    	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                    	activity.startActivity(intent);
                    }
                }
            });
        }
        return result;
    }
    
    /**
     * 检查是否支持发送到朋友圈，若不支持则提示更新微信软件对话框
     * @return
     */
    public boolean isSupportTimeline() {

        boolean result = isSupportSend();
        if (result) {
        	result = isWXAppSupportAPI();
        	if (result == false) {
        		CommonUtils.showNormalDialog(activity, activity.getString(R.string.upgarde_weixin_tip), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DialogInterface.BUTTON_POSITIVE == which) {
                        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                        	activity.startActivity(intent);
                        }
                    }
                });
        	}
        }
        
        return result;
    }
    
    /**
     * 调用api接口响应数据到微信
     * @param req
     * @param isTimeline
     */
    public void sendReq(SendMessageToWX.Req req, boolean isTimeline) {
    	if (api.isWXAppInstalled() == false) {
    		return;
    	}
    	
    	int scene = SendMessageToWX.Req.WXSceneSession;
    	if (isWXAppSupportAPI() && isTimeline) { 
            scene = SendMessageToWX.Req.WXSceneTimeline;
    	}
    	req.scene = scene;
        api.sendReq(req);
    }
    
    /**
     * 调用api接口发送数据到微信
     * @param resp
     */
    public void sendResp(GetMessageFromWX.Resp resp) {
    	if (api.isWXAppInstalled() == false) {
    		return;
    	}
        api.sendResp(resp);
    }
    

    public static SendMessageToWX.Req makePOIReq(Activity activity, POI poi) {        
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = makeWXWebpageObject(activity, poi);
        return req;
    }
    
    private static WXMediaMessage makeWXWebpageObject(Activity activity, POI poi) {
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = String.format(POI_URL, MapEngine.getInstance().getCityId(poi.getPosition()), poi.getUUID());
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = poi.getName();
        msg.description = poi.getAddress();
        InputStream is;
        try {
            is = activity.getAssets().open("icon.png");
            ByteArrayOutputStream fout = new ByteArrayOutputStream();
            byte buf[]=new byte[1024];
            int len;
            while((len=is.read(buf))>0)
                fout.write(buf,0,len);
            fout.flush();
            fout.close();
            is.close();
            msg.thumbData = fout.toByteArray();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return msg;
    }

    private static String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
    
    public static GetMessageFromWX.Resp makePOIResp(Activity activity, POI poi, Bundle bundle) {
        // 构造一个Resp
        GetMessageFromWX.Resp resp = new GetMessageFromWX.Resp();
        // 将req的transaction设置到resp对象中，其中bundle为微信传递过来的intent所带的内容，通过getExtras方法获取
        resp.transaction = getTransaction(bundle);
        resp.message = makeWXWebpageObject(activity, poi);
        return resp;
    }

    private static String getTransaction(Bundle bundle) {
        final GetMessageFromWX.Req req = new GetMessageFromWX.Req(bundle);
        return req.transaction;
    }
	public void onDestory() {
		instance = null;
		activity = null;
	}
}

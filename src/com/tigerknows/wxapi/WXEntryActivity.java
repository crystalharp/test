package com.tigerknows.wxapi;


import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.ShowMessageFromWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXAppExtendObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tigerknows.R;
import com.tigerknows.Sphinx;

import net.sourceforge.simcpux.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
	
    private IWXAPI api;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 通过WXAPIFactory工厂，获取IWXAPI的实例
    	api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, false);
        
        api.handleIntent(getIntent(), this);
    }

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		setIntent(intent);
        api.handleIntent(intent, this);
	}

    @Override
    public void onReq(BaseReq req) {
        switch (req.getType()) {
        case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
        	Intent intent = getIntent();
        	if (intent != null) {
	            Intent sphinx = new Intent(this, Sphinx.class);
	            sphinx.putExtra(Sphinx.EXTRA_WEIXIN, true);
	            sphinx.putExtras(intent.getExtras());
	            startActivity(sphinx);
	            finish();
        	}
            break;
        case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
            goToShowMsg((ShowMessageFromWX.Req) req);
            break;
        default:
            break;
        }
    }

    @Override
    public void onResp(BaseResp resp) {
        int result = 0;
        
        switch (resp.errCode) {
        case BaseResp.ErrCode.ERR_OK:
            result = R.string.errcode_success;
            break;
        case BaseResp.ErrCode.ERR_USER_CANCEL:
            result = R.string.errcode_cancel;
            break;
        case BaseResp.ErrCode.ERR_AUTH_DENIED:
            result = R.string.errcode_deny;
            break;
        default:
            result = R.string.errcode_unknown;
            break;
        }
        
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        finish();
    }
    
    private void goToShowMsg(ShowMessageFromWX.Req showReq) {
        WXMediaMessage wxMsg = showReq.message;        
        WXAppExtendObject obj = (WXAppExtendObject) wxMsg.mediaObject;
        
        StringBuffer msg = new StringBuffer(); // 组织一个待显示的消息内容
        msg.append("description: ");
        msg.append(wxMsg.description);
        msg.append("\n");
        msg.append("extInfo: ");
        msg.append(obj.extInfo);
        msg.append("\n");
        msg.append("filePath: ");
        msg.append(obj.filePath);
        
        final String title = wxMsg.title;
        final String message = msg.toString();
        final byte[] thumbData = wxMsg.thumbData;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        
        if (thumbData != null && thumbData.length > 0) {
            ImageView thumbIv = new ImageView(this);
            thumbIv.setImageBitmap(BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length));
            builder.setView(thumbIv);
        }
        
        builder.show();
    }
}
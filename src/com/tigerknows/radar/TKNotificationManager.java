package com.tigerknows.radar;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.PullMessage.Message;
import com.tigerknows.model.PullMessage.Message.PulledDynamicPOI;
import com.tigerknows.model.PullMessage.Message.PulledProductMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class TKNotificationManager {

	private static boolean checkProdcutUpgrade(Message msg){
		PulledProductMessage productMsg;
		if((productMsg = msg.getProductMsg())!=null 
				&& productMsg.getDownloadUrl()!=null ){
			return true;
		}else{
			return false;
		}
	}
	
	private static boolean checkProdcutInfo(Message msg){
		PulledProductMessage productMsg = msg.getProductMsg();
		if(productMsg!=null && productMsg.getDescription()!=null ){
			return true;
		}else{
			return false;
		}
	}

    private static boolean checkZhanlanYanchu(Message msg) {
    	PulledDynamicPOI dynamicPOI = msg.getDynamicPOI();
    	if(dynamicPOI!=null){
    		return true;
    	}
    	
		return false;
	}

    public static PendingIntent makeIntent(Context context, Message msg) {
    	
    	Intent intent = null;
    	
    	switch ((int)msg.getType()) 
    	{
			case Message.TYPE_PRODUCT_UPGRADE:
				if(checkProdcutUpgrade(msg)){
		    			intent = new Intent(Intent.ACTION_VIEW);
		    			intent.setData(Uri.parse(msg.getProductMsg().getDownloadUrl()));
				}
				break;
	
			case Message.TYPE_PRODUCT_INFOMATION:
				if(checkProdcutInfo(msg)){
					intent = new Intent(context, Sphinx.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Sphinx.EXTRA_PULL_MESSAGE, msg);
				}
				break;
			case Message.TYPE_HOLIDAY:
				if(checkZhanlanYanchu(msg)){
					intent = new Intent(context, Sphinx.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Sphinx.EXTRA_PULL_MESSAGE, msg);
				}
				
				break;
				
			default:
				return null;
		}
    	
        // The PendingIntent to launch our activity if the user selects this
        // notification.  Note the use of FLAG_UPDATE_CURRENT so that if there
        // is already an active matching pending intent, we will update its
        // extras to be the ones passed in here.
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        
        return contentIntent;
        
    }

	public static void notify(Context context, Message msg) {
        if (msg == null) {
            return;
        }
        PendingIntent pendingIntent = makeIntent(context, msg);
        if(pendingIntent == null){
        	return;
        }
        
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification notif = new Notification();

        notif.contentIntent = pendingIntent;

        CharSequence text = context.getString(R.string.app_name);
        notif.tickerText = text;

        // the icon for the status bar
        notif.icon = R.drawable.icon;

        // our custom view
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
        contentView.setTextViewText(R.id.text_txv, msg.toString());
        contentView.setImageViewResource(R.id.icon_imv, R.drawable.icon);
        
        notif.contentView = contentView;

        // we use a string id because is a unique number.  we use it later to cancel the
        // notification
        nm.notify(R.layout.sphinx, notif);
    }

    public static void cancel(Context context) {
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.layout.sphinx);
    }
}

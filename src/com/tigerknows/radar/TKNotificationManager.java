package com.tigerknows.radar;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
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

	/**
	 * Check the integrity of the product upgrade info
	 * @param msg
	 * @return true if the pulledProductMessage and downloadUrl not null  
	 */
	private static boolean checkProdcutUpgrade(Message msg){
		PulledProductMessage productMsg;
		if((productMsg = msg.getProductMsg())!=null 
				&& productMsg.getDownloadUrl()!=null ){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Check the producInfo fileds
	 * @param msg
	 * @return true if the all product info needed fields needed are all there
	 */
	private static boolean checkProdcutInfo(Message msg){
		PulledProductMessage productMsg = msg.getProductMsg();
		if(productMsg!=null && productMsg.getDescription()!=null ){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * Check the zhnalan|yanchu fileds
	 * @param msg
	 * @return true if the all needed fields needed are all there
	 */
    private static boolean checkZhanlanYanchu(Message msg) {
    	PulledDynamicPOI dynamicPOI = msg.getDynamicPOI();
    	if(dynamicPOI!=null
    			&& (BaseQuery.DATA_TYPE_ZHANLAN.equals(""+dynamicPOI.getMasterType()) 
    					|| BaseQuery.DATA_TYPE_YANCHU.equals(""+dynamicPOI.getMasterType()) ) 
    			&& dynamicPOI.getMasterUID() != null){
    		return true;
    	}
		return false;
	}
    
	/**
	 * Check the film fileds
	 * @param msg
	 * @return true if the all needed fields needed are all there
	 */
    private static boolean checkFilminfo(Message msg) {
    	PulledDynamicPOI dynamicPOI = msg.getDynamicPOI();
    	if(dynamicPOI!=null
    			&& BaseQuery.DATA_TYPE_DIANYING.equals(""+dynamicPOI.getMasterType())
    			&& dynamicPOI.getMasterUID() != null
                && BaseQuery.DATA_TYPE_YINGXUN.equals(""+dynamicPOI.getSlaveType())
    			&& dynamicPOI.getSlaveUID() != null){
    		return true;
    	}
		return false;
	}

	/**
	 * Check the inerval msg fileds
	 * @param msg
	 * @return true if the all needed fields needed are all there
	 */
    private static boolean checkInterval(Message msg) {
    	PulledDynamicPOI dynamicPOI = msg.getDynamicPOI();
    	if(dynamicPOI!=null
    			&& ( (BaseQuery.DATA_TYPE_DIANYING.equals(""+dynamicPOI.getMasterType()) && checkFilminfo(msg))
    					|| checkZhanlanYanchu(msg) )
    			){
    		return true;
    	}
		return false;
	}

    /**
     * Check the msg integrity and make intent for the notification
     * @param context
     * @param msg
     * @return
     */
    public static PendingIntent checkAndMakeIntent(Context context, Message msg) {
    	
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
			case Message.TYPE_FILM:
				if(checkFilminfo(msg)){
					intent = new Intent(context, Sphinx.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Sphinx.EXTRA_PULL_MESSAGE, msg);
				}
				break;
				
			case Message.TYPE_INTERVAL:
				if((checkInterval(msg))){
					intent = new Intent(context, Sphinx.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Sphinx.EXTRA_PULL_MESSAGE, msg);
				}
				break;

			default:
			    intent = null;
		}
    	
    	if (intent == null) {
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
        PendingIntent pendingIntent = checkAndMakeIntent(context, msg);
        if(pendingIntent == null){
        	return;
        }
        
        ActionLog.getInstance(context).addAction(ActionLog.RadarShow, msg.getType(), msg.getDynamicPOI()==null?"none":(""+msg.getDynamicPOI().getMasterType()));
        
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification notif = createPulledMessageNotification(context, msg, pendingIntent);
        
        // we use a string id because is a unique number.  we use it later to cancel the
        // notification
        nm.notify(R.layout.sphinx, notif);
    }

	/**
	 * Create and set the notification contents
	 * @param context
	 * @param notif
	 * @param msg
	 * @param pendingIntent 
	 * @return the new notification with the specified content
	 * @see String
	 */
	private static Notification createPulledMessageNotification(Context context, Message msg, PendingIntent pendingIntent){
		Notification notif = new Notification();
        String contentText = "";
    	switch ((int)msg.getType()) 
    	{
			case Message.TYPE_PRODUCT_UPGRADE:
				contentText = context.getString(R.string.radar_new_version);
				break;
	
			case Message.TYPE_PRODUCT_INFOMATION:
				contentText = msg.getProductMsg().getDescription();
				break;
				
			case Message.TYPE_HOLIDAY:
				contentText = context.getString(R.string.radar_holidy) + 
								(msg.getDynamicPOI().getDescription()==null?"":msg.getDynamicPOI().getDescription());
				break;
				
			case Message.TYPE_FILM:
				contentText = context.getString(R.string.radar_new_film) + 
								(msg.getDynamicPOI().getDescription()==null?"":msg.getDynamicPOI().getDescription());
				break;
				
			case Message.TYPE_INTERVAL:
				contentText = context.getString(R.string.radar_interval) + 
								(msg.getDynamicPOI().getDescription()==null?"":msg.getDynamicPOI().getDescription());
				break;
		}
    	notif.icon = R.drawable.notif_left_icon;
    	notif.tickerText = contentText;
        notif.setLatestEventInfo(context, context.getString(R.string.app_name), contentText, pendingIntent);
        notif.flags = notif.flags|Notification.FLAG_AUTO_CANCEL;
        return notif;
	}
	
    public static void cancel(Context context) {
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(R.layout.sphinx);
    }
}

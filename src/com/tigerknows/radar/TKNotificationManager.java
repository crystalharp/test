package com.tigerknows.radar;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.model.PullMessage.Message;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class TKNotificationManager {

    public static PendingIntent makeIntent(Context context, Message data) {
        // The PendingIntent to launch our activity if the user selects this
        // notification.  Note the use of FLAG_UPDATE_CURRENT so that if there
        // is already an active matching pending intent, we will update its
        // extras to be the ones passed in here.
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, Sphinx.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Sphinx.EXTRA_PULL_MESSAGE, data),
                PendingIntent.FLAG_UPDATE_CURRENT);
        return contentIntent;
    }

    public static void notify(Context context, Message data) {
        if (data == null) {
            return;
        }
        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        Notification notif = new Notification();

        notif.contentIntent = makeIntent(context, data);

        CharSequence text = context.getString(R.string.app_name);
        notif.tickerText = text;

        // the icon for the status bar
        notif.icon = R.drawable.icon;

        // our custom view
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
        contentView.setTextViewText(R.id.text_txv, data.toString());
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

package com.tigerknows.widget;

import android.content.Context;

public class Toast {
    
    private static volatile android.widget.Toast toast = null;
    
    public static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;
    
    public static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;

    public static android.widget.Toast makeText(Context context, int resId, int duration) {
        return makeText(context, context.getString(resId), duration);
    }

    public static android.widget.Toast makeText(Context context, CharSequence text, int duration) {
        if (toast == null) {
            toast = android.widget.Toast.makeText(context, text, duration);
        }
        toast.cancel();
        toast.setText(text);
        toast.setDuration(duration);
        return toast;
    }
    
    public static void cancel() {
        android.widget.Toast toast = Toast.toast;
        if (toast != null) {
            toast.cancel();
        }
    }
}

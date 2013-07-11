package com.tigerknows.widget;

import android.content.Context;

public class Toast {
    
    public static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;
    
    public static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;

    public static android.widget.Toast makeText(Context context, int resId, int duration) {
        return makeText(context, context.getString(resId), duration);
    }

    public static android.widget.Toast makeText(Context context, CharSequence text, int duration) {
        return android.widget.Toast.makeText(context, text, duration);
    }
}

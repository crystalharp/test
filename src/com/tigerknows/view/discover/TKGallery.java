
package com.tigerknows.view.discover;

import com.decarta.Globals;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

public class TKGallery extends Gallery {
    
    int width;

    public TKGallery(Context context) {
        super(context);
    }

    public TKGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        width = (int) ((Globals.g_metrics.widthPixels > Globals.g_metrics.heightPixels ? Globals.g_metrics.heightPixels : Globals.g_metrics.widthPixels)*0.85);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        int kEvent;
        if (isScrollingLeft(e1, e2)) {
            // Check if scrolling left
            kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
        } else {
            // Otherwise scrolling right
            kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        onKeyDown(kEvent, null);
        return true;
//        if (velocityX > width) {
//            velocityX = width;
//        } else if (velocityX < -width) {
//            velocityX = -width;
//        }
//        return super.onFling(e1, e2, velocityX, velocityY);
    }

    private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2) {
        return e2.getX() > e1.getX();
    }

}


package com.tigerknows;

import com.tigerknows.R;
import com.tigerknows.view.ScrollScreen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class Help extends BaseActivity implements ScrollScreen.ScreenContentFactory,
        ScrollScreen.OnScreenChangeListener {
    
    public static final String APP_FIRST_START = "AppFirstStart";
    
    public static final String APP_UPGRADE = "AppUpgrade";

    private int mViewCount = 2;
    
    private View[] mViews;
    
    private boolean mAppFirstStart = false;
    
    private boolean mAppUpgrade = false;

    private ScrollScreen screen;

    private ScrollScreen.ScreenIndicator indicator;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionTag = ActionLog.Help;
        if (mIntent != null) {
            mAppFirstStart = mIntent.getBooleanExtra(APP_FIRST_START, false); 
            mAppUpgrade = mIntent.getBooleanExtra(APP_UPGRADE, false); 
            if (mAppUpgrade || mAppFirstStart) {
                mViewCount = 4;
            } else {
                mViewCount = 3;
            }
        }
        ScrollScreen scrollScreen = getScrollScreen();
        scrollScreen.addScreen(mViewCount, this);
        scrollScreen.setOnScreenChangedListener(this);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        screen = (ScrollScreen)findViewById(R.id.scroll_screen);

        if (screen == null) {
            throw new RuntimeException(
                    "Your content must have a ScrollScreen whose id attribute is "
                            + "'com.zhang.new_test.R.id.scroll_screen'");
        }

        indicator = (ScrollScreen.ScreenIndicator)findViewById(R.id.screen_indicator);
        if (indicator != null) {
            screen.setScreenIndicator(indicator);
        }
    }

    private void ensureScrollScreen() {
        if (screen == null) {
            this.setContentView(R.layout.help);
        }
    }

    public ScrollScreen getScrollScreen() {
        ensureScrollScreen();
        return screen;
    }

    @Override
    public View createScreenContent(int index) {
        if (mViews == null) {
            if (mAppUpgrade || mAppFirstStart) {
                screen.setBackgroundColor(0xff000000);
                mViews = new View[mViewCount];
                ImageView view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn_new1);
                view.setScaleType(ScaleType.FIT_XY);
                mViews[0] = view;
    
                view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn_new2);
                view.setScaleType(ScaleType.FIT_XY);
                mViews[1] = view;
    
                view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn_new3);
                view.setScaleType(ScaleType.FIT_XY);
                mViews[2] = view;
    
                LayoutInflater layoutInflater = (LayoutInflater)mThis.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = layoutInflater.inflate(R.layout.help_end, null, false);
                view = (ImageView) layout.findViewById(R.id.image_imv);
                view.setImageResource(R.drawable.ic_learn_new4);
                view.setScaleType(ScaleType.FIT_XY);
                Button button = (Button) layout.findViewById(R.id.enter_btn);
                button.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View arg0) {
                        finish();
                    }
                });
                mViews[3] = layout;
            } else {
                screen.setBackgroundColor(0xffe6e6e6);
                mViews = new View[mViewCount];
                ImageView view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn1);
                view.setScaleType(ScaleType.FIT_XY);
                mViews[0] = view;
    
                view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn2);
                view.setScaleType(ScaleType.FIT_XY);
                mViews[1] = view;
    
                view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn3);
                view.setScaleType(ScaleType.FIT_XY);
                mViews[2] = view;
            }
        }

        return mViews[index];
    }

    @Override
    public void onScreenChanged(int index) {
    }
    
    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(APP_FIRST_START, mAppFirstStart);
        intent.putExtra(APP_UPGRADE, mAppUpgrade);
        setResult(RESULT_OK, intent);
        super.finish();
    }

}

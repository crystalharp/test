
package com.tigerknows;

import com.tigerknows.R;
import com.tigerknows.view.FavoriteFragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import java.util.ArrayList;
import java.util.List;

public class Help extends BaseActivity {
    
    public static final String APP_FIRST_START = "AppFirstStart";
    
    public static final String APP_UPGRADE = "AppUpgrade";

    private boolean mAppFirstStart = false;
    
    private boolean mAppUpgrade = false;

    private ViewPager mViewPager;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionTag = ActionLog.Help;
        if (mIntent != null) {
            mAppFirstStart = mIntent.getBooleanExtra(APP_FIRST_START, false); 
            mAppUpgrade = mIntent.getBooleanExtra(APP_UPGRADE, false); 
        }
        
        mViewPager = new ViewPager(mThis);
        
        List<View> viewList = createViewList();
        mViewPager.setAdapter(new FavoriteFragment.MyAdapter(viewList));
        
        setContentView(mViewPager);
    }

    public List<View> createViewList() {
        List<View> viewList = new ArrayList<View>();
        if (mAppUpgrade || mAppFirstStart) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn1);
            view.setScaleType(ScaleType.FIT_XY);
            viewList.add(view);

            view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn2);
            view.setScaleType(ScaleType.FIT_XY);
            viewList.add(view);

            LayoutInflater layoutInflater = (LayoutInflater)mThis.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = layoutInflater.inflate(R.layout.help_end, null, false);
            view = (ImageView) layout.findViewById(R.id.image_imv);
            view.setImageResource(R.drawable.ic_learn3);
            view.setScaleType(ScaleType.FIT_XY);
            Button button = (Button) layout.findViewById(R.id.enter_btn);
            button.setOnClickListener(new View.OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    finish();
                }
            });
            viewList.add(layout);
        } else {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn1);
            view.setScaleType(ScaleType.FIT_XY);
            viewList.add(view);

            view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn2);
            view.setScaleType(ScaleType.FIT_XY);
            viewList.add(view);

            view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn3);
            view.setScaleType(ScaleType.FIT_XY);
            viewList.add(view);
        }
        
        return viewList;
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

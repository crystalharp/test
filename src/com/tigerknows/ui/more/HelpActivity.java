
package com.tigerknows.ui.more;

import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;

import java.util.HashMap;

public class HelpActivity extends BaseActivity {
    
    public static final String APP_FIRST_START = "AppFirstStart";
    
    public static final String APP_UPGRADE = "AppUpgrade";
    
    private int mPagecount = 1;

    private boolean mAppFirstStart = false;
    
    private boolean mAppUpgrade = false;

    private ViewPager mViewPager;
    
    private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionTag = ActionLog.Help;
        if (mIntent != null) {
            mAppFirstStart = mIntent.getBooleanExtra(APP_FIRST_START, false); 
            mAppUpgrade = mIntent.getBooleanExtra(APP_UPGRADE, false); 
        }
        
        if (mAppUpgrade) {
            mPagecount = 1;
        } else {
            mPagecount = 4;
        }
        
        mViewPager = new ViewPager(mThis);
        
        mViewPager.setAdapter(new MyAdapter());
        
        setContentView(mViewPager);
    }

    public View getView(int position) {
        if (viewMap.containsKey(position)) {
            return viewMap.get(position);
        }
        
        if (position == 0) {
            if (mAppUpgrade) {
                LayoutInflater layoutInflater = (LayoutInflater)mThis.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.more_help_end, null, false);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_imv);
                imageView.setImageResource(R.drawable.ic_learn1_upgrade);
                imageView.setScaleType(ScaleType.FIT_XY);
                Button button = (Button) view.findViewById(R.id.enter_btn);
                button.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View arg0) {
                        finish();
                    }
                });
                viewMap.put(position, view);
                return view;
            } else {
                ImageView view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn1);
                view.setScaleType(ScaleType.FIT_XY);
                viewMap.put(position, view);
                return view;
            }
        } else if (position == 1) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn2);
            view.setScaleType(ScaleType.FIT_XY);
            viewMap.put(position, view);
            return view;
        } else if (position == 2) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn3);
            view.setScaleType(ScaleType.FIT_XY);
            viewMap.put(position, view);
            return view;
        } else {
            if (mAppFirstStart) {
                LayoutInflater layoutInflater = (LayoutInflater)mThis.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.more_help_end, null, false);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_imv);
                imageView.setImageResource(R.drawable.ic_learn4);
                imageView.setScaleType(ScaleType.FIT_XY);
                Button button = (Button) view.findViewById(R.id.enter_btn);
                button.setOnClickListener(new View.OnClickListener() {
                    
                    @Override
                    public void onClick(View arg0) {
                        finish();
                    }
                });
                viewMap.put(position, view);
                return view;
            } else {
                ImageView view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn4);
                view.setScaleType(ScaleType.FIT_XY);
                viewMap.put(position, view);
                return view; 
            }
        }
    }
    
    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(APP_FIRST_START, mAppFirstStart);
        intent.putExtra(APP_UPGRADE, mAppUpgrade);
        setResult(RESULT_OK, intent);
        super.finish();
    }
    
     public class MyAdapter extends PagerAdapter {
        
        public MyAdapter() {
        }
        
        @Override
        public int getCount() {
            return mPagecount;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public void destroyItem(View contain, int position, Object arg2) {
             ((ViewPager) contain).removeView(getView(position));
        }

        @Override
        public Object instantiateItem(ViewGroup contain, int position) {
            View view = getView(position);
            contain.addView(getView(position), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            return view;
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
            // TODO Auto-generated method stub
        }

        @Override
        public Parcelable saveState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void finishUpdate(View arg0) {
            // TODO Auto-generated method stub
        }

    }
}

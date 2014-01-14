
package com.tigerknows;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;

import com.tigerknows.R;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.common.ActionLog;
import com.tigerknows.util.Utility;

@SuppressWarnings("deprecation")
public class GuideScreenActivity extends TKActivity {
    
    static final String TAG = "GuideScreenActivity";
    
    public static final String APP_FIRST_START = "AppFirstStart";
    
    public static final String APP_UPGRADE = "AppUpgrade";
    
    private int mPagecount = 2;

    private ViewPager mViewPager;
    
    private ViewGroup mIndicatorView;
    
    private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();
    
    private boolean mStartSphinx = false;
    
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.Help;

        setContentView(R.layout.guide_screen);
        
        findViews();
        setListener();
        
        Utility.pageIndicatorInit(mThis, mIndicatorView, mPagecount, 0, R.drawable.ic_dot_guide_screen_normal, R.drawable.ic_dot_guide_screen_focused);
        mViewPager.setAdapter(new MyAdapter());
        
        registerReceiver(mBroadcastReceiver, new IntentFilter(Sphinx.ACTION_ONRESUME));
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_BACK:
                startSphinx();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        unregisterReceiver(mBroadcastReceiver);
    }
    
    private Button startExperience;
    
    protected void findViews() {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mIndicatorView = (ViewGroup) findViewById(R.id.indicator_view);
    	startExperience = (Button) findViewById(R.id.start_btn);
    	
    }
    
    protected void setListener() {
        
        startExperience.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			    startSphinx();
			}
		});
        
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            
            @Override
            public void onPageSelected(int position) {
                Utility.pageIndicatorChanged(mThis, mIndicatorView, position, R.drawable.ic_dot_guide_screen_normal, R.drawable.ic_dot_guide_screen_focused);
            }
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub
                
            }
        });
    }

    private View getView(int position) {
        if (viewMap.containsKey(position)) {
            return viewMap.get(position);
        }
        
        if (position == 0) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_guide_screen1);
            view.setScaleType(ScaleType.CENTER);
            viewMap.put(position, view);
            return view;
        } else if (position == 1) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_guide_screen2);
            view.setScaleType(ScaleType.CENTER);
            viewMap.put(position, view);
            return view;
        }
        
        return null;
    }
    
    private void startSphinx() {
        if (mStartSphinx == false) {
            mStartSphinx = true;
            Intent intent = getIntent();
            Intent newIntent = new Intent();
            newIntent.setData(intent.getData());
            newIntent.putExtras(intent);
            newIntent.setClass(getBaseContext(), Sphinx.class);
            startActivity(newIntent);
        }
    }
    
    private class MyAdapter extends PagerAdapter {
        
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

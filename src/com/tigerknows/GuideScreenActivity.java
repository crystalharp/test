
package com.tigerknows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.app.TKActivity;
import com.tigerknows.common.ActionLog;

@SuppressWarnings("deprecation")
public class GuideScreenActivity extends TKActivity {
    
    static final String TAG = "GuideScreenActivity";
    
    public static final String APP_FIRST_START = "AppFirstStart";
    
    public static final String APP_UPGRADE = "AppUpgrade";
    
    private int mPagecount = 4;

    private ViewPager mViewPager;
    
    private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();
    
    private AbsoluteLayout animContainer;
    
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
        
        mViewPager.setAdapter(new MyAdapter());
		pageChangeListener.onPageScrolled(0, 0, 0);
        
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

    // 动画的配置，只要抓住几个关键帧就能够计算出各个参数。
	GuideScreenAnimateItem[] animItems = new GuideScreenAnimateItem[]{
    		new GuideScreenAnimateItem("cloud", 			0.21f, 0.15f, 	0.0f, 0.15f, 	0.0f, 0.5f, R.drawable.help_cloud),
//    		new HelpAnimateItem("house",	 		0.5f, 0.5f, 	0.0f, 0.5f, 	0.0f, 0.5f, R.drawable.help_house),
    		new GuideScreenAnimateItem("bubble_red",	 	0.6f, 0.6f, 	1.8f, 0.6f, 	0.0f, 0.5f, R.drawable.help_bubble_red),
    		new GuideScreenAnimateItem("bubble_green", 	0.1f, 0.41f, 	1.8f, 0.41f, 	0.0f, 0.5f, R.drawable.help_bubble_green),
    		new GuideScreenAnimateItem("bubble_blue", 		0.75f, 0.25f, 	1.8f, 0.25f, 	0.0f, 0.5f, R.drawable.help_bubble_blue),
    		new GuideScreenAnimateItem("bubble_yellow", 	0.8f, 0.45f, 	1.8f, 0.45f, 	0.0f, 0.5f, R.drawable.help_bubble_yellow),
    		new GuideScreenAnimateItem("near_by_hot", 		0.5f, 0.75f, 	-0.5f, 0.75f, 	0.0f, 0.6f, R.drawable.help_near_by_hot),
    		
    		new GuideScreenAnimateItem("cloud2", 			1.6f, 0.15f, 	1.8f, 0.15f, 	0.5f, 2.0f, R.drawable.help_cloud),
//    		new HelpAnimateItem("hotel", 			1.6f, 0.5f, 	1.5f, 0.5f, 	0.0f, 1.0f, R.drawable.help_hotel),
    		// 旗子进入
    		new GuideScreenAnimateItem("order_flag",	 	0.35f, 0.33f, 	1.3f, 0.33f, 	0.5f, 1.0f, R.drawable.help_order_flag),

    		new GuideScreenAnimateItem("order_hotel", 		1.7f, 0.75f, 	1.3f, 0.75f, 	0.5f, 1.5f, R.drawable.help_order_hotel),
    		
    		// 移入速度和移出速度不一样，所以使用两段来实现。
    		// 云慢慢进入
    		new GuideScreenAnimateItem("cloud3_in", 			2.6f, 0.30f, 	2.8f, 0.30f, 	1.5f, 2.0f, R.drawable.help_cloud),
    		new GuideScreenAnimateItem("cloud4_in", 			2.2f, 0.15f, 	2.3f, 0.15f, 	1.0f, 2.0f, R.drawable.help_cloud_big),
    		
    		// 此处旗子放在下边是为了防止云挡住旗子。
    		// 旗子移出
    		new GuideScreenAnimateItem("order_flag",	 	1.3f, 0.33f, 	2.7f, 0.33f, 	1.0f, 1.5f, R.drawable.help_order_flag),
    		
    		// 云随屏幕移动出
    		new GuideScreenAnimateItem("cloud3_out", 		2.8f, 0.30f, 	2.8f, 0.30f, 	2.0f, 3.0f, R.drawable.help_cloud),
    		new GuideScreenAnimateItem("cloud4_out", 		2.3f, 0.15f, 	2.8f, 0.15f, 	2.0f, 3.0f, R.drawable.help_cloud_big),
    		
    		new GuideScreenAnimateItem("tuangou_coupon", 	2.8f, 0.75f, 	2.2f, 0.75f, 	1.5f, 2.5f, R.drawable.help_tuangou_coupon),
    		

    		new GuideScreenAnimateItem("maze", 			3.5f, 0.4f, 	3.5f, 0.4f, 	2.0f, 3.0f, R.drawable.help_maze),
    		
    		// 从不可见进入该屏的视野中心，横移1.5， 纵移0.5， 移出亦然。 保证在（1.3， 2.7） 中间的位置的时候（即2.0）箭头达到希望位置
//    		new HelpAnimateItem("arrow1", 			0.85f, 1.25f, 	2.35f, 0.35f, 	1.5f, 2.0f, R.drawable.help_arrow1),
    		new GuideScreenAnimateItem("arrow1", 			0.84f, 0.8f, 	3.76f, -0.12f, 	1.3f, 2.7f, R.drawable.help_arrow1),
//    		new HelpAnimateItem("arrow2", 			0.8f, 0.95f, 	2.5f, 0.38f, 	1.3f, 2.0f, R.drawable.help_arrow2),
//    		new HelpAnimateItem("arrow3", 			1.05f, 1.0f, 	2.7f, 0.45f, 	1.3f, 2.0f, R.drawable.help_arrow3),
    		new GuideScreenAnimateItem("arrow2", 			0.8f, 0.95f, 	4.2f, -0.19f, 	1.3f, 2.7f, R.drawable.help_arrow2),
    		new GuideScreenAnimateItem("arrow3", 			1.05f, 1.0f, 	4.35f, -0.10f, 	1.3f, 2.7f, R.drawable.help_arrow3),
    		
//    		new HelpAnimateItem("order_hotel", 		1.9f, 0.75f, 	1.5f, 0.75f, 	0.84f, 1.0f, R.drawable.help_order_hotel),
//    		new HelpAnimateItem("order_hotel", 		1.9f, 0.75f, 	1.5f, 0.75f, 	0.84f, 1.0f, R.drawable.help_order_hotel),
//    		new HelpAnimateItem("order_hotel", 		1.9f, 0.75f, 	1.5f, 0.75f, 	0.84f, 1.0f, R.drawable.help_order_hotel)
    		

    		new GuideScreenAnimateItem("cloud3", 			3.6f, 0.10f, 	3.2f, 0.10f, 	2.5f, 3.0f, R.drawable.help_cloud),
    		
    		new GuideScreenAnimateItem("target_red", 		2.5f, 0.4f, 	3.5f, 0.4f, 	2.5f, 3.0f, R.drawable.help_target_red),
    		new GuideScreenAnimateItem("target_red", 		3.5f, 0.0f, 	3.5f, 0.4f, 	2.5f, 3.0f, R.drawable.help_arrow_traffic),

    		new GuideScreenAnimateItem("traffic_go", 		3.7f, 0.75f, 	3.3f, 0.75f, 	2.5f, 3.5f, R.drawable.help_traffic_go),

    		new GuideScreenAnimateItem("progress_line1",	0.35f, 0.96f, 	3.35f, 0.96f, 	0.0f, 3.0f, R.drawable.help_progress_line),
    		new GuideScreenAnimateItem("progress_line2",	0.45f, 0.96f, 	3.45f, 0.96f, 	0.0f, 3.0f, R.drawable.help_progress_line),
    		new GuideScreenAnimateItem("progress_line3",	0.55f, 0.96f, 	3.55f, 0.96f, 	0.0f, 3.0f, R.drawable.help_progress_line),
    		new GuideScreenAnimateItem("progress_line4",	0.65f, 0.96f, 	3.65f, 0.96f, 	0.0f, 3.0f, R.drawable.help_progress_line),

    		new GuideScreenAnimateItem("progress_car",		0.35f, 0.95f, 	3.65f, 0.95f, 	0.0f, 3.0f, R.drawable.help_car),

    		new GuideScreenAnimateItem("progress_car",		2.3f, 0.95f, 	3.88f, 0.95f, 	2.5f, 3.0f, R.drawable.btn_start_now)
    		
    };
    
    private List<GuideScreenAnimateItem> visibleItems = new ArrayList<GuideScreenAnimateItem>();
    
    private ImageView startExperience;
    
    protected void findViews() {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        
        animContainer = (AbsoluteLayout) findViewById(R.id.anim_container);

        // 初始化View和动画的Item的高度
    	int childCount = animContainer.getChildCount();
    	if(childCount<animItems.length){
    		for (int i = childCount; i < animItems.length; i++) {
    			ImageView view = new ImageView(this);
				animContainer.addView(view);
				view.setBackgroundResource(animItems[i].resId);
    			view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    			animItems[i].width = view.getMeasuredWidth();
    			animItems[i].height = view.getMeasuredHeight();
			}
    		
    	}
    	
    	startExperience = (ImageView) animContainer.getChildAt(animContainer.getChildCount()-1);
    	
    }
    
    private OnPageChangeListener pageChangeListener = new OnPageChangeListener() {
        
        @Override
        public void onPageSelected(int index) {
            mActionLog.addAction(mActionTag+ActionLog.ViewPageSelected, index);
            LogWrapper.i(TAG, "Page selected: " + index);
        }
        
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        	int screenWidth = Globals.g_metrics.widthPixels;
        	int screenHeight= Globals.g_metrics.heightPixels;
        	float curPosition = position + positionOffset;
        	
        	LogWrapper.i(TAG, "Page scrolled. Position: " + position + " offset: " + positionOffset );
        	
        	for (int i = 0; i < animItems.length; i++) {
        		GuideScreenAnimateItem item = animItems[i];
        		if(item.scrollStartX <= curPosition && item.scrollEndX>= curPosition){
        			
        			int left = 0;
        			int top = 0;
        			float progress = (curPosition-item.scrollStartX)
        					/(item.scrollEndX-item.scrollStartX); 
        			
        			left = (int) ((item.startX + ( item.endX - item.startX ) * progress - curPosition) * screenWidth);
        			top = (int) ( (item.startY + ( item.endY-item.startY ) * progress) * screenHeight);
        			item.x = left;
        			item.y = top;
        			visibleItems.add(item);
        			item.visible = true;
        		}else{
        			item.visible = false;
        		}
        			
    		}
        	
        	updateAnimViews(animContainer);
		}
        
        @Override
        public void onPageScrollStateChanged(int state) {
        	
        }
        
    };
    
    private void updateAnimViews(AbsoluteLayout container){
    	
    	
    	for (int i = 0; i < animItems.length; i++) {
    		View view = container.getChildAt(i);
    		GuideScreenAnimateItem item = animItems[i];
    		if(item.visible){
    			android.widget.AbsoluteLayout.LayoutParams params = (android.widget.AbsoluteLayout.LayoutParams) view.getLayoutParams();
    			
    			params.x = item.x - item.width/2;
    			params.y = item.y - item.height/2;
    			
    			LogWrapper.i(TAG, (i+1) + "/" + animItems.length + " " + params.x + ", " + params.y + " View width: " + view.getMeasuredWidth() + " Height: " + view.getMeasuredHeight() + " Tag: " + item.tag);
    			
    			view.setLayoutParams(params);
    			view.setVisibility(View.VISIBLE);
    		}else{
    			view.setVisibility(View.GONE); 
    		}
		}
    	
    }
    
    protected void setListener() {

        mViewPager.setOnPageChangeListener(pageChangeListener);
        
        startExperience.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			    startSphinx();
			}
		});
        
    }

    public View getView(int position) {
        if (viewMap.containsKey(position)) {
            return viewMap.get(position);
        }
        
        if (position == 0) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn1);
            view.setScaleType(ScaleType.FIT_XY);
            viewMap.put(position, view);
            return view;
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
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn4);
            view.setScaleType(ScaleType.FIT_XY);
            viewMap.put(position, view);
            return view;
        }
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

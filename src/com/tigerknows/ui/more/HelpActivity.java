
package com.tigerknows.ui.more;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.common.ActionLog;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;

public class HelpActivity extends BaseActivity {
    
    public static final String APP_FIRST_START = "AppFirstStart";
    
    public static final String APP_UPGRADE = "AppUpgrade";
    
    private int mPagecount = 1;

    private boolean mAppFirstStart = false;
    
    private boolean mAppUpgrade = false;

    private ViewPager mViewPager;
    
    private HashMap<Integer, View> viewMap = new HashMap<Integer, View>();
    
    private LinearLayout rootView;
    
    private AbsoluteLayout animContainer;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActionTag = ActionLog.Help;
        if (mIntent != null) {
            mAppFirstStart = mIntent.getBooleanExtra(APP_FIRST_START, false); 
            mAppUpgrade = mIntent.getBooleanExtra(APP_UPGRADE, false); 
        }
        
        if (mAppUpgrade) {
            mPagecount = 2;
        } else {
            mPagecount = 4;
        }

        setContentView(R.layout.more_help);
        
        findViews();
        setListener();
        
        mViewPager.setAdapter(new MyAdapter());
		pageChangeListener.onPageScrolled(0, 0, 0);
		
    }
    
    @Override
	protected void onResume() {
		super.onResume();
	}

    // 动画的配置，只要抓住几个关键帧就能够计算出各个参数。
	HelpAnimateItem[] animItems = new HelpAnimateItem[]{
    		new HelpAnimateItem("cloud", 			0.21f, 0.15f, 	0.0f, 0.15f, 	0.0f, 0.5f, R.drawable.help_cloud),
//    		new HelpAnimateItem("house",	 		0.5f, 0.5f, 	0.0f, 0.5f, 	0.0f, 0.5f, R.drawable.help_house),
    		new HelpAnimateItem("bubble_red",	 	0.6f, 0.6f, 	1.8f, 0.6f, 	0.0f, 0.5f, R.drawable.help_bubble_red),
    		new HelpAnimateItem("bubble_green", 	0.1f, 0.41f, 	1.8f, 0.41f, 	0.0f, 0.5f, R.drawable.help_bubble_green),
    		new HelpAnimateItem("bubble_blue", 		0.75f, 0.25f, 	1.8f, 0.25f, 	0.0f, 0.5f, R.drawable.help_bubble_blue),
    		new HelpAnimateItem("bubble_yellow", 	0.8f, 0.45f, 	1.8f, 0.45f, 	0.0f, 0.5f, R.drawable.help_bubble_yellow),
    		new HelpAnimateItem("near_by_hot", 		0.5f, 0.75f, 	-0.5f, 0.75f, 	0.0f, 0.6f, R.drawable.help_near_by_hot),
    		
    		new HelpAnimateItem("cloud2", 			1.6f, 0.15f, 	1.8f, 0.15f, 	0.5f, 2.0f, R.drawable.help_cloud),
//    		new HelpAnimateItem("hotel", 			1.6f, 0.5f, 	1.5f, 0.5f, 	0.0f, 1.0f, R.drawable.help_hotel),
    		// 旗子进入
    		new HelpAnimateItem("order_flag",	 	0.35f, 0.33f, 	1.3f, 0.33f, 	0.5f, 1.0f, R.drawable.help_order_flag),

    		new HelpAnimateItem("order_hotel", 		1.7f, 0.75f, 	1.3f, 0.75f, 	0.5f, 1.5f, R.drawable.help_order_hotel),
    		
    		// 移入速度和移出速度不一样，所以使用两段来实现。
    		// 云慢慢进入
    		new HelpAnimateItem("cloud3_in", 			2.6f, 0.30f, 	2.8f, 0.30f, 	1.5f, 2.0f, R.drawable.help_cloud),
    		new HelpAnimateItem("cloud4_in", 			2.2f, 0.15f, 	2.3f, 0.15f, 	1.0f, 2.0f, R.drawable.help_cloud_big),
    		
    		// 此处旗子放在下边是为了防止云挡住旗子。
    		// 旗子移出
    		new HelpAnimateItem("order_flag",	 	1.3f, 0.33f, 	2.7f, 0.33f, 	1.0f, 1.5f, R.drawable.help_order_flag),
    		
    		// 云随屏幕移动出
    		new HelpAnimateItem("cloud3_out", 		2.8f, 0.30f, 	2.8f, 0.30f, 	2.0f, 3.0f, R.drawable.help_cloud),
    		new HelpAnimateItem("cloud4_out", 		2.3f, 0.15f, 	2.8f, 0.15f, 	2.0f, 3.0f, R.drawable.help_cloud_big),
    		
    		new HelpAnimateItem("tuangou_coupon", 	2.8f, 0.75f, 	2.2f, 0.75f, 	1.5f, 2.5f, R.drawable.help_tuangou_coupon),
    		

    		new HelpAnimateItem("maze", 			3.5f, 0.4f, 	3.5f, 0.4f, 	2.0f, 3.0f, R.drawable.help_maze),
    		
    		// 从不可见进入该屏的视野中心，横移1.5， 纵移0.5， 移出亦然。 保证在（1.3， 2.7） 中间的位置的时候（即2.0）箭头达到希望位置
//    		new HelpAnimateItem("arrow1", 			0.85f, 1.25f, 	2.35f, 0.35f, 	1.5f, 2.0f, R.drawable.help_arrow1),
    		new HelpAnimateItem("arrow1", 			0.84f, 0.8f, 	3.76f, -0.12f, 	1.3f, 2.7f, R.drawable.help_arrow1),
//    		new HelpAnimateItem("arrow2", 			0.8f, 0.95f, 	2.5f, 0.38f, 	1.3f, 2.0f, R.drawable.help_arrow2),
//    		new HelpAnimateItem("arrow3", 			1.05f, 1.0f, 	2.7f, 0.45f, 	1.3f, 2.0f, R.drawable.help_arrow3),
    		new HelpAnimateItem("arrow2", 			0.8f, 0.95f, 	4.2f, -0.19f, 	1.3f, 2.7f, R.drawable.help_arrow2),
    		new HelpAnimateItem("arrow3", 			1.05f, 1.0f, 	4.35f, -0.10f, 	1.3f, 2.7f, R.drawable.help_arrow3),
    		
//    		new HelpAnimateItem("order_hotel", 		1.9f, 0.75f, 	1.5f, 0.75f, 	0.84f, 1.0f, R.drawable.help_order_hotel),
//    		new HelpAnimateItem("order_hotel", 		1.9f, 0.75f, 	1.5f, 0.75f, 	0.84f, 1.0f, R.drawable.help_order_hotel),
//    		new HelpAnimateItem("order_hotel", 		1.9f, 0.75f, 	1.5f, 0.75f, 	0.84f, 1.0f, R.drawable.help_order_hotel)
    		

    		new HelpAnimateItem("cloud3", 			3.6f, 0.10f, 	3.2f, 0.10f, 	2.5f, 3.0f, R.drawable.help_cloud),
    		
    		new HelpAnimateItem("target_red", 		2.5f, 0.4f, 	3.5f, 0.4f, 	2.5f, 3.0f, R.drawable.help_target_red),
    		new HelpAnimateItem("target_red", 		3.5f, 0.0f, 	3.5f, 0.4f, 	2.5f, 3.0f, R.drawable.help_arrow_traffic),

    		new HelpAnimateItem("traffic_go", 		3.7f, 0.75f, 	3.3f, 0.75f, 	2.5f, 3.5f, R.drawable.help_traffic_go),

    		new HelpAnimateItem("progress_line1",	0.35f, 0.91f, 	3.35f, 0.91f, 	0.0f, 3.0f, R.drawable.help_progress_line),
    		new HelpAnimateItem("progress_line2",	0.45f, 0.91f, 	3.45f, 0.91f, 	0.0f, 3.0f, R.drawable.help_progress_line),
    		new HelpAnimateItem("progress_line3",	0.55f, 0.91f, 	3.55f, 0.91f, 	0.0f, 3.0f, R.drawable.help_progress_line),
    		new HelpAnimateItem("progress_line4",	0.65f, 0.91f, 	3.65f, 0.91f, 	0.0f, 3.0f, R.drawable.help_progress_line),

    		new HelpAnimateItem("progress_car",		0.35f, 0.90f, 	3.65f, 0.90f, 	0.0f, 3.0f, R.drawable.help_car),

    		new HelpAnimateItem("progress_car",		2.3f, 0.90f, 	3.88f, 0.90f, 	2.5f, 3.0f, R.drawable.btn_start_now)
    		
    };
    
    private View[] animViews = new View[3];
    
    private List<HelpAnimateItem> visibleItems = new ArrayList<HelpAnimateItem>();
    
    private ImageView startExperience;
    
    protected void findViews() {
        super.findViews();
        rootView = (LinearLayout) findViewById(R.id.root_view);
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
            LogWrapper.i("HelpActivity", "Page selected: " + index);
        }
        
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        	int screenWidth = Globals.g_metrics.widthPixels;
        	int screenHeight= Globals.g_metrics.heightPixels;
        	float curPosition = position + positionOffset;
        	
        	LogWrapper.i("HelpActivity", "Page scrolled. Position: " + position + " offset: " + positionOffset );
        	
        	for (int i = 0; i < animItems.length; i++) {
        		HelpAnimateItem item = animItems[i];
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
    		HelpAnimateItem item = animItems[i];
    		if(item.visible){
    			android.widget.AbsoluteLayout.LayoutParams params = (android.widget.AbsoluteLayout.LayoutParams) view.getLayoutParams();
    			
    			params.x = item.x - item.width/2;
    			params.y = item.y - item.height/2;
    			
    			LogWrapper.i("HelpActivity", (i+1) + "/" + animItems.length + " " + params.x + ", " + params.y + " View width: " + view.getMeasuredWidth() + " Height: " + view.getMeasuredHeight() + " Tag: " + item.tag);
    			
    			view.setLayoutParams(params);
    			view.setVisibility(View.VISIBLE);
    		}else{
    			view.setVisibility(View.GONE); 
    		}
		}
    	
    }
    
    protected void setListener() {
        super.setListener();

        mViewPager.setOnPageChangeListener(pageChangeListener);
        
        startExperience.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
        
    }

    public View getView(int position) {
        if (viewMap.containsKey(position)) {
            return viewMap.get(position);
        }
        
        if (position == 0) {
            if (mAppUpgrade) {
                ImageView view = new ImageView(mThis);
                view.setImageResource(R.drawable.ic_learn4);
                view.setScaleType(ScaleType.FIT_XY);
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
            if (mAppUpgrade) {
                LayoutInflater layoutInflater = (LayoutInflater)mThis.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.more_help_end, null, false);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_imv);
                imageView.setImageResource(R.drawable.ic_learn5);
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
                view.setImageResource(R.drawable.ic_learn2);
                view.setScaleType(ScaleType.FIT_XY);
                viewMap.put(position, view);
                return view;
            }
        } else if (position == 2) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn3);
            view.setScaleType(ScaleType.FIT_XY);
            viewMap.put(position, view);
            return view;
        } else if (position == 3) {
            ImageView view = new ImageView(mThis);
            view.setImageResource(R.drawable.ic_learn4);
            view.setScaleType(ScaleType.FIT_XY);
            viewMap.put(position, view);
            return view;
        } else {
            if (mAppFirstStart) {
                LayoutInflater layoutInflater = (LayoutInflater)mThis.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = layoutInflater.inflate(R.layout.more_help_end, null, false);
                ImageView imageView = (ImageView) view.findViewById(R.id.image_imv);
                imageView.setImageResource(R.drawable.ic_learn5);
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
                view.setImageResource(R.drawable.ic_learn5);
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

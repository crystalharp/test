package com.tigerknows.ui.traffic;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[动画效果]]
 * @author linqingzu
 * InterceptTouchListener是为了防止在播放动画的时候被其他操作
 * 打断，实现原理是在sphinx.xml里面添加了一个透明层，拦截一切touch
 * 事件，在动画开始的时候显示，动画结束的时候关闭。
 */
public class TrafficQueryAnimationHelper {

	private TrafficQueryFragment mQueryFragment;
	
	class InterceptTouchListener implements AnimationListener {
        
        @Override
        public void onAnimationStart(Animation animation) {
        }
        
        @Override
        public void onAnimationRepeat(Animation animation) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void onAnimationEnd(Animation animation) {
        }
    };
    InterceptTouchListener interceptTouchListener = new InterceptTouchListener();
	
	public TrafficQueryAnimationHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	public void hideBlockAndMenuAnimation() {
		setAnimation(mQueryFragment.mBlock, new NormalToMapUpAnimation());
    }
	
	public void showBlockAndMenuAnimation() {

		setAnimation(mQueryFragment.mBlock, new MapToNormalDownAnimation());
		
		mQueryFragment.getContentView().setVisibility(View.VISIBLE);
		
    }
	
	public void setAnimation(View target, Animation a) {
		a.setStartTime(Animation.START_ON_FIRST_FRAME);
		target.setAnimation(a);
	}
	
	protected final int ANIMATION_TIME = 300;
	
	public class NormalToMapUpAnimation extends TranslateAnimation {
		
		public NormalToMapUpAnimation() {
			
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.ABSOLUTE, mQueryFragment.getContentView().getTop()-mQueryFragment.getContentView().getBottom());
			setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					mQueryFragment.post(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							mQueryFragment.getContentView().setVisibility(View.GONE);
						}
					});
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub
				}
	        	
	        });
		}
	}
	
	public class NormalToMapDownAnimation extends TranslateAnimation {

		public NormalToMapDownAnimation() {
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.ABSOLUTE, 0);
			setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			setAnimationListener(interceptTouchListener);
		}

	}
	
	public class NormalToMapDownAnimation2 extends TranslateAnimation {

		public NormalToMapDownAnimation2() {
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.ABSOLUTE, 0);
			setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					mQueryFragment.post(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
						}
					});
				}
			});
		}

	}
	
	public class MapToNormalDownAnimation extends TranslateAnimation {

		public MapToNormalDownAnimation() {
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, mQueryFragment.getContentView().getTop()-mQueryFragment.getContentView().getBottom(), 
	    			Animation.RELATIVE_TO_SELF, 0.0f);
			setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			setAnimationListener(interceptTouchListener);
			// TODO Auto-generated constructor stub
		}
	}

	public class MapToNormalUpAnimation extends TranslateAnimation {

		public MapToNormalUpAnimation() {
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, 0, 
	    			Animation.RELATIVE_TO_SELF, 0.0f);
	        setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			setAnimationListener(interceptTouchListener);
			// TODO Auto-generated constructor stub
		}

	}
}

package com.tigerknows.view;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.tigerknows.Sphinx;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[动画效果]]
 * @author linqingzu
 *
 */
public class TrafficQueryAnimationHelper {

	private TrafficQueryFragment mQueryFragment;
	
	public TrafficQueryAnimationHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	public void hideBlockAndMenuAnimation() {
		setAnimation(mQueryFragment.mBlock, new NormalToMapUpAnimation());
		setAnimation(mQueryFragment.mMenuFragment, new NormalToMapDownAnimation());
		setAnimation(mQueryFragment.mSphinx.getControlView(), new NormalToMapDownAnimation2());
		mQueryFragment.mSphinx.getHandler().sendEmptyMessage(Sphinx.ROOT_VIEW_INVALIDATE);
    }
	
	public void showBlockAndMenuAnimation() {

		setAnimation(mQueryFragment.mBlock, new MapToNormalDownAnimation());
		setAnimation(mQueryFragment.mMenuFragment, new MapToNormalUpAnimation());
		setAnimation(mQueryFragment.mSphinx.getControlView(), new MapToNormalUpAnimation());
		
		mQueryFragment.mMenuFragment.setVisibility(View.VISIBLE);
		mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, mQueryFragment.mMenuFragment.getMeasuredHeight());
		mQueryFragment.getContentView().setVisibility(View.VISIBLE);
		
		mQueryFragment.mSphinx.getHandler().sendEmptyMessage(Sphinx.ROOT_VIEW_INVALIDATE);
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
					// TODO Auto-generated method stub
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
	    			Animation.ABSOLUTE, mQueryFragment.mMenuFragment.getBottom() - mQueryFragment.mMenuFragment.getTop());
			setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
		}

	}
	
	public class NormalToMapDownAnimation2 extends TranslateAnimation {

		public NormalToMapDownAnimation2() {
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.ABSOLUTE, mQueryFragment.mMenuFragment.getBottom() - mQueryFragment.mMenuFragment.getTop());
			setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub
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
							mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
							mQueryFragment.mMenuFragment.setVisibility(View.GONE);
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
			// TODO Auto-generated constructor stub
		}
	}

	public class MapToNormalUpAnimation extends TranslateAnimation {

		public MapToNormalUpAnimation() {
			super(Animation.RELATIVE_TO_SELF, 0.0f, 
	    			Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, mQueryFragment.mMenuFragment.getBottom() - mQueryFragment.mMenuFragment.getTop(), 
	    			Animation.RELATIVE_TO_SELF, 0.0f);
	        setDuration(ANIMATION_TIME);
			setInterpolator(new AccelerateInterpolator());
			// TODO Auto-generated constructor stub
		}

	}
}

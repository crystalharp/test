/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.tigerknows.ActionLog;

/**
 * @author Peng Wenyue
 */
public class MenuFragment extends BaseFragment implements View.OnClickListener {

    public MenuFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }

    public static final int[] MENU_BACKGROUD = {R.drawable.menu_poi,
        R.drawable.menu_discover,
        R.drawable.menu_traffic, 
        R.drawable.menu_more}; 
    
    public static final int[] MENU_BACKGROUD_STILL = {R.drawable.menu_poi_still,
        R.drawable.menu_discover_still,
        R.drawable.menu_traffic_still, 
        R.drawable.menu_more_still}; 
    
    public static final int[] MENU_BACKGROUD_SELECTED = {R.drawable.menu_poi_focused, 
        R.drawable.menu_discover_focused,
        R.drawable.menu_traffic_focused, 
        R.drawable.menu_more_focused}; 
    
    private ImageButton mPOIBtn;
    private ImageButton mDiscoverBtn;
    private ImageButton mTrafficBtn;
    private ImageButton mMoreBtn;
    private ImageView mCursorImv;
    
    private ImageView mUpgradeImv;
    private ImageView mDiscvoerImv;

    
    private int mMenuIdSelected;
    private ImageButton[] mMenuBtnList = new ImageButton[4]; 
    

	int[] mLocationsPOIBtn = new int[2];
    int[] mLocationsDiscoverBtn = new int[2];
    int[] mLocationsTrafficBtn = new int[2];
    int[] mLlocationsMoreBtn = new int[2];
    int[][] mLocations = new int[][]{
    		mLocationsPOIBtn
    		,mLocationsDiscoverBtn
    		,mLocationsTrafficBtn
    		,mLlocationsMoreBtn};
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.menu, container, false);
        
        findViews();
        setListener();
        initAnimationListeners();
        
		return mRootView;
    }

	AnimationListener[] mAnimListeners = new AnimationListener[4];
	
    private void initAnimationListeners() {

		for (int i=0, size = mMenuBtnList.length; i<size; i++) {
			mAnimListeners[i] = new MenuBtnAnimListener(i);
		}
		
	}

    private boolean resumeFirstCalled = true;
    
    @Override
    public void onResume() {
        super.onResume();
        
        mRootView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mSphinx.getControlView().setPadding(0, 0, 0, getMeasuredHeight());
        
        mPOIBtn.getLocationOnScreen(mLocationsPOIBtn);
        mDiscoverBtn.getLocationOnScreen(mLocationsDiscoverBtn);
        mTrafficBtn.getLocationOnScreen(mLocationsTrafficBtn);
        mMoreBtn.getLocationOnScreen(mLlocationsMoreBtn);
        
        if( resumeFirstCalled){
			Animation animation = new TranslateAnimation(mLocations[0][0], mLocations[0][0], 0, 0);
			animation.setFillAfter(true);
			animation.setDuration(0);
			mCursorImv.startAnimation(animation);
			resumeFirstCalled = false;
			
			mMenuBtnList[0].setBackgroundResource(MENU_BACKGROUD_SELECTED[0]);
			for (int i=1, size = mMenuBtnList.length; i<size; i++) {
				mMenuBtnList[i].setBackgroundResource(MENU_BACKGROUD_STILL[i]);
			}
        }
		
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mSphinx.getControlView().setPadding(0, 0, 0, 0);
    }

    protected void findViews() {
        mPOIBtn = (ImageButton)mRootView.findViewById(R.id.poi_btn);
        mDiscoverBtn = (ImageButton)mRootView.findViewById(R.id.discover_btn);
        mDiscvoerImv = (ImageView)mRootView.findViewById(R.id.discover_imv);
        mTrafficBtn = (ImageButton)mRootView.findViewById(R.id.traffic_btn);
        
        mCursorImv = (ImageView)mRootView.findViewById(R.id.btn_cursor);
        
        mMoreBtn = (ImageButton)mRootView.findViewById(R.id.more_btn);
        mUpgradeImv = (ImageView)mRootView.findViewById(R.id.upgrade_imv);
        mMenuBtnList[0] = mPOIBtn;
        mMenuBtnList[1] = mDiscoverBtn;
        mMenuBtnList[2] = mTrafficBtn;
        mMenuBtnList[3] = mMoreBtn;
    }

    public class MenuBtnTouchListener implements OnTouchListener{

    	private int mIndex;
    	
    	private boolean mPresesd = false;
    	
    	public MenuBtnTouchListener(int index){
    		this.mIndex = index;
    	}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			switch (event.getAction()) {
			
				case MotionEvent.ACTION_DOWN:
					mMenuBtnList[mIndex].setBackgroundResource(MENU_BACKGROUD[mIndex]);
					mPresesd = true;
					break;
					
				case MotionEvent.ACTION_MOVE:
					if(mPresesd){
						if( !pointInView(mMenuBtnList[mIndex], event.getX(), event.getY())){
							mMenuBtnList[mIndex].setBackgroundResource(MENU_BACKGROUD_STILL[mIndex]);
							mPresesd=false;
							return false;
						}
					}
					
					break;
					
				case MotionEvent.ACTION_UP:
					mPresesd = false;
					break;
					
				default:
					break;
				}
			
			return false;
		}
    	
    }
    
    final boolean pointInView(ImageButton btn, float localX, float localY) {
        return localX >= 0 && localX < (btn.getRight() - btn.getLeft())
                && localY >= 0 && localY < (btn.getBottom()- btn.getTop());
    }
    
    protected void setListener() {
    	
        mPOIBtn.setOnClickListener(this);
        mDiscoverBtn.setOnClickListener(this);
        mTrafficBtn.setOnClickListener(this);
        mMoreBtn.setOnClickListener(this);

		for (int i=0, size = mMenuBtnList.length; i<size; i++) {
			mMenuBtnList[i].setOnTouchListener(new MenuBtnTouchListener(i));
		}
		
    }
    
    @Override
    public void onClick(View view) {
    	mSphinx.uiStackEmpty();
        switch (view.getId()) {
            case R.id.poi_btn:
            	mActionLog.addAction(ActionLog.MenuSearch);
                mSphinx.showView(R.id.view_home);
                break;
            case R.id.discover_btn:
                mSphinx.showView(R.id.view_discover);
                mSphinx.getDiscoverFragment().setCurrentItem(0);
                if (mDiscvoerImv.getVisibility() == View.VISIBLE) {
                    mDiscvoerImv.setVisibility(View.GONE);
                    mSphinx.showHint(TKConfig.PREFS_HINT_DISCOVER_HOME, R.layout.hint_discover_home);
                }
                break;
            case R.id.traffic_btn:
            	mActionLog.addAction(ActionLog.MenuTraffic);
            	mSphinx.getTrafficQueryFragment().setState(TrafficViewSTT.State.Normal);
                mSphinx.showView(R.id.view_traffic_query);
                break;
            case R.id.more_btn:
            	mActionLog.addAction(ActionLog.MenuMore);
                mSphinx.showView(R.id.view_more);
                break;

            default:
                break;
        }
    }
    
    public void updateMenuStatus(int id) {
    	int oldIndex = 0;
    	for(int i =0,size=mMenuBtnList.length; i<size; i++) {
            if (mMenuBtnList[i].getId() == mMenuIdSelected) {
            	oldIndex=i;
            	break;
            }
        }
        mMenuIdSelected = id;
        
        int i = 0;
        int indexPressed = 0;
        for(ImageButton imageButton : mMenuBtnList) {
            if (imageButton.getId() == mMenuIdSelected) {
            		indexPressed=i;
            } else {
            }
            i++;
        }
        
		Animation animation = new TranslateAnimation(mLocations[oldIndex][0], mLocations[indexPressed][0], 0, 0);
		animation.setFillAfter(true);
		animation.setDuration(350);
		animation.setAnimationListener(mAnimListeners[indexPressed]);
		mCursorImv.startAnimation(animation);
		
    }
    
    private class MenuBtnAnimListener implements AnimationListener{

    	private int indexPressed;
    	
    	public MenuBtnAnimListener(int indexPressed){
    		this.indexPressed = indexPressed;
    	}
    	
		@Override
		public void onAnimationEnd(Animation animation) {
			mMenuBtnList[indexPressed].setBackgroundResource(MENU_BACKGROUD_SELECTED[indexPressed]);
			for(int i=0,size=mMenuBtnList.length;i<size;i++){
				if(i!=indexPressed){
					mMenuBtnList[i].setBackgroundResource(MENU_BACKGROUD_STILL[i]);
				}
			}
		}
		
		@Override
		public void onAnimationStart(Animation animation) {
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			
		}
    	
    }
    
    public void setUpgrade(int visibility) {
        mUpgradeImv.setVisibility(visibility);
    }
    
    public void setDiscover(int visibility) {
        mDiscvoerImv.setVisibility(visibility);
    }
}

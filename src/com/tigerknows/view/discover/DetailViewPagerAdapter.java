package com.tigerknows.view.discover;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;


public class DetailViewPagerAdapter extends PagerAdapter {
    
    public int count = 0;
    public BaseDetailFragment fragment;
    
    public DetailViewPagerAdapter(BaseDetailFragment fragment) {
    	this.fragment = fragment;
    }
    
    @Override
    public int getCount() {
        return count;
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
    public void destroyItem(View container, int position, Object object) {
    	System.out.println("destroyItem: " + position);
    	fragment.reCycleDetailView((BaseDetailView)(object));
    }

    @Override
    public Object instantiateItem(View container, int position) {
    	System.out.println("instantiateItem: " + position);
    	
    	BaseDetailView view = fragment.createDetailViewForAdapter(position);
    	try{
    		((ViewPager)container).addView(view);
    	}catch(Exception e){
    	}
    	view.onResume();
       return view;
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate(View arg0) {
    }

    @Override
    public void finishUpdate(View arg0) {
    }
    
}

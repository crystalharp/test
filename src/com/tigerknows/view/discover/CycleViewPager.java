package com.tigerknows.view.discover;

import com.tigerknows.view.SpringbackListView.IPagerList;
import com.tigerknows.view.SpringbackListView.IPagerListCallBack;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

import java.util.List;

public class CycleViewPager {
    

    public static class CycleOnPageChangeListener implements OnPageChangeListener {
        
        public interface IRefreshViews {
            public void refreshViews(int position);
        }
        
        public IRefreshViews iRefreshViews;
        public IPagerList iPagerList;
        public IPagerListCallBack iPagerListCallBack;
        public int count = 0;
        public boolean isPageTurning = false;
        int time = 0;
        
        public CycleOnPageChangeListener(IRefreshViews iRefreshViews, IPagerListCallBack iPagerListCallBack) {
            this.iRefreshViews = iRefreshViews;
            this.iPagerListCallBack = iPagerListCallBack;
        }
        
        // 当滑动状态改变时调用
        @Override
        public void onPageScrollStateChanged(int position) {
        }

        // 当当前页面被滑动时调用
        @Override
        public void onPageScrolled(int position, float offset, int length) {
            if (position == count-1 && length == 0) {
                time++;
                if (time > 2 && iPagerList != null && iPagerList.canTurnPage(false) && isPageTurning == false) {
                    iPagerList.turnPageStart(false, iPagerListCallBack);
                    isPageTurning = true;
                }
            }
        }

        // 当新的页面被选中时调用
        @Override
        public void onPageSelected(int position) {
        	System.out.println("OnPageSelected: " + position);
            iRefreshViews.refreshViews(position);
            time = 0;
        }
    }
}

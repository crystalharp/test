package com.tigerknows.view.discover;

import com.tigerknows.ActionLog;
import com.tigerknows.view.SpringbackListView.IPagerList;
import com.tigerknows.view.SpringbackListView.IPagerListCallBack;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

import java.util.List;

public class CycleViewPager {
    
    public static class CyclePagerAdapter extends PagerAdapter {
        
        public List<View> viewList;
        public int count = 0;
        
        public CyclePagerAdapter(List<View> viewList) {
            this.viewList = viewList;
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
            // TODO Auto-generated method stub
            return super.getItemPosition(object);
        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            // TODO Auto-generated method stub
            // ((ViewPager) arg0).removeView(list.get(arg1));
        }

        @Override
        public Object instantiateItem(View arg0, int position) {
            // TODO Auto-generated method stub
            try {
                ((ViewPager)arg0).addView(viewList.get(position % viewList.size()));
            } catch (Exception e) {
                // TODO: handle exception
            }
            return viewList.get(position % viewList.size());
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
        Context mContext;
        public CycleOnPageChangeListener(Context context, IRefreshViews iRefreshViews, IPagerListCallBack iPagerListCallBack) {
            this.iRefreshViews = iRefreshViews;
            this.iPagerListCallBack = iPagerListCallBack;
            this.mContext = context;
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
            iRefreshViews.refreshViews(position);
            time = 0;
            ActionLog.getInstance(mContext).addAction(ActionLog.VIEWPAGER_SELECTED, position);
        }
    }
}

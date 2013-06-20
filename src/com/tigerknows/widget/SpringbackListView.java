package com.tigerknows.widget;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SpringbackListView extends ListView {  
    
    public interface IPagerList {
        public boolean canTurnPage(boolean isHeader);
        public void turnPageStart(boolean isHeader, IPagerListCallBack iPagerListCallBack);
    }
    
    public interface IPagerListCallBack {
        public void turnPageEnd(boolean isHeader, int size);
    }

    public final static int NONE = -1;  
    // 松开刷新标志   
    public final static int RELEASE_TO_REFRESH = 0;  
    // 下拉刷新标志   
    public final static int PULL_TO_REFRESH = 1;  
    // 正在刷新标志   
    public final static int REFRESHING = 2;  
    // 刷新完成标志   
    public final static int DONE = 3;  
  
    private View headerView;  

    private View footerView; 

    private TextView footerLoadintTxv; 

    private ProgressBar footerProgressBar; 
  
    // 用于保证startY的值在一个完整的touch事件中只被记录一次   
    private boolean isRecoredHeader = false; 
    private boolean isRecoredFooter = false;   
  
    private int headerContentWidth;  
    private int headerContentHeight;  
  
    private int footerContentWidth;  
    private int footerContentHeight;  
    
    private boolean headerSpringback = false;
    private boolean footerSpringback = false;
  
    private int startY;  
    private int stateHeader = DONE;  
    private int stateFooter = DONE;  
  
    private boolean isBack;  
  
    public OnRefreshListener refreshListener;  
  
    private final static String TAG = "SpringbackListView";  
    
    int MaxSpace = 100;
  
    public SpringbackListView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
        MaxSpace = Globals.g_metrics.heightPixels/2;
    }  
      
    @Override
    public void addHeaderView(View v) {
        headerView = v;
        measureView(headerView);  
  
        headerContentHeight = headerView.getMeasuredHeight();  
        headerContentWidth = headerView.getMeasuredWidth();  
  
        headerView.setPadding(0, 0, 0, 0);  
        headerView.invalidate();  
  
        super.addHeaderView(headerView);
        headerSpringback = true;
    }
    
    @Override
    public void addFooterView(View v) {
        footerView = v;
        measureView(footerView);  
  
        footerContentHeight = footerView.getMeasuredHeight();  
        footerContentWidth = footerView.getMeasuredWidth();  
  
        footerView.setPadding(0, -1 * footerContentHeight, 0, 0);  
        footerView.invalidate();  
  
        super.addFooterView(footerView); 
        footerLoadintTxv = (TextView)footerView.findViewById(R.id.loading_txv);
        footerProgressBar = (ProgressBar)footerView.findViewById(R.id.progress_prb);
        footerSpringback = true;
        footerView.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                if (stateFooter != REFRESHING) {
                    changeHeaderViewByState(false, REFRESHING);
                    onRefresh(false);
                }
            }
        });
    }
  
    public boolean onTouchEvent(MotionEvent event) {  
        switch (event.getAction()) {  
        case MotionEvent.ACTION_DOWN:  
            if ((headerSpringback && getFirstVisiblePosition() == 0) && !isRecoredHeader) {  
                startY = (int) event.getY();  
                isRecoredHeader = true;  
            }
            if ((footerSpringback && getLastVisiblePosition() == getCount()-1) && !isRecoredFooter) {  
                startY = (int) event.getY();  
                isRecoredFooter = true;  
                LogWrapper.d(TAG, "记录按下时的位置:"+event.getY());  
            }  
            break;  
  
        case MotionEvent.ACTION_UP:  
  
            if (headerSpringback && isRecoredHeader) {
                if (stateHeader != REFRESHING) {  
                    if (stateHeader == DONE) {  
                        LogWrapper.d(TAG, "什么都不做");  
                    }  
                    if (stateHeader == PULL_TO_REFRESH) {  
    //                    stateHeader = DONE;  
                        changeHeaderViewByState();  
      
                        LogWrapper.d(TAG, "由下拉刷新状态到刷新完成状态");  
                    }  
                    if (stateHeader == RELEASE_TO_REFRESH) {  
                        stateHeader = REFRESHING;  
                        changeHeaderViewByState();  
                        onRefresh(true);  
      
                        LogWrapper.d(TAG, "由松开刷新状态，到刷新完成状态");  
                    }  
                }  
      
                isRecoredHeader = false;  
            }
            if (footerSpringback && isRecoredFooter) {
                if (stateFooter != REFRESHING) {  
                    if (stateFooter == DONE) {  
                        LogWrapper.d(TAG, "什么都不做");  
                    }  
                    if (stateFooter == PULL_TO_REFRESH) {  
//                        stateFooter = DONE;  
                        changeHeaderViewByState();  
      
                        LogWrapper.d(TAG, "由下拉刷新状态到刷新完成状态");  
                    }  
                    if (stateFooter == RELEASE_TO_REFRESH) {  
                        stateFooter = REFRESHING;  
                        changeHeaderViewByState();  
                        onRefresh(false);  
      
                        LogWrapper.d(TAG, "由松开刷新状态，到刷新完成状态");  
                    }  
                }  
                
                isRecoredFooter = false;
            }
            isBack = false;  
  
            break;  
  
        case MotionEvent.ACTION_MOVE:  
            int tempY = (int) event.getY();
            if ((headerSpringback && getFirstVisiblePosition() == 0) && !isRecoredHeader) {  
                startY = tempY;  
                isRecoredHeader = true;  
            }
            if ((footerSpringback && getLastVisiblePosition() == getCount()-1) && !isRecoredFooter) {  
                startY = tempY;  
                isRecoredFooter = true;  
            }  
            if (headerSpringback && stateHeader != REFRESHING && isRecoredHeader && tempY > 0 && tempY - startY > 0 && tempY - startY < MaxSpace && headerView != null) {
                // 可以松开刷新了   
                if (stateHeader == RELEASE_TO_REFRESH) {  
                    // 往上推，推到屏幕足够掩盖head的程度，但还没有全部掩盖   
                    if ((tempY - startY < headerContentHeight)  
                            && (tempY - startY) > 0) {  
                        stateHeader = PULL_TO_REFRESH;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由松开刷新状态转变到下拉刷新状态");  
                    }  
                    // 一下子推到顶   
                    else if (tempY - startY <= 0) {  
                        stateHeader = DONE;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由松开刷新状态转变到done状态");  
                    }  
                    // 往下拉，或者还没有上推到屏幕顶部掩盖head   
                    else {  
                        // 不用进行特别的操作，只用更新paddingTop的值就行了   
                    }  
                }  
                // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态   
                if (stateHeader == PULL_TO_REFRESH) {  
                    // 下拉到可以进入RELEASE_TO_REFRESH的状态   
                    if (tempY - startY >= headerContentHeight) {  
                        stateHeader = RELEASE_TO_REFRESH;  
                        isBack = true;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由done或者下拉刷新状态转变到松开刷新");  
                    }  
                    // 上推到顶了   
                    else if (tempY - startY <= 0) {  
                        stateHeader = DONE;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由Done或者下拉刷新状态转变到done状态");  
                    }  
                }  
  
                // done状态下   
                if (stateHeader == DONE) {  
                    if (tempY - startY > 0) {  
                        stateHeader = PULL_TO_REFRESH;  
                        changeHeaderViewByState();  
                    }  
                }  
  
                // 更新headView的size   
                if (stateHeader == PULL_TO_REFRESH) {  
                    int temp = tempY - startY - headerContentHeight;
                    headerView.setPadding(0, temp > 0 ? temp : 0, 0, 0);  
                    headerView.invalidate();  
                }  
  
                // 更新headView的paddingTop   
                else if (stateHeader == RELEASE_TO_REFRESH) {  
                    int temp = tempY - startY - headerContentHeight;
                    headerView.setPadding(0, temp > 0 ? temp : 0, 0, 0);  
                    headerView.invalidate();  
                }  
            }
            if (footerSpringback && stateFooter != REFRESHING && isRecoredFooter && tempY > 0 && tempY - startY < 0 && tempY - startY > -MaxSpace && footerView != null){
                
                // 可以松开刷新了   
                if (stateFooter == RELEASE_TO_REFRESH) {  
                    // 往上推，推到屏幕足够掩盖head的程度，但还没有全部掩盖   
                    if ((tempY - startY > -footerContentHeight)  
                            && (tempY - startY) < 0) {  
                        stateFooter = PULL_TO_REFRESH;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由松开刷新状态转变到下拉刷新状态");  
                    }  
                    // 一下子推到顶   
                    else if (tempY - startY >= 0) {  
                        stateFooter = DONE;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由松开刷新状态转变到done状态");  
                    }  
                    // 往下拉，或者还没有上推到屏幕顶部掩盖head   
                    else {  
                        // 不用进行特别的操作，只用更新paddingTop的值就行了   
                    }  
                }  
                // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态   
                if (stateFooter == PULL_TO_REFRESH) {  
                    // 下拉到可以进入RELEASE_TO_REFRESH的状态   
                    if (tempY - startY <= -footerContentHeight) {  
                        stateFooter = RELEASE_TO_REFRESH;  
                        isBack = true;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由done或者下拉刷新状态转变到松开刷新");  
                    }  
                    // 上推到顶了   
                    else if (tempY - startY >= 0) {  
                        stateFooter = DONE;  
                        changeHeaderViewByState();  
  
                        LogWrapper.d(TAG, "由Done或者下拉刷新状态转变到done状态");  
                    }  
                }  
  
                // done状态下   
                if (stateFooter == DONE) {  
                    if (tempY - startY < 0) {  
                        stateFooter = PULL_TO_REFRESH;  
                        changeHeaderViewByState();  
                    }  
                }  
  
                // 更新headView的size   
                if (stateFooter == PULL_TO_REFRESH) {  
                    int temp = -(tempY - startY + footerContentHeight);
                    footerView.setPadding(0, 0, 0, temp > 0 ? temp : 0);  
                    footerView.invalidate();  
                }  
  
                // 更新headView的paddingTop   
                else if (stateFooter == RELEASE_TO_REFRESH) {  
                    int temp = -(tempY - startY + footerContentHeight);
                    footerView.setPadding(0, 0, 0, temp > 0 ? temp : 0);  
                    footerView.invalidate();  
                }  
            }
            break;  
        }  
        return super.onTouchEvent(event);  
    }
    
    public void changeHeaderViewByState(boolean isHeader, int state) {  
        if (isHeader) {
            stateHeader = state;
        } else {
            stateFooter = state;
        }
        changeHeaderViewByState();
    }
  
    // 当状态改变时候，调用该方法，以更新界面   
    private void changeHeaderViewByState() {  
        
        if (headerView != null) {
            switch (stateHeader) {  
                case RELEASE_TO_REFRESH:  
                    break;  
                    
                case PULL_TO_REFRESH:  
                    break;  
                    
                case REFRESHING:  
                    headerView.setPadding(0, 0, 0, 0);  
                    headerView.invalidate();  
                    break;  
                    
                case DONE:  
                    headerView.setPadding(0, -1 * headerContentHeight, 0, 0);  
                    headerView.invalidate();  
                    break;  
                    
                default:
                    headerView.setPadding(0, -1 * footerContentHeight, 0, 0);  
                    headerView.invalidate();  
                    break;
            }  
        }
        if (footerView != null) {
            switch (stateFooter) {  
                case RELEASE_TO_REFRESH:  
                    if (footerLoadintTxv != null && footerProgressBar != null) {
                        footerLoadintTxv.setText(R.string.pull_to_refresh);
                        footerProgressBar.setVisibility(View.INVISIBLE);
                    }
                    footerView.setPadding(0, 0, 0, 0);  
                    footerView.invalidate();  
                    LogWrapper.d(TAG, "当前状态，松开刷新");  
                    break;  
                    
                case PULL_TO_REFRESH:  
                    if (footerLoadintTxv != null && footerProgressBar != null) {
                        footerLoadintTxv.setText(R.string.pull_to_refresh);
                        footerProgressBar.setVisibility(View.INVISIBLE);
                    }
                    footerView.setPadding(0, 0, 0, 0);  
                    footerView.invalidate();  
                    LogWrapper.d(TAG, "当前状态，下拉刷新");  
                    break;  
                    
                case REFRESHING:  
                    if (footerLoadintTxv != null && footerProgressBar != null) {
                        footerLoadintTxv.setText(R.string.loading);
                        footerProgressBar.setVisibility(View.VISIBLE);
                    }
                    footerView.setPadding(0, 0, 0, 0);  
                    footerView.invalidate();  
                    
                    LogWrapper.d(TAG, "当前状态,正在刷新...");  
                    break;  
                    
                case DONE:  
                    footerView.setPadding(0, -1 * footerContentHeight, 0, 0);  
                    footerView.invalidate();  
                    
                    LogWrapper.d(TAG, "当前状态，done");  
                    break;  
                    
                default:
                    footerView.setPadding(0, -1 * footerContentHeight, 0, 0);  
                    footerView.invalidate();  
                    break;
            }
        }  
    }  
  
    public void setOnRefreshListener(OnRefreshListener refreshListener) {  
        this.refreshListener = refreshListener;  
    }  
  
    public interface OnRefreshListener {  
        public void onRefresh(boolean isHeader);  
    }  
  
    public void onRefreshComplete(boolean isHeader) {  
        if (isHeader) {
            stateHeader = DONE;  
        } else {
            stateFooter = DONE;
        }
        changeHeaderViewByState();  
    }  
  
    private void onRefresh(boolean isHeader) {  
        if (refreshListener != null) {  
            refreshListener.onRefresh(isHeader);  
        }  
    }  
  
    // 此处是“估计”headerView or footerView的width以及height   
    private void measureView(View child) {  
        ViewGroup.LayoutParams p = child.getLayoutParams();  
        if (p == null) {  
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,  
                    ViewGroup.LayoutParams.WRAP_CONTENT);  
        }  
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);  
        int lpHeight = p.height;  
        int childHeightSpec;  
        if (lpHeight > 0) {  
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,  
                    MeasureSpec.EXACTLY);  
        } else {  
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,  
                    MeasureSpec.UNSPECIFIED);  
        }  
        child.measure(childWidthSpec, childHeightSpec);  
    }

    public void setHeaderSpringback(boolean headerSpringback) {
        this.headerSpringback = headerSpringback;
    }

    public void setFooterSpringback(boolean footerSpringback) {
        this.footerSpringback = footerSpringback;
        if (footerSpringback) {
            this.stateFooter = PULL_TO_REFRESH;
            changeHeaderViewByState();  
        } else {
            this.stateFooter = DONE;
            changeHeaderViewByState();  
        }
    }

    public boolean isFooterSpringback() {
        return this.footerSpringback;
    }
    
    public int getState(boolean isHeader) {
        if (isHeader) {
            return stateHeader;
        } else {
            return stateFooter;
        }
    }
    
    public View getView(boolean isHeader) {
        if (isHeader)
            return headerView;
        else
            return footerView;
    }
}  

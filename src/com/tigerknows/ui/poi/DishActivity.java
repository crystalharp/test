/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment.LocalMark;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.FileUpload;
import com.tigerknows.model.TKDrawable;
import com.tigerknows.model.DataQuery.DishResponse;
import com.tigerknows.model.DataQuery.DishResponse.Category;
import com.tigerknows.model.DataQuery.DishResponse.DishList;
import com.tigerknows.model.Hotel.HotelTKDrawable;
import com.tigerknows.model.Dish;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.common.AddPictureActivity;
import com.tigerknows.ui.common.ViewImageActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.QueryingView;
import com.tigerknows.widget.RetryView;

/**
 * @author Peng Wenyue
 */
public class DishActivity extends BaseActivity implements View.OnClickListener, RetryView.CallBack {
    
    static final String TAG = "DishActivity";
    
    static final String LocalParameterMode = "LocalParameterMode";
    
    static final String LocalParameterTab = "LocalParameterTab";
    
    public static final String EXTRA_TAB = "EXTRA_TAB";
    
    static final int COLUMN_WIDTH = 148;
    
    private Runnable mLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            mHandler.removeCallbacks(mActualLoadedDrawableRun);
            mHandler.post(mActualLoadedDrawableRun);
        }
    };
    
    private Runnable mActualLoadedDrawableRun = new Runnable() {
        
        @Override
        public void run() {
            if (mMode == 0) {
                if (mTab == 0) {
                    mMyLikeAdapter.notifyDataSetChanged();
                } else {
                    mRecommendAdapter.notifyDataSetChanged();
                }
            } else {
                mSelectedAdapter.notifyDataSetChanged();
            }
        }
    };
    
    private Runnable mHideSelectViewRun = new Runnable() {
        
        @Override
        public void run() {
            mHandler.removeCallbacks(mActualHideSelectViewRun);
            mHandler.post(mActualHideSelectViewRun);
        }
    };
    
    private Runnable mActualHideSelectViewRun = new Runnable() {
        
        @Override
        public void run() {
            if (isFinishing() == false) {
                if (mSelectedView.getTag() == null) {
                    mSelectedView.setBackgroundDrawable(null);
                }
            }
        }
    };
    
    private static POI sPOI = null;
    
    private POI mPOI = null;
    
    private DataQuery mRecommendDataQuery;
    private DataQuery mAllDataQuery;

    private List<Category> mCategoryGroupList = new ArrayList<Category>();
    private List<Category> mCategoryList = new ArrayList<DataQuery.DishResponse.Category>();
    private List<Dish> mAllList = new ArrayList<Dish>();
    private List<Dish> mSelectedList = new ArrayList<Dish>();
    private List<Dish> mRecommedList = new ArrayList<Dish>();
    private List<Dish> mMyLikeList = new ArrayList<Dish>();
    
    private int mMode = 0;
    private int mTab = 0;
    
    protected int mColorNormal;

    protected int mColorSelect;
    
    private CategoryListAdapter mCategoryAdapter;
    private DishAdapter mSelectedAdapter;
    private DishAdapter mMyLikeAdapter;
    private DishAdapter mRecommendAdapter;
    
    public static Comparator<Dish> sLikeDishComparator = new Comparator<Dish>() {

        @Override
        public int compare(Dish dish1, Dish dish2) {
            return (int)(dish2.likeTimeStamp - dish1.likeTimeStamp);
        };
    };
    
    private View mRecommedView;
    private View mAllView;
    private Button mAllBtn;
    private Button mMyLikeBtn;
    private Button mRecommendBtn;

    private View mSelectedView;
    private AnimationListener mSelectedAnimationListener;
    private Animation mLikeAnimation;
    private ListView mCategoryLsv = null;
    private LinearLayout mCategoryGroupView = null;
    private int mGroupPosition = -1;
    private int mChildPosition = -1;
    private int mFromYDelta = 0;
    private int mFirstDishVisibleItem = 0;
    private int mFirstCategoryVisibleItem = 0;
    private int mTotalCateoryItem = 0;
    private ListView mAllLsv = null;
    private ViewPager mRecommendVpg = null;
    private GridView mMyLikeGdv = null;
    private GridView mRecommendGdv = null;

    private QueryingView mQueryingViewLike = null;
    private View mEmptyViewLike = null;
    private RetryView mRetryViewLike;

    private QueryingView mQueryingViewRecommend = null;
    private View mEmptyViewRecommend = null;
    private RetryView mRetryViewRecommend;

    private QueryingView mQueryingViewAll = null;
    private View mEmptyViewAll = null;
    private RetryView mRetryViewAll;
    
    private View mPaddingTopView;
    private View mPaddingBottomView;
    private View mCategoryView;
    private View mCategoryChildView;
    
    private boolean mManuallyChanged = false;
    
    public static void setPOI(POI poi) {
        sPOI = poi;
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.Dish;
        mId = R.id.activity_poi_dish;
        
        Resources resources = getResources();
        mColorNormal = resources.getColor(R.color.black_dark);
        mColorSelect = resources.getColor(R.color.orange);

        setContentView(R.layout.poi_dish);
        
        findViews();
        setListener();

        mSelectedAnimationListener = new AnimationListener() {
            
            @Override
            public void onAnimationStart(Animation animation) {
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onAnimationEnd(Animation animation) {
                mSelectedView.setTag(null);
                mCategoryAdapter.notifyDataSetChanged();
            }
        };
        
        AnimationListener likeAnimationListener = new AnimationListener() {
            
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
            }
        };
        mLikeAnimation = AnimationUtils.loadAnimation(mThis, R.anim.commend);
        mLikeAnimation.setAnimationListener(likeAnimationListener);
        
        mCategoryAdapter = new CategoryListAdapter(mThis, CategoryListAdapter.RESOURCE_ID, mCategoryList);
        mCategoryLsv.setAdapter(mCategoryAdapter);
        
        mSelectedAdapter = new DishAdapter(mThis, DishAdapter.TextView_Resource_ID, mSelectedList);
        mAllLsv.setAdapter(mSelectedAdapter);
        
        mMyLikeAdapter = new DishAdapter(mThis, DishAdapter.Recommend_TextView_Resource_ID, mMyLikeList);
        mMyLikeGdv.setAdapter(mMyLikeAdapter);

        mRecommendAdapter = new DishAdapter(mThis, DishAdapter.Recommend_TextView_Resource_ID, mRecommedList);
        mRecommendGdv.setAdapter(mRecommendAdapter);
        
        mTitleBtn.setText(R.string.recommend_dish);
        mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
        
        mPOI = sPOI;
        
        Intent intent = getIntent();
        mTab = intent.getIntExtra(EXTRA_TAB, 0);
        
        if (mPOI != null) {
            mAllDataQuery = mPOI.getDishQuery();
            mRecommendDataQuery = mPOI.getRecommendDishQuery();
            changeMode(0);
        } else {
            finish();
        }
        
        DataOperation dataOperation = Dish.getLocalMark().makeCommendDataOperation(this, LocalMark.STATE_DRAFT);
        List<BaseQuery> list = new ArrayList<BaseQuery>();
        if (dataOperation != null) {
            list.add(dataOperation);
        }
        dataOperation = Dish.getLocalMark().makeCommendDataOperation(this, LocalMark.STATE_DELETE);
        if (dataOperation != null) {
            list.add(dataOperation);
        }
        
        if (list.size() > 0) {
            queryStart(list);
        }
    }

    protected void findViews() {
        super.findViews();
        mRecommedView = findViewById(R.id.recommend_view);
        mAllView = findViewById(R.id.all_view);
        mAllBtn = (Button) findViewById(R.id.all_btn);

        mMyLikeBtn = (Button) findViewById(R.id.my_like_btn);
        mMyLikeBtn.setBackgroundResource(R.drawable.btn_tab_selected);
        mMyLikeBtn.setTextColor(mColorSelect);
        mRecommendBtn = (Button) findViewById(R.id.recommend_btn);
        
        mSelectedView = findViewById(R.id.selected_view);
        mCategoryGroupView = (LinearLayout) findViewById(R.id.category_group_view);
        mCategoryChildView = findViewById(R.id.category_child_view);
        mCategoryLsv = (ListView)findViewById(R.id.category_lsv);
        mAllLsv = (ListView)findViewById(R.id.all_lsv);
        mAllLsv.setSelector(R.color.transparent);
        
        mRecommendVpg = (ViewPager)findViewById(R.id.view_pager);
        
        int padding = Utility.dip2px(mThis, 8);
        int spacing = Utility.dip2px(mThis, 8);
        int horizontalSpacing = (Globals.g_metrics.widthPixels-(Utility.dip2px(mThis, COLUMN_WIDTH)*2))/3;
        
        mRecommendGdv = new GridView(mThis);
        mRecommendGdv.setNumColumns(GridView.AUTO_FIT);
        mRecommendGdv.setHorizontalSpacing(horizontalSpacing);
        mRecommendGdv.setVerticalSpacing(spacing);
        mRecommendGdv.setColumnWidth(Utility.dip2px(mThis, COLUMN_WIDTH));
        mRecommendGdv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mRecommendGdv.setGravity(Gravity.CENTER);
        mRecommendGdv.setPadding(horizontalSpacing, padding, horizontalSpacing, padding);
        mRecommendGdv.setSelector(R.color.transparent);
        
        mMyLikeGdv = new GridView(mThis);
        mMyLikeGdv.setNumColumns(GridView.AUTO_FIT);
        mMyLikeGdv.setHorizontalSpacing(horizontalSpacing);
        mMyLikeGdv.setVerticalSpacing(spacing);
        mMyLikeGdv.setColumnWidth(Utility.dip2px(mThis, COLUMN_WIDTH));
        mMyLikeGdv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mMyLikeGdv.setGravity(Gravity.CENTER);
        mMyLikeGdv.setPadding(horizontalSpacing, padding, horizontalSpacing, padding);
        mMyLikeGdv.setSelector(R.color.transparent);
        
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
        RelativeLayout likeLayout = new RelativeLayout(mThis);
        likeLayout.addView(mMyLikeGdv, layoutParams);
        mQueryingViewLike = new QueryingView(mThis);
        mQueryingViewLike.setVisibility(View.GONE);
        likeLayout.addView(mQueryingViewLike, layoutParams);
        mEmptyViewLike = mLayoutInflater.inflate(R.layout.empty_view, likeLayout, false);
        mEmptyViewLike.setVisibility(View.GONE);
        likeLayout.addView(mEmptyViewLike, layoutParams);
        TextView emptyTxv = (TextView) mEmptyViewLike.findViewById(R.id.empty_txv);
        ImageView emptyImv = (ImageView) mEmptyViewLike.findViewById(R.id.icon_imv);
        emptyTxv.setText(R.string.like_empty_tip);
        emptyImv.setBackgroundResource(R.drawable.ic_like_empty);
        mRetryViewLike = new RetryView(mThis);
        likeLayout.addView(mRetryViewLike, layoutParams);
        mRetryViewLike.setVisibility(View.GONE);
        mRetryViewLike.setText(R.string.touch_screen_and_retry, true);
        mRetryViewLike.setVisibility(View.GONE);
        
        RelativeLayout recommendLayout = new RelativeLayout(mThis);
        recommendLayout.addView(mRecommendGdv, layoutParams);
        mQueryingViewRecommend = new QueryingView(mThis);
        recommendLayout.addView(mQueryingViewRecommend, layoutParams);
        mQueryingViewRecommend.setVisibility(View.GONE);
        mEmptyViewRecommend = mLayoutInflater.inflate(R.layout.empty_view, likeLayout, false);
        mEmptyViewRecommend.setVisibility(View.GONE);
        recommendLayout.addView(mEmptyViewRecommend, layoutParams);
        emptyTxv = (TextView) mEmptyViewRecommend.findViewById(R.id.empty_txv);
        emptyTxv.setText(R.string.recommend_cook_empty);
        mRetryViewRecommend = new RetryView(mThis);
        recommendLayout.addView(mRetryViewRecommend, layoutParams);
        mRetryViewRecommend.setVisibility(View.GONE);
        mRetryViewRecommend.setText(R.string.touch_screen_and_retry, true);
        mRetryViewRecommend.setVisibility(View.GONE);
        
        List<View> viewList = new ArrayList<View>();
        viewList.add(likeLayout);
        viewList.add(recommendLayout);
        mRecommendVpg.setOnPageChangeListener(new MyPageChangeListener());
        mRecommendVpg.setAdapter(new MyAdapter(viewList));
        
        mQueryingViewAll = (QueryingView)findViewById(R.id.querying_view);
        mQueryingViewAll.setVisibility(View.GONE);
        mEmptyViewAll = findViewById(R.id.empty_view);
        mEmptyViewAll.setVisibility(View.GONE);
        mRetryViewAll = (RetryView) findViewById(R.id.retry_view);
        mRetryViewAll.setVisibility(View.GONE);
        mRetryViewAll.setText(R.string.touch_screen_and_retry, true);

        mPaddingTopView = findViewById(R.id.padding_top_view);
        mPaddingBottomView = findViewById(R.id.padding_bottom_view);
        mCategoryView = findViewById(R.id.category_view);
    }
    
    protected void setListener() {
        super.setListener();
        mTitleBtn.setOnClickListener(this);
        mAllBtn.setOnClickListener(this);
        mMyLikeBtn.setOnClickListener(this);
        mRecommendBtn.setOnClickListener(this);
        mRetryViewLike.setCallBack(this, mActionTag);
        mRetryViewRecommend.setCallBack(this, mActionTag);
        mRetryViewAll.setCallBack(this, mActionTag);
        
        mCategoryLsv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Category category = mCategoryList.get(position);
                mAllLsv.setSelectionFromTop(category.firstDishIndex, 0);
                animationSelectView(position);
            }
        });
        
        mAllLsv.setOnScrollListener(new OnScrollListener() {
            
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                if (mFirstDishVisibleItem != firstVisibleItem) {
                    mFirstDishVisibleItem = firstVisibleItem;
                    Dish dish = mSelectedList.get(mFirstDishVisibleItem);
                    animationSelectView(dish.categoryIndex);
                }
            }
        });
    }
    
    void animationSelectView(int childPosition) {
        mFromYDelta = (mChildPosition < 0 ? 0: mChildPosition)*mCategoryAdapter.childHeight-(mFirstCategoryVisibleItem*mCategoryAdapter.childHeight);
        if (childPosition == mChildPosition) {
            return;
        }
        mChildPosition = childPosition;

        int firstVisiblePosition = mFirstCategoryVisibleItem;
        int lastVisiblePosition = mFirstCategoryVisibleItem+mTotalCateoryItem-1;
        if (mChildPosition <= firstVisiblePosition) {
            mFirstCategoryVisibleItem = mChildPosition;
        } else if (mChildPosition > lastVisiblePosition) {
            mFirstCategoryVisibleItem = mChildPosition-mTotalCateoryItem+1;
        } else {
            mFirstCategoryVisibleItem = firstVisiblePosition;
        }
        mSelectedView.setTag(1);
        mCategoryAdapter.notifyDataSetChanged();
        mCategoryLsv.setSelectionFromTop(mFirstCategoryVisibleItem, 0);
        
        int toYDelta = (mChildPosition)*mCategoryAdapter.childHeight-(mFirstCategoryVisibleItem*mCategoryAdapter.childHeight);
        
        mSelectedView.setBackgroundResource(R.drawable.bg_dish_category_selected);
        
        Animation anim = mSelectedView.getAnimation();
        if (anim != null) {
            anim.reset();
        }
        mSelectedView.setAnimation(null);

        if (mFromYDelta + mCategoryAdapter.childHeight> mCategoryAdapter.categoryChildListViewHeight) {
            mFromYDelta = mCategoryAdapter.categoryChildListViewHeight - mCategoryAdapter.childHeight;
        }
        if (toYDelta+mCategoryAdapter.childHeight > mCategoryAdapter.categoryChildListViewHeight) {
            toYDelta = mCategoryAdapter.categoryChildListViewHeight - mCategoryAdapter.childHeight;
        }
        
        if (mFirstCategoryVisibleItem+mTotalCateoryItem == mCategoryList.size() && mCategoryList.size() > mTotalCateoryItem) {
            mFromYDelta += mCategoryAdapter.childPaddingBottom;
            toYDelta += mCategoryAdapter.childPaddingBottom;
        }
        
        if (mFromYDelta < 0) {
            mFromYDelta = 0;
        }
        
        if (toYDelta < 0) {
            toYDelta = 0;
        }
        anim = new TranslateAnimation(0, 0, mFromYDelta, toYDelta);
        anim.setDuration(300);
        anim.setFillAfter(true);
        anim.setAnimationListener(mSelectedAnimationListener);
        mSelectedView.startAnimation(anim);
    }
    
    protected void onResume() {
        super.onResume();
        if (isReLogin()) {
            return;
        }
    }
    
    private class DishAdapter extends ArrayAdapter<Dish> {
        static final int TextView_Resource_ID = R.layout.poi_dish_list_item;
        
        static final int Recommend_TextView_Resource_ID = R.layout.poi_dish_recommend_list_item;
        
        int textViewResourceId;

        public DishAdapter(Context context, int textViewResourceId, List<Dish> dishList) {
            super(context, textViewResourceId, dishList);
            this.textViewResourceId = textViewResourceId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(this.textViewResourceId, parent, false);
            } else {
                view = convertView;
            }

            TextView nameTxv = (TextView) view.findViewById(R.id.name_txv);
            TextView pictureCountTxv = (TextView) view.findViewById(R.id.picture_count_txv);
            TextView priceTxv = (TextView) view.findViewById(R.id.price_txv);
            View commendView = view.findViewById(R.id.commend_view);
            TextView commendTxv = (TextView)view.findViewById(R.id.commend_txv);
            ImageView commendImv = (ImageView)view.findViewById(R.id.commend_imv);
            ImageView pictureImv = (ImageView)view.findViewById(R.id.picture_imv);

            Dish data = getItem(position);
            commendView.setTag(R.id.commend_view, data);
            commendView.setTag(R.id.commend_imv, commendImv);
            commendView.setTag(R.id.commend_txv, commendTxv);
            commendView.setTag(R.id.index, position);
            commendView.setOnClickListener(DishActivity.this);
            commendView.setClickable(true);
            commendImv.setAnimation(null);
            pictureImv.setTag(R.id.picture_imv, data);
            pictureImv.setOnClickListener(DishActivity.this);
            
            HotelTKDrawable hotelTKDrawable;

            if (Recommend_TextView_Resource_ID == textViewResourceId) {
                hotelTKDrawable = data.getPictureRecommend();
            } else {
                hotelTKDrawable = data.getPictureAll();
            }
            
            TKDrawable tkDrawable = null;
            Drawable drawable = null;
            if (hotelTKDrawable != null) {
                tkDrawable = hotelTKDrawable.getTKDrawable();
                if (tkDrawable != null) {
                    drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, DishActivity.this.toString());
                    if(drawable != null) {
                        pictureImv.setImageDrawable(drawable);
                    } else {
                        pictureImv.setImageDrawable(null);
                    }
                }
            }
            
            if (tkDrawable == null) {
                drawable = getResources().getDrawable(R.drawable.btn_add_picture);
                pictureImv.setImageDrawable(drawable);
            } else if (drawable == null){
                drawable = getResources().getDrawable(R.drawable.bg_picture_detail);
                pictureImv.setImageDrawable(drawable);
            }
            
            long likes = data.getHitCount();
            String likesStr = String.valueOf(likes);
            if (data.isLike()) {
//                commendTxv.setTextColor(TKConfig.COLOR_ORANGE);
                commendImv.setImageResource(R.drawable.ic_like);
            } else {
//                commendTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                commendImv.setImageResource(R.drawable.ic_like_cancel);
            }
            commendTxv.setText(likesStr);
            
            pictureCountTxv.setVisibility(View.INVISIBLE);
//            pictureCountTxv.setText(getString(R.string.pictures, data.getPictureCount()));
            nameTxv.setText(data.getName());
            priceTxv.setText(data.getPrice());
            
            if (Recommend_TextView_Resource_ID == textViewResourceId) {
                int newWidth = (int) (Globals.g_metrics.density*COLUMN_WIDTH);
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.width = newWidth;
            } else {
            
                if (position == getCount() -1) {
                    view.setPadding(0, 0, 0, mCategoryAdapter.totalHeight-(mCategoryAdapter.childHeight*2));
                } else {
                    view.setPadding(0, 0, 0, 0);
                }
            }
        
            return view;
        }
    }
    
    public void finish() {
        sPOI = null;
        super.finish();        
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);

        BaseQuery baseQuery = (tkAsyncTask.getBaseQuery());
        if (baseQuery instanceof DataOperation) {
            return;
        }
        
        DataQuery dataQuery = (DataQuery)(tkAsyncTask.getBaseQuery());

        int mode = Integer.parseInt(dataQuery.getLocalParameter(LocalParameterMode));
        int tab = Integer.parseInt(dataQuery.getLocalParameter(LocalParameterTab));

        View mRetryView;
        View mQueryingView;
        View mEmptyView;
        
        if (mode == 0) {
            if (tab == 0) {
                mRetryView = mRetryViewLike;
                mQueryingView = mQueryingViewLike;
                mEmptyView = mEmptyViewLike;
            } else {
                mRetryView = mRetryViewRecommend;
                mQueryingView = mQueryingViewRecommend;
                mEmptyView = mEmptyViewRecommend;
            }
        } else {
            mRetryView = mRetryViewAll;
            mQueryingView = mQueryingViewAll;
            mEmptyView = mEmptyViewAll;
        }
        
        mQueryingView.setVisibility(View.GONE);
        if (BaseActivity.checkReLogin(dataQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mThis, null, false, mThis, false)) {
            
            mEmptyView.setVisibility(View.GONE);
            mRetryView.setVisibility(View.VISIBLE);
        } else {      
            setDataQuery(dataQuery, mode, tab);
            
            refreshData(mode, tab, false);
        }
    }
    
    void setDataQuery(DataQuery dataQuery, int mode, int tab) {

        if (mode == 0 && tab == 1) {
            mRecommendDataQuery = dataQuery;
            mPOI.setRecommendDishQuery(dataQuery);
        } else {
            mAllDataQuery = dataQuery;
            mPOI.setDishQuery(dataQuery);
        }
            
        DishResponse dishResponse = (DishResponse) dataQuery.getResponse();
        DishList dishList = dishResponse.getList();
        if (dishList != null) {
            List<Dish> dishes = dishList.getDishList();
            if (dishes != null && dishes.size() > 0) {

                LocalMark localMark = Dish.getLocalMark();
                for(int i = 0, size = dishes.size(); i < size; i++) {
                    Dish dish = dishes.get(i);
                    dish.likeTimeStamp = 0;
                    long draft = localMark.findCommend(mThis, String.valueOf(dish.getDishId()), LocalMark.STATE_DRAFT);
                    if (draft > 0) {
                        dish.likeTimeStamp = draft;
                    } else {
                        long sent = localMark.findCommend(mThis, String.valueOf(dish.getDishId()), LocalMark.STATE_SENT);
                        if (sent> 0) {
                            dish.likeTimeStamp = sent;
                        }
                    }
                    if (dish.isLike()) {
                        if (dish.likeTimeStamp <= 0) {
                            dish.deleteLike();
                        }
                    } else {
                        if (dish.likeTimeStamp > 0) {
                            dish.addLike(false);
                        }
                    }
                }
                
                if (mode == 0) {
                    if (tab == 0) {
                        
                        mMyLikeList.clear();
                        for(int i = 0, size = dishes.size(); i < size; i++) {
                            Dish dish = dishes.get(i);
                            if (dish.isLike()) {
                                if (mMyLikeList.contains(dish) == false) {
                                    mMyLikeList.add(dish);
                                }
                            }
                        }
                        
                        Collections.sort(mMyLikeList, sLikeDishComparator);
                        
                        mMyLikeAdapter.notifyDataSetChanged();
                        LogWrapper.d(TAG, "likedish.size()"+mMyLikeList.size());
                        
                    } else {
                        LogWrapper.d(TAG, "recommenddish.size()"+dishes.size());
                        
                        mRecommedList.clear();
                        mRecommedList.addAll(dishes);
                        mRecommendAdapter.notifyDataSetChanged();
                    }
                } else {
                    LogWrapper.d(TAG, "alldish.size()"+dishes.size());
                    
                    mAllList.clear();
                    mAllList.addAll(dishes);
                    
                    mCategoryChildView.setBackgroundColor(getResources().getColor(R.color.white));
                    List<Category> categories = dishList.getCategoryList();
                    if (categories != null) {
                        mCategoryGroupList.clear();
                        mCategoryGroupList.addAll(categories);
                        mCategoryAdapter.measure();
                        mCategoryGroupView.removeAllViews();
                        for(int i = 0, size = mCategoryGroupList.size(); i < size; i++) {
                            View view = getLayoutInflater().inflate(CategoryListAdapter.RESOURCE_ID, mCategoryGroupView, false);
                            view.setBackgroundResource(R.drawable.bg_dish_category);
                            view.setTag(i);
                            view.setOnClickListener(this);
                            Category data = (Category) mCategoryGroupList.get(i);
                            TextView textView = (TextView) view.findViewById(R.id.text_txv);
                            textView.setText(data.getName());
                            mCategoryGroupView.addView(view, new LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT, Utility.dip2px(mThis, 48)));
                        }
                        mGroupPosition = -1;
                        mChildPosition = -1;
                        refreshCategory(0);
                    }
                    
                }

            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.title_btn) {
            mActionLog.addAction(mActionTag + ActionLog.DishRecomend);
            mManuallyChanged = true;
            changeMode(0);
        } else if (id == R.id.all_btn) {
            mActionLog.addAction(mActionTag + ActionLog.DishAll);
            mManuallyChanged = true;
            changeMode(1);
        } else if (id == R.id.my_like_btn) {
            mActionLog.addAction(mActionTag + ActionLog.DishMyLike);
            mRecommendVpg.setCurrentItem(0);
        } else if (id == R.id.recommend_btn) {
            mActionLog.addAction(mActionTag + ActionLog.DishTigerRecommend);
            mRecommendVpg.setCurrentItem(1);
        } else if (id == R.id.commend_view) {
            v.setClickable(false);
            Dish data = (Dish) v.getTag(R.id.commend_view);
            int position = (Integer) v.getTag(R.id.index);
            ImageView commendImv = (ImageView) v.getTag(R.id.commend_imv);
            TextView commendTxv = (TextView) v.getTag(R.id.commend_txv);
            final boolean isLike = !data.isLike();
            if (isLike) {
                data.addLike(true);
                mActionLog.addAction(mActionTag + ActionLog.DishLike);
            } else {
                data.deleteLike();
                mActionLog.addAction(mActionTag + ActionLog.DishCancelLike);
            }
            final long dishId = data.getDishId();
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    String uuid = String.valueOf(dishId);
                    if (isLike) {
                        Dish.getLocalMark().addCommend(mThis, uuid, LocalMark.STATE_DRAFT);
                        Dish.getLocalMark().addCommend(mThis, uuid, LocalMark.STATE_SENT);
                        
                        DataOperation dataOperation = Dish.getLocalMark().makeCommendDataOperationByUUID(mThis, uuid, true);
                        if (dataOperation != null) {
                            dataOperation.query();
                        }
                    } else {
                        String[] uuids = new String[]{uuid};
                        Dish.getLocalMark().deleteCommend(mThis, uuids, LocalMark.STATE_DRAFT);
                        Dish.getLocalMark().deleteCommend(mThis, uuids, LocalMark.STATE_SENT);
                        Dish.getLocalMark().addCommend(mThis, uuid, LocalMark.STATE_DELETE);
                        
                        DataOperation dataOperation = Dish.getLocalMark().makeCommendDataOperationByUUID(mThis, uuid, false);
                        if (dataOperation != null) {
                            dataOperation.query();
                        }
                    }
                }
            }).start();

            for(int i = mAllList.size()-1; i >= 0; i--) {
                Dish c = mAllList.get(i);
                if (dishId == c.getDishId()) {
                    if (isLike) {
                        c.addLike(true);
                        if (mMyLikeList.contains(c) == false) {
                            mMyLikeList.add(c);
                        }
                    } else {
                        c.deleteLike();
                    }
                    break;
                }
            }
            for(int i = mRecommedList.size()-1; i >= 0; i--) {
                Dish c = mRecommedList.get(i);
                if (dishId == c.getDishId()) {
                    if (isLike) {
                        c.addLike(true);
                        if (mMyLikeList.contains(c) == false) {
                            mMyLikeList.add(c);
                        }
                    } else {
                        c.deleteLike();
                    }
                    break;
                }
            }
            if (isLike == false) {
                for(int i = mMyLikeList.size()-1; i >= 0; i--) {
                    Dish c = mMyLikeList.get(i);
                    if (dishId == c.getDishId()) {
                        c.deleteLike();
                        mMyLikeList.remove(i);
                        break;
                    }
                }
            }
            
            Collections.sort(mMyLikeList, sLikeDishComparator);
            
            v.setBackgroundResource(R.drawable.btn_subway_busstop_normal);
//            commendTxv.setTextColor(TKConfig.COLOR_ORANGE);
            commendTxv.setText(String.valueOf(data.getHitCount()));
            if (isLike) {
                commendImv.setImageResource(R.drawable.ic_like_cancel);
//                mLikeAnimation.reset();
//                commendImv.startAnimation(mLikeAnimation);
                mHandler.post(mLoadedDrawableRun);
            } else {
                commendImv.setImageResource(R.drawable.ic_like);
                mHandler.post(mLoadedDrawableRun);
            }
            
            if (mMyLikeList.size() == 0 && mMode == 0 && mTab == 0) {
                mEmptyViewLike.setVisibility(View.VISIBLE);
            }
        } else if (id == R.id.picture_imv) {
            Dish data = (Dish) v.getTag(R.id.picture_imv);
            if (data.getPicture() == null) {
                mActionLog.addAction(mActionTag + ActionLog.DishAddImage);
                Intent intent = new Intent();
                intent.putExtra(FileUpload.SERVER_PARAMETER_REF_DATA_TYPE, BaseQuery.DATA_TYPE_DISH);
                intent.putExtra(FileUpload.SERVER_PARAMETER_REF_ID, String.valueOf(data.getDishId()));
                intent.putExtra(AddPictureActivity.EXTRA_SUCCESS_TIP, getString(R.string.add_picture_success_for_dish));
                Utility.showTakePhotoDialog(mActionTag, mThis, intent);
                return;
            }
            viewImage(data);
        } else {
            int groupPosition = (Integer) v.getTag();
            refreshCategory(groupPosition);
        }
    }
    
    void changeMode(int mode) {
        mMode = mode;
        int tab = mTab;
        if (mMode == 0) {
            this.mRecommedView.setVisibility(View.VISIBLE);
            this.mAllView.setVisibility(View.INVISIBLE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
            mAllBtn.setBackgroundResource(R.drawable.btn_hot_comment);
            
            if (mRecommendVpg.getCurrentItem() != tab) {
                mRecommendVpg.setCurrentItem(tab);
            } else {
                if (tab == 0) {
                    mMyLikeAdapter.notifyDataSetChanged();
                } else {
                    mRecommendAdapter.notifyDataSetChanged();
                }
            }
        } else {
            this.mRecommedView.setVisibility(View.INVISIBLE);
            this.mAllView.setVisibility(View.VISIBLE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment);
            mAllBtn.setBackgroundResource(R.drawable.btn_hot_comment_focused);
            
            mSelectedAdapter.notifyDataSetChanged();
        }

        refreshData(mode, tab);
    }

    void refreshData(int mode, int tab) {
        refreshData(mode, tab, true);
    }
    
    void refreshData(int mode, int tab, boolean setDataQuery) {

        List<Dish> dataList;
        DataQuery dataQuery;
        if (mode == 0) {
            if (tab == 0) {
                dataQuery = mAllDataQuery;
                dataList = mMyLikeList;
            } else {
                dataQuery = mRecommendDataQuery;
                dataList = mRecommedList;
            }
        } else {
            dataQuery = mAllDataQuery;
            dataList = mAllList;
        }
        
        if (dataQuery == null) {
            queryDish(mode, tab);
            return;
        }
        
        if (dataList.size() <= 0 && setDataQuery) {
            setDataQuery(dataQuery, mode, tab);
        }

        View mRetryView;
        View mQueryingView;
        View mEmptyView;
        
        if (mode == 0) {
            if (tab == 0) {
                mRetryView = mRetryViewLike;
                mQueryingView = mQueryingViewLike;
                mEmptyView = mEmptyViewLike;
            } else {
                mRetryView = mRetryViewRecommend;
                mQueryingView = mQueryingViewRecommend;
                mEmptyView = mEmptyViewRecommend;
            }
        } else {
            mRetryView = mRetryViewAll;
            mQueryingView = mQueryingViewAll;
            mEmptyView = mEmptyViewAll;
        }
        
        if (dataList.size() <= 0) {
            if (mode == 0) {
                if (tab == 0) {
                    if (mManuallyChanged == false && mode == mMode && mTab == tab) {
                        mRecommendVpg.setCurrentItem(1);
                    }
                }
            }
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
        mQueryingView.setVisibility(View.GONE);
        mRetryView.setVisibility(View.GONE);
    }
    
    void refreshCategory(int groupPosition) {
        if (mGroupPosition != groupPosition) {
            mGroupPosition = groupPosition;
            
            int size = mCategoryGroupList.size();
            for(int i = 0; i < size; i++) {
                View view = mCategoryGroupView.getChildAt(i);
                TextView textView = (TextView) view.findViewById(R.id.text_txv);
                if (i == mGroupPosition+1) {
                    int topMargin = mCategoryAdapter.totalHeight;
                    topMargin -= ((mCategoryGroupList.size())*mCategoryAdapter.groupHeight);
                    ((LinearLayout.LayoutParams) view.getLayoutParams()).topMargin = topMargin;
                    textView.setTextColor(mCategoryAdapter.colorGroupNormal);
                    mCategoryAdapter.drawable.setBounds(0, 0, mCategoryAdapter.drawable.getIntrinsicWidth(), mCategoryAdapter.drawable.getIntrinsicHeight());
                    textView.setCompoundDrawables(null, null, mCategoryAdapter.drawable, null);
                } else {
                    if (i == mGroupPosition) {
                        textView.setTextColor(mCategoryAdapter.colorGroupFocused);
                        textView.setCompoundDrawables(null, null, null, null);
                    } else {
                        textView.setTextColor(mCategoryAdapter.colorGroupNormal);
                        mCategoryAdapter.drawable.setBounds(0, 0, mCategoryAdapter.drawable.getIntrinsicWidth(), mCategoryAdapter.drawable.getIntrinsicHeight());
                        textView.setCompoundDrawables(null, null, mCategoryAdapter.drawable, null);
                    }
                    ((LinearLayout.LayoutParams) view.getLayoutParams()).topMargin = 0;
                }
            }
            
            int top = (mGroupPosition+1)*mCategoryAdapter.groupHeight;
            int bottom = (size-mGroupPosition-1)*mCategoryAdapter.groupHeight;
            mPaddingTopView.getLayoutParams().height = top;
            mPaddingBottomView.getLayoutParams().height = bottom;
            mCategoryList.clear();
            mCategoryList.addAll(mCategoryGroupList.get(mGroupPosition).getChildList());
            
            size = mAllList.size();
            for(int i = size - 1; i >= 0 ; i--) {
                Dish dish = mAllList.get(i);
                dish.categoryIndex = -1;
            }
            
            mSelectedList.clear();
            size = mCategoryList.size();
            for(int i = 0; i < size; i++) {
                Category category = mCategoryList.get(i);
                List<Long> idList = category.getDishList();
                category.firstDishIndex = -1;
                for (int n = 0, total = idList.size(); n < total; n++) {
                    long id = idList.get(n);
                    for(int j = 0, count = mAllList.size(); j < count; j++) {
                        Dish dish = mAllList.get(j);
                        if (dish.categoryIndex == -1 && dish.getDishId() == id) {
                            mSelectedList.add(dish);
                            if (category.firstDishIndex == -1) {
                                category.firstDishIndex = mSelectedList.size()-1;
                            }
                            dish.categoryIndex = i;
                            break;
                        }
                    }
                }
            }
            for(int i = size - 1; i >= 0 ; i--) {
                Category category = mCategoryList.get(i);
                if (category.firstDishIndex == -1) {
                    mCategoryList.remove(i);
                    for(int j = 0, count = mSelectedList.size(); j < count; j++) {
                        Dish dish = mSelectedList.get(j);
                        if (dish.categoryIndex >= i) {
                            dish.categoryIndex--;
                        }
                    }
                }
            }
            
            mCategoryAdapter.notifyDataSetChanged();
            mCategoryLsv.setSelectionFromTop(0, 0);
            mFirstCategoryVisibleItem = 0;
            
            MotionEvent me = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            mAllLsv.onTouchEvent(me);
            me = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            mAllLsv.onTouchEvent(me);
            mSelectedAdapter.notifyDataSetChanged();
            mAllLsv.setSelectionFromTop(0, 0);
            
            int h = mCategoryAdapter.totalHeight - mCategoryGroupList.size()*mCategoryAdapter.groupHeight;
            mTotalCateoryItem = h/mCategoryAdapter.childHeight;
            mCategoryAdapter.childPaddingBottom = h % mCategoryAdapter.childHeight;
        }
        
        mChildPosition = -1;
        animationSelectView(0);
    }
    
    void changeTab(int position) {

        mTab = position;
        if (mTab == 0) {
            mMyLikeBtn.setBackgroundResource(R.drawable.btn_tab_selected);
            mMyLikeBtn.setTextColor(mColorSelect);
            mRecommendBtn.setBackgroundResource(R.drawable.btn_tab);
            mRecommendBtn.setTextColor(mColorNormal);
            mMyLikeAdapter.notifyDataSetChanged();
        } else {
            mMyLikeBtn.setBackgroundResource(R.drawable.btn_tab);
            mMyLikeBtn.setTextColor(mColorNormal);
            mRecommendBtn.setBackgroundResource(R.drawable.btn_tab_selected);
            mRecommendBtn.setTextColor(mColorSelect);
            mRecommendAdapter.notifyDataSetChanged();
        }
        refreshData(mMode, mTab);
    }

    @Override
    public void retry() {
        queryDish(mMode, mTab);
    }
    
    public void queryDish(int mode, int tab) {
        
        DataQuery dataQuery = new DataQuery(mThis);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DISH);
        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
        dataQuery.addLocalParameter(LocalParameterMode, String.valueOf(mode));
        dataQuery.addLocalParameter(LocalParameterTab, String.valueOf(tab));
        if (mode == 0 && tab == 1) {
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_RECOMMEND_DISH);
        }
        dataQuery.setup(mId, mId, null, false, false, mPOI);
        queryStart(dataQuery);

        View mRetryView;
        View mQueryingView;
        
        if (mode == 0) {
            if (tab == 0) {
                mRetryView = mRetryViewLike;
                mQueryingView = mQueryingViewLike;
            } else {
                mRetryView = mRetryViewRecommend;
                mQueryingView = mQueryingViewRecommend;
            }
        } else {
            mRetryView = mRetryViewAll;
            mQueryingView = mQueryingViewAll;
        }
        
        mRetryView.setVisibility(View.GONE);
        mQueryingView.setVisibility(View.VISIBLE);
    }
    
    class MyPageChangeListener implements OnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onPageSelected(int position) {
            mManuallyChanged = true;
            changeTab(position);
        }
        
    }
    
    public static class MyAdapter extends PagerAdapter {
        
        List<View> mViewList;
        
        public MyAdapter(List<View> viewList) {
            mViewList = viewList;
        }
        
        @Override
        public int getCount() {
            return mViewList.size();
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
             ((ViewPager) contain).removeView(mViewList.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup contain, int position) {
            contain.addView(mViewList.get(position), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            return mViewList.get(position);
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
    
    public class CategoryListAdapter extends ArrayAdapter<Category> {
        
        public CategoryListAdapter(Context context, int textViewResourceId, List<Category> objects) {
            super(context, textViewResourceId, objects);
            
            Resources resources = getResources();
            colorGroupNormal = resources.getColor(R.color.black);
            colorGroupFocused = resources.getColor(R.color.orange);
            drawable = resources.getDrawable(R.drawable.ic_triangle_down);
        }

        static final int RESOURCE_ID = R.layout.poi_dish_category_list_item;

        int totalHeight = 0;
        int groupHeight = 0;
        int childHeight = 0;
        int childPaddingBottom = 0;
        int categoryChildListViewHeight = 0;
        int padding;

        int colorGroupNormal;
        int colorGroupFocused;
        Drawable drawable;
        
        void measure() {
            if (totalHeight == 0) {
                totalHeight = mCategoryView.getBottom()-mCategoryView.getTop();
                View view = getLayoutInflater().inflate(RESOURCE_ID, mCategoryLsv, false);
                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                categoryChildListViewHeight = mCategoryChildView.getBottom()-mCategoryChildView.getTop();
                childHeight = view.getMeasuredHeight();
                groupHeight = childHeight;
                padding = Utility.dip2px(mThis, 12);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view;
            if (convertView == null) {
                view = getLayoutInflater().inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            
            Category data = getItem(position);
            TextView textView = (TextView) view.findViewById(R.id.text_txv);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            textView.setSingleLine(true);
            textView.setText(data.getName());
            if (position == mChildPosition && mSelectedView.getTag() == null) {
                textView.setBackgroundResource(R.drawable.bg_dish_category_selected);
                mHandler.postDelayed(mHideSelectViewRun, 100);
            } else {
                textView.setBackgroundColor(0x00000000);
            }
            textView.setPadding(padding, 0, 0, 0);

            return view;
        }

    }
    
    void viewImage(Dish dish) {
        if (dish == null) {
            return;
        }
        mActionLog.addAction(mActionTag + ActionLog.DishViewImage);
        Intent intent = new Intent();
        intent.setClass(mThis, ViewImageActivity.class);
        intent.putExtra(ViewImageActivity.EXTRA_TITLE, dish.getName());
        intent.putExtra(BaseQuery.SERVER_PARAMETER_REF_DATA_TYPE, BaseQuery.DATA_TYPE_DISH);
        intent.putExtra(BaseQuery.SERVER_PARAMETER_REF_ID, String.valueOf(dish.getDishId()));
        intent.putExtra(ViewImageActivity.EXTRA_CAN_ADD, true);
        intent.putExtra(ViewImageActivity.EXTRA_IMAGE, dish.getPicture());
        intent.putExtra(AddPictureActivity.EXTRA_SUCCESS_TIP, getString(R.string.add_picture_success_for_dish));
        startActivity(intent);
    }
}

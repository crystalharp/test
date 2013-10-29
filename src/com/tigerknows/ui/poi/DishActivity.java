/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
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
            if (isFinishing() == false) {
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

    private QueryingView mQueryingView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private ImageView mEmptyImv = null;
    
    private RetryView mRetryView;
    
    private View mPaddingTopView;
    private View mPaddingBottomView;
    private View mCategoryView;
    
    private boolean mManuallyChanged = false;
    
    public static void setPOI(POI poi) {
        sPOI = poi;
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.Dish;
        mId = R.id.activity_poi_dish;

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
                mSelectedView.setBackgroundDrawable(null);
                mCategoryAdapter.notifyDataSetChanged();
            }
        };
        mRetryView.setText(R.string.touch_screen_and_retry, true);
        
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
                mHandler.post(mLoadedDrawableRun);
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
        mRightBtn.setVisibility(View.GONE);
        
        Resources resources = getResources();
        mColorNormal = resources.getColor(R.color.black_dark);
        mColorSelect = resources.getColor(R.color.orange);
        
        mPOI = sPOI;
        mFromYDelta = Utility.dip2px(mThis, 48);
        
        Intent intent = getIntent();
        mTab = intent.getIntExtra(EXTRA_TAB, 0);
        
        if (mPOI != null) {
            mAllDataQuery = mPOI.getDishQuery();
            mRecommendDataQuery = mPOI.getRecommendDishQuery();
            changeMode(0);
        } else {
            finish();
        }
        
        DataOperation dataOperation = Dish.getLocalMark().makeCommendDataOperationByDraft(this);
        if (dataOperation != null) {
            queryStart(dataOperation);
        }
    }

    protected void findViews() {
        super.findViews();
        mRecommedView = findViewById(R.id.recommend_view);
        mAllView = findViewById(R.id.all_view);
        mAllBtn = (Button) findViewById(R.id.all_btn);

        mMyLikeBtn = (Button) findViewById(R.id.my_like_btn);
        mRecommendBtn = (Button) findViewById(R.id.recommend_btn);
        
        mSelectedView = findViewById(R.id.selected_view);
        mCategoryGroupView = (LinearLayout) findViewById(R.id.category_group_view);
        mCategoryLsv = (ListView)findViewById(R.id.category_lsv);
        mAllLsv = (ListView)findViewById(R.id.all_lsv);
        mAllLsv.setSelector(R.color.transparent);
        
        mRecommendVpg = (ViewPager)findViewById(R.id.view_pager);
        
        int padding = Utility.dip2px(mThis, 8);
        int spacing = Utility.dip2px(mThis, 8);
        
        mRecommendGdv = new GridView(mThis);
        mRecommendGdv.setNumColumns(GridView.AUTO_FIT);
        mRecommendGdv.setHorizontalSpacing(spacing);
        mRecommendGdv.setVerticalSpacing(spacing);
        mRecommendGdv.setColumnWidth(Utility.dip2px(mThis, COLUMN_WIDTH));
        mRecommendGdv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mRecommendGdv.setGravity(Gravity.CENTER);
        mRecommendGdv.setPadding(padding, padding, padding, padding);
        mRecommendGdv.setSelector(R.color.transparent);
        
        mMyLikeGdv = new GridView(mThis);
        mMyLikeGdv.setNumColumns(GridView.AUTO_FIT);
        mMyLikeGdv.setHorizontalSpacing(spacing);
        mMyLikeGdv.setVerticalSpacing(spacing);
        mMyLikeGdv.setColumnWidth(Utility.dip2px(mThis, COLUMN_WIDTH));
        mMyLikeGdv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mMyLikeGdv.setGravity(Gravity.CENTER);
        mMyLikeGdv.setPadding(padding, padding, padding, padding);
        mMyLikeGdv.setSelector(R.color.transparent);
        
        List<View> viewList = new ArrayList<View>();
        viewList.add(mMyLikeGdv);
        viewList.add(mRecommendGdv);
        mRecommendVpg.setOnPageChangeListener(new MyPageChangeListener());
        mRecommendVpg.setAdapter(new MyAdapter(viewList));
        
        mQueryingView = (QueryingView)findViewById(R.id.querying_view);
        mEmptyView = findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mEmptyImv = (ImageView) mEmptyView.findViewById(R.id.icon_imv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) findViewById(R.id.retry_view);

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
        mRetryView.setCallBack(this, mActionTag);
        
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
        if (childPosition == mChildPosition) {
            return;
        }
        
        mChildPosition = childPosition;

        int firstVisiblePosition = mCategoryLsv.getFirstVisiblePosition();
        int lastVisiblePosition = mCategoryLsv.getLastVisiblePosition();
        if (mChildPosition <= firstVisiblePosition) {
            mSelectedView.setBackgroundDrawable(null);
            mCategoryAdapter.notifyDataSetChanged();
            mFirstCategoryVisibleItem = mChildPosition;
            mCategoryLsv.setSelectionFromTop(mFirstCategoryVisibleItem, 0);
            return;
        } else if (mChildPosition >= lastVisiblePosition) {
            mSelectedView.setBackgroundDrawable(null);
            mCategoryAdapter.notifyDataSetChanged();
            mFirstCategoryVisibleItem = mChildPosition-mTotalCateoryItem+1;
            mCategoryLsv.setSelectionFromTop(mFirstCategoryVisibleItem, 0);
            return;
        } else {
            mFirstCategoryVisibleItem = firstVisiblePosition;
        }
        
        int toYDelta = (mGroupPosition+1)*mCategoryAdapter.groupHeight+mChildPosition*mCategoryAdapter.childHeight-(mFirstCategoryVisibleItem*mCategoryAdapter.childHeight);
        
        mSelectedView.setBackgroundResource(R.drawable.bg_dish_category_selected);
        mCategoryAdapter.notifyDataSetChanged();
        
        Animation anim = mSelectedView.getAnimation();
        if (anim != null) {
            anim.reset();
        }
        mSelectedView.setAnimation(null);
        anim = new TranslateAnimation(0, 0, mFromYDelta, toYDelta);
        anim.setDuration(300);
        anim.setFillAfter(true);
        anim.setAnimationListener(mSelectedAnimationListener);
        mSelectedView.startAnimation(anim);
        mFromYDelta = toYDelta;
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
            if (hotelTKDrawable != null) {
                tkDrawable = hotelTKDrawable.getTKDrawable();
                if (tkDrawable != null) {
                    Drawable drawable = tkDrawable.loadDrawable(mThis, mLoadedDrawableRun, DishActivity.this.toString());
                    if(drawable != null) {
                        //To prevent the problem of size change of the same pic 
                        //After it is used at a different place with smaller size
                        Rect bounds = drawable.getBounds();
                        if(bounds != null && (bounds.width() != pictureImv.getWidth() || bounds.height() != pictureImv.getHeight())){
                            pictureImv.setBackgroundDrawable(null);
                        }
                        pictureImv.setBackgroundDrawable(drawable);
                    } else {
                        pictureImv.setBackgroundDrawable(null);
                    }
                }
            }
            
            if (tkDrawable == null) {
                Drawable drawable = getResources().getDrawable(R.drawable.btn_add_picture);
                pictureImv.setBackgroundDrawable(drawable);
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
        if (mode == mMode && tab == mTab) {
            mQueryingView.setVisibility(View.GONE);
        }
        if (BaseActivity.checkReLogin(dataQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mThis, null, false, mThis, false)) {
            
            if (mode == mMode && tab == mTab) {
                mEmptyView.setVisibility(View.GONE);
                mRetryView.setVisibility(View.VISIBLE);
            }
        } else {      
            setDataQuery(dataQuery, mode, tab);
            
            refreshData(mode, tab, false);
        }
    }
    
    void setDataQuery(DataQuery dataQuery, int mode, int tab) {
        
        DishResponse dishResponse = (DishResponse) dataQuery.getResponse();
        DishList dishList = dishResponse.getList();
        if (dishList != null) {
            List<Dish> dishes = dishList.getDishList();
            if (dishes != null && dishes.size() > 0) {
                if (mode == 0) {
                    if (tab == 0) {
                        mAllDataQuery = dataQuery;
                        mPOI.setDishQuery(dataQuery);
                        
                        mMyLikeList.clear();
                        for(int i = 0, size = dishes.size(); i < size; i++) {
                            Dish dish = dishes.get(i);
                            if (dish.isLike()) {
                                mMyLikeList.add(dish);
                            }
                        }
                        mMyLikeAdapter.notifyDataSetChanged();
                        LogWrapper.d(TAG, "likedish.size()"+dishes.size());
                        
                    } else {
                        LogWrapper.d(TAG, "recommenddish.size()"+dishes.size());
                        mRecommendDataQuery = dataQuery;
                        mPOI.setRecommendDishQuery(dataQuery);
                        
                        mRecommedList.clear();
                        mRecommedList.addAll(dishes);
                        mRecommendAdapter.notifyDataSetChanged();
                    }
                } else {
                    LogWrapper.d(TAG, "alldish.size()"+dishes.size());
                    
                    mAllDataQuery = dataQuery;
                    mPOI.setDishQuery(dataQuery);
                    
                    mAllList.clear();
                    mAllList.addAll(dishes);
                    
                    List<Category> categories = dishList.getCategoryList();
                    if (categories != null) {
                        mCategoryGroupList.clear();
                        mCategoryGroupList.addAll(categories);
                        mCategoryAdapter.measure();
                        for(int i = 0, size = mCategoryGroupList.size(); i < size; i++) {
                            View view = getLayoutInflater().inflate(CategoryListAdapter.RESOURCE_ID, mCategoryGroupView, false);
                            view.setBackgroundResource(R.drawable.bg_dish_category);
                            view.setTag(i);
                            view.setOnClickListener(this);
                            Category data = (Category) mCategoryGroupList.get(i);
                            TextView textView = (TextView) view.findViewById(R.id.text_txv);
                            textView.setText(data.getName());
                            mCategoryGroupView.addView(view);
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
            Dish data = (Dish) v.getTag(R.id.commend_view);
            int position = (Integer) v.getTag(R.id.index);
            ImageView commendImv = (ImageView) v.getTag(R.id.commend_imv);
            TextView commendTxv = (TextView) v.getTag(R.id.commend_txv);
            final boolean isLike = !data.isLike();
            if (isLike) {
                data.addLike();
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
                        Dish.getLocalMark().addCommend(mThis, uuid, false);
                        Dish.getLocalMark().addCommend(mThis, uuid, true);
                        
                        DataOperation dataOperation = Dish.getLocalMark().makeCommendDataOperationByUUID(mThis, uuid);
                        if (dataOperation != null) {
                            dataOperation.query();
                        }
                    } else {
                        String[] uuids = new String[]{uuid};
                        Dish.getLocalMark().deleteCommend(mThis, uuids, false);
                        Dish.getLocalMark().deleteCommend(mThis, uuids, true);
                    }
                }
            }).start();

            for(int i = mAllList.size()-1; i >= 0; i--) {
                Dish c = mAllList.get(i);
                if (dishId == c.getDishId()) {
                    if (isLike) {
                        c.addLike();
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
                        c.addLike();
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
                mEmptyTxv.setText(R.string.like_empty_tip);
                mEmptyImv.setBackgroundResource(R.drawable.ic_like_empty);
                mEmptyView.setVisibility(View.VISIBLE);
            }
        } else if (id == R.id.picture_imv) {
            Dish data = (Dish) v.getTag(R.id.picture_imv);
            if (data.getPicture() == null) {
                Intent intent = new Intent(mThis, AddPictureActivity.class);
                intent.putExtra(FileUpload.SERVER_PARAMETER_REF_DATA_TYPE, BaseQuery.DATA_TYPE_DISH);
                intent.putExtra(FileUpload.SERVER_PARAMETER_REF_ID, String.valueOf(data.getDishId()));
                intent.putExtra(AddPictureActivity.EXTRA_SUCCESS_TIP, getString(R.string.add_picture_success_for_dish));
                startActivityForResult(intent, 0);
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
            }
        } else {
            this.mRecommedView.setVisibility(View.INVISIBLE);
            this.mAllView.setVisibility(View.VISIBLE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment);
            mAllBtn.setBackgroundResource(R.drawable.btn_hot_comment_focused);
            
            mSelectedAdapter.notifyDataSetChanged();
        }
        mRetryView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mQueryingView.setVisibility(View.GONE);

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
        
        if (dataList.size() <= 0 && mode == mMode && tab == mTab) {
            if (mode == 0 && tab == 0) {
                if (mManuallyChanged == false) {
                    mRecommendVpg.setCurrentItem(1);
                    return;
                }
                mEmptyImv.setBackgroundResource(R.drawable.ic_like_empty);
                mEmptyTxv.setText(R.string.like_empty_tip);
            } else {
                mEmptyImv.setBackgroundResource(R.drawable.bg_query_fail);
                mEmptyTxv.setText(R.string.no_result);
            }
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }
    
    void refreshCategory(int groupPosition) {
        if (mGroupPosition != groupPosition) {
            mGroupPosition = groupPosition;
            
            int size = mCategoryGroupList.size();
            for(int i = 0; i < size; i++) {
                View view = mCategoryGroupView.getChildAt(i);
                if (i == mGroupPosition+1) {
                    int topMargin = mCategoryAdapter.totalHeight;
                    topMargin -= ((mCategoryGroupList.size())*mCategoryAdapter.groupHeight);
                    ((LinearLayout.LayoutParams) view.getLayoutParams()).topMargin = topMargin;
                } else {
                    ((LinearLayout.LayoutParams) view.getLayoutParams()).topMargin = 0;
                }
            }
            
            int top = (mGroupPosition+1)*mCategoryAdapter.groupHeight;
            int bottom = (size-mGroupPosition-1)*mCategoryAdapter.groupHeight;
            mPaddingTopView.getLayoutParams().height = top;
            mPaddingBottomView.getLayoutParams().height = bottom;
            mCategoryList.clear();
            mCategoryList.addAll(mCategoryGroupList.get(mGroupPosition).getChildList());
            
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
                        if (dish.getDishId() == id) {
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
//            for(int i = size - 1; i >= 0 ; i--) {
//                Category category = mCategoryList.get(i);
//                if (category.firstDishIndex == -1) {
//                    mCategoryList.remove(i);
//                }
//            }
            
            mCategoryAdapter.notifyDataSetChanged();
            mCategoryLsv.setSelectionFromTop(0, 0);
            
            MotionEvent me = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            mAllLsv.onTouchEvent(me);
            me = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            mAllLsv.onTouchEvent(me);
            mSelectedAdapter.notifyDataSetChanged();
            mAllLsv.setSelectionFromTop(0, 0);
            
            int h = mCategoryAdapter.totalHeight - mCategoryGroupList.size()*mCategoryAdapter.groupHeight;
            mTotalCateoryItem = h/mCategoryAdapter.childHeight;
        }
        
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
        mRetryView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mQueryingView.setVisibility(View.GONE);
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
        dataQuery.setup(Globals.getCurrentCityInfo().getId(), mId, mId, null, false, false, mPOI);
        queryStart(dataQuery);
        
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
        }

        static final int RESOURCE_ID = R.layout.poi_dish_category_list_item;

        int totalHeight = 0;
        int groupHeight = 0;
        int childHeight = 0;
        int padding;
        
        void measure() {
            if (totalHeight == 0) {
                totalHeight = mCategoryView.getBottom()-mCategoryView.getTop();
                View view = getLayoutInflater().inflate(RESOURCE_ID, mCategoryLsv, false);
                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
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
            if (position == mChildPosition && mSelectedView.getBackground() == null) {
                textView.setBackgroundResource(R.drawable.bg_dish_category_selected);
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

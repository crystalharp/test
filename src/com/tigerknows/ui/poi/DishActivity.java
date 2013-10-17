/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.LinearLayout.LayoutParams;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.DataQuery.DishResponse;
import com.tigerknows.model.DataQuery.DishResponse.Category;
import com.tigerknows.model.DataQuery.DishResponse.DishList;
import com.tigerknows.model.Dish;
import com.tigerknows.model.POI;
import com.tigerknows.ui.BaseActivity;
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
    
    public static final int REQUEST_CODE_COMMENT = 1;

    private static POI sPOI = null;
    
    private POI mPOI = null;
    
    private DataQuery mRecommendDataQuery;
    private DataQuery mAllDataQuery;

    private List<Category> mCategoryList = new ArrayList<Category>();
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
    private ExpandableListView mCategoryElv = null;
    private int mGroupPos = -1;
    private int mFromYDelta = 0;
    private int mFirstVisibleItem = 0;
    private Category mCurrentCategoryItem = null;
    private ListView mAllLsv = null;
    private ViewPager mRecommendVpg = null;
    private ListView mMyLikeLsv = null;
    private GridView mRecommendGdv = null;

    private QueryingView mQueryingView = null;
    
    private TextView mQueryingTxv = null;
    
    private View mEmptyView = null;
    
    private TextView mEmptyTxv = null;
    
    private RetryView mRetryView;
    
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

        mSelectedView.setVisibility(View.INVISIBLE);
        mRetryView.setText(R.string.touch_screen_and_retry, true);
        
        mCategoryAdapter = new CategoryListAdapter();
        mCategoryElv.setAdapter(mCategoryAdapter);
        
        mSelectedAdapter = new DishAdapter(mThis, DishAdapter.TextView_Resource_ID, mSelectedList);
        mAllLsv.setAdapter(mSelectedAdapter);
        
        mMyLikeAdapter = new DishAdapter(mThis, DishAdapter.TextView_Resource_ID, mMyLikeList);
        mMyLikeAdapter.myLike = true;
        mMyLikeLsv.setAdapter(mMyLikeAdapter);

        mRecommendAdapter = new DishAdapter(mThis, DishAdapter.Recommend_TextView_Resource_ID, mRecommedList);
        mRecommendGdv.setAdapter(mRecommendAdapter);
        
        mTitleBtn.setText(R.string.recommend_dish);
        mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
        mRightBtn.setVisibility(View.GONE);
        
        Resources resources = getResources();
        mColorNormal = resources.getColor(R.color.black_dark);
        mColorSelect = resources.getColor(R.color.orange);
        
        mPOI = sPOI;
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
        mCategoryElv = (ExpandableListView)findViewById(R.id.category_elv);
        mAllLsv = (ListView)findViewById(R.id.all_lsv);
        
        mRecommendVpg = (ViewPager)findViewById(R.id.view_pager);
        
        mRecommendGdv = new GridView(mThis);
        mRecommendGdv.setNumColumns(GridView.AUTO_FIT);
        int spacing = Utility.dip2px(mThis, 8);
        mRecommendGdv.setHorizontalSpacing(spacing);
        mRecommendGdv.setVerticalSpacing(spacing);
        mRecommendGdv.setColumnWidth(Utility.dip2px(mThis, 120));
        mRecommendGdv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        mRecommendGdv.setGravity(Gravity.CENTER);
        
        mMyLikeLsv = Utility.makeListView(mThis);
        
        List<View> viewList = new ArrayList<View>();
        viewList.add(mMyLikeLsv);
        viewList.add(mRecommendGdv);
        mRecommendVpg.setOnPageChangeListener(new MyPageChangeListener());
        mRecommendVpg.setAdapter(new MyAdapter(viewList));
        
        mQueryingView = (QueryingView)findViewById(R.id.querying_view);
        mEmptyView = findViewById(R.id.empty_view);
        mEmptyTxv = (TextView) mEmptyView.findViewById(R.id.empty_txv);
        mQueryingTxv = (TextView) mQueryingView.findViewById(R.id.loading_txv);
        mRetryView = (RetryView) findViewById(R.id.retry_view);
    }
    
    protected void setListener() {
        super.setListener();
        mTitleBtn.setOnClickListener(this);
        mAllBtn.setOnClickListener(this);
        mMyLikeBtn.setOnClickListener(this);
        mRecommendBtn.setOnClickListener(this);
        mRetryView.setCallBack(this, mActionTag);
        
        mCategoryElv.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (mGroupPos == groupPosition) {
                    return true;
                }
                mGroupPos = groupPosition;
                return false;
            }
        });
        mCategoryElv.setOnGroupExpandListener(new OnGroupExpandListener() {
            
            @Override
            public void onGroupExpand(int groupPosition) {
                mFirstVisibleItem = 0;
                List<Category> categories = mCategoryList.get(groupPosition).getChildList();
                mCurrentCategoryItem = categories.get(0);
                List<Long> idList = new ArrayList<Long>();
                for(int i = 0, size = categories.size(); i < size; i++) {
                    Category category = categories.get(i);
                    List<Long> ides = category.getDishList();
                    if (ides != null) {
                        idList.addAll(ides);
                    }
                }
                refresSelectedList(idList);
                for(int i = 0, count = mCategoryAdapter.getGroupCount(); i < count; i++) {
                    if (i != groupPosition) {
                        mCategoryElv.collapseGroup(i);
                    }
                }
                
                animationSelectView(0);
            }
        });
        
        mCategoryElv.setOnChildClickListener(new OnChildClickListener() {
            
            @Override
            public boolean onChildClick(ExpandableListView arg0, View arg1, int groupPosition, int childPosition, long arg4) {
                Category category = mCategoryList.get(groupPosition).getChildList().get(childPosition);
                List<Long> idList = category.getDishList();
                refresSelectedList(idList);
                
                animationSelectView(childPosition);
                
                return false;
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
                if (mFirstVisibleItem != firstVisibleItem) {
                    mFirstVisibleItem = firstVisibleItem;
                    Dish dish = mSelectedList.get(mFirstVisibleItem);
                    long dishId = dish.getDishId();
                    if (mCurrentCategoryItem != null && !mCurrentCategoryItem.getDishList().contains(dishId)) {
                        List<Category> categoryList = mCategoryList.get(mGroupPos).getChildList();
                        int i = 0;
                        for(int size = categoryList.size(); i < size; i++) {
                            if (categoryList.get(i).getDishList().contains(dishId)) {
                                mCurrentCategoryItem = categoryList.get(i);
                                break;
                            }
                        }
                        animationSelectView(i);
                    }
                }
            }
        });
    }
    
    void animationSelectView(int childPosition) {
        int toYDelta = (mGroupPos+1)*mCategoryAdapter.groupHeight+childPosition*mCategoryAdapter.childHeight;
        
        Animation anim = new TranslateAnimation(0, 0, mFromYDelta, toYDelta);
        anim.setDuration(300);
        anim.setFillAfter(true);
        mSelectedView.startAnimation(anim);
        mFromYDelta = toYDelta;
    }
    
    void refresSelectedList(List<Long> idList) {
        mSelectedList.clear();
        if (idList != null) {
            for(int i = 0, size = mAllList.size(); i < size; i++) {
                Dish dish = mAllList.get(i);
                if (idList.contains(dish.getDishId())) {
                    mSelectedList.add(dish);
                }
            }
        }
        mSelectedAdapter.notifyDataSetChanged();
    }
    
    protected void onResume() {
        super.onResume();
        if (isReLogin()) {
            return;
        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_COMMENT == requestCode) {
            if (RESULT_OK == resultCode) {
                setResult(RESULT_OK, data);
                finish();
            }
        } else {
            // 在提示用户在登录超时对话框后，点击重新登录返回时
            if (isReLogin()) {
                return;
            }
        }
    }
    
    private class DishAdapter extends ArrayAdapter<Dish> {
        static final int TextView_Resource_ID = R.layout.poi_dish_list_item;
        
        static final int Recommend_TextView_Resource_ID = R.layout.poi_dish_recommend_list_item;
        
        boolean myLike = false;
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

            Dish data = getItem(position);
            commendView.setTag(R.id.commend_view, data);
            commendView.setTag(R.id.commend_imv, commendImv);
            commendView.setTag(R.id.commend_txv, commendTxv);
            commendView.setTag(R.id.index, position);
            commendView.setOnClickListener(DishActivity.this);
            long likes = data.getHitCount();
            String likesStr;
            if (data.isLike()) {
                commendTxv.setTextColor(TKConfig.COLOR_ORANGE);
                commendImv.setImageResource(R.drawable.ic_commend_enabled);
                if (myLike) {
                    likesStr = getString(R.string.cancel_like);
                } else {
                    likesStr = getString(R.string.like_, likes);
                }
                commendTxv.setText(likesStr);
            } else {
                commendTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                commendImv.setImageResource(R.drawable.ic_commend_disabled);
                likesStr = getString(R.string.like_, likes);
                commendTxv.setText(likesStr);
            }
            float right = likesStr.length()*Globals.g_metrics.density*8 + Globals.g_metrics.density*12;
            ((RelativeLayout.LayoutParams) commendImv.getLayoutParams()).rightMargin = (int)right;
            
            pictureCountTxv.setText(getString(R.string.pictures, data.getPictureCount()));
            nameTxv.setText(data.getName());
            priceTxv.setText(data.getPrice());
            
            if (Recommend_TextView_Resource_ID == textViewResourceId) {
                int newWidth = (int) (Globals.g_metrics.density*120);
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
                mRetryView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mode == 0) {
                if (tab == 0) {
                    mAllDataQuery = dataQuery;
                } else {
                    mRecommendDataQuery = dataQuery;
                }
            } else {
                mAllDataQuery = dataQuery;
            }
            
            setData(mode, tab);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.title_btn) {
            mActionLog.addAction(mActionTag + ActionLog.DishRecomend);
            changeMode(0);
        } else if (id == R.id.all_btn) {
            mActionLog.addAction(mActionTag + ActionLog.DishAll);
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
            mActionLog.addAction(mActionTag+ActionLog.POICommentListCommend, position, data.getHitCount());
            ImageView commendImv = (ImageView) v.getTag(R.id.commend_imv);
            TextView commendTxv = (TextView) v.getTag(R.id.commend_txv);
            final boolean isLike = !data.isLike();
            if (isLike) {
                data.addLike();
            } else {
                data.deleteLike();
            }
            final long dishId = data.getDishId();
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    for(int i = mAllList.size()-1; i >= 0; i--) {
                        Dish c = mAllList.get(i);
                        if (dishId == c.getDishId()) {
                            if (isLike) {
                                c.addLike();
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
                            } else {
                                c.deleteLike();
                            }
                            break;
                        }
                    }
                    
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
            
            v.setBackgroundResource(R.drawable.btn_subway_busstop_normal);
            commendTxv.setTextColor(TKConfig.COLOR_ORANGE);
            String txt = commendTxv.getText().toString();
            if (txt.length() > 0) {
                int val = Integer.parseInt(txt);
                commendTxv.setText(String.valueOf(val+1));
            }
            commendImv.setImageResource(R.drawable.ic_commend_enabled);
            Animation animation = AnimationUtils.loadAnimation(mThis, R.anim.commend);
            commendImv.startAnimation(animation);
        }
    }
    
    void changeMode(int mode) {
        mMode = mode;
        int tab = mTab;
        if (mMode == 0) {
            this.mRecommedView.setVisibility(View.VISIBLE);
            this.mAllView.setVisibility(View.GONE);
            mSelectedView.setVisibility(View.INVISIBLE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
            mAllBtn.setBackgroundResource(R.drawable.btn_hot_comment);
        } else {
            this.mRecommedView.setVisibility(View.GONE);
            this.mAllView.setVisibility(View.VISIBLE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment);
            mAllBtn.setBackgroundResource(R.drawable.btn_hot_comment_focused);
            if (mSelectedList.size() > 0) {
                mSelectedView.setVisibility(View.VISIBLE);
            }
        }
        mRetryView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mQueryingView.setVisibility(View.GONE);

        setData(mode, tab);
    }
    
    void setData(int mode, int tab) {

        List<Dish> dataList;
        DataQuery dataQuery;
        if (mode == 0) {
            if (tab == 0) {
                dataQuery = mAllDataQuery;
                dataList = mAllList;
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
        
        if (dataList.size() > 0) {
            if (mode == mMode && tab == mTab) {
                if (mode == 0 && tab == 0) {
                    if (mMyLikeList.size() <= 0) {
                        mEmptyView.setVisibility(View.VISIBLE);
                    }
                    return;
                }
            }
            return;
        }
        
        DishResponse dishResponse = (DishResponse) dataQuery.getResponse();
        DishList dishList = dishResponse.getList();
        if (dishList != null) {
            List<Dish> dishes = dishList.getDishList();
            if (dishes != null && dishes.size() > 0) {
                if (mode == 0 && tab == 1) {
                    mPOI.setRecommendDishQuery(dataQuery);
                    
                    mRecommedList.addAll(dishes);
                    mRecommendAdapter.notifyDataSetChanged();
                    return;
                }

                mPOI.setDishQuery(dataQuery);
                
                for(int i = 0, size = dishes.size(); i < size; i++) {
                    Dish dish = dishes.get(i);
                    if (dish.isLike()) {
                        mMyLikeList.add(dish);
                    }
                }
                mMyLikeAdapter.notifyDataSetChanged();
                
                mAllList.addAll(dishes);
                mSelectedList.addAll(dishes);
                mSelectedAdapter.notifyDataSetChanged();
                List<Category> categories = dishList.getCategoryList();
                if (categories != null) {
                    mCategoryList.addAll(categories);
                    mCategoryAdapter.measure();
                    mCategoryAdapter.notifyDataSetChanged();
                    mGroupPos = 0;
                    mCategoryElv.expandGroup(0);
                }

                if (mode == mMode && tab == mTab) {
                    if (mode == 0 && tab == 0) {
                        if (mMyLikeList.size() <= 0) {
                            mEmptyTxv.setText(R.string.like_empty_tip);
                            mEmptyView.setVisibility(View.VISIBLE);
                        }
                    }
                }
                return;
            }
        }
        
        if (mode == mMode && tab == mTab) {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }
    
    void changeTab(int position) {

        mTab = position;
        if (mTab == 0) {
            mMyLikeBtn.setBackgroundResource(R.drawable.btn_tab_selected);
            mMyLikeBtn.setTextColor(mColorSelect);
            mRecommendBtn.setBackgroundResource(R.drawable.btn_tab);
            mRecommendBtn.setTextColor(mColorNormal);
        } else {
            mMyLikeBtn.setBackgroundResource(R.drawable.btn_tab);
            mMyLikeBtn.setTextColor(mColorNormal);
            mRecommendBtn.setBackgroundResource(R.drawable.btn_tab_selected);
            mRecommendBtn.setTextColor(mColorSelect);
        }
        mRetryView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mQueryingView.setVisibility(View.GONE);
        setData(mMode, mTab);
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
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, "1");
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
    
    public class CategoryListAdapter extends BaseExpandableListAdapter {
        
        static final int RESOURCE_ID = R.layout.string_list_item;

        int titleHeight = 0;
        int groupHeight = 0;
        int childHeight = 0;
        
        void measure() {
            if (titleHeight == 0) {
                mLeftBtn.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                titleHeight = mLeftBtn.getMeasuredHeight();
                View view = getLayoutInflater().inflate(RESOURCE_ID, mCategoryElv, false);
                view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                childHeight = view.getMeasuredHeight();
                groupHeight = childHeight;
            }
        }

        public Object getChild(int groupPosition, int childPosition) {
            return mCategoryList.get(groupPosition).getChildList().get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return mCategoryList.get(groupPosition).getChildList().get(childPosition).getId();
        }

        public int getChildrenCount(int groupPosition) {
            return mCategoryList.get(groupPosition).getChildList().size();
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            
            View view;
            if (convertView == null) {
                view = getLayoutInflater().inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            view.setBackgroundResource(R.drawable.list_selector_background_focused);
            Category data = (Category) getChild(groupPosition, childPosition);
            TextView textView = (TextView) view.findViewById(R.id.text_txv);
            textView.setText(data.getName());
            int childrenCount = getChildrenCount(groupPosition);
            if (childPosition == childrenCount-1) {
                int bottom = Globals.g_metrics.heightPixels-titleHeight-getGroupCount()*groupHeight-childrenCount*childHeight;
                textView.setPadding(0, 0, 0, bottom);
            } else {
                textView.setPadding(0, 0, 0, 0);
            }
            
            return view;
        }

        public Object getGroup(int groupPosition) {
            return mCategoryList.get(groupPosition);
        }

        public int getGroupCount() {
            return mCategoryList.size();
        }

        public long getGroupId(int groupPosition) {
            return mCategoryList.get(groupPosition).getId();
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            
            View view;
            if (convertView == null) {
                view = getLayoutInflater().inflate(RESOURCE_ID, parent, false);
            } else {
                view = convertView;
            }
            view.setBackgroundResource(R.drawable.list_selector_background_focus);
            Category data = (Category) getGroup(groupPosition);
            TextView textView = (TextView) view.findViewById(R.id.text_txv);
            textView.setText(data.getName());
            
            return view;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }
    }
}

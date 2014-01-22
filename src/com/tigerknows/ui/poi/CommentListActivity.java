/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.User;
import com.tigerknows.model.Comment.LocalMark;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.RetryView;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

/**
 * @author Peng Wenyue
 */
public class CommentListActivity extends BaseActivity implements View.OnClickListener, RetryView.CallBack {
    
    static final String TAG = "POICommentList";
    
    public static final int REQUEST_CODE_COMMENT = 1;

    private static POI sPOI = null;
    
    private RetryView mRetryView;
    
    private Button mHotBtn;
    private POI mPOI = null;
    private SpringbackListView mCommentLsv = null;
    private DataQuery mCommentQuery;
    private List<Comment> mCommentArrayList = new ArrayList<Comment>();
    private CommentAdapter mCommentAdapter;
    private boolean mTurnPageHeader = false;
    private boolean mTurnPageFooter = false;
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mCommentLsv.getLastVisiblePosition() >= mCommentLsv.getCount()-2 &&
                    mCommentLsv.getFirstVisiblePosition() == 0) {
                mCommentLsv.getView(false).performClick();
            }
        }
    };

    private SpringbackListView mHotCommentLsv = null;
    private DataQuery mHotCommentQuery;
    private List<Comment> mHotCommentArrayList = new ArrayList<Comment>();
    private CommentAdapter mHotCommentAdapter;
    private boolean mHotTurnPageHeader = false;
    private boolean mHotTurnPageFooter = false;
    
    private Runnable mHotTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mHotCommentLsv.getLastVisiblePosition() >= mHotCommentLsv.getCount()-2 &&
                    mHotCommentLsv.getFirstVisiblePosition() == 0) {
                mHotCommentLsv.getView(false).performClick();
            }
        }
    };
    
    private View mCommentTipView;
    
    private View mEmptyView;
    
    private Button mCommentTipEdt;
    
    public static void setPOI(POI poi) {
        sPOI = poi;
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.POICommentList;
        mId = R.id.activity_poi_comment_list;
        mHandler = new Handler();

        setContentView(R.layout.poi_comment_list);
        
        findViews();
        setListener();

        mRetryView.setText(R.string.touch_screen_and_retry, true);
        
        mCommentAdapter = new CommentAdapter(mThis, mCommentArrayList);
        mCommentLsv.setAdapter(mCommentAdapter);

        mHotCommentAdapter = new CommentAdapter(mThis, mHotCommentArrayList);
        mHotCommentLsv.setAdapter(mHotCommentAdapter);
        
        mTitleBtn.setText(R.string.all_comment);
        mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
        mCommentTipView.setVisibility(View.GONE);
        
        mPOI = sPOI;
        if (mPOI != null) {
            changeMode(true);

            if (mPOI.isGoldStamp() || mPOI.isSilverStamp()) {
                mCommentTipEdt.setHint(R.string.comment_tip_hit1);
            } else {
                mCommentTipEdt.setHint(R.string.comment_tip_hit0);
            }
        } else {
            finish();
        }
        
        DataOperation dataOperation = Comment.getLocalMark().makeCommendDataOperation(mThis, LocalMark.STATE_DRAFT);
        if (dataOperation != null) {
            queryStart(dataOperation);
        }
    }

    protected void findViews() {
        super.findViews();
        mHotBtn = (Button) findViewById(R.id.hot_btn);
        mCommentLsv = (SpringbackListView)findViewById(R.id.comment_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
//        mCommentLsv.addHeaderView(v);
//        v = mLayoutInflater.inflate(R.layout.loading, null);
        mCommentLsv.addFooterView(v);
        mHotCommentLsv = (SpringbackListView)findViewById(R.id.hot_comment_lsv);
        v = mLayoutInflater.inflate(R.layout.loading, null);
//        mCommentLsv.addHeaderView(v);
//        v = mLayoutInflater.inflate(R.layout.loading, null);
        mHotCommentLsv.addFooterView(v);
        mCommentTipView = findViewById(R.id.tip_view);
        mCommentTipEdt = (Button) findViewById(R.id.comment_tip_btn);
        mEmptyView = findViewById(R.id.empty_view);
        mRetryView = (RetryView) findViewById(R.id.retry_view);
    }
    
    protected void setListener() {
        super.setListener();
        mTitleBtn.setOnClickListener(this);
        mHotBtn.setOnClickListener(this);
        mRetryView.setCallBack(this, mActionTag);
        mCommentLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage(true, isHeader);
            }
        });
        mHotCommentLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage(false, isHeader);
            }
        });

        mCommentTipEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mActionLog.addAction(mActionTag +  ActionLog.POICommentListInput);
                    POI poi = mPOI;
                    if (poi != null) {
                        boolean isMe = (poi.isGoldStamp() || poi.isSilverStamp());
                        if (poi.getStatus() < 0) {
                            int resId;
                            if (isMe) {
                                resId = R.string.poi_comment_poi_invalid_not_update;
                            } else {
                                resId = R.string.poi_comment_poi_invalid_not_create;
                            }

                            Utility.showNormalDialog(mThis, 
                                    mThis.getString(resId));
                        } else {
                            EditCommentActivity.setPOI(poi, R.id.activity_poi_comment_list, isMe ? EditCommentActivity.STATUS_MODIFY : EditCommentActivity.STATUS_NEW);
                            Intent intent = new Intent();
                            intent.setClass(mThis, EditCommentActivity.class);
                            mThis.startActivityForResult(intent, REQUEST_CODE_COMMENT);
                        }
                    }
                }
                return false;
            }
        });
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
    
    public void turnPage(boolean isNormal, boolean isHeader){
        synchronized (this) {
            SpringbackListView mCommentLsv;
            DataQuery mCommentQuery;
            List<Comment> mCommentArrayList;
            boolean mTurnPageHeader;
            boolean mTurnPageFooter;
            if (isNormal) {
                mCommentLsv = this.mCommentLsv;
                mCommentQuery = this.mCommentQuery;
                mCommentArrayList = this.mCommentArrayList;
                mTurnPageHeader = this.mTurnPageHeader;
                mTurnPageFooter = this.mTurnPageFooter;
            } else {
                mCommentLsv = this.mHotCommentLsv;
                mCommentQuery = this.mHotCommentQuery;
                mCommentArrayList = this.mHotCommentArrayList;
                mTurnPageHeader = this.mHotTurnPageHeader;
                mTurnPageFooter = this.mHotTurnPageFooter;
            }
        if (mCommentQuery == null) {
            DataQuery dataQuery = new DataQuery(mThis);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
            if (isNormal == false) {
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_HOT);
            }
            dataQuery.setup(mId, mId, null, false, false, mPOI);
            mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            queryStart(dataQuery);
            return;
        }
        if (isHeader) {
            if (mTurnPageHeader) {
                return;
            }
            if (isNormal) {
                this.mTurnPageHeader = true;
            } else {
                this.mHotTurnPageHeader = true;
            }
        } else {
            if (mTurnPageFooter) {
                return;
            }
            if (isNormal) {
                this.mTurnPageFooter = true;
            } else {
                this.mHotTurnPageFooter = true;
            }
        }

        mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);

        DataQuery dataQuery = new DataQuery(mCommentQuery);
        POI requestPOI = mCommentQuery.getPOI();
        if (mCommentArrayList.size() > 0) {
            mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            if (isHeader) {
                Comment comment = mCommentArrayList.get(0);
                if (Comment.isAuthorMe(comment) > 0) {
                    if (mCommentArrayList.size() > 1) {
                        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_TIME, mCommentArrayList.get(1).getTime());
                    } else {
                        dataQuery.addParameter(DataQuery.SERVER_PARAMETER_TIME, comment.getTime());
                    }
                } else {
                    dataQuery.addParameter(DataQuery.SERVER_PARAMETER_TIME, comment.getTime());
                }
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DIRECTION, DataQuery.DIRECTION_AFTER);
                dataQuery.setup(-1, -1, null, true, false, requestPOI);
                queryStart(dataQuery);
            } else {
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_TIME, mCommentArrayList.get(mCommentArrayList.size()-1).getTime());
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DIRECTION, DataQuery.DIRECTION_BEFORE);
                dataQuery.setup(-1, -1, null, true, false, requestPOI);
                queryStart(dataQuery);
            }
        } else {
            mCommentLsv.changeHeaderViewByState(isHeader, SpringbackListView.DONE);
        }
        }
    }

    @SuppressWarnings("unchecked")
    public void setData(boolean isNormal, DataQuery dataQuery) {
        SpringbackListView mCommentLsv;
        DataQuery mCommentQuery;
        List<Comment> mCommentArrayList;
        CommentAdapter mCommentAdapter;
        if (isNormal) {
            mCommentLsv = this.mCommentLsv;
            mCommentQuery = this.mCommentQuery;
            mCommentArrayList = this.mCommentArrayList;
            mCommentAdapter = this.mCommentAdapter;
        } else {
            mCommentLsv = this.mHotCommentLsv;
            mCommentQuery = this.mHotCommentQuery;
            mCommentArrayList = this.mHotCommentArrayList;
            mCommentAdapter = this.mHotCommentAdapter;
        }
        POI poi = dataQuery.getPOI();
        if (poi == null) {
            finish();
        }
        if (dataQuery.isTurnPage()) {
            boolean isHeader = false;
            if (mCommentQuery.hasParameter(DataQuery.SERVER_PARAMETER_DIRECTION)) {
                String direction = mCommentQuery.getParameter(DataQuery.SERVER_PARAMETER_DIRECTION);
                if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                    if (isNormal) {
                    mTurnPageHeader = false;
                    } else {
                        mHotTurnPageHeader = false;
                    }
                    isHeader = true;
                } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                    if (isNormal) {
                    mTurnPageFooter = false;
                    } else {
                        mHotTurnPageFooter = false;
                    }
                    isHeader = false;
                }
            }
            Response response = dataQuery.getResponse();
            if (response != null && response instanceof CommentResponse) {
                CommentResponse commentResponse = (CommentResponse)response;
                CommentList commentList = commentResponse.getList();
                if (commentList != null) {
                    List<Comment> commentArrayList = commentList.getList();
                    if (commentArrayList != null && commentArrayList.size() > 0) {
                        List<Comment> list = new ArrayList<Comment>();
                        for(Comment comment : commentArrayList) {
                            if (mCommentArrayList.contains(comment) == false) {
                                list.add(comment);
                            }
                        }
                        if (isHeader) {
                            int index = 0;
                            if (poi.isGoldStamp() || poi.isSilverStamp()) {
                                index = 1;
                            }
                            mCommentArrayList.addAll(index, list);
                        } else {
                            mCommentArrayList.addAll(list);
                        }
                        commentResponse = (CommentResponse)mCommentQuery.getResponse(); 
                        commentResponse.getList().getList().addAll(list);
                        Collections.sort(mCommentArrayList, Comment.COMPARATOR);
                        mCommentAdapter.notifyDataSetChanged();
                        if (commentResponse.canTurnPage() && list.size() >= TKConfig.getPageSize()) {
                            mCommentLsv.setFooterSpringback(true);
                        } else {
                            commentResponse.setCanTurnPage(false);
                        }
                    }
                }
            }
        } else {
            if (isNormal) {
            mTurnPageHeader = false;
            } else {
                mHotTurnPageHeader = false;
            }
            if (isNormal) {
            mTurnPageFooter = false;
            } else {
                mHotTurnPageFooter = false;
            }
            mCommentArrayList.clear();
            Response response = dataQuery.getResponse();
            if (response != null && response instanceof CommentResponse) {
                CommentResponse commentResponse = (CommentResponse)response;
                if (isNormal) {
                    poi.setCommentQuery(dataQuery);
                } else {
                    poi.setHotCommentQuery(dataQuery);
                }
                CommentList commentList = commentResponse.getList();
                if (commentList != null) {
                    List<Comment> commentArrayList = commentList.getList();
                    if (commentArrayList != null && commentArrayList.size() > 0) {
                        if (isNormal) {
                            this.mCommentQuery = dataQuery;
                        } else {
                            this.mHotCommentQuery = dataQuery;
                        }
                        mCommentArrayList.addAll(commentArrayList);
                        Collections.sort(mCommentArrayList, Comment.COMPARATOR);
                        mCommentAdapter.notifyDataSetChanged();
                        if (mCommentArrayList.isEmpty()) {
                            mCommentLsv.setVisibility(View.GONE);
                        } else {
                            mCommentLsv.setVisibility(View.VISIBLE);
                        }
                        if (commentResponse.canTurnPage() && commentArrayList.size() >= TKConfig.getPageSize()) {
                            mCommentLsv.setFooterSpringback(true);
                        } else {
                            commentResponse.setCanTurnPage(false);
                        }
                    }
                }
            }
            
            if (mCommentArrayList.isEmpty()) {
                if (isNormal) {
                finish();
                } else if (this.mCommentLsv.getVisibility() != View.VISIBLE){
                	mEmptyView.setVisibility(View.VISIBLE);
                }
            }
        }
        
        if (mCommentLsv.isFooterSpringback()) {
            mHandler.postDelayed(isNormal ? mTurnPageRun : mHotTurnPageRun, 1000);
        }
        if (isNormal) {
        mCommentTipView.setVisibility(View.VISIBLE);
        }
    }
    
    private class CommentAdapter extends ArrayAdapter<Comment>{
        private static final int TextView_Resource_ID = R.layout.poi_comment_list_item;

        public CommentAdapter(Context context, List<Comment> commentList) {
            super(context, TextView_Resource_ID, commentList);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(TextView_Resource_ID, parent, false);
            } else {
                view = convertView;
            }
            

            try {
                RatingBar gradeRtb = (RatingBar)view.findViewById(R.id.grade_rtb);
                TextView authorTxv = (TextView) view.findViewById(R.id.author_txv);
                TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);
                TextView commentTxv = (TextView) view.findViewById(R.id.comment_txv);
                TextView srcTxv = (TextView) view.findViewById(R.id.src_txv);
                View commendView = view.findViewById(R.id.commend_view);
                TextView commendTxv = (TextView)view.findViewById(R.id.commend_txv);
                ImageView commendImv = (ImageView)view.findViewById(R.id.commend_imv);
                TextView avgTxv = (TextView) view.findViewById(R.id.avg_txv);

                avgTxv.setVisibility(View.VISIBLE);
                commendView.setVisibility(View.VISIBLE);
                commendImv.setVisibility(View.VISIBLE);
                Comment comment = getItem(position);
                commendView.setTag(R.id.commend_view, comment);
                commendView.setTag(R.id.commend_imv, commendImv);
                commendView.setTag(R.id.commend_txv, commendTxv);
                commendView.setTag(R.id.index, position);
                commendView.setOnClickListener(CommentListActivity.this);
                String likes = String.valueOf(comment.getLikes());
                commendTxv.setText(likes);
                if (comment.isCommend()) {
                    commendView.setBackgroundResource(R.drawable.btn_subway_busstop_normal);
                    commendTxv.setTextColor(TKConfig.COLOR_ORANGE);
                    commendImv.setImageResource(R.drawable.ic_commend_enabled);
                } else {
                    commendView.setBackgroundResource(R.drawable.btn_subway_busstop);
                    commendTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                    commendImv.setImageResource(R.drawable.ic_commend_disabled);
                }
                float right = likes.length()*Globals.g_metrics.density*8 + Globals.g_metrics.density*12;
                ((RelativeLayout.LayoutParams) commendImv.getLayoutParams()).rightMargin = (int)right;
                
                long avg = comment.getAvg();
                if (avg > 0) {
                    avgTxv.setText(getString(R.string.yuan, avg));
                } else {
                    avgTxv.setText("");
                }
                
                float grade = comment.getGrade()/2.0f;
                gradeRtb.setRating(grade);
                
                authorTxv.setText(comment.getUser());
                if (Comment.isAuthorMe(comment) > 0) {
                    User user = Globals.g_User;
                    if (user != null) {
                        authorTxv.setText(user.getNickName());
                    }
                    view.setBackgroundResource(R.drawable.list_middle);
                    authorTxv.setTextColor(0xff009CFF);
                    mPOI.setMyComment(comment);
                    view.setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View arg0) {
                            mActionLog.addAction(mActionTag +  ActionLog.POICommentListMyComment);
                            if (mPOI.getStatus() >= 0) {
                                Intent intent = new Intent();
                                intent.setClass(mThis, EditCommentActivity.class);
                                EditCommentActivity.setPOI(mPOI, mId, EditCommentActivity.STATUS_MODIFY);
                                mThis.startActivityForResult(intent, R.id.activity_poi_edit_comment);
                            } else {
                                Utility.showNormalDialog(mThis, 
                                        mThis.getString(R.string.comment_poi_invalid_hit));
                            }
                        }
                    });
                } else {
                    view.setBackgroundResource(R.drawable.list_middle_normal);
                    authorTxv.setTextColor(TKConfig.COLOR_BLACK_LIGHT);
                    view.setOnClickListener(null);
                }
                
                String time = comment.getTime();
                if (!TextUtils.isEmpty(time) && time.length() >= 10) {
                    dateTxv.setText(time.subSequence(0, 10));
                } else {
                    dateTxv.setText(null);
                }
                
                String body = comment.getContent();
                commentTxv.setText(body);
                
                final String url = comment.getUrl();
                if (TextUtils.isEmpty(url)) {
                    srcTxv.setVisibility(View.GONE);
                } else {
                    srcTxv.setVisibility(View.VISIBLE);
                    String source = mThis.getString(R.string.source_);
                    String src = String.format(source, url);
                    SpannableString srcSns = new SpannableString(src);
                    srcSns.setSpan(new ClickableSpan() {
                        @Override
                        public void updateDrawState(TextPaint ds) {
                            ds.setUnderlineText(false);// 去掉链接的下划线
                        }
        
                        @Override
                        public void onClick(final View widget) {
                            mActionLog.addAction(mActionTag +  ActionLog.POICommentListUrl, position, url);
                            Utility.showNormalDialog(mThis,
                                    mThis.getString(R.string.prompt), 
                                    mThis.getString(R.string.are_you_view_url, url),
                                    new DialogInterface.OnClickListener() {
                                        
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            switch (id) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                                    mThis.startActivityForResult(intent, R.id.view_invalid);
                                                    break;
        
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                        }
                    }, String.format(source, "").length(), src.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    srcTxv.setText(srcSns);
                    srcTxv.setMovementMethod(LinkMovementMethod.getInstance()); 
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        
        boolean isHeader = false;
        boolean isNormal = (dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_BIAS) == false);
        if (dataQuery.hasParameter(DataQuery.SERVER_PARAMETER_DIRECTION)) {
            String direction = dataQuery.getParameter(DataQuery.SERVER_PARAMETER_DIRECTION);
            if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                if (isNormal) {
                mTurnPageHeader = false;
                } else {
                    mHotTurnPageHeader = false;
                }
                isHeader = true;
            } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                if (isNormal) {
                mTurnPageFooter = false;
                } else {
                    mHotTurnPageFooter = false;
                }
                isHeader = false;
            }
        }

        SpringbackListView mCommentLsv;
        if (isNormal) {
            mCommentLsv = this.mCommentLsv;
        } else {
            mCommentLsv = this.mHotCommentLsv;
        }
        mCommentLsv.onRefreshComplete(isHeader);
        
        if (isHeader) {
            mCommentLsv.setHeaderSpringback(false);
        } else {
            mCommentLsv.setFooterSpringback(false);
        }

        if (BaseActivity.checkReLogin(dataQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.hasAbnormalResponseCode(dataQuery, mThis, BaseActivity.SHOW_NOTHING, mThis, false)) {
            if (dataQuery.getResponse() == null) {
                if (isHeader) {
                    
                } else {
                    if (isNormal == false && mHotCommentArrayList.size() == 0) {
                        mRetryView.setVisibility(View.VISIBLE);
                    }
                    mCommentLsv.setFooterLoadFailed(true);
                }
                return;
            }
        }
        
        setData(isNormal, dataQuery);
    }
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        DataQuery commentQuery = (DataQuery)(tkAsyncTask.getBaseQuery());
        boolean isHeader = false;
        boolean isNormal = (commentQuery.hasParameter(DataQuery.SERVER_PARAMETER_BIAS) == false);
        if (commentQuery.hasParameter(DataQuery.SERVER_PARAMETER_DIRECTION)) {
            String direction = commentQuery.getParameter(DataQuery.SERVER_PARAMETER_DIRECTION);
            if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                if (isNormal) {
                mTurnPageHeader = false;
                } else {
                    mHotTurnPageHeader = false;
                }
                isHeader = true;
            } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                if (isNormal) {
                mTurnPageFooter = false;
                } else {
                    mHotTurnPageFooter = false;
                }
                isHeader = false;
            }
        }
        SpringbackListView mCommentLsv;
        if (isNormal) {
            mCommentLsv = this.mCommentLsv;
        } else {
            mCommentLsv = this.mHotCommentLsv;
        }
        mCommentLsv.onRefreshComplete(isHeader);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.title_btn) {
            mActionLog.addAction(mActionTag + ActionLog.POICommentListAllComment);
            changeMode(true);
        } else if (id == R.id.hot_btn) {
            mActionLog.addAction(mActionTag + ActionLog.POICommentListHotComment);
            changeMode(false);
        } else if (id == R.id.commend_view) {
            Comment comment = (Comment) v.getTag(R.id.commend_view);
            int position = (Integer) v.getTag(R.id.index);
            mActionLog.addAction(mActionTag+ActionLog.POICommentListCommend, position, comment.getLikes());
            ImageView commendImv = (ImageView) v.getTag(R.id.commend_imv);
            TextView commendTxv = (TextView) v.getTag(R.id.commend_txv);
            if (comment.isCommend() == false) {
                comment.addCommend(true);
                final String uuid = comment.getUid();
                new Thread(new Runnable() {
                    
                    @Override
                    public void run() {
                        for(int i = mCommentArrayList.size()-1; i >= 0; i--) {
                            Comment c = mCommentArrayList.get(i);
                            if (uuid != null && uuid.equals(c.getUid())) {
                                if (c.isCommend() == false) {
                                    c.addCommend(true);
                                }
                                break;
                            }
                        }
                        for(int i = mHotCommentArrayList.size()-1; i >= 0; i--) {
                            Comment c = mHotCommentArrayList.get(i);
                            if (uuid != null && uuid.equals(c.getUid())) {
                                if (c.isCommend() == false) {
                                    c.addCommend(true);
                                }
                                break;
                            }
                        }
                        
                        Comment.getLocalMark().addCommend(mThis, uuid, LocalMark.STATE_DRAFT);
                        Comment.getLocalMark().addCommend(mThis, uuid, LocalMark.STATE_SENT);
                        DataOperation dataOperation = Comment.getLocalMark().makeCommendDataOperationByUUID(mThis, uuid, true);
                        if (dataOperation != null) {
                            dataOperation.query();
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
    }
    
    void changeMode(boolean isNormal) {
        SpringbackListView mCommentLsv;
        CommentAdapter mCommentAdapter;
        if (isNormal) {
            mCommentLsv = this.mCommentLsv;
            mCommentAdapter = this.mCommentAdapter;
            this.mCommentLsv.setVisibility(View.VISIBLE);
            this.mHotCommentLsv.setVisibility(View.GONE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment_focused);
            mHotBtn.setBackgroundResource(R.drawable.btn_hot_comment);
        } else {
            mCommentLsv = this.mHotCommentLsv;
            mCommentAdapter = this.mHotCommentAdapter;
            this.mCommentLsv.setVisibility(View.GONE);
            this.mHotCommentLsv.setVisibility(View.VISIBLE);
            mTitleBtn.setBackgroundResource(R.drawable.btn_all_comment);
            mHotBtn.setBackgroundResource(R.drawable.btn_hot_comment_focused);
        }
        mRetryView.setVisibility(View.GONE);
        mEmptyView.setVisibility(View.GONE);
        mCommentAdapter.notifyDataSetChanged();
        mCommentLsv.setFooterSpringback(false);
        DataQuery dataQuery;
        if (isNormal) {
            dataQuery = mPOI.getCommentQuery();
        } else {
            dataQuery = mPOI.getHotCommentQuery();
        }
        if (dataQuery == null) {
            dataQuery = new DataQuery(mThis);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
            dataQuery.addParameter(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
            if (isNormal == false) {
                dataQuery.addParameter(DataQuery.SERVER_PARAMETER_BIAS, DataQuery.BIAS_HOT);
            }
            dataQuery.setup(mId, mId, null, false, false, mPOI);
            mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            queryStart(dataQuery);
        } else {
            mCommentLsv.onRefreshComplete(true);
            setData(isNormal, dataQuery);
            mCommentAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void retry() {
        turnPage(false, false);
        mRetryView.setVisibility(View.GONE);
    }
}

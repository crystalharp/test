/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import com.decarta.Globals;
import com.tigerknows.R;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.Response;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.TKAsyncTask;
import com.tigerknows.view.SpringbackListView;
import com.tigerknows.view.SpringbackListView.OnRefreshListener;
import com.tigerknows.view.user.User;

/**
 * @author Peng Wenyue
 */
public class POICommentList extends BaseActivity {
    
    static final String TAG = "POICommentList";
    
    public static final int REQUEST_CODE_COMMENT = 1;

    private static POI sPOI = null;
    
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
    
    private View mCommentTipView;
    
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

        mCommentAdapter = new CommentAdapter(mThis, mCommentArrayList);
        mCommentLsv.setAdapter(mCommentAdapter);
        
        mTitleBtn.setText(R.string.all_comment);
        mRightBtn.setVisibility(View.GONE);
        mCommentTipView.setVisibility(View.GONE);
        
        mPOI = sPOI;
        if (mPOI != null) {
            mCommentLsv.setFooterSpringback(false);
            DataQuery dataQuery = mPOI.getCommentQuery();
            if (dataQuery == null) {
                Hashtable<String, String> criteria = new Hashtable<String, String>();
                criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
                criteria.put(DataQuery.SERVER_PARAMETER_POI_ID, mPOI.getUUID());
                criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_POI);
                dataQuery = new DataQuery(mThis);
                dataQuery.setup(criteria, Globals.g_Current_City_Info.getId(), mId, mId, null, false, false, mPOI);
                mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
                queryStart(dataQuery);
            } else {
                mCommentLsv.onRefreshComplete(true);
                setData(dataQuery);
                mCommentAdapter.notifyDataSetChanged();
            }

            if (mPOI.isGoldStamp() || mPOI.isSilverStamp()) {
                mCommentTipEdt.setHint(R.string.comment_tip_hit1);
            } else {
                mCommentTipEdt.setHint(R.string.comment_tip_hit0);
            }
        } else {
            finish();
        }
    }

    protected void findViews() {
        super.findViews();
        mCommentLsv = (SpringbackListView)findViewById(R.id.comment_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
//        mCommentLsv.addHeaderView(v);
//        v = mLayoutInflater.inflate(R.layout.loading, null);
        mCommentLsv.addFooterView(v);
        mCommentTipView = findViewById(R.id.tip_view);
        mCommentTipEdt = (Button) findViewById(R.id.comment_tip_btn);
    }
    
    protected void setListener() {
        super.setListener();
        mCommentLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage(isHeader);
            }
        });

        mCommentTipEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "commentTip");
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

                            CommonUtils.showNormalDialog(mThis, 
                                    mThis.getString(resId));
                        } else {
                            POIComment.setPOI(poi, R.id.activity_poi_comment_list, isMe ? POIComment.STATUS_MODIFY : POIComment.STATUS_NEW);
                            Intent intent = new Intent();
                            intent.setClass(mThis, POIComment.class);
                            mThis.startActivityForResult(intent, REQUEST_CODE_COMMENT);
                        }
                    }
                }
                return false;
            }
        });
    }
    
    protected void onRusume() {
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
    
    public void turnPage(boolean isHeader){
        synchronized (this) {
        if (mCommentQuery == null) {
            mCommentLsv.changeHeaderViewByState(isHeader, SpringbackListView.DONE);
            return;
        }
        if (isHeader) {
            if (mTurnPageHeader) {
                return;
            }
            mTurnPageHeader = true;
        } else {
            if (mTurnPageFooter) {
                return;
            }
            mTurnPageFooter = true;
        }

        mActionLog.addAction(ActionLog.LISTVIEW_ITEM_ONCLICK, "loadMore");

        DataQuery dataQuery = new DataQuery(mThis);
        POI requestPOI = mCommentQuery.getPOI();
        int cityId = mCommentQuery.getCityId();
        Hashtable<String, String> criteria = mCommentQuery.getCriteria();
        if (mCommentArrayList.size() > 0) {
            mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            if (isHeader) {
                Comment comment = mCommentArrayList.get(0);
                if (Comment.isAuthorMe(comment) > 0) {
                    if (mCommentArrayList.size() > 1) {
                        criteria.put(DataQuery.SERVER_PARAMETER_TIME, mCommentArrayList.get(1).getTime());
                    } else {
                        criteria.put(DataQuery.SERVER_PARAMETER_TIME, comment.getTime());
                    }
                } else {
                    criteria.put(DataQuery.SERVER_PARAMETER_TIME, comment.getTime());
                }
                criteria.put(DataQuery.SERVER_PARAMETER_DIRECTION, DataQuery.DIRECTION_AFTER);
                dataQuery.setup(criteria, cityId, -1, -1, null, true, true, requestPOI);
                queryStart(dataQuery);
            } else {
                criteria.put(DataQuery.SERVER_PARAMETER_TIME, mCommentArrayList.get(mCommentArrayList.size()-1).getTime());
                criteria.put(DataQuery.SERVER_PARAMETER_DIRECTION, DataQuery.DIRECTION_BEFORE);
                dataQuery.setup(criteria, cityId, -1, -1, null, true, true, requestPOI);
                queryStart(dataQuery);
            }
        } else {
            mCommentLsv.changeHeaderViewByState(isHeader, SpringbackListView.DONE);
        }
        }
    }

    @SuppressWarnings("unchecked")
    public void setData(DataQuery dataQuery) {
        POI poi = dataQuery.getPOI();
        if (poi == null) {
            finish();
        }
        if (dataQuery.isTurnPage()) {
            boolean isHeader = true;
            Hashtable<String, String> criteria = mCommentQuery.getCriteria();
            if (criteria.containsKey(DataQuery.SERVER_PARAMETER_DIRECTION)) {
                String direction = criteria.get(DataQuery.SERVER_PARAMETER_DIRECTION);
                if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                    mTurnPageHeader = false;
                    isHeader = true;
                } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                    mTurnPageFooter = false;
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
                        ((CommentResponse)mCommentQuery.getResponse()).getList().getList().addAll(list);
                        Collections.sort(mCommentArrayList, Comment.COMPARATOR);
                        mCommentAdapter.notifyDataSetChanged();
                        if (list.size()+commentList.getTotal() >= TKConfig.getPageSize()) {
                            mCommentLsv.setFooterSpringback(true);
                        }
                    }
                }
            }
        } else {
            mTurnPageHeader = false;
            mTurnPageFooter = false;
            mCommentArrayList.clear();
            Response response = dataQuery.getResponse();
            if (response != null && response instanceof CommentResponse) {
                CommentResponse commentResponse = (CommentResponse)response;
                poi.setCommentQuery(dataQuery);
                CommentList commentList = commentResponse.getList();
                if (commentList != null) {
                    List<Comment> commentArrayList = commentList.getList();
                    if (commentArrayList != null && commentArrayList.size() > 0) {
                        mCommentQuery = dataQuery;
                        mCommentArrayList.addAll(commentArrayList);
                        Collections.sort(mCommentArrayList, Comment.COMPARATOR);
                        mCommentAdapter.notifyDataSetChanged();
                        if (mCommentArrayList.isEmpty()) {
                            mCommentLsv.setVisibility(View.GONE);
                        } else {
                            mCommentLsv.setVisibility(View.VISIBLE);
                        }
                        if (commentArrayList.size()+commentList.getTotal() >= TKConfig.getPageSize()) {
                            mCommentLsv.setFooterSpringback(true);
                        }
                    }
                }
            }
            
            if (mCommentArrayList.isEmpty()) {
                finish();
            }
        }
        
        if (mCommentLsv.isFooterSpringback()) {
            mHandler.postDelayed(mTurnPageRun, 1000);
        }
        mCommentTipView.setVisibility(View.VISIBLE);
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
                
                Comment comment = getItem(position);
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
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "myComment");
                            if (mPOI.getStatus() >= 0) {
                                Intent intent = new Intent();
                                intent.setClass(mThis, POIComment.class);
                                POIComment.setPOI(mPOI, mId, POIComment.STATUS_MODIFY);
                                mThis.startActivityForResult(intent, R.id.activity_poi_comment);
                            } else {
                                CommonUtils.showNormalDialog(mThis, 
                                        mThis.getString(R.string.comment_poi_invalid_hit));
                            }
                        }
                    });
                } else {
                    view.setBackgroundResource(R.drawable.list_middle_normal);
                    authorTxv.setTextColor(0xff000000);
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
                            mActionLog.addAction(ActionLog.CONTROL_ONCLICK, "source", position, url);
                            CommonUtils.showNormalDialog(mThis,
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
        DataQuery dataQuery = (DataQuery)(tkAsyncTask.getBaseQuery());

        if (BaseActivity.checkReLogin(dataQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(dataQuery, mThis, null, true, mThis, dataQuery.isTurnPage() == false)) {
            return;
        }
        
        boolean isHeader = true;
        Hashtable<String, String> criteria = dataQuery.getCriteria();
        if (criteria.containsKey(DataQuery.SERVER_PARAMETER_DIRECTION)) {
            String direction = criteria.get(DataQuery.SERVER_PARAMETER_DIRECTION);
            if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                mTurnPageHeader = false;
                isHeader = true;
            } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                mTurnPageFooter = false;
                isHeader = false;
            }
        }
        mCommentLsv.onRefreshComplete(isHeader);
        mCommentLsv.setFooterSpringback(false);
        
        setData(dataQuery);
    }
    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        DataQuery commentQuery = (DataQuery)(tkAsyncTask.getBaseQuery());
        boolean isHeader = true;
        Hashtable<String, String> criteria = commentQuery.getCriteria();
        if (criteria.containsKey(DataQuery.SERVER_PARAMETER_DIRECTION)) {
            String direction = criteria.get(DataQuery.SERVER_PARAMETER_DIRECTION);
            if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                mTurnPageHeader = false;
                isHeader = true;
            } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                mTurnPageFooter = false;
                isHeader = false;
            }
        }
        mCommentLsv.onRefreshComplete(isHeader);
    }
}

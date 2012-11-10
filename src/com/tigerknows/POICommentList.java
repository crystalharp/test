/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import android.app.AlertDialog;
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
import android.widget.Toast;

import com.tigerknows.R;
import com.tigerknows.model.BaseQuery;
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

/**
 * @author Peng Wenyue
 */
public class POICommentList extends BaseActivity {
    
    static final String TAG = "POICommentList";
    
    public static final int REQUEST_CODE_COMMENT = 1;

    private static List<DataQuery> sCommentQuery = new ArrayList<DataQuery>();
    
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
    
    private Button mCommentTipEdt;
    
    public static void setCommentQuery(DataQuery commentQuery) {
        synchronized (sCommentQuery) {
            sCommentQuery.add(commentQuery);
        }
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
        
        mTitleTxv.setText(R.string.all_comment);
        mRightTxv.setVisibility(View.GONE);
        
        synchronized (sCommentQuery) {
            int size = sCommentQuery.size();
            if (size > 0) {
                DataQuery dataQuery = sCommentQuery.get(size-1);
                mCommentLsv.onRefreshComplete(true);
                mCommentLsv.setFooterSpringback(false);
                setData(dataQuery);
                mCommentAdapter.notifyDataSetChanged();

                POI poi = dataQuery.getPOI();
                long attribute = poi.getAttribute();
                if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0) {
                    mCommentTipEdt.setHint(R.string.comment_tip_hit1);
                } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
                    mCommentTipEdt.setHint(R.string.comment_tip_hit1);
                } else {
                    mCommentTipEdt.setHint(R.string.comment_tip_hit0);
                }
            } else {
                finish();
            }
        }
    }

    protected void findViews() {
        super.findViews();
        mCommentLsv = (SpringbackListView)findViewById(R.id.comment_lsv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
//        mCommentLsv.addHeaderView(v);
//        v = mLayoutInflater.inflate(R.layout.loading, null);
        mCommentLsv.addFooterView(v);
        mCommentTipEdt = (Button) findViewById(R.id.comment_tip_btn);
    }
    
    protected void setListener() {
        super.setListener();
        mCommentLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                turnPage(isHeader);
                mActionLog.addAction(ActionLog.POICommentListClickMore);
            }
        });

        mCommentTipEdt.setOnTouchListener(new OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mActionLog.addAction(ActionLog.POICommentListClickInputBox);
                    if (mCommentQuery != null) {
                        POI poi = mCommentQuery.getPOI();
                        if (poi != null) {
                            boolean isMe = false;
                            long attribute = poi.getAttribute();
                            if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0) {
                                isMe = true;
                            } else if ((attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
                                isMe = true;
                            }
                            if (poi.getStatus() < 0) {
                                int resId;
                                if (isMe) {
                                    resId = R.string.poi_comment_poi_invalid_not_update;
                                } else {
                                    resId = R.string.poi_comment_poi_invalid_not_create;
                                }
                                final AlertDialog alertDialog = CommonUtils.getAlertDialog(mThis);
                                alertDialog.setMessage(mThis.getString(resId));
                                alertDialog.setButton(mThis.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                    
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        alertDialog.dismiss();
                                    }
                                });
                                alertDialog.show();
                            } else {
                                POIComment.setPOI(mCommentQuery.getPOI(), R.id.activity_poi_comment_list, isMe ? POIComment.STATUS_MODIFY : POIComment.STATUS_NEW);
                                Intent intent = new Intent();
                                intent.setClass(mThis, POIComment.class);
                                mThis.startActivityForResult(intent, REQUEST_CODE_COMMENT);
                            }
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

        mActionLog.addAction(ActionLog.SearchResultNextPage);

        DataQuery dataQuery = new DataQuery(mThis);
        POI requestPOI = mCommentQuery.getPOI();
        int cityId = mCommentQuery.getCityId();
        Hashtable<String, String> criteria = mCommentQuery.getCriteria();
        if (mCommentArrayList.size() > 0) {
            mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
            if (isHeader) {
                Comment comment = mCommentArrayList.get(0);
                if (comment.getAttribute() > 0) {
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
                            long attribute = poi.getAttribute();
                            int index = 0;
                            if ((attribute & POI.ATTRIBUTE_COMMENT_USER) > 0 || (attribute & POI.ATTRIBUTE_COMMENT_ANONYMOUS) > 0) {
                                index = 1;
                            }
                            mCommentArrayList.addAll(index, list);
                        } else {
                            mCommentArrayList.addAll(list);
                        }
                        ((CommentResponse)mCommentQuery.getResponse()).getList().getList().addAll(list);
                        poi.updateAttribute();
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
                CommentList commentList = commentResponse.getList();
                if (commentList != null) {
                    List<Comment> commentArrayList = commentList.getList();
                    if (commentArrayList != null && commentArrayList.size() > 0) {
                        mCommentQuery = dataQuery;
                        mCommentArrayList.addAll(commentArrayList);
                        poi.updateAttribute();
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
                float grade = comment.getGrade()/2;
                gradeRtb.setRating(grade);
                
                final POI poi = mCommentQuery.getPOI();
                if (comment.getAttribute() > 0) {
                    view.setBackgroundResource(R.drawable.list_middle);
                    authorTxv.setText(R.string.me);
                    authorTxv.setTextColor(0xff009CFF);
                    poi.setMyComment(comment);
                    view.setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View arg0) {
                            mActionLog.addAction(ActionLog.POICommentListClickMyComment);
                            if (poi.getStatus() >= 0) {
                                Intent intent = new Intent();
                                intent.setClass(mThis, POIComment.class);
                                POIComment.setPOI(poi, mId, POIComment.STATUS_MODIFY);
                                mThis.startActivityForResult(intent, R.id.activity_poi_comment);
                            } else {
                                final AlertDialog alertDialog = CommonUtils.getAlertDialog(mThis);
                                alertDialog.setMessage(mThis.getString(R.string.comment_poi_invalid_hit));
                                alertDialog.setButton(mThis.getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                    
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        alertDialog.dismiss();
                                    }
                                });
                                alertDialog.show();
                            }
                        }
                    });
                } else {
                    view.setBackgroundResource(R.drawable.list_middle_normal);
                    authorTxv.setTextColor(0xff000000);
                    authorTxv.setText(comment.getUser());
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
                            mActionLog.addAction(ActionLog.POICommentListClickUrl, url);
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
        synchronized (sCommentQuery) {
            int size = sCommentQuery.size();
            if (size > 0) {
                sCommentQuery.remove(size-1);
            }
        }
        super.finish();        
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();

        boolean exit = true;
        if (baseQuery.getCriteria().containsKey(DataQuery.SERVER_PARAMETER_TIME)) {
            mCommentLsv.onRefreshComplete(false);
            mCommentLsv.setFooterSpringback(true);
            exit = false;
        }
        if (BaseActivity.checkReLogin(baseQuery, mThis, mSourceUserHome, mId, mId, mId, mCancelLoginListener)) {
            isReLogin = true;
            return;
        } else if (BaseActivity.checkResponseCode(baseQuery, mThis, null, true, mThis, exit)) {
            return;
        }
        
        DataQuery commentQuery = (DataQuery)(baseQuery);
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
        mCommentLsv.setFooterSpringback(false);
        
        setData(commentQuery);
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

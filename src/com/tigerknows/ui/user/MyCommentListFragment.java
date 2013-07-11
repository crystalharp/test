/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.ui.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import com.decarta.Globals;
import com.decarta.android.exception.APIException;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TKConfig;
import com.tigerknows.android.os.TKAsyncTask;
import com.tigerknows.common.ActionLog;
import com.tigerknows.model.BaseQuery;
import com.tigerknows.model.Comment;
import com.tigerknows.model.DataOperation;
import com.tigerknows.model.DataQuery;
import com.tigerknows.model.POI;
import com.tigerknows.model.DataOperation.POIQueryResponse;
import com.tigerknows.model.DataQuery.CommentResponse;
import com.tigerknows.model.DataQuery.CommentResponse.CommentList;
import com.tigerknows.ui.BaseActivity;
import com.tigerknows.ui.BaseFragment;
import com.tigerknows.ui.poi.EditCommentActivity;
import com.tigerknows.util.Utility;
import com.tigerknows.widget.SpringbackListView;
import com.tigerknows.widget.SpringbackListView.OnRefreshListener;

/**
 * @author Peng Wenyue
 */
public class MyCommentListFragment extends BaseFragment {
    
    static final String TAG = "MyCommentListFragment";

    public MyCommentListFragment(Sphinx sphinx) {
        super(sphinx);
        // TODO Auto-generated constructor stub
    }
    
    private SpringbackListView mCommentLsv = null;
    private TextView mEmptyTxv;
    private DataQuery mDataQuery;
    private List<Comment> mCommentArrayList = new ArrayList<Comment>();
    private CommentAdapter mCommentAdapter;
    private View.OnClickListener mClickPOI = new View.OnClickListener() {
        
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();
            Comment comment = mCommentArrayList.get(position);
            mActionLog.addAction(mActionTag +  ActionLog.MyCommentPOI, position, comment.getPOIName());
            if (comment.getPOIStatus() >= 0) {
                POI poi = comment.getPOI();
                if (poi.getPosition() != null) {
                    mSphinx.showView(R.id.view_poi_detail);
                    mSphinx.getPOIDetailFragment().setData(poi, position);
                } else {
                    Hashtable<String, String> criteria = new Hashtable<String, String>();
                    criteria.put(DataOperation.SERVER_PARAMETER_DATA_TYPE, DataOperation.DATA_TYPE_POI);
                    criteria.put(DataOperation.SERVER_PARAMETER_SUB_DATA_TYPE, DataOperation.SUB_DATA_TYPE_POI);
                    criteria.put(DataOperation.SERVER_PARAMETER_OPERATION_CODE, DataOperation.OPERATION_CODE_QUERY);
                    criteria.put(DataOperation.SERVER_PARAMETER_DATA_UID, poi.getUUID());
                    criteria.put(DataOperation.SERVER_PARAMETER_NEED_FIELD, POI.NEED_FIELD);
                    DataOperation poiQuery = new DataOperation(mSphinx);
                    poiQuery.setup(criteria, Integer.parseInt(comment.getPoiCityId()), getId(), getId(), mSphinx.getString(R.string.doing_and_wait));
                    mSphinx.queryStart(poiQuery);
                }
            } else {
                Utility.showNormalDialog(mSphinx, 
                        mSphinx.getString(R.string.poi_invalid));
            }
        }
    };
    
    private Runnable mTurnPageRun = new Runnable() {
        
        @Override
        public void run() {
            if (mCommentLsv.getLastVisiblePosition() >= mCommentLsv.getCount()-2 &&
                    mCommentLsv.getFirstVisiblePosition() == 0) {
                mCommentLsv.getView(false).performClick();
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.MyComment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        
        mRootView = mLayoutInflater.inflate(R.layout.user_my_comment_list, container, false);
        
        findViews();
        setListener();

        mCommentAdapter = new CommentAdapter(mSphinx, mCommentArrayList);
        mCommentLsv.setAdapter(mCommentAdapter);
        
        return mRootView;
    }

    protected void findViews() {
        mCommentLsv = (SpringbackListView)mRootView.findViewById(R.id.comment_lsv);
        mEmptyTxv = (TextView)mRootView.findViewById(R.id.empty_txv);
        View v = mLayoutInflater.inflate(R.layout.loading, null);
//        mCommentLsv.addHeaderView(v);
//        v = mLayoutInflater.inflate(R.layout.loading, null);
        mCommentLsv.addFooterView(v);
    }
    
    protected void setListener() {

        mCommentLsv.setOnRefreshListener(new OnRefreshListener() {
            
            @Override
            public void onRefresh(boolean isHeader) {
                mActionLog.addAction(mActionTag+ActionLog.ListViewItemMore);
                turnPage(isHeader);
            }
        });

        mCommentLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (position < adapterView.getCount()) {
                    mActionLog.addAction(mActionTag + ActionLog.ListViewItem, position);
                    Comment comment = (Comment) adapterView.getAdapter().getItem(position);
                    if (comment != null) {
                        if (comment.getPOI().getStatus() <= 0) {
                            Utility.showNormalDialog(mSphinx, 
                                    mSphinx.getString(R.string.poi_comment_poi_invalid_not_update));
                        } else {
                            EditCommentActivity.setPOI(comment.getPOI(), getId(), EditCommentActivity.STATUS_MODIFY);
                            mSphinx.showView(R.id.activity_poi_edit_comment);
                        }
                    }
                }
            }
        });
    }
    
    public void turnPage(boolean isHeader){
        synchronized (this) {
        mCommentLsv.changeHeaderViewByState(false, SpringbackListView.REFRESHING);
        DataQuery lastDataQuery = mDataQuery;
        if (lastDataQuery == null) {
            DataQuery dataQuery = new DataQuery(mSphinx);
            Hashtable<String, String> criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
            criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_USER);
            dataQuery.setup(criteria, Globals.getCurrentCityInfo().getId(), getId(), getId(), null);
            mSphinx.queryStart(dataQuery);
            return;
        }

        DataQuery dataQuery = new DataQuery(mSphinx);
        int cityId = lastDataQuery.getCityId();
        Hashtable<String, String> criteria;
        if (mCommentArrayList.size() > 0) {
            criteria = lastDataQuery.getCriteria();
            if (isHeader) {
                criteria.put(DataQuery.SERVER_PARAMETER_TIME, mCommentArrayList.get(0).getTime());
                criteria.put(DataQuery.SERVER_PARAMETER_DIRECTION, DataQuery.DIRECTION_AFTER);
            } else {
                criteria.put(DataQuery.SERVER_PARAMETER_TIME, mCommentArrayList.get(mCommentArrayList.size()-1).getTime());
                criteria.put(DataQuery.SERVER_PARAMETER_DIRECTION, DataQuery.DIRECTION_BEFORE);
            }
        } else {
            criteria = new Hashtable<String, String>();
            criteria.put(DataQuery.SERVER_PARAMETER_DATA_TYPE, DataQuery.DATA_TYPE_DIANPING);
            criteria.put(DataQuery.SERVER_PARAMETER_REFER, DataQuery.REFER_USER);
        }
        dataQuery.setup(criteria, cityId, getId(), getId(), null, true, true, null);
        mSphinx.queryStart(dataQuery);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void onResume() {
        super.onResume();
        
        mTitleBtn.setText(R.string.my_comment);
        mRightBtn.setVisibility(View.INVISIBLE);
        
        mEmptyTxv.setVisibility(View.GONE);
        if (mCommentArrayList.isEmpty()) {    
            mCommentLsv.setFooterSpringback(false);
            turnPage(false);
        } else {
            if (mCommentLsv.isFooterSpringback()) {
                mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
            }
            for(int i  = 0, size = mCommentArrayList.size(); i < size; i++) {
                Comment comment = mCommentArrayList.get(i);
                Comment commentnew = comment.getPOI().getMyComment();
                if (commentnew != null && commentnew != comment) {
                    try {
                        commentnew.setData(null);
                        comment.init(commentnew.getData(), true);   //TODO 需要将false改为true
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            Collections.sort(mCommentArrayList, Comment.COMPARATOR_ONLY_TIME);
            mCommentAdapter.notifyDataSetChanged();
        }
    }
    
    private class CommentAdapter extends ArrayAdapter<Comment>{
        private static final int Resource_Id = R.layout.user_my_comment_list_item;

        public CommentAdapter(Context context, List<Comment> commentList) {
            super(context, Resource_Id, commentList);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mLayoutInflater.inflate(Resource_Id, parent, false);
            } else {
                view = convertView;
            }

            try {
                TextView poiNameTxv = (TextView)view.findViewById(R.id.poi_name_txv);
                RatingBar gradeRtb = (RatingBar)view.findViewById(R.id.grade_rtb);
                TextView authorTxv = (TextView) view.findViewById(R.id.author_txv);
                TextView dateTxv = (TextView) view.findViewById(R.id.date_txv);
                TextView commentTxv = (TextView) view.findViewById(R.id.comment_txv);
                TextView srcTxv = (TextView) view.findViewById(R.id.src_txv);
                
                final Comment comment = getItem(position);
                
                poiNameTxv.setText(comment.getPOI().getName());
                poiNameTxv.setTag(position);
                poiNameTxv.setOnClickListener(mClickPOI);
                
                float grade = comment.getGrade()/2.0f;
                gradeRtb.setRating(grade);

                authorTxv.setVisibility(View.GONE);
                String time = comment.getTime();
                if (!TextUtils.isEmpty(time) && time.length() >= 10) {
                    dateTxv.setText(time.subSequence(0, 10));
                } else {
                    dateTxv.setText(null);
                }
                String body = comment.getContent();
                commentTxv.setText(body);
                srcTxv.setVisibility(View.GONE);
            } catch (Exception e) {
            }
            
            return view;
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mCommentArrayList.clear();
        mCommentAdapter.notifyDataSetChanged();
        mDataQuery = null;
        mEmptyTxv.setVisibility(View.GONE);
    }

    @Override
    public void onCancelled(TKAsyncTask tkAsyncTask) {
        super.onCancelled(tkAsyncTask);
        mCommentLsv.onRefreshComplete(false);
    }

    @Override
    public void onPostExecute(TKAsyncTask tkAsyncTask) {   
        super.onPostExecute(tkAsyncTask);
        BaseQuery baseQuery = tkAsyncTask.getBaseQuery();   
        if (BaseActivity.checkReLogin(baseQuery, mSphinx, mSphinx.uiStackContains(R.id.view_user_home), getId(), R.id.view_user_home, R.id.view_more_home, null)) {
            isReLogin = true;
            return;
        }
        
        if (baseQuery instanceof DataOperation) {
            if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, false)) {
                return;
            }
            POIQueryResponse poiQueryResponse = (POIQueryResponse) baseQuery.getResponse();
            POI poi = poiQueryResponse.getPOI();
            for(int i = mCommentArrayList.size()-1; i >= 0; i--) {
                Comment comment = mCommentArrayList.get(i);
                POI commentPOI = comment.getPOI();
                String uuid = commentPOI.getUUID();
                if (uuid != null && uuid.equals(poi.getUUID())) {
                    try {
                        commentPOI.init(poi.getData(), false);
                        commentPOI.setMyComment(comment);
                        mSphinx.showView(R.id.view_poi_detail);
                        mSphinx.getPOIDetailFragment().setData(commentPOI, i);
                        return;
                    } catch (APIException e) {
                        e.printStackTrace();
                    }
                }
            }
            return;
            
        } else if (baseQuery instanceof DataQuery) { 

            boolean exit = true;
            if (baseQuery.getCriteria().containsKey(DataQuery.SERVER_PARAMETER_TIME)) {
                mCommentLsv.onRefreshComplete(false);
                mCommentLsv.setFooterSpringback(true);
                exit = false;
            }    
            if (BaseActivity.checkResponseCode(baseQuery, mSphinx, null, true, this, exit)) {
                return;
            }
            DataQuery dataQuery = (DataQuery)baseQuery;
            boolean isHeader = false;
            if (dataQuery.isTurnPage() == false) {
                mCommentLsv.onRefreshComplete(false);
            } else {
                Hashtable<String, String> criteria = dataQuery.getCriteria();
                if (criteria.containsKey(DataQuery.SERVER_PARAMETER_DIRECTION)) {
                    String direction = criteria.get(DataQuery.SERVER_PARAMETER_DIRECTION);
                    if (DataQuery.DIRECTION_AFTER.equals(direction)) {
                        isHeader = true;
                    } else if (DataQuery.DIRECTION_BEFORE.equals(direction)) {
                        isHeader = false;
                    }
                }
                mCommentLsv.onRefreshComplete(isHeader);
            }
            mCommentLsv.setFooterSpringback(false);
    
            if (dataQuery.isTurnPage() == false) {
                mDataQuery = dataQuery;
            }
            
            CommentResponse commentResponse = (CommentResponse)dataQuery.getResponse();
            CommentList commentList = commentResponse.getList();
            
            if (dataQuery.isTurnPage()) {
                List<Comment> list = commentList.getList();
                if (isHeader) {
                    if (list != null) {
                        mCommentArrayList.addAll(0, list);
                    }
                } else {
                    if (list != null) {
                        mCommentArrayList.addAll(list);
                    }
                }
                mCommentAdapter.notifyDataSetChanged();
                if (list != null && list.size() >= TKConfig.getPageSize()) {
                    mCommentLsv.setFooterSpringback(true);
                }
            } else if (commentList != null && commentList.getList() != null && commentList.getList().size() > 0) {
                mCommentArrayList.clear();
                List<Comment> list = commentList.getList();
                if (list != null) {
                    mCommentArrayList.addAll(list);
                }
                mCommentAdapter.notifyDataSetChanged();
                if (list != null && list.size() >= TKConfig.getPageSize()) {
                    mCommentLsv.setFooterSpringback(true);
                }
            }
            
            if (mCommentArrayList.isEmpty()) {
                mCommentLsv.setVisibility(View.GONE);
                mEmptyTxv.setText(R.string.poi_comment_go_tip);
                mEmptyTxv.setVisibility(View.VISIBLE);
            } else {
                mCommentLsv.setVisibility(View.VISIBLE);
                mEmptyTxv.setVisibility(View.GONE);
            }
            
            if (mCommentLsv.isFooterSpringback()) {
                mSphinx.getHandler().postDelayed(mTurnPageRun, 1000);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public void refreshComment() {
        if (mCommentArrayList != null) {
            Collections.sort(mCommentArrayList, Comment.COMPARATOR_ONLY_TIME);
            mCommentAdapter.notifyDataSetChanged();
        }
    }
}

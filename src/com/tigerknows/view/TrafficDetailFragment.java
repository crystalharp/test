/*
 * Copyright (C) 2010 pengwenyue@tigerknows.com
 */

package com.tigerknows.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.decarta.android.location.Position;
import com.decarta.android.util.LogWrapper;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.TransferErrorRecovery;
import com.tigerknows.maps.TrafficOverlayHelper;
import com.tigerknows.model.BaseData;
import com.tigerknows.model.TrafficModel;
import com.tigerknows.model.TrafficModel.Plan;
import com.tigerknows.provider.Tigerknows;
import com.tigerknows.util.CommonUtils;
import com.tigerknows.util.NavigationSplitJointRule;
import com.tigerknows.util.ShareTextUtil;
import com.tigerknows.util.WidgetUtils;
import com.tigerknows.view.ResultMapFragment.TitlePopupArrayAdapter;

public class TrafficDetailFragment extends BaseFragment implements View.OnClickListener{
    
    public TrafficDetailFragment(Sphinx sphinx) {
        super(sphinx);
    }

    private static final int SHOW_TYPE_TRANSFER = Plan.Step.TYPE_TRANSFER;
    
    private static final int SHOW_TYPE_DRVIE = Plan.Step.TYPE_DRIVE;
    
    private static final int SHOW_TYPE_WALK = Plan.Step.TYPE_WALK;
    
	private static final int TYPE_RESULT_LIST_START = 6;

	private static final int TYPE_RESULT_LIST_END = 7;
        
    private ListAdapter mResultAdapter;

    private TextView mStartTxv = null;

    private TextView mEndTxv = null;
    
    private TextView mLengthTxv = null;
    
    private ImageView mShadowImv = null;
    
    private ListView mResultLsv = null;
    
    private int mShowType = -1;

    private Plan plan = null;
    
    private int mFootLayoutId = R.layout.traffic_fav_share2;
    
    private int mChildLayoutId = R.layout.traffic_child_traffic;
    
    private List<Plan> mPlanList = null;
    
    private List<String> mTitlePopupList = new ArrayList<String>();
    
    private TitlePopupArrayAdapter mTitlePopupArrayAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionTag = ActionLog.TrafficDetail;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        LogWrapper.d(BaseFragment.TAG, "onCreateView()"+mActionTag);
        
        mRootView = mLayoutInflater.inflate(R.layout.traffic_result, container, false);
        findViews();
        setListener();
        
        mTitlePopupArrayAdapter = new TitlePopupArrayAdapter(mSphinx, mTitlePopupList);
        
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mResultAdapter = new StringListAdapter(mContext);
        mResultLsv.setAdapter(mResultAdapter);
        
        mStartTxv.setText(plan.getStart().getName());
        mEndTxv.setText(plan.getEnd().getName());
        
        switch(mShowType) {
        case SHOW_TYPE_TRANSFER:
        	mRightBtn.setBackgroundResource(R.drawable.ic_view_map);
        	mRightBtn.setOnClickListener(this);
            mTitleBtn.setText(mContext.getString(R.string.title_transfer_plan));
            break;
        case SHOW_TYPE_DRVIE:
            mTitleBtn.setText(mContext.getString(R.string.title_drive_plan));
            break;
        case SHOW_TYPE_WALK:
            mTitleBtn.setText(mContext.getString(R.string.title_walk_plan));
            break;
        default:
        }
        
        if (mPlanList != null) {
        	//有内容，需要弹出顶部切换菜单
//	        mTitleBtn.setText(mContext.getString(R.string.title_transfer_plan));
        	mTitleBtn.setText(mTitlePopupArrayAdapter.mSelectedItem);
	        mTitleBtn.setBackgroundResource(R.drawable.btn_title_popup);
	        mTitleBtn.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
			        mTitleFragment.showPopupWindow(mTitlePopupArrayAdapter, mTitlePopupOnItemClickListener);
			        mTitlePopupArrayAdapter.notifyDataSetChanged();
				}
	        });
        } else {
        	//不用顶部弹出切换
        	mTitleBtn.setText(mContext.getString(R.string.title_busline_line));
        }
        
        if (mShowType == SHOW_TYPE_TRANSFER) {
            mLengthTxv.setVisibility(View.GONE);
            mShadowImv.setVisibility(View.VISIBLE);
            mFootLayoutId = R.layout.traffic_fav_share2;
        } else {
            mLengthTxv.setVisibility(View.VISIBLE);
            mShadowImv.setVisibility(View.GONE);
            mFootLayoutId = R.layout.traffic_fav_share;
            mLengthTxv.setText(plan.getLengthStr(mSphinx));
        }
        
        history();
    }
    
    private OnItemClickListener mTitlePopupOnItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
            mTitleFragment.dismissPopupWindow();
            Plan clickedPlan = mPlanList.get(position);
            if (clickedPlan.equals(plan)) {
            	return;
            } else {
            	setData(clickedPlan);
            	onResume();            	
            }

        }
    };

    @Override
    public void onPause() {
        super.onPause();
    }
   
    protected void findViews() {
        mStartTxv = (TextView)mRootView.findViewById(R.id.start_txv);
        mEndTxv = (TextView)mRootView.findViewById(R.id.end_txv);
        mLengthTxv = (TextView)mRootView.findViewById(R.id.length_txv);
        mResultLsv = (ListView)mRootView.findViewById(R.id.result_lsv);
        mShadowImv = (ImageView)mRootView.findViewById(R.id.shadow2);
    }

    protected void setListener() {
        mResultLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

        	@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mActionLog.addAction(ActionLog.TrafficDetailStep, position);

				// 绘制交通图层
                viewMap();
                // 将地图平移到某一坐标点, 并缩放至某一级别
                if (position == 0) {
                	//mResultAdapter中起点项的处理
                	TrafficOverlayHelper.panToPosition(mSphinx.getHandler(), plan.getStepList().get(position).getPositionList().get(0), 
                		mSphinx.getMapView());
                } else if (position > plan.getStepList().size()) {
                	//mResultAdapter中终点项的处理
                	TrafficOverlayHelper.panToPosition(mSphinx.getHandler(), plan.getStepList().get(position - 2).getPositionList().get(0), 
                    		mSphinx.getMapView());
                } else {
                	TrafficOverlayHelper.panToPosition(mSphinx.getHandler(), plan.getStepList().get(position - 1).getPositionList().get(0), 
                		mSphinx.getMapView());
                }
                // 比例尺恢复到默认的200m
                mSphinx.getMapView().zoomTo(14);
            }
        });
    }
    
	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == R.id.right_btn) {
		    mActionLog.addAction(ActionLog.TrafficDetailMapBtn);
			// 绘制交通图层
			viewMap();
			// 将地图缩放至可以显示完整的交通路径, 并平移到交通路径中心点
			TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
		}
	}
    
    public void setData(Plan plan) {
    	
    	if (plan == null)
    		return;

        this.plan = plan;
        mShowType = plan.getType();

        if (mShowType == SHOW_TYPE_TRANSFER) {
	        mTitlePopupList.clear();
	        mTitlePopupArrayAdapter.mSelectedItem = mSphinx.getTrafficDetailFragment().getData().getTitle(mSphinx);
	        List<Plan> list = mSphinx.getTrafficResultFragment().getData();
	        for(int i = 0, size = list.size(); i < size; i++) {
	            mTitlePopupList.add(list.get(i).getTitle(mSphinx));
	        }
	        this.mPlanList = mSphinx.getTrafficResultFragment().getData();
        }
        
    }

    public static class StepViewHolder {
    	public ImageView image;
    	public TextView textView;
    }
    
    public static class ActionViewHolder {
    	public RelativeLayout favorite;
    	public RelativeLayout share;
    	public RelativeLayout errorrecovery;
    }

    class StringListAdapter extends BaseAdapter{

    	private static final int TYPE_STEP = 0;
    	
    	private static final int TYPE_ACTION = TYPE_STEP + 1;
    	
    	private static final int TYPE_COUNT = TYPE_ACTION + 1;
    	
        private List<Integer> types = new ArrayList<Integer>();
        
        private List<CharSequence> strList = new ArrayList<CharSequence>();

        public StringListAdapter(Context context) {
        	super();
            strList = NavigationSplitJointRule.splitJoint(context, mShowType, plan);
            types = (ArrayList<Integer>)plan.getStepTypeList(mContext);

            //列表前面显示起点，后面显示终点
            strList.add(0, plan.getStart().getName());
            strList.add(plan.getEnd().getName());
            types.add(0, TYPE_RESULT_LIST_START);
            types.add(TYPE_RESULT_LIST_END);
        }

        @Override
        public int getCount() {
            return strList.size() + 1;
        }

        private CharSequence getItemContent(int position) {
            return strList.get(position);
        }
                
        @Override
		public int getItemViewType(int position) {
        	if(position == getCount() - 1) {
        		return TYPE_ACTION;
        	}
			return TYPE_STEP;
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_COUNT;
		}

		@Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            int type = getItemViewType(position);
			if(convertView == null) {
				switch(type){
				case TYPE_STEP:
					convertView = mLayoutInflater.inflate(mChildLayoutId, parent, false);
					StepViewHolder stepHolder = new StepViewHolder();
					stepHolder.image = (ImageView)convertView.findViewById(R.id.image1);
					stepHolder.textView = (TextView)convertView.findViewById(R.id.text);
					convertView.setTag(stepHolder);
					break;
				case TYPE_ACTION:
					convertView = mLayoutInflater.inflate(mFootLayoutId, parent, false);
					ActionViewHolder actionHolder = new ActionViewHolder();
					actionHolder.favorite = (RelativeLayout)convertView.findViewById(R.id.favorite_rll);
					actionHolder.share = (RelativeLayout)convertView.findViewById(R.id.share_rll);
					actionHolder.errorrecovery = (RelativeLayout)convertView.findViewById(R.id.errorrecovery_rll);
					convertView.setTag(actionHolder);
					break;
				default:
				}
			}
			
			switch(type){
			case TYPE_STEP:
				StepViewHolder stepHolder = (StepViewHolder)convertView.getTag();
				
				Integer stepType = types.get(position);
                stepHolder.image.setBackgroundDrawable(getDrawable(stepType));
				stepHolder.textView.setText(getItemContent(position));
				stepHolder.textView.setTextColor(Color.parseColor("#000000"));
			
				break;
			case TYPE_ACTION:
				ActionViewHolder actionHolder = (ActionViewHolder)convertView.getTag();
				
				if (mShowType == SHOW_TYPE_TRANSFER) {
					actionHolder.errorrecovery.setVisibility(View.VISIBLE);
				} else {
					actionHolder.errorrecovery.setVisibility(View.GONE);
				}
				
				actionHolder.favorite.setOnClickListener(new ResultOnClickListener());
				actionHolder.share.setOnClickListener(new ResultOnClickListener());
				actionHolder.errorrecovery.setOnClickListener(new ResultOnClickListener());
				setFavoriteState(actionHolder.favorite, plan.checkFavorite(mContext));
				break;
			default:
			}
				
			return convertView;

        }
        
        private Drawable getDrawable(int steptype){
            if (mShowType == SHOW_TYPE_TRANSFER) {
            	return getTransferDrawable(steptype);
            } else {
            	return getDirectionDrawable(steptype);
            }
        }
        
        private Drawable getTransferDrawable(int steptype) {
        	int res = R.drawable.icon_marker_bus;
            
            switch(steptype){
            case TrafficModel.Plan.Step.TYPE_TRANSFER:
                res = R.drawable.icon_marker_bus;
                break;
            case TrafficModel.Plan.Step.TYPE_WALK:
                res = R.drawable.icon_marker_walk;
                break;
			case TYPE_RESULT_LIST_START:
				res = R.drawable.icon_marker_start;
				break;
            case TYPE_RESULT_LIST_END:
            	res = R.drawable.icon_marker_end;
            	break;
            default:
                
            }
            return mContext.getResources().getDrawable(res);
        }
        
        private Drawable getDirectionDrawable(int steptype) {
        	int res = R.drawable.icon_marker_drive_forward;
            
            switch(steptype){
            
            case TrafficModel.Plan.Step.TYPE_FORWARD:
                res = R.drawable.icon_marker_drive_forward;
                break;
            case TrafficModel.Plan.Step.TYPE_BACK:
                res = R.drawable.icon_marker_drive_back;
                break;
            case TrafficModel.Plan.Step.TYPE_ROUND_LEFT:
                res = R.drawable.icon_marker_drive_round_left;
                break;
            case TrafficModel.Plan.Step.TYPE_LEFT:
                res = R.drawable.icon_marker_drive_left;
                break;
            case TrafficModel.Plan.Step.TYPE_ROUND_RIGHT:
                res = R.drawable.icon_marker_drive_round_right;
                break;
            case TrafficModel.Plan.Step.TYPE_RIGHT:
                res = R.drawable.icon_marker_drive_right;
                break;
            default:
                
            }
            return mContext.getResources().getDrawable(res);
        }

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		@Override
		public boolean isEnabled(int position) {
			if(position == this.getCount()-1)
              return false;
          return true;
		}
    }

    // 可以提取出来
    private class ResultOnClickListener implements View.OnClickListener {
    	
		@Override
		public void onClick(View v) {

            if (v.getId() == R.id.share_rll) {
                //弹出分享对话框
                mActionLog.addAction(ActionLog.TrafficDetailShare);
                share(plan);
            } else if (v.getId() == R.id.favorite_rll) {
            	favorite(plan, v);
            } else if (v.getId() == R.id.errorrecovery_rll) {
                mActionLog.addAction(ActionLog.TrafficDetailErrorRecovery);
            	TransferErrorRecovery.addTarget(plan);
            	mSphinx.showView(R.id.activity_traffic_error_recovery);
            }
		}

		public void favorite(final BaseData data, final View v) {
			if (data == null)
				return ;
			
	        boolean isFavorite = data.checkFavorite(mContext);
	        if (isFavorite) {
	        	CommonUtils.showNormalDialog(mSphinx, 
                        mContext.getString(R.string.prompt),
                        mContext.getString(R.string.cancel_favorite_tip),
                        new DialogInterface.OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface arg0, int id) {
                                if (id == DialogInterface.BUTTON_POSITIVE) {
                                    mActionLog.addAction(ActionLog.TrafficDetailCancelFav);
                                    setFavoriteState(v, false);
                                    data.deleteFavorite(mContext);
                                }
                            }
                        });
	        } else {
	        	mActionLog.addAction(ActionLog.TrafficDetailFavorite);
	        	setFavoriteState(v, true);
				data.writeToDatabases(mContext, -1, Tigerknows.STORE_TYPE_FAVORITE);
                Toast.makeText(mSphinx, R.string.favorite_toast, Toast.LENGTH_LONG).show();
	        }
	    }
    }
    
    public void share(final Plan plan) {
        if(plan == null)
            return;
                
    	String smsContent = "";
    	String weiboContent = "";
    	String qzoneContent = "";
    	if (plan != null) {
    		if (SHOW_TYPE_TRANSFER == mShowType) {
                weiboContent = ShareTextUtil.shareTrafficTransferWeiboContent(plan, mContext);
                smsContent = ShareTextUtil.shareTrafficTransferSmsContent(plan, mContext);
                qzoneContent = ShareTextUtil.shareTrafficTransferQzoneContent(plan, mContext);
            } else if (SHOW_TYPE_DRVIE == mShowType) {
                weiboContent = ShareTextUtil.shareTrafficDriveWeiboContent(plan, mContext);
                smsContent = ShareTextUtil.shareTrafficDriveSmsContent(plan, mContext);
                qzoneContent = ShareTextUtil.shareTrafficDriveQzoneContent(plan, mContext);
            } else if (SHOW_TYPE_WALK == mShowType) {
                weiboContent = ShareTextUtil.shareTrafficWalkWeiboContnet(plan, mContext);
                smsContent = ShareTextUtil.shareTrafficWalkSmsContnet(plan, mContext);
                qzoneContent = ShareTextUtil.shareTrafficWalkQzoneContnet(plan, mContext);
            }
    	}
    	
    	mSphinx.clearMap();
    	TrafficOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), plan, mShowType);
    	Position position = TrafficOverlayHelper.panToViewWholeOverlay(plan, mSphinx.getMapView(), (Activity)mSphinx);
    	
    	WidgetUtils.share(mSphinx, smsContent, weiboContent, qzoneContent, position);
    }

    private void setFavoriteState(View v, boolean favoriteYet) {
    	
    	ImageView favorite = (ImageView)v.findViewById(R.id.image);
    	TextView text = (TextView)v.findViewById(R.id.favorite);
    	
    	if (favoriteYet) {
    	    favorite.setBackgroundResource(R.drawable.ic_favorite);
        	text.setText(mContext.getResources().getString(R.string.favorite_yet));	
    	} else {
        	favorite.setBackgroundResource(R.drawable.ic_favorite_cancel);
        	text.setText(mContext.getResources().getString(R.string.favorite));
    	}
    	
    }
    
    public void viewMap() {

        if (plan != null) {
            String actionTag = "";
        	switch(mShowType) {
        	case SHOW_TYPE_TRANSFER:
        	    actionTag = ActionLog.MapTrafficTransfer;
        		break;
        	case SHOW_TYPE_DRVIE:
                actionTag = ActionLog.MapTrafficDrive;
        		break;
        	case SHOW_TYPE_WALK:
                actionTag = ActionLog.MapTrafficWalk;
        		break;
        	default:
        			
        	}

        	mSphinx.clearMap();
        	TrafficOverlayHelper.drawOverlay(mSphinx, mSphinx.getHandler(), mSphinx.getMapView(), plan, mShowType);
        	mSphinx.setPreviousNextViewVisible();
        	
            mSphinx.getResultMapFragment().setData(mContext.getString(R.string.title_traffic_result_map), actionTag);
            mSphinx.showView(R.id.view_result_map);
        }
    }

    private void history() {
        if (plan != null) {
            plan.updateHistory(mContext);
        }
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                plan = null;
                break;
                
            default:
                break;
        }
        return false;
    }

    public Plan getData() {
        return plan;
    }
}

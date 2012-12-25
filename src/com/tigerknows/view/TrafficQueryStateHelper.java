package com.tigerknows.view;

import android.view.View;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制状态转换(界面转换)]]
 * @author linqingzu
 *
 */
public class TrafficQueryStateHelper {

	private TrafficQueryFragment mQueryFragment;
	
	public TrafficQueryStateHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	public abstract static class TrafficAction implements TrafficViewSTT.Action {
		
		@Override
		public void execute() {
			// TODO Auto-generated method stub
			eventExecute();
			uiExecute();
		}

		@Override
		public void rollback() {
			// TODO Auto-generated method stub
			eventRollback();
			uiRollback();
		}
		
		public abstract void uiExecute();
		
		public abstract void eventExecute();
		
		public abstract void uiRollback();
		
		public abstract void eventRollback();
	}
	
	private class NormalToInputAction extends TrafficAction {

		@Override
		public void uiExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficHome);
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInInputState();
		}

		@Override
		public void uiRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficInput);
			mQueryFragment.mSphinx.getMenuFragment().display();
//			resetNormalStateMap();
			applyInnateProperty(TrafficViewSTT.State.Normal);
		}

		@Override
		public void eventRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInNormalState();
			
//			if (!isEqualsToMapCenter(mCityInfo)) {
//        		mSphinx.getMapView().zoomTo(mCityInfo.getLevel(), getCityCenterPosition(mCityInfo));
//        	}
			mQueryFragment.mMapLocationHelper.showNormalStateMap();
		}

	}
	
	private class NormalToMapAction extends TrafficAction {
	
		@Override
		public void uiExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficHome);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeToMap);
			
			mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
        	
			mQueryFragment.mAnimationHelper.hideBlockAndMenuAnimation();

			applyInnateProperty(TrafficViewSTT.State.Map);
			mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
  		}

		@Override
		public void eventExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInMapState();
		}

		@Override
		public void uiRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficMap);
			mQueryFragment.mMapLocationHelper.resetNormalStateMap();

			mQueryFragment.mLogHelper.checkedRadioButton(R.id.traffic_transfer_rbt);
			mQueryFragment.mAnimationHelper.showBlockAndMenuAnimation();

			applyInnateProperty(TrafficViewSTT.State.Normal);
        	
		}

		@Override
		public void eventRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInNormalState();
		}
		
	}
	
	private class MapToInputAction extends TrafficAction {

		@Override
		public void uiExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficMap);
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInInputState();
		}

		@Override
		public void uiRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficInput);
			/*
			 * 从Input返回Map时, 记录之前Check的Id
			 */
			mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
			
			mQueryFragment.mSphinx.clearMap();
			
			mQueryFragment.getContentView().setVisibility(View.GONE);
			mQueryFragment.mCityTxt.setVisibility(View.VISIBLE);
			applyInnateProperty(TrafficViewSTT.State.Map);
			mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
		}

		@Override
		public void eventRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInMapState();
			mQueryFragment.mMapLocationHelper.resetMapStateMap();
		}
		
	}
	
	private class NormalToSelectPointAction extends TrafficAction {

		@Override
		public void uiExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficHome);
			mQueryFragment.displayCommonTitle();
			mQueryFragment.mRightBtn.setVisibility(View.GONE);
			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
			mQueryFragment.mTitle.setVisibility(View.GONE);
			mQueryFragment.mMenuFragment.hide();
			
			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
		}

		@Override
		public void eventExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInSelectPointState();
		}

		@Override
		public void uiRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.hideCommonTitle();
			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
			mQueryFragment.mMenuFragment.display();
			
			applyInnateProperty(TrafficViewSTT.State.Normal);
		}

		@Override
		public void eventRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.mMapLocationHelper.resetNormalStateMap();
		}
		
	}
	
	private class InputToSelectPointAction extends TrafficAction {

		@Override
		public void uiExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mActionLog.addAction(ActionLog.Dismiss, ActionLog.TrafficInput);
			mQueryFragment.displayCommonTitle();
			mQueryFragment.mRightBtn.setVisibility(View.GONE);
			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
			mQueryFragment.mTitle.setVisibility(View.GONE);
			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
		}

		@Override
		public void eventExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInSelectPointState();
		}

		@Override
		public void uiRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.hideCommonTitle();
			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventRollback() {
			// TODO Auto-generated method stub
		}
		
	}
	
	private class SelectPointToInputAction extends TrafficAction {
		@Override
		public void uiExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.hideCommonTitle();
			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventExecute() {
			// TODO Auto-generated method stub
			mQueryFragment.mEventHelper.applyListenersInInputState();
		}

		@Override
		public void uiRollback() {
			// TODO Auto-generated method stub
			mQueryFragment.displayCommonTitle();
			mQueryFragment.mRightBtn.setVisibility(View.GONE);
			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
			mQueryFragment.mTitle.setVisibility(View.GONE);
			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
		}

		@Override
		public void eventRollback() {
			// TODO Auto-generated method stub
		}
	}
	
	public NormalToInputAction createNormalToInputAction() {
		return new NormalToInputAction();
	}
	
	public NormalToMapAction createNormalToMapAction() {
		return new NormalToMapAction();
	}
	
	public MapToInputAction createMapToInputAction() {
		return new MapToInputAction();
	}
	
	public NormalToSelectPointAction createNormalToSelectPointAction() {
		return new NormalToSelectPointAction();
	}
	
	public InputToSelectPointAction createInputToSelectPointAction() {
		return new InputToSelectPointAction();
	}
	
	public SelectPointToInputAction createSelectPointToInputAction() {
		return new SelectPointToInputAction();
	}
	
	public void applyInnateProperty(TrafficViewSTT.State state) {
		switch(state) {
		case Normal:
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHome);
			applyNormalInnateProperty();
			break;
		case Input:
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficInput);
			applyInputInnateProperty();
			break;
		case Map:
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficMap);
			applyMapInnateProperty();
			break;
		case SelectPoint:
			// 单击选点在行为日志中并不算一个页面，仅是一个动作
			applySelectPointInnateProperty();
			break;
		default:
		}
	}
	
	public void applyNormalInnateProperty() {
		mQueryFragment.mSphinx.clearMap();
		mQueryFragment.mSphinx.setTouchMode(TouchMode.LONG_CLICK);
		
		mQueryFragment.mBlock.requestFocus();
		mQueryFragment.clearAllText();
		mQueryFragment.showStart();
		mQueryFragment.getContentView().setVisibility(View.VISIBLE);
		mQueryFragment.mCityTxt.setVisibility(View.VISIBLE);
		mQueryFragment.mBackBtn.setVisibility(View.GONE);
		mQueryFragment.mSuggestLnl.setVisibility(View.GONE);

		mQueryFragment.mStart.getEdt().setFocusable(false);
		mQueryFragment.mEnd.getEdt().setFocusable(false);
		mQueryFragment.mBusline.getEdt().setFocusable(false);
		mQueryFragment.mStart.getEdt().setFocusableInTouchMode(false);
		mQueryFragment.mEnd.getEdt().setFocusableInTouchMode(false);
		mQueryFragment.mBusline.getEdt().setFocusableInTouchMode(false);
	}
	
	public void applyInputInnateProperty() {
		mQueryFragment.mSphinx.clearMap();
		mQueryFragment.mSphinx.setTouchMode(TouchMode.NORMAL);
		
		mQueryFragment.mSuggestHistoryHelper.checkSuggestAndHistory();
		mQueryFragment.mSuggestLnl.setVisibility(View.VISIBLE);
		mQueryFragment.mSphinx.getMenuFragment().setVisibility(View.GONE);
		mQueryFragment.mCityTxt.setVisibility(View.GONE);
		mQueryFragment.mSuggestLsv.setVisibility(View.GONE);
		mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
		mQueryFragment.getContentView().setVisibility(View.VISIBLE);
		
		mQueryFragment.mStart.getEdt().setFocusable(true);
		mQueryFragment.mEnd.getEdt().setFocusable(true);
		mQueryFragment.mBusline.getEdt().setFocusable(true);
		mQueryFragment.mStart.getEdt().setFocusableInTouchMode(true);
		mQueryFragment.mEnd.getEdt().setFocusableInTouchMode(true);
		mQueryFragment.mBusline.getEdt().setFocusableInTouchMode(true);
	}
	
	public void applyMapInnateProperty() {
		mQueryFragment.mSphinx.setTouchMode(TouchMode.LONG_CLICK);
		
		mQueryFragment.mRadioGroup.clearCheck();
		mQueryFragment.mSuggestLnl.setVisibility(View.GONE);
		mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
		mQueryFragment.enableQueryBtn(false);
		mQueryFragment.mSphinx.getDownloadView().setPadding(0, Util.dip2px(Globals.g_metrics.density, 70), 0, 0);
	}
	
	public void applySelectPointInnateProperty() {
		
		mQueryFragment.getContentView().setVisibility(View.GONE);
		mQueryFragment.mCityTxt.setVisibility(View.VISIBLE);
		mQueryFragment.mMenuFragment.hide();
		mQueryFragment.mSuggestLnl.setVisibility(View.GONE);

		mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
		mQueryFragment.mSphinx.getDownloadView().setPadding(0, Util.dip2px(Globals.g_metrics.density, 30), 0, 0);
	}
}

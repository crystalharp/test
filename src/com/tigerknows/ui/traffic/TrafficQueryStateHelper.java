package com.tigerknows.ui.traffic;

import android.view.View;

import com.decarta.Globals;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.common.ActionLog;

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
			eventExecute();
			uiExecute();
		}

		@Override
		public void rollback() {
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
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeInput;
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventExecute() {
			mQueryFragment.mEventHelper.applyListenersInInputState();
		}

		@Override
		public void uiRollback() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeNormal;
			mQueryFragment.mSphinx.getMenuFragment().display();
//			resetNormalStateMap();
			applyInnateProperty(TrafficViewSTT.State.Normal);
			//返回normal状态时清理掉输入框的光标
			mQueryFragment.mSelectedEdt.getEdt().clearFocus();
		}

		@Override
		public void eventRollback() {
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
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeMap);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeMap;
			
			mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
        	
			mQueryFragment.mAnimationHelper.hideBlockAndMenuAnimation();

			applyInnateProperty(TrafficViewSTT.State.Map);
			mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
  		}

		@Override
		public void eventExecute() {
			mQueryFragment.mEventHelper.applyListenersInMapState();
		}

		@Override
		public void uiRollback() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeMap+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeNormal;
			mQueryFragment.mMapLocationHelper.resetNormalStateMap();

			mQueryFragment.mLogHelper.checkedRadioButton(R.id.traffic_transfer_rbt);
			mQueryFragment.mAnimationHelper.showBlockAndMenuAnimation();

			applyInnateProperty(TrafficViewSTT.State.Normal);
        	
		}

		@Override
		public void eventRollback() {
			mQueryFragment.mEventHelper.applyListenersInNormalState();
		}
		
	}
	
	private class MapToInputAction extends TrafficAction {

		@Override
		public void uiExecute() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeMap+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeInput;
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventExecute() {
			mQueryFragment.mEventHelper.applyListenersInInputState();
		}

		@Override
		public void uiRollback() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeMap);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeMap;
			/*
			 * 从Input返回Map时, 记录之前Check的Id
			 */
			mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
			
			mQueryFragment.mSphinx.clearMap();
			
			mQueryFragment.getContentView().setVisibility(View.GONE);
			mQueryFragment.mCityView.setVisibility(View.VISIBLE);
			mQueryFragment.mShadowWhite.setVisibility(View.GONE);
			applyInnateProperty(TrafficViewSTT.State.Map);
			mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
		}

		@Override
		public void eventRollback() {
			mQueryFragment.mEventHelper.applyListenersInMapState();
			mQueryFragment.mMapLocationHelper.resetMapStateMap();
		}
		
	}
	
	private class NormalToSelectPointAction extends TrafficAction {

		@Override
		public void uiExecute() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeSelectPoint;
			mQueryFragment.displayCommonTitle();
			mQueryFragment.mRightBtn.setVisibility(View.GONE);
			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
			mQueryFragment.mTitle.setVisibility(View.GONE);
			mQueryFragment.mMenuFragment.hide();
			
			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
		}

		@Override
		public void eventExecute() {
			mQueryFragment.mEventHelper.applyListenersInSelectPointState();
		}

		@Override
		public void uiRollback() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeNormal;
			mQueryFragment.hideCommonTitle();
			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
			mQueryFragment.mMenuFragment.display();
			
			applyInnateProperty(TrafficViewSTT.State.Normal);
		}

		@Override
		public void eventRollback() {
			mQueryFragment.mMapLocationHelper.resetNormalStateMap();
		}
		
	}
	
	private class InputToSelectPointAction extends TrafficAction {

		@Override
		public void uiExecute() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeSelectPoint;
			mQueryFragment.displayCommonTitle();
			mQueryFragment.mRightBtn.setVisibility(View.GONE);
			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
			mQueryFragment.mTitle.setVisibility(View.GONE);
			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
		}

		@Override
		public void eventExecute() {
			mQueryFragment.mEventHelper.applyListenersInSelectPointState();
		}

		@Override
		public void uiRollback() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeInput;
			mQueryFragment.hideCommonTitle();
			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventRollback() {
		}
		
	}
	
	private class SelectPointToInputAction extends TrafficAction {
		@Override
		public void uiExecute() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeInput;
			mQueryFragment.hideCommonTitle();
			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
			applyInnateProperty(TrafficViewSTT.State.Input);
		}

		@Override
		public void eventExecute() {
			mQueryFragment.mEventHelper.applyListenersInInputState();
		}

		@Override
		public void uiRollback() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput+ActionLog.Dismiss);
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeSelectPoint;
			mQueryFragment.displayCommonTitle();
			mQueryFragment.mRightBtn.setVisibility(View.GONE);
			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
			mQueryFragment.mTitle.setVisibility(View.GONE);
			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
		}

		@Override
		public void eventRollback() {
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
			applyNormalInnateProperty();
			break;
		case Input:
			applyInputInnateProperty();
			break;
		case Map:
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
		
//		mQueryFragment.mSelectStartBtn.requestFocus();
		mQueryFragment.clearAllText();
		mQueryFragment.initStartContent();
		mQueryFragment.getContentView().setVisibility(View.VISIBLE);
		mQueryFragment.mCityView.setVisibility(View.VISIBLE);
		mQueryFragment.mShadowWhite.setVisibility(View.GONE);
		mQueryFragment.mBackBtn.setVisibility(View.GONE);
		mQueryFragment.mSuggestLnl.setVisibility(View.GONE);
	}
	
	public void applyInputInnateProperty() {
		mQueryFragment.mSphinx.clearMap();
		mQueryFragment.mSphinx.setTouchMode(TouchMode.NORMAL);
		
		mQueryFragment.mSuggestLnl.setVisibility(View.VISIBLE);
		mQueryFragment.mSphinx.getMenuFragment().setVisibility(View.GONE);
		mQueryFragment.mCityView.setVisibility(View.GONE);
		mQueryFragment.mShadowWhite.setVisibility(View.VISIBLE);
		mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
		mQueryFragment.getContentView().setVisibility(View.VISIBLE);
	}
	
	public void applyMapInnateProperty() {
		mQueryFragment.mSphinx.setTouchMode(TouchMode.LONG_CLICK);
		
		mQueryFragment.mRadioGroup.clearCheck();
		mQueryFragment.mSuggestLnl.setVisibility(View.GONE);
		mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
		mQueryFragment.enableQueryBtn(mQueryFragment.mTrafficQueryBtn, false);
		mQueryFragment.enableQueryBtn(mQueryFragment.mBuslineQueryBtn, false);
		mQueryFragment.mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 78), 0, 0);
	}
	
	public void applySelectPointInnateProperty() {
		
		mQueryFragment.getContentView().setVisibility(View.GONE);
		mQueryFragment.mCityView.setVisibility(View.VISIBLE);
		mQueryFragment.mShadowWhite.setVisibility(View.GONE);
		mQueryFragment.mMenuFragment.hide();
		mQueryFragment.mSuggestLnl.setVisibility(View.GONE);

		mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
		mQueryFragment.mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 38), 0, 0);
	}
}

package com.tigerknows.view;

import android.view.View;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.ActionLog;
import com.tigerknows.R;
import com.tigerknows.Sphinx;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.view.TrafficViewSTT.State;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制状态转换(界面转换)]]
 * @author linqingzu
 *
 */
public class TrafficQueryStateHelper {

	private TrafficQueryFragment mQueryFragment;
	
	private static String TAG = "conan";
	
	public TrafficQueryStateHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	public abstract static class trafficAction implements TrafficViewSTT.Action {

        @Override
        public void preEnter() {
            LogWrapper.d(TAG, this.toString() + " preExecute");
        }

        @Override
        public void enterFrom(State oldState) {
            eventExecute(oldState); 
            uiExecute(oldState);
        }

        @Override
        public void preExit() {
            LogWrapper.d(TAG, this.toString() + " preLeave");
        }

        @Override
        public void postExit() {
            LogWrapper.d(TAG, this.toString() + " postLeave");
        }
        
        @Override
        public void postEnter() {
            LogWrapper.d(TAG, this.toString() + " postExecute");
        }
	    
        public abstract void eventExecute(State oldState);
        
        public abstract void uiExecute(State oldState);
	}
	
	private class MapAction extends trafficAction {

        @Override
        public void preEnter() {
            // TODO Auto-generated method stub
            super.preEnter();
        }

        @Override
        public void preExit() {
            // TODO Auto-generated method stub
            super.preExit();
        }

        @Override
        public void postExit() {
            // TODO Auto-generated method stub
            super.postExit();
        }

        @Override
        public void postEnter() {
            // TODO Auto-generated method stub
            super.postEnter();
            mQueryFragment.mEventHelper.applyListenersInMapState();
        }

        @Override
        public void eventExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "map.eventExecute, oldState:" + oldState);
        }

        @Override
        public void uiExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "map.uiExecute, oldState:" + oldState);
            if (oldState == State.Normal) {
                mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeToMap);
                
                mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
                
                mQueryFragment.mAnimationHelper.hideBlockAndMenuAnimation();
                
                mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
            }
            if (oldState == State.Input) {
                /*
                 * 从Input返回Map时, 记录之前Check的Id
                 */
                mQueryFragment.mMapLocationHelper.resetMapStateMap();
                mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
                
                mQueryFragment.mSphinx.clearMap();
                
                mQueryFragment.getContentView().setVisibility(View.GONE);
                mQueryFragment.mCityView.setVisibility(View.VISIBLE);
                mQueryFragment.mShadowWhite.setVisibility(View.GONE);
                mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
            }
            applyInnateProperty(TrafficViewSTT.State.Map);
        }
	    
	}
	
	private class NormalAction extends trafficAction {

        @Override
        public void eventExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "normal.eventExecute, oldState:" + oldState);
            if (oldState != State.SelectPoint) {
                mQueryFragment.mEventHelper.applyListenersInNormalState();
            }
        }

        @Override
        public void uiExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "normal.uiExecute, oldState:" + oldState);
            if (oldState == State.Input) {
                mQueryFragment.mSphinx.getMenuFragment().display();
                //返回normal状态时清理掉输入框的光标
                mQueryFragment.mSelectedEdt.getEdt().clearFocus();
                //放在这里是因为uiRollback最后执行，需要切换完normal之后才能把交通状态清除
//                mQueryFragment.clearTrafficMode();
            }
            if (oldState == State.Map) {

                mQueryFragment.mLogHelper.checkedRadioButton(R.id.traffic_transfer_rbt);
                mQueryFragment.mAnimationHelper.showBlockAndMenuAnimation();

            }
            if (oldState == State.SelectPoint) {
                mQueryFragment.hideCommonTitle();
                mQueryFragment.mTitle.setVisibility(View.VISIBLE);
                mQueryFragment.mMenuFragment.display();
            }
            mQueryFragment.mMapLocationHelper.resetNormalStateMap();
            applyInnateProperty(TrafficViewSTT.State.Normal);
        }
	    
	}
	
	private class InputAction extends trafficAction {

        @Override
        public void eventExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "input.eventExecute, oldState:" + oldState);
            //normal to input
            mQueryFragment.mEventHelper.applyListenersInInputState();
        }

        @Override
        public void uiExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "input.uiExecute, oldState:" + oldState);
            //normal to input
            applyInnateProperty(TrafficViewSTT.State.Input);
            if (oldState == State.SelectPoint) {
                mQueryFragment.hideCommonTitle();
                mQueryFragment.mTitle.setVisibility(View.VISIBLE);
            }
        }
	    
	}
	
	private class SelectPointAction extends trafficAction {

        @Override
        public void eventExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "selectpoint.eventExcute, oldState:" + oldState);
        }

        @Override
        public void uiExecute(State oldState) {
            // TODO Auto-generated method stub
            LogWrapper.d(TAG, "selectpoint.uiExcute, oldState:" + oldState);
            if (oldState == State.Normal) {
                //这个是hide动画
                mQueryFragment.mMenuFragment.hide();
            }
            //以下为normal和input模式的共同操作
            mQueryFragment.displayCommonTitle();
            mQueryFragment.mRightBtn.setVisibility(View.GONE);
            mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
            mQueryFragment.mTitle.setVisibility(View.GONE);
            
            applyInnateProperty(TrafficViewSTT.State.SelectPoint);
        }
        
        @Override
        public void postEnter(){
            super.postEnter();
            mQueryFragment.mEventHelper.applyListenersInSelectPointState();
        }
	    
	}
	
	public MapAction getMapAction(){
	    return new MapAction();
	}
	
	public NormalAction getNormalAction(){
	    return new NormalAction();
	}
	
	public InputAction getInputAction(){
	    return new InputAction();
	}
	
	public SelectPointAction getSelectPointAction(){
	    return new SelectPointAction();
	}
	/*new code end*/
//	private class NormalToInputAction extends TrafficAction {

//		@Override
//		public void uiExecute() {
//			applyInnateProperty(TrafficViewSTT.State.Input);
//		}
//
//		@Override
//		public void eventExecute() {
//			mQueryFragment.mEventHelper.applyListenersInInputState();
//		}

//		@Override
//		public void uiRollback() {
//			mQueryFragment.mSphinx.getMenuFragment().display();
////			resetNormalStateMap();
//			applyInnateProperty(TrafficViewSTT.State.Normal);
//			//返回normal状态时清理掉输入框的光标
//			mQueryFragment.mSelectedEdt.getEdt().clearFocus();
//			//放在这里是因为uiRollback最后执行，需要切换完normal之后才能把交通状态清除
//			mQueryFragment.clearTrafficMode();   
//		}

//		@Override
//		public void eventRollback() {
//			mQueryFragment.mEventHelper.applyListenersInNormalState();
//			
////			if (!isEqualsToMapCenter(mCityInfo)) {
////        		mSphinx.getMapView().zoomTo(mCityInfo.getLevel(), getCityCenterPosition(mCityInfo));
////        	}
//			mQueryFragment.mMapLocationHelper.showNormalStateMap();
//		}

//	}
	
//	private class NormalToMapAction extends TrafficAction {
	
//		@Override
//		public void uiExecute() {
//			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeToMap);
//			
//			mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
//        	
//			mQueryFragment.mAnimationHelper.hideBlockAndMenuAnimation();
//
//			applyInnateProperty(TrafficViewSTT.State.Map);
//			mQueryFragment.mBackBtn.setVisibility(View.VISIBLE);
//  		}

//		@Override
//		public void eventExecute() {
//			mQueryFragment.mEventHelper.applyListenersInMapState();
//		}

//		@Override
//		public void uiRollback() {
//			mQueryFragment.mMapLocationHelper.resetNormalStateMap();
//
//			mQueryFragment.mLogHelper.checkedRadioButton(R.id.traffic_transfer_rbt);
//			mQueryFragment.mAnimationHelper.showBlockAndMenuAnimation();
//
//			applyInnateProperty(TrafficViewSTT.State.Normal);
//        	
//		}

//		@Override
//		public void eventRollback() {
//			mQueryFragment.mEventHelper.applyListenersInNormalState();
//		}
		
//	}
	
//	private class MapToInputAction extends TrafficAction {

//		@Override
//		public void uiExecute() {
//			applyInnateProperty(TrafficViewSTT.State.Input);
//		}
//
//		@Override
//		public void eventExecute() {
//			mQueryFragment.mEventHelper.applyListenersInInputState();
//		}

//		@Override
//		public void uiRollback() {
//			/*
//			 * 从Input返回Map时, 记录之前Check的Id
//			 */
//			mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
//			
//			mQueryFragment.mSphinx.clearMap();
//			
//			mQueryFragment.getContentView().setVisibility(View.GONE);
//			mQueryFragment.mCityView.setVisibility(View.VISIBLE);
//			mQueryFragment.mShadowWhite.setVisibility(View.GONE);
//			applyInnateProperty(TrafficViewSTT.State.Map);
//			mQueryFragment.mSphinx.getControlView().setPadding(0, 0, 0, 0);
//		}

//		@Override
//		public void eventRollback() {
//			mQueryFragment.mEventHelper.applyListenersInMapState();
//			mQueryFragment.mMapLocationHelper.resetMapStateMap();
//		}
		
//	}
	
//	private class NormalToSelectPointAction extends TrafficAction {

//		@Override
//		public void uiExecute() {
//			mQueryFragment.displayCommonTitle();
//			mQueryFragment.mRightBtn.setVisibility(View.GONE);
//			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
//			mQueryFragment.mTitle.setVisibility(View.GONE);
//			mQueryFragment.mMenuFragment.hide();
//			
//			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
//		}

//		@Override
//		public void eventExecute() {
//			mQueryFragment.mEventHelper.applyListenersInSelectPointState();
//		}

//		@Override
//		public void uiRollback() {
//			mQueryFragment.hideCommonTitle();
//			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
//			mQueryFragment.mMenuFragment.display();
//			
//			applyInnateProperty(TrafficViewSTT.State.Normal);
//		}

//		@Override
//		public void eventRollback() {
//			mQueryFragment.mMapLocationHelper.resetNormalStateMap();
//		}
		
//	}
	
//	private class InputToSelectPointAction extends TrafficAction {

//		@Override
//		public void uiExecute() {
//			mQueryFragment.displayCommonTitle();
//			mQueryFragment.mRightBtn.setVisibility(View.GONE);
//			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
//			mQueryFragment.mTitle.setVisibility(View.GONE);
//			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
//		}

//		@Override
//		public void eventExecute() {
//			mQueryFragment.mEventHelper.applyListenersInSelectPointState();
//		}

//		@Override
//		public void uiRollback() {
//			mQueryFragment.hideCommonTitle();
//			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
//			applyInnateProperty(TrafficViewSTT.State.Input);
//		}
//
//		@Override
//		public void eventRollback() {
//		}
		
//	}
	
//	private class SelectPointToInputAction extends TrafficAction {
//		@Override
//		public void uiExecute() {
//			mQueryFragment.hideCommonTitle();
//			mQueryFragment.mTitle.setVisibility(View.VISIBLE);
//			applyInnateProperty(TrafficViewSTT.State.Input);
//		}
//
//		@Override
//		public void eventExecute() {
//			mQueryFragment.mEventHelper.applyListenersInInputState();
//		}

//		@Override
//		public void uiRollback() {
//			mQueryFragment.displayCommonTitle();
//			mQueryFragment.mRightBtn.setVisibility(View.GONE);
//			mQueryFragment.mTitleBtn.setText(R.string.title_click_map);
//			mQueryFragment.mTitle.setVisibility(View.GONE);
//			applyInnateProperty(TrafficViewSTT.State.SelectPoint);
//		}

//		@Override
//		public void eventRollback() {
//		}
//	}
	
//	public NormalToInputAction createNormalToInputAction() {
//		return new NormalToInputAction();
//	}
//	
//	public NormalToMapAction createNormalToMapAction() {
//		return new NormalToMapAction();
//	}
//	
//	public MapToInputAction createMapToInputAction() {
//		return new MapToInputAction();
//	}
//	
//	public NormalToSelectPointAction createNormalToSelectPointAction() {
//		return new NormalToSelectPointAction();
//	}
//	
//	public InputToSelectPointAction createInputToSelectPointAction() {
//		return new InputToSelectPointAction();
//	}
//	
//	public SelectPointToInputAction createSelectPointToInputAction() {
//		return new SelectPointToInputAction();
//	}
	
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

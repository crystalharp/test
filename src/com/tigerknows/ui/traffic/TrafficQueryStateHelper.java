package com.tigerknows.ui.traffic;

import android.view.View;

import com.decarta.Globals;
import com.decarta.android.util.LogWrapper;
import com.decarta.android.util.Util;
import com.tigerknows.R;
import com.tigerknows.Sphinx.TouchMode;
import com.tigerknows.ui.traffic.TrafficViewSTT.State;
import com.tigerknows.common.ActionLog;

/**
 * 负责“交通频道首页”TrafficQueryFragment的[[控制状态转换(界面转换)]]
 * @author linqingzu
 *
 */
public class TrafficQueryStateHelper {

	private TrafficQueryFragment mQueryFragment;
	
	private static String TAG = "TrafficQueryStateHelper";
	
	public TrafficQueryStateHelper(TrafficQueryFragment queryFragment) {
		super();
		this.mQueryFragment = queryFragment;
	}

	public abstract static class trafficAction implements TrafficViewSTT.Action {

        @Override
        public void enterFrom(State oldState) {
            LogWrapper.d(TAG, this.toString() + " enter from " + oldState);
        }

        @Override
        public void exit() {
            LogWrapper.d(TAG, this.toString() + " exit");
        }
        
        @Override
        public void postEnter() {
            LogWrapper.d(TAG, this.toString() + " postEnter");
        }
	}
	
	/**
	 * 
	 * @author xupeng
	 * Map状态可能由Normal和Input状态转换而来
	 */
	private class MapAction extends trafficAction {

        @Override
        public void exit() {
            // TODO Auto-generated method stub
            super.exit();
            mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeMap + ActionLog.Dismiss);
            mQueryFragment.mMapLocationHelper.checkMapCenterInCity();
        }

        @Override
        public void postEnter() {
            super.postEnter();
            mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeMap);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeMap;
			mQueryFragment.mStart.mEdt.mActionTag = ActionLog.TrafficHomeMap;
			mQueryFragment.mEnd.mEdt.mActionTag = ActionLog.TrafficHomeMap;
			mQueryFragment.mBusline.mEdt.mActionTag = ActionLog.TrafficHomeMap;
            mQueryFragment.mEventHelper.applyListenersInMapState();
//            mQueryFragment.mMapLocationHelper.checkMapCenterInCity();
        }

        @Override
        public void enterFrom(State oldState) {
            //input和normal到map状态都要记录之前的checkedBtn
            mQueryFragment.oldCheckButton = mQueryFragment.mRadioGroup.getCheckedRadioButtonId();
            if (oldState == State.Normal) {
                
                mQueryFragment.mAnimationHelper.hideBlockAndMenuAnimation();
            }
            if (oldState == State.Input) {
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
        public void postEnter() {
            // TODO Auto-generated method stub
            super.postEnter();
            mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeNormal;
			mQueryFragment.mStart.mEdt.mActionTag = ActionLog.TrafficHomeNormal;
			mQueryFragment.mEnd.mEdt.mActionTag = ActionLog.TrafficHomeNormal;
			mQueryFragment.mBusline.mEdt.mActionTag = ActionLog.TrafficHomeNormal;
        }

        @Override
        public void exit() {
            // TODO Auto-generated method stub
            super.exit();
            mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeNormal + ActionLog.Dismiss);
        }

        @Override
        public void enterFrom(State oldState) {
            //这个要放在checkRadioButton之前，否则会触发Map状态的Listener，
            //而不是触发Normal状态的Listener。
            if (oldState != State.SelectPoint) {
                mQueryFragment.mEventHelper.applyListenersInNormalState();
            }
            if (oldState == State.Input) {
                //返回normal状态时清理掉输入框的光标
                mQueryFragment.mSelectedEdt.getEdt().clearFocus();
                //放在这里是因为uiRollback最后执行，需要切换完normal之后才能把交通状态清除
//                mQueryFragment.clearTrafficMode();
            }
            if (oldState == State.Map) {

                mQueryFragment.mLogHelper.checkedRadioButton(R.id.traffic_transfer_rbt);
                mQueryFragment.mAnimationHelper.showBlockAndMenuAnimation();

            }
            mQueryFragment.mMenuFragment.display();
            applyInnateProperty(TrafficViewSTT.State.Normal);
        }
	    
	}
	   
	private class InputAction extends trafficAction {

        @Override
        public void exit() {
            // TODO Auto-generated method stub
            super.exit();
            mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput + ActionLog.Dismiss);
        }

        @Override
        public void postEnter() {
            super.postEnter();
            mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeInput);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeInput;
			mQueryFragment.mStart.mEdt.mActionTag = ActionLog.TrafficHomeInput;
			mQueryFragment.mEnd.mEdt.mActionTag = ActionLog.TrafficHomeInput;
			mQueryFragment.mBusline.mEdt.mActionTag = ActionLog.TrafficHomeInput;
            mQueryFragment.mEventHelper.applyListenersInInputState();
        }

        @Override
        public void enterFrom(State oldState) {
            applyInnateProperty(TrafficViewSTT.State.Input);
        }
	    
	}

	private class SelectPointAction extends trafficAction {

        @Override
        public void exit() {
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint+ActionLog.Dismiss);
            mQueryFragment.hideCommonTitle();
            mQueryFragment.mTitle.setVisibility(View.VISIBLE);
            mQueryFragment.mMapLocationHelper.checkMapCenterInCity();
        }

        @Override
        public void enterFrom(State oldState) {
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
			mQueryFragment.mActionLog.addAction(ActionLog.TrafficHomeSelectPoint);
			mQueryFragment.mActionTag = ActionLog.TrafficHomeSelectPoint;
			mQueryFragment.mStart.mEdt.mActionTag = ActionLog.TrafficHomeSelectPoint;
			mQueryFragment.mEnd.mEdt.mActionTag = ActionLog.TrafficHomeSelectPoint;
			mQueryFragment.mBusline.mEdt.mActionTag = ActionLog.TrafficHomeSelectPoint;
            mQueryFragment.mEventHelper.applyListenersInSelectPointState();
            mQueryFragment.mMapLocationHelper.checkMapCenterInCity();
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
        
//      mQueryFragment.mSelectStartBtn.requestFocus();
        mQueryFragment.clearAllText();
        mQueryFragment.initStartContent();
        mQueryFragment.getContentView().setVisibility(View.VISIBLE);
        mQueryFragment.mCityView.setVisibility(View.VISIBLE);
        mQueryFragment.mShadowWhite.setVisibility(View.GONE);
        mQueryFragment.mBackBtn.setVisibility(View.GONE);
        mQueryFragment.mSuggestLnl.setVisibility(View.GONE);
        mQueryFragment.mSphinx.layoutTopViewPadding(0, Util.dip2px(Globals.g_metrics.density, 182), 0, 0);
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
}

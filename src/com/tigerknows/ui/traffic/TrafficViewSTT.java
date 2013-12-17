//package com.tigerknows.ui.traffic;
//
//import com.decarta.android.util.LogWrapper;
//import java.util.Stack;
//
///**
// * 交通页面状态迁移表 
// * 
// * @author xupeng
// * 该文件是交通首页的状态迁移表(State transition table)，状态和状态迁移事件由枚举表示如下。
// * 
// * 状态跳转表用二维int数组表示，在本class创建的时候进行内容设置，所有不存在的状态跳转结果状态全部用MaxSize表示。
// * 
// * 状态跳转时的处理用接口Action进行执行。接口定义了三个行为，enterFrom，postEnter和exit，执行顺序为：
// * 1.新状态的enterFrom。一般用来放UI变换的函数。
// * 2.老状态的exit。一般用来执行老状态可能进行的界面等的清理。
// * 3.新状态的postEnter。一般用来放最后需要添加的各种控件的listener。
// * listener放在最后添加的原因是map界面下先添加listener再变换界面会导致RadioGroup的onCheckChanged被触发。
// * 
// * 状态可以回退，所以经过的状态用栈来存储，这样回退的时候可以弹出老状态。
// * 正常的状态跳转是遇到正常事件则将转换得到的新状态压栈，根据老状态和新状态进行界面变换;遇到back事件时弹栈，根据
// * 弹出的状态和栈顶状态进行界面变换。这个变换中有一个比较怪异的跳转，SelectPoint状态接受PointSelected事件的
// * 时候到input状态，这时候触发back事件并不会回到SelectPoint状态，而是回到normal状态。也就是说弹栈的时候
// * SelectPoint状态永远不在其中，于是PointSelected的的处理就比较特殊。接到PointSelected事件的时候固定把
// * SelectPoint状态弹出来，栈顶如果是input(从input状态转换过来)，就不需要再压入input状态了，这样触发back事件
// * 的时候永远不会回到SelectPiont。
// * 
// */
//public class TrafficViewSTT {
//	
//    //交通输入页的基本状态：普通状态，地图状态，输入状态，选点输入状态。
//	public static enum State {Normal, Map, Input, SelectPoint, MaxSize};
//	
//	//交通输入页的基本事件：点击起终点选择按钮，点击输入框，点击地图，点击上面的交通方式切换按钮
//	//                      长按地图，点击地图选点，选点完成, 按返回键或者返回按钮
//	public static enum Event {ClickSelectStartEndBtn, ClickEditText, TouchMap, ClickRadioGroup, LongClick, ClicktoSelectPoint, PointSelected, Back, MaxSize};
//
//	private Stack<State> stateStack = new Stack<State>();
//	
//	int[][] StateTransTbl = new int[State.MaxSize.ordinal()][Event.MaxSize.ordinal()];
//	
//	static Action[] ActionTbl = new Action[State.MaxSize.ordinal()];
//	
//	private String TAG = "TrafficViewSTT";
//	
//	//状态机的运转状态。可设置为false防止误触发状态机跳转
//	private boolean running = true;
//
//	public void resetInitState(State state) {
//	    stateStack.clear();
//	    stateStack.push(state);
//	}
//	
//	public State getCurrentState() {
//		return stateStack.peek();
//	}
//
//	public void setRunning(boolean run){
//	    running = run;
//	}
//
//	public TrafficViewSTT(TrafficQueryStateHelper mStateHelper) {
//	    //状态跳转表初始化
//	    for (int i = 0; i < State.MaxSize.ordinal(); i++) {
//	        for (int j = 0; j < Event.MaxSize.ordinal(); j++) {
//	            StateTransTbl[i][j] = State.MaxSize.ordinal();
//	        }
//	    }
//    	setStateTrans(State.Normal, Event.ClickSelectStartEndBtn, State.Input);
//    	setStateTrans(State.Normal, Event.ClickEditText, State.Input);
//    	setStateTrans(State.Normal, Event.ClickRadioGroup, State.Input);
//    	setStateTrans(State.Normal, Event.TouchMap, State.Map);
//    	setStateTrans(State.Normal, Event.ClicktoSelectPoint, State.SelectPoint);
//    	setStateTrans(State.Map, Event.ClickRadioGroup, State.Input);
//    	setStateTrans(State.Map, Event.LongClick, State.Input);
//    	setStateTrans(State.Input, Event.ClicktoSelectPoint, State.SelectPoint);
//    	setStateTrans(State.SelectPoint, Event.PointSelected, State.Input);
//    	//函数表
//    	ActionTbl[State.Input.ordinal()] = mStateHelper.getInputAction();
//    	ActionTbl[State.Map.ordinal()] = mStateHelper.getMapAction();
//    	ActionTbl[State.Normal.ordinal()] = mStateHelper.getNormalAction();
//    	ActionTbl[State.SelectPoint.ordinal()] = mStateHelper.getSelectPointAction();
//    	//初始化状态为normal
//    	stateStack.push(State.Normal);
//	}
//	   
//    public interface Action{
//        //UI需要做的变化
//        void enterFrom(State oldState);
//        //listener的挂接
//        void postEnter();
//        //状态离开需要做的清理
//        void exit();
//    }
//	
//	final void setStateTrans(State oldState, Event event, State newState) {
//	    StateTransTbl[oldState.ordinal()][event.ordinal()] = newState.ordinal();
//	}
//	
//	final State getState(State state, Event event) {
//	    return State.values()[StateTransTbl[state.ordinal()][event.ordinal()]];
//	}
//	
//	public boolean event(Event event) {
//	    LogWrapper.d(TAG, "Event:" + event);
//	    //如果是back,弹栈，执行上个state的和pointSelected，则压栈，否则弹栈。
//	    State oldState, newState;
//	    if (!running) {
//	        return false;
//	    }
//	    switch (event) {
//	    case Back:
//	        //返回上个状态，先弹栈，新状态是之前的状态
//	        if (stateStack.size() == 1) {
//	            return false;
//	        } else {
//    	        oldState = stateStack.pop();
//    	        newState = stateStack.peek();
//	        }
//	        break;
//	    case PointSelected:
//	        //地图选点状态下，选中点后地图选点模式需要被弹栈而不是压栈。如果不是从input过来，则需要把input压进去
//	        oldState = stateStack.pop();
//	        newState = getState(oldState, event);
//	        if (stateStack.peek() != State.Input) {
//	            stateStack.push(newState);
//	        }
//	        break;
//        default:
//            oldState = stateStack.peek();
//            newState = getState(oldState, event);
//            if (newState == State.MaxSize ) {
//                //没有这个转换规则，不做任何处理
//                return false;
//            } else {
//                stateStack.push(newState);
//            }
//            break;
//	    }
//	    
//        ActionTbl[newState.ordinal()].enterFrom(oldState);
//        ActionTbl[oldState.ordinal()].exit();
//        ActionTbl[newState.ordinal()].postEnter();
//        LogWrapper.d(TAG, oldState + " --" + event + "--> " + newState);
//        LogWrapper.d(TAG, "stack is:" + stateStack.toString());
//        
//        return true;
//	}
//}

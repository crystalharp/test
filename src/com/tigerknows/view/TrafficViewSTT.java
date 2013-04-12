package com.tigerknows.view;

import com.decarta.android.util.LogWrapper;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;

/**
 * 交通页面状态迁移表 
 * 
 * @author linqingzu
 *
 */
public class TrafficViewSTT {
	
    //交通输入页的基本状态：普通状态，地图状态，输入状态，选点输入状态。
	public enum State {Normal, Map, Input, SelectPoint, MaxSize};
	
	//交通输入页的基本事件：点击起终点选择按钮，点击输入框，点击地图，点击上面的交通方式切换按钮
	//                      长按地图，点击地图选点，选点完成, 按返回键或者返回按钮
	public enum Event {ClickSelectStartEndBtn, ClickEditText, TouchMap, ClickRadioGroup, LongClick, ClicktoSelectPoint, PointSelected, Back, MaxSize};

	private String TAG = "conan";
//	private Vector<Transition> transitions = new Vector<Transition>();
	
//	private State currentState = State.Normal;
	
//	private Stack<Transition> stack = new Stack<Transition>();
	
//	Stack<Integer> s = new Stack<Integer>();
	public void resetInitState(State state) {
//		this.currentState = currentState;
	    stateStack.clear();
	    stateStack.push(state);
	}
	
	public State getCurrentState() {
		return stateStack.peek();
	}

//
//	public interface Action {
//		void execute();
//		void rollback();
//	}
//
//	private class Transition {
//		
//	    private State startState;
//	    private Event event;
//	    private Action action;
//	    private State endState;
//		
//	    public Transition(State startState, Event event, Action action, State endState) {
//	        this.startState = startState;
//	        this.event = event;
//	        this.action = action;
//	        this.endState = endState;
//	    }
//		
//	    public String toString(){
//	        return startState.toString() + "->" + endState.toString() + "^" + event.toString(); 
//	    }
//	}
	//应该用二维数组，那样就可以STT[state][event]来进行检索了
//	public TrafficViewSTT(TrafficQueryStateHelper mStateHelper) {
//	    addTransition(State.Normal, Event.ClickSelectStartEndBtn, mStateHelper.createNormalToInputAction(), State.Input);
//		addTransition(State.Normal, Event.ClickEditText, mStateHelper.createNormalToInputAction(), State.Input);
//		addTransition(State.Normal, Event.TouchMap, mStateHelper.createNormalToMapAction(), State.Map);
//		addTransition(State.Normal, Event.ClicktoSelectPoint, mStateHelper.createNormalToSelectPointAction(), State.SelectPoint);
//		addTransition(State.Map, Event.ClickRadioGroup, mStateHelper.createMapToInputAction(), State.Input);
//		addTransition(State.Map, Event.LongClick, mStateHelper.createMapToInputAction(), State.Input);
//		addTransition(State.Input, Event.ClicktoSelectPoint, mStateHelper.createInputToSelectPointAction(), State.SelectPoint);
//		addTransition(State.SelectPoint, Event.PointSelected, mStateHelper.createSelectPointToInputAction(), State.Input);
//	}
//	
//	public void addTransition(State startState, Event event, Action action, State endState) {
//		transitions.add(new Transition(startState, event, action, endState));
//	}
//	
//	public void event(Event event) {
//		for (int i = 0 ;i < transitions.size(); i++) {
//			Transition transition = transitions.elementAt(i);
//			if (currentState == transition.startState && event == transition.event) {
//				LogWrapper.d("eric", "event " + event.toString() + " occur." + "Change from " + currentState.toString() + " to " + transition.endState.toString());
//				
//				transition.action.execute();
//				currentState = transition.endState;
//				stack.push(transition);
//			}
//		}
//	}
	private Stack<State> stateStack = new Stack<State>();
//	State [][]STT = new State[State.MaxSize.ordinal()][Event.MaxSize.ordinal()];
	int[][] StateTransTbl = new int[State.MaxSize.ordinal()][Event.MaxSize.ordinal()];
	static Action[] ActionTbl = new Action[State.MaxSize.ordinal()];

	public TrafficViewSTT(TrafficQueryStateHelper mStateHelper) {
	    for (int i = 0; i < State.MaxSize.ordinal(); i++) {
	        for (int j = 0; j < Event.MaxSize.ordinal(); j++) {
	            StateTransTbl[i][j] = State.MaxSize.ordinal();
	        }
	    }
	    //状态跳转表
    	setStateTrans(State.Normal, Event.ClickSelectStartEndBtn, State.Input);
    	setStateTrans(State.Normal, Event.ClickEditText, State.Input);
    	setStateTrans(State.Normal, Event.TouchMap, State.Map);
    	setStateTrans(State.Normal, Event.ClicktoSelectPoint, State.SelectPoint);
    	setStateTrans(State.Map, Event.ClickRadioGroup, State.Input);
    	setStateTrans(State.Map, Event.LongClick, State.Input);
    	setStateTrans(State.Input, Event.ClicktoSelectPoint, State.SelectPoint);
    	setStateTrans(State.SelectPoint, Event.PointSelected, State.Input);
    	//函数表
    	ActionTbl[State.Input.ordinal()] = mStateHelper.getInputAction();
    	ActionTbl[State.Map.ordinal()] = mStateHelper.getMapAction();
    	ActionTbl[State.Normal.ordinal()] = mStateHelper.getNormalAction();
    	ActionTbl[State.SelectPoint.ordinal()] = mStateHelper.getSelectPointAction();
    	//初始化状态为normal
    	stateStack.push(State.Normal);
	}
	   
    public interface Action{
        void preEnter();
        void enterFrom(State oldState);
        void postEnter();
        void preExit();
        void postExit();
    }
	
	final void setStateTrans(State oldState, Event event, State newState) {
	    StateTransTbl[oldState.ordinal()][event.ordinal()] = newState.ordinal();
	}
	
	final State getState(State state, Event event) {
	    return State.values()[StateTransTbl[state.ordinal()][event.ordinal()]];
	}
	
	public boolean event(Event event) {
	    LogWrapper.d(TAG, "Event:" + event);
	    //如果是back,弹栈，执行上个state的和pointSelected，则压栈，否则弹栈。
	    State oldState, newState;
	    switch (event) {
	    case Back:
	        //返回上个状态，先弹栈，新状态是之前的状态
	        //错误情况1：栈中没有更多状态了。
	        if (stateStack.size() == 1) {
	            return false;
	        } else {
    	        oldState = stateStack.pop();
    	        newState = stateStack.peek();
	        }
	        break;
	    case PointSelected:
	        //地图选点状态下，选中点后地图选点模式需要被弹栈而不是压栈。如果不是从input过来，则需要把input压进去
	        oldState = stateStack.pop();
	        newState = getState(oldState, event);
	        if (stateStack.peek() != State.Input) {
	            stateStack.push(newState);
	        }
	        break;
        default:
            oldState = stateStack.peek();
            newState = getState(oldState, event);
            if (newState == State.MaxSize ) {
                return false;
            } else {
                stateStack.push(newState);
            }
            break;
	    }
	    
	    //需要处理：没有这个状态怎么办？
	    ActionTbl[newState.ordinal()].preEnter();
	    ActionTbl[oldState.ordinal()].preExit();
        ActionTbl[newState.ordinal()].enterFrom(oldState);
        ActionTbl[newState.ordinal()].postEnter();
        ActionTbl[oldState.ordinal()].postExit();
        LogWrapper.d(TAG, oldState + " --" + event + "--> " + newState);
        LogWrapper.d(TAG, "stack is:" + stateStack.toString());
        
        return true;
	}
}

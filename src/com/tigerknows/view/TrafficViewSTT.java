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
	
	public enum State {Normal, Map, Input, SelectPoint};
	
	public enum Event {ClickBookmark, ClickEditText, TouchMap, ClickRadioGroup, LongClick, Point, SelectPoint};

	private Vector<Transition> transitions = new Vector<Transition>();
	
	private State currentState = State.Normal;
	
	private Stack<Transition> stack = new Stack<Transition>();
	
	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}
	
	public State getCurrentState() {
		return currentState;
	}


	public interface Action {
		void execute();
		void rollback();
	}

	private class Transition {
		
		public Transition(State startState, Event event, Action action, State endState) {
			this.startState = startState;
			this.event = event;
			this.action = action;
			this.endState = endState;
		}
		private State startState;
		private Event event;
		private Action action;
		private State endState;
	}
	
	public TrafficViewSTT(TrafficQueryStateHelper mStateHelper) {
		addTransition(State.Normal, Event.ClickBookmark, mStateHelper.createNormalToInputAction(), State.Input);
		addTransition(State.Normal, Event.ClickEditText, mStateHelper.createNormalToInputAction(), State.Input);
		addTransition(State.Normal, Event.TouchMap, mStateHelper.createNormalToMapAction(), State.Map);
		addTransition(State.Normal, Event.Point, mStateHelper.createNormalToSelectPointAction(), State.SelectPoint);
		addTransition(State.Map, Event.ClickRadioGroup, mStateHelper.createMapToInputAction(), State.Input);
		addTransition(State.Map, Event.LongClick, mStateHelper.createMapToInputAction(), State.Input);
		addTransition(State.Input, Event.Point, mStateHelper.createInputToSelectPointAction(), State.SelectPoint);
		addTransition(State.SelectPoint, Event.SelectPoint, mStateHelper.createSelectPointToInputAction(), State.Input);
	}
	
	public void addTransition(State startState, Event event, Action action, State endState) {
		transitions.add(new Transition(startState, event, action, endState));
	}
	
	public void event(Event event) {
		for (int i = 0 ;i < transitions.size(); i++) {
			Transition transition = transitions.elementAt(i);
			if (currentState == transition.startState && event == transition.event) {
				LogWrapper.d("eric", "event " + event.toString() + " occur." + "Change from " + currentState.toString() + " to " + transition.endState.toString());
				
				transition.action.execute();
				currentState = transition.endState;
				stack.push(transition);
			}
		}
	}
	
	public boolean rollback() {
		if (!stack.isEmpty()) {
			Transition transition = stack.pop();
			transition.action.rollback();
			currentState = transition.startState;
			
			LogWrapper.d("eric", "event " + " rollback." + "rollback from " + transition.endState.toString() + " to " + transition.startState.toString());
			
			return true;
		}
		return false;
	}
	
	public void mergeFirstTwoTranstion(Event event, Action action) {
		Transition first = stack.pop();
		Transition second = stack.pop();
		if (second.startState != first.endState) {
			stack.push(new Transition(second.startState, event, action, first.endState));
			LogWrapper.d("eric", "mergeFirstTwoTranstion " + "instead with " + second.startState.toString() + " - " + first.endState.toString());
		} else {
			LogWrapper.d("eric", "mergeFirstTwoTranstion " + "remove transition second: " + second + " first: " + first);
		}
	}
	
	public void statckPop() {
		stack.pop();
	}
	
	public void clearTransitionStack() {
		stack.clear();
	}
}

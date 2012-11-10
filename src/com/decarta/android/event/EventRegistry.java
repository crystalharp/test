/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.event;

import com.decarta.android.exception.APIException;

/**
 * EventRegistry provides static methods to do all event registering/unregistering operations.
 */
public class EventRegistry {
	/**
	 * add one listener of the event type to the event source
	 * @param source event source such as map, pin, infowindow
	 * @param eventType event type such as touch, moveend, zoomend, doubleclick
	 * @param listener listener to perform operation on specific event
	 */
	public static void addEventListener(EventSource source, int eventType, EventListener listener) throws APIException{
		source.addEventListener(eventType, listener);
	}
	
	/**
	 * remove all listeners of the event type from the event source
	 * @param source event source such as map, pin, infowindow
	 * @param eventType event type such as touch, moveend, zoomend, doubleclick
	 */
	public void removeAllEventListeners(EventSource source, int eventType) {
		source.removeAllEventListeners(eventType);
	}
	
	/**
	 * remove one listener of the event type from the event source
	 * @param source
	 * @param eventType
	 * @param listener
	 */
	public void removeEventListener(EventSource source, int eventType, EventListener listener) throws APIException{
		source.removeEventListener(eventType, listener);
	}
}

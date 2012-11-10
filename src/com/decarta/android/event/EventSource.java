/**
 * deCarta Android Mapping API
 * deCarta confidential and proprietary.
 * Copyright deCarta. All rights reserved.
 */
package com.decarta.android.event;

import com.decarta.android.exception.APIException;

/**
 * Interface for all event source such as map, pin, info window
 *
 */
public interface EventSource {
	public void addEventListener(int eventType, EventListener listener) throws APIException;
	public boolean isSupportedEventListener(int eventType, EventListener listener);
	public void removeEventListener(int eventType, EventListener listener) throws APIException;
	public void removeAllEventListeners(int eventType);
	
}

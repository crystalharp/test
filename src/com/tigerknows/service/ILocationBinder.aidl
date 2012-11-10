package com.tigerknows.service;

import com.tigerknows.service.ILocationListener;

interface ILocationBinder {

    boolean isTaskRunning();
       
    void stopRunningTask();   
    
    void registerCallback(ILocationListener cb);   
  
    void unregisterCallback(ILocationListener cb);

}
